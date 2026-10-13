package com.nateyburr.beyondthestars;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
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
 * <li>Persistence bench (B-112): {@code nbt x y z [key ...]} prints what any GT machine at x y z would save now (by
 * default our {@code bts*} keys and GT's run progress; read straight after a restart it is what the chunk held on
 * disk); {@code burstmode x y z [place|mode]} places a lone Burst Control Hatch (chunk force-loaded) and sets its mode
 * as the GUI does.</li>
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
            + " | fuel x y z on|off | junction x y z c k p3 [split|combine] | rebuild x y z [tier] | keep x1 y z1 x2 z2"
            + " | survival x y z mouth|mirror|junction|crown1|crown2|crown3 [facing|up|down] [creative]"
            + " | inject rx ry rz qx qy qz bu | inject rx ry rz off | nbt x y z [key ...] | burstmode x y z [place|mode]";
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
                world.getChunkFromBlockCoords(x, z);
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
            case "inject": {
                // inject rx ry rz qx qy qz <Bu/t> | inject rx ry rz off: a GRB Receptor Port receives a real quasar's
                // bursts every second (the host-side bench: Kerr, Homogeny, Reality Diffraction; P4 hosts unit)
                final com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor port = mte(
                    world,
                    x,
                    y,
                    z,
                    com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor.class);
                if (port == null) {
                    SLConsoleTest.tell(sender, "No GRB Receptor Port at " + x + ", " + y + ", " + z);
                    return;
                }
                if (args.length >= 5 && args[4].equalsIgnoreCase("off")) {
                    port.debugInject(null, 0);
                    SLConsoleTest.tell(sender, "Injection off at " + x + ", " + y + ", " + z);
                    return;
                }
                if (args.length < 8) break;
                final int qx = coord(sender, args[4], 0), qy = coord(sender, args[5], 1),
                    qz = coord(sender, args[6], 2);
                final MTEQuasarEngine q = mte(world, qx, qy, qz, MTEQuasarEngine.class);
                if (q == null) {
                    SLConsoleTest.tell(sender, "No Quasar Engine controller at " + qx + ", " + qy + ", " + qz);
                    return;
                }
                port.debugInject(q.source(), Double.parseDouble(args[7]));
                SLConsoleTest.tell(
                    sender,
                    "Injecting " + args[7]
                        + " Bu/t from the quasar at "
                        + qx
                        + ", "
                        + qy
                        + ", "
                        + qz
                        + " every second (linkable now: "
                        + q.linkable()
                        + ")");
                for (String line : port.statusLines()) SLConsoleTest.tell(sender, line);
                return;
            }
            case "survival": {
                // survival x y z <mouth|mirror|junction|crown1|crown2|crown3> [facing|up|down] [creative]: the
                // projector's survival path (or its creative construct) on a bare controller with that facing
                if (args.length < 5) break;
                ForgeDirection dir = ForgeDirection.NORTH;
                boolean creative = false;
                for (int i = 5; i < args.length; i++) {
                    final String a = args[i].toLowerCase(Locale.ROOT);
                    final ForgeDirection h = SLConsoleTest.parseFacing(a);
                    if (h != null) dir = h;
                    else if (a.equals("up")) dir = ForgeDirection.UP;
                    else if (a.equals("down")) dir = ForgeDirection.DOWN;
                    else if (a.equals("creative")) creative = true;
                }
                for (String line : ConduitKit.survivalTest(world, x, y, z, args[4], dir, creative)) {
                    SLConsoleTest.tell(sender, line);
                }
                return;
            }
            case "nbt": {
                // nbt x y z [key ...]: what the machine at x y z would save now (B-112 persistence bench). Read right
                // after a restart, before anything ticks it, it is what the chunk held on disk. Default keys: ours
                // (bts*) and GT's run progress.
                SLConsoleTest.loadChunks(world, new int[] { x, 0, z, x, 255, z }); // from disk if not loaded
                final TileEntity te = world.getTileEntity(x, y, z);
                if (!(te instanceof IGregTechTileEntity) || ((IGregTechTileEntity) te).getMetaTileEntity() == null) {
                    SLConsoleTest.tell(sender, "nbt: no GT machine at " + x + "," + y + "," + z);
                    return;
                }
                final IMetaTileEntity m = ((IGregTechTileEntity) te).getMetaTileEntity();
                final NBTTagCompound tag = new NBTTagCompound();
                m.saveNBTData(tag);
                final List<String> wanted = Arrays.asList(args)
                    .subList(4, args.length);
                final StringBuilder sb = new StringBuilder(
                    "nbt " + x
                        + ","
                        + y
                        + ","
                        + z
                        + " "
                        + m.getClass()
                            .getSimpleName()
                        + " t="
                        + world.getTotalWorldTime()
                        + ":");
                final java.util.TreeSet<String> keys = new java.util.TreeSet<>();
                for (Object k : tag.func_150296_c()) keys.add((String) k);
                for (String k : keys) {
                    final boolean pick = wanted.isEmpty()
                        ? k.startsWith("bts") || k.equals("mProgresstime") || k.equals("mMaxProgresstime")
                        : wanted.contains(k);
                    if (!pick) continue;
                    String v = tag.getTag(k)
                        .toString();
                    if (v.length() > 160) v = v.substring(0, 160) + "...";
                    sb.append(' ')
                        .append(k)
                        .append('=')
                        .append(v);
                }
                SLConsoleTest.tell(sender, sb.toString());
                return;
            }
            case "burstmode": {
                // burstmode x y z [place|<mode>]: a lone Burst Control Hatch (its chunk force-loaded) and its mode, set
                // the way the GUI sets it (B-112 persistence bench)
                if (args.length > 4 && args[4].equalsIgnoreCase("place")) {
                    com.nateyburr.beyondthestars.machines.kerr.KerrTestBench
                        .keep(world, new int[] { x, 0, z, x, 255, z });
                    SLConsoleTest.loadChunks(world, new int[] { x, 0, z, x, 255, z }); // setBlock needs it loaded
                    final boolean placed = com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1.placeGtMachine(
                        world,
                        x,
                        y,
                        z,
                        (short) com.nateyburr.beyondthestars.machines.BTSMachines.burstControlHatch.getItemDamage(),
                        ForgeDirection.NORTH);
                    SLConsoleTest.tell(
                        sender,
                        "burstmode: placed " + placed
                            + ", block "
                            + world.getBlock(x, y, z)
                                .getUnlocalizedName()
                            + ", id "
                            + com.nateyburr.beyondthestars.machines.BTSMachines.burstControlHatch.getItemDamage());
                }
                final com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl h = mte(
                    world,
                    x,
                    y,
                    z,
                    com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl.class);
                if (h == null) {
                    SLConsoleTest.tell(sender, "burstmode: no Burst Control Hatch at " + x + "," + y + "," + z);
                    return;
                }
                if (args.length > 4 && !args[4].equalsIgnoreCase("place")) {
                    final com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl.Mode mode = com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl.Mode
                        .parse(args[4]);
                    if (mode != null) h.setModeOrdinal(mode.ordinal());
                }
                SLConsoleTest.tell(sender, "burstmode " + x + "," + y + "," + z + ": " + h.getMode());
                return;
            }
            case "keep": {
                // keep x1 y z1 x2 z2: force-load a box of chunks (a whole rig after a restart)
                if (args.length < 6) break;
                final int x2 = coord(sender, args[4], 0), z2 = coord(sender, args[5], 2);
                final int n = com.nateyburr.beyondthestars.machines.kerr.KerrTestBench.keep(
                    world,
                    new int[] { Math.min(x, x2), 0, Math.min(z, z2), Math.max(x, x2), 255, Math.max(z, z2) });
                SLConsoleTest.tell(sender, "Conduit bench: forced " + n + " chunk(s)");
                return;
            }
            case "rebuild": {
                final MTEEnhancedMultiBlockBase<?> m = mte(world, x, y, z, MTEEnhancedMultiBlockBase.class);
                if (!(m instanceof com.gtnewhorizon.structurelib.alignment.constructable.IConstructable)) break;
                final IGregTechTileEntity base = m.getBaseMetaTileEntity();
                final ItemStack trigger = m.getStackForm(1);
                // the trigger's tier: arg 5 (quasar coil tier 1-4, crown Mk 1-3), else 3 for a quasar (never read
                // the tier off an unformed walk: it would rebuild the coils at tier 1)
                trigger.stackSize = args.length > 4 ? Integer.parseInt(args[4]) : m instanceof MTEQuasarEngine ? 3 : 1;
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
