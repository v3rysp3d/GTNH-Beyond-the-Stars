package com.nateyburr.beyondthestars.machines.eye.field;

import java.util.Locale;
import java.util.Map;

/**
 * D-49 stability field: the pure per-second maths (d49-build-plan.md §4). Mirrors the prototype's {@code step()},
 * {@code light()}, {@code startBatch()} and the ignition branch line for line (designs/star-lifecycle-prototype.html,
 * the ENGINE block), RNG draws included, so tools/fieldsim-golden.js can prove the Java equals the prototype
 * bit for bit. No Minecraft or GT types.
 * <p>
 * <b>Exact-match rules.</b> Floating-point expressions keep the prototype's operand order. RNG draws happen in this
 * order: one {@code uni} when a star with an event lights; one sign draw at a signed warning; one {@code uni} when an
 * event's last hit second ends; one draw per active gauge per lit second, in the order P, Θ, Ω, Π. Trims are summed
 * in the order of {@link StepInputs#u}. The clock {@link FieldState#t} advances every second, DARK included.
 * <p>
 * The parallel p divides trims ({@code fx·u/p}) and multiplies the feed and the product; the prototype has p = 1,
 * where both forms are identical.
 * <p>
 * <b>P4 (T7/T8, planets mode; quasar-p4.md §4).</b> Two more gauges, W (rail warp) and Λ (tidal shear), and the
 * prototype's additive hooks in its order: a transition's forced event terms ({@link Overlay#addEvents}) after its
 * drift, then the T8 satellite crossing; per-gauge σ; the burst terms ({@link TierParams#burstTerm}) after the event
 * term; the minor merger's precession flip; per second after the transition, the β sum, the T8 Ergomatter feed and
 * the crossing's end; at the cycle end the galaxy product (0 while a transition runs). RNG draws added by P4: one
 * {@code uni} for the crossing when a T8 lights (after the event's), one when a crossing ends. Pow / cbrt use
 * {@link StrictMath} (fdlibm, as V8).
 */
public final class StellarField {

    public static final int P = 0, TH = 1, OM = 2, PI = 3, W = 4, L = 5, V = 6, H = 7;
    /** Gauge slots in every field array (P, Θ, Ω, Π, P4's W, Λ, and P5's V, H). */
    public static final int GAUGES = 8;
    /** The D-49 and P4 gauges (the T1-T8 golden tests hash and list these six). */
    public static final int GAUGES_P4 = 6;
    public static final String[] GAUGE_SYM = { "P", "Θ", "Ω", "Π", "W", "Λ", "V", "H" };
    public static final String[] GAUGE_NAME = { "Pressure", "Core heat", "Spin", "Polarity", "Rail warp", "Tidal shear",
        "Infall", "Expansion" };
    public static final String[] ZONE_NAME = { "Resonant", "Stable", "Strained", "Critical", "Breach" };
    static final double[] ZONE_Y = { 1.25, 1.0, 0.5, 0, 0 };
    static final double[] ZONE_DI = { 1, 0.2, -0.5, -2, 0 };

    /** Harmonic Lock: real seconds in Resonant before Y = {@link #LOCK_Y}. */
    public static final int LOCK_SECONDS = 600;
    public static final double LOCK_Y = 1.5;
    /** T6: shield charge per Agent unit (per parallel). */
    public static final double SHIELD_PER_UNIT = 4;
    /**
     * T6: shield charge per second from the Agent upkeep, × r (my ruling 2026-10-13: the Agent fed also charges the
     * shield). 3,000 L per 60 s cycle = 0.5 unit/s at {@link #SHIELD_PER_UNIT}; with no trim A settles at 40.
     * PROPOSED (B-60).
     */
    public static final double SHIELD_PER_UPKEEP = 2;
    /** T4: litres of White Dwarf Matter per litre of HSM fed, before × mean Y × r. */
    public static final double WDM_PER_HSM = 3.6864;
    /** Cycle length and T5 batch length, Eye-seconds. */
    public static final double CYCLE_SECONDS = 60, BATCH_SECONDS = 7200;
    /** Ignition: P starts here, rises {@link #IGNITION_RATE}/s (not time-scaled), lights at {@link #IGNITION_LIT}. */
    public static final double IGNITION_START = -80, IGNITION_RATE = 1.5, IGNITION_LIT = -20;
    /** T5: the batch starts at Θ +90 (the setpoint's start), P 0. */
    public static final double BATCH_START_TH = 90;

    private StellarField() {}

    // ------------------------------------------------------------------ helpers (prototype names)

    /** Zone of the worst |error|: 0 Resonant .. 4 Breach. */
    public static int zoneOf(double w) {
        return w <= 10 ? 0 : w <= 40 ? 1 : w <= 70 ? 2 : w <= 100 ? 3 : 4;
    }

    /** T5 cooling setpoint at batch Eye-time {@code eyeT}: +90 → −90 over 7,200 s. */
    public static double spAt(double eyeT) {
        return 90 - 180 * Math.min(eyeT, 7200) / 7200;
    }

    /** The time-dial cap {@code 1 + 0.25·max(0, tt − N)}. */
    public static double mMax(TierParams t, int tt) {
        return 1 + 0.25 * Math.max(0, tt - t.n);
    }

    static double clamp(double v, double a, double b) {
        return v < a ? a : v > b ? b : v;
    }

    static double uni(FieldRng rng, double a, double b) {
        return a + (b - a) * rng.next();
    }

    /** "+12.3" / "−12.3" / "0.0", the prototype's fmtS. */
    public static String fmtSigned(double v) {
        return (v > 0 ? "+" : v < 0 ? "−" : "") + String.format(Locale.ROOT, "%.1f", Math.abs(v));
    }

    // ------------------------------------------------------------------ lifecycle pieces

    /**
     * Prototype {@code light()}, field part: Integrity 100, lock 0, shield 0, a fresh cycle, and the first event
     * scheduled at {@code t + uni(every) / min(dialSetpoint, m_max)}. The gauges are NOT reset (an ignition hands
     * over at P −20). P4: a T8 galaxy also schedules its satellite crossing at {@code t + uni(1,800, 3,600)} (after the
     * event's draw), and every T7+ table starts with the precession sense +1. The caller then starts a T5 batch
     * ({@link #startBatch}) and charges the first cycle.
     */
    public static void light(FieldState s, double dialSetpoint, int tt) {
        final TierParams t = s.tier;
        s.I = 100;
        s.lock = 0;
        s.A = 0;
        s.cyc = new FieldState.Cycle(0);
        s.ev = t.event != null
            ? new FieldState.EventState(
                s.t + uni(s.rng, t.event.everyMin, t.event.everyMax) / Math.min(dialSetpoint, mMax(t, tt)))
            : null;
        if (t.ergoFeed) s.tde = new FieldState.Crossing(
            s.t + uni(s.rng, GalaxyField.CROSSING_EVERY_MIN, GalaxyField.CROSSING_EVERY_MAX));
        if (t.n >= 7) s.prec = 1;
    }

    /**
     * Schedules the tier's event as {@link #light} does ({@code t + uni(every) / min(dialSetpoint, m_max)}, one RNG
     * draw), without the rest of {@code light()}: the Magnetar Merger lights the magnetar with its flares held back
     * and schedules them when it succeeds (plan §7.6). Clears the timer when the tier has no event.
     */
    public static void scheduleEvent(FieldState s, double dialSetpoint, int tt) {
        final TierParams t = s.tier;
        s.ev = t.event != null
            ? new FieldState.EventState(
                s.t + uni(s.rng, t.event.everyMin, t.event.everyMax) / Math.min(dialSetpoint, mMax(t, tt)))
            : null;
    }

    /**
     * Prototype {@code startBatch()}, field part: Θ +90, P 0, fresh batch accumulators. The caller takes the charge.
     */
    public static void startBatch(FieldState s) {
        s.x[TH] = BATCH_START_TH;
        s.x[P] = 0;
        s.batch = new FieldState.Batch();
    }

    /** Prototype {@code relight()} for T1-T3, field part: the protostar at P −80. Integrity 100, lock 0. */
    public static void beginIgnition(FieldState s) {
        s.setX(IGNITION_START, 0, 0, 0);
        s.I = 100;
        s.lock = 0;
    }

    /**
     * One IGNITING second: P rises +1.5 (not time-scaled, no noise, no trims). At P ≥ −20 the star lights
     * ({@link #light}). Advances the clock. Returns true when it lit: the caller marks the star LIT and charges the
     * first cycle.
     */
    public static boolean stepIgniting(FieldState s, double dialSetpoint, int tt) {
        s.zone = -1;
        s.x[P] = Math.min(s.x[P] + IGNITION_RATE, 0);
        boolean lit = false;
        if (s.x[P] >= IGNITION_LIT) {
            light(s, dialSetpoint, tt);
            lit = true;
        }
        s.t++;
        return lit;
    }

    /** One DARK (or IDLE) second: only the clock moves. */
    public static void stepDark(FieldState s) {
        s.zone = -1;
        s.t++;
    }

    /** Prototype {@code fail()} for a lost star (not a T5 fracture): all six gauges 0, shield 0, lock 0. */
    public static void resetLost(FieldState s) {
        s.setX(0, 0, 0, 0);
        s.A = 0;
        s.lock = 0;
    }

    // ------------------------------------------------------------------ the field second

    /** One LIT second. Mirrors the prototype's {@code step(S)} from the events block to the batch accounting. */
    public static StepResult step(FieldState s, Overlay ov, StepInputs in) {
        final TierParams T = s.tier;
        final double m = in.m, q = in.q, r = in.r, p = in.p;
        final StepResult out = new StepResult();
        out.m = m;

        // ---- events
        final double[] E = new double[GAUGES];
        int warn = 0;
        final EventDef ev = ov.event(T, s);
        if (ev != null && s.ev != null) {
            final FieldState.EventState st = s.ev;
            if (!st.warned && s.t >= st.next - ev.warn) {
                st.warned = true;
                st.sign = ev.signed ? (s.rng.next() < 0.5 ? -1 : 1) : 1;
                if (ev.warn > 0) out.eventWarned = true;
            }
            if (st.warned && st.left == 0 && s.t >= st.next) {
                st.left = ev.dur;
                out.eventHit = true;
            }
            if (st.left > 0) {
                for (int k = 0; k < GAUGES; k++) if (ev.fx(k) != 0) E[k] = E[k] + ev.fx(k) * st.sign * q;
                st.left--;
                if (st.left == 0) {
                    st.warned = false;
                    st.next = (double) (s.t + 1) + uni(s.rng, ev.everyMin, ev.everyMax) / m;
                    if (ev.flip) s.prec = st.sign;
                }
            }
            if (st.warned && ev.warn > 0) warn = st.sign;
        }

        // ---- extra drift d': upkeep sag, T5 plateau, transition swelling
        final double[] dp = new double[GAUGES];
        dp[P] = -5 * (1 - r);
        double sp = 0;
        if (T.batch) {
            sp = spAt(s.batch.eyeT);
            if (sp >= -10 && sp <= 10) dp[TH] += 0.15;
            if (sp > 10 && spAt(s.batch.eyeT + 15 * m) <= 10) {
                warn = 1;
                if (!s.batch.warned) {
                    s.batch.warned = true;
                    out.plateauWarned = true;
                }
            }
        }
        ov.addDrift(s, in, dp);
        // P4: a transition's forced event terms (pericentre passes) and its warning, then the satellite crossing
        final int w2 = ov.addEvents(s, in, E, q, out);
        if (w2 != 0) warn = w2;
        if (T.ergoFeed) crossingTick(s, E, q, ov.running(), out);

        // ---- the gauge update (P5: the T9 beam and flow, and the T10 universe clock, run inside the drift)
        final double[] d = ov.drift(T, s, in, out);
        final int[] gauges = ov.gauges(T);
        for (int k : gauges) {
            final double e = s.x[k] - (k == TH && T.batch ? sp : 0);
            final double c = ov.coupling(T, s.x, k);
            final double xi = (s.rng.next() * 2 - 1) * ov.sigma(T, k, in);
            double dk = d[k];
            if (dk == 0 || Double.isNaN(dk)) dk = 0; // JS (d[k]||0)
            double dv = m * (dk + dp[k] + ov.gain(T, k) * e + c + q * xi);
            for (Map.Entry<TrimDef, Double> tr : in.u.entrySet()) {
                final double f = tr.getKey()
                    .fx(k);
                if (f != 0) dv += f * tr.getValue() / p;
            }
            dv += E[k];
            if (T.galaxy) dv += T.burstTerm(k, s, in); // the prototype's T.bx (0 on P)
            s.x[k] = clamp(s.x[k] + dv, -120, 120);
        }

        // ---- T6 agent shield, after the update
        if (T.shield) {
            s.A = Math
                .min(100, 0.95 * s.A + SHIELD_PER_UNIT * in.units(TrimDef.Kind.AGENT) / p + ov.shieldPerUpkeep(T) * r);
            final double c = Math.min(Math.min(s.A, Math.abs(s.x[PI])), 25);
            s.x[PI] -= Math.signum(s.x[PI]) * c;
            s.A -= c;
        }

        // ---- zone, stability, Harmonic Lock, yield
        final double[] err = new double[GAUGES];
        double worst = 0;
        int wk = gauges[0];
        for (int k : gauges) {
            err[k] = s.x[k] - (k == TH && T.batch ? sp : 0);
            if (Math.abs(err[k]) > worst) {
                worst = Math.abs(err[k]);
                wk = k;
            }
        }
        final int z = zoneOf(worst);
        out.zone = z;
        s.zone = z;
        out.worst = worst;
        out.worstGauge = wk;
        out.err = err;
        out.stability = clamp(100 - worst, 0, 100);
        out.sp = T.batch ? sp : Double.NaN;
        out.warn = warn;
        s.lock = z == 0 ? s.lock + 1 : 0;
        double Y = ZONE_Y[z];
        if (s.lock >= LOCK_SECONDS) Y = LOCK_Y;
        if (ov.ash(T)) {
            final double he = in.units(TrimDef.Kind.HELIUM);
            if (he != 0 && !Double.isNaN(he)) Y *= Math.max(0, 1 - 0.005 * he / p);
        }
        out.Y = Y;
        s.prevE = err;
        s.prevWarn = warn;

        // ---- failure (P5: the Infall and Inflation recover softly, a universe collapses; before the clock moves)
        if (z == 4) {
            out.fail = "Breach, " + GAUGE_SYM[wk] + " " + fmtSigned(err[wk]);
            failRule(s, ov, in, out);
            s.t++;
            return out;
        }
        s.I = clamp(s.I + ZONE_DI[z], 0, 100);
        if (s.I <= 0) {
            out.fail = "Integrity reached 0";
            failRule(s, ov, in, out);
            s.t++;
            return out;
        }

        // ---- the transition (after Integrity, before the cycles: prototype order). A P5 soft fail inside it
        // (recovered) carries on into the per-second terms of the tier below, as the prototype's does.
        ov.tick(s, z, in, out);
        if ((out.fail != null && !out.recovered) || out.supernova) {
            s.t++;
            return out;
        }

        // ---- cycle and batch accounting (the bounce or a P4 transition may have changed the tier: use the current
        // one)
        final TierParams Tn = s.tier;
        // the prototype's S.trans after the transition's second (a success clears it before the per-second terms)
        final boolean trans = ov.running() && !out.transitionSuccess && !out.recovered;
        if (Tn.feed) {
            final double h = in.feed * m * p;
            s.cyc.hsm += h;
            out.hsmToDraw = h;
        }
        if (Tn.galaxy) { // the prototype's perSec: β sum; T8: the Ergomatter feed, then the crossing's end
            s.cyc.bSum += in.beta;
            if (Tn.ergoFeed) {
                if (!trans) {
                    final double f = in.f8 * m * p;
                    s.cyc.f8 += f;
                    out.ergoFed = true;
                    out.ergoToDraw = f;
                }
                crossingPost(s, z, in, out);
            }
            if (Tn.attractor) { // P5 T9: the flow's end, then its warning on the event channel
                AttractorField.flowPost(s, z, in, out);
                if (out.flowEnded && trans) ov.onFlowEnded();
                if (s.flowWarn != 0 && out.warn == 0) out.warn = s.flowWarn;
            }
        }
        if (Tn.universe) UniverseField.perSec(s, Y, in, trans, out);
        final FieldState.Cycle cy = s.cyc;
        cy.ySum += Y;
        cy.n++;
        cy.rMin = Math.min(cy.rMin, r);
        cy.eyeT += m;
        s.eyeT += m;
        if (cy.eyeT >= CYCLE_SECONDS - 1e-9) {
            final double mY = cy.ySum / cy.n;
            out.cycleMeanY = mY;
            out.cycleRMin = cy.rMin;
            if (!Tn.batch && !Tn.planets()) {
                if (Tn.universe) {
                    out.cycleProduct = 0; // the universe's product is per batch
                } else if (Tn.attractor) {
                    out.companionCycle = !trans;
                    out.galaxyCycle = !trans && mY >= 1;
                    out.cycleProduct = trans ? 0 : AttractorField.DM_CYCLE * p * mY * cy.rMin;
                } else if (Tn.galaxy) {
                    out.companionCycle = !trans;
                    out.cycleProduct = trans ? 0
                        : Tn.ergoFeed ? GalaxyField.T8_RATIO * cy.f8 * mY * cy.rMin
                            : Tn.productBase * p
                                * mY
                                * cy.rMin
                                * Math.min(cy.bSum / Math.max(1, cy.n), GalaxyField.BETA_CAP);
                } else {
                    final double base = Tn.feed ? WDM_PER_HSM * cy.hsm : Tn.productBase * p;
                    out.cycleProduct = base * mY * cy.rMin;
                }
                out.hasCycleProduct = true;
            }
            s.cyc = new FieldState.Cycle(cy.eyeT - CYCLE_SECONDS);
            out.cycleEnded = true;
        }
        if (Tn.batch) {
            final FieldState.Batch b = s.batch;
            b.ySum += Y;
            b.n++;
            b.rMin = Math.min(b.rMin, r);
            b.eyeT += m;
            if (b.eyeT >= BATCH_SECONDS - 1e-9) {
                out.batchDone = true;
                out.batchMeanY = b.ySum / b.n;
                out.batchProduct = Tn.productBase * p * out.batchMeanY * b.rMin;
            }
        }
        s.t++;
        return out;
    }

    /**
     * P5: the failure rule inside the field (the prototype's {@code fail} wrapper): a running Infall or Inflation soft
     * fails ({@link Overlay#onFail}); a T10 universe with no transition collapses ({@link UniverseField#collapse}).
     * Everything else is the runtime's (the star goes out).
     */
    static void failRule(FieldState s, Overlay ov, StepInputs in, StepResult out) {
        out.failEarly = true;
        if (ov.running()) {
            ov.onFail(s, in, out);
            return;
        }
        if (s.tier.universe) UniverseField.collapse(s, out);
    }

    /** P5: the time dial does nothing at T9 or T10 (F-P5-3); TecTech's Time generators stop at tier 9. */
    public static boolean dialRetired(TierParams t) {
        return t.n >= 9;
    }

    // ------------------------------------------------------------------ P4: the T8 satellite crossing

    /**
     * Prototype {@code tdeTick}: the 120 s countdown, the hit (300 s, τ from 0), and the P kick
     * {@code 12·(1 + τ/10)^(−5/3)·q} added to E while it runs. Nothing while a transition runs.
     */
    static void crossingTick(FieldState s, double[] E, double q, boolean transRunning, StepResult out) {
        final FieldState.Crossing td = s.tde;
        if (td == null || transRunning) return;
        if (!td.warned && td.left == 0 && s.t >= td.next - GalaxyField.CROSSING_WARN) {
            td.warned = true;
            out.crossingWarned = true;
        }
        if (td.warned && td.left == 0 && s.t >= td.next) {
            td.left = GalaxyField.CROSSING_DUR;
            td.tau = 0;
            td.ok = true;
            td.warned = false;
            out.crossingHit = true;
        }
        if (td.left > 0) E[P] = E[P] + GalaxyField.crossingKick(td.tau) * q;
    }

    /**
     * Prototype {@code tdePost} (per second, after the transition): a crossing in progress is missed once the zone is
     * worse than Stable; at its end a caught crossing pays a bonus cycle and the next one is scheduled (one draw).
     */
    static void crossingPost(FieldState s, int zone, StepInputs in, StepResult out) {
        final FieldState.Crossing td = s.tde;
        if (td == null || td.left <= 0) return;
        if (zone > 1) td.ok = false;
        td.tau++;
        td.left--;
        if (td.left == 0) {
            td.n++;
            if (td.ok) {
                td.caught++;
                out.crossingCaught = true;
                out.crossingBonus = GalaxyField.crossingBonus(in.f8, in.r, in.p);
            } else out.crossingMissed = true;
            td.next = (double) (s.t + 1) + uni(s.rng, GalaxyField.CROSSING_EVERY_MIN, GalaxyField.CROSSING_EVERY_MAX);
        }
    }
}
