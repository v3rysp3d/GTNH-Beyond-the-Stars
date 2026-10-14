package com.nateyburr.beyondthestars.machines.quasar.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;

/**
 * SMBH v1: the offline runner and golden test of the Supermassive Lattice (smbh-v1.md §2-§5, §9, §10.1; the B-37
 * discipline). A plain-JDK program (no Minecraft, GT or library dependency).
 *
 * <pre>
 * java -cp &lt;classes&gt; com.nateyburr.beyondthestars.machines.quasar.engine.SmbhSim golden &lt;smbh-golden.json&gt;
 * java -cp &lt;classes&gt; ...SmbhSim run '&lt;spec json&gt;'      (one case, its result tree as JSON)
 * java -cp &lt;classes&gt; ...SmbhSim tables                  (the spec's tables as this engine computes them)
 * </pre>
 *
 * tools/smbh-golden.js writes the golden file from the reference block (tools/fixtures/smbh-engine.js) and runs this
 * with {@code --java}. {@link #runCase} is a port of its harness, second for second: the disc rig and purge regulator
 * of the P4 prototype, {@link HorizonLattice#plan}, {@link QuasarDisc#step}, {@link HorizonLattice#step}, with the
 * case's supplies. {@link #tables} ports {@code smbhTables}. Every number must match bit for bit.
 */
public final class SmbhSim {

    private SmbhSim() {}

    /** The case's quasar and supplies (the harness's {@code __qc} defaults). */
    public static final class QC {

        public double M0 = 4096, b = 3, f = 1, purgeAt = 40, setpoint, overdrive, intake, od = 1, uni = -1, anchor;
        public int coil = 4, lattice;
        public QuasarGas gas = QuasarGas.HE;
        public boolean stocked = true, power = true, unbound;

        /** Overrides the fields present in {@code o} (a case's {@code qc}, or an event's). */
        void apply(Map<String, Object> o) {
            M0 = FieldSim.num(o, "M0", M0);
            b = FieldSim.num(o, "b", b);
            f = FieldSim.num(o, "f", f);
            purgeAt = FieldSim.num(o, "purgeAt", purgeAt);
            setpoint = FieldSim.num(o, "setpoint", setpoint);
            overdrive = FieldSim.num(o, "overdrive", overdrive);
            intake = FieldSim.num(o, "intake", intake);
            od = FieldSim.num(o, "od", od);
            uni = FieldSim.num(o, "uni", uni);
            anchor = FieldSim.num(o, "anchor", anchor);
            coil = (int) FieldSim.num(o, "coil", coil);
            lattice = (int) FieldSim.num(o, "lattice", lattice);
            if (o.get("gas") instanceof String) gas = QuasarGas.parse((String) o.get("gas"));
            stocked = QuasarSim.bool(o, "stocked", stocked);
            power = QuasarSim.bool(o, "power", power);
            unbound = QuasarSim.bool(o, "unbound", unbound);
        }
    }

    /** The harness's {@code __inputs}: the disc's draw, the rig's trims and purge, the settings. */
    static QuasarInputs inputs(QuasarState q, QC c, boolean rig) {
        final QuasarInputs in = new QuasarInputs();
        final double medd = QuasarParams.eddingtonFeed(q.M, c.gas);
        in.gas = c.gas;
        in.coilTier = c.coil;
        in.bSetpoint = c.b;
        in.powerOk = c.power;
        in.gasDrawn = c.stocked ? c.f * medd : 0;
        double uG = 0, uS = 0;
        if (rig) {
            if (q.lagE < -5) uG = 10;
            else if (q.lagE > 5) uS = 10;
        }
        in.gasTrimUnits = uG;
        in.spaceTrimUnits = uS;
        in.regulatorPurge = rig && c.purgeAt > 0 && q.lagPhi >= c.purgeAt;
        in.lattice = c.lattice;
        in.massSetpoint = c.setpoint;
        in.unbound = c.unbound;
        in.overdrive = c.overdrive;
        return in;
    }

    /** Runs one case exactly like tools/smbh-golden.js's {@code __run} and returns its result tree. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> runCase(Map<String, Object> spec) {
        final long secs = (long) FieldSim.num(spec, "secs", 3600);
        final long seed = (long) FieldSim.num(spec, "seed", 1);
        final boolean rig = !Boolean.FALSE.equals(spec.get("rig"));
        final QC c = new QC();
        c.apply(QuasarSim.sub(spec, "qc"));
        final List<Object> events = spec.get("events") instanceof List ? (List<Object>) spec.get("events")
            : new ArrayList<Object>();
        final QuasarState q = new QuasarState(QuasarState.rngFor(seed));
        q.seed(c.M0, QuasarParams.aEq(Math.min(c.f, 1), Math.min(c.b, c.coil)));
        double bu = 0, gas = 0, hold = 0, growth = 0, mhd = 0, uni = 0, anchor = 0, leaked = 0, grown = 0, xSum = 0;
        double maxM = q.M, minM = q.M;
        long shedS = 0, leakS = 0, anchorShortS = 0, fails = 0, eruptions = 0, purges = 0;
        final long[] zoneT = new long[5];
        int hq = FieldSim.FNV_OFFSET;
        for (long i = 0; i < secs; i++) {
            for (Object e : events) {
                final Map<String, Object> ev = (Map<String, Object>) e;
                if ((long) FieldSim.num(ev, "t", -1) == i) c.apply(QuasarSim.sub(ev, "qc"));
            }
            final double medd = QuasarParams.eddingtonFeed(q.M, c.gas);
            final QuasarInputs in = inputs(q, c, rig);
            in.anchorAvailable = c.anchor;
            final HorizonLattice.Plan p = HorizonLattice.plan(q, in);
            in.intakeAvailable = Math.max(0, c.intake * medd - in.gasDrawn);
            in.mhdcsmAvailable = c.od * p.overdriveNeed;
            in.universiumAvailable = (c.uni >= 0 ? c.uni : c.od) * p.overdriveNeed;
            final QuasarResult r = QuasarDisc.step(q, in);
            HorizonLattice.step(q, in, p, r);
            bu += r.G;
            gas += in.gasDrawn;
            hold += r.holdUpLitres;
            growth += r.growthLitres;
            mhd += r.mhdcsmLitres;
            uni += r.universiumLitres;
            anchor += r.anchorLitres;
            if (r.anchorShort) anchorShortS++;
            leaked += r.leakedMass;
            grown += r.grownMass;
            xSum += r.outputX;
            if (r.shedding) shedS++;
            if (r.leaking) leakS++;
            if (r.blowOut) fails++;
            if (r.eruption) eruptions++;
            if (r.purge) purges++;
            if (r.zone >= 0) zoneT[r.zone]++;
            if (q.M > maxM) maxM = q.M;
            if (q.M < minM) minM = q.M;
            for (double v : new double[] { q.E, q.Phi, q.a, q.M / QuasarParams.M_REF, q.G, q.I, q.oEff })
                hq = FieldSim.fnv(hq, FieldSim.q6(v));
            hq = FieldSim.fnv(hq, q.zone);
            hq = FieldSim.fnv(hq, q.reform);
            hq = FieldSim.fnv(hq, q.hole == QuasarState.Hole.OPEN ? 0 : 1);
            hq = FieldSim.fnv(hq, q.shed ? 1 : 0);
        }
        final LinkedHashMap<String, Object> L = new LinkedHashMap<>();
        L.put("bu", bu);
        L.put("gas", gas);
        L.put("hold", hold);
        L.put("growth", growth);
        L.put("mhdcsm", mhd);
        L.put("universium", uni);
        L.put("anchor", anchor);
        L.put("shedS", (double) shedS);
        L.put("leakS", (double) leakS);
        L.put("anchorShortS", (double) anchorShortS);
        L.put("leaked", leaked);
        L.put("grown", grown);
        L.put("maxM", maxM);
        L.put("minM", minM);
        L.put("xSum", xSum);
        L.put("fails", (double) fails);
        L.put("eruptions", (double) eruptions);
        L.put("purges", (double) purges);
        L.put("zoneT", QuasarSim.longs(zoneT));
        final LinkedHashMap<String, Object> res = new LinkedHashMap<>();
        res.put("M", q.M);
        res.put("a", q.a);
        res.put("E", q.E);
        res.put("Phi", q.Phi);
        res.put("I", q.I);
        res.put("open", q.hole == QuasarState.Hole.OPEN);
        res.put("reform", (double) q.reform);
        res.put("oEff", q.oEff);
        res.put("shed", q.shed);
        res.put("L", L);
        res.put("hash", (double) (hq & 0xFFFFFFFFL));
        return res;
    }

    // ================================================================== the spec's tables (smbhTables)

    /** The spec's assumed efficiency for its growth times (local work folder smbh/v3.js): η 0.0821. */
    public static final double TABLE_ETA = 0.0821;
    /** §9's practical ×1: the max-quasar bench's 9,796 Bu/t; one Space Elevator Pump III of Hydrogen, L/t. */
    public static final double X1_BU = 9796, PUMP3_H = 2.007e7;

    /**
     * §2.3's idealised growth time from 4,096 SM to {@code target}, hours (−1: never), at total intake {@code F}
     * (× Ṁ_Edd) on {@code gas} with lattice {@code band} installed: λc 1, f 1, η fixed, 60 s steps on ln M.
     */
    public static double growthHours(double target, double F, QuasarGas gas, int band) {
        final double dt = 60, lt = StrictMath.log(target);
        double lnM = StrictMath.log(QuasarParams.M_REF), T = 0;
        while (lnM < lt) {
            final double M = StrictMath.exp(lnM), medd = QuasarParams.eddingtonFeed(M, gas);
            final double x = QuasarParams.bandX(M, QuasarParams.LATTICE_COIL, band);
            final double safe = band > 0 ? QuasarParams.LATTICE_SAFE[band] : 0;
            final double disc = Math.min(1, F), hold = QuasarParams.HOLDUP_K * x, avail = Math.max(0, F - disc);
            final double holdPaid = Math.min(avail, hold), ch = hold > 0 ? holdPaid / hold : 1;
            final double grow = Math.max(0, Math.min(avail - holdPaid, QuasarParams.INTAKE_CAP - disc - hold));
            double d = QuasarParams.GROWTH * (1 - TABLE_ETA) * (disc + grow) * medd / M;
            if (band > 0 && M > safe)
                d -= (1 - ch) * StrictMath.log(M / safe) * QuasarParams.LN2 / QuasarParams.LEAK_HALF_LIFE;
            if (d <= 1e-12) return -1;
            lnM += d * dt;
            T += dt;
            if (T > 1e8) return -1;
        }
        return T / 3600;
    }

    /** One Unbound doubling at the 5× cap (disc 1 + growth 1 = 2× the Eddington rate), hours. */
    public static double doublingHours(QuasarGas gas) {
        return QuasarParams.LN2
            / (QuasarParams.GROWTH * (1 - TABLE_ETA)
                * QuasarParams.eddingtonFeed(1, gas)
                * (QuasarParams.INTAKE_CAP - QuasarParams.HOLDUP_K))
            / 3600;
    }

    private static List<Object> row(double... v) {
        final List<Object> l = new ArrayList<>();
        for (double d : v) l.add(d);
        return l;
    }

    /** The spec's tables (§2.3, §3.2, §5.2, §9) as this engine computes them; the reference's {@code smbhTables}. */
    public static Map<String, Object> tables() {
        final LinkedHashMap<String, Object> t = new LinkedHashMap<>();
        final double[][] bands = { { 65536, 1 }, { 65536, 2 }, { 2097152, 3 }, { 4194304 * 0.999, 3 }, { 4194304, 4 } };
        final List<Object> he = new ArrayList<>();
        for (int F = 1; F <= 5; F++) {
            final double[] r = new double[bands.length];
            for (int j = 0; j < bands.length; j++) r[j] = growthHours(bands[j][0], F, QuasarGas.HE, (int) bands[j][1]);
            he.add(row(r));
        }
        t.put("growthHe", he);
        final QuasarGas[] gases = { QuasarGas.H, QuasarGas.HE, QuasarGas.RSM };
        final List<Object> cap = new ArrayList<>(), perSM = new ArrayList<>();
        for (QuasarGas g : gases) {
            cap.add(
                row(
                    growthHours(65536, 5, g, 1),
                    growthHours(4194304, 5, g, 4),
                    growthHours(67108864 * 0.999, 5, g, 4),
                    doublingHours(g)));
            perSM.add(
                row(
                    QuasarParams.eddingtonFeed(1, g) / 20,
                    QuasarParams.INTAKE_CAP * QuasarParams.eddingtonFeed(1, g) / 20));
        }
        t.put("growthCap", cap);
        t.put("perSM", perSM);
        final List<Object> od = new ArrayList<>();
        for (double o : new double[] { 0, 0.25, 0.5, 0.75, 1 }) od.add(
            row(
                QuasarParams.overdriveUpkeep(o, 0),
                QuasarParams.outputX(262144, o),
                QuasarParams.outputX(4194304, o),
                QuasarParams.outputX(67108864, o)));
        t.put("overdrive", od);
        final List<Object> ub = new ArrayList<>();
        for (double n : new double[] { 0, 0.5, 1, 1.5, 2, 2.5, 3, 4 }) {
            final double odn = QuasarParams.overdriveUpkeep(1, n), a = QuasarParams.anchorUpkeep(n);
            ub.add(
                row(
                    QuasarParams.LATTICE_TOP[4] * QuasarParams.powE(2, n),
                    a,
                    odn,
                    2 * odn + a,
                    2 * QuasarParams.overdriveUpkeep(0.5, n) + a));
        }
        t.put("unbound", ub);
        final double[][] rows = { { 4096, 0 }, { 16384, 1 }, { 65536, 1 }, { 262144, 3 }, { 2097152, 3 },
            { 4194304, 3 }, { 16777216, 4 }, { 67108864, 4 }, { 94906266, 4 }, { 134217728, 4 }, { 189812531, 4 },
            { 268435456, 4 }, { 536870912, 4 } };
        final List<Object> t9 = new ArrayList<>();
        for (double[] rw : rows) {
            final double M = rw[0];
            final int L = (int) rw[1];
            final double medd = QuasarParams.eddingtonFeed(M, QuasarGas.H);
            final double x = QuasarParams.bandX(M, QuasarParams.LATTICE_COIL, L);
            final double n = L == 4 ? QuasarParams.unboundN(M) : 0;
            final double hold = (1 + QuasarParams.HOLDUP_K * x) * medd / 20;
            final double xs = QuasarParams.outputX(M, 0), xo = QuasarParams.outputX(M, 1);
            t9.add(
                row(
                    M,
                    L,
                    hold,
                    QuasarParams.INTAKE_CAP * medd / 20,
                    hold / PUMP3_H,
                    xs,
                    xs * X1_BU * 1e13,
                    xo,
                    xo * X1_BU * 1e13,
                    QuasarParams.overdriveUpkeep(1, n),
                    QuasarParams.anchorUpkeep(n)));
        }
        t.put("table9", t9);
        return t;
    }

    // ================================================================== invariants (no reference needed)

    /**
     * The engine's own promises, checked over a grid: X never passes ×100 and never falls as M or o rises; the
     * mass term is continuous at 4,096 SM; the new G0 equals today's at or under 4,096 SM with o 0, bit for bit;
     * every Mk's cap in EU/t stays under Long.MAX; the crown's wireless BigInteger matches floor(EU/t)·20; the
     * anchor's n round-trips. Returns the failures.
     */
    public static List<String> invariants() {
        final List<String> bad = new ArrayList<>();
        double prev = 0;
        for (int k = 0; k <= 400; k++) {
            final double M = 64 * QuasarParams.powE(2, k / 10.0);
            if (M > QuasarParams.UNBOUND_MAX) break;
            double prevO = -1;
            for (double o = 0; o <= 1.0000001; o += 0.05) {
                final double X = QuasarParams.outputX(M, o);
                if (!(X < QuasarParams.X_CEIL)) bad.add("X(" + M + ", " + o + ") = " + X + " is not under 100");
                if (X < prevO) bad.add("X falls with o at M " + M);
                prevO = X;
            }
            final double X0 = QuasarParams.outputX(M, 0);
            if (X0 < prev) bad.add("X falls with M at " + M);
            prev = X0;
            if (M <= QuasarParams.M_REF) {
                for (double a : new double[] { 0.3, 0.5, 0.9 }) {
                    final double old = QuasarParams.g0(0.97, a, 2.75, M), now = QuasarParams.g0(0.97, a, 2.75, M, 0);
                    if (Double.doubleToLongBits(old) != Double.doubleToLongBits(now))
                        bad.add("G0 changed at " + M + " SM: " + old + " vs " + now);
                }
            }
        }
        final double below = QuasarParams.massTerm(Math.nextDown(QuasarParams.M_REF)),
            above = QuasarParams.massTerm(Math.nextUp(QuasarParams.M_REF));
        if (Math.abs(above - below) > 1e-12) bad.add("the mass term jumps at 4,096 SM: " + below + " / " + above);
        for (int mk = 1; mk <= BurstRouting.MK_MAX; mk++) {
            final double eut = BurstRouting.crownEUt(Math.max(BurstRouting.crownCap(mk), BurstRouting.throatCap(mk)));
            if (!(eut < Long.MAX_VALUE)) bad.add("Mk " + mk + " cap " + eut + " EU/t passes Long.MAX");
        }
        final double peak = BurstRouting.crownCap(4);
        if (!BurstRouting.crownEUPerSecond(peak)
            .equals(
                java.math.BigInteger.valueOf((long) Math.floor(BurstRouting.crownEUt(peak)))
                    .multiply(java.math.BigInteger.valueOf(20))))
            bad.add("crownEUPerSecond differs from floor(EU/t)·20 at the Mk IV cap");
        if (BurstRouting.crownEUPerSecond(1e30)
            .signum() <= 0) bad.add("crownEUPerSecond overflows at 10^30 Bu/t");
        // two Mk IV carry the ×100 mean (§3.4): 9,796 × X(any M, 1) ≤ 2 × 524,288
        if (X1_BU * QuasarParams.outputX(QuasarParams.UNBOUND_MAX, 1) > 2 * BurstRouting.crownCap(4))
            bad.add("two Mk IV crowns do not carry the ×100 mean");
        for (double n : new double[] { 0.25, 0.5, 1, 1.5, 2, 3 }) {
            final double back = QuasarParams.anchorDoublings(QuasarParams.anchorUpkeep(n));
            if (Math.abs(back - n) > 1e-9) bad.add("anchor n " + n + " round-trips to " + back);
        }
        return bad;
    }

    // ================================================================== golden

    @SuppressWarnings("unchecked")
    static int golden(String file) throws IOException {
        final String text = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        final Map<String, Object> root = (Map<String, Object>) new FieldSim.Json(text).value();
        final List<Object> cases = (List<Object>) root.get("cases");
        int pass = 0, fail = 0;
        final long t0 = System.nanoTime();
        for (Object o : cases) {
            final Map<String, Object> cs = (Map<String, Object>) o;
            final String name = (String) cs.get("name");
            final List<String> diffs = new ArrayList<>();
            FieldSim.compare(cs.get("expect"), runCase((Map<String, Object>) cs.get("spec")), "", diffs);
            if (diffs.isEmpty()) {
                pass++;
                System.out.println("PASS " + name);
            } else {
                fail++;
                System.out.println("FAIL " + name);
                for (String d : diffs) System.out.println("    " + d);
            }
        }
        final List<String> td = new ArrayList<>();
        FieldSim.compare(root.get("tables"), tables(), "tables", td);
        if (td.isEmpty()) {
            pass++;
            System.out.println("PASS the spec's tables (§2.3, §3.2, §5.2, §9)");
        } else {
            fail++;
            System.out.println("FAIL the spec's tables");
            for (String d : td) System.out.println("    " + d);
        }
        final List<String> inv = invariants();
        if (inv.isEmpty()) {
            pass++;
            System.out
                .println("PASS invariants (X under 100 and monotone, G0 unchanged to 4,096 SM, no long overflow)");
        } else {
            fail++;
            System.out.println("FAIL invariants");
            for (String d : inv) System.out.println("    " + d);
        }
        System.out
            .printf("%d checks: %d PASS, %d FAIL (%.0f ms)%n", pass + fail, pass, fail, (System.nanoTime() - t0) / 1e6);
        return fail == 0 && pass > 0 ? 0 : 1;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        if (args.length >= 2 && "golden".equals(args[0])) {
            System.exit(golden(args[1]));
        } else if (args.length >= 2 && "run".equals(args[0])) {
            final Map<String, Object> spec = (Map<String, Object>) new FieldSim.Json(args[1]).value();
            System.out.println(FieldSim.Json.write(runCase(spec)));
        } else if (args.length >= 1 && "tables".equals(args[0])) {
            System.out.println(FieldSim.Json.write(tables()));
        } else {
            System.out.println("usage: SmbhSim golden <smbh-golden.json> | run '<spec json>' | tables");
            System.exit(2);
        }
    }
}
