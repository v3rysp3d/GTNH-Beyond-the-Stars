package com.nateyburr.beyondthestars.machines.quasar.engine;

import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;

/**
 * P4: the Quasar Engine's hole and accretion disc, the mutable state (quasar-p4.md §1; the prototype's
 * {@code newQuasar}). A plain data holder; {@link QuasarDisc} does the maths. The MTE saves every field to tile NBT,
 * and {@link #M} / {@link #a} also to the controller item's NBT (a wrench keeps the hole, §1.1). Doubles must be saved
 * as doubles.
 */
public final class QuasarState {

    /** The hole's state. */
    public enum Hole {

        /** No hole yet: insert a Quasar Seed and Open. */
        NONE,
        /** Open: the disc runs. */
        OPEN,
        /** The disc blew out; it re-forms over 300 s once f ≥ 0.3. M and a are kept. */
        REFORM,
        /** Collapsed by the GUI: M and a are kept; re-open with a Black Hole Seed. */
        DORMANT
    }

    public Hole hole = Hole.NONE;
    /** Mass, SM. */
    public double M = QuasarParams.SEED_MASS;
    /** Spin, 0..0.998. */
    public double a = QuasarParams.SEED_SPIN;
    /** Eddington balance, −120..+120 (target 0). */
    public double E;
    /** Horizon flux, 0..100. */
    public double Phi;
    /** Disc integrity, 0..100. */
    public double I = 100;
    /** Harmonic Lock streak: consecutive seconds Resonant. */
    public int lock;
    /** This second's zone (0 Resonant .. 4 Breach; −1 while re-forming or after a blow-out). */
    public int zone;
    /** This second's yield Y_q (0 while re-forming). */
    public double Yq = 1.25;
    /** Eruption seconds left, and the eruption's extra stream, Bu/t. */
    public int erupt;
    public double eruptG;
    /** Re-forming seconds counted. */
    public int reform;
    /** Last second's E and Φ (the 1 s sensor lag: rigs, sensors and Directive bits read these). */
    public double lagE, lagPhi;
    /** This second's G0, G (Bu/t, both jets), λ, λc, f_eff, b and Ṁ_Edd. */
    public double G0, G, lam = 1, lc = 1, fEff, b, Medd;
    /** A purge request (Burst Control Hatch rising edge, OpenComputers, the collapse assist); consumed next step. */
    public boolean purgeLatch;
    /** Seconds left on the eruption flash (render). */
    public int flash;
    /** True when the last dump was a full eruption (false: a purge). */
    public boolean lastWasEruption;

    // ---- milestones (display only)
    public boolean firstLight, eddDone, firstErupt;
    /** Consecutive seconds Resonant at λ ≥ 0.95. */
    public int edd;
    /** Bit i set once {@link QuasarParams#MASS_MILESTONES}[i] was reached. */
    public int massMilestones;

    /** The disc noise; save {@link FieldRng#state()}. */
    public FieldRng rng;

    public QuasarState(FieldRng rng) {
        this.rng = rng;
    }

    /** The prototype's seeding of its disc RNG: {@code mulberry((seed*40503+7)>>>0)}. */
    public static FieldRng rngFor(long seed) {
        return FieldRng.ofState((int) ((seed * 40503L + 7) & 0xFFFFFFFFL));
    }

    /** True while the jets can run (the hole is open and the disc formed). */
    public boolean open() {
        return hole == Hole.OPEN;
    }

    /** Requests a purge (consumed by the next {@link QuasarDisc#step} that is not re-forming; refused under Φ 20). */
    public void requestPurge() {
        purgeLatch = true;
    }

    /**
     * Seeds a new hole from a Quasar Seed: 64 SM, a = 0, the disc empty and settled (E 0, Φ 0, Integrity 100).
     * {@code a0} lets a test start spun up (the prototype starts at a_eq).
     */
    public void seed(double M0, double a0) {
        hole = Hole.OPEN;
        M = M0;
        a = a0;
        resetDisc();
    }

    /** The GUI's Collapse: dormant, M and a kept. */
    public void collapse() {
        hole = Hole.DORMANT;
        G = 0;
        G0 = 0;
        erupt = 0;
        zone = -1;
        Yq = 0;
    }

    /** Re-opens a dormant hole (a Black Hole Seed consumed by the MTE): the disc starts empty. */
    public void reopen() {
        if (hole != Hole.DORMANT) return;
        hole = Hole.OPEN;
        resetDisc();
    }

    private void resetDisc() {
        E = 0;
        Phi = 0;
        I = 100;
        lock = 0;
        zone = 0;
        Yq = 1.25;
        erupt = 0;
        eruptG = 0;
        reform = 0;
        lagE = 0;
        lagPhi = 0;
        G0 = 0;
        G = 0;
    }

    /** Stability readout, clamp(100 − |E|, 0, 100). */
    public double stability() {
        return QuasarParams.stability(E);
    }
}
