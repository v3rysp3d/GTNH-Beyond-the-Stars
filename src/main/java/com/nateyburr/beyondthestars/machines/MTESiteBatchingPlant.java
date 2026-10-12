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
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
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
import gregtech.api.enums.Materials;
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
 * D-26 THE SITE BATCHING PLANT - MTE 28112, EV (docs/production-lines.md S5.4).
 * <p>
 * <b>Why nothing already in the pack does this.</b> Volume. A tier-5 buildout of the Nuclear Plant
 * is roughly 8,000 blocks and <b>3,773 of them are Light Concrete</b>. Single-block-assembling
 * that is the largest single friction in building this plant, and the questbook's Blueprint quests
 * quote those counts straight at the player. Every real construction site of this size has a
 * batching plant standing on it; this one should too.
 * <p>
 * <b>It can make nothing GT cannot already make.</b> Every M-01 to M-06 output is a stock block
 * with a stock recipe that is left completely alone. What this machine sells is 8-32x the batch
 * for a multiblock and a higher voltage - a bulk route beside GT's own, never a replacement for
 * it.
 * <p>
 * <b>Deliberately the cheapest of the four new machines,</b> because it is the FIRST one a player
 * builds - before the silo, to build the silo. Solid Steel shell, Steel Frame Box lattice, one
 * Steel Pipe Casing riser, EV.
 * <p>
 * <b>Parallel = 4 x shell tier.</b> Solid Steel 4, Clean Stainless 8, Tungstensteel Reinforced 12,
 * and as in the Fuel Fabrication Hall the WEAKEST block in the wall sets the tier. The two upgrade
 * shells are blocks the plant already needs in bulk, so the batching plant ends up paying to
 * upgrade itself out of its own output.
 */
public class MTESiteBatchingPlant extends MTEProductionHallBase<MTESiteBatchingPlant> {

    /** M-01 to M-06, minus M-02d which is a Forming Press recipe on GT's own map. */
    public static final RecipeMap<RecipeMapBackend> RECIPE_MAP = RecipeMapBuilder.of("beyondthestars.recipe.batching")
        .maxIO(4, 2, 2, 1)
        .minInputs(1, 0)
        .build();

    private static IStructureDefinition<MTESiteBatchingPlant> STRUCTURE_DEFINITION = null;

    /** Shell census, one counter per legal wall block. Server-side, re-derived every check. */
    private int shellSteel;
    private int shellStainless;
    private int shellTungsten;

    public MTESiteBatchingPlant(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTESiteBatchingPlant(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTESiteBatchingPlant(this.mName);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RECIPE_MAP;
    }

    @Override
    protected Casings faceCasing() {
        return Casings.SolidSteelMachineCasing;
    }

    @Override
    protected ITexture[] frontOverlay(boolean active) {
        return active
            ? overlayPair(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE, OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW)
            : overlayPair(OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE, OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW);
    }

    /** 4, 8 or 12 - the weakest block in the wall sets the tier. */
    @Override
    public int getMaxParallelRecipes() {
        return 4 * Math.max(1, casingTier);
    }

    /** Tidy B8: the shell block for each tier, weakest first, as the GUI names it. */
    private static final String[] SHELL_NAMES = { "Solid Steel Machine Casing", "Clean Stainless Steel Machine Casing",
        "Tungstensteel Reinforced Block" };

    /** Tidy B8, GUI line: {@code shell: Iridium Reinforced Block, tier 2/3}. Argument only (AXIS 1). */
    @Override
    public String parallelSource(int tier) {
        final int t = Math.max(1, Math.min(SHELL_NAMES.length, tier));
        return "shell: " + SHELL_NAMES[t - 1] + ", tier " + t + "/" + SHELL_NAMES.length;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCasing = 0;
        shellSteel = 0;
        shellStainless = 0;
        shellTungsten = 0;
        casingTier = 1;
        if (!checkPiece(mName, 2, 2, 0, errors)) return;
        casingTier = shellSteel > 0 ? 1 : shellStainless > 0 ? 2 : shellTungsten > 0 ? 3 : 1;
        checkCasingMin(errors, mCasing, 100);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputBus(errors);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 2, 2, 0, elementBudget, env, false, true);
    }

    @Override
    public IStructureDefinition<MTESiteBatchingPlant> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            // Read [y from top][z from front][x]. 5 wide, 5 tall, 9 deep.
            final String[] roof = { "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS" };
            final String[] upper = { "SmSeS", "SGAGS", "SAAAS", "SGAGS", "SAPAS", "SGAGS", "SAAAS", "SGAGS", "SSSSS" };
            final String[] work = { "Sb~bS", "SAAAS", "SAAAS", "SAAAS", "SAPAS", "SAAAS", "SAAAS", "SAAAS", "SSSSS" };
            final String[] lower = { "SiSoS", "SGAGS", "SAAAS", "SGAGS", "SAPAS", "SGAGS", "SAAAS", "SGAGS", "SSSSS" };
            STRUCTURE_DEFINITION = StructureDefinition.<MTESiteBatchingPlant>builder()
                .addShape(mName, transpose(new String[][] { roof, upper, work, lower, roof }))
                .addElement('S', noOverwrite(shell()))
                .addElement('G', noOverwrite(ofFrame(Materials.Steel)))
                .addElement('P', noOverwrite(Casings.SteelPipeCasing.asElement()))
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
     * AXIS 4: {@code ofChain} passes {@code check} if ANY branch passes, but {@code spawnHint} and
     * {@code placeBlock} return at the FIRST branch that answers true - so the hologram draws, and
     * the projection builds, plain Solid Steel. That is what a player should be told to build; the
     * two upgrade shells are things you re-wall INTO once the plant is making them by the crate.
     */
    private static IStructureElement<MTESiteBatchingPlant> shell() {
        return ofChain(onElementPass(x -> {
            x.mCasing++;
            x.shellSteel++;
        }, Casings.SolidSteelMachineCasing.asElement()), onElementPass(x -> {
            x.mCasing++;
            x.shellStainless++;
        }, Casings.CleanStainlessSteelMachineCasing.asElement()), onElementPass(x -> {
            x.mCasing++;
            x.shellTungsten++;
        }, ofBlock(GregTechAPI.sBlockReinforced, 3)));
    }

    /** TC-1: one dedicated cell per hatch, chained to the same tiered shell behind it. */
    private static IStructureElement<MTESiteBatchingPlant> socket(HatchElement element, int hint) {
        return buildHatchAdder(MTESiteBatchingPlant.class).atLeast(element)
            .casingIndex(Casings.SolidSteelMachineCasing.textureId)
            .hint(hint)
            .buildAndChain(noOverwrite(shell()));
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(
            "Shell: " + shellSteel
                + " solid steel / "
                + shellStainless
                + " clean stainless / "
                + shellTungsten
                + " tungstensteel reinforced");
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String gray = EnumChatFormatting.GRAY.toString();
        tt.addMachineType("Bulk Casing Assembler")
            .addInfo("Bulk-makes the casings and concrete the Nuclear Plant is built from.")
            .addInfo("A tier-5 plant is about 8,000 blocks; 3,773 of them are Light Concrete.")
            .addInfo("Makes nothing GT cannot: each recipe is an 8-32x bulk batch")
            .addInfo("of a stock GT recipe.")
            .addInfo(aqua + "Build it first:" + gray + " before the silo, to build the silo.")
            .addInfo("Parallel: " + TooltipHelper.parallelText(4) + " per shell tier (4 / 8 / 12)")
            .addInfo(aqua + "Shell tier:" + gray + " Solid Steel 1, Clean Stainless Steel 2,")
            .addInfo("Tungstensteel Reinforced 3. The weakest block in the wall sets it.")
            .addInfo("Parallels are also limited by the power your Energy Hatches supply.")
            .beginStructureBlock(5, 5, 9, true)
            .addController("Front face, centre")
            .addCasing(
                "155-156",
                "Solid Steel Machine Casing / Clean Stainless Steel Machine Casing / Tungstensteel Reinforced Block",
                true)
            .addCasing("16", "Steel Frame Box (lattice)", false)
            .addCasing("3", "Steel Pipe Casing (riser)", false)
            .addAir("The rest of the interior: keep pipes and cables outside")
            .addEnergyHatch("1", "Front face, 2nd row from the top, right", 2)
            .addMaintenanceHatch("1", "Front face, 2nd row from the top, left", 1)
            .addInputBus("1-2", "Front face, either side of the controller", 3)
            .addInputHatch("1", "Front face, 2nd row from the bottom, left", 4)
            .addOutputBus("1", "Front face, 2nd row from the bottom, right", 5)
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }
}
