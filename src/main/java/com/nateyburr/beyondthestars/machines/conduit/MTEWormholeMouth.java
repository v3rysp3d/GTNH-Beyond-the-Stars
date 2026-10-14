package com.nateyburr.beyondthestars.machines.conduit;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.QuasarGeometry;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstDocks;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;
import com.nateyburr.beyondthestars.machines.quasar.burst.IBurstDock;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPacket;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * P4: the WORMHOLE MOUTH, MTE base+21 = 28121 (quasar-p4.md §3, §7; D-36). Two mouths paired with a data stick are one
 * wormhole: whatever enters one leaves the other, instantly and losslessly, at any distance and across dimensions, as
 * long as both are loaded. A stream enters a mouth's ring <b>through the front face</b> (the controller's side) and
 * leaves its partner <b>out of the back</b>, along the ring's axis.
 * <ul>
 * <li><b>On a jet port</b> (a dock, {@link IBurstDock}): the ring's front-face centre sits on the quasar's port cell,
 * facing the quasar, so the jet runs into the front. It registers that cell with {@link BurstDocks} every second.</li>
 * <li><b>From a ray</b>: a stream from a Mirror or Junction that crosses the ring (from either side) enters too.</li>
 * <li><b>Pairing</b>: right-click one mouth with a data stick (it remembers the mouth), then the other. A mouth has one
 * partner; re-pairing breaks the old pair. Each mouth pays its own upkeep (a pair is 2 units, as in the T7 rig).</li>
 * </ul>
 */
public class MTEWormholeMouth extends MTEConduitElement<MTEWormholeMouth> implements IBurstDock {

    public static final String PIECE = "conduit_mouth";
    private static final IIconContainer[] OVERLAYS = overlays("OVERLAY_FRONT_WORMHOLE_MOUTH");
    private static final String STICK_TAG = "btsWormholeMouth";

    private static volatile IStructureDefinition<MTEWormholeMouth> STRUCTURE_DEFINITION;

    /** The partner mouth {dim, x, y, z}, or null. */
    private int[] partner;
    private String dockNote = "";
    private long dockTick = Long.MIN_VALUE / 2;

    public MTEWormholeMouth(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEWormholeMouth(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEWormholeMouth(mName);
    }

    @Override
    public String piece() {
        return PIECE;
    }

    @Override
    public String elementName() {
        return "Wormhole Mouth";
    }

    @Override
    public IStructureDefinition<MTEWormholeMouth> getStructureDefinition() {
        IStructureDefinition<MTEWormholeMouth> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEWormholeMouth.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    final StructureDefinition.Builder<MTEWormholeMouth> b = StructureDefinition
                        .<MTEWormholeMouth>builder()
                        .addShape(PIECE, BTSShapes.load(PIECE))
                        .addElement('A', casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing))
                        .addElement('E', casing(Casings.ReinforcedSpatialStructureCasing, x -> ++x.mCasing))
                        .addElement('C', block(SLBlocks.slCasing2, BlockSLCasing2.JET_COLLIMATOR, x -> ++x.mCasing))
                        .addElement('I', block(SLBlocks.slCasing2, BlockSLCasing2.RIM_GAS_INJECTOR, x -> ++x.mCasing))
                        .addElement('P', block(SLBlocks.slCasing, BlockSLCasing.CASIMIR, x -> ++x.mCasing));
                    def = commonSockets(b, MTEWormholeMouth.class).build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    // ============================================================================ geometry

    @Override
    protected boolean takesIn(int i) {
        return i == 0;
    }

    @Override
    protected boolean twoSided(int i) {
        return true;
    }

    /** The dock cell: the ring's centre on the front face (layer 0), where a quasar's port cell must be. */
    public int[] dockCell() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final int[] rel = throats()[0].centreRel.clone();
        rel[2] = 0;
        return QuasarGeometry.world(getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
    }

    /** The ring's axis out of the back (the direction a stream leaves the partner), world, unit. */
    public double[] axis() {
        return worldThroat(0).normal;
    }

    // ============================================================================ pairing

    /** The partner mouth if it is loaded, formed and paired back to this one; else null. */
    public MTEWormholeMouth partnerMouth() {
        final int[] p = partner;
        if (p == null) return null;
        final World world = DimensionManager.getWorld(p[0]);
        if (world == null || p[2] < 0 || p[2] > 255 || !world.blockExists(p[1], p[2], p[3])) return null;
        final TileEntity te = world.getTileEntity(p[1], p[2], p[3]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(mte instanceof MTEWormholeMouth)) return null;
        final MTEWormholeMouth other = (MTEWormholeMouth) mte;
        final int[] mine = position();
        return mine != null && java.util.Arrays.equals(other.partner, mine) ? other : null;
    }

    /** {dim, x, y, z} of this controller. */
    public int[] position() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return null;
        return new int[] { base.getWorld().provider.dimensionId, base.getXCoord(), base.getYCoord(), base.getZCoord() };
    }

    /** Pairs two mouths (each forgets its old partner). The bench and the data stick both come here. */
    public static void pair(MTEWormholeMouth a, MTEWormholeMouth b) {
        a.partner = b.position();
        b.partner = a.position();
    }

    /** Why the partner is not usable, or "" when it is. */
    public String partnerProblem() {
        if (partner == null) return "not paired (right-click with a data stick, then the other mouth)";
        final World world = DimensionManager.getWorld(partner[0]);
        if (world == null || !world.blockExists(partner[1], partner[2], partner[3])) {
            return "partner at " + label(partner) + " is not loaded";
        }
        final MTEWormholeMouth other = partnerMouth();
        if (other == null) return "partner at " + label(partner) + " is gone or paired elsewhere";
        if (!other.isFormed()) return "partner at " + label(partner) + " is not formed";
        if (!other.isOpen()) return "partner at " + label(partner) + " is closed (its upkeep)";
        return "";
    }

    static String label(int[] p) {
        return p[1] + ", " + p[2] + ", " + p[3] + " (dim " + p[0] + ")";
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        final ItemStack held = aPlayer.getCurrentEquippedItem();
        final ItemStack stick = ItemList.Tool_DataStick.get(1L);
        if (held != null && stick != null && GTUtility.areStacksEqual(held, stick, true)) {
            if (aBaseMetaTileEntity.isServerSide()) useDataStick(aPlayer, held);
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer, side, aX, aY, aZ);
    }

    private void useDataStick(EntityPlayer player, ItemStack stick) {
        final int[] mine = position();
        if (mine == null) return;
        final NBTTagCompound tag = stick.hasTagCompound() ? stick.getTagCompound() : new NBTTagCompound();
        final int[] other = tag.getIntArray(STICK_TAG);
        if (other.length == 4 && !java.util.Arrays.equals(other, mine)) {
            final World world = DimensionManager.getWorld(other[0]);
            MTEWormholeMouth o = null;
            if (world != null && world.blockExists(other[1], other[2], other[3])) {
                final TileEntity te = world.getTileEntity(other[1], other[2], other[3]);
                if (te instanceof IGregTechTileEntity
                    && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEWormholeMouth) {
                    o = (MTEWormholeMouth) ((IGregTechTileEntity) te).getMetaTileEntity();
                }
            }
            if (o == null) {
                tell(
                    player,
                    EnumChatFormatting.RED + "The data stick's mouth at "
                        + label(other)
                        + " is not there (or not loaded).");
                return;
            }
            pair(this, o);
            tag.removeTag(STICK_TAG);
            stick.setTagCompound(tag);
            tell(player, EnumChatFormatting.GREEN + "Wormhole paired: " + label(mine) + " <-> " + label(other));
            return;
        }
        tag.setIntArray(STICK_TAG, mine);
        stick.setTagCompound(tag);
        tell(
            player,
            EnumChatFormatting.AQUA + "Data stick holds this Wormhole Mouth ("
                + label(mine)
                + "). Right-click the other mouth to pair.");
    }

    private static void tell(EntityPlayer player, String text) {
        player.addChatComponentMessage(new ChatComponentText(text));
    }

    // ============================================================================ the stream

    @Override
    protected void route(BurstStream s, int in, long now) {
        final MTEWormholeMouth other = partnerMouth();
        if (other == null || !other.isFormed()) {
            book.add("lost|partner|" + s.key(), s.buPerTick, now);
            note("a stream arrived but the " + partnerProblem(), now);
            return;
        }
        other.relay(s);
    }

    /** The partner side: the stream leaves this mouth out of the back. Its own cap and upkeep apply. */
    void relay(BurstStream s) {
        final long now = now();
        final String key = conduitKey();
        if (s.crossed(key)) {
            note("a stream came back here (a cycle): dropped", now);
            return;
        }
        final String sk = s.key() + "|relay";
        book.add("in|relay|" + sk, s.buPerTick, now);
        if (!open || throatMk() == 0) {
            book.add("lost|" + sk, s.buPerTick, now);
            final MTEQuasarEngine q = s.source.resolve();
            if (q != null) q.noteConduitClosed(conduitLabel() + (throatMk() == 0 ? ": no Conduit Throat" : ": closed"));
            return;
        }
        final double used = book.sum("pass|", now, FlowBook.SECOND);
        final double passed = Math.max(0, Math.min(s.buPerTick, throatCap() - used));
        book.add("pass|" + sk, passed, now);
        if (s.buPerTick - passed > 1e-9) book.add("clip|" + sk, s.buPerTick - passed, now);
        emit(
            0,
            s.through(key)
                .withBu(passed),
            now);
    }

    // ============================================================================ the dock

    @Override
    protected void extraSecond(IGregTechTileEntity base, long now) {
        final int[] c = dockCell();
        BurstDocks.register(base.getWorld(), c[0], c[1], c[2], this);
    }

    @Override
    public boolean dockReady() {
        return mMachine && open && throatMk() > 0 && partnerMouth() != null;
    }

    /** A jet from a quasar's port: it must run into the front face along the ring's axis. */
    @Override
    public double acceptJet(JetPacket jet) {
        final long now = now();
        final MTEQuasarEngine q = jet.source.resolve();
        if (q == null) return 0;
        final double[] jetAxis = ConduitThroats
            .worldDir(q.getExtendedFacing(), toDouble(QuasarGeometry.axisAbc(jet.port)));
        if (ConduitNet.dot(jetAxis, axis()) < 0.999) {
            dockTick = now;
            dockNote = "misaligned on the " + jet.port.label.toLowerCase(java.util.Locale.ROOT)
                + " jet port: face the quasar (the jet must run into the front face)";
            return REFUSED;
        }
        dockTick = now;
        dockNote = "docked on the " + jet.port.label.toLowerCase(java.util.Locale.ROOT)
            + " jet port of the quasar at "
            + jet.source.label();
        return receive(BurstStream.of(jet), 0);
    }

    static double[] toDouble(int[] v) {
        return new double[] { v[0], v[1], v[2] };
    }

    @Override
    public String dockLabel() {
        final MTEWormholeMouth other = partnerMouth();
        return "Wormhole Mouth" + (other == null || other.position() == null ? "" : " to " + label(other.position()));
    }

    // ============================================================================ readouts

    @Override
    protected void extraStatus(List<String> out, long now) {
        final String problem = partnerProblem();
        out.add(problem.isEmpty() ? "Paired with the mouth at " + label(partner) : "Partner: " + problem);
        out.add(
            "In (the ring): " + MTEQuasarEngine.buEu(inBu(0))
                + (now - dockTick < 60 && !dockNote.isEmpty() ? "; " + dockNote : ""));
        routeLines(out, new int[] { 0 }, new String[] { "Out of the back (from the partner)" });
    }

    /** The partner's position, or null. */
    public int[] getPartner() {
        return partner == null ? null : partner.clone();
    }

    @Override
    protected void writeElement(NBTTagCompound t) {
        if (partner != null) t.setIntArray("partner", partner);
    }

    @Override
    protected void readElement(NBTTagCompound t) {
        final int[] p = t.getIntArray("partner");
        partner = p.length == 4 ? p : null;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] size = BTSShapes.size(PIECE);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Einstein-Rosen Conduit: Wormhole Mouth")
            .addInfo("Two mouths are one wormhole: a stream that enters one leaves the other, instantly and")
            .addInfo("  losslessly, at any distance and across dimensions (both must be chunk-loaded).")
            .addInfo("Pair: right-click one mouth with a Data Stick, then the other. One partner each.")
            .addInfo("A stream enters the ring through the front face (the controller's side) and leaves the")
            .addInfo("  partner out of its back, along the ring's axis.")
            .addInfo("On a Quasar Engine jet port: lay the ring on the cap, front face on the port (the cell just")
            .addInfo("  past the cap, on the jet axis), facing the quasar. It takes that jet; an unfit mouth vents.")
            .addSeparator();
        commonInfo(tt).addInfo("Automation: OpenComputers bts_mouth (getLink, getThroat, getUpkeep, isOpen).")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("Front face, below the ring")
            .addCasingInfoExactly("Rim Gas Injector", bom(PIECE, 'I'), false)
            .addOtherStructurePart("Energy Hatch (optional, unused)", "1 socket beside the controller", 3);
        commonStructure(tt, PIECE).toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        return elementTexture(side, aFacing, aActive, OVERLAYS);
    }
}
