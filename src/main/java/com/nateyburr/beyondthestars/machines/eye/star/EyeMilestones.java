package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

/**
 * D-49: the per-controller milestone counters (d49-build-plan.md §8; design §4). Kept in ITEM NBT (they survive a
 * wrench), written and read by {@link EyeNbt}. Counters keep counting after the tier above unlocks (display only).
 * Resonant time counts REAL seconds on a star of that tier, so the time dial cannot shortcut it.
 * <p>
 * Step N → N+1 needs {@link #met(int) met(N)}:
 * <ul>
 * <li>T1 → T2: Raw Star Matter ≥ 20,000,000 L and Resonant ≥ 7,200 s;</li>
 * <li>T2 → T3: Excited Stellar Plasma ≥ 1,000,000 L and Resonant ≥ 10,800 s;</li>
 * <li>T3 → T4: Raw Hyperdense Stellar Plasma ≥ 400,000 L, Resonant ≥ 14,400 s and 3 helium flashes ridden (from the
 * warning until 30 s after the last hit second, never worse than Stable);</li>
 * <li>T4 → T5: White Dwarf Matter ≥ 147,456 L from cycles with no second worse than Stable, Resonant ≥ 14,400 s and
 * at least one Deep Dark run completed on this Eye;</li>
 * <li>T5 → T6: 6 cooling batches with mean Y ≥ 0.9 and no fracture (counted by the T5 runtime, unit 5).</li>
 * </ul>
 */
public final class EyeMilestones {

    /** Index = star tier 1..5: product litres (T4: WDM from all-Stable cycles). */
    public final double[] prod = new double[7];
    /** Index = star tier: Resonant real seconds. */
    public final long[] res = new long[7];
    /** T3: helium flashes ridden. */
    public int flashes;
    /** T4: Deep Dark runs completed on this Eye. */
    public int deepDark;
    /** T5: good batches (mean Y ≥ 0.9, no fracture). */
    public int goodBatches;

    /** Thresholds, index = the tier being left (plan §8). */
    public static final double[] PROD_NEED = { 0, 20_000_000, 1_000_000, 400_000, 147_456, 0, 0 };
    public static final long[] RES_NEED = { 0, 7_200, 10_800, 14_400, 14_400, 0, 0 };
    public static final int FLASHES_NEED = 3;
    public static final int DEEP_DARK_NEED = 1;
    public static final int GOOD_BATCHES_NEED = 6;
    private static final String[] PROD_NAME = { "", "Raw Star Matter", "Excited Stellar Plasma",
        "Raw Hyperdense Stellar Plasma", "White Dwarf Matter (all-Stable cycles)", "", "" };

    /** True when the milestones for leaving tier {@code n} (n → n+1) are all met. */
    public boolean met(int n) {
        switch (n) {
            case 1:
            case 2:
                return prod[n] >= PROD_NEED[n] && res[n] >= RES_NEED[n];
            case 3:
                return prod[3] >= PROD_NEED[3] && res[3] >= RES_NEED[3] && flashes >= FLASHES_NEED;
            case 4:
                return prod[4] >= PROD_NEED[4] && res[4] >= RES_NEED[4] && deepDark >= DEEP_DARK_NEED;
            case 5:
                return goodBatches >= GOOD_BATCHES_NEED;
            default:
                return false;
        }
    }

    /**
     * GUI: each part's progress toward leaving tier {@code n} (0..1 each): T1/T2 product and Resonant time; T3 also
     * the helium flashes; T4 also the Deep Dark runs; T5 the good batches. Empty outside 1..5.
     */
    public double[] partProgress(int n) {
        switch (n) {
            case 1:
            case 2:
                return new double[] { ratio(prod[n], PROD_NEED[n]), ratio(res[n], RES_NEED[n]) };
            case 3:
                return new double[] { ratio(prod[3], PROD_NEED[3]), ratio(res[3], RES_NEED[3]),
                    ratio(flashes, FLASHES_NEED) };
            case 4:
                return new double[] { ratio(prod[4], PROD_NEED[4]), ratio(res[4], RES_NEED[4]),
                    ratio(deepDark, DEEP_DARK_NEED) };
            case 5:
                return new double[] { ratio(goodBatches, GOOD_BATCHES_NEED) };
            default:
                return new double[0];
        }
    }

    private static double ratio(double have, double need) {
        return need <= 0 ? 1 : Math.max(0, Math.min(1, have / need));
    }

    /** GUI: how many parts the milestones for leaving tier {@code n} have. */
    public int parts(int n) {
        return partProgress(n).length;
    }

    /** GUI: how many of those parts are met. */
    public int done(int n) {
        int d = 0;
        for (double p : partProgress(n)) if (p >= 1) d++;
        return d;
    }

    /** GUI: the mean of the parts' progress (each capped at 1), 0..1. */
    public double fraction(int n) {
        final double[] p = partProgress(n);
        if (p.length == 0) return 0;
        double sum = 0;
        for (double v : p) sum += v;
        return sum / p.length;
    }

    /** Debug ({@code /btseye star … milestones n done}): marks tier n's milestones met. */
    public void complete(int n) {
        if (n < 1 || n > 5) return;
        prod[n] = Math.max(prod[n], PROD_NEED[n]);
        res[n] = Math.max(res[n], RES_NEED[n]);
        if (n == 3) flashes = Math.max(flashes, FLASHES_NEED);
        if (n == 4) deepDark = Math.max(deepDark, DEEP_DARK_NEED);
        if (n == 5) goodBatches = Math.max(goodBatches, GOOD_BATCHES_NEED);
    }

    /** One progress line for leaving tier {@code n}, e.g. "T1 → T2: Raw Star Matter 1.2M / 20.0M L, ...". */
    public String progressLine(int n) {
        if (n < 1 || n > 5) return "";
        final StringBuilder sb = new StringBuilder("Milestones T").append(n)
            .append(" -> T")
            .append(n + 1)
            .append(met(n) ? " MET: " : ": ");
        if (n == 5) {
            sb.append("good cooling batches ")
                .append(goodBatches)
                .append('/')
                .append(GOOD_BATCHES_NEED);
            return sb.toString();
        }
        sb.append(PROD_NAME[n])
            .append(' ')
            .append(compact(prod[n]))
            .append(" / ")
            .append(compact(PROD_NEED[n]))
            .append(" L, Resonant ")
            .append(res[n])
            .append(" / ")
            .append(RES_NEED[n])
            .append(" s");
        if (n == 3) sb.append(", helium flashes ridden ")
            .append(flashes)
            .append('/')
            .append(FLASHES_NEED);
        if (n == 4) sb.append(", Deep Dark runs ")
            .append(deepDark)
            .append('/')
            .append(DEEP_DARK_NEED);
        return sb.toString();
    }

    static String compact(double v) {
        if (v >= 1e9) return String.format(Locale.ROOT, "%.2fB", v / 1e9);
        if (v >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    // ================================================================================ NBT

    NBTTagCompound write() {
        final NBTTagCompound out = new NBTTagCompound();
        for (int n = 1; n <= 5; n++) {
            final NBTTagCompound t = new NBTTagCompound();
            if (n <= 3) t.setDouble("prod", prod[n]);
            if (n == 4) t.setDouble("wdmStable", prod[4]);
            if (n <= 4) t.setLong("res", res[n]);
            if (n == 3) t.setInteger("flashes", flashes);
            if (n == 4) t.setInteger("deepDark", deepDark);
            if (n == 5) t.setInteger("goodBatches", goodBatches);
            out.setTag("t" + n, t);
        }
        return out;
    }

    void read(NBTTagCompound in) {
        Arrays.fill(prod, 0);
        Arrays.fill(res, 0);
        flashes = deepDark = goodBatches = 0;
        if (in == null) return;
        for (int n = 1; n <= 5; n++) {
            if (!in.hasKey("t" + n)) continue;
            final NBTTagCompound t = in.getCompoundTag("t" + n);
            prod[n] = n == 4 ? t.getDouble("wdmStable") : t.getDouble("prod");
            res[n] = t.getLong("res");
            if (n == 3) flashes = t.getInteger("flashes");
            if (n == 4) deepDark = t.getInteger("deepDark");
            if (n == 5) goodBatches = t.getInteger("goodBatches");
        }
    }
}
