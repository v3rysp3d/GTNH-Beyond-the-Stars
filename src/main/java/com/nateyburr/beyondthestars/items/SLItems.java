package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

/**
 * SINGULARITY LADDER items (singularity-ladder-build.md §1.3). Owned by the Foundation unit;
 * called from {@code CommonProxy.preInit} right after {@link BTSItems#register()}.
 * <p>
 * Every field is a plain {@link Item} from {@link BTSItems#define(String)}: registry name,
 * unlocalized name ({@code item.beyondthestars.<name>}) and texture ({@code beyondthestars:<name>})
 * all derive from the one string, so they cannot drift. The field names ARE the registry names, so
 * they are frozen once shipped.
 */
public final class SLItems {

    /** Kerr controller component (x8). */
    public static Item superradiantMirrorSegment;
    /** Eye controller component (x6). */
    public static Item stabiliserPylon;
    /** Consumed by the Kerr controller on install: Kerr T2. */
    public static Item kerrDensityUpgrade;
    /** Consumed by the Eye controller on install: unlocks Eye T4. */
    public static Item galacticUpgradeCore;

    /** Eye T1 star target (not consumed). */
    public static Item starChartSol;
    /** Eye T2 star target. */
    public static Item starChartSiriusA;
    /** Eye T3 star target. */
    public static Item starChartArcturus;
    /** Eye T4 star target. */
    public static Item starChartSiriusB;
    /** Eye T4 interim Black Dwarf Matter target (Q-3). Delete with P4's Eye T5. */
    public static Item starChartSiriusBCooled;

    private SLItems() {}

    public static void register() {
        superradiantMirrorSegment = BTSItems.define("superradiantMirrorSegment");
        stabiliserPylon = BTSItems.define("stabiliserPylon");
        kerrDensityUpgrade = BTSItems.define("kerrDensityUpgrade");
        galacticUpgradeCore = BTSItems.define("galacticUpgradeCore");

        starChartSol = BTSItems.define("starChartSol")
            .setMaxStackSize(1);
        starChartSiriusA = BTSItems.define("starChartSiriusA")
            .setMaxStackSize(1);
        starChartArcturus = BTSItems.define("starChartArcturus")
            .setMaxStackSize(1);
        starChartSiriusB = BTSItems.define("starChartSiriusB")
            .setMaxStackSize(1);
        starChartSiriusBCooled = BTSItems.define("starChartSiriusBCooled")
            .setMaxStackSize(1);
    }
}
