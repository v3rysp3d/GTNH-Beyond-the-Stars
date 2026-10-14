package com.nateyburr.beyondthestars.client.render;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4f;
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
 * Renders v2 (§3.2 L3, §6.4): the glow sprite, a camera-facing additive glow in an object's colour. It is the far LOD
 * of every object (a quasar still reads from 300 blocks) and the glare of cores, jet bases and caps.
 * <ul>
 * <li>{@link #draw}: the {@code bts_glow} program on the shared quad: a Gaussian core plus a soft halo cut to 0 at the
 * edge, through the tonemap shoulder, so the centre stays coloured;</li>
 * <li>{@link #drawFixed}: the fixed-function disc of today (a linear falloff, {@link FixedShapes#ring}).</li>
 * </ul>
 * Both expect an additive state ({@link Layers#emissive()} / {@link Layers#additive()}).
 */
@SideOnly(Side.CLIENT)
public final class GlowSprite {

    private static final ShaderRecipe RECIPE = BTSShaders.recipe("bts_glow")
        .required("u_Color", "u_Shape")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_COLOR = RECIPE.uniform("u_Color");
    private static final Uniform U_SHAPE = RECIPE.uniform("u_Shape");

    /** The program (a renderer passes it to {@link RenderQuality#tier} when it may draw a glow). */
    public static final BTSShaders.Program PROGRAM = BTSShaders.program("bts_glow", RECIPE);
    private static final BTSShaders.Mesh QUAD = PROGRAM.mesh(Meshes::quad);

    private static final Matrix4f MODEL = new Matrix4f();

    private GlowSprite() {}

    /**
     * A shader glow of {@code radius} blocks round the camera-relative {@code (cx, cy, cz)}: colour {@code rgb} (may
     * exceed 1), {@code intensity}, core and halo widths in sprite radii (e.g. 0.18, 0.45). Returns false (drawing
     * nothing) when the program is off; the caller then draws {@link #drawFixed}.
     */
    public static boolean draw(double cx, double cy, double cz, double radius, float r, float g, float b,
        float intensity, float core, float halo) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject quad = QUAD.get();
        if (h == null || quad == null) return false;
        final Billboard bb = Billboard.at(cx, cy, cz);
        if (bb == null || intensity <= 0.002f) return true;
        h.use();
        h.uploadModel(bb.model(MODEL, cx, cy, cz, radius));
        GL20.glUniform4f(h.loc(U_COLOR), r, g, b, intensity);
        GL20.glUniform2f(h.loc(U_SHAPE), core, halo);
        quad.bind();
        quad.draw();
        quad.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** Today's fixed-function glow disc: {@code alpha} at the centre falling linearly to 0 at {@code radius}. */
    public static void drawFixed(Tessellator t, double cx, double cy, double cz, double radius, float r, float g,
        float b, float alpha) {
        final Billboard bb = Billboard.at(cx, cy, cz);
        if (bb == null) return;
        FixedShapes.ring(t, cx, cy, cz, bb.u, bb.v, 0, radius, r, g, b, alpha);
    }
}
