package com.nateyburr.beyondthestars.client.render;

import java.nio.ByteBuffer;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§4.3): the copy of the screen a black hole bends (framebuffer-copy lensing, HIGH / L0, no shaderpack).
 * One texture the size of the display; {@link #copy} copies a rectangle of the bound framebuffer into it at the same
 * offsets ({@code glCopyTexSubImage2D}), so a shader samples it with plain screen coordinates. It stays bound on
 * {@link #UNIT} until {@link #unbind}.
 */
@SideOnly(Side.CLIENT)
public final class FrameCopy {

    /** The texture unit the copy is bound on (the kit's noise and LUT use 4 and 5). */
    public static final int UNIT = 6;

    private static int tex;
    private static int texW, texH;

    private FrameCopy() {}

    /**
     * Copies the window pixels {@code [x0, x1) x [y0, y1)} (GL window coordinates, origin bottom left) and leaves the
     * copy bound on {@link #UNIT} (unit 0 active again). Returns the previous binding of that unit, for
     * {@link #unbind}.
     */
    public static int copy(int x0, int y0, int x1, int y1) {
        final Minecraft mc = Minecraft.getMinecraft();
        final int w = Math.max(1, mc.displayWidth), h = Math.max(1, mc.displayHeight);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        final int was = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (tex == 0 || texW != w || texH != h) {
            if (tex != 0) GL11.glDeleteTextures(tex);
            tex = GL11.glGenTextures();
            texW = w;
            texH = h;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                w,
                h,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        }
        final int cx0 = clamp(x0, 0, w), cy0 = clamp(y0, 0, h), cx1 = clamp(x1, 0, w), cy1 = clamp(y1, 0, h);
        if (cx1 > cx0 && cy1 > cy0) {
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, cx0, cy0, cx0, cy0, cx1 - cx0, cy1 - cy0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        return was;
    }

    /** Binds whatever was bound on {@link #UNIT} before {@link #copy}; unit 0 active again. */
    public static void unbind(int was) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, was);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /** GT's resource reload: drop the texture (rebuilt on the next copy). */
    static void markReload() {
        if (tex != 0) {
            GL11.glDeleteTextures(tex);
            tex = 0;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
