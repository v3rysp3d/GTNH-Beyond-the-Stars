#version 330 core
// The Eye's galaxies (renders v2 §9.2), the point stars: Angelica core profile fragment shader.

#include "galaxy/points_frag.glsl"

in vec4 v_Col;
in float v_Px;
in float v_Soft;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = galSprite(gl_PointCoord, v_Col, v_Px, v_Soft);
    if (c.a < 0.0) discard;
    fragColor = c;
}
