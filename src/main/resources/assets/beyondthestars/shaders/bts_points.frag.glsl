#version 120
// Point batch (renders v2 §2.1): legacy profile fragment shader.

#include "lib/glow.glsl"
#include "lib/tonemap.glsl"

// rgb multiplies every point's colour; a: brightness
uniform vec4 u_Tint;

varying vec3 v_Color;
varying float v_Fade;

void main() {
    float a = btsSoftPoint(gl_PointCoord) * v_Fade * u_Tint.a;
    if (a < 0.002) discard;
    gl_FragColor = vec4(btsShoulder(v_Color * u_Tint.rgb * a, 1.6), 1.0);
}
