// Beyond the Stars: the Eye's cosmic objects (P5, p5-t9-t10.md §2.1, §6.2), the shared geometry of the Great
// Attractor (T9) and the Universe (T10) in the Eye's voxel frame (designs/voxel.js: x the oblong axis, y up, the centre
// at the origin). Pure functions, no uniforms. CosmosFx.java computes the same filaments and crown on the CPU (the
// fixed look and the flow's clump); keep the two in step.
#include "lib/common.glsl"

// The shader clock wraps every hour (RenderClock): every motion turns a whole number of times an hour.
#define CO_HOUR 3600.0

// A filament's length, blocks: from the core out to well past the rail (88).
#define CO_FIL_LEN 200.0
// Where a streaming galaxy is captured: this far along its filament (about 15 blocks from the core).
#define CO_CAPTURE_U 0.07

// Filament f (0..5): its far direction and two axes across it. 0..2 come from the rail's high side (+z, where the
// rail's 8 degree tilt lifts it) and lean up, 3..5 from its low side and lean down.
void coFilBasis(float f, out vec3 dir, out vec3 e1, out vec3 e2) {
    float side = f < 2.5 ? 1.0 : -1.0;
    float j = f < 2.5 ? f : f - 3.0;
    float az = radians(side > 0.0 ? 35.0 + 55.0 * j : 215.0 + 55.0 * j);
    float el = radians(side * (20.0 + 6.0 * j));
    dir = vec3(cos(el) * cos(az), sin(el), cos(el) * sin(az));
    e1 = normalize(cross(dir, vec3(0.0, 1.0, 0.0)));
    e2 = cross(e1, dir);
}

// The filament's centre line at u (0 the core, 1 the far end): a gentle S-bend round the straight line.
vec3 coFilPoint(float f, float u, vec3 dir, vec3 e1, vec3 e2) {
    float j = f < 2.5 ? f : f - 3.0;
    float ph = 1.7 * f;
    vec3 a = cos(ph) * e1 + sin(ph) * e2;
    vec3 b = -sin(ph) * e1 + cos(ph) * e2;
    return u * CO_FIL_LEN * dir + sin(BTS_PI * u) * (14.0 + 6.0 * j) * a + sin(BTS_TAU * u) * 6.0 * b;
}

// The braid's half-width at u, blocks: tight at the core, wide far out.
float coFilSpread(float u) {
    return 1.5 + 9.0 * u;
}

// A Cosmic Microwave Background colour for a temperature anisotropy v (-1 cold .. 1 warm): the familiar map's
// blue -> pale -> orange -> red, the mockup's palette (p5-prototype.html initR10).
vec3 coCmb(float v) {
    float t = clamp(0.5 + 0.5 * v, 0.0, 1.0);
    return vec3(30.0 + 225.0 * pow(t, 1.3), 60.0 + 120.0 * sin(BTS_PI * t), 40.0 + 220.0 * (1.0 - t)) / 255.0;
}
