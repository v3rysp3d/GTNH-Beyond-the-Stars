package com.nateyburr.beyondthestars.client;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import com.nateyburr.beyondthestars.BTSConfigSync;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * B-227, client half of {@link BTSConfigSync}: once the player is in the world after a login packet, compare the
 * server's recipe settings with this client's config and, if they differ, say which keys in chat (and the log).
 * Public for FML's event bus.
 */
public final class BTSConfigSyncClient {

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new BTSConfigSyncClient());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final Map<String, String> server = BTSConfigSync.received;
        if (server == null) return;
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        BTSConfigSync.received = null;
        final List<String> diffs = BTSConfigSync.differences(server, Config.recipeFlags());
        if (diffs.isEmpty()) {
            BeyondTheStars.LOG.info("[BTS] The server's recipe settings match this client's config.");
            return;
        }
        BeyondTheStars.LOG.warn("[BTS] The server's recipe settings differ from this client's config: " + diffs);
        for (String line : BTSConfigSync.chatLines(diffs)) mc.thePlayer.addChatMessage(new ChatComponentText(line));
    }

    /** A packet from the last server must not be compared after joining the next one. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        BTSConfigSync.received = null;
    }
}
