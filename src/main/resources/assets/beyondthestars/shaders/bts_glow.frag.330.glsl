#version 330 core
// Glow sprite (renders v2 §2.1): Angelica core profile fragment shader.

#include "lib/glow.glsl"
#include "lib/tonemap.glsl"

// rgb: the glow's colour (may exceed 1: the shoulder keeps it coloured); a: intensity
uniform vec4 u_Color;
// x: core width, y: halo width (sprite radii)
uniform vec2 u_Shape;

in vec2 v_UV;

layout(location = 0) out vec4 fragColor;

void main() {
    float r = length(v_UV * 2.0 - 1.0);
    float g = btsGlow(r, u_Shape.x, u_Shape.y) * u_Color.a;
    // premultiplied: drawn with (ONE, ONE) or (SRC_ALPHA, ONE)
    fragColor = vec4(btsShoulder(u_Color.rgb * g, 1.6), 1.0);
}
