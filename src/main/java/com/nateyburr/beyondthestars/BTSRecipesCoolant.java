package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFreezerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;

import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluidsPropellant;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipeConstants;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtnhlanth.common.register.WerkstoffMaterialPool;

/**
 * D-26 THE COOLANT LINE - bulk manufacture only (docs/production-lines.md S4.2). <b>Owned
 * exclusively by Agent C (Propellant + Coolant).</b>
 *
 * <h2>Scope, said plainly</h2>
 *
 * The coolant ladder is a settled D-08 table of six EXISTING pack fluids - Water, Distilled, IC2
 * Coolant, Super Coolant, Liquid Cryotheum, Tachyon Rich Temporal Fluid. This file makes two of
 * those fluids at plant scale and does nothing else. Inventing a seventh rung or moving a Q would
 * be relitigating a ruling rather than implementing one.
 * <p>
 * <b>Nothing here is required.</b> Every rung keeps its stock pack chain, untouched and still the
 * cheapest way in materials to get a litre. These are additional BULK routes for a plant that
 * drinks faster than a single Mixer can pour, and a plant running today is unaffected by their
 * existence. Liquid Cryotheum and Tachyon Rich Temporal Fluid get no second route at all - both are
 * top-tier fluids with well-established production, and neither needs one.
 *
 * <h2>The balance rule these two recipes are built on</h2>
 *
 * <b>Materials scale exactly; the shim buys speed.</b> Both bulk recipes take exactly 8x the stock
 * recipe's inputs for exactly 8x its output, so the cost per litre in real materials is identical
 * to stock and cannot be arbitraged. What they add is 200-400 L of Chemical Shim and what they give
 * back is <b>2x the throughput per machine</b> (half the duration 8 stock cycles would take). A
 * corrosion inhibitor is exactly what lets a real loop be driven harder without pitting it.
 * <p>
 * The stock recipes were read out of the pinned GT 5.09.52.594 jar rather than guessed:
 *
 * <pre>
 *   IC2 Coolant    1 dust Lapis + circuit 4 + 1,000 L Distilled Water -> 1,000 L, 256 t, 48 EU/t
 *                  (gregtech.loaders.postload.recipes.MixerRecipes)
 *   Super Coolant  1,000 L Hot Super Coolant -> 1,000 L, 200 t, EV
 *                  (gtnhlanth.loader.RecipeLoader, on the Vacuum Freezer)
 * </pre>
 *
 * That snapshot is exactly as stable as the rest of this addon: the GT dependency is pinned in
 * dependencies.gradle and D-06 makes bumping it a deliberate porting milestone. <b>Re-read both
 * stock recipes when that pin moves</b> - if GT changes them and these do not follow, the 8x claim
 * above quietly stops being true and becomes a balance hole.
 *
 * <h2>C-01 is deliberately not implemented</h2>
 *
 * S4.2 listed a "deionised makeup" rung: 16,000 L Distilled Water + 2 Ion Exchange Resin -> 16,000 L
 * Distilled Water, described in the spec itself as "a no-op rung, present so the NEI page shows the
 * bottom of the ladder". Written literally that is a recipe which consumes resin and returns its own
 * input - a trap that costs a player materials for nothing, and a loop an autocrafter can chase. Its
 * stated purpose is already served: {@code BTSDisplayRecipes.plantCoolant} is a real NEI page that
 * already shows all six rungs, Water and Distilled included, with their Q values. So the rung is
 * omitted rather than shipped as a bear trap.
 *
 * <h2>Explicitly out of scope</h2>
 *
 * A coolant RECOVERY loop (hot coolant out of the Spiral chamber, chilled, returned). It would be
 * excellent and it is how a real plant works, but it needs {@code MTEReactorCoreMk1}'s chamber drain
 * to emit a fluid, which is a reactor-behaviour change and not a recipe. Flagged for a future ruling.
 */
public final class BTSRecipesCoolant {

    /** Both bulk recipes take 8x stock in and give 8x stock out. One factor, stated once. */
    private static final int BULK = 8;
    /** 8,000 L fits a plain 8,000 L input hatch exactly - the reason BULK is 8 and not S4.2's 10. */
    private static final int BULK_LITRES = 1_000 * BULK;

    private BTSRecipesCoolant() {}

    public static void register() {
        chemicalShim();
        bulkIC2Coolant();
        bulkSuperCoolant();
    }

    // ------------------------------------------------------------------------------------------
    // C-04 CHEMICAL SHIM - Large Chemical Reactor, HV
    // A real PWR chemical shim: boric acid as the neutron-absorbing soluble poison, lithium
    // hydroxide to hold the pH up so the borate does not eat the loop. It is a COOLANT-MANUFACTURING
    // reagent consumed by C-02 and C-03 and is never fed to the reactor - the D-08 ladder is six
    // fluids and this is not a seventh.
    //
    // Registered straight to the LCR rather than through GTRecipeConstants.UniversalChemical:
    // UniversalChemical fans a recipe out to the single-block Chemical Reactor as well, and that map
    // is maxIO(2, 2, 1, 1) - one fluid input. This recipe has two, so the single-block half of that
    // fan-out could never have held it. The LCR is maxIO(6, 6, 6, 6) and is what S4.2 asked for.
    //
    // One batch covers 80,000 L of C-02 or 40,000 L of C-03, so the shim is a light tax on the bulk
    // routes and never the thing that gates them.
    // ------------------------------------------------------------------------------------------

    private static void chemicalShim() {
        BTSRecipeGuard.recipe(
            "C-04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lithium, 2L))
                .circuit(1)
                .fluidInputs(BTSRecipeGuard.fluid(GTPPFluids.BoricAcid, 2_000), Materials.Water.getFluid(1_000))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsPropellant.chemicalShim, 2_000))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(multiblockChemicalReactorRecipes));
    }

    // ------------------------------------------------------------------------------------------
    // C-02 IC2 COOLANT, BULK - Chemical Plant tier 2, EV
    // 8x the stock Mixer recipe's distilled-water variant, plus shim, in half the time 8 stock
    // cycles would take.
    // ------------------------------------------------------------------------------------------

    private static void bulkIC2Coolant() {
        // GTModHandler.getIC2Coolant answers NULL when IC2 is absent - the same trap
        // BTSDisplayRecipes already documents on the coolant ladder's rung 3. Without this guard
        // the whole of BTSRecipes.register() dies on an IC2-less pack, taking the fuel, material
        // and machine lines down with it over a coolant recipe nobody could have used anyway.
        final FluidStack coolant = GTModHandler.getIC2Coolant(BULK_LITRES);
        if (coolant == null) return;

        BTSRecipeGuard.recipe(
            "C-02",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lapis, BULK))
                .circuit(11)
                .fluidInputs(
                    GTModHandler.getDistilledWater(BULK_LITRES),
                    BTSRecipeGuard.fluid(BTSFluidsPropellant.chemicalShim, 200))
                .fluidOutputs(coolant)
                .duration(1_024 * TICKS) // half of what 8 stock cycles cost (8 x 256 t)
                .eut(TierEU.RECIPE_EV)
                .metadata(GTRecipeConstants.CHEMPLANT_CASING_TIER, 2)
                .addTo(GTPPRecipeMaps.chemicalPlantRecipes));
    }

    // ------------------------------------------------------------------------------------------
    // C-03 SUPER COOLANT, BULK - Vacuum Freezer, IV
    // The stock route is a straight chill of gtnhlanth's Hot Super Coolant. This is 8x of it with
    // the shim dosed into the charge before the chill, in half the time 8 stock cycles would take.
    // The map is maxIO(1, 1, 2, 1): two fluid inputs is exactly the ceiling, which is why the shim
    // is the only additive here and why there is no circuit.
    // ------------------------------------------------------------------------------------------

    private static void bulkSuperCoolant() {
        final FluidStack hot = WerkstoffMaterialPool.HotSuperCoolant.getFluidOrGas(BULK_LITRES);
        if (hot == null) return;

        BTSRecipeGuard.recipe(
            "C-03",
            () -> GTValues.RA.stdBuilder()
                .fluidInputs(hot, BTSRecipeGuard.fluid(BTSFluidsPropellant.chemicalShim, 400))
                .fluidOutputs(Materials.SuperCoolant.getFluid(BULK_LITRES))
                .duration(800 * TICKS) // half of what 8 stock cycles cost (8 x 200 t)
                .eut(TierEU.RECIPE_IV)
                .addTo(vacuumFreezerRecipes));
    }
}
