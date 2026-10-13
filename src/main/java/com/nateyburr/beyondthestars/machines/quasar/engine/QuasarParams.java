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

    /** The coil mass cap, SM (tier clamped to 1..4). */
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
