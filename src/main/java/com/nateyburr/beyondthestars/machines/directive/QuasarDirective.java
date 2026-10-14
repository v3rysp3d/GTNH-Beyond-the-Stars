package com.nateyburr.beyondthestars.machines.directive;

/**
 * P4: the Quasar Engine's Directive codes as pure maths (quasar-p4.md §8.1, the Disc set; the prototype's
 * {@code W.dir.disc}). The quasar's hatch and the offline proof ({@code QuasarRigSim}) both call it, so a simulated
 * gate rig reads exactly the bits the hatch puts out. No Minecraft types.
 *
 * <pre>
 * Disc: [1] E &lt; −w: pulse feed gas      [2] E &gt; +w: Space trim
 *       [4] Φ ≥ the purge setpoint: purge  [8] alarm: blow-out or re-forming, coil power lost (b = 0),
 *                                              a jet port venting, or a Conduit element closed
 * </pre>
 * 
 * The window w is the hatch's (default 5); E and Φ are last second's ({@code lagE}, {@code lagPhi}: the 1 s lag).
 */
public final class QuasarDirective {

    private QuasarDirective() {}

    /** What the quasar knows at the end of its second. */
    public static final class State {

        /** Last second's E and Φ. */
        public double lagE, lagPhi;
        /** The regulator's purge setpoint (GUI; 0 = no purge bit). */
        public double purgeAt = 40;
        /** The hole is open and the disc formed (not re-forming, not dormant). */
        public boolean open = true;
        /** The field b in use (0 = no coil power). */
        public double b = 3;
        /** Bu/t vented this second (a jet port with nothing docked, or a closed mouth). */
        public double vented;
        /** Every Conduit element fed from this quasar is open (upkeep paid). */
        public boolean conduitOpen = true;

        // ---- SMBH (smbh-v1.md §6, the Horizon set); all false without a lattice
        /** The hold-up was short last second: the mass over the safe limit leaked. */
        public boolean leaking;
        /** Seconds of Disc + Hold-up the Accretion Buffer holds (+∞ when nothing is needed). */
        public double bufferSeconds = Double.POSITIVE_INFINITY;
        /** The Unbound anchor was short: the hole sheds to the doubling the supply pays for. */
        public boolean anchorShort;
        /** Overdrive is set and its upkeep was not fully paid last second. */
        public boolean overdriveShort;
    }

    /** The venting threshold, Bu/t (below it a port counts as not venting). */
    public static final double VENT_EPS = 0.005;

    /**
     * The Horizon set's 4-bit code (SMBH, smbh-v1.md §6): [1] hold-up short (leaking) [2] feed short (the Accretion
     * Buffer under 30 s) [4] anchor short (shedding) [8] Overdrive upkeep short.
     */
    public static int horizon(State s) {
        int c = 0;
        if (s.leaking) c |= 1;
        if (s.bufferSeconds < com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.FEED_SHORT_SECONDS) {
            c |= 2;
        }
        if (s.anchorShort) c |= 4;
        if (s.overdriveShort) c |= 8;
        return c;
    }

    /** The Disc set's 4-bit code. */
    public static int disc(State s, double w) {
        int c = 0;
        if (s.lagE < -w) c |= 1;
        if (s.lagE > w) c |= 2;
        if (s.purgeAt > 0 && s.lagPhi >= s.purgeAt) c |= 4;
        if (!s.open || s.b == 0 || s.vented > VENT_EPS || !s.conduitOpen) c |= 8;
        return c;
    }
}
