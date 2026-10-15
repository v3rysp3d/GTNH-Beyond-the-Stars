package com.nateyburr.beyondthestars.rendertest;

import java.util.HashMap;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.eye.render.CosmosInput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * P5 render QA: the client half of the Great Attractor's and the Universe's forced looks. The server half of each
 * case ({@link RenderTest#withCosmos}) forces the centre object's kind and transition; this sets the render input the
 * renderer reads ({@link CosmosInput#debug}), with every clock frozen, so each still catches a flow, a capture, the
 * Cosmic Dawn or a universe's end at a fixed point. The same looks are in the browser preview (render-preview.html).
 */
@SideOnly(Side.CLIENT)
final class CosmosLooks {

    private static final Map<String, CosmosInput> LOOKS = new HashMap<>();

    private CosmosLooks() {}

    private static CosmosInput put(String name) {
        final CosmosInput in = new CosmosInput().reset();
        LOOKS.put(name, in);
        return in;
    }

    private static CosmosInput beam(CosmosInput in, int grade, float rho, float rhoMax, int galaxies) {
        in.grade = (byte) grade;
        in.rho = rho;
        in.rhoMax = rhoMax;
        in.galaxies = galaxies;
        return in;
    }

    private static CosmosInput flow(CosmosInput in, int state, int side, int n, double flowIn, float held, long seed) {
        in.flowState = (byte) state;
        in.flowSide = (byte) side;
        in.flowSize = (byte) n;
        in.flowIn = flowIn;
        in.flowHeld = held;
        in.flowSeed = seed;
        return in;
    }

    private static CosmosInput universe(CosmosInput in, double age, double dawnIn) {
        in.uniAge = age;
        in.dawnIn = dawnIn;
        return in;
    }

    static {
        // ---- T9, the Great Attractor
        beam(put("eye_t9_superdense"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 4000);
        flow(
            beam(put("eye_t9_flow_warn"), CosmosInput.GRADE_SUPERDENSE, 1.3f, 8f, 4000),
            CosmosInput.FLOW_WARNED,
            1,
            10,
            60,
            0f,
            77_001L);
        final CosmosInput surge = flow(
            beam(put("eye_t9_flow_surge"), CosmosInput.GRADE_SUPERDENSE, 2.2f, 8f, 4000),
            CosmosInput.FLOW_SURGE,
            1,
            10,
            -120,
            1f,
            77_001L);
        surge.phi = 3.5f;
        beam(put("eye_t9_magnetar_nolens"), CosmosInput.GRADE_MAGNETAR, 0.6f, 2f, 40);
        beam(put("eye_t9_ergomatter"), CosmosInput.GRADE_ERGOMATTER, 1.5f, 4f, 25_000);
        flow(
            beam(put("eye_t9_declined"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 4000),
            CosmosInput.FLOW_PASSING,
            -1,
            12,
            -150,
            0f,
            99_107L);
        final CosmosInput capture = beam(put("eye_t9_capture"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 4010);
        capture.captureAge = 8;
        capture.captureN = 10;
        capture.flowSide = 1;
        capture.flowSeed = 77_001L;
        beam(put("eye_t9_infall"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 0).vRamp = 0.35f;
        beam(put("eye_t9_unfold"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 9000);
        beam(put("eye_t9_superdense_low"), CosmosInput.GRADE_SUPERDENSE, 1f, 8f, 4000);
        // ---- T10, the Universe
        universe(put("eye_t10_plasma"), 900, Double.NaN);
        universe(put("eye_t10_recomb"), 1810, Double.NaN);
        universe(put("eye_t10_dawn_warn"), 3500, 60);
        universe(put("eye_t10_dawn"), 3600, -2.5);
        universe(put("eye_t10_dawn_held"), 3640, -40);
        universe(put("eye_t10_late"), 6500, -2600);
        final CosmosInput rip = universe(put("eye_t10_rip"), 5000, -1200);
        rip.phase = CosmosInput.PHASE_RIP;
        rip.endAge = 1.5;
        final CosmosInput crunch = universe(put("eye_t10_crunch"), 5000, -1200);
        crunch.phase = CosmosInput.PHASE_CRUNCH;
        crunch.endAge = 2;
        put("eye_t10_inflation");
        put("eye_t10_inflation_rec");
        universe(put("eye_t10_dawn_low"), 3600, -2.5);
    }

    /** The client look of case {@code name}, or null (not a cosmic case). */
    static CosmosInput forCase(String name) {
        return LOOKS.get(name);
    }

    /** The case shows the stars unlensed ({@code renderBackgroundLensing} off). */
    static boolean noLensing(String name) {
        return name.endsWith("_nolens");
    }
}
