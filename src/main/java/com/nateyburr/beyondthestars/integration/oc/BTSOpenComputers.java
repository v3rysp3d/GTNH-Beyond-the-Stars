package com.nateyburr.beyondthestars.integration.oc;

import com.nateyburr.beyondthestars.BeyondTheStars;

import li.cil.oc.api.Driver;

/**
 * AUTOMATION PASS: the OpenComputers integration, a SOFT dependency (P4 Q-20). {@code CommonProxy.init} calls
 * {@link #register()} only when {@code Loader.isModLoaded("OpenComputers")}, so nothing in this package is ever loaded
 * without OpenComputers; the build sees OC's API jar compile-only (dependencies.gradle).
 * <p>
 * One block driver ({@link BTSDriver}) gives an OpenComputers Adapter placed against one of our machines a component:
 * {@code bts_eye}, {@code bts_kerr}, {@code bts_kerr_module}, {@code bts_sensor}, {@code bts_directive}, and P4's
 * {@code bts_quasar}, {@code bts_mouth}, {@code bts_mirror}, {@code bts_junction}, {@code bts_crown},
 * {@code bts_receptor}, {@code bts_homogeny} and {@code bts_reality}. Their methods are thin wrappers over
 * {@link com.nateyburr.beyondthestars.integration.BTSComputerApi} (and the quasar / conduit APIs); every setter takes
 * one call per 20 ticks per machine ({@code BTSComputerApi.rateOk}).
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

    /**
     * Test bench ({@code /btskerr occall x y z method [args...]}, P4): builds the component our driver gives an Adapter
     * at x y z and invokes its callback {@code method} exactly as a computer would (a proxy {@code Arguments} over the
     * words given: numbers, true / false, strings), printing what it returned. Proves the components, the setters'
     * clamps and the 20-tick rate limit on a dev server with no computer. {@code method} "x2" suffix calls it twice
     * in the same tick.
     */
    public static java.util.List<String> call(net.minecraft.world.World world, int x, int y, int z, String method,
        String[] words) {
        final java.util.List<String> out = new java.util.ArrayList<>();
        final net.minecraftforge.common.util.ForgeDirection side = net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
        final BTSDriver ours = new BTSDriver();
        if (!ours.worksWith(world, x, y, z, side)) {
            out.add("our driver does not take the block at " + x + ", " + y + ", " + z);
            return out;
        }
        final li.cil.oc.api.network.ManagedEnvironment env = ours.createEnvironment(world, x, y, z, side);
        final boolean twice = method.endsWith("x2");
        final String name = twice ? method.substring(0, method.length() - 2) : method;
        java.lang.reflect.Method cb = null;
        for (java.lang.reflect.Method m : env.getClass()
            .getMethods()) {
            if (m.getName()
                .equals(name) && m.isAnnotationPresent(li.cil.oc.api.machine.Callback.class)) cb = m;
        }
        if (cb == null) {
            out.add(((BTSEnvironments.Base) env).preferredName() + " has no callback " + name);
            return out;
        }
        final Object[] values = new Object[words.length];
        for (int i = 0; i < words.length; i++) values[i] = parse(words[i]);
        final li.cil.oc.api.machine.Arguments args = arguments(values);
        for (int n = 0; n < (twice ? 2 : 1); n++) {
            try {
                final Object[] r = (Object[]) cb.invoke(env, null, args);
                out.add(
                    ((BTSEnvironments.Base) env).preferredName() + "."
                        + name
                        + java.util.Arrays.toString(words)
                        + " = "
                        + java.util.Arrays.deepToString(r));
            } catch (ReflectiveOperationException | RuntimeException e) {
                final Throwable t = e.getCause() != null ? e.getCause() : e;
                out.add(name + " threw " + t);
            }
        }
        return out;
    }

    private static Object parse(String w) {
        if ("true".equalsIgnoreCase(w) || "false".equalsIgnoreCase(w)) return Boolean.parseBoolean(w);
        try {
            return Double.parseDouble(w);
        } catch (NumberFormatException notNumber) {
            return w;
        }
    }

    /** A minimal {@code Arguments} over plain values, as OpenComputers converts Lua's (numbers arrive as doubles). */
    private static li.cil.oc.api.machine.Arguments arguments(Object[] v) {
        return (li.cil.oc.api.machine.Arguments) java.lang.reflect.Proxy.newProxyInstance(
            li.cil.oc.api.machine.Arguments.class.getClassLoader(),
            new Class<?>[] { li.cil.oc.api.machine.Arguments.class },
            (proxy, m, a) -> {
                final String n = m.getName();
                if (n.equals("count")) return v.length;
                if (n.equals("toArray")) return v.clone();
                if (n.equals("iterator")) return java.util.Arrays.asList(v)
                    .iterator();
                final int i = a != null && a.length > 0 && a[0] instanceof Integer ? (Integer) a[0] : -1;
                final boolean has = i >= 0 && i < v.length && v[i] != null;
                if (n.startsWith("opt") && !has) return a[1];
                if (!has && n.startsWith("check")) throw new IllegalArgumentException("bad argument #" + (i + 1));
                if (n.startsWith("is")) {
                    if (!has) return false;
                    switch (n) {
                        case "isBoolean":
                            return v[i] instanceof Boolean;
                        case "isString":
                            return v[i] instanceof String;
                        case "isInteger":
                        case "isLong":
                        case "isDouble":
                            return v[i] instanceof Double;
                        default:
                            return false;
                    }
                }
                final Object o = v[i];
                switch (m.getReturnType()
                    .getName()) {
                    case "int":
                        return ((Number) o).intValue();
                    case "long":
                        return ((Number) o).longValue();
                    case "double":
                        return ((Number) o).doubleValue();
                    case "boolean":
                        return o;
                    case "java.lang.String":
                        return String.valueOf(o);
                    default:
                        return o;
                }
            });
    }

    public static void register() {
        Driver.add(new BTSDriver());
        BeyondTheStars.LOG.info(
            "[BTS] OpenComputers found: components bts_eye, bts_kerr, bts_kerr_module, bts_sensor, bts_directive, "
                + "bts_quasar, bts_mouth, bts_mirror, bts_junction, bts_crown, bts_receptor, bts_homogeny, bts_reality "
                + "registered (setters: one call per 20 ticks).");
    }
}
