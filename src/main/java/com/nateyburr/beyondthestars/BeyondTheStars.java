package com.nateyburr.beyondthestars;

import net.minecraft.util.EnumChatFormatting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = BeyondTheStars.MODID,
    version = Tags.VERSION,
    name = "GTNH: Beyond the Stars",
    acceptedMinecraftVersions = "[1.7.10]",
    // GregTech 5.09.54.205 is GTNH 2.9.0-RC-2's: an older pack gets FML's clean "requires gregtech" screen instead of a
    // crash inside a GregTech class. after:etfuturum: BTSPackFixes reads Et Futurum's Blast Furnace in our preInit.
    dependencies = "required-after:gregtech@[5.09.54.205,);required-after:structurelib;after:dreamcraft;after:tectech;"
        + "after:OpenComputers;after:etfuturum")
public class BeyondTheStars {

    public static final String MODID = "beyondthestars";
    public static final Logger LOG = LogManager.getLogger(MODID);
    /**
     * The "Added by" credit every multiblock tooltip of this addon passes to
     * {@code MultiblockTooltipBuilder.toolTipFinisher(...)}, so they all sign off the same way.
     */
    public static final String TOOLTIP_AUTHOR = EnumChatFormatting.AQUA + "Beyond the Stars";

    @SidedProxy(
        clientSide = "com.nateyburr.beyondthestars.ClientProxy",
        serverSide = "com.nateyburr.beyondthestars.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(cpw.mods.fml.common.event.FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }
}
