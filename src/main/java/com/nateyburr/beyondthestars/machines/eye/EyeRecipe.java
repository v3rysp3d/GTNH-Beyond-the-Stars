package com.nateyburr.beyondthestars.machines.eye;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

/**
 * SINGULARITY LADDER P3: one Eye of Harmony process recipe (singularity-ladder-build.md §3.3).
 * Owned by the Eye unit.
 * <p>
 * Not a GT recipe-map recipe: amounts are {@code long} (a Sol run eats a billion litres of each gas),
 * so the Eye keeps its own registry ({@link EyeRecipeRegistry}) the way TecTech's EOH does, and NEI
 * shows a display-only copy. A recipe is either a <b>star</b> (fixed inputs, always succeeds, the
 * field generators do not touch it) or a <b>planet</b> converted from TecTech's EOH (H/He in, ores
 * and plasmas out, success and speed moved by the field generators as in TecTech, Q-20).
 */
public final class EyeRecipe {

    /** An amount of one fluid, in litres, as a long. */
    public static final class FluidAmount {

        public final Fluid fluid;
        public final long amount;

        public FluidAmount(Fluid fluid, long amount) {
            this.fluid = fluid;
            this.amount = amount;
        }
    }

    /** An amount of one item, as a long. {@link #stack} has size 1 and is never handed out. */
    public static final class ItemAmount {

        public final ItemStack stack;
        public final long amount;

        public ItemAmount(ItemStack stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }

    /** Stable id, persisted while a run is in flight (e.g. "star:sol", "planet:Ow", "planet:DD"). */
    public final String key;
    /** Display name for the scanner and NEI. */
    public final String name;
    /** Eye tier the recipe belongs to, 1..4. */
    public final int tier;
    /** The controller-slot item that selects this recipe. Never consumed. */
    public final ItemStack trigger;
    public final List<FluidAmount> inputs;
    public final List<ItemAmount> itemOutputs;
    public final List<FluidAmount> fluidOutputs;
    /** Base duration in ticks, before field generators (planets only). */
    public final long baseTicks;
    /** Wireless EU per run (one parallel), debited when the run starts. */
    public final long euPerRun;
    /** Base success chance; stars are 1.0. */
    public final double baseSuccess;
    /**
     * Planets: the Spacetime Compression field tier (generator meta 0-8) the recipe needs, as
     * TecTech's {@code getSpacetimeCasingTierRequired()}. Stars: -1 (ignore the generators).
     */
    public final int compressionRequired;

    public EyeRecipe(String key, String name, int tier, ItemStack trigger, List<FluidAmount> inputs,
        List<ItemAmount> itemOutputs, List<FluidAmount> fluidOutputs, long baseTicks, long euPerRun, double baseSuccess,
        int compressionRequired) {
        this.key = key;
        this.name = name;
        this.tier = tier;
        this.trigger = trigger;
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.itemOutputs = Collections.unmodifiableList(new ArrayList<>(itemOutputs));
        this.fluidOutputs = Collections.unmodifiableList(new ArrayList<>(fluidOutputs));
        this.baseTicks = baseTicks;
        this.euPerRun = euPerRun;
        this.baseSuccess = baseSuccess;
        this.compressionRequired = compressionRequired;
    }

    /** True for planets (and the Deep Dark), which the field generators act on. */
    public boolean isPlanet() {
        return compressionRequired >= 0;
    }
}
