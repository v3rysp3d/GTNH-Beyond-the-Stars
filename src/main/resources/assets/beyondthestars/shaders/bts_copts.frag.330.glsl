#version 330 core
// The Eye's cosmic objects (P5 §2.1, §6.2), the points: Angelica core profile fragment shader.

#include "cosmos/points_frag.glsl"

in vec4 v_Col;
in float v_Px;
in float v_Kind;
in vec2 v_Rot;
in float v_Ell;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = coSprite(gl_PointCoord, v_Col, v_Px, v_Kind, v_Rot, v_Ell);
    if (c.a < 0.0) discard;
    fragColor = c;
}
