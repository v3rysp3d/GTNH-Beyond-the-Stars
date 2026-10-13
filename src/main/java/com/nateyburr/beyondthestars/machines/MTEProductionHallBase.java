package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * D-26: the shared body of the four new production multiblocks (docs/production-lines.md S5).
 * <p>
 * All four are the same kind of machine - a shell, a dedicated socket per hatch, a private recipe
 * map and a parallel count the STRUCTURE sets - and differ only in shape, casing and how the
 * parallel is derived. Everything that is identical lives here so it is written once and can only
 * be wrong once.
 * <p>
 * <b>AXIS 1, CLIENT vs SERVER.</b> {@link #casingTier} and {@link #mCasing} are written by
 * {@code checkMachine}, which runs SERVER-side only, and are read by {@link #getMaxParallelRecipes}
 * (server, inside the recipe check) and {@link #getInfoData} (server, for the scanner). The GUI
 * ({@link MTEProductionHallGui}) reads them only through sync values - the fields are 0 on the
 * client.
 * <p>
 * <b>AXIS 2, TILE vs ITEM NBT.</b> Neither field is persisted. Both are re-derived by every
 * structure check, which is the only way they can be right after a player swaps a casing: a
 * remembered tier would survive a downgrade and quietly keep paying the old parallel.
 */
public abstract class MTEProductionHallBase<T extends MTEProductionHallBase<T>>
    extends MTEExtendedPowerMultiBlockBase<T> implements ISurvivalConstructable {

    /** Shell blocks counted by the structure walk. Server-side; re-derived every check. */
    protected int mCasing;

    /**
     * The tier of the shell the walk actually found, 1-based. Machines with one legal shell block
     * leave it at 1; the Fuel Fabrication Hall reads 1, 2 or 3 off its walls. Server-side, never
     * persisted - see the class comment.
     */
    protected int casingTier = 1;

    protected MTEProductionHallBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTEProductionHallBase(String aName) {
        super(aName);
    }

    /** The casing the machine's own face is painted with. */
    protected abstract Casings faceCasing();

    /** The four front-overlay icons: {inactive, inactive glow, active, active glow}. */
    protected abstract ITexture[] frontOverlay(boolean active);

    /**
     * Parallel, derived from the structure. This is the one number that makes each of these
     * machines worth an MTE, so every subclass states its own rule and says why in its comment.
     */
    @Override
    public int getMaxParallelRecipes() {
        return 1;
    }

    /**
     * The parallel is read through a SUPPLIER, not baked in at construction. These machines can
     * be re-formed larger (another Cascade Hall stage bank, a Fuel Fabrication Hall re-walled in
     * Radiant Naquadah Alloy) without being broken and replaced, and a logic built once with a
     * fixed number would go on paying the old parallel until the chunk unloaded.
     * <p>
     * The supplier is GT's {@code getTrueParallel()} (TQ-8, settled 2026-10-13: "You have full free reign on
     * this"), as on every GT multi: the structure's parallel, capped by the Power Panel's limit when its "max"
     * box is unticked. The box is ticked by default, so only a player who set a limit sees a lower number.
     */
    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().setMaxParallelSupplier(this::getTrueParallel);
    }

    /**
     * TQ-6 (settled 2026-10-13): the halls offer GT's input-separation button like other GT multis. It starts OFF
     * (new and existing machines alike), so buses keep pooling until the player turns it on.
     */
    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean getDefaultInputSeparationMode() {
        return false;
    }

    /**
     * Tidy B8: the one number behind the parallel, for the GUI line - the shell tier here; the
     * Cascade Hall overrides it with its stage banks. Server-side value; the GUI syncs it.
     */
    public int getParallelDetail() {
        return casingTier;
    }

    /**
     * Tidy B8: what sets the parallel, as the GUI prints it in brackets after the number, from
     * the SYNCED {@link #getParallelDetail} value. It runs CLIENT-side, so overrides must use only
     * the argument and constants, never a field (AXIS 1). "" prints the bare number.
     */
    public String parallelSource(int detail) {
        return "";
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new MTEProductionHallGui(this);
    }

    /** GUI cleanup (controller-gui-spec.md §4.4): the kit's badge says it; GT's "Running perfectly." is not added. */
    @Override
    public boolean hasRunningText() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    /** Casings counted on the last successful structure check. */
    public int getCasingCount() {
        return mCasing;
    }

    /** The shell tier the last structure check measured. */
    public int getCasingTier() {
        return casingTier;
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Parallel: " + String.format("%,d", getTrueParallel()));
        info.add("Casings: " + String.format("%,d", mCasing));
        return info.toArray(new String[0]);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = faceCasing().getCasingTexture();
        if (side != aFacing) return new ITexture[] { base };
        final ITexture[] overlay = frontOverlay(aActive);
        final ITexture[] out = new ITexture[overlay.length + 1];
        out[0] = base;
        System.arraycopy(overlay, 0, out, 1, overlay.length);
        return out;
    }

    /** Two-icon front overlay (plain + glow), built the way every stock GT controller builds it. */
    protected static ITexture[] overlayPair(gregtech.api.interfaces.IIconContainer icon,
        gregtech.api.interfaces.IIconContainer glow) {
        return new ITexture[] { TextureFactory.builder()
            .addIcon(icon)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(glow)
                .extFacing()
                .glow()
                .build() };
    }
}
