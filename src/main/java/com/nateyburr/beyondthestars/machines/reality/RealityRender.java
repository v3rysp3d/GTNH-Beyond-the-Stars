package com.nateyburr.beyondthestars.machines.reality;

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
 * Renders v2 (§12, WP-8): Reality Diffraction and Reality Refab, from my two sketches. Drawn through GT's
 * {@code IMTERenderer} from the controller, in each mockup's voxel frame ({@link MachineFrame}: the machine's axis,
 * y 0 at the middle deck band / the belt).
 * <p>
 * <b>Diffraction</b>: the beam rises up the spine ({@code bts_mvol} mode 2) through three faceted lenses that glow and
 * disperse; in each of the upper three drums the lens splits it into two thin rainbow sheets fanning out toward that
 * drum's region blades, with interference fringes running outward, and two billowing region clouds in the regions'
 * colours (Hyperspace blue, Subspace violet, Forward Time gold, Reverse Time green, Positive Mass red-orange, Negative
 * Mass pink) fill the drum's halves; the bottom drum swirls with the Entanglement Medium drawn into the spine (mode 1);
 * faint fringes ripple over every deck band. The phase rings' field shows as a rim-lit sphere with shimmering
 * hexagonal cells ({@code bts_mfx} mode 0), and each region's red anchor pulses while it is held (an unanchored
 * region's cloud flickers dim).
 * <p>
 * <b>Refab</b>: matter streams up the hourglass spindle as a twisted glowing ribbon ({@code bts_beam}); over each red
 * tray two holographic glyph rings turn against each other ({@code bts_mfx} mode 1) and small voxels fly in and lock
 * into place (mode 2); the Reality Focus and the Ground Anchor crystal glow, the Focus sends a slow beam up, the six
 * Region Receivers on the belt glow in their regions' colours.
 * <p>
 * Running eases in and out from GT's active flag; idle keeps a dim calm look. The fixed-function (LOW) path draws
 * nothing, as before v2. L3: one glow sprite.
 */
@SideOnly(Side.CLIENT)
public final class RealityRender {

    /** The Diffraction's deck bands (voxel y) and drum walls' inner semi-axes. */
    private static final float[] DIFF_Y = { -28, -14, 0, 14, 28 };
    private static final float DRUM_A = 31.6f, DRUM_B = 29.7f;
    /** The deck bands' outer oval (blades and anchors stand on it). */
    private static final double DECK_A = 34, DECK_B = 32;
    /** The Refab's trays (voxel y). */
    private static final float[] TRAY_Y = { -24, 0, 24 };
    /** Medium (Entanglement Medium) colour. */
    private static final float[] MEDIUM = { 0.45f, 0.85f, 1.0f };
    /** The regions' colours (as fx/vol.glsl mvRegion). */
    private static final float[][] REGION = { { 0.30f, 0.75f, 1.00f }, { 0.55f, 0.30f, 1.00f }, { 1.00f, 0.80f, 0.30f },
        { 0.25f, 1.00f, 0.70f }, { 1.00f, 0.40f, 0.25f }, { 1.00f, 0.35f, 0.80f } };
    /** Volume steps and octaves per tier L0..L2 (drums, spine). */
    private static final int[][] DRUM_STEPS = { { 8, 2 }, { 6, 2 }, { 4, 1 } };
    private static final int[][] SPINE_STEPS = { { 10, 1 }, { 8, 1 }, { 6, 1 } };

    /**
     * Render QA harness override (renders v2 WP-6..WP-8, set by RenderTestClient): {activity 0..1 or -1 live, mask or
     * -1 live, progress or -1 live}; null live.
     */
    public static volatile float[] debugLook;

    private static final class State {

        final MachineFrame frame;
        float activity = -1;
        long lastNanos;

        State(String key) {
            frame = new MachineFrame(key);
        }
    }

    private static final Map<MTERealityBase<?>, State> STATES = new WeakHashMap<>();
    private static final Matrix4f MODEL = new Matrix4f();
    private static final MachineFx.Volume VOL = new MachineFx.Volume();
    private static final MachineFx.Beam BEAM = new MachineFx.Beam();
    private static final double[] C = new double[3], CAM = new double[3], P0 = new double[3], P1 = new double[3];

    private RealityRender() {}

    private static final DeferredDraws.Draw<MTERealityBase<?>> DRAW = RealityRender::renderNow;
    /** The programs the Reality renders draw with (one array: a frame allocates nothing, R-16). */
    private static final BTSShaders.Program[] PROGRAMS = { MachineFx.VOL_PROGRAM, MachineFx.BEAM_PROGRAM,
        MachineFx.MFX_PROGRAM, GlowSprite.PROGRAM };
    /** Refab's far glow colour. */
    private static final float[] REFAB_GLOW = { 0.45f, 0.75f, 1f };

    /** GT's pass-0 hook: queues the draw for after the translucent pass ({@link DeferredDraws}). */
    public static void render(MTERealityBase<?> m, double x, double y, double z, float partialTicks) {
        if (Config.renderReality && m.renderFormed()) DeferredDraws.add(DRAW, m, x, y, z, partialTicks);
    }

    private static void renderNow(MTERealityBase<?> m, double x, double y, double z, float partialTicks) {
        if (!Config.renderReality || RenderQuality.hidden()) return;
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        if (base == null || !m.renderFormed()) return;
        State s = STATES.get(m);
        if (s == null) STATES.put(m, s = new State(m.piece()));
        if (!s.frame.update(m.getExtendedFacing())) return;
        final boolean diffraction = m instanceof MTERealityDiffraction;
        s.frame.at(x, y, z, 0, diffraction ? 0 : 12, 0, C);
        final double dist = Math.sqrt(C[0] * C[0] + C[1] * C[1] + C[2] * C[2]);
        final Lod.Tier tier = RenderQuality.tier(m, diffraction ? 34 : 40, dist, PROGRAMS);
        if (!tier.shaded()) return;
        final float[] dbg = debugLook;
        final long now = System.nanoTime();
        final float dt = s.lastNanos == 0 ? 0f : Math.min(0.25f, (now - s.lastNanos) / 1e9f);
        s.lastNanos = now;
        final float target = base.isActive() ? 1f : 0f;
        if (s.activity < 0) s.activity = target;
        s.activity += (target - s.activity) * Math.min(1f, dt * (target > s.activity ? 1.2f : 0.6f));
        final float act = dbg != null && dbg[0] >= 0 ? dbg[0] : s.activity;
        final int mask = dbg != null && dbg[1] >= 0 ? (int) dbg[1] : m.renderMask();
        final float time = RenderClock.seconds(base.getWorld(), partialTicks);
        GpuTimer.begin("reality");
        final boolean glCheck = RenderQuality.glCheckBegin(m);
        final int[] noiseWas = NoiseTex.bind();
        final GlSave save = Layers.emissive();
        try {
            if (tier == Lod.Tier.L3) {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                final float[] c = diffraction ? MEDIUM : REFAB_GLOW;
                GlowSprite.draw(C[0], C[1], C[2], 30, c[0], c[1], c[2], 0.2f + 0.45f * act, 0.18f, 0.45f);
            } else if (diffraction) {
                drawDiffraction(s, x, y, z, tier, act, mask, time);
            } else {
                drawRefab(s, x, y, z, tier, act, time);
            }
        } finally {
            save.restore();
            NoiseTex.unbind(noiseWas);
            RenderQuality.glCheckEnd(m, glCheck, "a Reality machine");
            GpuTimer.end();
        }
    }

    // ============================================================================ Diffraction

    private static void drawDiffraction(State s, double x, double y, double z, Lod.Tier tier, float act, int mask,
        float time) {
        s.frame.at(x, y, z, 0, 0, 0, C);
        s.frame.model(MODEL, C, 1.0);
        s.frame.camLocal(C, CAM);
        final int ti = Math.min(2, tier.ordinal());
        // 1. the drums and the spine (premultiplied volumes)
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        final MachineFx.Volume v = VOL;
        v.time = time;
        for (int k = 0; k < 4; k++) {
            v.mode = 1f;
            v.a = DRUM_A;
            v.b = DRUM_B;
            v.y0 = DIFF_Y[k] + 2.55f;
            v.y1 = DIFF_Y[k + 1] - 2.55f;
            v.steps = DRUM_STEPS[ti][0];
            v.octaves = DRUM_STEPS[ti][1];
            v.va[0] = k;
            v.va[1] = act;
            v.va[2] = (float) Math.toRadians(60 * Math.max(0, k - 1));
            v.va[3] = mask & 0x3F;
            System.arraycopy(MEDIUM, 0, v.vb, 0, 3);
            v.ve[3] = 0.45f + 0.55f * act;
            if (!MachineFx.drawVolume(MODEL, CAM, v)) return;
        }
        v.mode = 2f;
        v.a = 3.7f;
        v.b = 3.7f;
        v.y0 = -42.5f;
        v.y1 = 37.5f;
        v.steps = SPINE_STEPS[ti][0];
        v.octaves = SPINE_STEPS[ti][1];
        v.va[0] = act;
        v.va[1] = 1f;
        v.va[2] = 0f;
        v.va[3] = 0f;
        System.arraycopy(MEDIUM, 0, v.vb, 0, 3);
        MachineFx.drawVolume(MODEL, CAM, v);

        // 2. the phase field (premultiplied, additive-only output)
        if (tier != Lod.Tier.L2) {
            GL11.glDisable(GL11.GL_CULL_FACE);
            MachineFx.drawShell(MODEL, CAM, 47f, 47f, 0f, time, 0.10f + 0.30f * act, 0.35f, 0.65f, 1.0f);
        }

        // 3. anchors, the apex crystal (additive glows)
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        for (int j = 0; j < 3; j++) {
            for (int side = 0; side < 2; side++) {
                final int region = 2 * j + side;
                final double a = Math.toRadians(60 * j + 180 * side);
                final double pr = polarE(DECK_A, DECK_B, a);
                s.frame.at(x, y, z, (pr + 2.6) * Math.cos(a), DIFF_Y[j + 1] - 2, (pr + 2.6) * Math.sin(a), P0);
                final boolean held = (mask >> region & 1) != 0;
                final float pulse = held ? 0.55f + 0.45f * (float) Math.sin(time * 3.2 + region) : 0.12f;
                GlowSprite.draw(P0[0], P0[1], P0[2], 3.2, 1f, 0.18f, 0.16f, pulse * (0.4f + 0.6f * act), 0.2f, 0.5f);
                // the blade beyond it glows in its region's colour
                s.frame.at(x, y, z, (pr + 8) * Math.cos(a), DIFF_Y[j + 1], (pr + 8) * Math.sin(a), P0);
                final float[] rc = REGION[region];
                GlowSprite.draw(
                    P0[0],
                    P0[1],
                    P0[2],
                    9,
                    rc[0],
                    rc[1],
                    rc[2],
                    (held ? 0.35f : 0.08f) * (0.25f + 0.75f * act),
                    0.2f,
                    0.5f);
            }
        }
        s.frame.at(x, y, z, 0, 50, 0, P0);
        GlowSprite.draw(P0[0], P0[1], P0[2], 9, 1f, 0.7f, 0.35f, 0.15f + 0.5f * act, 0.22f, 0.5f);
    }

    // ================================================================================ Refab

    private static double hullR(double y) {
        if (y <= -6) return 8 + 26 * Math.sqrt(Math.max(0, 1 - Math.pow((-6 - y) / 58, 2)));
        if (y < 6) return 34;
        return 34 - 17 * Math.pow((y - 6) / 40, 1.4);
    }

    private static double spindleR(double y) {
        return 4 + 5 * Math.pow(Math.abs(y - 3) / 43, 2);
    }

    private static void drawRefab(State s, double x, double y, double z, Lod.Tier tier, float act, float time) {
        s.frame.at(x, y, z, 0, 0, 0, C);
        s.frame.model(MODEL, C, 1.0);
        s.frame.camLocal(C, CAM);
        // 1. the stream up the spindle and the Focus's beam (additive)
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        final MachineFx.Beam b = BEAM;
        b.w0 = 1.3f;
        b.w1 = 1.3f;
        b.intensity = 0.25f + 0.85f * act;
        b.speed = 4f + 10f * act;
        b.time = time;
        b.strand = 1.1f;
        b.rate = 0.3f + 1.6f * act;
        b.fadeIn = 3f;
        b.fadeOut = 3f;
        b.seed = 5.1f;
        b.pitch = 7f;
        b.core[0] = 0.8f;
        b.core[1] = 0.95f;
        b.core[2] = 1f;
        b.sheath[0] = 0.25f;
        b.sheath[1] = 0.55f;
        b.sheath[2] = 1f;
        s.frame.at(x, y, z, 0, -39, 0, P0);
        s.frame.at(x, y, z, 0, 45, 0, P1);
        if (!MachineFx.drawBeam(P0, P1, b)) return;
        // the Focus: a slow beam up from its tip
        b.w0 = 0.7f;
        b.w1 = 2.2f;
        b.intensity = 0.08f + 0.5f * act;
        b.speed = 2.5f;
        b.strand = 0.4f;
        b.rate = 0.25f;
        b.fadeIn = 1f;
        b.fadeOut = 30f;
        b.seed = 9.3f;
        b.pitch = 12f;
        b.core[0] = 1f;
        b.core[1] = 0.85f;
        b.core[2] = 0.6f;
        b.sheath[0] = 1f;
        b.sheath[1] = 0.55f;
        b.sheath[2] = 0.2f;
        s.frame.at(x, y, z, 0, 72.5, 0, P0);
        s.frame.at(x, y, z, 0, 112, 0, P1);
        MachineFx.drawBeam(P0, P1, b);
        // crystal glows: the Focus, the Ground Anchor, the six Region Receivers
        s.frame.at(x, y, z, 0, 59.5, 0, P0);
        GlowSprite.draw(P0[0], P0[1], P0[2], 16, 1f, 0.62f, 0.25f, 0.18f + 0.45f * act, 0.2f, 0.5f);
        s.frame.at(x, y, z, 0, -52, 0, P0);
        GlowSprite.draw(P0[0], P0[1], P0[2], 14, 1f, 0.55f, 0.2f, 0.12f + 0.35f * act, 0.2f, 0.5f);
        final double[] elev = { 0, 30, -30, 0, 30, -30 };
        for (int k = 0; k < 6; k++) {
            final double a = Math.toRadians(60 * k), el = Math.toRadians(elev[k]);
            final double pr = polarE(36, 34, a);
            final double dx = Math.cos(el) * Math.cos(a), dy = Math.sin(el), dz = Math.cos(el) * Math.sin(a);
            s.frame.at(x, y, z, pr * Math.cos(a) + 9 * dx, 9 * dy, pr * Math.sin(a) + 9 * dz, P0);
            final float[] rc = REGION[k];
            GlowSprite.draw(P0[0], P0[1], P0[2], 8, rc[0], rc[1], rc[2], 0.06f + 0.3f * act, 0.2f, 0.5f);
        }

        // 2. glyph rings and reassembling voxels over the trays (premultiplied, additive-only output)
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        for (int i = 0; i < TRAY_Y.length; i++) {
            final float ty = TRAY_Y[i];
            final float rs = (float) spindleR(ty);
            final float rc = i == 1 ? 28f : (float) hullR(ty) - 4.5f;
            final float glow = 0.6f + 1.2f * act;
            MachineFx.drawGlyphRing(
                MODEL,
                CAM,
                rs + 2.6f,
                rs + 5.4f,
                ty + 1.6f,
                24f,
                time * 0.02f * (0.3f + act),
                time,
                glow,
                0.35f,
                0.75f,
                1f);
            MachineFx.drawGlyphRing(
                MODEL,
                CAM,
                rc - 9.5f,
                rc - 6.2f,
                ty + 2.4f,
                56f,
                -time * 0.012f * (0.3f + act),
                time,
                glow,
                0.35f,
                0.75f,
                1f);
            if (tier == Lod.Tier.L0 || tier == Lod.Tier.L1) {
                MachineFx.drawVoxels(
                    MODEL,
                    CAM,
                    ty - 0.5f,
                    rs + 6f,
                    rc - 10.5f,
                    0.45f,
                    0.22f + 0.1f * i,
                    act,
                    time,
                    0.45f,
                    0.85f,
                    1f);
            }
        }
    }

    static double polarE(double a, double b, double phi) {
        return a * b / Math.hypot(b * Math.cos(phi), a * Math.sin(phi));
    }
}
