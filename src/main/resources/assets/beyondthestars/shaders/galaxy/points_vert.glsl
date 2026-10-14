// Beyond the Stars: the Eye's galaxies (renders v2 §9.2), the point stars' placement (bts_galpts), shared by both
// profiles. One static GL_POINTS buffer per galaxy (20,000 stars) plus the companion (3,000), the dwarf (1,500) and
// the tidal tails (2,000); every motion is computed here from the star's own numbers and the clocks:
//   a_Position: groups 0 / 1 (old disc, young): (r, theta0, z); 2 / 3 / 6 / 7 (bulge, bar, halo, Magellanic Clouds):
//               (x, y, z); 4 / 5 (cluster stars, H II glows): (cluster id x 4 + dr + 1.5, dtheta, z);
//               tails: (s along the tail 0..1, two jitters)
//   a_UV:       x group x 8 + size in blocks, y the colour packed r x 65536 + g x 256 + b
#include "galaxy/lib.glsl"
#include "lib/noise.glsl"

// x pixels per block at one block, y least size px, z largest size px
uniform vec3 u_Points;
// 0 the galaxy, 1 the companion galaxy, 2 the dwarf (satellite / minor merger), 3 the tidal tails and bridge
uniform float u_Mode;
// x reach R, y scale length, z cot(pitch), w major-arm strength A2
uniform vec4 u_Gal0;
// x minor-arm strength A4, y flocculence 0..1, z pattern turns / hour, w young-star strength
uniform vec4 u_Gal1;
// x pattern angle (rad), y time (s), z formation 0..1 (the halo capture), w brightness
uniform vec4 u_Gal2;
// x v0, y rc (the rotation curve), z dust strength, w the ridge's inner radius
uniform vec4 u_Rot;
// x ring-wave radius (< 0 none), y its strength, z starburst flash 0..1, w burst expansion 0..1 (1 settled)
uniform vec4 u_Ev;
// companion: xyz centre (disc frame), w strength 0..1; u_CompN xyz its disc's normal, w its scale
uniform vec4 u_Comp;
uniform vec4 u_CompN;
// the dwarf: xyz centre, w strength; u_SatV xyz its direction of travel, w its stretch along it (1 round)
uniform vec4 u_Sat;
uniform vec4 u_SatV;
// x tails' growth 0..1, y the bridge 0..1, z the tails' sense (+-1), w the dwarf's scale
uniform vec4 u_Tail;
// xyz the eye in the disc frame, w daylight 0..1
uniform vec4 u_EyeD;

// Rotates v about the unit axis k by a (Rodrigues).
vec3 galRotate(vec3 v, vec3 k, float a) {
    float c = cos(a);
    float s = sin(a);
    return v * c + cross(k, v) * s + k * dot(k, v) * (1.0 - c);
}

// The dust's optical depth across the plane at (x, y), analytic (the disc shader adds texture detail).
float galDustAt(vec2 xy) {
    float r = length(xy);
    float th = atan(xy.y, xy.x);
    float R = u_Gal0.x;
    float ridge = galRidge(r, u_Gal2.x, u_Gal0.z, u_Rot.w);
    float lanes = galDust(galPhase(th, ridge, 2.0)) * (0.35 + u_Gal0.w)
        + 0.5 * u_Gal1.x * galDust(galPhase(th, ridge, 4.0));
    float floc = btsValueNoise2(vec2(log(max(r, 0.5)) * 6.0, (th + log(max(r, 0.5)) * u_Gal0.z) * 3.0));
    lanes = mix(lanes, 1.6 * smoothstep(0.45, 0.85, floc), u_Gal1.y);
    float radial = exp(-r / (1.4 * u_Gal0.y)) * smoothstep(0.2, 1.2, r) * (1.0 - smoothstep(0.8 * R, R, r));
    return u_Rot.z * radial * (0.25 + lanes) * u_Gal2.z * u_Gal2.z;
}

// How much of a star's light the dust lets through on its way to the eye.
float galDustSeen(vec3 P) {
    vec3 E = u_EyeD.xyz;
    vec3 v = P - E;
    float len = length(v);
    if (len < 1e-3) return 1.0;
    float cosi = max(abs(v.z) / len, 0.06);
    float side = E.z >= 0.0 ? 1.0 : -1.0;
    // 1 when the star is well behind the dust layer (0.12 blocks thick), 0 in front of it
    float behind = smoothstep(-1.0, 1.0, -P.z * side / 0.12);
    if (behind < 0.01) return 1.0;
    vec2 xy = abs(v.z) > 1e-4 ? (E + v * clamp(-E.z / v.z, 0.0, 1.0)).xy : P.xy;
    return exp(-galDustAt(xy) * behind / cosi);
}

// One star of the galaxy (mode 0) or the companion (mode 1, in its own frame): disc-frame position, brightness,
// pinkness (H II) and softness (glow sprites).
vec3 galMain(vec3 p, float group, out float bright, out float pink, out float soft) {
    float t = u_Gal2.y;
    float f = u_Gal2.z;
    bright = 1.0;
    pink = 0.0;
    soft = 0.0;
    float h1 = btsHash12(p.xy * 7.13 + group);
    float h2 = btsHash12(p.yz * 3.71 + 11.0);
    float h3 = btsHash12(p.zx * 5.29 + 23.0);
    vec3 pos;
    float rStar;
    if (group < 1.5) {
        // the disc: old (0) and young (1) stars on a flat rotation curve, bunched toward the arms
        float r = p.x;
        rStar = r;
        float th = p.y + galTurnAngle(galTurns(r, u_Rot.x, u_Rot.y), t);
        float ridge = galRidge(r, u_Gal2.x, u_Gal0.z, u_Rot.w);
        float ph2 = galPhase(th, ridge, 2.0);
        float ph4 = galPhase(th, ridge, 4.0);
        float young = group > 0.5 ? 1.0 : 0.0;
        float a2 = u_Gal0.w * (young > 0.5 ? 0.8 : 0.4);
        float a4 = u_Gal1.x * (young > 0.5 ? 0.6 : 0.4);
        float thd = th - a2 * sin(ph2) / 2.0 - a4 * sin(ph4) / 4.0;
        pos = vec3(r * cos(thd), r * sin(thd), p.z);
        if (young > 0.5) {
            float arm = galYoung(ph2) + 0.55 * (u_Gal1.x / max(u_Gal0.w, 0.05)) * galYoung(ph4);
            // a flocculent galaxy: patches of young stars lit at random along short sheared arcs
            float patch = smoothstep(0.35, 0.8, btsValueNoise2(vec2(log(max(r, 0.5)) * 7.0,
                (th + log(max(r, 0.5)) * u_Gal0.z) * 2.5 + floor(t / 600.0) * 3.7)));
            arm = mix(arm, 0.25 + 1.4 * patch, u_Gal1.y);
            bright = u_Gal1.w * (0.12 + 1.6 * arm) * smoothstep(0.35, 1.0, f);
            // the ring wave of a satellite crossing: star formation where it passes
            if (u_Ev.x > 0.0) {
                float d = (r - u_Ev.x) / 1.3;
                float wave = exp(-d * d);
                bright += u_Ev.y * wave * 2.4;
                pink = max(pink, 0.6 * u_Ev.y * wave);
            }
        } else {
            bright = 0.85 + 0.3 * galCrowd(ph2, 0.5 * u_Gal0.w);
        }
    } else if (group < 2.5) {
        // the bulge: old red-orange stars on random orbits, the whole turning slowly
        pos = galRotate(p, vec3(0.0, 0.0, 1.0), galTurnAngle(30.0, t) + h1 * 0.2);
        rStar = length(p.xy);
    } else if (group < 3.5) {
        // the bar turns with the arms
        pos = galRotate(p, vec3(0.0, 0.0, 1.0), u_Gal2.x);
        rStar = length(p.xy);
    } else if (group < 5.5) {
        // a star-forming cluster (4) or its H II glow (5): born on an arm (or anywhere, flocculent), sheared by the
        // rotation into a short trailing arc, fading as it ages; 180 s per generation, 20 generations an hour
        float id = floor(p.x / 4.0);
        float dr = p.x - 4.0 * id - 1.5;
        float L = 180.0;
        float ph0 = btsHash12(vec2(id, 3.1));
        float u = t / L + ph0;
        float gen = mod(floor(u), 20.0);
        float age = fract(u);
        vec2 hh = btsHash22(vec2(id * 1.37 + gen * 17.0, gen + 0.5));
        float R = u_Gal0.x;
        float rc = mix(u_Rot.w + 0.8, 0.92 * R, sqrt(hh.x));
        float pattern0 = u_Gal2.x - BTS_TAU * u_Gal1.z * (age * L) / GAL_HOUR;
        float onArm = galRidge(rc, pattern0, u_Gal0.z, u_Rot.w) + (hh.y < 0.5 ? 0.0 : BTS_PI) + 0.12;
        float anywhere = hh.y * BTS_TAU;
        float thc = mix(onArm, anywhere, max(u_Gal1.y, step(0.86, fract(hh.y * 7.0))));
        float r = max(0.6, rc + dr * (0.35 + 0.02 * rc));
        float om = u_Rot.x / sqrt(r * r + u_Rot.y * u_Rot.y);
        float th = thc + p.y + om * (age * L);
        pos = vec3(r * cos(th), r * sin(th), p.z);
        rStar = r;
        float env = smoothstep(0.0, 0.06, age) * pow(1.0 - age, 1.6);
        if (group > 4.5) {
            env = smoothstep(0.0, 0.05, age) * (1.0 - smoothstep(0.15, 0.55, age));
            soft = 1.0;
            pink = 1.0;
        }
        bright = env * smoothstep(0.45, 1.0, f) * (0.6 + 0.4 * u_Gal1.w);
    } else if (group < 6.5) {
        // the stellar halo and its globular clusters
        pos = galRotate(p, vec3(0.0, 0.0, 1.0), galTurnAngle(4.0, t));
        rStar = length(p);
    } else {
        // the Magellanic Clouds, drifting slowly round the disc
        pos = galRotate(p, vec3(0.0, 0.0, 1.0), galTurnAngle(2.0, t));
        rStar = length(p);
    }
    // the halo capture: every star starts on a random orbit in a round halo and settles into its place
    if (f < 0.999) {
        vec3 dir = normalize(vec3(h1, h2, h3) * 2.0 - 1.0 + vec3(1e-3));
        float rh = mix(3.0, 17.0, pow(fract(h1 * 13.7 + h2), 0.7)) * mix(0.15, 1.0, u_Ev.w);
        vec3 haloPos = galRotate(dir * rh, normalize(vec3(h2 - 0.5, h3 - 0.5, 1.0)), galTurnAngle(floor(8.0 + 20.0 * h3), t));
        float settle = smoothstep(0.0, 1.0, clamp(f * 1.25 - 0.25 * h2, 0.0, 1.0));
        pos = mix(haloPos, pos, settle);
        bright *= mix(0.55, 1.0, settle);
    }
    return pos;
}

// One star: its disc-frame position, colour weight and look.
vec3 galPlace(vec3 p, float group, out float bright, out float pink, out float soft) {
    bright = 1.0;
    pink = 0.0;
    soft = 0.0;
    if (u_Mode < 0.5) {
        vec3 pos = galMain(p, group, bright, pink, soft);
        // a pericentre starburst: both cores and the young stars flare pink-blue
        float r = length(pos.xy);
        bright *= 1.0 + u_Ev.z * (group > 0.5 && group < 1.5 ? 1.6 : 0.4) * exp(-r * r / 60.0);
        pink = max(pink, 0.4 * u_Ev.z * exp(-r * r / 40.0));
        return pos;
    }
    if (u_Mode < 1.5) {
        // the companion: a small spiral in its own frame, placed and tilted on its orbit
        vec3 local = galMain(p, group, bright, pink, soft);
        vec3 n = normalize(u_CompN.xyz);
        vec3 ax = normalize(abs(n.z) < 0.95 ? cross(n, vec3(0.0, 0.0, 1.0)) : cross(n, vec3(1.0, 0.0, 0.0)));
        vec3 ay = cross(n, ax);
        bright *= u_Comp.w * (1.6 + 1.0 * u_Ev.z);
        return u_Comp.xyz + u_CompN.w * (local.x * ax + local.y * ay + local.z * n);
    }
    if (u_Mode < 2.5) {
        // the dwarf: a small elliptical blob stretched along its fall by the tide
        vec3 v = normalize(u_SatV.xyz);
        vec3 q = p * u_Tail.w;
        q += v * dot(q, v) * (u_SatV.w - 1.0);
        bright = u_Sat.w;
        return u_Sat.xyz + q;
    }
    // the tidal tails (0 the companion's, 1 the galaxy's counter-tail) and the bridge (2)
    float s = p.x;
    float g = u_Tail.x;
    vec3 C = u_Comp.xyz;
    vec3 pos;
    if (group < 0.5) {
        vec3 n = normalize(u_CompN.xyz);
        pos = galRotate(C, n, -u_Tail.z * s * 2.4 * g) * (1.0 + 0.25 * s * g);
        pos += normalize(pos + vec3(1e-3)) * p.y * (0.6 + 2.4 * s) + n * p.z * (0.5 + 1.2 * s);
        bright = 2.4 * g * (1.0 - 0.6 * s) * smoothstep(0.0, 0.08, s);
    } else if (group < 1.5) {
        vec2 away = -normalize(C.xy + vec2(1e-3, 0.0));
        float a = atan(away.y, away.x) + u_Tail.z * s * 1.9 * g;
        float rr = u_Gal0.x * (0.62 + 0.42 * s * g);
        pos = vec3(rr * cos(a), rr * sin(a), u_Tail.z * s * s * 4.0 * g) + vec3(p.y, p.z, p.z * 0.5) * (0.7 + 2.0 * s);
        bright = 2.4 * g * (1.0 - 0.5 * s) * smoothstep(0.0, 0.1, s);
    } else {
        vec3 bow = normalize(cross(C + vec3(1e-3), vec3(0.0, 0.0, 1.0)) + vec3(0.0, 0.0, 0.3));
        pos = C * s + bow * sin(BTS_PI * s) * 2.2 + vec3(p.y, p.z, p.y) * 0.7;
        bright = 2.0 * u_Tail.y * (0.5 + 0.5 * sin(BTS_PI * s));
        pink = 0.5;
    }
    // nothing may reach the Eye's blocks (20.4 blocks out)
    float len = length(pos);
    if (len > 19.6) pos *= 19.6 / len;
    return pos;
}

// The whole vertex: sets gl_PointSize and returns (rgb, brightness) for the fragment; pos is the disc-frame
// position (the caller projects it through the model matrix).
vec4 galVertex(vec2 uv, float wClip, vec3 pos, float bright, float pink, out float px) {
    float group = floor(uv.x / 8.0);
    float size = uv.x - 8.0 * group;
    float packedRgb = uv.y;
    float cr = floor(packedRgb / 65536.0);
    float cg = floor((packedRgb - cr * 65536.0) / 256.0);
    float cb = packedRgb - cr * 65536.0 - cg * 256.0;
    vec3 col = vec3(cr, cg, cb) / 255.0;
    col = mix(col, vec3(1.0, 0.42, 0.68), clamp(pink, 0.0, 1.0));
    float raw = size * u_Points.x / max(wClip, 0.05);
    float sz = clamp(raw, u_Points.y, u_Points.z);
    // a point drawn larger than it is keeps its light: fade by the area it gained
    float fade = raw < sz ? (raw * raw) / (sz * sz) : 1.0;
    px = sz;
    gl_PointSize = sz;
    float b = bright * fade * galDustSeen(pos) * u_Gal2.w;
    return vec4(col, b);
}
