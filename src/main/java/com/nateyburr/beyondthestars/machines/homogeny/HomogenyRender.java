package com.nateyburr.beyondthestars.machines.homogeny;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.DeferredDraws;
import com.nateyburr.beyondthestars.client.render.GlSave;
import com.nateyburr.beyondthestars.client.render.GlowSprite;
import com.nateyburr.beyondthestars.client.render.GpuTimer;
import com.nateyburr.beyondthestars.client.render.Layers;
import com.nateyburr.beyondthestars.client.render.Lod;
import com.nateyburr.beyondthestars.client.render.MachineFrame;
import com.nateyburr.beyondthestars.client.render.MachineFx;
import com.nateyburr.beyondthestars.client.render.NoiseTex;
import com.nateyburr.beyondthestars.client.render.RenderClock;
import com.nateyburr.beyondthestars.client.render.RenderQuality;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Renders v2 (§11, WP-7): the Homogeny's vortex, from my "Mixer" sketch. Drawn through GT's
 * {@code IMTERenderer} from the controller, in the mockup's voxel frame ({@link MachineFrame}, machine
 * {@code homogeny}: the column's axis).
 * <ul>
 * <li><b>The vortex</b> ({@code bts_mvol} mode 0) fills the glass mixing column (radius 12, y -26 .. 26): one braid
 * of plasma per tier (T1 one, T2 two, T3 three) spiralling up a funnel that is fat in the middle and narrow at the
 * caps, each braid in its feed's colour at the bottom and the product's at the top (the mix homogenises as it rises),
 * a turbulent swirling sheath, sparks riding the flow, at T3 a bright core thread; the green shear helix glows on the
 * inside of the glass with pulses running up it. Running: bright and fast; idle: a dim, slow swirl.</li>
 * <li><b>Feed streams</b> ({@code bts_beam}): an arc from each feed vessel (T1 three, T2+ six) over the volute into the
 * column, in that vessel's feed colour, knots running inward while it runs.</li>
 * <li>A glow over the top cap's glass in the product colour; L3 is that glow alone.</li>
 * </ul>
 * Colours and tier arrive on GT's render-data channel ({@link MTEHomogeny#encodeRenderData}); the running state is
 * GT's active flag. The fixed-function (LOW) path draws nothing, as before v2.
 */
@SideOnly(Side.CLIENT)
public final class HomogenyRender {

    /** The column's air: radius and half-height in voxel blocks. */
    private static final float COL_R = 11.85f, COL_H = 26.45f;
    /** Feed vessels: distance from the axis; the T1 and T2 angles (degrees). */
    private static final double VESSEL_R = 38;
    private static final int[] VESSELS_T1 = { 30, 150, 270 }, VESSELS_T2 = { 90, 210, 330 };
    /** March steps and noise octaves per tier L0..L2. */
    private static final int[][] STEPS = { { 10, 2 }, { 8, 2 }, { 6, 1 } };

    private static final class State {

        final MachineFrame frame = new MachineFrame("homogeny");
        float activity = -1;
        long lastNanos;
    }

    /**
     * Render QA harness override (renders v2 WP-7, set by RenderTestClient): {activity 0..1 or -1 live, tier 1..3 or
     * 0 live}; null live.
     */
    public static volatile float[] debugLook;

    private static final Map<MTEHomogeny, State> STATES = new WeakHashMap<>();
    private static final Matrix4f MODEL = new Matrix4f();
    private static final MachineFx.Volume VOL = new MachineFx.Volume();
    private static final MachineFx.Beam BEAM = new MachineFx.Beam();
    private static final double[] C = new double[3], CAM = new double[3], P0 = new double[3], P1 = new double[3],
        P2 = new double[3];

    private HomogenyRender() {}

    private static final DeferredDraws.Draw<MTEHomogeny> DRAW = HomogenyRender::renderNow;
    /** The programs the vortex draws with (one array: a frame allocates nothing, R-16). */
    private static final BTSShaders.Program[] PROGRAMS = { MachineFx.VOL_PROGRAM, MachineFx.BEAM_PROGRAM,
        GlowSprite.PROGRAM };
    /** Colour scratch (render thread). */
    private static final float[] PRODUCT = new float[3], FEED = new float[3];

    /** GT's pass-0 hook: queues the draw for after the translucent pass ({@link DeferredDraws}). */
    public static void render(MTEHomogeny m, double x, double y, double z, float partialTicks) {
        if (Config.renderHomogeny && m.renderTier() > 0) DeferredDraws.add(DRAW, PROGRAMS, m, x, y, z, partialTicks);
    }

    private static void renderNow(MTEHomogeny m, double x, double y, double z, float partialTicks) {
        if (!Config.renderHomogeny || RenderQuality.hidden()) return;
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        if (base == null || m.renderTier() <= 0) return;
        State s = STATES.get(m);
        if (s == null) STATES.put(m, s = new State());
        if (!s.frame.update(m.getExtendedFacing())) return;
        s.frame.at(x, y, z, 0, 0, 0, C);
        final double dist = Math.sqrt(C[0] * C[0] + C[1] * C[1] + C[2] * C[2]);
        final Lod.Tier tier = RenderQuality.tier(m, COL_H, dist, PROGRAMS);
        if (!tier.shaded()) return;
        // running eases in over about a second and out over two
        final long now = System.nanoTime();
        final float dt = s.lastNanos == 0 ? 0f : Math.min(0.25f, (now - s.lastNanos) / 1e9f);
        s.lastNanos = now;
        final float target = base.isActive() ? 1f : 0f;
        if (s.activity < 0) s.activity = target;
        s.activity += (target - s.activity) * Math.min(1f, dt * (target > s.activity ? 1.2f : 0.6f));
        final float[] dbg = debugLook;
        final float act = dbg != null && dbg[0] >= 0 ? dbg[0] : s.activity;
        final int shownTier = dbg != null && dbg.length > 1 && dbg[1] > 0 ? (int) dbg[1] : m.renderTier();
        GpuTimer.begin("homogeny");
        final boolean glCheck = RenderQuality.glCheckBegin(m);
        try {
            draw(m, s, x, y, z, tier, RenderClock.seconds(base.getWorld(), partialTicks), act, shownTier);
        } finally {
            RenderQuality.glCheckEnd(m, glCheck, "the Homogeny");
            GpuTimer.end();
        }
    }

    private static void draw(MTEHomogeny m, State s, double x, double y, double z, Lod.Tier tier, float time, float act,
        int shownTier) {
        final int t = Math.max(1, Math.min(3, shownTier));
        final float[] product = rgb(m.renderColour(3), PRODUCT);
        final int[] noiseWas = NoiseTex.bind();
        GlSave save = Layers.emissive();
        try {
            if (tier == Lod.Tier.L3) {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                GlowSprite
                    .draw(C[0], C[1], C[2], 24, product[0], product[1], product[2], 0.25f + 0.5f * act, 0.18f, 0.45f);
                return;
            }
            // 1. the vortex (premultiplied)
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            s.frame.model(MODEL, C, 1.0);
            s.frame.camLocal(C, CAM);
            final MachineFx.Volume v = VOL;
            v.mode = 0f;
            v.a = COL_R;
            v.b = COL_R;
            v.y0 = -COL_H;
            v.y1 = COL_H;
            v.time = time;
            v.steps = STEPS[tier.ordinal()][0];
            v.octaves = STEPS[tier.ordinal()][1];
            v.va[0] = t;
            v.va[1] = act;
            v.va[2] = 0.3f;
            v.va[3] = 1f;
            for (int i = 0; i < 3; i++) {
                rgb(m.renderColour(i), i == 0 ? v.vb : i == 1 ? v.vc : v.vd);
            }
            System.arraycopy(product, 0, v.ve, 0, 3);
            if (!MachineFx.drawVolume(MODEL, CAM, v)) return;

            // 2. feed streams and the top glow (additive)
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            if (tier != Lod.Tier.L2) {
                for (int k = 0; k < (t >= 2 ? 6 : 3); k++) {
                    final int deg = k < 3 ? VESSELS_T1[k] : VESSELS_T2[k - 3];
                    final double a = Math.toRadians(deg), ca = Math.cos(a), sa = Math.sin(a);
                    s.frame.at(x, y, z, VESSEL_R * ca, 9.0, VESSEL_R * sa, P0);
                    s.frame.at(x, y, z, 25 * ca, 22.0, 25 * sa, P1);
                    s.frame.at(x, y, z, 12.4 * ca, 15.0, 12.4 * sa, P2);
                    final float[] c = rgb(m.renderColour(k % 3), FEED);
                    final MachineFx.Beam b = BEAM;
                    b.w0 = 0.45f;
                    b.w1 = 0.6f;
                    b.intensity = 0.1f + 0.85f * act;
                    b.speed = 3f + 6f * act;
                    b.time = time;
                    b.strand = 0.6f;
                    b.rate = 0.4f + 1.4f * act;
                    b.fadeIn = 1.2f;
                    b.fadeOut = 1.5f;
                    b.seed = k * 3.7f;
                    b.pitch = 4f;
                    b.core[0] = 0.55f + 0.45f * c[0];
                    b.core[1] = 0.55f + 0.45f * c[1];
                    b.core[2] = 0.55f + 0.45f * c[2];
                    b.sheath[0] = c[0];
                    b.sheath[1] = c[1];
                    b.sheath[2] = c[2];
                    MachineFx.drawBeam(P0, P1, P2, MachineFx.bezierLength(P0, P1, P2), b);
                }
            }
            s.frame.at(x, y, z, 0, 27.5, 0, P0);
            GlowSprite
                .draw(P0[0], P0[1], P0[2], 11, product[0], product[1], product[2], 0.12f + 0.3f * act, 0.25f, 0.5f);
        } finally {
            save.restore();
            NoiseTex.unbind(noiseWas);
        }
    }

    /** {@code c} (0xRRGGBB) as 0..1 floats into the first three entries of {@code out}; returns {@code out}. */
    static float[] rgb(int c, float[] out) {
        out[0] = ((c >> 16) & 255) / 255f;
        out[1] = ((c >> 8) & 255) / 255f;
        out[2] = (c & 255) / 255f;
        return out;
    }
}
