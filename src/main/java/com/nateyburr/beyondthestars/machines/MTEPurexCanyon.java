package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_CUTTING_CHAMBER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_CUTTING_CHAMBER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_CUTTING_CHAMBER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_CUTTING_CHAMBER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * D-26 THE PUREX CANYON - MTE 28111, IV (docs/production-lines.md S5.3).
 * <p>
 * <b>Why nothing already in the pack does this.</b> The Chemical Plant and the Digester can each
 * run one dissolution recipe. Neither can express <b>"the output slate depends on how long this
 * fuel cooled"</b> - which is the one thing a spent-fuel head-end genuinely is, and the mechanic
 * that makes reprocessing interesting. R-02 takes an assembly that has sat in a Decay Warehouse
 * and yields plutonium; R-02h takes one straight out of the reactor and does not. Nothing else in
 * the pack has an input whose HISTORY changes the recipe.
 * <p>
 * <b>It closes D-09.</b> R-03's raffinate is {@code BTSFluids.nuclearWasteWater}. That fluid has
 * had a consumer since 0.19.0 - the Distillation Tower recipe that turns it into distilled water
 * and a tantalum trace, the D-09 profit anchor - and until this machine existed it had <b>no
 * producer at all</b>.
 * <p>
 * <b>Its parallel is flat, on purpose.</b> Eight, regardless of how it is built. The Canyon earns
 * its MTE slot on the cooled-versus-uncooled mechanic, not on throughput; giving it a second
 * structure lever for parallel would just be the Cascade Hall again in a different shell, and the
 * backlog it has to clear when a plant first upgrades is a one-off, not a standing rate.
 */
public class MTEPurexCanyon extends MTEProductionHallBase<MTEPurexCanyon> {

    /** R-02, R-02h, R-03 and their rod-scale variants. A real map. */
    public static final RecipeMap<RecipeMapBackend> RECIPE_MAP = RecipeMapBuilder.of("beyondthestars.recipe.purex")
        .maxIO(3, 4, 2, 3)
        .minInputs(0, 0)
        .build();

    /** See the class comment: flat by design, because throughput is not this machine's point. */
    private static final int PARALLEL = 8;

    private static IStructureDefinition<MTEPurexCanyon> STRUCTURE_DEFINITION = null;

    public MTEPurexCanyon(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEPurexCanyon(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEPurexCanyon(this.mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RECIPE_MAP;
    }

    @Override
    protected Casings faceCasing() {
        return Casings.RadiationProofMachineCasing;
    }

    @Override
    protected ITexture[] frontOverlay(boolean active) {
        return active ? overlayPair(OVERLAY_FRONT_CUTTING_CHAMBER_ACTIVE, OVERLAY_FRONT_CUTTING_CHAMBER_ACTIVE_GLOW)
            : overlayPair(OVERLAY_FRONT_CUTTING_CHAMBER, OVERLAY_FRONT_CUTTING_CHAMBER_GLOW);
    }

    @Override
    public int getMaxParallelRecipes() {
        return PARALLEL;
    }

    /** Tidy B8, GUI line: the PUREX parallel is not set by the structure. */
    @Override
    public String parallelSource(int detail) {
        return "fixed";
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        if (!checkPiece(mName, 4, 3, 0, errors)) return;
        checkCasingMin(errors, mCasing, 220);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputBus(errors);
        checkHasOutputHatch(errors);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 4, 3, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 4, 3, 0, elementBudget, env, false, true);
    }

    @Override
    public IStructureDefinition<MTEPurexCanyon> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            // Read [y from top][z from front][x]. 9 wide, 7 tall, 9 deep: a shielded canyon of
            // Radiation Proof Machine Casing standing on a Light Concrete cell floor. Both are
            // blocks the plant already needs in bulk, so the material lines of S3 serve this
            // building too.
            final String[] shell = { "RRRRRRRRR", "RRRRRRRRR", "RRRRRRRRR", "RRRRRRRRR", "RRRRRRRRR", "RRRRRRRRR",
                "RRRRRRRRR", "RRRRRRRRR", "RRRRRRRRR" };
            final String[] hatchTop = { "RmReRRReR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR",
                "RAAAAAAAR", "RAAAAAAAR", "RRRRRRRRR" };
            final String[] plain = { "RRRRRRRRR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR",
                "RAAAAAAAR", "RAAAAAAAR", "RRRRRRRRR" };
            final String[] work = { "RbRi~iRoR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR",
                "RAAAAAAAR", "RAAAAAAAR", "RRRRRRRRR" };
            final String[] hatchLow = { "RuRRRRRuR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR", "RAAAAAAAR",
                "RAAAAAAAR", "RAAAAAAAR", "RRRRRRRRR" };
            final String[] floor = { "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO",
                "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEPurexCanyon>builder()
                .addShape(mName, transpose(new String[][] { shell, hatchTop, plain, work, plain, hatchLow, floor }))
                .addElement(
                    'R',
                    noOverwrite(onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())))
                // Light Concrete: GregTechAPI.sBlockConcretes meta 8, the same block the silo pad
                // and the Spiral's helix arcs stand on.
                .addElement('O', noOverwrite(ofBlock(GregTechAPI.sBlockConcretes, 8)))
                .addElement('A', noOverwrite(isAir()))
                .addElement('m', socket(Maintenance, 1))
                .addElement('e', socket(Energy, 2))
                .addElement('b', socket(InputBus, 3))
                .addElement('i', socket(InputHatch, 4))
                .addElement('o', socket(OutputBus, 5))
                .addElement('u', socket(OutputHatch, 6))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    /** TC-1: one dedicated cell per hatch, chained to the plain shielded wall behind it. */
    private static IStructureElement<MTEPurexCanyon> socket(HatchElement element, int hint) {
        return buildHatchAdder(MTEPurexCanyon.class).atLeast(element)
            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
            .hint(hint)
            .buildAndChain(
                noOverwrite(onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())));
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String gray = EnumChatFormatting.GRAY.toString();
        tt.addMachineType("Fuel Reprocessor")
            .addInfo("Chops, dissolves and partitions spent fuel: the head-end of PUREX.")
            .addInfo(aqua + "Cooling time matters:" + gray + " how long the fuel cooled changes what")
            .addInfo("comes out. Fuel cooled in a Decay Warehouse yields plutonium (for MOX);")
            .addInfo("fuel straight from the reactor yields uranium and waste only.")
            .addInfo("Circuit 1 = cooled fuel, 2 = uncooled fuel, 3 = partition the liquor.")
            .addInfo("Its Nuclear Waste Water goes to a Distillation Tower: distilled water")
            .addInfo("and a tantalum trace.")
            .addStaticParallelInfo(PARALLEL)
            .beginStructureBlock(9, 7, 9, true)
            .addController("Front face, centre")
            .addCasing("231-234", "Radiation Proof Machine Casing", false)
            .addCasing("81", "Light Concrete (floor)", false)
            .addAir("The whole interior: keep pipes and cables outside")
            .addEnergyHatch("1-2", "Front face, 2nd row from the top", 2)
            .addMaintenanceHatch("1", "Front face, 2nd row from the top, left", 1)
            .addInputBus("1", "Front face, controller row, left", 3)
            .addInputHatch("1-2", "Front face, either side of the controller", 4)
            .addOutputBus("1", "Front face, controller row, right", 5)
            .addOutputHatch("1-2", "Front face, 2nd row from the bottom", 6)
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }
}
