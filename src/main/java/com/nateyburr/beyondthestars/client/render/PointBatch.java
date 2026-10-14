package com.nateyburr.beyondthestars.client.render;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memAlloc;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memFree;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderProfile;
import gregtech.common.render.shader.ShaderRecipe;
import gregtech.common.render.shader.Uniform;
import gregtech.common.render.shader.VertexAttribute;

/**
 * Renders v2 (§9.2): a static cloud of point stars (a starfield, a galaxy's 20,000 stars, a companion), built once
 * into one vertex buffer and drawn as {@code GL_POINTS} with the {@code bts_points} program: each point's size in
 * blocks becomes {@code gl_PointSize} by its distance (at least {@code minPx}; a point drawn larger than it is fades
 * by the area it gained, so far stars do not swell), a round soft sprite, its own colour, additive.
 * <p>
 * The model matrix places and turns the whole cloud; per-star motion belongs in a program built on this one. The
 * fixed-function path ({@link #drawFixed}) draws the same points as camera-facing quads through the Tessellator (only
 * a subset: every {@code stride}-th point), the LOW look of today's galaxy dots.
 * <p>
 * Each point is a position and a UV pair: (size in blocks, colour packed as {@code r * 65536 + g * 256 + b}); a float
 * holds the packed colour exactly.
 */
@SideOnly(Side.CLIENT)
public final class PointBatch {

    private static final ShaderRecipe RECIPE = BTSShaders.recipe("bts_points")
        .required("u_Points", "u_Tint")
        .modelUniform("u_ModelMatrix")
        .attribute("a_Position", VertexAttribute.POSITION)
        .attribute("a_UV", VertexAttribute.UV);
    private static final Uniform U_POINTS = RECIPE.uniform("u_Points");
    private static final Uniform U_TINT = RECIPE.uniform("u_Tint");

    /** The point program (shared by every batch). */
    public static final BTSShaders.Program PROGRAM = BTSShaders.program("bts_points", RECIPE);

    /** GL_VERTEX_PROGRAM_POINT_SIZE (= GL_PROGRAM_POINT_SIZE) and GL_POINT_SPRITE (legacy only). */
    private static final int PROGRAM_POINT_SIZE = 0x8642, POINT_SPRITE = 0x8861;

    private final float[] xyz;
    private final float[] size;
    private final int[] rgb;
    private final int count;
    private final BTSShaders.Mesh mesh;

    private PointBatch(float[] xyz, float[] size, int[] rgb, int count) {
        this.xyz = xyz;
        this.size = size;
        this.rgb = rgb;
        this.count = count;
        this.mesh = PROGRAM.mesh(this::build);
    }

    /** Collects points for one batch. */
    public static Builder builder(int capacity) {
        return new Builder(capacity);
    }

    public int count() {
        return count;
    }

    /**
     * Draws the batch with {@code model} (camera-relative) and a tint (rgb multiplier, a brightness), the points at
     * least {@code minPx} and at most {@code maxPx} pixels across. Expects an additive state ({@link Layers#emissive}).
     * Returns false (drawing nothing) when the program is off: draw {@link #drawFixed} instead.
     */
    public boolean draw(Matrix4fc model, float r, float g, float b, float brightness, float minPx, float maxPx) {
        final ShaderHandle h = PROGRAM.get();
        final IVertexArrayObject vao = mesh.get();
        if (h == null || vao == null) return false;
        final boolean legacy = h.profile() == ShaderProfile.LEGACY;
        GL11.glEnable(PROGRAM_POINT_SIZE);
        if (legacy) GL11.glEnable(POINT_SPRITE);
        h.use();
        h.uploadModel(model);
        GL20.glUniform3f(h.loc(U_POINTS), (float) Lod.pixelsPerBlockAtUnit(), minPx, maxPx);
        GL20.glUniform4f(h.loc(U_TINT), r, g, b, brightness);
        vao.bind();
        vao.draw();
        vao.unbind();
        ShaderProgram.clear();
        if (legacy) GL11.glDisable(POINT_SPRITE);
        GL11.glDisable(PROGRAM_POINT_SIZE);
        return true;
    }

    /**
     * The fixed-function look: every {@code stride}-th point as a camera-facing square of half its size, its colour x
     * {@code alpha}, offset by the camera-relative {@code (ox, oy, oz)} (no rotation). Expects an additive untextured
     * state ({@link Layers#additive}).
     */
    public void drawFixed(Tessellator t, double ox, double oy, double oz, int stride, float alpha) {
        final Billboard bb = Billboard.at(ox, oy, oz);
        if (bb == null) return;
        t.startDrawingQuads();
        for (int i = 0; i < count; i += Math.max(1, stride)) {
            final int c = rgb[i];
            FixedShapes.billboardQuad(
                t,
                ox + xyz[i * 3],
                oy + xyz[i * 3 + 1],
                oz + xyz[i * 3 + 2],
                bb.u,
                bb.v,
                size[i] * 0.5,
                ((c >> 16) & 255) / 255f,
                ((c >> 8) & 255) / 255f,
                (c & 255) / 255f,
                alpha);
        }
        t.draw();
    }

    private IVertexArrayObject build(ShaderHandle h) {
        final int stride = h.vertexFormat()
            .getVertexSize();
        if (stride != 20)
            throw new IllegalStateException("bts_points: expected position + UV (20 bytes), got " + stride);
        final ByteBuffer buf = memAlloc(count * stride);
        try {
            for (int i = 0; i < count; i++) {
                final int base = i * stride;
                buf.putFloat(base, xyz[i * 3]);
                buf.putFloat(base + 4, xyz[i * 3 + 1]);
                buf.putFloat(base + 8, xyz[i * 3 + 2]);
                buf.putFloat(base + 12, size[i]);
                buf.putFloat(base + 16, rgb[i] & 0xFFFFFF);
            }
            buf.position(0)
                .limit(count * stride);
            return VertexBufferType.IMMUTABLE.allocate(h.vertexFormat(), GL11.GL_POINTS, buf, count);
        } finally {
            memFree(buf);
        }
    }

    /** Collects the points of a batch. */
    public static final class Builder {

        private final float[] xyz;
        private final float[] size;
        private final int[] rgb;
        private int n;

        private Builder(int capacity) {
            xyz = new float[capacity * 3];
            size = new float[capacity];
            rgb = new int[capacity];
        }

        /** One point at {@code (x, y, z)} (model space), {@code sizeBlocks} across, colour 0xRRGGBB. */
        public Builder add(float x, float y, float z, float sizeBlocks, int colour) {
            if (n == size.length) throw new IllegalStateException("PointBatch full at " + n);
            xyz[n * 3] = x;
            xyz[n * 3 + 1] = y;
            xyz[n * 3 + 2] = z;
            size[n] = sizeBlocks;
            rgb[n] = colour & 0xFFFFFF;
            n++;
            return this;
        }

        public PointBatch build() {
            return new PointBatch(xyz, size, rgb, n);
        }
    }
}
