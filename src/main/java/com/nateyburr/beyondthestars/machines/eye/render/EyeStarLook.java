package com.nateyburr.beyondthestars.machines.eye.render;

import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.star.CoreCollapse;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;

/**
 * D-49 my answer 3: what the Eye's centre render shows (the object, its size, colour and motion), computed on the
 * server from the star runtime once per field second and pushed to the {@link TileEyeStar} only when it moved. Pure
 * data: no world, no GL.
 * <p>
 * The colour goes through the Forge of the Gods star shader ({@code tectech:star}): it keeps the luminance of
 * TecTech's star textures and replaces their hue with {@link #rgb}, then raises it to {@code 1 / gamma}. So a hot
 * star has a high gamma (bright), and the T5 black dwarf cools by its gamma falling below 1 (dark), not by its RGB.
 * <p>
 * Sizes are in blocks (the sphere's radius). The nearest block of {@code eye_core} is 20.9 blocks from the centre, so
 * the largest object (Arcturus swelling in the Core Collapse: 17, with its wobble and outer shell about 19.4) still
 * clears it; the voxel mockup's star is 15. Only the supernova flash (a translucent shell) runs out past the bands.
 */
public final class EyeStarLook {

    /** No object (the tile is removed). */
    public static final byte NONE = 0;
    /** T1-T3 live star. */
    public static final byte STAR = 1;
    /** T1-T3 igniting: a dim red protostar swelling toward the star. */
    public static final byte PROTOSTAR = 2;
    /** T4 neutron star: small, blue-white, spinning fast. */
    public static final byte NEUTRON = 3;
    /** T5 Sirius B batch: a white dwarf cooling to a black dwarf. */
    public static final byte DWARF = 4;
    /** T6 SGR 1806-20: a magnetar, violet, flaring on its event warnings. */
    public static final byte MAGNETAR = 5;
    /** Core Collapse phase 1: Arcturus fast-forwarded, swelling and reddening. */
    public static final byte SWELL = 6;
    /** Core Collapse phase 3: the proto-neutron star with the bounce's shock shell running out. */
    public static final byte BOUNCE = 7;
    /** A supernova (the Core Collapse failed at the bounce): a flash that runs out, then nothing. */
    public static final byte NOVA = 8;

    public final byte kind;
    public final float radius;
    /** 0xRRGGBB. */
    public final int rgb;
    public final float gamma;
    /** Degrees per tick of the layers' rotation (scaled per layer as the Forge of the Gods does). */
    public final float spin;
    /** Radius wobble amplitude, a fraction of the radius (instability, flares). */
    public final float pulse;

    public EyeStarLook(byte kind, float radius, int rgb, float gamma, float spin, float pulse) {
        this.kind = kind;
        this.radius = radius;
        this.rgb = rgb;
        this.gamma = gamma;
        this.spin = spin;
        this.pulse = pulse;
    }

    /** The flash after a supernova (the star is already out). */
    public static final EyeStarLook NOVA_FLASH = new EyeStarLook(NOVA, 15f, 0xFFF0DC, 5f, 1f, 0f);

    // ------------------------------------------------------------------------------ per star

    private static final int SOL_RGB = 0xFFD678;
    private static final int SIRIUS_A_RGB = 0xAAC8FF;
    private static final int ARCTURUS_RGB = 0xFF8232;
    private static final int PROTO_RGB = 0xE6401A;
    private static final int NEUTRON_RGB = 0xBEDCFF;
    private static final int MAGNETAR_RGB = 0xC85AFF;

    /** What the centre shows now, or null for nothing (dark, IDLE, no star). Server state. */
    public static EyeStarLook of(EyeStarRuntime rt) {
        final StarDef star = rt.star();
        if (star == null) return null;
        final FieldReadout r = rt.lastReadout();
        final float unrest = r != null && r.lit ? (float) Math.max(0, Math.min(1, (100 - r.stability) / 100)) : 0f;
        final float warn = r != null && r.lit && r.warn != 0 ? 1f : 0f;
        final EyeTransition tr = rt.transition();
        switch (rt.state()) {
            case IGNITING: {
                // P climbs from -80 to -20 (lights): the protostar swells and heats toward the star's look
                final float f = (float) Math.max(0, Math.min(1, (rt.ignitionP() + 80) / 60));
                final EyeStarLook lit = lit(star.n(), 0, 0);
                return new EyeStarLook(
                    PROTOSTAR,
                    lit.radius * (0.35f + 0.45f * f),
                    lerp(PROTO_RGB, lit.rgb, f),
                    0.8f + (lit.gamma - 0.8f) * f,
                    0.5f,
                    0.05f);
            }
            case LIT:
            case PENDING:
            case BATCH:
                break;
            default:
                return null;
        }
        if (tr instanceof CoreCollapse) {
            final CollapseOverlay co = ((CoreCollapse) tr).co;
            if (co.phase == 1) {
                return swell((float) Math.max(0, Math.min(1, co.tach / CollapseOverlay.TACH_BOUNCE)));
            }
            return bounce(unrest);
        }
        if (star.n() == 5) {
            return dwarf(rt.batchFraction());
        }
        return lit(star.n(), unrest, warn);
    }

    /** Core Collapse phase 1 at {@code f} (0..1) of the way to the bounce: Arcturus swelling and reddening. */
    public static EyeStarLook swell(float f) {
        return new EyeStarLook(
            SWELL,
            15f + 2f * f,
            lerp(ARCTURUS_RGB, 0xFF3C1E, f),
            2.6f + f,
            1f + 3f * f,
            0.04f + 0.06f * f);
    }

    /** Core Collapse phase 3: the proto-neutron star and the bounce's shell, with the field's unrest (0..1). */
    public static EyeStarLook bounce(float unrest) {
        return new EyeStarLook(BOUNCE, 2.6f, 0xC8E1FF, 4.5f, 10f, 0.03f + 0.1f * unrest);
    }

    /** The lit look of a tier's star, with the field's unrest (0..1) and an event warning (0/1) as wobble. */
    public static EyeStarLook lit(int n, float unrest, float warn) {
        switch (n) {
            case 1:
                return new EyeStarLook(STAR, 9f, SOL_RGB, 3f, 1f, 0.02f + 0.1f * unrest + 0.06f * warn);
            case 2:
                return new EyeStarLook(STAR, 11f, SIRIUS_A_RGB, 3.2f, 1.2f, 0.02f + 0.1f * unrest + 0.06f * warn);
            case 3:
                // Arcturus, the red giant: the helium flash warning swells it
                return new EyeStarLook(STAR, 15f, ARCTURUS_RGB, 2.6f, 0.7f, 0.03f + 0.1f * unrest + 0.1f * warn);
            case 4:
                return new EyeStarLook(NEUTRON, 2.2f, NEUTRON_RGB, 4f, 12f, 0.03f + 0.08f * unrest + 0.05f * warn);
            case 5:
                return dwarf(0);
            default:
                // the magnetar's flares: a warning makes it throb
                return new EyeStarLook(MAGNETAR, 2.6f, MAGNETAR_RGB, 3.5f, 8f, 0.05f + 0.08f * unrest + 0.25f * warn);
        }
    }

    /**
     * Sirius B through a cooling batch (fraction 0..1): white, then orange, then a dull red that darkens to black as
     * its gamma falls under 1.
     */
    public static EyeStarLook dwarf(double fraction) {
        final float c = (float) Math.max(0, Math.min(1, fraction));
        final int rgb = c < 0.6f ? lerp(0xFFFFFF, 0xFF8C3C, c / 0.6f) : lerp(0xFF8C3C, 0x3C1428, (c - 0.6f) / 0.4f);
        final float gamma = 3.5f - 3.15f * c;
        return new EyeStarLook(DWARF, 3f, rgb, gamma, 0.6f, 0.01f);
    }

    /** Linear blend of two 0xRRGGBB colours. */
    static int lerp(int a, int b, float t) {
        final float u = Math.max(0, Math.min(1, t));
        final int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * u);
        final int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * u);
        final int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * u);
        return (r << 16) | (g << 8) | bl;
    }

    /** True when the two looks differ enough to be worth a packet (kind, colour, or 2 % / 0.05 moves). */
    public boolean differs(EyeStarLook o) {
        if (o == null || o.kind != kind || o.rgb != rgb) return true;
        return Math.abs(o.radius - radius) > 0.02f * Math.max(1f, radius) || Math.abs(o.gamma - gamma) > 0.05f
            || Math.abs(o.spin - spin) > 0.05f
            || Math.abs(o.pulse - pulse) > 0.02f;
    }

    /** One scanner word for the object. */
    public String label() {
        switch (kind) {
            case STAR:
                return "live star";
            case PROTOSTAR:
                return "igniting protostar";
            case NEUTRON:
                return "neutron star";
            case DWARF:
                return "cooling white dwarf";
            case MAGNETAR:
                return "magnetar";
            case SWELL:
                return "Core Collapse (fast-forward)";
            case BOUNCE:
                return "Core Collapse (bounce, proto-neutron star)";
            case NOVA:
                return "supernova";
            default:
                return "nothing";
        }
    }
}
