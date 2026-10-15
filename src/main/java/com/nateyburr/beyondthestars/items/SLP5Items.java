package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * P5 items (design repo {@code designs/docs/p5-t9-t10.md} §3, §7 and §9.6; D-52, D-53, Q-P5-1..13 accepted
 * 2026-10-16). A separate holder, so the earlier holders' fields stay as they are; called from
 * {@code CommonProxy.preInit} right after {@link SLQuasarItems#register()}.
 * <p>
 * Same rule as SLItems: the field names ARE the registry names (frozen once shipped), the unlocalized name is
 * {@code item.beyondthestars.<name>}, the texture {@code beyondthestars:<name>} (tools/gen-sl-textures.js), and the
 * tooltip is the lang key {@code item.beyondthestars.<name>.tooltip} (plus {@code .tooltip.shift} for the details
 * behind [LSHIFT]). Recipes: {@code BTSRecipesP5}.
 * <ul>
 * <li>The three <b>Gated Pellets</b> (§3.2), each pressed on the Kerr Compressor module from the grade below. The Eye's
 * Pellet Injector Hatch reads their energy through {@link #pelletGeV(ItemStack)}.</li>
 * <li>The three <b>Injector Barrels</b> (§3.3): the Pellet Injector Hatch's rate cap, the Conduit Throat pattern,
 * read through {@link #barrelTier(ItemStack)} and {@link #barrelRate(int)}.</li>
 * <li>The <b>Attractor Seed Core</b> (Eye T8 to T9, §2.8) and <b>Star Chart: Great Attractor</b> (the T9 target).</li>
 * <li><b>Attractor Dark Matter</b> Plate and Dense Plate (§7.1): the material's solid forms. They are addon items, not
 * a GT material, so no ores, dusts or tool parts appear for a fluid that only an Eye makes (question B-201).</li>
 * <li>The <b>UXV and MAX Superconductor Wires</b> (§7.2, §7.3): RC-2's superconductors stop at UMV. They are crafting
 * parts of the MAX and UXV hatches, coils and laser hatches, not cables (question B-202).</li>
 * <li>The <b>MAX Battery</b> (§7.2): an IC2-style electric item at the MAX tier, 2^63 - 1 EU.</li>
 * </ul>
 */
public final class SLP5Items {

    /** 1.25 EeV (1.25e9 GeV) per pellet: the primer, the core of the Ergomatter pellet. */
    public static Item gatedPelletMagnetar;
    /** 5 EeV per pellet: the core of the Superdense pellet. */
    public static Item gatedPelletErgomatter;
    /** 20 EeV per pellet, 40 % of the GZK limit: T9's working fuel. */
    public static Item gatedPelletSuperdense;

    /** Pellet Injector Hatch barrel, 4 pellets a second. */
    public static Item injectorBarrelMk1;
    /** 8 pellets a second. */
    public static Item injectorBarrelMk2;
    /** 16 pellets a second. */
    public static Item injectorBarrelMk3;

    /** Eye T8 -> T9 core (The Infall), held in escrow. */
    public static Item attractorSeedCore;
    /** Eye T9 target: the Great Attractor. */
    public static Item starChartGreatAttractor;

    /** 144 L of Attractor Dark Matter, solidified. */
    public static Item attractorDarkMatterPlate;
    /** Nine plates, pressed. */
    public static Item attractorDarkMatterDensePlate;

    /** UXV Superconductor Wire (Magnetar Matter, Antipolarization Agent): a crafting part. */
    public static Item superconductorWireUXV;
    /** MAX Superconductor Wire (Attractor Dark Matter): a crafting part. */
    public static Item superconductorWireMAX;

    /** MAX Battery: 2^63 - 1 EU at the MAX tier. */
    public static Item maxBattery;

    /** GeV per pellet by grade 1..3 (index 0 unused). The numbers of record: p5-t9-t10.md §3.2. */
    public static final double[] PELLET_GEV = { 0.0, 1.25e9, 5.0e9, 2.0e10 };
    /** Pellets per second by barrel Mk 1..3 (index 0 unused): §3.3. */
    public static final int[] BARREL_RATE = { 0, 4, 8, 16 };
    /** The GZK limit in GeV (5e10): the tooltip compares the top grade with it. */
    public static final double GZK_LIMIT_GEV = 5.0e10;

    private SLP5Items() {}

    public static void register() {
        gatedPelletMagnetar = SLItems.defineWithTooltip("gatedPelletMagnetar");
        gatedPelletErgomatter = SLItems.defineWithTooltip("gatedPelletErgomatter");
        gatedPelletSuperdense = SLItems.defineWithTooltip("gatedPelletSuperdense");
        injectorBarrelMk1 = SLItems.defineWithTooltip("injectorBarrelMk1")
            .setMaxStackSize(1);
        injectorBarrelMk2 = SLItems.defineWithTooltip("injectorBarrelMk2")
            .setMaxStackSize(1);
        injectorBarrelMk3 = SLItems.defineWithTooltip("injectorBarrelMk3")
            .setMaxStackSize(1);
        attractorSeedCore = SLItems.defineWithTooltip("attractorSeedCore");
        starChartGreatAttractor = SLItems.defineWithTooltip("starChartGreatAttractor")
            .setMaxStackSize(1);
        attractorDarkMatterPlate = SLItems.defineWithTooltip("attractorDarkMatterPlate");
        attractorDarkMatterDensePlate = SLItems.defineWithTooltip("attractorDarkMatterDensePlate");
        superconductorWireUXV = SLItems.defineWithTooltip("superconductorWireUXV");
        superconductorWireMAX = SLItems.defineWithTooltip("superconductorWireMAX");
        maxBattery = new ItemMaxBattery().setUnlocalizedName("beyondthestars.maxBattery")
            .setTextureName("beyondthestars:maxBattery")
            .setCreativeTab(BTSItems.TAB);
        GameRegistry.registerItem(maxBattery, "maxBattery");
    }

    /** The pellet grade of {@code stack}: 1 Magnetar, 2 Ergomatter, 3 Superdense, or 0 if it is not a pellet. */
    public static int pelletGrade(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        final Item item = stack.getItem();
        if (item == gatedPelletMagnetar) return 1;
        if (item == gatedPelletErgomatter) return 2;
        if (item == gatedPelletSuperdense) return 3;
        return 0;
    }

    /** GeV per pellet of {@code stack}, or 0 if it is not a pellet. */
    public static double pelletGeV(ItemStack stack) {
        return PELLET_GEV[pelletGrade(stack)];
    }

    /** One pellet of grade 1..3, or null. */
    public static ItemStack pellet(int grade, int count) {
        final Item item = grade == 1 ? gatedPelletMagnetar
            : grade == 2 ? gatedPelletErgomatter : grade == 3 ? gatedPelletSuperdense : null;
        return item == null ? null : new ItemStack(item, count);
    }

    /** The barrel Mk of {@code stack} (1-3), or 0 if it is not an Injector Barrel. */
    public static int barrelTier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        final Item item = stack.getItem();
        if (item == injectorBarrelMk1) return 1;
        if (item == injectorBarrelMk2) return 2;
        if (item == injectorBarrelMk3) return 3;
        return 0;
    }

    /** Pellets a second of barrel Mk {@code mk} (1-3), or 0. */
    public static int barrelRate(int mk) {
        return mk >= 1 && mk <= 3 ? BARREL_RATE[mk] : 0;
    }

    /** One barrel of Mk 1..3, or null. */
    public static ItemStack barrel(int mk) {
        final Item item = mk == 1 ? injectorBarrelMk1
            : mk == 2 ? injectorBarrelMk2 : mk == 3 ? injectorBarrelMk3 : null;
        return item == null ? null : new ItemStack(item, 1);
    }
}
