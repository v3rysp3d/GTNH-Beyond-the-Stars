package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.H;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;

/**
 * P5: Eye T10, the Universe (designs/docs/p5-t9-t10.md §6; the prototype's P5 block {@code universeTier},
 * {@code newUniverse} and its {@code fail} rule). One universe per 7,200 s batch, run from its first light to its last;
 * the gauge H is the expansion error against the crown's Friedmann curve. Pure: no Minecraft or GT types.
 * <ul>
 * <li>H drifts {@code 0.4 + 1.2·smoothstep(0.6 → 0.8 of the age)} (dark energy takes over late), +0.15 for the 300 s of
 * Recombination (1,800 s, warned 60 s), +0.3 for the 300 s of Cosmic Dawn (once per universe at 3,240-4,680 s, warned
 * 120 s). Recombination drops P by 6/s for 5 s; Cosmic Dawn adds {@code 12·(1 + τ/10)^(−5/3)} to P.</li>
 * <li>At the batch's end: Universium {@code 9,216 × mean Y × r}, +12.5 % when the dawn was held Stable or better
 * throughout; the next universe is charged (Dark Matter 9,216 L) and H returns to 0.</li>
 * <li>A Breach or Integrity 0 ends the universe early: 25 % × the fraction run is salvaged, the next universe is
 * charged, the Eye stays at T10 ({@link #collapse}).</li>
 * </ul>
 * Golden patches B-169 (tools/p5-golden.js): q is applied to the event terms however the Eye reached T10, and the
 * countdown is on the event channel.
 */
public final class UniverseField {

    private UniverseField() {}

    public static final int BATCH = 7200;
    /** Universium per universe at Y 1, litres; the Dark Matter charge per universe, litres. */
    public static final double OUT = 9216, CHARGE = 9216;
    /** The bonus for a caught Cosmic Dawn, as a fraction of {@link #OUT}. */
    public static final double DAWN_BONUS = 0.125;
    /** A collapsed universe salvages this × the fraction run. */
    public static final double SALVAGE = 0.25;
    public static final int REC_AT = 1800, REC_WARN = 60, REC_DUR = 300, REC_P_SECONDS = 5;
    public static final double REC_H = 0.15, REC_P = 6;
    public static final double DAWN_FROM = 3240, DAWN_TO = 4680;
    public static final int DAWN_WARN = 120, DAWN_DUR = 300;
    public static final double DAWN_H = 0.3;
    /** H's runaway gain and noise; P's at T10. */
    public static final double H_GAIN = 0.01, H_SIGMA = 1.5;
    /** The Universe rig's valve size, units per second (F-P5-2: 2-unit valves). */
    public static final double RIG_VALVE = 2;

    /** Dark energy's drift: 0.4 + 1.2·smoothstep((t/7,200 − 0.6)/0.2). */
    public static double deDrift(int t) {
        final double x = StellarField.clamp((t / (double) BATCH - 0.6) / 0.2, 0, 1);
        return 0.4 + 1.2 * x * x * (3 - 2 * x);
    }

    /** Prototype {@code newUniverse}: the dawn drawn (one draw). The caller takes the charge. */
    public static FieldState.Universe newUniverse(FieldState s) {
        final FieldState.Universe u = new FieldState.Universe();
        u.dawn = (int) Math.round(StellarField.uni(s.rng, DAWN_FROM, DAWN_TO));
        return u;
    }

    /** A countdown (Recombination or Cosmic Dawn ahead) is running at the universe's age. */
    public static boolean countdown(FieldState.Universe u) {
        return u != null
            && ((u.t > REC_AT - REC_WARN && u.t < REC_AT) || (u.t > u.dawn - DAWN_WARN && u.t < u.dawn));
    }

    /**
     * The universe's drift (the prototype's {@code universeTier(..).d}): P = 3·(β − 1); H by the curve and the events.
     * The universe is created on first need (charged: {@link StepResult#uniChargedInDrift}). While Inflation runs
     * ({@code inflation}) H drifts +0.4 and the universe's clock does not run.
     */
    static double[] drift(FieldState s, StepInputs in, boolean inflation, StepResult out) {
        FieldState.Universe u = s.uni;
        if (u == null) {
            u = s.uni = newUniverse(s);
            out.uniChargedInDrift = true;
        }
        final double q = in.q;
        final double[] d = new double[StellarField.GAUGES];
        d[P] = GalaxyField.P_PER_BETA * (in.beta - 1);
        if (inflation) {
            d[H] = InflationOverlay.H_DRIFT;
            return d;
        }
        u.t++;
        d[H] = deDrift(u.t);
        if (u.t >= REC_AT && u.t < REC_AT + REC_DUR) d[H] += REC_H;
        if (u.t == REC_AT - REC_WARN) out.recombinationWarned = true;
        if (u.t >= REC_AT && u.t < REC_AT + REC_P_SECONDS) d[P] -= REC_P * q;
        if (u.t == REC_AT) out.recombination = true;
        if (u.t == u.dawn - DAWN_WARN) out.dawnWarned = true;
        if (u.t >= u.dawn && u.t < u.dawn + DAWN_DUR) {
            final int tau = u.t - u.dawn;
            d[P] += GalaxyField.crossingKick(tau) * q;
            d[H] += DAWN_H;
            if (s.zone > 1) u.dawnOk = false;
            if (tau == 0) out.dawnHit = true;
        }
        return d;
    }

    /**
     * The universe's per-second rule (prototype {@code universeTier(..).perSec}, patch 2): the countdown on the event
     * channel; the batch's mean Y; at 7,200 s the Universium (and the dawn's bonus), then the next universe (charged:
     * {@link StepResult#uniChargedPerSec}) with H at 0.
     */
    static void perSec(FieldState s, double Y, StepInputs in, boolean trans, StepResult out) {
        final FieldState.Universe u = s.uni;
        if (u != null && !trans && countdown(u)) out.warn = 1;
        if (u == null || trans) return;
        u.ySum += Y;
        u.n++;
        u.rMin = Math.min(u.rMin, in.r);
        if (u.t >= BATCH) {
            final double mY = u.ySum / u.n;
            out.universeDone = true;
            out.universeMeanY = mY;
            out.universeBase = OUT * mY * u.rMin;
            out.universeBonus = u.dawnOk ? OUT * DAWN_BONUS * u.rMin : 0;
            out.universeDawnOk = u.dawnOk;
            s.uni = newUniverse(s);
            out.uniChargedPerSec = true;
            s.x[H] = 0;
        }
    }

    /**
     * Prototype {@code fail} at T10 with no Inflation running: the universe tears apart (H above 0, a Big Rip) or
     * recollapses (a Big Crunch); 25 % × the fraction run is salvaged ({@link StepResult#universeSalvage}, not × r);
     * Integrity 100, lock 0, H and P at 0, and the next universe is charged. The Eye stays lit at T10.
     */
    static void collapse(FieldState s, StepResult out) {
        final FieldState.Universe u = s.uni;
        final double frac = u != null ? u.t / (double) BATCH : 0;
        out.universeCollapsed = true;
        out.universeRip = s.x[H] > 0;
        out.universeFraction = frac;
        out.universeSalvage = OUT * SALVAGE * frac;
        out.recovered = true;
        s.I = 100;
        s.lock = 0;
        s.x[H] = 0;
        s.x[P] = 0;
        s.uni = newUniverse(s);
        out.uniChargedInFail = true;
    }

    /**
     * The universe's render / GUI phase at its age: 1 early (opaque), 2 Recombination (1,800-2,100 s), 3 transparent,
     * 4 Cosmic Dawn (its 300 s), 5 late (dark energy, from 60 % of the age).
     */
    public static int phase(FieldState.Universe u) {
        if (u == null) return 0;
        if (u.t >= u.dawn && u.t < u.dawn + DAWN_DUR) return 4;
        if (u.t >= REC_AT && u.t < REC_AT + REC_DUR) return 2;
        if (u.t >= 0.6 * BATCH) return 5;
        if (u.t >= REC_AT) return 3;
        return 1;
    }
}
