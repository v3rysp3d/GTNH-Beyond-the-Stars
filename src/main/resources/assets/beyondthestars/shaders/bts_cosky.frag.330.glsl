#version 330 core
// The Great Attractor's lensed backdrop (P5 §2.1); the look is in cosmos/sky.glsl: Angelica core profile fragment
// shader.

#include "cosmos/sky.glsl"

in vec3 v_Dir;
in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = coSky(normalize(v_Dir), v_Pos);
    if (c.a < 0.003 && dot(c.rgb, c.rgb) < 1e-6) discard;
    fragColor = c;
}
