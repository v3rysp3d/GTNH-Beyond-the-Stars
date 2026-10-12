package com.nateyburr.beyondthestars.machines;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * SINGULARITY LADDER shape loader (singularity-ladder-build.md §5.2, Contract additions FC-3).
 * <p>
 * The Kerr and the Eye are 15,000 to 35,000 placed blocks in bounding boxes of up to 800,000 cells.
 * A {@code String[][]} literal that size does not fit in a Java method or constant pool, so the
 * shapes live as resource text files generated from {@code designs/voxel.js} by
 * {@code tools/export-structures.js}, and this class turns them back into StructureLib's native
 * {@code String[depth][height]} layout, once, at first use.
 *
 * <h2>File format (the shared contract)</h2>
 *
 * <pre>
 * # piece=kerr_t1 w=76 h=65 d=79 off=38,20,0 sha=...
 * &lt;h rows of w chars, top row first&gt;     layer 0 (depth 0)
 * ---
 * &lt;h rows&gt;                               layer 1
 * ...
 * </pre>
 *
 * Char index within a row is the horizontal axis. {@code off} is the controller's
 * (horizontal, vertical, depth) offset, passed straight to {@code checkPiece} / {@code buildPiece} /
 * {@code survivalBuildPiece}. {@code \r} is stripped (git may check the files out CRLF); rows shorter
 * than {@code w} are right-padded with spaces (an editor may have stripped trailing whitespace);
 * anything else that disagrees with the header throws, so a broken shape fails the controller's
 * class-init loudly instead of quietly forming nothing.
 * <p>
 * <b>Trap 1 (client vs server):</b> this is pure resource I/O with no world or side state, so the
 * hologram's client-side {@code construct} path sees exactly what the server's check sees.
 * <p>
 * Callers must not mutate the returned arrays: they are the cached copy every controller shares.
 */
public final class BTSShapes {

    private static final String ROOT = "/assets/beyondthestars/structures/";

    private static final Map<String, Shape> CACHE = new ConcurrentHashMap<>();
    private static volatile JsonObject manifest;

    private BTSShapes() {}

    /** One parsed piece. */
    private static final class Shape {

        final String name;
        final int w;
        final int h;
        final int d;
        final int[] off;
        final String sha;
        final String[][] layers;

        Shape(String name, int w, int h, int d, int[] off, String sha, String[][] layers) {
            this.name = name;
            this.w = w;
            this.h = h;
            this.d = d;
            this.off = off;
            this.sha = sha;
            this.layers = layers;
        }
    }

    /** The piece's shape as {@code [depth][height]}, each string {@code w} chars. Cached. */
    public static String[][] load(String piece) {
        return shape(piece).layers;
    }

    /** The controller offset into the piece: {@code {horizontal, vertical, depth}}. */
    public static int[] offset(String piece) {
        return shape(piece).off.clone();
    }

    /** The piece's trimmed size: {@code {w, h, d}}. */
    public static int[] size(String piece) {
        final Shape s = shape(piece);
        return new int[] { s.w, s.h, s.d };
    }

    /** The sha1 of the voxel.js the piece was exported from (the header's {@code sha=}). */
    public static String sha(String piece) {
        return shape(piece).sha;
    }

    /** How many cells of the piece hold {@code c}. Counted from the shape itself. */
    public static int count(String piece, char c) {
        int n = 0;
        for (String[] layer : shape(piece).layers) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == c) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** {@code structures/manifest.json}, parsed once. Throws if it is missing or not an object. */
    public static JsonObject manifest() {
        JsonObject m = manifest;
        if (m == null) {
            synchronized (BTSShapes.class) {
                m = manifest;
                if (m == null) {
                    final String text = String.join("\n", readLines(ROOT + "manifest.json"));
                    final JsonElement root;
                    try {
                        root = new JsonParser().parse(text);
                    } catch (RuntimeException bad) {
                        throw new IllegalStateException("[BTS] " + ROOT + "manifest.json is not valid JSON", bad);
                    }
                    if (root == null || !root.isJsonObject()) {
                        throw new IllegalStateException("[BTS] " + ROOT + "manifest.json is not a JSON object");
                    }
                    m = root.getAsJsonObject();
                    manifest = m;
                }
            }
        }
        return m;
    }

    /**
     * The manifest entry for one piece: {@code manifest.pieces.<piece>} when the root has a
     * {@code pieces} object, else {@code manifest.<piece>}. Null if the piece is not listed.
     */
    public static JsonObject manifestPiece(String piece) {
        final JsonObject root = manifest();
        final JsonObject scope = root.has("pieces") && root.get("pieces")
            .isJsonObject() ? root.getAsJsonObject("pieces") : root;
        final JsonElement entry = scope.get(piece);
        return entry != null && entry.isJsonObject() ? entry.getAsJsonObject() : null;
    }

    /**
     * Every socket of one char in a piece, as (a, b, c) offsets from THAT piece's controller
     * ({@code sockets.<ch>[i].rel} in {@code manifest.json}; for a Kerr module piece the module's own
     * controller). Empty if none.
     */
    public static int[][] socketRels(String piece, char ch) {
        try {
            final JsonObject entry = manifestPiece(piece);
            if (entry == null || !entry.has("sockets")) return new int[0][];
            final JsonObject all = entry.getAsJsonObject("sockets");
            final String key = String.valueOf(ch);
            if (!all.has(key)) return new int[0][];
            final JsonArray list = all.getAsJsonArray(key);
            final int[][] out = new int[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                final JsonArray rel = list.get(i)
                    .getAsJsonObject()
                    .getAsJsonArray("rel");
                out[i] = new int[] { rel.get(0)
                    .getAsInt(),
                    rel.get(1)
                        .getAsInt(),
                    rel.get(2)
                        .getAsInt() };
            }
            return out;
        } catch (RuntimeException bad) {
            return new int[0][];
        }
    }

    // ------------------------------------------------------------------------------------------

    private static Shape shape(String piece) {
        final Shape cached = CACHE.get(piece);
        if (cached != null) {
            return cached;
        }
        final Shape parsed = parse(piece, readLines(ROOT + piece + ".txt"));
        CACHE.putIfAbsent(piece, parsed);
        return CACHE.get(piece);
    }

    private static List<String> readLines(String resource) {
        final InputStream in = BTSShapes.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException(
                "[BTS] structure resource " + resource
                    + " is missing from the jar. Regenerate it with tools/export-structures.js.");
        }
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.replace("\r", ""));
            }
        } catch (IOException io) {
            throw new IllegalStateException("[BTS] could not read structure resource " + resource, io);
        }
        return lines;
    }

    private static Shape parse(String piece, List<String> lines) {
        final String where = ROOT + piece + ".txt";
        int i = 0;
        while (i < lines.size() && lines.get(i)
            .trim()
            .isEmpty()) {
            i++;
        }
        if (i >= lines.size() || !lines.get(i)
            .startsWith("# piece=")) {
            throw new IllegalStateException("[BTS] " + where + ": the first line must be the '# piece=' header");
        }
        final String header = lines.get(i++);
        String name = null;
        String sha = "";
        int w = -1;
        int h = -1;
        int d = -1;
        int[] off = null;
        for (String token : header.substring(1)
            .trim()
            .split("\\s+")) {
            final int eq = token.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            final String key = token.substring(0, eq);
            final String value = token.substring(eq + 1);
            try {
                switch (key) {
                    case "piece":
                        name = value;
                        break;
                    case "w":
                        w = Integer.parseInt(value);
                        break;
                    case "h":
                        h = Integer.parseInt(value);
                        break;
                    case "d":
                        d = Integer.parseInt(value);
                        break;
                    case "off": {
                        final String[] parts = value.split(",");
                        if (parts.length != 3) {
                            throw new IllegalStateException("[BTS] " + where + ": off= needs three values");
                        }
                        off = new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()) };
                        break;
                    }
                    case "sha":
                        sha = value;
                        break;
                    default:
                        break; // unknown keys are allowed, so the exporter can add fields
                }
            } catch (NumberFormatException bad) {
                throw new IllegalStateException("[BTS] " + where + ": bad header value " + token, bad);
            }
        }
        if (name == null || w <= 0 || h <= 0 || d <= 0 || off == null) {
            throw new IllegalStateException(
                "[BTS] " + where + ": the header needs piece=, w=, h=, d= (all > 0) and off=; got: " + header);
        }
        if (!name.equals(piece)) {
            throw new IllegalStateException("[BTS] " + where + ": header names piece '" + name + "'");
        }

        final List<String[]> layers = new ArrayList<>(d);
        List<String> rows = new ArrayList<>(h);
        for (; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.startsWith("#")) {
                continue;
            }
            if (line.equals("---")) {
                layers.add(finishLayer(where, layers.size(), rows, w, h));
                rows = new ArrayList<>(h);
                continue;
            }
            rows.add(line);
        }
        // The last layer has no trailing separator. Trailing blank lines (a final newline, or an
        // editor's) are not rows: drop them before the count check.
        while (!rows.isEmpty() && rows.size() > h
            && rows.get(rows.size() - 1)
                .isEmpty()) {
            rows.remove(rows.size() - 1);
        }
        if (!rows.isEmpty()) {
            layers.add(finishLayer(where, layers.size(), rows, w, h));
        }
        if (layers.size() != d) {
            throw new IllegalStateException(
                "[BTS] " + where + ": header says d=" + d + " layers but the file has " + layers.size());
        }
        return new Shape(name, w, h, d, off, sha, layers.toArray(new String[0][]));
    }

    private static String[] finishLayer(String where, int index, List<String> rows, int w, int h) {
        if (rows.size() != h) {
            throw new IllegalStateException(
                "[BTS] " + where + ": layer " + index + " has " + rows.size() + " rows, header says h=" + h);
        }
        final String[] out = new String[h];
        for (int r = 0; r < h; r++) {
            final String row = rows.get(r);
            if (row.length() > w) {
                throw new IllegalStateException(
                    "[BTS] " + where
                        + ": layer "
                        + index
                        + " row "
                        + r
                        + " is "
                        + row.length()
                        + " chars, header says w="
                        + w);
            }
            out[r] = row.length() == w ? row : pad(row, w);
        }
        return out;
    }

    private static String pad(String row, int w) {
        final StringBuilder sb = new StringBuilder(w).append(row);
        while (sb.length() < w) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
