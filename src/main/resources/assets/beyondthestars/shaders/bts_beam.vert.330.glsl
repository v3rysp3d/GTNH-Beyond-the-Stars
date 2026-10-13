#version 330 core
// Beams and streams (renders v2 WP-6..WP-8): Angelica core profile vertex shader (fx/beam_vert.glsl).

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec3 u_P0;
uniform vec3 u_P1;
uniform vec3 u_P2;
uniform vec4 u_BW;

out float v_S;
out float v_U;
out float v_W;

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
    gl_Position = u_MVP * vec4(world, 1.0);
}
