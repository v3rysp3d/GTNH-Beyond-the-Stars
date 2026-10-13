#version 330 core
// Machine volumes (renders v2 WP-7 / WP-8): Angelica core profile fragment shader; the look is in fx/vol.glsl.

#include "fx/vol.glsl"

in vec3 v_Local;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = mvShade(v_Local);
    if (c.a < 0.0) discard;
    fragColor = c;
}
