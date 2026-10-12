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
 * TC-2 control-block sockets: the SILO CONTROL BLOCK in the silo face above the controller,
 * and the TURBINE CONTROL BLOCK in the output manifold's back face. One of each, registered on
 * the Nuclear Plant Foundation like any other hatch.
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
