package com.nateyburr.beyondthestars.client.render;

import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.2): the shader clock. {@code u_Time} is seconds from {@code (totalWorldTime % 72,000 + partialTicks)
 * / 20}: it wraps every hour of world time, so floats stay precise; every animated pattern either loops within the
 * hour or hides the wrap.
 */
@SideOnly(Side.CLIENT)
public final class RenderClock {

    /** One hour of world time, ticks. */
    public static final long PERIOD_TICKS = 72_000L;

    private RenderClock() {}

    /** Seconds 0..3600 for the shaders. */
    public static float seconds(World world, float partialTicks) {
        final long t = world == null ? 0 : world.getTotalWorldTime();
        return ((t % PERIOD_TICKS) + partialTicks) / 20f;
    }
}
