package com.nateyburr.beyondthestars.client.render;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2: the frame clock of the render kit. Counts frames (the LOD's once-per-frame projection read, the HIGH
 * budget ranking) and gives {@link GpuTimer} its frame boundaries. Registered once by the client proxy.
 */
@SideOnly(Side.CLIENT)
public final class RenderFrame {

    private static final RenderFrame INSTANCE = new RenderFrame();
    private static boolean registered;
    private static long frame;

    private RenderFrame() {}

    /** Called by the client proxy. */
    public static void register() {
        if (registered) return;
        registered = true;
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    /** The current frame number (render thread). */
    public static long frame() {
        return frame;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            frame++;
            NoiseTex.onFrameStart();
            RenderQuality.onFrameStart();
        } else {
            GpuTimer.onFrameEnd();
        }
    }
}
