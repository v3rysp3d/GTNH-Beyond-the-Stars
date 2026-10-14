package com.nateyburr.beyondthestars.client.render;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§4): the fixed-function shapes of the LOW path, shared by every renderer (moved unchanged out of the
 * Eye's star renderer, render QA rounds 3 and 4). All coordinates are camera-relative (the camera at the origin), as
 * a TESR gets them; the caller sets the blend and the culling ({@link Layers}).
 * <ul>
 * <li>{@link #ring}: a flat camera-facing ring or disc with a linear alpha falloff (the glow disc, the corona);</li>
 * <li>{@link #rimShell}: a rim-weighted additive sphere, {@code alpha = centre + (edge - centre) (1 - |N.V|)^3}, back
 * faces then front faces (shock fronts, magnetospheres);</li>
 * <li>{@link #sphere}: an untextured sphere with a per-corner alpha from N.V (limb darkening, veils);</li>
 * <li>{@link #billboardQuad}: one camera-facing square (galaxy dots, sparks).</li>
 * </ul>
 * The sphere is {@value #SLICES} x {@value #STACKS} and built once: building it per frame cost about 0.4 ms (round 4).
 */
@SideOnly(Side.CLIENT)
public final class FixedShapes {

    /** Segments of every untextured sphere (render QA round 4: at most 32 x 16 per layer). */
    public static final int SLICES = 32, STACKS = 16;
    /** Segments of a ring. */
    public static final int RING_SEGMENTS = 48;

    /**
     * The corners of every quad of a unit sphere (x, y, z per corner, four corners per quad), wound counter-clockwise
     * seen from outside.
     */
    private static final float[] UNIT_SPHERE = unitSphere();
    /** Per-corner alpha of the sphere being drawn (render thread only). */
    private static final float[] CORNER_ALPHA = new float[UNIT_SPHERE.length / 3];
    private static final double[] RING_COS = new double[RING_SEGMENTS + 1], RING_SIN = new double[RING_SEGMENTS + 1];

    static {
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            RING_COS[i] = Math.cos(2 * Math.PI * i / RING_SEGMENTS);
            RING_SIN[i] = Math.sin(2 * Math.PI * i / RING_SEGMENTS);
        }
    }

    private FixedShapes() {}

    private static float[] unitSphere() {
        final float[] v = new float[STACKS * SLICES * 12];
        int k = 0;
        for (int i = 0; i < STACKS; i++) {
            final double p0 = Math.PI * i / STACKS - Math.PI / 2;
            final double p1 = Math.PI * (i + 1) / STACKS - Math.PI / 2;
            final double y0 = Math.sin(p0), y1 = Math.sin(p1);
            final double r0 = Math.cos(p0), r1 = Math.cos(p1);
            for (int j = 0; j < SLICES; j++) {
                final double a = 2 * Math.PI * j / SLICES, c = 2 * Math.PI * (j + 1) / SLICES;
                final double c0 = Math.cos(a), s0 = Math.sin(a), c1 = Math.cos(c), s1 = Math.sin(c);
                final double[] q = { r0 * c0, y0, r0 * s0, r1 * c0, y1, r1 * s0, r1 * c1, y1, r1 * s1, r0 * c1, y0,
                    r0 * s1 };
                for (double d : q) v[k++] = (float) d;
            }
        }
        return v;
    }

    /**
     * The vertex radius of a {@link #SLICES} x {@link #STACKS} sphere whose flat faces all lie at least {@code minRad}
     * from the centre: 1 / (cos(half a slice) x cos(half a stack)) further out. With the vertices AT a core's radius
     * the faces sank up to 1 % inside it, i.e. inside the core's depth, and the depth test hid them (round 3).
     */
    public static double outside(double minRad) {
        return minRad / (Math.cos(Math.PI / SLICES) * Math.cos(Math.PI / (2 * STACKS)));
    }

    /**
     * A rim-weighted additive shell: alpha {@code centre + (edge - centre) (1 - |N.V|)^3}. Back faces first, then front
     * faces (the blend is additive, so the order only matters for the depth test against what is already drawn).
     * Leaves culling on, back faces culled.
     */
    public static void rimShell(Tessellator t, double cx, double cy, double cz, double rad, float r, float g, float b,
        float centre, float edge) {
        if (edge <= 0.003f || rad <= 0.05) return;
        alphas(cx, cy, cz, rad, centre, edge - centre, true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glCullFace(GL11.GL_FRONT);
        emit(t, cx, cy, cz, rad, r, g, b);
        GL11.glCullFace(GL11.GL_BACK);
        emit(t, cx, cy, cz, rad, r, g, b);
    }

    /**
     * One untextured sphere of vertex radius {@code rad} with per-corner alpha {@code a0 + a1 (1 - |N.V|)^3}
     * ({@code cubic}) or {@code a0 + a1 (1 - |N.V|)} (the caller sets the blend and the culling).
     */
    public static void sphere(Tessellator t, double cx, double cy, double cz, double rad, float r, float g, float b,
        float a0, float a1, boolean cubic) {
        alphas(cx, cy, cz, rad, a0, a1, cubic);
        emit(t, cx, cy, cz, rad, r, g, b);
    }

    /** Fills {@link #CORNER_ALPHA} for a sphere of vertex radius {@code rad} round {@code (cx, cy, cz)}. */
    private static void alphas(double cx, double cy, double cz, double rad, float a0, float a1, boolean cubic) {
        final float[] n = UNIT_SPHERE;
        for (int i = 0, k = 0; k < CORNER_ALPHA.length; i += 3, k++) {
            final double px = cx + n[i] * rad, py = cy + n[i + 1] * rad, pz = cz + n[i + 2] * rad;
            final double pl = Math.sqrt(px * px + py * py + pz * pz);
            final double nv = pl < 1e-6 ? 1 : Math.abs(n[i] * px + n[i + 1] * py + n[i + 2] * pz) / pl;
            final double rim = 1 - Math.min(1, nv);
            final double a = a0 + a1 * (cubic ? rim * rim * rim : rim);
            CORNER_ALPHA[k] = (float) Math.max(0, Math.min(1, a));
        }
    }

    /** Draws the sphere with the alphas of the last {@link #alphas} call. */
    private static void emit(Tessellator t, double cx, double cy, double cz, double rad, float r, float g, float b) {
        final float[] n = UNIT_SPHERE;
        t.startDrawingQuads();
        for (int i = 0, k = 0; k < CORNER_ALPHA.length; i += 3, k++) {
            t.setColorRGBA_F(r, g, b, CORNER_ALPHA[k]);
            t.addVertex(cx + n[i] * rad, cy + n[i + 1] * rad, cz + n[i + 2] * rad);
        }
        t.draw();
    }

    /**
     * A flat ring (or a disc, {@code inner} 0) centred on {@code (ox, oy, oz)} in the plane spanned by {@code u} and
     * {@code v} (a {@link Billboard} faces the camera): {@code alpha} at the inner edge falling linearly to 0 at
     * {@code outer}.
     */
    public static void ring(Tessellator t, double ox, double oy, double oz, double[] u, double[] v, double inner,
        double outer, float r, float g, float b, float alpha) {
        if (alpha <= 0.003f || outer <= inner) return;
        t.startDrawingQuads();
        for (int i = 0; i < RING_SEGMENTS; i++) {
            final double ca = RING_COS[i], sa = RING_SIN[i], cc = RING_COS[i + 1], sc = RING_SIN[i + 1];
            t.setColorRGBA_F(r, g, b, alpha);
            t.addVertex(
                ox + (u[0] * ca + v[0] * sa) * inner,
                oy + (u[1] * ca + v[1] * sa) * inner,
                oz + (u[2] * ca + v[2] * sa) * inner);
            t.setColorRGBA_F(r, g, b, 0f);
            t.addVertex(
                ox + (u[0] * ca + v[0] * sa) * outer,
                oy + (u[1] * ca + v[1] * sa) * outer,
                oz + (u[2] * ca + v[2] * sa) * outer);
            t.addVertex(
                ox + (u[0] * cc + v[0] * sc) * outer,
                oy + (u[1] * cc + v[1] * sc) * outer,
                oz + (u[2] * cc + v[2] * sc) * outer);
            t.setColorRGBA_F(r, g, b, alpha);
            t.addVertex(
                ox + (u[0] * cc + v[0] * sc) * inner,
                oy + (u[1] * cc + v[1] * sc) * inner,
                oz + (u[2] * cc + v[2] * sc) * inner);
        }
        t.draw();
    }

    /**
     * One camera-facing square of half-size {@code s} at {@code (x, y, z)}, into a Tessellator already drawing quads
     * (the caller batches many).
     */
    public static void billboardQuad(Tessellator t, double x, double y, double z, double[] u, double[] v, double s,
        float r, float g, float b, float alpha) {
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertex(x - (u[0] + v[0]) * s, y - (u[1] + v[1]) * s, z - (u[2] + v[2]) * s);
        t.addVertex(x + (u[0] - v[0]) * s, y + (u[1] - v[1]) * s, z + (u[2] - v[2]) * s);
        t.addVertex(x + (u[0] + v[0]) * s, y + (u[1] + v[1]) * s, z + (u[2] + v[2]) * s);
        t.addVertex(x - (u[0] - v[0]) * s, y - (u[1] - v[1]) * s, z - (u[2] - v[2]) * s);
    }
}
