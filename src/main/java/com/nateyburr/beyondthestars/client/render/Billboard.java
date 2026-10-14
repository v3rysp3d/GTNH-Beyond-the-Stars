package com.nateyburr.beyondthestars.client.render;

import org.joml.Matrix4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2: the axes of a camera-facing billboard through a camera-relative point (the camera at the origin, as a
 * TESR sees it). {@link #u} and {@link #v} are unit vectors perpendicular to the view direction {@link #w} (from the
 * camera to the point), {@code u} horizontal unless the view is near vertical. Render thread only: {@link #at}
 * reuses one instance.
 */
@SideOnly(Side.CLIENT)
public final class Billboard {

    private static final Billboard SHARED = new Billboard();

    public final double[] u = new double[3];
    public final double[] v = new double[3];
    public final double[] w = new double[3];
    /** The point's distance from the camera. */
    public double distance;

    private Billboard() {}

    /**
     * The billboard axes at {@code (cx, cy, cz)}; null when the camera is within {@code 1e-6} of the point. The
     * returned instance is shared: use it before the next call.
     */
    public static Billboard at(double cx, double cy, double cz) {
        final double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (dist < 1e-6) return null;
        final Billboard b = SHARED;
        b.distance = dist;
        final double dx = cx / dist, dy = cy / dist, dz = cz / dist;
        final double upX = Math.abs(dy) > 0.99 ? 1 : 0, upY = Math.abs(dy) > 0.99 ? 0 : 1;
        double ux = -dz * upY, uy = dz * upX, uz = dx * upY - dy * upX;
        final double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        b.u[0] = ux;
        b.u[1] = uy;
        b.u[2] = uz;
        b.v[0] = uy * dz - uz * dy;
        b.v[1] = uz * dx - ux * dz;
        b.v[2] = ux * dy - uy * dx;
        b.w[0] = dx;
        b.w[1] = dy;
        b.w[2] = dz;
        return b;
    }

    /**
     * {@code out} = translate(c) x [u v w] x scale(radius): a unit quad in its XY plane becomes this billboard of
     * {@code radius} blocks round {@code (cx, cy, cz)}.
     */
    public Matrix4f model(Matrix4f out, double cx, double cy, double cz, double radius) {
        final float s = (float) radius;
        return out.set(
            (float) u[0] * s,
            (float) u[1] * s,
            (float) u[2] * s,
            0f,
            (float) v[0] * s,
            (float) v[1] * s,
            (float) v[2] * s,
            0f,
            (float) w[0] * s,
            (float) w[1] * s,
            (float) w[2] * s,
            0f,
            (float) cx,
            (float) cy,
            (float) cz,
            1f);
    }
}
