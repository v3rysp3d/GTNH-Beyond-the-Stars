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

    private int[] owner;
    private int ticks;
    @SideOnly(Side.CLIENT)
    private AxisAlignedBB box;

    public TileQuasarHole() {}

    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}, client): when set, the disc and jet readers return these
     * instead of the synced look, so one still shot catches a zone, a re-forming disc or an eruption / purge flash at a
     * fixed age: {zone, lambda_c, E, jet top Bu/t, jet bottom Bu/t, flash age (ticks, negative: no flash), flash size}.
     * Null in play; only the render QA harness sets it. The hole's scale and GT's stability stay the synced ones.
     */
    public static volatile float[] debugLook;

    /** Indices into {@link #debugLook}. */
    public static final int DBG_ZONE = 0, DBG_LC = 1, DBG_E = 2, DBG_JET_TOP = 3, DBG_JET_BOTTOM = 4, DBG_FLASH_AGE = 5,
        DBG_FLASH_SIZE = 6, DBG_LENGTH = 7;

    // ------------------------------------------------------------------ readers (client renderer)

    public float getHoleScale() {
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

    // ------------------------------------------------------------------ server

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
        if (worldObj == null || worldObj.isRemote) return false;
        final boolean moved = Math.abs(scale - holeScale) > 0.02f * Math.max(0.1f, holeScale)
            || Math.abs(stability - shaderStability) > 0.02f
            || Math.abs(lambdaC - lc) > 0.02f
            || zoneNow != zone
            || Math.signum(eNow) != Math.signum(e) && Math.abs(eNow - e) > 2
            || Math.abs(eNow - e) > 8
            || jetMoved(top, jetTop)
            || jetMoved(bottom, jetBottom)
            || flash;
        holeScale = scale;
        shaderStability = stability;
        lc = lambdaC;
        zone = zoneNow;
        e = eNow;
        jetTop = top;
        jetBottom = bottom;
        if (flash) {
            flashTick = worldObj.getTotalWorldTime();
            flashSize = flashFrac;
        }
        if (moved) sendLook();
        return moved;
    }

    private static boolean jetMoved(float now, float was) {
        if (now == was) return false;
        if (now <= 0 || was <= 0) return true;
        return Math.abs(now - was) > 0.05f * Math.max(now, was);
    }

    /** The look to the players watching this chunk, without a block update (no chunk re-render). */
    public void sendLook() {
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

    /** The disc reaches r 62 and the jets y ±80 from the centre. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
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
