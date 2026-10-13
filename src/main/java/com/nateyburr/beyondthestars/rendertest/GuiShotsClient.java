package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * DEV-ONLY GUI screenshot harness (controller GUI cleanup, visual check against the mockups). Registered by the client
 * proxy only when the JVM runs with {@code -Dbts.guishots=true}; otherwise this class is never loaded and nothing here
 * runs.
 * <p>
 * It sets a fixed window, creates (or reuses) the creative superflat world {@code saves/bts-guishots}, then executes
 * the lines appended to {@code run/client/guishots/queue.txt} one at a time (the file is emptied at start, so a driver
 * script can append to it while the client runs). Everything is logged to {@code guishots/guishots.log}, including the
 * chat the bench commands print. Commands:
 * <ul>
 * <li>{@code cmd <command>}: the server runs it as the player (the bench commands {@code /btseye}, {@code /btskerr},
 * ...).</li>
 * <li>{@code tp <x> <y> <z>}: moves the (flying) player.</li>
 * <li>{@code wait <ticks>}.</li>
 * <li>{@code gui <x> <y> <z>}: the server opens the GT machine GUI at that block; waits until the screen is up.</li>
 * <li>{@code scale <n>}: GUI scale (applied to the open screen at once).</li>
 * <li>{@code mouse <x> <y>} / {@code mouse off}: a fake mouse position in GUI units relative to the main panel's top
 * left (re-applied every frame, so hover tooltips show without moving the real cursor). {@code off} parks it at the
 * screen corner, which is also the default (no stray tooltips).</li>
 * <li>{@code click <x> <y> [button]}: the fake mouse to that point, then a press and release there.</li>
 * <li>{@code scroll <x> <y> <n>}: the fake mouse there, then n wheel notches down (negative: up).</li>
 * <li>{@code shot <name>}: saves the next frame to {@code screenshots/guishots/<name>.png} and logs the open panels'
 * areas in window pixels (for cropping).</li>
 * <li>{@code nei <usage|recipe> <item> [tab <text>] [page <n>]}: NEI's recipe screen for an item
 * ({@code ItemList.<name>},
 * {@code sl.<SLCircuitItems field>}, {@code cc.<CircuitComponent name>} (the NAC's packaged component) or
 * {@code modid:name[:meta]}); {@code tab} selects the first tab whose name or handler class contains the text
 * ({@code _} for a space), {@code page} turns that many pages on (circuits v2 C7).</li>
 * <li>{@code shotw <name>}: like {@code shot}, but of the world with no screen open (HUD hidden for the frame).</li>
 * <li>{@code tp <x> <y> <z> [yaw] [pitch]}: the pitch defaults to 30 (looking down a little).</li>
 * <li>{@code close}: closes the screen. {@code say <text>}: logs {@code MARK <text>} (a driver waits for it).</li>
 * <li>{@code quit}: closes the client.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class GuiShotsClient {

    /** The switch: {@code -Dbts.guishots=true}. */
    public static final boolean ENABLED = Boolean.getBoolean("bts.guishots");

    private static final String WORLD = "bts-guishots";
    private static final int WIDTH = Integer.getInteger("bts.guishots.width", 1920);
    private static final int HEIGHT = Integer.getInteger("bts.guishots.height", 1080);

    private final Minecraft mc = Minecraft.getMinecraft();
    private final File dir;
    private final File queue;
    private PrintWriter log;
    private long queueRead;
    private final Deque<String> lines = new ArrayDeque<>();

    private int ticks;
    private boolean inWorld;
    private boolean launched;
    private int worldTicks;

    /** The current command, or null. */
    private String current;
    private int currentTicks;
    private CompletableFuture<?> pending;
    private int waitLeft;
    private String shotName;
    /** shotw: the next frame is saved even with no screen open. */
    private boolean shotWorld;
    private boolean hudWas;
    private int clickButton = -1;
    private int scrollNotches;
    /** gui: the controller waited for on the client, then opened by the server. */
    private int[] guiAt;

    /** Fake mouse in GUI units relative to the main panel (or -1: parked in the corner). */
    private int fakeX = -1;
    private int fakeY = -1;

    private static Field mouseX;
    private static Field mouseY;

    private GuiShotsClient() {
        dir = new File(mc.mcDataDir, "guishots");
        queue = new File(dir, "queue.txt");
    }

    /** Called by the client proxy when {@link #ENABLED}. */
    public static void register() {
        final GuiShotsClient h = new GuiShotsClient();
        FMLCommonHandler.instance()
            .bus()
            .register(h);
        MinecraftForge.EVENT_BUS.register(h);
        RenderTest.registerServerHook();
        BeyondTheStars.LOG
            .warn("[BTS gui shots] -Dbts.guishots=true: the GUI screenshot harness will drive this client");
    }

    private void say(String line) {
        BeyondTheStars.LOG.info("[BTS gui shots] " + line);
        if (log != null) log.println(String.format(Locale.ROOT, "[t%6d] %s", ticks, line));
    }

    private void setUp() {
        if (!dir.isDirectory() && !dir.mkdirs()) BeyondTheStars.LOG.error("[BTS gui shots] cannot create " + dir);
        final File shots = new File(mc.mcDataDir, "screenshots/guishots");
        if (!shots.isDirectory() && !shots.mkdirs()) BeyondTheStars.LOG.error("[BTS gui shots] cannot create " + shots);
        try {
            log = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "guishots.log")), StandardCharsets.UTF_8),
                true);
            new FileOutputStream(queue).close(); // empty the queue: a restart never replays old lines
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS gui shots] cannot open the log or queue", bad);
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
        mc.gameSettings.renderDistanceChunks = 6;
        mc.gameSettings.guiScale = 3;
        try {
            if (!Display.isFullscreen()) {
                Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
                mc.resize(WIDTH, HEIGHT);
            }
        } catch (Exception bad) {
            say("could not resize the window: " + bad);
        }
        say("harness up: window " + mc.displayWidth + "x" + mc.displayHeight + ", queue " + queue.getAbsolutePath());
    }

    // =========================================================================== events

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message != null) say("  chat: " + event.message.getUnformattedText());
    }

    /** Who closes a machine screen (logged with the call path, so a GUI that shuts itself can be traced). */
    @SubscribeEvent
    public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (event.gui == null && mc.currentScreen instanceof IMuiScreen) {
            final StringBuilder sb = new StringBuilder("  screen closed by:");
            final StackTraceElement[] st = new Throwable().getStackTrace();
            for (int i = 1; i < Math.min(st.length, 16); i++) sb.append(" <- ")
                .append(st[i]);
            say(sb.toString());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks == 1) setUp();
        if (!inWorld) {
            enterWorld();
            return;
        }
        if (mc.theWorld == null && (mc.currentScreen instanceof GuiMainMenu
            || mc.currentScreen instanceof net.minecraft.client.gui.GuiDisconnected)) {
            say("disconnected: back into the world");
            if (!(mc.currentScreen instanceof GuiMainMenu)) mc.displayGuiScreen(new GuiMainMenu());
            inWorld = false;
            launched = false;
            worldTicks = 0;
            pending = null;
            finish();
            return;
        }
        if (mc.thePlayer != null) {
            // stay where the last tp put us: flying, no drift (a fall takes the player out of GUI range)
            if (!mc.thePlayer.capabilities.isFlying) {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
        }
        readQueue();
        if (pending != null) {
            if (!pending.isDone()) return;
            try {
                final Object r = pending.get();
                if (r != null && !r.toString()
                    .isEmpty()) say("  " + r);
            } catch (Exception bad) {
                say("  server task FAILED: " + bad);
            }
            pending = null;
        }
        if (current == null) {
            if (lines.isEmpty()) return;
            start(lines.poll());
        } else {
            currentTicks++;
            step();
        }
    }

    /** Fake mouse: set LWJGL's position just before the frame reads it (the next frame's poll overwrites it). */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!inWorld) return;
        if (event.phase == TickEvent.Phase.START) {
            applyMouse();
            return;
        }
        if (shotName != null && (mc.currentScreen != null || shotWorld)) {
            final String name = shotName;
            shotName = null;
            if (shotWorld && currentTicks < 5) {
                shotName = name; // let a HUD-less frame render first
                return;
            }
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "guishots/" + name + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            say("SHOT " + name + " " + panels());
            if (shotWorld) {
                shotWorld = false;
                mc.gameSettings.hideGUI = hudWas;
            }
            finish();
        }
    }

    // ========================================================================== world

    private void enterWorld() {
        if (!launched) {
            if (mc.currentScreen instanceof GuiMainMenu) {
                final WorldSettings settings = new WorldSettings(
                    20261013L,
                    WorldSettings.GameType.CREATIVE,
                    false,
                    false,
                    WorldType.FLAT);
                settings.func_82750_a("2;7,2x3,2;1;");
                settings.enableCommands();
                say("launching the world saves/" + WORLD);
                launched = true;
                mc.launchIntegratedServer(WORLD, "BTS GUI shots", settings);
            }
            return;
        }
        final boolean ready = mc.theWorld != null && mc.thePlayer != null
            && mc.getIntegratedServer() != null
            && !(mc.currentScreen instanceof GuiDownloadTerrain);
        if (!ready) return;
        if (++worldTicks < 100) return;
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
            return;
        }
        inWorld = true;
        hideNei();
        pending = RenderTest.onServer(GuiShotsClient::prepare);
        say("in the world");
    }

    private static String prepare() {
        final net.minecraft.world.WorldServer w = RenderTest.world();
        w.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        w.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        w.setWorldTime(6000);
        final EntityPlayerMP p = RenderTest.player();
        if (p == null) return "no player";
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.capabilities.disableDamage = true;
        p.sendPlayerAbilities();
        return "world prepared (" + p.getCommandSenderName() + ")";
    }

    // ========================================================================== queue

    private void readQueue() {
        if (!queue.isFile() || queue.length() <= queueRead) return;
        try (RandomAccessFile f = new RandomAccessFile(queue, "r")) {
            f.seek(queueRead);
            final byte[] buf = new byte[(int) (f.length() - queueRead)];
            f.readFully(buf);
            final String text = new String(buf, StandardCharsets.UTF_8);
            final int last = text.lastIndexOf('\n');
            if (last < 0) return; // wait for a whole line
            queueRead += text.substring(0, last + 1)
                .getBytes(StandardCharsets.UTF_8).length;
            for (String line : text.substring(0, last)
                .split("\r?\n")) {
                if (!line.trim()
                    .isEmpty()
                    && !line.trim()
                        .startsWith("#"))
                    lines.add(line.trim());
            }
        } catch (IOException bad) {
            say("queue read failed: " + bad);
        }
    }

    private void start(String line) {
        current = line;
        currentTicks = 0;
        say("> " + line);
        final String[] a = line.split("\\s+");
        final String rest = line.indexOf(' ') < 0 ? "" : line.substring(line.indexOf(' ') + 1);
        try {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "cmd":
                    pending = RenderTest.onServer(() -> runCommand(rest));
                    waitLeft = 2;
                    break;
                case "tp": {
                    final double x = Double.parseDouble(a[1]), y = Double.parseDouble(a[2]),
                        z = Double.parseDouble(a[3]);
                    final float yaw = a.length > 4 ? Float.parseFloat(a[4]) : 0f;
                    final float pitch = a.length > 5 ? Float.parseFloat(a[5]) : 30f;
                    pending = RenderTest.onServer(() -> {
                        final EntityPlayerMP p = RenderTest.player();
                        if (p == null) return "no player";
                        p.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
                        return "teleported";
                    });
                    waitLeft = 40;
                    break;
                }
                case "wait":
                    waitLeft = Integer.parseInt(a[1]);
                    break;
                case "gui": {
                    // the client must hold the controller's tile entity first (a fresh build streams in later)
                    guiAt = new int[] { Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]) };
                    hideNei();
                    waitLeft = -3;
                    break;
                }
                case "dump": {
                    final ModularScreen ms = muiScreen();
                    if (ms == null) {
                        say("  not a ModularUI screen");
                    } else {
                        try {
                            final com.cleanroommc.modularui.value.sync.SyncHandler<?> rows = ms.getSyncManager()
                                .getMainPSM()
                                .findSyncHandlerNullable(a.length > 1 ? a[1] : "btsRows", 0);
                            final String v = rows instanceof com.cleanroommc.modularui.value.sync.StringSyncValue
                                ? ((com.cleanroommc.modularui.value.sync.StringSyncValue) rows).getStringValue()
                                : String.valueOf(rows);
                            say(
                                "  sync " + (a.length > 1 ? a[1] : "btsRows")
                                    + " = "
                                    + (v == null ? "null"
                                        : v.length() + " chars: "
                                            + v.substring(0, Math.min(200, v.length()))
                                                .replace((char) 1, '|')
                                                .replace((char) 2, '~')));
                        } catch (RuntimeException bad) {
                            say("  sync read failed: " + bad);
                        }
                        dumpTree(ms.getMainPanel(), 0);
                    }
                    finish();
                    return;
                }
                case "find": {
                    // find <MTE class simple name> <x> <y> <z> <r>: where those machines stand (server)
                    final String cls = a[1];
                    final int x = Integer.parseInt(a[2]), y = Integer.parseInt(a[3]), z = Integer.parseInt(a[4]),
                        r = Integer.parseInt(a[5]);
                    pending = RenderTest.onServer(() -> find(cls, x, y, z, r));
                    waitLeft = 1;
                    break;
                }
                case "probe":
                    pending = RenderTest.onServer(GuiShotsClient::probe);
                    waitLeft = 1;
                    break;
                case "guinow": {
                    final int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]), z = Integer.parseInt(a[3]);
                    pending = RenderTest.onServer(() -> openGui(x, y, z));
                    waitLeft = 0;
                    break;
                }
                case "use": {
                    // a real right-click on the block from the client (the way a player opens it)
                    final int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]), z = Integer.parseInt(a[3]);
                    hideNei();
                    final boolean ok = mc.playerController.onPlayerRightClick(
                        mc.thePlayer,
                        mc.theWorld,
                        mc.thePlayer.getHeldItem(),
                        x,
                        y,
                        z,
                        2,
                        net.minecraft.util.Vec3.createVectorHelper(x + 0.5, y + 0.5, z));
                    say("  right-click sent: " + ok);
                    waitLeft = -1;
                    break;
                }
                case "scale":
                    mc.gameSettings.guiScale = Integer.parseInt(a[1]);
                    rescale();
                    waitLeft = 10;
                    break;
                case "mouse":
                    if (a[1].equalsIgnoreCase("off")) {
                        fakeX = -1;
                        fakeY = -1;
                    } else {
                        fakeX = Integer.parseInt(a[1]);
                        fakeY = Integer.parseInt(a[2]);
                    }
                    waitLeft = 4;
                    break;
                case "click":
                    fakeX = Integer.parseInt(a[1]);
                    fakeY = Integer.parseInt(a[2]);
                    clickButton = a.length > 3 ? Integer.parseInt(a[3]) : 0;
                    waitLeft = 12;
                    break;
                case "scroll":
                    fakeX = Integer.parseInt(a[1]);
                    fakeY = Integer.parseInt(a[2]);
                    scrollNotches = Integer.parseInt(a[3]);
                    waitLeft = 12;
                    break;
                case "nei":
                    openNei(a);
                    waitLeft = 30;
                    break;
                case "shotw":
                    if (mc.currentScreen != null) mc.displayGuiScreen(null);
                    hudWas = mc.gameSettings.hideGUI;
                    mc.gameSettings.hideGUI = true;
                    shotWorld = true;
                    shotName = a[1];
                    waitLeft = -2;
                    break;
                case "shot":
                    if (mc.currentScreen == null) {
                        say("  no screen open: shot skipped");
                        finish();
                        return;
                    }
                    shotName = a[1];
                    waitLeft = -2; // finished by the render tick
                    break;
                case "close":
                    if (mc.thePlayer != null && mc.currentScreen != null) mc.thePlayer.closeScreen();
                    mc.displayGuiScreen(null);
                    fakeX = -1;
                    fakeY = -1;
                    waitLeft = 10;
                    break;
                case "say":
                    say("MARK " + rest);
                    finish();
                    break;
                case "quit":
                    say("quitting");
                    if (log != null) log.close();
                    skipExitConfirmation();
                    mc.shutdown();
                    break;
                default:
                    say("  unknown command");
                    finish();
            }
        } catch (RuntimeException bad) {
            say("  FAILED: " + bad);
            finish();
        }
    }

    private void step() {
        if (pending != null) return;
        if (waitLeft == -1) { // gui: until the screen has been up for 20 ticks
            if (currentTicks % 20 == 1) say(
                "  screen " + (mc.currentScreen == null ? "none"
                    : mc.currentScreen.getClass()
                        .getSimpleName())
                    + ", client container "
                    + (mc.thePlayer == null ? "?"
                        : mc.thePlayer.openContainer.getClass()
                            .getSimpleName()));
            if (mc.currentScreen != null && currentTicks > 20) {
                say(
                    "  screen " + mc.currentScreen.getClass()
                        .getSimpleName() + " " + panels());
                finish();
            } else if (currentTicks > 300) {
                say("  no screen after 15 s");
                finish();
            }
            return;
        }
        if (waitLeft == -3) {
            final TileEntity te = mc.theWorld == null ? null : mc.theWorld.getTileEntity(guiAt[0], guiAt[1], guiAt[2]);
            if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).getMetaTileEntity() != null
                && currentTicks > 10) {
                final int[] p = guiAt;
                pending = RenderTest.onServer(() -> openGui(p[0], p[1], p[2]));
                waitLeft = -1;
                currentTicks = 0;
            } else if (currentTicks > 1200) {
                say(
                    "  the client never got the machine at " + guiAt[0]
                        + " "
                        + guiAt[1]
                        + " "
                        + guiAt[2]
                        + " (client tile: "
                        + (te == null ? "none"
                            : te.getClass()
                                .getSimpleName())
                        + ")");
                finish();
            }
            return;
        }
        if (waitLeft == -2) {
            if (currentTicks > 100) {
                say("  shot timed out");
                shotName = null;
                finish();
            }
            return;
        }
        if (currentTicks == 6 && (clickButton >= 0 || scrollNotches != 0)) {
            final ModularScreen ms = muiScreen();
            if (ms == null) {
                say("  not a ModularUI screen");
            } else if (clickButton >= 0) {
                // the widget under the fake mouse (MUI2's own hover, computed in the last frame) gets the press
                final com.cleanroommc.modularui.api.widget.IWidget hovered = ms.getContext()
                    .getHovered();
                say(
                    "  mouse " + ms.getContext()
                        .getMouseX()
                        + ","
                        + ms.getContext()
                            .getMouseY()
                        + " hovers "
                        + (hovered == null ? "nothing"
                            : hovered.getClass()
                                .getSimpleName()));
                if (hovered instanceof com.cleanroommc.modularui.api.widget.Interactable) {
                    final com.cleanroommc.modularui.api.widget.Interactable w = (com.cleanroommc.modularui.api.widget.Interactable) hovered;
                    final Object r = w.onMousePressed(clickButton);
                    w.onMouseRelease(clickButton);
                    say("  pressed: " + r);
                }
            } else {
                final UpOrDown dir = scrollNotches > 0 ? UpOrDown.DOWN : UpOrDown.UP;
                // every widget under the fake mouse, top first, until one takes the wheel (a list's scroll area)
                int taken = 0;
                try {
                    // a scroll area reads the wheel amount from the context (normally the input event's)
                    final Field wheel = com.cleanroommc.modularui.screen.viewport.GuiContext.class
                        .getDeclaredField("mouseWheel");
                    wheel.setAccessible(true);
                    wheel.setInt(ms.getContext(), dir.isDown() ? -120 : 120);
                } catch (ReflectiveOperationException | RuntimeException bad) {
                    say("  wheel amount not set: " + bad);
                }
                for (int n = 0; n < Math.abs(scrollNotches); n++) {
                    for (com.cleanroommc.modularui.api.widget.IWidget w : ms.getContext()
                        .getAllBelowMouse()) {
                        if (w instanceof com.cleanroommc.modularui.api.widget.Interactable
                            && ((com.cleanroommc.modularui.api.widget.Interactable) w).onMouseScroll(dir, 1)) {
                            taken++;
                            break;
                        }
                    }
                }
                say("  wheel taken " + taken + " of " + Math.abs(scrollNotches));
                say("  scrolled " + scrollNotches);
            }
            clickButton = -1;
            scrollNotches = 0;
        }
        if (currentTicks >= waitLeft) finish();
    }

    private void finish() {
        current = null;
        currentTicks = 0;
        waitLeft = 0;
    }

    // ========================================================================== server side

    private static String runCommand(String command) {
        final EntityPlayerMP p = RenderTest.player();
        if (p == null) return "no player";
        final int r = MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(p, command);
        return "command returned " + r;
    }

    private static String find(String cls, int x, int y, int z, int r) {
        final StringBuilder sb = new StringBuilder("found:");
        for (Object o : RenderTest.world().loadedTileEntityList) {
            if (!(o instanceof IGregTechTileEntity)) continue;
            final IGregTechTileEntity te = (IGregTechTileEntity) o;
            final IMetaTileEntity mte = te.getMetaTileEntity();
            if (mte == null || !mte.getClass()
                .getSimpleName()
                .equals(cls)) continue;
            if (Math.abs(te.getXCoord() - x) > r || Math.abs(te.getYCoord() - y) > r
                || Math.abs(te.getZCoord() - z) > r) continue;
            sb.append(" [")
                .append(te.getXCoord())
                .append(' ')
                .append(te.getYCoord())
                .append(' ')
                .append(te.getZCoord())
                .append(" facing ")
                .append(te.getFrontFacing())
                .append(']');
        }
        return sb.toString();
    }

    /** The server's view of the open container (diagnostics). */
    private static String probe() {
        final EntityPlayerMP p = RenderTest.player();
        if (p == null) return "no player";
        final net.minecraft.inventory.Container c = p.openContainer;
        final String extra = "";
        return "server container " + c.getClass()
            .getSimpleName()
            + ", canInteractWith "
            + c.canInteractWith(p)
            + extra
            + String.format(Locale.ROOT, ", player at %.1f %.1f %.1f", p.posX, p.posY, p.posZ);
    }

    private static String openGui(int x, int y, int z) {
        final EntityPlayerMP player = RenderTest.player();
        final TileEntity te = RenderTest.world()
            .getTileEntity(x, y, z);
        if (player == null || !(te instanceof IGregTechTileEntity))
            return "no player or no machine at " + x + " " + y + " " + z;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(mte instanceof gregtech.api.metatileentity.CommonMetaTileEntity)) return "not a GT MTE";
        ((gregtech.api.metatileentity.CommonMetaTileEntity) mte).openGui(player);
        return "GUI opened: " + mte.getClass()
            .getSimpleName()
            + ", server container "
            + player.openContainer.getClass()
                .getSimpleName();
    }

    // ========================================================================== client helpers

    private ModularScreen muiScreen() {
        final GuiScreen s = mc.currentScreen;
        return s instanceof IMuiScreen ? ((IMuiScreen) s).getScreen() : null;
    }

    private int factor() {
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
    }

    private void rescale() {
        if (mc.currentScreen == null) return;
        final ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        mc.currentScreen.setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());
    }

    /** Every open panel's area in window pixels (x y w h), main panel first, plus the GUI scale. */
    private String panels() {
        final ModularScreen ms = muiScreen();
        if (ms == null) return "(not a ModularUI screen)";
        final int f = factor();
        final StringBuilder sb = new StringBuilder("scale " + f + " panels");
        final List<ModularPanel> open = ms.getPanelManager()
            .getOpenPanels();
        final ModularPanel main = ms.getMainPanel();
        appendArea(sb, main, f);
        for (ModularPanel p : open) if (p != main) appendArea(sb, p, f);
        return sb.toString();
    }

    private static void appendArea(StringBuilder sb, ModularPanel p, int f) {
        final Area a = p.getArea();
        sb.append(" [")
            .append(p.getName())
            .append(' ')
            .append(a.x * f)
            .append(' ')
            .append(a.y * f)
            .append(' ')
            .append(a.width * f)
            .append(' ')
            .append(a.height * f)
            .append(']');
    }

    private void dumpTree(com.cleanroommc.modularui.api.widget.IWidget w, int depth) {
        if (depth > 5) return;
        final Area a = w.getArea();
        final StringBuilder pad = new StringBuilder();
        for (int i = 0; i < depth; i++) pad.append("  ");
        final List<com.cleanroommc.modularui.api.widget.IWidget> kids = w.getChildren();
        say(
            "  " + pad
                + w.getClass()
                    .getSimpleName()
                + (w.isEnabled() ? "" : " (disabled)")
                + " "
                + a.rx
                + ","
                + a.ry
                + " "
                + a.width
                + "x"
                + a.height
                + (kids.isEmpty() ? "" : " [" + kids.size() + "]"));
        int shown = 0;
        for (com.cleanroommc.modularui.api.widget.IWidget k : kids) {
            if (shown++ > 12) {
                say("  " + pad + "  ...");
                break;
            }
            dumpTree(k, depth + 1);
        }
    }

    private void applyMouse() {
        if (mc.currentScreen == null) return;
        try {
            if (mouseX == null) {
                mouseX = Mouse.class.getDeclaredField("x");
                mouseY = Mouse.class.getDeclaredField("y");
                mouseX.setAccessible(true);
                mouseY.setAccessible(true);
            }
            final int f = factor();
            final ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int gx = 0, gy = sr.getScaledHeight() - 1;
            final ModularScreen ms = muiScreen();
            if (fakeX >= 0 && ms != null) {
                final Area a = ms.getMainPanel()
                    .getArea();
                gx = a.x + fakeX;
                gy = a.y + fakeY;
            } else if (fakeX >= 0 && mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
                // a plain container screen (a chest, NEI's recipe screen): relative to its top left
                final int[] origin = containerOrigin(
                    (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen);
                gx = origin[0] + fakeX;
                gy = origin[1] + fakeY;
            }
            mouseX.setInt(null, gx * f + f / 2);
            mouseY.setInt(null, (sr.getScaledHeight() - 1 - gy) * f + f / 2);
        } catch (ReflectiveOperationException | RuntimeException bad) {
            say("fake mouse failed: " + bad);
        }
    }

    /** dreamcraft's "really quit?" window would hang an unattended shutdown: off for this session only. */
    private static void skipExitConfirmation() {
        try {
            final Field flag = Class.forName("com.dreammaster.coremod.DreamCoreMod")
                .getDeclaredField("showConfirmExitWindow");
            flag.setAccessible(true);
            flag.setBoolean(null, false);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            // no coremod in this runtime
        }
    }

    private static int[] containerOrigin(net.minecraft.client.gui.inventory.GuiContainer gc) {
        try {
            final Field left = net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredField("guiLeft");
            final Field top = net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredField("guiTop");
            left.setAccessible(true);
            top.setAccessible(true);
            return new int[] { left.getInt(gc), top.getInt(gc) };
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return new int[] { 0, 0 };
        }
    }

    /** nei usage|recipe <item> [tab <text>] [page <n>] (circuits v2 C7: the chain and the raised Piko in NEI). */
    private void openNei(String[] a) {
        if (a.length < 3) {
            say("  usage: nei <usage|recipe> <item> [tab <text>] [page <n>]");
            return;
        }
        String tab = "";
        int pages = 0;
        for (int i = 3; i + 1 < a.length; i++) {
            if (a[i].equalsIgnoreCase("tab")) tab = a[i + 1].replace('_', ' ')
                .toLowerCase(Locale.ROOT);
            if (a[i].equalsIgnoreCase("page")) pages = Integer.parseInt(a[i + 1]);
        }
        final net.minecraft.item.ItemStack stack = neiStack(a[2]);
        if (stack == null) {
            say("  no item for '" + a[2] + "'");
            return;
        }
        try {
            final String cls = a[1].equalsIgnoreCase("usage") ? "codechicken.nei.recipe.GuiUsageRecipe"
                : "codechicken.nei.recipe.GuiCraftingRecipe";
            final Object ok = Class.forName(cls)
                .getMethod("openRecipeGui", String.class, Object[].class)
                .invoke(null, "item", new Object[] { stack });
            say("  NEI " + a[1] + " for " + stack.getDisplayName() + ": " + ok);
            if (!Boolean.TRUE.equals(ok)) return;
            final Class<?> gr = Class.forName("codechicken.nei.recipe.GuiRecipe");
            if (!gr.isInstance(mc.currentScreen)) {
                say("  NEI screen not up: " + mc.currentScreen);
                return;
            }
            final List<?> handlers = (List<?>) gr.getField("currenthandlers")
                .get(mc.currentScreen);
            final StringBuilder names = new StringBuilder();
            int found = -1;
            for (int i = 0; i < handlers.size(); i++) {
                final Object h = handlers.get(i);
                String name;
                try {
                    name = String.valueOf(
                        h.getClass()
                            .getMethod("getRecipeName")
                            .invoke(h));
                } catch (ReflectiveOperationException none) {
                    name = "?";
                }
                int n = -1;
                try {
                    n = (Integer) h.getClass()
                        .getMethod("numRecipes")
                        .invoke(h);
                } catch (ReflectiveOperationException | RuntimeException none) {
                    // not counted
                }
                names.append(i)
                    .append('=')
                    .append(name)
                    .append(" [")
                    .append(n)
                    .append("] ");
                if (found < 0 && !tab.isEmpty()
                    && (name.toLowerCase(Locale.ROOT)
                        .contains(tab)
                        || h.getClass()
                            .getName()
                            .toLowerCase(Locale.ROOT)
                            .contains(tab)))
                    found = i;
            }
            say("  NEI tabs: " + names);
            if (!tab.isEmpty()) {
                if (found < 0) {
                    say("  no tab matching '" + tab + "'");
                } else {
                    final java.lang.reflect.Method set = gr.getDeclaredMethod("setRecipePage", int.class);
                    set.setAccessible(true);
                    set.invoke(mc.currentScreen, found);
                    say("  NEI tab " + found + " selected");
                }
            }
            if (pages > 0) {
                final java.lang.reflect.Method next = gr.getDeclaredMethod("nextPage");
                next.setAccessible(true);
                for (int i = 0; i < pages; i++) next.invoke(mc.currentScreen);
                say(
                    "  NEI page now " + gr.getField("page")
                        .getInt(mc.currentScreen));
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException bad) {
            say("  NEI FAILED: " + bad);
        }
    }

    /** Shared with the recorder's {@code nei}: ItemList.<name>, sl.<field>, cc.<component>, mod:item[:meta]. */
    static net.minecraft.item.ItemStack neiStack(String spec) {
        try {
            if (spec.startsWith("ItemList.")) {
                return gregtech.api.enums.ItemList.valueOf(spec.substring(9))
                    .get(1);
            }
            if (spec.startsWith("sl.")) {
                final Object item = com.nateyburr.beyondthestars.items.SLCircuitItems.class.getField(spec.substring(3))
                    .get(null);
                return item == null ? null : new net.minecraft.item.ItemStack((net.minecraft.item.Item) item, 1, 0);
            }
            if (spec.startsWith("cc.")) {
                return gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent
                    .valueOf(spec.substring(3))
                    .getFakeStack(1);
            }
            final String[] p = spec.split(":");
            final net.minecraft.item.Item item = cpw.mods.fml.common.registry.GameRegistry.findItem(p[0], p[1]);
            return item == null ? null
                : new net.minecraft.item.ItemStack(item, 1, p.length > 2 ? Integer.parseInt(p[2]) : 0);
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return null;
        }
    }

    private void hideNei() {
        try {
            final Object tag = Class.forName("codechicken.nei.NEIClientConfig")
                .getMethod("getSetting", String.class)
                .invoke(null, "inventory.hidden");
            tag.getClass()
                .getMethod("setBooleanValue", boolean.class)
                .invoke(tag, true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            say("NEI panels: " + absent);
        }
    }
}
