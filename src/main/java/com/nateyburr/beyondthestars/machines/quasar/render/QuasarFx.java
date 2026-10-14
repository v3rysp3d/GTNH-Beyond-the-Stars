package com.nateyburr.beyondthestars.machines.quasar.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.Meshes;
import com.nateyburr.beyondthestars.client.render.NoiseTex;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.MeshBuilder;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 (§6.2): the quasar's three programs and their meshes, each drawn with the model matrix
 * {@code translate(centre)} (the TESR's modelview holds only the camera's rotation, so camera-relative = world axes).
 * <ul>
 * <li>{@code bts_qring} (GLSL {@code quasar/ring.glsl}): the accretion ring as a ray-marched volume inside its slab
 * proxy ({@link #RING_MESH}: top and bottom faces, inner and outer walls), plus a quad in front of the camera when the
 * camera stands inside the slab;</li>
 * <li>{@code bts_qjet} ({@code quasar/jet.glsl}): a jet on the shared ribbon, turned about its axis to face the
 * camera;</li>
 * <li>{@code bts_qfx} ({@code quasar/fx.glsl}): the field lines ({@link #FIELD_MESH}, 12 dipole arcs), the debris
 * ({@link #DEBRIS_MESH}, 64 sprites placed in the vertex shader from a seed and the event clock, no CPU particles) and
 * the eruption's shock shell (the shared sphere).</li>
 * </ul>
 * Uniform arrays are filled by {@link QuasarHoleRenderer}; this class only uploads and draws. Every draw expects the
 * caller's GL state (blend, depth, culling) and the shared textures bound ({@link NoiseTex#bind()}).
 */
@SideOnly(Side.CLIENT)
final class QuasarFx {

    /** Field lines and their segments; debris sprites. */
    static final int LINES = 12, LINE_SEGMENTS = 48, DEBRIS = 64;
    /** The ring proxy's segments round. */
    static final int RING_SEGMENTS = 128;

    // ------------------------------------------------------------------ the ring

    private static final ShaderRecipe RING = BTSShaders.recipe("bts_qring")
        .required(
            "u_Centre",
            "u_Ring",
            "u_Gas",
            "u_Look",
            "u_Look2",
            "u_Event",
            "u_Ray",
            "u_NearC",
            "u_NearU",
            "u_NearV")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .sampler("u_Blackbody", NoiseTex.LUT_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform R_CENTRE = RING.uniform("u_Centre");
    private static final Uniform R_RING = RING.uniform("u_Ring");
    private static final Uniform R_GAS = RING.uniform("u_Gas");
    private static final Uniform R_LOOK = RING.uniform("u_Look");
    private static final Uniform R_LOOK2 = RING.uniform("u_Look2");
    private static final Uniform R_EVENT = RING.uniform("u_Event");
    private static final Uniform R_RAY = RING.uniform("u_Ray");
    private static final Uniform R_NEAR_C = RING.uniform("u_NearC");
    private static final Uniform R_NEAR_U = RING.uniform("u_NearU");
    private static final Uniform R_NEAR_V = RING.uniform("u_NearV");
    static final BTSShaders.Program RING_PROGRAM = BTSShaders.program("bts_qring", RING);
    private static final BTSShaders.Mesh RING_MESH = RING_PROGRAM.mesh(QuasarFx::ringProxy);
    private static final BTSShaders.Mesh RING_NEAR = RING_PROGRAM.mesh(Meshes::quad);

    // ------------------------------------------------------------------ the jets

    private static final ShaderRecipe JET = BTSShaders.recipe("bts_qjet")
        .required("u_JLook", "u_JSpan", "u_JEvent", "u_JAxis", "u_Side")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform J_LOOK = JET.uniform("u_JLook");
    private static final Uniform J_SPAN = JET.uniform("u_JSpan");
    private static final Uniform J_EVENT = JET.uniform("u_JEvent");
    private static final Uniform J_AXIS = JET.uniform("u_JAxis");
    private static final Uniform J_SIDE = JET.uniform("u_Side");
    static final BTSShaders.Program JET_PROGRAM = BTSShaders.program("bts_qjet", JET);
    private static final BTSShaders.Mesh JET_MESH = JET_PROGRAM.mesh(h -> Meshes.ribbon(h, 96));

    // ------------------------------------------------------------------ field lines, debris, shell

    private static final ShaderRecipe FX = BTSShaders.recipe("bts_qfx")
        .required("u_Centre", "u_Fx", "u_FxA", "u_FxB", "u_FxC", "u_Right", "u_Up")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform F_CENTRE = FX.uniform("u_Centre");
    private static final Uniform F_FX = FX.uniform("u_Fx");
    private static final Uniform F_A = FX.uniform("u_FxA");
    private static final Uniform F_B = FX.uniform("u_FxB");
    private static final Uniform F_C = FX.uniform("u_FxC");
    private static final Uniform F_RIGHT = FX.uniform("u_Right");
    private static final Uniform F_UP = FX.uniform("u_Up");
    static final BTSShaders.Program FX_PROGRAM = BTSShaders.program("bts_qfx", FX);
    private static final BTSShaders.Mesh FIELD_MESH = FX_PROGRAM.mesh(QuasarFx::fieldLines);
    private static final BTSShaders.Mesh DEBRIS_MESH = FX_PROGRAM.mesh(QuasarFx::debris);
    private static final BTSShaders.Mesh SHELL_MESH = FX_PROGRAM.mesh(h -> Meshes.sphere(h, 64, 32));

    private static final Matrix4f MODEL = new Matrix4f();

    private QuasarFx() {}

    // ================================================================== meshes

    /**
     * The ring's slab proxy: per segment a top face, a bottom face, the inner wall and the outer wall; position
     * (cos, kind, sin), UV (across 0..1, angle 0..1); the vertex shader places them (quasar/ring_vert.glsl).
     */
    private static IVertexArrayObject ringProxy(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, RING_SEGMENTS * 4 * 6)) {
            for (int kind = 0; kind < 4; kind++) {
                for (int s = 0; s < RING_SEGMENTS; s++) {
                    final double u0 = (double) s / RING_SEGMENTS, u1 = (double) (s + 1) / RING_SEGMENTS;
                    final double a0 = 2 * Math.PI * u0, a1 = 2 * Math.PI * u1;
                    final double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                    m.vertex(c0, kind, s0, 0, u0);
                    m.vertex(c1, kind, s1, 0, u1);
                    m.vertex(c1, kind, s1, 1, u1);
                    m.vertex(c0, kind, s0, 1, u0);
                }
            }
            return m.build();
        }
    }

    /** The field lines: per line {@link #LINE_SEGMENTS} quads, position (line, s, side), UV (side 0..1, s). */
    private static IVertexArrayObject fieldLines(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, LINES * LINE_SEGMENTS * 6)) {
            for (int i = 0; i < LINES; i++) {
                for (int k = 0; k < LINE_SEGMENTS; k++) {
                    final double s0 = (double) k / LINE_SEGMENTS, s1 = (double) (k + 1) / LINE_SEGMENTS;
                    m.vertex(i, s0, -1, 0, s0);
                    m.vertex(i, s0, 1, 1, s0);
                    m.vertex(i, s1, 1, 1, s1);
                    m.vertex(i, s1, -1, 0, s1);
                }
            }
            return m.build();
        }
    }

    /** The debris sprites: per sprite one quad, position (index, corner x, corner y), UV the corner 0..1. */
    private static IVertexArrayObject debris(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, DEBRIS * 6)) {
            for (int i = 0; i < DEBRIS; i++) {
                m.vertex(i, -1, -1, 0, 0);
                m.vertex(i, 1, -1, 1, 0);
                m.vertex(i, 1, 1, 1, 1);
                m.vertex(i, -1, 1, 0, 1);
            }
            return m.build();
        }
    }

    // ================================================================== draws

    /** The ring's uniforms (filled by the renderer). */
    static final class RingLook {

        float r0p, r1p, hp, h0;
        float r0, r1, omega, time;
        float tWall, tBody, turbulence, exposure;
        float lock, embers, reform, beta;
        float event, eventAge, eventSize;
        float steps, octaves;
        /** One pixel's width per block of distance. */
        float pixel;
        /** The camera stands inside the slab: also draw the near quad from these (camera-relative) axes. */
        boolean inside;
        final float[] nearC = new float[3], nearU = new float[3], nearV = new float[3];
    }

    /** Draws the ring; false when its program is off. */
    static boolean drawRing(double cx, double cy, double cz, RingLook l) {
        final ShaderHandle h = RING_PROGRAM.get();
        final IVertexArrayObject mesh = RING_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(R_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform4f(h.loc(R_RING), l.r0p, l.r1p, l.hp, l.h0);
        GL20.glUniform4f(h.loc(R_GAS), l.r0, l.r1, l.omega, l.time);
        GL20.glUniform4f(h.loc(R_LOOK), l.tWall, l.tBody, l.turbulence, l.exposure);
        GL20.glUniform4f(h.loc(R_LOOK2), l.lock, l.embers, l.reform, l.beta);
        GL20.glUniform4f(h.loc(R_EVENT), l.event, l.eventAge, l.eventSize, 0f);
        GL20.glUniform3f(h.loc(R_NEAR_C), l.nearC[0], l.nearC[1], l.nearC[2]);
        GL20.glUniform3f(h.loc(R_NEAR_U), l.nearU[0], l.nearU[1], l.nearU[2]);
        GL20.glUniform3f(h.loc(R_NEAR_V), l.nearV[0], l.nearV[1], l.nearV[2]);
        GL20.glUniform4f(h.loc(R_RAY), l.steps, l.octaves, 0f, l.pixel);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        if (l.inside) {
            final IVertexArrayObject near = RING_NEAR.get();
            if (near != null) {
                GL20.glUniform4f(h.loc(R_RAY), l.steps, l.octaves, 1f, l.pixel);
                near.bind();
                near.draw();
                near.unbind();
            }
        }
        ShaderProgram.clear();
        return true;
    }

    /** One jet's uniforms. */
    static final class JetLook {

        float bright, knotRate, pitch, time;
        float z0, zEnd, seed, exposure;
        float event, eventAge, eventSize;
        /** A pixel's width at the jet, blocks. */
        float pixel;
        /** +1 up, -1 down. */
        int dir;
    }

    /** Draws one jet; {@code side} is the unit vector across the ribbon. False when the program is off. */
    static boolean drawJet(double cx, double cy, double cz, JetLook j, float sx, float sz) {
        final ShaderHandle h = JET_PROGRAM.get();
        final IVertexArrayObject mesh = JET_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform4f(h.loc(J_LOOK), j.bright, j.knotRate, j.pitch, j.time);
        GL20.glUniform4f(h.loc(J_SPAN), j.z0, j.zEnd, j.seed, j.exposure);
        GL20.glUniform4f(h.loc(J_EVENT), j.event, j.eventAge, j.eventSize, j.pixel);
        GL20.glUniform4f(h.loc(J_AXIS), 0f, j.dir, 0f, 0f);
        GL20.glUniform3f(h.loc(J_SIDE), sx, 0f, sz);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** Field-line uniforms. */
    static final class FieldLook {

        float size, start, twist, width;
        float intensity, crackle, snap, time;
        float r = 0.45f, g = 0.55f, b = 1.0f;
    }

    /** Draws the field lines; {@code half} +1 only in front of the centre plane, -1 only behind. */
    static boolean drawField(double cx, double cy, double cz, FieldLook f, float half) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = FIELD_MESH.get();
        if (h == null || mesh == null) return false;
        if (f.intensity <= 0.003f) return true;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        fxCommon(h, cx, cy, cz, 0f, f.time, half, f.intensity);
        GL20.glUniform4f(h.loc(F_A), f.size, f.start, f.twist, f.width);
        GL20.glUniform4f(h.loc(F_B), f.crackle, f.snap, 0f, 0f);
        GL20.glUniform4f(h.loc(F_C), f.r, f.g, f.b, 2f);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /**
     * Draws the debris of an event {@code age} seconds old: kind 1 an eruption (sprites thrown off the ring's inner
     * part), 3 a blow-out (chunks from the whole ring). {@code right} / {@code up}: the camera's axes.
     */
    static boolean drawDebris(double cx, double cy, double cz, float kind, float age, float size, float r0, float r1,
        float intensity, float time, double[] right, double[] up) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = DEBRIS_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        fxCommon(h, cx, cy, cz, 1f, time, 0f, intensity);
        GL20.glUniform4f(h.loc(F_A), age, size, r0, r1);
        GL20.glUniform4f(h.loc(F_B), kind, 0f, 0f, 0f);
        GL20.glUniform4f(h.loc(F_C), 1f, 1f, 1f, 1f);
        GL20.glUniform3f(h.loc(F_RIGHT), (float) right[0], (float) right[1], (float) right[2]);
        GL20.glUniform3f(h.loc(F_UP), (float) up[0], (float) up[1], (float) up[2]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** Draws the shock shell of {@code radius} blocks (the caller picks the faces with the cull mode). */
    static boolean drawShell(double cx, double cy, double cz, float radius, float intensity, float time, float r,
        float g, float b, float rimPower) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = SHELL_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        fxCommon(h, cx, cy, cz, 2f, time, 0f, intensity);
        GL20.glUniform4f(h.loc(F_A), radius, 0f, 0f, 0f);
        GL20.glUniform4f(h.loc(F_B), 0f, 0f, 0f, 0f);
        GL20.glUniform4f(h.loc(F_C), r, g, b, rimPower);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    private static void fxCommon(ShaderHandle h, double cx, double cy, double cz, float mode, float time, float half,
        float intensity) {
        GL20.glUniform3f(h.loc(F_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform4f(h.loc(F_FX), mode, time, half, intensity);
        GL20.glUniform3f(h.loc(F_RIGHT), 1f, 0f, 0f);
        GL20.glUniform3f(h.loc(F_UP), 0f, 1f, 0f);
    }
}
