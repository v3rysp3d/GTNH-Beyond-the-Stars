#version 330 core
// Quasar jet (renders v2 §6.2): Angelica core profile vertex shader; the placement is in quasar/jet_vert.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec4 u_JSpan;
uniform vec4 u_JAxis;
// SMBH (R-19): x Overdrive 0..1, y width scale, z shedding 0/1, w supermassive 0..1
uniform vec4 u_JSmbh;
uniform vec3 u_Side;

out float v_Z;
out float v_U;

#include "quasar/jet_vert.glsl"

void main() {
    vec3 local;
    float z;
    float u;
    qjPlace(a_Position, a_UV, local, z, u);
    v_Z = z;
    v_U = u;
    gl_Position = u_MVP * vec4(local, 1.0);
}
