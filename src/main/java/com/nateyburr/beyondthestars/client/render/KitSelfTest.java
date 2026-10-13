package com.nateyburr.beyondthestars.client.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.Random;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderProfile;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 WP-0, DEV ONLY ({@code -Dbts.render.selftest=true}): proves the render kit in a real client.
 * <ol>
 * <li>Once: compiles and links BOTH profiles of every kit program straight from the served (include-expanded)
 * sources, and logs each result; on a compatibility context (the dev rig) the {@code #version 330 core} variants
 * compile too, so a syntax or include error in either profile shows here. Writes the noise texture and the blackbody
 * LUT as PNGs ({@code screenshots/rendertest/kit-noise.png}, {@code kit-lut.png}).</li>
 * <li>Every frame: a panel ten blocks in front of the camera. Top row, left to right: the {@code bts_selftest}
 * program as GT baked it (the preferred profile), the same program's core variant linked by hand and fed its
 * {@code u_MVP} (so the 330 source really draws), a {@link GlowSprite} and a {@link PointBatch} starfield. Bottom
 * row: the noise texture and the LUT through fixed-function texturing (the uploaded texels as they are), then the
 * fixed-function glow and points (the LOW path). With {@code -Dbts.render.failShaders=true} the top row's glow and
 * points draw their fixed-function fallback instead.</li>
 * </ol>
 */
@SideOnly(Side.CLIENT)
public final class KitSelfTest {

    public static final boolean ENABLED = Boolean.getBoolean("bts.render.selftest");

    private static final ShaderRecipe RECIPE = BTSShaders.recipe("bts_selftest")
        .required("u_Time")
        .sampler("u_Noise", NoiseTex.NOISE_UNIT)
        .sampler("u_Blackbody", NoiseTex.LUT_UNIT)
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_TIME = RECIPE.uniform("u_Time");
    private static final BTSShaders.Program PROGRAM = BTSShaders.program("bts_selftest", RECIPE);
    private static final BTSShaders.Mesh QUAD = PROGRAM.mesh(Meshes::quad);

    /** The kit programs whose two profiles the self test compiles. */
    private static final String[] PROGRAMS = { "bts_selftest", "bts_glow", "bts_points", "bts_blackhole" };

    private static PointBatch stars;
    private static boolean checked;
    private static int coreProgram;
    private static boolean coreTried;

    private final Matrix4f model = new Matrix4f();
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);

    private KitSelfTest() {}

    public static void register() {
        if (!ENABLED) return;
        MinecraftForge.EVENT_BUS.register(new KitSelfTest());
        BeyondTheStars.LOG.warn("[BTS render kit] -Dbts.render.selftest=true: drawing the kit test panel");
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityLivingBase eye = mc.renderViewEntity;
        if (eye == null || mc.theWorld == null) return;
        if (!checked) {
            checked = true;
            compileCheck();
            dumpTextures(mc);
        }
        final float yaw = (float) Math.toRadians(eye.rotationYaw), pitch = (float) Math.toRadians(eye.rotationPitch);
        final double[] look = { -Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch) };
        final double[] right = { -Math.cos(yaw), 0, -Math.sin(yaw) };
        final double[] up = { right[1] * look[2] - right[2] * look[1], right[2] * look[0] - right[0] * look[2],
            right[0] * look[1] - right[1] * look[0] };
        final float time = RenderClock.seconds(mc.theWorld, event.partialTicks);

        final GlSave save = GlSave.capture();
        try {
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            // top row
            final double[] a = at(look, right, up, -7.5, 2.6);
            final double[] b = at(look, right, up, -2.6, 2.6);
            final double[] c = at(look, right, up, 2.4, 2.6);
            final double[] d = at(look, right, up, 7.2, 2.6);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            drawSelfTest(a, right, up, look, time, false);
            drawSelfTest(b, right, up, look, time, true);
            final GlSave em = Layers.emissive();
            try {
                if (!GlowSprite.draw(c[0], c[1], c[2], 2.2, 1.0f, 0.62f, 0.25f, 1.6f, 0.18f, 0.45f)) {
                    fixedGlow(c);
                }
                if (!stars()
                    .draw(model.translation((float) d[0], (float) d[1], (float) d[2]), 1, 1, 1, 1.4f, 1.5f, 24f)) {
                    fixedStars(d);
                }
            } finally {
                em.restore();
            }
            // bottom row: the textures as uploaded, then the LOW path
            texturedQuad(at(look, right, up, -7.5, -2.4), right, up, NoiseTex.noise(), 2.2);
            texturedQuad(at(look, right, up, -2.6, -2.4), right, up, NoiseTex.blackbodyLut(), 2.2);
            fixedGlow(at(look, right, up, 2.4, -2.4));
            fixedStars(at(look, right, up, 7.2, -2.4));
        } finally {
            save.restore();
        }
    }

    private static double[] at(double[] look, double[] right, double[] up, double x, double y) {
        return new double[] { look[0] * 10 + right[0] * x + up[0] * y, look[1] * 10 + right[1] * x + up[1] * y,
            look[2] * 10 + right[2] * x + up[2] * y };
    }

    private void drawSelfTest(double[] p, double[] right, double[] up, double[] look, float time, boolean core) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject quad = QUAD.get();
        if (h == null || quad == null) return;
        final float s = 2.2f;
        model.set(
            (float) right[0] * s,
            (float) right[1] * s,
            (float) right[2] * s,
            0,
            (float) up[0] * s,
            (float) up[1] * s,
            (float) up[2] * s,
            0,
            (float) look[0],
            (float) look[1],
            (float) look[2],
            0,
            (float) p[0],
            (float) p[1],
            (float) p[2],
            1);
        final int[] was = NoiseTex.bind();
        if (!core) {
            h.use();
            h.uploadModel(model);
            GL20.glUniform1f(h.loc(U_TIME), time);
        } else {
            final int prog = coreProgram(h);
            if (prog == 0) {
                NoiseTex.unbind(was);
                return;
            }
            GL20.glUseProgram(prog);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog, "u_Noise"), NoiseTex.NOISE_UNIT);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog, "u_Blackbody"), NoiseTex.LUT_UNIT);
            GL20.glUniform1f(GL20.glGetUniformLocation(prog, "u_Time"), time);
            final Matrix4f proj = new Matrix4f(), view = new Matrix4f();
            matrix.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrix);
            proj.set(matrix);
            matrix.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
            view.set(matrix);
            final Matrix4f mvp = proj.mul(view)
                .mul(model);
            matrix.clear();
            mvp.get(matrix);
            GL20.glUniformMatrix4(GL20.glGetUniformLocation(prog, "u_MVP"), false, matrix);
        }
        quad.bind();
        quad.draw();
        quad.unbind();
        ShaderProgram.clear();
        NoiseTex.unbind(was);
    }

    /**
     * The core variant of {@code bts_selftest}, linked by hand with its attributes at the baked program's locations
     * (so the same mesh feeds it); 0 if it does not compile or link (logged once).
     */
    private static int coreProgram(ShaderHandle baked) {
        if (coreTried) return coreProgram;
        coreTried = true;
        if (baked.profile() == ShaderProfile.ANGELICA_CORE) {
            // already the core variant: the baked handle is the proof
            return 0;
        }
        final int prog = link("bts_selftest", ".330", baked.program());
        coreProgram = prog;
        BeyondTheStars.LOG.info("[BTS render kit] hand-linked core bts_selftest: {}", prog != 0 ? "drawing" : "FAILED");
        return prog;
    }

    /** Compiles and links {@code name}'s variant ({@code ""} legacy, {@code ".330"} core); 0 on failure (logged). */
    private static int link(String name, String suffix, int bindLike) {
        final String vs = GlslSourcePack.expanded("shaders/" + name + ".vert" + suffix + ".glsl");
        final String fs = GlslSourcePack.expanded("shaders/" + name + ".frag" + suffix + ".glsl");
        if (vs == null || fs == null) {
            BeyondTheStars.LOG.error("[BTS render kit] {}{}: source missing", name, suffix);
            return 0;
        }
        final int v = compile(GL20.GL_VERTEX_SHADER, vs, name + ".vert" + suffix);
        final int f = compile(GL20.GL_FRAGMENT_SHADER, fs, name + ".frag" + suffix);
        if (v == 0 || f == 0) {
            if (v != 0) GL20.glDeleteShader(v);
            if (f != 0) GL20.glDeleteShader(f);
            return 0;
        }
        final int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        if (bindLike != 0) {
            for (String attr : new String[] { "a_Position", "a_UV" }) {
                final int loc = GL20.glGetAttribLocation(bindLike, attr);
                if (loc >= 0) GL20.glBindAttribLocation(p, loc, attr);
            }
        }
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            BeyondTheStars.LOG
                .error("[BTS render kit] {}{} does not link: {}", name, suffix, GL20.glGetProgramInfoLog(p, 4096));
            GL20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private static int compile(int type, String src, String label) {
        final int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            BeyondTheStars.LOG
                .error("[BTS render kit] {} does not compile: {}", label, GL20.glGetShaderInfoLog(s, 4096));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    /** Both profiles of every kit program, compiled and linked by hand; then every registered program's bake. */
    private static void compileCheck() {
        BeyondTheStars.LOG.info(
            "[BTS render kit] self test: preferred profile {}, GL {}, GLSL {}",
            ShaderProfile.preferred(),
            GL11.glGetString(GL11.GL_VERSION),
            GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        int failed = 0;
        for (String name : PROGRAMS) {
            for (String suffix : new String[] { "", ".330" }) {
                final int p = link(name, suffix, 0);
                BeyondTheStars.LOG.info(
                    "[BTS render kit] {} [{}]: {}",
                    name,
                    suffix.isEmpty() ? "legacy 120" : "core 330",
                    p != 0 ? "compiles and links" : "FAILED");
                if (p != 0) GL20.glDeleteProgram(p);
                else failed++;
            }
        }
        // load the kit's own program holders, so their programs are registered before the list is walked
        final BTSShaders.Program[] kit = { GlowSprite.PROGRAM, PointBatch.PROGRAM, PROGRAM };
        BeyondTheStars.LOG.info(
            "[BTS render kit] {} kit programs, {} registered",
            kit.length,
            BTSShaders.all()
                .size());
        for (BTSShaders.Program p : BTSShaders.all()) {
            BeyondTheStars.LOG
                .info("[BTS render kit] bake {}: {}", p.label(), p.usable() ? "OK" : "OFF (fixed-function path)");
        }
        BeyondTheStars.LOG.info("[BTS render kit] self test compile check: {} failure(s)", failed);
    }

    private static void dumpTextures(Minecraft mc) {
        try {
            final File dir = new File(mc.mcDataDir, "screenshots/rendertest");
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            final byte[] n = NoiseTex.noiseTexels();
            final BufferedImage noise = new BufferedImage(
                NoiseTex.NOISE_SIZE,
                NoiseTex.NOISE_SIZE,
                BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < NoiseTex.NOISE_SIZE * NoiseTex.NOISE_SIZE; i++) {
                noise.setRGB(
                    i % NoiseTex.NOISE_SIZE,
                    i / NoiseTex.NOISE_SIZE,
                    (n[i * 4] & 255) << 16 | (n[i * 4 + 1] & 255) << 8 | (n[i * 4 + 2] & 255));
            }
            ImageIO.write(noise, "png", new File(dir, "kit-noise.png"));
            final byte[] l = NoiseTex.lutTexels();
            final BufferedImage lut = new BufferedImage(NoiseTex.LUT_SIZE, 32, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < NoiseTex.LUT_SIZE; x++) {
                final int rgb = (l[x * 4] & 255) << 16 | (l[x * 4 + 1] & 255) << 8 | (l[x * 4 + 2] & 255);
                for (int y = 0; y < 32; y++) lut.setRGB(x, y, rgb);
            }
            ImageIO.write(lut, "png", new File(dir, "kit-lut.png"));
            BeyondTheStars.LOG.info(
                "[BTS render kit] wrote kit-noise.png and kit-lut.png; LUT 1000 K {} / 5772 K {} / 40000 K {}",
                String.format(Locale.ROOT, "%06X", Blackbody.packed(1000)),
                String.format(Locale.ROOT, "%06X", Blackbody.packed(5772)),
                String.format(Locale.ROOT, "%06X", Blackbody.packed(40000)));
        } catch (Exception bad) {
            BeyondTheStars.LOG.warn("[BTS render kit] could not write the texture dumps: {}", bad.toString());
        }
    }

    private static PointBatch stars() {
        if (stars == null) {
            final Random rnd = new Random(20261014L);
            final PointBatch.Builder b = PointBatch.builder(3000);
            for (int i = 0; i < 3000; i++) {
                // a flattened disc of stars, blue-white outside, warm inside
                final double r = 2.2 * Math.sqrt(rnd.nextDouble()), a = rnd.nextDouble() * Math.PI * 2;
                final double k = 3000 + 20000 * (r / 2.2) * rnd.nextDouble();
                b.add(
                    (float) (r * Math.cos(a)),
                    (float) (rnd.nextGaussian() * 0.15),
                    (float) (r * Math.sin(a)),
                    (float) (0.02 + 0.08 * Math.pow(rnd.nextDouble(), 6)),
                    Blackbody.packed(k));
            }
            stars = b.build();
        }
        return stars;
    }

    private static void fixedGlow(double[] p) {
        final GlSave s = Layers.additive();
        try {
            GlowSprite.drawFixed(Tessellator.instance, p[0], p[1], p[2], 2.2, 1.0f, 0.62f, 0.25f, 0.6f);
        } finally {
            s.restore();
        }
    }

    private static void fixedStars(double[] p) {
        final GlSave s = Layers.additive();
        try {
            stars().drawFixed(Tessellator.instance, p[0], p[1], p[2], 10, 0.9f);
        } finally {
            s.restore();
        }
    }

    private static void texturedQuad(double[] p, double[] right, double[] up, int texture, double s) {
        final GlSave save = GlSave.capture();
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            final int was = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            final Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setColorRGBA_F(1, 1, 1, 1);
            t.addVertexWithUV(
                p[0] - (right[0] + up[0]) * s,
                p[1] - (right[1] + up[1]) * s,
                p[2] - (right[2] + up[2]) * s,
                0,
                1);
            t.addVertexWithUV(
                p[0] + (right[0] - up[0]) * s,
                p[1] + (right[1] - up[1]) * s,
                p[2] + (right[2] - up[2]) * s,
                1,
                1);
            t.addVertexWithUV(
                p[0] + (right[0] + up[0]) * s,
                p[1] + (right[1] + up[1]) * s,
                p[2] + (right[2] + up[2]) * s,
                1,
                0);
            t.addVertexWithUV(
                p[0] - (right[0] - up[0]) * s,
                p[1] - (right[1] - up[1]) * s,
                p[2] - (right[2] - up[2]) * s,
                0,
                0);
            t.draw();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, was);
        } finally {
            save.restore();
        }
    }
}
