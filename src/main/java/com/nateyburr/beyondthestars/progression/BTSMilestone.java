package com.nateyburr.beyondthestars.progression;

import net.minecraft.util.EnumChatFormatting;

/**
 * D-23 PROGRESSION, the milestone layer: four lines the plant advances on just by RUNNING,
 * seven tiers each, twenty-eight tiers in all.
 * <p>
 * Each line watches ONE number the plant already knows about itself - it invents no new tick
 * loop and no new bookkeeping - and each tier it passes hands back two things:
 * <ol>
 * <li>tiers 1-4 a MULTIPLIER, which lands in the plant's EU/t derivation as its own labelled
 * term (see {@code MTEReactorCoreMk1.euDemandOf}) so the MATH panel keeps telling the truth;
 * <li>tiers 5-7 an UNLOCK, and every unlock is strictly ADDITIVE - it raises a ceiling or
 * lowers a cost, and never gates anything a plant can already do. A plant at zero progress
 * has exactly the capability it had before this system existed, which is why nothing here
 * needs grandfathering.
 * </ol>
 * <p>
 * THRESHOLDS ARE AN EXPLICIT TABLE, not a logarithm. The Godforge computes its tier with
 * {@code floor(log(total/BASE)/log(R)+1)} because it has unbounded post-tier-7 levels; we have
 * exactly seven, so a table is exact, has no float edge case at total = 0, and can be read off
 * by a player. {@link #tierOf} is {@code count(threshold <= counter)}, capped at 7.
 * <p>
 * The tables and every derivation on them are PURE STATIC FUNCTIONS of the counter. That is
 * deliberate (AXIS 1): the counters are written server-side and synced as four longs, and the
 * client re-derives tier, multiplier and next threshold with the same code the server runs - so
 * the GUI, both control blocks and the scanner cannot disagree about what tier a line is.
 */
public enum BTSMilestone {

    /**
     * UPTIME - generating ticks. The one line engineering cannot accelerate: it counts ticks on
     * which the plant actually put EU on the grid, so an idle chunk-loaded plant earns nothing.
     * Shallowest ladder (x2 a tier) because "just keep it lit" has to be the line that carries
     * the early multipliers.
     * <p>
     * F5 rate check: one point per generating tick = 72,000/h exactly, so the ladder is 1, 2, 4,
     * 8, 16, 32 and 64 hours. Correct as written.
     */
    UPTIME("UPTIME", "generating ticks", "btsProgUpTicks",
        new long[] { 72_000L, 144_000L, 288_000L, 576_000L, 1_152_000L, 2_304_000L, 4_608_000L },
        new int[] { 1000, 1020, 1050, 1090, 1150, 1150, 1150, 1150 },
        new String[] { "+2.0% output", "+5.0% output", "+9.0% output", "+15.0% output",
            "CONDENSATE RECOVERY I - feedwater draw halved again",
            "STEAM ACCUMULATOR - the steam buffer holds 1.6x the cycles",
            "CONDENSATE RECOVERY II - feedwater draw quartered again" }),

    /**
     * BURNUP - fuel actually consumed, weighted by ROD_MULT. Rewards the FUEL CHAIN rather than
     * the rod count: 201 uranium pillars score 201 a cycle, 201 naquadah-bonded ones score
     * 1,809. A starter plant on uranium pillars tops out around tier 3, which is this line
     * saying "your fuel chain is not finished" in the only language it has.
     * <p>
     * F5 rate check: one decay cycle is {@code genICampaignMinutes(720) x 60 x 20 / 8} = 108,000
     * ticks = 1.5 h, and a full tier-5 silo on naquadah scores 201 x 9 = 1,809 of them - 1,206/h,
     * so T7 is 600,000 / 1,206 = ~497 h. The same silo on plain uranium scores 134/h and would
     * need ~4,478 h, which is the fuel-chain pressure this line exists to apply. Correct as
     * written.
     */
    BURNUP("BURNUP", "fuel segments", "btsProgBurnup",
        new long[] { 2_000L, 5_000L, 13_000L, 34_000L, 88_000L, 230_000L, 600_000L },
        new int[] { 1000, 1030, 1070, 1120, 1200, 1200, 1200, 1200 },
        new String[] { "+3.0% output", "+7.0% output", "+12.0% output", "+20.0% output",
            "CONTROL ROD WITHDRAWAL I - rods up to 26/24", "CONTROL ROD WITHDRAWAL II - rods up to 28/24",
            "CONTROL ROD WITHDRAWAL III - rods up to 30/24" }),

    /**
     * REPROCESSING - finished Extraction Derrick loads and Enrichment Annex batches. The only
     * line that needs a BUILDING rather than a setting, so its rewards go straight back into
     * the two process modules: a richer by-product stream, faster cycles, a bigger batch.
     * <p>
     * F5 rate check: the Derrick deposits every {@code DERRICK_CYCLE_TICKS}(200) = 360/h and the
     * Annex every {@code ANNEX_CYCLE_TICKS}(600) = 120/h, so a plant running both scores 480/h
     * and T7 is 100,000 / 480 = ~208 h. Correct as written.
     */
    REPROCESSING("REPROCESSING", "Derrick loads + Annex batches", "btsProgReproc",
        new long[] { 250L, 700L, 1_900L, 5_200L, 14_000L, 38_000L, 100_000L },
        new int[] { 1000, 1020, 1045, 1070, 1100, 1100, 1100, 1100 },
        new String[] { "+2.0% output", "+4.5% output", "+7.0% output", "+10.0% output",
            "DEEP CUT - Derrick tantalum every 3rd load", "PROCESS INTENSIFICATION - Derrick and Annex cycles -20%",
            "YIELD UPLIFT - Annex batch x1.2 yellowcake" }),

    /**
     * TURBINE OPTIMIZATION - perfectly timed cycles: a cycle that had its steam slug AND had
     * every one of the eight injectors land exactly 1 L inside its +/-2t window. It cannot be
     * farmed by a fast clock with no fuel (no slug, no credit) and it cannot be farmed by a
     * half-plumbed chamber (7/8 timed scores nothing at all).
     * <p>
     * <b>F5 - RE-DERIVED AGAINST THE REAL CEILING.</b> The 0.22.0 ladder was sized against a
     * claimed 10,800 perfect cycles an hour. The plant cannot do that and nothing can raise it:
     * {@code evaluateCycle} scores exactly one point per accepted cycle, a cycle needs a full
     * slug of {@code CYCLE_SLUG_TICKS} (20) x {@code steamPerTick}, and the chain's buffer
     * refills at {@code steamPerTick} per running tick - so one cycle per 20 ticks, 1 Hz,
     * <b>3,600/h</b>, is the sustainable cap. The old T7 of 3,600,000 was therefore ~1,000
     * hours and gated the last 28 tree points behind it.
     * <p>
     * 10,800/h against 3,600/h is exactly 3x, so the whole ladder is divided by three - which
     * is the same thing as sliding it down one rung and growing a new x3 bottom rung at 1,800.
     * At the real 3,600/h that pays 0.5 h, 1.4, 4.2, 12.5, 37.5, 111 and <b>333 h</b>, which is
     * precisely the pacing the old numbers were aiming at, and it seats the line between
     * REPROCESSING (T7 ~208 h at 480 deposits/h) and BURNUP (T7 ~497 h at 1,809 a 1.5 h cycle)
     * with UPTIME's ~64 h below both.
     */
    TURBINE("TURBINE", "perfect cycles", "btsProgPerfect",
        new long[] { 1_800L, 5_000L, 15_000L, 45_000L, 135_000L, 400_000L, 1_200_000L },
        new int[] { 1000, 1025, 1050, 1085, 1120, 1120, 1120, 1120 },
        new String[] { "+2.5% output", "+5.0% output", "+8.5% output", "+12.0% output",
            "OVERSPEED TRIM I - redline may be set to 13,500 (RPM Limit field); gain follows it",
            "OVERSPEED TRIM II - redline may be set to 15,000 (RPM Limit field); gain follows it",
            "BLADE METALLURGY - rotor wear rolled half as often" });

    /** Tiers on every line. 7 x 4 = 28 tiers, and 28 triangular points a line = 112 in all. */
    public static final int MAX_TIER = 7;

    private final String display;
    private final String unit;
    private final String nbtKey;
    private final long[] thresholds;
    /** Output multiplier x1000, indexed by TIER (0..7). Tiers 5-7 hold the tier-4 value. */
    private final int[] multMilli;
    private final String[] grants;

    BTSMilestone(String display, String unit, String nbtKey, long[] thresholds, int[] multMilli, String[] grants) {
        this.display = display;
        this.unit = unit;
        this.nbtKey = nbtKey;
        this.thresholds = thresholds;
        this.multMilli = multMilli;
        this.grants = grants;
    }

    public String displayName() {
        return display;
    }

    public String unit() {
        return unit;
    }

    /** The tile/item NBT key this line's counter is stored under. Deliberately not an ordinal. */
    public String nbtKey() {
        return nbtKey;
    }

    /** The counter value tier {@code tier} demands (tier 0 = 0; above 7 clamps to tier 7). */
    public long threshold(int tier) {
        if (tier <= 0) return 0L;
        return thresholds[Math.min(tier, MAX_TIER) - 1];
    }

    /** The next tier's threshold, or -1 once the line is maxed. */
    public long nextThreshold(int tier) {
        return tier >= MAX_TIER ? -1L : thresholds[Math.max(0, tier)];
    }

    /** {@code count(thresholds <= counter)}, capped at 7. Exact; no float anywhere. */
    public int tierOf(long counter) {
        int tier = 0;
        for (final long threshold : thresholds) {
            if (counter >= threshold) tier++;
            else break;
        }
        return Math.min(MAX_TIER, tier);
    }

    /** This line's output multiplier x1000 at that tier (1000 = no effect at all). */
    public int multiplierMilli(int tier) {
        return multMilli[Math.max(0, Math.min(MAX_TIER, tier))];
    }

    /** What tier {@code tier} handed over, for the panel and the tooltips. */
    public String grant(int tier) {
        if (tier <= 0 || tier > MAX_TIER) return "";
        return grants[tier - 1];
    }

    /** The unlock's NAME alone - everything before the em-dash in {@link #grant}. */
    public String grantName(int tier) {
        final String full = grant(tier);
        final int dash = full.indexOf(" - ");
        return dash < 0 ? full : full.substring(0, dash);
    }

    /**
     * Progression points a line at this tier has EARNED, triangular exactly like the Godforge's
     * graviton shards: 1+2+..+t. A full sweep of all four lines is 4 x 28 = 112 points, which
     * is the number the upgrade tree is costed against.
     */
    public static int points(int tier) {
        final int t = Math.max(0, Math.min(MAX_TIER, tier));
        return t * (t + 1) / 2;
    }

    /** Progress through the CURRENT tier toward the next, in permille (a maxed line = 1000). */
    public int progressPermille(long counter) {
        final int tier = tierOf(counter);
        if (tier >= MAX_TIER) return 1000;
        final long floor = threshold(tier);
        final long ceiling = nextThreshold(tier);
        if (ceiling <= floor) return 1000;
        return (int) Math.max(0L, Math.min(1000L, (counter - floor) * 1000L / (ceiling - floor)));
    }

    // ------------------------------------------------------------------ shared formatters
    // Static and primitive-typed, exactly like the controller's own line formatters, so the
    // controller GUI, BOTH control blocks and the scanner print the same text from the same
    // code. Nothing here reads a field, so the client renders it off the four synced longs.

    /** UPTIME counts generating ticks; one hour of generating is 72,000 of them. */
    private static final double TICKS_PER_HOUR = 72_000.0;

    /**
     * A counter as the panel prints it. Tidy B5: UPTIME in hours ({@code 1.3 h}), because
     * "90,000 generating ticks" means nothing to a player; the other lines keep their counts.
     */
    private String amount(long value, boolean withUnit) {
        if (this == UPTIME) return String.format("%.1f", value / TICKS_PER_HOUR) + " h";
        return String.format("%,d", value) + (withUnit ? " " + unit : "");
    }

    /**
     * One milestone line: tier, counter against the next threshold, and what the NEXT tier pays.
     * Tidy B5: what the current tier already pays is on the summary and unlock lines below; this
     * line answers "what am I working towards".
     */
    public static String milestoneLine(BTSMilestone line, long counter) {
        final int tier = line.tierOf(counter);
        final long next = line.nextThreshold(tier);
        final EnumChatFormatting color = tier >= MAX_TIER ? EnumChatFormatting.LIGHT_PURPLE
            : tier >= 5 ? EnumChatFormatting.GREEN : tier > 0 ? EnumChatFormatting.AQUA : EnumChatFormatting.GRAY;
        final StringBuilder out = new StringBuilder();
        out.append(color)
            .append(line.displayName())
            .append(" T")
            .append(tier)
            .append('/')
            .append(MAX_TIER)
            .append(EnumChatFormatting.RESET)
            .append(" · ")
            .append(line.amount(counter, true));
        if (next < 0) {
            out.append(" · ")
                .append(EnumChatFormatting.LIGHT_PURPLE)
                .append("MAXED")
                .append(EnumChatFormatting.RESET);
        } else {
            out.append(" -> ")
                .append(line.amount(next, false))
                .append(" (")
                .append(line.progressPermille(counter) / 10)
                .append("%) · ")
                .append(EnumChatFormatting.GRAY)
                .append("next: ")
                .append(line.grant(tier + 1))
                .append(EnumChatFormatting.RESET);
        }
        return out.toString();
    }

    /** The four lines rolled up: tiers earned, the output term they buy, and the points. */
    public static String milestoneSummaryLine(long[] counters) {
        int tiers = 0;
        for (final BTSMilestone line : values()) {
            tiers += line.tierOf(BTSProgression.counterOf(counters, line));
        }
        final EnumChatFormatting color = tiers <= 0 ? EnumChatFormatting.GRAY : EnumChatFormatting.GOLD;
        return color + "Milestones "
            + tiers
            + "/"
            + (MAX_TIER * values().length)
            + EnumChatFormatting.RESET
            + " · plant output x"
            + String.format("%.3f", BTSProgression.outputMilli(counters) / 1000.0)
            + " · "
            + BTSProgression.pointsEarned(counters)
            + "/112 progression points earned";
    }

    /**
     * The tier-5+ UNLOCKS this plant is holding. Every entry names a NEW MAXIMUM: nothing in
     * this list can gate a capability the plant already had before it appeared.
     */
    public static String milestoneUnlockLine(long[] counters) {
        final StringBuilder out = new StringBuilder();
        for (final BTSMilestone line : values()) {
            final int tier = line.tierOf(BTSProgression.counterOf(counters, line));
            for (int t = 5; t <= tier; t++) {
                if (out.length() > 0) out.append(" · ");
                out.append(line.grantName(t));
            }
        }
        if (out.length() == 0) {
            return EnumChatFormatting.GRAY + "Unlocks: none yet - the first arrives at tier 5 on any line"
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.GREEN + "Unlocked: " + EnumChatFormatting.RESET + out;
    }
}
