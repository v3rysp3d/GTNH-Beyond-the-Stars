package com.nateyburr.beyondthestars.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

/**
 * The MAX Battery (p5-t9-t10.md §7.2): an IC2 electric item at GT's MAX tier, holding 2^59 - 1 EU (B-208). GT's battery
 * buffers, chargers and machines handle any {@link IElectricItem} through IC2's electric item manager, which keeps
 * the charge in the stack's NBT. One item, charged and empty alike (the charge bar shows the state), stack size 1.
 */
public class ItemMaxBattery extends BTSTooltipItem implements IElectricItem {

    /** GT's MAX voltage tier. */
    public static final int TIER = 14;
    /**
     * The capacity of record (B-208, settled 2026-10-16): 2^59 - 1 EU, about 5.76e17. GT's battery buffers add their
     * batteries' capacities as a long, so at the spec's 2^63 - 1 two batteries overflowed the readout; at 2^59 - 1 a
     * full 4x4 buffer of sixteen stays under 2^63.
     */
    public static final long CAPACITY = (1L << 59) - 1;

    public ItemMaxBattery() {
        setMaxStackSize(1);
        setMaxDamage(27);
        setNoRepair();
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return true;
    }

    @Override
    public Item getChargedItem(ItemStack stack) {
        return this;
    }

    @Override
    public Item getEmptyItem(ItemStack stack) {
        return this;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return GTValues.V[TIER];
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - Math.max(0.0, Math.min(1.0, charge(stack) / CAPACITY));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(this, 1));
        final ItemStack full = new ItemStack(this, 1);
        if (ElectricItem.manager != null) {
            ElectricItem.manager.charge(full, CAPACITY, Integer.MAX_VALUE, true, false);
        }
        list.add(full);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add(
            EnumChatFormatting.GRAY + "Charge: "
                + EnumChatFormatting.YELLOW
                + String.format("%,d", (long) charge(stack))
                + EnumChatFormatting.GRAY
                + " / "
                + EnumChatFormatting.YELLOW
                + String.format("%,d", CAPACITY)
                + EnumChatFormatting.GRAY
                + " EU");
        super.addInformation(stack, player, lines, advanced);
    }

    private static double charge(ItemStack stack) {
        return ElectricItem.manager == null ? 0.0 : ElectricItem.manager.getCharge(stack);
    }
}
