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
 */
@SideOnly(Side.CLIENT)
public final class DeferredDraws {

    private static final DeferredDraws INSTANCE = new DeferredDraws();
    private static final List<Runnable> QUEUE = new ArrayList<>();
    private static boolean registered;
    private static long queuedFrame = -1;

    private DeferredDraws() {}

    /** Called by the client proxy. */
    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    /** Queues {@code draw} for after the translucent pass of this frame (render thread). */
    public static void add(Runnable draw) {
        final long f = RenderFrame.frame();
        if (f != queuedFrame) {
            QUEUE.clear();
            queuedFrame = f;
        }
        QUEUE.add(draw);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (queuedFrame != RenderFrame.frame() || QUEUE.isEmpty()) {
            QUEUE.clear();
            return;
        }
        for (int i = 0; i < QUEUE.size(); i++) {
            try {
                QUEUE.get(i)
                    .run();
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS render] a deferred machine draw failed: {}", bad.toString());
            }
        }
        QUEUE.clear();
    }
}
