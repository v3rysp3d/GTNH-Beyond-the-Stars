package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.progression.BTSMilestone;
import com.nateyburr.beyondthestars.progression.BTSProgression;
import com.nateyburr.beyondthestars.progression.BTSUpgrade;

/**
 * {@code /btsmilestone} - reads and writes the D-23 progression record on the nearest Nuclear
 * Plant Foundation. Permission level 2, like every other bench command in this addon.
 * <p>
 * Progress starts at ZERO on every plant by ruling - there is no back-fill for the campaign a
 * player has already run - so this command is how a tier is reached in under an hour for testing
 * instead of in twenty days.
 *
 * <pre>
 * /btsmilestone                      the four lines, their tiers and what they are paying
 * /btsmilestone set &lt;line&gt; &lt;n&gt;        put a raw counter value on one line
 * /btsmilestone tier &lt;line&gt; &lt;0-7&gt;     put one line exactly on a tier's threshold
 * /btsmilestone all &lt;0-7&gt;             put ALL FOUR lines on that tier
 * /btsmilestone reset                 the four COUNTERS back to zero - THE TREE IS UNTOUCHED
 * /btsmilestone reset tree            counters AND every purchased node: the whole campaign
 * /btsmilestone verify                NBT round-trip self-test, tile path AND wrench path
 * </pre>
 * <p>
 * <b>F1 - RESET NEVER TAKES THE TREE SILENTLY.</b> {@code reset} used to call
 * {@link BTSProgression#clear()}, which zeroes the 25-node purchase mask as well as the four
 * counters, while its message said only "all four lines back to zero" - so a player resetting
 * to re-test pacing lost every node they had bought and was never told. The two are separated
 * now: bare {@code reset} clears {@link BTSProgression#clearCounters() the counters only} and
 * names what it left standing, and the destructive form has to be asked for by name. Points
 * then read NEGATIVE until the tree is refunded, which is exactly what
 * {@code BTSProgression.pointsAvailable} documents as the honest answer.
 */
public class CommandBTSMilestone extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsmilestone";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsmilestone [set <line> <n> | tier <line> <0-7> | all <0-7> | reset [tree] | verify]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "set", "tier", "all", "reset", "verify");
        }
        if (args.length == 2 && "reset".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "tree");
        }
        if (args.length == 2 && ("set".equalsIgnoreCase(args[0]) || "tier".equalsIgnoreCase(args[0]))) {
            final List<String> names = new ArrayList<>();
            for (final BTSMilestone line : BTSMilestone.values()) {
                names.add(
                    line.name()
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
            BTSCommands.noStation(player);
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
            // F9: and the three operator settings that ride the same wrench. The throttle was
            // written by saveNBTData and forgotten by setItemNBT, which is a class of bug this
            // command existed to catch and could not, because the throttle is not part of the
            // record. It is checked directly now.
            final String settingFaults = station.verifyItemSettings();
            if (faults.isEmpty() && settingFaults.isEmpty()) {
                say(
                    player,
                    EnumChatFormatting.GREEN + "Round-trip OK on BOTH paths - every counter, every node "
                        + "and every off-default operator setting survived saveNBTData (reload) and "
                        + "setItemNBT (wrench).");
            } else {
                if (!faults.isEmpty()) say(player, EnumChatFormatting.RED + "ROUND-TRIP FAULT: " + faults);
                if (!settingFaults.isEmpty()) {
                    say(player, EnumChatFormatting.RED + "SETTING NOT CARRIED BY THE WRENCH: " + settingFaults);
                }
            }
            return;
        }
        if ("reset".equals(verb)) {
            // F1: the tree only goes when the player asks for it by name, and either way the
            // message names exactly what was taken and what was left standing.
            final boolean withTree = args.length > 1 && "tree".equalsIgnoreCase(args[1]);
            if (args.length > 1 && !withTree) {
                say(
                    player,
                    EnumChatFormatting.RED + "Unknown reset option '"
                        + args[1]
                        + "' - the only one is 'tree'. Nothing was changed.");
                return;
            }
            final int heldNodes = BTSUpgrade.owned(progression.nodes())
                .size();
            if (withTree) {
                progression.clear();
                dirty(station);
                say(
                    player,
                    EnumChatFormatting.YELLOW + "Whole campaign reset - all four counters back to zero AND all "
                        + heldNodes
                        + " purchased upgrade node(s) refunded.");
            } else {
                progression.clearCounters();
                dirty(station);
                say(player, EnumChatFormatting.YELLOW + "Progression reset - all four COUNTERS back to zero.");
                say(
                    player,
                    (heldNodes > 0 ? EnumChatFormatting.GOLD : EnumChatFormatting.GRAY)
                        + "The upgrade tree is UNTOUCHED - "
                        + heldNodes
                        + " node(s) still held"
                        + (heldNodes > 0
                            ? ", so points now read " + progression.pointsAvailable()
                                + ". Use /btsmilestone reset tree (or /btstree reset) to hand them back."
                            : ".")
                        + EnumChatFormatting.RESET);
            }
            report(player, station, progression);
            return;
        }
        if ("all".equals(verb)) {
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            final int tier = clampTier(parseInt(sender, args[1]));
            for (final BTSMilestone line : BTSMilestone.values()) {
                progression.set(line, line.threshold(tier));
            }
            dirty(station);
            say(player, EnumChatFormatting.GREEN + "All four lines set to tier " + tier + ".");
            report(player, station, progression);
            return;
        }
        if ("set".equals(verb) || "tier".equals(verb)) {
            if (args.length < 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            final BTSMilestone line = lineOf(args[1]);
            if (line == null) {
                say(
                    player,
                    EnumChatFormatting.RED + "Unknown line '"
                        + args[1]
                        + "' - one of "
                        + Arrays.toString(BTSMilestone.values()));
                return;
            }
            if ("tier".equals(verb)) {
                final int tier = clampTier(parseInt(sender, args[2]));
                progression.set(line, line.threshold(tier));
            } else {
                // F2: an unparseable counter is an ERROR, not a zero. "600k" and "60O000" used
                // to wipe a T7 line and then print it back in green as though it had worked.
                final Long value = parseCounter(args[2]);
                if (value == null) {
                    say(
                        player,
                        EnumChatFormatting.RED + "'"
                            + args[2]
                            + "' is not a whole number - "
                            + line.displayName()
                            + " was NOT changed (still "
                            + String.format("%,d", progression.get(line))
                            + " "
                            + line.unit()
                            + "). Digits only; ',' and '_' may be used as separators.");
                    return;
                }
                progression.set(line, Math.max(0L, value.longValue()));
            }
            dirty(station);
            say(player, EnumChatFormatting.GREEN + BTSMilestone.milestoneLine(line, progression.get(line)));
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    /** The four lines, the roll-up and the derived limits the unlocks are actually moving. */
    private static void report(EntityPlayerMP player, MTEReactorCoreMk1 station, BTSProgression progression) {
        final long[] progress = progression.snapshot();
        say(player, MTEReactorCoreMk1.header("MILESTONES"));
        for (final BTSMilestone line : BTSMilestone.values()) {
            say(player, BTSMilestone.milestoneLine(line, BTSProgression.counterOf(progress, line)));
        }
        say(player, BTSMilestone.milestoneSummaryLine(progress));
        say(player, BTSMilestone.milestoneUnlockLine(progress));
        // The limits themselves, read back off the plant rather than off the tables - if an
        // unlock ever failed to reach the code that uses it, this is the line that says so.
        say(
            player,
            EnumChatFormatting.GRAY + "Live limits: rods up to "
                + station.maxThrottle()
                + "/24 · redline "
                + String.format("%,d", station.getRedlineRpm())
                + " RPM set of "
                + String.format("%,d", station.getUnlockedRedlineRpm())
                + " unlocked · output term x"
                + String.format("%.3f", station.progOutputMilli() / 1000.0));
    }

    private static BTSMilestone lineOf(String name) {
        final String key = name.toLowerCase();
        for (final BTSMilestone line : BTSMilestone.values()) {
            final String full = line.name()
                .toLowerCase();
            if (full.equals(key) || full.startsWith(key)) return line;
        }
        // The four counters are also known by what they measure, which is what the panel prints.
        if ("uptime".startsWith(key) || "ticks".equals(key)) return BTSMilestone.UPTIME;
        if ("fuel".equals(key)) return BTSMilestone.BURNUP;
        if ("reproc".startsWith(key) || "ore".startsWith(key) || "modules".equals(key))
            return BTSMilestone.REPROCESSING;
        if ("perfect".startsWith(key) || "cycles".equals(key)) return BTSMilestone.TURBINE;
        return null;
    }

    /**
     * {@code CommandBase} has no long parser in 1.7.10, and these counters are longs.
     * <p>
     * F2: {@code null} - never {@code 0L} - for text that is not a number. Returning zero made
     * every typo a silent wipe of the line being edited, and the caller printed the zeroed line
     * in GREEN as a success; {@code tier} and {@code all} on this same command have always
     * validated (they go through {@code CommandBase.parseInt}), so this was an oversight and
     * not a style choice.
     */
    private static Long parseCounter(String raw) {
        try {
            return Long.valueOf(
                Long.parseLong(
                    raw.replace(",", "")
                        .replace("_", "")));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(BTSMilestone.MAX_TIER, tier));
    }

    /**
     * A commanded write has to reach the disk. GT tiles ride their chunk's save, so the chunk is
     * what has to be marked - otherwise "set it, quit, come back" reads the pre-command value
     * and the persistence test proves nothing.
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
