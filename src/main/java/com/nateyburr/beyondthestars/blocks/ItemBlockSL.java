package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/**
 * The item form of the Singularity Ladder meta-blocks. Unlocalized name
 * {@code tile.beyondthestars.<registry>.<meta>}, so lang keys read
 * {@code tile.beyondthestars.slCasing.1.name}.
 */
public class ItemBlockSL extends ItemBlockWithMetadata {

    public ItemBlockSL(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return field_150939_a.getUnlocalizedName() + "." + stack.getItemDamage();
    }
}
