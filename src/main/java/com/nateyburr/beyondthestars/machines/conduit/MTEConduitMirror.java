package com.nateyburr.beyondthestars.machines.conduit;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * P4: the CONDUIT MIRROR, MTE base+22 = 28122 (quasar-p4.md §3, §7; D-36, Q-6). It turns a stream through 90°: v1 is
 * the
 * drawn corner, and the controller's facing (any of the 24 orientations) chooses the turning plane, so one mirror makes
 * every axis-to-axis turn. Reversible: a stream into either throat leaves by the other (P4-49). Throat 0 is the drawn
 * "in" (the front face), throat 1 the "out" (the side).
 */
public class MTEConduitMirror extends MTEConduitElement<MTEConduitMirror> {

    public static final String PIECE = "conduit_mirror";
    private static final IIconContainer[] OVERLAYS = overlays("OVERLAY_FRONT_CONDUIT_MIRROR");

    private static volatile IStructureDefinition<MTEConduitMirror> STRUCTURE_DEFINITION;

    public MTEConduitMirror(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEConduitMirror(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEConduitMirror(mName);
    }

    @Override
    public String piece() {
        return PIECE;
    }

    @Override
    public String elementName() {
        return "Conduit Mirror";
    }

    @Override
    public IStructureDefinition<MTEConduitMirror> getStructureDefinition() {
        IStructureDefinition<MTEConduitMirror> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEConduitMirror.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    final StructureDefinition.Builder<MTEConduitMirror> b = StructureDefinition
                        .<MTEConduitMirror>builder()
                        .addShape(PIECE, BTSShapes.load(PIECE))
                        .addElement('A', casing(Casings.ReinforcedTemporalStructureCasing, x -> ++x.mCasing))
                        .addElement('E', casing(Casings.ReinforcedSpatialStructureCasing, x -> ++x.mCasing))
                        .addElement('C', block(SLBlocks.slCasing2, BlockSLCasing2.JET_COLLIMATOR, x -> ++x.mCasing))
                        .addElement('M', block(SLBlocks.slCasing, BlockSLCasing.MIRROR, x -> ++x.mCasing))
                        .addElement('P', block(SLBlocks.slCasing, BlockSLCasing.CASIMIR, x -> ++x.mCasing));
                    def = commonSockets(b, MTEConduitMirror.class).build();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    @Override
    protected boolean takesIn(int i) {
        return i == 0 || i == 1;
    }

    @Override
    protected void route(BurstStream s, int in, long now) {
        emit(1 - in, s, now);
    }

    @Override
    protected void extraStatus(List<String> out, long now) {
        out.add("In (front): " + bu(inBu(0)) + " Bu/t, in (side): " + bu(inBu(1)) + " Bu/t");
        routeLines(out, new int[] { 1, 0 }, new String[] { "Out (side)", "Out (front)" });
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] size = BTSShapes.size(PIECE);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Einstein-Rosen Conduit: Mirror")
            .addInfo("Turns a gamma-ray burst stream through 90 degrees: in through the front throat, out of the")
            .addInfo("  side throat (or the other way round). Its facing picks the plane: any turn between axes.")
            .addSeparator();
        commonInfo(tt).addInfo("Automation: OpenComputers bts_mirror (getLink, getThroat, getUpkeep, isOpen).")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("Front, beside the in-throat")
            .addCasingInfoExactly("Superradiant Mirror Casing", bom(PIECE, 'M'), false);
        commonStructure(tt, PIECE).toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        return elementTexture(side, aFacing, aActive, OVERLAYS);
    }
}
