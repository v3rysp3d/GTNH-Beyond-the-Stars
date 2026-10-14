package com.nateyburr.beyondthestars.machines.conduit;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
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
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * P4: the conduit elements' console on the controller GUI kit (controller-gui-spec.md §5.5, B-116): the Wormhole Mouth,
 * the Conduit Mirror and the Conduit Junction. The header says NOT FORMED, CLOSED, OPENING (the 10 s of paid upkeep
 * counting) or OPEN, the mouth's pairing and the throat's Mk. An open element shows the stream through it against the
 * throat cap, the upkeep, and per element the partner and the ring (Mouth), the two faces (Mirror) or the mode and
 * the three legs (Junction), each out-throat with what its ray reaches; a closed one a checklist of what opening
 * needs. The gap row keeps the fuel cycle and, on the Junction, its split / combine panel; <b>i</b> has the
 * questbook's words. Every setter is clamped on the server.
 */
public class ConduitElementGui<T extends MTEConduitElement<?>> extends BTSControllerGui<T> {

    private static final String K = "beyondthestars.gui.conduit.";
    private static final int PANEL_W = 170;
    private static final int PANEL_H = 186;
    /** The limit field's top: Mk IV's throat cap (anything above changes nothing). */
    private static final double MAX_LIMIT = BurstRouting.throatCap(BurstRouting.MK_MAX);

    public ConduitElementGui(T multiblock) {
        super(multiblock, false, null);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        final MTEConduitElement<?> m = multiblock;
        syncManager.syncValue("cFuel", new IntSyncValue(m::getFuelModeOrdinal, m::setFuelModeOrdinal).allowC2S());
        if (m instanceof MTEConduitJunction) {
            final MTEConduitJunction j = (MTEConduitJunction) m;
            syncManager.syncValue("jMode", new IntSyncValue(j::getModeOrdinal, j::setModeOrdinal).allowC2S());
            syncManager.syncValue("jRatio", new DoubleSyncValue(j::getRatio, j::setRatio).allowC2S());
            syncManager.syncValue("jGain", new IntSyncValue(j::getGainIndex, j::setGainIndex).allowC2S());
            syncManager.syncValue("jOut3", new DoubleSyncValue(j::getOut3Share, j::setOut3Share).allowC2S());
            syncManager.syncValue("jLimit", new DoubleSyncValue(j::getLimit, j::setLimit).allowC2S());
        }
    }

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    private static String bu(double v) {
        return MTEConduitElement.bu(v);
    }

    // =============================================================================== the header

    @Override
    protected Badge badgeCode() {
        final MTEConduitElement<?> m = multiblock;
        if (!m.isFormed()) return Badge.NOT_FORMED;
        if (m.isOpen()) return Badge.OPEN;
        return m.throatMk() > 0 && m.getReopen() > 0 ? Badge.OPENING : Badge.CLOSED;
    }

    @Override
    protected String subject() {
        if (badgeCode() == Badge.OPENING) {
            return BTSText.lang(K + "opening", multiblock.getReopen(), BurstRouting.REOPEN_SECONDS);
        }
        if (multiblock instanceof MTEWormholeMouth) {
            return BTSText.lang(
                ((MTEWormholeMouth) multiblock).partnerProblem()
                    .isEmpty() ? K + "paired" : K + "notpaired");
        }
        return "";
    }

    @Override
    protected String rightChip() {
        final int mk = multiblock.throatMk();
        return mk == 0 ? BTSText.lang(K + "nothroat") : BTSText.lang(K + "mk", MTEConduitElement.roman(mk));
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        final int mk = multiblock.throatMk();
        out.add(
            mk == 0 ? BTSText.lang(K + "mk.tip0")
                : BTSText.lang(K + "mk.tip", MTEConduitElement.roman(mk), BTSFormat.buEu(BurstRouting.throatCap(mk))));
        out.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "mk.tip2")));
        return out;
    }

    // ============================================================================= status rows

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        final Badge b = badgeCode();
        if (b == Badge.NOT_FORMED) {
            rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "notformed"));
        } else if (b == Badge.OPEN) {
            openRows(rows);
        } else {
            closedRows(rows);
        }
        final String last = multiblock.lastNote();
        if (!last.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", last));
        }
    }

    private void openRows(BTSRows.Builder rows) {
        final MTEConduitElement<?> m = multiblock;
        final double cap = Math.max(1e-9, m.throatCap());
        final double through = m.throughBu(), clipped = m.clippedBu(), lost = m.lostBu();
        rows.bar(
            BTSText.lang(K + "through"),
            through / cap,
            BTSText.lit(bu(through) + " Bu/t"),
            Grad.MAGENTA,
            null,
            BTSText.lang(K + "through.tip", BTSFormat.buEu(through), BTSFormat.buEuShort(cap)),
            BTSText.cat(
                BTSText.lit("§7"),
                BTSText.lang(K + "through.tip2", BTSFormat.buEuShort(clipped), BTSFormat.buEuShort(lost))));
        rows.stat(
            BTSText.lang("beyondthestars.gui.kit.eut"),
            BTSText.lit(BTSFormat.euValue(through)),
            BTSText.lang("beyondthestars.gui.kit.eut.tip"));
        if (clipped > 0.005) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(K + "clipped", BTSFormat.buEuShort(clipped)),
                BTSText.lang(K + "clipped.tip"));
        }
        if (lost > 0.005) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(K + "lost", BTSFormat.buEuShort(lost)),
                BTSText.lang(K + "lost.tip"));
        }
        upkeepRow(rows);
        if (m instanceof MTEWormholeMouth) {
            final MTEWormholeMouth mouth = (MTEWormholeMouth) m;
            partnerRow(rows, mouth);
            rows.stat(
                BTSText.lang(K + "in.ring"),
                BTSText.lit(bu(m.inBu(0)) + " Bu/t §7= §f" + BTSFormat.euOfBu(m.inBu(0))),
                BTSText.lang(K + "in.ring.tip"));
            routeRow(rows, K + "out.back", 0);
        } else if (m instanceof MTEConduitMirror) {
            rows.stat(
                BTSText.lang(K + "in.front"),
                BTSText.lit(bu(m.inBu(0)) + " Bu/t §7= §f" + BTSFormat.euOfBu(m.inBu(0))),
                BTSText.lang(K + "in.mirror.tip"));
            rows.stat(
                BTSText.lang(K + "in.side"),
                BTSText.lit(bu(m.inBu(1)) + " Bu/t §7= §f" + BTSFormat.euOfBu(m.inBu(1))),
                BTSText.lang(K + "in.mirror.tip"));
            routeRow(rows, K + "out.side", 1);
            routeRow(rows, K + "out.front", 0);
        } else if (m instanceof MTEConduitJunction) {
            junctionRows(rows, (MTEConduitJunction) m);
        }
    }

    private void partnerRow(BTSRows.Builder rows, MTEWormholeMouth mouth) {
        final String problem = mouth.partnerProblem();
        final int[] p = mouth.getPartner();
        rows.stat(
            BTSText.lang(K + "partner"),
            problem.isEmpty() && p != null ? BTSText.lit(p[1] + ", " + p[2] + ", " + p[3])
                : BTSText.lit("§c" + (p == null ? tr(K + "partner.none") : tr(K + "partner.bad"))),
            problem.isEmpty() && p != null ? BTSText.lang(K + "partner.tip", MTEWormholeMouth.label(p))
                : BTSText.lit("§c" + problem),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "partner.how")));
    }

    private void junctionRows(BTSRows.Builder rows, MTEConduitJunction j) {
        final boolean split = j.getMode() == MTEConduitJunction.Mode.SPLIT;
        if (split) {
            final int s = j.getLastLevel();
            rows.stat(
                BTSText.lang(K + "mode"),
                BTSText.lit(
                    tr(
                        K + "mode.split.v",
                        String.format(Locale.ROOT, "%.2f", j.getRatio()),
                        MTEConduitJunction.fmtGain(j.getGain()))),
                BTSText.lang(
                    K + "mode.split.tip",
                    String.format(Locale.ROOT, "%.2f", j.getRatio()),
                    MTEConduitJunction.fmtGain(j.getGain()),
                    s < 0 ? tr(K + "mode.nohatch") : String.valueOf(s),
                    String.format(Locale.ROOT, "%.2f", j.share()),
                    String.format(Locale.ROOT, "%.0f", 100 * j.getOut3Share())),
                j.getLimit() > 0 ? BTSText.lang(K + "limit.tip", BTSFormat.buEu(j.getLimit()))
                    : BTSText.lang(K + "limit.none"));
            rows.stat(
                BTSText.lang(K + "in.big"),
                BTSText.lit(bu(j.inBu(0)) + " Bu/t §7= §f" + BTSFormat.euOfBu(j.inBu(0))),
                BTSText.lit(BTSFormat.buEu(j.inBu(0))));
            routeRow(rows, K + "out.1", 1);
            routeRow(rows, K + "out.2", 2);
            routeRow(rows, K + "out.3", 3);
        } else {
            rows.stat(
                BTSText.lang(K + "mode"),
                BTSText.lang(K + "mode.combine"),
                BTSText.lang(K + "mode.combine.tip", bu(j.inBu(1)), bu(j.inBu(2)), bu(j.inBu(3))));
            final double small = j.inBu(1) + j.inBu(2) + j.inBu(3);
            rows.stat(
                BTSText.lang(K + "in.small"),
                BTSText.lit(bu(j.inBu(1)) + " + " + bu(j.inBu(2)) + " + " + bu(j.inBu(3))),
                BTSText.lit(BTSFormat.buEu(small)));
            rows.stat(
                BTSText.lang("beyondthestars.gui.kit.eut"),
                BTSText.lit(BTSFormat.euValue(small)),
                BTSText.lang("beyondthestars.gui.kit.eut.tip"));
            routeRow(rows, K + "out.big", 0);
        }
    }

    /** An out-throat: its Bu/t and what the ray reaches (aqua), or where it is blocked (red). */
    private void routeRow(BTSRows.Builder rows, String labelKey, int out) {
        final ConduitNet.Route r = multiblock.lastRoute(out);
        final double flowBu = multiblock.outBu(out);
        final String flow = bu(flowBu) + " §7(" + BTSFormat.euOfBu(flowBu) + ")§f";
        final String where;
        final String tip;
        if (r == null) {
            where = "§8" + tr(K + "route.none");
            tip = tr(K + "route.none.tip");
        } else if (r.kind == ConduitNet.Kind.ELEMENT || r.kind == ConduitNet.Kind.RECEPTOR) {
            // "Conduit Mirror at 20265, 62, 133 (dim 0)" reads "Conduit Mirror": the place is in the tooltip
            final int at = r.label.indexOf(" at ");
            where = "§b" + (at > 0 ? r.label.substring(0, at) : r.label);
            tip = r.describe();
        } else {
            where = "§c" + tr(r.kind == ConduitNet.Kind.BLOCKED ? K + "route.blocked" : K + "route.lost");
            tip = r.describe();
        }
        rows.stat(
            BTSText.lang(labelKey),
            BTSText.lit("§f" + flow + " " + where),
            BTSText.lit(BTSFormat.buEu(flowBu) + " -> " + tip));
    }

    private void upkeepRow(BTSRows.Builder rows) {
        final MTEConduitElement<?> m = multiblock;
        final String line = m.getFuelLine();
        final boolean shortFuel = line.isEmpty() || line.endsWith("SHORT");
        final String v = line.isEmpty() ? "-" : line.replace(" - SHORT", "");
        rows.stat(
            BTSText.lang(K + "upkeep"),
            BTSText.lit(shortFuel ? "§c" + tr(K + "upkeep.short") : "§a" + v),
            BTSText.lang(K + "upkeep.tip", m.getFuelMode().label, v),
            BTSText.cat(
                BTSText.lit("§7"),
                BTSText.lang(
                    K + "upkeep.used",
                    m.fuelHatchCount(),
                    String.format(Locale.ROOT, "%,d", m.getLitresSpaceTime()),
                    String.format(Locale.ROOT, "%,d", m.getLitresErgomatter()))));
    }

    /** §5.5 Closed: what opening needs. */
    private void closedRows(BTSRows.Builder rows) {
        final MTEConduitElement<?> m = multiblock;
        rows.section(BTSText.lang(K + "sec.open", BurstRouting.REOPEN_SECONDS), "");
        final BTSChecklist c = new BTSChecklist();
        final int mk = m.throatMk();
        c.add(
            mk > 0,
            BTSText.lang(K + "chk.throat"),
            mk > 0 ? BTSText.lang(K + "mk", MTEConduitElement.roman(mk)) : BTSText.lang(K + "chk.empty"),
            -1,
            BTSText.lang(K + "chk.throat.tip"));
        final int hatches = m.fuelHatchCount();
        c.add(
            hatches > 0,
            BTSText.lang(K + "chk.hatch"),
            BTSText.lit(String.valueOf(hatches)),
            -1,
            BTSText.lang(K + "chk.hatch.tip"));
        final String line = m.getFuelLine();
        final boolean paying = mk > 0 && !line.isEmpty() && !line.endsWith("SHORT");
        final int r = m.getReopen();
        c.add(
            paying ? BTSChecklist.State.WARN : BTSChecklist.State.BAD,
            BTSText.lang(K + "chk.upkeep"),
            BTSText.lit(r + " / " + BurstRouting.REOPEN_SECONDS + " s"),
            r / (float) BurstRouting.REOPEN_SECONDS,
            BTSText.lang(K + "chk.upkeep.tip", m.getFuelMode().label),
            line.isEmpty() ? "" : BTSText.lit("§7" + line));
        rows.checks(c);
        if (m instanceof MTEWormholeMouth) partnerRow(rows, (MTEWormholeMouth) m);
    }

    // ================================================================================= Info page

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        final MTEConduitElement<?> m = multiblock;
        if (m instanceof MTEWormholeMouth) rows.para(BTSText.lang(K + "info.mouth"), BTSGuiKit.VALUE);
        else if (m instanceof MTEConduitMirror) rows.para(BTSText.lang(K + "info.mirror"), BTSGuiKit.VALUE);
        else if (m instanceof MTEConduitJunction) rows.para(BTSText.lang(K + "info.junction"), BTSGuiKit.VALUE);
        for (String k : new String[] { "throats", "upkeep", "rays", "auto" }) {
            rows.para(BTSText.lang(K + "info." + k), BTSGuiKit.VALUE);
        }
    }

    // ============================================================================ buttons

    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
    }

    @Override
    protected String powerSwitchDisabledKey() {
        return K + "switch";
    }

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

    /** Right, on screen left to right: [Junction panel (Junction only)] [fuel cycle] [i] (the row is reversed). */
    @Override
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue fuel = syncManager.findSyncHandler("cFuel", IntSyncValue.class);
        row.child(
            new ButtonWidget<>().size(18, 18)
                .marginLeft(2)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                .onMousePressed(b -> {
                    fuel.setIntValue(
                        Math.floorMod(
                            fuel.getIntValue() + (b == 1 ? -1 : 1),
                            MTEConduitElement.FuelMode.VALUES.length));
                    return true;
                })
                .tooltipDynamic(t -> {
                    t.addLine(tr(K + "fuel.tip", MTEConduitElement.FuelMode.of(fuel.getIntValue()).label));
                    for (MTEConduitElement.FuelMode f : MTEConduitElement.FuelMode.VALUES) {
                        t.addLine(EnumChatFormatting.GRAY + f.label + ": " + f.help);
                    }
                    t.addLine(EnumChatFormatting.GRAY + tr(K + "fuel.tip2"));
                })
                .tooltipAutoUpdate(true)
                .tooltipShowUpTimer(TOOLTIP_DELAY));
        if (!(multiblock instanceof MTEConduitJunction)) return;
        final IPanelHandler panel = syncManager
            .syncedPanel("jSplitPanel", true, (sub, handler) -> buildJunctionPanel(syncManager, parent));
        row.child(
            BTSGuiKit.panelButton(GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED, panel, K + "button.junction"));
    }

    // ============================================================================ junction panel

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

    private static ScrollingTextWidget panelLine(Supplier<String> text, int width) {
        return new ScrollingTextWidget(IKey.dynamic(text::get)).width(width)
            .height(9)
            .color(BTSGuiKit.PANEL_TEXT)
            .shadow(false);
    }

    private static TextWidget<?> panelPara(Supplier<String> text, int width, int lines) {
        return IKey.dynamic(text::get)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .textAlign(Alignment.TopLeft)
            .size(width, 9 * lines);
    }

    private static final IDrawable SLIDER_TRACK = (context, x, y, width, height, theme) -> {
        GuiDraw.drawRect(x, y, width, height, 0xFF373737);
        GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
    };

    private ModularPanel buildJunctionPanel(PanelSyncManager main, ModularPanel parent) {
        final IntSyncValue mode = main.findSyncHandler("jMode", IntSyncValue.class);
        final DoubleSyncValue ratio = main.findSyncHandler("jRatio", DoubleSyncValue.class);
        final IntSyncValue gain = main.findSyncHandler("jGain", IntSyncValue.class);
        final DoubleSyncValue out3 = main.findSyncHandler("jOut3", DoubleSyncValue.class);
        final DoubleSyncValue limit = main.findSyncHandler("jLimit", DoubleSyncValue.class);
        final int w = PANEL_W - 8;
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.lang(K + "panel.title")
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .style(EnumChatFormatting.BOLD)
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        new ButtonWidget<>().size(14, 14)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .onMousePressed(b -> {
                                mode.setIntValue(1 - Math.floorMod(mode.getIntValue(), 2));
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.mode.tip1"))
                                    .addLine(tr(K + "panel.mode.tip2")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(
                        panelLine(
                            () -> tr(
                                K + "panel.mode",
                                tr(mode.getIntValue() == 0 ? K + "panel.split" : K + "panel.combine")),
                            w - 17)))
            .child(
                keyValue(
                    () -> tr(K + "panel.ratio"),
                    () -> String.format(Locale.ROOT, "%.2f", ratio.getDoubleValue()),
                    w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(ratio::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != ratio.getDoubleValue()) ratio.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        new ButtonWidget<>().size(14, 14)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .onMousePressed(b -> {
                                gain.setIntValue(Math.floorMod(gain.getIntValue() + 1, BurstRouting.GAINS.length));
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.gain.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.gain.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.gain.tip3")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(
                        panelLine(
                            () -> tr(
                                K + "panel.gain",
                                MTEConduitJunction.fmtGain(
                                    BurstRouting.GAINS[Math.floorMod(gain.getIntValue(), BurstRouting.GAINS.length)])),
                            w - 17)))
            .child(
                keyValue(
                    () -> tr(K + "panel.out3"),
                    () -> String.format(Locale.ROOT, "%.0f %%", 100 * out3.getDoubleValue()),
                    w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(out3::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != out3.getDoubleValue()) out3.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10))
            .child(
                // P4-88: out 1's limit, Bu/t (0 = none); the Eye's J1 takes 7.5 per parallel
                Flow.row()
                    .width(w)
                    .height(12)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(panelLine(() -> tr(K + "panel.limit"), w - 63))
                    .child(
                        new TextFieldWidget().numbersDouble(v -> Math.max(0, Math.min(MAX_LIMIT, v)))
                            .value(new DoubleValue.Dynamic(limit::getDoubleValue, v -> {
                                final double l = Math.max(0, Math.min(MAX_LIMIT, v));
                                if (l != limit.getDoubleValue()) limit.setDoubleValue(l);
                            }))
                            .setTextAlignment(Alignment.Center)
                            .size(60, 12)
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.limit.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.limit.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.limit.tip3")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY)))
            .child(panelPara(() -> EnumChatFormatting.DARK_GRAY + tr(K + "panel.hint"), w, 4));
        return new ModularPanel("jSplitPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(PANEL_W, PANEL_H)
            .child(column);
    }
}
