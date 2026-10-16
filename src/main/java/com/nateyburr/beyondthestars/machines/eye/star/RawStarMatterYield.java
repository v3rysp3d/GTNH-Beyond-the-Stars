package com.nateyburr.beyondthestars.machines.eye.star;

import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * D-55 (2026-10-17): every lit Eye tier makes Raw Star Matter (the pack's Condensed Raw Stellar Plasma Mixture), in
 * increasing amounts. Per Eye-minute (one 60 Eye-s cycle): {@code N × 50,000 L × p × the cycle's mean Y × r_min}.
 * <ul>
 * <li>T1 (Sol) already makes exactly this as its product, so it earns nothing extra (not doubled).</li>
 * <li>T5 earns it every Eye-minute too, not only at the end of its 7,200 s batch; T10 likewise beside its universe
 * batch.</li>
 * <li>Planets mode (T7 / T8 / T9) earns a quarter of its tier's amount (it draws a quarter of the demand).</li>
 * <li>Companions earn nothing extra, and no transition earns any (the Magnetar Collapse, the Galactic Merger, the
 * Infall, Inflation, the Core Collapse and the rest).</li>
 * </ul>
 * The runtime adds it at each cycle end ({@link EyeStarRuntime}); the field and its golden tests do not see it.
 * Every number here is a tuning knob.
 */
public final class RawStarMatterYield {

    private RawStarMatterYield() {}

    /** Raw Star Matter per Eye-minute per tier step, litres (T2 100,000, T10 500,000). Tunable. */
    public static final double PER_TIER = 50_000;
    /** Planets mode's share of its tier's amount. Tunable. */
    public static final double PLANETS_SHARE = 0.25;

    /**
     * Raw Star Matter per Eye-minute at p = 1, Y 1, r 1, litres: {@code N × 50,000} (a quarter in planets mode); 0 at
     * T1, whose product it already is.
     */
    public static double perMinute(TierParams t) {
        if (t == null || t.n <= 1) return 0;
        return t.n * PER_TIER * (t.planets() ? PLANETS_SHARE : 1);
    }

    /** One cycle's Raw Star Matter, litres: {@link #perMinute} × p × mean Y × r_min. */
    public static double cycle(TierParams t, int p, double meanY, double rMin) {
        return perMinute(t) * Math.max(1, p) * meanY * rMin;
    }
}
