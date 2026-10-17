package com.nateyburr.beyondthestars;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.enums.Mods;
import gregtech.api.enums.OreDictNames;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Repairs to the pack that our presence makes necessary. Called from {@code CommonProxy.preInit}.
 *
 * <h2>The Small Coal Boiler recipe (and every other "craftingBlastFurnace" recipe)</h2>
 *
 * GT's Small Coal Boiler is {@code PPP / PwP / BFB}, where F is the ore name {@code craftingBlastFurnace}. GT fills
 * that name in its own preInit ({@code LoaderGTOreDictionary}) with {@code getModItem("etfuturum", "blast_furnace")},
 * but GT does not declare {@code after:etfuturum}, so the lookup only works when FML happens to sort Et Futurum
 * first. Without this mod it does (2.8.4, 2.9.0-beta-3); with it installed on 2.9.0-RC-2, GT's preInit runs before Et
 * Futurum's, the lookup returns null, the ore name stays empty and the boiler has no craftable recipe. Bartworks and
 * the coremod use the same name, so their recipes went with it.
 * <p>
 * Our preInit runs after both (required-after:gregtech, after:etfuturum) and before GT's init, where
 * {@code MTERecipeLoader} builds the boiler recipe, so registering the Blast Furnace here restores it. When the
 * name already has a member (the sort went the other way) this does nothing.
 */
public final class BTSPackFixes {

    private BTSPackFixes() {}

    public static void preInit() {
        restoreCraftingBlastFurnace();
    }

    private static void restoreCraftingBlastFurnace() {
        final String name = OreDictNames.craftingBlastFurnace.toString();
        if (!OreDictionary.getOres(name)
            .isEmpty()) return;
        if (!Mods.EtFuturumRequiem.isModLoaded()) {
            BeyondTheStars.LOG.warn("[BTS] ore name {} is empty and Et Futurum is not loaded; left as is.", name);
            return;
        }
        final ItemStack blastFurnace = GTModHandler.getModItem(Mods.EtFuturumRequiem.ID, "blast_furnace", 1);
        if (blastFurnace == null) {
            BeyondTheStars.LOG.warn(
                "[BTS] ore name {} is empty and Et Futurum has no blast_furnace (enableBlastFurnace=false?); "
                    + "the Small Coal Boiler stays uncraftable.",
                name);
            return;
        }
        GTOreDictUnificator.registerOre(OreDictNames.craftingBlastFurnace, blastFurnace);
        BeyondTheStars.LOG.info(
            "[BTS] ore name {} was empty (GT loaded before Et Futurum); registered etfuturum:blast_furnace, "
                + "which restores the Small Coal Boiler recipe.",
            name);
    }
}
