package com.nateyburr.beyondthestars.items;

/**
 * D-26 MATERIAL LINE ITEMS - the registration point for the casing lines (S3). Owned by
 * <b>Track B (Materials)</b>.
 * <p>
 * <b>It registers nothing, and that is the design rather than an oversight.</b> Every output of
 * M-01 to M-06 is a block that already exists: Light Concrete, the Solid Steel / Clean Stainless /
 * Filter / Radiation Proof / Europium Reinforced / Iridium Reinforced / Radiant Naquadah Alloy /
 * Tungstensteel Turbine casings, Steel Frame Box and Tungstensteel Reinforced Block. The material
 * lines are BULK ROUTES to stock blocks - a multiblock and a higher voltage traded for 8-32x the
 * batch - and inventing a parallel set of BTS-branded casings would break "boost, never replace"
 * and split every structure check in the pack that names those blocks.
 * <p>
 * The one new item M-03b consumes, {@code BTSItemsFuel.depletedUraniumIngot}, is the ENRICHMENT
 * TAILS and belongs to the fuel line that produces it. That interlock is deliberate: the fuel line
 * feeds the casing line.
 * <p>
 * The hook exists so that if Track B ever does need an item, there is exactly one file to add it
 * to and no shared registrar to reopen.
 */
public final class BTSItemsMaterials {

    private BTSItemsMaterials() {}

    public static void register() {
        // Intentionally empty - see the class comment. Add fields and BTSItems.define() calls
        // here if the material lines ever need an item of their own.
    }
}
