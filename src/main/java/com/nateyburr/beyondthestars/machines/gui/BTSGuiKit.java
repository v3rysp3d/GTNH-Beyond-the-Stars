package com.nateyburr.beyondthestars.machines.gui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Controller GUI cleanup (controller-gui-spec.md §4): the shared component kit every Beyond the Stars controller GUI
 * draws with. The palette and badges (§4.1), the rows (§4.3: badge, header, gauge, gauge pair, bar, check, stat, stat
 * pair, section, note, chips, lamps, para), the gap-row parts (stepper, info toggle) and the row pool that draws a
 * server-built {@link BTSRows} string. Every row is 180 wide and 9 high with a 1 px margin (pitch 10); all text is
 * drawn at scale 1.0 with no shadow, and a bar's label sits beside it, never on it. No texture assets: the bars, the
 * gauges and the ✓ ✗ ! ● icons are rectangles ({@link KitDraw}).
 */
public final class BTSGuiKit {

    private BTSGuiKit() {}

    // ================================================================================ palette (§4.1)

    public static final int LABEL = 0xFFAAAAAA, VALUE = 0xFFFFFFFF, OK = 0xFF55FF55, BAD = 0xFFFF5555,
        WARN = 0xFFFFAA00, DIM = 0xFF808080, SECTION = 0xFF8A8F99, RULE = 0xFF2A2D35, TRACK = 0xFF1E2026,
        EDGE = 0xFF4A4D55, TICK = 0xFFFFFF55, TICK_RED = 0xFFFF5555, UNDER_OK = 0xFF2F8F3F, UNDER_BAD = 0xFFA33A3A;
    /** Note colours besides DIM / WARN / BAD. */
    public static final int AQUA = 0xFF55FFFF, NOTE_BLUE = 0xFF7CA8FF;
    /** Text on GT's grey panels (side panels, the gap row). */
    public static final int PANEL_TEXT = 0xFF404040;
    /** Lamp fills: on, fault, off. */
    public static final int LAMP_ON = 0xFF2EBF4F, LAMP_FAULT = 0xFFB0302A, LAMP_OFF = 0xFF2A2D33;

    /** Gauge zone bands, Resonant .. Breach (dim, so the marker reads on them). */
    public static final int[] ZONE_BAND = { 0xFF1F6B3A, 0xFF2E5A6B, 0xFF6B6020, 0xFF7A3A14, 0xFF4A1414 };
    /** Zone text colours, Resonant .. Breach. */
    public static final String[] ZONE_TEXT = { "§a", "§b", "§e", "§6", "§c" };
    /** Zone edges about the centre: Resonant ±10, Stable ±40, Strained ±70, Critical ±100. */
    public static final int[] ZONE_EDGE = { 10, 40, 70, 100 };
    /** The gauges' range, ±120. */
    public static final double GAUGE_RANGE = 120;

    /** Badge fills / edges / text (§4.1). */
    public enum BadgeColour {

        GREEN(0xFF55C85A, 0xFF2F7A33, 0xFF0E1A0F),
        AQUA(0xFF48C9C9, 0xFF257A7A, 0xFF0B1A1A),
        YELLOW(0xFFE8DA50, 0xFF8F8526, 0xFF1A1806),
        GOLD(0xFFF0A030, 0xFF94601A, 0xFF1A1006),
        RED(0xFFD23C3C, 0xFF7E1F1F, 0xFFFFFFFF),
        PURPLE(0xFFA35BD6, 0xFF5E2F80, 0xFFFFFFFF),
        BLUE(0xFF5B8DEF, 0xFF2D4F94, 0xFFFFFFFF),
        GREY(0xFF8A8F99, 0xFF4E525A, 0xFF101010),
        DARK(0xFF3A3D45, 0xFF5A5E68, 0xFFC8C8C8);

        public final int fill, edge, text;

        BadgeColour(int fill, int edge, int text) {
            this.fill = fill;
            this.edge = edge;
            this.text = text;
        }
    }

    /**
     * Every state badge of the family (§4.1, §6). The server syncs the ordinal; the text is the lang key
     * {@code beyondthestars.gui.badge.<id>}. New badges are appended (the ordinal is only a GUI sync value, never
     * saved).
     */
    public enum Badge {

        RESONANT("resonant", BadgeColour.GREEN),
        STABLE("stable", BadgeColour.AQUA),
        STRAINED("strained", BadgeColour.YELLOW),
        CRITICAL("critical", BadgeColour.GOLD),
        DARK("dark", BadgeColour.DARK),
        NO_STAR("nostar", BadgeColour.GREY),
        IGNITING("igniting", BadgeColour.GOLD),
        PENDING("pending", BadgeColour.BLUE),
        TRANSITION("transition", BadgeColour.PURPLE),
        COOLING("cooling", BadgeColour.AQUA),
        IDLE_T5("idle", BadgeColour.YELLOW),
        IDLE("idle", BadgeColour.DARK),
        READY("ready", BadgeColour.BLUE),
        PAUSED("paused", BadgeColour.GOLD),
        PAUSED_RED("paused", BadgeColour.RED),
        NOT_FORMED("notformed", BadgeColour.RED),
        FROZEN("frozen", BadgeColour.RED),
        OPEN("open", BadgeColour.GREEN),
        CLOSED("closed", BadgeColour.DARK),
        GROWING("growing", BadgeColour.AQUA),
        DRIFTING("drifting", BadgeColour.GOLD),
        NO_POWER("nopower", BadgeColour.RED),
        RUNNING("running", BadgeColour.GREEN),
        WAITING("waiting", BadgeColour.YELLOW),
        INSTALLING("installing", BadgeColour.PURPLE),
        NOT_LINKED("notlinked", BadgeColour.RED),
        NO_HOLE("nohole", BadgeColour.DARK),
        DORMANT("dormant", BadgeColour.DARK),
        REFORMING("reforming", BadgeColour.GOLD),
        OPENING("opening", BadgeColour.GOLD),
        NOT_BUILT("notbuilt", BadgeColour.RED),
        HELD("held", BadgeColour.YELLOW),
        CLAMPED("clamped", BadgeColour.GOLD),
        OFFLINE("offline", BadgeColour.DARK);

        public final String id;
        public final BadgeColour colour;

        Badge(String id, BadgeColour colour) {
            this.id = id;
            this.colour = colour;
        }

        public String langKey() {
            return "beyondthestars.gui.badge." + id;
        }

        public static Badge of(int ordinal, Badge fallback) {
            final Badge[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : fallback;
        }

        /** The badge of a field zone (0 Resonant .. 3 Critical; Breach is momentary and shows CRITICAL). */
        public static Badge ofZone(int zone) {
            switch (zone) {
                case 0:
                    return RESONANT;
                case 1:
                    return STABLE;
                case 2:
                    return STRAINED;
                default:
                    return CRITICAL;
            }
        }
    }

    /** Bar gradients (§4.1), from → to. */
    public enum Grad {

        BLUE(0xFF2F6B8F, 0xFF7CD8FF),
        GREEN(0xFF1F6B3A, 0xFF55DD55),
        YELLOW(0xFF6B6020, 0xFFE8DA50),
        GOLD(0xFF7A3A10, 0xFFFFA040),
        RED(0xFF6E1A1A, 0xFFFF5555),
        LAVENDER(0xFF4B2F8F, 0xFF9A7CFF),
        CYAN(0xFF1F4A7A, 0xFF7FE0FF),
        MAGENTA(0xFF5E2F80, 0xFFE07CFF);

        public final int from, to;

        Grad(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public static Grad of(int ordinal) {
            final Grad[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : BLUE;
        }
    }

    /** The zone (0 Resonant .. 4 Breach) of a gauge error. */
    public static int zoneOf(double error) {
        final double e = Math.abs(error);
        for (int z = 0; z < ZONE_EDGE.length; z++) if (e <= ZONE_EDGE[z]) return z;
        return 4;
    }

    /** The bar gradient of a zone: Resonant GREEN, Stable BLUE, Strained YELLOW, Critical (and worse) GOLD. */
    public static Grad zoneGrad(int zone) {
        switch (zone) {
            case 0:
                return Grad.GREEN;
            case 1:
                return Grad.BLUE;
            case 2:
                return Grad.YELLOW;
            default:
                return Grad.GOLD;
        }
    }

    /** The health rule (§4.1): ≥ 75 green, ≥ 40 yellow, else red. Text colour. */
    public static String healthText(double pct) {
        return pct >= 75 ? "§a" : pct >= 40 ? "§e" : "§c";
    }

    /** The health rule as a bar gradient. */
    public static Grad healthGrad(double pct) {
        return pct >= 75 ? Grad.GREEN : pct >= 40 ? Grad.YELLOW : Grad.RED;
    }

    /** One bar tick for {@link BTSRows.Builder#bar}: a 1 px line at {@code frac} of the bar. */
    public static String tick(double frac, int colour) {
        return String.format(Locale.ROOT, "%.4f:%d", Math.max(0, Math.min(1, frac)), colour);
    }

    /** Joins ticks. */
    public static String ticks(String... ticks) {
        return String.join(";", ticks);
    }

    private static double[][] parseTicks(String s) {
        if (s == null || s.isEmpty()) return new double[0][];
        final String[] parts = s.split(";");
        final double[][] out = new double[parts.length][];
        for (int i = 0; i < parts.length; i++) {
            final String[] fc = parts[i].split(":");
            try {
                out[i] = new double[] { Double.parseDouble(fc[0]), Long.parseLong(fc[1]) };
            } catch (RuntimeException e) {
                out[i] = new double[] { -1, 0 };
            }
        }
        return out;
    }

    // ============================================================================== the widget

    /** Paints a kit widget in its own coordinates (0, 0 = top-left), client side. */
    public interface Painter {

        void paint(KitWidget w, int width, int height);
    }

    /** A widget drawn by a {@link Painter} (every kit row). */
    public static class KitWidget extends Widget<KitWidget> {

        private final Painter painter;
        /** The mouse x relative to this widget at its last draw (the context is transformed to it there). */
        private int mouseX = -1;

        public KitWidget(Painter painter) {
            this.painter = painter;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            mouseX = context.getMouseX();
            painter.paint(this, getArea().width, getArea().height);
        }

        /** The mouse x in this widget's coordinates at its last draw (a chip row picks the hovered chip). */
        public int mouseX() {
            return mouseX;
        }
    }

    /** A kit row: 180 x 9, pitch 10. */
    public static KitWidget row(Painter painter) {
        return new KitWidget(painter).size(180, 9)
            .marginBottom(1);
    }

    // ================================================================================== painters

    /** Badge (§4.3): h 11, w = text − 1 + 6; edge, fill inset 1, text at (3, 2). Returns its width. */
    public static int paintBadge(Badge badge, int x, int y) {
        if (badge == null) return 0;
        final String text = BTSText.resolve(BTSText.lang(badge.langKey()));
        final int w = KitDraw.width(text) - 1 + 6;
        KitDraw.box(x, y, w, 11, badge.colour.edge, badge.colour.fill);
        KitDraw.text(text, x + 3, y + 2, badge.colour.text);
        return w;
    }

    /** One gauge bar at (x, y), w x 7: track (Breach), zone bands about {@code centre}, tick, marker. */
    public static void paintGaugeBar(int x, int y, int w, double v, double centre, double tick) {
        final int h = 7;
        final double c = Double.isNaN(centre) ? 0 : centre;
        KitDraw.box(x, y, w, h, EDGE, ZONE_BAND[4]);
        final float inner = w - 2;
        for (int z = 3; z >= 0; z--) {
            final double lo = Math.max(-GAUGE_RANGE, c - ZONE_EDGE[z]);
            final double hi = Math.min(GAUGE_RANGE, c + ZONE_EDGE[z]);
            if (hi <= lo) continue;
            final float x0 = (float) (x + 1 + (lo + GAUGE_RANGE) / (2 * GAUGE_RANGE) * inner);
            final float x1 = (float) (x + 1 + (hi + GAUGE_RANGE) / (2 * GAUGE_RANGE) * inner);
            KitDraw.rect(x0, y + 1, x1 - x0, h - 2, ZONE_BAND[z]);
        }
        if (!Double.isNaN(centre) && Math.abs(centre) > 1e-9) {
            KitDraw.rect(Math.round(x + 1 + (c + GAUGE_RANGE) / (2 * GAUGE_RANGE) * inner), y, 1, h, TICK);
        }
        if (!Double.isNaN(tick)) {
            final double t = Math.max(-GAUGE_RANGE, Math.min(GAUGE_RANGE, tick));
            KitDraw.rect(Math.round(x + 1 + (t + GAUGE_RANGE) / (2 * GAUGE_RANGE) * inner), y - 1, 1, h + 2, TICK);
        }
        final double vv = Math.max(-GAUGE_RANGE, Math.min(GAUGE_RANGE, Double.isNaN(v) ? 0 : v));
        final int mx = (int) Math.round(x + 1 + (vv + GAUGE_RANGE) / (2 * GAUGE_RANGE) * inner) - 1;
        KitDraw.rect(mx - 1, y - 1, 4, h + 2, 0xFF000000);
        KitDraw.rect(mx, y - 1, 2, h + 2, 0xFFFFFFFF);
    }

    /** The zone-coloured signed value of a gauge. */
    public static String gaugeValue(double v, double centre) {
        final double c = Double.isNaN(centre) ? 0 : centre;
        return ZONE_TEXT[zoneOf(v - c)] + BTSFormat.signed(v);
    }

    /** Gauge (§4.3): symbol x 0, bar x 12 w 122 at y 1, value right-aligned to 180 in the zone colour. */
    public static void paintGauge(int width, String sym, double v, double centre, double tick) {
        KitDraw.text(sym, 0, 1, VALUE);
        paintGaugeBar(12, 1, 122, v, centre, tick);
        KitDraw.textRight(gaugeValue(v, centre), width, 1, VALUE);
    }

    /** Gauge pair (§4.3): two halves of 88 with a 4 px gap; symbol x 0, bar x 9 w 48, value at the half's edge. */
    public static void paintGaugePair(String s1, double v1, double c1, String s2, double v2, double c2) {
        paintGaugeHalf(0, s1, v1, c1);
        if (s2 != null && !s2.isEmpty()) paintGaugeHalf(92, s2, v2, c2);
    }

    private static void paintGaugeHalf(int x, String sym, double v, double c) {
        KitDraw.text(sym, x, 1, VALUE);
        paintGaugeBar(x + 9, 1, 48, v, c, Double.NaN);
        KitDraw.textRight(gaugeValue(v, c), x + 88, 1, VALUE);
    }

    /** Bar (§4.3): label in a 50 px column, bar x 52 w 80 h 7 at y 1, value right-aligned to the row's end. */
    public static void paintBar(int width, String label, double frac, String value, Grad grad, String ticks) {
        KitDraw.textCut(label, 0, 1, 50, LABEL);
        final int bx = 52, bw = 80, by = 1, bh = 7;
        KitDraw.box(bx, by, bw, bh, EDGE, TRACK);
        final double f = Double.isNaN(frac) ? 0 : Math.max(0, Math.min(1, frac));
        final float fill = (float) ((bw - 2) * f);
        if (fill > 0) KitDraw.gradient(bx + 1, by + 1, fill, bh - 2, grad.from, grad.to);
        for (double[] t : parseTicks(ticks)) {
            if (t[0] < 0) continue;
            KitDraw.rect(Math.round(bx + 1 + (bw - 2) * t[0]), by - 1, 1, bh + 2, (int) (long) t[1]);
        }
        KitDraw.textRight(value, width, 1, VALUE);
    }

    /** Check (§4.3): icon at (0, 1), label x 10, value right-aligned, the optional fill line at y 9. */
    public static void paintCheck(int width, BTSChecklist.State state, String label, String value, double frac) {
        final int icon = state == BTSChecklist.State.OK ? BTSRows.ICON_OK
            : state == BTSChecklist.State.BAD ? BTSRows.ICON_BAD : BTSRows.ICON_WARN;
        final int colour = state == BTSChecklist.State.OK ? OK : state == BTSChecklist.State.BAD ? BAD : WARN;
        KitDraw.icon(icon, 0, 1);
        final int vw = KitDraw.width(value);
        KitDraw.textRight(value, width, 1, colour);
        KitDraw.textCut(label, 10, 1, width - 10 - vw - 4, state == BTSChecklist.State.OK ? LABEL : VALUE);
        if (!Double.isNaN(frac) && frac >= 0) {
            KitDraw.rect(10, 9, width - 10, 1, RULE);
            KitDraw.rect(
                10,
                9,
                (float) ((width - 10) * Math.min(1, frac)),
                1,
                state == BTSChecklist.State.OK ? UNDER_OK : UNDER_BAD);
        }
    }

    /** Stat (§4.3): label x 0 (§7), value right-aligned. */
    public static void paintStat(int width, String label, String value) {
        final int vw = KitDraw.width(value);
        KitDraw.textRight(value, width, 1, VALUE);
        KitDraw.textCut(label, 0, 1, width - vw - 4, LABEL);
    }

    /** Stat pair (§4.3): halves of 87 with a 1 px divider at x 90. */
    public static void paintStat2(int width, String l1, String v1, String l2, String v2) {
        final int half = 87;
        final int w1 = KitDraw.width(v1);
        KitDraw.textRight(v1, half, 1, VALUE);
        KitDraw.textCut(l1, 0, 1, half - w1 - 3, LABEL);
        KitDraw.rect(90, 0, 1, 9, RULE);
        final int w2 = KitDraw.width(v2);
        KitDraw.textRight(v2, width, 1, VALUE);
        KitDraw.textCut(l2, 93, 1, width - 93 - w2 - 3, LABEL);
    }

    /** Section (§4.3): the label, then a rule at y 4, or a right-aligned text (a count) instead. */
    public static void paintSection(int width, String text, String right) {
        final int rw = right == null || right.isEmpty() ? 0 : KitDraw.width(right);
        final int tw = KitDraw.textCut(text, 0, 1, width - rw - 6, SECTION);
        if (rw > 0) KitDraw.textRight(right, width, 1, WARN);
        else KitDraw.rect(tw + 4, 4, width - tw - 4, 1, RULE);
    }

    /** Chips (§4.3): a 5x5 dot at y 2, 2 px, the white name, 5 px to the next. Returns the chips' x ranges. */
    public static List<int[]> paintChips(List<String[]> items) {
        final List<int[]> ranges = new ArrayList<>();
        int x = 0;
        for (String[] it : items) {
            final int colour = (int) parseLong(it.length > 0 ? it[0] : "0", DIM);
            final String name = BTSText.resolve(it.length > 1 ? it[1] : "");
            KitDraw.dot(x, 2, colour);
            final int nw = KitDraw.width(name);
            KitDraw.text(name, x + 7, 1, VALUE);
            ranges.add(new int[] { x, x + 7 + nw });
            x += 7 + nw + 5;
        }
        return ranges;
    }

    /** Lamps (§4.3): the label, then per lamp a 7x7 (edge + 5x5 fill), the name and 4 px. */
    public static void paintLamps(String label, List<String[]> items) {
        int x = 0;
        if (label != null && !label.isEmpty()) {
            KitDraw.text(label, 0, 1, LABEL);
            x = KitDraw.width(label) + 4;
        }
        for (String[] it : items) {
            final int state = (int) parseLong(it.length > 0 ? it[0] : "0", 0);
            final String name = BTSText.resolve(it.length > 1 ? it[1] : "");
            KitDraw.box(x, 1, 7, 7, EDGE, state == 1 ? LAMP_ON : state == 2 ? LAMP_FAULT : LAMP_OFF);
            KitDraw.text(name, x + 9, 1, state == 0 ? DIM : VALUE);
            x += 9 + KitDraw.width(name) + 4;
        }
    }

    private static long parseLong(String s, long fallback) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Header (§3): badge, subject (white) at badge + 4, the right chip (§7) right-aligned, the rule at y 13. */
    public static void paintHeader(int width, Badge badge, String subject, String right, boolean alert) {
        final int bw = paintBadge(badge, 0, 0);
        final int rightEdge = alert ? width - 16 : width - 4;
        final int rw = KitDraw.width(right);
        if (rw > 0) KitDraw.textRight(right, rightEdge, 2, LABEL);
        KitDraw.textCut(subject, bw + 4, 2, rightEdge - rw - 4 - (bw + 4), VALUE);
        KitDraw.rect(0, 13, width, 1, RULE);
    }

    // ================================================================================ factories

    private static Consumer<RichTooltip> lines(Supplier<List<String>> tip) {
        return t -> {
            if (tip == null) return;
            for (String l : tip.get()) t.addLine(BTSText.resolve(l));
        };
    }

    private static KitWidget withTip(KitWidget w, Supplier<List<String>> tip) {
        if (tip == null) return w;
        return w.tooltipDynamic(lines(tip))
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** A badge as a drawable widget (11 high, as wide as its text). */
    public static KitWidget badge(Supplier<Badge> badge) {
        return new KitWidget((w, width, height) -> paintBadge(badge.get(), 0, 0)).size(80, 11);
    }

    public static KitWidget gauge(String sym, DoubleSupplier v, DoubleSupplier centre, Double tick,
        Supplier<List<String>> tooltip) {
        return withTip(
            row(
                (w, width, h) -> paintGauge(
                    width,
                    sym,
                    v.getAsDouble(),
                    centre == null ? Double.NaN : centre.getAsDouble(),
                    tick == null ? Double.NaN : tick)),
            tooltip);
    }

    public static KitWidget gaugePair(String s1, DoubleSupplier v1, String s2, DoubleSupplier v2,
        Supplier<List<String>> tooltip) {
        return withTip(
            row(
                (w, width, h) -> paintGaugePair(
                    s1,
                    v1.getAsDouble(),
                    Double.NaN,
                    s2,
                    v2 == null ? 0 : v2.getAsDouble(),
                    Double.NaN)),
            tooltip);
    }

    public static KitWidget bar(String labelKey, DoubleSupplier frac, Supplier<String> value, Supplier<Grad> grad,
        Supplier<String> ticks, Supplier<List<String>> tooltip) {
        return withTip(
            row(
                (w, width, h) -> paintBar(
                    width,
                    BTSText.resolve(BTSText.lang(labelKey)),
                    frac.getAsDouble(),
                    value.get(),
                    grad.get(),
                    ticks == null ? "" : ticks.get())),
            tooltip);
    }

    public static KitWidget check(Supplier<BTSChecklist.CheckLine> line) {
        final KitWidget w = row((self, width, h) -> {
            final BTSChecklist.CheckLine l = line.get();
            if (l != null) paintCheck(width, l.state, BTSText.resolve(l.label), BTSText.resolve(l.value), l.frac);
        });
        return withTip(w, () -> {
            final BTSChecklist.CheckLine l = line.get();
            return l == null ? Collections.<String>emptyList() : l.tooltip;
        });
    }

    public static KitWidget stat(String labelKey, Supplier<String> value, Supplier<List<String>> tooltip) {
        return withTip(
            row((w, width, h) -> paintStat(width, BTSText.resolve(BTSText.lang(labelKey)), value.get())),
            tooltip);
    }

    public static KitWidget stat2(String l1, Supplier<String> v1, String l2, Supplier<String> v2,
        Supplier<List<String>> tooltip) {
        return withTip(
            row(
                (w, width, h) -> paintStat2(
                    width,
                    BTSText.resolve(BTSText.lang(l1)),
                    v1.get(),
                    BTSText.resolve(BTSText.lang(l2)),
                    v2.get())),
            tooltip);
    }

    public static KitWidget section(Supplier<String> text, Supplier<String> right) {
        return row((w, width, h) -> paintSection(width, text.get(), right == null ? "" : right.get()));
    }

    /** Note (§4.3): an optional 7x7 icon, then one line that scrolls on hover. */
    public static Flow note(Supplier<String> text, IntSupplier colour, IntSupplier icon,
        Supplier<List<String>> tooltip) {
        final ScrollingTextWidget line = new ScrollingTextWidget(IKey.dynamic(text::get)).width(170)
            .height(9)
            .color(colour)
            .shadow(false);
        if (tooltip != null) line.tooltipDynamic(lines(tooltip))
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return Flow.row()
            .size(180, 9)
            .marginBottom(1)
            .collapseDisabledChild()
            .child(
                new KitWidget((w, width, h) -> KitDraw.icon(icon.getAsInt(), 0, 1)).size(10, 9)
                    .setEnabledIf(w -> icon.getAsInt() != BTSRows.ICON_NONE))
            .child(line);
    }

    public static KitWidget chips(Supplier<List<String[]>> items) {
        final KitWidget w = row((self, width, h) -> paintChips(items.get()));
        return w.tooltipDynamic(t -> chipTooltip(t, items.get(), w.mouseX()))
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /**
     * The index of the chip under {@code x} (row coordinates, as {@link #paintChips} lays them out; the 5 px gap
     * after a chip counts as that chip's), or −1 when {@code x} is past the last chip or unknown.
     */
    public static int chipAt(List<String[]> items, int x) {
        if (x < 0) return -1;
        int left = 0;
        for (int i = 0; i < items.size(); i++) {
            final String[] it = items.get(i);
            final int right = left + 7 + KitDraw.width(BTSText.resolve(it.length > 1 ? it[1] : "")) + 5;
            if (x < right) return i;
            left = right;
        }
        return -1;
    }

    /** A chip row's tooltip: the hovered chip's lines (§4.3 "hover detail"), or every chip's when none is hit. */
    private static void chipTooltip(RichTooltip t, List<String[]> items, int mouseX) {
        final int hit = chipAt(items, mouseX);
        for (int i = 0; i < items.size(); i++) {
            if (hit >= 0 && i != hit) continue;
            final String[] it = items.get(i);
            if (it.length > 2 && !it[2].isEmpty()) {
                for (String l : it[2].split(String.valueOf(BTSRows.LINE))) t.addLine(BTSText.resolve(l));
            }
        }
    }

    public static KitWidget lamps(Supplier<String> label, Supplier<List<String[]>> items) {
        return row((w, width, h) -> paintLamps(label.get(), items.get()));
    }

    /** Para (§4.3): wrapped text, 176 wide. */
    public static TextWidget<?> para(Supplier<String> text, IntSupplier colour) {
        return new TextWidget<>(IKey.dynamic(text::get)).width(176)
            .textAlign(Alignment.TopLeft)
            .color(colour)
            .shadow(false)
            .marginBottom(1);
    }

    // ================================================================================ row pool

    /** Decodes the synced row string once per change (client). */
    public static final class RowCache {

        private final Supplier<String> source;
        private String last;
        private List<BTSRows.Row> rows = Collections.emptyList();

        public RowCache(Supplier<String> source) {
            this.source = source;
        }

        public List<BTSRows.Row> rows() {
            final String s = source.get();
            if (s != last && (s == null || !s.equals(last))) {
                last = s;
                rows = BTSRows.decode(s);
            }
            return rows;
        }

        public BTSRows.Row row(int i) {
            final List<BTSRows.Row> r = rows();
            return i < r.size() ? r.get(i) : null;
        }

        boolean is(int i, BTSRows.Kind... kinds) {
            final BTSRows.Row r = row(i);
            if (r == null) return false;
            for (BTSRows.Kind k : kinds) if (r.kind == k) return true;
            return false;
        }
    }

    private static final BTSRows.Kind[] PAINTED = { BTSRows.Kind.GAUGE, BTSRows.Kind.GAUGE2, BTSRows.Kind.BAR,
        BTSRows.Kind.CHECK, BTSRows.Kind.STAT, BTSRows.Kind.STAT2, BTSRows.Kind.SECTION, BTSRows.Kind.CHIPS,
        BTSRows.Kind.LAMPS };

    /**
     * Adds {@code n} pooled rows to {@code list}: row i draws the i-th row of the decoded string with the component of
     * its kind, or hides (the list collapses it). The list must use {@code collapseDisabledChild()}.
     */
    public static void rowPool(ListWidget<IWidget, ?> list, RowCache cache, int n) {
        for (int k = 0; k < n; k++) {
            final int i = k;
            final KitWidget painted = row((w, width, h) -> paintRow(cache.row(i), width));
            painted.setEnabledIf(w -> cache.is(i, PAINTED))
                .tooltipDynamic(t -> {
                    final BTSRows.Row r = cache.row(i);
                    if (r == null) return;
                    if (r.kind == BTSRows.Kind.CHIPS) {
                        chipTooltip(t, r.items(0), painted.mouseX());
                        return;
                    }
                    for (String l : r.lines(tipField(r.kind))) t.addLine(BTSText.resolve(l));
                })
                .tooltipAutoUpdate(true)
                .tooltipShowUpTimer(TOOLTIP_DELAY);
            list.child(painted);
            final Flow note = note(() -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? "" : BTSText.resolve(r.s(2));
            }, () -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? DIM : (int) (long) r.d(1);
            }, () -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? 0 : r.i(0);
            }, () -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? Collections.<String>emptyList() : r.lines(3);
            });
            list.child(note.setEnabledIf(w -> cache.is(i, BTSRows.Kind.NOTE)));
            list.child(para(() -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? "" : BTSText.resolve(r.s(0));
            }, () -> {
                final BTSRows.Row r = cache.row(i);
                return r == null ? VALUE : (int) (long) r.d(1);
            }).setEnabledIf(w -> cache.is(i, BTSRows.Kind.PARA)));
            list.child(
                new KitWidget((w, width, h) -> {}).size(180, 1)
                    .marginBottom(1)
                    .setEnabledIf(w -> cache.is(i, BTSRows.Kind.GAP)));
        }
    }

    private static int tipField(BTSRows.Kind kind) {
        switch (kind) {
            case GAUGE:
                return 4;
            case GAUGE2:
                return 6;
            case BAR:
                return 5;
            case CHECK:
                return 4;
            case STAT:
                return 2;
            case STAT2:
                return 4;
            default:
                return 99;
        }
    }

    /** Draws one decoded row with its kind's painter. */
    public static void paintRow(BTSRows.Row r, int width) {
        if (r == null) return;
        switch (r.kind) {
            case GAUGE:
                paintGauge(width, r.s(0), r.d(1), r.d(2), r.d(3));
                break;
            case GAUGE2:
                paintGaugePair(r.s(0), r.d(1), r.d(2), r.s(3), r.d(4), r.d(5));
                break;
            case BAR:
                paintBar(width, BTSText.resolve(r.s(0)), r.d(1), BTSText.resolve(r.s(2)), Grad.of(r.i(3)), r.s(4));
                break;
            case CHECK:
                paintCheck(
                    width,
                    BTSChecklist.State.values()[Math.max(0, Math.min(2, r.i(0)))],
                    BTSText.resolve(r.s(1)),
                    BTSText.resolve(r.s(2)),
                    r.d(3));
                break;
            case STAT:
                paintStat(width, BTSText.resolve(r.s(0)), BTSText.resolve(r.s(1)));
                break;
            case STAT2:
                paintStat2(
                    width,
                    BTSText.resolve(r.s(0)),
                    BTSText.resolve(r.s(1)),
                    BTSText.resolve(r.s(2)),
                    BTSText.resolve(r.s(3)));
                break;
            case SECTION:
                paintSection(width, BTSText.resolve(r.s(0)), BTSText.resolve(r.s(1)));
                break;
            case CHIPS:
                paintChips(r.items(0));
                break;
            case LAMPS:
                paintLamps(BTSText.resolve(r.s(0)), r.items(1));
                break;
            default:
                break;
        }
    }

    // ============================================================================== gap-row parts

    /**
     * Stepper (§4.3): the label (panel text), − (9x14), a dark value box (white, centred), + (9x14). Left click one
     * step, right click {@code rightMul} steps; clamped here and again on the server.
     */
    public static Flow stepper(String labelKey, DoubleSyncValue v, double step, double min, double max, int rightMul,
        String fmt, int boxWidth, Consumer<RichTooltip> valueTooltip, String minusTip, String plusTip,
        String extraTip) {
        return Flow.row()
            .coverChildrenWidth()
            .heightRel(1f)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .childPadding(1)
            .child(
                IKey.lang(labelKey)
                    .color(PANEL_TEXT)
                    .asWidget()
                    .marginRight(1))
            .child(stepButton(v, -step, min, max, rightMul, minusTip, extraTip))
            .child(
                IKey.dynamic(() -> String.format(Locale.ROOT, fmt, v.getDoubleValue()))
                    .color(VALUE)
                    .asWidget()
                    .size(boxWidth, 12)
                    .textAlign(Alignment.Center)
                    .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                    .tooltipDynamic(valueTooltip)
                    .tooltipAutoUpdate(true)
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(stepButton(v, step, min, max, rightMul, plusTip, extraTip));
    }

    private static IWidget stepButton(DoubleSyncValue value, double step, double min, double max, int rightMul,
        String tooltip, String extra) {
        return new ButtonWidget<>().size(9, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final double raw = value.getDoubleValue() + (button == 1 ? rightMul * step : step);
                final double next = Math.round(Math.max(min, Math.min(max, raw)) * 1000) / 1000.0;
                if (Math.abs(next - value.getDoubleValue()) > 1e-9) value.setDoubleValue(next);
                return true;
            })
            .tooltip(t -> {
                t.addLine(tooltip);
                if (extra != null && !extra.isEmpty()) t.addLine(EnumChatFormatting.GRAY + extra);
            })
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Info toggle (§4.3): 18x18, the information symbol; pressed while the Info page shows. */
    public static ToggleButton infoToggle(BoolValue info) {
        return new ToggleButton().size(18, 18)
            .marginLeft(2)
            .value(info)
            .overlay(
                GTGuiTextures.INFORMATION_SYMBOL.asIcon()
                    .size(7, 18))
            .tooltip(t -> t.addLine(IKey.lang("beyondthestars.gui.kit.info")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** A plain 18x18 gap-row button that opens / closes a side panel. */
    public static ButtonWidget<?> panelButton(com.cleanroommc.modularui.api.drawable.IDrawable overlay,
        com.cleanroommc.modularui.api.IPanelHandler panel, String tooltipKey) {
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(2)
            .overlay(overlay)
            .onMousePressed(button -> {
                if (panel.isPanelOpen()) panel.closePanel();
                else panel.openPanel();
                return true;
            })
            .tooltip(t -> t.addLine(IKey.lang(tooltipKey)))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** The header strip (§3): badge, subject, right chip with its tooltip, alert slot, rule. 180 x 14. */
    public static ParentWidget<?> header(Supplier<Badge> badge, Supplier<String> subject, Supplier<String> right,
        Supplier<List<String>> rightTip, BooleanSupplier alert, IWidget alertIcon) {
        final ParentWidget<?> p = new ParentWidget<>().size(180, 14);
        p.child(
            new KitWidget(
                (w, width, h) -> paintHeader(width, badge.get(), subject.get(), right.get(), alert.getAsBoolean()))
                    .size(180, 14)
                    .pos(0, 0));
        p.child(withTip(new KitWidget((w, width, h) -> {}).size(56, 11), rightTip).pos(120, 0));
        if (alertIcon != null) p.child(alertIcon);
        return p;
    }
}
