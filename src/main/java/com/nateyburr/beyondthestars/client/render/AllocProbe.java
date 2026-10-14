package com.nateyburr.beyondthestars.client.render;

import java.lang.management.ManagementFactory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (R-16, dev only): bytes the render thread has allocated so far, from HotSpot's per-thread counter
 * ({@code com.sun.management.ThreadMXBean#getThreadAllocatedBytes}). {@link GpuTimer} reads it round every timed
 * section, so the harness can show whether a renderer allocates per frame. Reading the counter allocates a little
 * itself (two small arrays on Java 8); {@link #overhead()} measures that and the reports subtract it.
 * <p>
 * Without the HotSpot extension (another VM) {@link #now()} returns -1 and nothing is reported.
 */
@SideOnly(Side.CLIENT)
public final class AllocProbe {

    private static boolean resolved;
    private static com.sun.management.ThreadMXBean bean;

    private AllocProbe() {}

    /** Bytes allocated by the current thread so far, or -1 when the VM cannot tell. */
    public static long now() {
        if (!resolved) resolve();
        if (bean == null) return -1;
        try {
            return bean.getThreadAllocatedBytes(
                Thread.currentThread()
                    .getId());
        } catch (RuntimeException | LinkageError bad) {
            bean = null;
            return -1;
        }
    }

    /** What one {@link #now()} pair costs by itself (the smallest of a few back-to-back pairs), bytes. */
    public static long overhead() {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            final long a = now();
            final long b = now();
            if (a < 0 || b < 0) return 0;
            min = Math.min(min, b - a);
        }
        return min == Long.MAX_VALUE ? 0 : min;
    }

    private static void resolve() {
        resolved = true;
        try {
            final java.lang.management.ThreadMXBean b = ManagementFactory.getThreadMXBean();
            if (b instanceof com.sun.management.ThreadMXBean) {
                final com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) b;
                if (sun.isThreadAllocatedMemorySupported()) {
                    if (!sun.isThreadAllocatedMemoryEnabled()) sun.setThreadAllocatedMemoryEnabled(true);
                    bean = sun;
                }
            }
        } catch (RuntimeException | LinkageError absent) {
            bean = null;
        }
    }
}
