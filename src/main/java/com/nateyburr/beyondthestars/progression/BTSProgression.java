package com.nateyburr.beyondthestars.progression;

import net.minecraft.nbt.NBTTagCompound;

/**
 * D-23 PROGRESSION, the per-controller campaign record: four counters, everything derived.
 * <p>
 * PERSISTENCE IS THE WHOLE POINT OF THIS CLASS. My ruling is per-controller storage
 * (the Godforge's shape), with the trade-off stated out loud: a bug here loses the campaign.
 * So there is exactly ONE serializer ({@link #writeTo}) called from BOTH writers -
 * {@code saveNBTData} (a reload) and {@code setItemNBT} (a WRENCH) - and exactly ONE reader
 * ({@link #readFrom}) behind {@code loadNBTData}, which is where item NBT and tile NBT both
 * land. They physically cannot drift apart, because there is only one of each.
 * <p>
 * {@code force} is the Godforge's {@code serializeNBT(nbt, force)} idea and it is load-bearing
 * on the ITEM path: {@code BaseMetaTileEntity.getDrops()} only attaches a tag to the dropped
 * stack {@code if (!tNBT.hasNoTags())}, so writing an unconditional schema int would give
 * EVERY wrenched controller a tag and stop fresh ones stacking. {@code force = false} writes
 * nothing at all when there is no progress, and the whole block the moment there is any.
 * <p>
 * NOTHING DERIVED IS EVER STORED. Tiers, multipliers, unlocks and points are pure functions of
 * the four counters, computed on demand on both sides. A stored tier would be a second source
 * of truth, and the two would eventually disagree.
 * <p>
 * Every accessor exists TWICE: a {@code static ...(long[] counters, ...)} form and an instance
 * delegate. The static form is what the CLIENT calls with the four synced longs (AXIS 1) and
 * what the debug command's round-trip test calls; the instance form is what the running plant
 * calls. One body, so a client readout cannot describe a different plant than the server runs.
 */
public final class BTSProgression {

    /** Schema version, written with the block. Bump only for a shape change, never a retune. */
    public static final int SCHEMA = 1;
    private static final String KEY_VERSION = "btsProgVersion";
    /** D-24: the upgrade tree's node set - a compound of booleans keyed by enum NAME. */
    public static final String KEY_NODES = "btsProgNodes";

    private final long[] counters = new long[BTSMilestone.values().length];
    /**
     * D-24: the purchased upgrade nodes, one bit each. Lives HERE, in the same record as the
     * four counters, deliberately - the tree rides the counters' already-verified persistence
     * path (one serializer, two writers, one reader) instead of growing a second one beside it.
     * On disk it is a compound of booleans keyed by {@code BTSUpgrade.name()}; the long is a
     * transport and arithmetic format only.
     */
    private long nodes;

    // ---------------------------------------------------------------- counters

    public long get(BTSMilestone line) {
        return counters[line.ordinal()];
    }

    public void set(BTSMilestone line, long value) {
        counters[line.ordinal()] = Math.max(0L, value);
    }

    /** Add to a counter. Saturating, never negative - a counter may only ever go up. */
    public void add(BTSMilestone line, long delta) {
        if (delta <= 0) return;
        final long now = counters[line.ordinal()];
        counters[line.ordinal()] = now > Long.MAX_VALUE - delta ? Long.MAX_VALUE : now + delta;
    }

    /** A defensive copy for the readouts and the sync values. Never the live array. */
    public long[] snapshot() {
        return counters.clone();
    }

    /**
     * True while the plant has earned NOTHING AND BOUGHT NOTHING - the "do not tag the item"
     * test. D-24 folds the node mask in: a plant that spent its points is not empty even if a
     * debug command later zeroed its counters, and its tree has to travel on the wrench.
     */
    public boolean isEmpty() {
        if (nodes != 0L) return false;
        for (final long counter : counters) {
            if (counter > 0L) return false;
        }
        return true;
    }

    /**
     * THE WHOLE RECORD - four counters AND the purchased node set. F1: this is the destructive
     * one, and the only caller is {@code /btsmilestone reset tree}, which says out loud that it
     * takes the tree with it. Anything that means "put the ladders back to zero" wants
     * {@link #clearCounters()} instead: a player re-testing pacing must not lose the tree.
     */
    public void clear() {
        clearCounters();
        nodes = 0L;
    }

    /**
     * THE FOUR COUNTERS ONLY. The node mask is deliberately left standing, so a debug reset of
     * the ladders cannot delete purchases the player made - {@link #pointsAvailable()} simply
     * goes negative and says so, which this class already documents as the honest answer.
     */
    public void clearCounters() {
        java.util.Arrays.fill(counters, 0L);
    }

    /** Safe read out of a snapshot array of any length (a short array reads 0). */
    public static long counterOf(long[] snapshot, BTSMilestone line) {
        if (snapshot == null || line.ordinal() >= snapshot.length) return 0L;
        return Math.max(0L, snapshot[line.ordinal()]);
    }

    public int tier(BTSMilestone line) {
        return line.tierOf(get(line));
    }

    public static int tier(long[] snapshot, BTSMilestone line) {
        return line.tierOf(counterOf(snapshot, line));
    }

    // ---------------------------------------------------------------- tiers 1-4: the multiplier

    /**
     * The ONE number the four early ladders pay out in: the product of all four lines'
     * multipliers, x1000. It enters the plant's arithmetic as the fourth of the five factors of
     * {@code MTEReactorCoreMk1.euDemandOf} - the fifth is the upgrade tree's own
     * {@link #upgradeMilli} - and is printed as its own term by the MATH panel.
     * <p>
     * It multiplies the EU DERIVATION and deliberately not the steam: steam L/t is what sets
     * the feedwater draw, the waste-water output and the export line, so a milestone that
     * raised it would raise the water demand of a plant that is already exactly supplied and
     * could starve one that was running fine yesterday. The reward the ruling asked for is EU,
     * and EU is where it lands.
     * <p>
     * At zero progress this is exactly 1000, which is why a plant with no progress derives a
     * bit-identical number to the one it derived before this system existed.
     */
    public static int outputMilli(long[] snapshot) {
        long milli = 1000L;
        for (final BTSMilestone line : BTSMilestone.values()) {
            milli = milli * line.multiplierMilli(tier(snapshot, line)) / 1000L;
        }
        return (int) Math.max(1000L, Math.min(Integer.MAX_VALUE, milli));
    }

    public int outputMilli() {
        return outputMilli(counters);
    }

    /** Triangular points earned across all four lines. A full sweep is exactly 112. */
    public static int pointsEarned(long[] snapshot) {
        int points = 0;
        for (final BTSMilestone line : BTSMilestone.values()) {
            points += BTSMilestone.points(tier(snapshot, line));
        }
        return points;
    }

    public int pointsEarned() {
        return pointsEarned(counters);
    }

    // ---------------------------------------------------------------- D-24: the upgrade tree
    // Points EARNED come from the milestone ladders above; points SPENT come from the node set.
    // Neither total is ever stored: earned is a pure function of the four counters and spent is
    // a pure function of the mask, so there is exactly one source of truth for each and they
    // cannot drift the way a persisted "spent" counter would.
    //
    // EVERY MUTATOR HERE IS SERVER-ONLY BY CONSTRUCTION: the only callers are the controller's
    // buy/refund wrappers (behind a SERVER-side synced action) and the debug command. The
    // client never reaches them - it renders off the synced mask through the static predicates,
    // which are the SAME bodies the server validates with (AXIS 1).

    /** The purchased-node bitmask, one bit per {@link BTSUpgrade}. */
    public long nodes() {
        return nodes;
    }

    /**
     * Replace the whole node set. Pruned on the way in, so no caller - not the debug command,
     * not a future loader - can install a set that contains an orphan.
     */
    public void setNodes(long mask) {
        nodes = BTSUpgrade.prune(mask);
    }

    public boolean owns(BTSUpgrade node) {
        return BTSUpgrade.owns(nodes, node);
    }

    public int pointsSpent() {
        return BTSUpgrade.pointsSpent(nodes);
    }

    /**
     * Points left to spend. MAY BE NEGATIVE, deliberately: a debug reset of the counters after
     * a purchase leaves the plant owing points, and the honest answer is to say so and refuse
     * further purchases - NOT to silently refund nodes the player bought, which would be this
     * system deleting a campaign on load. Refunds keep working, so the player can settle up.
     */
    public int pointsAvailable() {
        return pointsEarned() - pointsSpent();
    }

    /**
     * THE PURCHASE PREDICATE, and the only one. The GUI calls the identical static form to grey
     * a button out; the server calls this one before it writes a bit. A client that lies about
     * affordability therefore changes nothing at all - the answer is recomputed here.
     */
    public boolean canBuy(BTSUpgrade node) {
        if (node == null || owns(node)) return false;
        if (!BTSUpgrade.prerequisitesMet(nodes, node)) return false;
        if (!BTSUpgrade.gateMet(counters, node)) return false;
        return pointsAvailable() >= node.cost();
    }

    /** Buy it, server-side. Returns false and changes nothing when {@link #canBuy} refuses. */
    public boolean buy(BTSUpgrade node) {
        if (!canBuy(node)) return false;
        nodes |= node.bit();
        return true;
    }

    /**
     * Refunds are FREE, UNLIMITED and REVERSIBLE - the Godforge's own rule
     * ({@code ForgeOfGodsData:358-380}), and the only rule that makes a branching tree
     * explorable rather than a one-way trap. The single refusal is the one the Godforge has
     * too ({@code UpgradeStorage.checkDependents}): a refund may not orphan a node still held.
     */
    public boolean canRefund(BTSUpgrade node) {
        if (node == null || !owns(node)) return false;
        final long without = nodes & ~node.bit();
        for (final BTSUpgrade dependent : node.dependents()) {
            if (!BTSUpgrade.owns(without, dependent)) continue;
            if (!BTSUpgrade.prerequisitesMet(without, dependent)) return false;
        }
        return true;
    }

    public boolean refund(BTSUpgrade node) {
        if (!canRefund(node)) return false;
        nodes &= ~node.bit();
        return true;
    }

    /** Debug only: every node at once, ignoring points and gates. Never reachable in the GUI. */
    public void unlockAllNodes() {
        long all = 0L;
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            all |= node.bit();
        }
        nodes = all;
    }

    /** Debug only: hand every point back. The counters are untouched. */
    public void clearNodes() {
        nodes = 0L;
    }

    /**
     * The tree's OUTPUT term, x1000 - the FIFTH factor of {@code MTEReactorCoreMk1.euDemandOf},
     * printed by {@code mathTurbineLine} as its own labelled factor beside the milestone term.
     * <p>
     * It is a separate factor rather than folded into {@link #outputMilli} for one reason: the
     * MATH panel prints the derivation it actually performs. Two terms shown means two terms
     * applied, in that order - so the printed arithmetic and the executed arithmetic agree down
     * to the integer rounding.
     */
    public static int upgradeMilli(long nodeMask) {
        return Math.max(1000, BTSUpgrade.channel(nodeMask, BTSChannel.OUTPUT));
    }

    public int upgradeMilli() {
        return upgradeMilli(nodes);
    }

    // ---------------------------------------------------------------- tiers 5-7: the unlocks
    // Every one of these takes the plant's BASE constant and hands back a NEW MAXIMUM derived
    // from it. The constants stay where they are (in the controller) so there is still exactly
    // one place they are defined; only the RULE for raising them lives here. At tier < 5 every
    // single one of them returns its argument unchanged - that is the "a plant at zero progress
    // is untouched" guarantee, and it is checkable by reading these eleven lines.

    /** BURNUP 5/6/7: the control rods withdraw past the design stop. 24 -> 26 -> 28 -> 30. */
    public static int maxThrottle(long[] snapshot, long nodeMask, int base) {
        final int t = tier(snapshot, BTSMilestone.BURNUP);
        final int milestone = base + (t >= 7 ? 6 : t >= 6 ? 4 : t >= 5 ? 2 : 0);
        return milestone + BTSUpgrade.channel(nodeMask, BTSChannel.THROTTLE);
    }

    public int maxThrottle(int base) {
        return maxThrottle(counters, nodes, base);
    }

    /**
     * UPTIME 5/7: extra condensate stages, i.e. extra right-shifts on the feedwater draw on top
     * of the Cooling Towers' own. A pure REDUCTION in what the plant consumes, so it cannot
     * starve anything and cannot change a single output figure.
     */
    public static int condensateStages(long[] snapshot, long nodeMask) {
        final int t = tier(snapshot, BTSMilestone.UPTIME);
        return (t >= 7 ? 2 : t >= 5 ? 1 : 0) + BTSUpgrade.channel(nodeMask, BTSChannel.CONDENSATE);
    }

    public int condensateStages() {
        return condensateStages(counters, nodes);
    }

    /** UPTIME 6: the chain's standing steam buffer, in 20-tick slugs. 100 -> 160. */
    public static int steamBufferSlugs(long[] snapshot, long nodeMask, int base) {
        final int milestone = tier(snapshot, BTSMilestone.UPTIME) >= 6 ? base * 8 / 5 : base;
        return Math.max(base, milestone * BTSUpgrade.channel(nodeMask, BTSChannel.BUFFER) / 1000);
    }

    public int steamBufferSlugs(int base) {
        return steamBufferSlugs(counters, nodes, base);
    }

    /**
     * TURBINE 5/6: the redline CEILING. 12,000 -> 13,500 -> 15,000, and always in step with
     * {@link #rpmGainMilli} so the new redline is actually REACHABLE - the equilibrium is
     * {@code 10.486 x gain}, so raising one without the other would move a ceiling the plant
     * can never touch.
     * <p>
     * F4: this is what the campaign has UNLOCKED, not what the fan is held to. The plant runs
     * at {@code MTEReactorCoreMk1.operatingRedline(this, rpmLimitSetting)} - this value capped
     * by the RPM LIMIT the operator typed - because chamber coolant draw goes with the SQUARE
     * of RPM and a redline that rose on its own broke plants that were exactly supplied for
     * 12,000. Every unlock in this system either has to be opted into or can only ever reduce
     * a cost; this one is now the former.
     */
    public static int redlineRpm(long[] snapshot, long nodeMask, int base) {
        final int t = tier(snapshot, BTSMilestone.TURBINE);
        final int milestone = t >= 6 ? base * 5 / 4 : t >= 5 ? base * 9 / 8 : base;
        return Math.max(base, milestone * BTSUpgrade.channel(nodeMask, BTSChannel.REDLINE) / 1000);
    }

    public int redlineRpm(int base) {
        return redlineRpm(counters, nodes, base);
    }

    /**
     * TURBINE 5/6: RPM gained per perfectly timed cycle, x1000 - the CEILING again, pairing with
     * {@link #redlineRpm}. F4: the plant applies {@code MTEReactorCoreMk1.rpmGainMilli()}, which
     * holds this to the authorised overspeed, so a sub-redline plant (Lv 3 propellant settles at
     * ~9,227) is not pushed to a higher equilibrium - and a higher coolant draw - by a tier.
     */
    public static int rpmGainMilli(long[] snapshot, long nodeMask) {
        final int t = tier(snapshot, BTSMilestone.TURBINE);
        final int milestone = t >= 6 ? 1250 : t >= 5 ? 1125 : 1000;
        return Math.max(1000, milestone * BTSUpgrade.channel(nodeMask, BTSChannel.RPM_GAIN) / 1000);
    }

    public int rpmGainMilli() {
        return rpmGainMilli(counters, nodes);
    }

    /** TURBINE 7: the rotor-wear divisor. 600 -> 1,200, i.e. blades last twice as long. */
    public static int rotorWearDivisor(long[] snapshot, long nodeMask, int base) {
        final int milestone = tier(snapshot, BTSMilestone.TURBINE) >= 7 ? base * 2 : base;
        return Math.max(base, milestone * BTSUpgrade.channel(nodeMask, BTSChannel.ROTOR_WEAR) / 1000);
    }

    public int rotorWearDivisor(int base) {
        return rotorWearDivisor(counters, nodes, base);
    }

    /** REPROCESSING 5: the Derrick pays tantalum every 3rd load instead of every 4th. */
    public static int derrickTantalumEvery(long[] snapshot, long nodeMask, int base) {
        final int milestone = tier(snapshot, BTSMilestone.REPROCESSING) >= 5 ? base - 1 : base;
        return Math.max(1, milestone - BTSUpgrade.channel(nodeMask, BTSChannel.TANTALUM));
    }

    public int derrickTantalumEvery(int base) {
        return derrickTantalumEvery(counters, nodes, base);
    }

    /** REPROCESSING 6: both process modules turn over 20% faster. 200 -> 160, 600 -> 480. */
    public static int moduleCycleTicks(long[] snapshot, long nodeMask, int base) {
        final int milestone = tier(snapshot, BTSMilestone.REPROCESSING) >= 6 ? base * 4 / 5 : base;
        return Math.max(1, milestone * 1000 / Math.max(1000, BTSUpgrade.channel(nodeMask, BTSChannel.MODULE_SPEED)));
    }

    public int moduleCycleTicks(int base) {
        return moduleCycleTicks(counters, nodes, base);
    }

    /** REPROCESSING 7: the Annex batch grows a fifth. 20 -> 24 pellets. */
    public static int annexPellets(long[] snapshot, long nodeMask, int base) {
        final int milestone = tier(snapshot, BTSMilestone.REPROCESSING) >= 7 ? base * 6 / 5 : base;
        return Math.max(base, milestone * BTSUpgrade.channel(nodeMask, BTSChannel.ANNEX_BATCH) / 1000);
    }

    public int annexPellets(int base) {
        return annexPellets(counters, nodes, base);
    }

    // ---------------------------------------------------------------- persistence

    /**
     * THE ONE SERIALIZER. Called by {@code saveNBTData} with {@code force = true} (tile NBT,
     * survives a reload and a chunk unload) and by {@code setItemNBT} with {@code force = false}
     * (item NBT, survives a WRENCH). Two writers, one body - they cannot drift.
     * <p>
     * With {@code force = false} an empty record writes NOTHING, deliberately: a controller that
     * has never earned a tier drops as a plain, stackable machine item exactly as it always did.
     * The moment there IS progress the whole block goes on the item, schema int and all.
     */
    public void writeTo(NBTTagCompound nbt, boolean force) {
        if (!force && isEmpty()) return;
        nbt.setInteger(KEY_VERSION, SCHEMA);
        for (final BTSMilestone line : BTSMilestone.values()) {
            nbt.setLong(line.nbtKey(), get(line));
        }
        // D-24: the tree, in the SAME body, so it inherits the counters' whole verified path -
        // saveNBTData (reload, chunk unload) and setItemNBT (WRENCH) both arrive here, and
        // readFrom is the single reader on the other side of both.
        //
        // Keyed by BTSUpgrade.name(), never ordinal: the Godforge's ordinal keys
        // (UpgradeStorage:196) silently reassign every unlock the day a node is inserted
        // mid-enum, and this record is my campaign. A name this build does not know is
        // simply not read - it cannot land on the wrong node.
        //
        // Written only when there IS a node. Both writers hand in a FRESH compound, so an
        // absent tag reads as "no nodes" and a full refund genuinely clears the tag rather
        // than leaving a stale one behind.
        if (nodes != 0L) {
            final NBTTagCompound nodeTag = new NBTTagCompound();
            for (final BTSUpgrade node : BTSUpgrade.VALUES) {
                if (owns(node)) nodeTag.setBoolean(node.name(), true);
            }
            nbt.setTag(KEY_NODES, nodeTag);
        }
    }

    /**
     * THE ONE READER, behind {@code loadNBTData} - which is where BOTH paths land. Traced, not
     * assumed: {@code ItemMachines.placeBlock} -> {@code setInitialValuesAsNBT} (server only) ->
     * {@code BaseMetaTileEntity.loadMetaTileNBT} -> {@code CommonBaseMetaTileEntity} ->
     * {@code getMetaTileEntity().loadNBTData(aNBT)}, the same call a chunk load makes.
     * <p>
     * Missing keys read 0, which is the correct answer for a pre-0.22.0 world and for a fresh
     * controller alike. Values are clamped non-negative: a counter may only ever go up, and the
     * tier tables would happily read a negative as tier 0 but the readouts would print it.
     */
    public void readFrom(NBTTagCompound nbt) {
        for (final BTSMilestone line : BTSMilestone.values()) {
            set(line, Math.max(0L, nbt.getLong(line.nbtKey())));
        }
        // D-24. getCompoundTag returns an EMPTY compound when the key is absent, so a
        // pre-0.22.0 world, a fresh controller and a fully refunded one all read mask 0 -
        // which is the correct answer for every one of them.
        //
        // setNodes prunes: a mask can only ever install a set whose prerequisites all hold.
        // It does NOT re-check points or milestone gates, on purpose - those are PURCHASE-time
        // rules, and enforcing them on load would let a debug counter reset delete nodes the
        // player legitimately bought. pointsAvailable() goes negative instead and says so.
        long mask = 0L;
        final NBTTagCompound nodeTag = nbt.getCompoundTag(KEY_NODES);
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            if (nodeTag.getBoolean(node.name())) mask |= node.bit();
        }
        setNodes(mask);
    }

    /** The schema the tag was written under (0 = no progression block present). */
    public static int schemaOf(NBTTagCompound nbt) {
        return nbt.getInteger(KEY_VERSION);
    }

    /**
     * The round-trip self-test behind {@code /btsmilestone verify}: serialize through BOTH
     * writers, read each back into a fresh record, and report any counter that did not survive.
     * A forgotten key shows up here the instant it is forgotten, on both paths, without the
     * player having to wrench anything.
     */
    public String verifyRoundTrip() {
        final StringBuilder faults = new StringBuilder();
        for (final boolean force : new boolean[] { true, false }) {
            final NBTTagCompound probe = new NBTTagCompound();
            writeTo(probe, force);
            final BTSProgression copy = new BTSProgression();
            copy.readFrom(probe);
            for (final BTSMilestone line : BTSMilestone.values()) {
                if (copy.get(line) != get(line)) {
                    if (faults.length() > 0) faults.append("; ");
                    faults.append(force ? "tile " : "item ")
                        .append(line.nbtKey())
                        .append(' ')
                        .append(get(line))
                        .append(" -> ")
                        .append(copy.get(line));
                }
            }
            // D-24: and the tree, node by node, on both paths. A forgotten key in either
            // writer shows up the instant it is forgotten, without wrenching anything.
            for (final BTSUpgrade node : BTSUpgrade.VALUES) {
                if (copy.owns(node) != owns(node)) {
                    if (faults.length() > 0) faults.append("; ");
                    faults.append(force ? "tile " : "item ")
                        .append(KEY_NODES)
                        .append('/')
                        .append(node.name())
                        .append(' ')
                        .append(owns(node))
                        .append(" -> ")
                        .append(copy.owns(node));
                }
            }
        }
        return faults.toString();
    }
}
