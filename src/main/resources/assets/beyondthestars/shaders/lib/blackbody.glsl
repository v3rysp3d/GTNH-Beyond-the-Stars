// Beyond the Stars shared GLSL (renders v2 §2.1): blackbody colour.
// The LUT (NoiseTex.blackbodyLut, 256 x 1) holds display colour (sRGB, max channel 1) from 1,000 to 40,000 K on a log
// scale: Planck x the CIE 1964 10-degree observer (Blackbody.java computes the same values on the CPU).

#define BTS_BB_TMIN 1000.0
#define BTS_BB_TMAX 40000.0

// Kelvin -> the LUT coordinate 0..1 (the same mapping as Blackbody.lutCoord).
float btsBlackbodyCoord(float kelvin) {
    float t = clamp(kelvin, BTS_BB_TMIN, BTS_BB_TMAX);
    return log(t / BTS_BB_TMIN) / log(BTS_BB_TMAX / BTS_BB_TMIN);
}

// Display colour of a blackbody, from the LUT.
vec3 btsBlackbody(sampler2D lut, float kelvin) {
    float u = btsBlackbodyCoord(kelvin);
    return BTS_TEX(lut, vec2(u * (255.0 / 256.0) + 0.5 / 256.0, 0.5)).rgb;
}

// Analytic approximation (Tanner Helland's fit, display colour, max channel about 1) for programs with no LUT bound.
vec3 btsBlackbodyApprox(float kelvin) {
    float t = clamp(kelvin, BTS_BB_TMIN, BTS_BB_TMAX) / 100.0;
    vec3 c;
    if (t <= 66.0) {
        c.r = 1.0;
        c.g = clamp(0.39008158 * log(t) - 0.63184144, 0.0, 1.0);
        c.b = t <= 19.0 ? 0.0 : clamp(0.54320679 * log(t - 10.0) - 1.19625409, 0.0, 1.0);
    } else {
        c.r = clamp(1.29293619 * pow(t - 60.0, -0.1332047592), 0.0, 1.0);
        c.g = clamp(1.12989086 * pow(t - 60.0, -0.0755148492), 0.0, 1.0);
        c.b = 1.0;
    }
    return c;
}
