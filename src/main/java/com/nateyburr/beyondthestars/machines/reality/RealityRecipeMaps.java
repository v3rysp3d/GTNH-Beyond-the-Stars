package com.nateyburr.beyondthestars.machines.reality;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nonnull;

import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import tectech.recipe.BECAssemblyFrontend;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the three machines' recipe maps (design repo
 * {@code designs/docs/homogeny-reality.md} §1.3, §2.3, §3.3, PROPOSED). Built in a static initialiser that each
 * controller touches at its class init (preInit), so the maps exist before NEI builds its handlers. The process
 * recipes are in {@link RealityProcessRecipes}.
 * <ul>
 * <li>{@link #HOMOGENY}: fluids only, up to 3 in and 1 out. The recipe's special value is the minimum Homogeny tier,
 * shown in NEI as "Homogeny tier N+".</li>
 * <li>{@link #DIFFRACTION}: the medium plus the six region anchors in (input slot k = anchor of region k), the six
 * Entangled Region condensates out (output slot k = region k + 1). The machine reads its amounts from this map's
 * recipe, so a tweak to SL-R03 here is the whole change.</li>
 * <li>{@link #REFAB}: RC-2's Condensate Assembler layout (items with one nanite tier each, one item out), with up to
 * six condensates (one more than GT's four, because the six regions belong together). As GT's map does, the
 * compile step moves the fluid inputs into {@link GTRecipeConstants#CONDENSATE_INPUT}, so recipes are found by
 * their items and the condensates are drawn from the BEC network while the craft runs.</li>
 * </ul>
 */
public final class RealityRecipeMaps {

    private RealityRecipeMaps() {}

    /** SL-H06 / H11 / H21: the Homogeny's mixes. Special value = minimum Homogeny tier (1..3). */
    public static final RecipeMap<RecipeMapBackend> HOMOGENY = RecipeMapBuilder.of("beyondthestars.recipe.homogeny")
        .maxIO(0, 0, 3, 1)
        .minInputs(0, 1)
        .neiSpecialInfoFormatter(
            info -> Arrays.asList(
                "Homogeny tier " + Math.max(1, info.recipe.mSpecialValue) + "+",
                "No overclock: EU/t x parallels"))
        .build();

    /** SL-R03: Entanglement Medium + six anchors -> the six Entangled Regions, into the BEC network. */
    public static final RecipeMap<RecipeMapBackend> DIFFRACTION = RecipeMapBuilder
        .of("beyondthestars.recipe.reality_diffraction")
        .maxIO(0, 0, 7, 6)
        .minInputs(0, 1)
        .neiSpecialInfoFormatter(
            info -> Arrays.asList(
                "Anchors: each in its own Region Anchor socket",
                "An unanchored region yields nothing",
                "Outputs go into the BEC network"))
        .build();

    /** SL-R04 / R05: nanite-tier items + the six regions (drawn from the BEC network) -> one item. */
    public static final RecipeMap<RecipeMapBackend> REFAB = RecipeMapBuilder
        .<RecipeMapBackend>of("beyondthestars.recipe.reality_refab", props -> new RecipeMapBackend(props) {

            @Override
            public @Nonnull GTRecipe compileRecipe(@Nonnull GTRecipe recipe) {
                recipe.getMetadataStorage()
                    .store(GTRecipeConstants.CONDENSATE_INPUT, recipe.mFluidInputs);
                recipe.mFluidInputs = GTValues.emptyFluidStackArray;
                return super.compileRecipe(recipe);
            }
        })
        .maxIO(16, 1, 6, 0)
        .minInputs(1, 0)
        .neiRecipeBackgroundSize(170, 118)
        .neiTransferRect(88, 8, 18, 72)
        .neiTransferRect(124, 8, 18, 72)
        .frontend(BECAssemblyFrontend::new)
        .neiFluidInputsGetter(recipe -> {
            final FluidStack[] stored = recipe.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
            return stored == null ? GTValues.emptyFluidStackArray : stored;
        })
        .neiSpecialInfoFormatter(
            info -> Collections.singletonList("Regions drawn from the BEC network; nanites borrowed"))
        .build();
}
