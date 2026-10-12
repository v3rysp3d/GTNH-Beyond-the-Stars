package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.RenderState;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;
import tectech.rendering.EOH.EOHRenderingUtils;

/**
 * D-49 my answer 3: draws the Eye's centre object ({@link TileEyeStar}) with the pack's own star renderer parts, so
 * it looks like TecTech's Eye of Harmony and the Forge of the Gods:
 * <ul>
 * <li>the Forge of the Gods star shader ({@code tectech:star}: the texture's luminance with our hue, raised to
 * {@code 1/gamma}), baked from the same recipe {@code RenderForgeOfGods} uses;</li>
 * <li>TecTech's three star textures ({@code StarLayer0..2}), an opaque core and two translucent shells turning on
 * three axes, the layer order and blending of {@code RenderForgeOfGods};</li>
 * <li>TecTech's sphere mesh and cull helpers ({@link EOHRenderingUtils#buildSphere},
 * {@link EOHRenderingUtils#beginSphereCull}).</li>
 * </ul>
 * We bake our own handle of the shader because {@code RenderForgeOfGods} keeps its own private and always draws its
 * rings and beam with the star; GT's Eye renderer ({@code EOHTileEntitySR}) has a fixed orange star. The shader and
 * mesh are (re)built lazily on the first frame after GT's resource-reload hook ({@link #markReload()}). If the shader
 * does not compile (an unusual GL profile), nothing is drawn and one line is logged, exactly as the Forge of the Gods
 * then draws nothing.
 * <p>
 * Shells: the Core Collapse bounce sends a shock shell out from the proto-neutron star over 5 s; a supernova is a white
 * flash expanding to three times Arcturus's radius over 3 s. Both are additive spheres timed from when the client saw
 * the object change ({@link TileEyeStar#kindAge}).
 */
@SideOnly(Side.CLIENT)
public class EyeStarRenderer extends TileEntitySpecialRenderer {

    private static final String TT = "tectech";
    private static final ResourceLocation LAYER_0 = new ResourceLocation(TT, "models/StarLayer0.png");
    private static final ResourceLocation LAYER_1 = new ResourceLocation(TT, "models/StarLayer1.png");
    private static final ResourceLocation LAYER_2 = new ResourceLocation(TT, "models/StarLayer2.png");

    private static final ShaderRecipe STAR = ShaderRecipe.of(TT, "star")
        .required("u_Color", "u_Gamma")
        .sampler("u_Texture", 0)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_COLOR = STAR.uniform("u_Color");
    private static final Uniform U_GAMMA = STAR.uniform("u_Gamma");

    private static final int TESSELLATION = 64;
    /** Bounce shock shell and supernova flash lengths, ticks. */
    private static final double BOUNCE_TICKS = 100, NOVA_TICKS = 60;

    private static ShaderHandle shader;
    private static IVertexArrayObject sphere;
    private static boolean dirty = true;
    private static boolean warned;

    private final Matrix4f base = new Matrix4f();
    private final Matrix4f layer = new Matrix4f();

    /** GT's resource-reload hook (ClientProxy): rebuild the shader and mesh on the next frame. */
    public static void markReload() {
        dirty = true;
    }

    private static void release() {
        if (sphere != null) {
            sphere.delete();
            sphere = null;
        }
        if (shader != null) {
            shader.release();
            shader = null;
        }
    }

    private static boolean ready() {
        if (dirty) {
            dirty = false;
            release();
            try {
                shader = STAR.bake();
                if (shader.isValid()) {
                    sphere = EOHRenderingUtils.buildSphere(shader, TESSELLATION, TESSELLATION);
                } else if (!warned) {
                    warned = true;
                    BeyondTheStars.LOG
                        .warn("[BTS] Eye of Harmony centre render: the tectech:star shader did not bake.");
                }
            } catch (RuntimeException e) {
                release();
                if (!warned) {
                    warned = true;
                    BeyondTheStars.LOG.warn("[BTS] Eye of Harmony centre render disabled: {}", e.toString());
                }
            }
        }
        return shader != null && shader.isValid() && sphere != null;
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!Config.eyeRenderCentre || !(tile instanceof TileEyeStar)) return;
        final TileEyeStar star = (TileEyeStar) tile;
        final World world = star.getWorldObj();
        if (world == null || !ready()) return;

        final double now = world.getTotalWorldTime() + partialTicks;
        final byte kind = star.getKind();
        final double age = star.kindAge(now);
        float radius = star.easedRadius(now);
        // wobble: two incommensurate sines, so an unstable star breathes rather than ticks
        final float pulse = star.getPulse();
        if (pulse > 0) {
            radius *= 1f + pulse * (float) (0.6 * Math.sin(now * 0.31) + 0.4 * Math.sin(now * 0.87 + 1.3));
        }
        final int rgb = star.getRgb();
        final float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float gamma = star.getGamma();
        float coreAlpha = 1f;
        if (kind == EyeStarLook.NOVA) {
            final float f = (float) Math.min(1, age / NOVA_TICKS);
            radius *= 1f - 0.8f * f; // the star itself is gone: what is left fades fast
            coreAlpha = 1f - f;
            gamma *= 1f + f;
        }
        final double spin = now * star.getSpin();

        base.translation((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);

        final boolean blendWas = GL11.glGetBoolean(GL11.GL_BLEND);
        final boolean depthTestWas = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        final boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean alphaTestWas = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
        final long blendFuncWas = RenderState.savedBlendFunc();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        shader.use();
        final long cullWas = EOHRenderingUtils.beginSphereCull(false);
        sphere.bind();
        GL20.glUniform1f(shader.loc(U_GAMMA), gamma);

        // 1) the opaque core writes depth (the Forge of the Gods' order)
        if (coreAlpha >= 1f) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            drawLayer(LAYER_0, r, g, b, 1f, radius, 0f, 1f, 1f, 130 + spin);
        }
        // 2) the translucent shells blend over it without writing depth
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        if (coreAlpha < 1f) drawLayer(LAYER_0, r, g, b, coreAlpha, radius, 0f, 1f, 1f, 130 + spin);
        drawLayer(LAYER_1, r, g, b, 0.4f * coreAlpha, radius * 1.02f, 1f, 1f, 0f, -49 + spin * 0.8);
        drawLayer(LAYER_2, r, g, b, 0.2f * coreAlpha, radius * 1.04f, 1f, 0f, 1f, 67 + spin * 1.1);

        // 3) the transition shells, additive
        if (kind == EyeStarLook.BOUNCE && age < BOUNCE_TICKS) {
            final float f = (float) (age / BOUNCE_TICKS);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL20.glUniform1f(shader.loc(U_GAMMA), 4f);
            drawLayer(LAYER_2, 0.8f, 0.9f, 1f, 0.6f * (1f - f), 2.6f + 16f * f, 0f, 1f, 1f, spin * 0.3);
        } else if (kind == EyeStarLook.NOVA && age < NOVA_TICKS) {
            final float f = (float) (age / NOVA_TICKS);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL20.glUniform1f(shader.loc(U_GAMMA), 5f);
            drawLayer(LAYER_1, 1f, 0.95f, 0.85f, 0.9f * (1f - f), 15f + 30f * f, 1f, 1f, 0f, spin * 0.2);
            drawLayer(LAYER_2, 1f, 0.6f, 0.3f, 0.6f * (1f - f), 12f + 24f * f, 0f, 1f, 1f, -spin * 0.3);
        }

        sphere.unbind();
        EOHRenderingUtils.endSphereCull(cullWas);
        ShaderProgram.clear();

        RenderState.restore(GL11.GL_BLEND, blendWas);
        RenderState.restore(GL11.GL_DEPTH_TEST, depthTestWas);
        RenderState.restore(GL11.GL_ALPHA_TEST, alphaTestWas);
        GL11.glDepthMask(depthMaskWas);
        RenderState.restoreBlendFunc(blendFuncWas);
    }

    /** One textured sphere layer (the shader and the mesh are bound by the caller). */
    private void drawLayer(ResourceLocation texture, float r, float g, float b, float a, float scale, float ax,
        float ay, float az, double degrees) {
        if (a <= 0.003f || scale <= 0.01f) return;
        bindTexture(texture);
        final float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        layer.set(base)
            .rotate((float) Math.toRadians(degrees % 360.0), ax / len, ay / len, az / len)
            .scale(scale);
        shader.uploadModel(layer);
        GL20.glUniform4f(shader.loc(U_COLOR), r, g, b, a);
        sphere.draw();
    }
}
