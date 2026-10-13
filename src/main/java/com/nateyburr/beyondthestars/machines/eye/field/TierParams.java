package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.L;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.OM;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.W;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D-49 stability field: the per-tier field constants, one table (d49-build-plan.md §5; the prototype's
 * {@code TIERS}). Everything here is tunable, but the golden tests (tools/fieldsim-golden.js for T1-T6,
 * tools/quasar-golden.js for T6-T8) pin the prototypes' values: change a number here and regenerate the golden file
 * from an updated prototype, or the test fails.
 * <p>
 * Upkeep fluids and amounts are NOT here: they belong to the star definitions of the runtime (plan §6.1, unit 4).
 * The field only sees the upkeep fraction r.
 * <p>
 * <b>P4 (quasar-p4.md §4).</b> T7 Triangulum and T8 Milky Way are burst-driven: Θ, Ω, Π and the shield retire; P is
 * driven by β (bursts against the demand D_T), W by the precession sense and the rail's high/low banks, Λ (T8) by the
 * Ergomatter feed and the halo. {@link #PLANETS7} / {@link #PLANETS8} are planets mode's lighter one-gauge field
 * (3 / 6 slots). {@link #of(int)} returns the galaxy table for 7 and 8; {@link #of(int, boolean)} picks the mode.
 */
public enum TierParams {

    /** Sol: P only, self-settling (unattended it settles at P about +35, Stable). */
    T1(1, "Sol", new int[] { P }, 0.5, gains(-0.02, 0, 0, 0), null, F.IGNITE, 50000,
        new TrimDef[] { trim(TrimDef.Kind.HELIUM, 10, -2, 0, 0, 0, null),
            trim(TrimDef.Kind.HYDROGEN, 10, 2, 0, 0, 0, null) }),
    /** Sirius A: P drifting; Helium leaves ash (yield × max(0, 1 − 0.005·u_He/p)). */
    T2(2, "Sirius A", new int[] { P }, 2.0, gains(-0.01, 0, 0, 0), null, F.IGNITE | F.ASH, 2500,
        new TrimDef[] { trim(TrimDef.Kind.HELIUM, 20, -2, 0, 0, 0, "ash: -0.5 % yield per unit"),
            trim(TrimDef.Kind.HYDROGEN, 20, 2, 0, 0, 0, null) }),
    /** Arcturus: P and Θ, P gains +0.02·Θ, Θ runs away; warned helium flashes. */
    T3(3, "Arcturus", new int[] { P, TH }, 1.5, gains(-0.01, 0.01, 0, 0),
        new EventDef("Helium flash", 1200, 2400, 15, 3, 0, 30, 0, 0, false), F.IGNITE, 625,
        new TrimDef[] { trim(TrimDef.Kind.HELIUM, 20, -1, 3, 0, 0, null),
            trim(TrimDef.Kind.HYDROGEN, 20, 3, 0.5, 0, 0, null),
            trim(TrimDef.Kind.SUPER_COOLANT, 20, -1, -3, 0, 0, null) }),
    /** Neutron star (Arcturus Remnant): P, Θ, Ω; the HSM feed pushes; unwarned glitches. Product = 3.6864 × HSM. */
    T4(4, "Neutron Star", new int[] { P, TH, OM }, 2.0, gains(-0.01, 0, 0.02, 0),
        new EventDef("Glitch", 900, 1800, 0, 1, 0, 0, 20, 0, false), F.FEED, 0,
        new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null),
            trim(TrimDef.Kind.SPACETIME, 10, 2, 0, 0, 0, null), trim(TrimDef.Kind.SUPER_COOLANT, 10, 0, -3, 0, 0, null),
            trim(TrimDef.Kind.TENGAM, 5, 0, 1, -4, 0, null) }),
    /** Sirius B: P and Θ against the cooling setpoint; 7,200 Eye-second batches. */
    T5(5, "Sirius B", new int[] { P, TH }, 1.5, gains(-0.01, 0.01, 0, 0), null, F.BATCH, 4608,
        new TrimDef[] { trim(TrimDef.Kind.DSSF, 10, -0.5, -1, 0, 0, null),
            trim(TrimDef.Kind.RAW_STAR_MATTER, 5, 0, 3, 0, 0, null), trim(TrimDef.Kind.HELIUM, 10, 2, 0, 0, 0, null) }),
    /** SGR 1806-20, the magnetar: P, Θ, Ω, Π plus the Agent shield A; signed, warned giant flares. */
    T6(6, "SGR 1806-20", new int[] { P, TH, OM, PI }, 2.0, gains(-0.01, 0, 0.02, 0.03),
        new EventDef("Giant flare", 60, 120, 15, 4, 0, 0, 0, 25, true), F.SHIELD, 15,
        new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null),
            trim(TrimDef.Kind.SPACETIME, 10, 2, 0, 0, 0, null), trim(TrimDef.Kind.SUPER_COOLANT, 10, 0, -3, 0, 0, null),
            trim(TrimDef.Kind.TENGAM, 5, 0, 1, -4, 0, null),
            trim(TrimDef.Kind.AGENT, 5, 0, 0.5, 0, 0, "shield A +4 per unit; the upkeep adds 2r/s") }),
    /**
     * P4 T7 Triangulum (galaxy mode): P (bursts against D₇) and W (the tilted rail's warp); signed, warned minor
     * mergers flip the precession. Product: Ergomatter 600 × mean Y × r × min(β̄, 1.5). Space is the only trim.
     */
    T7(7, "Triangulum", new int[] { P, W }, 1, sig(1, 1.5, 0), gains6(-0.01, 0.02, 0), GalaxyField.MINOR_MERGER,
        F.GALAXY, GalaxyField.T7_ERGOMATTER, 0, new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null) }),
    /**
     * P4 T8 Milky Way (galaxy mode): P, W and Λ (tidal shear, driven by the Ergomatter feed f₈, held by halo bursts);
     * minor mergers and the satellite crossing. Product: Superdense Ergomatter 1.0 × Ergomatter fed × mean Y × r.
     */
    T8(8, "Milky Way", new int[] { P, W, L }, 1, sig(1, 1.5, 1.5), gains6(-0.01, 0.02, 0.02), GalaxyField.MINOR_MERGER,
        F.GALAXY | F.ERGO, 0, 0, new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null) }),
    /** P4 planets mode at T7: 3 slots; one gauge P, d = 1·(β − 1), σ 0.5, no events, D = 0.25·D₇. */
    PLANETS7(7, "3 planets", new int[] { P }, 0.5, sig(0.5, 0, 0), gains6(-0.01, 0, 0), null, F.PLANETS, 0, 3,
        new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null) }),
    /** P4 planets mode at T8: 6 slots; the same light field at D = 0.25·D₈. */
    PLANETS8(8, "6 planets", new int[] { P }, 0.5, sig(0.5, 0, 0), gains6(-0.01, 0, 0), null, F.PLANETS, 0, 6,
        new TrimDef[] { trim(TrimDef.Kind.SPACE, 10, -2, 0, 0, 0, null) });

    /** Tier flags (a nested holder: an enum's constant arguments cannot forward-reference its own statics). */
    static final class F {

        static final int IGNITE = 1;
        static final int ASH = 2;
        static final int FEED = 4;
        static final int BATCH = 8;
        static final int SHIELD = 16;
        /** P4: burst-driven P and W (and Λ on T8), the β sum, the cycle product from bursts. */
        static final int GALAXY = 32;
        /** P4: planets mode's one-gauge burst field; slot runs instead of a cycle product. */
        static final int PLANETS = 64;
        /** P4 T8: the Ergomatter feed (Λ drift, the product) and the satellite crossing. */
        static final int ERGO = 128;
    }

    /** Star tier 1..8. */
    public final int n;
    /** The prototype's star name (display only). */
    public final String star;
    /** Active gauges in update order (ascending P, Θ, Ω, Π, W, Λ). */
    private final int[] gauges;
    /** Noise half-width σ (the per-gauge table {@link #sigma(int, int)} wins when present). */
    public final double sigma;
    /** P4: σ per gauge, or null (T1-T6 use {@link #sigma}). */
    private final double[] sigmaK;
    /** Feedback gain g per gauge (negative restores, positive runs away). */
    private final double[] gain;
    /** The tier's event, or null. */
    public final EventDef event;
    /** T1-T3: lit by an ignition (Stellar Ignition Charge). */
    public final boolean ignite;
    /** T2: Helium trims cut the yield. */
    public final boolean ash;
    /** T4: the HSM feed drives the drift and the product. */
    public final boolean feed;
    /** T5: cooling batches, Θ against the setpoint. */
    public final boolean batch;
    /** T6: the Agent shield A absorbs Π. */
    public final boolean shield;
    /** P4 T7/T8 galaxy mode: burst-driven P/W(/Λ), the β sum, the product from bursts. */
    public final boolean galaxy;
    /** P4 T8: the Ergomatter feed f₈ (Λ's drift and the product) and the satellite crossing. */
    public final boolean ergoFeed;
    /** P4 planets mode: the number of planet slots (0 elsewhere). */
    public final int slots;
    /**
     * Product per 60 s cycle at p = 1, before × mean Y × r (T4: 0, the product is 3.6864 × HSM fed; T5: per batch; T7:
     * also × min(β̄, 1.5); T8: 0, the product is 1.0 × Ergomatter fed; planets: none).
     */
    public final double productBase;
    /** Trims in table order (the drain order in play). */
    private final List<TrimDef> trims;

    TierParams(int n, String star, int[] gauges, double sigma, double[] gain, EventDef event, int flags,
        double productBase, TrimDef[] trims) {
        this(n, star, gauges, sigma, null, gain, event, flags, productBase, 0, trims);
    }

    TierParams(int n, String star, int[] gauges, double sigma, double[] sigmaK, double[] gain, EventDef event,
        int flags, double productBase, int slots, TrimDef[] trims) {
        this.n = n;
        this.star = star;
        this.gauges = gauges;
        this.sigma = sigma;
        this.sigmaK = sigmaK;
        this.gain = gain;
        this.event = event;
        this.ignite = (flags & F.IGNITE) != 0;
        this.ash = (flags & F.ASH) != 0;
        this.feed = (flags & F.FEED) != 0;
        this.batch = (flags & F.BATCH) != 0;
        this.shield = (flags & F.SHIELD) != 0;
        this.galaxy = (flags & F.GALAXY) != 0;
        this.ergoFeed = (flags & F.ERGO) != 0;
        this.slots = slots;
        this.productBase = productBase;
        List<TrimDef> list = new ArrayList<>();
        Collections.addAll(list, trims);
        this.trims = Collections.unmodifiableList(list);
    }

    private static double[] gains(double p, double th, double om, double pi) {
        return new double[] { p, th, om, pi, 0, 0 };
    }

    /** P4 gains on P, W, Λ (Θ, Ω, Π retired). */
    private static double[] gains6(double p, double w, double l) {
        return new double[] { p, 0, 0, 0, w, l };
    }

    /** P4 σ on P, W, Λ. */
    private static double[] sig(double p, double w, double l) {
        return new double[] { p, 0, 0, 0, w, l };
    }

    private static TrimDef trim(TrimDef.Kind kind, double cap, double p, double th, double om, double pi, String note) {
        return new TrimDef(kind, cap, p, th, om, pi, note);
    }

    /** The tier with number {@code n} (1..8; galaxy mode for 7 and 8). */
    public static TierParams of(int n) {
        return of(n, false);
    }

    /** The tier with number {@code n}; for 7 and 8, planets mode when {@code planets}. */
    public static TierParams of(int n, boolean planets) {
        if (planets && n == 7) return PLANETS7;
        if (planets && n == 8) return PLANETS8;
        for (TierParams t : values()) if (t.n == n) return t;
        throw new IllegalArgumentException("no field tier " + n);
    }

    /** P4: true for the burst-driven tables (galaxy and planets mode, T7+). */
    public boolean burstDriven() {
        return galaxy || slots > 0;
    }

    /** True for planets mode. */
    public boolean planets() {
        return slots > 0;
    }

    /** The active gauges in update order. Do not modify the returned array. */
    public int[] gauges() {
        return gauges;
    }

    /** True when gauge {@code k} is active on this tier. */
    public boolean active(int k) {
        for (int g : gauges) if (g == k) return true;
        return false;
    }

    /** Feedback gain on gauge {@code k}. */
    public double gain(int k) {
        return gain[k];
    }

    /**
     * Noise half-width on gauge {@code k}: σ on T1-T6; the per-gauge table on T7+, where P gains +0.25 per companion
     * (written as the prototype's {@code 1+0.25*nc}).
     */
    public double sigma(int k, int companions) {
        if (sigmaK == null) return sigma;
        return k == P ? sigmaK[P] + GalaxyField.COMPANION_SIGMA_P * companions : sigmaK[k];
    }

    /** Trims in table order. */
    public List<TrimDef> trims() {
        return trims;
    }

    /** This tier's trim of that kind, or null. */
    public TrimDef trim(TrimDef.Kind kind) {
        for (TrimDef t : trims) if (t.kind == kind) return t;
        return null;
    }

    /**
     * Drift d per Eye-second for T1-T6 (plan §5). {@code feed} is the T4 HSM feed in L/s per parallel; {@code r} is
     * the cycle's upkeep fraction (the T6 feed push). Expressions are written exactly as the prototype's (rounding).
     * The P4 tiers use {@link #burstDrift}; here they return zeros.
     */
    public double[] drift(double feed, double r) {
        switch (n) {
            case 1:
                return new double[] { 0.7, 0, 0, 0, 0, 0 };
            case 2:
                return new double[] { 2, 0, 0, 0, 0, 0 };
            case 3:
                return new double[] { 1.5, 1, 0, 0, 0, 0 };
            case 4:
                return new double[] { -3 + 5 * feed, -0.5 + 3 * feed, 1.5 * feed, 0, 0, 0 };
            case 5:
                return new double[] { -1, 0.02, 0, 0, 0, 0 };
            case 6:
                return new double[] { -4 + 5 * r, -0.5 + 3 * r, r, 0, 0, 0 };
            default:
                return new double[StellarField.GAUGES];
        }
    }

    /**
     * P4 drift d (quasar-p4.md §4.2, the prototype's {@code galaxyTier(..).d} / {@code planetsTier(..).d}):
     * planets P = 1·(β − 1); galaxy P = 3·(β − 1), W = 1.5·s, and on T8 Λ = 0.4·f₈·{@code rampL} (the Galactic
     * Merger ramps Λ in; 1 otherwise).
     */
    public double[] burstDrift(FieldState s, StepInputs in, double rampL) {
        final double[] d = new double[StellarField.GAUGES];
        if (slots > 0) {
            d[P] = GalaxyField.P_PER_BETA_PLANETS * (in.beta - 1);
            return d;
        }
        d[P] = GalaxyField.P_PER_BETA * (in.beta - 1);
        d[W] = GalaxyField.W_DRIFT * s.prec;
        if (ergoFeed) d[L] = GalaxyField.L_PER_F8 * in.f8 * rampL;
        return d;
    }

    /**
     * P4 burst terms on gauge {@code k} (not × m; the prototype's {@code T.bx}): W +6·(γ_lo − γ_hi); Λ −10·γ_halo
     * (T8). 0 elsewhere and off the galaxy tiers.
     */
    public double burstTerm(int k, StepInputs in) {
        if (!galaxy) return 0;
        if (k == W) return GalaxyField.W_PER_GAMMA * (in.gLo - in.gHi);
        if (k == L && ergoFeed) return GalaxyField.L_PER_HALO * in.gHalo;
        return 0;
    }

    /** Coupling c_k(x): T3's P gains +0.02·Θ; zero elsewhere. */
    public double coupling(double[] x, int k) {
        return n == 3 && k == P ? 0.02 * x[TH] : 0;
    }

    /**
     * The design invariant (plan §5): for every active gauge, the strongest actuator must out-push the worst case,
     * {@code authority > m_max·(|d| + σ + |g|·40)}, with m_max at Time Acceleration tier 9 and d at its extremes
     * (T4 feed 0 and 1.5 L/s, T6 r 0 and 1). T6's Π is held by the shield (4 per Agent trim unit; the upkeep's 2·r/s is
     * not counted, r may be 0). The P4 tiers are checked against their bursts at the 1.5× rule (quasar-p4.md §4.4):
     * P down by the Space trim with β at 1.5; W by 6·β_rail; Λ by 10·γ_halo at the maximum feed (10 L/s); P's upward
     * side is bursts only and is the player's 1.5× rule, not a trim, so it is not checked. Returns one line per
     * violation; empty means OK. For the boot log.
     */
    public static List<String> invariantViolations() {
        List<String> bad = new ArrayList<>();
        for (TierParams t : values()) {
            final double mMax = StellarField.mMax(t, 9);
            if (t.burstDriven()) {
                final double bMax = GalaxyField.BETA_CAP;
                for (int k : t.gauges) {
                    double best, d;
                    if (k == P) {
                        best = 0;
                        for (TrimDef tr : t.trims) best = Math.max(best, tr.cap * Math.abs(tr.fx(P)));
                        d = (t.slots > 0 ? GalaxyField.P_PER_BETA_PLANETS : GalaxyField.P_PER_BETA) * (bMax - 1);
                    } else if (k == W) {
                        best = GalaxyField.W_PER_GAMMA * bMax;
                        d = GalaxyField.W_DRIFT;
                    } else {
                        best = Math.abs(GalaxyField.L_PER_HALO) * bMax;
                        d = GalaxyField.L_PER_F8 * GalaxyField.F8_MAX;
                    }
                    final double need = mMax * (d + t.sigma(k, 0) + Math.abs(t.gain[k]) * 40);
                    if (!(best > need)) bad.add(
                        "T" + t.n
                            + (t.slots > 0 ? " planets " : " ")
                            + StellarField.GAUGE_SYM[k]
                            + ": authority "
                            + best
                            + " < needed "
                            + need);
                }
                continue;
            }
            final double[] dA = t.drift(0, 0), dB = t.drift(1.5, 1);
            for (int k : t.gauges) {
                double best = 0;
                for (TrimDef tr : t.trims) best = Math.max(best, tr.cap * Math.abs(tr.fx(k)));
                if (t.shield && k == PI) {
                    TrimDef agent = t.trim(TrimDef.Kind.AGENT);
                    if (agent != null) best = Math.max(best, agent.cap * StellarField.SHIELD_PER_UNIT);
                }
                final double d = Math.max(Math.abs(dA[k]), Math.abs(dB[k])) + (t.batch && k == TH ? 0.15 : 0);
                final double need = mMax * (d + t.sigma + Math.abs(t.gain[k]) * 40);
                if (!(best > need))
                    bad.add("T" + t.n + " " + StellarField.GAUGE_SYM[k] + ": trims push " + best + " < needed " + need);
            }
        }
        return bad;
    }
}
