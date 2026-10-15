#version 330 core
// The Eye's cosmic objects (P5 §2.1, §6.2), the effects (core, rays, membrane, rings): Angelica core profile vertex
// shader. The placement is in cosmos/fx_vert.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;

out vec3 v_P;
out vec4 v_Info;

#include "cosmos/fx_vert.glsl"

void main() {
    vec4 info;
    vec3 p = coFxPlace(a_Position, a_UV, info);
    v_P = p;
    v_Info = info;
    gl_Position = u_MVP * vec4(p, 1.0);
}
