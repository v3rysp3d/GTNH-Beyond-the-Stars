package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Dynamo;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.SimpleShutDownReason;

/**
 * Turbine Hall plant module - the gabled generator building from the Unit One mockup, docked
 * via its controller in the Reactor Silo wall. The silo pipes its steam here each tick; the
 * hall converts it to EU through its own dynamos with the fan T multiplier (fan in the hall's
 * input bus), stocked-coolant K, and naquadah propellant P (blowout on non-TDA fans, seizure
 * on starvation). No hall docked = the silo exports fluids as before.
 */
public class MTEModuleTurbineHall extends MTEPlantModuleBase implements ISurvivalConstructable {

    private static final ShutDownReason BLOWOUT_REASON = SimpleShutDownReason.ofCritical("beyondthestars.blowout");
    private static final ShutDownReason SEIZURE_REASON = SimpleShutDownReason.ofCritical("beyondthestars.seizure");

    private static IStructureDefinition<MTEPlantModuleBase> STRUCTURE_DEFINITION = null;

    private long lastEUOutput;
    private int activePropellantMult = 1;
    private boolean boostedLastTick;
    private transient boolean graceCheck;

    public MTEModuleTurbineHall(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEModuleTurbineHall(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEModuleTurbineHall(this.mName);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        moduleCasings = 0;
        if (!checkPiece(mName, 2, 3, 0, errors)) return;
        checkCasingMin(errors, moduleCasings, 80);
        checkHatchMin(errors, Dynamo, 1);
        checkHasMaintenanceHatch(errors);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 2, 3, 0, elementBudget, env, false, true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 3, 0);
    }

    @Override
    public IStructureDefinition<MTEPlantModuleBase> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final String[] roof = { "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS" };
            final String[] upper = { "SWWWS", "WAPAW", "WAPAW", "WAPAW", "WAPAW", "WAPAW", "WAPAW", "WAPAW", "SSSSS" };
            // Nothing sits below grade: the plinth is the bottom layer AT pad level, standing
            // on the foundation platform's concrete, with the controller anchored in its front.
            final String[] main = { "SSSSS", "SAPAS", "SAPAS", "SAPAS", "SAPAS", "SAPAS", "SAPAS", "SAPAS", "SSSSS" };
            final String[] plinth = { "UU~UU", "UUUUU", "UUUUU", "UUUUU", "UUUUU", "UUUUU", "UUUUU", "UUUUU", "UUUUU" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEPlantModuleBase>builder()
                .addShape(mName, transpose(new String[][] { roof, upper, main, plinth }))
                .addElement('A', isAir())
                .addElement('W', ofBlock(Blocks.glass, 0))
                .addElement('P', Casings.SteelPipeCasing.asElement())
                .addElement(
                    'S',
                    ofChain(
                        onElementPass(x -> ++x.moduleCasings, Casings.CleanStainlessSteelMachineCasing.asElement())))
                .addElement(
                    'U',
                    buildHatchAdder(MTEPlantModuleBase.class).atLeast(Dynamo, InputHatch, InputBus, Maintenance)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.moduleCasings, Casings.TungstensteelTurbineCasing.asElement())))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    /** 0 = none, 1 = matched (x1.25), 2 = TDA (x1.25, propellant-rated), 3 = optimized (x2.5). */
    public int installedFanTier() {
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanTDAOptimized, 1)) > 0) return 3;
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanTDA, 1)) > 0) return 2;
        if (countMatchingInputs(new ItemStack(BTSItems.turbineFanMatched, 1)) > 0) return 1;
        return 0;
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
                    getBaseMetaTileEntity().getYCoord() + 1.5,
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

    /**
     * Called by the docked silo each running tick. Returns the EU generated, or -1 when no fan
     * is installed (the silo then exports steam instead). Startup housekeeping (grace pass,
     * blowout, seizure) lives here with the hall's own hatches.
     */
    public long processReactorSteam(long steamPerTick, boolean highFlow, int flowPerTick) {
        if (!isOperational() || !getBaseMetaTileEntity().isAllowedToWork()) return -1;

        final int fanTier = installedFanTier();
        final ArrayList<FluidStack> stored = getStoredFluids();

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
            return 0;
        }
        if (fanTier == 0) {
            boostedLastTick = false;
            graceCheck = false;
            return -1;
        }

        final boolean canSeize = boostedLastTick && !graceCheck;

        // Stocked coolant charge in the hall's own hatches: K = 2.5, or 5 with tier-5 high-flow.
        long euThisTick = steamPerTick / 2;
        final int coolantStock = countFluid(stored, GTModHandler.getIC2Coolant(1));
        if (highFlow && coolantStock >= flowPerTick * 2) {
            euThisTick = euThisTick * 5;
        } else if (coolantStock >= flowPerTick) {
            euThisTick = euThisTick * 5 / 2;
        }

        int propellantMult = 1;
        if (foundPropellant >= 0) {
            final int litres = (int) Math.max(1, euThisTick / 5_000_000L);
            if (depleteInput(new FluidStack(propellants[foundPropellant], litres))) {
                propellantMult = propellantMults[foundPropellant];
                boostedLastTick = true;
            } else if (canSeize) {
                destroyFan(fanTier, SEIZURE_REASON);
                return 0;
            } else {
                boostedLastTick = false;
            }
        } else if (canSeize) {
            destroyFan(fanTier, SEIZURE_REASON);
            return 0;
        } else {
            boostedLastTick = false;
        }
        graceCheck = false;

        long output = fanTier == 3 ? euThisTick * 5 / 2 : euThisTick * 5 / 4;
        output *= propellantMult;
        output = Math.min(output, maxDynamoOutput());
        output = Math.min(output, Integer.MAX_VALUE);
        addEnergyOutput(output);
        lastEUOutput = output;
        activePropellantMult = propellantMult;
        return output;
    }

    public long getLastEUOutput() {
        return lastEUOutput;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("btsBoosted", boostedLastTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        boostedLastTick = aNBT.getBoolean("btsBoosted");
        graceCheck = true;
    }

    @Override
    public void stopMachine(@javax.annotation.Nonnull ShutDownReason reason) {
        boostedLastTick = false;
        super.stopMachine(reason);
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        final int fan = installedFanTier();
        info.add(
            "Fan: " + (fan == 3 ? "Optimized TDA (x2.5)"
                : fan == 2 ? "TDA (x1.25, propellant-rated)" : fan == 1 ? "Matched (x1.25)" : "NONE (input bus)"));
        info.add("Propellant boost: x" + activePropellantMult);
        info.add("Generating: " + lastEUOutput + " EU/t");
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Plant Module - Turbine Hall")
            .addInfo("Docks onto the Plant Foundation pad BESIDE the silo; converts its steam directly to EU.")
            .addInfo("Face the controller ALONG the platform so the hall runs parallel to the silo, not into it.")
            .addInfo("Fan in this hall's input bus: Matched x1.25, TDA x1.25 (propellant-rated), Optimized x2.5.")
            .addInfo("Stock IC2 Coolant in this hall's input hatch as the K charge (x2.5; tier-5 silo x5).")
            .addInfo("Naquadah Propellant Mk I/II/III: x2/x3/x10 - blowout on non-TDA fans, seizure on starvation.")
            .addInfo("Undocked, this building is inert.")
            .beginStructureBlock(5, 4, 9, true)
            .addController("Front of the plinth, standing on the marked dock position beside the silo")
            .addCasingInfoRange("Clean Stainless Steel Machine Casing (walls/roof)", 60, 75, false)
            .addCasingInfoRange("Tungstensteel Turbine Casing (plinth)", 37, 45, false)
            .addCasingInfoExactly("Steel Pipe Casing (shaft)", 14, false)
            .addDynamoHatch("Plinth, 1+", 1)
            .addMaintenanceHatch("Plinth", 1)
            .addInputHatch("Plinth (coolant + propellant)", 1)
            .addInputBus("Plinth (fan)", 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.TungstensteelTurbineCasing.getCasingTexture(),
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
                rTexture = new ITexture[] { Casings.TungstensteelTurbineCasing.getCasingTexture(),
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
            rTexture = new ITexture[] { Casings.TungstensteelTurbineCasing.getCasingTexture() };
        }
        return rTexture;
    }
}
