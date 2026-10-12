package com.nateyburr.beyondthestars.machines.eye;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import tectech.thing.casing.TTCasingsContainer;

/**
 * SINGULARITY LADDER P3: THE EYE OF HARMONY, T1-T4 - MTE base+15 (28115), UMV
 * (singularity-ladder-build.md §3.3, D-28, D-34, D-41, D-42). Owned by the Eye unit.
 * <p>
 * One controller, grounded on six pylons over an oval plinth (D-34). It replaces TecTech's Eye of
 * Harmony ({@code Config.replaceTecTechEyeOfHarmony}, {@link EyeRecipeRegistry}).
 *
 * <h2>Shape and tiers</h2>
 * Four resource pieces exported from {@code designs/voxel.js}, loaded by {@link BTSShapes}, each
 * checked at its own {@code off=}: {@code eye_core} (T1, required), {@code eye_beam} (T2 beam band),
 * {@code eye_jet} (T3 jet crescent, INDEPENDENT of the beam band so T2 stays optional, D-42) and
 * {@code eye_arms} (T4 spiral arms). T2 recipes need the beam band, T3 the jet crescent, T4 the jet
 * crescent + the arms + an installed Galactic Upgrade Core (Q-11). The blocks are TecTech's own EOH
 * blocks wherever a role fits (the mid-run ruling): Reinforced Spatial shell, Reinforced Temporal
 * plinth, Infinite Spacetime Energy Boundary lining, Quantum Glass, and the three tiered field
 * generators; only the three band casings are ours.
 *
 * <h2>Field generators (Q-20, TecTech's semantics)</h2>
 * {@code W} Spacetime Compression (upper clamp struts), {@code X} Time Dilation (lower struts),
 * {@code Y} Stabilisation (pylon foot pads), any tier 0-8, mixable - the LOWEST of each type counts
 * (as the Kerr's rails, Q-26). On PLANETS only, as TecTech's EOH: compression must reach the
 * planet's requirement and each tier above it cuts time 3 %; each dilation tier halves time and
 * costs 9.25 % success; each stabilisation tier adds 5 % success and costs 5 % yield. Stars ignore
 * all three.
 *
 * <h2>Running</h2>
 * Inputs drain every second from the private sockets into {@code long} buffers. The controller slot
 * holds the target (a star chart or a dimension display block; never consumed). A run debits its
 * whole EU from the owner's wireless network at start (no EU return, Q-4), takes the inputs, rolls
 * success per parallel, and on completion puts the outputs into the ME output bus / hatch. Parallel =
 * {@code 2^(installed tier - recipe tier)}, cut down to what the buffers can pay for.
 *
 * <h2>The four traps</h2>
 * <ol>
 * <li><b>Client vs server.</b> {@link #construct} reads only the trigger stack (1..4 = T1..T4, bands
 * cumulative); it never reads {@link #galacticInstalled}.</li>
 * <li><b>Tile vs item NBT.</b> {@link #writeEyeState} is the one serializer of the persisted state (the
 * Galactic core and the buffers), called from both {@link #saveNBTData} and {@link #setItemNBT}.</li>
 * <li><b>Plant-wide hatch lists.</b> Input sockets live in private lists ({@link EyeHatchElement}); a
 * band whose walk fails drops its sockets again (mark-and-truncate).</li>
 * <li><b>Element chains.</b> Every socket is {@code buildHatchAdder(...).buildAndChain(casing)}, the
 * hatch first; the field-generator chains start at tier 0, which is what holograms place.</li>
 * </ol>
 * All walk state is zeroed in {@link #resetWalk()}, the first statement of {@link #checkMachine}.
 */
public class MTEEyeOfHarmonyBTS extends MTEExtendedPowerMultiBlockBase<MTEEyeOfHarmonyBTS>
    implements ISurvivalConstructable {

    // ------------------------------------------------------------------------------- pieces
    public static final String PIECE_CORE = "eye_core";
    public static final String PIECE_BEAM = "eye_beam";
    public static final String PIECE_JET = "eye_jet";
    public static final String PIECE_ARMS = "eye_arms";
    /** Pieces in hologram order; the trigger stack size (1..4) projects the first N. */
    public static final String[] PIECES = { PIECE_CORE, PIECE_BEAM, PIECE_JET, PIECE_ARMS };

    // -------------------------------------------------------------------- field generators
    public static final int FIELD_COMPRESSION = 0;
    public static final int FIELD_DILATION = 1;
    public static final int FIELD_STABILISATION = 2;
    private static final String[] FIELD_NAMES = { "Spacetime Compression", "Time Dilation", "Stabilisation" };
    /** TecTech's EOH constants (MTEEyeOfHarmony, 5.09.52.594). */
    public static final double COMPRESSION_TIME_DISCOUNT = 0.97;
    public static final double DILATION_CHANCE_COST = 0.0925;
    public static final double STABILISATION_STEP = 0.05;

    /** Buffers cap at 2^62 L each (spec §3.3). */
    public static final long BUFFER_CAP = 1L << 62;
    private static final int DRAIN_INTERVAL = 20;

    /** Touch the NEI page at class init (preInit), before NEI builds its handlers. */
    private static final RecipeMap<?> DISPLAY_MAP = EyeRecipeMaps.EYE;
    private static final Random RANDOM = new Random();

    // ------------------------------------------------------------------------------ textures
    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE_GLOW");

    private static IStructureDefinition<MTEEyeOfHarmonyBTS> STRUCTURE_DEFINITION = null;

    // ---------------------------------------------------- PERSISTED (writeEyeState is the one writer)
    private boolean galacticInstalled;
    /** Fluid name → litres. */
    private final Map<String, Long> buffers = new LinkedHashMap<>();

    // ------------------------------------------------------------- RUN state (server, tile NBT)
    private String runningKey;
    private long runningParallel;
    private long runningSuccesses;
    private double runningYield = 1.0;
    private String lastRun = "";

    // ---------------------------------------------------------- WALK state (server, resetWalk)
    private int mCasing;
    private final int[] fieldMin = { -1, -1, -1 };
    private final int[] fieldCount = new int[3];
    private boolean beamBuilt;
    private boolean jetBuilt;
    private boolean armsBuilt;
    private String beamFault = "";
    private String jetFault = "";
    private String armsFault = "";
    private final ArrayList<MTEHatchInput> gasHatches = new ArrayList<>();
    private final ArrayList<MTEHatchInput> stellarHatches = new ArrayList<>();

    public MTEEyeOfHarmonyBTS(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEEyeOfHarmonyBTS(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEEyeOfHarmonyBTS(this.mName);
    }

    // =================================================================================== shape

    private static int[] off(String piece) {
        return BTSShapes.offset(piece);
    }

    @Override
    public IStructureDefinition<MTEEyeOfHarmonyBTS> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = buildDefinition();
        }
        return STRUCTURE_DEFINITION;
    }

    private static IStructureDefinition<MTEEyeOfHarmonyBTS> buildDefinition() {
        final Block sl = SLBlocks.slCasing;
        final int plinthIndex = Casings.ReinforcedTemporalStructureCasing.getTextureId();
        final int shellIndex = Casings.ReinforcedSpatialStructureCasing.getTextureId();
        return StructureDefinition.<MTEEyeOfHarmonyBTS>builder()
            .addShape(PIECE_CORE, BTSShapes.load(PIECE_CORE))
            .addShape(PIECE_BEAM, BTSShapes.load(PIECE_BEAM))
            .addShape(PIECE_JET, BTSShapes.load(PIECE_JET))
            .addShape(PIECE_ARMS, BTSShapes.load(PIECE_ARMS))
            // ---- TecTech's own EOH blocks (the mid-run ruling)
            .addElement('U', casing(Casings.ReinforcedSpatialStructureCasing))
            .addElement('Q', casing(Casings.ReinforcedTemporalStructureCasing))
            .addElement('J', casing(Casings.InfiniteSpacetimeEnergyBoundaryCasing))
            .addElement('Z', casing(Casings.QuantumGlass))
            .addElement('W', field(FIELD_COMPRESSION, TTCasingsContainer.SpacetimeCompressionFieldGenerators))
            .addElement('X', field(FIELD_DILATION, TTCasingsContainer.TimeAccelerationFieldGenerator))
            .addElement('Y', field(FIELD_STABILISATION, TTCasingsContainer.StabilisationFieldGenerators))
            // ---- our three per-tier band casings
            .addElement('R', block(sl, BlockSLCasing.BEAM_BAND))
            .addElement('S', block(sl, BlockSLCasing.JET_CRESCENT))
            .addElement('T', block(sl, BlockSLCasing.GALACTIC_SHELL))
            // ---- plinth sockets
            .addElement(
                'h',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Gas)
                    .casingIndex(plinthIndex)
                    .hint(1)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .addElement(
                'o',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Output)
                    .casingIndex(plinthIndex)
                    .hint(2)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .addElement(
                'm',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(Maintenance)
                    .casingIndex(plinthIndex)
                    .hint(3)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            // ---- band sockets (stellar fluids)
            .addElement(
                's',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Stellar)
                    .casingIndex(shellIndex)
                    .hint(4)
                    .buildAndChain(block(sl, BlockSLCasing.BEAM_BAND)))
            .addElement(
                't',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Stellar)
                    .casingIndex(shellIndex)
                    .hint(4)
                    .buildAndChain(block(sl, BlockSLCasing.JET_CRESCENT)))
            .build();
    }

    private static IStructureElement<MTEEyeOfHarmonyBTS> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEEyeOfHarmonyBTS> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /**
     * One TecTech field-generator type, tiers 0-8 MIXABLE: the walk records the lowest tier it meets
     * (Q-20). Tier 0 comes first, so holograms and survival auto-place use it (trap 4).
     */
    @SuppressWarnings("unchecked")
    private static IStructureElement<MTEEyeOfHarmonyBTS> field(int type, Block generator) {
        final IStructureElement<MTEEyeOfHarmonyBTS>[] tiers = new IStructureElement[9];
        for (int meta = 0; meta < 9; meta++) {
            final int tier = meta;
            tiers[meta] = onElementPass(x -> x.noteField(type, tier), ofBlock(generator, meta));
        }
        return noOverwrite(ofChain(tiers));
    }

    private void noteField(int type, int meta) {
        fieldCount[type]++;
        fieldMin[type] = fieldMin[type] < 0 ? meta : Math.min(fieldMin[type], meta);
    }

    /** Horizontal facings only: the Eye stands on its plinth. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    /** Every field a walk writes, zeroed in one place that nothing can get past. */
    private void resetWalk() {
        mCasing = 0;
        Arrays.fill(fieldMin, -1);
        Arrays.fill(fieldCount, 0);
        beamBuilt = false;
        jetBuilt = false;
        armsBuilt = false;
        beamFault = "";
        jetFault = "";
        armsFault = "";
        gasHatches.clear();
        stellarHatches.clear();
        mOutputBusses.clear();
        mOutputHatches.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final int[] o = off(PIECE_CORE);
        if (!checkPiece(PIECE_CORE, o[0], o[1], o[2], errors)) return;
        checkHasMaintenanceHatch(errors);
        if (gasHatchCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.eye.gas"));
        }
        if (outputBusCount() == 0 || outputHatchCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.eye.output"));
        }

        // The three OPTIONAL bands: their own error lists, so nothing they report un-forms the core.
        // A failed band drops the stellar sockets it registered (mark-and-truncate).
        final List<StructureError> pieceErrors = new ArrayList<>();
        beamBuilt = band(PIECE_BEAM, pieceErrors);
        beamFault = fault(beamBuilt, pieceErrors);
        pieceErrors.clear();
        jetBuilt = band(PIECE_JET, pieceErrors);
        jetFault = fault(jetBuilt, pieceErrors);
        pieceErrors.clear();
        armsBuilt = band(PIECE_ARMS, pieceErrors);
        armsFault = fault(armsBuilt, pieceErrors);
    }

    private boolean band(String piece, List<StructureError> pieceErrors) {
        final int mark = stellarHatches.size();
        final int[] o = off(piece);
        final boolean built = checkPiece(piece, o[0], o[1], o[2], pieceErrors);
        if (!built) {
            while (stellarHatches.size() > mark) {
                stellarHatches.remove(stellarHatches.size() - 1);
            }
        }
        return built;
    }

    private static String fault(boolean built, List<StructureError> walkErrors) {
        if (built || walkErrors.isEmpty()) return "";
        final String reason = walkErrors.get(0)
            .getDisplayString();
        return reason == null || reason.isEmpty() ? "incomplete" : reason;
    }

    /** True when recipes of Eye tier {@code tier} may run on this structure (server state). */
    public boolean tierAvailable(int tier) {
        switch (tier) {
            case 1:
                return mMachine;
            case 2:
                return mMachine && beamBuilt;
            case 3:
                return mMachine && jetBuilt;
            case 4:
                return mMachine && jetBuilt && armsBuilt && galacticInstalled;
            default:
                return false;
        }
    }

    /** The highest tier available, 0 if unformed. */
    public int installedTier() {
        for (int t = 4; t >= 1; t--) {
            if (tierAvailable(t)) return t;
        }
        return 0;
    }

    /** Lowest tier (generator meta 0-8) of one field type, or -1 if none was walked. */
    public int fieldTier(int type) {
        return fieldMin[type];
    }

    // ===================================================================== private hatch adders

    private static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    public boolean addGasHatch(IGregTechTileEntity te, int casingIndex) {
        return addInput(te, casingIndex, gasHatches);
    }

    public boolean addStellarHatch(IGregTechTileEntity te, int casingIndex) {
        return addInput(te, casingIndex, stellarHatches);
    }

    private static boolean addInput(IGregTechTileEntity te, int casingIndex, List<MTEHatchInput> list) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInput) {
            ((MTEHatchInput) mte).updateTexture(casingIndex);
            list.add((MTEHatchInput) mte);
            return true;
        }
        return false;
    }

    /** ME outputs only: they are the Eye's only output path, so they join GT's output lists. */
    public boolean addOutputME(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchOutputBusME) {
            ((MTEHatchOutputBusME) mte).updateTexture(casingIndex);
            mOutputBusses.add((MTEHatchOutputBusME) mte);
            return true;
        }
        if (mte instanceof MTEHatchOutputME) {
            ((MTEHatchOutputME) mte).updateTexture(casingIndex);
            mOutputHatches.add((MTEHatchOutputME) mte);
            return true;
        }
        return false;
    }

    private static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity mte : list) {
            if (live(mte)) n++;
        }
        return n;
    }

    public int gasHatchCount() {
        return liveCount(gasHatches);
    }

    public int stellarHatchCount() {
        return liveCount(stellarHatches);
    }

    public int outputBusCount() {
        return liveCount(mOutputBusses);
    }

    public int outputHatchCount() {
        return liveCount(mOutputHatches);
    }

    // ==================================================================== construct / survival

    /** Trap 1: the trigger stack alone picks how many bands to project (1 = T1 .. 4 = T4). */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int n = bandsFor(stackSize);
        for (int i = 0; i < n; i++) {
            final int[] o = off(PIECES[i]);
            buildPiece(PIECES[i], stackSize, hintsOnly, o[0], o[1], o[2]);
        }
    }

    private static int bandsFor(ItemStack stack) {
        final int size = stack == null ? 1 : stack.stackSize;
        return Math.max(1, Math.min(PIECES.length, size));
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final int n = bandsFor(stackSize);
        if (mMachine && installedBands() >= n) return -1;
        int built = -1;
        for (int i = 0; i < n; i++) {
            final int[] o = off(PIECES[i]);
            built = survivalBuildPiece(PIECES[i], stackSize, o[0], o[1], o[2], elementBudget, env, false, true);
            if (built >= 0) return built;
        }
        return built;
    }

    private int installedBands() {
        if (!mMachine) return 0;
        if (armsBuilt && jetBuilt && beamBuilt) return 4;
        if (jetBuilt && beamBuilt) return 3;
        return beamBuilt ? 2 : 1;
    }

    // ============================================================================ the upgrade

    /**
     * Right-click holding a Galactic Upgrade Core: consumes one (not in creative) and unlocks T4.
     * Irreversible (Q-10). A plain right-click, not sneak (Q-24).
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        final ItemStack held = aPlayer.getHeldItem();
        if (held != null && SLItems.galacticUpgradeCore != null && held.getItem() == SLItems.galacticUpgradeCore) {
            if (!aBaseMetaTileEntity.isServerSide()) return true;
            if (galacticInstalled) {
                GTUtility.sendChatToPlayer(aPlayer, "This Eye already carries the Galactic Upgrade Core.");
                return true;
            }
            if (!aPlayer.capabilities.isCreativeMode) {
                held.stackSize--;
                if (held.stackSize <= 0) {
                    aPlayer.inventory.setInventorySlotContents(aPlayer.inventory.currentItem, null);
                }
            }
            installGalactic();
            checkStructure(true, aBaseMetaTileEntity);
            GTUtility.sendChatToPlayer(
                aPlayer,
                EnumChatFormatting.AQUA + "Galactic Upgrade Core installed."
                    + EnumChatFormatting.RESET
                    + " T4 (Sirius B, the Deep Dark) runs once the jet crescent and the spiral arms stand"
                    + (tierAvailable(4) ? "." : EnumChatFormatting.YELLOW + " - not yet: build them."));
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer);
    }

    /** Unlocks T4. Also the creative build's hook. */
    public void installGalactic() {
        galacticInstalled = true;
    }

    public boolean isGalacticInstalled() {
        return galacticInstalled;
    }

    // ============================================================================== buffers

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && mMachine && aTick % DRAIN_INTERVAL == 0) {
            drainSockets();
        }
    }

    /** Moves every Eye input fluid out of the sockets into the long buffers, as TecTech's EOH does. */
    private void drainSockets() {
        for (MTEHatchInput hatch : gasHatches) {
            drain(hatch);
        }
        for (MTEHatchInput hatch : stellarHatches) {
            drain(hatch);
        }
    }

    private void drain(MTEHatchInput hatch) {
        if (!live(hatch)) return;
        final boolean aware = hatch instanceof IRecipeProcessingAwareHatch;
        if (aware) ((IRecipeProcessingAwareHatch) hatch).startRecipeProcessing();
        try {
            for (FluidStack stored : storedFluids(hatch)) {
                if (stored == null || stored.amount <= 0 || !EyeRecipeRegistry.isInputFluid(stored.getFluid())) {
                    continue;
                }
                final long room = BUFFER_CAP - buffer(stored.getFluid());
                if (room <= 0) continue;
                final FluidStack want = stored.copy();
                want.amount = (int) Math.min(stored.amount, room);
                final FluidStack got = hatch.drain(ForgeDirection.UNKNOWN, want, true);
                if (got != null && got.amount > 0) addBuffer(got.getFluid(), got.amount);
            }
        } finally {
            if (aware) ((IRecipeProcessingAwareHatch) hatch).endRecipeProcessing(this);
        }
    }

    private static FluidStack[] storedFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchInputME) return ((MTEHatchInputME) hatch).getStoredFluids();
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    public long buffer(Fluid fluid) {
        if (fluid == null) return 0;
        final Long v = buffers.get(fluid.getName());
        return v == null ? 0 : v;
    }

    private void addBuffer(Fluid fluid, long amount) {
        final long now = buffer(fluid);
        buffers.put(fluid.getName(), Math.min(BUFFER_CAP, now + amount));
    }

    private void takeBuffer(Fluid fluid, long amount) {
        final long left = buffer(fluid) - amount;
        if (left > 0) {
            buffers.put(fluid.getName(), left);
        } else {
            buffers.remove(fluid.getName());
        }
    }

    /** Debug/creative: fill every Eye input buffer to {@code litres}. */
    public void debugFillBuffers(long litres) {
        for (Fluid fluid : EyeRecipeRegistry.inputFluids()) {
            buffers.put(fluid.getName(), Math.min(BUFFER_CAP, Math.max(buffer(fluid), litres)));
        }
    }

    // ============================================================================ processing

    @Override
    public RecipeMap<?> getRecipeMap() {
        return DISPLAY_MAP;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.singletonList(DISPLAY_MAP);
    }

    private UUID owner() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final UUID uuid = base == null ? null : base.getOwnerUuid();
        if (uuid != null) WirelessNetworkManager.strongCheckOrAddUser(uuid);
        return uuid;
    }

    /**
     * Looks up the target, checks the tier and the fields, sizes the parallel to the buffers, debits
     * the run's EU from the owner's wireless network, takes the inputs and rolls success. The
     * outputs land in {@link #outputAfterRecipe()}.
     */
    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        final ItemStack target = getControllerSlot();
        if (target == null) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.notarget");
        final EyeRecipe recipe = EyeRecipeRegistry.lookup(target);
        if (recipe == null) return CheckRecipeResultRegistry.NO_RECIPE;
        if (!tierAvailable(recipe.tier)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.tier" + recipe.tier);
        }
        if (recipe.isPlanet() && fieldMin[FIELD_COMPRESSION] < recipe.compressionRequired) {
            return CheckRecipeResultRegistry.insufficientMachineTier(recipe.compressionRequired + 1);
        }
        long parallel = 1L << (installedTier() - recipe.tier);
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            parallel = Math.min(parallel, in.amount <= 0 ? parallel : buffer(in.fluid) / in.amount);
        }
        if (parallel < 1) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.inputs");
        final UUID owner = owner();
        if (owner == null) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.noowner");
        final BigInteger cost = BigInteger.valueOf(recipe.euPerRun)
            .multiply(BigInteger.valueOf(parallel));
        if (!WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost.negate())) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.noeu");
        }
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            takeBuffer(in.fluid, in.amount * parallel);
        }

        final double chance = successChance(recipe);
        long successes = 0;
        for (long i = 0; i < parallel; i++) {
            if (RANDOM.nextDouble() < chance) successes++;
        }
        runningKey = recipe.key;
        runningParallel = parallel;
        runningSuccesses = successes;
        runningYield = yieldOf(recipe);
        mOutputItems = null;
        mOutputFluids = null;
        lEUt = 0;
        mEfficiency = 10_000;
        mEfficiencyIncrease = 10_000;
        mMaxProgresstime = (int) Math.min(Integer.MAX_VALUE, runTicks(recipe));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /** Stars always succeed; planets: TecTech's base, minus dilation, plus stabilisation. */
    public double successChance(EyeRecipe recipe) {
        if (!recipe.isPlanet()) return 1.0;
        final double chance = recipe.baseSuccess - DILATION_CHANCE_COST * Math.max(0, fieldMin[FIELD_DILATION])
            + STABILISATION_STEP * Math.max(0, fieldMin[FIELD_STABILISATION]);
        return Math.max(0.0, Math.min(1.0, chance));
    }

    /** Stars: 1; planets: 1 - 5 % per stabilisation tier (TecTech). */
    public double yieldOf(EyeRecipe recipe) {
        if (!recipe.isPlanet()) return 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - STABILISATION_STEP * Math.max(0, fieldMin[FIELD_STABILISATION])));
    }

    /** Stars: the base time; planets: halved per dilation tier, -3 % per compression tier above need. */
    public long runTicks(EyeRecipe recipe) {
        if (!recipe.isPlanet()) return Math.max(1, recipe.baseTicks);
        final double t = recipe.baseTicks * Math.pow(2.0, -Math.max(0, fieldMin[FIELD_DILATION]))
            * Math
                .pow(COMPRESSION_TIME_DISCOUNT, Math.max(0, fieldMin[FIELD_COMPRESSION]) - recipe.compressionRequired);
        return Math.max(1L, Math.round(t));
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        final EyeRecipe recipe = EyeRecipeRegistry.byKey(runningKey);
        if (recipe == null) {
            if (runningKey != null) lastRun = runningKey + ": recipe no longer registered, outputs lost";
        } else if (runningSuccesses <= 0) {
            lastRun = recipe.name + ": FAILED (0/" + runningParallel + " succeeded)";
        } else {
            final double mult = runningSuccesses * runningYield;
            final ItemEjectionHelper items = new ItemEjectionHelper(this);
            for (EyeRecipe.ItemAmount out : recipe.itemOutputs) {
                ejectItem(items, out.stack, (long) Math.floor(out.amount * mult));
            }
            items.commit();
            for (EyeRecipe.FluidAmount out : recipe.fluidOutputs) {
                dumpFluid(out.fluid, (long) Math.floor(out.amount * mult));
            }
            lastRun = recipe.name + ": " + runningSuccesses + "/" + runningParallel + " succeeded";
        }
        runningKey = null;
        runningParallel = 0;
        runningSuccesses = 0;
        runningYield = 1.0;
    }

    private static void ejectItem(ItemEjectionHelper helper, ItemStack stack, long amount) {
        for (long done = 0; done < amount; done += Integer.MAX_VALUE) {
            final int n = (int) Math.min(Integer.MAX_VALUE, amount - done);
            helper.ejectStack(GTUtility.copyAmountUnsafe(n, stack));
        }
    }

    private void dumpFluid(Fluid fluid, long amount) {
        long left = amount;
        while (left > 0) {
            final int n = (int) Math.min(Integer.MAX_VALUE, left);
            dumpFluid(mOutputHatches, new FluidStack(fluid, n), false);
            left -= n;
        }
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    // ===================================================================================== NBT

    /** The ONE serializer of the persisted state (trap 2): the Galactic core and the buffers. */
    private void writeEyeState(NBTTagCompound nbt) {
        if (galacticInstalled) nbt.setBoolean("btsEyeGalactic", true);
        if (!buffers.isEmpty()) {
            final NBTTagCompound b = new NBTTagCompound();
            for (Map.Entry<String, Long> e : buffers.entrySet()) {
                b.setLong(e.getKey(), e.getValue());
            }
            nbt.setTag("btsEyeBuffers", b);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeEyeState(aNBT);
        if (runningKey != null) {
            aNBT.setString("btsEyeRunKey", runningKey);
            aNBT.setLong("btsEyeRunParallel", runningParallel);
            aNBT.setLong("btsEyeRunSuccesses", runningSuccesses);
            aNBT.setDouble("btsEyeRunYield", runningYield);
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeEyeState(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        galacticInstalled = aNBT.getBoolean("btsEyeGalactic");
        buffers.clear();
        if (aNBT.hasKey("btsEyeBuffers")) {
            final NBTTagCompound b = aNBT.getCompoundTag("btsEyeBuffers");
            for (Object key : b.func_150296_c()) {
                final String name = (String) key;
                if (FluidRegistry.getFluid(name) != null && b.getLong(name) > 0) {
                    buffers.put(name, Math.min(BUFFER_CAP, b.getLong(name)));
                }
            }
        }
        runningKey = aNBT.hasKey("btsEyeRunKey") ? aNBT.getString("btsEyeRunKey") : null;
        runningParallel = aNBT.getLong("btsEyeRunParallel");
        runningSuccesses = aNBT.getLong("btsEyeRunSuccesses");
        runningYield = aNBT.hasKey("btsEyeRunYield") ? aNBT.getDouble("btsEyeRunYield") : 1.0;
    }

    // ============================================================================ panel/tooltip

    private static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static String fieldName(int meta) {
        return meta < 0 ? "none" : "tier " + (meta + 1);
    }

    /** The status lines the scanner and the creative build print. Server-side state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(
            "Eye T" + installedTier()
                + (galacticInstalled ? " (Galactic Upgrade Core installed)" : "")
                + ": beam band "
                + state(beamBuilt, beamFault)
                + ", jet crescent "
                + state(jetBuilt, jetFault)
                + ", spiral arms "
                + state(armsBuilt, armsFault));
        out.add(
            "Fields: " + FIELD_NAMES[0]
                + " "
                + fieldName(fieldMin[0])
                + ", "
                + FIELD_NAMES[1]
                + " "
                + fieldName(fieldMin[1])
                + ", "
                + FIELD_NAMES[2]
                + " "
                + fieldName(fieldMin[2]));
        out.add(
            "Sockets: gas " + gasHatchCount()
                + ", stellar "
                + stellarHatchCount()
                + ", ME out "
                + outputBusCount()
                + " bus / "
                + outputHatchCount()
                + " hatch");
        if (buffers.isEmpty()) {
            out.add("Buffers: empty");
        } else {
            final StringBuilder sb = new StringBuilder("Buffers:");
            for (Map.Entry<String, Long> e : buffers.entrySet()) {
                final Fluid fluid = FluidRegistry.getFluid(e.getKey());
                sb.append(' ')
                    .append(fluid == null ? e.getKey() : fluid.getLocalizedName(new FluidStack(fluid, 1)))
                    .append(' ')
                    .append(fmt(e.getValue()))
                    .append(" L;");
            }
            out.add(sb.toString());
        }
        final EyeRecipe target = EyeRecipeRegistry.lookup(getControllerSlot());
        if (target != null) {
            out.add(
                "Target: " + target.name
                    + " (T"
                    + target.tier
                    + "), parallel up to "
                    + (installedTier() >= target.tier ? (1L << (installedTier() - target.tier)) : 0)
                    + ", success "
                    + Math.round(successChance(target) * 100)
                    + "%, "
                    + fmt(runTicks(target) / 20)
                    + " s a run");
        } else {
            out.add("Target: none (put a star chart or a dimension display block in the controller slot)");
        }
        if (runningKey != null) {
            out.add("Running: " + runningKey + " x" + runningParallel + " (" + runningSuccesses + " will succeed)");
        }
        if (!lastRun.isEmpty()) out.add("Last run: " + lastRun);
        return out;
    }

    private static String state(boolean built, String fault) {
        return built ? "FORMED" : fault.isEmpty() ? "absent" : "absent (" + fault + ")";
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
        final int[] core = BTSShapes.size(PIECE_CORE);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Stellar Forge, Eye of Harmony")
            .addInfo("Beyond the Stars: replaces TecTech's Eye of Harmony.")
            .addInfo("Put a Star Chart or a dimension display block in the controller slot (not consumed).")
            .addInfo("T1 core: Sol (Raw Star Matter) and planets R0-R3.")
            .addInfo("T2 beam band (optional): Sirius A (Excited Stellar Plasma) and planets R4-R6.")
            .addInfo("T3 jet crescent: Arcturus (Raw Hyperdense Stellar Plasma) and planets R7-R8.")
            .addInfo("T4 jet crescent + spiral arms + Galactic Upgrade Core (right-click it in):")
            .addInfo("  Sirius B (White Dwarf Matter) and the Deep Dark.")
            .addInfo("Planets give TecTech's ores and plasmas, without Raw Star Matter or dwarf matter.")
            .addSeparator()
            .addInfo("Power: wireless EU, the whole run debited at start; no EU is returned.")
            .addInfo("Parallel x2 for every installed tier above the recipe's tier.")
            .addInfo("Field generators act on planets only, as in TecTech's Eye (lowest tier of each counts):")
            .addInfo("  Spacetime Compression: must reach the planet's tier; -3% time per tier above")
            .addInfo("  Time Dilation: half the time per tier, -9.25% success per tier")
            .addInfo("  Stabilisation: +5% success, -5% yield per tier. Stars always succeed.")
            .addInfo("Inputs drain every second into internal buffers; outputs go to ME only.")
            .addSeparator()
            .addInfo("Chunk-load the full footprint: the structure check fails across unloaded chunks.")
            .beginVariableStructureBlock(core[0], core[0], core[1], core[1] + 5, core[2], core[2] + 57, false)
            .addController("Plinth, front centre")
            .addCasingInfoExactly("Reinforced Spatial Structure Casing", bom(PIECE_CORE, 'U'), false)
            .addCasingInfoExactly("Reinforced Temporal Structure Casing", bom(PIECE_CORE, 'Q'), false)
            .addCasingInfoExactly(
                "Infinite SpaceTime Energy Boundary Casing",
                bom(PIECE_CORE, 'J') + bom(PIECE_BEAM, 'J') + bom(PIECE_JET, 'J') + bom(PIECE_ARMS, 'J'),
                false)
            .addCasingInfoExactly(
                "Quantum Glass",
                bom(PIECE_CORE, 'Z') + bom(PIECE_BEAM, 'Z') + bom(PIECE_JET, 'Z') + bom(PIECE_ARMS, 'Z'),
                false)
            .addCasingInfoExactly("Spacetime Compression Field Generator (any tier)", bom(PIECE_CORE, 'W'), true)
            .addCasingInfoExactly("Time Dilation Field Generator (any tier)", bom(PIECE_CORE, 'X'), true)
            .addCasingInfoExactly("Stabilisation Field Generator (any tier)", bom(PIECE_CORE, 'Y'), true)
            .addOtherStructurePart("T2 beam band", bom(PIECE_BEAM, 'R') + " Beam Band Casing", 4)
            .addOtherStructurePart("T3 jet crescent", bom(PIECE_JET, 'S') + " Jet Crescent Casing", 4)
            .addOtherStructurePart("T4 spiral arms", bom(PIECE_ARMS, 'T') + " Galactic Shell Plating")
            .addOtherStructurePart("Gas input hatch (H, He)", "plinth, at least 1", 1)
            .addOtherStructurePart("ME Output Bus + ME Output Hatch", "plinth, at least 1 of each", 2)
            .addMaintenanceHatch("Plinth", 3)
            .addOtherStructurePart("Stellar input hatch", "beam band and jet crescent sockets", 4)
            .addStructureInfo("Hologram: trigger stack 1 = T1 core, 2 = + beam band, 3 = + jet crescent, 4 = + arms.")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ReinforcedSpatialStructureCasing.getCasingTexture();
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
