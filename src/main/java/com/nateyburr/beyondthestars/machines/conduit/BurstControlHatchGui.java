package com.nateyburr.beyondthestars.machines.conduit;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/**
 * P4: the Burst Control Hatch's GUI (MUI2, laid out like the Directive Hatch's): the machine it sits in, the mode with
 * what it does, the live redstone level and the line the host reports.
 */
public class BurstControlHatchGui extends MTEHatchBaseGui<MTEHatchBurstControl> {

    public BurstControlHatchGui(MTEHatchBurstControl hatch) {
        super(hatch);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        final IntSyncValue mode = new IntSyncValue(machine::getModeOrdinal, machine::setModeOrdinal).allowC2S();
        final IntSyncValue level = new IntSyncValue(machine::level);
        final IntSyncValue edges = new IntSyncValue(machine::getEdges);
        final StringSyncValue host = new StringSyncValue(machine::getHost);
        final StringSyncValue status = new StringSyncValue(machine::statusLine);
        syncManager.syncValue("btsBurstMode", mode);
        syncManager.syncValue("btsBurstLevel", level);
        syncManager.syncValue("btsBurstEdges", edges);
        syncManager.syncValue("btsBurstHost", host);
        syncManager.syncValue("btsBurstStatus", status);

        final Flow col = Flow.column()
            .child(IKey.dynamic(() -> {
                final String h = host.getStringValue();
                return "Machine: " + (h.isEmpty() ? "none reading it" : h);
            })
                .asWidget())
            .child(
                Flow.row()
                    .child(
                        new CycleButtonWidget().value(mode)
                            .stateCount(MTEHatchBurstControl.Mode.VALUES.length)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .size(16))
                    .child(
                        IKey.dynamic(() -> "Mode: " + MTEHatchBurstControl.Mode.of(mode.getIntValue()).label)
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                IKey.dynamic(() -> "§7" + MTEHatchBurstControl.Mode.of(mode.getIntValue()).help)
                    .asWidget())
            .child(
                IKey.dynamic(
                    () -> "Redstone in: " + level.getIntValue() + "  (rising edges: " + edges.getIntValue() + ")")
                    .asWidget())
            .child(
                IKey.dynamic(status::getStringValue)
                    .asWidget())
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(2);
        return super.createContentSection(panel, syncManager).child(col);
    }
}
