package com.nateyburr.beyondthestars.machines.homogeny;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing3;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.items.SLMaxItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.OwnerGate;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.conduit.ReceptorHatchElement;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;
import com.nateyburr.beyondthestars.machines.reality.RealityRecipeMaps;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): THE HOMOGENY, MTE base+30 (28130), my "Mixer" sketch (design repo
 * {@code designs/docs/homogeny-reality.md} §1, PROPOSED; D-46/D-47/D-49).
 * <p>
 * One controller on the volute's discharge throat, upgraded in place through three tiers:
 * <ul>
 * <li><b>T1 (UMV)</b>, {@code homog_t1}: mixing column, shear helix, rails, volute, three feed vessels. 1 parallel.
 * Raw Star Matter 100 : 1 into Excited Stellar Plasma Mixture (SL-H06), the alternative ESPM source (D-49: Eye T2
 * stays required).</li>
 * <li><b>T2 (UXV)</b>, + {@code homog_t2}: the middle stirring ring and three more vessels. 4 parallels; Degenerate
 * Stellar Mixture (SL-H11).</li>
 * <li><b>T3 (MAX)</b>, + {@code homog_t3}: the tilted outer ring and its tail. 16 parallels; Entanglement Medium
 * (SL-H21).</li>
 * </ul>
 * <b>The tier</b> is {@code min(installedTier, structureTier)}. The structure tier is cumulative (a ring counts only
 * while every ring below it stands). The installed tier is permanent and rises only through the one install path
 * ({@link #beginInstall()} then {@link #completeInstall()}, the milestone + install-run pattern of rulings 4-5):
 * <ol>
 * <li>the next ring stands (structure);</li>
 * <li>the milestone: {@value #MILESTONE_LITRES} L of the current tier's product mixed over the machine's life (ESPM
 * for T1 to T2, Degenerate Stellar Mixture for T2 to T3);</li>
 * <li>the next tier's core is in escrow (right-click it onto the controller; sneak-punch returns it, cancelling an
 * install in progress);</li>
 * <li>Install (GUI button or screwdriver): {@value #INSTALL_TICKS} ticks offline, paying the install EU/t from the
 * energy hatches every tick (a short tick pauses, never loses, progress).</li>
 * </ol>
 * <b>Running</b>: GT's processing logic on {@link RealityRecipeMaps#HOMOGENY}, fluids from the feed-vessel input
 * hatches, parallels 1 / 4 / 16, NO overclock (EU/t = recipe EU/t x parallels). A recipe's special value is its
 * minimum Homogeny tier. No stability field and no maintenance hatch (H-7).
 * <p>
 * <b>Traps.</b> (1) {@link #construct} reads the trigger stack only (1..3 = T1..T3). (2) {@link #writeState} is the one
 * serializer, used by both tile and item NBT, so the installed tier, milestones, escrow and an install in progress
 * survive a wrench. (3) A ring whose walk fails drops the feed hatches it registered (mark-and-truncate on GT's input
 * list). (4) Sockets are {@code buildHatchAdder(...).buildAndChain(casing)}, the hatch first.
 */
public class MTEHomogeny extends MTEExtendedPowerMultiBlockBase<MTEHomogeny>
    implements ISurvivalConstructable, IMTERenderer {

    public static final String PIECE_T1 = "homog_t1";
    public static final String PIECE_T2 = "homog_t2";
    public static final String PIECE_T3 = "homog_t3";
    /** Pieces in tier order; the trigger stack (1..3) projects the first N. */
    public static final String[] PIECES = { PIECE_T1, PIECE_T2, PIECE_T3 };
    private static final String[] PIECE_NAMES = { "column + volute", "stirring ring", "outer ring + tail" };
    public static final int MAX_TIER = PIECES.length;
    /** Parallels per tier (index = tier). "New materials and faster" (D-47). */
    public static final int[] PARALLELS = { 0, 1, 4, 16 };
    /** The milestone per upgrade: litres of the current tier's product mixed over the machine's life. */
    public static final long MILESTONE_LITRES = 100_000L;
    /** The install run, 10 minutes. */
    public static final int INSTALL_TICKS = 12_000;
    /**
     * Install progress marks the chunk for saving this often: nothing else touches a chunk whose only change is our
     * counter, so without it a restart reloaded the install as it was when it started.
     */
    private static final int SAVE_EVERY_TICKS = 20;
    /** The install run's EU/t, index = target tier (doc §1.3: RECIPE_UXV for T2, RECIPE_MAX for T3). */
    public static final long[] INSTALL_EUT = { 0, 0, TierEU.RECIPE_UXV, TierEU.RECIPE_MAX };

    /** Touch the recipe map at class init (preInit), before NEI builds its handlers. */
    private static final RecipeMap<?> MAP = RealityRecipeMaps.HOMOGENY;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_HOMOGENY");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_HOMOGENY_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_HOMOGENY_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_HOMOGENY_ACTIVE_GLOW");

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread. */
    private static volatile IStructureDefinition<MTEHomogeny> STRUCTURE_DEFINITION;

    // ------------------------------------------------------ PERSISTED (writeState is the one writer)
    private int installedTier = 1;
    private long mixedEspm;
    private long mixedDegenerate;
    private ItemStack escrow;
    private boolean installing;
    private int installTicks;

    // ------------------------------------------------------------- RUN state (tile NBT)
    private long pendingEspm;
    private long pendingDegenerate;
    private String lastNote = "";
    /** Why the install did not advance this tick ("" when it did). Server, not persisted. */
    private String installStall = "";

    // ------------------------------------------------- P4 quasar link (server, never saved: nothing about a link is)
    private final BurstHost burstHost = new BurstHost();
    /** The running mix needs σ ≥ 1 (Entanglement Medium); restored from the run's outputs after a load. */
    private boolean runNeedsLink;

    // ---------------------------------------------------------------- WALK state (resetWalk)
    private int mCasing;
    /** P4: GRB Receptor Ports in the stirring ring's {@code g} cells (only while the ring counts). */
    private final ArrayList<MTEHatchGRBReceptor> receptors = new ArrayList<>();
    private final boolean[] pieceBuilt = new boolean[PIECES.length];
    private final String[] pieceFault = { "", "", "" };

    // ------------------------------------- renders v2 (§11, §13): GT's render-data channel only, never saved
    /** Feed colours 0..2 and the product colour 3 (RGB) by tier: Raw Star Matter, ESPM, cyan; the product's. */
    private static final int[][] RENDER_PALETTE = { { 0xFF8A2A, 0xE040C0, 0x40C8FF, 0xE04ACF },
        { 0xFF8A2A, 0xE040C0, 0x40C8FF, 0xE04ACF }, { 0xE04ACF, 0xFFB040, 0x6A4AFF, 0x8FA8FF },
        { 0xFF7A2A, 0xE040C0, 0x6A4AFF, 0x40E0FF } };
    /** Client: the tier the vortex shows (0: not formed, draw nothing) and its four colours, as last sent. */
    private int renderTier;
    private final int[] renderColours = RENDER_PALETTE[1].clone();
    /** Server: the last product colour (kept while idle). */
    private int lastProductColour = -1;

    public MTEHomogeny(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEHomogeny(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHomogeny(this.mName);
    }

    // =================================================================================== shape

    @Override
    public IStructureDefinition<MTEHomogeny> getStructureDefinition() {
        IStructureDefinition<MTEHomogeny> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEHomogeny.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    /**
     * The texture page index hatches on the Homogeny take: the column caps' Reinforced Temporal Structure Casing
     * (TecTech's Eye casing, x32 in the BEC since ruling 11a). The caps were Godforge casing + lens glass: 2,430
     * Godforge casings on an optional UMV machine was more than a Godforge module costs (question B-80).
     */
    static int hatchIndex() {
        return Casings.ReinforcedTemporalStructureCasing.getTextureId();
    }

    private static IStructureDefinition<MTEHomogeny> buildDefinition() {
        final Block c3 = SLBlocks.slCasing3;
        final int index = hatchIndex();
        return StructureDefinition.<MTEHomogeny>builder()
            .addShape(PIECE_T1, BTSShapes.load(PIECE_T1))
            .addShape(PIECE_T2, BTSShapes.load(PIECE_T2))
            .addShape(PIECE_T3, BTSShapes.load(PIECE_T3))
            // ---- our casings (slCasing3) and the reused SL blocks
            .addElement('A', block(c3, BlockSLCasing3.STIRRING_BAND))
            .addElement('B', block(c3, BlockSLCasing3.SHEAR_HELIX))
            .addElement('C', block(c3, BlockSLCasing3.FEED_VESSEL))
            .addElement('D', block(c3, BlockSLCasing3.VORTEX_INLET))
            .addElement('E', crystalOrLens())
            .addElement('F', block(c3, BlockSLCasing3.ETERNITY_SHELL))
            .addElement('G', block(SLBlocks.slCasing, BlockSLCasing.GALACTIC_SHELL))
            .addElement('J', block(SLBlocks.slCasing2, BlockSLCasing2.LINING))
            .addElement('O', block(SLBlocks.slCasing, BlockSLCasing.BASTION))
            .addElement('P', block(SLBlocks.slCasing, BlockSLCasing.CASIMIR))
            // ---- pack blocks
            .addElement('K', casing(Casings.QuantumGlass))
            .addElement('Z', casing(Casings.QuantumGlass))
            .addElement('M', casing(Casings.ReinforcedTemporalStructureCasing))
            // P4 (quasar-p4.md section 3.1): a GRB Receptor Port, or the Galactic Shell these cells held before P4, so
            // builds that stand keep forming. Three cells on the T2 stirring ring. Hologram hint 4.
            .addElement(
                'g',
                buildHatchAdder(MTEHomogeny.class)
                    .atLeast(ReceptorHatchElement.<MTEHomogeny>of(MTEHomogeny::addReceptor, MTEHomogeny::receptorCount))
                    .casingIndex(index)
                    .hint(4)
                    .buildAndChain(block(SLBlocks.slCasing, BlockSLCasing.GALACTIC_SHELL)))
            // ---- sockets: the hatch first, then the casing it falls back to
            .addElement(
                'h',
                buildHatchAdder(MTEHomogeny.class).atLeast(InputHatch)
                    .casingIndex(index)
                    .hint(1)
                    .buildAndChain(block(c3, BlockSLCasing3.FEED_VESSEL)))
            .addElement(
                'o',
                buildHatchAdder(MTEHomogeny.class).atLeast(OutputHatch)
                    .casingIndex(index)
                    .hint(2)
                    .buildAndChain(block(c3, BlockSLCasing3.STIRRING_BAND)))
            .addElement(
                'e',
                buildHatchAdder(MTEHomogeny.class).atLeast(Energy.or(ExoticEnergy))
                    .casingIndex(index)
                    .hint(3)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .build();
    }

    private static IStructureElement<MTEHomogeny> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEHomogeny> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /**
     * {@code E}: the throat tip's glowing core (T1, 5 cells) and the T3 tail's tip. Region Crystal is the drawn block,
     * but it needs Ergomatter (stage S8), which a UMV player cannot have, so the Godforge lens glass is accepted too
     * (question B-76 item 3). Hints and creative builds place the crystal; survival auto-place falls back to the lens.
     */
    private static IStructureElement<MTEHomogeny> crystalOrLens() {
        return noOverwrite(
            onElementPass(
                x -> ++x.mCasing,
                ofChain(
                    ofBlock(SLBlocks.slCasing3, BlockSLCasing3.REGION_CRYSTAL),
                    Casings.SpatiallyTranscendentGravitationalLens.asElement())));
    }

    /** Horizontal facings only: the Homogeny stands on its column. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    private void resetWalk() {
        mCasing = 0;
        receptors.clear();
        Arrays.fill(pieceBuilt, false);
        Arrays.fill(pieceFault, "");
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final int[] o = BTSShapes.offset(PIECE_T1);
        if (!checkPiece(PIECE_T1, o[0], o[1], o[2], errors)) return;
        pieceBuilt[0] = true;
        // The optional rings, each with its own error list so nothing they report un-forms T1. Cumulative: a ring
        // counts only while every ring below it counts; a ring that does not count drops its feed hatches.
        final List<StructureError> ring = new ArrayList<>();
        boolean below = true;
        for (int i = 1; i < PIECES.length; i++) {
            ring.clear();
            final int mark = mInputHatches.size();
            final int rMark = receptors.size();
            final int[] at = BTSShapes.offset(PIECES[i]);
            pieceBuilt[i] = checkPiece(PIECES[i], at[0], at[1], at[2], ring);
            pieceFault[i] = pieceBuilt[i] || ring.isEmpty() ? ""
                : String.valueOf(
                    BTSStructures.oneLine(
                        ring.get(0)
                            .getDisplayString()));
            below = below && pieceBuilt[i];
            if (!below) {
                while (mInputHatches.size() > mark) mInputHatches.remove(mInputHatches.size() - 1);
                while (receptors.size() > rMark) receptors.remove(receptors.size() - 1);
            }
        }
        if (mInputHatches.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.homogeny.feed"));
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.homogeny.output"));
        if (getExoticAndNormalEnergyHatchList().isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.homogeny.energy"));
        }
    }

    /** P4: a GRB Receptor Port in a stirring-ring {@code g} cell. */
    public boolean addReceptor(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchGRBReceptor) {
            ((MTEHatchGRBReceptor) mte).updateTexture(casingIndex);
            receptors.add((MTEHatchGRBReceptor) mte);
            return true;
        }
        return false;
    }

    public int receptorCount() {
        int n = 0;
        for (MTEHatchGRBReceptor r : receptors) if (r != null && r.isValid()) n++;
        return n;
    }

    /** The stirring ring's receptor ports (server state, read-only). */
    public List<MTEHatchGRBReceptor> receptors() {
        return java.util.Collections.unmodifiableList(receptors);
    }

    /** Structural tier: T1 piece = 1, + stirring ring = 2, + outer ring = 3 (cumulative). 0 if unformed. Server. */
    public int structureTier() {
        if (!mMachine) return 0;
        int tier = 1;
        while (tier < PIECES.length && pieceBuilt[tier]) tier++;
        return tier;
    }

    /** The tier the Homogeny runs at: {@code min(installed, structure)}. 0 if unformed. Server state. */
    public int tier() {
        return Math.min(installedTier, structureTier());
    }

    public int installedTier() {
        return installedTier;
    }

    public int maxParallel() {
        return PARALLELS[Math.max(0, Math.min(MAX_TIER, tier()))];
    }

    /** Why ring {@code index} failed its last walk (its first structure error, one line), or "". Server state. */
    public String pieceFault(int index) {
        return index >= 0 && index < PIECES.length ? pieceFault[index] : "";
    }

    /** True while the escrow holds the core of the next tier. Server state. */
    public boolean nextCoreInEscrow() {
        return escrow != null && installedTier < MAX_TIER && escrow.getItem() == coreFor(installedTier + 1);
    }

    /** Why a started install run is not advancing ("" while it advances). Server state. */
    public String installStall() {
        return installStall;
    }

    /** Ticks left in the install run (0 when none runs). Server state. */
    public int installTicksLeft() {
        return installing ? Math.max(0, INSTALL_TICKS - installTicks) : 0;
    }

    /**
     * GUI cleanup (controller-gui-spec.md §5.6): the next tier's three conditions, each met or not, from the same
     * fields {@link #upgradeLine()} and {@link #installBlocker()} read: the milestone mixed, the core in escrow, the
     * next ring built. Reads only. Empty at T3.
     */
    public BTSChecklist upgradeChecklist() {
        final BTSChecklist c = new BTSChecklist();
        if (installedTier >= MAX_TIER) return c;
        final int next = installedTier + 1;
        final String g = "beyondthestars.gui.homogeny.";
        final long have = milestoneProgress();
        final Fluid productFluid = installedTier == 1 ? SLFluids.excitedStellarPlasma
            : SLRealityFluids.degenerateStellarMixture;
        final String product = productFluid == null ? (installedTier == 1 ? "ESPM" : "Degenerate")
            : new FluidStack(productFluid, 1).getLocalizedName();
        c.add(
            milestoneMet(),
            BTSText.lang(g + "chk.mixed"),
            BTSText.lit(BTSFormat.haveNeed(Math.min(have, MILESTONE_LITRES), MILESTONE_LITRES) + " L"),
            BTSChecklist.frac(have, MILESTONE_LITRES),
            BTSText.lang(g + "chk.mixed.tip", product, fmt(Math.min(have, MILESTONE_LITRES)), fmt(MILESTONE_LITRES)));
        final boolean core = nextCoreInEscrow();
        c.add(
            core,
            BTSText.lang(g + "chk.core", next == 2 ? "II" : "III"),
            BTSText.lang(core ? "beyondthestars.gui.eye.chk.escrow.in" : "beyondthestars.gui.eye.chk.escrow.out"),
            -1,
            BTSText.lang(g + "chk.core.tip"));
        final boolean ring = structureTier() >= next;
        c.add(
            ring,
            BTSText.lang(g + "chk.ring", BTSText.key(g + (next == 2 ? "ring.stir" : "ring.outer"))),
            BTSText.lang(g + (ring ? "chk.ring.built" : "chk.ring.absent")),
            -1,
            pieceFault[next - 1].isEmpty() ? BTSText.lang(g + "chk.ring.tip", next)
                : BTSText.lit(pieceFault[next - 1]));
        return c;
    }

    /** True if piece {@code index} passed its own walk (whether or not it counts). Server state. */
    public boolean pieceStands(int index) {
        return mMachine && index >= 0 && index < PIECES.length && pieceBuilt[index];
    }

    // ==================================================================== construct / survival

    private static int tiersFor(ItemStack stack) {
        final int size = stack == null ? 1 : stack.stackSize;
        return Math.max(1, Math.min(MAX_TIER, size));
    }

    /** Trap 1: the trigger stack alone picks how many rings to project (1 = T1 .. 3 = T3). */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int n = tiersFor(stackSize);
        for (int i = 0; i < n; i++) {
            final int[] o = BTSShapes.offset(PIECES[i]);
            buildPiece(PIECES[i], stackSize, hintsOnly, o[0], o[1], o[2]);
        }
    }

    /** Survival auto-place in tier order; a ring starts once every piece before it is complete. -1 when done. */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final int n = tiersFor(stackSize);
        if (mMachine && structureTier() >= n) return -1;
        final int budget = BTSStructures.survivalBudget(elementBudget);
        for (int i = 0; i < n; i++) {
            final int[] o = BTSShapes.offset(PIECES[i]);
            final int built = survivalBuildPiece(PIECES[i], stackSize, o[0], o[1], o[2], budget, env, false, true);
            if (built >= 0) return built;
        }
        return -1;
    }

    // ===================================================================== upgrade: escrow, install

    /** The core that takes the Homogeny from {@code tier - 1} to {@code tier}, or null. */
    public static Item coreFor(int tier) {
        return tier == 2 ? SLMaxItems.homogenyTier2Core : tier == 3 ? SLMaxItems.homogenyTier3Core : null;
    }

    /** Litres of the current tier's product mixed so far (the milestone toward the next tier). */
    public long milestoneProgress() {
        return installedTier == 1 ? mixedEspm : installedTier == 2 ? mixedDegenerate : 0;
    }

    public boolean milestoneMet() {
        return installedTier < MAX_TIER && milestoneProgress() >= MILESTONE_LITRES;
    }

    public ItemStack escrow() {
        return escrow;
    }

    public boolean isInstalling() {
        return installing;
    }

    public double installFraction() {
        return installing ? Math.min(1.0, installTicks / (double) INSTALL_TICKS) : -1;
    }

    /** Why the install cannot start now, or "" if it can. Server state. */
    public String installBlocker() {
        if (installing) return "an install is already running";
        if (installedTier >= MAX_TIER) return "the Homogeny is at T" + MAX_TIER;
        final int next = installedTier + 1;
        if (!mMachine) return "the structure is not formed";
        if (structureTier() < next) return "build the " + PIECE_NAMES[next - 1] + " (T" + next + ") first";
        if (!milestoneMet()) {
            return "mix " + fmt(MILESTONE_LITRES)
                + " L of "
                + (installedTier == 1 ? "Excited Stellar Plasma Mixture" : "Degenerate Stellar Mixture")
                + " first ("
                + fmt(milestoneProgress())
                + " so far)";
        }
        if (escrow == null || escrow.getItem() != coreFor(next)) {
            return "right-click the Homogeny Tier " + (next == 2 ? "II" : "III") + " Core onto the controller";
        }
        return "";
    }

    /**
     * THE ONE INSTALL PATH, step 1 (rulings 4-5): starts the install run if {@link #installBlocker()} allows.
     * Returns the chat/GUI line. Server side.
     */
    public String beginInstall() {
        final String blocker = installBlocker();
        if (!blocker.isEmpty()) return "Cannot install: " + blocker + ".";
        installing = true;
        installTicks = 0;
        lastNote = "Install T" + (installedTier + 1) + " started";
        markDirtyBase();
        scheduleRecipeCheckImmediate(); // the idle "installing" result shows at once
        return "Install run started: " + INSTALL_TICKS / 20
            + " s offline at "
            + fmt(INSTALL_EUT[installedTier + 1])
            + " EU/t.";
    }

    /** THE ONE INSTALL PATH, step 2: the run is paid for; the core is used and the tier is permanent. */
    public void completeInstall() {
        installedTier = Math.min(MAX_TIER, installedTier + 1);
        escrow = null;
        installing = false;
        installTicks = 0;
        lastNote = "Installed T" + installedTier;
        markDirtyBase();
        // RC-2 recipe checks are event-driven: feeds that filled up during the run never fire a hatch event again,
        // so without this push the machine sat idle at its new tier until a hatch changed or it was toggled.
        scheduleRecipeCheckImmediate();
    }

    /** Creative / bench: sets the permanent installed tier (the kit's T{@code n} build). */
    public void debugInstall(int tier) {
        installedTier = Math.max(1, Math.min(MAX_TIER, tier));
        installing = false;
        installTicks = 0;
        markDirtyBase();
    }

    /** Bench: sets the milestone counters. */
    public void debugMilestones(long espm, long degenerate) {
        mixedEspm = Math.max(0, espm);
        mixedDegenerate = Math.max(0, degenerate);
        markDirtyBase();
    }

    /** Bench: runs {@code ticks} install ticks now (paid from the hatches as the real run is). Returns ticks done. */
    public int debugInstallTicks(int ticks) {
        int done = 0;
        for (int i = 0; i < ticks && installing; i++) {
            if (installTick()) done++;
        }
        return done;
    }

    /** Bench: puts a core into escrow without a player. */
    public void debugEscrow(ItemStack core) {
        escrow = core == null ? null : GTUtility.copyAmount(1, core);
        markDirtyBase();
    }

    /**
     * One install tick: pays the EU/t and advances; completes at the end. True if it advanced. An Install pressed while
     * a mix runs waits for that mix (no new one starts while installing), so a Homogeny fed without a gap (ME stocking)
     * needs no stopping first.
     */
    private boolean installTick() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (mMaxProgresstime > 0) {
            installStall = "waiting for the running mix to finish";
            return false;
        }
        if (!mMachine) {
            installStall = "the structure is not formed";
            return false;
        }
        if (base == null || !base.isAllowedToWork()) {
            installStall = "the controller is disabled";
            return false;
        }
        final long eut = INSTALL_EUT[Math.min(MAX_TIER, installedTier + 1)];
        if (!drainEnergyInput(eut)) {
            installStall = "not enough power (" + fmt(eut) + " EU/t)";
            return false;
        }
        installStall = "";
        installTicks++;
        if (installTicks >= INSTALL_TICKS) completeInstall();
        else if (installTicks % SAVE_EVERY_TICKS == 0) markDirtyBase(); // else a restart reloads the last save
        return true;
    }

    /** Right-click holding a Homogeny core: puts the next tier's core into escrow (one item). */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        final ItemStack held = aPlayer.getHeldItem();
        if (held != null
            && (held.getItem() == SLMaxItems.homogenyTier2Core || held.getItem() == SLMaxItems.homogenyTier3Core)) {
            if (aBaseMetaTileEntity.isServerSide()) {
                final String line;
                boolean taken = false;
                final Item wanted = coreFor(installedTier + 1);
                if (escrow != null) {
                    line = "The escrow already holds " + escrow.getDisplayName() + " (sneak-punch to take it back).";
                } else if (wanted == null) {
                    line = "The Homogeny is at T" + MAX_TIER + ": no core goes in.";
                } else if (held.getItem() != wanted) {
                    line = "This Homogeny is T" + installedTier
                        + ": it takes the "
                        + new ItemStack(wanted).getDisplayName()
                        + " next.";
                } else {
                    escrow = GTUtility.copyAmount(1, held);
                    taken = true;
                    if (!aPlayer.capabilities.isCreativeMode) {
                        held.stackSize--;
                        if (held.stackSize <= 0) {
                            aPlayer.inventory.setInventorySlotContents(aPlayer.inventory.currentItem, null);
                        }
                    }
                    markDirtyBase();
                    final String blocker = installBlocker();
                    line = escrow.getDisplayName() + " in escrow. "
                        + (blocker.isEmpty() ? "Press Install in the GUI (or use a screwdriver)."
                            : "Before Install: " + blocker + ".");
                }
                GTUtility
                    .sendChatToPlayer(aPlayer, (taken ? EnumChatFormatting.AQUA : EnumChatFormatting.YELLOW) + line);
            }
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer);
    }

    /** Sneak-punch with an empty hand returns the escrowed core, cancelling an install in progress. */
    @Override
    public void onLeftclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        super.onLeftclick(aBaseMetaTileEntity, aPlayer);
        if (!aBaseMetaTileEntity.isServerSide() || aPlayer == null
            || !aPlayer.isSneaking()
            || aPlayer.getHeldItem() != null
            || escrow == null) return;
        // QA-B15: only the owner (or the owner's team) takes the escrowed core and cancels an install
        if (!OwnerGate.check(aBaseMetaTileEntity, aPlayer, "take the escrowed core")) return;
        final ItemStack core = escrow;
        final boolean cancelled = installing;
        escrow = null;
        installing = false;
        installTicks = 0;
        markDirtyBase();
        if (cancelled) scheduleRecipeCheckImmediate(); // mixing resumes without waiting for a hatch event
        if (!aPlayer.inventory.addItemStackToInventory(core)) aPlayer.dropPlayerItemWithRandomChoice(core, false);
        GTUtility.sendChatToPlayer(
            aPlayer,
            "Escrow returned: " + core.getDisplayName() + (cancelled ? " (the install run was cancelled)." : "."));
    }

    /** Screwdriver: starts the install run (the GUI's Install button), or says what is missing. */
    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        GTUtility.sendChatToPlayer(aPlayer, beginInstall());
    }

    private void markDirtyBase() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) base.markDirty();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aTick % 20 == 0) {
            if (mMachine) burstSecond(aBaseMetaTileEntity);
            if (aTick % 40 == 0) sendRenderDataToClient(this);
            // Install progress, milestones, the pending mix totals and the burst counters change without an
            // inventory change, and GT marks a chunk for saving only on those: once a second, as the KEE and the
            // P4 machines do (B-112, P4-85).
            aBaseMetaTileEntity.markDirty();
        }
        if (aBaseMetaTileEntity.isServerSide() && !installing && mMaxProgresstime > 0 && !runConditionHolds())
            aBaseMetaTileEntity.setActive(false);
        if (!aBaseMetaTileEntity.isServerSide() || !installing) return;
        if (mStartUpCheck >= 0) return; // not walked yet after a load
        final boolean advanced = installTick();
        if (mMaxProgresstime == 0) aBaseMetaTileEntity.setActive(advanced); // a finishing mix keeps its own lamp
    }

    // ============================================================================ renders v2

    /** Renders v2 (§11): the vortex, the helix glow and the feed streams (client; HomogenyRender). */
    @Override
    public void renderTESR(double x, double y, double z, float partialTicks) {
        HomogenyRender.render(this, x, y, z, partialTicks);
    }

    /** The whole machine (its centre stands 34 blocks from the controller, the T3 ring 70 from the centre). */
    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x - 110, y - 40, z - 110, x + 111, y + 41, z + 111);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256.0 * 256.0;
    }

    /** Server: the tier (0 unformed) and the feed and product colours, every 2 s (render-only, never saved). */
    @Override
    public void encodeRenderData(ByteBuf buf) {
        final int t = mMachine ? Math.max(1, tier()) : 0;
        buf.writeByte(t);
        final int[] pal = RENDER_PALETTE[Math.max(1, Math.min(MAX_TIER, t))];
        final int[] c = pal.clone();
        int n = 0;
        final ArrayList<FluidStack> stored = mMachine ? getStoredFluids() : null;
        if (stored != null) {
            for (FluidStack fs : stored) {
                if (n >= 3 || fs == null || fs.getFluid() == null || fs.amount <= 0) continue;
                final int rgb = fs.getFluid()
                    .getColor(fs) & 0xFFFFFF;
                if (rgb == 0xFFFFFF || rgb == 0) continue;
                boolean dup = false;
                for (int i = 0; i < n; i++) dup |= c[i] == rgb;
                if (!dup) c[n++] = rgb;
            }
        }
        if (mOutputFluids != null && mMaxProgresstime > 0) {
            for (FluidStack out : mOutputFluids) {
                if (out == null || out.getFluid() == null) continue;
                final int rgb = out.getFluid()
                    .getColor(out) & 0xFFFFFF;
                if (rgb != 0xFFFFFF && rgb != 0) lastProductColour = rgb;
                break;
            }
        }
        if (lastProductColour >= 0) c[3] = lastProductColour;
        for (int v : c) buf.writeInt(v);
    }

    /** Client. */
    @Override
    public void decodeRenderData(ByteArrayDataInput in) {
        renderTier = in.readByte();
        for (int i = 0; i < 4; i++) renderColours[i] = in.readInt();
    }

    /** Client: the tier the vortex shows (0: draw nothing). */
    public int renderTier() {
        return renderTier;
    }

    /** Client: feed colour 0..2 or the product colour 3, RGB. */
    public int renderColour(int i) {
        return renderColours[i];
    }

    // ============================================================================ processing

    @Override
    public RecipeMap<?> getRecipeMap() {
        return MAP;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (recipe.mSpecialValue > tier()) {
                    return SimpleCheckRecipeResult.ofFailure("beyondthestars.homogeny.tier");
                }
                if (quasarSigma() < minSigma(recipe)) {
                    return SimpleCheckRecipeResult.ofFailure("beyondthestars.homogeny.stability");
                }
                return super.validateRecipe(recipe);
            }

            @Nonnull
            @Override
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return super.createOverclockCalculator(recipe).setNoOverclock(true);
            }
        }.setMaxParallelSupplier(this::maxParallel)
            .setSpeedBonusSupplier(() -> 1.0 / speedFactor());
    }

    // ======================================================================= P4: the quasar link

    /** This Homogeny's burst demand D_h: 1.5 Bu/t at T2, 6 at T3 (quasar-p4.md §3.1); 0 at T1 (no ring, no ports). */
    public double burstDemand() {
        final int t = tier();
        return t >= 3 ? BurstLink.HOMOGENY_T3 : t == 2 ? BurstLink.HOMOGENY_T2 : 0;
    }

    /**
     * The Quasar link's σ at this host on the 60 s mean, read from GRB Receptor Ports in the stirring ring's {@code g}
     * cells; 0 with none. Q-8: 0 while its quasar is frozen or unloaded (the speed bonus and Entanglement Medium pause;
     * the mean is held for when it returns).
     */
    public double quasarSigma() {
        return receptorCount() == 0 || burstDemand() <= 0 ? 0 : burstHost.sigmaInForce();
    }

    /**
     * The speed factor σ gives at T2 / T3: 1 + 0.5·min(σ, 1), so the vortex runs at up to 6x (T2, 4 parallels) or 24x
     * (T3, 16 parallels) the T1 rate. 1 at T1 or with no link. Fixed when a mix starts.
     */
    public double speedFactor() {
        return tier() >= 2 ? 1.0 + 0.5 * Math.min(1.0, Math.max(0, quasarSigma())) : 1.0;
    }

    /**
     * The sigma a recipe needs: Entanglement Medium (SL-H21) needs 1. Degenerate Stellar Mixture never does (Eye T6
     * needs it before any quasar exists), nor does anything else.
     */
    static double minSigma(GTRecipe recipe) {
        if (recipe == null || recipe.mFluidOutputs == null) return 0;
        for (FluidStack out : recipe.mFluidOutputs) {
            if (out != null && out.getFluid() != null && out.getFluid() == SLRealityFluids.entanglementMedium) return 1;
        }
        return 0;
    }

    /** True while the running mix may progress: a mix that needs σ ≥ 1 pauses below it (kept, never voided). */
    private boolean runConditionHolds() {
        return !runNeedsLink || quasarSigma() >= 1.0;
    }

    /** Once a second: read the ring's ports (they learn their host) and report to the feeding quasars. */
    private void burstSecond(IGregTechTileEntity base) {
        if (receptors.isEmpty() || base.getWorld() == null) return;
        final String key = "homogeny@" + base.getWorld().provider.dimensionId
            + ":"
            + base.getXCoord()
            + ","
            + base.getYCoord()
            + ","
            + base.getZCoord();
        burstHost.second(receptors, key, "Homogeny", Math.max(BurstLink.HOMOGENY_T2, burstDemand()), 0);
    }

    public BurstHost burstHost() {
        return burstHost;
    }

    /** "Quasar link σ 1.02: speed x1.50" for the GUI / scanner; "" with no receptor port. Server state. */
    public String burstLine() {
        if (receptorCount() == 0) return "";
        // QA-B44: at installed T1 the ports report a demand to the quasar but give no speed
        if (burstDemand() <= 0)
            return "Quasar link: it speeds the Homogeny up from installed T2 (" + burstHost.linkLine() + ")";
        if (burstHost.paused()) return burstHost.linkLine() + " (link paused: speed x1, Entanglement Medium paused)";
        return String.format(
            Locale.ROOT,
            "Quasar link σ %.2f (demand %s): speed x%.2f%s; %s",
            quasarSigma(),
            com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(burstDemand()),
            speedFactor(),
            quasarSigma() >= 1 ? ", Entanglement Medium open" : "",
            burstHost.linkLine());
    }

    /**
     * QA-B08: a missing block (or a footprint chunk that is not loaded yet after a restart) pauses the machine instead
     * of voiding its work. GT calls this every tick while the structure is incomplete; the running recipe, its
     * progress, its outputs and everything drawn for it are kept, and the controller stays enabled, so the run
     * resumes by itself the tick the structure is whole again. Every other reason stops as GT does.
     */
    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        if (ShutDownReasonRegistry.STRUCTURE_INCOMPLETE.getID()
            .equals(reason.getID())) return;
        super.stopMachine(reason);
    }

    /** "the run is kept and resumes when the structure is whole again" while a run waits on a broken structure. */
    public String structurePauseNote() {
        return !mMachine && mMaxProgresstime > 0
            ? "PAUSED: the structure is incomplete; the run is kept and resumes when it is whole again"
            : "";
    }

    /** Pauses a running Entanglement Medium mix while σ is below 1 (never voided, quasar-p4.md §3.1). */
    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (mMaxProgresstime > 0 && !runConditionHolds()) return;
        super.runMachine(aBaseMetaTileEntity, aTick);
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        pendingEspm = 0;
        pendingDegenerate = 0;
        runNeedsLink = false;
        if (installing) return SimpleCheckRecipeResult.ofFailure("beyondthestars.homogeny.installing");
        final CheckRecipeResult result = super.checkProcessing();
        if (result.wasSuccessful() && mOutputFluids != null) {
            for (FluidStack out : mOutputFluids) {
                if (out == null) continue;
                final Fluid f = out.getFluid();
                if (f == SLFluids.excitedStellarPlasma) pendingEspm += out.amount;
                else if (f == SLRealityFluids.degenerateStellarMixture) pendingDegenerate += out.amount;
                else if (f == SLRealityFluids.entanglementMedium) runNeedsLink = true;
            }
        }
        return result;
    }

    /** The milestones count what was actually mixed (a run stopped part-way counts nothing). */
    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        mixedEspm += pendingEspm;
        mixedDegenerate += pendingDegenerate;
        pendingEspm = 0;
        pendingDegenerate = 0;
        markDirtyBase();
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    // ===================================================================================== GUI

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new HomogenyGui(this);
    }

    public boolean isFormedForGui() {
        return mMachine;
    }

    /** GUI cleanup (controller-gui-spec.md §4.4): the kit's badge says it; GT's "Running perfectly." is not added. */
    @Override
    public boolean hasRunningText() {
        return false;
    }

    // ===================================================================================== NBT

    /** The ONE serializer of the persisted state (trap 2), for tile and item NBT alike. */
    private void writeState(NBTTagCompound nbt) {
        nbt.setInteger("btsHomTier", installedTier);
        nbt.setLong("btsHomEspm", mixedEspm);
        nbt.setLong("btsHomDegenerate", mixedDegenerate);
        if (escrow != null) nbt.setTag("btsHomEscrow", escrow.writeToNBT(new NBTTagCompound()));
        if (installing) {
            nbt.setBoolean("btsHomInstalling", true);
            nbt.setInteger("btsHomInstallTicks", installTicks);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeState(aNBT);
        aNBT.setLong("btsHomPendEspm", pendingEspm);
        aNBT.setLong("btsHomPendDeg", pendingDegenerate);
        aNBT.setString("btsHomNote", lastNote);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeState(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        installedTier = aNBT.hasKey("btsHomTier") ? Math.max(1, Math.min(MAX_TIER, aNBT.getInteger("btsHomTier"))) : 1;
        mixedEspm = aNBT.getLong("btsHomEspm");
        mixedDegenerate = aNBT.getLong("btsHomDegenerate");
        escrow = aNBT.hasKey("btsHomEscrow") ? ItemStack.loadItemStackFromNBT(aNBT.getCompoundTag("btsHomEscrow"))
            : null;
        installing = aNBT.getBoolean("btsHomInstalling");
        installTicks = aNBT.getInteger("btsHomInstallTicks");
        pendingEspm = aNBT.getLong("btsHomPendEspm");
        pendingDegenerate = aNBT.getLong("btsHomPendDeg");
        runNeedsLink = false;
        if (mOutputFluids != null) {
            for (FluidStack out : mOutputFluids) {
                if (out != null && out.getFluid() != null && out.getFluid() == SLRealityFluids.entanglementMedium) {
                    runNeedsLink = true;
                }
            }
        }
        lastNote = aNBT.getString("btsHomNote");
    }

    /** Bench {@code wrench}: what picking the controller up and placing it back keeps (item NBT only). */
    public void debugWrench() {
        final NBTTagCompound item = new NBTTagCompound();
        setItemNBT(item);
        loadNBTData(item);
    }

    // ============================================================================ panel/tooltip

    static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    /** "T2 (installed T2, structure T3), 4 parallels". Server state. */
    public String headLine() {
        if (!mMachine) return "Not formed (installed T" + installedTier
            + ")"
            + (structurePauseNote().isEmpty() ? "" : ": " + structurePauseNote());
        return "Homogeny T" + tier()
            + " (installed T"
            + installedTier
            + ", structure T"
            + structureTier()
            + "), "
            + maxParallel()
            + (maxParallel() == 1 ? " parallel" : " parallels");
    }

    /** "stirring ring FORMED, outer ring + tail absent (...)". Server state. */
    public String ringLine() {
        final StringBuilder sb = new StringBuilder("Rings: ");
        for (int i = 1; i < PIECES.length; i++) {
            if (i > 1) sb.append(", ");
            sb.append(PIECE_NAMES[i])
                .append(' ')
                .append(
                    pieceBuilt[i] ? "FORMED" : pieceFault[i].isEmpty() ? "absent" : "absent (" + pieceFault[i] + ")");
        }
        return sb.toString();
    }

    /** The upgrade line: milestone, escrow, install state. "" at T3. Server state. */
    public String upgradeLine() {
        if (installedTier >= MAX_TIER && !installing) return "";
        final int next = installedTier + 1;
        final StringBuilder sb = new StringBuilder("Upgrade to T").append(next)
            .append(": milestone ")
            .append(fmt(Math.min(MILESTONE_LITRES, milestoneProgress())))
            .append(" / ")
            .append(fmt(MILESTONE_LITRES))
            .append(" L ")
            .append(installedTier == 1 ? "ESPM" : "Degenerate")
            .append(", core ")
            .append(escrow == null ? "not in escrow" : "in escrow")
            .append(", ring ")
            .append(structureTier() >= next ? "built" : "missing");
        return sb.toString();
    }

    /** The install line while one runs, else what Install would say. Server state. */
    public String installLine() {
        if (installing) {
            return "Installing T" + (installedTier + 1)
                + ": "
                + String.format(Locale.ROOT, "%.1f%%", installFraction() * 100)
                + " ("
                + fmt((INSTALL_TICKS - installTicks) / 20)
                + " s left)"
                + (installStall.isEmpty() ? "" : ", paused: " + installStall);
        }
        if (installedTier >= MAX_TIER) return "";
        final String blocker = installBlocker();
        return blocker.isEmpty() ? "Ready to install: press Install" : "Install: " + blocker;
    }

    public String lastNote() {
        return lastNote;
    }

    /** The status lines the scanner and the creative build print. Server state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(headLine());
        out.add(ringLine());
        out.add(
            "Sockets: feed " + mInputHatches.size()
                + ", output "
                + mOutputHatches.size()
                + ", energy "
                + getExoticAndNormalEnergyHatchList().size());
        out.add(
            "Mixed so far: " + fmt(mixedEspm)
                + " L Excited Stellar Plasma Mixture, "
                + fmt(mixedDegenerate)
                + " L Degenerate Stellar Mixture");
        final String up = upgradeLine();
        if (!up.isEmpty()) out.add(up);
        final String burst = burstLine();
        if (!burst.isEmpty()) out.add(burst);
        if (mMaxProgresstime > 0 && !runConditionHolds()) {
            out.add("Entanglement Medium PAUSED: the quasar link is below σ 1 (the mix is kept)");
        }
        final String inst = installLine();
        if (!inst.isEmpty()) out.add(inst);
        if (!lastNote.isEmpty()) out.add("Last: " + lastNote);
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

    /** Assembled {w, h, d} of the first {@code n} pieces, from their sizes and offsets. */
    static int[] assembledSize(String[] pieces, int n) {
        final int[] lo = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        final int[] hi = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        try {
            for (int i = 0; i < n; i++) {
                final int[] o = BTSShapes.offset(pieces[i]);
                final int[] sz = BTSShapes.size(pieces[i]);
                for (int k = 0; k < 3; k++) {
                    lo[k] = Math.min(lo[k], -o[k]);
                    hi[k] = Math.max(hi[k], sz[k] - 1 - o[k]);
                }
            }
        } catch (RuntimeException missing) {
            return new int[] { 1, 1, 1 };
        }
        return new int[] { hi[0] - lo[0] + 1, hi[1] - lo[1] + 1, hi[2] - lo[2] + 1 };
    }

    private static String size(int[] s) {
        return s[0] + " x " + s[1] + " x " + s[2];
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] t1 = assembledSize(PIECES, 1);
        final int[] t2 = assembledSize(PIECES, 2);
        final int[] t3 = assembledSize(PIECES, 3);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Mixer, Homogeny")
            .addInfo("A mega mixer. One controller, three tiers: build the next ring, then install its core.")
            .addInfo(
                EnumChatFormatting.AQUA + "T1 (UMV)"
                    + EnumChatFormatting.GRAY
                    + ": Raw Star Matter 100 : 1 into Excited Stellar Plasma Mixture. 1 parallel.")
            .addInfo(
                EnumChatFormatting.AQUA + "T2 (UXV)"
                    + EnumChatFormatting.GRAY
                    + ": 4 parallels. Unlocks Degenerate Stellar Mixture (White + Black Dwarf Matter in ESPM).")
            .addInfo(
                EnumChatFormatting.AQUA + "T3 (MAX)"
                    + EnumChatFormatting.GRAY
                    + ": 16 parallels. Unlocks Entanglement Medium, for Reality Diffraction.")
            .addInfo("No overclock: each parallel draws the recipe's EU/t.")
            .addInfo("No hydrogen or helium, no ignition, no maintenance: it runs unattended.")
            .addInfo("Quasar link (optional): GRB Receptor Ports in the stirring ring's 3 port cells (hint 4).")
            .addInfo("  σ = bursts ÷ 1.5 Bu/t (T2, 1.5e13 EU/t) or 6 (T3, 6.0e13 EU/t), 60 s mean. Speed")
            .addInfo("  x(1 + 0.5 min(σ, 1)): up to")
            .addInfo("  6x the T1 rate at T2, 24x at T3. Entanglement Medium needs σ ≥ 1 (a mix that loses it")
            .addInfo("  pauses, never voids). Degenerate Stellar Mixture never needs bursts.")
            .addInfo("It costs 100 L of Raw Star Matter per 1 L: 10x what an Eye T2 (Sirius A) needs.")
            .addInfo("  Use it to bank a Sol surplus or run a hands-off line. It does not replace")
            .addInfo("  Sirius A: Eye T2 is still required, and only an Eye hosting Sol makes Raw Star Matter.")
            .addSeparator()
            .addInfo(EnumChatFormatting.GOLD + "Upgrading" + EnumChatFormatting.GRAY + " (permanent):")
            .addInfo("  1. Build the next ring (the hologram's next stack size).")
            .addInfo("  2. Mix " + fmt(MILESTONE_LITRES) + " L of the current tier's product (ESPM, then Degenerate).")
            .addInfo("  3. Right-click the next Homogeny Core onto the controller (sneak-punch returns it).")
            .addInfo(
                "  4. Press Install (GUI or screwdriver): " + INSTALL_TICKS / 20 / 60
                    + " min offline at "
                    + fmt(TierEU.RECIPE_UXV)
                    + " (T2) / "
                    + fmt(TierEU.RECIPE_MAX)
                    + " (T3) EU/t.")
            .addInfo("  A mix already running finishes first; no new mix starts while installing.")
            .addInfo("  Progress shows in the GUI and the scanner, and survives a wrench.")
            .addInfo("Automation: feed hatches take ME input hatches; outputs go to ME output hatches.")
            .beginVariableStructureBlock(t1[0], t3[0], t1[1], t3[1], t1[2], t3[2], false)
            .addController("Tip of the volute's discharge throat")
            .addCasingInfoExactly("Stirring Band Casing", bom(PIECE_T1, 'A'), false)
            .addCasingInfoExactly("Shear Helix Coil", bom(PIECE_T1, 'B'), false)
            .addCasingInfoExactly("Feed Vessel Casing", bom(PIECE_T1, 'C') + bom(PIECE_T2, 'C'), false)
            .addCasingInfoExactly("Vortex Inlet", bom(PIECE_T1, 'D') + bom(PIECE_T2, 'D'), false)
            .addCasingInfoExactly("Bastion Plate", bom(PIECE_T1, 'O') + bom(PIECE_T2, 'O') + bom(PIECE_T3, 'O'), false)
            .addCasingInfoExactly(
                "Harmonic Lining",
                bom(PIECE_T1, 'J') + bom(PIECE_T2, 'J') + bom(PIECE_T3, 'J'),
                false)
            .addCasingInfoExactly(
                "Quantum Glass",
                bom(PIECE_T1, 'Z') + bom(PIECE_T1, 'K') + bom(PIECE_T2, 'K') + bom(PIECE_T2, 'Z') + bom(PIECE_T3, 'K'),
                false)
            .addCasingInfoExactly("Reinforced Temporal Structure Casing", bom(PIECE_T1, 'M'), false)
            .addCasingInfoExactly(
                "Region Crystal (or lens glass at the throat tip)",
                bom(PIECE_T1, 'E') + bom(PIECE_T3, 'E'),
                false)
            .addOtherStructurePart(
                "T2 stirring ring",
                bom(PIECE_T2, 'G') + bom(PIECE_T2, 'g') + " Galactic Shell Plating, 3 more vessels")
            .addOtherStructurePart(
                "T3 outer ring + tail",
                bom(PIECE_T3, 'F') + " Eternity Shell Casing, " + bom(PIECE_T3, 'P') + " Casimir Plate")
            .addOtherStructurePart("Input Hatch (fluid)", "top of each Feed Vessel (3 at T1, 6 at T2+)", 1)
            .addOtherStructurePart("Output Hatch", "the throat, either face", 2)
            .addOtherStructurePart("Energy or Laser Hatch", "bottom cap rim", 3)
            .addStructureInfo("Sizes: T1 " + size(t1) + ", T2 " + size(t2) + ", T3 " + size(t3) + ".")
            .addStructureInfo("Rings are cumulative: the outer ring counts only with the stirring ring.")
            .addOtherStructurePart("GRB Receptor Port (optional)", "3 cells on the stirring ring's outer face", 4)
            .addStructureInfo("Hologram: stack 1 = T1, 2 = + stirring ring, 3 = + outer ring. NEI previews each tier.")
            .addStructureInfo("Builds never overwrite hatches or other tiles.")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ReinforcedTemporalStructureCasing.getCasingTexture();
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
