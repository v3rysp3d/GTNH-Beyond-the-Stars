package com.nateyburr.beyondthestars.machines;

import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
 * ceiling and the clamp), MATH (the numbered derivation of that EU/t - the pillars' own steam
 * sum -> throttle -> spine -> RPM/500 x rotors -> the output clamp - with every factor carrying
 * its current value, closing on the WEAKEST term and its fix; a plant with no Turbine Generator
 * gets the steam-export derivation instead, because it derives no EU at all) plus the
 * test-bench flags. Every line
 * is built by the controller's shared formatters, so no readout can drift from another. The control-rod throttle and
 * Build
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
        // M1: the tier of the shell PHYSICALLY on the pad, measured off the world. It is what
        // tells "no silo yet" from "a silo is standing and does not pass" - two states the
        // panel used to print the same build instruction for.
        // B2: legacySiloTier() is that measurement narrowed to the state the line actually
        // describes - a standing shell whose FACE still conflicts. A silo halfway through
        // being hand-built reads 0 here and is told to keep projecting, not to migrate.
        syncManager.syncValue("standingTier", new IntSyncValue(multiblock::legacySiloTier));
        syncManager.syncValue("siloEdge", new IntSyncValue(multiblock::getEdge));
        syncManager.syncValue("throttlePct", new IntSyncValue(multiblock::getThrottlePercent));
        syncManager.syncValue("modules", new StringSyncValue(multiblock::getModulesSummary));
        // D-20: the supply lines now sit on three different buildings, so the panel carries the
        // three figures together - a rod queue of 0 against non-zero pillar gaps IS the missing
        // output, and 0 L of propellant is a fan that will never accelerate.
        syncManager.syncValue("rodsQueued", new IntSyncValue(multiblock::getRodsQueued));
        syncManager.syncValue("pillarGaps", new IntSyncValue(multiblock::getPillarGaps));
        syncManager.syncValue("propellantLitres", new IntSyncValue(multiblock::getPropellantLitres));
        syncManager.syncValue("coolantLitres", new IntSyncValue(multiblock::getChamberCoolantLitres));
        syncManager.syncValue("fuelColumns", new IntSyncValue(multiblock::getFuelColumns));
        syncManager.syncValue("columnCap", new IntSyncValue(multiblock::getColumnCap));
        syncManager.syncValue("fuelName", new StringSyncValue(multiblock::getFuelDisplayName));
        // B2: the terms of the EU/t derivation, so the math section can label every factor with
        // the value it has RIGHT NOW rather than with the formula's constants.
        syncManager.syncValue("fuelMult", new IntSyncValue(multiblock::getActiveFuelMult));
        // R7: fuelMult is the BEST grade in the silo, which is not what the cascade summed.
        // The pre-throttle sum and the grade census come across too, so the client renders the
        // server's arithmetic instead of re-deriving a different (higher) number from a label.
        syncManager.syncValue("fuelBase", new LongSyncValue(multiblock::getFuelBaseSteam));
        syncManager.syncValue("fuelMix", new StringSyncValue(multiblock::getFuelGradeMix));
        syncManager.syncValue("throttleN", new IntSyncValue(multiblock::getThrottleN));
        syncManager.syncValue("steamAfterSpine", new LongSyncValue(multiblock::getSteamAfterSpine));
        syncManager.syncValue("liveDemand", new LongSyncValue(multiblock::getLiveEUDemand));
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
        // C1: the manifold's Energy Hatches are furniture, never a load - the panel reports how
        // many are in as a NOTE, because the structure check no longer stops the plant over it.
        syncManager.syncValue("energyHatches", new IntSyncValue(multiblock::getEnergyHatchCount));
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
        // M8: the TC-7 interlock's two refusals, as the interlock itself asks them. Without
        // these the coolant line reads "idle" and the weakest-term line says "cycle faster" on
        // a plant that is refusing every single cycle for want of coolant.
        syncManager.syncValue("coolantReady", new BooleanSyncValue(multiblock::isChamberCoolantReady));
        syncManager.syncValue("injectorsDry", new BooleanSyncValue(multiblock::isInjectorsDry));
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
        final IntSyncValue standingTier = syncManager.findSyncHandler("standingTier", IntSyncValue.class);
        final IntSyncValue edge = syncManager.findSyncHandler("siloEdge", IntSyncValue.class);
        final IntSyncValue throttlePct = syncManager.findSyncHandler("throttlePct", IntSyncValue.class);
        final StringSyncValue modules = syncManager.findSyncHandler("modules", StringSyncValue.class);
        final IntSyncValue rodsQueued = syncManager.findSyncHandler("rodsQueued", IntSyncValue.class);
        final IntSyncValue pillarGaps = syncManager.findSyncHandler("pillarGaps", IntSyncValue.class);
        final IntSyncValue propellantLitres = syncManager.findSyncHandler("propellantLitres", IntSyncValue.class);
        final IntSyncValue coolantLitres = syncManager.findSyncHandler("coolantLitres", IntSyncValue.class);
        final IntSyncValue columns = syncManager.findSyncHandler("fuelColumns", IntSyncValue.class);
        final IntSyncValue columnCap = syncManager.findSyncHandler("columnCap", IntSyncValue.class);
        final StringSyncValue fuelName = syncManager.findSyncHandler("fuelName", StringSyncValue.class);
        final IntSyncValue fuelMult = syncManager.findSyncHandler("fuelMult", IntSyncValue.class);
        final LongSyncValue fuelBase = syncManager.findSyncHandler("fuelBase", LongSyncValue.class);
        final StringSyncValue fuelMix = syncManager.findSyncHandler("fuelMix", StringSyncValue.class);
        final IntSyncValue throttleN = syncManager.findSyncHandler("throttleN", IntSyncValue.class);
        final LongSyncValue steamAfterSpine = syncManager.findSyncHandler("steamAfterSpine", LongSyncValue.class);
        final LongSyncValue liveDemand = syncManager.findSyncHandler("liveDemand", LongSyncValue.class);
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
        final BooleanSyncValue coolantReady = syncManager.findSyncHandler("coolantReady", BooleanSyncValue.class);
        final BooleanSyncValue injectorsDry = syncManager.findSyncHandler("injectorsDry", BooleanSyncValue.class);
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
        final IntSyncValue energyHatches = syncManager.findSyncHandler("energyHatches", IntSyncValue.class);
        final BooleanSyncValue euClamped = syncManager.findSyncHandler("euClamped", BooleanSyncValue.class);
        final LongSyncValue steamOutput = syncManager.findSyncHandler("steamOutput", LongSyncValue.class);

        return super.createTerminalTextWidget(syncManager, parent)
            // --- REACTOR
            .child(row(() -> MTEReactorCoreMk1.header("REACTOR")))
            .child(
                row(
                    () -> MTEReactorCoreMk1.siloAbsentLine(standingTier.getIntValue()),
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
                    () -> MTEReactorCoreMk1.supplyLine(
                        siloBuilt.getBoolValue(),
                        rodsQueued.getIntValue(),
                        pillarGaps.getIntValue(),
                        propellantLitres.getIntValue(),
                        coolantLitres.getIntValue())))
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
                        coolantReady.getBoolValue(),
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
            // C1: a non-fatal note, not an error. The plant never draws EU, so an empty E
            // socket costs nothing and must not un-form a running reactor.
            .child(
                row(
                    () -> MTEReactorCoreMk1.energyHatchNote(true, energyHatches.getIntValue()),
                    () -> galleryBuilt.getBoolValue() && energyHatches.getIntValue() < 4))
            // --- MATH: the Water-Line-style derivation of the number above. Four numbered
            // steps from the pillars to the manifold, every factor carrying its CURRENT value,
            // then the one term that is costing the most and what to do about it.
            // R9: steps 3 and 4 are GENERATOR steps and get the export step instead when there
            // is no generator, so the panel never invents rotors it has nowhere to seat.
            // R1-B (B1): the gate is galleryACTIVE - housings AND rotors - which is what the
            // OUTPUT group's own line uses. Gating on galleryBuilt alone made the two groups
            // disagree on a formable plant: OUTPUT said "Steam export ... (no generator)" while
            // MATH printed the EU derivation right underneath it.
            .child(row(() -> MTEReactorCoreMk1.mathHeader(galleryActive.getBoolValue()), siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.mathSteamLine(
                        cascadeMode.getBoolValue(),
                        columns.getIntValue(),
                        fuelMult.getIntValue(),
                        throttleN.getIntValue(),
                        fuelBase.getLongValue(),
                        fuelMix.getStringValue(),
                        steamPerTick.getLongValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.mathSpineLine(
                        spineSpeed.getIntValue(),
                        spineEff.getIntValue(),
                        steamAfterSpine.getLongValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1
                        .mathTurbineLine(rpm.getIntValue(), rotorFactor.getIntValue(), liveDemand.getLongValue()),
                    () -> siloBuilt.getBoolValue() && galleryActive.getBoolValue()))
            .child(
                row(
                    () -> MTEReactorCoreMk1.mathClampLine(liveDemand.getLongValue(), euCeiling.getLongValue()),
                    () -> siloBuilt.getBoolValue() && galleryActive.getBoolValue()))
            .child(
                row(
                    () -> MTEReactorCoreMk1.mathExportLine(spineSpeed.getIntValue(), steamOutput.getLongValue()),
                    () -> siloBuilt.getBoolValue() && !galleryActive.getBoolValue()))
            // B2: the weakest-term line takes every optional piece's BUILT flag, so it can
            // tell an UNTUNED Spine (wrench its housings) from an ABSENT one (build it first).
            .child(
                row(
                    () -> MTEReactorCoreMk1.mathLimiterLine(
                        spineBuilt.getBoolValue(),
                        spineHousings.getIntValue(),
                        spiralBuilt.getBoolValue(),
                        galleryBuilt.getBoolValue(),
                        galleryActive.getBoolValue(),
                        columns.getIntValue(),
                        columnCap.getIntValue(),
                        totalColumns.getIntValue(),
                        MTEReactorCoreMk1.avgFuelMilli(
                            cascadeMode.getBoolValue(),
                            columns.getIntValue(),
                            fuelMult.getIntValue(),
                            fuelBase.getLongValue()),
                        throttleN.getIntValue(),
                        spineSpeed.getIntValue(),
                        spineEff.getIntValue(),
                        rpm.getIntValue(),
                        injectors.getIntValue(),
                        injectorsDry.getBoolValue(),
                        coolantReady.getBoolValue(),
                        rotorFactor.getIntValue(),
                        liveDemand.getLongValue(),
                        euCeiling.getLongValue()),
                    siloBuilt::getBoolValue))
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
