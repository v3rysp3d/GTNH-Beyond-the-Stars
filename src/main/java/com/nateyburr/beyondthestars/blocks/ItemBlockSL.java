package com.nateyburr.beyondthestars.blocks;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The item form of the Singularity Ladder meta-blocks. Unlocalized name
 * {@code tile.beyondthestars.<registry>.<meta>}, so lang keys read
 * {@code tile.beyondthestars.slCasing.1.name}. A {@code tile.beyondthestars.<registry>.<meta>.tooltip} key, when the
 * lang file has one, is shown as grey tooltip lines (split on a literal backslash-n): which machine uses the block and
 * where it is made (ruling 14).
 */
public class ItemBlockSL extends ItemBlockWithMetadata {

    public ItemBlockSL(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return field_150939_a.getUnlocalizedName() + "." + stack.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        final String key = getUnlocalizedName(stack) + ".tooltip";
        if (!StatCollector.canTranslate(key)) return;
        for (String line : StatCollector.translateToLocal(key)
            .split("\\\\n")) {
            lines.add(EnumChatFormatting.GRAY + line);
        }
    }
}
