package com.nateyburr.beyondthestars.machines.quasar.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.Billboard;
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
 * SMBH v1 (smbh-v1.md §7, R-19): the Supermassive Lattice's effects round a quasar, one program ({@code bts_smbh},
 * GLSL {@code smbh/fx.glsl}) and three meshes, each one draw:
 * <ul>
 * <li>{@link #drawLattice}: the 48 Containment Nodes' glow sprites in their tier's colour, a field arc from every node
 * over the outer wall into the ring's outer edge, and (Unbound) faint violet anchor threads from node to node;</li>
 * <li>{@link #drawStreams}: the intake, 12 spirals of gas streaming in across the ring while the Growth segment runs;
 * </li>
 * <li>{@link #drawHalo}: Unbound, a darker lensing halo round the hole (heavier per doubling), with a thin violet ring
 * at its rim (premultiplied: it darkens what lies behind it).</li>
 * </ul>
 * The node cells come from the exported shape's rule (the vertex shader places them), so nothing per node is synced.
 * Every draw expects the caller's GL state (blend, depth) and the shared textures bound ({@link NoiseTex#bind()}).
 */
@SideOnly(Side.CLIENT)
final class SmbhFx {

    /** Nodes (16 per ring, 3 rings); segments per field arc, per anchor thread and per intake stream; streams. */
    static final int NODES = 48, ARC_SEGMENTS = 32, THREAD_SEGMENTS = 16, STREAMS = 12, STREAM_SEGMENTS = 48;

    private static final ShaderRecipe RECIPE = BTSShaders.recipe("bts_smbh")
        .required("u_Centre", "u_S", "u_SA", "u_SB", "u_SC", "u_Right", "u_Up")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_CENTRE = RECIPE.uniform("u_Centre");
    private static final Uniform U_S = RECIPE.uniform("u_S");
    private static final Uniform U_SA = RECIPE.uniform("u_SA");
    private static final Uniform U_SB = RECIPE.uniform("u_SB");
    private static final Uniform U_SC = RECIPE.uniform("u_SC");
    private static final Uniform U_RIGHT = RECIPE.uniform("u_Right");
    private static final Uniform U_UP = RECIPE.uniform("u_Up");
    static final BTSShaders.Program PROGRAM = BTSShaders.program("bts_smbh", RECIPE);
    private static final BTSShaders.Mesh LATTICE_MESH = PROGRAM.mesh(SmbhFx::lattice);
    private static final BTSShaders.Mesh STREAM_MESH = PROGRAM.mesh(SmbhFx::streams);
    private static final BTSShaders.Mesh HALO_MESH = PROGRAM.mesh(Meshes::quad);

    private static final Matrix4f MODEL = new Matrix4f();

    private SmbhFx() {}

    // ================================================================== meshes

    /**
     * The lattice mesh: per node a sprite quad (position (0, node, 0), UV the corner), then per node a field arc
     * (position (1, node, side), UV (side 0..1, s)), then per node an anchor thread (kind 2).
     */
    private static IVertexArrayObject lattice(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, NODES * (1 + ARC_SEGMENTS + THREAD_SEGMENTS) * 6)) {
            for (int i = 0; i < NODES; i++) {
                m.vertex(0, i, 0, 0, 0);
                m.vertex(0, i, 0, 1, 0);
                m.vertex(0, i, 0, 1, 1);
                m.vertex(0, i, 0, 0, 1);
            }
            ribbons(m, 1, NODES, ARC_SEGMENTS);
            ribbons(m, 2, NODES, THREAD_SEGMENTS);
            return m.build();
        }
    }

    /** The intake streams: per stream {@link #STREAM_SEGMENTS} quads, kind 3. */
    private static IVertexArrayObject streams(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, STREAMS * STREAM_SEGMENTS * 6)) {
            ribbons(m, 3, STREAMS, STREAM_SEGMENTS);
            return m.build();
        }
    }

    private static void ribbons(MeshBuilder m, int kind, int count, int segments) {
        for (int i = 0; i < count; i++) {
            for (int k = 0; k < segments; k++) {
                final double s0 = (double) k / segments, s1 = (double) (k + 1) / segments;
                m.vertex(kind, i, -1, 0, s0);
                m.vertex(kind, i, 1, 1, s0);
                m.vertex(kind, i, 1, 1, s1);
                m.vertex(kind, i, -1, 0, s1);
            }
        }
    }

    // ================================================================== draws

    /** The lattice's uniforms (filled by the renderer). */
    static final class LatticeLook {

        /** The ring's outer gas edge (blocks), the arcs' and threads' half-widths, a node sprite's radius. */
        float ringOuter, arcWidth, threadWidth, nodeSize;
        /** Overdrive flare 0..1, leak 0..1 (the hold-up short), the threads' intensity. */
        float flare, leak, threads;
        /** One pixel's width per block of distance; the nodes' and the arcs' intensities; the clock. */
        float pixel, intensity, arcIntensity, time;
        /** The tier colour. */
        float r, g, b;
    }

    /** Draws the nodes, the field arcs and the anchor threads (additive); false when the program is off. */
    static boolean drawLattice(double cx, double cy, double cz, LatticeLook l, double[] right, double[] up) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject mesh = LATTICE_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(U_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform4f(h.loc(U_S), 0f, l.time, l.intensity, 0f);
        GL20.glUniform4f(h.loc(U_SA), l.ringOuter, l.arcWidth, l.threadWidth, l.nodeSize);
        GL20.glUniform4f(h.loc(U_SB), l.flare, l.leak, l.threads, l.pixel);
        GL20.glUniform4f(h.loc(U_SC), l.r, l.g, l.b, l.arcIntensity);
        GL20.glUniform3f(h.loc(U_RIGHT), (float) right[0], (float) right[1], (float) right[2]);
        GL20.glUniform3f(h.loc(U_UP), (float) up[0], (float) up[1], (float) up[2]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** The intake streams' uniforms. */
    static final class StreamLook {

        /** Outer and inner radius (blocks), half-width, the turn from outside in (rad), the gas's half-height. */
        float rOut, rIn, width, turn, halfHeight;
        float intensity, pixel, time;
    }

    /** Draws the intake streams (additive); false when the program is off. */
    static boolean drawStreams(double cx, double cy, double cz, StreamLook s) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject mesh = STREAM_MESH.get();
        if (h == null || mesh == null) return false;
        if (s.intensity <= 0.003f) return true;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(U_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform4f(h.loc(U_S), 1f, s.time, s.intensity, 0f);
        GL20.glUniform4f(h.loc(U_SA), s.rOut, s.rIn, s.width, s.turn);
        GL20.glUniform4f(h.loc(U_SB), 0f, s.halfHeight, 0f, s.pixel);
        GL20.glUniform4f(h.loc(U_SC), 1f, 1f, 1f, 1f);
        GL20.glUniform3f(h.loc(U_RIGHT), 1f, 0f, 0f);
        GL20.glUniform3f(h.loc(U_UP), 0f, 1f, 0f);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /**
     * Draws the Unbound halo of {@code radius} blocks round the hole (premultiplied: ONE, ONE_MINUS_SRC_ALPHA):
     * {@code shadowFrac} the shadow's radius over the halo's, {@code dark} 0..1, {@code ring} the rim's brightness,
     * {@code breathe} -1..1. False when the program is off.
     */
    static boolean drawHalo(double cx, double cy, double cz, float radius, float shadowFrac, float dark, float ring,
        float breathe, float time) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject mesh = HALO_MESH.get();
        if (h == null || mesh == null) return false;
        final Billboard bb = Billboard.at(cx, cy, cz);
        if (bb == null || dark <= 0.003f && ring <= 0.003f) return true;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(U_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform4f(h.loc(U_S), 2f, time, 1f, 0f);
        GL20.glUniform4f(h.loc(U_SA), radius, 0f, 0f, 0f);
        GL20.glUniform4f(h.loc(U_SB), shadowFrac, dark, ring, breathe);
        GL20.glUniform4f(h.loc(U_SC), 0.62f, 0.38f, 1f, 1f);
        GL20.glUniform3f(h.loc(U_RIGHT), (float) bb.u[0], (float) bb.u[1], (float) bb.u[2]);
        GL20.glUniform3f(h.loc(U_UP), (float) bb.v[0], (float) bb.v[1], (float) bb.v[2]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }
}
