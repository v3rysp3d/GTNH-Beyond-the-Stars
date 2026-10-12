package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        BTSItems.register();
        SLItems.register();
        SLMaxItems.register();
        SLQuasarItems.register();
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
        BTSRecipeRemovals.guard("TecTech Eye casings x32 (SL-B01/B02)", BTSRecipesBEC::onLoadComplete);
        BTSRecipeRemovals.guard("Eternity on Ergomatter (SL-Q26, D-48)", BTSRecipesQuasar::onLoadComplete);
        BTSRecipeRemovals.run();
        BTSRecipeGuard.checkResearchCollisions();
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
        event.registerServerCommand(new CommandBTSQuasar());
    }
}
