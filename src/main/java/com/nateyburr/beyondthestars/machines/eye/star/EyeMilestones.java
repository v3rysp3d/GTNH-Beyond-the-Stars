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
 * <li>P4 T6 → T7 (quasar-p4.md §5.2): the <b>Stability Link</b> (a machine milestone: a linked quasar holds a 60 s
 * mean of at least 1.5 Bu/t on the rail, the high bank 25-75 %, for 1,800 s unbroken), Magnetar Matter ≥ 2,700 L
 * from cycles with no second worse than Stable, Resonant ≥ 7,200 s and 50 giant flares ridden (never worse than
 * Stable from the warning to 30 s after);</li>
 * <li>P4 T7 → T8 (§5.3): the <b>Heavy Link</b> (6 Bu/t on the Eye's ports, at least half on the halo, 1,800 s
 * unbroken), Ergomatter ≥ 216,000 L from all-Stable cycles, Resonant ≥ 7,200 s and 4 minor mergers ridden in
 * Stable.</li>
 * </ul>
 * The two links' {@code done} flags and best streaks are here (item NBT); their live window and streak belong to the
 * star ({@link EyeGalaxy}, tile NBT).
 */
public final class EyeMilestones {

    /** Index = star tier 1..8: product litres (T4, T6, T7: from all-Stable cycles). */
    public final double[] prod = new double[9];
    /** Index = star tier: Resonant real seconds. */
    public final long[] res = new long[9];
    /** T3: helium flashes ridden. */
    public int flashes;
    /** T4: Deep Dark runs completed on this Eye. */
    public int deepDark;
    /** T5: good batches (mean Y ≥ 0.9, no fracture). */
    public int goodBatches;
    /** P4 T6: giant flares ridden in Stable. */
    public int flares;
    /** P4 T7: minor mergers ridden in Stable. */
    public int mergers;
    /** P4: the machine milestones, index = the tier being left (6 the Stability Link, 7 the Heavy Link). */
    public final boolean[] linkDone = new boolean[9];
    /** P4: the best link streak so far, seconds (display). */
    public final int[] linkBest = new int[9];
    /** P4: the live link streak, seconds (set by the star every second; display only, not saved here). */
    public final int[] linkStreak = new int[9];

    /** Thresholds, index = the tier being left (plan §8). */
    public static final double[] PROD_NEED = { 0, 20_000_000, 1_000_000, 400_000, 147_456, 0, 2_700, 216_000, 0 };
    public static final long[] RES_NEED = { 0, 7_200, 10_800, 14_400, 14_400, 0, 7_200, 7_200, 0 };
    public static final int FLASHES_NEED = 3;
    public static final int DEEP_DARK_NEED = 1;
    public static final int GOOD_BATCHES_NEED = 6;
    /** P4: giant flares (T6) and minor mergers (T7) ridden in Stable. */
    public static final int FLARES_NEED = 50, MERGERS_NEED = 4;
    /** P4: the link's streak, seconds (BurstLink.STREAK_SECONDS). */
    public static final int LINK_NEED = 1800;
    private static final String[] PROD_NAME = { "", "Raw Star Matter", "Excited Stellar Plasma",
        "Raw Hyperdense Stellar Plasma", "White Dwarf Matter (all-Stable cycles)", "",
        "Magnetar Matter (all-Stable cycles)", "Ergomatter (all-Stable cycles)", "" };
    /** P4: the link milestone's name, index = the tier being left. */
    public static final String[] LINK_NAME = { "", "", "", "", "", "", "Stability Link", "Heavy Link", "" };

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
            case 6:
                return linkDone[6] && prod[6] >= PROD_NEED[6] && res[6] >= RES_NEED[6] && flares >= FLARES_NEED;
            case 7:
                return linkDone[7] && prod[7] >= PROD_NEED[7] && res[7] >= RES_NEED[7] && mergers >= MERGERS_NEED;
            default:
                return false;
        }
    }

    /**
     * P4 (P4-76): leaving T6 / T7, everything but the link milestone is met. The link's test fire (Slots [8]) needs the
     * next core in escrow, so the core is wanted from here (Transition [4]); the link itself stays a precondition of
     * the
     * transition through {@link #met}. Other tiers: {@link #met}.
     */
    public boolean metButLink(int n) {
        switch (n) {
            case 6:
                return prod[6] >= PROD_NEED[6] && res[6] >= RES_NEED[6] && flares >= FLARES_NEED;
            case 7:
                return prod[7] >= PROD_NEED[7] && res[7] >= RES_NEED[7] && mergers >= MERGERS_NEED;
            default:
                return met(n);
        }
    }

    /** Debug ({@code /btseye star … milestones n done}): marks tier n's milestones met. */
    public void complete(int n) {
        if (n < 1 || n > 7) return;
        prod[n] = Math.max(prod[n], PROD_NEED[n]);
        res[n] = Math.max(res[n], RES_NEED[n]);
        if (n == 3) flashes = Math.max(flashes, FLASHES_NEED);
        if (n == 4) deepDark = Math.max(deepDark, DEEP_DARK_NEED);
        if (n == 5) goodBatches = Math.max(goodBatches, GOOD_BATCHES_NEED);
        if (n == 6) flares = Math.max(flares, FLARES_NEED);
        if (n == 7) mergers = Math.max(mergers, MERGERS_NEED);
        if (n >= 6) {
            linkDone[n] = true;
            linkBest[n] = Math.max(linkBest[n], LINK_NEED);
        }
    }

    /** Bench ({@code /btseye star … milestones n reset}): tier n's milestones back to nothing (a clean proof run). */
    public void reset(int n) {
        if (n < 1 || n > 7) return;
        prod[n] = 0;
        res[n] = 0;
        if (n == 3) flashes = 0;
        if (n == 4) deepDark = 0;
        if (n == 5) goodBatches = 0;
        if (n == 6) flares = 0;
        if (n == 7) mergers = 0;
        linkDone[n] = false;
        linkBest[n] = 0;
        linkStreak[n] = 0;
    }

    /** One progress line for leaving tier {@code n}, e.g. "T1 → T2: Raw Star Matter 1.2M / 20.0M L, ...". */
    public String progressLine(int n) {
        if (n < 1 || n > 7) return "";
        final StringBuilder sb = new StringBuilder("Milestones T").append(n)
            .append(" -> T")
            .append(n + 1)
            .append(met(n) ? " MET: " : ": ");
        if (n >= 6) {
            sb.append(LINK_NAME[n])
                .append(linkDone[n] ? " HELD" : " " + clock(linkStreak[n]) + " / " + clock(LINK_NEED))
                .append(linkDone[n] || linkBest[n] <= 0 ? "" : " (best " + clock(linkBest[n]) + ")")
                .append(", ")
                .append(PROD_NAME[n])
                .append(' ')
                .append(compact(prod[n]))
                .append(" / ")
                .append(compact(PROD_NEED[n]))
                .append(" L, Resonant ")
                .append(res[n])
                .append(" / ")
                .append(RES_NEED[n])
                .append(" s, ")
                .append(
                    n == 6 ? "giant flares ridden " + flares + "/" + FLARES_NEED
                        : "minor mergers ridden " + mergers + "/" + MERGERS_NEED);
            return sb.toString();
        }
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

    /** "12:34" (minutes:seconds). */
    public static String clock(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
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
        // P4: T6 -> T7 and T7 -> T8 (new keys: a pre-P4 save reads zeros)
        for (int n = 6; n <= 7; n++) {
            final NBTTagCompound t = new NBTTagCompound();
            t.setDouble("prod", prod[n]);
            t.setLong("res", res[n]);
            t.setInteger(n == 6 ? "flares" : "mergers", n == 6 ? flares : mergers);
            t.setBoolean("link", linkDone[n]);
            t.setInteger("linkBest", linkBest[n]);
            out.setTag("t" + n, t);
        }
        return out;
    }

    void read(NBTTagCompound in) {
        Arrays.fill(prod, 0);
        Arrays.fill(res, 0);
        flashes = deepDark = goodBatches = flares = mergers = 0;
        Arrays.fill(linkDone, false);
        Arrays.fill(linkBest, 0);
        Arrays.fill(linkStreak, 0);
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
        for (int n = 6; n <= 7; n++) {
            if (!in.hasKey("t" + n)) continue;
            final NBTTagCompound t = in.getCompoundTag("t" + n);
            prod[n] = t.getDouble("prod");
            res[n] = t.getLong("res");
            if (n == 6) flares = t.getInteger("flares");
            else mergers = t.getInteger("mergers");
            linkDone[n] = t.getBoolean("link");
            linkBest[n] = t.getInteger("linkBest");
        }
    }
}
