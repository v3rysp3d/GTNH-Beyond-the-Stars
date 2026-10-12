package com.nateyburr.beyondthestars.machines.quasar.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nateyburr.beyondthestars.machines.directive.GalaxyDirective;
import com.nateyburr.beyondthestars.machines.directive.QuasarDirective;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalacticMergerOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.Overlay;
import com.nateyburr.beyondthestars.machines.eye.field.PlanetSlots;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;

/**
 * P4: the offline world runner and golden test (quasar-p4.md §1-§5, §8; the B-37 discipline). A plain-JDK program:
 * the field, quasar-engine and Directive-rule classes have no Minecraft, GT or library dependency, so it compiles
 * and runs with javac + java alone.
 *
 * <pre>
 * java -cp &lt;classes&gt; com.nateyburr.beyondthestars.machines.quasar.engine.QuasarSim golden &lt;quasar-golden.json&gt;
 * java -cp &lt;classes&gt; ...QuasarSim run &lt;spec json&gt;      (one case, its result tree as JSON)
 * </pre>
 *
 * tools/quasar-golden.js writes the golden file from the prototype engine (designs/quasar-p4-prototype.html: the
 * shared ENGINE block plus the P4 block) and runs this with {@code --java}. {@link World} is a port of the prototype's
 * world ({@code newWorld} / {@code worldTick}): the Quasar Engine with the prototype's disc rig and purge regulator,
 * the standard Conduit rig ({@code conduitStep}: two mouth pairs, a combine, J1 Eye : crown, J3 halo : rail, J2
 * high : low, crowns, out 3), the Stability and Heavy Links, the Directive bits, and the Eye at T6-T8 (galaxy,
 * planets mode, companions, the satellite crossing, the Magnetar Collapse and the Galactic Merger) around
 * {@link StellarField}. It must reproduce every number of every case: counts, ledgers, the final states and four
 * FNV-1a hashes per second (Eye gauges and state; the quasar's disc; the bursts and links; the Directive codes).
 * <p>
 * The prototype rig is the reference automation; {@link QuasarRigSim} runs the same world on a gate rig that reads
 * only Directive bits (the §8.4 proof). The MTE units call {@link QuasarDisc}, {@link BurstRouting},
 * {@link BurstLink}, the field classes and the Directive rules directly; this class is not loaded in the game.
 */
public final class QuasarSim {

    private QuasarSim() {}

    // ================================================================== configuration (the prototype's CFG, QC, K,
    // RIG)

    /** The Eye's configuration (prototype {@code CFG}). */
    public static final class Cfg {

        public long seed = 1;
        public double m = 1, r = 1, feed = 0.694, f8 = GalaxyField.F8_DEFAULT, shock = 80;
        public int tt = 1, ts = 1;
        public boolean unlocked7 = true, unlocked8, coreMC = true, bufMC = true, coreGM = true, bufGM = true;
        /** Planets mode at T7/T8 (else galaxy mode). */
        public boolean planets;
        /** Companion tiers (galaxy mode; T7 takes the first, T8 the first two). */
        public int[] comp = {};
        /** Planet ids for the slots (prototype list). */
        public String[] planetIds = { "mars", "titan", "bare", "io", "pluto", "dd" };
    }

    /** The quasar's configuration (prototype {@code QC}). */
    public static final class QCfg {

        public double M0 = 64, b = 3, f = 1, purgeAt = QuasarParams.PURGE_AT_DEFAULT;
        public int coil = 3;
        public QuasarGas gas = QuasarGas.HE;
        public boolean stocked = true, power = true;
    }

    /** What a jet port docks. */
    public enum Dock {
        MOUTH,
        CROWN,
        VENT
    }

    /** The conduit rig (prototype {@code K}). */
    public static final class KCfg {

        public Dock top = Dock.MOUTH, bot = Dock.MOUTH;
        public int throat = 1, mk = 2;
        public boolean fuelErg, fuelOK = true, inv1 = true, stack, test8, twoLevel;
        public double c1 = 0.5, k1 = 2, p3 = 0, c2 = 0.5, k2 = 2, c3 = 0.2, k3 = 2, win = 30, c3t = 0.75;
    }

    /** The prototype's automation sub-switches ({@code RIG}). */
    public static final class Rig {

        public boolean disc = true, aim = true, space = true, assist = true;
    }

    /** The prototype's Directive window (the rig's thresholds ±5). */
    public static final double DIRECTIVE_W = 5;
    /** The prototype's binary Space valve on the Eye: Field bit 1 opens it at 10 units/s. */
    public static final double SPACE_ON = 10;

    // ================================================================== the Eye (prototype S, T6-T8)

    /** The prototype's Eye: one field, its state machine, its ledger. */
    public static final class Eye {

        public static final int LIT = 0, IGNITING = 1, DARK = 2;

        final Cfg c;
        final Rig rig;
        public final FieldState s;
        public int state = LIT;
        public int zone;
        /** The running transition: a {@link MagnetarCollapseOverlay} or a {@link GalacticMergerOverlay}, or null. */
        public Overlay trans;
        public final FieldSim.Ledger L = new FieldSim.Ledger();
        public final LinkedHashMap<String, Double> runs = new LinkedHashMap<>();
        public PlanetSlots slots;
        /** The field's event-channel warning at the last lit second (prototype {@code S.warn}). */
        public int warn;
        final StepInputs in = new StepInputs();

        Eye(int tier, Cfg c, Rig rig) {
            this.c = c;
            this.rig = rig;
            s = new FieldState(TierParams.of(tier, c.planets), new FieldRng(c.seed));
            if (s.tier.planets()) {
                final List<PlanetSlots.Planet> ps = new ArrayList<>();
                for (String id : c.planetIds) ps.add(PlanetSlots.prototype(id));
                slots = new PlanetSlots(s.tier.n, ps);
            }
            light();
        }

        /** Companions running beside a galaxy of tier n (none in planets mode). */
        int[] comps(TierParams t) {
            if (!t.galaxy) return new int[0];
            final int n = Math.min(c.comp.length, GalaxyField.maxCompanions(t.n));
            final int[] out = new int[n];
            System.arraycopy(c.comp, 0, out, 0, n);
            return out;
        }

        void light() {
            state = LIT;
            StellarField.light(s, c.m, c.tt);
            chargeCycle();
        }

        void chargeCycle() {
            final TierParams T = s.tier;
            final int n = T.n;
            if (n == 6) {
                FieldSim.add(L.up, "White Dwarf Matter", 60 * c.r);
                FieldSim.add(L.up, "Black Dwarf Matter", 15 * c.r);
            } else if (T.galaxy) {
                if (n == 7) FieldSim.add(L.up, "Magnetar Matter", GalaxyField.T7_MAGNETAR_UPKEEP * c.r);
                for (int k : comps(T)) {
                    final GalaxyField.Companion cp = GalaxyField.Companion.of(k);
                    final double par = GalaxyField.companionParallel(n, k);
                    for (int i = 0; i < cp.upkeepCount(); i++)
                        FieldSim.add(L.up, cp.upkeepName(i), cp.upkeepAmount(i) * par * c.r);
                }
            }
            L.eu += FieldSim.RECIPE_UMV * Math.pow(4, n - 1) * 1200;
        }

        void fail() {
            L.fails++;
            if (trans == null && slots != null && s.tier.planets()) slots.failRuns();
            trans = null;
            state = DARK;
            StellarField.resetLost(s);
        }

        /** Prototype {@code relight(S)} for T6-T8. */
        void relight() {
            final TierParams T = s.tier;
            if (T.n >= 7) {
                s.setX(0, 0, 0, 0);
                light();
            } else if (T.n == 6) {
                s.setX(0, 0, 0, 0);
                light();
                FieldSim.add(L.up, "White Dwarf Matter", 18432);
                FieldSim.add(L.up, "Black Dwarf Matter", 4608);
                FieldSim.add(L.up, "Antipolarization Agent", 100000);
            }
        }

        /** Prototype {@code step(S)} for T6-T8, with the bursts {@code w} solved this second. */
        void step(World w) {
            if (state == DARK) {
                zone = -1;
                StellarField.stepDark(s);
                return;
            }
            final TierParams T = s.tier;
            final Overlay tr = trans;
            final double m = tr != null ? 1 : Math.min(c.m, StellarField.mMax(T, c.tt));
            in.m = m;
            in.q = StepInputs.qFor(c.ts);
            in.r = c.r;
            in.p = 1;
            in.feed = c.feed;
            in.dialSetpoint = c.m;
            in.tt = c.tt;
            in.beta = w.gBeta;
            in.gHi = w.gHi;
            in.gLo = w.gLo;
            in.gHalo = w.gHalo;
            in.gEye = w.gEye;
            in.f8 = c.f8;
            in.companions = comps(T).length;
            final LinkedHashMap<String, Double> raw;
            if (T.n <= 6) raw = FieldRig.ctrl(T, s.prevE, s.prevWarn, FieldRig.Mode.FINE);
            else {
                raw = new LinkedHashMap<>();
                if (rig.space && s.prevE[StellarField.P] > 5) raw.put("Space", SPACE_ON);
            }
            FieldRig.toUnits(T, raw, in.u);
            final StepResult out = StellarField.step(s, tr != null ? tr : Overlay.NONE, in);

            for (Map.Entry<TrimDef, Double> e : in.u.entrySet())
                FieldSim.add(L.trims, e.getKey().kind.key, e.getValue() * e.getKey().kind.unitLitres);
            if (m > 1) L.tach += ((m - 1) / 0.25) * 10 * Math.pow(2, T.n - 1);
            L.zoneT[out.zone]++;
            L.ySum += out.Y;
            L.yN++;
            zone = out.zone;
            warn = out.warn;

            // the transition's charges (taken before its own failure, as the prototype's transTick)
            if (out.wdmCharged > 0) FieldSim.add(L.up, "White Dwarf Matter", out.wdmCharged);
            if (out.bdmCharged > 0) FieldSim.add(L.up, "Black Dwarf Matter", out.bdmCharged);
            if (out.ergoCharged > 0) FieldSim.add(L.up, "Ergomatter", out.ergoCharged);
            if (out.fail != null) {
                fail();
                return;
            }
            if (out.relit) {
                state = LIT;
                chargeCycle();
            }
            if (out.transitionSuccess) {
                if (tr instanceof MagnetarCollapseOverlay) {
                    c.unlocked7 = true;
                    c.coreMC = false;
                } else {
                    c.unlocked8 = true;
                    c.coreGM = false;
                }
                trans = null;
            }
            final TierParams Tn = s.tier;
            if (out.ergoFed) FieldSim.add(L.up, "Ergomatter", out.ergoToDraw);
            if (out.crossingCaught) FieldSim.add(L.prod, "Superdense Ergomatter", out.crossingBonus);
            if (Tn.planets() && slots != null)
                for (PlanetSlots.Run run : slots.tick(Tn.n, out.zone, m)) FieldSim.add(runs, run.key(), run.succeeded);
            if (out.cycleEnded) {
                if (out.hasCycleProduct) {
                    if (out.companionCycle) for (int k : comps(Tn)) {
                        final GalaxyField.Companion cp = GalaxyField.Companion.of(k);
                        FieldSim.add(
                            L.prod,
                            cp.product + " (companion)",
                            cp.productPerCycle * GalaxyField.companionParallel(Tn.n, k)
                                * out.cycleMeanY
                                * out.cycleRMin);
                    }
                    FieldSim.add(L.prod, productName(Tn), out.cycleProduct);
                }
                L.cycles++;
                chargeCycle();
            }
        }

        static String productName(TierParams t) {
            switch (t.n) {
                case 6:
                    return "Magnetar Matter";
                case 7:
                    return "Ergomatter";
                default:
                    return "Superdense Ergomatter";
            }
        }

        /** The burst demand now (prototype {@code demandNow}), Bu/t. */
        double demandNow() {
            if (trans instanceof MagnetarCollapseOverlay) return ((MagnetarCollapseOverlay) trans).demand();
            if (trans instanceof GalacticMergerOverlay) return ((GalacticMergerOverlay) trans).demand();
            final TierParams T = s.tier;
            return T.n >= 7 ? GalaxyField.demand(T.n, T.planets(), comps(T).length) : GalaxyField.D7;
        }

        /** β's normaliser (prototype {@code normDemand}): D₇ during the halo capture, else the demand. */
        double normDemand() {
            if (trans instanceof MagnetarCollapseOverlay && ((MagnetarCollapseOverlay) trans).phase == 3)
                return GalaxyField.D7;
            return demandNow();
        }

        boolean overfeed() {
            return trans instanceof MagnetarCollapseOverlay && ((MagnetarCollapseOverlay) trans).phase == 1;
        }

        int transSt() {
            if (trans instanceof MagnetarCollapseOverlay) return ((MagnetarCollapseOverlay) trans).st;
            if (trans instanceof GalacticMergerOverlay) return ((GalacticMergerOverlay) trans).st;
            return 0;
        }

        /** Prototype {@code beginMC}. */
        public void beginMC() {
            trans = new MagnetarCollapseOverlay(c.shock);
        }

        /** Prototype {@code beginGM}. */
        public void beginGM() {
            trans = GalacticMergerOverlay.begin(s);
        }
    }

    // ================================================================== the world (prototype W)

    /** The quasar's running totals (prototype {@code Q.L}). */
    public static final class QLedger {

        public double gas, gasTrim, space, coilEU, ySum, bu;
        public long n, fails, eruptions, purges;
        public final long[] zoneT = new long[5];
    }

    /** One quasar, its standard conduit rig and one Eye. */
    public static final class World {

        public final Cfg c;
        public final QCfg qc;
        public final KCfg k;
        public final Rig rig;
        public final Eye eye;
        public final QuasarState q;
        final QuasarInputs qin = new QuasarInputs();
        public final QLedger QL = new QLedger();
        /** The conduit's open state and its reopen count (prototype {@code W.Kst}). */
        public boolean kOpen = true;
        int reopen;
        /** The conduit's totals (prototype {@code W.KL}). */
        public final LinkedHashMap<String, Double> KL = new LinkedHashMap<>();
        public final BurstLink link = new BurstLink(), link8 = new BurstLink();
        public boolean linked, force1, hold;
        /** Bursts vented this second. */
        public double vent;
        /** The Directive codes (prototype {@code W.dir}): disc, field, event, transition. */
        public int dirDisc, dirField, dirEvent, dirTrans;
        /** The Eye's burst inputs this second (prototype {@code S.g}). */
        public double gBeta, gHi, gLo, gHalo, gEye, gD;
        /** Bu/t into crowns this second (EU/t = 10¹³ × this). */
        public double crownBu;

        public World(int tier, Cfg c, QCfg qc, KCfg k, Rig rig) {
            this.c = c;
            this.qc = qc;
            this.k = k;
            this.rig = rig;
            eye = new Eye(tier, c, rig);
            q = new QuasarState(QuasarState.rngFor(c.seed));
            q.seed(qc.M0, QuasarParams.aEq(Math.min(qc.f, 1), Math.min(qc.b, qc.coil)));
        }

        /** Prototype {@code worldTick}: the Transition bits, the quasar, the conduit, the Eye, the other bits. */
        public void tick() {
            final int tb = transBits();
            dirTrans = tb;
            final boolean assist = rig.assist;
            hold = assist && (tb & 8) != 0;
            if (assist && (tb & 4) != 0) q.requestPurge();
            force1 = assist && (tb & 4) != 0;
            quasarStep();
            conduitStep();
            eye.step(this);
            final QuasarDirective.State ds = new QuasarDirective.State();
            ds.lagE = q.lagE;
            ds.lagPhi = q.lagPhi;
            ds.purgeAt = qc.purgeAt;
            ds.open = q.hole == QuasarState.Hole.OPEN;
            ds.b = q.b;
            ds.vented = vent;
            ds.conduitOpen = kOpen;
            dirDisc = QuasarDirective.disc(ds, DIRECTIVE_W);
            final FieldState s = eye.s;
            final boolean lit = s.tier.n >= 7 && eye.state == Eye.LIT;
            dirField = GalaxyDirective
                .field(lit, s.tier.planets(), s.prevE[StellarField.P], s.prevE[StellarField.W], DIRECTIVE_W);
            dirEvent = GalaxyDirective.events(
                lit,
                s.tier.planets(),
                s.prevE[StellarField.L],
                eye.warn,
                s.tde != null && s.tde.warned,
                DIRECTIVE_W);
        }

        /** The Galaxy Transition bits (prototype {@code transBits}) from last second's state. */
        int transBits() {
            final FieldState s = eye.s;
            final int n = s.tier.n;
            final boolean running = eye.trans != null;
            final boolean ready = GalaxyDirective.ready(
                n,
                s.tier.planets(),
                eye.state == Eye.LIT,
                running,
                eye.zone,
                n == 6 ? link.done : link8.done,
                n == 6 ? c.coreMC : c.coreGM,
                n == 6 ? c.bufMC : c.bufGM);
            return GalaxyDirective.transition(running, ready, eye.overfeed(), eye.transSt(), q.Phi, q.b);
        }

        /** The prototype's quasarStep: its disc rig and purge regulator, then {@link QuasarDisc#step}. */
        void quasarStep() {
            final boolean auto = rig.disc;
            qin.gas = qc.gas;
            qin.coilTier = qc.coil;
            qin.bSetpoint = qc.b;
            qin.powerOk = qc.power;
            final double medd = QuasarParams.eddingtonFeed(q.M, qc.gas);
            final double F = qc.stocked ? qc.f * medd : 0;
            qin.gasDrawn = F;
            QL.gas += F;
            double uG = 0, uS = 0;
            if (auto) {
                if (q.lagE < -5) uG = 10;
                else if (q.lagE > 5) uS = 10;
            }
            qin.gasTrimUnits = uG;
            qin.spaceTrimUnits = uS;
            qin.regulatorPurge = auto && qc.purgeAt > 0 && !hold && q.lagPhi >= qc.purgeAt;
            final QuasarResult r = QuasarDisc.step(q, qin);
            QL.coilEU += r.coilEUt * 20;
            if (r.reforming) return;
            QL.gasTrim += r.gasTrimLitres;
            QL.space += r.spaceLitres;
            QL.zoneT[r.zone]++;
            if (r.blowOut) {
                QL.fails++;
                return;
            }
            QL.ySum += q.Yq;
            QL.n++;
            if (r.eruption) QL.eruptions++;
            if (r.purge) QL.purges++;
            QL.bu += r.G;
        }

        /** The prototype's conduitStep: the standard rig, crowns, the Eye's burst inputs and the links. */
        void conduitStep() {
            final FieldState s = eye.s;
            final TierParams T = s.tier;
            final boolean wired = rig.aim;
            final double half = q.G / 2, cap = BurstRouting.throatCap(k.throat);
            final int nMouth = (k.top == Dock.MOUTH ? 1 : 0) + (k.bot == Dock.MOUTH ? 1 : 0);
            final boolean planets = T.n >= 7 && T.planets();
            final boolean t8 = (T.n >= 8 && !planets) || eye.trans instanceof GalacticMergerOverlay;
            final boolean test8 = T.n == 7 && !planets && eye.trans == null && k.test8;
            final int mouths = 2 * nMouth, comb = nMouth == 2 ? 1 : 0, j1 = nMouth > 0 ? 1 : 0;
            final int j3 = nMouth > 0 && (t8 || test8) ? 1 : 0, j2 = j1;
            final int elements = mouths + comb + j1 + j3 + j2;
            final boolean ergOK = c.unlocked7 || T.n >= 7;
            final boolean erg = k.fuelErg && ergOK;
            if (!k.fuelOK) {
                kOpen = false;
                reopen = 0;
            } else if (!kOpen) {
                reopen++;
                if (reopen >= BurstRouting.REOPEN_SECONDS) kOpen = true;
            }
            final double fuelLs = k.fuelOK ? elements * BurstRouting.throatUpkeep(k.throat, erg) : 0;
            FieldSim.add(KL, erg ? "Ergomatter" : "Molten SpaceTime", fuelLs);
            double sIn = 0, clip = 0, vented = 0;
            final Dock[] docks = { k.top, k.bot };
            for (Dock d : docks) {
                if (d == Dock.CROWN) continue;
                if (d == Dock.MOUTH) {
                    if (kOpen) {
                        final double v = Math.min(half, cap);
                        clip += half - v;
                        sIn += v;
                    } else vented += half;
                } else vented += half;
            }
            if (comb == 1 && kOpen) {
                final double v = Math.min(sIn, cap);
                clip += sIn - v;
                sIn = v;
            }
            final boolean lit = eye.state == Eye.LIT;
            final double P = lit ? s.prevE[StellarField.P] : 0, Wg = lit ? s.prevE[StellarField.W] : 0;
            final double Lg = lit ? s.prevE[StellarField.L] : 0;
            int s1 = k.twoLevel ? BurstRouting.twoLevel((dirField & 2) != 0, (dirField & 1) != 0)
                : k.inv1 ? BurstRouting.level(-P, k.win) : BurstRouting.level(P, k.win);
            if (force1) s1 = 15;
            final double k1 = (wired && T.n >= 7) || force1 ? k.k1 : 0;
            final double sh1 = test8 ? 1 : BurstRouting.share(k.c1, k1, s1);
            final double out3 = k.p3 * sIn, eyeS = sh1 * (1 - k.p3) * sIn;
            final double crownLeg = (1 - sh1) * (1 - k.p3) * sIn + out3;
            double halo = 0, rail = eyeS, dump = 0;
            if (j3 == 1 && T.n >= 8 && !planets) {
                final int s3 = k.twoLevel ? BurstRouting.twoLevel((dirEvent & 1) != 0, (dirEvent & 2) != 0)
                    : BurstRouting.level(Lg, k.win);
                final double sh3 = BurstRouting.share(k.c3, wired ? k.k3 : 0, s3);
                halo = eyeS * sh3;
                rail = eyeS - halo;
            } else if (j3 == 1 && test8) {
                halo = eyeS * k.c3t;
                rail = eyeS - halo;
                dump = halo;
            }
            final int s2 = k.twoLevel ? BurstRouting.twoLevel((dirField & 4) != 0, (dirField & 8) != 0)
                : BurstRouting.level(Wg, k.win);
            final double sh2 = BurstRouting.share(k.c2, wired ? k.k2 : 0, s2);
            final double hi = rail * sh2, lo = rail - hi;
            // crowns: on each docked jet cap, and the power plant on J1's crown leg
            final double ccap = BurstRouting.crownCap(k.mk) * (k.stack ? 2 : 1);
            double eu = 0, cclip = 0, used = 0;
            final double[] sites = { k.top == Dock.CROWN ? half : -1, k.bot == Dock.CROWN ? half : -1,
                j1 == 1 ? crownLeg : -1 };
            for (double st : sites) {
                if (st < 0) continue;
                final double u = Math.min(st, ccap);
                eu += BurstRouting.crownEUt(u);
                cclip += st - u;
                used += u;
            }
            crownBu = used;
            FieldSim.add(KL, "eu", eu * 20);
            final Double oldVent = KL.get("vent"); // the prototype's (KL.vent||0)+vent+cclip, left to right
            KL.put("vent", (oldVent == null ? 0 : oldVent) + vented + cclip);
            FieldSim.add(KL, "clip", clip);
            vent = vented;
            // the Eye's burst inputs
            final double mm = eye.trans != null ? 1 : Math.min(c.m, StellarField.mMax(T, c.tt));
            final double Dn = eye.normDemand();
            gEye = eyeS;
            gD = eye.demandNow();
            gBeta = (eyeS - dump) / (Dn * mm);
            gHi = hi / (Dn * mm);
            gLo = lo / (Dn * mm);
            gHalo = (halo - dump) / (Dn * mm);
            // machine unlocks: a linked quasar actively stabilising the Eye
            linked = kOpen && q.hole == QuasarState.Hole.OPEN && q.b > 0 && nMouth > 0;
            link.roll(rail, hi);
            if (T.n == 6 && eye.trans == null && !link.done) link.update(link.stabilityOk(linked));
            link8.roll(eyeS, halo);
            if (T.n == 7 && !planets && eye.trans == null && !link8.done) link8.update(link8.heavyOk(linked, test8));
        }
    }

    // ================================================================== one golden case

    /** Runs one case exactly like tools/quasar-golden.js's {@code __run} and returns its result tree. */
    public static Map<String, Object> runCase(Map<String, Object> spec) {
        final int tier = (int) FieldSim.num(spec, "tier", 7);
        final long secs = (long) FieldSim.num(spec, "secs", 7200);
        final boolean autoBegin = Boolean.TRUE.equals(spec.get("autoBegin"));
        final World W = new World(tier, cfgOf(spec), qcfgOf(spec), kcfgOf(spec), rigOf(spec));
        final Eye S = W.eye;
        final QuasarState Q = W.q;
        int he = FieldSim.FNV_OFFSET, hq = FieldSim.FNV_OFFSET, hg = FieldSim.FNV_OFFSET, hd = FieldSim.FNV_OFFSET;
        boolean begun = false;
        long beginT = -1;
        String beginKind = "";
        for (long i = 0; i < secs; i++) {
            if (autoBegin && !begun && (W.dirTrans & 1) != 0) {
                begun = true;
                beginT = S.s.t;
                beginKind = S.s.tier.n == 6 ? "mc" : "gm";
                if (S.s.tier.n == 6) S.beginMC();
                else S.beginGM();
            }
            W.tick();
            if (S.state == Eye.DARK) S.relight();
            final FieldState s = S.s;
            for (int k = 0; k < StellarField.GAUGES; k++) he = FieldSim.fnv(he, FieldSim.q6(s.x[k]));
            he = FieldSim.fnv(he, FieldSim.q6(s.A));
            he = FieldSim.fnv(he, FieldSim.q6(s.I));
            he = FieldSim.fnv(he, S.zone);
            he = FieldSim.fnv(he, S.state);
            he = FieldSim.fnv(he, s.tier.n);
            he = FieldSim.fnv(he, s.lock);
            he = FieldSim.fnv(he, s.prec);
            for (double v : new double[] { Q.E, Q.Phi, Q.a, Q.M, Q.G, Q.I }) hq = FieldSim.fnv(hq, FieldSim.q6(v));
            hq = FieldSim.fnv(hq, Q.zone);
            hq = FieldSim.fnv(hq, Q.reform);
            hq = FieldSim.fnv(hq, Q.hole == QuasarState.Hole.OPEN ? 0 : 1);
            for (double v : new double[] { W.gBeta, W.gHi, W.gLo, W.gHalo, W.gEye, W.link.mean, W.link8.mean })
                hg = FieldSim.fnv(hg, FieldSim.q6(v));
            hg = FieldSim.fnv(hg, W.link.streak);
            hg = FieldSim.fnv(hg, W.link8.streak);
            hd = FieldSim.fnv(hd, W.dirDisc);
            hd = FieldSim.fnv(hd, W.dirField);
            hd = FieldSim.fnv(hd, W.dirEvent);
            hd = FieldSim.fnv(hd, W.dirTrans);
        }
        final FieldSim.Ledger L = S.L;
        final LinkedHashMap<String, Object> res = new LinkedHashMap<>();
        res.put("zoneT", longs(L.zoneT));
        res.put("ySum", L.ySum);
        res.put("yN", (double) L.yN);
        res.put("fails", (double) L.fails);
        res.put("charges", (double) L.charges);
        res.put("cycles", (double) L.cycles);
        res.put("eu", L.eu);
        res.put("up", new LinkedHashMap<String, Object>(L.up));
        res.put("prod", new LinkedHashMap<String, Object>(L.prod));
        res.put("trims", new LinkedHashMap<String, Object>(L.trims));
        res.put("runs", new LinkedHashMap<String, Object>(S.runs));
        final List<Object> xs = new ArrayList<>();
        for (double v : S.s.x) xs.add(v);
        res.put("x", xs);
        res.put("A", S.s.A);
        res.put("I", S.s.I);
        res.put("lock", (double) S.s.lock);
        res.put("t", (double) S.s.t);
        res.put("eyeT", S.s.eyeT);
        res.put("tier", (double) S.s.tier.n);
        res.put("state", (double) S.state);
        res.put("prec", (double) S.s.prec);
        if (S.s.tde != null) {
            final LinkedHashMap<String, Object> cr = new LinkedHashMap<>();
            cr.put("n", (double) S.s.tde.n);
            cr.put("caught", (double) S.s.tde.caught);
            cr.put("left", (double) S.s.tde.left);
            res.put("crossing", cr);
        } else res.put("crossing", null);
        final QLedger QL = W.QL;
        final LinkedHashMap<String, Object> qr = new LinkedHashMap<>();
        qr.put("M", Q.M);
        qr.put("a", Q.a);
        qr.put("E", Q.E);
        qr.put("Phi", Q.Phi);
        qr.put("I", Q.I);
        qr.put("open", Q.hole == QuasarState.Hole.OPEN);
        qr.put("reform", (double) Q.reform);
        qr.put("lock", (double) Q.lock);
        qr.put("gas", QL.gas);
        qr.put("gasTrim", QL.gasTrim);
        qr.put("space", QL.space);
        qr.put("coilEU", QL.coilEU);
        qr.put("zoneT", longs(QL.zoneT));
        qr.put("ySum", QL.ySum);
        qr.put("n", (double) QL.n);
        qr.put("fails", (double) QL.fails);
        qr.put("eruptions", (double) QL.eruptions);
        qr.put("purges", (double) QL.purges);
        qr.put("bu", QL.bu);
        qr.put("firstLight", Q.firstLight);
        qr.put("edd", (double) Q.edd);
        qr.put("eddDone", Q.eddDone);
        qr.put("firstErupt", Q.firstErupt);
        res.put("quasar", qr);
        res.put("conduit", new LinkedHashMap<String, Object>(W.KL));
        res.put("open", W.kOpen);
        res.put("link", linkMap(W.link));
        res.put("link8", linkMap(W.link8));
        res.put("unlocked7", W.c.unlocked7);
        res.put("unlocked8", W.c.unlocked8);
        res.put("coreMC", W.c.coreMC);
        res.put("coreGM", W.c.coreGM);
        if (S.trans != null) {
            final LinkedHashMap<String, Object> tr = new LinkedHashMap<>();
            if (S.trans instanceof MagnetarCollapseOverlay) {
                final MagnetarCollapseOverlay mc = (MagnetarCollapseOverlay) S.trans;
                tr.put("kind", "mc");
                tr.put("phase", (double) mc.phase);
                tr.put("st", (double) mc.st);
                tr.put("streak", (double) mc.streak);
            } else {
                final GalacticMergerOverlay gm = (GalacticMergerOverlay) S.trans;
                tr.put("kind", "gm");
                tr.put("phase", (double) gm.phase);
                tr.put("st", (double) gm.st);
                tr.put("streak", (double) gm.streak);
            }
            res.put("trans", tr);
        } else res.put("trans", null);
        res.put("beginT", (double) beginT);
        res.put("beginKind", beginKind);
        final LinkedHashMap<String, Object> hash = new LinkedHashMap<>();
        hash.put("eye", (double) (he & 0xFFFFFFFFL));
        hash.put("quasar", (double) (hq & 0xFFFFFFFFL));
        hash.put("bursts", (double) (hg & 0xFFFFFFFFL));
        hash.put("directive", (double) (hd & 0xFFFFFFFFL));
        res.put("hash", hash);
        return res;
    }

    static List<Object> longs(long[] a) {
        final List<Object> l = new ArrayList<>();
        for (long v : a) l.add((double) v);
        return l;
    }

    static Map<String, Object> linkMap(BurstLink l) {
        final LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("streak", (double) l.streak);
        m.put("best", (double) l.best);
        m.put("done", l.done);
        m.put("mean", l.mean);
        m.put("frac", l.frac);
        m.put("n", (double) l.n);
        return m;
    }

    // ================================================================== spec → configuration

    @SuppressWarnings("unchecked")
    static Map<String, Object> sub(Map<String, Object> spec, String key) {
        final Object o = spec.get(key);
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<String, Object>();
    }

    static boolean bool(Map<String, Object> o, String k, boolean dflt) {
        final Object v = o.get(k);
        return v instanceof Boolean ? (Boolean) v : dflt;
    }

    public static Cfg cfgOf(Map<String, Object> spec) {
        final Cfg c = new Cfg();
        c.seed = (long) FieldSim.num(spec, "seed", 1);
        final Map<String, Object> o = sub(spec, "cfg");
        c.m = FieldSim.num(o, "m", c.m);
        c.tt = (int) FieldSim.num(o, "tt", c.tt);
        c.ts = (int) FieldSim.num(o, "ts", c.ts);
        c.r = FieldSim.num(o, "r", c.r);
        c.feed = FieldSim.num(o, "feed", c.feed);
        c.f8 = FieldSim.num(o, "f8", c.f8);
        c.shock = FieldSim.num(o, "shock", c.shock);
        c.unlocked7 = bool(o, "unlocked7", c.unlocked7);
        c.unlocked8 = bool(o, "unlocked8", c.unlocked8);
        c.coreMC = bool(o, "coreMC", c.coreMC);
        c.bufMC = bool(o, "bufMC", c.bufMC);
        c.coreGM = bool(o, "coreGM", c.coreGM);
        c.bufGM = bool(o, "bufGM", c.bufGM);
        c.planets = "planets".equals(o.get("mode"));
        final Object comp = o.get("comp");
        if (comp instanceof List) {
            final List<?> l = (List<?>) comp;
            c.comp = new int[l.size()];
            for (int i = 0; i < l.size(); i++) c.comp[i] = ((Number) l.get(i)).intValue();
        }
        final Object pl = o.get("planets");
        if (pl instanceof List) {
            final List<?> l = (List<?>) pl;
            c.planetIds = new String[l.size()];
            for (int i = 0; i < l.size(); i++) c.planetIds[i] = (String) l.get(i);
        }
        return c;
    }

    public static QCfg qcfgOf(Map<String, Object> spec) {
        final QCfg q = new QCfg();
        final Map<String, Object> o = sub(spec, "qc");
        q.M0 = FieldSim.num(o, "M0", q.M0);
        q.coil = (int) FieldSim.num(o, "coil", q.coil);
        q.b = FieldSim.num(o, "b", q.b);
        q.f = FieldSim.num(o, "f", q.f);
        q.purgeAt = FieldSim.num(o, "purgeAt", q.purgeAt);
        if (o.get("gas") instanceof String) q.gas = QuasarGas.parse((String) o.get("gas"));
        q.stocked = bool(o, "stocked", q.stocked);
        q.power = bool(o, "power", q.power);
        return q;
    }

    static Dock dock(Object v, Dock dflt) {
        if (!(v instanceof String)) return dflt;
        return "crown".equals(v) ? Dock.CROWN : "mouth".equals(v) ? Dock.MOUTH : Dock.VENT;
    }

    public static KCfg kcfgOf(Map<String, Object> spec) {
        final KCfg k = new KCfg();
        final Map<String, Object> o = sub(spec, "k");
        k.top = dock(o.get("top"), k.top);
        k.bot = dock(o.get("bot"), k.bot);
        k.throat = (int) FieldSim.num(o, "throat", k.throat);
        k.mk = (int) FieldSim.num(o, "mk", k.mk);
        k.fuelErg = "erg".equals(o.get("fuel"));
        k.fuelOK = bool(o, "fuelOK", k.fuelOK);
        k.inv1 = bool(o, "inv1", k.inv1);
        k.stack = bool(o, "stack", k.stack);
        k.test8 = bool(o, "test8", k.test8);
        k.twoLevel = bool(o, "twoLevel", k.twoLevel);
        k.c1 = FieldSim.num(o, "c1", k.c1);
        k.k1 = FieldSim.num(o, "k1", k.k1);
        k.p3 = FieldSim.num(o, "p3", k.p3);
        k.c2 = FieldSim.num(o, "c2", k.c2);
        k.k2 = FieldSim.num(o, "k2", k.k2);
        k.c3 = FieldSim.num(o, "c3", k.c3);
        k.k3 = FieldSim.num(o, "k3", k.k3);
        k.win = FieldSim.num(o, "win", k.win);
        k.c3t = FieldSim.num(o, "c3t", k.c3t);
        return k;
    }

    public static Rig rigOf(Map<String, Object> spec) {
        final Rig r = new Rig();
        final Map<String, Object> o = sub(spec, "rig");
        r.disc = bool(o, "disc", r.disc);
        r.aim = bool(o, "aim", r.aim);
        r.space = bool(o, "space", r.space);
        r.assist = bool(o, "assist", r.assist);
        return r;
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
            final Map<String, Object> spec = (Map<String, Object>) cs.get("spec");
            final String name = (String) cs.get("name");
            final Map<String, Object> got = runCase(spec);
            final List<String> diffs = new ArrayList<>();
            FieldSim.compare(cs.get("expect"), got, "", diffs);
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

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        if (args.length >= 2 && "golden".equals(args[0])) {
            System.exit(golden(args[1]));
        } else if (args.length >= 2 && "run".equals(args[0])) {
            final Map<String, Object> spec = (Map<String, Object>) new FieldSim.Json(args[1]).value();
            System.out.println(FieldSim.Json.write(runCase(spec)));
        } else {
            System.out.println("usage: QuasarSim golden <quasar-golden.json> | run '<spec json>'");
            System.exit(2);
        }
    }
}
