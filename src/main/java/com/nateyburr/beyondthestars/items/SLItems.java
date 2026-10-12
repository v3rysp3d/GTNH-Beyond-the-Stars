package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * SINGULARITY LADDER items (singularity-ladder-build.md §1.3). Owned by the Foundation unit;
 * called from {@code CommonProxy.preInit} right after {@link BTSItems#register()}.
 * <p>
 * Every field is a plain {@link Item} from {@link BTSItems#define(String)}: registry name,
 * unlocalized name ({@code item.beyondthestars.<name>}) and texture ({@code beyondthestars:<name>})
 * all derive from the one string, so they cannot drift. The field names ARE the registry names, so
 * they are frozen once shipped. The Eye's D-49 items use {@link #defineWithTooltip(String)}, the same
 * rule plus a lang-file tooltip.
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
    /**
     * Obsolete since D-49 (the interim Eye T4 Black Dwarf Matter target, Q-3). Stays registered so saved
     * worlds keep their charts; SL-P36 crafts it back into Star Chart: Sirius B.
     */
    public static Item starChartSiriusBCooled;

    // ---- D-49 star lifecycle (d49-build-plan.md §9). Field names are registry names: frozen once shipped.
    /** Lights a dark T1-T3 star (one per ignition), taken from a plinth input bus. */
    public static Item stellarIgnitionCharge;
    /** Escrowed core for the T1 -> T2 Mass Infusion. */
    public static Item stellarMassCore;
    /** Escrowed core for the T2 -> T3 Shell Ignition. */
    public static Item shellBurningCore;
    /** Escrowed core for the T4 -> T5 First Cooling. */
    public static Item subspaceCoolingCore;
    /** Escrowed core for the T5 -> T6 Magnetar Merger. */
    public static Item magnetarSeedCore;
    /** Eye T4 target: the neutron star left by Arcturus' Core Collapse. */
    public static Item starChartNeutronStar;
    /** Eye T6 target: SGR 1806-20. */
    public static Item starChartMagnetar;

    private SLItems() {}

    public static void register() {
        superradiantMirrorSegment = BTSItems.define("superradiantMirrorSegment");
        stabiliserPylon = BTSItems.define("stabiliserPylon");
        kerrDensityUpgrade = BTSItems.define("kerrDensityUpgrade");
        galacticUpgradeCore = defineWithTooltip("galacticUpgradeCore");

        starChartSol = BTSItems.define("starChartSol")
            .setMaxStackSize(1);
        starChartSiriusA = BTSItems.define("starChartSiriusA")
            .setMaxStackSize(1);
        starChartArcturus = BTSItems.define("starChartArcturus")
            .setMaxStackSize(1);
        starChartSiriusB = defineWithTooltip("starChartSiriusB").setMaxStackSize(1);
        starChartSiriusBCooled = defineWithTooltip("starChartSiriusBCooled").setMaxStackSize(1);

        // D-49 (d49-build-plan.md §9): appended, never reordered.
        stellarIgnitionCharge = defineWithTooltip("stellarIgnitionCharge").setMaxStackSize(16);
        stellarMassCore = defineWithTooltip("stellarMassCore");
        shellBurningCore = defineWithTooltip("shellBurningCore");
        subspaceCoolingCore = defineWithTooltip("subspaceCoolingCore");
        magnetarSeedCore = defineWithTooltip("magnetarSeedCore");
        starChartNeutronStar = defineWithTooltip("starChartNeutronStar").setMaxStackSize(1);
        starChartMagnetar = defineWithTooltip("starChartMagnetar").setMaxStackSize(1);
    }

    /**
     * {@link BTSItems#define(String)} with a tooltip: the same registry name, unlocalized name and
     * texture from the one string, plus the lines of {@code item.beyondthestars.<name>.tooltip} (split
     * on a literal backslash-n, shown in grey) when the lang file has that key. The item class is not part of a
     * saved world, so an existing item may switch to this one.
     */
    private static Item defineWithTooltip(String name) {
        final Item item = new BTSTooltipItem().setUnlocalizedName("beyondthestars." + name)
            .setTextureName("beyondthestars:" + name)
            .setCreativeTab(BTSItems.TAB);
        GameRegistry.registerItem(item, name);
        return item;
    }
}
