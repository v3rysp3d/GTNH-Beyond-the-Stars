// Beyond the Stars: the Eye's cosmic objects (P5 §2.1, §6.2), one point's sprite (bts_copts) in both profiles: a soft
// round dot (dust, halo, web), or a small galaxy turned and inclined: a spiral (a bright bulge and two trailing arms)
// or an elliptical (a smooth de Vaucouleurs-like glow). Tiny galaxies fall back to the dot. Additive.
#include "lib/glow.glsl"
#include "lib/tonemap.glsl"

vec4 coSprite(vec2 pc, vec4 col, float px, float kind, vec2 rot, float ell) {
    float a;
    if (kind < 0.5) {
        a = btsSoftPoint(pc);
    } else {
        vec2 d = pc * 2.0 - 1.0;
        vec2 q = vec2(rot.x * d.x - rot.y * d.y, rot.y * d.x + rot.x * d.y);
        q.y /= max(ell, 0.2);
        float r = length(q);
        float g;
        if (kind < 1.5) {
            float th = atan(q.y, q.x);
            float arm = 0.5 + 0.5 * cos(2.0 * th - 6.5 * log(max(r, 0.04)));
            g = 1.1 * exp(-r * r * 16.0) + exp(-r * 3.4) * (0.2 + 0.6 * arm * arm);
        } else {
            g = 1.25 * exp(-4.6 * pow(r, 0.6));
        }
        g *= 1.0 - btsSmooth01((r - 0.7) / 0.3);
        a = mix(btsSoftPoint(pc), g, btsSmooth01((px - 3.0) / 5.0));
    }
    a *= col.a;
    if (a < 0.002) return vec4(-1.0);
    return vec4(btsShoulder(col.rgb * a, 1.6), 1.0);
}
