package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import tectech.recipe.TecTechRecipeMaps;

/**
 * BHC to KERR RECIPE SWAP (GTNH 2.9.0-RC-2 port, user ruling "the Kerr replaces the BHC in recipes").
 *
 * <h2>Why</h2>
 *
 * In 2.9.0-RC-2 the Black Hole Compressor controller stopped being only a machine: every Spacetime
 * Compression Field Generator tier (TecTech, Condensate Assembler) consumes one, and the coremod
 * spends 64 on {@code Compressed_Circuit_Array_Empty}. With {@code removeBlackHoleCompressorRecipe}
 * on, removing the BHC controller recipe alone would leave all of those uncraftable, and with them
 * the Eye of Harmony ladder. So, before the BHC recipe goes, every recipe that consumes a BHC
 * controller is rewritten to consume the Kerr Ergosphere Engine controller instead, same count.
 *
 * <h2>Where it looks</h2>
 * <ul>
 * <li>every GT recipe map ({@code RecipeMap.ALL_RECIPE_MAPS}: the Condensate Assembler, the
 * assembly-line NEI page, the scanner and research-station NEI pages, and anything else), item
 * inputs and the NEI ore-dict alternatives;</li>
 * <li>the assembly-line data lists ({@code sAssemblylineRecipes} and TecTech's
 * {@code researchableALRecipeList}): inputs, alternatives and the research item;</li>
 * <li>the vanilla/Forge crafting table (shaped, shapeless and their ore variants).</li>
 * </ul>
 *
 * <h2>How</h2>
 *
 * The stack is replaced IN PLACE, at the same array index. That keeps everything the recipe carries
 * (metadata, the Condensate Assembler's per-input nanite tiers and its condensate fluids - which
 * that map moves out of {@code mFluidInputs} at compile time, so a remove-and-re-add would lose
 * them - category, duration, EU). A changed map then rebuilds its lookup with {@code reInit()},
 * exactly what GT does after an id remap. Assembly-line data sticks carry only the output, so they
 * resolve to the rewritten recipe; GT's NEI sticks are re-written via
 * {@code RecipeAssemblyLine.reInit()}.
 *
 * <h2>Safety</h2>
 *
 * The rewrite is idempotent. Each place is contained on its own; {@link #run} reports how many
 * consumers are LEFT after the pass, and the caller removes the BHC controller recipe only when that
 * is zero - so no recipe is ever left consuming an item nobody can craft.
 */
public final class BTSBlackHoleSwap {

    private BTSBlackHoleSwap() {}

    /** Recipes handed to the ledger so far, for their ids (SL-K01/swap-01 ...). */
    private static int ledgered;

    /** What one pass did. */
    public static final class Result {

        /** Recipes rewritten on this pass. */
        public int rewritten;
        /** Rewritten recipes per place (map name, "assembly line data", "crafting table"). */
        public final Map<String, Integer> perPlace = new TreeMap<>();
        /** Places whose scan or rebuild threw. */
        public int failures;
        /** Consumers of {@code from} still present after the pass (0 = safe to remove its recipe). */
        public int remaining;
    }

    /**
     * Rewrites every consumer of {@code from} to consume {@code to} (same count), logging each one as
     * {@code [BTS] BHC->Kerr (<phase>): <place>: <output>}, then counts what is left.
     */
    public static Result run(ItemStack from, ItemStack to, String phase) {
        final Result result = new Result();
        swapRecipeMaps(from, to, phase, result);
        swapAssemblyLines(from, to, phase, result);
        swapCraftingTable(from, to, phase, result);
        result.remaining = countConsumers(from);
        return result;
    }

    /** How many recipes anywhere still take {@code stack} as an input or research item. */
    public static int countConsumers(ItemStack stack) {
        int left = 0;
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            try {
                for (GTRecipe recipe : map.getAllRecipes()) {
                    if (recipe != null && (contains(recipe.mInputs, stack) || altContains(recipe, stack))) left++;
                }
            } catch (RuntimeException ignored) {
                // A backend that cannot be walked was already reported by the swap pass.
            }
        }
        for (GTRecipe.RecipeAssemblyLine recipe : assemblyLines()) {
            if (contains(recipe.mInputs, stack) || contains(recipe.mOreDictAlt, stack)
                || matches(recipe.mResearchItem, stack)) left++;
        }
        for (Object recipe : CraftingManager.getInstance()
            .getRecipeList()) {
            if (craftingInputsContain(recipe, stack)) left++;
        }
        return left;
    }

    // ------------------------------------------------------------------------------ GT recipe maps

    private static void swapRecipeMaps(ItemStack from, ItemStack to, String phase, Result result) {
        // Sorted by name so the log reads the same on every boot.
        final Map<String, RecipeMap<?>> maps = new TreeMap<>(RecipeMap.ALL_RECIPE_MAPS);
        for (Map.Entry<String, RecipeMap<?>> entry : maps.entrySet()) {
            final String place = entry.getKey();
            final RecipeMap<?> map = entry.getValue();
            boolean changed = false;
            try {
                for (GTRecipe recipe : map.getAllRecipes()) {
                    if (recipe == null) continue;
                    // The pack's recipe as it was, for /btsrecipes dump (taken only for a consumer).
                    final Map<String, Object> before = contains(recipe.mInputs, from) || altContains(recipe, from)
                        ? CommandBTSRecipes.snapshot(recipe)
                        : null;
                    boolean hit = replace(recipe.mInputs, from, to);
                    if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
                        hit |= replace(((GTRecipe.GTRecipe_WithAlt) recipe).mOreDictAlt, from, to);
                    }
                    if (hit) {
                        changed = true;
                        note(result, phase, place, firstOutput(recipe.mOutputs));
                        BTSRecipeLedger.changed(
                            String.format("SL-K01/swap-%02d", ++ledgered),
                            CommandBTSRecipes.KERR,
                            "removeBlackHoleCompressorRecipe=true",
                            "Black Hole Compressor controller -> Kerr Ergosphere Engine controller (SL-K01), same count",
                            map,
                            recipe,
                            before);
                    }
                }
            } catch (RuntimeException | LinkageError failed) {
                result.failures++;
                BeyondTheStars.LOG
                    .error("[BTS] BHC->Kerr ({}): recipe map {} could not be rewritten.", phase, place, failed);
            } finally {
                // Whatever was already swapped in place must reach the lookup, even after a throw.
                if (changed) rebuild(map, phase, place, result);
            }
        }
    }

    private static void rebuild(RecipeMap<?> map, String phase, String place, Result result) {
        try {
            map.getBackend()
                .reInit();
        } catch (RuntimeException | LinkageError failed) {
            result.failures++;
            BeyondTheStars.LOG
                .error("[BTS] BHC->Kerr ({}): recipe map {} could not rebuild its lookup.", phase, place, failed);
        }
    }

    // ------------------------------------------------------------------------ assembly-line data

    private static void swapAssemblyLines(ItemStack from, ItemStack to, String phase, Result result) {
        try {
            boolean changed = false;
            for (GTRecipe.RecipeAssemblyLine recipe : assemblyLines()) {
                boolean hit = replace(recipe.mInputs, from, to);
                hit |= replace(recipe.mOreDictAlt, from, to);
                if (matches(recipe.mResearchItem, from)) {
                    recipe.mResearchItem = swapped(recipe.mResearchItem, to);
                    hit = true;
                }
                if (hit) {
                    changed = true;
                    note(result, phase, "assembly line data", recipe.mOutput);
                }
            }
            if (changed) {
                // Data sticks hold only the output; refresh GT's NEI sticks so their pages list the Kerr.
                GTRecipe.RecipeAssemblyLine.reInit();
            }
        } catch (RuntimeException | LinkageError failed) {
            result.failures++;
            BeyondTheStars.LOG.error("[BTS] BHC->Kerr ({}): assembly-line data could not be rewritten.", phase, failed);
        }
    }

    /** GT's assembly-line list plus TecTech's research-station list, each recipe once. */
    private static List<GTRecipe.RecipeAssemblyLine> assemblyLines() {
        final Set<GTRecipe.RecipeAssemblyLine> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<GTRecipe.RecipeAssemblyLine> all = new ArrayList<>();
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (recipe != null && seen.add(recipe)) all.add(recipe);
        }
        for (GTRecipe.RecipeAssemblyLine recipe : TecTechRecipeMaps.researchableALRecipeList) {
            if (recipe != null && seen.add(recipe)) all.add(recipe);
        }
        return all;
    }

    // ---------------------------------------------------------------------------- crafting table

    private static void swapCraftingTable(ItemStack from, ItemStack to, String phase, Result result) {
        try {
            for (Object recipe : CraftingManager.getInstance()
                .getRecipeList()) {
                if (replaceCraftingInputs(recipe, from, to)) {
                    note(result, phase, "crafting table", ((IRecipe) recipe).getRecipeOutput());
                }
            }
        } catch (RuntimeException | LinkageError failed) {
            result.failures++;
            BeyondTheStars.LOG
                .error("[BTS] BHC->Kerr ({}): crafting-table recipes could not be rewritten.", phase, failed);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean replaceCraftingInputs(Object recipe, ItemStack from, ItemStack to) {
        if (recipe instanceof ShapedRecipes) {
            return replace(((ShapedRecipes) recipe).recipeItems, from, to);
        }
        if (recipe instanceof ShapelessRecipes) {
            return replaceInList(((ShapelessRecipes) recipe).recipeItems, from, to);
        }
        if (recipe instanceof ShapedOreRecipe) {
            // Ore-dict entries are shared lists owned by the ore dictionary: only plain stacks are swapped.
            final Object[] inputs = ((ShapedOreRecipe) recipe).getInput();
            boolean hit = false;
            for (int i = 0; inputs != null && i < inputs.length; i++) {
                if (inputs[i] instanceof ItemStack && matches((ItemStack) inputs[i], from)) {
                    inputs[i] = swapped((ItemStack) inputs[i], to);
                    hit = true;
                }
            }
            return hit;
        }
        if (recipe instanceof ShapelessOreRecipe) {
            return replaceInList(((ShapelessOreRecipe) recipe).getInput(), from, to);
        }
        return false;
    }

    private static boolean craftingInputsContain(Object recipe, ItemStack stack) {
        if (recipe instanceof ShapedRecipes) return contains(((ShapedRecipes) recipe).recipeItems, stack);
        if (recipe instanceof ShapelessRecipes) return listContains(((ShapelessRecipes) recipe).recipeItems, stack);
        if (recipe instanceof ShapedOreRecipe) {
            for (Object input : ((ShapedOreRecipe) recipe).getInput()) {
                if (input instanceof ItemStack && matches((ItemStack) input, stack)) return true;
            }
            return false;
        }
        if (recipe instanceof ShapelessOreRecipe) return listContains(((ShapelessOreRecipe) recipe).getInput(), stack);
        return false;
    }

    // ------------------------------------------------------------------------------------ helpers

    private static void note(Result result, String phase, String place, ItemStack output) {
        result.rewritten++;
        result.perPlace.merge(place, 1, Integer::sum);
        BeyondTheStars.LOG.info("[BTS] BHC->Kerr ({}): {}: {}", phase, place, describe(output));
    }

    private static boolean matches(ItemStack stack, ItemStack target) {
        return stack != null && stack.getItem() != null
            && stack.getItem() == target.getItem()
            && stack.getItemDamage() == target.getItemDamage();
    }

    /** {@code to} at the replaced stack's size. */
    private static ItemStack swapped(ItemStack old, ItemStack to) {
        final ItemStack copy = to.copy();
        copy.stackSize = old.stackSize;
        return copy;
    }

    private static boolean replace(ItemStack[] stacks, ItemStack from, ItemStack to) {
        if (stacks == null) return false;
        boolean hit = false;
        for (int i = 0; i < stacks.length; i++) {
            if (matches(stacks[i], from)) {
                stacks[i] = swapped(stacks[i], to);
                hit = true;
            }
        }
        return hit;
    }

    private static boolean replace(ItemStack[][] alternatives, ItemStack from, ItemStack to) {
        if (alternatives == null) return false;
        boolean hit = false;
        for (ItemStack[] slot : alternatives) {
            hit |= replace(slot, from, to);
        }
        return hit;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean replaceInList(List list, ItemStack from, ItemStack to) {
        if (list == null) return false;
        boolean hit = false;
        for (int i = 0; i < list.size(); i++) {
            final Object input = list.get(i);
            if (input instanceof ItemStack && matches((ItemStack) input, from)) {
                list.set(i, swapped((ItemStack) input, to));
                hit = true;
            }
        }
        return hit;
    }

    private static boolean contains(ItemStack[] stacks, ItemStack target) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) {
            if (matches(stack, target)) return true;
        }
        return false;
    }

    private static boolean contains(ItemStack[][] alternatives, ItemStack target) {
        if (alternatives == null) return false;
        for (ItemStack[] slot : alternatives) {
            if (contains(slot, target)) return true;
        }
        return false;
    }

    private static boolean altContains(GTRecipe recipe, ItemStack target) {
        return recipe instanceof GTRecipe.GTRecipe_WithAlt
            && contains(((GTRecipe.GTRecipe_WithAlt) recipe).mOreDictAlt, target);
    }

    @SuppressWarnings("rawtypes")
    private static boolean listContains(List list, ItemStack target) {
        if (list == null) return false;
        for (Object input : list) {
            if (input instanceof ItemStack && matches((ItemStack) input, target)) return true;
        }
        return false;
    }

    private static ItemStack firstOutput(ItemStack[] outputs) {
        if (outputs == null) return null;
        for (ItemStack stack : outputs) {
            if (stack != null) return stack;
        }
        return null;
    }

    /**
     * Log text for an output. Server-safe: the formatting codes are stripped by hand, because
     * {@code EnumChatFormatting.getTextWithoutFormattingCodes} is client-only in 1.7.10. Never throws:
     * a log line must not abort a rewrite half-way.
     */
    private static String describe(ItemStack stack) {
        try {
            if (stack == null || stack.getItem() == null) return "(no item output)";
            String name;
            try {
                name = stack.getDisplayName();
            } catch (RuntimeException unnamed) {
                name = stack.getUnlocalizedName();
            }
            return stack.stackSize + " x "
                + (name == null ? "?"
                    : FORMATTING.matcher(name)
                        .replaceAll(""));
        } catch (RuntimeException | LinkageError unnamed) {
            return "(unnamed output)";
        }
    }

    private static final Pattern FORMATTING = Pattern.compile("§.");
}
