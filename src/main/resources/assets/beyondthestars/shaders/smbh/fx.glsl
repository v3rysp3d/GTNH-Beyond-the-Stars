// Beyond the Stars: the Supermassive Lattice's effects (smbh-v1.md §7, R-19), the fragment body of bts_smbh in both
// profiles: the Containment Nodes' glow, the field arcs and the anchor threads (mode 0, additive), the intake streams
// (mode 1, additive) and the Unbound halo (mode 2, premultiplied: it darkens what lies behind it and adds a thin violet
// ring).

#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
// x: mode, y: time s, z: intensity, w: Unbound doublings n
uniform vec4 u_S;
// lattice: x Overdrive flare 0..1, y leak 0..1 (the hold-up short), z thread intensity, w a pixel per block of
// distance; streams: x Growth 0..1, y the gas's half-height; halo: x the shadow's radius over the halo's, y darkness,
// z ring brightness, w breathing -1..1
uniform vec4 u_SB;
// lattice: the tier colour, w the arcs' intensity; halo: the ring colour
uniform vec4 u_SC;

vec4 smShade(vec4 misc, float kind) {
    float t = u_S.y;
    float I = u_S.z;
    if (kind > 3.5) {
        // the Unbound halo: a heavier, darker lensing halo per doubling, a thin violet ring at its rim
        float x = length(misc.xy);
        if (x >= 1.0) return vec4(0.0, 0.0, 0.0, -1.0);
        float xs = u_SB.x;
        float body = pow(1.0 - x * x, 1.4) * smoothstep(0.55 * xs, 1.05 * xs, x);
        float alpha = u_SB.y * body;
        float d = (x - 0.82 * (1.0 + 0.03 * u_SB.w)) / 0.022;
        float ringA = exp(-d * d) * u_SB.z;
        float shimmer = 0.75 + 0.25 * btsNoise2(u_Noise, vec2(atan(misc.y, misc.x) * 9.0 + t * 0.6, t * 0.2));
        vec3 c = u_SC.rgb * (ringA * shimmer + 0.05 * u_SB.y * body);
        return vec4(btsShoulder(c, 1.6), alpha);
    }
    float i = misc.z;
    if (kind > 2.5) {
        // an intake stream: clumpy gas flowing inward, heating as it goes
        float across = misc.x;
        float s = misc.y;
        float prof = exp(-across * across * 2.6);
        float n = btsNoise2(u_Noise, vec2(s * 16.0 - t * 1.4, i * 5.3 + across * 0.6));
        float n2 = btsNoise2(u_Noise, vec2(s * 41.0 - t * 2.9, i * 9.1 + 3.0));
        float clump = smoothstep(0.42, 0.9, 0.65 * n + 0.35 * n2);
        float ends = smoothstep(0.0, 0.3, s) * (1.0 - smoothstep(0.82, 1.0, s));
        // cool, red infalling gas, heating to orange-white as it reaches the inner wall
        float kel = mix(1700.0, 5200.0, s * s);
        vec3 c = btsBlackbodyApprox(kel) * prof * ends * (0.12 + 1.6 * clump) * I;
        return vec4(btsShoulder(c, 1.6), 1.0);
    }
    if (kind < 0.5) {
        // a Containment Node: its tier colour, whiter and flickering as Overdrive flares it, amber blinking while the
        // hold-up is short
        float r = length(misc.xy);
        if (r >= 1.0) return vec4(0.0, 0.0, 0.0, -1.0);
        float flare = u_SB.x;
        float core = exp(-r * r / 0.035);
        float halo = 0.4 / (1.0 + r * r / 0.025);
        float edge = 1.0 - smoothstep(0.65, 1.0, r);
        float flick = 1.0 + flare * 0.45 * (btsHash12(vec2(floor(t * 14.0), i)) - 0.5);
        vec3 col = mix(u_SC.rgb, vec3(1.0), 0.35 * core + 0.45 * flare);
        float blink = 0.5 + 0.5 * sin(t * 5.0 + i * 0.4);
        col = mix(col, vec3(1.0, 0.6, 0.12), u_SB.y * (0.5 + 0.5 * blink));
        vec3 c = col * (1.5 * core + halo) * edge * I * (1.0 + 1.6 * flare) * flick;
        return vec4(btsShoulder(c, 1.6), 1.0);
    }
    float across = misc.x;
    float s = misc.y;
    float prof = exp(-across * across * 3.2);
    if (kind < 1.5) {
        // a field arc: beads of light running down into the ring, crackling in Overdrive
        float flare = u_SB.x;
        float ends = smoothstep(0.0, 0.05, s) * (1.0 - smoothstep(0.93, 1.0, s));
        float b = 0.5 + 0.5 * sin(s * 30.0 - t * (5.0 + 5.0 * flare) - i * 1.3);
        b = b * b * b;
        float crack = 1.0;
        if (flare > 0.0) {
            float h = btsHash12(vec2(floor(t * 20.0), i * 3.0));
            crack = mix(1.0, 0.45 + 1.3 * step(0.35, h), 0.6 * flare);
        }
        vec3 col = mix(u_SC.rgb, vec3(1.0), 0.2 + 0.5 * flare);
        vec3 c = col * prof * ends * (0.3 + 1.1 * b) * crack * u_SC.w * (1.0 + 1.2 * flare);
        return vec4(btsShoulder(c, 1.6), 1.0);
    }
    // an anchor thread: faint violet, shimmering
    float sh = 0.55 + 0.45 * sin(s * 19.0 + t * 2.2 + i * 0.7);
    vec3 c = vec3(0.62, 0.36, 1.0) * prof * sh * u_SB.z;
    return vec4(btsShoulder(c, 1.6), 1.0);
}
