#version 120
// Machine volumes (renders v2 WP-7 / WP-8): legacy profile fragment shader; the look is in fx/vol.glsl.

#include "fx/vol.glsl"

varying vec3 v_Local;

void main() {
    vec4 c = mvShade(v_Local);
    if (c.a < 0.0) discard;
    gl_FragColor = c;
}
