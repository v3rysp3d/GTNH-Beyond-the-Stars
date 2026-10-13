package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.compat.BTSRackComponents;
import com.nateyburr.beyondthestars.items.SLCircuitItems;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import tectech.recipe.TecTechRecipeMaps;

/**
 * CIRCUITS v2 checks for {@code /btsrecipes circuits} (spec §7.2, phases C1 and C2). Read-only; op tooling.
 * <ol>
 * <li>C1: each circuit's ore names ({@link SLCircuitItems#oreReport()}).</li>
 * <li>Which item GT unifies each of our six circuit ore names to (it must stay a stock item, or every recipe that asks
 * for "any circuit of the tier" would show and name ours).</li>
 * <li>The chain: all 32 SL-X ids registered and live.</li>
 * <li><b>Slot fills</b>: our Mainframes in the ore-dict circuit slots of GT's research assembly lines (Emitter UMV's
 * {@code circuitExotic}, Field Generator UMV's {@code circuitCosmic} x4, Field Generator UXV's
 * {@code circuitTranscendent} x4), checked the way the Assembly Line checks a slot (item and meta against the
 * slot's alternatives, count against the slot's count), and a real recipe-map lookup ({@code findRecipeQuery}) per
 * tier with a stock recipe's circuit replaced by ours.</li>
 * <li>The nine Research Station entries (research item, computation, per second, EU/t, amperage; NEI page; the
 * assembly-line twin that the data stick points at).</li>
 * <li>The computer rack ({@link BTSRackComponents#report()}).</li>
 * <li>C4, whatever {@code stellarCircuits} says: the Piko raise SL-G10..G13
 * ({@link BTSStockCircuitRaise#report()}).</li>
 * </ol>
 * Ends with one {@code PASS}/{@code FAIL} summary line.
 */
public final class BTSCircuitChecks {

    private BTSCircuitChecks() {}

    public static List<String> run() {
        final List<String> out = new ArrayList<>();
        int fails = 0;
        fails += section(out, "ore names", SLCircuitItems.oreReport());
        // C4: the Piko raise does not depend on stellarCircuits.
        fails += section(out, "raise", BTSStockCircuitRaise.report());
        if (!Config.stellarCircuits) {
            out.add("stellarCircuits=false: no recipe, slot, research or rack check applies.");
            out.add(
                (fails == 0 ? "PASS" : "FAIL") + " circuits (stellarCircuits=false): " + fails + " section(s) failed");
            return out;
        }
        fails += section(out, "unification", unification());
        fails += section(out, "chain", chain());
        fails += section(out, "slots", slots());
        fails += section(out, "lookup", lookups());
        fails += section(out, "research", research());
        fails += section(out, "rack", BTSRackComponents.report());
        out.add((fails == 0 ? "PASS" : "FAIL") + " circuits C1+C2+C4: " + fails + " section(s) failed");
        return out;
    }

    private static int section(List<String> out, String name, List<String> lines) {
        out.add("-- " + name + " --");
        out.addAll(lines);
        final String last = lines.isEmpty() ? "" : lines.get(lines.size() - 1);
        return last.startsWith("PASS") ? 0 : 1;
    }

    // --------------------------------------------------------------------------------------- unification

    private static List<String> unification() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        for (int t = 0; t < SLCircuitItems.TIER_NAMES.length; t++) {
            final ItemStack main = GTOreDictUnificator.get(OrePrefixes.circuit, SLCircuitItems.tierMaterial(t), 1L);
            final boolean ours = main != null && isOurs(main.getItem());
            if (main == null || ours) bad++;
            lines.add(
                (main != null && !ours ? "ok   " : "BAD  ") + SLCircuitItems
                    .oreName(t) + " [" + SLCircuitItems.TIER_NAMES[t] + "] unifies to " + describe(main));
        }
        lines.add((bad == 0 ? "PASS" : "FAIL") + " unification: every circuit ore name keeps a stock target");
        return lines;
    }

    // -------------------------------------------------------------------------------------------- chain

    private static List<String> chain() {
        final List<String> lines = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        final List<String> idle = new ArrayList<>();
        final java.util.Set<String> live = new java.util.HashSet<>();
        for (BTSRecipeLedger.Added a : BTSRecipeLedger.allAdded()) {
            if (a.id.startsWith("SL-X")) live.add(a.id);
        }
        for (int i = 1; i <= BTSRecipesCircuits.RECIPE_COUNT; i++) {
            final String id = String.format("SL-X%02d", i);
            if (!BTSRecipeLedger.guardedIds()
                .contains(id)) missing.add(id);
            else if (!live.contains(id)) idle.add(id);
        }
        lines.add(
            "SL-X01..X" + BTSRecipesCircuits.RECIPE_COUNT
                + ": "
                + (BTSRecipesCircuits.RECIPE_COUNT - missing.size())
                + " registered, missing "
                + missing
                + ", registered but nothing live "
                + idle);
        lines.add(
            (missing.isEmpty() && idle.isEmpty() ? "PASS" : "FAIL") + " chain: "
                + BTSRecipesCircuits.RECIPE_COUNT
                + " recipes expected");
        return lines;
    }

    // -------------------------------------------------------------------------------------------- slots

    private static List<String> slots() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        bad += slot(lines, ItemList.Emitter_UMV, SLCircuitItems.siderealMainframe, Materials.UMV, 4);
        bad += slot(lines, ItemList.Field_Generator_UMV, SLCircuitItems.empyreanMainframe, Materials.UXV, 4);
        bad += slot(lines, ItemList.Field_Generator_UXV, SLCircuitItems.omegaMainframe, Materials.MAX, 4);
        lines.add((bad == 0 ? "PASS" : "FAIL") + " slots: our Mainframes fill GT's research assembly-line slots");
        return lines;
    }

    /**
     * Finds the assembly line making {@code output} and its slot that asks for {@code tier}'s circuit ore name, then
     * checks ours is one of that slot's alternatives at the slot's count, beside a stock circuit (so it is the generic
     * slot, not a named one).
     */
    private static int slot(List<String> lines, ItemList output, Item ours, Materials tier, int wantCount) {
        final ItemStack out = output.get(1);
        final String ore = String.valueOf(OrePrefixes.circuit.get(tier));
        for (GTRecipe.RecipeAssemblyLine line : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (line == null || line.mOutput == null || !GTUtility.areStacksEqual(line.mOutput, out, true)) continue;
            if (line.mOreDictAlt == null) continue;
            for (int i = 0; i < line.mOreDictAlt.length; i++) {
                final ItemStack[] alts = line.mOreDictAlt[i];
                if (alts == null || alts.length == 0 || !isCircuitOf(alts, tier)) continue;
                ItemStack mine = null;
                ItemStack stock = null;
                for (ItemStack alt : alts) {
                    if (alt == null) continue;
                    if (alt.getItem() == ours && alt.getItemDamage() == 0) mine = alt;
                    else if (!isOurs(alt.getItem()) && stock == null) stock = alt;
                }
                final boolean ok = mine != null && stock != null
                    && mine.stackSize == wantCount
                    && mine.stackSize == stock.stackSize;
                lines.add(
                    (ok ? "ok   " : "BAD  ") + describe(out)
                        + " slot "
                        + (i + 1)
                        + " ("
                        + ore
                        + ", "
                        + alts.length
                        + " alternatives): "
                        + (mine == null ? "ours NOT accepted" : mine.stackSize + "x " + describe(mine) + " accepted")
                        + ", stock alternative "
                        + describe(stock)
                        + (ok ? "" : " (want " + wantCount + "x ours beside a stock circuit)"));
                return ok ? 0 : 1;
            }
        }
        lines.add("BAD  " + describe(out) + ": no assembly line with a " + ore + " slot found");
        return 1;
    }

    /** True if the slot's alternatives are GT's circuit ore name of {@code tier} (by a stock member's association). */
    private static boolean isCircuitOf(ItemStack[] alts, Materials tier) {
        for (ItemStack alt : alts) {
            if (alt == null) continue;
            final ItemData data = GTOreDictUnificator.getAssociation(alt);
            if (data != null && data.mPrefix == OrePrefixes.circuit
                && data.mMaterial != null
                && data.mMaterial.mMaterial == tier) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------- lookups

    /** For UMV, UXV and MAX: a stock recipe-map recipe found again by GT's lookup with our Mainframe in its slot. */
    private static List<String> lookups() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        bad += lookup(lines, Materials.UMV, SLCircuitItems.siderealMainframe);
        bad += lookup(lines, Materials.UXV, SLCircuitItems.empyreanMainframe);
        bad += lookup(lines, Materials.MAX, SLCircuitItems.omegaMainframe);
        lines.add((bad == 0 ? "PASS" : "FAIL") + " lookup: GT's recipe lookup accepts our Mainframes as any-of-tier");
        return lines;
    }

    private static int lookup(List<String> lines, Materials tier, Item ours) {
        int tried = 0;
        // The Assembler first (a plain map), then every other real map.
        final List<RecipeMap<?>> maps = new ArrayList<>();
        maps.add(RecipeMaps.assemblerRecipes);
        for (RecipeMap<?> m : RecipeMap.ALL_RECIPE_MAPS.values()) if (m != RecipeMaps.assemblerRecipes) maps.add(m);
        for (RecipeMap<?> map : maps) {
            final String n = map.unlocalizedName.toLowerCase(java.util.Locale.ROOT);
            if (n.contains("fake") || n.contains("visual") || n.contains("nanochip") || n.contains("research"))
                continue;
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (recipe == null || recipe.mFakeRecipe || !recipe.mEnabled || recipe.mInputs == null) continue;
                final int slot = circuitSlot(recipe.mInputs, tier);
                if (slot < 0) continue;
                // The stock inputs must find the recipe first, or this recipe's lookup needs more than items/fluids.
                final ItemStack[] stock = copy(recipe.mInputs);
                if (find(map, stock, recipe) != recipe) continue;
                tried++;
                final ItemStack[] mine = copy(recipe.mInputs);
                mine[slot] = new ItemStack(ours, Math.max(1, recipe.mInputs[slot].stackSize), 0);
                final GTRecipe found = find(map, mine, recipe);
                final boolean ok = found == recipe;
                lines.add(
                    (ok ? "ok   " : "BAD  ") + map.unlocalizedName
                        + " -> "
                        + describe(recipe.mOutputs != null && recipe.mOutputs.length > 0 ? recipe.mOutputs[0] : null)
                        + ": "
                        + mine[slot].stackSize
                        + "x "
                        + describe(mine[slot])
                        + " in place of "
                        + describe(recipe.mInputs[slot])
                        + (ok ? " FOUND the same recipe" : " NOT found"));
                return ok ? 0 : 1;
            }
        }
        lines.add(
            "BAD  no stock recipe with a " + OrePrefixes.circuit.get(tier)
                + " input could be looked up ("
                + tried
                + " tried)");
        return 1;
    }

    private static int circuitSlot(ItemStack[] inputs, Materials tier) {
        for (int i = 0; i < inputs.length; i++) {
            final ItemStack in = inputs[i];
            if (in == null || in.stackSize <= 0 || isOurs(in.getItem())) continue;
            final ItemData data = GTOreDictUnificator.getAssociation(in);
            if (data != null && data.mPrefix == OrePrefixes.circuit
                && data.mMaterial != null
                && data.mMaterial.mMaterial == tier) return i;
        }
        return -1;
    }

    private static GTRecipe find(RecipeMap<?> map, ItemStack[] items, GTRecipe want) {
        try {
            final FluidStack[] fluids = recipeFluids(want);
            return map.findRecipeQuery()
                .items(items)
                .fluids(fluids)
                .voltage(Long.MAX_VALUE / 4)
                .filter(r -> r == want)
                .find();
        } catch (RuntimeException | LinkageError odd) {
            return null;
        }
    }

    private static FluidStack[] recipeFluids(GTRecipe recipe) {
        if (recipe.mFluidInputs == null) return new FluidStack[0];
        final FluidStack[] out = new FluidStack[recipe.mFluidInputs.length];
        for (int i = 0; i < out.length; i++) {
            final FluidStack f = recipe.mFluidInputs[i];
            out[i] = f == null ? null : new FluidStack(f.getFluid(), Math.max(1, f.amount));
        }
        return out;
    }

    private static ItemStack[] copy(ItemStack[] in) {
        final ItemStack[] out = new ItemStack[in.length];
        for (int i = 0; i < in.length; i++) {
            if (in[i] == null) continue;
            out[i] = in[i].copy();
            // A not-consumed input (count 0) still has to be present.
            if (out[i].stackSize <= 0) out[i].stackSize = 1;
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------ research

    /** The nine Research Station entries: {output, research item, total computation, per second, EU/t, amps}. */
    private static Object[][] researchTable() {
        final long uev = gregtech.api.enums.TierEU.RECIPE_UEV, uiv = gregtech.api.enums.TierEU.RECIPE_UIV,
            umv = gregtech.api.enums.TierEU.RECIPE_UMV;
        return new Object[][] {
            { SLCircuitItems.siderealAssembly, SLCircuitItems.siderealProcessor, 384_000L, 512L, uev, 16L },
            { SLCircuitItems.siderealSupercomputer, SLCircuitItems.siderealAssembly, 768_000L, 1_024L, uev, 32L },
            { SLCircuitItems.siderealMainframe, SLCircuitItems.siderealSupercomputer, 1_536_000L, 2_048L, uev, 64L },
            { SLCircuitItems.empyreanAssembly, SLCircuitItems.empyreanProcessor, 768_000L, 1_024L, uiv, 16L },
            { SLCircuitItems.empyreanSupercomputer, SLCircuitItems.empyreanAssembly, 1_536_000L, 2_048L, uiv, 32L },
            { SLCircuitItems.empyreanMainframe, SLCircuitItems.empyreanSupercomputer, 3_072_000L, 4_096L, uiv, 64L },
            { SLCircuitItems.omegaAssembly, SLCircuitItems.omegaProcessor, 1_536_000L, 2_048L, umv, 4L },
            { SLCircuitItems.omegaSupercomputer, SLCircuitItems.omegaAssembly, 3_072_000L, 4_096L, umv, 8L },
            { SLCircuitItems.omegaMainframe, SLCircuitItems.omegaSupercomputer, 6_144_000L, 4_096L, umv, 16L } };
    }

    private static List<String> research() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        for (Object[] row : researchTable()) {
            final Item out = (Item) row[0];
            final Item item = (Item) row[1];
            final long total = (Long) row[2], perSec = (Long) row[3], eut = (Long) row[4], amps = (Long) row[5];
            TecTechRecipeMaps.TTResearchStationALRecipe tt = null;
            int count = 0;
            for (TecTechRecipeMaps.TTResearchStationALRecipe r : TecTechRecipeMaps.researchableALRecipeList) {
                if (r != null && r.mOutput != null && r.mOutput.getItem() == out) {
                    tt = r;
                    count++;
                }
            }
            boolean ok = tt != null && count == 1
                && tt.mResearchItem != null
                && tt.mResearchItem.getItem() == item
                && tt.mComputation == total
                && tt.mComputationRequiredPerSec == perSec
                && tt.mEUt == eut
                && tt.mAmperage == amps;
            // NEI's Research Station page and the assembly-line twin the data stick names.
            boolean nei = false;
            for (GTRecipe r : TecTechRecipeMaps.researchStationFakeRecipes.getAllRecipes()) {
                if (r != null && r.mInputs != null
                    && r.mInputs.length > 0
                    && r.mInputs[0] != null
                    && r.mInputs[0].getItem() == item
                    && r.mOutputs != null
                    && r.mOutputs.length > 0
                    && r.mOutputs[0] != null
                    && r.mOutputs[0].getItem() == out) nei = true;
            }
            boolean twin = false;
            GTRecipe.RecipeAssemblyLine twinLine = null;
            if (tt != null) {
                for (GTRecipe.RecipeAssemblyLine l : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
                    if (l != null && l.getPersistentHash() == tt.getPersistentHash()
                        && l.mOutput != null
                        && l.mOutput.getItem() == out) {
                        twin = true;
                        twinLine = l;
                    }
                }
            }
            ok = ok && nei && twin;
            if (!ok) bad++;
            lines.add(
                (ok ? "ok   " : "BAD  ") + Item.itemRegistry.getNameForObject(out)
                    + " <- research "
                    + (tt == null || tt.mResearchItem == null ? "none" : describe(tt.mResearchItem))
                    + (tt == null ? ""
                        : ": " + tt.mComputation
                            + " computation at "
                            + tt.mComputationRequiredPerSec
                            + "/s, "
                            + tt.mEUt
                            + " EU/t x "
                            + tt.mAmperage
                            + " A; AL "
                            + tt.mDuration
                            + " t at "
                            + (twinLine == null ? "?" : String.valueOf(twinLine.mEUt))
                            + " EU/t, "
                            + tt.mInputs.length
                            + " items + "
                            + tt.mFluidInputs.length
                            + " fluids")
                    + (count > 1 ? " (" + count + " entries!)" : "")
                    + "; NEI page="
                    + nei
                    + ", data-stick twin="
                    + twin);
        }
        lines.add(
            (bad == 0 ? "PASS" : "FAIL") + " research: "
                + researchTable().length
                + " Research Station entries, "
                + bad
                + " problem(s)");
        return lines;
    }

    // ------------------------------------------------------------------------- "no card names ours" (gate dump)

    /**
     * Revision 3 (no special casing): every SL recipe outside the circuit chain (SL-X / SL-N) whose inputs NAME one
     * of our 28 circuit items, as opposed to taking it through an ore-dict slot that also takes stock items. One line
     * per offender; empty when clean. Used by {@code /btseye gates}' circuits section.
     */
    public static List<String> cardsNamingOurs() {
        final List<String> out = new ArrayList<>();
        for (BTSRecipeLedger.Added a : BTSRecipeLedger.allAdded()) {
            if (a.id.startsWith("SL-X") || a.id.startsWith("SL-N")) continue;
            if (a.gt != null && a.gt.mInputs != null) {
                for (ItemStack in : a.gt.mInputs) {
                    if (in != null && isOurs(in.getItem())) {
                        out.add(
                            a.id + " (" + (a.map == null ? "?" : a.map.unlocalizedName) + ") names " + describe(in));
                    }
                }
            }
            if (a.assemblyLine != null && a.assemblyLine.mInputs != null) {
                final ItemStack[][] alt = a.assemblyLine.mOreDictAlt;
                for (int i = 0; i < a.assemblyLine.mInputs.length; i++) {
                    final ItemStack[] alts = alt != null && i < alt.length ? alt[i] : null;
                    boolean stockToo = false;
                    boolean oursHere = false;
                    if (alts != null && alts.length > 0) {
                        for (ItemStack s : alts) {
                            if (s == null) continue;
                            if (isOurs(s.getItem())) oursHere = true;
                            else stockToo = true;
                        }
                    } else {
                        final ItemStack in = a.assemblyLine.mInputs[i];
                        oursHere = in != null && isOurs(in.getItem());
                    }
                    if (oursHere && !stockToo) {
                        out.add(a.id + " (assembly line) names " + describe(a.assemblyLine.mInputs[i]));
                    }
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------- helpers

    /** True for any of the 28 Circuits v2 items. */
    public static boolean isOurs(Item item) {
        return item != null && SLCircuitItems.all()
            .contains(item);
    }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return "none";
        String name;
        try {
            name = stack.getDisplayName();
        } catch (RuntimeException unnamed) {
            name = "?";
        }
        return name + " (" + Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage() + ")";
    }
}
