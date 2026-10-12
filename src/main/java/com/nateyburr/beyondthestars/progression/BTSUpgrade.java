package com.nateyburr.beyondthestars.progression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import com.nateyburr.beyondthestars.progression.BTSChannel.Effect;

/**
 * D-24 UPGRADE TREE, the node graph: twenty-five nodes, four branches, one free root, and a
 * cost that sums to exactly 112 - the progression points a full sweep of all four milestone
 * lines pays out.
 * <p>
 * <b>THE CURRENCY IS PROGRESSION POINTS, AND NOTHING ELSE.</b> That is the Godforge's own
 * answer: its milestones grant graviton shards triangularly (1+2+..+t per line), its tree spends
 * them, and the two totals match exactly - I re-summed {@code ForgeOfGodsUpgrade}'s costs at
 * tag 5.09.52.594 and they come to 112 against a 112-shard sweep, so a maxed campaign buys the
 * whole tree with nothing wasted and nothing short. {@link BTSMilestone#points} already pays out
 * on exactly that curve, so this tree is costed against it and lands on the same number. An
 * ITEM cost was the alternative and is deliberately not taken: it needs a per-node "cost paid"
 * flag in NBT, which is a second persistent shape and a second failure mode on the one axis
 * this release is most exposed on - for no design gain, since the controller has no free input
 * bus of its own to insert into.
 * <p>
 * <b>EVERY EDGE, GATE AND POSITION IS DERIVED, NOT TYPED.</b> A node knows only its branch, its
 * depth and its cost; the graph falls out of those:
 * <ul>
 * <li><b>prerequisites</b> - the node one step shallower on the same branch (depth 1 hangs off
 * the root), plus, at depth 6, the depth-4 node of the NEXT branch round, which is what makes
 * this a graph and not four separate ladders;
 * <li><b>the milestone gate</b> - a node at depth {@code d} on branch {@code b} needs line
 * {@code b} at tier {@code d}. One sentence, no table, and it welds the two systems together:
 * the branch that rewards the turbine is the branch the TURBINE line opens;
 * <li><b>the connector lines</b> the GUI draws - straight off the prerequisite edges, so the
 * picture and the rules physically cannot disagree. (The Godforge maintains a second
 * thirty-seven-entry list of lines by hand; this is the one place I improved on it.)
 * </ul>
 * <p>
 * <b>EVERY EFFECT IS ADDITIVE.</b> A node carries {@link BTSChannel.Effect}s - a channel and a
 * positive delta - and every channel's base is a no-op. Nothing here can gate a capability the
 * plant already has, so nothing needed grandfathering and there is no "was-already-X" flag in
 * NBT to get wrong.
 * <p>
 * <b>NBT KEYS ARE {@code name()}, NEVER {@code ordinal()}.</b> The Godforge writes
 * {@code "upgrade" + ordinal} ({@code UpgradeStorage:196}), which silently reassigns every
 * unlock the day a node is inserted mid-enum. The bitmask below is a TRANSPORT format only -
 * client and server run the same jar within a session - and the disk format is names.
 */
public enum BTSUpgrade {

    // -------------------------------------------------------------------------- the root
    /** Free, effect-free, and the only node buyable at zero points. Click it and begin. */
    COMMISSIONING("COMMISSION", "Plant Commissioning", -1, 0, 0),

    // ------------------------------------------------------- depth 1 (one per branch, cheap)
    FEEDWATER_PREHEAT("Preheat", "Feedwater Preheat", 0, 1, 1, BTSChannel.OUTPUT.by(20)),
    MODERATOR_TRIM("Moderator", "Moderator Trim", 1, 1, 1, BTSChannel.OUTPUT.by(30)),
    ORE_SORTING("Sorting", "Ore Sorting Line", 2, 1, 1, BTSChannel.OUTPUT.by(20)),
    ROTOR_BALANCING("Balancing", "Rotor Balancing", 3, 1, 2, BTSChannel.ROTOR_WEAR.by(250)),

    // -------------------------------------------------------------------------- depth 2
    SURFACE_CONDENSER("Condenser", "Surface Condenser", 0, 2, 2, BTSChannel.CONDENSATE.by(1)),
    ROD_SEAL_UPGRADE("Rod Seals", "Rod Seal Upgrade", 1, 2, 2, BTSChannel.THROTTLE.by(2)),
    SOLVENT_RECOVERY("Solvent", "Solvent Recovery", 2, 2, 2, BTSChannel.MODULE_SPEED.by(100)),
    MAGNETIC_BEARINGS("Bearings", "Magnetic Bearings", 3, 2, 3, BTSChannel.RPM_GAIN.by(100)),

    // -------------------------------------------------------------------------- depth 3
    PIPEWORK_LAGGING("Lagging", "Pipework Lagging", 0, 3, 3, BTSChannel.OUTPUT.by(30)),
    RECLADDING_LINE("Recladding", "Recladding Line", 1, 3, 3, BTSChannel.OUTPUT.by(30)),
    EXTRA_CENTRIFUGE("Centrifuge", "Extra Centrifuge Stage", 2, 3, 3, BTSChannel.ANNEX_BATCH.by(250)),
    NOZZLE_REGRIND("Nozzles", "Nozzle Regrind", 3, 3, 4, BTSChannel.OUTPUT.by(30)),

    // -------------------------------------------------------------------------- depth 4
    ACCUMULATOR_BANK("Accumul.", "Steam Accumulator Bank", 0, 4, 4, BTSChannel.BUFFER.by(500)),
    CASCADE_SEQUENCER("Sequencer", "Cascade Sequencer", 1, 4, 5, BTSChannel.OUTPUT.by(40)),
    TAILINGS_RECLAIM("Tailings", "Tailings Reclaim", 2, 4, 4, BTSChannel.TANTALUM.by(1)),
    OVERSPEED_CERT("Overspeed", "Overspeed Certification", 3, 4, 5, BTSChannel.REDLINE.by(100),
        BTSChannel.RPM_GAIN.by(100)),

    // -------------------------------------------------------------------------- depth 5
    RESIDUAL_HEAT_SINK("Heat Sink", "Residual Heat Sink", 0, 5, 6, BTSChannel.HEAT_SINK.by(4),
        BTSChannel.DECAY_WINDOW.by(500)),
    DEEP_BURN_PROFILE("Deep Burn", "Deep Burn Profile", 1, 5, 5, BTSChannel.THROTTLE.by(2), BTSChannel.HEAT_SINK.by(2)),
    MODULE_AUTOMATION("Automation", "Module Automation", 2, 5, 8, BTSChannel.MODULE_SPEED.by(400)),
    SUPERALLOY_BLADES("Superalloy", "Superalloy Blades", 3, 5, 6, BTSChannel.ROTOR_WEAR.by(500),
        BTSChannel.OUTPUT.by(30)),

    // ------------------------------------------------- depth 6, the capstones (cross-linked)
    BASELOAD_LICENCE("Baseload", "Baseload Licence", 0, 6, 12, BTSChannel.OUTPUT.by(90), BTSChannel.CONDENSATE.by(1)),
    OVERPOWER_LICENCE("Overpower", "Overpower Licence", 1, 6, 12, BTSChannel.THROTTLE.by(2), BTSChannel.OUTPUT.by(60)),
    CLOSED_FUEL_CYCLE("Closed Cyc", "Closed Fuel Cycle", 2, 6, 10, BTSChannel.OUTPUT.by(70),
        BTSChannel.ANNEX_BATCH.by(250)),
    SYNCHRONOUS_CONDENSER("Sync Cond", "Synchronous Condenser", 3, 6, 8, BTSChannel.REDLINE.by(200),
        BTSChannel.RPM_GAIN.by(200));

    // ------------------------------------------------------------------------ shape constants

    /**
     * The root belongs to no milestone line, so it carries no gate. Spelled {@code -1} in the
     * constant list above rather than by this name: an enum constant's arguments may not name a
     * static field of its own enum, which is the same forward-reference rule that pushed the
     * Godforge's whole upgrade table into a {@code static {}} block.
     */
    public static final int ROOT_BRANCH = -1;
    /** Branches, one per {@link BTSMilestone} line, in that enum's ordinal order. */
    public static final int BRANCHES = 4;
    /** Depths below the root. Depth d on branch b needs line b at tier d. */
    public static final int MAX_DEPTH = 6;
    /** The full tree's price. Equal by construction to a full 28-tier sweep of all four lines. */
    public static final int TOTAL_COST = 112;

    // The tree's PIXEL geometry, kept here rather than in the panel so the node buttons and the
    // connector lines are laid out off one set of numbers. 292 wide is the Godforge's own
    // known-good inner width for a 300px panel padded 4/4.
    public static final int BUTTON_W = 62;
    public static final int BUTTON_H = 16;
    /** Inner width of the 300px panel once it is padded 4/4 - the Godforge's own number. */
    public static final int TREE_WIDTH = 292;
    private static final int[] COLUMN_X = { 4, 76, 148, 220 };
    private static final int[] ROW_Y = { 6, 68, 130, 192, 254, 316, 378 };
    /** Total scrollable height of the graph, for the panel's {@code VerticalScrollData}. */
    public static final int TREE_HEIGHT = ROW_Y[MAX_DEPTH] + BUTTON_H + 8;

    public static final BTSUpgrade[] VALUES = values();

    private final String label;
    private final String title;
    private final int branch;
    private final int depth;
    private final int cost;
    private final Effect[] effects;

    // Spelled out rather than sharing one static EMPTY constant: an enum's instance-field
    // initializers may not reference a static field of the enum (the same rule that pushed the
    // graph wiring below into a static block). The root keeps these empties for good.
    private BTSUpgrade[] prerequisites = new BTSUpgrade[0];
    private BTSUpgrade[] dependents = new BTSUpgrade[0];

    BTSUpgrade(String label, String title, int branch, int depth, int cost, Effect... effects) {
        this.label = label;
        this.title = title;
        this.branch = branch;
        this.depth = depth;
        this.cost = cost;
        this.effects = effects == null ? new Effect[0] : effects;
    }

    static {
        // Prerequisites, derived. Every edge in the graph is one of exactly two rules, so there
        // is no hand-maintained edge list that can drift out of step with the tree.
        for (final BTSUpgrade node : VALUES) {
            if (node.depth == 0) continue;
            final List<BTSUpgrade> prereqs = new ArrayList<>(2);
            prereqs.add(node.depth == 1 ? COMMISSIONING : at(node.branch, node.depth - 1));
            // The capstone cross-link: depth 6 on branch b also wants depth 4 on branch b+1.
            // This is what stops the tree being four independent ladders, and it is why the
            // late tree forces breadth the way the Godforge's convergence does.
            if (node.depth == MAX_DEPTH) {
                prereqs.add(at((node.branch + 1) % BRANCHES, 4));
            }
            node.prerequisites = prereqs.toArray(new BTSUpgrade[0]);
        }
        // Dependents, by inverting the prerequisite map - exactly the Godforge's static-block
        // inversion. This is what the refund check walks, so it must be derived and not typed.
        final List<List<BTSUpgrade>> inverted = new ArrayList<>(VALUES.length);
        for (int i = 0; i < VALUES.length; i++) {
            inverted.add(new ArrayList<BTSUpgrade>(2));
        }
        for (final BTSUpgrade node : VALUES) {
            for (final BTSUpgrade prereq : node.prerequisites) {
                inverted.get(prereq.ordinal())
                    .add(node);
            }
        }
        for (final BTSUpgrade node : VALUES) {
            node.dependents = inverted.get(node.ordinal())
                .toArray(new BTSUpgrade[0]);
        }
    }

    // ------------------------------------------------------------------------ identity

    /** The short label the 62x16 node button carries. */
    public String label() {
        return label;
    }

    /** The full name, for the tooltip's title line and the chat report. */
    public String title() {
        return title;
    }

    /** 0..3, one per milestone line, or {@link #ROOT_BRANCH} for the root. */
    public int branch() {
        return branch;
    }

    public int depth() {
        return depth;
    }

    /** Progression points this node costs. The twenty-five of them sum to {@link #TOTAL_COST}. */
    public int cost() {
        return cost;
    }

    public Effect[] effects() {
        return effects;
    }

    /** The single bit this node occupies in the purchased-node mask. 25 nodes, 64 bits. */
    public long bit() {
        return 1L << ordinal();
    }

    public BTSUpgrade[] prerequisites() {
        return prerequisites;
    }

    public BTSUpgrade[] dependents() {
        return dependents;
    }

    /** The milestone line this node's branch belongs to, or {@code null} for the root. */
    public BTSMilestone gateLine() {
        return branch < 0 ? null : BTSMilestone.values()[branch];
    }

    /** The tier that line must be at. Equal to the depth, by the tree's one gating rule. */
    public int gateTier() {
        return branch < 0 ? 0 : depth;
    }

    public int treeX() {
        // The root sits centred over the four columns; everything else sits in its branch's.
        if (branch < 0) return (TREE_WIDTH - BUTTON_W) / 2;
        return COLUMN_X[branch];
    }

    public int treeY() {
        return ROW_Y[Math.max(0, Math.min(MAX_DEPTH, depth))];
    }

    // ------------------------------------------------------------------------ lookup

    /** The node on that branch at that depth. Never null for a legal (branch, depth) pair. */
    public static BTSUpgrade at(int branch, int depth) {
        if (depth <= 0) return COMMISSIONING;
        for (final BTSUpgrade node : VALUES) {
            if (node.branch == branch && node.depth == depth) return node;
        }
        return COMMISSIONING;
    }

    /** By ordinal, bounds-checked - this is what the C2S purchase packet carries. */
    public static BTSUpgrade byIndex(int index) {
        if (index < 0 || index >= VALUES.length) return null;
        return VALUES[index];
    }

    /** By {@code name()}, which is the NBT key. Null for a name this build does not know. */
    public static BTSUpgrade byName(String name) {
        if (name == null) return null;
        for (final BTSUpgrade node : VALUES) {
            if (node.name()
                .equals(name)) return node;
        }
        return null;
    }

    // ------------------------------------------------------------------ mask arithmetic
    // The purchased set is one long. All of this is static and pure so the CLIENT runs the
    // identical code on the identical synced value (AXIS 1) - the tooltip's verdict and the
    // server's verdict come out of one body.

    public static boolean owns(long mask, BTSUpgrade node) {
        return node != null && (mask & node.bit()) != 0L;
    }

    /** Points committed to the purchased set. Derived; never stored (a second truth would drift). */
    public static int pointsSpent(long mask) {
        int spent = 0;
        for (final BTSUpgrade node : VALUES) {
            if (owns(mask, node)) spent += node.cost;
        }
        return spent;
    }

    /** True when every prerequisite this node names is present in that mask. */
    public static boolean prerequisitesMet(long mask, BTSUpgrade node) {
        for (final BTSUpgrade prereq : node.prerequisites) {
            if (!owns(mask, prereq)) return false;
        }
        return true;
    }

    /** True when the node's own milestone line has reached the tier its depth demands. */
    public static boolean gateMet(long[] counters, BTSUpgrade node) {
        final BTSMilestone line = node.gateLine();
        if (line == null) return true;
        return BTSProgression.tier(counters, line) >= node.gateTier();
    }

    /**
     * Drop any node whose prerequisites are not present, repeatedly until the set is stable.
     * <p>
     * On an unchanged build this can never fire: the only writer is a purchase that already
     * checked the prerequisites. It exists for the one case that CAN produce an orphan - a
     * future build that adds an edge to an existing node - because an orphan would then sit in
     * the graph forever making the refund check answer questions about an edge that no longer
     * holds. It removes rather than repairs, and it removes the SHALLOWEST-invalid first, so it
     * can never cascade further than the broken edge.
     */
    public static long prune(long mask) {
        long pruned = mask;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final BTSUpgrade node : VALUES) {
                if (!owns(pruned, node)) continue;
                if (prerequisitesMet(pruned, node)) continue;
                pruned &= ~node.bit();
                changed = true;
            }
        }
        return pruned;
    }

    /** Every node in the mask, in tree order - for the chat report and the panel roll-up. */
    public static List<BTSUpgrade> owned(long mask) {
        final List<BTSUpgrade> list = new ArrayList<>();
        for (final BTSUpgrade node : VALUES) {
            if (owns(mask, node)) list.add(node);
        }
        return list;
    }

    // ------------------------------------------------------------------ channel totals

    /**
     * THE ONE PLACE A CHANNEL'S VALUE IS DERIVED: its no-op base plus the deltas of every
     * purchased node that names it. Pure, static and monotone - which is the guarantee that a
     * plant at mask 0 gets exactly the base and so behaves exactly as it did in 0.21.0.
     */
    public static int channel(long mask, BTSChannel channel) {
        int total = channel.base();
        for (final BTSUpgrade node : VALUES) {
            if (!owns(mask, node)) continue;
            for (final Effect effect : node.effects) {
                if (effect.channel() == channel) total += effect.delta();
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ shared formatters
    // Static, primitive-typed and shared by the GUI tooltip, the chat command and the status
    // report, exactly like BTSMilestone's - so no two readouts can describe the same node
    // differently.

    /** This node's effects, generated from its channels. Empty for the root. */
    public String effectText() {
        if (effects.length == 0) return "no effect - the certificate that opens the tree";
        final StringBuilder out = new StringBuilder();
        for (final Effect effect : effects) {
            if (out.length() > 0) out.append(", ");
            out.append(effect.describe());
        }
        return out.toString();
    }

    /** The prerequisites, by title. Empty for the root. */
    public String prerequisiteText() {
        if (prerequisites.length == 0) return "";
        final StringBuilder out = new StringBuilder();
        for (final BTSUpgrade prereq : prerequisites) {
            if (out.length() > 0) out.append(" + ");
            out.append(prereq.title());
        }
        return out.toString();
    }

    /** The milestone gate, as the tooltip and the chat report both print it. */
    public String gateText() {
        final BTSMilestone line = gateLine();
        if (line == null) return "";
        return line.displayName() + " tier " + gateTier();
    }

    /** One node as a chat line: state, name, cost, effect. */
    public static String nodeLine(long mask, long[] counters, BTSUpgrade node) {
        final boolean has = owns(mask, node);
        final boolean open = prerequisitesMet(mask, node) && gateMet(counters, node);
        final EnumChatFormatting color = has ? EnumChatFormatting.GREEN
            : open ? EnumChatFormatting.AQUA : EnumChatFormatting.GRAY;
        return color + (has ? "[x] " : open ? "[ ] " : "[-] ")
            + node.title()
            + EnumChatFormatting.RESET
            + " · "
            + node.cost()
            + " pt"
            + (node.cost() == 1 ? "" : "s")
            + (node.gateLine() == null ? "" : " · needs " + node.gateText())
            + " · "
            + EnumChatFormatting.GRAY
            + node.effectText()
            + EnumChatFormatting.RESET;
    }

    /** The tree rolled up: nodes held, points committed, and what is still on the table. */
    public static String treeSummaryLine(long mask, long[] counters) {
        final int spent = pointsSpent(mask);
        final int earned = BTSProgression.pointsEarned(counters);
        final int held = owned(mask).size();
        final EnumChatFormatting color = held <= 0 ? EnumChatFormatting.GRAY : EnumChatFormatting.GOLD;
        return color + "Upgrades "
            + held
            + "/"
            + VALUES.length
            + EnumChatFormatting.RESET
            + " · "
            + spent
            + "/"
            + TOTAL_COST
            + " points spent · "
            + (earned - spent >= 0 ? EnumChatFormatting.AQUA : EnumChatFormatting.RED)
            + (earned - spent)
            + " available"
            + EnumChatFormatting.RESET;
    }

    /**
     * The channels the tree is actually moving right now. Prints only what is off its base, so
     * a plant that has bought nothing prints the one line that says exactly that.
     * <p>
     * Tidy B5: each entry is the channel's own {@link BTSChannel#describe} of the summed delta,
     * so the panel reads "+27.0% plant output · decay-heat window 1.5x shorter" in the same words
     * the node tooltips use, instead of "plant output x1.27 · Derrick tantalum interval +1".
     */
    public static String treeEffectLine(long mask) {
        final StringBuilder out = new StringBuilder();
        for (final BTSChannel channel : BTSChannel.values()) {
            final int total = channel(mask, channel);
            if (total == channel.base()) continue;
            if (out.length() > 0) out.append(" · ");
            out.append(channel.describe(total - channel.base()));
        }
        if (out.length() == 0) {
            return EnumChatFormatting.GRAY + "Tree effects: none - nothing purchased, the plant runs stock"
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.GREEN + "Tree effects: " + EnumChatFormatting.RESET + out;
    }

    /** A self-check the debug command runs: the twenty-five costs must still sum to 112. */
    public static String auditCosts() {
        int sum = 0;
        final int[] perBranch = new int[BRANCHES];
        for (final BTSUpgrade node : VALUES) {
            sum += node.cost;
            if (node.branch >= 0) perBranch[node.branch] += node.cost;
        }
        return "tree cost " + sum
            + "/"
            + TOTAL_COST
            + (sum == TOTAL_COST ? " OK" : " MISMATCH")
            + " · per branch "
            + Arrays.toString(perBranch)
            + " (a full milestone line pays "
            + BTSMilestone.points(BTSMilestone.MAX_TIER)
            + ")";
    }
}
