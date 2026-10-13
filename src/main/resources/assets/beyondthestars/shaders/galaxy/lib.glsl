// Beyond the Stars: the Eye's galaxies (renders v2 §9), the model the point stars (bts_galpts) and the diffuse disc
// (bts_galdisc) share, so the stars crowd exactly where the disc glows and the dust lanes lie where the stars dim.
//
// The disc frame: x, y in the disc plane, z along its normal, blocks from the galaxy's centre. Stars turn
// anticlockwise seen from +z on a flat rotation curve, Omega(r) = v0 / sqrt(r^2 + rc^2), quantised to whole turns per
// hour so the hour-long shader clock wraps without a jump. The arms are a density wave: log spirals turning rigidly
// with the pattern angle (u_Gal2.x), trailing (the ridge falls behind as r grows). Old stars are bunched toward the
// ridge (a phase compression of their orbits, no winding up); young blue stars light up as they cross the ridge and
// fade downstream; the dust lies just upstream of it, on the arm's inner edge.
#include "lib/common.glsl"

#define GAL_HOUR 3600.0

// The flat rotation curve: whole turns per hour at radius r (v0 rad/s x blocks, rc the core radius).
float galTurns(float r, float v0, float rc) {
    float om = v0 / sqrt(r * r + rc * rc);
    return max(1.0, floor(om * GAL_HOUR / BTS_TAU + 0.5));
}

// The angle (radians, 0..2pi) after t seconds at k whole turns per hour: exact at the clock's wrap.
float galTurnAngle(float k, float t) {
    return BTS_TAU * fract(k * (t / GAL_HOUR));
}

// The arms' ridge angle at radius r: the pattern angle less the log-spiral wind (cot the pitch's cotangent).
float galRidge(float r, float pattern, float cot, float r0) {
    return pattern - log(max(r, r0) / r0) * cot;
}

// The phase past the ridge for an m-armed pattern, 0..2pi (0 on the ridge, growing downstream).
float galPhase(float theta, float ridge, float m) {
    return mod(m * (theta - ridge), BTS_TAU);
}

// Young stars and H II: born on the ridge, fading downstream (a sharp leading edge, a long soft tail).
float galYoung(float phi) {
    return smoothstep(0.0, 0.10, phi) * exp(-phi / 0.85) + 0.18 * exp(-(BTS_TAU - phi) / 0.12);
}

// The dust lane: a narrow band just upstream of the ridge (the arm's concave inner edge).
float galDust(float phi) {
    float d = phi - (BTS_TAU - 0.24);
    float w = phi + 0.24;
    return exp(-d * d / 0.030) + exp(-w * w / 0.030) * 0.6 + 0.25 * exp(-(BTS_TAU - phi) / 0.5);
}

// The old stars' density gain toward the ridge for compression a (0..0.9): 1 / (1 - a cos phi), normalised.
float galCrowd(float phi, float a) {
    return sqrt(1.0 - a * a) / (1.0 - a * cos(phi));
}

// erf, Winitzki's approximation (|error| < 2e-3, plenty for a column).
float galErf(float x) {
    float x2 = x * x;
    float ax2 = 0.147 * x2;
    float e = sqrt(1.0 - exp(-x2 * (1.2732395 + ax2) / (1.0 + ax2)));
    return x < 0.0 ? -e : e;
}

// The column of a unit Gaussian layer exp(-(z / h)^2) along a ray segment whose height runs z0 -> z1 over length len.
float galLayer(float z0, float z1, float len, float h) {
    float dz = z1 - z0;
    if (abs(dz) < 1e-3 * h) {
        float zm = 0.5 * (z0 + z1) / h;
        return exp(-zm * zm) * len;
    }
    return 0.886227 * h * len / dz * (galErf(z1 / h) - galErf(z0 / h));
}

// The column of an ellipsoidal Gaussian exp(-|S (p - c)|^2) (S = 1 / the semi-axes, in the frame the axes ux, uy span
// with uz = their normal) along o + t d, t in [ta, tb].
float galBlob(vec3 o, vec3 d, vec3 c, vec3 inv, vec2 ux, float ta, float tb) {
    vec3 q = o - c;
    // the blob's frame turns about z by the angle whose cosine and sine are ux
    vec3 qq = vec3(q.x * ux.x + q.y * ux.y, -q.x * ux.y + q.y * ux.x, q.z) * inv;
    vec3 dd = vec3(d.x * ux.x + d.y * ux.y, -d.x * ux.y + d.y * ux.x, d.z) * inv;
    float a = max(dot(dd, dd), 1e-6);
    float b = dot(qq, dd);
    float c0 = dot(qq, qq) - b * b / a;
    if (c0 > 12.0) return 0.0;
    float sa = sqrt(a);
    float t0 = b / a;
    return exp(-c0) * 0.886227 / sa * (galErf(sa * (tb + t0)) - galErf(sa * (ta + t0)));
}
