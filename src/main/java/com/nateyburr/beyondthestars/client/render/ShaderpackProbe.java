package com.nateyburr.beyondthestars.client.render;

import java.lang.reflect.Method;
import java.util.Optional;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.4, §4.3): is an Angelica (Iris) shaderpack in use right now? Reflective and optional: no Angelica,
 * a renamed class or any {@link LinkageError} reads as "no". Only the framebuffer-copy lensing asks (a shaderpack owns
 * the framebuffers); everything else draws the same under a pack. Checked at most once a second.
 */
@SideOnly(Side.CLIENT)
public final class ShaderpackProbe {

    private static final long RECHECK_NS = 1_000_000_000L;

    private static boolean resolved;
    /** Iris API v0: {@code IrisApi.getInstance().isShaderPackInUse()}. */
    private static Object api;
    private static Method inUse;
    /** Older Iris / Angelica: {@code Iris.getCurrentPack()} (an Optional). */
    private static Method currentPack;

    private static long checkedAt;
    private static boolean active;

    private ShaderpackProbe() {}

    /** True while a shaderpack is in use (cached for a second). */
    public static boolean active() {
        final long now = System.nanoTime();
        if (resolved && now - checkedAt < RECHECK_NS) return active;
        checkedAt = now;
        if (!resolved) resolve();
        active = query();
        return active;
    }

    private static void resolve() {
        resolved = true;
        try {
            final Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = c.getMethod("getInstance")
                .invoke(null);
            inUse = c.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            api = null;
            inUse = null;
        }
        if (inUse == null) {
            try {
                currentPack = Class.forName("net.coderbot.iris.Iris")
                    .getMethod("getCurrentPack");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
                currentPack = null;
            }
        }
        BeyondTheStars.LOG.info(
            "[BTS render] shaderpack probe: {}",
            inUse != null ? "Iris API" : currentPack != null ? "Iris.getCurrentPack" : "none (no Angelica / Iris)");
    }

    private static boolean query() {
        try {
            if (inUse != null) return Boolean.TRUE.equals(inUse.invoke(api));
            if (currentPack != null) {
                final Object pack = currentPack.invoke(null);
                return pack instanceof Optional && ((Optional<?>) pack).isPresent();
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException bad) {
            inUse = null;
            currentPack = null;
        }
        return false;
    }
}
