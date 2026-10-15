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

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BTSRecipesQuasar;
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
     * fed for SL-K05 made Eternal Singularities instead (dev-server test, B-56). A DENSITY match also
     * hides GT's maps ({@link #shadowsLaterMaps}): while the gate is shut (T1, under 128 SM) the module
     * waits with "density" and keeps the SL-K05 inputs, and SL-K05 starts once the gate opens.
     */
    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.<RecipeMap<?>>asList(
            KerrRecipeMaps.DENSITY,
            RecipeMaps.compressorRecipes,
            RecipeMaps.neutroniumCompressorRecipes);
    }

    /** SL-K05 inputs wait for the DENSITY gate; they never fall through to GT's maps. */
    @Override
    protected boolean shadowsLaterMaps(@Nonnull RecipeMap<?> map) {
        return map == KerrRecipeMaps.DENSITY;
    }

    /**
     * P4: the running recipe needs the KEE's quasar link (SL-Q23, the Superdense refinery). Set when the recipe is
     * validated, saved with the run, so a restart mid-run keeps the condition.
     */
    private boolean runNeedsLink;

    /**
     * DENSITY needs the linked KEE at Kerr T2 holding at least 128 SM (SL-K05). P4's Quasar Seed (SL-Q09) and
     * Superdense refinery (SL-Q23) need 256 SM, and the refinery also a quasar link on the KEE at σ ≥ 1 (60 s mean,
     * quasar-p4.md §3.1, R.3).
     */
    @Nullable
    @Override
    protected CheckRecipeResult gateRecipe(@Nonnull GTRecipe recipe) {
        if (!KerrRecipeMaps.DENSITY.getAllRecipes()
            .contains(recipe)) {
            runNeedsLink = false;
            return null;
        }
        densityAsked = true;
        if (!recipeGateOpen()) return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module.density");
        final MTEKerrEngine kee = kee();
        final double minMass = BTSRecipesQuasar.densityMinMass(recipe);
        if (minMass > 0 && (kee == null || kee.mass() < minMass)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module.density256");
        }
        final boolean needsLink = BTSRecipesQuasar.densityNeedsLink(recipe);
        if (needsLink && !linkHolds(kee)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module.quasarlink");
        }
        runNeedsLink = needsLink;
        return null;
    }

    /**
     * QA-A12: the last recipe check met a DENSITY recipe (its inputs are in the module), so the Densification line
     * concerns the player. Without it a T1 compressor showed an amber "needs Kerr T2" all the time, which read as
     * "this module needs T2" although GT's compressor recipes run at T1. Server state.
     */
    private boolean densityAsked;

    private static boolean linkHolds(@Nullable MTEKerrEngine kee) {
        return kee != null && kee.burstSigma() >= BTSRecipesQuasar.SUPERDENSE_MIN_SIGMA;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        runNeedsLink = false;
        densityAsked = false;
        final CheckRecipeResult r = super.checkProcessing();
        if (!r.wasSuccessful()) runNeedsLink = false;
        return r;
    }

    /** A Superdense run pauses (kept, never voided) while the KEE's link is below σ 1. */
    @Override
    protected boolean runConditionHolds() {
        return !runNeedsLink || linkHolds(kee());
    }

    /** Bit 0 the DENSITY gate (T2, 128 SM), bit 1 256 SM, bit 2 the quasar link at σ ≥ 1. */
    @Override
    protected int gateState() {
        final MTEKerrEngine kee = kee();
        int bits = recipeGateOpen() ? 1 : 0;
        if (bits != 0 && kee.mass() >= BTSRecipesQuasar.DENSITY_P4_MIN_MASS) bits |= 2;
        if (linkHolds(kee)) bits |= 4;
        return bits;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (runNeedsLink) aNBT.setBoolean("btsKerrRunLink", true);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        runNeedsLink = aNBT.getBoolean("btsKerrRunLink");
    }

    /** The DENSITY gate: a linked open hole at Kerr T2 with at least 128 SM. */
    @Override
    protected boolean recipeGateOpen() {
        final MTEKerrEngine kee = kee();
        return kee != null && kee.isHoleOpen()
            && kee.densityInstalled()
            && kee.mass() >= MTEKerrEngine.DENSIFY_MIN_MASS;
    }

    /** The DENSITY gate, live: what is missing, or that it is open. */
    @Override
    public String gateLine() {
        // QA-A12: only once Densification inputs are in, or while its gate is open
        if (!densityAsked && !recipeGateOpen()) return "";
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
        final String p4;
        if (mMaxProgresstime > 0 && !runConditionHolds()) {
            p4 = "§e; Superdense refining PAUSED: the KEE's quasar link is below σ 1";
        } else if (kee.mass() < BTSRecipesQuasar.DENSITY_P4_MIN_MASS) {
            p4 = "§7; Quasar Seed and Superdense need 256 SM";
        } else {
            p4 = linkHolds(kee) ? "§b; Superdense: quasar link σ " + MTEKerrEngine.fmt(kee.burstSigma())
                : "§7; Superdense needs a quasar link on the KEE at σ ≥ 1";
        }
        return "§bDensification: available (Kerr T2, " + MTEKerrEngine.fmt(kee.mass()) + " SM)" + p4;
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
            .addInfo("  Quasar Seed and Superdense Ergomatter need 256 SM; Superdense also a quasar link on")
            .addInfo("  the KEE at σ ≥ 1 (a run that loses it pauses, never voids).")
            .addInfo("The hole sets the bonus, fixed when a recipe starts:")
            .addInfo("  parallel 8 x the tier of this module's total input (V x A) x floor(sqrt(M)),")
            .addInfo("  e.g. one 2 A UIV hatch counts as UMV: 384 at 16 SM, 768 at 64 SM;")
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
