package com.nateyburr.beyondthestars.machines;

import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
 * Controller screen for the Reactor Silo: the plant's full instrument panel, grouped and
 * colour-coded exactly like the two control blocks - REACTOR (tier, cascade, fuel, steam,
 * heat/decay/radiation), STEAM CHAIN (spine pattern and multipliers, injectors and propellant,
 * coolant and starvation), TURBINE (RPM against the redline, cycles/min, steam buffer, rotor
 * condition per stage, and WHY the fan is being held back), OUTPUT (EU/t, the dynamo+laser
 * ceiling and the clamp) plus the test-bench flags. Every line is built by the controller's
 * shared formatters, so no readout can drift from another. The control-rod throttle and Build
 * Tier stay editable fields (a programmed circuit still overrides the throttle).
 */
public class MTEReactorSiloGui extends MTEMultiBlockBaseGui<MTEReactorCoreMk1> {

    public MTEReactorSiloGui(MTEReactorCoreMk1 multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("siloBuilt", new BooleanSyncValue(multiblock::isSiloBuilt));
        syncManager.syncValue("siloTier", new IntSyncValue(multiblock::getReactorTier));
        syncManager.syncValue("siloEdge", new IntSyncValue(multiblock::getEdge));
        syncManager.syncValue("throttlePct", new IntSyncValue(multiblock::getThrottlePercent));
        syncManager.syncValue("modules", new StringSyncValue(multiblock::getModulesSummary));
        syncManager.syncValue("fuelColumns", new IntSyncValue(multiblock::getFuelColumns));
        syncManager.syncValue("columnCap", new IntSyncValue(multiblock::getColumnCap));
        syncManager.syncValue("fuelName", new StringSyncValue(multiblock::getFuelDisplayName));
        syncManager.syncValue("steamPerTick", new LongSyncValue(multiblock::getSteamPerTick));
        syncManager.syncValue("heat", new IntSyncValue(multiblock::getHeat));
        syncManager.syncValue("decaySeconds", new IntSyncValue(multiblock::getDecaySeconds));
        syncManager.syncValue("radiationSeconds", new IntSyncValue(multiblock::getRadiationSeconds));
        syncManager.syncValue("deckInstalled", new BooleanSyncValue(multiblock::isTurbineInstalled));
        syncManager.syncValue("euOutput", new LongSyncValue(multiblock::getLastEUOutput));
        syncManager.syncValue("euDemand", new LongSyncValue(multiblock::getLastEUDemand));
        syncManager.syncValue("euCeiling", new LongSyncValue(multiblock::outputCeiling));
        syncManager.syncValue("dynamoCap", new LongSyncValue(multiblock::getDynamoCapacity));
        syncManager.syncValue("laserCap", new LongSyncValue(multiblock::getLaserCapacity));
        syncManager.syncValue("laserCount", new IntSyncValue(multiblock::getLaserHatchCount));
        syncManager.syncValue("laserLinked", new IntSyncValue(multiblock::getLinkedLaserHatchCount));
        syncManager.syncValue("euClamped", new BooleanSyncValue(multiblock::isOutputClamped));
        syncManager.syncValue("steamOutput", new LongSyncValue(multiblock::getBoostedSteamOutput));
        syncManager.syncValue("cascadeMode", new BooleanSyncValue(multiblock::isCascadeMode));
        syncManager.syncValue("liveColumns", new IntSyncValue(multiblock::getLiveColumns));
        syncManager.syncValue("totalColumns", new IntSyncValue(multiblock::getTotalColumns));
        syncManager.syncValue("spineBuilt", new BooleanSyncValue(multiblock::isSpineBuilt));
        syncManager.syncValue("spinePattern", new StringSyncValue(multiblock::getSpinePattern));
        syncManager.syncValue("spineHousings", new IntSyncValue(multiblock::getSpineHousings));
        syncManager.syncValue("spineSpeed", new IntSyncValue(multiblock::getSpineSpeedMilli));
        syncManager.syncValue("spineEff", new IntSyncValue(multiblock::getSpineEffMilli));
        syncManager.syncValue("spineSwirl", new IntSyncValue(multiblock::getSpineSwirl));
        syncManager.syncValue("spiralBuilt", new BooleanSyncValue(multiblock::isSpiralBuilt));
        syncManager.syncValue("injectors", new IntSyncValue(multiblock::getRegisteredInjectors));
        syncManager.syncValue("goodInjectors", new IntSyncValue(multiblock::getLastGoodInjectors));
        syncManager.syncValue("propellant", new StringSyncValue(multiblock::getPropellantName));
        syncManager.syncValue("cycleProp", new IntSyncValue(multiblock::getLastCyclePropMilli));
        syncManager.syncValue("chamberCoolant", new StringSyncValue(multiblock::getChamberCoolantName));
        syncManager.syncValue("chamberDrain", new IntSyncValue(multiblock::getChamberCoolantDrain));
        syncManager.syncValue("chamberStarved", new BooleanSyncValue(multiblock::isChamberStarved));
        syncManager.syncValue("rpm", new IntSyncValue(multiblock::getRpm));
        syncManager.syncValue("cyclesPerMinute", new IntSyncValue(multiblock::getCyclesPerMinute));
        syncManager.syncValue("bufferSlugs", new IntSyncValue(multiblock::getSteamBufferSlugsTenths));
        syncManager.syncValue("benchClock", new BooleanSyncValue(multiblock::isDebugAutoCycle));
        syncManager.syncValue("infiniteSupplies", new BooleanSyncValue(multiblock::isDebugInfiniteSupplies));
        syncManager.syncValue("holdReason", new StringSyncValue(multiblock::getTurbineHoldReason));
        syncManager.syncValue("galleryBuilt", new BooleanSyncValue(multiblock::isGalleryBuilt));
        syncManager.syncValue("galleryActive", new BooleanSyncValue(multiblock::isGalleryActive));
        syncManager.syncValue("galleryStages", new IntSyncValue(multiblock::getGalleryStages));
        syncManager.syncValue("rotorFactor", new IntSyncValue(multiblock::getAvgRotorFactorMilli));
        syncManager.syncValue("rotorWear", new StringSyncValue(multiblock::getRotorWear));
        syncManager.syncValue(
            "throttleSetting",
            new IntSyncValue(multiblock::getThrottleSetting, multiblock::setThrottleSetting).allowC2S());
        syncManager.syncValue(
            "buildTierSetting",
            new IntSyncValue(multiblock::getBuildTierSetting, multiblock::setBuildTierSetting).allowC2S());
    }

    /** One terminal line. */
    private static IWidget row(Supplier<String> text) {
        return IKey.dynamic(() -> text.get())
            .asWidget()
            .marginBottom(2);
    }

    /** One terminal line that only appears while it has something to say. */
    private static IWidget row(Supplier<String> text, BooleanSupplier shown) {
        return IKey.dynamic(() -> text.get())
            .asWidget()
            .marginBottom(2)
            .setEnabledIf(w -> shown.getAsBoolean());
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final BooleanSyncValue siloBuilt = syncManager.findSyncHandler("siloBuilt", BooleanSyncValue.class);
        final IntSyncValue tier = syncManager.findSyncHandler("siloTier", IntSyncValue.class);
        final IntSyncValue edge = syncManager.findSyncHandler("siloEdge", IntSyncValue.class);
        final IntSyncValue throttlePct = syncManager.findSyncHandler("throttlePct", IntSyncValue.class);
        final StringSyncValue modules = syncManager.findSyncHandler("modules", StringSyncValue.class);
        final IntSyncValue columns = syncManager.findSyncHandler("fuelColumns", IntSyncValue.class);
        final IntSyncValue columnCap = syncManager.findSyncHandler("columnCap", IntSyncValue.class);
        final StringSyncValue fuelName = syncManager.findSyncHandler("fuelName", StringSyncValue.class);
        final LongSyncValue steamPerTick = syncManager.findSyncHandler("steamPerTick", LongSyncValue.class);
        final IntSyncValue heat = syncManager.findSyncHandler("heat", IntSyncValue.class);
        final IntSyncValue decaySeconds = syncManager.findSyncHandler("decaySeconds", IntSyncValue.class);
        final IntSyncValue radiationSeconds = syncManager.findSyncHandler("radiationSeconds", IntSyncValue.class);
        final BooleanSyncValue deckInstalled = syncManager.findSyncHandler("deckInstalled", BooleanSyncValue.class);
        final BooleanSyncValue cascadeMode = syncManager.findSyncHandler("cascadeMode", BooleanSyncValue.class);
        final IntSyncValue liveColumns = syncManager.findSyncHandler("liveColumns", IntSyncValue.class);
        final IntSyncValue totalColumns = syncManager.findSyncHandler("totalColumns", IntSyncValue.class);
        final BooleanSyncValue spineBuilt = syncManager.findSyncHandler("spineBuilt", BooleanSyncValue.class);
        final StringSyncValue spinePattern = syncManager.findSyncHandler("spinePattern", StringSyncValue.class);
        final IntSyncValue spineHousings = syncManager.findSyncHandler("spineHousings", IntSyncValue.class);
        final IntSyncValue spineSpeed = syncManager.findSyncHandler("spineSpeed", IntSyncValue.class);
        final IntSyncValue spineEff = syncManager.findSyncHandler("spineEff", IntSyncValue.class);
        final IntSyncValue spineSwirl = syncManager.findSyncHandler("spineSwirl", IntSyncValue.class);
        final BooleanSyncValue spiralBuilt = syncManager.findSyncHandler("spiralBuilt", BooleanSyncValue.class);
        final IntSyncValue injectors = syncManager.findSyncHandler("injectors", IntSyncValue.class);
        final IntSyncValue goodInjectors = syncManager.findSyncHandler("goodInjectors", IntSyncValue.class);
        final StringSyncValue propellant = syncManager.findSyncHandler("propellant", StringSyncValue.class);
        final IntSyncValue cycleProp = syncManager.findSyncHandler("cycleProp", IntSyncValue.class);
        final StringSyncValue chamberCoolant = syncManager.findSyncHandler("chamberCoolant", StringSyncValue.class);
        final IntSyncValue chamberDrain = syncManager.findSyncHandler("chamberDrain", IntSyncValue.class);
        final BooleanSyncValue chamberStarved = syncManager.findSyncHandler("chamberStarved", BooleanSyncValue.class);
        final IntSyncValue rpm = syncManager.findSyncHandler("rpm", IntSyncValue.class);
        final IntSyncValue cyclesPerMinute = syncManager.findSyncHandler("cyclesPerMinute", IntSyncValue.class);
        final IntSyncValue bufferSlugs = syncManager.findSyncHandler("bufferSlugs", IntSyncValue.class);
        final BooleanSyncValue benchClock = syncManager.findSyncHandler("benchClock", BooleanSyncValue.class);
        final BooleanSyncValue infiniteSupplies = syncManager
            .findSyncHandler("infiniteSupplies", BooleanSyncValue.class);
        final StringSyncValue holdReason = syncManager.findSyncHandler("holdReason", StringSyncValue.class);
        final BooleanSyncValue galleryBuilt = syncManager.findSyncHandler("galleryBuilt", BooleanSyncValue.class);
        final BooleanSyncValue galleryActive = syncManager.findSyncHandler("galleryActive", BooleanSyncValue.class);
        final IntSyncValue galleryStages = syncManager.findSyncHandler("galleryStages", IntSyncValue.class);
        final IntSyncValue rotorFactor = syncManager.findSyncHandler("rotorFactor", IntSyncValue.class);
        final StringSyncValue rotorWear = syncManager.findSyncHandler("rotorWear", StringSyncValue.class);
        final LongSyncValue euOutput = syncManager.findSyncHandler("euOutput", LongSyncValue.class);
        final LongSyncValue euDemand = syncManager.findSyncHandler("euDemand", LongSyncValue.class);
        final LongSyncValue euCeiling = syncManager.findSyncHandler("euCeiling", LongSyncValue.class);
        final LongSyncValue dynamoCap = syncManager.findSyncHandler("dynamoCap", LongSyncValue.class);
        final LongSyncValue laserCap = syncManager.findSyncHandler("laserCap", LongSyncValue.class);
        final IntSyncValue laserCount = syncManager.findSyncHandler("laserCount", IntSyncValue.class);
        final IntSyncValue laserLinked = syncManager.findSyncHandler("laserLinked", IntSyncValue.class);
        final BooleanSyncValue euClamped = syncManager.findSyncHandler("euClamped", BooleanSyncValue.class);
        final LongSyncValue steamOutput = syncManager.findSyncHandler("steamOutput", LongSyncValue.class);

        return super.createTerminalTextWidget(syncManager, parent)
            // --- REACTOR
            .child(row(() -> MTEReactorCoreMk1.header("REACTOR")))
            .child(
                row(
                    () -> EnumChatFormatting.YELLOW + translateToLocal("beyondthestars.gui.silo.absent")
                        + EnumChatFormatting.RESET,
                    () -> !siloBuilt.getBoolValue()))
            .child(
                row(
                    () -> MTEReactorCoreMk1.tierLine(
                        tier.getIntValue(),
                        edge.getIntValue(),
                        throttlePct.getIntValue(),
                        modules.getStringValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.cascadeLine(
                        cascadeMode.getBoolValue(),
                        liveColumns.getIntValue(),
                        totalColumns.getIntValue(),
                        columns.getIntValue(),
                        columnCap.getIntValue(),
                        fuelName.getStringValue(),
                        steamPerTick.getLongValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1
                        .heatLine(heat.getIntValue(), decaySeconds.getIntValue(), radiationSeconds.getIntValue()),
                    siloBuilt::getBoolValue))
            // --- STEAM CHAIN
            .child(row(() -> MTEReactorCoreMk1.header("STEAM CHAIN"), siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.spineLine(
                        spineBuilt.getBoolValue(),
                        spinePattern.getStringValue(),
                        spineHousings.getIntValue(),
                        spineSpeed.getIntValue(),
                        spineEff.getIntValue(),
                        spineSwirl.getIntValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.spiralLine(
                        spiralBuilt.getBoolValue(),
                        injectors.getIntValue(),
                        goodInjectors.getIntValue(),
                        propellant.getStringValue(),
                        cycleProp.getIntValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.coolantLine(
                        spiralBuilt.getBoolValue(),
                        chamberStarved.getBoolValue(),
                        chamberCoolant.getStringValue(),
                        chamberDrain.getIntValue()),
                    siloBuilt::getBoolValue))
            // --- TURBINE
            .child(row(() -> MTEReactorCoreMk1.header("TURBINE"), siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.rpmLine(
                        rpm.getIntValue(),
                        cyclesPerMinute.getIntValue(),
                        bufferSlugs.getIntValue(),
                        benchClock.getBoolValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.rotorLine(
                        galleryBuilt.getBoolValue(),
                        galleryStages.getIntValue(),
                        rotorFactor.getIntValue(),
                        rotorWear.getStringValue()),
                    siloBuilt::getBoolValue))
            // The interlock's voice: WHY the fan is not accelerating (missing coolant
            // headroom, dry injectors, a missed slug). Absent while everything is fine.
            .child(
                row(
                    () -> MTEReactorCoreMk1.holdLine(holdReason.getStringValue()),
                    () -> !holdReason.getStringValue()
                        .isEmpty()))
            // --- OUTPUT
            .child(row(() -> MTEReactorCoreMk1.header("OUTPUT"), siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.outputLine(
                        galleryActive.getBoolValue(),
                        deckInstalled.getBoolValue(),
                        euOutput.getLongValue(),
                        euDemand.getLongValue(),
                        euClamped.getBoolValue(),
                        steamOutput.getLongValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1
                        .ceilingLine(euCeiling.getLongValue(), dynamoCap.getLongValue(), laserCap.getLongValue()),
                    galleryBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.laserLine(laserCount.getIntValue(), laserLinked.getIntValue()),
                    galleryBuilt::getBoolValue))
            // --- test bench flags, only while one of them is on
            .child(
                row(
                    () -> MTEReactorCoreMk1.debugLine(benchClock.getBoolValue(), infiniteSupplies.getBoolValue()),
                    () -> benchClock.getBoolValue() || infiniteSupplies.getBoolValue()));
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        return super.createRightPanelGapRow(parent, syncManager).child(
            new TextFieldWidget().formatAsInteger(true)
                .defaultNumber(24)
                .numbersInt(1, 24)
                .syncHandler("throttleSetting")
                .width(30)
                .tooltip(t -> t.addLine(translateToLocal("beyondthestars.gui.throttle.tooltip"))))
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .defaultNumber(1)
                    .numbersInt(1, 5)
                    .syncHandler("buildTierSetting")
                    .width(20)
                    .tooltip(t -> t.addLine(translateToLocal("beyondthestars.gui.buildtier.tooltip"))));
    }
}
