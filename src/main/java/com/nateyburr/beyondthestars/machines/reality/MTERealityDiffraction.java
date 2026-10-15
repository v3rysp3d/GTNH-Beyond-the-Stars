package com.nateyburr.beyondthestars.machines.reality;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing3;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.conduit.ReceptorHatchElement;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): REALITY DIFFRACTION, MTE base+31 (28131), my "reality
 * diffraction" sketch (design repo {@code designs/docs/homogeny-reality.md} §2, PROPOSED; D-43).
 * <p>
 * "Entangle inputs throughout 6 regions outside of reality": each run splits Entanglement Medium into six parts, one
 * per region, and puts them into the BEC network as the six Entangled Region condensates, where Reality Refab grabs
 * them back. One parallel, no overclock, RECIPE_MAX (SL-R03; every amount, the time and the EU/t are read from the
 * map's recipe in {@link RealityRecipeMaps#DIFFRACTION}).
 * <ul>
 * <li><b>Region Anchors</b> (H-13, the light stability hook): at the start of a run each region checks its own
 * socket. An anchored region (its anchor fluid stocked in its own socket) is paid and yields its 1,000 L; an
 * unanchored region yields nothing, its sixth of the medium is lost. No region anchored: the run does not start.</li>
 * <li><b>BEC</b>: the outputs are injected at the end of the run. Whatever the network does not take (no running
 * Containment Field) waits in the controller and is retried every second; the next run waits until it is gone, so
 * nothing is ever voided.</li>
 * </ul>
 */
public class MTERealityDiffraction extends MTERealityBase<MTERealityDiffraction> {

    public static final String PIECE = "reality_diffraction";
    public static final String[] REGION_NAMES = { "I Hyperspace", "II Subspace", "III Forward Time", "IV Reverse Time",
        "V Positive Mass", "VI Negative Mass" };
    /** Socket chars of Regions I..VI in the exported shape. */
    public static final char[] ANCHOR_CHARS = { 'r', 's', 't', 'u', 'v', 'w' };

    private static final RecipeMap<?> MAP = RealityRecipeMaps.DIFFRACTION;

    private static final IIconContainer[] OVERLAYS = {
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_DIFFRACTION"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_DIFFRACTION_GLOW"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_DIFFRACTION_ACTIVE"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_DIFFRACTION_ACTIVE_GLOW") };

    private static volatile IStructureDefinition<MTERealityDiffraction> STRUCTURE_DEFINITION;

    // ---- walk state (private anchor sockets, one list per region)
    @SuppressWarnings("unchecked")
    private final List<MTEHatchInput>[] anchorHatches = new List[] { new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };

    /** P4: GRB Receptor Ports in the six portal {@code g} cells (walk state). */
    private final List<MTEHatchGRBReceptor> receptors = new ArrayList<>();
    /** P4: the quasar link (never saved: nothing about a link is). */
    private final BurstHost burstHost = new BurstHost();

    // ---- run state (tile NBT)
    /** Bit k: region k+1 was anchored at the start of the running run. */
    private int runMask;
    /** Litres of each region waiting to go into the network. */
    private final long[] pending = new long[6];
    private long runs;

    public MTERealityDiffraction(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTERealityDiffraction(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTERealityDiffraction(this.mName);
    }

    @Override
    public String piece() {
        return PIECE;
    }

    @Override
    protected IIconContainer[] overlays() {
        return OVERLAYS;
    }

    // =================================================================================== shape

    @Override
    public IStructureDefinition<MTERealityDiffraction> getStructureDefinition() {
        IStructureDefinition<MTERealityDiffraction> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTERealityDiffraction.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTERealityDiffraction> buildDefinition() {
        final Block c3 = SLBlocks.slCasing3;
        final int index = hatchIndex();
        final StructureDefinition.Builder<MTERealityDiffraction> b = StructureDefinition
            .<MTERealityDiffraction>builder()
            .addShape(PIECE, com.nateyburr.beyondthestars.machines.BTSShapes.load(PIECE))
            .addElement('A', block(c3, BlockSLCasing3.REGION_DECK_BAND))
            .addElement('E', block(c3, BlockSLCasing3.REGION_CRYSTAL))
            .addElement('H', block(c3, BlockSLCasing3.PHASE_RING))
            .addElement('I', block(c3, BlockSLCasing3.REGION_ANCHOR))
            .addElement('J', block(SLBlocks.slCasing2, BlockSLCasing2.LINING))
            .addElement('K', casing(Casings.QuantumGlass))
            .addElement('Z', casing(Casings.QuantumGlass))
            .addElement('L', casing(Casings.SpatiallyTranscendentGravitationalLens))
            .addElement('N', casing(Casings.CoherencePreservingPlasmaConduit))
            .addElement('Q', casing(Casings.ElectromagneticallyIsolatedCasing))
            // P4 (quasar-p4.md section 3.1): a GRB Receptor Port, or the deck band these six portal cells held before
            // P4, so builds that stand keep forming. Top deck band, outer face, one per region. Hologram hint 10.
            .addElement(
                'g',
                buildHatchAdder(MTERealityDiffraction.class)
                    .atLeast(
                        ReceptorHatchElement.<MTERealityDiffraction>of(
                            MTERealityDiffraction::addReceptor,
                            MTERealityDiffraction::receptorCount))
                    .casingIndex(index)
                    .hint(10)
                    .buildAndChain(block(c3, BlockSLCasing3.REGION_DECK_BAND)))
            .addElement(
                'b',
                buildHatchAdder(MTERealityDiffraction.class).atLeast(RealityHatchElement.Bec)
                    .casingIndex(index)
                    .hint(1)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)))
            .addElement(
                'h',
                buildHatchAdder(MTERealityDiffraction.class).atLeast(InputHatch)
                    .casingIndex(index)
                    .hint(2)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)))
            .addElement(
                'e',
                buildHatchAdder(MTERealityDiffraction.class).atLeast(Energy.or(ExoticEnergy))
                    .casingIndex(index)
                    .hint(9)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)));
        for (int i = 0; i < 6; i++) {
            b.addElement(
                ANCHOR_CHARS[i],
                buildHatchAdder(MTERealityDiffraction.class).atLeast(RealityHatchElement.anchor(i))
                    .casingIndex(index)
                    .hint(3 + i)
                    .buildAndChain(block(c3, BlockSLCasing3.REGION_ANCHOR)));
        }
        return b.build();
    }

    private static IStructureElement<MTERealityDiffraction> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTERealityDiffraction> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        for (List<MTEHatchInput> l : anchorHatches) l.clear();
        receptors.clear();
        final boolean ok = walk(errors);
        afterWalk();
        if (!ok) return;
        if (becHatches.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.reality.bec"));
        if (mInputHatches.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.reality.medium"));
        if (getExoticAndNormalEnergyHatchList().isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.reality.energy"));
        }
    }

    public boolean addAnchorHatch(int region, IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInput) {
            ((MTEHatchInput) mte).updateTexture(casingIndex);
            // Watch the anchor (GT's clearHatches drops the watcher again): refilling it after a "reality.anchors"
            // failure then pushes a recipe check like any input hatch does.
            addIfSmartInput(mte);
            anchorHatches[region].add((MTEHatchInput) mte);
            return true;
        }
        return false;
    }

    /** P4: a GRB Receptor Port in a portal {@code g} cell. */
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
        for (MTEHatchGRBReceptor r : receptors) if (live(r)) n++;
        return n;
    }

    /** The portal's receptor ports (server state, read-only). */
    public List<MTEHatchGRBReceptor> receptors() {
        return java.util.Collections.unmodifiableList(receptors);
    }

    public int anchorHatchCount(int region) {
        int n = 0;
        for (MTEHatchInput h : anchorHatches[region]) if (live(h)) n++;
        return n;
    }

    // ============================================================================ processing

    @Override
    public RecipeMap<?> getRecipeMap() {
        return MAP;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return java.util.Collections.singletonList(MAP);
    }

    /** SL-R03 (the only recipe in the map), or null if its fluids are missing. */
    public static GTRecipe recipe() {
        final Collection<GTRecipe> all = RealityRecipeMaps.DIFFRACTION.getAllRecipes();
        for (GTRecipe r : all) {
            if (r.mFluidInputs != null && r.mFluidInputs.length >= 7
                && r.mFluidOutputs != null
                && r.mFluidOutputs.length >= 6) return r;
        }
        return null;
    }

    /** Renders v2: the regions anchored now (bits 0-5), or those of the running run. */
    @Override
    protected int renderMaskNow() {
        if (mMaxProgresstime > 0 && runMask != 0) return runMask;
        return anchorMask() & 0x3F;
    }

    /** True if region {@code k}'s socket holds its anchor for one run. Server state. */
    public boolean anchored(int k) {
        final GTRecipe r = recipe();
        if (r == null || anchorHatchCount(k) == 0) return false;
        final FluidStack a = r.mFluidInputs[k + 1];
        return a != null && available(anchorHatches[k], a.getFluid()) >= a.amount;
    }

    /** Bits 0-5: anchored now; bits 8-13: the region's socket has a hatch. Server state (GUI lamps). */
    public int anchorMask() {
        int m = 0;
        for (int k = 0; k < 6; k++) {
            if (anchorHatchCount(k) > 0) m |= 1 << (8 + k);
            if (anchored(k)) m |= 1 << k;
        }
        return m;
    }

    /**
     * P4 (quasar-p4.md section 3.1): with GRB Receptor Ports fitted the regions ride the burst stream: a run needs
     * σ ≥ 0.5 (60 s mean, demand D₉ = 24 Bu/t), pauses below it (kept, never voided) and runs at speed x min(σ, 1.5).
     * With no port fitted the regions are anchored by their fluids alone and run at x1: no host needs a link (P4-74).
     */
    public static final double MIN_SIGMA = 0.5;
    /** P4: the speed cap σ can give. */
    public static final double MAX_SPEED = 1.5;

    /**
     * The Quasar link's σ at this host on the 60 s mean; 0 while its quasar is frozen or unloaded (Q-8: the
     * burst-driven regions pause; the mean is held for when it returns).
     */
    public double quasarSigma() {
        return receptorCount() == 0 ? 0 : burstHost.sigmaInForce();
    }

    /** True when the regions ride the burst stream (at least one receptor port fitted). */
    public boolean burstDriven() {
        return receptorCount() > 0;
    }

    /** The run speed: x1 with no port; else x min(σ, 1.5), 0 below σ 0.5 (paused). */
    public double speedFactor() {
        if (!burstDriven()) return 1.0;
        final double s = quasarSigma();
        return s < MIN_SIGMA ? 0 : Math.min(MAX_SPEED, s);
    }

    public BurstHost burstHost() {
        return burstHost;
    }

    /** "Quasar link σ 1.02: speed x1.02" / "PAUSED ..."; "" with no port. Server state. */
    public String burstLine() {
        if (!burstDriven()) return "";
        if (burstHost.paused()) return burstHost.linkLine() + " (runs paused)";
        final double s = quasarSigma();
        return String.format(
            java.util.Locale.ROOT,
            "Quasar link σ %.2f (demand %s): %s; %s",
            s,
            com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(BurstLink.REALITY),
            s < MIN_SIGMA ? "regions UNSTABLE, runs pause below σ 0.5"
                : String.format(java.util.Locale.ROOT, "speed x%.2f", speedFactor()),
            burstHost.linkLine());
    }

    /** Pauses a running run while the burst-driven regions are below σ 0.5 (kept, never voided). */
    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (mMaxProgresstime > 0 && burstDriven() && quasarSigma() < MIN_SIGMA) {
            aBaseMetaTileEntity.setActive(false);
            return;
        }
        super.runMachine(aBaseMetaTileEntity, aTick);
    }

    private long mediumAvailable(Fluid medium) {
        long n = 0;
        for (FluidStack s : getStoredFluids()) {
            if (s != null && s.getFluid() == medium) n += s.amount;
        }
        return n;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        if (pendingTotal() > 0) {
            flushPending();
            if (pendingTotal() > 0) return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.full");
        }
        final GTRecipe r = recipe();
        if (r == null) return CheckRecipeResultRegistry.NO_RECIPE;
        if (burstDriven() && quasarSigma() < MIN_SIGMA) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.stability");
        }
        if (network == null || !hasStorage()) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.nonetwork");
        }
        if (getMaxInputEu() < r.mEUt) return CheckRecipeResultRegistry.insufficientPower(r.mEUt);
        final FluidStack medium = r.mFluidInputs[0];
        if (mediumAvailable(medium.getFluid()) < medium.amount) return CheckRecipeResultRegistry.NO_RECIPE;
        int mask = 0;
        for (int k = 0; k < 6; k++) if (anchored(k)) mask |= 1 << k;
        if (mask == 0) return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.anchors");
        if (!depleteInput(medium.copy())) return CheckRecipeResultRegistry.NO_RECIPE;
        for (int k = 0; k < 6; k++) {
            if ((mask & (1 << k)) == 0) continue;
            final FluidStack a = r.mFluidInputs[k + 1];
            if (take(anchorHatches[k], a.getFluid(), a.amount) < a.amount) mask &= ~(1 << k);
        }
        runMask = mask;
        mOutputItems = null;
        mOutputFluids = null;
        lEUt = -r.mEUt;
        mEfficiency = 10_000;
        mEfficiencyIncrease = 10_000;
        mMaxProgresstime = Math.max(1, (int) Math.ceil(r.mDuration / Math.max(0.01, speedFactor())));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        final GTRecipe r = recipe();
        if (r == null || runMask == 0) {
            runMask = 0;
            return;
        }
        int n = 0;
        for (int k = 0; k < 6; k++) {
            if ((runMask & (1 << k)) == 0) continue;
            pending[k] += r.mFluidOutputs[k].amount;
            n++;
        }
        runs++;
        lastNote = "Run " + runs + ": " + n + " of 6 regions entangled" + (n < 6 ? " (an anchor was missing)" : "");
        runMask = 0;
        flushPending();
    }

    private long pendingTotal() {
        long n = 0;
        for (long p : pending) n += p;
        return n;
    }

    /**
     * Injects every waiting region into the network; what it does not take keeps waiting. Marks the chunk for saving
     * when anything went in: the network saves its own storage, so a restart that reloaded the older buffer would
     * inject those litres twice.
     */
    private void flushPending() {
        final Fluid[] regions = SLRealityFluids.regions();
        boolean moved = false;
        for (int k = 0; k < 6; k++) {
            if (pending[k] <= 0 || regions[k] == null) continue;
            final long left = inject(regions[k], pending[k]);
            moved |= left != pending[k];
            pending[k] = left;
        }
        if (moved && getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
        if (moved && pendingTotal() == 0) scheduleRecipeCheckImmediate(); // the "full" wait is over
    }

    @Override
    protected void onSecond() {
        if (pendingTotal() > 0) flushPending();
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (mMachine && !receptors.isEmpty() && base != null && base.getWorld() != null) {
            final String key = "reality@" + base.getWorld().provider.dimensionId
                + ":"
                + base.getXCoord()
                + ","
                + base.getYCoord()
                + ","
                + base.getZCoord();
            burstHost.second(receptors, key, "Reality Diffraction", BurstLink.REALITY, 0);
        }
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    // ===================================================================================== GUI

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new RealityDiffractionGui(this);
    }

    /** "Waiting to enter the network: 2,000 L (no running Containment Field)" or "". Server state. */
    public String pendingLine() {
        final long p = pendingTotal();
        if (p <= 0) return "";
        return "Waiting to enter the BEC network: " + fmt(p) + " L (start a Containment Field)";
    }

    /** Litres of the last runs' regions still waiting to enter the BEC network. Server state. */
    public long pendingLitres() {
        return pendingTotal();
    }

    /**
     * GUI cleanup (controller-gui-spec.md §5.7): the anchors as check rows, from {@link #anchorLine()}'s loop. One
     * red row per region that is not anchored for the next run ({@code have / need L}, or "no hatch" for an empty
     * socket), then one green row counting the anchored regions. Reads only.
     */
    public BTSChecklist anchorChecklist() {
        final BTSChecklist c = new BTSChecklist();
        final String g = "beyondthestars.gui.reality.";
        final GTRecipe r = recipe();
        if (r == null) {
            c.add(false, BTSText.lang(g + "chk.norecipe"), "", -1, BTSText.lit(anchorLine()));
            return c;
        }
        int anchored = 0;
        for (int k = 0; k < 6; k++) {
            if (anchored(k)) {
                anchored++;
                continue;
            }
            final FluidStack a = r.mFluidInputs[k + 1];
            final boolean hatch = anchorHatchCount(k) > 0;
            final long have = hatch && a != null ? available(anchorHatches[k], a.getFluid()) : 0;
            final long need = a == null ? 0 : a.amount;
            c.add(
                false,
                BTSText.lang(g + "region." + k),
                hatch ? BTSText.lit(BTSFormat.haveNeed(have, need) + " L") : BTSText.lang(g + "chk.nohatch"),
                hatch ? BTSChecklist.frac(have, need) : -1,
                BTSText.lang(
                    g + "chk.anchor.tip",
                    BTSText.key(g + "region." + k),
                    fmt(need),
                    a == null ? "?" : a.getLocalizedName()));
        }
        // The count of anchored regions (the missing ones each have a red row above); none when nothing is anchored.
        if (anchored > 0) c.add(
            true,
            BTSText.lang(g + (anchored == 6 ? "chk.allregions" : "chk.others")),
            BTSText.lang("beyondthestars.gui.kit.of", anchored, 6),
            -1,
            BTSText.lang(g + "chk.others.tip"));
        return c;
    }

    /** One line naming every missing anchor, or "". Server state. */
    public String anchorLine() {
        final GTRecipe r = recipe();
        if (r == null) return "The Diffraction recipe did not load (its fluids are missing; see the server log)";
        final StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 6; k++) {
            if (anchored(k)) continue;
            if (sb.length() > 0) sb.append("; ");
            final FluidStack a = r.mFluidInputs[k + 1];
            sb.append(REGION_NAMES[k].split(" ")[0])
                .append(": ")
                .append(anchorHatchCount(k) == 0 ? "no hatch" : fmt(a.amount) + " L " + a.getLocalizedName());
        }
        return sb.length() == 0 ? "" : "Missing anchors - " + sb;
    }

    @Override
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add("Reality Diffraction" + (mMachine ? "" : ", NOT FORMED"));
        out.add(networkLine());
        out.add(
            "Sockets: BEC " + becHatchCount()
                + ", medium "
                + mInputHatches.size()
                + ", energy "
                + getExoticAndNormalEnergyHatchList().size()
                + ", anchors "
                + anchoredSockets()
                + "/6");
        final StringBuilder lamps = new StringBuilder("Regions: ");
        for (int k = 0; k < 6; k++) {
            if (k > 0) lamps.append(", ");
            lamps.append(REGION_NAMES[k])
                .append(anchored(k) ? " anchored" : " NOT anchored");
        }
        out.add(lamps.toString());
        final String p = pendingLine();
        if (!p.isEmpty()) out.add(p);
        final String burst = burstLine();
        out.add(burst.isEmpty() ? "No GRB Receptor Port: regions anchored by fluid alone, speed x1" : burst);
        out.add("Runs: " + fmt(runs));
        if (!lastNote.isEmpty()) out.add("Last: " + lastNote);
        return out;
    }

    private int anchoredSockets() {
        int n = 0;
        for (int k = 0; k < 6; k++) if (anchorHatchCount(k) > 0) n++;
        return n;
    }

    /** Bench {@code fill}: fills every anchor socket's hatch with its own region's anchor. Returns hatches filled. */
    public int debugFillAnchors() {
        int n = 0;
        for (int k = 0; k < 6; k++) {
            final Fluid f = anchorFluid(k);
            if (f == null) continue;
            for (MTEHatchInput h : anchorHatches[k]) {
                if (live(h) && h.fill(new FluidStack(f, Integer.MAX_VALUE), true) > 0) n++;
            }
        }
        return n;
    }

    /** Bench: the six pending amounts. */
    public long[] debugPending() {
        return pending.clone();
    }

    /** Bench: puts {@code litres} of every region in the buffer (as a run with no Containment Field would). */
    public void debugSeedPending(long litres) {
        for (int k = 0; k < 6; k++) pending[k] += Math.max(0, litres);
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    /** Bench {@code wrench}: what picking the controller up and placing it back keeps (item NBT only). */
    public void debugWrench() {
        final NBTTagCompound item = new NBTTagCompound();
        setItemNBT(item);
        loadNBTData(item);
    }

    // ===================================================================================== NBT

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsDiffMask", runMask);
        for (int k = 0; k < 6; k++) aNBT.setLong("btsDiffPend" + k, pending[k]);
        aNBT.setLong("btsDiffRuns", runs);
        aNBT.setString("btsDiffNote", lastNote);
    }

    /**
     * Trap 2: the never-void buffer and the run count ride on the controller item, so wrenching or breaking it never
     * deletes litres waiting for a Containment Field; {@link #loadNBTData} reads the same keys on placement and the
     * next {@link #onSecond} flushes them.
     */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        for (int k = 0; k < 6; k++) if (pending[k] > 0) aNBT.setLong("btsDiffPend" + k, pending[k]);
        if (runs > 0) aNBT.setLong("btsDiffRuns", runs);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        runMask = aNBT.getInteger("btsDiffMask");
        for (int k = 0; k < 6; k++) pending[k] = aNBT.getLong("btsDiffPend" + k);
        runs = aNBT.getLong("btsDiffRuns");
        lastNote = aNBT.getString("btsDiffNote");
    }

    // ================================================================================ tooltip

    private static String anchorName(int k) {
        final GTRecipe r = recipe();
        if (r != null) return r.mFluidInputs[k + 1].getLocalizedName();
        final String[] fallback = { "Space", "SpaceTime", "Time", "Eternity", "Superdense Ergomatter",
            "White Hole Matter" };
        return fallback[k];
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] s = size();
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Reality Diffraction, BEC condensate source")
            .addInfo("Splits Entanglement Medium across six regions outside reality. The six parts")
            .addInfo("  leave as Entangled Region condensates, into the BEC network.")
            .addInfo(
                EnumChatFormatting.AQUA + "4,000 L Entanglement Medium -> 1,000 L of each region"
                    + EnumChatFormatting.GRAY
                    + ", 120 s at MAX.")
            .addInfo("One parallel, no overclock.")
            .addSeparator()
            .addInfo(EnumChatFormatting.GOLD + "Region Anchors" + EnumChatFormatting.GRAY + ": each region needs its")
            .addInfo("  anchor fluid in its OWN socket (the red lens under its blade):")
            .addInfo("  I Hyperspace: molten Space, II Subspace: molten SpaceTime,")
            .addInfo("  III Forward Time: molten Time, IV Reverse Time: molten Eternity,")
            .addInfo("  V Positive Mass: Superdense Ergomatter, VI Negative Mass: White Hole Matter.")
            .addInfo("  An unanchored region yields nothing and its share of the medium is lost.")
            .addInfo("  The GUI shows six lamps and names every missing anchor.")
            .addSeparator()
            .addInfo(EnumChatFormatting.GOLD + "BEC network" + EnumChatFormatting.GRAY + ": a BEC Hatch on the base")
            .addInfo("  ring, facing a BEC conduit, and a running Containment Field on that network.")
            .addInfo("  What the network cannot take waits in the controller (never voided).")
            .addInfo("  Give the regions their own network, or fence them with a Condensate Diode")
            .addInfo("  (manual filters): more than three foreign condensates clog Condensate Assembler crafts.")
            .addInfo(
                EnumChatFormatting.GOLD + "Quasar link"
                    + EnumChatFormatting.GRAY
                    + " (optional): GRB Receptor Ports in the six")
            .addInfo("  portal cells (hint 10). With any port fitted the regions ride the stream:")
            .addInfo("  σ = bursts ÷ 24 Bu/t (2.4e14 EU/t; 60 s mean), speed x min(σ, 1.5); below σ 0.5 runs")
            .addInfo("  pause")
            .addInfo("  (kept, never voided). With no port: fluid anchors alone, speed x1.")
            .addInfo("Automation: anchors take ME input hatches; a level emitter on each keeps them stocked.")
            .addInfo("Entanglement Medium is 0 K: pipe it with cryo-proof pipes.")
            .beginStructureBlock(s[0], s[1], s[2], false)
            .addController("Base ring, front")
            .addCasingInfoExactly("Region Deck Band", bom(PIECE, 'A') + bom(PIECE, 'g'), false)
            .addCasingInfoExactly("Phase Ring Casing", bom(PIECE, 'H'), false)
            .addCasingInfoExactly("Region Crystal", bom(PIECE, 'E'), false)
            .addCasingInfoExactly("Region Anchor Casing", bom(PIECE, 'I'), false)
            .addCasingInfoExactly("Harmonic Lining", bom(PIECE, 'J'), false)
            .addCasingInfoExactly("Quantum Glass", bom(PIECE, 'K') + bom(PIECE, 'Z'), false)
            .addCasingInfoExactly("Spatially Transcendent Gravitational Lens Block", bom(PIECE, 'L'), false)
            .addCasingInfoExactly("Coherence-Preserving Plasma Conduit", bom(PIECE, 'N'), false)
            .addCasingInfoExactly("Electromagnetically-Isolated Casing", bom(PIECE, 'Q'), false)
            .addOtherStructurePart("BEC Hatch (front facing a BEC conduit)", "base ring", 1)
            .addOtherStructurePart("Input Hatch (Entanglement Medium)", "base ring", 2)
            .addOtherStructurePart("Region Anchor I..VI (any Input Hatch)", "under each blade, one each", 3)
            .addOtherStructurePart("Energy or Laser Hatch", "base ring", 9)
            .addStructureInfo("Anchor hints 3..8 = Regions I..VI (" + anchorName(0) + " .. " + anchorName(5) + ").")
            .addOtherStructurePart("GRB Receptor Port (optional)", "6 portal cells, top deck band's outer face", 10)
            .addStructureInfo("Builds never overwrite hatches or other tiles.")
            .toolTipFinisher();
        return tt;
    }

    /** The anchor fluid of region k (bench / kit), or null. */
    public static Fluid anchorFluid(int k) {
        final GTRecipe r = recipe();
        if (r == null) return null;
        final FluidStack a = r.mFluidInputs[k + 1];
        return a == null ? null : a.getFluid();
    }

    /** The anchor amount of region k per run (bench / kit), 0 if the recipe is missing. */
    public static int anchorAmount(int k) {
        final GTRecipe r = recipe();
        return r == null || r.mFluidInputs[k + 1] == null ? 0 : r.mFluidInputs[k + 1].amount;
    }
}
