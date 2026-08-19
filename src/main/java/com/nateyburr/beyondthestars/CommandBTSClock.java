package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * /btsclock [on|off] - toggles the bench clock on the nearest Unit One main station. With it
 * on the turbine takes one cycle a second with no redstone wiring (a test aid); with it off
 * the plant behaves as designed and only cycles on redstone edges at the Turbine Control
 * Block, so clock frequency is back under the player's control.
 */
public class CommandBTSClock extends CommandBase {

    private static final int SEARCH_RADIUS = 64;

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

        // Walk the loaded tile entities rather than probing half a million block positions.
        MTEReactorCoreMk1 nearest = null;
        double bestDist = SEARCH_RADIUS * (double) SEARCH_RADIUS;
        for (final Object o : new java.util.ArrayList<Object>(world.loadedTileEntityList)) {
            if (!(o instanceof IGregTechTileEntity)) continue;
            final IGregTechTileEntity gte = (IGregTechTileEntity) o;
            if (!(gte.getMetaTileEntity() instanceof MTEReactorCoreMk1)) continue;
            final TileEntity te = (TileEntity) o;
            final double d = player.getDistanceSq(te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (d < bestDist) {
                bestDist = d;
                nearest = (MTEReactorCoreMk1) gte.getMetaTileEntity();
            }
        }
        if (nearest == null) {
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "No Unit One main station within 64 blocks."));
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
