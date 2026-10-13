package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Locale;

import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSHatchGui;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;

/**
 * P4: the GRB Receptor Port's GUI on the hatch layout (controller-gui-spec.md §5.10, B-116): a readout box with the
 * machine it feeds, a LINKED lamp and last second's Bu/t, and the reading by source (or the Q-8 pause). Nothing to set.
 */
public class GRBReceptorGui extends BTSHatchGui<MTEHatchGRBReceptor> {

    private static final String K = "beyondthestars.gui.receptor.";
    private static final int Y_HOST = 5, Y_LAST = 16, Y_LINE = 27;

    private StringSyncValue host, reading;
    private DoubleSyncValue total, lifetime;
    private BooleanSyncValue linked, paused;

    public GRBReceptorGui(MTEHatchGRBReceptor hatch) {
        super(hatch);
    }

    @Override
    protected int readoutHeight() {
        return 38;
    }

    @Override
    protected int panelHeight() {
        return 166;
    }

    @Override
    protected void addReadout(ParentWidget<?> box, PanelSyncManager syncManager) {
        // one reading a sync: the server reads the port once for all four values (reading() forgets stale sources)
        final BurstReading[] last = { BurstReading.EMPTY };
        host = new StringSyncValue(machine::getHost);
        total = new DoubleSyncValue(() -> {
            last[0] = machine.reading();
            return Math.round(last[0].total * 100) / 100.0;
        });
        linked = new BooleanSyncValue(() -> last[0].linked());
        paused = new BooleanSyncValue(() -> last[0].paused);
        reading = new StringSyncValue(() -> last[0].line());
        lifetime = new DoubleSyncValue(machine::getLifetime);
        syncManager.syncValue("btsRcpTotal", total);
        syncManager.syncValue("btsRcpHost", host);
        syncManager.syncValue("btsRcpLinked", linked);
        syncManager.syncValue("btsRcpPaused", paused);
        syncManager.syncValue("btsRcpReading", reading);
        syncManager.syncValue("btsRcpLife", lifetime);

        box.child(line(Y_HOST, () -> tr(K + "port"), BTSGuiKit.VALUE, () -> {
            final String h = host.getStringValue();
            return h.isEmpty() ? "§6" + tr(K + "nohost") : h;
        }, BTSGuiKit.LABEL).tooltip(t -> t.addLine(tr(K + "host.tip"))));
        box.child(
            lampLine(
                Y_LAST,
                () -> paused.getBoolValue() ? BTSGuiKit.WARN
                    : linked.getBoolValue() ? BTSGuiKit.LAMP_ON : BTSGuiKit.LAMP_OFF,
                () -> paused.getBoolValue() ? "§6" + tr(K + "paused")
                    : linked.getBoolValue() ? tr(K + "linked") : "§7" + tr(K + "notlinked"),
                () -> tr(K + "last", String.format(Locale.ROOT, "%.2f", total.getDoubleValue()))).tooltipDynamic(
                    t -> t.addLine(tr(K + "linked.tip"))
                        .addLine(
                            "§7" + tr(K + "lifetime", String.format(Locale.ROOT, "%,.0f", lifetime.getDoubleValue()))))
                    .tooltipAutoUpdate(true));
        box.child(
            line(Y_LINE, () -> reading.getStringValue(), BTSGuiKit.DIM, null, 0)
                .tooltipDynamic(t -> t.addLine(reading.getStringValue()))
                .tooltipAutoUpdate(true));
    }

    @Override
    protected void addControls(Flow column, PanelSyncManager syncManager) {}
}
