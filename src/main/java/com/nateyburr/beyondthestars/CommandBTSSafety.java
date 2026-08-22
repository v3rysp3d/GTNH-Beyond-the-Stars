package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btssafety [on|off] - D-25: which mode the nearest Unit One main station's OVER-INJECTION
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
        final boolean on = args.length > 0 ? args[0].equalsIgnoreCase("on") : !nearest.isDebugOutputSafety();
        // Changing mode clears the latched record; setting the mode it is already in keeps it,
        // so re-typing the command does not wipe the numbers somebody is reading.
        nearest.setDebugOutputSafety(on);
        if (on) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE
                        + "OUTPUT SAFETY ON (test mode) - an over-injection will NOT explode this plant. It is clamped away, flagged with its numbers on the GUI, both control blocks and the scanner, and logged server-side. This is a BENCH setting: /btssafety off to put the plant back in normal play."));
            return;
        }
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.YELLOW
                    + "Output safety OFF - the plant is ARMED and in normal play: over-injecting the hatch row explodes it, exactly as GregTech does. Any latched over-injection record has been cleared."));
    }
}
