// Beyond the Stars: machine effects' placement (renders v2 WP-6..WP-8), shared by both profiles' vertex shaders.
// Every mode works in the frame of its model matrix (a machine's voxel frame, or a Wormhole Mouth's ring frame);
// u_Cam is the camera in that frame.
//   0 field shell: the unit sphere scaled to (u_MA.x, u_MA.y, u_MA.x), centred at y u_MA.z.
//   1 glyph ring / 3 portal disc: the unit annulus at radius mix(u_MA.x, u_MA.y, uv.x), height u_MA.z.
//   2 voxels: one quad per sprite (pos = (index, corner x, corner y)), each a small cube flying from a scattered start
//     to its cell on a tray (y u_MA.x, radii u_MA.y .. u_MA.z), glowing as it locks in, then fading; a camera-facing
//     square of u_MA.w blocks.

void mxVoxel(float i, float t, out vec3 p, out float fade, out float lock) {
    float rate = u_MB.x;
    float h1 = btsHash12(vec2(i, 3.7));
    float h2 = btsHash12(vec2(i, 9.1));
    float h3 = btsHash12(vec2(i, 15.3));
    float cyc = t * rate + h1;
    float ph = fract(cyc);
    float gen = floor(cyc);
    float ang = i * 2.39996 + gen * 0.7;
    float rr = mix(u_MA.y, u_MA.z, h2);
    vec3 target = vec3(cos(ang) * rr, u_MA.x + 0.6 + floor(h3 * 3.0) * 0.7, sin(ang) * rr);
    float sa = ang + (h3 - 0.5) * 2.4;
    vec3 start = vec3(cos(sa) * (rr + 6.0 + 6.0 * h1), u_MA.x + 4.0 + 7.0 * h2, sin(sa) * (rr + 6.0 + 6.0 * h1));
    float fly = btsSmooth01(ph / 0.55);
    float e = 1.0 - (1.0 - fly) * (1.0 - fly) * (1.0 - fly);
    p = mix(start, target, e);
    float lk = (ph - 0.56) / 0.05;
    lock = exp(-lk * lk);
    fade = btsSmooth01(ph / 0.08) * (1.0 - btsSmooth01((ph - 0.82) / 0.18)) * u_MB.y;
}

void mxPlace(vec3 pos, vec2 uv, out vec3 local, out vec2 vuv, out vec3 vn, out vec2 vfx) {
    float mode = u_MMode.x;
    vuv = uv;
    vfx = vec2(1.0, 0.0);
    if (mode < 0.5) {
        local = vec3(pos.x * u_MA.x, pos.y * u_MA.y + u_MA.z, pos.z * u_MA.x);
        vn = pos;
    } else if (mode < 1.5 || mode > 2.5) {
        float r = mix(u_MA.x, u_MA.y, uv.x);
        local = vec3(pos.x * r, u_MA.z, pos.z * r);
        vn = vec3(0.0, 1.0, 0.0);
    } else {
        vec3 p;
        float fade;
        float lock;
        mxVoxel(pos.x, u_MMode.y, p, fade, lock);
        vec3 w = normalize(p - u_Cam);
        vec3 up = abs(w.y) > 0.98 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
        vec3 rt = normalize(cross(w, up));
        vec3 uv2 = cross(rt, w);
        float s = u_MA.w * (1.0 + 0.6 * lock);
        local = p + (rt * pos.y + uv2 * pos.z) * s;
        vn = w;
        vfx = vec2(fade, lock);
    }
}
