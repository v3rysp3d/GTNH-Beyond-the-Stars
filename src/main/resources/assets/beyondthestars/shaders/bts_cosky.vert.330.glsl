#version 330 core
// The Great Attractor's lensed backdrop (P5 §2.1): Angelica core profile vertex shader. The look is in
// cosmos/sky.glsl.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
uniform vec3 u_Centre;
// x radius (blocks)
uniform vec4 u_Sky;

out vec3 v_Dir;
out vec3 v_Pos;
// the UV only keeps the attribute bound (GT requires every declared attribute)
out vec2 v_UV;

void main() {
    vec3 local = a_Position * u_Sky.x;
    v_Dir = a_Position;
    v_Pos = u_Centre + local;
    v_UV = a_UV;
    gl_Position = u_MVP * vec4(local, 1.0);
}
