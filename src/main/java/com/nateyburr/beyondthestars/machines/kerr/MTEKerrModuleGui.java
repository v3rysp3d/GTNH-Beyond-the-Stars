package com.nateyburr.beyondthestars.machines.kerr;

import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;

/**
 * A Kerr module's screen on the controller GUI kit (controller-gui-spec.md §5.3). It is a recipe machine: GT's rows
 * (progress, outputs, EU, the recipe result) and GT's gap-row buttons (void excess, input separation, batch mode, the
 * power panel) stay. The family header says the link: RUNNING / IDLE while linked to a KEE with an open hole, PAUSED
 * while the hole is closed, NOT LINKED otherwise (NOT FORMED first, while the arc itself is not formed); the right
 * chip is the hole's parallel. Under GT's rows a linked module shows the hole bonus (parallel and speed), the KEE's
 * cell and the hole; an unlinked or paused one shows what it runs again on, every link condition met or not. The
 * compressor's DENSITY gate is a note. What the module runs and how the hole sets its bonus are on the <b>i</b> page.
 * Every row is decided on the server ({@link BTSRows}); the link is the module's own server state.
 */
public class MTEKerrModuleGui extends BTSControllerGui<MTEKerrModuleBase<?>> {

    private static final String M = "beyondthestars.gui.module.";

    public MTEKerrModuleGui(MTEKerrModuleBase<?> multiblock) {
        super(multiblock, true, null);
    }

    // =============================================================================== the header

    /** §5.3 badge table (NOT FORMED first, as every family machine). */
    @Override
    protected Badge badgeCode() {
        if (!multiblock.mMachine) return Badge.NOT_FORMED;
        switch (multiblock.getLink()) {
            case LINKED:
                return multiblock.getBaseMetaTileEntity()
                    .isActive() ? Badge.RUNNING : Badge.IDLE;
            case HOLE_CLOSED:
                return Badge.PAUSED;
            default:
                return Badge.NOT_LINKED;
        }
    }

    @Override
    protected String subject() {
        switch (multiblock.getLink()) {
            case LINKED:
                return BTSText.lang(M + "linked");
            case HOLE_CLOSED:
                return BTSText.lang(M + "holeclosed");
            default:
                return BTSText.lang(M + "notlinked");
        }
    }

    @Override
    protected String rightChip() {
        return multiblock.getLink() == MTEKerrModuleBase.Link.LINKED ? BTSText.lit("x" + multiblock.bonusParallel())
            : "";
    }

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 12;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        final MTEKerrModuleBase.Link link = multiblock.getLink();
        if (!multiblock.mMachine) {
            rows.note(
                BTSRows.ICON_BAD,
                BTSGuiKit.BAD,
                BTSText.lang(M + "notformed", multiblock.slot().hintDots),
                BTSText.lang(M + "notformed.tip"));
        }
        if (link == MTEKerrModuleBase.Link.LINKED) {
            linkedRows(rows);
        } else {
            pausedRows(rows, link);
        }
        gateRow(rows);
    }

    private void linkedRows(BTSRows.Builder rows) {
        rows.section(BTSText.lang(M + "bonus"), "");
        rows.stat2(
            BTSText.lang("beyondthestars.gui.kit.parallel"),
            BTSText.lit("§b" + multiblock.bonusParallel()),
            BTSText.lang(M + "speed"),
            BTSText.lit("§bx" + BTSFormat.f2(multiblock.bonusSpeed())),
            BTSText.lang(M + "bonus.tip"));
        rows.stat(BTSText.lang(M + "kee"), BTSText.lit(multiblock.keePosText()), BTSText.lang(M + "kee.tip"));
        rows.stat(
            BTSText.lang(M + "hole"),
            BTSText.cat(
                BTSText.lit("§a"),
                BTSText.lang("beyondthestars.gui.badge.open"),
                BTSText.lit(" " + BTSFormat.fmt1(multiblock.linkedMass()) + " SM")));
    }

    /** §5.3 otherwise: the link conditions, each met or not, read from the module's last link check. */
    private void pausedRows(BTSRows.Builder rows, MTEKerrModuleBase.Link link) {
        rows.section(BTSText.lang(M + "sec"), "");
        final String why = BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.kerr.module." + link.key);
        final String pos = multiblock.keePosText();
        final boolean found = link != MTEKerrModuleBase.Link.NO_KEE && link != MTEKerrModuleBase.Link.WRONG_FACING;
        final BTSChecklist c = new BTSChecklist();
        c.add(
            found,
            BTSText.lang(M + "chk.found"),
            found ? BTSText.lit(pos)
                : BTSText
                    .lang(link == MTEKerrModuleBase.Link.WRONG_FACING ? M + "chk.found.facing" : M + "chk.found.none"),
            -1,
            found ? BTSText.lang(M + "chk.found.tip", pos) : why);
        final boolean ready = link == MTEKerrModuleBase.Link.HOLE_CLOSED;
        c.add(
            ready,
            BTSText.lang(M + "chk.loaded"),
            ready ? ""
                : link == MTEKerrModuleBase.Link.KEE_NOT_FORMED ? BTSText.lang("beyondthestars.gui.badge.notformed")
                    : link == MTEKerrModuleBase.Link.KEE_NOT_LOADED ? BTSText.lang(M + "chk.notloaded")
                        : BTSText.lit("-"),
            -1,
            ready ? "" : why);
        c.add(
            false,
            BTSText.lang(M + "chk.open"),
            ready ? BTSText.cat(BTSText.lit("§c"), BTSText.lang("beyondthestars.gui.badge.closed")) : BTSText.lit("-"),
            -1,
            BTSText.lang(M + "chk.open.tip"));
        rows.checks(c);
        rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang(M + "paused"));
        if (multiblock.mMaxProgresstime > 0) rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang(M + "kept"));
    }

    /** The compressor's DENSITY gate (the scanner's line): aqua while open, amber while shut. */
    private void gateRow(BTSRows.Builder rows) {
        final String gate = multiblock.gateLine();
        if (gate.isEmpty()) return;
        rows.note(
            BTSRows.ICON_NONE,
            multiblock.recipeGateOpen() ? BTSGuiKit.AQUA : BTSGuiKit.WARN,
            BTSText.lit(gate.replaceAll("§.", "")),
            BTSText.lang("GT5U.gui.text.recipe_result.beyondthestars.kerr.module.density"));
    }

    // ================================================================================= Info page

    /** What the module runs (the maps line, §5.3), the link, the bonus, where it stands. */
    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        final KerrSlot slot = multiblock.slot();
        rows.para(BTSText.lang(M + "info.maps." + slot.key), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(M + "info.link"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(M + "info.bonus"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(M + "info.place", slot.hintDots), BTSGuiKit.VALUE);
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }
}
