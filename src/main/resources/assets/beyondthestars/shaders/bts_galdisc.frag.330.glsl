#version 330 core
// The Eye's galaxies (renders v2 §9.2), the diffuse light; the look is in galaxy/disc.glsl: Angelica core profile fragment
// shader.

#include "galaxy/disc.glsl"

in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galDisc(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    fragColor = c;
}
