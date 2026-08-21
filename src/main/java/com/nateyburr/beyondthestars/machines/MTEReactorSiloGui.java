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
import com.nateyburr.beyondthestars.progression.BTSMilestone;
import com.nateyburr.beyondthestars.progression.BTSProgression;
import com.nateyburr.beyondthestars.progression.BTSUpgrade;

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
        // D-17 / D-22: the other four buildings. Every one of these is written SERVER-side by
        // checkMachine and read CLIENT-side here, so all of them have to come across as sync
        // values - a client-side read of the fields themselves is 0/false and would print "no
        // Switchyard" on a plant that has one.
        syncManager.syncValue("plantModules", new StringSyncValue(multiblock::getPlantModulesSummary));
        syncManager.syncValue("ventStack", new BooleanSyncValue(multiblock::isVentStackBuilt));
        syncManager.syncValue("switchyard", new BooleanSyncValue(multiblock::isSwitchyardBuilt));
        syncManager.syncValue("derrick", new BooleanSyncValue(multiblock::isDerrickBuilt));
        syncManager.syncValue("annex", new BooleanSyncValue(multiblock::isAnnexBuilt));
        syncManager.syncValue("derrickCycles", new IntSyncValue(multiblock::getDerrickCycles));
        syncManager.syncValue("annexCycles", new IntSyncValue(multiblock::getAnnexCycles));
        syncManager.syncValue("moduleStall", new StringSyncValue(multiblock::getModuleStall));
        // The grid soft cap, as a NUMBER rather than as the config flag: gridCeiling() already
        // folds in both "is the cap on" and "is a yard standing", so the client cannot reach a
        // different verdict than the tick that clamped the output did.
        syncManager.syncValue("gridCeiling", new LongSyncValue(multiblock::gridCeiling));
        syncManager.syncValue("gridCapEnabled", new BooleanSyncValue(multiblock::isGridCapEnabled));
        syncManager.syncValue("gridCapped", new BooleanSyncValue(multiblock::isGridCapped));
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
        // C1/C5: the manifold's Energy Hatches are not a structure DEMAND - the panel reports how
        // many are in as a NOTE, because the structure check no longer stops the plant over it.
        // They are a LOAD, though: the Extraction Derrick and the Enrichment Annex draw through
        // them (1,920 and 480 EU/t), so with a module standing the note turns from grey to red.
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
        // D-23 PROGRESSION / AXIS 1: the four milestone counters are written SERVER-side (in
        // onRunningTick, the campaign commit, the two module deposits and the cycle scorer), so
        // they have to come across as sync values or the client would read four zeros and paint
        // a finished campaign as a fresh one. Only the COUNTERS are synced: tier, multiplier,
        // next threshold, unlocks and points are pure functions of them, and the client runs
        // the same static code the server does (BTSMilestone / BTSProgression) - so no readout
        // can drift, and thirty separate handles are not needed for what four longs determine.
        syncManager.syncValue("progUptime", new LongSyncValue(multiblock::getProgUptime));
        syncManager.syncValue("progBurnup", new LongSyncValue(multiblock::getProgBurnup));
        syncManager.syncValue("progReproc", new LongSyncValue(multiblock::getProgReproc));
        syncManager.syncValue("progPerfect", new LongSyncValue(multiblock::getProgPerfect));
        // D-24 UPGRADE TREE / AXIS 1: ONE sync value for the whole tree - the purchased-node
        // bitmask. Points earned, points spent, affordability, prerequisites, gates and all
        // twelve effect channels are PURE FUNCTIONS of this long plus the four counters above,
        // and the client runs the identical static bodies (BTSUpgrade / BTSProgression) the
        // server's tick runs. Twenty-five nodes therefore cost one handle, not twenty-five, and
        // no derived readout can drift from the plant.
        syncManager.syncValue("treeNodes", new LongSyncValue(multiblock::getUpgradeNodes));
        // D-24 PURCHASE, SERVER-AUTHORITATIVE. registerServerSyncedAction runs the body ONLY on
        // the server (executeClient = false), so the click sends a node INDEX and the server
        // re-asks every rule against its own record before a single bit moves. The client is
        // never trusted with affordability - its tooltip's verdict is decoration, and the
        // authority is MTEReactorCoreMk1.buyUpgrade / refundUpgrade.
        //
        // Registered on the MAIN panel's sync manager, not the tree sub-panel's: this manager
        // provably exists on both sides for the whole life of the GUI, which the sub-panel's
        // only does while it happens to be open.
        syncManager.registerServerSyncedAction("btsTreeBuy", packet -> {
            final BTSUpgrade node = BTSUpgrade.byIndex(packet.readByte());
            if (node != null) multiblock.buyUpgrade(node);
        });
        syncManager.registerServerSyncedAction("btsTreeRefund", packet -> {
            final BTSUpgrade node = BTSUpgrade.byIndex(packet.readByte());
            if (node != null) multiblock.refundUpgrade(node);
        });
        syncManager.syncValue(
            "throttleSetting",
            new IntSyncValue(multiblock::getThrottleSetting, multiblock::setThrottleSetting).allowC2S());
        syncManager.syncValue(
            "buildTierSetting",
            new IntSyncValue(multiblock::getBuildTierSetting, multiblock::setBuildTierSetting).allowC2S());
        // F4: the authorised overspeed. A WRITEABLE handle, so it is also the value every
        // redline readout on this panel derives from - the client takes the unlocked ceiling
        // from the synced counters and node mask and runs the same MTEReactorCoreMk1
        // .operatingRedline body the server runs (AXIS 1).
        syncManager.syncValue(
            "rpmLimitSetting",
            new IntSyncValue(multiblock::getRpmLimitSetting, multiblock::setRpmLimitSetting).allowC2S());
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
        final StringSyncValue plantModules = syncManager.findSyncHandler("plantModules", StringSyncValue.class);
        final BooleanSyncValue ventStack = syncManager.findSyncHandler("ventStack", BooleanSyncValue.class);
        final BooleanSyncValue switchyard = syncManager.findSyncHandler("switchyard", BooleanSyncValue.class);
        final BooleanSyncValue derrick = syncManager.findSyncHandler("derrick", BooleanSyncValue.class);
        final BooleanSyncValue annex = syncManager.findSyncHandler("annex", BooleanSyncValue.class);
        final IntSyncValue derrickCycles = syncManager.findSyncHandler("derrickCycles", IntSyncValue.class);
        final IntSyncValue annexCycles = syncManager.findSyncHandler("annexCycles", IntSyncValue.class);
        final StringSyncValue moduleStall = syncManager.findSyncHandler("moduleStall", StringSyncValue.class);
        final LongSyncValue gridCeiling = syncManager.findSyncHandler("gridCeiling", LongSyncValue.class);
        final BooleanSyncValue gridCapEnabled = syncManager.findSyncHandler("gridCapEnabled", BooleanSyncValue.class);
        final BooleanSyncValue gridCapped = syncManager.findSyncHandler("gridCapped", BooleanSyncValue.class);
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
        final IntSyncValue rpmLimit = syncManager.findSyncHandler("rpmLimitSetting", IntSyncValue.class);
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
        final LongSyncValue progUptime = syncManager.findSyncHandler("progUptime", LongSyncValue.class);
        final LongSyncValue progBurnup = syncManager.findSyncHandler("progBurnup", LongSyncValue.class);
        final LongSyncValue progReproc = syncManager.findSyncHandler("progReproc", LongSyncValue.class);
        final LongSyncValue progPerfect = syncManager.findSyncHandler("progPerfect", LongSyncValue.class);
        final LongSyncValue treeNodes = syncManager.findSyncHandler("treeNodes", LongSyncValue.class);
        // The four counters in BTSMilestone ordinal order - the one shape every static
        // derivation in BTSProgression takes. Re-read on every frame, like every other row.
        final Supplier<long[]> progress = () -> new long[] { progUptime.getLongValue(), progBurnup.getLongValue(),
            progReproc.getLongValue(), progPerfect.getLongValue() };

        return super.createTerminalTextWidget(syncManager, parent)
            // --- REACTOR
            .child(row(() -> MTEReactorCoreMk1.header("REACTOR")))
            .child(
                row(
                    () -> MTEReactorCoreMk1.siloAbsentLine(standingTier.getIntValue()),
                    () -> !siloBuilt.getBoolValue()))
            .child(
                row(
                    () -> MTEReactorCoreMk1.tierLine(tier.getIntValue(), edge.getIntValue(), throttlePct.getIntValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.supplyLine(
                        siloBuilt.getBoolValue(),
                        rodsQueued.getIntValue(),
                        pillarGaps.getIntValue(),
                        propellantLitres.getIntValue(),
                        coolantLitres.getIntValue())))
            // D-21: UNGATED, exactly like statusReport's. The tower inventory used to ride
            // the tail of tierLine, which is gated on siloBuilt - so a tower fault was
            // invisible on a bare platform, which is the state a player builds towers in and
            // the state where the only other panel (the Silo Control Block) does not exist
            // yet, because it IS one of the silo face's sockets.
            .child(row(() -> MTEReactorCoreMk1.towerLine(modules.getStringValue())))
            // D-17: the other four buildings, on the same terms and for the same reason - all
            // four are most likely to be built on a BARE PLATFORM, where this is the only panel
            // that exists (the Silo Control Block IS one of the silo face's sockets).
            .child(row(() -> MTEReactorCoreMk1.moduleLine(plantModules.getStringValue())))
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
                    () -> MTEReactorCoreMk1.heatLine(
                        heat.getIntValue(),
                        decaySeconds.getIntValue(),
                        radiationSeconds.getIntValue(),
                        ventStack.getBoolValue()),
                    siloBuilt::getBoolValue))
            // The two self-fuelling modules and whatever is holding them up. Ungated on the
            // silo - a Derrick on a bare platform mines perfectly well - but hidden while
            // neither of them stands, which is most plants.
            // C2: ...unless there is a STALL to report, which is exactly the case where a module
            // has just been broken open with a committed batch inside it. The batch is being
            // HELD, not eaten, and the player can only know that if the line stays up.
            .child(
                row(
                    () -> MTEReactorCoreMk1.selfFuelLine(
                        derrick.getBoolValue(),
                        annex.getBoolValue(),
                        derrickCycles.getIntValue(),
                        annexCycles.getIntValue(),
                        moduleStall.getStringValue()),
                    () -> derrick.getBoolValue() || annex.getBoolValue()
                        || !moduleStall.getStringValue()
                            .isEmpty()))
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
                        MTEReactorCoreMk1.operatingRedline(
                            BTSProgression
                                .redlineRpm(progress.get(), treeNodes.getLongValue(), MTEReactorCoreMk1.RPM_REDLINE),
                            rpmLimit.getIntValue()),
                        BTSProgression
                            .redlineRpm(progress.get(), treeNodes.getLongValue(), MTEReactorCoreMk1.RPM_REDLINE),
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
                        gridCapped.getBoolValue(),
                        steamOutput.getLongValue()),
                    siloBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1
                        .ceilingLine(euCeiling.getLongValue(), dynamoCap.getLongValue(), laserCap.getLongValue()),
                    galleryBuilt::getBoolValue))
            // D-17: the grid soft cap, in whichever of its four states this plant is in, right
            // under the hatch ceiling it shares a Math.min with.
            .child(
                row(
                    () -> MTEReactorCoreMk1.switchyardLine(
                        switchyard.getBoolValue(),
                        gridCapEnabled.getBoolValue(),
                        liveDemand.getLongValue(),
                        euCeiling.getLongValue()),
                    galleryBuilt::getBoolValue))
            .child(
                row(
                    () -> MTEReactorCoreMk1.laserLine(laserCount.getIntValue(), laserLinked.getIntValue()),
                    galleryBuilt::getBoolValue))
            // C1/C5: a non-fatal note, not an error. The REACTOR never draws EU, so an empty E
            // socket cannot un-form a running reactor - but the two D-17 process modules DO draw
            // through it, and energyHatchNote says which of the two truths applies.
            .child(
                row(
                    () -> MTEReactorCoreMk1.energyHatchNote(
                        true,
                        energyHatches.getIntValue(),
                        derrick.getBoolValue() || annex.getBoolValue()),
                    () -> galleryBuilt.getBoolValue() && (energyHatches.getIntValue() < 4
                        || (energyHatches.getIntValue() <= 0 && (derrick.getBoolValue() || annex.getBoolValue())))))
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
                    () -> MTEReactorCoreMk1.mathTurbineLine(
                        rpm.getIntValue(),
                        rotorFactor.getIntValue(),
                        BTSProgression.outputMilli(progress.get()),
                        BTSProgression.upgradeMilli(treeNodes.getLongValue()),
                        liveDemand.getLongValue()),
                    () -> siloBuilt.getBoolValue() && galleryActive.getBoolValue()))
            .child(
                row(
                    () -> MTEReactorCoreMk1
                        .mathClampLine(liveDemand.getLongValue(), euCeiling.getLongValue(), gridCeiling.getLongValue()),
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
                        // F7: the live cap, derived CLIENT-side from the same two synced values
                        // and the same static body the server's maxThrottle() calls (AXIS 1) -
                        // no thirtieth sync handle for a number four longs already determine.
                        BTSProgression.maxThrottle(progress.get(), treeNodes.getLongValue(), 24),
                        spineSpeed.getIntValue(),
                        spineEff.getIntValue(),
                        rpm.getIntValue(),
                        MTEReactorCoreMk1.operatingRedline(
                            BTSProgression
                                .redlineRpm(progress.get(), treeNodes.getLongValue(), MTEReactorCoreMk1.RPM_REDLINE),
                            rpmLimit.getIntValue()),
                        injectors.getIntValue(),
                        injectorsDry.getBoolValue(),
                        coolantReady.getBoolValue(),
                        rotorFactor.getIntValue(),
                        liveDemand.getLongValue(),
                        euCeiling.getLongValue(),
                        gridCeiling.getLongValue()),
                    siloBuilt::getBoolValue))
            // --- D-23 MILESTONES: the campaign record, in the same order and off the same
            // static formatters statusReport() uses, so the GUI, both control blocks and the
            // scanner cannot print three different tiers for the same line. UNGATED on the
            // silo - REPROCESSING accumulates on a bare platform's Derrick, and this is the
            // only panel that exists while a player is still building the silo.
            .child(row(() -> MTEReactorCoreMk1.header("MILESTONES")))
            .child(row(() -> BTSMilestone.milestoneLine(BTSMilestone.UPTIME, progUptime.getLongValue())))
            .child(row(() -> BTSMilestone.milestoneLine(BTSMilestone.BURNUP, progBurnup.getLongValue())))
            .child(row(() -> BTSMilestone.milestoneLine(BTSMilestone.REPROCESSING, progReproc.getLongValue())))
            .child(row(() -> BTSMilestone.milestoneLine(BTSMilestone.TURBINE, progPerfect.getLongValue())))
            .child(row(() -> BTSMilestone.milestoneSummaryLine(progress.get())))
            .child(row(() -> BTSMilestone.milestoneUnlockLine(progress.get())))
            // --- D-24 UPGRADES: what those points have been spent on, and what the purchases
            // are moving. Off the same two static formatters statusReport() calls, so the GUI,
            // both control blocks and the scanner print one tree. The graph itself lives on the
            // side panel behind the button in the gap row.
            .child(row(() -> BTSUpgrade.treeSummaryLine(treeNodes.getLongValue(), progress.get())))
            .child(row(() -> BTSUpgrade.treeEffectLine(treeNodes.getLongValue())))
            // --- test bench flags, only while one of them is on
            .child(
                row(
                    () -> MTEReactorCoreMk1.debugLine(benchClock.getBoolValue(), infiniteSupplies.getBoolValue()),
                    () -> benchClock.getBoolValue() || infiniteSupplies.getBoolValue()));
    }

    /**
     * The three editable fields, in the order they are DRAWN: Build Tier on the left, then the
     * RPM LIMIT, then the control-rod throttle on the right.
     * <p>
     * GT's gap row is {@code Flow.row().mainAxisAlignment(END)...reverseLayout(true)}, so the
     * flow lays its children out through a {@code ReversedList} - the LAST child added is the
     * LEFTMOST on screen. That is why the throttle is appended first and Build Tier second, and
     * why the questbook's step 2 names Build Tier first: both describe the same left-to-right
     * strip. Add a child here and it lands on the LEFT of everything already in the row.
     */
    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        return super.createRightPanelGapRow(parent, syncManager).child(
            // D-23/D-24: the field's range is the full 1..36 because a MUI2 text field's bounds
            // are fixed when the widget is built, and the CLIENT does not get to decide what
            // this plant may do. setThrottleSetting clamps to the unlocked cap server-side and
            // the field re-syncs, so a plant with neither the BURNUP unlocks nor the tree's rod
            // nodes types 36 and lands on 24. That is the safe direction: a field offering a
            // value the server refuses, never a field refusing one the server would accept.
            // 36 is the absolute ceiling - 24 design + 6 from BURNUP T5/6/7 + 6 from the tree's
            // Rod Seals, Deep Burn Profile and Overpower Licence.
            new TextFieldWidget().formatAsInteger(true)
                .defaultNumber(24)
                .numbersInt(1, 36)
                .syncHandler("throttleSetting")
                .width(30)
                .tooltip(
                    t -> t.addLine(translateToLocal("beyondthestars.gui.throttle.tooltip"))
                        .addLine(translateToLocal("beyondthestars.gui.throttle.milestone"))))
            // F4: the RPM LIMIT, sitting next to the rods field because the two are the same
            // kind of control - an operating limit the player sets, held below a ceiling the
            // campaign raises. Range fixed at 12,000..19,500 for the same reason the rods field
            // is 1..36: a MUI2 field's bounds are decided when the widget is built and the
            // CLIENT does not get to decide what this plant may do. setRpmLimitSetting clamps
            // to the unlocked ceiling server-side and the field re-syncs.
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .defaultNumber(MTEReactorCoreMk1.RPM_REDLINE)
                    .numbersInt(MTEReactorCoreMk1.RPM_REDLINE, MTEReactorCoreMk1.RPM_LIMIT_MAX)
                    .syncHandler("rpmLimitSetting")
                    .width(36)
                    .tooltip(
                        t -> t.addLine(translateToLocal("beyondthestars.gui.rpmlimit.tooltip"))
                            .addLine(translateToLocal("beyondthestars.gui.rpmlimit.milestone"))))
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .defaultNumber(1)
                    .numbersInt(1, 5)
                    .syncHandler("buildTierSetting")
                    .width(20)
                    .tooltip(t -> t.addLine(translateToLocal("beyondthestars.gui.buildtier.tooltip"))))
            // D-24: the upgrade tree opens on a panel of its own, so the controller's own layout
            // is untouched - this row is reverseLayout(true), so the last child added is the
            // LEFTMOST, and the three fields keep their places against the right edge. F8: that
            // panel is centred on the SCREEN rather than hung off this one's right edge, which
            // is what put two thirds of it off a 480x270 viewport at GUI Scale 4.
            .child(BTSUpgradeTreePanel.openButton(syncManager));
    }
}
