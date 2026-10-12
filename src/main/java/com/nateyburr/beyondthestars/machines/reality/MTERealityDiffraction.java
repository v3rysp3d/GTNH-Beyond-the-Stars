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
            anchorHatches[region].add((MTEHatchInput) mte);
            return true;
        }
        return false;
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
        mMaxProgresstime = Math.max(1, r.mDuration);
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

    /** Injects every waiting region into the network; what it does not take keeps waiting. */
    private void flushPending() {
        final Fluid[] regions = SLRealityFluids.regions();
        for (int k = 0; k < 6; k++) {
            if (pending[k] > 0 && regions[k] != null) pending[k] = inject(regions[k], pending[k]);
        }
    }

    @Override
    protected void onSecond() {
        if (pendingTotal() > 0) flushPending();
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

    /** One line naming every missing anchor, or "". Server state. */
    public String anchorLine() {
        final GTRecipe r = recipe();
        if (r == null) return "SL-R03 is not registered (its fluids are missing)";
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

    // ===================================================================================== NBT

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsDiffMask", runMask);
        for (int k = 0; k < 6; k++) aNBT.setLong("btsDiffPend" + k, pending[k]);
        aNBT.setLong("btsDiffRuns", runs);
        aNBT.setString("btsDiffNote", lastNote);
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
            .addInfo("Automation: anchors take ME input hatches; a level emitter on each keeps them stocked.")
            .addInfo("Entanglement Medium is 0 K: pipe it with cryo-proof pipes.")
            .beginStructureBlock(s[0], s[1], s[2], false)
            .addController("Base ring, front")
            .addCasingInfoExactly("Region Deck Band", bom(PIECE, 'A'), false)
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
