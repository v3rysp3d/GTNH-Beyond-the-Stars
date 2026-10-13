#version 120
// Quasar effects (renders v2 §6.2): legacy profile fragment shader; the look is in quasar/fx.glsl.

#include "quasar/fx.glsl"

varying vec3 v_Pos;
varying vec4 v_Misc;



void main() {
    vec3 c = qfShade(v_Pos, v_Misc);
    if (c.r < 0.0) discard;
    gl_FragColor = vec4(c, 1.0);
}
