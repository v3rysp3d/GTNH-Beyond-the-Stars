package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.machines.eye.render.EyeStarRenderer;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import gregtech.common.render.RenderInit;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here for client-only behaviour (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // D-49: the Eye of Harmony's centre object, drawn with TecTech's star shader and textures. GT's resource-reload
        // hook tells it to rebuild its shader and mesh (they are also built lazily on the first frame).
        ClientRegistry.bindTileEntitySpecialRenderer(TileEyeStar.class, new EyeStarRenderer());
        RenderInit.onResourceReload(EyeStarRenderer::markReload);
    }
}
