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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
