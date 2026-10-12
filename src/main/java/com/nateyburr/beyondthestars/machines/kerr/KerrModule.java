package com.nateyburr.beyondthestars.machines.kerr;

import java.util.ArrayList;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;

/**
 * SINGULARITY LADDER P2: one optional module arc of the Kerr Ergosphere Engine
 * (singularity-ladder-build.md "Contract additions (Kerr)", "Adding a Kerr module"). Holds what the
 * walk needs to know about the arc (its piece, its core block) and what the walk found (the core
 * count, whether it formed, why not, and the arc's PRIVATE recipe I/O). The controller walks every
 * module the same way and binds only the active mode's module into GT's standard hatch lists (trap 3).
 *
 * <h2>Adding a module</h2>
 * <ol>
 * <li><b>Shape:</b> tag the new arc in {@code tools/export-structures.js} (a {@code tag("kerr_x")}
 * anchor), give its sockets and core cell chars in the socket table and the legend, and re-export.</li>
 * <li><b>Core block:</b> a new {@code slCasing2} meta with texture, lang and recipe.</li>
 * <li><b>Sockets:</b> two {@link KerrHatchElement} entries ({@code XInput} / {@code XOutput}) whose
 * adders call {@link #addInput} / {@link #addOutput} on the new module.</li>
 * <li><b>Controller:</b> one {@code KerrModule} field plus its entry in {@code modules}; the piece's
 * {@code addShape} and its socket/core {@code addElement}s; a {@code MODE_X} constant with a case in
 * {@code moduleFor}, {@code getRecipeMap}, {@code findRecipeMatches} and {@code needsFor}; the map in
 * {@code getAvailableRecipeMaps} (NEI's catalyst index reads it).</li>
 * <li><b>Recipe map:</b> GT's own map when one fits; a Kerr-only map in {@link KerrRecipeMaps} only if
 * the module makes something GT has no machine for.</li>
 * </ol>
 * Walking, survival build, holograms, the fault string and the status lines then follow on their own.
 */
final class KerrModule {

    /** Shown in the scanner and the panel: "Compressor module: FORMED". */
    final String label;
    /** The optional piece name in the controller's structure definition. */
    final String piece;
    /** The shape char of the module's core block (its manifest socket locates the cell). */
    final char coreChar;
    /** The core block's {@code slCasing2} meta. */
    final int coreMeta;
    /** False for an item-only module: its sockets refuse fluid hatches. */
    final boolean fluids;

    // ---------------------------------------------------------------- walk state (resetWalk)
    int cores;
    boolean built;
    String fault = "";
    final ArrayList<MTEHatchInputBus> inBuses = new ArrayList<>();
    final ArrayList<MTEHatchInput> inHatches = new ArrayList<>();
    final ArrayList<MTEHatchOutputBus> outBuses = new ArrayList<>();
    final ArrayList<MTEHatchOutput> outHatches = new ArrayList<>();

    KerrModule(String label, String piece, char coreChar, int coreMeta, boolean fluids) {
        this.label = label;
        this.piece = piece;
        this.coreChar = coreChar;
        this.coreMeta = coreMeta;
        this.fluids = fluids;
    }

    void resetWalk() {
        cores = 0;
        built = false;
        fault = "";
        clearIO();
    }

    void clearIO() {
        inBuses.clear();
        inHatches.clear();
        outBuses.clear();
        outHatches.clear();
    }

    int inputCount() {
        return MTEKerrEngine.liveCount(inBuses) + MTEKerrEngine.liveCount(inHatches);
    }

    int outputCount() {
        return MTEKerrEngine.liveCount(outBuses) + MTEKerrEngine.liveCount(outHatches);
    }

    /** Hatch adder for the module's input sockets: an input bus, or (fluid modules) an input hatch. */
    boolean addInput(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInputBus) {
            ((MTEHatchInputBus) mte).updateTexture(casingIndex);
            inBuses.add((MTEHatchInputBus) mte);
            return true;
        }
        if (fluids && mte instanceof MTEHatchInput) {
            ((MTEHatchInput) mte).updateTexture(casingIndex);
            inHatches.add((MTEHatchInput) mte);
            return true;
        }
        return false;
    }

    /** Hatch adder for the module's output sockets: an output bus, or (fluid modules) an output hatch. */
    boolean addOutput(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchOutputBus) {
            ((MTEHatchOutputBus) mte).updateTexture(casingIndex);
            outBuses.add((MTEHatchOutputBus) mte);
            return true;
        }
        if (fluids && mte instanceof MTEHatchOutput) {
            ((MTEHatchOutput) mte).updateTexture(casingIndex);
            outHatches.add((MTEHatchOutput) mte);
            return true;
        }
        return false;
    }
}
