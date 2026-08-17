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
    public static Item fuelRodUranium;
    public static Item fuelAssemblyUranium;

    private BTSItems() {}

    public static void register() {
        uraniumFuelPellet = new Item().setUnlocalizedName("beyondthestars.uraniumFuelPellet")
            .setTextureName("beyondthestars:uraniumFuelPellet")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(uraniumFuelPellet, "uraniumFuelPellet");

        fuelRodUranium = new Item().setUnlocalizedName("beyondthestars.fuelRodUranium")
            .setTextureName("beyondthestars:fuelRodUranium")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelRodUranium, "fuelRodUranium");

        fuelAssemblyUranium = new Item().setUnlocalizedName("beyondthestars.fuelAssemblyUranium")
            .setTextureName("beyondthestars:fuelAssemblyUranium")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelAssemblyUranium, "fuelAssemblyUranium");
    }
}
