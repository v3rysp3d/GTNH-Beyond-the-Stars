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
    /** At a cycle end: the cycle's mean Y and its lowest r (companion products: base × parallel × mY × rMin). */
    public double cycleMeanY, cycleRMin;
    /** Galaxy mode: the companions produce this cycle (false during a transition, like the galaxy's own product). */
    public boolean companionCycle;
    /** T8: the Ergomatter feed ran this second; {@link #ergoToDraw} litres to draw (f8 · m · p). */
    public boolean ergoFed;
    public double ergoToDraw;

    // ---- P4 (T8) satellite crossing
    /** The 120 s countdown began / the crossing hit / it ended (caught or missed) this second. */
    public boolean crossingWarned, crossingHit, crossingCaught, crossingMissed;
    /** Caught: the bonus cycle of Superdense Ergomatter, litres (1.0 × f8 × 60 × r × p). */
    public double crossingBonus;

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
    /** A transition countdown began this second (the collapse in 30 s; a pericentre pass in 60 s). */
    public boolean countdown;
    /** Magnetar Collapse: the overfeed charge taken this second, litres (WDM 61.44 + BDM 15.36 in phase 1). */
    public double wdmCharged, bdmCharged;
    /** Galactic Merger: the Ergomatter charge taken this second, litres (5). */
    public double ergoCharged;
    /** Magnetar Collapse: the magnetar collapsed this second (the tier is now T7; halo capture begins). */
    public boolean collapsed;
    /** Galactic Merger: the first pass bound the second nucleus this second (the tier is now T8). */
    public boolean merged;
}
