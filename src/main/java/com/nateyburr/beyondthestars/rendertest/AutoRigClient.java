package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * DEV-ONLY automation photo tour ({@link AutoRig}), the client half. A fixed 1280x720 window; a fresh creative
 * superflat world per run; the rigs are built, then a script of steps runs: camera moves (the server teleports the
 * player), shots that wait for a world condition seen by THIS client (a dust lit or unlit, a lamp on), waits on server
 * state (the star lit, the hole open), GUI shots (the server opens the machine's GUI for the player) and a tooltip
 * shot. Pictures go to {@code screenshots/autorig/}, the shot log to {@code autorig.log} next to them, the per-second
 * rig logs ({@code eye.tsv}, {@code kerr.tsv}) are written by the server half.
 */
@SideOnly(Side.CLIENT)
public final class AutoRigClient {

    private static final int WIDTH = 1280, HEIGHT = 720;
    private static final int MAX_TICKS = 75 * 60 * 20;
    /** How long the star must have burned before the run ends (seconds), {@code -Dbts.autorig.lit=}. */
    private static final int LIT_TARGET = Integer.getInteger("bts.autorig.lit", 720);
    private static final int STILL = 40;

    private enum Phase {
        WAIT_MENU,
        WAIT_WORLD,
        WAIT,
        RUN,
        SETTLE,
        COND,
        SHOOT,
        FINISH,
        DONE
    }

    // ------------------------------------------------------------------------------- steps

    private abstract static class Step {

        final String label;

        Step(String label) {
            this.label = label;
        }
    }

    /** A server action (its line goes to the log). */
    private static final class Do extends Step {

        final Supplier<String> server;

        Do(String label, Supplier<String> server) {
            super(label);
            this.server = server;
        }
    }

    /** Polls a server condition once a second until true or the timeout. */
    private static final class Wait extends Step {

        final BooleanSupplier server;
        final int timeoutSec;

        Wait(String label, BooleanSupplier server, int timeoutSec) {
            super(label);
            this.server = server;
            this.timeoutSec = timeoutSec;
        }
    }

    /** Camera: {eye, target}, computed on the server. */
    private static final class Move extends Step {

        final Supplier<double[][]> where;
        final boolean night, fresh;

        Move(String label, Supplier<double[][]> where, boolean night, boolean fresh) {
            super(label);
            this.where = where;
            this.night = night;
            this.fresh = fresh;
        }
    }

    /** A world shot once {@code cond} holds on this client (or at the timeout). */
    private static final class Shot extends Step {

        final BooleanSupplier cond;
        final int timeoutTicks;

        Shot(String file, BooleanSupplier cond, int timeoutTicks) {
            super(file);
            this.cond = cond;
            this.timeoutTicks = timeoutTicks;
        }
    }

    /** A GUI shot: the server opens a GUI for the player, the client shoots it with the GUI on screen. */
    private static final class Gui extends Step {

        final Supplier<String> open;
        final int scale;

        Gui(String file, Supplier<String> open) {
            this(file, open, 0);
        }

        Gui(String file, Supplier<String> open, int scale) {
            super(file);
            this.open = open;
            this.scale = scale;
        }
    }

    /** A tooltip shot of an item. */
    private static final class Tip extends Step {

        final Supplier<ItemStack> stack;

        Tip(String file, Supplier<ItemStack> stack) {
            super(file);
            this.stack = stack;
        }
    }

    // ------------------------------------------------------------------------------- state

    private final Minecraft mc = Minecraft.getMinecraft();
    private final File dir;
    private PrintWriter log;
    private Phase phase = Phase.WAIT_MENU;
    private int ticks, phaseTicks;
    private CompletableFuture<?> pending;
    private Consumer<Object> then;
    private final List<Step> steps = new ArrayList<>();
    private int index = -1;
    private int stillTicks, lastChunks = -1, settleMin, settleMax;
    private boolean guiMode;
    private int shots;
    private String world;

    private AutoRigClient() {
        dir = new File(mc.mcDataDir, "screenshots/autorig");
    }

    /** Called instead of the render-QA client when {@link AutoRig#ENABLED}. */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new AutoRigClient());
        RenderTest.registerServerHook();
        BeyondTheStars.LOG
            .warn("[BTS autorig] -Dbts.rendertest.cases=automation: the automation photo tour will drive this client");
    }

    private void say(String line) {
        BeyondTheStars.LOG.info("[BTS autorig] " + line);
        if (log != null) log.println(String.format(Locale.ROOT, "[t%6d] %s", ticks, line));
    }

    // ------------------------------------------------------------------------------- script

    private static double[][] v(double[] eye, double[] target) {
        return new double[][] { eye, target };
    }

    private static double[] add(double[] a, double x, double y, double z) {
        return new double[] { a[0] + x, a[1] + y, a[2] + z };
    }

    private boolean dustLit(int[] p) {
        return mc.theWorld != null && mc.theWorld.getBlock(p[0], p[1], p[2]) == Blocks.redstone_wire
            && mc.theWorld.getBlockMetadata(p[0], p[1], p[2]) > 0;
    }

    private boolean dustOff(int[] p) {
        return mc.theWorld != null && mc.theWorld.getBlock(p[0], p[1], p[2]) == Blocks.redstone_wire
            && mc.theWorld.getBlockMetadata(p[0], p[1], p[2]) == 0;
    }

    private void script() {
        final int min = 20 * 60;
        // ---------------- Rig A while the upkeep valves stock the ignition (Status bit 1 on)
        steps.add(new Move("Eye overview", () -> {
            final double[] c = AutoRig.eyeCentre;
            return v(add(c, 46, 30, -76), add(c, 0, -6, -18));
        }, false, true));
        steps.add(new Shot("a01_eye_overview_dark", null, 0));
        steps.add(
            new Move(
                "upkeep station (Helium)",
                () -> v(AutoRig.upHe.atD(8, 3, 5), AutoRig.upHe.atD(1.5, -0.3, 0.3)),
                false,
                false));
        steps.add(new Shot("a02_upkeep_valve_open", () -> dustLit(AutoRig.upHe.at(3, 0, 0)), 3 * min));
        steps.add(
            new Move(
                "Status hatch (top of the line)",
                () -> v(AutoRig.fStatus.atD(6, 3, 5), AutoRig.fStatus.atD(1, 0.8, 0.5)),
                false,
                false));
        steps.add(new Shot("a03_status_hatch_lit", () -> dustLit(AutoRig.fStatus.at(1, 1, 0)), 3 * min));
        steps.add(new Move("Status staircase (side)", () -> {
            final AutoRig.Frame F = AutoRig.fStatus;
            final double[] mid = new double[] { AutoRig.stairMid[0] + 0.5, AutoRig.stairMid[1],
                AutoRig.stairMid[2] + 0.5 };
            return v(add(mid, F.s.offsetX * 48 + F.o.offsetX * 4, 6, F.s.offsetZ * 48 + F.o.offsetZ * 4), mid);
        }, false, false));
        steps.add(new Shot("a04_status_staircase", () -> dustLit(AutoRig.fStatus.at(1, 1, 0)), 2 * min));
        // ---------------- Rig B: the start-up in front of the camera
        steps.add(new Move("Kerr overview", () -> {
            final double[] c = AutoRig.kerrCentre;
            return v(add(c, 36, 16, -58), add(c, 0, -6, -4));
        }, false, true));
        steps.add(new Shot("b01_kerr_overview_closed", null, 0));
        steps.add(
            new Move(
                "seed station",
                () -> v(AutoRig.kSeed.atD(4.5, 1.8, 3.5), AutoRig.kSeed.atD(0.8, 0.6, 0.8)),
                false,
                false));
        steps.add(new Shot("b02_seed_station_closed_bit_on", () -> dustLit(AutoRig.kSeed.at(1, 1, 0)), 2 * min));
        steps.add(new Do("the 64k cell into the ME drive", () -> AutoRigAE2.insertCell(RenderTest.world())));
        steps.add(
            new Wait(
                "the hole opens",
                () -> AutoRig.kee() != null && AutoRig.kee()
                    .isHoleOpen(),
                120));
        steps.add(new Shot("b03_seed_station_open_bit_off", () -> dustOff(AutoRig.kSeed.at(1, 1, 0)), 2 * min));
        steps.add(
            new Move(
                "bolt station",
                () -> v(AutoRig.kBolt.atD(4.5, 1.8, 3.5), AutoRig.kBolt.atD(0.8, 0.6, 0.8)),
                false,
                false));
        steps.add(new Shot("b04_bolt_station_bits_off", () -> dustOff(AutoRig.kBolt.at(1, 1, 0)), 3 * min));
        steps.add(new Shot("b05_bolt_station_bolts_short_on", () -> dustLit(AutoRig.kBolt.at(1, 1, 0)), 4 * min));
        steps
            .add(new Move("ME core", () -> v(AutoRig.kBolt.atD(11, 3, 5), AutoRig.kBolt.atD(5, 0, 0.4)), false, false));
        steps.add(new Shot("b06_me_core_level_emitter", null, 0));
        steps.add(new Move("Kerr overview (open)", () -> {
            final double[] c = AutoRig.kerrCentre;
            return v(add(c, 36, 16, -58), add(c, 0, -6, -4));
        }, false, false));
        steps.add(new Shot("b07_kerr_overview_open", null, 0));
        steps.add(new Move("KEE controller", () -> {
            final double[] p = new double[] { AutoRig.keeCtrl[0] + 0.5, AutoRig.keeCtrl[1] + 0.5,
                AutoRig.keeCtrl[2] + 0.5 };
            return v(add(p, 0, 0.2, -3.5), p);
        }, false, false));
        steps.add(new Gui("b08_kee_gui", () -> AutoRig.openGui(AutoRig.keeCtrl)));
        // ---------------- Rig A lit: the trims
        steps.add(new Wait("the star lights", AutoRig::eyeLit, 15 * 60));
        steps.add(
            new Move(
                "Helium trim station",
                () -> v(AutoRig.fHe.atD(4.2, 2.3, 3.8), AutoRig.fHe.atD(0.9, 0.7, 1.3)),
                false,
                true));
        steps.add(new Shot("a05_trim_helium_off", () -> dustOff(AutoRig.fHe.at(1, 1, 0)), 4 * min));
        steps.add(new Shot("a06_trim_helium_on", () -> dustLit(AutoRig.fHe.at(1, 1, 0)), 6 * min));
        steps.add(
            new Move(
                "Hydrogen trim station",
                () -> v(AutoRig.fH.atD(4.2, 2.3, 3.8), AutoRig.fH.atD(0.9, 0.7, 1.3)),
                false,
                false));
        steps.add(new Shot("a07_trim_hydrogen_station", null, 0));
        steps.add(
            new Move(
                "NOT gate station",
                () -> v(AutoRig.fNot.atD(6, 2.5, 5), AutoRig.fNot.atD(1, 0.8, 2)),
                false,
                false));
        steps.add(
            new Shot("a08_not_gate_in_band", () -> mc.theWorld != null && lampOn(AutoRig.fNot.at(1, 1, 4)), 3 * min));
        steps.add(
            new Move(
                "Transition comparator",
                () -> v(AutoRig.fTrans.atD(2.2, 2.4, 3.2), AutoRig.fTrans.atD(1.1, 0.9, 0)),
                false,
                false));
        steps.add(new Shot("a09_transition_comparator", null, 0));
        steps.add(new Move("Eye controller", () -> {
            final double[] p = new double[] { AutoRig.eyeCtrl[0] + 0.5, AutoRig.eyeCtrl[1] + 0.5,
                AutoRig.eyeCtrl[2] + 0.5 };
            return v(add(p, 0, 0.2, -3.5), p);
        }, false, false));
        steps.add(new Gui("a10_eye_gui", () -> AutoRig.openGui(AutoRig.eyeCtrl)));
        steps.add(
            new Move(
                "Helium Trims hatch",
                () -> v(AutoRig.fHe.atD(3.5, 1.2, 0), AutoRig.fHe.atD(0, 1, 0)),
                false,
                false));
        steps.add(new Gui("a11_trims_hatch_gui", () -> AutoRig.openGui(AutoRig.fHe.at(0, 1, 0)), 2));
        steps.add(
            new Tip(
                "a12_directive_hatch_tooltip",
                () -> BTSMachines.directiveHatch == null ? null : BTSMachines.directiveHatch.copy()));
        steps.add(new Move("Eye overview (lit)", () -> {
            final double[] c = AutoRig.eyeCentre;
            return v(add(c, 46, 30, -76), add(c, 0, -6, -18));
        }, false, false));
        steps.add(new Shot("a13_eye_overview_lit", null, 0));
        steps.add(new Move("Eye overview (night)", () -> {
            final double[] c = AutoRig.eyeCentre;
            return v(add(c, 46, 30, -76), add(c, 0, -6, -18));
        }, true, false));
        steps.add(new Shot("a14_eye_overview_night", null, 0));
        steps.add(
            new Move(
                "Helium trim station (night)",
                () -> v(AutoRig.fHe.atD(4.2, 2.3, 3.8), AutoRig.fHe.atD(0.9, 0.7, 1.3)),
                true,
                false));
        steps.add(new Shot("a15_trim_helium_on_night", () -> dustLit(AutoRig.fHe.at(1, 1, 0)), 6 * min));
        steps.add(
            new Move(
                "day again",
                () -> v(AutoRig.fHe.atD(4.2, 2.3, 3.8), AutoRig.fHe.atD(0.9, 0.7, 1.3)),
                false,
                false));
        // ---------------- let it run, then a last look
        steps.add(
            new Wait("the star has burned " + LIT_TARGET + " s", () -> AutoRig.litSeconds() >= LIT_TARGET, 40 * 60));
        steps.add(
            new Move(
                "upkeep station (end)",
                () -> v(AutoRig.upHe.atD(8, 3, 5), AutoRig.upHe.atD(1.5, -0.3, 0.3)),
                false,
                false));
        steps.add(new Shot("a16_upkeep_station_end", null, 0));
    }

    private boolean lampOn(int[] p) {
        return mc.theWorld.getBlock(p[0], p[1], p[2]) == Blocks.lit_redstone_lamp;
    }

    // ------------------------------------------------------------------------------- ticks

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) return;
        ticks++;
        phaseTicks++;
        if (ticks == 1) setUp();
        if (ticks > MAX_TICKS && phase != Phase.FINISH) {
            say("TIMEOUT after " + ticks + " ticks: closing");
            pending = null;
            then = null;
            go(Phase.FINISH);
        }
        if (!guiMode && phase.ordinal() >= Phase.WAIT.ordinal() && phase != Phase.FINISH) keepScreenClear();
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
            case COND:
                cond();
                break;
            case RUN:
                if (--waitTicks <= 0) pollWait(waiting, waited + 1);
                break;
            case FINISH:
                finish();
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase != Phase.SHOOT) return;
        if (!guiMode && mc.currentScreen != null) return;
        final Step s = steps.get(index);
        final String file = s.label + ".png";
        ScreenShotHelper
            .saveScreenshot(mc.mcDataDir, "autorig/" + file, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        shots++;
        say("SHOT " + file);
        await(AutoRig::status, r -> {
            say("  at the shot: " + r);
            if (guiMode) {
                if (mc.thePlayer != null) mc.thePlayer.closeScreen();
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                mc.gameSettings.guiScale = 0;
                guiMode = false;
                hideNei(false);
            }
            next();
        });
    }

    private void go(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    private void await(Supplier<?> work, Consumer<Object> next) {
        pending = RenderTest.onServer(work);
        then = next;
        go(Phase.WAIT);
    }

    private void next() {
        index++;
        if (index >= steps.size()) {
            go(Phase.FINISH);
            return;
        }
        final Step s = steps.get(index);
        say("--- step " + (index + 1) + "/" + steps.size() + ": " + s.label);
        if (s instanceof Do) {
            await(((Do) s).server::get, r -> {
                say("  " + r);
                next();
            });
        } else if (s instanceof Wait) {
            pollWait((Wait) s, 0);
        } else if (s instanceof Move) {
            final Move m = (Move) s;
            await(() -> {
                final double[][] w = m.where.get();
                return AutoRig.view(w[0], w[1], m.night);
            }, r -> {
                say("  view " + r);
                settleMin = m.fresh ? 160 : 50;
                settleMax = m.fresh ? 1600 : 600;
                stillTicks = 0;
                lastChunks = -1;
                go(Phase.SETTLE);
            });
        } else if (s instanceof Shot) {
            condTicks = 0;
            go(((Shot) s).cond == null ? Phase.SHOOT : Phase.COND);
        } else if (s instanceof Gui) {
            guiMode = true;
            hideNei(true);
            mc.gameSettings.hideGUI = false;
            mc.gameSettings.guiScale = ((Gui) s).scale;
            await(((Gui) s).open::get, r -> {
                say("  " + r);
                go(Phase.COND);
            });
        } else if (s instanceof Tip) {
            final ItemStack stack = ((Tip) s).stack.get();
            if (stack == null) {
                say("  no item for the tooltip");
                next();
                return;
            }
            guiMode = true;
            hideNei(true);
            mc.gameSettings.hideGUI = false;
            mc.gameSettings.guiScale = 2;
            mc.displayGuiScreen(new TipScreen(stack));
            go(Phase.COND);
        }
    }

    private void pollWait(Wait w, int elapsed) {
        await(() -> w.server.getAsBoolean(), r -> {
            if (Boolean.TRUE.equals(r)) {
                say("  done after " + elapsed + " s");
                next();
            } else if (elapsed >= w.timeoutSec) {
                say("  TIMEOUT after " + elapsed + " s, going on");
                next();
            } else {
                phase = Phase.RUN;
                waitTicks = 20;
                waiting = w;
                waited = elapsed;
            }
        });
    }

    private int waitTicks, waited;
    private int condTicks;
    private static final int HOLD = 6;
    private Wait waiting;

    private void cond() {
        final Step s = steps.get(index);
        if (s instanceof Shot) {
            final Shot sh = (Shot) s;
            // the condition must hold for HOLD ticks, so the chunk re-render (lit dust, a lamp) is in the frame
            if (sh.cond.getAsBoolean()) {
                if (++condTicks >= HOLD) {
                    say("  condition met (held " + HOLD + " ticks) after " + phaseTicks + " ticks");
                    go(Phase.SHOOT);
                }
            } else {
                condTicks = 0;
            }
            if (phase == Phase.SHOOT) return;
            if (phaseTicks >= sh.timeoutTicks) {
                say("  condition NOT met in " + sh.timeoutTicks + " ticks: shooting anyway");
                go(Phase.SHOOT);
            }
            return;
        }
        // Gui / Tip: the screen must be up for 2 s
        if (mc.currentScreen != null && phaseTicks >= 40) {
            say(
                "  screen " + mc.currentScreen.getClass()
                    .getSimpleName());
            go(Phase.SHOOT);
        } else if (phaseTicks >= 200) {
            say(
                "  no screen after 10 s (" + (mc.currentScreen == null ? "none"
                    : mc.currentScreen.getClass()
                        .getName())
                    + "): skipping");
            guiMode = false;
            mc.gameSettings.hideGUI = true;
            mc.gameSettings.guiScale = 0;
            next();
        }
    }

    // ------------------------------------------------------------------------------- setup

    private void setUp() {
        if (!dir.isDirectory() && !dir.mkdirs()) BeyondTheStars.LOG.error("[BTS autorig] cannot create " + dir);
        try {
            log = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "autorig.log")), StandardCharsets.UTF_8),
                true);
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] cannot open the log", bad);
        }
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.renderDistanceChunks = 16;
        mc.gameSettings.fovSetting = 70f;
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
        mc.gameSettings.showDebugInfo = false;
        mc.gameSettings.guiScale = 0;
        try {
            Display.setVSyncEnabled(false);
            if (!Display.isFullscreen()) {
                Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
                mc.resize(WIDTH, HEIGHT);
            }
        } catch (Exception bad) {
            say("could not resize the window: " + bad);
        }
        script();
        final String only = System.getProperty("bts.autorig.only", "")
            .trim();
        if (!only.isEmpty()) {
            // a partial re-shoot: keep only the steps whose label (spaces as _) starts with one of the prefixes
            steps.removeIf(st -> {
                for (String pre : only.split(",")) if (!pre.trim()
                    .isEmpty() && st.label.replace(' ', '_')
                        .startsWith(pre.trim()))
                    return false;
                return true;
            });
        }
        say(
            "autorig tour up: window " + mc.displayWidth
                + "x"
                + mc.displayHeight
                + ", "
                + steps.size()
                + " steps, lit target "
                + LIT_TARGET
                + " s");
    }

    private void waitMenu() {
        if (mc.currentScreen instanceof GuiMainMenu) {
            world = "bts-autorig-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            final WorldSettings settings = new WorldSettings(
                20261013L,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                WorldType.FLAT);
            settings.func_82750_a("2;7,2x3,2;1;");
            settings.enableCommands();
            say("creating the test world saves/" + world);
            go(Phase.WAIT_WORLD);
            mc.launchIntegratedServer(world, "BTS Autorig", settings);
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
                say("building both rigs ...");
                await(AutoRig::buildAll, lines -> {
                    if (lines instanceof List) for (Object l : (List<?>) lines) say("  " + l);
                    next();
                });
            });
        }
    }

    private void settle() {
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
            mc.thePlayer.capabilities.isFlying = true;
        }
        final int chunks = mc.theWorld == null ? -1
            : mc.theWorld.getChunkProvider()
                .getLoadedChunkCount();
        final int queued = renderQueue();
        final boolean here = mc.thePlayer != null && mc.theWorld != null
            && !mc.theWorld
                .getChunkFromBlockCoords((int) Math.floor(mc.thePlayer.posX), (int) Math.floor(mc.thePlayer.posZ))
                .isEmpty();
        if (here && Math.abs(chunks - lastChunks) <= 2 && queued <= 3) stillTicks++;
        else stillTicks = 0;
        lastChunks = chunks;
        if (phaseTicks >= settleMax) {
            say("  settle cap hit (" + settleMax + " ticks; " + chunks + " chunks, " + queued + " queued)");
        } else if (phaseTicks < settleMin || stillTicks < STILL) {
            return;
        }
        next();
    }

    private void finish() {
        if (phaseTicks == 1) {
            say("=== " + shots + " screenshots in " + dir.getAbsolutePath());
            await(() -> {
                AutoRig.closeLogs();
                return RenderTest.releaseAll();
            }, r -> {
                say(String.valueOf(r));
                go(Phase.FINISH);
                phaseTicks = 2;
            });
            return;
        }
        if (phaseTicks < 40) return;
        say("all done: closing the client");
        if (log != null) log.close();
        phase = Phase.DONE;
        skipExitConfirmation();
        mc.shutdown();
    }

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

    /** NEI's item panels off while a GUI is photographed (inventory.hidden), back on after; quiet if NEI is absent. */
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

    /** The tooltip of one item, wrapped to fit, over the world (no pause). */
    private static final class TipScreen extends GuiScreen {

        private final ItemStack stack;

        TipScreen(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void drawScreen(int mouseX, int mouseY, float partial) {
            drawRect(0, 0, width, height, 0x70000000);
            final List<String> raw = stack.getTooltip(mc.thePlayer, false);
            final List<String> lines = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                final String line = raw.get(i);
                final List<String> parts = fontRendererObj.listFormattedStringToWidth(line, width - 40);
                for (int k = 0; k < parts.size(); k++) lines.add(k == 0 ? parts.get(k) : "§7  " + parts.get(k));
            }
            drawHoveringText(lines, 8, 16, fontRendererObj);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
