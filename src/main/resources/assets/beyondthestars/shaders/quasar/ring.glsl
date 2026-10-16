// Beyond the Stars: the quasar's accretion ring (renders v2 §6.2), the body of bts_qring in both profiles.
//
// The ring is a volume: a flared Gaussian slab of gas from the hot inner wall at r0 out to a ragged, dusty outer edge
// near r1, in the quasar's equatorial plane (y = 0, world axes, blocks, the quasar centre at the origin). The mesh is
// the slab's proxy (top and bottom faces, inner and outer walls); each front-facing fragment is where its view ray
// enters the proxy, and it marches that ray to where it leaves (the slab's planes, the outer wall, or the inner wall
// into the hole's space), integrating emission and absorption front to back. A ray that crosses the hole's space and
// re-enters the far side gets a second fragment from the far wall. A camera inside the slab draws one more quad
// (mode 1) marching from the camera.
//
// Look: sheared Keplerian filaments (the inner gas laps the outer), m = 2 and m = 3 trailing spiral density waves
// turning slower than the gas, a bright hot inner wall (1.5 blocks), cooler and redder outward with dark dust lanes,
// Doppler beaming (the side turning toward the camera brighter and bluer), the zone's temperature and turbulence, the
// Harmonic Lock's standing ripples, re-forming embers, and the eruption / purge / blow-out / re-formed events.
//
// Output: premultiplied colour + alpha (glBlendFunc ONE, ONE_MINUS_SRC_ALPHA).

#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
uniform sampler2D u_Blackbody;
// the quasar centre relative to the camera, blocks
uniform vec3 u_Centre;
// x: proxy inner radius, y: proxy outer radius, z: proxy half height, w: gas half-thickness H0 (blocks)
uniform vec4 u_Ring;
// x: gas inner edge r0, y: gas outer edge r1, z: Keplerian rate at r0 (rad/s), w: time (s)
uniform vec4 u_Gas;
// x: inner wall temperature K, y: body temperature K, z: turbulence 0..1, w: exposure
uniform vec4 u_Look;
// x: Harmonic Lock 0..1, y: re-forming embers 0/1, z: re-forming fraction 0..1, w: Doppler beta
uniform vec4 u_Look2;
// x: event kind (0 none, 1 eruption, 2 purge, 3 blow-out, 4 re-formed, 5 First Light), y: age s, z: size 0..1
uniform vec4 u_Event;
// x: march steps, y: noise octaves, z: mode (0 proxy faces, 1 camera inside: march from the camera), w: one pixel's
// width per block of distance
uniform vec4 u_Ray;

// erf, Winitzki's approximation (|error| < 2e-3, plenty for a column density)
float qrErf(float x) {
    float x2 = x * x;
    float ax2 = 0.147 * x2;
    float e = sqrt(1.0 - exp(-x2 * (1.2732395 + ax2) / (1.0 + ax2)));
    return x < 0.0 ? -e : e;
}

// The gas's half-thickness at radius r (flared outward).
float qrHeight(float r) {
    float x = (r - u_Gas.x) / max(u_Gas.y - u_Gas.x, 1.0);
    return u_Ring.w * (0.55 + 0.6 * clamp(x, 0.0, 1.3));
}

// The ring's gas in the plane at local point p (its height profile is integrated by the caller): x = surface
// density, y = emission scale, z = temperature factor, w = dust 0..1.
vec4 qrGas(vec3 p, float r, float t, float octaves, float fp) {
    float r0 = u_Gas.x;
    float r1 = u_Gas.y;
    float w = max(r1 - r0, 1.0);
    float x = (r - r0) / w;
    vec2 dir = p.xz / r;
    float ta = u_Look.z;
    float ev = u_Event.x;
    float age = u_Event.y;

    // blow-out: the ring breaks into chunks flying outward (sample where the gas came from)
    float rs = r;
    if (ev > 2.5 && ev < 3.5) rs = r - age * (5.0 + 4.0 * btsNoise2(u_Noise, dir * 9.0 + 3.0));
    float xs = (rs - r0) / w;
    // blow-out chunks: the gas tears into separate clumps
    float chunks = 1.0;
    if (ev > 2.5 && ev < 3.5) {
        vec4 ck = btsNoise2x4(u_Noise, dir * 6.0 + vec2(rs * 0.11, 31.0));
        chunks = mix(1.0, smoothstep(0.5, 0.62, ck.r), smoothstep(0.0, 0.5, age));
    }

    float vert = 1.0;

    // Keplerian shear: the gas's direction advanced by Omega(r) t, two 12 s cycles cross-faded so it shears but
    // never winds up (12 s divides the shader clock's hour)
    float om = u_Gas.z * pow(max(rs / r0, 0.5), -1.5);
    float fil = 0.0;
    // hot clumps and dust lanes: coarse fields drifting with the gas (one lookup each)
    float ca0 = cos(om * t * 0.5);
    float sa0 = sin(om * t * 0.5);
    vec2 cs0 = vec2(dir.x * ca0 - dir.y * sa0, dir.x * sa0 + dir.y * ca0);
    // (one lookup, two channels: B the clumps, A the lanes; the radial term turns the field so it is not a cylinder)
    vec4 cl = btsNoise2x4(u_Noise, cs0 * (11.0 + 0.12 * (rs - r0)) + vec2((rs - r0) * 0.31, 0.0) + 17.0);
    float clump = cl.b;
    float lane = cl.a;
    for (int k = 0; k < 2; k++) {
        float fk = float(k);
        float ph = fract(t / 8.0 + 0.5 * fk);
        float wk = 1.0 - abs(2.0 * ph - 1.0);
        float a = om * ph * 8.0 + 2.1 * fk + 0.05 * p.y;
        float ca = cos(a);
        float sa = sin(a);
        vec2 cs = vec2(dir.x * ca - dir.y * sa, dir.x * sa + dir.y * ca);
        // streaks along the orbit: coarse round the ring, fine across it; the laminar (calm) ring finer still
        float lam = mix(1.6, 0.9, ta);
        float zr = (rs - r0) * 0.35 * lam + 31.0 * fk + 0.25 * p.y;
        float n0 = btsNoise3(u_Noise, vec3(cs * 13.0, zr));
        float n1 = btsNoise3(u_Noise, vec3(cs * 27.0, zr * 2.1 + 5.0));
        // octaves finer than the sample's footprint (pixel or step) fade out: no sparkle
        float w1 = step(1.5, octaves) * 0.55 * (1.0 - smoothstep(0.2, 0.4, fp));
        float w2 = step(2.5, octaves) * 0.25 * (1.0 - smoothstep(0.09, 0.18, fp));
        float n2 = w2 > 0.0 ? btsNoise3(u_Noise, vec3(cs * 46.0, zr * 3.3 + 11.0)) : 0.5;
        fil += wk * (n0 * 0.5 + n1 * w1 * 0.5 + n2 * w2 * 0.5) / (0.5 + 0.5 * (w1 + w2));
    }

    // envelope: a sharp inner wall, a ragged outer edge
    float inner = smoothstep(-0.035, 0.02, xs);
    float outer = 1.0 - smoothstep(0.42, 1.0, xs + 0.35 * (clump - 0.5) + 0.12 * (fil - 0.5));
    // spiral density waves, m = 2 and 3, trailing, pitch about 15 degrees, turning slower than the gas
    float c1 = dir.x;
    float s1 = dir.y;
    float c2 = c1 * c1 - s1 * s1;
    float s2 = 2.0 * c1 * s1;
    float c3 = c1 * (4.0 * c1 * c1 - 3.0);
    float s3 = s1 * (3.0 - 4.0 * s1 * s1);
    float lr = log(max(rs / r0, 0.5)) * 3.73;
    float pa = 0.031416 * t;
    float a2 = 2.0 * (lr + pa);
    float a3 = 3.0 * (lr + 0.7 * pa) + 1.3;
    float arm2 = c2 * cos(a2) + s2 * sin(a2);
    float arm3 = c3 * cos(a3) + s3 * sin(a3);
    float arms = 0.5 + 0.3 * arm2 + 0.2 * arm3;

    // filaments: bright streaks over dark lanes, more ragged and clumpy as the turbulence rises
    float f = smoothstep(0.25, 0.75, fil);
    float turb = mix(0.12 + 1.5 * f * f, 0.04 + 2.2 * f * f * f, ta);
    float hot = smoothstep(0.45, 0.85, clump);
    float dens = chunks * vert * inner * outer * turb * (0.35 + 1.1 * arms * arms) * (1.0 + mix(1.0, 4.0, ta) * hot * hot);

    // the outer half is dusty: lanes that absorb more than they shine
    float dust = smoothstep(0.2, 0.8, xs) * smoothstep(0.42, 0.7, lane);
    // brighter inward; the inner wall glows (1.5 blocks), wavering when Critical (B-251: up to x1.8 at about 3.5 Hz,
    // was x2.4 at 6 Hz)
    float wallShape = exp(-((rs - r0) * (rs - r0)) / 2.2) * vert * inner * chunks;
    float wall = wallShape;
    if (wallShape > 0.01) wall *= 1.0 + 0.8 * ta * btsNoise2(u_Noise, vec2(t * 3.5, dir.x * 3.0));
    // the inner wall is dense and hot whatever the filaments do
    dens += 0.7 * wallShape * (0.6 + 0.4 * f);
    float bright = 1.0 - 0.65 * clamp(xs, 0.0, 1.0);
    bright = bright * sqrt(bright) * (1.0 - 0.75 * dust) + wall;
    float temp = 1.0 - 0.5 * clamp(xs, 0.0, 1.0) + 0.3 * hot;

    // Harmonic Lock: a standing pattern of concentric bright ripples
    float lock = u_Look2.x;
    if (lock > 0.0) {
        float rip = cos(6.2832 * (rs - r0) / 3.6) * (0.7 + 0.3 * cos(1.5708 * t));
        bright *= max(0.15, 1.0 + lock * 1.1 * rip);
        dens *= 1.0 + lock * 0.35 * rip;
    }

    // re-forming: embers only, filling in from the inner wall
    if (u_Look2.y > 0.5) {
        float front = u_Look2.z;
        float emb = smoothstep(0.55, 0.85, clump * 0.6 + fil * 0.6) * (1.0 - smoothstep(front - 0.08, front + 0.04, xs));
        dens *= 0.3 + 0.9 * emb;
        bright *= 0.3 + 1.4 * emb;
    }
    return vec4(dens, bright, temp, dust);
}

// The event's effect at radius r: x = brightness factor, y = temperature factor, z = density factor.
vec3 qrEvent(float r) {
    float ev = u_Event.x;
    float age = u_Event.y;
    float size = u_Event.z;
    float r0 = u_Gas.x;
    vec3 e = vec3(1.0);
    if (ev < 0.5) return e;
    if (ev < 1.5) {
        // eruption: a flare front races out from the inner wall, the ring brightens behind it
        float rf = r0 - 2.0 + 17.0 * age;
        float d = (r - rf) / 3.0;
        float front = exp(-d * d) * exp(-age / 2.0);
        float behind = (1.0 - smoothstep(rf - 2.0, rf + 2.0, r)) * exp(-age / 1.6);
        e.x = 1.0 + (1.6 * front + 0.7 * behind) * (0.5 + size);
        e.y = 1.0 + 0.35 * front + 0.2 * behind;
    } else if (ev < 2.5) {
        // purge: the ring dims for two seconds (a calm, controlled release)
        float dim = smoothstep(0.0, 0.4, age) * (1.0 - smoothstep(2.0, 3.0, age));
        e.x = 1.0 - 0.6 * dim;
        e.y = 1.0 + 0.25 * exp(-age / 0.6);
    } else if (ev < 3.5) {
        // blow-out: a white-hot flash, then the chunks fade to nothing over five seconds
        e.x = (1.0 + 2.5 * exp(-age / 0.5)) * (1.0 - smoothstep(1.5, 5.0, age));
        e.y = 1.0 + 0.4 * exp(-age / 0.8);
        e.z = 1.0 - smoothstep(2.0, 5.0, age);
    } else {
        // re-formed / First Light: the inner wall kindles and the gas fills outward over two seconds
        float fill = smoothstep(-6.0, 0.0, 14.0 * age - (r - r0));
        float kindle = exp(-((r - r0) * (r - r0)) / 6.0) * exp(-age / 1.4);
        e.x = fill * (1.0 + 4.0 * kindle);
        e.y = 1.0 + 0.5 * kindle;
        e.z = fill;
    }
    return e;
}

// The ray-slab-annulus segment from the entry at t = tE along rd from the camera (local o): its exit.
float qrExit(vec3 o, vec3 rd, float tE) {
    float H = u_Ring.z;
    float tx = 1e5;
    if (rd.y > 1e-5) tx = (H - o.y) / rd.y;
    else if (rd.y < -1e-5) tx = (-H - o.y) / rd.y;
    float a = dot(rd.xz, rd.xz);
    if (a > 1e-8) {
        float b = dot(o.xz, rd.xz);
        float c1 = dot(o.xz, o.xz) - u_Ring.y * u_Ring.y;
        float d1 = b * b - a * c1;
        if (d1 > 0.0) tx = min(tx, (-b + sqrt(d1)) / a);
        float c0 = dot(o.xz, o.xz) - u_Ring.x * u_Ring.x;
        float d0 = b * b - a * c0;
        if (d0 > 0.0) {
            float t0 = (-b - sqrt(d0)) / a;
            if (t0 > tE + 1e-3) tx = min(tx, t0);
        }
    }
    return max(tx, tE);
}

// The whole look for the fragment at camera-relative pos; kind: 0 top, 1 bottom, 2 inner wall, 3 outer wall, 4 the
// camera-inside quad. Returns premultiplied rgb + alpha; alpha < 0 means "discard".
vec4 qrShade(vec3 pos, float kind, vec2 fragXY) {
    vec3 o = -u_Centre;
    float tE;
    vec3 rd = normalize(pos);
    if (kind < 3.5) {
        vec3 pe = pos - u_Centre;
        vec3 n;
        if (kind < 0.5) n = vec3(0.0, 1.0, 0.0);
        else if (kind < 1.5) n = vec3(0.0, -1.0, 0.0);
        else {
            vec2 d = normalize(pe.xz);
            n = kind < 2.5 ? vec3(-d.x, 0.0, -d.y) : vec3(d.x, 0.0, d.y);
        }
        // only where the ray enters the slab
        if (dot(n, rd) >= 0.0) return vec4(0.0, 0.0, 0.0, -1.0);
        tE = length(pos);
    } else {
        tE = 0.0;
    }
    float tX = qrExit(o, rd, tE);
    float len = tX - tE;
    if (len < 0.02) return vec4(0.0, 0.0, 0.0, -1.0);

    float t = u_Gas.w;
    float octaves = u_Ray.y;
    // sub-segments: each samples the gas where it comes nearest the plane and integrates the Gaussian height profile
    // exactly through it (no banding from the steps, and no noise lookups away from the plane)
    // (a constant count: a count that followed the length would jump between pixels and draw contour lines)
    // sub-segments at least two gas half-thicknesses long (a ray from above needs only a few); the count is not an
    // integer: the last one shrinks to nothing as its end passes the exit, so neighbouring pixels stay continuous
    float ds = max(len / u_Ray.x, 2.0 * u_Ring.w);
    float steps = len / ds;
    vec3 col = vec3(0.0);
    float trans = 1.0;
    float beta = u_Look2.w;
    float expo = u_Look.w;
    for (int i = 0; i < 32; i++) {
        if (float(i) >= steps) break;
        float ta = tE + float(i) * ds;
        float dl = min(ds, tX - ta);
        vec3 pa = o + rd * ta;
        vec3 pb = pa + rd * dl;
        float ya = pa.y;
        float yb = pb.y;
        float sn = ya * yb < 0.0 ? ya / (ya - yb) : (abs(ya) < abs(yb) ? 0.0 : 1.0);
        vec3 p = mix(pa, pb, sn);
        float r = length(p.xz);
        if (r < 1.0) continue;
        float h = qrHeight(r);
        if (ya * yb > 0.0 && min(abs(ya), abs(yb)) > 2.6 * h) continue;
        float dy = yb - ya;
        float column = abs(dy) < 0.05 * h ? dl * exp(-(p.y * p.y) / (h * h))
            : 0.8862 * h * abs(qrErf(yb / h) - qrErf(ya / h)) * dl / abs(dy);
        if (column < 1e-3) continue;
        // the sample's footprint in the plane: its sub-segment's horizontal reach, or a pixel
        // (a pixel stretches across the plane as the view grazes it)
        float fp = max(0.5 * dl * length(rd.xz), (ta + 0.5 * dl) * u_Ray.w / max(abs(rd.y), 0.12));
        vec4 g = qrGas(p, r, t, octaves, fp);
        if (g.x <= 1e-4) continue;
        vec3 ev = qrEvent(r);
        float dens = g.x * ev.z;
        // Doppler: gas turning anticlockwise seen from above (velocity (z, -x) / r), toward the camera brighter
        vec2 vm = vec2(p.z, -p.x) / r;
        float cosT = -(vm.x * rd.x + vm.y * rd.z);
        float delta = sqrt(1.0 - beta * beta) / (1.0 - beta * cosT);
        float kel = mix(u_Look.y, u_Look.x, 1.0 - smoothstep(0.0, 2.8, r - u_Gas.x)) * g.z * ev.y * delta;
        vec3 bb = btsBlackbody(u_Blackbody, kel);
        // re-forming embers: a violet cast
        bb = mix(bb, vec3(0.75, 0.30, 0.85) * btsLuma(bb) * 1.6, 0.45 * u_Look2.y);
        // mostly optically thin (the streaks show at any angle), the dusty lanes absorb
        float kappa = dens * (0.05 + 0.9 * g.w);
        vec3 src = bb * g.y * ev.x * delta * delta * delta * expo * (1.0 - 0.8 * g.w);
        col += trans * src * dens * column * 0.15;
        trans *= exp(-kappa * column);
        if (trans < 0.01) break;
    }
    float alpha = 1.0 - trans;
    if (alpha < 0.002) return vec4(0.0, 0.0, 0.0, -1.0);
    return vec4(btsShoulder(col, 1.6), alpha);
}
