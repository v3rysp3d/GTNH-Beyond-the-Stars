// Beyond the Stars: the Eye's cosmic objects (P5 §2.1, §6.2), the placement of bts_cofx in both profiles. One
// program, four meshes chosen by u_FxMode:
//   0 the dark core: the shared sphere, scaled to u_FxA.x (model: the voxel frame)
//   1 the Cosmic Dawn's twelve rays: a ribbon per spike of the crown (a_Position = across, along, ray), placed from
//     the crown's ellipse here and turned to the camera (model: the voxel frame)
//   2 the last-scattering surface: the shared annulus as a disc in the crown's plane (model: the crown frame)
//   3 the glowing rings: the five collars' glass channels (0..4) and the crown (5), camera-facing ribbons (model: the
//     voxel frame)
#include "cosmos/lib.glsl"

#include "cosmos/fx_uniforms.glsl"

// The crown's centre line at ellipse parameter th: cr = (y, a, b, tilt).
vec3 coCrown(vec4 cr, float th) {
    return vec3(cr.y * cos(th), cr.x + cr.z * sin(th) * sin(cr.w), cr.z * sin(th) * cos(cr.w));
}

// Ray k's flare at the Cosmic Dawn: the twelve rays fire one after another round the crown (0.45 s apart, 3 s each),
// then glow with the starburst's (1 + tau / 10)^(-5/3); the warning runs a slow glint round them.
float coRayFlare(float k) {
    float tau = u_FxC.x;
    float lk = tau - 0.45 * k;
    float burst = lk > 0.0 && lk < 3.0 ? sin(BTS_PI * lk / 3.0) : 0.0;
    float held = tau > 0.0 && tau < 300.0 ? 0.55 * pow(1.0 + tau / 10.0, -5.0 / 3.0) * (1.0 - btsSmooth01((tau - 270.0)
        / 30.0)) : 0.0;
    float glint = pow(0.5 + 0.5 * sin(u_FxB.w * BTS_TAU * 1146.0 / CO_HOUR - k * 0.5236), 6.0) * 0.35 * u_FxC.y;
    return (1.6 * burst + held) * u_FxC.w + glint;
}

vec3 coFxPlace(vec3 P, vec2 uv, out vec4 info) {
    if (u_FxMode < 0.5) {
        info = vec4(P, 0.0);
        return P * u_FxA.x;
    }
    if (u_FxMode < 1.5) {
        float k = P.z;
        float th = radians(30.0 * k + 15.0);
        vec3 base = coCrown(u_FxA, th);
        float ph = atan(base.z, base.x);
        float m = mod(k + 0.5, 3.0);
        float el = radians(m < 1.0 ? 14.0 : m < 2.0 ? -4.0 : 6.0);
        vec3 d = vec3(cos(el) * cos(ph), sin(el), cos(el) * sin(ph));
        float s = P.y;
        vec3 p = base + d * (u_FxB.x + s * u_FxB.y);
        vec3 side = cross(d, p - u_Cam);
        float sl = length(side);
        side = sl > 1e-5 ? side / sl : vec3(0.0, 1.0, 0.0);
        float fl = coRayFlare(k);
        float hw = max(u_FxB.z * (1.0 - 0.55 * s) * (1.0 + 1.4 * fl), u_FxD.x * 1.5 * length(p - u_Cam));
        info = vec4(s, P.x * 2.2, k, fl);
        return p + side * (P.x * hw * 2.2);
    }
    if (u_FxMode < 2.5) {
        vec2 e = vec2(P.x, P.z) * uv.x;
        info = vec4(e, uv.x, 0.0);
        return vec3(e.x * u_FxA.x, 0.0, e.y * u_FxA.y) * u_FxA.z;
    }
    // the rings: 0..4 the collars' glass channels (y 32 + 14 j, the collar radius), 5 the crown
    float ring = P.z;
    float th = BTS_TAU * P.y;
    vec3 c;
    vec3 tg;
    if (ring < 4.5) {
        float y = 32.0 + 14.0 * ring;
        float R = 14.0 - (y - 2.0 - 30.0) * 0.12;
        c = vec3(R * cos(th), y, R * sin(th));
        tg = vec3(-sin(th), 0.0, cos(th));
    } else {
        c = coCrown(u_FxC, th);
        tg = normalize(coCrown(u_FxC, th + 0.01) - coCrown(u_FxC, th - 0.01));
    }
    vec3 side = cross(tg, c - u_Cam);
    float sl = length(side);
    side = sl > 1e-5 ? side / sl : vec3(0.0, 1.0, 0.0);
    float hw = max(u_FxA.x * (ring > 4.5 ? 2.6 : 1.0), u_FxD.x * 1.5 * length(c - u_Cam));
    info = vec4(P.y, P.x * 2.0, ring, 0.0);
    return c + side * (P.x * hw * 2.0);
}
