#version 330 core
// The Eye's star loops (renders v2 §7.2, §8.2); the look is in star/lines.glsl: Angelica core profile fragment shader.

#include "star/lines.glsl"

in vec4 v_Misc;
layout(location = 0) out vec4 fragColor;

void main() {
    vec3 c = slShade(v_Misc);
    if (c.r < 0.0) discard;
    fragColor = vec4(c, 1.0);
}
