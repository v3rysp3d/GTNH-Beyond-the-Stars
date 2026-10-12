package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY IN-GAME VIDEO RECORDER, the client half. Registered by the client proxy only when the JVM runs with
 * {@code -Dbts.rec=true}; otherwise this class is never loaded. How to drive it: {@code docs/recording.md}.
 * <p>
 * It locks the window (default 1280x720), creates a fresh creative superflat world as the photo harness does
 * ({@link AutoRigClient}), then runs a shot script ({@link RecScript}) scene by scene. The camera is the player, flown
 * and placed by the client on every rendered frame, so orbits, dollies and pans are smooth at any frame rate (the
 * server only teleports it for a {@code goto}, which also loads the chunks). Each scene's recorded spans are piped to
 * ffmpeg ({@link FrameRecorder}) as one segment file; at the end the segments are joined with ffmpeg's concat demuxer
 * into {@code <out>/<script>-<time>.mp4}. The HUD, hand and debug screen are hidden; the caption bar, cards and the
 * time-lapse tag ({@link RecOverlay}) are drawn last in each frame.
 */
@SideOnly(Side.CLIENT)
public final class RecClient {

    /** The switch: {@code -Dbts.rec=true}. */
    public static final boolean ENABLED = Boolean.getBoolean("bts.rec");

    private static final int MAX_TICKS = 4 * 60 * 60 * 20;

    private enum Phase {
        WAIT_MENU,
        WAIT_WORLD,
        WAIT,
        RUN,
        FINISH,
        DONE
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private int width = 1920, height = 1080;
    /** GUI scale for {@code gui} / {@code questbook}: 2 at 720p, 3 at 1080p (the height / 360). */
    private int guiScaleDefault = 3;
    private double fps = 30;
    private File outRoot, runDir, scriptFile;
    private String runName;
    private RecScript script;
    private FrameRecorder rec;
    private String ffmpeg;
    private PrintWriter log;
    private Phase phase = Phase.WAIT_MENU;
    private int ticks, phaseTicks;
    private CompletableFuture<?> pending;
    private Consumer<Object> then;
    private boolean failed;

    // ---- script state
    private int sceneIndex = -1;
    private RecScript.Scene scene;
    private int cmdIndex;
    private final ArrayDeque<Act> acts = new ArrayDeque<>();
    private Act act;
    private final List<File> segments = new ArrayList<>();
    private final List<String> summary = new ArrayList<>();
    private String[] only = new String[0];

    // ---- camera: x, y (the eye), z, yaw, pitch; null = the client player is left alone
    private double[] pose;

    // ---- overlay
    private List<String> card;
    private float cardAlpha = 1;
    private String tag;
    private boolean guiOpen;
    private long sceneRecStart = -1;

    // ---- per-scene frame statistics
    private long renderFrames, lastFrame = -1, maxGap;
    private double recordedSeconds;

    private RecClient() {}

    /** Called by the client proxy when {@link #ENABLED}. */
    public static void register() {
        final RecClient c = new RecClient();
        FMLCommonHandler.instance()
            .bus()
            .register(c);
        MinecraftForge.EVENT_BUS.register(c);
        RenderTest.registerServerHook();
        BeyondTheStars.LOG.warn("[BTS rec] -Dbts.rec=true: the video recorder will drive this client");
    }

    private void say(String line) {
        BeyondTheStars.LOG.info("[BTS rec] " + line);
        if (log != null) log.println(
            String.format(
                Locale.ROOT,
                "[%s t%6d] %s",
                new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date()),
                ticks,
                line));
    }

    // ================================================================================= setup

    private void setUp() {
        final String size = System.getProperty("bts.rec.size", "1920x1080");
        try {
            final String[] wh = size.toLowerCase(Locale.ROOT)
                .split("x");
            width = Integer.parseInt(wh[0].trim());
            height = Integer.parseInt(wh[1].trim());
        } catch (RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS rec] bad -Dbts.rec.size=" + size + ", using 1920x1080");
        }
        guiScaleDefault = Math.max(2, height / 360);
        fps = Double.parseDouble(System.getProperty("bts.rec.fps", "30"));
        final String onlyProp = System.getProperty("bts.rec.only", "")
            .trim();
        if (!onlyProp.isEmpty()) only = onlyProp.split(",");
        final File run = canonical(new File(mc.mcDataDir, ".."));
        outRoot = canonical(new File(System.getProperty("bts.rec.out", new File(run, "recordings").getPath())));
        scriptFile = findScript(System.getProperty("bts.rec.script", ""));
        final String stem = scriptFile == null ? "recording"
            : scriptFile.getName()
                .replaceAll("\\.[^.]*$", "");
        runName = stem + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        runDir = new File(outRoot, runName);
        if (!runDir.isDirectory() && !runDir.mkdirs()) BeyondTheStars.LOG.error("[BTS rec] cannot create " + runDir);
        try {
            log = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(runDir, "rec.log")), StandardCharsets.UTF_8),
                true);
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS rec] cannot open the log", bad);
        }
        say("recorder up; output " + runDir);
        if (scriptFile == null) {
            fail("no shot script: -Dbts.rec.script=<file> (" + System.getProperty("bts.rec.script", "") + ")");
            return;
        }
        try {
            script = RecScript.load(scriptFile);
            Files.copy(scriptFile.toPath(), new File(runDir, scriptFile.getName()).toPath());
        } catch (IOException | RuntimeException bad) {
            fail("the shot script does not load: " + bad.getMessage());
            return;
        }
        int cmds = 0;
        for (RecScript.Scene s : script.scenes) cmds += s.cmds.size();
        say("script " + scriptFile + ": " + (script.scenes.size() - 1) + " scenes, " + cmds + " commands");
        ffmpeg = System.getProperty("bts.rec.ffmpeg", "ffmpeg");
        String encoder = System.getProperty("bts.rec.encoder", "auto")
            .toLowerCase(Locale.ROOT);
        if (encoder.equals("auto")) encoder = FrameRecorder.nvencWorks(ffmpeg) ? "nvenc" : "x264";
        final int crf = Integer.getInteger("bts.rec.crf", 20);
        say("ffmpeg " + ffmpeg + ", encoder " + encoder + ", quality " + crf + ", " + FrameRecorder.fmt(fps) + " fps");
        if (!ffmpegRuns()) {
            fail("ffmpeg does not run: -Dbts.rec.ffmpeg=<path to ffmpeg.exe> (" + ffmpeg + ")");
            return;
        }
        rec = new FrameRecorder(ffmpeg, FrameRecorder.codecArgs(encoder, crf), width, height, fps);
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.renderDistanceChunks = Integer.getInteger("bts.rec.distance", 16);
        mc.gameSettings.fovSetting = Float.parseFloat(System.getProperty("bts.rec.fov", "70"));
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.viewBobbing = false;
        mc.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
        mc.gameSettings.showDebugInfo = false;
        mc.gameSettings.guiScale = guiScaleDefault;
        lockWindow();
        say("window " + mc.displayWidth + "x" + mc.displayHeight + " (video " + width + "x" + height + ")");
    }

    private File findScript(String path) {
        if (path.isEmpty()) return null;
        final File direct = new File(path);
        if (direct.isAbsolute()) return direct.isFile() ? direct : null;
        for (File base : new File[] { new File("."), mc.mcDataDir, new File(mc.mcDataDir, "../..") }) {
            final File f = new File(base, path);
            if (f.isFile()) return canonical(f);
        }
        return null;
    }

    private static File canonical(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException bad) {
            return f.getAbsoluteFile();
        }
    }

    private boolean ffmpegRuns() {
        try {
            final Process p = new ProcessBuilder(ffmpeg, "-hide_banner", "-version").redirectErrorStream(true)
                .redirectOutput(new File(runDir, "ffmpeg-version.txt"))
                .start();
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException | RuntimeException bad) {
            return false;
        }
    }

    private void lockWindow() {
        try {
            Display.setVSyncEnabled(false);
            if (!Display.isFullscreen() && (mc.displayWidth != width || mc.displayHeight != height)) {
                Display.setDisplayMode(new DisplayMode(width, height));
                mc.resize(width, height);
            }
            Display.setResizable(false);
        } catch (Exception bad) {
            say("could not lock the window: " + bad);
        }
    }

    private void fail(String why) {
        say("FAILED: " + why);
        failed = true;
        go(Phase.FINISH);
    }

    // ================================================================================= events

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) return;
        ticks++;
        phaseTicks++;
        if (ticks == 1) setUp();
        if (ticks > MAX_TICKS && phase != Phase.FINISH) {
            say("TIMEOUT after " + ticks + " ticks: finishing with what was recorded");
            endScene();
            go(Phase.FINISH);
        }
        mc.gameSettings.showDebugInfo = false;
        if (phase.ordinal() >= Phase.WAIT.ordinal()) {
            mc.gameSettings.hideGUI = true;
            if (!guiOpen) keepScreenClear();
            if (mc.inGameHasFocus && mc.currentScreen == null) mc.setIngameNotInFocus();
            if (mc.displayWidth != width || mc.displayHeight != height) {
                say("window is " + mc.displayWidth + "x" + mc.displayHeight + ": locking it again");
                lockWindow();
            }
        }
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
            case RUN:
                if (act != null) act.tick();
                advance(System.nanoTime());
                applyPose();
                break;
            case FINISH:
                finish();
                break;
            default:
                break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (phase != Phase.RUN) return;
        final long now = System.nanoTime();
        if (event.phase == TickEvent.Phase.START) {
            if (act != null) act.frame(now);
            applyPose();
            return;
        }
        drawOverlay(now);
        if (act != null && act.records() && !act.done) {
            if (mc.displayWidth == width && mc.displayHeight == height) rec.frame(mc, now, act.until);
            if (lastFrame > 0) maxGap = Math.max(maxGap, now - lastFrame);
            lastFrame = now;
            renderFrames++;
            act.after(now);
        } else {
            lastFrame = -1;
        }
        advance(now);
    }

    /** The HUD (hotbar, chat, crosshair, the debug screen) is not drawn while the recorder runs. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (phase.ordinal() >= Phase.WAIT.ordinal() && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            event.setCanceled(true);
            // the HUD would have set up the 2D projection a GUI screen is then drawn with
            mc.entityRenderer.setupOverlayRendering();
        }
    }

    private void drawOverlay(long now) {
        if (card == null && tag == null && (scene == null || scene.caption.isEmpty())) return;
        RecOverlay.begin(mc);
        try {
            if (card != null) {
                RecOverlay.card(mc, card, cardAlpha);
            } else {
                if (scene != null && !scene.caption.isEmpty()) {
                    final float a = sceneRecStart < 0 ? 1f : (float) Math.min(1.0, (now - sceneRecStart) / 0.4e9);
                    RecOverlay.caption(mc, scene.caption, Math.max(0.05f, a));
                }
                if (tag != null) RecOverlay.tag(mc, tag);
            }
        } finally {
            RecOverlay.end();
        }
    }

    // ================================================================================= world

    private void go(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    private void await(Supplier<?> work, Consumer<Object> next) {
        pending = RenderTest.onServer(work);
        then = next;
        go(Phase.WAIT);
    }

    private void waitMenu() {
        if (mc.currentScreen instanceof GuiMainMenu) {
            final String world = "bts-rec-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            final WorldSettings settings = new WorldSettings(
                20261013L,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                WorldType.FLAT);
            settings.func_82750_a("2;7,2x3,2;1;");
            settings.enableCommands();
            say("creating the world saves/" + world);
            go(Phase.WAIT_WORLD);
            mc.launchIntegratedServer(world, "BTS Recorder", settings);
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
                go(Phase.RUN);
            });
        }
    }

    // ================================================================================= the script

    /** Runs finished acts out and starts the next ones (several instant acts can run in one call). */
    private void advance(long now) {
        for (int guard = 0; guard < 64 && phase == Phase.RUN; guard++) {
            if (act == null) {
                if (acts.isEmpty() && !nextCommand()) return;
                if (acts.isEmpty()) continue;
                act = acts.poll();
                startAct(act, now);
            }
            if (!act.done) return;
            endAct(act);
            act = null;
        }
    }

    /** Compiles the next command into acts (or moves to the next scene). False when the script is done. */
    private boolean nextCommand() {
        if (scene == null || cmdIndex >= scene.cmds.size()) {
            endScene();
            sceneIndex++;
            if (sceneIndex >= script.scenes.size()) {
                go(Phase.FINISH);
                return false;
            }
            scene = script.scenes.get(sceneIndex);
            cmdIndex = 0;
            if (!scene.setup() && !wanted(scene.name)) {
                say("=== scene " + sceneIndex + " " + scene.name + ": skipped (-Dbts.rec.only)");
                cmdIndex = scene.cmds.size();
                return true;
            }
            startScene();
            return true;
        }
        final RecScript.Cmd c = scene.cmds.get(cmdIndex++);
        try {
            compile(c);
        } catch (RuntimeException bad) {
            say("line " + c.line + " '" + c + "': ERROR " + bad.getMessage() + " - skipped");
            BeyondTheStars.LOG.warn("[BTS rec] line " + c.line, bad);
        }
        return true;
    }

    private boolean wanted(String name) {
        if (only.length == 0) return true;
        for (String o : only) if (!o.trim()
            .isEmpty() && name.startsWith(o.trim())) return true;
        return false;
    }

    private void startScene() {
        say(
            "=== scene " + sceneIndex
                + " "
                + scene.name
                + (scene.caption.isEmpty() ? "" : " - \"" + scene.caption + "\""));
        sceneRecStart = -1;
        renderFrames = 0;
        maxGap = 0;
        lastFrame = -1;
        recordedSeconds = 0;
        card = null;
        tag = null;
    }

    private void endScene() {
        if (scene == null) return;
        if (rec != null && rec.isOpen()) {
            final File f = rec.file();
            final long frames = rec.frames, dups = rec.dups, caps = rec.captures, skipped = rec.skipped;
            final int exit = rec.close();
            final double secs = frames / fps;
            final String line = String.format(
                Locale.ROOT,
                "%-22s %6.2f s video (%d frames: %d rendered, %d repeated), %.2f s recorded wall time, render %.1f fps avg, slowest frame %.0f ms, %d frames not needed, ffmpeg exit %d",
                scene.name,
                secs,
                frames,
                caps,
                dups,
                recordedSeconds,
                recordedSeconds > 0 ? renderFrames / recordedSeconds : 0,
                maxGap / 1e6,
                skipped,
                exit);
            say("segment " + f.getName() + ": " + line);
            summary.add(line);
            if (exit == 0 && frames > 0) segments.add(f);
        }
        sceneRecStart = -1;
        card = null;
        tag = null;
        scene = null;
    }

    private void startAct(Act a, long stale) {
        // a fresh clock: closing the previous scene's ffmpeg may have taken a moment since the caller read it
        final long now = Math.max(stale, System.nanoTime());
        a.t0 = now;
        if (a.records()) {
            if (!rec.isOpen()) {
                final File f = new File(runDir, String.format(Locale.ROOT, "%02d_%s.mp4", sceneIndex, scene.name));
                try {
                    rec.open(f);
                } catch (IOException bad) {
                    say("cannot start ffmpeg: " + bad);
                    fail("ffmpeg");
                    return;
                }
            }
            if (sceneRecStart < 0) sceneRecStart = now;
            lastFrame = -1;
        }
        a.start(now);
        if (a.records()) rec.beginSpan(now, a.speed());
        say("  " + a.what);
    }

    private void endAct(Act a) {
        if (a.records()) {
            rec.endSpan();
            recordedSeconds += (System.nanoTime() - a.t0) / 1e9;
        }
        a.end();
    }

    // ---------------------------------------------------------------------------------- compile

    private void compile(RecScript.Cmd c) {
        switch (c.verb) {
            case "card": {
                final Object[] card = RecScript.card(c);
                @SuppressWarnings("unchecked")
                final List<String> lines = (List<String>) card[1];
                acts.add(new Card((Double) card[0], lines));
                break;
            }
            case "hold":
                acts.add(new Hold(Double.parseDouble(c.arg(0))));
                break;
            case "build": {
                final String which = c.opt(0, "both");
                final boolean eye = !which.equals("kerr"), kerr = !which.equals("eye");
                acts.add(new Server("build " + which, () -> AutoRig.build(eye, kerr)));
                break;
            }
            case "goto": {
                final double[] at = resolve(c.arg(0));
                final double[] look = c.kv.containsKey("look") ? resolve(c.kv.get("look")) : null;
                acts.add(new Goto(at, look));
                break;
            }
            case "look": {
                final double[] target = resolve(c.arg(0));
                acts.add(new Pan(target, 0));
                break;
            }
            case "pan": {
                final double[] target = resolve(c.arg(0));
                acts.add(new Pan(target, c.need("seconds")));
                break;
            }
            case "orbit": {
                final double[] centre = resolve(c.arg(0));
                final double[] look = c.kv.containsKey("look") ? resolve(c.kv.get("look")) : centre;
                final Orbit o = new Orbit(
                    centre,
                    look,
                    c.need("radius"),
                    c.num("height", 0),
                    c.num("from", 0),
                    c.num("to", 90),
                    c.need("seconds"),
                    c.kv.getOrDefault("ease", "inout"));
                moveFirst(o.at(0), look);
                acts.add(o);
                break;
            }
            case "dolly": {
                final double[] from = resolve(c.arg(0)), to = resolve(c.arg(1));
                final double[] look = c.kv.containsKey("look") ? resolve(c.kv.get("look")) : null;
                moveFirst(from, look == null ? to : look);
                acts.add(new Dolly(from, to, look, c.need("seconds"), c.kv.getOrDefault("ease", "inout")));
                break;
            }
            case "timelapse":
                acts.add(new Timelapse(Double.parseDouble(c.arg(0)), c.num("speed", 20), c.flag("sky")));
                break;
            case "gui":
                acts.add(new Gui(block(resolve(c.arg(0))), (int) c.num("scale", guiScaleDefault)));
                break;
            case "questbook":
                acts.add(new Questbook(c.opt(0, ""), (int) c.num("scale", guiScaleDefault)));
                break;
            case "close":
                acts.add(new Close());
                break;
            case "place": {
                final String spec = c.arg(0);
                final int[] p = block(resolve(c.arg(1)));
                final String facing = c.kv.getOrDefault("facing", "north");
                acts.add(new Server("place " + spec, () -> RecServer.place(spec, p, RecServer.facing(facing))));
                break;
            }
            case "hologram": {
                final int[] p = block(resolve(c.arg(0)));
                final int tier = (int) c.num("tier", 1);
                if (c.opt(1, "hints")
                    .equals("build")) {
                    acts.add(new Server("hologram build", () -> RecServer.construct(p, tier)));
                } else {
                    acts.add(new Hints(p, tier));
                }
                break;
            }
            case "time": {
                final String t = c.arg(0);
                acts.add(new Server("time " + t, () -> RecServer.time(t)));
                break;
            }
            case "wait":
                acts.add(new Wait(c.arg(0), c.opt(1, ""), (int) c.num("timeout", 600)));
                break;
            case "cell":
                acts.add(new Server("ME cell into the drive", () -> AutoRigAE2.insertCell(RenderTest.world())));
                break;
            case "command": {
                final String cmd = c.rest;
                acts.add(new Server("command " + cmd, () -> RecServer.command(cmd)));
                break;
            }
            default:
                throw new IllegalArgumentException("not a command here: " + c.verb);
        }
    }

    /** Before a move that starts at {@code start}: a goto there first if the camera is not already close to it. */
    private void moveFirst(double[] start, double[] look) {
        final double[] here = pose != null ? pose : playerPose();
        if (here == null || dist(here, start) > 2.5) acts.add(new Goto(start, look));
    }

    /**
     * A place: {@code eye}, {@code eyectrl}, {@code kerr}, {@code keectrl}, {@code placed} or {@code here}, optionally
     * {@code +dx,dy,dz} (e.g. {@code eye+0,30,-80}), or absolute {@code x,y,z}.
     */
    private double[] resolve(String spec) {
        final String s = spec.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty place");
        if (Character.isDigit(s.charAt(0)) || s.charAt(0) == '-') return triple(s);
        final int plus = s.indexOf('+');
        final String name = (plus < 0 ? s : s.substring(0, plus)).toLowerCase(Locale.ROOT);
        final double[] off = plus < 0 ? new double[3] : triple(s.substring(plus + 1));
        final double[] base;
        switch (name) {
            case "eye":
                base = need(AutoRig.eyeCentre, "eye (build eye first)");
                break;
            case "eyectrl":
                base = centre(need(AutoRig.eyeCtrl, "eyectrl (build eye first)"));
                break;
            case "kerr":
                base = need(AutoRig.kerrCentre, "kerr (build kerr first)");
                break;
            case "keectrl":
                base = centre(need(AutoRig.keeCtrl, "keectrl (build kerr first)"));
                break;
            case "placed":
                base = centre(need(RecServer.placed, "placed (place a machine first)"));
                break;
            case "here": {
                final double[] h = pose != null ? pose : playerPose();
                base = need(h, "here (no camera)");
                break;
            }
            default:
                throw new IllegalArgumentException("unknown place '" + name + "'");
        }
        return new double[] { base[0] + off[0], base[1] + off[1], base[2] + off[2] };
    }

    private static <T> T need(T v, String what) {
        if (v == null) throw new IllegalArgumentException("no " + what);
        return v;
    }

    private static double[] centre(int[] p) {
        return new double[] { p[0] + 0.5, p[1] + 0.5, p[2] + 0.5 };
    }

    private static int[] block(double[] p) {
        return new int[] { (int) Math.floor(p[0]), (int) Math.floor(p[1]), (int) Math.floor(p[2]) };
    }

    private static double[] triple(String s) {
        final String[] v = s.split(",");
        if (v.length != 3) throw new IllegalArgumentException("not x,y,z: " + s);
        return new double[] { Double.parseDouble(v[0].trim()), Double.parseDouble(v[1].trim()),
            Double.parseDouble(v[2].trim()) };
    }

    // ---------------------------------------------------------------------------------- camera

    private double[] playerPose() {
        final EntityClientPlayerMP p = mc.thePlayer;
        return p == null ? null : new double[] { p.posX, p.posY, p.posZ, p.rotationYaw, p.rotationPitch };
    }

    /** The pose at {@code eye} looking at {@code target}. */
    private static double[] lookAt(double[] eye, double[] target) {
        final double dx = target[0] - eye[0], dy = target[1] - eye[1], dz = target[2] - eye[2];
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        final double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new double[] { eye[0], eye[1], eye[2], yaw, pitch };
    }

    private static double dist(double[] a, double[] b) {
        final double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Puts the client player (the camera) exactly at the pose, with no interpolation left between ticks. */
    private void applyPose() {
        final EntityClientPlayerMP p = mc.thePlayer;
        if (pose == null || p == null) return;
        p.setPosition(pose[0], pose[1], pose[2]);
        p.prevPosX = p.lastTickPosX = pose[0];
        p.prevPosY = p.lastTickPosY = pose[1];
        p.prevPosZ = p.lastTickPosZ = pose[2];
        p.rotationYaw = p.prevRotationYaw = p.rotationYawHead = p.prevRotationYawHead = (float) pose[3];
        p.rotationPitch = p.prevRotationPitch = (float) pose[4];
        p.motionX = p.motionY = p.motionZ = 0;
        p.capabilities.isFlying = true;
    }

    private static double ease(String kind, double u) {
        final double x = Math.max(0, Math.min(1, u));
        switch (kind) {
            case "linear":
                return x;
            case "in":
                return x * x;
            case "out":
                return 1 - (1 - x) * (1 - x);
            default:
                return 0.5 - 0.5 * Math.cos(Math.PI * x);
        }
    }

    // ================================================================================= acts

    private abstract class Act {

        final String what;
        boolean done;
        long t0, until = Long.MAX_VALUE;

        Act(String what) {
            this.what = what;
        }

        boolean records() {
            return false;
        }

        /** Wall-clock seconds per video second (a time-lapse is faster than 1). */
        double speed() {
            return 1;
        }

        void start(long now) {}

        /** Client tick. */
        void tick() {}

        /** Render frame start (the camera for this frame). */
        void frame(long now) {}

        /** Render frame end, after the frame went to the recorder. */
        void after(long now) {
            if (now >= until) done = true;
        }

        void end() {}

        double u(long now) {
            return until == t0 ? 1 : Math.max(0, Math.min(1, (now - t0) / (double) (until - t0)));
        }

        long nanos(double seconds) {
            return (long) (seconds * 1e9);
        }
    }

    /** A recorded span of {@code seconds}. */
    private class Hold extends Act {

        final double seconds;

        Hold(double seconds) {
            this("hold " + seconds + " s", seconds);
        }

        Hold(String what, double seconds) {
            super(what);
            this.seconds = seconds;
        }

        @Override
        boolean records() {
            return true;
        }

        @Override
        void start(long now) {
            until = now + nanos(seconds * speed());
        }
    }

    private final class Card extends Hold {

        final List<String> lines;

        Card(double seconds, List<String> lines) {
            super("card " + seconds + " s: " + lines, seconds);
            this.lines = lines;
        }

        @Override
        void start(long now) {
            super.start(now);
            card = lines;
            cardAlpha = 0;
        }

        @Override
        void frame(long now) {
            final double s = (now - t0) / 1e9, left = (until - now) / 1e9;
            cardAlpha = (float) Math.max(0, Math.min(1, Math.min(s / 0.5, left / 0.5)));
        }

        @Override
        void end() {
            card = null;
        }
    }

    private final class Orbit extends Hold {

        final double[] centre, look;
        final double radius, h, from, to;
        final String ease;

        Orbit(double[] centre, double[] look, double radius, double h, double from, double to, double seconds,
            String ease) {
            super(
                String.format(
                    Locale.ROOT,
                    "orbit r %.0f h %.0f %.0f..%.0f deg in %.1f s around (%.1f, %.1f, %.1f)",
                    radius,
                    h,
                    from,
                    to,
                    seconds,
                    centre[0],
                    centre[1],
                    centre[2]),
                seconds);
            this.centre = centre;
            this.look = look;
            this.radius = radius;
            this.h = h;
            this.from = from;
            this.to = to;
            this.ease = ease;
        }

        /** Angle 0 = north of the centre (-z), 90 = east (+x). */
        double[] at(double e) {
            final double a = Math.toRadians(from + (to - from) * e);
            return new double[] { centre[0] + radius * Math.sin(a), centre[1] + h, centre[2] - radius * Math.cos(a) };
        }

        @Override
        void start(long now) {
            super.start(now);
            pose = lookAt(at(0), look);
        }

        @Override
        void frame(long now) {
            pose = lookAt(at(ease(ease, u(now))), look);
        }
    }

    private final class Dolly extends Hold {

        final double[] from, to, look;
        final String ease;
        float yaw, pitch;

        Dolly(double[] from, double[] to, double[] look, double seconds, String ease) {
            super("dolly in " + seconds + " s", seconds);
            this.from = from;
            this.to = to;
            this.look = look;
            this.ease = ease;
        }

        double[] at(double e) {
            return new double[] { from[0] + (to[0] - from[0]) * e, from[1] + (to[1] - from[1]) * e,
                from[2] + (to[2] - from[2]) * e };
        }

        @Override
        void start(long now) {
            super.start(now);
            final double[] here = pose != null ? pose : playerPose();
            yaw = here == null ? 0 : (float) here[3];
            pitch = here == null ? 0 : (float) here[4];
            frame(now);
        }

        @Override
        void frame(long now) {
            final double[] p = at(ease(ease, u(now)));
            pose = look != null ? lookAt(p, look) : new double[] { p[0], p[1], p[2], yaw, pitch };
        }
    }

    /** Turns the camera to look at a target: at once ({@code look}) or recorded over {@code seconds} ({@code pan}). */
    private final class Pan extends Hold {

        final double[] target;
        double[] a, b;

        Pan(double[] target, double seconds) {
            super((seconds > 0 ? "pan " + seconds + " s" : "look") + " at " + Arrays.toString(target), seconds);
            this.target = target;
        }

        @Override
        boolean records() {
            return seconds > 0;
        }

        @Override
        void start(long now) {
            super.start(now);
            a = pose != null ? pose.clone() : playerPose();
            if (a == null) {
                done = true;
                return;
            }
            b = lookAt(a, target);
            // the short way round
            while (b[3] - a[3] > 180) b[3] -= 360;
            while (b[3] - a[3] < -180) b[3] += 360;
            if (seconds <= 0) {
                pose = b;
                done = true;
            }
        }

        @Override
        void frame(long now) {
            if (done || a == null) return;
            final double e = ease("inout", u(now));
            pose = new double[] { a[0], a[1], a[2], a[3] + (b[3] - a[3]) * e, a[4] + (b[4] - a[4]) * e };
        }
    }

    /** A time-lapse: {@code seconds} of video recorded over {@code seconds x speed} of real time. */
    private final class Timelapse extends Hold {

        final double speed;
        final boolean sky;

        Timelapse(double seconds, double speed, boolean sky) {
            super("time-lapse " + seconds + " s of video at " + speed + "x" + (sky ? ", sky moving" : ""), seconds);
            this.speed = speed;
            this.sky = sky;
        }

        @Override
        double speed() {
            return speed;
        }

        @Override
        void start(long now) {
            super.start(now);
            tag = "TIME-LAPSE  " + FrameRecorder.fmt(speed) + "x";
            if (sky) RenderTest.onServer(() -> RecServer.daylight(true));
        }

        @Override
        void end() {
            tag = null;
            if (sky) RenderTest.onServer(() -> RecServer.daylight(false));
        }
    }

    /** A server task (not recorded); done when it has run. */
    private final class Server extends Act {

        final Supplier<?> work;
        CompletableFuture<?> f;

        Server(String what, Supplier<?> work) {
            super(what);
            this.work = work;
        }

        @Override
        void start(long now) {
            f = RenderTest.onServer(work);
        }

        @Override
        void tick() {
            if (f == null || !f.isDone()) return;
            try {
                final Object r = f.get();
                if (r instanceof List) for (Object l : (List<?>) r) say("    " + l);
                else say("    " + r);
            } catch (Exception bad) {
                say("    FAILED: " + bad);
            }
            done = true;
        }
    }

    /** Server teleport (loads the chunks), then waits until the chunks around the camera have arrived and rendered. */
    private final class Goto extends Act {

        final double[] at, look;
        CompletableFuture<?> f;
        int ticks, still, lastChunks = -1;

        Goto(double[] at, double[] look) {
            super(String.format(Locale.ROOT, "goto (%.1f, %.1f, %.1f)", at[0], at[1], at[2]));
            this.at = at;
            this.look = look;
        }

        @Override
        void start(long now) {
            pose = null;
            final double[] target = look != null ? look : new double[] { at[0], at[1], at[2] - 10 };
            f = RenderTest.onServer(() -> AutoRig.view(at, target));
        }

        @Override
        void tick() {
            if (f != null) {
                if (!f.isDone()) return;
                try {
                    say("    " + f.get());
                } catch (Exception bad) {
                    say("    goto FAILED: " + bad);
                }
                f = null;
            }
            ticks++;
            final EntityClientPlayerMP p = mc.thePlayer;
            if (p != null) {
                p.motionX = p.motionY = p.motionZ = 0;
                p.capabilities.isFlying = true;
            }
            final int chunks = mc.theWorld == null ? -1
                : mc.theWorld.getChunkProvider()
                    .getLoadedChunkCount();
            final int queued = renderQueue();
            final boolean here = p != null && mc.theWorld != null
                && !mc.theWorld.getChunkFromBlockCoords((int) Math.floor(p.posX), (int) Math.floor(p.posZ))
                    .isEmpty();
            if (here && Math.abs(chunks - lastChunks) <= 2 && queued <= 3) still++;
            else still = 0;
            lastChunks = chunks;
            if (ticks >= 900) say("    settle cap hit (" + chunks + " chunks, " + queued + " queued)");
            else if (ticks < 30 || still < 30) return;
            pose = playerPose();
            done = true;
        }
    }

    /** The server opens a machine's GUI for the player; done once the screen has been up for a second. */
    private final class Gui extends Act {

        final int[] p;
        final int scale;
        CompletableFuture<?> f;
        int up, waited;

        Gui(int[] p, int scale) {
            super("open the GUI at " + RenderTest.pos(p) + " (gui scale " + scale + ")");
            this.p = p;
            this.scale = scale;
        }

        @Override
        void start(long now) {
            guiOpen = true;
            hideNei(true);
            mc.gameSettings.guiScale = scale;
            f = RenderTest.onServer(() -> AutoRig.openGui(p));
        }

        @Override
        void tick() {
            if (f != null) {
                if (!f.isDone()) return;
                try {
                    say("    " + f.get());
                } catch (Exception bad) {
                    say("    FAILED: " + bad);
                }
                f = null;
            }
            waited++;
            if (mc.currentScreen != null) up++;
            if (up >= 20) {
                say(
                    "    screen " + mc.currentScreen.getClass()
                        .getSimpleName());
                done = true;
            } else if (waited >= 300) {
                say("    no screen after 15 s");
                guiOpen = false;
                done = true;
            }
        }
    }

    /** BetterQuesting's quest book (by reflection): the chapter list, or one quest by its id low (high 190). */
    private final class Questbook extends Act {

        final String id;
        final int scale;
        int up, waited;

        Questbook(String id, int scale) {
            super("open the quest book" + (id.isEmpty() ? "" : " at quest " + id));
            this.id = id;
            this.scale = scale;
        }

        @Override
        void start(long now) {
            guiOpen = true;
            hideNei(true);
            mc.gameSettings.guiScale = scale;
            try {
                final GuiScreen screen;
                if (id.isEmpty()) {
                    screen = (GuiScreen) Class.forName("betterquesting.client.gui2.GuiQuestLines")
                        .getConstructor(GuiScreen.class)
                        .newInstance((Object) null);
                } else {
                    final String[] hl = id.split(":");
                    final UUID uuid = hl.length == 2 ? new UUID(Long.parseLong(hl[0]), Long.parseLong(hl[1]))
                        : new UUID(190L, Long.parseLong(id));
                    screen = (GuiScreen) Class.forName("betterquesting.client.gui2.GuiQuest")
                        .getConstructor(GuiScreen.class, UUID.class)
                        .newInstance(null, uuid);
                }
                mc.displayGuiScreen(screen);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException bad) {
                say("    quest book FAILED: " + bad);
                guiOpen = false;
                done = true;
            }
        }

        @Override
        void tick() {
            waited++;
            if (mc.currentScreen != null) up++;
            if (up >= 20 || waited >= 200) done = true;
        }
    }

    private final class Close extends Act {

        Close() {
            super("close the screen");
        }

        @Override
        void start(long now) {
            if (mc.currentScreen != null && mc.thePlayer != null) mc.thePlayer.closeScreen();
            mc.displayGuiScreen(null);
            guiOpen = false;
            hideNei(false);
            mc.gameSettings.guiScale = guiScaleDefault;
            done = true;
        }
    }

    /** The Hologram Projector's hints (client particles) for the multiblock at {@code p}. */
    private final class Hints extends Act {

        final int[] p;
        final int tier;

        Hints(int[] p, int tier) {
            super("hologram hints at " + RenderTest.pos(p));
            this.p = p;
            this.tier = tier;
        }

        @Override
        void start(long now) {
            try {
                final ItemStack projector = new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(1, tier));
                ConstructableUtility.handle(projector, mc.thePlayer, mc.theWorld, p[0], p[1], p[2], 1);
                // (on the client handle() returns false even when the hints are shown)
                say("    hints requested");
            } catch (RuntimeException | LinkageError bad) {
                say("    hints FAILED: " + bad);
            }
            done = true;
        }
    }

    /** Waits (not recorded) for a server condition or a number of seconds. */
    private final class Wait extends Act {

        final String what2, arg;
        final int timeout;
        CompletableFuture<?> f;
        int ticks;

        Wait(String what, String arg, int timeout) {
            super("wait " + what + (arg.isEmpty() ? "" : " " + arg) + " (timeout " + timeout + " s)");
            this.what2 = what;
            this.arg = arg;
            this.timeout = timeout;
        }

        private boolean cond() {
            switch (what2) {
                case "eyelit":
                    return AutoRig.eyeLit();
                case "holeopen":
                    return AutoRig.kee() != null && AutoRig.kee()
                        .isHoleOpen();
                default:
                    return false;
            }
        }

        @Override
        void tick() {
            ticks++;
            if (what2.equals("seconds")) {
                if (ticks >= Double.parseDouble(arg) * 20) done = true;
                return;
            }
            if (f != null && f.isDone()) {
                try {
                    if (Boolean.TRUE.equals(f.get())) {
                        say("    done after " + ticks / 20 + " s");
                        done = true;
                        return;
                    }
                } catch (Exception bad) {
                    say("    FAILED: " + bad);
                }
                f = null;
            }
            if (ticks >= timeout * 20) {
                say("    TIMEOUT, going on");
                done = true;
                return;
            }
            if (f == null && ticks % 20 == 1) f = RenderTest.onServer(this::cond);
        }
    }

    // ================================================================================= finish

    private void finish() {
        if (phaseTicks != 1) {
            if (phaseTicks >= 40) shutDown();
            return;
        }
        endScene();
        if (rec != null && rec.isOpen()) rec.abort();
        File out = null;
        if (!segments.isEmpty()) {
            out = new File(outRoot, runName + ".mp4");
            final File list = new File(runDir, "concat.txt");
            try (PrintWriter w = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(list), StandardCharsets.UTF_8))) {
                for (File s : segments) w.println(
                    "file '" + s.getAbsolutePath()
                        .replace('\\', '/') + "'");
            } catch (IOException bad) {
                say("cannot write " + list + ": " + bad);
            }
            try {
                final Process p = new ProcessBuilder(
                    ffmpeg,
                    "-hide_banner",
                    "-loglevel",
                    "warning",
                    "-y",
                    "-f",
                    "concat",
                    "-safe",
                    "0",
                    "-i",
                    list.getPath(),
                    "-c",
                    "copy",
                    "-movflags",
                    "+faststart",
                    out.getPath()).redirectErrorStream(true)
                        .redirectOutput(new File(runDir, "concat.log"))
                        .start();
                final boolean ok = p.waitFor(300, TimeUnit.SECONDS) && p.exitValue() == 0;
                say((ok ? "joined " : "JOIN FAILED for ") + segments.size() + " segments -> " + out);
            } catch (IOException | InterruptedException bad) {
                say("JOIN FAILED: " + bad);
            }
        }
        say("=== SUMMARY" + (failed ? " (FAILED)" : ""));
        for (String line : summary) say(line);
        say(segments.size() + " segments in " + runDir + (out != null ? "; video " + out : "; no video"));
        if (RenderTest.player() != null || mc.getIntegratedServer() != null) {
            RenderTest.onServer(() -> {
                RecServer.daylight(false);
                return RenderTest.releaseAll();
            });
        }
    }

    private void shutDown() {
        say(
            "all done"
                + (Boolean.parseBoolean(System.getProperty("bts.rec.exit", "true")) ? ": closing the client" : ""));
        if (log != null) log.close();
        log = null;
        phase = Phase.DONE;
        if (!Boolean.parseBoolean(System.getProperty("bts.rec.exit", "true"))) return;
        skipExitConfirmation();
        mc.shutdown();
    }

    // ================================================================================= helpers

    private void skipExitConfirmation() {
        try {
            final Class<?> core = Class
                .forName("com.dreammaster.coremod.DreamCoreMod", true, getClass().getClassLoader());
            final java.lang.reflect.Field flag = core.getDeclaredField("showConfirmExitWindow");
            flag.setAccessible(true);
            flag.setBoolean(null, false);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            // no coremod: nothing to switch off
        }
    }

    /** NEI's item panels off while a GUI is filmed (inventory.hidden), back on after; quiet if NEI is absent. */
    private void hideNei(boolean hide) {
        try {
            final Object tag = Class.forName("codechicken.nei.NEIClientConfig")
                .getMethod("getSetting", String.class)
                .invoke(null, "inventory.hidden");
            tag.getClass()
                .getMethod("setBooleanValue", boolean.class)
                .invoke(tag, hide);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            say("  NEI panels: " + absent);
        }
    }

    private void keepScreenClear() {
        if (mc.currentScreen == null || mc.currentScreen instanceof GuiDownloadTerrain) return;
        say(
            "closing screen " + mc.currentScreen.getClass()
                .getName());
        mc.displayGuiScreen(null);
    }

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
