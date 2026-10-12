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
    // SINGULARITY LADDER P2 (Kerr unit): the Kerr Ergosphere Engine, base+14.
    public static ItemStack kerrEngine;
    // KERR REDESIGN: the module machines, base+17 / base+18 (docs/kerr-redesign.md).
    public static ItemStack kerrCompressorModule;
    public static ItemStack kerrExtractorModule;
    // SINGULARITY LADDER P3 (Eye unit): the Eye of Harmony, base+15.
    public static ItemStack eyeOfHarmony;
    // D-49 (d49-build-plan.md §9): the Stellar Field Sensor Hatch, base+16.
    public static ItemStack stellarFieldSensor;
    // AUTOMATION PASS (brief item 18): the Directive Hatch, base+27 (P4's reserved id, one MTE for every host).
    public static ItemStack directiveHatch;
    // P4 (quasar-p4.md §6.1): the stacks the recipes make (BTSRecipesQuasar). Each MTE unit assigns its own in
    // register() below; until it does the field is null and its recipe waits (one "[BTS] P4 recipe ... waits" line).
    /** base+20, Quasar Engine (MTE 28120). */
    public static ItemStack quasarEngine;
    /** base+21, Wormhole Mouth (28121). */
    public static ItemStack wormholeMouth;
    /** base+22, Conduit Mirror (28122). */
    public static ItemStack conduitMirror;
    /** base+23, Conduit Junction (28123). */
    public static ItemStack conduitJunction;
    /** base+24, Burst Thermaliser Crown, Mk by structure (28124). */
    public static ItemStack burstThermaliserCrown;
    /** base+25, GRB Receptor Port hatch (28125). */
    public static ItemStack grbReceptorPort;
    /** base+26, Burst Control Hatch (28126). */
    public static ItemStack burstControlHatch;
    // H/R unit 2 (homogeny-reality.md): the Homogeny base+30, Reality Diffraction base+31, Reality Refab base+32.
    public static ItemStack homogeny;
    public static ItemStack realityDiffraction;
    public static ItemStack realityRefab;

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
        // LEDGER. base live; base+1 BURNED (0.5.0 Turbine Hall); base+2, base+3, base+6..base+9
        // live; base+4 and base+5 live-but-retired museum pieces; base+10..base+13 the four
        // below. NEXT FREE ID IS base+14, and there is room: gregtech.api.enums.MetaTileEntityIDs
        // in the pinned 5.09.52.594 jar holds 2029 ids for the whole pack, and its band runs out
        // at 28055 (CropsNHIndustrialFarm) and does not resume until 29990 (MAGLEV_PYLON_MV), so
        // 28056..29989 is empty and our 28100..28113 sits well inside it.
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

        // SINGULARITY LADDER ID BLOCK (singularity-ladder-build.md §3.4): base+14..base+39 =
        // 28114..28139 is reserved, contiguous, for the ladder. Frozen once shipped.
        // base+14 (28114) Kerr Ergosphere Engine multimachine.beyondthestars.kerrengine (P2)
        // base+15 (28115) Eye of Harmony multimachine.beyondthestars.eyeofharmony (P3)
        // base+16 (28116) Stellar Field Sensor Hatch hatch.beyondthestars.stellarfieldsensor (D-49)
        // base+17 (28117) Kerr Compressor Module, base+18 (28118) Kerr Extractor Module (Kerr redesign)
        // base+19 spare (the next Kerr module)
        // base+20..29 P4 (quasar-p4.md §6.1): Quasar Engine 20, Wormhole Mouth 21, Conduit Mirror 22,
        // Conduit Junction 23, Burst Thermaliser Crown 24 (one controller, Mk by structure), GRB Receptor Port 25,
        // Burst Control Hatch 26, Directive Hatch 27 (claimed by the automation pass: ONE hatch for the Eye, the Kerr
        // and the Quasar), spare 28-29
        // base+30..34 Homogeny 30, Reality Diffraction 31, Reality Refab 32 (H/R unit 2, ruling 16), spare 33-34
        // base+35..39 spare
        // The Kerr and Eye units each append their own single registration line below.
        kerrEngine = new com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine(
            base + 14,
            "multimachine.beyondthestars.kerrengine",
            "Kerr Ergosphere Engine").getStackForm(1);
        kerrCompressorModule = new com.nateyburr.beyondthestars.machines.kerr.MTEKerrCompressorModule(
            base + 17,
            "multimachine.beyondthestars.kerr.compressor",
            "Kerr Compressor Module").getStackForm(1);
        kerrExtractorModule = new com.nateyburr.beyondthestars.machines.kerr.MTEKerrExtractorModule(
            base + 18,
            "multimachine.beyondthestars.kerr.extractor",
            "Kerr Extractor Module").getStackForm(1);
        eyeOfHarmony = new com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS(
            base + 15,
            "multimachine.beyondthestars.eyeofharmony",
            "Eye of Harmony").getStackForm(1);
        // D-49: base+16 (28116), the first of the reserved P2/P3 spares, is the Stellar Field Sensor Hatch
        // hatch.beyondthestars.stellarfieldsensor (the Eye's n sockets). base+17 / base+18 are claimed by the
        // Kerr redesign's module controllers (Compressor 28117, Extractor 28118); base+19 is spare.
        stellarFieldSensor = new com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor(
            base + 16,
            "hatch.beyondthestars.stellarfieldsensor",
            "Stellar Field Sensor Hatch").getStackForm(1);
        // AUTOMATION PASS: base+27 (28127), the Directive Hatch (machines/directive), P4's reserved id. One MTE for the
        // Eye (Sensor sockets), the Kerr (utility sockets) and later the Quasar; its set is chosen in its GUI.
        directiveHatch = new com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective(
            base + 27,
            "hatch.beyondthestars.directive",
            "Directive Hatch").getStackForm(1);
        // P4 (quasar-p4.md §6.1): base+20 (28120) the Quasar Engine, base+26 (28126) the Burst Control Hatch. Each
        // assignment also lets its BTSRecipesQuasar recipe (SL-Q01, SL-Q20) register.
        quasarEngine = new com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine(
            base + 20,
            "multimachine.beyondthestars.quasarengine",
            "Quasar Engine").getStackForm(1);
        burstControlHatch = new com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl(
            base + 26,
            "hatch.beyondthestars.burstcontrol",
            "Burst Control Hatch").getStackForm(1);
        // P4 conduit + crowns unit (quasar-p4.md §2, §3): base+21..25. Each assignment lets its recipe (SL-Q10, Q11,
        // Q12,
        // Q16, Q19) register.
        wormholeMouth = new com.nateyburr.beyondthestars.machines.conduit.MTEWormholeMouth(
            base + 21,
            "multimachine.beyondthestars.wormholemouth",
            "Wormhole Mouth").getStackForm(1);
        conduitMirror = new com.nateyburr.beyondthestars.machines.conduit.MTEConduitMirror(
            base + 22,
            "multimachine.beyondthestars.conduitmirror",
            "Conduit Mirror").getStackForm(1);
        conduitJunction = new com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction(
            base + 23,
            "multimachine.beyondthestars.conduitjunction",
            "Conduit Junction").getStackForm(1);
        burstThermaliserCrown = new com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown(
            base + 24,
            "multimachine.beyondthestars.burstcrown",
            "Burst Thermaliser Crown").getStackForm(1);
        grbReceptorPort = new com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor(
            base + 25,
            "hatch.beyondthestars.grbreceptor",
            "GRB Receptor Port").getStackForm(1);
        // H/R unit 2 (ruling 16, homogeny-reality.md §4.1): the three machines unheld from the 28130..28134 block.
        homogeny = new com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny(
            base + 30,
            "multimachine.beyondthestars.homogeny",
            "The Homogeny").getStackForm(1);
        realityDiffraction = new com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction(
            base + 31,
            "multimachine.beyondthestars.realitydiffraction",
            "Reality Diffraction").getStackForm(1);
        realityRefab = new com.nateyburr.beyondthestars.machines.reality.MTERealityRefab(
            base + 32,
            "multimachine.beyondthestars.realityrefab",
            "Reality Refab").getStackForm(1);
    }
}
