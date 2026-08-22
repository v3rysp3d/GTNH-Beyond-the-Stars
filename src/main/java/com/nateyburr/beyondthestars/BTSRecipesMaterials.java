package com.nateyburr.beyondthestars;

/**
 * D-26 THE MATERIAL LINES - bulk casing manufacture on the Site Batching Plant
 * (docs/production-lines.md S3). <b>Owned exclusively by Agent B (Materials).</b>
 * <p>
 * Empty at the split commit, because there was nothing to move: no bulk casing route existed
 * before 0.24.0. M-01 to M-06 belong here.
 * <p>
 * <b>These recipes sit BESIDE GT own casing recipes and never replace them.</b> Every output is a
 * stock block; what the line sells is 8-32x the batch for a multiblock and a higher voltage. A T5
 * buildout is about 8,000 blocks and 3,773 of them are Light Concrete (118 M-01 batches), which
 * is the single largest friction in building this plant and the entire justification for the
 * machine.
 * <p>
 * <b>The deliberate interlock:</b> M-03b spends {@code BTSItemsFuel.depletedUraniumIngot} - the
 * ENRICHMENT TAILS from F-11c - on Tungstensteel Reinforced Blocks. Depleted uranium is real
 * radiation shielding for exactly the reason the recipe implies, and it makes the fuel line feed
 * the casing line.
 * <p>
 * Recipes go to {@code MTESiteBatchingPlant.RECIPE_MAP}. M-02d is the one exception - a Forming
 * Press recipe on GT own map.
 */
public final class BTSRecipesMaterials {

    private BTSRecipesMaterials() {}

    public static void register() {
        // Nothing yet - see the class comment for what belongs here.
    }
}
