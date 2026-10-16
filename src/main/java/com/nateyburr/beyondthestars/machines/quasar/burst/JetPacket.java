package com.nateyburr.beyondthestars.machines.quasar.burst;

/**
 * P4: one second of one jet port's stream, as the Quasar Engine hands it to the dock on that port (quasar-p4.md §2,
 * §3.1). Immutable. The Conduit unit forwards a packet's {@link #source} with every stream it routes, so each GRB
 * Receptor Port can record "how much arrived from which source last second", which is what a link is.
 */
public final class JetPacket {

    /** Who sent it. */
    public final BurstSource source;
    public final JetPort port;
    /** Bu/t on this port this second (half the stream; eruptions included). */
    public final double buPerTick;
    /**
     * The source end of a link (quasar-p4.md §3.1): the quasar is formed, its hole OPEN (not dormant, not
     * re-forming) and its coils powered (b &gt; 0). A host is linked only if this holds and the whole path is open.
     */
    public final boolean linkable;
    /** The world's total time when it was sent (a consumer treats a packet older than 40 ticks as stale). */
    public final long tick;

    /** A packet older than this many ticks is stale: the source stopped publishing (Q-8: pause, do not read 0). */
    public static final long STALE_TICKS = 40;

    /**
     * SMBH (smbh-v1.md §1.3, §10.2): the source record P5's Supermassive Link reads, the lattice tier in force at the
     * source (0-4) and its hole's mass (SM) when the packet was sent (the bench injector copies its quasar's).
     */
    public final int lattice;
    public final double mass;
    /**
     * QA-B68: the source's hole is shedding mass (smbh-v1.md §2.5), so the jet carries nothing while it lasts. A dock
     * shows why it makes nothing instead of reading as broken.
     */
    public final boolean shed;

    public JetPacket(BurstSource source, JetPort port, double buPerTick, boolean linkable, long tick) {
        this(source, port, buPerTick, linkable, tick, 0, 0);
    }

    public JetPacket(BurstSource source, JetPort port, double buPerTick, boolean linkable, long tick, int lattice,
        double mass) {
        this(source, port, buPerTick, linkable, tick, lattice, mass, false);
    }

    public JetPacket(BurstSource source, JetPort port, double buPerTick, boolean linkable, long tick, int lattice,
        double mass, boolean shed) {
        this.lattice = lattice;
        this.mass = mass;
        this.shed = shed;
        this.source = source;
        this.port = port;
        this.buPerTick = buPerTick;
        this.linkable = linkable;
        this.tick = tick;
    }

    /** True when the packet is older than {@link #STALE_TICKS} at world time {@code now}. */
    public boolean stale(long now) {
        return now - tick > STALE_TICKS;
    }
}
