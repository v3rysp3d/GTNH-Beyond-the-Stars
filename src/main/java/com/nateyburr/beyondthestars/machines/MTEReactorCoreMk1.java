package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Dynamo;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.SimpleShutDownReason;

/**
 * The Reactor Silo - ONE controller for the whole Nuclear Campus generation ladder. A rounded
 * (octagonal) silo: bio-shield floor, lower plenum, three fuel-column layers, upper plenum and
 * containment lid, with an OPTIONAL Turbine Deck addon built on top (turbine casings + shaft +
 * dynamo hatches) that turns the silo into a direct EU generator. Five silo diameters on one
 * controller; walls change material as tiers rise (radiation-proof -> europium-reinforced ->
 * radiant naquadah alloy), shields upgrade from tungstensteel- to iridium-reinforced blocks.
 * Without the deck the silo exports Steam / Hot Coolant exactly as before. With the deck and an
 * installed fan (input bus) it generates EU directly: fan T multiplier, stocked-coolant K
 * multiplier, naquadah propellant P multiplier, with blowout and seizure on misuse.
 * Full formula and numbers per docs/reactor-gen1.md.
 */
public class MTEReactorCoreMk1 extends MTEExtendedPowerMultiBlockBase<MTEReactorCoreMk1>
    implements ISurvivalConstructable {

    /** L/t of steam per full fuel column at 100% throttle. */
    private static final long STEAM_PER_COLUMN = 16_384L;
    private static final int SCRAM_HEAT = 8_000;
    private static final int MELTDOWN_HEAT = 10_000;
    private static final int HEAT_PER_STARVED_TICK = 4;
    private static final int HEAT_DECAY_PER_TICK = 2;
    private static final int DECAY_HEAT_TICKS = 24_000;
    private static final int DECAY_WATER_PER_TICK = 64;
    private static final int RADIATION_TICKS = 36_000;
    private static final int RADIATION_RADIUS = 12;
    private static final int RADIATION_LEVEL = 3;
    private static final long EU_PER_HOT_COOLANT = 200L;
    private static final ShutDownReason SCRAM_REASON = SimpleShutDownReason.ofCritical("beyondthestars.scram");
    private static final ShutDownReason MELTDOWN_REASON = SimpleShutDownReason.ofCritical("beyondthestars.meltdown");
    private static final ShutDownReason BLOWOUT_REASON = SimpleShutDownReason.ofCritical("beyondthestars.blowout");
    private static final ShutDownReason SEIZURE_REASON = SimpleShutDownReason.ofCritical("beyondthestars.seizure");

    private static final int MAX_TIER = 5;
    /** Octagon corner cut per tier (0 unused). */
    private static final int[] CUT = { 0, 1, 1, 2, 2, 3 };
    /** Interior fuel-column capacity per tier (0 unused). */
    private static final int[] COLUMN_CAP = { 0, 9, 25, 45, 77, 109 };

    private static IStructureDefinition<MTEReactorCoreMk1> STRUCTURE_DEFINITION = null;

    private int mCasing;
    private int turbineCasings;
    private int reactorTier;
    private boolean turbineInstalled;
    private int fuelSegments;
    private int activeColumns;
    private int throttleN = 24;
    private int activeFuelMult = 1;
    private int heat;
    private int decayHeatTicks;
    private int radiationTicks;
    private long steamPerTick;
    private long lastEUOutput;
    private boolean starvedThisTick;
    private boolean wasRunningLastTick;
    private boolean boostedLastTick;
    /** One free pass after chunk load so a booting AE grid cannot false-seize the fan. */
    private transient boolean graceCheck;

    public MTEReactorCoreMk1(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEReactorCoreMk1(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEReactorCoreMk1(this.mName);
    }

    // ------------------------------------------------------------------ structure

    /** Silo cross-section edge for a tier: 5, 7, 9, 11, 13. */
    private static int edgeFor(int tier) {
        return 3 + 2 * tier;
    }

    private static String corePiece(String base, int tier) {
        return base + ".tier" + tier + ".core";
    }

    private static String turbinePiece(String base, int tier) {
        return base + ".tier" + tier + ".turbine";
    }

    private static boolean insideOct(int n, int k, int r, int x) {
        return Math.min(r, n - 1 - r) + Math.min(x, n - 1 - x) >= k;
    }

    private static boolean interiorCell(int n, int k, int r, int x) {
        return r >= 1 && r <= n - 2 && x >= 1 && x <= n - 2 && Math.min(r, n - 1 - r) + Math.min(x, n - 1 - x) >= k + 1;
    }

    private static int octagonCells(int tier) {
        final int n = edgeFor(tier);
        final int k = CUT[tier];
        return n * n - 2 * k * (k + 1);
    }

    private static int ringCells(int tier) {
        return octagonCells(tier) - COLUMN_CAP[tier];
    }

    private static int minCasingsFor(int tier) {
        // Five wall-ring layers (two plenums + three fuel), minus controller and 8 hatches.
        return 5 * ringCells(tier) - 9;
    }

    private enum LayerKind {
        SHIELD,
        PLENUM,
        FUEL,
        FUEL_CTRL,
        TURBINE
    }

    private static String[] buildLayer(int tier, LayerKind kind) {
        final int n = edgeFor(tier);
        final int k = CUT[tier];
        final char wall = tier >= 5 ? 'E' : tier >= 3 ? 'D' : 'C';
        final char shield = tier >= 3 ? 'I' : 'B';
        final String[] rows = new String[n];
        for (int r = 0; r < n; r++) {
            final char[] row = new char[n];
            for (int x = 0; x < n; x++) {
                if (!insideOct(n, k, r, x)) {
                    row[x] = ' ';
                } else if (kind == LayerKind.SHIELD) {
                    row[x] = shield;
                } else if (kind == LayerKind.TURBINE) {
                    row[x] = (r == n / 2 && x == n / 2) ? 'P' : interiorCell(n, k, r, x) ? 'A' : 'T';
                } else if (interiorCell(n, k, r, x)) {
                    row[x] = kind == LayerKind.PLENUM ? 'A' : 'F';
                } else {
                    row[x] = (kind == LayerKind.FUEL_CTRL && r == 0 && x == n / 2) ? '~' : wall;
                }
            }
            rows[r] = new String(row);
        }
        return rows;
    }

    /** Lid, upper plenum, three fuel layers (controller mid), lower plenum, floor. */
    private static String[][] coreShape(int tier) {
        return new String[][] { buildLayer(tier, LayerKind.SHIELD), buildLayer(tier, LayerKind.PLENUM),
            buildLayer(tier, LayerKind.FUEL), buildLayer(tier, LayerKind.FUEL_CTRL), buildLayer(tier, LayerKind.FUEL),
            buildLayer(tier, LayerKind.PLENUM), buildLayer(tier, LayerKind.SHIELD) };
    }

    /** Dome cap over two turbine-deck layers; sits directly on the core lid. */
    private static String[][] turbineShape(int tier) {
        return new String[][] { buildLayer(tier, LayerKind.SHIELD), buildLayer(tier, LayerKind.TURBINE),
            buildLayer(tier, LayerKind.TURBINE) };
    }

    @Override
    public IStructureDefinition<MTEReactorCoreMk1> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final StructureDefinition.Builder<MTEReactorCoreMk1> builder = StructureDefinition.builder();
            for (int t = 1; t <= MAX_TIER; t++) {
                builder.addShape(corePiece(mName, t), transpose(coreShape(t)));
                builder.addShape(turbinePiece(mName, t), transpose(turbineShape(t)));
            }
            STRUCTURE_DEFINITION = builder.addElement('B', ofBlock(GregTechAPI.sBlockReinforced, 3))
                .addElement('I', ofBlock(GregTechAPI.sBlockReinforced, 1))
                .addElement('A', isAir())
                .addElement('P', Casings.SteelPipeCasing.asElement())
                .addElement(
                    'C',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())))
                .addElement(
                    'D',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.EuropiumReinforcedRadiationProofMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(
                                x -> ++x.mCasing,
                                Casings.EuropiumReinforcedRadiationProofMachineCasing.asElement())))
                .addElement(
                    'E',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.RadiantNaquadahAlloyCasing.textureId)
                        .hint(1)
                        .buildAndChain(onElementPass(x -> ++x.mCasing, Casings.RadiantNaquadahAlloyCasing.asElement())))
                .addElement(
                    'T',
                    buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Dynamo, Maintenance)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(2)
                        .buildAndChain(
                            onElementPass(x -> ++x.turbineCasings, Casings.TungstensteelTurbineCasing.asElement())))
                .addElement(
                    'F',
                    ofChain(onElementPass(x -> ++x.fuelSegments, ofBlock(BTSBlocks.reactorFuelColumn, 0)), isAir()))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        reactorTier = 0;
        turbineInstalled = false;
        // Largest silo first; only the smallest attempt reports structure errors. Failed walks
        // register hatches as a side effect, so clear between probes (GT mega-multi precedent).
        for (int t = MAX_TIER; t >= 1; t--) {
            clearHatches();
            mCasing = 0;
            turbineCasings = 0;
            fuelSegments = 0;
            final boolean smallest = t == 1;
            if (checkPiece(corePiece(mName, t), edgeFor(t) / 2, 3, 0, smallest ? errors : null)) {
                reactorTier = t;
                break;
            }
            if (smallest) return;
        }
        // Optional Turbine Deck addon above the lid. A failed probe may leave stray hatches, so
        // rebuild the core registration cleanly in that case.
        turbineInstalled = checkPiece(turbinePiece(mName, reactorTier), edgeFor(reactorTier) / 2, 6, 0, null);
        if (!turbineInstalled) {
            clearHatches();
            mCasing = 0;
            turbineCasings = 0;
            fuelSegments = 0;
            checkPiece(corePiece(mName, reactorTier), edgeFor(reactorTier) / 2, 3, 0, null);
        }
        checkCasingMin(errors, mCasing, minCasingsFor(reactorTier));
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
        if (turbineInstalled) {
            checkHatchMin(errors, Dynamo, 1);
        }
    }

    private static int tierFromTrigger(ItemStack stackSize) {
        return Math.min(Math.max(stackSize.stackSize, 1), MAX_TIER);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        final int t = tierFromTrigger(stackSize);
        int built = survivalBuildPiece(
            corePiece(mName, t),
            stackSize,
            edgeFor(t) / 2,
            3,
            0,
            elementBudget,
            env,
            false,
            true);
        if (built >= 0 && built < elementBudget) {
            final int more = survivalBuildPiece(
                turbinePiece(mName, t),
                stackSize,
                edgeFor(t) / 2,
                6,
                0,
                elementBudget - built,
                env,
                false,
                true);
            if (more > 0) built += more;
        }
        return built;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int t = tierFromTrigger(stackSize);
        buildPiece(corePiece(mName, t), stackSize, hintsOnly, edgeFor(t) / 2, 3, 0);
        buildPiece(turbinePiece(mName, t), stackSize, hintsOnly, edgeFor(t) / 2, 6, 0);
    }

    // ------------------------------------------------------------------ helpers

    private int countMatchingInputs(ItemStack sample) {
        int available = 0;
        for (ItemStack stack : getStoredInputs()) {
            if (GTUtility.areStacksEqual(sample, stack)) {
                available += stack.stackSize;
            }
        }
        return available;
    }

    private int countFluid(ArrayList<FluidStack> stored, FluidStack sample) {
        if (sample == null) return 0;
        long amount = 0;
        for (FluidStack stack : stored) {
            if (stack != null && stack.isFluidEqual(sample)) {
                amount += stack.amount;
            }
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    /** 0 = none, 1 = matched (x1.25), 2 = TDA (x1.25, propellant-rated), 3 = optimized (x2.5). */
    private int installedFanTier() {
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanTDAOptimized, 1)) > 0) return 3;
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanTDA, 1)) > 0) return 2;
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanMatched, 1)) > 0) return 1;
        return 0;
    }

    private void destroyFan(int fanTier, ShutDownReason reason) {
        final Item fan = fanTier == 3 ? BTSItems.turbineFanTDAOptimized
            : fanTier == 2 ? BTSItems.turbineFanTDA : BTSItems.turbineFanMatched;
        depleteInput(new ItemStack(fan, 1));
        updateSlots();
        boostedLastTick = false;
        stopMachine(reason);
        getBaseMetaTileEntity().disableWorking();
        if (Config.meltdownExplosions && GregTechAPI.sMachineExplosions
            && getBaseMetaTileEntity().getRandomNumber(4) == 0) {
            getBaseMetaTileEntity().getWorld()
                .newExplosion(
                    null,
                    getBaseMetaTileEntity().getXCoord() + 0.5,
                    getBaseMetaTileEntity().getYCoord() - 2.5,
                    getBaseMetaTileEntity().getZCoord() + 0.5,
                    2.0F,
                    false,
                    true);
        }
    }

    private long maxDynamoOutput() {
        long total = 0;
        for (MTEHatchDynamo dynamo : validMTEList(mDynamoHatches)) {
            total += (long) dynamo.maxAmperesOut() * dynamo.maxEUOutput();
        }
        return total;
    }

    // ------------------------------------------------------------------ campaign

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        int columns = Math.min(fuelSegments / 3, COLUMN_CAP[Math.max(1, reactorTier)]);
        if (columns <= 0) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        int n = 24;
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(24, circuit.getItemDamage());
        }

        // Fuel ladder (best first): naq-bonded x9, thorium-bonded x5, MOX x2, uranium x1.
        final Item[] assemblies = { BTSItems.fuelAssemblyNaqBonded, BTSItems.fuelAssemblyThoriumBonded,
            BTSItems.fuelAssemblyMox, BTSItems.fuelAssemblyUranium };
        final Item[] depletedForms = { BTSItems.depletedFuelAssemblyNaqBonded,
            BTSItems.depletedFuelAssemblyThoriumBonded, BTSItems.depletedFuelAssemblyMox,
            BTSItems.depletedFuelAssemblyUranium };
        final int[] fuelMults = { 9, 5, 2, 1 };
        ItemStack chosen = null;
        ItemStack depleted = null;
        int fuelMult = 1;
        for (int i = 0; i < assemblies.length; i++) {
            final ItemStack candidate = new ItemStack(assemblies[i], 1);
            if (countMatchingInputs(candidate) >= columns) {
                chosen = candidate;
                depleted = new ItemStack(depletedForms[i], columns);
                fuelMult = fuelMults[i];
                break;
            }
        }
        if (chosen == null) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        for (int i = 0; i < columns; i++) {
            depleteInput(chosen);
        }
        updateSlots();

        activeColumns = columns;
        throttleN = n;
        activeFuelMult = fuelMult;
        steamPerTick = STEAM_PER_COLUMN * columns * fuelMult * n / 24;
        this.mOutputItems = new ItemStack[] { depleted };
        this.mMaxProgresstime = Math.max(1, Config.genICampaignMinutes) * 60 * 20;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        this.lEUt = 0;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (steamPerTick <= 0 || !getBaseMetaTileEntity().isServerSide()) return true;

        final int flowPerTick = (int) ((steamPerTick + GTValues.STEAM_PER_WATER - 1) / GTValues.STEAM_PER_WATER);
        startRecipeProcessing();
        if (turbineInstalled) {
            runTurbineTick(flowPerTick);
        } else {
            runFluidTick(flowPerTick);
        }
        endRecipeProcessing();
        return true;
    }

    /** Fluid-export mode: the silo without a Turbine Deck behaves exactly as before. */
    private void runFluidTick(int flowPerTick) {
        final int coolantFlow = reactorTier >= MAX_TIER ? flowPerTick * 2 : flowPerTick;
        if (depleteInput(GTModHandler.getIC2Coolant(coolantFlow))) {
            addOutput(GTModHandler.getHotCoolant(coolantFlow));
            emitWasteWater();
        } else if (depleteInput(Materials.Water.getFluid(flowPerTick))
            || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                addOutput(Materials.Steam.getGas(steamPerTick));
                emitWasteWater();
            } else {
                starveTick();
            }
    }

    /**
     * Turbine Deck mode: feedwater is consumed, stocked coolant (not consumed) carries K, the
     * installed fan carries T, propellant carries P, and EU goes straight to the dynamos.
     */
    private void runTurbineTick(int flowPerTick) {
        final int fanTier = installedFanTier();
        final ArrayList<FluidStack> stored = getStoredFluids();

        // Propellant presence (Mk III > II > I) - demands a TDA-rated fan.
        final Fluid[] propellants = { BTSFluids.naqPropellantT3, BTSFluids.naqPropellantT2, BTSFluids.naqPropellantT1 };
        final int[] propellantMults = { 10, 3, 2 };
        int foundPropellant = -1;
        for (int i = 0; i < propellants.length; i++) {
            if (propellants[i] != null && countFluid(stored, new FluidStack(propellants[i], 1)) > 0) {
                foundPropellant = i;
                break;
            }
        }
        if (foundPropellant >= 0 && fanTier > 0 && fanTier < 2) {
            destroyFan(fanTier, BLOWOUT_REASON);
            return;
        }

        // No fan installed: the deck cannot spin - fall back to fluid export.
        if (fanTier == 0) {
            if (boostedLastTick && !graceCheck) {
                // Fan pulled mid-boost counts as seizure of what remains: just stop the boost.
                boostedLastTick = false;
            }
            graceCheck = false;
            runFluidTick(flowPerTick);
            return;
        }

        // Feedwater keeps the cycle alive; starving it heats the core exactly as fluid mode.
        if (!depleteInput(Materials.Water.getFluid(flowPerTick))
            && !depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
            starveTick();
            return;
        }

        final boolean canSeize = boostedLastTick && !graceCheck;

        // Stocked coolant charge (present, not consumed): K = 2.5, or 5 on tier-5 high-flow.
        long euThisTick = steamPerTick / 2;
        final int coolantStock = countFluid(stored, GTModHandler.getIC2Coolant(1));
        if (reactorTier >= MAX_TIER && coolantStock >= flowPerTick * 2) {
            euThisTick = euThisTick * 5;
        } else if (coolantStock >= flowPerTick) {
            euThisTick = euThisTick * 5 / 2;
        }

        // Propellant burn: single litres per tick.
        int propellantMult = 1;
        if (foundPropellant >= 0) {
            final int litres = (int) Math.max(1, euThisTick / 5_000_000L);
            if (depleteInput(new FluidStack(propellants[foundPropellant], litres))) {
                propellantMult = propellantMults[foundPropellant];
                boostedLastTick = true;
            } else if (canSeize) {
                destroyFan(fanTier, SEIZURE_REASON);
                return;
            } else {
                boostedLastTick = false;
            }
        } else if (canSeize) {
            destroyFan(fanTier, SEIZURE_REASON);
            return;
        } else {
            boostedLastTick = false;
        }
        graceCheck = false;

        // T multiplier, P multiplier, then the safety clamps (dynamo capacity + int-safe EU).
        long output = fanTier == 3 ? euThisTick * 5 / 2 : euThisTick * 5 / 4;
        output *= propellantMult;
        output = Math.min(output, maxDynamoOutput());
        output = Math.min(output, Integer.MAX_VALUE);
        addEnergyOutput(output);
        lastEUOutput = output;
        emitWasteWater();
    }

    private void starveTick() {
        starvedThisTick = true;
        heat += HEAT_PER_STARVED_TICK;
        if (heat >= SCRAM_HEAT) {
            scram();
        }
    }

    private void emitWasteWater() {
        if (activeColumns > 0 && BTSFluids.nuclearWasteWater != null) {
            addOutput(new FluidStack(BTSFluids.nuclearWasteWater, activeColumns));
        }
    }

    private void clearCampaign() {
        steamPerTick = 0;
        activeColumns = 0;
        lastEUOutput = 0;
    }

    /** Campaign lost, machine disabled until the operator re-enables it. Decay heat follows. */
    private void scram() {
        clearCampaign();
        stopMachine(SCRAM_REASON);
        getBaseMetaTileEntity().disableWorking();
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        clearCampaign();
        boostedLastTick = false;
        super.stopMachine(reason);
    }

    /** Vertical detonation and a lingering radiation field; both honor config toggles. */
    private void meltdown() {
        heat = 0;
        decayHeatTicks = 0;
        clearCampaign();
        stopMachine(MELTDOWN_REASON);
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        base.disableWorking();
        if (Config.meltdownExplosions && GregTechAPI.sMachineExplosions) {
            final World world = base.getWorld();
            for (int dy = 3; dy <= 12; dy += 3) {
                world.newExplosion(
                    null,
                    base.getXCoord() + 0.5,
                    base.getYCoord() + dy + 0.5,
                    base.getZCoord() + 0.5,
                    3.0F,
                    true,
                    true);
            }
        }
        if (Config.radiationField) {
            radiationTicks = RADIATION_TICKS;
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;

        final boolean running = mMaxProgresstime > 0;
        if (wasRunningLastTick && !running) {
            decayHeatTicks = DECAY_HEAT_TICKS;
        }
        wasRunningLastTick = running;

        // The decay-heat obligation only ticks down while the structure stands - dismantling the
        // reactor merely defers it, it never waives it (and a broken hot core stays hot).
        if (decayHeatTicks > 0 && mMachine) {
            decayHeatTicks--;
            if (!running) {
                startRecipeProcessing();
                boolean cooled = depleteInput(Materials.Water.getFluid(DECAY_WATER_PER_TICK))
                    || depleteInput(GTModHandler.getDistilledWater(DECAY_WATER_PER_TICK))
                    || depleteInput(GTModHandler.getIC2Coolant(DECAY_WATER_PER_TICK));
                endRecipeProcessing();
                if (!cooled) {
                    starvedThisTick = true;
                    heat += HEAT_PER_STARVED_TICK / 2;
                    if (heat >= MELTDOWN_HEAT) {
                        meltdown();
                    }
                }
            }
        }

        if (radiationTicks > 0) {
            radiationTicks--;
            if (aTick % 40 == 0) {
                final World world = aBaseMetaTileEntity.getWorld();
                final int x = aBaseMetaTileEntity.getXCoord();
                final int y = aBaseMetaTileEntity.getYCoord();
                final int z = aBaseMetaTileEntity.getZCoord();
                final List<EntityLivingBase> victims = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    AxisAlignedBB.getBoundingBox(
                        x - RADIATION_RADIUS,
                        y - RADIATION_RADIUS,
                        z - RADIATION_RADIUS,
                        x + RADIATION_RADIUS + 1,
                        y + RADIATION_RADIUS + 1,
                        z + RADIATION_RADIUS + 1));
                for (EntityLivingBase living : victims) {
                    if (living instanceof EntityPlayer && ((EntityPlayer) living).capabilities.isCreativeMode) {
                        continue;
                    }
                    GTUtility.applyRadioactivity(living, RADIATION_LEVEL, 1);
                }
            }
        }

        final boolean brokenHotCore = !mMachine && decayHeatTicks > 0;
        if (!starvedThisTick && !brokenHotCore && heat > 0) {
            heat = Math.max(0, heat - HEAT_DECAY_PER_TICK);
        }
        starvedThisTick = false;
    }

    // ------------------------------------------------------------------ persistence & info

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsHeat", heat);
        aNBT.setInteger("btsReactorTier", reactorTier);
        aNBT.setBoolean("btsTurbine", turbineInstalled);
        aNBT.setInteger("btsActiveColumns", activeColumns);
        aNBT.setInteger("btsThrottleN", throttleN);
        aNBT.setInteger("btsFuelMult", activeFuelMult);
        aNBT.setInteger("btsDecayHeatTicks", decayHeatTicks);
        aNBT.setInteger("btsRadiationTicks", radiationTicks);
        aNBT.setLong("btsSteamPerTick", steamPerTick);
        aNBT.setBoolean("btsBoosted", boostedLastTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        heat = aNBT.getInteger("btsHeat");
        reactorTier = aNBT.getInteger("btsReactorTier");
        turbineInstalled = aNBT.getBoolean("btsTurbine");
        activeColumns = aNBT.getInteger("btsActiveColumns");
        throttleN = aNBT.getInteger("btsThrottleN");
        activeFuelMult = Math.max(1, aNBT.getInteger("btsFuelMult"));
        decayHeatTicks = aNBT.getInteger("btsDecayHeatTicks");
        radiationTicks = aNBT.getInteger("btsRadiationTicks");
        steamPerTick = aNBT.getLong("btsSteamPerTick");
        boostedLastTick = aNBT.getBoolean("btsBoosted");
        graceCheck = true;
    }

    private static String fuelName(int mult) {
        switch (mult) {
            case 9:
                return "Naquadah-Bonded (x9)";
            case 5:
                return "Thorium-Bonded (x5)";
            case 2:
                return "MOX (x2)";
            default:
                return "Uranium";
        }
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        final int tier = Math.max(1, reactorTier);
        info.add("Silo tier: " + tier + " (" + edgeFor(tier) + " wide octagon)");
        info.add("Fuel columns: " + Math.min(fuelSegments / 3, COLUMN_CAP[tier]) + " / " + COLUMN_CAP[tier]);
        info.add("Fuel type: " + fuelName(activeFuelMult));
        info.add("Control rods: " + (throttleN * 100 / 24) + "% power");
        if (turbineInstalled) {
            final int fan = installedFanTier();
            info.add(
                "Addon - Turbine Deck: installed (fan: "
                    + (fan == 0 ? "NONE - install one in an input bus"
                        : fan == 3 ? "Optimized TDA x2.5" : fan == 2 ? "TDA x1.25, propellant-rated" : "Matched x1.25")
                    + ")");
            info.add("Generating: " + lastEUOutput + " EU/t (dynamo-clamped)");
        } else {
            info.add("Addon - Turbine Deck: not built (silo exports Steam / Hot Coolant)");
            info.add("Steam output: " + steamPerTick + " L/t");
        }
        info.add("Core heat: " + heat + " / " + MELTDOWN_HEAT + " (SCRAM at " + SCRAM_HEAT + ")");
        if (decayHeatTicks > 0) {
            info.add("Decay heat: cooling required for " + (decayHeatTicks / 20) + "s");
        }
        if (radiationTicks > 0) {
            info.add("RADIATION FIELD: " + (radiationTicks / 20) + "s remaining");
        }
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Fission Reactor Silo")
            .addInfo("One controller for the whole ladder: five silo diameters (5 to 13 wide octagon).")
            .addInfo("Hologram: trigger stack size 1-5 selects the silo tier (deck is projected too).")
            .addInfo("Walls upgrade by tier: Radiation Proof -> Europium-Reinforced -> Radiant Naquadah Alloy.")
            .addInfo("Shield floor/lid: Tungstensteel Reinforced (T1-2), Iridium Tungstensteel (T3+).")
            .addInfo("Silo: shield floor, plenum, 3 fuel layers (stack columns 3 high), plenum, lid.")
            .addInfo("ADDON - Turbine Deck on the lid: 2 turbine-casing layers + shaft + dome, dynamos.")
            .addInfo("With the deck + a fan in an input bus the silo generates EU directly:")
            .addInfo("fan T (x1.25 / x1.25 TDA / x2.5 optimized), stocked coolant K (x2.5, tier-5 x5),")
            .addInfo("naquadah propellant P (Mk I/II/III = x2/x3/x10, single litres per tick).")
            .addInfo("Propellant on a non-TDA fan = BLOWOUT; cutting it mid-boost = SEIZURE.")
            .addInfo("Without the deck: exports Steam (or Hot Coolant when fed IC2 Coolant).")
            .addInfo("Fuel ladder: Uranium x1, MOX x2, Thorium-Bonded x5, Naquadah-Bonded x9.")
            .addInfo("Heat: SCRAM at 8,000; decay heat 20 min after shutdown; 10,000 = MELTDOWN")
            .addInfo("(vertical detonation + radiation field; hazmat counters; config toggles).")
            .beginStructureBlock(13, 10, 13, true)
            .addController("Silo wall, front center of the middle fuel layer")
            .addCasingInfoRange("Tier wall casing (see above)", 51, 171, false)
            .addCasingInfoExactly("Shield blocks (floor + lid, + dome with deck)", 42, false)
            .addEnergyHatch("Any wall casing", 1)
            .addMaintenanceHatch("Any wall casing", 1)
            .addInputHatch("Any wall casing", 1)
            .addOutputHatch("Any wall casing", 1)
            .addInputBus("Any wall casing", 1)
            .addOutputBus("Any wall casing", 1)
            .addDynamoHatch("Turbine Deck casing (with addon)", 2)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture() };
        }
        return rTexture;
    }

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }
}
