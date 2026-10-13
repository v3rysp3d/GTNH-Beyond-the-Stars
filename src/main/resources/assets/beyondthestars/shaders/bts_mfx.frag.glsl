#version 120
// Machine effects (renders v2 WP-6..WP-8): legacy profile fragment shader; the look is in fx/mfx.glsl.

#include "fx/mfx.glsl"

varying vec3 v_Local;
varying vec2 v_UV;
varying vec3 v_N;
varying vec2 v_Fx;

void main() {
    gl_FragColor = mxShade(v_Local, v_UV, v_N, v_Fx);
}
