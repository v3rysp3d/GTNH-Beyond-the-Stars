package com.nateyburr.beyondthestars;

import net.minecraft.item.ItemStack;

import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * BEC rewrites at load-complete (brief ruling 11a, bec-expansion-proposal §3): RC-2's own Condensate Assembler
 * recipes for TecTech's Reinforced Spatial Structure Casing (SL-B01) and Reinforced Temporal Structure Casing
 * (SL-B02) output 32 instead of 4. They replace the retired batch Assembler recipes SL-P27/P28, which I found
 * "a bit too cheap". Applied whatever {@code replaceTecTechEyeOfHarmony} / {@code removeBlackHoleCompressorRecipe}
 * say, because our Eye needs about 13,900 of these casings either way.
 * <p>
 * Only the output stack size changes, so GT's item-keyed lookup index needs no rebuild, and NEI builds its cache
 * later. Idempotent: a recipe already at 32 is left alone. If RC-2's recipe is missing (a pack that moved it), the
 * casings keep TecTech's research-station recipes and an ERROR says so; the fallback recipes the proposal calls
 * SL-B03/B04 are not built (question B-59).
 */
public final class BTSRecipesBEC {

    /** The rewritten output size (ruling 11a). */
    public static final int CASINGS_PER_CRAFT = 32;

    private BTSRecipesBEC() {}

    /** Called from {@link CommonProxy#loadComplete}, inside {@link BTSRecipeRemovals#guard}. */
    public static void onLoadComplete() {
        rewrite("SL-B01", CustomItemList.EOH_Reinforced_Spatial_Casing, "Reinforced Spatial Structure Casing");
        rewrite("SL-B02", CustomItemList.EOH_Reinforced_Temporal_Casing, "Reinforced Temporal Structure Casing");
    }

    private static void rewrite(String id, CustomItemList casing, String name) {
        final ItemStack want = casing.get(1);
        if (want == null || want.getItem() == null) {
            BeyondTheStars.LOG.error("[BTS] {}: TecTech's {} is not in this pack; nothing to rewrite.", id, name);
            return;
        }
        int hits = 0;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null || recipe.mOutputs.length != 1) continue;
            final ItemStack out = recipe.mOutputs[0];
            if (out == null || !out.isItemEqual(want)) continue;
            hits++;
            final int was = out.stackSize;
            if (was == CASINGS_PER_CRAFT) {
                BeyondTheStars.LOG.info(
                    "[BTS] {}: RC-2 {} (Condensate Assembler) already outputs {}; left alone.",
                    id,
                    name,
                    CASINGS_PER_CRAFT);
                continue;
            }
            recipe.mOutputs[0] = GTUtility.copyAmount(CASINGS_PER_CRAFT, out);
            if (was == 4) {
                BeyondTheStars.LOG.info(
                    "[BTS] {}: RC-2 {} (Condensate Assembler) now outputs {}, was 4.",
                    id,
                    name,
                    CASINGS_PER_CRAFT);
            } else {
                BeyondTheStars.LOG.warn(
                    "[BTS] {}: RC-2 {} (Condensate Assembler) output was {}, not the expected 4; set to {} as ruled.",
                    id,
                    name,
                    was,
                    CASINGS_PER_CRAFT);
            }
        }
        if (hits > 1) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: {} Condensate Assembler recipes make the {}; every one now outputs {}.",
                id,
                hits,
                name,
                CASINGS_PER_CRAFT);
        }
        if (hits == 0) {
            BeyondTheStars.LOG.error(
                "[BTS] {}: no Condensate Assembler recipe makes TecTech's {} in this pack (RC-2 has one in "
                    + "BECRecipes.addEyeOfHarmonyCasings); the casing keeps only TecTech's research-station recipe, "
                    + "so the Eye's shell costs 8x what Beyond the Stars intends.",
                id,
                name);
        }
    }
}
