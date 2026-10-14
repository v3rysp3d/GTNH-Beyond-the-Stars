#version 120
// The Eye's galaxies (renders v2 §9.2), the point stars: legacy profile fragment shader.

#include "galaxy/points_frag.glsl"

varying vec4 v_Col;
varying float v_Px;
varying float v_Soft;

void main() {
    vec4 c = galSprite(gl_PointCoord, v_Col, v_Px, v_Soft);
    if (c.a < 0.0) discard;
    gl_FragColor = c;
}
