package com.nateyburr.beyondthestars.machines.conduit;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSHatchGui;

/**
 * P4: the Burst Control Hatch's GUI on the hatch layout (controller-gui-spec.md §5.10, B-116). The readout box says the
 * mode and the machine reading the hatch, the live redstone level as a 0-15 bar, and what the host does with it ("b =
 * 1.83", "purge armed"). Under it the Mode cycle (its tooltip now lists the four modes and what each does), the mode's
 * help and the rising-edge count.
 */
public class BurstControlHatchGui extends BTSHatchGui<MTEHatchBurstControl> {

    private static final String K = "beyondthestars.gui.burst.";
    private static final int Y_MODE = 5, Y_BAR = 16, Y_STATUS = 27;

    private IntSyncValue mode, level, edges;
    private StringSyncValue host, hostLine;

    public BurstControlHatchGui(MTEHatchBurstControl hatch) {
        super(hatch);
    }

    @Override
    protected int readoutHeight() {
        return 38;
    }

    @Override
    protected int panelHeight() {
        return 196;
    }

    private MTEHatchBurstControl.Mode mode() {
        return MTEHatchBurstControl.Mode.of(mode.getIntValue());
    }

    /** The mode's short name for the readout and the Mode row. */
    static String modeName(MTEHatchBurstControl.Mode m) {
        return tr(
            K + "mode."
                + m.name()
                    .toLowerCase(java.util.Locale.ROOT));
    }

    static String modeHelp(MTEHatchBurstControl.Mode m) {
        return tr(
            K + "help."
                + m.name()
                    .toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    protected void addReadout(ParentWidget<?> box, PanelSyncManager syncManager) {
        mode = new IntSyncValue(machine::getModeOrdinal, machine::setModeOrdinal).allowC2S();
        level = new IntSyncValue(machine::level);
        edges = new IntSyncValue(machine::getEdges);
        host = new StringSyncValue(machine::getHost);
        hostLine = new StringSyncValue(machine::getHostLine);
        syncManager.syncValue("btsBurstMode", mode);
        syncManager.syncValue("btsBurstLevel", level);
        syncManager.syncValue("btsBurstEdges", edges);
        syncManager.syncValue("btsBurstHost", host);
        syncManager.syncValue("btsBurstHostLine", hostLine);

        box.child(line(Y_MODE, () -> modeName(mode()), BTSGuiKit.VALUE, () -> {
            final String h = host.getStringValue();
            return h.isEmpty() ? "§6" + tr(K + "nohost") : h;
        }, BTSGuiKit.LABEL).tooltipDynamic(this::hostTooltip)
            .tooltipAutoUpdate(true));
        box.child(
            barLine(
                Y_BAR,
                () -> level.getIntValue() / 15.0,
                () -> Double.NaN,
                () -> BTSGuiKit.Grad.CYAN,
                () -> (level.getIntValue() > 0 ? "§a" : "§8") + level.getIntValue()));
        box.child(line(Y_STATUS, () -> {
            final String h = hostLine.getStringValue();
            if (host.getStringValue()
                .isEmpty()) return "§8" + tr(K + "status.none");
            return tr(K + "status", level.getIntValue()) + (h.isEmpty() ? "" : " > §b" + h);
        }, BTSGuiKit.LABEL, null, 0));
    }

    private void hostTooltip(RichTooltip t) {
        if (host.getStringValue()
            .isEmpty()) {
            t.addLine(tr(K + "nohost.tip"));
        } else {
            t.addLine(tr(K + "host.tip", host.getStringValue()));
        }
    }

    @Override
    protected void addControls(Flow column, PanelSyncManager syncManager) {
        column.child(
            cycleRow(mode, MTEHatchBurstControl.Mode.VALUES.length, () -> tr(K + "moderow", modeName(mode())), t -> {
                t.addLine(tr(K + "mode.tip"));
                for (MTEHatchBurstControl.Mode m : MTEHatchBurstControl.Mode.VALUES) {
                    t.addLine((m == mode() ? "§f> " : "§7  ") + modeName(m) + ": " + modeHelp(m));
                }
                t.addLine("§7" + tr(K + "mode.tip2"));
            }));
        column.child(
            IKey.dynamic(() -> modeHelp(mode()))
                .color(0xFF555555)
                .asWidget()
                .textAlign(Alignment.TopLeft)
                .size(176 - CONTROL_X - 7, 18));
        column.child(
            IKey.dynamic(() -> tr(K + "edges", edges.getIntValue()))
                .color(BTSGuiKit.PANEL_TEXT)
                .asWidget()
                .height(9)
                .tooltip(t -> t.addLine(tr(K + "edges.tip"))));
    }
}
