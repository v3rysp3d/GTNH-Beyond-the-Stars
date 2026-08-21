package com.nateyburr.beyondthestars.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.IGTHatchAdder;

/**
 * D-17 / D-22: the sockets of the two PROCESS modules - the Extraction Derrick's product bus
 * and the Enrichment Annex's feed bus, acid hatch and pellet bus. Four dedicated sockets, on
 * the buildings that consume and produce through them, exactly as POINT OF USE (D-20) asks.
 * <p>
 * <b>None of them joins a plant-wide list.</b> That is the whole reason they exist as their
 * own hatch elements instead of {@code HatchElement.InputBus} / {@code OutputBus} /
 * {@code InputHatch}, and it is a correctness property, not tidiness:
 * <ul>
 * <li>{@code mMaintenanceHatches}, {@code mInputHatches}, {@code mOutputBusses} and friends
 * have no notion of WHICH PIECE registered a hatch, so an Annex acid hatch sitting in
 * {@code mInputHatches} would satisfy the SILO's {@code checkHasInputHatch} demand and let a
 * plant form with no feedwater socket at all - the exact class of bug the Cooling Towers' ring
 * hatch was un-registered to avoid;</li>
 * <li>and in the other direction, a shared list means shared CONTENTS: the reactor's blanket
 * item draws (rod distribution, the assembly fallback) would happily eat the Annex's uraninite,
 * and its depleted rods would land in the Derrick's product bus.</li>
 * </ul>
 * So each socket keeps a dedicated field on the controller, each module reads and writes only
 * its own, and the plant-wide demands cannot see any of them.
 */
public enum ModuleHatchElement implements IHatchElement<MTEReactorCoreMk1> {

    /** Extraction Derrick: the ONE bus its mined uraninite/thorium/tantalum leaves through. */
    DerrickBus(MTEReactorCoreMk1::addDerrickBusToMachineList, MTEHatchOutputBus.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getDerrickBusCount();
        }
    },
    /** Enrichment Annex: the bus its uraninite feedstock is drawn from. */
    AnnexFeed(MTEReactorCoreMk1::addAnnexFeedBusToMachineList, MTEHatchInputBus.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getAnnexFeedBusCount();
        }
    },
    /** Enrichment Annex: the hatch its hydrofluoric acid is drawn from. */
    AnnexAcid(MTEReactorCoreMk1::addAnnexAcidHatchToMachineList, MTEHatchInput.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getAnnexAcidHatchCount();
        }
    },
    /** Enrichment Annex: the bus its fuel pellets and thorium tails leave through. */
    AnnexProduct(MTEReactorCoreMk1::addAnnexProductBusToMachineList, MTEHatchOutputBus.class) {

        @Override
        public long count(MTEReactorCoreMk1 t) {
            return t.getAnnexProductBusCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEReactorCoreMk1> adder;

    @SafeVarargs
    ModuleHatchElement(IGTHatchAdder<MTEReactorCoreMk1> adder, Class<? extends IMetaTileEntity>... mteClasses) {
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
