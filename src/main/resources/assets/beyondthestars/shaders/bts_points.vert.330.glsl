#version 330 core
// Point batch (renders v2 §2.1, §9.2): Angelica core profile vertex shader. One GL_POINTS vertex per star.

in vec3 a_Position;
// x: size in blocks; y: colour packed as r * 65536 + g * 256 + b (0..255 each)
in vec2 a_UV;

uniform mat4 u_MVP;
// x: pixels per block at distance 1; y: least size, px; z: largest size, px
uniform vec3 u_Points;

out vec3 v_Color;
out float v_Fade;

void main() {
    gl_Position = u_MVP * vec4(a_Position, 1.0);
#include "lib/points_vert.glsl"
}
