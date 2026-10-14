package com.nateyburr.beyondthestars.rendertest;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.QuasarKit;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.reality.HRKit;

/**
 * DEV-ONLY render QA harness (renders v2, R-16): the performance scene. Four of the big renders stand close together
 * on one site so that one camera sees them all at once: a Kerr Ergosphere Engine T2 with a 512 SM hole, the Homogeny
 * at its top tier (mixing), an Eye at T8 showing the Galactic Merger (the heaviest galaxy look: both galaxies, the
 * tails) and a quasar at 4,096 SM whose eruption is frozen at its busiest moment (client side, through the quasar's
 * look override). The Kerr and the Homogeny form the north row, the Eye and the quasar the south row; each structure's
 * box is taken from its kit's bounds so none overlaps another.
 * <p>
 * Views (the case lists them): {@code tilt} (the oblique view that frames all four renders largest, found by a search
 * over directions and distances that keeps every render centre inside the frame and within 235 blocks),
 * {@code over} (straight down over the middle), {@code qring} (inside the quasar's shell over its ring, the hole and
 * the far ring large, the rest of the scene round it), {@code klevel} (the Kerr through its window band with the Eye
 * behind), {@code ebowl} (over the Eye's bowl), and the LOD walks {@code lodk:<d>}, {@code lode:<d>}, {@code lodq:<d>}
 * ({@code d} blocks from the Kerr, the Eye or the quasar along a fixed line).
 */
final class RenderScene {

    private RenderScene() {}

    /** Gap between two structures' boxes, blocks. */
    private static final int GAP = 6;
    /** Every render centre must stay this close to the camera in the all-in views (Kerr and Eye draw to 256). */
    private static final double MAX_DIST = 235;

    /** Kerr hole mass, quasar mass. */
    static final double KERR_MASS = 512, QUASAR_MASS = 4096;

    /** The scene's quasar look: Stable, jets 600 Bu/t, an eruption (size 1) frozen 25 ticks in: the busiest frame. */
    static final float[] QUASAR_LOOK = { 1, 0.85f, 10f, 600f, 600f, 25f, 1f, 1, 0.6f, 2f, 0, 0f };
    /** The scene's Homogeny look: mixing, live tier (3). */
    static final float[] HOMOGENY_LOOK = { 1f, 0f, -1f, -2f };

    /** The Eye's look: the Milky Way in the Galactic Merger (both galaxies and the tidal tails). */
    static EyeStarLook eyeLook() {
        return EyeStarLook.galaxy(8, 0.3f, 0.5f);
    }

    /** One structure of the scene. */
    static final class Part {

        final String name;
        final RenderTest.Built b = new RenderTest.Built();
        /** Where its render is (the hole, the galaxy, the vortex). */
        double[] centre;
        /** About how far its render reaches, blocks (the all-in view's size score). */
        final double radius;

        Part(String name, double radius) {
            this.name = name;
            this.radius = radius;
        }
    }

    static final Part KERR = new Part("kerr", 6), HOMOGENY = new Part("homogeny", 12), EYE = new Part("eye", 20),
        QUASAR = new Part("quasar", 62);
    static final Part[] PARTS = { KERR, HOMOGENY, EYE, QUASAR };

    /** The search's all-in view: {eye x, y, z}, or null before the build. */
    private static double[] tiltEye;

    // ================================================================================ build

    static void build(WorldServer world, RenderTest.Built b) {
        final int x0 = b.site.originX();
        final int z0 = RenderTest.SITE_Z;
        final ExtendedFacing ef = ExtendedFacing.of(RenderTest.FACING);
        final String[] eyePieces = Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, 8);
        final HRKit.Machine hm = HRKit.Machine.HOMOGENY;
        final String[] hPieces = Arrays.copyOf(hm.pieces, hm.maxTier());
        // each structure's box relative to its controller
        final int[] kRel = KerrKit.assemblyBounds(RenderTest.FACING, 0, 100, 0, true);
        final int[] hRel = SLConsoleTest.bounds(ef, 0, 100, 0, hPieces);
        final int[] eRel = SLConsoleTest.bounds(ef, 0, 100, 0, eyePieces);
        final int[] qRel = QuasarKit.bounds(RenderTest.FACING, 0, 100, 0);
        // north row: the Kerr, the Homogeny east of it; south row: the Eye south of the Kerr, the quasar east of it
        final int kx = x0, kz = z0;
        final int hx = kx + kRel[3] + GAP - hRel[0], hz = z0;
        final int ex = x0;
        final int ez = Math.max(kz + kRel[5], overlapsX(ex, eRel, hx, hRel) ? hz + hRel[5] : Integer.MIN_VALUE) + GAP
            - eRel[2];
        final int qx = Math.max(ex + eRel[3], kx + kRel[3]) + GAP - qRel[0];
        final int qz = Math.max(hz + hRel[5], overlapsX(qx, qRel, kx, kRel) ? kz + kRel[5] : Integer.MIN_VALUE) + GAP
            - qRel[2];
        b.lines.add(
            String.format(
                Locale.ROOT,
                "scene controllers: Kerr (%d, %d), Homogeny (%d, %d), Eye (%d, %d), quasar (%d, %d)",
                kx,
                kz,
                hx,
                hz,
                ex,
                ez,
                qx,
                qz));

        RenderTest.buildKerr(world, KERR.b, true, kx, kz);
        RenderTest.buildHR(world, HOMOGENY.b, hm, hx, hz);
        RenderTest.buildEye(world, EYE.b, 8, ex, ez);
        RenderTest.buildQuasar(world, QUASAR.b, qx, qz);
        int[] box = null;
        for (Part p : PARTS) {
            for (String line : p.b.lines) b.lines.add(p.name + ": " + line);
            if (p.b.error != null) {
                b.error = p.name + ": " + p.b.error;
                return;
            }
            p.centre = p.b.focus != null ? p.b.focus : p.b.centre;
            box = box == null ? p.b.box.clone() : union(box, p.b.box);
        }
        b.box = box;
        b.controller = KERR.b.controller;
        b.centre = centroid();
        tiltEye = searchTilt(world);
        for (Part p : PARTS) {
            b.lines.add(
                String.format(
                    Locale.ROOT,
                    "%s render centre (%.1f, %.1f, %.1f)%s",
                    p.name,
                    p.centre[0],
                    p.centre[1],
                    p.centre[2],
                    tiltEye == null ? ""
                        : String.format(Locale.ROOT, ", %.0f blocks from the tilt view", dist(tiltEye, p.centre))));
        }
        if (tiltEye == null) b.lines.add("WARNING: no all-in view found within " + MAX_DIST + " blocks");
    }

    private static boolean overlapsX(int ax, int[] aRel, int bx, int[] bRel) {
        return ax + aRel[0] - GAP <= bx + bRel[3] && bx + bRel[0] - GAP <= ax + aRel[3];
    }

    private static int[] union(int[] a, int[] b) {
        return new int[] { Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[3], b[3]),
            Math.max(a[4], b[4]), Math.max(a[5], b[5]) };
    }

    static double[] centroid() {
        final double[] c = new double[3];
        for (Part p : PARTS) for (int i = 0; i < 3; i++) c[i] += p.centre[i] / PARTS.length;
        return c;
    }

    // ================================================================================ state

    /** Sets every part's state (server thread). */
    static String apply(WorldServer world) {
        final StringBuilder out = new StringBuilder();
        final int[] k = KERR.b.controller;
        final MTEKerrEngine kee = KerrTestBench.keeAt(world, k[0], k[1], k[2]);
        if (kee != null) {
            kee.debugOpenHole(KERR_MASS);
            out.append("Kerr hole ")
                .append(kee.isHoleOpen() ? "open" : "CLOSED")
                .append(" at ")
                .append(MTEKerrEngine.fmt(kee.mass()))
                .append(" SM");
        } else out.append("no KEE");
        final int[] q = QUASAR.b.controller;
        final MTEQuasarEngine qe = QuasarKit.quasarAt(world, q[0], q[1], q[2]);
        if (qe != null) {
            qe.debugSeed(QUASAR_MASS, QuasarParams.aEq(1, 3));
            qe.benchRun(30, true);
            out.append(String.format(Locale.ROOT, "; quasar %.0f SM", qe.getMass()));
        } else out.append("; no quasar");
        final int[] e = EYE.b.controller;
        final MTEEyeOfHarmonyBTS eye = RenderTest.eyeAt(world, e[0], e[1], e[2]);
        if (eye != null) {
            eye.debugForceLook(eyeLook());
            out.append("; Eye look ")
                .append(eyeLook().label());
        } else out.append("; no Eye");
        out.append("; Homogeny mixing (client look)");
        return out.toString();
    }

    /** Once a second: hold the hole's mass and the quasar's gas and mass. */
    static void maintain(WorldServer world) {
        final int[] k = KERR.b.controller;
        final MTEKerrEngine kee = KerrTestBench.keeAt(world, k[0], k[1], k[2]);
        if (kee != null && Math.abs(kee.mass() - KERR_MASS) > 0.5) kee.debugOpenHole(KERR_MASS);
        final int[] q = QUASAR.b.controller;
        final MTEQuasarEngine qe = QuasarKit.quasarAt(world, q[0], q[1], q[2]);
        if (qe != null) {
            qe.debugFillBuffer();
            if (Math.abs(qe.getMass() - QUASAR_MASS) > 1) qe.debugMass(QUASAR_MASS);
        }
    }

    // ================================================================================ views

    /** {eye, target} of a scene view, or null if {@code view} is not one. */
    static double[][] view(World world, String view) {
        final double[] up = { 0, 1, 0 };
        final ForgeDirection f = RenderTest.FACING;
        final double[] F = { f.offsetX, 0, f.offsetZ };
        final int colon = view.indexOf(':');
        if (colon > 0) {
            final String kind = view.substring(0, colon);
            final double d = Double.parseDouble(view.substring(colon + 1));
            final Part p = kind.equals("lodk") ? KERR : kind.equals("lode") ? EYE : kind.equals("lodq") ? QUASAR : null;
            if (p == null) return null;
            // the Kerr through its window band; the Eye from above its bowl; the quasar from the north, rising
            final double[] dir = p == KERR ? F : p == EYE ? norm(add(F, up, 1.7)) : norm(add(F, up, 0.45));
            return new double[][] { add(p.centre, dir, d), p.centre };
        }
        switch (view) {
            case "tilt":
            case "hud":
            case "inv":
                if (tiltEye == null) return null;
                return new double[][] { tiltEye.clone(), centroid() };
            case "over": {
                final double[] c = centroid();
                double h = 0;
                // high enough that every centre is in the frame, never past MAX_DIST from any
                for (Part p : PARTS) {
                    final double horiz = Math.hypot(p.centre[0] - c[0], p.centre[2] - c[2]);
                    h = Math.max(h, p.centre[1] + horiz / Math.tan(Math.toRadians(30)));
                }
                h = Math.min(h, 250);
                return new double[][] { new double[] { c[0] + 0.01, h, c[2] + 0.01 },
                    new double[] { c[0], c[1] - 50, c[2] } };
            }
            case "qring": {
                final double[] qc = QUASAR.centre;
                return new double[][] { add(add(qc, F, 52), up, 46), add(qc, F, -12) };
            }
            case "klevel": {
                final double[] kc = KERR.centre;
                final int[] box = KERR.b.box;
                final double ext = Math.max((box[2] - kc[2]) * F[2], (box[5] + 1 - kc[2]) * F[2]);
                return new double[][] { add(kc, F, ext + 24), kc };
            }
            case "ebowl": {
                final double[] ec = EYE.centre;
                return new double[][] { add(ec, norm(add(F, up, 1.7)), 72), ec };
            }
            default:
                return null;
        }
    }

    /**
     * The all-in view: over directions round the scene's middle (azimuth every 10 degrees, elevation 25-80) and
     * distances, the camera whose frame holds every render centre (with a margin) within {@link #MAX_DIST} of each and
     * shows them largest (the sum of their projected sizes); null if none.
     */
    private static double[] searchTilt(World world) {
        final double[] c = centroid();
        final double tanH = Math.tan(Math.toRadians(35)) * 16.0 / 9.0 * 0.85, tanV = Math.tan(Math.toRadians(35)) * 0.8;
        double best = -1;
        double[] bestEye = null;
        for (int az = 0; az < 360; az += 10) {
            for (int el = 25; el <= 80; el += 5) {
                final double ca = Math.cos(Math.toRadians(az)), sa = Math.sin(Math.toRadians(az));
                final double ce = Math.cos(Math.toRadians(el)), se = Math.sin(Math.toRadians(el));
                final double[] dir = { ca * ce, se, sa * ce };
                for (double d = 80; d <= MAX_DIST; d += 10) {
                    final double[] eye = add(c, dir, d);
                    if (eye[1] > 250) break;
                    final double score = score(eye, c, tanH, tanV);
                    if (score > best && RenderTest.clearAt(world, eye)) {
                        best = score;
                        bestEye = eye;
                    }
                }
            }
        }
        return bestEye;
    }

    private static double score(double[] eye, double[] target, double tanH, double tanV) {
        final double[] fw = norm(sub(target, eye));
        final double[] right = norm(new double[] { -fw[2], 0, fw[0] });
        final double[] up = { right[1] * fw[2] - right[2] * fw[1], right[2] * fw[0] - right[0] * fw[2],
            right[0] * fw[1] - right[1] * fw[0] };
        double s = 0;
        for (Part p : PARTS) {
            final double[] v = sub(p.centre, eye);
            final double z = dot(v, fw);
            final double len = Math.sqrt(dot(v, v));
            if (z <= 1 || len > MAX_DIST) return -1;
            if (Math.abs(dot(v, right)) / z > tanH || Math.abs(dot(v, up)) / z > tanV) return -1;
            s += p.radius / len;
        }
        return s;
    }

    static double dist(double[] a, double[] b) {
        final double[] d = sub(a, b);
        return Math.sqrt(dot(d, d));
    }

    private static double[] add(double[] a, double[] d, double s) {
        return new double[] { a[0] + d[0] * s, a[1] + d[1] * s, a[2] + d[2] * s };
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] norm(double[] a) {
        final double l = Math.sqrt(dot(a, a));
        return new double[] { a[0] / l, a[1] / l, a[2] / l };
    }
}
