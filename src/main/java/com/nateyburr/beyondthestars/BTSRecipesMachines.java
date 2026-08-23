package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;

/**
 * D-26 PLANT HARDWARE - the controllers, hatches, control blocks, plant blocks and the turbine
 * rotor. <b>Owned exclusively by Agent D (Machines).</b>
 * <p>
 * Every block below was moved here verbatim by the split commit: not one ingredient changed.
 * These are the recipes for the plant own furniture, which S3 explicitly leaves alone - the
 * material lines are BULK routes to stock GT casings that sit beside GT recipes, and they live in
 * {@link BTSRecipesMaterials}, not here.
 * <p>
 * <b>The four new S5 multiblocks are built here too</b>, at the bottom of the file: the Site
 * Batching Plant at EV, and the Cascade Hall, the Fuel Fabrication Hall and the PUREX Canyon at
 * IV. Each is assembled out of the casing it is walled in, so the machine a player looks at is
 * the machine they paid for - and none of them costs a block that only the Batching Plant can
 * make, because every one of those keeps its stock GT recipe (S3's whole premise). There is no
 * bootstrap loop: the Batching Plant can always be built by hand first.
 */
public final class BTSRecipesMachines {

    private BTSRecipesMachines() {}

    /**
     * The bartworks bridge for Rhodium-Plated Palladium, which ten of this file's recipes spend.
     * <p>
     * It is resolved defensively because it is resolved ONCE, at the top of {@link #register()},
     * outside any single recipe - so a bartworks that has moved or gone would otherwise throw
     * where {@link BTSRecipeGuard#recipe(String, Runnable)} cannot see it and cost the whole
     * machines line, all fifteen recipes, rather than the ten that actually use it. Answering null
     * instead lets each of those ten fail on its own and the other five register normally.
     */
    private static Materials rhodiumPlatedPalladium() {
        try {
            return WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial();
        } catch (RuntimeException | LinkageError absent) {
            BeyondTheStars.LOG.error(
                "[BTS] bartworks' RhodiumPlatedPalladium could not be resolved. The ten machine "
                    + "recipes that spend it will be skipped and named in the summary; the rest of "
                    + "the machines line is unaffected.",
                absent);
            return null;
        }
    }

    public static void register() {
        final Materials rhPdPalladium = rhodiumPlatedPalladium();

        // Stainless structure with platinum lining -> reactor fuel column segments
        BTSRecipeGuard.recipe(
            "reactorFuelColumn",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.StainlessSteel, 4L),
                    BTSRecipeGuard.req(OrePrefixes.pipeMedium, Materials.StainlessSteel, 1L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Platinum, 2L))
                .circuit(3)
                .itemOutputs(new ItemStack(BTSBlocks.reactorFuelColumn, 2))
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // SD-2: Turbine Housing - the Spine's rotor socket (right-click sets spin direction)
        BTSRecipeGuard.recipe(
            "SD-2",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 4L),
                    BTSRecipeGuard.req(OrePrefixes.stickLong, Materials.TungstenSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 1L))
                .circuit(9)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(new ItemStack(BTSBlocks.turbineHousing, 2))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // SD-1: Rod Loading Port - the pillar's automation input
        BTSRecipeGuard.recipe(
            "SD-1",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSBlocks.reactorFuelColumn, 1),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.StainlessSteel, 2L),
                    BTSRecipeGuard.nn(ItemList.Robot_Arm_EV.get(1L), "ItemList.Robot_Arm_EV"))
                .circuit(8)
                .itemOutputs(new ItemStack(BTSBlocks.rodLoadingPort, 1))
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // SD-3: Spiral chamber hatches
        BTSRecipeGuard.recipe(
            "SD-3",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Electric_Pump_IV.get(1L), "ItemList.Electric_Pump_IV"),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 1L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 1L))
                .circuit(10)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.precisionInjector.copy())
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        BTSRecipeGuard.recipe(
            "spineSensor",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Cover_ActivityDetector.get(1L), "ItemList.Cover_ActivityDetector"),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.EV, 2L))
                .circuit(10)
                .itemOutputs(BTSMachines.spineSensor.copy())
                .duration(15 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // TC-2: the plant's control-block panels (dedicated sockets on silo face / gallery)
        BTSRecipeGuard.recipe(
            "TC-2",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Cover_Screen.get(1L), "ItemList.Cover_Screen"),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 2L))
                .circuit(11)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.siloControlBlock.copy())
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        BTSRecipeGuard.recipe(
            "turbineControlBlock",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Cover_Screen.get(1L), "ItemList.Cover_Screen"),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 2L))
                .circuit(12)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.turbineControlBlock.copy())
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // TC-4: the Turbine Rotor - the gallery stages' consumable heart
        BTSRecipeGuard.recipe(
            "TC-4",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 6L),
                    BTSRecipeGuard.req(OrePrefixes.stickLong, Materials.TungstenSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 1L))
                .circuit(13)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(new ItemStack(BTSItems.turbineRotor, 1))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // SD-4: the Turbine Hall and its fans are RETIRED - recipes removed, existing
        // items/machines keep working as museum pieces until a later breaking release.

        // Extraction Platform controller (EV): the campus fuel front end
        BTSRecipeGuard.recipe(
            "extractionPlatform",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.StainlessSteel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Steel, 4L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.EV, 2L))
                .circuit(4)
                .itemOutputs(BTSMachines.extractionPlatform.copy())
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // Enrichment Train controller (EV): the campus processing factory
        BTSRecipeGuard.recipe(
            "enrichmentTrain",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.StainlessSteel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.pipeMedium, Materials.StainlessSteel, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.EV, 2L))
                .circuit(5)
                .itemOutputs(BTSMachines.enrichmentTrain.copy())
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // A3: BOTH plant module controllers are RETIRED - the buildings are pieces of the main
        // station now, so neither has a recipe. The MTE ids stay registered (BTSMachines) so
        // worlds that already contain one load cleanly; both tooltips say they are museum
        // pieces and point at the Nuclear Plant Foundation.

        // Radiation proof casing + IV circuits + rare metals -> Reactor Silo controller
        BTSRecipeGuard.recipe(
            "reactorCoreMk1",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Casing_RadiationProof.get(1L), "ItemList.Casing_RadiationProof"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Lead, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.stick, Materials.Tungsten, 8L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Iridium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L))
                .circuit(4)
                .fluidInputs(Materials.Platinum.getMolten(4 * INGOTS))
                .itemOutputs(BTSMachines.reactorCoreMk1.copy())
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // ==== D-26 S5: THE FOUR NEW PRODUCTION MULTIBLOCKS ==================================
        // Same map and same shape as the controllers above - EV runs dry at 30s, IV takes one
        // ingot of molten platinum at 45s, and only the reactor itself costs more than that.
        // Checked against the pinned jar before writing rather than assumed: RecipeMaps.<clinit>
        // builds "gt.recipe.assembler" with maxIO(9, 1, 1, 0) and minInputs(1, 0), and
        // GTRecipeBuilder.circuit(int) APPENDS the integrated circuit into the item inputs (it
        // calls applyPendingCircuit on inputsBasic), so a programmed circuit spends one of the
        // nine slots. RecipeMapBackend.doAdd enforces only the MINIMUM - a recipe under
        // minItemInputs is dropped with handleInvalidRecipeLowItems and an empty return - but it
        // also drops any recipe that collides with one already registered, which is why these
        // four take fresh circuit numbers 14-17 rather than reusing any above. Widest is seven.

        // Site Batching Plant (EV): the M-01..M-06 host, and the FIRST of the four a player
        // builds - before the silo, in order to build the silo. Deliberately the cheapest of
        // them: structural steel and one EV circuit, nothing off the platinum line.
        BTSRecipeGuard.recipe(
            "M-01",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    Casings.SolidSteelMachineCasing.toStack(8),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Steel, 4L),
                    Casings.SteelPipeCasing.toStack(2),
                    BTSRecipeGuard.nn(ItemList.Conveyor_Module_EV.get(2L), "ItemList.Conveyor_Module_EV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.EV, 2L))
                .circuit(14)
                .itemOutputs(BTSMachines.siteBatchingPlant.copy())
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // Cascade Hall (IV): the gas centrifuge cascade of F-11/F-11b. Motors and long shafts -
        // the rotors are the machine - inside the tungstensteel turbine casing its stage banks
        // are walled and cored with.
        BTSRecipeGuard.recipe(
            "F-11",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    Casings.TungstensteelTurbineCasing.toStack(8),
                    Casings.TungstensteelPipeCasing.toStack(4),
                    BTSRecipeGuard.nn(ItemList.Electric_Motor_IV.get(4L), "ItemList.Electric_Motor_IV"),
                    BTSRecipeGuard.req(OrePrefixes.stickLong, Materials.TungstenSteel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 4L))
                .circuit(15)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.cascadeHall.copy())
                .duration(45 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // Fuel Fabrication Hall (IV): the rod and bundle line of F-17/F-18. Arms and conveyors,
        // because that is what a fabrication line is, in the clean stainless shell it starts at
        // tier 1 in, over the turbine-casing spine that runs down its centreline.
        BTSRecipeGuard.recipe(
            "F-17",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    Casings.CleanStainlessSteelMachineCasing.toStack(8),
                    Casings.TungstensteelTurbineCasing.toStack(4),
                    BTSRecipeGuard.nn(ItemList.Robot_Arm_IV.get(4L), "ItemList.Robot_Arm_IV"),
                    BTSRecipeGuard.nn(ItemList.Conveyor_Module_IV.get(4L), "ItemList.Conveyor_Module_IV"),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 4L))
                .circuit(16)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.fuelFabricationHall.copy())
                .duration(45 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));

        // PUREX Canyon (IV): the spent-fuel head end of R-02/R-03. Shielded the way the silo is,
        // radiation proof casing over dense lead, around the mixer-settler pumps and the piping
        // the U/Pu partition actually runs in.
        BTSRecipeGuard.recipe(
            "R-02",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(ItemList.Casing_RadiationProof.get(8L), "ItemList.Casing_RadiationProof"),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Lead, 4L),
                    BTSRecipeGuard.nn(ItemList.Electric_Pump_IV.get(4L), "ItemList.Electric_Pump_IV"),
                    BTSRecipeGuard.req(OrePrefixes.pipeMedium, Materials.StainlessSteel, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plate, rhPdPalladium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.IV, 4L))
                .circuit(17)
                .fluidInputs(Materials.Platinum.getMolten(1 * INGOTS))
                .itemOutputs(BTSMachines.purexCanyon.copy())
                .duration(45 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(assemblerRecipes));
    }
}
