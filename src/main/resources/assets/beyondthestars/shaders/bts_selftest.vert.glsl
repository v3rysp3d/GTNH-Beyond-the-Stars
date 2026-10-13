#version 120
// Render kit self test (renders v2 WP-0): legacy profile vertex shader.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;

varying vec2 v_UV;

void main() {
    v_UV = a_UV;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(a_Position, 1.0);
}
