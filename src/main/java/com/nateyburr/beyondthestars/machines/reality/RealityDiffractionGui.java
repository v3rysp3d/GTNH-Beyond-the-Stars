package com.nateyburr.beyondthestars.machines.reality;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;

/**
 * Reality Diffraction's screen on the controller GUI kit (controller-gui-spec.md §5.7). A recipe machine: GT's rows
 * stay. The header says RUNNING, WAITING (no BEC network or storage, no region anchored, or the last regions still
 * waiting to enter the network) or IDLE, with what it waits on. Under GT's rows: the BEC network as a check, one red
 * check per region that is not anchored (have / need, or "no hatch"), a green count of the anchored ones, the litres
 * waiting to enter the network, the last run. The gap row keeps the six region lamps. Every row is decided on the
 * server ({@link BTSRows}).
 */
public class RealityDiffractionGui extends BTSControllerGui<MTERealityDiffraction> {

    private static final String R = "beyondthestars.gui.reality.";

    public RealityDiffractionGui(MTERealityDiffraction multiblock) {
        super(multiblock, true, null);
    }

    // =============================================================================== the header

    /** What the machine waits on (a lang key under {@link #R}), or null when it waits on nothing of its own. */
    private String waitKey() {
        if (!multiblock.hasNetwork() || !multiblock.hasStorage()) return "wait.network";
        if (multiblock.pendingLitres() > 0) return "wait.full";
        if ((multiblock.anchorMask() & 0x3F) == 0) return "wait.anchors";
        return null;
    }

    /** NOT FORMED, RUNNING, WAITING (no network, no anchor, or full), IDLE. */
    @Override
    protected Badge badgeCode() {
        final Badge b = recipeBadge();
        if (b == Badge.NOT_FORMED || b == Badge.RUNNING) return b;
        return waitKey() != null ? Badge.WAITING : b;
    }

    @Override
    protected String subject() {
        if (!multiblock.mMachine) return "";
        if (multiblock.getBaseMetaTileEntity() != null && multiblock.getBaseMetaTileEntity()
            .isActive()) return BTSText.lang(R + "diffracting");
        final String w = waitKey();
        return w == null ? "" : BTSText.lang(R + w);
    }

    @Override
    protected String rightChip() {
        return "";
    }

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 12;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        final BTSChecklist net = new BTSChecklist();
        multiblock.addNetworkCheck(net, true);
        rows.checks(net);
        rows.checks(multiblock.anchorChecklist());
        final long pending = multiblock.pendingLitres();
        if (pending > 0) {
            rows.check(
                BTSChecklist.State.WARN,
                BTSText.lang(R + "chk.waiting"),
                BTSText.lit(String.format(java.util.Locale.ROOT, "%,d L", pending)),
                -1,
                BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.reality.full"));
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang(R + "startfield"));
        }
        final String note = multiblock.lastNote();
        if (!note.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", note));
        }
    }

    // ================================================================================= Info page

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        rows.para(BTSText.lang(R + "info.diffraction"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.anchors"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.network"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.diffraction.auto"), BTSGuiKit.VALUE);
    }

    // ================================================================================== gap row

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("diffMask", new IntSyncValue(multiblock::anchorMask));
    }

    /**
     * The six region lamps, I..VI: green = anchored for the next run, red = a hatch but not enough anchor, grey = no
     * hatch in that socket. Hover a lamp for its region.
     */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue mask = syncManager.findSyncHandler("diffMask", IntSyncValue.class);
        return HRGui.regionLamps(super.createLeftPanelGapRow(parent, syncManager), region -> {
            final int m = mask.getIntValue();
            if ((m & (1 << region)) != 0) return HRGui.LAMP_ON;
            return (m & (1 << (8 + region))) != 0 ? HRGui.LAMP_OFF : HRGui.LAMP_IDLE;
        }, "lamp.diffraction");
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }

    @Override
    protected boolean shouldDisplayBatchMode() {
        return false;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }
}
