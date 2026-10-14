#version 330 core
// The Eye's galaxies (renders v2 §9.1), the dark backdrop; the look is in galaxy/sky.glsl: Angelica core profile fragment
// shader.

#include "galaxy/sky.glsl"

in vec3 v_Dir;
in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galSky(normalize(v_Dir), v_Pos);
    if (c.a < 0.003) discard;
    fragColor = c;
}
