package com.nateyburr.beyondthestars.machines.quasar.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.BlackholeRenderer;

/**
 * P4 (quasar-p4.md §1.6): draws the Quasar Engine's hole, accretion disc and two jets.
 * <ul>
 * <li><b>The hole:</b> GT's own {@link BlackholeRenderer} (the Black Hole Compressor's shader, model and growth /
 * collapse animation), {@code glScalef}-scaled about the centre exactly as the Kerr's {@code KerrHoleRenderer} does;
 * lasers off. Its stability follows the disc's Stability.</li>
 * <li><b>The accretion disc</b> (our own geometry; GT's model disc only reaches about 1.6× the horizon): an additive
 * annulus from r 40 to r 40 + 22·λc (the voxel disc band), so its width shows how much gas is in it. Its colour follows
 * E's zone (Resonant ice-white, Stable gold, Strained orange, Critical red, dark while re-forming); three spiral arms
 * turn with it; it puffs up (top and bottom faces part) when E is above 0, and flashes outward on an eruption.</li>
 * <li><b>The jets:</b> two additive cones along the axis to ±78 blocks, a bright core in a wide sheath, each as bright
 * as
 * its port's Bu/t, so the flux sawtooth and the eruptions are visible.</li>
 * </ul>
 * Everything is depth-tested with no depth writes, so the structure hides what is behind it and the bands never cut
 * each other. {@link Config#renderQuasar} off (or the machine's GUI switch, which removes the block) draws nothing.
 */
@SideOnly(Side.CLIENT)
public class QuasarHoleRenderer extends TileEntitySpecialRenderer {

    private final BlackholeRenderer delegate = new BlackholeRenderer();

    /** Disc geometry, blocks from the centre (the voxel disc band). */
    static final float DISC_R0 = 40f, DISC_W = 22f;
    /** Jet reach, blocks along the axis. */
    static final float JET_LEN = 78f;

    private static final int SEGMENTS = 96;
    private static final int RINGS = 6;
    private static final int JET_SIDES = 16;
    private static final int JET_STACKS = 12;

    /** Zone colours (0 Resonant .. 4 Breach), RGB 0..1. */
    private static final float[][] ZONE_RGB = { { 0.75f, 0.92f, 1.00f }, { 1.00f, 0.82f, 0.48f },
        { 1.00f, 0.54f, 0.24f }, { 1.00f, 0.23f, 0.18f }, { 1.00f, 0.10f, 0.10f } };
    private static final float[] DARK_RGB = { 0.35f, 0.25f, 0.45f };

    /** The delegate binds its textures through the dispatcher, so it gets the same one. */
    @Override
    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        super.func_147497_a(dispatcher);
        delegate.func_147497_a(dispatcher);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!Config.renderQuasar || !(tile instanceof TileQuasarHole)) return;
        final TileQuasarHole q = (TileQuasarHole) tile;
        final float s = q.getHoleScale();
        final double cx = x + 0.5;
        final double cy = y + 0.5;
        final double cz = z + 0.5;
        GL11.glPushMatrix();
        GL11.glTranslated(cx, cy, cz);
        GL11.glScalef(s, s, s);
        GL11.glTranslated(-cx, -cy, -cz);
        delegate.renderTileEntityAt(tile, x, y, z, partialTicks);
        GL11.glPopMatrix();

        final long now = tile.getWorldObj() == null ? 0
            : tile.getWorldObj()
                .getTotalWorldTime();
        final float t = now + partialTicks;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glTranslated(cx, cy, cz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        try {
            drawDisc(q, t);
            drawJet(q.getJetTop(), 1, t);
            drawJet(q.getJetBottom(), -1, t);
        } finally {
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glDepthMask(true);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    /** The accretion disc: an additive annulus with three turning spiral arms. */
    private static void drawDisc(TileQuasarHole q, float t) {
        final float lc = Math.max(0, Math.min(1, q.getLambdaC()));
        final int zone = q.getZone();
        final float[] rgb = zone >= 0 && zone < ZONE_RGB.length ? ZONE_RGB[zone] : DARK_RGB;
        final float width = zone < 0 ? 4f : 2f + DISC_W * lc;
        final float r0 = DISC_R0, r1 = DISC_R0 + width;
        // E above 0 puffs the disc up: the two faces part by up to 4 blocks
        final float puff = 0.3f + Math.max(0, Math.min(4f, q.getE() / 25f));
        // the eruption flash: brighter for 3 s, fading
        final float since = t - q.getFlashTick();
        final float flash = since >= 0 && since < 60 ? (1f - since / 60f) * (0.6f + q.getFlashSize()) : 0f;
        final float base = (zone < 0 ? 0.12f : 0.35f) * (1f + 1.5f * flash);
        final float spin = t * 0.004f;
        final Tessellator tess = Tessellator.instance;
        for (int face = -1; face <= 1; face += 2) {
            final float yy = face * puff * 0.5f;
            tess.startDrawingQuads();
            for (int ring = 0; ring < RINGS; ring++) {
                final float fa = ring / (float) RINGS, fb = (ring + 1) / (float) RINGS;
                final float ra = r0 + (r1 - r0) * fa, rb = r0 + (r1 - r0) * fb;
                // brightest at the inner edge, gone at the outer
                final float aa = base * (1f - fa) * (1f - fa), ab = base * (1f - fb) * (1f - fb);
                for (int seg = 0; seg < SEGMENTS; seg++) {
                    final double t0 = seg * 2 * Math.PI / SEGMENTS, t1 = (seg + 1) * 2 * Math.PI / SEGMENTS;
                    final float arm0 = arm(t0, ra, spin), arm1 = arm(t1, ra, spin);
                    final float arm2 = arm(t1, rb, spin), arm3 = arm(t0, rb, spin);
                    vertex(tess, rgb, aa * arm0, ra * Math.cos(t0), yy, ra * Math.sin(t0));
                    vertex(tess, rgb, aa * arm1, ra * Math.cos(t1), yy, ra * Math.sin(t1));
                    vertex(tess, rgb, ab * arm2, rb * Math.cos(t1), yy, rb * Math.sin(t1));
                    vertex(tess, rgb, ab * arm3, rb * Math.cos(t0), yy, rb * Math.sin(t0));
                }
            }
            tess.draw();
        }
        // the flash ring, running outward over the disc
        if (flash > 0) {
            final float rr = r0 + (r1 - r0 + 8f) * Math.min(1f, since / 60f);
            tess.startDrawingQuads();
            for (int seg = 0; seg < SEGMENTS; seg++) {
                final double t0 = seg * 2 * Math.PI / SEGMENTS, t1 = (seg + 1) * 2 * Math.PI / SEGMENTS;
                final float a = 0.6f * flash;
                final float[] white = { 1f, 0.95f, 0.9f };
                vertex(tess, white, a, (rr - 1.5f) * Math.cos(t0), 0, (rr - 1.5f) * Math.sin(t0));
                vertex(tess, white, a, (rr - 1.5f) * Math.cos(t1), 0, (rr - 1.5f) * Math.sin(t1));
                vertex(tess, white, 0, (rr + 1.5f) * Math.cos(t1), 0, (rr + 1.5f) * Math.sin(t1));
                vertex(tess, white, 0, (rr + 1.5f) * Math.cos(t0), 0, (rr + 1.5f) * Math.sin(t0));
            }
            tess.draw();
        }
    }

    /** Spiral-arm brightness 0.55..1 at angle {@code th}, radius {@code r}. */
    private static float arm(double th, float r, float spin) {
        return 0.775f + 0.225f * (float) Math.sin(3 * th - r * 0.12 - spin * 6);
    }

    /**
     * One jet: a cone from the horizon out to {@link #JET_LEN} along {@code dir} (+1 up, −1 down), as bright as its
     * Bu/t (log scale: 0.1 Bu/t faint, 1,000 Bu/t full), a core and a sheath.
     */
    private static void drawJet(float bu, int dir, float t) {
        if (bu <= 0) return;
        final float bright = Math.max(0.08f, Math.min(1f, (float) (0.35 + 0.2 * Math.log10(bu))));
        final float[] core = { 0.85f, 0.95f, 1f };
        final float[] sheath = { 0.35f, 0.65f, 1f };
        drawCone(core, 0.55f * bright, 0.6f, 1.4f, dir, t);
        drawCone(sheath, 0.22f * bright, 1.6f, 4.2f, dir, t * 0.7f);
    }

    private static void drawCone(float[] rgb, float alpha, float rBase, float rTip, int dir, float t) {
        final Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        final float y0 = 3f;
        for (int st = 0; st < JET_STACKS; st++) {
            final float fa = st / (float) JET_STACKS, fb = (st + 1) / (float) JET_STACKS;
            final float ya = dir * (y0 + (JET_LEN - y0) * fa), yb = dir * (y0 + (JET_LEN - y0) * fb);
            final float ra = rBase + (rTip - rBase) * fa, rb = rBase + (rTip - rBase) * fb;
            // fades along its length, with a travelling pulse
            final float pa = 0.8f + 0.2f * (float) Math.sin(fa * 18 - t * 0.35f);
            final float pb = 0.8f + 0.2f * (float) Math.sin(fb * 18 - t * 0.35f);
            final float aa = alpha * (1f - 0.7f * fa) * pa, ab = alpha * (1f - 0.7f * fb) * pb;
            for (int s = 0; s < JET_SIDES; s++) {
                final double t0 = s * 2 * Math.PI / JET_SIDES, t1 = (s + 1) * 2 * Math.PI / JET_SIDES;
                vertex(tess, rgb, aa, ra * Math.cos(t0), ya, ra * Math.sin(t0));
                vertex(tess, rgb, aa, ra * Math.cos(t1), ya, ra * Math.sin(t1));
                vertex(tess, rgb, ab, rb * Math.cos(t1), yb, rb * Math.sin(t1));
                vertex(tess, rgb, ab, rb * Math.cos(t0), yb, rb * Math.sin(t0));
            }
        }
        tess.draw();
    }

    private static void vertex(Tessellator tess, float[] rgb, float a, double x, double y, double z) {
        tess.setColorRGBA_F(rgb[0], rgb[1], rgb[2], Math.max(0, Math.min(1, a)));
        tess.addVertex(x, y, z);
    }
}
