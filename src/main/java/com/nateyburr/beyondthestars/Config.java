package com.nateyburr.beyondthestars;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Base MetaTileEntity ID. 28100+ sits in a gap unused by GT5-Unofficial as of 5.09.54.93. */
    public static int mteBaseId = 28100;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        mteBaseId = configuration.getInt(
            "mteBaseId",
            Configuration.CATEGORY_GENERAL,
            28100,
            1,
            65000,
            "Base MetaTileEntity ID for Beyond the Stars machines. Change only on ID collision.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
