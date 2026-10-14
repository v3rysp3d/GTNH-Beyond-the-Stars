#version 120
// Quasar ring (renders v2 §6.2): legacy profile vertex shader; the placement is in quasar/ring_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec3 u_Centre;
uniform vec4 u_Ring;
uniform vec4 u_Ray;
uniform vec3 u_NearC;
uniform vec3 u_NearU;
uniform vec3 u_NearV;

varying vec3 v_Pos;
varying float v_Kind;

#include "quasar/ring_vert.glsl"

void main() {
    float kind;
    vec3 local = qrPlace(a_Position, a_UV, kind);
    v_Kind = kind;
    v_Pos = u_Centre + local;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
