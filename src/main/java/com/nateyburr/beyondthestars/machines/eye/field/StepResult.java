package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: what one field second produced (d49-build-plan.md §4). The controller applies it in this
 * order: transition materials ({@link #bounced}), {@link #fail} / {@link #supernova}, {@link #relit} (charge the new
 * cycle), {@link #transitionSuccess}, {@link #hsmToDraw}, the cycle ({@link #cycleEnded}, then charge the next
 * cycle), then the batch ({@link #batchDone}, then start the next batch).
 */
public final class StepResult {

    /** 0 Resonant, 1 Stable, 2 Strained, 3 Critical, 4 Breach. */
    public int zone;
    /** Worst |error| across the active gauges. */
    public double worst;
    /** {@code clamp(100 - worst, 0, 100)}. */
    public double stability;
    /** Yield this second. */
    public double Y;
    /** The gauge with the worst error. */
    public int worstGauge;
    /** Errors per gauge this second (0 for inactive gauges). */
    public double[] err;
    /** Event-channel warning: 0, +1 or -1 (T5: +1 ahead of the plateau). */
    public int warn;
    /** T5 setpoint this second (NaN on other tiers). */
    public double sp = Double.NaN;
    /** The dial used this second. */
    public double m;

    /** An event warning was announced this second. */
    public boolean eventWarned;
    /** An event started hitting this second. */
    public boolean eventHit;
    /** T5: the plateau warning was announced this second. */
    public boolean plateauWarned;

    /** Non-null: the star failed (Breach, Integrity 0, or a transition failure). The caller runs its failure rule. */
    public String fail;

    /** The cycle completed this second: debit EU and take upkeep for the next one (it sets the next r). */
    public boolean cycleEnded;
    /** True when {@link #cycleProduct} is valid (non-batch tiers, at a cycle end). */
    public boolean hasCycleProduct;
    /** The completed cycle's product, litres. */
    public double cycleProduct;
    /** T4: HSM fed this second, litres, to draw from the buffer. */
    public double hsmToDraw;

    /** T5: the batch completed. Emit {@link #batchProduct}, then start the next batch (or go IDLE). */
    public boolean batchDone;
    public double batchMeanY;
    public double batchProduct;

    // ---- transitions (set by an Overlay)
    /** Time fluid dosed by the transition this second (Core Collapse phase 1), litres. */
    public double timeDosed;
    /** The Core Collapse bounced this second; take {@link #spaceTimeTaken} and {@link #hsmSeedTaken}. */
    public boolean bounced;
    public double spaceTimeTaken;
    public double hsmSeedTaken;
    /** The bounce ended in a supernova: the star is lost (gauges already zeroed); the caller goes DARK. */
    public boolean supernova;
    public String supernovaReason;
    /** {@link StellarField#light} ran mid-step (the bounce lit the proto-neutron star): charge its first cycle. */
    public boolean relit;
    /** The transition completed this second. */
    public boolean transitionSuccess;
}
