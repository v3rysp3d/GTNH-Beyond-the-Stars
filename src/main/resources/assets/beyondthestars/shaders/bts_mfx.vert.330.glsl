#version 330 core
// Machine effects (renders v2 WP-6..WP-8): Angelica core profile vertex shader (fx/mfx_vert.glsl).

#include "lib/common.glsl"

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec4 u_MMode;
uniform vec4 u_MA;
uniform vec4 u_MB;
uniform vec3 u_Cam;

out vec3 v_Local;
out vec2 v_UV;
out vec3 v_N;
out vec2 v_Fx;

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
    gl_Position = u_MVP * vec4(local, 1.0);
}
