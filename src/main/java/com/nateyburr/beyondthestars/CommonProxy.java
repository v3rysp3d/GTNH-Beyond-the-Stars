package com.nateyburr.beyondthestars;

import net.minecraftforge.common.MinecraftForge;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.items.SLCircuitItems;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.items.SLUpgradeItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        BTSItems.register();
        SLItems.register();
        SLMaxItems.register();
        // B-83: the field-generator upgrade path's Retrofit Frame and Kits.
        SLUpgradeItems.register();
        SLQuasarItems.register();
        // Circuits v2 (circuits-v2.md §1): the 28 items, then their GT circuit ore names (preInit, before GT and
        // TecTech register the recipes that snapshot ore members).
        SLCircuitItems.register();
        SLCircuitItems.registerOreDict();
        BTSBlocks.register();
        BTSFluids.register();
        // GT5U registers its own machines in its preInit; required-after:gregtech guarantees ours runs later.
        BTSMachines.register();

        BeyondTheStars.LOG.info("GTNH: Beyond the Stars " + Tags.VERSION + " loading.");
    }

    public void init(FMLInitializationEvent event) {
        // AUTOMATION PASS: OpenComputers components, only with OpenComputers present (soft dependency).
        if (Loader.isModLoaded("OpenComputers")) {
            com.nateyburr.beyondthestars.integration.oc.BTSOpenComputers.register();
        }

        // Questbook: the op login line after an auto-install that skipped pack edits (FML bus), and the
        // progress backup of our quests on world save (Forge bus). One listener object, on both buses.
        final QuestbookEvents questbookEvents = new QuestbookEvents();
        FMLCommonHandler.instance()
            .bus()
            .register(questbookEvents);
        MinecraftForge.EVENT_BUS.register(questbookEvents);
    }

    public void postInit(FMLPostInitializationEvent event) {
        // GT registers its own recipes in postInit; required-after:gregtech orders ours after.
        BTSRecipes.register();
        // Circuits v2 §3.4: our Mainframes as computer-rack parts (after TecTech's MTEHatchRack.run, TecTech init).
        com.nateyburr.beyondthestars.compat.BTSRackComponents.register();
    }

    /**
     * SINGULARITY LADDER load-complete hook (singularity-ladder-build.md §4, Contract additions FC-4).
     * FML dispatches load-complete in dependency order, and the @Mod string says after:tectech and
     * after:dreamcraft, so TecTech's EOH storage is built and dreamcraft's assembly lines are
     * registered before anything here runs. Each line is one owner's entry point; the Eye unit's
     * conversion snapshot goes ABOVE the removals, which clear what it snapshots.
     */
    public void loadComplete(FMLLoadCompleteEvent event) {
        com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry.onLoadComplete();
        BTSRecipeRemovals.guard("TecTech Eye casings x32 (SL-B01/B02)", BTSRecipesBEC::onLoadComplete);
        // B-29 / Q4 / Q21 / B-83: GT's Resonance Chambers II-IV rewritten in place, then the generator upgrade path
        // derived from GT's generator recipes (both need GT's Condensate Assembler recipes, which exist only from
        // load-complete).
        BTSRecipeRemovals
            .guard("Resonance Chambers II-IV in place (SL-G01..G03)", BTSRecipesSingularity::onLoadComplete);
        // Circuits v2 §5: the NAC's Piko / Quantum / Raw Manifold Bud / Planck steps raised in place (the coremod's
        // matrix recipes exist by now; nothing has matched them yet).
        BTSRecipeRemovals.guard("Piko / Quantum / Planck raise (SL-G10..G13)", BTSStockCircuitRaise::onLoadComplete);
        // Circuits v2 §4.3: the coremod builds the NAC's real <-> fake conversions in its own load-complete pass; ours
        // are checked (missing ones added) or, with the NAC route off, the real -> fake ones removed.
        BTSRecipeRemovals.guard("NAC conversions of our parts and circuits", BTSRecipesNac::onLoadComplete);
        BTSRecipeRemovals.guard("Field-generator upgrade path (SL-U00..U48)", BTSRecipesGeneratorUpgrade::register);
        BTSRecipeRemovals.guard("Eternity on Ergomatter (SL-Q26, D-48)", BTSRecipesQuasar::onLoadComplete);
        BTSRecipeRemovals.run();
        BTSRecipeGuard.checkResearchCollisions();
    }

    public void serverStopping(cpw.mods.fml.common.event.FMLServerStoppingEvent event) {
        TimeWarp.serverStopping();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        BTSRecipeRemovals.lateBlackHoleCheck();
        event.registerServerCommand(new CommandBTSKit());
        event.registerServerCommand(new CommandBTSHR());
        event.registerServerCommand(new CommandBTSBuild());
        event.registerServerCommand(new CommandBTSMigrate());
        event.registerServerCommand(new CommandBTSClock());
        event.registerServerCommand(new CommandBTSSupply());
        event.registerServerCommand(new CommandBTSSafety());
        event.registerServerCommand(new CommandBTSQuests());
        event.registerServerCommand(new CommandBTSMilestone());
        event.registerServerCommand(new CommandBTSTree());
        event.registerServerCommand(new CommandBTSKerr());
        event.registerServerCommand(new CommandBTSEye());
        event.registerServerCommand(new CommandBTSRecipes());
        event.registerServerCommand(new CommandBTSQuasar());
        event.registerServerCommand(new CommandBTSConduit());
        // SMBH v1 (smbh-v1.md §10.4): the Supermassive Lattice's op-only bench
        event.registerServerCommand(new CommandBTSSmbh());
        // the time warp cheat (op only; docs/automation.md 4.2): off at every server start and stop
        TimeWarp.serverStarting();
        event.registerServerCommand(new CommandBTSWarp());
    }

    /**
     * QUESTBOOK (docs/questbook-integration.md §4.5): BetterQuesting loads the world's quest database (and copies
     * DefaultQuests into a NEW world) in its own FMLServerStartingEvent handler, so the auto-install runs one event
     * later, when that is complete. It does nothing unless the world differs from the jar's bundle.
     */
    public void serverStarted(FMLServerStartedEvent event) {
        QuestbookInstaller.onServerStarted();
    }
}
