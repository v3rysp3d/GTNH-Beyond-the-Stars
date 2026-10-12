package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
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
 * SINGULARITY LADDER P3: THE EYE'S CRAFTING RECIPES, SL-E01 and SL-E02 (singularity-ladder-build.md
 * §2.4). <b>Owned by the Eye unit.</b> Called from one line in {@link BTSRecipes}.
 * <p>
 * The Eye's PROCESS recipes are not here: they are long-sized and live in
 * {@code machines/eye/EyeRecipeRegistry}, built at load-complete.
 *
 * <h2>Deviations from the §2.4 table</h2>
 * <ul>
 * <li><b>SL-E01</b> follows FC-1: the retired Eye Shell ×64 and Containment Deck ×64 lines are dropped
 * (the card already has Reinforced Spatial ×64 and Reinforced Temporal ×64), and Harmonic Lining ×64
 * becomes TecTech's Infinite SpaceTime Energy Boundary Casing ×64.</li>
 * <li><b>Dense SpaceTime</b> (×64, ×32) goes through {@link #denseSpaceTime}: the dense plate if GT
 * has one, else the same mass as SpaceTime blocks (one slot), else {@code BTSRecipesSingularity.dense}'s
 * 9-plates fallback (Q-18). Blocks keep both assembly lines within 16 item slots.</li>
 * </ul>
 */
public final class BTSRecipesEye {

    private BTSRecipesEye() {}

    public static void register() {
        controller();
        galacticCore();
    }

    private static void controller() {
        // SL-E01 Eye of Harmony (controller, MTE base+15). Research: Reinforced Spatial Structure Casing.
        BTSRecipeGuard.recipe(
            "SL-E01",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 1))
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 64),
                        tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 64),
                        tectech(CustomItemList.SpacetimeCompressionFieldGeneratorTier8, 1),
                        tectech(CustomItemList.TimeAccelerationFieldGeneratorTier8, 1),
                        tectech(CustomItemList.StabilisationFieldGeneratorTier8, 1),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 64L),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        denseSpaceTime(64),
                        tectech(CustomItemList.EOH_Infinite_Energy_Casing, 64),
                        BTSRecipeGuard.stack(SLItems.stabiliserPylon, 6)))
                .fluidInputs(
                    molten(Materials.SpaceTime, 1_179_648),
                    nn(Materials.ExcitedDTEC.getFluid(256_000L), "Materials.ExcitedDTEC fluid"),
                    molten(Materials.Infinity, 589_824))
                .itemOutputs(nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony").copy())
                .duration(36_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    private static void galacticCore() {
        // SL-E02 Galactic Upgrade Core (unlocks Eye T4). Research: the Eye controller. HSM is the gate.
        BTSRecipeGuard.recipe(
            "SL-E02",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony").copy())
                .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
                .itemInputs(
                    items(
                        SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                        nn(ItemList.Field_Generator_UMV.get(32L), "ItemList.Field_Generator_UMV"),
                        BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 64L),
                        SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                        denseSpaceTime(32),
                        nn(ItemList.Emitter_UMV.get(16L), "ItemList.Emitter_UMV")))
                .fluidInputs(
                    BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 256_000),
                    molten(Materials.SpaceTime, 147_456),
                    molten(Materials.TranscendentMetal, 147_456))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.galacticUpgradeCore, 1))
                .duration(48_000)
                .eut(TierEU.RECIPE_UMV * 4)
                .addTo(AssemblyLine));
    }

    // ================================================================================ helpers

    /**
     * {@code count} dense SpaceTime plates; if GT never generated them, {@code count} SpaceTime
     * blocks (9 ingots each, the same mass in one slot); else the shared 9-plates fallback.
     */
    private static ItemStack[] denseSpaceTime(int count) {
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

    /** Flattens ItemStacks and ItemStack[]s into one input array (at most 16 slots). */
    private static ItemStack[] items(Object... parts) {
        final List<ItemStack> out = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemStack) {
                out.add((ItemStack) part);
            } else if (part instanceof ItemStack[]) {
                for (ItemStack stack : (ItemStack[]) part) {
                    out.add(stack);
                }
            } else {
                throw new IllegalArgumentException("not an item input: " + part);
            }
        }
        if (out.size() > 16) {
            throw new IllegalStateException("assembly line takes at most 16 item inputs, got " + out.size());
        }
        return out.toArray(new ItemStack[0]);
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static FluidStack molten(Materials material, int litres) {
        return nn(material.getMolten(litres), "molten " + material);
    }
}
