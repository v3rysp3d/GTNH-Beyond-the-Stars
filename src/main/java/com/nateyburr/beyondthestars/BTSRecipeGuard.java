package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * THE REGISTRATION GUARD. <b>A missing component costs ONE SKIPPED RECIPE, never the game.</b>
 *
 * <h2>The bug this exists to make survivable</h2>
 *
 * 0.24.0 did not boot. {@code GTOreDictUnificator.get(OrePrefixes.dust, Materials.Fluorite, 8L)}
 * compiles - {@code Materials.Fluorite} is a real field - but it answers <b>null</b> at runtime,
 * because Fluorite is absent from {@code MaterialsIDMap} and so never gets a meta-item generated
 * for it no matter what {@code loadFluorite()} asks for. GT's own builder then threw
 * {@code IllegalArgumentException: null in argument} out of
 * {@code GTRecipeBuilder.handleNullRecipeComponents}, that escaped {@code CommonProxy.postInit},
 * and the whole mod - four production lines, every machine, every item - died over one dust.
 * <p>
 * <b>A recipe that compiles can still be null at registration.</b> A source review cannot catch
 * this class of bug; only the runtime can. So the answer is not to audit harder, it is to make the
 * failure cheap. There are ~138 OreDict lookups in the five recipe files and any of them can turn
 * null when the pack, a dependency or a GT version moves underneath us. Before this guard, the
 * first one to do so killed postInit and the player's only diagnostic was a crash log naming a
 * single line. Fixing them one crash-and-relaunch at a time is a night's work per missing material.
 *
 * <h2>How it behaves</h2>
 *
 * Every recipe in the five files is wrapped in {@link #recipe(String, Runnable)}, and every
 * component lookup inside goes through a resolver here. When a resolver cannot resolve:
 * <ol>
 * <li>it logs an ERROR naming the exact prefix, material and amount, the design code of the recipe
 * (F-11c, M-03b, P-02a ...) and the {@code File.java:line} it happened on, recovered from the
 * stack trace;</li>
 * <li>it raises {@link MissingComponent}, which unwinds the recipe's own builder chain <b>before
 * GT is ever handed the null</b> and is caught by {@link #recipe(String, Runnable)};</li>
 * <li>registration carries straight on with the next recipe.</li>
 * </ol>
 * {@link MissingComponent} is a private control-flow signal and <b>never escapes this class</b>.
 * From a caller's point of view the recipe simply does not register. It is deliberately raised at
 * the point of the failed lookup rather than returned as a placeholder value, because a placeholder
 * would go on to be registered as a real, wrong, craftable recipe - a far worse outcome than a
 * missing one.
 *
 * <h2>The catch-all is the load-bearing half</h2>
 *
 * {@link #recipe(String, Runnable)} also catches every other {@code RuntimeException} and
 * {@code LinkageError}. That is what makes the guarantee total rather than partial: a null
 * {@code Fluid} passed to a {@code FluidStack} constructor NPEs, a missing {@code GregtechItemList}
 * entry returns null and trips GT's own null check, a dependency that changed its API throws
 * {@code NoSuchMethodError} - all of them are one skipped recipe with a logged file:line, none of
 * them is a dead game. The named resolvers below exist to make the log message <i>precise</i>; the
 * catch-all is what makes it <i>safe</i>.
 * <p>
 * {@link #line(String, Runnable)} applies the same containment one level up, around each whole
 * production line, so that a failure in code that is not inside any recipe - a helper, a static
 * lookup, an OreDict registration - costs one production line rather than all five.
 *
 * <h2>The summary is the point</h2>
 *
 * {@link #summary()} logs how many recipes registered, how many were skipped, and every skipped
 * one with the component it could not find. <b>That single block turns a night of
 * crash-and-relaunch into one pass:</b> boot once, read the list, fix everything on it.
 *
 * @see BTSRecipes#register()
 */
public final class BTSRecipeGuard {

    private BTSRecipeGuard() {}

    /**
     * The control-flow signal raised by a failed resolver. Private to this file by convention:
     * it is caught by {@link #recipe(String, Runnable)} and {@link #line(String, Runnable)} and
     * never escapes to a caller. It keeps its stack trace, because the stack trace is where the
     * {@code File.java:line} in the log message comes from.
     */
    static final class MissingComponent extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /** Human description of the component that could not be resolved. */
        final String component;

        MissingComponent(String component) {
            super(component);
            this.component = component;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Registration tally. Static because registration is a single-threaded postInit pass; reset by
    // begin() so a second call (a reload, a test) starts from zero rather than accumulating.
    // ------------------------------------------------------------------------------------------

    private static int registered;
    private static int skipped;
    private static final List<String> skips = new ArrayList<>();

    /** The design code of the recipe currently being registered, for log messages. */
    private static String current = "?";

    /** Index into GT's assembly-line list where this pass started adding; see {@link #summary()}. */
    private static int assemblyLineStart;

    /** The assembly-line recipes this mod added on the last pass, for {@link #checkResearchCollisions()}. */
    private static final List<GTRecipe.RecipeAssemblyLine> ownAssemblyLines = new ArrayList<>();

    /** Index into TecTech's research-station list where this pass started adding (Circuits v2 RSAL recipes). */
    private static int stationStart;

    /** The research-station recipes this mod added on the last pass, for {@link #checkResearchCollisions()}. */
    private static final List<TecTechRecipeMaps.TTResearchStationALRecipe> ownStationLines = new ArrayList<>();

    /** Clears the tally. Called once at the top of {@link BTSRecipes#register()}. */
    public static void begin() {
        registered = 0;
        skipped = 0;
        skips.clear();
        current = "?";
        assemblyLineStart = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.size();
        ownAssemblyLines.clear();
        stationStart = TecTechRecipeMaps.researchableALRecipeList.size();
        ownStationLines.clear();
    }

    // ------------------------------------------------------------------------------------------
    // The two containment scopes.
    // ------------------------------------------------------------------------------------------

    /**
     * Registers one recipe, or skips it loudly. {@code body} is the whole
     * {@code GTValues.RA.stdBuilder()...addTo(map)} chain.
     *
     * <b>"registered" means the chain ran to completion, not that GT kept the recipe.</b>
     * {@code addTo} can still drop one as a duplicate or a collision without telling us, and that
     * is invisible from here. The count is a measure of what this mod ASKED for; the skip list is
     * the exact thing, and the skip list is the half that matters.
     *
     * @param id   the recipe's design code from docs/production-lines.md - F-11c, M-03b, P-02a.
     *             It is what makes the summary readable against the spec.
     * @param body the builder chain. Runs exactly once.
     */
    public static void recipe(String id, Runnable body) {
        current = id;
        boolean ok = false;
        // The ledger notes what this body adds to the live maps, for /btsrecipes dump (BTSRecipeLedger).
        BTSRecipeLedger.begin();
        try {
            body.run();
            registered++;
            ok = true;
        } catch (MissingComponent missing) {
            // A resolver named the component precisely; note() logs it and records it.
            note(id, missing.component, origin(missing));
        } catch (RuntimeException | LinkageError other) {
            // Anything else that went wrong inside one recipe: a null Fluid, a dependency that
            // moved, GT's own null check firing on a component we do not resolve through a named
            // helper. Still one recipe, still not the game.
            final String what = other.getClass()
                .getSimpleName() + (other.getMessage() == null ? "" : ": " + other.getMessage());
            note(id, what, origin(other));
        } finally {
            BTSRecipeLedger.end(id, ok);
            current = "?";
        }
    }

    /**
     * Runs one whole production line's registration under the same containment. A throw from
     * outside any single recipe - a helper method, an OreDict registration, a static initialiser
     * touching a moved dependency - costs that one line instead of all five.
     */
    public static void line(String name, Runnable body) {
        BTSRecipeLedger.line(name);
        try {
            body.run();
        } catch (RuntimeException | LinkageError failed) {
            BeyondTheStars.LOG.error(
                "[BTS] PRODUCTION LINE '{}' FAILED and was abandoned partway through. Recipes it had "
                    + "already registered are kept; the rest of that line is missing. This is a bug - "
                    + "please report it with the stack trace below.",
                name,
                failed);
        } finally {
            BTSRecipeLedger.line(null);
        }
    }

    private static void note(String id, String component, String where) {
        skipped++;
        final String entry = id + " (" + where + ") - missing " + component;
        skips.add(entry);
        BeyondTheStars.LOG.error(
            "[BTS] SKIPPED RECIPE {} at {} - could not resolve {}. The recipe was not "
                + "registered; everything else continues.",
            id,
            where,
            component);
    }

    /**
     * The first frame inside this mod, as {@code Class.method(File.java:line)}. That is the line
     * that asked for the component, which is the line somebody has to go and fix.
     */
    private static String origin(Throwable thrown) {
        for (StackTraceElement frame : thrown.getStackTrace()) {
            final String cls = frame.getClassName();
            if (cls.startsWith("com.nateyburr.beyondthestars") && !cls.equals(BTSRecipeGuard.class.getName())) {
                final int dot = cls.lastIndexOf('.');
                return cls.substring(dot + 1) + "."
                    + readable(frame.getMethodName())
                    + "("
                    + frame.getFileName()
                    + ":"
                    + frame.getLineNumber()
                    + ")";
            }
        }
        return "unknown source";
    }

    /**
     * Every recipe body is a lambda, so the frame that failed is named {@code lambda$frontEnd$7}.
     * The interesting part is the method it was written in.
     */
    private static String readable(String frameMethod) {
        if (frameMethod == null || !frameMethod.startsWith("lambda$")) {
            return frameMethod;
        }
        final String withoutPrefix = frameMethod.substring("lambda$".length());
        final int trailing = withoutPrefix.lastIndexOf('$');
        return trailing <= 0 ? withoutPrefix : withoutPrefix.substring(0, trailing);
    }

    /**
     * The one block worth reading after a boot. Logged at the end of {@link BTSRecipes#register()}.
     */
    public static void summary() {
        // Registration is one single-threaded postInit pass, so everything appended since begin() is ours.
        final List<GTRecipe.RecipeAssemblyLine> all = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes;
        for (int i = assemblyLineStart; i < all.size(); i++) ownAssemblyLines.add(all.get(i));
        final List<TecTechRecipeMaps.TTResearchStationALRecipe> station = TecTechRecipeMaps.researchableALRecipeList;
        for (int i = stationStart; i < station.size(); i++) ownStationLines.add(station.get(i));

        if (skipped == 0) {
            BeyondTheStars.LOG.info("[BTS] Recipe registration complete: {} registered, 0 skipped.", registered);
            return;
        }

        BeyondTheStars.LOG.warn(
            "[BTS] Recipe registration complete: {} registered, {} SKIPPED. The game is fine and is "
                + "still playable - but the {} recipes listed below do not exist, so anything that "
                + "needed them cannot be crafted. Each line names the component that could not be "
                + "resolved and the source line that asked for it.",
            registered,
            skipped,
            skipped);

        for (int i = 0; i < skips.size(); i++) {
            BeyondTheStars.LOG.warn("[BTS]   skipped {}/{}: {}", i + 1, skips.size(), skips.get(i));
        }
    }

    /**
     * RESEARCH-ITEM COLLISIONS. GT's scanner and TecTech's research station write a data stick for
     * the FIRST assembly-line recipe whose research item matches the scanned stack (NBT ignored), so
     * a second recipe sharing a research item can never be put on a data stick and is uncraftable.
     * Called from {@code CommonProxy.loadComplete}, after the removals, so every mod's assembly lines
     * are in the list. Logs an ERROR per collision; changes nothing.
     */
    public static void checkResearchCollisions() {
        final List<GTRecipe.RecipeAssemblyLine> all = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes;
        // TecTech's research assembly lines put a placeholder research item into GT's list (the real one is in
        // TecTech's own list, checked below), so the placeholder is not a research item that can collide.
        final ItemStack placeholder = CustomItemList.UnusedStuff.get(1);
        int collisions = 0;
        int scanner = 0;
        for (GTRecipe.RecipeAssemblyLine own : ownAssemblyLines) {
            if (own.mResearchItem == null || GTUtility.areStacksEqual(own.mResearchItem, placeholder, true)) continue;
            scanner++;
            for (GTRecipe.RecipeAssemblyLine other : all) {
                if (other == own) continue;
                if (other.mResearchItem == null
                    || !GTUtility.areStacksEqual(own.mResearchItem, other.mResearchItem, true)) continue;
                final boolean otherFirst = all.indexOf(other) < all.indexOf(own);
                // A pair of our own recipes is reported once, from the later one.
                if (!otherFirst && ownAssemblyLines.contains(other)) continue;
                collisions++;
                BeyondTheStars.LOG.error(
                    "[BTS] RESEARCH COLLISION: {} and {} are both researched from {}. Only the earlier one ({}) "
                        + "can ever be written to a data stick; the other is uncraftable. Please report this.",
                    describe(own.mOutput),
                    describe(other.mOutput),
                    describe(own.mResearchItem),
                    otherFirst ? describe(other.mOutput) : describe(own.mOutput));
            }
        }
        collisions += checkStationCollisions();
        if (collisions == 0) {
            BeyondTheStars.LOG.info(
                "[BTS] Research items: all {} of our assembly-line recipes ({} scanner, {} Research Station) have a "
                    + "unique research item.",
                scanner + ownStationLines.size(),
                scanner,
                ownStationLines.size());
        }
    }

    /**
     * The same rule for TecTech's Research Station (Circuits v2 §2.4): it writes the data stick of the FIRST recipe in
     * {@code researchableALRecipeList} whose research item matches the held stack, so a second one sharing it is
     * uncraftable.
     */
    private static int checkStationCollisions() {
        final List<TecTechRecipeMaps.TTResearchStationALRecipe> all = TecTechRecipeMaps.researchableALRecipeList;
        int collisions = 0;
        for (TecTechRecipeMaps.TTResearchStationALRecipe own : ownStationLines) {
            if (own.mResearchItem == null) continue;
            for (TecTechRecipeMaps.TTResearchStationALRecipe other : all) {
                if (other == own || other.mResearchItem == null
                    || !GTUtility.areStacksEqual(own.mResearchItem, other.mResearchItem, true)) continue;
                final boolean otherFirst = all.indexOf(other) < all.indexOf(own);
                if (!otherFirst && ownStationLines.contains(other)) continue;
                collisions++;
                BeyondTheStars.LOG.error(
                    "[BTS] RESEARCH COLLISION (Research Station): {} and {} are both researched from {}. Only the "
                        + "earlier one ({}) can ever be written to a data stick; the other is uncraftable. Please "
                        + "report this.",
                    describe(own.mOutput),
                    describe(other.mOutput),
                    describe(own.mResearchItem),
                    otherFirst ? describe(other.mOutput) : describe(own.mOutput));
            }
        }
        return collisions;
    }

    /** The research-station recipes this mod added on the last pass (read-only view, for checks). */
    public static List<TecTechRecipeMaps.TTResearchStationALRecipe> ownStationRecipes() {
        return java.util.Collections.unmodifiableList(ownStationLines);
    }

    /** Display name and meta of a stack, for log lines that must tell metas apart. */
    private static String describe(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return name(stack);
        try {
            return stack.getDisplayName() + " (" + name(stack) + ":" + stack.getItemDamage() + ")";
        } catch (RuntimeException noName) {
            return name(stack) + ":" + stack.getItemDamage();
        }
    }

    /** How many recipes registered on the last pass. For an in-game diagnostic command to read. */
    public static int registeredCount() {
        return registered;
    }

    /** How many recipes were skipped on the last pass. For an in-game diagnostic command to read. */
    public static int skippedCount() {
        return skipped;
    }

    /** The skipped recipes, each with its missing component. For an in-game diagnostic to read. */
    public static List<String> skippedRecipes() {
        return new ArrayList<>(skips);
    }

    // ------------------------------------------------------------------------------------------
    // The resolvers. Each has the same shape: resolve, and on null name the thing precisely and
    // raise. They keep the signature of the call they replace, so the call sites read the same.
    // ------------------------------------------------------------------------------------------

    /**
     * Replaces {@code GTOreDictUnificator.get(prefix, material, amount)} - the ~138-call-site
     * lookup that broke the game. Same signature, same return, but a null is named and skipped
     * instead of being handed to GT.
     * <p>
     * The message reports whether the material is in {@code prefix.mGeneratedItems}, which
     * separates the two very different causes: <b>not generated</b> means GT never made this
     * prefix/material pairing at all and the recipe needs a different material (the Fluorite
     * case), while <b>generated but unresolved</b> means it exists and something else - a
     * unification or load-order problem - is in the way.
     */
    public static ItemStack req(OrePrefixes prefix, Object material, long amount) {
        final ItemStack resolved = GTOreDictUnificator.get(prefix, material, amount);
        if (resolved != null) {
            return resolved;
        }

        final String why;
        if (prefix == null) {
            why = "the prefix itself was null";
        } else if (material == null) {
            // Usually a bridged material - a Werkstoff or a foreign mod's Materials handle - whose
            // own lookup already answered null further up. Say that, rather than asking a
            // Collection whether it contains null and risking an NPE on the way to the message.
            why = "the MATERIAL was null before the lookup even ran, so whatever produces it "
                + "(a Werkstoff bridge, another mod's material handle) is the thing that failed";
        } else if (prefix.mGeneratedItems.contains(material)) {
            why = "the material IS in " + prefix
                + ".mGeneratedItems, so the pairing exists and something else - unification, or load "
                + "order - is in the way";
        } else {
            why = "the material is NOT in " + prefix
                + ".mGeneratedItems: GT never generated this prefix for it, so this recipe needs a "
                + "different material or a different prefix";
        }
        throw new MissingComponent("item " + prefix + " of " + material + " x" + amount + " (" + why + ")");
    }

    /**
     * Replaces {@code new FluidStack(fluid, amount)}. A null {@code Fluid} - a fluid another mod
     * did not register, or one of ours that failed to - NPEs inside the Forge constructor, which
     * reads as a bare NPE in a crash log.
     * <p>
     * It cannot name the fluid, because the reference it was handed is the thing that is null.
     * What it does give is the recipe id and the {@code File.java:line}, which is enough to walk
     * to the argument - and, unlike the raw NPE, it costs one recipe instead of the boot.
     */
    public static FluidStack fluid(Fluid fluid, int amount) {
        if (fluid == null) {
            throw new MissingComponent("fluid (null Fluid) x" + amount);
        }
        return new FluidStack(fluid, amount);
    }

    /**
     * Replaces {@code new ItemStack(item, count)} for items that could be absent. A null
     * {@code Item} makes an ItemStack that looks valid and then behaves as air.
     * <p>
     * <b>Offered, not wired.</b> The ~88 {@code new ItemStack(..)} sites in the recipe files build
     * this mod's OWN items and blocks, which are registered in preInit and are null only if that
     * failed - and several of them take a Block rather than an Item, so a blanket rewrite would not
     * even compile. They are covered by {@link #recipe(String, Runnable)}'s catch-all instead. Use
     * this for a foreign item whose presence is genuinely in doubt.
     */
    public static ItemStack stack(Item item, int count) {
        if (item == null) {
            throw new MissingComponent("item (null Item) x" + count);
        }
        return new ItemStack(item, count);
    }

    /**
     * The general form, for anything without a named resolver: a {@code Materials.getFluid} that
     * answered null, a {@code GregtechItemList} entry that is not in this pack, a Werkstoff whose
     * fluid was never made. {@code what} is what ends up in the log, so name the thing, not the type.
     *
     * <pre>
     * BTSRecipeGuard.nn(GregtechItemList.Milling_Ball_Alumina.get(0), "GregtechItemList.Milling_Ball_Alumina")
     * </pre>
     */
    public static <T> T nn(T value, String what) {
        if (value == null) {
            throw new MissingComponent(what);
        }
        return value;
    }

    /** The design code of the recipe being registered, or {@code ?} outside one. For diagnostics. */
    public static String currentRecipe() {
        return current;
    }

    /**
     * A short readable name for a recipe's subject, for building ids in the parameterised helpers.
     * <p>
     * F-13, F-14, F-17, P-02 and the rest are each one method called three or six times with
     * different items, so a fixed id would put the same label on several different recipes and the
     * summary could not tell you which grade or which tier was the one that went missing. Passing
     * the discriminating parameter through here fixes that: {@code "F-14 " + name(sintered)} reads
     * as {@code F-14 sinteredPelletMox}.
     */
    public static String name(Object subject) {
        if (subject == null) {
            return "null";
        }
        if (subject instanceof ItemStack) {
            final Item item = ((ItemStack) subject).getItem();
            return item == null ? "empty stack" : trim(item.getUnlocalizedName());
        }
        if (subject instanceof Item) {
            return trim(((Item) subject).getUnlocalizedName());
        }
        if (subject instanceof Fluid) {
            return ((Fluid) subject).getName();
        }
        return String.valueOf(subject);
    }

    /** Unlocalized names arrive as {@code item.bts.greenPellet}; the tail is the useful part. */
    private static String trim(String unlocalized) {
        if (unlocalized == null) {
            return "unnamed";
        }
        final int dot = unlocalized.lastIndexOf('.');
        return dot < 0 ? unlocalized : unlocalized.substring(dot + 1);
    }
}
