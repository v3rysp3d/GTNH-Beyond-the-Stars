package com.nateyburr.beyondthestars.machines.quasar.engine;

/**
 * P4: what the Quasar Engine's controller supplies to one disc second (quasar-p4.md §1.3). Reusable.
 * <p>
 * The controller's second, in order: compute Ṁ_Edd ({@link QuasarParams#eddingtonFeed}) for the current mass and
 * gas; drain up to {@code f·Ṁ_Edd} litres of the feed gas from the Rim Gas Injectors (any input hatch, ME stocking
 * fine) into {@link #gasDrawn}; drain the coil power ({@link QuasarParams#coilEUt} at min(b, coil tier)) and set
 * {@link #powerOk}; read the trim hatches (plain input hatches only) into {@link #gasTrimUnits} /
 * {@link #spaceTrimUnits} (≤ 10 each; one unit = {@link QuasarParams#gasTrimUnitLitres} /
 * {@link QuasarParams#spaceTrimUnitLitres} litres); then {@link QuasarDisc#step}. A purge from a Burst Control Hatch
 * rising edge or OpenComputers is {@link QuasarState#requestPurge}, not an input.
 */
public final class QuasarInputs {

    public QuasarGas gas = QuasarGas.HE;
    /** Disc Field Coil tier 1..4 (the structure's {@code coil} channel). */
    public int coilTier = 3;
    /** The field setting b (GUI, or the Burst Control Hatch in b mode); the engine uses min(b, coil tier). */
    public double bSetpoint = 3;
    /** The coil power was paid this second (else b = 0: no jets, Φ frozen). */
    public boolean powerOk = true;
    /** Feed gas drawn this second, litres (the prototype: {@code f·Ṁ_Edd} when stocked, else 0). */
    public double gasDrawn;
    /** Trim units drained this second: the feed gas (E +2 each) and Spatially Enlarged Fluid (E −2 each), ≤ 10. */
    public double gasTrimUnits, spaceTrimUnits;
    /**
     * The regulator's purge this second, already evaluated (the prototype's automation: Φ (last second) ≥ the purge
     * setpoint and not held). In the mod this is a Directive Disc bit 3 → Burst Control Hatch rig, i.e. a
     * {@link QuasarState#requestPurge}; the golden harness uses this field.
     */
    public boolean regulatorPurge;
}
