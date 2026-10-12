package com.nateyburr.beyondthestars.machines;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;

import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * Spine Sensor - the chain's confirmation reference. Since TC-3 the plant no longer runs a
 * free clock: a cycle starts on a redstone rising edge at the Turbine Control Block, and is
 * ACCEPTED only if a buffered steam slug is there to drink. The silo pulses this hatch on
 * every accepted cycle, full strength for 2 ticks on all sides; the +/-2 tick injection
 * window opens on that same edge. Gating injections on this echo is legitimate play - it
 * saves propellant on cycles that found no steam.
 */
public class MTEHatchSpineSensor extends MTEHatch {

    /** World time (exclusive) at which the signal drops; emitted synchronously in pulse(). */
    private long pulseUntil;

    public MTEHatchSpineSensor(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            4,
            0,
            new String[] { "Optional: Spiral chamber roof, front centre",
                "Emits a 2-tick redstone pulse on every accepted turbine cycle",
                "Use it as the timing reference if you inject propellant by hand" });
    }

    public MTEHatchSpineSensor(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchSpineSensor(mName, mTier, mDescriptionArray, mTextures);
    }

    /**
     * Without this the sensor can never be aimed: MTEHatch does not override it and
     * CommonMetaTileEntity's default is FALSE, so setFrontFacing is a silent no-op and the
     * hatch keeps the freshly-placed default (ForgeDirection.DOWN) - reading as plain casing
     * from every side a player can see. The redstone pulse goes out on all six sides
     * regardless; this is purely so the socket looks like the instrument it is.
     */
    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY_FRONT_HEAT_EXCHANGER) };
    }

    /**
     * Called by the silo the tick a slug enters. Emits synchronously and forces the
     * neighbour notify, so the rising edge is ALWAYS the slug tick regardless of
     * tile-entity tick order - player timing calibrations survive chunk reloads.
     */
    public void pulse(int ticks) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide()) return;
        pulseUntil = base.getWorld()
            .getTotalWorldTime() + ticks;
        for (final ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            base.setInternalOutputRedstoneSignal(side, (byte) 15);
        }
        if (base instanceof gregtech.api.metatileentity.CommonBaseMetaTileEntity) {
            ((gregtech.api.metatileentity.CommonBaseMetaTileEntity) base).doBlockUpdateServer();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        final byte strength = aBaseMetaTileEntity.getWorld()
            .getTotalWorldTime() < pulseUntil ? (byte) 15 : 0;
        for (final ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            aBaseMetaTileEntity.setInternalOutputRedstoneSignal(side, strength);
        }
    }

    @Override
    public boolean allowGeneralRedstoneOutput() {
        return true;
    }
}
