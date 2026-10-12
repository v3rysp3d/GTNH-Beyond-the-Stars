package com.nateyburr.beyondthestars.integration.oc;

import com.nateyburr.beyondthestars.BeyondTheStars;

import li.cil.oc.api.Driver;

/**
 * AUTOMATION PASS: the OpenComputers integration, a SOFT dependency (P4 Q-20). {@code CommonProxy.init} calls
 * {@link #register()} only when {@code Loader.isModLoaded("OpenComputers")}, so nothing in this package is ever loaded
 * without OpenComputers; the build sees OC's API jar compile-only (dependencies.gradle).
 * <p>
 * One block driver ({@link BTSDriver}) gives an OpenComputers Adapter placed against one of our machines a component:
 * {@code bts_eye}, {@code bts_kerr}, {@code bts_kerr_module}, {@code bts_sensor} or {@code bts_directive}. Their
 * methods are thin wrappers over {@link com.nateyburr.beyondthestars.integration.BTSComputerApi}.
 */
public final class BTSOpenComputers {

    private BTSOpenComputers() {}

    public static void register() {
        Driver.add(new BTSDriver());
        BeyondTheStars.LOG.info(
            "[BTS] OpenComputers found: components bts_eye, bts_kerr, bts_kerr_module, bts_sensor, bts_directive registered.");
    }
}
