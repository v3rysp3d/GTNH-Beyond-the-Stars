package com.nateyburr.beyondthestars;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.ConduitKit;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction;
import com.nateyburr.beyondthestars.machines.conduit.MTEWormholeMouth;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * P4: {@code /btsconduit}, the Einstein-Rosen Conduit's and the crowns' scanner and test bench (quasar-p4.md §2, §3).
 * <ul>
 * <li>{@code status x y z}: the scanner lines of a Mouth, Mirror, Junction, crown, GRB Receptor Port or quasar.</li>
 * <li>{@code dock x y z}: a quasar's two port cells and how a crown or a mouth docks on each.</li>
 * <li>{@code rig x y z}: raises the standard test rig around a quasar at x y z; {@code rig status} reads it all.</li>
 * <li>Bench: {@code pair x1 y1 z1 x2 y2 z2}, {@code throat x y z mk}, {@code fuel x y z on|off} (fills or empties an
 * element's fuel hatches), {@code junction x y z c k p3 [split|combine]}, {@code rebuild x y z} (re-projects any
 * of our multis at x y z).</li>
 * </ul>
 */
public class CommandBTSConduit extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsconduit";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsconduit status x y z | dock x y z | rig x y z | rig status | pair x1 y1 z1 x2 y2 z2 | throat x y z mk"
            + " | fuel x y z on|off | junction x y z c k p3 [split|combine] | rebuild x y z";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "status",
                "dock",
                "rig",
                "pair",
                "throat",
                "fuel",
                "junction",
                "rebuild");
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender.getEntityWorld() instanceof WorldServer) {
            ((WorldServer) sender.getEntityWorld()).resetUpdateEntityTick();
        }
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote || args.length == 0) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        final String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("rig") && args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            for (String line : ConduitKit.rigStatus()) SLConsoleTest.tell(sender, line);
            return;
        }
        if (args.length < 4) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        final int x = coord(sender, args[1], 0), y = coord(sender, args[2], 1), z = coord(sender, args[3], 2);
        switch (verb) {
            case "status":
                for (String line : ConduitKit.status(world, x, y, z)) SLConsoleTest.tell(sender, line);
                return;
            case "dock": {
                final MTEQuasarEngine q = mte(world, x, y, z, MTEQuasarEngine.class);
                if (q == null) {
                    SLConsoleTest.tell(sender, "No Quasar Engine controller at " + x + ", " + y + ", " + z);
                    return;
                }
                for (String line : ConduitKit.dockCells(q)) SLConsoleTest.tell(sender, line);
                return;
            }
            case "rig": {
                final long start = System.nanoTime();
                for (String line : ConduitKit.rig(world, x, y, z)) SLConsoleTest.tell(sender, line);
                SLConsoleTest.tell(sender, "Rig: " + (System.nanoTime() - start) / 1_000_000L + " ms");
                return;
            }
            case "pair": {
                if (args.length < 7) break;
                final MTEWormholeMouth a = mte(world, x, y, z, MTEWormholeMouth.class);
                final MTEWormholeMouth b = mte(
                    world,
                    coord(sender, args[4], 0),
                    coord(sender, args[5], 1),
                    coord(sender, args[6], 2),
                    MTEWormholeMouth.class);
                if (a == null || b == null) {
                    SLConsoleTest.tell(sender, "Two Wormhole Mouth controllers needed");
                    return;
                }
                MTEWormholeMouth.pair(a, b);
                SLConsoleTest.tell(sender, "Paired");
                return;
            }
            case "throat": {
                final MTEConduitElement<?> e = mte(world, x, y, z, MTEConduitElement.class);
                if (e == null || args.length < 5) break;
                final int mk = Integer.parseInt(args[4]);
                e.mInventory[e.getControllerSlotIndex()] = mk <= 0 ? null : SLQuasarItems.throat(mk);
                SLConsoleTest.tell(sender, "Throat set: Mk " + mk);
                for (String line : e.statusLines()) SLConsoleTest.tell(sender, line);
                return;
            }
            case "fuel": {
                final MTEConduitElement<?> e = mte(world, x, y, z, MTEConduitElement.class);
                if (e == null) break;
                final boolean on = args.length < 5 || !args[4].equalsIgnoreCase("off");
                int n = 0;
                for (MTEHatchInput h : e.fuelHatches()) {
                    if (on) {
                        if (MTEConduitElement.spaceTimeFluid() != null) {
                            h.fill(new FluidStack(MTEConduitElement.spaceTimeFluid(), Integer.MAX_VALUE), true);
                        }
                    } else {
                        h.setFillableStack(null);
                        h.setDrainableStack(null);
                    }
                    n++;
                }
                SLConsoleTest.tell(sender, "Fuel " + (on ? "filled" : "emptied") + " in " + n + " hatch(es)");
                return;
            }
            case "junction": {
                final MTEConduitJunction j = mte(world, x, y, z, MTEConduitJunction.class);
                if (j == null || args.length < 7) break;
                j.setRatio(Double.parseDouble(args[4]));
                j.setGain(Double.parseDouble(args[5]));
                j.setOut3Share(Double.parseDouble(args[6]));
                if (args.length > 7) {
                    j.setMode(
                        args[7].equalsIgnoreCase("combine") ? MTEConduitJunction.Mode.COMBINE
                            : MTEConduitJunction.Mode.SPLIT);
                }
                for (String line : j.statusLines()) SLConsoleTest.tell(sender, line);
                return;
            }
            case "rebuild": {
                final MTEEnhancedMultiBlockBase<?> m = mte(world, x, y, z, MTEEnhancedMultiBlockBase.class);
                if (!(m instanceof com.gtnewhorizon.structurelib.alignment.constructable.IConstructable)) break;
                final IGregTechTileEntity base = m.getBaseMetaTileEntity();
                final ItemStack trigger = m.getStackForm(1);
                trigger.stackSize = m instanceof MTEQuasarEngine ? Math.max(1, ((MTEQuasarEngine) m).getCoilTier()) : 1;
                ((com.gtnewhorizon.structurelib.alignment.constructable.IConstructable) m).construct(trigger, false);
                m.checkStructure(true, base);
                SLConsoleTest.tell(sender, "Re-projected: " + (m.mMachine ? "FORMED" : "NOT FORMED"));
                return;
            }
            default:
                break;
        }
        SLConsoleTest.tell(sender, getCommandUsage(sender) + "  (got: " + Arrays.toString(args) + ")");
    }

    private int coord(ICommandSender sender, String arg, int axis) {
        final double base = axis == 0 ? sender.getPlayerCoordinates().posX
            : axis == 1 ? sender.getPlayerCoordinates().posY : sender.getPlayerCoordinates().posZ;
        return (int) func_110666_a(sender, base, arg);
    }

    private static <T> T mte(World world, int x, int y, int z, Class<T> type) {
        if (y < 0 || y > 255) return null;
        world.getChunkFromBlockCoords(x, z);
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity m = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(m) ? type.cast(m) : null;
    }
}
