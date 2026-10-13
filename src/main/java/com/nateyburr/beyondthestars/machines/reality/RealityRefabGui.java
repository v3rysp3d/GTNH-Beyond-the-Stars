package com.nateyburr.beyondthestars.machines.reality;

import java.util.Locale;

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
 * Reality Refab's screen on the controller GUI kit (controller-gui-spec.md §5.7). A recipe machine: GT's rows stay. The
 * header says RUNNING, WAITING or IDLE and, while a craft runs, its step. Under GT's rows: the BEC network and the
 * linked Condensate Assembler as checks, the step being worked (its nanite tier, the contaminants), how much of the
 * regions is drawn (a bar), why it waits (a red check), the last craft. The gap row keeps the six region lamps. Every
 * row is decided on the server ({@link BTSRows}).
 */
public class RealityRefabGui extends BTSControllerGui<MTERealityRefab> {

    private static final String R = "beyondthestars.gui.reality.";

    public RealityRefabGui(MTERealityRefab multiblock) {
        super(multiblock, true, null);
    }

    // =============================================================================== the header

    /** NOT FORMED, RUNNING (a craft advancing), WAITING (a craft that waits, or a failed check), IDLE. */
    @Override
    protected Badge badgeCode() {
        final Badge b = recipeBadge();
        if (b == Badge.NOT_FORMED) return b;
        if (!multiblock.waitLine()
            .isEmpty()) return Badge.WAITING;
        return b;
    }

    @Override
    protected String subject() {
        final int s = multiblock.stepIndex();
        return s < 0 ? "" : BTSText.lang(R + "step", s + 1, multiblock.stepCount());
    }

    @Override
    protected String rightChip() {
        return "";
    }

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 10;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        rows.checks(multiblock.linkChecklist());
        final int s = multiblock.stepIndex();
        if (s >= 0) {
            rows.stat(
                BTSText.lang(R + "step", s + 1, multiblock.stepCount()),
                BTSText.lang(R + "step.v", multiblock.stepTier(), multiblock.contaminants()),
                BTSText.lang(R + "step.tip"));
        }
        final double regions = multiblock.regionFraction();
        if (regions >= 0) {
            rows.bar(
                BTSText.lang(R + "regions"),
                regions,
                BTSText.lit(String.format(Locale.ROOT, "%.1f%%", regions * 100)),
                BTSGuiKit.Grad.MAGENTA,
                "",
                BTSText.lang(R + "regions.tip"));
        }
        final String wait = multiblock.waitLine();
        if (!wait.isEmpty()) {
            rows.check(BTSChecklist.State.BAD, BTSText.lit(wait), "", -1, BTSText.lang(R + "waiting", wait));
        }
        final String note = multiblock.lastNote();
        if (!note.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", note));
        }
    }

    // ================================================================================= Info page

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        rows.para(BTSText.lang(R + "info.refab"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.nanites", MTERealityRefab.LINK_RANGE), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.steps"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.lockstep"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(R + "info.refab.auto"), BTSGuiKit.VALUE);
    }

    // ================================================================================== gap row

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("refMask", new IntSyncValue(multiblock::regionMask));
    }

    /** The six region lamps: green = this Refab's BEC network holds that region now, red = it does not. */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IntSyncValue mask = syncManager.findSyncHandler("refMask", IntSyncValue.class);
        return HRGui.regionLamps(
            super.createLeftPanelGapRow(parent, syncManager),
            region -> (mask.getIntValue() & (1 << region)) != 0 ? HRGui.LAMP_ON : HRGui.LAMP_OFF,
            "lamp.refab");
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }
}
