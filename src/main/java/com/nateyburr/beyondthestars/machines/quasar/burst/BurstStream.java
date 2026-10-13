package com.nateyburr.beyondthestars.machines.quasar.burst;

import java.util.Arrays;

/**
 * P4: one stream in flight through the Einstein-Rosen Conduit (quasar-p4.md §3, §3.1). It starts as one jet port's
 * second ({@link #of(JetPacket)}) and keeps its <b>source</b> through every Mouth, Mirror and Junction, so the GRB
 * Receptor Port that finally takes it knows which quasar it came from: that is what a link is. Immutable: a Junction's
 * split makes new streams ({@link #withBu}), and each element it passes appends itself to the path
 * ({@link #through}), so a stream that comes back to an element it already crossed is a cycle and is dropped.
 */
public final class BurstStream {

    public final BurstSource source;
    public final JetPort port;
    /** Bu/t this stream carries this second. */
    public final double buPerTick;
    /** The source end of a link: formed, OPEN and b &gt; 0 (from the {@link JetPacket}). */
    public final boolean linkable;
    /** World time the jet was sent (the whole walk runs in that tick). */
    public final long tick;
    /** Elements crossed so far, as {@code dim:x,y,z} keys; its length is the hop count. */
    private final String[] path;

    private BurstStream(BurstSource source, JetPort port, double buPerTick, boolean linkable, long tick,
        String[] path) {
        this.source = source;
        this.port = port;
        this.buPerTick = Math.max(0, buPerTick);
        this.linkable = linkable;
        this.tick = tick;
        this.path = path;
    }

    /** The stream a jet port sends into the dock on its cell. */
    public static BurstStream of(JetPacket p) {
        return new BurstStream(p.source, p.port, p.buPerTick, p.linkable, p.tick, new String[0]);
    }

    /** The same stream carrying {@code bu} instead (a Junction's share, a clipped throat). */
    public BurstStream withBu(double bu) {
        return new BurstStream(source, port, bu, linkable, tick, path);
    }

    /** The stream after it crossed the element {@code key}. */
    public BurstStream through(String key) {
        final String[] p = Arrays.copyOf(path, path.length + 1);
        p[path.length] = key;
        return new BurstStream(source, port, buPerTick, linkable, tick, p);
    }

    /** True when the stream already crossed {@code key} (a cycle). */
    public boolean crossed(String key) {
        for (String k : path) if (k.equals(key)) return true;
        return false;
    }

    /** Elements crossed so far. */
    public int hops() {
        return path.length;
    }

    /** "source|port": the per-second identity of a stream for the throughput books and the receptors. */
    public String key() {
        return source.dim + ":" + source.x + "," + source.y + "," + source.z + "|" + port.key;
    }

    /** An element's key in a path: {@code dim:x,y,z}. */
    public static String elementKey(int dim, int x, int y, int z) {
        return dim + ":" + x + "," + y + "," + z;
    }
}
