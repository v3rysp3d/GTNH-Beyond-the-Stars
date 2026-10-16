// Beyond the Stars: the quasar's effects (renders v2 §6.2), the fragment body of bts_qfx in both profiles:
// field lines (mode 0), debris (mode 1) and the eruption's shock shell (mode 2). Additive (ONE, ONE).

#include "lib/noise.glsl"
#include "lib/blackbody.glsl"
#include "lib/tonemap.glsl"

uniform sampler2D u_Noise;
// x: mode, y: time s, z: half (+1 only in front of the centre plane, -1 only behind, 0 all), w: intensity
uniform vec4 u_Fx;
// field: crackle 0..1, snap 0..1, spare, spare; debris: kind (1 eruption, 3 blow-out); shell: spare
uniform vec4 u_FxB;
// the effect's colour (rgb), w: shell rim power
uniform vec4 u_FxC;

vec3 qfShade(vec3 pos, vec4 misc) {
    float mode = u_Fx.x;
    float t = u_Fx.y;
    float I = u_Fx.w;
    if (mode < 0.5) {
        // a field line
        if (u_Fx.z * misc.w > 0.0) return vec3(-1.0);
        float across = misc.x;
        float s = misc.y;
        float i = misc.z;
        float prof = exp(-across * across * 3.5);
        float ends = sqrt(max(sin(3.14159 * s), 0.0));
        float beads = 0.55 + 0.45 * sin(s * 46.0 - t * 4.0 + i * 1.7);
        float crackle = u_FxB.x;
        float flick = 1.0;
        float spark = 0.0;
        if (crackle > 0.0) {
            // B-251: each line wavers smoothly, 0.7 to 1.3 at about 4 Hz (was a hard 24 Hz switch between 0.35 and
            // 1.75); the sparks carry the crackle
            float h = btsValueNoise2(vec2(t * 4.0, i * 7.0));
            flick = mix(1.0, 0.7 + 0.6 * h, crackle);
            float n = btsNoise2(u_Noise, vec2(s * 40.0 + i * 13.0, t * 9.0 + i));
            spark = crackle * pow(n, 7.0) * 9.0;
        }
        // the eruption snap: the lines tear at the equator and fade
        float snap = u_FxB.y;
        float tear = smoothstep(0.0, 0.08, abs(s - 0.5) - 0.45 * snap);
        float a = prof * ends * (beads * flick + spark) * tear * (1.0 - snap) * I;
        vec3 c = mix(u_FxC.rgb, vec3(0.92, 0.95, 1.0), clamp(spark, 0.0, 1.0));
        return btsShoulder(c * a, 1.6);
    } else if (mode < 1.5) {
        // a debris sprite
        float life = misc.z;
        if (life >= 1.0) return vec3(-1.0);
        float r = length(misc.xy);
        float blob = exp(-3.2 * r * r) * (1.0 - smoothstep(0.7, 1.0, r));
        float fade = 1.0 - smoothstep(0.55, 1.0, life);
        float kel = mix(9000.0, 1500.0, sqrt(life)) * (0.8 + 0.4 * misc.w);
        vec3 c = btsBlackbodyApprox(kel) * mix(1.0, 0.65, step(2.5, u_FxB.x));
        return btsShoulder(c * blob * fade * I * 1.6, 1.6);
    }
    // the shock shell: rim-bright, noise-broken
    vec3 n = misc.xyz;
    vec3 V = normalize(pos);
    float ndv = abs(dot(n, V));
    float rim = pow(max(1.0 - ndv, 0.0), u_FxC.w);
    float nz = btsNoise3(u_Noise, n * 5.0 + vec3(0.0, 0.0, t * 0.8)) * 0.65
        + btsNoise3(u_Noise, n * 13.0 + vec3(5.0, 3.0, t * 1.6)) * 0.35;
    // a thin, crisp front: bright only near its silhouette, broken by the noise, almost clear face-on (so it reads
    // as a passing wave from inside it too)
    float mask = smoothstep(0.38, 0.68, nz * 0.85 + rim * 0.25);
    float a = (rim * (0.3 + 2.2 * mask) + 0.012 * mask) * I;
    return btsShoulder(u_FxC.rgb * a, 1.6);
}
