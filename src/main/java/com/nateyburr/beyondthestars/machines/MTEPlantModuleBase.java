package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

/**
 * Base for Unit One plant modules - the Space Elevator pattern: each plant building is its own
 * multiblock machine whose CONTROLLER docks into the Reactor Silo's wall (the silo's hatch
 * adder accepts it like a hatch). A module only ticks while the silo connects it; undocked
 * modules are frozen, exactly like elevator modules. The silo is the connection authority.
 */
public abstract class MTEPlantModuleBase extends MTEExtendedPowerMultiBlockBase<MTEPlantModuleBase> {

    protected boolean isConnected;
    protected MTEReactorCoreMk1 parent;
    protected int moduleCasings;

    protected MTEPlantModuleBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTEPlantModuleBase(String aName) {
        super(aName);
    }

    public void connect(MTEReactorCoreMk1 silo) {
        this.parent = silo;
        this.isConnected = true;
    }

    public void disconnect() {
        this.parent = null;
        this.isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public MTEReactorCoreMk1 getParent() {
        return parent;
    }

    /** Docked and structurally formed - the silo only takes effects from live modules. */
    public boolean isOperational() {
        return isConnected && mMachine;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        // Elevator-module contract: an undocked module is frozen server-side.
        if (aBaseMetaTileEntity.isServerSide() && !isConnected) {
            return;
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(
            isConnected ? "Docked to Reactor Silo" + (mMachine ? " (operational)" : " (structure incomplete)")
                : "NOT DOCKED - place this controller in a Reactor Silo wall");
        return info.toArray(new String[0]);
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    /** Modules idle by default; productive modules override. */
    @Override
    @javax.annotation.Nonnull
    public gregtech.api.recipe.check.CheckRecipeResult checkProcessing() {
        this.lEUt = 0;
        this.mMaxProgresstime = 0;
        return gregtech.api.recipe.check.CheckRecipeResultRegistry.NO_RECIPE;
    }
}
