package com.nateyburr.beyondthestars.rendertest;

import java.util.Arrays;
import java.util.Locale;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

import com.nateyburr.beyondthestars.client.render.AllocProbe;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY render QA harness (renders v2, R-16): what a whole frame costs, so the big renders can be compared with
 * the scene drawn without them. Per frame, over the window since {@link #reset()}:
 * <ul>
 * <li>the interval between two frame starts (wall clock: what the FPS is; the harness holds the frame cap and vsync
 * off, so it measures the frame and not the monitor);</li>
 * <li>the CPU time of the render tick (world, tile entities, hand and GUI; not the buffer swap);</li>
 * <li>the GPU time between two {@code GL_TIMESTAMP} queries at the render tick's start and end (it includes the time
 * the GPU waits for the CPU to submit, so a CPU-bound frame reads about its CPU time);</li>
 * <li>the bytes the render thread allocated during the render tick.</li>
 * </ul>
 * Reported as mean, median, 95th and 99th percentile and the worst frame.
 */
@SideOnly(Side.CLIENT)
public final class FrameProbe {

    private static final FrameProbe INSTANCE = new FrameProbe();
    private static final int CAP = 16384;
    private static final int RING = 16;

    private final double[] interval = new double[CAP], cpu = new double[CAP], gpu = new double[CAP];
    private final long[] alloc = new long[CAP];
    private int nInterval, nCpu, nGpu;
    private long lastStart, start, startAlloc;
    private int window;

    private Boolean timestamps;
    private final int[] qStart = new int[RING], qEnd = new int[RING], qWindow = new int[RING];
    private final boolean[] busy = new boolean[RING];
    private int slot = -1;

    private FrameProbe() {}

    static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    static FrameProbe get() {
        return INSTANCE;
    }

    /** A new measuring window (the harness: when a view starts holding still). */
    void reset() {
        window++;
        nInterval = nCpu = nGpu = 0;
        lastStart = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        final long now = System.nanoTime();
        if (lastStart != 0 && nInterval < CAP) interval[nInterval++] = (now - lastStart) / 1e6;
        lastStart = now;
        start = now;
        startAlloc = AllocProbe.now();
        slot = -1;
        if (timestampsSupported()) {
            poll();
            for (int i = 0; i < RING; i++) {
                if (!busy[i]) {
                    slot = i;
                    break;
                }
            }
            if (slot >= 0) {
                if (qStart[slot] == 0) {
                    qStart[slot] = GL15.glGenQueries();
                    qEnd[slot] = GL15.glGenQueries();
                }
                GL33.glQueryCounter(qStart[slot], GL33.GL_TIMESTAMP);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || start == 0) return;
        final long a = AllocProbe.now();
        if (nCpu < CAP) {
            cpu[nCpu] = (System.nanoTime() - start) / 1e6;
            alloc[nCpu] = startAlloc < 0 || a < 0 ? 0 : a - startAlloc;
            nCpu++;
        }
        if (slot >= 0) {
            GL33.glQueryCounter(qEnd[slot], GL33.GL_TIMESTAMP);
            busy[slot] = true;
            qWindow[slot] = window;
        }
    }

    private void poll() {
        for (int i = 0; i < RING; i++) {
            if (!busy[i]) continue;
            if (GL15.glGetQueryObjecti(qEnd[i], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) continue;
            final long t0 = GL33.glGetQueryObjecti64(qStart[i], GL15.GL_QUERY_RESULT);
            final long t1 = GL33.glGetQueryObjecti64(qEnd[i], GL15.GL_QUERY_RESULT);
            busy[i] = false;
            if (qWindow[i] == window && nGpu < CAP) gpu[nGpu++] = (t1 - t0) / 1e6;
        }
    }

    private boolean timestampsSupported() {
        if (timestamps == null) {
            boolean ok;
            try {
                ok = GLContext.getCapabilities().OpenGL33;
            } catch (RuntimeException | LinkageError bad) {
                ok = false;
            }
            timestamps = ok;
        }
        return timestamps;
    }

    /** The window's numbers. */
    static final class Report {

        int frames;
        double fps;
        /** mean, p50, p95, p99, max. */
        final double[] interval = new double[5], cpu = new double[5], gpu = new double[5];
        double allocKbPerFrame;

        String line() {
            return String.format(
                Locale.ROOT,
                "%d frames, %.0f fps; frame ms mean %.3f p50 %.3f p95 %.3f p99 %.3f max %.2f; cpu ms mean %.3f p95 %.3f"
                    + "; gpu ms mean %.3f p50 %.3f p95 %.3f p99 %.3f; alloc %.1f KB/frame",
                frames,
                fps,
                interval[0],
                interval[1],
                interval[2],
                interval[3],
                interval[4],
                cpu[0],
                cpu[2],
                gpu[0],
                gpu[1],
                gpu[2],
                gpu[3],
                allocKbPerFrame);
        }
    }

    Report report() {
        if (timestampsSupported()) poll();
        final Report r = new Report();
        r.frames = nInterval;
        stats(interval, nInterval, r.interval);
        stats(cpu, nCpu, r.cpu);
        stats(gpu, nGpu, r.gpu);
        r.fps = r.interval[0] > 0 ? 1000.0 / r.interval[0] : 0;
        long sum = 0;
        for (int i = 0; i < nCpu; i++) sum += alloc[i];
        r.allocKbPerFrame = nCpu == 0 ? 0 : sum / 1024.0 / nCpu;
        return r;
    }

    private static void stats(double[] v, int n, double[] out) {
        if (n == 0) {
            Arrays.fill(out, 0);
            return;
        }
        final double[] s = Arrays.copyOf(v, n);
        Arrays.sort(s);
        double sum = 0;
        for (double x : s) sum += x;
        out[0] = sum / n;
        out[1] = s[(int) Math.min(n - 1, Math.floor(0.50 * n))];
        out[2] = s[(int) Math.min(n - 1, Math.floor(0.95 * n))];
        out[3] = s[(int) Math.min(n - 1, Math.floor(0.99 * n))];
        out[4] = s[n - 1];
    }
}
