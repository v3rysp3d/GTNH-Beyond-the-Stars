package com.nateyburr.beyondthestars.machines.gui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/**
 * Controller GUI cleanup (controller-gui-spec.md §5.10): the family layout for our hatch GUIs. A dark readout box
 * ({@code BACKGROUND_TERMINAL}, 166 wide) at (5, 5) holds the live value; the controls sit under it, 16x16 buttons in
 * rows with an 18 px pitch starting 5 px below the box, labels in panel text; the player inventory follows at GT's
 * place for the panel height. No G logo and no corner flows, so nothing shares the controls' rows.
 * <p>
 * A subclass gives the box height ({@link #readoutHeight}), fills the box ({@link #addReadout}, box-relative
 * positions; the text column runs from x {@link #TEXT_X} for {@link #TEXT_W} pixels) and adds its control rows
 * ({@link #addControls}) with {@link #cycleRow}, {@link #toggleRow} and {@link #fieldRow}. The readout parts are kit
 * painters ({@link #line}, {@link #lamp}, {@link #gaugeLine}, {@link #barLine}, {@link #lampLine}); every text is
 * drawn at scale 1.0 with no shadow.
 */
public abstract class BTSHatchGui<T extends MTEHatch> extends MTEHatchBaseGui<T> {

    /** The readout box: panel position, width. */
    public static final int BOX_X = 5, BOX_Y = 5, BOX_W = 166;
    /** The box's text column: left x and width (5 px inside each edge). */
    public static final int TEXT_X = 5, TEXT_W = 156;
    /** Control rows: the button size, the row pitch, the gap under the box, the button x on the panel. */
    public static final int BUTTON = 16, PITCH = 18, GAP = 5, CONTROL_X = 7;
    /** The default panel height (the Directive Hatch and the Stellar Field Sensor Hatch). */
    public static final int PANEL_H = 214;
    /** The content section's origin on the panel (GT's border; this layout drops the content padding). */
    private static final int ORIGIN = 4;

    protected BTSHatchGui(T hatch) {
        super(hatch);
    }

    /** The panel height; {@link #PANEL_H} unless the hatch needs another. */
    protected int panelHeight() {
        return PANEL_H;
    }

    /** The readout box height. */
    protected abstract int readoutHeight();

    /** Registers the hatch's sync values, then fills the box (box-relative positions). */
    protected abstract void addReadout(ParentWidget<?> box, PanelSyncManager syncManager);

    /** Adds the control rows, top to bottom (called after {@link #addReadout}). */
    protected abstract void addControls(Flow column, PanelSyncManager syncManager);

    @Override
    protected int getBasePanelHeight() {
        return panelHeight();
    }

    @Override
    protected boolean supportsBottomRowOverlap() {
        return true;
    }

    @Override
    protected int getContentPaddingHorizontal() {
        return 0;
    }

    @Override
    protected int getContentPaddingVertical() {
        return 0;
    }

    @Override
    protected boolean supportsBottomLeftCornerFlow() {
        return false;
    }

    @Override
    protected boolean supportsBottomRightCornerFlow() {
        return false;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        final int h = readoutHeight();
        final ParentWidget<?> box = new ParentWidget<>().pos(BOX_X - ORIGIN, BOX_Y - ORIGIN)
            .size(BOX_W, h)
            .widgetTheme(GTWidgetThemes.BACKGROUND_TERMINAL);
        addReadout(box, syncManager);
        final Flow column = Flow.column()
            .pos(CONTROL_X - ORIGIN, BOX_Y + h + GAP - ORIGIN)
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(PITCH - BUTTON);
        addControls(column, syncManager);
        return super.createContentSection(panel, syncManager).child(box)
            .child(column);
    }

    // ================================================================================== text

    /** A lang key translated in the running side's language (the client's in a GUI, the server's on the bench). */
    public static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    // ============================================================================ readout parts

    /**
     * A text line in the box at {@code y} (box-relative): {@code left} from the text column's left edge in
     * {@code leftColour}, {@code right} right-aligned to its right edge (§ codes inside override the colours). When
     * the two do not fit, the right text is cut first, so the left one (what the line is about) stays whole.
     */
    public static BTSGuiKit.KitWidget line(int y, Supplier<String> left, int leftColour, Supplier<String> right,
        int rightColour) {
        return line(y, left, leftColour, right, rightColour, false);
    }

    /** As {@link #line(int, Supplier, int, Supplier, int)}; {@code keepRight}: cut the left text instead. */
    public static BTSGuiKit.KitWidget line(int y, Supplier<String> left, int leftColour, Supplier<String> right,
        int rightColour, boolean keepRight) {
        return new BTSGuiKit.KitWidget(
            (w, width, height) -> paintLine(
                width,
                left.get(),
                leftColour,
                right == null ? "" : right.get(),
                rightColour,
                keepRight)).pos(TEXT_X, y - 1)
                    .size(TEXT_W, 10);
    }

    static void paintLine(int width, String left, int leftColour, String right, int rightColour, boolean keepRight) {
        if (right == null || right.isEmpty()) {
            KitDraw.textCut(left, 0, 1, width, leftColour);
            return;
        }
        if (keepRight) {
            final int rw = KitDraw.width(right);
            KitDraw.textRight(right, width, 1, rightColour);
            // a long channel name ("Zone (0 Resonant .. 4 Breach)") loses its parenthetical before it is cut
            String l = left;
            if (l != null && KitDraw.width(l) > width - rw - 6) l = l.replaceAll(" \\([^)]*\\)", "");
            KitDraw.textCut(l, 0, 1, width - rw - 6, leftColour);
            return;
        }
        final int lw = Math.min(KitDraw.width(left), width);
        KitDraw.textCut(left, 0, 1, width, leftColour);
        final int room = width - lw - 6;
        if (room <= 0) return;
        String r = right;
        if (KitDraw.width(r) > room) r = KitDraw.font()
            .trimStringToWidth(r, Math.max(0, room - KitDraw.width("."))) + ".";
        KitDraw.textRight(r, width, 1, rightColour);
    }

    /**
     * A lamp text that does not fit keeps only the first word of a parenthetical ("4 (unused on T1)" reads "4
     * (unused)") before it is cut (dev client round 1: "4 (unused o." in the Directive Hatch's readout).
     */
    static String fit(String s, int width) {
        if (s == null || KitDraw.width(s) <= width) return s;
        return s.replaceAll("\\(([^ )]+)[^)]*\\)", "($1)");
    }

    /**
     * A lamp at (x, y) in the box: a 7x7 (edge + 5x5 fill), lit {@code §a} green while {@code on}, then its text (white
     * while on, grey while off). {@code width} bounds the text.
     */
    public static BTSGuiKit.KitWidget lamp(int x, int y, int width, BooleanSupplier on, Supplier<String> text) {
        return new BTSGuiKit.KitWidget((w, ww, hh) -> {
            final boolean lit = on.getAsBoolean();
            KitDraw.box(0, 1, 7, 7, BTSGuiKit.EDGE, lit ? BTSGuiKit.OK : BTSGuiKit.LAMP_OFF);
            KitDraw.textCut(fit(text.get(), ww - 10), 10, 1, ww - 10, lit ? BTSGuiKit.VALUE : BTSGuiKit.DIM);
        }).pos(x, y - 1)
            .size(width, 10);
    }

    /**
     * A lamp of any colour with a text, and a value right-aligned (a sensor's zone or flag channel): the whole text
     * column at {@code y}.
     */
    public static BTSGuiKit.KitWidget lampLine(int y, IntSupplier fill, Supplier<String> text, Supplier<String> value) {
        return new BTSGuiKit.KitWidget((w, width, h) -> {
            KitDraw.box(0, 1, 7, 7, BTSGuiKit.EDGE, fill.getAsInt());
            paintLineAt(10, width - 10, text.get(), BTSGuiKit.VALUE, value.get(), BTSGuiKit.VALUE);
        }).pos(TEXT_X, y - 1)
            .size(TEXT_W, 10);
    }

    private static void paintLineAt(int x, int width, String left, int leftColour, String right, int rightColour) {
        final int rw = right == null ? 0 : KitDraw.width(right);
        if (rw > 0) KitDraw.textRight(right, x + width, 1, rightColour);
        KitDraw.textCut(left, x, 1, width - (rw > 0 ? rw + 6 : 0), leftColour);
    }

    /** The gauge part's bar width (the value takes the rest of the text column). */
    public static final int GAUGE_W = 120;

    /**
     * A gauge in the box at {@code y}: the kit's ±120 zone bar ({@link BTSGuiKit#paintGaugeBar}) {@link #GAUGE_W}
     * wide with an optional yellow tick (NaN: none), and the value right-aligned.
     */
    public static BTSGuiKit.KitWidget gaugeLine(int y, DoubleSupplier v, DoubleSupplier tick, Supplier<String> value) {
        return new BTSGuiKit.KitWidget((w, width, h) -> {
            BTSGuiKit.paintGaugeBar(0, 1, GAUGE_W, v.getAsDouble(), Double.NaN, tick.getAsDouble());
            KitDraw.textRight(value.get(), width, 1, BTSGuiKit.VALUE);
        }).pos(TEXT_X, y - 1)
            .size(TEXT_W, 10);
    }

    /**
     * A plain bar in the box at {@code y}: {@link #GAUGE_W} wide, the gradient filled to {@code frac}, an optional
     * yellow tick at {@code tick} (a fraction; NaN: none), and the value right-aligned.
     */
    public static BTSGuiKit.KitWidget barLine(int y, DoubleSupplier frac, DoubleSupplier tick,
        Supplier<BTSGuiKit.Grad> grad, Supplier<String> value) {
        return new BTSGuiKit.KitWidget((w, width, h) -> {
            final int bh = 7;
            KitDraw.box(0, 1, GAUGE_W, bh, BTSGuiKit.EDGE, BTSGuiKit.TRACK);
            final double f = frac.getAsDouble();
            final double ff = Double.isNaN(f) ? 0 : Math.max(0, Math.min(1, f));
            final BTSGuiKit.Grad g = grad.get();
            if (ff > 0) KitDraw.gradient(1, 2, (float) ((GAUGE_W - 2) * ff), bh - 2, g.from, g.to);
            final double t = tick.getAsDouble();
            if (!Double.isNaN(t)) {
                KitDraw.rect(Math.round(1 + (GAUGE_W - 2) * Math.max(0, Math.min(1, t))), 0, 1, bh + 2, BTSGuiKit.TICK);
            }
            KitDraw.textRight(value.get(), width, 1, BTSGuiKit.VALUE);
        }).pos(TEXT_X, y - 1)
            .size(TEXT_W, 10);
    }

    // ============================================================================== control rows

    /** A control and its label 4 px to its right, the label as wide as the panel leaves (scrolls on hover). */
    private static Flow controlRow(com.cleanroommc.modularui.api.widget.IWidget control, int controlWidth,
        Supplier<String> label) {
        return Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .childPadding(4)
            .child(control)
            .child(
                new ScrollingTextWidget(IKey.dynamic(label::get)).width(176 - CONTROL_X - 7 - 4 - controlWidth)
                    .height(9)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .shadow(false));
    }

    /**
     * A cycle button (GT's cyclic overlay) and its label: left click the next state, right click the previous one
     * (MUI2's cycle button). {@code tooltip} explains the button.
     */
    public static Flow cycleRow(IntSyncValue value, int states, Supplier<String> label, Consumer<RichTooltip> tooltip) {
        final CycleButtonWidget button = new CycleButtonWidget().value(value)
            .stateCount(states)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
            .size(BUTTON);
        button.tooltipDynamic(tooltip)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return controlRow(button, BUTTON, label);
    }

    /** A toggle button with its on / off overlays and its label. */
    public static Flow toggleRow(BooleanSyncValue value, IDrawable on, IDrawable off, Supplier<String> label,
        Consumer<RichTooltip> tooltip) {
        final ToggleButton button = new ToggleButton().value(value)
            .overlay(true, on)
            .overlay(false, off)
            .size(BUTTON);
        button.tooltipDynamic(tooltip)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return controlRow(button, BUTTON, label);
    }

    /** A number field ({@code width} x 12, white text on GT's dark field) and its label. */
    public static Flow fieldRow(TextFieldWidget field, int width, Supplier<String> label,
        Consumer<RichTooltip> tooltip) {
        field.size(width, 12);
        if (tooltip != null) field.tooltip(tooltip)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return controlRow(field, width, label).height(BUTTON);
    }
}
