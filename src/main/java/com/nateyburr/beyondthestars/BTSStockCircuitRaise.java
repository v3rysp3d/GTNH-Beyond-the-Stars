package com.nateyburr.beyondthestars;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.items.CircuitComponentFakeItem;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;

/**
 * CIRCUITS v2 §5 (design repo designs/docs/circuits-v2.md, revision 3): <b>the Piko raise, SL-G10..G13.</b> The NAC
 * Assembly Matrix steps that make GT's special circuits are rewritten IN PLACE at load-complete: each step's time x E
 * (EU/t unchanged, so its EU x E) and its items and fluids x M (rounded up), except the previous special circuit inside
 * it, which carries its own raise already. User: "I want the current pico's to be probably twice or 3x as expensive
 * than
 * they are right now"; factors "2.5x EU / 2x materials" (about 2.45x per circuit), "On by default".
 * <table>
 * <tr>
 * <th>Id</th>
 * <th>Output (CircuitComponent)</th>
 * <th>Not multiplied</th>
 * </tr>
 * <tr>
 * <td>SL-G10</td>
 * <td>PicoCircuit (Piko)</td>
 * <td>nothing (the Pico CPU x M too)</td>
 * </tr>
 * <tr>
 * <td>SL-G11</td>
 * <td>QuantumCircuit</td>
 * <td>Quantum CPU (it holds a Pico rack = raised Piko)</td>
 * </tr>
 * <tr>
 * <td>SL-G12</td>
 * <td>RawManifoldBud</td>
 * <td>nothing</td>
 * </tr>
 * <tr>
 * <td>SL-G13</td>
 * <td>PlanckCircuit</td>
 * <td>Planck CPU, Calabi-Yau Manifold (raised Quantum, raised Bud)</td>
 * </tr>
 * </table>
 * <b>How.</b> Every recipe of {@link RecipeMaps#nanochipAssemblyMatrixRecipes} whose first output is the component's
 * fake stack (item and meta) is rewritten: the map's emitter makes a hidden lookup recipe per ore-dict combination plus
 * one NEI recipe with the alternatives, so a component has two or more recipe objects and each is raised once. Found by
 * OUTPUT, so a reordered or rebalanced coremod recipe is still found; the kept inputs are found by component, not by
 * slot. Fresh arrays and copied stacks are written ({@code mInputs}, {@code mFluidInputs}, {@code mOreDictAlt}); no
 * shared stack is mutated. The item keys do not change (only counts), so the map's lookup index needs no rebuild. GT
 * caches a recipe's merged inputs on its first match after full load and throws if {@code mInputs} is replaced after
 * that, so a recipe whose cache is already set is skipped with a warning (never expected at load-complete).
 * Idempotent: a raised recipe is remembered (by identity) and never raised twice.
 * <p>
 * Single owner of SL-G10..G13. Called from {@code CommonProxy.loadComplete} inside {@link BTSRecipeRemovals#guard}.
 * With
 * {@code raiseStockSpecialCircuits=false} nothing is touched (stock RC-2 costs, 0 changes). Independent of
 * {@code stellarCircuits}. Each raised component is one "changed-GT" row of {@code /btsrecipes} (group Circuits) and
 * one boot log line with the old and new duration; {@link #report()} re-checks the live recipes for
 * {@code /btsrecipes circuits}.
 */
public final class BTSStockCircuitRaise {

    private BTSStockCircuitRaise() {}

    /** One raised step: its id, its name in logs, its output component and the inputs left at x1. */
    private static final class Step {

        final String id, name;
        final CircuitComponent output;
        final CircuitComponent[] keep;
        /** The duration before and after, as found at load-complete (-1: not raised). */
        int before = -1, after = -1, recipes;

        Step(String id, String name, CircuitComponent output, CircuitComponent... keep) {
            this.id = id;
            this.name = name;
            this.output = output;
            this.keep = keep;
        }

        boolean kept(ItemStack stack) {
            final CircuitComponent cc = component(stack);
            if (cc == null) return false;
            for (CircuitComponent k : keep) if (k == cc) return true;
            return false;
        }

        String keepText() {
            if (keep.length == 0) return "";
            final StringBuilder b = new StringBuilder(" (except ");
            for (int i = 0; i < keep.length; i++) {
                if (i > 0) b.append(" and ");
                b.append(keepName(keep[i]));
            }
            return b.append(')')
                .toString();
        }
    }

    private static final List<Step> STEPS = Collections.unmodifiableList(
        Arrays.asList(
            new Step("SL-G10", "Piko", CircuitComponent.PicoCircuit),
            new Step("SL-G11", "Quantum", CircuitComponent.QuantumCircuit, CircuitComponent.QuantumCPU),
            new Step("SL-G12", "Raw Manifold Bud", CircuitComponent.RawManifoldBud),
            new Step(
                "SL-G13",
                "Planck",
                CircuitComponent.PlanckCircuit,
                CircuitComponent.PlanckCPU,
                CircuitComponent.CalabiYauManifold)));

    /** Every recipe object raised so far (identity): the idempotence mark. */
    private static final Set<GTRecipe> RAISED = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final String FLAG = "raiseStockSpecialCircuits=true";

    // ============================================================================================ load-complete

    /** Called once from {@code CommonProxy.loadComplete}, inside {@link BTSRecipeRemovals#guard}. */
    public static void onLoadComplete() {
        if (!Config.raiseStockSpecialCircuits) {
            BeyondTheStars.LOG.info(
                "[BTS] raiseStockSpecialCircuits=false: the NAC's Piko, Quantum, Raw Manifold Bud and Planck steps keep"
                    + " their stock costs (SL-G10..G13: 0 recipes changed).");
            return;
        }
        final double e = Config.stockSpecialCircuitEnergyFactor, m = Config.stockSpecialCircuitMaterialFactor;
        int changed = 0;
        for (Step step : STEPS) {
            try {
                if (raise(step, e, m)) changed++;
            } catch (RuntimeException | LinkageError failed) {
                BeyondTheStars.LOG.error("[BTS] {}: the {} raise FAILED and was skipped.", step.id, step.name, failed);
            }
        }
        BeyondTheStars.LOG.info(
            "[BTS] Piko / Quantum / Planck raise (SL-G10..G13): {} of {} NAC Assembly Matrix steps raised (time x{},"
                + " items and fluids x{}).",
            changed,
            STEPS.size(),
            factor(e),
            factor(m));
    }

    /** Raises every matrix recipe that makes {@code step.output}. True if at least one recipe is raised now. */
    private static boolean raise(Step step, double e, double m) {
        final RecipeMap<?> map = RecipeMaps.nanochipAssemblyMatrixRecipes;
        final List<GTRecipe> found = matrixRecipes(step.output);
        if (found.isEmpty()) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: no NAC Assembly Matrix recipe makes the {} (component {}); nothing raised. The pack moved or"
                    + " dropped it.",
                step.id,
                step.name,
                step.output.name());
            return false;
        }
        GTRecipe ledgerRecipe = null;
        Map<String, Object> ledgerBefore = null;
        int raisedNow = 0, already = 0, cached = 0, t0 = -1, t1 = -1;
        for (GTRecipe recipe : found) {
            if (RAISED.contains(recipe)) {
                already++;
                continue;
            }
            if (inputCacheBuilt(recipe)) {
                cached++;
                BeyondTheStars.LOG.warn(
                    "[BTS] {}: a {} matrix recipe was already matched (GT cached its inputs), so it is left stock: {}",
                    step.id,
                    step.name,
                    recipe);
                continue;
            }
            final Map<String, Object> before = CommandBTSRecipes.snapshot(recipe);
            final int was = recipe.mDuration;
            apply(recipe, step, e, m);
            RAISED.add(recipe);
            raisedNow++;
            if (t0 < 0) {
                t0 = was;
                t1 = recipe.mDuration;
            } else if (t0 != was) {
                BeyondTheStars.LOG.warn(
                    "[BTS] {}: the {} matrix recipes had different durations ({} and {} t).",
                    step.id,
                    step.name,
                    t0,
                    was);
            }
            // One ledger row per step: the lookup recipe the machine runs, else the NEI one.
            if (ledgerRecipe == null || (ledgerRecipe.mFakeRecipe && !recipe.mFakeRecipe)) {
                ledgerRecipe = recipe;
                ledgerBefore = before;
            }
        }
        if (raisedNow == 0) {
            BeyondTheStars.LOG.info(
                "[BTS] {}: {} NAC Assembly Matrix recipe(s) for the {}: {} already raised, {} left stock (cached).",
                step.id,
                found.size(),
                step.name,
                already,
                cached);
            return false;
        }
        step.before = t0;
        step.after = t1;
        step.recipes = raisedNow;
        final String note = step.name + " (NAC Assembly Matrix): "
            + t0
            + " -> "
            + t1
            + " t, items and fluids x"
            + factor(m)
            + step.keepText();
        BTSRecipeLedger.changed(step.id, CommandBTSRecipes.CIRCUITS, FLAG, note, map, ledgerRecipe, ledgerBefore);
        BeyondTheStars.LOG.info(
            "[BTS] {}: {} (NAC Assembly Matrix) raised: {} -> {} t ({} -> {} s, EU/t unchanged, EU x{}), items and"
                + " fluids x{}{}; {} recipe object(s) (lookup + NEI page){}.",
            step.id,
            step.name,
            t0,
            t1,
            t0 / 20,
            t1 / 20,
            factor(e),
            factor(m),
            step.keepText(),
            raisedNow,
            cached > 0 ? ", " + cached + " left stock (cached)" : "");
        return true;
    }

    /** Rewrites one recipe: fresh input, fluid and alternative arrays, then the duration. */
    private static void apply(GTRecipe recipe, Step step, double e, double m) {
        recipe.mInputs = scaled(recipe.mInputs, step, m);
        if (recipe.mFluidInputs != null) {
            final FluidStack[] fluids = new FluidStack[recipe.mFluidInputs.length];
            for (int i = 0; i < fluids.length; i++) {
                final FluidStack f = recipe.mFluidInputs[i];
                if (f == null) continue;
                fluids[i] = f.copy();
                fluids[i].amount = times(f.amount, m);
            }
            recipe.mFluidInputs = fluids;
        }
        if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
            final GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
            if (alt.mOreDictAlt != null) {
                final ItemStack[][] slots = new ItemStack[alt.mOreDictAlt.length][];
                for (int i = 0; i < slots.length; i++) slots[i] = scaled(alt.mOreDictAlt[i], step, m);
                alt.mOreDictAlt = slots;
            }
        }
        recipe.mDuration = times(recipe.mDuration, e);
    }

    private static ItemStack[] scaled(ItemStack[] stacks, Step step, double m) {
        if (stacks == null) return null;
        final ItemStack[] out = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            final ItemStack s = stacks[i];
            if (s == null) continue;
            out[i] = s.copy();
            if (!step.kept(s)) out[i].stackSize = times(s.stackSize, m);
        }
        return out;
    }

    /** {@code n x f} rounded up, without float noise turning 8,800.0002 into 8,801. */
    static int times(int n, double f) {
        final double v = n * f;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(v - 1e-6 * Math.max(1.0, Math.abs(v))));
    }

    // ================================================================================================ helpers

    /** Every Assembly Matrix recipe (lookup and NEI) whose first output is {@code output}'s fake stack. */
    private static List<GTRecipe> matrixRecipes(CircuitComponent output) {
        final ItemStack fake = output.getFakeStack(1);
        final List<GTRecipe> found = new ArrayList<>();
        for (GTRecipe recipe : RecipeMaps.nanochipAssemblyMatrixRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null || recipe.mOutputs.length == 0) continue;
            final ItemStack out = recipe.mOutputs[0];
            if (out != null && out.getItem() == fake.getItem() && out.getItemDamage() == fake.getItemDamage()) {
                found.add(recipe);
            }
        }
        return found;
    }

    private static CircuitComponent component(ItemStack stack) {
        if (stack == null || stack.getItem() != CircuitComponentFakeItem.INSTANCE) return null;
        return CircuitComponent.tryGetFromFakeStack(stack);
    }

    private static String keepName(CircuitComponent cc) {
        switch (cc) {
            case QuantumCPU:
                return "the Quantum CPU";
            case PlanckCPU:
                return "the Planck CPU";
            case CalabiYauManifold:
                return "the Calabi-Yau Manifold";
            default:
                return cc.name();
        }
    }

    private static Field mergedInputCache;
    private static boolean cacheFieldLooked;

    /** True if GT already built this recipe's merged-input cache (replacing mInputs would then throw on next match). */
    private static boolean inputCacheBuilt(GTRecipe recipe) {
        if (!cacheFieldLooked) {
            cacheFieldLooked = true;
            try {
                mergedInputCache = GTRecipe.class.getDeclaredField("mergedInputCache");
                mergedInputCache.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException missing) {
                BeyondTheStars.LOG.warn(
                    "[BTS] SL-G10..G13: GTRecipe.mergedInputCache not found ({}); the raise assumes no recipe was"
                        + " matched yet (it runs at load-complete).",
                    missing.toString());
            }
        }
        if (mergedInputCache == null) return false;
        try {
            return mergedInputCache.get(recipe) != null;
        } catch (ReflectiveOperationException | RuntimeException odd) {
            return false;
        }
    }

    private static String factor(double f) {
        return f == Math.rint(f) ? String.valueOf((long) f) : String.valueOf((float) f);
    }

    // ================================================================================================ the check

    /**
     * The C4 check for {@code /btsrecipes circuits}: for each step, the live matrix recipes, their duration, whether
     * they were raised, and (with the raise on) that the duration is still the raised one; with the raise off, that no
     * SL-G1x ledger row exists. Also lists any recipe OUTSIDE the NAC that makes one of the four real items (it would
     * bypass the raise). Ends with a PASS/FAIL line.
     */
    public static List<String> report() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        try {
            final boolean on = Config.raiseStockSpecialCircuits;
            lines.add(
                "raiseStockSpecialCircuits=" + on
                    + (on
                        ? " (time x" + factor(Config.stockSpecialCircuitEnergyFactor)
                            + ", items and fluids x"
                            + factor(Config.stockSpecialCircuitMaterialFactor)
                            + ")"
                        : " (stock costs expected)"));
            int ledgerRows = 0;
            for (BTSRecipeLedger.Changed c : BTSRecipeLedger.allChanged()) {
                if (c.id.startsWith("SL-G1")) ledgerRows++;
            }
            for (Step step : STEPS) {
                final List<GTRecipe> live = matrixRecipes(step.output);
                int raised = 0;
                final Set<Integer> durations = new TreeSet<>();
                for (GTRecipe r : live) {
                    if (RAISED.contains(r)) raised++;
                    durations.add(r.mDuration);
                }
                final boolean ok;
                if (on) {
                    ok = !live.isEmpty() && raised == live.size()
                        && step.after > 0
                        && durations.size() == 1
                        && durations.contains(step.after);
                } else {
                    ok = !live.isEmpty() && raised == 0;
                }
                final String lookup = on && ok ? lookup(step, live) : "";
                final boolean lookupOk = !lookup.startsWith("BAD");
                if (!ok || !lookupOk) bad++;
                lines.add(
                    (ok && lookupOk ? "ok   " : "BAD  ") + step.id
                        + " "
                        + step.name
                        + ": "
                        + live.size()
                        + " matrix recipe(s), "
                        + raised
                        + " raised, duration now "
                        + durations
                        + " t"
                        + (step.before > 0 ? " (stock " + step.before + " t)" : "")
                        + lookup);
            }
            final boolean ledgerOk = on ? ledgerRows == STEPS.size() : ledgerRows == 0;
            if (!ledgerOk) bad++;
            lines.add(
                (ledgerOk ? "ok   " : "BAD  ") + "changed-GT rows SL-G10..G13: "
                    + ledgerRows
                    + " (expected "
                    + (on ? STEPS.size() : 0)
                    + ")");
            final List<String> bypass = bypasses();
            if (!bypass.isEmpty()) bad++;
            lines.add(
                (bypass.isEmpty() ? "ok   " : "BAD  ")
                    + "recipes outside the NAC making Piko / Quantum / Bud / Planck: "
                    + (bypass.isEmpty() ? "none" : bypass.toString()));
        } catch (RuntimeException | LinkageError odd) {
            bad++;
            lines.add("BAD  raise check failed: " + odd);
        }
        lines.add((bad == 0 ? "PASS" : "FAIL") + " raise: SL-G10..G13, " + bad + " problem(s)");
        return lines;
    }

    /**
     * GT's own lookup on the matrix map, as the module does it ({@code findRecipeQuery}): the raised amounts must find
     * a
     * recipe making the step's output; the stock amounts (each multiplied input divided by M again) must not.
     */
    private static String lookup(Step step, List<GTRecipe> live) {
        GTRecipe real = null;
        for (GTRecipe r : live) if (!r.mFakeRecipe) real = r;
        if (real == null) return "; BAD no lookup recipe (only the NEI page)";
        final double m = Config.stockSpecialCircuitMaterialFactor;
        final ItemStack[] stock = new ItemStack[real.mInputs.length];
        for (int i = 0; i < stock.length; i++) {
            final ItemStack s = real.mInputs[i];
            if (s == null) continue;
            stock[i] = s.copy();
            if (!step.kept(s)) stock[i].stackSize = Math.max(1, (int) Math.floor(s.stackSize / m));
        }
        final GTRecipe raised = RecipeMaps.nanochipAssemblyMatrixRecipes.findRecipeQuery()
            .items(real.mInputs)
            .fluids(real.mFluidInputs)
            .find();
        final GTRecipe atStock = RecipeMaps.nanochipAssemblyMatrixRecipes.findRecipeQuery()
            .items(stock)
            .fluids(real.mFluidInputs)
            .find();
        final boolean found = raised != null && matrixRecipes(step.output).contains(raised);
        final boolean ok = found && atStock == null;
        return "; " + (ok ? "lookup ok" : "BAD lookup")
            + " (raised amounts "
            + (found ? "found" : "NOT found")
            + ", stock amounts "
            + (atStock == null ? "rejected" : "still accepted")
            + ")";
    }

    /** Recipes in any other GT map (or a GT assembly line) whose output is one of the four real items. */
    private static List<String> bypasses() {
        final List<ItemStack> real = new ArrayList<>();
        for (Step step : STEPS) {
            final ItemStack r = CircuitComponent.tryGetRealStack(step.output.getFakeStack(1));
            if (r != null && r.getItem() != null) real.add(r);
        }
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            if (map == RecipeMaps.nanochipConversionRecipes) continue;
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (recipe == null || recipe.mOutputs == null) continue;
                for (ItemStack o : recipe.mOutputs) {
                    if (o != null && matchesAny(o, real)) out.merge(map.unlocalizedName, 1, Integer::sum);
                }
            }
        }
        for (GTRecipe.RecipeAssemblyLine line : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (line != null && line.mOutput != null && matchesAny(line.mOutput, real)) {
                out.merge("sAssemblylineRecipes", 1, Integer::sum);
            }
        }
        final List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : out.entrySet()) lines.add(e.getKey() + " x" + e.getValue());
        return lines;
    }

    private static boolean matchesAny(ItemStack stack, List<ItemStack> targets) {
        for (ItemStack t : targets) {
            if (stack.getItem() == t.getItem() && stack.getItemDamage() == t.getItemDamage()) return true;
        }
        return false;
    }
}
