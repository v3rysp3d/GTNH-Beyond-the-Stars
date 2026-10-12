package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * D-49 my answer 3: the Eye of Harmony's centre render tile. It carries the synced {@link EyeStarLook} (what
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
    private AxisAlignedBB box;
    private float boxRadius = -1f;

    public TileEyeStar() {}

    // ------------------------------------------------------------------ server side

    /** Server: sets the owner (saved; no packet of its own). */
    public void setOwner(int x, int y, int z) {
        owner = new int[] { x, y, z };
        markDirty();
    }

    /** Server: the new look, in one packet to the clients watching this chunk. */
    public void setLook(EyeStarLook look) {
        if (worldObj == null || worldObj.isRemote || look == null) return;
        kind = look.kind;
        radius = look.radius;
        rgb = look.rgb;
        gamma = look.gamma;
        spin = look.spin;
        pulse = look.pulse;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
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
            kindSince = now;
            seenKind = kind;
        }
        return now - kindSince;
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
    }

    /** Only the look goes to clients (the owner is server business). */
    @Override
    public Packet getDescriptionPacket() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag(NBT_LOOK, writeLook());
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readLook(
            pkt.func_148857_g()
                .getCompoundTag(NBT_LOOK));
    }
}
