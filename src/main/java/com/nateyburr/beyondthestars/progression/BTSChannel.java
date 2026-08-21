package com.nateyburr.beyondthestars.progression;

/**
 * D-24 UPGRADE TREE, the effect channels: the twelve numbers a purchased node is allowed to
 * move, and the ONLY twelve.
 * <p>
 * This enum is the whole safety argument for the tree. A node does not carry code - it carries
 * a channel and a DELTA, and the plant reads the channel through one accessor per channel. So:
 * <ul>
 * <li>the set of things the tree can possibly affect is this file, and it is twenty lines long;
 * <li>every channel starts at a BASE that makes it a no-op ({@code 1000} for a x1000 multiplier,
 * {@code 0} for an adder), so a plant that has bought nothing derives bit-identical numbers to
 * the ones it derived before this system existed;
 * <li>every delta is POSITIVE and every channel is monotone in the player's favour, so no node
 * can take a capability away and none of them needs grandfathering (the release ruling);
 * <li>the effect TEXT the GUI prints is generated from the channel and the delta by
 * {@link #describe}, so a node cannot advertise one thing and do another.
 * </ul>
 * <p>
 * AXIS 1: nothing here reads a field. Channel totals are pure functions of the purchased-node
 * bitmask, so the client renders them off the one synced long and reaches the same answer the
 * server's tick did.
 */
public enum BTSChannel {

    /**
     * The plant's EU/t derivation, x1000. Lands as the FIFTH factor of
     * {@code MTEReactorCoreMk1.euDemandOf}, next to - and separate from - the milestone term,
     * and {@code mathTurbineLine} prints it as its own labelled factor. Like the milestone
     * term it multiplies the EU and deliberately NOT the steam, so it cannot move the feedwater
     * draw of a plant that is already exactly supplied.
     */
    OUTPUT(1000, Kind.MILLI, "plant output"),

    /**
     * Extra rungs on the control-rod cap, on top of the BURNUP milestone line's. The N/24
     * DENOMINATOR never moves, so 36/24 really is 150% of design power. This one DOES raise
     * steam, and therefore feedwater - which is why the same tree hands out CONDENSATE stages
     * on the branch that pays for it.
     */
    THROTTLE(0, Kind.ADD, "control-rod cap"),

    /** Extra right-shifts on the feedwater draw. A pure REDUCTION; it cannot starve anything. */
    CONDENSATE(0, Kind.ADD, "feedwater condensate stages"),

    /** The chain's standing steam buffer, x1000, on top of the UPTIME line's. */
    BUFFER(1000, Kind.MILLI, "steam buffer"),

    /** RPM gained per accepted cycle, x1000. Always raised WITH the redline, never alone. */
    RPM_GAIN(1000, Kind.MILLI, "cycle RPM gain"),

    /** The redline, x1000, on top of the TURBINE line's. Always raised WITH the cycle gain. */
    REDLINE(1000, Kind.MILLI, "turbine redline"),

    /** The rotor-wear DIVISOR, x1000 - a bigger divisor is a rarer wear roll. */
    ROTOR_WEAR(1000, Kind.MILLI, "rotor life"),

    /** Derrick and Annex cycle length divisor, x1000 - a bigger number is a faster module. */
    MODULE_SPEED(1000, Kind.MILLI, "Derrick and Annex cycle speed"),

    /** The Enrichment Annex's batch size, x1000. */
    ANNEX_BATCH(1000, Kind.MILLI, "Annex batch"),

    /** Loads shaved off the Derrick's tantalum interval. */
    TANTALUM(0, Kind.ADD, "Derrick tantalum interval"),

    /** Extra heat bled off per tick, on top of the towers'. */
    HEAT_SINK(0, Kind.ADD, "heat rejection"),

    /** The post-shutdown decay-heat window DIVISOR, x1000 - bigger is a shorter window. */
    DECAY_WINDOW(1000, Kind.MILLI, "decay-heat window");

    /** How a channel's total is spelled: a x1000 multiplier, or a plain adder. */
    public enum Kind {
        MILLI,
        ADD
    }

    private final int base;
    private final Kind kind;
    private final String subject;

    BTSChannel(int base, Kind kind, String subject) {
        this.base = base;
        this.kind = kind;
        this.subject = subject;
    }

    /** The value of this channel on a plant that has bought nothing. Always a no-op. */
    public int base() {
        return base;
    }

    public Kind kind() {
        return kind;
    }

    public String subject() {
        return subject;
    }

    /** One (channel, delta) pair, as a node declares it. */
    public Effect by(int delta) {
        return new Effect(this, delta);
    }

    /**
     * The effect text, GENERATED from the channel and the delta rather than typed alongside
     * them. A node therefore cannot advertise one number and apply another - there is only one
     * number, and this is it rendered.
     */
    public String describe(int delta) {
        if (delta <= 0) return "";
        switch (this) {
            case OUTPUT:
                return "+" + String.format("%.1f", delta / 10.0) + "% plant output";
            case THROTTLE:
                return "control-rod cap +" + delta + " (N/24)";
            case CONDENSATE:
                return delta == 1 ? "feedwater draw halved again" : "feedwater draw shifted " + delta + " more stages";
            case TANTALUM:
                return "Derrick tantalum " + delta + " load" + (delta == 1 ? "" : "s") + " sooner";
            case HEAT_SINK:
                return "+" + delta + " heat/t bled off";
            case ROTOR_WEAR:
                return "rotor wear rolled x" + milli(delta) + " less often";
            case MODULE_SPEED:
                return "Derrick and Annex cycles x" + milli(delta) + " faster";
            case DECAY_WINDOW:
                return "decay-heat window x" + milli(delta) + " shorter";
            default:
                return subject + " x" + milli(delta);
        }
    }

    private static String milli(int delta) {
        return String.format("%.2f", (1000 + delta) / 1000.0);
    }

    /**
     * A node's effect: one channel and one positive delta. Deliberately a value, not a lambda -
     * the whole point is that the set of things a node can do is enumerable and printable.
     */
    public static final class Effect {

        private final BTSChannel channel;
        private final int delta;

        private Effect(BTSChannel channel, int delta) {
            this.channel = channel;
            this.delta = Math.max(0, delta);
        }

        public BTSChannel channel() {
            return channel;
        }

        public int delta() {
            return delta;
        }

        public String describe() {
            return channel.describe(delta);
        }
    }
}
