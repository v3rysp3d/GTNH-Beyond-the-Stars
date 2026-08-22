package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.fluidExtractionRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidSolidifierRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.fluids.BTSFluidsPropellant;
import com.nateyburr.beyondthestars.items.BTSItemsPropellant;

import bartworks.system.material.Werkstoff;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeConstants;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/**
 * D-26 THE PROPELLANT LINE (docs/production-lines.md S4.1). <b>Owned exclusively by Agent C
 * (Propellant + Coolant).</b>
 *
 * <h2>The one ruling that shapes this whole file</h2>
 *
 * S4.1 and S8.1 say the six one-step Mixer recipes are "kept but rebuilt on a longer chain" - that
 * is, retired and replaced. <b>They are not retired here, and that is deliberate.</b> The six
 * {@code naqPropellantT1..T6} fluids are what a RUNNING PLANT drinks. Retiring the only recipe that
 * makes them stops a working plant dead the moment the jar is swapped, and every Mixer and AE2
 * pattern feeding it goes invalid at the same instant. The fuel line could afford exactly that,
 * because I ruled explicitly that fuel-chain stock and automation may stop being makeable
 * the old way. No such ruling covers the propellant ladder, and the standing instruction for this
 * file is the opposite - extend the chains BEHIND the rungs, and starve nothing.
 * <p>
 * So the six legacy Mixer recipes stand below, unchanged, and the new chain is added ALONGSIDE them
 * as a cheaper route. Nothing a player has built stops working; the reward for building the chain
 * is a smaller bill, never a rung they can no longer reach.
 *
 * <h2>What the chain buys, and the invariant it must not break</h2>
 *
 * The chain halves the naquadah-fuel bill per litre of propellant and leaves the metal-dust bill
 * exactly where the legacy recipe put it:
 *
 * <pre>
 *   rung   legacy Mixer                         new chain (P-03 / P-03h)
 *   Lv 1   2 dust NaqEnriched + 1,000 L light   2 dust Al   (500 L slurry) +  500 L light
 *   Lv 2   8 dust NaqEnriched + 1,000 L MkI     8 dust Al (2,000 L slurry) +  500 L MkI
 *   Lv 3   4 dust Adamantium  + 1,000 L MkIII   4 dust Ad (1,000 L slurry) +  500 L MkIII
 *   Lv 4   8 dust Adamantium  + 1,000 L MkIV    8 dust Ad (2,000 L slurry) +  500 L MkIV
 *   Lv 5   2 dust Infinity    + 1,000 L MkV     2 dust In   (500 L slurry) +  500 L MkV
 *   Lv 6   4 dust Infinity    + 1,000 L MkVI    4 dust In (1,000 L slurry) +  500 L MkVI
 * </pre>
 *
 * Every rung: naquadah fuel 1,000 L -> 500 L, the same dust count, plus 1,000 L of oxidiser. Lv 1
 * and Lv 2 additionally trade Naquadah Enriched dust for Aluminium - that is S4.1's deliberate
 * "aluminised propellant is the textbook case" choice, and it is the single largest saving in the
 * file. It is paid for by four machines (Chemical Plant, IsaMill, Chemical Plant, Chemical
 * Plant/QFT) standing where one Mixer used to.
 * <p>
 * <b>250 L per cycle, both routes, every rung - and the new route is SLOWER.</b> D-20 balanced the
 * Spiral chamber's supply rate against its clock frequency on 250 L per cycle, and D-08's P values
 * are not up for revision here. P-03 emits the same 250 L, over 30 s instead of the legacy 20 s. So
 * the chain is strictly a MATERIALS play: it buys cheaper propellant per litre of naquadah fuel, and
 * it can never buy a higher supply rate out of one machine. That is the same shape as the water
 * ladder's balance guard - the long way round buys cheaper input, never more throughput.
 *
 * <h2>Two nitrogen tetroxides, because the pack has two</h2>
 *
 * P-01 is registered twice. GTNH ships {@code Materials.DinitrogenTetroxide} (GT core, a GAS, made
 * from Nitrogen Dioxide at LV) and {@code GTPPFluids.NitrogenTetroxide} (a separate GT++ fluid, made
 * from Nitric Acid over copper in the Chemical Plant). They are the same molecule under two registry
 * entries. Accepting only one would have sent a player who already produces the other off to build a
 * second N2O4 line for no reason, so P-01 accepts either.
 * <p>
 * Note {@code getGas}, not {@code getFluid}: {@code Materials.DinitrogenTetroxide} is registered
 * gas-side, and {@code getFluid} on it does not return the fluid a player can actually make.
 */
public final class BTSRecipesPropellant {

    /** Oxidiser charge per blended rung - flat across the ladder, so the NEI page reads at a glance. */
    private static final int OXIDISER_PER_RUNG = 1_000;
    /** Naquadah fuel per blended rung. The legacy Mixer step spends 1,000 L for the same 250 L out. */
    private static final int NAQ_FUEL_PER_RUNG = 500;
    /** Litres of slurry one dust of metal becomes: P-02b turns 8 dust into 2,000 L. */
    private static final int SLURRY_PER_DUST = 250;
    /** D-20: the supply-rate-against-clock balance is calibrated on this. Both routes, every rung. */
    private static final int PROPELLANT_PER_CYCLE = 250;

    private BTSRecipesPropellant() {}

    public static void register() {
        legacyMixerLadder();
        oxidiserConditioning();
        metalAdditiveMilling();
        metalAdditiveSlurry();
        blending();
        cartridges();
    }

    // ------------------------------------------------------------------------------------------
    // THE LEGACY LADDER - moved verbatim at the split commit, and KEPT. See the class comment:
    // retiring these six would stop a running plant dead, and the standing instruction for this
    // file is to extend the chains behind the rungs and starve nothing.
    // ------------------------------------------------------------------------------------------

    private static void legacyMixerLadder() {
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

    // ------------------------------------------------------------------------------------------
    // P-01 OXIDISER CONDITIONING - Chemical Plant tier 2, EV
    // 2,000 L Nitric Acid + 1,000 L N2O4 -> 3,000 L Oxidiser Blend. Red fuming nitric acid: HNO3
    // with N2O4 dissolved into it. The real storable oxidiser, and the reason the blend is one
    // fluid rather than two hatches feeding the blender.
    // ------------------------------------------------------------------------------------------

    private static void oxidiserConditioning() {
        // GT core's N2O4 - registered gas-side, so getGas and NOT getFluid.
        GTValues.RA.stdBuilder()
            .circuit(1)
            .fluidInputs(Materials.NitricAcid.getFluid(2_000), Materials.DinitrogenTetroxide.getGas(1_000))
            .fluidOutputs(new FluidStack(BTSFluidsPropellant.oxidiserBlend, 3_000))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .metadata(GTRecipeConstants.CHEMPLANT_CASING_TIER, 2)
            .addTo(GTPPRecipeMaps.chemicalPlantRecipes);

        // GT++'s own N2O4, the one a standing rocket-fuel line already makes.
        GTValues.RA.stdBuilder()
            .circuit(1)
            .fluidInputs(Materials.NitricAcid.getFluid(2_000), new FluidStack(GTPPFluids.NitrogenTetroxide, 1_000))
            .fluidOutputs(new FluidStack(BTSFluidsPropellant.oxidiserBlend, 3_000))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .metadata(GTRecipeConstants.CHEMPLANT_CASING_TIER, 2)
            .addTo(GTPPRecipeMaps.chemicalPlantRecipes);
    }

    // ------------------------------------------------------------------------------------------
    // P-02a ULTRAFINE MILLING - IsaMill (Ore Milling Plant), items only
    // The map is maxIO(3, 1, 0, 0): no fluid slots at all, and the milling ball comes from the
    // Bus_Milling_Balls hatch rather than from the recipe - putting one in itemInputs would have
    // made every P-02 recipe demand a ball it then consumed. The three dusts distinguish the three
    // recipes on their own, so no circuit is demanded of the player.
    // ------------------------------------------------------------------------------------------

    private static void metalAdditiveMilling() {
        mill(Materials.Aluminium, BTSItemsPropellant.ultrafineAluminium, TierEU.RECIPE_EV);
        mill(Materials.Adamantium, BTSItemsPropellant.ultrafineAdamantium, TierEU.RECIPE_LuV);
        mill(Materials.Infinity, BTSItemsPropellant.ultrafineInfinity, TierEU.RECIPE_UHV);
    }

    private static void mill(Materials metal, Item powder, long tier) {
        GTValues.RA.stdBuilder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, metal, 8L))
            .itemOutputs(new ItemStack(powder, 1))
            .duration(30 * SECONDS)
            .eut(tier)
            .addTo(GTPPRecipeMaps.millingRecipes);
    }

    // ------------------------------------------------------------------------------------------
    // P-02b SLURRY BLENDING - Chemical Plant tier 2
    // The powder goes into kerosene. 8 dust -> 2,000 L, i.e. 250 L of slurry per dust, which is the
    // conversion the blending table in the class comment is written against.
    // ------------------------------------------------------------------------------------------

    private static void metalAdditiveSlurry() {
        slurry(BTSItemsPropellant.ultrafineAluminium, BTSFluidsPropellant.metalSlurryAluminium, TierEU.RECIPE_EV);
        slurry(BTSItemsPropellant.ultrafineAdamantium, BTSFluidsPropellant.metalSlurryAdamantium, TierEU.RECIPE_LuV);
        slurry(BTSItemsPropellant.ultrafineInfinity, BTSFluidsPropellant.metalSlurryInfinity, TierEU.RECIPE_UHV);
    }

    private static void slurry(Item powder, Fluid out, long tier) {
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(powder, 1))
            .fluidInputs(new FluidStack(GTPPFluids.Kerosene, 1_000))
            .fluidOutputs(new FluidStack(out, 2_000))
            .duration(20 * SECONDS)
            .eut(tier)
            .metadata(GTRecipeConstants.CHEMPLANT_CASING_TIER, 2)
            .addTo(GTPPRecipeMaps.chemicalPlantRecipes);
    }

    // ------------------------------------------------------------------------------------------
    // P-03 / P-03h BLENDING - the rung itself
    // Lv 1-3 on the Chemical Plant tier 3; Lv 4-6 on the Quantum Force Transformer, focus tier 1,
    // over a Simple Naquadah Catalyst - the same catalyst GoodGenerator's own naquadah fuel chain
    // runs on, so it is a thing the player provably already has by the time they are standing here.
    // Circuit 5 throughout, matching the legacy ladder's convention.
    // ------------------------------------------------------------------------------------------

    private static void blending() {
        blendLow(
            GGMaterial.lightNaquadahFuel,
            BTSFluidsPropellant.metalSlurryAluminium,
            2,
            BTSFluids.naqPropellantT1,
            TierEU.RECIPE_LuV);
        blendLow(
            GGMaterial.naquadahBasedFuelMkI,
            BTSFluidsPropellant.metalSlurryAluminium,
            8,
            BTSFluids.naqPropellantT2,
            TierEU.RECIPE_ZPM);
        blendLow(
            GGMaterial.naquadahBasedFuelMkIII,
            BTSFluidsPropellant.metalSlurryAdamantium,
            4,
            BTSFluids.naqPropellantT3,
            TierEU.RECIPE_UV);

        blendHigh(
            GGMaterial.naquadahBasedFuelMkIV,
            BTSFluidsPropellant.metalSlurryAdamantium,
            8,
            BTSFluids.naqPropellantT4,
            TierEU.RECIPE_UHV);
        blendHigh(
            GGMaterial.naquadahBasedFuelMkV,
            BTSFluidsPropellant.metalSlurryInfinity,
            2,
            BTSFluids.naqPropellantT5,
            TierEU.RECIPE_UEV);
        blendHigh(
            GGMaterial.naquadahBasedFuelMkVI,
            BTSFluidsPropellant.metalSlurryInfinity,
            4,
            BTSFluids.naqPropellantT6,
            TierEU.RECIPE_UIV);
    }

    /** Lv 1-3, Chemical Plant tier 3. {@code dustEquivalent} is the legacy recipe's dust count. */
    private static void blendLow(Werkstoff fuel, Fluid slurry, int dustEquivalent, Fluid out, long tier) {
        GTValues.RA.stdBuilder()
            .circuit(5)
            .fluidInputs(
                fuel.getFluidOrGas(NAQ_FUEL_PER_RUNG),
                new FluidStack(BTSFluidsPropellant.oxidiserBlend, OXIDISER_PER_RUNG),
                new FluidStack(slurry, dustEquivalent * SLURRY_PER_DUST))
            .fluidOutputs(new FluidStack(out, PROPELLANT_PER_CYCLE))
            .duration(30 * SECONDS)
            .eut(tier)
            .metadata(GTRecipeConstants.CHEMPLANT_CASING_TIER, 3)
            .addTo(GTPPRecipeMaps.chemicalPlantRecipes);
    }

    /** Lv 4-6, Quantum Force Transformer, focus tier 1. */
    private static void blendHigh(Werkstoff fuel, Fluid slurry, int dustEquivalent, Fluid out, long tier) {
        GTValues.RA.stdBuilder()
            .circuit(5)
            .fluidInputs(
                fuel.getFluidOrGas(NAQ_FUEL_PER_RUNG),
                new FluidStack(BTSFluidsPropellant.oxidiserBlend, OXIDISER_PER_RUNG),
                new FluidStack(slurry, dustEquivalent * SLURRY_PER_DUST))
            .fluidOutputs(new FluidStack(out, PROPELLANT_PER_CYCLE))
            .duration(30 * SECONDS)
            .eut(tier)
            .metadata(GTRecipeConstants.QFT_CATALYST, GregtechItemList.SimpleNaquadahCatalyst.get(0))
            .metadata(GTRecipeConstants.QFT_FOCUS_TIER, 1)
            .addTo(GTPPRecipeMaps.quantumForceTransformerRecipes);
    }

    // ------------------------------------------------------------------------------------------
    // P-04 CARTRIDGES - storage and logistics, NOT a rung
    // A cartridge is worth exactly the 1,000 L that went into it and is round-trippable, so it
    // moves no P value, and the Spiral chamber cannot be fed one. The Fluid Solidifier map is
    // maxIO(1, 1, 1, 0) - the mould takes the only item slot, so the fluid alone has to distinguish
    // the six recipes, which it does.
    // ------------------------------------------------------------------------------------------

    private static void cartridges() {
        cartridge(BTSFluids.naqPropellantT1, BTSItemsPropellant.propellantCartridge1);
        cartridge(BTSFluids.naqPropellantT2, BTSItemsPropellant.propellantCartridge2);
        cartridge(BTSFluids.naqPropellantT3, BTSItemsPropellant.propellantCartridge3);
        cartridge(BTSFluids.naqPropellantT4, BTSItemsPropellant.propellantCartridge4);
        cartridge(BTSFluids.naqPropellantT5, BTSItemsPropellant.propellantCartridge5);
        cartridge(BTSFluids.naqPropellantT6, BTSItemsPropellant.propellantCartridge6);
    }

    private static void cartridge(Fluid propellant, Item cartridge) {
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.Shape_Mold_Ball.get(0))
            .fluidInputs(new FluidStack(propellant, 1_000))
            .itemOutputs(new ItemStack(cartridge, 1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(fluidSolidifierRecipes);

        // The return leg. Without it a cartridge is a one-way trip, and the "worth exactly the
        // fluid that went into it" claim above would simply be false.
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(cartridge, 1))
            .fluidOutputs(new FluidStack(propellant, 1_000))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(fluidExtractionRecipes);
    }
}
