package com.nateyburr.beyondthestars;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * The questbook's two event hooks, registered on both FML's bus (login) and Forge's (world save).
 * PUBLIC on purpose: FML calls each handler through a generated class in another package, and a
 * non-public listener class fails with IllegalAccessError inside {@code save-all} (found on the
 * dev server, where it aborted every world-save listener after ours).
 */
public final class QuestbookEvents {

    /** Keeps {@code questbook-progress.dat} fresh: every player's progress on our quests. */
    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (event.world == null || event.world.isRemote || event.world.provider.dimensionId != 0) return;
        if (!BQBridge.isLoaded()) return;
        QuestbookInstaller.backupProgress();
    }

    /** Tells an op, once per login, that the last auto-install skipped pack edits or failed. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        final String notice = QuestbookInstaller.loginNotice();
        if (notice == null) return;
        final EntityPlayer player = event.player;
        if (player == null || !player.canCommandSenderUseCommand(2, "btsquests")) return;
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + notice));
    }
}
