package com.nateyburr.beyondthestars.machines.eye.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

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
 * <b>Two render passes</b> (render QA round 4, D1). Pass 0 draws the object itself: the opaque core, the two thin
 * additive surface shells (1.01 / 1.02 radii), the cooling dwarf's dark-red rim, a limb darkening (colour x
 * {@code 0.75 + 0.25 |N.V|}, D6) and the black dwarf's veil, all glued to the core's surface. Pass 1, after the
 * translucent and glowing block faces, draws every see-through layer around it: the corona ({@code 1.12 R}, D6), the
 * glow disc, the pulsar beams (D7), the magnetar's magnetosphere and flare shells (D3), the bounce's shock shell (D2)
 * and the supernova's flash. Those layers blend additively, are depth-tested, write no depth, and draw their back faces
 * before their front faces. In round 3 they were drawn in pass 0, so the bands' translucent and glow faces behind them,
 * drawn later, showed through them untinted.
 * <p>
 * <b>Rim shells</b> (D2, D3): the bounce's shock front and the magnetar's shells are untextured spheres
 * ({@value #MESH_SLICES} x {@value #MESH_STACKS}) whose alpha rises toward the limb,
 * {@code centre + (edge - centre) (1 - |N.V|)^3}, computed per vertex: clear in the middle, a bright edge. The cloud
 * texture of round 3 read as fog.
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

    /** Segments of every sphere, textured or not (render QA round 4: at most 32 x 16 per layer). */
    private static final int MESH_SLICES = 32, MESH_STACKS = 16;
    /** Bounce shock shell and supernova flash lengths, ticks. */
    private static final double BOUNCE_TICKS = 100, NOVA_TICKS = 60;
    /** A magnetar flare runs from the magnetosphere out to the outer shell over this many ticks. */
    private static final double FLARE_TICKS = 40;
    /** The translucent surface shells' gamma ceiling (they are additive, so a high gamma whitens the star's face). */
    private static final float SHELL_GAMMA = 1.2f;
    /**
     * How far from the centre a see-through layer may reach: the nearest {@code eye_core} block is 20.9 blocks out
     * (centre to centre), its face 20.4. A layer inside this sphere can never be cut by a block of the Eye (round 3,
     * E-5), so the glow disc, the corona and the beams all stop here.
     */
    static final float CLEARANCE = 20.4f;
    /**
     * The glow billboard: radius in object radii, its least radius in blocks, and its alpha at the centre; at most
     * {@link #CLEARANCE}.
     */
    private static final float GLOW_RADII = 2.5f, GLOW_MIN = 6f, GLOW_ALPHA = 0.30f;
    /** The additive surface shells' alphas (inner, outer): the bright kinds' and the others' (round 3, E-1..E-4). */
    private static final float SHELL_A_BRIGHT = 0.12f, SHELL_B_BRIGHT = 0.06f, SHELL_A = 0.3f, SHELL_B = 0.15f;
    /** The cooling dwarf's dark veil (round 3, E-7): colour, radius in object radii, most alpha. */
    private static final float VEIL_R = 0x0E / 255f, VEIL_G = 0x0A / 255f, VEIL_B = 0x0C / 255f, VEIL_RADII = 1.003f,
        VEIL_ALPHA = 0.85f;
    /** Limb darkening (round 4, D6): the core's colour x {@code 1 - LIMB (1 - |N.V|)}, i.e. 0.75 at the limb. */
    private static final float LIMB = 0.25f;
    /** The corona (round 4, D6): out to {@code CORONA_RADII} x the limb, {@code CORONA_ALPHA} at the limb. */
    private static final float CORONA_RADII = 1.12f, CORONA_ALPHA = 0.30f;
    /** The neutron star's wider glow (round 4, D7), so the 2.2-block ball reads from 136 blocks. */
    private static final float NEUTRON_GLOW_RADII = 1.6f, NEUTRON_GLOW_ALPHA = 0.45f;
    private static final int RING_SEGMENTS = 48;
    /** Rim shells (round 4, D2 / D3): alpha in the middle of the disc. */
    private static final float RIM_CENTRE = 0.04f;
    /** The bounce's shock front (D2): edge alpha, start and end radius. */
    private static final float BOUNCE_EDGE = 0.64f, BOUNCE_FROM = 2.6f, BOUNCE_TO = 18.6f;
    /**
     * The magnetar (D3): the magnetosphere (inner shell, C080FF) and the outer shell (7A4CFF), radii in blocks (2.7 x
     * the 2.6 core at most), edge alphas at full, and the quiet magnetosphere's edge when no warning is up.
     */
    private static final float MAG_INNER_RADIUS = 4.5f, MAG_OUTER_RADIUS = 7f, MAG_INNER_EDGE = 0.6f,
        MAG_OUTER_EDGE = 0.5f, MAG_QUIET_EDGE = 0.3f;
    private static final float MAG_INNER_R = 0xC0 / 255f, MAG_INNER_G = 0x80 / 255f, MAG_INNER_B = 1f;
    private static final float MAG_OUTER_R = 0x7A / 255f, MAG_OUTER_G = 0x4C / 255f, MAG_OUTER_B = 1f;
    /** An event warning pulses both magnetar shells' edges between these, at 2 Hz (radians per tick). */
    private static final float WARN_LOW = 0.35f, WARN_HIGH = 0.6f;
    private static final double WARN_RATE = 2 * Math.PI * 2 / 20;
    /** The Core Collapse swell throbs at 0.7 Hz (radians per tick, round 4 D4). */
    private static final double SWELL_RATE = 2 * Math.PI * 0.7 / 20;
    /**
     * Polar beams (neutron star, magnetar, D7): half-width in blocks, additive alpha at the root (0 at the tip), tilt
     * off the spin axis; the tip stands one block inside {@link #CLEARANCE}.
     */
    private static final double BEAM_HALF_WIDTH = 0.45, BEAM_TILT = Math.toRadians(20), BEAM_TIP = CLEARANCE - 1;
    private static final float BEAM_ALPHA = 0.6f;

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
                    sphere = EOHRenderingUtils.buildSphere(shader, MESH_SLICES, MESH_STACKS);
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
        // 0: the object; 1: the see-through layers round it; -1 (drawn outside the world passes): both
        final int pass = MinecraftForgeClient.getRenderPass();

        final double now = world.getTotalWorldTime() + partialTicks;
        final byte kind = star.getKind();
        final double age = star.kindAge(now);
        float radius = star.easedRadius(now);
        float gamma = star.getGamma();
        final float pulse = star.getPulse();
        if (pulse > 0 && kind == EyeStarLook.SWELL) {
            // D4: a 0.7 Hz throb; the radius only shrinks (by up to a third of the pulse) so the swell's peak never
            // reaches a block, and the brightness swings by the full pulse
            final double s = Math.sin(now * SWELL_RATE);
            radius *= 1f - pulse / 3f * (float) (0.5 + 0.5 * s);
            gamma *= 1f + pulse * (float) s;
        } else if (pulse > 0) {
            // wobble: two incommensurate sines, so an unstable star breathes rather than ticks
            radius *= 1f + pulse * (float) (0.6 * Math.sin(now * 0.31) + 0.4 * Math.sin(now * 0.87 + 1.3));
        }
        final int rgb = star.getRgb();
        final float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float coreAlpha = 1f;
        if (kind == EyeStarLook.NOVA) {
            final float f = (float) Math.min(1, age / NOVA_TICKS);
            radius *= 1f - 0.8f * f; // the star itself is gone: what is left fades fast
            coreAlpha = 1f - f;
            gamma *= 1f + f;
        }
        final double spin = now * star.getSpin();
        final double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;

        base.translation((float) cx, (float) cy, (float) cz);

        final boolean blendWas = GL11.glGetBoolean(GL11.GL_BLEND);
        final boolean depthTestWas = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        final boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean alphaTestWas = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
        final long blendFuncWas = RenderState.savedBlendFunc();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        if (pass != 1) drawObject(star, kind, cx, cy, cz, radius, r, g, b, gamma, coreAlpha, spin);
        if (pass != 0) drawSurroundings(star, kind, now, age, cx, cy, cz, radius, r, g, b, coreAlpha, spin);

        RenderState.restore(GL11.GL_BLEND, blendWas);
        RenderState.restore(GL11.GL_DEPTH_TEST, depthTestWas);
        RenderState.restore(GL11.GL_ALPHA_TEST, alphaTestWas);
        GL11.glDepthMask(depthMaskWas);
        RenderState.restoreBlendFunc(blendFuncWas);
    }

    // ================================================================================ pass 0: the object

    /**
     * Pass 0: the core (opaque, writing depth; alpha-blended while a supernova's remnant fades), the two additive
     * surface shells, the cooling dwarf's dark-red rim, then (untextured) the limb darkening and the black dwarf's
     * veil.
     */
    private void drawObject(TileEyeStar star, byte kind, double cx, double cy, double cz, float radius, float r,
        float g, float b, float gamma, float coreAlpha, double spin) {
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
        // 2) the surface shells, additive and without writing depth; the cooling dwarf's fade as it cools
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        if (coreAlpha < 1f) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawLayer(LAYER_0, r, g, b, coreAlpha, radius, 0f, 1f, 1f, 130 + spin);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        final float aux = star.getAux();
        final float shells = coreAlpha * (kind == EyeStarLook.DWARF ? 1f - aux : 1f);
        // additive shells with a gamma of at most 1.2 (round 2); the bright kinds at 0.12 / 0.06 and every kind at
        // 1.01 / 1.02 radii (round 3, E-1..E-4, E-8)
        final boolean bright = brightShells(kind);
        final float shellA = (bright ? SHELL_A_BRIGHT : SHELL_A) * shells;
        final float shellB = (bright ? SHELL_B_BRIGHT : SHELL_B) * shells;
        GL20.glUniform1f(shader.loc(U_GAMMA), Math.min(gamma, SHELL_GAMMA));
        drawLayer(LAYER_1, r, g, b, shellA, radius * 1.01f, 1f, 1f, 0f, -49 + spin * 0.8);
        drawLayer(LAYER_2, r, g, b, shellB, radius * 1.02f, 1f, 0f, 1f, 67 + spin * 1.1);
        if (kind == EyeStarLook.DWARF) {
            // E7: a faint dark-red rim, so the near-black ball still reads against dark casings at night. Round 4
            // (D5): it fades in with the cooling (0 up to half way, full at 90 %), as the veil does; at full from the
            // start it added red over the early white dwarf's whole face (measured F6F1F4, red above blue)
            GL20.glUniform1f(shader.loc(U_GAMMA), 0.6f);
            drawLayer(
                LAYER_1,
                0x40 / 255f,
                0x18 / 255f,
                0x10 / 255f,
                0.25f * cooled(aux),
                radius * 1.03f,
                0f,
                1f,
                0f,
                spin * 0.5);
        }

        sphere.unbind();
        EOHRenderingUtils.endSphereCull(cullWas);
        ShaderProgram.clear();

        // 3) untextured, alpha-blended, front faces only: the limb darkening (D6) and the black dwarf's veil (E-7)
        final float veil = kind == EyeStarLook.DWARF ? VEIL_ALPHA * cooled(aux) : 0f;
        if (coreAlpha < 1f && veil <= 0.003f) return;
        final Flat flat = Flat.begin();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glCullFace(GL11.GL_BACK);
        final Tessellator t = Tessellator.instance;
        if (coreAlpha >= 1f) {
            sphereMesh(t, cx, cy, cz, outside(radius), 0f, 0f, 0f, 0f, LIMB, false);
        }
        if (veil > 0.003f) {
            sphereMesh(t, cx, cy, cz, outside(radius * VEIL_RADII), VEIL_R, VEIL_G, VEIL_B, veil, 0f, false);
        }
        flat.end();
    }

    /** How far the cooling dwarf has gone dark: 0 up to half its batch, 1 from 90 % (the veil and the rim). */
    private static float cooled(float aux) {
        return Math.max(0f, Math.min(1f, (aux - 0.5f) / 0.4f));
    }

    /**
     * The kinds whose additive surface shells run at {@link #SHELL_A_BRIGHT} / {@link #SHELL_B_BRIGHT}: the lit T1-T3
     * stars, the Core Collapse swell and bounce and the neutron star (render QA round 3, E-1..E-4). The protostar,
     * the magnetar, the nova and (round 4, D5: the early white dwarf measured D9DBE2 at 0.12 / 0.06, under the 235 per
     * channel it needs) the cooling dwarf keep 0.3 / 0.15; the dwarf's fade out as it cools.
     */
    private static boolean brightShells(byte kind) {
        switch (kind) {
            case EyeStarLook.STAR:
            case EyeStarLook.SWELL:
            case EyeStarLook.NEUTRON:
            case EyeStarLook.BOUNCE:
                return true;
            default:
                return false;
        }
    }

    /**
     * The vertex radius of a {@link #MESH_SLICES} x {@link #MESH_STACKS} sphere whose flat faces all lie at least
     * {@code minRad} from the centre: 1 / (cos(half a slice) x cos(half a stack)) further out. With the vertices AT
     * the core's radius the faces sank up to 1 % inside it, i.e. inside the core's depth, and the depth test hid them
     * (round 3, the veil).
     */
    private static double outside(double minRad) {
        return minRad / (Math.cos(Math.PI / MESH_SLICES) * Math.cos(Math.PI / (2 * MESH_STACKS)));
    }

    // ===================================================================== pass 1: the see-through layers

    /**
     * Pass 1, all additive, depth-tested, no depth writes: the supernova's textured flash, the magnetar's shells, the
     * bounce's shock front, the corona, the glow disc and the polar beams.
     */
    private void drawSurroundings(TileEyeStar star, byte kind, double now, double age, double cx, double cy, double cz,
        float radius, float r, float g, float b, float coreAlpha, double spin) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        if (kind == EyeStarLook.NOVA && age < NOVA_TICKS) {
            // the supernova's flash: TecTech's textures, expanding past the bands
            shader.use();
            final long cullWas = EOHRenderingUtils.beginSphereCull(false);
            sphere.bind();
            final float f = (float) (age / NOVA_TICKS);
            GL20.glUniform1f(shader.loc(U_GAMMA), 5f);
            drawLayer(LAYER_1, 1f, 0.95f, 0.85f, 0.9f * (1f - f), 15f + 30f * f, 1f, 1f, 0f, spin * 0.2);
            drawLayer(LAYER_2, 1f, 0.6f, 0.3f, 0.6f * (1f - f), 12f + 24f * f, 0f, 1f, 1f, -spin * 0.3);
            sphere.unbind();
            EOHRenderingUtils.endSphereCull(cullWas);
            ShaderProgram.clear();
        }

        final double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        final Flat flat = Flat.begin();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        final Tessellator t = Tessellator.instance;

        // the shells: back faces first, then front faces
        if (kind == EyeStarLook.MAGNETAR) {
            magnetarShells(t, star, now, cx, cy, cz);
        } else if (kind == EyeStarLook.BOUNCE && age < BOUNCE_TICKS) {
            // D2: the shock front, rim-weighted and fading as it runs out (about 7 blocks at 30 ticks)
            final float f = (float) (age / BOUNCE_TICKS);
            final float fade = 1f - f;
            rimShell(
                t,
                cx,
                cy,
                cz,
                BOUNCE_FROM + (BOUNCE_TO - BOUNCE_FROM) * f,
                r,
                g,
                b,
                RIM_CENTRE * fade,
                BOUNCE_EDGE * fade);
        }

        // billboards: the corona, the glow, the beams
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (dist > radius + 0.5) {
            final double dx = cx / dist, dy = cy / dist, dz = cz / dist;
            // u, v: the billboards' axes, perpendicular to the view direction
            final double upX = Math.abs(dy) > 0.99 ? 1 : 0, upY = Math.abs(dy) > 0.99 ? 0 : 1;
            double ux = -dz * upY, uy = dz * upX, uz = dx * upY - dy * upX;
            final double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= ul;
            uy /= ul;
            uz /= ul;
            final double vx = uy * dz - uz * dy, vy = uz * dx - ux * dz, vz = ux * dy - uy * dx;
            final double[] u = { ux, uy, uz }, v = { vx, vy, vz };

            // the corona (D6) and the neutron star's wider glow (D7): a ring from the limb outward, in the plane of
            // the limb, so the star hides none of it and it hides none of the star
            final float corona = coronaAlpha(kind, star.getAux()) * coreAlpha;
            if (corona > 0.003f) {
                final double limbBack = radius * radius / dist; // the limb's plane, this far toward the camera
                final double limb = radius * Math.sqrt(1 - (radius / dist) * (radius / dist));
                final double reach = Math.sqrt(Math.max(0, CLEARANCE * CLEARANCE - limbBack * limbBack));
                final double outer = Math.min(reach, limb * coronaRadii(kind));
                if (outer > limb + 0.05) {
                    ring(
                        t,
                        cx - dx * limbBack,
                        cy - dy * limbBack,
                        cz - dz * limbBack,
                        u,
                        v,
                        limb,
                        outer,
                        r,
                        g,
                        b,
                        corona);
                }
            }

            // the glow: a disc through the object's centre that never reaches a block (round 3, E-5)
            final double glow = Math.min(CLEARANCE, Math.max(GLOW_MIN, GLOW_RADII * radius));
            if (coreAlpha > 0.003f) ring(t, cx, cy, cz, u, v, 0, glow, r, g, b, GLOW_ALPHA * coreAlpha);

            if ((kind == EyeStarLook.NEUTRON || kind == EyeStarLook.MAGNETAR) && coreAlpha > 0.003f) {
                beams(t, cx, cy, cz, radius, r, g, b, BEAM_ALPHA * coreAlpha, spin);
            }
            // P4: the galaxies' arms and planets mode's orbits (billboard dots, additive)
            if (kind == EyeStarLook.TRIANGULUM || kind == EyeStarLook.MILKY_WAY) {
                galaxyArms(t, kind, now, cx, cy, cz, u, v, star.getAux());
            } else if (kind == EyeStarLook.PLANETS) {
                planetOrbits(t, now, cx, cy, cz, u, v, star.getAux());
            }
        }
        flat.end();
    }

    // ======================================================================== P4: galaxies

    /** The galaxy disc's tilt (the stability rail's 8°) and its turn rate, radians per tick (one turn in ~2.6 min). */
    private static final double DISC_TILT = Math.toRadians(8), DISC_RATE = 2 * Math.PI / (20 * 160);
    private static final double SIN_TILT = Math.sin(DISC_TILT), COS_TILT = Math.cos(DISC_TILT);

    /** A deterministic 0..1 per index and salt (the dots' scatter must not flicker). */
    private static double hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /**
     * The arms of a galaxy: logarithmic-ish spirals of camera-facing dots in the tilted disc plane, turning slowly.
     * Triangulum: three loose arms of blue-white dots with a few pink H II knots; the Milky Way: a central bar and four
     * arms, a warm inner glow. A warning ({@code aux}) brightens them. Every dot stays inside the arms' reach
     * ({@link EyeStarLook#galaxyReach}), clear of the core band.
     */
    private static void galaxyArms(Tessellator t, byte kind, double now, double cx, double cy, double cz, double[] u,
        double[] v, float aux) {
        final boolean mw = kind == EyeStarLook.MILKY_WAY;
        final int arms = mw ? 4 : 3, perArm = mw ? 56 : 44;
        final double reach = EyeStarLook.galaxyReach(kind), inner = mw ? 4.5 : 3;
        final double turn = now * DISC_RATE;
        final float bright = 1f + 0.6f * aux;
        // a broad faint disc glow first
        ring(t, cx, cy, cz, u, v, 0, reach * 0.85, mw ? 1f : 0.7f, mw ? 0.85f : 0.8f, mw ? 0.6f : 1f, 0.10f * bright);
        t.startDrawingQuads();
        for (int a = 0; a < arms; a++) {
            final double off = 2 * Math.PI * a / arms;
            for (int i = 0; i < perArm; i++) {
                final int id = a * 1000 + i;
                final double f = (i + hash(id, 1)) / perArm;
                final double rad = inner + (reach - inner) * f;
                final double ang = off + turn + (mw ? 3.4 : 2.6) * Math.log(rad / inner) + 0.35 * (hash(id, 2) - 0.5);
                final double jitter = 0.9 * (hash(id, 3) - 0.5);
                final boolean knot = !mw && hash(id, 4) < 0.08;
                final float cr = knot ? 1f : mw ? 0.75f : 0.62f, cg = knot ? 0.5f : mw ? 0.82f : 0.74f,
                    cb = knot ? 0.75f : 1f;
                final float alpha = (float) Math.min(1, (0.55 - 0.35 * f) * bright);
                dot(t, cx, cy, cz, rad + jitter, ang, u, v, (0.55 - 0.25 * f) * (knot ? 1.4 : 1), cr, cg, cb, alpha);
            }
        }
        if (mw) {
            // the bar: warm dots along a line through the centre, turning with the disc
            for (int i = 0; i < 30; i++) {
                final double s = (i / 29.0 - 0.5) * 2 * inner * 1.1;
                final double ang = turn + (s < 0 ? Math.PI : 0);
                dot(t, cx, cy, cz, Math.abs(s), ang, u, v, 0.6, 1f, 0.82f, 0.55f, 0.5f * bright);
            }
        }
        t.draw();
    }

    /** Planets mode: up to nine planets on their orbits in the tilted plane, each a small dot. */
    private static void planetOrbits(Tessellator t, double now, double cx, double cy, double cz, double[] u, double[] v,
        float aux) {
        final int n = Math.max(0, Math.min(9, Math.round(aux * 9)));
        if (n == 0) return;
        final float[][] palette = { { 0.85f, 0.6f, 0.4f }, { 0.5f, 0.7f, 1f }, { 0.9f, 0.85f, 0.6f },
            { 0.6f, 0.9f, 0.7f }, { 1f, 0.5f, 0.4f }, { 0.75f, 0.75f, 0.9f }, { 0.95f, 0.75f, 0.5f },
            { 0.6f, 0.6f, 0.65f }, { 0.4f, 0.55f, 0.9f } };
        t.startDrawingQuads();
        for (int i = 0; i < n; i++) {
            final double rad = 5 + 1.6 * i;
            final double ang = 2 * Math.PI * hash(i, 7) + now * DISC_RATE * 6 / Math.sqrt(1 + i);
            final float[] c = palette[i];
            dot(t, cx, cy, cz, rad, ang, u, v, 0.45, c[0], c[1], c[2], 0.9f);
        }
        t.draw();
    }

    /** One billboard dot of half-size {@code s} at polar (rad, ang) in the tilted disc plane. */
    private static void dot(Tessellator t, double cx, double cy, double cz, double rad, double ang, double[] u,
        double[] v, double s, float r, float g, float b, float alpha) {
        if (alpha <= 0.003f) return;
        final double x = cx + rad * Math.cos(ang);
        final double y = cy + rad * Math.sin(ang) * SIN_TILT;
        final double z = cz + rad * Math.sin(ang) * COS_TILT;
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertex(x - (u[0] + v[0]) * s, y - (u[1] + v[1]) * s, z - (u[2] + v[2]) * s);
        t.addVertex(x + (u[0] - v[0]) * s, y + (u[1] - v[1]) * s, z + (u[2] - v[2]) * s);
        t.addVertex(x + (u[0] + v[0]) * s, y + (u[1] + v[1]) * s, z + (u[2] + v[2]) * s);
        t.addVertex(x - (u[0] - v[0]) * s, y - (u[1] - v[1]) * s, z - (u[2] - v[2]) * s);
    }

    /** The corona's alpha at the limb for {@code kind} (0: none). */
    private static float coronaAlpha(byte kind, float aux) {
        switch (kind) {
            case EyeStarLook.STAR:
            case EyeStarLook.PROTOSTAR:
            case EyeStarLook.SWELL:
            case EyeStarLook.BOUNCE:
                return CORONA_ALPHA;
            case EyeStarLook.DWARF:
                return CORONA_ALPHA * (1f - aux);
            case EyeStarLook.NEUTRON:
                return NEUTRON_GLOW_ALPHA;
            default:
                return 0f;
        }
    }

    private static float coronaRadii(byte kind) {
        return kind == EyeStarLook.NEUTRON ? NEUTRON_GLOW_RADII : CORONA_RADII;
    }

    /**
     * D3: the magnetosphere (4.5 blocks, C080FF) always; on an event warning the outer shell (7 blocks, 7A4CFF) as
     * well, both edges pulsing between {@link #WARN_LOW} and {@link #WARN_HIGH} at 2 Hz so the warning reads from a
     * distance; and a flare every 4-8 s running from the magnetosphere out to the outer shell.
     */
    private static void magnetarShells(Tessellator t, TileEyeStar star, double now, double cx, double cy, double cz) {
        final boolean warn = star.getAux() > 0.5f;
        final float p = warn ? WARN_LOW + (WARN_HIGH - WARN_LOW) * (float) (0.5 + 0.5 * Math.sin(now * WARN_RATE)) : 0f;
        final float innerEdge = warn ? p : MAG_QUIET_EDGE;
        rimShell(t, cx, cy, cz, MAG_INNER_RADIUS, MAG_INNER_R, MAG_INNER_G, MAG_INNER_B, RIM_CENTRE, innerEdge);
        if (warn) {
            rimShell(
                t,
                cx,
                cy,
                cz,
                MAG_OUTER_RADIUS,
                MAG_OUTER_R,
                MAG_OUTER_G,
                MAG_OUTER_B,
                RIM_CENTRE,
                p * MAG_OUTER_EDGE / MAG_INNER_EDGE);
        }
        final double flare = star.flareAge(now);
        if (flare < FLARE_TICKS) {
            final float f = (float) (flare / FLARE_TICKS);
            rimShell(
                t,
                cx,
                cy,
                cz,
                MAG_INNER_RADIUS + (MAG_OUTER_RADIUS - MAG_INNER_RADIUS) * f,
                MAG_OUTER_R,
                MAG_OUTER_G,
                MAG_OUTER_B,
                RIM_CENTRE * (1f - f),
                MAG_OUTER_EDGE * (1f - f));
        }
    }

    /**
     * A rim-weighted additive shell: alpha {@code centre + (edge - centre) (1 - |N.V|)^3}. Back faces first, then front
     * faces (the blend is additive, so the order only matters for the depth test against what is already drawn).
     */
    private static void rimShell(Tessellator t, double cx, double cy, double cz, double rad, float r, float g, float b,
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

    /** One untextured sphere: {@link #alphas} then {@link #emit} (the caller sets the blend and the culling). */
    private static void sphereMesh(Tessellator t, double cx, double cy, double cz, double rad, float r, float g,
        float b, float a0, float a1, boolean cubic) {
        alphas(cx, cy, cz, rad, a0, a1, cubic);
        emit(t, cx, cy, cz, rad, r, g, b);
    }

    /**
     * The corners of every quad of a unit {@link #MESH_SLICES} x {@link #MESH_STACKS} sphere (x, y, z per corner, four
     * corners per quad), wound counter-clockwise seen from outside. Built once: the round-4 harness showed the trig of
     * building it per frame (three magnetar shells, both faces, plus the limb darkening) cost about 0.4 ms a frame.
     */
    private static final float[] UNIT_SPHERE = unitSphere();
    /** Per-corner alpha of the sphere being drawn (render thread only). */
    private static final float[] CORNER_ALPHA = new float[UNIT_SPHERE.length / 3];

    private static float[] unitSphere() {
        final float[] v = new float[MESH_STACKS * MESH_SLICES * 12];
        int k = 0;
        for (int i = 0; i < MESH_STACKS; i++) {
            final double p0 = Math.PI * i / MESH_STACKS - Math.PI / 2;
            final double p1 = Math.PI * (i + 1) / MESH_STACKS - Math.PI / 2;
            final double y0 = Math.sin(p0), y1 = Math.sin(p1);
            final double r0 = Math.cos(p0), r1 = Math.cos(p1);
            for (int j = 0; j < MESH_SLICES; j++) {
                final double a = 2 * Math.PI * j / MESH_SLICES, c = 2 * Math.PI * (j + 1) / MESH_SLICES;
                final double c0 = Math.cos(a), s0 = Math.sin(a), c1 = Math.cos(c), s1 = Math.sin(c);
                final double[] q = { r0 * c0, y0, r0 * s0, r1 * c0, y1, r1 * s0, r1 * c1, y1, r1 * s1, r0 * c1, y0,
                    r0 * s1 };
                for (double d : q) v[k++] = (float) d;
            }
        }
        return v;
    }

    /**
     * Fills {@link #CORNER_ALPHA} for a sphere of vertex radius {@code rad} round {@code (cx, cy, cz)}
     * (camera-relative: the camera is at the origin): {@code a0 + a1 (1 - |N.V|)^3} ({@code cubic}) or
     * {@code a0 + a1 (1 - |N.V|)}.
     */
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

    private static final double[] RING_COS = new double[RING_SEGMENTS + 1], RING_SIN = new double[RING_SEGMENTS + 1];

    static {
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            RING_COS[i] = Math.cos(2 * Math.PI * i / RING_SEGMENTS);
            RING_SIN[i] = Math.sin(2 * Math.PI * i / RING_SEGMENTS);
        }
    }

    /**
     * A flat ring (or a disc, {@code inner} 0) facing the camera, centred on {@code (ox, oy, oz)} in the plane spanned
     * by {@code u} and {@code v}: {@code alpha} at the inner edge falling linearly to 0 at {@code outer}.
     */
    private static void ring(Tessellator t, double ox, double oy, double oz, double[] u, double[] v, double inner,
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
     * D7: two polar beams leaning {@link #BEAM_TILT} off the vertical spin axis and sweeping round it with the
     * object's spin, from just inside the surface to {@link #BEAM_TIP} blocks from the centre (one block inside
     * {@link #CLEARANCE}, so a beam never reaches a band), {@link #BEAM_HALF_WIDTH} wide each side, {@code alpha} at
     * the root falling to 0 at the tip.
     */
    private static void beams(Tessellator t, double cx, double cy, double cz, float radius, float r, float g, float b,
        float alpha, double spinDeg) {
        final double turn = Math.toRadians(spinDeg % 360.0);
        final double ax = Math.sin(BEAM_TILT) * Math.cos(turn), ay = Math.cos(BEAM_TILT),
            az = Math.sin(BEAM_TILT) * Math.sin(turn);
        final float br = 0.5f + 0.5f * r, bg = 0.5f + 0.5f * g, bb = 0.5f + 0.5f * b;
        final double root = radius * 0.9;
        if (BEAM_TIP <= root) return;
        t.startDrawingQuads();
        for (int sign = -1; sign <= 1; sign += 2) {
            final double x0 = cx + sign * ax * root, y0 = cy + sign * ay * root, z0 = cz + sign * az * root;
            final double x1 = cx + sign * ax * BEAM_TIP, y1 = cy + sign * ay * BEAM_TIP, z1 = cz + sign * az * BEAM_TIP;
            // width across the beam and the line of sight
            double wx = ay * z0 - az * y0, wy = az * x0 - ax * z0, wz = ax * y0 - ay * x0;
            final double wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (wl < 1e-6) continue;
            wx *= BEAM_HALF_WIDTH / wl;
            wy *= BEAM_HALF_WIDTH / wl;
            wz *= BEAM_HALF_WIDTH / wl;
            t.setColorRGBA_F(br, bg, bb, alpha);
            t.addVertex(x0 - wx, y0 - wy, z0 - wz);
            t.addVertex(x0 + wx, y0 + wy, z0 + wz);
            t.setColorRGBA_F(br, bg, bb, 0f);
            t.addVertex(x1 + wx, y1 + wy, z1 + wz);
            t.addVertex(x1 - wx, y1 - wy, z1 - wz);
        }
        t.draw();
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

    /**
     * The fixed-function state of the untextured layers: no texture (on either unit), no lighting, no fog (it would
     * add the fog colour to an additive layer), smooth shading, blending and the depth test on, no depth writes. Saved
     * by {@link #begin}, put back by {@link #end}, including the cull face, its mode and the front-face winding.
     */
    private static final class Flat {

        private static final Flat INSTANCE = new Flat();

        private boolean texture, lightmap, lighting, cull, fog;
        private int shade, cullMode, frontFace;

        static Flat begin() {
            final Flat s = INSTANCE;
            s.texture = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
            s.lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
            s.cull = GL11.glGetBoolean(GL11.GL_CULL_FACE);
            s.fog = GL11.glGetBoolean(GL11.GL_FOG);
            s.shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            s.cullMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
            s.frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            s.lightmap = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            return s;
        }

        void end() {
            GL11.glShadeModel(shade);
            GL11.glCullFace(cullMode);
            GL11.glFrontFace(frontFace);
            RenderState.restore(GL11.GL_FOG, fog);
            RenderState.restore(GL11.GL_CULL_FACE, cull);
            RenderState.restore(GL11.GL_LIGHTING, lighting);
            RenderState.restore(GL11.GL_TEXTURE_2D, texture);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            RenderState.restore(GL11.GL_TEXTURE_2D, lightmap);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }
}
