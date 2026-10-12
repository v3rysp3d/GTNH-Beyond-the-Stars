package com.nateyburr.beyondthestars.machines.quasar.engine;

import java.util.Locale;

import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * P4: the offline proof of quasar-p4.md §8.4 (like {@code DirectiveRigSim} for T1-T6): the T6 → T7 and T7 → T8
 * chains run unattended on a rig that reads ONLY Directive codes (computed by {@code QuasarDirective} and
 * {@code GalaxyDirective}, the functions the hatches use) and three analog sensor wires (P, W, Λ at window ±30, the
 * Stellar Field Sensor Hatch's galaxy channels, 0 below T7), with two logic gates: one AND NOT and one OR. A
 * plain-JDK program; run it with
 * {@code java -cp build/classes/java/main com.nateyburr.beyondthestars.machines.quasar.engine.QuasarRigSim [hours]
 * [seeds]}.
 * <p>
 * <b>The wiring</b> (every signal a second late, as a hatch puts it out):
 * <ul>
 * <li>Quasar disc: Disc [1] → the feed-gas trim valve (10 units/s); Disc [2] → the Space trim valve (10 units/s).</li>
 * <li>Quasar purge: (Disc [4] AND NOT Galaxy Transition [8]) OR Galaxy Transition [4] → the quasar's Burst Control
 * Hatch, which purges on the rising edge.</li>
 * <li>J1 (Eye : crown, centre 0.5, k 2): the sensor's P, inverted, window ±30; OR Galaxy Transition [4] → 15; OR, at
 * T6, Slots [8] → 15 (the Stability Link's test fire: everything to the Eye, F-6).</li>
 * <li>J2 (high : low, centre 0.5, k 2): the sensor's W; J3 (halo : rail, centre 0.2, k 2): the sensor's Λ.</li>
 * <li>Eye Space: Field [1] → one binary valve, 10 units/s.</li>
 * <li>Heavy Link test fire (T7): Slots [8] → J1 to 15 and J3 to its 75 % preset.</li>
 * <li>Begin: Galaxy Transition [1] → the Eye's Burst Control Hatch in begin mode (a rising edge).</li>
 * </ul>
 * <b>All binary</b> (F-8): the three Junctions read two Directive bits each as 15 / 8 / 0 (Field [2]/[1], Field
 * [4]/[8], Events [1]/[2]); no analog signal at all (an AE2-only factory).
 * <p>
 * A failed transition is rerun as in the mod: the Magnetar Collapse from a re-merged magnetar (the core stays in
 * escrow, the Stability Link milestone is kept), the Galactic Merger from a relit Triangulum. Exit code 0 when every
 * chain reaches its tier on every seed with no Eye failure and no disc blow-out, for both wirings.
 */
public final class QuasarRigSim {

    private QuasarRigSim() {}

    /** The analog sensor window (§1.5: the rigs use ±30). */
    public static final double SENSOR_W = 30;

    /** The gate rig: Directive codes, three analog sensor wires, one AND NOT and one OR. */
    static final class Gate implements QuasarSim.GateRig {

        final boolean allBinary;
        private boolean purgeWire, beginWire;
        boolean testFire;

        Gate(boolean allBinary) {
            this.allBinary = allBinary;
        }

        /** The sensor's galaxy channel k (P, W, Λ): last second's error at T7+, else 0. */
        static double sensor(QuasarSim.World w, int k) {
            final QuasarSim.Eye e = w.eye;
            return e.s.tier.n >= 7 && e.state == QuasarSim.Eye.LIT ? e.s.prevE[k] : 0;
        }

        static boolean bit(int code, int n) {
            return DirectiveSet.isSet(code, n);
        }

        @Override
        public void beginTick(QuasarSim.World w) {
            // (Disc [4] AND NOT Transition [8]) OR Transition [4] → the quasar's Burst Control Hatch, rising edge
            final boolean wire = (bit(w.dirDisc, 3) && !bit(w.dirTrans, 4)) || bit(w.dirTrans, 3);
            if (wire && !purgeWire) w.q.requestPurge();
            purgeWire = wire;
            testFire = bit(w.dirSlots, 4);
        }

        /** Galaxy Transition [1] → begin mode (a rising edge); returns true on the edge. */
        boolean beginEdge(QuasarSim.World w) {
            final boolean wire = bit(w.dirTrans, 1);
            final boolean edge = wire && !beginWire;
            beginWire = wire;
            return edge;
        }

        @Override
        public double[] discTrims(QuasarSim.World w) {
            return new double[] { bit(w.dirDisc, 1) ? 10 : 0, bit(w.dirDisc, 2) ? 10 : 0 };
        }

        @Override
        public double eyeSpace(QuasarSim.World w) {
            return bit(w.dirField, 1) ? QuasarSim.SPACE_ON : 0;
        }

        @Override
        public int j1(QuasarSim.World w) {
            if (bit(w.dirTrans, 3) || (testFire && w.eye.s.tier.n == 6)) return 15; // the OR
            return allBinary ? BurstRouting.twoLevel(bit(w.dirField, 2), bit(w.dirField, 1))
                : BurstRouting.level(-sensor(w, StellarField.P), SENSOR_W);
        }

        @Override
        public int j2(QuasarSim.World w) {
            return allBinary ? BurstRouting.twoLevel(bit(w.dirField, 3), bit(w.dirField, 4))
                : BurstRouting.level(sensor(w, StellarField.W), SENSOR_W);
        }

        @Override
        public int j3(QuasarSim.World w) {
            return allBinary ? BurstRouting.twoLevel(bit(w.dirEvent, 1), bit(w.dirEvent, 2))
                : BurstRouting.level(sensor(w, StellarField.L), SENSOR_W);
        }

        @Override
        public boolean testFire(QuasarSim.World w) {
            return testFire;
        }
    }

    /** One chain run's tally. */
    static final class Run {

        boolean reached;
        long reachedAt = -1, eyeFails, transFails, blowOuts, secs;
        long[] zonesAfter = new long[5];
        double ySumAfter;
        long yNAfter;
        String note = "";
    }

    /** Runs one chain: from {@code tier} (6 or 7) to the next, unattended, on the gate rig. */
    static Run chain(int tier, long seed, long secs, boolean allBinary, double M0) {
        final QuasarSim.Cfg c = new QuasarSim.Cfg();
        c.seed = seed;
        c.shock = 120; // F-2's recommended shock (the prototype's default is 80)
        if (tier == 6) {
            c.unlocked7 = false;
            c.coreGM = false; // no Supermassive Seed Core yet: no Heavy Link test fire right after the collapse
            c.bufGM = false;
        }
        final QuasarSim.QCfg qc = new QuasarSim.QCfg();
        qc.M0 = M0;
        final QuasarSim.KCfg k = new QuasarSim.KCfg();
        final QuasarSim.World w = new QuasarSim.World(tier, c, qc, k, new QuasarSim.Rig());
        final Gate gate = new Gate(allBinary);
        w.gate = gate;
        final QuasarSim.Eye e = w.eye;
        final Run r = new Run();
        final int target = tier + 1;
        for (long i = 0; i < secs; i++) {
            if (gate.beginEdge(w) && e.trans == null) {
                if (e.s.tier.n == 6) e.beginMC();
                else if (e.s.tier.n == 7) e.beginGM();
            }
            final boolean wasTrans = e.trans != null;
            final long failsBefore = e.L.fails, blowBefore = w.QL.fails;
            w.tick();
            r.blowOuts += w.QL.fails - blowBefore;
            if (e.L.fails > failsBefore) {
                r.eyeFails += e.L.fails - failsBefore;
                if (wasTrans) {
                    // the mod: the core stays in escrow; rerun from the lit object below
                    r.transFails++;
                    e.s.tier = TierParams.of(tier);
                }
            }
            if (e.state == QuasarSim.Eye.DARK) e.relight();
            final boolean unlocked = target == 7 ? c.unlocked7 : c.unlocked8;
            if (unlocked && !r.reached) {
                r.reached = true;
                r.reachedAt = e.s.t;
                for (int z = 0; z < 5; z++) r.zonesAfter[z] = -e.L.zoneT[z];
                r.ySumAfter = -e.L.ySum;
                r.yNAfter = -e.L.yN;
            }
        }
        if (r.reached) { // the lit seconds after the unlock (the ledger's totals since then)
            for (int z = 0; z < 5; z++) r.zonesAfter[z] += e.L.zoneT[z];
            r.ySumAfter += e.L.ySum;
            r.yNAfter += e.L.yN;
        }
        r.secs = secs;
        if (!r.reached) r.note = "link "
            + (tier == 6 ? w.link.done + " best " + w.link.best : w.link8.done + " best " + w.link8.best)
            + ", trans "
            + (e.trans != null);
        return r;
    }

    static String fmt(Run r) {
        long n = 0;
        for (long z : r.zonesAfter) n += z;
        final StringBuilder zs = new StringBuilder();
        for (int i = 0; i < 4; i++) zs.append(i > 0 ? "/" : "")
            .append(n > 0 ? Math.round(100.0 * r.zonesAfter[i] / n) : 0);
        return String.format(
            Locale.ROOT,
            "%s at %s  Eye fails %d (transitions %d)  blow-outs %d  after: zones %s  Y %.2f %s",
            r.reached ? "REACHED" : "NOT REACHED",
            r.reachedAt < 0 ? "-" : String.format(Locale.ROOT, "%d:%02d", r.reachedAt / 3600, r.reachedAt % 3600 / 60),
            r.eyeFails,
            r.transFails,
            r.blowOuts,
            zs,
            r.yNAfter > 0 ? r.ySumAfter / r.yNAfter : 0,
            r.note);
    }

    public static void main(String[] args) {
        final double hours = args.length > 0 ? Double.parseDouble(args[0]) : 4;
        final int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        final long secs = (long) (hours * 3600);
        boolean ok = true;
        for (boolean binary : new boolean[] { false, true }) {
            System.out.println(
                binary ? "== All binary: Junctions on two Directive bits each (15 / 8 / 0), no analog signal"
                    : "== Directive bits + three analog sensor wires (P inverted, W, L at +-30)");
            for (int tier = 6; tier <= 7; tier++) {
                final double m0 = tier == 6 ? 64 : 120;
                int reached = 0;
                for (long seed = 1; seed <= seeds; seed++) {
                    final Run r = chain(tier, seed, secs, binary, m0);
                    System.out
                        .printf(Locale.ROOT, "  T%d -> T%d  %3.0f SM  seed %d  %s%n", tier, tier + 1, m0, seed, fmt(r));
                    if (r.reached) reached++;
                    if (!r.reached || r.eyeFails > 0 || r.blowOuts > 0) ok = false;
                }
                System.out.printf(Locale.ROOT, "  T%d -> T%d: %d of %d reached%n", tier, tier + 1, reached, seeds);
            }
        }
        System.out.println(
            ok ? "RESULT: PASS (every chain reached its tier, 0 Eye failures, 0 disc blow-outs)" : "RESULT: FAIL");
        System.exit(ok ? 0 : 1);
    }
}
