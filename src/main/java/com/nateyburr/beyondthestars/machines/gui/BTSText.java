package com.nateyburr.beyondthestars.machines.gui;

import net.minecraft.util.StatCollector;

/**
 * Controller GUI cleanup (controller-gui-spec.md §4.5, §6): text the SERVER builds and the CLIENT translates. A text is
 * a run of segments joined by {@link #SEG}; a segment is a literal, or a lang reference {@code \u0003key\u0004arg…}
 * whose arguments are literals or, with a {@link #KEY} prefix, lang keys translated too. So every new player-facing
 * word goes through {@code en_US.lang} although the row is decided on the server, and a translated client shows its
 * own language. Fluid and item names are sent already localised (the server's {@code getLocalizedName}, as the GUIs
 * did before), and the runtime sentences the scanner shares stay English (spec Q6).
 */
public final class BTSText {

    /** Starts a lang reference segment. */
    public static final char LANG = '\u0003';
    /** Separates a lang reference's key and arguments. */
    public static final char ARG = '\u0004';
    /** Prefix of an argument that is itself a lang key. */
    public static final char KEY = '\u0005';
    /** Separates segments. */
    public static final char SEG = '\u0006';

    private BTSText() {}

    /** A literal, with the codec's control characters stripped. */
    public static String lit(Object s) {
        final String str = String.valueOf(s);
        StringBuilder out = null;
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c < 0x10 && c != '\t') {
                if (out == null) out = new StringBuilder(str.substring(0, i));
                continue;
            }
            if (out != null) out.append(c);
        }
        return out == null ? str : out.toString();
    }

    /** A lang reference; each argument is a literal, or {@link #key(String)} to translate it too. */
    public static String lang(String key, Object... args) {
        final StringBuilder sb = new StringBuilder().append(LANG)
            .append(key);
        for (Object a : args) {
            sb.append(ARG);
            final String s = String.valueOf(a);
            if (!s.isEmpty() && s.charAt(0) == KEY) sb.append(KEY)
                .append(lit(s.substring(1)));
            else sb.append(lit(s));
        }
        return sb.toString();
    }

    /** An argument for {@link #lang} that the client translates. */
    public static String key(String key) {
        return KEY + key;
    }

    /** Joins encoded texts (and literals: pass them through {@link #lit} first) into one text. */
    public static String cat(String... parts) {
        final StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(SEG);
            sb.append(p);
        }
        return sb.toString();
    }

    /** The displayed string of an encoded text (client side; works on the server too, in its own language). */
    public static String resolve(String enc) {
        if (enc == null || enc.isEmpty()) return "";
        if (enc.indexOf(LANG) < 0 && enc.indexOf(SEG) < 0) return enc;
        final StringBuilder out = new StringBuilder();
        int start = 0;
        while (start <= enc.length()) {
            int end = enc.indexOf(SEG, start);
            if (end < 0) end = enc.length();
            out.append(segment(enc.substring(start, end)));
            start = end + 1;
        }
        return out.toString();
    }

    private static String segment(String seg) {
        if (seg.isEmpty() || seg.charAt(0) != LANG) return seg;
        final String[] parts = seg.substring(1)
            .split(String.valueOf(ARG), -1);
        if (parts.length == 1) return StatCollector.translateToLocal(parts[0]);
        final Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            final String a = parts[i];
            args[i - 1] = !a.isEmpty() && a.charAt(0) == KEY ? StatCollector.translateToLocal(a.substring(1)) : a;
        }
        return StatCollector.translateToLocalFormatted(parts[0], args);
    }
}
