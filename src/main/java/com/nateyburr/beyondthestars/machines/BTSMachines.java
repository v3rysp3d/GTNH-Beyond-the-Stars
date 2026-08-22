package com.nateyburr.beyondthestars.machines;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.Config;

public final class BTSMachines {

    public static ItemStack reactorCoreMk1;
    public static ItemStack extractionPlatform;
    public static ItemStack enrichmentTrain;
    public static ItemStack moduleTurbineHall;
    public static ItemStack moduleCoolingTower;
    public static ItemStack precisionInjector;
    public static ItemStack spineSensor;
    public static ItemStack siloControlBlock;
    public static ItemStack turbineControlBlock;
    // D-26 (docs/production-lines.md S5): the four new production multiblocks. Ids base+10,
    // base+11, base+12 and base+13. base+1 looks free and is not - see the ledger in register().
    public static ItemStack cascadeHall;
    public static ItemStack fuelFabricationHall;
    public static ItemStack purexCanyon;
    public static ItemStack siteBatchingPlant;

    private BTSMachines() {}

    public static void register() {
        final int base = Config.mteBaseId;
        reactorCoreMk1 = new MTEReactorCoreMk1(
            base,
            "multimachine.beyondthestars.reactorcore.mk1",
            "Nuclear Plant Foundation").getStackForm(1);
        extractionPlatform = new MTEExtractionPlatform(
            base + 2,
            "multimachine.beyondthestars.extractionplatform",
            "Extraction Platform").getStackForm(1);
        enrichmentTrain = new MTEEnrichmentTrain(
            base + 3,
            "multimachine.beyondthestars.enrichmenttrain",
            "Enrichment Train").getStackForm(1);
        moduleTurbineHall = new MTEModuleTurbineHall(
            base + 4,
            "multimachine.beyondthestars.module.turbinehall",
            "Plant Module - Turbine Hall").getStackForm(1);
        moduleCoolingTower = new MTEModuleCoolingTower(
            base + 5,
            "multimachine.beyondthestars.module.coolingtower",
            "Plant Module - Cooling Tower").getStackForm(1);
        precisionInjector = new MTEHatchPrecisionInjector(
            base + 6,
            "hatch.beyondthestars.precisioninjector",
            "Precision Injector").getStackForm(1);
        spineSensor = new MTEHatchSpineSensor(base + 7, "hatch.beyondthestars.spinesensor", "Spine Sensor")
            .getStackForm(1);
        siloControlBlock = new MTESiloControlBlock(
            base + 8,
            "hatch.beyondthestars.silocontrolblock",
            "Silo Control Block").getStackForm(1);
        turbineControlBlock = new MTETurbineControlBlock(
            base + 9,
            "hatch.beyondthestars.turbinecontrolblock",
            "Turbine Control Block").getStackForm(1);

        // D-26 THE FOUR NEW PRODUCTION MULTIBLOCKS. Ids are FROZEN once shipped: an MTE id is
        // what a placed machine in a saved world is bound to, so these four may never be
        // renumbered, only retired in place the way the two plant-module controllers were.
        //
        // ID LEDGER - base+1 IS BURNED, NOT FREE. The gap between base and base+2 is not an
        // unused id, it is a RETIRED one. `git show 0.5.0:src/main/java/com/nateyburr/
        // beyondthestars/machines/BTSMachines.java` registers MTETurbineHall(base + 1), and
        // 0.5.0 is a shipped tag. Commit d96e26a deleted that machine when the Turbine Hall
        // folded into the Reactor Silo - but deleting the class does not unbind the id, and a
        // world saved on 0.5.0 still has a Turbine Hall sitting at 28101. GT resolves a placed
        // machine by its id alone, so handing 28101 to a new controller would silently turn
        // every one of those into a Cascade Hall on load. The very rule that freezes these four
        // is what forbids reusing that one.
        //
        // Ids in use, and why: base live, base+1 BURNED (0.5.0 Turbine Hall), base+2..base+9
        // live, base+4/base+5 live-but-retired museum pieces. FIRST FREE ID IS base+13.
        cascadeHall = new MTECascadeHall(base + 13, "multimachine.beyondthestars.cascadehall", "Cascade Hall")
            .getStackForm(1);
        fuelFabricationHall = new MTEFuelFabricationHall(
            base + 10,
            "multimachine.beyondthestars.fuelfabricationhall",
            "Fuel Fabrication Hall").getStackForm(1);
        purexCanyon = new MTEPurexCanyon(base + 11, "multimachine.beyondthestars.purexcanyon", "PUREX Canyon")
            .getStackForm(1);
        siteBatchingPlant = new MTESiteBatchingPlant(
            base + 12,
            "multimachine.beyondthestars.sitebatchingplant",
            "Site Batching Plant").getStackForm(1);
    }
}
