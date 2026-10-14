package com.nateyburr.beyondthestars.client.render;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.1): the two shared textures every program samples, built once (no per-instance textures).
 * <ul>
 * <li><b>Noise</b>, 256 x 256 RGBA, repeat, linear: white-noise texels, so a linear lookup is tileable value noise
 * ({@code lib/noise.glsl}). R, B and A are independent; G is R shifted by (37, 17) texels, the classic trick that
 * gives 3D value noise from one 2D lookup ({@code btsNoise3}).</li>
 * <li><b>Blackbody LUT</b>, 256 x 1 RGB, clamp, linear: {@link Blackbody#rgb} from 1,000 to 40,000 K on a log scale
 * ({@code lib/blackbody.glsl}).</li>
 * </ul>
 * They are bound on texture units {@value #NOISE_UNIT} and {@value #LUT_UNIT}, away from Minecraft's 0 (blocks) and 1
 * (lightmap); {@link #bind()} returns what was bound there before and {@link #unbind(int[])} puts it back. Built
 * lazily, deleted on a resource reload (GL ids survive a reload, but a context loss would not).
 */
@SideOnly(Side.CLIENT)
public final class NoiseTex {

    public static final int NOISE_UNIT = 4, LUT_UNIT = 5;
    public static final int NOISE_SIZE = 256, LUT_SIZE = 256;
    /** The 3D-noise shift of the G channel, texels (lib/noise.glsl uses the same). */
    public static final int SHIFT_X = 37, SHIFT_Y = 17;

    private static int noise, lut;

    private NoiseTex() {}

    static void markReload() {
        if (noise != 0) GL11.glDeleteTextures(noise);
        if (lut != 0) GL11.glDeleteTextures(lut);
        noise = 0;
        lut = 0;
    }

    /** The previous bindings each nested {@link #bind} saved (render thread only). */
    private static final int[][] SAVED = new int[8][2];
    private static int depth;

    /** Each frame starts with an empty save stack (a draw that skipped its unbind cannot leak into the next). */
    static void onFrameStart() {
        depth = 0;
    }

    /** The noise texture id (built on first use). */
    public static int noise() {
        if (noise == 0) noise = upload(noiseTexels(), NOISE_SIZE, NOISE_SIZE, GL11.GL_REPEAT);
        return noise;
    }

    /** The blackbody LUT id (built on first use). */
    public static int blackbodyLut() {
        if (lut == 0) lut = upload(lutTexels(), LUT_SIZE, 1, GL12.GL_CLAMP_TO_EDGE);
        return lut;
    }

    /**
     * Binds the noise on {@link #NOISE_UNIT} and the LUT on {@link #LUT_UNIT}, leaving unit 0 active; returns the
     * previous bindings for {@link #unbind}.
     */
    public static int[] bind() {
        final int n = noise(), l = blackbodyLut();
        // R-16: the saves come from a small stack (binds nest: the quasar's, then its hole's), so a frame allocates
        // nothing; unbind in reverse order (a finally does). Deeper than the stack: a fresh array, never a failure
        final int[] was = depth < SAVED.length ? SAVED[depth] : new int[2];
        depth++;
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + NOISE_UNIT);
        was[0] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, n);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + LUT_UNIT);
        was[1] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, l);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        return was;
    }

    /** Restores the bindings {@link #bind} returned. */
    public static void unbind(int[] was) {
        if (depth > 0) depth--;
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + NOISE_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, was[0]);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + LUT_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, was[1]);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /** RGBA bytes of the noise texture (deterministic: the same on every client). */
    static byte[] noiseTexels() {
        final int n = NOISE_SIZE;
        final byte[] r = new byte[n * n], b = new byte[n * n], a = new byte[n * n];
        long s = 0x5DEECE66DL ^ 20261013L;
        for (int i = 0; i < n * n; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            r[i] = (byte) (s >>> 56);
            b[i] = (byte) (s >>> 48);
            a[i] = (byte) (s >>> 40);
        }
        final byte[] out = new byte[n * n * 4];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                final int i = y * n + x;
                // G(x, y) = R(x - 37, y - 17): lib/noise.glsl's btsNoise3 reads layer z from G and z + 1 from R
                final int j = ((y - SHIFT_Y) & (n - 1)) * n + ((x - SHIFT_X) & (n - 1));
                out[i * 4] = r[i];
                out[i * 4 + 1] = r[j];
                out[i * 4 + 2] = b[i];
                out[i * 4 + 3] = a[i];
            }
        }
        return out;
    }

    /** RGBA bytes of the blackbody LUT. */
    static byte[] lutTexels() {
        final byte[] out = new byte[LUT_SIZE * 4];
        for (int i = 0; i < LUT_SIZE; i++) {
            // texel centres: u = (i + 0.5) / 256 is the coordinate the shader asks for
            final float[] c = Blackbody.rgb(Blackbody.kelvinAt((i + 0.5) / LUT_SIZE));
            out[i * 4] = (byte) Math.round(c[0] * 255);
            out[i * 4 + 1] = (byte) Math.round(c[1] * 255);
            out[i * 4 + 2] = (byte) Math.round(c[2] * 255);
            out[i * 4 + 3] = (byte) 255;
        }
        return out;
    }

    private static int upload(byte[] texels, int w, int h, int wrap) {
        final ByteBuffer buf = BufferUtils.createByteBuffer(texels.length);
        buf.put(texels)
            .flip();
        final int was = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        final int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, was);
        return id;
    }
}
