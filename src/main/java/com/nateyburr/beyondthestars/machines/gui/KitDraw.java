package com.nateyburr.beyondthestars.machines.gui;

import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.utils.Platform;

/**
 * Client-only drawing helpers for the kit (controller-gui-spec.md §4.3): text at scale 1.0 with no shadow, measured and
 * cut to a column, rectangles, and the 7x7 pixel icons (✓ ✗ ! ●) the font cannot draw cleanly. Only ever called from
 * a widget's draw method, so a dedicated server never loads it.
 */
final class KitDraw {

    private KitDraw() {}

    /** 7x7 icons, '#' = lit (spec §4.3). */
    private static final String[] ICON_OK = { "......#", ".....##", "#...##.", "##.##..", ".###...", "..#....",
        "......." };
    private static final String[] ICON_BAD = { "#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##",
        "#.....#" };
    private static final String[] ICON_WARN = { "..###..", "..###..", "..###..", "..###..", ".......", "..###..",
        "..###.." };
    private static final String[] DOT = { ".###.", "#####", "#####", "#####", ".###." };
    /** 9x9 alert icons: a wrench (maintenance) and a stop square (shutdown). */
    private static final String[] WRENCH = { ".........", ".....##..", "....#..#.", "....#.##.", "...###...",
        "..###....", ".###.....", ".##......", "........." };

    static FontRenderer font() {
        return TextRenderer.getFontRenderer();
    }

    static int width(String s) {
        return s == null || s.isEmpty() ? 0 : font().getStringWidth(s);
    }

    /** Draws {@code s} at (x, y) in {@code colour} (ARGB); § codes inside override it. */
    static void text(String s, int x, int y, int colour) {
        if (s == null || s.isEmpty()) return;
        Platform.setupDrawFont();
        font().drawString(s, x, y, colour, false);
    }

    /** Draws {@code s} right-aligned so it ends at {@code right}; returns its left x. */
    static int textRight(String s, int right, int y, int colour) {
        final int x = right - width(s);
        text(s, x, y, colour);
        return x;
    }

    /**
     * Draws {@code s} right-aligned to {@code right}, cut to {@code maxW} pixels first (a "." marks the cut; the §
     * codes
     * at its start are kept). Returns its left x.
     */
    static int textRightCut(String s, int right, int y, int maxW, int colour) {
        if (s == null || s.isEmpty()) return right;
        String out = s;
        if (maxW > 0 && width(s) > maxW) out = font().trimStringToWidth(s, Math.max(0, maxW - width("."))) + ".";
        return textRight(out, right, y, colour);
    }

    /** Draws {@code s} cut to {@code maxW} pixels (a "." marks the cut); returns the drawn width. */
    static int textCut(String s, int x, int y, int maxW, int colour) {
        if (s == null || s.isEmpty() || maxW <= 0) return 0;
        String out = s;
        if (width(s) > maxW) {
            out = font().trimStringToWidth(s, Math.max(0, maxW - width("."))) + ".";
        }
        text(out, x, y, colour);
        return width(out);
    }

    static void rect(float x, float y, float w, float h, int colour) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.drawRect(x, y, w, h, colour);
    }

    static void gradient(float x, float y, float w, float h, int from, int to) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.drawHorizontalGradientRect(x, y, w, h, from, to);
    }

    /** A bordered box: 1 px edge, the inside filled. */
    static void box(float x, float y, float w, float h, int edge, int fill) {
        rect(x, y, w, h, edge);
        rect(x + 1, y + 1, w - 2, h - 2, fill);
    }

    private static void pixels(String[] map, int x, int y, int colour) {
        for (int r = 0; r < map.length; r++) {
            final String row = map[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) != '#') {
                    c++;
                    continue;
                }
                int e = c;
                while (e < row.length() && row.charAt(e) == '#') e++;
                rect(x + c, y + r, e - c, 1, colour);
                c = e;
            }
        }
    }

    /** ✓ / ✗ / ! at (x, y), 7x7; {@link BTSRows#ICON_NONE} draws nothing. */
    static void icon(int icon, int x, int y) {
        switch (icon) {
            case BTSRows.ICON_OK:
                pixels(ICON_OK, x, y, BTSGuiKit.OK);
                break;
            case BTSRows.ICON_BAD:
                pixels(ICON_BAD, x, y, BTSGuiKit.BAD);
                break;
            case BTSRows.ICON_WARN:
                pixels(ICON_WARN, x, y, BTSGuiKit.WARN);
                break;
            default:
                break;
        }
    }

    /** The 5x5 chip dot. */
    static void dot(int x, int y, int colour) {
        pixels(DOT, x, y, colour);
    }

    /** The 9x9 maintenance alert: a yellow wrench on dark amber. */
    static void wrench(int x, int y) {
        rect(x, y, 9, 9, 0xFF5A4A10);
        pixels(WRENCH, x, y, 0xFFE8DA50);
    }

    /** The 9x9 shutdown alert: a red stop square. */
    static void stop(int x, int y) {
        box(x, y, 9, 9, 0xFF7E1F1F, 0xFFD23C3C);
        rect(x + 2, y + 4, 5, 1, 0xFFFFFFFF);
    }
}
