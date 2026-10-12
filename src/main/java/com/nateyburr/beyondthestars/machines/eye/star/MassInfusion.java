package com.nateyburr.beyondthestars.machines.eye.star;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;

import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * D-49 Mass Infusion, T1 → T2: Sol becomes Sirius A (d49-build-plan.md §7.2; design §4). 5,000,000 L of Raw Star
 * Matter are taken when it begins; for 600 s Sol runs under T1 rules with the extra drift {@code d'_P += 4}, its
 * upkeep, output and trims continuing. Survive the 600 s: the star becomes Sirius A, gauges and Integrity carried
 * over, the Harmonic Lock reset and the partial cycle discarded (R-5). Breach or Integrity 0 loses Sol and the RSPM,
 * and keeps the core.
 */
public final class MassInfusion extends EyeTransition {

    public static final int DURATION = 600;
    public static final double SWELL = 4;
    /** Raw Star Matter taken at the start. */
    public static final long RSPM = 5_000_000L;

    MassInfusion() {
        super(Type.MASS_INFUSION);
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        if (t < DURATION) dp[P] += SWELL;
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        if (t >= DURATION) {
            becomeTier(s, TierParams.T2, in, out);
            out.transitionSuccess = true;
        }
    }

    @Override
    public String phaseLine() {
        return "Mass Infusion: " + t
            + " / "
            + DURATION
            + " s (P drift +"
            + (int) SWELL
            + "; survive it and Sol becomes"
            + " Sirius A)";
    }
}
