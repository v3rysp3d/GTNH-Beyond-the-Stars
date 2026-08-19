package com.nateyburr.beyondthestars.blocks;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;

public final class BTSBlocks {

    public static Block reactorFuelColumn;
    public static Block rodLoadingPort;
    public static Block turbineHousing;

    private BTSBlocks() {}

    public static void register() {
        reactorFuelColumn = new BlockReactorFuelColumn();
        GameRegistry.registerBlock(reactorFuelColumn, "reactorFuelColumn");
        rodLoadingPort = new BlockRodLoadingPort();
        GameRegistry.registerBlock(rodLoadingPort, "rodLoadingPort");
        GameRegistry.registerTileEntity(TileRodLoadingPort.class, "beyondthestars.rodLoadingPort");
        turbineHousing = new BlockTurbineHousing();
        GameRegistry.registerBlock(turbineHousing, "turbineHousing");
        GameRegistry.registerTileEntity(TileTurbineHousing.class, "beyondthestars.turbineHousing");
    }
}
