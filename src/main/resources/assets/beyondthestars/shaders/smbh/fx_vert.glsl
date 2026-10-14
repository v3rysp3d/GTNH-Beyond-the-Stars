// Beyond the Stars: the Supermassive Lattice's effects (smbh-v1.md §7, R-19), vertex placement shared by both
// profiles. One program, three meshes; u_S.x picks the mode:
//   0 the lattice mesh, the kind in position.x:
//       0 a Containment Node's glow sprite (48: position (0, node, 0), UV the corner 0..1),
//       1 a field arc from a node down to the ring's outer edge (48: position (1, node, side -1/1), UV (side, s)),
//       2 an anchor thread from a node to its neighbour on the same ring (48, Unbound only; as the arcs);
//   1 the intake streams: 12 spirals of gas across the ring, outer wall to inner wall (position (3, stream, side));
//   2 the Unbound halo: the shared unit quad as a billboard round the hole.
// The nodes sit where the exported shape puts them (16 per ring at 11.25 + 22.5k degrees; the equator at r 88.9, the
// two latitude rings at r 72.9, y +-50; rounded to the block grid as the exporter rounds them). That set is the same
// under every quarter turn and mirror, so the controller's facing never moves it.
// local = the quasar frame (world axes, blocks, the hole centre at the origin).

float smHash(float i, float k) {
    return btsHash12(vec2(i * 1.618 + k * 7.31, k * 3.7 + 11.0));
}

// node i (0..47): ring 0 the equator, 1 the upper ring, 2 the lower one
vec3 smNode(float i, out float th, out float ring) {
    ring = floor(i / 16.0 + 0.01);
    float k = i - 16.0 * ring;
    th = radians(11.25 + 22.5 * k);
    float r = ring < 0.5 ? 88.9 : 72.9;
    float y = ring < 0.5 ? 0.0 : (ring < 1.5 ? 50.0 : -50.0);
    return vec3(floor(r * cos(th) + 0.5), y, floor(r * sin(th) + 0.5));
}

// A camera-facing ribbon round the curve point P with tangent T: half-width w, at least a pixel and a half wide.
vec3 smRibbon(vec3 P, vec3 T, float side, float w) {
    vec3 Q = u_Centre + P;
    vec3 sv = cross(normalize(T), Q);
    float sl = length(sv);
    sv = sl > 1e-4 ? sv / sl : vec3(0.0, 1.0, 0.0);
    float ww = max(w, 1.5 * length(Q) * u_SB.w);
    return P + sv * side * ww;
}

void smPlace(vec3 pos, vec2 uv, out vec3 local, out vec4 misc, out float kind) {
    float mode = u_S.x;
    float t = u_S.y;
    if (mode > 1.5) {
        // the halo
        kind = 4.0;
        local = (pos.x * u_Right + pos.y * u_Up) * u_SA.x;
        misc = vec4(pos.xy, 0.0, 0.0);
        return;
    }
    kind = pos.x;
    float i = pos.y;
    if (mode > 0.5) {
        // an intake stream: a trailing spiral inward (the gas turns as the ring does: its angle falls with time)
        float s = uv.y;
        float side = pos.z;
        float th0 = BTS_TAU * (i + 0.6 * smHash(i, 1.0)) / 12.0;
        float turn = u_SA.w * (0.8 + 0.4 * smHash(i, 2.0));
        float th = th0 - turn * s;
        float r = mix(u_SA.x, u_SA.y, pow(s, 0.85));
        // over and under the ring's gas, alternately, settling into it at the inner wall
        float y = u_SB.y * (mod(i, 2.0) < 0.5 ? 1.0 : -1.0) * (0.7 + 0.3 * smHash(i, 3.0)) * (1.0 - s * s);
        vec3 P = vec3(r * cos(th), y, r * sin(th));
        float dr = u_SA.y - u_SA.x;
        vec3 T = vec3(dr * cos(th) + r * turn * sin(th), 0.0, dr * sin(th) - r * turn * cos(th));
        float w = u_SA.z * (0.55 + 0.45 * sin(BTS_PI * s)) * (0.7 + 0.6 * smHash(i, 4.0));
        local = smRibbon(P, T, side, w);
        misc = vec4(side, s, i, 0.0);
        return;
    }
    float th;
    float ring;
    vec3 N = smNode(i, th, ring);
    vec3 dir = vec3(cos(th), 0.0, sin(th));
    if (kind < 0.5) {
        // a node's glow sprite (it flares in Overdrive)
        vec2 c = uv * 2.0 - 1.0;
        float size = u_SA.w * (1.0 + 0.9 * u_SB.x);
        local = N + (c.x * u_Right + c.y * u_Up) * size;
        misc = vec4(c, i, 0.0);
        return;
    }
    float s = uv.y;
    float side = pos.z;
    if (kind < 1.5) {
        // a field arc: over (or under) the outer wall into the ring's outer edge, a quadratic curve
        float k = i - 16.0 * ring;
        vec3 P2 = dir * (u_SA.x - 1.5);
        vec3 P1;
        if (ring < 0.5) {
            float up = mod(k, 2.0) < 0.5 ? 1.0 : -1.0;
            P1 = dir * (0.5 * (88.9 + u_SA.x) + 2.0) + vec3(0.0, up * 22.0, 0.0);
        } else {
            P1 = dir * (0.5 * (72.9 + u_SA.x) + 9.0) + vec3(0.0, N.y * 0.62, 0.0);
        }
        float a = 1.0 - s;
        vec3 P = a * a * N + 2.0 * a * s * P1 + s * s * P2;
        vec3 T = 2.0 * a * (P1 - N) + 2.0 * s * (P2 - P1);
        // a slow sway across the curve
        vec3 across = normalize(cross(T, vec3(0.0, 1.0, 0.0)) + vec3(0.0, 1e-3, 0.0));
        P += across * 0.7 * sin(BTS_PI * s) * sin(t * 1.3 + i * 2.1);
        local = smRibbon(P, T, side, u_SA.y * (0.55 + 0.45 * sin(BTS_PI * s)));
        misc = vec4(side, s, i, 1.0);
        return;
    }
    // an anchor thread (Unbound): along the ring just outside the frame to the next node, sagging between them
    if (u_SA.z <= 0.0) {
        local = N;
        misc = vec4(0.0, 0.0, i, 2.0);
        return;
    }
    float rr = (ring < 0.5 ? 88.9 : 72.9) + 2.2 + 1.8 * sin(BTS_PI * s);
    float a1 = th + radians(22.5) * s;
    float y = N.y - (ring < 0.5 ? 0.0 : sign(N.y)) * 1.6 * sin(BTS_PI * s) + 0.9 * sin(BTS_TAU * s) * sin(t * 0.9 + i);
    vec3 P = vec3(rr * cos(a1), y, rr * sin(a1));
    vec3 T = vec3(-sin(a1), 0.0, cos(a1));
    local = smRibbon(P, T, side, u_SA.z);
    misc = vec4(side, s, i, 2.0);
}
