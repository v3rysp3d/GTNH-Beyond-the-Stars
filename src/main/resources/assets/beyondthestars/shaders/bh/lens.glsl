// Beyond the Stars: the black-hole lens (renders v2 §5.2), the body of bts_blackhole in both profiles.
//
// Units: the Schwarzschild radius r_s = 1, the hole at the origin, axes = the world's (y up). Each fragment traces its
// view ray BACKWARDS from the camera through the hole's field (Schwarzschild bending, Binet form, plus a weak
// frame-dragging term for the Kerr D-shape), from where it enters the bounding sphere until it falls in (the shadow),
// leaves again (escaped) or the step budget ends. On the way it integrates the accretion disc as a thin Gaussian slab
// (exact column through each step), so the near side, the lensed far side over and under the shadow and the photon
// ring images all come from the same march. Front-to-back: col = emission so far, trans = what still shows through.
//
// Output: premultiplied colour + alpha for glBlendFunc(ONE, ONE_MINUS_SRC_ALPHA). The shadow is alpha 1 (black); a
// fragment with nothing in it is discarded by the caller. The colour pass writes no depth: a second, cheaper pass
// (u_Lens.w = 1: no screen copy, a coarse march, one octave) writes the quad's depth only where the hole has a body
// (alpha 0.05 and up: the shadow, the disc and its arcs; the glow and the photon-ring halo add light, never alpha), so
// the faint glow round it never hides what draws later (glass, water, rain, particles) while the black and the disc
// still hide what lies behind them.
//
// This include declares the program's uniforms (it is bts_blackhole's own body, not a shared library).

#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
uniform sampler2D u_Blackbody;
// the copy of the screen behind the hole (framebuffer lensing, HIGH only)
uniform sampler2D u_Frame;
// the camera in the hole frame, r_s units
uniform vec3 u_Cam;
// x: disc inner edge, y: disc outer edge, z: disc half-thickness / r, w: bounding radius (all r_s)
uniform vec4 u_Disc;
// x: peak disc temperature K, y: turbulence amplitude 0..1, z: time s, w: exposure
uniform vec4 u_Look;
// x: infall streamers 0..1, y: Doppler beaming exponent, z: photon ring gain, w: glow gain
uniform vec4 u_Look2;
// x: march steps, y: step / r, z: noise octaves, w: frame-drag strength
uniform vec4 u_Ray;
// x: framebuffer lensing 0/1, y: star brightness (lensed starfield without it), z: pixel size r_s, w: depth pass 0/1
uniform vec4 u_Lens;
// x: opening ring radius, y: its intensity, z: Hawking ring radius, w: its intensity (r_s)
uniform vec4 u_Flash;
// the copied screen rectangle in screen uv: x0, y0, x1, y1
uniform vec4 u_FrameRect;
// rows: disc x axis, disc normal (spin axis), disc z axis; u_DiscFrame * p = p in the disc's frame
uniform mat3 u_DiscFrame;
// camera-relative direction -> clip space (framebuffer lensing)
uniform mat4 u_ViewProj;

// erf, Winitzki's approximation (|error| < 2e-3, plenty for a column density)
float bhErf(float x) {
    float x2 = x * x;
    float ax2 = 0.147 * x2;
    float e = sqrt(1.0 - exp(-x2 * (1.2732395 + ax2) / (1.0 + ax2)));
    return x < 0.0 ? -e : e;
}

// The disc's gas at a point of its plane (q in the disc frame, r its radius): x = filaments 0..1, y = hot clumps
// 0..~1. Noise on a cylinder (cos, sin of the angle; log r), so it never shows a seam. Keplerian shear: the angle is
// advected by Omega(r) = 2.4 r^-1.5 rad/s over a 7.2 s cycle, two cycles half a period apart cross-fade, so the filaments shear
// and stretch like gas but never wind up into a solid disc (and the period divides the shader clock's hour).
vec2 bhGas(vec2 q, float r, float yn, float t, float octaves) {
    float lr = log(r);
    float om = 2.4 * pow(r, -1.5);
    vec2 dir = q / r;
    float fil = 0.0;
    float clump = 0.0;
    for (int k = 0; k < 2; k++) {
        float fk = float(k);
        float ph = fract(t / 7.2 + 0.5 * fk);
        float w = 1.0 - abs(2.0 * ph - 1.0);
        // the gas's direction advanced by Omega t (rotating the unit vector: no atan per sample)
        // (yn: height in half-thicknesses; a thick disc's gas twists with height instead of standing in columns)
        float a = om * ph * 7.2 + 2.3 * fk + 0.35 * yn;
        float ca = cos(a);
        float sa = sin(a);
        vec2 cs = vec2(dir.x * ca - dir.y * sa, dir.x * sa + dir.y * ca);
        // octaves unrolled (their texture reads issue together); octaves past the tier's count weigh 0
        float z = lr * 20.0 + 29.0 * fk + 0.8 * yn;
        float n0 = btsNoise3(u_Noise, vec3(cs * 3.0, z));
        float n1 = btsNoise3(u_Noise, vec3(cs * 6.09, z * 2.03 + 7.0));
        float n2 = btsNoise3(u_Noise, vec3(cs * 12.36, z * 4.12 + 14.0));
        float n3 = btsNoise3(u_Noise, vec3(cs * 25.1, z * 8.36 + 21.0));
        float w1 = step(1.5, octaves) * 0.55;
        float w2 = step(2.5, octaves) * 0.3025;
        float w3 = step(3.5, octaves) * 0.166;
        fil += w * (n0 * 0.5 + n1 * w1 * 0.5 + n2 * w2 * 0.5 + n3 * w3 * 0.5) / (0.5 + 0.5 * (w1 + w2 + w3));
        // hot clumps: a coarse field drifting inward through each cycle
        float c = btsNoise3(u_Noise, vec3(cs * 2.6 + 11.0, lr * 4.0 + ph * 1.4 + 53.0 * fk));
        clump += w * c;
    }
    return vec2(fil, clump);
}

// One march step through the disc slab: da -> db already in the disc's frame (the caller carries db to the next step),
// L the step's length, dir the ray's direction on it (hole frame).
void bhDiscStep(vec3 da, vec3 db, float L, vec3 dir, float t, inout vec3 col, inout float trans, inout float budget,
    inout vec2 gasMemo) {
    float ya = da.y;
    float yb = db.y;
    // the point of the step nearest the plane
    float s = ya * yb < 0.0 ? ya / (ya - yb) : (abs(ya) < abs(yb) ? 0.0 : 1.0);
    vec3 q = mix(da, db, s);
    float r = length(q.xz);
    float rIn = u_Disc.x;
    float rOut = u_Disc.y;
    float strm = u_Look2.x;
    float rMin = strm > 0.0 ? 1.02 : rIn * 0.9;
    if (r < rMin || r > rOut * 1.1) return;
    // thicker inside the streamer zone
    // never thinner than about a pixel, so the edge-on disc still reads from far away
    float H = max(u_Disc.z * r + 0.02, 0.6 * u_Lens.z) + strm * 0.18 * (1.0 - smoothstep(rIn, rIn * 1.6, r));
    if (ya * yb > 0.0 && min(abs(ya), abs(yb)) > 2.6 * H) return;

    // column through a Gaussian slab rho = exp(-(y / H)^2) along the step
    float dy = yb - ya;
    float column = abs(dy) < 0.05 * H ? L * exp(-(q.y * q.y) / (H * H))
        : 0.8862 * H * abs(bhErf(yb / H) - bhErf(ya / H)) * L / abs(dy);
    if (column < 1e-4) return;

    // at most a few full gas samples per ray (an edge-on ray runs along the disc for many steps): later steps reuse
    // the last one
    if (budget > 0.0) {
        gasMemo = bhGas(q.xz, r, q.y / H, t, u_Ray.z);
        budget -= 1.0;
    }
    vec2 gas = gasMemo;
    float fil = smoothstep(0.22, 0.78, gas.x);
    float ta = u_Look.y;

    // ragged outer edge, soft inner edge (the ISCO)
    float edge = rOut * (0.90 + 0.10 * gas.x);
    float outer = 1.0 - smoothstep(edge * 0.72, edge, r);
    float inner = smoothstep(rIn * 0.95, rIn * 1.3, r);
    float dens = inner * outer * mix(0.45 + 0.9 * fil, 0.05 + 1.8 * fil, ta);

    // temperature: T(r) ~ x^-3/4 (1 - x^-1/2)^1/4, normalised to 1 at its peak (x = 49/36)
    float x = max(r / rIn, 1.0);
    float ix = inversesqrt(x);
    float prof = ix * sqrt(ix) * sqrt(sqrt(max(1.0 - ix, 0.0))) * 2.0498;
    // filaments: fine bright streaks over darker lanes (more contrast as the turbulence rises)
    float streak = mix(0.35 + 1.3 * fil * fil, 0.12 + 2.0 * fil * fil, ta);
    // two trailing spiral density waves (pitch about 16 degrees) turning slower than the gas: 2 phi - 7 ln r, at a
    // pattern speed whose period divides the shader clock's hour
    vec2 d2 = q.xz / r;
    float c2 = d2.x * d2.x - d2.y * d2.y;
    float s2 = 2.0 * d2.x * d2.y;
    float pa = 0.20944 * t - 7.0 * log(r);
    float arm = 0.5 + 0.5 * (c2 * cos(pa) - s2 * sin(pa));
    float g2 = gas.y * gas.y;
    float bright = prof * sqrt(prof) * streak * (1.0 + 5.0 * g2 * g2) * (0.8 + 0.45 * arm);
    float temp = prof;

    // infall streamers: trailing spiral filaments from the inner edge down to the horizon
    if (strm > 0.0 && r < rIn * 1.6) {
        float phi = atan(q.z, q.x);
        float arm = 0.5 + 0.5 * cos(3.0 * phi + 7.0 * log(r) + t * 1.7);
        float s3 = strm * pow(arm, 5.0) * (0.3 + gas.x) * smoothstep(1.02, 1.2, r)
            * (1.0 - smoothstep(rIn * 1.1, rIn * 1.6, r));
        dens += 1.4 * s3;
        bright += 1.6 * s3;
        temp = mix(temp, 0.62, clamp(s3 * 3.0, 0.0, 1.0));
    }
    if (dens <= 0.0) return;

    // Doppler: Keplerian gas turning anticlockwise seen from above (disc frame velocity (z, -x) / r)
    vec3 dd = u_DiscFrame * dir;
    vec2 vm = vec2(q.z, -q.x) / r;
    float beta = min(sqrt(0.5 / max(r - 1.0, 0.05)), 0.6);
    float cosT = -(vm.x * dd.x + vm.y * dd.z);
    float delta = sqrt(1.0 - beta * beta) / (1.0 - beta * cosT);
    // gravitational redshift
    float grav = sqrt(max(1.0 - 1.0 / r, 0.03));
    float shift = delta * grav;

    vec3 bb = btsBlackbody(u_Blackbody, u_Look.x * temp * shift);
    vec3 src = bb * bright * pow(shift, u_Look2.y) * u_Look.w;
    float al = 1.0 - exp(-7.0 * dens * column);
    col += trans * al * src;
    trans *= 1.0 - al;
}

// The procedural starfield seen along d (the lensed background without the framebuffer copy).
float bhStars(vec3 d) {
    vec3 g = d * 70.0;
    vec3 cell = floor(g);
    float h = btsHash13(cell);
    vec3 jit = vec3(btsHash13(cell + 17.3), btsHash13(cell + 41.9), btsHash13(cell + 73.1)) - 0.5;
    vec3 f = fract(g) - 0.5 - jit * 0.6;
    return step(0.92, h) * exp(-dot(f, f) * 60.0) * (0.4 + 0.6 * btsHash13(cell + 5.0));
}

// The depth pass's shortcut (no march): true for the shadow and the photon ring round it. A ray moving inward from
// outside the photon sphere (r 1.5) with a straight-line impact parameter below b_crit = 2.6 r_s is captured; the
// photon ring lies just outside (light without alpha, so the march would leave it out and glass behind it would show
// over it). 3.1 r_s takes in the ring and the side the frame dragging pushes out (seen in the depth check), not the
// softer halo beyond (to about 4 r_s).
bool bhDepthMask(vec3 rd) {
    vec3 ro = u_Cam;
    float tca = -dot(ro, rd);
    vec3 cp = ro + rd * tca;
    return tca > 0.0 && dot(ro, ro) > 9.0 && dot(cp, cp) < 3.1 * 3.1;
}

// The whole look for one view ray rd (unit, world axes). Returns premultiplied rgb + alpha.
vec4 bhShade(vec3 rd) {
    vec3 ro = u_Cam;
    float Rb = u_Disc.w;
    float t = u_Look.z;
    float rOut = u_Disc.y;

    // straight-line impact parameter and closest point
    float tca = -dot(ro, rd);
    vec3 pc = ro + rd * max(tca, 0.0);
    float b = length(ro + rd * tca);

    // additive extras: a soft glow round the disc, and the opening / Hawking flashes (rings in the sky plane)
    vec3 tint = btsBlackbody(u_Blackbody, u_Look.x * 0.9);
    float glow = u_Look2.w * exp(-(b * b) / (0.30 * rOut * rOut)) * (1.0 - smoothstep(0.7 * Rb, Rb, b));
    vec3 extra = tint * glow * u_Look.w;
    float pxw = max(u_Lens.z * 2.0, 0.08);
    if (u_Flash.y > 0.0) {
        float d1 = (b - u_Flash.x) / max(0.10 * u_Flash.x + 0.3, pxw);
        extra += vec3(1.0, 0.93, 0.80) * u_Flash.y * exp(-d1 * d1);
    }
    if (u_Flash.w > 0.0) {
        float d2 = (b - u_Flash.z) / max(0.08 * u_Flash.z + 0.3, pxw);
        extra += vec3(0.62, 0.80, 1.0) * u_Flash.w * (exp(-d2 * d2) + 1.6 * exp(-(b * b) / (0.04 * u_Flash.z * u_Flash.z + 0.8)));
    }

    if (b >= Rb || tca < -Rb) {
        vec3 e = btsShoulder(extra, 1.6);
        return vec4(e, 0.0);
    }

    // enter the bounding sphere (or start at the camera inside it)
    vec3 p = ro;
    float r0 = length(ro);
    if (r0 > Rb) p = ro + rd * (tca - sqrt(max(Rb * Rb - b * b, 0.0)));
    vec3 v = rd;
    vec3 spin = vec3(u_DiscFrame[0][1], u_DiscFrame[1][1], u_DiscFrame[2][1]);

    vec3 col = vec3(0.0);
    float trans = 1.0;
    bool captured = false;
    float budget = 4.0;
    float rMinSeen = 1e4;
    vec2 gasMemo = vec2(0.5, 0.0);
    float transStop = u_Lens.w > 0.5 ? 0.95 : 0.004;
    float steps = u_Ray.x;
    float k = u_Ray.y;
    // the slab's reach above the plane, a bound for any radius r: 2.6 H(r) (see bhDiscStep)
    float slabK = 2.6 * u_Disc.z;
    float slabC = 2.6 * (max(0.02, 0.6 * u_Lens.z) + u_Look2.x * 0.18);
    vec3 pPrev = p;
    float yPrev = dot(spin, p);
    for (int i = 0; i < 96; i++) {
        if (float(i) >= steps) break;
        float r = length(p);
        float dt = clamp(k * r, 0.025, 2.5);
        // drift - kick - drift (leapfrog)
        p += v * (0.5 * dt);
        float ir = inversesqrt(dot(p, p));
        float ir3 = ir * ir * ir;
        vec3 hv = cross(p, v);
        vec3 acc = -1.5 * dot(hv, hv) * p * (ir3 * ir * ir);
        // frame dragging: a gravitomagnetic dipole about the spin axis (pushes co-rotating light out, pulls
        // counter-rotating light in: the shadow flattens on the approaching side, the D-shape)
        vec3 ph = p * ir;
        vec3 bg = (3.0 * ph * dot(spin, ph) - spin) * ir3;
        // (traced backwards: the force flips sign with the direction of travel)
        acc += u_Ray.w * cross(v, bg);
        v = normalize(v + acc * dt);
        p += v * (0.5 * dt);
        float rn = length(p);
        float yNow = dot(spin, p);
        // the disc only where this step comes near its plane (heights first, the full frame only then)
        if (yPrev * yNow <= 0.0 || min(abs(yPrev), abs(yNow)) < slabK * max(r, rn) + slabC) {
            bhDiscStep(u_DiscFrame * pPrev, u_DiscFrame * p, dt, v, t, col, trans, budget, gasMemo);
        }
        pPrev = p;
        yPrev = yNow;
        rMinSeen = min(rMinSeen, rn);
        if (rn < 1.0) {
            captured = true;
            break;
        }
        if (rn > Rb && dot(p, v) > 0.0) break;
        // (the depth pass only asks whether alpha reaches 0.05: it stops there)
        if (trans < transStop) break;
        // leaving (past r 3 r_s and moving outward, its remaining bend small; not with the screen copy, which wants
        // the final direction): the ray meets nothing more if it moves away from the disc plane clear of the slab,
        // or will cross the plane well outside the disc
        if (u_Lens.x < 0.5 && rn > 3.0 && dot(p, v) > 0.0) {
            float vy = dot(spin, v);
            if (yNow * vy > 0.0) {
                if (abs(yNow) > slabK * rn + slabC + 0.05) break;
            } else if (abs(vy) > 1e-3) {
                float tc = -yNow / vy;
                if (length(p + v * tc) > u_Disc.y * 1.1 + 0.15 * tc + 0.5) break;
            }
        }
    }

    vec3 emit = col;
    float alpha;
    vec3 behind = vec3(0.0);
    if (captured) {
        alpha = 1.0;
    } else {
        // photon ring: rays that grazed the photon sphere (r 1.5) on their way; a thin bright ring there and a softer
        // glow from the rays that came within 3 r_s, brighter on the approaching side; cream-gold
        vec3 pd = u_DiscFrame * pc;
        vec3 rdd = u_DiscFrame * rd;
        // the orbital direction at the ring point, scaled by how far it sits from the projected spin axis (so it
        // fades smoothly to 0 above and below the shadow instead of flipping sign there)
        float side = dot(vec3(pd.z, 0.0, -pd.x), -rdd) / max(length(pd), 1e-3);
        float ringW = 0.10 + 3.0 * clamp(u_Lens.z, 0.0, 0.3);
        // squared by hand: pow() of a negative base is undefined in GLSL (NaN where pow is exp2(y * log2(x)))
        float ringD = (rMinSeen - 1.53) / ringW;
        float ring = exp(-ringD * ringD) + 0.22 * (1.0 - smoothstep(1.6, 3.4, rMinSeen));
        ring *= u_Look2.z * mix(0.45, 1.75, 0.5 + 0.5 * side);
        emit += (0.4 + 0.6 * trans) * ring * mix(vec3(1.0, 0.90, 0.70), tint, 0.35);

        float lensZone = 1.0 - smoothstep(0.55 * Rb, Rb, b);
        if (u_Lens.x > 0.5) {
            // the screen behind, seen along the bent ray; the bending fades out toward the bounding sphere, so its
            // edge matches the unbent screen exactly
            vec3 dir = normalize(mix(rd, v, lensZone));
            vec4 clip = u_ViewProj * vec4(dir, 0.0);
            if (clip.w > 1e-4) {
                vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
                if (uv.x > u_FrameRect.x && uv.y > u_FrameRect.y && uv.x < u_FrameRect.z && uv.y < u_FrameRect.w) {
                    behind = BTS_TEX(u_Frame, uv).rgb;
                }
            }
            alpha = 1.0;
        } else {
            // a bent starfield inside the lensing ring (shows at night; invisible against a day sky)
            emit += trans * u_Lens.y * bhStars(v) * lensZone * (1.0 - smoothstep(1.2, 1.8, b / 2.6));
            alpha = 1.0 - trans;
        }
    }
    vec3 outc = btsShoulder(emit + extra * (captured ? 0.12 : 1.0), 1.6) + trans * behind;
    return vec4(outc, alpha);
}
