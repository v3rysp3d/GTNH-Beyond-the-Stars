package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btsclock [on|off] - toggles the bench clock on the nearest Unit One main station. With it
 * on the turbine takes one cycle a second with no redstone wiring (a test aid); with it off
 * the plant behaves as designed and only cycles on redstone edges at the Turbine Control
 * Block, so clock frequency is back under the player's control.
 */
public class CommandBTSClock extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsclock";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsclock [on|off]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final MTEReactorCoreMk1 nearest = BTSCommands.nearestStation(player);
        if (nearest == null) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "No Unit One main station within "
                        + BTSCommands.SEARCH_RADIUS
                        + " blocks."));
            return;
        }
        final boolean on = args.length > 0 ? args[0].equalsIgnoreCase("on") : !nearest.isDebugAutoCycle();
        nearest.setDebugAutoCycle(on);
        player.addChatMessage(
            new ChatComponentText(
                on ? EnumChatFormatting.GREEN + "Bench clock ON - one turbine cycle per second, no redstone needed."
                    : EnumChatFormatting.YELLOW
                        + "Bench clock OFF - the turbine now only cycles on redstone edges at the Turbine Control Block."));
    }
}
