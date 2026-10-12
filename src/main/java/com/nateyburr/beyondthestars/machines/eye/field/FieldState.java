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
    /**
     * Gauges P, Θ, Ω, Π, W, Λ (index {@link StellarField#P} ..), each in [-120, +120]. Inactive gauges keep their
     * value. W and Λ are the P4 galaxy gauges (T7/T8).
     */
    public final double[] x = new double[StellarField.GAUGES];
    /** T6 Agent shield, 0..100. */
    public double A;
    /** Integrity, 0..100. */
    public double I = 100;
    /** Harmonic Lock streak: consecutive real seconds in Resonant. */
    public int lock;
    /** Last second's errors per gauge (0 for inactive gauges), for sensors and rigs. */
    public double[] prevE = new double[StellarField.GAUGES];
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
    /** P4 (T7/T8): the rail's precession sense s, +1 or -1; a minor merger flips it to its sign. Save it. */
    public int prec = 1;
    /** P4 (T8): the satellite-crossing timer, or null (T7, planets mode, the Galactic Merger). Save it. */
    public Crossing tde;

    public FieldState(TierParams tier, FieldRng rng) {
        this.tier = tier;
        this.rng = rng;
    }

    /** Sets the four stellar gauges; W and Λ go to 0 (every prototype reset writes all six). */
    public void setX(double p, double th, double om, double pi) {
        x[StellarField.P] = p;
        x[StellarField.TH] = th;
        x[StellarField.OM] = om;
        x[StellarField.PI] = pi;
        x[StellarField.W] = 0;
        x[StellarField.L] = 0;
    }

    /**
     * P4 (T8): the satellite crossing's timer (prototype {@code S.tde}): an exact 120 s countdown, then 300 s of a
     * decaying P kick; caught (+1 cycle) if the field stays Stable or better throughout.
     */
    public static final class Crossing {

        /** Field-clock second the crossing hits (integral seconds plus a uniform draw). */
        public double next;
        /** Hit seconds left (300 → 0); 0 when not crossing. */
        public int left;
        /** True from the 120 s warning until the hit. */
        public boolean warned;
        /** False once the field left Stable during this crossing. */
        public boolean ok = true;
        /** Seconds since the hit (τ in 12·(1 + τ/10)^(−5/3)). */
        public int tau;
        /** Crossings finished, and crossings caught. */
        public int n, caught;

        public Crossing(double next) {
            this.next = next;
        }
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
        /** P4 (T7/T8): the sum of β over the cycle's seconds (T7's product takes min(β̄, 1.5)). */
        public double bSum;
        /** P4 (T8): Ergomatter fed this cycle, litres (the product is 1.0 × this × mean Y × r). */
        public double f8;

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
