// Beyond the Stars shared GLSL (renders v2 §2.1): radial falloffs for glow sprites and soft points.
#include "lib/common.glsl"

// A glow at distance r (0 centre, 1 the sprite's edge): a Gaussian core of width `core` plus a 1 / (1 + (r / halo)^2)
// halo, cut to exactly 0 at the sprite's edge so the quad never shows.
float btsGlow(float r, float core, float halo) {
    float c = exp(-(r * r) / max(core * core, 1e-5));
    float h = 1.0 / (1.0 + (r * r) / max(halo * halo, 1e-5));
    float edge = 1.0 - btsSmooth01((r - 0.7) / 0.3);
    return (c + 0.35 * h) * edge;
}

// A soft round point sprite: 1 at the centre, 0 at the point's edge (pc = gl_PointCoord).
float btsSoftPoint(vec2 pc) {
    vec2 d = pc * 2.0 - 1.0;
    float r2 = dot(d, d);
    return exp(-4.0 * r2) * (1.0 - btsSmooth01((r2 - 0.6) / 0.4));
}
