package com.nateyburr.beyondthestars.machines.reality;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;

import appeng.api.storage.data.IAEFluidStack;
import appeng.util.item.AEFluidStack;
import gregtech.api.casing.Casings;
import gregtech.api.factory.RoutedNode;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import tectech.mechanics.boseEinsteinCondensate.BECFactoryElement;
import tectech.mechanics.boseEinsteinCondensate.BECFactoryGrid;
import tectech.mechanics.boseEinsteinCondensate.BECFactoryNetwork;
import tectech.mechanics.boseEinsteinCondensate.BECInventory;
import tectech.mechanics.boseEinsteinCondensate.BECRouteInfo;
import tectech.mechanics.boseEinsteinCondensate.NotableBECFactoryElement;
import tectech.thing.metaTileEntity.hatch.bec.MTEHatchBEC;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): what Reality Diffraction and Reality Refab share. Both are single-piece
 * MAX machines and both are elements of RC-2's Bose-Einstein Condensate network (design repo
 * {@code designs/docs/homogeny-reality.md} §2.3 / §3.3, H-14).
 *
 * <h2>The BEC glue</h2>
 * RC-2's own BEC machines extend {@code MTEBECMultiblockBase} (a TecTech multiblock with a text-structure wrapper);
 * ours keep GT's {@link MTEExtendedPowerMultiBlockBase} and the exported shapes, and implement the two BEC interfaces
 * themselves, exactly as {@code MTEBECMultiblockBase} does:
 * <ul>
 * <li>{@link BECFactoryElement}: our neighbours are our formed BEC Hatches ({@code b} socket, GT's
 * {@link MTEHatchBEC}); we never connect on a side.</li>
 * <li>{@link NotableBECFactoryElement}: our routed neighbours are found by a walk from our hatches through the grid's
 * edges, so storages (Containment Fields), the Assembler and the Diode see us and we see them.</li>
 * <li>We register with {@link BECFactoryGrid} on the first tick and after every walk, and leave it on removal and
 * unload. GT's hatch only lists TecTech BEC controllers as its own neighbours, so a hatch that re-registers itself
 * (its first tick, a facing or colour change) drops the edge to us; we re-assert it every second
 * ({@link #REASSERT_TICKS}), which costs one neighbour-set compare when nothing changed (question B-63 item 1).</li>
 * </ul>
 * Condensate moves through GT's own network calls: {@link BECFactoryNetwork#injectCondensate},
 * {@link BECFactoryNetwork#drainCondensate}, {@link BECFactoryNetwork#getStoredCondensate}, so diodes and filters
 * apply as they do to every BEC machine.
 */
public abstract class MTERealityBase<T extends MTERealityBase<T>> extends MTEExtendedPowerMultiBlockBase<T>
    implements ISurvivalConstructable, BECFactoryElement, NotableBECFactoryElement {

    /** How often the BEC edge to our hatches is re-asserted. */
    public static final int REASSERT_TICKS = 20;

    protected final List<MTEHatchBEC> becHatches = new ArrayList<>();
    private Set<MTEHatchBEC> previousBecHatches = new HashSet<>();
    protected BECFactoryNetwork network;
    protected int mCasing;
    protected String lastNote = "";

    protected MTERealityBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTERealityBase(String aName) {
        super(aName);
    }

    /** The machine's one piece (its manifest name). */
    public abstract String piece();

    protected abstract IIconContainer[] overlays();

    // =================================================================================== shape

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int[] o = BTSShapes.offset(piece());
        buildPiece(piece(), stackSize, hintsOnly, o[0], o[1], o[2]);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        final int[] o = BTSShapes.offset(piece());
        return survivalBuildPiece(
            piece(),
            stackSize,
            o[0],
            o[1],
            o[2],
            BTSStructures.survivalBudget(elementBudget),
            env,
            false,
            true);
    }

    /** Walk the piece; subclasses add their own checks after. Hatch lists were cleared by GT's clearHatches. */
    protected boolean walk(List<gregtech.api.structure.error.StructureError> errors) {
        mCasing = 0;
        becHatches.clear();
        final int[] o = BTSShapes.offset(piece());
        return checkPiece(piece(), o[0], o[1], o[2], errors);
    }

    /** Called after every walk: re-registers with the BEC grid if our hatch set changed. */
    protected void afterWalk() {
        final Set<MTEHatchBEC> now = new HashSet<>(becHatches);
        if (!now.equals(previousBecHatches) && isServer()) BECFactoryGrid.INSTANCE.updateElement(this);
        previousBecHatches = now;
    }

    // ============================================================================ BEC hatches

    /** Adder for the {@code b} socket: GT's BEC Hatch only. */
    public boolean addBecHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchBEC) {
            ((MTEHatchBEC) mte).updateTexture(casingIndex);
            becHatches.add((MTEHatchBEC) mte);
            return true;
        }
        return false;
    }

    public int becHatchCount() {
        int n = 0;
        for (MTEHatchBEC h : becHatches) if (live(h)) n++;
        return n;
    }

    protected static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    private boolean isServer() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.isServerSide();
    }

    // ============================================================================ BEC element

    @Override
    public void getNeighbours(Collection<BECFactoryElement> neighbours) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isDead()) return;
        for (MTEHatchBEC h : becHatches) if (live(h)) neighbours.add(h);
    }

    @Override
    public BECFactoryNetwork getNetwork() {
        return network;
    }

    @Override
    public void setNetwork(BECFactoryNetwork network) {
        this.network = network;
    }

    @Override
    public ConnectionType getConnectionOnSide(ForgeDirection side) {
        return ConnectionType.NONE;
    }

    /** The routed walk of {@code MTEBECMultiblockBase.getRoutedNeighbours}, seeded from our BEC hatches. */
    @Override
    public List<RoutedNode<NotableBECFactoryElement, BECRouteInfo>> getRoutedNeighbours() {
        final List<RoutedNode<NotableBECFactoryElement, BECRouteInfo>> nodes = new ArrayList<>();
        final HashSet<BECFactoryElement> visited = new HashSet<>();
        for (BECFactoryElement seed : new ArrayList<>(becHatches)) {
            final Deque<ObjectIntPair<BECFactoryElement>> queue = new ArrayDeque<>();
            queue.add(ObjectIntPair.of(seed, 0));
            while (!queue.isEmpty()) {
                final ObjectIntPair<BECFactoryElement> curr = queue.pop();
                if (!visited.add(curr.left())) continue;
                if (curr.left() instanceof NotableBECFactoryElement) {
                    final NotableBECFactoryElement notable = (NotableBECFactoryElement) curr.left();
                    if (notable != this) nodes.add(new RoutedNode<>(notable, new BECRouteInfo(curr.rightInt())));
                    continue;
                }
                final Set<BECFactoryElement> edges = BECFactoryGrid.INSTANCE.edges.get(curr.left());
                final int dist = curr.rightInt() + 1;
                for (BECFactoryElement conn : edges) queue.add(ObjectIntPair.of(conn, dist));
            }
        }
        return nodes;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aBaseMetaTileEntity.isServerSide()) BECFactoryGrid.INSTANCE.updateElement(this);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aTick % REASSERT_TICKS == 0) {
            BECFactoryGrid.INSTANCE.updateElement(this);
            onSecond();
            // RC-2 recipe checks are event-driven, but our idle reasons (no running Containment Field, buffer full,
            // Assembler between crafts, no network) clear through changes no hatch of ours reports. An idle, formed,
            // enabled machine therefore asks for one check a second (throttled: GT's post-failure cooldown applies).
            if (mMachine && mMaxProgresstime == 0 && aBaseMetaTileEntity.isAllowedToWork()) {
                scheduleRecipeCheck(RecipeCheckReason.THROTTLED);
            }
        }
    }

    /** Once a second on the server (after the BEC re-assert, before the idle recipe-check push). */
    protected void onSecond() {}

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (GTUtility.isServer()) BECFactoryGrid.INSTANCE.removeElement(this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (GTUtility.isServer()) BECFactoryGrid.INSTANCE.removeElement(this);
    }

    // ============================================================================ BEC network I/O

    /** True when our network has at least one condensate storage (a running Containment Field accepts). */
    public boolean hasStorage() {
        return network != null && !network.getComponents(BECInventory.class)
            .isEmpty();
    }

    /** Condensate of {@code fluid} reachable from us, in litres. */
    public long stored(Fluid fluid) {
        if (network == null || fluid == null) return 0;
        return network.getStoredCondensate(this, fluid);
    }

    /** Injects {@code amount} L of {@code fluid}; returns what the network did NOT take. */
    public long inject(Fluid fluid, long amount) {
        if (network == null || fluid == null || amount <= 0) return amount;
        final IAEFluidStack stack = AEFluidStack.create(new FluidStack(fluid, 1))
            .setStackSize(amount);
        network.injectCondensate(this, stack);
        return Math.max(0, stack.getStackSize());
    }

    /** Drains up to {@code amount} L of {@code fluid}; returns what was drained. */
    public long drain(Fluid fluid, long amount) {
        if (network == null || fluid == null || amount <= 0) return 0;
        final IAEFluidStack stack = AEFluidStack.create(new FluidStack(fluid, 1))
            .setStackSize(amount);
        network.drainCondensate(this, stack);
        return amount - Math.max(0, stack.getStackSize());
    }

    /** "network 12 (3 storages)" or "none". */
    public String networkLine() {
        if (network == null) return "BEC network: none (needs a BEC Hatch facing a BEC conduit)";
        final int storages = network.getComponents(BECInventory.class)
            .size();
        return "BEC network " + network.id
            + ": "
            + storages
            + (storages == 1 ? " storage" : " storages")
            + (storages == 0 ? " (add a Containment Field)" : "");
    }

    // ===================================================================== private fluid hatches

    /** Fluid of {@code fluid} held by the hatches in {@code list} (not GT's lists: never started by GT). */
    protected static long available(List<MTEHatchInput> list, Fluid fluid) {
        long n = 0;
        for (MTEHatchInput hatch : list) {
            if (!live(hatch)) continue;
            for (FluidStack s : storedFluids(hatch)) {
                if (s != null && s.getFluid() == fluid) n += s.amount;
            }
        }
        return n;
    }

    /** Drains {@code amount} of {@code fluid} from the hatches in {@code list}; returns what was drained. */
    protected long take(List<MTEHatchInput> list, Fluid fluid, long amount) {
        long got = 0;
        for (MTEHatchInput hatch : list) {
            if (got >= amount) break;
            if (!live(hatch)) continue;
            final boolean aware = hatch instanceof IRecipeProcessingAwareHatch;
            if (aware) ((IRecipeProcessingAwareHatch) hatch).startRecipeProcessing();
            try {
                for (FluidStack s : storedFluids(hatch)) {
                    if (got >= amount) break;
                    if (s == null || s.getFluid() != fluid || s.amount <= 0) continue;
                    final int want = (int) Math.min(s.amount, amount - got);
                    final FluidStack d = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(fluid, want), true);
                    if (d != null) got += d.amount;
                }
            } finally {
                if (aware) ((IRecipeProcessingAwareHatch) hatch).endRecipeProcessing(this);
            }
        }
        return got;
    }

    protected static FluidStack[] storedFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchInputME) return ((MTEHatchInputME) hatch).getStoredFluids();
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    // ============================================================================ common bits

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    public boolean isFormedForGui() {
        return mMachine;
    }

    public String lastNote() {
        return lastNote;
    }

    /** Status lines for the scanner and the kit. Server state. */
    public abstract List<String> statusLines();

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    protected static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    protected static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    protected int[] size() {
        try {
            return BTSShapes.size(piece());
        } catch (RuntimeException missing) {
            return new int[] { 1, 1, 1 };
        }
    }

    /** The BEC casing every Reality hatch is textured as (GT: "BEC Hatch on any Electromagnetically-Isolated"). */
    public static int hatchIndex() {
        return Casings.ElectromagneticallyIsolatedCasing.getTextureId();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ElectromagneticallyIsolatedCasing.getCasingTexture();
        if (side != aFacing) return new ITexture[] { base };
        final IIconContainer[] o = overlays();
        return new ITexture[] { base, TextureFactory.builder()
            .addIcon(aActive ? o[2] : o[0])
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(aActive ? o[3] : o[1])
                .extFacing()
                .glow()
                .build() };
    }
}
