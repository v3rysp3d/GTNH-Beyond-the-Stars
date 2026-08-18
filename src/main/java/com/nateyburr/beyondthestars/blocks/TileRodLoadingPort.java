package com.nateyburr.beyondthestars.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import com.nateyburr.beyondthestars.items.BTSItems;

/**
 * Inventory for the Rod Loading Port: sits directly above a fuel pillar and buffers fuel
 * rods for it. Hoppers/AE2 insert here; the Reactor silo drains the buffer into the pillar
 * below at each decay-cycle boundary (rods cascade to the lowest empty segment).
 */
public class TileRodLoadingPort extends TileEntity implements IInventory {

    public static final int SLOTS = 4;

    private final ItemStack[] slots = new ItemStack[SLOTS];

    public static boolean isFuelRod(ItemStack stack) {
        if (stack == null) return false;
        final Item item = stack.getItem();
        return item == BTSItems.fuelRodUranium || item == BTSItems.fuelRodMox
            || item == BTSItems.fuelRodThoriumBonded
            || item == BTSItems.fuelRodNaqBonded;
    }

    /**
     * Removes and returns one rod item from the buffer, or null if none. Non-rod stacks that
     * somehow reach a slot are skipped, never consumed - they stay visible and drop on break.
     */
    public ItemStack takeOneRod() {
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null && slots[i].stackSize > 0 && isFuelRod(slots[i])) {
                return decrStackSize(i, 1);
            }
        }
        return null;
    }

    @Override
    public int getSizeInventory() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        final ItemStack stack = slots[slot];
        if (stack == null) return null;
        if (stack.stackSize <= amount) {
            slots[slot] = null;
            markDirty();
            return stack;
        }
        final ItemStack split = stack.splitStack(amount);
        markDirty();
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        final ItemStack stack = slots[slot];
        slots[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        slots[slot] = stack;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "beyondthestars.rodLoadingPort";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isFuelRod(stack);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        final NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null) {
                final NBTTagCompound entry = new NBTTagCompound();
                entry.setByte("Slot", (byte) i);
                slots[i].writeToNBT(entry);
                list.appendTag(entry);
            }
        }
        tag.setTag("Items", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        final NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = null;
        }
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound entry = list.getCompoundTagAt(i);
            final int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < SLOTS) {
                slots[slot] = ItemStack.loadItemStackFromNBT(entry);
            }
        }
    }
}
