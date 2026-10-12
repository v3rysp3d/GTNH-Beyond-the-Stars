package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
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
 * <p>
 * Render QA round 1 additions: the two translucent shells blend additively (E2: alpha blending let their dark texels
 * draw a grey ring against the day sky); every object gets an additive glow billboard, 2.5 times its radius and at
 * least 6 blocks (E1/E9: the crown band hides the object from the front, and the small objects were pinpricks); the
 * cooling dwarf's shells fade as it cools and a faint dark-red rim keeps the black dwarf readable (E7); the magnetar
 * has a violet flare shell that brightens on an event warning, plus a flare running out to 6 radii every 4-8 s (E8);
 * the neutron star and the magnetar sweep two polar beams (E9).
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
    /** A magnetar flare runs out to {@link #FLARE_REACH} radii over this many ticks. */
    private static final double FLARE_TICKS = 40;
    private static final float FLARE_REACH = 6f;
    /** The glow billboard: radius in object radii, its least radius in blocks, and its alpha at the centre. */
    private static final float GLOW_RADII = 2.5f, GLOW_MIN = 6f, GLOW_ALPHA = 0.30f;
    private static final int GLOW_SEGMENTS = 48;
    /** Polar beams (neutron star, magnetar): length and width in blocks, alpha at the base, tilt off the spin axis. */
    private static final double BEAM_LENGTH = 12, BEAM_HALF_WIDTH = 0.2, BEAM_TILT = Math.toRadians(20);
    private static final float BEAM_ALPHA = 0.5f;

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
        // 2) the translucent shells, additive (E2) and without writing depth; the cooling dwarf's fade as it cools
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        if (coreAlpha < 1f) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawLayer(LAYER_0, r, g, b, coreAlpha, radius, 0f, 1f, 1f, 130 + spin);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        final float aux = star.getAux();
        final float shells = coreAlpha * (kind == EyeStarLook.DWARF ? 1f - aux : 1f);
        drawLayer(LAYER_1, r, g, b, 0.4f * shells, radius * 1.02f, 1f, 1f, 0f, -49 + spin * 0.8);
        drawLayer(LAYER_2, r, g, b, 0.2f * shells, radius * 1.04f, 1f, 0f, 1f, 67 + spin * 1.1);
        if (kind == EyeStarLook.DWARF) {
            // E7: a faint dark-red rim, so the near-black ball still reads against dark casings at night
            GL20.glUniform1f(shader.loc(U_GAMMA), 1f);
            drawLayer(LAYER_1, 0x40 / 255f, 0x18 / 255f, 0x10 / 255f, 0.25f, radius * 1.03f, 0f, 1f, 0f, spin * 0.5);
        } else if (kind == EyeStarLook.MAGNETAR) {
            // E8: the flare shell (brighter on an event warning) and a flare every 4-8 s
            GL20.glUniform1f(shader.loc(U_GAMMA), 2.5f);
            drawLayer(
                LAYER_2,
                0xB0 / 255f,
                0x70 / 255f,
                1f,
                0.15f + 0.45f * aux,
                radius * 1.8f,
                0f,
                1f,
                1f,
                -spin * 0.4);
            final double flare = star.flareAge(now);
            if (flare < FLARE_TICKS) {
                shockShell(0xB0 / 255f, 0x70 / 255f, 1f, 0.4f, radius, radius * FLARE_REACH, flare / FLARE_TICKS, spin);
            }
        }

        // 3) the transition shells, additive
        if (kind == EyeStarLook.BOUNCE && age < BOUNCE_TICKS) {
            shockShell(0.8f, 0.9f, 1f, 0.6f, 2.6f, 18.6f, age / BOUNCE_TICKS, spin);
        } else if (kind == EyeStarLook.NOVA && age < NOVA_TICKS) {
            final float f = (float) (age / NOVA_TICKS);
            GL20.glUniform1f(shader.loc(U_GAMMA), 5f);
            drawLayer(LAYER_1, 1f, 0.95f, 0.85f, 0.9f * (1f - f), 15f + 30f * f, 1f, 1f, 0f, spin * 0.2);
            drawLayer(LAYER_2, 1f, 0.6f, 0.3f, 0.6f * (1f - f), 12f + 24f * f, 0f, 1f, 1f, -spin * 0.3);
        }

        sphere.unbind();
        EOHRenderingUtils.endSphereCull(cullWas);
        ShaderProgram.clear();

        // 4) the glow and the polar beams: untextured, additive, depth-tested, no depth writes (E1, E9)
        final boolean beams = kind == EyeStarLook.NEUTRON || kind == EyeStarLook.MAGNETAR;
        glowAndBeams(x + 0.5, y + 0.5, z + 0.5, radius, r, g, b, GLOW_ALPHA * coreAlpha, beams, spin);

        RenderState.restore(GL11.GL_BLEND, blendWas);
        RenderState.restore(GL11.GL_DEPTH_TEST, depthTestWas);
        RenderState.restore(GL11.GL_ALPHA_TEST, alphaTestWas);
        GL11.glDepthMask(depthMaskWas);
        RenderState.restoreBlendFunc(blendFuncWas);
    }

    /**
     * An additive shock shell (the bounce's, a magnetar flare): {@code f} (0..1) of the way out from radius
     * {@code from} to {@code to}, fading from {@code alpha}. The blend is already additive.
     */
    private void shockShell(float r, float g, float b, float alpha, float from, float to, double f, double spin) {
        final float t = (float) Math.max(0, Math.min(1, f));
        GL20.glUniform1f(shader.loc(U_GAMMA), 4f);
        drawLayer(LAYER_2, r, g, b, alpha * (1f - t), from + (to - from) * t, 0f, 1f, 1f, spin * 0.3);
    }

    /**
     * The glow billboard and (if {@code beams}) the two polar beams, in the TESR's camera-relative coordinates
     * (the camera is at the origin), around the object centre {@code (cx, cy, cz)}.
     * <p>
     * The glow is a disc facing the camera: the object's colour at {@code alpha} in the middle, falling linearly to 0
     * at {@link #GLOW_RADII} object radii (at least {@link #GLOW_MIN} blocks). It stands just behind the object (and is
     * scaled up to keep its apparent size), so the object's depth hides its middle and the face of the star stays
     * clean; the bands in front of it hide it, and it shows through their gaps.
     */
    private static void glowAndBeams(double cx, double cy, double cz, float radius, float r, float g, float b,
        float alpha, boolean beams, double spinDeg) {
        final double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (dist <= radius + 0.5 || alpha <= 0.003f) return;
        final double dx = cx / dist, dy = cy / dist, dz = cz / dist;
        // u, v: the billboard's axes, perpendicular to the view direction
        final double upX = Math.abs(dy) > 0.99 ? 1 : 0, upY = Math.abs(dy) > 0.99 ? 0 : 1;
        double ux = dy * 0 - dz * upY, uy = dz * upX - dx * 0, uz = dx * upY - dy * upX;
        final double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        final double vx = uy * dz - uz * dy, vy = uz * dx - ux * dz, vz = ux * dy - uy * dx;

        final boolean textureWas = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
        final boolean lightingWas = GL11.glGetBoolean(GL11.GL_LIGHTING);
        final boolean cullWas = GL11.glGetBoolean(GL11.GL_CULL_FACE);
        final boolean fogWas = GL11.glGetBoolean(GL11.GL_FOG);
        final int shadeWas = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        final boolean lightmapWas = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        final Tessellator t = Tessellator.instance;
        // the glow: pushed back behind the object, scaled to keep its size as seen from the camera
        final double back = radius * 1.05 + 0.5;
        final double grow = (dist + back) / dist;
        final double R = Math.max(GLOW_MIN, GLOW_RADII * radius) * grow;
        final double gx = cx + dx * back, gy = cy + dy * back, gz = cz + dz * back;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertex(gx, gy, gz);
        t.setColorRGBA_F(r, g, b, 0f);
        for (int i = 0; i <= GLOW_SEGMENTS; i++) {
            final double a = 2 * Math.PI * i / GLOW_SEGMENTS;
            final double c = Math.cos(a) * R, s = Math.sin(a) * R;
            t.addVertex(gx + ux * c + vx * s, gy + uy * c + vy * s, gz + uz * c + vz * s);
        }
        t.draw();

        if (beams) {
            // the beam axis leans BEAM_TILT off the vertical spin axis and sweeps round it with the object's spin
            final double turn = Math.toRadians(spinDeg % 360.0);
            final double ax = Math.sin(BEAM_TILT) * Math.cos(turn), ay = Math.cos(BEAM_TILT),
                az = Math.sin(BEAM_TILT) * Math.sin(turn);
            final float br = 0.5f + 0.5f * r, bg = 0.5f + 0.5f * g, bb = 0.5f + 0.5f * b;
            t.startDrawingQuads();
            for (int sign = -1; sign <= 1; sign += 2) {
                final double x0 = cx + sign * ax * radius * 0.9, y0 = cy + sign * ay * radius * 0.9,
                    z0 = cz + sign * az * radius * 0.9;
                final double x1 = cx + sign * ax * (radius + BEAM_LENGTH), y1 = cy + sign * ay * (radius + BEAM_LENGTH),
                    z1 = cz + sign * az * (radius + BEAM_LENGTH);
                // width across the beam and the line of sight
                double wx = ay * z0 - az * y0, wy = az * x0 - ax * z0, wz = ax * y0 - ay * x0;
                final double wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
                if (wl < 1e-6) continue;
                wx *= BEAM_HALF_WIDTH / wl;
                wy *= BEAM_HALF_WIDTH / wl;
                wz *= BEAM_HALF_WIDTH / wl;
                t.setColorRGBA_F(br, bg, bb, BEAM_ALPHA * alpha / GLOW_ALPHA);
                t.addVertex(x0 - wx, y0 - wy, z0 - wz);
                t.addVertex(x0 + wx, y0 + wy, z0 + wz);
                t.setColorRGBA_F(br, bg, bb, 0f);
                t.addVertex(x1 + wx, y1 + wy, z1 + wz);
                t.addVertex(x1 - wx, y1 - wy, z1 - wz);
            }
            t.draw();
        }

        GL11.glShadeModel(shadeWas);
        RenderState.restore(GL11.GL_FOG, fogWas);
        RenderState.restore(GL11.GL_CULL_FACE, cullWas);
        RenderState.restore(GL11.GL_LIGHTING, lightingWas);
        RenderState.restore(GL11.GL_TEXTURE_2D, textureWas);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        RenderState.restore(GL11.GL_TEXTURE_2D, lightmapWas);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
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
