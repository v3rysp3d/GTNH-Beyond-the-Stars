// Beyond the Stars: the Eye's stars (renders v2 §7, §8), helpers shared by the surface, halo and line programs.
#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"

// The look modes (EyeStarFx.MODE_*).
#define ST_MAIN 0.0
#define ST_GIANT 1.0
#define ST_PROTO 2.0
#define ST_NEUTRON 3.0
#define ST_DWARF 4.0
#define ST_MAGNETAR 5.0

vec3 stHash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

// Cellular noise in 3D from the 2 x 2 x 2 cells nearest p (Gustavson's trick: 8 hashes, not 27). Feature points
// wander with t (boiling). Returns F1, F2 (distances to the nearest two points) and the nearest cell's hash in z.
vec3 stCells3(vec3 p, float t) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 o = step(0.5, f) - 1.0;
    float d1 = 8.0;
    float d2 = 8.0;
    float id = 0.0;
    for (int k = 0; k < 8; k++) {
        vec3 c = o + vec3(float(k - 2 * (k / 2)), float((k / 2) - 2 * (k / 4)), float(k / 4));
        vec3 h = stHash33(i + c);
        // a point inside its cell, drifting on a small loop (the granule boils)
        vec3 q = c + 0.5 + 0.38 * (h - 0.5) * 2.0 * 0.8
            + 0.12 * vec3(sin(t * (0.7 + h.x) + h.y * 6.28), sin(t * (0.6 + h.y) + h.z * 6.28),
                          sin(t * (0.8 + h.z) + h.x * 6.28));
        vec3 r = q - f;
        float d = dot(r, r);
        if (d < d1) {
            d2 = d1;
            d1 = d;
            id = h.x;
        } else if (d < d2) {
            d2 = d;
        }
    }
    return vec3(sqrt(d1), sqrt(d2), id);
}

// A point on the unit sphere turned by the surface's rotation about the spin axis (y): (cos, sin) of the angle.
vec3 stTurn(vec3 n, vec2 cs) {
    return vec3(cs.x * n.x + cs.y * n.z, n.y, -cs.y * n.x + cs.x * n.z);
}

// Display colour for a temperature, a little more saturated than the plain blackbody so a star reads as its colour
// (yellow-white, blue-white, orange) rather than a white disc.
vec3 stKelvin(sampler2D lut, float k, float sat) {
    // below about 6,000 K the display temperature runs lower, so a Sun-like star reads golden and a giant orange
    k *= mix(0.62, 1.0, btsSmooth01((k - 6000.0) / 3500.0));
    vec3 c = btsBlackbody(lut, k);
    float l = btsLuma(c);
    return max(mix(vec3(l), c, sat), vec3(0.0));
}

// Ray / sphere: the near hit distance along the unit ray rd from the camera (origin), or -1.
float stSphereHit(vec3 rd, vec3 centre, float r) {
    float tc = dot(centre, rd);
    vec3 m = centre - tc * rd;
    float b2 = dot(m, m);
    if (b2 > r * r) return -1.0;
    return tc - sqrt(r * r - b2);
}
