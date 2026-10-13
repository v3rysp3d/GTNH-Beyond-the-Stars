package com.nateyburr.beyondthestars.client.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.2): the GL state of the additive and alpha layers round an object. Each {@code begin} captures the
 * state ({@link GlSave}) and sets the layer's; restore the returned save in a finally.
 * <ul>
 * <li>{@link #untextured()}: no texture on either unit, no lighting, no fog (fog would add its colour to an additive
 * layer), smooth shading, blending and the depth test on, no depth writes. The caller picks the blend function and
 * the culling (the Eye's fixed layers, render QA round 4).</li>
 * <li>{@link #additive()}: the same with an additive blend ({@code SRC_ALPHA, ONE}), no culling and no alpha test: a
 * glow, a jet or a shell that lights what is behind it and hides nothing.</li>
 * <li>{@link #emissive()}: for a shader layer: the lightmap at full brightness, additive, no culling, no alpha test,
 * no depth writes; textures stay as they are (the program samples its own units).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class Layers {

    private Layers() {}

    /** The untextured fixed-function layer state; restore the result. */
    public static GlSave untextured() {
        final GlSave s = GlSave.capture();
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        return s;
    }

    /** {@link #untextured()} with an additive blend, no culling, no alpha test; restore the result. */
    public static GlSave additive() {
        final GlSave s = untextured();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        return s;
    }

    /** The state for an additive shader layer; restore the result. */
    public static GlSave emissive() {
        final GlSave s = GlSave.capture();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        return s;
    }
}
