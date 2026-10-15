#version 120
// The Great Attractor's lensed backdrop (P5 §2.1); the look is in cosmos/sky.glsl: legacy profile fragment shader.

#include "cosmos/sky.glsl"

varying vec3 v_Dir;
varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = coSky(normalize(v_Dir), v_Pos);
    if (c.a < 0.003 && dot(c.rgb, c.rgb) < 1e-6) discard;
    gl_FragColor = c;
}
