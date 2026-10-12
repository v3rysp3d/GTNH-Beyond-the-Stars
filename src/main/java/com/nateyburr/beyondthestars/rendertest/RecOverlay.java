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

    /** A tag in the top-right corner (e.g. "TIME-LAPSE x20"). */
    static void tag(Minecraft mc, String tag) {
        final FontRenderer f = mc.fontRenderer;
        final float scale = Math.max(1.5f, mc.displayHeight / 360f);
        final int pad = Math.round(6 * scale);
        final int tw = Math.round(f.getStringWidth(tag) * scale);
        final int x1 = mc.displayWidth - pad * 2, y0 = pad * 2;
        rect(x1 - tw - pad * 2, y0, x1, y0 + Math.round(9 * scale) + pad * 2, 0xB0000000);
        text(f, tag, x1 - tw - pad, y0 + pad + scale, scale, 0xFFD54A, 1f);
    }
}
