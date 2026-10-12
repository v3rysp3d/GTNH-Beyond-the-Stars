package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.recipe.Scanning;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER P3: THE EYE'S CRAFTING RECIPES, SL-E01 and SL-E02 (singularity-ladder-build.md
 * §2.4), plus D-49's transition cores and sensor hatch, SL-E03..SL-E07 (d49-build-plan.md §10.2).
 * <b>Owned by the Eye unit.</b> Called from one line in {@link BTSRecipes}.
 * <p>
 * The Eye's PROCESS recipes are not here: they are long-sized and live in
 * {@code machines/eye/EyeRecipeRegistry}, built at load-complete.
 *
 * <h2>Deviations from the §2.4 table</h2>
 * <ul>
 * <li><b>SL-E01</b> follows FC-1: the retired Eye Shell ×64 and Containment Deck ×64 lines are dropped
 * (the card already has Reinforced Spatial ×64 and Reinforced Temporal ×64), and Harmonic Lining ×64
 * becomes TecTech's Infinite SpaceTime Energy Boundary Casing ×64. The three field generators are
 * Tier0 (Crude), not the card's Tier8: under the 1:1 compression gate (Q-20) Crude is the T1 Eye's own
 * gate, and Tier8 would force every generator research step before T1 exists (Q-32).</li>
 * <li><b>Research items</b> are addon-only stacks (SL-E01: the Stabiliser Pylon; SL-E02: the Eye
 * controller). TecTech's own EOH research recipes already use its EOH casings, and the scanner writes
 * only the first recipe sharing a research item.</li>
 * <li><b>Dense SpaceTime</b> (×64, ×32) goes through {@link #denseSpaceTime}: the dense plate if GT
 * has one, else the same mass as SpaceTime blocks (one slot), else {@code BTSRecipesSingularity.dense}'s
 * 9-plates fallback (Q-18). Blocks keep both assembly lines within 16 item slots.</li>
 * </ul>
 */
public final class BTSRecipesEye {

    private BTSRecipesEye() {}

    public static void register() {
        controller();
        galacticCore();
        lifecycleCores();
        sensorHatch();
    }

    private static void controller() {
        // SL-E01 Eye of Harmony (controller, MTE base+15). Research: the Stabiliser Pylon - addon-only,
        // because TecTech's own EOH research recipes already use the Reinforced Spatial Structure Casing
        // and the scanner writes only the FIRST recipe sharing a research item.
        BTSRecipeGuard.recipe(
            "SL-E01",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 64),
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 64),
                        // Tier0 (Crude): the T1 Eye's own compression gate (1:1, Q-20), as TecTech's EOH
                        // controller recipe uses. Tier8 would force all 24 generator research steps first.
                        tectech(CustomItemList.SpacetimeCompressionFieldGeneratorTier0, 1),
                        tectech(CustomItemList.TimeAccelerationFieldGeneratorTier0, 1),
                        tectech(CustomItemList.StabilisationFieldGeneratorTier0, 1),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 64L),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        denseSpaceTime(64),
                        tectech(CustomItemList.EOH_Infinite_Energy_Casing, 64),
                        BTSRecipeGuard.stack(SLItems.stabiliserPylon, 6)))
                // 294,912 L SpaceTime, not the card's 1,179,648 L: the controller chain was 33k SpaceTime
                // ingots against TecTech's controller at about 2k ingots of Time and Space (cost audit #12).
                .fluidInputs(
                    molten(Materials.SpaceTime, 294_912),
                    nn(Materials.ExcitedDTEC.getFluid(256_000L), "Materials.ExcitedDTEC fluid"),
                    molten(Materials.Infinity, 589_824))
                .itemOutputs(nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony").copy())
                .duration(36_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    private static void galacticCore() {
        // SL-E02 Galactic Upgrade Core (unlocks Eye T4). Research: the Eye controller. HSM is the gate:
        // 32,000 L (about 6 Arcturus runs), not the card's 256,000 L (51 runs, cost audit #11). A crafting
        // recipe only; under D-49 the core goes into escrow for the T3 -> T4 Core Collapse (d49-build-plan.md §8).
        BTSRecipeGuard.recipe(
            "SL-E02",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony").copy())
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 64L),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                        denseSpaceTime(32),
                        nn(ItemList.Emitter_UMV.get(16L), "ItemList.Emitter_UMV")))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 32_000),
                    molten(Materials.SpaceTime, 147_456),
                    molten(Materials.TranscendentMetal, 147_456))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.galacticUpgradeCore, 1))
                .duration(48_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ================================================================================ D-49 cores

    /**
     * SL-E03..SL-E06: the escrowed transition cores (d49-build-plan.md §8, §10.2). Assembly lines in the
     * SL-E02 pattern; each one's gating fluid is the product of the tier it leaves (RSPM, ESPM, WDM, BDM),
     * so a core can never be made before its tier runs. GT's Scanner CONSUMES the research item, so
     * the research items are cheap and unique to this mod (D-49 review: E04-E06 used to research from
     * the previous core, which made the player build each of those cores twice): E03 the Stellar
     * Ignition Charge, E04 Star Chart: Arcturus (SL-P24, made from ESPM), E05 Star Chart: Sirius B
     * (SL-P35, from WDM) and E06 Star Chart: SGR 1806-20 (SL-P37, from BDM) - each the chart of the
     * star the core's transition makes, made from the product of the tier it leaves. None is a research
     * item anywhere else, so the scanner never writes the wrong recipe.
     */
    private static void lifecycleCores() {
        // SL-E03 Stellar Mass Core (T1 -> T2, Mass Infusion). RSPM 256,000 L = about 5 min of Sol.
        BTSRecipeGuard.recipe(
            "SL-E03",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 1))
                .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.BEAM_BAND, 32),
                        nn(ItemList.Field_Generator_UMV.get(16L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 32L),
                        denseSpaceTime(16),
                        nn(ItemList.Emitter_UMV.get(8L), "ItemList.Emitter_UMV"),
                        BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 16)))
                .fluidInputs(
                    nn(Materials.RawStarMatter.getFluid(256_000L), "Materials.RawStarMatter fluid"),
                    molten(Materials.SpaceTime, 36_864),
                    molten(Materials.Infinity, 73_728))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.stellarMassCore, 1))
                .duration(24_000)
                .eut(TierEU.RECIPE_UMV)
                .addTo(AssemblyLine));

        // SL-E04 Shell-Burning Core (T2 -> T3, Shell Ignition). ESPM 128,000 L = about 51 min of Sirius A.
        BTSRecipeGuard.recipe(
            "SL-E04",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartArcturus, 1))
                .metadata(SCANNING, new Scanning(6 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 32),
                        nn(ItemList.Field_Generator_UMV.get(24L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 48L),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 16),
                        denseSpaceTime(24),
                        nn(ItemList.Emitter_UMV.get(12L), "ItemList.Emitter_UMV")))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 128_000),
                    molten(Materials.SpaceTime, 73_728),
                    molten(Materials.TranscendentMetal, 73_728))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.shellBurningCore, 1))
                .duration(36_000)
                .eut(TierEU.RECIPE_UMV * 2)
                .addTo(AssemblyLine));

        // SL-E05 Subspace Cooling Core (T4 -> T5, First Cooling). WDM 147,456 L = the T4 milestone's own
        // figure; DSSF is the T5 heat sink's fluid.
        BTSRecipeGuard.recipe(
            "SL-E05",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UXV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                        nn(ItemList.Field_Generator_UXV.get(16L), "ItemList.Field_Generator_UXV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UXV, 16L),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                        denseSpaceTime(48),
                        nn(ItemList.Sensor_UXV.get(8L), "ItemList.Sensor_UXV")))
                .fluidInputs(
                    molten(Materials.WhiteDwarfMatter, 147_456),
                    nn(
                        Materials.DimensionallyShiftedSuperfluid.getFluid(256_000L),
                        "Materials.DimensionallyShiftedSuperfluid fluid"),
                    molten(Materials.SpaceTime, 147_456))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.subspaceCoolingCore, 1))
                .duration(48_000)
                .eut(TierEU.RECIPE_UXV)
                .addTo(AssemblyLine));

        // SL-E06 Magnetar Seed Core (T5 -> T6, Magnetar Merger). BDM 36,864 L = 8 cooling batches at p = 1.
        BTSRecipeGuard.recipe(
            "SL-E06",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartMagnetar, 1))
                .metadata(SCANNING, new Scanning(10 * MINUTES, TierEU.RECIPE_UXV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                        nn(ItemList.Field_Generator_UXV.get(32L), "ItemList.Field_Generator_UXV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UXV, 32L),
                        SLBlocks.casing(BlockSLCasing.BASTION, 32),
                        denseSpaceTime(64),
                        nn(ItemList.Emitter_UXV.get(16L), "ItemList.Emitter_UXV")))
                .fluidInputs(
                    molten(Materials.BlackDwarfMatter, 36_864),
                    BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 64_000),
                    molten(Materials.SpaceTime, 294_912))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.magnetarSeedCore, 1))
                .duration(60_000)
                .eut(TierEU.RECIPE_UXV * 2)
                .addTo(AssemblyLine));
    }

    /** SL-E07 Stellar Field Sensor Hatch (MTE 28116): a UMV hull with two sensors. */
    private static void sensorHatch() {
        BTSRecipeGuard.recipe(
            "SL-E07",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Hull_UMV.get(1L), "ItemList.Hull_UMV"),
                    nn(ItemList.Sensor_UMV.get(2L), "ItemList.Sensor_UMV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.SpaceTime, 4L))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(nn(BTSMachines.stellarFieldSensor, "BTSMachines.stellarFieldSensor").copy())
                .duration(400)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ================================================================================ helpers

    /**
     * {@code count} dense SpaceTime plates; if GT never generated them, {@code count} SpaceTime
     * blocks (9 ingots each, the same mass in one slot); else the shared 9-plates fallback.
     */
    private static ItemStack[] denseSpaceTime(int count) {
        if (GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.SpaceTime, 1L) == null) {
            final ItemStack block = GTOreDictUnificator.get(OrePrefixes.block, Materials.SpaceTime, count);
            if (block != null) return new ItemStack[] { block };
        }
        return BTSRecipesSingularity.dense(Materials.SpaceTime, count);
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        if (!entry.hasBeenSet()) return nn(null, "CustomItemList." + entry.name());
        return nn(entry.get(count), "CustomItemList." + entry.name());
    }

    /** Flattens ItemStacks and ItemStack[]s into one input array (at most 16 slots). */
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
        if (out.size() > 16) {
            throw new IllegalStateException("assembly line takes at most 16 item inputs, got " + out.size());
        }
        return out.toArray(new ItemStack[0]);
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static FluidStack molten(Materials material, int litres) {
        return nn(material.getMolten(litres), "molten " + material);
    }
}
