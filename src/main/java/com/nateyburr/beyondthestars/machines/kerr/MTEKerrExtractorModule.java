package com.nateyburr.beyondthestars.machines.kerr;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
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
import gregtech.api.render.TextureFactory;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * KERR REDESIGN: the Kerr Extractor Module, MTE base+18 (28118). A separate machine standing in the
 * KEE's east arc (docs/kerr-redesign.md). It runs GT's ITEM extractor recipes
 * ({@code extractorRecipes}) and nothing else (user ruling, Q-9/Q-22), so its sockets take item
 * buses only (plus its own energy).
 * <p>
 * Its controller is the outermost arc cell on the 340 degree ray, on the outer face (the old core
 * cell faced the hole and is an I/O socket now, with two more sockets beside the controller).
 */
public class MTEKerrExtractorModule extends MTEKerrModuleBase<MTEKerrExtractorModule> {

    private static final IIconContainer[] OVERLAYS = {
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_EXTRACTOR"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_EXTRACTOR_GLOW"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_EXTRACTOR_ACTIVE"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_EXTRACTOR_ACTIVE_GLOW") };

    private static volatile IStructureDefinition<MTEKerrExtractorModule> STRUCTURE_DEFINITION;

    public MTEKerrExtractorModule(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEKerrExtractorModule(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEKerrExtractorModule(this.mName);
    }

    @Override
    public KerrSlot slot() {
        return KerrSlot.EXTRACTOR;
    }

    @Override
    public IStructureDefinition<MTEKerrExtractorModule> getStructureDefinition() {
        IStructureDefinition<MTEKerrExtractorModule> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEKerrExtractorModule.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = StructureDefinition.<MTEKerrExtractorModule>builder()
                        .addShape(MTEKerrEngine.PIECE_EXTRACTOR, BTSShapes.load(MTEKerrEngine.PIECE_EXTRACTOR))
                        .addElement('J', noOverwrite(ofBlock(SLBlocks.slCasing2, BlockSLCasing2.LINING)))
                        .addElement('K', noOverwrite(Casings.HawkingRadiationRealignmentFocus.asElement()))
                        .addElement('N', noOverwrite(ofBlock(SLBlocks.slCasing, BlockSLCasing.EXTRACTION)))
                        .addElement(
                            'x',
                            buildHatchAdder(MTEKerrExtractorModule.class)
                                .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy))
                                .casingIndex(Casings.ExtremeDensitySpaceBendingCasing.textureId)
                                .hint(1)
                                .buildAndChain(noOverwrite(ofBlock(SLBlocks.slCasing, BlockSLCasing.EXTRACTION))))
                        .build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.extractorRecipes;
    }

    @Override
    public String mapsLine() {
        return "§7Maps: GT extractor (items only)";
    }

    /** GT's item extractor map only (Q-9/Q-22). */
    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.singletonList(RecipeMaps.extractorRecipes);
    }

    @Override
    protected ITexture casingTexture() {
        return TextureFactory.of(SLBlocks.slCasing, BlockSLCasing.EXTRACTION);
    }

    @Override
    protected IIconContainer[] overlays() {
        return OVERLAYS;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final String p = MTEKerrEngine.PIECE_EXTRACTOR;
        final int[] size = BTSShapes.size(p);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Extractor")
            .addInfo("A Kerr Ergosphere Engine module: a separate machine standing in the KEE's east arc.")
            .addInfo("Runs only while linked to a formed KEE with an OPEN hole. When the hole closes it")
            .addInfo("  pauses and keeps its recipe; it resumes when the hole reopens.")
            .addInfo("Runs GT's Extractor recipes (items only).")
            .addInfo("The hole sets the bonus, fixed when a recipe starts:")
            .addInfo("  parallel 8 x voltage tier (this module's energy) x floor(sqrt(M)),")
            .addInfo("  speed x(3200/M)/50, from 1 to 4. The module costs the hole nothing.")
            .addSeparator()
            .addInfo("Place this controller where the KEE's hologram shows marker 8, facing out of the cage.")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("East arc, outer face, on the 340 degree ray")
            .addCasingInfoExactly("Ergosphere Extraction Casing", MTEKerrCompressorModule.bom(p, 'N'), false)
            .addCasingInfoExactly("Harmonic Lining", MTEKerrCompressorModule.bom(p, 'J'), false)
            .addCasingInfoExactly("Hawking Radiation Realignment Focus", MTEKerrCompressorModule.bom(p, 'K'), false)
            .addInputBus("Any arc socket (at least 1)", 1)
            .addOutputBus("Any arc socket (at least 1)", 1)
            .addEnergyHatch("Any arc socket (1 laser/multi-amp or up to 2 energy hatches)", 1)
            .addStructureInfo("No maintenance hatch. Item buses only. Hologram: one tier, the arc.")
            .toolTipFinisher();
        return tt;
    }
}
