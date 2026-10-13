// Beyond the Stars shared GLSL (renders v2 §2.2): the soft shoulder every emissive layer passes its colour through,
// so a hot centre stays coloured instead of clipping to a flat white field (render QA round 3: 71-100 % white pixels
// read as "a white disc").
#include "lib/common.glsl"

// c' = 1 - exp(-k c), per channel (k about 1.6). 0 -> 0, slope k at 0, never reaches 1.
vec3 btsShoulder(vec3 c, float k) {
    return vec3(1.0) - exp(-k * max(c, vec3(0.0)));
}

// The shoulder rescaled so an input of 1 still maps to 1 (keeps a layer's authored peak, compresses above it).
vec3 btsShoulderNorm(vec3 c, float k) {
    return btsShoulder(c, k) / (1.0 - exp(-k));
}

// Luminance (Rec. 709 weights) of a display colour.
float btsLuma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// The shoulder, whitening a little only where the input's luminance passes 1 (a hot core whitens at its very centre,
// never across the whole layer).
vec3 btsHotWhite(vec3 c, float k) {
    float l = btsLuma(c);
    vec3 s = btsShoulder(c, k);
    float w = btsSmooth01((l - 1.0) / 3.0);
    return mix(s, vec3(btsLuma(s)), w * 0.6);
}
