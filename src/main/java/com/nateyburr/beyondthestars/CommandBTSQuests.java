package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * /btsquests [status|install] - tells the player why the Beyond the Stars chapter is missing
 * from an existing world's quest book, and fixes it.
 * <p>
 * BetterQuesting copies {@code config/betterquesting/DefaultQuests/} into a world exactly
 * once, when that world is created; from then on the world reads only its own
 * {@code saves/<world>/betterquesting/QuestDatabase.json}. Merging our overlay into an
 * instance therefore lands the chapter in NEW worlds and nowhere else - which is exactly the
 * "the files are right but I still can't see it" situation this command exists for.
 */
public class CommandBTSQuests extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsquests";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsquests [status|install]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "status", "install");
        return null;
    }

    private static void tell(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final String sub = args.length > 0 ? args[0].toLowerCase() : "status";

        if (!BQBridge.isLoaded()) {
            tell(
                sender,
                EnumChatFormatting.RED
                    + "BetterQuesting is not loaded in this instance - there is no quest book to add to.");
            return;
        }

        if (sub.equals("install")) {
            install(sender);
            return;
        }
        status(sender);
    }

    // ------------------------------------------------------------------------- status

    private static void status(ICommandSender sender) {
        final BQBridge.Status status = BQBridge.status();

        tell(sender, EnumChatFormatting.AQUA + "== Beyond the Stars questbook ==");
        tell(sender, "BetterQuesting " + BQBridge.version() + " · defaults: " + BQBridge.defaultsDir());

        if (!status.readable) {
            tell(
                sender,
                EnumChatFormatting.RED + "Could not read this world's quest database ("
                    + status.error
                    + "). This BetterQuesting build is not the one the addon was written against; use the manual path below.");
        } else {
            tell(
                sender,
                "This world's database holds " + status.totalQuests + " quests in " + status.totalLines + " chapters.");
            tell(
                sender,
                line("Chapter \"Beyond the Stars\" (190/47990)", status.chapterPresent)
                    + line(
                        "  · its quests (190/" + BQBridge.chapterQuestLabel() + ")",
                        status.missingQuestLows.isEmpty())
                    + " "
                    + status.chapterQuests
                    + "/"
                    + status.expectedChapterQuests);
            if (!status.missingQuestLows.isEmpty()) {
                // Naming the ids is the whole point of holding a list: "20/25" only says that
                // something is wrong, "missing 190/48021, 48022 ..." says which quests to hunt.
                tell(sender, EnumChatFormatting.RED + "  · missing from this world: 190/" + status.missingLabel());
            }
            tell(
                sender,
                line("Entry quest \"Unit One\" (190/48020) in How to Generate Power", status.entryQuestPresent));
        }

        if (status.readable && status.complete()) {
            tell(
                sender,
                EnumChatFormatting.GREEN
                    + "Everything is loaded. Open the quest book: \"Unit One\" sits in How to Generate Power (after Standard Nuclear Fuel: Uranium), and the full campaign is the \"Beyond the Stars\" chapter.");
            tell(
                sender,
                EnumChatFormatting.GRAY
                    + "Still not visible? The book only redraws on a resync - run /bq_user refresh, or close and reopen it.");
            return;
        }

        tell(sender, EnumChatFormatting.YELLOW + "Not in this world yet. Why:");
        tell(
            sender,
            EnumChatFormatting.GRAY
                + "BetterQuesting reads config/betterquesting/DefaultQuests ONLY when a world is created. This world already has its own saves/<world>/betterquesting/QuestDatabase.json and never looks at the defaults again, so merging the overlay fixed new worlds and left this one alone.");
        tell(sender, EnumChatFormatting.WHITE + "Two ways to fix it:");
        tell(
            sender,
            EnumChatFormatting.GREEN + "  1) /btsquests install"
                + EnumChatFormatting.RESET
                + " - adds ONLY our chapter and our entry quest to this world's live database, keeps every other quest and all your progress, and resyncs your book on the spot. Preferred.");
        tell(
            sender,
            EnumChatFormatting.YELLOW + "  2) /bq_admin default load"
                + EnumChatFormatting.RESET
                + " - BetterQuesting's own command. It reloads the WHOLE DefaultQuests folder over this world's database.");
        tell(
            sender,
            EnumChatFormatting.GRAY
                + "     On BetterQuesting 3.8.x-GTNH that command snapshots quest progress and writes it back afterwards, so completed quests survive - but any quest you edited in-world reverts to the pack default, and progress on quests the defaults no longer contain is dropped. On older BetterQuesting builds it is a straight overwrite and progress does NOT survive. Back the world up first.");
        tell(
            sender,
            EnumChatFormatting.GRAY
                + "     If the overlay is not merged into the instance yet, neither option can work - see questbook/README.md.");
        tell(
            sender,
            EnumChatFormatting.GRAY
                + "BetterQuesting hands out its live databases through QuestingAPI, but ships no \"load one chapter into this world\" operation - option 1 is that operation, built from the databases' own methods.");
    }

    private static String line(String label, boolean present) {
        return (present ? EnumChatFormatting.GREEN + "[found] " : EnumChatFormatting.RED + "[missing] ")
            + EnumChatFormatting.RESET
            + label;
    }

    // ------------------------------------------------------------------------ install

    private static void install(ICommandSender sender) {
        final BQBridge.Install result = BQBridge.install();

        if (!result.ok) {
            tell(sender, EnumChatFormatting.RED + "Install failed: " + result.error);
            tell(
                sender,
                EnumChatFormatting.YELLOW
                    + "Nothing was changed. Fall back to /bq_admin default load (back the world up first) - see /btsquests status.");
            return;
        }

        tell(sender, EnumChatFormatting.AQUA + "== Beyond the Stars installed into this world ==");
        tell(sender, "Read from " + result.defaultsPath);
        tell(
            sender,
            "Quests: " + result.questsAdded
                + " added, "
                + result.questsUpdated
                + " refreshed. Chapter entries: "
                + result.chapterEntries
                + (result.chapterCreated ? " (chapter created)" : " (chapter refreshed)")
                + ". Grafted into pack chapters: "
                + result.graftedEntries
                + ".");
        for (final String note : result.notes) {
            tell(sender, EnumChatFormatting.YELLOW + "Note: " + note);
        }
        tell(
            sender,
            EnumChatFormatting.GREEN
                + "Progress was snapshotted before the write and restored after it - no completed quest was lost, and no pack quest was touched.");
        if (result.synced) {
            tell(
                sender,
                "Every open book was told to resync - reopen yours and look for the \"Beyond the Stars\" chapter, and \"Unit One\" in How to Generate Power.");
        }
        tell(
            sender,
            EnumChatFormatting.GRAY
                + "If a client still shows the old book, /bq_user refresh forces it. The world writes the new database on its next save.");

        final BQBridge.Status after = BQBridge.status();
        if (after.readable && !after.complete()) {
            tell(
                sender,
                EnumChatFormatting.RED + "Verification: still incomplete - chapter "
                    + (after.chapterPresent ? "ok" : "MISSING")
                    + ", quests "
                    + after.chapterQuests
                    + "/"
                    + after.expectedChapterQuests
                    + (after.missingQuestLows.isEmpty() ? "" : " (missing 190/" + after.missingLabel() + ")")
                    + ", entry quest "
                    + (after.entryQuestPresent ? "ok" : "MISSING")
                    + ". Check that questbook/DefaultQuests was merged into the instance.");
        } else if (after.readable) {
            tell(
                sender,
                EnumChatFormatting.GREEN + "Verification: chapter present, "
                    + after.chapterQuests
                    + "/"
                    + after.expectedChapterQuests
                    + " quests, entry quest present.");
        }
    }
}
