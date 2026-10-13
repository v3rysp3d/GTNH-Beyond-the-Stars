package com.nateyburr.beyondthestars.machines.eye.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.Meshes;
import com.nateyburr.beyondthestars.client.render.NoiseTex;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.MeshBuilder;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 WP-3 / WP-4 (spec §7, §8): the Eye's stars and compact objects, three programs of our own.
 * <ul>
 * <li>{@code bts_star} (GLSL {@code shaders/star/surface.glsl}): the photosphere on a sphere mesh a little larger than
 * the star; each fragment traces its ray onto the true sphere (a perfect silhouette, the surface in 3D: no seams).
 * Granulation (cellular noise, boiling), sunspots with penumbrae and faculae, a giant's convection cells, a
 * protostar's blotches, the neutron star's hot polar caps under light bending, the white dwarf's cooling and the
 * black dwarf's crystal sheen, the magnetar's glowing crust faults, quadratic limb darkening, a helium-flash front.
 * Opaque, writes depth (pass 0).</li>
 * <li>{@code bts_starhalo} ({@code star/halo.glsl}): one quad in front of the star (or of the camera inside the
 * Eye's cavity) whose fragments integrate their rays analytically: glow, corona streamers, a giant's haze and wind,
 * a protostar's absorbing cocoon, dusty disc and outflows, lighthouse beams, the magnetosphere, two thin shells
 * (shock fronts with Rayleigh-Taylor fingers, the supernova remnant's filaments, flares, flashes). Premultiplied
 * ({@code ONE, ONE_MINUS_SRC_ALPHA}, pass 1).</li>
 * <li>{@code bts_starlines} ({@code star/lines.glsl}): up to 16 camera-facing ribbon loops placed in the vertex
 * shader: dipole field lines (neutron star, magnetar: twisted, crackling, torn open by a giant flare) or prominences
 * (Sol: rising and draining). Additive (pass 1).</li>
 * </ul>
 * {@link #compute} maps the synced look ({@link TileEyeStar}) to the uniforms; the renderer draws.
 */
@SideOnly(Side.CLIENT)
final class EyeStarFx {

    /** The look modes (star/lib.glsl ST_*). */
    static final float MODE_MAIN = 0, MODE_GIANT = 1, MODE_PROTO = 2, MODE_NEUTRON = 3, MODE_DWARF = 4,
        MODE_MAGNETAR = 5;
    /** Line slots in the loop mesh and segments per line. */
    static final int LINES = 16, LINE_SEGMENTS = 40;
    /** Nothing round a star may reach past the nearest block of the Eye (EyeStarRenderer.CLEARANCE). */
    static final float CLEARANCE = 20.4f;
    /** The magnetic axis leans this far off the spin axis (radians), as the fixed-function beams. */
    private static final double MAG_TILT = Math.toRadians(20);

    // ------------------------------------------------------------------ programs

    private static final ShaderRecipe STAR = BTSShaders.recipe("bts_star")
        .required("u_Centre", "u_Eye", "u_Star", "u_Mag", "u_Surf", "u_Surf2", "u_Fx", "u_Fx2", "u_Tint", "u_Turn")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .sampler("u_Blackbody", NoiseTex.LUT_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform S_CENTRE = STAR.uniform("u_Centre");
    private static final Uniform S_EYE = STAR.uniform("u_Eye");
    private static final Uniform S_STAR = STAR.uniform("u_Star");
    private static final Uniform S_MAG = STAR.uniform("u_Mag");
    private static final Uniform S_SURF = STAR.uniform("u_Surf");
    private static final Uniform S_SURF2 = STAR.uniform("u_Surf2");
    private static final Uniform S_FX = STAR.uniform("u_Fx");
    private static final Uniform S_FX2 = STAR.uniform("u_Fx2");
    private static final Uniform S_TINT = STAR.uniform("u_Tint");
    private static final Uniform S_TURN = STAR.uniform("u_Turn");
    static final BTSShaders.Program STAR_PROGRAM = BTSShaders.program("bts_star", STAR);
    private static final BTSShaders.Mesh SPHERE = STAR_PROGRAM.mesh(h -> Meshes.sphere(h, 64, 32));

    private static final ShaderRecipe HALO = BTSShaders.recipe("bts_starhalo")
        .required(
            "u_QC",
            "u_QU",
            "u_QV",
            "u_Centre",
            "u_Eye",
            "u_Star",
            "u_Mag",
            "u_Glow",
            "u_Corona",
            "u_Env",
            "u_Beam",
            "u_Shell",
            "u_Shell2",
            "u_ShellCol",
            "u_Shell2Col",
            "u_Flash",
            "u_HaloTint")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .sampler("u_Blackbody", NoiseTex.LUT_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform H_QC = HALO.uniform("u_QC");
    private static final Uniform H_QU = HALO.uniform("u_QU");
    private static final Uniform H_QV = HALO.uniform("u_QV");
    private static final Uniform H_CENTRE = HALO.uniform("u_Centre");
    private static final Uniform H_EYE = HALO.uniform("u_Eye");
    private static final Uniform H_STAR = HALO.uniform("u_Star");
    private static final Uniform H_MAG = HALO.uniform("u_Mag");
    private static final Uniform H_GLOW = HALO.uniform("u_Glow");
    private static final Uniform H_CORONA = HALO.uniform("u_Corona");
    private static final Uniform H_ENV = HALO.uniform("u_Env");
    private static final Uniform H_BEAM = HALO.uniform("u_Beam");
    private static final Uniform H_SHELL = HALO.uniform("u_Shell");
    private static final Uniform H_SHELL2 = HALO.uniform("u_Shell2");
    private static final Uniform H_SHELL_COL = HALO.uniform("u_ShellCol");
    private static final Uniform H_SHELL2_COL = HALO.uniform("u_Shell2Col");
    private static final Uniform H_FLASH = HALO.uniform("u_Flash");
    private static final Uniform H_TINT = HALO.uniform("u_HaloTint");
    static final BTSShaders.Program HALO_PROGRAM = BTSShaders.program("bts_starhalo", HALO);
    private static final BTSShaders.Mesh QUAD = HALO_PROGRAM.mesh(Meshes::quad);

    private static final ShaderRecipe LINES_RECIPE = BTSShaders.recipe("bts_starlines")
        .required("u_Centre", "u_Mag", "u_Lines", "u_Lines2", "u_Turn", "u_Pixel", "u_LineCol", "u_LineFx")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform L_CENTRE = LINES_RECIPE.uniform("u_Centre");
    private static final Uniform L_MAG = LINES_RECIPE.uniform("u_Mag");
    private static final Uniform L_LINES = LINES_RECIPE.uniform("u_Lines");
    private static final Uniform L_LINES2 = LINES_RECIPE.uniform("u_Lines2");
    private static final Uniform L_TURN = LINES_RECIPE.uniform("u_Turn");
    private static final Uniform L_PIXEL = LINES_RECIPE.uniform("u_Pixel");
    private static final Uniform L_COL = LINES_RECIPE.uniform("u_LineCol");
    private static final Uniform L_FX = LINES_RECIPE.uniform("u_LineFx");
    static final BTSShaders.Program LINES_PROGRAM = BTSShaders.program("bts_starlines", LINES_RECIPE);
    private static final BTSShaders.Mesh LINE_MESH = LINES_PROGRAM.mesh(EyeStarFx::lineMesh);

    private static final Matrix4f MODEL = new Matrix4f();

    private EyeStarFx() {}

    /** The loops: per line {@link #LINE_SEGMENTS} quads, position (line, s, side), UV (side 0..1, s). */
    private static IVertexArrayObject lineMesh(ShaderHandle h) {
        try (MeshBuilder m = MeshBuilder.of(h, LINES * LINE_SEGMENTS * 6)) {
            for (int i = 0; i < LINES; i++) {
                for (int k = 0; k < LINE_SEGMENTS; k++) {
                    final double s0 = (double) k / LINE_SEGMENTS, s1 = (double) (k + 1) / LINE_SEGMENTS;
                    m.vertex(i, s0, -1, 0, s0);
                    m.vertex(i, s0, 1, 1, s0);
                    m.vertex(i, s1, 1, 1, s1);
                    m.vertex(i, s1, -1, 0, s1);
                }
            }
            return m.build();
        }
    }

    // ================================================================== the look

    /** Every uniform of the three programs for one star this frame. */
    static final class Look {

        // photosphere
        final float[] star = new float[4], mag = new float[4], surf = new float[4], surf2 = new float[4],
            fx = new float[4], fx2 = new float[4], tint = new float[3], turn = new float[2];
        // halo
        final float[] glow = new float[4], corona = new float[4], env = new float[4], beam = new float[4],
            shell = new float[4], shell2 = new float[4], shellCol = new float[3], shell2Col = new float[3],
            flash = new float[4], haloTint = new float[3];
        float beamPulse;
        // loops
        final float[] lines = new float[4], lines2 = new float[4], lineCol = new float[4], lineFx = new float[4];
        /** Draw the photosphere / the loops. */
        boolean surface, loops;
        /** The eye relative to the render origin (first person: 0.1 blocks behind it). */
        final float[] eye = new float[3];
        /** How far the halo's effects reach from the centre, blocks. */
        float reach;

        void reset() {
            set(star, 1, 5772, 0, MODE_MAIN);
            set(mag, 0, 1, 0, 0);
            set(surf, 0.5f, 22, 0.35f, 0);
            set(surf2, 0.55f, 0.2f, 1f, 3);
            set(fx, 0, -1, 0, 0);
            set(fx2, 0, 0, 1.3f, 0);
            set3(tint, 1, 1, 1);
            turn[0] = 1;
            turn[1] = 0;
            set(glow, 0, 0.3f, 0, 1.6f);
            set(corona, 0.8f, 0, 0, 1.3f);
            set(env, 0, 0, 0, 0);
            set(beam, 0, CLEARANCE - 1, 0.25f, 0.02f);
            set(shell, 0, 1, 0, 0);
            set(shell2, 0, 1, 0, 0);
            set3(shellCol, 1, 1, 1);
            set3(shell2Col, 1, 1, 1);
            set(flash, 0, 0, 0, CLEARANCE);
            set3(haloTint, 1, 0.9f, 0.8f);
            beamPulse = 0;
            set(lines, 0, 0, 1, 0.1f);
            set(lines2, 0, 0, 1, 0);
            set(lineCol, 1, 1, 1, 0);
            set(lineFx, 0, 6, 0, 0);
            surface = true;
            loops = false;
            reach = CLEARANCE;
        }
    }

    private static void set(float[] a, float x, float y, float z, float w) {
        a[0] = x;
        a[1] = y;
        a[2] = z;
        a[3] = w;
    }

    private static void set3(float[] a, float x, float y, float z) {
        a[0] = x;
        a[1] = y;
        a[2] = z;
    }

    private static float clamp01(double x) {
        return (float) Math.max(0, Math.min(1, x));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smooth(float a, float b, float x) {
        final float t = clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    /** Temperatures of the tiers' stars (K): Sol, Sirius A, Arcturus. */
    private static final float[] T_STAR = { 5772, 5772, 9940, 4286 };
    /** Their lit radii (EyeStarLook.lit). */
    private static final float[] R_STAR = { 9, 9, 11, 15 };

    /**
     * The look of {@code tile} this frame. {@code radius} is the drawn radius (eased, wobbled), {@code now} world
     * ticks with the partial tick, {@code time} the shader clock (s), {@code detail} 0..3 (the LOD's octaves).
     */
    static void compute(Look L, TileEyeStar tile, byte kind, float radius, double now, float time, int detail) {
        L.reset();
        final float R = radius;
        final float pulse = tile.getPulse();
        final float aux = tile.getAux();
        final int n = tile.getTierN();
        final float unrest = clamp01((pulse - 0.02f) / 0.16f);
        final double spinRad = Math.toRadians((now * tile.getSpin()) % 360.0);
        final boolean slow = kind == EyeStarLook.STAR || kind == EyeStarLook.SWELL || kind == EyeStarLook.PROTOSTAR;
        final double turnRad = slow ? spinRad * 0.15 : spinRad;
        L.turn[0] = (float) Math.cos(turnRad);
        L.turn[1] = (float) Math.sin(turnRad);
        final float mx = (float) (Math.sin(MAG_TILT) * Math.cos(spinRad)), my = (float) Math.cos(MAG_TILT),
            mz = (float) (Math.sin(MAG_TILT) * Math.sin(spinRad));
        // the event channel: 0 at the warning, 1 at the hit; seconds since the hit (-1 none)
        final int ev = tile.getEvent();
        final double evIn = tile.eventIn(now);
        final boolean hasEv = ev != 0 && !Double.isNaN(evIn);
        final float warnF = hasEv && evIn > 0 ? clamp01(1 - evIn / 15.0) : 0f;
        final float hitAge = hasEv && evIn <= 0 ? (float) -evIn : -1f;
        final int trans = tile.getTransition();
        final float transP = tile.getTransitionProgress();
        final double age = tile.kindAge(now);
        L.star[0] = R;
        L.star[2] = time;
        L.surf2[3] = detail;

        switch (kind) {
            case EyeStarLook.STAR: {
                starLook(L, n, R, unrest, warnF, hitAge, ev, trans, transP, time, detail);
                // the ignition flash: the protostar's cocoon blown away
                if (tile.previousKind() == EyeStarLook.PROTOSTAR && age >= 0 && age < 60) {
                    final float f = (float) (age / 60);
                    set(L.shell, R * (1.05f + 1.2f * (float) Math.sqrt(f)), 1.5f + 2 * f, 0.05f * pow(1 - f, 1.5f), 4);
                    set3(L.shellCol, 1f, 0.9f, 0.7f);
                    set(L.shell2, R * (1.3f + f), 2.5f, 0.12f * (1 - f), 1);
                    set3(L.shell2Col, 0.55f, 0.25f, 0.12f);
                    L.fx[2] = 0.3f * pow(1 - f, 3);
                    L.flash[0] = 0.18f * (1 - f) * (1 - f);
                }
                break;
            }
            case EyeStarLook.PROTOSTAR: {
                final int k = n >= 1 && n <= 3 ? n : 1;
                final float c = clamp01((R / R_STAR[k] - 0.35f) / 0.45f);
                set(L.star, R, lerp(2300, T_STAR[k] * 0.85f, pow(c, 1.5f)), time, MODE_PROTO);
                set(L.surf2, 0.6f, 0.15f, 0.75f + 0.25f * c, detail);
                set(L.glow, 0.18f, 0.35f, 0, 1.4f);
                set(L.env, lerp(4.5f, 0.6f, c), lerp(0.25f, 0.95f, c), lerp(1.4f, 0.5f, c), 0.25f + 0.3f * c);
                set(L.fx2, 0, 0, 1.3f, 0);
                break;
            }
            case EyeStarLook.SWELL: {
                final float f = clamp01((R - 15) / 5);
                set(L.star, R, lerp(4286, 2700, f), time, MODE_GIANT);
                set(L.surf, 0.95f, lerp(2.6f, 3.2f, f), 0.05f * (1 + 10 * f), 0);
                set(L.surf2, 0.85f, 0.05f, lerp(1.0f, 0.7f, f), detail);
                set(L.glow, 0.14f, 0.25f, 0, 1.3f);
                set(L.corona, 0, 0.5f, lerp(0.32f, 0.6f, f), 1.3f);
                L.flash[2] = 0.8f;
                set3(L.haloTint, 1f, 0.3f, 0.1f);
                set(L.fx2, 0, 0, 1.25f, 0);
                break;
            }
            case EyeStarLook.NEUTRON:
            case EyeStarLook.BOUNCE: {
                final boolean bounce = kind == EyeStarLook.BOUNCE;
                set(L.star, R, bounce ? 26000 : 38000, time, MODE_NEUTRON);
                set(L.mag, mx, my, mz, bounce ? 0.4f : 1.0f);
                set(L.surf2, 0.25f, 0, 0.85f, detail);
                set(L.fx, 0, -1, 0, 0.4f);
                set(L.fx2, 0, 0, 1.2f, 0);
                set(L.glow, 0.16f, 0.7f, 0, 1.6f);
                if (!bounce) {
                    set(L.beam, 0.5f, CLEARANCE - 1, 0.2f, 0.018f);
                    L.loops = true;
                    set(L.lines, 0, 8, R * 3.4f, 0.05f);
                    set(L.lines2, 0.2f, 0, R, time);
                    set(L.lineCol, 0.45f, 0.6f, 1.0f, 0.45f);
                    // a glitch: the crust slips, a flash, the beams stutter
                    if (ev == EyeStarLook.EV_GLITCH && hitAge >= 0 && hitAge < 1.5f) {
                        final float g = hitAge / 1.5f;
                        L.fx[2] = 0.35f * (1 - g) * (1 - g);
                        L.beam[0] *= 0.35f + 0.65f * g;
                        set(L.shell, R * (1.2f + 3 * g), 0.6f, 0.04f * (1 - g), 4);
                        set3(L.shellCol, 0.7f, 0.85f, 1f);
                    }
                } else if (age >= 0 && age < 100) {
                    final float f = (float) (age / 100);
                    final float sr = 2.6f + 16 * f;
                    set(L.shell, sr, 0.8f + 0.12f * sr, 0.16f * pow(1 - f, 0.8f), 1);
                    set3(L.shellCol, 1f, 0.8f, 0.55f);
                    set(L.shell2, sr * 0.82f, 0.3f * sr, 0.025f * (1 - f), 5);
                    set3(L.shell2Col, 0.45f, 0.6f, 1f);
                    L.flash[0] = age < 10 ? 0.9f * (float) (1 - age / 10) : 0f;
                }
                break;
            }
            case EyeStarLook.NOVA: {
                final float f = clamp01(age / 60);
                L.surface = false;
                set(L.star, 1.2f, 30000, time, -1);
                final float sr = 15 + 30 * (float) Math.sqrt(f);
                set(L.shell, sr, 2.5f + 4 * f, 0.12f * pow(1 - f, 1.2f), 2);
                set3(L.shellCol, 0.55f, 0.72f, 1f);
                set(L.shell2, sr * 0.8f, 0.75f * sr, 0.03f * (1 - f), 5);
                set3(L.shell2Col, 0.5f, 0.65f, 1f);
                final float early = clamp01(1 - age / 18);
                set(L.flash, 2.4f * early * early + 0.15f * (1 - f), 0, 0, 80);
                set(L.glow, 1.2f * (1 - f), 3.0f, 0, 1.4f);
                L.reach = sr + 6;
                break;
            }
            case EyeStarLook.DWARF: {
                final float f = aux;
                float t, expo, ember = 0, crystal = 0;
                if (f <= 0.9f) {
                    t = 25000f * pow(2800f / 25000f, pow(f / 0.9f, 0.55f));
                    expo = lerp(0.42f, 1.0f, (float) Math.sqrt(clamp01((t - 2800) / 22200)));
                } else {
                    final float g = (f - 0.9f) / 0.1f;
                    t = lerp(2800, 1400, g);
                    expo = lerp(0.42f, 0.03f, (float) Math.sqrt(clamp01(g)));
                    ember = lerp(0.45f, 0.2f, g);
                    crystal = smooth(0.2f, 1.0f, g);
                }
                set(L.star, R, t, time, MODE_DWARF);
                set(L.surf2, 0.32f, 0.1f, expo, detail);
                set(L.fx2, crystal, ember, 1.25f, 0);
                set(L.glow, 0.42f * expo, 0.12f, 0, 1.4f);
                // the First Cooling: the white dwarf assembles (matter falling in, a contracting shell)
                if (trans == 4) {
                    final double ta = tile.transitionAge(now);
                    if (ta >= 0 && ta < 80) {
                        final float g = (float) (ta / 80);
                        set(L.shell, R * (4.5f - 3.4f * g), 0.8f, 0.03f * g * (1 - g) * 4, 4);
                        set3(L.shellCol, 0.75f, 0.85f, 1f);
                        L.flash[0] = g > 0.85f ? 0.5f * (1 - g) / 0.15f : 0f;
                    }
                }
                break;
            }
            case EyeStarLook.MAGNETAR: {
                magnetarLook(L, tile, R, aux, now, time, mx, my, mz, ev, warnF, hitAge, trans, transP);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Renders v2 WP-5, planets mode: a calm Sol of radius {@code R} (the planets' sun), at {@code detail}, with no
     * prominence loops (they would be finer than a pixel on so small a star).
     */
    static void sunLook(Look L, float R, float time, int detail) {
        L.reset();
        L.star[0] = R;
        L.star[2] = time;
        L.surf2[3] = detail;
        starLook(L, 1, R, 0, 0, -1, 0, 0, 0, time, detail);
        L.loops = false;
        L.reach = Math.min(CLEARANCE, R * 4f);
    }

    private static void starLook(Look L, int n, float R, float unrest, float warnF, float hitAge, int ev, int trans,
        float transP, float time, int detail) {
        final int k = n >= 1 && n <= 3 ? n : R < 10 ? 1 : R < 13 ? 2 : 3;
        float t = T_STAR[k];
        if (k == 1) {
            set(L.surf, 0.45f, 34, 0.35f * (1 + 4 * unrest), 0.35f + 0.65f * unrest);
            set(L.surf2, 0.55f, 0.2f, 1.35f, detail);
            set(L.glow, 0.3f, 0.22f, 0.65f * (1 + 0.5f * unrest), 1.9f);
            set(L.corona, 1.0f, unrest, 0, 1.3f);
            set3(L.haloTint, 1f, 0.8f, 0.55f);
            set(L.fx2, 0, 0, 1.6f, 0);
            // prominences: more of them on an active Sol
            L.loops = true;
            set(L.lines, 1, 2 + Math.round(3 * unrest), 0.22f, 0.3f);
            set(L.lines2, 0, 0, R, time);
            set(L.lineCol, 1f, 0.34f, 0.26f, 1.1f);
        } else if (k == 2) {
            set(L.surf, 0.22f, 34, 0.5f * (1 + 4 * unrest), 0);
            set(L.surf2, 0.38f, 0.12f, 0.95f, detail);
            set(L.glow, 0.18f, 0.3f, 1.3f, 1.4f);
            set(L.corona, 0.45f, unrest, 0, 1.3f);
            set3(L.haloTint, 0.78f, 0.86f, 1f);
            set(L.fx2, 0, 0, 1.8f, 0.3f);
            set3(L.tint, 0.5f, 0.68f, 1f);
        } else {
            L.star[3] = MODE_GIANT;
            set(L.surf, 0.8f, 2.6f, 0.05f * (1 + 4 * unrest + 6 * warnF), 0);
            set(L.surf2, 0.78f, 0.08f, 1.25f, detail);
            set(L.glow, 0.12f, 0.2f, 0, 1.3f);
            set(L.corona, 0, unrest, 0.32f, 1.32f);
            L.flash[2] = 0.55f;
            set3(L.haloTint, 1f, 0.42f, 0.16f);
            set(L.fx2, 0, 0, 1.2f, 0);
        }
        if (trans == 1) {
            // the Mass Infusion: Sol heats toward Sirius A while matter rains in along arcs
            final float p = transP;
            t = (float) Math.exp(lerp((float) Math.log(5772), (float) Math.log(9940), p * p));
            L.surf[0] = lerp(0.45f, 0.22f, p);
            L.glow[2] *= 1 + p;
            L.loops = true;
            set(L.lines, 1, 10, 0.75f, 0.35f);
            set(L.lines2, 0, 0, R, time);
            set(L.lineCol, 1f, 0.78f, 0.5f, 0.9f);
        } else if (trans == 2) {
            // the Shell Ignition: Sirius A swells and cools toward Arcturus, its cells growing
            final float p = transP;
            t = (float) Math.exp(lerp((float) Math.log(9940), (float) Math.log(4286), p));
            L.surf[1] = (float) Math.exp(lerp((float) Math.log(34), (float) Math.log(5), p));
            L.surf[0] = lerp(0.22f, 0.7f, p * p * p);
            L.surf2[2] = lerp(0.82f, 1.2f, p);
            L.fx2[2] = lerp(2.1f, 1.3f, p);
            L.corona[2] = 0.3f * p;
            L.corona[3] = 1.3f;
            set3(L.haloTint, lerp(0.78f, 1f, p), lerp(0.86f, 0.42f, p), lerp(1f, 0.16f, p));
        }
        L.star[1] = t;
        // the helium flash: the warning boils the cells and the star throbs, the hit sends a front across it
        if (ev == EyeStarLook.EV_HELIUM_FLASH) {
            if (warnF > 0) {
                L.surf[2] *= 1 + 6 * warnF;
                L.fx[2] = 0.08f * warnF * (0.5f + 0.5f * (float) Math.sin(time * (6 + 14 * warnF)));
            }
            if (hitAge >= 0 && hitAge < 3) {
                final float f = hitAge / 3;
                L.fx[1] = f;
                L.fx[2] = 0.15f * (1 - f) * (1 - f);
                set(L.shell, R * (1.02f + 0.22f * (float) Math.sqrt(f)), 0.9f, 0.07f * (1 - f), 4);
                set3(L.shellCol, 1f, 0.75f, 0.45f);
            }
        }
    }

    private static void magnetarLook(Look L, TileEyeStar tile, float R, float aux, double now, float time, float mx,
        float my, float mz, int ev, float warnF, float hitAge, int trans, float transP) {
        // EyeStarLook: aux is the warning (0 / 1); the overfed magnetar spins up from 8 to 24 degrees a tick
        final boolean warn = aux >= 0.99f;
        final float overfed = clamp01((tile.getSpin() - 8f) / 16f);
        final double fa = tile.flareAge(now);
        final float flare = fa >= 0 && fa < 40 ? (float) (fa / 40) : -1f;
        final float warnLevel = warn ? Math.max(warnF, 0.35f) : 0f;
        set(L.star, R, 30000, time, MODE_MAGNETAR);
        set(L.mag, mx, my, mz, 0.8f);
        set(L.fx, 0.12f + 0.4f * Math.max(warnLevel, overfed) + (flare >= 0 ? 0.35f * (1 - flare) : 0f), -1, 0, 0.3f);
        set(L.fx2, 0, 0, 1.2f, 0.75f);
        set3(L.tint, 0.55f, 0.3f, 1f);
        set(L.surf2, 0.35f, 0.1f, 0.8f, L.surf2[3]);
        set(L.glow, 0.25f, 0.6f, 0, 1.6f);
        L.flash[1] = 0.16f + 0.12f * overfed;
        set(L.beam, 0.38f, CLEARANCE - 1, 0.22f, 0.02f);
        if (flare >= 0) {
            set(L.shell, 4.5f + 4.5f * (float) Math.sqrt(flare), 1.2f, 0.05f * (1 - flare), 3);
            set3(L.shellCol, 0.75f, 0.5f, 1f);
        }
        if (warn) {
            // a warning pulses the magnetosphere at 2 Hz, faster as the flare nears (a shell here cost a quarter of
            // the budget at the hero view for almost nothing seen)
            L.flash[1] *= 1.2f + 0.6f * (float) Math.sin(time * 2 * Math.PI * (2 + 2 * warnF));
        }
        // the field: twisted by the unrest of a warning or an overfed star, crackling
        float twist = 0.6f + 2.2f * Math.max(warnLevel, overfed);
        float crackle = Math.max(warnLevel * 0.9f, overfed * 0.6f);
        float open = 0f;
        if (ev == EyeStarLook.EV_GIANT_FLARE && hitAge >= 0 && hitAge < 4) {
            final float g = hitAge / 4;
            L.flash[0] = 0.35f * (1 - g) * (1 - g);
            set(L.shell2, 3 + 15 * (float) Math.sqrt(g), 2 + 3 * g, 0.04f * (1 - g), 4);
            set3(L.shell2Col, 0.85f, 0.8f, 1f);
            L.fx[2] = 0.4f * (1 - g) * (1 - g);
            open = (float) Math.sin(Math.PI * Math.min(1, g * 1.4));
            crackle = 1f;
        }
        if (trans == 5) {
            // the Magnetar Merger: the dynamo winds the field up through the imprint, the release relaxes it
            final float p = transP;
            twist = p < 0.8f ? 0.6f + 3.4f * (p / 0.8f) : lerp(4f, 0.6f, (p - 0.8f) / 0.2f);
            crackle = Math.max(crackle, p < 0.8f ? 0.5f * (p / 0.8f) : 0.2f);
            L.fx[0] = Math.max(L.fx[0], 0.2f + 0.4f * Math.min(1, p / 0.8f));
            final double ta = tile.transitionAge(now);
            if (ta >= 0 && ta < 60) {
                // the merger itself: two white dwarfs become one star
                final float g = (float) (ta / 60);
                L.flash[0] = Math.max(L.flash[0], 0.4f * (1 - g) * (1 - g));
                set(L.shell, R * (1.2f + 5 * (float) Math.sqrt(g)), 1.2f, 0.045f * (1 - g), 4);
                set3(L.shellCol, 0.85f, 0.75f, 1f);
            }
        }
        L.loops = true;
        set(L.lines, 0, 12, R * 3.0f, 0.06f);
        set(L.lines2, twist, open, R, time);
        set(L.lineCol, 0.65f, 0.4f, 1f, 0.6f);
        set(L.lineFx, crackle, 6f + 6f * crackle, 0, 0);
    }

    private static float pow(float a, float b) {
        return (float) Math.pow(Math.max(a, 0f), b);
    }

    // ================================================================== draws

    /**
     * The photosphere (pass 0) round the camera-relative centre; the shared textures bound, the caller's GL state
     * (opaque, depth writes). False when the program is off.
     */
    static boolean drawSurface(double cx, double cy, double cz, Look l) {
        final ShaderHandle h = STAR_PROGRAM.get();
        final IVertexArrayObject mesh = SPHERE.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(S_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform3f(h.loc(S_EYE), l.eye[0], l.eye[1], l.eye[2]);
        u4(h, S_STAR, l.star);
        u4(h, S_MAG, l.mag);
        u4(h, S_SURF, l.surf);
        u4(h, S_SURF2, l.surf2);
        u4(h, S_FX, l.fx);
        u4(h, S_FX2, l.fx2);
        GL20.glUniform3f(h.loc(S_TINT), l.tint[0], l.tint[1], l.tint[2]);
        GL20.glUniform2f(h.loc(S_TURN), l.turn[0], l.turn[1]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /**
     * The halo (pass 1) on the quad {@code qc + x qu + y qv} (camera-relative); premultiplied blending set by the
     * caller. False when the program is off.
     */
    static boolean drawHalo(double cx, double cy, double cz, Look l, float[] qc, float[] qu, float[] qv) {
        final ShaderHandle h = HALO_PROGRAM.get();
        final IVertexArrayObject mesh = QUAD.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.identity());
        GL20.glUniform3f(h.loc(H_QC), qc[0], qc[1], qc[2]);
        GL20.glUniform3f(h.loc(H_QU), qu[0], qu[1], qu[2]);
        GL20.glUniform3f(h.loc(H_QV), qv[0], qv[1], qv[2]);
        GL20.glUniform3f(h.loc(H_CENTRE), (float) cx, (float) cy, (float) cz);
        GL20.glUniform3f(h.loc(H_EYE), l.eye[0], l.eye[1], l.eye[2]);
        u4(h, H_STAR, l.star);
        GL20.glUniform4f(h.loc(H_MAG), l.mag[0], l.mag[1], l.mag[2], l.beamPulse);
        u4(h, H_GLOW, l.glow);
        u4(h, H_CORONA, l.corona);
        u4(h, H_ENV, l.env);
        u4(h, H_BEAM, l.beam);
        u4(h, H_SHELL, l.shell);
        u4(h, H_SHELL2, l.shell2);
        GL20.glUniform3f(h.loc(H_SHELL_COL), l.shellCol[0], l.shellCol[1], l.shellCol[2]);
        GL20.glUniform3f(h.loc(H_SHELL2_COL), l.shell2Col[0], l.shell2Col[1], l.shell2Col[2]);
        u4(h, H_FLASH, l.flash);
        GL20.glUniform3f(h.loc(H_TINT), l.haloTint[0], l.haloTint[1], l.haloTint[2]);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    /** The loops (pass 1, additive); {@code pixel} blocks per pixel at one block. False when the program is off. */
    static boolean drawLoops(double cx, double cy, double cz, Look l, float pixel) {
        final ShaderHandle h = LINES_PROGRAM.get();
        final IVertexArrayObject mesh = LINE_MESH.get();
        if (h == null || mesh == null) return false;
        h.use();
        h.uploadModel(MODEL.translation((float) cx, (float) cy, (float) cz));
        GL20.glUniform3f(h.loc(L_CENTRE), (float) cx, (float) cy, (float) cz);
        u4(h, L_MAG, l.mag);
        u4(h, L_LINES, l.lines);
        u4(h, L_LINES2, l.lines2);
        GL20.glUniform2f(h.loc(L_TURN), l.turn[0], l.turn[1]);
        GL20.glUniform1f(h.loc(L_PIXEL), pixel);
        u4(h, L_COL, l.lineCol);
        u4(h, L_FX, l.lineFx);
        mesh.bind();
        mesh.draw();
        mesh.unbind();
        ShaderProgram.clear();
        return true;
    }

    private static void u4(ShaderHandle h, Uniform u, float[] v) {
        GL20.glUniform4f(h.loc(u), v[0], v[1], v[2], v[3]);
    }
}
