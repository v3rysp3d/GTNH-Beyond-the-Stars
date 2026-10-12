package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

/**
 * SINGULARITY LADDER items of the Homogeny and the Reality machines (design repo
 * {@code designs/docs/homogeny-reality.md} §4.3, PROPOSED). A separate holder, so the {@link SLItems} fields stay as
 * they are; called from {@code CommonProxy.preInit} right after {@link SLItems#register()}.
 * <p>
 * Same rule as SLItems: the field names ARE the registry names (frozen once shipped), the unlocalized name is
 * {@code item.beyondthestars.<name>}, the texture {@code beyondthestars:<name>}, and the tooltip is the lang key
 * {@code item.beyondthestars.<name>.tooltip} (ruling 14). Every one is made in the Condensate Assembler or by
 * Reality Refab (ruling 16).
 */
public final class SLMaxItems {

    /** Installed in the Homogeny's core slot: T1 -> T2 (4 parallels, Degenerate Stellar Mixture). SL-H10. */
    public static Item homogenyTier2Core;
    /** Installed in the Homogeny's core slot: T2 -> T3 (16 parallels, Entanglement Medium). SL-H20. */
    public static Item homogenyTier3Core;
    /** Made by Reality Refab (SL-R04); the Eye of Harmony's T9 -> T10 core (D-43). */
    public static Item universalUpgradeCore;
    /** Made by Reality Refab (SL-R05, P5 proposal); the Eye T10 star-chart target. */
    public static Item starChartUniverse;

    private SLMaxItems() {}

    public static void register() {
        homogenyTier2Core = SLItems.defineWithTooltip("homogenyTier2Core");
        homogenyTier3Core = SLItems.defineWithTooltip("homogenyTier3Core");
        universalUpgradeCore = SLItems.defineWithTooltip("universalUpgradeCore");
        starChartUniverse = SLItems.defineWithTooltip("starChartUniverse")
            .setMaxStackSize(1);
    }
}
