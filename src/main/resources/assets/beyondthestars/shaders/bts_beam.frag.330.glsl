#version 330 core
// Beams and streams (renders v2 WP-6..WP-8): Angelica core profile fragment shader; the look is in fx/beam.glsl.

#include "fx/beam.glsl"

in float v_S;
in float v_U;
in float v_W;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = bmShade(v_S, v_U, v_W);
}
