package com.nateyburr.beyondthestars.machines.eye;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.star.CoreCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.EyeBursts;
import com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxy;
import com.nateyburr.beyondthestars.machines.eye.star.EyeMilestones;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.FirstCooling;
import com.nateyburr.beyondthestars.machines.eye.star.GalacticMerger;
import com.nateyburr.beyondthestars.machines.eye.star.MagnetarCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.ShellIgnition;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Grad;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * The Eye of Harmony's console on the controller GUI kit (controller-gui-spec.md §5.1). The header says the state
 * (a badge per §5.1.1: NOT FORMED, NO STAR, RUNNING / WAITING for a planet, TRANSITION, IGNITING, COOLING, IDLE,
 * PENDING, the lit zone, DARK), the star and the Eye's tier; the rows under it are the state's (§5.1.3): gauges, the
 * Stability / Integrity / Shield / Lock / Cycle bars, the dial and feed, milestones, warnings and the last event when
 * lit; a checklist of EVERY ignition or transition condition when it waits ({@link EyeStarRuntime#ignitionChecklist},
 * {@link EyeStarRuntime#transitionChecklist}); the phase and the bounce prediction during a transition; the batch on
 * T5. The <b>i</b> page has the questbook's words (§5.1.5). The gap row keeps the Dial and (T4 only) the Feed
 * steppers and the Field panel; the centre render switch is in the button column. Every row is decided on the
 * server and sent as one string ({@link BTSRows}); the two setpoints are written back through the runtime's clamping
 * setters ({@link EyeStarRuntime#setDial}, {@link EyeStarRuntime#setFeed}).
 * <p>
 * P4 (merged from master, B-115): the galaxy tiers' rows on T6-T8 (the bursts on the GRB Receptor Ports and beta
 * against the demand, the Stability / Heavy Link streak, the event countdowns, the planet slots or companions), the T6
 * / T7 milestones and transition conditions, and a Galaxy panel (mode, the T8 Ergomatter feed f8, Begin). B-115 also
 * puts each buffer against its intake cap (B-108) on the Field panel.
 */
public class EyeGui extends BTSControllerGui<MTEEyeOfHarmonyBTS> {

    /** QA-A16: the mode toggle's second click must come within this (the quasar's Collapse uses 4 s too). */
    private static final long MODE_ARM_MS = 4000L;
    private static final int FIELD_PANEL_W = 176;
    private static final int FIELD_PANEL_H = 196;
    private static final int SENSOR_ROWS = 8, TRIM_ROWS = 6;
    /** B-115: buffer rows on the Field panel (a T8 Eye in galaxy mode with companions can hold a dozen fluids). */
    private static final int INTAKE_ROWS = 14;
    private static final int GALAXY_PANEL_W = 176, GALAXY_PANEL_H = 196, GALAXY_TEXT_ROWS = 9;
    /**
     * B-119: the width of a row inside the Field / Galaxy panel's list. The list is FIELD_PANEL_W - 8 wide; once its
     * content outgrows it (the Field panel always does since B-115 added the Buffers section) MUI2 draws the scrollbar
     * over the list's right edge, so rows keep 6 px clear of it (as the Silo terminal does) and right-aligned values
     * are
     * no longer cut.
     */
    private static final int ROW_W = FIELD_PANEL_W - 8 - 6;
    private static final String RESULT = EyeStarRuntime.RESULT_KEY;

    public EyeGui(MTEEyeOfHarmonyBTS multiblock) {
        super(multiblock, true, "eyeRender");
    }

    private EyeStarRuntime rt() {
        return multiblock.starRuntime();
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager
            .syncValue("eyeDial", new DoubleSyncValue(() -> rt().dialSetpoint(), v -> rt().setDial(v)).allowC2S());
        syncManager
            .syncValue("eyeFeed", new DoubleSyncValue(() -> rt().feedSetpoint(), v -> rt().setFeed(v)).allowC2S());
        syncManager.syncValue("eyeDialPaid", new DoubleSyncValue(() -> round2(rt().dialPaid())));
        syncManager.syncValue("eyeDialMax", new DoubleSyncValue(() -> round2(rt().dialMax())));
        syncManager
            .syncValue("eyeFeedActual", new DoubleSyncValue(() -> Math.round(rt().feedActual() * 1000) / 1000.0));
        syncManager.syncValue("eyeFeedOn", new BooleanSyncValue(this::feedShown));
        syncManager.syncValue(
            "eyeRender",
            new BooleanSyncValue(multiblock::isRenderEnabled, multiblock::setRenderEnabled).allowC2S());
        syncManager.syncValue("eyeSockets", new StringSyncValue(this::socketLine));
        syncManager.syncValue("eyeSensors", new StringSyncValue(this::sensorLines));
        syncManager.syncValue("eyeTrims", new StringSyncValue(this::trimLines));
        // B-115: each buffer against its intake cap (B-108), for the Field panel
        syncManager.syncValue("eyeIntake", new StringSyncValue(this::intakeLines));
        // ---- P4: the Galaxy panel (T7 / T8: mode, the Ergomatter feed f8, Begin)
        syncManager.syncValue(
            "eyePlanets",
            new BooleanSyncValue(
                () -> rt().mode() == EyeGalaxy.Mode.PLANETS,
                v -> rt().setMode(v ? EyeGalaxy.Mode.PLANETS : EyeGalaxy.Mode.GALAXY)).allowC2S());
        // QA-A16: the mode toggle asks for a second click while switching would put a lit field out
        syncManager.syncValue("eyeModeLive", new BooleanSyncValue(() -> rt().modeSwitchPutsOut()));
        syncManager.syncValue("eyeF8", new DoubleSyncValue(() -> rt().f8Setpoint(), v -> rt().setF8(v)).allowC2S());
        syncManager.syncValue("eyeReady", new BooleanSyncValue(() -> rt().galaxyReady()));
        syncManager.syncValue("eyeBeginMsg", new StringSyncValue(() -> beginMsg));
        syncManager.registerServerSyncedAction("eyeBegin", packet -> beginMsg = rt().requestBegin());
        syncManager.syncValue("eyeGalaxyText", new StringSyncValue(this::galaxyPanelText));
    }

    /** The Bursts bar's full scale, Bu/t (the T8 demand D8 = 6: spec §5.1.3 "frac G/6"). */
    private static final double BURST_SCALE = 6;

    /** The last Begin click's answer (server side; synced to the Galaxy panel). */
    private String beginMsg = "";

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /** The Feed stepper shows only while the star burning, or the chart selected, is the T4 neutron star (Q9). */
    private boolean feedShown() {
        final EyeStarRuntime rt = rt();
        if (rt.live() && rt.star() != null
            && rt.star()
                .n() == 4)
            return true;
        final StarDef want = StarDef.byChart(multiblock.getControllerSlot());
        return want != null && want.n() == 4;
    }

    // =============================================================================== the header

    private StarDef want() {
        return StarDef.byChart(multiblock.getControllerSlot());
    }

    @Override
    protected Badge badgeCode() {
        if (!multiblock.isFormedForGui()) return Badge.NOT_FORMED;
        final EyeRecipe planet = multiblock.planetTarget();
        final StarDef want = want();
        final EyeStarRuntime rt = rt();
        if (planet != null) {
            return multiblock.getBaseMetaTileEntity()
                .isActive() ? Badge.RUNNING : Badge.WAITING;
        }
        if (want == null && !rt.live()) return Badge.NO_STAR;
        // P4 (spec §5.1.1 rows 5 and 7, B-116): the galaxy's quasar frozen or unloaded; the galaxy transition waiting
        // only for Begin
        if (galaxyPaused()) return Badge.PAUSED_RED;
        if (rt.transition() != null) return Badge.TRANSITION;
        if (rt.galaxyReady()) return Badge.READY;
        switch (rt.state()) {
            case IGNITING:
                return Badge.IGNITING;
            case BATCH:
                return Badge.COOLING;
            case IDLE:
                return Badge.IDLE_T5;
            case PENDING:
                return Badge.PENDING;
            case LIT:
                return rt.lastZone() < 0 ? Badge.STABLE : Badge.ofZone(rt.lastZone());
            default:
                return want == null ? Badge.NO_STAR : Badge.DARK;
        }
    }

    @Override
    protected String subject() {
        final Badge b = badgeCode();
        final EyeStarRuntime rt = rt();
        final StarDef star = rt.star();
        final StarDef want = want();
        switch (b) {
            case NOT_FORMED:
            case NO_STAR:
                return "";
            case RUNNING:
            case WAITING: {
                final EyeRecipe planet = multiblock.planetTarget();
                return planet == null ? "" : BTSText.lit(planet.name);
            }
            case TRANSITION:
                return BTSText.lit(rt.transition().type.title);
            case PENDING:
                return BTSText.lit(shortName(star) + " > " + shortName(want));
            case DARK:
                return want == null ? "" : BTSText.lit(shortName(want));
            default:
                return star != null ? BTSText.lit(shortName(star)) : want != null ? BTSText.lit(shortName(want)) : "";
        }
    }

    /**
     * A star's name for the header: without its parenthetical ("Neutron Star (Arcturus Remnant)" reads "Neutron
     * Star"), so the subject never runs into the right chip (dev client round 1). The full name stays in the rows.
     */
    private static String shortName(StarDef star) {
        if (star == null) return "?";
        final int cut = star.name.indexOf(" (");
        return cut > 0 ? star.name.substring(0, cut) : star.name;
    }

    @Override
    protected String rightChip() {
        final int u = rt().unlockedTier();
        if (!multiblock.isFormedForGui()) return BTSText.lang("beyondthestars.gui.eye.right.unlocked", u);
        final int inst = multiblock.installedTier();
        return BTSText.cat(BTSText.lit(inst < u ? "§e" : ""), BTSText.lang("beyondthestars.gui.eye.right", inst));
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        out.add(
            BTSText.lang(
                "beyondthestars.gui.eye.right.tip",
                multiblock.installedTier(),
                rt().unlockedTier(),
                multiblock.structureTier(),
                multiblock.compressionTier()));
        out.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang("beyondthestars.gui.eye.right.tip2")));
        return out;
    }

    /** P4: a burning T6-T8 star whose bursts are paused (Q-8: the quasar feeding its ports is frozen or unloaded). */
    private boolean galaxyPaused() {
        final EyeStarRuntime rt = rt();
        final StarDef star = rt.star();
        return rt.burning() && star != null
            && star.n() >= 6
            && rt.galaxy()
                .bursts().paused;
    }

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 28;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        // QA-A11: frozen while a footprint chunk is unloaded; S-05: switched off, and why
        if (multiblock.isFrozen()) {
            rows.note(BTSRows.ICON_BAD, BTSGuiKit.WARN, BTSText.lang("beyondthestars.gui.eye.frozen"));
        }
        final String off = multiblock.offReasonForGui();
        if (!off.isEmpty()) rows.note(BTSRows.ICON_BAD, BTSGuiKit.WARN, BTSText.lit(off));
        final Badge b = badgeCode();
        switch (b) {
            case NOT_FORMED:
                rows.note(
                    BTSRows.ICON_BAD,
                    BTSGuiKit.BAD,
                    BTSText.lang("beyondthestars.gui.eye.notformed", rt().unlockedTier()));
                rows.stat(BTSText.lang("beyondthestars.gui.eye.unlocked"), BTSText.lit("T" + rt().unlockedTier()));
                return;
            case NO_STAR:
                noStarRows(rows);
                return;
            case RUNNING:
            case WAITING:
                planetRows(rows);
                return;
            case TRANSITION:
                transitionRows(rows);
                return;
            case IGNITING:
                ignitingRows(rows);
                return;
            case COOLING:
                batchRows(rows, false);
                lastRow(rows);
                return;
            case IDLE_T5:
                idleRows(rows);
                return;
            case PENDING:
                pendingRows(rows);
                return;
            case READY:
                readyRows(rows);
                return;
            case DARK:
                darkRows(rows);
                return;
            default:
                litRows(rows);
        }
    }

    private void noStarRows(BTSRows.Builder rows) {
        final ItemStack target = multiblock.getControllerSlot();
        rows.section(BTSText.lang("beyondthestars.gui.eye.sec.run"), "");
        rows.check(
            BTSChecklist.State.BAD,
            BTSText.lang("beyondthestars.gui.eye.chk.chart"),
            BTSText.lang(target == null ? "beyondthestars.gui.eye.chk.empty" : "beyondthestars.gui.eye.chk.notachart"),
            -1,
            BTSText.lang(RESULT + "notarget"));
        rows.para(BTSText.lang("beyondthestars.gui.eye.nostar.hint"), BTSGuiKit.DIM);
    }

    private void planetRows(BTSRows.Builder rows) {
        final long p = multiblock.runningParallelForGui();
        rows.stat(BTSText.lang("beyondthestars.gui.kit.parallel"), BTSText.lit(p > 0 ? String.valueOf(p) : "-"));
        rows.checks(multiblock.planetChecklist());
        final String last = multiblock.lastRunForGui();
        if (!last.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", last));
        }
    }

    // ---------------------------------------------------------------------------- the gauges

    /** The active gauges: one row each, or pairs (index order) with four or more (spec §5.1.3 LIT 1). */
    private void gaugeRows(BTSRows.Builder rows, double tick) {
        final EyeStarRuntime rt = rt();
        final FieldReadout r = rt.lastReadout();
        final int[] active = rt.activeGauges();
        if (r == null || !r.lit || active.length == 0) return;
        final double sp = Double.isNaN(r.sp) ? Double.NaN : round1(r.sp);
        // two columns with four or more gauges, and on a galaxy (T7 / T8: spec §5.1, B-116)
        final StarDef star = rt.star();
        if (active.length >= 4 || active.length >= 2 && star != null && star.n() >= 7) {
            for (int i = 0; i < active.length; i += 2) {
                final int k1 = active[i];
                final int k2 = i + 1 < active.length ? active[i + 1] : -1;
                rows.gaugePair(
                    StellarField.GAUGE_SYM[k1],
                    round1(r.x(k1)),
                    k1 == StellarField.TH ? sp : Double.NaN,
                    k2 < 0 ? "" : StellarField.GAUGE_SYM[k2],
                    k2 < 0 ? 0 : round1(r.x(k2)),
                    k2 == StellarField.TH ? sp : Double.NaN,
                    gaugeTip(k1, r.x(k1), k1 == StellarField.TH ? sp : Double.NaN),
                    k2 < 0 ? "" : gaugeTip(k2, r.x(k2), k2 == StellarField.TH ? sp : Double.NaN));
            }
            return;
        }
        for (int k : active) {
            final double c = k == StellarField.TH ? sp : Double.NaN;
            rows.gauge(StellarField.GAUGE_SYM[k], round1(r.x(k)), c, tick, gaugeTip(k, r.x(k), c));
        }
    }

    private static String gaugeTip(int k, double v, double centre) {
        final double c = Double.isNaN(centre) ? 0 : centre;
        final int zone = BTSGuiKit.zoneOf(v - c);
        final String base = BTSText.lang(
            "beyondthestars.gui.eye.gauge.tip",
            BTSText.key("beyondthestars.gui.eye.gauge." + k),
            BTSFormat.signed(v),
            BTSText.key("beyondthestars.gui.zone." + zone));
        if (Double.isNaN(centre)) return base;
        return BTSText.cat(base, BTSText.lang("beyondthestars.gui.eye.gauge.centre", BTSFormat.signed(centre)));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // ------------------------------------------------------------------------------ the bars

    private void stabilityRow(BTSRows.Builder rows, FieldReadout r) {
        final int zone = Math.max(0, Math.min(4, r.zone));
        rows.bar(
            BTSText.lang("beyondthestars.gui.kit.stability"),
            r.stability / 100,
            BTSText.lit(BTSGuiKit.ZONE_TEXT[zone] + Math.round(r.stability)),
            BTSGuiKit.zoneGrad(zone),
            null,
            BTSText.lang(
                "beyondthestars.gui.eye.stability.tip",
                Math.round(r.stability),
                BTSText.key("beyondthestars.gui.zone." + zone)));
    }

    private void integrityRow(BTSRows.Builder rows, double integrity) {
        rows.bar(
            BTSText.lang("beyondthestars.gui.kit.integrity"),
            integrity / 100,
            BTSText.lit(BTSGuiKit.healthText(integrity) + Math.round(integrity)),
            BTSGuiKit.healthGrad(integrity),
            null,
            BTSText.lang("beyondthestars.gui.eye.integrity.tip"));
    }

    private void yParallelRow(BTSRows.Builder rows, FieldReadout r) {
        final int zone = Math.max(0, Math.min(4, r.zone));
        rows.stat2(
            BTSText.lit("Y"),
            BTSText.lit(BTSGuiKit.ZONE_TEXT[zone] + BTSFormat.f2(r.Y)),
            BTSText.lang("beyondthestars.gui.kit.parallel"),
            BTSText.lit(String.valueOf(rt().lastParallel())),
            BTSText.lang("beyondthestars.gui.eye.y.tip"));
    }

    private void lastRow(BTSRows.Builder rows) {
        final String last = rt().lastEvent();
        if (last != null && !last.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", last));
        }
    }

    /** The event name of a warning, as the scanner words it. */
    private String warnName(FieldReadout r) {
        final TierParams t = TierParams.of(Math.max(1, Math.min(8, r.tier)), r.planets);
        final EyeTransition tr = rt().transition();
        final String name = t.batch ? "crystallisation plateau"
            : tr instanceof ShellIgnition ? "forced helium flash" : t.event != null ? t.event.name : "event";
        final boolean signed = t.event != null && t.event.signed && !t.batch;
        return name + (signed ? " (" + (r.warn > 0 ? "+" : "-") + ")" : "");
    }

    private void warnRow(BTSRows.Builder rows, FieldReadout r) {
        if (r.warn == 0) return;
        rows.note(
            BTSRows.ICON_WARN,
            BTSGuiKit.WARN,
            BTSText.lang("beyondthestars.gui.eye.warn", warnName(r)),
            BTSText.lang("beyondthestars.gui.eye.warn.tip"));
    }

    // ------------------------------------------------------------------------------ LIT

    private void litRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        final FieldReadout r = rt.lastReadout();
        final StarDef star = rt.star();
        gaugeRows(rows, Double.NaN);
        if (r == null || !r.lit || star == null) {
            lastRow(rows);
            return;
        }
        rows.gap(2);
        stabilityRow(rows, r);
        integrityRow(rows, r.I);
        if (r.tier == 6) {
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.shield"),
                r.A / 100,
                BTSText.lit(BTSGuiKit.healthText(r.A) + Math.round(r.A)),
                BTSGuiKit.healthGrad(r.A),
                null,
                BTSText.lang("beyondthestars.gui.eye.shield.tip"));
        }
        final boolean held = r.lock >= StellarField.LOCK_SECONDS;
        rows.bar(
            BTSText.lang("beyondthestars.gui.eye.lock"),
            Math.min(1, r.lock / (double) StellarField.LOCK_SECONDS),
            BTSText.lit((held ? "§6" : "") + r.lock + "/" + StellarField.LOCK_SECONDS),
            Grad.LAVENDER,
            null,
            held ? BTSText.cat(BTSText.lit("§6"), BTSText.lang("beyondthestars.gui.eye.lock.held")) : "",
            BTSText.lang("beyondthestars.gui.eye.lock.tip"));
        final double m = Math.max(1e-9, rt.dialPaid());
        final double next = rt.nextCycleSeconds();
        final double upR = rt.upkeepR();
        final double euCycle = (double) star.euPerCycle * Math.max(1, rt.lastParallel());
        rows.bar(
            BTSText.lang("beyondthestars.gui.eye.cycle"),
            1 - next / (StellarField.CYCLE_SECONDS / m),
            BTSText.lit(upR < 1 ? "§cr " + BTSFormat.f2(upR) : Math.round(next) + " s"),
            upR < 1 ? Grad.RED : Grad.BLUE,
            null,
            BTSText
                .lang("beyondthestars.gui.eye.cycle.tip", BTSFormat.f2(upR), Math.round(next), BTSFormat.eu(euCycle)),
            upR < 1 ? BTSText.cat(BTSText.lit("§c"), BTSText.lang("beyondthestars.gui.eye.cycle.short")) : "");
        yParallelRow(rows, r);
        galaxyRows(rows, star);
        if (star.n() == 4) {
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.hsm"),
                BTSText.lit(String.format(Locale.ROOT, "%.3f / %.3f L/s", rt.feedActual(), rt.feedSetpoint())),
                BTSText.lang("beyondthestars.gui.eye.hsm.tip"));
        }
        dialRows(rows);
        // a galaxy's countdown row already names the event (B-116: the minor merger showed twice); kept when the
        // countdown rows name other events only (B-120: a merger warned during a satellite crossing)
        if (star.n() < 6 || !warnNamedByEvents()) warnRow(rows, r);
        final int u = rt.unlockedTier();
        final EyeMilestones ms = rt.milestones();
        if (u <= 7 && !ms.met(u)) {
            final ItemStack esc = rt.escrow();
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.milestones"),
                ms.fraction(u),
                BTSText.lang("beyondthestars.gui.kit.of", ms.done(u), ms.parts(u)),
                Grad.MAGENTA,
                null,
                BTSText.lit(ms.progressLine(u)),
                BTSText.lang(
                    "beyondthestars.gui.eye.escrow",
                    esc == null ? BTSText.key("beyondthestars.gui.eye.escrow.empty") : esc.getDisplayName()));
        }
        renderAndOwedRows(rows);
        lastRow(rows);
    }

    /**
     * The dial runs short of its setpoint: either the Field sockets could not pay the Tachyon Rich Temporal Fluid
     * (spec §5.1.3 LIT 10), or the Time Dilation generators cap it below the setpoint (B-96: told apart, so a capped
     * dial is not reported as a fluid shortage).
     */
    private void dialRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        if (rt.transition() != null) return;
        final double set = rt.dialSetpoint(), paid = rt.dialPaid(), cap = rt.dialMax();
        final double want = Math.min(set, cap);
        if (paid < want - 0.01) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang("beyondthestars.gui.eye.dialshort", BTSFormat.f2(paid), BTSFormat.f2(set)),
                BTSText.lang("beyondthestars.gui.eye.dialshort.tip"));
        } else if (set > cap + 0.01) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang("beyondthestars.gui.eye.dialcap", BTSFormat.f2(cap), BTSFormat.f2(set)),
                BTSText.lang("beyondthestars.gui.eye.dialcap.tip"));
        }
    }

    private void renderAndOwedRows(BTSRows.Builder rows) {
        final String note = multiblock.getRenderNote();
        if (note != null && !note.isEmpty()) rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lit(note));
        if (rt().hasOwed()) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang("beyondthestars.gui.eye.owed", rt().owedLine()),
                BTSText.lang("beyondthestars.gui.eye.owed.tip"));
        }
    }

    // ----------------------------------------------------------------------- P4: T6-T8

    /**
     * P4 (T6-T8): the bursts on the GRB Receptor Ports with beta against the demand, the link milestone's streak (T6
     * Stability Link, T7 Heavy Link), the running countdown, and the planet slots or companions. The slot sentences
     * are the runtime's (shared with the scanner, spec Q6).
     */
    private void galaxyRows(BTSRows.Builder rows, StarDef star) {
        final int n = star.n();
        if (n < 6) return;
        final EyeStarRuntime rt = rt();
        final EyeBursts b = rt.galaxy()
            .bursts();
        if (b.paused) {
            rows.note(
                BTSRows.ICON_BAD,
                BTSGuiKit.BAD,
                BTSText.lit(b.line()),
                BTSText.lang("beyondthestars.gui.eye.bursts.paused.tip"));
        } else if (b.ports() == 0) {
            if (n == 6) {
                rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.eye.bursts.noports6"));
            } else {
                rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang("beyondthestars.gui.eye.bursts.noports"));
            }
        } else {
            final boolean driven = star.tier.burstDriven() || rt.galaxyTransition();
            final double beta = rt.galaxy()
                .beta();
            final double demand = rt.galaxy()
                .demand();
            // spec §5.1.3 P4 (B-116): a bar over 0-6 Bu/t; the yellow tick is the link milestone's floor while it is to
            // come (T6 Stability Link 1.5, T7 Heavy Link 6), else a burst-driven galaxy's demand; the value's colour is
            // beta (green 1 and up, yellow 0.8 and up, red under)
            final boolean linkToCome = (n == 6 || n == 7) && !star.tier.planets() && !rt.milestones().linkDone[n];
            final double tickAt = linkToCome ? (n == 6 ? GalaxyField.D7 : GalaxyField.demandT(8))
                : driven ? demand : Double.NaN;
            // P5: the bar's full scale follows the tier from T9 (D9 24, D10 96 Bu/t: the bar runs to 2 x the demand)
            final double scale = n >= 9 ? Math.max(BURST_SCALE, 2 * demand) : BURST_SCALE;
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.bursts"),
                b.total() / scale,
                BTSText.lit(
                    (driven ? beta >= 1 ? "§a" : beta >= 0.8 ? "§e" : "§c" : "") + BTSFormat.f2(b.total()) + " Bu/t"),
                Grad.MAGENTA,
                Double.isNaN(tickAt) ? null : BTSGuiKit.tick(tickAt / scale, BTSGuiKit.TICK),
                driven ? BTSText.lang("beyondthestars.gui.eye.bursts.beta", BTSFormat.f2(beta)) : "",
                BTSText.lang(
                    "beyondthestars.gui.eye.bursts.tip",
                    BTSFormat.f2(b.hi),
                    BTSFormat.f2(b.lo),
                    BTSFormat.f2(b.halo)),
                BTSText.lang("beyondthestars.gui.eye.bursts.total", BTSFormat.buEu(b.total())),
                driven ? BTSText.lang("beyondthestars.gui.eye.bursts.demand", BTSFormat.buEuShort(demand)) : "",
                linkToCome ? BTSText.cat(
                    BTSText.lit("§e"),
                    BTSText.lang(
                        "beyondthestars.gui.eye.bursts.floor",
                        EyeMilestones.LINK_NAME[n],
                        BTSFormat.buEuShort(tickAt)))
                    : "",
                b.source.isEmpty() ? BTSText.lang("beyondthestars.gui.eye.bursts.notlinked")
                    : BTSText.lang("beyondthestars.gui.eye.bursts.linked", b.source));
            // §10.3: the bursts' EU/t right under the bar
            rows.stat(
                BTSText.lang("beyondthestars.gui.kit.eut"),
                BTSText.lit(BTSFormat.euValue(b.total())),
                BTSText.lang("beyondthestars.gui.kit.eut.tip"));
        }
        if ((n == 6 || n == 7) && !star.tier.planets()) {
            final String name = EyeMilestones.LINK_NAME[n];
            if (rt.milestones().linkDone[n]) {
                rows.bar(
                    BTSText.lang("beyondthestars.gui.eye.link"),
                    1,
                    BTSText.cat(BTSText.lit("§a"), BTSText.lang("beyondthestars.gui.badge.held")),
                    Grad.LAVENDER,
                    null,
                    BTSText.cat(BTSText.lit("§a" + name + " "), BTSText.lang("beyondthestars.gui.eye.link.held")),
                    BTSText.lang("beyondthestars.gui.eye.link.tip"));
            } else {
                final BurstLink l = rt.activeLink();
                if (l != null) {
                    rows.bar(
                        BTSText.lang("beyondthestars.gui.eye.link"),
                        l.streak / (double) EyeMilestones.LINK_NEED,
                        BTSText.lit(EyeMilestones.clock(l.streak)),
                        Grad.LAVENDER,
                        null,
                        BTSText.lang(
                            "beyondthestars.gui.eye.link.streak",
                            name,
                            EyeMilestones.clock(l.streak),
                            EyeMilestones.clock(EyeMilestones.LINK_NEED)),
                        BTSText.lang(
                            "beyondthestars.gui.eye.link.bar.tip",
                            name,
                            EyeMilestones.clock(Math.max(l.best, rt.milestones().linkBest[n])),
                            BTSFormat.buEuShort(l.mean),
                            BTSText
                                .key(n == 6 ? "beyondthestars.gui.eye.link.high" : "beyondthestars.gui.eye.link.halo"),
                            BTSFormat.pct(l.frac)),
                        BTSText.lang("beyondthestars.gui.eye.link.tip"));
                }
            }
        }
        supermassiveRow(rows, star);
        p5Rows(rows, star);
        for (String ev : eventLines()) {
            rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, ev, BTSText.lang("beyondthestars.gui.eye.warn.tip"));
        }
        slotRows(rows, star);
    }

    /**
     * P5 (p5-t9-t10.md §2, §3, §6): the Deep Link at T8 (while it is to come), the pellet beam, the stock, the Galactic
     * Flow and the galaxies held at T9, the universe at T10. GT-style short values; the numbers in the tooltips.
     */
    private void p5Rows(BTSRows.Builder rows, StarDef star) {
        final EyeStarRuntime rt = rt();
        final com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor a = rt.attractor();
        final com.nateyburr.beyondthestars.machines.eye.field.FieldState fs = rt.fieldState();
        final int n = fs == null ? star.n() : fs.tier.n;
        if (n == 8 && !star.tier.planets() && !rt.milestones().linkDone[8]) {
            final com.nateyburr.beyondthestars.machines.quasar.engine.DeepLink d = a.deepLink();
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.p5.deeplink"),
                d.link.streak / (double) EyeMilestones.LINK_NEED,
                BTSText.lit(EyeMilestones.clock(d.link.streak)),
                Grad.LAVENDER,
                null,
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.deeplink.tip0",
                    BTSFormat.buEuShort(d.link.mean),
                    BTSFormat.pct(d.link.frac),
                    BTSFormat.f2(d.rho)),
                BTSText.lang("beyondthestars.gui.eye.p5.deeplink.tip1"));
        }
        if (fs != null && (fs.tier.attractor
            || rt.transition() instanceof com.nateyburr.beyondthestars.machines.eye.star.Infall)) {
            final double rho = fs.beam == null ? 0 : fs.beam.rho;
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.p5.beam"),
                a.rhoMax() > 0 ? rho / a.rhoMax() : 0,
                BTSText.lit(
                    (rho >= 0.9 ? "§a" : rho >= 0.5 ? "§e" : "§c") + "ρ "
                        + BTSFormat.f2(rho)
                        + " · "
                        + com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.gradeName(a.grade())),
                Grad.MAGENTA,
                a.rhoMax() > 0 ? BTSGuiKit.tick(1 / a.rhoMax(), BTSGuiKit.TICK) : null,
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.beam.tip0",
                    com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.gevText(a.grade()),
                    BTSFormat.f2(a.rate()),
                    BTSFormat.f2(a.rhoMax())),
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.beam.tip1",
                    String.valueOf(a.holdRating()),
                    String.valueOf(a.acceptLimit())),
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.beam.tip2",
                    a.level() < 0 ? BTSText.key(
                        a.unwired() ? "beyondthestars.gui.eye.p5.beam.unwiredlevel"
                            : "beyondthestars.gui.eye.p5.beam.nolevel")
                        : String.valueOf(a.level())),
                BTSText.lang("beyondthestars.gui.eye.p5.beam.tip3"));
            // B-222 / B-221: the beam idle while V climbs, or a Burst Control Hatch nobody wired
            if (a.beamFault()) {
                rows.note(
                    BTSRows.ICON_WARN,
                    BTSGuiKit.WARN,
                    BTSText.lang("beyondthestars.gui.eye.p5.beam.fault", a.idleReason()),
                    BTSText.lang("beyondthestars.gui.eye.p5.beam.fault.tip"));
            } else if (a.unwired()) {
                rows.note(
                    BTSRows.ICON_WARN,
                    BTSGuiKit.WARN,
                    BTSText.lang("beyondthestars.gui.eye.p5.beam.unwired"),
                    BTSText.lang("beyondthestars.gui.eye.p5.beam.unwired.tip"));
            }
            final double secs = a.stockSeconds();
            rows.stat2(
                BTSText.lang("beyondthestars.gui.eye.p5.pellets"),
                BTSText.lit(
                    (a.pelletsLow() ? "§e" : "") + BTSFormat.f2(a.stock())
                        .replace(".00", "") + (Double.isInfinite(secs) ? "" : " (" + Math.round(secs) + " s)")),
                BTSText.lang("beyondthestars.gui.eye.p5.galaxies"),
                BTSText.lit(String.valueOf(rt.milestones().galaxies)),
                BTSText.lang("beyondthestars.gui.eye.p5.pellets.tip"),
                BTSText.lang("beyondthestars.gui.eye.p5.galaxies.tip"));
            final com.nateyburr.beyondthestars.machines.eye.field.FieldState.Flow f = fs.flow;
            final String flow = f == null ? BTSText.lang("beyondthestars.gui.eye.p5.flow.none")
                : f.left > 0
                    ? BTSText.lang(
                        "beyondthestars.gui.eye.p5.flow.surge",
                        String.valueOf(f.size),
                        String.valueOf(com.nateyburr.beyondthestars.machines.eye.field.AttractorField.FLOW_DUR - f.tau),
                        String.valueOf(f.ok))
                    : f.warned ? BTSText.lang(
                        f.accepted ? "beyondthestars.gui.eye.p5.flow.warned"
                            : "beyondthestars.gui.eye.p5.flow.declined",
                        String.valueOf(f.size),
                        BTSText.key(
                            f.sign > 0 ? "beyondthestars.gui.eye.p5.side.high" : "beyondthestars.gui.eye.p5.side.low"),
                        String.valueOf(Math.round(f.next - fs.t)))
                        : BTSText
                            .lang("beyondthestars.gui.eye.p5.flow.next", String.valueOf(Math.round(f.next - fs.t)));
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.p5.flow"),
                flow,
                BTSText.lang("beyondthestars.gui.eye.p5.flow.tip0"),
                a.lastFlow()
                    .isEmpty() ? "" : BTSText.lang("beyondthestars.gui.eye.p5.flow.last", a.lastFlow()));
        }
        // B-224: a lit T10 between universes says what the next one waits for (bursts on the crown, its charge)
        if (fs != null && fs.tier.universe
            && rt.burning()
            && !rt.waitReason()
                .isEmpty()) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang("beyondthestars.gui.eye.p5.universe.waiting", rt.waitReason()),
                BTSText.lang("beyondthestars.gui.eye.p5.universe.waiting.tip"));
        }
        if (fs != null && fs.tier.universe
            && fs.uni != null
            && !(rt.transition() instanceof com.nateyburr.beyondthestars.machines.eye.star.Inflation)) {
            final com.nateyburr.beyondthestars.machines.eye.field.FieldState.Universe u = fs.uni;
            rows.bar(
                BTSText.lang("beyondthestars.gui.eye.p5.universe"),
                u.t / (double) com.nateyburr.beyondthestars.machines.eye.field.UniverseField.BATCH,
                BTSText.lit(EyeMilestones.clock(u.t)),
                Grad.LAVENDER,
                BTSGuiKit.tick(
                    u.dawn / (double) com.nateyburr.beyondthestars.machines.eye.field.UniverseField.BATCH,
                    BTSGuiKit.TICK),
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.universe.tip0",
                    BTSText.key(
                        "beyondthestars.gui.eye.p5.phase."
                            + com.nateyburr.beyondthestars.machines.eye.field.UniverseField.phase(u))),
                BTSText.lang(
                    "beyondthestars.gui.eye.p5.universe.tip1",
                    EyeMilestones.clock(com.nateyburr.beyondthestars.machines.eye.field.UniverseField.REC_AT),
                    EyeMilestones.clock(u.dawn),
                    BTSText
                        .key(u.dawnOk ? "beyondthestars.gui.eye.p5.dawn.ok" : "beyondthestars.gui.eye.p5.dawn.lost")),
                BTSText.lang("beyondthestars.gui.eye.p5.universe.tip2", BTSFormat.f2(u.n > 0 ? u.ySum / u.n : 0)),
                a.chargeOwed() > 0
                    ? BTSText.lang("beyondthestars.gui.eye.p5.universe.owed", String.valueOf(a.chargeOwed()))
                    : "");
            rows.stat2(
                BTSText.lang("beyondthestars.gui.eye.p5.universes"),
                BTSText.lit(String.valueOf(rt.milestones().universes)),
                BTSText.lang("beyondthestars.gui.eye.p5.dawns"),
                BTSText.lit(String.valueOf(rt.milestones().dawns)),
                BTSText.lang("beyondthestars.gui.eye.p5.universes.tip"));
        }
    }

    /**
     * SMBH (smbh-v1.md §1.3): the Supermassive Link, P5's Eye T9 -> T10 machine milestone, as a read-only checklist
     * row marked P5 (yellow until held): the streak against 30 min, with the 60 s mean against 96 Bu/t and the link
     * source's stream record in the tooltip. Shown from T8, or while a qualifying hole is linked, or once a streak
     * was seen.
     */
    private void supermassiveRow(BTSRows.Builder rows, StarDef star) {
        final EyeStarRuntime rt = rt();
        final EyeBursts b = rt.galaxy()
            .bursts();
        final EyeMilestones ms = rt.milestones();
        if (star.n() < 8 && !(b.linkedAll && b.supermassiveSource()) && ms.smLinkBest <= 0) return;
        final BurstLink l = rt.galaxy()
            .supermassiveLink();
        final boolean held = ms.smLinkDone;
        rows.check(
            held ? BTSChecklist.State.OK : BTSChecklist.State.WARN,
            BTSText.lang("beyondthestars.gui.eye.chk.smlink"),
            held ? BTSText.lang("beyondthestars.gui.badge.held")
                : BTSText.lit(EyeMilestones.clock(l.streak) + " / " + EyeMilestones.clock(EyeMilestones.LINK_NEED)),
            held ? 1 : BTSChecklist.frac(l.streak, EyeMilestones.LINK_NEED),
            BTSText.lang("beyondthestars.gui.eye.chk.smlink.tip0"),
            BTSText.lang(
                "beyondthestars.gui.eye.chk.smlink.tip1",
                BTSFormat.buEuShort(l.mean),
                BTSFormat.buEuShort(BurstLink.supermassiveMean())),
            BTSText.lang("beyondthestars.gui.eye.chk.smlink.tip2", EyeStarRuntime.supermassiveSourceText(b)),
            BTSText.lang("beyondthestars.gui.eye.chk.smlink.tip3", EyeMilestones.clock(ms.smLinkBest)),
            BTSText.lang("beyondthestars.gui.eye.chk.smlink.tip4"));
    }

    /**
     * The running countdowns, one line each: the collapse or a pericentre pass, the satellite crossing, a minor merger
     * (B-120: a merger can be warned while a crossing counts down or runs, and both are shown).
     */
    private List<String> eventLines() {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = rt();
        if (!rt.burning()) return out;
        final int c = rt.countdownSeconds();
        final EyeTransition tr = rt.transition();
        if (tr instanceof MagnetarCollapse && c >= 0) out.add(BTSText.lang("beyondthestars.gui.eye.ev.collapse", c));
        if (tr instanceof GalacticMerger && c >= 0) {
            out.add(
                BTSText
                    .lang("beyondthestars.gui.eye.ev.pass", c, ((GalacticMerger) tr).nextPassSign() < 0 ? "-" : "+"));
        }
        if (rt.crossingCountdown() >= 0) {
            out.add(BTSText.lang("beyondthestars.gui.eye.ev.crossing", rt.crossingCountdown()));
        } else if (rt.crossingLeft() > 0) {
            out.add(BTSText.lang("beyondthestars.gui.eye.ev.crossingleft", rt.crossingLeft()));
        }
        if (rt.mergerWarning() != 0) {
            out.add(
                BTSText.lang(
                    "beyondthestars.gui.eye.ev.merger",
                    rt.mergerCountdown(),
                    rt.mergerWarning() < 0 ? "-" : "+"));
        }
        return out;
    }

    /**
     * True when a countdown row names the event the readout's warning is for: the collapse or pass countdown (the
     * transition's warning wins the readout) or the minor merger. A crossing alone never sets the warning.
     */
    private boolean warnNamedByEvents() {
        final EyeStarRuntime rt = rt();
        if (!rt.burning()) return false;
        final EyeTransition tr = rt.transition();
        if ((tr instanceof MagnetarCollapse || tr instanceof GalacticMerger) && rt.countdownSeconds() >= 0) return true;
        return rt.mergerWarning() != 0;
    }

    /** Planets mode's slots (one row, each slot in the tooltip), or the companions (T7+). */
    private void slotRows(BTSRows.Builder rows, StarDef star) {
        final EyeStarRuntime rt = rt();
        if (star.n() < 7) return;
        if (star.tier.planets()) {
            final List<String> lines = rt.slotLines();
            int running = 0;
            for (String l : lines) if (l.contains("%")) running++;
            final List<String> tip = new ArrayList<>();
            for (String l : lines) tip.add(BTSText.lit(l));
            if (rt.galaxy()
                .lastRunLost()) tip.add(BTSText.lang("beyondthestars.gui.eye.slots.lost"));
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.slots"),
                BTSText.lit(running + "/" + lines.size() + "  " + BTSFormat.pct(rt.slotProgress())),
                tip.toArray(new String[0]));
            return;
        }
        rows.note(
            BTSRows.ICON_NONE,
            BTSGuiKit.DIM,
            BTSText.lang("beyondthestars.gui.eye.companions", rt.companionNames()),
            BTSText.lang("beyondthestars.gui.eye.companions.tip"));
    }

    // --------------------------------------------------------------------------- IGNITING

    private void ignitingRows(BTSRows.Builder rows) {
        final double p = round1(rt().ignitionP());
        rows.gauge(
            "P",
            p,
            Double.NaN,
            StellarField.IGNITION_LIT,
            BTSText.lang("beyondthestars.gui.eye.ignition.tip", BTSFormat.signed(p)));
        final double frac = (p - StellarField.IGNITION_START)
            / (StellarField.IGNITION_LIT - StellarField.IGNITION_START);
        rows.bar(
            BTSText.lang("beyondthestars.gui.eye.ignition"),
            frac,
            BTSText.lit(BTSFormat.pct(frac)),
            Grad.GOLD,
            null);
        rows.stat(
            BTSText.lang("beyondthestars.gui.eye.lightsat"),
            BTSText.lang(
                "beyondthestars.gui.eye.lightsin",
                Math.max(0, Math.round((StellarField.IGNITION_LIT - p) / StellarField.IGNITION_RATE))));
        rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.eye.nooutput"));
        lastRow(rows);
    }

    // ---------------------------------------------------------------------------- PENDING

    private void pendingRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        gaugeRows(rows, Double.NaN);
        final int from = rt.star() == null ? rt.unlockedTier()
            : rt.star()
                .n();
        final BTSChecklist c = rt.transitionChecklist(from);
        rows.section(BTSText.lit(EyeStarRuntime.eventFor(from)), c.countText());
        rows.checks(c);
        lastRow(rows);
    }

    /**
     * READY (P4, B-116): a lit magnetar or Triangulum with every condition of its galaxy transition met; Begin starts
     * it. The gauges, the bursts and the link, the countdown, then the Begin line (the conditions in its tooltip).
     */
    private void readyRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        final StarDef star = rt.star();
        gaugeRows(rows, Double.NaN);
        if (star != null) galaxyRows(rows, star);
        final int from = star == null ? rt.unlockedTier() : star.n();
        final EyeTransition.Type type = EyeTransition.Type.leaving(from);
        final BTSChecklist c = rt.transitionChecklist(from);
        final List<String> tip = new ArrayList<>();
        tip.add(BTSText.lang("beyondthestars.gui.eye.begin.tip"));
        for (BTSChecklist.CheckLine l : c.lines()) {
            tip.add(BTSText.cat(BTSText.lit(l.met() ? "§a+ §7" : "§c- §7"), l.label, BTSText.lit(": "), l.value));
        }
        weakQuasarRow(rows);
        rows.note(
            BTSRows.ICON_OK,
            BTSGuiKit.NOTE_BLUE,
            BTSText.lang("beyondthestars.gui.eye.begin", type == null ? "?" : type.title),
            tip.toArray(new String[0]));
        lastRow(rows);
    }

    /**
     * B-118 "Warn, allow" (2026-10-13): a red row above Begin on a magnetar whose linked quasar cannot cancel the
     * collapse shock even with a purge at full flux (its peak under the countdown line's number). Begin stays enabled.
     */
    private void weakQuasarRow(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        if (!rt.collapseQuasarWeak()) return;
        final double peak = rt.linkedQuasarPeak(), need = rt.collapseFullCancelBu();
        rows.check(
            BTSChecklist.State.BAD,
            BTSText.lang("beyondthestars.gui.eye.chk.weakquasar"),
            BTSText.lit(
                BTSFormat.euSci(peak * BTSFormat.EU_PER_BU) + " / "
                    + BTSFormat.euSci(need * BTSFormat.EU_PER_BU)
                    + " EU/t"),
            BTSChecklist.frac(peak, need),
            BTSText.lang("beyondthestars.gui.eye.chk.weakquasar.tip0", BTSFormat.buEu(peak), BTSFormat.buEu(need)),
            BTSText.lang("beyondthestars.gui.eye.chk.weakquasar.tip1", BTSFormat.buEuShort(need)),
            BTSText.lang(
                "beyondthestars.gui.eye.chk.weakquasar.tip2",
                Math.round(rt.collapseShockAtPeak()),
                Math.round(rt.collapseShock() > 0 ? rt.collapseShock() : MagnetarCollapseOverlay.DEFAULT_SHOCK)),
            BTSText.lang("beyondthestars.gui.eye.chk.weakquasar.tip3"));
    }

    // ------------------------------------------------------------------------- TRANSITION

    private void transitionRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        final EyeTransition tr = rt.transition();
        final double progress = Math.max(0, rt.transitionProgress());
        rows.bar(
            BTSText.lang("beyondthestars.gui.eye.phase"),
            progress,
            BTSText.lit(tr.phase() + "/" + tr.phases() + "  " + BTSFormat.pct(progress)),
            Grad.MAGENTA,
            null,
            BTSText.lit(rt.transitionLine()),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang("beyondthestars.gui.eye.phase.tip")));
        if (tr instanceof FirstCooling) {
            batchRows(rows, true);
            lastRow(rows);
            return;
        }
        final FieldReadout r = rt.lastReadout();
        if (tr instanceof CoreCollapse) {
            final CoreCollapse cc = (CoreCollapse) tr;
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.coremass"),
                BTSText.lit(BTSFormat.f2(cc.co.Mc) + " / " + BTSFormat.f2(CollapseOverlay.MC_BOUNCE)));
        }
        gaugeRows(rows, Double.NaN);
        if (tr instanceof CoreCollapse && ((CoreCollapse) tr).co.phase == 1) bounceRows(rows);
        if (r != null && r.lit) {
            stabilityRow(rows, r);
            integrityRow(rows, r.I);
            yParallelRow(rows, r);
        }
        lastRow(rows);
    }

    /** Core Collapse, fast-forward: the bounce's prediction as checks (today only in the scanner). */
    private void bounceRows(BTSRows.Builder rows) {
        final double[] pred = rt().collapsePrediction();
        if (pred == null) return;
        final double st = pred[0], pAfter = pred[1], thAfter = pred[2];
        final boolean seed = pred[3] > 0;
        final double full = CollapseOverlay.MAX_ABSORB_POINTS * CollapseOverlay.SPACETIME_PER_POINT;
        rows.section(BTSText.lang("beyondthestars.gui.eye.bounce"), "");
        rows.check(
            st >= full ? BTSChecklist.State.OK : BTSChecklist.State.WARN,
            BTSText.lang("beyondthestars.gui.eye.chk.stbuf"),
            BTSText.lit(BTSFormat.litres(st) + " L"),
            BTSChecklist.frac(st, full),
            BTSText.lang(
                "beyondthestars.gui.eye.chk.stbuf.tip",
                BTSFormat.exact((long) st),
                BTSFormat.exact((long) full)));
        final boolean safe = pAfter <= 100 && thAfter <= 100;
        rows.check(
            safe ? BTSChecklist.State.OK : BTSChecklist.State.BAD,
            BTSText.lang("beyondthestars.gui.eye.chk.after"),
            safe ? BTSText.lit(BTSFormat.signed(pAfter) + " / " + BTSFormat.signed(thAfter))
                : BTSText.lang("beyondthestars.gui.eye.supernova"),
            -1,
            BTSText.lang("beyondthestars.gui.eye.chk.after.tip", BTSFormat.signed(pAfter), BTSFormat.signed(thAfter)));
        rows.check(
            seed ? BTSChecklist.State.OK : BTSChecklist.State.BAD,
            BTSText.lang("beyondthestars.gui.eye.chk.hsmseed"),
            BTSText.lit(BTSFormat.exact((long) CollapseOverlay.HSM_SEED) + " L"),
            -1,
            BTSText.lang("beyondthestars.gui.eye.chk.hsmseed.tip"));
    }

    // ------------------------------------------------------------------------- COOLING / IDLE

    private void batchRows(BTSRows.Builder rows, boolean firstCooling) {
        final EyeStarRuntime rt = rt();
        final FieldReadout r = rt.lastReadout();
        final double eyeT = rt.batchEyeSeconds();
        final double frac = eyeT / StellarField.BATCH_SECONDS;
        rows.bar(
            BTSText.lang("beyondthestars.gui.eye.batch"),
            frac,
            BTSText.lit(BTSFormat.pct(frac)),
            Grad.CYAN,
            null,
            BTSText.lang("beyondthestars.gui.eye.batch.tip", BTSFormat.exact(Math.round(eyeT))));
        final double sp = rt.batchSetpoint();
        if (!Double.isNaN(sp)) {
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.setpoint"),
                Math.abs(sp) <= BTSGuiKit.ZONE_EDGE[0]
                    ? BTSText
                        .cat(BTSText.lit(BTSFormat.signed(sp) + "  §e"), BTSText.lang("beyondthestars.gui.eye.plateau"))
                    : BTSText.lit(BTSFormat.signed(sp)),
                BTSText.lang("beyondthestars.gui.eye.setpoint.tip"));
        }
        if (r != null && r.lit) {
            final double c = Double.isNaN(sp) ? Double.NaN : round1(sp);
            rows.gauge(
                StellarField.GAUGE_SYM[StellarField.TH],
                round1(r.x(StellarField.TH)),
                c,
                Double.NaN,
                gaugeTip(StellarField.TH, r.x(StellarField.TH), c));
            rows.gauge(
                StellarField.GAUGE_SYM[StellarField.P],
                round1(r.x(StellarField.P)),
                Double.NaN,
                Double.NaN,
                gaugeTip(StellarField.P, r.x(StellarField.P), Double.NaN));
            integrityRow(rows, r.I);
        }
        final double meanY = rt.batchMeanY();
        final String mean = Double.isNaN(meanY) ? "-" : BTSFormat.f2(meanY);
        if (firstCooling) {
            final boolean good = !Double.isNaN(meanY) && meanY >= FirstCooling.MEAN_Y;
            rows.stat2(
                BTSText.lang("beyondthestars.gui.eye.meany"),
                BTSText.lit((good ? "§a" : "§c") + mean),
                BTSText.lang("beyondthestars.gui.eye.need"),
                BTSText.lit(BTSFormat.f2(FirstCooling.MEAN_Y)),
                BTSText.lang("beyondthestars.gui.eye.meany.tip"));
        } else {
            rows.stat(BTSText.lang("beyondthestars.gui.eye.meany"), BTSText.lit(mean));
        }
        if (r != null && r.lit) warnRow(rows, r);
    }

    private void idleRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        if (rt.unlockedTier() >= 5) {
            nextBatchRows(rows);
        } else {
            final BTSChecklist c = rt.transitionChecklist(4);
            rows.section(BTSText.lit(EyeStarRuntime.eventFor(4)), c.countText());
            rows.checks(c);
        }
        lastRow(rows);
    }

    /** The T5 batch conditions, from the runtime's own test ({@link EyeStarRuntime#batchChecklist}). */
    private void nextBatchRows(BTSRows.Builder rows) {
        rows.section(BTSText.lang("beyondthestars.gui.eye.nextbatch"), "");
        rows.checks(rt().batchChecklist());
    }

    // ------------------------------------------------------------------------------- DARK

    private void darkRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        final String lost = rt.lastFailure();
        if (lost != null && !lost.isEmpty()) {
            rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang("beyondthestars.gui.kit.lost", lost));
        }
        final StarDef want = want();
        final int n = want == null ? 0 : want.n();
        // D-54: a lost T4, T7-T9 or T10 relights at the base of its band from a new charge
        final StarDef base = rt.baseRelightFor(want);
        if (base != null) {
            rows.section(BTSText.lang("beyondthestars.gui.eye.sec.base", base.name, base.n()), "");
            rows.checks(rt.baseRelightChecklist(base));
            rows.para(
                BTSText.lang(
                    base == want ? "beyondthestars.gui.eye.base.same" : "beyondthestars.gui.eye.base.climb",
                    want.name),
                BTSGuiKit.DIM);
            lastRow(rows);
            return;
        }
        final StarDef lights = n >= 1 && n <= 4 ? rt.ignitableFor(want) : null;
        if (lights == want && n >= 1 && n <= 3) {
            rows.section(BTSText.lang("beyondthestars.gui.eye.sec.light", want.name), "");
            rows.checks(rt.ignitionChecklist(want));
        } else if (n >= 1 && n <= 4) {
            // QA-A02: the chart one tier above the unlocked one (and T4's Neutron Star chart) relights the star below
            final StarDef progenitor = lights;
            if (progenitor != null) {
                rows.section(BTSText.lang("beyondthestars.gui.eye.sec.relight", progenitor.name), "");
                rows.checks(rt.ignitionChecklist(progenitor));
            } else {
                final StarDef arcturus = StarDef.ofTier(3);
                rows.section(BTSText.lang("beyondthestars.gui.eye.sec.remnant"), "");
                rows.check(
                    BTSChecklist.State.BAD,
                    BTSText.lang("beyondthestars.gui.eye.chk.progenitor", arcturus == null ? "T3" : arcturus.name),
                    BTSText.lang("beyondthestars.gui.eye.dark"),
                    -1);
                rows.para(BTSText.lang(RESULT + "star_remnant"), BTSGuiKit.DIM);
            }
        } else if (n == 5) {
            if (rt.unlockedTier() >= 5) {
                nextBatchRows(rows);
            } else {
                final BTSChecklist c = rt.transitionChecklist(4);
                rows.section(BTSText.lit(EyeStarRuntime.eventFor(4)), c.countText());
                rows.checks(c);
            }
        } else if (n >= 6) {
            // T6: the Magnetar Merger from Sirius B; T7 / T8 (P4): the Magnetar Collapse / Galactic Merger
            final BTSChecklist c = rt.transitionChecklist(n - 1);
            rows.section(BTSText.lit(EyeStarRuntime.eventFor(n - 1)), c.countText());
            rows.checks(c);
        }
        lastRow(rows);
    }

    // ================================================================================= Info page

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        final int u = rt().unlockedTier();
        rows.para(
            BTSText.lang(
                "beyondthestars.gui.eye.info.tier",
                multiblock.installedTier(),
                u,
                multiblock.structureTier(),
                multiblock.compressionTier()),
            BTSGuiKit.VALUE);
        for (String k : new String[] { "light", "run", "zones", "lock", "dial", "feed", "rig" }) {
            rows.para(BTSText.lang("beyondthestars.gui.eye.info." + k), BTSGuiKit.VALUE);
        }
        if (u >= 6) rows.para(BTSText.lang("beyondthestars.gui.eye.info.galaxy"), BTSGuiKit.VALUE);
        if (u < 8) {
            rows.para(
                BTSText.lang(
                    "beyondthestars.gui.eye.info.next",
                    BTSText.key("beyondthestars.gui.eye.info.next." + (u + 1))),
                BTSGuiKit.VALUE);
        }
    }

    // ================================================================================ gap row

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }

    @Override
    protected boolean shouldDisplayBatchMode() {
        return false;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }

    /** Dial and (T4 only) Feed: − / + (right-click: four steps), the setpoint in a dark box (spec §5.1.2). */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final DoubleSyncValue dial = syncManager.findSyncHandler("eyeDial", DoubleSyncValue.class);
        final DoubleSyncValue feed = syncManager.findSyncHandler("eyeFeed", DoubleSyncValue.class);
        final DoubleSyncValue paid = syncManager.findSyncHandler("eyeDialPaid", DoubleSyncValue.class);
        final DoubleSyncValue cap = syncManager.findSyncHandler("eyeDialMax", DoubleSyncValue.class);
        final DoubleSyncValue fed = syncManager.findSyncHandler("eyeFeedActual", DoubleSyncValue.class);
        final BooleanSyncValue feedOn = syncManager.findSyncHandler("eyeFeedOn", BooleanSyncValue.class);
        final Flow dialStepper = BTSGuiKit.stepper(
            "beyondthestars.gui.eye.dial",
            dial,
            EyeStarRuntime.DIAL_STEP,
            1,
            EyeStarRuntime.DIAL_MAX,
            4,
            "%.2f",
            24,
            t -> {
                for (int i = 1; i <= 5; i++) t.addLine(tr("beyondthestars.gui.eye.dial.tip" + i));
                t.addLine(
                    EnumChatFormatting.GRAY + tr(
                        "beyondthestars.gui.eye.dialtip",
                        BTSFormat.f2(dial.getDoubleValue()),
                        BTSFormat.f2(paid.getDoubleValue()),
                        BTSFormat.f2(cap.getDoubleValue())));
            },
            tr("beyondthestars.gui.eye.dial.minus"),
            tr("beyondthestars.gui.eye.dial.plus"),
            tr("beyondthestars.gui.eye.dial.screwdriver"));
        final Flow feedStepper = BTSGuiKit.stepper(
            "beyondthestars.gui.eye.feed",
            feed,
            EyeStarRuntime.FEED_STEP,
            0,
            EyeStarRuntime.FEED_MAX,
            4,
            "%.2f",
            24,
            t -> {
                for (int i = 1; i <= 4; i++) t.addLine(tr("beyondthestars.gui.eye.feed.tip" + i));
                t.addLine(
                    EnumChatFormatting.GRAY + tr(
                        "beyondthestars.gui.eye.feedtip",
                        String.format(Locale.ROOT, "%.3f", fed.getDoubleValue()),
                        String.format(Locale.ROOT, "%.3f", feed.getDoubleValue())));
            },
            tr("beyondthestars.gui.eye.feed.minus"),
            tr("beyondthestars.gui.eye.feed.plus"),
            tr("beyondthestars.gui.eye.feed.screwdriver"));
        feedStepper.marginLeft(4)
            .setEnabledIf(w -> feedOn.getBoolValue());
        return super.createLeftPanelGapRow(parent, syncManager).collapseDisabledChild()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(dialStepper)
            .child(feedStepper);
    }

    private static String tr(String key, Object... args) {
        return BTSText.resolve(BTSText.lang(key, args));
    }

    /**
     * Right of the gap row, on screen left to right: [Galaxy] [Field] [i] (no power panel on the Eye, Q2). The Galaxy
     * panel (P4) is for T7 / T8; on a lower Eye it says what it is for.
     */
    @Override
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler fieldPanel = syncManager
            .syncedPanel("eyeFieldPanel", true, (sub, handler) -> buildFieldPanel(syncManager, parent));
        final IPanelHandler galaxyPanel = syncManager
            .syncedPanel("eyeGalaxyPanel", true, (sub, handler) -> buildGalaxyPanel(syncManager, parent));
        row.child(
            BTSGuiKit.panelButton(
                GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_SINGULARITY,
                galaxyPanel,
                "beyondthestars.gui.eye.galaxy.button"));
        row.child(
            BTSGuiKit.panelButton(
                GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS,
                fieldPanel,
                "beyondthestars.gui.eye.field.button"));
    }

    @Override
    protected void renderTooltip(RichTooltip t) {
        t.addLine(tr("beyondthestars.gui.eye.render.tip1"));
        t.addLine(EnumChatFormatting.GRAY + tr("beyondthestars.gui.eye.render.tip2"));
        t.addLine(
            EnumChatFormatting.GRAY + tr(
                Config.eyeRenderCentre ? "beyondthestars.gui.eye.render.tip3"
                    : "beyondthestars.gui.eye.render.tip3off"));
    }

    // ============================================================================== field panel

    /** "live\u0002refused\u0002sensors" (server). */
    private String socketLine() {
        return multiblock.fieldHatchCount() + "\u0002"
            + multiblock.rejectedFieldHatchCount()
            + "\u0002"
            + multiblock.sensorHatchCount();
    }

    /** One line per live sensor: "condition\u0002output" (server). */
    private String sensorLines() {
        final StringBuilder sb = new StringBuilder();
        for (MTEHatchStellarFieldSensor s : multiblock.sensorHatches()) {
            if (s == null || !s.isValid()) continue;
            String line = s.statusLine();
            final int cut = line.lastIndexOf(", out ");
            if (cut >= 0) line = line.substring(0, cut);
            if (sb.length() > 0) sb.append('\n');
            sb.append(BTSText.lit(line))
                .append('\u0002')
                .append(s.currentOutput());
        }
        return sb.toString();
    }

    /** One line per trim drained this second: "name\u0002litres" (server). */
    private String trimLines() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : rt().lastTrimsByName()
            .entrySet()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(BTSText.lit(e.getKey()))
                .append('\u0002')
                .append(e.getValue());
        }
        return sb.toString();
    }

    /** B-115: one line per buffer held or pulled: name, held, cap (-1: not pulled), 0x02-separated (server). */
    private String intakeLines() {
        final StringBuilder sb = new StringBuilder();
        for (MTEEyeOfHarmonyBTS.IntakeRow r : multiblock.intakeRows()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(BTSText.lit(r.name))
                .append((char) 2)
                .append(r.held)
                .append((char) 2)
                .append(r.cap);
        }
        return sb.toString();
    }

    /** Bench: the Galaxy panel's text lines as the client would draw them (server side, colour codes stripped). */
    public List<String> benchGalaxyPanel() {
        final List<String> out = new ArrayList<>();
        for (String l : galaxyPanelText().split("\n")) {
            if (!l.isEmpty()) out.add(
                "  " + BTSText.resolve(l)
                    .replaceAll("§.", ""));
        }
        return out;
    }

    /** The Galaxy panel's body: the slots or companions, the next transition's state, the test fire (server). */
    private String galaxyPanelText() {
        final EyeStarRuntime rt = rt();
        final StringBuilder sb = new StringBuilder();
        for (String l : rt.slotLines()) sb.append(BTSText.lit(l))
            .append('\n');
        if (rt.mode() == EyeGalaxy.Mode.GALAXY) {
            sb.append(BTSText.lang("beyondthestars.gui.eye.companions", rt.companionNames()))
                .append('\n');
        }
        final StarDef star = rt.star();
        if (star != null && (star.n() == 6 || star.n() == 7) && rt.state() == EyeStarRuntime.StarState.PENDING) {
            final List<String> miss = rt.missing(star.n());
            sb.append(
                miss.isEmpty()
                    ? BTSText.lang("beyondthestars.gui.eye.galaxy.ready", EyeTransition.Type.leaving(star.n()).title)
                    : BTSText.lang("beyondthestars.gui.eye.galaxy.waits", miss.get(0)))
                .append('\n');
            // B-118 "Warn, allow": the linked quasar's peak is under what cancels the collapse shock (red, Begin works)
            if (rt.collapseQuasarWeak()) {
                sb.append(
                    BTSText.cat(
                        BTSText.lit("§c"),
                        BTSText.lang(
                            "beyondthestars.gui.eye.galaxy.weakquasar",
                            BTSFormat.fmt1(rt.linkedQuasarPeak()),
                            BTSFormat.buEuShort(rt.collapseFullCancelBu()))))
                    .append('\n');
            }
        }
        if (rt.testFireWanted()) {
            sb.append(
                BTSText.lang(
                    star != null && star.n() == 7 ? "beyondthestars.gui.eye.galaxy.testfire7"
                        : "beyondthestars.gui.eye.galaxy.testfire"))
                .append('\n');
        } else {
            final String note = rt.heavyTestFireNote();
            if (!note.isEmpty()) sb.append(BTSText.lit(note))
                .append('\n');
        }
        return sb.toString();
    }

    /** B-120: the buffer rows the Field panel has no room for (0: all shown). */
    private static int intakeOverflow(String s) {
        if (s == null || s.isEmpty()) return 0;
        return Math.max(0, s.split("\n", -1).length - INTAKE_ROWS);
    }

    private static String[] part(String s, int line, int field) {
        if (s == null || s.isEmpty()) return null;
        final String[] lines = s.split("\n", -1);
        if (line >= lines.length) return null;
        final String[] f = lines[line].split("\u0002", -1);
        return field < f.length ? f : null;
    }

    private static final int PANEL_DIM = 0xFF555555;

    private static ScrollingTextWidget panelText(java.util.function.Supplier<String> text, int colour) {
        return new ScrollingTextWidget(IKey.dynamic(text::get)).width(ROW_W)
            .height(9)
            .color(colour)
            .shadow(false)
            .marginBottom(1);
    }

    private static IWidget panelStat(String labelKey, java.util.function.Supplier<String> value) {
        return Flow.row()
            .size(ROW_W, 9)
            .marginBottom(1)
            .child(
                IKey.lang(labelKey)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .size(100, 9)
                    .textAlign(Alignment.CenterLeft))
            .child(
                IKey.dynamic(value::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .size(ROW_W - 100, 9)
                    .textAlign(Alignment.CenterRight));
    }

    /** A Field panel row: text on the left (scrolls on hover when long), a value right-aligned in panel text. */
    private static Flow panelPair(java.util.function.Supplier<String> left, int leftColour,
        java.util.function.Supplier<String> right) {
        final int w = ROW_W, rw = 44;
        return Flow.row()
            .size(w, 9)
            .marginBottom(1)
            .child(
                new ScrollingTextWidget(IKey.dynamic(left::get)).size(w - rw, 9)
                    .color(leftColour)
                    .shadow(false))
            .child(
                IKey.dynamic(right::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .size(rw, 9)
                    .textAlign(Alignment.CenterRight));
    }

    /** One column of a zone legend line ("name range yield", split on double spaces). */
    private static IWidget legendCell(String key, int col, int width, Alignment align) {
        return IKey.dynamic(() -> {
            final String[] cells = StatCollector.translateToLocal(key)
                .split(" {2,}");
            if (cells.length < 3) return col == 0 ? StatCollector.translateToLocal(key) : "";
            return cells[col];
        })
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .size(width, 9)
            .textAlign(align);
    }

    private static IWidget divider() {
        return ((IDrawable) (context, x, y, width, height, theme) -> GuiDraw.drawRect(x, y, width, height, 0xFF8B8B8B))
            .asWidget()
            .size(ROW_W, 1)
            .margin(0, 2);
    }

    /**
     * The Field panel (spec §5.1.6, 176 x 196, text at 1.0): the zone legend, the sockets and sensors (each sensor's
     * condition and output), the trims drained this second, and a pointer to the standard rig on the Info page.
     */
    private ModularPanel buildFieldPanel(PanelSyncManager main, ModularPanel parent) {
        final StringSyncValue sockets = main.findSyncHandler("eyeSockets", StringSyncValue.class);
        final StringSyncValue sensors = main.findSyncHandler("eyeSensors", StringSyncValue.class);
        final StringSyncValue trims = main.findSyncHandler("eyeTrims", StringSyncValue.class);
        final StringSyncValue intake = main.findSyncHandler("eyeIntake", StringSyncValue.class);
        final int w = ROW_W;
        final ListWidget<IWidget, ?> list = new ListWidget<>().size(FIELD_PANEL_W - 8, FIELD_PANEL_H - 8)
            .pos(4, 4)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .collapseDisabledChild();
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.title")
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .style(EnumChatFormatting.BOLD)
                .size(w, 10)
                .textAlign(Alignment.Center)
                .marginBottom(2));
        for (int z = 0; z < 5; z++) {
            final int colour = BTSGuiKit.ZONE_BAND[z];
            // "Resonant ±10 x1.25": name, range and yield in three columns (the mockup's table); a translation
            // without the double spaces shows whole in the first column
            final String key = "beyondthestars.gui.eye.field.zone." + z;
            list.child(
                Flow.row()
                    .size(w, 9)
                    .marginBottom(1)
                    .child(
                        ((IDrawable) (context, x, y, width, height, theme) -> GuiDraw
                            .drawRect(x, y, width, height, colour)).asWidget()
                                .size(12, 8)
                                .marginRight(3))
                    .child(legendCell(key, 0, 58, Alignment.CenterLeft))
                    .child(legendCell(key, 1, 34, Alignment.CenterLeft))
                    .child(legendCell(key, 2, w - 15 - 58 - 34, Alignment.CenterRight)));
        }
        list.child(divider());
        list.child(panelStat("beyondthestars.gui.eye.field.sockets", () -> {
            final String[] f = part(sockets.getStringValue(), 0, 0);
            return f == null ? "" : tr("beyondthestars.gui.eye.field.live", "§2" + f[0]);
        }));
        list.child(panelStat("beyondthestars.gui.eye.field.mestock", () -> {
            final String[] f = part(sockets.getStringValue(), 0, 1);
            final String n = f == null ? "0" : f[1];
            return tr("beyondthestars.gui.eye.field.refused", ("0".equals(n) ? "" : "§4") + n);
        }));
        list.child(panelStat("beyondthestars.gui.eye.field.sensors", () -> {
            final String[] f = part(sockets.getStringValue(), 0, 2);
            return f == null ? "0" : f[2];
        }));
        for (int k = 0; k < SENSOR_ROWS; k++) {
            final int i = k;
            list.child(panelPair(() -> {
                final String[] f = part(sensors.getStringValue(), i, 1);
                return f == null ? "" : " " + f[0];
            }, PANEL_DIM, () -> {
                final String[] f = part(sensors.getStringValue(), i, 1);
                if (f == null) return "";
                return (!"0".equals(f[1]) ? "§2" : "§8") + tr("beyondthestars.gui.eye.field.out", f[1]);
            }).setEnabledIf(x -> part(sensors.getStringValue(), i, 1) != null));
        }
        list.child(divider());
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.trims")
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginBottom(1));
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.notrims")
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginBottom(1)
                .setEnabledIf(
                    x -> trims.getStringValue() == null || trims.getStringValue()
                        .isEmpty()));
        for (int k = 0; k < TRIM_ROWS; k++) {
            final int i = k;
            list.child(panelPair(() -> {
                final String[] f = part(trims.getStringValue(), i, 1);
                return f == null ? "" : " " + f[0];
            }, PANEL_DIM, () -> {
                final String[] f = part(trims.getStringValue(), i, 1);
                return f == null ? "" : BTSFormat.exact(parseLong(f[1])) + " L";
            }).setEnabledIf(x -> part(trims.getStringValue(), i, 1) != null));
        }
        // B-115 (B-108): each buffer against the intake cap the sockets fill it to
        list.child(divider());
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.buffers")
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginBottom(1)
                .tooltip(
                    t -> t.addLine(tr("beyondthestars.gui.eye.field.buffers.tip1"))
                        .addLine(EnumChatFormatting.GRAY + tr("beyondthestars.gui.eye.field.buffers.tip2"))
                        .addLine(EnumChatFormatting.GRAY + tr("beyondthestars.gui.eye.field.buffers.tip3")))
                .tooltipShowUpTimer(TOOLTIP_DELAY));
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.nobuffers")
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginBottom(1)
                .setEnabledIf(
                    x -> intake.getStringValue() == null || intake.getStringValue()
                        .isEmpty()));
        for (int k = 0; k < INTAKE_ROWS; k++) {
            final int i = k;
            list.child(BTSGuiKit.panelBar(w, () -> {
                final String[] f = part(intake.getStringValue(), i, 2);
                return f == null ? "" : f[0];
            }, () -> {
                final String[] f = part(intake.getStringValue(), i, 2);
                if (f == null) return 0;
                final long held = parseLong(f[1]), cap = parseLong(f[2]);
                return cap <= 0 ? (held > 0 ? 1 : 0) : held / (double) cap;
            }, () -> {
                final String[] f = part(intake.getStringValue(), i, 2);
                if (f == null) return Grad.BLUE;
                final long held = parseLong(f[1]), cap = parseLong(f[2]);
                return cap < 0 || held > cap ? Grad.GOLD : held >= cap ? Grad.GREEN : Grad.BLUE;
            }, () -> {
                final String[] f = part(intake.getStringValue(), i, 2);
                if (f == null) return "";
                final long held = parseLong(f[1]), cap = parseLong(f[2]);
                return cap < 0 ? BTSFormat.litres(held) + " L / " + tr("beyondthestars.gui.eye.field.notpulled")
                    : BTSFormat.haveNeed(held, cap) + " L";
            })
                .tooltipDynamic(t -> {
                    final String[] f = part(intake.getStringValue(), i, 2);
                    if (f == null) return;
                    final long held = parseLong(f[1]), cap = parseLong(f[2]);
                    t.addLine(f[0]);
                    t.addLine(
                        EnumChatFormatting.GRAY + tr(
                            cap < 0 ? "beyondthestars.gui.eye.field.buffer.tipnp"
                                : held > cap ? "beyondthestars.gui.eye.field.buffer.tipover"
                                    : "beyondthestars.gui.eye.field.buffer.tip",
                            BTSFormat.exact(held),
                            BTSFormat.exact(Math.max(0, cap))));
                })
                .tooltipAutoUpdate(true)
                .tooltipShowUpTimer(TOOLTIP_DELAY)
                .marginBottom(1)
                .setEnabledIf(x -> part(intake.getStringValue(), i, 2) != null));
        }
        // B-120: the rows past INTAKE_ROWS are counted, not dropped silently (the scanner lists them all)
        list.child(
            IKey.dynamic(() -> tr("beyondthestars.gui.eye.field.buffers.more", intakeOverflow(intake.getStringValue())))
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginBottom(1)
                .setEnabledIf(x -> intakeOverflow(intake.getStringValue()) > 0));
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.footer")
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .textAlign(Alignment.CenterLeft)
                .marginTop(2));
        return new ModularPanel("eyeFieldPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(FIELD_PANEL_W, FIELD_PANEL_H)
            .child(list);
    }

    // ============================================================================= galaxy panel

    /**
     * P4: the Galaxy panel (176 x 196, text at 1.0): the mode (galaxy / planets; switching puts a lit T7 / T8 field
     * out and lights the other), the T8 Ergomatter feed f8 (the kit stepper), Begin (enabled while Galaxy Transition
     * bit 1 is on, the answer beside it), then the slots or companions with what the next transition waits for, and
     * the standard rig in brief.
     */
    private ModularPanel buildGalaxyPanel(PanelSyncManager main, ModularPanel parent) {
        final BooleanSyncValue planets = main.findSyncHandler("eyePlanets", BooleanSyncValue.class);
        final BooleanSyncValue modeLive = main.findSyncHandler("eyeModeLive", BooleanSyncValue.class);
        final DoubleSyncValue f8 = main.findSyncHandler("eyeF8", DoubleSyncValue.class);
        // QA-A16: two clicks within MODE_ARM_MS while a lit field would go out (the first arms it, client side only)
        final long[] armedAt = { 0L };
        final BooleanSupplier armed = () -> modeLive.getBoolValue()
            && System.currentTimeMillis() - armedAt[0] < MODE_ARM_MS;
        final BooleanSyncValue ready = main.findSyncHandler("eyeReady", BooleanSyncValue.class);
        final StringSyncValue msg = main.findSyncHandler("eyeBeginMsg", StringSyncValue.class);
        final StringSyncValue text = main.findSyncHandler("eyeGalaxyText", StringSyncValue.class);
        final int w = ROW_W;
        final ListWidget<IWidget, ?> list = new ListWidget<>().size(GALAXY_PANEL_W - 8, GALAXY_PANEL_H - 8)
            .pos(4, 4)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .collapseDisabledChild();
        list.child(
            IKey.lang("beyondthestars.gui.eye.galaxy.title")
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .style(EnumChatFormatting.BOLD)
                .size(w, 10)
                .textAlign(Alignment.Center)
                .marginBottom(2));
        list.child(
            Flow.row()
                .size(w, 16)
                .childPadding(4)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .marginBottom(2)
                .child(
                    new ButtonWidget<>().size(16, 16)
                        .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                        .onMousePressed(btn -> {
                            if (modeLive.getBoolValue() && !armed.getAsBoolean()) {
                                armedAt[0] = System.currentTimeMillis();
                                return true;
                            }
                            armedAt[0] = 0L;
                            planets.setBoolValue(!planets.getBoolValue());
                            return true;
                        })
                        .tooltip(t -> {
                            t.addLine(tr("beyondthestars.gui.eye.galaxy.mode.tip1"));
                            for (int i = 2; i <= 6; i++) {
                                t.addLine(EnumChatFormatting.GRAY + tr("beyondthestars.gui.eye.galaxy.mode.tip" + i));
                            }
                        })
                        .tooltipShowUpTimer(TOOLTIP_DELAY))
                .child(
                    IKey.dynamic(
                        () -> armed.getAsBoolean()
                            ? EnumChatFormatting.RED + tr("beyondthestars.gui.eye.galaxy.confirm")
                            : tr(
                                "beyondthestars.gui.eye.galaxy.mode",
                                tr(
                                    planets.getBoolValue() ? "beyondthestars.gui.eye.galaxy.planets"
                                        : "beyondthestars.gui.eye.galaxy.galaxy")))
                        .color(BTSGuiKit.PANEL_TEXT)
                        .asWidget()
                        .size(w - 20, 9)));
        list.child(
            Flow.row()
                .size(w, 16)
                .marginBottom(2)
                .child(
                    BTSGuiKit.stepper(
                        "beyondthestars.gui.eye.galaxy.f8",
                        f8,
                        EyeGalaxy.F8_STEP,
                        GalaxyField.F8_MIN,
                        GalaxyField.F8_MAX,
                        4,
                        "%.1f",
                        28,
                        t -> {
                            for (int i = 1; i <= 4; i++) t.addLine(
                                (i == 1 ? "" : EnumChatFormatting.GRAY.toString())
                                    + tr("beyondthestars.gui.eye.galaxy.f8.tip" + i));
                        },
                        tr("beyondthestars.gui.eye.galaxy.f8.minus"),
                        tr("beyondthestars.gui.eye.galaxy.f8.plus"),
                        "")));
        list.child(
            Flow.row()
                .size(w, 16)
                .childPadding(4)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .marginBottom(2)
                .child(
                    new ButtonWidget<>().size(16, 16)
                        .overlay(GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                        .onMousePressed(btn -> {
                            main.callSyncedAction("eyeBegin", packet -> {});
                            return true;
                        })
                        .tooltip(t -> {
                            t.addLine(tr("beyondthestars.gui.eye.galaxy.begin.tip1"));
                            for (int i = 2; i <= 4; i++) {
                                t.addLine(EnumChatFormatting.GRAY + tr("beyondthestars.gui.eye.galaxy.begin.tip" + i));
                            }
                        })
                        .tooltipShowUpTimer(TOOLTIP_DELAY))
                .child(
                    new ScrollingTextWidget(
                        IKey.dynamic(
                            () -> (ready.getBoolValue() ? "§2" + tr("beyondthestars.gui.eye.galaxy.isready")
                                : tr("beyondthestars.gui.eye.galaxy.notready"))
                                + (msg.getStringValue()
                                    .isEmpty() ? "" : "§8: " + msg.getStringValue()))).size(w - 20, 9)
                                        .color(BTSGuiKit.PANEL_TEXT)
                                        .shadow(false)));
        list.child(divider());
        for (int k = 0; k < GALAXY_TEXT_ROWS; k++) {
            final int i = k;
            list.child(panelText(() -> {
                final String[] lines = text.getStringValue()
                    .split("\n");
                return i < lines.length ? BTSText.resolve(lines[i]) : "";
            }, BTSGuiKit.PANEL_TEXT).setEnabledIf(x -> {
                final String[] lines = text.getStringValue()
                    .split("\n");
                return i < lines.length && !lines[i].isEmpty();
            }));
        }
        list.child(divider());
        for (int i = 1; i <= 3; i++) {
            final String key = "beyondthestars.gui.eye.galaxy.rig" + i;
            list.child(panelText(() -> tr(key), PANEL_DIM));
        }
        return new ModularPanel("eyeGalaxyPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(GALAXY_PANEL_W, GALAXY_PANEL_H)
            .child(list);
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
