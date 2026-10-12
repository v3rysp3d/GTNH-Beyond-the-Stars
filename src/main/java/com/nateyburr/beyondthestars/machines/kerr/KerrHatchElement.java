package com.nateyburr.beyondthestars.machines.kerr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleUtility;

/**
 * SINGULARITY LADDER P2: the Kerr Ergosphere Engine's PRIVATE sockets (singularity-ladder-build.md
 * §3.1, the {@code ModuleHatchElement} pattern). Owned by the Kerr unit.
 * <p>
 * <b>Trap 3 (plant-wide hatch lists).</b> None of these registers into a GT list during the walk.
 * Each socket kind has its own list on the controller, keyed by the piece that owns it:
 * <ul>
 * <li>the governor's <b>bolt buses</b> on the cage: recipes can never eat a bolt or a seed, and the
 * governor can never eat a recipe input;</li>
 * <li>the <b>compressor module's</b> and the <b>extractor module's</b> I/O: a module that is not
 * standing cannot leave a hatch registered, and the two modules never share inputs. Only the active
 * mode's module is bound into GT's standard lists ({@code mInputBusses} and friends), just before a
 * recipe search (see {@code MTEKerrEngine.bindModuleIO});</li>
 * <li>the BHC's own <b>utility hatch</b>, which the Kerr drives from its hole state (Q-21).</li>
 * </ul>
 */
public enum KerrHatchElement implements IHatchElement<MTEKerrEngine> {

    /** Cage: the governor's bolt and seed feed. Never joins a recipe list. */
    BoltBus(MTEKerrEngine::addBoltBus, MTEHatchInputBus.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.boltBusCount();
        }
    },
    /** Cage, optional: the Black Hole Compressor's utility hatch, redstone on while the hole is open. */
    Utility(MTEKerrEngine::addUtilityHatch, MTEBlackHoleUtility.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.utilityHatchCount();
        }
    },
    /** Compressor arc: recipe inputs (item buses and fluid hatches). */
    CompressorInput(MTEKerrEngine::addCompressorInput, MTEHatchInputBus.class, MTEHatchInput.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.compressorInputCount();
        }
    },
    /** Compressor arc: recipe outputs (item buses and fluid hatches). */
    CompressorOutput(MTEKerrEngine::addCompressorOutput, MTEHatchOutputBus.class, MTEHatchOutput.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.compressorOutputCount();
        }
    },
    /** Extractor arc: recipe inputs. GT's extractor map is items only, so input buses only. */
    ExtractorInput(MTEKerrEngine::addExtractorInput, MTEHatchInputBus.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.extractorInputCount();
        }
    },
    /** Extractor arc: recipe outputs, output buses only. */
    ExtractorOutput(MTEKerrEngine::addExtractorOutput, MTEHatchOutputBus.class) {

        @Override
        public long count(MTEKerrEngine t) {
            return t.extractorOutputCount();
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
