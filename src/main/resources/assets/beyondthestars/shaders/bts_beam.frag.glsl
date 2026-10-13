#version 120
// Beams and streams (renders v2 WP-6..WP-8): legacy profile fragment shader; the look is in fx/beam.glsl.

#include "fx/beam.glsl"

varying float v_S;
varying float v_U;
varying float v_W;

void main() {
    gl_FragColor = bmShade(v_S, v_U, v_W);
}
