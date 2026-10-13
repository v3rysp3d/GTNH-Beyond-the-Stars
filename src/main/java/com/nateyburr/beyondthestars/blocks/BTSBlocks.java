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
        GameRegistry.registerBlock(reactorFuelColumn, ItemBlockBTSTooltip.class, "reactorFuelColumn");
        rodLoadingPort = new BlockRodLoadingPort();
        GameRegistry.registerBlock(rodLoadingPort, ItemBlockBTSTooltip.class, "rodLoadingPort");
        GameRegistry.registerTileEntity(TileRodLoadingPort.class, "beyondthestars.rodLoadingPort");
        turbineHousing = new BlockTurbineHousing();
        GameRegistry.registerBlock(turbineHousing, ItemBlockBTSTooltip.class, "turbineHousing");
        GameRegistry.registerTileEntity(TileTurbineHousing.class, "beyondthestars.turbineHousing");
        // SINGULARITY LADDER (singularity-ladder-build.md §1.2): the Kerr / Eye-band casings.
        SLBlocks.register();
        // D-49: the Eye of Harmony's centre render block and tile (d49-build-plan.md, my answer 3).
        com.nateyburr.beyondthestars.machines.eye.render.BlockEyeStar.register();
        // KERR REDESIGN R4: the KEE's black-hole render block and tile (docs/kerr-redesign.md §5).
        com.nateyburr.beyondthestars.machines.kerr.BlockKerrHole.register();
        // P4 (quasar-p4.md §1.6): the Quasar Engine's hole / disc / jet render block and tile.
        com.nateyburr.beyondthestars.machines.quasar.render.BlockQuasarHole.register();
    }
}
