package com.nateyburr.beyondthestars.machines.kerr;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.blocks.BlockRenderer;

/**
 * KERR REDESIGN R4: the block that carries the KEE's black-hole render ({@link TileKerrHole}). GT's own
 * {@link BlockRenderer} (the Black Hole Compressor's render block: no collision, no selection box,
 * unbreakable, no drops, TESR only), registered by its constructor as
 * {@code beyondthestars:gt.beyondthestars.kerrHole}. Only a KEE places it (docs/kerr-redesign.md §5).
 * If something does destroy it (a big explosion), the KEE places it again on its next render check.
 */
public class BlockKerrHole extends BlockRenderer<TileKerrHole> {

    /** The one instance (null before preInit). */
    public static BlockKerrHole INSTANCE;

    private BlockKerrHole() {
        super("beyondthestars.kerrHole", TileKerrHole::new);
    }

    /** PreInit, from BTSBlocks: the block (registered by GT's constructor) and its tile. */
    public static void register() {
        INSTANCE = new BlockKerrHole();
        GameRegistry.registerTileEntity(TileKerrHole.class, "beyondthestars.kerrHole");
    }

    /** No item in NEI or the creative tab: a render block is nothing a player should hold. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("rawtypes")
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {}
}
