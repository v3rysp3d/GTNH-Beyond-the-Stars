// Beyond the Stars: the Eye's galaxies (renders v2 §9.2), the diffuse light (bts_galdisc) in both profiles. One quad
// in front of the galaxy (or of the camera inside it); each fragment follows its view ray through the galaxy in the
// disc frame and integrates, front to back:
//   - the stellar disc: an exponential disc in a Gaussian layer, old light bunched toward the arms, young blue light
//     and pink H II knots just downstream of each ridge, clumped by noise turned with the pattern and sheared by the
//     wind of the spiral (flocculent when u_Gal1.y is high);
//   - the dust: a thinner Gaussian layer, lanes just upstream of each ridge broken into filaments; it dims whatever
//     lies behind it on the ray (the far half of the bulge seen edge-on: the NGC 891 lane), and its absorption is the
//     output alpha (premultiplied, ONE / ONE_MINUS_SRC_ALPHA), so the backdrop and the world behind darken too;
//   - the bulge and the bar: ellipsoidal Gaussians integrated exactly (erf), split by the dust;
//   - the events: the crossing's ring wave, the pericentre starburst, the companion's and the dwarf's cores, the
//     Magnetar Collapse's flash and shell.
// The ray is cut into sub-segments inside the dust / star slab; each samples the plane-crossing point and integrates
// the layers' Gaussian height profiles exactly over its own length, so a face-on view needs one or two and an edge-on
// view up to u_Detail.x.
#include "galaxy/lib.glsl"
#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
uniform vec3 u_Centre;
// the eye relative to the render origin (Minecraft's first-person camera sits 0.1 blocks behind it)
uniform vec3 u_Eye;
// the disc's in-plane axes (camera space, unit); the normal is their cross product
uniform vec3 u_AxX;
uniform vec3 u_AxY;
// x reach R, y scale length, z cot(pitch), w major-arm strength A2
uniform vec4 u_Gal0;
// x minor-arm strength A4, y flocculence 0..1, z pattern turns / hour, w young-light strength
uniform vec4 u_Gal1;
// x pattern angle (rad), y time (s), z formation 0..1, w brightness
uniform vec4 u_Gal2;
// x v0, y rc, z dust strength, w the ridge's inner radius
uniform vec4 u_Rot;
// x ring-wave radius (< 0 none), y its strength, z starburst flash 0..1, w burst expansion 0..1
uniform vec4 u_Ev;
// the companion (xyz disc frame, w strength) and the dwarf (xyz, w strength)
uniform vec4 u_Comp;
uniform vec4 u_Sat;
// x bulge radius, y bulge brightness, z bar half-length, w bar strength
uniform vec4 u_Bulge;
// x shell radius, y shell strength, z core flash, w the dwarf's impact flash
uniform vec4 u_Flash;
// x most sub-segments, y noise octaves, z disc light colour mix (0 Triangulum blue .. 1 Milky Way warm), w unused 0
uniform vec4 u_Detail;

const float GAL_HS = 0.42;
const float GAL_HD = 0.13;

vec3 galToDisc(vec3 v, vec3 n) {
    return vec3(dot(v, u_AxX), dot(v, u_AxY), dot(v, n));
}

// The sheared pattern frame: the plane turned with the arms and wound with the spiral, for the noise.
vec2 galPatternUV(vec2 xy, float r) {
    float a = -u_Gal2.x + log(max(r, 0.6) / u_Rot.w) * u_Gal0.z * 0.55;
    float c = cos(a);
    float s = sin(a);
    return vec2(c * xy.x - s * xy.y, s * xy.x + c * xy.y);
}

vec4 galDisc(vec3 rdCam) {
    vec3 n = normalize(cross(u_AxX, u_AxY));
    vec3 o = galToDisc(u_Eye - u_Centre, n);
    vec3 d = galToDisc(rdCam, n);
    float R = u_Gal0.x;
    float f = u_Gal2.z;
    // the bounding sphere (nothing reaches past the Eye's blocks)
    float bS = dot(o, d);
    float cS = dot(o, o) - 20.4 * 20.4;
    float hS = bS * bS - cS;
    if (hS <= 0.0) return vec4(-1.0);
    hS = sqrt(hS);
    float tA = max(0.0, -bS - hS);
    float tB = -bS + hS;
    if (tB <= 0.0) return vec4(-1.0);

    // the slab the stars and dust live in, cut to the disc's cylinder
    float zb = 4.0 * GAL_HS;
    float s0 = tA;
    float s1 = tB;
    bool slab = true;
    if (abs(d.z) > 1e-5) {
        float ta = (-zb - o.z) / d.z;
        float tb = (zb - o.z) / d.z;
        s0 = max(s0, min(ta, tb));
        s1 = min(s1, max(ta, tb));
    } else if (abs(o.z) > zb) {
        slab = false;
    }
    float qa = dot(d.xy, d.xy);
    if (qa > 1e-8) {
        float qb = dot(o.xy, d.xy);
        float qc = dot(o.xy, o.xy) - R * R * 1.04;
        float qh = qb * qb - qa * qc;
        if (qh <= 0.0) {
            slab = false;
        } else {
            qh = sqrt(qh);
            s0 = max(s0, (-qb - qh) / qa);
            s1 = min(s1, (-qb + qh) / qa);
        }
    } else if (dot(o.xy, o.xy) > R * R) {
        slab = false;
    }
    if (s1 <= s0) slab = false;

    // the bulge (round, a little flattened) and the bar (turning with the arms)
    float rb = max(u_Bulge.x, 0.2);
    vec3 invB = vec3(1.0 / rb, 1.0 / rb, 1.0 / (0.72 * rb));
    vec2 bulgeAx = vec2(1.0, 0.0);
    vec3 invBar = vec3(1.0 / max(u_Bulge.z, 0.2), 1.0 / (0.3 * max(u_Bulge.z, 0.2)), 1.0 / (0.22 * max(u_Bulge.z, 0.2)));
    vec2 barAx = vec2(cos(u_Gal2.x), sin(u_Gal2.x));
    // B-251: the Magnetar Collapse's core flash lifts the bulge x3.5 at its peak (was x5)
    float flashB = 1.0 + 1.2 * u_Ev.z + 2.5 * u_Flash.z;
    float bulgeAmp = u_Bulge.y * flashB / (rb * 1.7725);
    float barAmp = u_Bulge.w * flashB / (0.6 * max(u_Bulge.z, 0.2) * 1.7725);
    vec3 bulgeCol = mix(vec3(1.0, 0.72, 0.45), vec3(1.0, 0.86, 0.66), 0.4) * mix(vec3(1.0), vec3(1.0, 0.7, 0.95), u_Ev.z);
    // formation: the bulge is a loose glowing halo until the galaxy settles
    float spread = mix(3.5, 1.0, f);

    vec3 col = vec3(0.0);
    float T = 1.0;
    // 1) in front of the slab: the bulge alone
    float front = slab ? s0 : tB;
    if (front > tA) {
        col += bulgeCol * (bulgeAmp * galBlob(o, d, vec3(0.0), invB / spread, bulgeAx, tA, front)
            + barAmp * galBlob(o, d, vec3(0.0), invBar / spread, barAx, tA, front));
    }
    // 2) through the slab
    if (slab) {
        float len = s1 - s0;
        float stepLen = max(1.4, R / 11.0);
        int nSeg = int(clamp(ceil(len / stepLen), 1.0, u_Detail.x));
        float seg = len / float(nSeg);
        int oct = int(u_Detail.y);
        vec3 oldCol = mix(vec3(0.80, 0.86, 1.0), vec3(1.0, 0.86, 0.66), u_Detail.z);
        vec3 youngCol = vec3(0.60, 0.74, 1.0);
        vec3 hiiCol = vec3(1.0, 0.38, 0.62);
        float ridgeR0 = u_Rot.w;
        for (int i = 0; i < 24; i++) {
            if (i >= nSeg) break;
            float ta = s0 + seg * float(i);
            float tb = ta + seg;
            float z0 = o.z + d.z * ta;
            float z1 = o.z + d.z * tb;
            // the bulge and the bar where this sub-segment passes near them
            vec3 em = vec3(0.0);
            float near = min(length(o + d * ta), length(o + d * tb));
            near = min(near, length(o + d * clamp(-dot(o, d), ta, tb)));
            if (near < 3.2 * spread * max(rb, u_Bulge.z * step(0.01, barAmp))) {
                em += bulgeCol * bulgeAmp * galBlob(o, d, vec3(0.0), invB / spread, bulgeAx, ta, tb);
                if (barAmp > 0.0) em += bulgeCol * barAmp * galBlob(o, d, vec3(0.0), invBar / spread, barAx, ta, tb);
            }
            // a sub-segment that stays well clear of the plane holds no disc light or dust worth a lookup
            float zMin = z0 * z1 <= 0.0 ? 0.0 : min(abs(z0), abs(z1));
            if (zMin > 2.6 * GAL_HS * spread) {
                col += T * em;
                continue;
            }
            float tm = abs(d.z) > 1e-5 ? clamp(-o.z / d.z, ta, tb) : 0.5 * (ta + tb);
            vec3 P = o + d * tm;
            float r = length(P.xy);
            float th = atan(P.y, P.x);
            float cs = galLayer(z0, z1, seg, GAL_HS * spread) / (GAL_HS * spread * 1.7725);
            float cd = galLayer(z0, z1, seg, GAL_HD) / (GAL_HD * 1.7725);
            float ridge = galRidge(r, u_Gal2.x, u_Gal0.z, ridgeR0);
            float ph2 = galPhase(th, ridge, 2.0);
            float ph4 = galPhase(th, ridge, 4.0);
            float edge = 1.0 - smoothstep(0.82 * R, 1.02 * R, r);
            // one RGBA lookup per octave carries four decorrelated fields: clumps (r), H II knots (g), flocculent
            // patches (b), dust filaments (a)
            vec2 puv = galPatternUV(P.xy, r);
            vec4 nz = vec4(0.5);
            if (oct > 0) {
                vec4 n1 = btsNoise2x4(u_Noise, puv * 1.35 + vec2(31.0, 7.0));
                vec4 n2 = oct > 1 ? btsNoise2x4(u_Noise, puv * 2.9 + vec2(17.0, 93.0)) : n1;
                vec4 n3 = oct > 2 ? btsNoise2x4(u_Noise, puv * 6.1 + vec2(71.0, 29.0)) : n2;
                nz = n1 * 0.55 + n2 * 0.3 + n3 * 0.15;
                nz.g = n2.g * 0.6 + n3.g * 0.4;
            }
            float clump = nz.r;
            float floc = nz.b;
            // old light: the exponential disc, bunched toward the arms
            float sOld = exp(-r / u_Gal0.y) * galCrowd(ph2, 0.35 * u_Gal0.w) * (0.75 + 0.5 * clump) * edge;
            // young light: just downstream of each ridge, clumpy; flocculent patches instead of clean arms
            float arm = galYoung(ph2) + 0.55 * (u_Gal1.x / max(u_Gal0.w, 0.05)) * galYoung(ph4);
            arm = mix(arm, 1.5 * smoothstep(0.42, 0.72, floc), u_Gal1.y);
            float young = arm * smoothstep(0.35, 0.85, clump + 0.15) * exp(-r / (1.6 * u_Gal0.y))
                * smoothstep(0.8, 2.2, r) * edge * u_Gal1.w * smoothstep(0.35, 1.0, f);
            // H II: pink knots where the young light peaks
            float hii = young * smoothstep(0.6, 0.82, nz.g) * 2.2;
            // the crossing's ring wave
            float ring = 0.0;
            if (u_Ev.x > 0.0) {
                float dr = (r - u_Ev.x) / 1.1;
                ring = u_Ev.y * exp(-dr * dr) * (0.55 + 0.9 * clump) * edge;
            }
            // dust: lanes upstream of the ridges, broken into filaments; patchy when flocculent
            float lanes = galDust(ph2) * (0.35 + u_Gal0.w) + 0.5 * u_Gal1.x * galDust(ph4);
            lanes = mix(lanes, 1.6 * smoothstep(0.45, 0.78, floc), u_Gal1.y);
            float fil = nz.a;
            // (thinner inside the bar's reach, so the bar and the inner disc are not lost behind it)
            float inner = mix(0.3, 1.0, smoothstep(0.5 * ridgeR0, 1.1 * ridgeR0, r));
            float tau = u_Rot.z * exp(-r / (1.4 * u_Gal0.y)) * smoothstep(0.2, 1.2, r) * edge * (0.25 + lanes)
                * (0.35 + 1.3 * fil) * f * f * inner + 0.6 * ring;
            em += (oldCol * sOld * 0.55 + youngCol * young * 0.9 + hiiCol * hii + vec3(0.95, 0.55, 1.0) * ring
                * 1.4) * cs;
            float a = tau * cd;
            col += T * em * exp(-0.5 * a);
            T *= exp(-a);
            if (T < 0.01) break;
        }
        // 3) behind the slab: the rest of the bulge, through the dust
        if (tB > s1) {
            col += T * bulgeCol * (bulgeAmp * galBlob(o, d, vec3(0.0), invB / spread, bulgeAx, s1, tB)
                + barAmp * galBlob(o, d, vec3(0.0), invBar / spread, barAx, s1, tB));
        }
    }

    // the scattered glow round the core (keeps the bulge from looking cut out)
    float bc = length(o - d * dot(o, d));
    col += bulgeCol * u_Bulge.y * 0.035 * flashB / (1.0 + bc * bc / (4.0 * rb * rb)) * step(0.0, dot(-o, d));
    // the companion's and the dwarf's cores
    if (u_Comp.w > 0.0) {
        float g = galBlob(o, d, u_Comp.xyz, vec3(1.0 / 1.2), vec2(1.0, 0.0), tA, tB);
        float halo = galBlob(o, d, u_Comp.xyz, vec3(1.0 / 3.6), vec2(1.0, 0.0), tA, tB);
        col += (vec3(1.0, 0.8, 0.62) * g * 0.5 + vec3(0.7, 0.75, 1.0) * halo * 0.07) * u_Comp.w * (1.0 + 1.2 * u_Ev.z);
    }
    if (u_Sat.w > 0.0) {
        float g = galBlob(o, d, u_Sat.xyz, vec3(1.0 / 0.9), vec2(1.0, 0.0), tA, tB);
        col += vec3(1.0, 0.85, 0.7) * g * u_Sat.w * 0.8;
        float fl = galBlob(o, d, u_Sat.xyz, vec3(1.0 / 2.2), vec2(1.0, 0.0), tA, tB);
        col += vec3(0.85, 0.75, 1.0) * fl * u_Flash.w * 1.2;
    }
    // the Magnetar Collapse: a core flash and a violet-white shell running out to the bands
    if (u_Flash.y > 0.0) {
        float S = u_Flash.x;
        float bb = length(o - d * dot(o, d));
        if (bb < S) {
            float w = max(0.6, 0.06 * S);
            float h = sqrt(S * S - bb * bb);
            float si = max(S - w, 0.0);
            float hin = sqrt(max(si * si - bb * bb, 0.0));
            float path = (h - hin) / w;
            float tear = btsFbm2(u_Noise, (o.xy + d.xy * (dot(-o, d))) * 0.35 + vec2(11.0, 3.0), 2);
            col += vec3(0.78, 0.62, 1.0) * u_Flash.y * min(path, 6.0) * 0.18 * (0.5 + tear);
        }
    }
    col *= u_Gal2.w;
    float alpha = 1.0 - T;
    if (max(max(col.r, col.g), col.b) < 0.002 && alpha < 0.004) return vec4(-1.0);
    return vec4(btsShoulder(col, 1.6), alpha);
}
