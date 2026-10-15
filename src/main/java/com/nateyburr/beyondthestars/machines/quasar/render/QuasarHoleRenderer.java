package com.nateyburr.beyondthestars.machines.quasar.render;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
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
import com.nateyburr.beyondthestars.client.render.NoiseTex;
import com.nateyburr.beyondthestars.client.render.RenderClock;
import com.nateyburr.beyondthestars.client.render.RenderQuality;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.BlackholeRenderer;

/**
 * The Quasar Engine's hole, accretion ring, jets and events (renders v2 §6, WP-2; P4 quasar-p4.md §1.6).
 * <p>
 * <b>Shader tiers (L0-L3):</b>
 * <ul>
 * <li><b>The hole</b>: the shared lensed black hole ({@link BlackHoleLens}) at GT's old size, with a hot inner disc
 * whose inner edge sits at the spin's ISCO (Bardeen): a spun-up hole hugs its shadow with a white-blue edge, a slow
 * one shows a dark gap. Its own LOD tier comes from its own size on screen.</li>
 * <li><b>The ring</b> (r 40 to 40 + 2 + 22 lambda_c, the voxel disc band): a ray-marched volume ({@code bts_qring}):
 * a flared turbulent slab with sheared Keplerian filaments, m = 2 / 3 spiral density waves, a bright hot inner wall
 * fading to a dusty, ragged, redder outer edge, Doppler beaming; the zone sets its temperature and turbulence
 * (Resonant ice-blue and laminar, Stable gold-white, Strained orange, Critical red and violent, Breach embers), E its
 * thickness, the Harmonic Lock standing ripples; re-forming: violet-red embers filling in from the inner wall.</li>
 * <li><b>The jets</b> ({@code bts_qjet}): a white-blue core, two helical strands (pitch from spin and flux), knots
 * launched as often as the port's Bu/t allows, a widening violet sheath and a terminal shock bow at y +-78; a base
 * glare at each pole.</li>
 * <li><b>The field lines</b> ({@code bts_qfx}): 12 dipole arcs round the hole's poles, brighter and tighter as the
 * flux builds, crackling above 90.</li>
 * <li><b>Events</b> (the last flash's kind, render-only): an eruption snaps the field lines, sends a noise-broken
 * shock shell out across the ring, flares the ring from its inner wall outward, fires a giant knot up both jets and
 * throws debris; a purge (blue-white, no shell) unwinds the field lines, runs a spiral flash up both jets and dims the
 * ring for two seconds; a blow-out breaks the ring into chunks flying outward; a re-formed ring / First Light kindles
 * the inner wall and fills outward.</li>
 * <li>From outside, the walls hide the ring (R-09): the jets and a zone-coloured glow over each cap carry the
 * look.</li>
 * <li><b>Supermassive</b> (SMBH v1, smbh-v1.md §7, R-19; a Supermassive Lattice in force): the hole keeps the log law
 * up to 4,096 SM and then a compressed one ({@link #drawScale}: x1.2 at 16,384 SM, x2.1 at 67 M SM, never past x2.6),
 * so every mass up to Unbound stays inside the inner wall (r 34) while its shadow and lensing grow; the ring runs
 * hotter, thicker and more violent with stronger Doppler beaming, the photon ring brighter; the jets fatter with
 * brighter knots. The 48 Containment Nodes glow in their tier's colour with a field arc from each into the ring
 * ({@link SmbhFx}), blinking amber while the hold-up is short. <b>Overdrive</b> (by its level): the ring and the jets
 * brighten toward white-violet, MHDCSM field lines spiral round the jets, the nodes flare and the arcs crackle.
 * <b>Unbound</b> (by its doublings n): a darker, wider lensing halo per doubling with a violet rim, the horizon
 * breathing +-3 % at 0.5 Hz, and violet anchor threads from node to node. <b>Intake</b>: while the Growth segment
 * runs, gas streams in across the ring (as wide as the Growth share) and infall streamers feed the inner disc. A
 * <b>shed</b> holds the eruption's flare up both jets.</li>
 * </ul>
 * Draw order and depth: the ring, the far jet and the far halves of the field lines and shell go first, then the hole
 * (it writes depth, and at L0 it bends the screen copy, ring and far jet included), then the near halves. Everything
 * but the hole is depth-tested without depth writes.
 * <p>
 * <b>Fixed-function tier</b> (quality low, shaders off or failed): today's look, unchanged: GT's own
 * {@link BlackholeRenderer} scaled about the centre, the Tessellator disc and the two jet cones.
 * <p>
 * Dev switch {@code -Dbts.render.qRepeat=n}: the v2 draw runs n times inside its timer section (GPU marginal cost;
 * the picture is wrong in such a run).
 */
@SideOnly(Side.CLIENT)
public class QuasarHoleRenderer extends TileEntitySpecialRenderer {

    private final BlackholeRenderer delegate = new BlackholeRenderer();

    /** Disc geometry, blocks from the centre (the voxel disc band). */
    static final float DISC_R0 = 40f, DISC_W = 22f;
    /** Jet reach, blocks along the axis. */
    static final float JET_LEN = 78f;
    /**
     * A shedding hole makes no power (its jets sync 0 Bu/t), but the shed look holds the eruption's flare up both jets:
     * they are drawn at this nominal stream while {@code JetLook.shed} is set (B-166).
     */
    private static final float SHED_JET_BU = 1000f;
    /** QA-B12: the shed flare's exposure lift, and its period (the shader's giant knot, every 1.4 s). */
    private static final float SHED_EXPOSURE = 1.6f, SHED_PERIOD = 1.4f;
    /**
     * QA-B12: each hole's last lit jet stream (client only): a shed draws its flare at the stream it replaced (at least
     * {@link #SHED_JET_BU}), never dimmer and calmer than the running jet was.
     */
    private final Map<TileQuasarHole, Float> lastJetBu = new WeakHashMap<>();

    private static final int SEGMENTS = 96;
    private static final int RINGS = 6;
    private static final int JET_SIDES = 16;
    private static final int JET_STACKS = 12;

    /** Zone colours (0 Resonant .. 4 Breach), RGB 0..1. */
    private static final float[][] ZONE_RGB = { { 0.75f, 0.92f, 1.00f }, { 1.00f, 0.82f, 0.48f },
        { 1.00f, 0.54f, 0.24f }, { 1.00f, 0.23f, 0.18f }, { 1.00f, 0.10f, 0.10f } };
    private static final float[] DARK_RGB = { 0.35f, 0.25f, 0.45f };

    // ---- renders v2

    /** GT's halo at GL scale 1 reached about 1.8 blocks: the inner disc's outer edge in blocks per unit scale. */
    static final double HALO_PER_SCALE = 1.8;
    /** The inner disc's outer edge, r_s. */
    static final float R_OUT = 8f;
    /** GT's opening / collapse animation, ticks. */
    private static final float GROW_TICKS = 40f;
    /** Per zone (0 Resonant .. 4 Breach, 5 re-forming): ring inner wall and body temperatures (K), turbulence. */
    private static final float[] T_WALL = { 30000f, 6200f, 3600f, 1900f, 1500f, 1900f };
    private static final float[] T_BODY = { 15000f, 3900f, 2500f, 1150f, 1000f, 1400f };
    private static final float[] TURB = { 0.12f, 0.35f, 0.62f, 1.0f, 1.0f, 0.5f };
    private static final float[] RING_EXPOSURE = { 1.35f, 0.85f, 0.9f, 0.8f, 0.7f, 0.6f };
    /** How long each event's effects run, s (index = event kind). */
    private static final float[] EVENT_SECONDS = { 0f, 4.5f, 3.5f, 7f, 4f, 4f };
    /** Ring march steps and noise octaves per tier L0..L3. */
    private static final float[][] RING_STEPS = { { 16, 3 }, { 12, 2 }, { 8, 2 }, { 6, 1 } };
    /** Jet end and cap glow height, blocks from the centre. */
    private static final float CAP_GLOW_Y = 84f;

    // ---- SMBH (R-19)
    /** The hole's scale at 4,096 SM, and the scale per doubling of mass, of {@code MTEQuasarEngine.renderScaleFor}. */
    static final float S_REF = (4f + 4f * 6f / 6f) / 1.3f, S_PER_DOUBLING = 4f / 6f / 1.3f;
    /** The lattice's reach (the equatorial nodes at r 89 and the arcs and threads just outside), blocks. */
    static final float LATTICE_R = 104f;
    /** The Containment Nodes' colours per lattice tier L1..L4 (the node textures' glow colours). */
    private static final float[][] TIER_RGB = { { 0.42f, 0.91f, 0.82f }, { 0.86f, 0.92f, 1.00f },
        { 0.60f, 0.46f, 1.00f }, { 1.00f, 0.32f, 0.78f } };

    /** Dev only: the v2 draw repeated n times (GPU marginal cost). */
    private static final int REPEAT = Math.max(1, Integer.getInteger("bts.render.qRepeat", 1));

    private final BlackHoleLens.Params hole = new BlackHoleLens.Params();
    /** The programs the v2 quasar draws with (one array: a frame allocates nothing, R-16). */
    private static final BTSShaders.Program[] PROGRAMS = { QuasarFx.RING_PROGRAM, QuasarFx.JET_PROGRAM,
        QuasarFx.FX_PROGRAM, BlackHoleLens.PROGRAM, GlowSprite.PROGRAM };
    /** The same with the Supermassive Lattice's program (a lattice quasar falls back whole when it is off). */
    private static final BTSShaders.Program[] PROGRAMS_SMBH = { QuasarFx.RING_PROGRAM, QuasarFx.JET_PROGRAM,
        QuasarFx.FX_PROGRAM, BlackHoleLens.PROGRAM, GlowSprite.PROGRAM, SmbhFx.PROGRAM };
    private final QuasarFx.RingLook ring = new QuasarFx.RingLook();
    private final QuasarFx.JetLook jet = new QuasarFx.JetLook();
    private final QuasarFx.FieldLook field = new QuasarFx.FieldLook();
    private final SmbhFx.LatticeLook lattice = new SmbhFx.LatticeLook();
    private final SmbhFx.StreamLook streams = new SmbhFx.StreamLook();
    /** The hole's own LOD tier per quasar (hysteresis memory; the quasar's tier is in RenderQuality). */
    private final Map<TileQuasarHole, Lod.Tier> holeTiers = new WeakHashMap<>();
    private static final FloatBuffer M16 = BufferUtils.createFloatBuffer(16);
    private final double[] right = new double[3], up = new double[3], fwd = new double[3];

    /** The delegate binds its textures through the dispatcher, so it gets the same one. */
    @Override
    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        super.func_147497_a(dispatcher);
        delegate.func_147497_a(dispatcher);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!Config.renderQuasar || !(tile instanceof TileQuasarHole) || RenderQuality.hidden()) return;
        final TileQuasarHole q = (TileQuasarHole) tile;
        final float s = drawScale(q.getHoleScale());
        final double cx = x + 0.5;
        final double cy = y + 0.5;
        final double cz = z + 0.5;
        // the whole quasar: the ring reaches r 62, a lattice's nodes r 89 (TileQuasarHole's render box)
        final boolean smbh = q.getLattice() > 0;
        final Lod.Tier tier = RenderQuality.tier(
            tile,
            smbh ? LATTICE_R : DISC_R0 + DISC_W,
            Math.sqrt(cx * cx + cy * cy + cz * cz),
            smbh ? PROGRAMS_SMBH : PROGRAMS);
        GpuTimer.begin("quasar");
        final boolean glCheck = RenderQuality.glCheckBegin(tile);
        try {
            if (tier.shaded()) {
                boolean drawn = false;
                for (int i = 0; i < REPEAT; i++) drawn = drawV2(q, s, cx, cy, cz, tier, partialTicks);
                if (drawn) return;
            }
            drawFixed(q, tile, x, y, z, cx, cy, cz, s, partialTicks);
        } finally {
            RenderQuality.glCheckEnd(tile, glCheck, "the quasar");
            GpuTimer.end();
        }
    }

    // ============================================================================== renders v2

    /** The v2 quasar; false if a program is off (then the caller draws the fixed path). */
    private boolean drawV2(TileQuasarHole q, float s, double cx, double cy, double cz, Lod.Tier tier,
        float partialTicks) {
        final World world = q.getWorldObj();
        final float time = RenderClock.seconds(world, partialTicks);
        final double now = (world == null ? 0 : world.getTotalWorldTime()) + partialTicks;
        final double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        cameraAxes();

        // ---- the state
        final int zone = q.getZone();
        final int zi = zone < 0 ? 5 : Math.min(zone, 4);
        final float lc = clamp01(q.getLambdaC());
        final float e = q.getE();
        final float a = Math.max(0f, Math.min(0.998f, q.getSpin()));
        final float phi = Math.max(0f, Math.min(100f, q.getPhi()));
        final int ev = q.getEvent();
        final double flashAge = q.flashAge(now);
        final float evAge = (float) (flashAge / 20.0);
        final boolean live = ev > 0 && ev < EVENT_SECONDS.length && flashAge >= 0 && evAge < EVENT_SECONDS[ev];
        final int event = live ? ev : 0;
        final float evSize = clamp01(q.getFlashSize());
        // SMBH (R-19): the lattice in force, its Overdrive, Unbound, intake and shed; sm 0..1 how supermassive (0 at
        // 4,096 SM, 1 at the L4 top)
        final int lt = Math.max(0, Math.min(4, q.getLattice()));
        final boolean smbh = lt > 0;
        final float sm = smbh ? clamp01(doublings(q.getHoleScale()) / 14f) : 0f;
        final float od = smbh ? clamp01(q.getOverdrive()) : 0f;
        final float un = smbh ? Math.max(0f, Math.min(6f, q.getUnboundN())) : 0f;
        final float gr = smbh && zi < 5 ? clamp01(q.getGrowth()) : 0f;
        // QA-B12: any shed shows (a coil-downgrade shed has no lattice in force)
        final boolean shed = q.getShed() && zi < 5;
        final float leak = smbh && zi < 5 ? clamp01((1f - q.getHold()) * 4f) : 0f;

        // ---- the hole (GT's growth curve, size as GT's halo)
        final float age = (float) (now - q.getStartTime());
        final boolean opening = q.getScaling();
        final float growth = growth(opening, age);
        // Unbound: the horizon breathes, +-3 % at 0.5 Hz
        final float breathe = un > 0 ? (float) Math.sin(Math.PI * time) : 0f;
        final double rsFull = s * HALO_PER_SCALE / R_OUT * (1 + 0.03 * breathe * Math.min(1f, un));
        final double rs = rsFull * Math.max(growth, 0.02f);
        holeLook(hole, zi, a, event, evAge, evSize, time);
        if (smbh) smbhHole(hole, zi, sm, od, gr);
        if (shed) hole.exposure *= 1.3f; // QA-B12: the held flare lights the disc too
        hole.cx = cx;
        hole.cy = cy;
        hole.cz = cz;
        hole.rsBlocks = rs;
        final double toRs = rsFull / rs;
        hole.openIntensity = hole.hawkIntensity = 0f;
        if (opening && age >= 0 && age < 25) {
            final float f = age / 25f;
            hole.openRadius = (float) (Math.pow(f, 0.6) * R_OUT * toRs);
            hole.openIntensity = 1.6f * (1 - f) * (1 - f);
        } else if (!opening && age >= 24 && age < GROW_TICKS) {
            final float f = (age - 24) / (GROW_TICKS - 24);
            hole.hawkRadius = (float) (f * R_OUT * 0.9 * toRs);
            hole.hawkIntensity = 2.2f * (1 - f);
        }
        hole.stars = world == null ? 0f : Math.min(1f, world.getStarBrightness(partialTicks) * 2f);
        final double holeBound = rsFull * R_OUT * BlackHoleLens.BOUND_PER_OUTER;
        final Lod.Tier holeSize = Lod.tierFor(Lod.projectedRadius(holeBound, dist), holeTiers.get(q));
        holeTiers.put(q, holeSize);
        final Lod.Tier holeTier = holeSize.ordinal() > tier.ordinal() ? holeSize : tier;

        // ---- the ring
        // a blow-out breaks up the ring that was there (the zone is already dark): Critical colours, full width
        final boolean blowing = event == TileQuasarHole.EV_BLOWOUT && evAge < 5.5f;
        ringLook(
            ring,
            blowing ? 3 : zi,
            blowing ? 0.9f : lc,
            e,
            q.getLock(),
            q.getReform(),
            event,
            evAge,
            evSize,
            time,
            tier);
        if (smbh) smbhRing(ring, zi, sm, od);
        final double camR = Math.sqrt(cx * cx + cz * cz);
        ring.inside = Math.abs(cy) < ring.hp && camR > ring.r0p && camR < ring.r1p;
        if (ring.inside) {
            for (int i = 0; i < 3; i++) {
                ring.nearC[i] = (float) (fwd[i] * 0.2);
                ring.nearU[i] = (float) right[i];
                ring.nearV[i] = (float) up[i];
            }
        }

        // ---- the jets: brightness, knots, helix
        final float pitch = (14f - 8f * a) * (1f - 0.35f * phi / 100f);
        final float z0 = (float) (rs * 2.8);
        float topBu = q.getJetTop(), bottomBu = q.getJetBottom();
        if (topBu > 0 || bottomBu > 0) lastJetBu.put(q, Math.max(topBu, bottomBu));
        // QA-B12: a shedding hole's jets sync 0 Bu/t; its flare is drawn at the stream it replaced
        final float shedBu = shed ? Math.max(SHED_JET_BU, lastJetBu.getOrDefault(q, 0f)) : 0f;
        final float shedPhase = (time % SHED_PERIOD) / SHED_PERIOD;
        if (shed) {
            if (topBu <= 0) topBu = shedBu;
            if (bottomBu <= 0) bottomBu = shedBu;
        }
        // the ribbon's across vector: horizontal, perpendicular to the view of the axis
        double sx = -cz, sz = cx;
        final double sl = Math.sqrt(sx * sx + sz * sz);
        if (sl < 1e-4) {
            sx = 1;
            sz = 0;
        } else {
            sx /= sl;
            sz /= sl;
        }
        // the jet pointing away from the camera is drawn before the hole (the screen copy bends it)
        final int farDir = cy < 0 ? -1 : 1;
        // SMBH: fatter with mass, white-violet in Overdrive, the held flare of a shed
        jet.overdrive = od;
        jet.width = (1f + 0.9f * sm) * (shed ? 1.5f : 1f); // QA-B12: the held flare is fatter than the jet it replaces
        jet.shed = shed ? 1f : 0f;
        jet.supermassive = sm;

        // ---- field lines
        fieldLook(field, rs, phi, zi, event, evAge, time, dist);

        // every layer's program and mesh first: a hole that cannot draw after the ring and jets are down would put
        // the fixed path on top of them (review of R-16)
        if (growth > 0.003f && !BlackHoleLens.usable()) return false;
        final int[] noiseWas = NoiseTex.bind();
        try {
            // 1. the ring (premultiplied over, no depth writes)
            GlSave save = premultiplied();
            try {
                if (!QuasarFx.drawRing(cx, cy, cz, ring)) return false;
            } finally {
                save.restore();
            }
            // 2. what lies behind the hole
            save = Layers.emissive();
            try {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                drawJet(
                    cx,
                    cy,
                    cz,
                    farDir > 0 ? topBu : bottomBu,
                    farDir,
                    z0,
                    pitch,
                    a,
                    event,
                    evAge,
                    evSize,
                    time,
                    (float) sx,
                    (float) sz);
                if (tier.ordinal() <= Lod.Tier.L2.ordinal()) QuasarFx.drawField(cx, cy, cz, field, -1f);
                drawShell(cx, cy, cz, rs, event, evAge, evSize, time, GL11.GL_FRONT);
                if (shed && event == 0) drawShedShell(cx, cy, cz, rs, shedPhase, time, GL11.GL_FRONT);
                // SMBH: the intake streaming in across the ring
                if (gr > 0.002f && tier.ordinal() <= Lod.Tier.L1.ordinal()) drawStreams(cx, cy, cz, gr, ring.h0, time);
            } finally {
                save.restore();
            }
            // 2b. SMBH, Unbound: the darker lensing halo under the hole (premultiplied: it darkens what lies behind)
            if (un > 0.01f && growth > 0.003f && tier.ordinal() <= Lod.Tier.L2.ordinal()) {
                save = premultiplied();
                try {
                    final float halo = (float) Math.min(33.0, rs * (4.2 + 2.6 * un));
                    SmbhFx.drawHalo(
                        cx,
                        cy,
                        cz,
                        halo,
                        (float) (2.6 * rs / halo),
                        Math.min(0.92f, 0.38f + 0.2f * un),
                        Math.min(1.2f, 0.35f + 0.3f * un),
                        breathe,
                        time);
                } finally {
                    save.restore();
                }
            }
            // 3. the hole (writes depth where it has a body; at L0 bends the screen copy)
            if (growth > 0.003f && !BlackHoleLens.draw(hole, holeTier)) return false;
            // 4. what lies in front of it
            save = Layers.emissive();
            try {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                drawJet(
                    cx,
                    cy,
                    cz,
                    farDir > 0 ? bottomBu : topBu,
                    -farDir,
                    z0,
                    pitch,
                    a,
                    event,
                    evAge,
                    evSize,
                    time,
                    (float) sx,
                    (float) sz);
                if (tier.ordinal() <= Lod.Tier.L2.ordinal()) QuasarFx.drawField(cx, cy, cz, field, 1f);
                drawShell(cx, cy, cz, rs, event, evAge, evSize, time, GL11.GL_BACK);
                if (shed && event == 0) drawShedShell(cx, cy, cz, rs, shedPhase, time, GL11.GL_BACK);
                GL11.glDisable(GL11.GL_CULL_FACE);
                if (RenderQuality.particles()
                    && (event == TileQuasarHole.EV_ERUPTION || event == TileQuasarHole.EV_BLOWOUT)) {
                    final boolean blow = event == TileQuasarHole.EV_BLOWOUT;
                    QuasarFx.drawDebris(
                        cx,
                        cy,
                        cz,
                        event,
                        evAge,
                        blow ? 0.8f : 0.45f,
                        ring.r0,
                        ring.r1,
                        blow ? 0.9f : 1.0f,
                        time,
                        right,
                        up);
                }
                drawGlows(q, cx, cy, cz, zi, z0, topBu, bottomBu, rs, tier, event, evAge, shed ? shedPhase : -1f);
                // SMBH: the Containment Nodes, their field arcs and (Unbound) the anchor threads
                if (smbh && tier.ordinal() <= Lod.Tier.L2.ordinal()) {
                    drawLattice(cx, cy, cz, lt, zi, od, un, leak, ring.r1, tier, time);
                }
            } finally {
                save.restore();
            }
        } finally {
            NoiseTex.unbind(noiseWas);
        }
        return true;
    }

    /** The inner disc's look from the zone, the spin and the event. */
    private static void holeLook(BlackHoleLens.Params p, int zi, float a, int event, float evAge, float evSize,
        float time) {
        p.rIn = (float) Math.max(1.12, Math.min(3.0, iscoRg(a) / 2.0));
        p.rOut = R_OUT;
        p.temperature = zi == 5 ? 3600f : 8500f + 9500f * a;
        p.turbulence = 0.2f + 0.6f * TURB[zi];
        p.thickness = 0.02f + 0.03f * TURB[zi];
        p.streamers = zi == 3 ? 0.35f : zi == 4 ? 0.7f : 0f;
        float expo = zi == 5 ? 0.3f : 1.25f;
        if (event == TileQuasarHole.EV_ERUPTION) expo *= 1f + (1f + 1.5f * evSize) * (float) Math.exp(-evAge / 0.8);
        if (event == TileQuasarHole.EV_PURGE) expo *= 1f + 0.6f * (float) Math.exp(-evAge / 0.5);
        p.exposure = expo;
        p.beaming = 2.5f;
        p.ring = 2.5f;
        p.glow = 0.08f;
        p.drag = 0.42f * a;
        p.tilt = 0f;
        p.tiltAzimuth = 0f;
        p.time = time;
    }

    // ============================================================================== SMBH (R-19)

    /** Doublings of mass past 4,096 SM from the synced hole scale (0 at or under it). */
    static float doublings(float s) {
        return Math.max(0f, (s - S_REF) / S_PER_DOUBLING);
    }

    /**
     * The scale the hole is drawn at: the synced log law up to 4,096 SM, then compressed so every supermassive mass
     * stays inside the inner Eddington wall (r 34): x1.2 at 16,384 SM, x1.9 at 4.2 M, x2.1 at the L4 top (67 M SM),
     * x2.6 at most (2^50 SM). Both paths draw it (the fixed path's GT hole too).
     */
    static float drawScale(float s) {
        if (s <= S_REF) return s;
        return (float) (S_REF * (1 + 1.6 * Math.tanh(doublings(s) / 16.0)));
    }

    /** A supermassive hole: hotter, a brighter photon ring and glow; infall streamers while the intake grows it. */
    private static void smbhHole(BlackHoleLens.Params p, int zi, float sm, float od, float gr) {
        if (zi == 5) return;
        // a little hotter with mass (it stays coloured); Overdrive drives it to blue-white
        p.temperature *= 1f + 0.12f * sm + 0.9f * od;
        p.exposure *= 1f + 0.1f * sm + 0.45f * od;
        p.turbulence = Math.min(1f, p.turbulence + 0.25f * sm);
        p.ring = 2.5f + 1.8f * sm;
        p.glow = 0.08f + 0.06f * sm + 0.05f * od;
        p.beaming = 2.5f + 0.8f * sm;
        p.streamers = Math.max(p.streamers, 0.65f * (float) Math.sqrt(gr));
    }

    /** A supermassive ring: hotter, thicker, more violent, stronger beaming; Overdrive drives it toward white. */
    private static void smbhRing(QuasarFx.RingLook r, int zi, float sm, float od) {
        if (zi == 5) return;
        final float lift = 0.35f + 0.65f * sm;
        r.tWall *= 1f + 0.3f * lift;
        r.tBody *= 1f + 0.3f * lift;
        r.turbulence = Math.min(1f, r.turbulence + 0.3f * lift + 0.2f * od);
        // thicker, but no brighter for it (the column grows with the thickness): the light stays coloured
        final float thick = 1f + 0.45f * lift;
        r.h0 *= thick;
        r.hp = 3.6f * r.h0 + 0.3f;
        r.beta = 0.11f + 0.13f * lift;
        r.exposure /= thick;
        if (od > 0) {
            r.tWall += (40000f - r.tWall) * 0.6f * od;
            r.tBody += (22000f - r.tBody) * 0.45f * od;
            r.exposure *= 1f + 0.45f * od;
        }
    }

    /** The intake streams across the ring (outer wall to inner wall), as wide as the Growth share. */
    private void drawStreams(double cx, double cy, double cz, float gr, float h0, float time) {
        final SmbhFx.StreamLook st = streams;
        st.rOut = 63.5f;
        st.rIn = DISC_R0 - 1f;
        st.width = 0.4f + 1.5f * (float) Math.sqrt(gr);
        st.turn = 0.95f;
        st.halfHeight = h0 * 2.4f;
        st.intensity = 0.35f + 1.1f * (float) Math.sqrt(gr);
        st.pixel = (float) (1.0 / Math.max(1e-3, Lod.pixelsPerBlockAtUnit()));
        st.time = time;
        SmbhFx.drawStreams(cx, cy, cz, st);
    }

    /** The Containment Nodes in their tier's colour, their field arcs into the ring, and Unbound's anchor threads. */
    private void drawLattice(double cx, double cy, double cz, int lt, int zi, float od, float un, float leak,
        float ringOuter, Lod.Tier tier, float time) {
        final SmbhFx.LatticeLook l = lattice;
        final float[] c = TIER_RGB[lt - 1];
        final boolean dark = zi == 5;
        l.r = c[0];
        l.g = c[1];
        l.b = c[2];
        l.ringOuter = ringOuter;
        l.nodeSize = 5f;
        l.arcWidth = 0.32f;
        l.threadWidth = un > 0.01f && tier.ordinal() <= Lod.Tier.L1.ordinal() ? 0.28f : 0f;
        l.threads = Math.min(1f, 0.3f + 0.35f * un);
        l.flare = od;
        l.leak = leak;
        l.intensity = dark ? 0.4f : 1.1f;
        // the arcs only while the disc burns, and only close enough to read (L0 / L1)
        l.arcIntensity = dark || tier.ordinal() > Lod.Tier.L1.ordinal() ? 0f : 0.55f;
        l.pixel = (float) (1.0 / Math.max(1e-3, Lod.pixelsPerBlockAtUnit()));
        l.time = time;
        SmbhFx.drawLattice(cx, cy, cz, l, right, up);
    }

    /** Bardeen's prograde ISCO in r_g (6 at a = 0, 1.24 at a = 0.998). */
    static double iscoRg(double a) {
        final double z1 = 1 + Math.cbrt(1 - a * a) * (Math.cbrt(1 + a) + Math.cbrt(1 - a));
        final double z2 = Math.sqrt(3 * a * a + z1 * z1);
        return 3 + z2 - Math.sqrt((3 - z1) * (3 + z1 + 2 * z2));
    }

    /** The ring's look from the zone, lambda_c, E, the lock, re-forming and the event. */
    private static void ringLook(QuasarFx.RingLook r, int zi, float lc, float e, boolean lock, float reform, int event,
        float evAge, float evSize, float time, Lod.Tier tier) {
        r.r0 = DISC_R0;
        r.r1 = DISC_R0 + 2f + DISC_W * (zi == 5 ? 0.75f : lc);
        r.h0 = 1.2f + 0.4f * Math.max(0f, Math.min(4f, e / 25f)) - 0.3f * clamp01(-e / 60f);
        r.r0p = r.r0 - 1.5f;
        r.r1p = r.r1 + 5f;
        r.hp = 3.6f * r.h0 + 0.3f;
        r.omega = 0.1f;
        r.time = time;
        r.tWall = T_WALL[zi];
        r.tBody = T_BODY[zi];
        r.turbulence = TURB[zi];
        r.exposure = RING_EXPOSURE[zi];
        r.lock = lock && zi == 0 ? 1f : 0f;
        r.embers = zi == 5 ? 1f : 0f;
        r.reform = zi == 5 ? clamp01(reform) : 0f;
        r.beta = 0.11f;
        r.event = event;
        r.eventAge = evAge;
        r.eventSize = evSize;
        final float[] st = RING_STEPS[Math.min(3, tier.ordinal())];
        r.steps = st[0];
        r.octaves = st[1];
        r.pixel = (float) (1.0 / Math.max(1e-3, Lod.pixelsPerBlockAtUnit()));
    }

    /** The field lines from the flux and the event. */
    private static void fieldLook(QuasarFx.FieldLook f, double rs, float phi, int zi, int event, float evAge,
        float time, double dist) {
        final float fr = phi / 100f;
        final double rOut = rs * R_OUT;
        f.size = (float) (rOut * (2.3 - 0.8 * fr));
        f.start = (float) (rs * 1.7);
        float twist = 0.35f + 2.0f * fr;
        float intensity = zi == 5 ? 0.04f : 0.10f + 0.9f * fr * fr;
        f.snap = 0f;
        if (event == TileQuasarHole.EV_ERUPTION) {
            f.snap = clamp01(evAge / 1.2f);
            intensity = Math.max(intensity, 1.0f);
        } else if (event == TileQuasarHole.EV_PURGE && evAge < 1.6f) {
            final float u = smooth(evAge / 1.2f);
            twist *= 1f - 2f * u;
            intensity = Math.max(intensity, 0.9f * (1f - smooth((evAge - 0.6f) / 1.0f)));
        }
        f.twist = twist;
        f.intensity = intensity;
        f.crackle = clamp01((phi - 90f) / 10f);
        // at least about a pixel and a half wide
        final double px = dist / Math.max(1e-3, Lod.pixelsPerBlockAtUnit());
        f.width = (float) Math.max(0.025 * f.size, 1.5 * px);
        f.time = time;
    }

    private void drawJet(double cx, double cy, double cz, float bu, int dir, float z0, float pitch, float a, int event,
        float evAge, float evSize, float time, float sx, float sz) {
        final QuasarFx.JetLook j = jet;
        if (bu <= 0) {
            if (j.shed <= 0) return;
            bu = SHED_JET_BU;
        }
        j.bright = Math.max(0.08f, Math.min(1f, (float) (0.35 + 0.2 * Math.log10(bu))));
        j.knotRate = (float) (0.15 + 0.3 * Math.log10(1 + bu));
        j.pitch = pitch;
        j.time = time;
        j.z0 = z0;
        j.zEnd = JET_LEN;
        j.seed = dir > 0 ? 1f : 2f;
        j.exposure = j.shed > 0 ? SHED_EXPOSURE : 1f;
        final boolean jetEvent = event == TileQuasarHole.EV_ERUPTION || event == TileQuasarHole.EV_PURGE;
        j.event = jetEvent ? event : 0;
        j.eventAge = evAge;
        j.eventSize = evSize;
        j.dir = dir;
        // a pixel's width half-way up the jet
        final double mx = cx, my = cy + dir * JET_LEN * 0.4, mz = cz;
        j.pixel = (float) (Math.sqrt(mx * mx + my * my + mz * mz) / Math.max(1e-3, Lod.pixelsPerBlockAtUnit()));
        QuasarFx.drawJet(cx, cy, cz, j, sx, sz);
    }

    /** The eruption's (and a blow-out's) shock shell; {@code cullFace} picks its far or near half. */
    private static void drawShell(double cx, double cy, double cz, double rs, int event, float evAge, float evSize,
        float time, int cullFace) {
        final float radius, intensity, r, g, b;
        if (event == TileQuasarHole.EV_ERUPTION && evAge < 4f) {
            radius = (float) (rs * 3 + 85 * (1 - Math.exp(-evAge / 1.5)));
            intensity = (0.35f + 0.6f * evSize) * (1f - smooth((evAge - 0.5f) / 3.5f))
                * (1f - 0.7f * smooth((radius - 50f) / 35f));
            r = 1.0f;
            g = 0.80f;
            b = 0.58f;
        } else if (event == TileQuasarHole.EV_BLOWOUT && evAge < 3f) {
            radius = (float) (DISC_R0 * 0.6 + 50 * (1 - Math.exp(-evAge / 1.1)));
            intensity = 0.55f * (1f - smooth(evAge / 2.8f));
            r = 1.0f;
            g = 0.42f;
            b = 0.25f;
        } else {
            return;
        }
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(cullFace);
        QuasarFx.drawShell(cx, cy, cz, radius, intensity, time, r, g, b, 4.5f);
    }

    /**
     * QA-B12: while shedding, a weak shock pulse in phase with each giant knot (the eruption's shell, a fifth of its
     * reach and a third of its light), so a shed reads from far outside.
     */
    private static void drawShedShell(double cx, double cy, double cz, double rs, float phase, float time,
        int cullFace) {
        final float radius = (float) (rs * 3 + 26 * (1 - Math.exp(-phase * SHED_PERIOD / 0.6)));
        final float intensity = 0.3f * (1f - smooth(phase));
        if (intensity <= 0.005f) return;
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(cullFace);
        QuasarFx.drawShell(cx, cy, cz, radius, intensity, time, 1.0f, 0.86f, 0.70f, 4.5f);
    }

    /** The jet base glares, the cap glows (the outside look, R-09) and, small on screen, the whole quasar's glow. */
    private static void drawGlows(TileQuasarHole q, double cx, double cy, double cz, int zi, float z0, float topBu,
        float bottomBu, double rs, Lod.Tier tier, int event, float evAge, float shedPhase) {
        for (int dir = -1; dir <= 1; dir += 2) {
            final float bu = dir > 0 ? topBu : bottomBu;
            if (bu <= 0) continue;
            final float bright = Math.max(0.08f, Math.min(1f, (float) (0.35 + 0.2 * Math.log10(bu))));
            float pulse = 1f;
            if (event == TileQuasarHole.EV_PURGE) pulse += 1.5f * (float) Math.exp(-evAge / 0.5);
            // QA-B12: a shed's glare is bigger and pulses with each giant knot leaving the base (what reads at range)
            double size = 1.0 + rs * 1.1;
            if (shedPhase >= 0) {
                pulse *= 1.6f + 1.4f * (float) Math.exp(-shedPhase * SHED_PERIOD / 0.35);
                size *= 2.2;
            }
            GlowSprite.draw(cx, cy + dir * z0, cz, size, 0.80f, 0.90f, 1.0f, 0.6f * bright * pulse, 0.16f, 0.4f);
        }
        final float[] c = zoneGlow(zi);
        final float capI = zi == 5 ? 0.18f : 0.30f;
        for (int dir = -1; dir <= 1; dir += 2) {
            GlowSprite.draw(cx, cy + dir * CAP_GLOW_Y, cz, 18, c[0], c[1], c[2], capI, 0.22f, 0.5f);
        }
        if (tier.ordinal() >= Lod.Tier.L2.ordinal()) {
            GlowSprite.draw(cx, cy, cz, DISC_R0 + DISC_W, c[0], c[1], c[2], 0.35f, 0.3f, 0.6f);
        }
    }

    /** A zone's glow colour (the ring's inner-wall temperature; re-forming violet). */
    private static float[] zoneGlow(int zi) {
        return ZONE_GLOW[zi];
    }

    /** Each zone's glow colour, worked out once (R-16: a frame allocates nothing). */
    private static final float[][] ZONE_GLOW = new float[6][];

    static {
        for (int zi = 0; zi < 5; zi++) ZONE_GLOW[zi] = Blackbody.rgb(T_WALL[zi] * 0.8f);
        ZONE_GLOW[5] = new float[] { 0.55f, 0.22f, 0.70f };
    }

    /** The premultiplied-over state for the ring (depth test, no depth writes, lightmap full, no fog). */
    private static GlSave premultiplied() {
        final GlSave s = Layers.emissive();
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        return s;
    }

    /** The camera's right, up and forward axes (camera-relative world), from the TESR's modelview. */
    private void cameraAxes() {
        M16.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, M16);
        right[0] = M16.get(0);
        right[1] = M16.get(4);
        right[2] = M16.get(8);
        up[0] = M16.get(1);
        up[1] = M16.get(5);
        up[2] = M16.get(9);
        fwd[0] = -M16.get(2);
        fwd[1] = -M16.get(6);
        fwd[2] = -M16.get(10);
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

    private static float clamp01(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    private static float smooth(float x) {
        final float t = clamp01(x);
        return t * t * (3f - 2f * t);
    }

    // ============================================================================== fixed function (LOW)

    /** Today's look (renders v2: the LOW / fallback path, unchanged): GT's hole, the Tessellator disc and cones. */
    private void drawFixed(TileQuasarHole q, TileEntity tile, double x, double y, double z, double cx, double cy,
        double cz, float s, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(cx, cy, cz);
        GL11.glScalef(s, s, s);
        GL11.glTranslated(-cx, -cy, -cz);
        delegate.renderTileEntityAt(tile, x, y, z, partialTicks);
        GL11.glPopMatrix();

        final long now = tile.getWorldObj() == null ? 0
            : tile.getWorldObj()
                .getTotalWorldTime();
        final float t = now + partialTicks;
        GL11.glPushMatrix();
        final GlSave save = GlSave.capture();
        GL11.glTranslated(cx, cy, cz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        try {
            drawDisc(q, t);
            // QA-B12: a shedding hole's jets (0 Bu/t) still show the shed's flare on the LOW path
            final boolean shed = q.getShed() && q.getZone() >= 0;
            final float pulse = shed ? 1.25f + 0.35f * (float) Math.exp(-((t / 20f) % SHED_PERIOD) / 0.35) : 1f;
            drawJet(q.getJetTop() > 0 || !shed ? q.getJetTop() : SHED_JET_BU, 1, t, pulse);
            drawJet(q.getJetBottom() > 0 || !shed ? q.getJetBottom() : SHED_JET_BU, -1, t, pulse);
        } finally {
            save.restore();
            GL11.glPopMatrix();
        }
    }

    /** The accretion disc: an additive annulus with three turning spiral arms. */
    private static void drawDisc(TileQuasarHole q, float t) {
        final float lc = Math.max(0, Math.min(1, q.getLambdaC()));
        final int zone = q.getZone();
        final float[] rgb = zone >= 0 && zone < ZONE_RGB.length ? ZONE_RGB[zone] : DARK_RGB;
        final float width = zone < 0 ? 4f : 2f + DISC_W * lc;
        final float r0 = DISC_R0, r1 = DISC_R0 + width;
        // E above 0 puffs the disc up: the two faces part by up to 4 blocks
        final float puff = 0.3f + Math.max(0, Math.min(4f, q.getE() / 25f));
        // the eruption flash: brighter for 3 s, fading
        final float since = q.flashAge(t);
        // (the render-only blow-out / re-formed / First Light flashes of renders v2 do not flash this look)
        final boolean shows = q.getEvent() <= TileQuasarHole.EV_PURGE;
        final float flash = shows && since >= 0 && since < 60 ? (1f - since / 60f) * (0.6f + q.getFlashSize()) : 0f;
        final float base = (zone < 0 ? 0.12f : 0.35f) * (1f + 1.5f * flash);
        final float spin = t * 0.004f;
        final Tessellator tess = Tessellator.instance;
        for (int face = -1; face <= 1; face += 2) {
            final float yy = face * puff * 0.5f;
            tess.startDrawingQuads();
            for (int ring = 0; ring < RINGS; ring++) {
                final float fa = ring / (float) RINGS, fb = (ring + 1) / (float) RINGS;
                final float ra = r0 + (r1 - r0) * fa, rb = r0 + (r1 - r0) * fb;
                // brightest at the inner edge, gone at the outer
                final float aa = base * (1f - fa) * (1f - fa), ab = base * (1f - fb) * (1f - fb);
                for (int seg = 0; seg < SEGMENTS; seg++) {
                    final double t0 = seg * 2 * Math.PI / SEGMENTS, t1 = (seg + 1) * 2 * Math.PI / SEGMENTS;
                    final float arm0 = arm(t0, ra, spin), arm1 = arm(t1, ra, spin);
                    final float arm2 = arm(t1, rb, spin), arm3 = arm(t0, rb, spin);
                    vertex(tess, rgb, aa * arm0, ra * Math.cos(t0), yy, ra * Math.sin(t0));
                    vertex(tess, rgb, aa * arm1, ra * Math.cos(t1), yy, ra * Math.sin(t1));
                    vertex(tess, rgb, ab * arm2, rb * Math.cos(t1), yy, rb * Math.sin(t1));
                    vertex(tess, rgb, ab * arm3, rb * Math.cos(t0), yy, rb * Math.sin(t0));
                }
            }
            tess.draw();
        }
        // the flash ring, running outward over the disc
        if (flash > 0) {
            final float rr = r0 + (r1 - r0 + 8f) * Math.min(1f, since / 60f);
            tess.startDrawingQuads();
            for (int seg = 0; seg < SEGMENTS; seg++) {
                final double t0 = seg * 2 * Math.PI / SEGMENTS, t1 = (seg + 1) * 2 * Math.PI / SEGMENTS;
                final float a = 0.6f * flash;
                final float[] white = { 1f, 0.95f, 0.9f };
                vertex(tess, white, a, (rr - 1.5f) * Math.cos(t0), 0, (rr - 1.5f) * Math.sin(t0));
                vertex(tess, white, a, (rr - 1.5f) * Math.cos(t1), 0, (rr - 1.5f) * Math.sin(t1));
                vertex(tess, white, 0, (rr + 1.5f) * Math.cos(t1), 0, (rr + 1.5f) * Math.sin(t1));
                vertex(tess, white, 0, (rr + 1.5f) * Math.cos(t0), 0, (rr + 1.5f) * Math.sin(t0));
            }
            tess.draw();
        }
    }

    /** Spiral-arm brightness 0.55..1 at angle {@code th}, radius {@code r}. */
    private static float arm(double th, float r, float spin) {
        return 0.775f + 0.225f * (float) Math.sin(3 * th - r * 0.12 - spin * 6);
    }

    /**
     * One jet: a cone from the horizon out to {@link #JET_LEN} along {@code dir} (+1 up, −1 down), as bright as its
     * Bu/t (log scale: 0.1 Bu/t faint, 1,000 Bu/t full), a core and a sheath.
     */
    private static void drawJet(float bu, int dir, float t, float pulse) {
        if (bu <= 0) return;
        final float bright = pulse * Math.max(0.08f, Math.min(1f, (float) (0.35 + 0.2 * Math.log10(bu))));
        final float[] core = { 0.85f, 0.95f, 1f };
        final float[] sheath = { 0.35f, 0.65f, 1f };
        drawCone(core, 0.55f * bright, 0.6f, 1.4f, dir, t);
        drawCone(sheath, 0.22f * bright, 1.6f, 4.2f, dir, t * 0.7f);
    }

    private static void drawCone(float[] rgb, float alpha, float rBase, float rTip, int dir, float t) {
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        final float y0 = 3f;
        for (int st = 0; st < JET_STACKS; st++) {
            final float fa = st / (float) JET_STACKS, fb = (st + 1) / (float) JET_STACKS;
            final float ya = dir * (y0 + (JET_LEN - y0) * fa), yb = dir * (y0 + (JET_LEN - y0) * fb);
            final float ra = rBase + (rTip - rBase) * fa, rb = rBase + (rTip - rBase) * fb;
            // fades along its length, with a travelling pulse
            final float pa = 0.8f + 0.2f * (float) Math.sin(fa * 18 - t * 0.35f);
            final float pb = 0.8f + 0.2f * (float) Math.sin(fb * 18 - t * 0.35f);
            final float aa = alpha * (1f - 0.7f * fa) * pa, ab = alpha * (1f - 0.7f * fb) * pb;
            for (int s = 0; s < JET_SIDES; s++) {
                final double t0 = s * 2 * Math.PI / JET_SIDES, t1 = (s + 1) * 2 * Math.PI / JET_SIDES;
                vertex(tess, rgb, aa, ra * Math.cos(t0), ya, ra * Math.sin(t0));
                vertex(tess, rgb, aa, ra * Math.cos(t1), ya, ra * Math.sin(t1));
                vertex(tess, rgb, ab, rb * Math.cos(t1), yb, rb * Math.sin(t1));
                vertex(tess, rgb, ab, rb * Math.cos(t0), yb, rb * Math.sin(t0));
            }
        }
        tess.draw();
    }

    private static void vertex(Tessellator tess, float[] rgb, float a, double x, double y, double z) {
        tess.setColorRGBA_F(rgb[0], rgb[1], rgb[2], Math.max(0, Math.min(1, a)));
        tess.addVertex(x, y, z);
    }
}
