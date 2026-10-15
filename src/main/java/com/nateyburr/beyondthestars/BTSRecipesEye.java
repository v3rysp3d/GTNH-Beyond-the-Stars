package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.recipe.Scanning;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER P3: THE EYE'S CRAFTING RECIPES. Only SL-E01 (the controller) is here now: SL-E02 and D-49's
 * transition cores and sensor hatch, SL-E03..SL-E07 (d49-build-plan.md §10.2), moved to the Condensate Assembler
 * (BEC expansion, ruling 11b: {@link BTSRecipesBEC}, which keeps their old bodies as fallbacks).
 * <b>Owned by the Eye unit.</b> Called from one line in {@link BTSRecipes}.
 * <p>
 * The Eye's PROCESS recipes are not here: they are long-sized and live in
 * {@code machines/eye/EyeRecipeRegistry}, built at load-complete.
 *
 * <h2>Deviations from the §2.4 table</h2>
 * <ul>
 * <li><b>SL-E01</b> follows FC-1: the retired Eye Shell ×64 and Containment Deck ×64 lines are dropped
 * (the card already has Reinforced Spatial ×64 and Reinforced Temporal ×64), and Harmonic Lining ×64
 * becomes TecTech's Infinite SpaceTime Energy Boundary Casing ×64. The three field generators are
 * Tier0 (Crude), not the card's Tier8: under the 1:1 compression gate (Q-20) Crude is the T1 Eye's own
 * gate, and Tier8 would force every generator research step before T1 exists (Q-32).</li>
 * <li><b>Research items</b> are addon-only stacks (SL-E01: the Stabiliser Pylon; SL-E02: the Eye
 * controller). TecTech's own EOH research recipes already use its EOH casings, and the scanner writes
 * only the first recipe sharing a research item.</li>
 * <li><b>Dense SpaceTime</b> (×64, ×32) goes through {@link #denseSpaceTime}: the dense plate if GT
 * has one, else the same mass as SpaceTime blocks (one slot), else {@code BTSRecipesSingularity.dense}'s
 * 9-plates fallback (Q-18). Blocks keep both assembly lines within 16 item slots.</li>
 * </ul>
 */
public final class BTSRecipesEye {

    private BTSRecipesEye() {}

    public static void register() {
        controller();
        // SL-E02..E07 (Galactic Upgrade Core, the four D-49 transition cores, the Stellar Field Sensor Hatch) moved
        // to the Condensate Assembler as SL-B60, B30, B50, B70, B80 and B22 (BTSRecipesBEC, ruling 11b); their old
        // bodies are BTSRecipesBEC's fallbacks.
    }

    private static void controller() {
        // SL-E01 Eye of Harmony (controller, MTE base+15). Research: the Stabiliser Pylon - addon-only,
        // because TecTech's own EOH research recipes already use the Reinforced Spatial Structure Casing
        // and the scanner writes only the FIRST recipe sharing a research item.
        BTSRecipeGuard.recipe(
            "SL-E01",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    BTSRecipeGuard.alInputs(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 64),
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 64),
                        // Tier0 (Crude): the T1 Eye's own compression gate (1:1, Q-20), as TecTech's EOH
                        // controller recipe uses. Tier8 would force all 24 generator research steps first.
                        tectech(CustomItemList.SpacetimeCompressionFieldGeneratorTier0, 1),
                        tectech(CustomItemList.TimeAccelerationFieldGeneratorTier0, 1),
                        tectech(CustomItemList.StabilisationFieldGeneratorTier0, 1),
                        BTSRecipeGuard.anyCircuit(Materials.UMV, 64),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        denseSpaceTime(64),
                        tectech(CustomItemList.EOH_Infinite_Energy_Casing, 64),
                        BTSRecipeGuard.stack(SLItems.stabiliserPylon, 6)))
                // 294,912 L SpaceTime, not the card's 1,179,648 L: the controller chain was 33k SpaceTime
                // ingots against TecTech's controller at about 2k ingots of Time and Space (cost audit #12).
                .fluidInputs(
                    molten(Materials.SpaceTime, 294_912),
                    nn(Materials.ExcitedDTEC.getFluid(256_000L), "Materials.ExcitedDTEC fluid"),
                    molten(Materials.Infinity, 589_824))
                .itemOutputs(nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony").copy())
                .duration(36_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ================================================================================ helpers

    /**
     * {@code count} dense SpaceTime plates; if GT never generated them, {@code count} SpaceTime
     * blocks (9 ingots each, the same mass in one slot); else the shared 9-plates fallback.
     */
    static ItemStack[] denseSpaceTime(int count) {
        if (GTOreDictUnificator.get(OrePrefixes.plateDense, Materials.SpaceTime, 1L) == null) {
            final ItemStack block = GTOreDictUnificator.get(OrePrefixes.block, Materials.SpaceTime, count);
            if (block != null) return new ItemStack[] { block };
        }
        return BTSRecipesSingularity.dense(Materials.SpaceTime, count);
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        if (!entry.hasBeenSet()) return nn(null, "CustomItemList." + entry.name());
        return nn(entry.get(count), "CustomItemList." + entry.name());
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static FluidStack molten(Materials material, int litres) {
        return nn(material.getMolten(litres), "molten " + material);
    }
}
