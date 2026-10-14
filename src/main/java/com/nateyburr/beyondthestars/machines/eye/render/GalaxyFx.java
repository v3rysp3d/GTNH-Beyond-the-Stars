package com.nateyburr.beyondthestars.machines.eye.render;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memAlloc;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memFree;

import java.nio.ByteBuffer;
import java.util.Random;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.Blackbody;
import com.nateyburr.beyondthestars.client.render.Lod;
import com.nateyburr.beyondthestars.client.render.Meshes;
import com.nateyburr.beyondthestars.client.render.NoiseTex;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderProfile;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 WP-5 (spec §9): the Eye's galaxies, T7 Triangulum and T8 the Milky Way, and their events. Three programs:
 * <ul>
 * <li>{@code bts_galsky} (GLSL {@code shaders/galaxy/sky.glsl}): the dark backdrop, the far side of a sphere just
 * inside the Eye's clearance, so a galaxy reads as light against deep space even at noon (TecTech's Eye of Harmony
 * trick). Premultiplied, front faces culled.</li>
 * <li>{@code bts_galdisc} ({@code galaxy/disc.glsl}): the diffuse light on one quad in front of the galaxy: the
 * exponential disc with its density-wave arms, young light and H II knots, the dust lanes (absorbing, so an edge-on
 * galaxy shows the lane splitting its bulge), the bulge and the bar, and the events' glows (ring wave, starburst,
 * collapse flash). Premultiplied.</li>
 * <li>{@code bts_galpts} ({@code galaxy/points_vert.glsl}): the stars, static {@code GL_POINTS} buffers (20,000 per
 * galaxy, 3,000 for the companion, 1,500 for the dwarf, 2,000 for the tidal tails and bridge) whose motion the vertex
 * shader computes: a flat rotation curve, old stars bunched toward the arms, young stars lit as they cross a ridge,
 * star-forming clusters sheared into short arcs, the bulge and bar turning, the halo capture, the dust dimming the
 * stars behind it. Additive.</li>
 * </ul>
 * {@link #compute} maps the synced look and the event clocks to the uniforms; {@link EyeStarRenderer} draws.
 */
@SideOnly(Side.CLIENT)
final class GalaxyFx {

    /** The disc's tilt about the x axis (the stability rail's 8 degrees, as the fixed-function look). */
    static final double TILT = Math.toRadians(8);
    /** Nothing may reach the Eye's blocks. */
    static final float CLEARANCE = 20.4f;
    /** Point-star groups (galaxy/points_vert.glsl). */
    static final int G_OLD = 0, G_YOUNG = 1, G_BULGE = 2, G_BAR = 3, G_CLUSTER = 4, G_HII = 5, G_HALO = 6, G_MC = 7;
    /** The Galactic Merger's pericentre passes, seconds into it (GalacticMergerOverlay.PASSES). */
    static final float[] PASSES = { 600, 1200, 1800 };
    /** Its passes phase plus the settle window, seconds (progress 1 = 2,100 s). */
    static final float MERGER_TOTAL = 2100;

    // ------------------------------------------------------------------ programs

    private static final ShaderRecipe PTS = BTSShaders.recipe("bts_galpts")
        .required(
            "u_Points",
            "u_Mode",
            "u_Gal0",
            "u_Gal1",
            "u_Gal2",
            "u_Rot",
            "u_Ev",
            "u_Comp",
            "u_CompN",
            "u_Sat",
            "u_SatV",
            "u_Tail",
            "u_EyeD")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform P_POINTS = PTS.uniform("u_Points");
    private static final Uniform P_MODE = PTS.uniform("u_Mode");
    private static final Uniform P_GAL0 = PTS.uniform("u_Gal0");
    private static final Uniform P_GAL1 = PTS.uniform("u_Gal1");
    private static final Uniform P_GAL2 = PTS.uniform("u_Gal2");
    private static final Uniform P_ROT = PTS.uniform("u_Rot");
    private static final Uniform P_EV = PTS.uniform("u_Ev");
    private static final Uniform P_COMP = PTS.uniform("u_Comp");
    private static final Uniform P_COMPN = PTS.uniform("u_CompN");
    private static final Uniform P_SAT = PTS.uniform("u_Sat");
    private static final Uniform P_SATV = PTS.uniform("u_SatV");
    private static final Uniform P_TAIL = PTS.uniform("u_Tail");
    private static final Uniform P_EYED = PTS.uniform("u_EyeD");
    static final BTSShaders.Program POINTS_PROGRAM = BTSShaders.program("bts_galpts", PTS);
    private static final BTSShaders.Mesh STARS_T7 = POINTS_PROGRAM.mesh(h -> build(h, stars(false)));
    private static final BTSShaders.Mesh STARS_T8 = POINTS_PROGRAM.mesh(h -> build(h, stars(true)));
    private static final BTSShaders.Mesh COMPANION = POINTS_PROGRAM.mesh(h -> build(h, companion()));
    private static final BTSShaders.Mesh DWARF = POINTS_PROGRAM.mesh(h -> build(h, dwarf()));
    private static final BTSShaders.Mesh TAILS = POINTS_PROGRAM.mesh(h -> build(h, tails()));

    private static final ShaderRecipe DISC = BTSShaders.recipe("bts_galdisc")
        .required(
            "u_QC",
            "u_QU",
            "u_QV",
            "u_Centre",
            "u_Eye",
            "u_AxX",
            "u_AxY",
            "u_Gal0",
            "u_Gal1",
            "u_Gal2",
            "u_Rot",
            "u_Ev",
            "u_Comp",
            "u_Sat",
            "u_Bulge",
            "u_Flash",
            "u_Detail")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform D_QC = DISC.uniform("u_QC");
    private static final Uniform D_QU = DISC.uniform("u_QU");
    private static final Uniform D_QV = DISC.uniform("u_QV");
    private static final Uniform D_CENTRE = DISC.uniform("u_Centre");
    private static final Uniform D_EYE = DISC.uniform("u_Eye");
    private static final Uniform D_AXX = DISC.uniform("u_AxX");
    private static final Uniform D_AXY = DISC.uniform("u_AxY");
    private static final Uniform D_GAL0 = DISC.uniform("u_Gal0");
    private static final Uniform D_GAL1 = DISC.uniform("u_Gal1");
    private static final Uniform D_GAL2 = DISC.uniform("u_Gal2");
    private static final Uniform D_ROT = DISC.uniform("u_Rot");
    private static final Uniform D_EV = DISC.uniform("u_Ev");
    private static final Uniform D_COMP = DISC.uniform("u_Comp");
    private static final Uniform D_SAT = DISC.uniform("u_Sat");
    private static final Uniform D_BULGE = DISC.uniform("u_Bulge");
    private static final Uniform D_FLASH = DISC.uniform("u_Flash");
    private static final Uniform D_DETAIL = DISC.uniform("u_Detail");
    static final BTSShaders.Program DISC_PROGRAM = BTSShaders.program("bts_galdisc", DISC);
    private static final BTSShaders.Mesh QUAD = DISC_PROGRAM.mesh(Meshes::quad);

    private static final ShaderRecipe SKY = BTSShaders.recipe("bts_galsky")
        .required("u_Centre", "u_Eye", "u_Sky")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform S_CENTRE = SKY.uniform("u_Centre");
    private static final Uniform S_EYE = SKY.uniform("u_Eye");
    private static final Uniform S_SKY = SKY.uniform("u_Sky");
    static final BTSShaders.Program SKY_PROGRAM = BTSShaders.program("bts_galsky", SKY);
    private static final BTSShaders.Mesh SPHERE = SKY_PROGRAM.mesh(h -> Meshes.sphere(h, 48, 24));

    private static final ShaderRecipe PLAN = BTSShaders.recipe("bts_galplan")
        .required("u_QC", "u_QU", "u_QV", "u_Centre", "u_Eye", "u_AxX", "u_AxY", "u_Plan")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform N_QC = PLAN.uniform("u_QC");
    private static final Uniform N_QU = PLAN.uniform("u_QU");
    private static final Uniform N_QV = PLAN.uniform("u_QV");
    private static final Uniform N_CENTRE = PLAN.uniform("u_Centre");
    private static final Uniform N_EYE = PLAN.uniform("u_Eye");
    private static final Uniform N_AXX = PLAN.uniform("u_AxX");
    private static final Uniform N_AXY = PLAN.uniform("u_AxY");
    private static final Uniform N_PLAN = PLAN.uniform("u_Plan");
    static final BTSShaders.Program PLANETS_PROGRAM = BTSShaders.program("bts_galplan", PLAN);
    private static final BTSShaders.Mesh PLAN_QUAD = PLANETS_PROGRAM.mesh(Meshes::quad);

    /** GL_VERTEX_PROGRAM_POINT_SIZE (= GL_PROGRAM_POINT_SIZE) and GL_POINT_SPRITE (legacy only). */
    private static final int PROGRAM_POINT_SIZE = 0x8642, POINT_SPRITE = 0x8861;
    private static final Matrix4f MODEL = new Matrix4f();

    private GalaxyFx() {}

    // ================================================================== the look

    /** Every uniform of the three programs for one galaxy this frame. */
    static final class Look {

        final float[] gal0 = new float[4], gal1 = new float[4], gal2 = new float[4], rot = new float[4],
            ev = new float[4], comp = new float[4], compN = new float[4], sat = new float[4], satV = new float[4],
            tail = new float[4], bulge = new float[4], flash = new float[4], detail = new float[4], sky = new float[4];
        /** The eye in the disc frame (camera-relative eye minus the centre, in the disc axes). */
        final float[] eyeD = new float[4];
        /** The eye relative to the render origin. */
        final float[] eye = new float[3];
        /** The disc axes in camera space. */
        final float[] axX = new float[3], axY = new float[3], axN = new float[3];
        /** How much of each galaxy's stars to draw (T7, T8: the merger cross-fades them), 0..1. */
        float w7, w8;
        /** Draw the companion / the dwarf / the tails. */
        boolean companion, dwarf, tails;
        /** The share of each point buffer drawn (LOD). */
        float density;
        /** The glow sprite's colour at L3. */
        final float[] glow = new float[3];
    }

    private static void set(float[] a, float x, float y, float z, float w) {
        a[0] = x;
        a[1] = y;
        a[2] = z;
        a[3] = w;
    }

    private static float clamp01(double x) {
        return (float) Math.max(0, Math.min(1, x));
    }

    private static float smooth(double a, double b, double x) {
        final float t = clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** One galaxy's shape: reach, scale length, pitch, arms, flocculence, rotation, dust, bulge, bar, colour. */
    private static final class Shape {

        final float reach, scale, cot, a2, a4, floc, young, rc, dust, r0, bulgeR, bulgeB, barLen, barB, warm,
            outerPeriod;

        Shape(float reach, float scale, float pitchDeg, float a2, float a4, float floc, float young, float rc,
            float dust, float r0, float bulgeR, float bulgeB, float barLen, float barB, float warm, float outerPeriod) {
            this.reach = reach;
            this.scale = scale;
            this.cot = (float) (1 / Math.tan(Math.toRadians(pitchDeg)));
            this.a2 = a2;
            this.a4 = a4;
            this.floc = floc;
            this.young = young;
            this.rc = rc;
            this.dust = dust;
            this.r0 = r0;
            this.bulgeR = bulgeR;
            this.bulgeB = bulgeB;
            this.barLen = barLen;
            this.barB = barB;
            this.warm = warm;
            this.outerPeriod = outerPeriod;
        }
    }

    /** Triangulum (M33): flocculent, small nucleus, blue, lots of H II. */
    private static final Shape TRIANGULUM = new Shape(
        16f,
        4.2f,
        24f,
        0.28f,
        0f,
        0.8f,
        1.25f,
        1.8f,
        0.9f,
        1.0f,
        0.85f,
        1.5f,
        0f,
        0f,
        0.2f,
        300f);
    /** The Milky Way: barred, two major and two minor arms, a peanut bulge, strong dust lanes. */
    private static final Shape MILKY_WAY = new Shape(
        19f,
        5.0f,
        17f,
        0.6f,
        0.35f,
        0f,
        1.15f,
        2.5f,
        3.2f,
        4.2f,
        2.1f,
        0.75f,
        4.4f,
        1.1f,
        0.85f,
        320f);
    /** The arms' pattern speed, whole turns per hour (one turn in 15 minutes, slower than any star). */
    private static final float PATTERN_TURNS = 4f;

    /**
     * The look of {@code tile} this frame: {@code now} world ticks with the partial tick, {@code time} the shader clock
     * (s), {@code detail} 1..3 (L2..L0), {@code day} 0..1, {@code eyeRel} the eye relative to the render origin and
     * {@code (cx, cy, cz)} the camera-relative centre.
     */
    static void compute(Look L, TileEyeStar tile, byte kind, double now, float time, int detail, float day, float cx,
        float cy, float cz) {
        final int trans = tile.getTransition();
        final float transP = tile.getTransitionProgress();
        final int ev = tile.getEvent();
        final double evIn = tile.eventIn(now);
        final double age = tile.kindAge(now);

        // the merger's clock: seconds into the Galactic Merger (its pass on the event channel, else its progress)
        double mst = -1;
        if (trans == 7) {
            final int pass = ev >= EyeStarLook.EV_PERICENTRE_1 && ev <= EyeStarLook.EV_PERICENTRE_3
                ? ev - EyeStarLook.EV_PERICENTRE_1
                : -1;
            mst = pass >= 0 && !Double.isNaN(evIn) ? PASSES[pass] - evIn : transP * MERGER_TOTAL;
        }
        // the morph from Triangulum to the Milky Way over the merger
        final float m = kind == EyeStarLook.MILKY_WAY ? 1f : mst >= 0 ? smooth(600, 2000, mst) : 0f;
        final Shape a = TRIANGULUM, b = MILKY_WAY;
        final float reach = lerp(a.reach, b.reach, m);
        final float outer = lerp(a.outerPeriod, b.outerPeriod, m);
        final float rc = lerp(a.rc, b.rc, m);
        final float v0 = (float) (2 * Math.PI / outer * Math.sqrt(reach * reach + rc * rc));
        final float pattern = (float) (2 * Math.PI * frac(PATTERN_TURNS * time / 3600.0) + 0.7);

        // the halo capture (Magnetar Collapse, phase 3): the stars settle from a round halo into the disc
        float form = 1f;
        float burst = 1f;
        float shellR = 0, shellS = 0, coreFlash = 0;
        if (trans == 6) form = smooth(0, 1, (transP - 1 / 3.0) / (2 / 3.0));
        if (tile.previousKind() == EyeStarLook.MAGNETAR && age >= 0 && age < 200) {
            // the collapse itself: the core implodes and flashes, the stars burst out, a shell runs to the bands
            final double s = age / 20.0;
            burst = smooth(0, 3, s);
            coreFlash = (float) Math.exp(-s / 0.8);
            shellR = (float) (1.5 + 17.5 * Math.sqrt(Math.min(1, s / 5)));
            shellS = (float) Math.max(0, 1 - s / 5) * 1.4f;
            form = Math.min(form, 0.02f);
        }

        set(L.gal0, reach, lerp(a.scale, b.scale, m), lerp(a.cot, b.cot, m), lerp(a.a2, b.a2, m));
        set(L.gal1, lerp(a.a4, b.a4, m), lerp(a.floc, b.floc, m), PATTERN_TURNS, lerp(a.young, b.young, m));
        final float pulse = tile.getPulse();
        final float throb = 1f + 0.6f * pulse * (float) Math.sin(time * 2.2);
        set(L.gal2, pattern, time, form, (0.9f + 0.15f * tile.getAux()) * throb);
        set(L.rot, v0, rc, lerp(a.dust, b.dust, m), lerp(a.r0, b.r0, m));
        set(
            L.bulge,
            lerp(a.bulgeR, b.bulgeR, m),
            lerp(a.bulgeB, b.bulgeB, m),
            Math.max(0.2f, lerp(a.barLen, b.barLen, m)),
            lerp(a.barB, b.barB, m));
        set(L.ev, -1, 0, 0, burst);
        set(L.comp, 0, 0, 0, 0);
        set(L.compN, 0, 0, 1, 1);
        set(L.sat, 0, 0, 0, 0);
        set(L.satV, 0, 0, 1, 1);
        set(L.tail, 0, 0, 1, 1);
        set(L.flash, shellR, shellS, coreFlash, 0);
        L.w7 = kind == EyeStarLook.MILKY_WAY ? 0 : 1 - m;
        L.w8 = 1 - L.w7;
        L.companion = false;
        L.dwarf = false;
        L.tails = false;

        // ---- the Galactic Merger: the companion's orbit, tidal tails, bridges and starbursts
        if (mst >= 0) merger(L, (float) mst, reach);

        // ---- a minor merger: a dwarf falls in from above or below the disc and is swallowed
        final boolean minor = ev == EyeStarLook.EV_MINOR_MERGER_UP || ev == EyeStarLook.EV_MINOR_MERGER_DOWN;
        final double minorIn = minor && !Double.isNaN(evIn) ? evIn : tile.pastEventIn(now, true);
        if (!Double.isNaN(minorIn) && minorIn > -25 && minorIn < 15.5) {
            final int sign = minor ? (ev == EyeStarLook.EV_MINOR_MERGER_DOWN ? -1 : 1) : tile.pastEventSign();
            minorMerger(L, (float) minorIn, sign, tile.eventSeed(), reach);
        }

        // ---- a satellite crossing (T8): the dwarf falls through the disc and a ring wave of star formation runs out
        final double xIn = tile.crossingIn(now);
        if (!Double.isNaN(xIn) && xIn < 121 && xIn > -300) crossing(L, (float) xIn, reach);

        // ---- detail
        final int segs = detail >= 3 ? 12 : detail == 2 ? 7 : 3;
        set(L.detail, segs, detail >= 3 ? 3 : detail == 2 ? 2 : 1, lerp(a.warm, b.warm, m), 0);
        L.density = detail >= 3 ? 1f : detail == 2 ? 0.5f : 0.15f;
        set(L.sky, CLEARANCE - 0.4f, 0.5f + 0.32f * day, 1f, time);
        L.glow[0] = lerp(0.7f, 1f, m);
        L.glow[1] = lerp(0.8f, 0.85f, m);
        L.glow[2] = lerp(1f, 0.62f, m);

        // ---- the disc frame in camera space, and the eye in it
        final float ct = (float) Math.cos(TILT), st = (float) Math.sin(TILT);
        L.axX[0] = 1;
        L.axX[1] = 0;
        L.axX[2] = 0;
        // right-handed with the normal up (tilted 8 degrees toward -z, the Eye's front)
        L.axY[0] = 0;
        L.axY[1] = -st;
        L.axY[2] = -ct;
        L.axN[0] = 0;
        L.axN[1] = ct;
        L.axN[2] = -st;
        final float ex = L.eye[0] - cx, ey = L.eye[1] - cy, ez = L.eye[2] - cz;
        set(
            L.eyeD,
            ex * L.axX[0] + ey * L.axX[1] + ez * L.axX[2],
            ex * L.axY[0] + ey * L.axY[1] + ez * L.axY[2],
            ex * L.axN[0] + ey * L.axN[1] + ez * L.axN[2],
            day);
    }

    private static double frac(double x) {
        return x - Math.floor(x);
    }

    /** The companion's orbit: apocentres 18, 13, 8.5; pericentres 4.5, 3, 0.8 at 600 / 1,200 / 1,800 s. */
    private static final float[] APO = { 18f, 13f, 8.5f }, PERI = { 4.5f, 3f, 0.8f };
    /** The orbit plane leans this far off the disc. */
    private static final double ORBIT_INCL = Math.toRadians(38), ORBIT_NODE = 0.6;

    /** The companion's disc-frame position at {@code st} seconds into the merger (out: xyz). */
    static void orbit(float st, float[] out) {
        final double phi;
        final float rp0, rp1, apo;
        int seg;
        if (st < PASSES[0]) {
            seg = 0;
            phi = 0.5 + 0.5 * Math.max(0, st) / PASSES[0];
            rp0 = PERI[0];
            rp1 = PERI[0];
            apo = APO[0];
        } else {
            seg = st < PASSES[1] ? 1 : 2;
            final float from = PASSES[seg - 1], to = PASSES[Math.min(seg, 2)];
            phi = Math.min(1, (st - from) / (to - from));
            rp0 = PERI[seg - 1];
            rp1 = PERI[seg];
            apo = APO[seg];
        }
        final double rp = rp0 + (rp1 - rp0) * phi;
        final double r = rp + (apo - rp) * Math.pow(Math.sin(Math.PI * phi), 1.3);
        // fast through pericentre, slow at apocentre
        final double ang = seg * 2 * Math.PI + 2 * Math.PI * (phi + 0.75 / (2 * Math.PI) * Math.sin(2 * Math.PI * phi));
        final double ci = Math.cos(ORBIT_INCL), si = Math.sin(ORBIT_INCL);
        final double cn = Math.cos(ORBIT_NODE), sn = Math.sin(ORBIT_NODE);
        // e1 along the node line (in the disc plane), e2 up the inclined plane
        final double e1x = cn, e1y = sn, e1z = 0;
        final double e2x = -sn * ci, e2y = cn * ci, e2z = si;
        final double c = Math.cos(ang), s = Math.sin(ang);
        out[0] = (float) (r * (c * e1x + s * e2x));
        out[1] = (float) (r * (c * e1y + s * e2y));
        out[2] = (float) (r * (c * e1z + s * e2z));
    }

    private static void merger(Look L, float st, float reach) {
        final float[] p = new float[3];
        orbit(Math.min(st, PASSES[2]), p);
        // after the last pass the companion has fallen in: it dissolves into the disc over two minutes
        final float alive = st <= PASSES[2] ? 1f : clamp01(1 - (st - PASSES[2]) / 120f);
        final float puffed = 1.6f + 0.5f * smooth(PASSES[0], PASSES[2], st);
        set(L.comp, p[0], p[1], p[2], alive * smooth(-30, 60, st));
        set(L.compN, (float) Math.sin(Math.toRadians(50)), 0.3f, (float) Math.cos(Math.toRadians(50)), puffed);
        // the tails grow after the first pass and fade in the settle
        final float grow = smooth(PASSES[0] - 60, PASSES[1] + 60, st)
            * (st <= PASSES[2] ? 1f : clamp01(1 - (st - PASSES[2]) / 300f));
        // the bridge stands for about a minute round each pass; the starburst flashes 2 s and glows for half a minute
        float bridge = 0, flare = 0;
        for (float pass : PASSES) {
            final float d = st - pass;
            bridge = Math.max(bridge, (float) Math.exp(-d * d / (2 * 25 * 25)));
            if (d > -5 && d < 60) {
                final float rise = d < 0 ? smooth(-5, 0, d) * 0.3f : 1f;
                flare = Math
                    .max(flare, rise * (d < 0 ? 1f : (float) (0.7 * Math.exp(-d / 1.0) + 0.3 * Math.exp(-d / 30))));
            }
        }
        set(L.tail, grow, bridge * L.comp[3], 1f, 1f);
        L.ev[2] = Math.max(L.ev[2], flare);
        L.companion = L.comp[3] > 0.003f;
        L.tails = grow > 0.003f || bridge * L.comp[3] > 0.01f;
    }

    /**
     * A minor merger: {@code in} seconds to the hit (15 s warning, 5 s hit, an afterglow after), the dwarf coming from
     * above ({@code sign} +1) or below, from a direction drawn from the event's seed.
     */
    private static void minorMerger(Look L, float in, int sign, int seed, float reach) {
        final double a0 = (seed & 0xFFFF) / 65536.0 * 2 * Math.PI;
        final float u = clamp01(1 - in / 15f);
        final float e = (float) Math.pow(u, 1.6);
        final double ang = a0 + 1.3 * e;
        final float r = lerp(reach * 1.0f, 5.5f, e);
        final float z = sign * lerp(9f, 0f, e);
        final double clamp = Math.min(1, 19.4 / Math.sqrt(r * r + z * z));
        final float x = (float) (r * Math.cos(ang) * clamp), y = (float) (r * Math.sin(ang) * clamp);
        // the direction of travel (down the path)
        final double ang2 = a0 + 1.3 * Math.pow(Math.min(1, u + 0.02), 1.6);
        final float r2 = lerp(reach, 5.5f, (float) Math.pow(Math.min(1, u + 0.02), 1.6));
        float vx = (float) (r2 * Math.cos(ang2)) - x, vy = (float) (r2 * Math.sin(ang2)) - y, vz = -sign * 0.15f;
        final float vl = (float) Math.sqrt(vx * vx + vy * vy + vz * vz) + 1e-6f;
        final float fadeIn = smooth(15.5, 13, in);
        // after the hit it is torn apart and swallowed
        final float after = in < 0 ? clamp01(1 + in / 12f) : 1f;
        set(L.sat, x, y, in < 0 ? 0 : z, 1.6f * fadeIn * after);
        set(L.satV, vx / vl, vy / vl, vz / vl, 1f + 1.2f * u * u + (in < 0 ? 2.5f * (1 - after) : 0));
        L.tail[3] = 0.85f + (in < 0 ? 1.5f * (1 - after) : 0);
        L.flash[3] = Math.max(L.flash[3], in <= 0 ? (float) Math.exp(in / 1.5) : 0f);
        L.ev[2] = Math.max(L.ev[2], in <= 0 && in > -20 ? 0.25f * (float) Math.exp(in / 4) : 0f);
        L.dwarf = L.sat[3] > 0.003f;
    }

    /**
     * A satellite crossing (T8): {@code in} seconds to the hit (a 120 s countdown, then 300 s): the dwarf falls from
     * high above the disc, punches through near the centre and a ring wave of star formation runs out to the edge.
     */
    private static void crossing(Look L, float in, float reach) {
        final float hx = 3.6f, hy = 2.1f;
        if (in >= 0) {
            final float u = clamp01(1 - in / 120f);
            // free fall: slow far out, fast at the end
            final float e = (float) Math.pow(u, 2.2);
            final float z = lerp(16f, 0f, e);
            final float x = lerp(6.5f, hx, e), y = lerp(3.8f, hy, e);
            set(L.sat, x, y, z, 1.5f * smooth(0, 10, 120 - in));
            set(L.satV, -0.1f, -0.06f, -1f, 1f + 1.6f * u * u);
            L.tail[3] = 1.7f;
        } else {
            final float tau = -in;
            final float out = clamp01(tau / 60f);
            set(L.sat, hx - 0.8f * out, hy - 0.5f * out, -15f * (float) Math.pow(out, 0.8), 1.5f * (1 - out));
            set(L.satV, -0.1f, -0.06f, -1f, 2.6f + 2f * out);
            L.tail[3] = 1.7f + 0.8f * out;
            final float wave = smooth(0, 5, tau) * (1 - smooth(230, 300, tau));
            L.ev[0] = 1.5f + (reach - 1.5f) * (float) Math.pow(Math.min(1, tau / 300f), 0.8);
            L.ev[1] = wave;
            L.flash[3] = Math.max(L.flash[3], (float) Math.exp(-tau / 2));
            L.ev[2] = Math.max(L.ev[2], 0.45f * (float) Math.exp(-tau / 4));
        }
        L.dwarf = L.sat[3] > 0.003f;
    }

    // ================================================================== draws

    /** The backdrop (pass 1): premultiplied, front faces culled; the caller saved the state. */
    static boolean drawSky(double cx, double cy, double cz, Look l) {
        final ShaderHandle h = SKY_PROGRAM.get();
        final IVertexArrayObject mesh = SPHERE.get();
        if (h == null || mesh == null) return false;
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(S_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform3f(h.loc(S_EYE), l.eye[0], l.eye[1], l.eye[2]);
        u4(h, S_SKY, l.sky);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_CULL_FACE);
        return true;
    }

    /**
     * Planets mode (spec §9.1): the frame and the backdrop of {@code tile}'s planets this frame ({@link #drawPlanets}
     * reads {@code planets}, {@code time}); the sun is the star programs' (EyeStarFx).
     */
    static void computePlanets(Look L, TileEyeStar tile, float time, float day, float cx, float cy, float cz,
        float sunRadius) {
        final int n = Math.max(0, Math.min(9, Math.round(tile.getAux() * 9)));
        set(L.sky, CLEARANCE - 0.4f, 0.5f + 0.32f * day, 0.8f, time);
        set(L.detail, n, time, sunRadius, 1f);
        final float ct = (float) Math.cos(TILT), st = (float) Math.sin(TILT);
        L.axX[0] = 1;
        L.axX[1] = 0;
        L.axX[2] = 0;
        L.axY[0] = 0;
        L.axY[1] = -st;
        L.axY[2] = -ct;
    }

    /** The planets and their orbits (pass 1) on the quad {@code qc + x qu + y qv}; premultiplied. */
    static boolean drawPlanets(double cx, double cy, double cz, Look l, float[] qc, float[] qu, float[] qv) {
        final ShaderHandle h = PLANETS_PROGRAM.get();
        final IVertexArrayObject mesh = PLAN_QUAD.get();
        if (h == null || mesh == null) return false;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        h.use();
        h.uploadModel(MODEL.identity());
        GL20.glUniform3f(h.loc(N_QC), qc[0], qc[1], qc[2]);
        GL20.glUniform3f(h.loc(N_QU), qu[0], qu[1], qu[2]);
        GL20.glUniform3f(h.loc(N_QV), qv[0], qv[1], qv[2]);
        GL20.glUniform3f(h.loc(N_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform3f(h.loc(N_EYE), l.eye[0], l.eye[1], l.eye[2]);
        GL20.glUniform3f(h.loc(N_AXX), l.axX[0], l.axX[1], l.axX[2]);
        GL20.glUniform3f(h.loc(N_AXY), l.axY[0], l.axY[1], l.axY[2]);
        u4(h, N_PLAN, l.detail);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** The diffuse light (pass 1) on the quad {@code qc + x qu + y qv}; premultiplied. */
    static boolean drawDisc(double cx, double cy, double cz, Look l, float[] qc, float[] qu, float[] qv) {
        final ShaderHandle h = DISC_PROGRAM.get();
        final IVertexArrayObject mesh = QUAD.get();
        if (h == null || mesh == null) return false;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        h.use();
        h.uploadModel(MODEL.identity());
        GL20.glUniform3f(h.loc(D_QC), qc[0], qc[1], qc[2]);
        GL20.glUniform3f(h.loc(D_QU), qu[0], qu[1], qu[2]);
        GL20.glUniform3f(h.loc(D_QV), qv[0], qv[1], qv[2]);
        GL20.glUniform3f(h.loc(D_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform3f(h.loc(D_EYE), l.eye[0], l.eye[1], l.eye[2]);
        GL20.glUniform3f(h.loc(D_AXX), l.axX[0], l.axX[1], l.axX[2]);
        GL20.glUniform3f(h.loc(D_AXY), l.axY[0], l.axY[1], l.axY[2]);
        u4(h, D_GAL0, l.gal0);
        u4(h, D_GAL1, l.gal1);
        u4(h, D_GAL2, l.gal2);
        u4(h, D_ROT, l.rot);
        u4(h, D_EV, l.ev);
        u4(h, D_COMP, l.comp);
        u4(h, D_SAT, l.sat);
        u4(h, D_BULGE, l.bulge);
        u4(h, D_FLASH, l.flash);
        u4(h, D_DETAIL, l.detail);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** The stars (pass 1, additive): the galaxy (or both during the merger), the companion, the dwarf, the tails. */
    static boolean drawStars(double cx, double cy, double cz, Look l) {
        final ShaderHandle h = POINTS_PROGRAM.get();
        if (h == null) return false;
        final boolean legacy = h.profile() == ShaderProfile.LEGACY;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glEnable(PROGRAM_POINT_SIZE);
        if (legacy) GL11.glEnable(POINT_SPRITE);
        h.use();
        // the disc frame: x along world x, y and the normal tilted about x
        final float ct = (float) Math.cos(TILT), st = (float) Math.sin(TILT);
        MODEL.set(1, 0, 0, 0, 0, -st, -ct, 0, 0, ct, -st, 0, (float) cx, (float) cy, (float) cz, 1);
        h.uploadModel(MODEL);
        GL20.glUniform3f(h.loc(P_POINTS), (float) Lod.pixelsPerBlockAtUnit(), 1.0f, 26f);
        u4(h, P_GAL0, l.gal0);
        u4(h, P_GAL1, l.gal1);
        u4(h, P_ROT, l.rot);
        u4(h, P_EV, l.ev);
        u4(h, P_COMP, l.comp);
        u4(h, P_COMPN, l.compN);
        u4(h, P_SAT, l.sat);
        u4(h, P_SATV, l.satV);
        u4(h, P_TAIL, l.tail);
        u4(h, P_EYED, l.eyeD);
        final float bright = l.gal2[3];
        GL20.glUniform1f(h.loc(P_MODE), 0f);
        if (l.w7 > 0.003f) points(h, STARS_T7, l, bright * l.w7, l.density);
        if (l.w8 > 0.003f) points(h, STARS_T8, l, bright * l.w8, l.density);
        if (l.companion) {
            GL20.glUniform1f(h.loc(P_MODE), 1f);
            points(h, COMPANION, l, bright, Math.max(0.3f, l.density));
        }
        if (l.dwarf) {
            GL20.glUniform1f(h.loc(P_MODE), 2f);
            points(h, DWARF, l, bright, Math.max(0.3f, l.density));
        }
        if (l.tails) {
            GL20.glUniform1f(h.loc(P_MODE), 3f);
            points(h, TAILS, l, bright, Math.max(0.3f, l.density));
        }
        ShaderProgram.clear();
        if (legacy) GL11.glDisable(POINT_SPRITE);
        GL11.glDisable(PROGRAM_POINT_SIZE);
        return true;
    }

    private static void points(ShaderHandle h, BTSShaders.Mesh mesh, Look l, float bright, float density) {
        final IVertexArrayObject vao = mesh.get();
        if (vao == null) return;
        GL20.glUniform4f(h.loc(P_GAL2), l.gal2[0], l.gal2[1], l.gal2[2], bright);
        final int count = vao.getVBO()
            .getVertexCount();
        final int n = Math.max(1, Math.min(count, Math.round(count * density)));
        vao.bind();
        vao.draw(0, n);
        vao.unbind();
    }

    private static void u4(ShaderHandle h, Uniform u, float[] v) {
        GL20.glUniform4f(h.loc(u), v[0], v[1], v[2], v[3]);
    }

    // ================================================================== the star buffers

    /** A star list: position, group x 8 + size, packed colour; shuffled so any prefix is a fair sample. */
    private static final class Stars {

        final float[] xyz;
        final float[] gs;
        final int[] rgb;
        int n;

        Stars(int capacity) {
            xyz = new float[capacity * 3];
            gs = new float[capacity];
            rgb = new int[capacity];
        }

        void add(double x, double y, double z, int group, double size, int colour) {
            if (n == gs.length) return;
            xyz[n * 3] = (float) x;
            xyz[n * 3 + 1] = (float) y;
            xyz[n * 3 + 2] = (float) z;
            gs[n] = group * 8 + (float) Math.max(0.02, Math.min(7.9, size));
            rgb[n] = colour & 0xFFFFFF;
            n++;
        }

        Stars shuffled(long seed) {
            final Random r = new Random(seed);
            for (int i = n - 1; i > 0; i--) {
                final int j = r.nextInt(i + 1);
                for (int k = 0; k < 3; k++) {
                    final float t = xyz[i * 3 + k];
                    xyz[i * 3 + k] = xyz[j * 3 + k];
                    xyz[j * 3 + k] = t;
                }
                final float g = gs[i];
                gs[i] = gs[j];
                gs[j] = g;
                final int c = rgb[i];
                rgb[i] = rgb[j];
                rgb[j] = c;
            }
            return this;
        }
    }

    private static IVertexArrayObject build(ShaderHandle h, Stars s) {
        final int stride = h.vertexFormat()
            .getVertexSize();
        if (stride != 20)
            throw new IllegalStateException("bts_galpts: expected position + UV (20 bytes), got " + stride);
        final ByteBuffer buf = memAlloc(s.n * stride);
        try {
            for (int i = 0; i < s.n; i++) {
                final int base = i * stride;
                buf.putFloat(base, s.xyz[i * 3]);
                buf.putFloat(base + 4, s.xyz[i * 3 + 1]);
                buf.putFloat(base + 8, s.xyz[i * 3 + 2]);
                buf.putFloat(base + 12, s.gs[i]);
                buf.putFloat(base + 16, s.rgb[i]);
            }
            buf.position(0)
                .limit(s.n * stride);
            return VertexBufferType.IMMUTABLE.allocate(h.vertexFormat(), GL11.GL_POINTS, buf, s.n);
        } finally {
            memFree(buf);
        }
    }

    /** A colour of {@code kelvin} at {@code lum} (0..1) of full brightness. */
    private static int colour(double kelvin, double lum) {
        final float[] c = Blackbody.rgb(kelvin);
        final double l = Math.max(0.05, Math.min(1, lum));
        final int r = (int) Math.round(255 * Math.min(1, c[0]) * l);
        final int g = (int) Math.round(255 * Math.min(1, c[1]) * l);
        final int b = (int) Math.round(255 * Math.min(1, c[2]) * l);
        return (r << 16) | (g << 8) | b;
    }

    /** A star's size in blocks from a power-law luminosity function (a few large, many small). */
    private static double size(Random r, double base, double spread) {
        return base * Math.min(6, Math.pow(1 - r.nextDouble() * 0.999, -spread));
    }

    private static double gauss(Random r) {
        return r.nextGaussian();
    }

    /** A truncated exponential radius with scale {@code h} between {@code min} and {@code max}. */
    private static double expRadius(Random r, double h, double min, double max) {
        final double a = Math.exp(-min / h), b = Math.exp(-max / h);
        return -h * Math.log(a - r.nextDouble() * (a - b));
    }

    /** The 20,000 stars of Triangulum ({@code mw} false) or the Milky Way. */
    static Stars stars(boolean mw) {
        final Shape sh = mw ? MILKY_WAY : TRIANGULUM;
        final Random r = new Random(mw ? 0x5EED_0008L : 0x5EED_0007L);
        final Stars s = new Stars(20_000);
        final double R = sh.reach * 0.98;
        // old disc: warm, smooth, a few red giants
        final int old = mw ? 8_200 : 8_400;
        for (int i = 0; i < old; i++) {
            final double rr = expRadius(r, sh.scale, mw ? 1.2 : 0.6, R);
            final boolean giant = r.nextDouble() < 0.07;
            final double k = giant ? 3300 + 500 * r.nextDouble() : 4300 + 2400 * Math.pow(r.nextDouble(), 1.5);
            final double z = gauss(r) * (0.32 + 0.025 * rr);
            s.add(
                rr,
                r.nextDouble() * 2 * Math.PI,
                z,
                G_OLD,
                size(r, giant ? 0.13 : 0.075, 0.45),
                colour(k, giant ? 0.85 : 0.35 + 0.5 * r.nextDouble()));
        }
        // young: blue-white, lit where they cross an arm
        final int young = mw ? 4_800 : 6_000;
        for (int i = 0; i < young; i++) {
            final double rr = expRadius(r, sh.scale * 1.3, mw ? 3.0 : 1.4, R);
            final double k = 9000 * Math.pow(30000 / 9000.0, Math.pow(r.nextDouble(), 1.4));
            s.add(
                rr,
                r.nextDouble() * 2 * Math.PI,
                gauss(r) * 0.14,
                G_YOUNG,
                size(r, 0.09, 0.55),
                colour(k, 0.45 + 0.55 * r.nextDouble()));
        }
        // bulge: red-orange, round (Triangulum: a small bright nucleus)
        final int bulge = mw ? 2_300 : 1_500;
        final double sb = sh.bulgeR * (mw ? 0.62 : 0.55);
        for (int i = 0; i < bulge; i++) {
            double x = gauss(r) * sb, y = gauss(r) * sb, z = gauss(r) * sb * 0.72;
            if (mw && r.nextDouble() < 0.3) {
                // the peanut: some stars ride the X up out of the plane
                final double t = gauss(r) * sb * 1.3;
                x = t;
                z = Math.abs(t) * 0.55 * (r.nextBoolean() ? 1 : -1) + gauss(r) * 0.25;
                y = gauss(r) * sb * 0.6;
            }
            s.add(
                x,
                y,
                z,
                G_BULGE,
                size(r, 0.08, 0.4),
                colour(3400 + 1700 * r.nextDouble(), 0.4 + 0.6 * r.nextDouble()));
        }
        // the bar (the Milky Way)
        if (mw) {
            for (int i = 0; i < 1_300; i++) {
                final double x = Math.max(-1.05, Math.min(1.05, gauss(r) * 0.5)) * sh.barLen;
                s.add(
                    x,
                    gauss(r) * 0.3 * sh.barLen * 0.35,
                    gauss(r) * 0.22,
                    G_BAR,
                    size(r, 0.08, 0.4),
                    colour(3800 + 1600 * r.nextDouble(), 0.45 + 0.55 * r.nextDouble()));
            }
        }
        // star-forming clusters and their H II glows
        final int clusters = mw ? 42 : 90, per = mw ? 34 : 34;
        for (int c = 0; c < clusters; c++) {
            for (int i = 0; i < per; i++) {
                final double dr = Math.max(-1.4, Math.min(1.4, gauss(r) * 0.55));
                final double k = 11000 * Math.pow(30000 / 11000.0, r.nextDouble());
                s.add(
                    c * 4 + dr + 1.5,
                    gauss(r) * 0.07,
                    gauss(r) * 0.1,
                    G_CLUSTER,
                    size(r, 0.085, 0.5),
                    colour(k, 0.5 + 0.5 * r.nextDouble()));
            }
            for (int i = 0; i < 2; i++) {
                s.add(c * 4 + gauss(r) * 0.3 + 1.5, gauss(r) * 0.04, 0, G_HII, 0.9 + 0.8 * r.nextDouble(), 0xFF6CA8);
            }
        }
        // the halo and its globular clusters
        for (int i = 0; i < 320; i++) {
            final double rad = 3 + 15 * Math.pow(r.nextDouble(), 1.6);
            final double u = 2 * r.nextDouble() - 1, ph = 2 * Math.PI * r.nextDouble(), q = Math.sqrt(1 - u * u);
            final boolean globular = i < 60;
            s.add(
                rad * q * Math.cos(ph),
                rad * q * Math.sin(ph),
                rad * u * 0.8,
                G_HALO,
                globular ? 0.18 : size(r, 0.06, 0.3),
                colour(globular ? 5200 : 4200 + 1200 * r.nextDouble(), globular ? 0.55 : 0.3));
        }
        // the Magellanic Clouds and their stream (the Milky Way)
        if (mw) {
            cloud(s, r, 15.0, Math.toRadians(215), -6.5, 1.25, 650);
            cloud(s, r, 16.5, Math.toRadians(250), -5.0, 0.8, 330);
            for (int i = 0; i < 160; i++) {
                final double t = r.nextDouble();
                final double a = Math.toRadians(250 + 70 * t), rad = 16.5 + 1.5 * t;
                s.add(
                    rad * Math.cos(a) + gauss(r) * 0.4,
                    rad * Math.sin(a) + gauss(r) * 0.4,
                    -5 - 1.5 * t + gauss(r) * 0.3,
                    G_MC,
                    size(r, 0.06, 0.3),
                    colour(8000, 0.25));
            }
        }
        return s.shuffled(mw ? 81L : 71L);
    }

    private static void cloud(Stars s, Random r, double rad, double ang, double z, double sigma, int n) {
        final double cx = rad * Math.cos(ang), cy = rad * Math.sin(ang);
        for (int i = 0; i < n; i++) {
            // irregular: a lumpy, barred blob
            final double bar = r.nextDouble() < 0.35 ? 1.8 : 1;
            final double k = r.nextDouble() < 0.4 ? 9000 + 15000 * r.nextDouble() : 4500 + 2000 * r.nextDouble();
            s.add(
                cx + gauss(r) * sigma * bar,
                cy + gauss(r) * sigma * 0.7,
                z + gauss(r) * sigma * 0.45,
                G_MC,
                size(r, 0.07, 0.45),
                colour(k, 0.35 + 0.65 * r.nextDouble()));
        }
    }

    /** The companion galaxy: a small spiral of 3,000 stars (its own frame, radius about 5). */
    static Stars companion() {
        final Random r = new Random(0x5EED_00C0L);
        final Stars s = new Stars(3_000);
        for (int i = 0; i < 1_500; i++) {
            s.add(
                expRadius(r, 1.8, 0.4, 5.5),
                r.nextDouble() * 2 * Math.PI,
                gauss(r) * 0.25,
                G_OLD,
                size(r, 0.08, 0.45),
                colour(4300 + 2200 * r.nextDouble(), 0.4 + 0.6 * r.nextDouble()));
        }
        for (int i = 0; i < 800; i++) {
            s.add(
                expRadius(r, 2.4, 1.0, 5.5),
                r.nextDouble() * 2 * Math.PI,
                gauss(r) * 0.1,
                G_YOUNG,
                size(r, 0.09, 0.5),
                colour(10000 + 18000 * r.nextDouble(), 0.5 + 0.5 * r.nextDouble()));
        }
        for (int i = 0; i < 700; i++) {
            s.add(
                gauss(r) * 0.65,
                gauss(r) * 0.65,
                gauss(r) * 0.45,
                G_BULGE,
                size(r, 0.08, 0.4),
                colour(3500 + 1500 * r.nextDouble(), 0.5 + 0.5 * r.nextDouble()));
        }
        return s.shuffled(91L);
    }

    /** The dwarf (a satellite or a minor merger): a small elliptical blob, unit scale. */
    static Stars dwarf() {
        final Random r = new Random(0x5EED_00D0L);
        final Stars s = new Stars(1_500);
        for (int i = 0; i < 1_500; i++) {
            final double k = r.nextDouble() < 0.2 ? 9000 + 12000 * r.nextDouble() : 4000 + 2000 * r.nextDouble();
            s.add(
                gauss(r),
                gauss(r) * 0.8,
                gauss(r) * 0.7,
                G_OLD,
                size(r, 0.075, 0.45),
                colour(k, 0.35 + 0.65 * r.nextDouble()));
        }
        return s.shuffled(93L);
    }

    /** The tidal tails (the companion's, the galaxy's counter-tail) and the bridge. */
    static Stars tails() {
        final Random r = new Random(0x5EED_0070L);
        final Stars s = new Stars(2_000);
        for (int i = 0; i < 2_000; i++) {
            final int g = i < 800 ? 0 : i < 1_600 ? 1 : 2;
            final double t = g == 2 ? r.nextDouble() : Math.pow(r.nextDouble(), 0.9);
            final boolean young = r.nextDouble() < (g == 2 ? 0.7 : 0.45);
            final double k = young ? 10000 + 18000 * r.nextDouble() : 4300 + 2000 * r.nextDouble();
            s.add(t, gauss(r) * 0.6, gauss(r) * 0.6, g, size(r, 0.11, 0.5), colour(k, 0.5 + 0.5 * r.nextDouble()));
        }
        return s.shuffled(97L);
    }
}
