package com.nateyburr.beyondthestars.machines.conduit;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;
import com.nateyburr.beyondthestars.machines.quasar.render.BlockQuasarHole;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * P4: THE CONDUIT SOLVER's world side (quasar-p4.md §3, "Transport between elements" and "Solver").
 * <p>
 * <b>The walk.</b> A Quasar Engine pushes each jet port's second into the dock on its cell (a crown or a Wormhole
 * Mouth). From a mouth the stream goes on synchronously, in the same tick: the partner mouth emits it, a ray finds the
 * next element, that element passes it on (Mirror: the other throat; Junction: split or combine), and so on until a
 * GRB Receptor Port takes it or the ray finds nothing. So the network is walked from every jet port once a second, as
 * the design asks; a stream carries its source the whole way ({@code BurstStream}); at most
 * {@link BurstRouting#MAX_HOPS} elements; a stream that comes back to an element it crossed is a cycle and is dropped
 * (the element says so). Sinks keep last second's delivery, which is the D-49 1 s lag.
 * <p>
 * <b>Rays.</b> A stream leaves an out-throat along its outward normal. It enters another element's in-throat if the ray
 * crosses that opening's plane within its radius (+0.5), travelling into it (at most 60° off its axis), within
 * {@link #MAX_RAY} blocks (64 blocks of open air plus the throats' own depth, P4-47). Every cell on the way must be air
 * or one of our render blocks; the first GRB Receptor Port on the ray takes the stream; anything else blocks it
 * ("blocked
 * at x, y, z"). Elements cache each out-throat's route for {@link #ROUTE_TICKS} ticks and drop the cache on any
 * structure check.
 * <p>
 * <b>The registry.</b> Every formed element registers its in-throats here once a second ({@link #register}); entries
 * older than {@link #FRESH_TICKS} are ignored, so nothing here outlives a machine. Keyed per world (weakly), and an
 * entry holds its element weakly too (P4-93): the element's tile reaches its world, so a strong value would pin the key
 * and leak every unloaded world. Server thread only.
 */
public final class ConduitNet {

    private ConduitNet() {}

    /** A registration counts for this many ticks (two element seconds). */
    public static final long FRESH_TICKS = 40;
    /** A cached route is re-traced after this many ticks (the design's 5 s). */
    public static final long ROUTE_TICKS = 100;
    /** Throat centre to throat centre: 64 blocks of air plus up to 8 for the two throats' own depth (P4-47). */
    public static final double MAX_RAY = BurstRouting.MAX_HOP + 8;
    /** A stream enters an opening at most this far off its axis: cos 60°. */
    public static final double MIN_ENTRY_COS = 0.5;

    /** One element's in-throats, as last registered. The element is held weakly (P4-93). */
    private static final class Entry {

        private final WeakReference<IConduitElement> element;
        final ConduitThroats.WorldThroat[] in;
        final boolean[] twoSided;
        final long tick;

        Entry(IConduitElement element, ConduitThroats.WorldThroat[] in, boolean[] twoSided, long tick) {
            this.element = new WeakReference<>(element);
            this.in = in;
            this.twoSided = twoSided;
            this.tick = tick;
        }

        /** The element, or null once it is collected. */
        IConduitElement element() {
            return element.get();
        }

        /** Fresh at {@code now} and still a valid element. */
        boolean live(long now) {
            final IConduitElement el = element();
            return now - tick <= FRESH_TICKS && el != null && el.conduitValid();
        }
    }

    private static final Map<World, Map<String, Entry>> TABLE = new WeakHashMap<>();

    /**
     * An element says "these are my open in-throats" (once a second while formed). {@code twoSided[i]} lets a stream in
     * from either side of throat i (a Wormhole Mouth's ring).
     */
    public static void register(World world, IConduitElement element, ConduitThroats.WorldThroat[] in,
        boolean[] twoSided) {
        if (world == null || world.isRemote || element == null) return;
        TABLE.computeIfAbsent(world, w -> new HashMap<>())
            .put(element.conduitKey(), new Entry(element, in, twoSided, world.getTotalWorldTime()));
    }

    public static void unregister(World world, IConduitElement element) {
        final Map<String, Entry> m = world == null ? null : TABLE.get(world);
        if (m != null && element != null) m.remove(element.conduitKey());
    }

    /** The registered, fresh, valid element with this key, or null. */
    public static IConduitElement element(World world, String key) {
        final Map<String, Entry> m = world == null ? null : TABLE.get(world);
        final Entry e = m == null ? null : m.get(key);
        if (e == null) return null;
        if (!e.live(world.getTotalWorldTime())) {
            m.remove(key);
            return null;
        }
        return e.element();
    }

    // ===================================================================================== routes

    /** What a ray out of a throat reaches. */
    public enum Kind {
        /** Another element's in-throat. */
        ELEMENT,
        /** A GRB Receptor Port. */
        RECEPTOR,
        /** A block in the way. */
        BLOCKED,
        /** An unloaded chunk in the way. */
        UNLOADED,
        /** Nothing within range (the stream is lost). */
        NOTHING
    }

    /** One traced route. Immutable. */
    public static final class Route {

        public final Kind kind;
        /** ELEMENT: the target's key and throat. */
        public final String target;
        public final int throat;
        /** RECEPTOR / BLOCKED / UNLOADED: the cell. */
        public final int[] cell;
        public final double distance;
        public final String label;

        Route(Kind kind, String target, int throat, int[] cell, double distance, String label) {
            this.kind = kind;
            this.target = target;
            this.throat = throat;
            this.cell = cell;
            this.distance = distance;
            this.label = label;
        }

        /** "Conduit Mirror at 1, 2, 3 (40 blocks)", "blocked at 1, 2, 3", "nothing within 64 blocks". */
        public String describe() {
            switch (kind) {
                case ELEMENT:
                case RECEPTOR:
                    return label + String.format(Locale.ROOT, " (%.0f blocks)", distance);
                case BLOCKED:
                    return "blocked at " + cell[0] + ", " + cell[1] + ", " + cell[2] + " (" + label + ")";
                case UNLOADED:
                    return "an unloaded chunk at " + cell[0] + ", " + cell[1] + ", " + cell[2];
                default:
                    return "nothing within " + BurstRouting.MAX_HOP + " blocks: the stream is lost";
            }
        }
    }

    /**
     * Traces the ray out of {@code out} (an out-throat of {@code from}): the nearest open in-throat it enters, unless a
     * receptor or a block comes first.
     */
    public static Route trace(World world, IConduitElement from, ConduitThroats.WorldThroat out) {
        final double[] o = out.centre;
        final double[] d = out.normal;
        IConduitElement bestElement = null;
        int bestThroat = -1;
        double tBest = MAX_RAY;
        final Map<String, Entry> m = TABLE.get(world);
        if (m != null) {
            final long now = world.getTotalWorldTime();
            for (Iterator<Entry> it = m.values()
                .iterator(); it.hasNext();) {
                final Entry e = it.next();
                final IConduitElement el = e.element();
                if (!e.live(now)) {
                    it.remove();
                    continue;
                }
                if (el == from) continue;
                for (int i = 0; i < e.in.length; i++) {
                    final ConduitThroats.WorldThroat th = e.in[i];
                    final double dn = dot(d, th.normal);
                    // travelling into the opening: against its outward normal (either way for a two-sided ring)
                    if (e.twoSided[i] ? Math.abs(dn) < MIN_ENTRY_COS : dn > -MIN_ENTRY_COS) continue;
                    final double t = (dot(th.centre, th.normal) - dot(o, th.normal)) / dn;
                    if (t <= 0.5 || t > tBest) continue;
                    final double px = o[0] + t * d[0] - th.centre[0];
                    final double py = o[1] + t * d[1] - th.centre[1];
                    final double pz = o[2] + t * d[2] - th.centre[2];
                    if (Math.sqrt(px * px + py * py + pz * pz) > th.radius + 0.5) continue;
                    tBest = t;
                    bestElement = el;
                    bestThroat = th.index;
                }
            }
        }
        // walk the cells up to the opening (or the range): air and render blocks pass, a receptor takes it
        final Route hit = walk(world, o, d, tBest);
        if (hit != null) return hit;
        if (bestElement == null) return new Route(Kind.NOTHING, null, -1, null, MAX_RAY, "");
        return new Route(Kind.ELEMENT, bestElement.conduitKey(), bestThroat, null, tBest, bestElement.conduitLabel());
    }

    /**
     * Amanatides-Woo voxel walk from {@code o} along {@code d} (unit) up to distance {@code limit}; the start cell is
     * the emitter's.
     */
    private static Route walk(World world, double[] o, double[] d, double limit) {
        int x = floor(o[0]), y = floor(o[1]), z = floor(o[2]);
        final int sx = d[0] > 1e-9 ? 1 : d[0] < -1e-9 ? -1 : 0;
        final int sy = d[1] > 1e-9 ? 1 : d[1] < -1e-9 ? -1 : 0;
        final int sz = d[2] > 1e-9 ? 1 : d[2] < -1e-9 ? -1 : 0;
        double tx = sx == 0 ? Double.POSITIVE_INFINITY : (sx > 0 ? x + 1 - o[0] : o[0] - x) / Math.abs(d[0]);
        double ty = sy == 0 ? Double.POSITIVE_INFINITY : (sy > 0 ? y + 1 - o[1] : o[1] - y) / Math.abs(d[1]);
        double tz = sz == 0 ? Double.POSITIVE_INFINITY : (sz > 0 ? z + 1 - o[2] : o[2] - z) / Math.abs(d[2]);
        final double dx = sx == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(d[0]);
        final double dy = sy == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(d[1]);
        final double dz = sz == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(d[2]);
        for (int guard = 0; guard < 512; guard++) {
            final double t;
            if (tx <= ty && tx <= tz) {
                t = tx;
                x += sx;
                tx += dx;
            } else if (ty <= tz) {
                t = ty;
                y += sy;
                ty += dy;
            } else {
                t = tz;
                z += sz;
                tz += dz;
            }
            if (t >= limit - 1e-6) return null;
            if (y < 0 || y > 255) return new Route(Kind.NOTHING, null, -1, new int[] { x, y, z }, t, "");
            if (!world.blockExists(x, y, z)) {
                return new Route(Kind.UNLOADED, null, -1, new int[] { x, y, z }, t, "");
            }
            final Block block = world.getBlock(x, y, z);
            if (block.isAir(world, x, y, z) || block == BlockQuasarHole.INSTANCE) continue;
            final TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof IGregTechTileEntity) {
                final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
                if (mte instanceof MTEHatchGRBReceptor) {
                    return new Route(
                        Kind.RECEPTOR,
                        null,
                        -1,
                        new int[] { x, y, z },
                        t,
                        "GRB Receptor Port at " + x + ", " + y + ", " + z);
                }
            }
            return new Route(Kind.BLOCKED, null, -1, new int[] { x, y, z }, t, block.getLocalizedName());
        }
        return null;
    }

    /** The receptor a RECEPTOR route names, if it is still there. */
    public static MTEHatchGRBReceptor receptorAt(World world, int[] c) {
        if (c == null || c[1] < 0 || c[1] > 255 || !world.blockExists(c[0], c[1], c[2])) return null;
        final TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEHatchGRBReceptor ? (MTEHatchGRBReceptor) mte : null;
    }

    private static int floor(double v) {
        final int i = (int) v;
        return v < i ? i - 1 : i;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
