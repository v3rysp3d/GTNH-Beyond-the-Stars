package com.nateyburr.beyondthestars.machines.directive;

/**
 * P5: the codes of the two new Eye Directive sets (designs/docs/p5-t9-t10.md §9.3; the prototype's P5
 * {@code worldTick} wrapper), bit n = 2^(n−1), in the Degasser pattern. Pure.
 * <ul>
 * <li><b>EYE_FLOW</b> (T9 lit): 1 V &gt; +w (fire faster), 2 V &lt; −w (fire slower), 4 a Galactic Flow is coming and
 * accepted, 8 the beam starved: pellets low (under 120 s of fire at the current rate; the prototype: none to fire),
 * or (B-222) the beam firing nothing while V is above +5.</li>
 * <li><b>EYE_UNIVERSE</b> (T10 lit): 1 H &gt; +w (Dark Matter trim), 2 H &lt; −w (SpaceTime trim), 4 a countdown is
 * running (Recombination or Cosmic Dawn), 8 the next universe's charge is short.</li>
 * </ul>
 */
public final class P5Directive {

    private P5Directive() {}

    /** The pellet stock counts as low below this many seconds of fire at the current rate. */
    public static final double PELLETS_LOW_SECONDS = 120;

    public static int flow(boolean lit9, double V, boolean flowAccepted, boolean beamStarved, double w) {
        if (!lit9) return 0;
        return (V > w ? 1 : 0) | (V < -w ? 2 : 0) | (flowAccepted ? 4 : 0) | (beamStarved ? 8 : 0);
    }

    public static int universe(boolean lit10, double H, boolean countdown, boolean chargeShort, double w) {
        if (!lit10) return 0;
        return (H > w ? 1 : 0) | (H < -w ? 2 : 0) | (countdown ? 4 : 0) | (chargeShort ? 8 : 0);
    }
}
