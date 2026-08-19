package com.nateyburr.beyondthestars.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * TC-2 control-block sockets: the SILO CONTROL BLOCK in the silo face above the main
 * station, and the TURBINE CONTROL BLOCK in the rotor gallery's front wall. One of each,
 * registered on the main station like any other hatch.
 */
public enum PanelHatchElement implements IHatchElement<MTEReactorCoreMk1> {

    SiloPanel(MTEReactorCoreMk1::addSiloPanelToMachineList, MTESiloControlBlock.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.siloPanel == null ? 0 : 1;
        }
    },
    TurbinePanel(MTEReactorCoreMk1::addTurbinePanelToMachineList, MTETurbineControlBlock.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.turbinePanel == null ? 0 : 1;
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEReactorCoreMk1> adder;

    @SafeVarargs
    PanelHatchElement(IGTHatchAdder<MTEReactorCoreMk1> adder, Class<? extends IMetaTileEntity>... mteClasses) {
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
}
