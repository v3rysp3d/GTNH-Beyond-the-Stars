package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;

import goodgenerator.loader.Loaders;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import tectech.recipe.TTRecipeAdder;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER PARTS, SL-P01..SL-P32 (singularity-ladder-build.md §2.2; SL-P27..P32 come
 * from the cost audit, docs/singularity-ladder-costs.md). <b>Owned by the
 * Foundation unit.</b> The Kerr's and the Eye's own recipes live in {@code BTSRecipesKerr} and
 * {@code BTSRecipesEye}.
 *
 * <h2>Deviations from the §2.2 table, and why</h2>
 *
 * <ul>
 * <li><b>Retired: SL-P01 (Containment Deck Plating), SL-P11 (Eye Shell Casing), SL-P13 (Spatial
 * Glass Channel).</b> I ruled (2026-10-12) that existing Eye of Harmony and Black Hole
 * Compressor blocks play those roles (Contract additions FC-1), so the blocks are not registered.
 * The IDs stay reserved, like SL-P20.</li>
 * <li><b>SL-P12 (Harmonic Lining) is back</b> as a Kerr-only block (round-4 ruling: one new white
 * Kerr lining block, slCasing2:2), x4 per craft with one fluid instead of the card's condensates.</li>
 * <li><b>SL-P21</b> (Stabiliser Pylon) takes TecTech's EOH casings in place of those three blocks.</li>
 * <li><b>One fluid per Assembler recipe.</b> GT's {@code assemblerRecipes} is {@code maxIO(9, 1, 1, 0)}:
 * a single-block Assembler has one fluid slot. Four cards had two. SL-P03 moves its SpaceTime into
 * two SpaceTime foils; SL-P07 drops the Lubricant and keeps the SpaceTime; SL-P16 keeps the Raw Star
 * Matter (the gate) and takes its 4,608 L of SpaceTime as 32 ingots; SL-P17 keeps the Hyperdense
 * Stellar Mixture (the T4 gate) and takes its 1,152 L of Transcendent Metal as 8 ingots.</li>
 * <li><b>Dense plates</b> go through {@link #dense(Materials, int)}: if GT never generated a
 * {@code plateDense} for a material (likely for SpaceTime), the recipe takes 9 plates per dense
 * plate instead, and the boot log says so once per material (spec §2.2).</li>
 * </ul>
 * Every recipe runs under {@link BTSRecipeGuard#recipe(String, Runnable)}; every foreign component
 * goes through {@code req}, {@code fluid} or {@code nn}.
 */
public final class BTSRecipesSingularity {

    private BTSRecipesSingularity() {}

    public static void register() {
        kerrParts();
        eyeParts();
        starCharts();
        if (Config.replaceTecTechEyeOfHarmony) {
            tecTechEyeBlocks();
            compressionGates();
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] replaceTecTechEyeOfHarmony=false: SL-P27..P32 (batch TecTech Eye casings and the "
                    + "compression-generator gate recipes) are not registered; TecTech's own recipes apply.");
        }
    }

    // ================================================================ Kerr structure (UIV / UMV)

    private static void kerrParts() {
        // SL-P02 Mirror Backing Casing x2
        BTSRecipeGuard.recipe(
            "SL-P02",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 6L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L))
                .fluidInputs(molten(Materials.Infinity, 1_152))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.MIRROR_BACKING, 2))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P03 Superradiant Mirror Casing x2 (SpaceTime as foil: one fluid slot)
        BTSRecipeGuard.recipe(
            "SL-P03",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 8L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 2L),
                    SLBlocks.casing(BlockSLCasing.MIRROR_BACKING, 1))
                .fluidInputs(molten(Materials.Infinity, 576))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.MIRROR, 2))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P04 Superradiant Mirror Segment (component, x8 per Kerr controller). Research: a Superradiant
        // Mirror casing - addon-only, because the scanner writes only the FIRST recipe sharing a research
        // item and the Black Hole Seed is SL-K01's (BTSRecipeGuard.checkResearchCollisions).
        BTSRecipeGuard.recipe(
            "SL-P04",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.MIRROR, 1))
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 32L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 64L),
                    nn(ItemList.Emitter_UIV.get(4L), "ItemList.Emitter_UIV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 4L),
                    SLBlocks.casing(BlockSLCasing.MIRROR, 4))
                .fluidInputs(molten(Materials.Infinity, 18_432), molten(Materials.SpaceTime, 2_304))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 1))
                .duration(4_800)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-P05 Bolt Injector Rail Mk I
        BTSRecipeGuard.recipe(
            "SL-P05",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Robot_Arm_UIV.get(1L), "ItemList.Robot_Arm_UIV"),
                    nn(ItemList.Electric_Piston_UIV.get(1L), "ItemList.Electric_Piston_UIV"),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L),
                    BTSRecipeGuard.req(OrePrefixes.gearGt, Materials.SpaceTime, 2L))
                .fluidInputs(nn(Materials.Lubricant.getFluid(1_000L), "Materials.Lubricant fluid"))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1))
                .duration(300)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P06 Bolt Injector Rail Mk II
        BTSRecipeGuard.recipe(
            "SL-P06",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1),
                    nn(ItemList.Conveyor_Module_UIV.get(2L), "ItemList.Conveyor_Module_UIV"),
                    nn(ItemList.Robot_Arm_UIV.get(1L), "ItemList.Robot_Arm_UIV"))
                .fluidInputs(nn(Materials.Lubricant.getFluid(2_000L), "Materials.Lubricant fluid"))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK2, 1))
                .duration(400)
                .eut(TierEU.RECIPE_UIV * 4)
                .addTo(assemblerRecipes));

        // SL-P07 Bolt Injector Rail Mk III (Lubricant dropped: one fluid slot)
        BTSRecipeGuard.recipe(
            "SL-P07",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.INJECTOR_MK2, 1),
                    nn(ItemList.Robot_Arm_UMV.get(1L), "ItemList.Robot_Arm_UMV"),
                    nn(ItemList.Electric_Piston_UMV.get(1L), "ItemList.Electric_Piston_UMV"),
                    nn(ItemList.Conveyor_Module_UMV.get(1L), "ItemList.Conveyor_Module_UMV"))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK3, 1))
                .duration(600)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P08 Ergosphere Field Coil. A Kerr T1 holds 1,258 coils (842 placed, 416 inside quadrupoles),
        // so 16 wires and 8,000 L, not the card's 64 and 32,000 L (cost audit #6).
        BTSRecipeGuard.recipe(
            "SL-P08",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUIV, 16L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 8L))
                .fluidInputs(superCoolant(8_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 1))
                .duration(1_200)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P09 Beam Focusing Quadrupole x4 (the card made 1; 208 per Kerr T1, cost audit #7)
        BTSRecipeGuard.recipe(
            "SL-P09",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                    nn(ItemList.Sensor_UIV.get(1L), "ItemList.Sensor_UIV"),
                    casingStack(Casings.ParticleBeamGuidancePipeCasing, 2),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 8L))
                .fluidInputs(superCoolant(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P10 Beam Dump
        BTSRecipeGuard.recipe(
            "SL-P10",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 8),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.CosmicNeutronium, 16L),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L)))
                .fluidInputs(superCoolant(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.BEAM_DUMP, 1))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P12 Harmonic Lining x4: the Kerr's white inner face (user ruling, round 4: one new Kerr
        // lining block). Thousands per Kerr (1,072 at T1 with both modules, 4,552 at T2), so a batch
        // Assembler recipe of plain UIV-tier metals; it replaces the BHC's Radiation Absorbent Casing in
        // that role, which costs 64,000 L of UU-Matter per block.
        BTSRecipeGuard.recipe(
            "SL-P12",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 6L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 8L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 1L))
                .fluidInputs(molten(Materials.Infinity, 576))
                .itemOutputs(SLBlocks.casing2(BlockSLCasing2.LINING, 4))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P14 Ergosphere Extraction Casing x2
        BTSRecipeGuard.recipe(
            "SL-P14",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 2),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L),
                        // a Piston, not a Pump: the module runs item recipes (cost audit #14, B-17)
                        nn(ItemList.Electric_Piston_UIV.get(1L), "ItemList.Electric_Piston_UIV")))
                .fluidInputs(molten(Materials.SpaceTime, 288))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.EXTRACTION, 2))
                .duration(800)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P18 Bastion Plate x4 (Kerr T2)
        BTSRecipeGuard.recipe(
            "SL-P18",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 6),
                        BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 2L),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 1L)))
                .fluidInputs(molten(Materials.Infinity, 2_304))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.BASTION, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P19 Casimir Plate x4 (Kerr T2)
        BTSRecipeGuard.recipe(
            "SL-P19",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 8L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 16L))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.CASIMIR, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ================================================================ Eye structure (UMV)

    private static void eyeParts() {
        // SL-P15 Beam Band Casing x16 (Eye T2). The band is 2,862 blocks: x4 with 2 quadrupoles cost 715
        // TecTech Spatial casings and 12.9k UIV/UMV circuits (cost audit #8).
        BTSRecipeGuard.recipe(
            "SL-P15",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 1, "EOH_Reinforced_Spatial_Casing"),
                        SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 1),
                        nn(ItemList.Emitter_UMV.get(2L), "ItemList.Emitter_UMV"),
                        dense(Materials.SpaceTime, 4)))
                .fluidInputs(molten(Materials.SpaceTime, 9_216))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.BEAM_BAND, 16))
                .duration(1_800)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P16 Jet Crescent Casing x16 (Eye T3). Raw Star Matter is the fluid (the gate), still 4,000 L
        // per block; the card's 4,608 L of SpaceTime arrives as 32 ingots. x16 with 2 emitters, not x4
        // with 1: the crescent is 2,755 blocks (cost audit #9).
        BTSRecipeGuard.recipe(
            "SL-P16",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 1, "EOH_Reinforced_Temporal_Casing"),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                        dense(Materials.Infinity, 8),
                        nn(ItemList.Emitter_UMV.get(2L), "ItemList.Emitter_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.SpaceTime, 32L)))
                .fluidInputs(rawStarMatter(64_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 16))
                .duration(2_400)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P17 Galactic Shell Plating x16 (Eye T4). HSM is the fluid (the T4 gate); the card's
        // 1,152 L of Transcendent Metal arrives as ingots. x16 on 250 L, not x2 on 1,000 L: the arms are
        // 5,325 blocks, which was 2.66M L HSM = 532 Arcturus runs; now about 83k L (cost audit #10).
        // A crafting recipe only: D-49 keeps HSM as the T4 gate and leaves this recipe alone.
        BTSRecipeGuard.recipe(
            "SL-P17",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Infinity, 8),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 2L),
                        BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 8L),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.TranscendentMetal, 16L)))
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 250))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 16))
                .duration(800)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P21 Stabiliser Pylon (component, x6 per Eye controller). TecTech's EOH casings stand in
        // for the retired Eye Shell / Harmonic Lining / Deck blocks (FC-1). Research: Star Chart: Sol -
        // addon-only, because TecTech's own EOH research recipes already use its EOH casings.
        BTSRecipeGuard.recipe(
            "SL-P21",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartSol, 1))
                .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 32, "EOH_Reinforced_Spatial_Casing"),
                        tectech(CustomItemList.EOH_Infinite_Energy_Casing, 16, "EOH_Infinite_Energy_Casing"),
                        nn(ItemList.Field_Generator_UMV.get(8L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Neutronium, 16L),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 8L),
                        dense(Materials.Neutronium, 16),
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 16, "EOH_Reinforced_Temporal_Casing")))
                // 36,864 L SpaceTime, not the card's 147,456 L (x6 per Eye; cost audit #12)
                .fluidInputs(molten(Materials.SpaceTime, 36_864), superCoolant(500_000))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1))
                .duration(18_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ======================================= TecTech Eye blocks in bulk (only with TecTech's Eye replaced)

    /**
     * SL-P27..P29: batch Assembler recipes for the TecTech Eye blocks our Eye uses in bulk (cost audit
     * #3/#4, B-23). TecTech prices them for its own small Eye (1,430 Spatial/Temporal casings, 36
     * Boundary casings); ours uses 13,872 and 6,426. The blocks and the look stay TecTech's (round-1
     * ruling). Registered only while {@code replaceTecTechEyeOfHarmony} is on, so a pack that keeps
     * TecTech's Eye keeps TecTech's prices for it. TecTech's own assembly lines stay as well.
     */
    private static void tecTechEyeBlocks() {
        // SL-P27 Reinforced Spatial Structure Casing x4: 1 Godforge Singularity Shielding Casing, not 32.
        // Programmed circuit 1 (not consumed) keeps P27 and P28 apart: they share every other input.
        BTSRecipeGuard.recipe(
            "SL-P27",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(
                            CustomItemList.Godforge_SingularityShieldingCasing,
                            1,
                            "Godforge_SingularityShieldingCasing"),
                        dense(Materials.Neutronium, 4),
                        dense(Materials.CosmicNeutronium, 4),
                        dense(Materials.Infinity, 1),
                        GTUtility.getIntegratedCircuit(1)))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 4, "EOH_Reinforced_Spatial_Casing"))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P28 Reinforced Temporal Structure Casing x4: as P27, on programmed circuit 2.
        BTSRecipeGuard.recipe(
            "SL-P28",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(
                            CustomItemList.Godforge_SingularityShieldingCasing,
                            1,
                            "Godforge_SingularityShieldingCasing"),
                        dense(Materials.Neutronium, 4),
                        dense(Materials.CosmicNeutronium, 4),
                        dense(Materials.Infinity, 1),
                        GTUtility.getIntegratedCircuit(2)))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(
                    tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 4, "EOH_Reinforced_Temporal_Casing"))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P29 Infinite SpaceTime Energy Boundary Casing x8: TecTech's needs an LSC, 16 Active
        // Transformers and 4 UMV wireless hatches per block. The SC UMV wire (TecTech's research item for
        // it) and an Emitter UIV keep the energy theme.
        BTSRecipeGuard.recipe(
            "SL-P29",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        BTSRecipeGuard.req(OrePrefixes.wireGt16, Materials.SuperconductorUMV, 1L),
                        dense(Materials.Neutronium, 8),
                        dense(Materials.CosmicNeutronium, 8),
                        nn(ItemList.Emitter_UIV.get(1L), "ItemList.Emitter_UIV")))
                .fluidInputs(molten(Materials.SpaceTime, 1_152))
                .itemOutputs(tectech(CustomItemList.EOH_Infinite_Energy_Casing, 8, "EOH_Infinite_Energy_Casing"))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ============================== Compression gate: generator tier upgrades (only with TecTech's Eye replaced)

    /** TecTech's per-tier generator research cost (x(t+1)), researched from our star charts. */
    private static final int GATE_RESEARCH_COMPUTATION = 48_000_000;

    private static final int GATE_RESEARCH_PER_SECOND = 16_384;

    /** Flat time for an upgrade step (cost audit #15): the old generator has already paid for its tier. */
    private static final int GATE_UPGRADE_TICKS = 80_000;

    /**
     * One row of the gate table in {@link #compressionGates()}: TecTech's irregular per-tier counts
     * (Yotta Fluid Tank Cell count and meta, Cosmic Fabric Manipulators, UXV circuits), the gate fluid
     * and the research chart.
     */
    private static final class GateStep {

        final String id;
        final int tier;
        final CustomItemList from;
        final CustomItemList to;
        final int yottaCount;
        final int yottaMeta;
        final int fabric;
        final int uxv;
        final Supplier<FluidStack> gate;
        final Supplier<ItemStack> research;

        GateStep(String id, int tier, CustomItemList from, CustomItemList to, int yottaCount, int yottaMeta, int fabric,
            int uxv, Supplier<FluidStack> gate, Supplier<ItemStack> research) {
            this.id = id;
            this.tier = tier;
            this.from = from;
            this.to = to;
            this.yottaCount = yottaCount;
            this.yottaMeta = yottaMeta;
            this.fabric = fabric;
            this.uxv = uxv;
            this.gate = gate;
            this.research = research;
        }
    }

    /**
     * SL-P30..P32: Spacetime Compression Field Generator tier t-1 -> tier t, for t = 1..3 (TecTech
     * metas 1-3, the gates of Eye T2/T3/T4 under the 1:1 rule, Q-20). Cost audit #2 and #15, B-22,
     * B-25.
     * <p>
     * TecTech's own recipe for tier t takes 2 / 8 / 32 White Dwarf Matter bolts, and with its Eye
     * removed WDM's only source is our Eye T4: Eye T2 could never be built. This recipe is TecTech's
     * (the same items, fluids and research cost, decoded with javap from
     * {@code ResearchStationAssemblyLine.addEOHRecipes}) with three changes:
     * <ol>
     * <li>the WDM bolts become a 4th fluid, the product of the Eye tier below (the table here);</li>
     * <li>the generator of the tier below replaces the Reinforced Spatial Structure Casing, so an Eye
     * tier step upgrades its 188 generators instead of stranding them, at {@link #GATE_UPGRADE_TICKS}
     * flat (TecTech: 80,000 x (t+1));</li>
     * <li>the research item is the tier's star chart, not the generator below: TecTech's recipe
     * already uses that one, and a research item can belong to one recipe only.</li>
     * </ol>
     * TecTech's recipes stay; they simply cannot run without WDM. The Tier0 (Crude) generator, the
     * Eye T1 gate, keeps TecTech's own recipe (Shirabon bolts, no WDM).
     */
    private static void compressionGates() {
        final GateStep[] table = {
            new GateStep(
                "SL-P30",
                1,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier0,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier1,
                2,
                6,
                2,
                1,
                () -> rawStarMatter(16_000),
                () -> BTSRecipeGuard.stack(SLItems.starChartSiriusA, 1)),
            new GateStep(
                "SL-P31",
                2,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier1,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier2,
                3,
                6,
                3,
                1,
                () -> BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 4_000),
                () -> BTSRecipeGuard.stack(SLItems.starChartArcturus, 1)),
            new GateStep(
                "SL-P32",
                3,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier2,
                CustomItemList.SpacetimeCompressionFieldGeneratorTier3,
                1,
                7,
                1,
                2,
                () -> BTSRecipeGuard.fluid(SLFluids.rawHyperdensePlasma, 2_000),
                () -> BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1)) };
        for (GateStep step : table) {
            BTSRecipeGuard.recipe(step.id, () -> compressionGate(step));
        }
    }

    private static void compressionGate(GateStep g) {
        final int t = g.tier;
        final Object[] inputs = { tectech(g.from, 1, g.from.name()),
            new ItemStack(nn(Loaders.yottaFluidTankCell, "Loaders.yottaFluidTankCell"), g.yottaCount, g.yottaMeta),
            nn(ItemList.Quantum_Tank_IV.get(4L * (t + 1)), "ItemList.Quantum_Tank_IV"),
            nn(GTModHandler.getModItem(Mods.AvaritiaAddons.ID, "InfinityChest", t + 1), "AvaritiaAddons InfinityChest"),
            nn(GregtechItemList.CosmicFabricManipulator.get(g.fabric), "GregtechItemList.CosmicFabricManipulator"),
            singularityCell(), singularityCell(), new Object[] { OrePrefixes.circuit.get(Materials.UXV), g.uxv } };
        final FluidStack[] fluids = {
            nn(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(2_880 << t), "MUTATED_LIVING_SOLDER fluid"),
            molten(Materials.Space, 1_440 * (t + 1)), molten(Materials.SpaceTime, 1_440), g.gate.get() };
        final boolean added = TTRecipeAdder.addResearchableAssemblylineRecipe(
            g.research.get(),
            GATE_RESEARCH_COMPUTATION * (t + 1),
            GATE_RESEARCH_PER_SECOND * (t + 1),
            (int) TierEU.RECIPE_MAX,
            8 * (t + 1),
            inputs,
            fluids,
            tectech(g.to, 1, g.to.name()),
            GATE_UPGRADE_TICKS,
            (int) TierEU.RECIPE_UMV);
        if (!added) {
            throw new IllegalStateException("TTRecipeAdder refused " + g.id);
        }
    }

    private static ItemStack singularityCell() {
        return nn(
            GTModHandler.getModItem("appliedenergistics2", "item.ItemExtremeStorageCell.Singularity", 1L),
            "AE2 Singularity storage cell");
    }

    // ================================================================ Star charts (Eye targets)

    private static void starCharts() {
        // Each chart's fluid is the previous tier's product, so a chart cannot be crafted before
        // its tier is reachable (§2.2).

        // SL-P22 Star Chart: Sol (T1)
        BTSRecipeGuard.recipe(
            "SL-P22",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    dataOrb(),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 4L))
                .fluidInputs(molten(Materials.SpaceTime, 144))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSol, 1))
                .duration(200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P23 Star Chart: Sirius A (T2), from Raw Stellar Plasma Mixture
        BTSRecipeGuard.recipe(
            "SL-P23",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dataOrb(),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L),
                        dense(Materials.SpaceTime, 1)))
                .fluidInputs(rawStarMatter(10_000))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusA, 1))
                .duration(200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P24 Star Chart: Arcturus (T3), from Excited Stellar Plasma Mixture
        BTSRecipeGuard.recipe(
            "SL-P24",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    dataOrb(),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 4L))
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 1_000))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartArcturus, 1))
                .duration(200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P25 Star Chart: Sirius B (T4), from Hyperdense Stellar Mixture
        BTSRecipeGuard.recipe(
            "SL-P25",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    dataOrb(),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L),
                    SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 1))
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 1_000))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1))
                .duration(200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P26 Star Chart: Sirius B, Cooled (interim Black Dwarf Matter target, Q-3)
        if (Config.interimBlackDwarfAtEyeT4) {
            BTSRecipeGuard.recipe(
                "SL-P26",
                () -> GTValues.RA.stdBuilder()
                    .itemInputs(
                        BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L))
                    .fluidInputs(superCoolant(64_000))
                    .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusBCooled, 1))
                    .duration(200)
                    .eut(TierEU.RECIPE_UMV)
                    .addTo(assemblerRecipes));
        }
    }

    // ================================================================ helpers

    private static final Set<Materials> DENSE_FALLBACK_LOGGED = new HashSet<>();

    /**
     * {@code count} dense plates of {@code material}, or - if GT never generated a {@code plateDense}
     * for it - {@code 9 x count} plain plates in stacks of at most 64 (spec §2.2). The fallback is
     * logged once per material so the deviation is visible in the boot log. Throws through
     * {@link BTSRecipeGuard#req} if even the plate is missing.
     * <p>
     * Public so the Kerr and Eye recipe files can share the same rule for their dense SpaceTime.
     */
    public static ItemStack[] dense(Materials material, int count) {
        final ItemStack dense = GTOreDictUnificator.get(OrePrefixes.plateDense, material, count);
        if (dense != null) {
            return new ItemStack[] { dense };
        }
        if (DENSE_FALLBACK_LOGGED.add(material)) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: GT has no Dense {} Plate in this pack; the Singularity Ladder recipe takes 9 plates "
                    + "per dense plate instead.",
                BTSRecipeGuard.currentRecipe(),
                material);
        }
        final List<ItemStack> plates = new ArrayList<>();
        int left = count * 9;
        while (left > 0) {
            final int n = Math.min(64, left);
            plates.add(BTSRecipeGuard.req(OrePrefixes.plate, material, n));
            left -= n;
        }
        return plates.toArray(new ItemStack[0]);
    }

    /** Flattens ItemStacks and ItemStack[]s (from {@link #dense}) into one input array. */
    private static ItemStack[] items(Object... parts) {
        final List<ItemStack> out = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemStack) {
                out.add((ItemStack) part);
            } else if (part instanceof ItemStack[]) {
                for (ItemStack stack : (ItemStack[]) part) {
                    out.add(stack);
                }
            } else {
                throw new IllegalArgumentException("not an item input: " + part);
            }
        }
        return out.toArray(new ItemStack[0]);
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

    /** Raw Stellar Plasma Mixture = the pack's Raw Star Matter (Q-1). */
    private static FluidStack rawStarMatter(int litres) {
        return nn(Materials.RawStarMatter.getFluid(litres), "Materials.RawStarMatter fluid");
    }

    private static ItemStack dataOrb() {
        return nn(ItemList.Tool_DataOrb.get(1L), "ItemList.Tool_DataOrb");
    }

    private static ItemStack tectech(CustomItemList entry, int count, String what) {
        if (!entry.hasBeenSet()) {
            return nn(null, "CustomItemList." + what);
        }
        return nn(entry.get(count), "CustomItemList." + what);
    }

    private static ItemStack casingStack(Casings casing, int count) {
        final Block block = nn(casing.getBlock(), "Casings." + casing.name());
        return new ItemStack(block, count, casing.getBlockMeta());
    }
}
