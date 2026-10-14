package com.nateyburr.beyondthestars.machines.conduit;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.QuietTileUpdate;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
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
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * P4: THE EINSTEIN-ROSEN CONDUIT ELEMENT, the shared base of the Wormhole Mouth (28121), the Conduit Mirror (28122)
 * and the Conduit Junction (28123) (quasar-p4.md §3, §7; D-36). Lossless; the only losses are a throat's cap (the rest
 * is <b>clipped</b> and shown, no damage) and a closed element.
 * <ul>
 * <li><b>Throat.</b> A Conduit Throat item in the controller slot, like a turbine's rotor: Mk I / II / III carry 24 /
 * 384 / 6,144 Bu/t through the element ({@link BurstRouting#throatCap}). No throat: the element is closed.</li>
 * <li><b>Upkeep</b> (F-5: one unit per element, whatever it carries): {@link BurstRouting#throatUpkeep} litres a second
 * from the {@code u} hatches (any input hatch, ME stocking included). Molten SpaceTime 4 / 16 / 64 L/s works from the
 * start; Ergomatter 0.25 / 1 / 4 L/s once Eye T7 makes it (Q-3). The fuel is picked in the GUI: Auto takes Ergomatter
 * when the hatches hold enough, else SpaceTime. Short upkeep closes the element (its streams stop, and its quasar's
 * Directive Disc bit 4 alarm rises); it opens after {@link BurstRouting#REOPEN_SECONDS} s of full upkeep, a new element
 * too (P4-48).</li>
 * <li><b>Streams</b> ({@link #receive}): a stream is checked for cycles and the hop limit, counted against the throat
 * cap for this second, then passed on by the subclass ({@link #route}), which emits through out-throats
 * ({@link #emit}). Routes are traced by {@link ConduitNet} and cached for 5 s (dropped on any structure check).</li>
 * </ul>
 * <h2>The four traps</h2>
 * <ol>
 * <li>{@link #construct} reads only the trigger; the GUI reads synced values only.</li>
 * <li>Every setting is tile NBT ({@link #saveNBTData}); a wrench drops the throat like any controller slot.</li>
 * <li>Fuel and Burst Control sockets are private ({@link ConduitHatchElement}); no plant-wide list.</li>
 * <li>Every socket chains its fallback casing.</li>
 * </ol>
 */
public abstract class MTEConduitElement<T extends MTEConduitElement<T>> extends MTEExtendedPowerMultiBlockBase<T>
    implements ISurvivalConstructable, IConduitElement {

    /** The upkeep fuel choice (GUI, OpenComputers). NBT keeps the name. */
    public enum FuelMode {

        AUTO("Auto", "Ergomatter when the hatches hold enough, else Molten SpaceTime"),
        SPACETIME("Molten SpaceTime", "always Molten SpaceTime (4 / 16 / 64 L/s)"),
        ERGOMATTER("Ergomatter", "always Ergomatter (0.25 / 1 / 4 L/s, from Eye T7)");

        public static final FuelMode[] VALUES = values();
        public final String label, help;

        FuelMode(String label, String help) {
            this.label = label;
            this.help = help;
        }

        public static FuelMode of(int o) {
            return VALUES[Math.floorMod(o, VALUES.length)];
        }
    }

    /** Periodic structure re-check, ticks. */
    protected static final int RECHECK = 200;

    // ------------------------------------------------- PERSISTED
    protected FuelMode fuelMode = FuelMode.AUTO;
    /** Open: upkeep paid. A new element is closed until 10 s of full upkeep. */
    protected boolean open;
    protected int reopen;
    /** Fractional litres carried to the next second (0.25 L/s Ergomatter drains 1 L every 4 s). */
    protected double owed;
    protected String owedFuel = "";
    protected long litresSpaceTime, litresErgomatter;

    // ------------------------------------------------- WALK
    protected int mCasing;
    protected final ArrayList<MTEHatchInput> fuelHatches = new ArrayList<>();
    protected final ArrayList<MTEHatchBurstControl> burstHatches = new ArrayList<>();

    // ------------------------------------------------- RUN (server)
    protected final FlowBook book = new FlowBook();
    private ConduitNet.Route[] routes;
    private long[] routeTick;
    private String fuelLine = "";
    private String note = "";
    private long noteTick = Long.MIN_VALUE / 2;
    private final QuietTileUpdate quietTileUpdate = new QuietTileUpdate();

    protected MTEConduitElement(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTEConduitElement(String aName) {
        super(aName);
    }

    // ============================================================================ what a subclass is

    /** The shape and the {@code manifest.json machines} key. */
    public abstract String piece();

    /** The machine's name ("Wormhole Mouth"). */
    public abstract String elementName();

    /** True when throat {@code i} takes streams in now (the Junction's mode decides). */
    protected abstract boolean takesIn(int i);

    /** True when throat {@code i} takes a stream from either side (a Wormhole Mouth's ring). */
    protected boolean twoSided(int i) {
        return false;
    }

    /**
     * Pass a stream on: it came in on {@code in}, already capped, with this element in its path. Call {@link #emit}
     * for each out-throat.
     */
    protected abstract void route(BurstStream s, int in, long now);

    /** Lines a subclass adds to the scanner / GUI (after the common ones). */
    protected void extraStatus(List<String> out, long now) {}

    /** Once a second while formed, after the upkeep (mouth: dock; junction: poll its hatches). */
    protected void extraSecond(IGregTechTileEntity base, long now) {}

    // ============================================================================ shape helpers

    public ConduitThroats.Throat[] throats() {
        return ConduitThroats.of(piece());
    }

    /** Throat {@code i} in the world for this controller's facing. */
    public ConduitThroats.WorldThroat worldThroat(int i) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return ConduitThroats
            .world(throats()[i], getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    protected static <T> IStructureElement<T> casing(Casings casing, java.util.function.Consumer<T> pass) {
        return noOverwrite(onElementPass(pass, casing.asElement()));
    }

    protected static <T> IStructureElement<T> block(Block block, int meta, java.util.function.Consumer<T> pass) {
        return noOverwrite(onElementPass(pass, ofBlock(block, meta)));
    }

    /** The {@code u}, {@code e} and {@code m} sockets every element has (fallbacks: spatial / temporal casings). */
    protected static <T extends MTEConduitElement<T>> StructureDefinition.Builder<T> commonSockets(
        StructureDefinition.Builder<T> b, Class<T> type) {
        final int shell = Casings.ReinforcedSpatialStructureCasing.textureId;
        return b.addElement(
            'u',
            buildHatchAdder(type).atLeast(ConduitHatchElement.Fuel)
                .casingIndex(shell)
                .hint(1)
                .buildAndChain(casing(Casings.ReinforcedSpatialStructureCasing, x -> ++x.mCasing)))
            .addElement(
                'e',
                ofChain(
                    casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing),
                    buildHatchAdder(type).atLeast(Energy, ExoticEnergy)
                        .casingIndex(shell)
                        .hint(3)
                        .build()))
            .addElement(
                'm',
                ofChain(
                    casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing),
                    buildHatchAdder(type).atLeast(Maintenance)
                        .casingIndex(shell)
                        .hint(4)
                        .build()));
    }

    // ============================================================================ the walk

    protected void resetWalk() {
        mCasing = 0;
        fuelHatches.clear();
        burstHatches.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final int[] o = BTSShapes.offset(piece());
        if (!checkPiece(piece(), o[0], o[1], o[2], errors)) return;
        if (liveCount(fuelHatches) == 0) errors.add(StructureErrors.of("beyondthestars.structure.conduit.fuel"));
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onStructureCheckFinished(quietTileUpdate.wrap(aBaseMetaTileEntity));
        quietTileUpdate.after(this, aBaseMetaTileEntity);
        // keep the last routes for the scanner, but re-trace each one on its next stream
        if (routeTick != null) java.util.Arrays.fill(routeTick, Long.MIN_VALUE / 2);
    }

    static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity m : list) if (live(m)) n++;
        return n;
    }

    public boolean addFuelHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchInput)) return false;
        ((MTEHatchInput) mte).updateTexture(casingIndex);
        fuelHatches.add((MTEHatchInput) mte);
        return true;
    }

    public boolean addBurstControlHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchBurstControl)) return false;
        ((MTEHatchBurstControl) mte).updateTexture(casingIndex);
        burstHatches.add((MTEHatchBurstControl) mte);
        return true;
    }

    public int fuelHatchCount() {
        return liveCount(fuelHatches);
    }

    public int burstControlCount() {
        return liveCount(burstHatches);
    }

    public List<MTEHatchInput> fuelHatches() {
        return Collections.unmodifiableList(fuelHatches);
    }

    public List<MTEHatchBurstControl> burstControlHatches() {
        return Collections.unmodifiableList(burstHatches);
    }

    // ==================================================================== construct / survival

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

    // ============================================================================ identity

    @Override
    public String conduitKey() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return "?";
        return BurstStream
            .elementKey(base.getWorld().provider.dimensionId, base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    @Override
    public String conduitLabel() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null) return elementName();
        return elementName() + " at " + base.getXCoord() + ", " + base.getYCoord() + ", " + base.getZCoord();
    }

    @Override
    public boolean conduitValid() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return isValid() && base != null && !base.isDead() && mMachine;
    }

    protected long now() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null || base.getWorld() == null ? 0
            : base.getWorld()
                .getTotalWorldTime();
    }

    /** The throat Mk in the controller slot, 0-3. */
    public int throatMk() {
        return SLQuasarItems.throatTier(mInventory[getControllerSlotIndex()]);
    }

    public double throatCap() {
        final int mk = throatMk();
        return mk == 0 ? 0 : BurstRouting.throatCap(mk);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isFormed() {
        return mMachine;
    }

    // ============================================================================ the stream

    @Override
    public double receive(BurstStream s, int throat) {
        if (s == null || !mMachine) return 0;
        final long now = now();
        final String key = conduitKey();
        if (s.crossed(key)) {
            note("a stream came back here (a cycle): dropped", now);
            return 0;
        }
        if (s.hops() >= BurstRouting.MAX_HOPS) {
            note("a stream reached its " + BurstRouting.MAX_HOPS + "-element limit: dropped", now);
            return 0;
        }
        if (throat < 0 || throat >= throats().length || !takesIn(throat)) {
            note("a stream hit throat " + throat + ", which takes nothing in this mode", now);
            return 0;
        }
        final String sk = s.key() + "|" + throat;
        book.add("in|" + throat + "|" + sk, s.buPerTick, now);
        final int mk = throatMk();
        if (!open || mk == 0) {
            book.add("lost|" + sk, s.buPerTick, now);
            final MTEQuasarEngine q = s.source.resolve();
            if (q != null) q.noteConduitClosed(conduitLabel() + (mk == 0 ? ": no Conduit Throat" : ": closed"));
            return 0;
        }
        final double cap = BurstRouting.throatCap(mk);
        final double used = book.sum("pass|", now, FlowBook.SECOND);
        final double passed = Math.max(0, Math.min(s.buPerTick, cap - used));
        book.add("pass|" + sk, passed, now);
        if (s.buPerTick - passed > 1e-9) book.add("clip|" + sk, s.buPerTick - passed, now);
        route(
            s.through(key)
                .withBu(passed),
            throat,
            now);
        return passed;
    }

    /** Sends {@code s} out of throat {@code out} along its normal, to whatever the (cached) route reaches. */
    protected void emit(int out, BurstStream s, long now) {
        book.add("out|" + out + "|" + s.key(), s.buPerTick, now);
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final World world = base.getWorld();
        ConduitNet.Route r = route(out, now, false);
        if (!deliver(world, r, s)) {
            r = route(out, now, true);
            if (!deliver(world, r, s)) book.add("lost|out|" + out + "|" + s.key(), s.buPerTick, now);
        }
    }

    private static boolean deliver(World world, ConduitNet.Route r, BurstStream s) {
        switch (r.kind) {
            case ELEMENT: {
                final IConduitElement e = ConduitNet.element(world, r.target);
                if (e == null) return false;
                e.receive(s, r.throat);
                return true;
            }
            case RECEPTOR: {
                final MTEHatchGRBReceptor p = ConduitNet.receptorAt(world, r.cell);
                if (p == null) return false;
                p.receive(s);
                return true;
            }
            default:
                return false;
        }
    }

    /** The route out of throat {@code out}: cached for 5 s, re-traced when {@code fresh} or stale. */
    public ConduitNet.Route route(int out, long now, boolean fresh) {
        final int n = throats().length;
        if (routes == null || routes.length != n) {
            routes = new ConduitNet.Route[n];
            routeTick = new long[n];
        }
        if (fresh || routes[out] == null || now - routeTick[out] >= ConduitNet.ROUTE_TICKS) {
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            routes[out] = ConduitNet.trace(base.getWorld(), this, emissionThroat(out));
            routeTick[out] = now;
        }
        return routes[out];
    }

    /** The geometry a stream leaves throat {@code out} by (its centre, along its outward normal). */
    protected ConduitThroats.WorldThroat emissionThroat(int out) {
        return worldThroat(out);
    }

    /** The last route traced out of throat {@code out}, or null (scanner). */
    public ConduitNet.Route lastRoute(int out) {
        return routes == null || out >= routes.length ? null : routes[out];
    }

    protected void note(String text, long now) {
        note = text;
        noteTick = now;
    }

    /** The last event, while it is under a minute old ("" otherwise): the GUI's Last row (B-116). */
    public String lastNote() {
        return !note.isEmpty() && now() - noteTick < 1200 ? note : "";
    }

    // ============================================================================ the tick

    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {}

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        if (aTick > 0 && aTick % RECHECK == 0) checkStructure(true, aBaseMetaTileEntity);
        if (aTick % 20 != 0) return;
        final World world = aBaseMetaTileEntity.getWorld();
        final long now = world.getTotalWorldTime();
        if (!mMachine) {
            ConduitNet.unregister(world, this);
            aBaseMetaTileEntity.setActive(false);
            return;
        }
        upkeep();
        aBaseMetaTileEntity.markDirty(); // open / reopen / owed / litres change every second (P4-85)
        register(world);
        extraSecond(aBaseMetaTileEntity, now);
        book.prune(now);
        aBaseMetaTileEntity.setActive(open && book.sum("pass|", now, FlowBook.SHOW) > 1e-9);
    }

    private void register(World world) {
        final ConduitThroats.Throat[] th = throats();
        final List<ConduitThroats.WorldThroat> in = new ArrayList<>();
        final List<Boolean> two = new ArrayList<>();
        for (int i = 0; i < th.length; i++) {
            if (!takesIn(i)) continue;
            in.add(worldThroat(i));
            two.add(twoSided(i));
        }
        final boolean[] t = new boolean[two.size()];
        for (int i = 0; i < t.length; i++) t[i] = two.get(i);
        ConduitNet.register(world, this, in.toArray(new ConduitThroats.WorldThroat[0]), t);
    }

    // ============================================================================ upkeep

    public static Fluid spaceTimeFluid() {
        final FluidStack s = Materials.SpaceTime.getMolten(1L);
        return s == null ? null : s.getFluid();
    }

    public static Fluid ergomatterFluid() {
        return SLQuasarFluids.ergomatter;
    }

    /** SMBH: a Throat Mk IV also runs on Superdense Ergomatter, 2 L/s (smbh-v1.md §3.4). */
    public static Fluid superdenseFluid() {
        return SLQuasarFluids.superdenseErgomatter;
    }

    /** One second of upkeep: pay {@link BurstRouting#throatUpkeep} from the fuel hatches, open or close. */
    private void upkeep() {
        final int mk = throatMk();
        if (mk == 0) {
            if (open) note("no Conduit Throat in the controller slot: closed", now());
            open = false;
            reopen = 0;
            owed = 0;
            fuelLine = "no throat, no upkeep";
            return;
        }
        final Fluid ergo = ergomatterFluid();
        final Fluid st = spaceTimeFluid();
        final double rateE = BurstRouting.throatUpkeep(mk, true);
        final double rateS = BurstRouting.throatUpkeep(mk, false);
        final boolean useErgo;
        switch (fuelMode) {
            case ERGOMATTER:
                useErgo = true;
                break;
            case SPACETIME:
                useErgo = false;
                break;
            default:
                useErgo = ergo != null && available(fuelHatches, ergo) >= Math.ceil(owedFor(ergo) + rateE);
                break;
        }
        // SMBH: a Mk IV throat prefers Superdense Ergomatter (2 L/s) whenever it may burn the Ergomatter family and
        // the hatches hold enough; its litres count with the Ergomatter's
        final double rateSD = BurstRouting.throatUpkeepSuperdense(mk);
        final Fluid sde = rateSD > 0 && fuelMode != FuelMode.SPACETIME ? superdenseFluid() : null;
        final boolean useSuperdense = sde != null && available(fuelHatches, sde) >= Math.ceil(owedFor(sde) + rateSD);
        final Fluid fluid = useSuperdense ? sde : useErgo ? ergo : st;
        final double rate = useSuperdense ? rateSD : useErgo ? rateE : rateS;
        final String name = fluid == null ? "" : fluid.getName();
        if (!name.equals(owedFuel)) {
            owed = 0;
            owedFuel = name;
        }
        final double need = owed + rate;
        final boolean ok = fluid != null && available(fuelHatches, fluid) >= Math.ceil(need - 1e-9);
        if (ok) {
            final long whole = (long) Math.floor(need + 1e-9);
            final long got = take(fuelHatches, fluid, whole);
            owed = need - got;
            if (useErgo || useSuperdense) litresErgomatter += got;
            else litresSpaceTime += got;
        } else {
            owed = 0;
        }
        fuelLine = String.format(
            Locale.ROOT,
            "%s %s L/s%s",
            useSuperdense ? "Superdense Ergomatter" : useErgo ? "Ergomatter" : "Molten SpaceTime",
            rate < 1 ? String.format(Locale.ROOT, "%.2f", rate) : String.format(Locale.ROOT, "%.0f", rate),
            ok ? "" : " - SHORT");
        if (!ok) {
            if (open)
                note("upkeep short: closed (opens after " + BurstRouting.REOPEN_SECONDS + " s of full upkeep)", now());
            open = false;
            reopen = 0;
        } else if (!open) {
            if (++reopen >= BurstRouting.REOPEN_SECONDS) {
                open = true;
                reopen = 0;
                note("opened: upkeep paid for " + BurstRouting.REOPEN_SECONDS + " s", now());
            }
        }
    }

    private double owedFor(Fluid f) {
        return f != null && f.getName()
            .equals(owedFuel) ? owed : 0;
    }

    static long available(List<MTEHatchInput> list, Fluid fluid) {
        if (fluid == null) return 0;
        long n = 0;
        for (MTEHatchInput hatch : list) {
            if (!live(hatch)) continue;
            for (FluidStack s : storedFluids(hatch)) if (s != null && s.getFluid() == fluid) n += s.amount;
        }
        return n;
    }

    private long take(List<MTEHatchInput> list, Fluid fluid, long amount) {
        if (fluid == null || amount <= 0) return 0;
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

    static FluidStack[] storedFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchInputME) return ((MTEHatchInputME) hatch).getStoredFluids();
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    // ============================================================================ settings

    public FuelMode getFuelMode() {
        return fuelMode;
    }

    public int getFuelModeOrdinal() {
        return fuelMode.ordinal();
    }

    public void setFuelModeOrdinal(int o) {
        fuelMode = FuelMode.of(o);
    }

    public void setFuelMode(FuelMode m) {
        if (m != null) fuelMode = m;
    }

    // ============================================================================ readouts

    /** Bu/t in on throat {@code i} last second. */
    public double inBu(int i) {
        return book.sum("in|" + i + "|", now(), FlowBook.SHOW);
    }

    /** Bu/t out of throat {@code i} last second. */
    public double outBu(int i) {
        return book.sum("out|" + i + "|", now(), FlowBook.SHOW);
    }

    /** Bu/t clipped by the throat cap last second. */
    public double clippedBu() {
        return book.sum("clip|", now(), FlowBook.SHOW);
    }

    /** Bu/t lost last second (closed, or an out-throat that reaches nothing). */
    public double lostBu() {
        return book.sum("lost|", now(), FlowBook.SHOW);
    }

    /** Bu/t through the element last second (what passed the cap). */
    public double throughBu() {
        return book.sum("pass|", now(), FlowBook.SHOW);
    }

    public String getFuelLine() {
        return fuelLine;
    }

    public int getReopen() {
        return reopen;
    }

    /** Upkeep litres a second at the current throat, on Ergomatter / on SpaceTime. */
    public double upkeepRate(boolean ergomatter) {
        final int mk = throatMk();
        return mk == 0 ? 0 : BurstRouting.throatUpkeep(mk, ergomatter);
    }

    public long getLitresSpaceTime() {
        return litresSpaceTime;
    }

    public long getLitresErgomatter() {
        return litresErgomatter;
    }

    public static String bu(double v) {
        return MTEQuasarEngine.bu(v);
    }

    /** The scanner and the bench. Server state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        final long now = now();
        final int mk = throatMk();
        out.add(
            conduitLabel() + (mMachine ? "" : ", NOT FORMED")
                + ": "
                + (mk == 0 ? "no Conduit Throat (closed)"
                    : "throat Mk " + roman(mk) + " (" + bu(BurstRouting.throatCap(mk)) + " Bu/t)")
                + (open ? ", OPEN"
                    : mk == 0 ? ""
                        : ", CLOSED (" + reopen + " / " + BurstRouting.REOPEN_SECONDS + " s of upkeep to open)"));
        out.add(
            "Upkeep (" + fuelMode.label
                + "): "
                + (fuelLine.isEmpty() ? "-" : fuelLine)
                + "; fuel hatches "
                + fuelHatchCount()
                + String.format(
                    Locale.ROOT,
                    "; used %,d L SpaceTime, %,d L Ergomatter",
                    litresSpaceTime,
                    litresErgomatter));
        out.add(
            "Through: " + bu(throughBu())
                + " Bu/t"
                + (clippedBu() > 0.005 ? ", clipped " + bu(clippedBu()) : "")
                + (lostBu() > 0.005 ? ", lost " + bu(lostBu()) : ""));
        extraStatus(out, now);
        if (!note.isEmpty() && now - noteTick < 1200) out.add("Last: " + note);
        return out;
    }

    public String statusText() {
        return String.join("\n", statusLines());
    }

    /** "out 1 -> Conduit Mirror at ... (40 blocks)" for each out-throat {@code outs}. */
    protected void routeLines(List<String> out, int[] outs, String[] names) {
        for (int k = 0; k < outs.length; k++) {
            final ConduitNet.Route r = lastRoute(outs[k]);
            out.add(
                names[k] + ": "
                    + bu(outBu(outs[k]))
                    + " Bu/t -> "
                    + (r == null ? "(not traced yet: no stream)" : r.describe()));
        }
    }

    static String roman(int mk) {
        return mk == 1 ? "I" : mk == 2 ? "II" : mk == 3 ? "III" : String.valueOf(mk);
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    // ===================================================================================== NBT

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        final NBTTagCompound t = new NBTTagCompound();
        t.setString("fuel", fuelMode.name());
        t.setBoolean("open", open);
        t.setInteger("reopen", reopen);
        t.setDouble("owed", owed);
        t.setString("owedFuel", owedFuel);
        t.setLong("litresST", litresSpaceTime);
        t.setLong("litresErgo", litresErgomatter);
        writeElement(t);
        aNBT.setTag("btsConduit", t);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        final NBTTagCompound t = aNBT.getCompoundTag("btsConduit");
        try {
            fuelMode = t.hasKey("fuel") ? FuelMode.valueOf(t.getString("fuel")) : FuelMode.AUTO;
        } catch (IllegalArgumentException bad) {
            fuelMode = FuelMode.AUTO;
        }
        open = t.getBoolean("open");
        reopen = t.getInteger("reopen");
        owed = t.getDouble("owed");
        if (!Double.isFinite(owed)) owed = 0; // a save from before the setters refused NaN heals (P4-91)
        owedFuel = t.getString("owedFuel");
        litresSpaceTime = t.getLong("litresST");
        litresErgomatter = t.getLong("litresErgo");
        readElement(t);
    }

    /** Subclass settings into the element's tag. */
    protected void writeElement(NBTTagCompound t) {}

    protected void readElement(NBTTagCompound t) {}

    // ============================================================================ tooltip helpers

    protected static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    /** The lines every element's tooltip shares (throat, upkeep, rays). */
    protected static MultiblockTooltipBuilder commonInfo(MultiblockTooltipBuilder tt) {
        return tt.addInfo("Throat: a Conduit Throat in the controller slot (swap to upgrade): Mk I 24, Mk II 384,")
            .addInfo("  Mk III 6,144, Mk IV 524,288 Bu/t. Above the cap the stream is clipped (shown), never damaged.")
            .addInfo("Upkeep, one unit per element: Molten SpaceTime 4 / 16 / 64 / 256 L/s by throat Mk, or")
            .addInfo("  Ergomatter 0.25 / 1 / 4 / 16 L/s (after Eye T7); Mk IV prefers Superdense Ergomatter, 2 L/s.")
            .addInfo("  GUI: Auto / SpaceTime / Ergomatter. Short upkeep closes")
            .addInfo("  the element (its streams stop); it opens after 10 s of full upkeep (a new element too).")
            .addInfo("Between elements: a straight ray of up to 64 blocks of air out of a throat into the next")
            .addInfo("  element's throat (or onto a GRB Receptor Port). Any block in the way stops it (the scanner")
            .addInfo("  says where). The stream keeps its source quasar all the way: that is the host's link.");
    }

    protected static MultiblockTooltipBuilder commonStructure(MultiblockTooltipBuilder tt, String piece) {
        return tt.addCasingInfoExactly("Reinforced Spatial Structure Casing", bom(piece, 'E'), false)
            .addCasingInfoExactly("Reinforced Temporal Structure Casing", bom(piece, 'A'), false)
            .addCasingInfoExactly("Casimir Plate", bom(piece, 'P'), false)
            .addCasingInfoExactly("Jet Collimator Ring", bom(piece, 'C'), false)
            .addOtherStructurePart(
                "Input Hatch (throat fuel, ME stocking OK)",
                bom(piece, 'u') + " sockets, at least 1",
                1)
            .addStructureInfo("Any facing (wrench the front; sneak-wrench flips). Builds never overwrite a hatch.")
            .addStructureInfo("No maintenance or power needed (the energy socket is optional).");
    }

    protected ITexture[] elementTexture(ForgeDirection side, ForgeDirection facing, boolean active,
        IIconContainer[] overlays) {
        final ITexture base = Casings.ReinforcedTemporalStructureCasing.getCasingTexture();
        if (side != facing) return new ITexture[] { base };
        return new ITexture[] { base, TextureFactory.builder()
            .addIcon(active ? overlays[2] : overlays[0])
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(active ? overlays[3] : overlays[1])
                .extFacing()
                .glow()
                .build() };
    }

    /** The four overlays of a front texture set: base, glow, active, active glow. */
    protected static IIconContainer[] overlays(String name) {
        return new IIconContainer[] { Textures.BlockIcons.custom("beyondthestars:iconsets/" + name),
            Textures.BlockIcons.custom("beyondthestars:iconsets/" + name + "_GLOW"),
            Textures.BlockIcons.custom("beyondthestars:iconsets/" + name + "_ACTIVE"),
            Textures.BlockIcons.custom("beyondthestars:iconsets/" + name + "_ACTIVE_GLOW") };
    }

    // ===================================================================================== GUI

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new ConduitElementGui<>(this);
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.emptyList();
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    /** For the bench: the facing a controller of this element needs so throat {@code i} faces {@code normal}. */
    public static ExtendedFacing facingFor(ConduitThroats.Throat[] th, int i, double[] normal, int j, double[] other) {
        ExtendedFacing best = null;
        for (ExtendedFacing f : ExtendedFacing.VALUES) {
            if (f.getFlip()
                .isVerticallyFliped()) continue;
            final double[] n = ConduitThroats.worldDir(f, th[i].normalAbc);
            if (ConduitNet.dot(n, normal) < 0.999) continue;
            if (j >= 0) {
                final double[] m = ConduitThroats.worldDir(f, th[j].normalAbc);
                if (ConduitNet.dot(m, other) < 0.999) continue;
            }
            if (best == null || score(f) < score(best)) best = f;
        }
        return best;
    }

    private static int score(ExtendedFacing f) {
        return (f.getDirection().offsetY != 0 ? 4 : 0) + f.getRotation()
            .getIndex()
            + f.getFlip()
                .getIndex();
    }
}
