#version 330 core
// The Eye's star photosphere (renders v2 §7.2); the look is in star/surface.glsl: Angelica core profile fragment shader.

#include "star/surface.glsl"

in vec3 v_Pos;
in vec2 v_UV;
layout(location = 0) out vec4 fragColor;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = starSurface(normalize(v_Pos - u_Eye));
    if (c.a < 0.5) discard;
    fragColor = vec4(c.rgb, 1.0);
}
