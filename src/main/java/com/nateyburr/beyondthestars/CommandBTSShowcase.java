package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.nateyburr.beyondthestars.showcase.BTSShowcase;
import com.nateyburr.beyondthestars.showcase.ShowcaseHost;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * /btsshowcase: switches SHOWCASE MODE (designs/docs/showcase-worlds.md) on the Beyond the Stars controllers that
 * support it. A showcase machine feeds and steers itself, keeps its footprint loaded and survives restarts, for the
 * downloadable showcase world. Op only, and a test command (gone with enableTestCommands=false); a flag already set
 * keeps working either way, because it is saved in the machine.
 *
 * <ul>
 * <li>{@code /btsshowcase status} - the machines holding chunks, and the showcase machines near you;</li>
 * <li>{@code /btsshowcase on|off x y z} - the controller at x y z (works from the console);</li>
 * <li>{@code /btsshowcase all on|off [radius]} - every supporting controller in a loaded chunk within radius blocks
 * of you (default 1024), or in the whole overworld from the console.</li>
 * </ul>
 */
public class CommandBTSShowcase extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsshowcase";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsshowcase status  |  /btsshowcase on|off <x> <y> <z>  |  /btsshowcase all on|off [radius]"
            + "  |  /btsshowcase fluid <x> <y> <z> <fluid> [hatches]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "status", "on", "off", "all");
        if (args.length == 2 && "all".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "on", "off");
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));
        final String verb = args[0].toLowerCase(java.util.Locale.ROOT);
        final World world = sender.getEntityWorld() != null ? sender.getEntityWorld() : DimensionManager.getWorld(0);
        switch (verb) {
            case "status": {
                final List<String> held = BTSShowcase.heldLines();
                tell(
                    sender,
                    held.isEmpty() ? "No showcase machine holds chunks."
                        : held.size() + " showcase machine(s) hold chunks:");
                for (String l : held) tell(sender, "  " + l);
                int on = 0, off = 0;
                for (Found f : find(world, sender, 1024)) {
                    if (f.host.isShowcase()) on++;
                    else off++;
                }
                tell(sender, "Loaded controllers that support it: " + on + " on, " + off + " off.");
                return;
            }
            case "on":
            case "off": {
                if (args.length != 4) throw new WrongUsageException(getCommandUsage(sender));
                final int x = parseInt(sender, args[1]), y = parseInt(sender, args[2]), z = parseInt(sender, args[3]);
                final ShowcaseHost host = hostAt(world, x, y, z);
                if (host == null) {
                    tell(sender, "No controller that supports showcase mode at " + x + " " + y + " " + z + ".");
                    return;
                }
                host.setShowcase("on".equals(verb));
                tell(sender, host.showcaseName() + " at " + x + " " + y + " " + z + ": showcase " + verb + ".");
                return;
            }
            case "fluid": {
                // /btsshowcase fluid x y z <fluid> [n]: n (default 1) empty input hatches of that controller filled
                if (args.length < 5 || args.length > 6) throw new WrongUsageException(getCommandUsage(sender));
                final int x = parseInt(sender, args[1]), y = parseInt(sender, args[2]), z = parseInt(sender, args[3]);
                final net.minecraftforge.fluids.Fluid fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(args[4]);
                final int want = args.length == 6 ? parseIntWithMin(sender, args[5], 1) : 1;
                final TileEntity te = world.getTileEntity(x, y, z);
                final Object mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
                    : null;
                if (fluid == null || !(mte instanceof gregtech.api.metatileentity.implementations.MTEMultiBlockBase)) {
                    tell(sender, fluid == null ? "No fluid named " + args[4] + "." : "No multiblock controller there.");
                    return;
                }
                int n = 0;
                for (gregtech.api.metatileentity.implementations.MTEHatchInput h : ((gregtech.api.metatileentity.implementations.MTEMultiBlockBase) mte).mInputHatches) {
                    if (n >= want) break;
                    if (h == null || !h.isValid() || h.getFluid() != null) continue;
                    if (h.fill(new net.minecraftforge.fluids.FluidStack(fluid, Integer.MAX_VALUE), true) > 0) n++;
                }
                tell(sender, n + " empty input hatch(es) filled with " + fluid.getName() + ".");
                return;
            }
            case "all": {
                if (args.length < 2 || args.length > 3) throw new WrongUsageException(getCommandUsage(sender));
                final boolean on = BTSCommands.parseToggle(sender, this, new String[] { args[1] }, false);
                final int radius = args.length == 3 ? parseIntWithMin(sender, args[2], 1) : 1024;
                int n = 0;
                for (Found f : find(world, sender, radius)) {
                    f.host.setShowcase(on);
                    tell(sender, "  " + f.host.showcaseName() + " at " + f.x + " " + f.y + " " + f.z);
                    n++;
                }
                tell(sender, "Showcase " + (on ? "on" : "off") + " for " + n + " controller(s).");
                return;
            }
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private static final class Found {

        final ShowcaseHost host;
        final int x, y, z;

        Found(ShowcaseHost host, int x, int y, int z) {
            this.host = host;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Supporting controllers in loaded chunks; a player only gets those within {@code radius} blocks. */
    private static List<Found> find(World world, ICommandSender sender, int radius) {
        final List<Found> out = new ArrayList<>();
        final ChunkCoordinates at = sender.getPlayerCoordinates();
        final boolean console = sender.getEntityWorld() == null || at == null
            || sender instanceof net.minecraft.server.MinecraftServer;
        final long r2 = (long) radius * radius;
        for (Object o : new ArrayList<Object>(world.loadedTileEntityList)) {
            if (!(o instanceof IGregTechTileEntity)) continue;
            final Object mte = ((IGregTechTileEntity) o).getMetaTileEntity();
            if (!(mte instanceof ShowcaseHost)) continue;
            final TileEntity te = (TileEntity) o;
            if (!console) {
                final long dx = te.xCoord - at.posX, dz = te.zCoord - at.posZ;
                if (dx * dx + dz * dz > r2) continue;
            }
            out.add(new Found((ShowcaseHost) mte, te.xCoord, te.yCoord, te.zCoord));
        }
        return out;
    }

    private static ShowcaseHost hostAt(World world, int x, int y, int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof ShowcaseHost ? (ShowcaseHost) mte : null;
    }

    private static void tell(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText(line));
    }
}
