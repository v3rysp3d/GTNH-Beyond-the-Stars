package com.nateyburr.beyondthestars.machines.kerr;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * KERR REDESIGN: the Kerr Ergosphere Engine's console (docs/kerr-redesign.md §4.1 and the GUI
 * mockup), GT's standard MUI2 multiblock layout. Nothing a player needs takes a screwdriver.
 * <ul>
 * <li><b>Terminal:</b> hole state; a mass bar (mass over the tier's ceiling, the setpoint as a
 * yellow tick, "16.0 / 64 SM" on it); setpoint and bleed; governor (bolts per tick against the rail
 * cap); bolt stock; stability (and time to evaporation); containment power; the modules.</li>
 * <li><b>Gap row:</b> the setpoint (a number field with - / +) on the left; the Hole control and
 * Modules side panels and the render toggle on the right (no power panel, B-52).</li>
 * <li><b>Hole control panel:</b> a setpoint slider over the tier's range with its scale, the number
 * field mirrored, the tier, the Seed and Collapser counts, Open (needs a Seed in a bolt bus) and
 * Collapse (needs a Collapser; two clicks within a few seconds, the first warns how many modules
 * will pause) and the Auto check box (a Seed or Collapser dropped into a bolt bus acts on its own,
 * so automation keeps working).</li>
 * <li><b>Modules panel:</b> one row per slot: the module's icon, name, state, bonus or reason, and
 * coordinates. Read-only: modules are run from their own GUIs.</li>
 * </ul>
 * Every value is server state and comes across as a sync value; every setter is validated on the
 * server ({@link MTEKerrEngine#setSetpoint} clamps to the tier, the hole actions re-check the hole
 * and the bolt buses). The client-side clamps and checks here only keep the display honest. The two
 * hole actions are server-only synced actions registered on the MAIN panel's manager (the side
 * panels call them; the BTSUpgradeTreePanel pattern). Only one side panel is open at a time: they
 * share the spot to the right of the main panel.
 */
public class MTEKerrEngineGui extends MTEMultiBlockBaseGui<MTEKerrEngine> {

    /** The mass bar: track, border, the BHC-lavender fill gradient, the setpoint tick. */
    private static final int BAR_TRACK = 0xFF2A2D33;
    private static final int BAR_EDGE = 0xFF4A4D55;
    private static final int BAR_FILL_FROM = 0xFF4B2F8F;
    private static final int BAR_FILL_TO = 0xFF9A7CFF;
    private static final int BAR_TICK = 0xFFFFFF55;

    /** Collapse stays armed this long after its first click. */
    private static final long COLLAPSE_ARM_MS = 4000L;

    private static final int HOLE_PANEL_W = 150;
    private static final int HOLE_PANEL_H = 176;
    private static final int MODULES_PANEL_W = 172;
    private static final int MODULE_ROW_H = 28;

    public MTEKerrEngineGui(MTEKerrEngine multiblock) {
        super(multiblock);
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("kerrFormed", new BooleanSyncValue(multiblock::isFormed));
        syncManager.syncValue("kerrOpen", new BooleanSyncValue(multiblock::isHoleOpen));
        syncManager.syncValue("kerrMass", new DoubleSyncValue(multiblock::getMass));
        // Settable from the client; MTEKerrEngine.setSetpoint clamps to the tier's range on the server.
        syncManager
            .syncValue("kerrSetpoint", new IntSyncValue(multiblock::getSetpoint, multiblock::setSetpoint).allowC2S());
        syncManager.syncValue("kerrMaxSetpoint", new IntSyncValue(multiblock::maxSetpoint));
        syncManager.syncValue("kerrBleed", new DoubleSyncValue(multiblock::getBleed));
        syncManager.syncValue("kerrBoltsPerTick", new DoubleSyncValue(multiblock::getBoltsPerTick));
        syncManager.syncValue("kerrBoltCap", new IntSyncValue(multiblock::boltCap));
        syncManager.syncValue("kerrRailMk", new IntSyncValue(multiblock::railTier));
        syncManager.syncValue("kerrStockSm", new DoubleSyncValue(multiblock::getBoltStockSm));
        syncManager.syncValue("kerrStockSeconds", new LongSyncValue(multiblock::getBoltStockSeconds));
        syncManager.syncValue("kerrStability", new DoubleSyncValue(multiblock::getStability));
        syncManager.syncValue("kerrEvapSeconds", new LongSyncValue(multiblock::getEvapSeconds));
        syncManager.syncValue("kerrContainment", new LongSyncValue(multiblock::getContainmentEut));
        syncManager.syncValue("kerrPowerOk", new BooleanSyncValue(multiblock::isPowerOk));
        syncManager.syncValue("kerrDensity", new BooleanSyncValue(multiblock::isDensityInstalled));
        syncManager.syncValue("kerrSeeds", new IntSyncValue(multiblock::getSeeds));
        syncManager.syncValue("kerrCollapsers", new IntSyncValue(multiblock::getCollapsers));
        syncManager
            .syncValue("kerrAuto", new BooleanSyncValue(multiblock::isAutoSeeds, multiblock::setAutoSeeds).allowC2S());
        syncManager.syncValue(
            "kerrRender",
            new BooleanSyncValue(multiblock::isRenderEnabled, multiblock::setRenderEnabled).allowC2S());
        syncManager.syncValue("kerrLinked", new IntSyncValue(multiblock::getLinkedModules));
        syncManager.syncValue("kerrBurst", new StringSyncValue(multiblock::burstGuiLine));
        syncManager.syncValue("kerrMigration", new StringSyncValue(multiblock::getMigrationNote));
        syncManager.syncValue("kerrRenderNote", new StringSyncValue(multiblock::getRenderNote));
        for (KerrSlot slot : KerrSlot.VALUES) {
            syncManager.syncValue("kerrSlot." + slot.key, new StringSyncValue(() -> multiblock.slotStatus(slot)));
        }
        // Server-only actions (executeClient = false): the server re-checks every condition.
        syncManager.registerServerSyncedAction("kerrOpenHole", packet -> multiblock.openHoleFromGui());
        syncManager.registerServerSyncedAction("kerrCollapseHole", packet -> multiblock.collapseHoleFromGui());
    }

    /** A terminal line; wraps inside the terminal instead of running off its edge. */
    private static IWidget row(Supplier<String> text) {
        return IKey.dynamic(text::get)
            .asWidget()
            .fullWidth()
            .marginBottom(2);
    }

    // ================================================================================ terminal

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final BooleanSyncValue formed = syncManager.findSyncHandler("kerrFormed", BooleanSyncValue.class);
        final BooleanSyncValue open = syncManager.findSyncHandler("kerrOpen", BooleanSyncValue.class);
        final DoubleSyncValue mass = syncManager.findSyncHandler("kerrMass", DoubleSyncValue.class);
        final IntSyncValue setpoint = syncManager.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        final IntSyncValue maxSetpoint = syncManager.findSyncHandler("kerrMaxSetpoint", IntSyncValue.class);
        final DoubleSyncValue bleed = syncManager.findSyncHandler("kerrBleed", DoubleSyncValue.class);
        final DoubleSyncValue bpt = syncManager.findSyncHandler("kerrBoltsPerTick", DoubleSyncValue.class);
        final IntSyncValue cap = syncManager.findSyncHandler("kerrBoltCap", IntSyncValue.class);
        final IntSyncValue rail = syncManager.findSyncHandler("kerrRailMk", IntSyncValue.class);
        final DoubleSyncValue stockSm = syncManager.findSyncHandler("kerrStockSm", DoubleSyncValue.class);
        final LongSyncValue stockSec = syncManager.findSyncHandler("kerrStockSeconds", LongSyncValue.class);
        final DoubleSyncValue stability = syncManager.findSyncHandler("kerrStability", DoubleSyncValue.class);
        final LongSyncValue evap = syncManager.findSyncHandler("kerrEvapSeconds", LongSyncValue.class);
        final LongSyncValue containment = syncManager.findSyncHandler("kerrContainment", LongSyncValue.class);
        final BooleanSyncValue powerOk = syncManager.findSyncHandler("kerrPowerOk", BooleanSyncValue.class);
        final BooleanSyncValue density = syncManager.findSyncHandler("kerrDensity", BooleanSyncValue.class);
        final StringSyncValue migration = syncManager.findSyncHandler("kerrMigration", StringSyncValue.class);
        final StringSyncValue renderNote = syncManager.findSyncHandler("kerrRenderNote", StringSyncValue.class);
        final StringSyncValue burst = syncManager.findSyncHandler("kerrBurst", StringSyncValue.class);
        final StringSyncValue[] slots = slotValues(syncManager);

        return super.createTerminalTextWidget(syncManager, parent)
            .child(row(() -> holeLine(formed.getBoolValue(), open.getBoolValue(), density.getBoolValue())))
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.WHITE + fmt1(mass.getDoubleValue())
                        + " / "
                        + maxSetpoint.getIntValue()
                        + " SM ")
                    .asWidget()
                    .textAlign(Alignment.CenterRight)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(massBar(mass, setpoint, maxSetpoint))
                    .marginBottom(3)
                    .setEnabledIf(w -> open.getBoolValue()))
            .child(
                row(
                    () -> EnumChatFormatting.WHITE + "Setpoint "
                        + setpoint.getIntValue()
                        + " SM"
                        + EnumChatFormatting.GRAY
                        + (open.getBoolValue()
                            ? ", bleed " + String.format(Locale.ROOT, "%.4f", bleed.getDoubleValue()) + " SM/t"
                            : " (a new hole opens at 8 SM)")))
            .child(
                row(
                    () -> EnumChatFormatting.GRAY + "Governor "
                        + EnumChatFormatting.WHITE
                        + String.format(Locale.ROOT, "%.2f", bpt.getDoubleValue())
                        + EnumChatFormatting.GRAY
                        + " / "
                        + cap.getIntValue()
                        + " bolts/t, Mk "
                        + roman(rail.getIntValue())
                        + " rails"))
            .child(
                row(
                    () -> EnumChatFormatting.GRAY + "Bolt stock "
                        + EnumChatFormatting.WHITE
                        + MTEKerrEngine.fmt(stockSm.getDoubleValue())
                        + " SM"
                        + EnumChatFormatting.GRAY
                        + (stockSec.getLongValue() >= 0 ? ", " + duration(stockSec.getLongValue()) : "")))
            .child(row(() -> stabilityLine(open.getBoolValue(), stability.getDoubleValue(), evap.getLongValue())))
            .child(row(() -> containmentLine(open.getBoolValue(), powerOk.getBoolValue(), containment.getLongValue())))
            .child(row(() -> modulesLine(slots)))
            .child(
                IKey.dynamic(burst::getStringValue)
                    .asWidget()
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !burst.getStringValue()
                            .isEmpty()))
            .child(
                IKey.dynamic(() -> EnumChatFormatting.AQUA + migration.getStringValue())
                    .asWidget()
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !migration.getStringValue()
                            .isEmpty()))
            .child(
                IKey.dynamic(() -> EnumChatFormatting.GOLD + renderNote.getStringValue())
                    .asWidget()
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !renderNote.getStringValue()
                            .isEmpty()));
    }

    private static StringSyncValue[] slotValues(PanelSyncManager syncManager) {
        final StringSyncValue[] out = new StringSyncValue[KerrSlot.VALUES.length];
        for (KerrSlot slot : KerrSlot.VALUES) {
            out[slot.ordinal()] = syncManager.findSyncHandler("kerrSlot." + slot.key, StringSyncValue.class);
        }
        return out;
    }

    /** The mass bar: a lavender fill to M / ceiling, a yellow tick at the setpoint. */
    private static IDrawable massBar(DoubleSyncValue mass, IntSyncValue setpoint, IntSyncValue max) {
        return (context, x, y, width, height, theme) -> {
            final double top = Math.max(1, max.getIntValue());
            final double fill = Math.max(0, Math.min(1, mass.getDoubleValue() / top));
            final double tick = Math.max(0, Math.min(1, setpoint.getIntValue() / top));
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, BAR_TRACK);
            final float fillW = (float) ((width - 2) * fill);
            if (fillW > 0)
                GuiDraw.drawHorizontalGradientRect(x + 1, y + 1, fillW, height - 2, BAR_FILL_FROM, BAR_FILL_TO);
            GuiDraw.drawRect(x + 1 + (float) ((width - 2) * tick) - 0.5f, y, 1, height, BAR_TICK);
        };
    }

    static String holeLine(boolean formed, boolean open, boolean density) {
        final String tier = density ? "Kerr T2" : "Kerr T1";
        if (!formed) return EnumChatFormatting.RED + "Not formed: the hole is frozen (" + tier + ")";
        if (open) return EnumChatFormatting.GREEN + "Hole OPEN" + EnumChatFormatting.GRAY + " - " + tier;
        return EnumChatFormatting.GRAY + "Hole closed - " + tier + ": a Seed in a bolt bus, or Open";
    }

    static String stabilityLine(boolean open, double stability, long evapSeconds) {
        if (!open) return EnumChatFormatting.DARK_GRAY + "Stability: no hole";
        final int pct = (int) Math.round(stability * 100);
        if (stability >= 0.999) {
            return EnumChatFormatting.GRAY + "Stability "
                + EnumChatFormatting.GREEN
                + "100%"
                + EnumChatFormatting.GRAY
                + " (holding)";
        }
        return (stability >= 0.5 ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + "Stability "
            + pct
            + "% (drifting, evaporates in "
            + duration(evapSeconds)
            + " without bolts)";
    }

    static String containmentLine(boolean open, boolean powerOk, long eut) {
        if (!open) return EnumChatFormatting.DARK_GRAY + "Containment: idle (no hole)";
        if (!powerOk) return EnumChatFormatting.RED + "Containment power LOW: governor stopped";
        return EnumChatFormatting.GRAY + "Containment " + EnumChatFormatting.WHITE + compactEu(eut) + " EU/t";
    }

    static String modulesLine(StringSyncValue[] slots) {
        final StringBuilder sb = new StringBuilder(EnumChatFormatting.GRAY + "Modules:");
        for (KerrSlot slot : KerrSlot.VALUES) {
            final String state = field(slots[slot.ordinal()], 0);
            sb.append(slot.ordinal() == 0 ? " " : EnumChatFormatting.GRAY + ", ")
                .append(EnumChatFormatting.WHITE)
                .append(slot.label)
                .append(' ')
                .append(stateColour(state))
                .append(shortState(state));
        }
        return sb.toString();
    }

    /** Terminal (dark background) colours. */
    static EnumChatFormatting stateColour(String state) {
        if (state.startsWith("RUNNING")) return EnumChatFormatting.GREEN;
        if (state.startsWith("IDLE")) return EnumChatFormatting.AQUA;
        if (state.startsWith("PAUSED")) return EnumChatFormatting.GOLD;
        if (state.startsWith("NOT BUILT")) return EnumChatFormatting.DARK_GRAY;
        return EnumChatFormatting.RED;
    }

    /** Side-panel (grey background) colours: the bright terminal ones do not read on GT's grey. */
    static EnumChatFormatting panelColour(String state) {
        if (state.startsWith("RUNNING")) return EnumChatFormatting.DARK_GREEN;
        if (state.startsWith("IDLE")) return EnumChatFormatting.DARK_AQUA;
        if (state.startsWith("PAUSED")) return EnumChatFormatting.GOLD;
        if (state.startsWith("NOT BUILT")) return EnumChatFormatting.DARK_GRAY;
        return EnumChatFormatting.DARK_RED;
    }

    private static String shortState(String state) {
        final int colon = state.indexOf(':');
        return colon > 0 ? state.substring(0, colon) : state;
    }

    static String duration(long seconds) {
        if (seconds < 0) return "-";
        if (seconds < 60) return seconds + " s";
        if (seconds < 3600) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds / 3600 + "h " + seconds / 60 % 60 + "m";
    }

    /** "16.0" below 100, "140" above (the mass bar label). */
    static String fmt1(double v) {
        return v >= 100 ? String.format(Locale.ROOT, "%,.0f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** "8.39M", "1.25G", "512k". */
    static String compactEu(long eu) {
        if (eu >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fG", eu / 1e9);
        if (eu >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", eu / 1e6);
        if (eu >= 10_000L) return String.format(Locale.ROOT, "%.0fk", eu / 1e3);
        return String.format(Locale.ROOT, "%,d", eu);
    }

    private static String roman(int tier) {
        switch (tier) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            default:
                return "-";
        }
    }

    /** The power switch does not stop the hole (B-52): shown greyed out. */
    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
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

    private static int clampSetpoint(int v, IntSyncValue max) {
        return Math
            .max(MTEKerrEngine.SETPOINT_MIN, Math.min(Math.max(MTEKerrEngine.SETPOINT_MIN, max.getIntValue()), v));
    }

    /**
     * The setpoint as a whole number of SM: a number field bounded by the tier's synced range (scroll:
     * 1, shift 8, ctrl 1, alt 64), flanked by - / +. The server clamps again.
     */
    private static TextFieldWidget setpointField(IntSyncValue setpoint, IntSyncValue max) {
        return new TextFieldWidget().formatAsInteger(true)
            .numbersInt(() -> MTEKerrEngine.SETPOINT_MIN, () -> clampSetpoint(Integer.MAX_VALUE, max))
            .value(new IntValue.Dynamic(setpoint::getIntValue, v -> {
                final int clamped = clampSetpoint(v, max);
                if (clamped != setpoint.getIntValue()) setpoint.setIntValue(clamped);
            }))
            .setTextAlignment(Alignment.Center)
            .scrollValues(1, 8, 1, 64)
            .tooltip(
                t -> t.addLine("Hole mass setpoint, SM (whole numbers)")
                    .addLine(EnumChatFormatting.GRAY + "Kerr T1: 2-64 SM. Kerr T2: 2-512 SM.")
                    .addLine(EnumChatFormatting.GRAY + "Scroll: 1, shift: 8, alt: 64."))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue setpoint = syncManager.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        final IntSyncValue max = syncManager.findSyncHandler("kerrMaxSetpoint", IntSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.str("Setpoint")
                    .asWidget()
                    .marginRight(2)
                    .heightRel(1f))
            .child(stepButton(setpoint, max, -1))
            .child(
                setpointField(setpoint, max).width(30)
                    .height(14))
            .child(stepButton(setpoint, max, 1))
            .child(
                IKey.str("SM")
                    .asWidget()
                    .marginLeft(1)
                    .heightRel(1f));
    }

    private static IWidget stepButton(IntSyncValue setpoint, IntSyncValue max, int step) {
        return new ButtonWidget<>().size(10, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final int next = clampSetpoint(setpoint.getIntValue() + (button == 1 ? step * 8 : step), max);
                if (next != setpoint.getIntValue()) setpoint.setIntValue(next);
                return true;
            })
            .tooltip(t -> t.addLine((step < 0 ? "Lower" : "Raise") + " the setpoint by 1 SM (right-click: 8)"))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Right (GT lays this row out reversed: the last child is leftmost). */
    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler[] panels = new IPanelHandler[2];
        panels[0] = syncManager
            .syncedPanel("kerrHolePanel", true, (sub, handler) -> buildHolePanel(syncManager, parent));
        panels[1] = syncManager
            .syncedPanel("kerrModulesPanel", true, (sub, handler) -> buildModulesPanel(syncManager, parent));
        final BooleanSyncValue render = syncManager.findSyncHandler("kerrRender", BooleanSyncValue.class);
        return super.createRightPanelGapRow(parent, syncManager).child(
            new ToggleButton().size(18, 18)
                .marginLeft(2)
                .value(render)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SHOW_WORK_AREA)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_WORK_AREA)
                .tooltipBuilder(
                    t -> t.addLine("Black hole render on / off (this machine)")
                        .addLine(EnumChatFormatting.GRAY + "The config can turn it off everywhere."))
                .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                panelButton(
                    panels,
                    1,
                    GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS,
                    "Modules: each arc's machine, its state and bonus"))
            .child(
                panelButton(
                    panels,
                    0,
                    GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED,
                    "Hole control: setpoint slider, Open, Collapse, Auto"));
    }

    /** Toggles side panel {@code which}; opening it closes the other (they share one spot). */
    private static IWidget panelButton(IPanelHandler[] panels, int which, IDrawable icon, String tooltip) {
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(2)
            .overlay(icon)
            .onMousePressed(button -> {
                final IPanelHandler panel = panels[which];
                if (panel.isPanelOpen()) {
                    panel.closePanel();
                } else {
                    for (int i = 0; i < panels.length; i++) {
                        if (i != which && panels[i].isPanelOpen()) panels[i].closePanel();
                    }
                    panel.openPanel();
                }
                return true;
            })
            .tooltip(t -> t.addLine(tooltip))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ======================================================================= hole control panel

    private static TextWidget<?> panelText(Supplier<String> text, int width, float scale) {
        return IKey.dynamic(text::get)
            .asWidget()
            .scale(scale)
            .maxWidth(width);
    }

    /** "label ......... value" across the panel. */
    private static IWidget keyValue(String label, Supplier<String> value, int width) {
        return Flow.row()
            .width(width)
            .height(9)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(
                IKey.str(label)
                    .asWidget())
            .child(
                IKey.dynamic(value::get)
                    .asWidget());
    }

    private ModularPanel buildHolePanel(PanelSyncManager main, ModularPanel parent) {
        final BooleanSyncValue formed = main.findSyncHandler("kerrFormed", BooleanSyncValue.class);
        final BooleanSyncValue open = main.findSyncHandler("kerrOpen", BooleanSyncValue.class);
        final IntSyncValue setpoint = main.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        final IntSyncValue max = main.findSyncHandler("kerrMaxSetpoint", IntSyncValue.class);
        final BooleanSyncValue density = main.findSyncHandler("kerrDensity", BooleanSyncValue.class);
        final IntSyncValue seeds = main.findSyncHandler("kerrSeeds", IntSyncValue.class);
        final IntSyncValue collapsers = main.findSyncHandler("kerrCollapsers", IntSyncValue.class);
        final BooleanSyncValue auto = main.findSyncHandler("kerrAuto", BooleanSyncValue.class);
        final IntSyncValue linked = main.findSyncHandler("kerrLinked", IntSyncValue.class);
        final int w = HOLE_PANEL_W - 8;
        final int half = (w - 4) / 2;

        // Collapse needs two clicks within COLLAPSE_ARM_MS; the first arms it (client-side only).
        final long[] armedAt = { 0L };
        final BooleanSupplier armed = () -> open.getBoolValue()
            && System.currentTimeMillis() - armedAt[0] < COLLAPSE_ARM_MS;
        final BooleanSupplier canOpen = () -> formed.getBoolValue() && !open.getBoolValue() && seeds.getIntValue() > 0;
        // The server refuses a collapse while the cage is unformed (the hole is frozen), so the button does too.
        final BooleanSupplier canCollapse = () -> formed.getBoolValue() && open.getBoolValue()
            && collapsers.getIntValue() > 0;

        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Hole control")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(
                keyValue(
                    "Setpoint",
                    () -> MTEKerrEngine.SETPOINT_MIN + " - " + clampSetpoint(Integer.MAX_VALUE, max) + " SM",
                    w))
            // The slider runs 0..1 over the tier's CURRENT range (it follows a Density Upgrade going in
            // while the panel is open); it snaps to whole SM and sends only actual changes.
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(() -> toSlider(setpoint.getIntValue(), max), v -> {
                        final int i = fromSlider(v, max);
                        if (i != setpoint.getIntValue()) setpoint.setIntValue(i);
                    }))
                    .size(w, 10))
            .child(
                Flow.row()
                    .width(w)
                    .height(7)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(panelText(() -> String.valueOf(MTEKerrEngine.SETPOINT_MIN), w, 0.75f))
                    .child(panelText(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max) / 4), w, 0.75f))
                    .child(panelText(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max) / 2), w, 0.75f))
                    .child(panelText(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max)), w, 0.75f)))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .mainAxisAlignment(Alignment.MainAxis.CENTER)
                    .child(
                        setpointField(setpoint, max).width(40)
                            .height(14))
                    .child(
                        IKey.str("SM")
                            .asWidget()
                            .heightRel(1f)))
            .child(
                panelText(
                    () -> density.getBoolValue()
                        ? EnumChatFormatting.DARK_AQUA + "Kerr T2: Density Upgrade installed, 2-512 SM."
                        : "Kerr T1. Right-click the controller with a Kerr Density Upgrade for 2-512 SM.",
                    w,
                    0.75f))
            .child(keyValue("Seeds in bolt buses", () -> String.valueOf(seeds.getIntValue()), w))
            .child(keyValue("Collapsers", () -> String.valueOf(collapsers.getIntValue()), w))
            .child(
                Flow.row()
                    .width(w)
                    .height(16)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(
                        new ButtonWidget<>().size(half, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> (canOpen.getAsBoolean() ? EnumChatFormatting.WHITE
                                        : EnumChatFormatting.DARK_GRAY) + "Open hole"))
                            .onMousePressed(button -> {
                                if (canOpen.getAsBoolean()) main.callSyncedAction("kerrOpenHole", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Consumes one Black Hole Seed from a bolt bus")
                                    .addLine(EnumChatFormatting.GRAY + "The hole opens at 8 SM. Needs a formed cage.")))
                    .child(
                        new ButtonWidget<>().size(half, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (armed.getAsBoolean()) return EnumChatFormatting.RED + "Confirm";
                                return (canCollapse.getAsBoolean() ? EnumChatFormatting.WHITE
                                    : EnumChatFormatting.DARK_GRAY) + "Collapse";
                            }))
                            .onMousePressed(button -> {
                                if (!canCollapse.getAsBoolean()) {
                                    armedAt[0] = 0L;
                                } else if (!armed.getAsBoolean()) {
                                    armedAt[0] = System.currentTimeMillis();
                                } else {
                                    armedAt[0] = 0L;
                                    main.callSyncedAction("kerrCollapseHole", packet -> {});
                                }
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Consumes one Black Hole Collapser from a bolt bus")
                                    .addLine(EnumChatFormatting.GRAY + "Click twice. Linked modules pause and keep")
                                    .addLine(EnumChatFormatting.GRAY + "their recipes until the hole reopens."))))
            .child(panelText(() -> {
                final int n = linked.getIntValue();
                final String modules = n + " linked module" + (n == 1 ? "" : "s");
                if (armed.getAsBoolean()) {
                    return EnumChatFormatting.DARK_RED + "Click Collapse again: " + modules + " will pause.";
                }
                if (!open.getBoolValue()) return EnumChatFormatting.DARK_GRAY + "Open needs a Seed in a bolt bus.";
                if (!formed.getBoolValue()) return EnumChatFormatting.YELLOW + "Collapse needs a formed cage.";
                if (auto.getBoolValue()) {
                    return EnumChatFormatting.YELLOW + "Auto on: a Collapser in a bolt bus collapses at once.";
                }
                return "Collapse asks twice: " + modules + " will pause.";
            }, w, 0.75f))
            .child(
                Flow.row()
                    .width(w)
                    .height(12)
                    .childPadding(3)
                    .child(
                        new ToggleButton().size(12, 12)
                            .value(new BoolValue.Dynamic(auto::getBoolValue, auto::setBoolValue))
                            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                            .tooltip(
                                t -> t.addLine("Auto ON: a Seed or Collapser placed in a bolt bus")
                                    .addLine("acts on its own (automation keeps working): a Collapser")
                                    .addLine("collapses the hole within a second, without asking.")
                                    .addLine(EnumChatFormatting.GRAY + "OFF: only these buttons open and close."))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(panelText(() -> "Auto: act on Seeds / Collapsers", w - 15, 0.75f).heightRel(1f)));

        return new ModularPanel("kerrHolePanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(HOLE_PANEL_W, HOLE_PANEL_H)
            .child(column);
    }

    /** Setpoint to the slider's 0..1 over the tier's current range. */
    static double toSlider(int setpoint, IntSyncValue max) {
        final int top = clampSetpoint(Integer.MAX_VALUE, max);
        if (top <= MTEKerrEngine.SETPOINT_MIN) return 0;
        return Math
            .max(0, Math.min(1, (setpoint - MTEKerrEngine.SETPOINT_MIN) / (double) (top - MTEKerrEngine.SETPOINT_MIN)));
    }

    /** The slider's 0..1 back to a whole setpoint in the tier's current range. */
    static int fromSlider(double v, IntSyncValue max) {
        final int top = clampSetpoint(Integer.MAX_VALUE, max);
        return clampSetpoint(
            (int) Math
                .round(MTEKerrEngine.SETPOINT_MIN + Math.max(0, Math.min(1, v)) * (top - MTEKerrEngine.SETPOINT_MIN)),
            max);
    }

    // =========================================================================== modules panel

    private ModularPanel buildModulesPanel(PanelSyncManager main, ModularPanel parent) {
        final StringSyncValue[] slots = slotValues(main);
        final int w = MODULES_PANEL_W - 8;
        final int textW = w - 21;
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Modules")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center));
        for (KerrSlot slot : KerrSlot.VALUES) {
            final StringSyncValue v = slots[slot.ordinal()];
            final ItemStack icon = slot.controllerStack();
            column.child(
                Flow.row()
                    .width(w)
                    .height(MODULE_ROW_H)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        (icon != null ? new ItemDrawable(icon) : IDrawable.EMPTY).asWidget()
                            .size(18))
                    .child(
                        Flow.column()
                            .width(textW)
                            .heightRel(1f)
                            .crossAxisAlignment(Alignment.CrossAxis.START)
                            .mainAxisAlignment(Alignment.MainAxis.CENTER)
                            .childPadding(1)
                            .child(panelText(() -> {
                                final String state = field(v, 0);
                                return slot.label + " " + panelColour(state) + shortState(state);
                            }, textW, 1f))
                            .child(panelText(() -> moduleDetail(slot, v), textW, 0.75f))
                            .child(panelText(() -> {
                                final String pos = field(v, 3);
                                return EnumChatFormatting.DARK_GRAY + (pos.isEmpty() ? "" : "at " + pos);
                            }, textW, 0.75f))));
        }
        column.child(
            panelText(() -> EnumChatFormatting.DARK_GRAY + "Run each module from its own controller.", w, 0.75f));
        return new ModularPanel("kerrModulesPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(MODULES_PANEL_W, 34 + (MODULE_ROW_H + 2) * KerrSlot.VALUES.length)
            .child(column);
    }

    /** The second line of a module row: its bonus when linked, else why it is not running. */
    static String moduleDetail(KerrSlot slot, StringSyncValue v) {
        final String state = field(v, 0);
        if (state.startsWith("RUNNING") || state.startsWith("IDLE")) {
            return "parallel " + field(v, 1) + ", speed x" + field(v, 2);
        }
        if (state.startsWith("PAUSED")) {
            final int colon = state.indexOf(':');
            return colon > 0 ? state.substring(colon + 1)
                .trim() : "paused";
        }
        if (state.startsWith("NOT BUILT")) return "place its controller at marker " + slot.hintDots;
        if (state.startsWith("NOT FORMED")) return "see its own controller's GUI";
        if (state.startsWith("NOT LOADED")) return "its chunk is not loaded";
        return "not linked: see its own GUI";
    }

    private static String field(StringSyncValue v, int i) {
        final String[] f = v.getStringValue()
            .split("\\|", -1);
        return i < f.length ? f[i] : "";
    }
}
