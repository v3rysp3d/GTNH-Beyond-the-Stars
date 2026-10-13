package com.nateyburr.beyondthestars.rendertest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY video recorder ({@link RecClient}): reads the finished frame with {@code glReadPixels} into one reused
 * direct buffer and pipes raw RGB to an ffmpeg child process over its stdin, one ffmpeg (one segment file) per scene.
 * <p>
 * <b>Real speed.</b> Frames go out on a fixed wall-clock schedule: a span starts at {@link #beginSpan} and slot
 * {@code n} is due at {@code start + n * period} (period {@code 1 / fps}, or {@code speed / fps} in a time-lapse). On
 * each rendered frame every slot that has come due is filled: the newest with this frame, older ones (the game rendered
 * slower than the schedule) with the last frame sent. A rendered frame with no slot due (the game is faster) is not
 * sent. So {@code frames / fps} equals the wall-clock length of the recorded spans.
 * <p>
 * The pixel copy is the only work on the render thread; a writer thread feeds ffmpeg. A small pool of frame buffers
 * gives back-pressure: if ffmpeg falls behind the render thread waits, and the schedule duplicates to catch up.
 */
@SideOnly(Side.CLIENT)
final class FrameRecorder {

    /** One frame's bytes; counted references (the last frame may sit in the queue several times). */
    private static final class Buf {

        final byte[] data;
        int refs;

        Buf(int size) {
            data = new byte[size];
        }
    }

    private static final Buf POISON = new Buf(0);

    final int width, height;
    final double fps;
    private final String ffmpeg;
    private final List<String> codec;
    private final ByteBuffer pixels;
    private final BlockingQueue<Buf> free = new ArrayBlockingQueue<>(8);
    private final BlockingQueue<Buf> queue = new ArrayBlockingQueue<>(24);

    private Process proc;
    private Thread writer;
    private volatile IOException writeError;
    private File file;
    private Buf last;
    private double period;
    private long spanStart = -1;
    private long slot;

    /** Counters for the current segment. */
    long frames, dups, skipped, captures;

    FrameRecorder(String ffmpeg, List<String> codec, int width, int height, double fps) {
        this.ffmpeg = ffmpeg;
        this.codec = codec;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.period = 1e9 / fps;
        pixels = BufferUtils.createByteBuffer(width * height * 3);
        for (int i = 0; i < 8; i++) free.add(new Buf(width * height * 3));
    }

    /** The codec arguments for {@code nvenc} or {@code x264} at a constant quality ({@code crf}). */
    static List<String> codecArgs(String encoder, int crf) {
        if ("nvenc".equals(encoder)) return Arrays
            .asList("-c:v", "h264_nvenc", "-preset", "p5", "-rc", "vbr", "-cq", String.valueOf(crf), "-b:v", "0");
        return Arrays.asList("-c:v", "libx264", "-crf", String.valueOf(crf), "-preset", "veryfast");
    }

    /** True if this ffmpeg can encode with h264_nvenc here (a 0.1 s test encode). */
    static boolean nvencWorks(String ffmpeg) {
        try {
            final Process p = new ProcessBuilder(
                ffmpeg,
                "-hide_banner",
                "-loglevel",
                "error",
                "-f",
                "lavfi",
                "-i",
                "color=black:s=256x144:d=0.1",
                "-c:v",
                "h264_nvenc",
                "-f",
                "null",
                "-").redirectErrorStream(true)
                    .redirectOutput(new File(System.getProperty("java.io.tmpdir"), "bts-rec-nvenc-test.log"))
                    .start();
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException | RuntimeException bad) {
            return false;
        }
    }

    boolean isOpen() {
        return proc != null;
    }

    File file() {
        return file;
    }

    /** Starts ffmpeg writing {@code out}; its log goes to {@code out}.log. */
    void open(File out) throws IOException {
        file = out;
        frames = dups = skipped = captures = 0;
        final List<String> cmd = new ArrayList<>(
            Arrays.asList(
                ffmpeg,
                "-hide_banner",
                "-loglevel",
                "warning",
                "-y",
                "-f",
                "rawvideo",
                "-pix_fmt",
                "rgb24",
                "-s",
                width + "x" + height,
                "-framerate",
                fmt(fps),
                "-i",
                "-",
                "-vf",
                "vflip"));
        cmd.addAll(codec);
        cmd.addAll(Arrays.asList("-pix_fmt", "yuv420p", "-r", fmt(fps), "-movflags", "+faststart", out.getPath()));
        final File log = new File(out.getPath() + ".log");
        proc = new ProcessBuilder(cmd).redirectErrorStream(true)
            .redirectOutput(log)
            .start();
        final OutputStream stdin = new BufferedOutputStream(proc.getOutputStream(), 1 << 20);
        writeError = null;
        writer = new Thread(() -> {
            try {
                while (true) {
                    final Buf b = queue.take();
                    if (b == POISON) break;
                    try {
                        if (writeError == null) stdin.write(b.data);
                    } catch (IOException bad) {
                        writeError = bad;
                    }
                    release(b);
                }
                stdin.close();
            } catch (InterruptedException | IOException bad) {
                if (bad instanceof IOException) writeError = (IOException) bad;
            }
        }, "BTS rec ffmpeg writer");
        writer.setDaemon(true);
        writer.start();
        spanStart = -1;
    }

    /** A recorded span starts now: slot 0 is due at once. {@code speed} > 1 is a time-lapse. */
    void beginSpan(long now, double speed) {
        period = 1e9 / fps * Math.max(1e-3, speed);
        spanStart = now;
        slot = 0;
    }

    /** Recording paused (between spans): nothing is due until the next {@link #beginSpan}. */
    void endSpan() {
        spanStart = -1;
    }

    /**
     * Called once per rendered frame, after everything is drawn: fills every slot due by {@code now} whose time is
     * before {@code until} (the span's end).
     */
    void frame(Minecraft mc, long now, long until) {
        if (proc == null || spanStart < 0) return;
        if (writeError != null) return;
        int due = 0;
        while (true) {
            final long t = spanStart + (long) (slot * period);
            if (t > now || t >= until) break;
            due++;
            slot++;
        }
        if (due == 0) {
            skipped++;
            return;
        }
        // older slots get the last frame sent (the game was slower than the schedule), the newest gets this one
        for (int i = 0; i < due - 1; i++) {
            if (last == null) break;
            send(last);
            dups++;
        }
        final boolean hadLast = last != null;
        final Buf b = capture(mc);
        if (b == null) return;
        if (!hadLast) for (int i = 0; i < due - 1; i++) {
            send(b);
            dups++;
        }
        send(b);
        captures++;
    }

    private Buf capture(Minecraft mc) {
        final Buf b;
        try {
            b = free.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException bad) {
            return null;
        }
        if (b == null) {
            BeyondTheStars.LOG.warn("[BTS rec] ffmpeg is not taking frames (5 s): frame dropped");
            return null;
        }
        if (net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled()) mc.getFramebuffer()
            .bindFramebuffer(false);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        pixels.clear();
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);
        pixels.rewind();
        pixels.get(b.data);
        synchronized (b) {
            b.refs = 1; // held as "last"
        }
        if (last != null) release(last);
        last = b;
        return b;
    }

    private void send(Buf b) {
        synchronized (b) {
            b.refs++;
        }
        try {
            queue.put(b);
            frames++;
        } catch (InterruptedException bad) {
            release(b);
        }
    }

    private void release(Buf b) {
        synchronized (b) {
            if (--b.refs > 0) return;
        }
        free.offer(b);
    }

    /** Finishes the segment: the queue drains, ffmpeg's stdin closes and ffmpeg is waited for. Returns its exit. */
    int close() {
        if (proc == null) return -1;
        spanStart = -1;
        try {
            queue.put(POISON);
            writer.join(60_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread()
                .interrupt();
        }
        if (last != null) {
            release(last);
            last = null;
        }
        int exit = -1;
        try {
            if (proc.waitFor(120, TimeUnit.SECONDS)) exit = proc.exitValue();
            else proc.destroy();
        } catch (InterruptedException ignored) {
            proc.destroy();
        }
        if (writeError != null) BeyondTheStars.LOG.warn("[BTS rec] writing to ffmpeg failed: " + writeError);
        proc = null;
        writer = null;
        return exit;
    }

    /** Kills a running ffmpeg (the client is closing without a clean finish). */
    void abort() {
        if (proc != null) proc.destroy();
        proc = null;
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.3f", v);
    }
}
