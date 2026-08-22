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
 * D-26 PLANT HARDWARE - the controllers, hatches, control blocks, plant blocks and the turbine
 * rotor. <b>Owned exclusively by Agent D (Machines).</b>
 * <p>
 * Every block below was moved here verbatim by the split commit: not one ingredient changed.
 * These are the recipes for the plant own furniture, which S3 explicitly leaves alone - the
 * material lines are BULK routes to stock GT casings that sit beside GT recipes, and they live in
 * {@link BTSRecipesMaterials}, not here.
 * <p>
 * <b>Still to add:</b> controller recipes for the four new multiblocks of S5 -
 * {@code BTSMachines.siteBatchingPlant} (EV, the cheapest of the four and the FIRST thing a
 * player builds), {@code cascadeHall}, {@code fuelFabricationHall} and {@code purexCanyon} (all
 * IV). All four ItemStacks are registered already and are ready to be named as recipe outputs.
 */
public final class BTSRecipesMachines {

    private BTSRecipesMachines() {}

    public static void register() {
        final Materials rhPdPalladium = WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial();

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

        // SD-2: Turbine Housing - the Spine's rotor socket (right-click sets spin direction)
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 4L),
                GTOreDictUnificator.get(OrePrefixes.stickLong, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 1L))
            .circuit(9)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(new ItemStack(BTSBlocks.turbineHousing, 2))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // SD-1: Rod Loading Port - the pillar's automation input
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSBlocks.reactorFuelColumn, 1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 2L),
                ItemList.Robot_Arm_EV.get(1L))
            .circuit(8)
            .itemOutputs(new ItemStack(BTSBlocks.rodLoadingPort, 1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // SD-3: Spiral chamber hatches
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Pump_IV.get(1L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.IV, 1L))
            .circuit(10)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(BTSMachines.precisionInjector.copy())
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Cover_ActivityDetector.get(1L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2L))
            .circuit(10)
            .itemOutputs(BTSMachines.spineSensor.copy())
            .duration(15 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // TC-2: the plant's control-block panels (dedicated sockets on silo face / gallery)
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Cover_Screen.get(1L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.IV, 2L))
            .circuit(11)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(BTSMachines.siloControlBlock.copy())
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Cover_Screen.get(1L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.IV, 2L))
            .circuit(12)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(BTSMachines.turbineControlBlock.copy())
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // TC-4: the Turbine Rotor - the gallery stages' consumable heart
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 6L),
                GTOreDictUnificator.get(OrePrefixes.stickLong, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 1L))
            .circuit(13)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.turbineRotor, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // SD-4: the Turbine Hall and its fans are RETIRED - recipes removed, existing
        // items/machines keep working as museum pieces until a later breaking release.

        // Extraction Platform controller (EV): the campus fuel front end
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 8L),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 4L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2L))
            .circuit(4)
            .itemOutputs(BTSMachines.extractionPlatform.copy())
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // Enrichment Train controller (EV): the campus processing factory
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 8L),
                GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.StainlessSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2L))
            .circuit(5)
            .itemOutputs(BTSMachines.enrichmentTrain.copy())
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        // A3: BOTH plant module controllers are RETIRED - the buildings are pieces of the main
        // station now, so neither has a recipe. The MTE ids stay registered (BTSMachines) so
        // worlds that already contain one load cleanly; both tooltips say they are museum
        // pieces and point at the Nuclear Plant Foundation.

        // Radiation proof casing + IV circuits + rare metals -> Reactor Silo controller
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
