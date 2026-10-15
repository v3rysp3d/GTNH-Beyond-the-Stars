package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.H;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;

/**
 * P5: Inflation (T9 → T10) as a field overlay (designs/docs/p5-t9-t10.md §6.3; the prototype's {@code beginInflation}
 * and the P5 wrappers of {@code transTick} and {@code fail}, kind 'inf').
 * <ol>
 * <li>t 0-300, unfolding: the Attractor runs under T9 rules (flows still come); the demand ramps 24 → 96 Bu/t over
 * 900 s ({@link #demand}).</li>
 * <li>t 300, inflation: the tier becomes T10; W, Λ and V retire; H appears at +{@link #shock} (85 adopted); Integrity
 * −20. From then the charge drains 30.72 L/s of Dark Matter ({@link StepResult#inflationCharge}); H drifts +0.4.</li>
 * <li>t 600, recombination: Stable or better for 120 consecutive seconds inside 600-900 s, or it fails.</li>
 * </ol>
 * On success H returns to 0, the first universe is charged and T10 lights. On failure (the soft rule) the Great
 * Attractor is relit at T9 with Integrity 50: {@link #softFail}. The runtime owns the core, the charge and the unlock.
 */
public final class InflationOverlay implements Overlay {

    public static final int INFLATE_AT = 300, RECOMBINE_AT = 600, DEADLINE = 900, SETTLE_STREAK = 120;
    public static final double DEFAULT_SHOCK = 85;
    /** H's drift once inflated (the universe expanding too fast). */
    public static final double H_DRIFT = 0.4;
    /** The charge drained per second from inflation on, litres of Dark Matter; the buffer it needs to begin. */
    public static final double CHARGE_PER_SECOND = 30.72, CHARGE_BUFFER = 18432;
    public static final double DEMAND_FROM = 24, DEMAND_RISE = 72;
    public static final double INTEGRITY_LOSS = 20, SOFT_FAIL_INTEGRITY = 50;

    /** H at inflation. */
    public final double shock;
    /** 1 unfolding, 2 inflation, 3 recombination. */
    public int phase = 1;
    public int st;
    public int streak;

    public InflationOverlay(double shock) {
        this.shock = shock > 0 ? shock : DEFAULT_SHOCK;
    }

    /** A restored Inflation (NBT). */
    public static InflationOverlay restore(double shock, int phase, int st, int streak) {
        final InflationOverlay o = new InflationOverlay(shock);
        o.phase = phase;
        o.st = st;
        o.streak = streak;
        return o;
    }

    /** The burst demand, Bu/t: 24 + 72·min(1, t/900) (also β's normaliser). */
    public double demand() {
        return DEMAND_FROM + DEMAND_RISE * Math.min(1, st / (double) DEADLINE);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in, StepResult out) {
        if (t.universe) return UniverseField.drift(s, in, true, out);
        return Overlay.super.drift(t, s, in, out);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        st++;
        if (phase == 1 && st >= INFLATE_AT) {
            phase = 2;
            s.tier = TierParams.T10;
            final double keep = s.x[P];
            s.setX(keep, 0, 0, 0);
            s.x[H] = shock;
            s.I = Math.max(0, s.I - INTEGRITY_LOSS);
            s.lock = 0;
            out.inflated = true;
        }
        if (phase == 2) {
            out.inflationCharge = CHARGE_PER_SECOND;
            if (st >= RECOMBINE_AT) {
                phase = 3;
                out.recombination = true;
            }
        }
        if (phase == 3) {
            streak = zone >= 0 && zone <= 1 ? streak + 1 : 0;
            if (streak >= SETTLE_STREAK) {
                out.transitionSuccess = true;
                s.x[H] = 0;
                s.uni = UniverseField.newUniverse(s);
                out.uniChargedInTick = true;
                StellarField.light(s, in.dialSetpoint, in.tt);
                out.relit = true;
            } else if (st >= DEADLINE) {
                out.fail = "no 120 s Stable streak inside 600-900 s";
                softFail(s, in, out);
            }
        }
    }

    @Override
    public void onFail(FieldState s, StepInputs in, StepResult out) {
        softFail(s, in, out);
    }

    /**
     * The soft fail: the Great Attractor relit at T9 (galaxy mode) with every gauge 0 and Integrity 50; the flow and
     * the
     * universe in hand are kept (as the prototype). The charge is spent; the core stays in escrow.
     */
    static void softFail(FieldState s, StepInputs in, StepResult out) {
        s.tier = TierParams.T9;
        s.setX(0, 0, 0, 0);
        StellarField.light(s, in.dialSetpoint, in.tt);
        s.I = SOFT_FAIL_INTEGRITY;
        out.recovered = true;
        out.softFail = true;
        out.relit = true;
    }
}
