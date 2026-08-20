package com.nateyburr.beyondthestars.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.IGTHatchAdder;

/**
 * The plant's three DISCRIMINATED fluid sockets. All three take an ordinary Input Hatch and all
 * three land in {@code mInputHatches} like any other, but the plant has to know which is which:
 * the SILO's face carries the reactor's feedwater, and the SPIRAL CHAMBER's front wall carries
 * both its own coolant and the bulk propellant line the controller stocks the eight Precision
 * Injectors from. A blanket {@code depleteInput()} across the shared pool would let the reactor
 * drink the chamber's coolant, so each socket char gets its own adder and its own list - the
 * same pattern the control-block and injector sockets already use.
 * <p>
 * D-20 moved these sockets onto the buildings they serve; the discrimination is what makes that
 * safe, and it is unchanged by the move. The plant's other sockets (4 Energy on the manifold, 3
 * Maintenance, the Output Hatch, the Rod Input Bus and the Depleted Output Bus) need no
 * discrimination and use GT's stock {@code HatchElement} entries.
 */
public enum FluidHatchElement implements IHatchElement<MTEReactorCoreMk1> {

    Feedwater(MTEReactorCoreMk1::addFeedwaterHatchToMachineList, MTEHatchInput.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getFeedwaterHatchCount();
        }
    },
    Coolant(MTEReactorCoreMk1::addChamberCoolantHatchToMachineList, MTEHatchInput.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getChamberCoolantHatchCount();
        }
    },
    Propellant(MTEReactorCoreMk1::addPropellantHatchToMachineList, MTEHatchInput.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getPropellantHatchCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEReactorCoreMk1> adder;

    @SafeVarargs
    FluidHatchElement(IGTHatchAdder<MTEReactorCoreMk1> adder, Class<? extends IMetaTileEntity>... mteClasses) {
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
