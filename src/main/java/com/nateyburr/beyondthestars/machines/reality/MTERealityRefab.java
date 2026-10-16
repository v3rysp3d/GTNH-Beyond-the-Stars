package com.nateyburr.beyondthestars.machines.reality;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing3;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;

import gregtech.api.casing.Casings;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import tectech.thing.metaTileEntity.multi.bec.MTEBECAssembler;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): REALITY REFAB, MTE base+32 (28132), my "reality refab" sketch
 * (design repo {@code designs/docs/homogeny-reality.md} §3, PROPOSED; D-43). "The other will grab it and reassemble."
 * <p>
 * Built like RC-2's Teleportation (I/O) Node, with three differences (question B-77 items 4-7):
 * <ul>
 * <li><b>Assembler link.</b> It borrows the nanites of a formed, running Condensate Assembler: the nearest one within
 * {@value #LINK_RANGE} blocks in the same dimension, found by itself and kept (re-found if it goes). They are a
 * catalyst: nothing is taken from the Assembler. (GT links a node by a line-of-sight beam; the doc's Data Stick could
 * not be done from our side, since a stick right-clicked on the Assembler never reaches our code.)</li>
 * <li><b>GT's node rules</b> otherwise: the duration splits into one step per item slot; a step needs nanites of at
 * least its tier, each tier above slows it 2x; progress per tick = nanites / tier penalty / (contaminants + 1), so
 * durations are nanite-time. More than three foreign condensates in the network clog it (it waits).</li>
 * <li><b>The reassembly lock.</b> The six regions are drawn from OUR BEC Hatch's network in lockstep with the
 * progress: the craft never advances past the fraction the scarcest region can pay for, so a short region pauses it
 * and nothing is wasted (GT's node fails the craft at the end instead).</li>
 * <li><b>Its own EU</b> through its own energy hatches, only on ticks that advance.</li>
 * </ul>
 */
public class MTERealityRefab extends MTERealityBase<MTERealityRefab> {

    private static final int SAVE_EVERY_TICKS = 20;

    public static final String PIECE = "reality_refab";
    public static final int LINK_RANGE = 64;

    private static final RecipeMap<?> MAP = RealityRecipeMaps.REFAB;

    private static final IIconContainer[] OVERLAYS = {
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_REFAB"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_REFAB_GLOW"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_REFAB_ACTIVE"),
        Textures.BlockIcons.custom("beyondthestars:iconsets/OVERLAY_FRONT_REALITY_REFAB_ACTIVE_GLOW") };

    private static volatile IStructureDefinition<MTERealityRefab> STRUCTURE_DEFINITION;

    // ---- link (tile NBT)
    private boolean linked;
    private int linkDim;
    private int linkX;
    private int linkY;
    private int linkZ;

    // ---- the craft in progress (tile NBT)
    private NaniteTier[] steps;
    private Fluid[] reqFluids;
    private long[] reqAmounts;
    private long[] consumed;
    private int subticks;
    private GTRecipe started;

    // ---- per tick (not persisted)
    private boolean stalled;
    private String wait = "";
    private long lastRelink = Long.MIN_VALUE / 2;
    private int slowdowns;

    public MTERealityRefab(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTERealityRefab(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTERealityRefab(this.mName);
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
    public IStructureDefinition<MTERealityRefab> getStructureDefinition() {
        IStructureDefinition<MTERealityRefab> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTERealityRefab.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTERealityRefab> buildDefinition() {
        final Block c3 = SLBlocks.slCasing3;
        final int index = hatchIndex();
        return StructureDefinition.<MTERealityRefab>builder()
            .addShape(PIECE, BTSShapes.load(PIECE))
            .addElement('E', block(c3, BlockSLCasing3.REGION_CRYSTAL))
            .addElement('R', block(c3, BlockSLCasing3.REASSEMBLY_HULL))
            .addElement('S', block(c3, BlockSLCasing3.REASSEMBLY_TRAY))
            .addElement('T', block(c3, BlockSLCasing3.REASSEMBLY_CLAW))
            .addElement('J', block(SLBlocks.slCasing2, BlockSLCasing2.LINING))
            .addElement('K', casing(Casings.QuantumGlass))
            .addElement('Z', casing(Casings.QuantumGlass))
            .addElement('L', casing(Casings.SpatiallyTranscendentGravitationalLens))
            .addElement('Q', casing(Casings.ElectromagneticallyIsolatedCasing))
            .addElement('V', casing(Casings.CondensateGuidanceCoil))
            .addElement(
                'b',
                buildHatchAdder(MTERealityRefab.class).atLeast(RealityHatchElement.Bec)
                    .casingIndex(index)
                    .hint(1)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)))
            .addElement(
                'i',
                buildHatchAdder(MTERealityRefab.class).atLeast(InputBus)
                    .casingIndex(index)
                    .hint(2)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)))
            .addElement(
                'o',
                buildHatchAdder(MTERealityRefab.class).atLeast(OutputBus)
                    .casingIndex(index)
                    .hint(3)
                    .buildAndChain(block(c3, BlockSLCasing3.REASSEMBLY_HULL)))
            .addElement(
                'e',
                buildHatchAdder(MTERealityRefab.class).atLeast(Energy.or(ExoticEnergy))
                    .casingIndex(index)
                    .hint(4)
                    .buildAndChain(casing(Casings.ElectromagneticallyIsolatedCasing)))
            .build();
    }

    private static IStructureElement<MTERealityRefab> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTERealityRefab> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        final boolean ok = walk(errors);
        afterWalk();
        if (!ok) return;
        if (becHatches.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.reality.bec"));
        if (mInputBusses.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.reality.inputbus"));
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.of("beyondthestars.structure.reality.outputbus"));
        if (getExoticAndNormalEnergyHatchList().isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.reality.energy"));
        }
    }

    // ============================================================================ the link

    /** The linked Condensate Assembler, if it still stands, is formed and is ours to borrow from. Server. */
    public MTEBECAssembler assembler() {
        if (!linked) return null;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return null;
        final World world = base.getWorld();
        if (world.provider.dimensionId != linkDim || !world.blockExists(linkX, linkY, linkZ)) return null;
        final TileEntity te = world.getTileEntity(linkX, linkY, linkZ);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEBECAssembler ? (MTEBECAssembler) mte : null;
    }

    /** Links the nearest Condensate Assembler within {@value #LINK_RANGE} blocks; true if one was found. Server. */
    public boolean relink() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return false;
        final World world = base.getWorld();
        final int x = base.getXCoord();
        final int y = base.getYCoord();
        final int z = base.getZCoord();
        TileEntity best = null;
        long bestD = (long) LINK_RANGE * LINK_RANGE + 1;
        // QA-B41: only the loaded chunks within range, not a copy of the world's whole tile-entity list
        final List<TileEntity> near = new ArrayList<>();
        for (int cx = (x - LINK_RANGE) >> 4; cx <= (x + LINK_RANGE) >> 4; cx++) {
            for (int cz = (z - LINK_RANGE) >> 4; cz <= (z + LINK_RANGE) >> 4; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                for (Object o : world.getChunkFromChunkCoords(cx, cz).chunkTileEntityMap.values()) {
                    if (o instanceof TileEntity) near.add((TileEntity) o);
                }
            }
        }
        for (TileEntity te : near) {
            if (!(te instanceof IGregTechTileEntity) || te.isInvalid()) continue;
            if (!(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEBECAssembler)) continue;
            final long dx = te.xCoord - x;
            final long dy = te.yCoord - y;
            final long dz = te.zCoord - z;
            final long d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = te;
            }
        }
        if (best == null) return false;
        linked = true;
        linkDim = world.provider.dimensionId;
        linkX = best.xCoord;
        linkY = best.yCoord;
        linkZ = best.zCoord;
        base.markDirty();
        return true;
    }

    /** Linked, formed and running (GT's node rule: an Assembler that is off lends nothing). */
    private MTEBECAssembler runningAssembler() {
        final MTEBECAssembler a = assembler();
        return a != null && a.mMachine && a.mMaxProgresstime > 0 ? a : null;
    }

    /** QA-B44: a stopped Refab (power, the soft mallet) no longer shows the old "Waiting:" reason. */
    @Override
    protected void onStopped() {
        wait = "";
    }

    @Override
    protected void onSecond() {
        if (assembler() == null) {
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            if (base != null && base.getTimer() % 100 < 20) relink();
        }
    }

    public String linkLine() {
        final MTEBECAssembler a = assembler();
        if (a == null) {
            return "Condensate Assembler: none within " + LINK_RANGE + " blocks (it links the nearest by itself)";
        }
        final NaniteTier t = a.getCurrentNaniteTier();
        return "Condensate Assembler at " + linkX
            + ", "
            + linkY
            + ", "
            + linkZ
            + (a.mMachine && a.mMaxProgresstime > 0 ? "" : " (OFF)")
            + ": "
            + (t == null ? "no nanites" : fmt(a.getAvailableNanites()) + " " + t.describe());
    }

    // ============================================================================ processing

    @Override
    public RecipeMap<?> getRecipeMap() {
        return MAP;
    }

    private static NaniteTier highest(NaniteTier[] tiers) {
        NaniteTier max = null;
        if (tiers == null) return null;
        for (NaniteTier t : tiers) if (t != null && (max == null || t.tier > max.tier)) max = t;
        return max;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                final MTEBECAssembler a = runningAssembler();
                if (a == null) return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.noassembler");
                final NaniteTier need = highest(recipe.getMetadata(GTRecipeConstants.NANITE_TIERS));
                final NaniteTier have = a.getCurrentNaniteTier();
                if (need != null && (have == null || have.tier < need.tier)) {
                    return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.nanites");
                }
                return super.validateRecipe(recipe);
            }

            @Nonnull
            @Override
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return super.createOverclockCalculator(recipe).setNoOverclock(true);
            }

            @Nonnull
            @Override
            protected CheckRecipeResult onRecipeStart(@Nonnull GTRecipe recipe) {
                started = recipe;
                return super.onRecipeStart(recipe);
            }
        }.setMaxParallel(1);
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        started = null;
        if (network == null || becHatches.isEmpty()) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.nonetwork");
        }
        if (runningAssembler() == null) {
            // QA-B41: at most one search every 5 s (an idle machine checks its recipe every second)
            final IGregTechTileEntity b = getBaseMetaTileEntity();
            if (assembler() == null && b != null && b.getTimer() - lastRelink >= 100) {
                lastRelink = b.getTimer();
                relink();
            }
            if (runningAssembler() == null) {
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.reality.noassembler");
            }
        }
        final CheckRecipeResult result = super.checkProcessing();
        if (result.wasSuccessful() && started != null) setUpCraft(started);
        return result;
    }

    private void setUpCraft(GTRecipe recipe) {
        steps = recipe.getMetadata(GTRecipeConstants.NANITE_TIERS);
        if (steps == null || steps.length == 0) steps = new NaniteTier[] { NaniteTier.Carbon };
        final FluidStack[] cond = recipe.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
        final int n = cond == null ? 0 : cond.length;
        reqFluids = new Fluid[n];
        reqAmounts = new long[n];
        consumed = new long[n];
        for (int i = 0; i < n; i++) {
            reqFluids[i] = cond[i].getFluid();
            reqAmounts[i] = cond[i].amount;
        }
        subticks = 0;
        lastNote = "Reassembling " + (mOutputItems != null && mOutputItems.length > 0 && mOutputItems[0] != null
            ? mOutputItems[0].getDisplayName()
            : "?");
    }

    /** Step index at progress {@code p}: the duration is split into equal steps, one per item slot. */
    private int stepAt(int p) {
        if (steps == null || steps.length == 0 || mMaxProgresstime <= 0) return 0;
        return Math.max(0, Math.min(steps.length - 1, (int) ((long) p * steps.length / mMaxProgresstime)));
    }

    private int stepStart(int i) {
        return (int) ((long) i * mMaxProgresstime / steps.length);
    }

    /** No EU on a tick that does not advance (the node's Assembler-pays rule, mirrored: we pay only for progress). */
    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (stalled) return true;
        return super.onRunningTick(aStack);
    }

    @Override
    protected void incrementProgressTime() {
        stalled = true;
        if (steps == null || reqFluids == null) {
            wait = "the craft lost its recipe data; break and replace the input to restart";
            return;
        }
        final MTEBECAssembler a = runningAssembler();
        if (a == null) {
            wait = "the Condensate Assembler is gone or off";
            return;
        }
        if (network == null) {
            wait = "no BEC network";
            return;
        }
        final int step = stepAt(mProgresstime);
        final NaniteTier need = steps[step];
        final NaniteTier have = a.getCurrentNaniteTier();
        if (have == null || have.tier < need.tier) {
            wait = "step " + (step + 1) + " needs " + need.describe();
            return;
        }
        final List<Fluid> wanted = new ArrayList<>();
        for (Fluid f : reqFluids) wanted.add(f);
        slowdowns = network.getSlowdowns(this, wanted);
        if (slowdowns > 3) {
            wait = "clogged: " + slowdowns + " foreign condensates in the network (fence it with a Diode)";
            return;
        }
        final long divisor = (1L << Math.max(0, have.tier - need.tier)) * (slowdowns + 1L);
        subticks += a.getAvailableNanites();
        final int full = (int) Math.min(Integer.MAX_VALUE, subticks / divisor);
        subticks -= (int) (full * divisor);
        int next = (int) Math.min(mMaxProgresstime, (long) mProgresstime + full);
        // A later step that needs better nanites is a gate the progress cannot pass.
        for (int i = step + 1; i < steps.length; i++) {
            if (steps[i].tier > have.tier) {
                next = Math.min(next, stepStart(i));
                break;
            }
        }
        if (next <= mProgresstime) {
            // full == 0: sub-ticks are accruing (few nanites), which is progress and pays; otherwise a gate
            stalled = full != 0;
            wait = full == 0 ? "" : "the next step needs better nanites";
            return;
        }
        next = lockstep(next);
        if (next <= mProgresstime) {
            wait = "waiting for regions (the reassembly lock)";
            return;
        }
        // The regions drawn so far live in this tile's NBT while the BEC network saves its own storage: mark the
        // chunk for saving as the craft advances (GT marks it only at start and end), so a restart cannot reload an
        // older "consumed" and draw those regions twice.
        if (next / SAVE_EVERY_TICKS != mProgresstime / SAVE_EVERY_TICKS) getBaseMetaTileEntity().markDirty();
        mProgresstime = next;
        stalled = false;
        wait = "";
    }

    /**
     * The reassembly lock: draws every condensate up to the fraction {@code next / max} of its requirement, but never
     * past what the scarcest one can pay for. Returns the progress actually paid for.
     */
    private int lockstep(int next) {
        final int max = mMaxProgresstime;
        double fraction = next / (double) max;
        boolean isShort = false;
        for (int i = 0; i < reqFluids.length; i++) {
            final long target = target(i, fraction);
            final long want = target - consumed[i];
            if (want <= 0) continue;
            final long have = stored(reqFluids[i]);
            if (have < want) {
                fraction = Math.min(fraction, (consumed[i] + have) / (double) reqAmounts[i]);
                isShort = true;
            }
        }
        // B-137: next / max * max can come back as 54.999... and floor to the progress already paid, which froze a
        // craft for good at 54 / 2400 with every region in stock; with no region short the asked progress is paid
        final int paid = isShort ? Math.min(next, (int) Math.floor(fraction * max + EPS)) : next;
        fraction = paid / (double) max;
        for (int i = 0; i < reqFluids.length; i++) {
            final long want = target(i, fraction) - consumed[i];
            if (want > 0) consumed[i] += drain(reqFluids[i], want);
        }
        // a race (someone else drained) leaves the slowest region short: never advance past it
        double got = 1.0;
        for (int i = 0; i < reqFluids.length; i++) {
            if (reqAmounts[i] > 0) got = Math.min(got, consumed[i] / (double) reqAmounts[i]);
        }
        final int byGot = got >= 1.0 ? max : (int) Math.floor(got * max + EPS);
        return Math.min(paid, byGot);
    }

    /** Rounding slack for the lockstep's fraction-to-ticks conversions (B-137). */
    private static final double EPS = 1e-9;

    private long target(int i, double fraction) {
        return fraction >= 1.0 ? reqAmounts[i] : Math.min(reqAmounts[i], (long) Math.ceil(reqAmounts[i] * fraction));
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        if (steps != null) lastNote = lastNote.replace("Reassembling", "Reassembled");
        steps = null;
        reqFluids = null;
        reqAmounts = null;
        consumed = null;
        subticks = 0;
        wait = "";
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    // ===================================================================================== GUI

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new RealityRefabGui(this);
    }

    /** "Step 3/8: needs Black Dwarf (tier 7), 1,024 nanites, 0 contaminants" or "". Server state. */
    public String stepLine() {
        if (steps == null || mMaxProgresstime <= 0) return "";
        final int s = stepAt(mProgresstime);
        return "Step " + (s + 1) + "/" + steps.length + ": " + steps[s].describe() + ", contaminants " + slowdowns;
    }

    /** The regions drawn so far, as the slowest fraction: "Regions 42.0% drawn". Server state. */
    public String regionLine() {
        final double got = regionFraction();
        if (got < 0) return "";
        return String.format(Locale.ROOT, "Regions drawn: %.1f%% (lockstep)", got * 100);
    }

    /** The regions drawn so far, as the slowest fraction (0..1), or −1 when no craft is set up. Server state. */
    public double regionFraction() {
        if (reqFluids == null || reqFluids.length == 0) return -1;
        double got = 1.0;
        for (int i = 0; i < reqFluids.length; i++) {
            if (reqAmounts[i] > 0) got = Math.min(got, consumed[i] / (double) reqAmounts[i]);
        }
        return got;
    }

    /** The step being worked (0-based), or −1 while no craft runs. Server state. */
    public int stepIndex() {
        return steps == null || mMaxProgresstime <= 0 ? -1 : stepAt(mProgresstime);
    }

    /** Steps of the running craft (0 while none runs). Server state. */
    public int stepCount() {
        return steps == null || mMaxProgresstime <= 0 ? 0 : steps.length;
    }

    /** The running step's nanite tier, "Neutronium (Tier 3)", or "". Server state. */
    public String stepTier() {
        final int s = stepIndex();
        return s < 0 ? "" : steps[s].describe();
    }

    /** Foreign condensates in the network slowing the craft. Server state. */
    public int contaminants() {
        return slowdowns;
    }

    /**
     * GUI cleanup (controller-gui-spec.md §5.7): the BEC network and the Condensate Assembler link as check rows, from
     * the state {@link #networkLine()} and {@link #linkLine()} read. Reads only.
     */
    public BTSChecklist linkChecklist() {
        final BTSChecklist c = new BTSChecklist();
        addNetworkCheck(c, false);
        final String g = "beyondthestars.gui.reality.";
        final String label = BTSText.lang(g + "chk.assembler");
        final MTEBECAssembler a = assembler();
        if (a == null) {
            c.add(
                false,
                label,
                BTSText.lang(g + "chk.assembler.none", LINK_RANGE),
                -1,
                BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.reality.noassembler"));
            return c;
        }
        final String at = linkX + ", " + linkY + ", " + linkZ;
        if (!(a.mMachine && a.mMaxProgresstime > 0)) {
            c.add(
                false,
                label,
                BTSText.lang(g + "chk.assembler.off"),
                -1,
                BTSText.lang(g + "chk.assembler.at", at),
                BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.reality.noassembler"));
            return c;
        }
        final NaniteTier t = a.getCurrentNaniteTier();
        if (t == null) {
            c.add(
                false,
                label,
                BTSText.lang(g + "chk.assembler.nonanites"),
                -1,
                BTSText.lang(g + "chk.assembler.at", at),
                BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.reality.nanites"));
            return c;
        }
        c.add(
            true,
            label,
            BTSText.lang(g + "chk.assembler.nanites", fmt(a.getAvailableNanites())),
            -1,
            BTSText.lang(g + "chk.assembler.at", at),
            BTSText.lit(fmt(a.getAvailableNanites()) + " " + t.describe()));
        return c;
    }

    public String waitLine() {
        return wait;
    }

    /** Bits 0-5: the network holds region k (any amount). Server state (GUI lamps). */
    public int regionMask() {
        final Fluid[] regions = com.nateyburr.beyondthestars.fluids.SLRealityFluids.regions();
        int m = 0;
        for (int k = 0; k < 6; k++) if (regions[k] != null && stored(regions[k]) > 0) m |= 1 << k;
        return m;
    }

    @Override
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add("Reality Refab" + (mMachine ? "" : ", NOT FORMED"));
        if (!structurePauseNote().isEmpty()) out.add(structurePauseNote());
        out.add(networkLine());
        out.add(linkLine());
        out.add(
            "Sockets: BEC " + becHatchCount()
                + ", input bus "
                + mInputBusses.size()
                + ", output bus "
                + mOutputBusses.size()
                + ", energy "
                + getExoticAndNormalEnergyHatchList().size());
        final String s = stepLine();
        if (!s.isEmpty()) out.add(s);
        final String r = regionLine();
        if (!r.isEmpty()) out.add(r);
        if (!wait.isEmpty()) out.add("Waiting: " + wait);
        if (!lastNote.isEmpty()) out.add("Last: " + lastNote);
        return out;
    }

    // ===================================================================================== NBT

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("btsRefabLinked", linked);
        aNBT.setInteger("btsRefabDim", linkDim);
        aNBT.setInteger("btsRefabX", linkX);
        aNBT.setInteger("btsRefabY", linkY);
        aNBT.setInteger("btsRefabZ", linkZ);
        aNBT.setString("btsRefabNote", lastNote);
        if (steps != null && reqFluids != null) {
            final StringBuilder sb = new StringBuilder();
            for (NaniteTier t : steps) sb.append(t.name())
                .append(',');
            aNBT.setString("btsRefabSteps", sb.toString());
            final NBTTagCompound c = new NBTTagCompound();
            for (int i = 0; i < reqFluids.length; i++) {
                c.setString("f" + i, reqFluids[i].getName());
                c.setLong("r" + i, reqAmounts[i]);
                c.setLong("c" + i, consumed[i]);
            }
            c.setInteger("n", reqFluids.length);
            aNBT.setTag("btsRefabCraft", c);
            aNBT.setInteger("btsRefabSub", subticks);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        linked = aNBT.getBoolean("btsRefabLinked");
        linkDim = aNBT.getInteger("btsRefabDim");
        linkX = aNBT.getInteger("btsRefabX");
        linkY = aNBT.getInteger("btsRefabY");
        linkZ = aNBT.getInteger("btsRefabZ");
        lastNote = aNBT.getString("btsRefabNote");
        steps = null;
        reqFluids = null;
        if (aNBT.hasKey("btsRefabSteps") && aNBT.hasKey("btsRefabCraft")) {
            final List<NaniteTier> list = new ArrayList<>();
            for (String name : aNBT.getString("btsRefabSteps")
                .split(",")) {
                if (name.isEmpty()) continue;
                try {
                    list.add(NaniteTier.valueOf(name));
                } catch (IllegalArgumentException gone) {
                    list.add(NaniteTier.Carbon);
                }
            }
            final NBTTagCompound c = aNBT.getCompoundTag("btsRefabCraft");
            final int n = c.getInteger("n");
            final Fluid[] f = new Fluid[n];
            final long[] r = new long[n];
            final long[] k = new long[n];
            boolean ok = !list.isEmpty();
            for (int i = 0; i < n; i++) {
                f[i] = FluidRegistry.getFluid(c.getString("f" + i));
                r[i] = c.getLong("r" + i);
                k[i] = c.getLong("c" + i);
                if (f[i] == null) ok = false;
            }
            if (ok) {
                steps = list.toArray(new NaniteTier[0]);
                reqFluids = f;
                reqAmounts = r;
                consumed = k;
                subticks = aNBT.getInteger("btsRefabSub");
            }
        }
    }

    // ================================================================================ tooltip

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] s = size();
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Reality Refab, BEC reassembler")
            .addInfo("Reassembles the six Entangled Regions from the BEC network.")
            .addInfo("Makes the " + BTSTip.term("Universal Upgrade Core") + ": the Eye's jump to T10.")
            .addSeparator()
            .addInfo("Nanites: borrowed from the nearest running")
            .addInfo(
                BTSTip.term("Condensate Assembler") + " within "
                    + BTSTip.num(LINK_RANGE + " blocks")
                    + ". Never used up.")
            .addInfo("Each step needs nanites of at least its tier.")
            .addInfo("Every tier above slows the step " + BTSTip.num("2x") + ".")
            .addInfo("Regions are drawn from its own BEC Hatch's network as it works;")
            .addInfo("a short region pauses the craft. Nothing is wasted.")
            .addInfo(BTSTip.warn("More than three foreign condensates clog it."))
            .addSeparator()
            .addInfo("Power: its own energy hatches, only while it advances.")
            .addInfo("No overclock. Automation: ME input bus in, ME output bus out.")
            .beginStructureBlock(s[0], s[1], s[2], false)
            .addController("Foot, front")
            .addCasingInfoExactly("Reassembly Hull Plating", bom(PIECE, 'R'), false)
            .addCasingInfoExactly("Reassembly Tray Plating", bom(PIECE, 'S'), false)
            .addCasingInfoExactly("Reassembly Claw", bom(PIECE, 'T'), false)
            .addCasingInfoExactly("Region Crystal", bom(PIECE, 'E'), false)
            .addCasingInfoExactly("Harmonic Lining", bom(PIECE, 'J'), false)
            .addCasingInfoExactly("Quantum Glass", bom(PIECE, 'K') + bom(PIECE, 'Z'), false)
            .addCasingInfoExactly("Spatially Transcendent Gravitational Lens Block", bom(PIECE, 'L'), false)
            .addCasingInfoExactly("Condensate Guidance Coil", bom(PIECE, 'V'), false)
            // QA-B44: the second input-bus socket is EM-Isolated Casing while it holds no bus, so this is a range.
            .addCasingInfoRange(
                "Electromagnetically-Isolated Casing",
                bom(PIECE, 'Q'),
                bom(PIECE, 'Q') + bom(PIECE, 'i') - 1,
                false)
            .addMiscHatch("1", "BEC Hatch", "foot, facing a BEC conduit", 1)
            .addInputBus("1", "Foot", 2)
            .addOutputBus("1", "Cap, by the Reality Focus", 3)
            .addEnergyHatch("1+", "Foot (energy or laser)", 4)
            .addStructureInfo("Builds never overwrite hatches or other tiles.")
            .toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }
}
