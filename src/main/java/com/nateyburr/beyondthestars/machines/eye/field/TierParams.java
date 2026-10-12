package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.OM;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D-49 stability field: the per-tier field constants, one table (d49-build-plan.md §5; the prototype's
 * {@code TIERS}). Everything here is tunable, but the golden test (tools/fieldsim-golden.js) pins the prototype's
 * values: change a number here and regenerate the golden file from an updated prototype, or the test fails.
 * <p>
 * Upkeep fluids and amounts are NOT here: they belong to the star definitions of the runtime (plan §6.1, unit 4).
 * The field only sees the upkeep fraction r.
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
            trim(TrimDef.Kind.AGENT, 5, 0, 0.5, 0, 0, "shield A +4 per unit; the upkeep adds 2r/s") });

    /** Tier flags (a nested holder: an enum's constant arguments cannot forward-reference its own statics). */
    static final class F {

        static final int IGNITE = 1;
        static final int ASH = 2;
        static final int FEED = 4;
        static final int BATCH = 8;
        static final int SHIELD = 16;
    }

    /** Star tier 1..6. */
    public final int n;
    /** The prototype's star name (display only). */
    public final String star;
    /** Active gauges in update order (ascending P, Θ, Ω, Π). */
    private final int[] gauges;
    /** Noise half-width σ. */
    public final double sigma;
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
    /** Product per 60 s cycle at p = 1, before × mean Y × r (T4: 0, the product is 3.6864 × HSM fed; T5: per batch). */
    public final double productBase;
    /** Trims in table order (the drain order in play). */
    private final List<TrimDef> trims;

    TierParams(int n, String star, int[] gauges, double sigma, double[] gain, EventDef event, int flags,
        double productBase, TrimDef[] trims) {
        this.n = n;
        this.star = star;
        this.gauges = gauges;
        this.sigma = sigma;
        this.gain = gain;
        this.event = event;
        this.ignite = (flags & F.IGNITE) != 0;
        this.ash = (flags & F.ASH) != 0;
        this.feed = (flags & F.FEED) != 0;
        this.batch = (flags & F.BATCH) != 0;
        this.shield = (flags & F.SHIELD) != 0;
        this.productBase = productBase;
        List<TrimDef> list = new ArrayList<>();
        Collections.addAll(list, trims);
        this.trims = Collections.unmodifiableList(list);
    }

    private static double[] gains(double p, double th, double om, double pi) {
        return new double[] { p, th, om, pi };
    }

    private static TrimDef trim(TrimDef.Kind kind, double cap, double p, double th, double om, double pi, String note) {
        return new TrimDef(kind, cap, p, th, om, pi, note);
    }

    /** The tier with number {@code n} (1..6). */
    public static TierParams of(int n) {
        for (TierParams t : values()) if (t.n == n) return t;
        throw new IllegalArgumentException("no field tier " + n);
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
     * Drift d per Eye-second (plan §5). {@code feed} is the T4 HSM feed in L/s per parallel; {@code r} is the cycle's
     * upkeep fraction (the T6 feed push). Expressions are written exactly as the prototype's (rounding).
     */
    public double[] drift(double feed, double r) {
        switch (n) {
            case 1:
                return new double[] { 0.7, 0, 0, 0 };
            case 2:
                return new double[] { 2, 0, 0, 0 };
            case 3:
                return new double[] { 1.5, 1, 0, 0 };
            case 4:
                return new double[] { -3 + 5 * feed, -0.5 + 3 * feed, 1.5 * feed, 0 };
            case 5:
                return new double[] { -1, 0.02, 0, 0 };
            default:
                return new double[] { -4 + 5 * r, -0.5 + 3 * r, r, 0 };
        }
    }

    /** Coupling c_k(x): T3's P gains +0.02·Θ; zero elsewhere. */
    public double coupling(double[] x, int k) {
        return n == 3 && k == P ? 0.02 * x[TH] : 0;
    }

    /**
     * The design invariant (plan §5): for every active gauge, the strongest trim must out-push the worst case,
     * {@code cap × |effect| > m_max·(|d| + σ + |g|·40)}, with m_max at Time Acceleration tier 9 and d at its extremes
     * (T4 feed 0 and 1.5 L/s, T6 r 0 and 1). T6's Π is held by the shield (4 per Agent trim unit; the upkeep's 2·r/s is
     * not counted, r may be 0). Returns one line per
     * violation; empty means OK. For the boot log.
     */
    public static List<String> invariantViolations() {
        List<String> bad = new ArrayList<>();
        for (TierParams t : values()) {
            final double mMax = StellarField.mMax(t, 9);
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
