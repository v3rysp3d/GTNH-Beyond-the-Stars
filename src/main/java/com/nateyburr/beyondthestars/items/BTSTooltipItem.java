package com.nateyburr.beyondthestars.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A plain item whose tooltip comes from the lang file: the lines of
 * {@code <unlocalized name>.tooltip} (for example {@code item.beyondthestars.yellowcake.tooltip}),
 * split on a literal backslash-n and shown in grey. An item without that key shows no tooltip, so
 * every item of the addon can use this class. The item class is not part of a saved world, so an
 * existing item may switch to it freely.
 */
public class BTSTooltipItem extends Item {

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        addLangTooltip(getUnlocalizedName(stack), lines);
    }

    /**
     * Appends the lines of {@code <unlocalizedName>.tooltip}, if the lang file has that key. Shared
     * with the block items, so items and blocks read their tooltips the same way.
     */
    public static void addLangTooltip(String unlocalizedName, List<String> lines) {
        final String key = unlocalizedName + ".tooltip";
        if (!StatCollector.canTranslate(key)) return;
        addLines(StatCollector.translateToLocal(key), lines);
        // docs/tooltip-style.md rule 5: the details behind [LSHIFT], from an optional <key>.shift (client only)
        final String shift = key + ".shift";
        if (!StatCollector.canTranslate(shift)) return;
        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
            || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) {
            addLines(StatCollector.translateToLocal(shift), lines);
        } else {
            lines.add(EnumChatFormatting.DARK_GRAY + "Hold [LSHIFT] for details");
        }
    }

    private static void addLines(String text, List<String> lines) {
        for (String line : text.split("\\\\n")) {
            lines.add(EnumChatFormatting.GRAY + line);
        }
    }
}
