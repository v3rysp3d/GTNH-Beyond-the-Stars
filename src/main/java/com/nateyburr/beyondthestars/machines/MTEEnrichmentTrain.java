package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.items.BTSItems;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * Enrichment Train - the campus processing factory. A long conversion line that leaches
 * uraninite feedstock with hydrofluoric acid into fuel pellets at better yield than the bench
 * assembler route (20 pellets per 8 dust, plus recovered thorium tails). Numbers per
 * docs/reactor-gen1.md.
 */
public class MTEEnrichmentTrain extends MTEExtendedPowerMultiBlockBase<MTEEnrichmentTrain>
    implements ISurvivalConstructable {

    private static final int INPUT_URANINITE = 8;
    private static final int ACID_MB = 2_000;
    private static final int OUTPUT_PELLETS = 20;
    private static final int OUTPUT_THORIUM = 2;

    private static IStructureDefinition<MTEEnrichmentTrain> STRUCTURE_DEFINITION = null;

    private int mCasing;

    public MTEEnrichmentTrain(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEEnrichmentTrain(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEEnrichmentTrain(this.mName);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        if (!checkPiece(mName, 1, 1, 0, errors)) return;
        checkCasingMin(errors, mCasing, 65);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasAnyInput(errors);
        checkHasAnyOutput(errors);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 1, 1, 0, elementBudget, env, false, true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 1, 1, 0);
    }

    @Override
    public IStructureDefinition<MTEEnrichmentTrain> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final String[] top = { "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS" };
            final String[] mid = { "S~S", "SPS", "SPS", "SPS", "SPS", "SPS", "SPS", "SPS", "SSS" };
            final String[] bottom = { "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS", "SSS" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEEnrichmentTrain>builder()
                .addShape(mName, transpose(new String[][] { top, mid, bottom }))
                .addElement('P', Casings.SteelPipeCasing.asElement())
                .addElement(
                    'S',
                    buildHatchAdder(MTEEnrichmentTrain.class)
                        .atLeast(InputBus, InputHatch, OutputBus, Energy, Maintenance)
                        .casingIndex(Casings.CleanStainlessSteelMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.mCasing, Casings.CleanStainlessSteelMachineCasing.asElement())))
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
        final ItemStack oneUraninite = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 1L);
        if (countMatchingInputs(oneUraninite) < INPUT_URANINITE) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        if (!depleteInput(Materials.HydrofluoricAcid.getFluid(ACID_MB))) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        for (int i = 0; i < INPUT_URANINITE; i++) {
            depleteInput(oneUraninite);
        }
        updateSlots();

        this.mOutputItems = new ItemStack[] { new ItemStack(BTSItems.uraniumFuelPellet, OUTPUT_PELLETS),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, OUTPUT_THORIUM) };
        this.mMaxProgresstime = 600;
        this.lEUt = -TierEU.RECIPE_HV;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Enrichment Line")
            .addInfo("The campus processing factory: leaches uraninite into fuel pellets.")
            .addInfo("Per 30s cycle: 8x Uraninite Dust + 2,000 L Hydrofluoric Acid ->")
            .addInfo("20x Fuel Pellet (Uranium) + 2x Thorium Dust (better yield than the assembler).")
            .beginStructureBlock(3, 3, 9, true)
            .addController("Front center")
            .addCasingInfoRange("Clean Stainless Steel Machine Casing", 65, 72, false)
            .addCasingInfoExactly("Steel Pipe Casing (conversion line)", 7, false)
            .addEnergyHatch("Any casing", 1)
            .addMaintenanceHatch("Any casing", 1)
            .addInputBus("Any casing", 1)
            .addInputHatch("Any casing", 1)
            .addOutputBus("Any casing", 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.CleanStainlessSteelMachineCasing.getCasingTexture(),
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
                rTexture = new ITexture[] { Casings.CleanStainlessSteelMachineCasing.getCasingTexture(),
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
            rTexture = new ITexture[] { Casings.CleanStainlessSteelMachineCasing.getCasingTexture() };
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
