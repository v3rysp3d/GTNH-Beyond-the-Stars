package com.nateyburr.beyondthestars;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The questbook bundle the jar carries, {@code assets/beyondthestars/questbook/overlay.json}
 * (docs/questbook-integration.md §4.2, written by {@code tools/gen-singularity.js}). It holds
 * every chapter and quest of ours in BetterQuesting's typed-key JSON, the grafts of our quests
 * into pack chapters (with their positions for the edited and the unedited pack), and the pack
 * edits with their variants already resolved against the build's feature flags.
 * <p>
 * Parsed with Gson; every typed-key object is turned into NBT by BetterQuesting's own converter
 * ({@link BQBridge#jsonToNbt}), so this class needs BetterQuesting at load time and is only ever
 * touched when it is present.
 */
final class QuestbookBundle {

    static final String RESOURCE = "/assets/beyondthestars/questbook/overlay.json";

    /** One chapter of ours: a QuestLine.json compound plus its entries. */
    static final class Chapter {

        String key;
        UUID id;
        /** "afterPackLine" (directly after {@link #after}, in bundle order) or "last". */
        String order;
        UUID after;
        NBTTagCompound line;
        /** questId -> {x, y, sizeX, sizeY} */
        final Map<UUID, NBTTagCompound> entries = new LinkedHashMap<>();
    }

    /** One of our quests placed in a pack chapter. */
    static final class Graft {

        UUID quest;
        UUID line;
        int[] on;
        int[] off;
    }

    /** One quest of ours. */
    static final class Quest {

        long low;
        UUID id;
        String key;
        boolean placeholder;
        final List<String> needs = new ArrayList<>();
        /** The pack edit whose absence this fallback quest stands in for, or null. */
        String onlyWithout;
        NBTTagCompound nbt;
    }

    /** One pack edit (§3, §4.4). */
    static final class Edit {

        String id;
        UUID quest;
        String questB64;
        String expectName;
        UUID expectChapter;
        final List<String> config = new ArrayList<>();
        /** "keep" or "completionOnly" (§4.6). */
        String progress;
        final List<JsonObject> ops = new ArrayList<>();

        /** Every chapter this edit moves the quest into or out of. */
        boolean touchesLine(UUID line) {
            for (final JsonObject op : ops) {
                if (!"move".equals(str(op, "op"))) continue;
                if (line.equals(BQBridge.decodeId(str(op, "from"))) || line.equals(BQBridge.decodeId(str(op, "to"))))
                    return true;
            }
            return false;
        }
    }

    /**
     * One pack chapter edit ({@code packChapterEdits}): today only {@code hideWhenEmpty}, a pack
     * chapter that our edits leave with no quest at all is taken out of the book while it is empty.
     */
    static final class ChapterEdit {

        String id;
        UUID line;
        String expectName;
        String op;
    }

    String version;
    int packVersion;
    String sourceHead = "";
    final Map<String, Boolean> features = new LinkedHashMap<>();
    final List<Chapter> chapters = new ArrayList<>();
    final List<Graft> grafts = new ArrayList<>();
    final Map<UUID, Quest> quests = new LinkedHashMap<>();
    final List<Edit> edits = new ArrayList<>();
    final List<ChapterEdit> chapterEdits = new ArrayList<>();

    private QuestbookBundle() {}

    /** True when the jar carries a bundle at all (no BetterQuesting needed to ask). */
    static boolean present() {
        return QuestbookBundle.class.getResource(RESOURCE) != null;
    }

    private static QuestbookBundle cached;

    /** The jar's bundle, parsed once per JVM; null when the jar has none. */
    static synchronized QuestbookBundle load() throws Exception {
        if (cached != null) return cached;
        final InputStream in = QuestbookBundle.class.getResourceAsStream(RESOURCE);
        if (in == null) return null;
        final JsonObject root;
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader)
                .getAsJsonObject();
        }
        cached = parse(root);
        return cached;
    }

    private static QuestbookBundle parse(JsonObject root) throws Exception {
        final QuestbookBundle b = new QuestbookBundle();
        if (root.has("format") && root.get("format")
            .getAsInt() != 1) throw new IllegalStateException(
                "overlay.json format " + root.get("format")
                    .getAsInt() + " is newer than this installer (1)");
        b.version = str(root, "bundleVersion");
        b.packVersion = root.has("packVersion") ? root.get("packVersion")
            .getAsInt() : 0;
        if (root.has("source") && root.getAsJsonObject("source")
            .has("srcHead")) b.sourceHead = str(root.getAsJsonObject("source"), "srcHead");
        if (root.has("features")) {
            for (final Map.Entry<String, JsonElement> e : root.getAsJsonObject("features")
                .entrySet())
                b.features.put(
                    e.getKey(),
                    e.getValue()
                        .getAsBoolean());
        }
        for (final JsonElement el : array(root, "chapters")) {
            final JsonObject o = el.getAsJsonObject();
            final Chapter c = new Chapter();
            c.key = str(o, "key");
            c.id = BQBridge.decodeId(str(o, "b64"));
            c.order = str(o, "order");
            c.after = o.has("after") ? BQBridge.decodeId(str(o, "after")) : null;
            c.line = BQBridge.jsonToNbt(o.getAsJsonObject("line"));
            for (final JsonElement en : array(o, "entries")) {
                final NBTTagCompound nbt = BQBridge.jsonToNbt(en.getAsJsonObject());
                final UUID quest = BQBridge.readId(nbt, "questID");
                if (quest != null) c.entries.put(quest, nbt);
            }
            b.chapters.add(c);
        }
        for (final JsonElement el : array(root, "grafts")) {
            final JsonObject o = el.getAsJsonObject();
            final Graft g = new Graft();
            g.quest = new UUID(
                BQBridge.ID_HIGH,
                o.get("quest")
                    .getAsLong());
            g.line = BQBridge.decodeId(str(o, "line"));
            g.on = pos(o.getAsJsonObject("on"));
            g.off = pos(o.getAsJsonObject(o.has("off") ? "off" : "on"));
            b.grafts.add(g);
        }
        for (final JsonElement el : array(root, "quests")) {
            final JsonObject o = el.getAsJsonObject();
            final Quest q = new Quest();
            q.low = o.get("low")
                .getAsLong();
            q.id = new UUID(BQBridge.ID_HIGH, q.low);
            q.key = str(o, "key");
            q.placeholder = o.has("placeholder") && o.get("placeholder")
                .getAsBoolean();
            for (final JsonElement n : array(o, "needs")) q.needs.add(n.getAsString());
            q.onlyWithout = o.has("onlyWithout") ? str(o, "onlyWithout") : null;
            q.nbt = BQBridge.jsonToNbt(o.getAsJsonObject("nbt"));
            b.quests.put(q.id, q);
        }
        for (final JsonElement el : array(root, "packEdits")) {
            final JsonObject o = el.getAsJsonObject();
            final Edit e = new Edit();
            e.id = str(o, "id");
            e.questB64 = str(o, "quest");
            e.quest = BQBridge.decodeId(e.questB64);
            e.expectName = str(o, "expectName");
            e.expectChapter = BQBridge.decodeId(str(o, "expectChapter"));
            for (final JsonElement c : array(o, "config")) e.config.add(c.getAsString());
            e.progress = o.has("progress") ? str(o, "progress") : "keep";
            for (final JsonElement op : array(o, "ops")) e.ops.add(op.getAsJsonObject());
            b.edits.add(e);
        }
        for (final JsonElement el : array(root, "packChapterEdits")) {
            final JsonObject o = el.getAsJsonObject();
            final ChapterEdit c = new ChapterEdit();
            c.id = str(o, "id");
            c.line = BQBridge.decodeId(str(o, "line"));
            c.expectName = str(o, "expectName");
            c.op = str(o, "op");
            b.chapterEdits.add(c);
        }
        return b;
    }

    Edit edit(String id) {
        for (final Edit e : edits) if (e.id.equals(id)) return e;
        return null;
    }

    Chapter chapter(UUID id) {
        for (final Chapter c : chapters) if (c.id.equals(id)) return c;
        return null;
    }

    // ----------------------------------------------------------------- JSON helpers

    static String str(JsonObject o, String key) {
        final JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    static Iterable<JsonElement> array(JsonObject o, String key) {
        final JsonElement e = o.get(key);
        if (e == null || !e.isJsonArray()) return Collections.emptyList();
        final JsonArray a = e.getAsJsonArray();
        final List<JsonElement> out = new ArrayList<>();
        for (final JsonElement x : a) out.add(x);
        return out;
    }

    private static int[] pos(JsonObject o) {
        final int size = o.get("size")
            .getAsInt();
        return new int[] { o.get("x")
            .getAsInt(),
            o.get("y")
                .getAsInt(),
            size, size };
    }

    /**
     * A typed-key payload ({@code "tasks:9": {...}}, an item compound) wrapped under one key and
     * converted, so lists and compounds parse exactly as in a quest file.
     */
    static NBTTagCompound wrapped(String typedKey, JsonElement payload) throws Exception {
        final JsonObject wrap = new JsonObject();
        wrap.add(typedKey, payload);
        return BQBridge.jsonToNbt(wrap);
    }
}
