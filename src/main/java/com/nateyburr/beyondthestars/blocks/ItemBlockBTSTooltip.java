package com.nateyburr.beyondthestars.blocks;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.items.BTSTooltipItem;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The item form of the nuclear plant's own blocks (Reactor Fuel Pillar Frame, Rod Loading Port,
 * Turbine Housing): a plain {@link ItemBlock} plus the tooltip in
 * {@code tile.beyondthestars.<name>.tooltip}. Registering a block with a different item class
 * keeps its registry name, so placed blocks and stored stacks are unaffected.
 */
public class ItemBlockBTSTooltip extends ItemBlock {

    public ItemBlockBTSTooltip(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        BTSTooltipItem.addLangTooltip(getUnlocalizedName(stack), lines);
    }
}
