package com.nateyburr.beyondthestars.machines.quasar.burst;

/**
 * P4: a machine that takes a Quasar Engine's jet on a jet port (quasar-p4.md §2): the Burst Thermaliser Crown (28124)
 * or a Wormhole Mouth (28121). Implemented by the crown and conduit units.
 * <p>
 * <b>How a dock is found (P4-02).</b> Each second while it is formed, a dock registers its intake cell with
 * {@link BurstDocks#register}: a crown its base centre ({@code manifest.json machines.crown.intake[piece].rel} from
 * its controller), a mouth its throat centre. The quasar looks up each port cell ({@code machines.quasar.slots.<port>
 * .portRel}) in that registry once a second and pushes the port's {@link JetPacket}. A port whose cell has no fresh,
 * {@link #dockReady() ready} dock <b>vents</b>: the bursts are lost and the quasar says "venting". The dock's facing
 * check (a crown's intake axis, a mouth's throat normal along the jet axis) is the dock's own: it registers only when
 * it faces the right way.
 */
public interface IBurstDock {

    /**
     * True while the dock can take a jet: formed, and (for a mouth) paired and open. The quasar re-checks it before
     * every push, so a dock that unforms between registrations vents at once.
     */
    boolean dockReady();

    /**
     * One second of the jet on this dock's port (server thread, once per quasar second). Returns the Bu/t the dock
     * took: a crown up to its cap (and a stacked crown's), a mouth up to its throat cap. What it did not take the
     * quasar reports as clipped (no damage).
     */
    double acceptJet(JetPacket jet);

    /** A short label for the quasar's GUI and scanner, e.g. "Crown Mk II" or "Wormhole Mouth to 120, 80, -40". */
    String dockLabel();
}
