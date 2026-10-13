#version 120
// Machine effects (renders v2 WP-6..WP-8): legacy profile vertex shader; the placement is in fx/mfx_vert.glsl.

#include "lib/common.glsl"

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec4 u_MMode;
uniform vec4 u_MA;
uniform vec4 u_MB;
uniform vec3 u_Cam;

varying vec3 v_Local;
varying vec2 v_UV;
varying vec3 v_N;
varying vec2 v_Fx;

#include "fx/mfx_vert.glsl"

void main() {
    vec3 local;
    vec2 uv;
    vec3 n;
    vec2 fx;
    mxPlace(a_Position, a_UV, local, uv, n, fx);
    v_Local = local;
    v_UV = uv;
    v_N = n;
    v_Fx = fx;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
