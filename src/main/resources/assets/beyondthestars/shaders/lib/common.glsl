// Beyond the Stars shared GLSL (renders v2 §2.1): constants and ALU hashes.
// Included through GlslSourcePack: BTS_TEX is texture2D (legacy 120) or texture (core 330), BTS_CORE is defined in
// the core profile. Every function here is pure; no include declares a uniform.

#define BTS_PI 3.14159265358979
#define BTS_TAU 6.28318530717959

// A 0..1 hash of a 2D / 3D point (Dave Hoskins' hash without sine: stable across drivers).
float btsHash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float btsHash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 btsHash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

// Smooth 0..1 ramp with zero slope at both ends.
float btsSmooth01(float x) {
    x = clamp(x, 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
}
