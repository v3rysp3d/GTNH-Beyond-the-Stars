package com.nateyburr.beyondthestars.machines;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ListWidget;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Tidy B8: the controller screen of the four production halls (PUREX Canyon, Fuel Fabrication
 * Hall, Cascade Hall, Site Batching Plant). GT's standard multiblock layout plus ONE terminal line
 * saying what the parallel is and what sets it - {@code Parallel: 16 (shell: Iridium Reinforced
 * Block, tier 2/3)}, or {@code Parallel: 8 (stage banks 3/4)} on the Cascade Hall - which is the
 * number that makes each of these machines worth building. Pattern: GT's
 * {@code MTELargeFluidExtractorGui}.
 * <p>
 * <b>AXIS 1, CLIENT vs SERVER.</b> The parallel, the shell tier and the stage banks are written by
 * {@code checkMachine}, which runs SERVER-side only; on the client those fields are 0. So the
 * parallel and the one "detail" number ({@link MTEProductionHallBase#getParallelDetail}: shell
 * tier or stage banks) come across as sync values, and the line is rendered from them through
 * {@link MTEProductionHallBase#parallelSource}, which reads only its argument - never a field.
 */
public class MTEProductionHallGui extends MTEMultiBlockBaseGui<MTEProductionHallBase<?>> {

    public MTEProductionHallGui(MTEProductionHallBase<?> multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("hallParallel", new IntSyncValue(multiblock::getTrueParallel));
        syncManager.syncValue("hallDetail", new IntSyncValue(multiblock::getParallelDetail));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final IntSyncValue parallel = syncManager.findSyncHandler("hallParallel", IntSyncValue.class);
        final IntSyncValue detail = syncManager.findSyncHandler("hallDetail", IntSyncValue.class);
        // GT's own "is the structure formed" flag (mMachine), synced by the base GUI.
        final BooleanSyncValue formed = syncManager.findSyncHandler("machine", BooleanSyncValue.class);
        return super.createTerminalTextWidget(syncManager, parent).child(
            IKey.dynamic(() -> parallelLine(parallel.getIntValue(), multiblock.parallelSource(detail.getIntValue())))
                .asWidget()
                .marginBottom(2)
                .setEnabledIf(w -> formed == null || formed.getBoolValue()));
    }

    /** {@code Parallel: 16 (shell: Iridium Reinforced Block, tier 2/3)}. */
    private static String parallelLine(int parallel, String source) {
        return EnumChatFormatting.WHITE + "Parallel: "
            + EnumChatFormatting.YELLOW
            + String.format("%,d", Math.max(1, parallel))
            + EnumChatFormatting.WHITE
            + (source == null || source.isEmpty() ? "" : " (" + source + ")")
            + EnumChatFormatting.RESET;
    }
}
