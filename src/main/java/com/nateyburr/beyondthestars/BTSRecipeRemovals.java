package com.nateyburr.beyondthestars;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Multimap;
import com.nateyburr.beyondthestars.machines.BTSMachines;

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
 * <h2>The Black Hole Compressor (GTNH 2.9.0-RC-2)</h2>
 *
 * The BHC controller is an ingredient in 2.9 (the Spacetime Compression Field Generators, the
 * coremod's Compressed Circuit Array), so its recipe is removed only AFTER {@link BTSBlackHoleSwap}
 * has moved every consumer onto the Kerr Ergosphere Engine controller, and only if none is left.
 *
 * <h2>Containment</h2>
 *
 * Every removal runs inside {@link #guard(String, Runnable)}: a throw (a moved field, a GT update)
 * costs that one removal and logs it, never the boot. Each removal logs its per-collection counts
 * and WARNs if every count is 0, because that means the pack changed underneath us.
 */
public final class BTSRecipeRemovals {

    private BTSRecipeRemovals() {}

    /**
     * True once the load-complete pass has removed the BHC controller recipe; the server-start pass
     * then catches any BHC consumer registered after load-complete.
     */
    private static boolean blackHoleCompressorRemoved;

    /** Called once from {@code CommonProxy.loadComplete}. */
    public static void run() {
        if (Config.removeBlackHoleCompressorRecipe) {
            guard("Black Hole Compressor -> Kerr Ergosphere Engine", BTSRecipeRemovals::replaceBlackHoleCompressor);
        } else {
            BeyondTheStars.LOG.info("[BTS] removeBlackHoleCompressorRecipe=false: the BHC recipe is kept.");
        }
        // The TecTech Eye of Harmony removal (replaceTecTechEyeOfHarmony) belongs to the Eye unit's
        // EyeRecipeRegistry, which snapshots TecTech's recipes first and then calls
        // removeAssemblyLineByOutput itself for the controller (§4).
    }

    /**
     * GTNH 2.9.0-RC-2: the BHC controller is an INGREDIENT now (every Spacetime Compression Field
     * Generator tier in the Condensate Assembler, the coremod's 64-BHC Compressed Circuit Array).
     * Removing its recipe alone would strand all of them, so (user ruling) every consumer is first
     * rewritten to take the Kerr Ergosphere Engine controller, same count ({@link BTSBlackHoleSwap}),
     * and the BHC recipe goes only if the Kerr is itself craftable and no consumer is left.
     */
    private static void replaceBlackHoleCompressor() {
        final ItemStack bhc = ItemList.Machine_Multi_BlackHoleCompressor.get(1);
        if (bhc == null || bhc.getItem() == null) {
            BeyondTheStars.LOG.warn("[BTS] Black Hole Compressor: not in this pack; nothing to replace or remove.");
            return;
        }
        final ItemStack kerr = BTSMachines.kerrEngine;
        if (kerr == null || kerr.getItem() == null || !hasAssemblyLineFor(kerr)) {
            BeyondTheStars.LOG.error(
                "[BTS] Black Hole Compressor recipe KEPT: the Kerr Ergosphere Engine controller has no assembly-line "
                    + "recipe (SL-K01 skipped?), so nothing that consumes a BHC can be moved onto it. Every BHC "
                    + "recipe and consumer is left exactly as the pack ships it.");
            return;
        }
        final BTSBlackHoleSwap.Result result = BTSBlackHoleSwap.run(bhc, kerr, "load-complete");
        if (result.rewritten == 0) {
            BeyondTheStars.LOG.warn(
                "[BTS] BHC->Kerr: no recipe consumed a Black Hole Compressor controller. GTNH 2.9.0-RC-2 has 9 "
                    + "(Spacetime Compression Field Generator tiers, Condensate Assembler) plus the coremod's "
                    + "Compressed Circuit Array; the pack moved them or no longer has them.");
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] BHC->Kerr: {} recipes now take the Kerr controller: {}",
                result.rewritten,
                result.perPlace);
        }
        if (result.failures > 0 || result.remaining > 0) {
            BeyondTheStars.LOG.error(
                "[BTS] Black Hole Compressor recipe KEPT: {} place(s) failed to rewrite and {} recipe(s) still consume "
                    + "a BHC controller (see the errors above). Removing it would leave them uncraftable.",
                result.failures,
                result.remaining);
            return;
        }
        removeAssemblyLineByOutput(bhc, "Black Hole Compressor controller");
        blackHoleCompressorRemoved = true;
    }

    /**
     * Called from {@code CommonProxy.serverStarting}. Anything that registered a BHC consumer after
     * load-complete (a late script, another mod's server-start hook) is moved onto the Kerr too.
     * Idempotent and silent when there is nothing to do.
     */
    public static void lateBlackHoleCheck() {
        if (!blackHoleCompressorRemoved) return;
        guard("Black Hole Compressor -> Kerr (server start)", () -> {
            final ItemStack bhc = ItemList.Machine_Multi_BlackHoleCompressor.get(1);
            if (BTSBlackHoleSwap.countConsumers(bhc) == 0) return;
            final BTSBlackHoleSwap.Result result = BTSBlackHoleSwap.run(bhc, BTSMachines.kerrEngine, "server start");
            BeyondTheStars.LOG.warn(
                "[BTS] BHC->Kerr: {} recipe(s) consuming a BHC controller appeared after load-complete and now take "
                    + "the Kerr controller; {} still consume a BHC.",
                result.rewritten,
                result.remaining);
        });
    }

    private static boolean hasAssemblyLineFor(ItemStack output) {
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (recipe != null && recipe.mOutput != null && recipe.mOutput.isItemEqual(output)) return true;
        }
        return false;
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
