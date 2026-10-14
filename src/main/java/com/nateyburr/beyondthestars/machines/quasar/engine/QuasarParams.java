package com.nateyburr.beyondthestars.machines.quasar.engine;

/**
 * P4: the Quasar Engine's disc constants and closed-form helpers (quasar-p4.md §1.3-§1.4, §2; the prototype's P4
 * block, designs/quasar-p4-prototype.html: {@code GASES}, {@code COILS}, {@code aEq}, {@code iscoEta},
 * {@code quasarStep}). Pure (no Minecraft or GT types). The golden test (tools/quasar-golden.js) pins every number
 * here to the prototype's: change one and regenerate from an updated prototype, or the test fails.
 * <p>
 * Floating-point expressions keep the prototype's operand order; {@code cbrt} and {@code pow} use
 * {@link StrictMath} (fdlibm, the algorithms V8 uses), so Java and JavaScript agree bit for bit.
 */
public final class QuasarParams {

    private QuasarParams() {}

    // ------------------------------------------------------------------ units and power

    /** GT UXV voltage (EU/t per amp). */
    public static final double UXV_V = 536870912;
    /** 1 Bu/t of bursts thermalised by a crown = 10¹³ EU/t (D-30's 1 %). */
    public static final double EU_PER_BU = 1e13;
    /** Coil power, amps of UXV per b²: 2 A UXV × b² while the hole is open. */
    public static final double COIL_AMPS_PER_B2 = 2;

    /** Coil power at field b, EU/t: 2·UXV·b·b (prototype {@code 2*UXV_V*b*b}). */
    public static double coilEUt(double b) {
        return COIL_AMPS_PER_B2 * UXV_V * b * b;
    }

    // ------------------------------------------------------------------ the hole

    /** The Quasar Seed's hole: 64 SM, spin 0. */
    public static final double SEED_MASS = 64, SEED_SPIN = 0;
    /** The spin cap a_max (Thorne). */
    public static final double A_MAX = 0.998;
    /** Disc Field Coil tiers 1-4: max b = tier; mass caps 256 / 1,024 / 2,048 / 4,096 SM. */
    public static final double[] COIL_CAP = { 0, 256, 1024, 2048, 4096 };
    /** Coil names (Infinity, Transcendent Metal, SpaceTime, MagMatter). */
    public static final String[] COIL_NAME = { "", "Infinity", "Transcendent Metal", "SpaceTime", "MagMatter" };
    /** The field b's GUI range: 0.5 up to the coil tier. */
    public static final double B_MIN = 0.5;
    /** Mass milestones (display only; the questbook and BTSUpgradeTreePanel). */
    public static final double[] MASS_MILESTONES = { 128, 415, 1024, 4096 };

    /** The coil mass cap, SM (tier clamped to 1..4). Without a lattice this is also the structure's top (§10.3). */
    public static double massCap(int coilTier) {
        return COIL_CAP[Math.max(1, Math.min(4, coilTier))];
    }

    /** The equilibrium spin {@code λc / (λc/0.998 + 0.1091·b²)} (0 when λc ≤ 0). Prototype {@code aEq}. */
    public static double aEq(double l, double b) {
        return l > 0 ? l / (l / A_MAX + 0.1091 * b * b) : 0;
    }

    /** Novikov-Thorne efficiency η(a) at the ISCO (bh-model {@code iscoEta}; prototype {@code iscoEta}). */
    public static double iscoEta(double a) {
        final double z1 = 1 + StrictMath.cbrt(1 - a * a) * (StrictMath.cbrt(1 + a) + StrictMath.cbrt(1 - a));
        final double z2 = Math.sqrt(3 * a * a + z1 * z1);
        final double r = 3 + z2 - Math.sqrt((3 - z1) * (3 + z1 + 2 * z2));
        return 1 - Math.sqrt(1 - 2 / (3 * r));
    }

    // ------------------------------------------------------------------ the disc (E, Φ, λ)

    /** The Eddington feed Ṁ_Edd = 40·M/κ L/s (He: 12,800 L/s at 64 SM). */
    public static final double EDD_PER_SM = 40;

    /** Ṁ_Edd for mass {@code M} on {@code gas}, L/s (prototype {@code 40*Q.M/GASES[gas].k}). */
    public static double eddingtonFeed(double M, QuasarGas gas) {
        return EDD_PER_SM * M / gas.kappa;
    }

    /** The feed setpoint f's range and default (GUI). */
    public static final double F_MIN = 0, F_MAX = 1.5, F_DEFAULT = 0.85;
    /** E relaxes toward 100·(f_eff − 1) over 30 s. */
    public static final double E_TAU = 30;
    /** Radiation-pressure runaway above Eddington: +0.04·max(E, 0) per s. */
    public static final double E_RUNAWAY = 0.04;
    /** Noise ξ ~ U(−2, +2) per s. */
    public static final double E_NOISE = 2;
    /** Trim effects per unit on E: the feed gas +2, Spatially Enlarged Fluid −2; cap 10 units per s each. */
    public static final double TRIM_GAS_E = 2, TRIM_SPACE_E = -2, TRIM_CAP = 10;
    /** Trim units scale with M: the feed gas 1,000·(M/64) L, Space 100·(M/64) L per unit. */
    public static final double TRIM_GAS_L = 1000, TRIM_SPACE_L = 100;
    /** Spin relaxes toward a_eq over 1,800 s. */
    public static final double A_TAU = 1800;
    /** Φ charges b·λc/2.4 per s (b = 3: 0 → 100 in 80 s). */
    public static final double PHI_PER_B = 2.4;
    /** A purge needs Φ ≥ 20; the sensor's eruption warning is Φ ≥ 90; Φ = 100 erupts by itself. */
    public static final double PURGE_MIN = 20, ERUPT_WARN = 90, PHI_MAX = 100;
    /** The default regulator purge setpoint (the Directive Disc set's bit 3). */
    public static final double PURGE_AT_DEFAULT = 40;
    /** An eruption: +3·G0·Y_q·Φ/100 Bu/t for 3 s; E −30·Φ/100; a −0.002·Φ/100. */
    public static final double ERUPT_GAIN = 3, ERUPT_E = 30, ERUPT_SPIN = 0.002;
    public static final int ERUPT_SECONDS = 3;
    /** Re-forming after a blow-out: 300 s at f_eff ≥ 0.3. */
    public static final int REFORM_SECONDS = 300;
    public static final double REFORM_MIN_FEED = 0.3;
    /** Growth: dM/dt = 6.4×10⁻⁸ · (1 − η(a)) · λc · Ṁ_Edd SM/s. */
    public static final double GROWTH = 6.4e-8;
    /** Eddington milestone: 1 h Resonant at λ ≥ 0.95. */
    public static final double EDD_MILESTONE_LAMBDA = 0.95;
    public static final int EDD_MILESTONE_SECONDS = 3600;

    /** Feed-gas trim litres per unit at mass M: 1,000·M/64 (prototype order {@code 1000*M/64}). */
    public static double gasTrimUnitLitres(double M) {
        return TRIM_GAS_L * M / 64;
    }

    /** Space trim litres per unit at mass M: 100·M/64. */
    public static double spaceTrimUnitLitres(double M) {
        return TRIM_SPACE_L * M / 64;
    }

    /** G0 = λc · a² · b² · (M/64)² Bu/t (bh-model at λ = 1, J0 = 1). */
    public static double g0(double lc, double a, double b, double M) {
        return lc * a * a * b * b * StrictMath.pow(M / 64, 2);
    }

    /**
     * SMBH (smbh-v1.md §3.1): G0 = λc · a² · b² · 4,096 · X(M, o). At or under 4,096 SM with no Overdrive this IS
     * {@link #g0(double, double, double, double)}, the same expression, so every pre-SMBH number (and the P4 golden
     * file) is unchanged bit for bit; X = (M/4,096)² there, so the two forms are continuous at 4,096 SM.
     */
    public static double g0(double lc, double a, double b, double M, double oEff) {
        if (!(oEff > 0) && M <= M_REF) return g0(lc, a, b, M);
        return lc * a * a * b * b * M_REF * outputX(M, oEff);
    }

    // ------------------------------------------------------------------ SMBH: the Supermassive Lattice (smbh-v1.md)
    // Every number here is a knob of the spec (§10.1); the SMBH golden (tools/smbh-golden.js) pins them to the
    // reference block tools/fixtures/smbh-engine.js.

    /** The reference mass: the MagMatter coil cap, where X = 1 (×1 = today's maxed quasar). */
    public static final double M_REF = 4096;
    /** Lattice tiers L1-L4 (index 0 = no lattice): the node material, the safe limit and the band top, SM (§2.1). */
    public static final String[] LATTICE_NAME = { "", "Superdense Ergomatter", "White Hole Matter", "Universium",
        "Constrained Eternity" };
    public static final double[] LATTICE_SAFE = { 0, 16384, 65536, 262144, 4194304 };
    public static final double[] LATTICE_TOP = { 0, 65536, 262144, 4194304, 67108864 };
    public static final int LATTICE_MAX = 4;
    /** Overdrive needs this lattice tier or higher (it burns Universium, §3.2). */
    public static final int LATTICE_OVERDRIVE = 3;
    /** A lattice counts only on MagMatter coils (the Saturation milestone is held on them, §1.2). */
    public static final int LATTICE_COIL = 4;
    /** Unbound mass is a double, clamped at 2⁵⁰ SM for numeric sanity only (§5.1). */
    public static final double UNBOUND_MAX = 1125899906842624.0;
    /** The intake cap, × Ṁ_Edd ("a max per SM", §2.3), and the hold-up, 3·x × Ṁ_Edd. */
    public static final double INTAKE_CAP = 5, HOLDUP_K = 3;
    /** Without hold-up the over-limit part halves on a log scale every this many seconds (§2.3 leak). */
    public static final double LEAK_HALF_LIFE = 3600;
    /** Overdrive upkeep per second, each of Molten MHDCSM and Molten Universium: 100,000 · o² · 2^(−n) L (§3.2). */
    public static final double OVERDRIVE_UPKEEP = 1e5;
    /** raw = Xm · (1 + 7·o); X = 1 + 99·tanh((raw − 1)/99) past 1, so X never passes ×100 (§3.1). */
    public static final double OVERDRIVE_GAIN = 7, X_CEIL = 100, XM_CEIL = 32;
    /** The GUI / hatch Overdrive step (5 %). */
    public static final double OVERDRIVE_STEP = 0.05;
    /** The Unbound anchor: U(n) = 1,024 · 4^(n²) L/s of Primordial Matter (§5.2). */
    public static final double ANCHOR_BASE = 1024, ANCHOR_GROWTH = 4;
    /** A shed takes 2 % of the mass a second (M × 0.98) down to its target (§2.5). */
    public static final double SHED_RATE = 0.02;
    /**
     * The controller's Accretion Buffer holds this many seconds of Disc + Hold-up (§4.2; not Growth), and the
     * Directive's "feed short" bit comes on under {@link #FEED_SHORT_SECONDS}. Controller-side, not engine maths.
     */
    public static final int ACCRETION_SECONDS = 60, FEED_SHORT_SECONDS = 30;
    /** The Supermassive Link (Eye T9 → T10, §1.3): a source with a lattice and at least this mass, SM. */
    public static final double SUPERMASSIVE_LINK_MASS = 65536;

    /** ln 2, as a literal so the Java and the reference agree on it. */
    public static final double LN2 = 0.6931471805599453;

    /**
     * b^y as exp(y·ln b). The SMBH maths uses this, never {@code pow}, for fractional powers: fdlibm's exp and log
     * (StrictMath) equal V8's bit for bit, its pow does not (about 1 in 10 differ by an ulp), and the golden test
     * compares exactly.
     */
    public static double powE(double b, double y) {
        return StrictMath.exp(y * StrictMath.log(b));
    }

    /**
     * The mass term Xm (§3.1): (M/4,096)² at or under 4,096 SM, then 1 + 31·tanh((√r − 1)/31), r = M/4,096 (about √r
     * at first, flat toward ×32).
     */
    public static double massTerm(double M) {
        final double r = M / M_REF;
        if (r <= 1) return r * r;
        return 1 + (XM_CEIL - 1) * StrictMath.tanh((Math.sqrt(r) - 1) / (XM_CEIL - 1));
    }

    /** The output multiplier X(M, o) (§3.1): raw = Xm·(1 + 7·o); raw itself at or under 1, else the ×100 soft cap. */
    public static double outputX(double M, double o) {
        final double raw = massTerm(M) * (1 + OVERDRIVE_GAIN * o);
        if (raw <= 1) return raw;
        return 1 + (X_CEIL - 1) * StrictMath.tanh((raw - 1) / (X_CEIL - 1));
    }

    /** The lattice tier that counts: {@code lattice} (0-4) on MagMatter coils, else 0. */
    public static int latticeInForce(int coilTier, int lattice) {
        return coilTier >= LATTICE_COIL ? Math.max(0, Math.min(LATTICE_MAX, lattice)) : 0;
    }

    /** The safe limit, SM: the lattice's (§2.1), or the coil cap without one. */
    public static double safeLimit(int coilTier, int lattice) {
        final int L = latticeInForce(coilTier, lattice);
        return L > 0 ? LATTICE_SAFE[L] : massCap(coilTier);
    }

    /** The band top, SM: the lattice's (§2.1), or the coil cap without one. */
    public static double bandTop(int coilTier, int lattice) {
        final int L = latticeInForce(coilTier, lattice);
        return L > 0 ? LATTICE_TOP[L] : massCap(coilTier);
    }

    /**
     * The structure's top, SM: the most mass this structure holds (§10.3): the coil cap without a lattice, the
     * lattice's band top with one, 2⁵⁰ on L4 when Unbound. Debug masses, seeds and saved masses clamp here; a hole
     * above it sheds (open) or will not open (dormant).
     */
    public static double structureTop(int coilTier, int lattice, boolean unbound) {
        final int L = latticeInForce(coilTier, lattice);
        if (L == LATTICE_MAX && unbound) return UNBOUND_MAX;
        return bandTop(coilTier, lattice);
    }

    /**
     * The mass setpoint in force (§2.2): without a lattice, the coil cap (growth stops there, as before the SMBH);
     * with one, {@code setpoint} (0 or less = the default, the safe limit) clamped to 4,096 .. the structure's top.
     */
    public static double massSetpoint(int coilTier, int lattice, boolean unbound, double setpoint) {
        final int L = latticeInForce(coilTier, lattice);
        if (L == 0) return massCap(coilTier);
        final double top = structureTop(coilTier, lattice, unbound);
        if (!(setpoint > 0)) return LATTICE_SAFE[L];
        return Math.max(M_REF, Math.min(top, setpoint));
    }

    /**
     * The band position x (§2.3): 0 at or under the safe limit, log(M/safe)/log(top/safe) inside the band, 1 at the
     * top and above it (Unbound). 0 without a lattice.
     */
    public static double bandX(double M, int coilTier, int lattice) {
        final int L = latticeInForce(coilTier, lattice);
        if (L == 0) return 0;
        final double safe = LATTICE_SAFE[L], top = LATTICE_TOP[L];
        if (M <= safe) return 0;
        if (M >= top) return 1;
        return StrictMath.log(M / safe) / StrictMath.log(top / safe);
    }

    /** The intake cap I_max = 5·Ṁ_Edd, L/s (§2.3). */
    public static double intakeCap(double M, QuasarGas gas) {
        return INTAKE_CAP * eddingtonFeed(M, gas);
    }

    /** The hold-up the band needs, 3·x·Ṁ_Edd L/s (§2.3). */
    public static double holdUpNeed(double x, double medd) {
        return HOLDUP_K * x * medd;
    }

    /** Unbound doublings n = log₂(M / 67,108,864) past the L4 top, 0 at or under it (§5.1). */
    public static double unboundN(double M) {
        final double top = LATTICE_TOP[LATTICE_MAX];
        return M > top ? StrictMath.log(M / top) / LN2 : 0;
    }

    /** Overdrive upkeep, L/s of EACH of Molten MHDCSM and Molten Universium: 100,000 · o² · 2^(−n) (§3.2). */
    public static double overdriveUpkeep(double o, double n) {
        return OVERDRIVE_UPKEEP * o * o * powE(2, -n);
    }

    /** The anchor U(n) = 1,024 · 4^(n²) L/s of Primordial Matter for n &gt; 0 doublings, 0 at n = 0 (§5.2). */
    public static double anchorUpkeep(double n) {
        return n > 0 ? ANCHOR_BASE * powE(ANCHOR_GROWTH, n * n) : 0;
    }

    /** The most doublings {@code litres} of Primordial Matter a second pay for: U(n) ≤ litres (0 under 1,024 L). */
    public static double anchorDoublings(double litres) {
        if (!(litres >= ANCHOR_BASE)) return 0;
        return Math.sqrt(StrictMath.log(litres / ANCHOR_BASE) / StrictMath.log(ANCHOR_GROWTH));
    }

    /** "Anchor short" ignores a cover this close to 1 (the round trip n → U(n) → n is not exact in floating point). */
    public static final double ANCHOR_SLACK = 1e-9;

    /**
     * The most mass {@code litres} of Primordial Matter a second hold Unbound: 67,108,864 · 2^n for the n they pay
     * for (the L4 top under 1,024 L). Unbound growth stops here and a bigger hole sheds to it (§5.2).
     */
    public static double anchorCap(double litres) {
        return LATTICE_TOP[LATTICE_MAX] * powE(2, anchorDoublings(litres));
    }

    /**
     * The disc's growth cap this second: the mass setpoint in force, and when Unbound also the anchor's cap for the
     * Primordial Matter on hand ({@link QuasarInputs#anchorAvailable}). Without a lattice: the coil cap, as always.
     */
    public static double growthCap(QuasarInputs in) {
        final double sp = massSetpoint(in.coilTier, in.lattice, in.unbound, in.massSetpoint);
        if (!(in.unbound && latticeInForce(in.coilTier, in.lattice) == LATTICE_MAX)) return sp;
        return Math.min(sp, anchorCap(in.anchorAvailable));
    }

    /** A shed's time from M down to {@code target}: ln(M/target)/0.0202 s (§2.5). */
    public static double shedSeconds(double M, double target) {
        return M > target ? StrictMath.log(M / target) / -StrictMath.log(1 - SHED_RATE) : 0;
    }

    /** Bu/t → EU/t for a readout (10¹³ each). */
    public static double euPerTick(double bu) {
        return bu * EU_PER_BU;
    }

    /** The steady stream at Φ: G = G0·Y_q·(0.5 + Φ/100). */
    public static double stream(double g0, double yq, double phi) {
        return g0 * yq * (0.5 + phi / 100);
    }

    // ------------------------------------------------------------------ the mean factors (§1.3; crown banks, NEI)

    /** Never purging: the sawtooth plus an eruption every 80 s averages 1.11 · G0 · Y_q. */
    public static final double MEAN_NO_PURGE = 1.11;
    /** Purging small and often at Φ 40: about 0.8 · G0 · Y_q. */
    public static final double MEAN_PURGE_40 = 0.8;

    /** The mean stream, Bu/t, for sizing crown banks and the NEI page: G0·Y_q × 1.11 (no purge) or × 0.8 (Φ 40). */
    public static double meanStream(double g0, double yq, boolean purging) {
        return g0 * yq * (purging ? MEAN_PURGE_40 : MEAN_NO_PURGE);
    }

    /**
     * The most a disc second can carry: a purge (or eruption) at full flux, Φ 100, adds 3·G0·Y_q for its 3 s on top
     * of the reset sawtooth's 0.5·G0·Y_q, so 3.5·G0·Y_q (QuasarDisc.step).
     */
    public static final double PEAK_PURGE = 3.5;

    /** The peak stream, Bu/t: a purge at full flux, G0·Y_q × 3.5, before any throat cap or Junction split. */
    public static double peakStream(double g0, double yq) {
        return g0 * yq * PEAK_PURGE;
    }

    /** The disc's Stability readout: clamp(100 − |E|, 0, 100). */
    public static double stability(double E) {
        final double v = 100 - Math.abs(E);
        return v < 0 ? 0 : v > 100 ? 100 : v;
    }
}
