package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btssupply [on|off] - toggles INFINITE SUPPLIES on the nearest Unit One main station,
 * mirroring /btsclock. With it on the plant's consumables are refilled every tick - feedwater
 * in the silo's face-panel hatches, the best coolant that exists in the Spiral chamber's own
 * hatch, and the best propellant in all eight Precision Injectors - and every accepted cycle
 * gets a perfectly timed 1L shot per injector, so a commanded plant reaches the redline and
 * stays there indefinitely. With it off nothing is topped up and the plant runs on whatever
 * the player has plumbed; the interlock then simply holds the RPM down instead of blowing up.
 */
public class CommandBTSSupply extends CommandBase {

    @Override
    public String getCommandName() {
        return "btssupply";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btssupply [on|off]";
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
        final boolean on = args.length > 0 ? args[0].equalsIgnoreCase("on") : !nearest.isDebugInfiniteSupplies();
        nearest.setDebugInfiniteSupplies(on);
        if (!on) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.YELLOW
                        + "Infinite supplies OFF - the plant now runs on what you plumb. Missing coolant or propellant just holds the RPM down (the panels say why); nothing seizes."));
            return;
        }
        final int[] filled = nearest.refillDebugSupplies();
        final FluidStack coolant = nearest.bestCoolant(1);
        final FluidStack propellant = MTEReactorCoreMk1.bestPropellant(1);
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GREEN + "Infinite supplies ON - "
                    + filled[0]
                    + " feedwater hatches, "
                    + filled[1]
                    + " chamber hatches of "
                    + (coolant == null ? "coolant" : coolant.getLocalizedName())
                    + ", "
                    + filled[2]
                    + " injectors of "
                    + (propellant == null ? "propellant" : propellant.getLocalizedName())
                    + ", topped up every tick."));
    }
}
