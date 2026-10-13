package com.nateyburr.beyondthestars.machines.kerr;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.client.render.GpuTimer;
import com.nateyburr.beyondthestars.client.render.RenderQuality;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.BlackholeRenderer;

/**
 * KERR REDESIGN R4: draws the KEE's hole with GT's own {@link BlackholeRenderer} (its shader, model and
 * growth/collapse animation), scaled up around the hole centre (docs/kerr-redesign.md §5).
 * <p>
 * Both of GT's shader profiles multiply by the current GL modelview (the legacy shader through
 * {@code gl_ModelViewProjectionMatrix}, the core profile through {@code FrameMatrices}), so a
 * {@code glScalef} about the centre scales GT's draw with no copied shader code. The camera vector GT
 * passes is only used as a direction, which a uniform scale about the centre leaves unchanged. GT's
 * static shader state and resource-reload hook are shared; if its shader failed to compile, nothing is
 * drawn, exactly as for the Black Hole Compressor.
 * <p>
 * Render kit (renders v2 WP-0): the draw is timed by {@code GpuTimer} and asks {@code RenderQuality} for its detail
 * tier; every tier draws GT's hole for now (the lensed v2 hole replaces it at L0-L3 in WP-1, and GT's render stays the
 * fixed-function / low-quality path).
 */
@SideOnly(Side.CLIENT)
public class KerrHoleRenderer extends TileEntitySpecialRenderer {

    private final BlackholeRenderer delegate = new BlackholeRenderer();

    /** The delegate binds its textures through the dispatcher, so it gets the same one. */
    @Override
    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        super.func_147497_a(dispatcher);
        delegate.func_147497_a(dispatcher);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!Config.renderKerrHole || !(tile instanceof TileKerrHole)) return;
        final float s = ((TileKerrHole) tile).getKerrScale();
        final double cx = x + 0.5;
        final double cy = y + 0.5;
        final double cz = z + 0.5;
        // the hole and its disc reach about 2.1 x the scale (TileKerrHole's render box)
        RenderQuality.tier(tile, s * 2.1, Math.sqrt(cx * cx + cy * cy + cz * cz));
        GpuTimer.begin("kerr");
        final boolean glCheck = RenderQuality.glCheckBegin(tile);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(cx, cy, cz);
            GL11.glScalef(s, s, s);
            GL11.glTranslated(-cx, -cy, -cz);
            delegate.renderTileEntityAt(tile, x, y, z, partialTicks);
        } finally {
            GL11.glPopMatrix();
            RenderQuality.glCheckEnd(tile, glCheck, "the Kerr hole");
            GpuTimer.end();
        }
    }
}
