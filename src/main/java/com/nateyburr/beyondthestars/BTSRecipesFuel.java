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

import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * D-26 THE FUEL LINE - ore to bundle, and the PUREX head-end that closes the loop
 * (docs/production-lines.md S1). <b>Owned exclusively by Agent A (Fuel).</b>
 * <p>
 * <b>What stands in here right now is the OLD chain, moved verbatim.</b> The split commit that
 * created this file changed not one ingredient, count, duration or voltage - the point of moving
 * first and editing second is that the split is provably behaviour-neutral before anybody starts
 * adding to it. The eighteen recipes S8.1 retires are still standing below; retire each one as
 * its replacement step lands, so the file never passes through a state where a fuel grade has no
 * route at all.
 * <p>
 * <b>The two ratios that may not move.</b> F-17 takes 4 pellets per rod and F-18 takes 3 rods per
 * assembly, which are exactly the ratios of the retired recipes below. That is what makes every
 * pellet, rod and assembly already in a player chest worth precisely what it was worth yesterday:
 * the redesign changes the ROUTE, never the value (S8.2).
 * <p>
 * <b>The water step.</b> F-12 is the ONLY recipe in this file with a water rung, and its nine
 * parallel entries must take their output count from {@code BTSWaterGrade.powderFor(grade)}
 * rather than from a literal - so the NEI page and the reactor readout are derived from one table
 * and cannot drift apart. F-20 deliberately has no water key (S2.3).
 */
public final class BTSRecipesFuel {

    private BTSRecipesFuel() {}

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

        // Naquadah-bonded assemblies (F=9): the top rod fuel
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

        // SD-1 rod cascade: bonded rods (an assembly is three rods - inputs scale to a third)
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 1),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 3L),
                GTOreDictUnificator.get(OrePrefixes.plate, rhPdPalladium, 1L))
            .circuit(7)
            .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelRodThoriumBonded, 1))
            .duration(12 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BTSItems.fuelRodUranium, 1),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 3L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium235, 1L),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iridium, 1L))
            .circuit(7)
            .fluidInputs(Materials.Naquadah.getMolten(1 * INGOTS))
            .itemOutputs(new ItemStack(BTSItems.fuelRodNaqBonded, 1))
            .duration(16 * SECONDS)
            .eut(TierEU.RECIPE_ZPM)
            .addTo(assemblerRecipes);

        // SD-1: depleted ROD reprocessing (a third of the assembly yields)
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelRodUranium, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Lead, 1L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 1L))
            .outputChances(10000, 6700, 10000, 5000)
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(centrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelRodMox, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Lead, 2L),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Platinum, 1L))
            .outputChances(10000, 10000, 10000, 5000)
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(centrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelRodThoriumBonded, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Lead, 2L))
            .outputChances(10000, 10000, 6700, 10000)
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(centrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(BTSItems.depletedFuelRodNaqBonded, 1))
            .itemOutputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uranium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Naquadah, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Lead, 2L))
            .outputChances(10000, 10000, 10000, 10000)
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(centrifugeRecipes);
    }
}
