// Beyond the Stars: the Eye's cosmic objects (P5 §2.1, §6.2), the shading of bts_cofx in both profiles (the
// placement and the uniforms are in cosmos/fx_vert.glsl):
//   0 the dark core: black, its rim faintly lit (opaque; it writes depth, so what passes behind it is hidden)
//   1 a Cosmic Dawn ray: a white-gold core and a warm sheath, brightest at the spike, flaring as it fires (additive)
//   2 the last-scattering surface: a slowly turning CMB map, multi-scale anisotropy on the cold-blue to warm-orange
//     palette, a hot plasma glow before Recombination, torn out (Big Rip) or crushed white (Big Crunch) at the end
//     (premultiplied)
//   3 a glowing ring: the collars' glass channels pulsing down the stack as the beam spools, or the crown (additive)
#include "cosmos/fx_uniforms.glsl"
#include "cosmos/lib.glsl"
#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;

vec4 coFxShade(vec3 mp, vec4 info) {
    if (u_FxMode < 0.5) {
        vec3 n = normalize(info.xyz);
        vec3 v = normalize(mp - u_Cam);
        float nv = abs(dot(n, v));
        float rim = pow(1.0 - nv, 3.0);
        return vec4(u_FxB.rgb * rim * u_FxA.y, 1.0);
    }
    if (u_FxMode < 1.5) {
        float s = info.x;
        float x = info.y;
        float fl = info.w;
        float ax = abs(x);
        if (ax > 2.2) return vec4(0.0);
        float core = exp(-x * x / 0.08);
        float sheath = exp(-x * x / 0.9);
        float along = btsSmooth01(s / 0.06) * pow(1.0 - s, 1.3);
        float idle = u_FxC.z;
        vec3 warm = vec3(1.0, 0.86, 0.63);
        vec3 c = (warm * sheath * 0.45 + mix(warm, vec3(1.0), 0.6) * core) * along * (idle + fl);
        return vec4(btsShoulder(c, 1.6), 1.0);
    }
    if (u_FxMode < 2.5) {
        vec2 e = info.xy;
        float r = info.z;
        float time = u_FxA.w;
        float edge = 1.0 - btsSmooth01((r - 0.93) / 0.07);
        float hole = btsSmooth01((r - u_FxC.z) / 0.05);
        // the map turns slowly in its plane
        float ca = cos(time * BTS_TAU * 17.0 / CO_HOUR);
        float sa = sin(time * BTS_TAU * 17.0 / CO_HOUR);
        vec2 q = vec2(ca * e.x - sa * e.y, sa * e.x + ca * e.y) * 9.0;
        int oct = int(u_FxB.w + 0.5);
        float big = btsFbm2(u_Noise, q + vec2(31.0, 7.0), oct);
        float small = btsNoise2(u_Noise, q * 3.1 + vec2(3.0, 91.0));
        float aniso = (big - 0.5) * 2.6 + (small - 0.5) * 0.7;
        vec3 cmb = coCmb(clamp(aniso, -1.0, 1.0));
        // a bright limb: the last-scattering surface seen edge-on at its rim
        cmb *= 0.85 + 0.35 * btsSmooth01((r - 0.75) / 0.2);
        // before Recombination: an opaque glowing plasma, boiling slowly
        float bw = time * BTS_TAU * 3.0 / CO_HOUR;
        float boil = btsFbm2(u_Noise, q * 0.6 + 8.0 * vec2(cos(bw), sin(bw)) + vec2(0.0, 13.0), 2);
        vec3 plasma = vec3(1.0, 0.55, 0.22) * (0.55 + 0.6 * boil) * u_FxC.w;
        float reveal = u_FxB.y;
        vec3 c = mix(plasma, cmb * (1.0 + u_FxB.z), reveal);
        float a = u_FxB.x * edge * hole;
        if (u_FxC.x > 0.5) {
            float k = u_FxC.y;
            if (u_FxC.x < 1.5) {
                // Big Rip: holes open and run together, the map reddens and fades
                float n = btsFbm2(u_Noise, q * 0.8 + vec2(71.0, 5.0), 2);
                a *= 1.0 - btsSmooth01((k * 1.25 - n) / 0.08);
                c = mix(c, vec3(1.0, 0.3, 0.15), k * 0.6);
            } else {
                // Big Crunch: everything heats toward white as it falls together
                c = mix(c, vec3(1.0, 0.95, 0.88), k);
                a = min(1.0, a * (1.0 + 1.5 * k)) * (1.0 - btsSmooth01((k - 0.9) / 0.1));
            }
        }
        if (a < 0.003) return vec4(0.0);
        return vec4(c * a, a);
    }
    float ring = info.z;
    float x = info.y;
    if (abs(x) > 2.0) return vec4(0.0);
    float prof = exp(-x * x / 0.18) + 0.25 * exp(-x * x / 1.2);
    float glow;
    if (ring < 4.5) {
        // the pulse runs down the collimator stack (top collar first), brighter as the beam spools
        float w = 0.5 + 0.5 * sin(u_FxA.z * BTS_TAU * 2865.0 / CO_HOUR + ring * 1.3);
        glow = u_FxA.w * (0.25 + 0.75 * w * w * w) * u_FxA.y;
    } else {
        glow = u_FxB.w;
    }
    return vec4(btsShoulder(u_FxB.rgb * prof * glow, 1.6), 1.0);
}
