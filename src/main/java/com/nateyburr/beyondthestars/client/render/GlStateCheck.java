package com.nateyburr.beyondthestars.client.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (R-16, dev only): the GL state leak check. With {@code -Dbts.render.leakcheck=true} the GL state is read
 * before and after every renderer's draw (the outermost {@link GpuTimer} section, which wraps each renderer) and every
 * difference is counted per renderer and state: the bound program, the active texture unit, the 2D texture bound on
 * units 0-7, texturing on units 0 and 1, the vertex / index buffer and vertex array bindings, the framebuffers, the
 * blend switch, functions and equation, the depth test, function and mask, the alpha test and its reference, culling
 * and its face and winding, lighting, light 0 and 1, fog, the shade model, colour material, normalize, rescale normal,
 * the colour mask, polygon offset and mode, scissor and stencil tests, point sprites and the program point size, the
 * fixed-function and generic vertex arrays, the matrix mode, the three matrix stack depths and the modelview,
 * projection and texture matrices, the current colour, line width, point size and the lightmap coordinates
 * ({@code OpenGlHelper.lastBrightnessX/Y}). A renderer that leaves any of them changed would change how what is drawn
 * after it looks (other mods' tiles and entities, the hand, the GUI, NEI).
 * <p>
 * About 90 {@code glGet}s per section: a check run, never a timing run.
 */
@SideOnly(Side.CLIENT)
public final class GlStateCheck {

    public static final boolean ENABLED = Boolean.getBoolean("bts.render.leakcheck");

    private static final String[] INT_NAMES = new String[72];
    private static final String[] FLOAT_NAMES = new String[64];
    private static final int[] BEFORE_I = new int[INT_NAMES.length], AFTER_I = new int[INT_NAMES.length];
    private static final float[] BEFORE_F = new float[FLOAT_NAMES.length], AFTER_F = new float[FLOAT_NAMES.length];
    private static final IntBuffer IB = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer FB = BufferUtils.createFloatBuffer(16);
    private static int nInts, nFloats;

    /** "key: state" -> {count, example}. */
    private static final Map<String, Object[]> LEAKS = new LinkedHashMap<>();
    private static long sections;
    private static String openKey;

    private GlStateCheck() {}

    /** Before a renderer draws (outermost section only). */
    static void before(String key) {
        openKey = key;
        read(BEFORE_I, BEFORE_F);
    }

    /** After it drew: count every state that differs. */
    static void after() {
        if (openKey == null) return;
        read(AFTER_I, AFTER_F);
        sections++;
        for (int i = 0; i < nInts; i++) {
            if (BEFORE_I[i] != AFTER_I[i]) leak(INT_NAMES[i], BEFORE_I[i] + " -> " + AFTER_I[i]);
        }
        for (int i = 0; i < nFloats; i++) {
            if (Float.floatToIntBits(BEFORE_F[i]) != Float.floatToIntBits(AFTER_F[i])) {
                leak(FLOAT_NAMES[i], BEFORE_F[i] + " -> " + AFTER_F[i]);
            }
        }
        openKey = null;
    }

    private static void leak(String state, String example) {
        final String k = openKey + ": " + state;
        Object[] e = LEAKS.get(k);
        if (e == null) {
            e = new Object[] { 0, example };
            LEAKS.put(k, e);
            BeyondTheStars.LOG.warn("[BTS render] GL state leak after {}: {} {}", openKey, state, example);
        }
        e[0] = (Integer) e[0] + 1;
    }

    /** The sections checked and the leaks since the last {@link #reset()}; "" when the check is off. */
    public static String report() {
        if (!ENABLED) return "";
        if (LEAKS.isEmpty()) return String.format(Locale.ROOT, "%d draws checked, no GL state leaks", sections);
        final StringBuilder b = new StringBuilder();
        b.append(String.format(Locale.ROOT, "%d draws checked, LEAKS: ", sections));
        boolean first = true;
        for (Map.Entry<String, Object[]> e : LEAKS.entrySet()) {
            if (!first) b.append("; ");
            first = false;
            b.append(e.getKey())
                .append(" x")
                .append(e.getValue()[0])
                .append(" (")
                .append(e.getValue()[1])
                .append(')');
        }
        return b.toString();
    }

    /** A new window (the harness: each shot). */
    public static void reset() {
        LEAKS.clear();
        sections = 0;
    }

    // ================================================================================ reading

    private static void read(int[] ints, float[] floats) {
        final ContextCapabilities caps = GLContext.getCapabilities();
        int i = 0;
        int f = 0;
        i = geti(ints, i, "program", GL20.GL_CURRENT_PROGRAM);
        final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        ints[i] = active;
        INT_NAMES[i++] = "active texture";
        i = geti(ints, i, "client active texture", GL13.GL_CLIENT_ACTIVE_TEXTURE);
        for (int u = 0; u < 8; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            ints[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            INT_NAMES[i++] = "texture bound on unit " + u;
            if (u < 2) {
                ints[i] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D) ? 1 : 0;
                INT_NAMES[i++] = "texturing on unit " + u;
            }
        }
        GL13.glActiveTexture(active);
        i = geti(ints, i, "array buffer", GL15.GL_ARRAY_BUFFER_BINDING);
        i = geti(ints, i, "element buffer", GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        if (caps.OpenGL30) {
            i = geti(ints, i, "vertex array", GL30.GL_VERTEX_ARRAY_BINDING);
            i = geti(ints, i, "draw framebuffer", GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            i = geti(ints, i, "read framebuffer", GL30.GL_READ_FRAMEBUFFER_BINDING);
        }
        i = on(ints, i, "blend", GL11.GL_BLEND);
        i = geti(ints, i, "blend src rgb", GL14.GL_BLEND_SRC_RGB);
        i = geti(ints, i, "blend dst rgb", GL14.GL_BLEND_DST_RGB);
        i = geti(ints, i, "blend src alpha", GL14.GL_BLEND_SRC_ALPHA);
        i = geti(ints, i, "blend dst alpha", GL14.GL_BLEND_DST_ALPHA);
        i = geti(ints, i, "blend equation", GL20.GL_BLEND_EQUATION_RGB);
        i = on(ints, i, "depth test", GL11.GL_DEPTH_TEST);
        i = geti(ints, i, "depth func", GL11.GL_DEPTH_FUNC);
        ints[i] = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0;
        INT_NAMES[i++] = "depth mask";
        i = on(ints, i, "alpha test", GL11.GL_ALPHA_TEST);
        i = geti(ints, i, "alpha func", GL11.GL_ALPHA_TEST_FUNC);
        i = on(ints, i, "cull face", GL11.GL_CULL_FACE);
        i = geti(ints, i, "cull face mode", GL11.GL_CULL_FACE_MODE);
        i = geti(ints, i, "front face", GL11.GL_FRONT_FACE);
        i = on(ints, i, "lighting", GL11.GL_LIGHTING);
        i = on(ints, i, "light 0", GL11.GL_LIGHT0);
        i = on(ints, i, "light 1", GL11.GL_LIGHT1);
        i = on(ints, i, "fog", GL11.GL_FOG);
        i = geti(ints, i, "shade model", GL11.GL_SHADE_MODEL);
        i = on(ints, i, "colour material", GL11.GL_COLOR_MATERIAL);
        i = on(ints, i, "normalize", GL11.GL_NORMALIZE);
        i = on(ints, i, "rescale normal", GL12.GL_RESCALE_NORMAL);
        IB.clear();
        GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, IB);
        ints[i] = IB.get(0) | IB.get(1) << 1 | IB.get(2) << 2 | IB.get(3) << 3;
        INT_NAMES[i++] = "colour mask (rgba bits)";
        i = on(ints, i, "polygon offset fill", GL11.GL_POLYGON_OFFSET_FILL);
        IB.clear();
        GL11.glGetInteger(GL11.GL_POLYGON_MODE, IB);
        ints[i] = IB.get(0);
        INT_NAMES[i++] = "polygon mode";
        i = on(ints, i, "scissor test", GL11.GL_SCISSOR_TEST);
        i = on(ints, i, "stencil test", GL11.GL_STENCIL_TEST);
        i = on(ints, i, "program point size", GL20.GL_VERTEX_PROGRAM_POINT_SIZE);
        i = on(ints, i, "point sprite", GL20.GL_POINT_SPRITE);
        i = on(ints, i, "vertex array (client)", GL11.GL_VERTEX_ARRAY);
        i = on(ints, i, "colour array (client)", GL11.GL_COLOR_ARRAY);
        i = on(ints, i, "normal array (client)", GL11.GL_NORMAL_ARRAY);
        i = on(ints, i, "texcoord array (client)", GL11.GL_TEXTURE_COORD_ARRAY);
        for (int a = 0; a < 8; a++) {
            IB.clear();
            GL20.glGetVertexAttrib(a, GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED, IB);
            ints[i] = IB.get(0);
            INT_NAMES[i++] = "vertex attrib array " + a;
        }
        i = geti(ints, i, "matrix mode", GL11.GL_MATRIX_MODE);
        i = geti(ints, i, "modelview stack depth", GL11.GL_MODELVIEW_STACK_DEPTH);
        i = geti(ints, i, "projection stack depth", GL11.GL_PROJECTION_STACK_DEPTH);
        i = geti(ints, i, "texture stack depth", GL11.GL_TEXTURE_STACK_DEPTH);
        nInts = i;

        floats[f] = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        FLOAT_NAMES[f++] = "alpha ref";
        FB.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, FB);
        for (int c = 0; c < 4; c++) {
            floats[f] = FB.get(c);
            FLOAT_NAMES[f++] = "current colour " + "rgba".charAt(c);
        }
        floats[f] = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        FLOAT_NAMES[f++] = "line width";
        floats[f] = GL11.glGetFloat(GL11.GL_POINT_SIZE);
        FLOAT_NAMES[f++] = "point size";
        floats[f] = OpenGlHelper.lastBrightnessX;
        FLOAT_NAMES[f++] = "lightmap x (lastBrightnessX)";
        floats[f] = OpenGlHelper.lastBrightnessY;
        FLOAT_NAMES[f++] = "lightmap y (lastBrightnessY)";
        f = matrix(floats, f, "modelview", GL11.GL_MODELVIEW_MATRIX);
        f = matrix(floats, f, "projection", GL11.GL_PROJECTION_MATRIX);
        f = matrix(floats, f, "texture matrix", GL11.GL_TEXTURE_MATRIX);
        nFloats = f;
    }

    private static int geti(int[] ints, int i, String name, int pname) {
        ints[i] = GL11.glGetInteger(pname);
        INT_NAMES[i] = name;
        return i + 1;
    }

    private static int on(int[] ints, int i, String name, int cap) {
        ints[i] = GL11.glIsEnabled(cap) ? 1 : 0;
        INT_NAMES[i] = name;
        return i + 1;
    }

    private static int matrix(float[] floats, int f, String name, int pname) {
        FB.clear();
        GL11.glGetFloat(pname, FB);
        for (int k = 0; k < 16; k++) {
            floats[f] = FB.get(k);
            FLOAT_NAMES[f++] = name + "[" + k + "]";
        }
        return f;
    }
}
