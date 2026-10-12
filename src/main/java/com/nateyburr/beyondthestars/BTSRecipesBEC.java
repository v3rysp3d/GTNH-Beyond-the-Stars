package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
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
 * later. Idempotent: a recipe already at 32 is left alone. A recipe matches when any of its outputs is the casing.
 * <p>
 * RC-2's BEC recipe is the ONLY recipe in the pack that makes these two casings (TecTech's research-station recipe
 * is gone in RC-2), and every Eye part needs them. So if it is missing (a pack that moved it), SL-B03/B04 register
 * RC-2's own recipe at x32 (same inputs, nanite tiers, condensates, 40,000 s at UMV); if even that cannot be built
 * (an input absent), the old Assembler bodies SL-P27/P28 come back as a last resort, so no Eye part can become
 * uncraftable (question B-60).
 */
public final class BTSRecipesBEC {

    /** The rewritten output size (ruling 11a). */
    public static final int CASINGS_PER_CRAFT = 32;

    private BTSRecipesBEC() {}

    /** Called from {@link CommonProxy#loadComplete}, inside {@link BTSRecipeRemovals#guard}. */
    public static void onLoadComplete() {
        if (rewrite("SL-B01", CustomItemList.EOH_Reinforced_Spatial_Casing, "Reinforced Spatial Structure Casing")
            == 0) {
            fallback(true);
        }
        if (rewrite("SL-B02", CustomItemList.EOH_Reinforced_Temporal_Casing, "Reinforced Temporal Structure Casing")
            == 0) {
            fallback(false);
        }
    }

    /** Rewrites every Condensate Assembler recipe making {@code casing}; returns how many there were (-1: no item). */
    private static int rewrite(String id, CustomItemList casing, String name) {
        final ItemStack want = casing.hasBeenSet() ? casing.get(1) : null;
        if (want == null || want.getItem() == null) {
            BeyondTheStars.LOG.error("[BTS] {}: TecTech's {} is not in this pack; nothing to rewrite.", id, name);
            return -1;
        }
        int hits = 0;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null) continue;
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                final ItemStack out = recipe.mOutputs[i];
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
                recipe.mOutputs[i] = GTUtility.copyAmount(CASINGS_PER_CRAFT, out);
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
        }
        if (hits > 1) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: {} Condensate Assembler outputs are the {}; every one now outputs {}.",
                id,
                hits,
                name,
                CASINGS_PER_CRAFT);
        }
        if (hits == 0) {
            BeyondTheStars.LOG.error(
                "[BTS] {}: no Condensate Assembler recipe makes TecTech's {} in this pack (RC-2 has one in "
                    + "BECRecipes.addEyeOfHarmonyCasings), and nothing else makes it: registering Beyond the Stars' "
                    + "fallback so the Eye stays craftable.",
                id,
                name);
        }
        return hits;
    }

    /**
     * SL-B03 (spatial) / SL-B04 (temporal): RC-2's own Condensate Assembler recipe at x32. If an input is missing,
     * SL-P27 / SL-P28 (the retired Assembler recipes, x4) instead.
     */
    private static void fallback(boolean spatial) {
        final String id = spatial ? "SL-B03" : "SL-B04";
        final CustomItemList casing = spatial ? CustomItemList.EOH_Reinforced_Spatial_Casing
            : CustomItemList.EOH_Reinforced_Temporal_Casing;
        final int before = BTSRecipeGuard.skippedCount();
        BTSRecipeGuard.recipe(
            id,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    tectech(CustomItemList.Godforge_SingularityShieldingCasing, 32),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.ProtoHalkonite, 32L),
                    BTSRecipeGuard.req(OrePrefixes.block, Materials.Hexanite, 8L),
                    BTSRecipeGuard.req(OrePrefixes.nanite, Materials.Neutronium, 48L),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Bedrockium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.CosmicNeutronium, 2L),
                    BTSRecipeGuard.nn(GGMaterial.shirabon.get(OrePrefixes.plateSuperdense, 2), "superdense Shirabon"),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Infinity, 2L),
                    BTSRecipeGuard.nn(ItemList.Machine_UV_SolarPanel.get(1L), "ItemList.Machine_UV_SolarPanel"),
                    spatial ? BTSRecipeGuard.nn(ItemList.Quantum_Chest_IV.get(1L), "ItemList.Quantum_Chest_IV")
                        : BTSRecipeGuard.nn(ItemList.AcceleratorUV.get(4L), "ItemList.AcceleratorUV"),
                    BTSRecipeGuard.nn(
                        GTModHandler.getModItem(Mods.GraviSuite.ID, "itemSimpleItem", 64, 3),
                        "GraviSuite itemSimpleItem:3"),
                    BTSRecipeGuard.nn(ItemList.EnergisedTesseract.get(1L), "ItemList.EnergisedTesseract"))
                .fluidInputs(
                    entangled(CondensateType.Neutronium, 512 * INGOTS),
                    entangled(CondensateType.CosmicNeutronium, 512 * INGOTS),
                    entangled(CondensateType.Bedrockium, 256 * INGOTS),
                    entangled(spatial ? CondensateType.Space : CondensateType.Time, 10 * INGOTS))
                .itemOutputs(tectech(casing, CASINGS_PER_CRAFT))
                // RC-2's nanites(4, 1, 1, 2, 1, 1, 4, 1, 1, 1, 1, 2), one per item input.
                .metadata(
                    NANITE_TIERS,
                    new NaniteTier[] { NaniteTier.Transcendent, NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Silver,
                        NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Transcendent, NaniteTier.Carbon,
                        NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Silver })
                .duration(40_000 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(TecTechRecipeMaps.condensateAssemblingRecipes));
        if (BTSRecipeGuard.skippedCount() == before) {
            BeyondTheStars.LOG
                .warn("[BTS] {}: fallback Condensate Assembler recipe registered (x{}).", id, CASINGS_PER_CRAFT);
            return;
        }
        // Last resort: the retired SL-P27/P28 bodies (programmed circuit 1/2 keeps them apart).
        final String last = spatial ? "SL-P27" : "SL-P28";
        BTSRecipeGuard.recipe(
            last,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    tectech(CustomItemList.Godforge_SingularityShieldingCasing, 1),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Neutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.CosmicNeutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Infinity, 1L),
                    GTUtility.getIntegratedCircuit(spatial ? 1 : 2))
                .fluidInputs(BTSRecipeGuard.nn(Materials.SpaceTime.getMolten(576), "molten SpaceTime"))
                .itemOutputs(tectech(casing, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
        BeyondTheStars.LOG
            .error("[BTS] {} could not be built; {} (Assembler, x4) registered as the last resort.", id, last);
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.hasBeenSet() ? entry.get(count) : null, "CustomItemList." + entry.name());
    }

    private static FluidStack entangled(CondensateType type, int amount) {
        return BTSRecipeGuard.nn(type.getEntangled(amount), "entangled " + type.name());
    }
}
