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
    public static Item fuelAssemblyThoriumBonded;
    public static Item depletedFuelAssemblyThoriumBonded;
    public static Item fuelAssemblyNaqBonded;
    public static Item depletedFuelAssemblyNaqBonded;
    public static Item fuelRodThoriumBonded;
    public static Item fuelRodNaqBonded;
    public static Item depletedFuelRodUranium;
    public static Item depletedFuelRodMox;
    public static Item depletedFuelRodThoriumBonded;
    public static Item depletedFuelRodNaqBonded;
    public static Item turbineFanMatched;
    public static Item turbineFanTDA;
    public static Item turbineFanTDAOptimized;
    public static Item turbineRotor;

    private BTSItems() {}

    /**
     * D-26: the one-line item registrar every BTS item file uses.
     * <p>
     * Every field in this package is a plain {@link Item} with a name, a texture and this mod's
     * creative tab, and thirty-odd hand-written four-line blocks is thirty-odd chances to paste
     * the wrong registry name under the right unlocalized one. The registry name is DERIVED from
     * the same string as the unlocalized name and the texture, so the three can never disagree -
     * which is what keeps an existing world's items bound to the fields they were bound to
     * before the split.
     */
    public static Item define(String name) {
        final Item item = new Item().setUnlocalizedName("beyondthestars." + name)
            .setTextureName("beyondthestars:" + name)
            .setCreativeTab(TAB);
        GameRegistry.registerItem(item, name);
        return item;
    }

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

        fuelAssemblyThoriumBonded = new Item().setUnlocalizedName("beyondthestars.fuelAssemblyThoriumBonded")
            .setTextureName("beyondthestars:fuelAssemblyThoriumBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelAssemblyThoriumBonded, "fuelAssemblyThoriumBonded");

        depletedFuelAssemblyThoriumBonded = new Item()
            .setUnlocalizedName("beyondthestars.depletedFuelAssemblyThoriumBonded")
            .setTextureName("beyondthestars:depletedFuelAssemblyThoriumBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelAssemblyThoriumBonded, "depletedFuelAssemblyThoriumBonded");

        fuelAssemblyNaqBonded = new Item().setUnlocalizedName("beyondthestars.fuelAssemblyNaqBonded")
            .setTextureName("beyondthestars:fuelAssemblyNaqBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelAssemblyNaqBonded, "fuelAssemblyNaqBonded");

        depletedFuelAssemblyNaqBonded = new Item().setUnlocalizedName("beyondthestars.depletedFuelAssemblyNaqBonded")
            .setTextureName("beyondthestars:depletedFuelAssemblyNaqBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelAssemblyNaqBonded, "depletedFuelAssemblyNaqBonded");

        fuelRodThoriumBonded = new Item().setUnlocalizedName("beyondthestars.fuelRodThoriumBonded")
            .setTextureName("beyondthestars:fuelRodThoriumBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelRodThoriumBonded, "fuelRodThoriumBonded");

        fuelRodNaqBonded = new Item().setUnlocalizedName("beyondthestars.fuelRodNaqBonded")
            .setTextureName("beyondthestars:fuelRodNaqBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(fuelRodNaqBonded, "fuelRodNaqBonded");

        depletedFuelRodUranium = new Item().setUnlocalizedName("beyondthestars.depletedFuelRodUranium")
            .setTextureName("beyondthestars:depletedFuelRodUranium")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelRodUranium, "depletedFuelRodUranium");

        depletedFuelRodMox = new Item().setUnlocalizedName("beyondthestars.depletedFuelRodMox")
            .setTextureName("beyondthestars:depletedFuelRodMox")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelRodMox, "depletedFuelRodMox");

        depletedFuelRodThoriumBonded = new Item().setUnlocalizedName("beyondthestars.depletedFuelRodThoriumBonded")
            .setTextureName("beyondthestars:depletedFuelRodThoriumBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelRodThoriumBonded, "depletedFuelRodThoriumBonded");

        depletedFuelRodNaqBonded = new Item().setUnlocalizedName("beyondthestars.depletedFuelRodNaqBonded")
            .setTextureName("beyondthestars:depletedFuelRodNaqBonded")
            .setCreativeTab(TAB);
        GameRegistry.registerItem(depletedFuelRodNaqBonded, "depletedFuelRodNaqBonded");

        turbineFanMatched = new Item().setUnlocalizedName("beyondthestars.turbineFanMatched")
            .setTextureName("beyondthestars:turbineFanMatched")
            .setMaxStackSize(1)
            .setCreativeTab(TAB);
        GameRegistry.registerItem(turbineFanMatched, "turbineFanMatched");

        turbineFanTDA = new Item().setUnlocalizedName("beyondthestars.turbineFanTDA")
            .setTextureName("beyondthestars:turbineFanTDA")
            .setMaxStackSize(1)
            .setCreativeTab(TAB);
        GameRegistry.registerItem(turbineFanTDA, "turbineFanTDA");

        turbineFanTDAOptimized = new Item().setUnlocalizedName("beyondthestars.turbineFanTDAOptimized")
            .setTextureName("beyondthestars:turbineFanTDAOptimized")
            .setMaxStackSize(1)
            .setCreativeTab(TAB);
        GameRegistry.registerItem(turbineFanTDAOptimized, "turbineFanTDAOptimized");

        // TC-4: the gallery stage rotor - wear lives on the item (0-9; shatters at 10).
        turbineRotor = new Item().setUnlocalizedName("beyondthestars.turbineRotor")
            .setTextureName("beyondthestars:turbineRotor")
            .setMaxStackSize(1)
            .setMaxDamage(10)
            .setNoRepair()
            .setCreativeTab(TAB);
        GameRegistry.registerItem(turbineRotor, "turbineRotor");

        // D-26 THE SPLIT (docs/production-lines.md S7.1). The four production lines' own items
        // are registered by the three files below - one per owning unit - so that four units
        // can add recipes in parallel without ever opening the same file. This orchestrator and
        // the twenty-two fields above are FROZEN: the fields are what an existing world's
        // stacks are bound to, and nothing in 0.24.0 may move, rename or reorder them.
        BTSItemsFuel.register();
        BTSItemsMaterials.register();
        BTSItemsPropellant.register();
    }
}
