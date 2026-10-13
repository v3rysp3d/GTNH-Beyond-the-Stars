package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: one trim of one tier (d49-build-plan.md §5, "Trims"). Trims go into the Field sockets only,
 * are never buffered, are drained each second up to {@code cap × p} units, and move the gauges by
 * {@code fx × units / p}. They are not time-scaled.
 * <p>
 * The fluid is named by {@link Kind}; mapping a kind to a real Forge fluid is the runtime's job (plan §5 names the
 * {@code Materials} accessor for each). No Minecraft or GT types here.
 */
public final class TrimDef {

    /** The trim fluids. {@link #key} is the prototype's TRIM key; {@link #unitLitres} is one unit. */
    public enum Kind {

        /** Helium gas ({@code Materials.Helium.getGas}). */
        HELIUM("He", "Helium", 1000),
        /** Hydrogen gas ({@code Materials.Hydrogen.getGas}). */
        HYDROGEN("H", "Hydrogen", 1000),
        /** Super Coolant ({@code Materials.SuperCoolant.getFluid}). */
        SUPER_COOLANT("Cool", "Super Coolant", 100),
        /** Spatially Enlarged Fluid ({@code Materials.Space}, {@code spatialfluid}). */
        SPACE("Space", "Spatially Enlarged Fluid", 100),
        /** Molten SpaceTime ({@code Materials.SpaceTime.getMolten}). */
        SPACETIME("ST", "Molten SpaceTime", 100),
        /** Molten Purified Tengam ({@code Materials.TengamPurified.getMolten}). */
        TENGAM("Tg", "Molten Purified Tengam", 36),
        /** Dimensionally Shifted Superfluid ({@code Materials.DimensionallyShiftedSuperfluid.getFluid}). */
        DSSF("DSSF", "Dimensionally Shifted Superfluid", 100),
        /** Raw Star Matter ({@code Materials.RawStarMatter.getFluid}). */
        RAW_STAR_MATTER("RSPM", "Raw Star Matter", 1000),
        /** Antipolarization Agent (the addon's {@code btsantipolarizationagent}). */
        AGENT("Agent", "Antipolarization Agent", 100);

        public final String key;
        public final String displayName;
        public final int unitLitres;

        Kind(String key, String displayName, int unitLitres) {
            this.key = key;
            this.displayName = displayName;
            this.unitLitres = unitLitres;
        }

        /** The kind with this prototype key, or null. */
        public static Kind byKey(String key) {
            for (Kind k : values()) if (k.key.equals(key)) return k;
            return null;
        }
    }

    public final Kind kind;
    /** Units per second per parallel. */
    public final double cap;
    /** Effect per unit on P, Θ, Ω, Π, W, Λ (index {@link StellarField#P} ..; no trim moves W or Λ). */
    private final double[] fx;
    /** A side note for tooltips and NEI (may be empty). */
    public final String note;

    TrimDef(Kind kind, double cap, double p, double th, double om, double pi, String note) {
        this.kind = kind;
        this.cap = cap;
        this.fx = new double[] { p, th, om, pi, 0, 0 };
        this.note = note == null ? "" : note;
    }

    /** Effect per unit on gauge {@code k}. */
    public double fx(int k) {
        return fx[k];
    }

    @Override
    public String toString() {
        return kind.key;
    }
}
