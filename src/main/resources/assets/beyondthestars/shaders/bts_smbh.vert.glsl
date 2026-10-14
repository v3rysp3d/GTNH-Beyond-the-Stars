#version 120
// Supermassive Lattice effects (smbh-v1.md §7, R-19): legacy profile vertex shader; the placement is in
// smbh/fx_vert.glsl.

#include "lib/common.glsl"

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
// the quasar centre relative to the camera, blocks
uniform vec3 u_Centre;
uniform vec4 u_S;
// lattice: x the ring's outer edge, y arc width, z thread width (0: none), w node sprite radius; streams: x outer
// radius, y inner radius, z width, w turn (rad); halo: x radius (blocks)
uniform vec4 u_SA;
uniform vec4 u_SB;
// billboard axes (nodes, halo)
uniform vec3 u_Right;
uniform vec3 u_Up;

varying vec4 v_Misc;
varying float v_Kind;

#include "smbh/fx_vert.glsl"

void main() {
    vec3 local;
    vec4 misc;
    float kind;
    smPlace(a_Position, a_UV, local, misc, kind);
    v_Misc = misc;
    v_Kind = kind;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
