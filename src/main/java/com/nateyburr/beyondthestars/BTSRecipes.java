package com.nateyburr.beyondthestars;

/**
 * D-26 THE RECIPE ORCHESTRATOR (docs/production-lines.md S7).
 * <p>
 * This file used to be 486 lines and 26 registration blocks. Four production lines had to be
 * built at once by four different people, and one file that everybody edits is one file that
 * everybody conflicts in - so the blocks moved out, VERBATIM, into five files with one owner
 * each, and what is left is the call order.
 *
 * <pre>
 *   BTSRecipesFuel        Agent A   S1  ore -> bundle, and the PUREX head-end
 *   BTSRecipesMaterials   Agent B   S3  bulk casings on the Site Batching Plant
 *   BTSRecipesPropellant  Agent C   S4.1 oxidiser / slurry / blend, the six rungs
 *   BTSRecipesCoolant     Agent C   S4.2 bulk coolant and the chemical shim
 *   BTSRecipesMachines    Agent D   controllers, hatches, control blocks, rotor
 * </pre>
 *
 * <b>Nothing may be added to THIS file.</b> A recipe here would be a recipe with no owner, and
 * the next release would have to work out whose it was.
 * <p>
 * <b>Every line runs under {@link BTSRecipeGuard}.</b> 0.24.0 did not boot because one OreDict
 * lookup in one recipe answered null and the exception escaped postInit, taking all four lines with
 * it. {@link BTSRecipeGuard#line(String, Runnable)} contains a failure to the line it happened in,
 * {@link BTSRecipeGuard#recipe(String, Runnable)} contains it to the single recipe, and
 * {@link BTSRecipeGuard#summary()} prints what was lost. <b>These calls are not recipes and do not
 * break the no-recipes-here rule</b> - they are the call order's own error handling.
 * <p>
 * <b>Order is deliberate.</b> The lines are independent - GT recipe maps do not care what order
 * they are filled in - but {@link BTSDisplayRecipes} runs LAST for a reason: its three
 * display-only NEI pages read {@code MTEReactorCoreMk1} runtime tables, and running it last keeps
 * it the final thing that can throw during postInit, where one escaping exception costs the whole
 * mod rather than one NEI tab.
 */
public final class BTSRecipes {

    private BTSRecipes() {}

    public static void register() {
        BTSRecipeGuard.begin();

        BTSRecipeGuard.line("fuel", BTSRecipesFuel::register);
        BTSRecipeGuard.line("materials", BTSRecipesMaterials::register);
        BTSRecipeGuard.line("propellant", BTSRecipesPropellant::register);
        BTSRecipeGuard.line("coolant", BTSRecipesCoolant::register);
        BTSRecipeGuard.line("machines", BTSRecipesMachines::register);
        BTSRecipeGuard.line("singularity", BTSRecipesSingularity::register);
        BTSRecipeGuard.line("kerr", BTSRecipesKerr::register);
        BTSRecipeGuard.line("display", BTSDisplayRecipes::register);

        BTSRecipeGuard.summary();
    }
}
