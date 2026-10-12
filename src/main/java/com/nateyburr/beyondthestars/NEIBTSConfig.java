package com.nateyburr.beyondthestars;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.machines.BTSMachines;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

/**
 * NEI plugin (found by NEI's class scan: the name is NEI*Config, as GT's own NEIGTConfig).
 * Client only - NEI never loads it on a dedicated server.
 * <p>
 * Tidy C10: hides the two RETIRED plant module controllers (Turbine Hall, Cooling Tower). They
 * stay registered so old worlds load, but they do nothing, so the item list should not offer
 * them to a new player.
 */
public class NEIBTSConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        hide(BTSMachines.moduleTurbineHall);
        hide(BTSMachines.moduleCoolingTower);
    }

    private static void hide(ItemStack stack) {
        if (stack != null) API.hideItem(stack);
    }

    @Override
    public String getName() {
        return "Beyond the Stars";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
