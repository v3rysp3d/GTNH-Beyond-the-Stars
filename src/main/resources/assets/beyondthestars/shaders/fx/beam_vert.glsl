// Beyond the Stars: a beam's ribbon placement (renders v2 WP-6..WP-8), shared by both profiles' vertex shaders.
// The mesh is the unit ribbon (x -1..1 across, y 0..1 along). The ribbon follows the quadratic Bezier curve
// u_P0 -> u_P1 -> u_P2 (camera-relative: the camera is the origin) and turns at each point to face the camera. Its
// half-width runs from u_BW.x to u_BW.y, never under u_BW.z (a pixel's width per block of distance) x 1.5 x the
// distance, with room for the halo.

vec3 bmCurve(float s) {
    float r = 1.0 - s;
    return r * r * u_P0 + 2.0 * r * s * u_P1 + s * s * u_P2;
}

vec3 bmTangent(float s) {
    vec3 d = 2.0 * (1.0 - s) * (u_P1 - u_P0) + 2.0 * s * (u_P2 - u_P1);
    float l = length(d);
    return l > 1e-6 ? d / l : vec3(0.0, 1.0, 0.0);
}

void bmPlace(vec3 pos, vec2 uv, out vec3 world, out float s, out float u, out float hw) {
    s = uv.y;
    vec3 p = bmCurve(s);
    vec3 tg = bmTangent(s);
    vec3 side = cross(tg, p);
    float sl = length(side);
    side = sl > 1e-5 ? side / sl : vec3(1.0, 0.0, 0.0);
    hw = max(mix(u_BW.x, u_BW.y, s), u_BW.z * 1.5 * length(p));
    u = pos.x * hw * 2.15;
    world = p + side * u;
}
