package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btsclock [on|off] - toggles the bench clock on the nearest Nuclear Plant Foundation. With it
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
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return BTSCommands.toggleCompletions(args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final MTEReactorCoreMk1 nearest = BTSCommands.nearestStation(player);
        if (nearest == null) {
            BTSCommands.noStation(player);
            return;
        }
        final boolean on = BTSCommands.parseToggle(sender, this, args, nearest.isDebugAutoCycle());
        nearest.setDebugAutoCycle(on);
        if (on) {
            BTSChat.ok(
                player,
                "Bench clock ON for " + BTSCommands.plantAt(nearest)
                    + ": one turbine cycle per second, no redstone needed.");
        } else {
            BTSChat.warn(
                player,
                "Bench clock OFF for " + BTSCommands.plantAt(nearest)
                    + ": the turbine cycles only on redstone pulses at the Turbine Control Block.");
        }
    }
}
