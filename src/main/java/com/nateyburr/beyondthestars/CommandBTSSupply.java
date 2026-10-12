package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btssupply [on|off] - toggles INFINITE SUPPLIES on the nearest Nuclear Plant Foundation,
 * mirroring /btsclock. With it on the plant's consumables are refilled every tick - the silo's
 * Feedwater hatch, the Spiral chamber's own Coolant hatch and the best propellant in all
 * eight Precision Injectors - and every accepted cycle
 * gets a perfectly timed 1L shot per injector, so a commanded plant reaches the redline and
 * stays there indefinitely. With it off nothing is topped up and the plant runs on whatever
 * the player has plumbed; the interlock then holds the RPM down (coolant lost at speed near
 * SCRAM heat can still seize the Spine).
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
        final boolean on = BTSCommands.parseToggle(sender, this, args, nearest.isDebugInfiniteSupplies());
        nearest.setDebugInfiniteSupplies(on);
        if (!on) {
            BTSChat.warn(
                player,
                "Infinite supplies OFF for " + BTSCommands.plantAt(nearest)
                    + " - the plant now runs on what you plumb. Missing coolant or propellant holds the RPM down (the panels say why). Losing coolant at speed near SCRAM heat can still seize the Spine.");
            return;
        }
        final int[] filled = nearest.refillDebugSupplies();
        final FluidStack coolant = nearest.bestCoolant(1);
        final FluidStack propellant = MTEReactorCoreMk1.bestPropellant(1);
        BTSChat.ok(
            player,
            "Infinite supplies ON for " + BTSCommands.plantAt(nearest)
                + " - "
                + filled[0]
                + " feedwater hatches, "
                + filled[1]
                + " chamber hatches of "
                + (coolant == null ? "coolant" : coolant.getLocalizedName())
                + ", "
                + filled[2]
                + " injectors of "
                + (propellant == null ? "propellant" : propellant.getLocalizedName())
                + ", topped up every tick.");
    }
}
