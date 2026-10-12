package com.nateyburr.beyondthestars;

import net.minecraftforge.common.MinecraftForge;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import cpw.mods.fml.common.FMLCommonHandler;
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
        BTSBlocks.register();
        BTSFluids.register();
        // GT5U registers its own machines in its preInit; required-after:gregtech guarantees ours runs later.
        BTSMachines.register();

        BeyondTheStars.LOG.info("GTNH: Beyond the Stars " + Tags.VERSION + " loading.");
    }

    public void init(FMLInitializationEvent event) {
        // Questbook: the op login line after an auto-install that skipped pack edits (FML bus), and the
        // progress backup of our quests on world save (Forge bus). One listener object, on both buses.
        final QuestbookInstaller.Events questbookEvents = new QuestbookInstaller.Events();
        FMLCommonHandler.instance()
            .bus()
            .register(questbookEvents);
        MinecraftForge.EVENT_BUS.register(questbookEvents);
    }

    public void postInit(FMLPostInitializationEvent event) {
        // GT registers its own recipes in postInit; required-after:gregtech orders ours after.
        BTSRecipes.register();
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
        BTSRecipeRemovals.run();
        BTSRecipeGuard.checkResearchCollisions();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        BTSRecipeRemovals.lateBlackHoleCheck();
        event.registerServerCommand(new CommandBTSKit());
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
