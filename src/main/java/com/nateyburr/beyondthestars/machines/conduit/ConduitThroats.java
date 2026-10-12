package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;

/**
 * P4: the conduit elements' throats from {@code manifest.json machines.<piece>.throats[]} (the exporter writes them
 * from the voxel mockup; nothing here is typed by hand): each throat's centre cell from the controller, its opening's
 * outward normal and its radius, in the controller's (a, b, c) frame. {@link #world} turns one into world coordinates
 * for a facing. Pure resource reads (trap 1: the same on both sides).
 */
public final class ConduitThroats {

    private ConduitThroats() {}

    /** One throat in the controller's frame. */
    public static final class Throat {

        public final int index;
        public final String role;
        public final double radius;
        public final int[] centreRel;
        public final double[] normalAbc;

        Throat(int index, String role, double radius, int[] centreRel, double[] normalAbc) {
            this.index = index;
            this.role = role;
            this.radius = radius;
            this.centreRel = centreRel;
            this.normalAbc = normalAbc;
        }
    }

    /** A throat in the world: centre (block centre), unit outward normal, radius. */
    public static final class WorldThroat {

        public final int index;
        public final double[] centre;
        public final double[] normal;
        public final double radius;
        public final int[] cell;

        public WorldThroat(int index, int[] cell, double[] normal, double radius) {
            this.index = index;
            this.cell = cell;
            this.centre = new double[] { cell[0] + 0.5, cell[1] + 0.5, cell[2] + 0.5 };
            this.normal = normal;
            this.radius = radius;
        }

        /** The same opening seen from the other side (normal reversed). */
        public WorldThroat reversed() {
            return new WorldThroat(index, cell, new double[] { -normal[0], -normal[1], -normal[2] }, radius);
        }
    }

    private static final Map<String, Throat[]> CACHE = new ConcurrentHashMap<>();

    /** The throats of {@code piece} (a {@code machines} key: conduit_mouth, conduit_mirror, conduit_junction). */
    public static Throat[] of(String piece) {
        return CACHE.computeIfAbsent(piece, ConduitThroats::load);
    }

    private static Throat[] load(String piece) {
        final JsonObject m = BTSShapes.manifest()
            .getAsJsonObject("machines")
            .getAsJsonObject(piece);
        if (m == null || !m.has("throats")) {
            throw new IllegalStateException("[BTS] manifest.json has no machines." + piece + ".throats: re-export");
        }
        final JsonArray list = m.getAsJsonArray("throats");
        final Throat[] out = new Throat[list.size()];
        for (int i = 0; i < out.length; i++) {
            final JsonObject t = list.get(i)
                .getAsJsonObject();
            final JsonArray c = t.getAsJsonArray("centreRel");
            final JsonArray n = t.getAsJsonArray("normalAbc");
            out[i] = new Throat(
                i,
                t.get("role")
                    .getAsString(),
                t.get("radius")
                    .getAsDouble(),
                new int[] { c.get(0)
                    .getAsInt(),
                    c.get(1)
                        .getAsInt(),
                    c.get(2)
                        .getAsInt() },
                new double[] { num(n.get(0)), num(n.get(1)), num(n.get(2)) });
        }
        return out;
    }

    private static double num(JsonElement e) {
        return e.getAsDouble();
    }

    /** {@code throat} for a controller at (x, y, z) facing {@code f}. */
    public static WorldThroat world(Throat throat, ExtendedFacing f, int x, int y, int z) {
        final int[] off = new int[3];
        f.getWorldOffset(throat.centreRel, off);
        final double[] n = new double[3];
        f.getWorldOffset(throat.normalAbc, n);
        final double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (len > 0) for (int i = 0; i < 3; i++) n[i] /= len;
        return new WorldThroat(throat.index, new int[] { x + off[0], y + off[1], z + off[2] }, n, throat.radius);
    }

    /** An (a, b, c) direction in the world for a facing (unit). */
    public static double[] worldDir(ExtendedFacing f, double[] abc) {
        final double[] n = new double[3];
        f.getWorldOffset(abc, n);
        final double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (len > 0) for (int i = 0; i < 3; i++) n[i] /= len;
        return n;
    }

    /** "+x", "-y", or "(0.50, 0.00, -0.87)" for a direction. */
    public static String dirName(double[] d) {
        final String[] axis = { "x", "y", "z" };
        for (int i = 0; i < 3; i++) {
            if (Math.abs(Math.abs(d[i]) - 1) < 1e-6) return (d[i] > 0 ? "+" : "-") + axis[i];
        }
        return String.format(java.util.Locale.ROOT, "(%.2f, %.2f, %.2f)", d[0], d[1], d[2]);
    }
}
