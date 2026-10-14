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

import com.nateyburr.beyondthestars.BTSRecipeLedger;
import com.nateyburr.beyondthestars.BTSRecipeRemovals;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.CommandBTSRecipes;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.GalacticMergerOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.eye.star.EyeMilestones;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
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
    private static boolean tecTechCleared;

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

    /** True once TecTech's own Eye of Harmony recipes were cleared (the converted planets replace them). */
    public static boolean tecTechReplaced() {
        return tecTechCleared;
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
                        "TecTech Eye of Harmony controller",
                        new BTSRecipeRemovals.Ledger(
                            "SL-E01/removes-EOH",
                            CommandBTSRecipes.EYE,
                            "replaceTecTechEyeOfHarmony=true",
                            "Eye of Harmony (Beyond the Stars) controller (SL-E01); TecTech's planet recipes are converted "
                                + "to it")));
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
        registerGalaxies(wdm, bdm);
    }

    /**
     * P4 (quasar-p4.md §4.6-§4.8): Eye T7 Triangulum (upkeep Molten Magnetar Matter 7.5 L a cycle, the T6 "touch",
     * plus bursts; product Ergomatter 600 L × Y × r × min(β̄, 1.5); born by the Magnetar Collapse, whose charge is
     * shown
     * as its charge) and T8 Milky Way (the Ergomatter feed f₈ 0-10 L/s, default 5, as its feed; product Superdense
     * Ergomatter 1.0 × the feed × Y × r; born by the Galactic Merger). Planets mode at T7 / T8 is a variant of each,
     * registered by key only: the galaxy's chart picks the tier, the Eye's mode the field.
     */
    private static void registerGalaxies(Fluid wdm, Fluid bdm) {
        final List<EyeRecipe.FluidAmount> none = Collections.emptyList();
        final Fluid magnetar = SLFluids.magnetarMatter;
        final Fluid ergo = SLQuasarFluids.ergomatter;
        final Fluid superdense = SLQuasarFluids.superdenseErgomatter;
        if (magnetar == null || ergo == null || superdense == null || wdm == null || bdm == null) {
            BeyondTheStars.LOG.error(
                "[BTS] Eye T7/T8 skipped: Magnetar Matter, Ergomatter or Superdense Ergomatter is"
                    + " not registered.");
            return;
        }
        final Map<Fluid, Double> t7Upkeep = new LinkedHashMap<>();
        t7Upkeep.put(magnetar, GalaxyField.T7_MAGNETAR_UPKEEP);
        galaxyDef(
            "star:triangulum",
            "Triangulum (M33)",
            TierParams.T7,
            SLQuasarItems.starChartTriangulum,
            t7Upkeep,
            ergo,
            null,
            list(in(wdm, EyeStarRuntime.MC_WDM), in(bdm, EyeStarRuntime.MC_BDM)),
            false);
        galaxyDef(
            "star:milkyway",
            "Milky Way",
            TierParams.T8,
            SLQuasarItems.starChartMilkyWay,
            Collections.emptyMap(),
            superdense,
            ergo,
            list(in(ergo, EyeStarRuntime.GM_ERGO)),
            false);
        galaxyDef(
            "star:planets7",
            "Triangulum: 3 planets",
            TierParams.PLANETS7,
            SLQuasarItems.starChartTriangulum,
            Collections.emptyMap(),
            null,
            null,
            none,
            true);
        galaxyDef(
            "star:planets8",
            "Milky Way: 6 planets",
            TierParams.PLANETS8,
            SLQuasarItems.starChartMilkyWay,
            Collections.emptyMap(),
            null,
            null,
            none,
            true);
    }

    private static void galaxyDef(String key, String name, TierParams tier, Item chart, Map<Fluid, Double> exact,
        Fluid product, Fluid feed, List<EyeRecipe.FluidAmount> charge, boolean variant) {
        if (chart == null) {
            BeyondTheStars.LOG.error("[BTS] Eye star {} skipped: its star chart item is not registered.", key);
            return;
        }
        final StarDef def = new StarDef(
            key,
            name,
            tier,
            new ItemStack(chart, 1, 0),
            Collections.emptyList(),
            exact,
            product,
            feed,
            Collections.emptyList(),
            charge);
        if (variant) StarDef.registerVariant(def);
        else StarDef.register(def);
        INPUT_FLUIDS.addAll(def.bufferedFluids());
    }

    /**
     * T6 upkeep: Antipolarization Agent per 60 s cycle (50 L/s), the magnetar's CONSTANT feed (my answer to
     * plan §17 item 5). It also charges the flare shield (2·r A/s, user ruling 2026-10-13, B-60); the Agent trim in the
     * Field sockets comes on top. PROPOSED (B-37).
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
            // /btsrecipes dump shows TecTech's recipe as the "before" of this conversion.
            BTSRecipeLedger.tecTechPlanet("planet:" + dim, source);
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
                    (int) Math.max(0, Math.min(8, source.getSpacetimeCasingTierRequired())),
                    deepDark ? 9 : (int) Math.max(0, Math.min(9, rocket))));
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
        tecTechCleared = true;
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
        // D-49 unit 5: the transition and stability-field pages
        for (EyeTransition.Type type : EyeTransition.Type.values()) {
            try {
                displayTransition(type);
            } catch (RuntimeException broken) {
                failed++;
                BeyondTheStars.LOG.warn("[BTS] Eye transition page {} skipped: {}", type, broken.toString());
            }
        }
        for (StarDef def : StarDef.all()) {
            try {
                displayField(def);
            } catch (RuntimeException broken) {
                failed++;
                BeyondTheStars.LOG.warn("[BTS] Eye field page for {} skipped: {}", def.key, broken.toString());
            }
        }
        if (failed > 0) BeyondTheStars.LOG.warn("[BTS] {} Eye NEI pages skipped (see above).", failed);
    }

    /**
     * D-49 transition page (plan §12 NEI (b)): the chart that goes in the slot (not consumed) and the core in, the
     * event fluids in, the new star's product out; the milestones and the rules in the description.
     */
    private static void displayTransition(EyeTransition.Type type) {
        final StarDef from = StarDef.ofTier(type.from), to = StarDef.byKey(type.dest);
        final Item core = EyeStarRuntime.coreFor(type.from);
        if (to == null || core == null) return;
        final List<FluidStack> fluids = new ArrayList<>();
        final List<String> desc = new ArrayList<>();
        final String fromName = from == null ? "T" + type.from : clip(from.name, 14);
        desc.add("T" + type.from + " -> T" + (type.from + 1) + ": " + type.title);
        desc.add("Chart in the slot, core in");
        desc.add(" escrow (consumed on");
        desc.add(" success, first time only)");
        desc.add("Milestones on " + fromName + ":");
        switch (type) {
            case MASS_INFUSION:
            case SHELL_IGNITION: {
                final boolean mi = type == EyeTransition.Type.MASS_INFUSION;
                desc.add(
                    " " + compact((long) EyeMilestones.PROD_NEED[type.from])
                        + " L made, Resonant "
                        + fmt(EyeMilestones.RES_NEED[type.from])
                        + " s");
                desc.add(fromName + " lit, Stable or better;");
                desc.add(" the fluid taken at start");
                desc.add(mi ? "600 s live, P drift +4" : "603 s: Θ ramps in, T3 trims");
                if (!mi) desc.add(" forced helium flash at 600 s");
                desc.add("Survive: it becomes " + clip(to.name, 9));
                break;
            }
            case CORE_COLLAPSE:
                fluids.add(new FluidStack(EyeStarRuntime.timeFluid(), 94_000));
                // the bounce absorbs up to 90,000 L of SpaceTime on top of the neutron star's first cycle
                fluids.add(new FluidStack(EyeStarRuntime.spaceTimeFluid(), 90_000));
                desc.add(
                    " " + compact((long) EyeMilestones.PROD_NEED[3])
                        + " L, "
                        + fmt(EyeMilestones.RES_NEED[3])
                        + " s, "
                        + EyeMilestones.FLASHES_NEED
                        + " flashes");
                desc.add("1: Time <=100 L/s, 94,000 L");
                desc.add("2: bounce +" + Math.round(CollapseOverlay.BOUNCE_SHOCK) + ", -1 per");
                desc.add(" 1,000 L SpaceTime (<=90k)");
                desc.add(" P or Θ > +100: SUPERNOVA");
                desc.add("3: Stable 60 s within 180 s");
                desc.add("Re-runs need no core");
                break;
            case FIRST_COOLING:
                desc.add(
                    " " + compact((long) EyeMilestones.PROD_NEED[4])
                        + " L WDM (Stable), "
                        + fmt(EyeMilestones.RES_NEED[4])
                        + " s");
                desc.add(" Resonant, 1 Deep Dark run");
                desc.add("The first Sirius B batch:");
                desc.add(" mean Y >= 0.9, no fracture");
                desc.add("Fail: BDM salvaged, the");
                desc.add(" next charge tries again");
                break;
            case MAGNETAR_COLLAPSE:
                desc.add(" Stability Link 30 min (1.5");
                desc.add(" Bu/t = 1.5e13 EU/t on the rail,");
                desc.add(" high bank 25-75%)");
                desc.add(
                    " " + compact((long) EyeMilestones.PROD_NEED[6])
                        + " L Stable, "
                        + fmt(EyeMilestones.RES_NEED[6])
                        + " s, "
                        + EyeMilestones.FLARES_NEED
                        + " flares");
                desc.add("Rail ports: 1+ high, 1+ low");
                desc.add("Begin: GUI, Begin hatch, OC");
                desc.add("1: overfeed 300 s (charge)");
                desc.add("2: shock P +" + Math.round(MagnetarCollapseOverlay.DEFAULT_SHOCK) + ", -10 per");
                desc.add(" Bu/t above 1.5 that second");
                desc.add(" (1 Bu/t = 1e13 EU/t)");
                desc.add(" (time a purge onto it)");
                desc.add("3: capture: Stable 60 s");
                desc.add(" within 420-600 s");
                desc.add("Re-runs: no core, no link");
                break;
            case GALACTIC_MERGER:
                desc.add(" Heavy Link 30 min (6 Bu/t");
                desc.add(" = 6.0e13 EU/t, >= 50% on");
                desc.add(" the halo ports)");
                desc.add(
                    " " + compact((long) EyeMilestones.PROD_NEED[7])
                        + " L Stable, "
                        + fmt(EyeMilestones.RES_NEED[7])
                        + " s, "
                        + EyeMilestones.MERGERS_NEED
                        + " mergers");
                desc.add("All 8 halo ports; galaxy");
                desc.add(" mode, no companions");
                desc.add("Begin: GUI, Begin hatch, OC");
                desc.add(GalacticMergerOverlay.DURATION + " s: demand 1.5 -> 6 Bu/t");
                desc.add(" (1.5e13 -> 6.0e13 EU/t)");
                desc.add("3 passes (600/1200/1800 s):");
                desc.add(" W +-12, P +5 for 5 s");
                desc.add("Then Stable 120 s in 300 s");
                desc.add("Re-runs: no core, no link");
                break;
            case MAGNETAR_MERGER:
            default:
                desc.add(" " + EyeMilestones.GOOD_BATCHES_NEED + " batches, mean Y >= 0.9");
                desc.add("No star lit. Imprint: hold");
                desc.add(" Π in +60..+90 for 120 s");
                desc.add(" (<=600 s); release: Π");
                desc.add(" inside ±40 within 60 s");
                desc.add("Every re-spawn: the charge");
                break;
        }
        desc.add("Fail: star lost, core kept");
        // the event fluids plus the new star's first cycle (p = 1, default feed): the same amounts the controller
        // waits for before it begins (EyeStarRuntime.transitionNeeds)
        for (Map.Entry<Fluid, Long> need : EyeStarRuntime.transitionNeeds(type, 1, EyeStarRuntime.FEED_DEFAULT)
            .entrySet()) {
            if (need.getKey() == null) return;
            final FluidStack have = findFluid(fluids, need.getKey());
            final long litres = need.getValue() + (have == null ? 0 : have.amount);
            if (have != null) fluids.remove(have);
            fluids.add(new FluidStack(need.getKey(), (int) Math.min(Integer.MAX_VALUE, litres)));
        }
        desc.add("Buffers also hold the new");
        desc.add(" star's first cycle (x p)");
        for (FluidStack f : fluids) if (f.getFluid() == null) return;
        final double product = to.tier.feed ? StellarField.WDM_PER_HSM * 0.694 * StellarField.CYCLE_SECONDS
            : to.tier.ergoFeed ? GalaxyField.T8_RATIO * GalaxyField.F8_DEFAULT * StellarField.CYCLE_SECONDS
                : to.productBase();
        GTValues.RA.stdBuilder()
            .itemInputs(to.chart.copy(), new ItemStack(core, 1, 0))
            .fluidInputs(fluids.toArray(new FluidStack[0]))
            .fluidOutputs(new FluidStack(to.product, (int) Math.max(1, Math.round(product))))
            .duration(20)
            .eut(0)
            .fake()
            .ignoreCollision()
            .setNEIDesc(desc.toArray(new String[0]))
            .addTo(EyeRecipeMaps.TRANSITION);
    }

    private static FluidStack findFluid(List<FluidStack> list, Fluid fluid) {
        for (FluidStack f : list) if (f.getFluid() == fluid) return f;
        return null;
    }

    /**
     * D-49 stability-field page (plan §12 NEI (c)): the chart in, one unit of each trim fluid in, the effects per
     * unit and the caps (per second, x parallel) in the description.
     */
    private static void displayField(StarDef def) {
        final TierParams t = def.tier;
        final List<FluidStack> fluids = new ArrayList<>();
        final List<String> desc = new ArrayList<>();
        desc.add("T" + t.n + " " + clip(def.name, 25));
        desc.add("Gauges " + gaugeList(t) + ", 0 is the target");
        if (t.event != null) {
            desc.add(
                clip(t.event.name, 12) + " every "
                    + Math.round(t.event.everyMin)
                    + "-"
                    + Math.round(t.event.everyMax)
                    + " s");
        }
        if (t.batch) desc.add("Θ follows +90 -> -90 / 7,200 s");
        if (t.shield) desc.add("Agent upkeep + trim charge A");
        if (t.burstDriven()) {
            // P4: bursts drive the galaxy field (quasar-p4.md §4.2)
            desc.add(t.planets() ? "P: +1 x (β - 1), bursts heat" : "P: +3 x (β - 1), bursts heat");
            if (t.galaxy) {
                desc.add("W: 1.5 x s, +6 (γlo - γhi)");
                desc.add(" aim the rail's two banks");
            }
            if (t.ergoFeed) desc.add("Λ: 0.4 x f8, -10 γhalo");
            desc.add("β = bursts / (D x m x p)");
        }
        desc.add("Trims, Field sockets only:");
        for (TrimDef trim : t.trims()) {
            final Fluid fluid = EyeStarRuntime.trimFluid(trim.kind);
            if (fluid == null) continue;
            if (fluids.size() < 6) fluids.add(new FluidStack(fluid, trim.kind.unitLitres));
            final StringBuilder fx = new StringBuilder(clip(trim.kind.key, 6)).append(':');
            for (int k = 0; k < 4; k++) {
                final double v = trim.fx(k);
                if (v == 0) continue;
                fx.append(' ')
                    .append(StellarField.GAUGE_SYM[k])
                    .append(v > 0 ? "+" : "")
                    .append(v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
            }
            if (trim.kind == TrimDef.Kind.AGENT) fx.append(" A+4");
            fx.append(" cap ")
                .append(Math.round(trim.cap));
            desc.add(fx.toString());
        }
        desc.add("Per unit shown; cap units/s");
        desc.add(" x parallel; not buffered");
        if (fluids.isEmpty()) return;
        GTValues.RA.stdBuilder()
            .itemInputs(def.chart.copy())
            .fluidInputs(fluids.toArray(new FluidStack[0]))
            .duration(20)
            .eut(0)
            .fake()
            .ignoreCollision()
            .setNEIDesc(desc.toArray(new String[0]))
            .addTo(EyeRecipeMaps.FIELD);
    }

    /**
     * D-49 star page, per 60 s cycle at p = 1 (plan §12 NEI (a)): the chart in, the upkeep fluids in (up to 4), the
     * product out; the description has the EU, the ignition and the field rules. T4's HSM is fed per second at the
     * setpoint, shown here at the default (0.694 L/s = 41.64 L per cycle); T5 is per batch.
     */
    private static void displayStar(StarDef def) {
        if (def.galaxyTier()) {
            displayGalaxy(def);
            return;
        }
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
        if (t.shield) {
            desc.add("Dwarf top-up: WDM " + compact(EyeStarRuntime.TOPUP_WDM));
            desc.add(" + BDM " + EyeStarRuntime.TOPUP_BDM + " L per " + EyeStarRuntime.TOPUP_CYCLES + " cycles");
        }
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

    /**
     * P4 star page per 60 s cycle at p = 1 (quasar-p4.md §4.6-§4.8): T7 Triangulum (Magnetar Matter 7.5 L in, shown
     * rounded up; Ergomatter 600 L out before × Y × r × min(β̄, 1.5)), T8 Milky Way (the Ergomatter feed at its default
     * 5 L/s = 300 L a cycle in, Superdense Ergomatter 300 L out before × Y × r), and planets mode (the chart; the
     * slots'
     * rules). Bursts are not an item: the description gives the demand.
     */
    private static void displayGalaxy(StarDef def) {
        final TierParams t = def.tier;
        final List<String> desc = new ArrayList<>();
        final List<FluidStack> in = new ArrayList<>();
        final List<FluidStack> out = new ArrayList<>();
        desc.add("T" + t.n + " " + clip(def.name, 25));
        if (t.planets()) {
            desc.add("Planets mode: " + t.slots + " slots");
            desc.add("The galaxy's chart + mode");
            desc.add(" planets (GUI, OC setMode)");
            desc.add("Slot bus: 1 planet item each");
            desc.add("Each: parallel 2^(N-k),");
            desc.add(" its H/He and EU at start");
            desc.add("Success/parallel: (1-0.05R)");
            desc.add(" x field (Res/Stable 1,");
            desc.add(" Strained 0.5, Critical 0)");
            desc.add("Field: P only, d = (β-1)");
            desc.add("Bursts: D " + String.format(Locale.ROOT, "%.3f", GalaxyField.demand(t.n, true, 0)) + " Bu/t x p");
            desc.add(
                " (" + com.nateyburr.beyondthestars.machines.gui.BTSFormat.euOfBu(GalaxyField.demand(t.n, true, 0))
                    + " x p)");
            desc.add("Breach: runs fail, field");
            desc.add(" re-forms (nothing else)");
            desc.add(sci(def.euPerCycle) + " EU/cycle, wireless");
        } else {
            final boolean t8 = t.ergoFeed;
            desc.add("Per 60 s cycle (live galaxy):");
            if (!t8) {
                desc.add("Upkeep 7.5 L Magnetar Matter");
                for (Map.Entry<Fluid, Double> up : def.exactUpkeep.entrySet()) {
                    in.add(new FluidStack(up.getKey(), (int) Math.ceil(up.getValue())));
                }
                out.add(new FluidStack(def.product, (int) Math.round(GalaxyField.T7_ERGOMATTER)));
                desc.add("Out 600 L x Y x r x min(β̄,1.5)");
                desc.add("Born: the Magnetar Collapse");
            } else {
                final int perCycle = (int) Math.round(GalaxyField.F8_DEFAULT * StellarField.CYCLE_SECONDS);
                in.add(new FluidStack(def.feed, perCycle));
                out.add(new FluidStack(def.product, (int) Math.round(GalaxyField.T8_RATIO * perCycle)));
                desc.add("Feed f8 0-10 L/s (shown 5)");
                desc.add("Out 1.0 x fed x Y x r");
                desc.add("Satellite crossing caught:");
                desc.add(" +1 cycle (stay Stable)");
                desc.add("Born: the Galactic Merger");
            }
            desc.add(
                "Bursts: D " + String.format(Locale.ROOT, "%.1f", GalaxyField.demandT(t.n)) + " Bu/t x p (GRB ports)");
            desc.add(
                " (" + com.nateyburr.beyondthestars.machines.gui.BTSFormat.euOfBu(GalaxyField.demandT(t.n)) + " x p)");
            desc.add("Bring 1.5x D; surplus: crown");
            desc.add("Field: " + gaugeList(t) + ", minor mergers");
            desc.add("Space trims P-; aim the rail");
            desc.add(sci(def.euPerCycle) + " EU/cycle, wireless");
            desc.add("Companions: " + GalaxyField.maxCompanions(t.n) + " (slot bus)");
        }
        desc.add("x2 parallel per tier > T" + t.n);
        GTValues.RA.stdBuilder()
            .itemInputs(def.chart.copy())
            .fluidInputs(in.toArray(new FluidStack[0]))
            .fluidOutputs(out.toArray(new FluidStack[0]))
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
