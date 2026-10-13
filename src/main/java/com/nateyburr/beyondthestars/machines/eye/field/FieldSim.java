package com.nateyburr.beyondthestars.machines.eye.field;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D-49 stability field: the offline runner (d49-build-plan.md §15.1). A plain-JDK program: the field package has no
 * Minecraft, GT or library dependency, so it compiles and runs with javac + java alone.
 *
 * <pre>
 * java -cp &lt;classes&gt; com.nateyburr.beyondthestars.machines.eye.field.FieldSim golden &lt;fieldsim-golden.json&gt;
 * java -cp &lt;classes&gt; ...FieldSim run &lt;tier&gt; &lt;off|bang|fine&gt; &lt;seed&gt; &lt;secs&gt; [m tt ts r feed]
 * java -cp &lt;classes&gt; ...FieldSim collapse &lt;spaceTimeL&gt; &lt;shock&gt; [seed secs]
 * java -cp &lt;classes&gt; ...FieldSim invariant
 * </pre>
 *
 * tools/fieldsim-golden.js writes the golden file from the prototype engine and runs this (its {@code --java} flag).
 * {@code golden} replays every case through {@link Proto}, a port of the prototype's harness (newState, light,
 * chargeCycle, fail, relight and its ledger) around {@link StellarField}, and demands identical numbers: every
 * count, every ledger total, the final state and an FNV-1a hash of the gauges, A, I, zone, state, tier and lock
 * every second. Exit code 0 when every case prints PASS.
 */
public final class FieldSim {

    private FieldSim() {}

    // ================================================================== the prototype harness

    /** Prototype {@code RECIPE_UMV}. */
    public static final double RECIPE_UMV = 125829120;

    /** One case's configuration (prototype {@code CFG}). */
    public static final class Cfg {

        public long seed = 1806;
        public FieldRig.Mode rig = FieldRig.Mode.FINE;
        public double m = 1, r = 1, feed = 0.694;
        public int tt = 1, ts = 1;
        public double tachRate = 100, stBuf;
        public boolean core = true, hsm = true, unlocked4;
        /** Collapse cases: the bounce shock, and the first field second at which the collapse may begin. */
        public double shock = CollapseOverlay.PROTOTYPE_BOUNCE_SHOCK;
        public boolean collapse;
        public long beginAt = 600;
        /**
         * AUTOMATION PASS: a rig that replaces the prototype's ({@link #rig}) when set - the Directive gate rig of
         * {@link DirectiveRigSim}. Null in every golden case.
         */
        public java.util.function.BiFunction<TierParams, FieldState, LinkedHashMap<String, Double>> rigFn;
    }

    /** The prototype's ledger ({@code newLedger}). Map keys are the prototype's display names. */
    public static final class Ledger {

        public final LinkedHashMap<String, Double> up = new LinkedHashMap<>();
        public final LinkedHashMap<String, Double> prod = new LinkedHashMap<>();
        public final LinkedHashMap<String, Double> trims = new LinkedHashMap<>();
        public double eu, tach, ySum;
        public long charges, fails, cycles, batches, yN;
        public final long[] zoneT = new long[5];
    }

    public static void add(Map<String, Double> o, String k, double v) {
        final Double old = o.get(k);
        o.put(k, (old == null ? 0 : old) + v);
    }

    /** Prototype upkeep per cycle ({@code TIERS[n].up}); the mod's real table is the runtime's (plan §6.1). */
    static String[] upNames(int n) {
        switch (n) {
            case 1:
                return new String[] { "Hydrogen", "Helium" };
            case 2:
                return new String[] { "Hydrogen", "Helium", "Raw Star Matter" };
            case 3:
                return new String[] { "Hydrogen", "Helium", "Excited Stellar Plasma" };
            case 4:
                return new String[] { "Molten SpaceTime" };
            case 5:
                return new String[] {};
            default:
                return new String[] { "White Dwarf Matter", "Black Dwarf Matter" };
        }
    }

    static double[] upAmounts(int n) {
        switch (n) {
            case 1:
                return new double[] { 5e7, 5e7 };
            case 2:
                return new double[] { 1.25e7, 1.25e7, 25000 };
            case 3:
                return new double[] { 1.25e7, 1.25e7, 1250 };
            case 4:
                return new double[] { 144 };
            case 5:
                return new double[] {};
            default:
                return new double[] { 60, 15 };
        }
    }

    static final String[] PROD_NAME = { "", "Raw Star Matter", "Excited Stellar Plasma",
        "Raw Hyperdense Stellar Plasma", "White Dwarf Matter", "Black Dwarf Matter", "Magnetar Matter" };

    /** The prototype harness: one star, its state machine, its ledger. */
    public static final class Proto {

        public static final int LIT = 0, IGNITING = 1, DARK = 2;

        public final Cfg c;
        public final FieldState s;
        public final Ledger L = new Ledger();
        public int state = LIT;
        public int zone;
        public CollapseOverlay trans;
        private final StepInputs in = new StepInputs();

        /** Prototype {@code newState(tier, cfg)}: the star is lit at once. */
        public Proto(int tier, Cfg c) {
            this.c = c;
            this.s = new FieldState(TierParams.of(tier), new FieldRng(c.seed));
            light();
        }

        void light() {
            state = LIT;
            StellarField.light(s, c.m, c.tt);
            if (s.tier.batch) startBatch();
            chargeCycle();
        }

        void startBatch() {
            StellarField.startBatch(s);
            add(L.up, "White Dwarf Matter", 18432); // TIERS[5].batchUp
        }

        void chargeCycle() {
            final int n = s.tier.n;
            final String[] names = upNames(n);
            final double[] amts = upAmounts(n);
            for (int i = 0; i < names.length; i++) add(L.up, names[i], amts[i] * c.r);
            L.eu += RECIPE_UMV * Math.pow(4, n - 1) * 1200;
        }

        void fail(String reason) {
            final TierParams T = s.tier;
            L.fails++;
            if (T.batch && trans == null) {
                final double frac = s.batch.eyeT / 7200, y = 4608 * 0.25 * frac;
                add(L.prod, PROD_NAME[T.n], y);
                s.I = 100;
                s.lock = 0;
                startBatch();
                return;
            }
            trans = null;
            state = DARK;
            StellarField.resetLost(s);
        }

        /** Prototype {@code relight(S)}. */
        public void relight() {
            final TierParams T = s.tier;
            if (T.ignite) {
                state = IGNITING;
                StellarField.beginIgnition(s);
                L.charges++;
                final double seed = 1e8 * Math.pow(2, T.n - 1);
                add(L.up, "Hydrogen", seed);
                add(L.up, "Helium", seed);
                L.eu += 10 * RECIPE_UMV * Math.pow(4, T.n - 1) * 1200;
            } else if (T.n == 4) {
                s.setX(0, 60, 30, 0);
                light();
            } else if (T.n == 6) {
                s.setX(0, 0, 0, 0);
                light();
                add(L.up, "White Dwarf Matter", 18432);
                add(L.up, "Black Dwarf Matter", 4608);
                add(L.up, "Antipolarization Agent", 100000);
            }
        }

        /** Prototype {@code canBegin()} (the collapse button). */
        public boolean canBegin() {
            return s.tier.n == 3 && state == LIT
                && trans == null
                && zone >= 0
                && zone <= 1
                && c.core
                && c.hsm
                && c.tachRate > 0;
        }

        /** Prototype {@code beginCollapse(S)}. */
        public void beginCollapse() {
            trans = new CollapseOverlay(c.shock);
        }

        /** Prototype {@code step(S)}. */
        public void step() {
            if (state == DARK) {
                zone = -1;
                StellarField.stepDark(s);
                return;
            }
            if (state == IGNITING) {
                zone = -1;
                if (StellarField.stepIgniting(s, c.m, c.tt)) {
                    state = LIT;
                    chargeCycle(); // light() ran inside; T1-T3 have no batch
                }
                return;
            }
            final TierParams T = s.tier;
            final CollapseOverlay tr = trans;
            final double m = tr != null ? 1 : Math.min(c.m, StellarField.mMax(T, c.tt));
            in.m = m;
            in.q = StepInputs.qFor(c.ts);
            in.r = c.r;
            in.p = 1;
            in.feed = c.feed;
            in.dialSetpoint = c.m;
            in.tt = c.tt;
            FieldRig
                .toUnits(T, c.rigFn != null ? c.rigFn.apply(T, s) : FieldRig.ctrl(T, s.prevE, s.prevWarn, c.rig), in.u);
            if (tr != null) {
                tr.dose = c.tachRate;
                tr.spaceTimeBuffer = c.stBuf;
                tr.hsmSeedPresent = true;
            }
            final StepResult out = StellarField.step(s, tr != null ? tr : Overlay.NONE, in);

            // ledger (prototype: before the failure checks)
            for (Map.Entry<TrimDef, Double> e : in.u.entrySet())
                add(L.trims, e.getKey().kind.key, e.getValue() * e.getKey().kind.unitLitres);
            if (m > 1) L.tach += ((m - 1) / 0.25) * 10 * Math.pow(2, T.n - 1);
            L.zoneT[out.zone]++;
            L.ySum += out.Y;
            L.yN++;
            zone = out.zone;

            if (out.fail != null) {
                // Breach / Integrity 0 (before the transition) or the settle failure (phase 3, no dose then)
                fail(out.fail);
                return;
            }
            L.tach += out.timeDosed;
            if (out.bounced) {
                c.stBuf -= out.spaceTimeTaken;
                add(L.up, "Molten SpaceTime", out.spaceTimeTaken);
                add(L.up, "Hyperdense Stellar Mixture", out.hsmSeedTaken);
                c.hsm = false;
            }
            if (out.supernova) {
                L.fails++;
                trans = null;
                state = DARK;
                return;
            }
            if (out.relit) chargeCycle();
            if (out.transitionSuccess) {
                trans = null;
                c.unlocked4 = true;
                c.core = false;
            }
            final TierParams Tn = s.tier;
            if (Tn.feed) add(L.up, "Hyperdense Stellar Mixture", out.hsmToDraw);
            if (out.cycleEnded) {
                if (out.hasCycleProduct) add(L.prod, PROD_NAME[Tn.n], out.cycleProduct);
                L.cycles++;
                chargeCycle();
            }
            if (out.batchDone) {
                add(L.prod, PROD_NAME[Tn.n], out.batchProduct);
                L.batches++;
                startBatch();
            }
        }
    }

    // ================================================================== one golden case

    public static final int FNV_OFFSET = 0x811c9dc5, FNV_PRIME = 16777619;

    public static int fnv(int h, int v) {
        for (int i = 0; i < 4; i++) {
            h ^= (v >>> (8 * i)) & 255;
            h *= FNV_PRIME;
        }
        return h;
    }

    public static int q6(double v) {
        return (int) Math.round(v * 1e6);
    }

    /** Runs one case exactly like tools/fieldsim-golden.js's {@code runCase} and returns its result tree. */
    public static Map<String, Object> runCase(int tier, Cfg c, long secs) {
        final Proto S = new Proto(tier, c);
        int h = FNV_OFFSET;
        CollapseOverlay tr = null;
        long beginT = -1, endT = -1;
        for (long i = 0; i < secs; i++) {
            if (c.collapse && tr == null && S.s.t >= c.beginAt && S.canBegin()) {
                S.beginCollapse();
                tr = S.trans;
                beginT = S.s.t;
            }
            S.step();
            if (S.state == Proto.DARK) S.relight();
            if (tr != null && endT < 0 && S.trans != tr) endT = S.s.t;
            final FieldState s = S.s;
            h = fnv(h, q6(s.x[0]));
            h = fnv(h, q6(s.x[1]));
            h = fnv(h, q6(s.x[2]));
            h = fnv(h, q6(s.x[3]));
            h = fnv(h, q6(s.A));
            h = fnv(h, q6(s.I));
            h = fnv(h, S.zone);
            h = fnv(h, S.state);
            h = fnv(h, s.tier.n);
            h = fnv(h, s.lock);
        }
        final Ledger L = S.L;
        final LinkedHashMap<String, Object> res = new LinkedHashMap<>();
        final List<Object> zt = new ArrayList<>();
        for (long z : L.zoneT) zt.add((double) z);
        res.put("zoneT", zt);
        res.put("ySum", L.ySum);
        res.put("yN", (double) L.yN);
        res.put("meanY", L.yN > 0 ? L.ySum / L.yN : 0.0);
        res.put("fails", (double) L.fails);
        res.put("charges", (double) L.charges);
        res.put("cycles", (double) L.cycles);
        res.put("batches", (double) L.batches);
        res.put("eu", L.eu);
        res.put("tach", L.tach);
        res.put("up", new LinkedHashMap<String, Object>(L.up));
        res.put("prod", new LinkedHashMap<String, Object>(L.prod));
        res.put("trims", new LinkedHashMap<String, Object>(L.trims));
        final List<Object> xs = new ArrayList<>();
        for (int k = 0; k < 4; k++) xs.add(S.s.x[k]); // the D-49 golden lists P, Θ, Ω, Π
        res.put("x", xs);
        res.put("A", S.s.A);
        res.put("I", S.s.I);
        res.put("lock", (double) S.s.lock);
        res.put("t", (double) S.s.t);
        res.put("eyeT", S.s.eyeT);
        res.put("tier", (double) S.s.tier.n);
        res.put("state", (double) S.state);
        res.put("hash", (double) (h & 0xFFFFFFFFL));
        if (c.collapse) {
            final LinkedHashMap<String, Object> col = new LinkedHashMap<>();
            col.put("beginT", (double) beginT);
            col.put("endT", (double) endT);
            String outcome = "none";
            if (tr != null) {
                if (c.unlocked4) outcome = "settled";
                else if (tr.bounced && tr.phase == 1) outcome = "supernova";
                else if (tr.phase == 3 && S.trans != tr) outcome = "failed";
                else if (S.trans != tr) outcome = "lost";
                else outcome = "running";
                col.put("phase", (double) tr.phase);
                col.put("tach", tr.tach);
                col.put("Mc", tr.Mc);
                col.put("st", (double) tr.st);
                col.put("streak", (double) tr.streak);
                if (tr.bounced) {
                    final LinkedHashMap<String, Object> sh = new LinkedHashMap<>();
                    sh.put("P0", tr.shockP0);
                    sh.put("T0", tr.shockTh0);
                    sh.put("P", tr.shockP);
                    sh.put("Th", tr.shockTh);
                    sh.put("s", tr.absorbed);
                    col.put("shock", sh);
                } else col.put("shock", null);
            }
            col.put("outcome", outcome);
            res.put("collapse", col);
        }
        return res;
    }

    /** Builds a Cfg from a golden case's spec object. */
    static Cfg cfgOf(Map<String, Object> spec) {
        final Cfg c = new Cfg();
        c.seed = (long) num(spec, "seed", 1806);
        c.rig = FieldRig.Mode.parse((String) spec.get("rig"));
        c.m = num(spec, "m", 1);
        c.tt = (int) num(spec, "tt", 1);
        c.ts = (int) num(spec, "ts", 1);
        c.r = num(spec, "r", 1);
        c.feed = num(spec, "feed", 0.694);
        @SuppressWarnings("unchecked")
        final Map<String, Object> col = (Map<String, Object>) spec.get("collapse");
        if (col != null) {
            c.collapse = true;
            c.beginAt = (long) num(col, "beginAt", 600);
            c.stBuf = num(col, "stBuf", 0);
            c.shock = num(col, "shock", 90);
            c.tachRate = num(col, "tachRate", 100);
        }
        return c;
    }

    public static double num(Map<String, Object> o, String k, double dflt) {
        final Object v = o.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : dflt;
    }

    // ================================================================== comparison

    /** Deep-compares a golden expectation with a result tree (numbers exactly, maps by key set). */
    public static void compare(Object exp, Object act, String path, List<String> diffs) {
        if (diffs.size() > 12) return;
        if (exp == null || act == null) {
            if (exp != act) diffs.add(path + ": expected " + exp + ", got " + act);
            return;
        }
        if (exp instanceof Number && act instanceof Number) {
            final double a = ((Number) exp).doubleValue(), b = ((Number) act).doubleValue();
            if (!(a == b || (Double.isNaN(a) && Double.isNaN(b))))
                diffs.add(path + ": expected " + a + ", got " + b + " (delta " + (b - a) + ")");
            return;
        }
        if (exp instanceof Map && act instanceof Map) {
            final Map<?, ?> e = (Map<?, ?>) exp, g = (Map<?, ?>) act;
            if (!e.keySet()
                .equals(g.keySet())) diffs.add(path + ": keys " + e.keySet() + " vs " + g.keySet());
            for (Map.Entry<?, ?> en : e.entrySet()) if (g.containsKey(en.getKey()))
                compare(en.getValue(), g.get(en.getKey()), path + "." + en.getKey(), diffs);
            return;
        }
        if (exp instanceof List && act instanceof List) {
            final List<?> e = (List<?>) exp, g = (List<?>) act;
            if (e.size() != g.size()) {
                diffs.add(path + ": length " + e.size() + " vs " + g.size());
                return;
            }
            for (int i = 0; i < e.size(); i++) compare(e.get(i), g.get(i), path + "[" + i + "]", diffs);
            return;
        }
        if (!exp.equals(act)) diffs.add(path + ": expected " + exp + ", got " + act);
    }

    @SuppressWarnings("unchecked")
    static int golden(String file) throws IOException {
        final String text = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        final Map<String, Object> root = (Map<String, Object>) new Json(text).value();
        final List<Object> cases = (List<Object>) root.get("cases");
        int pass = 0, fail = 0;
        final long t0 = System.nanoTime();
        for (Object o : cases) {
            final Map<String, Object> cs = (Map<String, Object>) o;
            final Map<String, Object> spec = (Map<String, Object>) cs.get("spec");
            final String name = (String) cs.get("name");
            final Map<String, Object> got = runCase(
                (int) num(spec, "tier", 1),
                cfgOf(spec),
                (long) num(spec, "secs", 14400));
            final List<String> diffs = new ArrayList<>();
            compare(cs.get("expect"), got, "", diffs);
            if (diffs.isEmpty()) {
                pass++;
                System.out.println("PASS " + name);
            } else {
                fail++;
                System.out.println("FAIL " + name);
                for (String d : diffs) System.out.println("    " + d);
            }
        }
        System.out
            .printf("%d cases: %d PASS, %d FAIL (%.0f ms)%n", pass + fail, pass, fail, (System.nanoTime() - t0) / 1e6);
        return fail == 0 && pass > 0 ? 0 : 1;
    }

    // ================================================================== main

    public static void main(String[] args) throws IOException {
        if (args.length >= 2 && "golden".equals(args[0])) {
            System.exit(golden(args[1]));
        } else if (args.length >= 5 && "run".equals(args[0])) {
            final Cfg c = new Cfg();
            c.rig = FieldRig.Mode.parse(args[2]);
            c.seed = Long.parseLong(args[3]);
            if (args.length > 5) c.m = Double.parseDouble(args[5]);
            if (args.length > 6) c.tt = Integer.parseInt(args[6]);
            if (args.length > 7) c.ts = Integer.parseInt(args[7]);
            if (args.length > 8) c.r = Double.parseDouble(args[8]);
            if (args.length > 9) c.feed = Double.parseDouble(args[9]);
            System.out.println(Json.write(runCase(Integer.parseInt(args[1]), c, Long.parseLong(args[4]))));
        } else if (args.length >= 3 && "collapse".equals(args[0])) {
            final Cfg c = new Cfg();
            c.collapse = true;
            c.stBuf = Double.parseDouble(args[1]);
            c.shock = Double.parseDouble(args[2]);
            if (args.length > 3) c.seed = Long.parseLong(args[3]);
            final long secs = args.length > 4 ? Long.parseLong(args[4]) : 3600;
            System.out.println(Json.write(runCase(3, c, secs)));
        } else if (args.length >= 1 && "invariant".equals(args[0])) {
            final List<String> bad = TierParams.invariantViolations();
            System.out.println(bad.isEmpty() ? "TierParams invariant OK" : String.join("\n", bad));
        } else {
            System.out.println(
                "usage: FieldSim golden <json> | run <tier> <off|bang|fine> <seed> <secs> [m tt ts r feed]"
                    + " | collapse <spaceTimeL> <shock> [seed secs] | invariant");
            System.exit(2);
        }
    }

    // ================================================================== a minimal JSON reader / writer (no deps)

    /** Just enough JSON for the golden file: objects (ordered), arrays, strings, doubles, booleans, null. */
    public static final class Json {

        private final String s;
        private int i;

        public Json(String s) {
            this.s = s;
        }

        public Object value() {
            ws();
            final char ch = s.charAt(i);
            if (ch == '{') return object();
            if (ch == '[') return array();
            if (ch == '"') return string();
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            final int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw new IllegalStateException("bad JSON at " + i);
            return Double.parseDouble(s.substring(st, i));
        }

        private Map<String, Object> object() {
            final LinkedHashMap<String, Object> o = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') {
                i++;
                return o;
            }
            while (true) {
                ws();
                final String k = string();
                ws();
                expect(':');
                o.put(k, value());
                ws();
                if (s.charAt(i) == ',') {
                    i++;
                    continue;
                }
                expect('}');
                return o;
            }
        }

        private List<Object> array() {
            final List<Object> a = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') {
                i++;
                return a;
            }
            while (true) {
                a.add(value());
                ws();
                if (s.charAt(i) == ',') {
                    i++;
                    continue;
                }
                expect(']');
                return a;
            }
        }

        private String string() {
            expect('"');
            final StringBuilder b = new StringBuilder();
            while (true) {
                final char ch = s.charAt(i++);
                if (ch == '"') return b.toString();
                if (ch != '\\') {
                    b.append(ch);
                    continue;
                }
                final char e = s.charAt(i++);
                switch (e) {
                    case 'b':
                        b.append('\b');
                        break;
                    case 'f':
                        b.append('\f');
                        break;
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'u':
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default:
                        b.append(e);
                }
            }
        }

        private void expect(char ch) {
            if (s.charAt(i) != ch) throw new IllegalStateException("expected '" + ch + "' at " + i);
            i++;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        public static String write(Object v) {
            final StringBuilder b = new StringBuilder();
            write(v, b);
            return b.toString();
        }

        private static void write(Object v, StringBuilder b) {
            if (v == null) b.append("null");
            else if (v instanceof Map) {
                b.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                    if (!first) b.append(',');
                    first = false;
                    b.append('"')
                        .append(e.getKey())
                        .append("\":");
                    write(e.getValue(), b);
                }
                b.append('}');
            } else if (v instanceof List) {
                b.append('[');
                boolean first = true;
                for (Object o : (List<?>) v) {
                    if (!first) b.append(',');
                    first = false;
                    write(o, b);
                }
                b.append(']');
            } else if (v instanceof Double) {
                final double d = (Double) v;
                if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d);
                else b.append(d);
            } else if (v instanceof String) b.append('"')
                .append(v)
                .append('"');
            else b.append(v);
        }
    }
}
