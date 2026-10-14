#version 330 core
// Quasar ring (renders v2 §6.2): Angelica core profile vertex shader; the placement is in quasar/ring_vert.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec3 u_Centre;
uniform vec4 u_Ring;
uniform vec4 u_Ray;
uniform vec3 u_NearC;
uniform vec3 u_NearU;
uniform vec3 u_NearV;

out vec3 v_Pos;
out float v_Kind;

#include "quasar/ring_vert.glsl"

void main() {
    float kind;
    vec3 local = qrPlace(a_Position, a_UV, kind);
    v_Kind = kind;
    v_Pos = u_Centre + local;
    gl_Position = u_MVP * vec4(local, 1.0);
}
