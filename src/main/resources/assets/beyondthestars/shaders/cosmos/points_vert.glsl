// Beyond the Stars: the Eye's cosmic objects (P5 §2.1, §6.2), the point placement of bts_copts in both profiles. One
// static GL_POINTS buffer per object; every motion is computed here from the point's own numbers and the clocks, so
// nothing moves on the CPU:
//   a_UV.x  group x 64 + filament x 8 + size (blocks), a_UV.y a 0..1 seed
//   group 0 filament dust     a_Position (u0, a, b): along the filament, across it (in braid half-widths)
//         1 streaming galaxy  (phase0, a, ellipticity): down a filament, then captured into the halo
//         2 halo dot          (radius, inclination, phase0): the dark-matter halo of the galaxies held
//         3 flow member       (index 0..11, a, b): the Galactic Flow's group, placed by u_CoFlow
//         4 web point         (x, y, z) crown-local on a web edge (T10)
//         5 web node          (x, y, z) crown-local
#include "cosmos/lib.glsl"

// x pixels per block at one block, y least size px, z largest size px
uniform vec3 u_Points;
// x time (s), y streaming galaxies drawn 0..1 (phi / 4), z emergence 0..1 (The Infall), w brightness
uniform vec4 u_CoTime;
// the flow: x filament (-1 none), y the group's centre u, z 1 declined (streams past), w group size n
uniform vec4 u_CoFlow;
// x the flow filament's highlight 0..1, y the members captured (count), z the capture spiral 0..1, w the group's light
uniform vec4 u_CoFlow2;
// the halo: x share drawn 0..1 (galaxies held, log), y the V tint -1..1, z brightness, w the core's radius
uniform vec4 u_CoHalo;
// the web: x growth 0..1, y brightness, z first stars 0..1 (Cosmic Dawn), w scale (the universe's size)
uniform vec4 u_CoWeb;
// x fate (0 none, 1 Big Rip, 2 Big Crunch), y its progress 0..1, z the flow group's paleness (1: declined), w unused
uniform vec4 u_CoFate;

vec3 coHash3(float s) {
    return vec3(btsHash12(vec2(s * 91.7, 3.1)), btsHash12(vec2(s * 57.3, 8.9)), btsHash12(vec2(s * 23.9, 1.7)));
}

// A galaxy captured into the halo: from where it left its filament, spiralling in round the core and fading.
vec3 coSpiral(vec3 start, float seed, float q, out float fade) {
    float r0 = max(length(start), 1.0);
    vec3 ea = start / r0;
    vec3 k = normalize(coHash3(seed) - 0.5 + vec3(0.0, 0.8, 0.0));
    vec3 eb = normalize(cross(k, ea));
    float ang = q * BTS_TAU * 1.4;
    float r = r0 * (1.0 - 0.62 * q);
    fade = 1.0 - btsSmooth01(q);
    return r * (cos(ang) * ea + sin(ang) * eb);
}

// Places one point: its position in the model frame, colour and light (col), size (blocks) and sprite kind
// (0 a star dot, 1 a spiral galaxy, 2 an elliptical), the sprite's turn (cos, sin) and ellipticity.
vec3 coPlace(vec3 P, vec2 uv, out vec4 col, out float sizeB, out float kind, out vec2 rot, out float ell) {
    float group = floor(uv.x / 64.0);
    float rem = uv.x - group * 64.0;
    float f = floor(rem / 8.0);
    sizeB = rem - f * 8.0;
    float seed = uv.y;
    float t = u_CoTime.x;
    kind = 0.0;
    rot = vec2(1.0, 0.0);
    ell = 1.0;
    vec3 dir;
    vec3 e1;
    vec3 e2;
    if (group > 3.5) {
        // ---- the Universe's web: from scattered matter onto the filaments as it ages; torn out or crushed at the end
        vec3 scatter = (coHash3(seed) - 0.5) * 32.0 * (1.0 - u_CoWeb.x);
        vec3 pos = (P + scatter) * u_CoWeb.w;
        float node = group > 4.5 ? 1.0 : 0.0;
        float shown = 0.3 + 0.7 * u_CoWeb.x;
        float a = (fract(seed * 7.13) < shown ? 1.0 : 0.0) * (0.10 + 0.22 * fract(seed * 3.7) + 0.6 * node);
        a *= u_CoWeb.y * (0.35 + 0.65 * u_CoWeb.x);
        vec3 c = mix(vec3(1.0, 0.90, 0.76), vec3(1.0, 0.95, 0.86), node);
        // the first stars: after the Cosmic Dawn a share of the web lights up blue-white
        float star = fract(seed * 13.37) < 0.3 ? u_CoWeb.z : 0.0;
        c = mix(c, vec3(0.78, 0.86, 1.0), star);
        a *= 1.0 + 2.2 * star;
        if (u_CoFate.x > 0.5) {
            float k = u_CoFate.y;
            if (u_CoFate.x < 1.5) {
                pos *= 1.0 + 2.5 * k * k;
                a *= 1.0 - k;
            } else {
                pos *= pow(1.0 - k, 1.5);
                c = mix(c, vec3(1.0), k);
                a *= (1.0 + 2.0 * k) * (1.0 - btsSmooth01((k - 0.85) / 0.15));
            }
        }
        col = vec4(c, a);
        return pos;
    }
    if (group < 0.5) {
        // ---- filament dust: a braid of faint points drifting inward
        coFilBasis(f, dir, e1, e2);
        float u = fract(P.x - t * (12.0 + floor(seed * 6.0)) / CO_HOUR);
        vec3 pos = coFilPoint(f, u, dir, e1, e2) + coFilSpread(u) * (P.y * e1 + P.z * e2);
        float a = (0.12 + 0.25 * seed) * (1.0 - btsSmooth01((u - 0.86) / 0.14)) * btsSmooth01((u - 0.02) / 0.06);
        float hl = abs(f - u_CoFlow.x) < 0.5 ? u_CoFlow2.x : 0.0;
        col = vec4(mix(vec3(0.47, 0.59, 0.90), vec3(1.0, 0.84, 0.55), hl), a * (1.0 + 1.2 * hl) * u_CoTime.z);
        return pos;
    }
    if (group < 1.5) {
        // ---- a streaming galaxy: how many stream follows the flow phi; faster as it nears the core
        float shown = 1.0 - btsSmooth01((fract(seed * 7.31) - u_CoTime.y + 0.04) / 0.04);
        coFilBasis(f, dir, e1, e2);
        float p = fract(P.x + t * (6.0 + floor(seed * 5.0)) / CO_HOUR);
        vec3 pos;
        float a;
        if (p < 0.88) {
            float x = p / 0.88;
            float u = 1.0 - (1.0 - CO_CAPTURE_U) * pow(x, 1.6);
            float lat = coFilSpread(u) * 0.35;
            pos = coFilPoint(f, u, dir, e1, e2) + lat * (P.y * e1 + (fract(seed * 5.3) - 0.5) * 2.0 * e2);
            a = btsSmooth01(x / 0.05) * (1.0 - btsSmooth01((u - 0.85) / 0.15) * 0.6);
        } else {
            vec3 start = coFilPoint(f, CO_CAPTURE_U, dir, e1, e2);
            pos = coSpiral(start, seed, (p - 0.88) / 0.12, a);
        }
        kind = fract(seed * 13.1) < 0.6 ? 1.0 : 2.0;
        float ang = seed * BTS_TAU + t * BTS_TAU * floor((fract(seed * 3.3) - 0.5) * 80.0) / CO_HOUR;
        rot = vec2(cos(ang), sin(ang));
        ell = P.z;
        vec3 c = fract(seed * 17.9) < 0.6 ? vec3(0.75, 0.80, 1.0) : vec3(1.0, 0.93, 0.82);
        col = vec4(c, a * shown * u_CoTime.z);
        return pos;
    }
    if (group < 2.5) {
        // ---- the halo of the galaxies held: dots on tilted orbits round the core, tinted by V
        float shown = fract(seed * 3.71) < u_CoHalo.x ? 1.0 : 0.0;
        float r = P.x;
        float node = seed * BTS_TAU;
        vec3 n1 = vec3(cos(node), 0.0, sin(node));
        vec3 n2 = cos(P.y) * vec3(-sin(node), 0.0, cos(node)) + vec3(0.0, sin(P.y), 0.0);
        float turns = max(1.0, floor(515.0 / pow(r / 7.0, 1.5)));
        float ang = P.z + t * BTS_TAU * turns / CO_HOUR * (fract(seed * 9.1) < 0.5 ? -1.0 : 1.0);
        vec3 pos = r * (cos(ang) * n1 + sin(ang) * n2);
        float v = u_CoHalo.y;
        vec3 c = v > 0.0 ? mix(vec3(0.74, 0.80, 1.0), vec3(1.0, 0.62, 0.35), v) : mix(vec3(0.74, 0.80, 1.0),
            vec3(0.45, 0.62, 1.0), -v);
        col = vec4(c, shown * 0.24 * u_CoHalo.z);
        return pos;
    }
    // ---- a member of the Galactic Flow's group
    float i = P.x;
    float fil = u_CoFlow.x;
    if (fil < -0.5 || i >= u_CoFlow.w - 0.5) {
        col = vec4(0.0);
        return vec3(0.0);
    }
    coFilBasis(fil, dir, e1, e2);
    float uc = u_CoFlow.y + (fract(seed * 4.7) - 0.5) * 0.05;
    bool passes = u_CoFlow.z > 0.5 || i >= u_CoFlow2.y - 0.5;
    vec3 pos;
    float a = 1.0;
    if (uc >= CO_CAPTURE_U) {
        pos = coFilPoint(fil, uc, dir, e1, e2) + coFilSpread(uc) * 0.5 * (P.y * e1 + P.z * e2);
    } else if (passes) {
        // declined (or not caught): it swings past the core and out again
        float s = CO_CAPTURE_U - uc;
        vec3 out1 = normalize(0.75 * e1 - 0.65 * dir + 0.1 * e2);
        pos = coFilPoint(fil, CO_CAPTURE_U, dir, e1, e2) + s * CO_FIL_LEN * out1 + 4.0 * (P.y * e1 + P.z * e2);
        a = 1.0 - btsSmooth01((s - 0.2) / 0.3);
    } else {
        vec3 start = coFilPoint(fil, CO_CAPTURE_U, dir, e1, e2) + 3.0 * (P.y * e1 + P.z * e2);
        pos = coSpiral(start, seed, clamp((CO_CAPTURE_U - uc) / 0.05, 0.0, 1.0), a);
    }
    kind = fract(seed * 13.1) < 0.6 ? 1.0 : 2.0;
    float ang = seed * BTS_TAU + t * BTS_TAU * 229.0 / CO_HOUR;
    rot = vec2(cos(ang), sin(ang));
    ell = 0.45 + 0.4 * fract(seed * 2.9);
    col = vec4(mix(vec3(1.0, 0.82, 0.59), vec3(0.85, 0.9, 1.0), u_CoFate.z), a * u_CoFlow2.w);
    return pos;
}

// The point's size on screen from its size in blocks; a point drawn larger than it is keeps its light.
float coPointSize(float sizeB, float w, out float fade, out float px) {
    px = sizeB * u_Points.x / max(w, 0.05);
    float size = clamp(px, u_Points.y, u_Points.z);
    fade = px < size ? (px * px) / (size * size) : 1.0;
    return size;
}
