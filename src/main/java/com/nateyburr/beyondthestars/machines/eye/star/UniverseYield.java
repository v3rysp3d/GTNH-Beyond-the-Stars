package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;

/**
 * D-55 (2026-10-17): a universe holds stars, planets and black holes, so each completed 7,200 s universe batch of Eye
 * T10 yields what they make, beside its Molten Universium.
 * <ul>
 * <li><b>Star matter</b> ({@link #STAR_MATTER}): each lower object's product at its companion-table rate × 120
 * cycles, × the batch's p × mean Y × r_min (the factors Universium uses). Raw Star Matter is not in the table: it
 * comes every Eye-minute ({@link RawStarMatterYield}). Attractor Dark Matter is not either: it is the batch's own
 * charge.</li>
 * <li><b>Planets</b>: every registered planet recipe (the Deep Dark included) runs once per batch at parallel
 * 2^(10 − k) (k the planet's Eye tier), each parallel succeeding at (1 − 0.05·R) × the batch's field factor (Resonant /
 * Stable 1, Strained 0.5, Critical 0, averaged over the batch), the rolls planets mode uses. No H/He or EU is paid:
 * the universe is paid by its stability and its charge.</li>
 * <li><b>The filter</b>: the slot bus (T10 has no companions or planets mode) is a whitelist and is never consumed.
 * An empty bus yields everything. A planet item in it limits the planets to those in it; a star chart of T2..T8 in it
 * limits the star matter to those charts' (Sol's chart does nothing: Raw Star Matter always comes). Two toggles (GUI,
 * OpenComputers) switch the star matter and the planets off altogether.</li>
 * </ul>
 * A collapsed universe yields none of this. Every number in the table is a tuning knob.
 */
public final class UniverseYield {

    private UniverseYield() {}

    /** One row of the star-matter table: the object's tier (its product fluid) and the litres per batch at Y 1. */
    public static final class Row {

        public final int tier;
        public final double litres;

        Row(int tier, double litres) {
            this.tier = tier;
            this.litres = litres;
        }

        /** The object's product fluid (its star definition's), or null when it is not registered. */
        public Fluid fluid() {
            final StarDef d = StarDef.ofTier(tier);
            return d == null ? null : d.product;
        }
    }

    /** Cycles per universe batch (7,200 Eye-s / 60). */
    public static final int CYCLES = 120;

    /**
     * The star matter per universe batch at p = 1, Y 1, r 1 (each = the companion table's rate × 120). Tunable:
     * Excited Stellar Plasma 300,000; Raw Hyperdense Stellar Plasma 75,000; White Dwarf Matter 18,420; Black Dwarf
     * Matter 4,608; Molten Magnetar Matter 1,800; Ergomatter 72,000; Superdense Ergomatter 36,000.
     */
    public static final List<Row> STAR_MATTER = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Row(2, 300_000),
            new Row(3, 75_000),
            new Row(4, 18_420),
            new Row(5, 4_608),
            new Row(6, 1_800),
            new Row(7, 72_000),
            new Row(8, 36_000)));

    /** The Eye tier the universe's planet parallels count from: 2^(10 − k). */
    public static final int UNIVERSE_TIER = 10;

    /** A planet's parallel in a universe batch: 2^(10 − k). */
    public static long planetParallel(EyeRecipe r) {
        return 1L << Math.max(0, UNIVERSE_TIER - r.tier);
    }

    /** A planet's per-parallel success at the batch's field factor {@code f}: (1 − 0.05·R) × f, at least 0. */
    public static double planetChance(EyeRecipe r, double f) {
        return Math.max(0, GalaxyField.planetBase(r.rocketTier) * f);
    }

    /** Every registered planet recipe, in registry order. */
    public static List<EyeRecipe> planets() {
        final List<EyeRecipe> out = new ArrayList<>();
        for (EyeRecipe r : EyeRecipeRegistry.all()) if (r.isPlanet()) out.add(r);
        return out;
    }

    /** What one batch yields under the toggles and the slot bus's whitelist. */
    public static final class Selection {

        /** The star-matter rows that yield (empty when star matter is off). */
        public final List<Row> stars = new ArrayList<>();
        /** The planets that yield (empty when planets are off). */
        public final List<EyeRecipe> planets = new ArrayList<>();
        /** The slot bus narrowed the star matter / the planets. */
        public boolean starsFiltered, planetsFiltered;
        public boolean starsOn, planetsOn;
        public int planetTotal;
    }

    /** The batch's yield for these toggles and slot bus items (a whitelist; empty = everything). */
    public static Selection select(boolean starsOn, boolean planetsOn, List<ItemStack> bus) {
        final Selection s = new Selection();
        s.starsOn = starsOn;
        s.planetsOn = planetsOn;
        final Set<Integer> charts = new LinkedHashSet<>();
        final Set<String> planetKeys = new LinkedHashSet<>();
        if (bus != null) for (ItemStack stack : bus) {
            if (stack == null) continue;
            final EyeRecipe r = EyeRecipeRegistry.lookup(stack);
            if (r != null && r.isPlanet()) {
                planetKeys.add(r.key);
                continue;
            }
            final StarDef d = StarDef.byChart(stack);
            if (d != null && d.n() >= 2 && d.n() <= 8) charts.add(d.n());
        }
        final List<EyeRecipe> all = planets();
        s.planetTotal = all.size();
        s.starsFiltered = !charts.isEmpty();
        s.planetsFiltered = !planetKeys.isEmpty();
        if (starsOn) for (Row row : STAR_MATTER) if (charts.isEmpty() || charts.contains(row.tier)) s.stars.add(row);
        if (planetsOn) for (EyeRecipe r : all) if (planetKeys.isEmpty() || planetKeys.contains(r.key)) s.planets.add(r);
        return s;
    }

    /** "7 star matters, 12 of 43 planets (filtered by slot bus)", or with "star matter off" / "planets off". */
    public static String summary(Selection s) {
        final StringBuilder sb = new StringBuilder();
        if (!s.starsOn) sb.append("star matter off");
        else sb.append(s.stars.size())
            .append(s.stars.size() == 1 ? " star matter" : " star matters");
        sb.append(", ");
        if (!s.planetsOn) sb.append("planets off");
        else if (s.planetsFiltered) sb.append(s.planets.size())
            .append(" of ")
            .append(s.planetTotal)
            .append(" planets");
        else sb.append("all ")
            .append(s.planetTotal)
            .append(" planets");
        if ((s.starsOn && s.starsFiltered) || (s.planetsOn && s.planetsFiltered)) sb.append(" (filtered by slot bus)");
        return sb.toString();
    }
}
