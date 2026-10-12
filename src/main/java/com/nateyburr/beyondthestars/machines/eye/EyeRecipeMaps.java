package com.nateyburr.beyondthestars.machines.eye;

import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.common.gui.modularui.UIHelper;
import gregtech.nei.RecipeDisplayInfo;

/**
 * SINGULARITY LADDER P3: the Eye of Harmony's NEI page (singularity-ladder-build.md §2.4). Owned by
 * the Eye unit.
 * <p>
 * DISPLAY ONLY. The Eye runs its own registry ({@link EyeRecipeRegistry}) because its amounts are
 * longs; every entry on this page is {@code .fake()} and mirrors one registry recipe, with the true
 * (long) amounts, time, EU and success in the description. The layout copies TecTech's EOH page: the
 * target in the top centre, a 9 x 9 output grid, two rows of output fluids.
 * <p>
 * Built in a static initialiser that {@link MTEEyeOfHarmonyBTS} touches at its class init (preInit),
 * so the map exists before NEI builds its handlers; the recipes arrive at load-complete, as TecTech's
 * own EOH page does.
 */
public final class EyeRecipeMaps {

    private EyeRecipeMaps() {}

    /** NEI page: the target in, everything the run makes out. */
    public static final RecipeMap<RecipeMapBackend> EYE = RecipeMapBuilder.of("beyondthestars.recipe.eye")
        .maxIO(1, 81, 2, 18)
        .minInputs(1, 0)
        .frontend(EyeFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(330))
        .build();

    /** TecTech's EOH layout, with the energy and duration lines suppressed (they are in the text). */
    public static final class EyeFrontend extends RecipeMapFrontend {

        public EyeFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
            NEIRecipePropertiesBuilder neiPropertiesBuilder) {
            super(
                uiPropertiesBuilder.logoPos(new Pos2d(8, 8)),
                neiPropertiesBuilder.recipeBackgroundSize(new Size(170, 241)));
        }

        @Override
        public List<Pos2d> getItemInputPositions(int itemInputCount) {
            return UIHelper.getGridPositions(itemInputCount, 79, 8, 1, 1);
        }

        @Override
        public List<Pos2d> getItemOutputPositions(int itemOutputCount) {
            return UIHelper.getGridPositions(Math.min(itemOutputCount, 81), 7, 44, 9, 9);
        }

        @Override
        public List<Pos2d> getFluidInputPositions(int fluidInputCount) {
            return UIHelper.getGridPositions(fluidInputCount, 115, 8, 2, 1);
        }

        @Override
        public List<Pos2d> getFluidOutputPositions(int fluidOutputCount) {
            return UIHelper.getGridPositions(fluidOutputCount, 7, 206, 9, 2);
        }

        /** The run's EU is a wireless lump, stated in the description. */
        @Override
        protected void drawEnergyInfo(RecipeDisplayInfo recipeInfo) {}

        /** The real duration (minutes, before the field generators) is in the description. */
        @Override
        protected void drawDurationInfo(RecipeDisplayInfo recipeInfo) {}
    }
}
