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
    private static int depth;

    private GpuTimer() {}

    /** Starts a timed section; pair with {@link #end()} in a finally. No-op unless {@link #ENABLED}. */
    public static void begin(String key) {
        if (!ENABLED) return;
        if (depth++ > 0) return; // nested: the outer section already times it
        openKey = key;
        openCpu = System.nanoTime();
        openQuery = 0;
        if (supported()) {
            final Integer q = FREE.poll();
            openQuery = q != null ? q : GL15.glGenQueries();
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, openQuery);
        }
    }

    /** Ends the section {@link #begin} opened. */
    public static void end() {
        if (!ENABLED || depth == 0) return;
        if (--depth > 0) return;
        final long cpu = System.nanoTime() - openCpu;
        final Sum s = sum(openKey);
        s.cpuNs += cpu;
        s.sections++;
        if (openQuery != 0) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            final Pending p = new Pending();
            p.query = openQuery;
            p.key = openKey;
            p.window = window;
            PENDING.add(p);
        }
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
        final StringBuilder detail = new StringBuilder();
        for (Map.Entry<String, Sum> e : SUMS.entrySet()) {
            final double gpu = frames == 0 ? 0 : e.getValue().gpuNs / 1e6 / frames;
            final double cpu = frames == 0 ? 0 : e.getValue().cpuNs / 1e6 / frames;
            r.gpuMs += gpu;
            r.cpuMs += cpu;
            if (detail.length() > 0) detail.append(' ');
            detail.append(String.format(Locale.ROOT, "%s=%.4f/%.4f", e.getKey(), gpu, cpu));
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
        /** {@code key=gpu/cpu} per key, ms per frame. */
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
