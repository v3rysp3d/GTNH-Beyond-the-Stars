package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        BTSItems.register();
        BTSBlocks.register();
        BTSFluids.register();
        // GT5U registers its own machines in its preInit; required-after:gregtech guarantees ours runs later.
        BTSMachines.register();

        BeyondTheStars.LOG.info("GTNH: Beyond the Stars " + Tags.VERSION + " loading.");
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        // GT registers its own recipes in postInit; required-after:gregtech orders ours after.
        BTSRecipes.register();
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
