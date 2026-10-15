package com.nateyburr.beyondthestars.machines.quasar.engine;

import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;

/**
 * P5: the Deep Link, the T8 → T9 milestone (designs/docs/p5-t9-t10.md §2.8; the prototype's {@code newLink9} /
 * {@code deepLinkTick}). Linked; a 60 s mean of at least D₉ = 24 Bu/t landing on the Eye's ports, at least half of it
 * test-fired into the collar ports (no field effect at T8); and the beam test-firing at a 60 s mean ρ ≥ 1; for 1,800 s
 * unbroken. The test fire forces J1 to the Eye. Pure; the host rolls it once per second while the T8 test fire is on.
 */
public final class DeepLink {

    /** The beam's 60 s mean ρ it needs. */
    public static final double RHO_MIN = 1;
    /** The collar ports' share of the Eye's bursts it needs. */
    public static final double COLLAR_FRAC_MIN = 0.5;
    /** The collar Junction share of the test fire (the prototype's c4t). */
    public static final double TEST_FIRE_SHARE = 0.75;

    /** The Eye's total and its collar part. */
    public final BurstLink link = new BurstLink();
    private final double[] rr = new double[BurstLink.WINDOW];
    private double rsum;
    /** The beam's 60 s mean ρ. */
    public double rho;

    /** One second: {@code eye} Bu/t on the Eye's ports, {@code collar} of it on the collar ports, the beam at ρ. */
    public void roll(double eye, double collar, double beamRho) {
        final int i = (int) (link.n % BurstLink.WINDOW);
        rsum += beamRho - rr[i];
        rr[i] = beamRho;
        link.roll(eye, collar);
        rho = rsum / BurstLink.WINDOW;
    }

    /** The condition this second. */
    public boolean ok(boolean linked) {
        return linked && link.full()
            && link.mean >= GalaxyField.demandT(9)
            && link.frac >= COLLAR_FRAC_MIN
            && rho >= RHO_MIN;
    }

    /** The streak (+1 when ok, else 0); true on the completing second. */
    public boolean update(boolean ok) {
        return link.update(ok);
    }

    public boolean done() {
        return link.done;
    }

    /** Persistence: the Eye's and collar windows (as {@link BurstLink#window()}) then the 60 ρ values. */
    public double[] window() {
        final double[] w = link.window(), out = new double[w.length + BurstLink.WINDOW];
        System.arraycopy(w, 0, out, 0, w.length);
        System.arraycopy(rr, 0, out, w.length, BurstLink.WINDOW);
        return out;
    }

    public void restore(double[] saved, long rolled) {
        if (saved == null || saved.length != 3 * BurstLink.WINDOW) return;
        final double[] w = new double[2 * BurstLink.WINDOW];
        System.arraycopy(saved, 0, w, 0, w.length);
        link.restore(w, rolled);
        System.arraycopy(saved, w.length, rr, 0, BurstLink.WINDOW);
        rsum = 0;
        for (double v : rr) rsum += v;
        rho = rsum / BurstLink.WINDOW;
    }
}
