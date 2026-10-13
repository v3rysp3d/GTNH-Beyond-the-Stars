package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * DEV-ONLY shot script for the video recorder ({@link RecClient}); the format is documented in
 * {@code docs/recording.md}.
 * <p>
 * One command a line, {@code #} starts a comment. Commands before the first {@code scene} line are the setup (never
 * recorded). Every {@code scene <name>} starts a scene: its commands run in order and each scene is written to its own
 * segment file. Only the recording commands ({@code card}, {@code hold}, {@code orbit}, {@code dolly}, {@code pan},
 * {@code timelapse}) put frames in the video; everything else (moves, waits, GUIs opening, builds) happens between
 * recorded spans and is cut out.
 */
final class RecScript {

    /** Every verb the recorder knows. */
    static final Set<String> VERBS = new HashSet<>(
        Arrays.asList(
            "scene",
            "caption",
            "card",
            "build",
            "goto",
            "look",
            "pan",
            "orbit",
            "dolly",
            "hold",
            "timelapse",
            "gui",
            "questbook",
            "close",
            "place",
            "hologram",
            "time",
            "wait",
            "cell",
            "command",
            "label",
            "kit",
            "nei",
            "aecells",
            "fov",
            "warp",
            "use",
            "terminal",
            "menet"));

    /** The verbs whose time is recorded. */
    static final Set<String> RECORDING = new HashSet<>(
        Arrays.asList("card", "hold", "orbit", "dolly", "pan", "timelapse"));

    /** Keys that take the next token as their value. */
    static final Set<String> KEYS = new HashSet<>(
        Arrays.asList(
            "radius",
            "height",
            "from",
            "to",
            "seconds",
            "look",
            "scale",
            "speed",
            "facing",
            "tier",
            "timeout",
            "ease",
            "until",
            "max",
            "tail",
            "budget",
            "every",
            "tab",
            "name",
            "gen",
            "side"));

    /** One command line. */
    static final class Cmd {

        final int line;
        final String verb;
        final List<String> pos = new ArrayList<>();
        final Map<String, String> kv = new HashMap<>();
        /** Everything after the verb, raw (captions and cards). */
        final String rest;

        Cmd(int line, String verb, String rest) {
            this.line = line;
            this.verb = verb;
            this.rest = rest;
        }

        String arg(int i) {
            if (i >= pos.size())
                throw new IllegalArgumentException("line " + line + ": '" + verb + "' needs argument " + (i + 1));
            return pos.get(i);
        }

        String opt(int i, String def) {
            return i < pos.size() ? pos.get(i) : def;
        }

        boolean flag(String f) {
            return pos.contains(f);
        }

        double num(String key, double def) {
            final String v = kv.get(key);
            if (v == null) return def;
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException bad) {
                throw new IllegalArgumentException("line " + line + ": '" + key + "' is not a number: " + v);
            }
        }

        double need(String key) {
            if (!kv.containsKey(key))
                throw new IllegalArgumentException("line " + line + ": '" + verb + "' needs '" + key + " <value>'");
            return num(key, 0);
        }

        boolean records() {
            return RECORDING.contains(verb);
        }

        @Override
        public String toString() {
            return verb + " " + rest;
        }
    }

    /** One scene: one segment file. */
    static final class Scene {

        final String name;
        final int line;
        String caption = "";
        final List<Cmd> cmds = new ArrayList<>();

        Scene(String name, int line) {
            this.name = name;
            this.line = line;
        }

        boolean setup() {
            return line == 0;
        }
    }

    final File file;
    /** The setup (index 0, never recorded), then the scenes in order. */
    final List<Scene> scenes = new ArrayList<>();

    private RecScript(File file) {
        this.file = file;
    }

    static RecScript load(File file) throws IOException {
        final RecScript s = new RecScript(file);
        final List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        Scene cur = new Scene("setup", 0);
        s.scenes.add(cur);
        for (int i = 0; i < lines.size(); i++) {
            final int n = i + 1;
            String raw = lines.get(i);
            if (n == 1 && raw.startsWith("﻿")) raw = raw.substring(1);
            final String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            final int sp = line.indexOf(' ');
            final String verb = (sp < 0 ? line : line.substring(0, sp)).toLowerCase(Locale.ROOT);
            final String rest = sp < 0 ? ""
                : line.substring(sp + 1)
                    .trim();
            if (!VERBS.contains(verb))
                throw new IllegalArgumentException(file.getName() + " line " + n + ": unknown command '" + verb + "'");
            if (verb.equals("scene")) {
                if (rest.isEmpty()) throw new IllegalArgumentException(file.getName() + " line " + n + ": scene name?");
                cur = new Scene(rest.replaceAll("[^A-Za-z0-9_.-]+", "_"), n);
                s.scenes.add(cur);
                continue;
            }
            if (verb.equals("caption")) {
                if (cur.setup()) throw new IllegalArgumentException(
                    file.getName() + " line " + n + ": caption before the first scene");
                // the first caption names the scene in the log; every caption is also a command, so a scene can
                // change its caption as it goes (a walk-through)
                if (cur.caption.isEmpty()) cur.caption = unquote(rest);
                cur.cmds.add(new Cmd(n, verb, unquote(rest)));
                continue;
            }
            final Cmd c = new Cmd(n, verb, rest);
            if (!verb.equals("card") && !verb.equals("command") && !verb.equals("label")) {
                final List<String> toks = tokens(rest);
                for (int k = 0; k < toks.size(); k++) {
                    final String t = toks.get(k);
                    final String key = t.toLowerCase(Locale.ROOT);
                    if (KEYS.contains(key) && k + 1 < toks.size()) c.kv.put(key, toks.get(++k));
                    else c.pos.add(t);
                }
            }
            if (cur.setup() && c.records()) throw new IllegalArgumentException(
                file.getName() + " line " + n + ": '" + verb + "' records, so it must be inside a scene");
            cur.cmds.add(c);
        }
        return s;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            final char ch = line.charAt(i);
            if (ch == '"') quoted = !quoted;
            else if (ch == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    static List<String> tokens(String s) {
        final List<String> out = new ArrayList<>();
        final StringBuilder b = new StringBuilder();
        boolean quoted = false, any = false;
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
                any = true;
            } else if (Character.isWhitespace(ch) && !quoted) {
                if (any) out.add(b.toString());
                b.setLength(0);
                any = false;
            } else {
                b.append(ch);
                any = true;
            }
        }
        if (any) out.add(b.toString());
        return out;
    }

    static String unquote(String s) {
        final String t = s.trim();
        return t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"") ? t.substring(1, t.length() - 1) : t;
    }

    /** {@code card <seconds> <line> | <line> ...}: the seconds and the lines. */
    static Object[] card(Cmd c) {
        final String rest = c.rest.trim();
        final int sp = rest.indexOf(' ');
        if (sp < 0) throw new IllegalArgumentException("line " + c.line + ": card <seconds> <title> | <line> ...");
        final double secs;
        try {
            secs = Double.parseDouble(rest.substring(0, sp));
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException("line " + c.line + ": card <seconds> first");
        }
        final List<String> lines = new ArrayList<>();
        for (String part : rest.substring(sp + 1)
            .split("\\|")) lines.add(unquote(part));
        return new Object[] { secs, lines };
    }
}
