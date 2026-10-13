#version 330 core
// The Eye's galaxies (renders v2 §9.2), the diffuse light: Angelica core profile vertex shader.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
// the quad: centre and half axes, camera-relative blocks
uniform vec3 u_QC;
uniform vec3 u_QU;
uniform vec3 u_QV;
out vec3 v_Pos;
// the UV only keeps the attribute bound (GT requires every declared attribute)
out vec2 v_UV;

void main() {
    vec3 local = u_QC + a_Position.x * u_QU + a_Position.y * u_QV;
    v_UV = a_UV;
    v_Pos = local;
    gl_Position = u_MVP * vec4(local, 1.0);
}
