package com.nateyburr.beyondthestars.machines.quasar.burst;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * P4: what one or more GRB Receptor Ports received last second (quasar-p4.md §3.1), the host-side view of the
 * conduit. A host reads its ports once a second ({@code MTEHatchGRBReceptor.reading} for one port,
 * {@link BurstHost} for a host's whole set) and gets:
 * <ul>
 * <li>{@link #total} Bu/t, and {@link #bySource} (each source quasar's share);</li>
 * <li>{@link #linkedSources}: the quasars it is <b>linked</b> to. A source is linked while, last second, at least one
 * port received bursts (&gt; 0 Bu/t) from it on a stream whose source end was linkable (formed, hole OPEN, b &gt; 0)
 * and whose whole path was open: a closed element or a blocked ray simply delivers nothing, so the path condition is
 * the delivery itself. Nothing about a link is saved.</li>
 * <li>{@link #paused} (Q-8): a source that fed these ports recently is FROZEN or not loaded. The host must hold its
 * field / cycle / means still instead of reading the missing bursts as zero. A source that is RUNNING but no longer
 * reaches the ports (its path broke), or GONE, is felt in full.</li>
 * </ul>
 */
public final class BurstReading {

    /** No ports, nothing received. */
    public static final BurstReading EMPTY = new BurstReading();

    public double total;
    public final Map<BurstSource, Double> bySource = new LinkedHashMap<>();
    public final Set<BurstSource> linkedSources = new LinkedHashSet<>();
    public boolean paused;
    /** The first source that pauses this host (Q-8), or null. */
    public BurstSource pausedBy;
    /** Ports read. */
    public int ports;

    /** Adds one delivery (a port's record). */
    public void add(BurstSource source, double bu, boolean linkable) {
        if (source == null) return;
        total += bu;
        bySource.merge(source, bu, Double::sum);
        if (linkable && bu > 1e-9) linkedSources.add(source);
    }

    /** Folds another port's reading into this one. */
    public void merge(BurstReading o) {
        if (o == null) return;
        total += o.total;
        for (Map.Entry<BurstSource, Double> e : o.bySource.entrySet())
            bySource.merge(e.getKey(), e.getValue(), Double::sum);
        linkedSources.addAll(o.linkedSources);
        if (o.paused && !paused) {
            paused = true;
            pausedBy = o.pausedBy;
        }
        ports += o.ports;
    }

    /** True while at least one source is linked (§3.1). */
    public boolean linked() {
        return !linkedSources.isEmpty();
    }

    /** The linked source that delivered the most, or null. */
    public BurstSource linkSource() {
        BurstSource best = null;
        double bu = -1;
        for (BurstSource s : linkedSources) {
            final double v = bySource.getOrDefault(s, 0d);
            if (v > bu) {
                bu = v;
                best = s;
            }
        }
        return best;
    }

    public Map<BurstSource, Double> bySourceView() {
        return Collections.unmodifiableMap(bySource);
    }

    /** "2.31 Bu/t from 120, 80, -40 (dim 0)", "paused: Quasar ... not loaded", "nothing received". */
    public String line() {
        if (paused) return "PAUSED: its quasar at " + (pausedBy == null ? "?" : pausedBy.label()) + " is not running";
        if (bySource.isEmpty()) return "nothing received";
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<BurstSource, Double> e : bySource.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(e.getValue()))
                .append(" from ")
                .append(
                    e.getKey()
                        .label())
                .append(linkedSources.contains(e.getKey()) ? " (linked)" : "");
        }
        return sb.toString();
    }
}
