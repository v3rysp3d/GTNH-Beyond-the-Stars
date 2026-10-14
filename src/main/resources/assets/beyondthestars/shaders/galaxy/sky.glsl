// Beyond the Stars: the Eye's galaxies (renders v2 §9.1), the dark backdrop (bts_galsky) in both profiles: the far
// side of a sphere just inside the Eye's clearance (back faces only, TecTech's Eye of Harmony trick) shows deep space,
// near black with a faint violet / teal nebula and a sparse field of stars, so a galaxy reads as light against the
// dark even at noon. Premultiplied (ONE, ONE_MINUS_SRC_ALPHA): its alpha darkens the sky and the cavity behind it; the
// rim fades where the sphere turns edge-on, so it never shows a hard circle.
#include "lib/noise.glsl"

uniform sampler2D u_Noise;
// the eye relative to the render origin
uniform vec3 u_Eye;
// x radius (blocks), y alpha (day 0.75, night 0.5), z nebula strength, w time (s)
uniform vec4 u_Sky;

vec4 galSky(vec3 dir, vec3 pos) {
    vec3 view = normalize(pos - u_Eye);
    float rim = smoothstep(0.0, 0.5, abs(dot(dir, view)));
    float n1 = btsFbm3(u_Noise, dir * 2.3 + vec3(5.0, 1.0, 7.0), 3);
    float n2 = btsFbm3(u_Noise, dir * 5.1 + vec3(17.0, 3.0, 9.0), 2);
    vec3 neb = vec3(0.13, 0.05, 0.20) * smoothstep(0.42, 0.85, n1) + vec3(0.02, 0.09, 0.13) * smoothstep(0.48, 0.9, n2);
    // a faint band of unresolved stars across the backdrop
    float mw = exp(-dir.y * dir.y * 18.0) * (0.5 + 0.5 * n2);
    neb += vec3(0.07, 0.065, 0.08) * mw;
    // sparse stars: one candidate per cell of a 3D grid the sphere passes through
    vec3 q = dir * 70.0;
    vec3 cell = floor(q);
    float h = btsHash13(cell);
    float star = 0.0;
    if (h > 0.972) {
        vec3 off = vec3(btsHash13(cell + 1.3), btsHash13(cell + 2.7), btsHash13(cell + 5.1)) - 0.5;
        float dd = length(fract(q) - 0.5 - off * 0.5);
        float tw = 0.75 + 0.25 * sin(u_Sky.w * (1.5 + 3.0 * h) + h * 60.0);
        star = (1.0 - smoothstep(0.0, 0.16, dd)) * (0.3 + 0.7 * (h - 0.972) / 0.028) * tw;
    }
    vec3 c = neb * u_Sky.z + vec3(0.85, 0.9, 1.0) * star * 0.9;
    // only round the cavity's height: above the core band (the open bowl) and far below it, no dark cap
    float band = (1.0 - smoothstep(0.30, 0.52, dir.y)) * (1.0 - smoothstep(0.50, 0.78, -dir.y));
    float a = u_Sky.y * rim * band;
    return vec4(c * rim, a);
}
