package com.nateyburr.beyondthestars.machines.gui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Controller GUI cleanup (controller-gui-spec.md §3, §4.4): the family frame every Beyond the Stars controller GUI
 * shares. On top of GT's MUI2 multiblock layout it makes the terminal 118 px high (panel 198 x 227) and fills it with
 * a fixed header (state badge, subject, right chip, an alert icon only when something is wrong) over a scrolling list
 * of kit rows, and an Info page the <b>i</b> button swaps in. GT's G logo and always-on maintenance icon are not added
 * (the parent is ours). The render toggle goes to the top of the button column; a disabled power switch draws
 * TecTech's icon (GT 5.09.54.205 has no {@code power_switch_disabled} overlay of its own).
 * <p>
 * A subclass decides, on the SERVER, which rows its state shows ({@link #addStatusRows}, {@link #addInfoRows}); the
 * rows travel as one string each ({@link BTSRows}), rebuilt every {@link #refreshTicks()} ticks, and the client's row
 * pool draws them. Recipe machines ({@code recipeMachine}) keep GT's recipe rows above ours.
 */
public abstract class BTSControllerGui<T extends MTEMultiBlockBase> extends MTEMultiBlockBaseGui<T> {

    /** Terminal height (GT's is 94) and the extra panel height it costs. */
    public static final int TERMINAL_HEIGHT = 118, EXTRA_HEIGHT = 24;

    private static final String SYNC_BADGE = "btsBadge", SYNC_SUBJECT = "btsSubject", SYNC_RIGHT = "btsRight",
        SYNC_RIGHT_TIP = "btsRightTip", SYNC_ROWS = "btsRows", SYNC_INFO = "btsInfo";

    /** Keep GT's recipe rows (recipe result, outputs) above the kit rows. */
    protected final boolean recipeMachine;
    /** The BooleanSyncValue key of the centre render switch, or null when the machine has no render. */
    protected final String renderSyncKey;
    /** Client only: true while the Info page shows. */
    protected final BoolValue info = new BoolValue(false);

    private long rowsAt = Long.MIN_VALUE, infoAt = Long.MIN_VALUE;
    private String rowsCache = "", infoCache = "";

    protected BTSControllerGui(T multiblock, boolean recipeMachine, String renderSyncKey) {
        super(multiblock);
        this.recipeMachine = recipeMachine;
        this.renderSyncKey = renderSyncKey;
    }

    // ============================================================================ what a machine says

    /** Server: the state badge (first match of the machine's table). */
    protected abstract BTSGuiKit.Badge badgeCode();

    /** Server: the header subject, a {@link BTSText} encoding ("" for none). */
    protected abstract String subject();

    /** Server: the header's right chip, a {@link BTSText} encoding. */
    protected abstract String rightChip();

    /** Server: the right chip's tooltip lines (encoded texts). */
    protected List<String> rightChipTooltip() {
        return Collections.emptyList();
    }

    /** Server: the status rows of the state the machine is in now. */
    protected abstract void addStatusRows(BTSRows.Builder rows);

    /** Server: the Info page rows (the questbook's words). */
    protected abstract void addInfoRows(BTSRows.Builder rows);

    /** Machine panel buttons for the gap row's right group (added after <b>i</b>, so drawn left of it). */
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {}

    /** The render toggle's tooltip (only with a render). */
    protected void renderTooltip(RichTooltip t) {
        t.addLine(IKey.lang("beyondthestars.gui.kit.render"));
    }

    /** The lang key of the disabled power switch's tooltip. */
    protected String powerSwitchDisabledKey() {
        return "GT5U.gui.button.power_switch";
    }

    /** Rows in the status pool (the most a state shows). */
    protected int statusPoolSize() {
        return 24;
    }

    /** Rows in the Info pool. */
    protected int infoPoolSize() {
        return 16;
    }

    /** Ticks between rebuilds of the row strings (the values change at most once a second on our machines). */
    protected int refreshTicks() {
        return 5;
    }

    /**
     * Bench: the header and the status (or Info) rows as plain text, built exactly as the GUI syncs them (server
     * side, so a dev server can check a machine's GUI without a client).
     */
    public List<String> benchDump(boolean infoPage) {
        final List<String> out = new ArrayList<>();
        final BTSGuiKit.Badge b = badgeCode();
        out.add(
            ("[" + BTSText.resolve(
                BTSText.lang(b.langKey())) + "] " + BTSText.resolve(subject()) + " | " + BTSText.resolve(rightChip()))
                    .replaceAll("\u00a7.", ""));
        final BTSRows.Builder rows = new BTSRows.Builder();
        if (infoPage) addInfoRows(rows);
        else addStatusRows(rows);
        for (BTSRows.Row r : BTSRows.decode(rows.build())) out.add("  " + BTSRows.describe(r));
        return out;
    }

    // =================================================================================== geometry

    @Override
    protected int getTerminalRowHeight() {
        return TERMINAL_HEIGHT;
    }

    @Override
    protected int getBasePanelHeight() {
        return 181 + getTextBoxToInventoryGap() + EXTRA_HEIGHT;
    }

    // ================================================================================== sync

    private long now() {
        return baseMetaTileEntity == null ? 0 : baseMetaTileEntity.getTimer();
    }

    private boolean server() {
        return baseMetaTileEntity != null && baseMetaTileEntity.isServerSide();
    }

    private String rowsNow() {
        if (!server()) return rowsCache;
        final long t = now();
        if (t - rowsAt >= refreshTicks() || t < rowsAt) {
            final BTSRows.Builder b = new BTSRows.Builder();
            addStatusRows(b);
            rowsCache = b.build();
            rowsAt = t;
        }
        return rowsCache;
    }

    private String infoNow() {
        if (!server()) return infoCache;
        final long t = now();
        if (t - infoAt >= 20 || t < infoAt) {
            final BTSRows.Builder b = new BTSRows.Builder();
            addInfoRows(b);
            infoCache = b.build();
            infoAt = t;
        }
        return infoCache;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(SYNC_BADGE, new IntSyncValue(() -> server() ? badgeCode().ordinal() : 0));
        syncManager.syncValue(SYNC_SUBJECT, new StringSyncValue(() -> server() ? subject() : ""));
        syncManager.syncValue(SYNC_RIGHT, new StringSyncValue(() -> server() ? rightChip() : ""));
        syncManager.syncValue(
            SYNC_RIGHT_TIP,
            new StringSyncValue(() -> server() ? BTSRows.Builder.tooltip(rightChipTooltip()) : ""));
        syncManager.syncValue(SYNC_ROWS, new StringSyncValue(this::rowsNow));
        syncManager.syncValue(SYNC_INFO, new StringSyncValue(this::infoNow));
    }

    // ================================================================================ terminal

    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager syncManager) {
        final int w = getTerminalWidgetWidth(), h = getTerminalWidgetHeight();
        final ParentWidget<?> parent = new ParentWidget<>().size(w, h)
            .widgetTheme(GTWidgetThemes.BACKGROUND_TERMINAL);
        parent.child(createHeader(syncManager).pos(4, 4));
        final ListWidget<IWidget, ?> status = createTerminalTextWidget(syncManager, panel);
        status.pos(4, 20)
            .size(w - 4, h - 24)
            .collapseDisabledChild()
            .setEnabledIf(x -> !info.getBoolValue());
        parent.child(status);
        final StringSyncValue infoSync = syncManager.findSyncHandler(SYNC_INFO, StringSyncValue.class);
        final ListWidget<IWidget, ?> infoList = new ListWidget<>().crossAxisAlignment(Alignment.CrossAxis.START);
        BTSGuiKit.rowPool(infoList, new BTSGuiKit.RowCache(infoSync::getStringValue), infoPoolSize());
        infoList.pos(4, 20)
            .size(w - 4, h - 24)
            .collapseDisabledChild()
            .setEnabledIf(x -> info.getBoolValue());
        parent.child(infoList);
        return parent;
    }

    private ParentWidget<?> createHeader(PanelSyncManager syncManager) {
        final IntSyncValue badge = syncManager.findSyncHandler(SYNC_BADGE, IntSyncValue.class);
        final StringSyncValue subject = syncManager.findSyncHandler(SYNC_SUBJECT, StringSyncValue.class);
        final StringSyncValue right = syncManager.findSyncHandler(SYNC_RIGHT, StringSyncValue.class);
        final StringSyncValue rightTip = syncManager.findSyncHandler(SYNC_RIGHT_TIP, StringSyncValue.class);
        final IntSyncValue maint = syncManager.findSyncHandler("maintCount", IntSyncValue.class);
        final BooleanSyncValue shut = syncManager.findSyncHandler("wasShutdown", BooleanSyncValue.class);
        final boolean maintAlerts = multiblock.supportsMaintenanceIssueHoverable();
        final java.util.function.BooleanSupplier shutdown = () -> shut != null && shut.getBoolValue()
            && shouldShutdownReasonBeDisplayed(getShutDownReasonKey());
        final java.util.function.BooleanSupplier maintenance = () -> maintAlerts && maint != null
            && maint.getIntValue() > 0;
        final java.util.function.BooleanSupplier alert = () -> shutdown.getAsBoolean() || maintenance.getAsBoolean();
        final BTSGuiKit.KitWidget alertIcon = new BTSGuiKit.KitWidget((w, width, height) -> {
            if (shutdown.getAsBoolean()) KitDraw.stop(0, 0);
            else if (maintenance.getAsBoolean()) KitDraw.wrench(0, 0);
        }).size(9, 9)
            .pos(167, 1)
            .setEnabledIf(x -> alert.getAsBoolean())
            .tooltipDynamic(t -> {
                if (maintenance.getAsBoolean()) makeMaintenanceHoverableTooltip(t, maint);
                if (shutdown.getAsBoolean()) t.addLine(getToolTipForReason(getShutDownReasonKey()));
            })
            .tooltipAutoUpdate(true);
        return BTSGuiKit.header(
            () -> BTSGuiKit.Badge.of(badge.getIntValue(), BTSGuiKit.Badge.DARK),
            () -> BTSText.resolve(subject.getStringValue()),
            () -> BTSText.resolve(right.getStringValue()),
            () -> {
                final String s = rightTip.getStringValue();
                if (s == null || s.isEmpty()) return Collections.<String>emptyList();
                final List<String> out = new ArrayList<>();
                for (String l : s.split(String.valueOf(BTSRows.LINE))) out.add(l);
                return out;
            },
            alert,
            alertIcon);
    }

    /**
     * The status list: GT's rows (recipe machines: everything GT shows; others: the startup line, the shutdown and
     * structure-error rows with GT's highlight button) and then the kit rows.
     */
    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final ListWidget<IWidget, ?> list;
        if (recipeMachine) {
            list = super.createTerminalTextWidget(syncManager, parent);
        } else {
            final IntSyncValue startup = new IntSyncValue(multiblock::getmStartUpCheck);
            syncManager.syncValue("startupCheck", startup);
            list = new ListWidget<>().crossAxisAlignment(Alignment.CrossAxis.START)
                .child(
                    IKey.lang("GT5U.multiblock.startup")
                        .color(Color.WHITE.main)
                        .asWidget()
                        .textAlign(Alignment.CenterLeft)
                        .setEnabledIf(w -> startup.getValue() > 0)
                        .marginBottom(2)
                        .width(180))
                .child(createShutdownDurationWidget(syncManager))
                .child(createShutdownReasonWidget(syncManager))
                .child(createStructureErrorWidget(syncManager));
        }
        final StringSyncValue rows = syncManager.findSyncHandler(SYNC_ROWS, StringSyncValue.class);
        BTSGuiKit.rowPool(list, new BTSGuiKit.RowCache(rows::getStringValue), statusPoolSize());
        return list;
    }

    // ================================================================================ gap row

    /** GT's power panel (where the machine supports it), then <b>i</b>, then the machine's panel buttons. */
    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final Flow row = super.createRightPanelGapRow(parent, syncManager);
        row.child(BTSGuiKit.infoToggle(info));
        addRightButtons(row, parent, syncManager);
        return row;
    }

    // ============================================================================ button column

    /** GT's column (bottom-up: controller slot, power, structure update), then the render toggle in the top slot. */
    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        final Flow column = super.createButtonColumn(panel, syncManager);
        if (renderSyncKey != null) {
            final BooleanSyncValue render = syncManager.findSyncHandler(renderSyncKey, BooleanSyncValue.class);
            if (render != null) column.child(
                new ToggleButton().size(18, 18)
                    .value(render)
                    .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SHOW_WORK_AREA)
                    .overlay(false, GTGuiTextures.OVERLAY_BUTTON_WORK_AREA)
                    .tooltipBuilder(this::renderTooltip)
                    .tooltipShowUpTimer(TOOLTIP_DELAY));
        }
        return column;
    }

    /** A disabled power switch draws TecTech's disabled icon (GT's own overlay is missing in 5.09.54.205). */
    @Override
    protected ToggleButton createPowerSwitchButton() {
        if (!isPowerSwitchDisabled()) return super.createPowerSwitchButton();
        return new ToggleButton().value(new BoolValue(false))
            .overlay(true, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_DISABLED)
            .overlay(false, GTGuiTextures.TT_OVERLAY_BUTTON_POWER_SWITCH_DISABLED)
            .tooltip(t -> t.addLine(IKey.lang(powerSwitchDisabledKey())))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }
}
