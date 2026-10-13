package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

import gregtech.api.enums.TierEU;

/**
 * D-49: one live star the Eye can host (d49-build-plan.md §3.1, §6.1). The per-cycle table: what a 60 Eye-second
 * cycle costs (upkeep, EU) and makes (product), per parallel. The field constants are {@link TierParams}; this class
 * holds the fluids and amounts, which are the runtime's business. Built at load-complete by
 * {@code EyeRecipeRegistry.registerStars()} (the fluids must exist); looked up by key (NBT) or by chart (the
 * controller slot).
 * <p>
 * Every amount is tunable and PROPOSED (plan §6.1; T6's upkeep follows my answer that the Antipolarization
 * Agent is the magnetar's constant feed, B-37).
 */
public final class StarDef {

    /** Seconds of EU per cycle: {@code RECIPE_UMV × 4^(N−1) × 1,200} (plan §6.1). */
    public static final long CYCLE_TICKS = 1200;
    /** Ignition: cycles of upkeep and EU the buffers / network must hold (plan §6.3). */
    public static final int IGNITION_CYCLES = 10;
    /** Ignition seed cloud per gas: {@code 10^8 × 2^(N−1)} litres (× p). */
    public static final long IGNITION_SEED_BASE = 100_000_000L;

    /** Stable id, persisted in the tile while the star lives ("star:sol" ...). */
    public final String key;
    /** Display name. */
    public final String name;
    /** The field tier (gauges, drift, trims, events). */
    public final TierParams tier;
    /** The controller-slot chart. */
    public final ItemStack chart;
    /** Upkeep per 60 s cycle, per parallel, taken from the buffers when the cycle is charged. */
    public final List<EyeRecipe.FluidAmount> upkeep;
    /** The cycle's (T5: the batch's) product fluid. */
    public final Fluid product;
    /** T4: the fed fluid (HSM), drawn per second from the buffer at the feed setpoint; else null. */
    public final Fluid feed;
    /** T1-T3: the ignition seed cloud (H and He); empty for remnants. Per parallel. */
    public final List<EyeRecipe.FluidAmount> seed;
    /** T5: the batch charge; T6: the merger charge. Per parallel. Empty for T1-T4. */
    public final List<EyeRecipe.FluidAmount> charge;
    /** Wireless EU per cycle, per parallel. */
    public final long euPerCycle;
    /**
     * P4: upkeep per cycle and parallel in fractional litres (T7's Molten Magnetar Matter 7.5 L), paid with a carry so
     * the long-run mean is exact. Empty for T1-T6.
     */
    public final Map<Fluid, Double> exactUpkeep;

    public StarDef(String key, String name, TierParams tier, ItemStack chart, List<EyeRecipe.FluidAmount> upkeep,
        Fluid product, Fluid feed, List<EyeRecipe.FluidAmount> seed, List<EyeRecipe.FluidAmount> charge) {
        this(key, name, tier, chart, upkeep, Collections.emptyMap(), product, feed, seed, charge);
    }

    public StarDef(String key, String name, TierParams tier, ItemStack chart, List<EyeRecipe.FluidAmount> upkeep,
        Map<Fluid, Double> exactUpkeep, Fluid product, Fluid feed, List<EyeRecipe.FluidAmount> seed,
        List<EyeRecipe.FluidAmount> charge) {
        this.exactUpkeep = Collections.unmodifiableMap(new LinkedHashMap<>(exactUpkeep));
        this.key = key;
        this.name = name;
        this.tier = tier;
        this.chart = chart;
        this.upkeep = Collections.unmodifiableList(new ArrayList<>(upkeep));
        this.product = product;
        this.feed = feed;
        this.seed = Collections.unmodifiableList(new ArrayList<>(seed));
        this.charge = Collections.unmodifiableList(new ArrayList<>(charge));
        this.euPerCycle = euPerCycle(tier.n);
    }

    /** {@code RECIPE_UMV × 4^(N−1) × 1,200}: 1.51e11 at T1 .. 1.55e14 at T6. */
    public static long euPerCycle(int tierN) {
        return TierEU.RECIPE_UMV * (1L << (2 * (tierN - 1))) * CYCLE_TICKS;
    }

    /** Star tier 1..6. */
    public int n() {
        return tier.n;
    }

    /** Product per cycle at p = 1 and Y = r = 1 (T4: 0, it is 3.6864 × HSM fed; T5: per batch). */
    public double productBase() {
        return tier.productBase;
    }

    /** Every fluid this star takes from the Eye's buffers (upkeep, feed, seed, charge). */
    public List<Fluid> bufferedFluids() {
        final List<Fluid> out = new ArrayList<>();
        for (EyeRecipe.FluidAmount fa : upkeep) out.add(fa.fluid);
        for (EyeRecipe.FluidAmount fa : seed) out.add(fa.fluid);
        for (EyeRecipe.FluidAmount fa : charge) out.add(fa.fluid);
        out.addAll(exactUpkeep.keySet());
        if (feed != null) out.add(feed);
        return out;
    }

    /** P4: a galaxy-tier star (T7 Triangulum, T8 Milky Way, and their planets-mode fields). */
    public boolean galaxyTier() {
        return tier.burstDriven();
    }

    // ================================================================================== registry

    private static final Map<String, StarDef> BY_KEY = new LinkedHashMap<>();
    private static final Map<Item, StarDef> BY_CHART = new IdentityHashMap<>();

    /** Registers a star (load-complete, server and client alike). A later key replaces an earlier one. */
    public static void register(StarDef def) {
        BY_KEY.put(def.key, def);
        BY_CHART.put(def.chart.getItem(), def);
    }

    public static StarDef byKey(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    /** The star the controller-slot item selects, or null (a planet, an empty slot, anything else). */
    public static StarDef byChart(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        final StarDef def = BY_CHART.get(stack.getItem());
        return def != null && def.chart.getItemDamage() == stack.getItemDamage() ? def : null;
    }

    /**
     * P4: registers a variant of a star by key only (planets mode at T7 / T8 shares its galaxy's chart; the chart
     * still selects the galaxy, the Eye's mode picks the variant).
     */
    public static void registerVariant(StarDef def) {
        BY_KEY.put(def.key, def);
    }

    /** The registered star of tier {@code n}, or null (never a planets-mode variant). */
    public static StarDef ofTier(int n) {
        for (StarDef d : BY_KEY.values()) if (d.tier.n == n && !d.tier.planets()) return d;
        return null;
    }

    /** P4: the planets-mode variant of tier {@code n} (7 or 8), or null. */
    public static StarDef planetsOf(int n) {
        for (StarDef d : BY_KEY.values()) if (d.tier.n == n && d.tier.planets()) return d;
        return null;
    }

    public static Collection<StarDef> all() {
        return Collections.unmodifiableCollection(BY_KEY.values());
    }
}
