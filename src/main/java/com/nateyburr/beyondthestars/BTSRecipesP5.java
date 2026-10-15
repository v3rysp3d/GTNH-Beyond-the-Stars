package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.alloyBlastSmelterRecipes;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.benderRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidSolidifierRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.COAL_CASING_TIER;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing4;
import com.nateyburr.beyondthestars.blocks.BlockSLGlass;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLCondensates;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLP5Items;
import com.nateyburr.beyondthestars.machines.BTSMaxHatches;
import com.nateyburr.beyondthestars.machines.kerr.KerrRecipeMaps;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.util.ItemRefer;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * P5: THE GREAT ATTRACTOR, MAX AND UXV PARTS (design repo {@code designs/docs/p5-t9-t10.md} §3, §7; D-47, D-52, D-53,
 * Q-P5-1..13 accepted 2026-10-16). <b>One owner: the P5 content unit.</b> Called from one line in {@link BTSRecipes}
 * ("p5", after "quasar"). The Homogeny / Reality changes (SL-C04, SL-H20, SL-H21, SL-R01..R06) live with their own
 * lines ({@link BTSRecipesReality}, {@code RealityProcessRecipes}); the lattice nodes with {@link BTSRecipesQuasar}.
 *
 * <h2>What is here</h2>
 * <ul>
 * <li><b>T8 -> T9</b> (gate: nothing first made at T9): SL-F01..F03 the Gated Pellets on the Kerr Compressor module's
 * DENSITY map, each pressed from the grade below; SL-F04..F06 the Injector Barrels Mk I-III; SL-F07 the Attractor Seed
 * Core; SL-F08 Star Chart: Great Attractor; SL-F09 the Pellet Injector Hatch (MTE 28128, the Eye unit's machine: it
 * waits, one log line, until that id is registered).</li>
 * <li><b>Attractor Dark Matter's solid forms</b>: SL-M01 plate (Fluid Solidifier, 144 L), SL-M02 dense plate (Bender,
 * 9 plates), SL-M03 frame box (Assembler, 2 plates).</li>
 * <li><b>MAX parts</b> (§7.2, every one before Eye T10, so no Universium, MHDCSM or nanite above Black Dwarf; H-18):
 * SL-M04 MAX Superconductor Wire, SL-M05 Dark Matter Borosilicate Glass, SL-M06 Dark Matter Heating Coil, SL-M07 MAX
 * Battery, SL-M08 / M09 MAX Machine Casing / Hull, SL-M10..M17 the eight MAX components (Component Assembly Line, MAX
 * casing tier), SL-M18 the MAX Component Assembly Line Casing, SL-M19 / M20 MAX Energy / Dynamo Hatch, SL-M21 MAX
 * Wireless Energy Hatch, SL-M22 / M23 the MAX 4,096 A Laser Target / Source Hatch.</li>
 * <li><b>UXV leftovers</b> (§7.3, ruling 19: before Eye T7, so Magnetar Matter and the Antipolarization Agent only):
 * SL-V01 UXV Superconductor Wire, SL-V02 Magnetar-Reinforced Borosilicate Glass, SL-V03 / V04 the UXV 4,096 A Laser
 * Target / Source Hatch.</li>
 * </ul>
 *
 * <h2>Costs (back-solved against one Eye T9, about 12,300 L/h of Attractor Dark Matter)</h2>
 * Every MAX card's Dark Matter, counted "all in" (plates 144 L, dense plates 1,296 L, frame boxes 288 L, MAX wire 54 L
 * each, the hull 2,304 L), equals §7.2: wire x16 864 L, motor / pump / conveyor / piston / robot arm 2,304 L, emitter /
 * sensor 3,456 L, field generator 4,608 L, CoAL casing 4,608 L, casing and hull 2,304 L, energy / dynamo / wireless
 * hatches 9,216 L, laser hatches 18,432 L, coil x2 1,152 L, glass x4 144 L, frame box 288 L, battery 36,864 L.
 * <p>
 * <b>The B-72 rule</b> (question B-205): RC-2's UXV components need molten Universium and MHDCSM (Eye T10; read off
 * the live recipes by {@code /btseye gates}), so where §7 asks for "4 of the UXV part" a MAX card takes 8 of the UMV
 * part, and the UXV laser hatches take UMV hatches, sensors and emitters. Research items are parts a player can hold
 * before T10.
 */
public final class BTSRecipesP5 {

    // Nanite tiers (GT's NaniteTier.tier): never above 7, Black Dwarf (8+ are Universium-made).
    private static final NaniteTier N3 = NaniteTier.Gold;
    private static final NaniteTier N4 = NaniteTier.Transcendent;
    private static final NaniteTier N5 = NaniteTier.SixPhasedCopper;
    private static final NaniteTier N6 = NaniteTier.WhiteDwarf;
    private static final NaniteTier N7 = NaniteTier.BlackDwarf;

    /** Attractor Dark Matter in one plate (L). */
    public static final int DM_PLATE = 144;
    /** In one dense plate: nine plates. */
    public static final int DM_DENSE = 9 * DM_PLATE;
    /** In one MAX Superconductor Wire: 864 L per 16. */
    public static final int DM_WIRE = 864 / 16;
    /** In one MAX Machine Hull (its casing's). */
    public static final int DM_HULL = 2_304;
    /** Pellets per craft (§3.2). */
    public static final int PELLETS_PER_CRAFT = 16;
    /** The Component Assembly Line's MAX casing tier (GoodGenerator: LV 1 .. MAX 14). */
    private static final int COAL_MAX = 14;
    /** Components per Component Assembly Line batch. */
    private static final int BATCH = 16;

    /** P5 recipes that wait for another unit's machine (the Pellet Injector Hatch). */
    private static final List<String> WAITING = new ArrayList<>();

    private BTSRecipesP5() {}

    public static void register() {
        WAITING.clear();
        pellets();
        t9Parts();
        darkMatterSolids();
        uxvLeftovers();
        maxMaterials();
        maxComponents();
        maxPower();
        if (WAITING.isEmpty()) {
            BeyondTheStars.LOG.info("[BTS] P5 recipes: every machine recipe is registered.");
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] P5 recipes: {} wait for their machine (not registered in this build): {}",
                WAITING.size(),
                String.join(", ", WAITING));
        }
    }

    // ================================================================= the Gated Pellets (Kerr DENSITY, §3.2)

    private static void pellets() {
        // SL-F01 Gated Pellet (Magnetar) x16: Magnetar Matter 72 L + Degenerate Stellar Mixture 1,000 L + 2 Casimir
        // Plates. 1.25 EeV a pellet: the primer, the core of the Ergomatter grade.
        BTSRecipeGuard.recipe(
            "SL-F01",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(SLBlocks.casing(BlockSLCasing.CASIMIR, 2))
                    .fluidInputs(
                        BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 72),
                        BTSRecipeGuard.fluid(SLRealityFluids.degenerateStellarMixture, 1_000))
                    .itemOutputs(pellet(1, PELLETS_PER_CRAFT))
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                KerrRecipeMaps.DENSITY));
        // SL-F02 Gated Pellet (Ergomatter) x16: 4 Magnetar pellets + Ergomatter 1,000 L. 5 EeV.
        BTSRecipeGuard.recipe(
            "SL-F02",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(pellet(1, 4))
                    .fluidInputs(BTSRecipeGuard.fluid(SLQuasarFluids.ergomatter, 1_000))
                    .itemOutputs(pellet(2, PELLETS_PER_CRAFT))
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                KerrRecipeMaps.DENSITY));
        // SL-F03 Gated Pellet (Superdense) x16: 4 Ergomatter pellets + Superdense Ergomatter 144 L. 20 EeV, T9's fuel:
        // one pellet carries 9 L Superdense, 15.6 L Ergomatter, 0.28 L Magnetar Matter and 3.9 L Degenerate Mixture.
        BTSRecipeGuard.recipe(
            "SL-F03",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(pellet(2, 4))
                    .fluidInputs(BTSRecipeGuard.fluid(SLQuasarFluids.superdenseErgomatter, 144))
                    .itemOutputs(pellet(3, PELLETS_PER_CRAFT))
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                KerrRecipeMaps.DENSITY));
    }

    // ============================================================ the T8 -> T9 parts (barrels, core, chart, hatch)

    private static void t9Parts() {
        // SL-F04 Injector Barrel Mk I (4 pellets a second): the Conduit Throat Mk I pattern, a Jet Collimator Ring
        // core (the collimator stack is the accelerator). Condensate Assembler, UXV.
        BTSRecipeGuard.recipe(
            "SL-F04",
            () -> assemble(
                stack(SLP5Items.injectorBarrelMk1, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(gt(CondensateType.SpaceTime, 28 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 16), N4),
                s(item(ItemList.Field_Generator_UMV, 4), N5),
                s(SLBlocks.casing2(BlockSLCasing2.JET_COLLIMATOR, 4), N3)));
        // SL-F05 Injector Barrel Mk II (8 a second): 4 Mk I + the Agent (pre-T7 matter, as the Throat Mk II).
        BTSRecipeGuard.recipe(
            "SL-F05",
            () -> assemble(
                stack(SLP5Items.injectorBarrelMk2, 1),
                900 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(ours(SLCondensates.entangledAgent, 32_000, 1_000)),
                s(stack(SLP5Items.injectorBarrelMk1, 4), N4),
                s(item(ItemList.Field_Generator_UMV, 16), N5),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 32), N4)));
        // SL-F06 Injector Barrel Mk III (16 a second): 4 Mk II + Superdense Ergomatter 16,000 L (Eye T8). Assembly
        // line, research: the Mk II.
        BTSRecipeGuard.recipe(
            "SL-F06",
            () -> assemblyLine(
                stack(SLP5Items.injectorBarrelMk2, 1),
                stack(SLP5Items.injectorBarrelMk3, 1),
                900 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    stack(SLP5Items.injectorBarrelMk2, 4),
                    item(ItemList.Field_Generator_UMV, 16),
                    SLBlocks.casing(BlockSLCasing.CASIMIR, 64),
                    BTSRecipeGuard.anyCircuit(Materials.UXV, 16)),
                BTSRecipeGuard.fluid(SLQuasarFluids.superdenseErgomatter, 16_000)));

        // SL-F07 Attractor Seed Core (Eye T8 -> T9, The Infall; §2.8): T8's product, with T7's and T6's. About 3.2 h
        // of one Eye T8 in its Superdense. Condensate Assembler, UXV, 3,600 s.
        BTSRecipeGuard.recipe(
            "SL-F07",
            () -> assemble(
                stack(SLP5Items.attractorSeedCore, 1),
                3_600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLRealityFluids.entangledSuperdense, 64_000, 1_000),
                    ours(SLRealityFluids.entangledErgomatter, 32_000, 1_000),
                    ours(SLRealityFluids.entangledMagnetar, 2_304, 144)),
                s(pellet(3, 64), N5),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 64), N3),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 32), N7),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 32), N6)));

        // SL-F08 Star Chart: Great Attractor (Eye T9), gated on Superdense Ergomatter (T8), as each chart is gated on
        // the tier below. Four Superdense pellets where the Milky Way chart has a Bastion Plate keep the sets apart.
        BTSRecipeGuard.recipe(
            "SL-F08",
            () -> assemble(
                stack(SLP5Items.starChartGreatAttractor, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLRealityFluids.entangledSuperdense, 4_000, 1_000),
                    gt(CondensateType.SpaceTime, 28 * INGOTS)),
                s(item(ItemList.Tool_DataOrb, 1), N4),
                s(item(ItemList.Sensor_UMV, 4), N6),
                s(pellet(3, 4), N5)));

        // SL-F09 Pellet Injector Hatch (MTE 28128, the Eye unit's machine). Assembler, UMV: a UMV hull, robot arms to
        // feed the barrel, field generators and collimator rings to aim it. Waits while 28128 is not registered.
        final ItemStack hatch = mte(Config.mteBaseId + 28);
        if (hatch == null) {
            WAITING.add("SL-F09 Pellet Injector Hatch (MTE " + (Config.mteBaseId + 28) + ")");
            return;
        }
        BTSRecipeGuard.recipe("SL-F09", () -> {
            added(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        item(ItemList.Hull_UMV, 1),
                        item(ItemList.Robot_Arm_UMV, 4),
                        item(ItemList.Field_Generator_UMV, 2),
                        SLBlocks.casing2(BlockSLCasing2.JET_COLLIMATOR, 4),
                        circuit(Materials.UXV, 2))
                    .itemOutputs(hatch)
                    .duration(600 * SECONDS)
                    .eut(TierEU.RECIPE_UMV),
                assemblerRecipes);
        });
    }

    // ======================================================= Attractor Dark Matter: plate, dense plate, frame

    private static void darkMatterSolids() {
        // SL-M01 Attractor Dark Matter Plate: 144 L in a plate mould (not consumed).
        BTSRecipeGuard.recipe(
            "SL-M01",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(item(ItemList.Shape_Mold_Plate, 0))
                    .fluidInputs(darkMatter(DM_PLATE))
                    .itemOutputs(stack(SLP5Items.attractorDarkMatterPlate, 1))
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                fluidSolidifierRecipes));
        // SL-M02 Attractor Dark Matter Dense Plate: nine plates on circuit 9, GT's dense-plate rule.
        BTSRecipeGuard.recipe(
            "SL-M02",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(stack(SLP5Items.attractorDarkMatterPlate, 9), GTUtility.getIntegratedCircuit(9))
                    .itemOutputs(stack(SLP5Items.attractorDarkMatterDensePlate, 1))
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                benderRecipes));
        // SL-M03 Attractor Dark Matter Frame Box: two plates (288 L), circuit 4.
        BTSRecipeGuard.recipe(
            "SL-M03",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(stack(SLP5Items.attractorDarkMatterPlate, 2), GTUtility.getIntegratedCircuit(4))
                    .itemOutputs(SLBlocks.casing4(BlockSLCasing4.DARK_MATTER_FRAME, 1))
                    .duration(10 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                assemblerRecipes));
    }

    // ============================================================== the UXV leftovers (§7.3, before Eye T7)

    private static void uxvLeftovers() {
        // SL-V01 UXV Superconductor Wire x16 (Alloy Blast Smelter, UXV): 16 UMV Superconductor Wire, 576 L Magnetar
        // Matter (the spec's 4 plates: Magnetar Matter has no plate), 2,000 L Agent, 64,000 L Super Coolant.
        BTSRecipeGuard.recipe(
            "SL-V01",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUMV, 16))
                    .fluidInputs(
                        BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 576),
                        BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 2_000),
                        superCoolant(64_000))
                    .itemOutputs(stack(SLP5Items.superconductorWireUXV, 16))
                    .duration(60 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                alloyBlastSmelterRecipes));
        // SL-V02 Magnetar-Reinforced Borosilicate Glass x4 (Alloy Blast Smelter, UXV): the UMV tier's glass
        // (BartWorks' Transcendentally Reinforced Borosilicate Glass) + 144 L Magnetar Matter, 36 L a block.
        BTSRecipeGuard.recipe(
            "SL-V02",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(umvGlass(4))
                    .fluidInputs(BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 144))
                    .itemOutputs(SLBlocks.glass(BlockSLGlass.MAGNETAR_UXV, 4))
                    .duration(20 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                alloyBlastSmelterRecipes));
        // SL-V03 / V04 UXV 4,096 A Laser Target / Source Hatch (Assembly Line, UXV). Card: the UXV Energy / Dynamo
        // Hatch + 16 UXV Sensors / Emitters; those take Universium in RC-2, so two UMV hatches and 32 UMV sensors /
        // emitters (B-205). Magnetar Matter 2,304 L + Agent 32,000 L. The pack's own recipe stays beside it (it needs
        // UXV parts, so it is the post-T10 route).
        BTSRecipeGuard.recipe(
            "SL-V03",
            () -> assemblyLine(
                item(ItemList.Hatch_Energy_UMV, 1),
                tectech(CustomItemList.eM_energyTunnel3_UXV, 1),
                1_800 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    item(ItemList.Hatch_Energy_UMV, 2),
                    item(ItemList.Sensor_UMV, 32),
                    BTSRecipeGuard.anyCircuit(Materials.UXV, 8),
                    stack(SLP5Items.superconductorWireUXV, 16)),
                BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 2_304),
                BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 32_000)));
        BTSRecipeGuard.recipe(
            "SL-V04",
            () -> assemblyLine(
                item(ItemList.Hatch_Dynamo_UMV, 1),
                tectech(CustomItemList.eM_dynamoTunnel3_UXV, 1),
                1_800 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    item(ItemList.Hatch_Dynamo_UMV, 2),
                    item(ItemList.Emitter_UMV, 32),
                    BTSRecipeGuard.anyCircuit(Materials.UXV, 8),
                    stack(SLP5Items.superconductorWireUXV, 16)),
                BTSRecipeGuard.fluid(SLFluids.magnetarMatter, 2_304),
                BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 32_000)));
    }

    // ================================================================ MAX materials (§7.2: wire, glass, coil, battery)

    private static void maxMaterials() {
        // SL-M04 MAX Superconductor Wire x16 (Mega / Alloy Blast Smelter, MAX): 2 plates + 576 L = 864 L, 16 UXV
        // Superconductor Wire, 64,000 L Super Coolant. 4 minutes of one Eye T9.
        BTSRecipeGuard.recipe(
            "SL-M04",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        stack(SLP5Items.attractorDarkMatterPlate, 2),
                        stack(SLP5Items.superconductorWireUXV, 16))
                    .fluidInputs(darkMatter(576), superCoolant(64_000))
                    .itemOutputs(stack(SLP5Items.superconductorWireMAX, 16))
                    .duration(60 * SECONDS)
                    .eut(TierEU.RECIPE_MAX),
                alloyBlastSmelterRecipes));
        // SL-M05 Dark Matter Borosilicate Glass x4 (Alloy Blast Smelter, MAX): 4 Magnetar-Reinforced + one plate.
        BTSRecipeGuard.recipe(
            "SL-M05",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        SLBlocks.glass(BlockSLGlass.MAGNETAR_UXV, 4),
                        stack(SLP5Items.attractorDarkMatterPlate, 1))
                    .itemOutputs(SLBlocks.glass(BlockSLGlass.DARK_MATTER_MAX, 4))
                    .duration(20 * SECONDS)
                    .eut(TierEU.RECIPE_MAX),
                alloyBlastSmelterRecipes));
        // SL-M06 Dark Matter Heating Coil x2 (Assembly Line): 2 frame boxes + 4 MAX wire + 2 plates + 72 L = 1,152 L.
        // Research: the frame box. Coils are bought by the hundred, so it is the only MAX part priced per pair.
        BTSRecipeGuard.recipe(
            "SL-M06",
            () -> assemblyLine(
                SLBlocks.casing4(BlockSLCasing4.DARK_MATTER_FRAME, 1),
                SLBlocks.casing4(BlockSLCasing4.DARK_MATTER_COIL, 2),
                60 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    SLBlocks.casing4(BlockSLCasing4.DARK_MATTER_FRAME, 2),
                    stack(SLP5Items.superconductorWireMAX, 4),
                    stack(SLP5Items.attractorDarkMatterPlate, 2),
                    BTSRecipeGuard.anyCircuit(Materials.MAX, 1)),
                darkMatter(1_152 - 2 * 288 - 4 * DM_WIRE - 2 * DM_PLATE)));
        // SL-M07 MAX Battery (2^59 - 1 EU, B-208; Assembly Line): 16 dense plates + 32 MAX wire + a MAX Field Generator
        // +
        // 9,792 L = 36,864 L, 3 h of one Eye T9. Research: the dense plate.
        BTSRecipeGuard.recipe(
            "SL-M07",
            () -> assemblyLine(
                stack(SLP5Items.attractorDarkMatterDensePlate, 1),
                stack(SLP5Items.maxBattery, 1),
                1_800 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    stack(SLP5Items.attractorDarkMatterDensePlate, 16),
                    stack(SLP5Items.superconductorWireMAX, 32),
                    item(ItemList.Field_Generator_MAX, 1),
                    BTSRecipeGuard.anyCircuit(Materials.MAX, 8)),
                darkMatter(36_864 - 16 * DM_DENSE - 32 * DM_WIRE - 4_608),
                superCoolant(64_000)));
    }

    // ===================================================== the eight MAX components + the MAX CoAL casing (§7.2)

    private static void maxComponents() {
        // SL-M08 MAX Machine Casing (Assembler, MAX): 8 plates + 1,152 L = 2,304 L. RC-2's "Casing_MAX" is the old
        // UHV-tier casing; the MAX tier's is Casing_MAXV.
        BTSRecipeGuard.recipe(
            "SL-M08",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(stack(SLP5Items.attractorDarkMatterPlate, 8), GTUtility.getIntegratedCircuit(8))
                    .fluidInputs(darkMatter(2_304 - 8 * DM_PLATE))
                    .itemOutputs(item(ItemList.Casing_MAXV, 1))
                    .duration(50 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                assemblerRecipes));
        // SL-M09 MAX Machine Hull (Assembler): the casing (2,304 L) + 2 UMV superconductor cables + Radox Polymer, GT's
        // hull pattern; no more Dark Matter than its casing.
        BTSRecipeGuard.recipe(
            "SL-M09",
            () -> added(
                GTValues.RA.stdBuilder()
                    .itemInputs(
                        item(ItemList.Casing_MAXV, 1),
                        BTSRecipeGuard.req(OrePrefixes.wireGt04, Materials.SuperconductorUMV, 2))
                    .fluidInputs(BTSRecipeGuard.nn(Materials.RadoxPolymer.getMolten(288L), "molten Radox Polymer"))
                    .itemOutputs(item(ItemList.Hull_MAXV, 1))
                    .duration(50 * SECONDS)
                    .eut(TierEU.RECIPE_UXV),
                assemblerRecipes));

        // SL-M10..M17: the eight MAX components on the Component Assembly Line at its MAX casing tier, 16 a batch.
        // Each: 8 of the UMV part (the card's 4 UXV, B-205), 2 MAX wire, 1 MAX circuit, Dark Matter to the §7.2 total.
        coal("SL-M10", ItemList.Electric_Motor_MAX, ItemList.Electric_Motor_UMV, 2_304);
        coal("SL-M11", ItemList.Electric_Pump_MAX, ItemList.Electric_Pump_UMV, 2_304);
        coal("SL-M12", ItemList.Conveyor_Module_MAX, ItemList.Conveyor_Module_UMV, 2_304);
        coal("SL-M13", ItemList.Electric_Piston_MAX, ItemList.Electric_Piston_UMV, 2_304);
        coal("SL-M14", ItemList.Robot_Arm_MAX, ItemList.Robot_Arm_UMV, 2_304);
        coal("SL-M15", ItemList.Emitter_MAX, ItemList.Emitter_UMV, 3_456);
        coal("SL-M16", ItemList.Sensor_MAX, ItemList.Sensor_UMV, 3_456);
        coal("SL-M17", ItemList.Field_Generator_MAX, ItemList.Field_Generator_UMV, 4_608);

        // SL-M18 Component Assembly Line Casing (MAX) (Assembly Line): 2 UMV casings (the UXV one takes Universium),
        // 2 dense plates + 8 MAX wire + 1,584 L = 4,608 L. Research: the MAX Robot Arm.
        BTSRecipeGuard.recipe(
            "SL-M18",
            () -> assemblyLine(
                item(ItemList.Robot_Arm_MAX, 1),
                gg(ItemRefer.Compassline_Casing_MAX, 1),
                600 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    gg(ItemRefer.Compassline_Casing_UMV, 2),
                    stack(SLP5Items.attractorDarkMatterDensePlate, 2),
                    stack(SLP5Items.superconductorWireMAX, 8),
                    BTSRecipeGuard.anyCircuit(Materials.MAX, 4)),
                darkMatter(4_608 - 2 * DM_DENSE - 8 * DM_WIRE)));
    }

    /**
     * One MAX component on the Component Assembly Line (MAX casing tier), {@link #BATCH} at a time: 8 of
     * {@code umvPart} each, 2 MAX Superconductor Wire, 1 MAX circuit, and Attractor Dark Matter so the part costs
     * {@code dmEach} litres all in.
     */
    private static void coal(String id, ItemList max, ItemList umvPart, int dmEach) {
        BTSRecipeGuard.recipe(id, () -> {
            final List<ItemStack> in = new ArrayList<>();
            for (int left = 8 * BATCH; left > 0; left -= 64) in.add(item(umvPart, Math.min(64, left)));
            in.add(stack(SLP5Items.superconductorWireMAX, 2 * BATCH));
            in.add(circuit(Materials.MAX, BATCH));
            added(
                GTValues.RA.stdBuilder()
                    .itemInputs(in.toArray(new ItemStack[0]))
                    .fluidInputs(darkMatter((dmEach - 2 * DM_WIRE) * BATCH))
                    .itemOutputs(item(max, BATCH))
                    .metadata(COAL_CASING_TIER, COAL_MAX)
                    .duration(20 * MINUTES)
                    .eut(TierEU.RECIPE_MAX),
                GoodGeneratorRecipeMaps.componentAssemblyLineRecipes);
        });
    }

    // =============================================================================== MAX power (§7.2)

    private static void maxPower() {
        // SL-M19 MAX Energy Hatch / SL-M20 MAX Dynamo Hatch (Assembly Line): the hull (2,304 L) + 16 MAX wire (864 L)
        // + 6,048 L = 9,216 L, 45 min of one Eye T9 each. Research: the MAX casing / hull.
        maxHatch(
            "SL-M19",
            item(ItemList.Casing_MAXV, 1),
            () -> BTSMaxHatches.energyHatchMAX,
            16,
            0,
            ItemList.Electric_Pump_UMV,
            ItemList.Sensor_UMV,
            9_216);
        maxHatch(
            "SL-M20",
            item(ItemList.Hull_MAXV, 1),
            () -> BTSMaxHatches.dynamoHatchMAX,
            16,
            0,
            ItemList.Electric_Pump_UMV,
            ItemList.Emitter_UMV,
            9_216);
        // SL-M21 MAX Wireless Energy Hatch (Assembly Line, 9,216 L). RC-2 has no MAX wireless dynamo hatch to price.
        // Research: the MAX Field Generator.
        maxHatch(
            "SL-M21",
            item(ItemList.Field_Generator_MAX, 1),
            () -> ItemList.Wireless_Hatch_Energy_MAX.hasBeenSet() ? ItemList.Wireless_Hatch_Energy_MAX.get(1) : null,
            16,
            0,
            ItemList.Sensor_UMV,
            ItemList.Emitter_UMV,
            9_216);
        // SL-M22 / M23 MAX 4,096 A Laser Target / Source Hatch (Assembly Line): the hull + 32 MAX wire + 14,400 L =
        // 18,432 L, 90 min of one Eye T9 each. Research: the MAX Sensor / Emitter.
        maxHatch(
            "SL-M22",
            item(ItemList.Sensor_MAX, 1),
            () -> BTSMaxHatches.laserTargetMAX,
            32,
            8,
            ItemList.Sensor_UMV,
            ItemList.Field_Generator_UMV,
            18_432);
        maxHatch(
            "SL-M23",
            item(ItemList.Emitter_MAX, 1),
            () -> BTSMaxHatches.laserSourceMAX,
            32,
            8,
            ItemList.Emitter_UMV,
            ItemList.Field_Generator_UMV,
            18_432);
    }

    /** A supplier of a machine stack that may be absent (another unit's, or not in this pack). */
    private interface Machine {

        ItemStack get();
    }

    /**
     * One MAX hatch on the Assembly Line: a MAX hull, {@code wires} MAX Superconductor Wire, 4 of {@code partA}, 4
     * (+{@code extraB}) of {@code partB}, 2 + wires / 8 MAX circuits, Attractor Dark Matter to {@code dmTotal} all in
     * and Super Coolant 16,000 L.
     */
    private static void maxHatch(String id, ItemStack research, Machine output, int wires, int extraB, ItemList partA,
        ItemList partB, int dmTotal) {
        BTSRecipeGuard.recipe(id, () -> {
            final ItemStack out = output.get();
            if (out == null) {
                throw new IllegalStateException("its output machine is not registered in this pack");
            }
            assemblyLine(
                research,
                out,
                1_200 * SECONDS,
                TierEU.RECIPE_UXV,
                BTSRecipeGuard.alInputs(
                    item(ItemList.Hull_MAXV, 1),
                    stack(SLP5Items.superconductorWireMAX, wires),
                    item(partA, 4),
                    item(partB, 4 + extraB),
                    BTSRecipeGuard.anyCircuit(Materials.MAX, 2 + wires / 8)),
                darkMatter(dmTotal - DM_HULL - wires * DM_WIRE),
                superCoolant(16_000));
        });
    }

    // =========================================================================================== helpers

    /** One Condensate Assembler slot: item stacks (split at 64) at one nanite tier. */
    private static final class Slot {

        final List<ItemStack> stacks = new ArrayList<>();
        final NaniteTier tier;

        Slot(ItemStack part, NaniteTier tier) {
            this.tier = tier;
            BTSRecipeGuard.nn(part, "an item input");
            for (int left = part.stackSize; left > 0; left -= 64) {
                stacks.add(GTUtility.copyAmount(Math.min(64, left), part));
            }
        }
    }

    private static Slot s(ItemStack part, NaniteTier tier) {
        return new Slot(part, tier);
    }

    private static FluidStack[] cond(FluidStack... condensates) {
        return condensates;
    }

    /** {@code litres} of one of GT's own condensates. */
    private static FluidStack gt(CondensateType type, int litres) {
        return BTSRecipeGuard.nn(type.getEntangled(litres), "entangled " + type.name());
    }

    /**
     * {@code litres} of one of our condensates, which its Condensate Generator line (bec / reality / quasar) has made
     * a recipe for; checked against GT's half-unit rule ({@code unit} 144 or 1,000).
     */
    private static FluidStack ours(Fluid condensate, int litres, int unit) {
        final Fluid fluid = BTSRecipeGuard.nn(condensate, "an addon condensate");
        boolean made = false;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mFluidOutputs == null) continue;
            for (FluidStack out : recipe.mFluidOutputs) {
                if (out != null && out.getFluid() == fluid) made = true;
            }
        }
        if (!made) throw new IllegalStateException("no Condensate Generator recipe makes " + fluid.getName());
        if (litres % (unit / 2) != 0) {
            throw new IllegalArgumentException(litres + " L of " + fluid.getName() + " breaks the half-unit rule");
        }
        return new FluidStack(fluid, litres);
    }

    /** A Condensate Assembler recipe: at most 16 item stacks and 4 condensates, nanites at most Black Dwarf. */
    private static void assemble(ItemStack output, int ticks, long eut, FluidStack[] condensates, Slot... slots) {
        final List<ItemStack> items = new ArrayList<>();
        final List<NaniteTier> tiers = new ArrayList<>();
        for (Slot slot : slots) {
            for (ItemStack stack : slot.stacks) {
                items.add(stack);
                tiers.add(slot.tier);
            }
        }
        if (items.size() > 16) throw new IllegalStateException("more than 16 item stacks: " + items.size());
        if (condensates.length > 4) throw new IllegalStateException("more than 4 condensates");
        for (NaniteTier tier : tiers) {
            if (tier.getTier() > N7.getTier()) throw new IllegalStateException("nanite tier " + tier + " (H-17)");
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

    /** An Assembly Line recipe, researched from {@code research} on the Scanner (UXV, 8 min). */
    private static void assemblyLine(ItemStack research, ItemStack output, int ticks, long eut, Object[] items,
        FluidStack... fluids) {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.nn(research, "the research item"))
            .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UXV))
            .itemInputs(items)
            .fluidInputs(fluids)
            .itemOutputs(BTSRecipeGuard.nn(output, "the output"))
            .duration(ticks)
            .eut(eut)
            .addTo(AssemblyLine);
    }

    /** {@code builder.addTo(map)}, throwing if GT kept nothing (so the guard counts a skip). */
    private static Collection<GTRecipe> added(GTRecipeBuilder builder, RecipeMap<?> map) {
        final Collection<GTRecipe> recipes = builder.addTo(map);
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalStateException("GT rejected the recipe for " + map.unlocalizedName);
        }
        return recipes;
    }

    private static FluidStack darkMatter(int litres) {
        return BTSRecipeGuard.fluid(SLRealityFluids.darkMatter, litres);
    }

    private static FluidStack superCoolant(int litres) {
        return BTSRecipeGuard.nn(Materials.SuperCoolant.getFluid(litres), "Materials.SuperCoolant fluid");
    }

    private static ItemStack pellet(int grade, int count) {
        return BTSRecipeGuard.nn(SLP5Items.pellet(grade, count), "Gated Pellet grade " + grade);
    }

    private static ItemStack stack(Item item, int count) {
        return BTSRecipeGuard.stack(item, count);
    }

    private static ItemStack item(ItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.hasBeenSet() ? entry.get(count) : null, "ItemList." + entry.name());
    }

    private static ItemStack circuit(Materials tier, int count) {
        return BTSRecipeGuard.req(OrePrefixes.circuit, tier, count);
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.hasBeenSet() ? entry.get(count) : null, "CustomItemList." + entry.name());
    }

    private static ItemStack gg(ItemRefer entry, int count) {
        return BTSRecipeGuard.nn(entry.get(count), "GoodGenerator " + entry);
    }

    /** A GT machine by MTE id, or null if that id is not registered. */
    private static ItemStack mte(int id) {
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] == null) {
            return null;
        }
        return new ItemStack(GregTechAPI.sBlockMachines, 1, id);
    }

    /** BartWorks' UMV-tier main glass (Transcendentally Reinforced Borosilicate Glass), {@code count} blocks. */
    private static ItemStack umvGlass(int count) {
        return new ItemStack(
            BTSRecipeGuard.nn(bartworks.common.loaders.ItemRegistry.bw_realglas, "BartWorks glass"),
            count,
            9);
    }

    /** The P5 recipe ids that wait for another unit's machine. */
    public static List<String> waiting() {
        return new ArrayList<>(WAITING);
    }
}
