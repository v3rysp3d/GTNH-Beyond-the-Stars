#version 120
// Black-hole lens (renders v2 §5.2): legacy profile fragment shader; the look is in bh/lens.glsl.

#include "bh/lens.glsl"

varying vec3 v_Pos;

void main() {
    if (u_Lens.w > 0.5) {
        // the depth pass (colour writes off): where the hole has any body (the shadow at once, else a cheap march
        // without the screen copy: alpha 0.05 and up, the disc and its arcs); the glow and the photon-ring halo add
        // light but no alpha, so they write no depth
        vec3 rd = normalize(v_Pos);
        if (!bhDepthMask(rd) && bhShade(rd).a < 0.05) discard;
        // (u_Lens.w 2: the dev check, -Dbts.render.bhDepthDebug, shows it in red)
        gl_FragColor = vec4(u_Lens.w > 1.5 ? 1.0 : 0.0, 0.0, 0.0, 1.0);
        return;
    }
    vec4 c = bhShade(normalize(v_Pos));
    // nothing here: no colour
    if (c.a < 0.004 && c.r + c.g + c.b < 0.004) discard;
    gl_FragColor = c;
}
