package com.nateyburr.beyondthestars.machines.eye.field;

import java.util.ArrayList;
import java.util.List;

/**
 * P4 planets mode (quasar-p4.md §4.8; the prototype's {@code planetTick}): 3 / 6 / 9 slots at T7 / T8 / T9, each
 * running one converted TecTech planet at parallel 2^(N − k). A run's success per parallel is (1 − 0.05·R) × the
 * field factor averaged over the run (Resonant / Stable 1, Strained 0.5, Critical 0). A field breach fails the runs
 * in flight ({@link #failRuns}); their inputs are spent. Pure: the runtime maps planet items to {@link Planet}s, pays
 * each run's H/He and EU at its start, and turns {@link Run#succeeded} into outputs.
 */
public final class PlanetSlots {

    /** A planet as the slots see it: rocket tier R, Eye tier k (R0-3 → 1, R4-6 → 2, R7-8 → 3, Deep Dark → 4). */
    public static final class Planet {

        public final String id, name;
        public final int R, k;
        /** Base run time, minutes ({@code 20 × 1.25^R} as converted, less compression). */
        public final double minutes;

        public Planet(String id, String name, int R, int k, double minutes) {
            this.id = id;
            this.name = name;
            this.R = R;
            this.k = k;
            this.minutes = minutes;
        }
    }

    /** The prototype's planet list (the golden test's; the mod's list comes from the P3 conversion). */
    public static final Planet[] PROTOTYPE = { new Planet("moon", "Moon", 1, 1, 25),
        new Planet("mars", "Mars", 2, 1, 31), new Planet("io", "Io", 3, 1, 39), new Planet("titan", "Titan", 5, 2, 61),
        new Planet("pluto", "Pluto", 6, 2, 76), new Planet("barc", "Barnard C", 7, 3, 95),
        new Planet("bare", "Barnard E", 8, 3, 119), new Planet("dd", "Deep Dark", 9, 4, 240) };

    /** A prototype planet by id, or null. */
    public static Planet prototype(String id) {
        for (Planet p : PROTOTYPE) if (p.id.equals(id)) return p;
        return null;
    }

    /** One slot's run in progress. */
    public static final class Slot {

        public final Planet planet;
        /** Eye-seconds run; Σ field factor; seconds counted. */
        public double t, zf;
        public int n;

        Slot(Planet planet) {
            this.planet = planet;
        }

        /** Progress 0..1. */
        public double progress() {
            return Math.min(1, t / (planet.minutes * 60));
        }
    }

    /** A finished run. */
    public static final class Run {

        public final Planet planet;
        public final double parallel, fieldFactor, succeeded;

        Run(Planet planet, double parallel, double fieldFactor, double succeeded) {
            this.planet = planet;
            this.parallel = parallel;
            this.fieldFactor = fieldFactor;
            this.succeeded = succeeded;
        }

        /** The prototype's ledger key. */
        public String key() {
            return planet.name + " (R" + planet.R + " → T" + planet.k + ") ×" + (long) parallel;
        }
    }

    public final Slot[] slots;

    /** Slots for these planets (at most {@link GalaxyField#planetSlots} of them are used). */
    public PlanetSlots(int tier, List<Planet> planets) {
        final int n = Math.min(planets.size(), GalaxyField.planetSlots(tier));
        slots = new Slot[n];
        for (int i = 0; i < n; i++) slots[i] = new Slot(planets.get(i));
    }

    /**
     * One lit field second at Eye tier {@code tier} in {@code zone}, dial {@code m}. Returns the runs that finished
     * (in slot order); their slots restart.
     */
    public List<Run> tick(int tier, int zone, double m) {
        final List<Run> done = new ArrayList<>();
        final double zf = GalaxyField.PLANET_ZONE_FACTOR[zone];
        for (Slot sl : slots) {
            final Planet p = sl.planet;
            sl.t += m;
            sl.zf += zf;
            sl.n++;
            if (sl.t >= p.minutes * 60) {
                final double par = GalaxyField.planetParallel(tier, p.k), f = sl.zf / sl.n;
                final double ok = par * GalaxyField.planetBase(p.R) * f;
                done.add(new Run(p, par, f, ok));
                sl.t = 0;
                sl.zf = 0;
                sl.n = 0;
            }
        }
        return done;
    }

    /** A field breach: every run in flight fails and restarts (its inputs are spent). */
    public void failRuns() {
        for (Slot sl : slots) {
            sl.t = 0;
            sl.zf = 0;
            sl.n = 0;
        }
    }

    /** True when a slot of this tier has no planet (the Directive Slots set's bit 1). */
    public boolean anyIdle(int tier) {
        return slots.length < GalaxyField.planetSlots(tier);
    }
}
