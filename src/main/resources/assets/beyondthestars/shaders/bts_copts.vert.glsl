#version 120
// The Eye's cosmic objects (P5 §2.1, §6.2), the points: legacy profile vertex shader. The motions are in
// cosmos/points_vert.glsl.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;

varying vec4 v_Col;
varying float v_Px;
varying float v_Kind;
varying vec2 v_Rot;
varying float v_Ell;

#include "cosmos/points_vert.glsl"

void main() {
    vec4 col;
    float sizeB;
    float kind;
    vec2 rot;
    float ell;
    vec3 pos = coPlace(a_Position, a_UV, col, sizeB, kind, rot, ell);
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(pos, 1.0);
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
