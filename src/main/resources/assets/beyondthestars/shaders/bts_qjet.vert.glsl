#version 120
// Quasar jet (renders v2 §6.2): legacy profile vertex shader; the placement is in quasar/jet_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec4 u_JSpan;
uniform vec4 u_JAxis;
uniform vec3 u_Side;

varying float v_Z;
varying float v_U;

#include "quasar/jet_vert.glsl"

void main() {
    vec3 local;
    float z;
    float u;
    qjPlace(a_Position, a_UV, local, z, u);
    v_Z = z;
    v_U = u;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
