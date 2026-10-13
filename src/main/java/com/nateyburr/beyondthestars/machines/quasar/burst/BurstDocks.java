package com.nateyburr.beyondthestars.machines.quasar.burst;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.World;

import gregtech.api.metatileentity.MetaTileEntity;

/**
 * P4: the server-side table of jet docks by intake cell ({@link IBurstDock}). A dock re-registers every second while
 * it is formed; an entry older than {@link #FRESH_TICKS} is ignored and dropped, so nothing here can outlive the
 * machine (an unloaded or broken dock simply stops registering). Keyed per world (weakly), so a world unload or a
 * second save in the same JVM never sees another world's docks; an entry holds its dock weakly too (P4-93), since the
 * dock's tile reaches its world and a strong value would pin the key. Server thread only.
 */
public final class BurstDocks {

    private BurstDocks() {}

    /** A registration counts for this many ticks (two dock seconds). */
    public static final long FRESH_TICKS = 40;

    private static final class Entry {

        private final WeakReference<IBurstDock> ref;
        final long tick;

        Entry(IBurstDock dock, long tick) {
            this.ref = new WeakReference<>(dock);
            this.tick = tick;
        }

        /** The dock, or null once it is collected. */
        IBurstDock dock() {
            return ref.get();
        }
    }

    private static final Map<World, Map<Long, Entry>> TABLE = new WeakHashMap<>();

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** A dock says "my intake is at (x, y, z)" (call once a second while formed and facing the right way). */
    public static void register(World world, int x, int y, int z, IBurstDock dock) {
        if (world == null || world.isRemote || dock == null) return;
        TABLE.computeIfAbsent(world, w -> new HashMap<>())
            .put(key(x, y, z), new Entry(dock, world.getTotalWorldTime()));
    }

    /** A dock leaving (unformed, broken): optional, since stale entries expire on their own. */
    public static void unregister(World world, int x, int y, int z, IBurstDock dock) {
        final Map<Long, Entry> m = world == null ? null : TABLE.get(world);
        if (m == null) return;
        final Entry e = m.get(key(x, y, z));
        if (e != null && e.dock() == dock) m.remove(key(x, y, z));
    }

    /** The ready dock whose intake is (x, y, z), or null (none, stale, invalid or not ready). */
    public static IBurstDock find(World world, int x, int y, int z) {
        final Map<Long, Entry> m = world == null ? null : TABLE.get(world);
        if (m == null) return null;
        final long k = key(x, y, z);
        final Entry e = m.get(k);
        if (e == null) return null;
        final IBurstDock dock = e.dock();
        if (world.getTotalWorldTime() - e.tick > FRESH_TICKS || !valid(dock)) {
            m.remove(k);
            return null;
        }
        return dock.dockReady() ? dock : null;
    }

    /** Drops every stale or dead entry of a world (the quasar calls it now and then; cheap). */
    public static void prune(World world) {
        final Map<Long, Entry> m = world == null ? null : TABLE.get(world);
        if (m == null) return;
        final long now = world.getTotalWorldTime();
        for (Iterator<Entry> it = m.values()
            .iterator(); it.hasNext();) {
            final Entry e = it.next();
            if (now - e.tick > FRESH_TICKS || !valid(e.dock())) it.remove();
        }
    }

    private static boolean valid(IBurstDock dock) {
        if (dock == null) return false;
        if (dock instanceof MetaTileEntity) {
            final MetaTileEntity mte = (MetaTileEntity) dock;
            return mte.isValid() && mte.getBaseMetaTileEntity() != null;
        }
        return true;
    }
}
