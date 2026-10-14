#version 120
// Machine volumes (renders v2 WP-7 / WP-8): legacy profile vertex shader. The proxy is the unit cylinder (x, z the
// unit circle, the height 0..1 in UV v) scaled to u_Box; the fragment shader marches (fx/vol.glsl).

attribute vec3 a_Position;
attribute vec2 a_UV;

uniform mat4 u_ModelMatrix;
uniform vec4 u_Box;

varying vec3 v_Local;

void main() {
    vec3 local = vec3(a_Position.x * u_Box.x, mix(u_Box.z, u_Box.w, a_UV.y), a_Position.z * u_Box.y);
    v_Local = local;
    gl_Position = gl_ModelViewProjectionMatrix * u_ModelMatrix * vec4(local, 1.0);
}
