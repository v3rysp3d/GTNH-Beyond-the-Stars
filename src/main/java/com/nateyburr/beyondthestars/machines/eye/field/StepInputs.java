package com.nateyburr.beyondthestars.machines.eye.field;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * D-49 stability field: what the controller supplies to one field second (d49-build-plan.md §4, "The controller
 * around step"). Reusable: call {@link #clearTrims()} and refill each second.
 */
public final class StepInputs {

    /** The time dial actually paid for this second (1 during a transition). */
    public double m = 1;
    /** Stabilisation: {@code 1 - 0.05·(ts - 1)}. */
    public double q = 1;
    /** This cycle's upkeep fraction. */
    public double r = 1;
    /** Parallel {@code 2^(installedTier - starTier)}. */
    public double p = 1;
    /** T4: HSM feed available this second, L/s per parallel (the setpoint when the buffer is full). */
    public double feed;
    /** The dial setpoint (for scheduling the first event when a star lights mid-step, e.g. at the bounce). */
    public double dialSetpoint = 1;
    /** Lowest Time Acceleration generator tier (1..9), for m_max. */
    public int tt = 1;
    // ---- P4 (T7/T8): the bursts delivered last second (the conduit solver's result), normalised by D·m·p
    /** β = G_eye / (D·m·p): every burst landing on this Eye's GRB Receptor Ports (halo test fire excluded). */
    public double beta;
    /** γ_hi, γ_lo, γ_halo by receptor group (rail-high, rail-low, halo). */
    public double gHi, gLo, gHalo;
    /** The bursts on the Eye's ports this second, Bu/t, unnormalised (the Magnetar Collapse's shock cancel). */
    public double gEye;
    /** T8: the Ergomatter feed f8 available this second, L/s per parallel (the setpoint when the buffer is full). */
    public double f8;
    /** Galaxy mode: companions running beside the galaxy (each adds +0.25 to the noise on P). */
    public int companions;

    /** Trim units drained this second, in drain order; each at most {@code cap · p}. */
    public final LinkedHashMap<TrimDef, Double> u = new LinkedHashMap<>();

    public StepInputs clearTrims() {
        u.clear();
        return this;
    }

    /** Units of the trim of that kind drained this second (0 when none). */
    public double units(TrimDef.Kind kind) {
        for (Map.Entry<TrimDef, Double> e : u.entrySet()) if (e.getKey().kind == kind) return e.getValue();
        return 0;
    }

    /** {@code q} for the lowest Stabilisation generator tier {@code ts} (1..9). */
    public static double qFor(int ts) {
        return 1 - 0.05 * (ts - 1);
    }
}
