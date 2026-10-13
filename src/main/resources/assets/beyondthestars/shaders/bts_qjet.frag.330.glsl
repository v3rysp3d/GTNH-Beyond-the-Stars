#version 330 core
// Quasar jet (renders v2 §6.2): Angelica core profile fragment shader; the look is in quasar/jet.glsl.

#include "quasar/jet.glsl"

in float v_Z;
in float v_U;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = qjShade(v_Z, v_U);
}
