package com.nateyburr.beyondthestars.machines.quasar.engine;

import java.util.Locale;

/**
 * P4: the Quasar Engine's feed gases (quasar-p4.md §1.3: "gas is a strategy"). The opacity κ sets the Eddington feed
 * Ṁ_Edd = 40·M/κ: Helium doubles Hydrogen's, Raw Star Matter doubles it again (the hole grows about 2× faster than on
 * Helium). The same gas is the feed-gas trim (+2 on E per unit). The runtime maps {@link #key} to a Forge fluid
 * (Hydrogen and Helium gas from GT {@code Materials}, Raw Star Matter = {@code Materials.RawStarMatter.getFluid}).
 */
public enum QuasarGas {

    H("H", "Hydrogen", 0.4),
    HE("He", "Helium", 0.2),
    RSM("RSM", "Raw Star Matter", 0.1);

    /** The prototype's key. */
    public final String key;
    public final String displayName;
    /** Opacity κ. */
    public final double kappa;

    QuasarGas(String key, String displayName, double kappa) {
        this.key = key;
        this.displayName = displayName;
        this.kappa = kappa;
    }

    /** The gas with this key or name (any case), or null. */
    public static QuasarGas parse(String s) {
        if (s == null) return null;
        for (QuasarGas g : values()) if (g.key.equalsIgnoreCase(s) || g.name()
            .equals(s.toUpperCase(Locale.ROOT)) || g.displayName.equalsIgnoreCase(s)) return g;
        return null;
    }
}
