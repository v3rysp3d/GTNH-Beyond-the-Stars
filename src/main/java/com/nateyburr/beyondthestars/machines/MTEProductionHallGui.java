package com.nateyburr.beyondthestars.machines;

import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;

/**
 * The controller screen of the four production halls (PUREX Canyon, Fuel Fabrication Hall, Cascade Hall, Site Batching
 * Plant) on the controller GUI kit (controller-gui-spec.md §5.9): the family header only. GT's rows (progress, outputs,
 * EU, the recipe result) and GT's gap-row buttons and power panel stay; the header says RUNNING / WAITING / IDLE / NOT
 * FORMED and the parallel ("x16"); under GT's rows one stat says the parallel and one dim note what sets it (the
 * shell's tier, or the Cascade Hall's stage banks), the number that makes each of these machines worth building.
 * <p>
 * <b>AXIS 1, CLIENT vs SERVER.</b> The parallel, the shell tier and the stage banks are written by
 * {@code checkMachine}, SERVER-side only. The kit builds every row on the server ({@link BTSRows}), so they are read
 * there, through {@link MTEProductionHallBase#parallelSource}, which reads only its argument.
 */
public class MTEProductionHallGui extends BTSControllerGui<MTEProductionHallBase<?>> {

    private static final String P = "beyondthestars.gui.hall.";

    public MTEProductionHallGui(MTEProductionHallBase<?> multiblock) {
        super(multiblock, true, null);
    }

    @Override
    protected Badge badgeCode() {
        return recipeBadge();
    }

    /** "x16": the parallel GT runs (TQ-8: the power panel's limit applies). */
    @Override
    protected String subject() {
        return multiblock.mMachine ? BTSText.lit("x" + Math.max(1, multiblock.getTrueParallel())) : "";
    }

    @Override
    protected String rightChip() {
        return "";
    }

    @Override
    protected int statusPoolSize() {
        return 4;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        if (!multiblock.mMachine) return;
        rows.stat(
            BTSText.lang("beyondthestars.gui.kit.parallel"),
            BTSText.lit(String.format(java.util.Locale.ROOT, "§e%,d", Math.max(1, multiblock.getTrueParallel()))),
            BTSText.lang(P + "parallel.tip"));
        final String source = multiblock.parallelSource(multiblock.getParallelDetail());
        if (source != null && !source.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang(P + "source", source));
        }
    }

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        rows.para(BTSText.lang(P + "info.parallel"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(P + "info.run"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(P + "info.auto"), BTSGuiKit.VALUE);
    }
}
