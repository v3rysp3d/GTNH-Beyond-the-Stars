package com.nateyburr.beyondthestars.machines.kerr;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * SINGULARITY LADDER P2: the Kerr Ergosphere Engine's OWN recipe map (singularity-ladder-build.md
 * §3.1). Owned by the Kerr unit.
 * <p>
 * The modules run GT's own maps wherever one fits: the compressor module runs
 * {@code compressorRecipes} and {@code neutroniumCompressorRecipes} (the Black Hole Compressor's
 * maps), the extractor module runs {@code extractorRecipes} (user ruling: GT's item extractor
 * recipes only, Q-9/Q-22; the earlier Kerr-only "Ergosphere Extraction" map is retired). Only
 * {@link #DENSITY} is new: DENSIFY mode, Kerr T2 + the compressor module + a hole of at least 128 SM,
 * holding SL-K05 (Hyperdense Stellar Mixture, the Eye T4 gate), which no GT machine makes.
 * <p>
 * Built in a static initialiser that {@link MTEKerrEngine} touches at preInit (its class init), so
 * the map exists before NEI builds its handlers. A future module gets a map here only if GT has no
 * machine for what it makes (see {@link KerrModule}).
 */
public final class KerrRecipeMaps {

    private KerrRecipeMaps() {}

    /** SL-K05: RHSP + molten SpaceTime + Neutronium bolts -> Hyperdense Stellar Mixture. */
    public static final RecipeMap<RecipeMapBackend> DENSITY = RecipeMapBuilder.of("beyondthestars.recipe.kerrdensity")
        .maxIO(2, 1, 2, 1)
        .minInputs(1, 1)
        .build();
}
