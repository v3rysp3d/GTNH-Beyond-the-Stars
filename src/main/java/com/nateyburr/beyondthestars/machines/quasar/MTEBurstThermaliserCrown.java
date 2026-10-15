package com.nateyburr.beyondthestars.machines.quasar;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing4;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.QuietTileUpdate;
import com.nateyburr.beyondthestars.machines.conduit.ConduitRender;
import com.nateyburr.beyondthestars.machines.conduit.ConduitThroats;
import com.nateyburr.beyondthestars.machines.conduit.FlowBook;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstDocks;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;
import com.nateyburr.beyondthestars.machines.quasar.burst.IBurstDock;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPacket;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

/**
 * P4: the BURST THERMALISER CROWN, MTE base+24 = 28124 (quasar-p4.md §2, §7; D-30, D-32, D-35, Q-7). One controller;
 * its Mk is the biggest of the three drawn crowns that stands around it ({@code crown_mk3}, then {@code mk2}, then
 * {@code mk1}): Mk I / II / III take 32 / 512 / 8,192 Bu/t. Upgrading is building the bigger crown around the same
 * controller.
 * <ul>
 * <li><b>On a jet port</b> (a dock): its base centre (the intake, {@code manifest.json machines.crown.intake}) sits on
 * the quasar's port cell, the crown standing on the cap (upside down under the bottom cap). It registers that cell with
 * {@link BurstDocks} every second and takes the jet up to its cap (and a stacked crown's).</li>
 * <li><b>Remote</b>: the GRB Receptor Port in its base ({@code r}) takes a conduit stream.</li>
 * <li><b>Power</b>: EU/t = 10¹³ × min(S, cap) ({@link BurstRouting#crownEUt}), pushed once a second to the owner's
 * wireless network (D-32 "wireless only"; no dynamo). Overflow goes to a crown stacked on this one
 * ({@code machines.crown.stack}, same facing); two can stack. The rest is clipped (shown).</li>
 * </ul>
 */
public class MTEBurstThermaliserCrown extends MTEExtendedPowerMultiBlockBase<MTEBurstThermaliserCrown>
    implements ISurvivalConstructable, IBurstDock, IMTERenderer {

    /** Mk I-III (P4) and Mk IV, the Event Horizon Crown (SMBH v1, smbh-v1.md §3.4): the biggest that stands wins. */
    public static final String[] PIECES = { "crown_mk1", "crown_mk2", "crown_mk3", "crown_mk4" };
    /** The highest Mk. */
    public static final int MK_MAX = PIECES.length;
    public static final String CHANNEL_CROWN = "crown";
    private static final int RECHECK = 200;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_BURST_CROWN");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_BURST_CROWN_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_BURST_CROWN_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_BURST_CROWN_ACTIVE_GLOW");

    private static volatile IStructureDefinition<MTEBurstThermaliserCrown> STRUCTURE_DEFINITION;
    private static volatile int[][] INTAKE, INTAKE_AXIS, STACK;

    // ---- persisted
    /** The Mk last formed (picks which piece's errors the GUI shows). */
    private int lastMk = 1;
    /**
     * QA-B38 (QK-04): the Mk the hologram projector last asked for (server, not saved; 0: none). While it has not
     * formed, the structure errors are that Mk's: an upgrade in progress lists the cells the bigger crown still needs,
     * not the old crown's.
     */
    private int askedMk;
    private BigInteger lifetimeEU = BigInteger.ZERO;

    // ---- walk
    private int mCasing;
    private int mk;
    private final ArrayList<MTEHatchGRBReceptor> receptors = new ArrayList<>();

    // ---- run
    private final FlowBook book = new FlowBook();
    private final BurstHost remote = new BurstHost();
    private final QuietTileUpdate quietTileUpdate = new QuietTileUpdate();
    private UUID owner;
    private double lastStream, lastUsed, lastOver, lastClipped, lastRemote, lastJets, lastOverIn;
    private String stackLine = "";
    private String dockNote = "";
    /** Client: the Mk and the burst taken / thermalised last second, as last sent (renders v2 §10, never saved). */
    private int renderMk;
    private float renderStream, renderUsed;
    private long dockTick = Long.MIN_VALUE / 2;

    public MTEBurstThermaliserCrown(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEBurstThermaliserCrown(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEBurstThermaliserCrown(mName);
    }

    // =================================================================================== shape

    @Override
    public IStructureDefinition<MTEBurstThermaliserCrown> getStructureDefinition() {
        IStructureDefinition<MTEBurstThermaliserCrown> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEBurstThermaliserCrown.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    final int shell = Casings.ExtremeDensitySpaceBendingCasing.textureId;
                    final StructureDefinition.Builder<MTEBurstThermaliserCrown> b = StructureDefinition
                        .<MTEBurstThermaliserCrown>builder();
                    for (String p : PIECES) b.addShape(p, BTSShapes.load(p));
                    def = b.addElement('A', casing(Casings.ExtremeDensitySpaceBendingCasing))
                        .addElement('C', block(BlockSLCasing2.JET_COLLIMATOR))
                        .addElement('F', block(BlockSLCasing2.BURST_THERMALISER))
                        .addElement('R', block(BlockSLCasing2.RADIATOR_PETAL))
                        // SMBH: the Mk IV crown's core (Event Horizon Thermaliser Casing, slCasing4:5)
                        .addElement(
                            'X',
                            noOverwrite(
                                onElementPass(
                                    x -> ++x.mCasing,
                                    ofBlock(SLBlocks.slCasing4, BlockSLCasing4.HORIZON_THERMALISER))))
                        .addElement(
                            'r',
                            buildHatchAdder(MTEBurstThermaliserCrown.class).atLeast(CrownHatchElement.Receptor)
                                .casingIndex(shell)
                                .hint(1)
                                .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
                        .addElement(
                            'm',
                            ofChain(
                                casing(Casings.ExtremeDensitySpaceBendingCasing),
                                buildHatchAdder(MTEBurstThermaliserCrown.class).atLeast(Maintenance)
                                    .casingIndex(shell)
                                    .hint(2)
                                    .build()))
                        .build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureElement<MTEBurstThermaliserCrown> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEBurstThermaliserCrown> block(int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(SLBlocks.slCasing2, meta)));
    }

    /** Standing on a cap (normal) or hanging under one (upside down): horizontal facings only. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && !f.isVerticallyFliped() && (r.isNotRotated() || r.isUpsideDown());
    }

    private static void loadGeometry() {
        if (STACK != null) return;
        synchronized (MTEBurstThermaliserCrown.class) {
            if (STACK != null) return;
            final JsonObject m = BTSShapes.manifest()
                .getAsJsonObject("machines")
                .getAsJsonObject("crown");
            final int[][] in = new int[MK_MAX][], ax = new int[MK_MAX][], st = new int[MK_MAX][];
            for (int i = 0; i < MK_MAX; i++) {
                in[i] = vec(
                    m.getAsJsonObject("intake")
                        .getAsJsonObject(PIECES[i]),
                    "rel");
                ax[i] = vec(
                    m.getAsJsonObject("intake")
                        .getAsJsonObject(PIECES[i]),
                    "axisAbc");
                st[i] = vec(
                    m.getAsJsonObject("stack")
                        .getAsJsonObject(PIECES[i]),
                    "rel");
            }
            INTAKE = in;
            INTAKE_AXIS = ax;
            STACK = st;
        }
    }

    private static int[] vec(JsonObject o, String name) {
        final JsonArray a = o == null ? null : o.getAsJsonArray(name);
        if (a == null || a.size() != 3) throw new IllegalStateException("[BTS] crown manifest: bad '" + name + "'");
        return new int[] { a.get(0)
            .getAsInt(),
            a.get(1)
                .getAsInt(),
            a.get(2)
                .getAsInt() };
    }

    /** The intake (base centre) offset for Mk {@code mk}, from the controller. */
    public static int[] intakeRel(int mk) {
        loadGeometry();
        return INTAKE[Math.max(1, Math.min(MK_MAX, mk)) - 1].clone();
    }

    /** Where a crown stacked on a Mk {@code mk} crown has its controller, from this one's. */
    public static int[] stackRel(int mk) {
        loadGeometry();
        return STACK[Math.max(1, Math.min(MK_MAX, mk)) - 1].clone();
    }

    /** The jet's direction into the base, (a, b, c). */
    public static int[] intakeAxis(int mk) {
        loadGeometry();
        return INTAKE_AXIS[Math.max(1, Math.min(MK_MAX, mk)) - 1].clone();
    }

    // ==================================================================================== walk

    private void resetWalk() {
        mCasing = 0;
        receptors.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        // the biggest crown that stands wins; a failed try reports only for the Mk last formed (its errors are the
        // ones the player is fixing)
        mk = 0;
        List<StructureError> asked = null;
        for (int t = MK_MAX; t >= 1; t--) {
            clearHatches();
            resetWalk();
            final int[] o = BTSShapes.offset(PIECES[t - 1]);
            final List<StructureError> trial = new ArrayList<>();
            if (checkPiece(PIECES[t - 1], o[0], o[1], o[2], trial)) {
                mk = t;
                lastMk = t;
                if (askedMk <= t) askedMk = 0;
                return;
            }
            if (t == askedMk) asked = trial;
            if (t == lastMk) errors.addAll(trial);
        }
        // QA-B38 (QK-04): nothing stands (an upgrade has replaced a cell of the old Mk): list the cells of the Mk the
        // projector is building, not the old Mk's
        if (asked != null && askedMk != lastMk) {
            errors.clear();
            errors.addAll(asked);
        }
        clearHatches();
        resetWalk();
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onStructureCheckFinished(quietTileUpdate.wrap(aBaseMetaTileEntity));
        quietTileUpdate.after(this, aBaseMetaTileEntity);
    }

    public boolean addReceptor(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity m = te.getMetaTileEntity();
        if (!(m instanceof MTEHatchGRBReceptor)) return false;
        ((MTEHatchGRBReceptor) m).updateTexture(casingIndex);
        receptors.add((MTEHatchGRBReceptor) m);
        return true;
    }

    public int receptorCount() {
        int n = 0;
        for (MTEHatchGRBReceptor r : receptors) if (live(r)) n++;
        return n;
    }

    private static boolean live(MetaTileEntity m) {
        return m != null && m.isValid();
    }

    // ==================================================================== construct / survival

    /** The Mk the trigger asks for: the {@code crown} channel, else the stack size (1-4). Trap 1. */
    public static int triggerMk(ItemStack trigger) {
        final int ch = BTSStructures.channelIndex(trigger, CHANNEL_CROWN, -1) + 1;
        if (ch >= 1) return Math.min(MK_MAX, ch);
        return trigger == null ? 1 : Math.max(1, Math.min(MK_MAX, trigger.stackSize));
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final String piece = PIECES[triggerMk(stackSize) - 1];
        final int[] o = BTSShapes.offset(piece);
        buildPiece(piece, stackSize, hintsOnly, o[0], o[1], o[2]);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final int want = triggerMk(stackSize);
        if (want > mk) askedMk = want;
        if (mMachine && mk >= want) return -1;
        final String piece = PIECES[want - 1];
        final int[] o = BTSShapes.offset(piece);
        return survivalBuildPiece(
            piece,
            stackSize,
            o[0],
            o[1],
            o[2],
            BTSStructures.survivalBudget(elementBudget),
            env,
            false,
            true);
    }

    // ==================================================================================== tick

    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {}

    /**
     * QA-B13: GT resets the active flag from mMaxProgresstime every tick, and this machine runs no recipe: the flag
     * worked out once a second is set again every tick after GT's, so the ACTIVE overlay shows and clients are not
     * sent two texture changes a second.
     */
    private boolean activeNow;

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        tickServer(aBaseMetaTileEntity, aTick);
        aBaseMetaTileEntity.setActive(activeNow);
    }

    private void tickServer(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (owner == null && aBaseMetaTileEntity.getOwnerUuid() != null) {
            owner = WirelessNetworkManager.processInitialSettings(aBaseMetaTileEntity);
        }
        if (aTick > 0 && aTick % RECHECK == 0) checkStructure(true, aBaseMetaTileEntity);
        if (aTick % 20 != 0) return;
        final World world = aBaseMetaTileEntity.getWorld();
        final long now = world.getTotalWorldTime();
        if (!mMachine) {
            lastStream = lastUsed = lastOver = lastClipped = 0;
            activeNow = (false);
            if (aTick % 100 == 0) sendRenderDataToClient(this);
            return;
        }
        final int[] intake = intakeCell();
        BurstDocks.register(world, intake[0], intake[1], intake[2], this);
        second(now);
        aBaseMetaTileEntity.markDirty(); // the lifetime EU changes every second (P4-85)
        book.prune(now);
        activeNow = (lastUsed > 1e-9);
        sendRenderDataToClient(this);
    }

    // ============================================================================ renders v2

    /** Renders v2 (§10): the intake's heat bloom (client; ConduitRender). */
    @Override
    public void renderTESR(double x, double y, double z, float partialTicks) {
        ConduitRender.renderCrown(this, x, y, z, partialTicks);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x - 30, y - 30, z - 30, x + 31, y + 31, z + 31);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256.0 * 256.0;
    }

    /** Server, once a second: the Mk and the burst taken and thermalised last second (render-only, never saved). */
    @Override
    public void encodeRenderData(ByteBuf buf) {
        buf.writeByte(mMachine ? mk : 0);
        buf.writeFloat((float) lastStream);
        buf.writeFloat((float) lastUsed);
    }

    /** Client. */
    @Override
    public void decodeRenderData(ByteArrayDataInput in) {
        renderMk = in.readByte();
        renderStream = in.readFloat();
        renderUsed = in.readFloat();
    }

    /** Client: the Mk as last sent (0 not formed). */
    public int renderMk() {
        return renderMk;
    }

    /** Client: the stream taken last second, Bu/t. */
    public float renderStream() {
        return renderStream;
    }

    /** Client: the burst thermalised last second, Bu/t. */
    public float renderUsed() {
        return renderUsed;
    }

    /** One crown second: jets + the receptor + a lower crown's overflow; EU to wireless; overflow up the stack. */
    private void second(long now) {
        final BurstReading r = remote.second(receptors, crownKey(), "Burst Thermaliser Crown Mk " + roman(mk), 0, 0);
        // QA-B45: a crown has nothing to freeze: what arrived is power, even while another source is paused
        lastRemote = r.total;
        lastJets = book.sum("jet|", now, FlowBook.SHOW);
        lastOverIn = book.sum("over|", now, FlowBook.SHOW);
        final double s = lastJets + lastRemote + lastOverIn;
        final double cap = BurstRouting.crownCap(mk);
        final double used = Math.min(s, cap);
        double over = s - used;
        lastStream = s;
        lastUsed = used;
        lastOver = 0;
        lastClipped = 0;
        stackLine = "";
        final MTEBurstThermaliserCrown above = lastOverIn > 1e-9 ? null : stacked();
        if (above != null) {
            stackLine = "stacked: Mk " + roman(above.mk) + " above";
            if (over > 1e-9) {
                above.addOverflow(crownKey(), over, now);
                lastOver = over;
                over = 0;
            }
        }
        lastClipped = over;
        final double eut = BurstRouting.crownEUt(used);
        if (eut >= 1 && owner != null) {
            final BigInteger eu = BurstRouting.crownEUPerSecond(used); // no long on the way (SMBH §3.4)
            WirelessNetworkManager.addEUToGlobalEnergyMap(owner, eu);
            lifetimeEU = lifetimeEU.add(eu);
        }
    }

    /** A lower crown hands its overflow up the stack. */
    void addOverflow(String from, double bu, long now) {
        book.add("over|" + from, bu, now);
    }

    /** The crown stacked on this one (same facing, formed), or null. */
    public MTEBurstThermaliserCrown stacked() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || mk == 0) return null;
        final int[] p = QuasarGeometry
            .world(getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), stackRel(mk));
        final World world = base.getWorld();
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity m = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(m instanceof MTEBurstThermaliserCrown)) return null;
        final MTEBurstThermaliserCrown c = (MTEBurstThermaliserCrown) m;
        return c.mMachine && c.getExtendedFacing() == getExtendedFacing() ? c : null;
    }

    private String crownKey() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return "crown:" + base.getWorld().provider.dimensionId
            + ":"
            + base.getXCoord()
            + ","
            + base.getYCoord()
            + ","
            + base.getZCoord();
    }

    /** The intake cell (base centre) in the world, where a quasar's port cell must be. */
    public int[] intakeCell() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return QuasarGeometry.world(
            getExtendedFacing(),
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            intakeRel(Math.max(1, mk)));
    }

    // =================================================================================== dock

    @Override
    public boolean dockReady() {
        return mMachine && mk > 0;
    }

    @Override
    public double acceptJet(JetPacket jet) {
        final long now = getBaseMetaTileEntity().getWorld()
            .getTotalWorldTime();
        final MTEQuasarEngine q = jet.source.resolve();
        if (q == null) return 0;
        final int[] a = QuasarGeometry.axisAbc(jet.port);
        final double[] jetAxis = ConduitThroats.worldDir(q.getExtendedFacing(), new double[] { a[0], a[1], a[2] });
        final int[] ia = intakeAxis(mk);
        final double[] mine = ConduitThroats.worldDir(getExtendedFacing(), new double[] { ia[0], ia[1], ia[2] });
        dockTick = now;
        if (jetAxisDot(jetAxis, mine) < 0.999) {
            dockNote = "misaligned on the " + jet.port.label.toLowerCase(Locale.ROOT)
                + " jet port: stand on the cap (upside down under the bottom cap)";
            return REFUSED;
        }
        dockNote = "docked on the " + jet.port.label.toLowerCase(Locale.ROOT)
            + " jet port of the quasar at "
            + jet.source.label();
        final MTEBurstThermaliserCrown above = stacked();
        final double cap = BurstRouting.crownCap(mk) + (above == null ? 0 : BurstRouting.crownCap(above.mk));
        final double used = book.sum("jet|", now, FlowBook.SECOND) + lastRemote;
        final double taken = Math.max(0, Math.min(jet.buPerTick, cap - used));
        book.add("jet|" + jet.source.label() + "|" + jet.port.key, taken, now);
        return taken;
    }

    private static double jetAxisDot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    @Override
    public String dockLabel() {
        return "Crown Mk " + roman(mk) + (stacked() != null ? " (+ stacked)" : "");
    }

    // ================================================================================ readouts

    public int getMk() {
        return mMachine ? mk : 0;
    }

    public double getStream() {
        return lastStream;
    }

    public double getUsed() {
        return lastUsed;
    }

    /** EU/t pushed to wireless last second. */
    public double getEUt() {
        return BurstRouting.crownEUt(lastUsed);
    }

    public double getClipped() {
        return lastClipped;
    }

    public BigInteger getLifetimeEU() {
        return lifetimeEU;
    }

    public UUID getOwner() {
        return owner;
    }

    public BurstHost remoteHost() {
        return remote;
    }

    /** B-116 GUI: last second's parts of the stream (the jets, the receptor, a lower crown's overflow). */
    public double getJets() {
        return lastJets;
    }

    public double getRemote() {
        return lastRemote;
    }

    public double getOverIn() {
        return lastOverIn;
    }

    /** Bu/t handed up to the crown stacked above last second. */
    public double getOver() {
        return lastOver;
    }

    /** "stacked: Mk II above", or "". */
    public String getStackLine() {
        return stackLine;
    }

    /** The dock note while it is fresh (3 s), or "" (no jet port docked). */
    public String getDockNote() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final long now = base == null || base.getWorld() == null ? 0
            : base.getWorld()
                .getTotalWorldTime();
        return now - dockTick < 60 ? dockNote : "";
    }

    static String roman(int mk) {
        return mk == 1 ? "I" : mk == 2 ? "II" : mk == 3 ? "III" : mk == 4 ? "IV" : "-";
    }

    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        final long now = getBaseMetaTileEntity() == null ? 0
            : getBaseMetaTileEntity().getWorld()
                .getTotalWorldTime();
        out.add(
            "Burst Thermaliser Crown"
                + (mMachine ? " Mk " + roman(mk) + " (cap " + MTEQuasarEngine.buEu(BurstRouting.crownCap(mk)) + ")"
                    : ", NOT FORMED"));
        out.add(
            String.format(
                Locale.ROOT,
                "Stream %s (jets %s, receptor %s, from below %s) -> %s used",
                MTEQuasarEngine.buEu(lastStream),
                MTEQuasarEngine.buEu(lastJets),
                MTEQuasarEngine.buEu(lastRemote),
                MTEQuasarEngine.buEu(lastOverIn),
                MTEQuasarEngine.buEu(lastUsed)));
        out.add(
            String.format(
                Locale.ROOT,
                "Power: %.3e EU/t (%s) to the wireless network",
                getEUt(),
                BTSFormat.euWords(getEUt())) + (owner == null ? " - NO OWNER (place it yourself)" : ""));
        if (lastOver > 1e-9) out.add("Overflow " + MTEQuasarEngine.buEu(lastOver) + " to the crown above");
        if (lastClipped > 0.005) out.add("Clipped " + MTEQuasarEngine.buEu(lastClipped) + " (over the cap)");
        if (!stackLine.isEmpty()) out.add(stackLine);
        if (now - dockTick < 60 && !dockNote.isEmpty()) out.add(dockNote);
        else out.add("Jet port: none docked (the base centre must sit on a quasar's port cell)");
        if (receptorCount() > 0) out.add(
            "Receptor: " + remote.last()
                .line());
        out.add("Lifetime: " + String.format(Locale.ROOT, "%,d", lifetimeEU) + " EU");
        return out;
    }

    public String statusText() {
        return String.join("\n", statusLines());
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    // =================================================================================== NBT

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsCrownLastMk", lastMk);
        aNBT.setString("btsCrownLifetime", lifetimeEU.toString());
    }

    /** QA-B16: a wrenched crown keeps its lifetime EU and the Mk it last formed. */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        aNBT.setInteger("btsCrownLastMk", lastMk);
        aNBT.setString("btsCrownLifetime", lifetimeEU.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        lastMk = Math.max(1, Math.min(MK_MAX, aNBT.hasKey("btsCrownLastMk") ? aNBT.getInteger("btsCrownLastMk") : 1));
        try {
            lifetimeEU = aNBT.hasKey("btsCrownLifetime") ? new BigInteger(aNBT.getString("btsCrownLifetime"))
                : BigInteger.ZERO;
        } catch (NumberFormatException bad) {
            lifetimeEU = BigInteger.ZERO;
        }
    }

    // ================================================================================ tooltip

    private static int bom(int mk, char c) {
        try {
            return BTSShapes.count(PIECES[mk - 1], c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    /** "Radiator Petal (Mk I; Mk II 1,428, Mk III 2,803, Mk IV 4,599)": the count shown is Mk I's. */
    private static String casingName(String name, char c) {
        return String
            .format(Locale.ROOT, "%s (Mk I; Mk II %,d, Mk III %,d, Mk IV %,d)", name, bom(2, c), bom(3, c), bom(4, c));
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] size = BTSShapes.size(PIECES[MK_MAX - 1]);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Gamma-Ray Burst Power Plant")
            .addInfo("Turns gamma-ray bursts into EU: 1 Bu/t = 10^13 EU/t, pushed to your wireless network.")
            .addInfo("  No dynamo hatch. Its Mk is the crown built around the controller:")
            .addInfo("  Mk I 32 Bu/t (3.2e14 EU/t), Mk II 512 Bu/t (5.1e15 EU/t), Mk III 8,192 Bu/t (8.2e16 EU/t).")
            .addInfo("  Upgrade by building the bigger crown around the same controller. The bigger Mk does not keep")
            .addInfo(
                "  the smaller one's petals and collimators: clear them first (the projector never breaks a block).")
            .addInfo("  A crown stacked on top sits higher over a bigger lower crown: re-place it after the upgrade.")
            .addInfo(
                "  Mk IV, the Event Horizon Crown: 524,288 Bu/t (5.2e18 EU/t, 5.2 quintillion), for a Supermassive")
            .addInfo(
                "  Lattice quasar. Its core is Event Horizon Thermaliser Casing (4 Mk IV are the most any hole needs).")
            .addInfo("On a Quasar Engine jet port: the base centre on the port cell (the cell just past the cap,")
            .addInfo("  on the jet axis), standing on the top cap, upside down under the bottom cap (wrench the")
            .addInfo("  front to rotate). It takes that jet. A portable scanner on the Quasar Engine prints both port")
            .addInfo("  cells, and where each crown's controller goes and which way it faces.")
            .addInfo("Remote: a GRB Receptor Port in the base takes a conduit stream.")
            .addInfo("A second crown stacked on this one (same facing) takes the overflow; the rest is clipped.")
            .addInfo("Automation: nothing to do. OpenComputers bts_crown (getPower, getStream, getMk).")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("Base, beside the intake")
            .addCasingInfoExactly(casingName("Radiator Petal", 'R'), bom(1, 'R'), false)
            .addCasingInfoExactly(casingName("Burst Thermaliser Casing", 'F'), bom(1, 'F'), false)
            .addCasingInfoExactly("Event Horizon Thermaliser Casing (Mk IV only)", bom(4, 'X'), false)
            .addCasingInfoExactly(casingName("Extreme Density Space-Bending Casing", 'A'), bom(1, 'A'), false)
            .addCasingInfoExactly(casingName("Jet Collimator Ring", 'C'), bom(1, 'C'), false)
            .addOtherStructurePart("GRB Receptor Port (remote feed)", "1 base socket, optional", 1)
            .addStructureInfo("Hologram: the crown channel (or trigger stack 1-4) picks the Mk.")
            .addStructureInfo("No energy or maintenance hatch needed. Builds never overwrite a hatch.")
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

    // ===================================================================================== GUI

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new CrownGui(this);
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.singletonList(QuasarRecipeMaps.THERMALISER);
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

    /** For the bench: the facing a docked crown needs on a quasar's port. */
    public static ExtendedFacing dockFacing(ExtendedFacing quasar,
        com.nateyburr.beyondthestars.machines.quasar.burst.JetPort port) {
        final int[] f = new int[3];
        quasar.getWorldOffset(QuasarGeometry.crownFacingAbc(port), f);
        final ForgeDirection dir = ForgeDirection.UNKNOWN;
        ForgeDirection d = dir;
        for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
            if (fd.offsetX == f[0] && fd.offsetY == f[1] && fd.offsetZ == f[2]) d = fd;
        }
        final int[] want = new int[3];
        quasar.getWorldOffset(QuasarGeometry.axisAbc(port), want);
        for (ExtendedFacing e : ExtendedFacing.VALUES) {
            if (e.getDirection() != d || e.getFlip()
                .isVerticallyFliped()) continue;
            if (!(e.getRotation()
                .isNotRotated()
                || e.getRotation()
                    .isUpsideDown()))
                continue;
            final int[] ia = intakeAxis(1);
            final int[] w = new int[3];
            e.getWorldOffset(ia, w);
            if (w[0] == want[0] && w[1] == want[1] && w[2] == want[2]) return e;
        }
        return null;
    }
}
