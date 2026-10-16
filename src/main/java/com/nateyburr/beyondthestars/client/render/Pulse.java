package com.nateyburr.beyondthestars.client.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * B-251: how far anything we draw may pulse, so every render reads unrest the same way. An unstable object shows it
 * through its brightness, colour, turbulence and detail first; its size moves at most {@link #SIZE} of its radius, and
 * only at the worst state. Every wave here is smooth (sums of sines, no steps, no saw edges) and slow (periods of
 * seconds, not fractions of one), so nothing hops, squashes or strobes.
 * <p>
 * Amplitudes are scaled by a severity (0 calm .. 1 the worst the state gets), eased so mild unrest barely shows:
 * {@code amp(max, s) = max * s^2 (3 - 2 s)}.
 * <p>
 * {@link Phase}: a clock that integrates a rate. A pattern drawn as {@code time * rate} jumps whenever the rate
 * changes (by {@code time * delta}, and the shader clock runs to 3,600 s); a rate that ramps makes it race. Anything
 * whose speed follows a synced value (a spin, a boil rate, a warning's ramp) runs on a {@link Phase} instead.
 */
@SideOnly(Side.CLIENT)
public final class Pulse {

    /** The most a size may swing, a fraction of the radius (at severity 1). */
    public static final float SIZE = 0.04f;
    /** The most a whole object's brightness may swing (at severity 1); detail and colour may move more. */
    public static final float GLOW = 0.12f;
    /** The shortest period a whole-object wave may have, seconds. */
    public static final double MIN_PERIOD = 1.2;

    private Pulse() {}

    /** {@code x} as a severity: 0 at {@code calm} or below, 1 at {@code worst} or above. */
    public static float severity(float x, float calm, float worst) {
        return clamp01((x - calm) / (worst - calm));
    }

    /** The amplitude {@code max} scaled by an eased severity (smoothstep), so mild unrest barely moves. */
    public static float amp(float max, float severity) {
        final float s = clamp01(severity);
        return max * s * s * (3f - 2f * s);
    }

    /**
     * A smooth wave in -1..1 with a period of about {@code period} seconds ({@link #MIN_PERIOD} at least): two
     * incommensurate sines (the second 0.62 of the first's period), so it breathes without settling into a beat.
     */
    public static float wave(double seconds, double period, double phase) {
        final double p = Math.max(MIN_PERIOD, period);
        final double w = 2 * Math.PI / p;
        return (float) (0.6 * Math.sin(seconds * w + phase) + 0.4 * Math.sin(seconds * w / 0.62 + 1.3 + phase));
    }

    /** One sine in -1..1 of period {@code period} seconds ({@link #MIN_PERIOD} at least): a slow, even beat. */
    public static float beat(double seconds, double period, double phase) {
        return (float) Math.sin(seconds * 2 * Math.PI / Math.max(MIN_PERIOD, period) + phase);
    }

    /** A smooth hump 0..1..0 over {@code f} in 0..1 (sin^2): a repeating cue that eases in and out, never snaps. */
    public static float hump(float f) {
        final double s = Math.sin(Math.PI * clamp01(f));
        return (float) (s * s);
    }

    public static float clamp01(float x) {
        return x < 0f ? 0f : x > 1f ? 1f : x;
    }

    /**
     * A clock that integrates a rate (units per tick) over world ticks, so a changing rate speeds the pattern up or
     * slows it down instead of jumping it. The first call (and the first after a gap of more than two seconds, or a
     * step back in time) starts from {@code seed}, the value {@code time * rate} gave before, so a steady rate draws
     * exactly as it did. It wraps at {@code wrap}. Render thread only; a repeat at the same {@code now} (a second
     * pass) returns the same value.
     */
    public static final class Phase {

        private double value, last = Double.NaN;

        public double at(double now, double ratePerTick, double seed, double wrap) {
            if (Double.isNaN(last) || now < last || now - last > 40) {
                value = seed;
            } else {
                value += (now - last) * ratePerTick;
            }
            if (wrap > 0) value %= wrap;
            last = now;
            return value;
        }
    }
}
