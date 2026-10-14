package com.nateyburr.beyondthestars.client.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 (§2.1, §5.2): the shared black-hole program {@code bts_blackhole} (GLSL in {@code shaders/bh/lens.glsl}).
 * One camera-facing quad through the hole; each fragment traces its view ray backwards through the hole's field
 * (Schwarzschild bending plus a weak frame-dragging term), integrating a thin turbulent accretion disc on the way: the
 * shadow, the photon ring, the near side of the disc, its lensed far side over and under the shadow, Doppler beaming,
 * gravitational redshift and the opening / Hawking flashes all come out of one draw.
 * <ul>
 * <li>Drawn with premultiplied alpha ({@code ONE, ONE_MINUS_SRC_ALPHA}) and the depth test on, without depth writes;
 * empty fragments are discarded. A second draw of the same quad (colour writes off, no screen copy, L3's coarse
 * march with one octave that stops at alpha 0.05; the shadow and the photon ring without a march) then writes its depth
 * where the hole has a
 * body (alpha 0.05 and up): the black and the disc hide what draws later behind them (a window's far glass, water,
 * rain), while the faint glow and the photon-ring halo round them (light without alpha) no longer cut a circle out of
 * it (review of R-16).</li>
 * <li>At L0 with {@code renderBackgroundLensing} and no shaderpack the screen under the quad is copied
 * ({@link FrameCopy}) and bent; otherwise a lensed starfield is drawn inside the lensing ring (night only).</li>
 * <li>Steps, step size and noise octaves come from the LOD tier ({@link #STEPS}).</li>
 * </ul>
 * Users: the Kerr hole now; the quasar's hole, the Wormhole Mouth and Sgr A* later. Each fills a {@link Params}.
 */
@SideOnly(Side.CLIENT)
public final class BlackHoleLens {

    /**
     * The bounding radius of the effect over the disc's outer radius (§5.2 says 1.15; 1.08 holds the disc's lensed
     * image and its glow and saves an eighth of the pixels).
     */
    public static final float BOUND_PER_OUTER = 1.08f;

    /** Per LOD tier L0..L3: march steps, step size / r, noise octaves. */
    static final float[][] STEPS = { { 72, 0.11f, 4 }, { 32, 0.20f, 3 }, { 24, 0.28f, 2 }, { 14, 0.40f, 1 } };

    private static final ShaderRecipe RECIPE = BTSShaders.recipe("bts_blackhole")
        .required(
            "u_QuadC",
            "u_QuadU",
            "u_QuadV",
            "u_Cam",
            "u_Disc",
            "u_Look",
            "u_Look2",
            "u_Ray",
            "u_Lens",
            "u_Flash",
            "u_FrameRect",
            "u_DiscFrame",
            "u_ViewProj")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .sampler("u_Blackbody", NoiseTex.LUT_UNIT)
        .sampler("u_Frame", FrameCopy.UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_QUAD_C = RECIPE.uniform("u_QuadC");
    private static final Uniform U_QUAD_U = RECIPE.uniform("u_QuadU");
    private static final Uniform U_QUAD_V = RECIPE.uniform("u_QuadV");
    private static final Uniform U_CAM = RECIPE.uniform("u_Cam");
    private static final Uniform U_DISC = RECIPE.uniform("u_Disc");
    private static final Uniform U_LOOK = RECIPE.uniform("u_Look");
    private static final Uniform U_LOOK2 = RECIPE.uniform("u_Look2");
    private static final Uniform U_RAY = RECIPE.uniform("u_Ray");
    private static final Uniform U_LENS = RECIPE.uniform("u_Lens");
    private static final Uniform U_FLASH = RECIPE.uniform("u_Flash");
    private static final Uniform U_FRAME_RECT = RECIPE.uniform("u_FrameRect");
    private static final Uniform U_DISC_FRAME = RECIPE.uniform("u_DiscFrame");
    private static final Uniform U_VIEW_PROJ = RECIPE.uniform("u_ViewProj");

    /** The program (pass it to {@link RenderQuality#tier}). */
    public static final BTSShaders.Program PROGRAM = BTSShaders.program("bts_blackhole", RECIPE);
    private static final BTSShaders.Mesh QUAD = PROGRAM.mesh(Meshes::quad);

    /**
     * Dev only ({@code -Dbts.render.bhRepeat=n}, GPU measurement): draws the hole n times inside its timer section, so
     * {@code (t(n) - t(1)) / (n - 1)} is a draw's marginal (throughput) cost without the latency a timer query
     * serialises. The picture is wrong in such a run (the layers add up).
     */
    private static final int REPEAT = Math.max(1, Integer.getInteger("bts.render.bhRepeat", 1));
    /**
     * Dev only ({@code -Dbts.render.bhDepthDebug=true}): the depth pass draws in red with colour writes on, to check
     * where it writes depth against the hole it covers.
     */
    private static final boolean DEPTH_DEBUG = Boolean.getBoolean("bts.render.bhDepthDebug");
    /**
     * Dev only, GPU measurement: {@code -Dbts.render.bhDepthRepeat=n} draws the depth pass n times (its marginal cost
     * as {@link #REPEAT}'s), 0 skips it.
     */
    private static final int DEPTH_REPEAT = Math.max(0, Integer.getInteger("bts.render.bhDepthRepeat", 1));

    private static final Matrix4f MODEL = new Matrix4f();
    private static final Matrix4f PROJ = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Vector4f CORNER = new Vector4f();
    private static final FloatBuffer M16 = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer M9 = BufferUtils.createFloatBuffer(9);
    private static final ByteBuffer COLOR_MASK = BufferUtils.createByteBuffer(16);

    private BlackHoleLens() {}

    /** One hole's look this frame (filled by its renderer, render thread only). Lengths in r_s unless noted. */
    public static final class Params {

        /** The hole centre relative to the camera, blocks. */
        public double cx, cy, cz;
        /** Blocks per r_s (already multiplied by any growth / collapse). */
        public double rsBlocks;
        /** Disc inner edge, outer edge; half-thickness over r. */
        public float rIn = 1.45f, rOut = 7.5f, thickness = 0.02f;
        /** Peak disc temperature (display), K. */
        public float temperature = 6000f;
        /** Turbulence amplitude 0..1. */
        public float turbulence = 0.25f;
        /** Exposure (emission scale). */
        public float exposure = 1.2f;
        /** Infall streamers 0..1. */
        public float streamers;
        /** Doppler beaming exponent (intensity x delta^beaming). */
        public float beaming = 2.5f;
        /** Photon ring gain, soft glow gain. */
        public float ring = 2.5f, glow = 0.07f;
        /** Frame dragging (the D-shaped shadow), 0 for a non-spinning hole. */
        public float drag = 0.35f;
        /** The disc's tilt from horizontal (radians) and the azimuth of its tilt axis (precession). */
        public float tilt, tiltAzimuth;
        /** Opening ring radius / intensity, Hawking ring radius / intensity (r_s; intensity 0 = none). */
        public float openRadius, openIntensity, hawkRadius, hawkIntensity;
        /** Shader clock, seconds ({@link RenderClock}). */
        public float time;
        /** Star brightness 0..1 for the lensed starfield (the world's night sky). */
        public float stars;
    }

    /** True when the program and its quad are both there, so {@link #draw} will draw (check before drawing layers). */
    public static boolean usable() {
        return PROGRAM.get() != null && QUAD.get() != null;
    }

    /**
     * Draws the hole at {@code tier} (L0..L3). Returns false when the program is off (draw the fixed path instead).
     * Leaves the GL state as it found it.
     */
    public static boolean draw(Params p, Lod.Tier tier) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject quad = QUAD.get();
        if (h == null || quad == null) return false;
        final double dist = Math.sqrt(p.cx * p.cx + p.cy * p.cy + p.cz * p.cz);
        final Billboard bb = Billboard.at(p.cx, p.cy, p.cz);
        if (bb == null || p.rsBlocks <= 1e-5) return true;
        // inside the horizon: nothing sensible to draw
        if (dist < p.rsBlocks) return true;

        final double rb = p.rOut * BOUND_PER_OUTER;
        final double rbBlocks = rb * p.rsBlocks;
        // the quad through the centre that covers the bounding sphere's silhouette (and the flash rings)
        double rq = dist > rbBlocks * 1.02 ? rbBlocks * dist / Math.sqrt(dist * dist - rbBlocks * rbBlocks)
            : Math.max(rbBlocks, dist) * 8;
        final double flash = Math
            .max(p.openIntensity > 0 ? p.openRadius * 1.5 + 1 : 0, p.hawkIntensity > 0 ? p.hawkRadius * 1.5 + 1 : 0)
            * p.rsBlocks;
        rq = Math.max(rq, flash * 1.1);
        final double ux = bb.u[0] * rq, uy = bb.u[1] * rq, uz = bb.u[2] * rq;
        final double vx = bb.v[0] * rq, vy = bb.v[1] * rq, vz = bb.v[2] * rq;
        bb.model(MODEL, p.cx, p.cy, p.cz, rq);

        final int ti = Math.min(3, Math.max(0, tier.ordinal()));
        final float[] steps = STEPS[ti];
        final boolean lens = RenderQuality.backgroundLensing(tier);

        // camera -> clip for camera-relative positions (the TESR modelview holds only the view rotation)
        M16.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, M16);
        PROJ.set(M16);
        M16.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, M16);
        VIEW.set(M16);
        PROJ.mul(VIEW);

        final GlSave save = GlSave.capture();
        int frameWas = -1;
        final int[] noiseWas = NoiseTex.bind();
        float fx0 = 0, fy0 = 0, fx1 = 0, fy1 = 0;
        boolean maskSaved = false;
        try {
            if (lens) {
                final Minecraft mc = Minecraft.getMinecraft();
                final int w = mc.displayWidth, hgt = mc.displayHeight;
                final int[] rect = screenRect(p.cx, p.cy, p.cz, ux, uy, uz, vx, vy, vz, w, hgt);
                frameWas = FrameCopy.copy(rect[0], rect[1], rect[2], rect[3]);
                fx0 = rect[0] / (float) w;
                fy0 = rect[1] / (float) hgt;
                fx1 = rect[2] / (float) w;
                fy1 = rect[3] / (float) hgt;
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            h.use();
            h.uploadModel(MODEL);
            GL20.glUniform3f(h.loc(U_QUAD_C), (float) p.cx, (float) p.cy, (float) p.cz);
            GL20.glUniform3f(h.loc(U_QUAD_U), (float) ux, (float) uy, (float) uz);
            GL20.glUniform3f(h.loc(U_QUAD_V), (float) vx, (float) vy, (float) vz);
            final double inv = 1.0 / p.rsBlocks;
            GL20.glUniform3f(h.loc(U_CAM), (float) (-p.cx * inv), (float) (-p.cy * inv), (float) (-p.cz * inv));
            GL20.glUniform4f(h.loc(U_DISC), p.rIn, p.rOut, p.thickness, (float) rb);
            GL20.glUniform4f(h.loc(U_LOOK), p.temperature, p.turbulence, p.time, p.exposure);
            GL20.glUniform4f(h.loc(U_LOOK2), p.streamers, p.beaming, p.ring, p.glow);
            GL20.glUniform4f(h.loc(U_RAY), steps[0], steps[1], steps[2], p.drag);
            final double pxBlocks = dist / Math.max(1e-3, Lod.pixelsPerBlockAtUnit());
            final float pxRs = (float) (pxBlocks * inv);
            GL20.glUniform4f(h.loc(U_LENS), lens ? 1f : 0f, p.stars, pxRs, 0f);
            GL20.glUniform4f(h.loc(U_FLASH), p.openRadius, p.openIntensity, p.hawkRadius, p.hawkIntensity);
            GL20.glUniform4f(h.loc(U_FRAME_RECT), fx0, fy0, fx1, fy1);
            GL20.glUniformMatrix3(h.loc(U_DISC_FRAME), false, discFrame(p.tilt, p.tiltAzimuth));
            M16.clear();
            PROJ.get(M16);
            GL20.glUniformMatrix4(h.loc(U_VIEW_PROJ), false, M16);
            quad.bind();
            for (int i = 0; i < REPEAT; i++) quad.draw();
            // the depth pass, colour writes off: depth only where the hole has a body, alpha 0.05 and up (the
            // shadow and the photon ring without a march, else a coarse one: no screen copy, L3's 14 steps, one
            // octave)
            if (DEPTH_REPEAT > 0) {
                final float[] ds = STEPS[3];
                GL20.glUniform4f(h.loc(U_RAY), ds[0], ds[1], 1f, p.drag);
                COLOR_MASK.clear();
                GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, COLOR_MASK);
                maskSaved = true;
                if (!DEPTH_DEBUG) GL11.glColorMask(false, false, false, false);
                GL11.glDepthMask(true);
                GL20.glUniform4f(h.loc(U_LENS), 0f, p.stars, pxRs, DEPTH_DEBUG ? 2f : 1f);
                for (int i = 0; i < DEPTH_REPEAT; i++) quad.draw();
            }
            quad.unbind();
            ShaderProgram.clear();
        } finally {
            if (maskSaved) {
                GL11.glColorMask(
                    COLOR_MASK.get(0) != 0,
                    COLOR_MASK.get(1) != 0,
                    COLOR_MASK.get(2) != 0,
                    COLOR_MASK.get(3) != 0);
            }
            if (frameWas >= 0) FrameCopy.unbind(frameWas);
            NoiseTex.unbind(noiseWas);
            save.restore();
        }
        return true;
    }

    /**
     * The disc frame as a column-major 3 x 3 whose rows are the disc's x axis, its normal (the spin axis) and its z
     * axis: the normal tilted by {@code tilt} toward the azimuth {@code az}.
     */
    private static FloatBuffer discFrame(float tilt, float az) {
        final double st = Math.sin(tilt), ct = Math.cos(tilt);
        final double nx = st * Math.cos(az), ny = ct, nz = st * Math.sin(az);
        // e2: world z made perpendicular to n; e1 = n x e2
        double ex = -nz * nx, ey = -nz * ny, ez = 1 - nz * nz;
        final double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
        ex /= el;
        ey /= el;
        ez /= el;
        final double ax = ny * ez - nz * ey, ay = nz * ex - nx * ez, az2 = nx * ey - ny * ex;
        M9.clear();
        // column-major: column j holds (e1_j, n_j, e2_j)
        M9.put((float) ax)
            .put((float) nx)
            .put((float) ex);
        M9.put((float) ay)
            .put((float) ny)
            .put((float) ey);
        M9.put((float) az2)
            .put((float) nz)
            .put((float) ez);
        M9.flip();
        return M9;
    }

    /** The rectangle {@link #screenRect} returns (shared: R-16, a frame allocates nothing). */
    private static final int[] RECT = new int[4];

    private static int[] rect(int x0, int y0, int x1, int y1) {
        RECT[0] = x0;
        RECT[1] = y0;
        RECT[2] = x1;
        RECT[3] = y1;
        return RECT;
    }

    /** The window-pixel rectangle {x0, y0, x1, y1} the quad covers (the whole window if a corner is behind). */
    private static int[] screenRect(double cx, double cy, double cz, double ux, double uy, double uz, double vx,
        double vy, double vz, int w, int h) {
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            final double su = (i & 1) == 0 ? -1 : 1, sv = (i & 2) == 0 ? -1 : 1;
            CORNER.set(
                (float) (cx + su * ux + sv * vx),
                (float) (cy + su * uy + sv * vy),
                (float) (cz + su * uz + sv * vz),
                1f);
            PROJ.transform(CORNER);
            if (CORNER.w < 1e-3f) return rect(0, 0, w, h);
            final double px = (CORNER.x / CORNER.w * 0.5 + 0.5) * w, py = (CORNER.y / CORNER.w * 0.5 + 0.5) * h;
            x0 = Math.min(x0, px);
            y0 = Math.min(y0, py);
            x1 = Math.max(x1, px);
            y1 = Math.max(y1, py);
        }
        return rect(
            (int) Math.floor(x0) - 2,
            (int) Math.floor(y0) - 2,
            (int) Math.ceil(x1) + 2,
            (int) Math.ceil(y1) + 2);
    }
}
