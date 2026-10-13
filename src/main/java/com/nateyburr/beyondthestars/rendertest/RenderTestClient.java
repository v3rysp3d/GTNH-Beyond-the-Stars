package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.client.render.GpuTimer;
import com.nateyburr.beyondthestars.client.render.RenderQuality;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;
import com.nateyburr.beyondthestars.machines.kerr.TileKerrHole;
import com.nateyburr.beyondthestars.machines.quasar.render.TileQuasarHole;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY REAL-CLIENT RENDER QA harness, part 1 (the client half). Registered by the client proxy only when the JVM
 * runs with {@code -Dbts.rendertest=true}; otherwise this class is never loaded.
 * <p>
 * On start it sets a fixed 1280x720 window, 16-chunk render distance, no vsync / frame cap and no pause on focus loss,
 * then from the main menu creates (or reuses) the creative superflat world {@code saves/bts-rendertest} on the
 * integrated server. For each {@link RenderTest#CASES case} it has the server build the case's site (once per site) and
 * set its state, then, by day and by night, moves the player to the four {@link RenderTest#VIEWS viewpoints}, waits
 * until the chunks have arrived and rendered, and saves {@code screenshots/rendertest/<case>_<view>_<day|night>.png}
 * with vanilla's {@link ScreenShotHelper}. The FPS of the debug counter at each shot is logged per case in
 * {@code screenshots/rendertest/rendertest.log} and {@code fps.tsv}. When every case is done it closes the game.
 * <p>
 * Renders v2 (WP-0): {@code -Dbts.rendertest.quality=low|medium|high} shoots a quality other than the config's, and
 * {@code -Dbts.rendertest.gputime=true} adds each shot's GPU and CPU milliseconds per frame of our renderers, averaged
 * over the view's still window ({@link GpuTimer}), to {@code fps.tsv} ({@code gpu_ms}, {@code cpu_ms},
 * {@code gpu_detail}) and the log, with every object's detail tier.
 */
@SideOnly(Side.CLIENT)
public final class RenderTestClient {

    /** Window size the shots are taken at. */
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    /** Give up and close after this many client ticks (150 minutes: renders v2 has about 45 cases). */
    private static final int MAX_TICKS = 150 * 60 * 20;
    /** Settling at a newly built site: at least / at most (ticks). */
    private static final int SITE_MIN = 120;
    private static final int SITE_MAX = 1600;
    /** Settling after a move on the same site: at least / at most (ticks). */
    private static final int VIEW_MIN = 50;
    private static final int VIEW_MAX = 600;
    /** Chunks and render updates must hold still this long (ticks) before a shot. */
    private static final int STILL = 40;

    private enum Phase {
        /** Waiting for the main menu, then launching the world. */
        WAIT_MENU,
        /** Waiting for the player to be in the world. */
        WAIT_WORLD,
        /** Waiting for a server task; its continuation picks the next phase. */
        WAIT,
        /** Waiting for the chunks and render updates to settle at a viewpoint. */
        SETTLE,
        /** The next rendered frame is saved. */
        SHOOT,
        FINISH,
        DONE
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final File dir;
    private PrintWriter log;
    private PrintWriter fps;
    /** Renders v2 (R-01): the computed day poses of this run, in the committed poses-file format. */
    private PrintWriter poses;

    private Phase phase = Phase.WAIT_MENU;
    private int ticks;
    private int phaseTicks;
    private CompletableFuture<?> pending;
    private Consumer<Object> then;

    private int caseIndex = -1;
    private RenderTest.Site site;
    private boolean siteFresh;
    private int shot;
    private int stillTicks;
    private int lastChunks = -1;
    /** What re-renders chunk sections, counted over each shot's still window (render QA E10). */
    private RenderDirtTracker dirt;
    private int settleMin;
    private int settleMax;
    private String lastScreen = "";
    private final List<Integer> caseFps = new ArrayList<>();
    private final List<String> summary = new ArrayList<>();
    private final List<String> files = new ArrayList<>();

    private RenderTestClient() {
        dir = new File(mc.mcDataDir, "screenshots/rendertest");
    }

    /** Called by the client proxy when {@link RenderTest#ENABLED}. */
    public static void register() {
        if (AutoRig.ENABLED) {
            // -Dbts.rendertest.cases=automation: the automation photo tour drives this client instead
            AutoRigClient.register();
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(new RenderTestClient());
        RenderTest.registerServerHook();
        BeyondTheStars.LOG
            .warn("[BTS render test] -Dbts.rendertest=true: the render QA harness will drive this client");
    }

    // ================================================================================== log

    private void openLogs() {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            BeyondTheStars.LOG.error("[BTS render test] cannot create " + dir);
        }
        try {
            log = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "rendertest.log")), StandardCharsets.UTF_8),
                true);
            fps = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "fps.tsv")), StandardCharsets.UTF_8),
                true);
            fps.println("case\tview\tfps\tchunk_updates\tfile\tgpu_ms\tcpu_ms\tgpu_detail");
            poses = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "poses.tsv")), StandardCharsets.UTF_8),
                true);
            poses.println("# case\tview\tdx\tdy\tdz\tyaw\tpitch  (eye relative to the case's render centre)");
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS render test] cannot open the log", bad);
        }
    }

    private void say(String line) {
        BeyondTheStars.LOG.info("[BTS render test] " + line);
        if (log != null) log.println(String.format(Locale.ROOT, "[t%6d] %s", ticks, line));
    }

    // ================================================================================ ticks

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) return;
        ticks++;
        phaseTicks++;
        if (ticks == 1) {
            openLogs();
            setUpClient();
        }
        // renders v2 (R-01): a dev client's options.txt is read back a second after start-up and put the render
        // distance (and with it the integrated server's view distance) back to the file's value (4 in the dev run
        // dirs), so every far view stood beyond the terrain; hold 16 for the whole run, as the recorder does (B-109)
        if (mc.gameSettings.renderDistanceChunks != 16) {
            say("render distance was " + mc.gameSettings.renderDistanceChunks + ": holding 16");
            mc.gameSettings.renderDistanceChunks = 16;
        }
        if (mc.theWorld != null && (dirt == null || dirt.world() != mc.theWorld)) {
            dirt = new RenderDirtTracker(mc.theWorld);
            mc.theWorld.addWorldAccess(dirt);
        }
        if (ticks > MAX_TICKS && phase != Phase.FINISH) {
            say("TIMEOUT after " + ticks + " ticks: closing with what was shot");
            pending = null;
            then = null;
            go(Phase.FINISH);
        }
        if (phase.ordinal() >= Phase.WAIT.ordinal() && phase != Phase.FINISH) keepScreenClear();
        if (pending != null) {
            if (!pending.isDone()) return;
            Object result;
            try {
                result = pending.get();
            } catch (Exception bad) {
                result = null;
                say("server task FAILED: " + bad);
            }
            final Consumer<Object> next = then;
            pending = null;
            then = null;
            if (next != null) next.accept(result);
        }
        switch (phase) {
            case WAIT_MENU:
                waitMenu();
                break;
            case WAIT_WORLD:
                waitWorld();
                break;
            case SETTLE:
                settle();
                break;
            case FINISH:
                finish();
                break;
            default:
                break;
        }
    }

    /** The shot is taken after a full frame has been drawn into the framebuffer. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase != Phase.SHOOT || mc.currentScreen != null) return;
        final RenderTest.Case c = RenderTest.CASES.get(caseIndex);
        final String view = viewName(shot);
        final String file = c.name + "_" + view + ".png";
        ScreenShotHelper
            .saveScreenshot(mc.mcDataDir, "rendertest/" + file, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        final int[] counter = debugCounter();
        caseFps.add(counter[0]);
        files.add(file);
        final GpuTimer.Report gpu = GpuTimer.ENABLED ? GpuTimer.report() : null;
        if (fps != null) {
            fps.println(
                c.name + "\t"
                    + view
                    + "\t"
                    + counter[0]
                    + "\t"
                    + counter[1]
                    + "\t"
                    + file
                    + "\t"
                    + (gpu == null ? "" : String.format(Locale.ROOT, "%.4f", gpu.gpuMs))
                    + "\t"
                    + (gpu == null ? "" : String.format(Locale.ROOT, "%.4f", gpu.cpuMs))
                    + "\t"
                    + (gpu == null ? "" : gpu.detail));
        }
        String pose;
        while ((pose = RenderTest.POSE_LOG.poll()) != null) {
            if (poses != null) poses.println(pose);
        }
        say("SHOT " + file + "  fps " + counter[0] + ", " + counter[1] + " chunk updates; " + clientView());
        say("  render: " + RenderQuality.describe());
        if (gpu != null) {
            say(
                String.format(
                    Locale.ROOT,
                    "  gpu %.4f ms, cpu %.4f ms per frame over %d frames%s: %s",
                    gpu.gpuMs,
                    gpu.cpuMs,
                    gpu.frames,
                    gpu.gpuSupported ? "" : " (no timer queries: cpu only)",
                    gpu.detail));
        }
        if (dirt != null) {
            say("  re-render causes over the last " + stillTicks + " still ticks: " + dirt.top(6));
            final String paths = dirt.stacks();
            if (!paths.isEmpty()) say("  client call path of the first machine block update: " + paths);
        }
        shot++;
        await(RenderTest::status, r -> {
            if (r != null && !r.toString()
                .isEmpty()) say("  state: " + r);
            if (shot < RenderTest.VIEWS.length * 2) {
                move();
            } else {
                endCase(c);
                startCase(caseIndex + 1);
            }
        });
    }

    // ============================================================================== phases

    private void go(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    /** Runs {@code work} on the server, then {@code next} with its result on the client thread. */
    private void await(Supplier<?> work, Consumer<Object> next) {
        pending = RenderTest.onServer(work);
        then = next;
        go(Phase.WAIT);
    }

    private void setUpClient() {
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.renderDistanceChunks = 16;
        mc.gameSettings.fovSetting = 70f;
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
        mc.gameSettings.showDebugInfo = false;
        try {
            Display.setVSyncEnabled(false);
            if (!Display.isFullscreen()) {
                Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
                mc.resize(WIDTH, HEIGHT);
            }
        } catch (Exception bad) {
            say("could not resize the window: " + bad);
        }
        say(
            "harness up: window " + mc.displayWidth
                + "x"
                + mc.displayHeight
                + ", render distance 16, no frame cap, "
                + RenderTest.CASES.size()
                + " cases x "
                + RenderTest.VIEWS.length * 2
                + " shots; render quality "
                + RenderQuality.quality()
                + (GpuTimer.ENABLED ? ", GPU timer on" : ""));
    }

    private void waitMenu() {
        if (mc.currentScreen instanceof GuiMainMenu) {
            final boolean reuse = new File(mc.mcDataDir, "saves/" + RenderTest.WORLD_FOLDER + "/level.dat").isFile();
            final WorldSettings settings = new WorldSettings(
                20261013L,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                WorldType.FLAT);
            settings.func_82750_a("2;7,2x3,2;1;");
            settings.enableCommands();
            say((reuse ? "reusing" : "creating") + " the test world saves/" + RenderTest.WORLD_FOLDER);
            go(Phase.WAIT_WORLD);
            mc.launchIntegratedServer(RenderTest.WORLD_FOLDER, "BTS Render Test", settings);
        } else if (phaseTicks % 200 == 0) {
            say("waiting for the main menu (screen " + screenName() + ")");
        }
    }

    private void waitWorld() {
        final boolean ready = mc.theWorld != null && mc.thePlayer != null
            && mc.getIntegratedServer() != null
            && !(mc.currentScreen instanceof GuiDownloadTerrain);
        if (ready && phaseTicks > 100) {
            if (mc.currentScreen != null) {
                keepScreenClear();
                return;
            }
            mc.gameSettings.hideGUI = true;
            mc.gameSettings.thirdPersonView = 0;
            await(RenderTest::prepareWorld, r -> {
                say(String.valueOf(r));
                startCase(0);
            });
        } else if (phaseTicks % 200 == 0) {
            say(
                "waiting for the world (screen " + screenName()
                    + ", world "
                    + (mc.theWorld != null)
                    + ", player "
                    + (mc.thePlayer != null)
                    + ")");
        }
    }

    private void startCase(int index) {
        caseIndex = index;
        if (caseIndex >= RenderTest.CASES.size()) {
            go(Phase.FINISH);
            return;
        }
        final RenderTest.Case c = RenderTest.CASES.get(caseIndex);
        caseFps.clear();
        shot = 0;
        say("=== CASE " + (caseIndex + 1) + "/" + RenderTest.CASES.size() + ": " + c.name + " - " + c.what);
        if (c.site == site) {
            siteFresh = false;
            applyCase(c);
            return;
        }
        site = c.site;
        siteFresh = true;
        say("building site " + site + " ...");
        await(() -> RenderTest.buildSite(c.site), r -> {
            final RenderTest.Built b = (RenderTest.Built) r;
            if (b == null || b.error != null) {
                final String why = b == null ? "the build task failed" : b.error;
                if (b != null) for (String line : b.lines) say("  " + line);
                say("  SITE ERROR: " + why + " - skipping its cases");
                int i = caseIndex;
                while (i < RenderTest.CASES.size() && RenderTest.CASES.get(i).site == c.site) {
                    summary.add(String.format(Locale.ROOT, "%-24s SKIPPED: %s", RenderTest.CASES.get(i).name, why));
                    i++;
                }
                startCase(i);
                return;
            }
            for (String line : b.lines) say("  " + line);
            say(
                "  box (" + b.box[0]
                    + ", "
                    + b.box[1]
                    + ", "
                    + b.box[2]
                    + ")..("
                    + b.box[3]
                    + ", "
                    + b.box[4]
                    + ", "
                    + b.box[5]
                    + "), render centre "
                    + String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", b.centre[0], b.centre[1], b.centre[2]));
            applyCase(c);
        });
    }

    private void applyCase(RenderTest.Case c) {
        TileEyeStar.debugEventAge = c.eventAge;
        // renders v2: the Eye's event clock and previous kind ({eventIn, prevKind[, crossingIn]}; NaN / -1 live)
        final boolean eye = c.site.eyeTier() > 0 && c.clientLook != null
            && (c.clientLook.length == 2 || c.clientLook.length == 3);
        TileEyeStar.debugEventIn = eye ? c.clientLook[0] : Double.NaN;
        TileEyeStar.debugPrevKind = eye ? (int) c.clientLook[1] : -1;
        TileEyeStar.debugCrossingIn = eye && c.clientLook.length == 3 ? c.clientLook[2] : Double.NaN;
        // renders v2: the client-side look overrides (null / -1: the live synced look)
        final boolean kerr = c.site == RenderTest.Site.KERR_T1 || c.site == RenderTest.Site.KERR_T2;
        TileKerrHole.debugStability = kerr && c.clientLook != null ? c.clientLook[0] : -1f;
        TileQuasarHole.debugLook = !kerr && c.clientLook != null && c.clientLook.length == TileQuasarHole.DBG_LENGTH
            ? c.clientLook.clone()
            : null;
        await(() -> RenderTest.applyCase(c), r -> {
            say("  " + r);
            move();
        });
    }

    /** Moves to the current shot's viewpoint (the server moves the player), then settles. */
    private void move() {
        final boolean night = shot >= RenderTest.VIEWS.length;
        final String view = RenderTest.VIEWS[shot % RenderTest.VIEWS.length];
        final boolean first = shot == 0 && siteFresh;
        await(() -> RenderTest.view(view, night), r -> {
            say("  view " + r);
            settleMin = first ? SITE_MIN : VIEW_MIN;
            settleMax = first ? SITE_MAX : VIEW_MAX;
            stillTicks = 0;
            lastChunks = -1;
            if (dirt != null) dirt.reset();
            go(Phase.SETTLE);
        });
    }

    private void settle() {
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.capabilities.isFlying = true;
        }
        final int chunks = mc.theWorld == null ? -1
            : mc.theWorld.getChunkProvider()
                .getLoadedChunkCount();
        final int queued = renderQueue();
        // the loaded-chunk count flickers by a chunk or two at the render-distance edge; only a real stream counts
        // and nothing counts as settled until the client has the chunk under the camera (else it draws only sky)
        final boolean here = mc.thePlayer != null && mc.theWorld != null
            && !mc.theWorld
                .getChunkFromBlockCoords((int) Math.floor(mc.thePlayer.posX), (int) Math.floor(mc.thePlayer.posZ))
                .isEmpty();
        // (GT's Black Hole Compressor keeps one chunk re-rendering every second, so up to 2 queued updates count as
        // still)
        if (here && Math.abs(chunks - lastChunks) <= 2 && queued <= 2) {
            stillTicks++;
        } else {
            stillTicks = 0;
            if (dirt != null) dirt.reset();
            // the GPU numbers cover the still window only
            GpuTimer.reset();
        }
        lastChunks = chunks;
        if (phaseTicks >= settleMax) {
            say(
                "  settle cap hit (" + settleMax
                    + " ticks; "
                    + chunks
                    + " chunks, "
                    + queued
                    + " render updates queued)");
        } else if (phaseTicks < settleMin || stillTicks < STILL) {
            return;
        }
        go(Phase.SHOOT);
    }

    private void endCase(RenderTest.Case c) {
        int min = Integer.MAX_VALUE;
        int max = 0;
        long sum = 0;
        for (int f : caseFps) {
            min = Math.min(min, f);
            max = Math.max(max, f);
            sum += f;
        }
        final String line = String.format(
            Locale.ROOT,
            "%-24s fps avg %4d  min %4d  max %4d  (%d shots)",
            c.name,
            caseFps.isEmpty() ? 0 : sum / caseFps.size(),
            caseFps.isEmpty() ? 0 : min,
            max,
            caseFps.size());
        summary.add(line);
        say("CASE DONE " + line);
    }

    private void finish() {
        if (phaseTicks == 1) {
            say("=== SUMMARY (fps = the debug counter at each shot) ===");
            for (String line : summary) say(line);
            say(files.size() + " screenshots in " + dir.getAbsolutePath());
            await(RenderTest::releaseAll, r -> {
                say(String.valueOf(r));
                go(Phase.FINISH);
                phaseTicks = 2;
            });
            return;
        }
        if (phase != Phase.FINISH || phaseTicks < 40) return;
        say("all done: closing the client");
        if (log != null) log.close();
        if (fps != null) fps.close();
        if (poses != null) poses.close();
        TileKerrHole.debugStability = -1f;
        TileQuasarHole.debugLook = null;
        TileEyeStar.debugEventAge = -1;
        TileEyeStar.debugEventIn = Double.NaN;
        TileEyeStar.debugPrevKind = -1;
        TileEyeStar.debugCrossingIn = Double.NaN;
        phase = Phase.DONE;
        skipExitConfirmation();
        mc.shutdown();
    }

    /**
     * The GTNH coremod (dreamcraft) cancels {@code Minecraft.shutdown()} and opens a Swing "really quit?" window when
     * its {@code showConfirmExitWindow} option is on, which would leave an unattended run hanging. Switch it off for
     * this session only (the config file is not touched).
     */
    private void skipExitConfirmation() {
        try {
            final Class<?> core = Class
                .forName("com.dreammaster.coremod.DreamCoreMod", true, getClass().getClassLoader());
            final java.lang.reflect.Field flag = core.getDeclaredField("showConfirmExitWindow");
            flag.setAccessible(true);
            flag.setBoolean(null, false);
            say("dreamcraft's exit confirmation switched off for this session");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            // no coremod in this runtime: nothing to switch off
        }
    }

    // ============================================================================= helpers

    /** Any screen that pops up while the harness drives (a pause menu, a chat) is closed; it is logged once. */
    private void keepScreenClear() {
        if (mc.currentScreen == null || mc.currentScreen instanceof GuiDownloadTerrain) return;
        final String name = screenName();
        if (!name.equals(lastScreen)) say("closing screen " + name);
        lastScreen = name;
        mc.displayGuiScreen(null);
    }

    private String screenName() {
        return mc.currentScreen == null ? "none"
            : mc.currentScreen.getClass()
                .getName();
    }

    private static String viewName(int shot) {
        return RenderTest.VIEWS[shot % RenderTest.VIEWS.length] + (shot >= RenderTest.VIEWS.length ? "_night" : "_day");
    }

    /** Where the client camera really is and whether its world has the terrain there (diagnostics). */
    private String clientView() {
        if (mc.thePlayer == null || mc.theWorld == null) return "no client player";
        final int x = (int) Math.floor(mc.thePlayer.posX);
        final int z = (int) Math.floor(mc.thePlayer.posZ);
        final net.minecraft.world.chunk.Chunk chunk = mc.theWorld.getChunkFromBlockCoords(x, z);
        return String.format(
            Locale.ROOT,
            "client at (%.1f, %.1f, %.1f) yaw %.0f pitch %.0f dim %d, chunk %s, ground %s, renderers %s",
            mc.thePlayer.posX,
            mc.thePlayer.posY,
            mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.rotationPitch,
            mc.theWorld.provider.dimensionId,
            chunk == null ? "null" : chunk.isEmpty() ? "EMPTY" : "loaded",
            net.minecraft.block.Block.blockRegistry.getNameForObject(mc.theWorld.getBlock(x, 3, z)),
            mc.renderGlobal.getDebugInfoRenders());
    }

    private static final Pattern FPS = Pattern.compile("^(\\d+) fps, (\\d+) chunk updates");

    /** {fps, chunk updates} from the debug counter (the F3 line), {-1, -1} if unreadable. */
    private int[] debugCounter() {
        final Matcher m = FPS.matcher(mc.debug == null ? "" : mc.debug);
        if (!m.find()) return new int[] { -1, -1 };
        return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
    }

    /** Chunk sections waiting for a render update (RenderGlobal.worldRenderersToUpdate), or -1. */
    private int renderQueue() {
        try {
            final List<?> list = ReflectionHelper
                .getPrivateValue(RenderGlobal.class, mc.renderGlobal, "worldRenderersToUpdate", "field_72767_j");
            return list == null ? -1 : list.size();
        } catch (RuntimeException bad) {
            return -1;
        }
    }
}
