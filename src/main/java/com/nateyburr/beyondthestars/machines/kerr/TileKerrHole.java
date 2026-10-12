package com.nateyburr.beyondthestars.machines.kerr;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.render.RenderingTileEntityBlackhole;

/**
 * KERR REDESIGN R4: the Kerr Ergosphere Engine's black-hole render tile (docs/kerr-redesign.md §5).
 * <p>
 * GT's own {@link RenderingTileEntityBlackhole} (the Black Hole Compressor's: growth/collapse animation,
 * stability, its description-packet sync) plus a synced {@link #getKerrScale() scale} that
 * {@link KerrHoleRenderer} applies around GT's draw, and the KEE that owns it.
 * <p>
 * <b>Server-placed, server-removed</b>, as GT's BHC does it: the KEE places the block at the hole centre
 * and removes it ({@link MTEKerrEngine}). This tile also polices itself, so a render block can never be
 * orphaned: every {@link #SELF_CHECK} ticks on the server it asks the owner's cell, once that cell is
 * loaded, whether a KEE there still claims this exact cell, and turns itself to air if not (the KEE was
 * broken while this chunk was unloaded, the KEE was rotated or re-placed, a world edit, an ownerless
 * block).
 */
public class TileKerrHole extends RenderingTileEntityBlackhole {

    /** Server: ticks between owner checks. */
    static final int SELF_CHECK = 100;

    private static final String NBT_SCALE = "btsKerrScale";
    private static final String NBT_OWNER = "btsKerrOwner";

    /** The draw scale around GT's model (1 = the BHC's 2.6-block hole). Synced. */
    private float kerrScale = 1f;
    /** The owning KEE controller, or null (an ownerless render block removes itself). */
    private int[] owner;
    private int ticks;
    /** Client: the bounding box for {@link #boxScale}; rebuilt when the scale changes. */
    private AxisAlignedBB box;
    private float boxScale = -1f;

    public TileKerrHole() {}

    public float getKerrScale() {
        return kerrScale;
    }

    /** Server: sets the owner (no packet; the next scale/stability push carries it). */
    void setOwner(int x, int y, int z) {
        owner = new int[] { x, y, z };
    }

    /**
     * Server: the scale and the governor's coverage (0..1), in one packet to the clients watching this
     * chunk. GT's {@link #setStability} reads 0.5 and up as calm, so it gets {@code coverage / 2}: the
     * shader's stability then equals the coverage.
     */
    void setLook(float scale, float coverage) {
        if (worldObj == null || worldObj.isRemote) return;
        kerrScale = scale;
        setStability(coverage * 0.5f);
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (++ticks % SELF_CHECK != 0) return;
        if (owner == null) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            return;
        }
        // Never loads the owner's chunk: an unloaded owner is checked again later.
        if (!worldObj.blockExists(owner[0], owner[1], owner[2])) return;
        final TileEntity te = worldObj.getTileEntity(owner[0], owner[1], owner[2]);
        if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEKerrEngine
            && ((MTEKerrEngine) ((IGregTechTileEntity) te).getMetaTileEntity()).claimsRender(xCoord, yCoord, zCoord)) {
            return;
        }
        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }

    /** The hole and its disc at this scale (the model is 4.15 units at GT's 0.5), plus a block. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (box == null || boxScale != kerrScale) {
            final double r = kerrScale * 2.1 + 1;
            box = AxisAlignedBB
                .getBoundingBox(xCoord - r, yCoord - r, zCoord - r, xCoord + 1 + r, yCoord + 1 + r, zCoord + 1 + r);
            boxScale = kerrScale;
        }
        return box;
    }

    /** The cage is 76 blocks across: visible from well outside it (vanilla is 64 blocks). */
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 256.0 * 256.0;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat(NBT_SCALE, kerrScale);
        if (owner != null) compound.setIntArray(NBT_OWNER, owner);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        kerrScale = compound.hasKey(NBT_SCALE) ? compound.getFloat(NBT_SCALE) : 1f;
        final int[] o = compound.getIntArray(NBT_OWNER);
        owner = o.length == 3 ? o : null;
    }
}
