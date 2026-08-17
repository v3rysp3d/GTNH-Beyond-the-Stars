package com.nateyburr.beyondthestars.machines;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.Config;

public final class BTSMachines {

    public static ItemStack reactorCoreMk1;
    public static ItemStack turbineHall;

    private BTSMachines() {}

    public static void register() {
        final int base = Config.mteBaseId;
        reactorCoreMk1 = new MTEReactorCoreMk1(
            base,
            "multimachine.beyondthestars.reactorcore.mk1",
            "Reactor Core (Gen I)").getStackForm(1);
        turbineHall = new MTETurbineHall(base + 1, "multimachine.beyondthestars.turbinehall", "Turbine Hall")
            .getStackForm(1);
    }
}
