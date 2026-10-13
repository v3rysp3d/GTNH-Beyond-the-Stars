#version 120
// The Eye's galaxies (renders v2 §9.1), the dark backdrop; the look is in galaxy/sky.glsl: legacy profile fragment
// shader.

#include "galaxy/sky.glsl"

varying vec3 v_Dir;
varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = galSky(normalize(v_Dir), v_Pos);
    if (c.a < 0.003) discard;
    gl_FragColor = c;
}
