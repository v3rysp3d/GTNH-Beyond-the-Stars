# The render kit (renders v2, WP-0)

Package `com.nateyburr.beyondthestars.client.render`, client only. Spec: the design repo's
`designs/docs/renders-v2.md` §2-§4. Every big render (the Kerr hole, the quasar, the Eye's centre object, and the
new renders of WP-1..WP-8) draws through it.

## Programs and GLSL

| Piece | What it does |
|---|---|
| `BTSShaders` | Registry of every program. `program(label, recipe)` registers a GT `ShaderRecipe`; `Program.get()` bakes it lazily (GT's `ShaderProfile` picks the legacy `#version 120` or the Angelica core `#version 330` variant) and returns null when it does not bake: the object then draws its fixed-function path. A dead program logs one line and stays dead until the next resource reload (`markReload`, hooked to GT's `RenderInit.onResourceReload`). `Program.mesh(builder)` owns a VBO built against the program's vertex format, rebuilt after each bake. |
| `GlslSourcePack` | Serves our sources to GT under the resource domain `beyondthestars_glsl`: reads `assets/beyondthestars/shaders/<file>`, expands `#include "lib/x.glsl"` (each file once, recursively, followed by a `#line`), and after the `#version` line defines `BTS_TEX` (`texture2D` / `texture`) and, for `.330.glsl` files, `BTS_CORE`. Added to the live resource manager on first bake. |
| `shaders/lib/*.glsl` | `common` (hashes, smoothstep), `noise` (texture value noise 2D / 3D, fbm, ALU noise, cells), `blackbody` (LUT lookup + analytic fallback), `tonemap` (the soft shoulder `1 - exp(-k c)`, luma, hot-white), `glow` (glow sprite falloff, soft point). Includes declare no uniforms; samplers are passed as arguments. |
| `NoiseTex` | The two shared textures: 256 x 256 RGBA noise (repeat, linear; G = R shifted by (37, 17) for `btsNoise3`) on unit 4 and the 256 x 1 blackbody LUT (1,000-40,000 K, log scale) on unit 5. `bind()` / `unbind(was)` restore the previous bindings. |
| `Blackbody` | Planck x CIE 1964 10-degree, display sRGB with max channel 1. CPU colours for the fixed paths and the LUT texels. |

A new program: add `shaders/<name>.vert.glsl`, `.frag.glsl` (120) and `.vert.330.glsl`, `.frag.330.glsl` (core),
then

```java
private static final ShaderRecipe RECIPE = BTSShaders.recipe("<name>")
    .required("u_Color")
    .sampler("u_Noise", NoiseTex.NOISE_UNIT)
    .modelUniform("u_ModelMatrix") // legacy: gl_ModelViewProjectionMatrix * u_ModelMatrix; core: u_MVP
    .attribute("a_Position", VertexAttribute.POSITION)
    .attribute("a_UV", VertexAttribute.UV);
static final BTSShaders.Program PROGRAM = BTSShaders.program("<name>", RECIPE);
static final BTSShaders.Mesh QUAD = PROGRAM.mesh(Meshes::quad);
```

Every uniform a recipe declares must be used in both variants (GT refuses the bake otherwise).

## Quality, LOD and the budget

| Piece | What it does |
|---|---|
| `RenderQuality.tier(key, radius, distance, programs...)` | The object's tier this frame: `FIXED` (today's fixed-function render) for `renderQuality=low`, a context without GL 2.1 + VBOs, a program that does not bake, or a GL error after the object's draw (checked once a second in a dev environment or with `-Dbts.render.glcheck=true`); else the LOD tier. `medium` never draws L0; only the `renderMaxHighObjects` largest objects on screen (last frame's ranking) may draw L0. |
| `Lod` | Projected radius `R / d x (H / 2) / tan(fov / 2)` (FOV from the projection matrix, once per frame); tiers L0 > 300 px, L1 > 80, L2 > 20, L3 below, x `renderLodScale`, 10 % hysteresis. |
| `ShaderpackProbe` | Reflective: is an Iris / Angelica shaderpack in use (cached a second). `RenderQuality.backgroundLensing(tier)` asks it. |
| Config (`client_render`) | `renderQuality` (high), `renderLodScale` (1.0), `renderMaxHighObjects` (2), `renderBackgroundLensing` (true), `renderParticles` (true). The old switches (`eyeRenderCentre`, `renderKerrHole`, `renderQuasar`, GUI switches) still win. |

## Drawing helpers

| Piece | What it does |
|---|---|
| `GlSave` | Captures and restores the GL state a layer touches (blend + function, depth test + mask, alpha test, cull + mode + front face, lighting, fog, shade model, texturing on unit 0 and the lightmap unit). No `glPushAttrib` (Angelica's core profile has none). |
| `Layers` | `untextured()`, `additive()` (fixed-function layers), `emissive()` (shader layers: lightmap 240, additive, no depth writes). Each returns the `GlSave` to restore. |
| `FixedShapes` | The LOW path's shapes, moved unchanged from the Eye: `ring`, `rimShell`, `sphere` (per-corner N.V alpha), `outside`, `billboardQuad`. |
| `Billboard` | Camera-facing axes at a camera-relative point; `model(...)` maps the unit quad onto it. |
| `Meshes` | VBO meshes: unit quad, UV sphere, unit annulus (the shader places radius `mix(r0, r1, uv.x)`), ribbon. |
| `GlowSprite` | `draw(...)`: the `bts_glow` program (Gaussian core + halo through the shoulder); `drawFixed(...)`: today's linear disc. |
| `BlackHoleLens` | The shared black-hole program `bts_blackhole` (GLSL `shaders/bh/lens.glsl`, spec §5.2): one camera-facing quad; each fragment traces its view ray backwards through the hole (Schwarzschild bending in Binet form, leapfrog steps of `k x r`, plus a weak gravitomagnetic term for the Kerr D-shape) and integrates a thin Gaussian disc slab on the way (exact column per step, so the edge-on disc and the lensed arcs come from the same march). Doppler beaming and gravitational redshift per crossing, Keplerian-sheared cylinder noise for the filaments and hot clumps (two 7.2 s cycles cross-faded, so the gas shears but never winds up), a photon ring from the rays bent by about half a turn, opening / Hawking flash rings. Premultiplied alpha with depth writes; empty fragments discard. Steps / step size / octaves per tier: L0 72 / 0.11 / 4, L1 32 / 0.20 / 3, L2 24 / 0.28 / 2, L3 14 / 0.40 / 1; at most 4 gas samples per ray, plane heights checked before the disc transform, and a ray leaving past 3 r_s clear of the disc stops early (not at L0, whose screen copy wants the final direction). Bounding sphere 1.08 x the disc's outer edge. A renderer fills a `Params` (size in blocks per r_s, disc edges, temperature, turbulence, streamers, tilt, flashes). |
| Quasar (`machines/quasar/render/QuasarFx`, WP-2) | Three programs of the quasar's own (spec �6): `bts_qring` (GLSL `shaders/quasar/ring.glsl`), the accretion ring as a **ray-marched volume** inside a slab proxy (top and bottom faces, inner and outer walls; the vertex shader places them from the radii, each front-facing fragment marches from where its ray enters to where it leaves, a quad in front of the camera covers a camera standing inside the slab). The march takes at most 16 / 12 / 8 / 6 sub-segments per tier, each at least two gas half-thicknesses long, samples the gas where the sub-segment comes nearest the plane and integrates the Gaussian height profile through it exactly (erf), so there is no step banding and no noise lookup away from the plane; noise octaves finer than the sample's footprint (its horizontal reach, or a pixel stretched by the grazing angle) fade out. `bts_qjet` (`quasar/jet.glsl`): a jet on the shared ribbon turned about its axis to the camera: core, two helical strands, knots on a hashed launch schedule, sheath, terminal bow. `bts_qfx` (`quasar/fx.glsl`): one program, three meshes chosen by a mode uniform: 12 dipole field lines (camera-facing ribbons placed in the vertex shader), 64 debris sprites (positions from a seed and the event clock, no CPU particles) and the shock shell (the shared sphere, rim-bright, noise-broken). Draw order: ring, the far jet and the far halves of the field lines and shell, then the hole (`BlackHoleLens`, depth writes, its L0 screen copy holds the ring and the far jet), then the near halves, glows and debris. |
| `FrameCopy` | The screen copy a black hole bends at L0 (§4.3): one display-sized texture on unit 6, `glCopyTexSubImage2D` of the quad's screen rectangle at the same offsets, so the shader samples it with screen coordinates. Only when `RenderQuality.backgroundLensing` (L0, `renderBackgroundLensing`, no shaderpack). |
| `PointBatch` | A static `GL_POINTS` VBO (position, size in blocks, packed colour) drawn by `bts_points` with distance-scaled `gl_PointSize` and soft round sprites; `drawFixed` draws a subset as billboard quads. |
| `RenderClock` | `u_Time` seconds, `(totalWorldTime % 72,000 + partialTicks) / 20`. |
| `RenderFrame` | Frame counter and the per-frame hooks (LOD ranking, GPU timer). |

## Dev switches

| Switch | Effect |
|---|---|
| `-Dbts.rendertest.quality=low\|medium\|high` | Overrides `renderQuality` (the harness shoots all three). |
| `-Dbts.rendertest.gputime=true` | `GpuTimer`: `GL_TIME_ELAPSED` queries round each renderer, read back without stalling; the harness writes `gpu_ms`, `cpu_ms` and `gpu_detail` (per renderer, ms per frame over the view's still window) to `fps.tsv` and logs each object's tier. |
| `-Dbts.render.failShaders=true` | Every registered program reports dead: shoot the fallback paths. |
| `-Dbts.render.selftest=true` | `KitSelfTest`: compiles and links both profiles of every kit program from the served sources, logs each bake, writes `kit-noise.png` / `kit-lut.png`, and draws a test panel in front of the camera (the self-test program as baked and as a hand-linked core variant, glow and points by shader and by fixed function, the two textures). |
| `-Dbts.render.bhRepeat=n` | `BlackHoleLens` draws each hole n times inside its timer section: `(t(n) - t(1)) / (n - 1)` is a draw's marginal (throughput) cost, without the latency a timer query serialises round one small draw (which dominates the single-draw number for anything under about 100 px). The picture is wrong in such a run. |
| `-Dbts.render.qRepeat=n` | The quasar's whole v2 draw runs n times inside its timer section: the marginal cost per quasar, as `bhRepeat` for the Kerr. |
| `-Dbts.render.glcheck=true` | The GL error check outside a dev environment. |
