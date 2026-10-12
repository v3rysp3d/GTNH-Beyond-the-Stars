package com.nateyburr.beyondthestars.machines.eye;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
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
 * <p>
 * <b>D-49</b> (d49-build-plan.md §11.2): {@link #Field} (the stability-field trims, never buffered),
 * {@link #Sensor} (the Stellar Field Sensor Hatch) and {@link #Items} (input buses on the plinth gas
 * sockets, for the Stellar Ignition Charges). An ME stocking input hatch in a Field socket is walked,
 * so the Eye still forms, but it goes to a rejected list and is never drained (R-15).
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
    /**
     * D-49 Field sockets ({@code f}, core band): plain input hatches whose fluids are trims, drained per field
     * second by the star runtime and never buffered. ME stocking hatches register as rejected (R-15).
     */
    Field(MTEEyeOfHarmonyBTS::addFieldHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.fieldHatchCount() + t.rejectedFieldHatchCount();
        }
    },
    /** D-49 Sensor sockets ({@code n}, core band): Stellar Field Sensor Hatches (MTE 28116) only. */
    Sensor(MTEEyeOfHarmonyBTS::addSensorHatch, MTEHatchStellarFieldSensor.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.sensorHatchCount();
        }
    },
    /** AUTOMATION PASS: Directive Hatches (MTE 28127) also fit the Sensor sockets ({@code n}). */
    Directive(MTEEyeOfHarmonyBTS::addDirectiveHatch, MTEHatchDirective.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.directiveHatchCount();
        }
    },
    /** D-49 item input on the plinth gas sockets ({@code h}): input buses, ME input bus included (R-1). */
    Items(MTEEyeOfHarmonyBTS::addItemBus, MTEHatchInputBus.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.itemBusCount();
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
