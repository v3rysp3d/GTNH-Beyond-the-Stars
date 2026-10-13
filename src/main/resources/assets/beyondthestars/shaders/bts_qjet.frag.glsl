#version 120
// Quasar jet (renders v2 §6.2): legacy profile fragment shader; the look is in quasar/jet.glsl.

#include "quasar/jet.glsl"

varying float v_Z;
varying float v_U;



void main() {
    gl_FragColor = qjShade(v_Z, v_U);
}
