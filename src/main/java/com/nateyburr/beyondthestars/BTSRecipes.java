package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Gen I fuel chain, priced on the platinum line per review ruling: platinum cladding for the
 * basic rods (platinum is cheap at IV), rhodium-plated palladium + iridium for the assembly
 * hardware, and circuit + rare-metal spam (tungstensteel/tungsten) on the controller.
 */
public final class BTSRecipes {

    private BTSRecipes() {}

    public static void register() {
        final Materials rhPdPalladium = WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial();

        // Uranium dust -> fuel pellets
        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L))
            .circuit(2)
            .itemOutputs(new ItemStack(BTSItems.uraniumFuelPellet, 2))
            .duration(5 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(assemblerRecipes);

        // Pellets in platinum cladding -> fuel rod
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.uraniumFuelPellet, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Platinum, 2L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 4L))
            .circuit(1)
            .itemOutputs(new ItemStack(BTSItems.fuelRodUranium, 1))
            .duration(12 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // Rods in rhodium-plated palladium + iridium hardware -> fuel assembly
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 3),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iridium, 1L))
            .circuit(1)
            .fluidInputs(Materials.Platinum.getMolten(2 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelAssemblyUranium, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // Stainless structure with platinum lining -> reactor fuel column segments
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 4L),
                GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.StainlessSteel, 1L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 2L))
            .circuit(3)
            .itemOutputs(new ItemStack(BTSBlocks.reactorFuelColumn, 2))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // Radiation proof casing + IV circuits + rare metals -> Reactor Core (Gen I) controller
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_RadiationProof.get(1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.IV, 4L),
                GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.Lead, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 8L),
                GTOreDictUnificator.get(OrePrefixes.stick, Materials.Tungsten, 8L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iridium, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L))
            .circuit(4)
            .fluidInputs(Materials.Platinum.getMolten(4 * INGOTS))
            .itemOutputs(BTSMachines.reactorCoreMk1.copy())
            .duration(60 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);
    }
}
