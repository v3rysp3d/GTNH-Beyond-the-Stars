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
}
