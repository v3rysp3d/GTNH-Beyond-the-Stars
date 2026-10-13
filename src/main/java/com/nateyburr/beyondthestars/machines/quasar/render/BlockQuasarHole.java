package com.nateyburr.beyondthestars.machines.quasar.render;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.blocks.BlockRenderer;

/**
 * P4 (quasar-p4.md §1.6): the block that carries the Quasar Engine's render ({@link TileQuasarHole}): GT's own
 * {@link BlockRenderer} (no collision, no selection box, unbreakable, no drops, TESR only), registered by its
 * constructor as {@code gt.beyondthestars.quasarHole}. Only a Quasar Engine places it; if something destroys it, the
 * quasar places it again on its next render check.
 */
public class BlockQuasarHole extends BlockRenderer<TileQuasarHole> {

    /** The one instance (null before preInit). */
    public static BlockQuasarHole INSTANCE;

    private BlockQuasarHole() {
        super("beyondthestars.quasarHole", TileQuasarHole::new);
    }

    /** PreInit, from BTSBlocks: the block (registered by GT's constructor) and its tile. */
    public static void register() {
        INSTANCE = new BlockQuasarHole();
        GameRegistry.registerTileEntity(TileQuasarHole.class, "beyondthestars.quasarHole");
    }

    /** No item in NEI or the creative tab. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("rawtypes")
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {}
}
