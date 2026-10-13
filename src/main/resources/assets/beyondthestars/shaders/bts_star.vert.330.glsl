#version 330 core
// The Eye's star photosphere (renders v2 §7.2): Angelica core profile vertex shader.

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
// the centre relative to the camera; x of u_Star the radius
uniform vec3 u_Centre;
uniform vec4 u_Star;
out vec3 v_Pos;
// the UV only keeps the attribute bound (GT requires every declared attribute)
out vec2 v_UV;

void main() {
    // the mesh is a little larger than the star: the fragment shader cuts the true sphere out of it
    vec3 local = a_Position * (u_Star.x * 1.012);
    v_UV = a_UV;
    v_Pos = u_Centre + local;
    gl_Position = u_MVP * vec4(local, 1.0);
}
