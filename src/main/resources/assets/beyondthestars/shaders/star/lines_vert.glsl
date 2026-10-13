// Beyond the Stars: the Eye's stars (renders v2 §7.2, §8.2), loop placement for bts_starlines in both profiles.
// The mesh holds up to 16 lines of ribbon quads: position (line index, s 0..1 along, side -1 / 1), UV (side, s).
// u_Lines.x picks the shape:
//   0 dipole field lines round the magnetic axis (neutron star, magnetar), twisted, opening into a fan on a giant flare;
//   1 prominences: glowing loops standing on the surface, rising and draining, turning with the star.
// local = the star frame (world axes, blocks, origin at the centre).

float slHash(float i, float k) {
    return btsHash12(vec2(i * 1.618 + k * 7.31, k * 3.7 + 11.0));
}

void slPlace(vec3 pos, vec2 uv, out vec3 local, out vec4 misc) {
    float i = pos.x;
    float s = uv.y;
    float side = pos.z;
    float mode = u_Lines.x;
    float count = u_Lines.y;
    float R = u_Lines2.z;
    float t = u_Lines2.w;
    float width = u_Lines.w;
    float life = 1.0;
    vec3 P;
    vec3 T;
    if (i >= count) {
        local = vec3(0.0);
        misc = vec4(0.0, 0.0, -1.0, 0.0);
        return;
    }
    if (mode < 0.5) {
        vec3 m = u_Mag.xyz;
        vec3 e1 = cross(m, vec3(0.0, 1.0, 0.0));
        e1 = dot(e1, e1) > 1e-5 ? normalize(e1) : vec3(1.0, 0.0, 0.0);
        vec3 e2 = cross(m, e1);
        float open = u_Lines2.y;
        float L = u_Lines.z * (0.7 + 0.6 * slHash(i, 1.0)) * (1.0 + 3.5 * open);
        float th0 = asin(sqrt(clamp(R / L, 0.0, 0.95)));
        // a giant flare tears the loops open: only their roots stay
        float span = mix(1.0, 0.42, open);
        float th = s < 0.5 ? mix(th0, 1.5708, 2.0 * s * span) : mix(3.14159 - th0, 1.5708, 2.0 * (1.0 - s) * span);
        float phi0 = 6.28318 * (i + 0.3 * slHash(i, 2.0)) / count;
        float tw = u_Lines2.x * (0.8 + 0.4 * slHash(i, 3.0));
        float st = sin(th);
        float r = L * st * st;
        float phi = phi0 + tw * cos(th);
        P = m * (r * cos(th)) + (e1 * cos(phi) + e2 * sin(phi)) * (r * st);
        float th2 = th + (s < 0.5 ? 0.02 : -0.02);
        float st2 = sin(th2);
        float r2 = L * st2 * st2;
        float phi2 = phi0 + tw * cos(th2);
        vec3 P2 = m * (r2 * cos(th2)) + (e1 * cos(phi2) + e2 * sin(phi2)) * (r2 * st2);
        T = normalize(P2 - P);
        width *= 0.6 + 0.8 * st;
    } else {
        // prominence i: a cycle of rise and drain, a fresh footpoint each cycle
        float period = 40.0 + 50.0 * slHash(i, 4.0);
        float ph = t / period + slHash(i, 5.0);
        float cyc = floor(ph);
        life = fract(ph);
        float lat = (slHash(i + cyc * 3.1, 6.0) - 0.5) * 1.4;
        float lon = slHash(i + cyc * 3.1, 7.0) * 6.28318;
        float az = slHash(i + cyc * 3.1, 8.0) * 6.28318;
        vec3 c = vec3(cos(lat) * cos(lon), sin(lat), cos(lat) * sin(lon));
        // into the world: undo the surface turn (u_Turn rotates world -> surface)
        c = vec3(u_Turn.x * c.x - u_Turn.y * c.z, c.y, u_Turn.y * c.x + u_Turn.x * c.z);
        vec3 ta = normalize(cross(c, abs(c.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
        vec3 tb = cross(c, ta);
        vec3 dir = ta * cos(az) + tb * sin(az);
        float sigma = 0.16 + 0.14 * slHash(i, 9.0);
        float rise = smoothstep(0.0, 0.35, life);
        float H = R * u_Lines.z * (0.5 + 0.5 * slHash(i + cyc, 10.0)) * (0.35 + 0.65 * rise);
        float beta = (s - 0.5) * sigma;
        vec3 base = c * cos(beta) + dir * sin(beta);
        float h = H * pow(sin(3.14159 * s), 0.8);
        P = base * (R * 0.995 + h);
        float beta2 = (s + 0.01 - 0.5) * sigma;
        vec3 base2 = c * cos(beta2) + dir * sin(beta2);
        vec3 P2 = base2 * (R * 0.995 + H * pow(sin(3.14159 * min(s + 0.01, 1.0)), 0.8));
        T = normalize(P2 - P + 1e-5);
        life = sin(3.14159 * life);
    }
    vec3 Q = u_Centre + P;
    vec3 sv = cross(T, Q);
    float sl = length(sv);
    sv = sl > 1e-4 ? sv / sl : vec3(1.0, 0.0, 0.0);
    // never thinner than about a pixel and a half; a widened line keeps its light
    float minW = u_Pixel * length(Q) * 0.75;
    float w = max(width, minW);
    local = P + sv * side * w;
    misc = vec4(side, s, i, life * width / w);
}
