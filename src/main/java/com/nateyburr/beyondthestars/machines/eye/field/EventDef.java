package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: a scripted field event (d49-build-plan.md §5, "Events"): the T3 helium flash, the T4 glitch
 * and the T6 giant flare; P4 adds the T7/T8 minor merger (W, signed, with the precession flip). Events run on the
 * Eye clock: the next one is scheduled {@code uni(every) / m} seconds after
 * the last hit. Their size is scaled by q and they are not time-scaled otherwise.
 */
public final class EventDef {

    public final String name;
    /** The interval [a, b] in Eye-seconds. */
    public final double everyMin;
    public final double everyMax;
    /** Warning lead in seconds (0 = no warning). */
    public final int warn;
    /** Duration in seconds; the effect applies every one of them. */
    public final int dur;
    /** Effect per second on P, Θ, Ω, Π, W, Λ. */
    private final double[] fx;
    /** True when each event draws a sign (+/-), announced with the warning. */
    public final boolean signed;
    /** P4 minor merger: when its last hit second ends, the precession sense s takes the event's sign. */
    public final boolean flip;

    public EventDef(String name, double everyMin, double everyMax, int warn, int dur, double p, double th, double om,
        double pi, boolean signed) {
        this.name = name;
        this.everyMin = everyMin;
        this.everyMax = everyMax;
        this.warn = warn;
        this.dur = dur;
        this.fx = new double[] { p, th, om, pi, 0, 0, 0, 0 };
        this.signed = signed;
        this.flip = false;
    }

    /** An event on any of the six gauges ({@code fx} indexed P, Θ, Ω, Π, W, Λ). */
    public EventDef(String name, double everyMin, double everyMax, int warn, int dur, double[] fx, boolean signed,
        boolean flip) {
        this.name = name;
        this.everyMin = everyMin;
        this.everyMax = everyMax;
        this.warn = warn;
        this.dur = dur;
        this.fx = new double[StellarField.GAUGES];
        System.arraycopy(fx, 0, this.fx, 0, Math.min(fx.length, StellarField.GAUGES));
        this.signed = signed;
        this.flip = flip;
    }

    /** Effect per second on gauge {@code k}. */
    public double fx(int k) {
        return fx[k];
    }
}
