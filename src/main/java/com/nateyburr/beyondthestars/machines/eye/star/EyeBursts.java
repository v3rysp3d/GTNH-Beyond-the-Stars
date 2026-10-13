package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;

/**
 * P4: what the Eye's GRB Receptor Ports received last second, by group (quasar-p4.md §3.1, §4.1; the manifest's
 * {@code sockets.r[i].group}): the rail's high bank, its low bank, and the T8 shard halo. The controller reads its
 * ports through {@code BurstHost} once a field second and hands the runtime this snapshot; the runtime normalises it
 * into β and the γs. Not saved: nothing about a link is.
 */
public final class EyeBursts {

    /** No ports, nothing received. */
    public static final EyeBursts NONE = new EyeBursts();

    /** Bu/t on the rail-high, rail-low and halo ports last second. */
    public double hi, lo, halo;
    /** A linked quasar delivered to the rail (the Stability Link's "linked"), to any port (the Heavy Link's). */
    public boolean linkedRail, linkedAll;
    /** Q-8: a source that fed these ports is frozen or not loaded: the galaxy field and cycle pause. */
    public boolean paused;
    /** The pausing source, or the linked source that delivered the most, as text ("" for none). */
    public String pausedBy = "", source = "";
    /** The linked source that delivered the most (the collapse assist reads its Φ), or null. */
    public BurstSource sourceId;
    /** Receptor Ports standing in each group (the walk's count). */
    public int hiPorts, loPorts, haloPorts;
    /** The host's 60 s mean of everything received, Bu/t, and σ on it (BurstHost). */
    public double mean60, sigmaMean;

    public double rail() {
        return hi + lo;
    }

    public double total() {
        return hi + lo + halo;
    }

    public int ports() {
        return hiPorts + loPorts + haloPorts;
    }

    /** One scanner line. */
    public String line() {
        if (paused) return "PAUSED: its quasar at " + pausedBy + " is not running (Q-8: the galaxy holds still)";
        if (ports() == 0) return "no GRB Receptor Ports (the T7 rail and the T8 halo carry them)";
        return String.format(
            Locale.ROOT,
            "Bursts %.2f Bu/t: rail high %.2f (%d ports), low %.2f (%d), halo %.2f (%d)%s",
            total(),
            hi,
            hiPorts,
            lo,
            loPorts,
            halo,
            haloPorts,
            source.isEmpty() ? (total() > 1e-9 ? ", not linked" : "") : ", linked to " + source);
    }
}
