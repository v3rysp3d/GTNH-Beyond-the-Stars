package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: a transition's hooks into the field second (d49-build-plan.md §3.1, §7). The defaults are
 * the plain tier rules; {@link #NONE} is used when no transition runs. A transition overrides what it changes:
 * extra drift d', parameter ramps (Shell Ignition's Θ), forced events, and its own per-second state machine in
 * {@link #tick}.
 */
public interface Overlay {

    /** No transition: the tier's own rules. */
    Overlay NONE = new Overlay() {};

    /** The event that runs this second (the tier's, a forced one, or null). */
    default EventDef event(TierParams t, FieldState s) {
        return t.event;
    }

    /** The gauges updated this second, ascending. */
    default int[] gauges(TierParams t) {
        return t.gauges();
    }

    /** Feedback gain on gauge k. */
    default double gain(TierParams t, int k) {
        return t.gain(k);
    }

    /** Coupling c_k(x). */
    default double coupling(TierParams t, double[] x, int k) {
        return t.coupling(x, k);
    }

    /** Drift d (before d'). */
    default double[] drift(TierParams t, StepInputs in) {
        return t.drift(in.feed, in.r);
    }

    /** Adds the transition's extra drift d' (after the upkeep sag and the T5 plateau). */
    default void addDrift(FieldState s, StepInputs in, double[] dp) {}

    /**
     * Runs after the Integrity update, before the cycle accounting (prototype order). May set {@code out.fail},
     * {@code out.supernova}, {@code out.relit}, {@code out.transitionSuccess}, and may change {@code s.tier}.
     */
    default void tick(FieldState s, int zone, StepInputs in, StepResult out) {}
}
