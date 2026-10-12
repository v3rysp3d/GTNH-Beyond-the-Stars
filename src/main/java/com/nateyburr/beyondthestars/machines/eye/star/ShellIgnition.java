package com.nateyburr.beyondthestars.machines.eye.star;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

import com.nateyburr.beyondthestars.machines.eye.field.EventDef;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * D-49 Shell Ignition, T2 → T3: Sirius A becomes Arcturus (d49-build-plan.md §7.3, plan reading R-4). 250,000 L of
 * Excited Stellar Plasma are taken when it begins. The core heat gauge Θ appears at 0 and ramps in over 600 s with
 * {@code s = t/600}: {@code d_Θ = 1.0·s}, {@code g_Θ = +0.01·s}, and P gains the coupling {@code 0.02·Θ·s}. P keeps
 * Sirius A's parameters; the T3 trim table applies from the start (Θ needs Super Coolant) and the ash is off. It
 * ends in one forced helium flash: the event channel warns at t = 585, Θ takes +30·q for t = 600..602, and the star is
 * Arcturus at t = 603 if it survived.
 */
public final class ShellIgnition extends EyeTransition {

    public static final int RAMP = 600;
    /** The forced flash hits at t = 600, 601, 602; success at t = 603 (the tick count reaches 604). */
    public static final int FLASH_AT = 600;
    public static final int END = 604;
    /** Excited Stellar Plasma taken at the start. */
    public static final long ESPM = 250_000L;
    private static final int[] GAUGES = { P, TH };

    ShellIgnition() {
        super(Type.SHELL_IGNITION);
    }

    private double ramp() {
        return Math.min(1.0, t / (double) RAMP);
    }

    @Override
    void onBegin(FieldState s) {
        s.x[TH] = 0;
        s.ev = new FieldState.EventState((double) s.t + FLASH_AT);
    }

    @Override
    public EventDef event(TierParams tier, FieldState s) {
        return TierParams.T3.event;
    }

    @Override
    public int[] gauges(TierParams tier) {
        return GAUGES;
    }

    @Override
    public double gain(TierParams tier, int k) {
        return k == TH ? 0.01 * ramp() : tier.gain(k);
    }

    @Override
    public double coupling(TierParams tier, double[] x, int k) {
        return k == P ? 0.02 * x[TH] * ramp() : 0;
    }

    @Override
    public double[] drift(TierParams tier, StepInputs in) {
        final double[] d = tier.drift(in.feed, in.r);
        d[TH] = 1.0 * ramp();
        return d;
    }

    @Override
    public boolean ash(TierParams tier) {
        return false;
    }

    @Override
    public TierParams trimTable(TierParams tier) {
        return TierParams.T3;
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        if (t >= END) {
            becomeTier(s, TierParams.T3, in, out);
            out.transitionSuccess = true;
        }
    }

    @Override
    public String phaseLine() {
        return "Shell Ignition: " + t
            + " / "
            + (END - 1)
            + " s, Θ ramp "
            + Math.round(ramp() * 100)
            + " %"
            + (t < FLASH_AT - 15 ? " (forced helium flash warned at 585 s, hits at 600-602 s)"
                : t < FLASH_AT ? " (FORCED HELIUM FLASH in " + (FLASH_AT - t) + " s: pre-cool Θ)"
                    : " (forced helium flash)");
    }
}
