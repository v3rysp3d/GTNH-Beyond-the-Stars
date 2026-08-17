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

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.items.BTSItems;

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

/**
 * Reactor Core (Gen I) of the Nuclear Campus. 5x5x5 radiation-proof shell around a 3x3x3
 * interior of fuel column stacks (3 stacked segments = one full column, up to 9 columns).
 * Consumes one Fuel Assembly per column per campaign, drinks water, emits Steam. Throttled by a
 * programmed circuit (control-rod insertion). Water starvation builds heat; at the heat cap the
 * reactor SCRAMs: the campaign is lost and the machine disables itself until re-enabled.
 * Numbers per docs/reactor-gen1.md; explosion/radiation events (D-02) are a later build.
 */
public class MTEReactorCoreMk1 extends MTEExtendedPowerMultiBlockBase<MTEReactorCoreMk1>
    implements ISurvivalConstructable {

    /** L/t of steam per full fuel column at 100% throttle (9 columns ~ 73,728 EU/t). */
    private static final long STEAM_PER_COLUMN = 16_384L;
    private static final int HEAT_SCRAM = 10_000;
    private static final int HEAT_PER_STARVED_TICK = 4;
    private static final int HEAT_DECAY_PER_TICK = 2;

    private static IStructureDefinition<MTEReactorCoreMk1> STRUCTURE_DEFINITION = null;

    private int mCasing;
    private int fuelSegments;
    private int activeColumns;
    private int throttleN = 24;
    private int heat;
    private long steamPerTick;
    private boolean starvedThisTick;

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
        checkCasingMin(errors, mCasing, 85);
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
            final String[] solid = { "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC" };
            final String[] interior = { "CCCCC", "CFFFC", "CFFFC", "CFFFC", "CCCCC" };
            final String[] controller = { "CC~CC", "CFFFC", "CFFFC", "CFFFC", "CCCCC" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEReactorCoreMk1>builder()
                .addShape(mName, transpose(new String[][] { solid, interior, controller, interior, solid }))
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

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        int columns = fuelSegments / 3;
        if (columns <= 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        int n = 24;
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(24, circuit.getItemDamage());
        }

        if (!depleteInput(new ItemStack(BTSItems.fuelAssemblyUranium, columns))) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        updateSlots();

        activeColumns = columns;
        throttleN = n;
        steamPerTick = STEAM_PER_COLUMN * columns * n / 24;
        this.mMaxProgresstime = Math.max(1, Config.genICampaignMinutes) * 60 * 20;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        this.lEUt = 0;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (steamPerTick > 0 && getBaseMetaTileEntity().isServerSide()) {
            int waterPerTick = (int) ((steamPerTick + GTValues.STEAM_PER_WATER - 1) / GTValues.STEAM_PER_WATER);
            startRecipeProcessing();
            boolean watered = depleteInput(Materials.Water.getFluid(waterPerTick))
                || depleteInput(GTModHandler.getDistilledWater(waterPerTick));
            if (watered) {
                addOutput(Materials.Steam.getGas(steamPerTick));
            } else {
                starvedThisTick = true;
                heat += HEAT_PER_STARVED_TICK;
                if (heat >= HEAT_SCRAM) {
                    scram();
                }
            }
            endRecipeProcessing();
        }
        return true;
    }

    /** Campaign lost, machine disabled until the operator re-enables it. Heat then decays. */
    private void scram() {
        this.mMaxProgresstime = 0;
        this.mProgresstime = 0;
        this.steamPerTick = 0;
        this.activeColumns = 0;
        getBaseMetaTileEntity().disableWorking();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            if (!starvedThisTick && heat > 0) {
                heat = Math.max(0, heat - HEAT_DECAY_PER_TICK);
            }
            starvedThisTick = false;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsHeat", heat);
        aNBT.setInteger("btsActiveColumns", activeColumns);
        aNBT.setInteger("btsThrottleN", throttleN);
        aNBT.setLong("btsSteamPerTick", steamPerTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        heat = aNBT.getInteger("btsHeat");
        activeColumns = aNBT.getInteger("btsActiveColumns");
        throttleN = aNBT.getInteger("btsThrottleN");
        steamPerTick = aNBT.getLong("btsSteamPerTick");
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Fuel columns: " + (fuelSegments / 3) + " / 9");
        info.add("Control rods: " + (throttleN * 100 / 24) + "% power");
        info.add("Steam output: " + steamPerTick + " L/t");
        info.add("Core heat: " + heat + " / " + HEAT_SCRAM);
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Fission Reactor")
            .addInfo("Gen I Reactor Core of the Nuclear Campus - the water/steam stage.")
            .addInfo("Interior: stack Reactor Fuel Columns 3 high; each full stack is one fuel column.")
            .addInfo("Consumes 1 Fuel Assembly (Uranium) per column per campaign.")
            .addInfo("Outputs 16,384 L/t of Steam per column; drinks 1 L water per 160 L steam.")
            .addInfo("Programmed circuit in controller = control-rod throttle (N/24 power).")
            .addInfo("No water while fueled builds heat - at 10,000 the reactor SCRAMs:")
            .addInfo("the campaign is lost and the reactor stays down until re-enabled.")
            .beginStructureBlock(5, 5, 5, true)
            .addController("Front center")
            .addCasingInfoRange("Radiation Proof Machine Casing", 85, 97, false)
            .addEnergyHatch("Any Casing", 1)
            .addMaintenanceHatch("Any Casing", 1)
            .addInputHatch("Any Casing", 1)
            .addOutputHatch("Any Casing", 1)
            .addInputBus("Any Casing", 1)
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
