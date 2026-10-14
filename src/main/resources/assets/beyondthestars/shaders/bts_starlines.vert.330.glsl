#version 330 core
// The Eye's star loops (renders v2 §7.2, §8.2); the placement is in star/lines_vert.glsl: Angelica core profile vertex shader.

#include "lib/common.glsl"

in vec3 a_Position;
in vec2 a_UV;

uniform mat4 u_MVP;
// the centre relative to the camera
uniform vec3 u_Centre;
// xyz the magnetic axis (world)
uniform vec4 u_Mag;
// x mode, y count, z size, w width
uniform vec4 u_Lines;
// x twist, y open, z star radius, w time
uniform vec4 u_Lines2;
// the surface turn (cos, sin)
uniform vec2 u_Turn;
// blocks per pixel at one block
uniform float u_Pixel;
out vec4 v_Misc;
#include "star/lines_vert.glsl"

void main() {
    vec3 local;
    vec4 misc;
    slPlace(a_Position, a_UV, local, misc);
    v_Misc = misc;
    gl_Position = u_MVP * vec4(local, 1.0);
}
