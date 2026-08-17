package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
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
 * Reactor Core (Gen I) of the Nuclear Campus. 5x5x5: tungstensteel-reinforced bio-shield roof
 * and floor around radiation-proof casing walls, 3x3x3 interior of fuel column stacks (3
 * segments = one column, up to 9). Campaigns burn Fuel Assemblies (MOX doubles output) and run
 * either the water/steam loop or the IC2 coolant loop (hot coolant into a Large Heat Exchanger,
 * worth 2.5x). Emits Nuclear Waste Water while running. Heat model: starvation heats the core;
 * SCRAM at 8,000 disables the reactor; decay heat demands cooling for 20 minutes after any
 * shutdown; 10,000 heat is a MELTDOWN - vertical detonation plus a lingering radiation field
 * countered by hazmat (both config-toggleable). Numbers per docs/reactor-gen1.md.
 */
public class MTEReactorCoreMk1 extends MTEExtendedPowerMultiBlockBase<MTEReactorCoreMk1>
    implements ISurvivalConstructable {

    /** L/t of steam per full fuel column at 100% throttle (9 columns ~ 73,728 EU/t). */
    private static final long STEAM_PER_COLUMN = 16_384L;
    private static final int SCRAM_HEAT = 8_000;
    private static final int MELTDOWN_HEAT = 10_000;
    private static final int HEAT_PER_STARVED_TICK = 4;
    private static final int HEAT_DECAY_PER_TICK = 2;
    /** Decay-heat cooling demand after shutdown: duration and water draw. */
    private static final int DECAY_HEAT_TICKS = 24_000;
    private static final int DECAY_WATER_PER_TICK = 64;
    private static final int RADIATION_TICKS = 36_000;
    private static final int RADIATION_RADIUS = 12;
    private static final int RADIATION_LEVEL = 3;
    private static final ShutDownReason SCRAM_REASON = SimpleShutDownReason.ofCritical("beyondthestars.scram");
    private static final ShutDownReason MELTDOWN_REASON = SimpleShutDownReason.ofCritical("beyondthestars.meltdown");

    private static IStructureDefinition<MTEReactorCoreMk1> STRUCTURE_DEFINITION = null;

    private int mCasing;
    private int fuelSegments;
    private int activeColumns;
    private int throttleN = 24;
    private int activeFuelMult = 1;
    private int heat;
    private int decayHeatTicks;
    private int radiationTicks;
    private long steamPerTick;
    private boolean starvedThisTick;
    private boolean wasRunningLastTick;

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

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        fuelSegments = 0;
        if (!checkPiece(mName, 2, 2, 0, errors)) return;
        checkCasingMin(errors, mCasing, 39);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 2, 2, 0, elementBudget, env, false, true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 2, 0);
    }

    @Override
    public IStructureDefinition<MTEReactorCoreMk1> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final String[] shield = { "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB" };
            final String[] interior = { "CCCCC", "CFFFC", "CFFFC", "CFFFC", "CCCCC" };
            final String[] controller = { "CC~CC", "CFFFC", "CFFFC", "CFFFC", "CCCCC" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEReactorCoreMk1>builder()
                .addShape(mName, transpose(new String[][] { shield, interior, controller, interior, shield }))
                .addElement('B', ofBlock(GregTechAPI.sBlockReinforced, 3))
                .addElement(
                    'C',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())))
                .addElement(
                    'F',
                    ofChain(onElementPass(x -> ++x.fuelSegments, ofBlock(BTSBlocks.reactorFuelColumn, 0)), isAir()))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    private int countMatchingInputs(ItemStack sample) {
        int available = 0;
        for (ItemStack stack : getStoredInputs()) {
            if (GTUtility.areStacksEqual(sample, stack)) {
                available += stack.stackSize;
            }
        }
        return available;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        int columns = fuelSegments / 3;
        if (columns <= 0) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        int n = 24;
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(24, circuit.getItemDamage());
        }

        // Fuel selection: MOX (x2 output) is preferred over uranium when both are stocked.
        // depleteInput(stack) only consumes from a single slot holding the full amount, so count
        // across all buses first, then deplete one assembly at a time.
        final ItemStack oneMox = new ItemStack(BTSItems.fuelAssemblyMox, 1);
        final ItemStack oneUranium = new ItemStack(BTSItems.fuelAssemblyUranium, 1);
        final ItemStack chosen;
        final ItemStack depleted;
        final int fuelMult;
        if (countMatchingInputs(oneMox) >= columns) {
            chosen = oneMox;
            depleted = new ItemStack(BTSItems.depletedFuelAssemblyMox, columns);
            fuelMult = 2;
        } else if (countMatchingInputs(oneUranium) >= columns) {
            chosen = oneUranium;
            depleted = new ItemStack(BTSItems.depletedFuelAssemblyUranium, columns);
            fuelMult = 1;
        } else {
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
        // Spent fuel is delivered at campaign completion; a SCRAM forfeits it with the campaign.
        this.mOutputItems = new ItemStack[] { depleted };
        this.mMaxProgresstime = Math.max(1, Config.genICampaignMinutes) * 60 * 20;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        this.lEUt = 0;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (steamPerTick > 0 && getBaseMetaTileEntity().isServerSide()) {
            int flowPerTick = (int) ((steamPerTick + GTValues.STEAM_PER_WATER - 1) / GTValues.STEAM_PER_WATER);
            startRecipeProcessing();
            // Coolant loop first: hot coolant into a Large Heat Exchanger is worth 2.5x the
            // steam route (200 EU/L), and the LHE returns the cold coolant 1:1.
            if (depleteInput(GTModHandler.getIC2Coolant(flowPerTick))) {
                addOutput(FluidRegistry.getFluidStack("ic2hotcoolant", flowPerTick));
                emitWasteWater();
            } else if (depleteInput(Materials.Water.getFluid(flowPerTick))
                || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                    addOutput(Materials.Steam.getGas(steamPerTick));
                    emitWasteWater();
                } else {
                    starvedThisTick = true;
                    heat += HEAT_PER_STARVED_TICK;
                    if (heat >= SCRAM_HEAT) {
                        scram();
                    }
                }
            endRecipeProcessing();
        }
        return true;
    }

    private void emitWasteWater() {
        if (activeColumns > 0 && BTSFluids.nuclearWasteWater != null) {
            addOutput(new FluidStack(BTSFluids.nuclearWasteWater, activeColumns));
        }
    }

    private void clearCampaign() {
        steamPerTick = 0;
        activeColumns = 0;
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
            // Any shutdown - completion, SCRAM, or structure loss - leaves decay heat behind.
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
                    // GTProxy precedent: creative players are exempt from radioactivity ticks.
                    if (living instanceof EntityPlayer && ((EntityPlayer) living).capabilities.isCreativeMode) {
                        continue;
                    }
                    // applyRadioactivity is a no-op for entities in full radiation hazmat.
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

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsHeat", heat);
        aNBT.setInteger("btsActiveColumns", activeColumns);
        aNBT.setInteger("btsThrottleN", throttleN);
        aNBT.setInteger("btsFuelMult", activeFuelMult);
        aNBT.setInteger("btsDecayHeatTicks", decayHeatTicks);
        aNBT.setInteger("btsRadiationTicks", radiationTicks);
        aNBT.setLong("btsSteamPerTick", steamPerTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        heat = aNBT.getInteger("btsHeat");
        activeColumns = aNBT.getInteger("btsActiveColumns");
        throttleN = aNBT.getInteger("btsThrottleN");
        activeFuelMult = Math.max(1, aNBT.getInteger("btsFuelMult"));
        decayHeatTicks = aNBT.getInteger("btsDecayHeatTicks");
        radiationTicks = aNBT.getInteger("btsRadiationTicks");
        steamPerTick = aNBT.getLong("btsSteamPerTick");
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Fuel columns: " + (fuelSegments / 3) + " / 9");
        info.add("Fuel type: " + (activeFuelMult >= 2 ? "MOX (x2)" : "Uranium"));
        info.add("Control rods: " + (throttleN * 100 / 24) + "% power");
        info.add("Steam output: " + steamPerTick + " L/t");
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
        tt.addMachineType("Fission Reactor")
            .addInfo("Gen I Reactor Core of the Nuclear Campus - the water/steam stage.")
            .addInfo("Interior: stack Reactor Fuel Columns 3 high; each full stack is one fuel column.")
            .addInfo("Consumes 1 Fuel Assembly per column per campaign - MOX assemblies double the output.")
            .addInfo("Outputs 16,384 L/t of Steam per column; drinks 1 L water per 160 L steam.")
            .addInfo(
                "Coolant loop: feed IC2 Coolant to emit Hot Coolant instead - worth 2.5x via a Large Heat Exchanger.")
            .addInfo("Emits 1 L/t of Nuclear Waste Water per column while running - distill it for tantalum.")
            .addInfo("Programmed circuit in controller = control-rod throttle (N/24 power).")
            .addInfo("Ejects one Depleted Fuel Assembly per column at campaign end - reprocess in a centrifuge.")
            .addInfo("No coolant while fueled builds heat: SCRAM at 8,000 disables the reactor.")
            .addInfo("Decay heat demands cooling water for 20 minutes after every shutdown.")
            .addInfo("10,000 heat = MELTDOWN: vertical detonation, then a lingering radiation field.")
            .addInfo("Hazmat counters the radiation. Both effects have config toggles.")
            .beginStructureBlock(5, 5, 5, true)
            .addController("Front center")
            .addCasingInfoExactly("Tungstensteel Reinforced Block (roof & floor bio-shield)", 50, false)
            .addCasingInfoRange("Radiation Proof Machine Casing", 39, 47, false)
            .addEnergyHatch("Any wall casing", 1)
            .addMaintenanceHatch("Any wall casing", 1)
            .addInputHatch("Any wall casing", 1)
            .addOutputHatch("Any wall casing", 1)
            .addInputBus("Any wall casing", 1)
            .addOutputBus("Any wall casing", 1)
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
