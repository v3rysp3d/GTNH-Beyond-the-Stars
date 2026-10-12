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

    /**
     * Test bench ({@code /btskerr ocprobe x y z}): whether OpenComputers' driver registry finds a driver for the block
     * at x y z (what an Adapter would ask), and the component our driver builds there - its name, its network node and
     * its callbacks - without a computer.
     */
    public static java.util.List<String> probe(net.minecraft.world.World world, int x, int y, int z) {
        final java.util.List<String> out = new java.util.ArrayList<>();
        final net.minecraftforge.common.util.ForgeDirection side = net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
        final li.cil.oc.api.driver.SidedBlock found = Driver.driverFor(world, x, y, z, side);
        out.add(
            "OpenComputers registry: " + (found == null ? "no driver"
                : found.getClass()
                    .getName())
                + " for the block at "
                + x
                + ", "
                + y
                + ", "
                + z);
        final BTSDriver ours = new BTSDriver();
        if (!ours.worksWith(world, x, y, z, side)) {
            out.add("  our driver does not take this block");
            return out;
        }
        final li.cil.oc.api.network.ManagedEnvironment env = ours.createEnvironment(world, x, y, z, side);
        final java.util.TreeSet<String> callbacks = new java.util.TreeSet<>();
        for (java.lang.reflect.Method m : env.getClass()
            .getMethods()) {
            if (m.isAnnotationPresent(li.cil.oc.api.machine.Callback.class)) callbacks.add(m.getName());
        }
        out.add(
            "  component " + ((BTSEnvironments.Base) env).preferredName()
                + ", node "
                + (env.node() == null ? "MISSING"
                    : "ok (reachability " + env.node()
                        .reachability() + ")")
                + ", callbacks "
                + callbacks);
        return out;
    }

    public static void register() {
        Driver.add(new BTSDriver());
        BeyondTheStars.LOG.info(
            "[BTS] OpenComputers found: components bts_eye, bts_kerr, bts_kerr_module, bts_sensor, bts_directive registered.");
    }
}
