package com.nateyburr.beyondthestars.client.render;

/**
 * Renders v2 (§2.1): the colour of a blackbody at a temperature, for the shared LUT ({@link NoiseTex#blackbodyLut})
 * and for CPU-side colour picks (the fixed-function paths).
 * <p>
 * Planck's law integrated over 380-780 nm against the CIE 1964 10-degree colour matching functions (the single-lobe
 * fits of Wyman, Sloan and Shirley 2013), XYZ to linear sRGB (D65), out-of-gamut negatives lifted by adding white,
 * normalised so the largest channel is 1, then sRGB-encoded (Minecraft has no linear pipeline: the LUT holds display
 * colour). Pure Java, no GL.
 */
public final class Blackbody {

    /** The LUT's range, kelvin; it is indexed by log temperature. */
    public static final double T_MIN = 1000, T_MAX = 40000;

    private Blackbody() {}

    /** Display (sRGB) colour, max channel 1, of a blackbody at {@code kelvin} (clamped to the LUT range). */
    public static float[] rgb(double kelvin) {
        final double t = Math.max(T_MIN, Math.min(T_MAX, kelvin));
        double x = 0, y = 0, z = 0;
        for (int nm = 380; nm <= 780; nm += 5) {
            final double p = planck(nm * 1e-9, t);
            x += p * xBar(nm);
            y += p * yBar(nm);
            z += p * zBar(nm);
        }
        // XYZ -> linear sRGB (D65)
        double r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
        double g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
        double b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;
        final double lift = Math.min(0, Math.min(r, Math.min(g, b)));
        r -= lift;
        g -= lift;
        b -= lift;
        final double max = Math.max(r, Math.max(g, b));
        if (max <= 0) return new float[] { 0, 0, 0 };
        return new float[] { (float) encode(r / max), (float) encode(g / max), (float) encode(b / max) };
    }

    /** {@link #rgb} packed as 0xRRGGBB. */
    public static int packed(double kelvin) {
        final float[] c = rgb(kelvin);
        return Math.round(c[0] * 255) << 16 | Math.round(c[1] * 255) << 8 | Math.round(c[2] * 255);
    }

    /** The LUT coordinate 0..1 of {@code kelvin} (log scale), the same mapping the shaders use. */
    public static double lutCoord(double kelvin) {
        final double t = Math.max(T_MIN, Math.min(T_MAX, kelvin));
        return Math.log(t / T_MIN) / Math.log(T_MAX / T_MIN);
    }

    /** The temperature at LUT coordinate {@code u} 0..1. */
    public static double kelvinAt(double u) {
        return T_MIN * Math.pow(T_MAX / T_MIN, Math.max(0, Math.min(1, u)));
    }

    private static double planck(double lambda, double t) {
        final double h = 6.62607015e-34, c = 2.99792458e8, k = 1.380649e-23;
        return (2 * h * c * c) / (Math.pow(lambda, 5) * (Math.exp(h * c / (lambda * k * t)) - 1));
    }

    private static double xBar(double nm) {
        final double a = Math.log((nm + 570.1) / 1014);
        final double b = Math.log((1338 - nm) / 743.5);
        return 0.398 * Math.exp(-1250 * a * a) + 1.132 * Math.exp(-234 * b * b);
    }

    private static double yBar(double nm) {
        final double a = (nm - 556.1) / 46.14;
        return 1.011 * Math.exp(-0.5 * a * a);
    }

    private static double zBar(double nm) {
        if (nm <= 265.8) return 0;
        final double a = Math.log((nm - 265.8) / 180.4);
        return 2.060 * Math.exp(-32 * a * a);
    }

    private static double encode(double linear) {
        final double c = Math.max(0, Math.min(1, linear));
        return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
    }
}
