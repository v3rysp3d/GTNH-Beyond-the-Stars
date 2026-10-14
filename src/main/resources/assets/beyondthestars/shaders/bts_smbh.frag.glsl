#version 120
// Supermassive Lattice effects (smbh-v1.md §7, R-19): legacy profile fragment shader; the look is in smbh/fx.glsl.

#include "smbh/fx.glsl"

varying vec4 v_Misc;
varying float v_Kind;

void main() {
    vec4 c = smShade(v_Misc, floor(v_Kind + 0.5));
    if (c.a < 0.0) discard;
    gl_FragColor = c;
}
