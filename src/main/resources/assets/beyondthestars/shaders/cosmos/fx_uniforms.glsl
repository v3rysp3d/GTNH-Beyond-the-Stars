// Beyond the Stars: the uniforms of bts_cofx (P5), shared by its vertex and fragment stages.

uniform float u_FxMode;
// 0: x radius, y rim strength; 1: crown (y, a, b, tilt rad); 2: x a, y b (blocks), z scale, w time; 3: x half-width,
// y intensity, z time, w spool
uniform vec4 u_FxA;
// 0: rgb rim colour; 1: x inner offset, y length, z half-width (blocks), w time; 2: x alpha, y reveal (0 plasma, 1
// the CMB), z the recombination flash, w noise octaves; 3: rgb colour, w the crown's glow
uniform vec4 u_FxB;
// 1: x seconds since the Cosmic Dawn (very negative: none), y its warning 0..1, z idle glow, w the burst's strength;
// 2: x fate (0, 1 Big Rip, 2 Big Crunch), y its progress 0..1, z the jet's hole (unit radius), w the plasma's glow;
// 3: the collars (first glass channel's y, the step, the first radius, its change per collar), or the crown (y, a,
// b, tilt rad) for ring 5
uniform vec4 u_FxC;
// x blocks per pixel at one block (ribbons stay a pixel and a half wide), y collar rings drawn (count), z, w unused
uniform vec4 u_FxD;
// the camera in the model frame
uniform vec3 u_Cam;
