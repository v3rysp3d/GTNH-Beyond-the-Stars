#version 330 core
// Quasar effects (renders v2 §6.2): Angelica core profile vertex shader; the placement is in quasar/fx_vert.glsl.

#include "lib/common.glsl"

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
// the quasar centre relative to the camera, blocks
uniform vec3 u_Centre;
uniform vec4 u_Fx;
// field: line size L, start radius, twist, width; debris: age s, size, ring inner and outer radius; shell: radius
uniform vec4 u_FxA;
uniform vec4 u_FxB;
// billboard axes (debris)
uniform vec3 u_Right;
uniform vec3 u_Up;

out vec3 v_Pos;
out vec4 v_Misc;

#include "quasar/fx_vert.glsl"

void main() {
    vec3 local;
    vec4 misc;
    qfPlace(a_Position, a_UV, local, misc);
    v_Pos = u_Centre + local;
    v_Misc = misc;
    gl_Position = u_MVP * vec4(local, 1.0);
}
