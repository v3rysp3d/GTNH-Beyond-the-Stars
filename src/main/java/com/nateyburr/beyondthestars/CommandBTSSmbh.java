package com.nateyburr.beyondthestars;

import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.integration.QuasarComputerApi;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngineGui;
import com.nateyburr.beyondthestars.machines.quasar.SmbhBench;

/**
 * SMBH v1: {@code /btssmbh}, the Supermassive Lattice's op-only bench (smbh-v1.md §10.4; it ships like
 * {@code /btswarp}, item 32d). Nothing here is gameplay; every reply says what the script set.
 * <ul>
 * <li>{@code bench [x y z [facing]]}: an L4-lattice quasar (MagMatter coils, Saturation set), a 4,096 SM hole, every
 * upkeep hatch filled and a Mk IV crown on each jet port; without coordinates, in front of the player.</li>
 * <li>{@code mass <M> [x y z]}: the hole's mass, clamped to what the structure holds (§10.3); the mass setpoint
 * follows it.</li>
 * <li>{@code fill [x y z]}: the gas, MHDCSM, Universium and Primordial Matter hatches to capacity.</li>
 * <li>{@code drain anchor [x y z]}: the Primordial Matter hatches emptied (the anchor is cut).</li>
 * <li>{@code saturate on|off [x y z]}, {@code unbind on|off [x y z]}, {@code damage [x y z]} (one lattice frame
 * block removed, then a re-walk), {@code status [x y z]}.</li>
 * <li>{@code overdrive <o> [x y z]} and {@code setpoint <M> [x y z]}: the GUI's setters (the same clamps);
 * {@code unbind} goes through the GUI's checks too ({@code force} sets it raw); {@code shed [x y z]}: the GUI's
 * Shed.</li>
 * <li>{@code gui [info] [x y z]}: the controller GUI's header and rows (or its Info page) as text, built as the GUI
 * syncs them; {@code oc [x y z]}: what the OpenComputers SMBH getters return.</li>
 * </ul>
 * Without x y z the verbs act on the last bench quasar.
 */
public class CommandBTSSmbh extends CommandBase {

    private static final int STAND_OFF = 3;

    @Override
    public String getCommandName() {
        return "btssmbh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btssmbh bench [x y z [facing]] | mass <M> [x y z] | fill [x y z] | drain anchor [x y z]"
            + " | saturate on|off [x y z] | unbind on|off [force] [x y z] | damage [x y z] | status [x y z]"
            + " | overdrive <o> [x y z] | setpoint <M> [x y z] | shed [x y z] | gui [info] [x y z] | oc [x y z]"
            + "   (op only; a test bench, not gameplay)";
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
                "bench",
                "mass",
                "fill",
                "drain",
                "saturate",
                "unbind",
                "damage",
                "status",
                "overdrive",
                "setpoint",
                "shed",
                "gui",
                "oc");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("drain")) {
            return getListOfStringsMatchingLastWord(args, "anchor");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("saturate") || args[0].equalsIgnoreCase("unbind"))) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final World world = sender.getEntityWorld();
        if (world == null || world.isRemote) return;
        if (world instanceof WorldServer) ((WorldServer) world).resetUpdateEntityTick();
        if (args.length == 0) {
            SLConsoleTest.tell(sender, getCommandUsage(sender));
            return;
        }
        final String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("bench")) {
            bench(sender, world, args);
            return;
        }
        int from;
        switch (verb) {
            case "mass":
            case "saturate":
            case "unbind":
            case "drain":
            case "overdrive":
            case "setpoint":
                from = 2;
                break;
            case "gui":
                from = args.length > 1 && args[1].equalsIgnoreCase("info") ? 2 : 1;
                break;
            default:
                from = 1;
                break;
        }
        if (verb.equals("unbind") && args.length > 2 && args[2].equalsIgnoreCase("force")) from = 3;
        final int[] at = coords(sender, args, from);
        final MTEQuasarEngine q = SmbhBench.quasar(world, at);
        if (q == null) {
            SLConsoleTest.tell(
                sender,
                EnumChatFormatting.RED + (at == null ? "No bench quasar yet: give x y z, or run /btssmbh bench first"
                    : "No Quasar Engine controller at " + at[0] + ", " + at[1] + ", " + at[2]));
            return;
        }
        switch (verb) {
            case "mass": {
                if (args.length < 2) {
                    SLConsoleTest.tell(sender, getCommandUsage(sender));
                    return;
                }
                final double want = parseDouble(sender, args[1]);
                final double got = q.debugMass(want);
                // a bench mass is held there: the setpoint follows it, so the hole does not shed straight back
                if (got > com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.M_REF)
                    q.setMassSetpoint(got);
                SLConsoleTest.tell(
                    sender,
                    q.getMass() <= 0 ? "SMBH bench: no hole (seed one first)"
                        : "Set up by script: mass " + MTEQuasarEngine.fmt(got)
                            + " SM"
                            + (got == want ? ""
                                : " (clamped: this structure holds " + MTEQuasarEngine.fmt(q.getStructureTop())
                                    + " SM)"));
                break;
            }
            case "fill":
                for (String line : SmbhBench.fill(world, q)) SLConsoleTest.tell(sender, line);
                break;
            case "unbind": {
                final boolean on = args.length < 2 || !args[1].equalsIgnoreCase("off");
                if (from == 3) {
                    q.setUnbound(on);
                    SLConsoleTest.tell(
                        sender,
                        "Set up by script: Unbind " + (on ? "on" : "off") + " (forced; it counts on an L4 lattice)");
                } else {
                    final String why = q.unbind(on);
                    SLConsoleTest.tell(
                        sender,
                        "Unbind " + (on ? "on" : "off")
                            + " (the GUI's checks): "
                            + (why.isEmpty() ? "done" : EnumChatFormatting.RED + why));
                }
                break;
            }
            case "overdrive": {
                if (args.length < 2) {
                    SLConsoleTest.tell(sender, getCommandUsage(sender));
                    return;
                }
                q.setOverdrive(parseDouble(sender, args[1]));
                SLConsoleTest.tell(
                    sender,
                    String.format(
                        Locale.ROOT,
                        "Overdrive set to %.0f %% (the GUI's setter; it needs L3 and its upkeep)",
                        100 * q.getOverdrive()));
                break;
            }
            case "setpoint": {
                if (args.length < 2) {
                    SLConsoleTest.tell(sender, getCommandUsage(sender));
                    return;
                }
                q.setMassSetpoint(parseDouble(sender, args[1]));
                SLConsoleTest.tell(
                    sender,
                    "Mass setpoint (the GUI's setter): in force " + MTEQuasarEngine.fmt(q.getMassCap()) + " SM");
                break;
            }
            case "shed":
                SLConsoleTest.tell(sender, "Shed (the GUI's button): " + q.shedToSafe());
                break;
            case "gui":
                // the header and rows (or the Info page) the kit console shows, built as the GUI syncs them
                for (String line : new MTEQuasarEngineGui(q).benchDump(from == 2)) SLConsoleTest.tell(sender, line);
                return;
            case "oc": {
                final String[] names = { "getBand", "getIntake", "getHoldUp", "getBuffer", "getOverdrive", "getAnchor",
                    "getHorizon", "getBurst" };
                final Object[] maps = { QuasarComputerApi.band(q), QuasarComputerApi.intake(q),
                    QuasarComputerApi.holdUp(q), QuasarComputerApi.buffer(q), QuasarComputerApi.overdrive(q),
                    QuasarComputerApi.anchor(q), QuasarComputerApi.horizon(q), QuasarComputerApi.burst(q) };
                for (int i = 0; i < names.length; i++) SLConsoleTest.tell(sender, "oc " + names[i] + "() = " + maps[i]);
                return;
            }
            case "damage":
                for (String line : SmbhBench.damage(world, q)) SLConsoleTest.tell(sender, line);
                break;
            case "drain":
                if (args.length < 2 || !args[1].equalsIgnoreCase("anchor")) {
                    SLConsoleTest.tell(sender, getCommandUsage(sender));
                    return;
                }
                SLConsoleTest.tell(sender, SmbhBench.drainAnchor(world, q));
                break;
            case "saturate": {
                final boolean on = args.length < 2 || !args[1].equalsIgnoreCase("off");
                SLConsoleTest.tell(
                    sender,
                    "Set up by script: Saturation " + (on ? "set" : "cleared")
                        + ", lattice in force L"
                        + q.debugSaturate(on));
                break;
            }
            case "status":
                for (String line : q.statusLines()) SLConsoleTest.tell(sender, line);
                // the bench crowns' owner: what they pushed to wireless, a BigInteger (the no-overflow check, §3.4)
                SLConsoleTest.tell(
                    sender,
                    "Wireless network of the bench owner: " + String
                        .format(Locale.ROOT, "%,d", com.nateyburr.beyondthestars.machines.conduit.ConduitKit.benchEU())
                        + " EU");
                return;
            default:
                SLConsoleTest.tell(sender, getCommandUsage(sender));
                return;
        }
        for (String line : q.statusLines()) SLConsoleTest.tell(sender, line);
    }

    private void bench(ICommandSender sender, World world, String[] args) {
        final SLConsoleTest.Placement at = SLConsoleTest.parsePlacement(sender, args);
        final SLConsoleTest.Placement p;
        if (at != null) {
            p = at;
        } else {
            final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            final int[] off = BTSShapes.offset(MTEQuasarEngine.PIECE);
            final int[] size = BTSShapes.size(MTEQuasarEngine.PIECE);
            final ForgeDirection look = ForgeDirection.getOrientation(
                net.minecraft.util.Direction.directionToFacing[MathHelper
                    .floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3]);
            p = new SLConsoleTest.Placement();
            // the lattice reaches 18 blocks past the quasar's walls: stand the controller that much further off
            p.x = MathHelper.floor_double(player.posX) + look.offsetX * (STAND_OFF + 18);
            p.z = MathHelper.floor_double(player.posZ) + look.offsetZ * (STAND_OFF + 18);
            p.y = MathHelper.floor_double(player.posY) + size[1] - 1 - off[1];
            p.facing = look.getOpposite();
        }
        final long start = System.nanoTime();
        for (String line : SmbhBench.bench(world, p.x, p.y, p.z, p.facing)) SLConsoleTest.tell(sender, line);
        SLConsoleTest.tell(
            sender,
            "SMBH bench at " + p.x
                + ", "
                + p.y
                + ", "
                + p.z
                + " facing "
                + p.facing.name()
                    .toLowerCase(Locale.ROOT)
                + " in "
                + (System.nanoTime() - start) / 1_000_000L
                + " ms (set up by script: the L4 lattice placed by the bench kit)");
    }

    /** x y z at args[from..from+2] ("~" relative to the sender), or null when there are none. */
    private static int[] coords(ICommandSender sender, String[] args, int from) {
        if (args.length < from + 3) return null;
        for (int i = from; i < from + 3; i++) if (!SLConsoleTest.isCoord(args[i])) return null;
        return new int[] { (int) func_110666_a(sender, sender.getPlayerCoordinates().posX, args[from]),
            (int) func_110666_a(sender, sender.getPlayerCoordinates().posY, args[from + 1]),
            (int) func_110666_a(sender, sender.getPlayerCoordinates().posZ, args[from + 2]) };
    }
}
