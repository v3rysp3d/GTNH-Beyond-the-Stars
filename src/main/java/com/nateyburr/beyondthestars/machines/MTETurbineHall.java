package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Dynamo;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
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
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.api.util.shutdown.SimpleShutDownReason;

/**
 * Turbine Hall of the Nuclear Campus - the campus's own generator. Consumes the reactor's Steam
 * (2 L = 1 EU) or Hot Coolant (1 L = 200 EU, cold coolant returned 1:1) and pushes EU through
 * dynamo hatches, clamped to their capacity. The installed fan (controller slot) carries the T
 * multiplier: Matched x1.25, TDA-Bonded x1.25 (unlocks propellant), Optimized TDA x2.5.
 * Naquadah propellant (Mk I/II/III fluids, single litres per tick) multiplies output x2/x3/x10 -
 * on a non-TDA fan it causes a BLOWOUT (fan destroyed); cutting the propellant mid-boost SEIZES
 * the turbine (fan destroyed, possible explosion). Numbers per docs/reactor-gen1.md.
 */
public class MTETurbineHall extends MTEExtendedPowerMultiBlockBase<MTETurbineHall> implements ISurvivalConstructable {

    private static final long EU_PER_HOT_COOLANT = 200L;
    private static final int HOT_COOLANT_CAP = 1_048_576;
    private static final int STEAM_CAP = 33_554_432;
    private static final ShutDownReason BLOWOUT_REASON = SimpleShutDownReason.ofCritical("beyondthestars.blowout");
    private static final ShutDownReason SEIZURE_REASON = SimpleShutDownReason.ofCritical("beyondthestars.seizure");

    private static IStructureDefinition<MTETurbineHall> STRUCTURE_DEFINITION = null;

    private int mCasing;
    private int activePropellantMult = 1;
    private boolean boostedLastTick;
    /** One free pass after chunk load so a booting AE grid cannot false-seize the fan. */
    private transient boolean graceCheck;

    public MTETurbineHall(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTETurbineHall(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTETurbineHall(this.mName);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        if (!checkPiece(mName, 1, 1, 0, errors)) return;
        checkCasingMin(errors, mCasing, 25);
        checkHatchMin(errors, Dynamo, 1);
        checkHasMaintenanceHatch(errors);
        checkHasInputHatch(errors);
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
    public IStructureDefinition<MTETurbineHall> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final String[] top = { "CCC", "CCC", "CCC", "CCC" };
            final String[] mid = { "C~C", "CPC", "CPC", "CCC" };
            final String[] bottom = { "CCC", "CCC", "CCC", "CCC" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTETurbineHall>builder()
                .addShape(mName, transpose(new String[][] { top, mid, bottom }))
                .addElement('P', Casings.SteelPipeCasing.asElement())
                .addElement(
                    'C',
                    buildHatchAdder(MTETurbineHall.class).atLeast(InputHatch, OutputHatch, Maintenance, Dynamo)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(1)
                        .buildAndChain(onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement())))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    /** 0 = none, 1 = matched (x1.25), 2 = TDA (x1.25, propellant-rated), 3 = optimized TDA (x2.5). */
    private int fanTier(ItemStack fan) {
        if (fan == null) return 0;
        final Item item = fan.getItem();
        if (item == BTSItems.turbineFanTDAOptimized) return 3;
        if (item == BTSItems.turbineFanTDA) return 2;
        if (item == BTSItems.turbineFanMatched) return 1;
        return 0;
    }

    private long maxDynamoOutput() {
        long total = 0;
        for (MTEHatchDynamo dynamo : validMTEList(mDynamoHatches)) {
            total += (long) dynamo.maxAmperesOut() * dynamo.maxEUOutput();
        }
        return total;
    }

    private int countFluid(ArrayList<FluidStack> stored, FluidStack sample) {
        if (sample == null) return 0;
        // Long accumulator: ME stocking hatches can present Integer.MAX_VALUE per fluid, and an
        // int sum would wrap negative and read a full hatch as empty.
        long amount = 0;
        for (FluidStack stack : stored) {
            if (stack != null && stack.isFluidEqual(sample)) {
                amount += stack.amount;
            }
        }
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    /** Fan destroyed; optional bang. The hall stays down until a new fan is installed. */
    private void destroyFan(ShutDownReason reason) {
        mInventory[1] = null;
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

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        final ItemStack fan = getControllerSlot();
        final int tier = fanTier(fan);
        if (tier == 0) {
            this.lEUt = 0;
            this.mEfficiency = 0;
            boostedLastTick = false;
            stopMachine(ShutDownReasonRegistry.NO_TURBINE);
            return CheckRecipeResultRegistry.NO_TURBINE_FOUND;
        }

        final ArrayList<FluidStack> stored = getStoredFluids();

        // Propellant: Mk III > Mk II > Mk I. Requires a TDA-rated fan or the fan blows out.
        int propellantMult = 1;
        final Fluid[] propellants = { BTSFluids.naqPropellantT3, BTSFluids.naqPropellantT2, BTSFluids.naqPropellantT1 };
        final int[] propellantMults = { 10, 3, 2 };
        int foundPropellant = -1;
        for (int i = 0; i < propellants.length; i++) {
            if (propellants[i] != null && countFluid(stored, new FluidStack(propellants[i], 1)) > 0) {
                foundPropellant = i;
                break;
            }
        }
        if (foundPropellant >= 0 && tier < 2) {
            destroyFan(BLOWOUT_REASON);
            return CheckRecipeResultRegistry.NO_TURBINE_FOUND;
        }

        // Fuel intake: hot coolant is worth 200 EU/L (cold coolant returned 1:1), steam 2 L = 1 EU.
        long euBase = 0;
        final FluidStack hotSample = GTModHandler.getHotCoolant(1);
        final int hotAvailable = Math.min(countFluid(stored, hotSample), HOT_COOLANT_CAP);
        if (hotAvailable > 0 && depleteInput(GTModHandler.getHotCoolant(hotAvailable))) {
            addOutput(GTModHandler.getIC2Coolant(hotAvailable));
            euBase += hotAvailable * EU_PER_HOT_COOLANT;
        }
        final FluidStack steamSample = gregtech.api.enums.Materials.Steam.getGas(1);
        final int steamAvailable = Math.min(countFluid(stored, steamSample), STEAM_CAP);
        if (steamAvailable > 1 && depleteInput(gregtech.api.enums.Materials.Steam.getGas(steamAvailable))) {
            euBase += steamAvailable / 2;
        }

        // Seizure only bites if the boost was genuinely cut mid-run: never on the post-load
        // grace pass, and stopMachine (structure break, disable) clears the flag separately.
        final boolean canSeize = boostedLastTick && !graceCheck;

        if (euBase <= 0) {
            if (canSeize) {
                // Spinning on propellant with no working fluid seizes the turbine too.
                destroyFan(SEIZURE_REASON);
                return CheckRecipeResultRegistry.NO_TURBINE_FOUND;
            }
            graceCheck = false;
            boostedLastTick = false;
            this.lEUt = 0;
            this.mEfficiency = 0;
            return CheckRecipeResultRegistry.NO_FUEL_FOUND;
        }

        // Burn propellant: single litres per tick, scaling gently with plant size.
        if (foundPropellant >= 0) {
            final int litres = (int) Math.max(1, euBase / 5_000_000L);
            if (depleteInput(new FluidStack(propellants[foundPropellant], litres))) {
                propellantMult = propellantMults[foundPropellant];
                boostedLastTick = true;
            } else if (canSeize) {
                destroyFan(SEIZURE_REASON);
                return CheckRecipeResultRegistry.NO_TURBINE_FOUND;
            } else {
                boostedLastTick = false;
            }
        } else if (canSeize) {
            destroyFan(SEIZURE_REASON);
            return CheckRecipeResultRegistry.NO_TURBINE_FOUND;
        } else {
            boostedLastTick = false;
        }
        graceCheck = false;

        // T multiplier: matched/TDA x1.25, optimized x2.5.
        long output = tier == 3 ? euBase * 5 / 2 : euBase * 5 / 4;
        output *= propellantMult;
        // Clamp to dynamo capacity - GT explodes the machine on overflow, we throttle instead.
        // Also cap below 2^31: GT's multi-dynamo injection accumulates in an int and would
        // duplicate EU past that.
        output = Math.min(output, maxDynamoOutput());
        output = Math.min(output, Integer.MAX_VALUE);

        activePropellantMult = propellantMult;
        this.lEUt = output;
        this.mMaxProgresstime = 1;
        this.mEfficiencyIncrease = 10;
        return CheckRecipeResultRegistry.GENERATING;
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        // Any external stop (structure break, maintenance, disable) ends the boost obligation;
        // destroyFan clears the flag itself before calling this, so punishments are unaffected.
        boostedLastTick = false;
        super.stopMachine(reason);
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
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        final int tier = fanTier(getControllerSlot());
        info.add(
            "Installed fan: " + (tier == 0 ? "none"
                : tier == 3 ? "Optimized TDA (x2.5)"
                    : tier == 2 ? "TDA-Bonded (x1.25, propellant-rated)" : "Matched (x1.25)"));
        info.add("Propellant boost: x" + activePropellantMult);
        info.add("Generating: " + lEUt + " EU/t (dynamo-clamped)");
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Steam & Coolant Turbine")
            .addInfo("The Nuclear Campus generator: burns the Reactor Core's output into EU.")
            .addInfo("Steam: 2 L = 1 EU. Hot Coolant: 1 L = 200 EU, cold coolant returned 1:1.")
            .addInfo("Install a turbine fan in the controller: Matched x1.25, TDA x1.25, Optimized TDA x2.5.")
            .addInfo("Naquadah Propellant (Mk I/II/III) boosts x2/x3/x10 - burned in single litres per tick.")
            .addInfo("Propellant demands a TDA-rated fan: lesser fans BLOW OUT (fan destroyed).")
            .addInfo("Cutting propellant or working fluid mid-boost SEIZES the turbine (fan destroyed).")
            .addInfo("Output is clamped to total dynamo capacity - bring enough amps.")
            .beginStructureBlock(3, 3, 4, true)
            .addController("Front center")
            .addCasingInfoRange("Tungstensteel Turbine Casing", 25, 33, false)
            .addCasingInfoExactly("Steel Pipe Casing (shaft)", 2, false)
            .addDynamoHatch("Any casing, 1+", 1)
            .addMaintenanceHatch("Any casing", 1)
            .addInputHatch("Any casing", 1)
            .addOutputHatch("Any casing (coolant return)", 1)
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

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }
}
