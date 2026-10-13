#version 120
// Black-hole lens (renders v2 §5.2): legacy profile fragment shader; the look is in bh/lens.glsl.

#include "bh/lens.glsl"

varying vec3 v_Pos;

void main() {
    vec4 c = bhShade(normalize(v_Pos));
    // nothing here: no colour and no depth (the glass and the world behind stay as they are)
    if (c.a < 0.004 && c.r + c.g + c.b < 0.004) discard;
    gl_FragColor = c;
}
