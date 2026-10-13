#version 330 core
// The Eye's galaxies (renders v2 §9.2), the point stars: Angelica core profile vertex shader. The motions are in
// galaxy/points_vert.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;

out vec4 v_Col;
out float v_Px;
out float v_Soft;

#include "galaxy/points_vert.glsl"

void main() {
    float group = floor(a_UV.x / 8.0);
    float bright;
    float pink;
    float soft;
    vec3 pos = galPlace(a_Position, group, bright, pink, soft);
    gl_Position = u_MVP * vec4(pos, 1.0);
    float px;
    v_Col = galVertex(a_UV, gl_Position.w, pos, bright, pink, px);
    v_Px = px;
    v_Soft = soft;
    // an unlit star costs no fragments
    if (v_Col.a < 0.002) gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
}
