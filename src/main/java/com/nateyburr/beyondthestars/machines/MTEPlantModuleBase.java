package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;

/**
 * RETIRED base for the old nuclear plant modules.
 * <p>
 * The Space Elevator pattern this implemented - each plant building its own multiblock, its
 * controller DOCKED into a marker on the Nuclear Plant Foundation's platform and frozen until
 * connected - is gone. The A3 ruling absorbed the buildings into the Foundation: the Cooling Towers are
 * optional CHECKED PIECES of {@link MTEReactorCoreMk1} now, the Turbine Hall was already
 * retired by SD-4, and the platform's eight dock markers are plain floor.
 * <p>
 * The classes and their MTE ids stay REGISTERED so worlds that already contain one of these
 * controllers load without corruption - they are museum pieces: no recipe, a tooltip that says
 * so, and no effect on any plant. They still tick and still form (there is nothing left to
 * connect to), so a leftover controller reads as an intact building rather than a dead block.
 */
public abstract class MTEPlantModuleBase extends MTEExtendedPowerMultiBlockBase<MTEPlantModuleBase> {

    protected int moduleCasings;

    protected MTEPlantModuleBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTEPlantModuleBase(String aName) {
        super(aName);
    }

    /** Formed. Nothing docks any more, so "operational" is just "the shell stands". */
    public boolean isOperational() {
        return mMachine;
    }

    /**
     * Tidy C10: the scanner's second line, per building - what replaced it. The first line is
     * shared ("Retired controller").
     */
    protected abstract String retiredNote();

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(EnumChatFormatting.YELLOW + "Retired controller - it does nothing" + EnumChatFormatting.RESET);
        info.add(retiredNote());
        return info.toArray(new String[0]);
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

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    /**
     * Retired modules idle for ever. Tidy C10: the GUI's status line says so in words
     * ({@code GT5U.gui.text.recipe_result.beyondthestars.retired}) instead of GT's "no recipe".
     */
    @Override
    @javax.annotation.Nonnull
    public gregtech.api.recipe.check.CheckRecipeResult checkProcessing() {
        this.lEUt = 0;
        this.mMaxProgresstime = 0;
        return RETIRED;
    }

    private static final gregtech.api.recipe.check.CheckRecipeResult RETIRED = gregtech.api.recipe.check.SimpleCheckRecipeResult
        .ofFailure("beyondthestars.retired");
}
