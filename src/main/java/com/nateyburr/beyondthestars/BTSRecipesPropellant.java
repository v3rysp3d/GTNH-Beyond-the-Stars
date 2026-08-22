package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluids;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;

/**
 * D-26 THE PROPELLANT LINE (docs/production-lines.md S4.1). <b>Owned exclusively by Agent C
 * (Propellant + Coolant).</b>
 * <p>
 * What stands below is the OLD line, moved verbatim: six one-step Mixer recipes, one per rung.
 * S4.1 replaces each with a real three-stage chain - P-01 conditions an oxidiser, P-02 mills a
 * metal additive into kerosene (aluminised propellant is the textbook case), P-03/P-03h blends
 * the two with a naquadah fuel.
 * <p>
 * <b>The constraint that governs this file: 250 L per cycle, unchanged.</b> The rungs P values
 * are D-08 and are not up for revision here, and the Spiral chamber whole supply-rate-against-
 * clock-frequency balance (D-20) is calibrated on 250 L. Only the INPUT side of each rung gets
 * longer. A rung that came out at a different volume would relitigate two settled rulings by
 * accident.
 */
public final class BTSRecipesPropellant {

    private BTSRecipesPropellant() {}

    public static void register() {
        // SD-3: high-grade naquadah propellants (Lv 4-6) off the deeper GoodGenerator fuels
        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Adamantium, 8L))
            .circuit(5)
            .fluidInputs(GGMaterial.naquadahBasedFuelMkIV.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT4, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_UHV)
            .addTo(mixerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Infinity, 2L))
            .circuit(5)
            .fluidInputs(GGMaterial.naquadahBasedFuelMkV.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT5, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_UEV)
            .addTo(mixerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Infinity, 4L))
            .circuit(5)
            .fluidInputs(GGMaterial.naquadahBasedFuelMkVI.getFluidOrGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluids.naqPropellantT6, 250))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_UIV)
            .addTo(mixerRecipes);

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
    }
}
