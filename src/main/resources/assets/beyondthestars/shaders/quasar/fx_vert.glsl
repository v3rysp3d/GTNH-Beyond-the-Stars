// Beyond the Stars: the quasar's effects (renders v2 §6.2), vertex placement shared by both profiles.
// One program, three meshes, u_Fx.x picks the mode:
//   0 field lines: 12 dipole arcs round the hole's poles, position (line index, s 0..1 along, side -1/1);
//   1 debris: 64 billboard sprites thrown off the ring, position (index, corner x, corner y);
//   2 shock shell: the unit sphere, position = its normal.
// local = the quasar frame (world axes, blocks); misc carries per-mode data to the fragment shader.

float qfHash(float i, float k) {
    return btsHash12(vec2(i * 1.618 + k * 7.31, k * 3.7 + 11.0));
}

// a point of field line i at polar angle th (dipole: r = L sin^2 th), twisted round the axis
vec3 qfCurve(float th, float phi0, float L, float twist) {
    float st = sin(th);
    float r = L * st * st;
    float phi = phi0 + twist * cos(th);
    return vec3(r * st * cos(phi), r * cos(th), r * st * sin(phi));
}

void qfPlace(vec3 pos, vec2 uv, out vec3 local, out vec4 misc) {
    float mode = u_Fx.x;
    float t = u_Fx.y;
    if (mode < 0.5) {
        float i = pos.x;
        float s = uv.y;
        float side = pos.z;
        float L = u_FxA.x * (0.75 + 0.5 * qfHash(i, 1.0)) * (1.0 + 2.2 * u_FxB.y);
        float th0 = asin(sqrt(clamp(u_FxA.y / L, 0.0, 0.9)));
        float th = mix(th0, 3.14159 - th0, s);
        float phi0 = 6.28318 * (i + 0.35 * qfHash(i, 2.0)) / 12.0 + 0.06 * t;
        float tw = u_FxA.z * (0.8 + 0.4 * qfHash(i, 3.0));
        vec3 P = qfCurve(th, phi0, L, tw);
        vec3 P2 = qfCurve(th + 0.02, phi0, L, tw);
        vec3 T = normalize(P2 - P);
        vec3 Q = u_Centre + P;
        vec3 sv = cross(T, Q);
        float sl = length(sv);
        sv = sl > 1e-4 ? sv / sl : vec3(1.0, 0.0, 0.0);
        float w = u_FxA.w * (0.5 + 0.8 * sin(th));
        local = P + sv * side * w;
        misc = vec4(side, s, i, dot(P, normalize(u_Centre)));
    } else if (mode < 1.5) {
        float i = pos.x;
        vec2 c = pos.yz;
        float age = u_FxA.x;
        bool blow = u_FxB.x > 2.5;
        float h1 = qfHash(i, 1.0);
        float h2 = qfHash(i, 2.0);
        float h3 = qfHash(i, 3.0);
        float h4 = qfHash(i, 4.0);
        float life = blow ? 3.0 + 3.0 * h3 : 1.6 + 2.2 * h3;
        float phi = 6.28318 * h1;
        float rs = mix(u_FxA.z, u_FxA.w, blow ? h2 : h2 * 0.35);
        float v = blow ? 3.0 + 9.0 * h4 : 8.0 + 20.0 * h4;
        float vy = (h2 - 0.5) * (blow ? 4.0 : 12.0) + (h3 - 0.5) * 3.0;
        vec3 P = vec3(cos(phi), 0.0, sin(phi)) * (rs + v * age) + vec3(0.0, vy * age, 0.0);
        float size = u_FxA.y * (0.5 + 1.2 * h3) * (1.0 + 0.25 * age);
        local = P + (c.x * u_Right + c.y * u_Up) * size;
        misc = vec4(uv * 2.0 - 1.0, age / life, h4);
    } else {
        local = pos * u_FxA.x;
        misc = vec4(pos, uv.x);
    }
}
