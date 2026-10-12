package com.nateyburr.beyondthestars.machines.quasar.engine;

/**
 * P4: what one disc second produced (quasar-p4.md §1.3-§1.4). The controller pushes {@link #jet} Bu/t into each jet
 * port (top and bottom; a port with nothing docked vents), drains {@link #gasTrimLitres} / {@link #spaceLitres}
 * from the trim hatches, and shows the rest.
 */
public final class QuasarResult {

    /** The field b actually used (0 without coil power). */
    public double b;
    /** Coil power drawn, EU/t (2·UXV·b²; charged while re-forming too). */
    public double coilEUt;
    /** Ṁ_Edd and f_eff this second. */
    public double medd, fEff;
    /** The stream, Bu/t, both jets together; each jet port carries {@link #jet} = G/2. */
    public double G, jet, G0;
    /** The trims applied, units, and their litres (units × 1,000 or 100 × M/64). */
    public double gasTrimUnits, spaceTrimUnits, gasTrimLitres, spaceLitres;
    /** This second's zone (−1 while re-forming); true when it counted towards the yield ledger. */
    public int zone = -1;
    public boolean yieldCounted;
    /** The disc is re-forming this second (no output). */
    public boolean reforming;
    /** The disc re-formed this second (E 0, Integrity 100, jets relit). */
    public boolean reformed;
    /** The disc blew out this second; {@link #blowOutReason} says why. M and a are kept. */
    public boolean blowOut;
    public String blowOutReason;
    /** Φ was dumped this second: a full eruption (Φ = 100) or a purge (Φ ≥ 20 on request). */
    public boolean eruption, purge;
    /** A purge was requested but refused (Φ under 20). */
    public boolean purgeRefused;
    /** Growth stopped at the coil's mass cap (the GUI says "coil cap"). */
    public boolean massCapped;
    /** Milestones reached this second. */
    public boolean firstLight, eddington, firstErupt;
    /** Index into {@link QuasarParams#MASS_MILESTONES} reached this second, or −1. */
    public int massMilestone = -1;
}
