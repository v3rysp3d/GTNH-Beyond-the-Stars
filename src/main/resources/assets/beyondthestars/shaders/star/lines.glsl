// Beyond the Stars: the Eye's stars (renders v2 §7.2, §8.2), the fragment body of bts_starlines in both profiles:
// field lines (mode 0) and prominences (mode 1). Additive (ONE, ONE); the photosphere's depth hides their far side.
#include "lib/noise.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
// x mode, y count, z size (field: blocks; prominence: height in radii), w width (blocks)
uniform vec4 u_Lines;
// x twist, y open (giant flare) 0..1, z star radius, w time s
uniform vec4 u_Lines2;
// rgb colour, w intensity
uniform vec4 u_LineCol;
// x crackle 0..1, y bead phase (radians; the bead speed integrated on the CPU, B-251)
uniform vec4 u_LineFx;

vec3 slShade(vec4 misc) {
    float across = misc.x;
    float s = misc.y;
    float i = misc.z;
    float k = misc.w;
    if (i < 0.0 || k <= 0.0) return vec3(-1.0);
    float t = u_Lines2.w;
    float prof = exp(-across * across * 3.2);
    float ends = sqrt(max(sin(3.14159 * s), 0.0));
    vec3 c = u_LineCol.rgb;
    float a;
    if (u_Lines.x < 0.5) {
        float beads = 0.6 + 0.4 * sin(s * 40.0 - u_LineFx.y + i * 1.7);
        float crackle = u_LineFx.x;
        float flick = 1.0;
        float spark = 0.0;
        if (crackle > 0.0) {
            // B-251: each line's light wavers smoothly, 0.7 to 1.3 at about 4 Hz (was a hard 20 Hz switch between
            // 0.4 and 1.7); the sparks carry the crackle
            float h = btsNoise2(u_Noise, vec2(t * 4.0, i * 7.0));
            flick = mix(1.0, 0.7 + 0.6 * h, crackle);
            float n = btsNoise2(u_Noise, vec2(s * 36.0 + i * 13.0, t * 8.0 + i));
            spark = crackle * pow(n, 6.0) * 7.0;
        }
        // a giant flare tears the loops at their apex: only the roots stay
        float tear = u_Lines2.y > 0.001 ? smoothstep(0.0, 0.08, abs(s - 0.5) - 0.45 * u_Lines2.y) : 1.0;
        a = prof * ends * (beads * flick + spark) * k * tear;
        c = mix(c, vec3(0.95, 0.92, 1.0), clamp(spark, 0.0, 1.0));
    } else {
        // a prominence: hot plasma blobs threading the loop and draining down its legs
        float n = btsNoise2(u_Noise, vec2(s * 18.0 + i * 31.0, t * 0.6 + i * 5.0));
        float drain = btsNoise2(u_Noise, vec2(abs(s - 0.5) * 30.0 - t * 1.5, i * 17.0));
        float blobs = 0.45 + 0.8 * n * (0.6 + 0.6 * drain);
        float core = exp(-across * across * 10.0);
        a = (prof * 0.7 + core * 0.6) * blobs * pow(max(sin(3.14159 * s), 0.0), 0.3) * k;
        c = mix(c, vec3(1.0, 0.85, 0.7), core * 0.35);
    }
    return btsShoulder(c * a * u_LineCol.w, 1.6);
}
