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
    /**
     * P5: Bu/t on the T9 collar ports (the Deep Link's test fire at T8, where they move nothing; part of the Eye from
     * T9) and on the T10 crown's 12 ports (every burst of the Universe).
     */
    public double collar, crown;
    public int collarPorts, crownPorts;
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
    /**
     * P4-88: the tightest Junction out-1 limit on the link source's streams, Bu/t (+infinity: none): what J1 at 15 can
     * put on the Eye (the T7 test fire's size).
     */
    public double limit = Double.POSITIVE_INFINITY;
    /**
     * SMBH (smbh-v1.md §1.3): the link source's record this second, the lattice tier in force and the hole's mass in
     * SM (0, 0: none), for the Supermassive Link (P5's Eye T9 -> T10 milestone).
     */
    public int smLattice;
    public double smMass;

    /** SMBH: the link source can serve the Supermassive Link (a lattice in force and at least 65,536 SM). */
    public boolean supermassiveSource() {
        return smLattice > 0
            && smMass >= com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.SUPERMASSIVE_LINK_MASS;
    }

    public double rail() {
        return hi + lo;
    }

    public double total() {
        return hi + lo + halo + collar + crown;
    }

    public int ports() {
        return hiPorts + loPorts + haloPorts + collarPorts + crownPorts;
    }

    /** One scanner line. */
    public String line() {
        if (paused) return "PAUSED: its quasar at " + pausedBy + " is not running (the galaxy holds still)";
        if (ports() == 0) return "no GRB Receptor Ports (the T7 rail and the T8 halo carry them)";
        return String.format(
            Locale.ROOT,
            "Bursts %s: rail high %.2f (%d ports), low %.2f (%d), halo %.2f (%d)%s%s",
            com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(total()),
            hi,
            hiPorts,
            lo,
            loPorts,
            halo,
            haloPorts,
            collarPorts + crownPorts == 0 ? ""
                : String.format(Locale.ROOT, ", collar %.2f (%d), crown %.2f (%d)", collar, collarPorts, crown, crownPorts),
            source.isEmpty() ? (total() > 1e-9 ? ", not linked" : "") : ", linked to " + source);
    }
}
