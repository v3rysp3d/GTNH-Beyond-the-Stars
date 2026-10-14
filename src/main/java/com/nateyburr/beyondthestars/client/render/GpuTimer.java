package com.nateyburr.beyondthestars.client.render;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.1, §3.1): the GPU-time probe of the render QA harness. Dev only: with
 * {@code -Dbts.rendertest.gputime=true} every renderer's draw is wrapped in a {@code GL_TIME_ELAPSED} query (GL 3.3 /
 * ARB_timer_query) and timed on the CPU; otherwise {@link #begin} is one boolean test and nothing else happens.
 * <p>
 * Results are read back without stalling (a query is polled each frame until its result is available, normally one to
 * three frames later) and summed per key over the window since {@link #reset()}; {@link #report()} gives each key's
 * GPU and CPU milliseconds per frame. The harness resets when a view starts holding still and reports at the shot, so
 * the numbers cover the still window only (the FPS counter is capped and measures nothing). Queries do not nest: a
 * {@link #begin} inside another section times the CPU only.
 */
@SideOnly(Side.CLIENT)
public final class GpuTimer {

    public static final boolean ENABLED = Boolean.getBoolean("bts.rendertest.gputime");

    private static final class Pending {

        int query;
        String key;
        int window;
    }

    private static final class Sum {

        long gpuNs;
        long cpuNs;
        /** Bytes the render thread allocated inside the sections (R-16), the probe's own cost taken off. */
        long allocBytes;
        int sections;
    }

    private static Boolean supported;
    private static final ArrayDeque<Integer> FREE = new ArrayDeque<>();
    private static final ArrayDeque<Pending> PENDING = new ArrayDeque<>();
    private static final Map<String, Sum> SUMS = new LinkedHashMap<>();
    private static int window;
    private static int frames;

    /** The open section: its key, query (0: CPU only), CPU start. */
    private static String openKey;
    private static int openQuery;
    private static long openCpu;
    private static long openAlloc;
    private static int depth;
    /** What one allocation-counter pair costs by itself (measured at each report). */
    private static long allocOverhead;

    private GpuTimer() {}

    /** Starts a timed section; pair with {@link #end()} in a finally. No-op unless {@link #ENABLED}. */
    public static void begin(String key) {
        if (!ENABLED && !GlStateCheck.ENABLED) return;
        if (depth++ > 0) return; // nested: the outer section already times it
        openKey = key;
        // R-16: the GL state leak check reads the state round the same sections (never in a timing run)
        if (GlStateCheck.ENABLED) GlStateCheck.before(key);
        if (!ENABLED) return;
        openQuery = 0;
        if (supported()) {
            final Integer q = FREE.poll();
            openQuery = q != null ? q : GL15.glGenQueries();
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, openQuery);
        }
        openAlloc = AllocProbe.now();
        openCpu = System.nanoTime();
    }

    /** Ends the section {@link #begin} opened. */
    public static void end() {
        if ((!ENABLED && !GlStateCheck.ENABLED) || depth == 0) return;
        if (--depth > 0) return;
        if (ENABLED) {
            final long cpu = System.nanoTime() - openCpu;
            final long alloc = openAlloc < 0 ? 0 : AllocProbe.now() - openAlloc;
            final Sum s = sum(openKey);
            s.cpuNs += cpu;
            s.allocBytes += alloc;
            s.sections++;
            if (openQuery != 0) {
                GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
                final Pending p = new Pending();
                p.query = openQuery;
                p.key = openKey;
                p.window = window;
                PENDING.add(p);
            }
        }
        if (GlStateCheck.ENABLED) GlStateCheck.after();
        openKey = null;
        openQuery = 0;
    }

    /** Starts a new measuring window (the harness: when a view starts holding still). */
    public static void reset() {
        window++;
        frames = 0;
        SUMS.clear();
    }

    /** Each key's GPU / CPU milliseconds per frame over the window, and the frame count. */
    public static Report report() {
        pollResults();
        final Report r = new Report();
        r.frames = frames;
        allocOverhead = AllocProbe.overhead();
        final StringBuilder detail = new StringBuilder();
        for (Map.Entry<String, Sum> e : SUMS.entrySet()) {
            final Sum s = e.getValue();
            final double gpu = frames == 0 ? 0 : s.gpuNs / 1e6 / frames;
            final double cpu = frames == 0 ? 0 : s.cpuNs / 1e6 / frames;
            // bytes per frame in the section, the counter's own cost per section taken off
            final double alloc = frames == 0 ? 0
                : Math.max(0, s.allocBytes - (double) allocOverhead * s.sections) / frames;
            r.gpuMs += gpu;
            r.cpuMs += cpu;
            r.allocBytes += alloc;
            if (detail.length() > 0) detail.append(' ');
            detail.append(String.format(Locale.ROOT, "%s=%.4f/%.4f/%.0fB", e.getKey(), gpu, cpu, alloc));
        }
        r.detail = detail.length() == 0 ? "-" : detail.toString();
        r.gpuSupported = supported();
        return r;
    }

    /** A window's totals. */
    public static final class Report {

        public int frames;
        public double gpuMs;
        public double cpuMs;
        /** Bytes allocated per frame inside the sections (R-16). */
        public double allocBytes;
        /** {@code key=gpu/cpu/allocB} per key: ms per frame, bytes per frame. */
        public String detail;
        public boolean gpuSupported;
    }

    static void onFrameEnd() {
        if (!ENABLED) return;
        frames++;
        pollResults();
    }

    private static void pollResults() {
        if (!supported()) return;
        final Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            final Pending p = it.next();
            if (GL15.glGetQueryObjecti(p.query, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) break; // in order
            final long ns = GL33.glGetQueryObjecti64(p.query, GL15.GL_QUERY_RESULT);
            if (p.window == window) sum(p.key).gpuNs += ns;
            FREE.add(p.query);
            it.remove();
        }
    }

    private static Sum sum(String key) {
        Sum s = SUMS.get(key);
        if (s == null) {
            s = new Sum();
            SUMS.put(key, s);
        }
        return s;
    }

    private static boolean supported() {
        if (supported == null) {
            boolean ok;
            try {
                ok = GLContext.getCapabilities().OpenGL33;
            } catch (RuntimeException | LinkageError bad) {
                ok = false;
            }
            supported = ok;
            BeyondTheStars.LOG.info(
                "[BTS render] GPU timer {}",
                ok ? "on (GL_TIME_ELAPSED queries)" : "CPU only: the context has no GL 3.3 timer queries");
        }
        return supported;
    }
}
