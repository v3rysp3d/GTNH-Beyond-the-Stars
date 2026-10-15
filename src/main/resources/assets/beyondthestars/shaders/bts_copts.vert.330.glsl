#version 330 core
// The Eye's cosmic objects (P5 §2.1, §6.2), the points: Angelica core profile vertex shader. The motions are in
// cosmos/points_vert.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;

out vec4 v_Col;
out float v_Px;
out float v_Kind;
out vec2 v_Rot;
out float v_Ell;

#include "cosmos/points_vert.glsl"

void main() {
    vec4 col;
    float sizeB;
    float kind;
    vec2 rot;
    float ell;
    vec3 pos = coPlace(a_Position, a_UV, col, sizeB, kind, rot, ell);
    gl_Position = u_MVP * vec4(pos, 1.0);
    float fade;
    float px;
    gl_PointSize = coPointSize(sizeB, gl_Position.w, fade, px);
    v_Col = vec4(col.rgb, col.a * fade);
    v_Px = px;
    v_Kind = kind;
    v_Rot = rot;
    v_Ell = ell;
    // a dark point costs no fragments
    if (v_Col.a < 0.002) gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
}
