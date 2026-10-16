package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.IGTHatchAdder;

/**
 * P4: the conduit elements' PRIVATE sockets (quasar-p4.md §7; singularity-ladder-build.md P4 "Conduit" chars). Trap 3:
 * nothing here joins a GT plant-wide list (the elements run no recipes), so a fuel hatch is only ever read for throat
 * upkeep and a Burst Control Hatch only for the Junction's split.
 */
public enum ConduitHatchElement implements IHatchElement<MTEConduitElement<?>> {

    /** {@code u}: the throat fuel (Molten SpaceTime or Ergomatter): any input hatch, ME stocking included. */
    Fuel(MTEConduitElement::addFuelHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEConduitElement<?> t) {
            return t.fuelHatchCount();
        }
    },
    /** {@code c} (Junction only, 2 cells): the split's analog level s; the second is spare. */
    BurstControl(MTEConduitElement::addBurstControlHatch, MTEHatchBurstControl.class) {

        @Override
        public long count(MTEConduitElement<?> t) {
            return t.burstControlCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEConduitElement<?>> adder;

    @SafeVarargs
    ConduitHatchElement(IGTHatchAdder<MTEConduitElement<?>> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    /**
     * B-249: the name GT's structure errors and the NEI hatch list print. GT's default key {@code hatch_type_<name>}
     * has no translation, so a generic error read "hatch_type_fuel".
     */
    @Override
    public String getDescriptionLangKey() {
        return "beyondthestars.hatch.conduit." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getDisplayName() {
        return StatCollector.translateToLocal(getDescriptionLangKey());
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEConduitElement<?>> adder() {
        return adder;
    }
}
