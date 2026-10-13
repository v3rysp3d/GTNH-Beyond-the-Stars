// Beyond the Stars: the quasar ring's proxy placement (renders v2 §6.2), shared by both profiles' vertex shaders.
// The mesh holds the unit proxy: position (cos, kind, sin) of the angle, UV (t across, angle 0..1); kind 0 top face,
// 1 bottom face, 2 inner wall, 3 outer wall. Mode 1 places the shared unit quad at u_NearC + x u_NearU + y u_NearV
// (camera-relative), the "camera inside the slab" quad.

// Local position (quasar frame, blocks) of this vertex; kind out.
vec3 qrPlace(vec3 pos, vec2 uv, out float kind) {
    if (u_Ray.z > 0.5) {
        kind = 4.0;
        return u_NearC + pos.x * u_NearU + pos.y * u_NearV - u_Centre;
    }
    kind = pos.y;
    float r0 = u_Ring.x;
    float r1 = u_Ring.y;
    float H = u_Ring.z;
    vec2 cs = pos.xz;
    if (kind < 0.5) return vec3(cs * mix(r0, r1, uv.x), H).xzy;
    if (kind < 1.5) return vec3(cs * mix(r0, r1, uv.x), -H).xzy;
    if (kind < 2.5) return vec3(cs * r0, mix(-H, H, uv.x)).xzy;
    return vec3(cs * r1, mix(-H, H, uv.x)).xzy;
}
