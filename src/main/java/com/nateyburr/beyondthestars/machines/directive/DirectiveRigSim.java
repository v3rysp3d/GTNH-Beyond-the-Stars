package com.nateyburr.beyondthestars.machines.directive;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.eye.field.EyeTrimRule;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * AUTOMATION PASS (plan item 18, task 4): the offline proof that the Eye's stars and the Kerr's hole run unattended
 * on plain redstone. A plain-JDK program like {@code FieldSim} (no Minecraft, GT or library classes are loaded: the
 * field package and the Directive maths are pure). Run it with
 * {@code java -cp build/classes/java/main com.nateyburr.beyondthestars.machines.directive.DirectiveRigSim [hours]
 * [seeds]}.
 * <p>
 * <b>The Eye's gate rig</b> reads ONLY Directive bits, a second late as a hatch puts them out: the Trims set (one
 * hatch per bit, "Bit n" mode) opens one fixed-rate valve per bit (a Fluid Regulator cover or an export bus with a
 * redstone card), the Status set's [4] opens the event valve (T3: more Super Coolant; T6: the Agent), its [2] hands
 * over a Stellar Ignition Charge after a loss (a relight), the Transition set's [1] starts the Core Collapse (an ME
 * interface pushing the Neutron Star chart) and its [8] (Tachyon wanted) holds the Tachyon valve open. No logic gates
 * are needed for the Eye. It is compared, seed for seed, with the prototype's bang-bang rig.
 * <p>
 * <b>The Kerr's gate rig</b> reads the Hole set: [4] "bolts short" switches an export bus that moves bolts into a bolt
 * bus, [1] "closed" exports one Black Hole Seed (Auto opens the hole with it). The hole physics mirror
 * {@code MTEKerrEngine.tickHole} (bleed 3.2/M² SM a tick, the governor firing up to the rail cap a tick), and the bits
 * are {@link KerrDirective#code}, the function the KEE's hatch uses. An OpenComputers-style setter loop then ramps
 * the setpoint the way a script would through {@code bts_kerr.setSetpoint}, with the GUI's clamp.
 * <p>
 * Exit code 0 when every Eye case loses no more stars than the prototype rig, every Kerr case never evaporates, and
 * the setter loop reaches its target.
 */
public final class DirectiveRigSim {

    private DirectiveRigSim() {}

    /** The Trims valves, units/s at p = 1 per trim-table position ({@link EyeTrimRule#rigRate}). */
    static double[] rates(int tier) {
        final double[] r = new double[4];
        for (int i = 0; i < 4; i++) r[i] = EyeTrimRule.rigRate(tier, i);
        return r;
    }

    /** The Status bit 3 valve ({@link EyeTrimRule#rigExtraRate}). */
    static double extraRate(int tier) {
        return EyeTrimRule.rigExtraRate(tier);
    }

    public static final int WINDOW = 10;

    /** The gate rig: only Directive bits from last second's field (the hatch's 1 s lag) open valves. */
    static LinkedHashMap<String, Double> gateRig(TierParams t, FieldState s) {
        final int trims = EyeTrimRule.trimBits(t, s.prevE, s.prevWarn, WINDOW);
        final boolean extra = EyeTrimRule.extra(t, s.prevE, s.prevWarn, WINDOW);
        return EyeTrimRule.gateRig(t, trims, extra, rates(t.n), extraRate(t.n));
    }

    /** One run's tally. */
    static final class Run {

        long fails, cycles, secs, mismatch;
        final long[] zones = new long[5];
        double ySum;
        long yN;
        String collapse = "";
    }

    static Run eye(int tier, long seed, long secs, boolean gate, boolean collapse) {
        final FieldSim.Cfg c = new FieldSim.Cfg();
        c.seed = seed;
        c.rig = FieldRig.Mode.BANG;
        if (gate) c.rigFn = DirectiveRigSim::gateRig;
        if (collapse) {
            c.collapse = true;
            c.stBuf = 90_000;
            c.shock = 150;
            c.tachRate = 100;
        }
        final FieldSim.Proto S = new FieldSim.Proto(tier, c);
        final Run r = new Run();
        boolean begun = false;
        for (long i = 0; i < secs; i++) {
            // Transition bit 1 (ready) -> the next chart goes in; bit 4 (Tachyon wanted) holds the valve open
            if (collapse && !begun && S.s.t >= c.beginAt && S.canBegin()) {
                S.beginCollapse();
                begun = true;
            }
            if (gate && (tier == 1 || tier == 2 || tier == 4) && S.state == FieldSim.Proto.LIT) {
                // the gate rig asks for exactly the trims the bang-bang rig asks for on these tiers
                final Map<String, Double> a = gateRig(S.s.tier, S.s);
                final Map<String, Double> b = FieldRig.ctrl(S.s.tier, S.s.prevE, S.s.prevWarn, FieldRig.Mode.BANG);
                if (!a.equals(b)) r.mismatch++;
            }
            final long failsBefore = S.L.fails;
            S.step();
            if (S.state == FieldSim.Proto.DARK) S.relight(); // Status bit 2: a charge goes in
            if (S.L.fails > failsBefore) r.fails += S.L.fails - failsBefore;
        }
        r.cycles = S.L.cycles;
        System.arraycopy(S.L.zoneT, 0, r.zones, 0, 5);
        r.ySum = S.L.ySum;
        r.yN = S.L.yN;
        r.secs = secs;
        if (collapse) r.collapse = c.unlocked4 ? "settled (T4 unlocked)" : begun ? "lost" : "never began";
        return r;
    }

    // ============================================================================ the Kerr

    /** The Kerr model: a hole, a bolt bus, the governor, the redstone rig and an optional setter script. */
    static final class Kerr {

        final KerrDirective.State st = new KerrDirective.State();
        final double bolt;
        final int cap;
        final int busCap = 16 * 64;
        int bolts, seeds;
        double windowFed, windowBled;
        int windowTicks;
        long evaporations, belowSeconds, shortSeconds, boltsUsed, seedsUsed;
        double maxDev;

        Kerr(double boltMass, int railCap, int setpoint) {
            bolt = boltMass;
            cap = railCap;
            st.formed = true;
            st.setpoint = setpoint;
        }

        /** One tick of {@code MTEKerrEngine.tickHole} (containment power always there). */
        void tick() {
            if (!st.open) return;
            final double bleed = 3.2 / (st.mass * st.mass);
            st.bleedPerTick = bleed;
            st.mass -= bleed;
            windowBled += bleed;
            double deficit = st.setpoint - st.mass;
            if (deficit > 0) {
                final int n = Math.min(bolts, Math.min(cap, (int) Math.ceil(deficit / bolt)));
                bolts -= n;
                boltsUsed += n;
                st.mass += n * bolt;
                windowFed += n * bolt;
            }
            if (++windowTicks >= 20) {
                final boolean holding = st.mass >= st.setpoint - 1e-3;
                st.stability = windowBled <= 0 || holding ? 1.0 : Math.min(1.0, windowFed / windowBled);
                windowFed = windowBled = 0;
                windowTicks = 0;
            }
            if (st.mass < 2.0) {
                st.open = false;
                st.mass = 0;
                evaporations++;
            }
        }

        /** One second: 20 ticks, the readouts, then the rig reacting to the Hole bits (a second late, as in play). */
        void second(int exportPerSecond) {
            // Auto: a Seed in the bolt bus opens a closed hole (8 SM)
            if (!st.open && seeds > 0) {
                seeds--;
                seedsUsed++;
                st.open = true;
                st.mass = 8.0;
            }
            for (int i = 0; i < 20; i++) tick();
            st.boltStockSm = bolts * bolt;
            st.seeds = seeds;
            final int code = KerrDirective.code(DirectiveSet.KERR_HOLE, st, WINDOW);
            if (st.open) {
                maxDev = Math.max(maxDev, Math.abs(st.mass - st.setpoint));
                if ((code & 2) != 0) belowSeconds++;
            }
            if ((code & 4) != 0) {
                shortSeconds++;
                bolts = Math.min(busCap, bolts + exportPerSecond); // export bus on bit 3 (value 4)
            }
            if ((code & 1) != 0 && seeds == 0) seeds++; // export bus on bit 1: one Seed
        }
    }

    static String kerrCase(String label, double boltMass, int cap, int setpoint, int maxSetpoint, int export, long secs,
        int scriptTarget) {
        final Kerr k = new Kerr(boltMass, cap, setpoint);
        String script = "";
        for (long i = 0; i < secs; i++) {
            k.second(export);
            // the OpenComputers-style setter loop: once a minute, if the hole holds (stability 1, inside the band),
            // ask for 8 SM more, through the GUI's clamp (2 .. the tier's top)
            if (scriptTarget > 0 && i % 60 == 59
                && k.st.open
                && k.st.stability >= 1
                && Math.abs(k.st.mass - k.st.setpoint) <= KerrDirective.BAND
                && k.st.setpoint < scriptTarget) {
                k.st.setpoint = Math.max(2, Math.min(maxSetpoint, (int) k.st.setpoint + 8));
            }
        }
        if (scriptTarget > 0) {
            final int clamped = Math.max(2, Math.min(maxSetpoint, 100_000));
            script = String.format(
                Locale.ROOT,
                "; setter loop reached %d SM (target %d), setSetpoint(100000) clamps to %d",
                (int) k.st.setpoint,
                scriptTarget,
                clamped);
        }
        final boolean ok = k.evaporations == 0 && k.st.open && (scriptTarget <= 0 || k.st.setpoint >= scriptTarget);
        return String.format(
            Locale.ROOT,
            "%s %s: %d s, evaporations %d, seeds used %d, mass %.2f SM (setpoint %d), max deviation %.2f SM, "
                + "below-setpoint %d s, bolts-short %d s, bolts used %d%s",
            ok ? "PASS" : "FAIL",
            label,
            secs,
            k.evaporations,
            k.seedsUsed,
            k.st.mass,
            (int) k.st.setpoint,
            k.maxDev,
            k.belowSeconds,
            k.shortSeconds,
            k.boltsUsed,
            script);
    }

    public static void main(String[] args) {
        final double hours = args.length > 0 ? Double.parseDouble(args[0]) : 4;
        final int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        final long secs = (long) (hours * 3600);
        boolean ok = true;
        System.out.printf(
            Locale.ROOT,
            "== Eye: the Directive gate rig (w = %d, fixed-rate valves) against the prototype's bang-bang rig, %d "
                + "seeds x %.1f h ==%n",
            WINDOW,
            seeds,
            hours);
        for (int tier = 1; tier <= 6; tier++) {
            long gf = 0, bf = 0, mis = 0, gz0 = 0, gz1 = 0, gs = 0, gc = 0;
            double gy = 0, by = 0;
            long gyn = 0, byn = 0;
            for (int seed = 1; seed <= seeds; seed++) {
                final Run g = eye(tier, seed, secs, true, false);
                final Run b = eye(tier, seed, secs, false, false);
                gf += g.fails;
                bf += b.fails;
                mis += g.mismatch;
                gz0 += g.zones[0];
                gz1 += g.zones[1];
                gs += g.secs;
                gc += g.cycles;
                gy += g.ySum;
                gyn += g.yN;
                by += b.ySum;
                byn += b.yN;
            }
            final boolean pass = gf <= bf;
            ok &= pass;
            System.out.printf(
                Locale.ROOT,
                "%s T%d %-12s gate rig: %d lost, %.1f %% Resonant, %.1f %% Stable, mean Y %.3f, %d cycles | "
                    + "bang-bang: %d lost, mean Y %.3f%s%n",
                pass ? "PASS" : "FAIL",
                tier,
                TierParams.of(tier).star,
                gf,
                100.0 * gz0 / Math.max(1, gs),
                100.0 * gz1 / Math.max(1, gs),
                gyn > 0 ? gy / gyn : 0,
                gc,
                bf,
                byn > 0 ? by / byn : 0,
                tier == 1 || tier == 2 || tier == 4 ? ", seconds asking differently: " + mis : "");
        }
        int settled = 0, settledB = 0;
        for (int seed = 1; seed <= seeds; seed++) {
            if (eye(3, seed, 3600, true, true).collapse.startsWith("settled")) settled++;
            if (eye(3, seed, 3600, false, true).collapse.startsWith("settled")) settledB++;
        }
        ok &= settled >= settledB;
        System.out.printf(
            Locale.ROOT,
            "%s Core Collapse T3 -> T4 (SpaceTime 90,000 L, bounce +150; begun on Transition bit 1, Tachyon on bit 4): "
                + "gate rig %d/%d settled, bang-bang %d/%d%n",
            settled >= settledB ? "PASS" : "FAIL",
            settled,
            seeds,
            settledB,
            seeds);

        System.out.printf(
            Locale.ROOT,
            "== Kerr: the Hole-set gate rig (w = %d min; [4] bolts short -> bolt export bus, [1] closed -> one Seed) ==%n",
            WINDOW);
        final String[] kc = {
            kerrCase("T1, SpaceTime bolts, Mk I rails, setpoint 32, 64 bolts/s export", 0.25, 4, 32, 64, 64, secs, 0),
            kerrCase(
                "T1, Neutronium bolts, Mk II rails, setpoint 16, 256 bolts/s export",
                1.0 / 64,
                8,
                16,
                64,
                256,
                secs,
                0),
            kerrCase(
                "T2, Infinity bolts, Mk III rails, setpoint 512, 64 bolts/s export",
                1.0 / 16,
                16,
                512,
                512,
                64,
                secs,
                0),
            kerrCase(
                "T1 from a closed hole, setter loop 8 -> 64 SM (bts_kerr.setSetpoint)",
                0.25,
                4,
                8,
                64,
                64,
                secs,
                64),
            kerrCase("T2 from a closed hole, setter loop 8 -> 512 SM", 0.125, 16, 8, 512, 64, secs, 512) };
        for (String line : kc) {
            ok &= line.startsWith("PASS");
            System.out.println(line);
        }
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        System.exit(ok ? 0 : 1);
    }
}
