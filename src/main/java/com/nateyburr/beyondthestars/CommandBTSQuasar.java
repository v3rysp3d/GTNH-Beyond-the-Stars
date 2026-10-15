package com.nateyburr.beyondthestars;

import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.QuasarKit;
import com.nateyburr.beyondthestars.machines.quasar.QuasarRigBench;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * P4: {@code /btsquasar}, the Quasar Engine's creative build and test bench (quasar-p4.md §1).
 * <ul>
 * <li>SMBH v1: {@code l1..l4} adds the Supermassive Lattice at that node tier (on MagMatter coils), and
 * {@code saturate} sets the Saturation milestone so it counts at once (op test only; smbh-v1.md §1.1-§1.2).</li>
 * <li>{@code /btsquasar [t1..t4] [seed] [force]}: a formed quasar in front of the player (coils of that tier), its
 * controller facing the player, every standard hatch fitted (creative energy, Helium in four gas hatches); {@code seed}
 * opens the hole with a Quasar Seed.</li>
 * <li>Console form: {@code /btsquasar [t1..t4] <x> <y> <z> [facing] [force] [survival] [seed]} builds at x y z (the
 * footprint's chunks loaded first) and prints FORMED / NOT FORMED, the re-projection and the survival
 * damage-and-repair checks; {@code check} instead re-walks what stands there (e.g. after a restart).</li>
 * <li>Bench: {@code status | seed [M] [a] | open | collapse | purge | mass M | feed f | gas H|He|RSM | b v | run s
 * [fill] | trim g s | refill | redstone on|off | keep | release}, each with the controller's x y z.</li>
 * <li>P4 hosts unit: {@code autorig x y z <s> [eye ex ey ez] [analog] [cut k] [mass M] | autorig off | autorig
 * status} - the gate rig on this real quasar (and a real Eye) through their Directive and Burst Control Hatches
 * ({@link QuasarRigBench}); {@code ocloop x y z} - the bts_quasar calls a script makes, and the 20-tick rate
 * limit.</li>
 * </ul>
 */
public class CommandBTSQuasar extends CommandBase {

    private static final int STAND_OFF = 3;

    @Override
    public String getCommandName() {
        return "btsquasar";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsquasar [t1..t4] [l1..l4] [saturate] [seed] [force]  |  console: /btsquasar [t1..t4] [l1..l4] [saturate] <x> <y> <z> [facing] [force] [survival] [seed] | [check]"
            + "  |  /btsquasar <status|seed [M] [a]|open|collapse|purge|mass M|feed f|gas H|He|RSM|b v|run s [fill]|trim g s|refill|redstone on|off|keep|gui [info]> x y z  |  /btsquasar release  |  /btsquasar mestock x y z <fluids> <L> <AE>|status|sockets <piece> <c>"
            + "  |  /btsquasar autorig x y z <s> [eye ex ey ez] [analog] [cut k] [mass M] [mk 1-4] [limit bu|off|std] | autorig off|status  |  /btsquasar ocloop x y z";
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
                "t1",
                "t2",
                "t3",
                "t4",
                "l1",
                "l2",
                "l3",
                "l4",
                "saturate",
                "status",
                "seed",
                "open",
                "collapse",
                "purge",
                "mass",
                "feed",
                "gas",
                "b",
                "run",
                "trim",
                "refill",
                "redstone",
                "keep",
                "gui",
                "release");
        }
        return null;
    }

    private static final List<String> BENCH = java.util.Arrays.asList(
        "status",
        "seed",
        "open",
        "collapse",
        "purge",
        "mass",
        "feed",
        "gas",
        "b",
        "run",
        "trim",
        "refill",
        "redstone",
        "keep",
        "gui");

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // a console test with no player online: every call keeps the world's tile entities ticking
        if (sender.getEntityWorld() instanceof WorldServer) {
            ((WorldServer) sender.getEntityWorld()).resetUpdateEntityTick();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("autorig")
            && (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("status"))) {
            SLConsoleTest.tell(
                sender,
                args[1].equalsIgnoreCase("off") ? QuasarRigBench.off(sender.getEntityWorld())
                    : QuasarRigBench.status());
            return;
        }
        if (args.length >= 4 && (args[0].equalsIgnoreCase("autorig") || args[0].equalsIgnoreCase("ocloop"))) {
            automation(sender, args);
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("release")) {
            SLConsoleTest.tell(sender, "Quasar bench: released " + KerrTestBench.release() + " chunk ticket(s)");
            return;
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("mestock")) {
            // dev bench (QA-B02/B03/B07): an ME stocking hatch on a normal AE network in a private socket
            final World world = sender.getEntityWorld();
            final int x = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]));
            final int y = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]));
            final int z = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]));
            for (String line : com.nateyburr.beyondthestars.rendertest.MeStockBench
                .run(world, x, y, z, java.util.Arrays.copyOfRange(args, 4, args.length)))
                SLConsoleTest.tell(sender, line);
            return;
        }
        if (args.length >= 4 && BENCH.contains(args[0].toLowerCase(Locale.ROOT))) {
            bench(sender, args);
            return;
        }
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        int tier = 3, lattice = 0;
        boolean force = false, survival = false, seed = false, check = false, saturate = false;
        for (int i = 0; i < args.length; i++) {
            if (at != null && at.used[i]) continue;
            final String a = args[i].toLowerCase(Locale.ROOT);
            if (a.length() == 2 && a.charAt(0) == 't' && a.charAt(1) >= '1' && a.charAt(1) <= '4')
                tier = a.charAt(1) - '0';
            if (a.length() == 2 && a.charAt(0) == 'l' && a.charAt(1) >= '1' && a.charAt(1) <= '4')
                lattice = a.charAt(1) - '0';
            if (a.equals("saturate")) saturate = true;
            if (a.equals("force") || a.equals("-f")) force = true;
            if (a.equals("survival")) survival = true;
            if (a.equals("seed")) seed = true;
            if (a.equals("check")) check = true;
        }
        if (at != null) {
            buildAt(sender, at, tier, lattice, saturate, force, survival, seed, check);
            return;
        }
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        final int[] off = BTSShapes.offset(MTEQuasarEngine.PIECE);
        final int[] size = BTSShapes.size(MTEQuasarEngine.PIECE);
        final int below = size[1] - 1 - off[1];
        final ForgeDirection look = ForgeDirection.getOrientation(
            net.minecraft.util.Direction.directionToFacing[MathHelper
                .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
        final SLConsoleTest.Placement p = new SLConsoleTest.Placement();
        p.x = MathHelper.floor_double(player.posX) + look.offsetX * STAND_OFF;
        p.z = MathHelper.floor_double(player.posZ) + look.offsetZ * STAND_OFF;
        p.y = MathHelper.floor_double(player.posY) + below;
        p.facing = look.getOpposite();
        p.used = new boolean[0];
        if (world.isRemote) return;
        buildAt(sender, p, tier, lattice, saturate, force, survival, seed, check);
    }

    private void buildAt(ICommandSender sender, SLConsoleTest.Placement at, int tier, int lattice, boolean saturate,
        boolean force, boolean survival, boolean seed, boolean check) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int[] box = QuasarKit.bounds(at.facing, at.x, at.y, at.z, lattice > 0);
        final String label = "Quasar Engine (coils T" + (lattice > 0 ? 4 : tier)
            + (lattice > 0 ? ", Supermassive Lattice L" + lattice : "")
            + ") facing "
            + at.facing.name()
                .toLowerCase(Locale.ROOT);
        if (box[1] < 1 || box[4] > 255) {
            SLConsoleTest.tell(
                sender,
                label + ": NOT BUILT - the footprint spans y " + box[1] + ".." + box[4] + ", outside 1..255.");
            return;
        }
        final int chunks = SLConsoleTest.loadChunks(world, box);
        if (check) {
            for (String line : QuasarKit.diagnose(world, at.x, at.y, at.z, false)) SLConsoleTest.tell(sender, line);
            return;
        }
        if (!force) {
            final int solid = solidPercent(world, box);
            if (solid > 15) {
                SLConsoleTest.tell(
                    sender,
                    label + ": NOT BUILT - the footprint is " + solid + "% solid; add force to overwrite.");
                return;
            }
        }
        final long start = System.nanoTime();
        final QuasarKit.Result r = QuasarKit.build(world, at.x, at.y, at.z, at.facing, tier, lattice, survival, seed);
        if (r.failure != null) {
            SLConsoleTest.tell(sender, label + ": BUILD FAILED - " + r.failure);
            return;
        }
        if (saturate) {
            final MTEQuasarEngine q = QuasarKit.quasarAt(world, at.x, at.y, at.z);
            if (q != null) {
                final int inForce = q.debugSaturate(true);
                r.formed = q.isFormed();
                r.lines.add(
                    "Saturation set by command (op test): lattice in force L" + inForce
                        + ", the structure holds "
                        + MTEQuasarEngine.fmt(q.getStructureTop())
                        + " SM");
            }
        }
        SLConsoleTest.tell(
            sender,
            "== " + label
                + (survival ? " (survival path)" : " (creative path)")
                + ": "
                + (r.formed ? "FORMED" : "NOT FORMED")
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
                + " chunks loaded, "
                + (System.nanoTime() - start) / 1_000_000L
                + " ms");
        for (String line : r.lines) SLConsoleTest.tell(sender, line);
        for (String line : QuasarKit.diagnose(world, at.x, at.y, at.z, true)) SLConsoleTest.tell(sender, line);
    }

    /** Percentage of a coarse sample of the box that is already solid. */
    private static int solidPercent(World world, int[] box) {
        int solid = 0, total = 0;
        for (int x = box[0]; x <= box[3]; x += 6) {
            for (int y = Math.max(0, box[1]); y <= Math.min(255, box[4]); y += 6) {
                for (int z = box[2]; z <= box[5]; z += 6) {
                    total++;
                    if (!world.isAirBlock(x, y, z) && !world.getBlock(x, y, z)
                        .isReplaceable(world, x, y, z)) solid++;
                }
            }
        }
        return total == 0 ? 0 : solid * 100 / total;
    }

    /** The bench verbs, on the controller at args 1..3. */
    private void bench(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int x = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]));
        final int y = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]));
        final int z = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]));
        final String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("keep")) {
            // load the controller's chunk first, so its facing (and so the footprint) is known; the box includes the
            // Supermassive Lattice's whether one stands or not (the walk may not have run yet in this tick)
            world.getChunkFromBlockCoords(x, z);
            final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
            final ForgeDirection f = q == null ? ForgeDirection.NORTH
                : q.getExtendedFacing()
                    .getDirection();
            SLConsoleTest.tell(
                sender,
                "Quasar bench: forced " + KerrTestBench.keep(world, QuasarKit.bounds(f, x, y, z, true))
                    + " chunk(s) around the quasar at "
                    + x
                    + ", "
                    + y
                    + ", "
                    + z);
            return;
        }
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
        if (q == null) {
            SLConsoleTest
                .tell(sender, EnumChatFormatting.RED + "No Quasar Engine controller at " + x + ", " + y + ", " + z);
            return;
        }
        final String arg = args.length > 4 ? args[4] : "";
        if (verb.equals("gui")) {
            // B-116: the header and rows (or the Info page) the kit console shows, built as the GUI syncs them
            for (String line : new com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngineGui(q)
                .benchDump(arg.equalsIgnoreCase("info"))) SLConsoleTest.tell(sender, line);
            return;
        }
        switch (verb) {
            case "seed":
                if (arg.isEmpty()) {
                    SLConsoleTest.tell(sender, "Quasar bench: " + QuasarKit.seedFromSlot(q));
                } else {
                    final double want = Double.parseDouble(arg);
                    final double got = q.debugSeed(want, args.length > 5 ? Double.parseDouble(args[5]) : 0);
                    SLConsoleTest.tell(sender, "Quasar bench: seeded (debug, no item) at " + massNote(q, want, got));
                }
                break;
            case "open": {
                final String why = q.openHole();
                SLConsoleTest.tell(sender, "Quasar bench: open -> " + (why.isEmpty() ? "opened" : why));
                break;
            }
            case "collapse":
                SLConsoleTest.tell(sender, "Quasar bench: collapse -> " + (q.collapseHole() ? "dormant" : "no hole"));
                break;
            case "purge":
                SLConsoleTest.tell(
                    sender,
                    "Quasar bench: purge -> "
                        + (q.requestPurge() ? "requested (next second)" : "refused (flux under 20)"));
                break;
            case "mass": {
                final double want = Double.parseDouble(arg);
                final double got = q.debugMass(want);
                SLConsoleTest.tell(
                    sender,
                    q.getMass() <= 0 ? "Quasar bench: no hole (seed one first)"
                        : "Quasar bench: mass set to " + massNote(q, want, got));
                break;
            }
            case "feed":
                q.setFeed(Double.parseDouble(arg));
                SLConsoleTest.tell(sender, "Quasar bench: feed f " + q.getFeed());
                break;
            case "gas": {
                final QuasarGas g = QuasarGas.parse(arg);
                if (g != null) q.setGas(g);
                SLConsoleTest.tell(sender, "Quasar bench: gas " + q.getGas().displayName);
                break;
            }
            case "b":
                q.setBSetting(Double.parseDouble(arg));
                SLConsoleTest.tell(sender, "Quasar bench: b setting " + q.getBSetting());
                break;
            case "run": {
                final int seconds = arg.isEmpty() ? 60 : Integer.parseInt(arg);
                final boolean fill = args.length > 5 && args[5].equalsIgnoreCase("fill");
                if (q.refreshFrozen()) {
                    SLConsoleTest.tell(sender, "Quasar bench: FROZEN (not formed or a chunk unloaded): nothing runs");
                    break;
                }
                final long start = System.nanoTime();
                final QuasarResult r = q.benchRun(seconds, fill);
                SLConsoleTest.tell(
                    sender,
                    String.format(
                        Locale.ROOT,
                        "Quasar bench: ran %d s in %d ms; last second G %s (jet %.3f), zone %s%s%s",
                        seconds,
                        (System.nanoTime() - start) / 1_000_000L,
                        com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(r.G),
                        r.jet,
                        MTEQuasarEngine.zoneName(r.zone),
                        r.blowOut ? ", BLOW-OUT: " + r.blowOutReason : "",
                        r.reforming ? ", re-forming" : ""));
                break;
            }
            case "trim": {
                final int g = arg.isEmpty() ? 0 : Integer.parseInt(arg);
                final int s = args.length > 5 ? Integer.parseInt(args[5]) : 0;
                SLConsoleTest.tell(sender, "Quasar bench: " + QuasarKit.fillTrims(q, world, g, s));
                break;
            }
            case "refill":
                SLConsoleTest
                    .tell(sender, "Quasar bench: refilled " + QuasarKit.refillGas(q, world) + " gas hatch(es)");
                break;
            case "redstone":
                SLConsoleTest.tell(sender, "Quasar bench: " + redstone(world, q, !arg.equalsIgnoreCase("off")));
                break;
            default:
                break;
        }
        for (String line : q.statusLines()) SLConsoleTest.tell(sender, line);
    }

    /** P4 hosts unit: {@code autorig} (the gate rig on this quasar, and an Eye) and {@code ocloop}. */
    private void automation(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        final int x = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posX, args[1]));
        final int y = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posY, args[2]));
        final int z = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[3]));
        world.getChunkFromBlockCoords(x, z);
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
        if (q == null) {
            SLConsoleTest
                .tell(sender, EnumChatFormatting.RED + "No Quasar Engine controller at " + x + ", " + y + ", " + z);
            return;
        }
        if (args[0].equalsIgnoreCase("ocloop")) {
            for (String line : ocLoop(q)) SLConsoleTest.tell(sender, line);
            return;
        }
        long secs = args.length > 4 ? Long.parseLong(args[4]) : 600;
        com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS eye = null;
        boolean analog = false;
        long cut = 0;
        double mass = 0;
        final int[] mk = { 1 };
        final double[] limit = { com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxyBench.STANDARD };
        for (int i = 5; i < args.length; i++) {
            final String a = args[i].toLowerCase(Locale.ROOT);
            final int used = com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxyBench
                .parseRigOption(args, i, mk, limit);
            if (used >= i) {
                i = used;
                continue;
            }
            if (a.equals("analog")) analog = true;
            else if (a.equals("cut") && i + 1 < args.length) cut = Long.parseLong(args[++i]);
            else if (a.equals("mass") && i + 1 < args.length) mass = Double.parseDouble(args[++i]);
            else if (a.equals("eye") && i + 3 < args.length) {
                final int ex = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posX, args[++i]));
                final int ey = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posY, args[++i]));
                final int ez = (int) Math.floor(func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[++i]));
                world.getChunkFromBlockCoords(ex, ez);
                final net.minecraft.tileentity.TileEntity te = world.getTileEntity(ex, ey, ez);
                if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te)
                    .getMetaTileEntity() instanceof com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS) {
                    eye = (com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS) ((IGregTechTileEntity) te)
                        .getMetaTileEntity();
                } else {
                    SLConsoleTest.tell(sender, "No Eye of Harmony controller at " + ex + ", " + ey + ", " + ez);
                    return;
                }
            }
        }
        if (q.isFrozen()) {
            SLConsoleTest.tell(sender, "Quasar bench: FROZEN (not formed or a chunk unloaded): nothing runs");
            return;
        }
        for (String line : QuasarRigBench.autorig(world, q, secs, eye, analog, cut, mass, mk[0], limit[0])) {
            SLConsoleTest.tell(sender, line);
        }
    }

    /**
     * {@code ocloop}: the bts_quasar calls a script makes (through the same API the component wraps), the setters'
     * clamps, and the 20-tick rate limit (two setFeed calls in one tick: the second is refused).
     */
    private static List<String> ocLoop(MTEQuasarEngine q) {
        final List<String> out = new java.util.ArrayList<>();
        out.add("getDisc() = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.disc(q));
        out.add("getBurst() = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.burst(q));
        out.add("getLinks() = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.links(q));
        out.add("getMilestones() = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.milestones(q));
        out.add("getDirective(5) = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.directive(q, 5));
        final double feed = q.getFeed();
        final double b = q.getBSetting();
        for (double v : new double[] { 9, -1, 1.25 }) {
            out.add("setFeed(" + v + ") = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.setFeed(q, v));
        }
        for (double v : new double[] { 99, 0, 2.5 }) {
            out.add(
                "setField(" + v + ") = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.setField(q, v));
        }
        out.add("setGas(argon) = " + com.nateyburr.beyondthestars.integration.QuasarComputerApi.setGas(q, "argon"));
        out.add("trim(gas, 12) = " + q.requestTrim("gas", 12) + " (clamped to 10)");
        out.add("trim(argon, 1) = " + q.requestTrim("argon", 1));
        out.add("trim(space, 3) = " + q.requestTrim("space", 3));
        final com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult tr = q.benchRun(1, true);
        out.add(
            "  the next disc second took trims gas " + (tr == null ? 0 : tr.gasTrimUnits)
                + " / space "
                + (tr == null ? 0 : tr.spaceTrimUnits)
                + " units (space needs Spatially Enlarged Fluid in a gas socket)");
        q.setFeed(feed);
        q.setBSetting(b);
        out.add("(feed and b put back to " + feed + " / " + b + ")");
        final boolean first = com.nateyburr.beyondthestars.integration.BTSComputerApi.rateOk(q, "ocloop.setFeed");
        final boolean second = com.nateyburr.beyondthestars.integration.BTSComputerApi.rateOk(q, "ocloop.setFeed");
        out.add(
            "rate limit, two calls in one tick: first " + (first ? "accepted" : "refused")
                + ", second "
                + (second ? "accepted (WRONG)"
                    : "refused: " + com.nateyburr.beyondthestars.integration.BTSComputerApi.RATE_MESSAGE));
        out.add("purge() = " + q.requestPurge() + " (false below flux 20)");
        return out;
    }

    /**
     * A redstone block on the free face of the first Burst Control Hatch (on), or gone (off): a real rising edge
     * through the hatch's own redstone input, as a lever or an AE2 emitter would give.
     */
    private static String redstone(World world, MTEQuasarEngine q, boolean on) {
        for (MTEHatchBurstControl h : q.burstControlHatches()) {
            final IGregTechTileEntity base = h.getBaseMetaTileEntity();
            if (base == null) continue;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                final int nx = base.getXCoord() + d.offsetX;
                final int ny = base.getYCoord() + d.offsetY;
                final int nz = base.getZCoord() + d.offsetZ;
                if (on && world.isAirBlock(nx, ny, nz)) {
                    world.setBlock(nx, ny, nz, Blocks.redstone_block);
                    return "redstone block at " + nx
                        + ", "
                        + ny
                        + ", "
                        + nz
                        + " beside the Burst Control Hatch ("
                        + h.getMode().label
                        + ")";
                }
                if (!on && world.getBlock(nx, ny, nz) == Blocks.redstone_block) {
                    world.setBlockToAir(nx, ny, nz);
                    return "redstone block removed at " + nx + ", " + ny + ", " + nz;
                }
            }
        }
        return "no Burst Control Hatch with a free face";
    }

    /** "5,000 SM", or "4,096 SM (clamped: this structure holds 4,096 SM)" (smbh-v1.md §10.3). */
    private static String massNote(MTEQuasarEngine q, double want, double got) {
        final String m = MTEQuasarEngine.fmt(got) + " SM";
        return got == want ? m
            : m + " (clamped: this structure holds " + MTEQuasarEngine.fmt(q.getStructureTop()) + " SM)";
    }
}
