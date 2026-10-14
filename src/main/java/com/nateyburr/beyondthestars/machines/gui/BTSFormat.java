package com.nateyburr.beyondthestars.machines.gui;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Controller GUI cleanup (controller-gui-spec.md §4.2): the number formats every kit GUI uses, so one machine never
 * writes "212,340,000 L" where another writes "212M". The terminal shows the compact forms; exact values go in the
 * tooltips ({@link #exact}). The server rounds what it syncs to what is shown, so a sync value only changes when the
 * text does.
 */
public final class BTSFormat {

    private BTSFormat() {}

    /** Litres, compact: {@code 5,000}, {@code 40.0M}, {@code 212M}, {@code 1.20G}, {@code 1.00T} (no unit). */
    public static String litres(double l) {
        // each unit ends where its rounded text would reach the next one ("1000.00G" is "1.00T")
        final double a = Math.abs(l);
        if (a < 999_999.5) return String.format(Locale.ROOT, "%,d", Math.round(l));
        if (a < 99.95e6) return String.format(Locale.ROOT, "%.1fM", l / 1e6);
        if (a < 999.5e6) return String.format(Locale.ROOT, "%.0fM", l / 1e6);
        // three significant figures, as the M range: "26.8G", "134G" (an Unbound hole's intake line fits its row)
        if (a < 9.995e9) return String.format(Locale.ROOT, "%.2fG", l / 1e9);
        if (a < 99.95e9) return String.format(Locale.ROOT, "%.1fG", l / 1e9);
        if (a < 999.5e9) return String.format(Locale.ROOT, "%.0fG", l / 1e9);
        if (a < 9.995e12) return String.format(Locale.ROOT, "%.2fT", l / 1e12);
        if (a < 99.95e12) return String.format(Locale.ROOT, "%.1fT", l / 1e12);
        return String.format(Locale.ROOT, "%.0fT", l / 1e12);
    }

    /** "{have} / {need}" in compact litres (no unit). */
    public static String haveNeed(double have, double need) {
        return litres(have) + " / " + litres(need);
    }

    /** EU per cycle / network: {@code 1.51e12} from 1e10 up, {@code %,d} below. */
    public static String eu(double eu) {
        if (Math.abs(eu) < 1e10) return String.format(Locale.ROOT, "%,d", Math.round(eu));
        return String.format(Locale.ROOT, "%.2e", eu)
            .replace("e+", "e");
    }

    /** {@link #eu(double)} for a big network balance. */
    public static String eu(BigInteger eu) {
        if (eu.bitLength() < 63) return eu(eu.doubleValue());
        return String.format(Locale.ROOT, "%.2e", eu.doubleValue())
            .replace("e+", "e");
    }

    /** EU/t, compact: {@code 512k}, {@code 8.39M}, {@code 1.25G} (as the Kerr GUI's compactEu). */
    public static String euT(long eu) {
        if (eu >= 999_995_000L) return String.format(Locale.ROOT, "%.2fG", eu / 1e9);
        if (eu >= 999_500L) return String.format(Locale.ROOT, "%.2fM", eu / 1e6);
        if (eu >= 10_000L) return String.format(Locale.ROOT, "%.0fk", eu / 1e3);
        return String.format(Locale.ROOT, "%,d", eu);
    }

    /** A signed gauge value, one decimal with an explicit sign: {@code +48.3}, {@code -6.3}. */
    public static String signed(double v) {
        return String.format(Locale.ROOT, "%+.1f", v);
    }

    /** One decimal below 100, whole above ({@code 16.1}, {@code 140}). */
    public static String fmt1(double v) {
        return Math.abs(v) >= 100 ? String.format(Locale.ROOT, "%,.0f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** Two decimals. */
    public static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Durations: {@code 21 s}, {@code 1m 47s}, {@code 2h 4m}; "-" when negative. */
    public static String duration(long seconds) {
        if (seconds < 0) return "-";
        if (seconds < 60) return seconds + " s";
        if (seconds < 3600) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds / 3600 + "h " + seconds / 60 % 60 + "m";
    }

    /** Exact values, for tooltips only: {@code 212,340,000}. */
    public static String exact(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    /** A percentage of a 0..1 fraction, whole: {@code 47%}. */
    public static String pct(double frac) {
        return String.format(Locale.ROOT, "%d%%", Math.round(Math.max(0, Math.min(1, frac)) * 100));
    }

    // ------------------------------------------------------------------ bursts in EU/t (smbh-v1.md §10.3)

    /**
     * 1 Bu/t of bursts = 10¹³ EU/t once a crown thermalises it (QuasarParams.EU_PER_BU; kept here so this class stays
     * free of engine imports). A player read "6,000 Bu/t" as 6,000 billion EU (2026-10-14): every Bu/t a player sees
     * goes through {@link #bu}, {@link #buEu} or {@link #buEuShort}, so the EU/t is always beside it.
     */
    public static final double EU_PER_BU = 1e13;

    /** The number of a Bu/t figure: {@code 0.81}, {@code 12.3}, {@code 4,712} (no unit). */
    public static String buNum(double bu) {
        final double a = Math.abs(bu);
        if (a >= 1000) return String.format(Locale.ROOT, "%,.0f", bu);
        if (a >= 10) return String.format(Locale.ROOT, "%.1f", bu);
        return String.format(Locale.ROOT, "%.2f", bu);
    }

    /**
     * A Bu/t number for a GUI row, where width is short: {@link #buNum} below 10,000 ({@code 9,788}), then
     * {@code 13.7k}, {@code 484k}, {@code 2.29M}, {@code 1.07G}: never wider than {@code 9,788}, so a "Bottom jet" row
     * keeps its "= 4.8e18 EU/t" whole (the exact figure goes in the row's tooltip).
     */
    public static String buCompact(double bu) {
        final double a = Math.abs(bu);
        if (a < 9_999.5) return buNum(bu);
        if (a < 99_950) return String.format(Locale.ROOT, "%.1fk", bu / 1e3);
        if (a < 999_500) return String.format(Locale.ROOT, "%.0fk", bu / 1e3);
        if (a < 999.995e6) return String.format(Locale.ROOT, "%.2fM", bu / 1e6);
        return String.format(Locale.ROOT, "%.2fG", bu / 1e9);
    }

    /**
     * EU/t in scientific form, two significant figures: {@code 9.8e16}, {@code 5.2e18}; below a million the plain
     * number ({@code 512,000}). No unit.
     */
    public static String euSci(double eu) {
        if (!Double.isFinite(eu)) return "-";
        if (Math.abs(eu) < 1e6) return String.format(Locale.ROOT, "%,d", Math.round(eu));
        return String.format(Locale.ROOT, "%.1e", eu)
            .replace("e+", "e")
            .replace("e0", "e");
    }

    /** The short-scale names {@link #euWords} uses, every 10³ from a thousand. */
    private static final String[] WORDS = { "thousand", "million", "billion", "trillion", "quadrillion", "quintillion",
        "sextillion", "septillion", "octillion" };

    /**
     * EU/t in plain words, two or three significant figures: {@code 98 quadrillion}, {@code 5.2 quintillion},
     * {@code 240 trillion} (quadrillion = 10¹⁵, quintillion = 10¹⁸). Below a thousand the plain number.
     */
    public static String euWords(double eu) {
        if (!Double.isFinite(eu)) return "-";
        final double a = Math.abs(eu);
        if (a < 999.5) return String.format(Locale.ROOT, "%,d", Math.round(eu));
        int k = (int) Math.floor(Math.log10(a) / 3);
        double v = eu / Math.pow(10, 3 * k);
        // 999.6 trillion rounds to "1,000 trillion": say "1.0 quadrillion"
        if (Math.abs(v) >= 999.5 && k < WORDS.length) {
            k++;
            v /= 1000;
        }
        k = Math.max(1, Math.min(WORDS.length, k));
        v = eu / Math.pow(10, 3 * k);
        final double av = Math.abs(v);
        final String num = av >= 99.5 ? String.format(Locale.ROOT, "%,.0f", v)
            : av >= 9.95 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
        return num + " " + WORDS[k - 1];
    }

    /** The EU/t of {@code bu} Bu/t, short: {@code 9.8e16 EU/t}. */
    public static String euOfBu(double bu) {
        return euSci(bu * EU_PER_BU) + " EU/t";
    }

    /**
     * A Bu/t figure with its EU/t, the full form for tooltips, NEI and quest text:
     * {@code 9,796 Bu/t (9.8e16 EU/t, 98 quadrillion)}.
     */
    public static String buEu(double bu) {
        final double eu = bu * EU_PER_BU;
        if (Math.abs(eu) < 999.5) return buNum(bu) + " Bu/t (0 EU/t)";
        return buNum(bu) + " Bu/t (" + euSci(eu) + " EU/t, " + euWords(eu) + ")";
    }

    /** The EU/t of {@code bu} Bu/t with its words, for a GUI value: {@code 9.8e16 EU/t (98 quadrillion)}. */
    public static String euValue(double bu) {
        final double eu = bu * EU_PER_BU;
        if (Math.abs(eu) < 999.5) return "0 EU/t";
        return euSci(eu) + " EU/t (" + euWords(eu) + ")";
    }

    /** A Bu/t figure with its EU/t where space is tight (scanner lines, GUI rows): {@code 9,796 Bu/t (9.8e16 EU/t)}. */
    public static String buEuShort(double bu) {
        return buNum(bu) + " Bu/t (" + euOfBu(bu) + ")";
    }

    /** Rounds litres to 3 significant figures (what {@link #litres} can show), so a sync value changes rarely. */
    public static double sig3(double v) {
        if (v == 0 || Double.isNaN(v) || Double.isInfinite(v)) return v;
        final double mag = Math.pow(10, Math.floor(Math.log10(Math.abs(v))) - 2);
        return Math.round(v / mag) * mag;
    }
}
