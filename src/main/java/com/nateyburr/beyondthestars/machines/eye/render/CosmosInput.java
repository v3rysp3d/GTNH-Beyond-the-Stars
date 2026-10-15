package com.nateyburr.beyondthestars.machines.eye.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * P5, client: what the Great Attractor's and the Universe's renders read this frame, in plain numbers (seconds
 * relative to now, no world ticks). It is the one place the render meets the machine's synced look: {@link #fill}
 * maps the render tile's P5 snapshot (p5-eye's {@code EyeP5Look}, see the P5 render interface) onto these fields, and
 * the render QA harness sets {@link #debug} instead. One mutable instance per renderer: a frame allocates nothing.
 */
@SideOnly(Side.CLIENT)
public final class CosmosInput {

    /** The look's kinds (EyeStarLook, P5 render interface): T9 the Great Attractor, T10 the Universe. */
    public static final byte ATTRACTOR = 12, UNIVERSE = 13;
    /** The transition channel (EyeStarLook.trans): The Infall (T8 to T9), Inflation (T9 to T10). */
    public static final int TRANS_INFALL = 8, TRANS_INFLATION = 9;

    /** The pellet grades: none, Magnetar (violet), Ergomatter (cyan), Superdense (white). */
    public static final byte GRADE_NONE = 0, GRADE_MAGNETAR = 1, GRADE_ERGOMATTER = 2, GRADE_SUPERDENSE = 3;
    /** The Galactic Flow: none, warned and accepted, warned and declined, surging, streaming past (declined). */
    public static final byte FLOW_NONE = 0, FLOW_WARNED = 1, FLOW_DECLINED = 2, FLOW_SURGE = 3, FLOW_PASSING = 4;
    /** A universe's phase (EyeP5Look.phase): the three ends run 3 s from {@link #endAge}. */
    public static final byte PHASE_RIP = 6, PHASE_CRUNCH = 7, PHASE_LAST_LIGHT = 8;
    /** A flow's warning and surge, a universe's batch and its Recombination, seconds. */
    public static final float FLOW_WARNING = 180f, FLOW_SURGE_SECONDS = 240f, UNIVERSE_SECONDS = 7200f,
        RECOMBINATION = 1800f;

    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}): when not null, {@link #fill} copies this instead of the
     * tile's snapshot, so a still catches a flow, a capture or the Cosmic Dawn at a fixed point. Null in play.
     */
    public static volatile CosmosInput debug;

    // ------------------------------------------------------------------ both
    /** The controller's horizontal facing (ForgeDirection ordinal 2..5), -1 unknown (drawn as north). */
    public int facing = -1;
    /** The transition channel and its progress 0..1. */
    public int trans;
    public float transP;

    // ------------------------------------------------------------------ T9
    public byte grade;
    /** rho = B / 10^10 GeV/s and the barrel's rho_max (brightness is rho against it). */
    public float rho, rhoMax = 8f;
    /** The flow phi = 1 + A (1 at rest, 4 at a group of 12's peak). */
    public float phi = 1f;
    public byte flowState, flowSide = 1, flowSize;
    /** Seconds to the group's arrival (the surge's start), negative after it; NaN none. */
    public double flowIn = Double.NaN;
    /** The flow's arrival tick (picks the filament on its side). */
    public long flowSeed;
    /** The share of the surge held Stable or better so far, 0..1. */
    public float flowHeld;
    /** Seconds since the last capture (its galaxies spiral into the halo), NaN none; how many it took. */
    public double captureAge = Double.NaN;
    public int captureN;
    /** Galaxies held (the halo fills with them, log scale). */
    public int galaxies;
    /** V's ramp-in over The Infall's first 600 s (1 outside it). */
    public float vRamp = 1f;
    /** The V gauge if the snapshot carries it (the halo's tint), else NaN. */
    public float v = Float.NaN;

    // ------------------------------------------------------------------ T10
    /** Seconds into the running universe, NaN none (forming). */
    public double uniAge = Double.NaN;
    /** The universe's phase (EyeP5Look.phase), 0 unknown. */
    public byte phase;
    /** Seconds to Recombination (negative after it), NaN: taken as 1,800 s into the universe. */
    public double recIn = Double.NaN;
    /** Seconds to the Cosmic Dawn (negative after it), NaN none. */
    public double dawnIn = Double.NaN;
    /** The H gauge (expansion error): the membrane breathes with it. */
    public float h;
    /** Seconds since the last universe ended (phases 6..8 run 3 s), NaN none. */
    public double endAge = Double.NaN;

    /** Back to the quiet look. */
    public CosmosInput reset() {
        facing = -1;
        trans = 0;
        transP = 0;
        grade = 0;
        rho = 0;
        rhoMax = 8f;
        phi = 1f;
        flowState = 0;
        flowSide = 1;
        flowSize = 0;
        flowIn = Double.NaN;
        flowSeed = 0;
        flowHeld = 0;
        captureAge = Double.NaN;
        captureN = 0;
        galaxies = 0;
        vRamp = 1f;
        v = Float.NaN;
        uniAge = Double.NaN;
        phase = 0;
        recIn = Double.NaN;
        dawnIn = Double.NaN;
        h = 0;
        endAge = Double.NaN;
        return this;
    }

    /** Copies {@code o} into this. */
    public CosmosInput copy(CosmosInput o) {
        facing = o.facing;
        trans = o.trans;
        transP = o.transP;
        grade = o.grade;
        rho = o.rho;
        rhoMax = o.rhoMax;
        phi = o.phi;
        flowState = o.flowState;
        flowSide = o.flowSide;
        flowSize = o.flowSize;
        flowIn = o.flowIn;
        flowSeed = o.flowSeed;
        flowHeld = o.flowHeld;
        captureAge = o.captureAge;
        captureN = o.captureN;
        galaxies = o.galaxies;
        vRamp = o.vRamp;
        v = o.v;
        uniAge = o.uniAge;
        phase = o.phase;
        recIn = o.recIn;
        dawnIn = o.dawnIn;
        h = o.h;
        endAge = o.endAge;
        return this;
    }

    /**
     * This frame's input for {@code tile} at world tick {@code now} (with the partial tick): the harness's
     * {@link #debug} look, else the tile's synced P5 snapshot. The tile's transition channel always wins.
     * <p>
     * Until p5-eye's {@code EyeP5Look} lands on this branch the tile has no snapshot, so a lit T9 / T10 draws the quiet
     * look (no beam, no flow, a mid-life universe). The wiring, against the P5 render interface, is:
     *
     * <pre>
     * final EyeP5Look p = tile.getP5();
     * if (p != null) {
     *     facing = p.facing; // render side needs: the controller's facing
     *     grade = p.beamGrade;
     *     rho = p.beamRho;
     *     rhoMax = p.beamRhoMax;
     *     phi = p.phi;
     *     flowState = p.flowState;
     *     flowSide = p.flowSide;
     *     flowSize = p.flowSize;
     *     flowSeed = p.flowAt;
     *     flowIn = tile.flowIn(now);
     *     flowHeld = p.flowHeld;
     *     captureAge = p.captureAt == 0 ? Double.NaN : (now - p.captureAt) / 20.0;
     *     captureN = p.captureN;
     *     galaxies = p.galaxies;
     *     vRamp = p.vRamp;
     *     uniAge = tile.universeAge(now);
     *     phase = p.phase;
     *     recIn = tile.recombinationIn(now);
     *     dawnIn = tile.dawnIn(now);
     *     h = p.H;
     *     endAge = tile.endAge(now);
     * }
     * </pre>
     */
    public CosmosInput fill(TileEyeStar tile, double now) {
        final CosmosInput forced = debug;
        if (forced != null) {
            copy(forced);
        } else {
            reset();
        }
        trans = tile.getTransition();
        transP = tile.getTransitionProgress();
        return this;
    }
}
