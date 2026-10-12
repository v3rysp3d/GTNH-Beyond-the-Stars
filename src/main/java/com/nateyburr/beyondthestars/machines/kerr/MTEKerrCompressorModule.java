package com.nateyburr.beyondthestars.machines.kerr;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * KERR REDESIGN: the Kerr Compressor Module, MTE base+17 (28117). A separate machine standing in
 * the KEE's south arc (docs/kerr-redesign.md). It searches GT's compressor and
 * neutronium-compressor maps (the Black Hole Compressor's) and {@link KerrRecipeMaps#DENSITY}
 * together, so it has no machine modes: the inputs pick the map. A DENSITY recipe needs the linked
 * KEE at Kerr T2 with at least 128 SM.
 * <p>
 * Its controller is the arc's outer shell cell below the T2 reinforcement band (the old core cell
 * above it is an I/O socket now, see the exporter). Sockets ({@code c}) take GT's standard input /
 * output buses and hatches and its own energy; at least 1 in, 1 out and 1 energy.
 */
public class MTEKerrCompressorModule extends MTEKerrModuleBase<MTEKerrCompressorModule> {

    private static final IIconContainer[] OVERLAYS = {
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_COMPRESSOR"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_GLOW"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE_GLOW") };

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread. */
    private static volatile IStructureDefinition<MTEKerrCompressorModule> STRUCTURE_DEFINITION;

    public MTEKerrCompressorModule(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEKerrCompressorModule(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEKerrCompressorModule(this.mName);
    }

    @Override
    public KerrSlot slot() {
        return KerrSlot.COMPRESSOR;
    }

    @Override
    public IStructureDefinition<MTEKerrCompressorModule> getStructureDefinition() {
        IStructureDefinition<MTEKerrCompressorModule> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEKerrCompressorModule.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = StructureDefinition.<MTEKerrCompressorModule>builder()
                        .addShape(MTEKerrEngine.PIECE_COMPRESSOR, BTSShapes.load(MTEKerrEngine.PIECE_COMPRESSOR))
                        .addElement('G', noOverwrite(Casings.ParticleBeamGuidancePipeCasing.asElement()))
                        .addElement('I', noOverwrite(Casings.BackgroundRadiationAbsorbentCasing.asElement()))
                        .addElement('J', noOverwrite(ofBlock(SLBlocks.slCasing2, BlockSLCasing2.LINING)))
                        .addElement('K', noOverwrite(Casings.HawkingRadiationRealignmentFocus.asElement()))
                        .addElement(
                            'c',
                            buildHatchAdder(MTEKerrCompressorModule.class)
                                .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy))
                                .casingIndex(Casings.BackgroundRadiationAbsorbentCasing.textureId)
                                .hint(1)
                                .buildAndChain(noOverwrite(Casings.BackgroundRadiationAbsorbentCasing.asElement())))
                        .build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    // ============================================================================ recipes

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.compressorRecipes;
    }

    /**
     * Searched together, in this order; NEI's catalyst index reads it too. DENSITY goes FIRST: its
     * recipe (SL-K05) takes molten SpaceTime, and the coremod's neutronium-compressor recipe turns 72 L
     * of molten SpaceTime alone into an Eternal Singularity, so with DENSITY searched last a T2 module
     * fed for SL-K05 made Eternal Singularities instead (dev-server test, B-43). When the DENSITY gate
     * is shut (T1, under 128 SM) the search falls through to GT's maps as before.
     */
    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.<RecipeMap<?>>asList(
            KerrRecipeMaps.DENSITY,
            RecipeMaps.compressorRecipes,
            RecipeMaps.neutroniumCompressorRecipes);
    }

    /** DENSITY (SL-K05) needs the linked KEE at Kerr T2 holding at least 128 SM. */
    @Nullable
    @Override
    protected CheckRecipeResult gateRecipe(@Nonnull GTRecipe recipe) {
        if (!KerrRecipeMaps.DENSITY.getAllRecipes()
            .contains(recipe)) return null;
        final MTEKerrEngine kee = kee();
        if (kee == null || !kee.densityInstalled() || kee.mass() < MTEKerrEngine.DENSIFY_MIN_MASS) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module.density");
        }
        return null;
    }

    @Override
    public String mapsLine() {
        return "§7Maps: compressor + neutronium compressor + Kerr density (the inputs pick)";
    }

    /** The DENSITY gate, live: what is missing, or that it is open. */
    @Override
    public String gateLine() {
        final MTEKerrEngine kee = kee();
        if (kee == null || !kee.isHoleOpen()) {
            return "§8Densification: needs a linked Kerr T2 hole of 128 SM";
        }
        if (!kee.densityInstalled()) {
            return "§7Densification: needs Kerr T2 (Density Upgrade in the KEE)";
        }
        if (kee.mass() < MTEKerrEngine.DENSIFY_MIN_MASS) {
            return "§7Densification: needs 128 SM (hole " + MTEKerrEngine.fmt(kee.mass()) + " SM)";
        }
        return "§bDensification: available (Kerr T2, " + MTEKerrEngine.fmt(kee.mass()) + " SM)";
    }

    // ============================================================================ looks

    @Override
    protected ITexture casingTexture() {
        return Casings.BackgroundRadiationAbsorbentCasing.getCasingTexture();
    }

    @Override
    protected IIconContainer[] overlays() {
        return OVERLAYS;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final String p = MTEKerrEngine.PIECE_COMPRESSOR;
        final int[] size = BTSShapes.size(p);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Compressor, Neutronium Compressor, Kerr Densification")
            .addInfo("A Kerr Ergosphere Engine module: a separate machine standing in the KEE's south arc.")
            .addInfo("Runs only while linked to a formed KEE with an OPEN hole. When the hole closes it")
            .addInfo("  pauses and keeps its recipe; it resumes when the hole reopens.")
            .addInfo("Runs GT's Compressor and Neutronium Compressor recipes (the Black Hole Compressor's)")
            .addInfo("  and Kerr Densification (needs Kerr T2 and a hole of at least 128 SM). No modes.")
            .addInfo("The hole sets the bonus, fixed when a recipe starts:")
            .addInfo("  parallel 8 x voltage tier (this module's energy) x floor(sqrt(M)),")
            .addInfo("  speed x(3200/M)/50, from 1 to 4. The module costs the hole nothing.")
            .addSeparator()
            .addInfo("Place this controller where the KEE's hologram shows marker 7, facing out of the cage.")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("South arc, outer face, below the Kerr T2 band")
            .addCasingInfoExactly("Background Radiation Absorbent Casing", bom(p, 'I'), false)
            .addCasingInfoExactly("Harmonic Lining", bom(p, 'J'), false)
            .addCasingInfoExactly("Hawking Radiation Realignment Focus", bom(p, 'K'), false)
            .addCasingInfoExactly("Particle Beam Guidance Pipe Casing", bom(p, 'G'), false)
            .addInputBus("Any arc socket (at least 1 input bus or hatch)", 1)
            .addInputHatch("Any arc socket", 1)
            .addOutputBus("Any arc socket (at least 1 output bus or hatch)", 1)
            .addOutputHatch("Any arc socket", 1)
            .addEnergyHatch("Any arc socket (1 laser/multi-amp or up to 2 energy hatches)", 1)
            .addStructureInfo("No maintenance hatch. Hologram: one tier, the arc.")
            .toolTipFinisher();
        return tt;
    }

    static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }
}
