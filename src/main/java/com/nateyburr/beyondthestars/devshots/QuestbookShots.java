package com.nateyburr.beyondthestars.devshots;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY questbook screenshot harness. Registered by the client proxy only when the JVM runs with
 * {@code -Dbts.qbshots=true}; otherwise this class is never loaded.
 * <p>
 * It sets a fixed window and GUI scale, switches off pause-on-lost-focus, creates (or reuses) the creative superflat
 * world {@code saves/bts-qbshots} (the questbook auto-install runs on its server start as on any server), then executes
 * {@code qbshots/script.txt} line by line. Lines appended to the script while the client runs are picked up, so a
 * session can be driven interactively. BetterQuesting is a runtime-only dependency, so every BQ call is reflective.
 * <p>
 * Script lines ({@code #} starts a comment):
 * <ul>
 * <li>{@code size W H}, {@code scale N} - window size, GUI scale</li>
 * <li>{@code line REF [tray]} - open the quest book on chapter REF (a questIDLow of ours, or a base64 pack id); with
 * {@code tray} the chapter list stays open</li>
 * <li>{@code fit}, {@code zoom Z}, {@code pan X Y} - the chapter map: zoom to fit, set zoom, scroll window top-left in
 * map units</li>
 * <li>{@code traysy Y} - scroll the chapter list</li>
 * <li>{@code quest REF}, {@code descy Y} - open a quest's page, scroll its description</li>
 * <li>{@code hover REF|off}, {@code mouse X Y} - put the virtual mouse on a quest of the open map (BQ draws its
 * tooltip) or at a GUI-space point</li>
 * <li>{@code chatlog} - copy the chat history (command replies) into the log</li>
 * <li>{@code info} - log the open screen's scroll state and quest buttons</li>
 * <li>{@code shot NAME} - save {@code screenshots/qbshots/NAME.png} from the client framebuffer after the next
 * frame</li>
 * <li>{@code chat TEXT} - send a chat line / command as the player; {@code close}; {@code wait TICKS};
 * {@code quit}</li>
 * </ul>
 * The mouse position given to the open screen is replaced with an off-screen point, so a real cursor over the window
 * never adds hover tooltips to a shot.
 */
@SideOnly(Side.CLIENT)
public final class QuestbookShots {

    private static final String WORLD = "bts-qbshots";
    private static final long ID_HIGH = 190L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final File script;
    private final File shots;
    private PrintWriter log;

    private int ticks;
    private boolean worldLaunched;
    private boolean ready;
    private int readyTicks;
    private int done;
    private int waitTicks;
    private String pendingShot;
    private int shotFrames;
    private boolean drawing;
    /** GUI-space mouse position given to the open screen (off-screen unless a {@code hover} is set). */
    private int mouseX = -4000;
    private int mouseY = -4000;

    private QuestbookShots() {
        script = new File(mc.mcDataDir, "qbshots/script.txt");
        shots = new File(mc.mcDataDir, "screenshots/qbshots");
    }

    /** Called by the client proxy when {@code -Dbts.qbshots=true}. */
    public static void register() {
        final QuestbookShots h = new QuestbookShots();
        FMLCommonHandler.instance()
            .bus()
            .register(h);
        MinecraftForge.EVENT_BUS.register(h);
        BeyondTheStars.LOG
            .warn("[BTS qbshots] -Dbts.qbshots=true: the questbook screenshot harness drives this client");
    }

    private void say(String line) {
        BeyondTheStars.LOG.info("[BTS qbshots] " + line);
        if (log != null) log.println(String.format(Locale.ROOT, "[t%6d] %s", ticks, line));
    }

    // ================================================================================ ticks

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks == 1) setUp();
        if (!worldLaunched) {
            if (mc.currentScreen instanceof GuiMainMenu) launchWorld();
            return;
        }
        if (!ready) {
            final boolean in = mc.theWorld != null && mc.thePlayer != null
                && mc.getIntegratedServer() != null
                && !(mc.currentScreen instanceof GuiDownloadTerrain);
            if (in && ++readyTicks > 200) {
                ready = true;
                say("in the world: running " + script);
            }
            return;
        }
        if (pendingShot != null) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        final List<String> lines;
        try {
            if (!script.isFile()) return;
            lines = Files.readAllLines(script.toPath(), StandardCharsets.UTF_8);
        } catch (IOException bad) {
            return;
        }
        // one command per tick; a GUI change gets a few ticks to initialise before the next line
        while (done < lines.size()) {
            final String line = lines.get(done++)
                .trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try {
                run(line);
            } catch (Throwable bad) {
                say("FAILED '" + line + "': " + bad);
            }
            return;
        }
    }

    /** Shots are taken after a full frame (GUI included) has been drawn into the framebuffer. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingShot == null) return;
        // let a couple of frames pass so animated trays and icon caches have settled
        if (++shotFrames < 4) return;
        final String name = pendingShot;
        ScreenShotHelper.saveScreenshot(
            mc.mcDataDir,
            "qbshots/" + name + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        say(
            "SHOT " + name
                + ".png  "
                + mc.displayWidth
                + "x"
                + mc.displayHeight
                + " screen "
                + screenName()
                + " focus "
                + Display.isActive());
        pendingShot = null;
    }

    /** The open screen is drawn with the mouse far off-screen (no hover tooltips from a real cursor). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!ready || drawing || event.gui == null) return;
        event.setCanceled(true);
        drawing = true;
        try {
            event.gui.drawScreen(mouseX, mouseY, event.renderPartialTicks);
        } finally {
            drawing = false;
        }
    }

    // ============================================================================== set up

    private void setUp() {
        try {
            if (!shots.isDirectory()) shots.mkdirs();
            script.getParentFile()
                .mkdirs();
            log = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(new File(script.getParentFile(), "qbshots.log"), true),
                    StandardCharsets.UTF_8),
                true);
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS qbshots] cannot open the log", bad);
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.renderDistanceChunks = 4;
        mc.gameSettings.guiScale = 2;
        mc.gameSettings.saveOptions();
        resize(1920, 1080);
        say("harness up: window " + mc.displayWidth + "x" + mc.displayHeight + ", gui scale 2, no pause on focus loss");
    }

    private void resize(int w, int h) {
        try {
            if (!Display.isFullscreen()) {
                Display.setDisplayMode(new DisplayMode(w, h));
                mc.resize(w, h);
            }
        } catch (Exception bad) {
            say("could not resize the window: " + bad);
        }
    }

    private void launchWorld() {
        final WorldSettings settings = new WorldSettings(
            20261013L,
            WorldSettings.GameType.CREATIVE,
            false,
            false,
            WorldType.FLAT);
        settings.func_82750_a("2;7,2x3,2;1;");
        settings.enableCommands();
        final boolean reuse = new File(mc.mcDataDir, "saves/" + WORLD + "/level.dat").isFile();
        say((reuse ? "reusing" : "creating") + " the world saves/" + WORLD);
        worldLaunched = true;
        mc.launchIntegratedServer(WORLD, "BTS Questbook Shots", settings);
    }

    // ============================================================================ commands

    private void run(String line) throws Exception {
        say("> " + line);
        final String[] a = line.split("\\s+", 2);
        final String arg = a.length > 1 ? a[1].trim() : "";
        final String[] p = arg.isEmpty() ? new String[0] : arg.split("\\s+");
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "size":
                resize(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                reinitScreen();
                break;
            case "scale":
                mc.gameSettings.guiScale = Integer.parseInt(p[0]);
                reinitScreen();
                break;
            case "line":
                unhover();
                openLine(id(p[0]), p.length > 1 && "tray".equalsIgnoreCase(p[1]));
                waitTicks = 10;
                break;
            case "fit":
                call(questCanvas(), "fitToWindow");
                break;
            case "zoom": {
                final Object cv = questCanvas();
                cv.getClass()
                    .getMethod("setZoom", float.class)
                    .invoke(cv, Float.parseFloat(p[0]));
                break;
            }
            case "pan": {
                final Object cv = questCanvas();
                call(cv, "refreshScrollBounds");
                cv.getClass()
                    .getMethod("setScrollX", int.class)
                    .invoke(cv, Integer.parseInt(p[0]));
                cv.getClass()
                    .getMethod("setScrollY", int.class)
                    .invoke(cv, Integer.parseInt(p[1]));
                call(cv, "updatePanelScroll");
                break;
            }
            case "traysy":
                scrollY(field(mc.currentScreen, "cvLines"), Integer.parseInt(p[0]));
                break;
            case "quest":
                unhover();
                openQuest(id(p[0]));
                waitTicks = 10;
                break;
            case "descy":
                scrollY(field(mc.currentScreen, "csDesc"), Integer.parseInt(p[0]));
                break;
            case "hover":
                if ("off".equalsIgnoreCase(p[0])) unhover();
                else hover(id(p[0]));
                break;
            case "mouse":
                mouseX = Integer.parseInt(p[0]);
                mouseY = Integer.parseInt(p[1]);
                break;
            case "chatlog":
                chatLog();
                break;
            case "info":
                info();
                break;
            case "shot":
                pendingShot = p[0];
                shotFrames = 0;
                break;
            case "chat":
                mc.thePlayer.sendChatMessage(arg);
                waitTicks = 40;
                break;
            case "close":
                mc.displayGuiScreen(null);
                break;
            case "wait":
                waitTicks = Integer.parseInt(p[0]);
                break;
            case "quit":
                say("quit: closing the client");
                if (log != null) log.close();
                skipExitConfirmation();
                mc.shutdown();
                break;
            default:
                say("unknown command: " + line);
        }
    }

    private void reinitScreen() {
        final GuiScreen s = mc.currentScreen;
        if (s != null) mc.displayGuiScreen(s);
    }

    /** A questIDLow of ours ({@code new UUID(190, low)}) or a BetterQuesting base64 id. */
    private static UUID id(String ref) {
        if (ref.matches("\\d+")) return new UUID(ID_HIGH, Long.parseLong(ref));
        final ByteBuffer b = ByteBuffer.wrap(
            Base64.getUrlDecoder()
                .decode(
                    ref.replace('+', '-')
                        .replace('/', '_')));
        return new UUID(b.getLong(), b.getLong());
    }

    private void openLine(UUID line, boolean tray) throws Exception {
        final Class<?> settings = Class.forName("betterquesting.api.storage.BQ_Settings");
        settings.getField("viewMode")
            .setBoolean(null, true);
        settings.getField("lockTray")
            .setBoolean(null, tray);
        settings.getField("skipHome")
            .setBoolean(null, true);
        final Class<?> gql = Class.forName("betterquesting.client.gui2.GuiQuestLines");
        final Field sel = gql.getDeclaredField("selectedLineId");
        sel.setAccessible(true);
        sel.set(null, line);
        final Constructor<?> c = gql.getConstructor(GuiScreen.class);
        mc.displayGuiScreen((GuiScreen) c.newInstance((GuiScreen) null));
    }

    private void openQuest(UUID quest) throws Exception {
        final Class<?> gq = Class.forName("betterquesting.client.gui2.GuiQuest");
        final Constructor<?> c = gq.getConstructor(GuiScreen.class, UUID.class);
        mc.displayGuiScreen((GuiScreen) c.newInstance(null, quest));
    }

    private Object questCanvas() throws Exception {
        return field(mc.currentScreen, "cvQuest");
    }

    private static Object field(Object o, String name) throws Exception {
        if (o == null) throw new IllegalStateException("no screen open");
        Class<?> k = o.getClass();
        while (k != null) {
            try {
                final Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException next) {
                k = k.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " in " + o.getClass());
    }

    private static Object call(Object o, String name) throws Exception {
        final Method m = o.getClass()
            .getMethod(name);
        return m.invoke(o);
    }

    private static void scrollY(Object canvas, int y) throws Exception {
        if (canvas == null) throw new IllegalStateException("no such canvas");
        call(canvas, "refreshScrollBounds");
        canvas.getClass()
            .getMethod("setScrollY", int.class)
            .invoke(canvas, y);
        call(canvas, "updatePanelScroll");
    }

    private static String rect(Object r) throws Exception {
        return String.format(
            Locale.ROOT,
            "(%s,%s %sx%s)",
            call(r, "getX"),
            call(r, "getY"),
            call(r, "getWidth"),
            call(r, "getHeight"));
    }

    private String canvasState(String name, Object cv) throws Exception {
        if (cv == null) return name + ": none";
        return name + ": zoom "
            + call(cv, "getZoom")
            + " scroll ("
            + call(cv, "getScrollX")
            + ","
            + call(cv, "getScrollY")
            + ") bounds "
            + rect(call(cv, "getScrollBounds"))
            + " window "
            + rect(call(cv, "getTransform"));
    }

    private void info() throws Exception {
        final GuiScreen s = mc.currentScreen;
        say(
            "screen " + screenName()
                + ", display "
                + mc.displayWidth
                + "x"
                + mc.displayHeight
                + ", gui scale "
                + mc.gameSettings.guiScale);
        if (s == null) return;
        final String k = s.getClass()
            .getSimpleName();
        if ("GuiQuestLines".equals(k)) {
            say("  " + canvasState("map", questCanvas()));
            say("  " + canvasState("chapters", field(s, "cvLines")));
            final Collection<?> buttons = (Collection<?>) call(questCanvas(), "getQuestButtons");
            for (Object b : buttons) {
                final Object stored = call(b, "getStoredValue");
                final UUID id = (UUID) ((Map.Entry<?, ?>) stored).getKey();
                final String ref = id.getMostSignificantBits() == ID_HIGH ? String.valueOf(id.getLeastSignificantBits())
                    : encode(id);
                say("  quest " + ref + " " + rect(field(b, "rect")));
            }
        } else if ("GuiQuest".equals(k)) {
            say("  " + canvasState("desc", field(s, "csDesc")));
        }
    }

    private void unhover() {
        mouseX = -4000;
        mouseY = -4000;
    }

    /** Puts the (virtual) mouse on a quest's button in the open chapter map, so BQ draws its tooltip. */
    private void hover(UUID quest) throws Exception {
        final Object cv = questCanvas();
        final Object t = call(cv, "getTransform");
        final float z = (Float) call(cv, "getZoom");
        final int sx = (Integer) call(cv, "getScrollX");
        final int sy = (Integer) call(cv, "getScrollY");
        for (Object b : (Collection<?>) call(cv, "getQuestButtons")) {
            final Object stored = call(b, "getStoredValue");
            if (!quest.equals(((Map.Entry<?, ?>) stored).getKey())) continue;
            final Object r = field(b, "rect");
            final float cx = (Integer) call(r, "getX") + (Integer) call(r, "getWidth") / 2f;
            final float cy = (Integer) call(r, "getY") + (Integer) call(r, "getHeight") / 2f;
            mouseX = Math.round((Integer) call(t, "getX") + (cx - sx) * z);
            mouseY = Math.round((Integer) call(t, "getY") + (cy - sy) * z);
            say("  hover at gui (" + mouseX + "," + mouseY + ")");
            return;
        }
        say("  hover: quest " + quest + " is not on this map");
    }

    /** Copies the chat history (newest last) into the log: command replies are not logged anywhere else. */
    private void chatLog() throws Exception {
        final List<?> lines = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
            net.minecraft.client.gui.GuiNewChat.class,
            mc.ingameGUI.getChatGUI(),
            "chatLines",
            "field_146252_h");
        for (int i = lines.size() - 1; i >= 0; i--) {
            final net.minecraft.client.gui.ChatLine l = (net.minecraft.client.gui.ChatLine) lines.get(i);
            say(
                "  chat: " + l.func_151461_a()
                    .getUnformattedText());
        }
    }

    private static String encode(UUID id) {
        final ByteBuffer b = ByteBuffer.allocate(16);
        b.putLong(id.getMostSignificantBits());
        b.putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder()
            .encodeToString(b.array());
    }

    private String screenName() {
        return mc.currentScreen == null ? "none"
            : mc.currentScreen.getClass()
                .getSimpleName();
    }

    /** dreamcraft's "really quit?" window would leave the run hanging; switch it off for this session only. */
    private void skipExitConfirmation() {
        try {
            final Class<?> core = Class
                .forName("com.dreammaster.coremod.DreamCoreMod", true, getClass().getClassLoader());
            final Field flag = core.getDeclaredField("showConfirmExitWindow");
            flag.setAccessible(true);
            flag.setBoolean(null, false);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            // no coremod in this runtime
        }
    }
}
