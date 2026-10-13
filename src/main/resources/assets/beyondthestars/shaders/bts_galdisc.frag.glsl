#version 120
// The Eye's galaxies (renders v2 §9.2), the diffuse light; the look is in galaxy/disc.glsl: legacy profile fragment
// shader.

#include "galaxy/disc.glsl"

varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galDisc(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    gl_FragColor = c;
}
