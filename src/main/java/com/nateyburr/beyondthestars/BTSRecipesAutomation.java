package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;

import com.nateyburr.beyondthestars.machines.BTSMachines;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;

/**
 * AUTOMATION PASS (brief item 18): THE DIRECTIVE HATCH, SL-A01. <b>Owned by the automation pass.</b> Called from one
 * line in {@link BTSRecipes}.
 * <p>
 * The hatch serves the Kerr Ergosphere Engine (UIV, the first ladder machine) as well as the Eye, so it is made at
 * UIV, where the Kerr is: a UIV hull with two UIV sensors and an emitter (it reads and it signals), two UIV circuits
 * and Redstone Alloy plates. That sits beside GT's own UIV hatches and well under the Stellar Field Sensor Hatch
 * (SL-B22, UMV, a Condensate Assembler recipe), which is an Eye-only part. P4's card (UXV parts) assumed a T7+ host;
 * the Kerr needs it from day one (question B-70).
 */
public final class BTSRecipesAutomation {

    private BTSRecipesAutomation() {}

    public static void register() {
        // SL-A01 Directive Hatch: assembler, 30 s at UIV.
        BTSRecipeGuard.recipe(
            "SL-A01",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Hull_UIV.get(1L), "ItemList.Hull_UIV"),
                    nn(ItemList.Sensor_UIV.get(2L), "ItemList.Sensor_UIV"),
                    nn(ItemList.Emitter_UIV.get(1L), "ItemList.Emitter_UIV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.RedstoneAlloy, 16L))
                .fluidInputs(nn(Materials.Infinity.getMolten(288L), "molten Infinity"))
                .itemOutputs(nn(BTSMachines.directiveHatch, "BTSMachines.directiveHatch").copy())
                .duration(30 * 20)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }
}
