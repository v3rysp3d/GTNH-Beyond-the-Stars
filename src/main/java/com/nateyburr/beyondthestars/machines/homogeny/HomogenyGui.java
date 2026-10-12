package com.nateyburr.beyondthestars.machines.homogeny;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.reality.HRGui;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the Homogeny's screen, GT's standard MUI2 multiblock layout (void
 * protection, batch mode, recipe lock and the power panel as on any GT multi) plus:
 * <ul>
 * <li><b>Terminal:</b> the tier (running, installed, structure) and parallels; the rings; the upgrade (milestone,
 * escrow, ring); the install line with a progress bar; the last event.</li>
 * <li><b>Gap row:</b> the Install button (the screwdriver does the same). It starts the run only if every condition
 * holds; otherwise the install line says what is missing.</li>
 * </ul>
 * Every value is server state, sent as a sync value; Install goes back through {@link MTEHomogeny#beginInstall()}.
 */
public class HomogenyGui extends MTEMultiBlockBaseGui<MTEHomogeny> {

    public HomogenyGui(MTEHomogeny multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("homHead", new StringSyncValue(multiblock::headLine));
        syncManager.syncValue("homRings", new StringSyncValue(multiblock::ringLine));
        syncManager.syncValue("homUpgrade", new StringSyncValue(multiblock::upgradeLine));
        syncManager.syncValue("homInstallLine", new StringSyncValue(multiblock::installLine));
        syncManager.syncValue("homInstallP", new DoubleSyncValue(multiblock::installFraction));
        syncManager.syncValue("homNote", new StringSyncValue(multiblock::lastNote));
        // Install is a server-side action (GT's InteractionSyncHandler pattern), not a synced boolean.
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue head = syncManager.findSyncHandler("homHead", StringSyncValue.class);
        final StringSyncValue rings = syncManager.findSyncHandler("homRings", StringSyncValue.class);
        final StringSyncValue upgrade = syncManager.findSyncHandler("homUpgrade", StringSyncValue.class);
        final StringSyncValue install = syncManager.findSyncHandler("homInstallLine", StringSyncValue.class);
        final DoubleSyncValue installP = syncManager.findSyncHandler("homInstallP", DoubleSyncValue.class);
        final StringSyncValue note = syncManager.findSyncHandler("homNote", StringSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent)
            .child(HRGui.row(() -> EnumChatFormatting.AQUA + head.getStringValue(), () -> true))
            .child(HRGui.row(() -> EnumChatFormatting.GRAY + rings.getStringValue(), () -> true))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.WHITE + upgrade.getStringValue(),
                    () -> !upgrade.getStringValue()
                        .isEmpty()))
            .child(
                HRGui.row(
                    () -> (installP.getDoubleValue() >= 0 ? EnumChatFormatting.LIGHT_PURPLE
                        : install.getStringValue()
                            .startsWith("Ready") ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW)
                        + install.getStringValue(),
                    () -> !install.getStringValue()
                        .isEmpty()))
            .child(HRGui.progress(installP))
            .child(
                HRGui.row(
                    () -> EnumChatFormatting.DARK_GRAY + "Last: " + note.getStringValue(),
                    () -> !note.getStringValue()
                        .isEmpty()));
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final InteractionSyncHandler install = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.beginInstall(); });
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(2)
            .child(
                new ButtonWidget<>().size(40, 16)
                    .syncHandler(install)
                    .overlay(IKey.str("Install"))
                    .tooltip(
                        t -> t.addLine("Install the next tier's core")
                            .addLine(HRGui.grey("Needs: the next ring built, the milestone mixed,"))
                            .addLine(HRGui.grey("and the core in escrow (right-click it on)."))
                            .addLine(HRGui.grey("10 min offline; sneak-punch cancels and returns the core.")))
                    .tooltipShowUpTimer(HRGui.TOOLTIP));
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }
}
