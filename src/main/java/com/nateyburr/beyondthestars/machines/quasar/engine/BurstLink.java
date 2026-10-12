package com.nateyburr.beyondthestars.machines.quasar.engine;

import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;

/**
 * P4: a host's link check (quasar-p4.md R.1, §3.1, §5.2-§5.3; the prototype's {@code newLink} / {@code roll} and the
 * Stability / Heavy Link conditions in {@code conduitStep}). A 60 s rolling mean of what the host's ports received
 * from a linked quasar (and of one part of it, for the bank / halo fraction), and a streak that counts seconds while
 * the condition holds and resets to 0 the second it fails. The milestone completes at a streak of 1,800 s; the host
 * saves {@link #done} (and may save {@link #best}); the window itself is not saved (a reload restarts the mean, the
 * same as a link dropping).
 * <p>
 * Pure; the host calls {@link #roll} once per second with last second's delivery, then {@link #update} with the
 * condition.
 */
public final class BurstLink {

    /** The rolling window, seconds. */
    public static final int WINDOW = 60;
    /** 30 min unbroken. */
    public static final int STREAK_SECONDS = 1800;
    /** Stability Link (T6 → T7): mean ≥ D₇ on the rail, the high bank taking 25-75 %. */
    public static final double STABILITY_FRAC_MIN = 0.25, STABILITY_FRAC_MAX = 0.75;
    /** Heavy Link (T7 → T8): mean ≥ D₈ = 6 Bu/t on the Eye, at least 50 % on the halo (test fire). */
    public static final double HEAVY_FRAC_MIN = 0.5;

    private final double[] r = new double[WINDOW], h = new double[WINDOW];
    private double sum, p;
    /** Seconds rolled. */
    public long n;
    /** The 60 s mean, Bu/t, and the part's fraction of it. */
    public double mean, frac;
    public int streak, best;
    public boolean done;

    /** One second: {@code v} Bu/t received, {@code part} of it on the measured group. Prototype {@code roll}. */
    public void roll(double v, double part) {
        final int i = (int) (n % WINDOW);
        sum += v - r[i];
        p += part - h[i];
        r[i] = v;
        h[i] = part;
        n++;
        mean = Math.max(0, sum / WINDOW);
        frac = sum > 1e-9 ? p / sum : 0;
    }

    /**
     * The streak: +1 when {@code ok}, else 0; done at {@link #STREAK_SECONDS}. Returns true on the completing second.
     */
    public boolean update(boolean ok) {
        streak = ok ? streak + 1 : 0;
        best = Math.max(best, streak);
        if (!done && streak >= STREAK_SECONDS) {
            done = true;
            return true;
        }
        return false;
    }

    /** The window is full (60 s rolled since the host started measuring). */
    public boolean full() {
        return n >= WINDOW;
    }

    /** Streak progress 0..1 (the sensor's streak channel). */
    public double progress() {
        return Math.min(1, streak / (double) STREAK_SECONDS);
    }

    /** The Stability Link condition (roll the rail total with the high bank as the part). */
    public boolean stabilityOk(boolean linked) {
        return linked && full() && mean >= GalaxyField.D7 && frac >= STABILITY_FRAC_MIN && frac <= STABILITY_FRAC_MAX;
    }

    /** The Heavy Link condition (roll the Eye's total with the halo as the part; the halo test fire is on). */
    public boolean heavyOk(boolean linked, boolean testFire) {
        return linked && testFire && full() && mean >= GalaxyField.demandT(8) && frac >= HEAVY_FRAC_MIN;
    }

    /**
     * σ for a host with demand {@code demand} Bu/t: delivered ÷ D_h (Kerr 0.1 / 0.25, Homogeny 1.5 / 6, Reality
     * Diffraction 24; quasar-p4.md R.2).
     */
    public static double sigma(double delivered, double demand) {
        return demand > 0 ? delivered / demand : 0;
    }

    /** Host demands D_h, Bu/t (quasar-p4.md R.2, §3.1). */
    public static final double KERR_T1 = 0.1, KERR_T2 = 0.25, HOMOGENY_T2 = 1.5, HOMOGENY_T3 = 6, REALITY = 24;
}
