package com.nateyburr.beyondthestars.machines.quasar.render;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.render.RenderingTileEntityBlackhole;

/**
 * P4 (quasar-p4.md §1.6): the Quasar Engine's render tile at the hole centre, a sibling of the Kerr's
 * {@code TileKerrHole}. It is GT's {@link RenderingTileEntityBlackhole} (so GT's own {@code BlackholeRenderer} draws
 * the horizon, with its growth/collapse animation and stability shader) plus the disc and jet look that
 * {@link QuasarHoleRenderer} draws around it:
 * <ul>
 * <li>{@link #getHoleScale()}: the GL scale about the centre, s = (4 + 4·log₂(M/64)/6)/1.3 (3.1 at 64 SM, 6.2 at
 * 4,096 SM);</li>
 * <li>the disc: λc (its width, r 40 to 40 + 22·λc), the zone (its colour; −1 dark / re-forming), E (it puffs up above
 * 0) and the eruption flash;</li>
 * <li>each jet's Bu/t (its brightness).</li>
 * <li>renders v2 (§6.3, §13, R-03), render-only: the last event's kind (eruption, purge, blow-out, re-formed, First
 * Light), the spin a (the inner disc's edge, the jets' helix), the flux Φ (the field lines), the Harmonic Lock and how
 * far a re-forming disc has come. They live in this render tile's look only, never in the machine's saved state.</li>
 * <li>SMBH v1 (smbh-v1.md §7, R-19), render-only as R-03: the Supermassive Lattice tier in force, the Overdrive level
 * in effect, the Unbound doublings n, the Growth share of the intake, the hold-up coverage and whether the hole is
 * shedding. The lattice's node cells are fixed by the exported shape (16 per ring, 11.25 + 22.5k degrees), so the
 * renderer places them itself.</li>
 * </ul>
 * <b>Sync without chunk re-renders</b> (the Eye's render QA E10 fix): a new look goes as a description packet straight
 * to the players watching this chunk ({@link #sendLook}), never through {@code markBlockForUpdate}; only GT's
 * growth/collapse start ({@code startScaleChange}) still uses GT's own update. The stability for GT's shader is
 * written into GT's own NBT tag, so GT's renderer reads it unchanged.
 * <p>
 * <b>Server-placed, server-removed</b>, and self-policing like the Kerr's: every {@link #SELF_CHECK} ticks it asks
 * its owner's cell (once loaded) whether a quasar there still claims this cell, and turns itself to air if not.
 */
public class TileQuasarHole extends RenderingTileEntityBlackhole {

    static final int SELF_CHECK = 100;

    private static final String NBT_LOOK = "btsQuasarLook";
    private static final String NBT_OWNER = "btsQuasarOwner";
    /** GT's stability tag (RenderingTileEntityBlackhole: "BLACKHOLE" + "STABILITY"). */
    private static final String GT_STABILITY = "BLACKHOLESTABILITY";

    // ---- the look (synced)
    private float holeScale = 3.1f;
    /** GT's internal stability 0..1 (1 calm). */
    private float shaderStability = 1f;
    private float lc;
    private int zone = -1;
    private float e;
    /** World time of the last eruption / purge flash (client animates from it). */
    private long flashTick = -1000;
    private float flashSize;
    private float jetTop, jetBottom;
    /** Renders v2: the last flash's event ({@link #EV_ERUPTION} ..), the spin, the flux, the lock, re-forming 0..1. */
    private int event;
    private float spin, phi, reform;
    private boolean lock;
    /**
     * SMBH (R-19), render-only: the lattice tier in force (0 none), the Overdrive level in effect (0..1), the Unbound
     * doublings n, the Growth segment over the intake cap (0..1), the hold-up coverage (0..1) and a running shed.
     */
    private int lattice;
    private float overdrive, unboundN, growth, hold = 1f;
    private boolean shed;
    /** A SMBH field moved enough to send (the next {@link #setLook} sends it). */
    private boolean smbhMoved;

    private int[] owner;
    private int ticks;
    @SideOnly(Side.CLIENT)
    private AxisAlignedBB box, latticeBox;

    public TileQuasarHole() {}

    /** Renders v2: the event kinds a flash can carry (0: none). */
    public static final int EV_ERUPTION = 1, EV_PURGE = 2, EV_BLOWOUT = 3, EV_REFORMED = 4, EV_FIRST_LIGHT = 5;

    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}, client): when set, the disc and jet readers return these
     * instead of the synced look, so one still shot catches a zone, a re-forming disc or an eruption / purge flash at a
     * fixed age: {zone, lambda_c, E, jet top Bu/t, jet bottom Bu/t, flash age (ticks, negative: no flash), flash size,
     * and (renders v2) the event kind, spin, flux, Harmonic Lock 0/1, re-forming fraction}. A 7-entry array (the
     * pre-v2 form) leaves the last five synced. Null in play; only the render QA harness sets it. The hole's scale and
     * GT's stability stay the synced ones.
     */
    public static volatile float[] debugLook;

    /** Indices into {@link #debugLook}. */
    public static final int DBG_ZONE = 0, DBG_LC = 1, DBG_E = 2, DBG_JET_TOP = 3, DBG_JET_BOTTOM = 4, DBG_FLASH_AGE = 5,
        DBG_FLASH_SIZE = 6, DBG_EVENT = 7, DBG_SPIN = 8, DBG_PHI = 9, DBG_LOCK = 10, DBG_REFORM = 11,
        DBG_LENGTH_V2 = 12;
    /**
     * SMBH (R-19): a 19-entry {@link #debugLook} adds the lattice tier, the Overdrive level, the Unbound doublings, the
     * Growth share, the hold-up coverage, shedding 0/1 and the mass (SM, &gt; 0: the hole is drawn at that mass instead
     * of the synced scale).
     */
    public static final int DBG_LATTICE = 12, DBG_OVERDRIVE = 13, DBG_UNBOUND = 14, DBG_GROWTH = 15, DBG_HOLD = 16,
        DBG_SHED = 17, DBG_MASS = 18, DBG_LENGTH = 19;

    // ------------------------------------------------------------------ readers (client renderer)

    public float getHoleScale() {
        final float[] d = debugLook;
        if (d != null && d.length > DBG_MASS && d[DBG_MASS] > 0) return MTEQuasarEngine.renderScaleFor(d[DBG_MASS]);
        return holeScale;
    }

    public float getLambdaC() {
        final float[] d = debugLook;
        return d != null ? d[DBG_LC] : lc;
    }

    public int getZone() {
        final float[] d = debugLook;
        return d != null ? Math.round(d[DBG_ZONE]) : zone;
    }

    public float getE() {
        final float[] d = debugLook;
        return d != null ? d[DBG_E] : e;
    }

    public long getFlashTick() {
        return flashTick;
    }

    /** Client: ticks since the last eruption / purge flash at world time {@code t} (render QA may freeze it). */
    public float flashAge(float t) {
        final float[] d = debugLook;
        if (d != null) return d[DBG_FLASH_AGE] < 0 ? 1e9f : d[DBG_FLASH_AGE];
        return t - flashTick;
    }

    /**
     * Client, renders v2: ticks since the last flash at world time {@code t} (double: no float rounding late in a
     * world).
     */
    public double flashAge(double t) {
        final float[] d = debugLook;
        if (d != null) return d[DBG_FLASH_AGE] < 0 ? 1e9 : d[DBG_FLASH_AGE];
        return t - flashTick;
    }

    public float getFlashSize() {
        final float[] d = debugLook;
        return d != null ? d[DBG_FLASH_SIZE] : flashSize;
    }

    public float getJetTop() {
        final float[] d = debugLook;
        return d != null ? d[DBG_JET_TOP] : jetTop;
    }

    public float getJetBottom() {
        final float[] d = debugLook;
        return d != null ? d[DBG_JET_BOTTOM] : jetBottom;
    }

    /** Renders v2: the last flash's event kind (0 none, {@link #EV_ERUPTION} .. {@link #EV_FIRST_LIGHT}). */
    public int getEvent() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_EVENT ? Math.round(d[DBG_EVENT]) : event;
    }

    /** Renders v2: the hole's spin a, 0..0.998. */
    public float getSpin() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_SPIN ? d[DBG_SPIN] : spin;
    }

    /** Renders v2: the horizon flux Φ, 0..100. */
    public float getPhi() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_PHI ? d[DBG_PHI] : phi;
    }

    /** Renders v2: the Harmonic Lock is on. */
    public boolean getLock() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_LOCK ? d[DBG_LOCK] > 0.5f : lock;
    }

    /** Renders v2: how far a re-forming disc has come, 0..1 (0 when not re-forming). */
    public float getReform() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_REFORM ? d[DBG_REFORM] : reform;
    }

    /** SMBH: the Supermassive Lattice tier in force, 0 (none) .. 4. */
    public int getLattice() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_LATTICE ? Math.round(d[DBG_LATTICE]) : lattice;
    }

    /** SMBH: the Overdrive level in effect, 0..1. */
    public float getOverdrive() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_OVERDRIVE ? d[DBG_OVERDRIVE] : overdrive;
    }

    /** SMBH: the Unbound doublings n past the L4 top (0 when not Unbound). */
    public float getUnboundN() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_UNBOUND ? d[DBG_UNBOUND] : unboundN;
    }

    /** SMBH: the Growth segment of the intake over the intake cap, 0..1. */
    public float getGrowth() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_GROWTH ? d[DBG_GROWTH] : growth;
    }

    /** SMBH: the hold-up coverage, 0..1 (1: held, below: leaking). */
    public float getHold() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_HOLD ? d[DBG_HOLD] : hold;
    }

    /** SMBH: the hole is shedding mass (the flare up both jets). */
    public boolean getShed() {
        final float[] d = debugLook;
        return d != null && d.length > DBG_SHED ? d[DBG_SHED] > 0.5f : shed;
    }

    // ------------------------------------------------------------------ server

    /**
     * Server, SMBH (R-19): the lattice's render-only look, carried by the next {@link #setLook} (sent when the tier,
     * the
     * shed or the leak state changes, Overdrive or n by 0.02, Growth or the coverage by 0.03).
     */
    public void setSmbh(int latticeNow, float overdriveNow, float nNow, float growthNow, float holdNow,
        boolean shedNow) {
        if (worldObj == null || worldObj.isRemote) return;
        smbhMoved |= latticeNow != lattice || shedNow != shed
            || Math.abs(overdriveNow - overdrive) > 0.02f
            || Math.abs(nNow - unboundN) > 0.02f
            || Math.abs(growthNow - growth) > 0.03f
            || Math.abs(holdNow - hold) > 0.03f
            || (holdNow < 0.999f) != (hold < 0.999f);
        lattice = latticeNow;
        overdrive = overdriveNow;
        unboundN = nNow;
        growth = growthNow;
        hold = holdNow;
        shed = shedNow;
    }

    /** Server: the owning controller (no packet; the next look carries it). */
    public void setOwner(int x, int y, int z) {
        owner = new int[] { x, y, z };
    }

    /**
     * Server: a new look. Sent to watchers only if it moved enough to see (scale 2 %, stability / λc 0.02, the zone,
     * E's sign, a jet 5 %, or a new flash). Returns true if a packet went out.
     */
    public boolean setLook(float scale, float stability, float lambdaC, int zoneNow, float eNow, float top,
        float bottom, boolean flash, float flashFrac) {
        return setLook(
            scale,
            stability,
            lambdaC,
            zoneNow,
            eNow,
            top,
            bottom,
            flash ? EV_ERUPTION : 0,
            flashFrac,
            spin,
            phi,
            lock,
            reform);
    }

    /**
     * Server, renders v2: a new look with the render-only fields. {@code eventNow} &gt; 0 starts a flash of that kind
     * ({@link #EV_ERUPTION} ..). Also sent when the spin moves by 0.01, the flux by 2, the lock or the re-forming
     * progress by 2 %.
     */
    public boolean setLook(float scale, float stability, float lambdaC, int zoneNow, float eNow, float top,
        float bottom, int eventNow, float flashFrac, float spinNow, float phiNow, boolean lockNow, float reformNow) {
        if (worldObj == null || worldObj.isRemote) return false;
        final boolean flash = eventNow > 0;
        // QA-B35: compared with what was last SENT, so a slow drift (flux +1.25 a second against a threshold of 2)
        // goes out once it adds up instead of never
        final boolean moved = Math.abs(scale - sent[0]) > 0.02f * Math.max(0.1f, sent[0])
            || Math.abs(stability - sent[1]) > 0.02f
            || Math.abs(lambdaC - sent[2]) > 0.02f
            || zoneNow != zone
            || Math.signum(eNow) != Math.signum(sent[3]) && Math.abs(eNow - sent[3]) > 2
            || Math.abs(eNow - sent[3]) > 8
            || jetMoved(top, sent[4])
            || jetMoved(bottom, sent[5])
            || flash
            || Math.abs(spinNow - sent[6]) > 0.01f
            || Math.abs(phiNow - sent[7]) > 2f
            || lockNow != lock
            || Math.abs(reformNow - sent[8]) > 0.02f
            || smbhMoved;
        smbhMoved = false;
        holeScale = scale;
        spin = spinNow;
        phi = phiNow;
        lock = lockNow;
        reform = reformNow;
        shaderStability = stability;
        lc = lambdaC;
        zone = zoneNow;
        e = eNow;
        jetTop = top;
        jetBottom = bottom;
        if (flash) {
            flashTick = worldObj.getTotalWorldTime();
            flashSize = flashFrac;
            event = eventNow;
        }
        if (moved) sendLook();
        return moved;
    }

    private static boolean jetMoved(float now, float was) {
        if (now == was) return false;
        if (now <= 0 || was <= 0) return true;
        return Math.abs(now - was) > 0.05f * Math.max(now, was);
    }

    /** The look as last sent (server): scale, stability, λc, E, top, bottom, spin, flux, re-form. */
    private final float[] sent = new float[9];

    /** The look to the players watching this chunk, without a block update (no chunk re-render). */
    public void sendLook() {
        if (!(worldObj instanceof WorldServer)) return;
        sent[0] = holeScale;
        sent[1] = shaderStability;
        sent[2] = lc;
        sent[3] = e;
        sent[4] = jetTop;
        sent[5] = jetBottom;
        sent[6] = spin;
        sent[7] = phi;
        sent[8] = reform;
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
        if (!worldObj.blockExists(owner[0], owner[1], owner[2])) return;
        final TileEntity te = worldObj.getTileEntity(owner[0], owner[1], owner[2]);
        if (te instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEQuasarEngine
            && ((MTEQuasarEngine) ((IGregTechTileEntity) te).getMetaTileEntity())
                .claimsRender(xCoord, yCoord, zCoord)) {
            return;
        }
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ render bounds

    /**
     * The disc reaches r 62 and the jets y ±80 from the centre; with a lattice in force its nodes reach r 89 (SMBH, the
     * equatorial ring) and the arcs and intake streams r 95.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (getLattice() > 0) {
            if (latticeBox == null) {
                latticeBox = AxisAlignedBB
                    .getBoundingBox(xCoord - 97, yCoord - 82, zCoord - 97, xCoord + 98, yCoord + 83, zCoord + 98);
            }
            return latticeBox;
        }
        if (box == null) {
            box = AxisAlignedBB
                .getBoundingBox(xCoord - 64, yCoord - 82, zCoord - 64, xCoord + 65, yCoord + 83, zCoord + 65);
        }
        return box;
    }

    /** The quasar is 143 blocks across: visible from well outside it. */
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 384.0 * 384.0;
    }

    // ------------------------------------------------------------------ NBT and packets

    private NBTTagCompound writeLook() {
        final NBTTagCompound t = new NBTTagCompound();
        t.setFloat("s", holeScale);
        t.setFloat("st", shaderStability);
        t.setFloat("lc", lc);
        t.setInteger("z", zone);
        t.setFloat("e", e);
        t.setLong("ft", flashTick);
        t.setFloat("fs", flashSize);
        t.setFloat("jt", jetTop);
        t.setFloat("jb", jetBottom);
        // renders v2 (R-03): render-only
        t.setByte("ev", (byte) event);
        t.setFloat("a", spin);
        t.setFloat("phi", phi);
        t.setBoolean("lock", lock);
        t.setFloat("rf", reform);
        // SMBH (R-19): render-only
        t.setByte("lt", (byte) lattice);
        t.setFloat("od", overdrive);
        t.setFloat("un", unboundN);
        t.setFloat("gr", growth);
        t.setFloat("hc", hold);
        t.setBoolean("sh", shed);
        return t;
    }

    private void readLook(NBTTagCompound t) {
        if (t == null || !t.hasKey("s")) return;
        holeScale = t.getFloat("s");
        shaderStability = t.getFloat("st");
        lc = t.getFloat("lc");
        zone = t.getInteger("z");
        e = t.getFloat("e");
        flashTick = t.getLong("ft");
        flashSize = t.getFloat("fs");
        jetTop = t.getFloat("jt");
        jetBottom = t.getFloat("jb");
        // renders v2: absent in an older look (an eruption flash then)
        event = t.hasKey("ev") ? t.getByte("ev") : EV_ERUPTION;
        spin = t.getFloat("a");
        phi = t.getFloat("phi");
        lock = t.getBoolean("lock");
        reform = t.getFloat("rf");
        // SMBH: absent in an older look (no lattice)
        lattice = t.getByte("lt");
        overdrive = t.getFloat("od");
        unboundN = t.getFloat("un");
        growth = t.getFloat("gr");
        hold = t.hasKey("hc") ? t.getFloat("hc") : 1f;
        shed = t.getBoolean("sh");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        // GT's shader reads its own stability tag: write ours over it (GT's setter would mark the block for update)
        compound.setFloat(GT_STABILITY, shaderStability);
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

    @Override
    public Packet getDescriptionPacket() {
        final NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
