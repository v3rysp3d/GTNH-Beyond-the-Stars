package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluids;

import gregtech.api.enums.Materials;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gtPlusPlus.core.fluids.GTPPFluids;

/**
 * Precision Injector - a tiny propellant port in the Spiral chamber wall. Every litre pushed
 * in is stamped with its arrival tick; the silo evaluates each steam slug's +/-2 tick window
 * and wants EXACTLY 1L per injector inside it. The 16L capacity is deliberate: this is a
 * timing instrument, not a tank.
 */
public class MTEHatchPrecisionInjector extends MTEHatchInput {

    /** Fill events since the last slug evaluation: {worldTick, litres}. */
    private final List<long[]> events = new ArrayList<>();

    public MTEHatchPrecisionInjector(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            4,
            new String[] { "Feeds the Spiral chamber 1L of propellant at a time",
                "Every litre is stamped with its arrival tick - the slug window wants EXACTLY 1L",
                "Mistimed or excess litres are wasted and heat the chamber", "Capacity: 16L - by design" });
    }

    public MTEHatchPrecisionInjector(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchPrecisionInjector(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public int getCapacity() {
        return 16;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        if (aFluid == null || aFluid.getFluid() == null) return false;
        return aFluid.getFluid() == Materials.Benzene.mFluid || aFluid.getFluid() == Materials.GasolinePremium.mFluid
            || aFluid.getFluid() == GTPPFluids.H8N4C2O4RocketFuel
            || aFluid.getFluid() == BTSFluids.naqPropellantT1
            || aFluid.getFluid() == BTSFluids.naqPropellantT2
            || aFluid.getFluid() == BTSFluids.naqPropellantT3
            || aFluid.getFluid() == BTSFluids.naqPropellantT4
            || aFluid.getFluid() == BTSFluids.naqPropellantT5
            || aFluid.getFluid() == BTSFluids.naqPropellantT6;
    }

    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        final int filled = super.fill(aFluid, doFill);
        if (doFill && filled > 0 && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            events.add(
                new long[] { getBaseMetaTileEntity().getWorld()
                    .getTotalWorldTime(), filled });
            if (events.size() > 64) {
                events.remove(0);
            }
        }
        return filled;
    }

    /** Drains and returns all fill events recorded since the last call. */
    public List<long[]> drainEvents() {
        final List<long[]> out = new ArrayList<>(events);
        events.clear();
        return out;
    }

    /** Removes the given litres from the stored propellant (used or voided by the silo). */
    public void consumeLitres(int litres) {
        if (mFluid != null) {
            mFluid.amount -= litres;
            if (mFluid.amount <= 0) {
                mFluid = null;
            }
        }
    }

    /** The propellant currently in the port, or null. */
    public FluidStack storedPropellant() {
        return mFluid;
    }

    /** Litres of propellant standing in the port. */
    public int storedLitres() {
        return mFluid == null ? 0 : mFluid.amount;
    }

    /**
     * TEST AID (the controller's "infinite supplies" debug flag): tops the port's reserve back
     * up WITHOUT stamping a timing event. Going through {@link #fill} would stamp every
     * refilled litre with the current tick, and the slug scorer would read those as a bucket
     * dumped into the window - eight wasted litres and a heat spike per cycle. The reserve is
     * inert until {@link #debugTimedShot} stamps exactly one litre of it.
     */
    public void debugRefill(FluidStack propellant) {
        if (propellant == null || propellant.getFluid() == null) return;
        // Exactly ONE litre - the amount a cycle can actually shoot. Topping the port up to
        // its full capacity turned the bench supply into a pump-out tap for the pack's best
        // propellant.
        if (mFluid == null || !mFluid.isFluidEqual(propellant)) {
            mFluid = new FluidStack(propellant.getFluid(), 1);
        } else if (mFluid.amount < 1) {
            mFluid.amount = 1;
        }
    }

    /**
     * TEST AID: stamps exactly 1L at the current tick - what a perfectly timed operator does
     * inside a cycle's +/-2t window. The litre is scored (and consumed) by the controller like
     * any other injection; the reserve behind it comes from {@link #debugRefill}.
     */
    public boolean debugTimedShot() {
        if (mFluid == null || mFluid.amount < 1) return false;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide()) return false;
        events.add(
            new long[] { base.getWorld()
                .getTotalWorldTime(), 1 });
        return true;
    }
}
