#version 120
// The Eye's galaxies (renders v2 §9.2), the point stars: legacy profile vertex shader. The motions are in
// galaxy/points_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;

varying vec4 v_Col;
varying float v_Px;
varying float v_Soft;

#include "galaxy/points_vert.glsl"

void main() {
    float group = floor(a_UV.x / 8.0);
    float bright;
    float pink;
    float soft;
    vec3 pos = galPlace(a_Position, group, bright, pink, soft);
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(pos, 1.0);
    float px;
    v_Col = galVertex(a_UV, gl_Position.w, pos, bright, pink, px);
    v_Px = px;
    v_Soft = soft;
    // an unlit star costs no fragments
    if (v_Col.a < 0.002) gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
}
