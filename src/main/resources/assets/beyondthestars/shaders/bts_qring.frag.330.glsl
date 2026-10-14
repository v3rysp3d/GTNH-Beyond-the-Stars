#version 330 core
// Quasar ring (renders v2 §6.2): Angelica core profile fragment shader; the look is in quasar/ring.glsl.

#include "quasar/ring.glsl"

in vec3 v_Pos;
in float v_Kind;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = qrShade(v_Pos, floor(v_Kind + 0.5), gl_FragCoord.xy);
    if (c.a < 0.0) discard;
    fragColor = c;
}
