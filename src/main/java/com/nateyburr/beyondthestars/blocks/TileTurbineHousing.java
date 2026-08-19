package com.nateyburr.beyondthestars.blocks;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * TC-4 rotor seat: every Turbine Housing carries one Turbine Rotor slot. In the rotor
 * gallery's stage sockets the seated rotor is what actually converts steam - wear lives ON
 * the item (damage 0-9 usable; at 10 the rotor SHATTERS and the stage goes dark). Spine
 * housings have the same seat but never use it (their meta is the CW/ACW spin toggle).
 */
public class TileTurbineHousing extends TileEntity {

    private ItemStack rotor;

    public boolean hasRotor() {
        return rotor != null;
    }

    /** Wear of the seated rotor (0..maxDamage-1), or -1 when the seat is empty. */
    public int getRotorDamage() {
        return rotor == null ? -1 : rotor.getItemDamage();
    }

    /** Seats a COPY (stackSize 1, wear preserved); false if a rotor is already seated. */
    public boolean installRotor(ItemStack stack) {
        if (rotor != null || stack == null) return false;
        rotor = stack.copy();
        rotor.stackSize = 1;
        markDirty();
        return true;
    }

    /** Removes and returns the seated rotor (wear preserved), or null. */
    public ItemStack extractRotor() {
        final ItemStack out = rotor;
        rotor = null;
        markDirty();
        return out;
    }

    /**
     * Adds wear to the seated rotor. Like a plane turbine, every chip hurts (-5% stage
     * factor per point); at max damage the rotor SHATTERS - item destroyed, stage dead
     * until a replacement is seated.
     */
    public void damageRotor(int points) {
        if (rotor == null) return;
        rotor.setItemDamage(rotor.getItemDamage() + points);
        if (rotor.getItemDamage() >= rotor.getMaxDamage()) {
            rotor = null;
            if (worldObj != null) {
                worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.break", 1.0F, 0.6F);
            }
        }
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (rotor != null) {
            final NBTTagCompound rotorTag = new NBTTagCompound();
            rotor.writeToNBT(rotorTag);
            tag.setTag("Rotor", rotorTag);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        rotor = tag.hasKey("Rotor") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Rotor")) : null;
    }
}
