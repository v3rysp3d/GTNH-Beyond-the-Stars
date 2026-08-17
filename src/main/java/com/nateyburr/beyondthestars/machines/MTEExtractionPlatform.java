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
import gregtech.api.util.MultiblockTooltipBuilder;

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
        tt.addMachineType("Drilling Platform")
            .addInfo("The campus fuel front end: drinks EV-tier EU, drills forever.")
            .addInfo("Every 10s: 6x Uraninite Dust + 1x Thorium Dust; every 4th cycle +1 Tantalum Dust.")
            .addInfo("Feed the uraninite to an Enrichment Train for fuel pellets.")
            .beginStructureBlock(5, 8, 5, true)
            .addController("Front center of the machine deck")
            .addCasingInfoRange("Solid Steel Machine Casing", 60, 70, false)
            .addCasingInfoExactly("Steel Frame Box (derrick tower)", 24, false)
            .addCasingInfoExactly("Steel Pipe Casing (drill string)", 7, false)
            .addEnergyHatch("Any base casing", 1)
            .addMaintenanceHatch("Any base casing", 1)
            .addOutputBus("Any base casing", 1)
            .toolTipFinisher();
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
}
