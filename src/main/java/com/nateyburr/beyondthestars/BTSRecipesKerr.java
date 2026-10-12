package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.kerr.KerrRecipeMaps;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.recipe.Scanning;

/**
 * SINGULARITY LADDER P2: THE KERR RECIPES, SL-K01..SL-K05 (singularity-ladder-build.md §2.3).
 * <b>Owned by the Kerr unit.</b> Called from one line in {@link BTSRecipes}.
 *
 * <h2>Deviations from the §2.3 table, and why</h2>
 * <ul>
 * <li><b>SL-K01</b> takes 32 Extreme Density Space-Bending Casing in place of 32 Containment Deck
 * Plating: the deck block is retired (FC-1; the BHC's casing plays every Kerr deck-like role).</li>
 * <li><b>Dense plates</b> go through {@link BTSRecipesSingularity#dense}: a missing
 * {@code plateDense} (likely SpaceTime) becomes 9 plates each, logged once (Q-18). Every AL here
 * stays at or under 16 item slots even with the fallback.</li>
 * <li><b>SL-K03</b>'s Huge Neutronium Fluid Pipe falls back to a Large one, then to Neutronium
 * plates x6 each, if this pack never generated it, so the recipe never silently disappears.</li>
 * <li><b>SL-K06 is retired</b> (the ID stays reserved). It was a Kerr-only "Ergosphere Extraction"
 * map that melted exotic metals; I ruled (Q-9/Q-22) that the extractor module runs GT's item
 * extractor recipes only, so the map and its recipes are gone.</li>
 * </ul>
 * The Kerr's modules run GT's own compressor and extractor maps, so it adds no recipes there.
 */
public final class BTSRecipesKerr {

    private BTSRecipesKerr() {}

    public static void register() {
        controllerAndModules();
        densityUpgrade();
        densify();
    }

    // ============================================================== controller + module cores (UIV)

    private static void controllerAndModules() {
        // SL-K01 Kerr Ergosphere Engine (controller). Research: Black Hole Seed.
        BTSRecipeGuard.recipe(
            "SL-K01",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, nn(ItemList.Black_Hole_Opener.get(1L), "ItemList.Black_Hole_Opener"))
                .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    items(
                        BTSRecipesSingularity.dense(Materials.SpaceTime, 32),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 32L),
                        nn(ItemList.Field_Generator_UIV.get(16L), "ItemList.Field_Generator_UIV"),
                        nn(ItemList.Emitter_UIV.get(16L), "ItemList.Emitter_UIV"),
                        nn(ItemList.Sensor_UIV.get(16L), "ItemList.Sensor_UIV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 32L),
                        BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 8),
                        SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 16),
                        nn(ItemList.Black_Hole_Opener.get(8L), "ItemList.Black_Hole_Opener"),
                        BTSRecipeGuard.req(OrePrefixes.screw, Materials.TranscendentMetal, 64L),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 16),
                        casingStack(Casings.ExtremeDensitySpaceBendingCasing, 32)))
                .fluidInputs(
                    molten(Materials.SpaceTime, 294_912),
                    molten(Materials.TranscendentMetal, 147_456),
                    superCoolant(1_000_000))
                .itemOutputs(nn(BTSMachines.kerrEngine, "BTSMachines.kerrEngine").copy())
                .duration(18_000)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-K02 Kerr Compressor Module (module core block). Research: the Kerr controller.
        BTSRecipeGuard.recipe(
            "SL-K02",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, kerr())
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    items(
                        nn(ItemList.Electric_Piston_UIV.get(32L), "ItemList.Electric_Piston_UIV"),
                        nn(ItemList.Field_Generator_UIV.get(8L), "ItemList.Field_Generator_UIV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 16L),
                        BTSRecipesSingularity.dense(Materials.Neutronium, 32),
                        nn(ItemList.Black_Hole_Opener.get(4L), "ItemList.Black_Hole_Opener"),
                        BTSRecipeGuard.req(OrePrefixes.gearGt, Materials.SpaceTime, 16L)))
                .fluidInputs(molten(Materials.SpaceTime, 73_728), superCoolant(500_000))
                .itemOutputs(SLBlocks.casing2(BlockSLCasing2.COMPRESSOR_MODULE, 1))
                .duration(12_000)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-K03 Kerr Extractor Module (module core block). Research: an Extraction casing (one research
        // item per recipe: the scanner writes only the FIRST recipe sharing one, and SL-K02 has kerr()).
        BTSRecipeGuard.recipe(
            "SL-K03",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.EXTRACTION, 1))
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    items(
                        nn(ItemList.Electric_Pump_UIV.get(32L), "ItemList.Electric_Pump_UIV"),
                        nn(ItemList.Field_Generator_UIV.get(8L), "ItemList.Field_Generator_UIV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 16L),
                        BTSRecipesSingularity.dense(Materials.Neutronium, 32),
                        SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 4),
                        neutroniumPipes(16)))
                .fluidInputs(molten(Materials.SpaceTime, 73_728), superCoolant(500_000))
                .itemOutputs(SLBlocks.casing2(BlockSLCasing2.EXTRACTOR_MODULE, 1))
                .duration(12_000)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));
    }

    // ===================================================================== Kerr T2 upgrade (UMV)

    private static void densityUpgrade() {
        // SL-K04 Kerr Density Upgrade. Research: a Bastion casing (unique research item, see SL-K03).
        // RSPM is Raw Star Matter (Q-1); Kerr T2 comes after Eye T1, so the Eye's own Sol output pays for it.
        BTSRecipeGuard.recipe(
            "SL-K04",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.BASTION, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.BASTION, 64),
                        SLBlocks.casing(BlockSLCasing.BASTION, 64),
                        BTSRecipesSingularity.dense(Materials.Infinity, 64),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 32L),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 32),
                        nn(ItemList.Black_Hole_Stabilizer.get(1L), "ItemList.Black_Hole_Stabilizer"),
                        SLBlocks.casing(BlockSLCasing.INJECTOR_MK3, 16)))
                .fluidInputs(
                    nn(Materials.RawStarMatter.getFluid(4_000_000L), "Materials.RawStarMatter fluid"),
                    molten(Materials.SpaceTime, 589_824),
                    nn(Materials.ExcitedDTEC.getFluid(128_000L), "Materials.ExcitedDTEC fluid"))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.kerrDensityUpgrade, 1))
                .duration(36_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ============================================================ DENSIFY (Kerr T2 + compressor)

    private static void densify() {
        // SL-K05 Hyperdense Stellar Mixture: Eye T3's raw plasma, finished in a 128+ SM hole.
        BTSRecipeGuard.recipe(
            "SL-K05",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.bolt, Materials.Neutronium, 64L))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.rawHyperdensePlasma, 10_000),
                    molten(Materials.SpaceTime, 288))
                .fluidOutputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 1_000))
                .duration(400)
                .eut(TierEU.RECIPE_UMV)
                .addTo(KerrRecipeMaps.DENSITY));
    }

    // ================================================================================ helpers

    private static ItemStack kerr() {
        return nn(BTSMachines.kerrEngine, "BTSMachines.kerrEngine").copy();
    }

    /** Huge Neutronium Fluid Pipe, else Large, else 6 Neutronium plates per pipe (SL-K03). */
    private static ItemStack[] neutroniumPipes(int count) {
        final ItemStack huge = GTOreDictUnificator.get(OrePrefixes.pipeHuge, Materials.Neutronium, count);
        if (huge != null) return new ItemStack[] { huge };
        final ItemStack large = GTOreDictUnificator.get(OrePrefixes.pipeLarge, Materials.Neutronium, count);
        if (large != null) return new ItemStack[] { large };
        final List<ItemStack> plates = new ArrayList<>();
        int left = count * 6;
        while (left > 0) {
            final int n = Math.min(64, left);
            plates.add(BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, n));
            left -= n;
        }
        return plates.toArray(new ItemStack[0]);
    }

    /** Flattens ItemStacks and ItemStack[]s into one input array. */
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

    private static FluidStack superCoolant(int litres) {
        return nn(Materials.SuperCoolant.getFluid(litres), "Materials.SuperCoolant fluid");
    }

    private static ItemStack casingStack(Casings casing, int count) {
        final Block block = nn(casing.getBlock(), "Casings." + casing.name());
        return new ItemStack(block, count, casing.getBlockMeta());
    }
}
