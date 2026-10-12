package com.nateyburr.beyondthestars.machines.kerr;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;

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
        GL11.glPushMatrix();
        GL11.glTranslated(cx, cy, cz);
        GL11.glScalef(s, s, s);
        GL11.glTranslated(-cx, -cy, -cz);
        delegate.renderTileEntityAt(tile, x, y, z, partialTicks);
        GL11.glPopMatrix();
    }
}
