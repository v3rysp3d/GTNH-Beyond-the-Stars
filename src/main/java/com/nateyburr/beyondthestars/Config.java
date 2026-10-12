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
     * SINGULARITY LADDER (Q-3): register the interim Eye T4 "Sirius B, Cooled" recipe that makes
     * Black Dwarf Matter until Eye T5 ships in P4, and the recipe for the star chart that targets it.
     * The star chart ITEM stays registered either way (item registry stays stable across config
     * changes); with the flag off it has no recipe and no Eye recipe accepts it.
     */
    public static boolean interimBlackDwarfAtEyeT4 = true;

    /**
     * SINGULARITY LADDER (D-34): render the band's object at the Eye's centre. Client cosmetic.
     * NOT IMPLEMENTED YET (Q-34): nothing reads this flag; it is registered so the key is stable.
     */
    public static boolean eyeRenderCentre = true;

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

        interimBlackDwarfAtEyeT4 = configuration.getBoolean(
            "interimBlackDwarfAtEyeT4",
            Configuration.CATEGORY_GENERAL,
            true,
            "Register the interim Eye of Harmony T4 recipe (Star Chart: Sirius B, Cooled) that makes Black Dwarf"
                + " Matter until Eye T5 exists, and the star chart's crafting recipe. The star chart item itself"
                + " stays registered either way (it has no recipe and no use when this is false).");

        eyeRenderCentre = configuration.getBoolean(
            "eyeRenderCentre",
            Configuration.CATEGORY_GENERAL,
            true,
            "NOT IMPLEMENTED YET - this setting currently has no effect. Will render the star or planet at the"
                + " centre of a formed Eye of Harmony (client cosmetic).");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
