package com.nateyburr.beyondthestars.machines.kerr;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * SINGULARITY LADDER P2: the Kerr Ergosphere Engine's two OWN recipe maps
 * (singularity-ladder-build.md §3.1). Owned by the Kerr unit.
 * <p>
 * The compressor module runs GT's own {@code compressorRecipes} and {@code neutroniumCompressorRecipes}
 * (the Black Hole Compressor's maps), so it needs no map here. The other two modes are new:
 * <ul>
 * <li>{@link #DENSITY} (DENSIFY mode): Kerr T2 + the compressor module + a hole of at least 128 SM.
 * Holds SL-K05, Hyperdense Stellar Mixture, the Eye T4 gate.</li>
 * <li>{@link #EXTRACTION} (EXTRACT mode): the extractor module. Its own map, so NEI shows exactly
 * what the module does and later phases can add Kerr-only extraction products without touching
 * GT's maps (Q-22).</li>
 * </ul>
 * Built in a static initialiser that {@link MTEKerrEngine} touches at preInit (its class init), so
 * the maps exist before NEI builds its handlers.
 */
public final class KerrRecipeMaps {

    private KerrRecipeMaps() {}

    /** SL-K05: RHSP + molten SpaceTime + Neutronium bolts -> Hyperdense Stellar Mixture. */
    public static final RecipeMap<RecipeMapBackend> DENSITY = RecipeMapBuilder.of("beyondthestars.recipe.kerrdensity")
        .maxIO(2, 1, 2, 1)
        .minInputs(1, 1)
        .build();

    /** SL-K06..: the extractor module's ergosphere extraction. */
    public static final RecipeMap<RecipeMapBackend> EXTRACTION = RecipeMapBuilder
        .of("beyondthestars.recipe.kerrextraction")
        .maxIO(1, 1, 0, 1)
        .minInputs(1, 0)
        .build();
}
