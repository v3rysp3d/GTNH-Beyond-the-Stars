// Beyond the Stars: the Eye's galaxies (renders v2 §9.2), one point star's sprite, shared by both profiles: a soft
// round core, faint four-point diffraction spikes on the stars drawn larger than about 6 pixels, or (H II regions) a
// wide soft pink glow. Additive.
#include "lib/glow.glsl"
#include "lib/tonemap.glsl"

vec4 galSprite(vec2 pc, vec4 col, float px, float soft) {
    vec2 d = pc * 2.0 - 1.0;
    float r2 = dot(d, d);
    float a;
    if (soft > 0.5) {
        a = 0.3 * exp(-3.0 * r2) * (1.0 - btsSmooth01((r2 - 0.6) / 0.4));
    } else {
        a = btsSoftPoint(pc);
        vec2 ad = abs(d);
        float spikes = exp(-ad.x * 26.0) * exp(-ad.y * 3.0) + exp(-ad.y * 26.0) * exp(-ad.x * 3.0);
        a += 0.55 * spikes * smoothstep(5.0, 12.0, px);
    }
    a *= col.a;
    if (a < 0.002) return vec4(-1.0);
    return vec4(btsShoulder(col.rgb * a, 1.6), 1.0);
}
