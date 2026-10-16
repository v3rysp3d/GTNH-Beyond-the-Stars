package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * D-49 answer 3: the Eye of Harmony's centre render tile. It carries the synced {@link EyeStarLook} (what
 * object, how big, what colour, how it moves) that {@link EyeStarRenderer} draws with TecTech's star textures and the
 * Forge of the Gods star shader.
 * <p>
 * <b>Server-placed, server-removed</b>, as GT's Black Hole Compressor and the Kerr's hole render do it: the Eye places
 * the block in the empty cell at its centre while it is formed and its star burns, and removes it when the structure
 * breaks, the star goes out, the controller is broken or wrenched, or the render is switched off. This tile also
 * polices itself, so no render block is ever orphaned: every {@link #SELF_CHECK} ticks on the server it turns itself to
 * air if the owner's cell is not loaded (the Eye places it again once it ticks), or if no Eye there claims this exact
 * cell (the Eye was broken while this chunk was unloaded, rotated, a world edit, an ownerless block).
 */
public class TileEyeStar extends TileEntity {

    /** Server: ticks between owner checks. */
    static final int SELF_CHECK = 100;

    private static final String NBT_LOOK = "btsEyeLook";
    private static final String NBT_OWNER = "btsEyeOwner";

    // ------------------------------------------------------------------ synced look
    private byte kind = EyeStarLook.STAR;
    private float radius = 9f;
    private int rgb = 0xFFD678;
    private float gamma = 3f;
    private float spin = 1f;
    private float pulse = 0.02f;
    private float aux;
    // renders v2 (R-04): render-only extras of the look (EyeStarLook.n, ev, evAt, trans, transP)
    private byte tierN;
    private byte ev;
    private long evAt;
    private byte trans;
    private float transP;
    // renders v2 WP-5: the T8 satellite crossing's hit as a world tick (0 none)
    private long xAt;
    /** P5: the T9 / T10 render-only snapshot (null for every other object). */
    private EyeP5Look p5;
    /**
     * B-253: the owning controller's horizontal facing (ForgeDirection ordinal 2..5), -1 unknown. Synced with every
     * look, not only inside {@link #p5}, so a T9 / T10 is drawn in its controller's frame from the first frame: before
     * a P5 snapshot arrives and under a render QA look that carries none.
     */
    private byte facing = -1;
    /** Dev-only render QA (P5): when not NaN, {@link #flowIn} / {@link #universeAge} return these. NaN in play. */
    public static volatile double debugFlowIn = Double.NaN, debugUniverseAge = Double.NaN;
    /** Client: the event the channel showed before the current one, and its hit (an afterglow outlives the event). */
    private byte pastEv;
    private long pastEvAt;

    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}, client): when 0 or more, {@link #kindAge} and
     * {@link #flareAge} return this instead of the real ages, so a still shot catches the bounce's shock shell or a
     * magnetar flare at a fixed point of its run. Negative (off) in play; only the render QA harness sets it.
     */
    public static volatile double debugEventAge = -1;
    /**
     * Dev-only render QA (renders v2): when not NaN, {@link #eventIn} returns this (seconds to the event's hit,
     * negative after it), so a still catches a warning or a hit at a fixed point. NaN in play.
     */
    public static volatile double debugEventIn = Double.NaN;
    /** Dev-only render QA: when 0 or more, {@link #previousKind} returns this (the ignition flash's protostar). */
    public static volatile int debugPrevKind = -1;
    /**
     * Dev-only render QA (renders v2 WP-5): when not NaN, {@link #crossingIn} returns this (seconds to the satellite
     * crossing's hit, negative after it). NaN in play.
     */
    public static volatile double debugCrossingIn = Double.NaN;

    /** Client: magnetar flares, every {@link #FLARE_MIN}..{@link #FLARE_MIN}x2 ticks. */
    static final int FLARE_MIN = 80;

    /** The owning Eye controller, or null (an ownerless render block removes itself). */
    private int[] owner;
    private int ticks;

    // ------------------------------------------------------------------ client animation state
    /** The radius drawn now, eased toward {@link #radius}; negative until the first frame. */
    private float shownRadius = -1f;
    private double lastFrame = -1;
    /** World time when {@link #kind} last changed on this client (the bounce and nova shells run from it). */
    private double kindSince = -1;
    private byte seenKind = -1;
    private byte prevKind = -1;
    /** World time when {@link #trans} last changed on this client (a transition's opening flash runs from it). */
    private double transSince = -1;
    private byte seenTrans = -1;
    /** World time the current magnetar flare started, and when the next one starts (negative: not scheduled). */
    private double flareSince = -1e9;
    private double nextFlare = -1;
    private int flares;
    private AxisAlignedBB box;
    private float boxRadius = -1f;

    public TileEyeStar() {}

    // ------------------------------------------------------------------ server side

    /** Server: sets the owner (saved; no packet of its own). */
    public void setOwner(int x, int y, int z) {
        owner = new int[] { x, y, z };
        markDirty();
    }

    /**
     * Server: the new look, in one description packet straight to the players watching this chunk. Not through
     * {@code markBlockForUpdate}: that also sends a block change, and every client then rebuilds the whole chunk
     * section (render QA E10: the cooling dwarf changes colour every field second). A player who starts watching the
     * chunk later gets the look with the chunk's tile data.
     */
    public void setLook(EyeStarLook look) {
        setLook(look, facing);
    }

    /** Server: as {@link #setLook(EyeStarLook)}, with the owning controller's facing (ForgeDirection ordinal). */
    public void setLook(EyeStarLook look, int facingOrdinal) {
        if (worldObj == null || worldObj.isRemote || look == null) return;
        facing = (byte) (facingOrdinal >= 2 && facingOrdinal <= 5 ? facingOrdinal : -1);
        kind = look.kind;
        radius = look.radius;
        rgb = look.rgb;
        gamma = look.gamma;
        spin = look.spin;
        pulse = look.pulse;
        aux = look.aux;
        tierN = look.n;
        ev = look.ev;
        evAt = look.evAt;
        trans = look.trans;
        transP = look.transP;
        xAt = look.xAt;
        p5 = look.p5;
        markDirty();
        if (!(worldObj instanceof WorldServer)) return;
        final WorldServer world = (WorldServer) worldObj;
        final Packet packet = getDescriptionPacket();
        final int cx = xCoord >> 4;
        final int cz = zCoord >> 4;
        for (Object o : world.playerEntities) {
            if (o instanceof EntityPlayerMP && world.getPlayerManager()
                .isPlayerWatchingChunk((EntityPlayerMP) o, cx, cz)) {
                ((EntityPlayerMP) o).playerNetServerHandler.sendPacket(packet);
            }
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (++ticks % SELF_CHECK != 0) return;
        if (owner == null) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            return;
        }
        // Never loads the owner's chunk. An Eye that is not loaded is not running, so its star is not shown: the
        // block goes, and the Eye places it again when it next ticks.
        if (!worldObj.blockExists(owner[0], owner[1], owner[2])) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            return;
        }
        final TileEntity te = worldObj.getTileEntity(owner[0], owner[1], owner[2]);
        if (te instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEEyeOfHarmonyBTS
            && ((MTEEyeOfHarmonyBTS) ((IGregTechTileEntity) te).getMetaTileEntity())
                .claimsRender(xCoord, yCoord, zCoord)) {
            return;
        }
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ client side

    public byte getKind() {
        return kind;
    }

    public float getRadius() {
        return radius;
    }

    public int getRgb() {
        return rgb;
    }

    public float getGamma() {
        return gamma;
    }

    public float getSpin() {
        return spin;
    }

    public float getPulse() {
        return pulse;
    }

    /** Kind-specific 0..1 ({@link EyeStarLook#aux}): the dwarf's cooling fraction, the magnetar's warning. */
    public float getAux() {
        return aux;
    }

    /** P5: the last synced T9 / T10 snapshot, or null. */
    public EyeP5Look getP5() {
        return p5;
    }

    /** B-253: the owning controller's facing (ForgeDirection ordinal 2..5) as last synced, -1 unknown. */
    public int getFacing() {
        return facing;
    }

    /** P5, client: seconds to the flow's arrival (negative after it), NaN with none. */
    @SideOnly(Side.CLIENT)
    public double flowIn(double now) {
        if (!Double.isNaN(debugFlowIn)) return debugFlowIn;
        return p5 == null || p5.flowAt == 0 ? Double.NaN : (p5.flowAt - now) / 20.0;
    }

    /** P5, client: the universe's age in seconds (extrapolated unless paused), NaN with none. */
    @SideOnly(Side.CLIENT)
    public double universeAge(double now) {
        if (!Double.isNaN(debugUniverseAge)) return debugUniverseAge;
        if (p5 == null || p5.uniStartAt == 0) return Double.NaN;
        return p5.paused ? p5.uniAge : (now - p5.uniStartAt) / 20.0;
    }

    /** P5, client: seconds to Cosmic Dawn's hit (negative after it), NaN with none. */
    @SideOnly(Side.CLIENT)
    public double dawnIn(double now) {
        return p5 == null || p5.dawnAt == 0 ? Double.NaN : (p5.dawnAt - now) / 20.0;
    }

    /** P5, client: seconds to Recombination (negative after it), NaN with none. */
    @SideOnly(Side.CLIENT)
    public double recombinationIn(double now) {
        return p5 == null || p5.recAt == 0 ? Double.NaN : (p5.recAt - now) / 20.0;
    }

    /** P5, client: seconds since the last universe ended (Big Rip, Big Crunch, last light), NaN with none. */
    @SideOnly(Side.CLIENT)
    public double endAge(double now) {
        return p5 == null || p5.endAt == 0 ? Double.NaN : (now - p5.endAt) / 20.0;
    }

    /** Renders v2: the star's tier (1..8), 0 unknown. */
    public int getTierN() {
        return tierN;
    }

    /** Renders v2: the event on the channel ({@code EyeStarLook.EV_*}), 0 none. */
    public int getEvent() {
        return ev;
    }

    /** Renders v2: the running transition (1..7), 0 none. */
    public int getTransition() {
        return trans;
    }

    /** Renders v2: how far the running transition is, 0..1. */
    public float getTransitionProgress() {
        return transP;
    }

    /**
     * Client, renders v2: seconds from {@code now} (world ticks) to the event's hit, negative after it; NaN with no
     * event.
     */
    @SideOnly(Side.CLIENT)
    public double eventIn(double now) {
        final double frozen = debugEventIn;
        if (!Double.isNaN(frozen)) return ev == 0 ? Double.NaN : frozen;
        return ev == 0 ? Double.NaN : (evAt - now) / 20.0;
    }

    /**
     * Client, renders v2 WP-5: seconds from {@code now} to the satellite crossing's hit, negative after it; NaN with
     * none.
     */
    @SideOnly(Side.CLIENT)
    public double crossingIn(double now) {
        final double frozen = debugCrossingIn;
        if (!Double.isNaN(frozen)) return frozen;
        return xAt == 0 ? Double.NaN : (xAt - now) / 20.0;
    }

    /**
     * Client, renders v2 WP-5: seconds from {@code now} to the hit of the event the channel showed before the current
     * one (negative: it hit that long ago), so a minor merger's afterglow outlives its event; NaN when there was none
     * or ({@code minorOnly}) it was not a minor merger.
     */
    @SideOnly(Side.CLIENT)
    public double pastEventIn(double now, boolean minorOnly) {
        if (pastEv == 0 || pastEvAt == 0) return Double.NaN;
        if (minorOnly && pastEv != EyeStarLook.EV_MINOR_MERGER_UP && pastEv != EyeStarLook.EV_MINOR_MERGER_DOWN) {
            return Double.NaN;
        }
        return (pastEvAt - now) / 20.0;
    }

    /** Client, renders v2 WP-5: the past minor merger's sign (+1 from above, -1 from below). */
    @SideOnly(Side.CLIENT)
    public int pastEventSign() {
        return pastEv == EyeStarLook.EV_MINOR_MERGER_DOWN ? -1 : 1;
    }

    /** Client, renders v2 WP-5: a number fixed per event (its hit tick), for the event's look (a dwarf's path). */
    @SideOnly(Side.CLIENT)
    public int eventSeed() {
        final long at = ev != 0 ? evAt : pastEvAt;
        final long h = (at * 0x9E3779B97F4A7C15L) ^ (xCoord * 73_856_093L) ^ (zCoord * 83_492_791L);
        return (int) (h ^ (h >>> 29));
    }

    /** Client, renders v2: ticks since the transition last changed (debug-frozen like {@link #kindAge}). */
    @SideOnly(Side.CLIENT)
    public double transitionAge(double now) {
        if (transSince < 0 || seenTrans != trans) {
            transSince = now;
            seenTrans = trans;
        }
        final double frozen = debugEventAge;
        return frozen >= 0 ? frozen : now - transSince;
    }

    /** Client, renders v2: the kind shown before the current one (-1: none seen). */
    @SideOnly(Side.CLIENT)
    public int previousKind() {
        final int forced = debugPrevKind;
        return forced >= 0 ? forced : prevKind;
    }

    /**
     * Client, once per frame: the radius to draw, eased toward the synced one with a 10-tick time constant (the server
     * sends at most once a second, so a cooling or collapsing object moves smoothly).
     */
    @SideOnly(Side.CLIENT)
    public float easedRadius(double now) {
        if (shownRadius < 0 || lastFrame < 0 || now < lastFrame) {
            shownRadius = radius;
        } else {
            final double k = 1 - Math.exp(-(now - lastFrame) / 10.0);
            shownRadius += (float) ((radius - shownRadius) * k);
        }
        lastFrame = now;
        return shownRadius;
    }

    /** Client: ticks since the object last changed kind (a fresh block counts from its first frame). */
    @SideOnly(Side.CLIENT)
    public double kindAge(double now) {
        if (kindSince < 0 || seenKind != kind) {
            if (seenKind >= 0 && seenKind != kind) prevKind = seenKind;
            kindSince = now;
            seenKind = kind;
        }
        final double frozen = debugEventAge;
        return frozen >= 0 ? frozen : now - kindSince;
    }

    /**
     * Client: ticks since the magnetar's current flare started. Flares come every {@link #FLARE_MIN} to twice that
     * many ticks (a fixed pseudo-random sequence per block), the first one a second after the object appears.
     */
    @SideOnly(Side.CLIENT)
    public double flareAge(double now) {
        if (nextFlare < 0 || now < flareSince) {
            nextFlare = now + 20;
            flareSince = -1e9;
        }
        if (now >= nextFlare) {
            flareSince = nextFlare;
            flares++;
            final int h = (xCoord * 73_856_093) ^ (yCoord * 19_349_663) ^ (zCoord * 83_492_791) ^ (flares * 2_654_435);
            nextFlare = Math.max(now, flareSince) + FLARE_MIN + Math.floorMod(h, FLARE_MIN + 1);
        }
        final double frozen = debugEventAge;
        return frozen >= 0 ? frozen : now - flareSince;
    }

    /**
     * Render QA round 4 (D1): the opaque core in pass 0, every see-through layer (corona, glow, beams, magnetar shells,
     * bounce and nova shells) in pass 1, after the translucent and glowing block faces, so those faces are tinted by
     * the layers in front of them instead of drawn over them. {@link EyeStarRenderer} asks the pass.
     */
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    /** The object and the nova / bounce shells (up to 3x the radius), plus a block. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        final float r = Math.max(radius, 15f) * 3f + 1f;
        if (box == null || boxRadius != r) {
            box = AxisAlignedBB
                .getBoundingBox(xCoord - r, yCoord - r, zCoord - r, xCoord + 1 + r, yCoord + 1 + r, zCoord + 1 + r);
            boxRadius = r;
        }
        return box;
    }

    /** The Eye is up to 158 blocks across: its star shows from well outside it (vanilla is 64 blocks). */
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 256.0 * 256.0;
    }

    // ------------------------------------------------------------------ NBT and sync

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag(NBT_LOOK, writeLook());
        if (owner != null) compound.setIntArray(NBT_OWNER, owner);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        readLook(compound.getCompoundTag(NBT_LOOK));
        final int[] o = compound.getIntArray(NBT_OWNER);
        owner = o.length == 3 ? o : null;
    }

    private NBTTagCompound writeLook() {
        final NBTTagCompound look = new NBTTagCompound();
        look.setByte("kind", kind);
        look.setFloat("radius", radius);
        look.setInteger("rgb", rgb);
        look.setFloat("gamma", gamma);
        look.setFloat("spin", spin);
        look.setFloat("pulse", pulse);
        look.setFloat("aux", aux);
        // renders v2 (R-04): render-only extras, written only when set
        if (tierN != 0) look.setByte("n", tierN);
        if (ev != 0) {
            look.setByte("ev", ev);
            look.setLong("evAt", evAt);
        }
        if (trans != 0) {
            look.setByte("tr", trans);
            look.setFloat("trP", transP);
        }
        // renders v2 WP-5: the satellite crossing's hit
        if (xAt != 0) look.setLong("xAt", xAt);
        // P5: the T9 / T10 snapshot, only when set
        if (p5 != null) look.setTag("p5", p5.write());
        // B-253: the controller's facing, for every look
        if (facing >= 0) look.setByte("fc", facing);
        return look;
    }

    private void readLook(NBTTagCompound look) {
        if (look == null || !look.hasKey("kind")) return;
        kind = look.getByte("kind");
        radius = look.getFloat("radius");
        rgb = look.getInteger("rgb");
        gamma = look.getFloat("gamma");
        spin = look.getFloat("spin");
        pulse = look.getFloat("pulse");
        aux = look.getFloat("aux");
        tierN = look.getByte("n");
        final byte newEv = look.getByte("ev");
        final long newEvAt = look.getLong("evAt");
        // the client keeps the event that just ended, for its afterglow
        if (ev != 0 && (newEv != ev || newEvAt != evAt)) {
            pastEv = ev;
            pastEvAt = evAt;
        }
        ev = newEv;
        evAt = newEvAt;
        xAt = look.getLong("xAt");
        trans = look.getByte("tr");
        transP = look.getFloat("trP");
        p5 = look.hasKey("p5") ? EyeP5Look.read(look.getCompoundTag("p5")) : null;
        facing = look.hasKey("fc") ? look.getByte("fc") : (byte) -1;
    }

    /** Only the look goes to clients (the owner is server business). */
    @Override
    public Packet getDescriptionPacket() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag(NBT_LOOK, writeLook());
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    /** Only the look changes: no chunk re-render (render QA E10). */
    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readLook(
            pkt.func_148857_g()
                .getCompoundTag(NBT_LOOK));
    }
}
