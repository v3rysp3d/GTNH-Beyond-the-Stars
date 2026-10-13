#version 120
// Black-hole lens (renders v2 §5.2): legacy profile vertex shader. A camera-facing quad through the hole; each
// fragment gets its camera-relative position (blocks) to rebuild its view ray.

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
// the quad: centre and half-axes, camera-relative blocks (the same quad as u_ModelMatrix places)
uniform vec3 u_QuadC;
uniform vec3 u_QuadU;
uniform vec3 u_QuadV;

varying vec3 v_Pos;

void main() {
    v_Pos = u_QuadC + (a_UV.x * 2.0 - 1.0) * u_QuadU + (a_UV.y * 2.0 - 1.0) * u_QuadV;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(a_Position, 1.0);
}
