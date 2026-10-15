package com.nateyburr.beyondthestars.machines.quasar.burst;

import java.util.Locale;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;

/**
 * P4: THE HOST-SIDE BURST API (quasar-p4.md R.2, §3.1), for every machine that takes bursts through GRB Receptor
 * Ports: the Eye (T7/T8), the Kerr, the Homogeny T2/T3, Reality Diffraction, and the Burst Thermaliser Crown's remote
 * feed. A host owns one {@code BurstHost} (the Homogeny and the Diffraction save its window, {@link #save}) and calls
 * {@link #second} once per host
 * second with its ports. It gets back last second's {@link BurstReading} and keeps:
 * <ul>
 * <li>{@link #sigma()} = last second's delivery ÷ the host's demand D_h ({@link BurstLink#sigma}; Kerr 0.1 / 0.25,
 * Homogeny 1.5 / 6, Reality 24 Bu/t), and {@link #sigmaMean()} on the 60 s mean. <b>Recipe conditions and
 * bonuses use {@link #sigmaMean()}</b> (F-3: the Φ sawtooth would trip a per-second floor; P4-50).</li>
 * <li>{@link #mean()}: the 60 s rolling mean of the total, Bu/t ({@link BurstLink#roll}).</li>
 * <li>{@link #linked()} / {@link #linkSource()}: the link (§3.1), re-derived every second.</li>
 * <li>{@link #paused()} (Q-8): a source that fed these ports is frozen or not loaded. The host must freeze its field,
 * cycle, streaks and means for that second (this object already holds its mean and σ still, so the link resumes where
 * it was). Hosts that use σ for a bonus or a recipe condition read {@link #sigmaInForce()}, which is 0 while paused:
 * Q-8 pauses the link's work, it does not keep it running on a held σ.</li>
 * </ul>
 * It reports itself to every quasar that fed it ({@link MTEQuasarEngine#reportHost}), so the quasar's Links list, its
 * σ / streak sensor channels and OpenComputers' {@code getLinks()} show the host. Milestone streaks (the Eye's
 * Stability and Heavy Links) are the host's own {@link BurstLink}s, rolled from the groups it reads.
 */
public final class BurstHost {

    private final BurstLink window = new BurstLink();
    private BurstReading last = BurstReading.EMPTY;
    private double sigma, demand;

    /**
     * One host second: reads {@code ports} (they learn their host's name), rolls the 60 s mean unless paused, works out
     * σ against {@code demand} Bu/t, and reports to the feeding quasars as {@code hostKey} / {@code hostLabel} with the
     * host's link streak progress {@code streak01} (0..1; 0 if the host has none).
     */
    public BurstReading second(Iterable<? extends MTEHatchGRBReceptor> ports, String hostKey, String hostLabel,
        double demand, double streak01) {
        final BurstReading r = read(ports, hostLabel);
        this.demand = demand;
        last = r;
        if (!r.paused) {
            window.roll(r.total, 0);
            sigma = BurstLink.sigma(r.total, demand);
        }
        for (Map.Entry<BurstSource, Double> e : r.bySource.entrySet()) {
            final MTEQuasarEngine q = e.getKey()
                .resolve();
            if (q != null) q.reportHost(hostKey, hostLabel, e.getValue(), sigma, streak01);
        }
        return r;
    }

    /**
     * QA-B42: the 60 s window into a host's tile NBT under {@code key}, so a reload resumes the mean instead of
     * pausing a run (or an Entanglement Medium mix) for up to a minute while it refills.
     */
    public void save(net.minecraft.nbt.NBTTagCompound nbt, String key) {
        final net.minecraft.nbt.NBTTagCompound t = new net.minecraft.nbt.NBTTagCompound();
        final double[] w = window.window();
        final net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        for (double v : w) list.appendTag(new net.minecraft.nbt.NBTTagDouble(v));
        t.setTag("w", list);
        t.setLong("n", window.n);
        t.setDouble("demand", demand);
        nbt.setTag(key, t);
    }

    /** Restores {@link #save}; a missing or malformed tag leaves an empty window. */
    public void load(net.minecraft.nbt.NBTTagCompound nbt, String key) {
        if (!nbt.hasKey(key)) return;
        final net.minecraft.nbt.NBTTagCompound t = nbt.getCompoundTag(key);
        final net.minecraft.nbt.NBTTagList list = t.getTagList("w", 6);
        if (list.tagCount() != 2 * BurstLink.WINDOW) return;
        final double[] w = new double[list.tagCount()];
        for (int i = 0; i < w.length; i++) {
            final double v = list.func_150309_d(i);
            w[i] = Double.isFinite(v) ? Math.max(0, v) : 0;
        }
        window.restore(w, t.getLong("n"));
        final double d = t.getDouble("demand");
        demand = Double.isFinite(d) ? Math.max(0, d) : 0;
        sigma = BurstLink.sigma(window.mean, demand);
    }

    /** Reads a set of ports into one reading (each port learns its host's name). Server thread. */
    public static BurstReading read(Iterable<? extends MTEHatchGRBReceptor> ports, String hostLabel) {
        final BurstReading r = new BurstReading();
        if (ports == null) return r;
        for (MTEHatchGRBReceptor p : ports) {
            if (p == null || !p.isValid()) continue;
            p.poll(hostLabel);
            r.merge(p.reading());
        }
        return r;
    }

    public BurstReading last() {
        return last;
    }

    /** Last second's Bu/t (0 while paused is not read: check {@link #paused()}). */
    public double delivered() {
        return last.total;
    }

    /** σ on last second's delivery. */
    public double sigma() {
        return sigma;
    }

    /** σ on the 60 s mean (recipe conditions and bonuses use this one). */
    public double sigmaMean() {
        return BurstLink.sigma(window.mean, demand);
    }

    /**
     * The σ a host may USE now (bonuses and recipe conditions): {@link #sigmaMean()}, or 0 while {@link #paused()}.
     * Q-8 pauses a host's link work while its quasar is frozen or unloaded (quasar-p4.md §3.1, R.6): the held mean is
     * only remembered so the link resumes where it was, never a free bonus while the quasar is down.
     */
    public double sigmaInForce() {
        return last.paused ? 0 : sigmaMean();
    }

    /** The 60 s rolling mean, Bu/t. */
    public double mean() {
        return window.mean;
    }

    /** True once 60 s have been rolled since the host started (a fresh host's mean is still filling). */
    public boolean meanFull() {
        return window.full();
    }

    public boolean linked() {
        return last.linked();
    }

    public BurstSource linkSource() {
        return last.linkSource();
    }

    public boolean paused() {
        return last.paused;
    }

    /** "Quasar link: x, y, z (dim d) · 2.31 Bu/t · σ 1.54", "No quasar link", "PAUSED: ..." (GUI, scanner). */
    public String linkLine() {
        if (last.paused) return last.line();
        final BurstSource s = last.linkSource();
        if (s == null) {
            return last.total > 1e-9
                ? "Bursts " + com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(last.total) + ", not linked"
                : "No quasar link";
        }
        return String.format(
            Locale.ROOT,
            "Quasar link: %s · %s · σ %.2f (60 s mean %.2f)",
            s.label(),
            com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(last.total),
            sigma,
            sigmaMean());
    }
}
