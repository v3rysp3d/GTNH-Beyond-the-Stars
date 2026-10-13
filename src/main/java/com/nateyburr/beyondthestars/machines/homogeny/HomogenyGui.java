package com.nateyburr.beyondthestars.machines.homogeny;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * The Homogeny's screen on the controller GUI kit (controller-gui-spec.md §5.6). A recipe machine: GT's rows (progress,
 * outputs, the recipe result) and GT's gap-row buttons stay. The header says the state (NOT FORMED, INSTALLING,
 * RUNNING, WAITING, IDLE), "Mixing" or the install step, and the tier with its parallels. Under GT's rows: the three
 * rings as lamps (hover one for its state and its fault), then either the next tier's three conditions as a checklist
 * or, while a core installs, the install bar, the time left and why it is paused. The gap row's Install button greys
 * out until every condition holds. Every row is decided on the server ({@link BTSRows}); Install goes back through
 * {@link MTEHomogeny#beginInstall()}, the one install path.
 */
public class HomogenyGui extends BTSControllerGui<MTEHomogeny> {

    private static final String H = "beyondthestars.gui.homogeny.";
    private static final String[] LAMP_KEYS = { "lamp.core", "lamp.stir", "lamp.outer" };
    private static final String[] RING_KEYS = { "ring.core", "ring.stir", "ring.outer" };

    public HomogenyGui(MTEHomogeny multiblock) {
        super(multiblock, true, null);
    }

    // =============================================================================== the header

    /** §5.6 badge table, first match wins. */
    @Override
    protected Badge badgeCode() {
        if (!multiblock.mMachine) return Badge.NOT_FORMED;
        if (multiblock.isInstalling()) return Badge.INSTALLING;
        return recipeBadge();
    }

    @Override
    protected String subject() {
        if (multiblock.isInstalling()) {
            return BTSText.lit("T" + multiblock.installedTier() + " > T" + (multiblock.installedTier() + 1));
        }
        return multiblock.mMachine ? BTSText.lang(H + "mixing") : "";
    }

    @Override
    protected String rightChip() {
        if (!multiblock.mMachine) return BTSText.lit("T" + multiblock.installedTier());
        return BTSText.lang(H + "right", multiblock.tier(), multiblock.maxParallel());
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        out.add(
            BTSText.lang(
                H + "right.tip",
                multiblock.tier(),
                multiblock.installedTier(),
                multiblock.structureTier(),
                multiblock.maxParallel()));
        out.add(BTSText.lang(H + "right.tip2"));
        return out;
    }

    // ============================================================================= status rows

    @Override
    protected int statusPoolSize() {
        return 10;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        rings(rows);
        // P4: the quasar link (only with a GRB Receptor Port fitted)
        final String burst = multiblock.burstLine();
        if (!burst.isEmpty()) {
            rows.note(
                BTSRows.ICON_NONE,
                BTSGuiKit.AQUA,
                BTSText.lit(burst),
                BTSText.lit(burst),
                BTSText.lang("beyondthestars.gui.kit.quasarlink.tip"));
        }
        if (multiblock.isInstalling()) {
            installRows(rows);
        } else if (multiblock.installedTier() < MTEHomogeny.MAX_TIER) {
            final BTSChecklist c = multiblock.upgradeChecklist();
            rows.section(BTSText.lang(H + "sec", multiblock.installedTier() + 1), c.countText());
            rows.checks(c);
        }
        final String note = multiblock.lastNote();
        if (!note.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", note));
        }
    }

    /**
     * Row 1: Core / Stir / Outer. Green while the piece stands; red while the installed tier needs it and it does not
     * stand (the tooltip carries its first structure error); grey while this tier does not need it yet (the upgrade
     * checklist says what the next tier needs).
     */
    private void rings(BTSRows.Builder rows) {
        final List<String[]> lamps = new ArrayList<>();
        for (int i = 0; i < MTEHomogeny.MAX_TIER; i++) {
            final boolean stands = i == 0 ? multiblock.mMachine : multiblock.pieceStands(i);
            final boolean needed = i + 1 <= multiblock.installedTier();
            final int state = stands ? 1 : needed ? 2 : 0;
            final List<String> tip = new ArrayList<>();
            tip.add(
                BTSText.lang(
                    H + "lamp.tip",
                    BTSText.key(H + RING_KEYS[i]),
                    i + 1,
                    BTSText.key(H + (stands ? "lamp.formed" : needed ? "lamp.missing" : "lamp.notneeded"))));
            final String fault = multiblock.pieceFault(i);
            if (!stands && !fault.isEmpty()) tip.add(BTSText.lit("§7" + fault));
            lamps.add(
                new String[] { String.valueOf(state), BTSText.lang(H + LAMP_KEYS[i]), BTSRows.Builder.tooltip(tip) });
        }
        rows.lamps(BTSText.lang(H + "rings"), lamps);
    }

    /** While a core installs: the bar, the time left, why it is paused, how to cancel. */
    private void installRows(BTSRows.Builder rows) {
        final double f = Math.max(0, multiblock.installFraction());
        rows.bar(
            BTSText.lang(H + "install"),
            f,
            BTSText.lit(String.format(java.util.Locale.ROOT, "%.1f%%", f * 100)),
            BTSGuiKit.Grad.MAGENTA,
            "",
            BTSText.lang(
                H + "install.bar.tip",
                multiblock.installedTier() + 1,
                BTSFormat.exact(MTEHomogeny.INSTALL_TICKS / 20),
                BTSFormat
                    .exact(MTEHomogeny.INSTALL_EUT[Math.min(MTEHomogeny.MAX_TIER, multiblock.installedTier() + 1)])));
        rows.stat(
            BTSText.lang(H + "timeleft"),
            BTSText.lit(BTSFormat.exact(multiblock.installTicksLeft() / 20) + " s"));
        final String stall = multiblock.installStall();
        if (!stall.isEmpty()) {
            rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lang(H + "paused", stall));
        }
        rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang(H + "cancel"));
    }

    // ================================================================================= Info page

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        rows.para(BTSText.lang(H + "info.mix"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(H + "info.tiers"), BTSGuiKit.VALUE);
        rows.para(BTSText.lang(H + "info.upgrade", BTSFormat.exact(MTEHomogeny.MILESTONE_LITRES)), BTSGuiKit.VALUE);
        rows.para(
            BTSText.lang(
                H + "info.install",
                MTEHomogeny.INSTALL_TICKS / 20 / 60,
                BTSFormat.euT(MTEHomogeny.INSTALL_EUT[2]),
                BTSFormat.euT(MTEHomogeny.INSTALL_EUT[3])),
            BTSGuiKit.VALUE);
        rows.para(BTSText.lang(H + "info.auto"), BTSGuiKit.VALUE);
    }

    // ================================================================================== gap row

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        // Install is a server-side action (GT's InteractionSyncHandler pattern); this flag only greys the button.
        syncManager.syncValue(
            "homInstallReady",
            new BooleanSyncValue(
                () -> multiblock.isInstalling() || multiblock.installBlocker()
                    .isEmpty()));
    }

    /** GT's left buttons, then Install: greyed (BUTTON_STANDARD_DISABLED) until every condition holds. */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final BooleanSyncValue ready = syncManager.findSyncHandler("homInstallReady", BooleanSyncValue.class);
        final InteractionSyncHandler install = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.beginInstall(); });
        final IDrawable face = (context, x, y, width, height,
            theme) -> (ready != null && ready.getBoolValue() ? GTGuiTextures.BUTTON_STANDARD
                : GTGuiTextures.BUTTON_STANDARD_DISABLED).draw(context, x, y, width, height, theme);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(2)
            .child(
                new ButtonWidget<>().size(40, 16)
                    .syncHandler(install)
                    .background(face)
                    .hoverBackground(face)
                    .overlay(
                        IKey.dynamic(
                            () -> (ready != null && ready.getBoolValue() ? "" : EnumChatFormatting.DARK_GRAY.toString())
                                + StatCollector.translateToLocal(H + "install")))
                    .tooltip(
                        t -> t.addLine(IKey.lang(H + "install.tip"))
                            .addLine(EnumChatFormatting.GRAY + StatCollector.translateToLocal(H + "install.tip1"))
                            .addLine(EnumChatFormatting.GRAY + StatCollector.translateToLocal(H + "install.tip2"))
                            .addLine(EnumChatFormatting.GRAY + StatCollector.translateToLocal(H + "install.tip3")))
                    .tooltipShowUpTimer(TOOLTIP_DELAY));
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }
}
