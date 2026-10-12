package com.nateyburr.beyondthestars;

import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.kerr.BlockKerrHole;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrMigration;
import com.nateyburr.beyondthestars.machines.kerr.KerrSlot;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.TileKerrHole;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * /btskerr [t1|t2] [force] - SINGULARITY LADDER P2 creative build: raises a complete, formed,
 * running Kerr: the Kerr Ergosphere Engine (hole only; every hatch, bolt buses stocked with a Black
 * Hole Seed and bolts, creative energy) AND both module machines (Kerr redesign: each its own
 * controller in its slot, its arc, its own hatches), a few blocks in front of the player, the KEE
 * controller facing the player and the cage stretching away from them. Owned by the Kerr unit.
 * <p>
 * {@code /btskerr migrate [x y z]} converts a pre-redesign Kerr (module core blocks in the arcs) into
 * KEE + module controllers (KerrMigration; the KEE also does this on its own once per load): the KEE
 * controller at x y z, else the nearest one within 128 blocks. Test bench: {@code /btskerr legacy x y z}
 * turns a built Kerr back into the pre-redesign layout so the migration can be exercised.
 * <p>
 * The cage is 65 blocks tall with the controller on its middle row, so the controller goes 32 blocks
 * above the player's feet and the cage's bottom row lands at foot level. The footprint is surveyed
 * first; more than 15% solid refuses to build without {@code force}.
 * <p>
 * <b>Console / command-block form:</b> {@code /btskerr [t1|t2] <x> <y> <z> [north|south|east|west]
 * [force] [survival]} puts the controller at x y z ("~" is relative to a command block) with its
 * front facing the given way (default north), loads the footprint's chunks first, and prints FORMED
 * or NOT FORMED with the first wrong cells, the mirror check and a re-projection check.
 * {@code survival} builds through the hologram projector's survival path instead of the creative one;
 * {@code check} builds nothing and re-walks the Kerr already standing there (e.g. after a restart).
 */
public class CommandBTSKerr extends CommandBase {

    /** Gap between the player and the controller's face. */
    private static final int STAND_OFF = 3;
    private static final double MAX_SOLID_FRACTION = 0.15;
    private static final int SURVEY_STRIDE = 4;

    @Override
    public String getCommandName() {
        return "btskerr";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btskerr [t1|t2] [force]  |  /btskerr migrate [x y z]  |  /btskerr render x y z [open [m]|close|off|on|break]  |  /btskerr module x y z <c|x> [stock|status|tier|clear]  |  /btskerr damage|repair x y z  |  console: /btskerr [t1|t2] <x> <y> <z> [facing] [force] [survival] | [check]  |  /btskerr autorig x y z [s] [bolts/s] [empty]  |  /btskerr ocloop x y z  |  /btskerr ctrl x y z on|off|out  |  /btskerr ocprobe x y z";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "t1", "t2", "migrate", "render");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "force");
        return null;
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // Test bench: with no player online the server stops ticking tile entities 1,200 ticks after the
        // last command-free minute; every /btskerr call keeps a console test's machines ticking.
        if (sender.getEntityWorld() instanceof WorldServer) {
            ((WorldServer) sender.getEntityWorld()).resetUpdateEntityTick();
        }
        if (args.length >= 4 && (args[0].equalsIgnoreCase("autorig") || args[0].equalsIgnoreCase("ocloop")
            || args[0].equalsIgnoreCase("ctrl")
            || args[0].equalsIgnoreCase("ocprobe"))) {
            automation(sender, args);
            return;
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("module")) {
            testBench(sender, args);
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("release")) {
            SLConsoleTest.tell(sender, "Kerr test bench: released " + KerrTestBench.release() + " chunk ticket(s)");
            return;
        }
        if (args.length >= 4 && (args[0].equalsIgnoreCase("damage") || args[0].equalsIgnoreCase("repair")
            || args[0].equalsIgnoreCase("keep"))) {
            testBench(sender, args);
            return;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("migrate") || args[0].equalsIgnoreCase("legacy"))) {
            migrate(sender, args);
            return;
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("render")) {
            render(sender, args);
            return;
        }
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        if (at != null) {
            processAt(sender, args, at);
            return;
        }
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;
        boolean tier2 = false;
        boolean force = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("t2")) tier2 = true; // no bare "2": it reads as an x coordinate
            if (arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("-f")) force = true;
        }
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        final int[] off = BTSShapes.offset(core);
        final int[] size = BTSShapes.size(core);
        final int below = size[1] - 1 - off[1];

        final ForgeDirection look = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        final ForgeDirection facing = look.getOpposite();
        final int px = MathHelper.floor_double(player.posX);
        final int py = MathHelper.floor_double(player.posY);
        final int pz = MathHelper.floor_double(player.posZ);
        final int x = px + look.offsetX * STAND_OFF;
        final int z = pz + look.offsetZ * STAND_OFF;
        final int y = py + below;
        if (y + off[1] > 255 || py < 1) {
            tell(
                player,
                EnumChatFormatting.RED + "No room: the cage is "
                    + size[1]
                    + " blocks tall and its bottom row sits at your feet. Stand between y=1 and y="
                    + (255 - off[1] - below)
                    + ".");
            return;
        }

        final ExtendedFacing alignment = ExtendedFacing.of(facing);
        if (!force) {
            final int percent = solidPercent(world, alignment, x, y, z, core);
            if (percent > MAX_SOLID_FRACTION * 100) {
                tell(
                    player,
                    EnumChatFormatting.RED + "Refusing to build: the "
                        + size[0]
                        + "x"
                        + size[1]
                        + "x"
                        + size[2]
                        + " footprint ahead of you is "
                        + percent
                        + "% solid and would be overwritten. Move to open ground (or the sky), or add "
                        + EnumChatFormatting.WHITE
                        + "force"
                        + EnumChatFormatting.RED
                        + ".");
                return;
            }
        }

        final KerrKit.Result result = KerrKit.build(world, x, y, z, facing, tier2);
        if (result.failure != null) {
            tell(player, EnumChatFormatting.RED + "Kerr build FAILED: " + result.failure + ".");
            return;
        }
        tell(player, EnumChatFormatting.AQUA + "== Kerr Ergosphere Engine (" + (tier2 ? "T2" : "T1") + ") ==");
        for (String line : result.lines) {
            tell(player, line);
        }
        tell(
            player,
            "The controller is " + below
                + " blocks above your feet on the cage's front (governor arc); the cage runs "
                + (size[2] + 8)
                + " blocks away from you.");
        tell(
            player,
            EnumChatFormatting.GREEN
                + "Creative energy hatches (UMV, 16 A) and an auto-maintenance hatch are fitted. Bolt buses hold a "
                + "Black Hole Seed and SpaceTime bolts: the hole opens within a second and both modules link. "
                + "Open the KEE's GUI for the hole (setpoint, Open / Collapse, modules); feed a module's own "
                + "input bus to run it.");
        if (!result.formed) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "Open the controller GUI: it names the first wrong cell. A NOT FORMED creative build is a bug - "
                    + "please report it (and check the orientation note in the build spec).");
        }
    }

    /** The console / command-block form: build at the given cell and facing, then report. */
    private void processAt(ICommandSender sender, String[] args, SLConsoleTest.Placement at) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        boolean tier2 = false;
        boolean force = false;
        boolean survival = false;
        boolean check = false;
        for (int i = 0; i < args.length; i++) {
            if (at.used[i]) continue;
            final String arg = args[i];
            if (arg.equalsIgnoreCase("t2")) tier2 = true; // no bare "2": it reads as an x coordinate
            if (arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("-f")) force = true;
            if (arg.equalsIgnoreCase("survival")) survival = true;
            if (arg.equalsIgnoreCase("check")) check = true;
        }
        final String core = tier2 ? MTEKerrEngine.PIECE_T2 : MTEKerrEngine.PIECE_T1;
        final ExtendedFacing alignment = ExtendedFacing.of(at.facing);
        final int[] box = KerrKit.assemblyBounds(at.facing, at.x, at.y, at.z, tier2);
        final String label = "Kerr " + (tier2 ? "T2" : "T1")
            + " facing "
            + at.facing.name()
                .toLowerCase();
        if (box[1] < 1 || box[4] > 255) {
            SLConsoleTest.tell(
                sender,
                label + ": NOT BUILT - the footprint spans y " + box[1] + ".." + box[4] + ", outside 1..255.");
            return;
        }
        final int chunks = SLConsoleTest.loadChunks(world, box);
        if (check) {
            for (String line : KerrKit.diagnose(world, at.x, at.y, at.z, at.facing, tier2, false)) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        if (!force) {
            final int percent = solidPercent(world, alignment, at.x, at.y, at.z, core);
            if (percent > MAX_SOLID_FRACTION * 100) {
                SLConsoleTest.tell(
                    sender,
                    label + ": NOT BUILT - the footprint is " + percent + "% solid; add force to overwrite.");
                return;
            }
        }
        final long start = System.nanoTime();
        final KerrKit.Result result = KerrKit.build(world, at.x, at.y, at.z, at.facing, tier2, survival);
        if (result.failure != null) {
            SLConsoleTest.tell(sender, label + ": BUILD FAILED - " + result.failure);
            return;
        }
        final long ms = (System.nanoTime() - start) / 1_000_000L;
        SLConsoleTest.tell(
            sender,
            "== " + label
                + (survival ? " (survival path)" : " (creative path)")
                + ": "
                + (result.formed ? "FORMED" : "NOT FORMED")
                + " == box ("
                + box[0]
                + ", "
                + box[1]
                + ", "
                + box[2]
                + ")..("
                + box[3]
                + ", "
                + box[4]
                + ", "
                + box[5]
                + "), "
                + chunks
                + " chunks loaded, built in "
                + ms
                + " ms");
        for (String line : result.lines) {
            SLConsoleTest.tell(sender, line);
        }
        for (String line : KerrKit.diagnose(world, at.x, at.y, at.z, at.facing, tier2, true)) {
            SLConsoleTest.tell(sender, line);
        }
    }

    /**
     * AUTOMATION PASS test bench: {@code /btskerr autorig x y z [seconds] [bolts/s]} runs the KEE on the Hole-set gate
     * rig (Directive Hatches fitted into free utility sockets first); {@code /btskerr ocloop x y z} makes the
     * OpenComputers calls a script would (machines/directive/DirectiveBench).
     */
    private void automation(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int x = (int) func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]);
        final int y = (int) func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]);
        final int z = (int) func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]);
        if (args[0].equalsIgnoreCase("ocprobe")) {
            // any block: what an OpenComputers Adapter against it would get (needs OpenComputers installed)
            if (!cpw.mods.fml.common.Loader.isModLoaded("OpenComputers")) {
                SLConsoleTest.tell(sender, "OpenComputers is not installed.");
                return;
            }
            for (String line : com.nateyburr.beyondthestars.integration.oc.BTSOpenComputers.probe(world, x, y, z)) {
                SLConsoleTest.tell(sender, line);
            }
            return;
        }
        final MTEKerrEngine kee = keeAt(world, x, y, z);
        if (kee == null) {
            SLConsoleTest.tell(sender, "No Kerr Ergosphere Engine controller at " + x + ", " + y + ", " + z);
            return;
        }
        final List<String> lines = args[0].equalsIgnoreCase("ctrl")
            ? com.nateyburr.beyondthestars.machines.directive.DirectiveBench
                .kerrCtrl(world, kee, args.length > 4 ? args[4].toLowerCase() : "")
            : args[0].equalsIgnoreCase("ocloop")
                ? com.nateyburr.beyondthestars.machines.directive.DirectiveBench.kerrOcLoop(kee)
                : com.nateyburr.beyondthestars.machines.directive.DirectiveBench.kerrAutorig(
                    world,
                    kee,
                    args.length > 4 ? Long.parseLong(args[4]) : 600,
                    args.length > 5 ? Integer.parseInt(args[5]) : 64,
                    args.length > 6 && args[6].equalsIgnoreCase("empty"));
        for (String line : lines) SLConsoleTest.tell(sender, line);
    }

    /**
     * {@code /btskerr migrate [x y z]}: runs KerrMigration on the KEE at x y z, else on the nearest KEE
     * controller within 128 blocks of the sender. Idempotent.
     */
    private void migrate(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        MTEKerrEngine kee = null;
        if (args.length >= 4) {
            final int x = (int) func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]);
            final int y = (int) func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]);
            final int z = (int) func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]);
            final TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEKerrEngine) {
                kee = (MTEKerrEngine) ((IGregTechTileEntity) te).getMetaTileEntity();
            }
        } else {
            final ChunkCoordinates c = sender.getPlayerCoordinates();
            double best = 128.0 * 128.0;
            for (Object o : world.loadedTileEntityList) {
                if (!(o instanceof IGregTechTileEntity)) continue;
                final IGregTechTileEntity te = (IGregTechTileEntity) o;
                if (!(te.getMetaTileEntity() instanceof MTEKerrEngine)) continue;
                final double dx = te.getXCoord() - c.posX;
                final double dy = te.getYCoord() - c.posY;
                final double dz = te.getZCoord() - c.posZ;
                final double d = dx * dx + dy * dy + dz * dz;
                if (d < best) {
                    best = d;
                    kee = (MTEKerrEngine) te.getMetaTileEntity();
                }
            }
        }
        if (kee == null) {
            SLConsoleTest.tell(sender, EnumChatFormatting.RED + "No Kerr Ergosphere Engine controller found there.");
            return;
        }
        if (args[0].equalsIgnoreCase("legacy")) {
            // Test bench: back to the pre-redesign layout, so the migration can be exercised.
            final boolean inflight = args.length >= 5 && args[4].equalsIgnoreCase("inflight");
            for (String line : KerrKit.makeLegacy(world, kee, inflight)) {
                SLConsoleTest.tell(sender, "Kerr legacy layout: " + line);
            }
            return;
        }
        final String note = KerrMigration.convert(kee);
        SLConsoleTest.tell(
            sender,
            note == null ? "Kerr migration: nothing to convert (no legacy module core block in its arcs)." : note);
        if (!kee.getMigrationNote()
            .isEmpty()) SLConsoleTest.tell(sender, "KEE note: " + kee.getMigrationNote());
    }

    /**
     * Test bench for the hole render (docs/kerr-redesign.md §5): {@code /btskerr render x y z
     * [open [m] | close | off | on | break]} acts on the KEE controller at x y z, then reports the hole,
     * both switches, what stands at the hole centre and every Kerr render block around x y z (an orphan
     * shows up as a block no KEE claims). Run it again a few seconds later to see the result of an action.
     */
    private void render(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int x = (int) func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]);
        final int y = (int) func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]);
        final int z = (int) func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]);
        final String action = args.length > 4 ? args[4].toLowerCase() : "";
        final TileEntity te = world.getTileEntity(x, y, z);
        final MTEKerrEngine kee = keeAt(world, x, y, z);
        if (kee != null) {
            switch (action) {
                case "open":
                    kee.debugOpenHole(args.length > 5 ? Double.parseDouble(args[5]) : 16.0);
                    break;
                case "close":
                    kee.debugCloseHole();
                    break;
                case "off":
                case "on":
                    kee.setRenderEnabled(action.equals("on"));
                    break;
                case "break":
                    final IGregTechTileEntity base = (IGregTechTileEntity) te;
                    if (base.getOwnerUuid() == null) {
                        base.setOwnerName("BTS console");
                        base.setOwnerUuid(UUID.nameUUIDFromBytes("BTS console test".getBytes()));
                    }
                    try {
                        world.setBlockToAir(x, y, z);
                    } catch (RuntimeException ownerless) {
                        SLConsoleTest.tell(sender, "Kerr render: GT threw while removing the controller: " + ownerless);
                    }
                    break;
                default:
                    break;
            }
        }
        final MTEKerrEngine now = keeAt(world, x, y, z);
        if (now == null) {
            SLConsoleTest.tell(
                sender,
                "Kerr render" + (action.isEmpty() ? "" : " [" + action + "]")
                    + ": no KEE at "
                    + x
                    + ", "
                    + y
                    + ", "
                    + z);
        } else {
            final int[] c = KerrSlot.worldPos(now.getExtendedFacing(), x, y, z, KerrSlot.centreRel());
            SLConsoleTest.tell(
                sender,
                "Kerr render" + (action.isEmpty() ? "" : " [" + action + "]")
                    + ": facing "
                    + now.getExtendedFacing()
                        .getDirection()
                    + ", hole "
                    + (now.isHoleOpen() ? "OPEN " + MTEKerrEngine.fmt(now.mass()) + " SM" : "closed")
                    + ", switch "
                    + (now.isRenderEnabled() ? "on" : "off")
                    + ", config "
                    + (Config.renderKerrHole ? "on" : "off")
                    + ", centre ("
                    + c[0]
                    + ", "
                    + c[1]
                    + ", "
                    + c[2]
                    + ") = "
                    + SLConsoleTest.blockAt(world, c[0], c[1], c[2])
                    + (now.claimsRender(c[0], c[1], c[2]) ? " (claimed)" : "")
                    + (now.getRenderNote()
                        .isEmpty() ? "" : ", note: " + now.getRenderNote()));
        }
        // Every Kerr render block within the cage's reach of x y z, in loaded chunks: more than the one
        // claimed at the centre is an orphan.
        int found = 0;
        final StringBuilder where = new StringBuilder();
        for (int bx = x - 100; bx <= x + 100; bx++) {
            for (int bz = z - 100; bz <= z + 100; bz++) {
                if (!world.blockExists(bx, 64, bz)) continue;
                for (int by = Math.max(0, y - 40); by <= Math.min(255, y + 40); by++) {
                    if (world.getBlock(bx, by, bz) != BlockKerrHole.INSTANCE) continue;
                    found++;
                    final TileEntity t = world.getTileEntity(bx, by, bz);
                    where.append(" (")
                        .append(bx)
                        .append(", ")
                        .append(by)
                        .append(", ")
                        .append(bz)
                        .append(t instanceof TileKerrHole ? " scale " + ((TileKerrHole) t).getKerrScale() : " NO TILE")
                        .append(")");
                }
            }
        }
        SLConsoleTest.tell(sender, "Kerr render blocks within 100 of the controller: " + found + where);
    }

    /**
     * Test bench: {@code /btskerr module x y z <c|x> [stock [map#] [longest|first|n] [copies] | status |
     * tier t [amps] | clear]} drives the module in the compressor / extractor slot of the KEE at x y z;
     * {@code /btskerr damage|repair x y z} unforms the KEE by one cage block and puts it back (KerrTestBench).
     */
    private void testBench(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int x = (int) func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]);
        final int y = (int) func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]);
        final int z = (int) func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]);
        final MTEKerrEngine kee = keeAt(world, x, y, z);
        if (kee == null) {
            SLConsoleTest.tell(sender, "Kerr test bench: no KEE at " + x + ", " + y + ", " + z);
            return;
        }
        final String verb = args[0].toLowerCase();
        if (verb.equals("damage")) {
            SLConsoleTest.tell(sender, "Kerr test bench: " + KerrTestBench.damage(world, kee));
            return;
        }
        if (verb.equals("keep")) {
            final boolean t2 = args.length > 4 && args[4].equalsIgnoreCase("t2");
            final int[] box = KerrKit.assemblyBounds(
                kee.getExtendedFacing()
                    .getDirection(),
                x,
                y,
                z,
                t2);
            SLConsoleTest.tell(
                sender,
                "Kerr test bench: forced " + KerrTestBench
                    .keep(world, box) + " chunk(s) around the Kerr at " + x + ", " + y + ", " + z);
            return;
        }
        if (verb.equals("repair")) {
            SLConsoleTest.tell(sender, "Kerr test bench: " + KerrTestBench.repair(world, kee));
            return;
        }
        final String which = args[4].toLowerCase();
        final KerrSlot slot = which.startsWith("c") ? KerrSlot.COMPRESSOR
            : which.startsWith("x") || which.startsWith("e") ? KerrSlot.EXTRACTOR : null;
        if (slot == null) {
            SLConsoleTest.tell(sender, "Kerr test bench: module must be c (compressor) or x (extractor)");
            return;
        }
        try {
            for (String line : KerrTestBench.module(world, kee, slot, args, 5)) {
                SLConsoleTest.tell(sender, "Kerr test bench: " + line);
            }
        } catch (RuntimeException bad) {
            SLConsoleTest.tell(sender, "Kerr test bench: failed - " + bad);
        }
    }

    private static MTEKerrEngine keeAt(World world, int x, int y, int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEKerrEngine
                ? (MTEKerrEngine) ((IGregTechTileEntity) te).getMetaTileEntity()
                : null;
    }

    /** Percentage of sampled cells of the piece's bounding box that are already solid. */
    private static int solidPercent(World world, ExtendedFacing alignment, int x, int y, int z, String core) {
        final int[] off = BTSShapes.offset(core);
        final int[] size = BTSShapes.size(core);
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int solid = 0;
        int total = 0;
        for (int a = 0; a < size[0]; a += SURVEY_STRIDE) {
            for (int b = 0; b < size[1]; b += SURVEY_STRIDE) {
                for (int c = 0; c < size[2] + 8; c += SURVEY_STRIDE) {
                    abc[0] = a - off[0];
                    abc[1] = b - off[1];
                    abc[2] = c - off[2];
                    alignment.getWorldOffset(abc, xyz);
                    final int wx = x + xyz[0];
                    final int wy = y + xyz[1];
                    final int wz = z + xyz[2];
                    if (wy < 0 || wy > 255) continue;
                    total++;
                    final Block block = world.getBlock(wx, wy, wz);
                    if (block == null || block.isAir(world, wx, wy, wz)) continue;
                    if (block.isReplaceable(world, wx, wy, wz)) continue;
                    solid++;
                }
            }
        }
        return total == 0 ? 0 : solid * 100 / total;
    }
}
