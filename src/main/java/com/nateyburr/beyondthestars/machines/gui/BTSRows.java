package com.nateyburr.beyondthestars.machines.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Controller GUI cleanup (controller-gui-spec.md §4.3, §4.5, §7 "How"): the terminal's rows as ONE synced string. The
 * server decides which rows a state shows and fills them ({@link Builder}); the client decodes the string once per
 * change ({@link #decode}) and its fixed pool of rows ({@link BTSGuiKit#rowPool}) draws row i with the component of
 * its {@link Kind}. So a machine's GUI logic lives in one server method, a sync only happens when the shown text
 * changes (the values are rounded to what is displayed), and a row that is not wanted simply is not sent.
 * <p>
 * Wire format: rows joined by {@link #ROW}; a row is its kind's ordinal then its fields, joined by {@link #FIELD};
 * tooltip lines joined by {@link #LINE}; list items (chips, lamps) joined by {@link #ITEM}, their parts by
 * {@link #PART}. Every text field is a {@link BTSText} encoding.
 */
public final class BTSRows {

    public static final char ROW = '\u0001';
    public static final char FIELD = '\u0002';
    public static final char LINE = '\u0007';
    public static final char ITEM = '\u0008';
    public static final char PART = '\u000E';

    /** The row kinds of spec §4.3 (the stepper and the info toggle live in the gap row, not here). */
    public enum Kind {
        /** sym, value, centre (NaN: 0), tick (NaN: none), tooltip. */
        GAUGE,
        /** sym1, v1, c1, sym2 ("" = none), v2, c2, tooltip. */
        GAUGE2,
        /** label, frac, value, gradient ordinal, ticks ("f:colour;…"), tooltip. */
        BAR,
        /** state ordinal, label, value, frac (−1 none), tooltip. */
        CHECK,
        /** label, value, tooltip. */
        STAT,
        /** label1, value1, label2, value2, tooltip. */
        STAT2,
        /** text, right text. */
        SECTION,
        /** icon (0 none, 1 ✓, 2 ✗, 3 !), colour, text, tooltip. */
        NOTE,
        /** items: dot colour, name, tooltip. */
        CHIPS,
        /** label, items: state (0 off/grey, 1 green, 2 red), name. */
        LAMPS,
        /** text, colour (wrapped, pitch 10). */
        PARA,
        /** a spacer of n pixels. */
        GAP
    }

    /** Note icons. */
    public static final int ICON_NONE = 0, ICON_OK = 1, ICON_BAD = 2, ICON_WARN = 3;

    /** One decoded row. */
    public static final class Row {

        public final Kind kind;
        private final String[] f;

        Row(Kind kind, String[] f) {
            this.kind = kind;
            this.f = f;
        }

        /** Field i, or "" past the end. */
        public String s(int i) {
            return i < f.length ? f[i] : "";
        }

        /** Field i as a number (NaN when absent or unreadable). */
        public double d(int i) {
            final String s = s(i);
            if (s.isEmpty()) return Double.NaN;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        public int i(int i) {
            final double v = d(i);
            return Double.isNaN(v) ? 0 : (int) v;
        }

        /** Field i as tooltip lines (encoded texts). */
        public List<String> lines(int i) {
            final String s = s(i);
            if (s.isEmpty()) return Collections.emptyList();
            final List<String> out = new ArrayList<>();
            for (String l : s.split(String.valueOf(LINE), -1)) if (!l.isEmpty()) out.add(l);
            return out;
        }

        /** Field i as list items, each split into its parts. */
        public List<String[]> items(int i) {
            final String s = s(i);
            if (s.isEmpty()) return Collections.emptyList();
            final List<String[]> out = new ArrayList<>();
            for (String it : s.split(String.valueOf(ITEM), -1)) out.add(it.split(String.valueOf(PART), -1));
            return out;
        }
    }

    private BTSRows() {}

    /** Decodes the synced string (client; empty list for ""). */
    public static List<Row> decode(String enc) {
        if (enc == null || enc.isEmpty()) return Collections.emptyList();
        final List<Row> out = new ArrayList<>();
        for (String r : enc.split(String.valueOf(ROW), -1)) {
            if (r.isEmpty()) continue;
            final String[] parts = r.split(String.valueOf(FIELD), -1);
            final int k;
            try {
                k = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (k < 0 || k >= Kind.values().length) continue;
            final String[] f = new String[parts.length - 1];
            System.arraycopy(parts, 1, f, 0, f.length);
            out.add(new Row(Kind.values()[k], f));
        }
        return out;
    }

    /** A row as one line of plain text (bench dumps, logs): the kind and its resolved texts and numbers. */
    public static String describe(Row r) {
        final StringBuilder sb = new StringBuilder(r.kind.name()).append(':');
        switch (r.kind) {
            case GAUGE:
                sb.append(' ')
                    .append(r.s(0))
                    .append(' ')
                    .append(r.s(1));
                if (!Double.isNaN(r.d(2))) sb.append(" centre ")
                    .append(r.s(2));
                if (!Double.isNaN(r.d(3))) sb.append(" tick ")
                    .append(r.s(3));
                break;
            case GAUGE2:
                sb.append(' ')
                    .append(r.s(0))
                    .append(' ')
                    .append(r.s(1));
                if (!r.s(3)
                    .isEmpty())
                    sb.append(" | ")
                        .append(r.s(3))
                        .append(' ')
                        .append(r.s(4));
                break;
            case BAR:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(0)))
                    .append(" [")
                    .append(r.s(1))
                    .append("] ")
                    .append(BTSText.resolve(r.s(2)));
                break;
            case CHECK:
                sb.append(r.i(0) == 0 ? " [OK] " : r.i(0) == 1 ? " [X] " : " [!] ")
                    .append(BTSText.resolve(r.s(1)))
                    .append(" = ")
                    .append(BTSText.resolve(r.s(2)));
                break;
            case STAT:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(0)))
                    .append(" = ")
                    .append(BTSText.resolve(r.s(1)));
                break;
            case STAT2:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(0)))
                    .append(" = ")
                    .append(BTSText.resolve(r.s(1)))
                    .append(" | ")
                    .append(BTSText.resolve(r.s(2)))
                    .append(" = ")
                    .append(BTSText.resolve(r.s(3)));
                break;
            case SECTION:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(0)));
                if (!r.s(1)
                    .isEmpty())
                    sb.append(" (")
                        .append(BTSText.resolve(r.s(1)))
                        .append(')');
                break;
            case NOTE:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(2)));
                break;
            case PARA:
                sb.append(' ')
                    .append(BTSText.resolve(r.s(0)));
                break;
            case CHIPS:
            case LAMPS:
                for (String[] it : r.items(r.kind == Kind.CHIPS ? 0 : 1)) {
                    sb.append(' ')
                        .append(BTSText.resolve(it.length > 1 ? it[1] : ""));
                }
                break;
            default:
                break;
        }
        final int tip = r.kind == Kind.GAUGE ? 4
            : r.kind == Kind.GAUGE2 ? 6
                : r.kind == Kind.BAR ? 5
                    : r.kind == Kind.CHECK ? 4
                        : r.kind == Kind.STAT ? 2 : r.kind == Kind.STAT2 ? 4 : r.kind == Kind.NOTE ? 3 : -1;
        if (tip >= 0) for (String l : r.lines(tip)) sb.append("  {")
            .append(BTSText.resolve(l))
            .append('}');
        return sb.toString()
            .replaceAll("§.", "");
    }

    /** Server side: appends rows in display order. */
    public static final class Builder {

        private final StringBuilder sb = new StringBuilder();
        private int count;

        private Builder row(Kind kind, Object... fields) {
            if (sb.length() > 0) sb.append(ROW);
            sb.append(kind.ordinal());
            for (Object f : fields) {
                sb.append(FIELD);
                sb.append(f == null ? "" : field(f));
            }
            count++;
            return this;
        }

        private static String field(Object f) {
            if (f instanceof Double || f instanceof Float) {
                final double d = ((Number) f).doubleValue();
                if (Double.isNaN(d)) return "";
                return String.format(Locale.ROOT, "%.4f", d);
            }
            return String.valueOf(f);
        }

        private static String tip(String... lines) {
            if (lines == null) return "";
            final StringBuilder t = new StringBuilder();
            for (String l : lines) {
                if (l == null || l.isEmpty()) continue;
                if (t.length() > 0) t.append(LINE);
                t.append(l);
            }
            return t.toString();
        }

        private static String tip(List<String> lines) {
            return tip(lines.toArray(new String[0]));
        }

        /** A gauge (−120..+120 with the zone bands about {@code centre}); NaN centre = 0, NaN tick = none. */
        public Builder gauge(String sym, double value, double centre, double tick, String... tooltip) {
            return row(Kind.GAUGE, sym, value, centre, tick, tip(tooltip));
        }

        /** Two half-width gauges on one row; {@code sym2} "" for a lone left half. */
        public Builder gaugePair(String sym1, double v1, double c1, String sym2, double v2, double c2,
            String... tooltip) {
            return row(Kind.GAUGE2, sym1, v1, c1, sym2, v2, c2, tip(tooltip));
        }

        /** A labelled bar; ticks are {@link BTSGuiKit#tick} strings. */
        public Builder bar(String label, double frac, String value, BTSGuiKit.Grad grad, String ticks,
            String... tooltip) {
            return row(
                Kind.BAR,
                label,
                Math.max(0, Math.min(1, frac)),
                value,
                grad.ordinal(),
                ticks == null ? "" : ticks,
                tip(tooltip));
        }

        public Builder check(BTSChecklist.State state, String label, String value, double frac, String... tooltip) {
            return row(Kind.CHECK, state.ordinal(), label, value, frac, tip(tooltip));
        }

        public Builder checks(BTSChecklist list) {
            for (BTSChecklist.CheckLine l : list.lines()) {
                row(Kind.CHECK, l.state.ordinal(), l.label, l.value, (double) l.frac, tip(l.tooltip));
            }
            return this;
        }

        public Builder stat(String label, String value, String... tooltip) {
            return row(Kind.STAT, label, value, tip(tooltip));
        }

        public Builder stat2(String label1, String value1, String label2, String value2, String... tooltip) {
            return row(Kind.STAT2, label1, value1, label2, value2, tip(tooltip));
        }

        /** A section label with a rule, or with a right-aligned text (a count) instead of the rule. */
        public Builder section(String text, String right) {
            return row(Kind.SECTION, text, right == null ? "" : right);
        }

        /** One line that scrolls on hover; {@code colour} ARGB. */
        public Builder note(int icon, int colour, String text, String... tooltip) {
            return row(Kind.NOTE, icon, colour, text, tip(tooltip));
        }

        /** Chips: {@code items} of {dot colour, name, tooltip}. */
        public Builder chips(List<String[]> items) {
            return row(Kind.CHIPS, items(items));
        }

        /** Lamps: {@code items} of {state, name}. */
        public Builder lamps(String label, List<String[]> items) {
            return row(Kind.LAMPS, label, items(items));
        }

        public Builder para(String text, int colour) {
            return row(Kind.PARA, text, colour);
        }

        public Builder gap(int px) {
            return row(Kind.GAP, px);
        }

        private static String items(List<String[]> items) {
            final StringBuilder t = new StringBuilder();
            for (String[] it : items) {
                if (t.length() > 0) t.append(ITEM);
                for (int i = 0; i < it.length; i++) {
                    if (i > 0) t.append(PART);
                    t.append(it[i] == null ? "" : it[i]);
                }
            }
            return t.toString();
        }

        public int count() {
            return count;
        }

        public String build() {
            return sb.toString();
        }

        /** The tooltip list helper for callers holding a List. */
        public static String tooltip(List<String> lines) {
            return tip(lines);
        }
    }
}
