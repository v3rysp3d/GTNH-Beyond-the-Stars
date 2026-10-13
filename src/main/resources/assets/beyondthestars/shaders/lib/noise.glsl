// Beyond the Stars shared GLSL (renders v2 §2.1): value noise and fbm.
// Texture noise reads NoiseTex (256 x 256, repeat, linear; G = R shifted by (37, 17) texels): one lookup per octave,
// cheaper than ALU noise on the target GPUs. The ALU noise is for programs that bind no noise texture.
#include "lib/common.glsl"

#define BTS_NOISE_SIZE 256.0
#define BTS_FBM_MAX 6

// 2D value noise from the texture, -> 0..1. One texel = one lattice cell.
float btsNoise2(sampler2D noiseTex, vec2 p) {
    return BTS_TEX(noiseTex, (p + 0.5) / BTS_NOISE_SIZE).r;
}

// The four channels at once (R, G = R shifted, B, A), -> 0..1 each.
vec4 btsNoise2x4(sampler2D noiseTex, vec2 p) {
    return BTS_TEX(noiseTex, (p + 0.5) / BTS_NOISE_SIZE);
}

// 3D value noise from one 2D lookup (the G-shift trick), -> 0..1.
float btsNoise3(sampler2D noiseTex, vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = (p.xy + vec2(37.0, 17.0) * p.z) + f.xy;
    vec2 gr = BTS_TEX(noiseTex, (uv + 0.5) / BTS_NOISE_SIZE).gr;
    return mix(gr.x, gr.y, f.z);
}

// fbm of btsNoise2 with up to BTS_FBM_MAX octaves (lacunarity 2.03, gain 0.5, a rotation per octave), -> about 0..1.
float btsFbm2(sampler2D noiseTex, vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    mat2 rot = mat2(0.80, 0.60, -0.60, 0.80);
    for (int i = 0; i < BTS_FBM_MAX; i++) {
        if (i >= octaves) break;
        sum += amp * btsNoise2(noiseTex, p);
        norm += amp;
        p = rot * p * 2.03 + vec2(17.1, 9.2);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// fbm of btsNoise3, -> about 0..1.
float btsFbm3(sampler2D noiseTex, vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < BTS_FBM_MAX; i++) {
        if (i >= octaves) break;
        sum += amp * btsNoise3(noiseTex, p);
        norm += amp;
        p = p * 2.03 + vec3(17.1, 9.2, 3.7);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// ALU 2D value noise, -> 0..1 (no texture).
float btsValueNoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = btsHash12(i);
    float b = btsHash12(i + vec2(1.0, 0.0));
    float c = btsHash12(i + vec2(0.0, 1.0));
    float d = btsHash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// ALU fbm, -> about 0..1.
float btsValueFbm2(vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < BTS_FBM_MAX; i++) {
        if (i >= octaves) break;
        sum += amp * btsValueNoise2(p);
        norm += amp;
        p = p * 2.03 + vec2(17.1, 9.2);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// Cellular (Worley-like) distance to the nearest of one hashed point per cell, -> 0..~1.2 (granulation, cells).
float btsCells2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float best = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 o = vec2(float(x), float(y));
            vec2 r = o + btsHash22(i + o) - f;
            best = min(best, dot(r, r));
        }
    }
    return sqrt(best);
}
