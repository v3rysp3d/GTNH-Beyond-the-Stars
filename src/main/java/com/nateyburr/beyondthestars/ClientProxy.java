package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.DeferredDraws;
import com.nateyburr.beyondthestars.client.render.KitSelfTest;
import com.nateyburr.beyondthestars.client.render.RenderFrame;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarRenderer;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;
import com.nateyburr.beyondthestars.machines.kerr.KerrHoleRenderer;
import com.nateyburr.beyondthestars.machines.kerr.TileKerrHole;
import com.nateyburr.beyondthestars.rendertest.GuiShotsClient;
import com.nateyburr.beyondthestars.rendertest.RecClient;
import com.nateyburr.beyondthestars.rendertest.RenderTest;
import com.nateyburr.beyondthestars.rendertest.RenderTestClient;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import gregtech.common.render.RenderInit;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here for client-only behaviour (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // Renders v2 render kit: every program (ours and TecTech's star shader for the Eye) is baked lazily and
        // released
        // on GT's resource-reload hook; the frame clock drives the LOD ranking and the dev GPU timer.
        RenderInit.onResourceReload(BTSShaders::markReload);
        RenderFrame.register();
        DeferredDraws.register();
        // Dev-only render kit self test (-Dbts.render.selftest=true): a test panel in front of the camera.
        KitSelfTest.register();

        // D-49: the Eye of Harmony's centre object, drawn with TecTech's star shader and textures.
        ClientRegistry.bindTileEntitySpecialRenderer(TileEyeStar.class, new EyeStarRenderer());

        // KERR REDESIGN R4: the KEE's black hole, drawn by GT's BlackholeRenderer (docs/kerr-redesign.md §5).
        ClientRegistry.bindTileEntitySpecialRenderer(TileKerrHole.class, new KerrHoleRenderer());

        // P4 (quasar-p4.md §1.6): the Quasar Engine's hole (GT's BlackholeRenderer), accretion disc and jets.
        ClientRegistry.bindTileEntitySpecialRenderer(
            com.nateyburr.beyondthestars.machines.quasar.render.TileQuasarHole.class,
            new com.nateyburr.beyondthestars.machines.quasar.render.QuasarHoleRenderer());

        // Dev-only video recorder (-Dbts.rec=true) or render QA harness (-Dbts.rendertest=true); never in normal play.
        if (RecClient.ENABLED) RecClient.register();
        else if (RenderTest.ENABLED) RenderTestClient.register();
        // Dev-only GUI screenshot harness: only with -Dbts.guishots=true (never in normal play).
        if (GuiShotsClient.ENABLED) GuiShotsClient.register();

        // Dev-only questbook screenshot harness: only with -Dbts.qbshots=true (never in normal play).
        if (Boolean.getBoolean("bts.qbshots")) com.nateyburr.beyondthestars.devshots.QuestbookShots.register();
    }
}
