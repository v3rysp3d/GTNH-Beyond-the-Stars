package com.nateyburr.beyondthestars;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * What the questbook installer did to ONE world, kept beside its save:
 * {@code saves/<world>/beyondthestars/questbook-state.dat} (docs/questbook-integration.md §4.5).
 * <p>
 * It is NBT, not the JSON the spec sketched: it stores whole pack quests, and NBT round-trips
 * them bit for bit (64-bit ids, tag types, float values), which is what lets
 * {@code /btsquests uninstall} restore a pack quest byte-identical to what the pack shipped.
 * <ul>
 * <li>the applied bundle version and the config key it was applied under;</li>
 * <li>per applied pack edit: the pack quest's ORIGINAL definition and chapter entries (captured
 * the first time the edit was applied and never overwritten while it stays applied) and the
 * definition as applied (to notice a {@code /bq_admin default load} or a hand edit);</li>
 * <li>which quests of ours were checkbox placeholders at the last install;</li>
 * <li>the last install's report (skipped edits and per-op misses), for {@code status} and the
 * op login line;</li>
 * <li>a sticky uninstall marker, so the auto-install does not undo {@code /btsquests uninstall}.</li>
 * </ul>
 */
final class QuestbookState {

    static final int FORMAT = 1;

    /** One applied pack edit. */
    static final class Applied {

        UUID quest;
        /** The pack quest as the pack wrote it ({@code QuestInstance.writeToNBT}). */
        NBTTagCompound original;
        /** Its chapter entries before the edit: lineId -> {x, y, sizeX, sizeY}. */
        final Map<UUID, NBTTagCompound> entries = new LinkedHashMap<>();
        /** The quest as the edit left it. */
        NBTTagCompound applied;
        /** Its chapter entries as the edit left them (a default load puts moved quests back). */
        final Map<UUID, NBTTagCompound> appliedEntries = new LinkedHashMap<>();
    }

    String bundleVersion = "";
    String configKey = "";
    /** "" = installed (or never touched), "edits" or "all" = an op uninstalled that much. */
    String uninstalled = "";
    final Map<String, Applied> edits = new LinkedHashMap<>();
    final Set<Long> placeholders = new LinkedHashSet<>();
    /** editId -> why it was skipped at the last install. */
    final Map<String, String> skipped = new LinkedHashMap<>();
    /** editId -> why it is switched off (a flag, an uninstall): not a warning. */
    final Map<String, String> disabled = new LinkedHashMap<>();
    /** editId -> ops of it that were skipped (text not found, prerequisite missing). */
    final Map<String, List<String>> misses = new LinkedHashMap<>();
    /** Set when the file was read; false = a world this installer never touched. */
    boolean existed;

    static File file(File worldDir) {
        return new File(new File(worldDir, BeyondTheStars.MODID), "questbook-state.dat");
    }

    static QuestbookState read(File worldDir) throws Exception {
        final QuestbookState s = new QuestbookState();
        if (worldDir == null) return s;
        final File f = file(worldDir);
        if (!f.isFile()) return s;
        final NBTTagCompound root = readNbt(f);
        s.existed = true;
        s.bundleVersion = root.getString("bundleVersion");
        s.configKey = root.getString("configKey");
        s.uninstalled = root.getString("uninstalled");
        final NBTTagCompound edits = root.getCompoundTag("edits");
        for (final Object key : edits.func_150296_c()) {
            final NBTTagCompound e = edits.getCompoundTag((String) key);
            final Applied a = new Applied();
            a.quest = BQBridge.readId(e, "questID");
            a.original = e.getCompoundTag("original");
            a.applied = e.getCompoundTag("applied");
            readEntries(e.getTagList("entries", 10), a.entries);
            readEntries(e.getTagList("appliedEntries", 10), a.appliedEntries);
            if (a.quest != null) s.edits.put((String) key, a);
        }
        for (final int low : root.getIntArray("placeholders")) s.placeholders.add((long) low);
        final NBTTagCompound skipped = root.getCompoundTag("skipped");
        for (final Object key : skipped.func_150296_c()) s.skipped.put((String) key, skipped.getString((String) key));
        final NBTTagCompound disabled = root.getCompoundTag("disabled");
        for (final Object key : disabled.func_150296_c())
            s.disabled.put((String) key, disabled.getString((String) key));
        final NBTTagCompound misses = root.getCompoundTag("misses");
        for (final Object key : misses.func_150296_c()) {
            final NBTTagList list = misses.getTagList((String) key, 8);
            final List<String> out = new ArrayList<>();
            for (int i = 0; i < list.tagCount(); i++) out.add(list.getStringTagAt(i));
            s.misses.put((String) key, out);
        }
        return s;
    }

    void write(File worldDir) throws Exception {
        if (worldDir == null) return;
        final NBTTagCompound root = new NBTTagCompound();
        root.setInteger("format", FORMAT);
        root.setString("bundleVersion", bundleVersion);
        root.setString("configKey", configKey);
        root.setString("uninstalled", uninstalled);
        final NBTTagCompound edits = new NBTTagCompound();
        for (final Map.Entry<String, Applied> e : this.edits.entrySet()) {
            final Applied a = e.getValue();
            final NBTTagCompound c = new NBTTagCompound();
            BQBridge.writeId(c, "questID", a.quest);
            c.setTag("original", a.original);
            c.setTag("applied", a.applied == null ? new NBTTagCompound() : a.applied);
            c.setTag("entries", writeEntries(a.entries));
            c.setTag("appliedEntries", writeEntries(a.appliedEntries));
            edits.setTag(e.getKey(), c);
        }
        root.setTag("edits", edits);
        final int[] lows = new int[placeholders.size()];
        int i = 0;
        for (final long low : placeholders) lows[i++] = (int) low;
        root.setIntArray("placeholders", lows);
        final NBTTagCompound skipped = new NBTTagCompound();
        for (final Map.Entry<String, String> e : this.skipped.entrySet()) skipped.setString(e.getKey(), e.getValue());
        root.setTag("skipped", skipped);
        final NBTTagCompound disabled = new NBTTagCompound();
        for (final Map.Entry<String, String> e : this.disabled.entrySet()) disabled.setString(e.getKey(), e.getValue());
        root.setTag("disabled", disabled);
        final NBTTagCompound misses = new NBTTagCompound();
        for (final Map.Entry<String, List<String>> e : this.misses.entrySet()) {
            final NBTTagList list = new NBTTagList();
            for (final String m : e.getValue()) list.appendTag(new NBTTagString(m));
            misses.setTag(e.getKey(), list);
        }
        root.setTag("misses", misses);
        writeNbt(file(worldDir), root);
    }

    private static void readEntries(NBTTagList list, Map<UUID, NBTTagCompound> into) {
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound en = list.getCompoundTagAt(i);
            into.put(BQBridge.readId(en, "questLineID"), en.getCompoundTag("entry"));
        }
    }

    private static NBTTagList writeEntries(Map<UUID, NBTTagCompound> entries) {
        final NBTTagList list = new NBTTagList();
        for (final Map.Entry<UUID, NBTTagCompound> en : entries.entrySet()) {
            final NBTTagCompound row = new NBTTagCompound();
            BQBridge.writeId(row, "questLineID", en.getKey());
            row.setTag("entry", en.getValue());
            list.appendTag(row);
        }
        return list;
    }

    // ------------------------------------------------------------ progress backup

    /**
     * {@code saves/<world>/beyondthestars/questbook-progress.dat}: every player's progress on OUR
     * quests (questIDHigh 190), refreshed on each world save. BetterQuesting's
     * {@code /bq_admin default load} - which it also runs by itself when the pack's
     * {@code pack_version} changes - clears the whole database and restores progress only for
     * quests that are in {@code DefaultQuests}; if our mirror was not merged there, our quests
     * come back from the jar with no progress, and this file is where it is put back from.
     * Rows are merged by quest id and never deleted, each tagged with the bundle it was taken
     * under ({@code btsBundle}).
     */
    static File progressFile(File worldDir) {
        return new File(new File(worldDir, BeyondTheStars.MODID), "questbook-progress.dat");
    }

    /** questId -> progress row ({@code QuestInstance.writeProgressToNBT} plus {@code btsBundle}). */
    static Map<UUID, NBTTagCompound> readProgressBackup(File worldDir) throws Exception {
        final Map<UUID, NBTTagCompound> rows = new LinkedHashMap<>();
        if (worldDir == null) return rows;
        final File f = progressFile(worldDir);
        if (!f.isFile()) return rows;
        final NBTTagList list = readNbt(f).getTagList("rows", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound row = list.getCompoundTagAt(i);
            final UUID id = BQBridge.readId(row, "questID");
            if (id != null) rows.put(id, row);
        }
        return rows;
    }

    static void writeProgressBackup(File worldDir, Map<UUID, NBTTagCompound> rows) throws Exception {
        if (worldDir == null) return;
        final NBTTagCompound root = new NBTTagCompound();
        root.setInteger("format", FORMAT);
        final NBTTagList list = new NBTTagList();
        for (final NBTTagCompound row : rows.values()) list.appendTag(row);
        root.setTag("rows", list);
        writeNbt(progressFile(worldDir), root);
    }

    // ---------------------------------------------------------------------- files

    private static NBTTagCompound readNbt(File f) throws Exception {
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            return CompressedStreamTools.readCompressed(in);
        }
    }

    /** Written to a temp file and renamed into place, so a crash mid-write never leaves half a file. */
    private static void writeNbt(File f, NBTTagCompound root) throws Exception {
        if (!f.getParentFile()
            .isDirectory()
            && !f.getParentFile()
                .mkdirs())
            throw new IllegalStateException("cannot create " + f.getParentFile());
        final File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
            CompressedStreamTools.writeCompressed(root, out);
        }
        if (f.exists() && !f.delete()) throw new IllegalStateException("cannot replace " + f);
        if (!tmp.renameTo(f)) throw new IllegalStateException("cannot rename " + tmp + " to " + f);
    }
}
