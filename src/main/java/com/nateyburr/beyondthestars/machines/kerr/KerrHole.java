package com.nateyburr.beyondthestars.machines.kerr;

/**
 * KERR REDESIGN: the read-only view a Kerr module has of its Kerr Ergosphere Engine
 * (docs/kerr-redesign.md §2.2). {@link MTEKerrEngine} implements it; a module never calls anything
 * else on the KEE, and never changes the hole.
 */
public interface KerrHole {

    /** The KEE's structure is complete (an unformed KEE freezes its hole and pauses every module). */
    boolean isFormed();

    /** A hole is open (a seed went in and it has not collapsed or evaporated). */
    boolean isHoleOpen();

    /** The hole's mass in solar masses (SM); 0 while closed. */
    double mass();

    /** The Kerr Density Upgrade (Kerr T2) is installed: DENSITY recipes are possible. */
    boolean densityInstalled();

    /**
     * P4 (quasar-p4.md §3.1): the quasar link's σ at this KEE on the 60 s mean (delivered ÷ 0.1 Bu/t at T1, 0.25 at
     * T2), 0 with no GRB Receptor Port or no bursts. 0 while its quasar is frozen or unloaded (Q-8 pauses the link).
     */
    double burstSigma();
}
