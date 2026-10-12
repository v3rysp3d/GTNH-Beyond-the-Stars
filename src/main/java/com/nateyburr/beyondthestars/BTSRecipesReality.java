package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing3;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import gregtech.api.enums.CondensateType;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.fluid.GTFluidFactory;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;
import tectech.thing.block.BlockGodforgeGlass;
import tectech.thing.block.BlockQuantumGlass;

/**
 * HOMOGENY + REALITY MACHINES, the recipe unit (H/R unit 3; design repo {@code designs/docs/homogeny-reality.md} §5,
 * PROPOSED; brief ruling 16: "make sure all of these recipes integrate with Bose Einstein condensate assembler").
 * One owner; run from one {@code BTSRecipeGuard.line("reality", ...)} in {@link BTSRecipes}, after the machines' own
 * process recipes ({@code reality-process}) and, once master's BEC expansion is merged, after {@code bec}.
 * <p>
 * <b>Everything here is a Bose-Einstein Condensate recipe</b>: the Condensate Assembler (GT 2.9.0-RC-2's
 * {@code gt.recipe.assemble-condensate}, items with one nanite tier per stack, at most 16 stacks, at most 4
 * condensates, one item out, nanite-time durations, constant EU/t) for every block, core and controller of the three
 * machines, and the Condensate Generator ({@code gt.recipe.create-condensate}, source fluid -&gt; the same amount of
 * condensate) for the five addon condensates they use. Controllers too: ruling 16 overrides the BEC proposal's R4
 * for these three machines (H-19).
 * <ul>
 * <li><b>SL-C01..C04</b>: Entangled Degenerate Stellar Mixture, Ergomatter, Superdense Ergomatter, White Hole
 * Matter. <b>SL-B16</b> (Entangled Magnetar Matter) gets no generator recipe: ruling 19a moved the Homogeny Tier II
 * Core, its only consumer, from Magnetar Matter to Black Dwarf Matter, and an orphan condensate in NEI suggests a use
 * that does not exist (BEC Q-B6). Its fluid stays registered as a reserved name.</li>
 * <li><b>The BEC expansion's condensates</b> this unit needs (SL-B10 Raw Star Matter, SL-B13 White Dwarf Matter,
 * SL-B14 Black Dwarf Matter) are master's ({@code SLCondensates}, line {@code bec}). This branch forked before they
 * landed, so {@link #shim} finds each by its frozen registry name, registers it (same name, colour and generator
 * recipe as master) only if nothing did, and never adds a second generator recipe on the same source fluid. Merged,
 * master's run first and the shim only reads them.</li>
 * <li><b>SL-H01..H05</b> the Homogeny T1 (UMV, stage S1: Raw Star Matter only, nanites at most Transcendent),
 * <b>SL-H10</b> the Tier II Core (stage S5: Black Dwarf Matter is the gate, ruling 19a), <b>SL-H20</b> the Tier III
 * Core and <b>SL-R12</b> the Eternity Shell Casing, <b>SL-R01/R02</b> the two Reality controllers and
 * <b>SL-R10/R11/R13..R17</b> their blocks (stages S6-S9, nanites at most Black Dwarf: GT makes tiers 8-10 from
 * Universium, an Eye T10 product, H-17).</li>
 * </ul>
 * <b>Gate audit.</b> Nothing here needs a product first made at a later Eye tier than the thing it builds. Recipes
 * whose fluids belong to later phases (Ergomatter and Superdense Ergomatter, P4; White Hole Matter, P5) are
 * registered against the placeholder fluids {@link SLRealityFluids} provides, so they are visible in NEI, and
 * {@link #gateLog} prints one line per recipe (process recipes included) that cannot be run until an Eye makes them.
 * <p>
 * Every item set is unique (the Condensate Assembler finds a recipe by its items only). Condensate amounts keep GT's
 * half-unit rule (multiples of 72 L for the 144 L metals, 500 L for the 1,000 L fluids), checked by {@link #cond}.
 */
public final class BTSRecipesReality {

    // Nanite tiers by the design doc's numbers (GT's NaniteTier.tier): 1 Carbon .. 7 Black Dwarf. Never above 7.
    private static final NaniteTier N1 = NaniteTier.Carbon;
    private static final NaniteTier N2 = NaniteTier.Silver;
    private static final NaniteTier N3 = NaniteTier.Gold;
    private static final NaniteTier N4 = NaniteTier.Transcendent;
    private static final NaniteTier N5 = NaniteTier.SixPhasedCopper;
    private static final NaniteTier N6 = NaniteTier.WhiteDwarf;
    private static final NaniteTier N7 = NaniteTier.BlackDwarf;

    /** Units (L) of the condensates this file meters: 1,000 for the fluids, 144 for the molten matter. */
    private static final Map<Fluid, Integer> UNITS = new LinkedHashMap<>();

    // The condensates actually used (master's, GT's or ours), resolved at registration.
    private static Fluid starMatter;
    private static Fluid whiteDwarf;
    private static Fluid blackDwarf;
    private static Fluid degenerate;
    private static Fluid ergomatter;
    private static Fluid superdense;
    private static Fluid whiteHole;

    private static int gated;

    private BTSRecipesReality() {}

    public static void register() {
        UNITS.clear();
        starMatter = whiteDwarf = blackDwarf = degenerate = ergomatter = superdense = whiteHole = null;
        gated = 0;

        condensates();
        homogenyT1();
        homogenyT2();
        homogenyT3();
        realityBlocks();
        realityControllers();
        gateLog();
    }

    // ===================================================================================== condensates

    private static void condensates() {
        // The BEC expansion's three (master: SLCondensates + BTSRecipesBEC.condensates(), same names and numbers).
        BTSRecipeGuard.recipe(
            "SL-B10",
            () -> starMatter = shim(
                "SL-B10",
                "btsentangledstarmatter",
                "Entangled Raw Star Matter",
                186,
                120,
                255,
                1_000,
                () -> Materials.RawStarMatter.getFluid(1_000L),
                30 * SECONDS,
                TierEU.RECIPE_UIV));
        BTSRecipeGuard.recipe(
            "SL-B13",
            () -> whiteDwarf = shim(
                "SL-B13",
                "btsentangledwhitedwarf",
                "Entangled White Dwarf Matter",
                236,
                240,
                255,
                144,
                () -> Materials.WhiteDwarfMatter.getMolten(144L),
                120 * SECONDS,
                TierEU.RECIPE_UXV));
        BTSRecipeGuard.recipe(
            "SL-B14",
            () -> blackDwarf = shim(
                "SL-B14",
                "btsentangledblackdwarf",
                "Entangled Black Dwarf Matter",
                44,
                36,
                64,
                144,
                () -> Materials.BlackDwarfMatter.getMolten(144L),
                150 * SECONDS,
                TierEU.RECIPE_UXV));

        // Ours (doc §5.1). Tiers rise with the product, like GT's own table (BEC Q-B13).
        BTSRecipeGuard.recipe(
            "SL-C01",
            () -> degenerate = generator(
                "SL-C01",
                SLRealityFluids.entangledDegenerate,
                1_000,
                BTSRecipeGuard.fluid(SLRealityFluids.degenerateStellarMixture, 1_000),
                120 * SECONDS,
                TierEU.RECIPE_UXV));
        BTSRecipeGuard.recipe(
            "SL-C02",
            () -> ergomatter = generator(
                "SL-C02",
                SLRealityFluids.entangledErgomatter,
                1_000,
                BTSRecipeGuard.fluid(SLRealityFluids.ergomatter, 1_000),
                120 * SECONDS,
                TierEU.RECIPE_UXV));
        BTSRecipeGuard.recipe(
            "SL-C03",
            () -> superdense = generator(
                "SL-C03",
                SLRealityFluids.entangledSuperdense,
                1_000,
                BTSRecipeGuard.fluid(SLRealityFluids.superdenseErgomatter, 1_000),
                150 * SECONDS,
                4 * TierEU.RECIPE_UXV));
        BTSRecipeGuard.recipe(
            "SL-C04",
            () -> whiteHole = generator(
                "SL-C04",
                SLRealityFluids.entangledWhiteHole,
                144,
                BTSRecipeGuard.fluid(SLRealityFluids.whiteHoleMatter, 144),
                180 * SECONDS,
                TierEU.RECIPE_MAX));
        BeyondTheStars.LOG.info(
            "[BTS] SL-B16 Entangled Magnetar Matter: no Condensate Generator recipe (ruling 19a took Magnetar Matter "
                + "out of the Homogeny Tier II Core, its only consumer); the fluid stays a reserved name.");
    }

    /**
     * One of the BEC expansion's condensates (master's {@code SLCondensates}): the registered fluid of that name, or
     * - on a branch without it - the same fluid registered here; and its generator recipe unless the Condensate
     * Generator already condenses the source fluid (then that recipe's output is used, as master does).
     */
    private static Fluid shim(String id, String name, String localName, int r, int g, int b, int unit,
        Supplier<FluidStack> source, int ticks, long eut) {
        Fluid fluid = FluidRegistry.getFluid(name);
        if (fluid == null) {
            fluid = GTFluidFactory.builder(name)
                .withDefaultLocalName(localName)
                .withTextureName("molten.autogenerated")
                .withColorRGBA(new short[] { (short) r, (short) g, (short) b, (short) 200 })
                .withStateAndTemperature(FluidState.GAS, 0)
                .buildAndRegister()
                .addLocalizedName()
                .asFluid();
            BeyondTheStars.LOG.info(
                "[BTS] {}: {} was not registered (the BEC expansion's SLCondensates is not in this build); the "
                    + "Homogeny/Reality recipes registered it under the same name.",
                id,
                name);
        }
        return generator(id, fluid, unit, BTSRecipeGuard.nn(source.get(), id + " source fluid"), ticks, eut);
    }

    /**
     * The Condensate Generator recipe {@code source -> the same amount of condensate}, unless one already condenses
     * that source fluid (master's, or a future GT condensate): then its output fluid is the one recipes ask for.
     */
    private static Fluid generator(String id, Fluid ours, int unit, FluidStack source, int ticks, long eut) {
        BTSRecipeGuard.nn(ours, id + " condensate fluid");
        for (GTRecipe recipe : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mFluidInputs == null
                || recipe.mFluidInputs.length == 0
                || recipe.mFluidOutputs == null
                || recipe.mFluidOutputs.length == 0
                || recipe.mFluidInputs[0] == null
                || recipe.mFluidOutputs[0] == null) continue;
            if (recipe.mFluidInputs[0].getFluid() != source.getFluid()) continue;
            final Fluid theirs = recipe.mFluidOutputs[0].getFluid();
            UNITS.put(theirs, unit);
            BeyondTheStars.LOG.info(
                "[BTS] {}: the Condensate Generator already makes {} from {}; the Homogeny/Reality recipes use it.",
                id,
                theirs.getName(),
                source.getFluid()
                    .getName());
            return theirs;
        }
        added(
            GTValues.RA.stdBuilder()
                .fluidInputs(source)
                .fluidOutputs(new FluidStack(ours, source.amount))
                .duration(ticks)
                .eut(eut),
            TecTechRecipeMaps.condensateGeneratorRecipes);
        UNITS.put(ours, unit);
        return ours;
    }

    // ====================================================================== Homogeny T1 (UMV, stage S1)
    // Per-craft counts are the doc's (§5.2-§5.5) except where the exported BOM (manifest.json, B-60) made a
    // component balloon: H02 pistons 8 -> 2, H03 field coils 8 -> 4 / SC UMV wire 16 -> 8 / motors 4 -> 2, R11
    // emitters 4 -> 2, R12 dense Infinity 16 -> 8 and SpaceTime 64 -> 32 ingots, R13 field generators 2 -> 1, R14
    // dense Infinity 32 -> 16 and SpaceTime 64 -> 32 ingots, R15 Casimir Plates 8 -> 4, R16 output x8 -> x32
    // (costs doc §12, question B-65).

    private static void homogenyT1() {
        // SL-H01 The Homogeny (controller). The card was an assembly line; its 2M L Super Coolant is dropped (not a
        // condensate) and Infinity + two metamaterials stand in. Gate: 1,000,000 L Raw Star Matter (20 min of Sol).
        BTSRecipeGuard.recipe(
            "SL-H01",
            () -> assemble(
                BTSRecipeGuard.nn(BTSMachines.homogeny, "BTSMachines.homogeny")
                    .copy(),
                3_600 * SECONDS,
                4 * TierEU.RECIPE_UMV,
                cond(
                    cond(starMatter, 1_000_000),
                    gt(CondensateType.SpaceTime, 4_096 * INGOTS),
                    gt(CondensateType.Infinity, 256 * INGOTS)),
                s(c3(BlockSLCasing3.STIRRING_BAND, 16), N2),
                s(c3(BlockSLCasing3.VORTEX_INLET, 4), N2),
                s(item(ItemList.Field_Generator_UMV, 16), N3),
                s(item(ItemList.Electric_Pump_UMV, 32), N2),
                s(circuit(Materials.UMV, 32), N2),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 32), N1),
                s(dense(Materials.SpaceTime, 16), N1),
                s(item(ItemList.MetaMaterial_EnergyConduit1, 4), N3),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 4), N4)));

        // SL-H02 Stirring Band Casing x32. The card's dense White Dwarf Matter (Eye T4) is an audit fix: Raw Star
        // Matter instead. 75 s of nanite-time per block, RC-2's BEC casing rate.
        BTSRecipeGuard.recipe(
            "SL-H02",
            () -> assemble(
                c3(BlockSLCasing3.STIRRING_BAND, 32),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(gt(CondensateType.SpaceTime, 64 * INGOTS), cond(starMatter, 8_000)),
                s(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 2), N4),
                s(dense(Materials.Infinity, 8), N1),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 8L), N1),
                s(item(ItemList.Electric_Piston_UMV, 2), N3),
                s(item(ItemList.MetaMaterial_Waveguide1, 2), N3)));

        // SL-H03 Shear Helix Coil x32: the Kerr's Ergosphere Field Coil, rewound.
        BTSRecipeGuard.recipe(
            "SL-H03",
            () -> assemble(
                c3(BlockSLCasing3.SHEAR_HELIX, 32),
                1_200 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(gt(CondensateType.SpaceTime, 16 * INGOTS), gt(CondensateType.Space, 8 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 4), N2),
                s(BTSRecipeGuard.req(OrePrefixes.wireGt04, Materials.SuperconductorUMV, 8L), N2),
                s(BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 32L), N1),
                s(item(ItemList.Electric_Motor_UMV, 2), N3),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 1), N4)));

        // SL-H04 Feed Vessel Casing x32.
        BTSRecipeGuard.recipe(
            "SL-H04",
            () -> assemble(
                c3(BlockSLCasing3.FEED_VESSEL, 32),
                900 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(gt(CondensateType.SpaceTime, 8 * INGOTS), gt(CondensateType.Infinity, 16 * INGOTS)),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 32L), N1),
                s(block(BlockQuantumGlass.INSTANCE, 0, 8, "TecTech Quantum Glass"), N1),
                s(item(ItemList.Electric_Pump_UMV, 2), N2),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 4L), N1),
                s(item(ItemList.MetaMaterial_Shielding1, 1), N3)));

        // SL-H05 Vortex Inlet x8. The card was an assembly line with Eddington Wall Casing (a P4 block): audit fix.
        BTSRecipeGuard.recipe(
            "SL-H05",
            () -> assemble(
                c3(BlockSLCasing3.VORTEX_INLET, 8),
                600 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(gt(CondensateType.SpaceTime, 16 * INGOTS), cond(starMatter, 4_000)),
                s(item(ItemList.Electric_Pump_UMV, 8), N2),
                s(neutroniumPipe(8), N1),
                s(c3(BlockSLCasing3.STIRRING_BAND, 4), N2),
                s(circuit(Materials.UMV, 4), N2)));
    }

    // =================================================================== Homogeny T2 core (UXV, stage S5)

    private static void homogenyT2() {
        // SL-H10 Homogeny Tier II Core. Ruling 19a: NOT Magnetar Matter (Eye T6 wants the Homogeny's Degenerate
        // Stellar Mixture before any Magnetar Matter exists); Black Dwarf Matter (Eye T5) is the gate instead, at
        // the Magnetar Seed Core's 256 ingots. White Dwarf nanite steps and the two WDM metamaterials are T4
        // products, so the core is buildable right after Eye T5.
        BTSRecipeGuard.recipe(
            "SL-H10",
            () -> assemble(
                BTSRecipeGuard.stack(SLMaxItems.homogenyTier2Core, 1),
                4_800 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    cond(blackDwarf, 256 * INGOTS),
                    cond(whiteDwarf, 256 * INGOTS),
                    gt(CondensateType.SpaceTime, 1_024 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64), N2),
                s(c3(BlockSLCasing3.FEED_VESSEL, 16), N2),
                s(c3(BlockSLCasing3.VORTEX_INLET, 8), N2),
                s(item(ItemList.Field_Generator_UXV, 16), N5),
                s(item(ItemList.Electric_Pump_UXV, 32), N4),
                s(circuit(Materials.UXV, 32), N4),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 32), N2),
                s(item(ItemList.MetaMaterial_ElectrograviticValve2, 4), N6),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 4), N6)));
    }

    // ============================================================ Homogeny T3 core + Eternity Shell (S6 / S9)

    private static void homogenyT3() {
        // SL-H20 Homogeny Tier III Core. Gate: White Hole Matter (Eye T9, P5): unreachable until P5.
        BTSRecipeGuard.recipe(
            "SL-H20",
            () -> assemble(
                BTSRecipeGuard.stack(SLMaxItems.homogenyTier3Core, 1),
                6_000 * SECONDS,
                TierEU.RECIPE_MAX,
                cond(
                    cond(whiteHole, 64 * INGOTS),
                    cond(superdense, 32_000),
                    cond(degenerate, 64_000),
                    gt(CondensateType.Eternity, 64 * INGOTS)),
                s(c3(BlockSLCasing3.ETERNITY_SHELL, 64), N6),
                s(c3(BlockSLCasing3.REGION_CRYSTAL, 8), N7),
                s(item(ItemList.Field_Generator_UXV, 64), N6),
                s(item(ItemList.Electric_Pump_UXV, 64), N5),
                s(circuit(Materials.UXV, 64), N5),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64), N3),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 8), N7),
                s(item(ItemList.MetaMaterial_SensorArray3, 4), N7)));

        // SL-R12 Eternity Shell Casing x32 (H-20): the card's MagMatter + SpaceTime identity; its dwarf matter comes
        // through Degenerate Stellar Mixture (about 1.1 L each of WDM and BDM per block), not dense dwarf plates.
        BTSRecipeGuard.recipe(
            "SL-R12",
            () -> assemble(
                c3(BlockSLCasing3.ETERNITY_SHELL, 32),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), gt(CondensateType.SpaceTime, 32 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 2), N4),
                s(dense(Materials.Infinity, 8), N1),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.MagMatter, 1L), N5),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 8L), N2),
                s(item(ItemList.MetaMaterial_Shielding2, 2), N6)));
    }

    // ========================================================= the Reality machines' blocks (MAX; S6 / S8)

    private static void realityBlocks() {
        // SL-R10 Region Deck Band x32 (Diffraction).
        BTSRecipeGuard.recipe(
            "SL-R10",
            () -> assemble(
                c3(BlockSLCasing3.REGION_DECK_BAND, 32),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), gt(CondensateType.SpaceTime, 32 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 2), N4),
                s(dense(Materials.TranscendentMetal, 16), N2),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 8L), N1),
                s(item(ItemList.ElectromagneticallyIsolatedCasing, 1), N4),
                s(item(ItemList.MetaMaterial_Shielding2, 1), N6)));

        // SL-R11 Region Crystal x16: the sketches' orange diamonds. Ergomatter (Eye T7) + Superdense (Eye T8): P4.
        BTSRecipeGuard.recipe(
            "SL-R11",
            () -> assemble(
                c3(BlockSLCasing3.REGION_CRYSTAL, 16),
                2_400 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    cond(ergomatter, 1_000),
                    cond(superdense, 500),
                    cond(degenerate, 500),
                    gt(CondensateType.MagMatter, 1 * INGOTS)),
                s(block(BlockGodforgeGlass.INSTANCE, 0, 2, "Spatially Transcendent Gravitational Lens Block"), N6),
                s(BTSRecipeGuard.req(OrePrefixes.block, Materials.Hexanite, 2L), N4),
                s(item(ItemList.Emitter_UXV, 2), N5),
                s(item(ItemList.MetaMaterial_WaveFocus2, 2), N6)));

        // SL-R13 Phase Ring Casing x32 (Diffraction). Ergomatter: P4.
        BTSRecipeGuard.recipe(
            "SL-R13",
            () -> assemble(
                c3(BlockSLCasing3.PHASE_RING, 32),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), cond(ergomatter, 1_000), gt(CondensateType.SpaceTime, 32 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 2), N4),
                s(dense(Materials.CosmicNeutronium, 16), N2),
                s(item(ItemList.Field_Generator_UXV, 1), N6),
                s(item(ItemList.CondensateTransformativeCoil, 1), N5),
                s(item(ItemList.MetaMaterial_Waveguide2, 1), N6)));

        // SL-R14 Reassembly Hull Plating x64 (Refab: stem, ribs, belt, cap).
        BTSRecipeGuard.recipe(
            "SL-R14",
            () -> assemble(
                c3(BlockSLCasing3.REASSEMBLY_HULL, 64),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), gt(CondensateType.SpaceTime, 32 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 2), N4),
                s(dense(Materials.Infinity, 16), N1),
                s(dense(Materials.CosmicNeutronium, 16), N2),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 16L), N2),
                s(item(ItemList.MetaMaterial_Shielding2, 2), N6)));

        // SL-R15 Reassembly Tray Plating x32 (Refab trays).
        BTSRecipeGuard.recipe(
            "SL-R15",
            () -> assemble(
                c3(BlockSLCasing3.REASSEMBLY_TRAY, 32),
                2_400 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), gt(CondensateType.SpaceTime, 32 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 1), N4),
                s(dense(Materials.TranscendentMetal, 16), N2),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 4), N3),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 8L), N1),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 1), N6)));

        // SL-R16 Reassembly Claw x32. The doc's x8 was priced for its estimate of 190 claws; the exported Refab has
        // 688, so x32 keeps the bill near the doc's (22 crafts, 176 Robot Arm UXV). Superdense Ergomatter: P4.
        BTSRecipeGuard.recipe(
            "SL-R16",
            () -> assemble(
                c3(BlockSLCasing3.REASSEMBLY_CLAW, 32),
                1_200 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(superdense, 500), cond(degenerate, 500)),
                s(item(ItemList.Robot_Arm_UXV, 8), N5),
                s(item(ItemList.Conveyor_Module_UXV, 4), N5),
                s(c3(BlockSLCasing3.REASSEMBLY_TRAY, 4), N3),
                s(circuit(Materials.UXV, 4), N4),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 1), N6)));

        // SL-R17 Region Anchor Casing x4: the red lens either side of each Region Anchor socket (12 per
        // Diffraction; unit 1 added the block after the doc's table). Region Crystal makes it P4.
        BTSRecipeGuard.recipe(
            "SL-R17",
            () -> assemble(
                c3(BlockSLCasing3.REGION_ANCHOR, 4),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(cond(degenerate, 500), gt(CondensateType.SpaceTime, 8 * INGOTS)),
                s(c3(BlockSLCasing3.REGION_CRYSTAL, 2), N7),
                s(item(ItemList.ElectromagneticallyIsolatedCasing, 2), N4),
                s(SLBlocks.casing2(BlockSLCasing2.LINING, 4), N1),
                s(item(ItemList.Sensor_UXV, 1), N5)));
    }

    // ============================================================= the Reality controllers (MAX, stage S9)

    private static void realityControllers() {
        // SL-R01 Reality Diffraction. Gate: White Hole Matter (Eye T9, P5).
        BTSRecipeGuard.recipe(
            "SL-R01",
            () -> assemble(
                BTSRecipeGuard.nn(BTSMachines.realityDiffraction, "BTSMachines.realityDiffraction")
                    .copy(),
                7_200 * SECONDS,
                TierEU.RECIPE_MAX,
                cond(
                    cond(whiteHole, 256 * INGOTS),
                    cond(superdense, 64_000),
                    cond(degenerate, 64_000),
                    gt(CondensateType.SpaceTime, 4_096 * INGOTS)),
                s(c3(BlockSLCasing3.REGION_CRYSTAL, 6), N7),
                s(c3(BlockSLCasing3.PHASE_RING, 32), N6),
                s(c3(BlockSLCasing3.REGION_DECK_BAND, 32), N6),
                s(item(ItemList.Field_Generator_UXV, 32), N6),
                s(item(ItemList.Emitter_UXV, 32), N5),
                s(circuit(Materials.UXV, 64), N5),
                s(item(ItemList.CoherencePreservingPlasmaConduit, 16), N5),
                s(item(ItemList.CondensateTransformativeCoil, 8), N5),
                s(item(ItemList.MetaMaterial_SensorArray3, 4), N7),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 4), N7)));

        // SL-R02 Reality Refab. Gate: White Hole Matter (Eye T9, P5).
        BTSRecipeGuard.recipe(
            "SL-R02",
            () -> assemble(
                BTSRecipeGuard.nn(BTSMachines.realityRefab, "BTSMachines.realityRefab")
                    .copy(),
                7_200 * SECONDS,
                TierEU.RECIPE_MAX,
                cond(
                    cond(whiteHole, 256 * INGOTS),
                    gt(CondensateType.Eternity, 128 * INGOTS),
                    cond(degenerate, 64_000),
                    gt(CondensateType.SpaceTime, 4_096 * INGOTS)),
                s(c3(BlockSLCasing3.REGION_CRYSTAL, 8), N7),
                s(c3(BlockSLCasing3.REASSEMBLY_HULL, 32), N6),
                s(c3(BlockSLCasing3.REASSEMBLY_CLAW, 24), N6),
                s(item(ItemList.Robot_Arm_UXV, 32), N5),
                s(item(ItemList.Field_Generator_UXV, 16), N6),
                s(circuit(Materials.UXV, 64), N5),
                s(item(ItemList.CondensateGuidanceCoil, 16), N5),
                s(item(ItemList.ElectromagneticallyIsolatedCasing, 16), N4),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 8), N6),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 4), N7)));
    }

    // ======================================================================================= gate log

    /**
     * One line per recipe (ours and the machines' process recipes) that needs a later phase's fluid nobody makes
     * yet: registered and visible in NEI, but not runnable until an Eye tier makes it (H-24, question B-65).
     */
    private static void gateLog() {
        final Fluid erg = SLRealityFluids.ergomatter;
        final Fluid sup = SLRealityFluids.superdenseErgomatter;
        final Fluid whm = SLRealityFluids.whiteHoleMatter;
        // Region Crystal needs Ergomatter + Superdense; anything holding one inherits that.
        gate("SL-C02 Entangled Ergomatter", erg);
        gate("SL-C03 Entangled Superdense Ergomatter", sup);
        gate("SL-C04 Entangled White Hole Matter", whm);
        gate("SL-R11 Region Crystal (Diffraction blades, Refab receivers, Homogeny T3 tail)", erg, sup);
        gate("SL-R13 Phase Ring Casing", erg);
        gate("SL-R16 Reassembly Claw", sup);
        gate("SL-R17 Region Anchor Casing (via Region Crystal)", erg, sup);
        gate("SL-H20 Homogeny Tier III Core", whm, sup, erg);
        gate("SL-R01 Reality Diffraction", whm, sup, erg);
        gate("SL-R02 Reality Refab", whm, sup, erg);
        // The machines' own maps (RealityProcessRecipes, line reality-process).
        gate("SL-H21 Entanglement Medium (Homogeny T3)", whm, sup);
        gate("SL-R03 Diffraction (medium and anchors V/VI)", whm, sup);
        gate("SL-R04 Universal Upgrade Core (Refab, from the six regions)", whm, sup);
        gate("SL-R05 Star Chart: The Observable Universe (Refab)", whm, sup);
        if (gated == 0) {
            BeyondTheStars.LOG.info(
                "[BTS] H/R gate: Ergomatter, Superdense Ergomatter and White Hole Matter all have an owner; "
                    + "every Homogeny/Reality recipe is reachable.");
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] H/R gate: {} Homogeny/Reality recipe(s) are registered but unreachable until P4/P5 build "
                    + "the Eye tiers that make their fluids (question B-65).",
                gated);
        }
    }

    private static void gate(String what, Fluid... needs) {
        final List<String> missing = new ArrayList<>();
        for (Fluid fluid : needs) {
            if (!SLRealityFluids.isPlaceholder(fluid)) continue;
            if (fluid == SLRealityFluids.ergomatter) missing.add("Ergomatter (Eye T7, P4)");
            else if (fluid == SLRealityFluids.superdenseErgomatter) missing.add("Superdense Ergomatter (Eye T8, P4)");
            else if (fluid == SLRealityFluids.whiteHoleMatter) missing.add("White Hole Matter (Eye T9, P5)");
            else missing.add(fluid.getName());
        }
        if (missing.isEmpty()) return;
        gated++;
        BeyondTheStars.LOG.info(
            "[BTS] H/R gate: {} is registered but unreachable until an Eye makes {}.",
            what,
            String.join(", ", missing));
    }

    // ======================================================================================= helpers

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
            BTSRecipeGuard.nn(part, "an item input");
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
     * {@code litres} of one of the condensates this file resolved, checked against GT's half-unit rule. Throws (a
     * logged skip) if its generator recipe could not be built.
     */
    private static FluidStack cond(Fluid condensate, int litres) {
        final Fluid fluid = BTSRecipeGuard.nn(condensate, "a condensate whose generator recipe was skipped");
        final Integer unit = UNITS.get(fluid);
        if (unit == null) {
            throw new IllegalStateException("no Condensate Generator recipe makes " + fluid.getName());
        }
        if (litres % (unit / 2) != 0) {
            throw new IllegalArgumentException(
                litres + " L of " + fluid.getName() + " is not a multiple of half its " + unit + " L unit");
        }
        return new FluidStack(fluid, litres);
    }

    /** {@code litres} of one of GT's own condensates (litres, not units: write N * INGOTS for the metals). */
    private static FluidStack gt(CondensateType type, int litres) {
        return BTSRecipeGuard.nn(type.getEntangled(litres), "entangled " + type.name());
    }

    /**
     * Registers one Condensate Assembler recipe: one nanite tier per item stack (GT's map throws otherwise), at most
     * 16 item stacks and 4 condensates, and GT must keep it (an empty addTo throws, so the guard logs a skip).
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
        for (NaniteTier tier : tiers) {
            if (tier.getTier() > N7.getTier()) {
                throw new IllegalStateException("nanite tier " + tier + " needs Universium (H-17)");
            }
        }
        added(
            GTValues.RA.stdBuilder()
                .itemInputs(items.toArray(new ItemStack[0]))
                .fluidInputs(condensates)
                .itemOutputs(BTSRecipeGuard.nn(output, "the output"))
                .metadata(NANITE_TIERS, tiers.toArray(new NaniteTier[0]))
                .duration(ticks)
                .eut(eut),
            TecTechRecipeMaps.condensateAssemblingRecipes);
    }

    /** {@code builder.addTo(map)}, throwing if GT kept nothing (so the guard counts a skip). */
    private static void added(GTRecipeBuilder builder, RecipeMap<?> map) {
        final Collection<GTRecipe> recipes = builder.addTo(map);
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalStateException("GT rejected the recipe for " + map.unlocalizedName);
        }
    }

    private static ItemStack c3(int meta, int count) {
        return SLBlocks.casing3(meta, count);
    }

    private static ItemStack item(ItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.get(count), "ItemList." + entry.name());
    }

    private static ItemStack circuit(Materials tier, int count) {
        return BTSRecipeGuard.req(OrePrefixes.circuit, tier, count);
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.hasBeenSet() ? entry.get(count) : null, "CustomItemList." + entry.name());
    }

    private static ItemStack block(Block block, int meta, int count, String what) {
        return new ItemStack(BTSRecipeGuard.nn(block, what), count, meta);
    }

    /** Dense plates; else the block (the same 9 ingots, one slot); else 9 plates each. */
    private static Object dense(Materials material, int count) {
        final ItemStack dense = GTOreDictUnificator.get(OrePrefixes.plateDense, material, count);
        if (dense != null) return dense;
        final ItemStack block = GTOreDictUnificator.get(OrePrefixes.block, material, count);
        if (block != null) return block;
        return BTSRecipeGuard.req(OrePrefixes.plate, material, count * 9L);
    }

    /** Huge Neutronium pipes; else large ones. */
    private static ItemStack neutroniumPipe(int count) {
        final ItemStack huge = GTOreDictUnificator.get(OrePrefixes.pipeHuge, Materials.Neutronium, count);
        return huge != null ? huge : BTSRecipeGuard.req(OrePrefixes.pipeLarge, Materials.Neutronium, count);
    }
}
