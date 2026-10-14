#version 330 core
// The Eye's planets mode (renders v2 §9.1), the planets and their orbits; the look is in galaxy/planets.glsl:
// Angelica core profile fragment shader.

#include "galaxy/planets.glsl"

in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galPlanets(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    fragColor = c;
}
