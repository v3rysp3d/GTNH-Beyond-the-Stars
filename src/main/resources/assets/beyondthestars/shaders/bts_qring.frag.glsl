#version 120
// Quasar ring (renders v2 §6.2): legacy profile fragment shader; the look is in quasar/ring.glsl.

#include "quasar/ring.glsl"

varying vec3 v_Pos;
varying float v_Kind;

void main() {
    vec4 c = qrShade(v_Pos, floor(v_Kind + 0.5), gl_FragCoord.xy);
    if (c.a < 0.0) discard;
    gl_FragColor = c;
}
