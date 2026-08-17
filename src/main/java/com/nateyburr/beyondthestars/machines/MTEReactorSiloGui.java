package com.nateyburr.beyondthestars.machines;

import static net.minecraft.util.StatCollector.translateToLocal;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Controller screen for the Reactor Silo: live plant status (tier, fuel, heat, decay heat,
 * radiation, Turbine Deck addon + fan, output) in the terminal area, plus the control-rod
 * throttle setting as an editable field (1-24; a programmed circuit still overrides it).
 */
public class MTEReactorSiloGui extends MTEMultiBlockBaseGui<MTEReactorCoreMk1> {

    public MTEReactorSiloGui(MTEReactorCoreMk1 multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("siloTier", new IntSyncValue(multiblock::getReactorTier));
        syncManager.syncValue("siloEdge", new IntSyncValue(multiblock::getEdge));
        syncManager.syncValue("fuelColumns", new IntSyncValue(multiblock::getFuelColumns));
        syncManager.syncValue("columnCap", new IntSyncValue(multiblock::getColumnCap));
        syncManager.syncValue("fuelName", new StringSyncValue(multiblock::getFuelDisplayName));
        syncManager.syncValue("heat", new IntSyncValue(multiblock::getHeat));
        syncManager.syncValue("decaySeconds", new IntSyncValue(multiblock::getDecaySeconds));
        syncManager.syncValue("radiationSeconds", new IntSyncValue(multiblock::getRadiationSeconds));
        syncManager.syncValue("deckInstalled", new BooleanSyncValue(multiblock::isTurbineInstalled));
        syncManager.syncValue("fanTier", new IntSyncValue(multiblock::installedFanTier));
        syncManager.syncValue("euOutput", new LongSyncValue(multiblock::getLastEUOutput));
        syncManager.syncValue("steamOutput", new LongSyncValue(multiblock::getSteamPerTick));
        syncManager.syncValue(
            "throttleSetting",
            new IntSyncValue(multiblock::getThrottleSetting, multiblock::setThrottleSetting).allowC2S());
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final IntSyncValue tier = syncManager.findSyncHandler("siloTier", IntSyncValue.class);
        final IntSyncValue edge = syncManager.findSyncHandler("siloEdge", IntSyncValue.class);
        final IntSyncValue columns = syncManager.findSyncHandler("fuelColumns", IntSyncValue.class);
        final IntSyncValue columnCap = syncManager.findSyncHandler("columnCap", IntSyncValue.class);
        final StringSyncValue fuelName = syncManager.findSyncHandler("fuelName", StringSyncValue.class);
        final IntSyncValue heat = syncManager.findSyncHandler("heat", IntSyncValue.class);
        final IntSyncValue decaySeconds = syncManager.findSyncHandler("decaySeconds", IntSyncValue.class);
        final IntSyncValue radiationSeconds = syncManager.findSyncHandler("radiationSeconds", IntSyncValue.class);
        final BooleanSyncValue deckInstalled = syncManager.findSyncHandler("deckInstalled", BooleanSyncValue.class);
        final IntSyncValue fanTier = syncManager.findSyncHandler("fanTier", IntSyncValue.class);
        final LongSyncValue euOutput = syncManager.findSyncHandler("euOutput", LongSyncValue.class);
        final LongSyncValue steamOutput = syncManager.findSyncHandler("steamOutput", LongSyncValue.class);

        return super.createTerminalTextWidget(syncManager, parent)
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.AQUA + translateToLocal("beyondthestars.gui.silo")
                        + " "
                        + tier.getIntValue()
                        + " ("
                        + edge.getIntValue()
                        + "x"
                        + edge.getIntValue()
                        + " octagon)"
                        + EnumChatFormatting.RESET)
                    .asWidget()
                    .marginBottom(2))
            .child(
                IKey.dynamic(
                    () -> translateToLocal("beyondthestars.gui.columns") + " "
                        + columns.getIntValue()
                        + "/"
                        + columnCap.getIntValue()
                        + "  ·  "
                        + fuelName.getStringValue())
                    .asWidget()
                    .marginBottom(2))
            .child(IKey.dynamic(() -> {
                final int h = heat.getIntValue();
                final EnumChatFormatting color = h >= 8000 ? EnumChatFormatting.RED
                    : h >= 4000 ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN;
                return color + translateToLocal(
                    "beyondthestars.gui.heat") + " " + h + " / 10,000 (SCRAM 8,000)" + EnumChatFormatting.RESET;
            })
                .asWidget()
                .marginBottom(2))
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.GOLD + translateToLocal(
                        "beyondthestars.gui.decay") + " " + decaySeconds.getIntValue() + "s" + EnumChatFormatting.RESET)
                    .asWidget()
                    .marginBottom(2)
                    .setEnabledIf(w -> decaySeconds.getIntValue() > 0))
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.DARK_RED + translateToLocal("beyondthestars.gui.radiation")
                        + " "
                        + radiationSeconds.getIntValue()
                        + "s"
                        + EnumChatFormatting.RESET)
                    .asWidget()
                    .marginBottom(2)
                    .setEnabledIf(w -> radiationSeconds.getIntValue() > 0))
            .child(IKey.dynamic(() -> {
                if (!deckInstalled.getBoolValue()) {
                    return translateToLocal("beyondthestars.gui.deck.absent");
                }
                final int fan = fanTier.getIntValue();
                final String fanText = fan == 3 ? translateToLocal("beyondthestars.gui.fan.optimized")
                    : fan == 2 ? translateToLocal("beyondthestars.gui.fan.tda")
                        : fan == 1 ? translateToLocal("beyondthestars.gui.fan.matched")
                            : EnumChatFormatting.RED + translateToLocal("beyondthestars.gui.fan.none");
                return EnumChatFormatting.AQUA + translateToLocal("beyondthestars.gui.deck.installed")
                    + EnumChatFormatting.RESET
                    + " "
                    + fanText
                    + EnumChatFormatting.RESET;
            })
                .asWidget()
                .marginBottom(2))
            .child(IKey.dynamic(() -> {
                if (deckInstalled.getBoolValue()) {
                    return translateToLocal("beyondthestars.gui.output.eu") + " "
                        + String.format("%,d", euOutput.getLongValue())
                        + " EU/t";
                }
                return translateToLocal("beyondthestars.gui.output.steam") + " "
                    + String.format("%,d", steamOutput.getLongValue())
                    + " L/t";
            })
                .asWidget()
                .marginBottom(2));
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        return super.createRightPanelGapRow(parent, syncManager).child(
            new TextFieldWidget().formatAsInteger(true)
                .defaultNumber(24)
                .numbersInt(1, 24)
                .syncHandler("throttleSetting")
                .width(30)
                .tooltip(t -> t.addLine(translateToLocal("beyondthestars.gui.throttle.tooltip"))));
    }
}
