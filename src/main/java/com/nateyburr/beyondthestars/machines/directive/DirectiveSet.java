package com.nateyburr.beyondthestars.machines.directive;

import java.util.Locale;

/**
 * AUTOMATION PASS (brief item 18): what a Directive Hatch ({@link MTEHatchDirective}, MTE 28127) reports, or which
 * setpoint it drives. One hatch class serves every host: the Eye of Harmony, the Kerr Ergosphere Engine and (P4) the
 * Quasar Engine. P4's sets (Disc, Field, Events, Galaxy Transition, Slots; quasar-p4.md §8.1) compute their bits in
 * {@link QuasarDirective} and {@link GalaxyDirective}. A hatch placed in a host adopts that host's first set if its own
 * set belongs to
 * another host.
 * <p>
 * An output set is a 4-bit code in GT's Grade-7 water Degasser Control Hatch style: bit n has the value 2^(n-1),
 * several bits can be on at once, and each names one thing the host needs now. A control set reads redstone 1-15
 * into a setpoint (0 leaves the setpoint alone). The bit tables live here, in the hatch tooltip
 * ({@link MTEHatchDirective#getDescription}) and in docs/automation.md.
 * <p>
 * NBT stores the {@link #name()}, so new sets may be added anywhere in the list; never rename one.
 */
public enum DirectiveSet {

    /** The live star's trims, in its trim table's order (tier-dependent labels from the host). */
    EYE_TRIMS(Host.EYE, false, "Eye: Trims", "trim 1", "trim 2", "trim 3", "trim 4"),
    EYE_STATUS(Host.EYE, false, "Eye: Status", "Buffers short", "Ignition charge wanted", "Event warned / Agent wanted",
        "Alarm"),
    EYE_TRANSITION(Host.EYE, false, "Eye: Transition", "Ready: load the next chart", "Transition running",
        "Next core wanted", "Tachyon wanted (collapse, dial)"),
    EYE_DIAL(Host.EYE, true, "Eye: control the time dial"),
    EYE_FEED(Host.EYE, true, "Eye: control the HSM feed"),
    KERR_HOLE(Host.KERR, false, "Kerr: Hole", "Hole closed", "Mass below setpoint", "Bolts short",
        "Mass above setpoint"),
    KERR_STATUS(Host.KERR, false, "Kerr: Status", "Containment power short", "No Black Hole Seed", "Unstable",
        "Not formed"),
    KERR_SETPOINT(Host.KERR, true, "Kerr: control the mass setpoint"),
    // ---- P4 (quasar-p4.md §8.1; the bit rules are QuasarDirective / GalaxyDirective). Appended: NBT keeps names.
    /** The Quasar Engine's disc. */
    QUASAR_DISC(Host.QUASAR, false, "Quasar: Disc", "E low: pulse feed gas", "E high: Space trim",
        "Purge now (flux at setpoint)", "Alarm: blow-out, no coil power, venting, conduit closed"),
    /** The Eye's galaxy field (T7+): P and W. */
    EYE_FIELD(Host.EYE, false, "Eye: Field (T7+)", "P high: Space trim", "P low: more bursts to the Eye",
        "W high: aim high", "W low: aim low"),
    /** The Eye's galaxy events (T7+): Λ and the countdowns. */
    EYE_EVENTS(Host.EYE, false, "Eye: Events (T7+)", "Λ high: more to the halo", "Λ low: less to the halo",
        "Countdown running", "Event sign is −"),
    /** The Eye's galaxy transitions (T6 → T7 Magnetar Collapse, T7 → T8 Galactic Merger). */
    EYE_GALAXY_TRANSITION(Host.EYE, false, "Eye: Galaxy Transition", "Ready: begin", "Transition running",
        "Purge now (collapse assist)", "Hold the regulator's purges"),
    /** The Eye's planet and companion slots (T7+), and the link test fire (T6 / T7). */
    EYE_SLOTS(Host.EYE, false, "Eye: Slots", "A slot is idle", "Companion upkeep short", "Planet run lost parallels",
        "Link test fire wanted"),
    /** SMBH (smbh-v1.md §6): the Supermassive Lattice's intake and upkeeps (QuasarDirective#horizon). */
    QUASAR_HORIZON(Host.QUASAR, false, "Quasar: Horizon", "Hold-up short: leaking", "Feed short: buffer under 30 s",
        "Anchor short: shedding", "Overdrive upkeep short");

    /** The machines a Directive Hatch can sit in. */
    public enum Host {

        /** Not placed in a host yet (or the host stopped reporting). */
        NONE("no machine"),
        EYE("Eye of Harmony"),
        KERR("Kerr Ergosphere Engine"),
        QUASAR("Quasar Engine");

        public final String label;

        Host(String label) {
            this.label = label;
        }
    }

    public static final DirectiveSet[] VALUES = values();

    public final Host host;
    /** A control input (redstone in → setpoint) rather than a code output. */
    public final boolean control;
    public final String label;
    private final String[] bits;

    DirectiveSet(Host host, boolean control, String label, String... bits) {
        this.host = host;
        this.control = control;
        this.label = label;
        this.bits = bits;
    }

    /** The default label of bit {@code i} (0..3); "" for a control set. A host may name it better (the Eye's trims). */
    public String bit(int i) {
        return i >= 0 && i < bits.length ? bits[i] : "";
    }

    /** The first set of a host (what a hatch adopts when placed in it). */
    public static DirectiveSet first(Host host) {
        for (DirectiveSet s : VALUES) if (s.host == host) return s;
        return EYE_TRIMS;
    }

    /** The next set of the same host (the GUI's cycle button), wrapping. */
    public DirectiveSet next() {
        for (int i = 1; i <= VALUES.length; i++) {
            final DirectiveSet s = VALUES[(ordinal() + i) % VALUES.length];
            if (s.host == host) return s;
        }
        return this;
    }

    /** The set with this name (any case), or null. */
    public static DirectiveSet parse(String name) {
        if (name == null) return null;
        final String n = name.trim()
            .toUpperCase(Locale.ROOT);
        for (DirectiveSet s : VALUES) if (s.name()
            .equals(n)) return s;
        return null;
    }

    /** The output mode of a hatch on an output set. */
    public enum Output {

        /** One signal, strength = the code 0-15 (the Degasser's way; decode with comparators or gates). */
        CODE("Code (strength = code)"),
        BIT1("Bit 1 only (15 / 0)"),
        BIT2("Bit 2 only (15 / 0)"),
        BIT3("Bit 3 only (15 / 0)"),
        BIT4("Bit 4 only (15 / 0)"),
        /** 15 while any bit is on. */
        ANY("Any bit (15 / 0)");

        public static final Output[] VALUES = values();

        public final String label;

        Output(String label) {
            this.label = label;
        }

        /** The redstone strength this mode drives for {@code code} (0-15). */
        public int apply(int code) {
            switch (this) {
                case CODE:
                    return code & 15;
                case ANY:
                    return (code & 15) != 0 ? 15 : 0;
                default:
                    return (code & (1 << (ordinal() - BIT1.ordinal()))) != 0 ? 15 : 0;
            }
        }

        public static Output of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }
    }

    /** Bit {@code n} (1..4) of {@code code}. */
    public static boolean isSet(int code, int n) {
        return (code & (1 << (n - 1))) != 0;
    }

    /**
     * A control strength 1-15 mapped linearly onto [lo, hi], snapped to {@code step} (0: no snapping). Strength 1
     * gives lo, 15 gives hi.
     */
    public static double controlValue(int strength, double lo, double hi, double step) {
        final int s = Math.max(1, Math.min(15, strength));
        double v = lo + (hi - lo) * (s - 1) / 14.0;
        if (step > 0) v = lo + Math.round((v - lo) / step) * step;
        return Math.max(lo, Math.min(hi, v));
    }
}
