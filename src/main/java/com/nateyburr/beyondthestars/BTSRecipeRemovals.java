package com.nateyburr.beyondthestars;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Multimap;

import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe;
import tectech.recipe.TecTechRecipeMaps;

/**
 * SINGULARITY LADDER RECIPE REMOVALS (singularity-ladder-build.md §4, Contract additions FC-4).
 * Owned by the Foundation unit. This is the first recipe-removal code in the mod.
 *
 * <h2>When it runs</h2>
 *
 * At {@code FMLLoadCompleteEvent}, from {@code CommonProxy.loadComplete}. The {@code @Mod}
 * dependency string says {@code after:dreamcraft;after:tectech}, and FML dispatches load-complete
 * in dependency order, so by the time this runs dreamcraft's assembly lines (the Black Hole
 * Compressor's recipe lives there) have long been registered and TecTech's own
 * {@code onLoadCompleted} has built its Eye of Harmony storage. Nothing that adds recipes runs
 * after load-complete, so nothing can put a removed recipe back.
 *
 * <h2>Where an assembly-line recipe lives</h2>
 *
 * Four places, and a removal that misses one leaves a ghost:
 * <ol>
 * <li>{@code RecipeMaps.assemblylineVisualRecipes} - the NEI page;</li>
 * <li>{@code GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes} - what a data stick resolves against,
 * through the lazily built {@code AssemblyLineUtils.AL_RECIPE_LOOKUP};</li>
 * <li>{@code TecTechRecipeMaps.researchableALRecipeList} and {@code researchStationFakeRecipes} -
 * research-station research and its NEI page;</li>
 * <li>{@code RecipeMaps.scannerFakeRecipes} - scanner research and its NEI page.</li>
 * </ol>
 * If the data-stick lookup was already built, it is cleared and flagged unbuilt by reflection, so it
 * rebuilds from the trimmed list on the next data-stick read.
 *
 * <h2>Containment</h2>
 *
 * Every removal runs inside {@link #guard(String, Runnable)}: a throw (a moved field, a GT update)
 * costs that one removal and logs it, never the boot. Each removal logs its per-collection counts
 * and WARNs if every count is 0, because that means the pack changed underneath us.
 */
public final class BTSRecipeRemovals {

    private BTSRecipeRemovals() {}

    /** Called once from {@code CommonProxy.loadComplete}. */
    public static void run() {
        if (Config.removeBlackHoleCompressorRecipe) {
            guard(
                "Black Hole Compressor controller",
                () -> removeAssemblyLineByOutput(
                    ItemList.Machine_Multi_BlackHoleCompressor.get(1),
                    "Black Hole Compressor controller"));
        } else {
            BeyondTheStars.LOG.info("[BTS] removeBlackHoleCompressorRecipe=false: the BHC recipe is kept.");
        }
        // The TecTech Eye of Harmony removal (replaceTecTechEyeOfHarmony) belongs to the Eye unit's
        // EyeRecipeRegistry, which snapshots TecTech's recipes first and then calls
        // removeAssemblyLineByOutput itself for the controller (§4).
    }

    /**
     * Runs one removal, containing any throw to it. The log names the removal and the stack trace.
     */
    public static void guard(String label, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException | LinkageError failed) {
            BeyondTheStars.LOG.error(
                "[BTS] RECIPE REMOVAL '{}' FAILED and was skipped; the recipe may still be craftable. "
                    + "Everything else continues. Please report this with the stack trace below.",
                label,
                failed);
        }
    }

    /**
     * Removes every assembly-line recipe whose output {@code isItemEqual(out)} from all four places
     * an assembly-line recipe lives (see the class comment), and resets the data-stick lookup.
     *
     * @return the total number of entries removed across all collections
     */
    public static int removeAssemblyLineByOutput(ItemStack out, String label) {
        if (out == null || out.getItem() == null) {
            BeyondTheStars.LOG.warn("[BTS] removed {}: nothing - the output stack is not in this pack.", label);
            return 0;
        }

        // 1. NEI visual page.
        final int visual = removeFromMap(RecipeMaps.assemblylineVisualRecipes, r -> outputMatches(r, out));

        // 2. What data sticks resolve against (includes TecTech's research-station recipes).
        int data = 0;
        for (Iterator<GTRecipe.RecipeAssemblyLine> it = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.iterator(); it
            .hasNext();) {
            final GTRecipe.RecipeAssemblyLine recipe = it.next();
            if (recipe != null && recipe.mOutput != null && recipe.mOutput.isItemEqual(out)) {
                it.remove();
                data++;
            }
        }

        // 3. Research station: the researchable list and its NEI page.
        int research = 0;
        for (Iterator<TecTechRecipeMaps.TTResearchStationALRecipe> it = TecTechRecipeMaps.researchableALRecipeList
            .iterator(); it.hasNext();) {
            final TecTechRecipeMaps.TTResearchStationALRecipe recipe = it.next();
            if (recipe != null && recipe.mOutput != null && recipe.mOutput.isItemEqual(out)) {
                it.remove();
                research++;
            }
        }
        research += removeFromMap(TecTechRecipeMaps.researchStationFakeRecipes, r -> researchMatches(r, out));

        // 4. Scanner research page.
        final int scanner = removeFromMap(RecipeMaps.scannerFakeRecipes, r -> researchMatches(r, out));

        if (data > 0) {
            resetAssemblyLineLookup();
        }

        final int total = visual + data + research + scanner;
        if (total == 0) {
            BeyondTheStars.LOG.warn(
                "[BTS] removed {}: AL-visual 0, AL-data 0, research 0, scanner 0 - NOTHING matched {}. "
                    + "The pack moved this recipe or no longer has it; the removal is a no-op.",
                label,
                out.getDisplayName());
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] removed {}: AL-visual {}, AL-data {}, research {}, scanner {}",
                label,
                visual,
                data,
                research,
                scanner);
        }
        return total;
    }

    // ------------------------------------------------------------------------------------------

    private interface RecipeTest {

        boolean matches(GTRecipe recipe);
    }

    private static int removeFromMap(RecipeMap<?> map, RecipeTest test) {
        if (map == null) {
            return 0;
        }
        final Collection<GTRecipe> all = map.getAllRecipes();
        final List<GTRecipe> doomed = new ArrayList<>();
        for (GTRecipe recipe : all) {
            if (recipe != null && test.matches(recipe)) {
                doomed.add(recipe);
            }
        }
        if (!doomed.isEmpty()) {
            map.getBackend()
                .removeRecipes(doomed);
        }
        return doomed.size();
    }

    private static boolean outputMatches(GTRecipe recipe, ItemStack out) {
        if (recipe.mOutputs == null) {
            return false;
        }
        for (ItemStack stack : recipe.mOutputs) {
            if (stack != null && stack.isItemEqual(out)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A research fake recipe (scanner or research station, built by GT's AssemblyLine emitter and
     * TecTech's TTRecipeAdder) takes the research item and OUTPUTS the assembly line's product, with
     * an NEI data stick as its special item. Match the output; as a fallback, a special data stick
     * that carries the product in its NBT.
     */
    private static boolean researchMatches(GTRecipe recipe, ItemStack out) {
        if (outputMatches(recipe, out)) {
            return true;
        }
        if (recipe.mSpecialItems instanceof ItemStack) {
            final ItemStack stick = (ItemStack) recipe.mSpecialItems;
            if (AssemblyLineUtils.isItemDataStick(stick)) {
                final ItemStack named = AssemblyLineUtils.getDataStickOutput(stick);
                return named != null && named.isItemEqual(out);
            }
        }
        return false;
    }

    /**
     * Clears {@code AssemblyLineUtils.AL_RECIPE_LOOKUP} and sets {@code loadedALRecipes} false, so the
     * next data-stick read rebuilds it from the trimmed {@code sAssemblylineRecipes}. Both are private
     * statics; if GT renames them, the lookup keeps the stale entry until restart and we say so.
     */
    private static void resetAssemblyLineLookup() {
        try {
            final Field loaded = AssemblyLineUtils.class.getDeclaredField("loadedALRecipes");
            loaded.setAccessible(true);
            final Field lookup = AssemblyLineUtils.class.getDeclaredField("AL_RECIPE_LOOKUP");
            lookup.setAccessible(true);
            if (loaded.getBoolean(null)) {
                final Object map = lookup.get(null);
                if (map instanceof Multimap) {
                    ((Multimap<?, ?>) map).clear();
                }
                loaded.setBoolean(null, false);
            }
        } catch (ReflectiveOperationException | RuntimeException moved) {
            BeyondTheStars.LOG.warn(
                "[BTS] could not reset GT's assembly-line data-stick lookup ({}). A data stick written "
                    + "before this point may still resolve a removed recipe until restart.",
                moved.toString());
        }
    }
}
