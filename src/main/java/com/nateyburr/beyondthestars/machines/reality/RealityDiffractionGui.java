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
 * HOMOGENY + REALITY MACHINES (H/R unit 2): Reality Diffraction's screen. GT's standard MUI2 layout plus:
 * <ul>
 * <li><b>Terminal:</b> the BEC network (and its storages), what waits to enter it, the missing anchors by name, the
 * last run.</li>
 * <li><b>Gap row:</b> six region lamps, I..VI: green = anchored for the next run, red = a hatch but not enough anchor,
 * grey = no hatch in that socket. Hover a lamp for the region and its anchor.</li>
 * </ul>
 */
public class RealityDiffractionGui extends MTEMultiBlockBaseGui<MTERealityDiffraction> {

    public RealityDiffractionGui(MTERealityDiffraction multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("diffNet", new StringSyncValue(multiblock::networkLine));
        syncManager.syncValue("diffPending", new StringSyncValue(multiblock::pendingLine));
        syncManager.syncValue("diffAnchors", new StringSyncValue(multiblock::anchorLine));
        syncManager.syncValue("diffNote", new StringSyncValue(multiblock::lastNote));
        syncManager.syncValue("diffMask", new IntSyncValue(multiblock::anchorMask));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue net = syncManager.findSyncHandler("diffNet", StringSyncValue.class);
        final StringSyncValue pending = syncManager.findSyncHandler("diffPending", StringSyncValue.class);
        final StringSyncValue anchors = syncManager.findSyncHandler("diffAnchors", StringSyncValue.class);
        final StringSyncValue note = syncManager.findSyncHandler("diffNote", StringSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent)
            .child(HRGui.row(() -> EnumChatFormatting.AQUA + net.getStringValue(), () -> true))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.GOLD + pending.getStringValue(),
                    () -> !pending.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.RED + anchors.getStringValue(),
                    () -> !anchors.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.DARK_GRAY + "Last: " + note.getStringValue(),
                    () -> !note.getStringValue()
                        .isEmpty()));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue mask = syncManager.findSyncHandler("diffMask", IntSyncValue.class);
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
                    .background(HRGui.lamp(() -> {
                        final int m = mask.getIntValue();
                        if ((m & (1 << region)) != 0) return HRGui.LAMP_ON;
                        return (m & (1 << (8 + region))) != 0 ? HRGui.LAMP_OFF : HRGui.LAMP_IDLE;
                    }))
                    .tooltip(
                        t -> t.addLine("Region " + MTERealityDiffraction.REGION_NAMES[region])
                            .addLine(HRGui.grey("Green: anchored for the next run."))
                            .addLine(HRGui.grey("Red: its socket's hatch lacks the anchor."))
                            .addLine(HRGui.grey("Grey: no hatch in its socket.")))
                    .tooltipShowUpTimer(HRGui.TOOLTIP));
        }
        return row;
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

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }
}
