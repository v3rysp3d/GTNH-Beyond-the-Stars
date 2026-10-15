package com.nateyburr.beyondthestars.machines.eye.render;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memAlloc;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memFree;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.Billboard;
import com.nateyburr.beyondthestars.client.render.FixedShapes;
import com.nateyburr.beyondthestars.client.render.GlowSprite;
import com.nateyburr.beyondthestars.client.render.Lod;
import com.nateyburr.beyondthestars.client.render.MachineFx;
import com.nateyburr.beyondthestars.client.render.Meshes;
import com.nateyburr.beyondthestars.client.render.NoiseTex;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.MeshBuilder;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderProfile;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * P5 (design p5-t9-t10.md §2.1, §6.2, §9.5): the Eye's two cosmic objects on the render kit. Three programs of their
 * own, GLSL {@code shaders/cosmos/}, plus the kit's beam ({@link MachineFx#drawBeam}) and glow sprite:
 * <ul>
 * <li>{@code bts_cosky} ({@code cosmos/sky.glsl}): the Great Attractor's backdrop, the far side of a sphere round the
 * dark core: stars and a faint nebula seen through the point-lens equation, so they smear into an Einstein ring and an
 * inner image (honours {@code renderBackgroundLensing}). Premultiplied.</li>
 * <li>{@code bts_copts} ({@code cosmos/points_vert.glsl}): static {@code GL_POINTS} buffers whose motion the vertex
 * shader computes: the Attractor's six cosmic-web filaments (12,000 dust points drifting in), 600 streaming galaxies
 * (sprites: spirals and ellipticals, captured into the halo at the end of their fall), 600 halo dots (as many shown as
 * the galaxies held, on a log scale) and the Galactic Flow's group of up to 12; the Universe's web (20,000 points
 * gathering from scattered matter onto filaments and nodes as it ages, lit by the first stars at the Cosmic Dawn).
 * Additive.</li>
 * <li>{@code bts_cofx} ({@code cosmos/fx.glsl}): the dark core (opaque, writing depth), the Cosmic Dawn's twelve rays
 * along the crown's spikes (one draw), the last-scattering surface (a CMB map in the crown's tilted plane), the
 * glowing rings (the collars' glass channels and the crown).</li>
 * </ul>
 * Everything is placed in the Eye's voxel frame (designs/voxel.js), taken into the world from the controller's facing
 * ({@link EyeCosmosLook#frame}); the crown's own frame adds its lift (y 62) and tilt (-10 degrees). Draws per object:
 * the Attractor 5 (core in pass 0; backdrop, points, collar rings, beam), the Universe 6 (core in pass 0; jet,
 * membrane, web, crown glow, rays). Detail: L0 every point, 2 nebula / 4 CMB octaves; L1 half the points; L2 15 %, no
 * flow highlight; L3 a glow sprite (and the core). FIXED: {@link #drawFixed}, a few hundred fixed-function quads.
 * Nothing allocates per frame: one {@link Look} per renderer.
 */
@SideOnly(Side.CLIENT)
final class CosmosFx {

    /** The dark core's radius and the Einstein radius (2.4 x), blocks. */
    static final float CORE = EyeStarLook.ATTRACTOR_CORE, EINSTEIN = CORE * 2.4f;
    /** The lensed backdrop's radius, blocks. */
    static final float SKY_RADIUS = 34f;
    /** The pellet beam: from the top collar's jet cells down to the core, voxel y. */
    static final float BEAM_TOP = 92f;
    /** The crown (designs/voxel.js EYE.uni): lift, half-axes, tilt. */
    static final float CROWN_Y = 62f, CROWN_A = 74f, CROWN_B = 70f;
    static final double CROWN_TILT = Math.toRadians(-10);
    /** The last-scattering surface's half-axes at full size, inside the crown's sawtooth. */
    static final float MEMBRANE_A = 68f, MEMBRANE_B = 64f;
    /** How the Attractor and the Universe rank for LOD: bounding radius, blocks. */
    static final float ATTRACTOR_BOUND = 60f, UNIVERSE_BOUND = 85f;
    /** The Infall and Inflation on the transition channel (EyeStarLook.trans: the tier they start from). */
    static final int TRANS_INFALL = 8, TRANS_INFLATION = 9;

    // ------------------------------------------------------------------ programs

    private static final ShaderRecipe SKY = BTSShaders.recipe("bts_cosky")
        .required("u_Centre", "u_Eye", "u_Sky", "u_Lens")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform S_CENTRE = SKY.uniform("u_Centre");
    private static final Uniform S_EYE = SKY.uniform("u_Eye");
    private static final Uniform S_SKY = SKY.uniform("u_Sky");
    private static final Uniform S_LENS = SKY.uniform("u_Lens");
    static final BTSShaders.Program SKY_PROGRAM = BTSShaders.program("bts_cosky", SKY);
    private static final BTSShaders.Mesh SKY_SPHERE = SKY_PROGRAM.mesh(h -> Meshes.sphere(h, 48, 24));

    private static final ShaderRecipe PTS = BTSShaders.recipe("bts_copts")
        .required("u_Points", "u_CoTime", "u_CoFlow", "u_CoFlow2", "u_CoHalo", "u_CoWeb", "u_CoFate")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform P_POINTS = PTS.uniform("u_Points");
    private static final Uniform P_TIME = PTS.uniform("u_CoTime");
    private static final Uniform P_FLOW = PTS.uniform("u_CoFlow");
    private static final Uniform P_FLOW2 = PTS.uniform("u_CoFlow2");
    private static final Uniform P_HALO = PTS.uniform("u_CoHalo");
    private static final Uniform P_WEB = PTS.uniform("u_CoWeb");
    private static final Uniform P_FATE = PTS.uniform("u_CoFate");
    static final BTSShaders.Program POINTS_PROGRAM = BTSShaders.program("bts_copts", PTS);
    private static final BTSShaders.Mesh ATTRACTOR_POINTS = POINTS_PROGRAM.mesh(h -> build(h, attractorPoints()));
    private static final BTSShaders.Mesh WEB_POINTS = POINTS_PROGRAM.mesh(h -> build(h, webPoints()));

    private static final ShaderRecipe FX = BTSShaders.recipe("bts_cofx")
        .required("u_FxMode", "u_FxA", "u_FxB", "u_FxC", "u_FxD", "u_Cam")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform F_MODE = FX.uniform("u_FxMode");
    private static final Uniform F_A = FX.uniform("u_FxA");
    private static final Uniform F_B = FX.uniform("u_FxB");
    private static final Uniform F_C = FX.uniform("u_FxC");
    private static final Uniform F_D = FX.uniform("u_FxD");
    private static final Uniform F_CAM = FX.uniform("u_Cam");
    static final BTSShaders.Program FX_PROGRAM = BTSShaders.program("bts_cofx", FX);
    private static final BTSShaders.Mesh CORE_SPHERE = FX_PROGRAM.mesh(h -> Meshes.sphere(h, 40, 20));
    private static final BTSShaders.Mesh RAYS = FX_PROGRAM.mesh(h -> ribbons(h, 12, 16));
    private static final BTSShaders.Mesh DISC = FX_PROGRAM.mesh(h -> Meshes.annulus(h, 128, 12));
    /** Rings 0..4 the collars' glass channels, 5 the crown. */
    private static final BTSShaders.Mesh RINGS = FX_PROGRAM.mesh(h -> ribbons(h, 6, 128));
    private static final int RING_SEGMENTS = 128;

    /** The programs each object draws with (one array each: a frame allocates nothing). */
    static final BTSShaders.Program[] ATTRACTOR_PROGRAMS = { SKY_PROGRAM, POINTS_PROGRAM, FX_PROGRAM,
        MachineFx.BEAM_PROGRAM, GlowSprite.PROGRAM },
        UNIVERSE_PROGRAMS = { POINTS_PROGRAM, FX_PROGRAM, MachineFx.BEAM_PROGRAM, GlowSprite.PROGRAM };

    /** GL_VERTEX_PROGRAM_POINT_SIZE (= GL_PROGRAM_POINT_SIZE) and GL_POINT_SPRITE (legacy only). */
    private static final int PROGRAM_POINT_SIZE = 0x8642, POINT_SPRITE = 0x8861;
    /** The flow's group comes first in the Attractor's buffer (never thinned by LOD). */
    private static final int FLOW_MEMBERS = 12;
    /** The frame of an Eye facing north (a look without a frame). */
    private static final byte NORTH_FRAME = EyeCosmosLook.frameOf(ExtendedFacing.of(ForgeDirection.NORTH));

    private CosmosFx() {}

    // ================================================================== the look

    /** Every uniform and placement of one object this frame (one instance per renderer). */
    static final class Look {

        /** The voxel axes in the world (y is always up) and the centre, camera-relative. */
        final double[] ex = new double[3], ey = new double[3], ez = new double[3];
        double cx, cy, cz;
        /** The eye relative to the render origin. */
        final float[] eye = new float[3];
        /** The model matrices: the voxel frame and the crown's frame. */
        final Matrix4f voxel = new Matrix4f(), crown = new Matrix4f();
        /** The camera in the voxel frame and in the crown's frame. */
        final float[] camV = new float[3], camC = new float[3];
        final float[] time = new float[4], flow = new float[4], flow2 = new float[4], halo = new float[4],
            web = new float[4], fate = new float[4], sky = new float[4], lens = new float[4];
        /** fx: the core, the rays, the membrane, the rings. */
        final float[] coreA = new float[4], coreB = new float[4], rayA = new float[4], rayB = new float[4],
            rayC = new float[4], memA = new float[4], memB = new float[4], memC = new float[4], ringA = new float[4],
            ringB = new float[4], ringC = new float[4];
        /** The beam (T9) or the jet (T10). */
        final MachineFx.Beam beam = new MachineFx.Beam();
        final double[] beamTop = new double[3], beamBottom = new double[3];
        boolean beamOn, membraneOn, ringsOn;
        /** The share of each point buffer drawn (LOD). */
        float density;
        /** The L3 sprite's colour. */
        final float[] glow = new float[3];
    }

    private static void set(float[] a, double x, double y, double z, double w) {
        a[0] = (float) x;
        a[1] = (float) y;
        a[2] = (float) z;
        a[3] = (float) w;
    }

    private static float clamp01(double x) {
        return (float) Math.max(0, Math.min(1, x));
    }

    private static float smooth(double a, double b, double x) {
        final float t = clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    /** The voxel frame from the look's frame byte, the centre camera-relative and the eye. */
    private static void frame(Look L, EyeCosmosLook c, double cx, double cy, double cz) {
        final byte f = c != null && c.frame >= 0 ? c.frame : NORTH_FRAME;
        EyeCosmosLook.axis(f / 6, L.ex);
        EyeCosmosLook.axis(f % 6, L.ez);
        L.ey[0] = 0;
        L.ey[1] = 1;
        L.ey[2] = 0;
        L.cx = cx;
        L.cy = cy;
        L.cz = cz;
        L.voxel.set(
            (float) L.ex[0],
            (float) L.ex[1],
            (float) L.ex[2],
            0f,
            0f,
            1f,
            0f,
            0f,
            (float) L.ez[0],
            (float) L.ez[1],
            (float) L.ez[2],
            0f,
            (float) cx,
            (float) cy,
            (float) cz,
            1f);
        // the crown's frame: lifted CROWN_Y, its x along the voxel x, its z along the tilted ellipse axis
        final float ct = (float) Math.cos(CROWN_TILT), st = (float) Math.sin(CROWN_TILT);
        L.crown.set(L.voxel)
            .translate(0f, CROWN_Y, 0f)
            .mul(1f, 0f, 0f, 0f, 0f, ct, -st, 0f, 0f, st, ct, 0f, 0f, 0f, 0f, 1f);
        final double rx = L.eye[0] - cx, ry = L.eye[1] - cy, rz = L.eye[2] - cz;
        final double vx = rx * L.ex[0] + ry * L.ex[1] + rz * L.ex[2];
        final double vy = rx * L.ey[0] + ry * L.ey[1] + rz * L.ey[2];
        final double vz = rx * L.ez[0] + ry * L.ez[1] + rz * L.ez[2];
        L.camV[0] = (float) vx;
        L.camV[1] = (float) vy;
        L.camV[2] = (float) vz;
        final double py = vy - CROWN_Y;
        L.camC[0] = (float) vx;
        L.camC[1] = (float) (py * ct - vz * st);
        L.camC[2] = (float) (py * st + vz * ct);
    }

    /** {@code out} = the camera-relative world point of voxel (x, y, z). */
    private static void world(Look L, double x, double y, double z, double[] out) {
        out[0] = L.cx + L.ex[0] * x + L.ey[0] * y + L.ez[0] * z;
        out[1] = L.cy + L.ex[1] * x + L.ey[1] * y + L.ez[1] * z;
        out[2] = L.cz + L.ex[2] * x + L.ey[2] * y + L.ez[2] * z;
    }

    /** The beam's colours per grade: core, sheath (Magnetar violet, Ergomatter cyan, Superdense white). */
    private static final float[][] GRADE_CORE = { { 0.9f, 0.9f, 1f }, { 0.86f, 0.76f, 1f }, { 0.80f, 0.97f, 1f },
        { 1f, 1f, 1f } },
        GRADE_SHEATH = { { 0.6f, 0.6f, 0.8f }, { 0.66f, 0.51f, 1f }, { 0.33f, 0.83f, 0.94f }, { 0.82f, 0.86f, 1f } };

    /** The filament a flow comes down: its side's three, picked by the flow's hit tick. */
    static int flowFilament(EyeCosmosLook c) {
        final long h = c.flowAt * 0x9E3779B97F4A7C15L;
        return (c.flowSide < 0 ? 3 : 0) + (int) Math.floorMod(h >>> 33, 3L);
    }

    /**
     * The Great Attractor this frame: {@code now} world ticks with the partial tick, {@code time} the shader clock,
     * {@code detail} 3..1 (L0..L2), {@code day} 0..1, {@code trans} / {@code transP} the transition channel.
     */
    static void computeAttractor(Look L, EyeCosmosLook c, double now, float time, int detail, float day, int trans,
        float transP, double cx, double cy, double cz) {
        frame(L, c, cx, cy, cz);
        final boolean lensing = Config.renderBackgroundLensing;
        // The Infall: out of the Zone of Avoidance, the filaments and the lens come up over its first 600 s
        final float emerge = trans == TRANS_INFALL ? smooth(0, 600, transP * 1800.0) : 1f;
        final float phi = c == null ? 1f : c.phi;
        set(L.time, time, Math.max(0.25, Math.min(1, phi / 4.0)), emerge, 1);
        // the halo: as many dots as the galaxies held, on a log scale (Laniakea's 100,000 fill it)
        final int held = c == null ? 0 : c.held;
        final double v = c == null ? 0 : c.v;
        set(L.halo, clamp01(Math.log10(1 + held) / 5.0), Math.max(-1, Math.min(1, v / 40.0)), 1, CORE);
        set(L.web, 0, 0, 0, 1);
        set(L.fate, 0, 0, 0, 0);
        flow(L, c, now, detail);
        set(L.sky, SKY_RADIUS, 0.5 + 0.32 * day, 1, time);
        set(
            L.lens,
            lensing ? EINSTEIN * emerge : 0,
            (lensing ? 1 : 0.5) * emerge,
            detail >= 3 ? 1 : detail == 2 ? 0.8 : 0.6,
            detail >= 2 ? 2 : 1);
        // the dark core's faint rim, a little brighter with the galaxies held
        set(L.coreA, CORE, 0.55 + 0.05 * Math.log10(1 + held), 0, 0);
        set(L.coreB, 0.55, 0.62, 1, 0);
        // the beam: down the collimator stack, its colour the grade, its brightness and width rho
        final int grade = c == null ? 0 : Math.max(0, Math.min(3, c.grade));
        final double rho = c == null ? 0 : Math.min(3, c.rho);
        L.beamOn = grade > 0 && rho > 0.01;
        final MachineFx.Beam b = L.beam;
        b.w0 = b.w1 = (float) (0.3 + 0.25 * rho);
        b.intensity = (float) Math.max(0.2, Math.min(1.1, 0.35 + 0.5 * rho));
        b.speed = 26f;
        b.rate = (float) (1 + 1.5 * rho);
        b.strand = 0.4f;
        b.pitch = 9f;
        b.fadeIn = 3f;
        b.fadeOut = 2f;
        b.seed = 9.1f;
        b.time = time;
        System.arraycopy(GRADE_CORE[grade], 0, b.core, 0, 3);
        System.arraycopy(GRADE_SHEATH[grade], 0, b.sheath, 0, 3);
        world(L, 0, BEAM_TOP, 0, L.beamTop);
        world(L, 0, CORE * 1.02, 0, L.beamBottom);
        // the collars' glass channels pulse as the beam spools
        final float spool = c == null ? 0f : c.spool;
        L.ringsOn = L.beamOn && spool > 0.01f;
        set(L.ringA, 0.45, 1, time, spool);
        set(L.ringB, b.sheath[0], b.sheath[1], b.sheath[2], 0);
        set(L.ringC, CROWN_Y, CROWN_A, CROWN_B, CROWN_TILT);
        L.membraneOn = false;
        L.density = detail >= 3 ? 1f : detail == 2 ? 0.5f : 0.15f;
        L.glow[0] = 0.55f;
        L.glow[1] = 0.65f;
        L.glow[2] = 1f;
    }

    /** The Galactic Flow's group on its filament and the filament's highlight. */
    private static void flow(Look L, EyeCosmosLook c, double now, int detail) {
        set(L.flow, -1, 1, 0, 0);
        set(L.flow2, 0, 0, 0, 0);
        if (c == null || c.flow == EyeCosmosLook.FLOW_NONE) return;
        final double in = c.flowIn(now);
        if (Double.isNaN(in)) return;
        final int fil = flowFilament(c);
        final boolean declined = c.flow == EyeCosmosLook.FLOW_DECLINED || c.flow == EyeCosmosLook.FLOW_PASSING;
        final double uc;
        float light = 1f, highlight;
        if (in >= 0) {
            // the warning: seen far out, falling in to about 90 blocks
            uc = 1 - 0.55 * (1 - Math.min(1, in / EyeCosmosLook.FLOW_WARNING));
            light = smooth(0, 8, EyeCosmosLook.FLOW_WARNING - in);
            highlight = smooth(0, 10, EyeCosmosLook.FLOW_WARNING - in);
        } else {
            final double tau = -in;
            if (declined) {
                uc = 0.45 - 0.95 * Math.min(1, tau / EyeCosmosLook.FLOW_SURGE_SECONDS);
            } else if (tau <= 200) {
                uc = 0.45 - (0.45 - 0.07) * smooth(0, 200, tau);
            } else {
                uc = 0.07 - 0.05 * Math.min(1, (tau - 200) / 40);
            }
            highlight = 1 - smooth(220, 240, tau);
        }
        if (declined) highlight *= 0.5f;
        if (detail <= 1) highlight = 0;
        final int captured = declined ? 0 : (int) Math.floor(c.flowN * c.flowHeld + 1e-4);
        set(L.flow, fil, uc, declined ? 1 : 0, c.flowN);
        set(L.flow2, highlight, captured, 0, light);
        L.fate[2] = declined ? 1 : 0;
    }

    /** The Universe this frame (see {@link #computeAttractor}). */
    static void computeUniverse(Look L, EyeCosmosLook c, double now, float time, int detail, int trans, float transP,
        double cx, double cy, double cz) {
        frame(L, c, cx, cy, cz);
        double age = c == null ? Double.NaN : c.universeAge(now);
        float scale, reveal, flash = 0f, plasma = 0.5f;
        boolean show = true;
        if (trans == TRANS_INFLATION) {
            // Inflation (900 s): unfolding to 300 s, then the universe inflates from almost nothing; it turns
            // transparent at 600 s
            final double s = transP * 900.0;
            show = s >= 300;
            scale = (float) (0.04 + 0.96 * (1 - Math.exp(-Math.max(0, s - 300) / 25.0)));
            reveal = smooth(600, 630, s);
            flash = s >= 600 ? (float) (1.2 * Math.exp(-(s - 600) / 25.0)) : 0f;
            plasma = 0.5f + 1.2f * (float) Math.exp(-Math.max(0, s - 300) / 20.0);
            age = s >= 600 ? EyeCosmosLook.RECOMBINATION_AT + (s - 600) : Math.max(0, s - 300);
        } else if (Double.isNaN(age)) {
            age = EyeCosmosLook.UNIVERSE_SECONDS * 0.5;
            scale = 0.9f;
            reveal = 1f;
        } else {
            final double f = Math.max(0, Math.min(1, age / EyeCosmosLook.UNIVERSE_SECONDS));
            scale = (float) (0.62 + 0.32 * Math.pow(f, 2.0 / 3.0) + 0.06 * smooth(0.6, 1.0, f));
            reveal = smooth(EyeCosmosLook.RECOMBINATION_AT - 5, EyeCosmosLook.RECOMBINATION_AT + 25, age);
            final double since = age - EyeCosmosLook.RECOMBINATION_AT;
            if (since >= 0) flash = (float) (1.2 * Math.exp(-since / 25.0));
        }
        final float h = c == null ? 0f : c.h;
        scale *= 1f + 0.04f * Math.max(-1f, Math.min(1f, h / 40f));
        // the end: a Big Rip tears it outward, a Big Crunch pulls it in, 3 s
        final double fa = c == null ? Double.NaN : c.fateAge(now);
        final int fate = c == null || Double.isNaN(fa) ? 0 : c.fate;
        final float fk = fate == 0 ? 0f : clamp01(fa / 3.0);
        if (fate == EyeCosmosLook.FATE_RIP) scale *= 1f + 2.5f * fk * fk;
        if (fate == EyeCosmosLook.FATE_CRUNCH) scale *= (float) Math.pow(1 - fk, 1.5);
        L.membraneOn = show && scale > 0.01f;
        set(L.memA, MEMBRANE_A, MEMBRANE_B, scale, time);
        set(L.memB, detail >= 2 ? 0.62 : 0.55, reveal, flash, detail >= 3 ? 4 : detail == 2 ? 3 : 2);
        set(L.memC, fate, fk, 12.0 / (MEMBRANE_A * Math.max(0.05, scale)), plasma);
        // the web: scattered matter gathers onto filaments as the universe ages; the first stars light it at Dawn
        final double dawnIn = c == null ? Double.NaN : c.dawnIn(now);
        final double tau = Double.isNaN(dawnIn) ? -1e4 : -dawnIn;
        final float stars = tau >= 0 ? smooth(0, 20, tau) : 0f;
        final float growth = smooth(0.03, 0.85, age / EyeCosmosLook.UNIVERSE_SECONDS);
        set(L.web, show ? growth : 0, show ? 0.25 + 0.75 * reveal : 0, stars, scale);
        set(L.fate, fate, fk, 0, 0);
        set(L.time, time, 0, 1, 1);
        set(L.flow, -1, 1, 0, 0);
        set(L.flow2, 0, 0, 0, 0);
        set(L.halo, 0, 0, 0, CORE);
        // the Cosmic Dawn's rays: one after another round the crown, the 120 s warning a glint running round them
        final float warn = !Double.isNaN(dawnIn) && dawnIn > 0 && dawnIn <= 120 ? 1f : 0f;
        set(L.rayA, CROWN_Y, CROWN_A, CROWN_B, CROWN_TILT);
        set(L.rayB, 3.5, 42, 1.4, time);
        set(L.rayC, tau, warn, 0.15, fate == EyeCosmosLook.FATE_NONE ? 1 : 1 - fk);
        // the crown's glow: brighter at Recombination and through the starburst
        final double burst = tau >= 0 && tau < 300 ? Math.pow(1 + tau / 10.0, -5.0 / 3.0) : 0;
        set(L.ringA, 0.6, 1, time, 0);
        set(L.ringB, 0.80, 0.75, 1.0, 0.22 + 0.6 * burst + 0.3 * flash + 0.15 * warn);
        set(L.ringC, CROWN_Y, CROWN_A, CROWN_B, CROWN_TILT);
        L.ringsOn = true;
        // the faint Attractor below
        set(L.coreA, CORE, 0.4, 0, 0);
        set(L.coreB, 0.55, 0.62, 1, 0);
        // the jet still passes up through the centre (the beam retires at T10: a faint white jet)
        final MachineFx.Beam b = L.beam;
        L.beamOn = true;
        b.w0 = b.w1 = 0.35f;
        b.intensity = 0.35f;
        b.speed = 18f;
        b.rate = 0.6f;
        b.strand = 0.3f;
        b.pitch = 10f;
        b.fadeIn = 4f;
        b.fadeOut = 6f;
        b.seed = 4.7f;
        b.time = time;
        System.arraycopy(GRADE_CORE[3], 0, b.core, 0, 3);
        System.arraycopy(GRADE_SHEATH[3], 0, b.sheath, 0, 3);
        world(L, 0, CORE * 1.02, 0, L.beamBottom);
        world(L, 0, BEAM_TOP, 0, L.beamTop);
        L.density = detail >= 3 ? 1f : detail == 2 ? 0.5f : 0.15f;
        L.glow[0] = 1f;
        L.glow[1] = 0.72f;
        L.glow[2] = 0.55f;
    }

    // ================================================================== draws

    /** Pass 0: the dark core, opaque and writing depth; the caller saved the state. */
    static boolean drawCore(Look L) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = CORE_SPHERE.get();
        if (h == null || mesh == null) return false;
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glCullFace(GL11.GL_BACK);
        h.use();
        h.uploadModel(L.voxel);
        fx(h, 0, L.coreA, L.coreB, L.coreB, L.camV);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** Pass 1, the Attractor: the lensed backdrop, the points, the collar rings, the beam (emissive state set). */
    static void drawAttractor(Look L) {
        drawSky(L);
        drawPoints(L, ATTRACTOR_POINTS, true);
        if (L.ringsOn) drawRings(L, 0, 5);
        if (L.beamOn) {
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            MachineFx.drawBeam(L.beamTop, L.beamBottom, L.beam);
        }
    }

    /** Pass 1, the Universe: the jet, the membrane, the web, the crown's glow, the rays (emissive state set). */
    static void drawUniverse(Look L) {
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        MachineFx.drawBeam(L.beamBottom, L.beamTop, L.beam);
        if (L.membraneOn) drawMembrane(L);
        if (L.membraneOn) drawPoints(L, WEB_POINTS, false);
        drawRings(L, 5, 1);
        drawRays(L);
    }

    private static void drawSky(Look L) {
        final ShaderHandle h = SKY_PROGRAM.get();
        final IVertexArrayObject mesh = SKY_SPHERE.get();
        if (h == null || mesh == null) return;
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        h.use();
        h.uploadModel(MODEL.translation((float) L.cx, (float) L.cy, (float) L.cz));
        GL20.glUniform3f(h.loc(S_CENTRE), (float) L.cx, (float) L.cy, (float) L.cz);
        GL20.glUniform3f(h.loc(S_EYE), L.eye[0], L.eye[1], L.eye[2]);
        u4(h, S_SKY, L.sky);
        u4(h, S_LENS, L.lens);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    private static final Matrix4f MODEL = new Matrix4f();

    private static void drawPoints(Look L, BTSShaders.Mesh mesh, boolean attractor) {
        final ShaderHandle h = POINTS_PROGRAM.get();
        final IVertexArrayObject vao = mesh.get();
        if (h == null || vao == null) return;
        final boolean legacy = h.profile() == ShaderProfile.LEGACY;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glEnable(PROGRAM_POINT_SIZE);
        if (legacy) GL11.glEnable(POINT_SPRITE);
        h.use();
        h.uploadModel(attractor ? L.voxel : L.crown);
        GL20.glUniform3f(h.loc(P_POINTS), (float) Lod.pixelsPerBlockAtUnit(), 1.0f, 28f);
        u4(h, P_TIME, L.time);
        u4(h, P_FLOW, L.flow);
        u4(h, P_FLOW2, L.flow2);
        u4(h, P_HALO, L.halo);
        u4(h, P_WEB, L.web);
        u4(h, P_FATE, L.fate);
        final int count = vao.getVBO()
            .getVertexCount();
        final int keep = attractor ? FLOW_MEMBERS : 0;
        final int n = Math.max(1, Math.min(count, keep + Math.round((count - keep) * L.density)));
        vao.bind();
        vao.draw(0, n);
        vao.unbind();
        ShaderProgram.clear();
        if (legacy) GL11.glDisable(POINT_SPRITE);
        GL11.glDisable(PROGRAM_POINT_SIZE);
    }

    private static void drawRings(Look L, int first, int rings) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = RINGS.get();
        if (h == null || mesh == null) return;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        h.use();
        h.uploadModel(L.voxel);
        fx(h, 3, L.ringA, L.ringB, L.ringC, L.camV);
        mesh.bind();
        mesh.draw(first * RING_SEGMENTS * 6, rings * RING_SEGMENTS * 6);
        mesh.unbind();
        ShaderProgram.clear();
    }

    private static void drawRays(Look L) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = RAYS.get();
        if (h == null || mesh == null) return;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        h.use();
        h.uploadModel(L.voxel);
        fx(h, 1, L.rayA, L.rayB, L.rayC, L.camV);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
    }

    private static void drawMembrane(Look L) {
        final ShaderHandle h = FX_PROGRAM.get();
        final IVertexArrayObject mesh = DISC.get();
        if (h == null || mesh == null) return;
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        h.use();
        h.uploadModel(L.crown);
        fx(h, 2, L.memA, L.memB, L.memC, L.camC);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
    }

    private static void fx(ShaderHandle h, int mode, float[] a, float[] b, float[] c, float[] cam) {
        GL20.glUniform1f(h.loc(F_MODE), mode);
        u4(h, F_A, a);
        u4(h, F_B, b);
        u4(h, F_C, c);
        GL20.glUniform4f(h.loc(F_D), (float) (1.0 / Math.max(1e-3, Lod.pixelsPerBlockAtUnit())), 0f, 0f, 0f);
        GL20.glUniform3f(h.loc(F_CAM), cam[0], cam[1], cam[2]);
    }

    private static void u4(ShaderHandle h, Uniform u, float[] v) {
        GL20.glUniform4f(h.loc(u), v[0], v[1], v[2], v[3]);
    }

    /** L3: one glow sprite at the object's heart (the core's opaque sphere still draws in pass 0). */
    static void drawSprite(Look L, boolean universe) {
        final double[] p = L.beamTop;
        if (universe) {
            world(L, 0, CROWN_Y, 0, p);
            GlowSprite.draw(p[0], p[1], p[2], CROWN_A * 0.9, L.glow[0], L.glow[1], L.glow[2], 0.7f, 0.18f, 0.5f);
        } else {
            GlowSprite.draw(L.cx, L.cy, L.cz, EINSTEIN * 2.2, L.glow[0], L.glow[1], L.glow[2], 0.6f, 0.1f, 0.5f);
        }
    }

    // ================================================================== the fixed look (renderQuality=low)

    private static final double[] P = new double[3], Q = new double[3];

    /**
     * The fixed-function look (renderQuality=low, no GL 2.1, a program that does not bake): pass 0 the black core;
     * pass 1 (additive) the ring's glow, 40 dots per filament and the beam as a quad (the Attractor), or the
     * membrane as a two-tone disc in the crown's plane and the twelve rays (the Universe). A few hundred quads.
     */
    static void drawFixed(Look L, EyeCosmosLook c, boolean universe, int pass, double now) {
        final Tessellator t = Tessellator.instance;
        if (pass != 1) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glCullFace(GL11.GL_BACK);
            FixedShapes.sphere(t, L.cx, L.cy, L.cz, FixedShapes.outside(CORE), 0f, 0f, 0f, 1f, 0f, false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        if (pass == 0) return;
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        final Billboard bb = Billboard.at(L.cx, L.cy, L.cz);
        if (bb == null) return;
        final double[] u = bb.u, v = bb.v;
        FixedShapes
            .ring(t, L.cx, L.cy, L.cz, u, v, CORE * 1.05, EINSTEIN * 1.4, 0.5f, 0.6f, 1f, universe ? 0.12f : 0.22f);
        if (!universe) {
            t.startDrawingQuads();
            for (int f = 0; f < 6; f++) {
                for (int i = 0; i < 40; i++) {
                    final double s = (i + 0.5) / 40;
                    filament(f, s, P);
                    world(L, P[0], P[1], P[2], Q);
                    FixedShapes.billboardQuad(t, Q[0], Q[1], Q[2], u, v, 0.5 + 1.6 * s, 0.47f, 0.59f, 0.9f, 0.28f);
                }
            }
            t.draw();
            if (L.beamOn) {
                final float[] col = L.beam.sheath;
                fixedBeam(t, L, CORE, BEAM_TOP, 0.6, col[0], col[1], col[2], 0.25f + 0.3f * L.beam.intensity);
            }
            return;
        }
        // the Universe: the membrane as a disc (warm centre, cold rim) in the crown's plane, the rays
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        final float scale = Math.max(0.05f, L.memA[2]);
        t.startDrawingQuads();
        for (int i = 0; i < 48; i++) {
            final double a0 = 2 * Math.PI * i / 48, a1 = 2 * Math.PI * (i + 1) / 48;
            membraneQuad(t, L, a0, a1, 0.18, 0.6, scale, 1f, 0.6f, 0.3f, 0.32f, 0.7f, 0.6f, 0.65f, 0.32f);
            membraneQuad(t, L, a0, a1, 0.6, 1.0, scale, 0.7f, 0.6f, 0.65f, 0.32f, 0.25f, 0.4f, 0.9f, 0.0f);
        }
        t.draw();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        final double dawnIn = c == null ? Double.NaN : c.dawnIn(now);
        final double tau = Double.isNaN(dawnIn) ? -1 : -dawnIn;
        t.startDrawingQuads();
        for (int k = 0; k < 12; k++) {
            final double lk = tau - 0.45 * k;
            final float fl = lk > 0 && lk < 3 ? (float) Math.sin(Math.PI * lk / 3) : 0f;
            rayQuad(t, L, k, 0.12f + 0.7f * fl);
        }
        t.draw();
        fixedBeam(t, L, CORE, BEAM_TOP, 0.35, 1f, 1f, 1f, 0.2f);
    }

    /** One quad of the fixed membrane: radii r0..r1 (of the half-axes) between angles a0..a1, colours inner / outer. */
    private static void membraneQuad(Tessellator t, Look L, double a0, double a1, double r0, double r1, float s,
        float rA, float gA, float bA, float aA, float rB, float gB, float bB, float aB) {
        crownPoint(L, a0, r0 * s, Q);
        t.setColorRGBA_F(rA, gA, bA, aA);
        t.addVertex(Q[0], Q[1], Q[2]);
        crownPoint(L, a0, r1 * s, Q);
        t.setColorRGBA_F(rB, gB, bB, aB);
        t.addVertex(Q[0], Q[1], Q[2]);
        crownPoint(L, a1, r1 * s, Q);
        t.addVertex(Q[0], Q[1], Q[2]);
        crownPoint(L, a1, r0 * s, Q);
        t.setColorRGBA_F(rA, gA, bA, aA);
        t.addVertex(Q[0], Q[1], Q[2]);
    }

    /** {@code out} = the camera-relative world point of the membrane at angle {@code a}, {@code r} of its half-axes. */
    private static void crownPoint(Look L, double a, double r, double[] out) {
        final double x = MEMBRANE_A * r * Math.cos(a), z = MEMBRANE_B * r * Math.sin(a);
        world(L, x, CROWN_Y + z * Math.sin(CROWN_TILT), z * Math.cos(CROWN_TILT), out);
    }

    private static final double[] RA = new double[3], RB = new double[3];

    /** Ray {@code k} of the fixed Cosmic Dawn: from its spike's base outward (cosmos/fx_vert.glsl mode 1). */
    private static void rayQuad(Tessellator t, Look L, int k, float alpha) {
        final double th = Math.toRadians(30 * k + 15);
        final double bx = CROWN_A * Math.cos(th), by = CROWN_Y + CROWN_B * Math.sin(th) * Math.sin(CROWN_TILT),
            bz = CROWN_B * Math.sin(th) * Math.cos(CROWN_TILT);
        final double ph = Math.atan2(bz, bx), el = Math.toRadians(k % 3 == 0 ? 14 : k % 3 == 1 ? -4 : 6);
        final double dx = Math.cos(el) * Math.cos(ph), dy = Math.sin(el), dz = Math.cos(el) * Math.sin(ph);
        world(L, bx + dx * 3.5, by + dy * 3.5, bz + dz * 3.5, RA);
        world(L, bx + dx * 45, by + dy * 45, bz + dz * 45, RB);
        lineQuad(t, RA, RB, 1.2, 1f, 0.86f, 0.63f, alpha, 0f);
    }

    /** The fixed beam (or jet) up the axis from voxel y {@code y0} to {@code y1}. */
    private static void fixedBeam(Tessellator t, Look L, double y0, double y1, double hw, float r, float g, float b,
        float alpha) {
        world(L, 0, y0, 0, RA);
        world(L, 0, y1, 0, RB);
        t.startDrawingQuads();
        lineQuad(t, RA, RB, hw, r, g, b, alpha, alpha * 0.6f);
        t.draw();
    }

    /** A camera-facing quad from a to b (camera-relative), half-width hw, alphas at each end (quads mode is on). */
    private static void lineQuad(Tessellator t, double[] a, double[] b, double hw, float r, float g, float bl,
        float alphaA, float alphaB) {
        final double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        // across: the segment's direction x the view direction to its start
        double wx = dy * a[2] - dz * a[1], wy = dz * a[0] - dx * a[2], wz = dx * a[1] - dy * a[0];
        final double wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (wl < 1e-6) return;
        wx *= hw / wl;
        wy *= hw / wl;
        wz *= hw / wl;
        t.setColorRGBA_F(r, g, bl, alphaA);
        t.addVertex(a[0] - wx, a[1] - wy, a[2] - wz);
        t.addVertex(a[0] + wx, a[1] + wy, a[2] + wz);
        t.setColorRGBA_F(r, g, bl, alphaB);
        t.addVertex(b[0] + wx, b[1] + wy, b[2] + wz);
        t.addVertex(b[0] - wx, b[1] - wy, b[2] - wz);
    }

    /** The filament's centre line (cosmos/lib.glsl coFilPoint), voxel coordinates. */
    static void filament(int f, double u, double[] out) {
        final double side = f < 3 ? 1 : -1;
        final double j = f < 3 ? f : f - 3;
        final double az = Math.toRadians(side > 0 ? 35 + 55 * j : 215 + 55 * j);
        final double el = Math.toRadians(side * (20 + 6 * j));
        final double dx = Math.cos(el) * Math.cos(az), dy = Math.sin(el), dz = Math.cos(el) * Math.sin(az);
        // e1 = normalize(dir x up) = normalize(-dz, 0, dx); e2 = e1 x dir
        final double l = Math.sqrt(dz * dz + dx * dx);
        final double e1x = -dz / l, e1y = 0, e1z = dx / l;
        final double e2x = e1y * dz - e1z * dy, e2y = e1z * dx - e1x * dz, e2z = e1x * dy - e1y * dx;
        final double ph = 1.7 * f;
        final double ax = Math.cos(ph) * e1x + Math.sin(ph) * e2x, ay = Math.cos(ph) * e1y + Math.sin(ph) * e2y,
            az2 = Math.cos(ph) * e1z + Math.sin(ph) * e2z;
        final double bx = -Math.sin(ph) * e1x + Math.cos(ph) * e2x, by = -Math.sin(ph) * e1y + Math.cos(ph) * e2y,
            bz = -Math.sin(ph) * e1z + Math.cos(ph) * e2z;
        final double bend = Math.sin(Math.PI * u) * (14 + 6 * j), wig = Math.sin(2 * Math.PI * u) * 6;
        out[0] = u * 200 * dx + bend * ax + wig * bx;
        out[1] = u * 200 * dy + bend * ay + wig * by;
        out[2] = u * 200 * dz + bend * az2 + wig * bz;
    }

    // ================================================================== meshes and point buffers

    /** {@code n} ribbons of {@code segs} quads: position (across -1..1, along 0..1, ribbon id), UV (across, along). */
    private static IVertexArrayObject ribbons(ShaderHandle h, int n, int segs) {
        try (MeshBuilder m = MeshBuilder.of(h, n * segs * 6)) {
            for (int i = 0; i < n; i++) {
                for (int k = 0; k < segs; k++) {
                    final double s0 = (double) k / segs, s1 = (double) (k + 1) / segs;
                    m.vertex(-1, s0, i, 0, s0);
                    m.vertex(1, s0, i, 1, s0);
                    m.vertex(1, s1, i, 1, s1);
                    m.vertex(-1, s1, i, 0, s1);
                }
            }
            return m.build();
        }
    }

    /** A point list: position, group x 64 + filament x 8 + size, seed; shuffled after a fixed head. */
    static final class Pts {

        final float[] xyz, code, seed;
        int n;

        Pts(int capacity) {
            xyz = new float[capacity * 3];
            code = new float[capacity];
            seed = new float[capacity];
        }

        void add(double x, double y, double z, int group, int fil, double size, float s) {
            if (n == code.length) return;
            xyz[n * 3] = (float) x;
            xyz[n * 3 + 1] = (float) y;
            xyz[n * 3 + 2] = (float) z;
            code[n] = group * 64 + fil * 8 + (float) Math.max(0.05, Math.min(7.9, size));
            seed[n] = s;
            n++;
        }

        /** Shuffles every point from {@code from} on, so any prefix is a fair sample (LOD draws a prefix). */
        Pts shuffled(long seedValue, int from) {
            final Random r = new Random(seedValue);
            for (int i = n - 1; i > from; i--) {
                final int j = from + r.nextInt(i - from + 1);
                for (int k = 0; k < 3; k++) {
                    final float t = xyz[i * 3 + k];
                    xyz[i * 3 + k] = xyz[j * 3 + k];
                    xyz[j * 3 + k] = t;
                }
                final float c = code[i];
                code[i] = code[j];
                code[j] = c;
                final float s = seed[i];
                seed[i] = seed[j];
                seed[j] = s;
            }
            return this;
        }
    }

    private static IVertexArrayObject build(ShaderHandle h, Pts p) {
        final int stride = h.vertexFormat()
            .getVertexSize();
        if (stride != 20)
            throw new IllegalStateException("bts_copts: expected position + UV (20 bytes), got " + stride);
        final ByteBuffer buf = memAlloc(p.n * stride);
        try {
            for (int i = 0; i < p.n; i++) {
                final int base = i * stride;
                buf.putFloat(base, p.xyz[i * 3]);
                buf.putFloat(base + 4, p.xyz[i * 3 + 1]);
                buf.putFloat(base + 8, p.xyz[i * 3 + 2]);
                buf.putFloat(base + 12, p.code[i]);
                buf.putFloat(base + 16, p.seed[i]);
            }
            buf.position(0)
                .limit(p.n * stride);
            return VertexBufferType.IMMUTABLE.allocate(h.vertexFormat(), GL11.GL_POINTS, buf, p.n);
        } finally {
            memFree(buf);
        }
    }

    private static double gauss(Random r, double sigma, double cap) {
        return Math.max(-cap, Math.min(cap, r.nextGaussian() * sigma));
    }

    /**
     * The Great Attractor's points (13,212): the flow's group of 12 first, then (shuffled) 6 x 2,000 filament dust
     * points (45 % in three twisted strands, the rest a Gaussian haze round the braid), 600 streaming galaxies (100
     * per filament) and 600 halo dots.
     */
    static Pts attractorPoints() {
        final Random r = new Random(0x5EED_00A9L);
        final Pts p = new Pts(FLOW_MEMBERS + 6 * 2000 + 600 + 600);
        for (int i = 0; i < FLOW_MEMBERS; i++) {
            p.add(i, gauss(r, 0.6, 1.4), gauss(r, 0.6, 1.4), 3, 0, 2.2 + 0.8 * r.nextDouble(), r.nextFloat());
        }
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 2000; i++) {
                final double u0 = r.nextDouble();
                double a, b;
                if (r.nextDouble() < 0.45) {
                    final double ang = r.nextInt(3) * 2.0944 + u0 * 14 + f;
                    final double rr = 0.55 + gauss(r, 0.12, 0.4);
                    a = rr * Math.cos(ang);
                    b = rr * Math.sin(ang);
                } else {
                    a = gauss(r, 0.45, 1.3);
                    b = gauss(r, 0.45, 1.3);
                }
                p.add(u0, a, b, 0, f, 0.45 + 0.35 * r.nextDouble(), r.nextFloat());
            }
        }
        for (int i = 0; i < 600; i++) {
            final double s = r.nextDouble();
            p.add(
                r.nextDouble(),
                gauss(r, 0.6, 1.4),
                0.35 + 0.55 * r.nextDouble(),
                1,
                i % 6,
                1.3 + 1.6 * s * s,
                r.nextFloat());
        }
        for (int i = 0; i < 600; i++) {
            p.add(
                7.5 + 9 * r.nextDouble(),
                gauss(r, 0.5, 1.4),
                r.nextDouble() * 2 * Math.PI,
                2,
                0,
                0.42,
                r.nextFloat());
        }
        return p.shuffled(0xA9L, FLOW_MEMBERS);
    }

    /**
     * The Universe's web (20,000 points, crown-local): 90 nodes inside the membrane, each joined to its three nearest;
     * 20 points round each node, the rest along the edges in proportion to their length. Shuffled.
     */
    static Pts webPoints() {
        final Random r = new Random(0x5EED_00AAL);
        final int nodes = 90, total = 20_000, perNode = 20;
        final double[][] node = new double[nodes][];
        for (int i = 0; i < nodes; i++) {
            final double rr = Math.sqrt(r.nextDouble()) * 60, a = r.nextDouble() * 2 * Math.PI;
            node[i] = new double[] { rr * Math.cos(a) * 0.98, gauss(r, 1.5, 4), rr * Math.sin(a) * 0.92 };
        }
        final List<int[]> edges = new ArrayList<>();
        final Set<Long> seen = new HashSet<>();
        double sum = 0;
        final Integer[] order = new Integer[nodes];
        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) order[j] = j;
            final double[] from = node[i];
            Arrays.sort(order, (x, y) -> Double.compare(d2(from, node[x]), d2(from, node[y])));
            for (int k = 1; k <= 3; k++) {
                final int j = order[k];
                final long key = Math.min(i, j) * 1000L + Math.max(i, j);
                if (seen.add(key)) {
                    edges.add(new int[] { i, j });
                    sum += Math.sqrt(d2(node[i], node[j]));
                }
            }
        }
        final Pts p = new Pts(total);
        for (int i = 0; i < nodes; i++) {
            for (int k = 0; k < perNode; k++) {
                p.add(
                    node[i][0] + gauss(r, 1.1, 3),
                    node[i][1] + gauss(r, 0.6, 2),
                    node[i][2] + gauss(r, 1.1, 3),
                    5,
                    0,
                    0.55 + 0.3 * r.nextDouble(),
                    r.nextFloat());
            }
        }
        final int left = total - nodes * perNode;
        for (int[] e : edges) {
            final double[] a = node[e[0]], b = node[e[1]];
            final int count = (int) Math.round(left * Math.sqrt(d2(a, b)) / sum);
            for (int k = 0; k < count; k++) {
                final double s = r.nextDouble();
                p.add(
                    a[0] + (b[0] - a[0]) * s + gauss(r, 0.45, 1.5),
                    a[1] + (b[1] - a[1]) * s + gauss(r, 0.3, 1),
                    a[2] + (b[2] - a[2]) * s + gauss(r, 0.45, 1.5),
                    4,
                    0,
                    0.35 + 0.25 * r.nextDouble(),
                    r.nextFloat());
            }
        }
        while (p.n < total) {
            final double[] a = node[r.nextInt(nodes)];
            p.add(a[0] + gauss(r, 2, 5), a[1] + gauss(r, 1, 3), a[2] + gauss(r, 2, 5), 4, 0, 0.35, r.nextFloat());
        }
        return p.shuffled(0xAAL, 0);
    }

    private static double d2(double[] a, double[] b) {
        final double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return x * x + y * y + z * z;
    }
}
