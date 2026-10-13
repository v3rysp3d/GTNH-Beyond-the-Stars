#version 330 core
// Machine effects (renders v2 WP-6..WP-8): Angelica core profile fragment shader; the look is in fx/mfx.glsl.

#include "fx/mfx.glsl"

in vec3 v_Local;
in vec2 v_UV;
in vec3 v_N;
in vec2 v_Fx;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = mxShade(v_Local, v_UV, v_N, v_Fx);
}
