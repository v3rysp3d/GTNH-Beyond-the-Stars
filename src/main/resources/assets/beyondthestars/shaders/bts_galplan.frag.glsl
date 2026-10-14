#version 120
// The Eye's planets mode (renders v2 §9.1), the planets and their orbits; the look is in galaxy/planets.glsl:
// legacy profile fragment shader.

#include "galaxy/planets.glsl"

varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galPlanets(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    gl_FragColor = c;
}
