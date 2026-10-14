#version 330 core
// Glow sprite (renders v2 §2.1): Angelica core profile vertex shader.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;

out vec2 v_UV;

void main() {
    v_UV = a_UV;
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
