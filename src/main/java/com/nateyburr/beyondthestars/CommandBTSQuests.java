package com.nateyburr.beyondthestars;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * {@code /btsquests [status|install [force]|uninstall [all|edits]|verify|dumpids]} - the
 * Singularity Ladder questbook in this world (docs/questbook-integration.md §4.5).
 * <p>
 * BetterQuesting copies {@code config/betterquesting/DefaultQuests/} into a world exactly once,
 * when that world is created; from then on the world reads only its own database. The mod
 * therefore installs its chapters, and its edits to the pack's own quests, straight into the
 * live database ({@link QuestbookInstaller}), from the bundle inside the jar. The server does it
 * by itself on start whenever the jar changed ({@code questbookAutoInstall}); this command shows
 * what the world has and lets an op install, refresh or take it all out again.
 */
public class CommandBTSQuests extends CommandBase {

    private static final EnumChatFormatting OK = EnumChatFormatting.GREEN;
    private static final EnumChatFormatting WARN = EnumChatFormatting.YELLOW;
    private static final EnumChatFormatting BAD = EnumChatFormatting.RED;
    private static final EnumChatFormatting DIM = EnumChatFormatting.GRAY;
    private static final EnumChatFormatting HEAD = EnumChatFormatting.AQUA;

    @Override
    public String getCommandName() {
        return "btsquests";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsquests [status|install [force]|uninstall [all|edits]|verify|dumpids]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, "status", "install", "uninstall", "verify", "dumpids");
        if (args.length == 2 && "install".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "force");
        if (args.length == 2 && "uninstall".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "all", "edits");
        return null;
    }

    private static void tell(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        final String arg = args.length > 1 ? args[1].toLowerCase() : "";

        if (!BQBridge.isLoaded()) {
            tell(sender, BAD + "BetterQuesting is not loaded in this instance - there is no quest book to add to.");
            return;
        }
        switch (sub) {
            case "install":
                install(sender, "force".equals(arg));
                return;
            case "uninstall":
                if (!arg.isEmpty() && !"all".equals(arg) && !"edits".equals(arg)) {
                    tell(sender, BAD + "Usage: /btsquests uninstall [all|edits]");
                    return;
                }
                uninstall(sender, !"edits".equals(arg));
                return;
            case "verify":
                verify(sender);
                return;
            case "dumpids":
                dumpIds(sender);
                return;
            case "status":
                status(sender);
                return;
            default:
                tell(sender, BAD + "Usage: " + getCommandUsage(sender));
        }
    }

    // ------------------------------------------------------------------------- status

    private static void status(ICommandSender sender) {
        final QuestbookInstaller.Status s = QuestbookInstaller.status();
        tell(sender, HEAD + "== Beyond the Stars questbook ==");
        tell(
            sender,
            "BetterQuesting " + BQBridge.version()
                + " | jar bundle "
                + QuestbookInstaller.shortVersion(s.jarBundle)
                + " | this world "
                + (s.stateExists ? QuestbookInstaller.shortVersion(s.worldBundle) : "never installed"));
        if (!s.readable) {
            tell(sender, BAD + "Could not read this world's quest database (" + s.error + ").");
            return;
        }
        tell(sender, DIM + "This world's book: " + s.totalQuests + " quests in " + s.totalLines + " chapters.");
        if (!s.uninstalled.isEmpty()) {
            tell(
                sender,
                WARN + "An op ran /btsquests uninstall "
                    + s.uninstalled
                    + " here; the server will not reinstall until /btsquests install.");
        }

        for (final QuestbookInstaller.ChapterCount c : s.chapters) {
            final boolean good = c.linePresent && c.missing.isEmpty();
            tell(
                sender,
                (good ? OK + "[ok] " : BAD + "[missing] ") + EnumChatFormatting.RESET
                    + c.label
                    + DIM
                    + "  "
                    + c.present
                    + "/"
                    + c.expected
                    + (c.linePresent ? "" : ", chapter itself missing"));
            if (!c.missing.isEmpty()) tell(sender, BAD + "    missing 190/" + join(c.missing));
        }
        tell(
            sender,
            (s.entryQuestPresent ? OK + "[ok] " : BAD + "[missing] ") + EnumChatFormatting.RESET
                + "Beyond the Stars - The Nuclear Plant (190/48020) in How to Generate Power");

        tell(
            sender,
            "Pack edits: " + OK
                + s.editsApplied
                + " applied"
                + EnumChatFormatting.RESET
                + " of "
                + s.editsTotal
                + (s.skipped.isEmpty() ? "" : ", " + WARN + s.skipped.size() + " skipped" + EnumChatFormatting.RESET)
                + DIM
                + "  (questbookPackEdits="
                + Config.questbookPackEdits
                + ")");
        for (final Map.Entry<String, String> e : s.skipped.entrySet())
            tell(sender, WARN + "  " + e.getKey() + DIM + " skipped: " + e.getValue());
        for (final Map.Entry<String, Integer> e : s.disabled.entrySet())
            tell(sender, DIM + "  " + e.getValue() + " switched off: " + e.getKey());
        for (final Map.Entry<String, List<String>> e : s.misses.entrySet())
            for (final String m : e.getValue()) tell(sender, WARN + "  " + e.getKey() + DIM + " " + m);
        if (!s.editsChanged.isEmpty()) {
            tell(
                sender,
                WARN + "  Changed since the install (a /bq_admin default load?): "
                    + String.join(", ", s.editsChanged)
                    + ". /btsquests install puts them back.");
        }
        for (final String f : s.fallbacks) tell(sender, DIM + "  Fallback " + f);
        if (!s.placeholders.isEmpty()) {
            tell(
                sender,
                DIM + "Planned checkbox quests in this build ("
                    + s.placeholders.size()
                    + "): they become real quests, under the same id, when their content ships.");
        }
        tell(
            sender,
            DIM + "Auto-install on server start: "
                + (Config.questbookAutoInstall ? "on" : "off")
                + (s.configChanged ? WARN + " (config changed since the last install: it re-runs on next start)" : ""));

        final boolean upToDate = s.complete() && s.uninstalled.isEmpty()
            && s.editsChanged.isEmpty()
            && !s.configChanged
            && (s.jarBundle.isEmpty() || s.jarBundle.equals(s.worldBundle));
        if (upToDate) {
            tell(
                sender,
                OK + "Everything is in place. Not seeing it? The book redraws on a resync: /bq_user refresh, or reopen it.");
            if (!s.skipped.isEmpty()) tell(
                sender,
                WARN + ""
                    + s.skipped.size()
                    + " pack edit(s) did not apply because the pack's quest changed; those quests stay as the pack wrote them.");
        } else {
            tell(
                sender,
                WARN + "Not up to date. "
                    + EnumChatFormatting.RESET
                    + "/btsquests install"
                    + DIM
                    + " brings this world in line with the jar. It keeps everyone's progress; a quest whose tasks"
                    + " changed keeps its completion and starts its tasks fresh.");
        }
    }

    // ------------------------------------------------------------------------ install

    private static void install(ICommandSender sender, boolean force) {
        final QuestbookInstaller.Report r = QuestbookInstaller.install(force, true);
        if (!r.ok) {
            tell(sender, BAD + "Install failed: " + r.error);
            tell(sender, WARN + "Progress was restored from the snapshot taken before the write. See the server log.");
            return;
        }
        if (!r.changed) {
            tell(
                sender,
                OK + "This world already matches the jar (bundle "
                    + QuestbookInstaller.shortVersion(r.bundleVersion)
                    + "). /btsquests install force re-runs it.");
            return;
        }
        tell(sender, HEAD + "== Beyond the Stars questbook installed ==");
        tell(sender, r.summary() + ".");
        report(sender, r);
        tell(
            sender,
            OK + "Progress was snapshotted first and restored after: every completed quest is still complete.");
        if (r.taskProgressDropped > 0 || r.progressReset > 0) {
            tell(
                sender,
                DIM + ""
                    + r.taskProgressDropped
                    + " quest(s) changed their tasks and start them fresh (completion kept); "
                    + r.progressReset
                    + " planned checkbox(es) became real quests and were cleared.");
        }
        tell(sender, DIM + "Every open book was told to resync; /bq_user refresh forces it.");
    }

    private static void uninstall(ICommandSender sender, boolean all) {
        final QuestbookInstaller.Report r = QuestbookInstaller.uninstall(all);
        if (!r.ok) {
            tell(sender, BAD + "Uninstall failed: " + r.error);
            tell(sender, WARN + "Progress was restored from the snapshot taken before the write. See the server log.");
            return;
        }
        tell(sender, HEAD + "== Beyond the Stars questbook: uninstall " + (all ? "all" : "edits") + " ==");
        if (all) {
            tell(
                sender,
                "Restored " + r.editsReverted
                    + " pack quest(s) from their stored originals; removed "
                    + r.chapters
                    + " chapter(s) and "
                    + r.questsRemoved
                    + " quest(s) of ours.");
        } else {
            tell(
                sender,
                "Restored " + r.editsReverted
                    + " pack quest(s) from their stored originals. Our chapters stay; the Kerr and Eye fallback quests stand beside the pack's own.");
        }
        report(sender, r);
        tell(
            sender,
            WARN + "The server will not reinstall on start until an op runs /btsquests install."
                + (all ? "" : " To keep the pack unedited for good, set questbookPackEdits=false."));
    }

    private static void report(ICommandSender sender, QuestbookInstaller.Report r) {
        for (final Map.Entry<String, String> e : r.skipped.entrySet())
            tell(sender, WARN + "Skipped " + e.getKey() + ": " + e.getValue());
        if (!r.disabled.isEmpty()) tell(
            sender,
            DIM + ""
                + r.disabled.size()
                + " pack edit(s) switched off: "
                + r.disabled.values()
                    .iterator()
                    .next());
        for (final Map.Entry<String, List<String>> e : r.misses.entrySet())
            for (final String m : e.getValue()) tell(sender, WARN + e.getKey() + ": " + m);
        for (final String n : r.notes) tell(sender, DIM + n);
    }

    // ---------------------------------------------------------------- verify, dumpids

    private static void verify(ICommandSender sender) {
        final Map<String, QuestbookInstaller.Ref> refs;
        try {
            refs = QuestbookInstaller.verify();
        } catch (final Throwable t) {
            tell(sender, BAD + "Verify failed: " + BQBridge.describe(t));
            return;
        }
        int misses = 0;
        for (final QuestbookInstaller.Ref r : refs.values()) {
            if (r.found) continue;
            misses++;
            tell(
                sender,
                BAD + "[missing] "
                    + EnumChatFormatting.RESET
                    + r.kind
                    + " "
                    + r.id
                    + DIM
                    + " ("
                    + r.detail
                    + "; used by "
                    + String.join(", ", r.usedBy)
                    + ")");
        }
        tell(
            sender,
            (misses == 0 ? OK : WARN) + "Questbook ids: "
                + (refs.size() - misses)
                + "/"
                + refs.size()
                + " resolved, "
                + misses
                + " missing.");
    }

    private static void dumpIds(ICommandSender sender) {
        try {
            final File out = QuestbookInstaller.dumpIds();
            tell(sender, OK + "Wrote " + out.getAbsolutePath());
            tell(sender, DIM + "Copy the verified entries into questbook/registry.json and re-run gen-bom.ps1.");
        } catch (final Throwable t) {
            tell(sender, BAD + "dumpids failed: " + BQBridge.describe(t));
        }
    }

    private static String join(List<Long> lows) {
        final StringBuilder out = new StringBuilder();
        for (final long low : lows) {
            if (out.length() > 0) out.append(", ");
            out.append(low);
        }
        return out.toString();
    }
}
