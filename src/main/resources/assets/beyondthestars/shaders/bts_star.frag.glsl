#version 120
// The Eye's star photosphere (renders v2 §7.2); the look is in star/surface.glsl: legacy profile fragment shader.

#include "star/surface.glsl"

varying vec3 v_Pos;
varying vec2 v_UV;

void main() {
    if (v_UV.x < -1.0) discard;
    vec4 c = starSurface(normalize(v_Pos - u_Eye));
    if (c.a < 0.5) discard;
    gl_FragColor = vec4(c.rgb, 1.0);
}
