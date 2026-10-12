package com.nateyburr.beyondthestars.machines.quasar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;

/**
 * P4: the Quasar Engine's fixed cells from {@code manifest.json machines.quasar} (P4-02), as offsets from the
 * controller in its (a, b, c) frame, so nothing here can disagree with the exported shape: the hole centre, each jet
 * port's cell and axis, and where a docked crown's controller stands. {@link #world} turns an offset into a world
 * cell for a controller's facing. Pure resource reads (trap 1: the same on both sides).
 */
public final class QuasarGeometry {

    private QuasarGeometry() {}

    private static volatile int[] centre;
    private static volatile int[][] ports, axes, crowns, crownFacings;

    private static void load() {
        if (crowns != null) return;
        synchronized (QuasarGeometry.class) {
            if (crowns != null) return;
            final JsonObject q = BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject("quasar");
            if (q == null) throw new IllegalStateException("[BTS] manifest.json has no machines.quasar: re-export");
            final int[] c = vec(q.getAsJsonObject("centre"), "rel");
            final JsonObject slots = q.getAsJsonObject("slots");
            final int[][] p = new int[JetPort.VALUES.length][];
            final int[][] a = new int[JetPort.VALUES.length][];
            final int[][] k = new int[JetPort.VALUES.length][];
            final int[][] cf = new int[JetPort.VALUES.length][];
            for (JetPort port : JetPort.VALUES) {
                final JsonObject s = slots.getAsJsonObject(port.key);
                if (s == null) throw new IllegalStateException("[BTS] manifest.json: no quasar slot " + port.key);
                p[port.ordinal()] = vec(s, "portRel");
                a[port.ordinal()] = vec(s, "axisAbc");
                k[port.ordinal()] = vec(s, "crownControllerRel");
                cf[port.ordinal()] = vec(s, "crownFacingAbc");
            }
            centre = c;
            ports = p;
            axes = a;
            crownFacings = cf;
            crowns = k;
        }
    }

    /** The hole centre (render cell), from the controller. */
    public static int[] centreRel() {
        load();
        return centre.clone();
    }

    /** The jet port's cell (where a dock's intake must be), from the controller. */
    public static int[] portRel(JetPort port) {
        load();
        return ports[port.ordinal()].clone();
    }

    /** The jet's direction out of the port, (a, b, c). */
    public static int[] axisAbc(JetPort port) {
        load();
        return axes[port.ordinal()].clone();
    }

    /** Where a docked crown's controller stands, from the quasar controller. */
    public static int[] crownControllerRel(JetPort port) {
        load();
        return crowns[port.ordinal()].clone();
    }

    /** The way a docked crown's controller faces, (a, b, c) in the quasar's frame. */
    public static int[] crownFacingAbc(JetPort port) {
        load();
        return crownFacings[port.ordinal()].clone();
    }

    /** {@code (x, y, z) + facing.worldOffset(rel)}. */
    public static int[] world(ExtendedFacing facing, int x, int y, int z, int[] rel) {
        final int[] xyz = new int[3];
        facing.getWorldOffset(rel, xyz);
        return new int[] { x + xyz[0], y + xyz[1], z + xyz[2] };
    }

    private static int[] vec(JsonObject o, String name) {
        final JsonArray a = o == null ? null : o.getAsJsonArray(name);
        if (a == null || a.size() != 3) throw new IllegalStateException("[BTS] quasar manifest: bad '" + name + "'");
        return new int[] { a.get(0)
            .getAsInt(),
            a.get(1)
                .getAsInt(),
            a.get(2)
                .getAsInt() };
    }
}
