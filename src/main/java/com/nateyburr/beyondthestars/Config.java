package com.nateyburr.beyondthestars;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Base MetaTileEntity ID. 28100+ sits in a gap unused by GT5-Unofficial as of 5.09.52.594. */
    public static int mteBaseId = 28100;

    /** Length of one Gen I reactor fuel campaign, in real-time minutes. */
    public static int genICampaignMinutes = 720;

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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
