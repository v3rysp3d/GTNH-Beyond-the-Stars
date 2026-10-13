#version 120
// The Eye's star loops (renders v2 §7.2, §8.2); the look is in star/lines.glsl: legacy profile fragment shader.

#include "star/lines.glsl"

varying vec4 v_Misc;

void main() {
    vec3 c = slShade(v_Misc);
    if (c.r < 0.0) discard;
    gl_FragColor = vec4(c, 1.0);
}
