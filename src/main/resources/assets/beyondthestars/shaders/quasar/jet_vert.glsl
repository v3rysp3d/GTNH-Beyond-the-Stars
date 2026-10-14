// Beyond the Stars: the quasar jet's ribbon placement (renders v2 §6.2), shared by both profiles' vertex shaders.
// The mesh is the unit ribbon (x -1..1 across, y 0..1 along). The ribbon runs along the axis from u_JSpan.x to
// u_JSpan.y blocks from the hole, u_JAxis.xyz the axis direction (unit), and is turned about the axis to face the
// camera: u_Side is the unit vector across it. Its half-width follows the jet's sheath, with room for the halo.

void qjPlace(vec3 pos, vec2 uv, out vec3 local, out float z, out float u) {
    z = mix(u_JSpan.x, u_JSpan.y, uv.y);
    float zz = z - u_JSpan.x;
    float W = 1.9 * (0.75 + 0.0524 * zz) + 0.3;
    u = pos.x * W;
    local = u_JAxis.xyz * z + u_Side * u;
}
