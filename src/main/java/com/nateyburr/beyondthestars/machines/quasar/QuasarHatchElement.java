package com.nateyburr.beyondthestars.machines.quasar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.IGTHatchAdder;

/**
 * P4: the Quasar Engine's PRIVATE sockets (quasar-p4.md §1.5, §7; singularity-ladder-build.md P4 "Chars → elements").
 * <p>
 * <b>Trap 3 (plant-wide hatch lists).</b> None of these registers into a GT list during the walk: the quasar runs no
 * recipes, and each socket kind has its own list on the controller, so the gas feed can never be mistaken for a trim
 * and an ME stocking hatch can never sit in a trim socket.
 */
public enum QuasarHatchElement implements IHatchElement<MTEQuasarEngine> {

    /** {@code h} x24, the Rim Gas Injectors: any input hatch, ME stocking included (buffered feed). At least 1. */
    Gas(MTEQuasarEngine::addGasHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEQuasarEngine t) {
            return t.gasHatchCount();
        }
    },
    /** {@code t} x4, the wall-tank trim outputs: a PLAIN input hatch only (no ME stocking: D-49's trim rule). */
    Trim(MTEQuasarEngine::addTrimHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEQuasarEngine t) {
            return t.trimHatchCount();
        }
    },
    /** {@code c} x4, the cap rims: Burst Control Hatch (purge on a rising edge, or b mode). */
    BurstControl(MTEQuasarEngine::addBurstControlHatch, MTEHatchBurstControl.class) {

        @Override
        public long count(MTEQuasarEngine t) {
            return t.burstControlCount();
        }
    },
    /** {@code n} x2, the control box: Stellar Field Sensor Hatch on the quasar's channels. */
    Sensor(MTEQuasarEngine::addSensorHatch, MTEHatchStellarFieldSensor.class) {

        @Override
        public long count(MTEQuasarEngine t) {
            return t.sensorHatchCount();
        }
    },
    /** {@code d} x2, the control box: Directive Hatch, the Disc set. */
    Directive(MTEQuasarEngine::addDirectiveHatch, MTEHatchDirective.class) {

        @Override
        public long count(MTEQuasarEngine t) {
            return t.directiveHatchCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEQuasarEngine> adder;

    @SafeVarargs
    QuasarHatchElement(IGTHatchAdder<MTEQuasarEngine> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEQuasarEngine> adder() {
        return adder;
    }
}
