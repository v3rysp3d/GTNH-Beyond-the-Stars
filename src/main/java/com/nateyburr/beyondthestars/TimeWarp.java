package com.nateyburr.beyondthestars;

import java.lang.reflect.Field;
import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * TIME WARP (a cheat and testing aid, {@code /btswarp}; docs/automation.md §4.2). While on, every server tick is
 * followed by up to {@code N - 1} extra ticks of the whole server, run through the same code paths as a real one:
 * FML's server-tick events (AE2's grids and buses, GT's and everyone's tick handlers), then for every loaded dimension
 * FML's world-tick events, {@code WorldServer.tick()} (time, weather, block and scheduled ticks, chunk unloading),
 * {@code updateEntities()} (entities and every tile entity: GT machines, our Eye / Kerr / Quasar / conduit logic, AE2
 * parts) and the entity tracker. Nothing is skipped or shortcut per machine: the game simply runs N times faster.
 * <p>
 * Guards: the extra ticks never re-enter themselves (the server-tick event they fire comes back here and is ignored);
 * they stop for this tick once the tick has used {@link #TICK_BUDGET_NS} of its 50 ms, so the server keeps its 20 real
 * ticks a second and never falls behind ("can't keep up"), and N above what the machine can hold just gives the most it
 * can (measured, see {@link #status()}); an exception in an extra tick turns the warp off before it is passed on, as a
 * real tick's would be. It is off by default, off again when the server stops or a different server starts, and only
 * an op with cheats on can turn it on (the command's permission level 2).
 * <p>
 * Not done in the extra ticks (once per real tick is enough): the network tick, the player-list ping, autosaves and the
 * time-sync packet. The client therefore sees the world time jump once a second while the daylight cycle runs.
 */
public final class TimeWarp {

    private TimeWarp() {}

    /** The highest N the command takes. */
    public static final int MAX = 1000;
    /** How much of each 50 ms tick the real tick plus its extra ticks may use ({@code -Dbts.warp.tickms}, 45). */
    static final long TICK_BUDGET_NS = Long.getLong("bts.warp.tickms", 45L) * 1_000_000L;
    /** The window the achieved multiplier is measured over. */
    private static final long WINDOW_NS = 3_000_000_000L;

    private static final TimeWarp INSTANCE = new TimeWarp();
    private static boolean registered;

    /** Requested multiplier; 1 = off. */
    private static volatile int requested = 1;
    /** Achieved multiplier over the last {@link #WINDOW_NS} (game ticks per real second / 20). */
    private static volatile double measured = 1;
    /** Game ticks (real + extra) since the warp went on, and the real time it went on. */
    private static volatile long ticksSinceOn;
    private static volatile long onAtNanos;
    /** Real ticks since on, and how many of them hit the budget before all N - 1 extra ticks ran. */
    private static volatile long realTicks, limitedTicks;
    private static volatile double extraTickMs;

    private static boolean inExtra;
    private static long tickStart;
    private static MinecraftServer server;
    private static Field tickCounter;

    // ring of (nanoTime, ticksSinceOn) once per real tick, for the measured multiplier
    private static final int RING = 256;
    private static final long[] ringT = new long[RING], ringN = new long[RING];
    private static int ringHead, ringSize;

    /** Registers the tick handler (once); called when a server starts. Also resets the warp. */
    public static void serverStarting() {
        off("server start");
        if (!registered) {
            registered = true;
            FMLCommonHandler.instance()
                .bus()
                .register(INSTANCE);
        }
    }

    /** Turns the warp off when the server stops. */
    public static void serverStopping() {
        off("server stop");
        com.nateyburr.beyondthestars.machines.directive.EyeLiveRig.clear();
    }

    /** Sets the multiplier (2..{@link #MAX}); 1 or less turns it off. Returns the line to report. */
    public static synchronized String set(int n, String who) {
        if (n <= 1) return off(who);
        final int capped = Math.min(MAX, n);
        final boolean wasOn = requested > 1;
        requested = capped;
        if (!wasOn) {
            ticksSinceOn = 0;
            realTicks = 0;
            limitedTicks = 0;
            onAtNanos = System.nanoTime();
            ringHead = ringSize = 0;
            measured = 1;
        }
        BeyondTheStars.LOG.warn(
            "[BTS warp] TIME WARP ON x" + capped
                + " by "
                + who
                + " (cheat: the whole server runs up to "
                + capped
                + " ticks per real tick, within "
                + TICK_BUDGET_NS / 1_000_000L
                + " ms of each 50 ms tick)");
        return "Time warp ON: x" + capped + " requested" + (capped != n ? " (capped at " + MAX + ")" : "")
            + ". The achieved rate depends on what the server can hold: /btswarp status";
    }

    /** Turns the warp off. Returns the line to report. */
    public static synchronized String off(String who) {
        final boolean wasOn = requested > 1;
        final String s = wasOn ? status() : "";
        requested = 1;
        measured = 1;
        if (wasOn) {
            BeyondTheStars.LOG.warn("[BTS warp] time warp OFF (" + who + "): " + s);
            return "Time warp OFF. Last run: " + s;
        }
        return "Time warp is off.";
    }

    public static boolean on() {
        return requested > 1;
    }

    public static int requested() {
        return requested;
    }

    /** The achieved multiplier (game ticks per real second / 20) over the last 3 s; 1 when off. */
    public static double measured() {
        return requested > 1 ? measured : 1;
    }

    /** Game ticks run since the warp went on (real and extra). */
    public static long gameTicksSinceOn() {
        return ticksSinceOn;
    }

    /** One line: requested, achieved, game time against real time, how often the budget limited it. */
    public static String status() {
        final double realS = onAtNanos == 0 ? 0 : (System.nanoTime() - onAtNanos) / 1e9;
        final double gameS = ticksSinceOn / 20.0;
        return String.format(
            Locale.ROOT,
            "requested x%d, achieved x%.1f (last 3 s), x%.1f overall: %.0f s of game time in %.0f s real; "
                + "%.0f%% of real ticks hit the %d ms budget; an extra tick costs %.2f ms",
            requested,
            measured(),
            realS > 0 ? gameS / realS : 1,
            gameS,
            realS,
            realTicks > 0 ? 100.0 * limitedTicks / realTicks : 0,
            TICK_BUDGET_NS / 1_000_000L,
            extraTickMs);
    }

    // ------------------------------------------------------------------------------------- the ticks

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !inExtra) tickStart = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || inExtra) return;
        final MinecraftServer now = MinecraftServer.getServer();
        if (now != server) {
            server = now;
            off("another server");
            return;
        }
        if (requested <= 1 || now == null) return;
        ticksSinceOn++;
        realTicks++;
        final int want = requested - 1;
        final long start = System.nanoTime();
        final long deadline = (tickStart > 0 && start - tickStart < 50_000_000L ? tickStart : start) + TICK_BUDGET_NS;
        int done = 0;
        inExtra = true;
        try {
            while (done < want && System.nanoTime() < deadline && requested > 1) {
                extraTick(now);
                done++;
                ticksSinceOn++;
            }
        } catch (RuntimeException | Error bad) {
            inExtra = false;
            off("an exception in a warped tick");
            BeyondTheStars.LOG.error("[BTS warp] exception in an extra tick; the warp is off", bad);
            throw bad;
        } finally {
            inExtra = false;
        }
        if (done < want) limitedTicks++;
        if (done > 0) {
            final double ms = (System.nanoTime() - start) / 1e6 / done;
            extraTickMs = extraTickMs == 0 ? ms : extraTickMs * 0.95 + ms * 0.05;
        }
        sample(System.nanoTime());
    }

    private static void sample(long t) {
        ringT[ringHead] = t;
        ringN[ringHead] = ticksSinceOn;
        ringHead = (ringHead + 1) % RING;
        if (ringSize < RING) ringSize++;
        // the oldest sample still inside the window
        int oldest = -1;
        for (int k = ringSize - 1; k >= 1; k--) {
            final int i = Math.floorMod(ringHead - 1 - k, RING);
            if (t - ringT[i] <= WINDOW_NS) {
                oldest = i;
                break;
            }
        }
        if (oldest < 0) return;
        final double dt = (t - ringT[oldest]) / 1e9;
        if (dt > 0.25) measured = (ticksSinceOn - ringN[oldest]) / dt / 20.0;
    }

    /** One extra server tick, as {@code MinecraftServer.tick()} runs it, without the network, saves and pings. */
    private static void extraTick(MinecraftServer srv) {
        FMLCommonHandler.instance()
            .onPreServerTick();
        bumpTickCounter(srv);
        net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
        for (Integer id : DimensionManager.getIDs()) {
            if (id != 0 && !srv.getAllowNether()) continue;
            final WorldServer world = DimensionManager.getWorld(id);
            if (world == null) continue;
            FMLCommonHandler.instance()
                .onPreWorldTick(world);
            world.tick();
            world.updateEntities();
            FMLCommonHandler.instance()
                .onPostWorldTick(world);
            world.getEntityTracker()
                .updateTrackedEntities();
        }
        FMLCommonHandler.instance()
            .onPostServerTick();
    }

    private static void bumpTickCounter(MinecraftServer srv) {
        try {
            if (tickCounter == null) tickCounter = ReflectionHelper
                .findField(MinecraftServer.class, "tickCounter", "field_71315_w");
            tickCounter.setInt(srv, tickCounter.getInt(srv) + 1);
        } catch (ReflectiveOperationException | RuntimeException absent) {
            // the counter only paces saves and pings; the world's own time still advances
        }
    }
}
