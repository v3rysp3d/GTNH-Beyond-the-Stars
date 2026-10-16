package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.QuasarDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarDisc;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarInputs;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;

/**
 * P4 test bench for the Eye's galaxy tiers ({@code /btseye star <x> <y> <z> burst | galaxy …}): the scripted link the
 * dev-server test runs a real Eye on.
 * <ul>
 * <li>{@code burst <Bu/t> [high share] [halo share] [quasar <mean Bu/t>] | burst off}: a constant, linked feed in
 * place of the receptor reading (rail split high / low by the share, the halo's share taken first). Every field
 * second until {@code off}. {@code quasar}: the linked quasar the Eye reads is scripted too, with that purging mean
 * stream (B-118's weak-quasar row).</li>
 * <li>{@code galaxy <seconds> [analog] [mass M]}: the §8.4 rig as {@code QuasarRigSim} runs it, but on this Eye: a
 * virtual quasar (the engine's {@link QuasarDisc}: helium, coils 3, b 3, f 1, M SM spun up to its equilibrium), both
 * jets into the standard conduit rig (two Mk I mouth pairs, a combine, J1 Eye : crown, J3 halo : rail, J2 high : low)
 * and the gate rig reading ONLY the Eye's Directive codes (window 5) and the disc's: Disc [1] / [2] the disc trims,
 * (Disc [4] AND NOT Galaxy Transition [8]) OR Galaxy Transition [4] the purge edge, J1 on Field [2] / [1] OR
 * Transition [4] (OR Slots [8] at T6), J2 on Field [4] / [8], J3 on Events [1] / [2] (all binary, F-8; {@code analog}:
 * the P / W / Λ sensor at ±30), Slots [8] the test fire, Galaxy Transition [1] the Begin edge. The Eye's own trims run
 * on the debug rig (FINE; at T7+ the binary Space valve on P &gt; +5 = Field [1]). Each second: the rig, the quasar,
 * the conduit, then the Eye's real field second.</li>
 * </ul>
 * The bench quasar is not a machine: its streams are fed to the Eye as its scripted bursts (linked while the virtual
 * hole is open with b &gt; 0). The receptor path itself is exercised with a real quasar by {@code /btsconduit rig}.
 */
public final class EyeGalaxyBench {

    private EyeGalaxyBench() {}

    /** {@code burst <Bu/t> [high] [halo]}: a constant linked feed; {@code burst off} ends it. */
    public static String burst(MTEEyeOfHarmonyBTS eye, String[] args, int at) {
        final EyeStarRuntime rt = eye.starRuntime();
        if (args.length <= at || "off".equalsIgnoreCase(args[at])) {
            rt.benchBursts = null;
            rt.benchFlux = null;
            return "Scripted bursts off: the Eye reads its GRB Receptor Ports again.";
        }
        // B-118: "quasar <mean Bu/t>" anywhere after the total scripts the linked quasar the Eye reads (its purging
        // mean stream; Φ 0, b 3), for the collapse's weak-quasar row and the T7 test-fire size check
        double quasarMean = -1;
        final List<String> rest = new ArrayList<>();
        for (int i = at; i < args.length; i++) {
            if ("quasar".equalsIgnoreCase(args[i]) && i + 1 < args.length) quasarMean = Double.parseDouble(args[++i]);
            else rest.add(args[i]);
        }
        final double total = Double.parseDouble(rest.get(0));
        final double hiShare = rest.size() > 1 ? Double.parseDouble(rest.get(1)) : 0.5;
        final double haloShare = rest.size() > 2 ? Double.parseDouble(rest.get(2)) : 0;
        rt.benchBursts = feed(
            eye,
            total * (1 - haloShare) * hiShare,
            total * (1 - haloShare) * (1 - hiShare),
            total * haloShare,
            total > 0);
        rt.benchFlux = quasarMean >= 0 ? new double[] { 0, 3, quasarMean } : null;
        return String.format(
            Locale.ROOT,
            "Scripted bursts: %.2f Bu/t (rail high %.2f, low %.2f, halo %.2f), linked, every field second%s",
            total,
            rt.benchBursts.hi,
            rt.benchBursts.lo,
            rt.benchBursts.halo,
            quasarMean >= 0 ? String.format(
                Locale.ROOT,
                "; the scripted quasar's mean stream %.2f Bu/t (peak %.2f)",
                quasarMean,
                rt.linkedQuasarPeak()) : "");
    }

    /** A scripted reading: these Bu/t on each group, the Eye's own port counts, linked when {@code linked}. */
    static EyeBursts feed(MTEEyeOfHarmonyBTS eye, double hi, double lo, double halo, boolean linked) {
        return feed(eye, hi, lo, halo, 0, 0, linked);
    }

    /** P5: a scripted reading with the collar (T9) and crown (T10) groups too. */
    static EyeBursts feed(MTEEyeOfHarmonyBTS eye, double hi, double lo, double halo, double collar, double crown,
        boolean linked) {
        final EyeBursts b = new EyeBursts();
        b.hi = Math.max(0, hi);
        b.lo = Math.max(0, lo);
        b.halo = Math.max(0, halo);
        b.collar = Math.max(0, collar);
        b.crown = Math.max(0, crown);
        b.collarPorts = eye.receptorCount(MTEEyeOfHarmonyBTS.GROUP_COLLAR);
        b.crownPorts = eye.receptorCount(MTEEyeOfHarmonyBTS.GROUP_CROWN);
        b.linkedRail = linked && b.rail() > 1e-9;
        b.linkedAll = linked && b.total() > 1e-9;
        b.hiPorts = eye.receptorCount(MTEEyeOfHarmonyBTS.GROUP_HIGH);
        b.loPorts = eye.receptorCount(MTEEyeOfHarmonyBTS.GROUP_LOW);
        b.haloPorts = eye.receptorCount(MTEEyeOfHarmonyBTS.GROUP_HALO);
        b.source = linked ? "the bench quasar" : "";
        b.mean60 = b.total();
        return b;
    }

    /**
     * P4 hosts unit: a rig outside this class that feeds this Eye its scripted bursts from a real quasar
     * ({@code machines.quasar.QuasarRigBench}): called first in {@link #liveSecond}, every real field second, while
     * set.
     */
    public static volatile java.util.function.Consumer<MTEEyeOfHarmonyBTS> liveHook;

    /**
     * P4 hosts unit: this second's scripted bursts on the Eye (in place of its Receptor Ports), from {@code source}:
     * linked when {@code linked}, paused (Q-8) when {@code paused}; {@code phi} / {@code b} are the source quasar's
     * flux and coil field for the collapse assist (P4-73).
     */
    public static void scripted(MTEEyeOfHarmonyBTS eye, double hi, double lo, double halo, boolean linked,
        String source, boolean paused, double phi, double b, double stream, double limit) {
        scripted(eye, hi, lo, halo, 0, 0, linked, source, paused, phi, b, stream, limit);
    }

    /** P5: as above, with the collar and crown groups. */
    public static void scripted(MTEEyeOfHarmonyBTS eye, double hi, double lo, double halo, double collar, double crown,
        boolean linked, String source, boolean paused, double phi, double b, double stream, double limit) {
        final EyeStarRuntime rt = eye.starRuntime();
        final EyeBursts e = feed(eye, hi, lo, halo, collar, crown, linked);
        e.limit = limit > 0 ? limit : Double.POSITIVE_INFINITY; // P4-88: J1's out-1 limit, as a receptor reads it
        e.source = linked ? source : "";
        e.paused = paused;
        e.pausedBy = paused ? source : "";
        rt.benchBursts = e;
        rt.benchFlux = new double[] { phi, b, stream }; // the source quasar as the Eye reads it (P4-73, P4-79)
    }

    /** P4 hosts unit: the Eye reads its GRB Receptor Ports again. */
    public static void unscripted(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        rt.benchBursts = null;
        rt.benchFlux = null;
    }

    /** The bench quasar, its conduit and the gate rig's wires (one per Eye being benched; not saved). */
    static final class Rig {

        final QuasarState q;
        final QuasarInputs qin = new QuasarInputs();
        final boolean analog;
        /** P4-88: the mouths' and elements' throat Mk, and J1's out-1 limit ({@link #STANDARD}, 0 none, or Bu/t). */
        int mk = 1;
        double limit = STANDARD;
        boolean purgeWire, beginWire, testFire;
        long eruptions, purges, blowOuts, begins;

        Rig(double m0, long seed, boolean analog) {
            this.analog = analog;
            q = new QuasarState(QuasarState.rngFor(seed));
            q.seed(m0, QuasarParams.aEq(1, 3));
        }
    }

    /** P4-88: a rig's J1 limit that follows the Eye ({@link EyeStarRuntime#standardJ1Limit}). */
    public static final double STANDARD = -1;

    /** P4-88: J1's limit this second for the rig setting {@code limit} (standard, none or Bu/t). */
    public static double j1Limit(MTEEyeOfHarmonyBTS eye, double limit) {
        return limit < 0 ? eye.starRuntime()
            .standardJ1Limit() : limit;
    }

    /** P4-88: "standard 7.50 Bu/t", "none" or "12.00 Bu/t", for the bench's lines. */
    public static String limitText(MTEEyeOfHarmonyBTS eye, double limit) {
        if (limit == 0) return "none";
        return String.format(Locale.ROOT, "%s%.2f Bu/t", limit < 0 ? "standard " : "", j1Limit(eye, limit));
    }

    /**
     * P4-88: the bench options {@code mk <1-4>} and {@code limit <Bu/t>|off|std} at {@code args[i]}; returns the
     * index of the last argument used (or {@code i - 1} if neither).
     */
    public static int parseRigOption(String[] args, int i, int[] mk, double[] limit) {
        if (i + 1 >= args.length) return i - 1;
        if ("mk".equalsIgnoreCase(args[i])) {
            mk[0] = Math.max(1, Math.min(BurstRouting.MK_MAX, Integer.parseInt(args[i + 1])));
            return i + 1;
        }
        if ("limit".equalsIgnoreCase(args[i])) {
            final String v = args[i + 1];
            limit[0] = "off".equalsIgnoreCase(v) || "none".equalsIgnoreCase(v) ? 0
                : "std".equalsIgnoreCase(v) || "standard".equalsIgnoreCase(v) ? STANDARD
                    : Math.max(0, Double.parseDouble(v));
            return i + 1;
        }
        return i - 1;
    }

    private static Rig rig;
    private static int rigFor;
    /** The rig keeps stepping in real time after a run (until {@code galaxy off}), so the galaxy is not starved. */
    private static boolean live;

    /**
     * Called by the Eye every real field second before its own: steps the live bench rig for this Eye (if any), so its
     * scripted bursts keep coming between bench commands.
     */
    public static void liveSecond(MTEEyeOfHarmonyBTS eye) {
        final java.util.function.Consumer<MTEEyeOfHarmonyBTS> hook = liveHook;
        if (hook != null) hook.accept(eye);
        EyeStdRig.second(eye); // B-225: the saved standard rig (a no-op when off)
        EyeP5Bench.refill(eye);
        if (!live || rig == null || rigFor != key(eye)) return;
        second(eye, eye.starRuntime(), rig);
    }

    /** {@code galaxy <seconds> [analog] [mass M]}: runs the rig and the Eye for that many field seconds. */
    public static List<String> galaxy(MTEEyeOfHarmonyBTS eye, String[] args, int at) {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = eye.starRuntime();
        if (args.length > at && "off".equalsIgnoreCase(args[at])) {
            live = false;
            rt.benchBursts = null;
            rt.benchFlux = null;
            out.add("galaxy rig off: the Eye reads its GRB Receptor Ports again");
            return out;
        }
        final long secs = args.length > at ? Long.parseLong(args[at]) : 600;
        boolean analog = false;
        double mass = 64;
        boolean fresh = false;
        final int[] mk = { -1 };
        final double[] limit = { Double.NaN };
        for (int i = at + 1; i < args.length; i++) {
            if ("analog".equalsIgnoreCase(args[i])) analog = true;
            if ("fresh".equalsIgnoreCase(args[i])) fresh = true;
            if ("mass".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                mass = Double.parseDouble(args[++i]);
                continue;
            }
            final int used = parseRigOption(args, i, mk, limit);
            if (used >= i) i = used;
        }
        final int id = key(eye);
        if (rig == null || rigFor != id || fresh) {
            rig = new Rig(mass, 1, analog);
            rigFor = id;
        }
        if (mk[0] > 0) rig.mk = mk[0];
        if (!Double.isNaN(limit[0])) rig.limit = limit[0];
        if (rt.rig == null) rt.setRig(FieldRig.Mode.FINE); // the Eye's own trims (T6 stellar trims, T7+ the Space
                                                           // valve)
        final int unlockedBefore = rt.unlockedTier();
        long fails = 0;
        final long t0 = System.nanoTime();
        for (long i = 0; i < secs; i++) {
            final String before = rt.lastFailure();
            second(eye, rt, rig);
            EyeP5Bench.refill(eye);
            eye.fieldSecond();
            if (!rt.lastFailure()
                .equals(before)) fails++;
        }
        live = true; // keep the rig stepping in real time (galaxy off stops it)
        final long ms = (System.nanoTime() - t0) / 1_000_000L;
        out.add(
            String.format(
                Locale.ROOT,
                "galaxy rig: %d s in %d ms (%s junctions, throats Mk %d, J1 limit %s); quasar M %.1f SM, a %.3f,"
                    + " E %s, Φ %.0f, G %.2f Bu/t; eruptions %d, purges %d, blow-outs %d; begins %d; Eye failures %d;"
                    + " unlocked T%d -> T%d",
                secs,
                ms,
                rig.analog ? "analog" : "binary",
                rig.mk,
                limitText(eye, rig.limit),
                rig.q.M,
                rig.q.a,
                StellarField.fmtSigned(rig.q.E),
                rig.q.Phi,
                rig.q.G,
                rig.eruptions,
                rig.purges,
                rig.blowOuts,
                rig.begins,
                fails,
                unlockedBefore,
                rt.unlockedTier()));
        return out;
    }

    /**
     * One rig second before the Eye's (the prototype's {@code worldTick} order): the Galaxy Transition bits, the
     * purge and begin wires, the quasar's disc second, the conduit and the junctions, then the Eye's scripted bursts.
     */
    static void second(MTEEyeOfHarmonyBTS eye, EyeStarRuntime rt, Rig r) {
        final int w = 5;
        final int trans = rt.directiveCode(DirectiveSet.EYE_GALAXY_TRANSITION, w);
        final int field = rt.directiveCode(DirectiveSet.EYE_FIELD, w);
        final int events = rt.directiveCode(DirectiveSet.EYE_EVENTS, w);
        final int slots = rt.directiveCode(DirectiveSet.EYE_SLOTS, w);
        final QuasarDirective.State ds = new QuasarDirective.State();
        ds.lagE = r.q.lagE;
        ds.lagPhi = r.q.lagPhi;
        ds.purgeAt = QuasarParams.PURGE_AT_DEFAULT;
        ds.open = r.q.hole == QuasarState.Hole.OPEN;
        ds.b = r.q.b;
        ds.vented = 0;
        ds.conduitOpen = true;
        final int disc = QuasarDirective.disc(ds, w);
        // ---- the wires: purge (Disc [4] AND NOT Transition [8]) OR Transition [4]; begin on Transition [1]
        final boolean purge = (bit(disc, 3) && !bit(trans, 4)) || bit(trans, 3);
        if (purge && !r.purgeWire) r.q.requestPurge();
        r.purgeWire = purge;
        r.testFire = bit(slots, 4);
        final boolean begin = bit(trans, 1);
        if (begin && !r.beginWire) {
            rt.requestBegin();
            r.begins++;
        }
        r.beginWire = begin;
        // ---- the quasar's disc second (helium, coils 3, b 3, f 1; the disc trims on Disc [1] / [2])
        r.qin.gas = QuasarGas.HE;
        r.qin.coilTier = 3;
        r.qin.bSetpoint = 3;
        r.qin.powerOk = true;
        r.qin.gasDrawn = QuasarParams.eddingtonFeed(r.q.M, QuasarGas.HE);
        r.qin.gasTrimUnits = bit(disc, 1) ? 10 : 0;
        r.qin.spaceTrimUnits = bit(disc, 2) ? 10 : 0;
        r.qin.regulatorPurge = false;
        final QuasarResult res = QuasarDisc.step(r.q, r.qin);
        if (res.eruption) r.eruptions++;
        if (res.purge) r.purges++;
        if (res.blowOut) r.blowOuts++;
        conduit(
            eye,
            res.reforming ? 0 : r.q.G / 2,
            r.q.hole == QuasarState.Hole.OPEN && r.q.b > 0,
            trans,
            field,
            events,
            r.testFire,
            r.analog,
            "the bench quasar",
            false,
            r.q.Phi,
            r.q.b,
            com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams.meanStream(r.q.G0, r.q.Yq, true),
            r.mk,
            j1Limit(eye, r.limit));
    }

    /**
     * The standard conduit rig, one second (shared by this bench's virtual quasar and the P4 hosts unit's real-quasar
     * rig, {@code machines.quasar.QuasarRigBench}): both jets ({@code jetPerPort} Bu/t each) into Mk {@code mk} mouths
     * (cap 24 Bu/t each at Mk I), combined; J1 Eye : crown on Field [2] / [1] (or the P sensor) OR Transition [4] (OR
     * Slots [8] at T6), its share taken of at most {@code limit} Bu/t (P4-88; 0 none); J3 halo : rail on Events [1] /
     * [2] (or the Λ sensor) at T8, 75 % halo on the T7 test fire; J2 high : low on Field [4] / [8] (or the W sensor).
     * The result is this second's scripted bursts on the Eye, from {@code source} (linked when {@code linkable} and
     * something flows; paused for Q-8 when {@code paused}).
     */
    public static void conduit(MTEEyeOfHarmonyBTS eye, double jetPerPort, boolean linkable, int trans, int field,
        int events, boolean testFire, boolean analog, String source, boolean paused, double phi, double b,
        double stream, int mk, double limit) {
        final EyeStarRuntime rt = eye.starRuntime();
        final double cap = BurstRouting.throatCap(mk);
        double sIn = 2 * Math.min(Math.max(0, jetPerPort), cap);
        sIn = Math.min(sIn, cap);
        final boolean linked = linkable && sIn > 1e-9;
        final int n = rt.fs == null ? 0 : rt.fs.tier.n;
        final boolean planets = rt.fs != null && rt.fs.tier.planets();
        final boolean gm = rt.transition() instanceof GalacticMerger;
        final boolean t8 = (n >= 8 && !planets) || gm;
        final boolean test8 = n == 7 && !planets && rt.transition() == null && testFire;
        final double[] e = rt.fs == null ? new double[StellarField.GAUGES] : rt.fs.prevE;
        final boolean lit = rt.galaxyLit();
        // J1 (Eye : crown): Transition [4] OR (T6 / T8 and Slots [8]) -> 15; else two Directive bits (or the P sensor)
        int s1 = analog ? BurstRouting.level(lit ? -e[StellarField.P] : 0, 30)
            : BurstRouting.twoLevel(bit(field, 2), bit(field, 1));
        // P5: the Deep Link's test fire (T8, Slots [8]) forces J1 too, and J4 sends 75 % of the Eye leg to the collars
        final boolean test9 = n == 8 && !planets && rt.transition() == null && testFire;
        if (bit(trans, 3) || (testFire && n == 6) || test9) s1 = 15;
        final double sh1 = test8 ? 1 : BurstRouting.share(0.5, 2, s1);
        // P4-88: J1's out 1 is a share of at most its limit (BurstRouting.split); the rest goes to the crown
        final double eyeAll = BurstRouting.split(sIn, sh1, 0, limit)[0];
        final double collar = test9
            ? eyeAll * com.nateyburr.beyondthestars.machines.quasar.engine.DeepLink.TEST_FIRE_SHARE
            : 0;
        // P5 T10 (and Inflation once inflated): every burst lands on the crown's 12 ports
        if (rt.fs != null && rt.fs.tier.universe) {
            scripted(eye, 0, 0, 0, 0, eyeAll, linked, source, paused, phi, b, stream, limit);
            return;
        }
        final double eyeS = eyeAll - collar;
        double halo = 0, rail = eyeS;
        if (t8) {
            final int s3 = analog ? BurstRouting.level(lit ? e[StellarField.L] : 0, 30)
                : BurstRouting.twoLevel(bit(events, 1), bit(events, 2));
            halo = eyeS * BurstRouting.share(0.2, 2, s3);
            rail = eyeS - halo;
        } else if (test8) {
            halo = eyeS * 0.75;
            rail = eyeS - halo;
        }
        final int s2 = analog ? BurstRouting.level(lit ? e[StellarField.W] : 0, 30)
            : BurstRouting.twoLevel(bit(field, 3), bit(field, 4));
        final double hi = rail * BurstRouting.share(0.5, 2, s2);
        scripted(eye, hi, rail - hi, halo, collar, 0, linked, source, paused, phi, b, stream, limit);
    }

    /** The Eye's position as the rig's key (a chunk reload makes a new controller object, not a new Eye). */
    private static int key(MTEEyeOfHarmonyBTS eye) {
        final gregtech.api.interfaces.tileentity.IGregTechTileEntity b = eye.getBaseMetaTileEntity();
        return b == null ? 0 : (b.getXCoord() * 31 + b.getYCoord()) * 31 + b.getZCoord();
    }

    private static boolean bit(int code, int n) {
        return DirectiveSet.isSet(code, n);
    }
}
