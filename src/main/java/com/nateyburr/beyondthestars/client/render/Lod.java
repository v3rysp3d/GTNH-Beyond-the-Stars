package com.nateyburr.beyondthestars.client.render;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§3.2): level of detail by projected size, not distance. An object's projected radius in pixels is
 * {@code R / d x (H / 2) / tan(fov / 2)}, with the field of view read from the current projection matrix (so FOV
 * changes, sprinting and zoom mods count) and H the display height.
 * <ul>
 * <li>L0 above 300 px: everything at full steps;</li>
 * <li>L1 80-300 px: half the march steps, half the noise octaves and points, no framebuffer lensing;</li>
 * <li>L2 20-80 px: no march, the analytic look;</li>
 * <li>L3 below 20 px: one glow sprite.</li>
 * </ul>
 * {@code renderLodScale} multiplies the thresholds; a tier only changes once the size has crossed its threshold by 10 %
 * (hysteresis), so an object at a boundary does not flicker.
 */
@SideOnly(Side.CLIENT)
public final class Lod {

    /** The detail tiers, finest first; {@link #FIXED} is the fixed-function (LOW) path. */
    public enum Tier {

        L0,
        L1,
        L2,
        L3,
        /** The fixed-function render of today: quality low, no shaders, a dead program or a GL error. */
        FIXED;

        /** True for the shader tiers. */
        public boolean shaded() {
            return this != FIXED;
        }

        /** One step coarser (L3 and FIXED stay). */
        public Tier coarser() {
            return this == L0 ? L1 : this == L1 ? L2 : this == L2 ? L3 : this;
        }
    }

    /** Pixel thresholds between L0/L1, L1/L2 and L2/L3 at {@code renderLodScale} 1. */
    public static final double[] THRESHOLDS = { 300, 80, 20 };
    private static final double HYSTERESIS = 0.10;

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static long projFrame = -1;
    private static double focal = 1.0;

    private Lod() {}

    /**
     * The projected radius in pixels of a sphere of {@code radius} blocks whose centre is {@code distance} blocks from
     * the camera; infinite when the camera is inside it. Reads the projection matrix once per frame.
     */
    public static double projectedRadius(double radius, double distance) {
        if (distance <= radius) return Double.POSITIVE_INFINITY;
        final long frame = RenderFrame.frame();
        if (projFrame != frame) {
            projFrame = frame;
            MATRIX.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MATRIX);
            // m11 = 1 / tan(fovY / 2) for a perspective projection; guard a degenerate or orthographic matrix
            final double m11 = MATRIX.get(5);
            focal = m11 > 0.05 && m11 < 100 ? m11 : 1.0 / Math.tan(Math.toRadians(35));
        }
        return radius / distance * (Minecraft.getMinecraft().displayHeight / 2.0) * focal;
    }

    /** Pixels per block at a distance of one block (for point sizes): {@code (H / 2) / tan(fov / 2)}. */
    public static double pixelsPerBlockAtUnit() {
        projectedRadius(0, 1);
        return Minecraft.getMinecraft().displayHeight / 2.0 * focal;
    }

    /** The tier for {@code px} given the tier the object had last frame (null or FIXED: none). */
    public static Tier tierFor(double px, Tier previous) {
        final double scale = Math.max(0.05, Config.renderLodScale);
        final int prev = previous != null && previous.shaded() ? previous.ordinal() : -1;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            // the boundary between tier i and i + 1 holds the previous side until it is crossed by 10 %
            double t = THRESHOLDS[i] * scale;
            if (prev >= 0) t *= prev <= i ? 1 - HYSTERESIS : 1 + HYSTERESIS;
            if (px > t) return Tier.values()[i];
        }
        return Tier.L3;
    }
}
