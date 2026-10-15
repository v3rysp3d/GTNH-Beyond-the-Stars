package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.V;

/**
 * P5: The Infall (T8 → T9) as a field overlay (designs/docs/p5-t9-t10.md §2.8; the prototype's {@code beginInfall}
 * and the P5 wrappers of {@code transTick}, {@code transDrift}, {@code fail}, {@code demandNow}).
 * <ol>
 * <li>t 0, out of the Zone of Avoidance: the object becomes the Attractor (the tier is T9 from the first second), V
 * appears at 0 and its drift ramps in over 600 s; the T8 feed stops; the charge drains at 10 L/s of Superdense
 * Ergomatter ({@link StepResult#infallCharge}); the demand ramps 6 → 24 Bu/t over 1,800 s ({@link #demand}).</li>
 * <li>A forced, accepted Galactic Flow of 6, warned at 1,020 s, arrives at 1,200 s (only that flow runs).</li>
 * <li>Settle: from 1,500 s, once the forced flow has ended, Stable or better for 120 consecutive seconds; past 2,100 s
 * it
 * fails.</li>
 * </ol>
 * On success the Great Attractor lights ({@link StellarField#light}, the first flow scheduled). On failure (Q-P5-5, the
 * soft rule) the Milky Way is thrown back out still lit at T8 with Integrity 50: {@link #softFail}. The runtime owns
 * the core (escrow), the charge buffer, the preconditions and the unlock.
 */
public final class InfallOverlay implements Overlay {

    public static final int RAMP_V = 600, DURATION = 1800, SETTLE_FROM = 1500, SETTLE_STREAK = 120, DEADLINE = 2100;
    /** The charge drained per second, litres of Superdense Ergomatter; the buffer it needs to begin. */
    public static final double CHARGE_PER_SECOND = 10, CHARGE_BUFFER = 18000;
    public static final double DEMAND_FROM = 6, DEMAND_RISE = 18;
    /** Integrity of the Milky Way thrown back out (the soft fail). */
    public static final double SOFT_FAIL_INTEGRITY = 50;

    public int phase = 1;
    public int st;
    public int streak;
    /** The forced flow has ended (set after the second it ends, as the prototype's flowPost). */
    public boolean flowDone;

    private InfallOverlay() {}

    /**
     * Prototype {@code beginInfall}: the tier becomes T9 (galaxy mode), V at 0, the forced flow, the beam kept (the
     * Deep
     * Link's test fire may have spooled it). No draws, no light (the cycle in progress carries on).
     */
    public static InfallOverlay begin(FieldState s) {
        s.tier = TierParams.T9;
        s.x[V] = 0;
        s.flow = AttractorField.forcedFlow(s);
        if (s.beam == null) s.beam = new FieldState.Beam();
        return new InfallOverlay();
    }

    /** A restored Infall (NBT). */
    public static InfallOverlay restore(int st, int streak, boolean flowDone) {
        final InfallOverlay o = new InfallOverlay();
        o.st = st;
        o.streak = streak;
        o.flowDone = flowDone;
        o.phase = st >= SETTLE_FROM && flowDone ? 3 : st >= AttractorField.FORCED_AT - AttractorField.FLOW_WARN ? 2 : 1;
        return o;
    }

    /** The burst demand, Bu/t: 6 + 18·min(1, t/1,800) (also β's normaliser). */
    public double demand() {
        return DEMAND_FROM + DEMAND_RISE * Math.min(1, st / (double) DURATION);
    }

    /** V's ramp: min(1, t/600). */
    public double vRamp() {
        return Math.min(1, st / (double) RAMP_V);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in, StepResult out) {
        if (t.attractor) return AttractorField.drift(s, in, vRamp(), true, out);
        return Overlay.super.drift(t, s, in, out);
    }

    @Override
    public void onFlowEnded() {
        flowDone = true;
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        out.infallCharge = CHARGE_PER_SECOND;
        st++;
        if (st >= SETTLE_FROM && flowDone) streak = zone >= 0 && zone <= 1 ? streak + 1 : 0;
        phase = st >= SETTLE_FROM && flowDone ? 3 : st >= AttractorField.FORCED_AT - AttractorField.FLOW_WARN ? 2 : 1;
        if (streak >= SETTLE_STREAK) {
            out.transitionSuccess = true;
            StellarField.light(s, in.dialSetpoint, in.tt);
            s.flow = AttractorField.newFlow(s);
            out.relit = true;
        } else if (st >= DEADLINE) {
            out.fail = "no 120 s Stable streak after the forced flow";
            softFail(s, in, out);
        }
    }

    @Override
    public void onFail(FieldState s, StepInputs in, StepResult out) {
        softFail(s, in, out);
    }

    /**
     * The soft fail (Q-P5-5): the tier is T8 again, the flow gone, every gauge 0, the Milky Way lit
     * ({@link StellarField#light}: its merger and crossing scheduled), Integrity 50. The charge is spent; the core
     * stays in escrow (the runtime's part). Sets {@link StepResult#recovered} and {@link StepResult#relit}.
     */
    static void softFail(FieldState s, StepInputs in, StepResult out) {
        s.tier = TierParams.T8;
        s.flow = null;
        s.setX(0, 0, 0, 0);
        StellarField.light(s, in.dialSetpoint, in.tt);
        s.I = SOFT_FAIL_INTEGRITY;
        out.recovered = true;
        out.softFail = true;
        out.relit = true;
    }

    /** 1 the infall, 2 the forced flow, 3 the settle. */
    public int phase() {
        return phase;
    }
}
