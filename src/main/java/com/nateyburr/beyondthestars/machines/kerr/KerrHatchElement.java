package com.nateyburr.beyondthestars.machines.kerr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleUtility;

/**
 * SINGULARITY LADDER P2 / KERR REDESIGN: the Kerr Ergosphere Engine's PRIVATE sockets
 * (singularity-ladder-build.md §3.1, docs/kerr-redesign.md §1).
 * <p>
 * <b>Trap 3 (plant-wide hatch lists).</b> Neither registers into a GT list during the walk; each has
 * its own list on the controller:
 * <ul>
 * <li>the governor's <b>bolt buses</b> on the cage: the bolt and seed feed;</li>
 * <li>the BHC's own <b>utility hatch</b>, which the KEE drives from its hole state (Q-21).</li>
 * </ul>
 * The modules' sockets are gone from here: since the redesign every module is its own machine and its
 * hatches go into ITS standard GT lists.
 */
public enum KerrHatchElement implements IHatchElement<MTEKerrEngine> {

    /** Cage: the governor's bolt and seed feed. Never joins a recipe list. */
    BoltBus(MTEKerrEngine::addBoltBus, MTEHatchInputBus.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.boltBusCount();
        }
    },
    /** Cage, optional (AUTOMATION PASS): a Directive Hatch (MTE 28127) in a utility socket. */
    Directive(MTEKerrEngine::addDirectiveHatch, MTEHatchDirective.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.directiveHatchCount();
        }
    },
    /**
     * Cage, optional (P4, quasar-p4.md §3.1): a GRB Receptor Port (MTE 28125) in a utility socket, through which a
     * quasar stabilises the hole (bleed, the T2 setpoint cap, the Superdense refinery).
     */
    Receptor(MTEKerrEngine::addReceptor, MTEHatchGRBReceptor.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.receptorCount();
        }
    },
    /** Cage, optional: the Black Hole Compressor's utility hatch, redstone on while the hole is open. */
    Utility(MTEKerrEngine::addUtilityHatch, MTEBlackHoleUtility.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.utilityHatchCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEKerrEngine> adder;

    @SafeVarargs
    KerrHatchElement(IGTHatchAdder<MTEKerrEngine> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEKerrEngine> adder() {
        return adder;
    }
}
