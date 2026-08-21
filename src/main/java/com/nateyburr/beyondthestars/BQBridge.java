package com.nateyburr.beyondthestars;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/**
 * Talks to BetterQuesting's LIVE quest database entirely by reflection, so this mod keeps no
 * compile-time or runtime dependency on it and loads fine in a pack without it.
 * <p>
 * <b>Why this exists.</b> BetterQuesting reads {@code config/betterquesting/DefaultQuests/}
 * exactly once - when a world is created. From then on the world owns a private copy at
 * {@code saves/<world>/betterquesting/QuestDatabase.json} and never looks at the defaults
 * again. Dropping our overlay into an instance therefore shows the quest line in NEW worlds
 * only; an existing save shows nothing, no matter how correct the files are.
 * <p>
 * <b>What we found in BetterQuesting.</b> The database is a UUID-keyed
 * {@code BiMap<UUID, IQuest>} ({@code betterquesting.questing.QuestDatabase.INSTANCE}) plus a
 * {@code BiMap<UUID, IQuestLine>} ({@code QuestLineDatabase.INSTANCE}) with a separate order
 * list. The {@code questIDHigh}/{@code questIDLow} pair in the JSON is just the UUID's two
 * halves, so our whole overlay is addressable as {@code new UUID(190, low)}. Quest PROGRESS
 * lives inside each quest object and can be snapshotted and re-applied with
 * {@code writeProgressToNBT}/{@code readProgressFromNBT} - which is exactly how BetterQuesting's
 * own {@code /bq_admin default load} avoids wiping it. That gives a safe injection path:
 * snapshot progress, add ONLY our quests and chapter to the live database, restore progress,
 * push a bulk resync at the player. Nothing else in the world's database is read or written.
 */
final class BQBridge {

    static final String MOD_ID = "betterquesting";

    /**
     * Every quest and quest line this addon ships uses {@code questIDHigh = 190}. GTNH's own
     * quests all use high 0, so this single number identifies our overlay in any folder and
     * can never collide with the pack (see questbook/README.md).
     */
    static final long ID_HIGH = 190L;

    /** The "Beyond the Stars" chapter (questLineIDHigh 190 / questLineIDLow 47990). */
    static final UUID LINE_UUID = new UUID(ID_HIGH, 47990L);

    /** The "Unit One" entry quest grafted into the pack's How to Generate Power chapter. */
    static final UUID ENTRY_QUEST_UUID = new UUID(ID_HIGH, 48020L);

    /**
     * EVERY quest low our own chapter ships - what {@link #status()} counts - and a LIST rather
     * than a first/last RANGE on purpose.
     * <p>
     * It used to be the pair {@code 48000..48019}. When 0.21.0 appended the four plant-module
     * blueprints and The 100M Wall at {@code 48021..48025} the pair was not widened, so
     * {@code status} looked for twenty ids, found all twenty and reported "Everything is
     * loaded" while five quests were missing from the book. A range was never able to describe
     * this chapter anyway: {@code 48020} is the {@link #ENTRY_QUEST_UUID} grafted into the
     * pack's own chapter and is counted separately, so the chapter has a hole in the middle of
     * it by design.
     * <p>
     * {@code questbook/gen-questbook.ps1} reads this array back out of this file and throws if
     * it does not match the ids it just handed out. Appending a quest to the book without
     * adding its low here therefore fails the questbook build rather than shipping a status
     * command that lies.
     */
    static final long[] CHAPTER_QUEST_LOWS = { 48000L, 48001L, 48002L, 48003L, 48004L, 48005L, 48006L, 48007L, 48008L,
        48009L, 48010L, 48011L, 48012L, 48013L, 48014L, 48015L, 48016L, 48017L, 48018L, 48019L, 48021L, 48022L, 48023L,
        48024L, 48025L, 48026L, 48027L, 48028L };

    /**
     * The chapter's ids the way a person reads them - {@code 48000-48019, 48021-48028} -
     * collapsed into runs off {@link #CHAPTER_QUEST_LOWS} itself, so no message can advertise a
     * range the check does not actually count.
     */
    static String chapterQuestLabel() {
        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < CHAPTER_QUEST_LOWS.length) {
            int end = i;
            while (end + 1 < CHAPTER_QUEST_LOWS.length && CHAPTER_QUEST_LOWS[end + 1] == CHAPTER_QUEST_LOWS[end] + 1) {
                end++;
            }
            if (out.length() > 0) out.append(", ");
            out.append(CHAPTER_QUEST_LOWS[i]);
            if (end > i) out.append('-')
                .append(CHAPTER_QUEST_LOWS[end]);
            i = end + 1;
        }
        return out.toString();
    }

    /**
     * The leading 12 base64 characters every one of our file names carries.
     * BetterQuesting names each file {@code <Name>-<base64 of the 16 UUID bytes>.json}
     * (url-safe alphabet, see {@code UuidConverter.encodeUuid}). Twelve characters cover the
     * first nine bytes: the whole high word plus the top byte of the low word - which is zero
     * for every id we use - so this prefix matches all of ours and none of the pack's, and it
     * lets the installer pick our files out of a folder without opening any of them.
     */
    private static final String OUR_ID_PREFIX = encodeId(new UUID(ID_HIGH, 0L)).substring(0, 12);

    private BQBridge() {}

    // ------------------------------------------------------------------------ presence

    static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /** BetterQuesting's version string, or "unknown". */
    static String version() {
        for (final ModContainer mod : Loader.instance()
            .getActiveModList()) {
            if (MOD_ID.equals(mod.getModId())) return mod.getVersion();
        }
        return "unknown";
    }

    // ------------------------------------------------------- reading the live database

    /**
     * One of BetterQuesting's live databases, through its own public accessor
     * {@code QuestingAPI.getAPI(ApiReference.<key>)}. That accessor is the only part of the
     * public API that touches the live data at all: it hands back the very same
     * {@code QuestDatabase.INSTANCE} / {@code QuestLineDatabase.INSTANCE} objects, and there is
     * no API operation for "load a quest line into this world" - which is why the install
     * below has to compose one out of the databases' own public methods.
     *
     * @param key      name of an {@code ApiReference} field, e.g. QUEST_DB
     * @param fallback the concrete singleton class to read INSTANCE off, if the API moved
     */
    private static Object database(String key, String fallback) throws Exception {
        try {
            final Object apiKey = Class.forName("betterquesting.api.api.ApiReference")
                .getField(key)
                .get(null);
            final Object db = Class.forName("betterquesting.api.api.QuestingAPI")
                .getMethod("getAPI", Class.forName("betterquesting.api.api.ApiKey"))
                .invoke(null, apiKey);
            if (db != null) return db;
        } catch (final Throwable ignored) {
            // Fall through to the singleton the API would have returned anyway.
        }
        return Class.forName(fallback)
            .getField("INSTANCE")
            .get(null);
    }

    /** The live quest database (a Guava BiMap of UUID -> IQuest under the hood). */
    private static Object questDb() throws Exception {
        return database("QUEST_DB", "betterquesting.questing.QuestDatabase");
    }

    /** The live chapter database (UUID -> IQuestLine, plus a separate order list). */
    private static Object lineDb() throws Exception {
        return database("LINE_DB", "betterquesting.questing.QuestLineDatabase");
    }

    /** What the world's CURRENT database holds of ours. Never throws. */
    static Status status() {
        final Status status = new Status();
        try {
            final Map<?, ?> quests = (Map<?, ?>) questDb();
            final Map<?, ?> lines = (Map<?, ?>) lineDb();
            status.readable = true;
            status.totalQuests = quests.size();
            status.totalLines = lines.size();
            status.chapterPresent = lines.containsKey(LINE_UUID);
            status.entryQuestPresent = quests.containsKey(ENTRY_QUEST_UUID);
            status.expectedChapterQuests = CHAPTER_QUEST_LOWS.length;
            for (final long low : CHAPTER_QUEST_LOWS) {
                if (quests.containsKey(new UUID(ID_HIGH, low))) status.chapterQuests++;
                else status.missingQuestLows.add(low);
            }
        } catch (final Throwable t) {
            status.readable = false;
            status.error = describe(t);
        }
        return status;
    }

    /** A snapshot of what the live database holds of ours. */
    static final class Status {

        boolean readable;
        String error = "";
        int totalQuests;
        int totalLines;
        boolean chapterPresent;
        boolean entryQuestPresent;
        int chapterQuests;
        int expectedChapterQuests;
        /** The lows of {@link BQBridge#CHAPTER_QUEST_LOWS} this world's database does NOT hold. */
        final List<Long> missingQuestLows = new ArrayList<>();

        /**
         * Complete means every id in {@link BQBridge#CHAPTER_QUEST_LOWS} is present - not that a
         * COUNT matched, which is what let a 25-quest chapter report complete at twenty.
         */
        boolean complete() {
            return readable && chapterPresent && entryQuestPresent && missingQuestLows.isEmpty();
        }

        /** The missing lows, comma-separated - empty when nothing is missing. */
        String missingLabel() {
            final StringBuilder out = new StringBuilder();
            for (final long low : missingQuestLows) {
                if (out.length() > 0) out.append(", ");
                out.append(low);
            }
            return out.toString();
        }
    }

    // --------------------------------------------------------------------- the install

    /** Outcome of one {@link #install} run. */
    static final class Install {

        boolean ok;
        String error = "";
        String defaultsPath = "";
        int questsAdded;
        int questsUpdated;
        int chapterEntries;
        int graftedEntries;
        boolean chapterCreated;
        boolean synced;
        final List<String> notes = new ArrayList<>();
    }

    /**
     * Adds every quest and chapter this addon owns (questIDHigh 190) from the instance's
     * {@code DefaultQuests} folder into the LIVE database of the world the player is in, then
     * pushes a resync so the book redraws without a relog.
     * <p>
     * Player progress is snapshotted before the write and restored after it, so an existing
     * campaign keeps every completed quest. Quests belonging to the pack (questIDHigh 0) are
     * never read, written, cleared or reordered - the only change to a pack chapter is that
     * our own entry quest gets grafted into it as one extra icon.
     */
    static Install install() {
        final Install result = new Install();
        try {
            final File defaults = defaultsDir();
            result.defaultsPath = defaults.getAbsolutePath();
            if (!defaults.isDirectory()) {
                result.error = "no DefaultQuests folder at " + result.defaultsPath
                    + " - merge the overlay from questbook/ into the instance first";
                return result;
            }

            final Object questDb = questDb();
            final Object lineDb = lineDb();
            final Class<?> questDbClass = questDb.getClass();
            final Class<?> lineDbClass = lineDb.getClass();
            final Map<?, ?> questMap = (Map<?, ?>) questDb;
            final Map<?, ?> lineMap = (Map<?, ?>) lineDb;

            // 1. Snapshot every player's progress exactly the way `/bq_admin default load`
            // does, so nothing we touch can cost a completed quest.
            final Method writeProgress = questDbClass.getMethod("writeProgressToNBT", NBTTagList.class, List.class);
            final NBTTagList progress = (NBTTagList) writeProgress.invoke(questDb, new NBTTagList(), null);

            // Steps 2-3 mutate the live database. If anything throws in the middle, the
            // FINALLY below still puts the campaign back - without it a half-applied write
            // could strand every player's progress while the command reported success.
            try {
                // 2. The chapters, which are also the authority on WHICH quests are ours: a
                // QuestLine.json carrying our high word is a chapter we own and is rebuilt from
                // scratch, and any other chapter is the pack's, so only the entry files carrying
                // our high word are grafted into it and nothing else in it is read or written.
                final Method putLine = lineDbClass.getMethod("put", UUID.class, Object.class);
                final Method setOrderIndex = lineDbClass.getMethod("setOrderIndex", UUID.class, int.class);
                final java.util.LinkedHashSet<UUID> wanted = new java.util.LinkedHashSet<>();
                final File[] lineDirs = new File(defaults, "QuestLines").listFiles();
                if (lineDirs != null) {
                    Arrays.sort(lineDirs);
                    for (final File dir : lineDirs) {
                        if (!dir.isDirectory()) continue;
                        final File lineFile = new File(dir, "QuestLine.json");
                        if (!lineFile.isFile()) continue;
                        final UUID lineId = readId(readJson(lineFile), "QUEST_LINE");
                        if (lineId == null) continue;
                        final boolean ours = lineId.getMostSignificantBits() == ID_HIGH;

                        // A pack chapter is only interesting if one of our entries sits in it, and
                        // that shows in the file NAME - BetterQuesting suffixes every entry file
                        // with its quest's base64 UUID, so the pack's thousands of entry files
                        // never have to be opened.
                        final List<File> entryFiles = new ArrayList<>();
                        for (final File file : jsonFilesIn(dir)) {
                            if (file.getName()
                                .equals("QuestLine.json")) continue;
                            if (ours || file.getName()
                                .contains(OUR_ID_PREFIX)) entryFiles.add(file);
                        }
                        if (!ours && entryFiles.isEmpty()) continue;

                        Object line;
                        if (ours) {
                            line = Class.forName("betterquesting.questing.QuestLine")
                                .getDeclaredConstructor()
                                .newInstance();
                            line.getClass()
                                .getMethod("readFromNBT", NBTTagCompound.class)
                                .invoke(line, readJson(lineFile));
                            result.chapterCreated = !lineMap.containsKey(lineId);
                        } else {
                            line = lineMap.get(lineId);
                            if (line == null) {
                                result.notes.add(
                                    "pack chapter " + dir.getName()
                                        + " is not in this world's database - its entry quest was skipped");
                                continue;
                            }
                        }

                        // QuestLine extends UuidDatabase<IQuestLineEntry>, whose put(UUID, T)
                        // erases to put(UUID, Object) - the entry type never appears in the
                        // reflective signature.
                        final Method putEntry = line.getClass()
                            .getMethod("put", UUID.class, Object.class);
                        for (final File file : entryFiles) {
                            final NBTTagCompound nbt = readJson(file);
                            final UUID questId = readId(nbt, "QUEST");
                            if (questId == null || questId.getMostSignificantBits() != ID_HIGH) continue;
                            final Object entry = Class.forName("betterquesting.questing.QuestLineEntry")
                                .getConstructor(NBTTagCompound.class)
                                .newInstance(nbt);
                            putEntry.invoke(line, questId, entry);
                            wanted.add(questId);
                            if (ours) result.chapterEntries++;
                            else result.graftedEntries++;
                        }

                        if (ours) {
                            putLine.invoke(lineDb, lineId, line);
                            // Chapters are ordered by a separate list; an index past the end is
                            // clamped, so this simply parks ours last.
                            setOrderIndex.invoke(lineDb, lineId, Integer.MAX_VALUE);
                        }
                    }
                }

                // 3. The quest definitions for exactly those ids, found by the same base64 UUID
                // suffix their chapter entry files carry.
                final Method createQuest = questDbClass.getMethod("createNew", UUID.class);
                final List<File> questFiles = jsonFilesUnder(new File(defaults, "Quests"));
                for (final UUID id : wanted) {
                    final File file = fileFor(questFiles, id);
                    if (file == null) {
                        result.notes.add("no definition file under Quests/ for quest " + id + " - it was skipped");
                        continue;
                    }
                    final NBTTagCompound nbt = readJson(file);
                    Object quest = questMap.get(id);
                    if (quest == null) {
                        quest = createQuest.invoke(questDb, id);
                        result.questsAdded++;
                    } else {
                        result.questsUpdated++;
                    }
                    quest.getClass()
                        .getMethod("readFromNBT", NBTTagCompound.class)
                        .invoke(quest, nbt);
                }

            } finally {
                // 4. Put the campaign back exactly as it was - on every path.
                questDbClass.getMethod("readProgressFromNBT", NBTTagList.class, boolean.class)
                    .invoke(questDb, progress, true);
            }

            // 5. Persist and redraw. markDirty schedules the world's QuestDatabase.json write;
            // sendReset is what `/bq_user refresh` uses to re-push the whole book to a client.
            final Object saveLoad = Class.forName("betterquesting.handlers.SaveLoadHandler")
                .getField("INSTANCE")
                .get(null);
            saveLoad.getClass()
                .getMethod("markDirty")
                .invoke(saveLoad);
            // A null target broadcasts: every client drops its copy and asks for a fresh one,
            // which is a two-byte packet rather than a database push.
            Class.forName("betterquesting.network.handlers.NetBulkSync")
                .getMethod("sendReset", EntityPlayerMP.class, boolean.class, boolean.class)
                .invoke(null, null, true, true);
            result.synced = true;
            result.ok = true;
        } catch (final Throwable t) {
            result.error = describe(t);
        }
        return result;
    }

    // --------------------------------------------------------------------- reflection aids

    /** {@code <instance>/config/betterquesting/DefaultQuests}, per BQ's own settings. */
    static File defaultsDir() {
        try {
            final String dir = (String) Class.forName("betterquesting.api.storage.BQ_Settings")
                .getField("defaultDir")
                .get(null);
            if (dir != null && !dir.isEmpty()) return new File(dir, "DefaultQuests");
        } catch (final Throwable ignored) {
            // Fall through to the stock relative path.
        }
        return new File("config/betterquesting/DefaultQuests");
    }

    /** JSON -> NBT through BetterQuesting's own converter, so the typed key suffixes parse. */
    private static NBTTagCompound readJson(File file) throws Exception {
        final Object json = Class.forName("betterquesting.api.utils.JsonHelper")
            .getMethod("ReadFromFile", File.class)
            .invoke(null, file);
        final Class<?> converter = Class.forName("betterquesting.api.utils.NBTConverter");
        final Class<?> jsonObject = Class.forName("com.google.gson.JsonObject");
        return (NBTTagCompound) converter.getMethod("JSONtoNBT_Object", jsonObject, NBTTagCompound.class, boolean.class)
            .invoke(null, json, new NBTTagCompound(), true);
    }

    /**
     * Reads a questIDHigh/questIDLow (or questLineID*) pair as the UUID it really is.
     * {@code type} is the name of a {@code NBTConverter.UuidValueType} constant - "QUEST" or
     * "QUEST_LINE" - fetched as the public static field an enum constant is.
     */
    private static UUID readId(NBTTagCompound nbt, String type) {
        try {
            final Class<?> kindClass = Class.forName("betterquesting.api.utils.NBTConverter$UuidValueType");
            final Object kind = kindClass.getField(type)
                .get(null);
            return (UUID) kindClass.getMethod("readId", NBTTagCompound.class)
                .invoke(kind, nbt);
        } catch (final Throwable t) {
            return null;
        }
    }

    /**
     * A UUID the way BetterQuesting spells it in a file name: the 16 bytes, high word first,
     * url-safe base64, padding stripped (mirrors {@code UuidConverter.encodeUuidStripPadding},
     * and a stripped string is a prefix of the padded one, so it matches either spelling).
     */
    private static String encodeId(UUID id) {
        final byte[] bytes = new byte[16];
        long high = id.getMostSignificantBits();
        long low = id.getLeastSignificantBits();
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) high;
            high >>>= 8;
        }
        for (int i = 15; i >= 8; i--) {
            bytes[i] = (byte) low;
            low >>>= 8;
        }
        return java.util.Base64.getUrlEncoder()
            .encodeToString(bytes)
            .replace("=", "");
    }

    /** The quest definition file whose name carries this UUID, or null. */
    private static File fileFor(List<File> files, UUID id) {
        final String encoded = encodeId(id);
        for (final File file : files) {
            if (file.getName()
                .contains(encoded)) return file;
        }
        return null;
    }

    private static List<File> jsonFilesIn(File dir) {
        final File[] files = dir.listFiles();
        if (files == null) return java.util.Collections.emptyList();
        final List<File> out = new ArrayList<>();
        for (final File file : files) {
            if (file.isFile() && file.getName()
                .endsWith(".json")) out.add(file);
        }
        return out;
    }

    /**
     * Every *.json at or below {@code root}. BetterQuesting groups quest files in one folder
     * per chapter plus the MultipleQuestLine / NoQuestLine catch-alls, but it walks the tree
     * recursively rather than assuming a depth, so this does too.
     */
    private static List<File> jsonFilesUnder(File root) {
        final List<File> out = new ArrayList<>(jsonFilesIn(root));
        final File[] children = root.listFiles();
        if (children != null) {
            Arrays.sort(children);
            for (final File child : children) {
                if (child.isDirectory()) out.addAll(jsonFilesUnder(child));
            }
        }
        return out;
    }

    private static String describe(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        final String message = cause.getMessage();
        return cause.getClass()
            .getSimpleName() + (message == null ? "" : ": " + message);
    }
}
