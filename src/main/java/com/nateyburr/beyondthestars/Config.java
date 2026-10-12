package com.nateyburr.beyondthestars;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Base MetaTileEntity ID. 28100+ sits in a gap unused by GT5-Unofficial as of 5.09.52.594. */
    public static int mteBaseId = 28100;

    /** Length of one Gen I reactor fuel campaign, in real-time minutes. */
    public static int genICampaignMinutes = 720;

    /** Meltdowns produce the vertical detonation column. */
    public static boolean meltdownExplosions = true;

    /** Meltdowns leave a lingering radiation field (hazmat counters it). */
    public static boolean radiationField = true;

    /**
     * D-17 GRID CONDITIONING. With this on (the default), a plant with no SWITCHYARD standing
     * may only inject {@link com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1#GRID_SOFT_CAP}
     * EU/t; everything the reactor derives above that is thrown away until the yard is built.
     * <p>
     * It is ONE setting because it is a live balance change: every plant shipped before 0.21.0
     * was uncapped, and a tier-5 build makes ten times the cap. Turn it off and the Switchyard
     * becomes pure decoration - nothing else in the plant changes, and no panel line lies about
     * it (they all read this flag).
     */
    public static boolean switchyardSoftCap = true;

    /**
     * SINGULARITY LADDER (D-44, singularity-ladder-build.md §4): remove the Black Hole Compressor
     * controller's assembly-line recipe, because the Kerr Ergosphere Engine takes its slot at UIV.
     * Placed compressors keep working; only the crafting recipe (and data sticks holding it) go.
     * GTNH 2.9.0-RC-2: every recipe that CONSUMES a BHC controller (the Spacetime Compression Field
     * Generators, the coremod's Compressed Circuit Array) is first rewritten to consume the Kerr
     * controller instead (BTSBlackHoleSwap); the BHC recipe is kept if that cannot be done safely.
     */
    public static boolean removeBlackHoleCompressorRecipe = true;

    /**
     * SINGULARITY LADDER (D-28): clear TecTech's Eye of Harmony recipes, its NEI page and its
     * controller recipe; our Eye of Harmony replaces it. Placed TecTech EOHs form but idle. Off:
     * TecTech's EOH runs as stock beside ours, and the pack's White/Black Dwarf Matter and
     * Universium sources are untouched.
     */
    public static boolean replaceTecTechEyeOfHarmony = true;

    /**
     * D-49 retired {@code interimBlackDwarfAtEyeT4} (the interim Eye T4 Black Dwarf Matter recipe, Q-3):
     * Black Dwarf Matter now comes from Eye T5's Sirius B cooling batches. The stale key is removed from
     * the config file on load (d49-build-plan.md §10.4).
     */
    private static final String RETIRED_INTERIM_BDM_KEY = "interimBlackDwarfAtEyeT4";

    /**
     * D-49 (my answer 3, D-34): draw the Eye's current object at its centre (machines/eye/render: the live star,
     * neutron star, cooling dwarf, magnetar, Core Collapse). The client skips drawing when it is off; the server
     * places no render block when it is off. Each Eye also has a GUI switch.
     */
    public static boolean eyeRenderCentre = true;

    /**
     * KERR REDESIGN R4 (docs/kerr-redesign.md §5): the Kerr Ergosphere Engine's black hole, drawn at the
     * cage centre with GT's Black Hole Compressor renderer and scaled by mass. Read on both sides: the
     * server places the render block only when it is on, and a client with it off draws nothing. Each
     * KEE also has its own switch in its GUI.
     */
    public static boolean renderKerrHole = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        mteBaseId = configuration.getInt(
            "mteBaseId",
            Configuration.CATEGORY_GENERAL,
            28100,
            1,
            32765,
            "Base MetaTileEntity ID for Beyond the Stars machines (GT's registry caps at 32765)."
                + " Change only on ID collision.");

        genICampaignMinutes = configuration.getInt(
            "genICampaignMinutes",
            Configuration.CATEGORY_GENERAL,
            720,
            1,
            20160,
            "Real-time minutes one Gen I reactor fuel campaign lasts per fuel assembly load.");

        meltdownExplosions = configuration.getBoolean(
            "meltdownExplosions",
            Configuration.CATEGORY_GENERAL,
            true,
            "Reactor meltdowns produce a vertical detonation column above the core.");

        radiationField = configuration.getBoolean(
            "radiationField",
            Configuration.CATEGORY_GENERAL,
            true,
            "Reactor meltdowns leave a lingering radiation field. Full radiation hazmat counters it.");

        switchyardSoftCap = configuration.getBoolean(
            "switchyardSoftCap",
            Configuration.CATEGORY_GENERAL,
            true,
            "Nuclear plants without a Switchyard are soft-capped at 100,000,000 EU/t of output."
                + " The reactor still derives its full figure; the surplus is discarded and every panel"
                + " says so. Set false to let a plant export its full output with no Switchyard.");

        removeBlackHoleCompressorRecipe = configuration.getBoolean(
            "removeBlackHoleCompressorRecipe",
            Configuration.CATEGORY_GENERAL,
            true,
            "Remove the Black Hole Compressor controller's assembly-line recipe (the Kerr Ergosphere Engine"
                + " replaces it at UIV). Every recipe that consumes a BHC controller (Spacetime Compression Field"
                + " Generators, Compressed Circuit Array) is rewritten to consume a Kerr Ergosphere Engine controller"
                + " instead. Placed compressors keep working. Set false to keep the BHC and its recipes as stock.");

        replaceTecTechEyeOfHarmony = configuration.getBoolean(
            "replaceTecTechEyeOfHarmony",
            Configuration.CATEGORY_GENERAL,
            true,
            "Clear TecTech's Eye of Harmony recipes, NEI page and controller recipe; Beyond the Stars' Eye of"
                + " Harmony replaces it. Placed TecTech EOHs idle. Finish or cancel running EOH recipes before"
                + " enabling. Set false to keep TecTech's EOH as stock.");

        final boolean retiredKeyPresent = configuration.getCategory(Configuration.CATEGORY_GENERAL)
            .containsKey(RETIRED_INTERIM_BDM_KEY);
        if (retiredKeyPresent) {
            configuration.getCategory(Configuration.CATEGORY_GENERAL)
                .remove(RETIRED_INTERIM_BDM_KEY);
        }

        eyeRenderCentre = configuration.getBoolean(
            "eyeRenderCentre",
            Configuration.CATEGORY_GENERAL,
            true,
            "Draw the Eye of Harmony's current object at the centre of the structure while it is formed and its star"
                + " burns: the live star (T1-T3), the neutron star (T4), the cooling white dwarf (T5), the magnetar"
                + " (T6) and the Core Collapse. Uses TecTech's star shader and textures. Off on a client: nothing is"
                + " drawn there. Off on a server: no Eye places its render block (and existing ones are removed)."
                + " Each Eye also has its own switch in its GUI.");

        renderKerrHole = configuration.getBoolean(
            "renderKerrHole",
            Configuration.CATEGORY_GENERAL,
            true,
            "Draw the Kerr Ergosphere Engine's black hole at the centre of its cage, scaled by its mass (GT's Black"
                + " Hole Compressor render). Cosmetic. On a server, false places no render block at all; on a client,"
                + " false draws nothing. Each Kerr also has its own render switch in its GUI.");

        // Always written back: Forge does not read comments from the file and does not count a changed comment as a
        // change, so an old file would keep saying eyeRenderCentre is "not implemented" (D-49 unit 6). Saving
        // rewrites every comment from the code; values are kept.
        configuration.save();
    }
}
