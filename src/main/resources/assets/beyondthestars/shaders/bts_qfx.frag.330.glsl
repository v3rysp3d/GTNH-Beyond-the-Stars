#version 330 core
// Quasar effects (renders v2 §6.2): Angelica core profile fragment shader; the look is in quasar/fx.glsl.

#include "quasar/fx.glsl"

in vec3 v_Pos;
in vec4 v_Misc;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 c = qfShade(v_Pos, v_Misc);
    if (c.r < 0.0) discard;
    fragColor = vec4(c, 1.0);
}
