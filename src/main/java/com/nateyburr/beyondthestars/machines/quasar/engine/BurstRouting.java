package com.nateyburr.beyondthestars.machines.quasar.engine;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * P4: the Einstein-Rosen Conduit's and the crowns' pure maths (quasar-p4.md §2, §3; the prototype's {@code THROAT},
 * {@code CROWN}, {@code lvlOf}, {@code shareOf} and {@code conduitStep}). The conduit solver (MTE units) walks the
 * network; each element applies these. SMBH v1 adds Mk IV (smbh-v1.md §3.4).
 */
public final class BurstRouting {

    private BurstRouting() {}

    // ------------------------------------------------------------------ throats (Conduit Throat item Mk I-IV)

    /** The highest throat and crown Mk (IV: the SMBH's, smbh-v1.md §3.4). */
    public static final int MK_MAX = 4;
    /**
     * Throat caps, Bu/t (index = Mk 1..4): anything above is clipped, no damage. Mk IV = 2¹⁹ = 524,288 Bu/t (5.24 ×
     * 10¹⁸ EU/t), under Long.MAX in EU/t, so no element's long path can overflow.
     */
    public static final double[] THROAT_CAP = { 0, 24, 384, 6144, 524288 };
    /**
     * Upkeep per element-leg, L/s: Molten SpaceTime (the bootstrap, before T7) or Ergomatter (after). Mk IV: 16 L/s
     * Ergomatter (§3.4); its SpaceTime figure continues the ×4 line (256), see B-150.
     */
    public static final double[] THROAT_SPACETIME = { 0, 4, 16, 64, 256 };
    public static final double[] THROAT_ERGOMATTER = { 0, 0.25, 1, 4, 16 };
    /** Mk IV's alternative upkeep: 2 L/s of Superdense Ergomatter per element-leg (§3.4; 0 = not offered). */
    public static final double[] THROAT_SUPERDENSE = { 0, 0, 0, 0, 2 };
    /** An element reopens after this many seconds of full upkeep. */
    public static final int REOPEN_SECONDS = 10;
    /** A stream must enter the next element's in-throat within this many blocks (longer hops use mouth pairs). */
    public static final int MAX_HOP = 64;
    /** The solver walks at most this many hops from each jet port. */
    public static final int MAX_HOPS = 32;

    /** Throat cap for Mk {@code mk} (1..4), Bu/t. */
    public static double throatCap(int mk) {
        return THROAT_CAP[Math.max(1, Math.min(MK_MAX, mk))];
    }

    /** Upkeep for one element-leg with throat Mk {@code mk}, L/s, on Ergomatter or on Molten SpaceTime. */
    public static double throatUpkeep(int mk, boolean ergomatter) {
        final int i = Math.max(1, Math.min(MK_MAX, mk));
        return ergomatter ? THROAT_ERGOMATTER[i] : THROAT_SPACETIME[i];
    }

    /** Mk {@code mk}'s Superdense Ergomatter upkeep per element-leg, L/s (0: that throat does not take it). */
    public static double throatUpkeepSuperdense(int mk) {
        return THROAT_SUPERDENSE[Math.max(1, Math.min(MK_MAX, mk))];
    }

    // ------------------------------------------------------------------ crowns

    /**
     * Crown caps, Bu/t (index = Mk 1..4); a second crown stacked on the first takes the overflow. Mk IV, the Event
     * Horizon Crown: 524,288 Bu/t = 5.24 × 10¹⁸ EU/t (§3.4), still under Long.MAX per element.
     */
    public static final double[] CROWN_CAP = { 0, 32, 512, 8192, 524288 };

    /** Crown cap for Mk {@code mk} (1..4), Bu/t. */
    public static double crownCap(int mk) {
        return CROWN_CAP[Math.max(1, Math.min(MK_MAX, mk))];
    }

    /** The EU/t a crown pushes to wireless for {@code used} Bu/t (10¹³ per Bu/t). */
    public static double crownEUt(double used) {
        return QuasarParams.EU_PER_BU * used;
    }

    /**
     * One second of a crown's wireless EU, floor(EU/t) × 20, as a BigInteger built straight from the double: no
     * {@code long} on the way, so no overflow at any stream (§3.4). 0 under 1 EU/t, non-finite or negative.
     */
    public static BigInteger crownEUPerSecond(double used) {
        final double eut = Math.floor(crownEUt(used));
        if (!(eut >= 1) || Double.isInfinite(eut)) return BigInteger.ZERO;
        return new BigDecimal(eut).toBigInteger()
            .multiply(BigInteger.valueOf(20));
    }

    // ------------------------------------------------------------------ Junction split (and its out-1 limit, P4-88)

    /** The GUI gains k a Junction offers (k = 0 ignores redstone). */
    public static final double[] GAINS = { 0, 1, 2, 4 };

    static double clamp(double v, double a, double b) {
        return v < a ? a : v > b ? b : v;
    }

    /**
     * share₁ = clamp(c + k·(s/15 − 0.5), 0, 1): the Junction's out-1 share for GUI ratio c, gain k and redstone
     * level s (0-15) on its Burst Control Hatch. Prototype {@code shareOf}.
     */
    public static double share(double c, double k, int s) {
        return clamp(c + k * (s / 15.0 - 0.5), 0, 1);
    }

    /**
     * The sensor hatch's analog output with window w: round(15·clamp((v + w)/(2w), 0, 1)) (§1.5; invert by passing
     * −v). Prototype {@code lvlOf}.
     */
    public static int level(double v, double w) {
        return (int) Math.round(15 * clamp((v + w) / (2 * w), 0, 1));
    }

    /**
     * The two-level route (AE2 has no analog output): a Junction fed two Directive bits reads 15 when {@code hi},
     * else 0 when {@code lo}, else 8 (finding F-8).
     */
    public static int twoLevel(boolean hi, boolean lo) {
        return hi ? 15 : lo ? 0 : 8;
    }

    /** A Junction's split: out 3 first (a fixed share p3), then out 1 / out 2 by share₁. Returns {out1, out2, out3}. */
    public static double[] split(double in, double share1, double p3) {
        final double out3 = p3 * in;
        final double out1 = share1 * (1 - p3) * in;
        final double out2 = (1 - share1) * (1 - p3) * in;
        return new double[] { out1, out2, out3 };
    }

    /**
     * P4-88: the split with an out-1 limit {@code limit} Bu/t (0 or less: none). Out 3 first, then share₁ of what is
     * left, but share₁ is taken of at most {@code limit}: everything above the limit goes straight to out 2. A limited
     * Junction therefore steers the same few Bu/t at any quasar size, so a P / W loop on it keeps the gain the
     * standard rig was tuned for. With no limit, or with the rest under it, this is exactly {@link #split(double,
     * double, double)}.
     */
    public static double[] split(double in, double share1, double p3, double limit) {
        final double rest = (1 - p3) * in;
        if (!(limit > 0) || rest <= limit) return split(in, share1, p3);
        final double out3 = p3 * in;
        final double out1 = share1 * limit;
        return new double[] { out1, rest - out1, out3 };
    }

    /**
     * QA-B06: the split of ONE stream against what is left of the Junction's out-1 limit this second ({@code budget}
     * Bu/t, already net of the streams that crossed earlier in the second; +∞ with no limit). Out 3 first, then
     * share₁ of the part of the rest that fits the budget; everything else goes to out 2. Returns {out1, out2, out3,
     * the part of the rest charged to the budget}. Summed over a second's streams, out 1 is share₁ × min(limit, the
     * total rest): the limit holds for the Junction, not per stream.
     */
    public static double[] splitWithin(double in, double share1, double p3, double budget) {
        final double out3 = p3 * in;
        final double rest = (1 - p3) * in;
        final double charged = Math.min(rest, Math.max(0, budget));
        final double out1 = share1 * charged;
        return new double[] { out1, rest - out1, out3, charged };
    }

    /**
     * P4-88: the standard rig's out-1 limit on J1 (Eye : crown), Bu/t per parallel and per unit of the Eye's time
     * dial: 5 × D₇. J1's two-level P loop then swings the Eye between 0, 4.3 and 7.5 Bu/t (β 0 / 2.8 / 5 at T7) at
     * any quasar size, instead of between 0 and the whole stream; the T7 test fire puts 7.5 on the Eye (rail 1.9, halo
     * 5.6), over the Heavy Link's 6. Prototype harness, Mk II / III throats, binary rig: 0 failures at 300 and 1,000 SM
     * on T7 and T8 (it was 83+ at 300 SM with no limit).
     */
    public static final double STANDARD_J1_LIMIT = 7.5;
}
