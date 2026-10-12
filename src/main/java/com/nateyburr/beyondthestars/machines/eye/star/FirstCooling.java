package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;

/**
 * D-49 First Cooling, T4 → T5 (d49-build-plan.md §7.5): the first Sirius B batch, under plain T5 rules (the white
 * dwarf assembled at Θ +90, the cooling setpoint ramping to −90 over 7,200 Eye-seconds). It runs at parallel 1 before
 * T5 is unlocked. The runtime decides it at the batch's end: mean Y ≥ 0.9 and no fracture unlocks T5 and consumes the
 * Subspace Cooling Core; a fracture, or a mean Y below 0.9, keeps the core and the next charge is another attempt.
 * The batch's Black Dwarf Matter is emitted either way (salvage or full).
 */
public final class FirstCooling extends EyeTransition {

    /** The batch mean Y the First Cooling needs (and the T5 → T6 milestone's "good batch"). */
    public static final double MEAN_Y = 0.9;

    FirstCooling() {
        super(Type.FIRST_COOLING);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
    }

    /** The phase line, with the batch's progress and its mean Y so far. */
    String line(FieldState s) {
        if (s == null || s.batch == null) return phaseLine();
        return String.format(
            Locale.ROOT,
            "First Cooling: the first batch %.0f / 7,200 Eye-s, mean Y so far %.3f (needs %.1f and no fracture)",
            s.batch.eyeT,
            s.batch.n > 0 ? s.batch.ySum / s.batch.n : 0,
            MEAN_Y);
    }

    @Override
    public String phaseLine() {
        return "First Cooling: the first batch (mean Y " + MEAN_Y + " and no fracture unlocks T5)";
    }
}
