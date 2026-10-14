// Beyond the Stars: the Eye's stars (renders v2 §7.2, §8.2), everything round a star (bts_starhalo) in both profiles.
// One camera-facing quad just in front of the star (or just in front of the camera when it stands inside the Eye's
// clearance); each fragment integrates its view ray analytically through the corona, the glow, a giant's haze and
// wind, a protostar's dusty envelope, disc and outflows, the lighthouse beams, the magnetosphere and up to two thin
// shells (shock fronts, flares, flashes, the supernova remnant). Whatever lies behind the star along the ray is cut by
// a ray / sphere test, so the quad needs no depth of its own. Premultiplied output: (ONE, ONE_MINUS_SRC_ALPHA); alpha
// is what the dust absorbs.
#include "star/lib.glsl"

uniform sampler2D u_Noise;
uniform sampler2D u_Blackbody;
uniform vec3 u_Centre;
// the eye relative to the render origin (Minecraft's first-person camera sits 0.1 blocks behind it)
uniform vec3 u_Eye;
// x radius (blocks), y temperature K, z time s, w mode (ST_*)
uniform vec4 u_Star;
// xyz the magnetic axis (world, unit), w the beam pulse 0..1 (flashes as a beam sweeps past the camera)
uniform vec4 u_Mag;
// x glow intensity, y glow scale (radii), z corona intensity, w corona reach (radii)
uniform vec4 u_Glow;
// x streamer contrast, y flicker 0..1, z haze intensity, w haze reach (radii)
uniform vec4 u_Corona;
// x envelope density, y polar clearing 0..1, z disc density, w outflow intensity
uniform vec4 u_Env;
// x beam intensity, y beam length (blocks), z half width at the root (blocks), w spread (half-angle tangent)
uniform vec4 u_Beam;
// shells: x radius (blocks), y thickness, z intensity, w kind (0 off, 1 shock, 2 remnant, 3 flare, 4 flash, 5 soft)
uniform vec4 u_Shell;
uniform vec4 u_Shell2;
uniform vec3 u_ShellCol;
uniform vec3 u_Shell2Col;
// x flash intensity, y magnetosphere glow, z wind, w clearance (blocks: nothing reaches past it)
uniform vec4 u_Flash;
// the corona / haze colour
uniform vec3 u_HaloTint;

// One thin shell seen along rd: rim-bright, broken by noise; the back half is cut where the star hides it.
vec3 stShell(vec3 rd, vec3 C, float tc, float b, float tHit, vec4 sh, vec3 col, float t, vec3 v) {
    float kind = sh.w;
    float S = sh.x;
    float w = max(sh.y, 0.05);
    if (kind < 0.5 || sh.z <= 0.0) return vec3(0.0);
    if (kind < 1.5) {
        // a shock front's Rayleigh-Taylor fingers push its edge out in places
        S *= 1.0 + 0.10 * (btsFbm3(u_Noise, v * 3.5 + vec3(0.0, 0.0, 0.15 * t), 2) - 0.5) * 2.0;
    }
    if (b >= S) return vec3(0.0);
    float h = sqrt(S * S - b * b);
    float si = max(S - w, 0.0);
    float hIn = sqrt(max(si * si - b * b, 0.0));
    // rim-bright: a face-on crossing counts once, a grazing one up to six times
    float path = (h - hIn) / w;
    float rim = 0.3 + 0.7 * min(path * sqrt(path), 8.0);
    // seen from inside, a shell is only a faint veil (it runs past the camera)
    float inside = dot(C, C) < S * S ? 0.45 : 1.0;
    vec3 sum = vec3(0.0);
    for (int k = 0; k < 2; k++) {
        float tt = k == 0 ? tc - 0.5 * (h + hIn) : tc + 0.5 * (h + hIn);
        if (tt <= 0.0) continue;
        if (tHit > 0.0 && tt > tHit) continue;
        vec3 P = tt * rd - C;
        vec3 d = P / max(length(P), 1e-4);
        float mask;
        vec3 c = col;
        if (kind < 1.5) {
            // shock: bright where the front is thin, ragged fingers
            float n = btsFbm3(u_Noise, d * 4.0 + vec3(0.0, 0.0, 0.2 * t), 3);
            float fing = pow(1.0 - abs(2.0 * n - 1.0), 3.0);
            mask = 0.04 + 1.1 * fing * smoothstep(0.3, 0.55, n);
        } else if (kind < 2.5) {
            // remnant: thin red-orange filaments over the shell
            float n = btsFbm3(u_Noise, d * 3.0 + vec3(1.0, 2.0, 0.05 * t), 3);
            float fil = pow(1.0 - abs(2.0 * n - 1.0), 8.0);
            float n2 = btsNoise3(u_Noise, d * 11.0 + vec3(3.0));
            mask = 0.25 + 2.2 * fil * (0.6 + 0.8 * n2);
            c = mix(col, vec3(1.0, 0.35, 0.12), smoothstep(0.2, 0.8, fil));
        } else if (kind < 3.5) {
            // a magnetar flare: torn, patchy
            float n = btsNoise3(u_Noise, d * 5.0 + vec3(0.0, 0.0, 0.8 * t)) * 0.65
                + btsNoise3(u_Noise, d * 13.0 + vec3(5.0, 3.0, 1.6 * t)) * 0.35;
            mask = smoothstep(0.38, 0.68, n) * 1.4 + 0.1;
        } else if (kind < 4.5) {
            float n = btsNoise3(u_Noise, d * 3.0 + vec3(0.0, 0.0, 0.5 * t));
            mask = 0.7 + 0.6 * n;
        } else {
            mask = 0.85 + 0.3 * btsNoise3(u_Noise, d * 2.5 + vec3(0.0, 0.0, 0.3 * t));
        }
        sum += c * mask;
    }
    return sum * rim * inside * sh.z;
}

// A beam along the unit axis a from the star (root at r0, tip at L): its light on the ray, cut behind the star.
vec3 stBeam(vec3 rd, vec3 C, float tHit, vec3 a, float r0, vec4 bm, vec3 core, vec3 edge, float pointing) {
    float B = dot(rd, a);
    float Dd = -dot(rd, C);
    float E = -dot(a, C);
    float den = 1.0 - B * B;
    float sl = den > 1e-4 ? (E - B * Dd) / den : r0;
    sl = clamp(sl, r0, bm.y);
    vec3 Q = C + sl * a;
    float tr = dot(Q, rd);
    if (tr <= 0.0) return vec3(0.0);
    if (tHit > 0.0 && tr > tHit) return vec3(0.0);
    float dist = length(tr * rd - Q);
    float wid = bm.z + (sl - r0) * bm.w;
    float x = dist / wid;
    float prof = exp(-x * x * 2.2);
    float coreP = exp(-x * x * 14.0);
    float along = clamp((sl - r0) / max(bm.y - r0, 1e-3), 0.0, 1.0);
    float fall = pow(1.0 - along, 1.6) * smoothstep(0.0, 0.04, along);
    // looking down the beam the light piles up along the line of sight
    float los = min(1.0 / max(sqrt(den), 0.18), 4.0);
    float flash = 1.0 + 6.0 * pow(max(pointing, 0.0), 8.0);
    return (edge * prof + core * coreP * 1.6) * fall * los * flash * bm.x;
}

vec4 starHalo(vec3 rd) {
    float R = u_Star.x;
    float T = u_Star.y;
    float t = u_Star.z;
    float mode = u_Star.w;
    vec3 C = u_Centre - u_Eye;
    float D = length(C);
    float tc = dot(C, rd);
    vec3 m = C - tc * rd;
    float b = length(m);
    float x = b / R;
    // a negative mode: nothing occludes (the supernova's star is gone)
    float tHit = (mode >= 0.0 && b < R && tc > 0.0) ? tc - sqrt(R * R - b * b) : -1.0;
    // the unit vector from the centre to the ray's closest point (perpendicular to the ray)
    vec3 v = b > 1e-5 ? -m / b : vec3(0.0, 1.0, 0.0);
    // the screen's "up" from the spin axis, for the corona's equatorial streamers
    vec3 up = vec3(0.0, 1.0, 0.0) - rd * rd.y;
    up = dot(up, up) > 1e-6 ? normalize(up) : vec3(1.0, 0.0, 0.0);
    float lat = dot(v, up);
    float clr = u_Flash.w;
    float clearFade = 1.0 - smoothstep(clr * 0.82, clr, b);

    vec3 em = vec3(0.0);
    float alpha = 0.0;
    vec3 starCol = stKelvin(u_Blackbody, T, 1.15);

    // ---- glow: the light scattered round the star (a tight glare on the compact objects)
    if (u_Glow.x > 0.0 && tc > 0.0) {
        float over = max(x - 1.0, 0.0);
        float g = exp(-over / max(u_Glow.y, 0.01)) * 0.75 + 0.25 / (1.0 + over * over / (u_Glow.y * u_Glow.y * 9.0));
        g *= mode < 0.0 ? 1.0 : smoothstep(0.965, 1.0, x);
        em += starCol * g * u_Glow.x * clearFade;
    }

    // ---- corona: radial streamers, densest round the equator, flickering with unrest
    if (u_Glow.z > 0.0 && x > 0.97 && tc > 0.0) {
        float lx = log(max(x, 1.0));
        float str = btsFbm3(u_Noise, v * 4.5 + vec3(0.0, 0.0, lx * 0.6 - 0.015 * t), 3);
        float str2 = btsNoise3(u_Noise, v * 13.0 + vec3(0.0, 0.0, lx * 1.2 - 0.03 * t));
        float rays = clamp(0.45 + u_Corona.x * ((str - 0.45) * 2.6 + (str2 - 0.5) * 0.6), 0.0, 2.0);
        float eq = 0.45 + 0.55 * (1.0 - lat * lat);
        float fade = 1.0 - smoothstep(u_Glow.w * 0.55, u_Glow.w, x);
        float fl = 1.0 + u_Corona.y * 0.5 * (btsNoise2(u_Noise, vec2(t * 9.0, 3.0)) - 0.5);
        float c = u_Glow.z * rays * eq * pow(max(x, 1.0), -2.4) * fade * fl * smoothstep(0.98, 1.06, x);
        em += u_HaloTint * c * clearFade;
    }

    // ---- a giant's extended atmosphere and its mass-loss wind
    if (u_Corona.z > 0.0 && tc > 0.0) {
        float over = max(x - 1.0, 0.0);
        float hz = exp(-over / max(u_Corona.w - 1.0, 0.02) * 2.2) * smoothstep(0.9, 1.0, x);
        float n = btsFbm3(u_Noise, v * 3.0 + vec3(0.0, 0.0, x * 1.5 - 0.04 * t), 3);
        em += u_HaloTint * u_Corona.z * hz * (0.7 + 0.6 * n) * clearFade;
        if (u_Flash.z > 0.0 && x > 1.0) {
            float wisp = btsFbm3(u_Noise, v * 6.0 + vec3(0.0, 0.0, x * 2.5 - 0.12 * t), 3);
            float wf = smoothstep(0.55, 0.85, wisp) * exp(-over / max(u_Corona.w * 1.4 - 1.0, 0.05));
            em += u_HaloTint * 0.6 * u_Flash.z * wf * clearFade;
        }
    }

    // ---- the magnetosphere: a soft violet glow, squashed toward the magnetic equator
    if (u_Flash.y > 0.0 && tc > 0.0) {
        float over = max(b - R, 0.0);
        float pm = abs(dot(v, normalize(u_Mag.xyz - rd * dot(u_Mag.xyz, rd) + vec3(1e-5))));
        float g = exp(-over / (0.6 + 0.6 * R)) * (0.55 + 0.45 * (1.0 - pm * pm));
        em += vec3(0.55, 0.32, 1.0) * u_Flash.y * g * smoothstep(0.9, 1.0, x) * clearFade;
    }

    // ---- a protostar's cocoon: dusty, absorbing, lit from inside, clearing from the poles
    if (u_Env.x > 0.0) {
        float E = R * 1.75;
        if (b < E) {
            float hE = sqrt(E * E - b * b);
            float tIn = max(tc - hE, 0.0);
            float tOut = tHit > 0.0 ? tHit : tc + hE;
            float len = max(tOut - tIn, 0.0) / R;
            vec3 Pm = (0.5 * (tIn + tOut)) * rd - C;
            vec3 dm = Pm / max(length(Pm), 1e-4);
            float holes = btsFbm3(u_Noise, dm * 3.2 + vec3(0.0, 0.0, 0.02 * t), 3);
            float clearing = 1.0 - u_Env.y * smoothstep(0.25, 0.85, abs(dm.y));
            float tau = u_Env.x * len * smoothstep(0.25, 0.7, holes) * 1.6 * clearing * pow(max(1.0 - (b * b) / (E * E), 0.0), 1.5);
            float a = 1.0 - exp(-tau);
            // scattered starlight: warm brown, brightest just over the star
            float lit = 0.25 + 0.75 * exp(-max(x - 1.0, 0.0) * 2.5);
            em = em * (1.0 - a) + vec3(0.2, 0.08, 0.035) * a * lit * (0.6 + 0.6 * holes);
            alpha = a;
        }
    }

    // ---- a protostar's thin dusty disc in its equator, dark against the star with a hot inner rim
    if (u_Env.z > 0.0 && abs(rd.y) > 1e-4) {
        float tp = C.y / rd.y;
        if (tp > 0.0 && (tHit < 0.0 || tp < tHit)) {
            vec3 P = tp * rd - C;
            float r = length(P.xz) / R;
            if (r > 1.12 && r < 2.9) {
                float ring = 0.75 + 0.5 * btsNoise3(u_Noise, vec3(normalize(P.xz) * 4.0, r * 9.0 + 0.1 * t));
                float prof = smoothstep(1.12, 1.3, r) * (1.0 - smoothstep(2.0, 2.9, r));
                float tau = u_Env.z * prof * ring / max(abs(rd.y), 0.12) * 0.35;
                float a = 1.0 - exp(-tau);
                float rim = exp(-(r - 1.15) * 5.0);
                vec3 dc = vec3(0.30, 0.10, 0.04) * (0.25 + 1.6 * rim) + starCol * rim * 0.5;
                em = em * (1.0 - a) + dc * a;
                alpha = 1.0 - (1.0 - alpha) * (1.0 - a);
            }
        }
    }

    // ---- lighthouse beams (neutron star, magnetar) and a protostar's bipolar outflows
    vec3 toCam = -C / max(D, 1e-4);
    if (u_Beam.x > 0.0) {
        vec3 a = u_Mag.xyz;
        vec3 core = vec3(0.92, 0.96, 1.0);
        vec3 edge = mode > 4.5 ? vec3(0.62, 0.34, 1.0) : vec3(0.45, 0.6, 1.0);
        float pulse = 1.0 + u_Mag.w;
        em += stBeam(rd, C, tHit, a, R * 0.95, u_Beam, core, edge, dot(a, toCam)) * pulse * clearFade;
        em += stBeam(rd, C, tHit, -a, R * 0.95, u_Beam, core, edge, dot(-a, toCam)) * pulse * clearFade;
    }
    if (u_Env.w > 0.0) {
        vec4 ob = vec4(u_Env.w, R * 2.8, R * 0.12, 0.16);
        vec3 knots = vec3(1.0, 0.45, 0.55);
        em += stBeam(rd, C, tHit, vec3(0.0, 1.0, 0.0), R * 1.05, ob, knots, vec3(0.75, 0.25, 0.32), 0.0) * clearFade;
        em += stBeam(rd, C, tHit, vec3(0.0, -1.0, 0.0), R * 1.05, ob, knots, vec3(0.75, 0.25, 0.32), 0.0) * clearFade;
    }

    // ---- shells
    em += stShell(rd, C, tc, b, tHit, u_Shell, u_ShellCol, t, v);
    em += stShell(rd, C, tc, b, tHit, u_Shell2, u_Shell2Col, t, v);

    // ---- a white flash (giant flare, ignition, the supernova's first moments)
    if (u_Flash.x > 0.0 && tc > 0.0) {
        float f = exp(-max(x - 1.0, 0.0) * 0.9) * (mode < 0.0 ? 1.0 : smoothstep(0.85, 1.0, x)) + 0.35 / (1.0 + x * x * 0.05);
        em += vec3(1.0, 0.96, 0.9) * u_Flash.x * f;
    }

    if (dot(em, em) < 1e-7 && alpha < 0.002) return vec4(-1.0);
    return vec4(btsShoulder(em, 1.5), alpha);
}
