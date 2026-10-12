package com.nateyburr.beyondthestars.machines;

import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.nateyburr.beyondthestars.progression.BTSMilestone;
import com.nateyburr.beyondthestars.progression.BTSProgression;
import com.nateyburr.beyondthestars.progression.BTSUpgrade;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.widget.RotatedDrawable;

/**
 * D-24 UPGRADE TREE, the panel: a real scrolling NODE GRAPH - twenty-five buttons at absolute
 * pixel positions with rotated connector lines drawn between them - not a list.
 * <p>
 * <b>WHY THIS AND NOT SOMETHING ELSE.</b> The recon's verdict was that the Godforge's approach
 * IS reachable from an addon, and it is: {@code ScrollWidget}, {@code VerticalScrollData},
 * {@code ButtonWidget}, {@code DynamicDrawable}, GT's {@code RotatedDrawable} and the whole
 * {@code PICTURE_UPGRADE_CONNECTOR_*} / {@code BUTTON_SPACE_*} texture set are all public in
 * the exact GT 5.09.52.594 + ModularUI2 2.3.70 pair this addon pins, verified with {@code javap}
 * against the jars in the Gradle cache. What is NOT reachable is the Godforge's
 * {@code SyncHypervisor} plumbing - every method on it is typed to {@code MTEForgeOfGods} and
 * {@code ForgeOfGodsData} - and we do not need one: it exists only because five Godforge MTEs
 * share one GUI, and we have a single controller. So the widgets are the Godforge's; the sync is
 * this addon's own.
 * <p>
 * <b>THE CONNECTOR LINES ARE DERIVED FROM THE PREREQUISITE EDGES.</b> MUI2 has no line
 * primitive, so a 6px texture is stretched to the DISTANCE between two node centres and rotated
 * to the ANGLE - the Godforge's trick, kept. What is not kept is its second, hand-maintained
 * thirty-seven-entry list of which lines to draw: this walks
 * {@link BTSUpgrade#prerequisites()} instead, so the picture cannot show an edge the rules do
 * not enforce, or hide one they do.
 * <p>
 * <b>NO DETAIL SUB-PANEL.</b> The Godforge needs one because some of its nodes take item costs
 * and need slots to insert into; ours cost points and nothing else. Everything a detail panel
 * would say - title, effect, cost, prerequisites, milestone gate, and exactly which requirement
 * is currently failing - is in the node's own auto-updating tooltip, one hover away instead of
 * two clicks, and it costs no extra panel and no extra sync handle.
 * <p>
 * <b>AXIS 1.</b> This class reads exactly ONE synced handle for the tree ({@code treeNodes}, the
 * purchased-node bitmask) and the four milestone counters that were already synced. Every
 * verdict it renders - owned, affordable, locked, and why - comes from the same static bodies in
 * {@link BTSUpgrade} and {@link BTSProgression} that the server validates with. It reads no
 * field of the MTE, because the client's MTE has none of them.
 * <p>
 * <b>THE CLIENT NEVER DECIDES A PURCHASE.</b> A click sends a node INDEX through a
 * server-only synced action ({@code btsTreeBuy} / {@code btsTreeRefund}, registered in
 * {@link MTEReactorSiloGui#registerSyncValues}); the greying-out here is a courtesy, and
 * {@code MTEReactorCoreMk1.buyUpgrade} re-asks every rule against the server's own record.
 */
public final class BTSUpgradeTreePanel {

    /**
     * <b>F8 - SIZED AND PLACED SO IT FITS AT EVERY GUI SCALE.</b> The panel was 300 square,
     * anchored {@code relative(parent).leftRel(1).topRel(0)} - pinned to the top-right corner of
     * GT's 198x203 controller panel - and the arithmetic was checked before it was changed.
     * <p>
     * 1.7.10's {@code ScaledResolution} starts at {@code scaleFactor = 1} and increments while
     * {@code width / (scaleFactor + 1) >= 320 && height / (scaleFactor + 1) >= 240} (capped at
     * the chosen GUI Scale; Auto is uncapped). At 1920x1080 on Auto that runs 2, 3, 4 and stops
     * at 5 because {@code 1080 / 5 = 216 < 240}, so the scaled viewport is <b>480x270</b>. GT's
     * panel is centred there: left {@code (480 - 198) / 2 = 141}, right 339, top
     * {@code (270 - 203) / 2 = 33}. {@code leftRel(1)} puts the tree's left edge on 339, so it
     * ran to 639 against a 480-wide screen - <b>159 of its 300 columns off the right edge</b>,
     * and {@code topRel(0)} put its bottom at 333 against 270, another 63 rows off the bottom.
     * The report's "right two thirds" is right, and the panel was too TALL as well.
     * <p>
     * The loop's own exit condition is the fix: it can only stop once a further step would take
     * the viewport below 320x240, so <b>every</b> resolution and every GUI Scale setting yields a
     * viewport of at least 320x240. A panel that fits inside 320x240 and is centred on the SCREEN
     * (not hung off the parent) is therefore reachable everywhere, with no client-side screen
     * measurement in a builder that also runs on a dedicated server. 300x232 fits with 20x8 to
     * spare; the graph was already scrolling, so the 68 rows come out of the viewport and not out
     * of the tree.
     */
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 232;
    /** Header: the summary line, the help line, then one branch caption over each column. */
    private static final int HEADER_H = 42;
    private static final int CONNECTOR_W = 6;
    private static final int TOOLTIP_DELAY = 8;

    /** One connector colour per branch, in {@link BTSMilestone} ordinal order. */
    private static final UITexture[] CONNECTOR = { GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_BLUE,
        GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_RED, GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_GREEN,
        GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_ORANGE };
    /** Tidy B6: the branch captions, in the same colours as the branch's connector lines. */
    private static final String[] BRANCH_CAPTION = { "UPTIME", "BURNUP", "ORE PROC.", "TURBINE" };
    private static final EnumChatFormatting[] BRANCH_COLOR = { EnumChatFormatting.BLUE, EnumChatFormatting.RED,
        EnumChatFormatting.GREEN, EnumChatFormatting.GOLD };
    private static final UITexture[] CONNECTOR_LIT = { GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_BLUE_OPAQUE,
        GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_RED_OPAQUE, GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_GREEN_OPAQUE,
        GTGuiTextures.PICTURE_UPGRADE_CONNECTOR_ORANGE_OPAQUE };

    private BTSUpgradeTreePanel() {}

    /**
     * Build the panel. {@code main} is the CONTROLLER's sync manager, not the sub-panel's: it is
     * where {@code treeNodes} and the four counters live and where the two purchase actions are
     * registered, and it provably exists on both sides for the whole life of the GUI.
     */
    static ModularPanel build(PanelSyncManager main) {
        final LongSyncValue nodesSync = main.findSyncHandler("treeNodes", LongSyncValue.class);
        final LongSyncValue uptime = main.findSyncHandler("progUptime", LongSyncValue.class);
        final LongSyncValue burnup = main.findSyncHandler("progBurnup", LongSyncValue.class);
        final LongSyncValue reproc = main.findSyncHandler("progReproc", LongSyncValue.class);
        final LongSyncValue perfect = main.findSyncHandler("progPerfect", LongSyncValue.class);

        final LongSupplier mask = nodesSync::getLongValue;
        // The four counters in BTSMilestone ordinal order - the one shape every static
        // derivation takes. Rebuilt per frame, exactly like the controller panel's rows.
        final Supplier<long[]> counters = () -> new long[] { uptime.getLongValue(), burnup.getLongValue(),
            reproc.getLongValue(), perfect.getLongValue() };

        // F8: centred on the SCREEN, not hung off the controller panel's right edge. This is
        // also ModularPanel's own default - its constructor ends in center() - so the two calls
        // below are the explicit form of "undo the anchoring", not a new mechanism. It covers
        // the controller panel while it is open, which is what the close button and the toggle
        // in the gap row are for.
        final ModularPanel panel = new ModularPanel("btsUpgradeTree").relativeToScreen()
            .center()
            .size(PANEL_W, PANEL_H)
            .background(GTGuiTextures.BACKGROUND_STAR)
            .disableHoverBackground();
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            IKey.dynamic(() -> headerLine(mask.getAsLong(), counters.get()))
                .asWidget()
                .pos(5, 5)
                .size(PANEL_W - 26, 10));
        panel.child(
            IKey.str(EnumChatFormatting.GRAY + "Left-click: buy · Right-click: refund · Hover: details")
                .asWidget()
                .pos(5, 17)
                .size(PANEL_W - 10, 10));
        // Tidy B6: which milestone line each column belongs to, above the column and fixed (the
        // graph scrolls under it). The x is the column's own node x, so a caption cannot drift
        // from its column.
        for (int branch = 0; branch < BTSUpgrade.BRANCHES; branch++) {
            panel.child(
                IKey.str(BRANCH_COLOR[branch] + BRANCH_CAPTION[branch])
                    .scale(0.8f)
                    .alignment(Alignment.CENTER)
                    .asWidget()
                    .pos(
                        4 + BTSUpgrade.at(branch, 1)
                            .treeX(),
                        30)
                    .size(BTSUpgrade.BUTTON_W, 9));
        }

        // ListWidget cannot lay this out - the nodes are absolutely positioned, which is the
        // whole point of a graph. VerticalScrollData only, deliberately: ScrollWidget's public
        // constructors take EITHER axis, and a two-axis pan would need a subclass of
        // AbstractScrollWidget. The tree is built 292 wide so it never needs one.
        final VerticalScrollData scrollData = new VerticalScrollData();
        scrollData.setScrollSize(BTSUpgrade.TREE_HEIGHT);
        final ScrollWidget<?> tree = new ScrollWidget<>(scrollData).size(BTSUpgrade.TREE_WIDTH, PANEL_H - HEADER_H - 4);
        tree.pos(4, HEADER_H);

        // Lines FIRST: connectors draw over the background layer no matter what order the
        // children go in, so the buttons have to be added after them to sit on top.
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            for (final BTSUpgrade prereq : node.prerequisites()) {
                tree.child(connector(prereq, node, mask));
            }
        }
        for (final BTSUpgrade node : BTSUpgrade.VALUES) {
            tree.child(nodeButton(node, main, mask, counters));
        }
        panel.child(tree);
        return panel;
    }

    // ------------------------------------------------------------------------ the graph

    /**
     * One prerequisite edge, as a stretched and rotated 6px texture - MUI2 has no line
     * primitive. Lit when BOTH ends are owned, so the shape of what the plant actually holds
     * reads at a glance.
     */
    private static IWidget connector(BTSUpgrade from, BTSUpgrade to, LongSupplier mask) {
        final int fromX = from.treeX() + BTSUpgrade.BUTTON_W / 2;
        final int fromY = from.treeY() + BTSUpgrade.BUTTON_H / 2;
        final int toX = to.treeX() + BTSUpgrade.BUTTON_W / 2;
        final int toY = to.treeY() + BTSUpgrade.BUTTON_H / 2;
        final int length = (int) Math.sqrt(Math.pow((double) toX - fromX, 2) + Math.pow((double) toY - fromY, 2));
        final float rotation = (float) (Math.atan2((double) toY - fromY, (double) toX - fromX) - Math.PI / 2);
        final int x = (fromX + toX) / 2 - CONNECTOR_W / 2;
        final int y = (fromY + toY) / 2 - length / 2;
        final int branch = Math.max(0, to.branch());
        return new DynamicDrawable(() -> {
            final long held = mask.getAsLong();
            final UITexture texture = BTSUpgrade.owns(held, from) && BTSUpgrade.owns(held, to) ? CONNECTOR_LIT[branch]
                : CONNECTOR[branch];
            return new RotatedDrawable(texture).rotationRadian(rotation);
        }).asWidget()
            .pos(x, y)
            .size(CONNECTOR_W, length);
    }

    /** One node. The click is a request; the server is the one that answers it. */
    private static IWidget nodeButton(BTSUpgrade node, PanelSyncManager main, LongSupplier mask,
        Supplier<long[]> counters) {
        return new ButtonWidget<>().size(BTSUpgrade.BUTTON_W, BTSUpgrade.BUTTON_H)
            .pos(node.treeX(), node.treeY())
            // The connector lines draw over the background layer regardless of child order, so
            // the node's look has to live in the OVERLAY - the Godforge hits the same wall.
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .overlay(
                new DynamicDrawable(
                    () -> BTSUpgrade.owns(mask.getAsLong(), node) ? GTGuiTextures.BUTTON_SPACE_PRESSED_32x16
                        : GTGuiTextures.BUTTON_SPACE_32x16),
                IKey.dynamic(() -> stateColor(node, mask.getAsLong(), counters.get()) + node.label())
                    .scale(0.7f)
                    .alignment(Alignment.CENTER))
            .onMousePressed(button -> {
                // AXIS 1 / server authority: the click carries a node INDEX and nothing else.
                // Both actions are registerServerSyncedAction, so the body runs ONLY on the
                // server, which re-asks ownership, prerequisites, the milestone gate and the
                // point budget against its own record before a bit moves.
                final String action = button == 1 ? "btsTreeRefund" : "btsTreeBuy";
                main.callSyncedAction(action, packet -> packet.writeByte(node.ordinal()));
                return true;
            })
            .tooltipDynamic(tooltip -> describe(tooltip, node, mask.getAsLong(), counters.get()))
            .tooltipAutoUpdate(true)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ------------------------------------------------------------------------ readouts
    // Everything below is a pure function of the synced mask and the four synced counters, run
    // through the same static predicates the server validates with. Nothing here reads a field.

    private static String headerLine(long mask, long[] counters) {
        final int spent = BTSUpgrade.pointsSpent(mask);
        final int free = BTSProgression.pointsEarned(counters) - spent;
        return EnumChatFormatting.GOLD + "UPGRADES "
            + BTSUpgrade.owned(mask)
                .size()
            + "/"
            + BTSUpgrade.VALUES.length
            + EnumChatFormatting.RESET
            + " · "
            + spent
            + "/"
            + BTSUpgrade.TOTAL_COST
            + " spent · "
            + (free > 0 ? EnumChatFormatting.AQUA : free < 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY)
            + free
            + " available";
    }

    /** Owned green, buyable aqua, affordable-later yellow, locked grey (GRAY: readable on the dark button). */
    private static String stateColor(BTSUpgrade node, long mask, long[] counters) {
        if (BTSUpgrade.owns(mask, node)) return EnumChatFormatting.GREEN.toString();
        if (!BTSUpgrade.prerequisitesMet(mask, node) || !BTSUpgrade.gateMet(counters, node)) {
            return EnumChatFormatting.GRAY.toString();
        }
        final int free = BTSProgression.pointsEarned(counters) - BTSUpgrade.pointsSpent(mask);
        return free >= node.cost() ? EnumChatFormatting.AQUA.toString() : EnumChatFormatting.YELLOW.toString();
    }

    private static void describe(RichTooltip tooltip, BTSUpgrade node, long mask, long[] counters) {
        tooltip.addLine(EnumChatFormatting.GOLD + node.title());
        tooltip.addLine(EnumChatFormatting.GRAY + node.effectText());
        tooltip.addLine(
            EnumChatFormatting.RESET + "Cost: " + node.cost() + " progression point" + (node.cost() == 1 ? "" : "s"));
        if (node.prerequisites().length > 0) {
            tooltip.addLine(
                (BTSUpgrade.prerequisitesMet(mask, node) ? EnumChatFormatting.GREEN : EnumChatFormatting.RED)
                    + "Needs: "
                    + node.prerequisiteText());
        }
        final BTSMilestone line = node.gateLine();
        if (line != null) {
            final int tier = BTSProgression.tier(counters, line);
            tooltip.addLine(
                (BTSUpgrade.gateMet(counters, node) ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + "Needs: "
                    + node.gateText()
                    + " (at T"
                    + tier
                    + " now)");
        }
        tooltip.addLine(BTSUpgrade.verdict(node, mask, counters));
    }

    /** The button that opens this panel, for the controller GUI's gap row. */
    static IWidget openButton(PanelSyncManager syncManager) {
        // GT's own precedent for a side panel off a multiblock GUI (createPowerPanelButton):
        // a synced panel handler keyed by name, toggled by an 18x18 button in the gap row.
        // The builder ignores the sub-panel's own sync manager - every handle the tree reads
        // and every action it calls lives on the MAIN manager captured here.
        final com.cleanroommc.modularui.api.IPanelHandler treePanel = syncManager
            .syncedPanel("btsUpgradeTree", true, (subManager, handler) -> build(syncManager));
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(4)
            // A chart icon rather than one of the Godforge's own pictures: this is a Beyond the
            // Stars panel, and GT's connector textures are 6px line strips that stretch badly.
            .overlay(GTGuiTextures.OVERLAY_BUTTON_TESLA_TOWER_CHART)
            .onMousePressed(button -> {
                if (treePanel.isPanelOpen()) {
                    treePanel.closePanel();
                } else {
                    treePanel.openPanel();
                }
                return true;
            })
            .tooltipBuilder(
                tooltip -> tooltip.addLine(EnumChatFormatting.GOLD + translateToLocal("beyondthestars.gui.tree.title"))
                    .addLine(EnumChatFormatting.GRAY + translateToLocal("beyondthestars.gui.tree.tooltip"))
                    .addLine(EnumChatFormatting.GRAY + translateToLocal("beyondthestars.gui.tree.refund")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }
}
