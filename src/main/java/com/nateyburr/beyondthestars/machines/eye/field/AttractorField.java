package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.L;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.V;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.W;

/**
 * P5: Eye T9, the Great Attractor (designs/docs/p5-t9-t10.md §2, §3; the prototype's P5 block {@code attractorTier},
 * {@code beamTick}, {@code flowTick}, {@code flowPost}). Pure: no Minecraft or GT types. The golden test
 * (tools/p5-golden.js) pins every number here to the prototype's.
 * <ul>
 * <li><b>The pellet beam.</b> Pellets fired down the collimator stack: {@code n = cap·clamp(c + k·(s/15 − 0.5), 0, 1)}
 * pellets per second, each of {@code e} GeV; the beam spools toward {@code n·e} in about 20 s,
 * {@code B ← B + (n·e − B)/20}; {@code ρ = B / Π₉}, Π₉ = 10¹⁰ GeV/s. ρ is V's only actuator ({@code −2·ρ}).</li>
 * <li><b>Galactic Flow.</b> Every 1,200-2,400 s a group of 3-12 galaxies is warned 180 s ahead with its side and size;
 * accepted if its size is at most the accept limit, else it streams past. An accepted group surges for 240 s:
 * {@code A = 0.25·n·sin²(πτ/240)·q}, φ = 1 + A. Held Stable or better for all 240 s, all n are captured
 * (+144 L of Dark Matter each); else ⌊n × the fraction⌋. Afterwards the rail's precession sense takes the flow's side.</li>
 * <li><b>The drift.</b> P = 3·(β − 1) + 0.5·A, W = 1.5·s + 0.5·k·A, Λ = 1 + φ, V = 2·φ (× the Infall's ramp).</li>
 * </ul>
 * RNG draws (prototype order): one {@code uni} when the flow timer is first needed (inside the drift, before the gauge
 * noise), the sign and (not for the forced flow) the size at a warning, one {@code uni} when a declined group passes
 * and when a surge ends.
 */
public final class AttractorField {

    private AttractorField() {}

    // ------------------------------------------------------------------ the beam

    /** Π₉: the Attractor's drive, GeV/s (ρ = 1 holds the steady flow). */
    public static final double PI9 = 1e10;
    /** The collars spool the beam toward its setpoint over about this many seconds. */
    public static final double SPOOL = 20;
    /** Dark Matter per 60 s cycle at Y 1, litres (one galaxy's halo, stripped); also per galaxy captured. */
    public static final double DM_CYCLE = 144;
    /** GeV per pellet: Magnetar, Ergomatter, Superdense (grade 1, 2, 3); index 0 = no pellet. */
    public static final double[] GRADE_GEV = { 0, 1.25e9, 5e9, 2e10 };
    /** The prototype's ledger names of the three grades. */
    public static final String[] GRADE_NAME = { "", "Magnetar Pellet", "Ergomatter Pellet", "Superdense Pellet" };
    /** Pellets per second by Injector Barrel Mk (1..3); index 0 = no barrel. */
    public static final double[] BARREL_CAP = { 0, 4, 8, 16 };
    /** V's drift per unit of φ and its beam authority per unit of ρ. */
    public static final double V_GRAV = 2, V_BEAM = 2;
    /** V's runaway gain and noise. */
    public static final double V_GAIN = 0.01, V_SIGMA = 1.5;
    /** The lowest ρ_max the Eye lights T9 with (the GUI names the grade and barrel needed). */
    public static final double LIGHT_RHO_MAX = 1.25;
    /** The beam's analog window on V (the sensor wire straight to the injector), and the two-level rule's ±5. */
    public static final double V_WINDOW = 30, TWO_LEVEL_W = 5;
    /** The pre-fire level the beam is raised to ahead of an accepted flow (debug rigs; the prototype's preFire). */
    public static final double PRE_FIRE_LEVEL = 12;

    /** ρ_max = cap·e/Π₉ for a barrel Mk and a pellet grade. */
    public static double rhoMax(int barrelMk, int grade) {
        return cap(barrelMk) * gev(grade) / PI9;
    }

    /** The hold rating: the largest group the beam can hold, clamp(⌊4·(ρ_max − 2)⌋, 0, 12). */
    public static int holdRating(int barrelMk, int grade) {
        return holdRating(rhoMax(barrelMk, grade));
    }

    public static int holdRating(double rhoMax) {
        return (int) Math.max(0, Math.min(FLOW_MAX, Math.floor(4 * (rhoMax - 2))));
    }

    /** The injector centre that fires ρ = 1 at V = 0 ("auto"): clamp(Π₉ / (e·cap), 0, 1). */
    public static double autoCentre(int barrelMk, int grade) {
        final double e = gev(grade), c = cap(barrelMk);
        if (!(e * c > 0)) return 0.5;
        return StellarField.clamp(PI9 / (e * c), 0, 1);
    }

    public static double gev(int grade) {
        return grade >= 1 && grade <= 3 ? GRADE_GEV[grade] : 0;
    }

    public static double cap(int barrelMk) {
        return barrelMk >= 1 && barrelMk <= 3 ? BARREL_CAP[barrelMk] : 0;
    }

    /** The pellet rate law: n = cap·clamp(c + k·(s/15 − 0.5), 0, 1) pellets per second. */
    public static double rate(double cap, double centre, double gain, double level) {
        return cap * StellarField.clamp(centre + gain * (level / 15 - 0.5), 0, 1);
    }

    /** The beam's level from the V sensor (analog, window ±w): round(15·clamp((V + w)/(2w), 0, 1)). */
    public static int analogLevel(double v, double w) {
        return (int) Math.round(15 * StellarField.clamp((v + w) / (2 * w), 0, 1));
    }

    /** The two-level route (two Directive bits): 15 when V > +5, 0 when V < −5, else 8. */
    public static int twoLevel(double v) {
        return v > TWO_LEVEL_W ? 15 : v < -TWO_LEVEL_W ? 0 : 8;
    }

    /**
     * The prototype's {@code beamTick}: one second of the beam with the inputs' level, centre, gain, cap and grade
     * (n = 0 when the injector has no pellets). Returns the pellets fired this second (fractional).
     */
    public static double beamTick(FieldState s, StepInputs in) {
        FieldState.Beam b = s.beam;
        if (b == null) b = s.beam = new FieldState.Beam();
        double n = rate(in.beamCap, in.beamCentre, in.beamGain, in.beamLevel);
        if (!in.pelletsOk) n = 0;
        b.s = in.beamLevel;
        b.n = n;
        b.set = n * in.beamGeV;
        b.B += (b.set - b.B) / SPOOL;
        b.rho = b.B / PI9;
        return n;
    }

    // ------------------------------------------------------------------ Galactic Flow

    public static final double FLOW_EVERY_MIN = 1200, FLOW_EVERY_MAX = 2400;
    public static final int FLOW_WARN = 180, FLOW_DUR = 240, FLOW_MIN = 3, FLOW_MAX = 12;
    /** The Infall's forced flow: its size, and when it arrives after the begin. */
    public static final int FORCED_SIZE = 6, FORCED_AT = 1200;
    /** A group of n adds A = 0.25·n to φ at its peak. */
    public static final double FLOW_A_PER_GALAXY = 0.25;

    /** sin²(πτ/240): the surge's bell. fdlibm sin and pow (bit-identical to V8). */
    public static double flowBell(int tau) {
        return StrictMath.pow(StrictMath.sin(Math.PI * tau / FLOW_DUR), 2);
    }

    /** Prototype {@code newFlow}: the next group at t + uni(1,200, 2,400) (one draw). */
    public static FieldState.Flow newFlow(FieldState s) {
        final FieldState.Flow f = new FieldState.Flow();
        f.next = s.t + StellarField.uni(s.rng, FLOW_EVERY_MIN, FLOW_EVERY_MAX);
        return f;
    }

    /** Prototype {@code beginInfall}'s flow: the forced, accepted group of 6 arriving 1,200 s on. */
    public static FieldState.Flow forcedFlow(FieldState s) {
        final FieldState.Flow f = new FieldState.Flow();
        f.next = s.t + FORCED_AT;
        f.size = FORCED_SIZE;
        f.accepted = true;
        f.forced = true;
        return f;
    }

    /**
     * Prototype {@code flowTick} (inside the drift): the warning, the arrival (or the pass), the surge. During the
     * Infall ({@code infall}) only the forced flow runs. Returns {A, sign, warn}.
     */
    static double[] flowTick(FieldState s, StepInputs in, boolean infall, StepResult out) {
        FieldState.Flow f = s.flow;
        if (f == null) f = s.flow = newFlow(s);
        final double[] r = { 0, f.sign, 0 };
        if (infall && !f.forced) return r;
        if (!f.warned && f.left == 0 && s.t >= f.next - FLOW_WARN) {
            f.warned = true;
            f.sign = s.rng.next() < 0.5 ? -1 : 1;
            f.size = f.forced ? FORCED_SIZE : (int) Math.round(FLOW_MIN + (FLOW_MAX - FLOW_MIN) * s.rng.next());
            f.accepted = f.forced || f.size <= in.acceptMax;
            out.flowWarned = true;
        }
        if (f.warned && f.left == 0 && s.t >= f.next) {
            f.warned = false;
            if (f.accepted) {
                f.left = FLOW_DUR;
                f.tau = 0;
                f.ok = 0;
                out.flowArrived = true;
            } else {
                f.next = (double) (s.t + 1) + StellarField.uni(s.rng, FLOW_EVERY_MIN, FLOW_EVERY_MAX);
                out.flowPassed = true;
            }
        }
        if (f.left > 0) {
            r[0] = FLOW_A_PER_GALAXY * f.size * flowBell(f.tau) * in.q;
            r[1] = f.sign;
        }
        if (f.warned && f.accepted) r[2] = f.sign;
        return r;
    }

    /**
     * The attractor's drift (the prototype's {@code attractorTier(..).d}): the beam second, the flow second, then
     * P, W, Λ, V. {@code vr} is V's ramp (the Infall's 600 s ramp-in; 1 otherwise), kept for the burst term.
     */
    static double[] drift(FieldState s, StepInputs in, double vr, boolean infall, StepResult out) {
        s.vr = vr;
        out.pellets = beamTick(s, in);
        out.beamFired = true;
        final double[] fl = flowTick(s, in, infall, out);
        s.flowA = fl[0];
        s.flowWarn = (int) fl[2];
        final double A = fl[0], phi = 1 + A;
        final double[] d = new double[StellarField.GAUGES];
        d[P] = GalaxyField.P_PER_BETA * (in.beta - 1) + 0.5 * A;
        d[W] = GalaxyField.W_DRIFT * s.prec + 0.5 * fl[1] * A;
        d[L] = (1 + phi);
        d[V] = V_GRAV * phi * vr;
        return d;
    }

    /** The attractor's burst terms (not × m): W +6·(γ_lo − γ_hi), Λ −10·γ_halo, V −2·ρ·(the ramp). */
    static double burstTerm(int k, FieldState s, StepInputs in) {
        if (k == W) return GalaxyField.W_PER_GAMMA * (in.gLo - in.gHi);
        if (k == L) return GalaxyField.L_PER_HALO * in.gHalo;
        if (k == V) return -V_BEAM * (s.beam == null ? 0 : s.beam.rho) * s.vr;
        return 0;
    }

    /**
     * Prototype {@code flowPost} (per second, after the transition): a surge counts its Stable-or-better seconds; at its
     * end the captured galaxies and their Dark Matter are reported, the precession takes the flow's side (not after the
     * forced flow) and the next group is scheduled (one draw).
     */
    static void flowPost(FieldState s, int zone, StepInputs in, StepResult out) {
        final FieldState.Flow f = s.flow;
        if (f == null || f.left <= 0) return;
        if (zone >= 0 && zone <= 1) f.ok++;
        f.tau++;
        f.left--;
        if (f.left == 0) {
            final double frac = f.ok / (double) FLOW_DUR;
            final double cap = frac >= 1 ? f.size : Math.floor(f.size * frac);
            out.flowEnded = true;
            out.flowHeld = frac >= 1;
            out.flowSize = f.size;
            out.flowCaptured = cap;
            out.flowFraction = frac;
            out.flowDarkMatter = DM_CYCLE * cap * in.r;
            if (!f.forced) s.prec = f.sign;
            f.forced = false;
            f.next = (double) (s.t + 1) + StellarField.uni(s.rng, FLOW_EVERY_MIN, FLOW_EVERY_MAX);
        }
    }

    /** True while a group is warned and accepted (the Flow set's bit 3). */
    public static boolean warnedAccepted(FieldState s) {
        return s.flow != null && s.flow.warned && s.flow.accepted;
    }

    /** Seconds until the warned group arrives (negative while it surges: −τ), or NaN with no flow on the channel. */
    public static double flowIn(FieldState s) {
        final FieldState.Flow f = s.flow;
        if (f == null) return Double.NaN;
        if (f.left > 0) return -f.tau;
        if (f.warned) return f.next - s.t;
        return Double.NaN;
    }
}
