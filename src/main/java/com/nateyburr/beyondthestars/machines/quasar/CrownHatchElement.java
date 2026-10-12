package com.nateyburr.beyondthestars.machines.quasar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * P4: the Burst Thermaliser Crown's private socket (trap 3): {@code r}, a GRB Receptor Port on the base, through which
 * a conduit can feed a remote crown (quasar-p4.md §2, §7).
 */
public enum CrownHatchElement implements IHatchElement<MTEBurstThermaliserCrown> {

    Receptor(MTEBurstThermaliserCrown::addReceptor, MTEHatchGRBReceptor.class) {

        @Override
        public long count(MTEBurstThermaliserCrown t) {
            return t.receptorCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEBurstThermaliserCrown> adder;

    @SafeVarargs
    CrownHatchElement(IGTHatchAdder<MTEBurstThermaliserCrown> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEBurstThermaliserCrown> adder() {
        return adder;
    }
}
