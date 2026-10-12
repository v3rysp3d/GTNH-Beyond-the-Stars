package com.nateyburr.beyondthestars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The questbook installer, BQBridge v2 (docs/questbook-integration.md §4.5-§4.7). It applies the
 * jar's {@link QuestbookBundle} to the LIVE BetterQuesting database of the loaded world:
 * <ol>
 * <li>snapshot every player's progress;</li>
 * <li>plan the pack edits: each one is matched by id AND name, and by the chapter it is
 * expected in, against the pack quest's ORIGINAL (stored in {@link QuestbookState} with the
 * world's {@code pack_version}, and trusted while that number is unchanged; captured from the
 * live quest the first time and after a pack update); a mismatch skips that edit with a warning
 * and the quest stays as the pack wrote it (RC-3 safety);</li>
 * <li>revert every edit that is applied but no longer wanted (flag off, drifted, dropped);</li>
 * <li>replace our quests and chapters wholesale from the bundle, order our chapters after
 * UMV, graft ours into pack chapters;</li>
 * <li>apply each wanted edit to a COPY OF THE ORIGINAL (so a second run is exactly the same
 * database, and append-style ops never stack);</li>
 * <li>restore progress, filtered by §4.6, on every path;</li>
 * <li>write the state file, mark BetterQuesting dirty and resync every client.</li>
 * </ol>
 * Progress rules (§4.6): completion is always kept, except on a quest of ours that was a
 * checkbox placeholder at the last install and is real now (a ticked box never stands in for
 * the machine). Task progress is dropped wherever a quest's task list changed - which is
 * exactly the {@code completionOnly} edits (E01, E10, E63, E64) on their first application, and
 * never on a re-install.
 */
final class QuestbookInstaller {

    private QuestbookInstaller() {}

    /** The op-facing line the login listener shows once per server start, or null. */
    private static volatile String loginNotice;

    /** The bundle this world's quests were last installed from ("" = none): tags progress backup rows. */
    private static volatile String installedBundle = "";

    // ======================================================================== reports

    /** What one install / uninstall did. */
    static final class Report {

        boolean ok;
        String error = "";
        /** False when nothing needed doing (the world already matched the jar). */
        boolean changed = true;
        /** True when the jar carried no bundle and the legacy DefaultQuests path ran. */
        boolean legacy;
        String bundleVersion = "";
        String sourcePath = "";
        int questsAdded;
        int questsUpdated;
        int questsRemoved;
        int chapters;
        int grafts;
        int editsApplied;
        int editsReverted;
        int editsRecaptured;
        int progressReset;
        int taskProgressDropped;
        /** Edits that did not apply because the pack's quest no longer matches: warnings. */
        final Map<String, String> skipped = new TreeMap<>();
        /** Edits switched off on purpose (questbookPackEdits, a removal flag, an uninstall). */
        final Map<String, String> disabled = new TreeMap<>();
        final Map<String, List<String>> misses = new TreeMap<>();
        final List<String> notes = new ArrayList<>();

        String summary() {
            if (!ok) return "failed: " + error;
            if (!changed) return "already up to date (bundle " + shortVersion(bundleVersion) + ")";
            if (legacy)
                return "installed from DefaultQuests: " + questsAdded + " added, " + questsUpdated + " refreshed";
            return "bundle " + shortVersion(bundleVersion)
                + ": quests "
                + questsAdded
                + " added, "
                + questsUpdated
                + " refreshed, "
                + questsRemoved
                + " removed; pack edits "
                + editsApplied
                + " applied, "
                + skipped.size()
                + " skipped, "
                + disabled.size()
                + " switched off, "
                + editsReverted
                + " reverted";
        }
    }

    static String shortVersion(String v) {
        return v == null || v.isEmpty() ? "none" : v.length() > 12 ? v.substring(0, 12) : v;
    }

    // =================================================================== entry points

    /**
     * FMLServerStartedEvent: BetterQuesting loaded the world's database (and, for a new world,
     * copied DefaultQuests into it) in its own FMLServerStartingEvent handler, so it is complete
     * by now. Does nothing unless the world differs from the jar.
     */
    static void onServerStarted() {
        loginNotice = null;
        if (!BQBridge.isLoaded()) return;
        // The progress backup tags its rows with this, so it must describe THIS world even when
        // the auto-install is off (an integrated server opens several worlds in one JVM).
        try {
            installedBundle = QuestbookState.read(BQBridge.worldDir()).bundleVersion;
        } catch (final Throwable t) {
            installedBundle = "";
        }
        if (!Config.questbookAutoInstall) {
            BeyondTheStars.LOG
                .info("Questbook: auto-install is off (questbookAutoInstall); /btsquests install by hand.");
            return;
        }
        final Report r = autoInstall();
        if (r == null) return;
        if (!r.ok) {
            BeyondTheStars.LOG.error("Questbook: auto-install failed: " + r.error);
            loginNotice = "Beyond the Stars questbook: the auto-install failed (" + r.error
                + "). Progress was restored. /btsquests status";
            return;
        }
        if (!r.changed) {
            BeyondTheStars.LOG.info("Questbook: " + r.summary() + ".");
            return;
        }
        BeyondTheStars.LOG.info("Questbook: " + r.summary() + ".");
        logDetails(r);
        if (!r.skipped.isEmpty()) {
            loginNotice = "Beyond the Stars questbook: " + r.skipped.size()
                + " pack edit(s) were skipped because the pack's quest no longer matches (pack update?). /btsquests status";
        }
    }

    /** Auto-install: null when it was skipped on purpose (uninstalled by an op). */
    static Report autoInstall() {
        try {
            final File world = BQBridge.worldDir();
            final QuestbookState state = QuestbookState.read(world);
            installedBundle = state.bundleVersion;
            if (!state.uninstalled.isEmpty()) {
                BeyondTheStars.LOG.info(
                    "Questbook: an op ran /btsquests uninstall " + state.uninstalled
                        + " in this world; the auto-install leaves it alone until /btsquests install.");
                return null;
            }
        } catch (final Throwable t) {
            final Report r = new Report();
            r.error = BQBridge.describe(t);
            return r;
        }
        return install(false, false);
    }

    /**
     * {@code /btsquests install [force]} and the auto-install.
     *
     * @param force run even when the world already matches the jar
     * @param byOp  an op asked: clears a sticky uninstall
     */
    static Report install(boolean force, boolean byOp) {
        final Report report = new Report();
        try {
            final QuestbookBundle bundle = QuestbookBundle.load();
            if (bundle == null) return legacy(report);
            report.bundleVersion = bundle.version;
            report.sourcePath = "jar:" + QuestbookBundle.RESOURCE;
            final File world = BQBridge.worldDir();
            final QuestbookState state = QuestbookState.read(world);
            if (!byOp && !state.uninstalled.isEmpty()) {
                report.ok = true;
                report.changed = false;
                return report;
            }
            final String stale = force || !state.uninstalled.isEmpty() ? "forced" : staleReason(bundle, state);
            if (stale == null) {
                // Every applied quest is still as we left it, so no default load ran since its
                // original was captured: an original stored without a pack_version (an older state
                // file) belongs to the pack this world has now. Stamp it, so a later pack update is seen.
                final int packNow = BQBridge.packVersion();
                boolean stamped = false;
                for (final QuestbookState.Applied a : state.edits.values()) {
                    if (a.packVersion < 0 && packNow >= 0) {
                        a.packVersion = packNow;
                        stamped = true;
                    }
                }
                if (stamped) state.write(world);
                report.ok = true;
                report.changed = false;
                return report;
            }
            if (!byOp) BeyondTheStars.LOG.info("Questbook: this world differs from the jar: " + stale);
            run(bundle, state, Config.questbookPackEdits, "", report, world);
            report.ok = true;
        } catch (final Throwable t) {
            report.ok = false;
            report.error = BQBridge.describe(t);
            BeyondTheStars.LOG.error("Questbook install failed", t);
        }
        return report;
    }

    /**
     * {@code /btsquests uninstall [all|edits]}.
     * <ul>
     * <li>{@code edits}: every pack quest is restored from its stored original, and the world is
     * left exactly as with {@code questbookPackEdits=false} (our chapters stay; the fallback
     * quests 48103 and 48109 stand in, so the KEE and the Eye are still taught - rule 14).</li>
     * <li>{@code all}: that, then every chapter and quest of ours (questIDHigh 190) is removed,
     * the plant chapter included, so {@code uninstall all} then {@code install} is a clean
     * reinstall.</li>
     * </ul>
     * Both are sticky: the auto-install leaves the world alone until an op runs install.
     */
    static Report uninstall(boolean all) {
        final Report report = new Report();
        try {
            final File world = BQBridge.worldDir();
            final QuestbookState state = QuestbookState.read(world);
            final QuestbookBundle bundle = QuestbookBundle.present() ? QuestbookBundle.load() : null;
            if (bundle != null) report.bundleVersion = bundle.version;
            if (!all) {
                if (bundle == null) throw new IllegalStateException("this jar carries no questbook bundle");
                run(bundle, state, false, "edits", report, world);
                report.ok = true;
                return report;
            }
            final Map<UUID, Object> questMap = BQBridge.quests();
            final Map<UUID, Object> lineMap = BQBridge.lines();
            final NBTTagList progress = BQBridge.writeProgress();
            final Set<UUID> dropTasks = new HashSet<>();
            try {
                for (final String id : new ArrayList<>(state.edits.keySet())) {
                    revert(id, state.edits.get(id), questMap, lineMap, dropTasks, report);
                    state.edits.remove(id);
                    report.editsReverted++;
                }
                for (final UUID line : new ArrayList<>(lineMap.keySet())) {
                    if (line.getMostSignificantBits() != BQBridge.ID_HIGH) continue;
                    for (final UUID quest : strandedPackQuests(line, lineMap, questMap, new HashSet<>())) {
                        report.notes.add(
                            "pack quest " + BQBridge.label(quest)
                                + " sat only in our chapter "
                                + BQBridge.label(line)
                                + " and is in no chapter now; the BetterQuesting editor or /bq_admin default load puts it back");
                    }
                    BQBridge.removeLine(line);
                    report.chapters++;
                }
                for (final UUID quest : new ArrayList<>(questMap.keySet())) {
                    if (quest.getMostSignificantBits() != BQBridge.ID_HIGH) continue;
                    BQBridge.removeQuest(quest);
                    report.questsRemoved++;
                }
            } finally {
                restoreProgress(progress, new HashSet<>(), dropTasks, new NBTTagList(), report);
                installedBundle = "";
                state.uninstalled = "all";
                state.bundleVersion = "";
                // state.placeholders is kept: the next install must still know which quests were
                // ticked boxes, so a quest that is real by then does not inherit a checkbox completion.
                state.skipped.clear();
                state.misses.clear();
                state.write(world);
            }
            BQBridge.saveAndResync();
            report.ok = true;
        } catch (final Throwable t) {
            report.ok = false;
            report.error = BQBridge.describe(t);
            BeyondTheStars.LOG.error("Questbook uninstall failed", t);
        }
        return report;
    }

    private static Report legacy(Report report) {
        final BQBridge.LegacyInstall legacy = BQBridge.installFromDefaults();
        report.legacy = true;
        report.ok = legacy.ok;
        report.error = legacy.error;
        report.sourcePath = legacy.defaultsPath;
        report.questsAdded = legacy.questsAdded;
        report.questsUpdated = legacy.questsUpdated;
        report.notes.addAll(legacy.notes);
        report.notes.add("this jar carries no questbook bundle, so only our own quests were added (no pack edits)");
        return report;
    }

    private static void logDetails(Report r) {
        for (final Map.Entry<String, String> e : r.skipped.entrySet())
            BeyondTheStars.LOG.warn("Questbook: pack edit " + e.getKey() + " skipped: " + e.getValue());
        for (final Map.Entry<String, List<String>> e : r.misses.entrySet()) for (final String m : e.getValue())
            BeyondTheStars.LOG.warn("Questbook: pack edit " + e.getKey() + ": " + m);
        for (final String n : r.notes) BeyondTheStars.LOG.info("Questbook: " + n);
    }

    // ================================================================ the cheap check

    /** The config the edits were applied under, so flipping a flag re-runs the install. */
    static String configKey(QuestbookBundle bundle) {
        final TreeSet<String> flags = new TreeSet<>();
        for (final QuestbookBundle.Edit e : bundle.edits) flags.addAll(e.config);
        final StringBuilder out = new StringBuilder("questbookPackEdits=").append(Config.questbookPackEdits);
        for (final String flag : flags) out.append(';')
            .append(flag)
            .append('=')
            .append(configFlag(flag));
        return out.toString();
    }

    /** A boolean field of {@link Config} by name; null if there is none. */
    static Boolean configFlag(String name) {
        try {
            return Config.class.getField(name)
                .getBoolean(null);
        } catch (final Throwable t) {
            return null;
        }
    }

    /**
     * True when the world holds exactly what this jar would install: same bundle, same config,
     * all our chapters and quests present, and every applied edit's quest still as we left it
     * (a {@code /bq_admin default load} changes that, and so does an edit by hand).
     */
    static boolean upToDate(QuestbookBundle bundle, QuestbookState state) throws Exception {
        return staleReason(bundle, state) == null;
    }

    /** Null when the world matches the jar, else the first difference found (logged by the auto-install). */
    static String staleReason(QuestbookBundle bundle, QuestbookState state) throws Exception {
        if (!state.existed) return "no install state in this world yet";
        if (!bundle.version.equals(state.bundleVersion))
            return "bundle " + state.bundleVersion + " -> " + bundle.version;
        if (!configKey(bundle).equals(state.configKey)) return "config changed";
        final Map<UUID, Object> questMap = BQBridge.quests();
        final Map<UUID, Object> lineMap = BQBridge.lines();
        for (final QuestbookBundle.Chapter c : bundle.chapters)
            if (!lineMap.containsKey(c.id)) return "chapter " + c.id + " missing";
        for (final QuestbookBundle.Quest q : bundle.quests.values()) {
            final boolean wanted = q.onlyWithout == null || !state.edits.containsKey(q.onlyWithout);
            if (wanted != questMap.containsKey(q.id)) return "quest " + q.id + (wanted ? " missing" : " present");
        }
        for (final QuestbookState.Applied a : state.edits.values()) {
            if (!asApplied(a, questMap, lineMap)) return "pack quest " + a.quest + " is not as the edit left it";
        }
        return null;
    }

    /**
     * True when an applied edit's quest is still exactly as we left it: its definition AND its
     * chapter entries (a /bq_admin default load with our mirror merged keeps the definition of a
     * move-only edit but puts the quest back in its pack chapter).
     */
    static boolean asApplied(QuestbookState.Applied a, Map<UUID, Object> questMap, Map<UUID, Object> lineMap)
        throws Exception {
        final Object live = questMap.get(a.quest);
        return live != null && BQBridge.questNbt(live)
            .equals(a.applied) && liveEntries(a.quest, lineMap).equals(a.appliedEntries);
    }

    // ===================================================================== the engine

    /** One pack edit, planned. */
    private static final class Plan {

        final QuestbookBundle.Edit edit;
        boolean apply;
        /** Not applied on purpose (a flag or an uninstall), as opposed to drift. */
        boolean disabled;
        String skip;
        NBTTagCompound original;
        Map<UUID, NBTTagCompound> entries;
        /** The world's pack_version {@link #original} belongs to (-1 = unknown). */
        int packVersion = -1;
        boolean recaptured;
        /** The live quest differed from both what we applied and the stored original. */
        boolean handEdited;

        Plan(QuestbookBundle.Edit edit) {
            this.edit = edit;
        }
    }

    private static void run(QuestbookBundle bundle, QuestbookState state, boolean packEdits, String sticky,
        Report report, File world) throws Exception {
        final Map<UUID, Object> questMap = BQBridge.quests();
        final Map<UUID, Object> lineMap = BQBridge.lines();
        report.bundleVersion = bundle.version;

        // 1. Snapshot every player's progress, and read the backup of our quests' progress (for
        // quests a default load took out of the database).
        final NBTTagList progress = BQBridge.writeProgress();
        final Set<UUID> dropAll = new HashSet<>();
        final Set<UUID> dropTasks = new HashSet<>();
        final NBTTagList fromBackup = new NBTTagList();
        Map<UUID, NBTTagCompound> backup;
        try {
            backup = QuestbookState.readProgressBackup(world);
        } catch (final Exception e) {
            backup = new HashMap<>();
            report.notes.add("the progress backup could not be read (" + BQBridge.describe(e) + ")");
        }
        boolean finished = false;
        try {
            // 2. Plan the pack edits against their originals.
            final List<Plan> plans = new ArrayList<>();
            final Set<String> applying = new HashSet<>();
            final int packNow = BQBridge.packVersion();
            for (final QuestbookBundle.Edit e : bundle.edits) {
                final Plan p = plan(e, state, packEdits, sticky, packNow, questMap, lineMap);
                plans.add(p);
                if (p.apply) applying.add(e.id);
                else if (p.disabled) report.disabled.put(e.id, p.skip);
                else report.skipped.put(e.id, p.skip);
                if (p.recaptured) report.editsRecaptured++;
                if (p.apply && p.handEdited) report.notes.add(
                    "pack edit " + e.id
                        + ": its quest was changed since the install (by hand?); the edit was re-applied over the stored pack original");
            }

            // 3. Revert every applied edit that is not wanted now.
            for (final String id : new ArrayList<>(state.edits.keySet())) {
                if (applying.contains(id)) continue;
                revert(id, state.edits.get(id), questMap, lineMap, dropTasks, report);
                state.edits.remove(id);
                report.editsReverted++;
            }

            // 4. Remove what of ours must not be there: quests this bundle no longer has, and the
            // fallback quests whose edit is being applied.
            for (final UUID id : new ArrayList<>(questMap.keySet())) {
                if (id.getMostSignificantBits() != BQBridge.ID_HIGH) continue;
                final QuestbookBundle.Quest q = bundle.quests.get(id);
                if (q == null) {
                    report.notes.add("removed quest " + BQBridge.label(id) + " (not in this mod's questbook any more)");
                } else if (q.onlyWithout == null || !applying.contains(q.onlyWithout)) {
                    continue;
                }
                BQBridge.removeQuest(id);
                report.questsRemoved++;
            }
            for (final UUID line : new ArrayList<>(lineMap.keySet())) {
                if (line.getMostSignificantBits() == BQBridge.ID_HIGH && bundle.chapter(line) == null) {
                    BQBridge.removeLine(line);
                    report.notes
                        .add("removed chapter " + BQBridge.label(line) + " (not in this mod's questbook any more)");
                }
            }

            // 5. Our quests, replaced wholesale from the bundle.
            for (final QuestbookBundle.Quest q : bundle.quests.values()) {
                if (q.onlyWithout != null && applying.contains(q.onlyWithout)) continue;
                Object live = questMap.get(q.id);
                NBTTagCompound before = null;
                if (live == null) {
                    live = BQBridge.createQuest(q.id);
                    report.questsAdded++;
                    final NBTTagCompound row = backup.get(q.id);
                    // Was it a ticked box when its progress was saved? The state's list, or the
                    // row's own flag (written by backupProgress, so it does not depend on the state).
                    final boolean wasBox = state.placeholders.contains(q.low)
                        || (row != null && row.getBoolean("btsPlaceholder"));
                    final boolean nowBuilt = wasBox && !q.placeholder && !isLoneCheckbox(q.nbt);
                    if (row != null && nowBuilt) {
                        report.notes.add(
                            "quest " + BQBridge.label(q.id)
                                + " ("
                                + q.key
                                + ") is built now: its saved checkbox completion was not restored");
                    } else if (row != null) {
                        // Re-created (a default load without our DefaultQuests mirror removed it): put its
                        // progress back. Task progress only if it was taken under this same bundle.
                        final NBTTagCompound restored = (NBTTagCompound) row.copy();
                        if (!bundle.version.equals(row.getString("btsBundle")))
                            restored.setTag("tasks", new NBTTagList());
                        fromBackup.appendTag(restored);
                    }
                } else {
                    before = BQBridge.questNbt(live);
                    report.questsUpdated++;
                }
                BQBridge.readQuest(live, (NBTTagCompound) q.nbt.copy());
                if (before == null) continue;
                final boolean wasPlaceholder = state.placeholders.contains(q.low)
                    || (!state.existed && isLoneCheckbox(before) && countAsQuest(before) == 0);
                if (wasPlaceholder && !q.placeholder && !isLoneCheckbox(q.nbt)) {
                    dropAll.add(q.id);
                    report.notes.add(
                        "quest " + BQBridge.label(q.id)
                            + " ("
                            + q.key
                            + ") is built now: its planned checkbox was cleared");
                } else if (!tasksOf(before).equals(tasksOf(BQBridge.questNbt(live)))) {
                    dropTasks.add(q.id);
                }
            }

            // 6. Our chapters: rebuilt, then ordered (ladder chapters directly after UMV, in bundle
            // order; the plant chapter parked last).
            final Set<UUID> placing = new HashSet<>();
            for (final Plan p : plans) if (p.apply) placing.add(p.edit.quest);
            for (final QuestbookBundle.Chapter c : bundle.chapters) {
                final Object line = BQBridge.newLine((NBTTagCompound) c.line.copy());
                // A pack quest that sits only in this chapter and that no applying edit will place
                // again (its edit was dropped without a stored original to go back to) keeps its
                // entry, rather than vanish from the book.
                final Object old = lineMap.get(c.id);
                for (final UUID quest : strandedPackQuests(c.id, lineMap, questMap, placing)) {
                    BQBridge.entries(line)
                        .put(
                            quest,
                            BQBridge.newEntry(
                                BQBridge.entryNbt(
                                    BQBridge.entries(old)
                                        .get(quest))));
                    report.notes.add(
                        "pack quest " + BQBridge.label(quest)
                            + " sat only in our chapter "
                            + c.key
                            + " with no edit to place it; it was kept there");
                }
                for (final Map.Entry<UUID, NBTTagCompound> en : c.entries.entrySet()) {
                    if (!questMap.containsKey(en.getKey())) continue;
                    BQBridge.entries(line)
                        .put(
                            en.getKey(),
                            BQBridge.newEntry(
                                (NBTTagCompound) en.getValue()
                                    .copy()));
                }
                BQBridge.putLine(c.id, line);
                report.chapters++;
            }
            int after = 0;
            for (final QuestbookBundle.Chapter c : bundle.chapters) {
                if (!"afterPackLine".equals(c.order)) continue;
                // Park it last first, so taking it out of the list cannot shift the anchor's index.
                BQBridge.setOrderIndex(c.id, Integer.MAX_VALUE);
                final int anchor = c.after == null || !lineMap.containsKey(c.after) ? -1 : BQBridge.orderIndex(c.after);
                if (anchor < 0) {
                    report.notes
                        .add("the pack chapter our " + c.key + " chapter follows is missing; it was parked last");
                    continue;
                }
                BQBridge.setOrderIndex(c.id, anchor + 1 + after++);
            }
            for (final QuestbookBundle.Chapter c : bundle.chapters) {
                if (!"afterPackLine".equals(c.order)) BQBridge.setOrderIndex(c.id, Integer.MAX_VALUE);
            }

            // 7. Our quests in pack chapters. A chapter whose pack quests we re-lay out uses the
            // edited positions only when every edit that moves quests in or out of it applied;
            // otherwise the positions that sit clear of the unedited pack.
            for (final QuestbookBundle.Graft g : bundle.grafts) {
                if (!questMap.containsKey(g.quest)) continue;
                for (final Map.Entry<UUID, Object> l : lineMap.entrySet()) {
                    if (bundle.chapter(l.getKey()) == null && l.getValue() != null) BQBridge.entries(l.getValue())
                        .remove(g.quest);
                }
                final Object line = lineMap.get(g.line);
                if (line == null) {
                    report.notes.add(
                        "pack chapter " + BQBridge.label(g.line)
                            + " is not in this world; quest "
                            + BQBridge.label(g.quest)
                            + " has no place in the book");
                    continue;
                }
                boolean edited = true;
                for (final QuestbookBundle.Edit e : bundle.edits) {
                    if (e.touchesLine(g.line) && !applying.contains(e.id)) {
                        edited = false;
                        break;
                    }
                }
                final int[] p = edited ? g.on : g.off;
                BQBridge.entries(line)
                    .put(g.quest, BQBridge.newEntry(BQBridge.entryNbt(p[0], p[1], p[2], p[3])));
                report.grafts++;
            }

            // 8. The pack edits, each applied to a copy of its original.
            for (final Plan p : plans) {
                if (p.apply) apply(p, state, questMap, lineMap, dropTasks, report);
            }
            finished = true;
        } finally {
            // 9. Put the campaign back, on every path.
            restoreProgress(progress, dropAll, dropTasks, fromBackup, report);
            if (fromBackup.tagCount() > 0) report.notes.add(
                "progress on " + fromBackup.tagCount()
                    + " re-created quest(s) of ours came back from the progress backup");
            installedBundle = finished ? bundle.version : "";
            state.bundleVersion = finished ? bundle.version : "";
            state.configKey = finished ? configKey(bundle) : "";
            state.uninstalled = sticky;
            if (finished) {
                // Only a finished install changes which quests are boxes; a failed one keeps the old list.
                state.placeholders.clear();
                for (final QuestbookBundle.Quest q : bundle.quests.values())
                    if (q.placeholder) state.placeholders.add(q.low);
            }
            state.skipped.clear();
            state.skipped.putAll(report.skipped);
            state.disabled.clear();
            state.disabled.putAll(report.disabled);
            state.misses.clear();
            state.misses.putAll(report.misses);
            try {
                state.write(world);
            } catch (final Exception t) {
                if (finished) throw t;
                BeyondTheStars.LOG.error("Questbook: could not write the state file after a failed install", t);
            }
        }
        // 10. Persist and redraw.
        BQBridge.saveAndResync();
    }

    private static Plan plan(QuestbookBundle.Edit e, QuestbookState state, boolean packEdits, String sticky,
        int packNow, Map<UUID, Object> questMap, Map<UUID, Object> lineMap) throws Exception {
        final Plan p = new Plan(e);
        p.disabled = true;
        if (!packEdits) {
            p.skip = sticky.isEmpty() ? "pack edits are off (questbookPackEdits=false)"
                : "pack edits were uninstalled (/btsquests uninstall " + sticky + ")";
            return p;
        }
        for (final String flag : e.config) {
            final Boolean on = configFlag(flag);
            if (on == null) {
                p.skip = "needs config flag " + flag + ", which this build does not have";
                return p;
            }
            if (!on) {
                p.skip = flag + " is off";
                return p;
            }
        }
        p.disabled = false;
        final Object live = questMap.get(e.quest);
        if (live == null) {
            p.skip = "pack quest " + e.questB64 + " is not in this world's quest database";
            return p;
        }
        final NBTTagCompound liveNbt = BQBridge.questNbt(live);
        final QuestbookState.Applied prev = state.edits.get(e.id);
        final boolean ours = prev != null && e.quest.equals(prev.quest);
        if (ours && (asApplied(prev, questMap, lineMap) || samePack(prev.packVersion, packNow))) {
            // Exactly as we left it, or changed while the pack stayed the same (a hand edit, a
            // same-pack default load): the stored original is still the pack's quest. Re-apply over
            // it; never recapture a live quest that may still carry our ops.
            p.original = prev.original;
            p.entries = new LinkedHashMap<>(prev.entries);
            p.packVersion = prev.packVersion >= 0 ? prev.packVersion : packNow;
            p.handEdited = !liveNbt.equals(prev.applied) && !liveNbt.equals(prev.original);
        } else {
            // First time, or the world's pack_version changed since the original was captured (a
            // pack update and BetterQuesting's default load): the LIVE quest is the pack's now.
            // If it no longer matches, the edit is skipped; step 3 then reverts what is left of
            // the old application and drops it from the state, so an uninstall never restores
            // the old pack's quest.
            p.original = liveNbt;
            p.entries = liveEntries(e.quest, lineMap);
            p.packVersion = packNow;
            p.recaptured = prev != null;
        }
        final String why = drift(e, p.original, p.entries);
        if (why != null) {
            p.skip = why;
            p.recaptured = false;
            p.handEdited = false;
            return p;
        }
        p.apply = true;
        return p;
    }

    /** Same pack: both numbers known and equal, or either unknown (then trust what is stored). */
    private static boolean samePack(int stored, int now) {
        return stored < 0 || now < 0 || stored == now;
    }

    /** Null when the quest is the one the edit was written for, else why it is not. */
    private static String drift(QuestbookBundle.Edit e, NBTTagCompound original, Map<UUID, NBTTagCompound> entries) {
        final String name = strip(props(original).getString("name"));
        if (!name.equals(e.expectName))
            return "name drift: expected '" + e.expectName + "', the pack has '" + name + "'";
        if (!entries.containsKey(e.expectChapter))
            return "chapter drift: '" + e.expectName + "' is no longer in chapter " + BQBridge.label(e.expectChapter);
        return null;
    }

    private static void apply(Plan p, QuestbookState state, Map<UUID, Object> questMap, Map<UUID, Object> lineMap,
        Set<UUID> dropTasks, Report report) throws Exception {
        final QuestbookBundle.Edit e = p.edit;
        final Object live = questMap.get(e.quest);
        final NBTTagCompound before = BQBridge.questNbt(live);
        final NBTTagCompound edited = (NBTTagCompound) p.original.copy();
        final Map<UUID, NBTTagCompound> entries = new LinkedHashMap<>();
        for (final Map.Entry<UUID, NBTTagCompound> en : p.entries.entrySet()) entries.put(
            en.getKey(),
            (NBTTagCompound) en.getValue()
                .copy());
        final List<String> misses = new ArrayList<>();
        for (final JsonObject op : e.ops) applyOp(op, edited, entries, questMap, lineMap, misses);

        BQBridge.readQuest(live, edited);
        final NBTTagCompound after = BQBridge.questNbt(live);
        if (!tasksOf(before).equals(tasksOf(after))) {
            // completionOnly edits land here on their first application (and never on a re-install,
            // where before == after); a 'keep' edit should never change tasks, so say so if it did.
            dropTasks.add(e.quest);
            if ("keep".equals(e.progress))
                misses.add("its task list changed although its progress policy is 'keep'; task progress was reset");
        }
        BQBridge.removeFromAllLines(e.quest);
        final Map<UUID, NBTTagCompound> placed = new LinkedHashMap<>();
        for (final Map.Entry<UUID, NBTTagCompound> en : entries.entrySet()) {
            final Object line = lineMap.get(en.getKey());
            if (line == null) {
                misses.add("chapter " + BQBridge.label(en.getKey()) + " is missing; that entry was dropped");
                continue;
            }
            placed.put(en.getKey(), BQBridge.entryNbt(BQBridge.newEntry(en.getValue())));
            BQBridge.entries(line)
                .put(e.quest, BQBridge.newEntry(en.getValue()));
        }

        final QuestbookState.Applied a = new QuestbookState.Applied();
        a.quest = e.quest;
        a.original = p.original;
        a.entries.putAll(p.entries);
        a.applied = after;
        a.appliedEntries.putAll(placed);
        a.packVersion = p.packVersion;
        state.edits.put(e.id, a);
        if (!misses.isEmpty()) report.misses.put(e.id, misses);
        report.editsApplied++;
    }

    /** Restores one pack quest from its stored original (definition and chapter entries). */
    private static void revert(String id, QuestbookState.Applied a, Map<UUID, Object> questMap,
        Map<UUID, Object> lineMap, Set<UUID> dropTasks, Report report) throws Exception {
        final Object live = questMap.get(a.quest);
        if (live == null) {
            report.notes.add("pack edit " + id + ": its quest is gone from this world; nothing to restore");
            return;
        }
        final NBTTagCompound before = BQBridge.questNbt(live);
        if (a.applied != null && !before.equals(a.applied)) {
            // A /bq_admin default load (or a hand edit) already replaced its definition: leave
            // that. Its PLACEMENT still goes back if it is where we put it, or only in chapters of
            // ours - those are rebuilt or removed next, and the quest would drop out of the book.
            final Map<UUID, NBTTagCompound> now = liveEntries(a.quest, lineMap);
            if (now.equals(a.appliedEntries) || onlyInOurLines(now)) {
                placeBack(a, lineMap);
                report.notes.add(
                    "pack edit " + id
                        + ": its quest changed since the install (a hand edit or a default load); its text was left as it is, its chapter placement restored");
            } else {
                report.notes
                    .add("pack edit " + id + ": its quest changed since the install (default load?); left as it is");
            }
            return;
        }
        BQBridge.readQuest(live, (NBTTagCompound) a.original.copy());
        if (!tasksOf(before).equals(tasksOf(BQBridge.questNbt(live)))) dropTasks.add(a.quest);
        placeBack(a, lineMap);
    }

    /** The pack quest's stored chapter entries back, and no others. */
    private static void placeBack(QuestbookState.Applied a, Map<UUID, Object> lineMap) throws Exception {
        BQBridge.removeFromAllLines(a.quest);
        for (final Map.Entry<UUID, NBTTagCompound> en : a.entries.entrySet()) {
            final Object line = lineMap.get(en.getKey());
            if (line == null) continue;
            BQBridge.entries(line)
                .put(
                    a.quest,
                    BQBridge.newEntry(
                        (NBTTagCompound) en.getValue()
                            .copy()));
        }
    }

    /** True when the quest has entries and every one is in a chapter of ours (questIDHigh 190). */
    private static boolean onlyInOurLines(Map<UUID, NBTTagCompound> entries) {
        if (entries.isEmpty()) return false;
        for (final UUID line : entries.keySet()) if (line.getMostSignificantBits() != BQBridge.ID_HIGH) return false;
        return true;
    }

    /**
     * Pack quests (not questIDHigh 190) in our chapter {@code line} that have no entry in any
     * pack chapter and are not in {@code placing}: removing or rebuilding {@code line} would
     * leave them in no chapter at all.
     */
    private static List<UUID> strandedPackQuests(UUID line, Map<UUID, Object> lineMap, Map<UUID, Object> questMap,
        Set<UUID> placing) {
        final List<UUID> out = new ArrayList<>();
        final Object ourLine = lineMap.get(line);
        if (ourLine == null) return out;
        for (final UUID quest : BQBridge.entries(ourLine)
            .keySet()) {
            if (quest.getMostSignificantBits() == BQBridge.ID_HIGH || placing.contains(quest)
                || !questMap.containsKey(quest)) continue;
            boolean housed = false;
            for (final Map.Entry<UUID, Object> l : lineMap.entrySet()) {
                if (l.getKey()
                    .getMostSignificantBits() == BQBridge.ID_HIGH || l.getValue() == null) continue;
                if (BQBridge.entries(l.getValue())
                    .containsKey(quest)) {
                    housed = true;
                    break;
                }
            }
            if (!housed) out.add(quest);
        }
        return out;
    }

    /**
     * The snapshot back, filtered (§4.6): a quest in {@code dropAll} loses everything (a placeholder
     * that is real now), a quest in {@code dropTasks} keeps its completion and starts its tasks
     * fresh (its task list changed, so old task indexes would land on new tasks).
     */
    private static void restoreProgress(NBTTagList progress, Set<UUID> dropAll, Set<UUID> dropTasks,
        NBTTagList fromBackup, Report report) throws Exception {
        final NBTTagList keep = new NBTTagList();
        for (int i = 0; i < fromBackup.tagCount(); i++) keep.appendTag(fromBackup.getCompoundTagAt(i));
        final Map<UUID, NBTTagCompound> completionOnly = new HashMap<>();
        for (int i = 0; i < progress.tagCount(); i++) {
            final NBTTagCompound row = progress.getCompoundTagAt(i);
            final UUID id = BQBridge.readId(row, "questID");
            if (id != null && dropAll.contains(id)) continue;
            if (id != null && dropTasks.contains(id)) {
                final NBTTagCompound trimmed = (NBTTagCompound) row.copy();
                trimmed.setTag("tasks", new NBTTagList());
                completionOnly.put(id, trimmed);
                continue;
            }
            keep.appendTag(row);
        }
        BQBridge.readProgress(keep, true);
        final Map<UUID, Object> questMap = BQBridge.quests();
        for (final UUID id : dropAll) {
            final Object quest = questMap.get(id);
            if (quest == null) continue;
            BQBridge.resetQuest(quest);
            report.progressReset++;
        }
        for (final Map.Entry<UUID, NBTTagCompound> e : completionOnly.entrySet()) {
            final Object quest = questMap.get(e.getKey());
            if (quest == null) continue;
            // Wipe whatever the task objects hold, then put the completion map back alone.
            BQBridge.resetQuest(quest);
            quest.getClass()
                .getMethod("readProgressFromNBT", NBTTagCompound.class, boolean.class)
                .invoke(quest, e.getValue(), true);
            report.taskProgressDropped++;
        }
    }

    // ===================================================================== the ops

    /** One §4.4 op on the copy of a pack quest's NBT and its chapter entries. */
    private static void applyOp(JsonObject op, NBTTagCompound q, Map<UUID, NBTTagCompound> entries,
        Map<UUID, Object> questMap, Map<UUID, Object> lineMap, List<String> misses) throws Exception {
        final String kind = QuestbookBundle.str(op, "op");
        final NBTTagCompound props = props(q);
        switch (kind) {
            case "setName":
                props.setString("name", QuestbookBundle.str(op, "name"));
                break;
            case "setDesc":
                props.setString("desc", QuestbookBundle.str(op, "desc"));
                break;
            case "setIcon":
                props.setTag(
                    "icon",
                    QuestbookBundle.wrapped("icon:10", op.get("icon"))
                        .getCompoundTag("icon"));
                break;
            case "replaceText": {
                final String desc = props.getString("desc");
                final String find = QuestbookBundle.str(op, "find");
                if (find.isEmpty() || !desc.contains(find)) {
                    misses.add("text not found, so not replaced: \"" + abbreviate(find) + "\"");
                } else {
                    props.setString("desc", desc.replace(find, QuestbookBundle.str(op, "with")));
                }
                break;
            }
            case "appendText":
                props.setString("desc", props.getString("desc") + "\n\n" + QuestbookBundle.str(op, "text"));
                break;
            case "setTasks":
                q.setTag(
                    "tasks",
                    QuestbookBundle.wrapped("tasks:9", op.get("tasks"))
                        .getTagList("tasks", 10));
                break;
            case "setRewards":
                q.setTag(
                    "rewards",
                    QuestbookBundle.wrapped("rewards:9", op.get("rewards"))
                        .getTagList("rewards", 10));
                break;
            case "swapItem": {
                final JsonObject from = op.getAsJsonObject("from");
                final JsonObject to = op.getAsJsonObject("to");
                final int n = swapItems(
                    q.getTagList("tasks", 10),
                    QuestbookBundle.str(from, "id"),
                    from.get("damage")
                        .getAsInt(),
                    QuestbookBundle.str(to, "id"),
                    to.get("damage")
                        .getAsInt());
                if (n == 0) misses.add(
                    "no task item " + QuestbookBundle.str(from, "id")
                        + ":"
                        + from.get("damage")
                            .getAsInt()
                        + " to swap");
                break;
            }
            case "setPrereqs": {
                final NBTTagList list = new NBTTagList();
                for (final com.google.gson.JsonElement el : QuestbookBundle.array(op, "prereqs")) {
                    final JsonObject pr = el.getAsJsonObject();
                    final UUID id = BQBridge.decodeId(QuestbookBundle.str(pr, "b64"));
                    if (!questMap.containsKey(id)) {
                        misses.add("prerequisite " + BQBridge.label(id) + " is not in this world; left out");
                        continue;
                    }
                    list.appendTag(prereq(id, type(pr)));
                }
                q.setTag("preRequisites", list);
                break;
            }
            case "addPrereq": {
                final JsonObject pr = op.getAsJsonObject("prereq");
                final UUID id = BQBridge.decodeId(QuestbookBundle.str(pr, "b64"));
                final NBTTagList list = q.getTagList("preRequisites", 10);
                if (!questMap.containsKey(id)) {
                    misses.add("prerequisite " + BQBridge.label(id) + " is not in this world; not added");
                } else if (indexOfPrereq(list, id) < 0) {
                    list.appendTag(prereq(id, type(pr)));
                    q.setTag("preRequisites", list);
                }
                break;
            }
            case "removePrereq": {
                final UUID id = BQBridge.decodeId(
                    QuestbookBundle
                        .str(op.has("prereq") ? op.getAsJsonObject("prereq") : op.getAsJsonObject("from"), "b64"));
                final NBTTagList list = q.getTagList("preRequisites", 10);
                final int at = indexOfPrereq(list, id);
                if (at < 0) misses.add("prerequisite " + BQBridge.label(id) + " was not there to remove");
                else list.removeTag(at);
                q.setTag("preRequisites", list);
                break;
            }
            case "replacePrereq": {
                final UUID from = BQBridge.decodeId(QuestbookBundle.str(op.getAsJsonObject("from"), "b64"));
                final UUID to = BQBridge.decodeId(QuestbookBundle.str(op.getAsJsonObject("to"), "b64"));
                final NBTTagList list = q.getTagList("preRequisites", 10);
                final int at = indexOfPrereq(list, from);
                if (at < 0) {
                    misses.add("prerequisite " + BQBridge.label(from) + " was not there to replace");
                } else if (!questMap.containsKey(to)) {
                    misses.add(
                        "replacement prerequisite " + BQBridge.label(to) + " is not in this world; kept the old one");
                } else {
                    final NBTTagCompound row = list.getCompoundTagAt(at);
                    BQBridge.writeId(row, "questID", to);
                }
                q.setTag("preRequisites", list);
                break;
            }
            case "move": {
                final UUID from = BQBridge.decodeId(QuestbookBundle.str(op, "from"));
                final UUID to = BQBridge.decodeId(QuestbookBundle.str(op, "to"));
                if (!lineMap.containsKey(to)) {
                    misses.add("target chapter " + BQBridge.label(to) + " is missing; the quest stays where it was");
                    break;
                }
                entries.remove(from);
                entries.put(
                    to,
                    BQBridge.entryNbt(
                        op.get("x")
                            .getAsInt(),
                        op.get("y")
                            .getAsInt(),
                        op.get("sizeX")
                            .getAsInt(),
                        op.get("sizeY")
                            .getAsInt()));
                break;
            }
            case "hide":
                props.setString("visibility", "HIDDEN");
                entries.clear();
                break;
            default:
                misses.add("unknown op '" + kind + "' (newer bundle than installer?)");
        }
    }

    /** {@code properties.betterquesting}, created and attached if absent. */
    private static NBTTagCompound props(NBTTagCompound quest) {
        NBTTagCompound properties = quest.getCompoundTag("properties");
        if (!quest.hasKey("properties", 10)) quest.setTag("properties", properties);
        final NBTTagCompound bq = properties.getCompoundTag("betterquesting");
        if (!properties.hasKey("betterquesting", 10)) properties.setTag("betterquesting", bq);
        return bq;
    }

    private static NBTTagCompound prereq(UUID id, int type) {
        final NBTTagCompound row = new NBTTagCompound();
        BQBridge.writeId(row, "questID", id);
        if (type != 0) row.setByte("type", (byte) type);
        return row;
    }

    private static int type(JsonObject pr) {
        return pr.has("type") ? pr.get("type")
            .getAsInt() : 0;
    }

    private static int indexOfPrereq(NBTTagList list, UUID id) {
        for (int i = 0; i < list.tagCount(); i++) {
            if (id.equals(BQBridge.readId(list.getCompoundTagAt(i), "questID"))) return i;
        }
        return -1;
    }

    /** Every item compound {id, Damage} under the tasks, swapped; counts and flags kept. */
    private static int swapItems(NBTBase tag, String fromId, int fromDamage, String toId, int toDamage) {
        int n = 0;
        if (tag instanceof NBTTagCompound) {
            final NBTTagCompound c = (NBTTagCompound) tag;
            if (c.hasKey("id", 8) && fromId.equals(c.getString("id"))
                && c.hasKey("Damage")
                && c.getInteger("Damage") == fromDamage) {
                c.setString("id", toId);
                c.setShort("Damage", (short) toDamage);
                n++;
            }
            for (final Object key : c.func_150296_c())
                n += swapItems(c.getTag((String) key), fromId, fromDamage, toId, toDamage);
        } else if (tag instanceof NBTTagList) {
            final NBTTagList list = (NBTTagList) tag;
            if (list.func_150303_d() == 10) {
                for (int i = 0; i < list.tagCount(); i++)
                    n += swapItems(list.getCompoundTagAt(i), fromId, fromDamage, toId, toDamage);
            }
        }
        return n;
    }

    static NBTTagList tasksOf(NBTTagCompound quest) {
        return quest.getTagList("tasks", 10);
    }

    private static boolean isLoneCheckbox(NBTTagCompound quest) {
        final NBTTagList tasks = tasksOf(quest);
        return tasks.tagCount() == 1 && "bq_standard:checkbox".equals(
            tasks.getCompoundTagAt(0)
                .getString("taskID"));
    }

    private static int countAsQuest(NBTTagCompound quest) {
        final NBTTagCompound bq = quest.getCompoundTag("properties")
            .getCompoundTag("betterquesting");
        return bq.hasKey("countAsQuest") ? bq.getByte("countAsQuest") : 1;
    }

    private static Map<UUID, NBTTagCompound> liveEntries(UUID quest, Map<UUID, Object> lineMap) throws Exception {
        final Map<UUID, NBTTagCompound> out = new LinkedHashMap<>();
        for (final Map.Entry<UUID, Object> l : lineMap.entrySet()) {
            if (l.getValue() == null) continue;
            final Object entry = BQBridge.entries(l.getValue())
                .get(quest);
            if (entry != null) out.put(l.getKey(), BQBridge.entryNbt(entry));
        }
        return out;
    }

    static String strip(String s) {
        return s == null ? ""
            : s.replaceAll("§.", "")
                .trim();
    }

    private static String abbreviate(String s) {
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }

    // ========================================================================= status

    /** One chapter (or graft set) of ours, counted. */
    static final class ChapterCount {

        String label;
        boolean linePresent = true;
        int present;
        int expected;
        final List<Long> missing = new ArrayList<>();
    }

    /** What {@code /btsquests status} prints. */
    static final class Status {

        boolean readable;
        String error = "";
        int totalQuests;
        int totalLines;
        String jarBundle = "";
        String worldBundle = "";
        boolean stateExists;
        String uninstalled = "";
        boolean configChanged;
        final List<ChapterCount> chapters = new ArrayList<>();
        boolean entryQuestPresent;
        int editsTotal;
        int editsApplied;
        /** Applied edits whose quest changed since (a default load): the next install re-applies them. */
        final List<String> editsChanged = new ArrayList<>();
        final Map<String, String> skipped = new TreeMap<>();
        /** reason -> how many edits are switched off for it. */
        final Map<String, Integer> disabled = new TreeMap<>();
        final Map<String, List<String>> misses = new TreeMap<>();
        /** "low key (needs a, b)" per placeholder in this build. */
        final List<String> placeholders = new ArrayList<>();
        final List<String> fallbacks = new ArrayList<>();

        boolean complete() {
            if (!readable || !entryQuestPresent) return false;
            for (final ChapterCount c : chapters) if (!c.linePresent || !c.missing.isEmpty()) return false;
            return true;
        }
    }

    static Status status() {
        final Status s = new Status();
        try {
            final Map<UUID, Object> questMap = BQBridge.quests();
            final Map<UUID, Object> lineMap = BQBridge.lines();
            s.readable = true;
            s.totalQuests = questMap.size();
            s.totalLines = lineMap.size();
            final QuestbookState state = QuestbookState.read(BQBridge.worldDir());
            s.stateExists = state.existed;
            s.worldBundle = state.bundleVersion;
            s.uninstalled = state.uninstalled;
            final QuestbookBundle bundle = QuestbookBundle.present() ? QuestbookBundle.load() : null;
            if (bundle != null) {
                s.jarBundle = bundle.version;
                s.configChanged = state.existed && !configKey(bundle).equals(state.configKey);
            }
            s.chapters.add(
                count(
                    "Beyond the Stars (the plant)",
                    BTSQuestIds.LINE_PLANT,
                    BTSQuestIds.PLANT_QUESTS,
                    questMap,
                    lineMap));
            s.chapters.add(count("Ours in Tier 11 - UIV", -1, BTSQuestIds.UIV_QUESTS, questMap, lineMap));
            s.chapters.add(count("Ours in Tier 12 - UMV", -1, BTSQuestIds.UMV_QUESTS, questMap, lineMap));
            s.chapters.add(count("Tier 13 - UXV", BTSQuestIds.LINE_UXV, BTSQuestIds.UXV_QUESTS, questMap, lineMap));
            s.chapters.add(count("Tier 14 - MAX", BTSQuestIds.LINE_MAX, BTSQuestIds.MAX_QUESTS, questMap, lineMap));
            s.chapters.add(count("The Endgame", BTSQuestIds.LINE_END, BTSQuestIds.END_QUESTS, questMap, lineMap));
            s.entryQuestPresent = questMap.containsKey(BQBridge.ENTRY_QUEST_UUID);

            s.editsTotal = BTSQuestIds.PACK_EDITS.length;
            for (final Map.Entry<String, QuestbookState.Applied> e : state.edits.entrySet()) {
                s.editsApplied++;
                if (!asApplied(e.getValue(), questMap, lineMap)) s.editsChanged.add(e.getKey());
            }
            java.util.Collections.sort(s.editsChanged);
            s.skipped.putAll(state.skipped);
            for (final String why : state.disabled.values()) s.disabled.merge(why, 1, Integer::sum);
            s.misses.putAll(state.misses);
            if (bundle != null) {
                for (final QuestbookBundle.Quest q : bundle.quests.values()) {
                    if (q.placeholder)
                        s.placeholders.add(q.low + " " + q.key + " (needs " + String.join(", ", q.needs) + ")");
                    if (q.onlyWithout != null) s.fallbacks.add(
                        q.low + " "
                            + q.key
                            + (questMap.containsKey(q.id) ? " shown (" + q.onlyWithout + " not applied)"
                                : state.edits.containsKey(q.onlyWithout) ? " hidden (" + q.onlyWithout + " applied)"
                                    : " not in this world"));
                }
            }
        } catch (final Throwable t) {
            s.readable = false;
            s.error = BQBridge.describe(t);
        }
        return s;
    }

    private static ChapterCount count(String label, long line, long[] lows, Map<UUID, Object> questMap,
        Map<UUID, Object> lineMap) {
        final ChapterCount c = new ChapterCount();
        c.label = label;
        if (line >= 0) c.linePresent = lineMap.containsKey(new UUID(BQBridge.ID_HIGH, line));
        c.expected = lows.length;
        for (final long low : lows) {
            if (questMap.containsKey(new UUID(BQBridge.ID_HIGH, low))) c.present++;
            else c.missing.add(low);
        }
        return c;
    }

    // ================================================================= verify, dumpids

    /** One id the bundle names, resolved against the live registries. */
    static final class Ref {

        String kind;
        String id;
        boolean found;
        String detail = "";
        final Set<String> usedBy = new TreeSet<>();
    }

    /**
     * Every item, fluid and ore name the bundle uses (quests and edit payloads), resolved against
     * {@code GameData}/{@code GameRegistry}, {@code FluidRegistry} and {@code OreDictionary}.
     */
    static Map<String, Ref> verify() throws Exception {
        final QuestbookBundle bundle = QuestbookBundle.load();
        final Map<String, Ref> refs = new TreeMap<>();
        if (bundle == null) return refs;
        for (final QuestbookBundle.Quest q : bundle.quests.values()) walk(q.nbt, BQBridge.label(q.id), refs);
        for (final QuestbookBundle.Edit e : bundle.edits) {
            for (final JsonObject op : e.ops) {
                final String kind = QuestbookBundle.str(op, "op");
                if ("setIcon".equals(kind)) walk(QuestbookBundle.wrapped("icon:10", op.get("icon")), e.id, refs);
                else if ("setTasks".equals(kind)) walk(QuestbookBundle.wrapped("tasks:9", op.get("tasks")), e.id, refs);
                else if ("setRewards".equals(kind))
                    walk(QuestbookBundle.wrapped("rewards:9", op.get("rewards")), e.id, refs);
                else if ("swapItem".equals(kind)) {
                    final JsonObject to = op.getAsJsonObject("to");
                    final NBTTagCompound c = new NBTTagCompound();
                    c.setString("id", QuestbookBundle.str(to, "id"));
                    c.setShort(
                        "Damage",
                        (short) to.get("damage")
                            .getAsInt());
                    walk(c, e.id, refs);
                }
            }
        }
        for (final Ref r : refs.values()) resolve(r);
        return refs;
    }

    private static void walk(NBTBase tag, String where, Map<String, Ref> refs) {
        if (tag instanceof NBTTagCompound) {
            final NBTTagCompound c = (NBTTagCompound) tag;
            if (c.hasKey("id", 8) && c.hasKey("Damage")) {
                final String id = c.getString("id");
                if (id.indexOf(':') > 0) ref(refs, "item", id + ":" + c.getInteger("Damage"), where);
            }
            if (c.hasKey("OreDict", 8) && !c.getString("OreDict")
                .isEmpty()) ref(refs, "ore", c.getString("OreDict"), where);
            if (c.hasKey("FluidName", 8)) ref(refs, "fluid", c.getString("FluidName"), where);
            for (final Object key : c.func_150296_c()) walk(c.getTag((String) key), where, refs);
        } else if (tag instanceof NBTTagList) {
            final NBTTagList list = (NBTTagList) tag;
            if (list.func_150303_d() == 10) {
                for (int i = 0; i < list.tagCount(); i++) walk(list.getCompoundTagAt(i), where, refs);
            }
        }
    }

    private static void ref(Map<String, Ref> refs, String kind, String id, String where) {
        final String key = kind + " " + id;
        Ref r = refs.get(key);
        if (r == null) {
            r = new Ref();
            r.kind = kind;
            r.id = id;
            refs.put(key, r);
        }
        r.usedBy.add(where);
    }

    private static void resolve(Ref r) {
        try {
            if ("fluid".equals(r.kind)) {
                r.found = FluidRegistry.getFluid(r.id) != null;
                if (r.found) r.detail = FluidRegistry.getFluid(r.id)
                    .getLocalizedName();
            } else if ("ore".equals(r.kind)) {
                final List<ItemStack> ores = OreDictionary.getOres(r.id);
                r.found = ores != null && !ores.isEmpty();
                if (r.found) {
                    final ItemStack first = ores.get(0);
                    r.detail = GameData.getItemRegistry()
                        .getNameForObject(first.getItem()) + ":"
                        + first.getItemDamage();
                }
            } else {
                final int cut = r.id.lastIndexOf(':');
                final String name = r.id.substring(0, cut);
                final int meta = Integer.parseInt(r.id.substring(cut + 1));
                final int modCut = name.indexOf(':');
                final Item item = GameRegistry.findItem(name.substring(0, modCut), name.substring(modCut + 1));
                if (item == null) {
                    r.found = false;
                    r.detail = "no such item";
                    return;
                }
                if ("gregtech:gt.blockmachines".equals(name)) {
                    final boolean exists = meta >= 0 && meta < gregtech.api.GregTechAPI.METATILEENTITIES.length
                        && gregtech.api.GregTechAPI.METATILEENTITIES[meta] != null;
                    if (!exists) {
                        r.found = false;
                        r.detail = "no GT machine with id " + meta;
                        return;
                    }
                }
                final String display = new ItemStack(item, 1, meta).getDisplayName();
                r.found = display != null && !display.isEmpty() && !display.contains(".name");
                r.detail = display == null ? "" : display;
            }
        } catch (final Throwable t) {
            r.found = false;
            r.detail = BQBridge.describe(t);
        }
    }

    /**
     * {@code /btsquests dumpids}: the resolved table, in {@code questbook/registry.json}'s shape,
     * at {@code config/beyondthestars/questbook-registry.json}, for copying back into the source.
     */
    static File dumpIds() throws Exception {
        final Map<String, Ref> refs = verify();
        final JsonObject root = new JsonObject();
        root.addProperty(
            "about",
            "Written by /btsquests dumpids: every item, fluid and ore name the questbook bundle uses, resolved in a live game."
                + " status 'verified' = it resolved; 'missing' = it did not.");
        final JsonObject items = new JsonObject();
        final JsonObject fluids = new JsonObject();
        final JsonObject ores = new JsonObject();
        for (final Ref r : refs.values()) {
            final JsonObject o = new JsonObject();
            o.addProperty(r.kind.equals("ore") ? "rep" : "name", r.detail);
            o.addProperty("source", "/btsquests dumpids; used by " + String.join(", ", r.usedBy));
            o.addProperty("status", r.found ? "verified" : "missing");
            if ("item".equals(r.kind)) items.add(r.id, o);
            else if ("fluid".equals(r.kind)) fluids.add(r.id, o);
            else ores.add(r.id, o);
        }
        root.add("items", items);
        root.add("fluids", fluids);
        root.add("ore", ores);
        final File dir = new File("config", BeyondTheStars.MODID);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("cannot create " + dir);
        final File out = new File(dir, "questbook-registry.json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(
                new GsonBuilder().setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(root));
            w.write('\n');
        }
        return out;
    }

    // ======================================================================== events

    /**
     * Refreshes {@link QuestbookState#progressFile}: every player's progress on the quests of ours
     * present now, merged over the rows already there (rows are never deleted, so a default load
     * that removed our quests cannot wipe the backup on the next save).
     */
    static void backupProgress() {
        try {
            final File world = BQBridge.worldDir();
            if (world == null) return;
            final Map<UUID, Object> questMap = BQBridge.quests();
            // One quest at a time: the List argument of QuestDatabase.writeProgressToNBT filters
            // PLAYERS, not quests (javap: QuestInstance.writeProgressToNBT tests users.contains).
            Map<UUID, NBTTagCompound> backup = null;
            for (final Map.Entry<UUID, Object> e : questMap.entrySet()) {
                if (e.getKey()
                    .getMostSignificantBits() != BQBridge.ID_HIGH || e.getValue() == null) continue;
                if (backup == null) backup = QuestbookState.readProgressBackup(world);
                final NBTTagCompound row = (NBTTagCompound) e.getValue()
                    .getClass()
                    .getMethod("writeProgressToNBT", NBTTagCompound.class, List.class)
                    .invoke(e.getValue(), new NBTTagCompound(), null);
                BQBridge.writeId(row, "questID", e.getKey());
                row.setString("btsBundle", installedBundle);
                // Whether it was a checkbox placeholder when saved (how the generator builds one:
                // a lone checkbox that does not count as a quest), so a later install can refuse to
                // hand a ticked box's completion to the real quest even without the state file.
                final NBTTagCompound def = BQBridge.questNbt(e.getValue());
                row.setBoolean("btsPlaceholder", isLoneCheckbox(def) && countAsQuest(def) == 0);
                backup.put(e.getKey(), row);
            }
            if (backup != null) QuestbookState.writeProgressBackup(world, backup);
        } catch (final Throwable t) {
            BeyondTheStars.LOG.warn("Questbook: could not back up progress on our quests: " + BQBridge.describe(t));
        }
    }

    /** For {@link QuestbookEvents}: the line to show an op on login, or null. */
    static String loginNotice() {
        return loginNotice;
    }
}
