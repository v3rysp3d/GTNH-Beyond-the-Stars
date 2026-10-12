package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.tooltip.TooltipHelper;

/**
 * D-26 THE FUEL FABRICATION HALL - MTE 28110, IV (docs/production-lines.md S5.2).
 * <p>
 * <b>Why nothing already in the pack does this.</b> Throughput, and it is a real building. A
 * tier-5 silo eats <b>201 rods per 90-minute decay cycle</b>, in up to four grades at once. The
 * Assembly Line runs one recipe at a time behind research; the Assembler has no parallel at all.
 * A fuel fabrication plant is a continuous line and this one behaves like one.
 * <p>
 * <b>Parallel is the shell.</b> {@code parallel = 8 x shell tier} - Clean Stainless Steel 8,
 * Iridium Reinforced 16, Radiant Naquadah Alloy 24 - and the tier is the WEAKEST block in the
 * wall, not the best. A hall with one naquadah block in a stainless wall is a stainless hall; the
 * upgrade is re-walling the building, which is what the material lines of S3 are for.
 * <p>
 * <b>It handles the first three grades only.</b> F-17 and F-18 on circuits 1-3 (uranium, MOX,
 * thorium). The naquadah grade's rod goes to the Precise Assembler and its bundle to the Assembly
 * Line instead, so the top fuel in the game still costs a real endgame machine and this hall
 * cannot become the one building that makes everything.
 */
public class MTEFuelFabricationHall extends MTEProductionHallBase<MTEFuelFabricationHall> {

    /** F-17 and F-18, grades 1-3. A real map: the hall genuinely processes out of it. */
    public static final RecipeMap<RecipeMapBackend> RECIPE_MAP = RecipeMapBuilder
        .of("beyondthestars.recipe.fabrication")
        .maxIO(6, 2, 2, 1)
        .minInputs(1, 0)
        .build();

    private static IStructureDefinition<MTEFuelFabricationHall> STRUCTURE_DEFINITION = null;

    /** Shell census, one counter per legal wall block. Server-side, re-derived every check. */
    private int shellStainless;
    private int shellIridium;
    private int shellNaquadah;

    public MTEFuelFabricationHall(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEFuelFabricationHall(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEFuelFabricationHall(this.mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RECIPE_MAP;
    }

    @Override
    protected Casings faceCasing() {
        return Casings.CleanStainlessSteelMachineCasing;
    }

    @Override
    protected ITexture[] frontOverlay(boolean active) {
        return active ? overlayPair(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE, OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW)
            : overlayPair(OVERLAY_FRONT_ASSEMBLY_LINE, OVERLAY_FRONT_ASSEMBLY_LINE_GLOW);
    }

    /** 8, 16 or 24 - see the class comment on why the tier is the weakest block in the wall. */
    @Override
    public int getMaxParallelRecipes() {
        return 8 * Math.max(1, casingTier);
    }

    /** Tidy B8: the shell block for each tier, weakest first, as the GUI names it. */
    private static final String[] SHELL_NAMES = { "Clean Stainless Steel Machine Casing", "Iridium Reinforced Block",
        "Radiant Naquadah Alloy Casing" };

    /** Tidy B8, GUI line: {@code shell: Iridium Reinforced Block, tier 2/3}. Argument only (AXIS 1). */
    @Override
    public String parallelSource(int tier) {
        final int t = Math.max(1, Math.min(SHELL_NAMES.length, tier));
        return "shell: " + SHELL_NAMES[t - 1] + ", tier " + t + "/" + SHELL_NAMES.length;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        shellStainless = 0;
        shellIridium = 0;
        shellNaquadah = 0;
        casingTier = 1;
        if (!checkPiece(mName, 3, 2, 0, errors)) return;
        // The WEAKEST block present sets the tier. Scoring the best block instead would let a
        // single Radiant Naquadah Alloy casing dropped into a stainless wall triple the hall's
        // parallel, which is not an upgrade path, it is an exploit.
        casingTier = shellStainless > 0 ? 1 : shellIridium > 0 ? 2 : shellNaquadah > 0 ? 3 : 1;
        checkCasingMin(errors, mCasing, 250);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputBus(errors);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 3, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 3, 2, 0, elementBudget, env, false, true);
    }

    @Override
    public IStructureDefinition<MTEFuelFabricationHall> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            // Read [y from top][z from front][x]. 7 wide, 5 tall, 12 deep, with a Tungstensteel
            // Turbine Casing spine ('S') down the centreline of the working level.
            final String[] roof = { "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC",
                "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC" };
            final String[] upper = { "CmCeCeC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC",
                "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CCCCCCC" };
            final String[] work = { "CbC~CbC", "CAASAAC", "CAASAAC", "CAASAAC", "CAASAAC", "CAASAAC", "CAASAAC",
                "CAASAAC", "CAASAAC", "CAASAAC", "CAASAAC", "CCCCCCC" };
            final String[] lower = { "CiCCCoC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC",
                "CAAAAAC", "CAAAAAC", "CAAAAAC", "CAAAAAC", "CCCCCCC" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTEFuelFabricationHall>builder()
                .addShape(mName, transpose(new String[][] { roof, upper, work, lower, roof }))
                // AXIS 4: ofChain passes check() if ANY branch passes, but spawnHint and
                // placeBlock return at the FIRST branch that answers true - so the hologram
                // draws, and the build places, Clean Stainless Steel. That is the intended
                // behaviour: the cheap wall is what a player is told to build, and the two
                // upgrades are things you re-wall INTO, deliberately, later.
                .addElement('C', noOverwrite(ofChain(onElementPass(x -> {
                    x.mCasing++;
                    x.shellStainless++;
                }, Casings.CleanStainlessSteelMachineCasing.asElement()), onElementPass(x -> {
                    x.mCasing++;
                    x.shellIridium++;
                }, ofBlock(GregTechAPI.sBlockReinforced, 1)), onElementPass(x -> {
                    x.mCasing++;
                    x.shellNaquadah++;
                }, Casings.RadiantNaquadahAlloyCasing.asElement()))))
                .addElement('S', noOverwrite(Casings.TungstensteelTurbineCasing.asElement()))
                .addElement('A', noOverwrite(isAir()))
                .addElement('m', socket(Maintenance, 1))
                .addElement('e', socket(Energy, 2))
                .addElement('b', socket(InputBus, 3))
                .addElement('i', socket(InputHatch, 4))
                .addElement('o', socket(OutputBus, 5))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    /**
     * TC-1: one dedicated cell per hatch, chained to the same tiered wall element the rest of the
     * shell uses - so an empty socket is a legal (if useless) piece of wall and still counts
     * toward the tier, and the hologram shows the exact hatch at the exact spot.
     */
    private static com.gtnewhorizon.structurelib.structure.IStructureElement<MTEFuelFabricationHall> socket(
        gregtech.api.enums.HatchElement element, int hint) {
        return buildHatchAdder(MTEFuelFabricationHall.class).atLeast(element)
            .casingIndex(Casings.CleanStainlessSteelMachineCasing.textureId)
            .hint(hint)
            .buildAndChain(noOverwrite(ofChain(onElementPass(x -> {
                x.mCasing++;
                x.shellStainless++;
            }, Casings.CleanStainlessSteelMachineCasing.asElement()), onElementPass(x -> {
                x.mCasing++;
                x.shellIridium++;
            }, ofBlock(GregTechAPI.sBlockReinforced, 1)), onElementPass(x -> {
                x.mCasing++;
                x.shellNaquadah++;
            }, Casings.RadiantNaquadahAlloyCasing.asElement()))));
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        // Tidy C12: one shell line, named as the GUI names it; the weakest block sets the tier.
        final int t = Math.max(1, Math.min(SHELL_NAMES.length, casingTier));
        info.add(
            "Shell: " + SHELL_NAMES[t - 1]
                + " (tier "
                + t
                + "/"
                + SHELL_NAMES.length
                + ") - the weakest block sets the tier");
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String gray = EnumChatFormatting.GRAY.toString();
        tt.addMachineType("Fuel Rod Assembler")
            .addInfo("Loads pellets into cladding tubes and bundles rods into assemblies.")
            .addInfo("A tier-5 Reactor Silo burns 201 rods per decay cycle: this feeds it.")
            .addInfo("Circuit 1 / 2 / 3 = the uranium, MOX and thorium grades.")
            .addInfo("The naquadah grade is made elsewhere: its rods in a Precise Assembler,")
            .addInfo("its assemblies on an Assembly Line.")
            .addInfo("Parallel: " + TooltipHelper.parallelText(8) + " per shell tier (8 / 16 / 24)")
            .addInfo(aqua + "Shell tier:" + gray + " Clean Stainless Steel 1, Iridium Reinforced 2,")
            .addInfo("Radiant Naquadah Alloy 3. The weakest block in the wall sets it.")
            .addInfo("Parallels are also limited by the power your Energy Hatches supply.")
            .beginStructureBlock(7, 5, 12, true)
            .addController("Front face, centre")
            .addCasing(
                "262-264",
                "Clean Stainless Steel Machine Casing / Iridium Reinforced Block / Radiant Naquadah Alloy Casing",
                true)
            .addCasing("10", "Tungstensteel Turbine Casing (spine down the centreline)", false)
            .addAir("The rest of the interior: keep pipes and cables outside")
            .addEnergyHatch("1-2", "Front face, 2nd row from the top", 2)
            .addMaintenanceHatch("1", "Front face, 2nd row from the top, left", 1)
            .addInputBus("1-2", "Front face, either side of the controller", 3)
            .addInputHatch("1", "Front face, 2nd row from the bottom, left", 4)
            .addOutputBus("1", "Front face, 2nd row from the bottom, right", 5)
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }
}
