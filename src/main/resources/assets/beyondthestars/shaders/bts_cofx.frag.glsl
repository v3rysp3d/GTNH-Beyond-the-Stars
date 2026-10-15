#version 120
// The Eye's cosmic objects (P5 §2.1, §6.2), the effects: legacy profile fragment shader. The look is in
// cosmos/fx.glsl.

#include "cosmos/fx.glsl"

varying vec3 v_P;
varying vec4 v_Info;

void main() {
    vec4 c = coFxShade(v_P, v_Info);
    if (u_FxMode > 0.5 && c.a <= 0.0) discard;
    gl_FragColor = c;
}
