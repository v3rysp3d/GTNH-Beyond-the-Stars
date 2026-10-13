// Beyond the Stars: the Eye's planets mode (renders v2 §9.1), the planets and their orbits (bts_galplan) in both
// profiles. One quad in front of the system; each fragment intersects its ray with up to nine planets (analytic
// spheres on their orbits in the tilted disc plane, each at its own Keplerian speed) and draws the nearest: lit toward
// the sun at the centre with a soft terminator, a surface by kind (rocky and cratered, cloud-veiled, an ocean world
// with land and clouds, a desert, banded gas giants, a ringed giant, ice giants), a thin atmosphere rim; behind them
// the faint orbit lines where the ray crosses the plane. Premultiplied (ONE, ONE_MINUS_SRC_ALPHA); the sun is the
// star programs' (EyeStarFx), drawn round it.
#include "galaxy/lib.glsl"
#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
uniform vec3 u_Centre;
// the eye relative to the render origin
uniform vec3 u_Eye;
// the disc's in-plane axes (camera space, unit)
uniform vec3 u_AxX;
uniform vec3 u_AxY;
// x planets shown (0..9), y time (s), z the sun's radius, w brightness
uniform vec4 u_Plan;

// Planet i: orbit radius, size, kind (0 rocky, 1 veiled, 2 ocean, 3 desert, 4 banded, 5 ringed, 6 ice, 7 deep ice,
// 8 dwarf), whole turns an hour (Keplerian: period proportional to r^1.5).
vec4 galPlanet(int i) {
    if (i == 0) return vec4(5.0, 0.32, 0.0, 90.0);
    if (i == 1) return vec4(6.6, 0.5, 1.0, 59.0);
    if (i == 2) return vec4(8.2, 0.55, 2.0, 43.0);
    if (i == 3) return vec4(9.8, 0.42, 3.0, 33.0);
    if (i == 4) return vec4(11.6, 1.25, 4.0, 26.0);
    if (i == 5) return vec4(13.4, 1.05, 5.0, 21.0);
    if (i == 6) return vec4(15.0, 0.78, 6.0, 17.0);
    if (i == 7) return vec4(16.5, 0.74, 7.0, 15.0);
    return vec4(17.8, 0.26, 8.0, 13.0);
}

vec3 galPlanetPos(int i, vec4 p) {
    float a = btsHash12(vec2(float(i) * 3.7, 1.3)) * BTS_TAU + galTurnAngle(p.w, u_Plan.y);
    return vec3(p.x * cos(a), p.x * sin(a), 0.0);
}

// The surface colour of a planet of kind k at the unit normal n (its own frame, z its axis), turning with time.
vec3 galSurface(float k, vec3 n, float t, float id) {
    float spin = t * (0.05 + 0.03 * id);
    float c = cos(spin);
    float s = sin(spin);
    vec3 q = vec3(c * n.x - s * n.y, s * n.x + c * n.y, n.z);
    float lat = n.z;
    if (k < 0.5) {
        float cr = btsFbm3(u_Noise, q * 3.0 + id, 3);
        float pit = smoothstep(0.62, 0.7, btsNoise3(u_Noise, q * 9.0 + 4.0));
        return vec3(0.52, 0.49, 0.46) * (0.6 + 0.6 * cr) * (1.0 - 0.35 * pit);
    }
    if (k < 1.5) {
        float sw = btsFbm3(u_Noise, vec3(q.x * 2.0, q.y * 2.0, lat * 6.0) + vec3(0.0, 0.0, t * 0.02), 3);
        return mix(vec3(0.86, 0.76, 0.52), vec3(0.98, 0.92, 0.74), sw);
    }
    if (k < 2.5) {
        float land = smoothstep(0.52, 0.58, btsFbm3(u_Noise, q * 1.8 + 7.0, 4));
        vec3 ground = mix(vec3(0.20, 0.42, 0.18), vec3(0.55, 0.45, 0.30), btsNoise3(u_Noise, q * 4.0));
        vec3 col = mix(vec3(0.07, 0.20, 0.52), ground, land);
        col = mix(col, vec3(0.92), smoothstep(0.78, 0.9, abs(lat)));
        float cloud = smoothstep(0.55, 0.75, btsFbm3(u_Noise, q * 2.6 + vec3(t * 0.03, 0.0, 3.0), 3));
        return mix(col, vec3(0.95), cloud * 0.8);
    }
    if (k < 3.5) {
        float d = btsFbm3(u_Noise, q * 2.4 + 2.0, 4);
        vec3 col = mix(vec3(0.62, 0.30, 0.16), vec3(0.82, 0.52, 0.32), d);
        return mix(col, vec3(0.9, 0.88, 0.86), smoothstep(0.86, 0.94, abs(lat)));
    }
    if (k < 5.5) {
        // banded giants: zones and belts stirred by turbulence, a storm on the banded one
        float turb = btsFbm3(u_Noise, vec3(q.xy * 3.0, lat * 2.0) + vec3(t * 0.01, 0.0, 0.0), 3);
        float bands = sin(lat * (k < 4.5 ? 15.0 : 11.0) + turb * 2.6);
        vec3 a = k < 4.5 ? vec3(0.80, 0.66, 0.48) : vec3(0.86, 0.78, 0.56);
        vec3 b = k < 4.5 ? vec3(0.62, 0.40, 0.26) : vec3(0.74, 0.62, 0.40);
        vec3 col = mix(b, a, 0.5 + 0.5 * bands);
        if (k < 4.5) {
            vec2 sp = vec2(atan(q.y, q.x) - 1.0, lat + 0.35);
            float storm = exp(-(sp.x * sp.x * 6.0 + sp.y * sp.y * 90.0));
            col = mix(col, vec3(0.78, 0.36, 0.24), storm * 0.8);
        }
        return col;
    }
    if (k < 6.5) {
        return mix(vec3(0.55, 0.82, 0.86), vec3(0.68, 0.9, 0.92), 0.5 + 0.5 * sin(lat * 9.0));
    }
    if (k < 7.5) {
        float bands = sin(lat * 10.0 + btsFbm3(u_Noise, q * 2.0, 2) * 2.0);
        vec3 col = mix(vec3(0.20, 0.35, 0.80), vec3(0.32, 0.50, 0.92), 0.5 + 0.5 * bands);
        float spot = exp(-dot(q.xy - vec2(0.6, 0.3), q.xy - vec2(0.6, 0.3)) * 30.0) * step(0.0, q.z);
        return mix(col, vec3(0.08, 0.12, 0.35), spot);
    }
    return vec3(0.62, 0.55, 0.48) * (0.7 + 0.5 * btsFbm3(u_Noise, q * 3.0, 2));
}

vec4 galPlanets(vec3 rdCam) {
    vec3 n3 = normalize(cross(u_AxX, u_AxY));
    vec3 oc = u_Eye - u_Centre;
    vec3 o = vec3(dot(oc, u_AxX), dot(oc, u_AxY), dot(oc, n3));
    vec3 d = vec3(dot(rdCam, u_AxX), dot(rdCam, u_AxY), dot(rdCam, n3));
    float t = u_Plan.y;
    int count = int(u_Plan.x + 0.5);
    // where the ray crosses the plane (the orbit lines), with its footprint taken in uniform control flow
    float tp = abs(d.z) > 1e-4 ? -o.z / d.z : -1.0;
    float rPlane = length((o + d * max(tp, 0.0)).xy);
    float wLine = max(0.03, 0.8 * fwidth(rPlane));
    float best = 1e9;
    vec3 col = vec3(0.0);
    float alpha = 0.0;
    // the nearest planet or ring along the ray
    for (int i = 0; i < 9; i++) {
        if (i >= count) break;
        vec4 p = galPlanet(i);
        vec3 P = galPlanetPos(i, p);
        vec3 q = o - P;
        float b = dot(q, d);
        float c = dot(q, q) - p.y * p.y;
        float h = b * b - c;
        // the ringed giant's rings: a tilted annulus round it
        if (p.z > 4.5 && p.z < 5.5) {
            vec3 ax = normalize(vec3(0.35, 0.0, 1.0));
            float dn = dot(d, ax);
            if (abs(dn) > 1e-4) {
                float tr = -dot(q, ax) / dn;
                if (tr > 0.0 && tr < best) {
                    vec3 hit = q + d * tr;
                    float rr = length(hit) / p.y;
                    if (rr > 1.35 && rr < 2.35) {
                        float bandN = btsNoise2(u_Noise, vec2(rr * 40.0, 3.0));
                        float dens = (0.45 + 0.55 * bandN) * smoothstep(1.35, 1.45, rr) * (1.0 - smoothstep(2.2, 2.35, rr))
                            * (1.0 - 0.75 * smoothstep(1.86, 1.9, rr) * (1.0 - smoothstep(1.94, 1.98, rr)));
                        // the planet's shadow across the rings, away from the sun
                        vec3 toSun = normalize(-P);
                        vec3 w = hit - toSun * dot(hit, toSun);
                        float shadow = dot(hit, toSun) < 0.0 && length(w) < p.y ? 0.25 : 1.0;
                        float lit = 0.35 + 0.65 * abs(dot(ax, toSun));
                        best = tr;
                        col = vec3(0.86, 0.78, 0.6) * lit * shadow * dens;
                        alpha = dens * 0.85;
                    }
                }
            }
        }
        if (h <= 0.0) continue;
        float th = -b - sqrt(h);
        if (th <= 0.0 || th >= best) continue;
        vec3 nrm = normalize(q + d * th);
        vec3 L = normalize(-(P + nrm * p.y));
        float diff = dot(nrm, L);
        float lit = smoothstep(-0.12, 0.35, diff) * (0.35 + 0.65 * max(diff, 0.0));
        vec3 surf = galSurface(p.z, nrm, t, float(i));
        float rim = pow(1.0 - max(dot(nrm, -d), 0.0), 3.0);
        vec3 atm = p.z > 0.5 && p.z < 2.5 || p.z > 5.5 ? vec3(0.45, 0.65, 1.0) : vec3(0.0);
        best = th;
        col = surf * (0.04 + 1.05 * lit) + atm * rim * (0.15 + 0.85 * smoothstep(-0.2, 0.4, diff)) * 0.6;
        alpha = 1.0;
    }
    // the faint orbit lines behind the planets
    if (alpha < 1.0 && tp > 0.0 && tp < best) {
        {
            float lines = 0.0;
            for (int i = 0; i < 9; i++) {
                if (i >= count) break;
                float dr = (rPlane - galPlanet(i).x) / wLine;
                lines += exp(-dr * dr);
            }
            col += (1.0 - alpha) * vec3(0.55, 0.7, 1.0) * lines * 0.12;
        }
    }
    col *= u_Plan.w;
    if (alpha < 0.003 && max(max(col.r, col.g), col.b) < 0.002) return vec4(-1.0);
    return vec4(col, alpha);
}
