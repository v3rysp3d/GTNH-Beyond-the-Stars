// Beyond the Stars: a beam / stream (renders v2 §10-§12, WP-6..WP-8), the body of bts_beam in both profiles.
//
// One ribbon along a quadratic Bezier curve (camera-relative control points, fx/beam_vert.glsl), turned at every
// point to face the camera. Per fragment: v_S (0..1 along), v_U (blocks across) and v_W (the half-width there). The
// look of the quasar's jets, reused for every stream: a white core, a coloured sheath, a helical strand, knots
// travelling along the stream at its speed (launched as often as its rate asks), fading in and out at the ends.
// Additive (ONE, ONE).

#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

// x intensity, y knot speed (blocks / s), z length (blocks), w time (s)
uniform vec4 u_BLook;
// rgb core colour, a strand amount
uniform vec4 u_BColA;
// rgb sheath colour, a knot rate (per s)
uniform vec4 u_BColB;
// x fade-in length, y fade-out length (blocks), z seed, w helix pitch (blocks)
uniform vec4 u_BEnds;

float bmHash(float n) {
    return btsHash12(vec2(n, u_BEnds.z * 31.0 + 7.0));
}

vec4 bmShade(float s, float u, float hw) {
    float x = u / max(hw, 1e-4);
    float ax = abs(x);
    if (ax > 2.15) return vec4(0.0);
    float L = u_BLook.z;
    float z = s * L;
    float t = u_BLook.w;
    float speed = max(u_BLook.y, 0.1);

    float sheath = sqrt(max(0.0, 1.0 - x * x)) * 0.38 + exp(-max(ax - 1.0, 0.0) * 3.0) * 0.10 * step(1.0, ax);
    sheath *= 0.65 + 0.35 * btsValueNoise2(vec2(z * 0.5 - t * speed * 0.5, x * 2.0 + u_BEnds.z));
    float core = exp(-(x * x) / 0.07) * (0.85 + 0.15 * sin(z * 1.9 - t * speed * 1.4));

    float pitch = max(u_BEnds.w, 1.0);
    float strands = 0.0;
    for (int k = 0; k < 2; k++) {
        float ph = BTS_TAU * (z / pitch - t * 0.45) + 3.14159 * float(k);
        float xs = 0.55 * cos(ph);
        float d = (x - xs) / 0.17;
        strands += exp(-d * d) * (0.35 + 0.65 * (0.5 + 0.5 * sin(ph)));
    }

    float rate = max(u_BColB.a, 0.02);
    float base = floor((t - z / speed) * rate);
    float knots = 0.0;
    for (int j = -1; j <= 1; j++) {
        float id = base + float(j);
        float tk = (id + 0.7 * bmHash(id)) / rate;
        float zk = speed * (t - tk);
        if (zk < 0.0) continue;
        float dz = (z - zk) / (0.7 + 0.25 * hw);
        knots += exp(-dz * dz - (x * x) / 0.3) * (0.6 + bmHash(id + 17.0));
    }

    float fade = btsSmooth01(z / max(u_BEnds.x, 0.01)) * (1.0 - btsSmooth01((z - (L - u_BEnds.y)) / max(u_BEnds.y, 0.01)));
    vec3 cA = u_BColA.rgb;
    vec3 cB = u_BColB.rgb;
    vec3 c = cB * sheath + mix(cA, vec3(1.0), 0.35) * core * 1.5 + mix(cA, cB, 0.4) * strands * u_BColA.a
        + mix(cA, vec3(1.0), 0.5) * knots * 1.1;
    c *= fade * u_BLook.x;
    return vec4(btsShoulder(c, 1.6), 1.0);
}
