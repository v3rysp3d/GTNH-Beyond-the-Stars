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
    /** SMBH: the source record (lattice tier in force and the hole's mass, SM) the packet carried; see JetPacket. */
    public final int lattice;
    public final double mass;

    private BurstStream(BurstSource source, JetPort port, double buPerTick, boolean linkable, long tick, String[] path,
        int lattice, double mass) {
        this.lattice = lattice;
        this.mass = mass;
        this.source = source;
        this.port = port;
        this.buPerTick = Math.max(0, buPerTick);
        this.linkable = linkable;
        this.tick = tick;
        this.path = path;
    }

    /** The stream a jet port sends into the dock on its cell. */
    public static BurstStream of(JetPacket p) {
        return new BurstStream(p.source, p.port, p.buPerTick, p.linkable, p.tick, new String[0], p.lattice, p.mass);
    }

    /** The same stream carrying {@code bu} instead (a Junction's share, a clipped throat). */
    public BurstStream withBu(double bu) {
        return new BurstStream(source, port, bu, linkable, tick, path, lattice, mass);
    }

    /** The stream after it crossed the element {@code key}. */
    public BurstStream through(String key) {
        final String[] p = Arrays.copyOf(path, path.length + 1);
        p[path.length] = key;
        return new BurstStream(source, port, buPerTick, linkable, tick, p, lattice, mass);
    }

    /**
     * SMBH (smbh-v1.md §1.3): the source can serve the Supermassive Link (Eye T9 -> T10): a lattice in force and a
     * hole of at least {@link com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams#SUPERMASSIVE_LINK_MASS}.
     */
    public boolean supermassiveSource() {
        return lattice > 0
            && mass >= com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.SUPERMASSIVE_LINK_MASS;
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
