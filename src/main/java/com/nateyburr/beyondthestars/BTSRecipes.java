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
 *   BTSRecipesStellar     D-49      the Antipolarization Agent chain (SL-S01..S02)
 *   BTSRecipesBEC         BEC       condensates + Condensate Assembler recipes (SL-B01..B81)
 *   (BTSRecipesGeneratorUpgrade, B-83, SL-G01 and BTSStockCircuitRaise SL-G10..G13 run at load-complete:
 *   CommonProxy.loadComplete)
 *   BTSRecipesAutomation  automation the Directive Hatch (SL-A01)
 *   BTSRecipesReality     H/R       the Homogeny / Reality BEC recipes (SL-C01..C04, SL-H01..H20, SL-R01..R17)
 *   BTSRecipesQuasar      P4        the quasar / conduit / Eye T7-T8 recipes (SL-B16, SL-Q01..Q26, SL-P40)
 *   BTSRecipesCircuits    circuits  Circuits v2: wafers, Stellar SMDs, boards, Sidereal / Empyrean / Omega (SL-X01..X32)
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
        BTSRecipeGuard.line("eye", BTSRecipesEye::register);
        BTSRecipeGuard.line("stellar", BTSRecipesStellar::register);
        BTSRecipeGuard.line("bec", BTSRecipesBEC::register);
        BTSRecipeGuard.line("automation", BTSRecipesAutomation::register);
        // H/R unit 2: the Homogeny / Reality machines' own process recipes (their maps; the BEC recipes are the
        // recipe unit's).
        BTSRecipeGuard
            .line("reality-process", com.nateyburr.beyondthestars.machines.reality.RealityProcessRecipes::register);
        // H/R unit 3: every block, core and controller of the three machines in the Condensate Assembler, and their
        // condensates (BTSRecipesReality, one owner). Runs after "bec", whose
        // condensates it reuses (registered here only if absent).
        BTSRecipeGuard.line("reality", BTSRecipesReality::register);
        // P4: the quasar, conduit, crowns, their blocks, the T7/T8 cores and charts, SL-P40 (BTSRecipesQuasar, one
        // owner). After "reality": it uses that line's Entangled Ergomatter (SL-C02).
        BTSRecipeGuard.line("quasar", BTSRecipesQuasar::register);
        // Circuits v2 (circuits-v2.md §2): the wafer / SMD / board / circuit chain in stock GT machines and the
        // research Assembly Line (BTSRecipesCircuits, one owner).
        BTSRecipeGuard.line("circuits", BTSRecipesCircuits::register);
        // Circuits v2 §4.5: the NAC route SL-N01..N23 (BTSRecipesNac, one owner). After "circuits": the matrix recipes
        // are derived from the live SL-X21..X32.
        BTSRecipeGuard.line("nac", BTSRecipesNac::register);
        BTSRecipeGuard.line("display", BTSDisplayRecipes::register);

        BTSRecipeGuard.summary();
    }
}
