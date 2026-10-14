package com.nateyburr.beyondthestars.machines.kerr;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.BlackHoleLens;
import com.nateyburr.beyondthestars.client.render.Blackbody;
import com.nateyburr.beyondthestars.client.render.GlSave;
import com.nateyburr.beyondthestars.client.render.GlowSprite;
import com.nateyburr.beyondthestars.client.render.GpuTimer;
import com.nateyburr.beyondthestars.client.render.Layers;
import com.nateyburr.beyondthestars.client.render.Lod;
import com.nateyburr.beyondthestars.client.render.RenderClock;
import com.nateyburr.beyondthestars.client.render.RenderQuality;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.BlackholeRenderer;

/**
 * The KEE's black hole (renders v2 §5, WP-1).
 * <p>
 * <b>Shader tiers (L0-L3):</b> the lensed hole of {@link BlackHoleLens}: a black shadow with a cream-gold photon ring,
 * a thin turbulent accretion disc in the cage's plane whose far side is lensed into arcs over and under the shadow,
 * the approaching side (the left, seen from outside; the disc turns anticlockwise seen from above) brighter and
 * whiter, the receding side dim gold, the inner edge reddened by gravity. Driven only by what the render tile already
 * syncs:
 * <ul>
 * <li>{@code kerrScale} (log mass): the size (the disc ends where GT's halo ended, {@code 1.8 x scale}, inside the
 * cage at every mass) and the disc's temperature, white-hot at 16 SM, gold-white at 64, golden-orange from 256;</li>
 * <li>GT's stability (coverage / 2): full coverage calm; less coverage thickens the disc, raises the turbulence,
 * slides its colour toward deep red and, past 40 % lost, drops plasma streamers into the shadow; no coverage tilts the
 * disc into a precessing wobble and makes it flicker;</li>
 * <li>GT's {@code scaling} / {@code startTime}: the hole grows from a point (GT's 40-tick smootherstep) with a white
 * opening ring, and shrinks to a point with a blue-white Hawking flash.</li>
 * </ul>
 * L3 (tiny on screen) adds a warm glow sprite so the hole still reads from far away.
 * <p>
 * <b>Fixed-function tier (quality low, shaders off or failed):</b> GT's own {@link BlackholeRenderer} (the BHC's
 * model and shader), scaled about the centre with {@code glScalef} exactly as before renders v2.
 */
@SideOnly(Side.CLIENT)
public class KerrHoleRenderer extends TileEntitySpecialRenderer {

    /** GT's halo, drawn at GL scale 1, reaches about 1.8 blocks: the disc's outer edge in blocks per unit scale. */
    static final double HALO_PER_SCALE = 1.8;
    /** The disc's outer and inner edges, r_s (the shadow's radius is about 2.6 r_s). */
    static final float R_OUT = 7.5f, R_IN = 1.45f;
    /** GT's opening / collapse animation, ticks. */
    private static final float GROW_TICKS = 40f;

    private final BlackholeRenderer delegate = new BlackholeRenderer();
    private final BlackHoleLens.Params params = new BlackHoleLens.Params();
    private final float[] glowRgb = new float[3];
    /** The programs the v2 hole draws with (one array: a frame allocates nothing, R-16). */
    private static final BTSShaders.Program[] PROGRAMS = { BlackHoleLens.PROGRAM, GlowSprite.PROGRAM };

    /** The delegate binds its textures through the dispatcher, so it gets the same one. */
    @Override
    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        super.func_147497_a(dispatcher);
        delegate.func_147497_a(dispatcher);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!Config.renderKerrHole || !(tile instanceof TileKerrHole) || RenderQuality.hidden()) return;
        final TileKerrHole hole = (TileKerrHole) tile;
        final float s = hole.getKerrScale();
        final double cx = x + 0.5;
        final double cy = y + 0.5;
        final double cz = z + 0.5;
        final double bound = s * HALO_PER_SCALE * BlackHoleLens.BOUND_PER_OUTER;
        final Lod.Tier tier = RenderQuality.tier(tile, bound, Math.sqrt(cx * cx + cy * cy + cz * cz), PROGRAMS);
        GpuTimer.begin("kerr");
        final boolean glCheck = RenderQuality.glCheckBegin(tile);
        try {
            if (tier.shaded() && drawLens(hole, s, cx, cy, cz, tier, partialTicks)) return;
            drawFixed(tile, s, x, y, z, cx, cy, cz, partialTicks);
        } finally {
            RenderQuality.glCheckEnd(tile, glCheck, "the Kerr hole");
            GpuTimer.end();
        }
    }

    /** GT's hole scaled about the centre (the LOW path, unchanged from before renders v2). */
    private void drawFixed(TileEntity tile, float s, double x, double y, double z, double cx, double cy, double cz,
        float partialTicks) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(cx, cy, cz);
            GL11.glScalef(s, s, s);
            GL11.glTranslated(-cx, -cy, -cz);
            delegate.renderTileEntityAt(tile, x, y, z, partialTicks);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** The v2 hole; false if the program is off (then the caller draws the fixed path). */
    private boolean drawLens(TileKerrHole hole, float s, double cx, double cy, double cz, Lod.Tier tier,
        float partialTicks) {
        final World world = hole.getWorldObj();
        final BlackHoleLens.Params p = params;
        final float time = RenderClock.seconds(world, partialTicks);
        final double now = (world == null ? 0 : world.getTotalWorldTime()) + partialTicks;
        final float age = (float) (now - hole.getStartTime());
        final boolean opening = hole.getScaling();
        final float growth = growth(opening, age);
        look(p, s, coverage(hole), time);

        p.cx = cx;
        p.cy = cy;
        p.cz = cz;
        final double rsFull = s * HALO_PER_SCALE / R_OUT;
        p.rsBlocks = rsFull * Math.max(growth, 0.02f);
        // flashes, radii in r_s of the current (grown / shrunk) hole
        final double toRs = rsFull / p.rsBlocks;
        p.openIntensity = p.hawkIntensity = 0f;
        if (opening && age >= 0 && age < 25) {
            final float f = age / 25f;
            p.openRadius = (float) (Math.pow(f, 0.6) * R_OUT * toRs);
            p.openIntensity = 1.6f * (1 - f) * (1 - f);
        } else if (!opening && age >= 24 && age < GROW_TICKS) {
            final float f = (age - 24) / (GROW_TICKS - 24);
            p.hawkRadius = (float) (f * R_OUT * 0.9 * toRs);
            p.hawkIntensity = 2.2f * (1 - f);
        }
        p.stars = world == null ? 0f : Math.min(1f, world.getStarBrightness(partialTicks) * 2f);

        if (tier == Lod.Tier.L3 && growth > 0.05f) {
            // far away: a warm glow so the hole still reads, then the hole over it (its shadow is the black dot)
            final float[] c = Blackbody.rgb(p.temperature * 0.85f, glowRgb);
            final GlSave save = Layers.emissive();
            try {
                GlowSprite
                    .draw(cx, cy, cz, rsFull * R_OUT * 1.5 * growth, c[0], c[1], c[2], 0.55f * p.exposure, 0.22f, 0.5f);
            } finally {
                save.restore();
            }
        }
        return BlackHoleLens.draw(p, tier);
    }

    /** The coverage 0..1 (GT's stability input is coverage / 2, see TileKerrHole#setLook). */
    static float coverage(TileKerrHole hole) {
        return Math.max(0f, Math.min(1f, hole.getStability() * 2f));
    }

    /**
     * GT's growth: 0 -> 1 over 40 ticks after opening (1 -> 0 after closing), the smootherstep GT applies to the first
     * half of its curve, normalised so a fully open hole is 1.
     */
    static float growth(boolean opening, float age) {
        float t = Math.max(0f, Math.min(1f, (opening ? age : GROW_TICKS - age) / GROW_TICKS)) * 0.5f;
        t = t * t * t * (t * (6f * t - 15f) + 10f);
        return t / 0.5f;
    }

    /** The mass (SM) a render scale stands for (MTEKerrEngine.renderScaleFor inverted; clamps above about 300 SM). */
    static double massForScale(float s) {
        final double core = s / (MTEKerrEngine.HALO_PER_CORE / HALO_PER_SCALE);
        return 16.0 * Math.pow(2.0, (core - 3.2) / 0.75);
    }

    /**
     * The look from mass and coverage (spec §5.3). Display temperatures: 9,000 K x (M / 16)^-0.3 clamped 3,800 to
     * 13,000 K (white-hot at 16 SM, gold-white at 64, golden-orange from 256), sliding toward 2,000 K by
     * {@code (1 - c)^2}.
     */
    static void look(BlackHoleLens.Params p, float s, float c, float time) {
        final double m = massForScale(s);
        final float u = 1f - c;
        float temp = (float) Math.max(3800, Math.min(13000, 9000 * Math.pow(Math.max(m, 1) / 16.0, -0.3)));
        temp += (2000 - temp) * u * u;
        p.temperature = temp;
        p.rIn = R_IN;
        p.rOut = R_OUT;
        p.turbulence = 0.25f + 0.75f * u;
        p.thickness = 0.02f + 0.08f * u;
        p.streamers = smooth((u - 0.4f) / 0.4f);
        final float fail = 1f - smooth(c / 0.05f);
        p.tilt = 0.14f * fail;
        p.tiltAzimuth = (float) (2 * Math.PI * time / 5.0);
        p.exposure = 1.2f
            * (1f - 0.35f * fail * (0.5f + 0.5f * (float) (Math.sin(time * 23.0) * Math.sin(time * 7.3))));
        p.beaming = 2.5f;
        p.ring = 2.5f;
        p.glow = 0.07f;
        p.drag = 0.35f;
        p.time = time;
    }

    private static float smooth(float x) {
        final float t = Math.max(0f, Math.min(1f, x));
        return t * t * (3f - 2f * t);
    }
}
