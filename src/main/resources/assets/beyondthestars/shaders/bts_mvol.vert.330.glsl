#version 330 core
// Machine volumes (renders v2 WP-7 / WP-8): Angelica core profile vertex shader (see bts_mvol.vert.glsl).

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec4 u_Box;

out vec3 v_Local;

void main() {
    vec3 local = vec3(a_Position.x * u_Box.x, mix(u_Box.z, u_Box.w, a_UV.y), a_Position.z * u_Box.y);
    v_Local = local;
    gl_Position = u_MVP * vec4(local, 1.0);
}
