package com.nateyburr.beyondthestars.machines.reality;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): Reality Refab's screen. GT's standard MUI2 layout plus:
 * <ul>
 * <li><b>Terminal:</b> the BEC network, the linked Condensate Assembler and its nanites, the step being worked (its
 * nanite tier, contaminants), how much of the regions is drawn, why it waits, the last craft.</li>
 * <li><b>Gap row:</b> six region lamps: green = the network holds that region now, red = it does not.</li>
 * </ul>
 */
public class RealityRefabGui extends MTEMultiBlockBaseGui<MTERealityRefab> {

    public RealityRefabGui(MTERealityRefab multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("refNet", new StringSyncValue(multiblock::networkLine));
        syncManager.syncValue("refLink", new StringSyncValue(multiblock::linkLine));
        syncManager.syncValue("refStep", new StringSyncValue(multiblock::stepLine));
        syncManager.syncValue("refRegions", new StringSyncValue(multiblock::regionLine));
        syncManager.syncValue("refWait", new StringSyncValue(multiblock::waitLine));
        syncManager.syncValue("refNote", new StringSyncValue(multiblock::lastNote));
        syncManager.syncValue("refMask", new IntSyncValue(multiblock::regionMask));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue net = syncManager.findSyncHandler("refNet", StringSyncValue.class);
        final StringSyncValue link = syncManager.findSyncHandler("refLink", StringSyncValue.class);
        final StringSyncValue step = syncManager.findSyncHandler("refStep", StringSyncValue.class);
        final StringSyncValue regions = syncManager.findSyncHandler("refRegions", StringSyncValue.class);
        final StringSyncValue wait = syncManager.findSyncHandler("refWait", StringSyncValue.class);
        final StringSyncValue note = syncManager.findSyncHandler("refNote", StringSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent)
            .child(HRGui.row(() -> EnumChatFormatting.AQUA + net.getStringValue(), () -> true))
            .child(HRGui.row(() -> EnumChatFormatting.AQUA + link.getStringValue(), () -> true))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.WHITE + step.getStringValue(),
                    () -> !step.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.WHITE + regions.getStringValue(),
                    () -> !regions.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.YELLOW + "Waiting: " + wait.getStringValue(),
                    () -> !wait.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.DARK_GRAY + "Last: " + note.getStringValue(),
                    () -> !note.getStringValue()
                        .isEmpty()));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue mask = syncManager.findSyncHandler("refMask", IntSyncValue.class);
        final Flow row = super.createLeftPanelGapRow(parent, syncManager).childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER);
        row.child(
            IKey.str("Regions")
                .asWidget()
                .marginRight(1)
                .heightRel(1f));
        for (int k = 0; k < 6; k++) {
            final int region = k;
            row.child(
                IKey.str(MTERealityDiffraction.REGION_NAMES[k].split(" ")[0])
                    .asWidget()
                    .size(12, 12)
                    .textAlign(Alignment.Center)
                    .scale(0.7f)
                    .background(
                        HRGui.lamp(() -> (mask.getIntValue() & (1 << region)) != 0 ? HRGui.LAMP_ON : HRGui.LAMP_OFF))
                    .tooltip(
                        t -> t.addLine("Entangled Region " + MTERealityDiffraction.REGION_NAMES[region])
                            .addLine(HRGui.grey("Green: this Refab's BEC network holds it now.")))
                    .tooltipShowUpTimer(HRGui.TOOLTIP));
        }
        return row;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }
}
