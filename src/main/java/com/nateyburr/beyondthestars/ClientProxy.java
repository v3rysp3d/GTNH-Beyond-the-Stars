package com.nateyburr.beyondthestars;

import com.nateyburr.beyondthestars.client.render.BTSShaders;
import com.nateyburr.beyondthestars.client.render.DeferredDraws;
import com.nateyburr.beyondthestars.client.render.KitSelfTest;
import com.nateyburr.beyondthestars.client.render.RenderFrame;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarRenderer;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;
import com.nateyburr.beyondthestars.machines.kerr.KerrHoleRenderer;
import com.nateyburr.beyondthestars.machines.kerr.TileKerrHole;

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

        // B-227: the server's recipe settings arrive at login; a mismatch is one chat line (NEI shows this client's).
        com.nateyburr.beyondthestars.client.BTSConfigSyncClient.register();

        // Dev-only harnesses, never in normal play. The release jar leaves them out (B-228, build.gradle.kts), so they
        // are reached by name, and only when their switch is set: the video recorder (-Dbts.rec=true) or the render QA
        // harness (-Dbts.rendertest=true), the GUI screenshot harness (-Dbts.guishots=true) and the questbook
        // screenshot harness (-Dbts.qbshots=true).
        if (Boolean.getBoolean("bts.rec")) devTool("rendertest.RecClient");
        else if (Boolean.getBoolean("bts.rendertest")) devTool("rendertest.RenderTestClient");
        if (Boolean.getBoolean("bts.guishots")) devTool("rendertest.GuiShotsClient");
        if (Boolean.getBoolean("bts.qbshots")) devTool("devshots.QuestbookShots");
    }

    /** Calls a dev harness's static {@code register()}; a release jar, which has none of them, logs one line. */
    private static void devTool(String name) {
        final String cls = "com.nateyburr.beyondthestars." + name;
        try {
            Class.forName(cls)
                .getMethod("register")
                .invoke(null);
        } catch (ClassNotFoundException e) {
            BeyondTheStars.LOG.warn("[BTS] " + cls + " is dev tooling and is not in this jar (use the -dev jar).");
        } catch (ReflectiveOperationException e) {
            BeyondTheStars.LOG.error("[BTS] could not start " + cls, e);
        }
    }
}
