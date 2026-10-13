package com.nateyburr.beyondthestars.machines.directive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The standard gate rig ({@link DirectiveBench.EyeRig}) LIVE: once every game second, for each Eye it is on, one
 * {@code act} on the codes its Trims, Status and Transition Directive Hatches put out (what plain redstone, export
 * buses
 * with redstone cards and an AE2 level maintainer do with those bits). {@code /btseye star x y z autorig <s>} runs the
 * same rig for s seconds inside one command; this one runs alongside the game, so the star is seen being held, and
 * under
 * the time warp ({@code /btswarp}) it runs as fast as everything else. Bench only: {@code /btseye star x y z liverig
 * on|off|status}. It keeps no state in the world and stops when the server stops.
 */
public final class EyeLiveRig {

    private EyeLiveRig() {}

    private static final class Entry {

        final int dim, x, y, z;
        DirectiveBench.EyeRig rig;
        long seconds;
        String last = "";

        Entry(int dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final Map<String, Entry> RIGS = new LinkedHashMap<>();
    private static boolean hooked;
    private static long ticks;

    private static String key(int dim, int x, int y, int z) {
        return dim + ":" + x + "," + y + "," + z;
    }

    /** Turns the live rig on or off for the Eye at (x, y, z) in {@code world}; returns the lines to print. */
    public static List<String> set(World world, int x, int y, int z, String how) {
        final List<String> out = new ArrayList<>();
        final String k = key(world.provider.dimensionId, x, y, z);
        if (how.equals("off")) {
            final Entry e = RIGS.remove(k);
            out.add(e == null ? "No live rig on this Eye." : "Live rig OFF after " + e.seconds + " s: " + stats(e));
            return out;
        }
        if (how.equals("status")) {
            final Entry e = RIGS.get(k);
            out.add(e == null ? "No live rig on this Eye." : "Live rig ON for " + e.seconds + " s: " + stats(e));
            return out;
        }
        final MTEEyeOfHarmonyBTS eye = eyeAt(world, x, y, z);
        if (eye == null || DirectiveBench.EyeRig.of(eye) == null) {
            out.add("The Eye needs Trims, Status and Transition Directive Hatches: run 'directive' first.");
            return out;
        }
        if (!hooked) {
            hooked = true;
            FMLCommonHandler.instance()
                .bus()
                .register(new EyeLiveRig.Ticker());
        }
        RIGS.putIfAbsent(k, new Entry(world.provider.dimensionId, x, y, z));
        BeyondTheStars.LOG.info("[BTS bench] live gate rig ON for the Eye at " + k);
        out.add(
            "Live rig ON: every game second it reads the Eye's Trims / Status / Transition codes and acts on them "
                + "(trim valves into the Field hatches, charges, cores and charts into a plinth bus, a level "
                + "maintainer on the buffers, Tachyon on Transition [8]). Make sure the debug rig is off (rig none).");
        return out;
    }

    /** Every live rig off (server stop). */
    public static void clear() {
        RIGS.clear();
    }

    private static String stats(Entry e) {
        final DirectiveBench.EyeRig r = e.rig;
        if (r == null) return "not acted yet";
        return String.format(
            Locale.ROOT,
            "charges %d, stock-ups %d, cores %d, charts %d, Tachyon seconds %d; trim bit-seconds %d/%d/%d/%d%s",
            r.charges,
            r.stocks,
            r.cores,
            r.charts,
            r.tach,
            r.bitSeconds[0],
            r.bitSeconds[1],
            r.bitSeconds[2],
            r.bitSeconds[3],
            e.last.isEmpty() ? "" : "; " + e.last);
    }

    private static MTEEyeOfHarmonyBTS eyeAt(World world, int x, int y, int z) {
        if (world == null || !world.blockExists(x, y, z)) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object m = ((IGregTechTileEntity) te).getMetaTileEntity();
        return m instanceof MTEEyeOfHarmonyBTS ? (MTEEyeOfHarmonyBTS) m : null;
    }

    /** Once a game second (every 20 server ticks, the time warp's included). */
    public static final class Ticker {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || RIGS.isEmpty()) return;
            if (++ticks % 20 != 0) return;
            for (Entry e : RIGS.values()) {
                final MTEEyeOfHarmonyBTS eye = eyeAt(DimensionManager.getWorld(e.dim), e.x, e.y, e.z);
                if (eye == null) continue;
                if (e.rig == null) e.rig = DirectiveBench.EyeRig.of(eye);
                if (e.rig == null) continue;
                try {
                    e.rig.act(eye, false);
                    e.seconds++;
                    e.last = eye.starRuntime()
                        .lastFailure();
                } catch (RuntimeException bad) {
                    BeyondTheStars.LOG.warn("[BTS bench] live rig at " + key(e.dim, e.x, e.y, e.z) + ": " + bad);
                }
            }
        }
    }
}
