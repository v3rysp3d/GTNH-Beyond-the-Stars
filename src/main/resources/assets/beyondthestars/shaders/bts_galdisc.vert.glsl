#version 120
// The Eye's galaxies (renders v2 §9.2), the diffuse light: legacy profile vertex shader.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
// the quad: centre and half axes, camera-relative blocks
uniform vec3 u_QC;
uniform vec3 u_QU;
uniform vec3 u_QV;
varying vec3 v_Pos;
// the UV only keeps the attribute bound (GT requires every declared attribute)
varying vec2 v_UV;

void main() {
    vec3 local = u_QC + a_Position.x * u_QU + a_Position.y * u_QV;
    v_UV = a_UV;
    v_Pos = local;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
