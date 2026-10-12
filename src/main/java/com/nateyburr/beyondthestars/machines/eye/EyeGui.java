package com.nateyburr.beyondthestars.machines.eye;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

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
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.star.CoreCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.EyeMilestones;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.FirstCooling;
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
 */
public class EyeGui extends BTSControllerGui<MTEEyeOfHarmonyBTS> {

    private static final int FIELD_PANEL_W = 176;
    private static final int FIELD_PANEL_H = 196;
    private static final int SENSOR_ROWS = 8, TRIM_ROWS = 6;
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
    }

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
        if (rt.transition() != null) return Badge.TRANSITION;
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
                return BTSText.lit((star == null ? "?" : star.name) + " > " + (want == null ? "?" : want.name));
            case DARK:
                return want == null ? "" : BTSText.lit(want.name);
            default:
                return star != null ? BTSText.lit(star.name) : want != null ? BTSText.lit(want.name) : "";
        }
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

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 26;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
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
        if (active.length >= 4) {
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
        final TierParams t = TierParams.of(Math.max(1, Math.min(6, r.tier)));
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
        if (star.n() == 4) {
            rows.stat(
                BTSText.lang("beyondthestars.gui.eye.hsm"),
                BTSText.lit(String.format(Locale.ROOT, "%.3f / %.3f L/s", rt.feedActual(), rt.feedSetpoint())),
                BTSText.lang("beyondthestars.gui.eye.hsm.tip"));
        }
        dialRows(rows);
        warnRow(rows, r);
        final int u = rt.unlockedTier();
        final EyeMilestones ms = rt.milestones();
        if (u <= 5 && !ms.met(u)) {
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

    private void nextBatchRows(BTSRows.Builder rows) {
        final EyeStarRuntime rt = rt();
        final Fluid wdm = EyeStarRuntime.wdmFluid();
        rows.section(BTSText.lang("beyondthestars.gui.eye.nextbatch"), "");
        if (wdm == null) {
            rows.check(BTSChecklist.State.BAD, BTSText.lang("beyondthestars.gui.eye.chk.nofluid"), "", -1);
            return;
        }
        final long need = EyeStarRuntime.BATCH_WDM * Math.max(1, rt.parallel(5));
        final long have = multiblock.buffer(wdm);
        final String name = wdm.getLocalizedName(new FluidStack(wdm, 1));
        rows.check(
            have >= need ? BTSChecklist.State.OK : BTSChecklist.State.BAD,
            BTSText.lit(name),
            BTSText.lit(BTSFormat.haveNeed(have, need) + " L"),
            BTSChecklist.frac(have, need),
            BTSText.lang("beyondthestars.gui.eye.batch.charge.tip", BTSFormat.exact(have), BTSFormat.exact(need)));
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
        if (n >= 1 && n <= 3) {
            rows.section(BTSText.lang("beyondthestars.gui.eye.sec.light", want.name), "");
            rows.checks(rt.ignitionChecklist(want));
        } else if (n == 4) {
            final StarDef progenitor = rt.ignitableFor(want);
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
        } else if (n == 6) {
            final BTSChecklist c = rt.transitionChecklist(5);
            rows.section(BTSText.lit(EyeStarRuntime.eventFor(5)), c.countText());
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
        if (u < 6) {
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

    /** Right of the gap row, on screen left to right: [Field] [i] (no power panel on the Eye, Q2). */
    @Override
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler fieldPanel = syncManager
            .syncedPanel("eyeFieldPanel", true, (sub, handler) -> buildFieldPanel(syncManager, parent));
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

    private static String[] part(String s, int line, int field) {
        if (s == null || s.isEmpty()) return null;
        final String[] lines = s.split("\n", -1);
        if (line >= lines.length) return null;
        final String[] f = lines[line].split("\u0002", -1);
        return field < f.length ? f : null;
    }

    private static final int PANEL_DIM = 0xFF555555;

    private static ScrollingTextWidget panelText(java.util.function.Supplier<String> text, int colour) {
        return new ScrollingTextWidget(IKey.dynamic(text::get)).width(FIELD_PANEL_W - 8)
            .height(9)
            .color(colour)
            .shadow(false)
            .marginBottom(1);
    }

    private static IWidget panelStat(String labelKey, java.util.function.Supplier<String> value) {
        return Flow.row()
            .size(FIELD_PANEL_W - 8, 9)
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
                    .size(FIELD_PANEL_W - 8 - 100, 9)
                    .textAlign(Alignment.CenterRight));
    }

    private static IWidget divider() {
        return ((IDrawable) (context, x, y, width, height, theme) -> GuiDraw.drawRect(x, y, width, height, 0xFF8B8B8B))
            .asWidget()
            .size(FIELD_PANEL_W - 8, 1)
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
        final int w = FIELD_PANEL_W - 8;
        final ListWidget<IWidget, ?> list = new ListWidget<>().size(w, FIELD_PANEL_H - 8)
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
            list.child(
                Flow.row()
                    .size(w, 9)
                    .marginBottom(1)
                    .childPadding(3)
                    .child(
                        ((IDrawable) (context, x, y, width, height, theme) -> GuiDraw
                            .drawRect(x, y, width, height, colour)).asWidget()
                                .size(12, 8))
                    .child(
                        IKey.lang("beyondthestars.gui.eye.field.zone." + z)
                            .color(BTSGuiKit.PANEL_TEXT)
                            .asWidget()
                            .height(9)
                            .textAlign(Alignment.CenterLeft)));
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
            list.child(panelText(() -> {
                final String[] f = part(sensors.getStringValue(), i, 1);
                if (f == null) return "";
                final boolean on = !"0".equals(f[1]);
                return f[0] + "  " + (on ? "§2" : "§8") + tr("beyondthestars.gui.eye.field.out", f[1]);
            }, PANEL_DIM).setEnabledIf(x -> part(sensors.getStringValue(), i, 1) != null));
        }
        list.child(divider());
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.trims")
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .size(w, 9)
                .marginBottom(1));
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.notrims")
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .marginBottom(1)
                .setEnabledIf(
                    x -> trims.getStringValue() == null || trims.getStringValue()
                        .isEmpty()));
        for (int k = 0; k < TRIM_ROWS; k++) {
            final int i = k;
            list.child(panelText(() -> {
                final String[] f = part(trims.getStringValue(), i, 1);
                return f == null ? "" : "§8" + f[0] + "  §0" + BTSFormat.exact(parseLong(f[1])) + " L";
            }, BTSGuiKit.PANEL_TEXT).setEnabledIf(x -> part(trims.getStringValue(), i, 1) != null));
        }
        list.child(
            IKey.lang("beyondthestars.gui.eye.field.footer")
                .color(PANEL_DIM)
                .asWidget()
                .size(w, 9)
                .marginTop(2));
        return new ModularPanel("eyeFieldPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(FIELD_PANEL_W, FIELD_PANEL_H)
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
