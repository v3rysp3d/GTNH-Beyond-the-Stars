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
 * (long) amounts, time, EU and success in the description. D-49 stars show one 60 s cycle: up to four
 * upkeep fluids in, the product out. The layout copies TecTech's EOH page: the
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
        .maxIO(1, 81, 4, 18)
        .minInputs(1, 0)
        .frontend(EyeFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(330))
        .build();

    /**
     * D-49 (d49-build-plan.md §12 NEI (b)): one page per transition. The star chart that goes in the slot and the
     * core in; the event fluids in; the next star's chart (or, for the First Cooling and the Magnetar Merger, the
     * product) out. The description lists the milestones and the rules. Display only ({@code .fake()}).
     */
    public static final RecipeMap<RecipeMapBackend> TRANSITION = RecipeMapBuilder
        .of("beyondthestars.recipe.eyetransition")
        .maxIO(2, 1, 4, 1)
        .minInputs(1, 0)
        .frontend(TextFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(240))
        .build();

    /**
     * D-49 (plan §12 NEI (c)): one page per star tier, its stability-field trims. The chart in; the trim fluids in,
     * one unit each; the effects per unit and the caps in the description. Display only ({@code .fake()}).
     */
    public static final RecipeMap<RecipeMapBackend> FIELD = RecipeMapBuilder.of("beyondthestars.recipe.eyefield")
        .maxIO(1, 0, 6, 0)
        .minInputs(1, 0)
        .frontend(TextFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(240))
        .build();

    /** The default layout without the energy and duration lines (nothing on these pages is a timed GT run). */
    public static final class TextFrontend extends RecipeMapFrontend {

        public TextFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
            NEIRecipePropertiesBuilder neiPropertiesBuilder) {
            super(uiPropertiesBuilder, neiPropertiesBuilder);
        }

        @Override
        protected void drawEnergyInfo(RecipeDisplayInfo recipeInfo) {}

        @Override
        protected void drawDurationInfo(RecipeDisplayInfo recipeInfo) {}
    }

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
            // D-49: up to 4 inputs (a star's upkeep per cycle), a 2 x 2 grid right of the target
            return UIHelper.getGridPositions(fluidInputCount, 115, 8, 2, 2);
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
