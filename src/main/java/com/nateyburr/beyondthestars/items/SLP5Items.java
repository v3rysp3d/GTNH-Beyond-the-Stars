package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * MERGE SHIM (p5-eye only; DROP AT MERGE: p5-content owns the real {@code items/SLP5Items.java} with these exact field
 * names and helper signatures). It registers the P5 items the Eye needs so the p5-eye branch compiles and its
 * dev-server bench runs. Called only from {@code P5MergeShim.register()}.
 */
public final class SLP5Items {

    public static Item gatedPelletMagnetar, gatedPelletErgomatter, gatedPelletSuperdense;
    public static Item attractorSeedCore, starChartGreatAttractor;
    public static Item injectorBarrelMk1, injectorBarrelMk2, injectorBarrelMk3;
    public static Item attractorDarkMatterPlate, attractorDarkMatterDensePlate;
    public static Item superconductorWireUXV, superconductorWireMAX, maxBattery;

    private SLP5Items() {}

    public static void register() {
        gatedPelletMagnetar = SLItems.defineWithTooltip("gatedPelletMagnetar");
        gatedPelletErgomatter = SLItems.defineWithTooltip("gatedPelletErgomatter");
        gatedPelletSuperdense = SLItems.defineWithTooltip("gatedPelletSuperdense");
        attractorSeedCore = SLItems.defineWithTooltip("attractorSeedCore");
        starChartGreatAttractor = SLItems.defineWithTooltip("starChartGreatAttractor")
            .setMaxStackSize(1);
        injectorBarrelMk1 = SLItems.defineWithTooltip("injectorBarrelMk1");
        injectorBarrelMk2 = SLItems.defineWithTooltip("injectorBarrelMk2");
        injectorBarrelMk3 = SLItems.defineWithTooltip("injectorBarrelMk3");
    }

    /** GeV per pellet (0 if the stack is not a Gated Pellet). */
    public static double pelletGeV(ItemStack stack) {
        switch (pelletGrade(stack)) {
            case 1:
                return 1.25e9;
            case 2:
                return 5e9;
            case 3:
                return 2e10;
            default:
                return 0;
        }
    }

    /** 1 Magnetar, 2 Ergomatter, 3 Superdense, 0 not a pellet. */
    public static int pelletGrade(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        final Item i = stack.getItem();
        if (i == gatedPelletMagnetar) return 1;
        if (i == gatedPelletErgomatter) return 2;
        if (i == gatedPelletSuperdense) return 3;
        return 0;
    }

    /** The Injector Barrel's Mk (1..3), 0 if not a barrel. */
    public static int barrelTier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        final Item i = stack.getItem();
        if (i == injectorBarrelMk1) return 1;
        if (i == injectorBarrelMk2) return 2;
        if (i == injectorBarrelMk3) return 3;
        return 0;
    }

    /** Pellets per second by barrel Mk: 4 / 8 / 16 (0 for none). */
    public static int barrelRate(int mk) {
        return mk == 1 ? 4 : mk == 2 ? 8 : mk == 3 ? 16 : 0;
    }
}
