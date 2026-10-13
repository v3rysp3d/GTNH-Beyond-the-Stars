#version 120
// The Eye's star surroundings (renders v2 §7.2); the look is in star/halo.glsl: legacy profile fragment shader.

#include "star/halo.glsl"

varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = starHalo(normalize(v_Pos - u_Eye));
    if (c.r < 0.0) discard;
    // premultiplied: (ONE, ONE_MINUS_SRC_ALPHA)
    gl_FragColor = c;
}
