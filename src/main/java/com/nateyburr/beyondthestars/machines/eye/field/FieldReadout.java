package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: an immutable per-second snapshot for the GUI, the scanner and the Stellar Field Sensor Hatch
 * (d49-build-plan.md §12). Built by the runtime after each field second; {@link #DARK} when no star is lit.
 * <p>
 * P4 (quasar-p4.md §8.1): the galaxy gauges W and Λ, β, the link streak and its 60 s mean, planets mode's slot progress
 * and the event countdown ride along ({@link #withGalaxy}); the sensor's new channels read them.
 */
public final class FieldReadout {

    /** The sensor hatch channels (plan §12). NBT keeps the ordinal: only append. */
    public enum Channel {

        P(true, 0),
        THETA(true, 0),
        OMEGA(true, 0),
        PI(true, 0),
        /** T6 shield, 0..100. */
        SHIELD(false, 100),
        /** Integrity, 0..100. */
        INTEGRITY(false, 100),
        /** Stability, 0..100. */
        STABILITY(false, 100),
        /** 0 Resonant .. 4 Breach (−1 when not lit). Analog: 0, 4, 8, 11, 15. */
        ZONE(false, 4),
        /** +1 warning with a + sign (or unsigned), −1 with a − sign, 0 none. Analog: 15 or 0. */
        WARN_PLUS(false, 1),
        WARN_MINUS(false, 1),
        WARN_ANY(false, 1),
        /** 1 when the cycle's upkeep is fully met (r = 1). Analog: 15 or 0. */
        UPKEEP_OK(false, 1),
        /** T5: Θ minus the cooling setpoint. */
        SETPOINT_ERROR(true, 0),
        // ---- P4 (appended)
        /** T7+: W, the rail's warp. */
        W(true, 0),
        /** T8: Λ, the tidal shear. */
        LAMBDA(true, 0),
        /** T7+: β, the bursts against the demand (0..2 analog). */
        BETA(false, 2),
        /** The Stability (T6) or Heavy (T7) Link's streak, % of 30 min. */
        LINK_STREAK(false, 100),
        /** The link's 60 s mean (or the Eye's), Bu/t; analog 0..w. */
        LINK_MEAN(false, 0),
        /** Planets mode: the running slots' mean progress, %. */
        SLOT_PROGRESS(false, 100),
        /** Seconds to the running countdown (merger, crossing, pass, collapse); −1 none; analog 0..w. */
        COUNTDOWN(false, 0),
        // ---- P5 (p5-t9-t10.md §9.3; appended)
        /** T9: V, the infall against the beam (the beam's own wire: analog, window ±30). */
        V(true, 0),
        /** T10: H, the expansion error against the Friedmann curve. */
        H(true, 0),
        /** T9: the beam's ρ (beam power over the Attractor's drive); analog 0..w. */
        BEAM_RHO(false, 0),
        /** T9: GeV per pellet of the grade fired, in EeV (1.25 / 5 / 20); analog 0..w. */
        BEAM_GEV(false, 0),
        /** T9: the beam's hold rating, 0..12. */
        HOLD_RATING(false, 12),
        /** T9: the warned or surging group's size, 0..12 (0 none). */
        FLOW_SIZE(false, 12),
        /** T9+: galaxies held (the display counter); analog 0..w. */
        GALAXIES(false, 0),
        /** T10: the universe's age, % of its 7,200 s. */
        UNIVERSE_AGE(false, 100),
        /** T8: the Deep Link's streak, % of 30 min. */
        DEEP_LINK_STREAK(false, 100);

        /** Gauge-scaled (−120..+120). */
        public final boolean gauge;
        /**
         * Otherwise the value's full scale (0..max maps to 0..15): 100, the zone's 4, a flag's 1; 0 means unbounded
         * (the sensor's window w is the full scale).
         */
        public final double max;

        Channel(boolean gauge, double max) {
            this.gauge = gauge;
            this.max = max;
        }

        /** P4: the window w is this channel's full scale (Bu/t, seconds). */
        public boolean unbounded() {
            return !gauge && max <= 0;
        }
    }

    /** No star lit. */
    public static final FieldReadout DARK = new FieldReadout(
        false,
        0,
        new double[StellarField.GAUGES],
        0,
        0,
        0,
        -1,
        0,
        0,
        0,
        Double.NaN,
        1,
        1,
        1,
        1);

    public final boolean lit;
    /** Star tier 1..8 (0 when dark). */
    public final int tier;
    private final double[] x;
    public final double A, I;
    public final int lock, zone;
    public final double stability, Y;
    public final int warn;
    /** T5 setpoint (NaN elsewhere). */
    public final double sp;
    public final double m, q, r, p;
    // ---- P4
    /** β last second; the active link's streak (0..1) and 60 s mean (Bu/t); slot progress 0..1; countdown (s, −1). */
    public final double beta, linkStreak, linkMean, slotProgress;
    public final int countdown;
    /** Planets mode (one gauge). */
    public final boolean planets;
    // ---- P5 (set by withP5)
    public double beamRho, beamEeV, galaxies, universeAge, deepLinkStreak;
    public int holdRating, flowSize;

    /** P5: this snapshot with the T9 / T10 extras (the fields are set in place on a fresh copy). */
    public FieldReadout withP5(double beamRho, double beamEeV, int holdRating, int flowSize, double galaxies,
        double universeAge, double deepLinkStreak) {
        final FieldReadout r = withGalaxy(beta, linkStreak, linkMean, slotProgress, countdown, planets);
        r.beamRho = beamRho;
        r.beamEeV = beamEeV;
        r.holdRating = holdRating;
        r.flowSize = flowSize;
        r.galaxies = galaxies;
        r.universeAge = universeAge;
        r.deepLinkStreak = deepLinkStreak;
        return r;
    }

    public FieldReadout(boolean lit, int tier, double[] x, double A, double I, int lock, int zone, double stability,
        double Y, int warn, double sp, double m, double q, double r, double p) {
        this(lit, tier, x, A, I, lock, zone, stability, Y, warn, sp, m, q, r, p, 0, 0, 0, 0, -1, false);
    }

    private FieldReadout(boolean lit, int tier, double[] x, double A, double I, int lock, int zone, double stability,
        double Y, int warn, double sp, double m, double q, double r, double p, double beta, double linkStreak,
        double linkMean, double slotProgress, int countdown, boolean planets) {
        this.lit = lit;
        this.tier = tier;
        final double[] all = new double[StellarField.GAUGES];
        System.arraycopy(x, 0, all, 0, Math.min(x.length, all.length));
        this.x = all;
        this.A = A;
        this.I = I;
        this.lock = lock;
        this.zone = zone;
        this.stability = stability;
        this.Y = Y;
        this.warn = warn;
        this.sp = sp;
        this.m = m;
        this.q = q;
        this.r = r;
        this.p = p;
        this.beta = beta;
        this.linkStreak = linkStreak;
        this.linkMean = linkMean;
        this.slotProgress = slotProgress;
        this.countdown = countdown;
        this.planets = planets;
    }

    /** A lit second's snapshot. */
    public static FieldReadout of(FieldState s, StepResult out, StepInputs in) {
        return new FieldReadout(
            true,
            s.tier.n,
            s.x,
            s.A,
            s.I,
            s.lock,
            out.zone,
            out.stability,
            out.Y,
            out.warn,
            out.sp,
            in.m,
            in.q,
            in.r,
            in.p);
    }

    /** P4: this snapshot with the galaxy extras. */
    public FieldReadout withGalaxy(double beta, double linkStreak, double linkMean, double slotProgress, int countdown,
        boolean planets) {
        return new FieldReadout(
            lit,
            tier,
            x,
            A,
            I,
            lock,
            zone,
            stability,
            Y,
            warn,
            sp,
            m,
            q,
            r,
            p,
            beta,
            linkStreak,
            linkMean,
            slotProgress,
            countdown,
            planets);
    }

    /** Gauge k (P, Θ, Ω, Π, W, Λ). */
    public double x(int k) {
        return k >= 0 && k < x.length ? x[k] : 0;
    }

    /** The channel's value. Gauges inactive on the tier read their stored value (0 unless set). */
    public double value(Channel c) {
        switch (c) {
            case P:
                return x[StellarField.P];
            case THETA:
                return x[StellarField.TH];
            case OMEGA:
                return x[StellarField.OM];
            case PI:
                return x[StellarField.PI];
            case SHIELD:
                return A;
            case INTEGRITY:
                return I;
            case STABILITY:
                return stability;
            case ZONE:
                return zone;
            case WARN_PLUS:
                return warn > 0 ? 1 : 0;
            case WARN_MINUS:
                return warn < 0 ? 1 : 0;
            case WARN_ANY:
                return warn != 0 ? 1 : 0;
            case UPKEEP_OK:
                return r >= 1 ? 1 : 0;
            case W:
                return x[StellarField.W];
            case LAMBDA:
                return x[StellarField.L];
            case BETA:
                return beta;
            case LINK_STREAK:
                return 100 * linkStreak;
            case LINK_MEAN:
                return linkMean;
            case SLOT_PROGRESS:
                return 100 * slotProgress;
            case COUNTDOWN:
                return countdown;
            case V:
                return x[StellarField.V];
            case H:
                return x[StellarField.H];
            case BEAM_RHO:
                return beamRho;
            case BEAM_GEV:
                return beamEeV;
            case HOLD_RATING:
                return holdRating;
            case FLOW_SIZE:
                return flowSize;
            case GALAXIES:
                return galaxies;
            case UNIVERSE_AGE:
                return 100 * universeAge;
            case DEEP_LINK_STREAK:
                return 100 * deepLinkStreak;
            default:
                return Double.isNaN(sp) ? 0 : x[StellarField.TH] - sp;
        }
    }

    /**
     * Analog redstone 0..15: {@code round(15·(v+120)/240)} for gauges, {@code round(15·v/max)} for the rest (0-100
     * values, the zone 0..4, and the 0/1 flags, which give 15 when set). Unbounded channels: the sensor applies its
     * window; here 0..100.
     */
    public int analog(Channel c) {
        final double v = value(c);
        final double max = c.unbounded() ? 100 : c.max;
        final long a = c.gauge ? Math.round(15 * (v + 120) / 240) : Math.round(15 * v / max);
        return (int) Math.max(0, Math.min(15, a));
    }
}
