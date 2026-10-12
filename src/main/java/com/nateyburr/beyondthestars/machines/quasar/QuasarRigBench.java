package com.nateyburr.beyondthestars.machines.quasar;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.directive.DirectiveBench;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxyBench;
import com.nateyburr.beyondthestars.machines.eye.star.EyeMilestones;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * P4 hosts unit, quasar-p4.md §8.3 / §8.4 on a dev server: the gate rig on a REAL Quasar Engine (MTE 28120) and,
 * optionally, a REAL Eye of Harmony ({@code /btsquasar autorig x y z <s> [eye ex ey ez] [analog] [cut k] [mass M]}).
 * The rig sees ONLY what the machines' Directive Hatches put out (the codes a comparator reads, a second late) and acts
 * only through the machines' own hatches, as AE2, redstone and logic gates would:
 * <ul>
 * <li><b>Quasar disc</b> (its Disc hatch, window 5): Disc [1] → 10 units of the feed gas into the plain trim hatch,
 * Disc [2] → 10 units of Spatially Enlarged Fluid (two export buses with redstone cards); the gas hatches kept
 * stocked (an ME stocking hatch).</li>
 * <li><b>Purge</b>: (Disc [4] AND NOT Galaxy Transition [8]) OR Galaxy Transition [4] → the quasar's own Burst Control
 * Hatch (PURGE mode), which purges on the rising edge.</li>
 * <li><b>The Eye</b> (with {@code eye}): its Trims / Status / Transition hatches drive the trim valves, the stock-up,
 * charges, cores and charts exactly as {@code /btseye star … autorig} does ({@link DirectiveBench.EyeRig}); from T7 the
 * one Space valve is Field [1] (§8.4); Galaxy Transition [1] → the Eye's own Burst Control Hatch in BEGIN mode
 * (rising edge); Slots [8] → the test fire.</li>
 * <li><b>The conduit</b>: the standard rig's maths ({@link EyeGalaxyBench#conduit}): both of the real quasar's jets
 * through two Mk I mouth pairs into J1 (Eye : crown, two Directive bits; OR Transition [4]; OR Slots [8] at T6), J3
 * (halo : rail at T8) and J2 (high : low) - all binary by default (F-8), or the P / W / Λ sensors with
 * {@code analog}. The real conduit elements are proven by {@code /btsconduit rig}; aiming 16 of them at an Eye's ports
 * is not part of the bench (P4-75).</li>
 * </ul>
 * Each rig second: the codes, the actions, the quasar's own disc second ({@link MTEQuasarEngine#benchRun}: its real
 * hatches, Directive and Burst Control polling), the conduit, then the Eye's own field second. Fast-forward runs set
 * the
 * Burst Control Hatches' input directly ({@link MTEHatchBurstControl#benchSignal}); after a run with an Eye the rig
 * keeps
 * acting in real time (each of the Eye's field seconds, through {@link EyeGalaxyBench#liveHook}) with real redstone
 * blocks against the two hatches, until {@code autorig … off}.
 */
public final class QuasarRigBench {

    private QuasarRigBench() {}

    /** The window every rig hatch reads at (the questbook's standard rig, §8.4). */
    public static final int W = 5;
    /** One trim valve's units a second (§8.4). */
    public static final int VALVE_UNITS = 10;

    /** One rig: the machines it acts on (by position) and its wires; not saved. */
    static final class Rig {

        final int qx, qy, qz;
        final int ex, ey, ez;
        final boolean withEye, analog;
        boolean purgeWire, beginWire;
        DirectiveBench.EyeRig eyeRig;
        long purgeEdges, beginEdges, gasValveS, spaceValveS, eyeSpaceS, testFireS;
        final long[] discBits = new long[4];
        final long[] discZones = new long[5];
        long eruptions, purges, blowOuts;
        /** Live: the redstone block placed against each hatch (to take away again). */
        final Map<MTEHatchBurstControl, int[]> blocks = new IdentityHashMap<>();

        Rig(MTEQuasarEngine q, MTEEyeOfHarmonyBTS eye, boolean analog) {
            final IGregTechTileEntity b = q.getBaseMetaTileEntity();
            qx = b.getXCoord();
            qy = b.getYCoord();
            qz = b.getZCoord();
            withEye = eye != null;
            final IGregTechTileEntity e = eye == null ? null : eye.getBaseMetaTileEntity();
            ex = e == null ? 0 : e.getXCoord();
            ey = e == null ? 0 : e.getYCoord();
            ez = e == null ? 0 : e.getZCoord();
            this.analog = analog;
        }

        boolean isEye(MTEEyeOfHarmonyBTS eye) {
            final IGregTechTileEntity e = eye.getBaseMetaTileEntity();
            return withEye && e != null && e.getXCoord() == ex && e.getYCoord() == ey && e.getZCoord() == ez;
        }
    }

    /** The rig acting in real time after a run (one at a time), or null. */
    private static volatile Rig live;

    private static boolean bit(int code, int n) {
        return DirectiveSet.isSet(code, n);
    }

    private static MTEHatchBurstControl hatch(List<MTEHatchBurstControl> list, MTEHatchBurstControl.Mode mode) {
        for (MTEHatchBurstControl h : list) if (h != null && h.isValid() && h.getMode() == mode) return h;
        for (MTEHatchBurstControl h : list) if (h != null && h.isValid()) return h; // a fresh hatch adopts the mode
        return null;
    }

    /** Stops the live rig (its redstone blocks go). */
    public static String off(World world) {
        final Rig r = live;
        live = null;
        EyeGalaxyBench.liveHook = null;
        if (r == null) return "No quasar rig running.";
        clearBlocks(world, r);
        return "Quasar rig off: the Eye reads its GRB Receptor Ports again on the next autorig-free second.";
    }

    /**
     * {@code autorig}: {@code secs} rig seconds at once, then (with an Eye) the rig keeps acting in real time.
     * {@code mass} (≤ 0: unchanged) sets the hole first; {@code cut} holds every valve shut for the first k seconds.
     */
    public static List<String> autorig(World world, MTEQuasarEngine q, long secs, MTEEyeOfHarmonyBTS eye,
        boolean analog, long cut, double mass) {
        final List<String> out = new ArrayList<>();
        final MTEHatchDirective disc = DirectiveBench.hatch(q.directiveHatches(), DirectiveSet.QUASAR_DISC);
        final MTEHatchBurstControl purgeH = hatch(q.burstControlHatches(), MTEHatchBurstControl.Mode.PURGE);
        if (disc == null || purgeH == null) {
            out.add("The quasar needs a Disc Directive Hatch and a Burst Control Hatch (the kit's build fits both).");
            return out;
        }
        disc.setWindow(W);
        final Rig r = new Rig(q, eye, analog);
        MTEHatchDirective transH = null, fieldH = null, eventsH = null, slotsH = null;
        MTEHatchBurstControl beginH = null;
        if (eye != null) {
            eye.benchEnsureChecked();
            if (DirectiveBench.EyeRig.of(eye) == null) out.addAll(DirectiveBench.eyeSetup(eye));
            r.eyeRig = DirectiveBench.EyeRig.of(eye);
            transH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_GALAXY_TRANSITION);
            fieldH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_FIELD);
            eventsH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_EVENTS);
            slotsH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_SLOTS);
            beginH = hatch(eye.burstControlHatches(), MTEHatchBurstControl.Mode.BEGIN);
            if (r.eyeRig == null || transH == null
                || fieldH == null
                || eventsH == null
                || slotsH == null
                || beginH == null) {
                out.add(
                    "The Eye needs Trims, Status, Transition, Field, Events, Galaxy Transition and Slots Directive"
                        + " Hatches and a Burst Control Hatch on a 'd' socket (/btseye t7|t8 fits the P4 ones, 'directive'"
                        + " the first three).");
                return out;
            }
            for (MTEHatchDirective h : new MTEHatchDirective[] { transH, fieldH, eventsH, slotsH }) h.setWindow(W);
        }
        if (live != null) clearBlocks(world, live);
        live = null;
        EyeGalaxyBench.liveHook = null;
        if (mass > 0) q.debugMass(mass);
        final EyeStarRuntime rt = eye == null ? null : eye.starRuntime();
        final int unlockedBefore = rt == null ? 0 : rt.unlockedTier();
        final long[] eyeZones = new long[5];
        long eyeFails = 0;
        int lastUnlocked = unlockedBefore;
        String lastFailure = rt == null ? "" : rt.lastFailure();
        final long t0 = System.nanoTime();
        for (long i = 0; i < secs; i++) {
            final boolean shut = i < cut;
            act(world, r, q, eye, disc, purgeH, transH, fieldH, eventsH, slotsH, beginH, shut, false);
            final QuasarResult res = q.benchRun(1, true);
            tally(r, res);
            if (eye != null) {
                feedEye(r, q, eye, transH, fieldH, eventsH, slotsH);
                eye.fieldSecond();
                if (!rt.lastFailure()
                    .equals(lastFailure)) {
                    lastFailure = rt.lastFailure();
                    eyeFails++;
                    out.add("  t+" + i + " s: " + lastFailure);
                }
                if (rt.burning()) {
                    final int z = rt.lastReadout().zone;
                    if (z >= 0 && z < 5) eyeZones[z]++;
                }
                if (rt.unlockedTier() != lastUnlocked) {
                    lastUnlocked = rt.unlockedTier();
                    out.add("  t+" + i + " s: UNLOCKED T" + lastUnlocked);
                }
            }
        }
        final long ms = (System.nanoTime() - t0) / 1_000_000L;
        final QuasarState s = q.state();
        out.add(
            String.format(
                Locale.ROOT,
                "quasar autorig %d s in %d ms (%s junctions): M %.1f SM, a %.3f, E %+.1f, Φ %.0f, b %.2f, G %.2f Bu/t;"
                    + " disc zones R/S/St/C/B %d/%d/%d/%d/%d; eruptions %d, purges %d (edges %d), blow-outs %d;"
                    + " valves: gas %d s, Space %d s",
                secs,
                ms,
                analog ? "analog" : "binary",
                s.M,
                s.a,
                s.E,
                s.Phi,
                s.b,
                s.G,
                r.discZones[0],
                r.discZones[1],
                r.discZones[2],
                r.discZones[3],
                r.discZones[4],
                r.eruptions,
                r.purges,
                r.purgeEdges,
                r.blowOuts,
                r.gasValveS,
                r.spaceValveS));
        out.add(
            String.format(
                Locale.ROOT,
                "Disc bit-seconds [1]/[2]/[4]/[8]: %d/%d/%d/%d; %s",
                r.discBits[0],
                r.discBits[1],
                r.discBits[2],
                r.discBits[3],
                disc.statusLine()));
        if (eye != null) {
            final EyeMilestones m = rt.milestones();
            out.add(
                String.format(
                    Locale.ROOT,
                    "Eye: unlocked T%d -> T%d, %s; zones R/S/St/C/B %d/%d/%d/%d/%d; failures %d; begin edges %d,"
                        + " Eye Space valve %d s, test fire %d s; rig: stock-ups %d, charges %d, cores %d, charts %d",
                    unlockedBefore,
                    rt.unlockedTier(),
                    rt.state(),
                    eyeZones[0],
                    eyeZones[1],
                    eyeZones[2],
                    eyeZones[3],
                    eyeZones[4],
                    eyeFails,
                    r.beginEdges,
                    r.eyeSpaceS,
                    r.testFireS,
                    r.eyeRig.stocks,
                    r.eyeRig.charges,
                    r.eyeRig.cores,
                    r.eyeRig.charts));
            out.add("  " + m.progressLine(6));
            out.add("  " + m.progressLine(7));
            // keep acting in real time: the Eye's field seconds call the hook (real redstone from now on)
            live = r;
            EyeGalaxyBench.liveHook = QuasarRigBench::liveSecond;
            out.add("The rig keeps acting in real time (autorig off stops it).");
        }
        return out;
    }

    private static void tally(Rig r, QuasarResult res) {
        if (res == null) return;
        if (res.eruption) r.eruptions++;
        if (res.purge) r.purges++;
        if (res.blowOut) r.blowOuts++;
        if (res.zone >= 0 && res.zone < 5) r.discZones[res.zone]++;
    }

    /**
     * One second's actions on last second's codes: the disc valves, the purge wire, the Eye's valves and items, the
     * begin wire. {@code realRedstone}: the wires are redstone blocks against the hatches (live), else the hatches'
     * bench input (fast-forward, no world ticks in between).
     */
    private static void act(World world, Rig r, MTEQuasarEngine q, MTEEyeOfHarmonyBTS eye, MTEHatchDirective disc,
        MTEHatchBurstControl purgeH, MTEHatchDirective transH, MTEHatchDirective fieldH, MTEHatchDirective eventsH,
        MTEHatchDirective slotsH, MTEHatchBurstControl beginH, boolean shut, boolean realRedstone) {
        final int d = shut ? 0 : disc.currentCode();
        final int trans = shut || transH == null ? 0 : transH.currentCode();
        final int field = shut || fieldH == null ? 0 : fieldH.currentCode();
        for (int b = 0; b < 4; b++) if ((d & (1 << b)) != 0) r.discBits[b]++;
        // the disc valves (export buses with redstone cards into the plain trim hatches)
        final int gas = bit(d, 1) ? VALVE_UNITS : 0, space = bit(d, 2) ? VALVE_UNITS : 0;
        if (gas + space > 0) QuasarKit.fillTrims(q, world, gas, space);
        if (gas > 0) r.gasValveS++;
        if (space > 0) r.spaceValveS++;
        // the purge wire: (Disc [4] AND NOT Transition [8]) OR Transition [4]
        final boolean purge = (bit(d, 3) && !bit(trans, 4)) || bit(trans, 3);
        if (purge && !r.purgeWire) r.purgeEdges++;
        r.purgeWire = purge;
        wire(world, r, purgeH, purge, realRedstone);
        if (eye == null) return;
        // the Eye's valves, stock-up, charges, cores and charts; from T7 the one Space valve is Field [1]
        final TierParams table = eye.starRuntime()
            .directiveTrimTable();
        final boolean galaxy = table != null && table.n >= 7;
        final int spaceCode = galaxy ? (bit(field, 1) ? 1 : 0) : -1;
        if (galaxy && bit(field, 1) && !shut) r.eyeSpaceS++;
        r.eyeRig.act(eye, shut, spaceCode);
        // the begin wire: Galaxy Transition [1]
        final boolean begin = bit(trans, 1);
        if (begin && !r.beginWire) r.beginEdges++;
        r.beginWire = begin;
        wire(world, r, beginH, begin, realRedstone);
        if (slotsH != null && bit(slotsH.currentCode(), 4)) r.testFireS++;
    }

    /** This second's scripted bursts on the Eye from the real quasar's last disc second, through the conduit model. */
    private static void feedEye(Rig r, MTEQuasarEngine q, MTEEyeOfHarmonyBTS eye, MTEHatchDirective transH,
        MTEHatchDirective fieldH, MTEHatchDirective eventsH, MTEHatchDirective slotsH) {
        final QuasarResult res = q.lastResult();
        final QuasarState s = q.state();
        final boolean running = s.hole == QuasarState.Hole.OPEN || s.hole == QuasarState.Hole.REFORM;
        final double jet = res == null || res.reforming || !running ? 0 : res.jet;
        EyeGalaxyBench.conduit(
            eye,
            jet,
            q.linkable(),
            transH.currentCode(),
            fieldH.currentCode(),
            eventsH.currentCode(),
            bit(slotsH.currentCode(), 4),
            r.analog,
            "the quasar at " + r.qx + ", " + r.qy + ", " + r.qz,
            q.isFrozen(),
            s.Phi,
            s.b);
    }

    /** A wire into a Burst Control Hatch: its bench input (fast-forward) or a real redstone block (live). */
    private static void wire(World world, Rig r, MTEHatchBurstControl h, boolean on, boolean realRedstone) {
        if (h == null) return;
        if (!realRedstone) {
            h.benchSignal(on ? 15 : 0);
            return;
        }
        final int[] at = r.blocks.get(h);
        if (on && at == null) {
            final IGregTechTileEntity b = h.getBaseMetaTileEntity();
            for (ForgeDirection f : ForgeDirection.VALID_DIRECTIONS) {
                final int x = b.getXCoord() + f.offsetX, y = b.getYCoord() + f.offsetY, z = b.getZCoord() + f.offsetZ;
                if (!world.isAirBlock(x, y, z)) continue;
                world.setBlock(x, y, z, Blocks.redstone_block);
                r.blocks.put(h, new int[] { x, y, z });
                return;
            }
        } else if (!on && at != null) {
            if (world.getBlock(at[0], at[1], at[2]) == Blocks.redstone_block) world.setBlockToAir(at[0], at[1], at[2]);
            r.blocks.remove(h);
        }
    }

    private static void clearBlocks(World world, Rig r) {
        for (int[] at : r.blocks.values()) {
            if (world.getBlock(at[0], at[1], at[2]) == Blocks.redstone_block) world.setBlockToAir(at[0], at[1], at[2]);
        }
        r.blocks.clear();
    }

    /**
     * Every real field second of the Eye the live rig is on (before the Eye's own second): the actions with real
     * redstone, then the conduit from the quasar's own last second (it ticks itself).
     */
    public static void liveSecond(MTEEyeOfHarmonyBTS eye) {
        final Rig r = live;
        if (r == null || !r.isEye(eye)) return;
        final IGregTechTileEntity eb = eye.getBaseMetaTileEntity();
        final World world = eb == null ? null : eb.getWorld();
        if (world == null || !world.blockExists(r.qx, r.qy, r.qz)) return;
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, r.qx, r.qy, r.qz);
        if (q == null) return;
        final MTEHatchDirective disc = DirectiveBench.hatch(q.directiveHatches(), DirectiveSet.QUASAR_DISC);
        final MTEHatchBurstControl purgeH = hatch(q.burstControlHatches(), MTEHatchBurstControl.Mode.PURGE);
        final MTEHatchDirective transH = DirectiveBench
            .hatch(eye.directiveHatches(), DirectiveSet.EYE_GALAXY_TRANSITION);
        final MTEHatchDirective fieldH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_FIELD);
        final MTEHatchDirective eventsH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_EVENTS);
        final MTEHatchDirective slotsH = DirectiveBench.hatch(eye.directiveHatches(), DirectiveSet.EYE_SLOTS);
        final MTEHatchBurstControl beginH = hatch(eye.burstControlHatches(), MTEHatchBurstControl.Mode.BEGIN);
        if (disc == null || purgeH == null || transH == null || fieldH == null || eventsH == null || slotsH == null) {
            return;
        }
        r.eyeRig = DirectiveBench.EyeRig.of(eye);
        if (r.eyeRig == null) return;
        act(world, r, q, eye, disc, purgeH, transH, fieldH, eventsH, slotsH, beginH, false, true);
        QuasarKit.refillGas(q, world); // the ME stocking hatches
        feedEye(r, q, eye, transH, fieldH, eventsH, slotsH);
    }

    /** {@code autorig status}: the live rig, if any. */
    public static String status() {
        final Rig r = live;
        if (r == null) return "No quasar rig running in real time.";
        return String.format(
            Locale.ROOT,
            "Live quasar rig: quasar %d %d %d -> Eye %d %d %d; purge edges %d, begin edges %d, eruptions %d,"
                + " purges %d, blow-outs %d",
            r.qx,
            r.qy,
            r.qz,
            r.ex,
            r.ey,
            r.ez,
            r.purgeEdges,
            r.beginEdges,
            r.eruptions,
            r.purges,
            r.blowOuts);
    }
}
