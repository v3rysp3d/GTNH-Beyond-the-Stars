#version 120
// The Eye's cosmic objects (P5 §2.1, §6.2), the points: legacy profile fragment shader.

#include "cosmos/points_frag.glsl"

varying vec4 v_Col;
varying float v_Px;
varying float v_Kind;
varying vec2 v_Rot;
varying float v_Ell;

void main() {
    vec4 c = coSprite(gl_PointCoord, v_Col, v_Px, v_Kind, v_Rot, v_Ell);
    if (c.a < 0.0) discard;
    gl_FragColor = c;
}
