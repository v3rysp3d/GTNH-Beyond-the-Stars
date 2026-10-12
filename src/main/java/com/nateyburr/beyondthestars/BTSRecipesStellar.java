package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFreezerRecipes;

import com.nateyburr.beyondthestars.fluids.SLFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;

/**
 * D-49: THE ANTIPOLARIZATION AGENT CHAIN, SL-S01..SL-S02 (d49-build-plan.md §10.1, star-lifecycle.md §5).
 * <b>Owned by the D-49 content unit.</b> Called from one line in {@link BTSRecipes}.
 *
 * <pre>
 *   Raw Tengam  --EMS-->  Purified Tengam  --Polarizer-->  Attuned Tengam dust        (GT, unchanged)
 *   Protomatter --Antimatter Forge-->  Semi-Stable Antimatter                            (GoodGenerator, unchanged)
 *   SL-S01  LCR:  16 Attuned Tengam dust + 100 L Antimatter + 144 L molten SpaceTime -> 1,000 L Positronic Slurry
 *   SL-S02  Vacuum Freezer:  1,000 L Slurry + 2,000 L Super Coolant -> 1,000 L Antipolarization Agent
 *   Eye T6: the Agent is the magnetar's constant feed (my answer 1 to the plan) and its flare-shield trim; the
 *           Eye returns 12 Purified Tengam dust per 1,000 L it burns (75 % of the Tengam), so Antimatter is the
 *           real consumable.
 * </pre>
 *
 * Every pack material here was checked in the 2.9.0-RC-2 GT jar (5.09.54.205 sources):
 * {@code Materials.TengamAttuned} (dust, metal items), {@code Materials.Antimatter} (fluid {@code antimatter},
 * linked by {@code LoaderGTBlockFluid}), {@code Materials.SpaceTime} (molten), {@code Materials.SuperCoolant}.
 * Purified to Attuned is GT's own polarizer recipe ({@code ProcessingTransforming}); Raw to Purified is its EMS
 * recipe ({@code ElectromagneticSeparatorRecipes}).
 */
public final class BTSRecipesStellar {

    private BTSRecipesStellar() {}

    public static void register() {
        // SL-S01 Positronic Tengam Slurry: the positrons go in. 20 s at UXV, like the proposal.
        BTSRecipeGuard.recipe(
            "SL-S01",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.TengamAttuned, 16L))
                .fluidInputs(
                    nn(Materials.Antimatter.getFluid(100L), "Materials.Antimatter fluid"),
                    nn(Materials.SpaceTime.getMolten(144L), "molten SpaceTime"))
                .fluidOutputs(BTSRecipeGuard.fluid(SLFluids.positronicTengamSlurry, 1_000))
                .duration(400)
                .eut(TierEU.RECIPE_UXV)
                .addTo(multiblockChemicalReactorRecipes));

        // SL-S02 Antipolarization Agent: field-cooling locks the reversed domains. 10 s at UXV.
        BTSRecipeGuard.recipe(
            "SL-S02",
            () -> GTValues.RA.stdBuilder()
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.positronicTengamSlurry, 1_000),
                    nn(Materials.SuperCoolant.getFluid(2_000L), "Materials.SuperCoolant fluid"))
                .fluidOutputs(BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 1_000))
                .duration(200)
                .eut(TierEU.RECIPE_UXV)
                .addTo(vacuumFreezerRecipes));
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }
}
