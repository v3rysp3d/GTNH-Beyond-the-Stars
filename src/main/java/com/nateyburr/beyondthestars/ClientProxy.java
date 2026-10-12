package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.machines.kerr.KerrHoleRenderer;
import com.nateyburr.beyondthestars.machines.kerr.TileKerrHole;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here for client-only behaviour (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // KERR REDESIGN R4: the KEE's black hole, drawn by GT's BlackholeRenderer (docs/kerr-redesign.md §5).
        ClientRegistry.bindTileEntitySpecialRenderer(TileKerrHole.class, new KerrHoleRenderer());
    }
}
