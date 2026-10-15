package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * P4: the constants of the Eye's galaxy tiers (quasar-p4.md §4; the prototype's P4 block,
 * designs/quasar-p4-prototype.html): the burst demand D_T, the T7 / T8 products and upkeep, the minor merger, the
 * satellite crossing, planets mode and companions. Pure (no Minecraft or GT types). The golden test
 * (tools/quasar-golden.js) pins every number here to the prototype's.
 */
public final class GalaxyField {

    private GalaxyField() {}

    // ------------------------------------------------------------------ burst demand

    /** D₇, Bu/t: the T7 demand, the Stability Link's floor and the halo capture's normaliser. */
    public static final double D7 = 1.5;

    /** D_T = 1.5 · 4^(T−7) Bu/t (T7 1.5, T8 6, T9 24, T10 96). */
    public static double demandT(int tier) {
        return 1.5 * Math.pow(4, tier - 7);
    }

    /** Planets mode draws a quarter of D_T. */
    public static final double PLANETS_DEMAND = 0.25;
    /** Each companion adds a quarter of D_T to the demand and this much to σ_P. */
    public static final double COMPANION_DEMAND = 0.25, COMPANION_SIGMA_P = 0.25;

    /** The demand factor: planets 0.25; galaxy 1 + 0.25 per companion. */
    public static double demandFactor(boolean planets, int companions) {
        return planets ? PLANETS_DEMAND : 1 + COMPANION_DEMAND * companions;
    }

    /** The demand of a lit galaxy-tier Eye (no transition), Bu/t: D_T × {@link #demandFactor}. */
    public static double demand(int tier, boolean planets, int companions) {
        return demandT(tier) * demandFactor(planets, companions);
    }

    /**
     * P5: the demand factor's companion weight for a list of companion tiers: 1 per star, 2 per galaxy (T9's
     * Triangulum and Milky Way, Q-P5-7: a galaxy companion adds 0.5·D₉). Pass it as {@code companions} to
     * {@link #demand}; σ_P still counts companions one each.
     */
    public static int companionWeight(int[] tiers) {
        int w = 0;
        for (int k : tiers) w += Companion.of(k).galaxy ? 2 : 1;
        return w;
    }

    // ------------------------------------------------------------------ the field's burst terms

    /** P drift per unit of (β − 1): galaxy 3, planets mode 1. */
    public static final double P_PER_BETA = 3, P_PER_BETA_PLANETS = 1;
    /** W drift 1.5·s (s the precession sense). */
    public static final double W_DRIFT = 1.5;
    /** W burst authority: +6·(γ_lo − γ_hi). */
    public static final double W_PER_GAMMA = 6;
    /** Λ drift 0.4 per L/s of the Ergomatter feed f₈. */
    public static final double L_PER_F8 = 0.4;
    /** Λ burst authority: −10·γ_halo. */
    public static final double L_PER_HALO = -10;

    // ------------------------------------------------------------------ products and upkeep

    /** T7: Ergomatter per cycle at p = 1, before × mean Y × r × min(β̄, {@link #BETA_CAP}). */
    public static final double T7_ERGOMATTER = 600;
    /** T7: the over-delivery cap on β̄. */
    public static final double BETA_CAP = 1.5;
    /** T7: Molten Magnetar Matter upkeep per cycle (the T6 "touch"). */
    public static final double T7_MAGNETAR_UPKEEP = 7.5;
    /** T8: Superdense Ergomatter per litre of Ergomatter fed (revision 2026-10-13: 1 : 1). */
    public static final double T8_RATIO = 1.0;
    /** T8: the Ergomatter feed f₈ range and default, L/s per parallel. */
    public static final double F8_MIN = 0, F8_MAX = 10, F8_DEFAULT = 5;

    // ------------------------------------------------------------------ events

    /** T7/T8 minor merger: every 900-1,800 Eye-s, 15 s warning with its sign, W ±12 per s for 5 s, then s = sign. */
    public static final EventDef MINOR_MERGER = new EventDef(
        "Minor merger",
        900,
        1800,
        15,
        5,
        new double[] { 0, 0, 0, 0, 12, 0 },
        true,
        true);

    /** T8 satellite crossing: every 1,800-3,600 Eye-s (not dial-scaled), an exact 120 s countdown, 300 s long. */
    public static final double CROSSING_EVERY_MIN = 1800, CROSSING_EVERY_MAX = 3600;
    public static final int CROSSING_WARN = 120, CROSSING_DUR = 300;
    /** The crossing's P kick: 12·(1 + τ/10)^(−5/3) per s, × q. */
    public static final double CROSSING_KICK = 12, CROSSING_TAU = 10, CROSSING_EXP = -5.0 / 3;

    /** The crossing's P kick at τ seconds after the hit (before × q). fdlibm pow: bit-identical to V8's. */
    public static double crossingKick(int tau) {
        return CROSSING_KICK * StrictMath.pow(1 + tau / CROSSING_TAU, CROSSING_EXP);
    }

    /** Caught crossing: +1 cycle of Superdense Ergomatter, T8_RATIO × f₈ × 60 × r (× p). */
    public static double crossingBonus(double f8, double r, double p) {
        return T8_RATIO * f8 * 60 * r * p;
    }

    // ------------------------------------------------------------------ planets mode and companions

    /** Planets mode: slots at T7 / T8 / T9. */
    public static int planetSlots(int tier) {
        return tier == 7 ? 3 : tier == 8 ? 6 : tier == 9 ? 9 : 0;
    }

    /** A planet run's parallel at Eye tier N for a planet of Eye tier k: 2^(N − k). */
    public static double planetParallel(int tier, int k) {
        return Math.pow(2, tier - k);
    }

    /** A planet run's base success per parallel: 1 − 0.05·R (R the rocket tier). */
    public static double planetBase(int rocketTier) {
        return 1 - 0.05 * rocketTier;
    }

    /** The field factor a second in this zone gives a planet run: Resonant / Stable 1, Strained 0.5, else 0. */
    public static final double[] PLANET_ZONE_FACTOR = { 1, 1, 0.5, 0, 0 };

    /** Companions per galaxy tier: T7 1, T8 2, T9 3 (T10 runs none). */
    public static int maxCompanions(int tier) {
        return tier >= 10 ? 0 : Math.max(0, Math.min(3, tier - 6));
    }

    /** The highest tier a companion of an Eye of tier N may be: stars (k ≤ 6) at T7/T8; at T9 also the galaxies 7, 8. */
    public static int maxCompanionTier(int tier) {
        return tier >= 9 ? 8 : 6;
    }

    /** A companion's parallel at Eye tier N for an earlier object of tier k: 2^(N − 1 − k). */
    public static double companionParallel(int tier, int k) {
        return Math.pow(2, tier - 1 - k);
    }

    /**
     * The earlier objects a galaxy can run as companions (prototype {@code COMP}): the product per cycle at
     * parallel 1, and the upkeep per cycle at parallel 1 (× the companion's parallel, × r). Names are the
     * prototype's display names (the runtime maps them to fluids).
     */
    public enum Companion {

        SOL(1, "Sol (T1)", "Raw Star Matter", 50000, new String[] { "Hydrogen", "Helium" }, new double[] { 5e7, 5e7 }),
        SIRIUS_A(2, "Sirius A (T2)", "Excited Stellar Plasma", 2500,
            new String[] { "Hydrogen", "Helium", "Raw Star Matter" }, new double[] { 1.25e7, 1.25e7, 25000 }),
        ARCTURUS(3, "Arcturus (T3)", "Raw Hyperdense Stellar Plasma", 625,
            new String[] { "Hydrogen", "Helium", "Excited Stellar Plasma" }, new double[] { 1.25e7, 1.25e7, 1250 }),
        NEUTRON_STAR(4, "Neutron star (T4)", "White Dwarf Matter", 153.5,
            new String[] { "Hyperdense Stellar Mixture", "Molten SpaceTime" }, new double[] { 41.64, 144 }),
        SIRIUS_B(5, "Sirius B (T5)", "Black Dwarf Matter", 38.4, new String[] { "White Dwarf Matter" },
            new double[] { 153.6 }),
        MAGNETAR(6, "SGR 1806-20 (T6)", "Magnetar Matter", 15,
            new String[] { "White Dwarf Matter", "Black Dwarf Matter", "Antipolarization Agent" },
            new double[] { 60, 15, 3000 }),
        /** P5 (Q-P5-7): at T9 a galaxy may ride the Attractor's flow. Triangulum, ×2 at T9. */
        TRIANGULUM(7, "Triangulum (T7)", "Ergomatter", 600, new String[] { "Magnetar Matter" }, new double[] { 7.5 }),
        /** P5: the Milky Way, ×1 at T9, fed Ergomatter like a T8. */
        MILKY_WAY(8, "Milky Way (T8)", "Superdense Ergomatter", 300, new String[] { "Ergomatter" },
            new double[] { 300 });

        public final int tier;
        public final String label;
        public final String product;
        public final double productPerCycle;
        /** P5: a galaxy (T7 / T8, only beside a T9): weighs 2 in the demand and needs galaxy companions allowed. */
        public final boolean galaxy;
        private final String[] upNames;
        private final double[] upAmounts;

        Companion(int tier, String label, String product, double productPerCycle, String[] upNames,
            double[] upAmounts) {
            this.tier = tier;
            this.label = label;
            this.product = product;
            this.productPerCycle = productPerCycle;
            this.galaxy = tier >= 7;
            this.upNames = upNames;
            this.upAmounts = upAmounts;
        }

        public int upkeepCount() {
            return upNames.length;
        }

        public String upkeepName(int i) {
            return upNames[i];
        }

        public double upkeepAmount(int i) {
            return upAmounts[i];
        }

        /** The companion object of tier k (1..8). */
        public static Companion of(int k) {
            for (Companion c : values()) if (c.tier == k) return c;
            throw new IllegalArgumentException("no companion of tier " + k);
        }
    }
}
