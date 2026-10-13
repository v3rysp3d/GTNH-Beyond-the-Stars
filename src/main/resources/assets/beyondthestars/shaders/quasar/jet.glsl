// Beyond the Stars: a quasar jet (renders v2 §6.2), the body of bts_qjet in both profiles.
//
// One ribbon along the jet's axis, turned about the axis to face the camera. Per fragment: v_Z, the distance along
// the axis from the hole (blocks), and v_U, the signed distance across it (blocks). The jet is a slowly widening
// cylinder (opening angle about 3 degrees): a white-blue core, two helical strands twisting round the axis (pitch
// from spin and flux), bright knots travelling outward on a deterministic launch schedule whose rate follows the
// port's Bu/t, a violet-blue sheath fading with distance, and a terminal shock bow where the jet ends. Purge: a
// spiral flash runs up the strands and the jet pulses once; eruption: a giant knot. Additive (ONE, ONE).

#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

// x: brightness 0..1, y: knot rate per s, z: helix pitch (blocks), w: time (s)
uniform vec4 u_JLook;
// x: start (blocks from the hole), y: end, z: seed, w: exposure
uniform vec4 u_JSpan;
// x: event kind (0 none, 1 eruption, 2 purge), y: age s, z: size 0..1, w: a pixel's width at the jet, blocks
uniform vec4 u_JEvent;

float qjHash(float n) {
    return btsHash12(vec2(n, u_JSpan.z * 31.0 + 7.0));
}

vec4 qjShade(float z, float u) {
    float z0 = u_JSpan.x;
    float zEnd = u_JSpan.y;
    float t = u_JLook.w;
    float zz = z - z0;
    // the sheath radius (opening angle about 3 degrees)
    float R = 0.75 + 0.0524 * zz;
    float s = u / R;
    float as = abs(s);
    if (as > 1.9) return vec4(0.0);

    // fades in at the base (the glare sprite covers the hole's pole), out toward the end
    float along = smoothstep(0.0, 2.5, zz) * exp(-zz / 140.0);
    float endFade = 1.0 - smoothstep(zEnd - 0.4, zEnd + 0.6, z);

    // the sheath: a cylinder's column (sqrt(1 - s^2)) with a soft halo outside
    float sheath = sqrt(max(0.0, 1.0 - s * s)) * 0.35 + exp(-max(as - 1.0, 0.0) * 4.0) * 0.08 * step(1.0, as);
    sheath *= 0.6 + 0.4 * btsValueNoise2(vec2(zz * 0.35 - t * 3.2, s * 2.0 + u_JSpan.z));

    // the core
    // never thinner than about a pixel and a half, so the jet still reads from far outside (its light spread, not
    // added)
    float Rc0 = 0.16 + 0.010 * zz;
    float Rc = max(Rc0, 1.5 * u_JEvent.w);
    float core = exp(-(u * u) / (Rc * Rc)) * (0.75 + 0.25 * exp(-zz / 20.0)) * sqrt(Rc0 / Rc);
    core *= 0.85 + 0.15 * sin(zz * 1.7 - t * 11.0);

    // two helical strands
    float pitch = max(u_JLook.z, 2.0);
    float ph0 = 6.28318 * (zz / pitch - t * 0.35);
    float strands = 0.0;
    float purgeBand = 0.0;
    if (u_JEvent.x > 1.5 && u_JEvent.x < 2.5) {
        // the purge's spiral flash, running up both jets
        float zf = 26.0 * u_JEvent.y;
        purgeBand = exp(-((zz - zf) * (zz - zf)) / 30.0) * (1.0 - smoothstep(2.5, 3.5, u_JEvent.y));
    }
    for (int k = 0; k < 2; k++) {
        float ph = ph0 + 3.14159 * float(k);
        float xs = 0.55 * R * cos(ph);
        float front = 0.5 + 0.5 * sin(ph);
        float w = 0.13 * R + 0.05;
        float d = (u - xs) / w;
        strands += exp(-d * d) * (0.35 + 0.65 * front);
    }
    strands *= 1.0 + 5.0 * purgeBand;

    // knots: launched rate times a second (jittered), moving out at 8 blocks/s
    float rate = max(u_JLook.y, 0.05);
    float v = 8.0;
    float base = floor((t - zz / v) * rate);
    float knots = 0.0;
    for (int j = -1; j <= 1; j++) {
        float id = base + float(j);
        float tk = (id + 0.7 * qjHash(id)) / rate;
        float zk = v * (t - tk);
        if (zk < 0.0) continue;
        float len = 0.9 + 0.035 * zk;
        float dz = (zz - zk) / len;
        float amp = 0.5 + qjHash(id + 17.0);
        knots += exp(-dz * dz - (u * u) / (0.30 * R * R)) * amp;
    }
    // eruption: one giant knot
    if (u_JEvent.x > 0.5 && u_JEvent.x < 1.5) {
        float zk = 13.0 * u_JEvent.y;
        float len = 2.2 + 0.05 * zk;
        float dz = (zz - zk) / len;
        knots += exp(-dz * dz - (u * u) / (0.6 * R * R)) * (2.5 + 3.0 * u_JEvent.z) * exp(-u_JEvent.y / 6.0);
    }

    // the terminal shock: a small bright bow where the jet ends
    float bowZ = zEnd - 1.6 + 1.3 * s * s;
    float bow = exp(-((z - bowZ) * (z - bowZ)) / 0.35) * (1.0 - smoothstep(1.0, 1.5, as)) * 0.9;

    float pulse = 1.0;
    if (u_JEvent.x > 1.5 && u_JEvent.x < 2.5) pulse += 1.3 * exp(-u_JEvent.y / 0.5);

    vec3 cCore = vec3(0.86, 0.92, 1.0);
    vec3 cStrand = vec3(0.58, 0.70, 1.0);
    vec3 cSheath = vec3(0.42, 0.33, 1.0);
    vec3 cKnot = vec3(0.95, 0.96, 1.0);
    vec3 cPurge = vec3(0.75, 0.90, 1.0);
    vec3 c = cSheath * sheath + cCore * core * 1.6 + cStrand * strands * 0.55 + cKnot * knots * 1.1
        + cPurge * purgeBand * 0.6 * (1.0 - as * 0.5);
    c = c * along * pulse + vec3(0.9, 0.85, 1.0) * bow;
    c *= endFade * u_JLook.x * u_JSpan.w;
    return vec4(btsShoulder(c, 1.6), 1.0);
}
