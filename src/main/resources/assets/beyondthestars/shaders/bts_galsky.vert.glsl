#version 120
// The Eye's galaxies (renders v2 §9.1), the dark backdrop: legacy profile vertex shader.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec3 u_Centre;
// x radius (blocks)
uniform vec4 u_Sky;

varying vec3 v_Dir;
varying vec3 v_Pos;
// the UV only keeps the attribute bound (GT requires every declared attribute)
varying vec2 v_UV;

void main() {
    vec3 local = a_Position * u_Sky.x;
    v_Dir = a_Position;
    v_Pos = u_Centre + local;
    v_UV = a_UV;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
