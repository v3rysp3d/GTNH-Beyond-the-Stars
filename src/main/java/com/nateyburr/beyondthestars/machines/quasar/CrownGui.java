package com.nateyburr.beyondthestars.machines.quasar;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * P4: the Burst Thermaliser Crown's GUI (MUI2, GT's multiblock layout): Mk and cap, the stream (jets, receptor, from
 * below), EU/t to wireless, overflow and clipping, the dock, the lifetime EU. Nothing to set.
 */
public class CrownGui extends MTEMultiBlockBaseGui<MTEBurstThermaliserCrown> {

    public CrownGui(MTEBurstThermaliserCrown multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("crStatus", new StringSyncValue(multiblock::statusText));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue status = syncManager.findSyncHandler("crStatus", StringSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent).child(
            IKey.dynamic(status::getStringValue)
                .asWidget()
                .fullWidth()
                .marginBottom(2));
    }

    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
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
}
