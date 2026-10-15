package com.nateyburr.beyondthestars.machines.kerr;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
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
 * The Kerr Ergosphere Engine's console on the controller GUI kit (controller-gui-spec.md §5.2). The header says the
 * hole's state (FROZEN, NOT FORMED, CLOSED, NO POWER, DRIFTING, GROWING, OPEN), its mass and the Kerr tier. An open
 * hole shows the Mass bar (the setpoint is its yellow tick), Stability (and the time to evaporation while it drifts),
 * the Governor, the bolt stock and how long it lasts, containment power, the Hawking bleed and the two module chips
 * (hover one for its state and bonus); a closed hole shows a checklist of what Open needs. The <b>i</b> page has the
 * questbook's words. Nothing a player needs takes a screwdriver:
 * <ul>
 * <li><b>Gap row:</b> the setpoint (a number field with − / +) on the left; the Hole control and Modules side panels
 * and <b>i</b> on the right. The render toggle is at the top of the button column; the power switch shows TecTech's
 * disabled icon (it does not stop the hole, B-52).</li>
 * <li><b>Hole control panel:</b> a setpoint slider over the tier's range with its scale, the number field mirrored,
 * the tier, the Seed and Collapser counts, Open (needs a Seed in a bolt bus) and Collapse (two clicks within a few
 * seconds, the first warns how many modules will pause) and the Auto check box (a Seed or Collapser dropped into a
 * bolt bus acts on its own, so automation keeps working).</li>
 * <li><b>Modules panel:</b> one row per slot: the module's icon, name, state, bonus or reason, and coordinates.
 * Read-only: modules are run from their own GUIs.</li>
 * </ul>
 * The rows are decided on the server ({@link BTSRows}); every setter is validated there
 * ({@link MTEKerrEngine#setSetpoint} clamps to the tier, the hole actions re-check the hole and the bolt buses). The
 * two hole actions are server-only synced actions registered on the MAIN panel's manager (the side panels call them).
 * Only one side panel is open at a time: they share the spot to the right of the main panel.
 */
public class MTEKerrEngineGui extends BTSControllerGui<MTEKerrEngine> {

    /** Collapse stays armed this long after its first click. */
    private static final long COLLAPSE_ARM_MS = 4000L;

    private static final int HOLE_PANEL_W = 150;
    private static final int HOLE_PANEL_H = 196;
    private static final int MODULES_PANEL_W = 172;
    private static final int MODULE_ROW_H = 32;

    /** Module chip dot colours (§5.2 row 8): RUNNING §a, IDLE §b, PAUSED §6, NOT BUILT §8, else §c. */
    private static final int DOT_DARK_GREY = 0xFF555555;

    private static final String K = "beyondthestars.gui.kerr.";

    public MTEKerrEngineGui(MTEKerrEngine multiblock) {
        super(multiblock, false, "kerrRender");
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("kerrFormed", new BooleanSyncValue(multiblock::isFormed));
        syncManager.syncValue("kerrOpen", new BooleanSyncValue(multiblock::isHoleOpen));
        // Settable from the client; MTEKerrEngine.setSetpoint clamps to the tier's range on the server.
        syncManager
            .syncValue("kerrSetpoint", new IntSyncValue(multiblock::getSetpoint, multiblock::setSetpoint).allowC2S());
        syncManager.syncValue("kerrMaxSetpoint", new IntSyncValue(multiblock::maxSetpoint));
        syncManager.syncValue("kerrDensity", new BooleanSyncValue(multiblock::isDensityInstalled));
        syncManager.syncValue("kerrSeeds", new IntSyncValue(multiblock::getSeeds));
        syncManager.syncValue("kerrCollapsers", new IntSyncValue(multiblock::getCollapsers));
        syncManager
            .syncValue("kerrAuto", new BooleanSyncValue(multiblock::isAutoSeeds, multiblock::setAutoSeeds).allowC2S());
        syncManager.syncValue(
            "kerrRender",
            new BooleanSyncValue(multiblock::isRenderEnabled, multiblock::setRenderEnabled).allowC2S());
        syncManager.syncValue("kerrLinked", new IntSyncValue(multiblock::getLinkedModules));
        for (KerrSlot slot : KerrSlot.VALUES) {
            syncManager.syncValue("kerrSlot." + slot.key, new StringSyncValue(() -> multiblock.slotStatus(slot)));
        }
        // Server-only actions (executeClient = false): the server re-checks every condition.
        syncManager.registerServerSyncedAction("kerrOpenHole", packet -> multiblock.openHoleFromGui());
        syncManager.registerServerSyncedAction("kerrCollapseHole", packet -> multiblock.collapseHoleFromGui());
    }

    // =============================================================================== the header

    private int kerrTier() {
        return multiblock.isDensityInstalled() ? 2 : 1;
    }

    /** §5.2 badge table, first match wins. */
    @Override
    protected Badge badgeCode() {
        final boolean formed = multiblock.isFormed();
        final boolean open = multiblock.isHoleOpen();
        if (!formed && open) return Badge.FROZEN;
        if (!formed) return Badge.NOT_FORMED;
        if (!open) return Badge.CLOSED;
        if (!multiblock.isPowerOk()) return Badge.NO_POWER;
        if (multiblock.getStability() < 0.999) return Badge.DRIFTING;
        if (multiblock.getMass() < multiblock.effectiveSetpoint() - 0.5) return Badge.GROWING;
        return Badge.OPEN;
    }

    @Override
    protected String subject() {
        if (multiblock.isHoleOpen()) return BTSText.lang(K + "hole", BTSFormat.fmt1(multiblock.getMass()));
        return multiblock.isFormed() ? BTSText.lang(K + "nohole") : "";
    }

    @Override
    protected String rightChip() {
        return BTSText.lang(K + "right", kerrTier());
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        out.add(
            BTSText.lang(
                K + "right.tip",
                kerrTier(),
                multiblock.maxSetpoint(),
                roman(multiblock.railTier()),
                multiblock.boltCap()));
        if (!multiblock.isDensityInstalled()) {
            out.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "right.tip2")));
        }
        return out;
    }

    // ============================================================================= status rows

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        switch (badgeCode()) {
            case NOT_FORMED:
                rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "notformed", kerrTier(), kerrTier()));
                chipsRow(rows);
                break;
            case FROZEN:
                rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "frozen"));
                massRow(rows);
                stabilityRow(rows);
                chipsRow(rows);
                break;
            case CLOSED:
                closedRows(rows);
                break;
            default:
                openRows(rows);
        }
        notesRows(rows);
    }

    private void openRows(BTSRows.Builder rows) {
        massRow(rows);
        stabilityRow(rows);
        if (multiblock.getStability() < 0.999) {
            rows.note(
                BTSRows.ICON_BAD,
                BTSGuiKit.BAD,
                BTSText.lang(K + "evap", BTSFormat.duration(multiblock.getEvapSeconds())),
                BTSText.lang(K + "evap.tip"));
        }
        governorRow(rows);
        stockRow(rows);
        final long eut = multiblock.getContainmentEut();
        rows.stat(
            BTSText.lang(K + "containment"),
            multiblock.isPowerOk() ? BTSText.lit("§a" + BTSFormat.euT(eut) + " EU/t")
                : BTSText.cat(BTSText.lit("§c"), BTSText.lang(K + "containment.low")),
            BTSText.lang(K + "containment.tip"),
            BTSText.cat(
                BTSText.lit("§7"),
                BTSText.lang(K + "containment.supply", BTSFormat.euT(multiblock.getMaxInputEu()))));
        rows.stat(
            BTSText.lang(K + "bleed"),
            BTSText.lit(String.format(Locale.ROOT, "%.4f SM/t", multiblock.getBleed())),
            BTSText.lang(K + "bleed.tip"));
        chipsRow(rows);
    }

    /** Mass, frac m / ceiling, the setpoint as the yellow tick; §f on the setpoint, §6 below it, §b above. */
    private void massRow(BTSRows.Builder rows) {
        final double m = multiblock.getMass();
        // QA-A30: the setpoint the governor holds (a stored 768 is held at 512 while a quasar link is lost)
        final int sp = multiblock.effectiveSetpoint();
        final int max = Math.max(1, multiblock.maxSetpoint());
        final String colour = Math.abs(m - sp) <= 0.5 ? "§f" : m < sp ? "§6" : "§b";
        rows.bar(
            BTSText.lang(K + "mass"),
            m / max,
            BTSText.lit(colour + BTSFormat.fmt1(m) + " SM"),
            Grad.LAVENDER,
            BTSGuiKit.tick(sp / (double) max, BTSGuiKit.TICK),
            BTSText.lang(K + "mass.tip", BTSFormat.fmt1(m), max, sp));
    }

    private void stabilityRow(BTSRows.Builder rows) {
        final double s = multiblock.getStability();
        final long pct = Math.round(s * 100);
        rows.bar(
            BTSText.lang("beyondthestars.gui.kit.stability"),
            s,
            BTSText.lit(BTSGuiKit.healthText(pct) + pct + "%"),
            BTSGuiKit.healthGrad(pct),
            null,
            BTSText.lang(K + "stability.tip"));
    }

    private void governorRow(BTSRows.Builder rows) {
        final double bpt = multiblock.getBoltsPerTick();
        final int cap = multiblock.boltCap();
        final boolean below = multiblock.getMass() < multiblock.effectiveSetpoint() - 1e-3;
        final String colour = bpt <= 0 && below ? "§c" : "§f";
        rows.bar(
            BTSText.lang(K + "governor"),
            cap > 0 ? bpt / cap : 0,
            BTSText.lit(colour + String.format(Locale.ROOT, "%.2f/%d", bpt, cap)),
            Grad.BLUE,
            null,
            BTSText.lang(K + "governor.tip", roman(multiblock.railTier()), cap));
    }

    /** Bolt stock and how long it lasts: §a 10 min or more, §6 1-10 min, §c under a minute or empty. */
    private void stockRow(BTSRows.Builder rows) {
        final double sm = multiblock.getBoltStockSm();
        final long sec = multiblock.getBoltStockSeconds();
        final String colour = sm <= 0 || sec >= 0 && sec < 60 ? "§c" : sec < 0 || sec >= 600 ? "§a" : "§6";
        rows.stat(
            BTSText.lang(K + "stock"),
            BTSText.lit(
                colour + MTEKerrEngine.fmt(sm) + " SM" + (sm > 0 && sec >= 0 ? "  " + BTSFormat.duration(sec) : "")),
            BTSText.lang(K + "stock.tip"));
    }

    /** §5.2 Closed: what Open needs, every line met or not. Consumes nothing. */
    private void closedRows(BTSRows.Builder rows) {
        rows.section(BTSText.lang(K + "sec.open"), "");
        final BTSChecklist c = new BTSChecklist();
        c.add(
            multiblock.isFormed(),
            BTSText.lang(K + "chk.cage"),
            BTSText.lang(K + "right", kerrTier()),
            -1,
            BTSText.lang(K + "chk.cage.tip"));
        final int seeds = multiblock.getSeeds();
        c.add(
            seeds > 0,
            BTSText.lang(K + "chk.seed"),
            BTSText.lang(K + "chk.seed.v", seeds),
            -1,
            BTSText.lang(K + "chk.seed.tip"));
        final long supply = multiblock.getMaxInputEu();
        final boolean power = multiblock.containmentReady();
        c.add(
            power,
            BTSText.lang(K + "chk.power"),
            BTSText.lit(
                power ? BTSFormat.euT(MTEKerrEngine.CONTAINMENT_EUT) + " EU/t"
                    : BTSFormat.euT(supply) + " / " + BTSFormat.euT(MTEKerrEngine.CONTAINMENT_EUT)),
            -1,
            BTSText.lang(K + "containment.tip"),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "containment.supply", BTSFormat.euT(supply))));
        // QA-A04: Open and Auto refuse without bolts (the Seed would evaporate in seconds)
        final double bolts = multiblock.getBoltStockSm();
        c.add(
            bolts >= MTEKerrEngine.OPEN_MIN_BOLTS_SM,
            BTSText.lang(K + "chk.bolts"),
            BTSText.lit(MTEKerrEngine.fmt(bolts) + " / " + MTEKerrEngine.fmt(MTEKerrEngine.OPEN_MIN_BOLTS_SM) + " SM"),
            -1,
            BTSText.lang(K + "chk.bolts.tip"));
        // QA-A36: an open hole with no module linked only costs power and bolts
        int built = 0;
        for (KerrSlot slot : KerrSlot.VALUES) {
            if (!multiblock.slotStatus(slot)
                .startsWith("NOT BUILT")) built++;
        }
        if (built == 0) {
            c.add(
                BTSChecklist.State.WARN,
                BTSText.lang(K + "chk.nomodule"),
                BTSText.lit("0 / 2"),
                -1,
                BTSText.lang(K + "chk.nomodule.tip"));
        }
        rows.checks(c);
        rows.stat(
            BTSText.lang(K + "auto"),
            multiblock.isAutoSeeds() ? BTSText.cat(BTSText.lit("§a"), BTSText.lang(K + "auto.on"))
                : BTSText.cat(BTSText.lit("§8"), BTSText.lang(K + "auto.off")),
            BTSText.lang(K + "auto.tip"));
        chipsRow(rows);
    }

    /** The two module chips; hover one for its state, bonus or reason, and position. */
    private void chipsRow(BTSRows.Builder rows) {
        final List<String[]> chips = new ArrayList<>();
        for (KerrSlot slot : KerrSlot.VALUES) {
            final String[] f = multiblock.slotStatus(slot)
                .split("\\|", -1);
            final String state = f[0];
            final String name = BTSText.lang(K + "slot." + slot.key);
            final List<String> tip = new ArrayList<>();
            tip.add(BTSText.cat(BTSText.lit("§f"), name, BTSText.lit(" " + stateText(state)), stateLang(state)));
            tip.add(BTSText.cat(BTSText.lit("§7"), moduleDetail(slot, f)));
            final String pos = f.length > 3 ? f[3] : "";
            if (!pos.isEmpty()) tip.add(BTSText.cat(BTSText.lit("§8"), BTSText.lang(K + "mod.at", pos)));
            chips.add(new String[] { String.valueOf(dotColour(state)), name, BTSRows.Builder.tooltip(tip) });
        }
        rows.chips(chips);
    }

    private void notesRows(BTSRows.Builder rows) {
        // P4: the quasar link (only with a GRB Receptor Port fitted; the line carries its own colour)
        final String burst = multiblock.burstGuiLine();
        if (!burst.isEmpty()) {
            rows.note(
                BTSRows.ICON_NONE,
                BTSGuiKit.VALUE,
                BTSText.lit(burst),
                BTSText.lit(burst),
                BTSText.lang("beyondthestars.gui.kit.quasarlink.tip"));
        }
        final String migration = multiblock.getMigrationNote();
        if (!migration.isEmpty()) rows.note(BTSRows.ICON_NONE, BTSGuiKit.AQUA, BTSText.lit(migration));
        final String render = multiblock.getRenderNote();
        if (!render.isEmpty()) rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lit(render));
    }

    // ---------------------------------------------------------------------------- module state

    /** A slot state's chip colour (§5.2 row 8). */
    static int dotColour(String state) {
        if (state.startsWith("RUNNING")) return BTSGuiKit.OK;
        if (state.startsWith("IDLE")) return BTSGuiKit.AQUA;
        if (state.startsWith("PAUSED")) return BTSGuiKit.WARN;
        if (state.startsWith("NOT BUILT")) return DOT_DARK_GREY;
        return BTSGuiKit.BAD;
    }

    /** A slot state's text colour code on the terminal. */
    private static String stateText(String state) {
        if (state.startsWith("RUNNING")) return "§a";
        if (state.startsWith("IDLE")) return "§b";
        if (state.startsWith("PAUSED")) return "§6";
        if (state.startsWith("NOT BUILT")) return "§8";
        return "§c";
    }

    /** Side-panel (grey background) colours: the bright terminal ones do not read on GT's grey. */
    static EnumChatFormatting panelColour(String state) {
        if (state.startsWith("RUNNING")) return EnumChatFormatting.DARK_GREEN;
        if (state.startsWith("IDLE")) return EnumChatFormatting.DARK_AQUA;
        if (state.startsWith("PAUSED")) return EnumChatFormatting.GOLD;
        if (state.startsWith("NOT BUILT")) return EnumChatFormatting.DARK_GRAY;
        return EnumChatFormatting.DARK_RED;
    }

    /** The state's word as a lang reference (the family badges' words). */
    static String stateLang(String state) {
        if (state.startsWith("RUNNING")) return BTSText.lang("beyondthestars.gui.badge.running");
        if (state.startsWith("IDLE")) return BTSText.lang("beyondthestars.gui.badge.idle");
        if (state.startsWith("PAUSED")) return BTSText.lang("beyondthestars.gui.badge.paused");
        if (state.startsWith("NOT BUILT")) return BTSText.lang("beyondthestars.gui.badge.notbuilt");
        if (state.startsWith("NOT FORMED")) return BTSText.lang("beyondthestars.gui.badge.notformed");
        if (state.startsWith("NOT LOADED")) return BTSText.lang(K + "state.notloaded");
        return BTSText.lang("beyondthestars.gui.badge.notlinked");
    }

    /** The detail line of a module: its bonus when linked, else why it is not running (a lang reference). */
    static String moduleDetail(KerrSlot slot, String[] f) {
        final String state = f[0];
        if (state.startsWith("RUNNING") || state.startsWith("IDLE")) {
            return BTSText.lang(K + "mod.bonus", f.length > 1 ? f[1] : "0", f.length > 2 ? f[2] : "1");
        }
        if (state.startsWith("PAUSED")) {
            return BTSText.lang(state.contains("hole closed") ? K + "mod.paused.hole" : K + "mod.paused.kee");
        }
        if (state.startsWith("NOT BUILT")) return BTSText.lang(K + "mod.notbuilt", slot.hintDots);
        if (state.startsWith("NOT FORMED")) return BTSText.lang(K + "mod.notformed");
        if (state.startsWith("NOT LOADED")) return BTSText.lang(K + "mod.notloaded");
        return BTSText.lang(K + "mod.notlinked");
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

    // ================================================================================= Info page

    /** §5.2 Info: the questbook's RUN steps, the bolt table, T2, Auto. */
    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        rows.para(BTSText.lang(K + "info.run"), BTSGuiKit.VALUE);
        for (int i = 1; i <= 5; i++) rows.para(BTSText.lang(K + "info.run" + i), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(K + "info.bolts"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(K + "info.t2"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(K + "info.auto"), BTSGuiKit.VALUE);
    }

    // ============================================================================ button column

    /** The power switch does not stop the hole (B-52): TecTech's disabled icon (the kit), with this tooltip. */
    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
    }

    @Override
    protected String powerSwitchDisabledKey() {
        return K + "switch";
    }

    @Override
    protected void renderTooltip(RichTooltip t) {
        t.addLine(tr(K + "render.tip1"));
        t.addLine(EnumChatFormatting.GRAY + tr(K + "render.tip2"));
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

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    private static int clampSetpoint(int v, IntSyncValue max) {
        return Math
            .max(MTEKerrEngine.SETPOINT_MIN, Math.min(Math.max(MTEKerrEngine.SETPOINT_MIN, max.getIntValue()), v));
    }

    /**
     * The setpoint as a whole number of SM: a number field bounded by the tier's synced range (scroll: 1, shift 8,
     * ctrl 1, alt 64), flanked by − / +. The server clamps again.
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
                t -> t.addLine(tr(K + "setpoint.tip1"))
                    .addLine(EnumChatFormatting.GRAY + tr(K + "setpoint.tip2"))
                    .addLine(EnumChatFormatting.GRAY + tr(K + "setpoint.tip3")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Left: today's Setpoint stepper (§5.2 gap row): − field + SM; right-click steps 8. */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue setpoint = syncManager.findSyncHandler("kerrSetpoint", IntSyncValue.class);
        final IntSyncValue max = syncManager.findSyncHandler("kerrMaxSetpoint", IntSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.lang(K + "setpoint")
                    .color(BTSGuiKit.PANEL_TEXT)
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
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .marginLeft(1)
                    .heightRel(1f));
    }

    private static IWidget stepButton(IntSyncValue setpoint, IntSyncValue max, int step) {
        return new ButtonWidget<>().size(9, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final int next = clampSetpoint(setpoint.getIntValue() + (button == 1 ? step * 8 : step), max);
                if (next != setpoint.getIntValue()) setpoint.setIntValue(next);
                return true;
            })
            .tooltip(t -> t.addLine(tr(step < 0 ? K + "setpoint.minus" : K + "setpoint.plus")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Right, on screen left to right: [Hole control] [Modules] [i] (the row is reversed: first added is right). */
    @Override
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler[] panels = new IPanelHandler[2];
        panels[0] = syncManager
            .syncedPanel("kerrHolePanel", true, (sub, handler) -> buildHolePanel(syncManager, parent));
        panels[1] = syncManager
            .syncedPanel("kerrModulesPanel", true, (sub, handler) -> buildModulesPanel(syncManager, parent));
        row.child(panelButton(panels, 1, GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS, K + "button.modules"))
            .child(panelButton(panels, 0, GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED, K + "button.hole"));
    }

    /** Toggles side panel {@code which}; opening it closes the other (they share one spot). */
    private static IWidget panelButton(IPanelHandler[] panels, int which, IDrawable icon, String tooltipKey) {
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
            .tooltip(t -> t.addLine(IKey.lang(tooltipKey)))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ======================================================================= hole control panel

    /** One line of panel text at scale 1.0 that scrolls on hover instead of wrapping. */
    private static ScrollingTextWidget panelLine(Supplier<String> text, int width) {
        return new ScrollingTextWidget(IKey.dynamic(text::get)).width(width)
            .height(9)
            .color(BTSGuiKit.PANEL_TEXT)
            .shadow(false);
    }

    /** Wrapped panel text at scale 1.0, {@code lines} lines high. */
    private static TextWidget<?> panelPara(Supplier<String> text, int width, int lines) {
        return IKey.dynamic(text::get)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .textAlign(Alignment.TopLeft)
            .size(width, 9 * lines);
    }

    /** "label ......... value" across the panel, both at 1.0. */
    private static IWidget keyValue(Supplier<String> label, Supplier<String> value, int width) {
        return Flow.row()
            .width(width)
            .height(9)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(
                IKey.dynamic(label::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget())
            .child(
                IKey.dynamic(value::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget());
    }

    private static IWidget scaleLabel(Supplier<String> text) {
        return IKey.dynamic(text::get)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .height(9);
    }

    /** The Hole control panel (§5.2): today's controls, every text at 1.0, 150 x 196, the hint wrapped at 140. */
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
        final Supplier<String> modules = () -> {
            final int n = linked.getIntValue();
            return n == 1 ? tr(K + "panel.modules.one") : tr(K + "panel.modules.many", n);
        };

        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.lang(K + "panel.hole.title")
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .style(EnumChatFormatting.BOLD)
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(
                keyValue(
                    () -> tr(K + "setpoint"),
                    () -> tr(K + "panel.range", MTEKerrEngine.SETPOINT_MIN, clampSetpoint(Integer.MAX_VALUE, max)),
                    w))
            // The slider runs 0..1 over the tier's CURRENT range (it follows a Density Upgrade going in
            // while the panel is open); it snaps to whole SM and sends only actual changes.
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(() -> toSlider(setpoint.getIntValue(), max), v -> {
                        final int i = fromSlider(v, max);
                        if (i != setpoint.getIntValue()) setpoint.setIntValue(i);
                    }))
                    // GT's theme gives a slider no track (dev client round 1: only the handle showed)
                    .background((IDrawable) (context, x, y, width, height, theme) -> {
                        GuiDraw.drawRect(x, y, width, height, 0xFF373737);
                        GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
                    })
                    .size(w, 10))
            .child(
                Flow.row()
                    .width(w)
                    .height(9)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(scaleLabel(() -> String.valueOf(MTEKerrEngine.SETPOINT_MIN)))
                    .child(scaleLabel(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max) / 4)))
                    .child(scaleLabel(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max) / 2)))
                    .child(scaleLabel(() -> String.valueOf(clampSetpoint(Integer.MAX_VALUE, max)))))
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
                            .color(BTSGuiKit.PANEL_TEXT)
                            .asWidget()
                            .heightRel(1f)))
            .child(
                Flow.row()
                    .width(w)
                    .height(9)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(
                        IKey.dynamic(() -> tr(K + "right", density.getBoolValue() ? 2 : 1))
                            .color(BTSGuiKit.PANEL_TEXT)
                            .asWidget())
                    .child(
                        IKey.dynamic(() -> tr(density.getBoolValue() ? K + "panel.density.t2" : K + "panel.density.t1"))
                            .color(BTSGuiKit.PANEL_TEXT)
                            .asWidget())
                    .tooltipDynamic(
                        t -> t.addLine(
                            tr(density.getBoolValue() ? K + "panel.density.tip.t2" : K + "panel.density.tip.t1")))
                    .tooltipAutoUpdate(true)
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(keyValue(() -> tr(K + "panel.seeds"), () -> String.valueOf(seeds.getIntValue()), w))
            .child(keyValue(() -> tr(K + "panel.collapsers"), () -> String.valueOf(collapsers.getIntValue()), w))
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
                                        : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.open")))
                            .onMousePressed(button -> {
                                if (canOpen.getAsBoolean()) main.callSyncedAction("kerrOpenHole", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.open.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.open.tip2"))))
                    .child(
                        new ButtonWidget<>().size(half, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (armed.getAsBoolean()) return EnumChatFormatting.RED + tr(K + "panel.confirm");
                                return (canCollapse.getAsBoolean() ? EnumChatFormatting.WHITE
                                    : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.collapse");
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
                                t -> t.addLine(tr(K + "panel.collapse.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.collapse.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.collapse.tip3")))))
            .child(panelPara(() -> {
                if (armed.getAsBoolean()) {
                    return EnumChatFormatting.DARK_RED + tr(K + "panel.hint.armed", modules.get());
                }
                if (!open.getBoolValue()) return EnumChatFormatting.DARK_GRAY + tr(K + "panel.hint.closed");
                if (!formed.getBoolValue()) return EnumChatFormatting.GOLD + tr(K + "panel.hint.frozen");
                if (auto.getBoolValue()) return EnumChatFormatting.GOLD + tr(K + "panel.hint.auto");
                return tr(K + "panel.hint.twice", modules.get());
            }, w - 2, 2))
            .child(
                Flow.row()
                    .width(w)
                    .height(18)
                    .childPadding(3)
                    .child(
                        new ToggleButton().size(12, 12)
                            .value(new BoolValue.Dynamic(auto::getBoolValue, auto::setBoolValue))
                            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.auto.tip1"))
                                    .addLine(tr(K + "panel.auto.tip2"))
                                    .addLine(tr(K + "panel.auto.tip3"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.auto.tip4")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(panelPara(() -> tr(K + "panel.auto"), w - 15, 2)));

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

    private static String[] fields(StringSyncValue v) {
        return v.getStringValue()
            .split("\\|", -1);
    }

    /** The Modules panel (§5.2): one 32 px row per slot, text at 1.0 (long lines scroll on hover). */
    private ModularPanel buildModulesPanel(PanelSyncManager main, ModularPanel parent) {
        final int w = MODULES_PANEL_W - 8;
        final int textW = w - 21;
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.lang(K + "panel.modules.title")
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .style(EnumChatFormatting.BOLD)
                    .width(w)
                    .textAlign(Alignment.Center));
        for (KerrSlot slot : KerrSlot.VALUES) {
            final StringSyncValue v = main.findSyncHandler("kerrSlot." + slot.key, StringSyncValue.class);
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
                            .child(panelLine(() -> {
                                final String state = fields(v)[0];
                                return tr(K + "slot." + slot.key) + " "
                                    + panelColour(state)
                                    + BTSText.resolve(stateLang(state));
                            }, textW))
                            .child(panelLine(() -> BTSText.resolve(moduleDetail(slot, fields(v))), textW))
                            .child(panelLine(() -> {
                                final String[] f = fields(v);
                                final String pos = f.length > 3 ? f[3] : "";
                                return EnumChatFormatting.DARK_GRAY + (pos.isEmpty() ? "" : tr(K + "mod.at", pos));
                            }, textW))));
        }
        column.child(panelPara(() -> EnumChatFormatting.DARK_GRAY + tr(K + "panel.modules.footer"), w, 2));
        return new ModularPanel("kerrModulesPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(MODULES_PANEL_W, 8 + 11 + (MODULE_ROW_H + 2) * KerrSlot.VALUES.length + 18 + 2)
            .child(column);
    }
}
