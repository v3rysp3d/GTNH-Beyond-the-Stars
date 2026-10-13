package com.nateyburr.beyondthestars.machines.conduit;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/** P4: the GRB Receptor Port's GUI (MUI2): the machine it feeds, last second's delivery by source, linked or paused. */
public class GRBReceptorGui extends MTEHatchBaseGui<MTEHatchGRBReceptor> {

    public GRBReceptorGui(MTEHatchGRBReceptor hatch) {
        super(hatch);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        final StringSyncValue status = new StringSyncValue(machine::statusText);
        syncManager.syncValue("btsReceptorStatus", status);
        final Flow col = Flow.column()
            .child(
                IKey.dynamic(status::getStringValue)
                    .asWidget()
                    .maxWidth(160))
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(2);
        return super.createContentSection(panel, syncManager).child(col);
    }
}
