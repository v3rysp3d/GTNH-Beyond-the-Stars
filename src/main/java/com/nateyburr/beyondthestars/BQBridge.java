package com.nateyburr.beyondthestars;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

import com.google.gson.JsonObject;

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
 * <b>What we found in BetterQuesting</b> (3.8.89-GTNH, every member below checked with javap).
 * The database is a UUID-keyed {@code BiMap<UUID, IQuest>} ({@code QuestDatabase.INSTANCE}) plus
 * a {@code BiMap<UUID, IQuestLine>} ({@code QuestLineDatabase.INSTANCE}) with a separate order
 * list ({@code protected List<UUID> lineOrder}). Each chapter is itself a
 * {@code UuidDatabase<IQuestLineEntry>}, i.e. a {@code BiMap<UUID, entry>}, so chapters are read
 * and written through the plain {@link Map} interface. The {@code questIDHigh}/{@code questIDLow}
 * pair in the JSON is just the UUID's two halves, so our whole overlay is addressable as
 * {@code new UUID(190, low)}. Quest PROGRESS lives inside each quest object
 * ({@code {questIDHigh, questIDLow, completed[{uuid, ...}], tasks[{index, ...}]}}) and is
 * snapshotted and re-applied with {@code writeProgressToNBT}/{@code readProgressFromNBT} - exactly
 * how BetterQuesting's own {@code /bq_admin default load} avoids wiping it.
 * {@code QuestInstance.readFromNBT} rebuilds properties, tasks, rewards and prerequisites and
 * leaves the completion map alone; {@code resetUser(null, true)} clears it.
 * <p>
 * {@link QuestbookInstaller} is the engine built on these primitives (bundle, pack edits, state
 * file, progress rules); this class only knows BetterQuesting.
 */
final class BQBridge {

    static final String MOD_ID = "betterquesting";

    /**
     * Every quest and quest line this addon ships uses {@code questIDHigh = 190}. GTNH's own
     * quests all use high 0, so this single number identifies our overlay in any folder and
     * can never collide with the pack (see questbook/README.md).
     */
    static final long ID_HIGH = BTSQuestIds.ID_HIGH;

    /** The "Beyond the Stars" chapter (questLineIDHigh 190 / questLineIDLow 47990). */
    static final UUID LINE_UUID = new UUID(ID_HIGH, BTSQuestIds.LINE_PLANT);

    /** The "Unit One" entry quest grafted into the pack's How to Generate Power chapter. */
    static final UUID ENTRY_QUEST_UUID = new UUID(ID_HIGH, BTSQuestIds.ENTRY_QUEST);

    /**
     * The leading 12 base64 characters every one of our file names carries.
     * BetterQuesting names each file {@code <Name>-<base64 of the 16 UUID bytes>.json}
     * (url-safe alphabet, see {@code UuidConverter.encodeUuid}). Twelve characters cover the
     * first nine bytes: the whole high word plus the top byte of the low word - which is zero
     * for every id we use - so this prefix matches all of ours and none of the pack's, and it
     * lets the legacy installer pick our files out of a folder without opening any of them.
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

    // ------------------------------------------------------------------- the databases

    /**
     * One of BetterQuesting's live databases, through its own public accessor
     * {@code QuestingAPI.getAPI(ApiReference.<key>)}. It hands back the very same
     * {@code QuestDatabase.INSTANCE} / {@code QuestLineDatabase.INSTANCE} objects, and there is
     * no API operation for "load a quest line into this world" - which is why everything here
     * is composed out of the databases' own public methods.
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
    static Object questDb() throws Exception {
        return database("QUEST_DB", "betterquesting.questing.QuestDatabase");
    }

    /** The live chapter database (UUID -> IQuestLine, plus a separate order list). */
    static Object lineDb() throws Exception {
        return database("LINE_DB", "betterquesting.questing.QuestLineDatabase");
    }

    @SuppressWarnings("unchecked")
    static Map<UUID, Object> quests() throws Exception {
        return (Map<UUID, Object>) questDb();
    }

    @SuppressWarnings("unchecked")
    static Map<UUID, Object> lines() throws Exception {
        return (Map<UUID, Object>) lineDb();
    }

    /** A chapter's entries: {@code QuestLine extends UuidDatabase<IQuestLineEntry>}, a BiMap. */
    @SuppressWarnings("unchecked")
    static Map<UUID, Object> entries(Object line) {
        return (Map<UUID, Object>) line;
    }

    // ------------------------------------------------------------------------- quests

    /** {@code QuestInstance.writeToNBT(new NBTTagCompound())}: properties, tasks, rewards, preRequisites. */
    static NBTTagCompound questNbt(Object quest) throws Exception {
        return (NBTTagCompound) quest.getClass()
            .getMethod("writeToNBT", NBTTagCompound.class)
            .invoke(quest, new NBTTagCompound());
    }

    /** {@code QuestInstance.readFromNBT}: replaces definition, keeps the completion map. */
    static void readQuest(Object quest, NBTTagCompound nbt) throws Exception {
        quest.getClass()
            .getMethod("readFromNBT", NBTTagCompound.class)
            .invoke(quest, nbt);
    }

    /** {@code QuestDatabase.createNew(UUID)}: a fresh QuestInstance put under that id. */
    static Object createQuest(UUID id) throws Exception {
        final Object db = questDb();
        return db.getClass()
            .getMethod("createNew", UUID.class)
            .invoke(db, id);
    }

    /**
     * Removes a quest from every chapter and from the database. {@code QuestDatabase.remove}
     * also strips the id from every other quest's prerequisites; it does NOT touch chapters,
     * which is why the entries go first.
     */
    static void removeQuest(UUID id) throws Exception {
        removeFromAllLines(id);
        quests().remove(id);
    }

    /** Every chapter entry of this quest, removed. */
    static void removeFromAllLines(UUID quest) throws Exception {
        for (final Object line : lines().values()) {
            if (line != null) entries(line).remove(quest);
        }
    }

    /**
     * {@code QuestInstance.resetUser(null, true)}: clears every player's completion and every
     * task's progress on this quest.
     */
    static void resetQuest(Object quest) throws Exception {
        quest.getClass()
            .getMethod("resetUser", UUID.class, boolean.class)
            .invoke(quest, null, true);
    }

    // ----------------------------------------------------------------------- chapters

    /** A new {@code QuestLine} read from a QuestLine.json-shaped compound (properties; no entries). */
    static Object newLine(NBTTagCompound nbt) throws Exception {
        final Object line = Class.forName("betterquesting.questing.QuestLine")
            .getDeclaredConstructor()
            .newInstance();
        line.getClass()
            .getMethod("readFromNBT", NBTTagCompound.class)
            .invoke(line, nbt);
        return line;
    }

    /** {@code QuestLine.writeToNBT}: properties plus its entries, as {@link #newLine} reads them back. */
    static NBTTagCompound lineNbt(Object line) throws Exception {
        return (NBTTagCompound) line.getClass()
            .getMethod("writeToNBT", NBTTagCompound.class)
            .invoke(line, new NBTTagCompound());
    }

    /** A {@code QuestLineEntry} from {@code {x, y, sizeX, sizeY}}. */
    static Object newEntry(NBTTagCompound nbt) throws Exception {
        return Class.forName("betterquesting.questing.QuestLineEntry")
            .getConstructor(NBTTagCompound.class)
            .newInstance(nbt);
    }

    static NBTTagCompound entryNbt(int x, int y, int sizeX, int sizeY) {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", x);
        nbt.setInteger("y", y);
        nbt.setInteger("sizeX", sizeX);
        nbt.setInteger("sizeY", sizeY);
        return nbt;
    }

    /** {@code QuestLineEntry.writeToNBT}: {@code {sizeX, sizeY, x, y}}. */
    static NBTTagCompound entryNbt(Object entry) throws Exception {
        return (NBTTagCompound) entry.getClass()
            .getMethod("writeToNBT", NBTTagCompound.class)
            .invoke(entry, new NBTTagCompound());
    }

    /** Puts (or replaces) a whole chapter; its order slot is set separately. */
    static void putLine(UUID id, Object line) throws Exception {
        lines().put(id, line);
    }

    /**
     * Removes a chapter AND its slot in the order list: {@code UuidDatabase.remove} leaves
     * {@code lineOrder} alone, and a stale id there would shift every later chapter's index.
     */
    static void removeLine(UUID id) throws Exception {
        lines().remove(id);
        final List<?> order = lineOrder();
        if (order != null) order.remove(id);
    }

    /** {@code QuestLineDatabase.lineOrder}, or null if this build renamed it. */
    private static List<?> lineOrder() {
        try {
            final Object db = lineDb();
            Class<?> type = db.getClass();
            while (type != null) {
                try {
                    final Field field = type.getDeclaredField("lineOrder");
                    field.setAccessible(true);
                    return (List<?>) field.get(db);
                } catch (final NoSuchFieldException next) {
                    type = type.getSuperclass();
                }
            }
        } catch (final Throwable ignored) {
            // Order list unreachable: removal still works, the slot just stays.
        }
        return null;
    }

    /** {@code getOrderIndex}: -1 if the chapter is absent (an absent order slot is appended). */
    static int orderIndex(UUID id) throws Exception {
        final Object db = lineDb();
        return (Integer) db.getClass()
            .getMethod("getOrderIndex", UUID.class)
            .invoke(db, id);
    }

    /** {@code setOrderIndex}: removes the id from the order list and re-inserts it, clamped. */
    static void setOrderIndex(UUID id, int index) throws Exception {
        final Object db = lineDb();
        db.getClass()
            .getMethod("setOrderIndex", UUID.class, int.class)
            .invoke(db, id, index);
    }

    // ----------------------------------------------------------------------- progress

    /** Every player's progress on every quest, the way {@code /bq_admin default load} snapshots it. */
    static NBTTagList writeProgress() throws Exception {
        final Object db = questDb();
        return (NBTTagList) db.getClass()
            .getMethod("writeProgressToNBT", NBTTagList.class, List.class)
            .invoke(db, new NBTTagList(), null);
    }

    /** {@code readProgressFromNBT(list, merge)}: entries for quests no longer present are skipped. */
    static void readProgress(NBTTagList progress, boolean merge) throws Exception {
        final Object db = questDb();
        db.getClass()
            .getMethod("readProgressFromNBT", NBTTagList.class, boolean.class)
            .invoke(db, progress, merge);
    }

    // ------------------------------------------------------------- persist and resync

    /**
     * markDirty schedules the world's QuestDatabase.json write; sendReset is what
     * {@code /bq_user refresh} uses to re-push the whole book. A null target broadcasts: every
     * client drops its copy and asks for a fresh one.
     */
    static void saveAndResync() throws Exception {
        final Object saveLoad = Class.forName("betterquesting.handlers.SaveLoadHandler")
            .getField("INSTANCE")
            .get(null);
        saveLoad.getClass()
            .getMethod("markDirty")
            .invoke(saveLoad);
        Class.forName("betterquesting.network.handlers.NetBulkSync")
            .getMethod("sendReset", EntityPlayerMP.class, boolean.class, boolean.class)
            .invoke(null, null, true, true);
    }

    /**
     * The loaded world's {@code pack_version} ({@code QuestSettings.INSTANCE.getProperty(NativeProps.PACK_VER)},
     * javap-checked on 3.8.89-GTNH), or -1 if it cannot be read. BetterQuesting copies it from
     * {@code DefaultQuests/QuestSettings.json} on every default load, including the automatic one
     * it runs when the pack's number changes, so a change of it means the pack's quests were
     * reloaded from a different pack.
     */
    static int packVersion() {
        try {
            final Object settings = Class.forName("betterquesting.storage.QuestSettings")
                .getField("INSTANCE")
                .get(null);
            final Object key = Class.forName("betterquesting.api.properties.NativeProps")
                .getField("PACK_VER")
                .get(null);
            final Object v = settings.getClass()
                .getMethod("getProperty", Class.forName("betterquesting.api.properties.IPropertyType"))
                .invoke(settings, key);
            return v instanceof Integer ? (Integer) v : -1;
        } catch (final Throwable t) {
            return -1;
        }
    }

    /** The loaded world's save folder ({@code saves/<world>} or the server's level folder). */
    static File worldDir() {
        return DimensionManager.getCurrentSaveRootDirectory();
    }

    // --------------------------------------------------------------------- id helpers

    /**
     * A UUID the way BetterQuesting spells it in a file name: the 16 bytes, high word first,
     * url-safe base64, padding stripped (mirrors {@code UuidConverter.encodeUuidStripPadding},
     * and a stripped string is a prefix of the padded one, so it matches either spelling).
     */
    static String encodeId(UUID id) {
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

    /** The inverse of {@link #encodeId}: a {@code DefaultQuests} file-name id (padded or not). */
    static UUID decodeId(String b64) {
        String text = b64.trim();
        while (text.length() % 4 != 0) text += "=";
        final byte[] bytes = java.util.Base64.getUrlDecoder()
            .decode(text);
        if (bytes.length != 16) throw new IllegalArgumentException("not a 16-byte quest id: " + b64);
        long high = 0;
        long low = 0;
        for (int i = 0; i < 8; i++) high = (high << 8) | (bytes[i] & 0xFF);
        for (int i = 8; i < 16; i++) low = (low << 8) | (bytes[i] & 0xFF);
        return new UUID(high, low);
    }

    /** {@code {questIDHigh, questIDLow}} (or questLineID*) as the UUID it is; null if absent. */
    static UUID readId(NBTTagCompound nbt, String prefix) {
        if (!nbt.hasKey(prefix + "High") || !nbt.hasKey(prefix + "Low")) return null;
        return new UUID(nbt.getLong(prefix + "High"), nbt.getLong(prefix + "Low"));
    }

    static void writeId(NBTTagCompound nbt, String prefix, UUID id) {
        nbt.setLong(prefix + "High", id.getMostSignificantBits());
        nbt.setLong(prefix + "Low", id.getLeastSignificantBits());
    }

    /** "190/48100" for ours, the base64 id for the pack's. */
    static String label(UUID id) {
        return id.getMostSignificantBits() == ID_HIGH ? ID_HIGH + "/" + id.getLeastSignificantBits() : encodeId(id);
    }

    // ------------------------------------------------------------------- JSON -> NBT

    /**
     * BetterQuesting's typed-key JSON ({@code "name:8"}, {@code "tasks:9"}) to NBT through its own
     * converter, so every suffix parses exactly the way {@code DefaultQuests} files do. Gson keeps
     * a number's digits until it is read, so 64-bit pack ids survive.
     */
    static NBTTagCompound jsonToNbt(JsonObject json) throws Exception {
        final Class<?> converter = Class.forName("betterquesting.api.utils.NBTConverter");
        return (NBTTagCompound) converter
            .getMethod("JSONtoNBT_Object", JsonObject.class, NBTTagCompound.class, boolean.class)
            .invoke(null, json, new NBTTagCompound(), true);
    }

    /** JSON file -> NBT through BetterQuesting's own reader and converter. */
    private static NBTTagCompound readJson(File file) throws Exception {
        final Object json = Class.forName("betterquesting.api.utils.JsonHelper")
            .getMethod("ReadFromFile", File.class)
            .invoke(null, file);
        return jsonToNbt((JsonObject) json);
    }

    static String describe(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        final String message = cause.getMessage();
        return cause.getClass()
            .getSimpleName() + (message == null ? "" : ": " + message);
    }

    // ------------------------------------------------- legacy path: from DefaultQuests

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

    /** Outcome of one {@link #installFromDefaults} run. */
    static final class LegacyInstall {

        boolean ok;
        String error = "";
        String defaultsPath = "";
        int questsAdded;
        int questsUpdated;
        int chapterEntries;
        int graftedEntries;
        final List<String> notes = new ArrayList<>();
    }

    /**
     * The pre-bundle installer, kept as the fallback for a jar that carries no
     * {@code overlay.json}: adds every quest and chapter of ours (questIDHigh 190) from the
     * instance's {@code DefaultQuests} folder into the live database. It never edits a pack
     * quest. Progress is snapshotted before the write and restored after it, on every path.
     */
    static LegacyInstall installFromDefaults() {
        final LegacyInstall result = new LegacyInstall();
        try {
            final File defaults = defaultsDir();
            result.defaultsPath = defaults.getAbsolutePath();
            if (!defaults.isDirectory()) {
                result.error = "no DefaultQuests folder at " + result.defaultsPath
                    + " - merge the overlay from questbook/ into the instance first";
                return result;
            }
            final Map<UUID, Object> questMap = quests();
            final Map<UUID, Object> lineMap = lines();
            final NBTTagList progress = writeProgress();
            try {
                final LinkedHashSet<UUID> wanted = new LinkedHashSet<>();
                final File[] lineDirs = new File(defaults, "QuestLines").listFiles();
                if (lineDirs != null) {
                    Arrays.sort(lineDirs);
                    for (final File dir : lineDirs) {
                        if (!dir.isDirectory()) continue;
                        final File lineFile = new File(dir, "QuestLine.json");
                        if (!lineFile.isFile()) continue;
                        final UUID lineId = readId(readJson(lineFile), "questLineID");
                        if (lineId == null) continue;
                        final boolean ours = lineId.getMostSignificantBits() == ID_HIGH;
                        final List<File> entryFiles = new ArrayList<>();
                        for (final File file : jsonFilesIn(dir)) {
                            if (file.getName()
                                .equals("QuestLine.json")) continue;
                            if (ours || file.getName()
                                .contains(OUR_ID_PREFIX)) entryFiles.add(file);
                        }
                        if (!ours && entryFiles.isEmpty()) continue;
                        final Object line = ours ? newLine(readJson(lineFile)) : lineMap.get(lineId);
                        if (line == null) {
                            result.notes.add(
                                "pack chapter " + dir.getName()
                                    + " is not in this world's database - its entry quest was skipped");
                            continue;
                        }
                        for (final File file : entryFiles) {
                            final NBTTagCompound nbt = readJson(file);
                            final UUID questId = readId(nbt, "questID");
                            if (questId == null || questId.getMostSignificantBits() != ID_HIGH) continue;
                            entries(line).put(questId, newEntry(nbt));
                            wanted.add(questId);
                            if (ours) result.chapterEntries++;
                            else result.graftedEntries++;
                        }
                        if (ours) {
                            putLine(lineId, line);
                            setOrderIndex(lineId, Integer.MAX_VALUE);
                        }
                    }
                }
                final List<File> questFiles = jsonFilesUnder(new File(defaults, "Quests"));
                for (final UUID id : wanted) {
                    final File file = fileFor(questFiles, id);
                    if (file == null) {
                        result.notes.add("no definition file under Quests/ for quest " + id + " - it was skipped");
                        continue;
                    }
                    Object quest = questMap.get(id);
                    if (quest == null) {
                        quest = createQuest(id);
                        result.questsAdded++;
                    } else {
                        result.questsUpdated++;
                    }
                    readQuest(quest, readJson(file));
                }
            } finally {
                readProgress(progress, true);
            }
            saveAndResync();
            result.ok = true;
        } catch (final Throwable t) {
            result.error = describe(t);
        }
        return result;
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
        if (files == null) return Collections.emptyList();
        final List<File> out = new ArrayList<>();
        for (final File file : files) {
            if (file.isFile() && file.getName()
                .endsWith(".json")) out.add(file);
        }
        return out;
    }

    /** Every *.json at or below {@code root}; BetterQuesting walks the tree, so this does too. */
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
}
