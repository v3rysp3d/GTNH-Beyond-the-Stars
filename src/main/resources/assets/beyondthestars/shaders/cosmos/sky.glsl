// Beyond the Stars: the Great Attractor's backdrop (bts_cosky, P5 §2.1, §9.5) in both profiles. The far side of a
// sphere round the dark core (front faces culled, the galaxies' backdrop trick, galaxy/sky.glsl) shows deep space:
// stars and a faint nebula, LENSED by the core. Each fragment's view ray passes the core at impact parameter b
// (blocks, in the lens plane); the point-lens equation beta = theta - theta_E^2 / theta, i.e. s = q (1 - b_E^2 / b^2)
// in the lens plane, gives the source the ray came from, so every background star shows its two images: the outer
// one pushed out and stretched along the ring, the faint inner one inside it, and the stars near the line of sight
// merge into an Einstein ring at b = b_E. That is how dark matter is mapped (weak and strong lensing). The ring glows
// faintly as well. Premultiplied (ONE, ONE_MINUS_SRC_ALPHA): the alpha darkens what is behind; the disc fades out
// toward its rim by b, so it never shows a hard circle.
#include "lib/noise.glsl"

uniform sampler2D u_Noise;
// the eye relative to the render origin
uniform vec3 u_Eye;
// the core, camera-relative
uniform vec3 u_Centre;
// x radius (blocks), y alpha (darkening), z nebula strength, w time (s)
uniform vec4 u_Sky;
// x the Einstein radius b_E (blocks; 0: no lensing), y the ring's glow, z star density 0..1, w nebula octaves
uniform vec4 u_Lens;

// One cell's star at the source direction src (cells of 1 / 150 rad), with a footprint of fw cells per pixel.
vec3 coStar(vec3 src, float fw) {
    vec3 sp = src * 150.0;
    vec3 cell = floor(sp);
    float h = btsHash13(cell);
    float thr = 1.0 - 0.075 * u_Lens.z;
    if (h <= thr) return vec3(0.0);
    vec3 off = vec3(btsHash13(cell + 1.3), btsHash13(cell + 2.7), btsHash13(cell + 5.1)) - 0.5;
    vec3 lp = fract(sp) - 0.5 - off * 0.6;
    lp -= src * dot(lp, src);
    float rad = max(0.085, 0.8 * fw);
    float m = (h - thr) / max(1.0 - thr, 1e-4);
    float tw = 0.8 + 0.2 * sin(u_Sky.w * (1.3 + 2.7 * m) + h * 60.0);
    float s = exp(-dot(lp, lp) / (rad * rad)) * (0.12 + 0.88 * m * m * m) * tw;
    vec3 col = btsHash13(cell + 9.7) < 0.7 ? vec3(0.80, 0.86, 1.0) : vec3(1.0, 0.86, 0.70);
    return col * s;
}

vec4 coSky(vec3 n, vec3 pos) {
    vec3 L = u_Centre - u_Eye;
    vec3 d = normalize(pos - u_Eye);
    // q: the ray's closest approach to the core, from the core (blocks); |q| the impact parameter
    vec3 q = d * dot(L, d) - L;
    float b = length(q);
    float R = u_Sky.x;
    float disc = 1.0 - smoothstep(0.5 * R, 0.97 * R, b);
    float bE = u_Lens.x;
    vec3 src = d;
    if (bE > 0.0) {
        float k = 1.0 - bE * bE / max(b * b, 0.01);
        src = normalize(L + q * k);
    }
    vec3 sp = src * 150.0;
    float fw = (length(dFdx(sp)) + length(dFdy(sp))) * 0.5;
    vec3 c = coStar(src, fw) * 1.4;
    int oct = int(u_Lens.w + 0.5);
    if (oct > 0) {
        float n1 = btsFbm3(u_Noise, src * 2.6 + vec3(5.0, 1.0, 7.0), oct);
        float n2 = btsFbm3(u_Noise, src * 6.1 + vec3(17.0, 3.0, 9.0), oct);
        c += (vec3(0.12, 0.05, 0.20) * smoothstep(0.42, 0.85, n1) + vec3(0.02, 0.08, 0.13) * smoothstep(0.48, 0.9, n2))
            * u_Sky.z;
    }
    if (bE > 0.0) {
        float x = (b - bE) / (0.22 * bE);
        float line = (b - bE) / (0.035 * bE + 0.6 * fw / 150.0 * length(L));
        c += vec3(0.50, 0.62, 1.0) * (0.30 * exp(-x * x) + 0.22 * exp(-line * line)) * u_Lens.y;
    }
    float a = u_Sky.y * disc;
    return vec4(c * disc, a);
}
