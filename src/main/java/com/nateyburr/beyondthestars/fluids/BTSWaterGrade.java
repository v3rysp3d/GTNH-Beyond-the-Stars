package com.nateyburr.beyondthestars.fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.Materials;
import gregtech.api.util.GTModHandler;
import gregtech.common.tileentities.machines.multi.purification.PurifiedWaterHelpers;

/**
 * D-26: THE PURIFIED-WATER LADDER, TYPED ONCE.
 * <p>
 * My ruling gives the water grade two effects, and this class is the only place either
 * of them is written down:
 * <ol>
 * <li>a <b>YIELD</b> term on the fuel line, at F-12 (reconversion) and nowhere else - distilled
 * x0.5, Grade 1 x1.0, +0.5 per grade to Grade 8 x4.5; and</li>
 * <li>an <b>OUTPUT</b> term on the reactor - x1.000 for plain and distilled water, then +5% per
 * purified grade to x1.400 at Grade 8.</li>
 * </ol>
 * <b>The balance guard, in one line of arithmetic:</b> the yield term spans x9.00 (500 -> 4500
 * milli) and the output term spans x1.40 (1000 -> 1400 milli). The output effect is smaller by
 * a factor of 6.4, and the whole water ladder (+40% EU) is worth less than the CHEAPEST single
 * rung of the fuel ladder (ROD_MULT 1 -> 2 is +100% EU). A Purification Plant is therefore a
 * route to cheaper fuel, never a route to power. Because both numbers live here, that guard is
 * checkable by reading one file, and the recipe page and the reactor readout cannot drift.
 * <p>
 * <b>Grade 0 is distilled water, not "no water".</b> {@link PurifiedWaterHelpers#getWaterTier}
 * returns 0 for plain water, for distilled water AND for an unrecognised fluid, so distilled has
 * to be caught by an explicit equality test BEFORE that helper is asked. {@link #gradeOf}
 * therefore answers -1 (not a fuel-line water) rather than folding the two cases together.
 * <p>
 * <b>Do not call {@code PurifiedWaterHelpers.getPurifiedWaterTier(0)}</b> - it throws
 * IllegalStateException for anything outside 1..8. {@link #fluidFor} handles grade 0 itself.
 */
public final class BTSWaterGrade {

    /** The highest purified rung the Purification Plant produces. */
    public static final int MAX_GRADE = 8;

    /** Distilled water: the bottom rung of the fuel-line ladder, and the x0.5 yield penalty. */
    public static final int DISTILLED = 0;

    /** Not a water this ladder recognises (plain water, an unknown fluid, or nothing at all). */
    public static final int NOT_GRADED = -1;

    private BTSWaterGrade() {}

    /**
     * The ladder rung a fluid sits on: {@link #DISTILLED} (0) for distilled water, 1..8 for the
     * purified grades, {@link #NOT_GRADED} (-1) for everything else - plain water included.
     * <p>
     * The distilled test runs FIRST and by fluid equality, because the GT helper cannot tell
     * distilled water from plain water from a bucket of creosote: all three answer 0.
     */
    public static int gradeOf(FluidStack fs) {
        if (fs == null || fs.getFluid() == null) return NOT_GRADED;
        final FluidStack distilled = GTModHandler.getDistilledWater(1L);
        if (distilled != null && fs.isFluidEqual(distilled)) return DISTILLED;
        final int tier = PurifiedWaterHelpers.getWaterTier(fs);
        return tier >= 1 && tier <= MAX_GRADE ? tier : NOT_GRADED;
    }

    /**
     * The FUEL-LINE yield multiplier, x1000: 500 at distilled, 1000 at Grade 1, +500 a grade to
     * 4500 at Grade 8. An ungraded fluid has no rung at all and answers 0 - F-12 has nine
     * explicit recipes and no default, so there is nothing sensible to return.
     */
    public static int yieldMilli(int grade) {
        if (grade == DISTILLED) return 500;
        if (grade < 1 || grade > MAX_GRADE) return 0;
        return 1000 + (grade - 1) * 500;
    }

    /**
     * The REACTOR OUTPUT multiplier, x1000: 1000 (a no-op) for plain water, distilled water and
     * anything ungraded; 1050..1400 for grades 1..8.
     * <p>
     * The floor of 1000 is what guarantees the release's regression test: a plant fed plain or
     * distilled water multiplies its demand by {@code 1000 / 1000}, which is exactly itself, so
     * it derives a BIT-IDENTICAL EU figure to the one it derived in 0.23.0. No water grade can
     * ever be MANDATORY, because the baseline is the unimproved number.
     */
    public static int outputMilli(int grade) {
        if (grade < 1 || grade > MAX_GRADE) return 1000;
        return 1000 + grade * 50;
    }

    /** {@link #outputMilli} straight off a hatch's fluid, for callers holding a stack. */
    public static int outputMilliOf(FluidStack fs) {
        return outputMilli(gradeOf(fs));
    }

    /**
     * F-12's output count: {@code round(4 * Y)} = 2, 4, 6, 8, 10, 12, 14, 16, 18. The base of 4
     * was chosen precisely so every rung of the ruled ladder lands on an integer.
     */
    public static int powderFor(int grade) {
        return 4 * yieldMilli(grade) / 1000;
    }

    /**
     * The fluid for a rung, for the nine parallel F-12 recipes and for the reactor's worst-first
     * feedwater sweep. Null for an out-of-range grade, so a caller that cannot build its rung can
     * SKIP it rather than throw - recipe registration runs in postInit, where one escaping
     * exception costs the whole mod.
     */
    public static FluidStack fluidFor(int grade, int litres) {
        if (grade == DISTILLED) return GTModHandler.getDistilledWater(litres);
        if (grade < 1 || grade > MAX_GRADE) return null;
        final Materials material = PurifiedWaterHelpers.getPurifiedWaterTier(grade);
        return material == null ? null : material.getFluid(litres);
    }

    /** Display name for a rung, for NEI descriptions and the plant's panels. */
    public static String label(int grade) {
        if (grade == DISTILLED) return "Distilled Water";
        if (grade < 1 || grade > MAX_GRADE) return "Water";
        return "Grade " + grade + " Purified Water";
    }
}
