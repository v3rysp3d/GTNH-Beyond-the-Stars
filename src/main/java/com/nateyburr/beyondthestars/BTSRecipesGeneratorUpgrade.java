package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.ItemFieldRetrofitKit;
import com.nateyburr.beyondthestars.items.SLUpgradeItems;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * THE FIELD-GENERATOR UPGRADE PATH (decided 2026-10-13, B-30: "Add an upgrade path"; question B-83). One owner.
 * Called from {@code CommonProxy.loadComplete}, after SL-G01..G03 and before the BHC swap: GT's Condensate Assembler
 * recipes are not in the map yet when our postInit runs (RC-2 dev server), so the derivation waits for load-complete,
 * like SL-B01/B02. Its recipe ids are counted in the boot log's own summary line, not in the postInit one.
 *
 * <h2>Why</h2>
 *
 * The Eye's compression gate is 1:1 (Q-20): an Eye of tier N needs every Spacetime Compression Field Generator at tier
 * N. In 2.9.0-RC-2 each generator is a Condensate Assembler craft that consumes a Black Hole Compressor controller,
 * which {@link BTSBlackHoleSwap} turns into a Kerr Ergosphere Engine controller, and a Stabilisation Field Generator
 * holds a compression and a time generator. So every Eye tier step stranded 188 compression generators with 188 Kerr
 * controllers inside, and re-bought them. These recipes upgrade a generator in place instead, keeping its controller.
 *
 * <h2>What, and why it is fair against RC-2</h2>
 *
 * The inputs are DERIVED at load from GT's own recipes for tier t and tier t-1 (whatever the pack ships), so they
 * stay fair if GT re-prices the generators:
 * <ul>
 * <li><b>SL-U01..U08</b> (compression, t = 1..8) and <b>SL-U11..U18</b> (Time Dilation): the tier t-1 generator, plus
 * every item and condensate GT's tier-t recipe takes MORE of than its tier t-1 recipe (an item that changes identity
 * between tiers - the tier material's dense plate, a manipulator or metamaterial of a new tier - is taken in full;
 * nothing is refunded for what the old generator held). The controller, the Reinforced Spatial / Temporal casing and
 * the AE2 singularity cells are in both recipes, so they stay inside. Same machine tier, EU/t and duration as GT's
 * tier-t recipe: never faster than crafting from scratch, only cheaper.</li>
 * <li><b>SL-U21..U28</b> (Stabilisation): the tier t-1 generator, one Compression and one Time Dilation Retrofit Kit of
 * tier t (in place of the two generators GT's recipe takes; the old ones are inside, Kerr included), and the
 * stabilisation recipe's own difference. 23 loose generator parts would not fit the Condensate Assembler's 16 item
 * slots, hence the kits.</li>
 * <li><b>SL-U31..U38 / SL-U41..U48</b>: the kits - a Retrofit Frame plus exactly what SL-U0t / SL-U1t add to the old
 * generator, at GT's tier-t duration. The frame keeps the kit recipe from being a subset of GT's own generator recipe
 * (the Condensate Assembler finds recipes by items only).</li>
 * <li><b>SL-U00</b>: the Retrofit Frame (Assembler, x2).</li>
 * </ul>
 * Nanite tiers: every derived input keeps the nanite GT's tier-t recipe gives it; the old generator takes the nanite
 * of the casing it stands in for, and a kit the nanite GT gives the generator it upgrades.
 *
 * <h2>Config</h2>
 *
 * Registered whatever {@code replaceTecTechEyeOfHarmony} says (B-89): our Eye, its 1:1 gate and the tooltip line that
 * teaches the in-place upgrade exist with the flag off too, and the recipes are derived from GT's own generator
 * recipes,
 * so they also serve TecTech's Eye. With {@code removeBlackHoleCompressorRecipe} on or off the controller kept inside
 * is
 * whichever one the pack's recipe holds (Kerr or BHC); nothing here names either.
 */
public final class BTSRecipesGeneratorUpgrade {

    private BTSRecipesGeneratorUpgrade() {}

    private static final String[] FAMILY_LIST = { "SpacetimeCompressionFieldGeneratorTier",
        "TimeAccelerationFieldGeneratorTier", "StabilisationFieldGeneratorTier" };
    private static final int COMPRESSION = 0, TIME = 1, STABILISATION = 2;
    private static final String[] FAMILY_NAMES = { "Spacetime Compression", "Time Dilation", "Stabilisation" };
    /** The casing each family's generator is built in: the old generator takes its place (and its nanite). */
    private static final CustomItemList[] SHELLS = { CustomItemList.EOH_Reinforced_Spatial_Casing,
        CustomItemList.EOH_Reinforced_Temporal_Casing, CustomItemList.EOH_Infinite_Energy_Casing };
    private static final int MAX_ITEMS = 16, MAX_CONDENSATES = 4;

    /** Every recipe registered here, for {@code /btseye bec}. */
    private static final List<GTRecipe> OURS = new ArrayList<>();

    // ================================================================================== registration

    public static void register() {
        OURS.clear();
        final Snapshot[][] gt = new Snapshot[3][9];
        for (int f = 0; f < 3; f++) {
            for (int t = 0; t < 9; t++) {
                gt[f][t] = Snapshot.of(generator(f, t, 1));
            }
        }
        BTSRecipeGuard.recipe("SL-U00", BTSRecipesGeneratorUpgrade::frame);
        for (int t = 1; t <= ItemFieldRetrofitKit.TIERS; t++) {
            final int tier = t;
            BTSRecipeGuard.recipe(id(0, t), () -> upgrade(gt, COMPRESSION, tier));
            BTSRecipeGuard.recipe(id(1, t), () -> upgrade(gt, TIME, tier));
            BTSRecipeGuard.recipe(id(3, t), () -> kit(gt, COMPRESSION, tier));
            BTSRecipeGuard.recipe(id(4, t), () -> kit(gt, TIME, tier));
            BTSRecipeGuard.recipe(id(2, t), () -> stabilisation(gt, tier));
        }
        final int expected = 1 + 5 * ItemFieldRetrofitKit.TIERS;
        final int made = OURS.size() + 1;
        if (made == expected) {
            BeyondTheStars.LOG.info(
                "[BTS] Field-generator upgrade path: {} Condensate Assembler recipes + the Retrofit Frame (tier t-1 -> t, "
                    + "the controller stays inside), 0 skipped.",
                OURS.size());
        } else {
            BeyondTheStars.LOG.error(
                "[BTS] Field-generator upgrade path: only {} of {} recipes registered (see the SKIPPED lines above).",
                made,
                expected);
        }
    }

    private static String id(int series, int t) {
        return "SL-U" + series + t;
    }

    /** SL-U00: Field Generator Retrofit Frame x2 (Assembler). Pre-Eye materials only. */
    private static void frame() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Hexanite, 1L),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.SpaceTime, 4L),
                BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L))
            .fluidInputs(BTSRecipeGuard.nn(Materials.Infinity.getMolten(144L), "molten Infinity"))
            .itemOutputs(stack(SLUpgradeItems.fieldRetrofitKit, ItemFieldRetrofitKit.FRAME, 2))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_UIV)
            .addTo(assemblerRecipes);
    }

    /** SL-U0t / SL-U1t: the tier t-1 generator + GT's tier-t minus tier t-1 difference -> the tier t generator. */
    private static void upgrade(Snapshot[][] gt, int family, int t) {
        final Snapshot now = need(gt, family, t), before = need(gt, family, t - 1);
        final List<Part> parts = new ArrayList<>();
        parts.add(new Part(generator(family, t - 1, 1), now.naniteOf(shell(family))));
        parts.addAll(now.minus(before, Collections.<String>emptySet()));
        assemble(generator(family, t, 1), parts, now.condensatesMinus(before), now);
    }

    /** SL-U3t / SL-U4t: a Retrofit Frame + exactly SL-U0t's / SL-U1t's difference -> the Retrofit Kit of tier t. */
    private static void kit(Snapshot[][] gt, int family, int t) {
        final Snapshot now = need(gt, family, t), before = need(gt, family, t - 1);
        final List<Part> parts = new ArrayList<>();
        parts.add(new Part(stack(SLUpgradeItems.fieldRetrofitKit, ItemFieldRetrofitKit.FRAME, 1), NaniteTier.Carbon));
        parts.addAll(now.minus(before, Collections.<String>emptySet()));
        final int base = family == COMPRESSION ? ItemFieldRetrofitKit.COMPRESSION : ItemFieldRetrofitKit.TIME;
        assemble(
            ItemFieldRetrofitKit.kit(SLUpgradeItems.fieldRetrofitKit, base, t, 1),
            parts,
            now.condensatesMinus(before),
            now);
    }

    /**
     * SL-U2t: the tier t-1 Stabilisation generator + a Compression and a Time Dilation Retrofit Kit of tier t + GT's
     * stabilisation difference (without the two generators, which the kits upgrade in place) -> tier t.
     */
    private static void stabilisation(Snapshot[][] gt, int t) {
        final Snapshot now = need(gt, STABILISATION, t), before = need(gt, STABILISATION, t - 1);
        final ItemStack comp = generator(COMPRESSION, t, 1), time = generator(TIME, t, 1);
        final Set<String> carried = new TreeSet<>();
        carried.add(key(comp));
        carried.add(key(time));
        final List<Part> parts = new ArrayList<>();
        parts.add(new Part(generator(STABILISATION, t - 1, 1), now.naniteOf(shell(STABILISATION))));
        parts.add(
            new Part(
                ItemFieldRetrofitKit.kit(SLUpgradeItems.fieldRetrofitKit, ItemFieldRetrofitKit.COMPRESSION, t, 1),
                now.naniteOf(comp)));
        parts.add(
            new Part(
                ItemFieldRetrofitKit.kit(SLUpgradeItems.fieldRetrofitKit, ItemFieldRetrofitKit.TIME, t, 1),
                now.naniteOf(time)));
        parts.addAll(now.minus(before, carried));
        assemble(generator(STABILISATION, t, 1), parts, now.condensatesMinus(before), now);
    }

    /** Registers one Condensate Assembler recipe (16 item stacks, 4 condensates at most; GT must keep it). */
    private static void assemble(ItemStack output, List<Part> parts, List<FluidStack> condensates, Snapshot like) {
        final List<ItemStack> items = new ArrayList<>();
        final List<NaniteTier> tiers = new ArrayList<>();
        for (Part part : parts) {
            int left = part.stack.stackSize;
            while (left > 0) {
                final int n = Math.min(64, left);
                items.add(GTUtility.copyAmount(n, part.stack));
                tiers.add(part.nanite);
                left -= n;
            }
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalStateException(
                "the upgrade needs " + items.size() + " item stacks; the Condensate Assembler takes " + MAX_ITEMS);
        }
        if (condensates.size() > MAX_CONDENSATES) {
            throw new IllegalStateException(
                "the upgrade needs " + condensates.size()
                    + " condensates; the Condensate Assembler takes "
                    + MAX_CONDENSATES);
        }
        final Collection<GTRecipe> added = GTValues.RA.stdBuilder()
            .itemInputs(items.toArray(new ItemStack[0]))
            .fluidInputs(condensates.toArray(new FluidStack[0]))
            .itemOutputs(output)
            .metadata(NANITE_TIERS, tiers.toArray(new NaniteTier[0]))
            .duration(like.duration)
            .eut(like.eut)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);
        if (added == null || added.isEmpty()) {
            throw new IllegalStateException("GT rejected the Condensate Assembler recipe");
        }
        OURS.addAll(added);
    }

    // ===================================================================================== GT snapshot

    private static Snapshot need(Snapshot[][] gt, int family, int t) {
        final Snapshot s = gt[family][t];
        if (s == null) {
            throw new IllegalStateException(
                "no GT Condensate Assembler recipe makes the tier-" + t + " " + FAMILY_NAMES[family] + " generator");
        }
        return s;
    }

    private static ItemStack generator(int family, int t, int count) {
        final CustomItemList entry = CustomItemList.valueOf(FAMILY_LIST[family] + t);
        return BTSRecipeGuard.nn(entry.get(count), "CustomItemList." + entry.name());
    }

    private static ItemStack shell(int family) {
        return BTSRecipeGuard.nn(SHELLS[family].get(1), "CustomItemList." + SHELLS[family].name());
    }

    private static ItemStack stack(Item item, int meta, int count) {
        return new ItemStack(BTSRecipeGuard.nn(item, "SLUpgradeItems.fieldRetrofitKit"), count, meta);
    }

    /** Identity of a stack for the difference: item + metadata (NBT ignored, like GT's lookup). */
    private static String key(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage();
    }

    /** One input: a stack (any size; split into stacks of 64 when registered) and its nanite tier. */
    private static final class Part {

        final ItemStack stack;
        final NaniteTier nanite;

        Part(ItemStack stack, NaniteTier nanite) {
            this.stack = stack;
            this.nanite = nanite;
        }
    }

    /** GT's recipe for one generator, merged by item identity, in GT's input order. */
    private static final class Snapshot {

        final Map<String, ItemStack> items = new LinkedHashMap<>();
        final Map<String, NaniteTier> nanites = new LinkedHashMap<>();
        final Map<Fluid, Integer> condensates = new LinkedHashMap<>();
        int duration;
        long eut;
        NaniteTier lowest;

        /** The GT recipe that makes {@code output} (not one of ours), or null. */
        static Snapshot of(ItemStack output) {
            GTRecipe found = null;
            for (GTRecipe recipe : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
                if (recipe == null || recipe.mOutputs == null || recipe.mOutputs.length != 1) continue;
                if (OURS.contains(recipe)) continue;
                final ItemStack out = recipe.mOutputs[0];
                if (out == null || out.getItem() != output.getItem() || out.getItemDamage() != output.getItemDamage())
                    continue;
                if (found != null) {
                    BeyondTheStars.LOG.warn(
                        "[BTS] Upgrade path: more than one Condensate Assembler recipe makes {}; the first is used.",
                        BTSRecipeGuard.name(output));
                    break;
                }
                found = recipe;
            }
            if (found == null) return null;
            final Snapshot s = new Snapshot();
            final NaniteTier[] tiers = found.getMetadata(GTRecipeConstants.NANITE_TIERS);
            for (int i = 0; i < found.mInputs.length; i++) {
                final ItemStack in = found.mInputs[i];
                if (in == null || in.getItem() == null || in.stackSize <= 0) continue;
                final String k = key(in);
                final ItemStack had = s.items.get(k);
                if (had == null) s.items.put(k, GTUtility.copyAmountUnsafe(in.stackSize, in));
                else had.stackSize += in.stackSize;
                final NaniteTier tier = tiers != null && i < tiers.length ? tiers[i] : null;
                if (tier != null) {
                    final NaniteTier was = s.nanites.get(k);
                    if (was == null || tier.tier > was.tier) s.nanites.put(k, tier);
                    if (s.lowest == null || tier.tier < s.lowest.tier) s.lowest = tier;
                }
            }
            FluidStack[] fluids = found.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
            if (fluids == null) fluids = found.mFluidInputs;
            if (fluids != null) {
                for (FluidStack fluid : fluids) {
                    if (fluid == null || fluid.getFluid() == null) continue;
                    s.condensates.merge(fluid.getFluid(), fluid.amount, Integer::sum);
                }
            }
            s.duration = found.mDuration;
            s.eut = found.mEUt;
            return s;
        }

        NaniteTier naniteOf(ItemStack stack) {
            final NaniteTier tier = nanites.get(key(stack));
            if (tier != null) return tier;
            return lowest != null ? lowest : NaniteTier.Carbon;
        }

        /** What this recipe takes more of than {@code before}, skipping the {@code carried} items. */
        List<Part> minus(Snapshot before, Set<String> carried) {
            final List<Part> out = new ArrayList<>();
            for (Map.Entry<String, ItemStack> e : items.entrySet()) {
                if (carried.contains(e.getKey())) continue;
                final ItemStack had = before.items.get(e.getKey());
                final int more = e.getValue().stackSize - (had == null ? 0 : had.stackSize);
                if (more <= 0) continue;
                out.add(new Part(GTUtility.copyAmountUnsafe(more, e.getValue()), naniteOf(e.getValue())));
            }
            return out;
        }

        List<FluidStack> condensatesMinus(Snapshot before) {
            final List<FluidStack> out = new ArrayList<>();
            for (Map.Entry<Fluid, Integer> e : condensates.entrySet()) {
                final Integer had = before.condensates.get(e.getKey());
                final int more = e.getValue() - (had == null ? 0 : had);
                if (more > 0) out.add(new FluidStack(e.getKey(), more));
            }
            return out;
        }
    }

    // ===================================================================================== test bench

    /**
     * {@code /btseye bec} (B-83): every upgrade-path recipe the server holds, whether GT's own lookup with its inputs
     * finds it (nothing shadows it), and its size.
     */
    public static List<String> benchLines() {
        final List<String> out = new ArrayList<>();
        int unique = 0;
        for (GTRecipe r : OURS) {
            final GTRecipe found = TecTechRecipeMaps.condensateAssemblingRecipes.findRecipeQuery()
                .items(r.mInputs)
                .fluids(r.mFluidInputs)
                .find();
            if (found == r) unique++;
            final FluidStack[] cond = r.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
            out.add(
                "  " + BTSRecipeGuard.name(r.mOutputs[0])
                    + " <- "
                    + r.mInputs.length
                    + " item stacks, "
                    + (cond == null ? 0 : cond.length)
                    + " condensates, "
                    + r.mDuration / 20
                    + " s"
                    + (found == r ? ""
                        : "; LOOKUP finds "
                            + (found == null || found.mOutputs == null || found.mOutputs.length == 0 ? "nothing"
                                : BTSRecipeGuard.name(found.mOutputs[0]))));
        }
        out.add(
            0,
            "Upgrade path: " + OURS.size()
                + " Condensate Assembler recipe(s) (40 expected), "
                + unique
                + " found by GT's own lookup with their inputs.");
        return out;
    }
}
