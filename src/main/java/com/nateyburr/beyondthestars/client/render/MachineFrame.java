package com.nateyburr.beyondthestars.client.render;

import org.joml.Matrix4f;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§11, §12, WP-7 / WP-8): a machine's voxel frame, the coordinates its mockup ({@code designs/voxel.js})
 * was drawn in, placed in the world from the controller. The structure manifest gives, per machine, the controller's
 * voxel cell, the quarter turns between the voxel axes and the structure's a / b / c axes, and the render centre as an
 * a / b / c offset from the controller ({@code machines.<key>.centre.rel}). A voxel offset {@code d} turns
 * {@code turns} times {@code (x, z) -> (-z, x)}, then {@code a = x, b = -y, c = -z}; GT's {@link ExtendedFacing} takes
 * a / b / c into the world.
 * <p>
 * Render code then works in voxel coordinates: {@link #model} is {@code translate(centre) x [ex ey ez]}, and
 * {@link #camLocal} the camera in the same frame, so every shape is the mockup's own (the Homogeny's column radius 13,
 * the Diffraction's drums at y -28 .. 28, ...). Client and render thread only; one instance per machine tile.
 */
@SideOnly(Side.CLIENT)
public final class MachineFrame {

    /** The voxel origin's block centre, as an offset from the controller block's minimum corner. */
    public final double[] origin = new double[3];
    /** World directions of the voxel +x, +y (up) and +z axes. */
    public final double[] ex = new double[3], ey = new double[3], ez = new double[3];

    private final String key;
    private ExtendedFacing facing;
    private boolean valid;

    public MachineFrame(String machineKey) {
        this.key = machineKey;
    }

    /** Recomputes the frame when the controller's facing changed; false when the manifest has no such machine. */
    public boolean update(ExtendedFacing f) {
        if (f == null) return false;
        if (f == facing) return valid;
        facing = f;
        valid = false;
        try {
            final JsonObject m = BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject(key);
            final int turns = m.has("turns") ? m.get("turns")
                .getAsInt() : 0;
            final JsonArray rel = m.getAsJsonObject("centre")
                .getAsJsonArray("rel");
            final int[] c = offset(
                f,
                new int[] { rel.get(0)
                    .getAsInt(),
                    rel.get(1)
                        .getAsInt(),
                    rel.get(2)
                        .getAsInt() });
            for (int i = 0; i < 3; i++) origin[i] = c[i] + 0.5;
            axis(f, turns, 1, 0, 0, ex);
            axis(f, turns, 0, 1, 0, ey);
            axis(f, turns, 0, 0, 1, ez);
            valid = true;
        } catch (RuntimeException missing) {
            valid = false;
        }
        return valid;
    }

    private static int[] offset(ExtendedFacing f, int[] abc) {
        final int[] xyz = new int[3];
        f.getWorldOffset(abc, xyz);
        return xyz;
    }

    /** The world direction of the voxel unit vector (x, y, z). */
    private static void axis(ExtendedFacing f, int turns, int x, int y, int z, double[] out) {
        int vx = x, vz = z;
        for (int i = 0; i < ((turns % 4) + 4) % 4; i++) {
            final int nx = -vz;
            vz = vx;
            vx = nx;
        }
        final int[] w = offset(f, new int[] { vx, -y, -vz });
        for (int i = 0; i < 3; i++) out[i] = w[i];
    }

    /**
     * {@code out} = the camera-relative world position of voxel point (vx, vy, vz), for a controller whose block
     * minimum corner is at camera-relative (x, y, z).
     */
    public double[] at(double x, double y, double z, double vx, double vy, double vz, double[] out) {
        for (int i = 0; i < 3; i++) {
            final double base = i == 0 ? x : i == 1 ? y : z;
            out[i] = base + origin[i] + ex[i] * vx + ey[i] * vy + ez[i] * vz;
        }
        return out;
    }

    /** {@code translate(c) x [ex ey ez] x scale(s)}: voxel coordinates (times s) to camera-relative world. */
    public Matrix4f model(Matrix4f out, double[] c, double s) {
        final float f = (float) s;
        return out.set(
            (float) ex[0] * f,
            (float) ex[1] * f,
            (float) ex[2] * f,
            0f,
            (float) ey[0] * f,
            (float) ey[1] * f,
            (float) ey[2] * f,
            0f,
            (float) ez[0] * f,
            (float) ez[1] * f,
            (float) ez[2] * f,
            0f,
            (float) c[0],
            (float) c[1],
            (float) c[2],
            1f);
    }

    /** The camera (the origin of camera-relative space) in voxel coordinates, for a voxel origin at {@code c}. */
    public double[] camLocal(double[] c, double[] out) {
        out[0] = -(c[0] * ex[0] + c[1] * ex[1] + c[2] * ex[2]);
        out[1] = -(c[0] * ey[0] + c[1] * ey[1] + c[2] * ey[2]);
        out[2] = -(c[0] * ez[0] + c[1] * ez[1] + c[2] * ez[2]);
        return out;
    }
}
