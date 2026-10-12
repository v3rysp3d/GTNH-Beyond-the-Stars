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

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.recipe.Scanning;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER PARTS, SL-P01..SL-P26 (singularity-ladder-build.md §2.2). <b>Owned by the
 * Foundation unit.</b> The Kerr's and the Eye's own recipes live in {@code BTSRecipesKerr} and
 * {@code BTSRecipesEye}.
 *
 * <h2>Deviations from the §2.2 table, and why</h2>
 *
 * <ul>
 * <li><b>Retired: SL-P01 (Containment Deck Plating), SL-P11 (Eye Shell Casing), SL-P12 (Harmonic
 * Lining), SL-P13 (Spatial Glass Channel).</b> I ruled (2026-10-12) that existing Eye of
 * Harmony and Black Hole Compressor blocks play those roles (Contract additions FC-1), so the blocks
 * are not registered. The IDs stay reserved, like SL-P20.</li>
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

        // SL-P04 Superradiant Mirror Segment (component, x8 per Kerr controller)
        BTSRecipeGuard.recipe(
            "SL-P04",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, nn(ItemList.Black_Hole_Opener.get(1L), "ItemList.Black_Hole_Opener"))
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

        // SL-P08 Ergosphere Field Coil
        BTSRecipeGuard.recipe(
            "SL-P08",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUIV, 64L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 8L))
                .fluidInputs(superCoolant(32_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 1))
                .duration(1_200)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P09 Beam Focusing Quadrupole
        BTSRecipeGuard.recipe(
            "SL-P09",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                    nn(ItemList.Sensor_UIV.get(1L), "ItemList.Sensor_UIV"),
                    casingStack(Casings.ParticleBeamGuidancePipeCasing, 2),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 8L))
                .fluidInputs(superCoolant(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 1))
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

        // SL-P14 Ergosphere Extraction Casing x2
        BTSRecipeGuard.recipe(
            "SL-P14",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 2),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L),
                        nn(ItemList.Electric_Pump_UIV.get(1L), "ItemList.Electric_Pump_UIV")))
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
        // SL-P15 Beam Band Casing x4 (Eye T2)
        BTSRecipeGuard.recipe(
            "SL-P15",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 1, "EOH_Reinforced_Spatial_Casing"),
                        SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 2),
                        nn(ItemList.Emitter_UMV.get(2L), "ItemList.Emitter_UMV"),
                        dense(Materials.SpaceTime, 2)))
                .fluidInputs(molten(Materials.SpaceTime, 9_216))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.BEAM_BAND, 4))
                .duration(1_800)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P16 Jet Crescent Casing x4 (Eye T3). Raw Star Matter is the fluid (the gate);
        // the card's 4,608 L of SpaceTime arrives as 32 ingots.
        BTSRecipeGuard.recipe(
            "SL-P16",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 1, "EOH_Reinforced_Temporal_Casing"),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                        dense(Materials.Infinity, 8),
                        nn(ItemList.Emitter_UMV.get(1L), "ItemList.Emitter_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.SpaceTime, 32L)))
                .fluidInputs(rawStarMatter(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 4))
                .duration(2_400)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P17 Galactic Shell Plating x2 (Eye T4). HSM is the fluid (the T4 gate); the card's
        // 1,152 L of Transcendent Metal arrives as 8 ingots.
        BTSRecipeGuard.recipe(
            "SL-P17",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Infinity, 4),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 1L),
                        BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 4L),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.TranscendentMetal, 8L)))
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 1_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 2))
                .duration(800)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P21 Stabiliser Pylon (component, x6 per Eye controller). TecTech's EOH casings stand in
        // for the retired Eye Shell / Harmonic Lining / Deck blocks (FC-1).
        BTSRecipeGuard.recipe(
            "SL-P21",
            () -> GTValues.RA.stdBuilder()
                .metadata(
                    RESEARCH_ITEM,
                    tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 1, "EOH_Reinforced_Spatial_Casing"))
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
                .fluidInputs(molten(Materials.SpaceTime, 147_456), superCoolant(500_000))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1))
                .duration(18_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
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
