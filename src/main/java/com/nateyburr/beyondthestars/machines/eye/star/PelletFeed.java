package com.nateyburr.beyondthestars.machines.eye.star;

/**
 * P5: the Pellet Injector's state as the controller read it this field second (p5-t9-t10.md §3.3): the barrel, the
 * pellet stock by grade (the injectors' own slots and any pellet stock buses in the top collar's sockets), the grade
 * lock, the rate law's centre and gain, and the level of the injector's Burst Control Hatch. Plain data; the host fills
 * it once a second.
 */
public final class PelletFeed {

    /** No Pellet Injector Hatch stands. */
    public static final PelletFeed NONE = new PelletFeed();

    /** Pellet Injector Hatches standing (the first one's settings count). */
    public int injectors;
    /** The best Injector Barrel fitted (1..3), 0 none. */
    public int barrelMk;
    /** Pellets in stock by grade (index 1 Magnetar, 2 Ergomatter, 3 Superdense). */
    public final long[] stock = new long[4];
    /** The grade locked in the injector's GUI (1..3), 0 = fire the highest grade in stock. */
    public int lockedGrade;
    /** The rate law's centre: auto (ρ = 1 at V = 0) or {@link #centre}; its gain k. */
    public boolean centreAuto = true;
    public double centre = 0.5, gain = 1;
    /** The level of the injector's Burst Control Hatch (0-15), −1 when none stands. */
    public int level = -1;

    /** The grade to fire: the locked one, else the highest in stock (0 when none). */
    public int grade() {
        if (lockedGrade >= 1 && lockedGrade <= 3) return lockedGrade;
        for (int g = 3; g >= 1; g--) if (stock[g] > 0) return g;
        return 0;
    }
}
