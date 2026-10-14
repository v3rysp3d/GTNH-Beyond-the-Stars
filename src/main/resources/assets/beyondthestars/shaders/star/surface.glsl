// Beyond the Stars: the Eye's stars (renders v2 §7.2, §8.2), the photosphere (bts_star) in both profiles.
// Drawn on a sphere mesh a little larger than the star; each fragment traces its view ray onto the true sphere (a
// perfect silhouette), so the surface is computed in 3D (no texture seams, no pinched poles). Opaque, writes depth.
#include "star/lib.glsl"

uniform sampler2D u_Noise;
uniform sampler2D u_Blackbody;
// the star's centre relative to the camera, blocks
uniform vec3 u_Centre;
// the eye relative to the render origin (Minecraft's first-person camera sits 0.1 blocks behind it)
uniform vec3 u_Eye;
// x radius (blocks), y temperature K, z time s, w mode (ST_*)
uniform vec4 u_Star;
// xyz the magnetic axis (world, unit), w hot spots 0..
uniform vec4 u_Mag;
// x granulation contrast, y granule frequency (cells per radius), z boil rate, w activity (spots, faculae) 0..1
uniform vec4 u_Surf;
// x limb u1, y limb u2, z exposure, w detail 0..3 (the LOD's octaves)
uniform vec4 u_Surf2;
// x crack glow, y helium-flash wave 0..1 (negative: none), z white flash, w compactness (light bending, 0 none)
uniform vec4 u_Fx;
// x crystal sheen (black dwarf), y ember glow, z saturation, w tint mix
uniform vec4 u_Fx2;
// the tint (magnetar violet), and the surface turn (cos, sin) about the spin axis
uniform vec3 u_Tint;
uniform vec2 u_Turn;

// Distance to the nearest edge of a hexagonal lattice in the plane (the black dwarf's crystal sheen).
float stHexEdge(vec2 p) {
    vec2 r = vec2(1.0, 1.7320508);
    vec2 h = r * 0.5;
    vec2 a = mod(p, r) - h;
    vec2 b = mod(p - h, r) - h;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    g = abs(g);
    return 0.5 - max(dot(g, vec2(0.5, 0.8660254)), g.x);
}

// x: brightness factor, y: temperature factor of a spot field (umbra, penumbra, faculae round them).
vec2 stSpots(vec3 s, float t, float activity, float mu, int count) {
    float I = 1.0;
    float Tf = 1.0;
    float fac = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= count) break;
        vec3 h = stHash33(vec3(float(i) * 3.7 + 1.3, 7.1, 3.3));
        float period = 70.0 + 80.0 * h.x;
        float life = fract(t / period + h.y);
        // a spot is born, grows and decays; with low activity most cycles stay empty
        float on = step(h.z, 0.25 + 0.75 * activity);
        float size = on * pow(max(sin(3.14159 * life), 0.0), 0.6) * (0.6 + 0.6 * h.z);
        if (size < 0.02) continue;
        float cyc = floor(t / period + h.y);
        vec3 h2 = stHash33(vec3(float(i) + cyc * 1.37, 2.2, 9.1));
        float lat = (h2.x - 0.5) * 1.1;
        lat += sign(lat) * 0.12;
        float lon = h2.y * 6.28318 + 0.02 * t;
        vec3 sc = vec3(cos(lat) * cos(lon), sin(lat), cos(lat) * sin(lon));
        float ang = sqrt(max(2.0 * (1.0 - dot(s, sc)), 0.0));
        float ru = 0.035 * size;
        float rp = ru * 2.4;
        if (ang > rp * 4.0) continue;
        vec3 dir = (s - sc) / max(ang, 1e-4);
        float fil = btsNoise3(u_Noise, dir * 7.0 + vec3(float(i) * 5.0, 0.0, 0.3 * t));
        float pen = 1.0 - smoothstep(rp * 0.85, rp, ang);
        float umb = 1.0 - smoothstep(ru * 0.7, ru, ang);
        I *= mix(1.0, 0.58 + 0.16 * fil, pen);
        I *= mix(1.0, 0.22, umb);
        Tf *= mix(1.0, 0.86, pen) * mix(1.0, 0.8, umb);
        fac += (1.0 - smoothstep(rp, rp * 4.0, ang)) * (1.0 - pen);
    }
    // faculae: the bright network round active regions shows toward the limb
    I += 0.35 * fac * pow(1.0 - mu, 1.5);
    return vec2(I, Tf);
}

// The photosphere seen along the camera-relative unit ray rd: rgb, a = 1 on the star (0: the ray misses it).
vec4 starSurface(vec3 rd) {
    float R = u_Star.x;
    float T = u_Star.y;
    float t = u_Star.z;
    float mode = u_Star.w;
    vec3 C = u_Centre - u_Eye;
    float tc = dot(C, rd);
    vec3 m = C - tc * rd;
    float b = length(m);
    if (b >= R || tc <= 0.0) return vec4(0.0);
    // the emission angle (from the apparent disc) and, with light bending, the surface angle psi behind it
    // (Beloborodov 2002: 1 - cos(alpha) = (1 - cos(psi)) (1 - u)): a neutron star shows more than half its surface
    float sinA = b / R;
    float mu = sqrt(max(1.0 - sinA * sinA, 0.0));
    float u = u_Fx.w;
    float cosP = 1.0 - (1.0 - mu) / max(1.0 - u, 0.05);
    float sinP = sqrt(max(1.0 - cosP * cosP, 0.0));
    vec3 e = b > 1e-5 ? -m / b : vec3(0.0, 1.0, 0.0);
    vec3 n = normalize(cosP * (-rd) + sinP * e);
    vec3 s = stTurn(n, u_Turn);
    float det = u_Surf2.w;
    // the pixel's footprint in surface units: finer detail fades out instead of shimmering
    float fp = length(fwidth(s));

    float I = 1.0;
    float Tf = 1.0;
    vec3 add = vec3(0.0);

    if (mode < 1.5) {
        // ---- main sequence (Sol, Sirius A) and giant (Arcturus, the swell): convection cells
        float fq = u_Surf.y;
        float cont = u_Surf.x;
        float boil = t * u_Surf.z;
        if (det > 0.5) {
            float fade = 1.0 - smoothstep(0.18, 0.55, fp * fq);
            if (fade > 0.0) {
                vec3 c = stCells3(s * fq, boil);
                float lane = smoothstep(0.0, mode > 0.5 ? 0.5 : 0.32, c.y - c.x);
                float body = 1.0 - 0.7 * c.x * c.x;
                float flick = 0.8 + 0.4 * fract(c.z * 13.7 + 0.05 * boil);
                float g = mix(mode > 0.5 ? 0.62 : 0.5, 1.0, lane) * body * flick;
                I *= mix(1.0, g * 1.15, cont * fade);
                Tf *= mix(1.0, 0.94 + 0.08 * g, fade);
            }
            if (det > 1.5) {
                float fq2 = fq * 2.6;
                float fade2 = 1.0 - smoothstep(0.18, 0.55, fp * fq2);
                if (fade2 > 0.0) {
                    vec3 c2 = stCells3(s * fq2 + vec3(4.1, 1.7, 2.9), boil * 1.4);
                    float lane2 = smoothstep(0.0, 0.25, c2.y - c2.x);
                    I *= mix(1.0, mix(0.7, 1.08, lane2), 0.5 * cont * fade2 * (mode > 0.5 ? 0.6 : 1.0));
                }
            }
        }
        // mesogranulation / the giant's mottling
        float meso = btsFbm3(u_Noise, s * (mode > 0.5 ? 5.0 : 3.0) + vec3(0.0, 0.0, 0.02 * boil), det > 1.5 ? 3 : 2);
        I *= 0.86 + 0.28 * meso;
        if (u_Surf.w > 0.0 && det > 0.5) {
            vec2 sp = stSpots(s, t, u_Surf.w, mu, det > 1.5 ? 6 : 4);
            I *= sp.x;
            Tf *= sp.y;
        }
    } else if (mode < 2.5) {
        // ---- protostar: a dim, blotchy, deeply convective ball
        float bl = btsFbm3(u_Noise, s * 3.5 + vec3(0.0, 0.03 * t, 0.0), det > 1.5 ? 4 : 2);
        float bl2 = btsNoise3(u_Noise, s * 9.0 + vec3(0.1 * t));
        I *= 0.55 + 0.75 * bl * (0.8 + 0.4 * bl2);
        Tf *= 0.85 + 0.25 * bl;
    } else if (mode < 3.5) {
        // ---- neutron star: a smooth, blinding crust with hot spots at the magnetic poles
        float crust = btsNoise3(u_Noise, s * 14.0 + vec3(0.0, 0.0, 0.5 * t));
        I *= 0.9 + 0.12 * crust;
        float pm = abs(dot(n, u_Mag.xyz));
        float spot = exp(-(1.0 - pm) * 26.0);
        I *= 1.0 + u_Mag.w * 2.2 * spot;
        Tf *= 1.0 + 0.6 * spot;
        add += vec3(0.9, 0.95, 1.0) * u_Mag.w * 0.5 * exp(-(1.0 - pm) * 90.0);
    } else if (mode < 4.5) {
        // ---- white dwarf: hard, dense, almost featureless; as a black dwarf a frozen crystal sheen
        // a cooling dwarf grows a convective skin: faint mottling that strengthens as it reddens
        float mot = btsFbm3(u_Noise, s * 7.0 + vec3(0.0, 0.0, 0.05 * t), 2);
        float cool = 1.0 - btsSmooth01((T - 3000.0) / 9000.0);
        I *= 1.0 + (0.06 + 0.3 * cool) * (mot - 0.5) * 2.0;
        if (u_Fx2.x > 0.0) {
            vec3 as = abs(s);
            vec2 q = as.x > as.y && as.x > as.z ? s.yz : as.y > as.z ? s.xz : s.xy;
            float edge = stHexEdge(q * 9.0);
            float fres = pow(1.0 - mu, 2.0);
            float line = 1.0 - smoothstep(0.0, 0.07 + 0.6 * fp * 9.0, edge);
            add += vec3(0.42, 0.55, 0.85) * u_Fx2.x * (0.008 + 0.22 * fres) * line;
            add += vec3(0.20, 0.27, 0.42) * u_Fx2.x * 0.12 * fres;
        }
    } else {
        // ---- magnetar: violet, hot poles, a cracked crust glowing along its faults
        float crust = btsFbm3(u_Noise, s * 6.0 + vec3(0.0, 0.0, 0.2 * t), 2);
        I *= 0.75 + 0.45 * crust;
        float pm = abs(dot(n, u_Mag.xyz));
        float spot = exp(-(1.0 - pm) * 18.0);
        I *= 1.0 + u_Mag.w * 1.6 * spot;
        Tf *= 1.0 + 0.4 * spot;
        if (u_Fx.x > 0.0 && det > 0.5) {
            vec3 c = stCells3(s * 4.5, 0.0);
            float w = 0.035 + 0.5 * fp * 4.5;
            float crack = 1.0 - smoothstep(0.0, w, c.y - c.x);
            float pulse = 0.6 + 0.4 * btsNoise3(u_Noise, s * 3.0 + vec3(0.0, 0.0, 2.0 * t));
            add += vec3(0.85, 0.7, 1.0) * crack * pulse * u_Fx.x;
        }
    }

    // limb darkening (quadratic law) and the cooler, redder limb
    float om = 1.0 - mu;
    float limb = max(1.0 - u_Surf2.x * om - u_Surf2.y * om * om, 0.0);
    I *= limb;
    Tf *= 0.80 + 0.20 * pow(mu, 0.35);

    // the helium flash: a bright front running across the surface from one point
    if (u_Fx.y >= 0.0) {
        vec3 o = normalize(vec3(0.3, 0.8, -0.5));
        float ang = acos(clamp(dot(s, o), -1.0, 1.0));
        float front = u_Fx.y * 3.4;
        float waveD = (ang - front) / 0.22;
        float wave = exp(-waveD * waveD) * (1.0 - u_Fx.y);
        I *= 1.0 + 0.9 * wave;
        Tf *= 1.0 + 0.35 * wave;
    }

    vec3 col = stKelvin(u_Blackbody, T * Tf, u_Fx2.z);
    col = mix(col, u_Tint * max(btsLuma(col), 0.35) * 1.4, u_Fx2.w);
    if (mode > 2.5 && mode < 3.5) {
        // a violet edge on the neutron star
        col = mix(col, vec3(0.62, 0.48, 1.0), 0.55 * om * om);
    }
    col *= I * u_Surf2.z;
    col += add;
    // the cooling dwarf's ember glow and the white flash of an event
    col += vec3(0.30, 0.05, 0.02) * u_Fx2.y * (0.4 + 0.6 * mu);
    col += vec3(1.0, 0.97, 0.92) * u_Fx.z;
    return vec4(btsShoulderNorm(col, 1.3), 1.0);
}
