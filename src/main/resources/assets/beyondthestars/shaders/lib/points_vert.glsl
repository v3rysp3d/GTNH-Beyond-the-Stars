// Point batch vertex body (renders v2 §9.2), shared by both profiles: expects gl_Position set, a_UV, u_Points, and
// writes gl_PointSize, v_Color, v_Fade. Perspective: gl_Position.w is the distance along the view axis.
    float px = a_UV.x * u_Points.x / max(gl_Position.w, 0.05);
    float size = clamp(px, u_Points.y, u_Points.z);
    // a point drawn larger than it is keeps its light: fade by the area it gained
    v_Fade = px < size ? (px * px) / (size * size) : 1.0;
    gl_PointSize = size;
    float packedRgb = a_UV.y;
    float r = floor(packedRgb / 65536.0);
    float g = floor((packedRgb - r * 65536.0) / 256.0);
    float b = packedRgb - r * 65536.0 - g * 256.0;
    v_Color = vec3(r, g, b) / 255.0;
