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

import cpw.mods.fml.common.registry.GameRegistry;
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
 * <li><b>SL-K01</b> is re-priced at Black Hole Compressor controller parity for GTNH 2.9.0-RC-2 (B-30):
 * the Kerr controller now stands in for the BHC controller in every recipe that consumes one
 * ({@link BTSBlackHoleSwap}), so the card's price (2,048 SpaceTime ingots, 8 seeds) would have been paid
 * once per Spacetime Compression Field Generator. Its Containment Deck Plating was already 32 (now 1)
 * Extreme Density Space-Bending Casing (FC-1).</li>
 * <li><b>Dense plates</b> go through {@link BTSRecipesSingularity#dense}: a missing
 * {@code plateDense} (likely SpaceTime) becomes 9 plates each, logged once (Q-18). Every AL here
 * stays at or under 16 item slots even with the fallback.</li>
 * <li><b>SL-K03</b>'s Huge Neutronium Fluid Pipe falls back to a Large one, then to Neutronium
 * plates x6 each, if this pack never generated it, so the recipe never silently disappears.</li>
 * <li><b>SL-K04 moved to the Condensate Assembler</b> as SL-B31 ({@link BTSRecipesBEC}, ruling 11b; Q-B1: no
 * research step). Both take 32 Black Hole Seeds in place of the card's Superstable Black Hole Seed, which
 * no recipe in the pack makes (cost audit #1, B-21).</li>
 * <li><b>SL-K03</b> takes Electric Pistons UIV in place of Pumps: the module runs item recipes (#14).</li>
 * <li><b>Kerr redesign:</b> SL-K02 / SL-K03 now make the module CONTROLLERS (the modules are separate
 * machines), same inputs; SL-K07 / SL-K08 turn a legacy core block into its controller.</li>
 * <li><b>SL-K06 is retired</b> (the ID stays reserved). It was a Kerr-only "Ergosphere Extraction"
 * map that melted exotic metals; I ruled (Q-9/Q-22) that the extractor module runs GT's item
 * extractor recipes only, so the map and its recipes are gone.</li>
 * </ul>
 * The Kerr's modules run GT's own compressor and extractor maps, so it adds no recipes there.
 */
public final class BTSRecipesKerr {

    private BTSRecipesKerr() {}

    /**
     * Black Hole Seeds in the Kerr Density Upgrade (SL-B31, and its fallback SL-K04), in place of the
     * uncraftable Superstable Black Hole Seed (cost audit #1, B-21). Each is one UHV Space Assembler craft.
     */
    static final long DENSITY_UPGRADE_SEEDS = 32L;

    public static void register() {
        controllerAndModules();
        legacyCores();
        // SL-K04 (Kerr Density Upgrade) moved to the Condensate Assembler as SL-B31 (BTSRecipesBEC, ruling 11b);
        // its old body is BTSRecipesBEC's fallback.
        densify();
    }

    // ============================================================== controller + module cores (UIV)

    private static void controllerAndModules() {
        // SL-K01 Kerr Ergosphere Engine (controller). Research: Black Hole Seed.
        // GTNH 2.9.0-RC-2: the BHC controller became an ingredient (every Spacetime Compression Field
        // Generator, the coremod's 64-controller Compressed Circuit Array), and BTSBlackHoleSwap moves all
        // of those onto this controller. So it is priced at BHC parity (B-30, costs doc §9): the BHC
        // controller's own compression lineage (HIP + Neutronium Compressor, 16 Advanced Implosion, 8
        // Compressor UIV, 4 Electric Implosion), its circuits, field generators, superdense plates and
        // fluids, with the Kerr's identity (a Black Hole Seed, a Superradiant Mirror Segment, Field Coils,
        // one BHC casing, TM screws, Super Coolant) in place of the BHC's lenses, ProtoHalkonite parts,
        // Kevlar and Oganesson. The old card price (2,048 SpaceTime ingots molten, 8 seeds, 15 min) made
        // one Eye T1 gate (236 generators) cost 480k SpaceTime ingots.
        BTSRecipeGuard.recipe(
            "SL-K01",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, nn(ItemList.Black_Hole_Opener.get(1L), "ItemList.Black_Hole_Opener"))
                .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    BTSRecipeGuard.alInputs(
                        nn(ItemList.Machine_Multi_HIPCompressor.get(1L), "ItemList.Machine_Multi_HIPCompressor"),
                        nn(
                            ItemList.Machine_Multi_NeutroniumCompressor.get(1L),
                            "ItemList.Machine_Multi_NeutroniumCompressor"),
                        nn(ItemList.AdvancedImplosionCompressor.get(16L), "ItemList.AdvancedImplosionCompressor"),
                        nn(ItemList.CompressorUIV.get(8L), "ItemList.CompressorUIV"),
                        nn(ItemList.ElectricImplosionCompressor.get(4L), "ItemList.ElectricImplosionCompressor"),
                        nn(ItemList.Field_Generator_UEV.get(4L), "ItemList.Field_Generator_UEV"),
                        BTSRecipeGuard.anyCircuit(Materials.UIV, 4),
                        nn(ItemList.Black_Hole_Opener.get(1L), "ItemList.Black_Hole_Opener"),
                        BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 1),
                        SLBlocks.casing(BlockSLCasing.FIELD_COIL, 4),
                        casingStack(Casings.ExtremeDensitySpaceBendingCasing, 1),
                        BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Neutronium, 16L),
                        BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Naquadria, 16L),
                        BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.CosmicNeutronium, 8L),
                        BTSRecipeGuard.req(OrePrefixes.screw, Materials.TranscendentMetal, 16L)))
                .fluidInputs(
                    molten(Materials.SpaceTime, 2_304),
                    molten(Materials.SuperconductorUIVBase, 43_200),
                    molten(Materials.Infinity, 14_400),
                    superCoolant(64_000))
                .itemOutputs(nn(BTSMachines.kerrEngine, "BTSMachines.kerrEngine").copy())
                .duration(3_600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-K02 Kerr Compressor Module. Kerr redesign: the module is a machine now, so the same inputs make
        // its CONTROLLER (MTE 28117) instead of the retired core block; the cost table is unchanged.
        // Research: the Kerr controller.
        BTSRecipeGuard.recipe(
            "SL-K02",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, kerr())
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    BTSRecipeGuard.alInputs(
                        nn(ItemList.Electric_Piston_UIV.get(32L), "ItemList.Electric_Piston_UIV"),
                        nn(ItemList.Field_Generator_UIV.get(8L), "ItemList.Field_Generator_UIV"),
                        BTSRecipeGuard.anyCircuit(Materials.UIV, 16),
                        BTSRecipesSingularity.dense(Materials.Neutronium, 32),
                        nn(ItemList.Black_Hole_Opener.get(4L), "ItemList.Black_Hole_Opener"),
                        BTSRecipeGuard.req(OrePrefixes.gearGt, Materials.SpaceTime, 16L)))
                .fluidInputs(molten(Materials.SpaceTime, 73_728), superCoolant(500_000))
                .itemOutputs(nn(BTSMachines.kerrCompressorModule, "BTSMachines.kerrCompressorModule").copy())
                .duration(12_000)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-K03 Kerr Extractor Module: its CONTROLLER (MTE 28118), same inputs (Kerr redesign). Research: an
        // Extraction casing (one research
        // item per recipe: the scanner writes only the FIRST recipe sharing one, and SL-K02 has kerr()).
        BTSRecipeGuard.recipe(
            "SL-K03",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.EXTRACTION, 1))
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    BTSRecipeGuard.alInputs(
                        // Pistons, not pumps: the module runs GT's ITEM extractor recipes (Q-9/Q-22, B-17;
                        // cost audit #14).
                        nn(ItemList.Electric_Piston_UIV.get(32L), "ItemList.Electric_Piston_UIV"),
                        nn(ItemList.Field_Generator_UIV.get(8L), "ItemList.Field_Generator_UIV"),
                        BTSRecipeGuard.anyCircuit(Materials.UIV, 16),
                        BTSRecipesSingularity.dense(Materials.Neutronium, 32),
                        SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 4),
                        neutroniumPipes(16)))
                .fluidInputs(molten(Materials.SpaceTime, 73_728), superCoolant(500_000))
                .itemOutputs(nn(BTSMachines.kerrExtractorModule, "BTSMachines.kerrExtractorModule").copy())
                .duration(12_000)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));
    }

    // ===================================================== legacy module cores -> controllers (redesign)

    /**
     * SL-K07 / SL-K08: a pre-redesign module core block (crafted by the old SL-K02 / SL-K03, same
     * inputs) turns 1:1 into its module controller, so cores in chests are not wasted. Shapeless.
     */
    private static void legacyCores() {
        BTSRecipeGuard.recipe(
            "SL-K07",
            () -> GameRegistry.addShapelessRecipe(
                nn(BTSMachines.kerrCompressorModule, "BTSMachines.kerrCompressorModule").copy(),
                SLBlocks.casing2(BlockSLCasing2.COMPRESSOR_MODULE, 1)));
        BTSRecipeGuard.recipe(
            "SL-K08",
            () -> GameRegistry.addShapelessRecipe(
                nn(BTSMachines.kerrExtractorModule, "BTSMachines.kerrExtractorModule").copy(),
                SLBlocks.casing2(BlockSLCasing2.EXTRACTOR_MODULE, 1)));
    }

    // ================================================ DENSITY (Kerr T2 + the compressor module)

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
