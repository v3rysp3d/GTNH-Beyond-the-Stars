package com.nateyburr.beyondthestars.machines.kerr;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static com.nateyburr.beyondthestars.machines.BTSStructures.tieredBlock;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleUtility;

/**
 * SINGULARITY LADDER P2: THE KERR ERGOSPHERE ENGINE - MTE base+14 (28114), UIV
 * (singularity-ladder-build.md §3.1 / §3.2, D-41, D-42, D-44). Owned by the Kerr unit.
 * <p>
 * Kerr T1 takes the Black Hole Compressor's slot: the BHC's crafting recipe is removed at
 * load-complete ({@code BTSRecipeRemovals}, config {@code removeBlackHoleCompressorRecipe}), placed
 * BHCs keep working. The Kerr does nothing on its own: it needs a <b>compressor module</b> and/or an
 * <b>extractor module</b>, each an OPTIONAL CHECKED PIECE of this one controller (the D-20 pattern,
 * Q-10), and both can stand at once. The compressor runs GT's compressor and neutronium-compressor
 * maps (the BHC's); the extractor runs GT's item extractor map only (user ruling, Q-9/Q-22). More
 * modules come later: each is one {@link KerrModule} (its piece, core block and private I/O), see
 * that class for the steps. Kerr T2 is the same controller with the <b>Kerr Density
 * Upgrade</b> installed: the required shape becomes {@code kerr_t2}, the setpoint range rises to
 * 512 SM and DENSIFY (Hyperdense Stellar Mixture) unlocks.
 *
 * <h2>Shapes</h2>
 * Four resource pieces exported from {@code designs/voxel.js} by {@code tools/export-structures.js},
 * loaded by {@link BTSShapes}: {@code kerr_t1} / {@code kerr_t2} (alternatives; exactly one is
 * required), {@code kerr_compressor} and {@code kerr_extractor}. Each piece is checked at its own
 * {@code off=}. The char-to-block mapping follows the "Structure export" legend and the mid-run
 * ruling: Black Hole Compressor blocks wherever a role fits, plus one new white lining block
 * (Harmonic Lining, slCasing2:2, char {@code J}) for the mockup's bright inner face.
 *
 * <h2>The hole (Q-8, numbers PROPOSED)</h2>
 * A Black Hole Seed in any bolt bus opens a hole at 8 SM; a Black Hole Collapser closes it. Every
 * tick the hole bleeds {@code 3.2 / M^2} SM (Hawking) and the governor fires bolts from the bolt
 * buses to hold the setpoint, at most {@code cap} bolts a tick where the cap is set by the LOWEST
 * injector-rail tier in the cage (Mk I 4, Mk II 8, Mk III 16). Below 2 SM the hole evaporates.
 * Mass buys parallel ({@code 8 x voltage tier x floor(sqrt(M))}); heat buys speed (recipe time
 * divided by {@code clamp(E / 50, 1, 4)} with {@code E = 3200 / M} GeV).
 *
 * <h2>The four traps</h2>
 * <ol>
 * <li><b>Client vs server.</b> {@link #construct} picks T1 or T2 from the trigger stack size only (1 =
 * T1, 2+ = T2) and always projects both modules; it never reads {@link #densityInstalled}, which is
 * 0 on a client that has not synced it. The walk fields are server-side; nothing client-side reads
 * them except the GUI's own synced values.</li>
 * <li><b>Tile vs item NBT.</b> {@link #writeKerrState} is the ONE serializer of the persisted state
 * (density, hole, setpoint), called from both {@link #saveNBTData} and {@link #setItemNBT}, so a
 * wrenched Kerr keeps its upgrade and its hole.</li>
 * <li><b>Plant-wide hatch lists.</b> Bolt buses, utility hatches and every module socket register into
 * PRIVATE lists ({@link KerrHatchElement}); only the active mode's module is bound into GT's
 * standard lists, by {@link #bindModuleIO}.</li>
 * <li><b>Element chains.</b> Every socket is {@code buildHatchAdder(...).buildAndChain(casing)}: the
 * hatch adder comes first, so the hologram hints the hatch.</li>
 * </ol>
 * All walk state is zeroed in {@link #resetWalk()}, the first statement of {@link #checkMachine},
 * so an early return cannot leave a module asserting it stands.
 */
public class MTEKerrEngine extends MTEExtendedPowerMultiBlockBase<MTEKerrEngine> implements ISurvivalConstructable {

    // ------------------------------------------------------------------------------- pieces
    public static final String PIECE_T1 = "kerr_t1";
    public static final String PIECE_T2 = "kerr_t2";
    public static final String PIECE_COMPRESSOR = "kerr_compressor";
    public static final String PIECE_EXTRACTOR = "kerr_extractor";

    // -------------------------------------------------------------------------------- modes
    public static final int MODE_COMPRESS = 0;
    public static final int MODE_EXTRACT = 1;
    public static final int MODE_DENSIFY = 2;
    private static final int MODE_COUNT = 3;
    private static final String[] MODE_NAMES = { "Compress", "Extract", "Densify" };

    // --------------------------------------------------------------- the hole (Q-8, PROPOSED)
    /** Mass a fresh hole opens at, SM. */
    public static final double OPEN_MASS = 8.0;
    /** Below this the hole evaporates, SM. */
    public static final double MASS_FLOOR = 2.0;
    /** Hawking bleed: {@code BLEED_K / M^2} SM per tick. */
    public static final double BLEED_K = 3.2;
    /** DENSIFY needs at least this much mass in the hole, SM. */
    public static final double DENSIFY_MIN_MASS = 128.0;
    /** Particle energy {@code E = ENERGY_K / M} GeV; speed = clamp(E / SPEED_DIVISOR, 1, MAX_SPEED). */
    public static final double ENERGY_K = 3200.0;
    public static final double SPEED_DIVISOR = 50.0;
    public static final double MAX_SPEED = 4.0;
    /** Setpoint ladder. The first {@link #T1_SETPOINTS} entries are open at T1; T2 opens the rest. */
    public static final double[] SETPOINTS = { 2, 4, 8, 16, 32, 64, 128, 256, 512 };
    private static final int T1_SETPOINTS = 6;
    /** Bolts per tick by the LOWEST injector-rail tier found in the walk (index = tier, Mk I = 1). */
    public static final int[] RAIL_CAP = { 0, 4, 8, 16 };
    /** Base parallel per voltage tier, the Black Hole Compressor's figure. */
    public static final int PARALLEL_PER_TIER = 8;

    /** Bolt materials and their masses in SM, lightest first. */
    private static final Materials[] BOLT_MATERIALS = { Materials.Neutronium, Materials.CosmicNeutronium,
        Materials.Infinity, Materials.TranscendentMetal, Materials.SpaceTime };
    private static final double[] BOLT_MASSES = { 1.0 / 64, 1.0 / 32, 1.0 / 16, 1.0 / 8, 1.0 / 4 };
    /** Resolved lazily (GT's items exist only after its preInit): {item, meta} per bolt material. */
    private static ItemStack[] boltStacks;

    // ------------------------------------------------------------------------------ textures
    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_ACTIVE_GLOW");

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread at load. */
    private static volatile IStructureDefinition<MTEKerrEngine> STRUCTURE_DEFINITION = null;

    /** Periodic structure re-check intervals in ticks (see structureChunksLoaded). */
    private static final int RECHECK_IDLE = 200;
    private static final int RECHECK_RUNNING = 1000;
    /** Horizontal reach that covers every Kerr piece from the controller (kerr_t2 is 92 x 89). */
    private static final int RECHECK_RADIUS = 96;

    // ---------------------------------------------------- PERSISTED (writeKerrState is the one writer)
    private boolean densityInstalled;
    private boolean holeOpen;
    private double mass;
    private double setpoint = OPEN_MASS;

    // ---------------------------------------------------------- WALK state (server, resetWalk)
    private int mCasing;
    private int railTierMin;
    private int railCount;
    private final ArrayList<MTEHatchInputBus> boltBuses = new ArrayList<>();
    private final ArrayList<MTEBlackHoleUtility> utilityHatches = new ArrayList<>();

    // ------------------------------------------------------- the modules (see KerrModule)
    /** South arc: GT's compressor + neutronium-compressor recipes, and DENSIFY at T2. Items and fluids. */
    private final KerrModule compressor = new KerrModule(
        "Compressor",
        PIECE_COMPRESSOR,
        'q',
        BlockSLCasing2.COMPRESSOR_MODULE,
        true);
    /** East arc: GT's item extractor recipes ({@code extractorRecipes}) only. Item buses only. */
    private final KerrModule extractor = new KerrModule(
        "Extractor",
        PIECE_EXTRACTOR,
        'k',
        BlockSLCasing2.EXTRACTOR_MODULE,
        false);
    /** Every module, in walk order. A new module is one more entry here (see KerrModule). */
    private final KerrModule[] modules = { compressor, extractor };

    // -------------------------------------------------------------------- RUN state (server)
    /** The mode whose module I/O is bound into GT's lists (the running recipe's mode). */
    private int boundMode = -1;
    private int lastBoltsFired;
    private double lastBleed;

    public MTEKerrEngine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEKerrEngine(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEKerrEngine(this.mName);
    }

    // =================================================================================== shape

    private static int[] off(String piece) {
        return BTSShapes.offset(piece);
    }

    @Override
    public IStructureDefinition<MTEKerrEngine> getStructureDefinition() {
        IStructureDefinition<MTEKerrEngine> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEKerrEngine.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTEKerrEngine> buildDefinition() {
        final Block sl = SLBlocks.slCasing;
        final Block sl2 = SLBlocks.slCasing2;
        final int shellIndex = Casings.ExtremeDensitySpaceBendingCasing.textureId;
        final int bhcIndex = Casings.BackgroundRadiationAbsorbentCasing.textureId;
        return StructureDefinition.<MTEKerrEngine>builder()
            .addShape(PIECE_T1, BTSShapes.load(PIECE_T1))
            .addShape(PIECE_T2, BTSShapes.load(PIECE_T2))
            .addShape(PIECE_COMPRESSOR, BTSShapes.load(PIECE_COMPRESSOR))
            .addShape(PIECE_EXTRACTOR, BTSShapes.load(PIECE_EXTRACTOR))
            // ---- the cage (BHC family where a BHC block fits: the mid-run ruling)
            .addElement('A', casing(Casings.ExtremeDensitySpaceBendingCasing))
            .addElement('B', noOverwrite(onElementPass(x -> ++x.mCasing, ofFrame(Materials.NaquadahAlloy))))
            .addElement('C', block(sl, BlockSLCasing.MIRROR))
            .addElement('D', block(sl, BlockSLCasing.MIRROR_BACKING))
            .addElement('E', rails(sl))
            .addElement('F', block(sl, BlockSLCasing.QUADRUPOLE))
            .addElement('G', casing(Casings.ParticleBeamGuidancePipeCasing))
            .addElement('H', block(sl, BlockSLCasing.FIELD_COIL))
            .addElement('I', casing(Casings.BackgroundRadiationAbsorbentCasing))
            // the white inner face of every arc and band: the one new Kerr lining block (round-4 ruling)
            .addElement('J', block(sl2, BlockSLCasing2.LINING))
            .addElement('K', casing(Casings.HawkingRadiationRealignmentFocus))
            .addElement('L', casing(Casings.ExtremeDensitySpaceBendingCasing))
            .addElement('M', block(sl, BlockSLCasing.BEAM_DUMP))
            .addElement('N', block(sl, BlockSLCasing.EXTRACTION))
            .addElement('O', block(sl, BlockSLCasing.BASTION))
            .addElement('P', block(sl, BlockSLCasing.CASIMIR))
            // ---- cage sockets
            .addElement(
                'e',
                buildHatchAdder(MTEKerrEngine.class).atLeast(Energy, ExoticEnergy)
                    .casingIndex(shellIndex)
                    .hint(1)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'm',
                buildHatchAdder(MTEKerrEngine.class).atLeast(Maintenance)
                    .casingIndex(shellIndex)
                    .hint(2)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'b',
                buildHatchAdder(MTEKerrEngine.class).atLeast(KerrHatchElement.BoltBus)
                    .casingIndex(shellIndex)
                    .hint(3)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'u',
                buildHatchAdder(MTEKerrEngine.class).atLeast(KerrHatchElement.Utility)
                    .casingIndex(shellIndex)
                    .hint(6)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            // ---- compressor module (south arc)
            .addElement(
                'c',
                buildHatchAdder(MTEKerrEngine.class)
                    .atLeast(KerrHatchElement.CompressorInput, KerrHatchElement.CompressorOutput)
                    .casingIndex(bhcIndex)
                    .hint(4)
                    .buildAndChain(casing(Casings.BackgroundRadiationAbsorbentCasing)))
            .addElement(
                'q',
                noOverwrite(onElementPass(x -> ++x.compressor.cores, ofBlock(sl2, BlockSLCasing2.COMPRESSOR_MODULE))))
            // ---- extractor module (east arc): item buses only, GT's extractor recipes
            .addElement(
                'x',
                buildHatchAdder(MTEKerrEngine.class)
                    .atLeast(KerrHatchElement.ExtractorInput, KerrHatchElement.ExtractorOutput)
                    .casingIndex(shellIndex)
                    .hint(5)
                    .buildAndChain(block(sl, BlockSLCasing.EXTRACTION)))
            .addElement(
                'k',
                noOverwrite(onElementPass(x -> ++x.extractor.cores, ofBlock(sl2, BlockSLCasing2.EXTRACTOR_MODULE))))
            .build();
    }

    private static IStructureElement<MTEKerrEngine> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEKerrEngine> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /** The injector rail metas, Mk I first. */
    private static final int[] RAIL_METAS = { BlockSLCasing.INJECTOR_MK1, BlockSLCasing.INJECTOR_MK2,
        BlockSLCasing.INJECTOR_MK3 };

    /**
     * The injector rails, Mk I/II/III, MIXABLE: unlike {@code ofBlocksTiered} (which demands one tier
     * across the whole structure) the walk records the LOWEST tier it meets, and that sets the bolt cap
     * - so upgrading rails one at a time never un-forms the machine. Builds place Mk I (survival takes
     * a higher Mk if the player has no Mk I) and never downgrade a rail already standing
     * ({@code BTSStructures.tieredBlock}).
     */
    private static IStructureElement<MTEKerrEngine> rails(Block sl) {
        return tieredBlock(
            sl,
            RAIL_METAS,
            ofChain(
                onElementPass(x -> x.noteRail(1), ofBlock(sl, BlockSLCasing.INJECTOR_MK1)),
                onElementPass(x -> x.noteRail(2), ofBlock(sl, BlockSLCasing.INJECTOR_MK2)),
                onElementPass(x -> x.noteRail(3), ofBlock(sl, BlockSLCasing.INJECTOR_MK3))),
            trigger -> 0);
    }

    private void noteRail(int tier) {
        railCount++;
        railTierMin = railTierMin == 0 ? tier : Math.min(railTierMin, tier);
    }

    /** Horizontal facings only: the cage is 65 blocks tall and built upright. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    /** Every field a walk writes, zeroed in one place that nothing can get past. */
    private void resetWalk() {
        mCasing = 0;
        railTierMin = 0;
        railCount = 0;
        boltBuses.clear();
        utilityHatches.clear();
        for (KerrModule module : modules) {
            module.resetWalk();
        }
    }

    /**
     * The core piece this controller must stand on right now (server: the installed state). Once the
     * Density Upgrade is in, only {@code kerr_t2} forms: with the reinforcement torn down the Kerr does
     * NOT fall back to T1, it stays unformed (user ruling 2026-10-12, round 4: "won't form").
     */
    public String corePiece() {
        return densityInstalled ? PIECE_T2 : PIECE_T1;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final String core = corePiece();
        final int[] o = off(core);
        if (!checkPiece(core, o[0], o[1], o[2], errors)) {
            bindModuleIO(currentBinding());
            return;
        }
        checkExoticAndNormalEnergyHatches(errors);
        checkHasMaintenanceHatch(errors);
        if (boltBusCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.kerr.boltbus"));
        }

        // The OPTIONAL modules, each walked on its own: nothing they report un-forms the core.
        for (KerrModule module : modules) {
            walkModule(module);
        }

        bindModuleIO(currentBinding());
    }

    /**
     * Walks one optional module arc with its own error list. Its sockets live in the module's
     * private lists, cleared again if the walk fails (mark-and-truncate), so a half-built arc leaves
     * nothing registered.
     */
    private void walkModule(KerrModule module) {
        final List<StructureError> pieceErrors = new ArrayList<>();
        final int[] o = off(module.piece);
        boolean built = checkPiece(module.piece, o[0], o[1], o[2], pieceErrors);
        if (built && module.cores != 1) {
            built = false;
            pieceErrors.add(StructureErrors.of("beyondthestars.structure.kerr.modulecore"));
        }
        if (built && module.inputCount() == 0) {
            built = false;
            pieceErrors.add(StructureErrors.of("beyondthestars.structure.kerr.moduleinput"));
        }
        if (built && module.outputCount() == 0) {
            built = false;
            pieceErrors.add(StructureErrors.of("beyondthestars.structure.kerr.moduleoutput"));
        }
        module.built = built;
        if (!built) {
            module.clearIO();
            module.fault = moduleFault(pieceErrors, module);
        }
    }

    /**
     * Why an optional module did not form, or "" when it simply is not there. A module whose core
     * block is not standing is a CHOICE, and reporting the walk's complaint about empty air would be
     * noise; a module whose core stands and still fails is a FAULT and is reported.
     */
    private String moduleFault(List<StructureError> walkErrors, KerrModule module) {
        if (walkErrors.isEmpty()) return "";
        if (!coreStanding(module)) return "";
        final String reason = walkErrors.get(0)
            .getDisplayString();
        return reason == null || reason.isEmpty() ? "structure incomplete" : reason;
    }

    /** True when the module's core block stands in its cell (located through the manifest). */
    private boolean coreStanding(KerrModule module) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return false;
        final int[] rel = socketRel(module.piece, module.coreChar, 0);
        if (rel == null) return true; // manifest unreadable: report rather than hide
        final int[] xyz = new int[3];
        getExtendedFacing().getWorldOffset(rel, xyz);
        final int x = base.getXCoord() + xyz[0];
        final int y = base.getYCoord() + xyz[1];
        final int z = base.getZCoord() + xyz[2];
        if (!base.getWorld()
            .blockExists(x, y, z)) return false;
        return base.getWorld()
            .getBlock(x, y, z) == SLBlocks.slCasing2
            && base.getWorld()
                .getBlockMetadata(x, y, z) == module.coreMeta;
    }

    /**
     * A socket's offset from the controller in StructureLib a/b/c ({@code sockets.<ch>[i].rel} in
     * {@code manifest.json}), or null if the manifest does not list it.
     */
    public static int[] socketRel(String piece, char ch, int index) {
        final int[][] all = socketRels(piece, ch);
        return all.length > index ? all[index] : null;
    }

    /** Every socket of one char in a piece, as a/b/c offsets from the controller. Empty if none. */
    public static int[][] socketRels(String piece, char ch) {
        try {
            final JsonObject entry = BTSShapes.manifestPiece(piece);
            if (entry == null || !entry.has("sockets")) return new int[0][];
            final JsonObject sockets = entry.getAsJsonObject("sockets");
            final String key = String.valueOf(ch);
            if (!sockets.has(key)) return new int[0][];
            final JsonArray list = sockets.getAsJsonArray(key);
            final int[][] out = new int[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                final JsonArray rel = list.get(i)
                    .getAsJsonObject()
                    .getAsJsonArray("rel");
                out[i] = new int[] { rel.get(0)
                    .getAsInt(),
                    rel.get(1)
                        .getAsInt(),
                    rel.get(2)
                        .getAsInt() };
            }
            return out;
        } catch (RuntimeException bad) {
            return new int[0][];
        }
    }

    // ===================================================================== private hatch adders

    private static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    public boolean addBoltBus(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        // An ME stocking bus shows shadow stacks the governor cannot decrement; plain buses only.
        if (mte instanceof MTEHatchInputBus && !(mte instanceof MTEHatchInputBusME)) {
            ((MTEHatchInputBus) mte).updateTexture(casingIndex);
            boltBuses.add((MTEHatchInputBus) mte);
            return true;
        }
        return false;
    }

    public boolean addUtilityHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEBlackHoleUtility) {
            ((MTEBlackHoleUtility) mte).updateTexture(casingIndex);
            utilityHatches.add((MTEBlackHoleUtility) mte);
            return true;
        }
        return false;
    }

    public boolean addCompressorInput(IGregTechTileEntity te, int casingIndex) {
        return compressor.addInput(te, casingIndex);
    }

    public boolean addCompressorOutput(IGregTechTileEntity te, int casingIndex) {
        return compressor.addOutput(te, casingIndex);
    }

    public boolean addExtractorInput(IGregTechTileEntity te, int casingIndex) {
        return extractor.addInput(te, casingIndex);
    }

    public boolean addExtractorOutput(IGregTechTileEntity te, int casingIndex) {
        return extractor.addOutput(te, casingIndex);
    }

    static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity mte : list) {
            if (live(mte)) n++;
        }
        return n;
    }

    public int boltBusCount() {
        return liveCount(boltBuses);
    }

    public int utilityHatchCount() {
        return liveCount(utilityHatches);
    }

    public int compressorInputCount() {
        return compressor.inputCount();
    }

    public int compressorOutputCount() {
        return compressor.outputCount();
    }

    public int extractorInputCount() {
        return extractor.inputCount();
    }

    public int extractorOutputCount() {
        return extractor.outputCount();
    }

    /** The module a mode runs on, or null for an unknown mode. A new mode adds one case here. */
    private KerrModule moduleFor(int mode) {
        switch (mode) {
            case MODE_COMPRESS:
            case MODE_DENSIFY:
                return compressor;
            case MODE_EXTRACT:
                return extractor;
            default:
                return null;
        }
    }

    private boolean anyModuleBuilt() {
        for (KerrModule module : modules) {
            if (module.built) return true;
        }
        return false;
    }

    /**
     * Binds ONE module's sockets into GT's standard recipe lists. GT's processing reads
     * {@code mInputBusses} / {@code mInputHatches} and writes {@code mOutputBusses} /
     * {@code mOutputHatches}; binding only the mode's own module keeps the compressor's inputs out
     * of extraction and vice versa, and keeps the bolt buses out of everything.
     */
    private void bindModuleIO(int mode) {
        mInputBusses.clear();
        mInputHatches.clear();
        mOutputBusses.clear();
        mOutputHatches.clear();
        boundMode = mode;
        final KerrModule module = moduleFor(mode);
        if (module != null && module.built) {
            mInputBusses.addAll(module.inBuses);
            mInputHatches.addAll(module.inHatches);
            mOutputBusses.addAll(module.outBuses);
            mOutputHatches.addAll(module.outHatches);
        }
    }

    /** A running recipe keeps the module it started on; an idle machine binds the selected mode. */
    private int currentBinding() {
        return mMaxProgresstime > 0 && boundMode >= 0 ? boundMode : machineMode;
    }

    /**
     * GT brackets every recipe attempt with start/endRecipeProcessing over mInputBusses and
     * mInputHatches (ME stocking hatches snapshot and commit there), so the selected mode's module
     * must be bound BEFORE the bracket opens, never inside checkProcessing: binding there would end
     * the bracket on a different module's hatches than it started on. Only checkRecipe calls this,
     * and only while idle, so the binding is the selected mode.
     */
    @Override
    public void startRecipeProcessing() {
        bindModuleIO(currentBinding());
        super.startRecipeProcessing();
    }

    // ==================================================================== construct / survival

    /** Trap 1: the trigger stack alone picks the core (1 = T1, 2+ = T2); both modules always. */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final String core = stackSize != null && stackSize.stackSize >= 2 ? PIECE_T2 : PIECE_T1;
        final int[] o = off(core);
        buildPiece(core, stackSize, hintsOnly, o[0], o[1], o[2]);
        for (KerrModule module : modules) {
            final int[] mo = off(module.piece);
            buildPiece(module.piece, stackSize, hintsOnly, mo[0], mo[1], mo[2]);
        }
    }

    /**
     * Survival auto-place (projector, from the player's inventory or a linked AE system, and NEI's
     * preview, which builds through here with a creative item source). Server-side only, so the
     * installed state may be read here: an upgraded Kerr builds T2, and a stack of 2+ pre-builds the
     * T2 reinforcement before the upgrade goes in. The core goes first; once it is complete every
     * module arc is built, each on its own (an unfinished module never blocks the next). -1 when
     * everything is standing. Hatches and other tiles are never overwritten ({@code noOverwrite}).
     */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final String core = densityInstalled || (stackSize != null && stackSize.stackSize >= 2) ? PIECE_T2 : PIECE_T1;
        if (mMachine && core.equals(corePiece()) && allModulesBuilt()) return -1;
        final int budget = BTSStructures.survivalBudget(elementBudget);
        final int[] o = off(core);
        final int built = survivalBuildPiece(core, stackSize, o[0], o[1], o[2], budget, env, false, true);
        if (built >= 0) return built;
        int placed = 0;
        boolean any = false;
        for (KerrModule module : modules) {
            if (placed >= budget) return placed;
            final int[] mo = off(module.piece);
            final int n = survivalBuildPiece(
                module.piece,
                stackSize,
                mo[0],
                mo[1],
                mo[2],
                budget - placed,
                env,
                false,
                true);
            if (n >= 0) {
                any = true;
                placed += n;
            }
        }
        return any ? placed : -1;
    }

    private boolean allModulesBuilt() {
        for (KerrModule module : modules) {
            if (!module.built) return false;
        }
        return true;
    }

    // ================================================================================= modes

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public void setMachineModeIcons() {
        machineModeIcons.clear();
        machineModeIcons.add(GTUITextures.OVERLAY_BUTTON_MACHINEMODE_COMPRESSING);
        machineModeIcons.add(GTUITextures.OVERLAY_BUTTON_MACHINEMODE_SEPARATOR);
        machineModeIcons.add(GTUITextures.OVERLAY_BUTTON_MACHINEMODE_SINGULARITY);
    }

    /** True when the mode's module stands (and, for DENSIFY, the Density Upgrade is installed). */
    public boolean modeAvailable(int mode) {
        final KerrModule module = moduleFor(mode);
        if (module == null || !module.built) return false;
        return mode != MODE_DENSIFY || densityInstalled;
    }

    /**
     * The next AVAILABLE mode. The walk fields are server-side, so a client asking (the GUI button)
     * sees nothing available and gets a plain cycle; the server re-checks every mode at recipe time.
     */
    @Override
    public int nextMachineMode() {
        for (int step = 1; step <= MODE_COUNT; step++) {
            final int candidate = (machineMode + step) % MODE_COUNT;
            if (modeAvailable(candidate)) return candidate;
        }
        return (machineMode + 1) % MODE_COUNT;
    }

    public static String modeName(int mode) {
        return mode >= 0 && mode < MODE_COUNT ? MODE_NAMES[mode] : "?";
    }

    /**
     * Screwdriver: plain = step the hole's mass SETPOINT; sneaking = cycle the machine MODE (the GUI
     * button cycles the mode too). Single-recipe locking is not offered.
     */
    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (aPlayer.isSneaking()) {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatToPlayer(
                aPlayer,
                "Kerr mode: " + EnumChatFormatting.AQUA
                    + modeName(machineMode)
                    + EnumChatFormatting.RESET
                    + (modeAvailable(machineMode) ? ""
                        : EnumChatFormatting.RED + "  (needs " + needsFor(machineMode) + ")"));
            return;
        }
        final int open = densityInstalled ? SETPOINTS.length : T1_SETPOINTS;
        int index = 0;
        for (int i = 0; i < open; i++) {
            if (SETPOINTS[i] <= setpoint + 1e-9) index = i;
        }
        setpoint = SETPOINTS[(index + 1) % open];
        GTUtility.sendChatToPlayer(
            aPlayer,
            "Kerr hole setpoint: " + EnumChatFormatting.AQUA
                + fmt(setpoint)
                + " SM"
                + EnumChatFormatting.RESET
                + (densityInstalled ? "" : "  (T1 tops out at 64 SM; the Density Upgrade opens 128-512)"));
    }

    private static String needsFor(int mode) {
        switch (mode) {
            case MODE_COMPRESS:
                return "the compressor module";
            case MODE_EXTRACT:
                return "the extractor module";
            default:
                return "the compressor module and the Kerr Density Upgrade";
        }
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    // ============================================================================ the upgrade

    /**
     * Right-click holding a Kerr Density Upgrade: consumes one (not in creative), installs Kerr T2
     * and re-checks the structure. Irreversible (Q-10). A plain right-click, not sneak: Minecraft
     * never activates a block for a sneaking player holding an item.
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        final ItemStack held = aPlayer.getHeldItem();
        if (held != null && SLItems.kerrDensityUpgrade != null && held.getItem() == SLItems.kerrDensityUpgrade) {
            if (!aBaseMetaTileEntity.isServerSide()) return true;
            if (densityInstalled) {
                GTUtility.sendChatToPlayer(aPlayer, "This Kerr already carries the Density Upgrade.");
                return true;
            }
            if (!aPlayer.capabilities.isCreativeMode) {
                held.stackSize--;
                if (held.stackSize <= 0) {
                    aPlayer.inventory.setInventorySlotContents(aPlayer.inventory.currentItem, null);
                }
            }
            installDensity();
            checkStructure(true, aBaseMetaTileEntity);
            GTUtility.sendChatToPlayer(
                aPlayer,
                EnumChatFormatting.AQUA + "Kerr Density Upgrade installed."
                    + EnumChatFormatting.RESET
                    + " The cage now needs the T2 reinforcement (kerr_t2); setpoints up to 512 SM and DENSIFY are open."
                    + (mMachine ? "" : EnumChatFormatting.YELLOW + " Not formed yet: build the reinforcement."));
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer);
    }

    /** Installs Kerr T2. Also the creative build's hook. */
    public void installDensity() {
        densityInstalled = true;
    }

    public boolean isDensityInstalled() {
        return densityInstalled;
    }

    // =============================================================================== the hole

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        if (aTick > 0 && aTick % (mMaxProgresstime > 0 ? RECHECK_RUNNING : RECHECK_IDLE) == 0
            && structureChunksLoaded(aBaseMetaTileEntity)) {
            checkStructure(true, aBaseMetaTileEntity);
        }
        if (!mMachine) return;
        if (aTick % 20 == 0) {
            handleSeeds();
            for (MTEBlackHoleUtility hatch : utilityHatches) {
                if (live(hatch)) hatch.updateRedstoneOutput(holeOpen);
            }
        }
        tickHole();
    }

    /**
     * The cage, the arcs and the modules are hundreds of separate islands as far as GT's
     * block-update walk is concerned, so most edits never reach the controller. A periodic walk
     * catches them (a built or broken module, swapped rails, the last hologram block). It is skipped
     * while any chunk the structure could touch is unloaded, so a half-loaded cage never un-forms.
     */
    private static boolean structureChunksLoaded(IGregTechTileEntity base) {
        final World world = base.getWorld();
        final int x = base.getXCoord();
        final int z = base.getZCoord();
        return world != null && world
            .checkChunksExist(x - RECHECK_RADIUS, 0, z - RECHECK_RADIUS, x + RECHECK_RADIUS, 255, z + RECHECK_RADIUS);
    }

    /** Hawking bleed, then the governor. Runs every tick while the machine is formed. */
    private void tickHole() {
        if (!holeOpen) {
            lastBoltsFired = 0;
            lastBleed = 0;
            return;
        }
        lastBleed = BLEED_K / (mass * mass);
        mass -= lastBleed;
        final double deficit = setpoint - mass;
        lastBoltsFired = deficit > 0 ? fireBolts(deficit, boltCap()) : 0;
        if (mass < MASS_FLOOR) {
            holeOpen = false;
            mass = 0;
        }
    }

    /** Bolts per tick the cage's rails allow (the lowest rail tier sets it). */
    public int boltCap() {
        return RAIL_CAP[Math.max(0, Math.min(RAIL_CAP.length - 1, railTierMin))];
    }

    /** Fires up to {@code cap} bolts out of the bolt buses to cover {@code deficit} SM. */
    private int fireBolts(double deficit, int cap) {
        int fired = 0;
        double left = deficit;
        for (MTEHatchInputBus bus : boltBuses) {
            if (fired >= cap || left <= 0) break;
            if (!live(bus)) continue;
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            final int size = inv.getSizeInventory();
            for (int slot = 0; slot < size && fired < cap && left > 0; slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0) continue;
                final double boltMass = boltMass(stack);
                if (boltMass <= 0) continue;
                final int want = (int) Math.ceil(left / boltMass);
                final int n = Math.min(stack.stackSize, Math.min(cap - fired, want));
                if (n <= 0) continue;
                inv.decrStackSize(slot, n);
                fired += n;
                mass += n * boltMass;
                left -= n * boltMass;
            }
        }
        return fired;
    }

    /** The mass of one bolt in SM, or 0 if the stack is not a governor bolt. */
    public static double boltMass(ItemStack stack) {
        if (stack == null) return 0;
        ItemStack[] table = boltStacks;
        if (table == null) {
            table = new ItemStack[BOLT_MATERIALS.length];
            for (int i = 0; i < BOLT_MATERIALS.length; i++) {
                table[i] = GTOreDictUnificator.get(OrePrefixes.bolt, BOLT_MATERIALS[i], 1L);
            }
            boltStacks = table;
        }
        for (int i = 0; i < table.length; i++) {
            final ItemStack bolt = table[i];
            if (bolt != null && bolt.getItem() == stack.getItem() && bolt.getItemDamage() == stack.getItemDamage()) {
                return BOLT_MASSES[i];
            }
        }
        return 0;
    }

    /** A Black Hole Seed opens a closed hole; a Black Hole Collapser closes an open one. */
    private void handleSeeds() {
        final ItemStack wanted = holeOpen ? ItemList.Black_Hole_Closer.get(1L) : ItemList.Black_Hole_Opener.get(1L);
        if (wanted == null) return;
        for (MTEHatchInputBus bus : boltBuses) {
            if (!live(bus)) continue;
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null || !GTUtility.areStacksEqual(stack, wanted, true)) continue;
                inv.decrStackSize(slot, 1);
                if (holeOpen) {
                    holeOpen = false;
                    mass = 0;
                } else {
                    holeOpen = true;
                    mass = OPEN_MASS;
                }
                return;
            }
        }
    }

    public boolean isHoleOpen() {
        return holeOpen;
    }

    public double getMass() {
        return mass;
    }

    /** Debug/creative: open the hole at {@code m} SM without a seed. */
    public void debugOpenHole(double m) {
        holeOpen = true;
        mass = Math.max(MASS_FLOOR, m);
        setpoint = clampSetpoint(mass);
    }

    /** Debug/creative: set the setpoint to the highest legal rung at or below {@code wanted}. */
    public void debugSetpoint(double wanted) {
        setpoint = clampSetpoint(wanted);
    }

    private double clampSetpoint(double wanted) {
        final int open = densityInstalled ? SETPOINTS.length : T1_SETPOINTS;
        double best = SETPOINTS[0];
        for (int i = 0; i < open; i++) {
            if (SETPOINTS[i] <= wanted + 1e-9) best = SETPOINTS[i];
        }
        return best;
    }

    /** Heat buys speed: {@code clamp((3200 / M) / 50, 1, 4)} while the hole is open, else 1. */
    public double speedFactor() {
        if (!holeOpen || mass <= 0) return 1.0;
        final double energy = ENERGY_K / mass;
        return Math.max(1.0, Math.min(MAX_SPEED, energy / SPEED_DIVISOR));
    }

    /** Mass buys parallel: {@code 8 x voltage tier x floor(sqrt(M))}; closed: {@code 8 x tier}. */
    @Override
    public int getMaxParallelRecipes() {
        final int base = PARALLEL_PER_TIER * Math.max(1, GTUtility.getTierExtended(getMaxInputEu()));
        if (!holeOpen) return base;
        return base * Math.max(1, (int) Math.floor(Math.sqrt(mass)));
    }

    // ============================================================================ processing

    /**
     * The mode's recipe map. EXTRACT runs GT's own item extractor map ({@code extractorRecipes}) and
     * nothing else: no fluid extraction and no Kerr-only map (user ruling, Q-9/Q-22).
     */
    @Override
    public RecipeMap<?> getRecipeMap() {
        switch (machineMode) {
            case MODE_EXTRACT:
                return RecipeMaps.extractorRecipes;
            case MODE_DENSIFY:
                return KerrRecipeMaps.DENSITY;
            default:
                return RecipeMaps.compressorRecipes;
        }
    }

    /** Every map any mode runs; NEI's catalyst index reads this, so a new module's map goes here. */
    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.<RecipeMap<?>>asList(
            RecipeMaps.compressorRecipes,
            RecipeMaps.neutroniumCompressorRecipes,
            RecipeMaps.extractorRecipes,
            KerrRecipeMaps.DENSITY);
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        if (!anyModuleBuilt()) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.nomodule");
        }
        if (!modeAvailable(machineMode)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.modeunavailable");
        }
        if (machineMode == MODE_DENSIFY && (!holeOpen || mass < DENSIFY_MIN_MASS)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.lowmass");
        }
        // The module's I/O was bound in startRecipeProcessing, before GT opened the processing bracket.
        return super.checkProcessing();
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            /** Mode and hole state the cached recipe was found under; a change drops the cache. */
            private int lastKey = -1;

            @Nonnull
            @Override
            protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
                final int key = machineMode * 2 + (holeOpen ? 1 : 0);
                if (key != lastKey) {
                    lastRecipe = null;
                    lastKey = key;
                }
                switch (machineMode) {
                    case MODE_EXTRACT:
                        return super.findRecipeMatches(RecipeMaps.extractorRecipes);
                    case MODE_DENSIFY:
                        return super.findRecipeMatches(KerrRecipeMaps.DENSITY);
                    default: {
                        final Stream<GTRecipe> plain = super.findRecipeMatches(RecipeMaps.compressorRecipes);
                        return holeOpen
                            ? Stream.concat(plain, super.findRecipeMatches(RecipeMaps.neutroniumCompressorRecipes))
                            : plain;
                    }
                }
            }

            /** Heated (HIP) and black-hole compressor recipes need the hole open; plain ones never do. */
            @Nonnull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (!holeOpen && recipe.getMetadataOrDefault(GTRecipeConstants.COMPRESSION_TIER, 0) > 0) {
                    return CheckRecipeResultRegistry.NO_BLACK_HOLE;
                }
                return super.validateRecipe(recipe);
            }
        }.setMaxParallelSupplier(this::getMaxParallelRecipes)
            .setSpeedBonusSupplier(() -> 1.0 / speedFactor());
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    // ===================================================================================== NBT

    /** The ONE serializer of the persisted state (trap 2): tile NBT and the wrenched item's NBT. */
    private void writeKerrState(NBTTagCompound nbt) {
        if (densityInstalled) nbt.setBoolean("btsKerrDensity", true);
        if (holeOpen) {
            nbt.setBoolean("btsKerrHoleOpen", true);
            nbt.setDouble("btsKerrMass", mass);
        }
        if (setpoint != OPEN_MASS) nbt.setDouble("btsKerrSetpoint", setpoint);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeKerrState(aNBT);
        // Tile only (a wrenched machine carries no running recipe): the module a running recipe started
        // on, so a reload mid-run still sends its outputs there even if the mode was switched since.
        if (mMaxProgresstime > 0 && boundMode >= 0) aNBT.setInteger("btsKerrBoundMode", boundMode);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeKerrState(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        densityInstalled = aNBT.getBoolean("btsKerrDensity");
        holeOpen = aNBT.getBoolean("btsKerrHoleOpen");
        mass = holeOpen ? Math.max(MASS_FLOOR, aNBT.getDouble("btsKerrMass")) : 0;
        setpoint = aNBT.hasKey("btsKerrSetpoint") ? clampSetpoint(aNBT.getDouble("btsKerrSetpoint")) : OPEN_MASS;
        boundMode = aNBT.hasKey("btsKerrBoundMode") ? aNBT.getInteger("btsKerrBoundMode") : -1;
    }

    // ============================================================================ panel/tooltip

    private static String fmt(double v) {
        if (v >= 100) return String.format(Locale.ROOT, "%,.0f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** The status lines the scanner and the creative build print. Server-side state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(
            "Kerr " + (densityInstalled ? "T2 (Density Upgrade installed)" : "T1")
                + ", mode "
                + modeName(machineMode)
                + (modeAvailable(machineMode) ? "" : " - needs " + needsFor(machineMode)));
        if (holeOpen) {
            out.add(
                "Hole OPEN: " + fmt(mass)
                    + " SM (setpoint "
                    + fmt(setpoint)
                    + "), bleed "
                    + String.format(Locale.ROOT, "%.4f", lastBleed)
                    + " SM/t, bolts "
                    + lastBoltsFired
                    + "/"
                    + boltCap()
                    + " a tick");
        } else {
            out.add("Hole closed: put a Black Hole Seed in a bolt bus (setpoint " + fmt(setpoint) + " SM)");
        }
        out.add(
            "Parallel " + String.format(Locale.ROOT, "%,d", getMaxParallelRecipes())
                + ", speed x"
                + String.format(Locale.ROOT, "%.2f", speedFactor())
                + ", rails Mk "
                + railTierMin
                + " ("
                + railCount
                + "), bolt buses "
                + boltBusCount());
        for (KerrModule module : modules) {
            out.add(
                module.label + " module: "
                    + (module.built ? "FORMED" : module.fault.isEmpty() ? "absent" : "FAULT - " + module.fault));
        }
        return out;
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    private static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] t1 = BTSShapes.size(PIECE_T1);
        final int[] t2 = BTSShapes.size(PIECE_T2);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Black Hole Fabricator, Kerr Ergosphere Engine")
            .addInfo("Kerr T1 replaces the Black Hole Compressor. Does nothing without a module:")
            .addInfo("  Compressor module (south arc): the BHC's Compressor and Neutronium Compressor recipes")
            .addInfo("  Extractor module (east arc): GT's Extractor recipes (items). Both modules can stand at once.")
            .addInfo("Right-click with a Kerr Density Upgrade for Kerr T2: heavier reinforcement, holes")
            .addInfo("  up to 512 SM, and DENSIFY (Hyperdense Stellar Mixture, needs 128+ SM). Irreversible.")
            .addSeparator()
            .addInfo("A Black Hole Seed in a bolt bus opens the hole at 8 SM; a Collapser closes it.")
            .addInfo("Hawking bleed 3.2/M² SM a tick; the governor fires bolts to hold the setpoint:")
            .addInfo("  Neutronium 1/64, Cosmic Neutronium 1/32, Infinity 1/16, Transcendent 1/8, SpaceTime 1/4 SM")
            .addInfo(
                "  at most 4/8/16 bolts a tick by the LOWEST injector rail (Mk I/II/III). Below 2 SM it evaporates.")
            .addInfo("Parallel 8 x voltage tier x floor(sqrt(M)). Speed x(3200/M)/50, 1 to 4: small holes are fast,")
            .addInfo("  big holes are wide. Closed hole: plain compressor recipes only, at 8 x tier.")
            .addInfo("Screwdriver: hole setpoint. Sneak + screwdriver (or the GUI button): mode.")
            .addInfo("Optional BHC Utility Hatches on the cage emit redstone while the hole is open.")
            .addSeparator()
            .addInfo("Chunk-load the full footprint: the structure check fails across unloaded chunks.")
            .beginVariableStructureBlock(t1[0], t2[0], t1[1], t2[1], t1[2] + 8, t2[2], false)
            .addController("Governor arc, front centre")
            .addCasingInfoExactly(
                "Extreme Density Space-Bending Casing",
                bom(PIECE_T1, 'A') + bom(PIECE_T1, 'L'),
                false)
            .addCasingInfoExactly("Naquadah Alloy Frame Box", bom(PIECE_T1, 'B'), false)
            .addCasingInfoExactly("Superradiant Mirror Casing", bom(PIECE_T1, 'C'), false)
            .addCasingInfoExactly("Mirror Backing Casing", bom(PIECE_T1, 'D'), false)
            .addCasingInfoExactly("Bolt Injector Rail (any Mk, mixable)", bom(PIECE_T1, 'E'), true)
            .addCasingInfoExactly("Ergosphere Field Coil", bom(PIECE_T1, 'H'), false)
            .addCasingInfoExactly("Beam Focusing Quadrupole", bom(PIECE_T1, 'F'), false)
            .addCasingInfoExactly("Beam Dump", bom(PIECE_T1, 'M'), false)
            .addCasingInfoExactly("Harmonic Lining", bom(PIECE_T1, 'J'), false)
            .addCasingInfoExactly("Hawking Radiation Realignment Focus", bom(PIECE_T1, 'K'), false)
            .addCasingInfoExactly("Particle Beam Guidance Pipe Casing", bom(PIECE_T1, 'G'), false)
            .addOtherStructurePart(
                "Kerr T2 adds",
                bom(PIECE_T2, 'O') + " Bastion Plate, " + bom(PIECE_T2, 'P') + " Casimir Plate, more lining and glass")
            .addOtherStructurePart(
                "Compressor module",
                "1 Kerr Compressor Module, " + bom(PIECE_COMPRESSOR, 'I')
                    + " Background Radiation Absorbent Casing, "
                    + bom(PIECE_COMPRESSOR, 'J')
                    + " Harmonic Lining",
                4)
            .addOtherStructurePart(
                "Extractor module",
                "1 Kerr Extractor Module, " + bom(PIECE_EXTRACTOR, 'N')
                    + " Ergosphere Extraction Casing, "
                    + bom(PIECE_EXTRACTOR, 'J')
                    + " Harmonic Lining",
                5)
            .addEnergyHatch("Governor arc (2 sockets; 1 laser or up to 2 energy)", 1)
            .addMaintenanceHatch("Governor arc", 2)
            .addOtherStructurePart("Bolt bus (input bus, private)", "8 sockets on the cage, at least 1", 3)
            .addOtherStructurePart("Black Hole Compressor Utility Hatch", "optional, beside the bolt buses", 6)
            .addInputBus("Module arcs (the active mode's module only)", 4, 5)
            .addOutputBus("Module arcs (the active mode's module only)", 4, 5)
            .addStructureInfo("Hologram: trigger stack 1 = Kerr T1, 2+ = Kerr T2; both modules are projected.")
            .addStructureInfo("Survival auto-place builds T2 once the Density Upgrade is in, and the modules")
            .addStructureInfo(
                "  after the core. Rails go in as Mk I; builds never overwrite hatches or downgrade rails.")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ExtremeDensitySpaceBendingCasing.getCasingTexture();
        if (side != aFacing) return new ITexture[] { base };
        return new ITexture[] { base, TextureFactory.builder()
            .addIcon(aActive ? OVERLAY_ACTIVE : OVERLAY)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(aActive ? OVERLAY_ACTIVE_GLOW : OVERLAY_GLOW)
                .extFacing()
                .glow()
                .build() };
    }
}
