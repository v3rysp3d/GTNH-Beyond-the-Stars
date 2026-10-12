package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

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
import com.nateyburr.beyondthestars.BeyondTheStars;

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
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.tooltip.TooltipHelper;

/**
 * Extraction Platform - the campus fuel front end. A drilling derrick that grinds EU into a
 * slow but INDEFINITE trickle of uranium-bearing feedstock (uraninite + thorium, with a
 * tantalum trace every few cycles). The "infinite resource with big infrastructure" fantasy,
 * made lifelike; feeds the Enrichment Train. Numbers per docs/reactor-gen1.md.
 */
public class MTEExtractionPlatform extends MTEExtendedPowerMultiBlockBase<MTEExtractionPlatform>
    implements ISurvivalConstructable {

    private static final int CYCLE_TICKS = 200;

    private static IStructureDefinition<MTEExtractionPlatform> STRUCTURE_DEFINITION = null;

    private int mCasing;
    private int cycleCounter;

    public MTEExtractionPlatform(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEExtractionPlatform(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEExtractionPlatform(this.mName);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        if (!checkPiece(mName, 2, 6, 0, errors)) return;
        checkCasingMin(errors, mCasing, 60);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 2, 6, 0, elementBudget, env, false, true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 6, 0);
    }

    @Override
    public IStructureDefinition<MTEExtractionPlatform> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final String[] crown = { "     ", " GGG ", " GPG ", " GGG ", "     " };
            final String[] tower = { "G   G", "     ", "  P  ", "     ", "G   G" };
            final String[] deck = { "SSSSS", "SSSSS", "SSPSS", "SSSSS", "SSSSS" };
            final String[] base = { "SS~SS", "SSSSS", "SSPSS", "SSSSS", "SSSSS" };
            final String[] foundation = { "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEExtractionPlatform>builder()
                .addShape(
                    mName,
                    transpose(new String[][] { crown, tower, tower, tower, tower, deck, base, foundation }))
                .addElement('G', ofFrame(Materials.Steel))
                .addElement('P', Casings.SteelPipeCasing.asElement())
                .addElement(
                    'S',
                    buildHatchAdder(MTEExtractionPlatform.class).atLeast(Energy, OutputBus, Maintenance)
                        .casingIndex(Casings.SolidSteelMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(onElementPass(x -> ++x.mCasing, Casings.SolidSteelMachineCasing.asElement())))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        cycleCounter++;
        final boolean bonusCycle = cycleCounter % 4 == 0;
        this.mOutputItems = bonusCycle
            ? new ItemStack[] { GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 6L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 1L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Tantalum, 1L) }
            : new ItemStack[] { GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 6L),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, 1L) };
        this.mMaxProgresstime = CYCLE_TICKS;
        this.lEUt = -TierEU.RECIPE_EV;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsCycleCounter", cycleCounter);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        cycleCounter = aNBT.getInteger("btsCycleCounter");
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Extraction cycles completed: " + cycleCounter);
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Ore Drill")
            .addInfo("A uranium mine that never runs dry: needs only power.")
            .addInfo("Every 10 s: 6 Uraninite Dust + 1 Thorium Dust,")
            .addInfo("plus 1 Tantalum Dust every 4th cycle.")
            .addInfo(
                "Uses " + TooltipHelper.euRateText(TierEU.RECIPE_EV)
                    + " ("
                    + TooltipHelper.voltageTierText(VoltageIndex.EV, false)
                    + ")")
            .addInfo("Feed the Uraninite to a Yellowcake Mill to make Yellowcake.")
            .addInfo("Output Bus required: output is lost without one.")
            .addInfo("The Nuclear Plant Foundation has this drill built in as its Extraction Derrick.")
            .beginStructureBlock(5, 8, 5, false)
            .addController("Front face, centre, 2nd layer from the bottom")
            .addCasing("60-70", "Solid Steel Machine Casing", false)
            .addCasing("24", "Steel Frame Box (derrick tower)", false)
            .addCasing("7", "Steel Pipe Casing (drill string)", false)
            .addEnergyHatch("1+", "Any Solid Steel Machine Casing", 1)
            .addMaintenanceHatch("1", "Any Solid Steel Machine Casing", 1)
            .addOutputBus("1+", "Any Solid Steel Machine Casing", 1)
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.SolidSteelMachineCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE)
                    .extFacing()
                    .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] { Casings.SolidSteelMachineCasing.getCasingTexture(), TextureFactory.builder()
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
            rTexture = new ITexture[] { Casings.SolidSteelMachineCasing.getCasingTexture() };
        }
        return rTexture;
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
