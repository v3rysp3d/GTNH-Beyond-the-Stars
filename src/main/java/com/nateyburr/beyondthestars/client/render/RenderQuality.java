package com.nateyburr.beyondthestars.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.launchwrapper.Launch;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§3, §4): which detail an object draws at this frame: the quality setting x its projected size (LOD) x
 * the HIGH budget x whether its programs bake x the GL context.
 * <ol>
 * <li>{@code renderQuality=low}, a context below GL 2.1 or without vertex buffers, a program of the object that does
 * not bake, or a GL error caught after the object's draw (dev only): {@link Lod.Tier#FIXED}, today's fixed-function
 * render.</li>
 * <li>Otherwise the LOD tier by projected radius ({@link Lod}); {@code medium} never draws L0.</li>
 * <li>Only the {@code renderMaxHighObjects} objects with the largest projected radius (last frame's ranking) may draw
 * L0; the others drop to L1 (§3.1: a quasar and an Eye in view stay inside the scene budget).</li>
 * </ol>
 * Dev switch {@code -Dbts.rendertest.quality=low|medium|high} overrides the config (the render QA harness shoots the
 * three).
 */
@SideOnly(Side.CLIENT)
public final class RenderQuality {

    /** The setting. */
    public enum Quality {

        LOW,
        MEDIUM,
        HIGH;

        static Quality parse(String s, Quality fallback) {
            if (s == null) return fallback;
            switch (s.trim()
                .toLowerCase(Locale.ROOT)) {
                case "low":
                    return LOW;
                case "medium":
                    return MEDIUM;
                case "high":
                    return HIGH;
                default:
                    return fallback;
            }
        }
    }

    /** The harness override, or null. */
    private static final Quality OVERRIDE = Quality.parse(System.getProperty("bts.rendertest.quality"), null);
    /** Dev environment or {@code -Dbts.render.glcheck=true}: check GL errors after each object's draw once a second. */
    private static final boolean GL_CHECK = Boolean.getBoolean("bts.render.glcheck") || isDevEnvironment();

    private static Boolean glCapable;

    /** Per object (a render tile): last tier, projected size, the frame it was seen, GL check state. */
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    /** The keys allowed L0 this frame (from last frame's sizes). */
    private static final Set<Object> HIGH = Collections.newSetFromMap(new WeakHashMap<>());

    private static final class State {

        /** The LOD tier by size (hysteresis input). */
        Lod.Tier tier;
        /** The tier actually drawn (after quality, bake and budget). */
        Lod.Tier drawn;
        double px;
        long frame = -1;
        long checkedAt;
        boolean glFailed;
    }

    private RenderQuality() {}

    /** The effective setting (harness override, else the config). */
    public static Quality quality() {
        return OVERRIDE != null ? OVERRIDE : Quality.parse(Config.renderQuality, Quality.HIGH);
    }

    /**
     * The tier {@code key} (one per drawn object, e.g. its tile) draws at this frame. {@code radius} is the object's
     * bounding radius in blocks, {@code distance} its centre's distance from the camera; every program it would draw
     * with must bake, else {@link Lod.Tier#FIXED}.
     */
    public static Lod.Tier tier(Object key, double radius, double distance, BTSShaders.Program... programs) {
        final State s = state(key);
        final double px = Lod.projectedRadius(radius, distance);
        s.px = px;
        s.frame = RenderFrame.frame();
        Lod.Tier t = Lod.tierFor(px, s.tier);
        s.tier = t;
        s.drawn = effective(key, s, t, programs);
        return s.drawn;
    }

    private static Lod.Tier effective(Object key, State s, Lod.Tier lod, BTSShaders.Program[] programs) {
        if (quality() == Quality.LOW || !glCapable() || s.glFailed) return Lod.Tier.FIXED;
        for (BTSShaders.Program p : programs) {
            if (p != null && !p.usable()) return Lod.Tier.FIXED;
        }
        if (lod == Lod.Tier.L0 && (quality() == Quality.MEDIUM || !HIGH.contains(key))) return Lod.Tier.L1;
        return lod;
    }

    /** {@code renderBackgroundLensing}, at HIGH, with no shaderpack in use (§4.3). */
    public static boolean backgroundLensing(Lod.Tier tier) {
        return tier == Lod.Tier.L0 && Config.renderBackgroundLensing && !ShaderpackProbe.active();
    }

    /** {@code renderParticles}. */
    public static boolean particles() {
        return Config.renderParticles;
    }

    /** GL 2.1 with vertex buffer objects (§4.2, 3); checked once. */
    public static boolean glCapable() {
        if (glCapable == null) {
            boolean ok;
            try {
                final ContextCapabilities caps = GLContext.getCapabilities();
                ok = caps.OpenGL21 && (caps.OpenGL15 || caps.GL_ARB_vertex_buffer_object);
            } catch (RuntimeException | LinkageError bad) {
                ok = false;
            }
            glCapable = ok;
            if (!ok) BeyondTheStars.LOG.warn("[BTS render] GL 2.1 with vertex buffers not available: low quality");
        }
        return glCapable;
    }

    // ============================================================================ GL error check

    /**
     * Before an object's shader draw: in a dev environment, once a second per object, clears the GL error flag so
     * {@link #glCheckEnd} sees only this draw's errors. Returns whether this frame checks.
     */
    public static boolean glCheckBegin(Object key) {
        if (!GL_CHECK) return false;
        final State s = state(key);
        final long now = System.nanoTime();
        if (now - s.checkedAt < 1_000_000_000L) return false;
        s.checkedAt = now;
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // drain errors left by earlier draws
        }
        return true;
    }

    /** After the draw: a GL error drops {@code key} to the fixed-function path for the session and logs once. */
    public static void glCheckEnd(Object key, boolean checking, String what) {
        if (!checking) return;
        final int err = GL11.glGetError();
        if (err == GL11.GL_NO_ERROR) return;
        final State s = state(key);
        if (!s.glFailed) {
            s.glFailed = true;
            BeyondTheStars.LOG.warn(
                "[BTS render] GL error 0x{} drawing {}: it draws the fixed-function path now",
                Integer.toHexString(err),
                what);
        }
    }

    // ============================================================================== frames

    /** The ranking for the HIGH budget, from the sizes reported last frame. */
    static void onFrameStart() {
        HIGH.clear();
        final int max = Math.max(0, Config.renderMaxHighObjects);
        if (max == 0 || STATES.isEmpty()) return;
        final long last = RenderFrame.frame() - 1;
        final List<Map.Entry<Object, State>> seen = new ArrayList<>();
        for (Map.Entry<Object, State> e : STATES.entrySet()) {
            if (e.getValue().frame >= last) seen.add(e);
        }
        seen.sort((a, b) -> Double.compare(b.getValue().px, a.getValue().px));
        for (int i = 0; i < seen.size() && i < max; i++) HIGH.add(
            seen.get(i)
                .getKey());
        // an object seen for the first time this frame is not in the ranking yet: it starts one tier down
    }

    /** Debug line for the harness: each object's last tier and size. */
    public static String describe() {
        final StringBuilder b = new StringBuilder("quality ").append(quality());
        final Set<String> parts = new HashSet<>();
        for (Map.Entry<Object, State> e : STATES.entrySet()) {
            final State s = e.getValue();
            if (s.frame < RenderFrame.frame() - 2) continue;
            parts.add(
                String.format(
                    Locale.ROOT,
                    "%s %.0fpx lod %s draws %s%s%s",
                    e.getKey()
                        .getClass()
                        .getSimpleName(),
                    Math.min(s.px, 99999),
                    s.tier,
                    s.drawn,
                    HIGH.contains(e.getKey()) ? " (high)" : "",
                    s.glFailed ? " (GL failed)" : ""));
        }
        for (String p : parts) b.append("; ")
            .append(p);
        return b.toString();
    }

    private static State state(Object key) {
        State s = STATES.get(key);
        if (s == null) {
            s = new State();
            STATES.put(key, s);
        }
        return s;
    }

    private static boolean isDevEnvironment() {
        try {
            final Object dev = Launch.blackboard == null ? null : Launch.blackboard.get("fml.deobfuscatedEnvironment");
            return Boolean.TRUE.equals(dev);
        } catch (RuntimeException | LinkageError bad) {
            return false;
        }
    }
}
