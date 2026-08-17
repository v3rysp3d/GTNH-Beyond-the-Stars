package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;

public final class BTSBlocks {

    public static Block reactorFuelColumn;

    private BTSBlocks() {}

    public static void register() {
        reactorFuelColumn = new BlockReactorFuelColumn();
        GameRegistry.registerBlock(reactorFuelColumn, "reactorFuelColumn");
    }
}
