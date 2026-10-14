// Beyond the Stars: machine volumes (renders v2 §11, §12, WP-7 / WP-8), the body of bts_mvol in both profiles.
//
// The proxy is an elliptic cylinder in the machine's voxel frame (u_Box: semi-axes a (x), b (z), floor y0, roof y1),
// matching the air the mockup leaves for the render (the Homogeny's glass column, a Diffraction drum, its spine).
// Only its back faces shade: each fragment intersects its view ray with the cylinder analytically, so the march runs
// from where the ray enters (or the camera, inside) to the back face, and blocks in front of the volume hide it
// through the depth test. Emission is integrated front to back over u_Mode.z steps, jittered per pixel; thin features
// (braids, sheets) widen to the step length and dim to keep their energy, so nothing aliases into bands.
// Output: premultiplied, alpha = absorption (0 for the pure emitters). Blend ONE, ONE_MINUS_SRC_ALPHA.
//
// Modes (u_Mode.x):
//   0 the Homogeny's vortex: braids of plasma spiralling up a funnel, the feed colours braiding at the bottom and
//     mixing into the product at the top, a swirling sheath, sparks, (T3) a core thread, the shear helix's glow.
//   1 a Diffraction drum: the Entanglement Medium's swirl (bottom drum) or two region clouds split along the drum's
//     axis, the lens's spectral fans (rainbow sheets with interference fringes) and faint fringes over the floor.
//   2 the Diffraction spine: the rising beam and the three diffraction lenses (faceted, dispersing).

#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform vec4 u_Box;
uniform vec3 u_Cam;
// x mode, y time (s), z steps, w octaves
uniform vec4 u_Mode;
// mode 0: x tier, y activity 0..1, z idle, w brightness | mode 1: x drum index, y activity, z region axis (rad), w held
// mask (6 bits as a float) | mode 2: x activity, y brightness, z -, w -
uniform vec4 u_VA;
uniform vec4 u_VB;
uniform vec4 u_VC;
uniform vec4 u_VD;
uniform vec4 u_VE;
uniform sampler2D u_Noise;

float mvN3(vec3 p) {
    return btsNoise3(u_Noise, p);
}

float mvFbm(vec3 p, int oct) {
    return btsFbm3(u_Noise, p, oct);
}

// hue 0..1 -> saturated rainbow colour
vec3 mvHue(float h) {
    vec3 k = vec3(0.0, 2.0 / 3.0, 1.0 / 3.0);
    vec3 c = clamp(abs(fract(vec3(h) + k) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return c * c * (3.0 - 2.0 * c);
}

// ------------------------------------------------------------------------------------------- mode 0: the vortex

// The funnel the braids follow (the mockup's homVortex): fat in the middle, narrow at both caps.
float mvFunnel(float f) {
    return 2.5 + 7.0 * pow(max(sin(clamp(f, 0.0, 1.0) * BTS_PI), 0.0), 0.6);
}

vec3 mvBraidColour(int k) {
    if (k == 0) return u_VB.rgb;
    if (k == 1) return u_VC.rgb;
    return u_VD.rgb;
}

vec3 mvVortex(vec3 p, float dt, float t, int oct) {
    float y0 = u_Box.z;
    float y1 = u_Box.w;
    float f = (p.y - y0) / (y1 - y0);
    float r = length(p.xz);
    float th = atan(p.z, p.x);
    float act = u_VA.y;
    float speed = mix(0.22, 1.0, act);
    float tier = u_VA.x;
    float Rf = mvFunnel(f);
    float mixT = btsSmooth01((f - 0.45) / 0.5);
    vec3 product = u_VE.rgb;

    // the swirl: angular speed falls with radius (inner gas laps the outer), gas rises
    float om = speed * 2.4 / (r + 2.0);
    float rise = speed * 2.2;
    float ths = th - om * t * 3.0;
    vec3 q = vec3(cos(ths) * r, p.y - rise * t, sin(ths) * r);

    vec3 c = vec3(0.0);

    // braids: one per feed (T1 one, T2 two, T3 three), on a helix 0.26 rad per block up the funnel
    int nb = int(clamp(tier, 1.0, 3.0));
    float tilt = 1.0 / sqrt(1.0 + (Rf * 0.26) * (Rf * 0.26));
    float wq = 1.35 + 0.25 * sin(p.y * 0.4 + t * speed);
    float weff = max(wq, dt * 0.9);
    float keep = wq / weff;
    float fil = mvN3(vec3(q.x * 0.55, q.y * 0.7, q.z * 0.55));
    for (int k = 0; k < 3; k++) {
        if (k >= nb) break;
        float thk = 0.26 * p.y + speed * t * 1.1 + BTS_TAU * float(k) / float(nb);
        vec2 cen = Rf * vec2(cos(thk), sin(thk));
        float dh = length(p.xz - cen) * tilt;
        float dens = exp(-(dh * dh) / (weff * weff)) * keep * (0.55 + 0.9 * fil);
        vec3 col = mix(mvBraidColour(k), product, mixT);
        c += col * dens * 2.3;
    }

    // the sheath: a turbulent swirling shell round the funnel, the feeds' colours mixed by angle
    float sh0 = (r - Rf * 0.85) / (Rf * 0.55 + 0.8);
    float shell = exp(-sh0 * sh0);
    float turb = mvFbm(vec3(q.x * 0.22, q.y * 0.16, q.z * 0.22) + vec3(0.0, 0.0, 3.1), (oct > 2 ? 2 : oct));
    float spiral = 0.5 + 0.5 * sin(3.0 * th - 2.2 * log(r + 1.0) * 3.0 + p.y * 0.35 - om * t * 6.0);
    float tt = max(turb - 0.35, 0.0) / 0.65;
    float sheath = shell * (0.08 + 0.92 * tt * tt) * (0.45 + 0.55 * spiral);
    vec3 feeds = (u_VB.rgb + (nb > 1 ? u_VC.rgb : u_VB.rgb) + (nb > 2 ? u_VD.rgb : u_VB.rgb)) / 3.0;
    float ang = 0.5 + 0.5 * cos(th - 0.26 * p.y - speed * t);
    vec3 sheathCol = mix(mix(u_VB.rgb, nb > 1 ? u_VC.rgb : u_VB.rgb, ang), product, mixT * 0.8 + 0.2);
    c += mix(sheathCol, feeds, 0.3) * sheath * 0.14;

    // sparks riding the flow
    if (oct >= 3) {
        float sp = mvN3(q * 0.45 + vec3(0.0, -t * 1.5, 0.0));
        c += mix(vec3(1.0, 0.95, 0.85), product, 0.4) * pow(sp, 12.0) * 5.0 * shell * act;
    }

    // T3: a bright core thread up the axis, with pulses rising
    if (tier > 2.5) {
        float rc = max(0.55, dt * 0.3);
        float core = exp(-(r * r) / (rc * rc)) * (0.55 / rc);
        float pulse = 0.6 + 0.4 * sin(p.y * 0.5 - t * 6.0 * speed);
        c += mix(vec3(1.0), product, 0.35) * core * pulse * 1.6;
    }

    // calmer, dimmer idle; fades at the caps
    float capFade = btsSmooth01(f / 0.05) * btsSmooth01((1.0 - f) / 0.05);
    return c * capFade * mix(u_VA.z, 1.0, act) * u_VA.w;
}

// The shear helix's glow on the inside of the glass where the ray crosses the wall (pitch 11 from y -24, single
// start), pulses running up it: evaluated once per wall crossing, not per step.
vec3 mvCoil(vec3 p) {
    float th = atan(p.z, p.x);
    float act = u_VA.y;
    float speed = mix(0.22, 1.0, act);
    float coil = mod(p.y + 24.0 - 11.0 * (th / BTS_TAU), 11.0);
    coil = min(coil, 11.0 - coil);
    float coilG = exp(-(coil * coil) / 0.8);
    float run = (p.y + 24.0) / 11.0 - th / BTS_TAU;
    float pulses = 0.35 + 0.65 * pow(0.5 + 0.5 * sin(BTS_TAU * (run * 0.5 - u_Mode.y * 0.6 * speed)), 6.0) * act;
    float f = (p.y - u_Box.z) / (u_Box.w - u_Box.z);
    float capFade = btsSmooth01(f / 0.04) * btsSmooth01((1.0 - f) / 0.04);
    return vec3(0.25, 1.0, 0.45) * coilG * pulses * capFade * mix(u_VA.z, 1.0, act) * u_VA.w;
}

// ------------------------------------------------------------------------------- mode 1: a Diffraction drum

vec3 mvRegion(int i) {
    // Hyperspace, Subspace, Forward Time, Reverse Time, Positive Mass, Negative Mass
    if (i == 0) return vec3(0.30, 0.75, 1.00);
    if (i == 1) return vec3(0.55, 0.30, 1.00);
    if (i == 2) return vec3(1.00, 0.80, 0.30);
    if (i == 3) return vec3(0.25, 1.00, 0.70);
    if (i == 4) return vec3(1.00, 0.40, 0.25);
    return vec3(1.00, 0.35, 0.80);
}

bool mvHeld(int i) {
    float m = floor(u_VA.w / pow(2.0, float(i)));
    return mod(m, 2.0) > 0.5;
}

vec3 mvDrum(vec3 p, float dt, float t, int oct) {
    float drum = u_VA.x;
    float act = u_VA.y;
    float r = length(p.xz);
    float th = atan(p.z, p.x);
    float y0 = u_Box.z;
    float y1 = u_Box.w;
    float ym = 0.5 * (y0 + y1);
    float hy = 0.5 * (y1 - y0);
    float a = u_Box.x;
    float b = u_Box.y;
    float edge = length(p.xz / vec2(a, b));
    float inner = btsSmooth01((r - 4.6) / 1.5);
    float outer = 1.0 - btsSmooth01((edge - 0.86) / 0.14);
    float vfade = 1.0 - btsSmooth01((abs(p.y - ym) - hy * 0.55) / (hy * 0.45));
    vec3 c = vec3(0.0);
    float speed = mix(0.25, 1.0, act);

    if (drum < 0.5) {
        // the bottom drum: the Entanglement Medium swirling inward to the spine
        float om = speed * 0.9 / (0.25 + r / 20.0);
        float ths = th + om * t * 0.25;
        float arms = 0.5 + 0.5 * sin(2.0 * ths + 3.4 * log(r + 1.0));
        vec3 q = vec3(cos(ths) * r, p.y * 1.5, sin(ths) * r);
        float d = mvFbm(q * 0.12 + vec3(1.7, 0.0, 4.2), oct);
        float dens = pow(d, 2.2) * (0.35 + 0.65 * arms) * inner * outer * vfade;
        vec3 med = u_VB.rgb;
        c += mix(med, vec3(1.0), 0.15 + 0.4 * d * d) * dens * 1.1;
    } else {
        // two region clouds, split along the drum's axis (u_VA.z), each in its region's colour
        float ax = u_VA.z;
        float side = cos(th - ax);
        int base = 2 * (int(drum + 0.5) - 1);
        int idx = side >= 0.0 ? base : base + 1;
        float gap = btsSmooth01((abs(side) - 0.18) / 0.2);
        bool held = mvHeld(idx);
        float flick = held ? 1.0 : 0.35 + 0.25 * mvN3(vec3(t * 3.0, float(idx) * 5.0, 0.0));
        float sw = speed * 0.2;
        vec3 q = vec3(p.x * cos(sw * t) - p.z * sin(sw * t), p.y, p.x * sin(sw * t) + p.z * cos(sw * t));
        float d = mvFbm(q * vec3(0.10, 0.16, 0.10) + vec3(float(idx) * 3.3, -t * 0.05, 0.0), oct);
        float billow = pow(max(d - 0.25, 0.0) / 0.75, 1.6);
        float dens = billow * gap * inner * outer * vfade * flick;
        vec3 reg = mvRegion(idx);
        c += mix(reg, vec3(1.0), 0.28 * billow) * dens * 1.5;
    }
    return c * u_VE.w;
}

// The lens's spectral fans, where the ray crosses the drum's middle plane: two thin rainbow sheets toward the region
// blades (hue across the fan), interference fringes running outward. Analytic (the sheet is thinner than any step);
// `slant` is the path length through the sheet per unit thickness (1 / |cos|, capped).
vec3 mvFans(vec3 p, float slant, float t) {
    float act = u_VA.y;
    if (u_VA.x < 0.5 || act < 0.01) return vec3(0.0);
    float r = length(p.xz);
    float th = atan(p.z, p.x);
    float ax = u_VA.z;
    float side = cos(th - ax);
    float dang = asin(clamp(sin(th - ax), -1.0, 1.0));
    float fx = dang / 0.36;
    float inFan = 1.0 - btsSmooth01((abs(fx) - 0.75) / 0.25);
    float edge = length(p.xz / u_Box.xy);
    float reach = btsSmooth01((r - 3.8) / 1.5) * (1.0 - btsSmooth01((edge - 0.7) / 0.3));
    float fr = 0.55 + 0.45 * cos(r * 2.4 - t * 5.0 + fx * 1.5);
    float thick = 0.35 + 0.03 * r;
    vec3 rainbow = mvHue(0.55 + 0.45 * fx * (side >= 0.0 ? 1.0 : -1.0));
    float fall = 0.55 + 0.9 * exp(-r / 9.0);
    return rainbow * inFan * fr * reach * act * fall * thick * min(slant, 6.0) * 1.3;
}

// Faint interference fringes over the drum's floor (the deck band below), analytic as the fans.
vec3 mvFloor(vec3 p, float slant, float t) {
    float r = length(p.xz);
    float edge = length(p.xz / u_Box.xy);
    float inner = btsSmooth01((r - 4.6) / 1.5);
    float outer = 1.0 - btsSmooth01((edge - 0.86) / 0.14);
    float speed = mix(0.25, 1.0, u_VA.y);
    float fringe = 0.5 + 0.5 * cos(r * 1.6 - t * 2.4 * speed);
    return vec3(0.45, 0.65, 1.0) * pow(fringe, 6.0) * inner * outer * (0.05 + 0.12 * u_VA.y) * min(slant, 3.0) * 0.6;
}

// ----------------------------------------------------------------------------------- mode 2: the spine

vec3 mvSpine(vec3 p, float dt, float t) {
    float act = u_VA.x;
    float r = length(p.xz);
    float th = atan(p.z, p.x);
    vec3 c = vec3(0.0);
    // the beam: a white core, medium-coloured at the foot, pulses rising
    float rb = max(0.55, dt * 0.25);
    float beam = exp(-(r * r) / (rb * rb)) * (0.55 / rb);
    float pulse = 0.65 + 0.35 * sin(p.y * 0.45 - t * 7.0);
    float foot = btsSmooth01((p.y + 40.0) / 30.0);
    vec3 bc = mix(u_VB.rgb, vec3(1.0, 0.97, 0.92), foot);
    c += bc * beam * pulse * act * 1.6;
    c += u_VB.rgb * exp(-r / 1.4) * 0.05 * (0.3 + act);
    // the three lenses (y -7, 7, 21): bipyramids 3.4 tall, 2.4 wide, glowing amber-white with dispersed facets
    for (int i = 0; i < 3; i++) {
        float ly = -7.0 + 14.0 * float(i);
        float dy = p.y - ly;
        float q = abs(dy) / 3.4 + r / 2.4;
        float inside = 1.0 - btsSmooth01((q - 0.92) / 0.12);
        float facet = 0.5 + 0.5 * cos(4.0 * th + dy * 1.4 + t * 0.6);
        float rim = btsSmooth01((q - 0.55) / 0.4) * inside;
        vec3 lc = mix(vec3(1.0, 0.62, 0.22), vec3(1.0, 0.95, 0.85), 0.5 + 0.5 * facet);
        c += lc * inside * (0.28 + 0.45 * act) * (0.5 + 0.5 * facet);
        c += mvHue(fract(th / BTS_TAU + dy * 0.08 + t * 0.05)) * rim * 0.55 * act;
        // the glare where the beam enters the lens
        float g = exp(-(dy * dy + r * r) / 1.2);
        c += vec3(1.0, 0.92, 0.8) * g * 1.4 * act;
    }
    return c * u_VA.y;
}

// --------------------------------------------------------------------------------------------- the march

vec4 mvShade(vec3 local) {
    vec3 ro = u_Cam;
    vec3 rd = normalize(local - ro);
    vec2 ab = u_Box.xy;
    vec2 o = ro.xz / ab;
    vec2 d = rd.xz / ab;
    float A = dot(d, d);
    float B = dot(o, d);
    float C = dot(o, o) - 1.0;
    float disc = B * B - A * C;
    if (disc <= 0.0 || A < 1e-8) return vec4(0.0);
    float sq = sqrt(disc);
    float t0 = (-B - sq) / A;
    float t1 = (-B + sq) / A;
    if (abs(rd.y) > 1e-5) {
        float ta = (u_Box.z - ro.y) / rd.y;
        float tb = (u_Box.w - ro.y) / rd.y;
        t0 = max(t0, min(ta, tb));
        t1 = min(t1, max(ta, tb));
    } else if (ro.y < u_Box.z || ro.y > u_Box.w) {
        return vec4(0.0);
    }
    t0 = max(t0, 0.05);
    if (t1 <= t0) return vec4(0.0);
    // only the back faces shade (the front faces' fragments stand at the near end)
    float tp = dot(local - ro, rd);
    if (tp < 0.5 * (t0 + t1)) return vec4(-1.0);

    int steps = int(u_Mode.z);
    int oct = int(u_Mode.w);
    float mode = u_Mode.x;
    float t = u_Mode.y;
    vec3 acc = vec3(0.0);
    float tw0 = t0;
    float tw1 = t1;
    if (mode < 0.5) {
        // the vortex: the coil's glow where the ray crosses the wall, the march only through the funnel (r < 10.8)
        acc += mvCoil(ro + rd * t1) * 4.0;
        if (C > 0.0 && tw0 > 0.06) acc += mvCoil(ro + rd * tw0) * 4.0;
        float ri = 10.8 / u_Box.x;
        float Ci = dot(o, o) - ri * ri;
        float di = B * B - A * Ci;
        if (di <= 0.0) return vec4(btsShoulder(acc * 0.1, 1.5), 0.0);
        float sqi = sqrt(di);
        t0 = max(t0, (-B - sqi) / A);
        t1 = min(t1, (-B + sqi) / A);
        if (t1 <= t0) return vec4(btsShoulder(acc * 0.1, 1.5), 0.0);
    }
    vec3 sheets = vec3(0.0);
    if (mode > 0.5 && mode < 1.5 && abs(rd.y) > 1e-4) {
        float slant = 1.0 / abs(rd.y);
        float tm = (0.5 * (u_Box.z + u_Box.w) - ro.y) / rd.y;
        if (tm > t0 && tm < t1) sheets += mvFans(ro + rd * tm, slant, t);
        float tf = (u_Box.z + 0.1 - ro.y) / rd.y;
        if (tf > t0 - 0.2 && tf < t1 + 0.2) sheets += mvFloor(ro + rd * tf, slant, t);
    }
    float dt = (t1 - t0) / float(steps);
    // interleaved gradient noise: a fine, even dither instead of white-noise grain
    float jit = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    for (int i = 0; i < 48; i++) {
        if (i >= steps) break;
        float tt = t0 + (float(i) + jit) * dt;
        vec3 p = ro + rd * tt;
        vec3 e;
        if (mode < 0.5) e = mvVortex(p, dt, t, oct);
        else if (mode < 1.5) e = mvDrum(p, dt, t, oct);
        else e = mvSpine(p, dt, t);
        acc += e * dt;
    }
    acc = acc * (mode < 0.5 ? 0.1 : mode < 1.5 ? 0.085 : 0.12) + sheets * u_VE.w;
    return vec4(btsShoulder(acc, 1.5), 0.0);
}
