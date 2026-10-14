#version 120
// Beams and streams (renders v2 WP-6..WP-8): legacy profile vertex shader; the placement is in fx/beam_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec3 u_P0;
uniform vec3 u_P1;
uniform vec3 u_P2;
uniform vec4 u_BW;

varying float v_S;
varying float v_U;
varying float v_W;

#include "fx/beam_vert.glsl"

void main() {
    vec3 world;
    float s;
    float u;
    float hw;
    bmPlace(a_Position, a_UV, world, s, u, hw);
    v_S = s;
    v_U = u;
    v_W = hw;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(world, 1.0);
}
