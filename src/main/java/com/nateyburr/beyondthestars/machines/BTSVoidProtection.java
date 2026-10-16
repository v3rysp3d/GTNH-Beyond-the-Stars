package com.nateyburr.beyondthestars.machines;

import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * B-230: the Extraction Platform and the Yellowcake Mill gained GT's void protection. Before it they did not support
 * it, so GT saved every one of them as {@code VOID_ALL}, and GT's load would keep that now that it is allowed. A
 * machine saved without our marker is therefore moved once to GT's default (protect, {@code VOID_NONE}); from then on
 * its voiding button is the player's. Tile NBT only, like GT's own voiding mode.
 */
public final class BTSVoidProtection {

    private BTSVoidProtection() {}

    /** Written by every save of a machine that has void protection. */
    public static final String NBT_KEY = "btsVoidProtection";

    /** Call after {@code super.loadNBTData}: a save from before void protection starts on GT's default. */
    public static void migrate(MTEMultiBlockBase machine, NBTTagCompound nbt) {
        if (!nbt.getBoolean(NBT_KEY)) machine.setVoidingMode(machine.getDefaultVoidingMode());
    }
}
