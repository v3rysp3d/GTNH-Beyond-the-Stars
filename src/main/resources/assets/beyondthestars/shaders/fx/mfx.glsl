// Beyond the Stars: machine effects (renders v2 WP-6..WP-8), the body of bts_mfx in both profiles.
// Premultiplied output (blend ONE, ONE_MINUS_SRC_ALPHA); the glowing modes leave alpha 0 (additive).
//   0 a field shell: rim-lit (Fresnel), a hexagonal cell pattern whose cells shimmer (Reality Diffraction's field).
//   1 a holographic glyph ring: angular glyphs on a 4 x 5 grid in cells round the ring, edge lines, flicker and
//     scanlines, turning (Reality Refab's trays).
//   2 a reassembling voxel: a small glowing square cube face, brightest as it locks into its cell.
//   3 a portal disc: a bent starfield and nebula of somewhere else inside a bright blue rim (the Wormhole Mouth).

#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform vec4 u_MMode;
uniform vec4 u_MA;
uniform vec4 u_MB;
uniform vec4 u_MC;
uniform vec3 u_Cam;
uniform sampler2D u_Noise;

float mxHex(vec2 p, out vec2 id) {
    vec2 s = vec2(1.0, 1.7320508);
    vec2 h = s * 0.5;
    vec2 a = mod(p, s) - h;
    vec2 b = mod(p - h, s) - h;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    id = p - g;
    vec2 q = abs(g);
    return 0.5 - max(dot(q, s * 0.5), q.x);
}

vec4 mxShell(vec3 local, vec3 n) {
    vec3 v = normalize(u_Cam - local);
    float nv = abs(dot(normalize(n), v));
    float fres = pow(1.0 - nv, 2.5);
    float t = u_MMode.y;
    vec2 uv = vec2(atan(n.z, n.x) * 7.0, asin(clamp(n.y, -1.0, 1.0)) * 7.0);
    vec2 id;
    float e = mxHex(uv, id);
    float w = fwidth(e) * 1.5 + 0.015;
    float line = 1.0 - smoothstep(0.0, w + 0.03, e);
    float cell = btsHash12(id + floor(t * 1.3));
    float sh = pow(cell, 6.0) * 0.6;
    float sweep = 0.5 + 0.5 * sin(n.y * 6.0 - t * 1.8);
    vec3 c = u_MC.rgb * (fres * (0.35 + 0.65 * line) * (0.6 + 0.4 * sweep) + (line * 0.12 + sh * 0.25) * (0.3 + fres));
    return vec4(btsShoulder(c * u_MB.x, 1.5), 0.0);
}

vec4 mxGlyph(vec2 uv) {
    float t = u_MMode.y;
    float n = u_MB.x;
    float cc = uv.y * n + u_MB.y * n;
    float cell = floor(cc);
    float fx = fract(cc);
    float fy = uv.x;
    float c = 0.0;
    // edge lines
    float wl = 0.035;
    c += (1.0 - smoothstep(0.0, wl, abs(fy - 0.08))) * 0.6;
    c += (1.0 - smoothstep(0.0, wl, abs(fy - 0.92))) * 0.6;
    // ticks between glyphs
    c += (1.0 - smoothstep(0.0, 0.04, abs(fx - 0.02))) * step(0.12, fy) * step(fy, 0.24) * 0.5;
    // the glyph: 4 x 5 cells, lit by the cell's hash; some cells blank
    if (fx > 0.14 && fx < 0.86 && fy > 0.2 && fy < 0.8 && btsHash12(vec2(cell, 1.1)) > 0.12) {
        float gx = (fx - 0.14) / 0.72 * 4.0;
        float gy = (fy - 0.2) / 0.6 * 5.0;
        vec2 g = floor(vec2(gx, gy));
        vec2 f = fract(vec2(gx, gy));
        float on = step(0.48, btsHash12(g + vec2(cell * 4.1, 7.0)));
        float px = smoothstep(0.08, 0.2, f.x) * smoothstep(0.08, 0.2, 1.0 - f.x) * smoothstep(0.08, 0.2, f.y)
            * smoothstep(0.08, 0.2, 1.0 - f.y);
        // a glyph re-writes itself now and then
        float rew = step(0.92, btsHash12(vec2(cell, floor(t * 0.7))));
        c += on * px * (0.9 + 0.8 * rew);
    }
    float flick = 0.85 + 0.15 * btsHash12(vec2(floor(t * 14.0), cell));
    float scan = 0.82 + 0.18 * sin(gl_FragCoord.y * 1.4 + t * 9.0);
    vec3 col = u_MC.rgb * c * flick * scan * u_MB.z;
    return vec4(btsShoulder(col, 1.5), 0.0);
}

vec4 mxVoxelShade(vec2 uv, vec2 fx) {
    vec2 d = abs(uv * 2.0 - 1.0);
    float e = max(d.x, d.y);
    if (e > 1.0) return vec4(0.0);
    float edge = exp(-(1.0 - e) / 0.09);
    float face = 0.22 + 0.15 * (1.0 - d.x * d.y);
    vec3 col = u_MC.rgb * (edge * 1.1 + face) + vec3(1.0) * fx.y * 1.4 * (1.0 - e * 0.6);
    return vec4(btsShoulder(col * fx.x, 1.5), 0.0);
}

vec4 mxPortal(vec2 uv) {
    float t = u_MMode.y;
    float r = uv.x;
    float a = uv.y * BTS_TAU;
    // the starfield bends round the throat: the angle is swirled more toward the centre
    float sw = a + 1.6 / (r + 0.12) - t * 0.15;
    vec2 q = vec2(cos(sw), sin(sw)) * pow(r, 0.6) * 9.0;
    vec2 cid = floor(q * 3.0);
    vec2 cf = fract(q * 3.0) - 0.5;
    vec2 jp = btsHash22(cid) - 0.5;
    float st = btsHash12(cid + 3.0);
    float star = exp(-dot(cf - jp * 0.7, cf - jp * 0.7) / 0.006) * step(0.72, st) * (0.5 + st);
    float neb = btsFbm2(u_Noise, q * 2.0 + vec2(t * 0.05, 0.0), 3);
    vec3 inner = vec3(0.05, 0.07, 0.18) + vec3(0.25, 0.15, 0.55) * pow(neb, 2.5) * 0.9 + vec3(0.85, 0.9, 1.0) * star;
    float rimD = (r - 0.93) / 0.05;
    float rim = exp(-rimD * rimD) * (0.75 + 0.25 * sin(a * 6.0 - t * 3.0));
    float glow = exp(-(1.0 - r) / 0.12) * 0.35;
    vec3 rimC = u_MC.rgb * (rim * 1.8 + glow);
    float alpha = 0.88 * (1.0 - smoothstep(0.9, 1.0, r)) * u_MB.x;
    vec3 col = inner * alpha + btsShoulder(rimC, 1.5) * u_MB.x;
    return vec4(col, alpha);
}

vec4 mxShade(vec3 local, vec2 uv, vec3 n, vec2 fx) {
    float mode = u_MMode.x;
    if (mode < 0.5) return mxShell(local, n);
    if (mode < 1.5) return mxGlyph(uv);
    if (mode < 2.5) return mxVoxelShade(uv, fx);
    return mxPortal(uv);
}
