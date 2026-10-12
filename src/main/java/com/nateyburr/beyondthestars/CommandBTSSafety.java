package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btssafety [on|off] - D-25: which mode the nearest Nuclear Plant Foundation's OVER-INJECTION
 * GUARD is in, mirroring /btsclock and /btssupply.
 * <p>
 * OFF (the default, and normal play) is GT parity: hand the manifold's hatch row more EU than
 * it can absorb and the plant explodes, exactly as {@code addEnergyOutputMultipleDynamos} would
 * have done. ON is the bench mode: the over-injection is clamped away, the plant keeps running,
 * and every panel prints what happened with the numbers - EU offered, summed hatch capacity,
 * the shortfall and which row came up short - plus one rate-limited line in the server log.
 * <p>
 * <b>Why this is not folded into /btssupply.</b> Bench supplies are an INPUT convenience; this
 * is a DESTRUCTION policy, and both directions of coupling break something real. Tie them
 * together and the armed path becomes untestable, because bench supplies are the only practical
 * way to drive output high enough to reach it and turning them on would disarm the very thing
 * you came to test. In the other direction, a player running a real supply chain who is nervous
 * about a mis-sized hatch row could not get the net without also cheating their inputs.
 */
public class CommandBTSSafety extends CommandBase {

    @Override
    public String getCommandName() {
        return "btssafety";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btssafety [on|off]";
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
        final boolean on = BTSCommands.parseToggle(sender, this, args, nearest.isDebugOutputSafety());
        // Changing mode clears the latched record; setting the mode it is already in keeps it,
        // so re-typing the command does not wipe the numbers somebody is reading.
        nearest.setDebugOutputSafety(on);
        if (on) {
            BTSChat.send(
                player,
                EnumChatFormatting.LIGHT_PURPLE + "Output safety ON (test mode) for "
                    + BTSCommands.plantAt(nearest)
                    + ": an over-injection is clamped and reported on the panels instead of exploding the plant. /btssafety off to re-arm.");
            return;
        }
        BTSChat.warn(
            player,
            "Output safety OFF for " + BTSCommands.plantAt(nearest)
                + ": armed, as in GT - over-injecting the hatch row explodes the plant. Any caught over-injection record was cleared.");
    }
}
