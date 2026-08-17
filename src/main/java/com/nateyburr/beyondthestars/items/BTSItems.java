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
    public static Item depletedFuelAssemblyUranium;
    public static Item moxFuelPellet;
    public static Item fuelRodMox;
    public static Item fuelAssemblyMox;
    public static Item depletedFuelAssemblyMox;

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

        depletedFuelAssemblyUranium = new Item().setUnlocalizedName("beyondthestars.depletedFuelAssemblyUranium")
            .setTextureName("beyondthestars:depletedFuelAssemblyUranium")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelAssemblyUranium, "depletedFuelAssemblyUranium");

        moxFuelPellet = new Item().setUnlocalizedName("beyondthestars.moxFuelPellet")
            .setTextureName("beyondthestars:moxFuelPellet")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(moxFuelPellet, "moxFuelPellet");

        fuelRodMox = new Item().setUnlocalizedName("beyondthestars.fuelRodMox")
            .setTextureName("beyondthestars:fuelRodMox")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelRodMox, "fuelRodMox");

        fuelAssemblyMox = new Item().setUnlocalizedName("beyondthestars.fuelAssemblyMox")
            .setTextureName("beyondthestars:fuelAssemblyMox")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelAssemblyMox, "fuelAssemblyMox");

        depletedFuelAssemblyMox = new Item().setUnlocalizedName("beyondthestars.depletedFuelAssemblyMox")
            .setTextureName("beyondthestars:depletedFuelAssemblyMox")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelAssemblyMox, "depletedFuelAssemblyMox");
    }
}
