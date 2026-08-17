package com.nateyburr.beyondthestars.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * Hatch element letting plant module CONTROLLERS dock into the Reactor Silo wall - the Space
 * Elevator's ProjectModuleElement pattern: the module controller is accepted like a hatch and
 * collected into the silo's module list.
 */
public enum PlantModuleElement implements IHatchElement<MTEReactorCoreMk1> {

    PlantModule(MTEReactorCoreMk1::addPlantModuleToMachineList, MTEPlantModuleBase.class) {

        @Override
        public long count(MTEReactorCoreMk1 silo) {
            return silo.plantModules.size();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEReactorCoreMk1> adder;

    @SafeVarargs
    PlantModuleElement(IGTHatchAdder<MTEReactorCoreMk1> adder, Class<? extends IMetaTileEntity>... mteClasses) {
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
