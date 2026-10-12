package com.nateyburr.beyondthestars.machines.conduit;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * P4: the conduit elements' GUI (MUI2, GT's multiblock layout). The terminal shows the element's status lines (throat,
 * open / closed, upkeep, flow through, clipped and lost, each out-throat's route); the gap row has the fuel choice and,
 * on the Junction, its split / combine panel (mode, c, k, out 3). Every setter is clamped on the server.
 */
public class ConduitElementGui<T extends MTEConduitElement<?>> extends MTEMultiBlockBaseGui<T> {

    private static final int PANEL_W = 170;
    private static final int PANEL_H = 150;

    public ConduitElementGui(T multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        final MTEConduitElement<?> m = multiblock;
        syncManager.syncValue("cStatus", new StringSyncValue(m::statusText));
        syncManager.syncValue("cFuel", new IntSyncValue(m::getFuelModeOrdinal, m::setFuelModeOrdinal).allowC2S());
        if (m instanceof MTEConduitJunction) {
            final MTEConduitJunction j = (MTEConduitJunction) m;
            syncManager.syncValue("jMode", new IntSyncValue(j::getModeOrdinal, j::setModeOrdinal).allowC2S());
            syncManager.syncValue("jRatio", new DoubleSyncValue(j::getRatio, j::setRatio).allowC2S());
            syncManager.syncValue("jGain", new IntSyncValue(j::getGainIndex, j::setGainIndex).allowC2S());
            syncManager.syncValue("jOut3", new DoubleSyncValue(j::getOut3Share, j::setOut3Share).allowC2S());
        }
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final StringSyncValue status = syncManager.findSyncHandler("cStatus", StringSyncValue.class);
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

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue fuel = syncManager.findSyncHandler("cFuel", IntSyncValue.class);
        final Flow row = super.createRightPanelGapRow(parent, syncManager).child(
            new ButtonWidget<>().size(18, 18)
                .marginLeft(2)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                .onMousePressed(b -> {
                    fuel.setIntValue(Math.floorMod(fuel.getIntValue() + 1, MTEConduitElement.FuelMode.VALUES.length));
                    return true;
                })
                .tooltip(
                    t -> t.addLine("Throat fuel: click to cycle Auto / Molten SpaceTime / Ergomatter")
                        .addLine(
                            EnumChatFormatting.GRAY + "Auto: Ergomatter when the hatches hold enough, else SpaceTime.")
                        .addLine(EnumChatFormatting.GRAY + "The terminal shows the choice and the litres a second."))
                .tooltipShowUpTimer(TOOLTIP_DELAY));
        if (!(multiblock instanceof MTEConduitJunction)) return row;
        final IPanelHandler panel = syncManager
            .syncedPanel("jSplitPanel", true, (sub, handler) -> buildJunctionPanel(syncManager, parent));
        return row.child(
            new ButtonWidget<>().size(18, 18)
                .marginLeft(2)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED)
                .onMousePressed(b -> {
                    if (panel.isPanelOpen()) panel.closePanel();
                    else panel.openPanel();
                    return true;
                })
                .tooltip(t -> t.addLine("Split / combine: mode, ratio c, gain k, out 3 share"))
                .tooltipShowUpTimer(TOOLTIP_DELAY));
    }

    private static IWidget keyValue(String label, Supplier<String> value, int width) {
        return Flow.row()
            .width(width)
            .height(9)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(
                IKey.str(label)
                    .asWidget())
            .child(
                IKey.dynamic(value::get)
                    .asWidget());
    }

    private ModularPanel buildJunctionPanel(PanelSyncManager main, ModularPanel parent) {
        final IntSyncValue mode = main.findSyncHandler("jMode", IntSyncValue.class);
        final DoubleSyncValue ratio = main.findSyncHandler("jRatio", DoubleSyncValue.class);
        final IntSyncValue gain = main.findSyncHandler("jGain", IntSyncValue.class);
        final DoubleSyncValue out3 = main.findSyncHandler("jOut3", DoubleSyncValue.class);
        final int w = PANEL_W - 8;
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Junction")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .child(
                        new ButtonWidget<>().size(14, 14)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .onMousePressed(b -> {
                                mode.setIntValue(1 - Math.floorMod(mode.getIntValue(), 2));
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Split: in through the big throat, out of the three small ones.")
                                    .addLine("Combine: in through the small throats, the sum out of the big one.")))
                    .child(
                        IKey.dynamic(() -> "Mode: " + (mode.getIntValue() == 0 ? "Split" : "Combine"))
                            .asWidget()))
            .child(keyValue("Ratio c (share 1)", () -> String.format(Locale.ROOT, "%.2f", ratio.getDoubleValue()), w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(ratio::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != ratio.getDoubleValue()) ratio.setDoubleValue(c);
                    }))
                    .size(w, 10))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .child(
                        new ButtonWidget<>().size(14, 14)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .onMousePressed(b -> {
                                gain.setIntValue(Math.floorMod(gain.getIntValue() + 1, BurstRouting.GAINS.length));
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Gain k: share 1 = c + k x (s/15 - 0.5)")
                                    .addLine(EnumChatFormatting.GRAY + "s: the Burst Control Hatch's redstone.")
                                    .addLine(EnumChatFormatting.GRAY + "k 0 ignores redstone.")))
                    .child(
                        IKey.dynamic(
                            () -> "Gain k: " + MTEConduitJunction.fmtGain(
                                BurstRouting.GAINS[Math.floorMod(gain.getIntValue(), BurstRouting.GAINS.length)]))
                            .asWidget()))
            .child(
                keyValue(
                    "Out 3 takes first",
                    () -> String.format(Locale.ROOT, "%.0f %%", 100 * out3.getDoubleValue()),
                    w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(out3::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != out3.getDoubleValue()) out3.setDoubleValue(c);
                    }))
                    .size(w, 10))
            .child(
                IKey.str(
                    EnumChatFormatting.DARK_GRAY
                        + "Out 3 is the fixed tap (e.g. 10 % to a Kerr). Two Directive bits as 15 / 8 / 0 on the hatch: an all-binary rig.")
                    .asWidget()
                    .scale(0.75f)
                    .maxWidth(w));
        return new ModularPanel("jSplitPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(PANEL_W, PANEL_H)
            .child(column);
    }
}
