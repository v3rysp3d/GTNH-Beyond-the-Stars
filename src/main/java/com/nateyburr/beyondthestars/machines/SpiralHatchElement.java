package com.nateyburr.beyondthestars.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * Hatch elements of the Spiral chamber: up to eight Precision Injectors and one Spine
 * Sensor, registered on the main station like any other hatch.
 */
public enum SpiralHatchElement implements IHatchElement<MTEReactorCoreMk1> {

    Injector(MTEReactorCoreMk1::addInjectorToMachineList, MTEHatchPrecisionInjector.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.spiralInjectors.size();
        }
    },
    Sensor(MTEReactorCoreMk1::addSensorToMachineList, MTEHatchSpineSensor.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.spineSensor == null ? 0 : 1;
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEReactorCoreMk1> adder;

    @SafeVarargs
    SpiralHatchElement(IGTHatchAdder<MTEReactorCoreMk1> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEReactorCoreMk1> adder() {
        return adder;
    }

    /** Lang key of this socket's player-facing name (structure errors, NEI preview, hints). */
    @Override
    public String getDescriptionLangKey() {
        return "beyondthestars.hatch." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getDisplayName() {
        return StatCollector.translateToLocal(getDescriptionLangKey());
    }
}
