package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: a transition's hooks into the field second (d49-build-plan.md §3.1, §7). The defaults are
 * the plain tier rules; {@link #NONE} is used when no transition runs. A transition overrides what it changes:
 * extra drift d', parameter ramps (Shell Ignition's Θ), forced events, and its own per-second state machine in
 * {@link #tick}.
 */
public interface Overlay {

    /** No transition: the tier's own rules. */
    Overlay NONE = new Overlay() {

        @Override
        public boolean running() {
            return false;
        }
    };

    /** True for a real transition (the prototype's {@code S.trans}); false only for {@link #NONE}. */
    default boolean running() {
        return true;
    }

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

    /** True when Helium trims cut the yield (T2's ash). Shell Ignition runs T2 with the ash off (plan R-4). */
    default boolean ash(TierParams t) {
        return t.ash;
    }

    /** Drift d (before d'). */
    default double[] drift(TierParams t, StepInputs in) {
        return t.drift(in.feed, in.r);
    }

    /**
     * Drift d with the field state (what {@link StellarField#step} calls). T1-T6 use {@link #drift(TierParams,
     * StepInputs)}; the P4 burst-driven tiers (T7/T8, planets mode) use {@link TierParams#burstDrift}, which reads the
     * precession sense from the state. A P4 transition overrides this one.
     */
    default double[] drift(TierParams t, FieldState s, StepInputs in) {
        return t.burstDriven() ? t.burstDrift(s, in, 1) : drift(t, in);
    }

    /** Noise half-width σ on gauge k (P4: per gauge, and +0.25 on P per companion). */
    default double sigma(TierParams t, int k, StepInputs in) {
        return t.sigma(k, in.companions);
    }

    /**
     * P4: the transition's forced event terms E (pericentre passes) added after the tier's own event, and its
     * event-channel warning (0 for none). Runs after {@link #addDrift}, before the satellite crossing.
     */
    default int addEvents(FieldState s, StepInputs in, double[] E, double q, StepResult out) {
        return 0;
    }

    /** Adds the transition's extra drift d' (after the upkeep sag and the T5 plateau). */
    default void addDrift(FieldState s, StepInputs in, double[] dp) {}

    /**
     * T6: the shield charge per second the paid Agent upkeep adds (× r; ruling 12, B-60). The Magnetar Merger's imprint
     * returns 0: at the plain rate the upkeep's shield ate the dynamo's Π every second and the imprint could never
     * reach its band (B-71).
     */
    default double shieldPerUpkeep(TierParams t) {
        return StellarField.SHIELD_PER_UPKEEP;
    }

    /**
     * Runs after the Integrity update, before the cycle accounting (prototype order). May set {@code out.fail},
     * {@code out.supernova}, {@code out.relit}, {@code out.transitionSuccess}, and may change {@code s.tier}.
     */
    default void tick(FieldState s, int zone, StepInputs in, StepResult out) {}
}
