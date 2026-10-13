package com.nateyburr.beyondthestars.client.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.MeshBuilder;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 (§10-§12, WP-6..WP-8): the three programs the machines without a render tile share (the Homogeny, the
 * two Reality machines, the conduit), drawn from GT's {@code IMTERenderer} hook in each controller.
 * <ul>
 * <li>{@code bts_mvol} (GLSL {@code fx/vol.glsl}): a ray-marched volume in an elliptic cylinder of the machine's voxel
 * frame (the Homogeny's vortex, a Diffraction drum's region clouds and spectral fans, the Diffraction spine's beam and
 * lenses). Only the cylinder's back faces shade; blocks in front hide it through the depth test.</li>
 * <li>{@code bts_beam} ({@code fx/beam.glsl}): a stream along a quadratic Bezier curve, camera-facing: a core, a
 * sheath, a helical strand and travelling knots (feed streams, the spindle stream, the Focus's beam, burst
 * streams).</li>
 * <li>{@code bts_mfx} ({@code fx/mfx.glsl}): a field shell (hexagonal cells, rim-lit), holographic glyph rings,
 * reassembling voxels (sprites placed in the vertex shader from a seed and the clock, no CPU particles) and a
 * Wormhole Mouth's portal disc.</li>
 * </ul>
 * Every draw expects the caller's GL state ({@link Layers#emissive()} and a blend: {@code ONE, ONE_MINUS_SRC_ALPHA}
 * for the volume and the effects, which are premultiplied, {@code ONE, ONE} for beams) and the shared textures bound
 * ({@link NoiseTex#bind()}). Each returns false when its program is off.
 */
@SideOnly(Side.CLIENT)
public final class MachineFx {

    /** Sprites in the voxel mesh. */
    public static final int VOXELS = 160;

    // ------------------------------------------------------------------ the volume

    private static final ShaderRecipe VOL = BTSShaders.recipe("bts_mvol")
        .required("u_Box", "u_Cam", "u_Mode", "u_VA", "u_VB", "u_VC", "u_VD", "u_VE")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform V_BOX = VOL.uniform("u_Box");
    private static final Uniform V_CAM = VOL.uniform("u_Cam");
    private static final Uniform V_MODE = VOL.uniform("u_Mode");
    private static final Uniform V_A = VOL.uniform("u_VA");
    private static final Uniform V_B = VOL.uniform("u_VB");
    private static final Uniform V_C = VOL.uniform("u_VC");
    private static final Uniform V_D = VOL.uniform("u_VD");
    private static final Uniform V_E = VOL.uniform("u_VE");
    public static final BTSShaders.Program VOL_PROGRAM = BTSShaders.program("bts_mvol", VOL);
    private static final BTSShaders.Mesh CYLINDER = VOL_PROGRAM.mesh(h -> cylinder(h, 64));

    // ------------------------------------------------------------------ beams

    private static final ShaderRecipe BEAM = BTSShaders.recipe("bts_beam")
        .required("u_P0", "u_P1", "u_P2", "u_BW", "u_BLook", "u_BColA", "u_BColB", "u_BEnds")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform B_P0 = BEAM.uniform("u_P0");
    private static final Uniform B_P1 = BEAM.uniform("u_P1");
    private static final Uniform B_P2 = BEAM.uniform("u_P2");
    private static final Uniform B_W = BEAM.uniform("u_BW");
    private static final Uniform B_LOOK = BEAM.uniform("u_BLook");
    private static final Uniform B_COLA = BEAM.uniform("u_BColA");
    private static final Uniform B_COLB = BEAM.uniform("u_BColB");
    private static final Uniform B_ENDS = BEAM.uniform("u_BEnds");
    public static final BTSShaders.Program BEAM_PROGRAM = BTSShaders.program("bts_beam", BEAM);
    private static final BTSShaders.Mesh RIBBON = BEAM_PROGRAM.mesh(h -> Meshes.ribbon(h, 64));

    // ------------------------------------------------------------------ effects

    private static final ShaderRecipe MFX = BTSShaders.recipe("bts_mfx")
        .required("u_MMode", "u_MA", "u_MB", "u_MC", "u_Cam")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform M_MODE = MFX.uniform("u_MMode");
    private static final Uniform M_A = MFX.uniform("u_MA");
    private static final Uniform M_B = MFX.uniform("u_MB");
    private static final Uniform M_C = MFX.uniform("u_MC");
    private static final Uniform M_CAM = MFX.uniform("u_Cam");
    public static final BTSShaders.Program MFX_PROGRAM = BTSShaders.program("bts_mfx", MFX);
    private static final BTSShaders.Mesh SPHERE = MFX_PROGRAM.mesh(h -> Meshes.sphere(h, 64, 32));
    private static final BTSShaders.Mesh ANNULUS = MFX_PROGRAM.mesh(h -> Meshes.annulus(h, 128, 4));
    private static final BTSShaders.Mesh SPRITES = MFX_PROGRAM.mesh(MachineFx::sprites);

    private static final Matrix4f IDENTITY = new Matrix4f();

    private MachineFx() {}

    // ================================================================== meshes

    /** The unit cylinder: x, z on the unit circle, y 0..1 (also in UV v); side, floor and roof (caps as fans). */
    private static IVertexArrayObject cylinder(ShaderHandle h, int n) {
        try (MeshBuilder m = MeshBuilder.of(h, n * 3 * 6)) {
            for (int i = 0; i < n; i++) {
                final double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
                final double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                m.vertex(c0, 0, s0, 0, 0);
                m.vertex(c1, 0, s1, 1, 0);
                m.vertex(c1, 1, s1, 1, 1);
                m.vertex(c0, 1, s0, 0, 1);
                for (int y = 0; y <= 1; y++) {
                    m.vertex(0, y, 0, 0.5, y);
                    m.vertex(0, y, 0, 0.5, y);
                    m.vertex(c1, y, s1, 1, y);
                    m.vertex(c0, y, s0, 0, y);
                }
            }
            return m.build();
        }
    }

    /** {@link #VOXELS} sprites: position (index, corner x, corner y), UV the corner 0..1. */
    private static IVertexArrayObject sprites(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, VOXELS * 6)) {
            for (int i = 0; i < VOXELS; i++) {
                m.vertex(i, -1, -1, 0, 0);
                m.vertex(i, 1, -1, 1, 0);
                m.vertex(i, 1, 1, 1, 1);
                m.vertex(i, -1, 1, 0, 1);
            }
            return m.build();
        }
    }

    // ================================================================== draws

    /** A volume's uniforms (mode, box, steps and the five parameter vectors of {@code fx/vol.glsl}). */
    public static final class Volume {

        public float mode, a, b, y0, y1, time;
        public int steps = 16, octaves = 2;
        public final float[] va = new float[4], vb = new float[4], vc = new float[4], vd = new float[4],
            ve = new float[4];
    }

    /** Draws a volume in the frame {@code model} (voxel coordinates), {@code cam} the camera in that frame. */
    public static boolean drawVolume(Matrix4f model, double[] cam, Volume v) {
        final ShaderHandle h = VOL_PROGRAM.get();
        final IVertexArrayObject mesh = CYLINDER.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(model);
        GL20.glUniform4f(h.loc(V_BOX), v.a, v.b, v.y0, v.y1);
        GL20.glUniform3f(h.loc(V_CAM), (float) cam[0], (float) cam[1], (float) cam[2]);
        GL20.glUniform4f(h.loc(V_MODE), v.mode, v.time, v.steps, v.octaves);
        GL20.glUniform4f(h.loc(V_A), v.va[0], v.va[1], v.va[2], v.va[3]);
        GL20.glUniform4f(h.loc(V_B), v.vb[0], v.vb[1], v.vb[2], v.vb[3]);
        GL20.glUniform4f(h.loc(V_C), v.vc[0], v.vc[1], v.vc[2], v.vc[3]);
        GL20.glUniform4f(h.loc(V_D), v.vd[0], v.vd[1], v.vd[2], v.vd[3]);
        GL20.glUniform4f(h.loc(V_E), v.ve[0], v.ve[1], v.ve[2], v.ve[3]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** A beam's look. Colours are RGB; {@code strand} the helical strand's amount, {@code rate} knots per second. */
    public static final class Beam {

        public float w0 = 0.6f, w1 = 0.6f, intensity = 1f, speed = 8f, time;
        public float strand = 0.5f, rate = 1f, fadeIn = 1f, fadeOut = 1f, seed, pitch = 6f;
        public final float[] core = { 0.86f, 0.92f, 1f }, sheath = { 0.42f, 0.33f, 1f };
    }

    /**
     * Draws a beam along the quadratic Bezier curve p0 -> p1 -> p2 (camera-relative), {@code length} its length in
     * blocks.
     */
    public static boolean drawBeam(double[] p0, double[] p1, double[] p2, double length, Beam b) {
        final ShaderHandle h = BEAM_PROGRAM.get();
        final IVertexArrayObject mesh = RIBBON.get();
        if (h == null || mesh == null) return false;
        if (b.intensity <= 0.002f) return true;
        h.use();
        h.uploadModel(IDENTITY.identity());
        GL20.glUniform3f(h.loc(B_P0), (float) p0[0], (float) p0[1], (float) p0[2]);
        GL20.glUniform3f(h.loc(B_P1), (float) p1[0], (float) p1[1], (float) p1[2]);
        GL20.glUniform3f(h.loc(B_P2), (float) p2[0], (float) p2[1], (float) p2[2]);
        GL20.glUniform4f(h.loc(B_W), b.w0, b.w1, (float) (1.0 / Math.max(1e-3, Lod.pixelsPerBlockAtUnit())), 0f);
        GL20.glUniform4f(h.loc(B_LOOK), b.intensity, b.speed, (float) length, b.time);
        GL20.glUniform4f(h.loc(B_COLA), b.core[0], b.core[1], b.core[2], b.strand);
        GL20.glUniform4f(h.loc(B_COLB), b.sheath[0], b.sheath[1], b.sheath[2], b.rate);
        GL20.glUniform4f(h.loc(B_ENDS), b.fadeIn, b.fadeOut, b.seed, b.pitch);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** A straight beam from a to b (camera-relative). */
    public static boolean drawBeam(double[] a, double[] b, Beam look) {
        final double[] mid = { (a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2 };
        final double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        return drawBeam(a, mid, b, Math.sqrt(dx * dx + dy * dy + dz * dz), look);
    }

    /** The length of the quadratic Bezier curve p0 -> p1 -> p2 (16 chords). */
    public static double bezierLength(double[] p0, double[] p1, double[] p2) {
        double len = 0, px = p0[0], py = p0[1], pz = p0[2];
        for (int i = 1; i <= 16; i++) {
            final double s = i / 16.0, r = 1 - s;
            final double x = r * r * p0[0] + 2 * r * s * p1[0] + s * s * p2[0];
            final double y = r * r * p0[1] + 2 * r * s * p1[1] + s * s * p2[1];
            final double z = r * r * p0[2] + 2 * r * s * p1[2] + s * s * p2[2];
            len += Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
            px = x;
            py = y;
            pz = z;
        }
        return len;
    }

    /** A field shell: radius {@code rxz} round, {@code ry} tall, centred at height {@code yc} of the frame. */
    public static boolean drawShell(Matrix4f model, double[] cam, float rxz, float ry, float yc, float time,
        float intensity, float r, float g, float b) {
        return effect(SPHERE, model, cam, 0f, time, rxz, ry, yc, 0f, intensity, 0f, 0f, 0f, r, g, b);
    }

    /**
     * A glyph ring from radius {@code r0} to {@code r1} at height {@code y}: {@code cells} glyphs round, turned by
     * {@code turn} (turns).
     */
    public static boolean drawGlyphRing(Matrix4f model, double[] cam, float r0, float r1, float y, float cells,
        float turn, float time, float intensity, float r, float g, float b) {
        return effect(ANNULUS, model, cam, 1f, time, r0, r1, y, 0f, cells, turn, intensity, 0f, r, g, b);
    }

    /**
     * Reassembling voxels over a tray at height {@code y}, landing between radii {@code rMin} and {@code rMax}:
     * {@code size} blocks, {@code rate} cycles per second, {@code activity} 0..1.
     */
    public static boolean drawVoxels(Matrix4f model, double[] cam, float y, float rMin, float rMax, float size,
        float rate, float activity, float time, float r, float g, float b) {
        return effect(SPRITES, model, cam, 2f, time, y, rMin, rMax, size, rate, activity, 0f, 0f, r, g, b);
    }

    /** A portal disc of {@code radius} blocks in the frame's XZ plane. */
    public static boolean drawPortal(Matrix4f model, double[] cam, float radius, float time, float intensity, float r,
        float g, float b) {
        return effect(ANNULUS, model, cam, 3f, time, 0f, radius, 0f, 0f, intensity, 0f, 0f, 0f, r, g, b);
    }

    private static boolean effect(BTSShaders.Mesh which, Matrix4f model, double[] cam, float mode, float time, float a0,
        float a1, float a2, float a3, float b0, float b1, float b2, float b3, float r, float g, float b) {
        final ShaderHandle h = MFX_PROGRAM.get();
        final IVertexArrayObject mesh = which.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(model);
        GL20.glUniform4f(h.loc(M_MODE), mode, time, 0f, 0f);
        GL20.glUniform4f(h.loc(M_A), a0, a1, a2, a3);
        GL20.glUniform4f(h.loc(M_B), b0, b1, b2, b3);
        GL20.glUniform4f(h.loc(M_C), r, g, b, 1f);
        GL20.glUniform3f(h.loc(M_CAM), (float) cam[0], (float) cam[1], (float) cam[2]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }
}
