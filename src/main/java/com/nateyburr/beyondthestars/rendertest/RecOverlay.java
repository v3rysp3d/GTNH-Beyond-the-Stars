package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY video recorder ({@link RecClient}): what is drawn over the finished frame, in window pixels: the caption
 * bar, the title / end card and the time-lapse tag. Drawn last in the frame, so it is both on screen and in the video.
 */
@SideOnly(Side.CLIENT)
final class RecOverlay {

    private RecOverlay() {}

    /** Card background. */
    private static final int CARD_BG = 0xFF0C0F16;

    /** Sets up a pixel-space 2D projection over the whole window; {@link #end} restores the GL state. */
    static void begin(Minecraft mc) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1000, 1000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.004f);
        GL11.glColor4f(1, 1, 1, 1);
    }

    static void end() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static int alpha(int argb, float a) {
        final int base = argb >>> 24;
        return Math.max(0, Math.min(255, Math.round(base * a))) << 24 | (argb & 0xFFFFFF);
    }

    /** Text at a scale, its left edge at x and top at y (window pixels); alpha 0..1. */
    private static void text(FontRenderer f, String s, double x, double y, float scale, int rgb, float a) {
        if (a <= 0.02f) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        f.drawStringWithShadow(s, 0, 0, alpha(0xFF000000 | rgb, a));
        GL11.glPopMatrix();
    }

    private static List<String> wrap(FontRenderer f, String s, int widthPx, float scale) {
        final List<String> out = new ArrayList<>();
        for (Object o : f.listFormattedStringToWidth(s, (int) (widthPx / scale))) out.add((String) o);
        return out;
    }

    private static void rect(int x0, int y0, int x1, int y1, int argb) {
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        Gui.drawRect(x0, y0, x1, y1, argb);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    /**
     * The caption: a semi-transparent bar across the bottom, white text centred in it (wrapped to two or three lines
     * if long). {@code a} fades it in.
     */
    static void caption(Minecraft mc, String caption, float a) {
        if (caption == null || caption.isEmpty() || a <= 0) return;
        final FontRenderer f = mc.fontRenderer;
        final int w = mc.displayWidth, h = mc.displayHeight;
        final float scale = Math.max(1.5f, h / 360f);
        final List<String> lines = wrap(f, caption, (int) (w * 0.86), scale);
        final double lineH = 11 * scale;
        final int pad = Math.round(9 * scale);
        final int barH = (int) Math.round(lines.size() * lineH + pad * 2 - 2 * scale);
        rect(0, h - barH, w, h, alpha(0xA8000000, a));
        double y = h - barH + pad;
        for (String line : lines) {
            final double x = (w - f.getStringWidth(line) * scale) / 2;
            text(f, line, x, y, scale, 0xFFFFFF, a);
            y += lineH;
        }
    }

    /**
     * A side panel of a machine's scanner lines (what a GT Portable Scanner prints), on the left: a title, then the
     * lines wrapped to the panel, as many as fit above the caption bar. Colour codes in the lines are kept.
     */
    static void panel(Minecraft mc, String title, List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        final FontRenderer f = mc.fontRenderer;
        final int h = mc.displayHeight;
        final float scale = Math.max(1.25f, h / 480f);
        final int pad = Math.round(6 * scale);
        final int width = Math.round(mc.displayWidth * 0.27f);
        final List<String> rows = new ArrayList<>();
        for (String l : lines) rows.addAll(wrap(f, l, width - pad * 2, scale));
        final double lineH = 10 * scale;
        final int maxRows = (int) ((h * 0.72 - pad * 3 - lineH * 1.3) / lineH);
        final List<String> shown = rows.size() > maxRows ? rows.subList(0, maxRows) : rows;
        final int x0 = pad * 2, y0 = pad * 2;
        final int y1 = (int) Math.round(y0 + pad * 2 + lineH * 1.3 + shown.size() * lineH);
        rect(x0, y0, x0 + width, y1, 0xC0000000);
        text(f, title, x0 + pad, y0 + pad, scale, 0xFFD54A, 1f);
        double y = y0 + pad + lineH * 1.3;
        for (String r : shown) {
            text(f, r, x0 + pad, y, scale, 0xFFFFFF, 1f);
            y += lineH;
        }
    }

    /** A title / end card: plain dark background, the first line large, the rest smaller, centred. */
    static void card(Minecraft mc, List<String> lines, float textAlpha) {
        final FontRenderer f = mc.fontRenderer;
        final int w = mc.displayWidth, h = mc.displayHeight;
        rect(0, 0, w, h, CARD_BG);
        if (lines.isEmpty()) return;
        final float big = Math.max(3f, h / 180f), small = Math.max(1.5f, h / 360f);
        final List<String[]> rows = new ArrayList<>(); // text, scale index
        double total = 0;
        for (int i = 0; i < lines.size(); i++) {
            final float s = i == 0 ? big : small;
            for (String l : wrap(f, lines.get(i), (int) (w * 0.8), s)) {
                rows.add(new String[] { l, i == 0 ? "0" : "1" });
                total += 11 * s;
            }
            if (i == 0) total += 8 * small;
        }
        double y = (h - total) / 2;
        boolean firstDone = false;
        for (String[] r : rows) {
            final boolean title = r[1].equals("0");
            if (!title && !firstDone) {
                y += 8 * small;
                firstDone = true;
            }
            final float s = title ? big : small;
            final double x = (w - f.getStringWidth(r[0]) * s) / 2;
            text(f, r[0], x, y, s, title ? 0xFFFFFF : 0xB8C4D6, textAlpha);
            y += 11 * s;
        }
    }

    /**
     * A callout: a dot on the target (window pixels {@code tx, ty}), a line out to a box with the text, the box set off
     * towards {@code side} (up, down, left, right; up-right by default). Off-screen targets are skipped.
     */
    static void label(Minecraft mc, double tx, double ty, String text, String side, float a) {
        label(mc, tx, ty, text, side, a, null);
    }

    /** As above; {@code taken} holds the boxes already drawn this frame, and this box moves clear of them. */
    static void label(Minecraft mc, double tx, double ty, String text, String side, float a, List<int[]> taken) {
        if (a <= 0.02f || text == null || text.isEmpty()) return;
        final FontRenderer f = mc.fontRenderer;
        final int w = mc.displayWidth, h = mc.displayHeight;
        if (tx < -50 || ty < -50 || tx > w + 50 || ty > h + 50) return;
        final float scale = Math.max(1.5f, Math.round(h / 400f));
        final List<String> lines = wrap(f, text, (int) (w * 0.28), scale);
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, f.getStringWidth(l));
        final int pad = Math.round(4 * scale);
        final int bw = Math.round(tw * scale) + pad * 2, bh = Math.round(lines.size() * 10 * scale) + pad * 2 - 1;
        final int off = Math.round(28 * scale);
        double bx, by; // the box's top-left
        switch (side == null ? "" : side) {
            case "left":
                bx = tx - off - bw;
                by = ty - bh / 2.0;
                break;
            case "right":
                bx = tx + off;
                by = ty - bh / 2.0;
                break;
            case "down":
                bx = tx - bw / 2.0;
                by = ty + off;
                break;
            case "up":
                bx = tx - bw / 2.0;
                by = ty - off - bh;
                break;
            default:
                bx = tx + off * 0.7;
                by = ty - off * 0.7 - bh;
                break;
        }
        // keep the box on screen and clear of the caption bar
        bx = Math.max(8, Math.min(w - bw - 8, bx));
        by = Math.max(8, Math.min(h - bh - Math.round(48 * h / 1080f) - 8, by));
        // clear of the boxes already drawn: step away (up, or down near the top) until nothing overlaps
        if (taken != null) {
            final double step = bh + 6;
            final boolean up = by > h / 3.0;
            for (int i = 0; i < 12 && overlaps(taken, bx, by, bw, bh); i++) by += up ? -step : step;
            by = Math.max(8, Math.min(h - bh - Math.round(48 * h / 1080f) - 8, by));
            taken.add(new int[] { (int) bx, (int) by, bw, bh });
        }
        // the box edge point nearest the target, for the leader line
        final double ex = Math.max(bx, Math.min(bx + bw, tx)), ey = Math.max(by, Math.min(by + bh, ty));
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(Math.max(2f, scale * 1.2f));
        GL11.glColor4f(1f, 0.84f, 0.29f, a);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(tx, ty);
        GL11.glVertex2d(ex, ey);
        GL11.glEnd();
        final double r = 3.5 * scale;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(tx - r, ty - r);
        GL11.glVertex2d(tx - r, ty + r);
        GL11.glVertex2d(tx + r, ty + r);
        GL11.glVertex2d(tx + r, ty - r);
        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        final int x0 = (int) Math.round(bx), y0 = (int) Math.round(by);
        rect(x0, y0, x0 + bw, y0 + bh, alpha(0xC8000000, a));
        rect(x0, y0, x0 + Math.max(2, Math.round(scale)), y0 + bh, alpha(0xFFFFD54A, a));
        double y = y0 + pad;
        for (String l : lines) {
            text(f, l, x0 + pad + scale, y, scale, 0xFFFFFF, a);
            y += 10 * scale;
        }
    }

    private static boolean overlaps(List<int[]> taken, double x, double y, int w, int h) {
        for (int[] r : taken) {
            if (x < r[0] + r[2] + 4 && x + w + 4 > r[0] && y < r[1] + r[3] + 4 && y + h + 4 > r[1]) return true;
        }
        return false;
    }

    /**
     * Renders showcase: a badge in the top-left corner, a coloured bar beside a large word and a smaller line under it
     * ({@code BEFORE} / the old render, {@code AFTER} / renders v2). {@code accent} is 0xRRGGBB.
     */
    static void badge(Minecraft mc, String word, String line, int accent, float a) {
        if (a <= 0.02f || word == null || word.isEmpty()) return;
        final FontRenderer f = mc.fontRenderer;
        final float big = Math.max(2f, mc.displayHeight / 270f), small = Math.max(1.25f, mc.displayHeight / 540f);
        final int pad = Math.round(7 * small);
        final int bar = Math.max(3, Math.round(2 * small));
        final int ww = Math.round(f.getStringWidth(word) * big);
        final int lw = line == null || line.isEmpty() ? 0 : Math.round(f.getStringWidth(line) * small);
        final int w = Math.max(ww, lw) + pad * 2 + bar;
        final int h = Math.round(9 * big) + (lw > 0 ? Math.round(11 * small) : 0) + pad * 2;
        final int x0 = pad * 3, y0 = pad * 3;
        rect(x0, y0, x0 + w, y0 + h, alpha(0xB4000000, a));
        rect(x0, y0, x0 + bar, y0 + h, alpha(0xFF000000 | accent, a));
        text(f, word, x0 + bar + pad, y0 + pad, big, accent, a);
        if (lw > 0) text(f, line, x0 + bar + pad, y0 + pad + 9 * big + 2 * small, small, 0xE6E9EF, a);
    }

    /** A tag in the top-right corner (e.g. "TIME-LAPSE x20"). */
    static void tag(Minecraft mc, String tag) {
        tag(mc, tag, 0);
    }

    /** A tag in the top-right corner; {@code row} 1 sits under row 0 (the time warp over the time-lapse). */
    static void tag(Minecraft mc, String tag, int row) {
        final FontRenderer f = mc.fontRenderer;
        final float scale = Math.max(1.5f, mc.displayHeight / 360f);
        final int pad = Math.round(6 * scale);
        final int tw = Math.round(f.getStringWidth(tag) * scale);
        final int x1 = mc.displayWidth - pad * 2, y0 = pad * 2 + row * (Math.round(9 * scale) + pad * 3);
        rect(x1 - tw - pad * 2, y0, x1, y0 + Math.round(9 * scale) + pad * 2, 0xB0000000);
        text(f, tag, x1 - tw - pad, y0 + pad + scale, scale, 0xFFD54A, 1f);
    }
}
