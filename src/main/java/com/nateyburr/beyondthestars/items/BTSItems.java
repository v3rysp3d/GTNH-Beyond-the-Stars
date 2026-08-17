package com.nateyburr.beyondthestars.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class BTSItems {

    public static final CreativeTabs TAB = new CreativeTabs("beyondthestars") {

        @Override
        public Item getTabIconItem() {
            return uraniumFuelPellet;
        }
    };

    public static Item uraniumFuelPellet;

    private BTSItems() {}

    public static void register() {
        uraniumFuelPellet = new Item().setUnlocalizedName("beyondthestars.uraniumFuelPellet")
            .setTextureName("beyondthestars:uraniumFuelPellet")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(uraniumFuelPellet, "uraniumFuelPellet");
    }
}
