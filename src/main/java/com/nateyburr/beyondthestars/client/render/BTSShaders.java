package com.nateyburr.beyondthestars.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.render.shader.ShaderHandle;
import gregtech.common.render.shader.ShaderRecipe;

/**
 * Renders v2 (§2.1, §4.2): every GLSL program the addon draws with, baked through GT's {@link ShaderRecipe} (so GT's
 * {@code ShaderProfile} picks the legacy {@code #version 120} or the Angelica core {@code #version 330} variant, and
 * falls back to legacy by itself), lazily on first use and again after every resource reload.
 * <p>
 * <b>Fallback.</b> A program that does not bake (or bakes invalid) is logged once and marked dead until the next
 * resource reload: {@link Program#get()} then returns null and the object draws its fixed-function (LOW) path. Meshes
 * built against a program's vertex format ({@link Program#mesh}) are built after the bake and deleted with it.
 * <p>
 * <b>Our sources</b> live in {@code assets/beyondthestars/shaders/} and are served with their {@code #include}s
 * expanded by {@link GlslSourcePack} ({@link #recipe}); a pack program (TecTech's star shader) is registered as is.
 * <p>
 * Dev switch {@code -Dbts.render.failShaders=true}: every program registered here reports dead, so the fallback path
 * of every object can be shot (render QA).
 */
@SideOnly(Side.CLIENT)
public final class BTSShaders {

    /** Dev only: every program acts as if it failed to bake. */
    public static final boolean FORCE_FAIL = Boolean.getBoolean("bts.render.failShaders");

    private static final List<Program> PROGRAMS = new ArrayList<>();

    private BTSShaders() {}

    /** A recipe for one of our programs ({@code shaders/<name>.vert[.330].glsl} + frag, includes expanded). */
    public static ShaderRecipe recipe(String name) {
        return ShaderRecipe.of(GlslSourcePack.DOMAIN, name);
    }

    /** Registers a program; {@code label} names it in the log. */
    public static Program program(String label, ShaderRecipe recipe) {
        final Program p = new Program(label, recipe);
        synchronized (PROGRAMS) {
            PROGRAMS.add(p);
        }
        return p;
    }

    /** GT's resource-reload hook (ClientProxy): release every program and mesh; each re-bakes on its next use. */
    public static void markReload() {
        synchronized (PROGRAMS) {
            for (Program p : PROGRAMS) p.markReload();
        }
        NoiseTex.markReload();
    }

    /** Every registered program (the self test walks them). */
    public static List<Program> all() {
        synchronized (PROGRAMS) {
            return new ArrayList<>(PROGRAMS);
        }
    }

    /** One program: its recipe, its baked handle (or dead), and the meshes built against it. */
    public static final class Program {

        private final String label;
        private final ShaderRecipe recipe;
        private final List<Mesh> meshes = new ArrayList<>();
        private ShaderHandle handle;
        private boolean dirty = true;
        private boolean dead;
        private boolean warned;

        private Program(String label, ShaderRecipe recipe) {
            this.label = label;
            this.recipe = recipe;
        }

        public String label() {
            return label;
        }

        /** The baked handle, or null when it does not bake (then draw the fixed-function path). */
        public ShaderHandle get() {
            if (dirty) bake();
            return dead ? null : handle;
        }

        /** True when {@link #get()} would return a handle (bakes on first call). */
        public boolean usable() {
            return get() != null;
        }

        /** True when the last bake failed (without baking). */
        public boolean isDead() {
            return dead && !dirty;
        }

        /** A mesh built against this program's vertex format, after each bake, by {@code builder}. */
        public Mesh mesh(Function<ShaderHandle, IVertexArrayObject> builder) {
            final Mesh m = new Mesh(this, builder);
            meshes.add(m);
            return m;
        }

        void markReload() {
            release();
            dirty = true;
            dead = false;
            warned = false;
        }

        private void bake() {
            dirty = false;
            release();
            if (FORCE_FAIL) {
                dead = true;
                warnOnce("switched off by -Dbts.render.failShaders=true");
                return;
            }
            // cheap when the pack is already in the resource manager; a pack program never asks it
            GlslSourcePack.ensureRegistered();
            try {
                handle = recipe.bake();
                dead = handle == null || !handle.isValid();
                if (dead) warnOnce("did not bake (see the GT shader log)");
            } catch (RuntimeException | LinkageError bad) {
                release();
                dead = true;
                warnOnce(bad.toString());
            }
        }

        private void release() {
            for (Mesh m : meshes) m.release();
            if (handle != null) {
                handle.release();
                handle = null;
            }
        }

        private void warnOnce(String why) {
            if (warned) return;
            warned = true;
            BeyondTheStars.LOG
                .warn("[BTS render] shader {} is off, its objects draw the fixed-function path: {}", label, why);
        }
    }

    /** A mesh owned by a {@link Program}: built on first use after each bake, deleted on release. */
    public static final class Mesh {

        private final Program owner;
        private final Function<ShaderHandle, IVertexArrayObject> builder;
        private IVertexArrayObject vao;
        private boolean failed;

        private Mesh(Program owner, Function<ShaderHandle, IVertexArrayObject> builder) {
            this.owner = owner;
            this.builder = builder;
        }

        /** The mesh, or null if its program is dead or the build failed. */
        public IVertexArrayObject get() {
            final ShaderHandle h = owner.get();
            if (h == null || failed) return null;
            if (vao == null) {
                try {
                    vao = builder.apply(h);
                } catch (RuntimeException bad) {
                    failed = true;
                    BeyondTheStars.LOG
                        .warn("[BTS render] a mesh for {} did not build: {}", owner.label, bad.toString());
                    return null;
                }
            }
            return vao;
        }

        void release() {
            failed = false;
            if (vao != null) {
                vao.delete();
                vao = null;
            }
        }
    }
}
