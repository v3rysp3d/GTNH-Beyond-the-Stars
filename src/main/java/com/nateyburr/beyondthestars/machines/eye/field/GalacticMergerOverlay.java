package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.L;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.W;

/**
 * P4: the Galactic Merger (T7 → T8) as a field overlay (quasar-p4.md §5.3; the prototype's {@code beginGM},
 * {@code transDrift} / {@code transTick} kind 'gm').
 * <ul>
 * <li>1,800 s: the demand ramps 1.5 → 6 Bu/t ({@link #demand}); the Ergomatter charge feeds 5 L/s
 * ({@link StepResult#ergoCharged}), 9,000 L in all (none in the settle); the galaxy's own minor mergers and crossings
 * are held back.</li>
 * <li>Three pericentre passes at 600, 1,200 and 1,800 s, each warned 60 s ahead with its sign (drawn at the begin):
 * W ±12 and P +5 per s for 5 s, × q.</li>
 * <li>At the first pass (t = 600) the tier becomes T8: Λ appears at 0, its drift (0.4 × 5 L/s) and gain (0.02) ramping
 * in over 600 s (finding F-4).</li>
 * <li>After t = 1,800: Stable or better for 120 consecutive seconds within 300 s, or it fails. On success the Milky
 * Way lights ({@link StellarField#light}: {@link StepResult#relit}, charge its first cycle).</li>
 * </ul>
 * Construct with {@link #begin}, which draws the three signs and holds the galaxy's events back.
 */
public final class GalacticMergerOverlay implements Overlay {

    public static final int DURATION = 1800, SETTLE_WINDOW = 300, SETTLE_STREAK = 120, PASS_WARN = 60, PASS_DUR = 5;
    public static final int[] PASSES = { 600, 1200, 1800 };
    public static final double PASS_W = 12, PASS_P = 5;
    /** The Ergomatter charge fed during the merger, L/s (also the Λ drift's feed). */
    public static final double CHARGE_PER_SECOND = 5;
    public static final double DEMAND_FROM = 1.5, DEMAND_RISE = 4.5;
    /** Far future: the galaxy's minor merger is held back during the merger. */
    public static final double HELD = 1e15;

    /** 1 = the passes, 2 = the settle after the last pass. */
    public int phase = 1;
    public int st;
    public int streak;
    /** The passes' signs, ±1. */
    public final int[] signs = new int[3];

    private GalacticMergerOverlay() {}

    /** Prototype {@code beginGM}: three sign draws, the event held back, no crossing. */
    public static GalacticMergerOverlay begin(FieldState s) {
        final GalacticMergerOverlay g = new GalacticMergerOverlay();
        for (int i = 0; i < 3; i++) g.signs[i] = s.rng.next() < 0.5 ? -1 : 1;
        s.ev = new FieldState.EventState(HELD);
        s.tde = null;
        return g;
    }

    /** A restored merger (NBT): phase, seconds, streak and signs as saved. */
    public static GalacticMergerOverlay restore(int phase, int st, int streak, int[] signs) {
        final GalacticMergerOverlay g = new GalacticMergerOverlay();
        g.phase = phase;
        g.st = st;
        g.streak = streak;
        System.arraycopy(signs, 0, g.signs, 0, 3);
        return g;
    }

    /** The burst demand, Bu/t: 1.5 + 4.5·min(1, t/1,800) (also β's normaliser). */
    public double demand() {
        return DEMAND_FROM + DEMAND_RISE * Math.min(1, st / (double) DURATION);
    }

    /** Λ's ramp at T8: clamp((t − 600)/600, 0, 1). */
    public double ramp() {
        return StellarField.clamp((st - PASSES[0]) / (double) PASSES[0], 0, 1);
    }

    /** The pass whose 60 s warning is running (index 0..2), or -1. */
    public int warnedPass() {
        for (int i = 0; i < 3; i++) if (st >= PASSES[i] - PASS_WARN && st < PASSES[i]) return i;
        return -1;
    }

    @Override
    public int addEvents(FieldState s, StepInputs in, double[] E, double q, StepResult out) {
        int w = 0;
        for (int i = 0; i < 3; i++) {
            final int pt = PASSES[i], sg = signs[i];
            if (st == pt - PASS_WARN) out.countdown = true;
            if (st >= pt - PASS_WARN && st < pt) w = sg;
            if (st >= pt && st < pt + PASS_DUR) {
                E[W] = E[W] + PASS_W * sg * q;
                E[P] = E[P] + PASS_P * q;
            }
        }
        return w;
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in) {
        if (!t.ergoFeed) return Overlay.super.drift(t, s, in);
        final double[] d = new double[StellarField.GAUGES];
        d[P] = GalaxyField.P_PER_BETA * (in.beta - 1);
        d[W] = GalaxyField.W_DRIFT * s.prec;
        d[L] = GalaxyField.L_PER_F8 * CHARGE_PER_SECOND * ramp();
        return d;
    }

    @Override
    public double gain(TierParams t, int k) {
        return t.ergoFeed && k == L ? t.gain(L) * ramp() : t.gain(k);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        // QA-A15: the charge feeds through the 1,800 s event only (9,000 L), not through the settle after it
        if (st < DURATION) out.ergoCharged = CHARGE_PER_SECOND;
        st++;
        if (st == PASSES[0] && s.tier.n == 7) {
            s.tier = TierParams.T8;
            s.x[L] = 0;
            s.tde = null;
            out.merged = true;
        }
        if (st > DURATION) {
            phase = 2;
            streak = zone >= 0 && zone <= 1 ? streak + 1 : 0;
            if (streak >= SETTLE_STREAK) {
                out.transitionSuccess = true;
                StellarField.light(s, in.dialSetpoint, in.tt);
                out.relit = true;
            } else if (st >= DURATION + SETTLE_WINDOW)
                out.fail = "no 120 s Stable streak inside 300 s after the last pass";
        }
    }
}
