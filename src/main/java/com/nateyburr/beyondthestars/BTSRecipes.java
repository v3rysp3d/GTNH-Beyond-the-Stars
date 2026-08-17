package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
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

        // Plutonium + uranium oxides -> MOX pellets (the reprocessing payoff, F=2 fuel)
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Plutonium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 8L))
            .circuit(2)
            .itemOutputs(new ItemStack(BTSItems.moxFuelPellet, 18))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(mixerRecipes);

        // MOX pellets in platinum cladding -> MOX fuel rod
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.moxFuelPellet, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Platinum, 2L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 4L))
            .circuit(2)
            .itemOutputs(new ItemStack(BTSItems.fuelRodMox, 1))
            .duration(12 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // MOX rods -> MOX fuel assembly
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodMox, 3),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iridium, 1L))
            .circuit(2)
            .fluidInputs(Materials.Platinum.getMolten(2 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelAssemblyMox, 1))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(assemblerRecipes);

        // Reprocessing: depleted uranium assemblies -> recovered fissiles + fission products
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelAssemblyUranium, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 4L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 2L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 1L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 2L))
            .outputChances(10000, 10000, 10000, 5000)
            .duration(1 * MINUTES)
            .eut(TierEU.RECIPE_EV)
            .addTo(centrifugeRecipes);

        // Reprocessing: depleted MOX assemblies - richer plutonium, more waste
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelAssemblyMox, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 3L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 3L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 2L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 2L))
            .outputChances(10000, 10000, 10000, 5000)
            .duration(1 * MINUTES)
            .eut(TierEU.RECIPE_EV)
            .addTo(centrifugeRecipes);

        // Nuclear waste water -> distilled water + tantalum (the D-09 profit anchor)
        GTValues.RA.stdBuilder()
            .circuit(1)
            .fluidInputs(new FluidStack(BTSFluids.nuclearWasteWater, 8_000))
            .itemOutputs(Materials.Tantalum.getDustSmall(1))
            .fluidOutputs(GTModHandler.getDistilledWater(7_200))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(distillationTowerRecipes);

        // Thorium-bonded assemblies (F=5): uranium rods bonded with thorium
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 3),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 8L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L))
            .circuit(3)
            .fluidInputs(Materials.Platinum.getMolten(2 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelAssemblyThoriumBonded, 1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(assemblerRecipes);

        // Naquadah-bonded assemblies (F=8): the top rod fuel
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 3),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 8L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iridium, 2L))
            .circuit(3)
            .fluidInputs(Materials.Naquadah.getMolten(2 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelAssemblyNaqBonded, 1))
            .duration(45 * SECONDS)
            .eut(TierEU.RECIPE_ZPM)
            .addTo(assemblerRecipes);

        // Reprocessing the bonded depleted forms
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelAssemblyThoriumBonded, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 4L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 4L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 2L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 2L))
            .outputChances(10000, 10000, 10000, 10000)
            .duration(1 * MINUTES)
            .eut(TierEU.RECIPE_IV)
            .addTo(centrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelAssemblyNaqBonded, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 4L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 4L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 3L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Lead, 2L))
            .outputChances(10000, 10000, 10000, 10000)
            .duration(1 * MINUTES)
            .eut(TierEU.RECIPE_LuV)
            .addTo(centrifugeRecipes);

        // Naquadah propellants: per-litre turbine accelerants from the GoodGenerator fuel family
        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 2L))
            .circuit(5)
            .fluidInputs(GGMaterial.lightNaquadahFuel.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT1, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(mixerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 8L))
            .circuit(5)
            .fluidInputs(GGMaterial.naquadahBasedFuelMkI.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT2, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_ZPM)
            .addTo(mixerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Adamantium, 4L))
            .circuit(5)
            .fluidInputs(GGMaterial.naquadahBasedFuelMkIII.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT3, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_UV)
            .addTo(mixerRecipes);

        // Turbine fans
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 8L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L),
                GTOreDictUnificator.get(OrePrefixes.stickLong, Materials.TungstenSteel, 1L))
            .circuit(12)
            .itemOutputs(new ItemStack(BTSItems.turbineFanMatched, 1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Trinium, 8L),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Adamantium, 1L),
                GTOreDictUnificator.get(OrePrefixes.stickLong, Materials.TungstenSteel, 2L))
            .circuit(12)
            .fluidInputs(Materials.Duranium.getMolten(2 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.turbineFanTDA, 1))
            .duration(1 * MINUTES)
            .eut(TierEU.RECIPE_UV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.turbineFanTDA, 1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Trinium, 8L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Adamantium, 8L))
            .circuit(12)
            .fluidInputs(Materials.NaquadahEnriched.getMolten(4 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.turbineFanTDAOptimized, 1))
            .duration(2 * MINUTES)
            .eut(TierEU.RECIPE_UHV)
            .addTo(assemblerRecipes);

        // Turbine Hall controller
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.TungstenSteel, 8L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.IV, 2L),
                GTOreDictUnificator.get(OrePrefixes.stickLong, Materials.TungstenSteel, 2L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 2L))
            .circuit(4)
            .fluidInputs(Materials.Platinum.getMolten(2 * INGOTS))
            .itemOutputs(BTSMachines.turbineHall.copy())
            .duration(45 * SECONDS)
            .eut(TierEU.RECIPE_IV)
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
