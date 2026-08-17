package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Placeholder recipe balance for the Gen I chain - deliberately cheap-ish while the campus is in
 * development. Real gating lands with the Enrichment Train (pellets will come from enriched
 * feedstock, not raw dust).
 */
public final class BTSRecipes {

    private BTSRecipes() {}

    public static void register() {
        // Uranium dust -> fuel pellets
        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L))
            .circuit(2)
            .itemOutputs(new ItemStack(BTSItems.uraniumFuelPellet, 2))
            .duration(5 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        // Pellets + tiny stainless pipe -> fuel rod
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.uraniumFuelPellet, 4),
                GTOreDictUnificator.get(OrePrefixes.pipeTiny, Materials.StainlessSteel, 1L))
            .circuit(1)
            .itemOutputs(new ItemStack(BTSItems.fuelRodUranium, 1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        // Rods + stainless plates -> fuel assembly
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 3),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 2L))
            .circuit(1)
            .itemOutputs(new ItemStack(BTSItems.fuelAssemblyUranium, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        // Stainless plates + medium pipe -> reactor fuel column segments
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 4L),
                GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.StainlessSteel, 1L))
            .circuit(3)
            .itemOutputs(new ItemStack(BTSBlocks.reactorFuelColumn, 2))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // Radiation proof casing + lead plates -> Reactor Core (Gen I) controller
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_RadiationProof.get(1L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Lead, 6L))
            .circuit(4)
            .itemOutputs(BTSMachines.reactorCoreMk1.copy())
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);
    }
}
