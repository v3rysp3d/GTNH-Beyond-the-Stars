package com.nateyburr.beyondthestars.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (WP-6..WP-8): draws queued by GT's {@code IMTERenderer} TESRs and run after the world's translucent pass.
 * <p>
 * GT calls an MTE's {@code renderTESR} in render pass 0 only, before the translucent blocks. The machines without a
 * render tile (the Homogeny, the Reality machines, the conduit) glow behind TecTech's Quantum Glass, a pass-1 blended
 * block that would then be drawn over them and swallow most of the light. So each of those TESRs only queues its draw
 * here (the camera-relative position it was given is still valid in the same frame), and {@link RenderWorldLastEvent}
 * runs the queue after the translucent pass, as the Eye's see-through layers draw in pass 1 (render QA round 4, D1).
 * The modelview then holds the same camera rotation a TESR sees. The queue is cleared every frame, so a draw never
 * outlives the frame it was queued in.
 * <p>
 * Under Angelica (R-16) the translucent pass writes the glass into the depth buffer, which hid everything behind
 * glass: {@link OpaqueDepth} puts the opaque world's depth back for these draws (or, where it cannot, they draw at
 * once in pass 0).
 */
@SideOnly(Side.CLIENT)
public final class DeferredDraws {

    private static final DeferredDraws INSTANCE = new DeferredDraws();
    /** The queue: entries reused frame after frame (R-16: queueing a draw allocates nothing). */
    private static final List<Entry<?>> QUEUE = new ArrayList<>();
    private static int queued;
    private static boolean registered;
    /**
     * Dev switch {@code -Dbts.render.machineImmediate=true}: draw at GT's pass-0 hook, never after the translucent
     * pass.
     */
    static final boolean IMMEDIATE = Boolean.getBoolean("bts.render.machineImmediate");
    private static long queuedFrame = -1;

    /**
     * A deferred draw: a machine and the camera-relative position its TESR was given. Keep the instance in a static
     * field (a method reference to a static method), so queueing captures nothing.
     */
    public interface Draw<T> {

        void draw(T machine, double x, double y, double z, float partialTicks);
    }

    private static final class Entry<T> {

        Draw<T> draw;
        T machine;
        double x, y, z;
        float partialTicks;

        void run() {
            draw.draw(machine, x, y, z, partialTicks);
        }
    }

    private DeferredDraws() {}

    /** Called by the client proxy. */
    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    /**
     * Queues {@code draw} of {@code machine} for after the translucent pass of this frame (render thread), if it can
     * draw a shader tier with {@code programs} ({@link RenderQuality#mayShade}). The caller queues only when the
     * machine has something to show. Both are decided here, in pass 0, because the first queued draw pays for the
     * opaque-depth capture under Angelica: at {@code renderQuality=low}, without shaders or with nothing to draw the
     * frame costs what it did before v2 (review of R-16).
     */
    @SuppressWarnings("unchecked")
    public static <T> void add(Draw<T> draw, BTSShaders.Program[] programs, T machine, double x, double y, double z,
        float partialTicks) {
        if (!RenderQuality.mayShade(machine, programs)) return;
        if (IMMEDIATE || OpaqueDepth.drawImmediately()) {
            // R-16: under Angelica without the depth blits (a shaderpack, no GL 3.0): now, behind the glass
            draw.draw(machine, x, y, z, partialTicks);
            return;
        }
        // R-16: under Angelica, the opaque world's depth for the deferred draws (no-op elsewhere)
        OpaqueDepth.captureOpaque();
        final long f = RenderFrame.frame();
        if (f != queuedFrame) {
            clear();
            queuedFrame = f;
        }
        if (queued == QUEUE.size()) QUEUE.add(new Entry<>());
        final Entry<T> e = (Entry<T>) QUEUE.get(queued++);
        e.draw = draw;
        e.machine = machine;
        e.x = x;
        e.y = y;
        e.z = z;
        e.partialTicks = partialTicks;
    }

    /** Empties the queue (the entries stay for reuse; they let go of their machines). */
    private static void clear() {
        for (int i = 0; i < queued; i++) {
            QUEUE.get(i).machine = null;
            QUEUE.get(i).draw = null;
        }
        queued = 0;
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (queuedFrame != RenderFrame.frame() || queued == 0) {
            clear();
            return;
        }
        final boolean opaque = OpaqueDepth.useOpaque();
        try {
            for (int i = 0; i < queued; i++) {
                try {
                    QUEUE.get(i)
                        .run();
                } catch (RuntimeException bad) {
                    BeyondTheStars.LOG.warn("[BTS render] a deferred machine draw failed: {}", bad.toString());
                }
            }
        } finally {
            if (opaque) OpaqueDepth.restore();
        }
        clear();
    }
}
