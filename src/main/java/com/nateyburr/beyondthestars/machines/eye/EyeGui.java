package com.nateyburr.beyondthestars.machines.eye;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * D-49 unit 6: the Eye of Harmony's console (d49-build-plan.md §12, GT's standard MUI2 multiblock layout). Nothing a
 * player needs takes a screwdriver any more (the screwdriver still steps the dial and the feed).
 * <ul>
 * <li><b>Terminal:</b> the Eye's tier (installed, unlocked, structure, compression); the star (state, tier, parallel)
 * or why it waits; one bar per active gauge (−120..+120, the zone bands drawn around the setpoint: Resonant ±10,
 * Stable ±40, Strained ±70, Critical ±100, Breach beyond; T5 draws them around the cooling setpoint); Stability, Y,
 * Harmonic Lock and Integrity; the event warning; upkeep r, the next cycle and its EU; the dial (set, paid, cap) and
 * the T4 HSM feed; the transition with a progress bar; the milestones and the escrow; the last failure; the render
 * note.</li>
 * <li><b>Gap row:</b> the time dial and the HSM feed setpoints, each with − / + (right-click: four steps); the centre
 * render switch and the Field panel on the right.</li>
 * <li><b>Field panel:</b> the zone legend, the Field and Sensor sockets (with every sensor's channel and output), the
 * trims drained this second, and the standard automation rig in one line.</li>
 * </ul>
 * Every value is server state and comes across as a sync value; the two setpoints are written back through the
 * runtime's clamping setters ({@link EyeStarRuntime#setDial}, {@link EyeStarRuntime#setFeed}).
 */
public class EyeGui extends MTEMultiBlockBaseGui<MTEEyeOfHarmonyBTS> {

    private static final int BAR_TRACK = 0xFF1E2026;
    private static final int BAR_EDGE = 0xFF4A4D55;
    /** Zone colours, Resonant .. Breach (dim, so the white marker and the label read on them). */
    private static final int[] ZONE_COLOUR = { 0xFF1F6B3A, 0xFF2E5A6B, 0xFF6B6020, 0xFF7A3A14, 0xFF6E1A1A };
    private static final int[] ZONE_EDGE = { 10, 40, 70, 100 };
    private static final int MARKER = 0xFFFFFFFF;
    private static final int SETPOINT_TICK = 0xFFFFFF55;
    private static final int PROGRESS_FROM = 0xFF2F6B8F;
    private static final int PROGRESS_TO = 0xFF7CD8FF;
    /** "No setpoint" (a sentinel, not NaN, so the sync value only sends real changes). */
    private static final double NO_SP = -999;

    private static final int FIELD_PANEL_W = 176;
    private static final int FIELD_PANEL_H = 188;

    public EyeGui(MTEEyeOfHarmonyBTS multiblock) {
        super(multiblock);
    }

    private EyeStarRuntime rt() {
        return multiblock.starRuntime();
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("eyeHead", new StringSyncValue(this::headLine));
        syncManager.syncValue("eyeStar", new StringSyncValue(this::starLine));
        syncManager.syncValue("eyeField", new StringSyncValue(this::fieldLine));
        syncManager.syncValue("eyeWarn", new StringSyncValue(this::warnLine));
        syncManager.syncValue("eyeUpkeep", new StringSyncValue(this::upkeepLine));
        syncManager.syncValue("eyeDialLine", new StringSyncValue(this::dialLine));
        syncManager.syncValue("eyeTrans", new StringSyncValue(() -> rt().transitionLine()));
        syncManager.syncValue("eyeTransP", new DoubleSyncValue(() -> rt().transitionProgress()));
        syncManager.syncValue("eyeMilestone", new StringSyncValue(this::milestoneLine));
        syncManager.syncValue("eyeFail", new StringSyncValue(() -> rt().lastFailure()));
        syncManager.syncValue("eyeRenderNote", new StringSyncValue(multiblock::getRenderNote));
        syncManager.syncValue("eyeGaugeMask", new IntSyncValue(this::gaugeMask));
        for (int k = 0; k < 4; k++) {
            final int gauge = k;
            syncManager.syncValue("eyeGauge" + k, new DoubleSyncValue(() -> gaugeValue(gauge)));
        }
        syncManager.syncValue("eyeSp", new DoubleSyncValue(this::setpoint));
        syncManager
            .syncValue("eyeDial", new DoubleSyncValue(() -> rt().dialSetpoint(), v -> rt().setDial(v)).allowC2S());
        syncManager
            .syncValue("eyeFeed", new DoubleSyncValue(() -> rt().feedSetpoint(), v -> rt().setFeed(v)).allowC2S());
        syncManager.syncValue(
            "eyeRender",
            new BooleanSyncValue(multiblock::isRenderEnabled, multiblock::setRenderEnabled).allowC2S());
        syncManager.syncValue("eyeSockets", new StringSyncValue(this::socketLines));
        syncManager.syncValue("eyeTrims", new StringSyncValue(this::trimLine));
    }

    // ============================================================================== server text

    private String headLine() {
        if (!multiblock.isFormedForGui()) {
            return EnumChatFormatting.RED + "Not formed"
                + EnumChatFormatting.GRAY
                + " (unlocked T"
                + rt().unlockedTier()
                + ")";
        }
        return EnumChatFormatting.AQUA + "Eye of Harmony T"
            + multiblock.installedTier()
            + EnumChatFormatting.GRAY
            + " (unlocked T"
            + rt().unlockedTier()
            + ", structure T"
            + multiblock.structureTier()
            + ", compression T"
            + multiblock.compressionTier()
            + ")";
    }

    private String starLine() {
        final EyeStarRuntime rt = rt();
        final StarDef star = rt.star();
        final StarDef target = StarDef.byChart(multiblock.getControllerSlot());
        switch (rt.state()) {
            case IGNITING:
                return EnumChatFormatting.GOLD + star.name
                    + " IGNITING"
                    + EnumChatFormatting.GRAY
                    + ", P "
                    + StellarField.fmtSigned(rt.ignitionP())
                    + " (lights at "
                    + StellarField.fmtSigned(StellarField.IGNITION_LIT)
                    + ")";
            case LIT:
            case PENDING:
            case BATCH: {
                final String state = rt.transition() != null ? "TRANSITION"
                    : rt.state() == EyeStarRuntime.StarState.BATCH
                        ? String.format(Locale.ROOT, "COOLING %.0f%%", rt.batchFraction() * 100)
                        : rt.state()
                            .toString();
                return EnumChatFormatting.GREEN + star.name
                    + " (T"
                    + star.n()
                    + ") "
                    + state
                    + EnumChatFormatting.GRAY
                    + ", parallel "
                    + rt.lastParallel();
            }
            case IDLE:
                return EnumChatFormatting.YELLOW + (star == null ? "Star" : star.name)
                    + " IDLE"
                    + EnumChatFormatting.GRAY
                    + (rt.waitReason()
                        .isEmpty() ? "" : ": " + rt.waitReason());
            default:
                if (target == null) {
                    return EnumChatFormatting.GRAY + "No star: put a Star Chart (or a planet) in the controller slot";
                }
                return EnumChatFormatting.GRAY + "DARK"
                    + (rt.waitReason()
                        .isEmpty() ? " (" + target.name + " selected)" : ": " + rt.waitReason());
        }
    }

    private String fieldLine() {
        final FieldReadout r = rt().lastReadout();
        if (r == null || !r.lit) return "";
        final int zone = Math.max(0, Math.min(4, r.zone));
        return zoneColour(zone) + "Stability "
            + Math.round(r.stability)
            + " "
            + StellarField.ZONE_NAME[zone]
            + EnumChatFormatting.GRAY
            + String.format(Locale.ROOT, ", Y %.2f, Lock %d/%d, Integrity %.0f", r.Y, r.lock, 600, r.I)
            + (r.tier == 6 ? String.format(Locale.ROOT, ", shield %.0f", r.A) : "");
    }

    private String warnLine() {
        final FieldReadout r = rt().lastReadout();
        if (r == null || !r.lit || r.warn == 0) return "";
        final TierParams t = TierParams.of(Math.max(1, Math.min(6, r.tier)));
        final boolean signed = t.event != null && t.event.signed && !t.batch;
        return EnumChatFormatting.GOLD + "Event warning: "
            + (t.batch ? "crystallisation plateau" : t.event != null ? t.event.name : "event")
            + (signed ? " (" + (r.warn > 0 ? "+" : "-") + ")" : "")
            + ", trim ahead of it";
    }

    private String upkeepLine() {
        final EyeStarRuntime rt = rt();
        if (!rt.burning() || rt.star() == null) return "";
        final double r = rt.upkeepR();
        return (r < 1 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + String.format(
            Locale.ROOT,
            "Upkeep r %.2f%s, next cycle in %.0f s, %.2e EU/cycle",
            r,
            r < 1 ? " (SHORT)" : "",
            rt.nextCycleSeconds(),
            (double) rt.star().euPerCycle * Math.max(1, rt.lastParallel()));
    }

    private String dialLine() {
        final EyeStarRuntime rt = rt();
        final StringBuilder sb = new StringBuilder(EnumChatFormatting.GRAY.toString());
        sb.append(String.format(Locale.ROOT, "Dial set %.2f", rt.dialSetpoint()));
        if (rt.burning()) {
            sb.append(String.format(Locale.ROOT, ", running %.2f (cap %.2f)", rt.dialPaid(), rt.dialMax()));
        }
        final StarDef star = rt.star();
        if (star != null && star.n() == 4 && rt.burning()) {
            sb.append(String.format(Locale.ROOT, "; HSM feed %.3f / %.3f L/s", rt.feedActual(), rt.feedSetpoint()));
        }
        return sb.toString();
    }

    private String milestoneLine() {
        final EyeStarRuntime rt = rt();
        final StringBuilder sb = new StringBuilder();
        if (rt.unlockedTier() <= 5) sb.append(
            rt.milestones()
                .progressLine(rt.unlockedTier()));
        if (rt.escrow() != null) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("escrow: ")
                .append(
                    rt.escrow()
                        .getDisplayName());
        }
        return sb.toString();
    }

    private int gaugeMask() {
        int mask = 0;
        for (int k : rt().activeGauges()) mask |= 1 << k;
        return mask;
    }

    private double gaugeValue(int k) {
        final FieldReadout r = rt().lastReadout();
        return r == null || !r.lit ? 0 : Math.round(r.x(k) * 10) / 10.0;
    }

    private double setpoint() {
        final FieldReadout r = rt().lastReadout();
        return r == null || !r.lit || Double.isNaN(r.sp) ? NO_SP : Math.round(r.sp * 10) / 10.0;
    }

    private String socketLines() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Field sockets: ")
            .append(multiblock.fieldHatchCount())
            .append(" live");
        if (multiblock.rejectedFieldHatchCount() > 0) sb.append(", ")
            .append(multiblock.rejectedFieldHatchCount())
            .append(" ME stocking REJECTED");
        sb.append("\nSensors: ")
            .append(multiblock.sensorHatchCount());
        for (MTEHatchStellarFieldSensor s : multiblock.sensorHatches()) {
            if (s != null && s.isValid()) sb.append("\n  ")
                .append(s.statusLine());
        }
        return sb.toString();
    }

    private String trimLine() {
        final Map<String, Long> trims = rt().lastTrimsByName();
        if (trims.isEmpty()) return "none this second";
        final List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Long> e : trims.entrySet()) parts.add(e.getKey() + " " + e.getValue() + " L");
        return String.join(", ", parts);
    }

    private static EnumChatFormatting zoneColour(int zone) {
        switch (zone) {
            case 0:
                return EnumChatFormatting.GREEN;
            case 1:
                return EnumChatFormatting.AQUA;
            case 2:
                return EnumChatFormatting.YELLOW;
            case 3:
                return EnumChatFormatting.GOLD;
            default:
                return EnumChatFormatting.RED;
        }
    }

    // ================================================================================ terminal

    private static IWidget row(StringSyncValue v) {
        return IKey.dynamic(v::getStringValue)
            .asWidget()
            .fullWidth()
            .marginBottom(2)
            .setEnabledIf(
                w -> !v.getStringValue()
                    .isEmpty());
    }

    private static IWidget row(Supplier<String> text, Supplier<Boolean> shown) {
        return IKey.dynamic(text::get)
            .asWidget()
            .fullWidth()
            .marginBottom(2)
            .setEnabledIf(w -> shown.get());
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue head = syncManager.findSyncHandler("eyeHead", StringSyncValue.class);
        final StringSyncValue star = syncManager.findSyncHandler("eyeStar", StringSyncValue.class);
        final StringSyncValue field = syncManager.findSyncHandler("eyeField", StringSyncValue.class);
        final StringSyncValue warn = syncManager.findSyncHandler("eyeWarn", StringSyncValue.class);
        final StringSyncValue upkeep = syncManager.findSyncHandler("eyeUpkeep", StringSyncValue.class);
        final StringSyncValue dial = syncManager.findSyncHandler("eyeDialLine", StringSyncValue.class);
        final StringSyncValue trans = syncManager.findSyncHandler("eyeTrans", StringSyncValue.class);
        final DoubleSyncValue transP = syncManager.findSyncHandler("eyeTransP", DoubleSyncValue.class);
        final StringSyncValue milestone = syncManager.findSyncHandler("eyeMilestone", StringSyncValue.class);
        final StringSyncValue fail = syncManager.findSyncHandler("eyeFail", StringSyncValue.class);
        final StringSyncValue renderNote = syncManager.findSyncHandler("eyeRenderNote", StringSyncValue.class);
        final IntSyncValue mask = syncManager.findSyncHandler("eyeGaugeMask", IntSyncValue.class);
        final DoubleSyncValue sp = syncManager.findSyncHandler("eyeSp", DoubleSyncValue.class);

        final ListWidget<IWidget, ?> list = super.createTerminalTextWidget(syncManager, parent).child(row(head))
            .child(row(star));
        for (int k = 0; k < 4; k++) {
            final int gauge = k;
            final DoubleSyncValue v = syncManager.findSyncHandler("eyeGauge" + k, DoubleSyncValue.class);
            list.child(
                IKey.dynamic(
                    () -> EnumChatFormatting.WHITE + " "
                        + StellarField.GAUGE_SYM[gauge]
                        + " "
                        + StellarField.GAUGE_NAME[gauge]
                        + " "
                        + StellarField.fmtSigned(v.getDoubleValue())
                        + (gauge == StellarField.TH && sp.getDoubleValue() > NO_SP + 1
                            ? " (setpoint " + StellarField.fmtSigned(sp.getDoubleValue()) + ")"
                            : ""))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(gaugeBar(v, gauge == StellarField.TH ? sp : null))
                    .marginBottom(2)
                    .setEnabledIf(w -> (mask.getIntValue() & (1 << gauge)) != 0));
        }
        return list.child(row(field))
            .child(row(warn))
            .child(row(upkeep))
            .child(row(dial))
            .child(
                row(() -> EnumChatFormatting.LIGHT_PURPLE + trans.getStringValue(), () -> transP.getDoubleValue() >= 0))
            .child(
                IKey.dynamic(() -> String.format(Locale.ROOT, "%.0f%%", Math.max(0, transP.getDoubleValue()) * 100))
                    .asWidget()
                    .textAlign(Alignment.Center)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(progressBar(transP))
                    .marginBottom(3)
                    .setEnabledIf(w -> transP.getDoubleValue() >= 0))
            .child(
                row(
                    () -> EnumChatFormatting.GRAY + "Milestones: " + milestone.getStringValue(),
                    () -> !milestone.getStringValue()
                        .isEmpty()))
            .child(
                row(
                    () -> EnumChatFormatting.RED + "Last failure: " + fail.getStringValue(),
                    () -> !fail.getStringValue()
                        .isEmpty()))
            .child(
                row(
                    () -> EnumChatFormatting.GOLD + renderNote.getStringValue(),
                    () -> !renderNote.getStringValue()
                        .isEmpty()));
    }

    /**
     * A gauge: −120..+120 across the bar, the zone bands drawn around the setpoint (0, or the T5 cooling setpoint),
     * a white marker at the value, a yellow tick at a moving setpoint.
     */
    private static IDrawable gaugeBar(DoubleSyncValue value, DoubleSyncValue setpoint) {
        return (context, x, y, width, height, theme) -> {
            final float inner = width - 2;
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, inner, height - 2, ZONE_COLOUR[4]);
            final double centre = setpoint != null && setpoint.getDoubleValue() > NO_SP + 1 ? setpoint.getDoubleValue()
                : 0;
            for (int z = 3; z >= 0; z--) {
                final double lo = Math.max(-120, centre - ZONE_EDGE[z]);
                final double hi = Math.min(120, centre + ZONE_EDGE[z]);
                if (hi <= lo) continue;
                final float x0 = (float) (x + 1 + (lo + 120) / 240 * inner);
                final float x1 = (float) (x + 1 + (hi + 120) / 240 * inner);
                GuiDraw.drawRect(x0, y + 1, x1 - x0, height - 2, ZONE_COLOUR[z]);
            }
            if (setpoint != null && setpoint.getDoubleValue() > NO_SP + 1) {
                GuiDraw.drawRect((float) (x + 1 + (centre + 120) / 240 * inner) - 0.5f, y, 1, height, SETPOINT_TICK);
            }
            final double v = Math.max(-120, Math.min(120, value.getDoubleValue()));
            GuiDraw.drawRect((float) (x + 1 + (v + 120) / 240 * inner) - 1f, y, 2, height, MARKER);
        };
    }

    private static IDrawable progressBar(DoubleSyncValue progress) {
        return (context, x, y, width, height, theme) -> {
            final double f = Math.max(0, Math.min(1, progress.getDoubleValue()));
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, BAR_TRACK);
            final float fill = (float) ((width - 2) * f);
            if (fill > 0)
                GuiDraw.drawHorizontalGradientRect(x + 1, y + 1, fill, height - 2, PROGRESS_FROM, PROGRESS_TO);
        };
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

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final DoubleSyncValue dial = syncManager.findSyncHandler("eyeDial", DoubleSyncValue.class);
        final DoubleSyncValue feed = syncManager.findSyncHandler("eyeFeed", DoubleSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.str("Dial")
                    .asWidget()
                    .marginRight(1)
                    .heightRel(1f))
            .child(
                stepButton(
                    dial,
                    -EyeStarRuntime.DIAL_STEP,
                    1,
                    EyeStarRuntime.DIAL_MAX,
                    "Time dial -0.25 (right-click: -1.00)",
                    true))
            .child(
                IKey.dynamic(() -> String.format(Locale.ROOT, "%.2f", dial.getDoubleValue()))
                    .asWidget()
                    .width(22)
                    .textAlign(Alignment.Center)
                    .heightRel(1f)
                    .tooltip(
                        t -> t.addLine("Time dial setpoint, 1.00 - 3.00")
                            .addLine(EnumChatFormatting.GRAY + "Each +0.25 runs the star faster and costs")
                            .addLine(EnumChatFormatting.GRAY + "10 x 2^(N-1) L/s of Tachyon Rich Temporal Fluid")
                            .addLine(EnumChatFormatting.GRAY + "from the Field sockets. Capped by the Time")
                            .addLine(EnumChatFormatting.GRAY + "Dilation generators: 1 + 0.25 x (tier - N)."))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                stepButton(
                    dial,
                    EyeStarRuntime.DIAL_STEP,
                    1,
                    EyeStarRuntime.DIAL_MAX,
                    "Time dial +0.25 (right-click: +1.00)",
                    true))
            .child(
                IKey.str("Feed")
                    .asWidget()
                    .marginLeft(3)
                    .marginRight(1)
                    .heightRel(1f))
            .child(
                stepButton(
                    feed,
                    -EyeStarRuntime.FEED_STEP,
                    0,
                    EyeStarRuntime.FEED_MAX,
                    "HSM feed -0.1 L/s (right-click: -0.4)",
                    false))
            .child(
                IKey.dynamic(() -> String.format(Locale.ROOT, "%.2f", feed.getDoubleValue()))
                    .asWidget()
                    .width(22)
                    .textAlign(Alignment.Center)
                    .heightRel(1f)
                    .tooltip(
                        t -> t.addLine("T4 neutron star: HSM feed setpoint, L/s per parallel (0 - 1.5)")
                            .addLine(EnumChatFormatting.GRAY + "Each litre of Hyperdense Stellar Mixture fed makes")
                            .addLine(EnumChatFormatting.GRAY + "3.6864 L of White Dwarf Matter x mean Y x r.")
                            .addLine(EnumChatFormatting.GRAY + "Other stars ignore it."))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                stepButton(
                    feed,
                    EyeStarRuntime.FEED_STEP,
                    0,
                    EyeStarRuntime.FEED_MAX,
                    "HSM feed +0.1 L/s (right-click: +0.4)",
                    false));
    }

    /** − / + for a setpoint: one step, four on right-click, clamped here and again on the server. */
    private static IWidget stepButton(DoubleSyncValue value, double step, double min, double max, String tooltip,
        boolean wraps) {
        return new ButtonWidget<>().size(9, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final double raw = value.getDoubleValue() + (button == 1 ? 4 * step : step);
                final double next = Math.round(Math.max(min, Math.min(max, raw)) * 1000) / 1000.0;
                if (Math.abs(next - value.getDoubleValue()) > 1e-9) value.setDoubleValue(next);
                return true;
            })
            .tooltip(
                t -> t.addLine(tooltip)
                    .addLine(
                        EnumChatFormatting.GRAY
                            + (wraps ? "The screwdriver steps it too." : "Sneak-screwdriver steps it too.")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Right (GT lays this row out reversed: the last child is leftmost). */
    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler fieldPanel = syncManager
            .syncedPanel("eyeFieldPanel", true, (sub, handler) -> buildFieldPanel(syncManager, parent));
        final BooleanSyncValue render = syncManager.findSyncHandler("eyeRender", BooleanSyncValue.class);
        return super.createRightPanelGapRow(parent, syncManager).child(
            new ToggleButton().size(18, 18)
                .marginLeft(2)
                .value(render)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SHOW_WORK_AREA)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_WORK_AREA)
                .tooltipBuilder(
                    t -> t.addLine("Centre render on / off (this Eye)")
                        .addLine(EnumChatFormatting.GRAY + "The star, neutron star, dwarf or magnetar at the")
                        .addLine(EnumChatFormatting.GRAY + "Eye's centre. The config (eyeRenderCentre) can")
                        .addLine(
                            EnumChatFormatting.GRAY + "turn it off everywhere"
                                + (Config.eyeRenderCentre ? "." : ": it is OFF in your config.")))
                .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                new ButtonWidget<>().size(18, 18)
                    .marginLeft(2)
                    .overlay(GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS)
                    .onMousePressed(button -> {
                        if (fieldPanel.isPanelOpen()) fieldPanel.closePanel();
                        else fieldPanel.openPanel();
                        return true;
                    })
                    .tooltip(t -> t.addLine("Field: zones, sockets, sensors, trims, the standard rig"))
                    .tooltipShowUpTimer(TOOLTIP_DELAY));
    }

    // ============================================================================== field panel

    private ModularPanel buildFieldPanel(PanelSyncManager main, ModularPanel parent) {
        final StringSyncValue sockets = main.findSyncHandler("eyeSockets", StringSyncValue.class);
        final StringSyncValue trims = main.findSyncHandler("eyeTrims", StringSyncValue.class);
        final int w = FIELD_PANEL_W - 8;
        final Flow legend = Flow.column()
            .width(w)
            .coverChildrenHeight()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(1);
        final String[] ranges = { "|error| <= 10", "<= 40", "<= 70", "<= 100", "> 100" };
        for (int z = 0; z < 5; z++) {
            final int colour = ZONE_COLOUR[z];
            legend.child(
                Flow.row()
                    .width(w)
                    .height(8)
                    .childPadding(3)
                    .child(
                        ((IDrawable) (context, x, y, width, height, theme) -> GuiDraw
                            .drawRect(x, y, width, height, colour)).asWidget()
                                .size(14, 7))
                    .child(
                        IKey.str(StellarField.ZONE_NAME[z] + "  " + ranges[z])
                            .asWidget()
                            .scale(0.75f)));
        }
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Stability field")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(legend)
            .child(
                IKey.dynamic(sockets::getStringValue)
                    .asWidget()
                    .scale(0.75f)
                    .maxWidth(w))
            .child(
                IKey.dynamic(() -> "Trims: " + trims.getStringValue())
                    .asWidget()
                    .scale(0.75f)
                    .maxWidth(w))
            .child(
                IKey.str(
                    EnumChatFormatting.DARK_GRAY
                        + "Rig: a Sensor past its threshold -> redstone -> a Fluid Regulator cover or an export bus"
                        + " with a redstone card -> a plain input hatch in a Field socket. Two sensors per two-sided"
                        + " gauge; the warning channels open a bigger valve.")
                    .asWidget()
                    .scale(0.75f)
                    .maxWidth(w));
        return new ModularPanel("eyeFieldPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(FIELD_PANEL_W, FIELD_PANEL_H)
            .child(column);
    }
}
