package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * P4 items (design repo {@code designs/docs/quasar-p4.md} §6.2, rev 7f91607; D-50). A separate holder, so the
 * {@link SLItems} / {@link SLMaxItems} fields stay as they are; called from {@code CommonProxy.preInit} right after
 * {@link SLMaxItems#register()}.
 * <p>
 * Same rule as SLItems: the field names ARE the registry names (frozen once shipped), the unlocalized name is
 * {@code item.beyondthestars.<name>}, the texture {@code beyondthestars:<name>} (tools/gen-sl-textures.js), and the
 * tooltip is the lang key {@code item.beyondthestars.<name>.tooltip} (ruling 14). Recipes: {@code BTSRecipesQuasar}.
 */
public final class SLQuasarItems {

    /** Opens the Quasar Engine's own hole at 64 SM, once (SL-Q09, Kerr Compressor module, DENSITY map). */
    public static Item quasarSeed;
    /** Conduit throat, 24 Bu/t: sits in a conduit element's controller slot like a turbine rotor (SL-Q13). */
    public static Item conduitThroatMk1;
    /** Conduit throat, 384 Bu/t (SL-Q14). */
    public static Item conduitThroatMk2;
    /** Conduit throat, 6,144 Bu/t; a post-T7 part (Ergomatter, SL-Q15). */
    public static Item conduitThroatMk3;
    /**
     * Conduit throat, 524,288 Bu/t: the SMBH's (smbh-v1.md §3.4, SL-Q35). Upkeep 16 L/s Ergomatter or 2 L/s Superdense
     * Ergomatter. Registered after the P4 items so their ids stay as they were.
     */
    public static Item conduitThroatMk4;
    /** Eye T6 -> T7 core (the Magnetar Collapse), held in escrow (SL-Q21). */
    public static Item intergalacticUpgradeCore;
    /** Eye T7 -> T8 core (the Galactic Merger), held in escrow (SL-Q22). */
    public static Item supermassiveSeedCore;
    /** Eye T7 target: Triangulum (M33), gated on Magnetar Matter (SL-Q24). */
    public static Item starChartTriangulum;
    /** Eye T8 target: the Milky Way, gated on Ergomatter (SL-Q25). */
    public static Item starChartMilkyWay;
    /**
     * NEI display item "Gamma-Ray Burst (Bu/t)": what a jet port, a conduit throat or a GRB Receptor Port carries.
     * Never crafted and never consumed; the stack size on an NEI page is the Bu/t.
     */
    public static Item gammaRayBurst;

    private SLQuasarItems() {}

    public static void register() {
        quasarSeed = SLItems.defineWithTooltip("quasarSeed")
            .setMaxStackSize(16);
        conduitThroatMk1 = SLItems.defineWithTooltip("conduitThroatMk1")
            .setMaxStackSize(1);
        conduitThroatMk2 = SLItems.defineWithTooltip("conduitThroatMk2")
            .setMaxStackSize(1);
        conduitThroatMk3 = SLItems.defineWithTooltip("conduitThroatMk3")
            .setMaxStackSize(1);
        intergalacticUpgradeCore = SLItems.defineWithTooltip("intergalacticUpgradeCore");
        supermassiveSeedCore = SLItems.defineWithTooltip("supermassiveSeedCore");
        starChartTriangulum = SLItems.defineWithTooltip("starChartTriangulum")
            .setMaxStackSize(1);
        starChartMilkyWay = SLItems.defineWithTooltip("starChartMilkyWay")
            .setMaxStackSize(1);
        gammaRayBurst = SLItems.defineWithTooltip("gammaRayBurst")
            .setCreativeTab(null);
        conduitThroatMk4 = SLItems.defineWithTooltip("conduitThroatMk4")
            .setMaxStackSize(1);
    }

    /**
     * The throat Mk of {@code stack} (1-4), or 0 if it is not a Conduit Throat. The conduit units read their element's
     * throat slot through this, so the cap / upkeep tier comes from one place ({@code BurstRouting}'s tier index is
     * this minus 1).
     */
    public static int throatTier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        final Item item = stack.getItem();
        if (item == conduitThroatMk1) return 1;
        if (item == conduitThroatMk2) return 2;
        if (item == conduitThroatMk3) return 3;
        if (item == conduitThroatMk4) return 4;
        return 0;
    }

    /** One throat of Mk {@code tier} (1-4), or null. */
    public static ItemStack throat(int tier) {
        final Item item = tier == 1 ? conduitThroatMk1
            : tier == 2 ? conduitThroatMk2 : tier == 3 ? conduitThroatMk3 : tier == 4 ? conduitThroatMk4 : null;
        return item == null ? null : new ItemStack(item, 1);
    }
}
