package com.nateyburr.beyondthestars.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (R-16): the depth of the opaque world, for the machine renders drawn after the translucent pass
 * ({@link DeferredDraws}) under Angelica.
 * <p>
 * The machine volumes (the Homogeny's vortex, the Diffraction's drums and spine, Refab's trays) sit inside TecTech's
 * glass and draw after the translucent pass, so the glass does not swallow their light. Vanilla's translucent pass
 * leaves them visible; Angelica's (Celeritas) writes the glass into the depth buffer, and every volume, glow and ring
 * behind glass then failed the depth test: the machines looked empty under Angelica (found by the R-16 run with
 * Angelica 2.2.26 in the dev client). So under Angelica, with no shaderpack, the depth buffer is saved when the first
 * machine queues its draw (GT calls the machines' hook in pass 0, after the opaque world), and for the deferred draws
 * that opaque depth is put back into the main framebuffer (blocks still hide the renders, glass no longer does), then
 * the full depth is restored for whatever draws next. Three depth blits per frame, only while a machine render is in
 * view. Framebuffer blits need GL 3.0 and the main framebuffer object; without them, or with a shaderpack (the pack
 * owns the framebuffers), the machines draw in pass 0 instead ({@link #drawImmediately()}): visible, behind the glass.
 */
@SideOnly(Side.CLIENT)
public final class OpaqueDepth {

    /** Angelica's state manager: present means Celeritas draws the world. */
    private static final boolean ANGELICA = classExists("com.gtnewhorizons.angelica.glsm.GLStateManager");
    /** Dev switch {@code -Dbts.render.opaqueDepth=false}: never blit (Angelica then draws the machines in pass 0). */
    private static final boolean ALLOWED = !"false".equals(System.getProperty("bts.render.opaqueDepth"));

    private static int opaqueFbo, opaqueRb, savedFbo, savedRb, width, height, format;
    private static boolean failed, logged;
    private static long capturedFrame = -1;

    private OpaqueDepth() {}

    /** True when the deferred machine draws should rather run at once in pass 0 (Angelica without the blits). */
    static boolean drawImmediately() {
        return ANGELICA && !usable();
    }

    /** Whether the blits run this frame (Angelica, no shaderpack, GL 3.0, the main framebuffer, no earlier failure). */
    static boolean usable() {
        if (!ANGELICA || !ALLOWED || failed || ShaderpackProbe.active()) return false;
        final Framebuffer fb = Minecraft.getMinecraft()
            .getFramebuffer();
        return fb != null && fb.framebufferObject > 0 && GLContext.getCapabilities().OpenGL30;
    }

    /** Pass 0 (a machine queues its first draw of the frame): save the opaque world's depth. */
    static void captureOpaque() {
        final long frame = RenderFrame.frame();
        if (capturedFrame == frame || !usable()) return;
        final Framebuffer fb = Minecraft.getMinecraft()
            .getFramebuffer();
        if (!ensure(fb)) return;
        GpuTimer.begin("depthcopy");
        try {
            blit(fb.framebufferObject, opaqueFbo);
        } finally {
            GpuTimer.end();
        }
        capturedFrame = frame;
    }

    /** Before the deferred draws: true when the opaque depth was put in place ({@link #restore} afterwards). */
    static boolean useOpaque() {
        if (capturedFrame != RenderFrame.frame() || !usable()) return false;
        final Framebuffer fb = Minecraft.getMinecraft()
            .getFramebuffer();
        GpuTimer.begin("depthcopy");
        try {
            blit(fb.framebufferObject, savedFbo);
            blit(opaqueFbo, fb.framebufferObject);
        } finally {
            GpuTimer.end();
        }
        return true;
    }

    /** After the deferred draws: the full depth (with the translucent world) back in the main framebuffer. */
    static void restore() {
        GpuTimer.begin("depthcopy");
        try {
            blit(
                savedFbo,
                Minecraft.getMinecraft()
                    .getFramebuffer().framebufferObject);
        } finally {
            GpuTimer.end();
        }
    }

    // ================================================================================ GL

    private static void blit(int from, int to) {
        final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
    }

    /**
     * Our two depth-only framebuffers, the main framebuffer's size and depth format (a depth blit needs the same
     * format); false (and the machines draw in pass 0 from then on) if they cannot be made.
     */
    private static boolean ensure(Framebuffer fb) {
        final int w = fb.framebufferWidth, h = fb.framebufferHeight;
        if (opaqueFbo != 0 && w == width && h == height) return true;
        release();
        // drain earlier errors so the check below sees ours; capped, a lost context keeps reporting one
        for (int i = 0; i < 64 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
            // drain
        }
        final int was = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb.framebufferObject);
        final int depthAttachment = attachedDepth();
        format = depthFormat(depthAttachment);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, was);
        if (format == 0) return fail("the main framebuffer's depth format is unknown");
        width = w;
        height = h;
        opaqueRb = GL30.glGenRenderbuffers();
        savedRb = GL30.glGenRenderbuffers();
        opaqueFbo = depthFbo(opaqueRb, depthAttachment);
        savedFbo = depthFbo(savedRb, depthAttachment);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, opaqueFbo);
        final int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, was);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE || GL11.glGetError() != GL11.GL_NO_ERROR) {
            release();
            return fail("depth framebuffer incomplete (0x" + Integer.toHexString(status) + ")");
        }
        if (!logged) {
            logged = true;
            BeyondTheStars.LOG.info(
                "[BTS render] Angelica: machine renders draw over the opaque depth ({}x{}, depth format 0x{})",
                w,
                h,
                Integer.toHexString(format));
        }
        return true;
    }

    /** The main framebuffer's depth attachment point: depth-stencil if it has one, else depth. */
    private static int attachedDepth() {
        final int ds = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_STENCIL_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        GL11.glGetError(); // an attachment query on a missing point may raise INVALID_ENUM / OPERATION
        return ds != GL11.GL_NONE ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT;
    }

    /** The internal format of the bound framebuffer's depth attachment, or 0. */
    private static int depthFormat(int attachment) {
        final int type = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            attachment,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        final int name = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_FRAMEBUFFER,
            attachment,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        if (name == 0) return 0;
        if (type == GL30.GL_RENDERBUFFER) {
            final int was = GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, name);
            final int f = GL30.glGetRenderbufferParameteri(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_INTERNAL_FORMAT);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, was);
            return f;
        }
        if (type == GL11.GL_TEXTURE) {
            final int was = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, name);
            final int f = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, was);
            return f;
        }
        return 0;
    }

    private static int depthFbo(int rb, int attachment) {
        final int wasRb = GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rb);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, format, width, height);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, wasRb);
        final int was = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        final int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attachment, GL30.GL_RENDERBUFFER, rb);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, was);
        return fbo;
    }

    private static boolean fail(String why) {
        failed = true;
        BeyondTheStars.LOG
            .warn("[BTS render] Angelica: no opaque-depth copy ({}); machine renders draw in pass 0", why);
        return false;
    }

    private static void release() {
        if (opaqueFbo != 0) GL30.glDeleteFramebuffers(opaqueFbo);
        if (savedFbo != 0) GL30.glDeleteFramebuffers(savedFbo);
        if (opaqueRb != 0) GL30.glDeleteRenderbuffers(opaqueRb);
        if (savedRb != 0) GL30.glDeleteRenderbuffers(savedRb);
        opaqueFbo = savedFbo = opaqueRb = savedRb = 0;
    }

    /** GT's resource reload: drop the buffers (made again on the next frame that needs them). */
    static void markReload() {
        release();
        failed = false;
        width = height = 0;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, OpaqueDepth.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError absent) {
            return false;
        }
    }
}
