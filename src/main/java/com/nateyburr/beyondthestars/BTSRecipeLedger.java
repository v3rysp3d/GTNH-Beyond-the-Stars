package com.nateyburr.beyondthestars;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import tectech.recipe.EyeOfHarmonyRecipe;

/**
 * THE RECIPE LEDGER: which live recipe is ours, and what we did to the pack's. Read by {@code /btsrecipes dump}
 * ({@link CommandBTSRecipes}), which walks the LIVE recipe maps after load-complete and looks every recipe up here;
 * nothing here is a list of what the source meant to register.
 * <p>
 * <b>How a recipe becomes "ours".</b> {@link BTSRecipeGuard#recipe(String, Runnable)} is the one door every recipe of
 * the mod goes through. Around each body it notes the size of every category list of every GT recipe map, GT's
 * assembly-line data list and the crafting table; whatever those gained while the body ran was added by that id. The
 * recipe objects themselves are remembered (by identity), so a recipe GT later dropped, or one a load-complete pass
 * removed, simply is not found by the dump. Nested guard calls are attributed to the innermost id.
 * <p>
 * <b>What we did to the pack's recipes</b> is reported by the code that does it: {@link #changed} (a GT recipe
 * rewritten in place, with a snapshot of it before), {@link #removed} (a GT/pack recipe taken out, with what replaces
 * it) and {@link #tecTechPlanet} (TecTech's Eye of Harmony planets, converted to our Eye).
 * <p>
 * <b>Marks:</b> {@link #gated} (the config flag a block of registrations depends on), {@link #fallback} (a recipe
 * registered only because the preferred one could not be) and {@link #placeholder} (registered and visible in NEI,
 * but unreachable until a later phase makes an input). Registration is single-threaded (postInit, load-complete), so
 * plain statics are enough. Costs nothing at run time: it is filled once at load and only read by the command.
 */
public final class BTSRecipeLedger {

    private BTSRecipeLedger() {}

    /** One live recipe added by one guarded id. Exactly one of {@code gt}, {@code crafting}, {@code line} is set. */
    public static final class Added {

        public final String id;
        /** The {@code BTSRecipeGuard.line} it ran in, or null (load-complete). */
        public final String line;
        public final String flag;
        public final RecipeMap<?> map;
        public final GTRecipe gt;
        public final IRecipe crafting;
        public final GTRecipe.RecipeAssemblyLine assemblyLine;

        Added(String id, String line, String flag, RecipeMap<?> map, GTRecipe gt, IRecipe crafting,
            GTRecipe.RecipeAssemblyLine assemblyLine) {
            this.id = id;
            this.line = line;
            this.flag = flag;
            this.map = map;
            this.gt = gt;
            this.crafting = crafting;
            this.assemblyLine = assemblyLine;
        }
    }

    /** A GT/pack recipe rewritten in place. {@code before} is a serialised snapshot taken before the edit. */
    public static final class Changed {

        public final String id, group, flag, note;
        public final RecipeMap<?> map;
        public final GTRecipe recipe;
        public final Map<String, Object> before;

        Changed(String id, String group, String flag, String note, RecipeMap<?> map, GTRecipe recipe,
            Map<String, Object> before) {
            this.id = id;
            this.group = group;
            this.flag = flag;
            this.note = note;
            this.map = map;
            this.recipe = recipe;
            this.before = before;
        }
    }

    /** A GT/pack recipe removed. The recipe object is kept (it is out of every map) so the dump can show it. */
    public static final class Removed {

        public final String id, group, flag, replacement, note;
        public final RecipeMap<?> map;
        public final GTRecipe recipe;

        Removed(String id, String group, String flag, String replacement, String note, RecipeMap<?> map,
            GTRecipe recipe) {
            this.id = id;
            this.group = group;
            this.flag = flag;
            this.replacement = replacement;
            this.note = note;
            this.map = map;
            this.recipe = recipe;
        }
    }

    private static final List<Added> ADDED = new ArrayList<>();
    private static final Map<Object, Added> BY_OBJECT = new IdentityHashMap<>();
    private static final Set<Object> CLAIMED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<String> GUARDED = new ArrayList<>();
    private static final List<Changed> CHANGED = new ArrayList<>();
    private static final Map<Object, Changed> CHANGED_BY_RECIPE = new IdentityHashMap<>();
    private static final List<Removed> REMOVED = new ArrayList<>();
    private static final Map<String, String> FALLBACKS = new LinkedHashMap<>();
    private static final Map<String, String> PLACEHOLDERS = new LinkedHashMap<>();
    private static final Map<String, EyeOfHarmonyRecipe> TECTECH_PLANETS = new LinkedHashMap<>();
    private static final Deque<Snapshot> OPEN = new ArrayDeque<>();

    private static String line;
    private static String flag;

    // ------------------------------------------------------------------------------- the guard's hooks

    /** Size of everything a recipe body can add to, taken before it runs. */
    private static final class Snapshot {

        final Map<Collection<GTRecipe>, Integer> lists = new IdentityHashMap<>();
        final int crafting;
        final int assemblyLines;

        Snapshot() {
            for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
                for (Collection<GTRecipe> list : categories(map)) lists.put(list, list.size());
            }
            crafting = CraftingManager.getInstance()
                .getRecipeList()
                .size();
            assemblyLines = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.size();
        }
    }

    private static Collection<Collection<GTRecipe>> categories(RecipeMap<?> map) {
        try {
            return map.getBackend()
                .getRecipeCategoryMap()
                .values();
        } catch (RuntimeException | LinkageError odd) {
            return Collections.emptyList();
        }
    }

    /** Called by {@link BTSRecipeGuard#line} around each production line. */
    static void line(String name) {
        line = name;
    }

    /** Called by {@link BTSRecipeGuard#recipe} before the body. Never throws. */
    static void begin() {
        try {
            OPEN.push(new Snapshot());
        } catch (RuntimeException | LinkageError odd) {
            OPEN.push(null);
        }
    }

    /** Called by {@link BTSRecipeGuard#recipe} after the body, whatever happened. Never throws. */
    static void end(String id, boolean ok) {
        final Snapshot before = OPEN.isEmpty() ? null : OPEN.pop();
        if (!ok || before == null) return;
        try {
            GUARDED.add(id);
            for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
                for (Collection<GTRecipe> list : categories(map)) {
                    final Integer was = before.lists.get(list);
                    final int skip = was == null ? 0 : was;
                    if (list.size() <= skip) continue;
                    int i = 0;
                    for (GTRecipe recipe : list) {
                        if (i++ < skip || recipe == null || !CLAIMED.add(recipe)) continue;
                        add(new Added(id, line, flag, map, recipe, null, null), recipe);
                    }
                }
            }
            final List<?> crafting = CraftingManager.getInstance()
                .getRecipeList();
            for (int i = before.crafting; i < crafting.size(); i++) {
                final Object recipe = crafting.get(i);
                if (recipe instanceof IRecipe && CLAIMED.add(recipe)) {
                    add(new Added(id, line, flag, null, null, (IRecipe) recipe, null), recipe);
                }
            }
            final List<GTRecipe.RecipeAssemblyLine> lines = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes;
            for (int i = before.assemblyLines; i < lines.size(); i++) {
                final GTRecipe.RecipeAssemblyLine recipe = lines.get(i);
                if (recipe != null && CLAIMED.add(recipe)) {
                    add(new Added(id, line, flag, null, null, null, recipe), recipe);
                }
            }
        } catch (RuntimeException | LinkageError odd) {
            BeyondTheStars.LOG.warn("[BTS] recipe ledger: could not attribute {} ({}).", id, odd.toString());
        }
    }

    private static void add(Added added, Object key) {
        ADDED.add(added);
        BY_OBJECT.put(key, added);
    }

    // ------------------------------------------------------------------------------------ the marks

    /** Runs {@code body} with every recipe it registers marked as needing {@code flagSetting} (e.g. "x=true"). */
    public static void gated(String flagSetting, Runnable body) {
        final String outer = flag;
        flag = flagSetting;
        try {
            body.run();
        } finally {
            flag = outer;
        }
    }

    /** {@code id} is registered only because the preferred recipe could not be; {@code why} says which. */
    public static void fallback(String id, String why) {
        FALLBACKS.put(id, why);
    }

    /** Recipes whose id is {@code id} (or starts with it) are registered but unreachable until {@code why}. */
    public static void placeholder(String id, String why) {
        PLACEHOLDERS.merge(id, why, (a, b) -> a + "; " + b);
    }

    /**
     * A GT/pack recipe rewritten in place. Call {@link CommandBTSRecipes#snapshot} BEFORE the edit and pass it as
     * {@code before}. Recording the same recipe twice keeps the first snapshot (the pack's original).
     */
    public static void changed(String id, String group, String flagSetting, String note, RecipeMap<?> map,
        GTRecipe recipe, Map<String, Object> before) {
        if (recipe == null || CHANGED_BY_RECIPE.containsKey(recipe)) return;
        final Changed changed = new Changed(id, group, flagSetting, note, map, recipe, before);
        CHANGED.add(changed);
        CHANGED_BY_RECIPE.put(recipe, changed);
    }

    /** A GT/pack recipe removed from {@code map}; {@code replacement} names what takes its place. */
    public static void removed(String id, String group, String flagSetting, String replacement, String note,
        RecipeMap<?> map, GTRecipe recipe) {
        if (recipe != null) REMOVED.add(new Removed(id, group, flagSetting, replacement, note, map, recipe));
    }

    /** TecTech's Eye of Harmony recipe for {@code key} ("planet:Ow") as it was before our conversion. */
    public static void tecTechPlanet(String key, EyeOfHarmonyRecipe source) {
        if (source != null) TECTECH_PLANETS.put(key, source);
    }

    // ------------------------------------------------------------------------------------- read side

    static Added added(Object liveRecipe) {
        return BY_OBJECT.get(liveRecipe);
    }

    static List<Added> allAdded() {
        return Collections.unmodifiableList(ADDED);
    }

    static List<String> guardedIds() {
        return Collections.unmodifiableList(GUARDED);
    }

    static List<Changed> allChanged() {
        return Collections.unmodifiableList(CHANGED);
    }

    static List<Removed> allRemoved() {
        return Collections.unmodifiableList(REMOVED);
    }

    static Map<String, String> fallbacks() {
        return Collections.unmodifiableMap(FALLBACKS);
    }

    static Map<String, String> placeholders() {
        return Collections.unmodifiableMap(PLACEHOLDERS);
    }

    static Map<String, EyeOfHarmonyRecipe> tecTechPlanets() {
        return Collections.unmodifiableMap(TECTECH_PLANETS);
    }
}
