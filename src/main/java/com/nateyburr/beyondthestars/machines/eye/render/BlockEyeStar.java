package com.nateyburr.beyondthestars.machines.eye.render;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.blocks.BlockRenderer;

/**
 * D-49 answer 3: the block that carries the Eye of Harmony's centre render ({@link TileEyeStar}). GT's own
 * {@link BlockRenderer} (the Black Hole Compressor's render block: no collision, no selection box, unbreakable, no
 * drops, TESR only), registered by its constructor as {@code beyondthestars:gt.beyondthestars.eyeStar}. Only an Eye
 * places it; it is never in NEI or a creative tab.
 */
public class BlockEyeStar extends BlockRenderer<TileEyeStar> {

    /** The one instance (null before preInit). */
    public static BlockEyeStar INSTANCE;

    private BlockEyeStar() {
        super("beyondthestars.eyeStar", TileEyeStar::new);
    }

    /** PreInit, from BTSBlocks: the block (registered by GT's constructor) and its tile. */
    public static void register() {
        INSTANCE = new BlockEyeStar();
        GameRegistry.registerTileEntity(TileEyeStar.class, "beyondthestars.eyeStar");
    }

    /** No item in NEI or the creative tab: a render block is nothing a player should hold. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("rawtypes")
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {}
}
