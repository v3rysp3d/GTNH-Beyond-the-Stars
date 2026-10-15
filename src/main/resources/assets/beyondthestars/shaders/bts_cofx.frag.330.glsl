#version 330 core
// The Eye's cosmic objects (P5 §2.1, §6.2), the effects: Angelica core profile fragment shader. The look is in
// cosmos/fx.glsl.

#include "cosmos/fx.glsl"

in vec3 v_P;
in vec4 v_Info;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 c = coFxShade(v_P, v_Info);
    if (u_FxMode > 0.5 && c.a <= 0.0) discard;
    fragColor = c;
}
