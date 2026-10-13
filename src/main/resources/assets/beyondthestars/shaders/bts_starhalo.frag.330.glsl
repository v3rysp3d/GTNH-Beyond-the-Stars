#version 330 core
// The Eye's star surroundings (renders v2 §7.2); the look is in star/halo.glsl: Angelica core profile fragment shader.

#include "star/halo.glsl"

in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = starHalo(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    fragColor = c;
}
