package com.nateyburr.beyondthestars.client.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.RenderState;

/**
 * Renders v2 (§2.2, state hygiene): saves the GL state a render layer changes and puts it back, exactly, after a TESR,
 * so no program, texture unit, blend function, cull or depth setting leaks into the next draw. Explicit
 * {@code glGet}s rather than {@code glPushAttrib} (Angelica's core profile has no attribute stack, and its state cache
 * answers the gets).
 * <p>
 * Saved: blend (on, function), depth test, depth mask, alpha test, cull (on, mode, front face), lighting, fog, shade
 * model, texturing on unit 0 and on the lightmap unit, the active texture unit. Instances are reused from a small
 * stack (render thread only), so a save costs no allocation.
 */
@SideOnly(Side.CLIENT)
public final class GlSave {

    private static final GlSave[] STACK = new GlSave[8];
    private static int top;

    private boolean blend, depthTest, depthMask, alphaTest, cull, lighting, fog, texture, lightmap;
    private long blendFunc;
    private int cullMode, frontFace, shade;

    private GlSave() {}

    /** Captures the current state. Pair with {@link #restore()} (in a finally). */
    public static GlSave capture() {
        if (top >= STACK.length) throw new IllegalStateException("[BTS render] GlSave nested too deep");
        GlSave s = STACK[top];
        if (s == null) STACK[top] = s = new GlSave();
        top++;
        s.blend = GL11.glGetBoolean(GL11.GL_BLEND);
        s.blendFunc = RenderState.savedBlendFunc();
        s.depthTest = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        s.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        s.alphaTest = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);
        s.cull = GL11.glGetBoolean(GL11.GL_CULL_FACE);
        s.cullMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        s.frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
        s.lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
        s.fog = GL11.glGetBoolean(GL11.GL_FOG);
        s.shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        s.texture = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        s.lightmap = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        return s;
    }

    /** Puts everything {@link #capture()} saved back, leaves the default texture unit active. */
    public void restore() {
        if (top == 0 || STACK[top - 1] != this) throw new IllegalStateException("[BTS render] GlSave out of order");
        top--;
        GL11.glShadeModel(shade);
        GL11.glCullFace(cullMode);
        GL11.glFrontFace(frontFace);
        RenderState.restore(GL11.GL_CULL_FACE, cull);
        RenderState.restore(GL11.GL_FOG, fog);
        RenderState.restore(GL11.GL_LIGHTING, lighting);
        RenderState.restore(GL11.GL_TEXTURE_2D, texture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        RenderState.restore(GL11.GL_TEXTURE_2D, lightmap);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        RenderState.restore(GL11.GL_ALPHA_TEST, alphaTest);
        RenderState.restore(GL11.GL_DEPTH_TEST, depthTest);
        GL11.glDepthMask(depthMask);
        RenderState.restore(GL11.GL_BLEND, blend);
        RenderState.restoreBlendFunc(blendFunc);
    }
}
