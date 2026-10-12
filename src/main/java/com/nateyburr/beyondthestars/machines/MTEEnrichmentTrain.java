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
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.items.BTSItemsFuel;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;
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
import gregtech.api.util.tooltip.TooltipHelper;
import gregtech.common.tileentities.machines.IDualInputHatch;

/**
 * Enrichment Train - the campus processing factory. A long conversion line that leaches uraninite
 * feedstock with hydrofluoric acid into YELLOWCAKE, plus the recovered thorium tails. Numbers per
 * docs/production-lines.md S8.3.
 * <p>
 * <b>D-26 M7: it makes yellowcake, and it used to make finished fuel pellets.</b> Paying
 * {@code uraniumFuelPellet} straight out of ore was a 10x bypass of the 23-step fuel line - 1.6
 * Uraninite per rod against the line's 16.0 at Grade 1 - and it skipped F-01 through F-11
 * entirely, F-12's water YIELD step included. That step is the ruling's PRIMARY effect, so a
 * route that never touches it makes the whole water ladder optional decoration.
 * <p>
 * The ratio is the front end's own, exactly: F-01 .. F-08 turn 16 Uraninite into 4 Yellowcake, so
 * 8 Uraninite buys 2 here. What the Train sells is therefore not yield but SIMPLICITY - one
 * multiblock and a hydrofluoric acid line in place of a mill, a flotation cell, a dehydrator, a
 * digester, two chemical plants, a distillation tower and their five reagent sub-recipes. It is a
 * bootstrap past the FRONT END; F-09 .. F-18 stay compulsory.
 * <p>
 * <b>What an existing world sees:</b> any Output Bus, AE2 storage bus or pipe reading this
 * machine now receives Yellowcake instead of Fuel Pellets (S8.4). Nothing already in a chest
 * changes.
 */
public class MTEEnrichmentTrain extends MTEExtendedPowerMultiBlockBase<MTEEnrichmentTrain>
    implements ISurvivalConstructable {

    private static final int INPUT_URANINITE = 8;
    private static final int ACID_MB = 2_000;
    /** 8 Uraninite -> 2 Yellowcake: 4 Uraninite per cake, the same ratio F-01 .. F-08 achieve. */
    private static final int OUTPUT_YELLOWCAKE = 2;
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

    /**
     * The Train draws by hand ({@link #checkProcessing()}: {@code depleteInput}), and GT's helpers
     * walk only {@code mInputHatches} / {@code mInputBusses}, never {@code mDualInputHatches}. A
     * crafting input buffer (or its slave) in a casing would form the structure and feed nothing,
     * so the Train refuses dual input hatches outright: the bus adder via this flag, the hatch
     * adder (GT 2.9.0-RC-2 also takes fluid CRIBs there, regardless of this flag) below.
     */
    @Override
    protected boolean supportsCraftingMEBuffer() {
        return false;
    }

    @Override
    public boolean addInputHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity != null && aTileEntity.getMetaTileEntity() instanceof IDualInputHatch) return false;
        return super.addInputHatchToMachineList(aTileEntity, aBaseCasingIndex);
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

        // M7: YELLOWCAKE, not finished pellets. This machine feeds the fuel line at F-09; it does
        // not replace it. See the class javadoc for why that distinction is the whole design.
        this.mOutputItems = new ItemStack[] { new ItemStack(BTSItemsFuel.yellowcake, OUTPUT_YELLOWCAKE),
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
        tt.addMachineType("Yellowcake Mill")
            .addInfo("Leaches Uraninite Dust with Hydrofluoric Acid into Yellowcake.")
            .addInfo(String.format("Every 30 s: %d Uraninite Dust + %,d L Hydrofluoric Acid", INPUT_URANINITE, ACID_MB))
            .addInfo(String.format("-> %d Yellowcake + %d Thorium Dust.", OUTPUT_YELLOWCAKE, OUTPUT_THORIUM))
            .addInfo(
                "Uses " + TooltipHelper.euRateText(TierEU.RECIPE_HV)
                    + " ("
                    + TooltipHelper.voltageTierText(VoltageIndex.HV, false)
                    + ")")
            .addInfo("Same 4 Uraninite per Yellowcake as the full ore line:")
            .addInfo("it saves machines, not ore.")
            .addInfo("Next step: Chemical Plant, Yellowcake -> Uranium Tetrafluoride.")
            .addInfo("Despite the name it does not enrich; enrichment is the Cascade Hall.")
            .addInfo("The Nuclear Plant Foundation has this built in as its Enrichment Annex.")
            .beginStructureBlock(3, 3, 9, false)
            .addController("Front face, centre")
            .addCasing("65-69", "Clean Stainless Steel Machine Casing", false)
            .addCasing("7", "Steel Pipe Casing (conversion line)", false)
            .addEnergyHatch("1+", "Any Clean Stainless Steel Machine Casing", 1)
            .addMaintenanceHatch("1", "Any Clean Stainless Steel Machine Casing", 1)
            .addInputBus("1+", "Any Clean Stainless Steel Machine Casing (no Crafting Input Buffer)", 1)
            .addInputHatch("1+", "Any Clean Stainless Steel Machine Casing (no Crafting Input Buffer)", 1)
            .addOutputBus("1+", "Any Clean Stainless Steel Machine Casing", 1)
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
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

    /**
     * Tidy B7: no parallel to limit, so GT's Power Panel would hold only its power-fail toggle. GT
     * hides it on its own single-parallel multis (Heat Exchanger, Air Filter, Integrated Ore Factory)
     * the same way. GUI only.
     */
    @Override
    public boolean supportsPowerPanel() {
        return false;
    }
}
