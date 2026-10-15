package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;

/**
 * D-26 THE CASCADE HALL - MTE 28113, IV (docs/production-lines.md S5.1).
 * <p>
 * NOT 28101: tag 0.5.0 shipped MTETurbineHall on base+1, so that id is BURNED, not free -
 * a world saved then binds a placed Turbine Hall to it and would resolve it to this class.
 * <p>
 * <b>Why nothing already in the pack does this.</b> GT's Industrial Centrifuge and GT++'s Reactor
 * Processing Unit are fixed-ratio splitters: one input set, one output set, the ratio baked into
 * the recipe. Isotope separation is a CASCADE, and its defining property is that it makes TWO
 * streams whose split is set by how much plant you built - with the tails as a real product
 * rather than a discard. F-11's tails go on to be MOX diluent (F-20) and the depleted-uranium
 * shielding the casing line spends (F-11c, M-03b). Nothing in the pack expresses "the structure
 * decides the split", which is precisely what this machine is for.
 * <p>
 * <b>Stage banks set PARALLEL, not assay.</b> {@code parallel = 2^banks}, so the five legal halls
 * run 1, 2, 4, 8 and 16 recipes at once. The ASSAY is chosen by the recipe's programmed circuit -
 * 1 for the low-enriched cut, 2 for the highly-enriched one - because a structure that changed
 * what a recipe PRODUCES would make one machine's NEI page unreadable, while a structure that
 * changes how MUCH is exactly the "build more cascade" fantasy this step should sell.
 * <p>
 * <b>Not to be confused with the Enrichment Train</b> ({@link MTEEnrichmentTrain}, MTE 28103), the
 * standalone Yellowcake mill that the Nuclear Plant Foundation also carries built in as its
 * Enrichment Annex. Despite the name it does no enrichment; this hall is where enrichment happens.
 */
public class MTECascadeHall extends MTEProductionHallBase<MTECascadeHall> {

    /**
     * F-11 and F-11b. A real map, not one of {@code BTSDisplayRecipes}' fake pages: this machine
     * genuinely processes out of it.
     */
    public static final RecipeMap<RecipeMapBackend> RECIPE_MAP = RecipeMapBuilder.of("beyondthestars.recipe.cascade")
        .maxIO(2, 1, 2, 3)
        .minInputs(0, 1)
        .build();

    private static final String PIECE_FRONT = "cascadeFront";
    private static final String PIECE_BANK = "cascadeBank";
    private static final String PIECE_BACK = "cascadeBack";

    /** N = 8 + 4k for k = 0..4, so the longest legal hall is 24 deep and runs 16 parallel. */
    public static final int MAX_BANKS = 4;
    private static final int PIECE_DEPTH = 4;

    private static IStructureDefinition<MTECascadeHall> STRUCTURE_DEFINITION = null;

    private int stageBanks;

    public MTECascadeHall(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTECascadeHall(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTECascadeHall(this.mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RECIPE_MAP;
    }

    @Override
    protected Casings faceCasing() {
        return Casings.TungstensteelTurbineCasing;
    }

    @Override
    protected ITexture[] frontOverlay(boolean active) {
        return active
            ? overlayPair(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE, OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW)
            : overlayPair(OVERLAY_FRONT_DISTILLATION_TOWER, OVERLAY_FRONT_DISTILLATION_TOWER_GLOW);
    }

    /** Stage banks fitted on the last successful structure check, 0..{@link #MAX_BANKS}. */
    public int getStageBanks() {
        return stageBanks;
    }

    /**
     * {@code 2^banks}: 1, 2, 4, 8, 16. Doubling per bank is the whole reason a longer hall is
     * worth building, and it caps at 16 so the top hall is a real target rather than an
     * asymptote.
     */
    @Override
    public int getMaxParallelRecipes() {
        return 1 << Math.max(0, Math.min(MAX_BANKS, stageBanks));
    }

    /** Tidy B8: the GUI's detail number is the stage-bank count, not a shell tier. */
    @Override
    public int getParallelDetail() {
        return stageBanks;
    }

    /** Tidy B8, GUI line: {@code stage banks 3/4}. Argument only (AXIS 1). */
    @Override
    public String parallelSource(int banks) {
        return "stage banks " + Math.max(0, Math.min(MAX_BANKS, banks)) + "/" + MAX_BANKS;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        stageBanks = 0;
        if (!checkPiece(PIECE_FRONT, 2, 2, 0, errors)) return;
        // Probe for banks one at a time. The probe MUST restore mCasing on failure: a bank that
        // is half-built still passes some of its cells, and every 'T' cell increments the counter
        // from inside check() - so a failed probe would leave the casing total inflated by
        // however far it got, and the minimum-casing test below would pass on a hall that is
        // missing a wall.
        while (stageBanks < MAX_BANKS) {
            final int before = mCasing;
            if (checkPiece(PIECE_BANK, 2, 2, -(PIECE_DEPTH + PIECE_DEPTH * stageBanks))) {
                stageBanks++;
            } else {
                mCasing = before;
                break;
            }
        }
        if (!checkPiece(PIECE_BACK, 2, 2, -(PIECE_DEPTH + PIECE_DEPTH * stageBanks), errors)) return;
        checkCasingMin(errors, mCasing, 100);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
        // Every cascade recipe makes two fluids (cut + tails) and a plain Output Hatch holds one, so one
        // plain hatch would form and then stall on "not enough fluid output space" for good.
        if (!mOutputHatches.isEmpty() && mOutputHatches.size() < 2
            && mOutputHatches.stream()
                .noneMatch(h -> h instanceof MTEHatchOutputME)) {
            errors.add(StructureErrors.of("beyondthestars.structure.cascade.outputs"));
        }
    }

    /** Hologram/build size comes off the held stack: 1 gives the bare hall, 5 the full cascade. */
    private static int requestedBanks(ItemStack stackSize) {
        if (stackSize == null) return 0;
        return Math.max(0, Math.min(MAX_BANKS, stackSize.stackSize - 1));
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int banks = requestedBanks(stackSize);
        buildPiece(PIECE_FRONT, stackSize, hintsOnly, 2, 2, 0);
        for (int i = 0; i < banks; i++) {
            buildPiece(PIECE_BANK, stackSize, hintsOnly, 2, 2, -(PIECE_DEPTH + PIECE_DEPTH * i));
        }
        buildPiece(PIECE_BACK, stackSize, hintsOnly, 2, 2, -(PIECE_DEPTH + PIECE_DEPTH * banks));
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        final int banks = requestedBanks(stackSize);
        int built = survivalBuildPiece(PIECE_FRONT, stackSize, 2, 2, 0, elementBudget, env, false, true);
        if (built >= 0) return built;
        for (int i = 0; i < banks; i++) {
            built = survivalBuildPiece(
                PIECE_BANK,
                stackSize,
                2,
                2,
                -(PIECE_DEPTH + PIECE_DEPTH * i),
                elementBudget,
                env,
                false,
                true);
            if (built >= 0) return built;
        }
        return survivalBuildPiece(
            PIECE_BACK,
            stackSize,
            2,
            2,
            -(PIECE_DEPTH + PIECE_DEPTH * banks),
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public IStructureDefinition<MTECascadeHall> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            // Read [y from top][z from front][x]. TC-1: every hatch has ONE dedicated cell on
            // the front face, so the hologram shows the exact count at the exact spot and there
            // is no blanket wall adder to hide a miscount in.
            final String[][] front = { { "TTTTT", "TTTTT", "TTTTT", "TTTTT" }, { "TmTeT", "TAAAT", "TAAAT", "TAAAT" },
                { "Tu~uT", "TAAAT", "TAAAT", "TAAAT" }, { "TTiTT", "TAAAT", "TAAAT", "TAAAT" },
                { "TTTTT", "TTTTT", "TTTTT", "TTTTT" } };
            // One stage bank: a 3x3 ring of Iridium Reinforced Block around a centrifuge rotor
            // bore, then three courses of empty shell before the next.
            final String[][] bank = { { "TTTTT", "TTTTT", "TTTTT", "TTTTT" }, { "TIIIT", "TAAAT", "TAAAT", "TAAAT" },
                { "TIRIT", "TAAAT", "TAAAT", "TAAAT" }, { "TIIIT", "TAAAT", "TAAAT", "TAAAT" },
                { "TTTTT", "TTTTT", "TTTTT", "TTTTT" } };
            final String[][] back = { { "TTTTT", "TTTTT", "TTTTT", "TTTTT" }, { "TAAAT", "TAAAT", "TAAAT", "TTTTT" },
                { "TAAAT", "TAAAT", "TAAAT", "TTTTT" }, { "TAAAT", "TAAAT", "TAAAT", "TTTTT" },
                { "TTTTT", "TTTTT", "TTTTT", "TTTTT" } };
            STRUCTURE_DEFINITION = StructureDefinition.<MTECascadeHall>builder()
                .addShape(PIECE_FRONT, transpose(front))
                .addShape(PIECE_BANK, transpose(bank))
                .addShape(PIECE_BACK, transpose(back))
                .addElement(
                    'T',
                    noOverwrite(onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement())))
                .addElement('I', noOverwrite(ofBlock(GregTechAPI.sBlockReinforced, 1)))
                .addElement('R', noOverwrite(Casings.TungstensteelPipeCasing.asElement()))
                .addElement('A', noOverwrite(isAir()))
                .addElement(
                    'm',
                    buildHatchAdder(MTECascadeHall.class).atLeast(Maintenance)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            noOverwrite(
                                onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement()))))
                .addElement(
                    'e',
                    buildHatchAdder(MTECascadeHall.class).atLeast(Energy)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(2)
                        .buildAndChain(
                            noOverwrite(
                                onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement()))))
                .addElement(
                    'i',
                    buildHatchAdder(MTECascadeHall.class).atLeast(InputHatch)
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(3)
                        .buildAndChain(
                            noOverwrite(
                                onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement()))))
                .addElement(
                    'u',
                    // a count of 2, so the projector's survival path fills BOTH cells (with the plain count of 1
                    // it stops after one and the hall would not form; checkMachine wants 2 or one ME hatch)
                    buildHatchAdder(MTECascadeHall.class)
                        .atLeast(
                            Collections.<IHatchElement<? super MTECascadeHall>, Integer>singletonMap(OutputHatch, 2))
                        .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                        .hint(4)
                        .buildAndChain(
                            noOverwrite(
                                onElementPass(x -> ++x.mCasing, Casings.TungstensteelTurbineCasing.asElement()))))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Stage banks: " + stageBanks + "/" + MAX_BANKS + " · hall depth " + (8 + 4 * stageBanks));
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Gas Centrifuge")
            .addInfo("Splits Uranium Hexafluoride into an enriched cut and tails.")
            .addSeparator()
            .addInfo(
                "Circuit " + BTSTip.num(1)
                    + ": low-enriched cut ("
                    + BTSTip.tier(gregtech.api.enums.VoltageIndex.IV)
                    + ").")
            .addInfo(
                "Circuit " + BTSTip.num(2)
                    + ": highly-enriched cut ("
                    + BTSTip.tier(gregtech.api.enums.VoltageIndex.LuV)
                    + " power).")
            .addInfo("No input bus: the circuit goes in the controller slot.")
            .addInfo("The tails (" + BTSTip.term("Depleted UF6") + ") are a product, not waste:")
            .addInfo("MOX diluent and Depleted Uranium Ingots.")
            .addSeparator()
            .addInfo(
                "Each stage bank doubles the parallels: " + BTSTip.parallel("1")
                    + " to "
                    + BTSTip.parallel(String.valueOf(1 << MAX_BANKS))
                    + ".")
            .addInfo("Your Energy Hatches' power also caps them.")
            .beginVariableStructureBlock(5, 5, 5, 5, 8, 24, true)
            .addController("Front face, centre")
            .addCasing("140-397", "Tungstensteel Turbine Casing (+64 per stage bank)", false)
            .addCasing("0-32", "Iridium Reinforced Block (8 per stage bank)", false)
            .addCasing("0-4", "Tungstensteel Pipe Casing (1 per stage bank)", false)
            .addAir("The whole interior: keep pipes and cables outside")
            .addEnergyHatch("1", "Front face, 2nd row from the top, right", 2)
            .addMaintenanceHatch("1", "Front face, 2nd row from the top, left", 1)
            .addInputHatch("1", "Front face, directly below the controller", 3)
            .addOutputHatch("2", "Front face, either side of the controller (or 1 ME Output Hatch)", 4)
            .addStructureInfo("Each stage bank adds 4 blocks of length: Iridium")
            .addStructureInfo("Reinforced Block round one Tungstensteel Pipe Casing.")
            .addStructureInfo("Projector: stack size 1-5 builds 0-4 stage banks.")
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }
}
