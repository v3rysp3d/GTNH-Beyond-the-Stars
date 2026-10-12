package com.nateyburr.beyondthestars.machines.directive;

/**
 * AUTOMATION PASS: the Kerr's Directive codes as pure maths (no Minecraft types). The KEE's hatch
 * ({@code MTEKerrEngine.directiveCode}) and the offline proof ({@link DirectiveRigSim}) both call it, so the
 * simulated gate rig reads exactly the bits the hatch puts out.
 */
public final class KerrDirective {

    private KerrDirective() {}

    /** The Hole set's "above / below the setpoint" band, SM. */
    public static final double BAND = 1.0;

    /** What the KEE knows at the end of a second. */
    public static final class State {

        public boolean formed, open, powerOk = true;
        public double mass, setpoint, boltStockSm, bleedPerTick, stability = 1;
        public int seeds;
    }

    /** Bolts short: an open hole's stock lasts under {@code minutes} at the current bleed; a closed hole has none. */
    public static boolean boltsShort(State s, int minutes) {
        if (!s.open) return s.boltStockSm <= 0;
        if (s.boltStockSm <= 0) return true;
        final double perSecond = Math.max(s.bleedPerTick, 0) * 20;
        return perSecond > 0 && s.boltStockSm / perSecond < minutes * 60.0;
    }

    /**
     * Hole: 1 closed (formed), 2 mass more than {@link #BAND} below the setpoint, 4 bolts short, 8 more than the band
     * above. Status: 1 containment power short, 2 no Black Hole Seed while closed, 4 stability below 100 %, 8 not
     * formed. Other sets: 0.
     */
    public static int code(DirectiveSet set, State s, int window) {
        int c = 0;
        if (set == DirectiveSet.KERR_HOLE) {
            if (s.formed && !s.open) c |= 1;
            if (s.open && s.mass < s.setpoint - BAND) c |= 2;
            if (boltsShort(s, window)) c |= 4;
            if (s.open && s.mass > s.setpoint + BAND) c |= 8;
        } else if (set == DirectiveSet.KERR_STATUS) {
            if (s.open && !s.powerOk) c |= 1;
            if (!s.open && s.seeds == 0) c |= 2;
            if (s.open && s.stability < 1 - 1e-9) c |= 4;
            if (!s.formed) c |= 8;
        }
        return c;
    }
}
