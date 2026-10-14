#version 120
// Render kit self test (renders v2 WP-0): legacy profile fragment shader.

#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"
#include "lib/glow.glsl"

uniform sampler2D u_Noise;
uniform sampler2D u_Blackbody;
// seconds (RenderClock)
uniform float u_Time;

varying vec2 v_UV;

// Six bands, top to bottom: texture fbm lit by the blackbody LUT (1,500 -> 30,000 K left to right); the LUT itself;
// the analytic blackbody; animated 3D texture noise x cells; ALU fbm; the tonemap shoulder (input 0..4) over a hard
// clip (top half of the band) - every shared include draws something.
void main() {
    vec2 uv = v_UV;
    float band = floor((1.0 - uv.y) * 6.0);
    float k = mix(1500.0, 30000.0, uv.x);
    vec3 col;
    if (band < 0.5) {
        float n = btsFbm2(u_Noise, uv * vec2(64.0, 24.0) + vec2(u_Time * 3.0, 0.0), 5);
        col = btsBlackbody(u_Blackbody, k) * (0.15 + 1.1 * n);
    } else if (band < 1.5) {
        col = btsBlackbody(u_Blackbody, k);
    } else if (band < 2.5) {
        col = btsBlackbodyApprox(k);
    } else if (band < 3.5) {
        float n = btsNoise3(u_Noise, vec3(uv * vec2(40.0, 15.0), u_Time * 0.5));
        float c = btsCells2(uv * vec2(30.0, 11.0));
        col = vec3(n) * (0.4 + 0.6 * btsSmooth01(c));
    } else if (band < 4.5) {
        col = vec3(btsValueFbm2(uv * vec2(50.0, 18.0), 5));
    } else {
        float x = uv.x * 4.0;
        float half_ = fract((1.0 - uv.y) * 6.0);
        col = half_ < 0.5 ? clamp(vec3(1.0, 0.55, 0.2) * x, 0.0, 1.0) : btsShoulder(vec3(1.0, 0.55, 0.2) * x, 1.6);
    }
    gl_FragColor = vec4(col, 1.0);
}
