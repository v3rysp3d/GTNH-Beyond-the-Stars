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
    }
}
