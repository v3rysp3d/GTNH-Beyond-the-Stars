package com.nateyburr.beyondthestars.machines.kerr;

import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * KERR REDESIGN: a Kerr module's screen (docs/kerr-redesign.md §4.2). The standard GT multiblock GUI
 * (void excess, input separation, batch mode and the power panel work as on any GT multi; there are
 * no machine modes), plus a Linked / Not linked label in the gap row (Godforge's
 * {@code createConnectionStatus}) and two terminal lines: the link (or why there is none) and the
 * hole's bonus. Every value is server state, so all of it comes across as sync values.
 */
public class MTEKerrModuleGui extends MTEMultiBlockBaseGui<MTEKerrModuleBase<?>> {

    public MTEKerrModuleGui(MTEKerrModuleBase<?> multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(
            "kerrLink",
            new IntSyncValue(
                () -> multiblock.getLink()
                    .ordinal()));
        syncManager.syncValue("kerrKeePos", new StringSyncValue(multiblock::keePosText));
        syncManager.syncValue("kerrMass", new DoubleSyncValue(multiblock::linkedMass));
        syncManager.syncValue("kerrParallel", new IntSyncValue(multiblock::bonusParallel));
        syncManager.syncValue("kerrSpeed", new DoubleSyncValue(multiblock::bonusSpeed));
    }

    private static IWidget row(Supplier<String> text) {
        return IKey.dynamic(text::get)
            .asWidget()
            .marginBottom(2);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final IntSyncValue link = syncManager.findSyncHandler("kerrLink", IntSyncValue.class);
        final StringSyncValue pos = syncManager.findSyncHandler("kerrKeePos", StringSyncValue.class);
        final DoubleSyncValue mass = syncManager.findSyncHandler("kerrMass", DoubleSyncValue.class);
        final IntSyncValue parallel = syncManager.findSyncHandler("kerrParallel", IntSyncValue.class);
        final DoubleSyncValue speed = syncManager.findSyncHandler("kerrSpeed", DoubleSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent)
            .child(
                row(
                    () -> MTEKerrModuleBase.linkLine(
                        MTEKerrModuleBase.Link.of(link.getIntValue()),
                        pos.getStringValue(),
                        mass.getDoubleValue())))
            .child(
                row(
                    () -> MTEKerrModuleBase.bonusLine(
                        MTEKerrModuleBase.Link.of(link.getIntValue()),
                        parallel.getIntValue(),
                        speed.getDoubleValue())));
    }

    /** Godforge's connection label: "Linked" in green, "Not linked" in red. */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue link = syncManager.findSyncHandler("kerrLink", IntSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(2)
            .child(IKey.dynamic(() -> {
                final MTEKerrModuleBase.Link l = MTEKerrModuleBase.Link.of(link.getIntValue());
                if (l == MTEKerrModuleBase.Link.LINKED) return EnumChatFormatting.DARK_GREEN + "Linked";
                if (l == MTEKerrModuleBase.Link.HOLE_CLOSED) return EnumChatFormatting.GOLD + "Hole closed";
                return EnumChatFormatting.DARK_RED + "Not linked";
            })
                .alignment(Alignment.CENTER)
                .asWidget()
                .size(62, 16));
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }
}
