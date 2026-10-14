#version 330 core
// Supermassive Lattice effects (smbh-v1.md §7, R-19): Angelica core profile fragment shader; the look is in
// smbh/fx.glsl.

#include "smbh/fx.glsl"

in vec4 v_Misc;
in float v_Kind;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = smShade(v_Misc, floor(v_Kind + 0.5));
    if (c.a < 0.0) discard;
    fragColor = c;
}
