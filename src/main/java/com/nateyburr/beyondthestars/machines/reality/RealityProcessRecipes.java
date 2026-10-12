package com.nateyburr.beyondthestars.machines.reality;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BTSRecipeGuard;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing3;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLMaxItems;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the PROCESS recipes of the three machines' own maps (design repo
 * {@code designs/docs/homogeny-reality.md} §5.6, PROPOSED). One owner (the machine unit); the Condensate Assembler
 * and Condensate Generator recipes for the blocks, cores and controllers live with the recipe unit. Run from one
 * {@code BTSRecipeGuard.line("reality-process", ...)} in {@code BTSRecipes}.
 * <ul>
 * <li>SL-H06 / H11 / H21: the Homogeny's three mixes, special value = minimum Homogeny tier.</li>
 * <li>SL-R03: the Diffraction's split. Input slot 0 is the medium, slots 1..6 the anchors of Regions I..VI in
 * order; output slot k is Region k+1. {@link MTERealityDiffraction} reads these amounts, the time and the EU/t
 * from here.</li>
 * <li>SL-R04 / R05: Reality Refab, RC-2 Condensate Assembler style: one nanite tier per item slot (all at or below
 * Black Dwarf, H-17), the six regions as condensates.</li>
 * </ul>
 * Recipes whose fluids are later phases' (White Hole Matter, Superdense Ergomatter) register against the
 * placeholder fluids {@link SLRealityFluids} provides (H-24): present in NEI, unreachable until an Eye makes them.
 */
public final class RealityProcessRecipes {

    private RealityProcessRecipes() {}

    public static void register() {
        homogeny();
        diffraction();
        refab();
    }

    // ================================================================================== Homogeny

    private static void homogeny() {
        BTSRecipeGuard.recipe(
            "SL-H06",
            () -> GTValues.RA.stdBuilder()
                .fluidInputs(BTSRecipeGuard.nn(Materials.RawStarMatter.getFluid(100_000L), "Raw Star Matter"))
                .fluidOutputs(BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 1_000))
                .specialValue(1)
                .duration(240 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RealityRecipeMaps.HOMOGENY));
        BTSRecipeGuard.recipe(
            "SL-H11",
            () -> GTValues.RA.stdBuilder()
                .fluidInputs(
                    BTSRecipeGuard.nn(Materials.WhiteDwarfMatter.getMolten(72L), "molten White Dwarf Matter"),
                    BTSRecipeGuard.nn(Materials.BlackDwarfMatter.getMolten(72L), "molten Black Dwarf Matter"),
                    BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 1_000))
                .fluidOutputs(BTSRecipeGuard.fluid(SLRealityFluids.degenerateStellarMixture, 1_000))
                .specialValue(2)
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RealityRecipeMaps.HOMOGENY));
        BTSRecipeGuard.recipe(
            "SL-H21",
            () -> GTValues.RA.stdBuilder()
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLRealityFluids.whiteHoleMatter, 144),
                    BTSRecipeGuard.fluid(SLRealityFluids.superdenseErgomatter, 500),
                    BTSRecipeGuard.fluid(SLRealityFluids.degenerateStellarMixture, 1_000))
                .fluidOutputs(BTSRecipeGuard.fluid(SLRealityFluids.entanglementMedium, 1_000))
                .specialValue(3)
                .duration(120 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RealityRecipeMaps.HOMOGENY));
    }

    // =============================================================================== Diffraction

    /** The anchor of each region, in order I..VI (doc §2.3; H-13). */
    static FluidStack[] anchors() {
        return new FluidStack[] { BTSRecipeGuard.nn(Materials.Space.getMolten(1_440L), "molten Space"),
            BTSRecipeGuard.nn(Materials.SpaceTime.getMolten(1_440L), "molten SpaceTime"),
            BTSRecipeGuard.nn(Materials.Time.getMolten(1_440L), "molten Time"),
            BTSRecipeGuard.nn(Materials.Eternity.getMolten(144L), "molten Eternity"),
            BTSRecipeGuard.fluid(SLRealityFluids.superdenseErgomatter, 500),
            BTSRecipeGuard.fluid(SLRealityFluids.whiteHoleMatter, 288) };
    }

    private static void diffraction() {
        BTSRecipeGuard.recipe("SL-R03", () -> {
            final FluidStack[] anchors = anchors();
            final FluidStack[] in = new FluidStack[7];
            in[0] = BTSRecipeGuard.fluid(SLRealityFluids.entanglementMedium, 4_000);
            System.arraycopy(anchors, 0, in, 1, 6);
            final Fluid[] regions = SLRealityFluids.regions();
            final FluidStack[] out = new FluidStack[6];
            for (int i = 0; i < 6; i++) {
                out[i] = BTSRecipeGuard.fluid(regions[i], 1_000);
            }
            GTValues.RA.stdBuilder()
                .fluidInputs(in)
                .fluidOutputs(out)
                .duration(120 * SECONDS)
                .eut(TierEU.RECIPE_MAX)
                .addTo(RealityRecipeMaps.DIFFRACTION);
        });
    }

    // ===================================================================================== Refab

    private static FluidStack[] regions(int litres) {
        final Fluid[] regions = SLRealityFluids.regions();
        final FluidStack[] out = new FluidStack[6];
        for (int i = 0; i < 6; i++) {
            out[i] = BTSRecipeGuard.fluid(regions[i], litres);
        }
        return out;
    }

    private static ItemStack crystal(int n) {
        return BTSRecipeGuard.nn(SLBlocks.casing3(BlockSLCasing3.REGION_CRYSTAL, n), "Region Crystal");
    }

    private static void refab() {
        // SL-R04 Universal Upgrade Core (Eye T9 -> T10, D-43). Doc: 4 x RECIPE_MAX; a GT recipe's EU/t is an int,
        // so it is registered at 1 x RECIPE_MAX (question B-77 item 8).
        BTSRecipeGuard.recipe(
            "SL-R04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(SLBlocks.casing3(BlockSLCasing3.ETERNITY_SHELL, 64), "Eternity Shell Casing"),
                    crystal(16),
                    BTSRecipeGuard.nn(ItemList.Field_Generator_UXV.get(64L), "Field Generator UXV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UXV, 64L),
                    BTSRecipeGuard.nn(ItemList.Emitter_UXV.get(32L), "Emitter UXV"),
                    BTSRecipeGuard.nn(ItemList.MetaMaterial_ResonanceChamber3.get(16L), "Resonance Chamber III"),
                    BTSRecipeGuard.nn(ItemList.MetaMaterial_SensorArray3.get(8L), "Sensor Array III"),
                    BTSRecipeGuard.stack(SLItems.stabiliserPylon, 6))
                .fluidInputs(regions(32_000))
                .itemOutputs(BTSRecipeGuard.stack(SLMaxItems.universalUpgradeCore, 1))
                .metadata(
                    NANITE_TIERS,
                    new NaniteTier[] { NaniteTier.WhiteDwarf, NaniteTier.BlackDwarf, NaniteTier.WhiteDwarf,
                        NaniteTier.SixPhasedCopper, NaniteTier.SixPhasedCopper, NaniteTier.BlackDwarf,
                        NaniteTier.BlackDwarf, NaniteTier.Transcendent })
                .duration(7_200 * SECONDS)
                .eut(TierEU.RECIPE_MAX)
                .addTo(RealityRecipeMaps.REFAB));
        // SL-R05 Star Chart: The Observable Universe (P5 proposal, the Eye T10 target).
        BTSRecipeGuard.recipe(
            "SL-R05",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Tool_DataOrb.get(1L), "Data Orb"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UXV, 1L),
                    crystal(1),
                    BTSRecipeGuard.nn(ItemList.MetaMaterial_SensorArray3.get(1L), "Sensor Array III"))
                .fluidInputs(regions(1_000))
                .itemOutputs(BTSRecipeGuard.stack(SLMaxItems.starChartUniverse, 1))
                .metadata(
                    NANITE_TIERS,
                    new NaniteTier[] { NaniteTier.Carbon, NaniteTier.Transcendent, NaniteTier.BlackDwarf,
                        NaniteTier.BlackDwarf })
                .duration(120 * SECONDS)
                .eut(TierEU.RECIPE_MAX)
                .addTo(RealityRecipeMaps.REFAB));
    }
}
