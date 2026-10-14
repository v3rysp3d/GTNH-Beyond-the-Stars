package com.nateyburr.beyondthestars.client.render;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.MeshBuilder;
import gregtech.common.render.shader.ShaderHandle;

/**
 * Renders v2 (§2.1): the shared meshes, each built once into a vertex buffer against a program's vertex format
 * (position + UV) through GT's {@link MeshBuilder}; per frame only the model matrix and the uniforms change. Use them
 * through {@link BTSShaders.Program#mesh} so they are rebuilt after a resource reload.
 * <ul>
 * <li>{@link #quad}: the unit quad, XY from -1 to 1, UV 0..1 (impostors, glow sprites, a galaxy's disc);</li>
 * <li>{@link #sphere}: a UV sphere of radius 1, UV = (longitude, latitude) 0..1 (stars, shells);</li>
 * <li>{@link #annulus}: the unit circle in the XZ plane split into rings, position = (cos, 0, sin) of the angle, UV =
 * (radial 0..1, angle 0..1): the vertex shader places each vertex at {@code mix(r0, r1, uv.x)} (accretion rings);</li>
 * <li>{@link #ribbon}: a strip along +Y from 0 to 1, X from -1 to 1, UV 0..1 (jets, beams, field lines).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class Meshes {

    private Meshes() {}

    /** The unit quad (two triangles). */
    public static IVertexArrayObject quad(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, 6)) {
            m.vertex(-1, -1, 0, 0, 0);
            m.vertex(1, -1, 0, 1, 0);
            m.vertex(1, 1, 0, 1, 1);
            m.vertex(-1, 1, 0, 0, 1);
            return m.build();
        }
    }

    /** A UV sphere of radius 1 with {@code slices} x {@code stacks} quads, wound counter-clockwise from outside. */
    public static IVertexArrayObject sphere(ShaderHandle h, int slices, int stacks) {
        try (MeshBuilder m = MeshBuilder.of(h, slices * stacks * 6)) {
            for (int i = 0; i < stacks; i++) {
                final double v0 = (double) i / stacks, v1 = (double) (i + 1) / stacks;
                final double p0 = Math.PI * v0 - Math.PI / 2, p1 = Math.PI * v1 - Math.PI / 2;
                final double y0 = Math.sin(p0), y1 = Math.sin(p1), r0 = Math.cos(p0), r1 = Math.cos(p1);
                for (int j = 0; j < slices; j++) {
                    final double u0 = (double) j / slices, u1 = (double) (j + 1) / slices;
                    final double a0 = 2 * Math.PI * u0, a1 = 2 * Math.PI * u1;
                    final double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                    m.vertex(r0 * c0, y0, r0 * s0, u0, v0);
                    m.vertex(r1 * c0, y1, r1 * s0, u0, v1);
                    m.vertex(r1 * c1, y1, r1 * s1, u1, v1);
                    m.vertex(r0 * c1, y0, r0 * s1, u1, v0);
                }
            }
            return m.build();
        }
    }

    /** The unit annulus: {@code segments} round, {@code rings} across (see the class comment). */
    public static IVertexArrayObject annulus(ShaderHandle h, int segments, int rings) {
        try (MeshBuilder m = MeshBuilder.of(h, segments * rings * 6)) {
            for (int r = 0; r < rings; r++) {
                final double t0 = (double) r / rings, t1 = (double) (r + 1) / rings;
                for (int s = 0; s < segments; s++) {
                    final double u0 = (double) s / segments, u1 = (double) (s + 1) / segments;
                    final double a0 = 2 * Math.PI * u0, a1 = 2 * Math.PI * u1;
                    final double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                    m.vertex(c0, 0, s0, t0, u0);
                    m.vertex(c1, 0, s1, t0, u1);
                    m.vertex(c1, 0, s1, t1, u1);
                    m.vertex(c0, 0, s0, t1, u0);
                }
            }
            return m.build();
        }
    }

    /** A ribbon of {@code segments} quads along +Y (0..1), X -1..1. */
    public static IVertexArrayObject ribbon(ShaderHandle h, int segments) {
        try (MeshBuilder m = MeshBuilder.of(h, segments * 6)) {
            for (int i = 0; i < segments; i++) {
                final double y0 = (double) i / segments, y1 = (double) (i + 1) / segments;
                m.vertex(-1, y0, 0, 0, y0);
                m.vertex(1, y0, 0, 1, y0);
                m.vertex(1, y1, 0, 1, y1);
                m.vertex(-1, y1, 0, 0, y1);
            }
            return m.build();
        }
    }
}
