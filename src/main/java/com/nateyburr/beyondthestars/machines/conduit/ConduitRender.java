package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.client.render.DeferredDraws;
import com.nateyburr.beyondthestars.client.render.GlSave;
import com.nateyburr.beyondthestars.client.render.GlowSprite;
import com.nateyburr.beyondthestars.client.render.GpuTimer;
import com.nateyburr.beyondthestars.client.render.Layers;
import com.nateyburr.beyondthestars.client.render.Lod;
import com.nateyburr.beyondthestars.client.render.MachineFx;
import com.nateyburr.beyondthestars.client.render.NoiseTex;
import com.nateyburr.beyondthestars.client.render.RenderClock;
import com.nateyburr.beyondthestars.client.render.RenderQuality;
import com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown;
import com.nateyburr.beyondthestars.machines.quasar.QuasarGeometry;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Renders v2 (§10, WP-6): the conduit, drawn through GT's {@code IMTERenderer} from each element's and crown's
 * controller.
 * <ul>
 * <li><b>Burst streams</b> ({@code bts_beam}): every out-throat that sent a stream last second draws it along the
 * throat's normal to where its route ends (the next element's opening, a GRB Receptor Port, the block in the way; a
 * stream that reaches nothing fades out over the ray's range): a white core, a violet sheath, a helical strand and
 * knots running at the stream's rate, wider and brighter with its Bu/t, a soft glow where it leaves and where it enters
 * a receiver.</li>
 * <li><b>The Wormhole Mouth</b> ({@code bts_mfx} mode 3): a portal disc in its ring, a bent starfield and nebula of
 * somewhere else inside a bright blue rim, full while paired with a live partner, faint while open and unpaired, a dim
 * rim while closed; brighter while a stream goes in.</li>
 * <li><b>The crown</b>: its intake glows with the absorbed burst, white at the throat to deep orange at the rim, with a
 * heat column rising into the cup, brighter with the Bu/t it takes (log scale to its Mk's cap).</li>
 * </ul>
 * The data arrive on GT's render-data channel once a second (out / in Bu/t, route length and kind; the crown's Mk and
 * stream). The fixed-function (LOW) path draws nothing, as before v2.
 */
@SideOnly(Side.CLIENT)
public final class ConduitRender {

    /** Route kinds (ConduitNet.Kind ordinals). */
    private static final int ELEMENT = 0, RECEPTOR = 1, NOTHING = 4;

    private static final class State {

        ExtendedFacing facing;
        ConduitThroats.WorldThroat[] throats;
    }

    /**
     * Render QA harness override (renders v2 WP-6, set by RenderTestClient): {Bu/t, ...}: every out-throat (and a
     * paired mouth's ring) streams that much, its length traced through the client world; null live. The harness rig
     * cannot form mouth A at its site height, so no stream reaches the conduit there by itself.
     */
    public static volatile float[] debugLook;

    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static final Matrix4f MODEL = new Matrix4f();
    private static final MachineFx.Beam BEAM = new MachineFx.Beam();
    private static final double[] A = new double[3], B = new double[3], CAM = new double[3];

    private ConduitRender() {}

    // ============================================================================ elements

    /** GT's pass-0 hook: queues the draw for after the translucent pass ({@link DeferredDraws}). */
    public static void renderElement(MTEConduitElement<?> m, double x, double y, double z, float partialTicks) {
        if (Config.renderConduit && m.renderThroats() > 0) {
            DeferredDraws.add(() -> renderElementNow(m, x, y, z, partialTicks));
        }
    }

    private static void renderElementNow(MTEConduitElement<?> m, double x, double y, double z, float partialTicks) {
        if (!Config.renderConduit) return;
        final IGregTechTileEntity base = m.getBaseMetaTileEntity();
        final int n = m.renderThroats();
        if (base == null || n == 0) return;
        final float[] dbg = debugLook;
        State s = STATES.get(m);
        if (s == null) STATES.put(m, s = new State());
        final ExtendedFacing f = m.getExtendedFacing();
        if (f == null) return;
        if (s.facing != f || s.throats == null || s.throats.length != n) {
            s.facing = f;
            s.throats = new ConduitThroats.WorldThroat[n];
            for (int i = 0; i < n; i++) s.throats[i] = m.worldThroat(i);
        }
        final double ox = x - base.getXCoord(), oy = y - base.getYCoord(), oz = z - base.getZCoord();
        final double[] c0 = s.throats[0].centre;
        final double dist = Math.sqrt(sq(c0[0] + ox) + sq(c0[1] + oy) + sq(c0[2] + oz));
        final Lod.Tier tier = RenderQuality.tier(
            m,
            Math.max(16, s.throats[0].radius + 4),
            dist,
            MachineFx.BEAM_PROGRAM,
            MachineFx.MFX_PROGRAM,
            GlowSprite.PROGRAM);
        if (!tier.shaded()) return;
        final float time = RenderClock.seconds(base.getWorld(), partialTicks);
        GpuTimer.begin("conduit");
        final boolean glCheck = RenderQuality.glCheckBegin(m);
        final int[] noiseWas = NoiseTex.bind();
        final GlSave save = Layers.emissive();
        try {
            // the mouth's portal (premultiplied: it hides what is behind it)
            if (m instanceof MTEWormholeMouth && tier != Lod.Tier.L3) {
                final ConduitThroats.WorldThroat t = s.throats[0];
                final float in = m.renderIn(0) + m.renderOut(0);
                final float glow = !m.renderOpen() ? 0.15f
                    : (m.renderLinked() ? 0.9f : 0.45f) + (in > 0 ? 0.1f * (float) Math.log10(1 + in) : 0f);
                portal(t.centre[0] + ox, t.centre[1] + oy, t.centre[2] + oz, t.normal, t.radius - 1.2, time, glow);
            }
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            for (int i = 0; i < n; i++) {
                final ConduitThroats.WorldThroat t = s.throats[i];
                float out = m.renderOut(i);
                int kind = m.renderKind(i);
                double len = m.renderDist(i) > 0.5f ? m.renderDist(i) : ConduitNet.MAX_RAY;
                if (dbg != null && emits(m, i)) {
                    out = dbg[0];
                    len = trace(base, t);
                    kind = len < ConduitNet.MAX_RAY - 0.5 ? ELEMENT : NOTHING;
                }
                if (out <= 1e-3f) continue;
                for (int k = 0; k < 3; k++) {
                    A[k] = t.centre[k] + (k == 0 ? ox : k == 1 ? oy : oz);
                    B[k] = A[k] + t.normal[k] * len;
                }
                final float lg = (float) Math.log10(1 + out);
                stream(A, B, len, lg, kind == NOTHING ? (float) len * 0.6f : 1.5f, time, i * 2.3f + base.getXCoord());
                GlowSprite.draw(A[0], A[1], A[2], 2.4 + 0.5 * lg, 0.75f, 0.65f, 1f, 0.35f + 0.12f * lg, 0.2f, 0.5f);
                if (kind == ELEMENT || kind == RECEPTOR) {
                    GlowSprite.draw(B[0], B[1], B[2], 3.2 + 0.6 * lg, 0.8f, 0.7f, 1f, 0.4f + 0.12f * lg, 0.2f, 0.5f);
                }
            }
        } finally {
            save.restore();
            NoiseTex.unbind(noiseWas);
            RenderQuality.glCheckEnd(m, glCheck, "a conduit element");
            GpuTimer.end();
        }
    }

    /** Harness: whether throat {@code i} sends streams out (an out-throat, or a mouth's ring). */
    private static boolean emits(MTEConduitElement<?> m, int i) {
        final String role = m.throats()[i].role;
        return role.startsWith("out") || m instanceof MTEWormholeMouth && role.startsWith("throat");
    }

    /** Harness: the stream's length out of a throat: the first solid block along its normal in the client world. */
    private static double trace(IGregTechTileEntity base, ConduitThroats.WorldThroat t) {
        final net.minecraft.world.World w = base.getWorld();
        for (double d = 2.5; d < ConduitNet.MAX_RAY; d += 0.5) {
            final int bx = (int) Math.floor(t.centre[0] + t.normal[0] * d);
            final int by = (int) Math.floor(t.centre[1] + t.normal[1] * d);
            final int bz = (int) Math.floor(t.centre[2] + t.normal[2] * d);
            if (!w.isAirBlock(bx, by, bz)) return d;
        }
        return ConduitNet.MAX_RAY;
    }

    private static void stream(double[] a, double[] b, double len, float lg, float fadeOut, float time, float seed) {
        final MachineFx.Beam bm = BEAM;
        bm.w0 = 0.32f + 0.16f * lg;
        bm.w1 = bm.w0 * 1.15f;
        bm.intensity = Math.min(1.4f, 0.55f + 0.22f * lg);
        bm.speed = 18f;
        bm.time = time;
        bm.strand = 0.6f;
        bm.rate = 0.6f + 0.9f * lg;
        bm.fadeIn = 1.2f;
        bm.fadeOut = fadeOut;
        bm.seed = seed;
        bm.pitch = 5f;
        bm.core[0] = 0.86f;
        bm.core[1] = 0.92f;
        bm.core[2] = 1f;
        bm.sheath[0] = 0.55f;
        bm.sheath[1] = 0.38f;
        bm.sheath[2] = 1f;
        MachineFx.drawBeam(a, b, bm);
    }

    /** A portal disc of {@code radius} at camera-relative {@code (cx, cy, cz)} facing {@code n} (both sides). */
    private static void portal(double cx, double cy, double cz, double[] n, double radius, float time, float glow) {
        // a frame with the normal as local +y
        double ux = -n[2], uy = 0, uz = n[0];
        double ul = Math.sqrt(ux * ux + uz * uz);
        if (ul < 1e-4) {
            ux = 1;
            uz = 0;
            ul = 1;
        }
        ux /= ul;
        uz /= ul;
        final double vx = n[1] * uz - n[2] * uy, vy = n[2] * ux - n[0] * uz, vz = n[0] * uy - n[1] * ux;
        MODEL.set(
            (float) ux,
            (float) uy,
            (float) uz,
            0f,
            (float) n[0],
            (float) n[1],
            (float) n[2],
            0f,
            (float) vx,
            (float) vy,
            (float) vz,
            0f,
            (float) cx,
            (float) cy,
            (float) cz,
            1f);
        CAM[0] = -(cx * ux + cy * uy + cz * uz);
        CAM[1] = -(cx * n[0] + cy * n[1] + cz * n[2]);
        CAM[2] = -(cx * vx + cy * vy + cz * vz);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        MachineFx.drawPortal(MODEL, CAM, (float) radius, time, glow, 0.35f, 0.6f, 1f);
    }

    // =============================================================================== crowns

    /** GT's pass-0 hook: queues the draw for after the translucent pass ({@link DeferredDraws}). */
    public static void renderCrown(MTEBurstThermaliserCrown c, double x, double y, double z, float partialTicks) {
        if (Config.renderConduit && c.renderMk() > 0) DeferredDraws.add(() -> renderCrownNow(c, x, y, z, partialTicks));
    }

    private static void renderCrownNow(MTEBurstThermaliserCrown c, double x, double y, double z, float partialTicks) {
        if (!Config.renderConduit) return;
        final IGregTechTileEntity base = c.getBaseMetaTileEntity();
        final int mk = c.renderMk();
        if (base == null || mk <= 0) return;
        final ExtendedFacing f = c.getExtendedFacing();
        if (f == null) return;
        final float stream = c.renderStream();
        if (stream <= 1e-3f) return;
        final int[] in = QuasarGeometry.world(f, 0, 0, 0, MTEBurstThermaliserCrown.intakeRel(mk));
        final int[] ia = MTEBurstThermaliserCrown.intakeAxis(mk);
        final double[] ax = ConduitThroats.worldDir(f, new double[] { ia[0], ia[1], ia[2] });
        final double ix = x + in[0] + 0.5, iy = y + in[1] + 0.5, iz = z + in[2] + 0.5;
        final double dist = Math.sqrt(ix * ix + iy * iy + iz * iz);
        final Lod.Tier tier = RenderQuality
            .tier(c, 10, dist, MachineFx.BEAM_PROGRAM, MachineFx.MFX_PROGRAM, GlowSprite.PROGRAM);
        if (!tier.shaded()) return;
        final double cap = Math.max(1, BurstRouting.crownCap(mk));
        final float heat = (float) Math.min(1.0, Math.log10(1 + stream) / Math.log10(1 + cap));
        final float time = RenderClock.seconds(base.getWorld(), partialTicks);
        GpuTimer.begin("conduit");
        final boolean glCheck = RenderQuality.glCheckBegin(c);
        final int[] noiseWas = NoiseTex.bind();
        final GlSave save = Layers.emissive();
        try {
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            final float flick = 0.9f + 0.1f * (float) Math.sin(time * 9.0);
            // the heat column rising into the cup
            if (tier != Lod.Tier.L3) {
                A[0] = ix;
                A[1] = iy;
                A[2] = iz;
                final double h = Math.abs(MTEBurstThermaliserCrown.stackRel(mk)[1]) + 1;
                B[0] = ix + ax[0] * h;
                B[1] = iy + ax[1] * h;
                B[2] = iz + ax[2] * h;
                final MachineFx.Beam bm = BEAM;
                bm.w0 = 1.3f;
                bm.w1 = 3.2f;
                bm.intensity = 0.25f + 0.75f * heat;
                bm.speed = 3f;
                bm.time = time;
                bm.strand = 0.35f;
                bm.rate = 0.9f;
                bm.fadeIn = 0.6f;
                bm.fadeOut = (float) h * 0.7f;
                bm.seed = 3.3f;
                bm.pitch = 6f;
                bm.core[0] = 1f;
                bm.core[1] = 0.78f;
                bm.core[2] = 0.45f;
                bm.sheath[0] = 1f;
                bm.sheath[1] = 0.42f;
                bm.sheath[2] = 0.1f;
                MachineFx.drawBeam(A, B, bm);
            }
            // the thermal shimmer over the cup
            final double top = Math.abs(MTEBurstThermaliserCrown.stackRel(mk)[1]) + 1.5;
            GlowSprite.draw(
                ix + ax[0] * top,
                iy + ax[1] * top,
                iz + ax[2] * top,
                11 + 2 * mk,
                1f,
                0.42f,
                0.1f,
                (0.2f + 0.6f * heat) * flick,
                0.3f,
                0.6f);
            // the bloom: white at the throat, deep orange at the rim
            GlowSprite.draw(
                ix + ax[0] * 0.6,
                iy + ax[1] * 0.6,
                iz + ax[2] * 0.6,
                3.0,
                1f,
                0.95f,
                0.85f,
                (0.35f + 0.9f * heat) * flick,
                0.25f,
                0.5f);
            GlowSprite.draw(
                ix + ax[0] * 2.0,
                iy + ax[1] * 2.0,
                iz + ax[2] * 2.0,
                12,
                1f,
                0.42f,
                0.1f,
                (0.25f + 0.6f * heat) * flick,
                0.25f,
                0.55f);
        } finally {
            save.restore();
            NoiseTex.unbind(noiseWas);
            RenderQuality.glCheckEnd(c, glCheck, "a crown");
            GpuTimer.end();
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
