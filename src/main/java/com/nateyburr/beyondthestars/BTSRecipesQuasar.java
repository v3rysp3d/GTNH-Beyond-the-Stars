package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLCondensates;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.kerr.KerrRecipeMaps;
import com.nateyburr.beyondthestars.machines.quasar.QuasarRecipeMaps;

import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import tectech.recipe.TecTechRecipeMaps;

/**
 * P4: THE QUASAR, CONDUIT AND EYE T7-T8 RECIPES (design repo {@code designs/docs/quasar-p4.md} §6.3, rev 7f91607;
 * D-50; every PROPOSED line accepted). <b>One owner: the P4 recipe unit.</b> Called from one line in
 * {@link BTSRecipes} ("quasar", after "reality", whose Entangled Ergomatter generator SL-C02 it uses) and, for the
 * Eternity-on-Ergomatter swap, from {@link CommonProxy#loadComplete}.
 *
 * <h2>What is here</h2>
 * <ul>
 * <li><b>SL-B16</b>: the BEC proposal's reserved Entangled Magnetar Matter generator (Magnetar Matter 144 L -> 144 L).
 * Its first consumers are P4's (the Intergalactic Upgrade Core, the Supermassive Seed Core, Star Chart: Triangulum).
 * </li>
 * <li><b>Blocks (slCasing2:3-11)</b>, all batch Condensate Assembler recipes because the BOMs are large (P4-05):
 * SL-Q02..Q05
 * Disc Field Coil T1-T4 (x32), SL-Q06 Eddington Wall Casing (x64; 27,815 per quasar), SL-Q07 Rim Gas Injector (x8),
 * SL-Q08 Jet Collimator Ring (x32), SL-Q17 Burst Thermaliser Casing (x64), SL-Q18 Radiator Petal (x64).</li>
 * <li><b>Controllers</b> (assembly lines, the design's machines): SL-Q01 Quasar Engine, SL-Q10 Wormhole Mouth, SL-Q11
 * Conduit Mirror, SL-Q12 Conduit Junction, SL-Q16 Burst Thermaliser Crown; <b>hatches</b> (Assembler): SL-Q19 GRB
 * Receptor Port, SL-Q20 Burst Control Hatch. Their outputs are the MTE units' stacks in {@link BTSMachines}; a recipe
 * whose machine is not registered yet waits (one log line) instead of counting as a skip.</li>
 * <li><b>Items</b>: SL-Q09 Quasar Seed and SL-Q23 Superdense Ergomatter (refined), both on the Kerr's
 * {@link KerrRecipeMaps#DENSITY} map; SL-Q13..Q15 Conduit Throat Mk I-III; SL-Q21 Intergalactic Upgrade Core; SL-Q22
 * Supermassive Seed Core; SL-Q24/Q25 Star Charts Triangulum / Milky Way.</li>
 * <li><b>Gate fixes</b> (behind {@code replaceTecTechEyeOfHarmony}, like SL-P39): SL-P40, the Resonance Chamber IV
 * with 2 SpaceTime foils + Entangled Ergomatter for GT's MHDCSM foil (the compression tier 8 gate); and SL-Q26, D-48's
 * Eternity-on-Ergomatter swap at load-complete ({@link #onLoadComplete}).</li>
 * <li><b>NEI</b>: SL-Q27 / SL-Q28, the display pages {@link QuasarRecipeMaps#QUASAR} and
 * {@link QuasarRecipeMaps#THERMALISER} (fake recipes).</li>
 * </ul>
 *
 * <h2>Rules (gate audit, quasar-p4.md §6.4-§6.5; logged as P4 questions)</h2>
 * <ul>
 * <li>Nothing needed before Eye T7 takes Ergomatter (T7's product): the Antipolarization Agent replaces it (ruling
 * 19b).
 * Superdense Ergomatter is used by nothing before T9.</li>
 * <li><b>UXV parts</b> (Field Generator / Sensor / Emitter, Machine Hull) need molten Universium and MHDCSM in RC-2, so
 * every part needed before T7 takes <b>twice as many UMV parts</b> instead, the B-72 stopgap I chose ("Ship UMV
 * stopgap"); UXV circuits stay (RC-2's own Eye T5/T6 compression generators take them).</li>
 * <li><b>MagMatter</b> (Q-22): the Godforge's magmatter mode needs the EE upgrade, which costs 2 Universium (T10) and
 * 16
 * Eternity (T7) frame boxes in RC-2, so MagMatter is not attainable before T7: the Intergalactic Upgrade Core takes the
 * SpaceTime Condensate fallback. Only the optional Disc Field Coil T4 keeps MagMatter.</li>
 * <li>Condensate Assembler: at most 16 item stacks and 4 condensates, one nanite tier per stack (never above Black
 * Dwarf, 7), condensate amounts on GT's half-unit rule, and unique item sets (it looks recipes up by items).</li>
 * </ul>
 * Every recipe runs under {@link BTSRecipeGuard#recipe}; every foreign component goes through {@code req}/{@code nn}.
 */
public final class BTSRecipesQuasar {

    // Nanite tiers (GT's NaniteTier.tier): 1 Carbon .. 7 Black Dwarf. Never above 7 (8+ are Universium-made).
    private static final NaniteTier N1 = NaniteTier.Carbon;
    private static final NaniteTier N2 = NaniteTier.Silver;
    private static final NaniteTier N3 = NaniteTier.Gold;
    private static final NaniteTier N4 = NaniteTier.Transcendent;
    private static final NaniteTier N5 = NaniteTier.SixPhasedCopper;
    private static final NaniteTier N6 = NaniteTier.WhiteDwarf;
    private static final NaniteTier N7 = NaniteTier.BlackDwarf;

    /** Quasar Seed / Superdense refinery: the linked KEE's minimum mass (quasar-p4.md §6.3). */
    public static final double DENSITY_P4_MIN_MASS = 256.0;
    /** Superdense refinery: the KEE's quasar link sigma it needs (R.3). */
    public static final double SUPERDENSE_MIN_SIGMA = 1.0;
    /** SL-Q26: Ergomatter in place of the Universium nanite in the Temporal Harmony Catalyst (L). */
    public static final int CATALYST_ERGOMATTER = 9_216;
    /** SL-Q26: Ergomatter in place of the Universium nanite in the Plasma Forge's Timepiece recipe (L). */
    public static final int TIMEPIECE_ERGOMATTER = 4_608;

    /** DENSITY recipes that need M >= 256 SM (SL-Q09, SL-Q23): the Hosts unit's validateRecipe reads these. */
    private static final Set<GTRecipe> DENSITY_256 = Collections.newSetFromMap(new IdentityHashMap<>());
    /** DENSITY recipes that also need the KEE's quasar link at sigma >= 1 (SL-Q23). */
    private static final Set<GTRecipe> DENSITY_LINK = Collections.newSetFromMap(new IdentityHashMap<>());

    /** The P4 recipes that wait for an MTE unit (their output machine is not registered yet). */
    private static final List<String> WAITING = new ArrayList<>();

    /** SL-Q26's outcome at load-complete: true once the Temporal Harmony Catalyst takes Ergomatter. */
    private static boolean eternitySwapped;

    private BTSRecipesQuasar() {}

    // =================================================================================== registration

    public static void register() {
        WAITING.clear();
        DENSITY_256.clear();
        DENSITY_LINK.clear();
        magnetarCondensate();
        blocks();
        density();
        throats();
        controllers();
        hatches();
        coresAndCharts();
        // SL-Q27 / SL-Q28: the NEI display pages (feed gas -> bursts; bursts -> EU), fake recipes.
        QuasarRecipeMaps.register();
        if (Config.replaceTecTechEyeOfHarmony) {
            resonanceChamberIV();
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] replaceTecTechEyeOfHarmony=false: SL-P40 (MHDCSM-free Resonance Chamber IV) is not registered "
                    + "and SL-Q26 (Eternity on Ergomatter) will not run; TecTech's Eye still makes Universium.");
        }
        if (WAITING.isEmpty()) {
            BeyondTheStars.LOG.info("[BTS] P4 recipes: every machine recipe is registered.");
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] P4 recipes: {} wait for their machine's MTE unit (not registered in this build): {}",
                WAITING.size(),
                String.join(", ", WAITING));
        }
    }

    // ------------------------------------------------------------------------- SL-B16 (reserved, P4 owns it)

    /**
     * SL-B16 Entangled Magnetar Matter: Magnetar Matter 144 L -> 144 L, 180 s at 2 A UXV (one step above Black Dwarf
     * Matter's SL-B14, the BEC table's pattern). Skipped if the Condensate Generator already condenses Magnetar
     * Matter; {@link #condensateOf} then finds that recipe's output.
     */
    private static void magnetarCondensate() {
        BTSRecipeGuard.recipe("SL-B16", () -> {
            final FluidStack source = BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 144);
            if (condensateOf(source.getFluid()) != null) {
                BeyondTheStars.LOG.info("[BTS] SL-B16: Magnetar Matter already has a Condensate Generator recipe.");
                return;
            }
            final Fluid ours = BTSRecipeGuard.nn(SLRealityFluids.entangledMagnetar, "Entangled Magnetar Matter");
            added(
                GTValues.RA.stdBuilder()
                    .fluidInputs(source)
                    .fluidOutputs(new FluidStack(ours, 144))
                    .duration(180 * SECONDS)
                    .eut(TierEU.RECIPE_UXV * 2),
                TecTechRecipeMaps.condensateGeneratorRecipes);
        });
    }

    // ===================================================================== blocks (slCasing2:3-11, BEC batches)

    private static void blocks() {
        // SL-Q02..Q05 Disc Field Coil T1-T4 x32. The quasar takes 3,354, all one tier (channel 'coil'). Per 32: 16
        // frames of the tier metal, 8 x16 UMV superconductor wire (the card's 32 wire per coil, batched), 4 Ergosphere
        // Field Coils, 4 circuits; the tier's condensate and SpaceTime. T4 (MagMatter, optional: it only lifts the cap
        // to 4,096 SM) waits for MagMatter (Q-22: after Eye T10 in RC-2) and takes UMV wire x16 in place of the
        // card's UXV Superconductor Wire, which no recipe makes.
        coil(1, Materials.Infinity, CondensateType.Infinity, 64, 16, N3, N2, Materials.UMV, 2_400, TierEU.RECIPE_UMV);
        coil(
            2,
            Materials.TranscendentMetal,
            CondensateType.TranscendentMetal,
            96,
            24,
            N4,
            N3,
            Materials.UMV,
            3_000,
            TierEU.RECIPE_UMV);
        coil(
            3,
            Materials.SpaceTime,
            CondensateType.SpaceTime,
            128,
            32,
            N4,
            N3,
            Materials.UMV,
            3_600,
            TierEU.RECIPE_UMV * 4);
        coil(
            4,
            Materials.MagMatter,
            CondensateType.MagMatter,
            160,
            64,
            N6,
            N5,
            Materials.UXV,
            4_200,
            TierEU.RECIPE_UXV);

        // SL-Q06 Eddington Wall Casing x64 (27,815 per quasar: 435 crafts). The card's 2 per 4 dense BDM plates would
        // be 55,000 dense plates; the batch keeps the neutronium body and a Black Dwarf identity at 72 L per 64.
        BTSRecipeGuard.recipe(
            "SL-Q06",
            () -> assemble(
                c2(BlockSLCasing2.EDDINGTON_WALL, 64),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.Infinity, 64 * INGOTS), ours(SLCondensates.entangledBlackDwarf, 72)),
                s(BTSRecipesSingularity.dense(Materials.Neutronium, 16), N1),
                s(req(OrePrefixes.plate, Materials.CosmicNeutronium, 32), N1),
                s(req(OrePrefixes.frameGt, Materials.Neutronium, 8), N2),
                s(item(ItemList.MetaMaterial_Shielding1, 2), N3)));

        // SL-Q07 Rim Gas Injector x8 (112 per quasar, 32 per Wormhole Mouth). The card was 1 per assembly line with
        // 16 UMV pumps each.
        BTSRecipeGuard.recipe(
            "SL-Q07",
            () -> assemble(
                c2(BlockSLCasing2.RIM_GAS_INJECTOR, 8),
                3_600 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.SpaceTime, 32 * INGOTS), entangled(CondensateType.Infinity, 16 * INGOTS)),
                s(item(ItemList.Electric_Pump_UMV, 8), N3),
                s(item(ItemList.FluidRegulator_UMV, 4), N3),
                s(item(ItemList.Hatch_Input_UMV, 1), N2),
                s(neutroniumPipes(8), N1),
                s(circuit(Materials.UMV, 4), N2)));

        // SL-Q08 Jet Collimator Ring x32 (596 per quasar, 297-452 per crown, 392 per Mouth, 648 per Junction).
        BTSRecipeGuard.recipe(
            "SL-Q08",
            () -> assemble(
                c2(BlockSLCasing2.JET_COLLIMATOR, 32),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(
                    entangled(CondensateType.SpaceTime, 16 * INGOTS),
                    ours(SLCondensates.entangledWhiteDwarf, 8 * INGOTS)),
                s(item(ItemList.Field_Generator_UMV, 2), N3),
                s(c2(BlockSLCasing2.DISC_COIL_T3, 8), N3),
                s(BTSRecipesSingularity.dense(Materials.WhiteDwarfMatter, 8), N3),
                s(circuit(Materials.UMV, 4), N2)));

        // SL-Q17 Burst Thermaliser Casing x64 (404-692 per crown core, plus the petals' share).
        BTSRecipeGuard.recipe(
            "SL-Q17",
            () -> assemble(
                c2(BlockSLCasing2.BURST_THERMALISER, 64),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.Hypogen, 32 * INGOTS), entangled(CondensateType.Infinity, 32 * INGOTS)),
                s(BTSRecipesSingularity.dense(Materials.Neutronium, 16), N1),
                s(nn(MaterialsElements.STANDALONE.HYPOGEN.getPlate(32), "Hypogen plate"), N3),
                s(req(OrePrefixes.frameGt, Materials.Infinity, 8), N2),
                s(item(ItemList.MetaMaterial_Shielding1, 1), N3)));

        // SL-Q18 Radiator Petal x64 (643 / 1,428 / 2,803 per crown Mk I / II / III).
        BTSRecipeGuard.recipe(
            "SL-Q18",
            () -> assemble(
                c2(BlockSLCasing2.RADIATOR_PETAL, 64),
                1_800 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(
                    entangled(CondensateType.Infinity, 32 * INGOTS),
                    entangled(CondensateType.TranscendentMetal, 16 * INGOTS)),
                s(c2(BlockSLCasing2.BURST_THERMALISER, 16), N2),
                s(req(OrePrefixes.plate, Materials.Infinity, 64), N1),
                s(req(OrePrefixes.wireGt04, Materials.SuperconductorUMV, 8), N2)));
    }

    /** One Disc Field Coil tier, x32 (SL-Q02 + tier - 1). */
    private static void coil(int tier, Materials metal, CondensateType tierCondensate, int tierIngots,
        int spaceTimeIngots, NaniteTier frameNanite, NaniteTier partNanite, Materials circuitTier, int seconds,
        long eut) {
        final String id = "SL-Q0" + (tier + 1);
        BTSRecipeGuard.recipe(
            id,
            () -> assemble(
                c2(BlockSLCasing2.discCoil(tier), 32),
                seconds * SECONDS,
                eut,
                tier == 3 ? cond(entangled(CondensateType.SpaceTime, (tierIngots + spaceTimeIngots) * INGOTS))
                    : cond(
                        entangled(tierCondensate, tierIngots * INGOTS),
                        entangled(CondensateType.SpaceTime, spaceTimeIngots * INGOTS)),
                s(req(OrePrefixes.frameGt, metal, 16), frameNanite),
                s(req(OrePrefixes.wireGt16, Materials.SuperconductorUMV, tier == 4 ? 16 : 8), partNanite),
                s(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 4), partNanite),
                s(circuit(circuitTier, 4), partNanite)));
    }

    // ================================================================== the Kerr's DENSITY map (Kerr T2 module)

    private static void density() {
        // SL-Q09 Quasar Seed: KEE T2 at M >= 256 SM (Hosts unit, validateRecipe); costs the KEE's hole nothing (Q-5).
        BTSRecipeGuard.recipe("SL-Q09", () -> {
            final Collection<GTRecipe> made = added(
                GTValues.RA.stdBuilder()
                    .itemInputs(item(ItemList.Black_Hole_Opener, 4))
                    .fluidInputs(
                        BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 64_000),
                        BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 1_440))
                    .itemOutputs(BTSRecipeGuard.stack(SLQuasarItems.quasarSeed, 1))
                    .duration(1_200 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                KerrRecipeMaps.DENSITY);
            DENSITY_256.addAll(made);
        });

        // SL-Q23 Superdense Ergomatter, refined 8 : 1 ("compacted and refined", R.3, Q-14): KEE T2, M >= 256 SM and
        // the KEE's quasar link at sigma >= 1. No item in the card; the DENSITY map needs one item input, so a
        // programmed circuit 8 (not consumed) carries it.
        BTSRecipeGuard.recipe("SL-Q23", () -> {
            final Collection<GTRecipe> made = added(
                GTValues.RA.stdBuilder()
                    .circuit(8)
                    .fluidInputs(
                        BTSRecipeGuard.fluid(SLQuasarFluids.ergomatter, 4_000),
                        molten(Materials.SpaceTime, 1_440))
                    .fluidOutputs(BTSRecipeGuard.fluid(SLQuasarFluids.superdenseErgomatter, 500))
                    .duration(600 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                KerrRecipeMaps.DENSITY);
            DENSITY_256.addAll(made);
            DENSITY_LINK.addAll(made);
        });
    }

    /**
     * The linked KEE's minimum mass for a DENSITY recipe (256 SM for SL-Q09 / SL-Q23), or 0 for one with no P4 gate
     * (SL-K05 keeps the module's own 128 SM rule). For the Hosts unit's {@code validateRecipe}.
     */
    public static double densityMinMass(GTRecipe recipe) {
        return recipe != null && DENSITY_256.contains(recipe) ? DENSITY_P4_MIN_MASS : 0.0;
    }

    /** True if a DENSITY recipe also needs the KEE's quasar link at sigma >= {@link #SUPERDENSE_MIN_SIGMA} (SL-Q23). */
    public static boolean densityNeedsLink(GTRecipe recipe) {
        return recipe != null && DENSITY_LINK.contains(recipe);
    }

    // ============================================================================== conduit throats

    private static void throats() {
        // SL-Q13 Conduit Throat Mk I (24 Bu/t). The card's Field Generator UXV x2 -> UMV x4 (B-72 stopgap).
        BTSRecipeGuard.recipe(
            "SL-Q13",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk1, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(entangled(CondensateType.SpaceTime, 28 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 16), N4),
                s(item(ItemList.Field_Generator_UMV, 4), N5),
                s(c2(BlockSLCasing2.EDDINGTON_WALL, 4), N3)));

        // SL-Q14 Conduit Throat Mk II (384 Bu/t). Built before T7: the Agent, not Ergomatter.
        BTSRecipeGuard.recipe(
            "SL-Q14",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 1),
                900 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(ours(SLCondensates.entangledAgent, 32_000)),
                s(BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk1, 4), N4),
                s(item(ItemList.Field_Generator_UMV, 16), N5),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 32), N4)));

        // SL-Q15 Conduit Throat Mk III (6,144 Bu/t): a post-T7 part, so it keeps its Ergomatter. Research: Mk II.
        BTSRecipeGuard.recipe(
            "SL-Q15",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UXV))
                .itemInputs(
                    items(
                        BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 4),
                        item(ItemList.Field_Generator_UMV, 16),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 64),
                        circuit(Materials.UMV, 16)))
                .fluidInputs(BTSRecipeGuard.fluid(SLQuasarFluids.ergomatter, 256_000))
                .itemOutputs(BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk3, 1))
                .duration(900 * SECONDS)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ================================================================================ controllers (assembly lines)

    private static void controllers() {
        // SL-Q01 Quasar Engine (MTE 28120). Research: the Quasar Seed. Ergomatter 256,000 L -> Agent 256,000 L; the
        // Magnetar Matter re-costed to about 20,000 L: the card's 9,216 L plus its 8 dense plates and 8 frame boxes
        // (no such items exist: Magnetar Matter is our fluid) folded in as fluid, 21,888 L in all. UXV FG / Emitter /
        // Sensor x32 -> UMV x64 (B-72 stopgap); the Black Dwarf Matter condensate as molten Black Dwarf Matter (an
        // assembly line cannot draw from the BEC network), 448 ingots.
        machine("SL-Q01", "Quasar Engine (MTE 28120)", () -> BTSMachines.quasarEngine, out -> {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLQuasarItems.quasarSeed, 1))
                .metadata(SCANNING, new Scanning(10 * MINUTES, TierEU.RECIPE_UXV))
                .itemInputs(
                    items(
                        BTSRecipesSingularity.dense(Materials.WhiteDwarfMatter, 64),
                        BTSRecipesSingularity.dense(Materials.BlackDwarfMatter, 64),
                        item(ItemList.Field_Generator_UMV, 64),
                        item(ItemList.Emitter_UMV, 64),
                        item(ItemList.Sensor_UMV, 64),
                        circuit(Materials.UXV, 64),
                        c2(BlockSLCasing2.EDDINGTON_WALL, 64),
                        c2(BlockSLCasing2.DISC_COIL_T3, 16),
                        c2(BlockSLCasing2.RIM_GAS_INJECTOR, 4),
                        c2(BlockSLCasing2.JET_COLLIMATOR, 8)))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 21_888),
                    BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 2_000_000),
                    BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 256_000),
                    molten(Materials.BlackDwarfMatter, 448 * INGOTS))
                .itemOutputs(out)
                .duration(2_400 * SECONDS)
                .eut(TierEU.RECIPE_UXV)
                .addTo(AssemblyLine);
        });

        // SL-Q10 Wormhole Mouth (28121). Research: the Rim Gas Injector. Ergomatter -> Agent, same litres.
        machine("SL-Q10", "Wormhole Mouth (MTE 28121)", () -> BTSMachines.wormholeMouth, out -> {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, c2(BlockSLCasing2.RIM_GAS_INJECTOR, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 64),
                        item(ItemList.Field_Generator_UMV, 32),
                        item(ItemList.Sensor_UMV, 16),
                        circuit(Materials.UMV, 32),
                        c2(BlockSLCasing2.EDDINGTON_WALL, 16),
                        BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 1),
                        BTSRecipesEye.denseSpaceTime(32)))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 128_000),
                    molten(Materials.SpaceTime, 589_824),
                    nn(Materials.ExcitedDTEC.getFluid(128_000), "Excited DTEC"))
                .itemOutputs(out)
                .duration(1_200 * SECONDS)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine);
        });

        // SL-Q11 Conduit Mirror (28122). Research: the Superradiant Mirror Segment. The card's "BEC Metamaterial
        // Film" was never built: GT's MetaMaterial Wave Focus II (a focusing metamaterial) takes its place.
        machine("SL-Q11", "Conduit Mirror (MTE 28122)", () -> BTSMachines.conduitMirror, out -> {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 4),
                        item(ItemList.MetaMaterial_WaveFocus2, 4),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                        c2(BlockSLCasing2.EDDINGTON_WALL, 32),
                        item(ItemList.Field_Generator_UMV, 8),
                        BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 1),
                        circuit(Materials.UMV, 8)))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 48_000),
                    molten(Materials.SpaceTime, 147_456))
                .itemOutputs(out)
                .duration(900 * SECONDS)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine);
        });

        // SL-Q12 Conduit Junction (28123). Research: the Jet Collimator Ring. It consumes a Wormhole Mouth, so it
        // also waits for the Mouth's MTE.
        machine("SL-Q12", "Conduit Junction (MTE 28123)", () -> BTSMachines.conduitJunction, out -> {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, c2(BlockSLCasing2.JET_COLLIMATOR, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        nn(BTSMachines.wormholeMouth, "BTSMachines.wormholeMouth (MTE 28121)").copy(),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 64),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 64),
                        item(ItemList.Field_Generator_UMV, 32),
                        item(ItemList.Sensor_UMV, 16),
                        item(ItemList.Emitter_UMV, 16),
                        circuit(Materials.UMV, 32),
                        BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk2, 3)))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 192_000),
                    molten(Materials.SpaceTime, 589_824),
                    nn(Materials.ExcitedDTEC.getFluid(64_000), "Excited DTEC"))
                .itemOutputs(out)
                .duration(1_500 * SECONDS)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine);
        });

        // SL-Q16 Burst Thermaliser Crown (28124): ONE controller card, Mk by structure (Q-7). Research: the Burst
        // Thermaliser Casing.
        machine("SL-Q16", "Burst Thermaliser Crown (MTE 28124)", () -> BTSMachines.burstThermaliserCrown, out -> {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, c2(BlockSLCasing2.BURST_THERMALISER, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        item(ItemList.Wireless_Dynamo_Energy_UMV, 2),
                        c2(BlockSLCasing2.BURST_THERMALISER, 32),
                        c2(BlockSLCasing2.RADIATOR_PETAL, 12),
                        item(ItemList.Field_Generator_UMV, 8),
                        circuit(Materials.UMV, 16),
                        BTSRecipesSingularity.dense(Materials.BlackDwarfMatter, 16)))
                .fluidInputs(superCoolant(4_000_000), molten(Materials.SpaceTime, 147_456))
                .itemOutputs(out)
                .duration(1_200 * SECONDS)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine);
        });
    }

    // ============================================================================================ hatches

    private static void hatches() {
        // SL-Q19 GRB Receptor Port (28125). Card: Hull UXV + Sensor UXV x2 (UXV hulls need MHDCSM and MagMatter
        // plates in RC-2): Hull UMV + Sensor UMV x4, at UMV.
        machine("SL-Q19", "GRB Receptor Port (MTE 28125)", () -> BTSMachines.grbReceptorPort, out -> {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    item(ItemList.Hull_UMV, 1),
                    item(ItemList.Sensor_UMV, 4),
                    SLBlocks.casing(BlockSLCasing.CASIMIR, 8),
                    c2(BlockSLCasing2.EDDINGTON_WALL, 2))
                .itemOutputs(out)
                .duration(600 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes);
        });

        // SL-Q20 Burst Control Hatch (28126): Hull UMV, Sensor UMV x2, Emitter UMV x2, UXV circuit x2.
        machine("SL-Q20", "Burst Control Hatch (MTE 28126)", () -> BTSMachines.burstControlHatch, out -> {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    item(ItemList.Hull_UMV, 1),
                    item(ItemList.Sensor_UMV, 2),
                    item(ItemList.Emitter_UMV, 2),
                    circuit(Materials.UXV, 2))
                .itemOutputs(out)
                .duration(600 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes);
        });
    }

    // ============================================================================== cores and star charts

    private static void coresAndCharts() {
        // SL-Q21 Intergalactic Upgrade Core (T6 -> T7, the Magnetar Collapse). As revised: no Galactic core (D-49
        // consumed it), Throat Mk I x2 for Mk III, Magnetar Matter 4,608 L + Agent 32,000 L (halved, the Stability
        // Link carries the milestone). Q-22 FAILED (MagMatter needs Universium): SpaceTime Condensate +32,000 L
        // instead of MagMatter Condensate. Four condensates at most: the card's 64,000 L of White Dwarf Matter
        // condensate becomes 48 dense White Dwarf Matter plates (62,208 L). FG UXV x32 -> UMV x64 (B-72).
        BTSRecipeGuard.recipe(
            "SL-Q21",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.intergalacticUpgradeCore, 1),
                3_000 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLRealityFluids.entangledMagnetar, 4_608),
                    ours(SLCondensates.entangledAgent, 32_000),
                    entangled(CondensateType.SpaceTime, 1_120 * INGOTS),
                    ours(SLCondensates.entangledBlackDwarf, 448 * INGOTS)),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 16), N6),
                s(item(ItemList.MetaMaterial_Shielding2, 16), N6),
                s(BTSRecipeGuard.stack(SLQuasarItems.conduitThroatMk1, 2), N4),
                s(item(ItemList.Field_Generator_UMV, 64), N5),
                s(circuit(Materials.UXV, 32), N5),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 64), N3),
                s(BTSRecipesSingularity.dense(Materials.WhiteDwarfMatter, 48), N6)));

        // SL-Q22 Supermassive Seed Core (T7 -> T8, the Galactic Merger): T7's product. Ergomatter 128,000 L (halved)
        // + Magnetar Matter 2,304 L, Bastion 64, metamaterials 64.
        BTSRecipeGuard.recipe(
            "SL-Q22",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.supermassiveSeedCore, 1),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLRealityFluids.entangledErgomatter, 128_000),
                    ours(SLRealityFluids.entangledMagnetar, 2_304)),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 64), N3),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 32), N7),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 32), N6)));

        // SL-Q24 Star Chart: Triangulum (Eye T7), gated on Magnetar Matter (T6). Sensor UXV x2 -> UMV x4 (B-72).
        BTSRecipeGuard.recipe(
            "SL-Q24",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.starChartTriangulum, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(ours(SLRealityFluids.entangledMagnetar, 2_304), entangled(CondensateType.SpaceTime, 28 * INGOTS)),
                s(item(ItemList.Tool_DataOrb, 1), N4),
                s(item(ItemList.Sensor_UMV, 4), N5),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 1), N3)));

        // SL-Q25 Star Chart: Milky Way (Eye T8), gated on Ergomatter (T7). A Bastion Plate where Triangulum has a
        // Casimir Plate keeps the two item sets apart.
        BTSRecipeGuard.recipe(
            "SL-Q25",
            () -> assemble(
                BTSRecipeGuard.stack(SLQuasarItems.starChartMilkyWay, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLRealityFluids.entangledErgomatter, 4_000),
                    entangled(CondensateType.SpaceTime, 28 * INGOTS)),
                s(item(ItemList.Tool_DataOrb, 1), N4),
                s(item(ItemList.Sensor_UMV, 4), N6),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 1), N3)));
    }

    // =============================================== the compression tier 8 gate (with TecTech's Eye replaced)

    /**
     * SL-P40: MetaMaterial Resonance Chamber IV with 2 SpaceTime foils + Entangled Ergomatter in place of GT's MHDCSM
     * foil (MHDCSM is Endgame, D-48; Universium-made). Everything else is RC-2's own Condensate Assembler recipe
     * ({@code BECMetaMaterialRecipes.registerResonanceChamber}, t = 3): 2 Emitter UMV, 2 SpaceTime rods, 1 Eternity
     * plate; nanites Black Dwarf / Transcendent / Six-Phased Copper / Transcendent; Chromatic Glass 24, Infinity 12,
     * Boundless Cosmic Solder 4; 300 s at UXV. The SL-P39 pattern. Its Eternity plate is reachable before T8 only with
     * SL-Q26 (Eternity on Ergomatter). The card's Ergomatter 1,440 L is 1,500 L of Entangled Ergomatter (GT's
     * half-unit rule for a 1,000 L condensate).
     */
    private static void resonanceChamberIV() {
        BTSRecipeGuard.recipe(
            "SL-P40",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        item(ItemList.Emitter_UMV, 2),
                        req(OrePrefixes.stick, Materials.SpaceTime, 2),
                        req(OrePrefixes.foil, Materials.SpaceTime, 2),
                        req(OrePrefixes.plate, Materials.Eternity, 1))
                    .fluidInputs(
                        entangled(CondensateType.ChromaticGlass, 24 * CondensateType.ChromaticGlass.getUnit()),
                        entangled(CondensateType.Infinity, 12 * CondensateType.Infinity.getUnit()),
                        entangled(
                            CondensateType.BoundlessCosmicSolder,
                            4 * CondensateType.BoundlessCosmicSolder.getUnit()),
                        ours(SLRealityFluids.entangledErgomatter, 1_500))
                    .itemOutputs(item(ItemList.MetaMaterial_ResonanceChamber4, 1))
                    .metadata(NANITE_TIERS, new NaniteTier[] { N7, N4, N5, N4 })
                    .duration(300 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                TecTechRecipeMaps.condensateAssemblingRecipes));
    }

    // =================================================================== SL-Q26: Eternity on Ergomatter (D-48)

    /**
     * D-48 proposal 1 (accepted 2026-10-12; Q-10 settled: ships in P4): the pack's Eternity QFT recipe
     * (coremod {@code ScriptGregtechPlusPlus}: Primordial Matter -> molten Eternity + molten Time) needs the Temporal
     * Harmony Catalyst, and GT++'s only catalyst recipe ({@code RecipeLoaderChemicalSkips}, Assembler, UXV) takes a
     * Universium Nanite: Universium is Eye T10's, so Eternity was a loop on the way to T10. Rewritten in place (one
     * recipe in NEI, my B-29 style): the Universium Nanite goes, Ergomatter (T7) takes the Assembler's one
     * fluid slot ({@value #CATALYST_ERGOMATTER} L), and the Dark Iron that held it moves into the items as 71 blocks
     * (the same 92,016 of its 92,160 L). Then the map's lookup is rebuilt.
     * <p>
     * Behind {@code replaceTecTechEyeOfHarmony} (with it off TecTech's Eye still makes Universium). Runs at
     * load-complete from {@link CommonProxy#loadComplete}, inside {@link BTSRecipeRemovals#guard}, because GT++ loads
     * its chemical skips late. Idempotent: a recipe without the nanite is left alone.
     */
    public static void onLoadComplete() {
        if (!Config.replaceTecTechEyeOfHarmony) return;
        final ItemStack catalyst = GregtechItemList.TemporalHarmonyCatalyst.get(1);
        final ItemStack nanite = GTOreDictUnificator.get(OrePrefixes.nanite, Materials.Universium, 1);
        final Fluid ergo = SLQuasarFluids.ergomatter;
        if (catalyst == null || catalyst.getItem() == null || nanite == null || ergo == null) {
            BeyondTheStars.LOG.error(
                "[BTS] SL-Q26: the Temporal Harmony Catalyst, the Universium Nanite or Ergomatter is missing; Eternity's "
                    + "pack recipe is unchanged and still needs Universium (Eye T10).");
            return;
        }
        final ItemStack darkIron = GTOreDictUnificator.get(OrePrefixes.block, Materials.DarkIron, 64);
        int rewritten = 0;
        for (GTRecipe recipe : assemblerRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null || recipe.mInputs == null) continue;
            if (!outputs(recipe, catalyst)) continue;
            final int at = indexOf(recipe.mInputs, nanite);
            if (at < 0) continue;
            final List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < recipe.mInputs.length; i++) {
                if (i != at) inputs.add(recipe.mInputs[i]);
            }
            if (darkIron != null && recipe.mFluidInputs != null
                && recipe.mFluidInputs.length > 0
                && inputs.size() + 2 <= 9) {
                inputs.add(GTUtility.copyAmount(64, darkIron));
                inputs.add(GTUtility.copyAmount(7, darkIron));
            }
            recipe.mInputs = inputs.toArray(new ItemStack[0]);
            recipe.mFluidInputs = new FluidStack[] { new FluidStack(ergo, CATALYST_ERGOMATTER) };
            rewritten++;
        }
        if (rewritten > 0) {
            assemblerRecipes.getBackend()
                .reInit();
            eternitySwapped = true;
            BeyondTheStars.LOG.info(
                "[BTS] SL-Q26: the Temporal Harmony Catalyst ({} recipe{}) now takes {} L of Ergomatter instead of a "
                    + "Universium Nanite: Eternity is made before Eye T10 (D-48).",
                rewritten,
                rewritten == 1 ? "" : "s",
                CATALYST_ERGOMATTER);
        } else {
            BeyondTheStars.LOG.error(
                "[BTS] SL-Q26: no Assembler recipe makes the Temporal Harmony Catalyst from a Universium Nanite in this "
                    + "pack (RC-2 has one in GT++'s RecipeLoaderChemicalSkips); nothing was rewritten. If Eternity "
                    + "still needs Universium, the Homogeny Tier III Core, Reality Refab and the Region IV anchor are "
                    + "unreachable before Eye T10.");
        }
        timepiece(nanite, ergo);
    }

    /**
     * SL-Q26, second half: the catalyst consumes a Timepiece, and the only other Timepiece recipe (coremod
     * {@code DTPFRecipes}: Dimensionally Transcendent Plasma Forge, 3 Timepieces) also eats a Universium Nanite, so
     * the first catalyst still needed Eye T10 (the gate dump found it). The nanite becomes
     * {@value #TIMEPIECE_ERGOMATTER}
     * L of Ergomatter, one more fluid input (the Plasma Forge takes 9). The Eternity QFT recipe hands a Timepiece back,
     * so after the first catalyst the loop feeds itself as the pack intended.
     */
    private static void timepiece(ItemStack nanite, Fluid ergo) {
        final ItemStack timepiece = ItemList.Timepiece.get(1);
        if (timepiece == null || timepiece.getItem() == null) return;
        int rewritten = 0;
        for (GTRecipe recipe : RecipeMaps.plasmaForgeRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null || recipe.mInputs == null) continue;
            if (!outputs(recipe, timepiece)) continue;
            final int at = indexOf(recipe.mInputs, nanite);
            if (at < 0) continue;
            final List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < recipe.mInputs.length; i++) {
                if (i != at) inputs.add(recipe.mInputs[i]);
            }
            final List<FluidStack> fluids = new ArrayList<>();
            if (recipe.mFluidInputs != null) Collections.addAll(fluids, recipe.mFluidInputs);
            fluids.add(new FluidStack(ergo, TIMEPIECE_ERGOMATTER));
            recipe.mInputs = inputs.toArray(new ItemStack[0]);
            recipe.mFluidInputs = fluids.toArray(new FluidStack[0]);
            rewritten++;
        }
        if (rewritten > 0) {
            RecipeMaps.plasmaForgeRecipes.getBackend()
                .reInit();
            BeyondTheStars.LOG.info(
                "[BTS] SL-Q26: the Timepiece ({} Plasma Forge recipe{}) takes {} L of Ergomatter instead of a Universium "
                    + "Nanite.",
                rewritten,
                rewritten == 1 ? "" : "s",
                TIMEPIECE_ERGOMATTER);
        } else {
            BeyondTheStars.LOG.warn(
                "[BTS] SL-Q26: no Plasma Forge recipe makes a Timepiece from a Universium Nanite in this pack; the first "
                    + "Temporal Harmony Catalyst needs whatever Timepiece source the pack has.");
        }
    }

    /**
     * True when Eternity is made before Eye T10: with {@code replaceTecTechEyeOfHarmony} the Temporal Harmony
     * Catalyst takes Ergomatter (SL-Q26, applied at load-complete; the boot log says if it found nothing to
     * rewrite), and without it TecTech's Eye still makes Universium. {@code BTSRecipesReality}'s gate log follows this
     * (it runs at postInit, before SL-Q26, so it reads the decision, not the outcome).
     */
    public static boolean eternityBeforeT10() {
        return true;
    }

    /** SL-Q26's outcome (false until load-complete, and if it found nothing to rewrite). */
    public static boolean eternitySwapped() {
        return eternitySwapped;
    }

    // =========================================================================================== helpers

    /** A P4 machine recipe: registered if its MTE unit has registered the machine, else it waits (logged once). */
    private interface MachineRecipe {

        void register(ItemStack output);
    }

    private static void machine(String id, String what, Supplier<ItemStack> output, MachineRecipe body) {
        final ItemStack out = output.get();
        if (out == null) {
            WAITING.add(id + " " + what);
            return;
        }
        BTSRecipeGuard.recipe(id, () -> body.register(out.copy()));
    }

    /** One group of item inputs that all take the same nanite tier (stacks above 64 are split). */
    private static final class Slot {

        final List<ItemStack> stacks = new ArrayList<>();
        final NaniteTier tier;

        Slot(NaniteTier tier) {
            this.tier = tier;
        }
    }

    /** {@code stacks} (an ItemStack or an ItemStack[]) at nanite {@code tier}. */
    private static Slot s(Object stacks, NaniteTier tier) {
        final Slot slot = new Slot(tier);
        final ItemStack[] parts = stacks instanceof ItemStack[] ? (ItemStack[]) stacks
            : new ItemStack[] { (ItemStack) stacks };
        for (ItemStack part : parts) {
            nn(part, "an item input");
            int left = part.stackSize;
            while (left > 0) {
                final int n = Math.min(64, left);
                slot.stacks.add(GTUtility.copyAmount(n, part));
                left -= n;
            }
        }
        return slot;
    }

    private static FluidStack[] cond(FluidStack... condensates) {
        return condensates;
    }

    /**
     * One Condensate Assembler recipe: one nanite tier per item stack, at most 16 item stacks and 4 condensates; GT
     * must keep it (an empty addTo throws, so the guard counts a skip).
     */
    private static void assemble(ItemStack output, int ticks, long eut, FluidStack[] condensates, Slot... slots) {
        final List<ItemStack> items = new ArrayList<>();
        final List<NaniteTier> tiers = new ArrayList<>();
        for (Slot slot : slots) {
            for (ItemStack stack : slot.stacks) {
                items.add(stack);
                tiers.add(slot.tier);
            }
        }
        if (items.size() > 16) {
            throw new IllegalStateException(
                "the Condensate Assembler takes at most 16 item stacks, got " + items.size());
        }
        if (condensates.length > 4) {
            throw new IllegalStateException("the Condensate Assembler takes at most 4 condensates");
        }
        added(
            GTValues.RA.stdBuilder()
                .itemInputs(items.toArray(new ItemStack[0]))
                .fluidInputs(condensates)
                .itemOutputs(nn(output, "the output"))
                .metadata(NANITE_TIERS, tiers.toArray(new NaniteTier[0]))
                .duration(ticks)
                .eut(eut),
            TecTechRecipeMaps.condensateAssemblingRecipes);
    }

    /** {@code builder.addTo(map)}, throwing if GT kept nothing (so the guard counts a skip). */
    private static Collection<GTRecipe> added(GTRecipeBuilder builder, RecipeMap<?> map) {
        final Collection<GTRecipe> recipes = builder.addTo(map);
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalStateException("GT rejected the recipe for " + map.unlocalizedName);
        }
        return recipes;
    }

    /** {@code litres} of GT's entangled {@code type}, checked against GT's half-unit rule. */
    private static FluidStack entangled(CondensateType type, int litres) {
        return nn(type.getEntangled(litres), "entangled " + type.name());
    }

    /**
     * {@code litres} of one of the addon's condensates: whatever fluid the Condensate Generator makes from the same
     * source fluid (ours, or GT's if GT condenses that source too), so the recipe asks for what a network can hold.
     * Throws (a logged skip) if no generator recipe makes it. Amounts must be multiples of half the unit (72 L for the
     * 144 L molten matter, 500 L for the 1,000 L fluids).
     */
    private static FluidStack ours(Fluid condensate, int litres) {
        final Fluid fluid = nn(condensate, "an addon condensate");
        final Fluid made = madeCondensate(fluid);
        if (made == null) {
            throw new IllegalStateException("no Condensate Generator recipe makes " + fluid.getName());
        }
        final int unit = unitOf(fluid);
        if (litres % (unit / 2) != 0) {
            throw new IllegalArgumentException(
                litres + " L of " + fluid.getName() + " is not a multiple of half its " + unit + " L unit");
        }
        return new FluidStack(made, litres);
    }

    /** 144 for the molten-matter condensates (White / Black Dwarf, Magnetar), 1,000 for the rest. */
    private static int unitOf(Fluid condensate) {
        if (condensate == SLCondensates.entangledWhiteDwarf || condensate == SLCondensates.entangledBlackDwarf
            || condensate == SLRealityFluids.entangledMagnetar) {
            return 144;
        }
        return 1_000;
    }

    /**
     * The condensate a generator makes in place of {@code ours}: {@code ours} itself if a recipe outputs it, else
     * the output of a recipe on the same source fluid (BEC's resolution), else null.
     */
    private static Fluid madeCondensate(Fluid ours) {
        for (GTRecipe recipe : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mFluidOutputs == null) continue;
            for (FluidStack out : recipe.mFluidOutputs) {
                if (out != null && out.getFluid() == ours) return ours;
            }
        }
        final Fluid source = sourceOf(ours);
        return source == null ? null : condensateOf(source);
    }

    /** The source fluid of one of our condensates, for the resolution above. */
    private static Fluid sourceOf(Fluid condensate) {
        final FluidStack stack;
        if (condensate == SLCondensates.entangledWhiteDwarf) stack = Materials.WhiteDwarfMatter.getMolten(1L);
        else if (condensate == SLCondensates.entangledBlackDwarf) stack = Materials.BlackDwarfMatter.getMolten(1L);
        else if (condensate == SLCondensates.entangledAgent) return SLFluids.antipolarizationAgent;
        else if (condensate == SLRealityFluids.entangledMagnetar) return SLFluids.magnetarMatter;
        else if (condensate == SLRealityFluids.entangledErgomatter) return SLQuasarFluids.ergomatter;
        else return null;
        return stack == null ? null : stack.getFluid();
    }

    /** The output of the Condensate Generator recipe whose input is {@code source}, or null. */
    private static Fluid condensateOf(Fluid source) {
        if (source == null) return null;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mFluidInputs == null
                || recipe.mFluidInputs.length == 0
                || recipe.mFluidOutputs == null
                || recipe.mFluidOutputs.length == 0
                || recipe.mFluidInputs[0] == null
                || recipe.mFluidOutputs[0] == null) continue;
            if (recipe.mFluidInputs[0].getFluid() == source) return recipe.mFluidOutputs[0].getFluid();
        }
        return null;
    }

    /** True if any output of {@code recipe} is {@code stack}'s item and meta. */
    private static boolean outputs(GTRecipe recipe, ItemStack stack) {
        for (ItemStack out : recipe.mOutputs) {
            if (out != null && out.getItem() == stack.getItem() && out.getItemDamage() == stack.getItemDamage()) {
                return true;
            }
        }
        return false;
    }

    private static int indexOf(ItemStack[] stacks, ItemStack want) {
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null && GTUtility.areStacksEqual(stacks[i], want, true)) return i;
        }
        return -1;
    }

    /** Flattens ItemStacks and ItemStack[]s into one assembly-line input array (at most 16 slots). */
    private static ItemStack[] items(Object... parts) {
        final List<ItemStack> out = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemStack) {
                out.add((ItemStack) part);
            } else if (part instanceof ItemStack[]) {
                Collections.addAll(out, (ItemStack[]) part);
            } else {
                throw new IllegalArgumentException("not an item input: " + part);
            }
        }
        if (out.size() > 16) {
            throw new IllegalStateException("assembly line takes at most 16 item inputs, got " + out.size());
        }
        return out.toArray(new ItemStack[0]);
    }

    /** Huge Neutronium Fluid Pipe, else Large, else 6 Neutronium plates per pipe (as SL-K03). */
    private static ItemStack[] neutroniumPipes(int count) {
        final ItemStack huge = GTOreDictUnificator.get(OrePrefixes.pipeHuge, Materials.Neutronium, count);
        if (huge != null) return new ItemStack[] { huge };
        final ItemStack large = GTOreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Neutronium, count);
        if (large != null) return new ItemStack[] { large };
        return new ItemStack[] { req(OrePrefixes.plate, Materials.Neutronium, Math.min(64, count * 6)) };
    }

    private static ItemStack c2(int meta, int count) {
        return SLBlocks.casing2(meta, count);
    }

    private static ItemStack item(ItemList entry, int count) {
        return nn(entry.get(count), "ItemList." + entry.name());
    }

    private static ItemStack circuit(Materials tier, int count) {
        return req(OrePrefixes.circuit, tier, count);
    }

    private static ItemStack req(OrePrefixes prefix, Materials material, int count) {
        return BTSRecipeGuard.req(prefix, material, count);
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static FluidStack molten(Materials material, int litres) {
        return nn(material.getMolten(litres), "molten " + material);
    }

    private static FluidStack superCoolant(int litres) {
        return nn(Materials.SuperCoolant.getFluid(litres), "Materials.SuperCoolant fluid");
    }

    /** The P4 recipe ids that wait for an MTE unit (for the gate dump and the build spec). */
    public static List<String> waiting() {
        return new ArrayList<>(WAITING);
    }
}
