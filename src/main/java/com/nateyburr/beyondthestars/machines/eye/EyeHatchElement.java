package com.nateyburr.beyondthestars.machines.eye;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;

/**
 * SINGULARITY LADDER P3: the Eye of Harmony's sockets (singularity-ladder-build.md §3.3, "Structure
 * export" sockets table). Owned by the Eye unit.
 * <p>
 * <b>Trap 3.</b> The input sockets never join GT's {@code mInputHatches}: they register into the
 * Eye's own lists and are drained every second into {@code long} buffers, the way TecTech's EOH
 * drains its hatches. A band's sockets are dropped again if the band's walk fails
 * (mark-and-truncate), so a half-built band feeds nothing. The ME outputs are the Eye's ONLY output
 * path, so they register into GT's {@code mOutputBusses} / {@code mOutputHatches}, which GT's
 * ejection helpers read.
 */
public enum EyeHatchElement implements IHatchElement<MTEEyeOfHarmonyBTS> {

    /** Plinth gas sockets (any input hatch, ME stocking included): H and He, or any Eye input. */
    Gas(MTEEyeOfHarmonyBTS::addGasHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.gasHatchCount();
        }
    },
    /** Beam-band and jet-crescent sockets: the stellar fluids (RSPM, ESPM, HSM, WDM, coolant). */
    Stellar(MTEEyeOfHarmonyBTS::addStellarHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.stellarHatchCount();
        }
    },
    /** Plinth output sockets: ME output bus and ME output hatch only (long-sized outputs). */
    Output(MTEEyeOfHarmonyBTS::addOutputME, MTEHatchOutputBusME.class, MTEHatchOutputME.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.outputBusCount() + t.outputHatchCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEEyeOfHarmonyBTS> adder;

    @SafeVarargs
    EyeHatchElement(IGTHatchAdder<MTEEyeOfHarmonyBTS> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEEyeOfHarmonyBTS> adder() {
        return adder;
    }
}
