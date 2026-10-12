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
        final double a = Math.abs(l);
        if (a < 1e6) return String.format(Locale.ROOT, "%,d", Math.round(l));
        if (a < 1e9) return a >= 1e8 ? String.format(Locale.ROOT, "%.0fM", l / 1e6)
            : String.format(Locale.ROOT, "%.1fM", l / 1e6);
        if (a < 1e12) return String.format(Locale.ROOT, "%.2fG", l / 1e9);
        return String.format(Locale.ROOT, "%.2fT", l / 1e12);
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
        if (eu >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fG", eu / 1e9);
        if (eu >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", eu / 1e6);
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

    /** Rounds litres to 3 significant figures (what {@link #litres} can show), so a sync value changes rarely. */
    public static double sig3(double v) {
        if (v == 0 || Double.isNaN(v) || Double.isInfinite(v)) return v;
        final double mag = Math.pow(10, Math.floor(Math.log10(Math.abs(v))) - 2);
        return Math.round(v / mag) * mag;
    }
}
