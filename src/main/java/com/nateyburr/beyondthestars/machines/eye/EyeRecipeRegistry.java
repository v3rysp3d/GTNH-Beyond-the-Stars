package com.nateyburr.beyondthestars.machines.eye;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BTSRecipeRemovals;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipe;
import tectech.TecTech;
import tectech.recipe.EyeOfHarmonyRecipe;
import tectech.recipe.EyeOfHarmonyRecipeStorage;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;
import tectech.util.FluidStackLong;
import tectech.util.ItemStackLong;

/**
 * SINGULARITY LADDER P3: the Eye of Harmony's recipe registry, the TecTech conversion and the TecTech
 * EOH removal (singularity-ladder-build.md §3.3, §4). Owned by the Eye unit.
 *
 * <h2>When it runs</h2>
 * {@link #onLoadComplete()} is the first line of {@code CommonProxy.loadComplete}, ABOVE the
 * Foundation's {@code BTSRecipeRemovals.run()}. FML dispatches load-complete in dependency order and
 * the {@code @Mod} string says {@code after:tectech}, so TecTech's {@code onLoadCompleted} has already
 * filled {@code TecTech.eyeOfHarmonyRecipeStorage}. The order inside is fixed:
 * <ol>
 * <li>the six D-49 live stars ({@link StarDef}, per 60 s cycle);</li>
 * <li>the <b>snapshot</b>: every TecTech planet recipe is read out of the storage's private
 * {@code recipeHashMap} by reflection and converted - always, whatever the config says;</li>
 * <li>only then, and only if {@code Config.replaceTecTechEyeOfHarmony} AND the snapshot converted at
 * least one planet AND the replacement stars registered (Sol, the neutron star and Sirius B),
 * TecTech's storage, NEI page and controller recipe are cleared. A failed
 * snapshot or a missing star leaves TecTech's EOH fully intact rather than deleting a source with no
 * replacement;</li>
 * <li>the NEI display page.</li>
 * </ol>
 * Every step is contained ({@link BTSRecipeRemovals#guard}): a moved field costs that step, never the
 * boot.
 *
 * <h2>Conversion (Q-12)</h2>
 * Same target item, TecTech's H/He requirement, TecTech's ores and plasmas, its base success
 * {@code 1 - 0.05R} and its Spacetime Compression tier (kept only as the baseline of the time reward;
 * the Eye's 1:1 tier gate alone decides whether a planet runs). Raw Star Matter and the special fluid
 * (WDM / BDM / Universium) are dropped ("planets without the matters", D-42): Sol is the only Raw
 * Star Matter source, the T4 neutron star the only WDM source and T5 Sirius B the only BDM source. Rocket tier R0-3 →
 * Eye T1, R4-6 → T2,
 * R7-8 → T3, the Deep Dark → T4. Times follow bh-model's ladder, {@code 20 min x 1.25^R}; the Deep
 * Dark is 240 min.
 *
 * <h2>Every amount is PROPOSED (Q-6, D-49 §6.1)</h2>
 * Each star consumes part of the previous star's product per cycle, so the tiers chain. Planet EU per
 * run = {@code RECIPE_UMV x 4^(tier-1) x base ticks}; a star's EU is the same rate per 60 s cycle,
 * wireless (Q-5).
 */
public final class EyeRecipeRegistry {

    private EyeRecipeRegistry() {}

    private static final long MINUTE = 20L * 60L;
    private static final long BILLION = 1_000_000_000L;

    /** Trigger item → recipe. Every target (star chart, dimension display block) is its own Item. */
    private static final Map<Item, EyeRecipe> BY_TRIGGER = new IdentityHashMap<>();
    private static final Map<String, EyeRecipe> BY_KEY = new LinkedHashMap<>();
    private static final Set<Fluid> INPUT_FLUIDS = new HashSet<>();
    private static boolean loaded;
    private static int converted;

    // ================================================================================= lookups

    /** The planet recipe the controller-slot item selects, or null (stars are {@link StarDef#byChart}). */
    public static EyeRecipe lookup(ItemStack target) {
        if (target == null || target.getItem() == null) return null;
        final EyeRecipe recipe = BY_TRIGGER.get(target.getItem());
        if (recipe == null) return null;
        return recipe.trigger.getItemDamage() == target.getItemDamage() ? recipe : null;
    }

    public static EyeRecipe byKey(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    public static Collection<EyeRecipe> all() {
        return Collections.unmodifiableCollection(BY_KEY.values());
    }

    /** Every fluid some recipe consumes: what the Eye's input sockets drain into its buffers. */
    public static boolean isInputFluid(Fluid fluid) {
        return fluid != null && INPUT_FLUIDS.contains(fluid);
    }

    public static Set<Fluid> inputFluids() {
        return Collections.unmodifiableSet(INPUT_FLUIDS);
    }

    /** Number of TecTech planet recipes converted at load-complete. */
    public static int convertedCount() {
        return converted;
    }

    // ============================================================================ load-complete

    /** Called once from {@code CommonProxy.loadComplete}, above {@code BTSRecipeRemovals.run()}. */
    public static void onLoadComplete() {
        if (loaded) return;
        loaded = true;
        BTSRecipeRemovals.guard("Eye of Harmony star recipes", EyeRecipeRegistry::registerStars);
        BTSRecipeRemovals.guard("Eye of Harmony TecTech planet conversion", EyeRecipeRegistry::convertTecTech);
        if (Config.replaceTecTechEyeOfHarmony) {
            if (converted > 0 && replacementStarsPresent()) {
                BTSRecipeRemovals.guard("TecTech Eye of Harmony recipes", EyeRecipeRegistry::clearTecTech);
                BTSRecipeRemovals.guard(
                    "TecTech Eye of Harmony controller",
                    () -> BTSRecipeRemovals.removeAssemblyLineByOutput(
                        CustomItemList.Machine_Multi_EyeOfHarmony.hasBeenSet()
                            ? CustomItemList.Machine_Multi_EyeOfHarmony.get(1)
                            : null,
                        "TecTech Eye of Harmony controller"));
            } else {
                BeyondTheStars.LOG.error(
                    "[BTS] replaceTecTechEyeOfHarmony=true, but {} TecTech planet recipes were converted and the "
                        + "replacement stars registered={}. TecTech's Eye of Harmony is LEFT INTACT so no planet or "
                        + "star-matter source disappears without a replacement.",
                    converted,
                    replacementStarsPresent());
            }
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] replaceTecTechEyeOfHarmony=false: TecTech's Eye of Harmony keeps its recipes and runs "
                    + "beside ours.");
        }
        BTSRecipeRemovals.guard("Eye of Harmony NEI page", EyeRecipeRegistry::registerDisplay);
        BeyondTheStars.LOG.info(
            "[BTS] Eye of Harmony: {} live stars, {} planet recipes ({} converted from TecTech).",
            StarDef.all()
                .size(),
            BY_KEY.size(),
            converted);
    }

    // =================================================================================== stars

    /**
     * D-49 (d49-build-plan.md §6.1): the stars are live, per-cycle {@link StarDef}s, not batch runs. Upkeep and
     * product per 60 s cycle at p = 1 (× p × mean Y × r). T1-T3 keep today's per-minute balance (H/He split 1:1 at
     * the same total, D-49 "constant supply of helium and hydrogen"); T4 is the neutron star (SpaceTime containment
     * plus the HSM feed, 3.6864 L of WDM per L of HSM); T5 Sirius B cools WDM into BDM in batches; T6 the magnetar is
     * held by a constant Antipolarization Agent feed (my answer, B-37). The T5/T6 runtime is unit 5.
     */
    private static void registerStars() {
        // fluidOrNull, never fluidOf: a missing fluid costs only the stars that use it (starDef() skips them).
        final Fluid hydrogen = fluidOrNull(Materials.Hydrogen.getGas(1L));
        final Fluid helium = fluidOrNull(Materials.Helium.getGas(1L));
        final Fluid rspm = fluidOrNull(Materials.RawStarMatter.getFluid(1L));
        final Fluid wdm = fluidOrNull(Materials.WhiteDwarfMatter.getMolten(1L));
        final Fluid bdm = fluidOrNull(Materials.BlackDwarfMatter.getMolten(1L));
        final Fluid spaceTime = fluidOrNull(Materials.SpaceTime.getMolten(1L));
        final List<EyeRecipe.FluidAmount> none = Collections.emptyList();

        starDef(
            "star:sol",
            "Sol",
            TierParams.T1,
            SLItems.starChartSol,
            list(in(hydrogen, 50_000_000L), in(helium, 50_000_000L)),
            rspm,
            null,
            seed(hydrogen, helium, 1),
            none);
        starDef(
            "star:siriusa",
            "Sirius A",
            TierParams.T2,
            SLItems.starChartSiriusA,
            list(in(hydrogen, 12_500_000L), in(helium, 12_500_000L), in(rspm, 25_000L)),
            SLFluids.excitedStellarPlasma,
            null,
            seed(hydrogen, helium, 2),
            none);
        starDef(
            "star:arcturus",
            "Arcturus",
            TierParams.T3,
            SLItems.starChartArcturus,
            list(in(hydrogen, 12_500_000L), in(helium, 12_500_000L), in(SLFluids.excitedStellarPlasma, 1_250L)),
            SLFluids.rawHyperdensePlasma,
            null,
            seed(hydrogen, helium, 3),
            none);
        starDef(
            "star:neutronstar",
            "Neutron Star (Arcturus Remnant)",
            TierParams.T4,
            SLItems.starChartNeutronStar,
            list(in(spaceTime, 144L)),
            wdm,
            SLFluids.hyperdenseStellarMixture,
            none,
            none);
        starDef(
            "star:siriusb",
            "Sirius B",
            TierParams.T5,
            SLItems.starChartSiriusB,
            none,
            bdm,
            null,
            none,
            list(in(wdm, 18_432L)));
        starDef(
            "star:magnetar",
            "SGR 1806-20 (Magnetar)",
            TierParams.T6,
            SLItems.starChartMagnetar,
            list(in(SLFluids.antipolarizationAgent, MAGNETAR_AGENT_PER_CYCLE)),
            SLFluids.magnetarMatter,
            null,
            none,
            list(in(wdm, 18_432L), in(bdm, 4_608L), in(SLFluids.antipolarizationAgent, 100_000L)));
    }

    /**
     * T6 upkeep: Antipolarization Agent per 60 s cycle (50 L/s), the magnetar's CONSTANT feed (my answer to
     * plan §17 item 5). The flare-shield Agent trim in the Field sockets comes on top. PROPOSED (B-37).
     */
    public static final long MAGNETAR_AGENT_PER_CYCLE = 3_000L;

    /** The T1-T3 ignition seed cloud: H and He, {@code 10^8 × 2^(N−1)} L each (plan §6.3). */
    private static List<EyeRecipe.FluidAmount> seed(Fluid hydrogen, Fluid helium, int tier) {
        final long amount = StarDef.IGNITION_SEED_BASE << (tier - 1);
        return list(in(hydrogen, amount), in(helium, amount));
    }

    private static void starDef(String key, String name, TierParams tier, Item chart,
        List<EyeRecipe.FluidAmount> upkeep, Fluid product, Fluid feed, List<EyeRecipe.FluidAmount> seed,
        List<EyeRecipe.FluidAmount> charge) {
        if (chart == null) {
            BeyondTheStars.LOG.error("[BTS] Eye star {} skipped: its star chart item is not registered.", key);
            return;
        }
        final List<EyeRecipe.FluidAmount> all = new ArrayList<>(upkeep);
        all.addAll(seed);
        all.addAll(charge);
        for (EyeRecipe.FluidAmount fa : all) {
            if (fa.fluid == null) {
                BeyondTheStars.LOG.error("[BTS] Eye star {} skipped: one of its fluids is not registered.", key);
                return;
            }
        }
        if (product == null || (tier.feed && feed == null)) {
            BeyondTheStars.LOG.error("[BTS] Eye star {} skipped: its product or feed fluid is not registered.", key);
            return;
        }
        final StarDef def = new StarDef(
            key,
            name,
            tier,
            new ItemStack(chart, 1, 0),
            upkeep,
            product,
            feed,
            seed,
            charge);
        StarDef.register(def);
        INPUT_FLUIDS.addAll(def.bufferedFluids());
    }

    /** {@code RECIPE_UMV x 4^(tier-1) x base ticks} (Q-5). */
    public static long euPerRun(int tier, long ticks) {
        return TierEU.RECIPE_UMV * (1L << (2 * (tier - 1))) * ticks;
    }

    // ======================================================================= TecTech conversion

    @SuppressWarnings("unchecked")
    private static Map<String, EyeOfHarmonyRecipe> tecTechMap() {
        final EyeOfHarmonyRecipeStorage storage = TecTech.eyeOfHarmonyRecipeStorage;
        if (storage == null) throw new IllegalStateException("TecTech.eyeOfHarmonyRecipeStorage is null");
        try {
            final Field field = EyeOfHarmonyRecipeStorage.class.getDeclaredField("recipeHashMap");
            field.setAccessible(true);
            return (Map<String, EyeOfHarmonyRecipe>) field.get(storage);
        } catch (ReflectiveOperationException moved) {
            throw new IllegalStateException("TecTech's EOH recipeHashMap moved: " + moved, moved);
        }
    }

    private static void convertTecTech() {
        final Fluid hydrogen = fluidOf(Materials.Hydrogen.getGas(1L), "hydrogen gas");
        final Fluid helium = fluidOf(Materials.Helium.getGas(1L), "helium gas");
        final Set<Fluid> stripped = strippedFluids();
        for (Map.Entry<String, EyeOfHarmonyRecipe> entry : tecTechMap().entrySet()) {
            final String dim = entry.getKey();
            final EyeOfHarmonyRecipe source = entry.getValue();
            if (source == null || source.getRecipeTriggerItem() == null
                || source.getRecipeTriggerItem()
                    .getItem() == null)
                continue;
            final long rocket = source.getRocketTier();
            final boolean deepDark = "DD".equals(dim) || rocket >= 9;
            final int tier = deepDark ? 4 : rocket <= 3 ? 1 : rocket <= 6 ? 2 : 3;
            final long ticks = deepDark ? 240 * MINUTE : Math.round(20 * MINUTE * Math.pow(1.25, rocket));

            final List<EyeRecipe.ItemAmount> items = new ArrayList<>();
            for (ItemStackLong out : source.getOutputItems()) {
                if (out == null || out.itemStack == null || out.stackSize <= 0) continue;
                final ItemStack one = out.itemStack.copy();
                one.stackSize = 1;
                items.add(new EyeRecipe.ItemAmount(one, out.stackSize));
            }
            final List<EyeRecipe.FluidAmount> fluids = new ArrayList<>();
            for (FluidStackLong out : source.getOutputFluids()) {
                if (out == null || out.fluidStack == null || out.amount <= 0) continue;
                final Fluid fluid = out.fluidStack.getFluid();
                if (fluid == null || stripped.contains(fluid)) continue;
                fluids.add(new EyeRecipe.FluidAmount(fluid, out.amount));
            }
            final ItemStack trigger = source.getRecipeTriggerItem()
                .copy();
            trigger.stackSize = 1;
            add(
                new EyeRecipe(
                    "planet:" + dim,
                    deepDark ? "The Deep Dark" : "Planet " + dim + " (R" + rocket + ")",
                    tier,
                    trigger,
                    list(in(hydrogen, source.getHydrogenRequirement()), in(helium, source.getHeliumRequirement())),
                    items,
                    fluids,
                    ticks,
                    euPerRun(tier, ticks),
                    source.getBaseRecipeSuccessChance(),
                    (int) Math.max(0, Math.min(8, source.getSpacetimeCasingTierRequired()))));
            converted++;
        }
    }

    /** Raw Star Matter and the three special matters: dropped from converted planets (Q-12). */
    private static Set<Fluid> strippedFluids() {
        final Set<Fluid> out = new HashSet<>();
        for (Materials m : new Materials[] { Materials.RawStarMatter, Materials.WhiteDwarfMatter,
            Materials.BlackDwarfMatter, Materials.Universium }) {
            for (FluidStack fs : new FluidStack[] { m.getFluid(1L), m.getMolten(1L), m.getGas(1L), m.getPlasma(1L) }) {
                if (fs != null && fs.getFluid() != null) out.add(fs.getFluid());
            }
        }
        return out;
    }

    /**
     * (b) empties TecTech's recipe storage, so its {@code recipeLookUp} finds nothing and placed
     * TecTech EOHs idle; (c) empties its NEI page.
     */
    private static void clearTecTech() {
        final Map<String, EyeOfHarmonyRecipe> map = tecTechMap();
        final int storage = map.size();
        map.clear();
        final Collection<GTRecipe> page = new ArrayList<>(TecTechRecipeMaps.eyeOfHarmonyRecipes.getAllRecipes());
        if (!page.isEmpty()) {
            TecTechRecipeMaps.eyeOfHarmonyRecipes.getBackend()
                .removeRecipes(page);
        }
        BeyondTheStars.LOG.info(
            "[BTS] removed TecTech Eye of Harmony recipes: storage {}, NEI {} (replaced by Beyond the Stars' Eye)",
            storage,
            page.size());
    }

    // ============================================================================ NEI display

    private static void registerDisplay() {
        int failed = 0;
        for (StarDef def : StarDef.all()) {
            try {
                displayStar(def);
            } catch (RuntimeException broken) {
                failed++;
                BeyondTheStars.LOG.warn("[BTS] Eye NEI page for {} skipped: {}", def.key, broken.toString());
            }
        }
        for (EyeRecipe recipe : BY_KEY.values()) {
            try {
                display(recipe);
            } catch (RuntimeException broken) {
                // One page, never the rest: the recipe itself still runs in the Eye.
                failed++;
                BeyondTheStars.LOG.warn("[BTS] Eye NEI page for {} skipped: {}", recipe.key, broken.toString());
            }
        }
        if (failed > 0) BeyondTheStars.LOG.warn("[BTS] {} Eye NEI pages skipped (see above).", failed);
    }

    /**
     * D-49 star page, per 60 s cycle at p = 1 (plan §12 NEI (a)): the chart in, the upkeep fluids in (up to 4), the
     * product out; the description has the EU, the ignition and the field rules. T4's HSM is fed per second at the
     * setpoint, shown here at the default (0.694 L/s = 41.64 L per cycle); T5 is per batch.
     */
    private static void displayStar(StarDef def) {
        final TierParams t = def.tier;
        final List<FluidStack> fluidsIn = new ArrayList<>();
        for (EyeRecipe.FluidAmount up : def.upkeep) {
            fluidsIn.add(new FluidStack(up.fluid, (int) Math.min(Integer.MAX_VALUE, up.amount)));
        }
        for (EyeRecipe.FluidAmount ch : def.charge) {
            if (fluidsIn.size() < 4)
                fluidsIn.add(new FluidStack(ch.fluid, (int) Math.min(Integer.MAX_VALUE, ch.amount)));
        }
        final double hsmPerCycle = 0.694 * StellarField.CYCLE_SECONDS;
        if (def.feed != null && fluidsIn.size() < 4) {
            fluidsIn.add(new FluidStack(def.feed, (int) Math.round(hsmPerCycle)));
        }
        final double product = t.feed ? StellarField.WDM_PER_HSM * hsmPerCycle : def.productBase();
        final List<String> desc = new ArrayList<>();
        desc.add("T" + t.n + " " + clip(def.name, 25));
        desc.add(t.batch ? "Per 7,200 s batch:" : "Per 60 s cycle (live star):");
        for (EyeRecipe.FluidAmount up : def.upkeep) {
            desc.add(
                "Upkeep " + compact(up.amount)
                    + " L "
                    + clip(up.fluid.getLocalizedName(new FluidStack(up.fluid, 1)), 13));
        }
        if (def.feed != null) desc.add("HSM feed 0-1.5 L/s (shown 0.694)");
        if (t.batch) desc.add("Charge per batch: WDM 18,432 L");
        if (!def.charge.isEmpty() && !t.batch) desc.add("Spawned by the Magnetar Merger");
        desc.add(sci(def.euPerCycle) + " EU/cycle, wireless");
        desc.add("Out x mean Y x upkeep r");
        if (t.ignite) {
            desc.add("Ignition: 1 Stellar Ignition");
            desc.add(" Charge + H/He " + compact(StarDef.IGNITION_SEED_BASE << (t.n - 1)) + " L");
        } else if (t.n == 4) {
            desc.add("Born by the Core Collapse");
        }
        desc.add("Field: " + gaugeList(t) + (t.event != null ? ", " + clip(t.event.name, 12) : ""));
        desc.add("x2 parallel per tier > T" + t.n);
        GTValues.RA.stdBuilder()
            .itemInputs(def.chart.copy())
            .fluidInputs(fluidsIn.toArray(new FluidStack[0]))
            .fluidOutputs(new FluidStack(def.product, (int) Math.max(1, Math.round(product))))
            .duration((int) StarDef.CYCLE_TICKS)
            .eut(0)
            .fake()
            .ignoreCollision()
            .setNEIDesc(desc.toArray(new String[0]))
            .addTo(EyeRecipeMaps.EYE);
    }

    private static String gaugeList(TierParams t) {
        final StringBuilder sb = new StringBuilder();
        for (int k : t.gauges()) {
            if (sb.length() > 0) sb.append('+');
            sb.append(StellarField.GAUGE_SYM[k]);
        }
        return sb.toString();
    }

    private static void display(EyeRecipe recipe) {
        final List<ItemStack> items = new ArrayList<>();
        for (EyeRecipe.ItemAmount out : recipe.itemOutputs) {
            if (items.size() >= 81) break;
            final ItemStack shown = out.stack.copy();
            shown.stackSize = (int) Math.min(Integer.MAX_VALUE, out.amount);
            items.add(shown);
        }
        final List<FluidStack> fluidsIn = new ArrayList<>();
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            fluidsIn.add(new FluidStack(in.fluid, (int) Math.min(Integer.MAX_VALUE, in.amount)));
        }
        final List<FluidStack> fluidsOut = new ArrayList<>();
        for (EyeRecipe.FluidAmount out : recipe.fluidOutputs) {
            if (fluidsOut.size() >= 18) break;
            fluidsOut.add(new FluidStack(out.fluid, (int) Math.min(Integer.MAX_VALUE, out.amount)));
        }
        // NEI does not wrap the description column (~28 characters), so the lines stay short. The
        // fluid slots above show the inputs; amounts past 2^31 L are clamped there, so the true
        // amounts are repeated here.
        final List<String> desc = new ArrayList<>();
        desc.add("T" + recipe.tier + " " + clip(recipe.name, 25));
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            desc.add(
                "In " + compact(in.amount) + " L " + clip(in.fluid.getLocalizedName(new FluidStack(in.fluid, 1)), 15));
        }
        desc.add(fmt(recipe.baseTicks / MINUTE) + " min" + (recipe.isPlanet() ? " before fields" : ""));
        desc.add(sci(recipe.euPerRun) + " EU/run, wireless");
        desc.add(
            recipe.isPlanet()
                ? "Success " + Math.round(recipe.baseSuccess * 100)
                    + "%, -3%/field > T"
                    + (recipe.compressionRequired + 1)
                : "Star: always succeeds");
        if (recipe.itemOutputs.size() > items.size()) {
            desc.add("+" + (recipe.itemOutputs.size() - items.size()) + " items not shown");
        }
        desc.add("x2 parallel per tier > T" + recipe.tier);
        GTValues.RA.stdBuilder()
            .itemInputs(recipe.trigger.copy())
            .itemOutputs(items.toArray(new ItemStack[0]))
            .fluidInputs(fluidsIn.toArray(new FluidStack[0]))
            .fluidOutputs(fluidsOut.toArray(new FluidStack[0]))
            .duration((int) Math.min(Integer.MAX_VALUE, recipe.baseTicks))
            .eut(0)
            .fake()
            .ignoreCollision()
            .setNEIDesc(desc.toArray(new String[0]))
            .addTo(EyeRecipeMaps.EYE);
    }

    // ================================================================================ helpers

    private static void add(EyeRecipe recipe) {
        BY_KEY.put(recipe.key, recipe);
        BY_TRIGGER.put(recipe.trigger.getItem(), recipe);
        for (EyeRecipe.FluidAmount fa : recipe.inputs) {
            INPUT_FLUIDS.add(fa.fluid);
        }
    }

    private static EyeRecipe.FluidAmount in(Fluid fluid, long amount) {
        return new EyeRecipe.FluidAmount(fluid, amount);
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        final List<T> out = new ArrayList<>();
        Collections.addAll(out, items);
        return out;
    }

    private static Fluid fluidOrNull(FluidStack stack) {
        return stack == null ? null : stack.getFluid();
    }

    /**
     * The stars that replace what TecTech's Eye was the pack's only source of (plan §10.4): Sol (Raw Star
     * Matter), the neutron star (White Dwarf Matter) and Sirius B (Black Dwarf Matter, T5). TecTech is only
     * cleared when every one of them registered.
     */
    private static boolean replacementStarsPresent() {
        return StarDef.byKey("star:sol") != null && StarDef.byKey("star:neutronstar") != null
            && StarDef.byKey("star:siriusb") != null;
    }

    private static Fluid fluidOf(FluidStack stack, String what) {
        if (stack == null || stack.getFluid() == null) {
            throw new IllegalStateException("the pack has no " + what);
        }
        return stack.getFluid();
    }

    static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    /** 1.0B / 2.5M / 18,432: short enough for the NEI column. */
    static String compact(long v) {
        if (v >= BILLION) return String.format(Locale.ROOT, "%.1fB", v / 1e9);
        if (v >= 1_000_000L) return String.format(Locale.ROOT, "%.1fM", v / 1e6);
        return fmt(v);
    }

    private static String clip(String s, int max) {
        return s == null ? "?" : s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }

    static String sci(long v) {
        return v < 1_000_000L ? fmt(v) : String.format(Locale.ROOT, "%.2e", (double) v);
    }
}
