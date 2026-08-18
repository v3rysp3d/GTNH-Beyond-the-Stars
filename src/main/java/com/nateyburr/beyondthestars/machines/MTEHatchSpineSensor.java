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
 * Spine Sensor - the Spiral chamber's timing reference. The silo pulses it the tick a steam
 * slug enters the chamber (every 20 ticks while running); the hatch emits a full-strength
 * redstone pulse for 2 ticks on all sides. Lock your injection circuits to this signal:
 * the slug window is +/-2 ticks around the pulse.
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
            new String[] { "Emits a 2-tick redstone pulse when a steam slug enters the Spiral",
                "One slug every 20 ticks while the reactor runs - lock your timing circuit to it",
                "The injection window is +/-2 ticks around the pulse" });
    }

    public MTEHatchSpineSensor(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchSpineSensor(mName, mTier, mDescriptionArray, mTextures);
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
