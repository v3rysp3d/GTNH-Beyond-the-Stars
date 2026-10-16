package com.nateyburr.beyondthestars.machines.conduit;

import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * P4: the CONDUIT JUNCTION, MTE base+23 = 28123 (quasar-p4.md §3, §7; D-36). One building, two modes (GUI):
 * <ul>
 * <li><b>Split</b>: in through the big throat (throat 0); out 3 (throat 3, −60°) takes the fixed GUI share p3 off the
 * top, then out 1 (throat 1, straight on) and out 2 (throat 2, +60°) share the rest: share₁ = clamp(c + k·(s/15 −
 * 0.5), 0, 1) with the GUI ratio c, the gain k ∈ {0, 1, 2, 4} and s the redstone level on its Burst Control Hatch
 * ({@link BurstRouting#share}, {@link BurstRouting#split}). k = 0, or no hatch, ignores redstone. P4-88: an optional
 * out-1 limit (GUI, Bu/t): share₁ is taken of at most that much and the rest goes to out 2, so a loop on out 1 steers
 * the same few Bu/t however big the quasar grows (the standard rig's J1: {@link BurstRouting#STANDARD_J1_LIMIT}).</li>
 * <li><b>Combine</b>: up to 3 in through the small throats, the sum out of the big throat.</li>
 * </ul>
 * The whole stream through it is capped by its throat (one throat item for the element; F-5: one upkeep unit). The
 * first Burst Control Hatch found in the two {@code c} sockets drives s; the second socket is spare (P4-06).
 */
public class MTEConduitJunction extends MTEConduitElement<MTEConduitJunction> {

    public static final String PIECE = "conduit_junction";
    private static final IIconContainer[] OVERLAYS = overlays("OVERLAY_FRONT_CONDUIT_JUNCTION");

    private static volatile IStructureDefinition<MTEConduitJunction> STRUCTURE_DEFINITION;

    /** Split or combine. NBT keeps the name. */
    public enum Mode {
        SPLIT,
        COMBINE
    }

    // ---- persisted
    private Mode mode = Mode.SPLIT;
    private double ratio = 0.5;
    private int gainIndex;
    private double out3Share;
    /** P4-88: out 1's limit, Bu/t (0 = none). */
    private double limit;

    // ---- run
    private int lastLevel = -1;
    private double lastShare = 0.5;

    public MTEConduitJunction(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEConduitJunction(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEConduitJunction(mName);
    }

    @Override
    public String piece() {
        return PIECE;
    }

    @Override
    public String elementName() {
        return "Conduit Junction";
    }

    @Override
    public IStructureDefinition<MTEConduitJunction> getStructureDefinition() {
        IStructureDefinition<MTEConduitJunction> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEConduitJunction.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    final StructureDefinition.Builder<MTEConduitJunction> b = StructureDefinition
                        .<MTEConduitJunction>builder()
                        .addShape(PIECE, BTSShapes.load(PIECE))
                        .addElement('A', casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing))
                        .addElement('E', casing(Casings.ReinforcedSpatialStructureCasing, x -> ++x.mCasing))
                        .addElement('C', block(SLBlocks.slCasing2, BlockSLCasing2.JET_COLLIMATOR, x -> ++x.mCasing))
                        .addElement('G', casing(Casings.QuantumGlass, x -> ++x.mCasing))
                        .addElement('P', block(SLBlocks.slCasing, BlockSLCasing.CASIMIR, x -> ++x.mCasing))
                        // The Burst Control sockets are OPTIONAL (with none the split holds its GUI ratio): plain
                        // casing unless the projector sets the bts_optional channel (BTSStructures.optionalHatch,
                        // B-229)
                        .addElement(
                            'c',
                            BTSStructures.optionalHatch(
                                GTStructureUtility.buildHatchAdder(MTEConduitJunction.class)
                                    .atLeast(ConduitHatchElement.BurstControl)
                                    .casingIndex(Casings.ReinforcedSpatialStructureCasing.textureId)
                                    .hint(2),
                                casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing)));
                    def = commonSockets(b, MTEConduitJunction.class).build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    // ============================================================================ the stream

    @Override
    protected boolean takesIn(int i) {
        return mode == Mode.SPLIT ? i == 0 : i >= 1 && i <= 3;
    }

    @Override
    protected void route(BurstStream s, int in, long now) {
        if (mode == Mode.COMBINE) {
            emit(0, s, now);
            return;
        }
        // QA-B06: the out-1 limit is the Junction's, for all the streams of a second together (the standard T7 rig
        // brings both jets into J1): each stream is split against what the earlier ones left of it
        final double[] out;
        if (limit > 0) {
            final double used = book.sum("lim|", now, FlowBook.SECOND);
            out = BurstRouting.splitWithin(s.buPerTick, share(), out3Share, limit - used);
            book.add("lim|" + s.key(), out[3], now);
        } else out = BurstRouting.split(s.buPerTick, share(), out3Share);
        emit(
            1,
            s.withBu(out[0])
                .withLimit(limit),
            now);
        emit(2, s.withBu(out[1]), now);
        if (out3Share > 0) emit(3, s.withBu(out[2]), now);
    }

    /** share₁ now: c alone with no hatch (or k = 0), else c + k·(s/15 − 0.5) on the hatch's level. */
    public double share() {
        final double k = BurstRouting.GAINS[gainIndex];
        if (lastLevel < 0 || k == 0) return Math.max(0, Math.min(1, ratio));
        return BurstRouting.share(ratio, k, lastLevel);
    }

    /** Once a second: the first Burst Control Hatch (SPLIT mode) gives s; the rest are spare. */
    @Override
    protected void extraSecond(IGregTechTileEntity base, long now) {
        int level = -1;
        for (MTEHatchBurstControl h : burstHatches) {
            if (!live(h)) continue;
            if (level < 0) {
                h.poll(
                    "Conduit Junction",
                    String.format(
                        Locale.ROOT,
                        "share 1 = %.2f",
                        BurstRouting.share(ratio, BurstRouting.GAINS[gainIndex], h.level())),
                    MTEHatchBurstControl.Mode.SPLIT);
                h.consumeRisingEdge();
                level = h.level();
            } else {
                h.poll("Conduit Junction", "spare socket: not read", MTEHatchBurstControl.Mode.SPLIT);
                h.consumeRisingEdge();
            }
        }
        lastLevel = level;
        lastShare = share();
    }

    // ============================================================================ settings (GUI, OC)

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode m) {
        if (m != null && m != mode) {
            mode = m;
            book.clear();
        }
    }

    public int getModeOrdinal() {
        return mode.ordinal();
    }

    public void setModeOrdinal(int o) {
        setMode(Mode.values()[Math.floorMod(o, Mode.values().length)]);
    }

    public double getRatio() {
        return ratio;
    }

    /** c (0..1); a non-finite value (Lua's 0/0, a bad client packet) is ignored. */
    public void setRatio(double c) {
        if (!Double.isFinite(c)) return;
        ratio = Math.max(0, Math.min(1, c));
    }

    public int getGainIndex() {
        return gainIndex;
    }

    public void setGainIndex(int i) {
        gainIndex = Math.floorMod(i, BurstRouting.GAINS.length);
    }

    /** Sets k by value (0, 1, 2 or 4; anything else snaps to the nearest). */
    public double setGain(double k) {
        int best = 0;
        for (int i = 1; i < BurstRouting.GAINS.length; i++) {
            if (Math.abs(BurstRouting.GAINS[i] - k) < Math.abs(BurstRouting.GAINS[best] - k)) best = i;
        }
        gainIndex = best;
        return BurstRouting.GAINS[best];
    }

    public double getGain() {
        return BurstRouting.GAINS[gainIndex];
    }

    public double getOut3Share() {
        return out3Share;
    }

    /** p3 (0..1); a non-finite value is ignored. */
    public void setOut3Share(double p) {
        if (!Double.isFinite(p)) return;
        out3Share = Math.max(0, Math.min(1, p));
    }

    /** P4-88: out 1's limit, Bu/t (0 = none). */
    public double getLimit() {
        return limit;
    }

    /**
     * P4-88: out 1's limit in Bu/t, 0 (or less) for none, at most Mk IV's throat cap; a non-finite value is ignored.
     */
    public void setLimit(double l) {
        if (!Double.isFinite(l)) return;
        limit = Math.max(0, Math.min(BurstRouting.throatCap(BurstRouting.MK_MAX), l));
    }

    public int getLastLevel() {
        return lastLevel;
    }

    public double getLastShare() {
        return lastShare;
    }

    // ============================================================================ readouts

    @Override
    protected void extraStatus(List<String> out, long now) {
        if (mode == Mode.SPLIT) {
            out.add(
                String.format(
                    Locale.ROOT,
                    "SPLIT: in %s; c %.2f, k %s, s %s -> share 1 = %.2f; out 3 takes %.0f%% first; out 1 limit %s",
                    MTEQuasarEngine.buEu(inBu(0)),
                    ratio,
                    fmtGain(getGain()),
                    lastLevel < 0 ? "- (no Burst Control Hatch)" : String.valueOf(lastLevel),
                    share(),
                    100 * out3Share,
                    limit > 0 ? MTEQuasarEngine.buEu(limit) + " (share 1 is of at most this; the rest to out 2)"
                        : "none"));
            routeLines(out, new int[] { 1, 2, 3 }, new String[] { "Out 1 (straight)", "Out 2 (+60)", "Out 3 (-60)" });
        } else {
            out.add(
                "COMBINE: in " + bu(
                    inBu(1)) + " + " + bu(inBu(2)) + " + " + MTEQuasarEngine.buEu(inBu(3)) + ", capped by the throat");
            routeLines(out, new int[] { 0 }, new String[] { "Out (big throat)" });
        }
    }

    static String fmtGain(double k) {
        return k == Math.rint(k) ? String.valueOf((int) k) : String.valueOf(k);
    }

    @Override
    protected void writeElement(NBTTagCompound t) {
        t.setString("mode", mode.name());
        t.setDouble("ratio", ratio);
        t.setInteger("gain", gainIndex);
        t.setDouble("out3", out3Share);
        t.setDouble("limit", limit);
    }

    @Override
    protected void readElement(NBTTagCompound t) {
        try {
            mode = t.hasKey("mode") ? Mode.valueOf(t.getString("mode")) : Mode.SPLIT;
        } catch (IllegalArgumentException bad) {
            mode = Mode.SPLIT;
        }
        ratio = 0.5;
        if (t.hasKey("ratio")) setRatio(t.getDouble("ratio"));
        setGainIndex(t.getInteger("gain"));
        out3Share = 0;
        setOut3Share(t.getDouble("out3"));
        limit = 0;
        setLimit(t.getDouble("limit"));
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] size = BTSShapes.size(PIECE);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Einstein-Rosen Conduit: Junction")
            .addInfo("Splits one burst stream in three, or combines up to three.")
            .addInfo("Split: in at the big throat. Out 3 takes a fixed share off the top;")
            .addInfo("outs 1 and 2 share the rest, set in the GUI or by redstone.")
            .addInfo("Out 1 limit (GUI): out 1 never takes more, the rest goes to out 2.")
            .addInfo("Combine: up to three streams in at the small throats,")
            .addInfo("the sum out of the big one.")
            .addSeparator();
        commonInfo(tt).addInfo("Automation: a " + BTSTip.term("Burst Control Hatch") + " steers the split.")
            .addInfo(BTSTip.hint("Rigs for the Eye: see the questbook. OpenComputers: bts_junction."))
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("Top centre")
            .addCasingInfoExactly("Quantum Glass", bom(PIECE, 'G'), false)
            .addMiscHatch("0-2", "Burst Control Hatch", "beside the controller (the first is read)", 2)
            .addEnergyHatch("0-1", "Beside the controller (optional, unused)", 3);
        commonStructure(tt, PIECE).toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR);
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        return elementTexture(side, aFacing, aActive, OVERLAYS);
    }
}
