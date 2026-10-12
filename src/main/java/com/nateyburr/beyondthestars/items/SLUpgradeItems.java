package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * SINGULARITY LADDER items of the field-generator upgrade path (user, 2026-10-13, B-30: "Add an upgrade path";
 * question B-83). A separate holder so {@link SLItems} and {@link SLMaxItems} stay as they are; called from
 * {@code CommonProxy.preInit} right after {@link SLMaxItems#register()}. The field name is the registry name, frozen
 * once shipped.
 */
public final class SLUpgradeItems {

    /** Retrofit Frame (meta 0) and the Compression / Time Dilation Retrofit Kits: {@link ItemFieldRetrofitKit}. */
    public static Item fieldRetrofitKit;

    private SLUpgradeItems() {}

    public static void register() {
        fieldRetrofitKit = new ItemFieldRetrofitKit();
        GameRegistry.registerItem(fieldRetrofitKit, "fieldRetrofitKit");
    }
}
