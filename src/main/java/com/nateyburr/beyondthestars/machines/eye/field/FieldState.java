package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: the mutable state of one live star's field (d49-build-plan.md §3.1). A plain data holder;
 * {@link StellarField} does the maths, the runtime (unit 4) owns the state machine (DARK / IGNITING / LIT ...) and
 * saves every field here to NBT (plan §3.3). All doubles must be saved as doubles: the prototype's event clock
 * ({@link EventState#next}) is fractional.
 */
public final class FieldState {

    /** The star's tier. The Core Collapse bounce switches it from T3 to T4 mid-step. */
    public TierParams tier;
    /** Gauges P, Θ, Ω, Π (index {@link StellarField#P} ..), each in [-120, +120]. Inactive gauges keep their value. */
    public final double[] x = new double[4];
    /** T6 Agent shield, 0..100. */
    public double A;
    /** Integrity, 0..100. */
    public double I = 100;
    /** Harmonic Lock streak: consecutive real seconds in Resonant. */
    public int lock;
    /** Last second's errors per gauge (0 for inactive gauges), for sensors and rigs. */
    public double[] prevE = new double[4];
    /** Last second's event-channel warning: 0, +1 or -1. */
    public int prevWarn;
    /** The event timer, or null when the tier has no event. */
    public EventState ev;
    /** The cycle in progress. */
    public Cycle cyc = new Cycle(0);
    /** The T5 cooling batch in progress, or null. */
    public Batch batch;
    /** The field clock: +1 every second, DARK and IGNITING seconds included. */
    public long t;
    /** Eye-seconds lived at the current star (advances by m per lit second). */
    public double eyeT;
    /** The noise source; save {@link FieldRng#state()}. */
    public FieldRng rng;

    public FieldState(TierParams tier, FieldRng rng) {
        this.tier = tier;
        this.rng = rng;
    }

    /** Sets all four gauges. */
    public void setX(double p, double th, double om, double pi) {
        x[StellarField.P] = p;
        x[StellarField.TH] = th;
        x[StellarField.OM] = om;
        x[StellarField.PI] = pi;
    }

    /** The event timer (prototype {@code S.ev}). */
    public static final class EventState {

        /** Field-clock second of the next hit (fractional). */
        public double next;
        /** Hit seconds left; 0 when not hitting. */
        public int left;
        /** True from the warning until the last hit second. */
        public boolean warned;
        /** +1 or -1 (signed events only). */
        public int sign = 1;

        public EventState(double next) {
            this.next = next;
        }
    }

    /** The 60 Eye-second cycle accumulators (prototype {@code S.cyc}). */
    public static final class Cycle {

        public double eyeT;
        public double ySum;
        public int n;
        public double rMin = 1;
        /** T4: HSM fed this cycle, in litres. */
        public double hsm;

        public Cycle(double eyeT) {
            this.eyeT = eyeT;
        }
    }

    /** The T5 batch accumulators (prototype {@code S.batch}). */
    public static final class Batch {

        public double eyeT;
        public double ySum;
        public int n;
        public double rMin = 1;
        /** True once the plateau warning has been announced for this batch. */
        public boolean warned;
    }
}
