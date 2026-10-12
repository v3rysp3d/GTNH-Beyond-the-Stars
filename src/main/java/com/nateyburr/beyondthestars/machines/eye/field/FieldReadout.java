package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: an immutable per-second snapshot for the GUI, the scanner and the Stellar Field Sensor Hatch
 * (d49-build-plan.md §12). Built by the runtime after each field second; {@link #DARK} when no star is lit.
 */
public final class FieldReadout {

    /** The sensor hatch channels (plan §12). */
    public enum Channel {

        P(true),
        THETA(true),
        OMEGA(true),
        PI(true),
        /** T6 shield, 0..100. */
        SHIELD(false),
        /** Integrity, 0..100. */
        INTEGRITY(false),
        /** Stability, 0..100. */
        STABILITY(false),
        /** 0 Resonant .. 4 Breach (−1 when not lit). */
        ZONE(false),
        /** +1 warning with a + sign (or unsigned), −1 with a − sign, 0 none. */
        WARN_PLUS(false),
        WARN_MINUS(false),
        WARN_ANY(false),
        /** 1 when the cycle's upkeep is fully met (r = 1). */
        UPKEEP_OK(false),
        /** T5: Θ minus the cooling setpoint. */
        SETPOINT_ERROR(true);

        /** Gauge-scaled (−120..+120) rather than 0..100. */
        public final boolean gauge;

        Channel(boolean gauge) {
            this.gauge = gauge;
        }
    }

    /** No star lit. */
    public static final FieldReadout DARK = new FieldReadout(
        false,
        0,
        new double[4],
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
    /** Star tier 1..6 (0 when dark). */
    public final int tier;
    private final double[] x;
    public final double A, I;
    public final int lock, zone;
    public final double stability, Y;
    public final int warn;
    /** T5 setpoint (NaN elsewhere). */
    public final double sp;
    public final double m, q, r, p;

    public FieldReadout(boolean lit, int tier, double[] x, double A, double I, int lock, int zone, double stability,
        double Y, int warn, double sp, double m, double q, double r, double p) {
        this.lit = lit;
        this.tier = tier;
        this.x = x.clone();
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

    /** Gauge k (P, Θ, Ω, Π). */
    public double x(int k) {
        return x[k];
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
            default:
                return Double.isNaN(sp) ? 0 : x[StellarField.TH] - sp;
        }
    }

    /** Analog redstone 0..15: {@code round(15·(v+120)/240)} for gauges, {@code round(15·v/100)} for 0-100 values. */
    public int analog(Channel c) {
        final double v = value(c);
        final long a = c.gauge ? Math.round(15 * (v + 120) / 240) : Math.round(15 * v / 100);
        return (int) Math.max(0, Math.min(15, a));
    }
}
