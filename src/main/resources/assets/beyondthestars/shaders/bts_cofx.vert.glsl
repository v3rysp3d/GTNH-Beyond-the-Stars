#version 120
// The Eye's cosmic objects (P5 §2.1, §6.2), the effects (core, rays, membrane, rings): legacy profile vertex shader.
// The placement is in cosmos/fx_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;

varying vec3 v_P;
varying vec4 v_Info;

#include "cosmos/fx_vert.glsl"

void main() {
    vec4 info;
    vec3 p = coFxPlace(a_Position, a_UV, info);
    v_P = p;
    v_Info = info;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(p, 1.0);
}
