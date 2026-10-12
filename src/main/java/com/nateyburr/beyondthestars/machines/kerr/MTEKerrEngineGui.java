package com.nateyburr.beyondthestars.machines.kerr;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * KERR REDESIGN: the Kerr Ergosphere Engine's console (docs/kerr-redesign.md §4.1), GT's standard
 * MUI2 multiblock layout. Nothing a player needs takes a screwdriver.
 * <ul>
 * <li><b>Terminal:</b> hole state, a mass bar with the setpoint tick, setpoint and bleed, governor
 * (bolts per tick against the rail cap), bolt stock, stability (and time to evaporation), containment
 * power, and the modules.</li>
 * <li><b>Gap row:</b> the setpoint (a number field with - / +) on the left; the Hole control and
 * Modules side panels and the render toggle on the right.</li>
 * <li><b>Hole control panel:</b> a setpoint slider, Open (needs a Seed in a bolt bus), Collapse (needs
 * a Collapser; two clicks, the first warns how many modules will pause) and the Auto toggle (a Seed or
 * Collapser dropped into a bolt bus acts on its own, so automation keeps working).</li>
 * <li><b>Modules panel:</b> each slot's state, bonus and coordinates. Read-only: modules are run from
 * their own GUIs.</li>
 * </ul>
 * Every value is server state and comes across as a sync value; setters clamp on the server. The two
 * hole actions are server-only synced actions registered on the MAIN panel's manager (the side
 * panels call them; the BTSUpgradeTreePanel pattern).
 */
public class MTEKerrEngineGui extends MTEMultiBlockBaseGui<MTEKerrEngine> {

    private static final int GREEN = 0xFF3FD06A;
    private static final int AMBER = 0xFFE0A030;
    private static final int RED = 0xFFE04040;

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
        syncManager.syncValue(
            "kerrEnabled",
            new BooleanSyncValue(
                () -> multiblock.getBaseMetaTileEntity()
                    .isAllowedToWork()));
        syncManager.syncValue("kerrDensity", new BooleanSyncValue(multiblock::isDensityInstalled));
        syncManager.syncValue("kerrSeeds", new IntSyncValue(multiblock::getSeeds));
        syncManager.syncValue("kerrCollapsers", new IntSyncValue(multiblock::getCollapsers));
        syncManager
            .syncValue("kerrAuto", new BooleanSyncValue(multiblock::isAutoSeeds, multiblock::setAutoSeeds).allowC2S());
        syncManager.syncValue(
            "kerrRender",
            new BooleanSyncValue(multiblock::isRenderEnabled, multiblock::setRenderEnabled).allowC2S());
        syncManager.syncValue("kerrLinked", new IntSyncValue(multiblock::getLinkedModules));
        syncManager.syncValue("kerrMigration", new StringSyncValue(multiblock::getMigrationNote));
        for (KerrSlot slot : KerrSlot.VALUES) {
            syncManager.syncValue("kerrSlot." + slot.key, new StringSyncValue(() -> multiblock.slotStatus(slot)));
        }
        // Server-only actions (executeClient = false): the server re-checks every condition.
        syncManager.registerServerSyncedAction("kerrOpenHole", packet -> multiblock.openHoleFromGui());
        syncManager.registerServerSyncedAction("kerrCollapseHole", packet -> multiblock.collapseHoleFromGui());
    }

    private static IWidget row(Supplier<String> text) {
        return IKey.dynamic(text::get)
            .asWidget()
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
        final BooleanSyncValue enabled = syncManager.findSyncHandler("kerrEnabled", BooleanSyncValue.class);
        final BooleanSyncValue density = syncManager.findSyncHandler("kerrDensity", BooleanSyncValue.class);
        final StringSyncValue migration = syncManager.findSyncHandler("kerrMigration", StringSyncValue.class);
        final StringSyncValue[] slots = slotValues(syncManager);

        return super.createTerminalTextWidget(syncManager, parent)
            .child(row(() -> holeLine(formed.getBoolValue(), open.getBoolValue(), density.getBoolValue())))
            .child(
                massBar(mass, setpoint, maxSetpoint, stability).asWidget()
                    .size(150, 5)
                    .marginBottom(2)
                    .setEnabledIf(w -> open.getBoolValue()))
            .child(
                row(
                    () -> EnumChatFormatting.WHITE + "Mass "
                        + MTEKerrEngine.fmt(mass.getDoubleValue())
                        + " / "
                        + maxSetpoint.getIntValue()
                        + " SM, setpoint "
                        + setpoint.getIntValue()
                        + (open.getBoolValue()
                            ? ", bleed " + String.format(Locale.ROOT, "%.4f", bleed.getDoubleValue()) + " SM/t"
                            : "")))
            .child(
                row(
                    () -> EnumChatFormatting.GRAY + "Governor "
                        + String.format(Locale.ROOT, "%.2f", bpt.getDoubleValue())
                        + " / "
                        + cap.getIntValue()
                        + " bolts/t (Mk "
                        + roman(rail.getIntValue())
                        + " rails)"))
            .child(
                row(
                    () -> EnumChatFormatting.GRAY + "Bolt stock "
                        + MTEKerrEngine.fmt(stockSm.getDoubleValue())
                        + " SM"
                        + (stockSec.getLongValue() >= 0 ? ", " + duration(stockSec.getLongValue()) : "")))
            .child(row(() -> stabilityLine(open.getBoolValue(), stability.getDoubleValue(), evap.getLongValue())))
            .child(
                row(
                    () -> containmentLine(
                        open.getBoolValue(),
                        enabled.getBoolValue(),
                        powerOk.getBoolValue(),
                        containment.getLongValue())))
            .child(row(() -> modulesLine(slots)))
            .child(
                IKey.dynamic(() -> EnumChatFormatting.AQUA + migration.getStringValue())
                    .asWidget()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !migration.getStringValue()
                            .isEmpty()));
    }

    private static StringSyncValue[] slotValues(PanelSyncManager syncManager) {
        final StringSyncValue[] out = new StringSyncValue[KerrSlot.VALUES.length];
        for (KerrSlot slot : KerrSlot.VALUES) {
            out[slot.ordinal()] = syncManager.findSyncHandler("kerrSlot." + slot.key, StringSyncValue.class);
        }
        return out;
    }

    /** The mass bar: fill to M / max, a white tick at the setpoint, coloured by stability. */
    private static IDrawable massBar(DoubleSyncValue mass, IntSyncValue setpoint, IntSyncValue max,
        DoubleSyncValue stability) {
        return (context, x, y, width, height, theme) -> {
            final double top = Math.max(1, max.getIntValue());
            final double fill = Math.max(0, Math.min(1, mass.getDoubleValue() / top));
            final double tick = Math.max(0, Math.min(1, setpoint.getIntValue() / top));
            final double s = stability.getDoubleValue();
            GuiDraw.drawRect(x, y, width, height, 0xFF202028);
            GuiDraw.drawRect(x, y, (float) (width * fill), height, s >= 0.999 ? GREEN : s >= 0.5 ? AMBER : RED);
            GuiDraw.drawRect(x + (float) (width * tick) - 0.5f, y - 1, 1, height + 2, 0xFFFFFFFF);
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
        if (stability >= 0.999) return EnumChatFormatting.GREEN + "Stability 100% (holding)";
        return (stability >= 0.5 ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + "Stability "
            + pct
            + "% (drifting, evaporates in "
            + duration(evapSeconds)
            + " without bolts)";
    }

    static String containmentLine(boolean open, boolean enabled, boolean powerOk, long eut) {
        if (!open) return EnumChatFormatting.DARK_GRAY + "Containment: idle (no hole)";
        if (!enabled) return EnumChatFormatting.RED + "Switched OFF: governor stopped, the hole bleeds";
        if (!powerOk) return EnumChatFormatting.RED + "Containment power LOW: governor stopped";
        return EnumChatFormatting.GRAY + "Containment " + String.format(Locale.ROOT, "%,d", eut) + " EU/t";
    }

    static String modulesLine(StringSyncValue[] slots) {
        final StringBuilder sb = new StringBuilder(EnumChatFormatting.WHITE + "Modules:");
        for (KerrSlot slot : KerrSlot.VALUES) {
            final String state = slots[slot.ordinal()].getStringValue()
                .split("\\|", -1)[0];
            sb.append(' ')
                .append(stateColour(state))
                .append(slot.label)
                .append(' ')
                .append(shortState(state))
                .append(EnumChatFormatting.WHITE);
        }
        return sb.toString();
    }

    static EnumChatFormatting stateColour(String state) {
        if (state.startsWith("RUNNING")) return EnumChatFormatting.GREEN;
        if (state.startsWith("IDLE")) return EnumChatFormatting.AQUA;
        if (state.startsWith("PAUSED")) return EnumChatFormatting.GOLD;
        if (state.startsWith("NOT BUILT")) return EnumChatFormatting.DARK_GRAY;
        return EnumChatFormatting.RED;
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

    /** Left: the setpoint, as a whole number of SM, with - / + (server clamps to the tier's range). */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue setpoint = syncManager.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .child(
                IKey.str("Setpoint")
                    .asWidget()
                    .marginRight(2)
                    .heightRel(1f))
            .child(stepButton(setpoint, -1))
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .numbersInt(MTEKerrEngine.SETPOINT_MIN, MTEKerrEngine.SETPOINT_MAX_T2)
                    .syncHandler("kerrSetpoint")
                    .width(28)
                    .height(14)
                    .tooltip(
                        t -> t.addLine("Hole mass setpoint, SM (whole numbers)")
                            .addLine(EnumChatFormatting.GRAY + "T1: 2-64 SM. Kerr T2: 2-512 SM.")))
            .child(stepButton(setpoint, 1));
    }

    private static IWidget stepButton(IntSyncValue setpoint, int step) {
        return new ButtonWidget<>().size(10, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final int by = button == 1 ? step * 8 : step;
                setpoint.setIntValue(setpoint.getIntValue() + by);
                return true;
            })
            .tooltip(t -> t.addLine((step < 0 ? "Lower" : "Raise") + " the setpoint by 1 SM (right-click: 8)"))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Right (GT lays this row out reversed: the last child is leftmost). */
    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler holePanel = syncManager
            .syncedPanel("kerrHolePanel", true, (sub, handler) -> buildHolePanel(syncManager, parent));
        final IPanelHandler modulesPanel = syncManager
            .syncedPanel("kerrModulesPanel", true, (sub, handler) -> buildModulesPanel(syncManager, parent));
        final BooleanSyncValue render = syncManager.findSyncHandler("kerrRender", BooleanSyncValue.class);
        return super.createRightPanelGapRow(parent, syncManager).child(
            new ToggleButton().size(18, 18)
                .marginLeft(2)
                .value(render)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SHOW_WORK_AREA)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_WORK_AREA)
                .tooltipBuilder(t -> t.addLine("Black hole render on / off (this machine)"))
                .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                panelButton(
                    modulesPanel,
                    GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS,
                    "Modules: each arc's machine, its state and bonus"))
            .child(
                panelButton(
                    holePanel,
                    GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED,
                    "Hole control: setpoint slider, Open, Collapse, Auto"));
    }

    private static IWidget panelButton(IPanelHandler panel, IDrawable icon, String tooltip) {
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(2)
            .overlay(icon)
            .onMousePressed(button -> {
                if (panel.isPanelOpen()) panel.closePanel();
                else panel.openPanel();
                return true;
            })
            .tooltip(t -> t.addLine(tooltip))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ======================================================================= hole control panel

    private ModularPanel buildHolePanel(PanelSyncManager main, ModularPanel parent) {
        final BooleanSyncValue formed = main.findSyncHandler("kerrFormed", BooleanSyncValue.class);
        final BooleanSyncValue open = main.findSyncHandler("kerrOpen", BooleanSyncValue.class);
        final IntSyncValue setpoint = main.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        final IntSyncValue maxSetpoint = main.findSyncHandler("kerrMaxSetpoint", IntSyncValue.class);
        final BooleanSyncValue density = main.findSyncHandler("kerrDensity", BooleanSyncValue.class);
        final IntSyncValue seeds = main.findSyncHandler("kerrSeeds", IntSyncValue.class);
        final IntSyncValue collapsers = main.findSyncHandler("kerrCollapsers", IntSyncValue.class);
        final BooleanSyncValue auto = main.findSyncHandler("kerrAuto", BooleanSyncValue.class);
        final IntSyncValue linked = main.findSyncHandler("kerrLinked", IntSyncValue.class);
        // Collapse needs two clicks; the first arms it (client-side state, reset when the hole closes).
        final boolean[] armed = { false };
        final int top = Math.max(MTEKerrEngine.SETPOINT_MIN + 1, maxSetpoint.getIntValue());

        return new ModularPanel("kerrHolePanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(140, 158)
            .child(
                Flow.column()
                    .full()
                    .padding(4)
                    .childPadding(3)
                    .child(
                        IKey.str(EnumChatFormatting.BOLD + "Hole control")
                            .asWidget()
                            .alignment(Alignment.Center))
                    .child(
                        IKey.dynamic(() -> "Setpoint " + setpoint.getIntValue() + " SM (2-" + top + ")")
                            .asWidget())
                    .child(
                        new SliderWidget().bounds(MTEKerrEngine.SETPOINT_MIN, top)
                            .value(new DoubleValue.Dynamic(setpoint::getIntValue, v -> {
                                final int i = (int) Math.round(v);
                                if (i != setpoint.getIntValue()) setpoint.setIntValue(i);
                            }))
                            .size(130, 10))
                    .child(
                        IKey.dynamic(
                            () -> density.getBoolValue() ? EnumChatFormatting.AQUA + "Density Upgrade: installed (T2)"
                                : EnumChatFormatting.GRAY + "No Density Upgrade: right-click it in")
                            .scale(0.8f)
                            .asWidget())
                    .child(
                        new ButtonWidget<>().size(130, 16)
                            .overlay(IKey.dynamic(() -> {
                                final boolean can = formed.getBoolValue() && !open.getBoolValue()
                                    && seeds.getIntValue() > 0;
                                return (can ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY) + "Open hole ("
                                    + seeds.getIntValue()
                                    + " seed"
                                    + (seeds.getIntValue() == 1 ? "" : "s")
                                    + ")";
                            }))
                            .onMousePressed(button -> {
                                if (formed.getBoolValue() && !open.getBoolValue() && seeds.getIntValue() > 0) {
                                    main.callSyncedAction("kerrOpenHole", packet -> {});
                                }
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Consumes one Black Hole Seed from a bolt bus")
                                    .addLine(EnumChatFormatting.GRAY + "The hole opens at 8 SM")))
                    .child(
                        new ButtonWidget<>().size(130, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (!open.getBoolValue()) armed[0] = false;
                                final boolean can = open.getBoolValue() && collapsers.getIntValue() > 0;
                                if (armed[0]) {
                                    return EnumChatFormatting.RED + "Confirm: "
                                        + linked.getIntValue()
                                        + " module"
                                        + (linked.getIntValue() == 1 ? "" : "s")
                                        + " pause";
                                }
                                return (can ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY) + "Collapse ("
                                    + collapsers.getIntValue()
                                    + " collapser"
                                    + (collapsers.getIntValue() == 1 ? "" : "s")
                                    + ")";
                            }))
                            .onMousePressed(button -> {
                                if (!open.getBoolValue() || collapsers.getIntValue() <= 0) {
                                    armed[0] = false;
                                    return true;
                                }
                                if (!armed[0]) {
                                    armed[0] = true;
                                } else {
                                    armed[0] = false;
                                    main.callSyncedAction("kerrCollapseHole", packet -> {});
                                }
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Consumes one Black Hole Collapser from a bolt bus")
                                    .addLine(EnumChatFormatting.GRAY + "Click twice. Linked modules pause and keep")
                                    .addLine(EnumChatFormatting.GRAY + "their recipes until the hole reopens.")))
                    .child(
                        new ButtonWidget<>().size(130, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> "Auto: " + (auto.getBoolValue() ? EnumChatFormatting.GREEN + "ON"
                                        : EnumChatFormatting.RED + "OFF")))
                            .onMousePressed(button -> {
                                auto.setBoolValue(!auto.getBoolValue());
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Auto ON: a Seed or Collapser placed in a bolt bus")
                                    .addLine("acts on its own (automation keeps working).")
                                    .addLine(EnumChatFormatting.GRAY + "OFF: only the buttons open and close."))));
    }

    // =========================================================================== modules panel

    private ModularPanel buildModulesPanel(PanelSyncManager main, ModularPanel parent) {
        final StringSyncValue[] slots = slotValues(main);
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(2)
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Modules")
                    .asWidget()
                    .alignment(Alignment.Center));
        for (KerrSlot slot : KerrSlot.VALUES) {
            final StringSyncValue v = slots[slot.ordinal()];
            column.child(IKey.dynamic(() -> {
                final String state = field(v, 0);
                return stateColour(state) + slot.label + ": " + state;
            })
                .asWidget());
            column.child(IKey.dynamic(() -> {
                final String state = field(v, 0);
                final String pos = field(v, 3);
                final boolean linked = state.startsWith("RUNNING") || state.startsWith("IDLE");
                return EnumChatFormatting.GRAY
                    + (linked ? "parallel " + field(v, 1) + ", speed x" + field(v, 2) + "  " : "")
                    + (pos.isEmpty() ? "" : "at " + pos);
            })
                .scale(0.8f)
                .asWidget()
                .marginBottom(3));
        }
        column.child(
            IKey.str(EnumChatFormatting.DARK_GRAY + "Run each module from its own controller.")
                .scale(0.8f)
                .asWidget());
        return new ModularPanel("kerrModulesPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(160, 30 + 26 * KerrSlot.VALUES.length)
            .child(column);
    }

    private static String field(StringSyncValue v, int i) {
        final String[] f = v.getStringValue()
            .split("\\|", -1);
        return i < f.length ? f[i] : "";
    }
}
