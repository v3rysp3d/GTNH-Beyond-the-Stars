#version 330 core
// Point batch (renders v2 §2.1): Angelica core profile fragment shader.

#include "lib/glow.glsl"
#include "lib/tonemap.glsl"

// rgb multiplies every point's colour; a: brightness
uniform vec4 u_Tint;

in vec3 v_Color;
in float v_Fade;

layout(location = 0) out vec4 fragColor;

void main() {
    float a = btsSoftPoint(gl_PointCoord) * v_Fade * u_Tint.a;
    if (a < 0.002) discard;
    fragColor = vec4(btsShoulder(v_Color * u_Tint.rgb * a, 1.6), 1.0);
}
