package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.progression.BTSChannel;
import com.nateyburr.beyondthestars.progression.BTSProgression;
import com.nateyburr.beyondthestars.progression.BTSUpgrade;

/**
 * {@code /btstree} - reads and writes the D-24 upgrade-tree node set on the nearest Unit One main
 * station. Permission level 2, like every other bench command in this addon.
 * <p>
 * The node set is the half of the campaign that a wrench is most likely to eat, so this command
 * exists mainly to make the four survival cases testable in minutes: set a state, run
 * {@code verify} (which round-trips through BOTH writers), then quit/reload, fly out of the
 * chunk, wrench the controller, or break a casing, and run it again.
 *
 * <pre>
 * /btstree                        every node, its state, its cost and its effect
 * /btstree buy &lt;node&gt;             buy one, through the SAME server-side rules the GUI click uses
 * /btstree refund &lt;node&gt;          refund one, same rules
 * /btstree all                    debug: every node, ignoring points and gates
 * /btstree reset                  debug: refund everything, counters untouched
 * /btstree audit                  the cost sum self-check (must be 112) and the live channels
 * /btstree verify                 NBT round-trip self-test, tile path AND wrench path
 * </pre>
 */
public class CommandBTSTree extends CommandBase {

    @Override
    public String getCommandName() {
        return "btstree";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btstree [buy <node> | refund <node> | all | reset | audit | verify]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "buy", "refund", "all", "reset", "audit", "verify");
        }
        if (args.length == 2 && ("buy".equalsIgnoreCase(args[0]) || "refund".equalsIgnoreCase(args[0]))) {
            final List<String> names = new ArrayList<>();
            for (final BTSUpgrade node : BTSUpgrade.VALUES) {
                names.add(
                    node.name()
                        .toLowerCase());
            }
            return getListOfStringsMatchingLastWord(args, names.toArray(new String[0]));
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final MTEReactorCoreMk1 station = BTSCommands.nearestStation(player);
        if (station == null) {
            say(
                player,
                EnumChatFormatting.RED + "No Unit One main station within " + BTSCommands.SEARCH_RADIUS + " blocks.");
            return;
        }
        final BTSProgression progression = station.getProgression();

        if (args.length == 0) {
            report(player, station, progression);
            return;
        }

        final String verb = args[0].toLowerCase();
        if ("verify".equals(verb)) {
            final String faults = progression.verifyRoundTrip();
            if (faults.isEmpty()) {
                say(
                    player,
                    EnumChatFormatting.GREEN + "Round-trip OK on BOTH paths - every counter AND every purchased "
                        + "node survived saveNBTData (reload) and setItemNBT (wrench).");
            } else {
                say(player, EnumChatFormatting.RED + "ROUND-TRIP FAULT: " + faults);
            }
            return;
        }
        if ("audit".equals(verb)) {
            say(player, EnumChatFormatting.GRAY + BTSUpgrade.auditCosts());
            for (final BTSChannel channel : BTSChannel.values()) {
                final int total = BTSUpgrade.channel(progression.nodes(), channel);
                say(
                    player,
                    (total == channel.base() ? EnumChatFormatting.DARK_GRAY : EnumChatFormatting.GREEN) + channel.name()
                        + " = "
                        + total
                        + (total == channel.base() ? " (base, no effect)" : " (base " + channel.base() + ")"));
            }
            // Read the limits back OFF THE PLANT, not off the tables: if a channel ever failed
            // to reach the call site that consumes it, this is the line that says so.
            say(
                player,
                EnumChatFormatting.GRAY + "Live limits: throttle cap "
                    + station.maxThrottle()
                    + "/24 · redline "
                    + String.format("%,d", station.getRedlineRpm())
                    + " RPM set of "
                    + String.format("%,d", station.getUnlockedRedlineRpm())
                    + " unlocked · milestone term x"
                    + String.format("%.3f", station.progOutputMilli() / 1000.0)
                    + " · tree term x"
                    + String.format("%.3f", station.progUpgradeMilli() / 1000.0));
            return;
        }
        if ("all".equals(verb)) {
            progression.unlockAllNodes();
            dirty(station);
            say(player, EnumChatFormatting.YELLOW + "Every node unlocked (debug - points and gates ignored).");
            say(player, BTSUpgrade.treeSummaryLine(progression.nodes(), progression.snapshot()));
            say(player, BTSUpgrade.treeEffectLine(progression.nodes()));
            return;
        }
        if ("reset".equals(verb)) {
            progression.clearNodes();
            dirty(station);
            say(player, EnumChatFormatting.YELLOW + "Tree reset - every point handed back, counters untouched.");
            say(player, BTSUpgrade.treeSummaryLine(progression.nodes(), progression.snapshot()));
            return;
        }
        if ("buy".equals(verb) || "refund".equals(verb)) {
            if (args.length < 2) {
                say(player, EnumChatFormatting.RED + getCommandUsage(sender));
                return;
            }
            final BTSUpgrade node = nodeOf(args[1]);
            if (node == null) {
                say(player, EnumChatFormatting.RED + "Unknown node '" + args[1] + "'.");
                return;
            }
            // Deliberately the SAME entry points the GUI's server-side action calls, not a
            // back door: if a rule is wrong, it is wrong in one place and this command finds it.
            final boolean done = "buy".equals(verb) ? station.buyUpgrade(node) : station.refundUpgrade(node);
            if (!done) {
                say(
                    player,
                    EnumChatFormatting.RED + "Refused: "
                        + node.title()
                        + " - "
                        + ("buy".equals(verb) ? "prerequisites, milestone gate or points"
                            : "a held node hangs off it"));
            }
            say(player, BTSUpgrade.nodeLine(progression.nodes(), progression.snapshot(), node));
            say(player, BTSUpgrade.treeSummaryLine(progression.nodes(), progression.snapshot()));
            return;
        }
        say(player, EnumChatFormatting.RED + getCommandUsage(sender));
    }

    private static void report(EntityPlayerMP player, MTEReactorCoreMk1 station, BTSProgression progression) {
        final long mask = progression.nodes();
        final long[] counters = progression.snapshot();
        say(player, MTEReactorCoreMk1.header("UPGRADE TREE"));
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            say(player, BTSUpgrade.nodeLine(mask, counters, node));
        }
        say(player, BTSUpgrade.treeSummaryLine(mask, counters));
        say(player, BTSUpgrade.treeEffectLine(mask));
    }

    private static BTSUpgrade nodeOf(String raw) {
        final BTSUpgrade exact = BTSUpgrade.byName(raw.toUpperCase());
        if (exact != null) return exact;
        final String key = raw.toLowerCase();
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            if (node.name()
                .toLowerCase()
                .startsWith(key)) return node;
        }
        return null;
    }

    /**
     * A commanded write has to reach the disk. GT tiles ride their chunk's save, so the chunk is
     * what has to be marked - otherwise "set it, quit, come back" reads the pre-command value and
     * the persistence test proves nothing. (buy/refund mark it themselves.)
     */
    private static void dirty(MTEReactorCoreMk1 station) {
        final Object base = station.getBaseMetaTileEntity();
        if (base instanceof TileEntity) {
            ((TileEntity) base).markDirty();
        }
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
