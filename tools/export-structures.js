#!/usr/bin/env node
// Singularity Ladder structure exporter (spec: docs/singularity-ladder-build.md §5.2 and "Structure export").
//
// Runs the approved voxel mockup (designs/voxel.js in the design repo) headlessly and writes one trimmed
// StructureLib shape file per multiblock piece, a legend (shape char -> block role -> intended block) and a
// manifest (bill of materials, anchors, sockets) under src/main/resources/assets/beyondthestars/structures/.
//
//   node tools/export-structures.js [path/to/voxel.js] [--check] [--out <dir>] [--flipH] [--flipD] [--quiet]
//
// The voxel path defaults to ../designs/voxel.js (relative to the addon root). --check regenerates in memory and
// compares with the committed files (exit 1 on drift). --flipH / --flipD mirror the horizontal / depth axis if the
// in-world orientation test shows the loader assumption is wrong. No npm dependencies.
//
// voxel.js is NEVER edited: the few hooks the exporter needs (piece tags, the bolt-bus block kind) are spliced into
// the source string in memory at asserted anchors, and the run fails loudly if an anchor moves.
"use strict";
const fs = require("fs");
const path = require("path");
const vm = require("vm");
const crypto = require("crypto");

const ADDON = path.resolve(__dirname, "..");
const args = process.argv.slice(2);
const flag = (f) => args.includes(f);
const optVal = (f) => { const i = args.indexOf(f); return i >= 0 ? args[i + 1] : null; };
const positional = args.filter((a, i) => !a.startsWith("--") && args[i - 1] !== "--out");
// A git worktree of the addon sits beside the design repo, not inside it: fall back to ../GTNH-Addons/designs.
const VOXEL = path.resolve(positional[0] || [path.join(ADDON, "..", "designs", "voxel.js"), path.join(ADDON, "..", "GTNH-Addons", "designs", "voxel.js")]
  .find((p) => fs.existsSync(p)) || path.join(ADDON, "..", "designs", "voxel.js"));
const OUT = path.resolve(optVal("--out") || path.join(ADDON, "src/main/resources/assets/beyondthestars/structures"));
const FLIP_H = flag("--flipH"), FLIP_D = flag("--flipD"), CHECK = flag("--check"), QUIET = flag("--quiet");
const log = (...a) => { if (!QUIET) console.log(...a); };
function fail(msg) { throw new Error("export-structures: " + msg); }

// ------------------------------------------------------------------------------------------------ headless load
// LF-normalised, so the sha and the anchors do not depend on git's autocrlf setting
const SRC = fs.readFileSync(VOXEL, "utf8").replace(/\r\n/g, "\n");
const SHA = crypto.createHash("sha1").update(SRC).digest("hex");

function splice(src, anchor, insert, where) { // where: "before" | "after"
  const i = src.indexOf(anchor);
  if (i < 0) fail(`anchor not found in voxel.js: ${JSON.stringify(anchor)}`);
  if (src.indexOf(anchor, i + 1) >= 0) fail(`anchor is not unique in voxel.js: ${JSON.stringify(anchor)}`);
  const at = where === "after" ? i + anchor.length : i;
  return src.slice(0, at) + insert + src.slice(at);
}

function loadVoxel() {
  const banner = "// ------------------------------------------------------------ GL";
  const cut = SRC.indexOf(banner);
  if (cut < 0) fail("GL banner not found");
  let body = SRC.slice(0, cut);
  // 1. buildKerr references B.boltbus but the mockup never def()s it, so those 8 cells come out empty. Define it
  //    (appended last, so every other block id is unchanged).
  if (/def\("boltbus"/.test(body)) fail("voxel.js now defines boltbus itself; drop the exporter's patch");
  body = splice(body, "BLOCKS.forEach((b, i) => { tile = i; b[3](); });",
    `def("boltbus", "Bolt Bus", "h", ()=>{});\n`, "before");
  // 2. Kerr piece tags (cells take the tag of their LAST writer, so pieces are disjoint by construction).
  body = splice(body, "function buildKerr(tier){", `tag("kerr_core");`, "after");
  body = splice(body, "arc(36, 245, 295", `tag("kerr_compressor");`, "before");
  body = splice(body, "arc(35, 70, 110", `tag("kerr_core");`, "before");
  body = splice(body, "arc(36, 328, 352", `tag("kerr_extractor");`, "before");
  body = splice(body, "if (tier>=2){", ` tag("kerr_density");`, "after");
  // 3. The Eye's six 7x7 pylon foot pads (deck, written inside the "ground" tag): only each pad's centre ring (the 8
  //    cells around the foot, 48 in all, as many as TecTech's Eye uses) gets the "pad" tag and becomes the
  //    stabilisation generators; the rest stays ground/plinth (cost audit #5, B-24: was all 294 cells).
  body = splice(body, "set(foot[0]+dx, GROUND+1, foot[2]+dz, B.deck);",
    `{ tag(Math.max(Math.abs(dx), Math.abs(dz)) === 1 ? "pad" : "ground"); `, "before");
  body = splice(body, "set(foot[0]+dx, GROUND+1, foot[2]+dz, B.deck);", ` } tag("ground");`, "after");
  // 4. A switch to build without clamps, so the exporter can tell a clamp strut (air without clamps) from a clamp
  //    root that overwrote a band cell.
  body = splice(body, "function clamp(p, q, mats, w=1.2){", ` if (globalThis.__noClamps) return;`, "after");
  // 5. The Eye's centre is render-only (D-34), but the T4 render draws one "planet" with the solid ring2 block. The
  //    exporter never draws the centre at all.
  body = splice(body, "centreRender(t, mulberry32(t*7+1));", "if (!globalThis.__noCentre) ", "before");
  body += `
globalThis.__vox = { SCENES, BLOCKS, B, RENDER_ONLY, TAGS,
  build(key, t, opts){ const s = SCENES.find(x => x.key === key); if (!s) throw new Error("no scene " + key);
    globalThis.__noClamps = !!(opts && opts.noClamps); globalThis.__noCentre = true;
    tagArr = new Uint8Array(270*195*260); curTag = 0; collide = {};
    s.build(t, s.style);
    if (W*H*D > tagArr.length) throw new Error("tag array too small");
    return { W, H, D, CX, CY, CZ, world: world.slice(), tags: tagArr.slice(0, W*H*D), collide: { ...collide } }; } };
})();`;
  const ctx2d = new Proxy({}, {
    get: (t, k) => k === "getImageData" ? () => ({ data: [0, 0, 0, 0] }) : (k === "canvas" ? {} : () => {}),
    set: () => true,
  });
  const canvas = { width: 0, height: 0, getContext: () => ctx2d, toDataURL: () => "" };
  const sandbox = { document: { createElement: () => canvas, body: { appendChild() {} } }, window: {}, $: () => null, console, Math };
  vm.createContext(sandbox);
  vm.runInContext(body, sandbox, { filename: "voxel.js(headless)" });
  return sandbox.__vox;
}
const V = loadVoxel();
const KIND = V.BLOCKS.map((b) => b[0]);
const isRender = (id) => !!V.RENDER_ONLY[id];

// ------------------------------------------------------------------------------------------------ legend
// Shape char -> role -> intended block. Per the mid-run ruling, existing blocks play every role they fit: TecTech's
// EOH blocks on the Eye, the Black Hole Compressor's blocks on the Kerr (see "Contract additions (foundation-code)"
// FC-1). The Java units own the final char -> element mapping; this names the intent.
const LEGEND = {
  kerr: {
    "~": { role: "controller", kind: "control", block: "Kerr Ergosphere Engine controller (MTE 28114)" },
    A: { role: "cage_casing", kind: "casing", block: "Casings.ExtremeDensitySpaceBendingCasing (GregTechAPI.sBlockCasings10:11, BHC)" },
    B: { role: "cage_frame", kind: "frame", block: "GTStructureUtility.ofFrame(Materials.NaquadahAlloy) (BHC frame)" },
    C: { role: "mirror", kind: "mirror", block: "NEW slCasing:1 Superradiant Mirror Casing" },
    D: { role: "mirror_backing", kind: "mirrorback", block: "NEW slCasing:2 Mirror Backing Casing" },
    E: { role: "injector_rail", kind: "injector", block: "NEW slCasing:3/4/5 Bolt Injector Rail Mk I/II/III, ofBlocksTiered (lowest tier sets the bolt cap)", tiered: true },
    F: { role: "quadrupole", kind: "quad", block: "NEW slCasing:7 Beam Focusing Quadrupole" },
    G: { role: "beam_pipe", kind: "pipe (+ beamhatch at x=17 and x=47)", block: "Casings.ParticleBeamGuidancePipeCasing" },
    H: { role: "field_coil", kind: "coil", block: "NEW slCasing:6 Ergosphere Field Coil" },
    I: { role: "compressor_shell", kind: "bhc", block: "Casings.BackgroundRadiationAbsorbentCasing (sBlockCasings10:12, BHC)" },
    J: { role: "arc_lining", kind: "lining", block: "NEW slCasing2:2 Harmonic Lining (the white inner face; round-4 ruling, BHC family)" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.HawkingRadiationRealignmentFocus (sBlockGlass1:4, the BHC's glass)" },
    L: { role: "arc_shell", kind: "eyeshell", block: "Casings.ExtremeDensitySpaceBendingCasing (governor and dump arc shells)" },
    M: { role: "beam_dump", kind: "dump (+ dump-arc ergohatch/output cells)", block: "NEW slCasing:8 Beam Dump" },
    N: { role: "extractor_shell", kind: "ring3", block: "NEW slCasing:15 Ergosphere Extraction Casing" },
    O: { role: "bastion", kind: "bastion", block: "NEW slCasing:13 Bastion Plate" },
    P: { role: "casimir", kind: "casimir", block: "NEW slCasing:14 Casimir Plate" },
    e: { role: "energy_socket", kind: "energy", block: "HatchElement.Energy / ExoticEnergy, else fallback", fallback: "L", min: 1, hint: 1 },
    m: { role: "maintenance_socket", kind: "maint", block: "HatchElement.Maintenance, else fallback", fallback: "L", min: 1, hint: 2 },
    b: { role: "bolt_bus", kind: "boltbus", block: "KerrHatchElement.BoltBus (private input bus list), else fallback", fallback: "A", min: 1, hint: 3 },
    u: { role: "utility_socket", kind: "maint (beside each bolt bus)", block: "BHC utility hatch MTEBlackHoleUtility (MetaTileEntityIDs.HATCH_BLACK_HOLE_UTILITY 3012, redstone hole-state output), optional, else fallback", fallback: "A", min: 0, hint: 6 },

    S: { role: "window_cage_casing", kind: "casing (the outer mirror ring's skin, the drum band's pane, |y| <= 2)", block: "Casings.QuantumGlass (TecTech) window pane; also accepts the old Extreme Density Space-Bending Casing (sBlockCasings10:11), so a Kerr built before the window bands still forms", window: "A" },
    T: { role: "window_arc_shell", kind: "eyeshell (governor arc's outer shell in front of the hole, the front pane, |y| <= 2)", block: "Casings.QuantumGlass (TecTech) window pane; also accepts the old Extreme Density Space-Bending Casing (sBlockCasings10:11), so a Kerr built before the window bands still forms", window: "L" },
  },
  // Kerr redesign (docs/kerr-redesign.md §6): each module arc is its own machine, exported in its own frame with its
  // own controller front-centre facing out of the cage. Its sockets go into GT's STANDARD hatch lists.
  kerr_compressor: {
    "~": { role: "controller", kind: "control on the compressor arc (the old core cell)", block: "Kerr Compressor Module controller (MTE 28117)" },
    G: { role: "beam_pipe", kind: "pipe", block: "Casings.ParticleBeamGuidancePipeCasing" },
    I: { role: "compressor_shell", kind: "bhc", block: "Casings.BackgroundRadiationAbsorbentCasing (sBlockCasings10:12, BHC)" },
    J: { role: "arc_lining", kind: "lining", block: "NEW slCasing2:2 Harmonic Lining" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.HawkingRadiationRealignmentFocus (sBlockGlass1:4, the BHC's glass)" },
    c: { role: "module_io", kind: "beamhatch/energy/maint on the compressor arc", block: "HatchElement InputBus / InputHatch / OutputBus / OutputHatch / Energy / ExoticEnergy (the module's own standard lists), else fallback", fallback: "I", min: "1 in + 1 out + 1 energy", hint: 1 },
  },
  kerr_extractor: {
    "~": { role: "controller", kind: "ring3, the outermost arc cell on the 340 deg ray at y 0", block: "Kerr Extractor Module controller (MTE 28118)" },
    J: { role: "arc_lining", kind: "lining", block: "NEW slCasing2:2 Harmonic Lining" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.HawkingRadiationRealignmentFocus (sBlockGlass1:4, the BHC's glass)" },
    N: { role: "extractor_shell", kind: "ring3", block: "NEW slCasing:15 Ergosphere Extraction Casing" },
    x: { role: "module_io", kind: "output on the inner face (334/340/346 deg) and ring3 at y +-2 beside the controller", block: "HatchElement InputBus / OutputBus / Energy / ExoticEnergy (item buses only: GT's extractor map), else fallback", fallback: "N", min: "1 in + 1 out + 1 energy", hint: 1 },
  },
  // Hint-only: where each module controller stands, in the KEE's frame (the KEE's hologram marks them; nothing is placed).
  kerr_slots: {
    1: { role: "compressor_slot", kind: "marker", block: "StructureLib ofHint(7): the Kerr Compressor Module controller goes here" },
    2: { role: "extractor_slot", kind: "marker", block: "StructureLib ofHint(8): the Kerr Extractor Module controller goes here" },
  },
  eye: {
    "~": { role: "controller", kind: "control", block: "Eye of Harmony controller (MTE 28115)" },
    U: { role: "band_shell", kind: "eyeshell (core band, rails, pylons)", block: "Casings.ReinforcedSpatialStructureCasing (TTCasingsContainer.sBlockCasingsBA0:11)" },
    Q: { role: "plinth", kind: "deck", block: "Casings.ReinforcedTemporalStructureCasing (BA0:10)" },
    J: { role: "band_lining", kind: "lining", block: "Casings.InfiniteSpacetimeEnergyBoundaryCasing (BA0:12)" },
    Z: { role: "glass_channel", kind: "sglass", block: "Casings.QuantumGlass (TecTech)" },
    W: { role: "compression_field", kind: "eyeshell clamps, upper rail -> core (T1)", block: "TTCasingsContainer.SpacetimeCompressionFieldGenerators metas 0-8, ofBlocksTiered (as TecTech's EOH)", tiered: true },
    X: { role: "dilation_field", kind: "eyeshell clamps, core -> lower rail (T1)", block: "TTCasingsContainer.TimeAccelerationFieldGenerator metas 0-8, ofBlocksTiered", tiered: true },
    Y: { role: "stabilisation_field", kind: "deck, the centre ring of each pylon foot pad (8 x 6)", block: "TTCasingsContainer.StabilisationFieldGenerators metas 0-8, ofBlocksTiered", tiered: true },
    R: { role: "beam_band", kind: "ring2", block: "NEW slCasing:10 Beam Band Casing" },
    S: { role: "jet_crescent", kind: "ring3", block: "NEW slCasing:11 Jet Crescent Casing" },
    T: { role: "galactic_shell", kind: "galactic", block: "NEW slCasing:12 Galactic Shell Plating (T4-T6 bands, D-49 R-12)" },
    h: { role: "gas_socket", kind: "gas", block: "EyeHatchElement.Gas (MTEHatchInput, private -> H/He buffers), else fallback", fallback: "Q", min: 1, hint: 1 },
    o: { role: "me_output_socket", kind: "energy (plinth)", block: "EyeHatchElement.Output (MTEHatchOutputBusME, MTEHatchOutputME), else fallback", fallback: "Q", min: "1 bus + 1 hatch", hint: 2 },
    m: { role: "maintenance_socket", kind: "maint", block: "HatchElement.Maintenance, else fallback", fallback: "Q", min: 1, hint: 3 },
    s: { role: "stellar_socket_beam", kind: "beamhatch on the beam band", block: "EyeHatchElement.Stellar (MTEHatchInput, private -> stellar buffers), else fallback", fallback: "R", min: 0, hint: 4 },
    t: { role: "stellar_socket_jet", kind: "beamhatch on the jet crescent", block: "EyeHatchElement.Stellar, else fallback", fallback: "S", min: 0, hint: 4 },
    // D-49 (d49-build-plan.md §11.1): the stability-field sockets on the core band's outer face, and the T6 rim's
    // four GRB cells as rim stellar sockets (R-11; GRB proper is T7+).
    f: { role: "field_socket", kind: "eyeshell (core band outer face, 1 below the mid-plane)", block: "EyeHatchElement.Field (MTEHatchInput, unbuffered trims; ME stocking hatches are walked but never drained), else fallback", fallback: "U", min: 0, hint: 5 },
    n: { role: "sensor_socket", kind: "eyeshell (core band outer face, on the mid-plane, above its f)", block: "EyeHatchElement.Sensor (MTEHatchStellarFieldSensor, MTE 28116), else fallback", fallback: "U", min: 0, hint: 6 },
    g: { role: "stellar_socket_rim", kind: "grb on the T6 rim", block: "EyeHatchElement.Stellar, else fallback", fallback: "T", min: 0, hint: 4 },

    A: { role: "window_band_shell", kind: "eyeshell (the core band's outer shell in its front ring segments, the pane, |y| <= 2)", block: "Casings.QuantumGlass (TecTech) window pane; also accepts the old Reinforced Spatial Structure Casing (BA0:11), so an Eye built before the window bands still forms", window: "U" },
  },
  // The Homogeny, Reality Diffraction and Reality Refab (design repo designs/docs/homogeny-reality.md, PROPOSED). Chars
  // shared by the three machines mean the same thing in each (E crystal, J lining, K channel, L lens, Q BEC casing,
  // Z glass); the rest are per machine. New blocks are the slCasing3 metas (BlockSLCasing3).
  homogeny: {
    "~": { role: "controller", kind: "control (the volute throat's outer face)", block: "The Homogeny controller (MTE 28130)" },
    A: { role: "volute_shell", kind: "stir (volute, discharge throat, throat tip shell)", block: "NEW slCasing3:0 Stirring Band Casing" },
    B: { role: "shear_helix", kind: "helix", block: "NEW slCasing3:1 Shear Helix Coil" },
    C: { role: "feed_vessel", kind: "vessel", block: "NEW slCasing3:2 Feed Vessel Casing" },
    D: { role: "vortex_inlet", kind: "inlet", block: "NEW slCasing3:3 Vortex Inlet" },
    E: { role: "region_crystal", kind: "region (throat tip core; T3 tail tip)", block: "NEW slCasing3:5 Region Crystal" },
    F: { role: "outer_ring_shell", kind: "eternity (T3 outer ring and tail)", block: "NEW slCasing3:10 Eternity Shell Casing" },
    G: { role: "stirring_ring_shell", kind: "galactic (T2 middle ring)", block: "slCasing:12 Galactic Shell Plating (existing)" },
    J: { role: "band_lining", kind: "lining", block: "slCasing2:2 Harmonic Lining (existing)" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.QuantumGlass (TecTech)" },
    M: { role: "column_cap", kind: "temporal (column caps, cap-rim clamps; the caps' centres are qglass, Z)", block: "Casings.ReinforcedTemporalStructureCasing (TecTech EOH, x32 in the BEC since ruling 11a; was Godforge casing + lens glass, B-80)" },
    O: { role: "rail", kind: "bastion (rails, spikes, clamps, T2 spokes)", block: "slCasing:13 Bastion Plate (existing)" },
    P: { role: "ring_trim", kind: "casimir (T3 outer ring trim)", block: "slCasing:14 Casimir Plate (existing)" },
    Z: { role: "column_glass", kind: "qglass (mixing column wall, vessel sight bands)", block: "Casings.QuantumGlass (TecTech)" },
    h: { role: "feed_socket", kind: "gas (top of each Feed Vessel)", block: "private feed Input Hatch element (MTEHatchInput), else fallback", fallback: "C", min: 1, hint: 1 },
    o: { role: "output_socket", kind: "output (throat, top and bottom faces)", block: "HatchElement.OutputHatch, else fallback", fallback: "A", min: 1, hint: 2 },
    e: { role: "energy_socket", kind: "energy (bottom cap rim)", block: "HatchElement.Energy / ExoticEnergy, else fallback", fallback: "M", min: 1, hint: 3 },
    g: { role: "grb_port_reserved", kind: "grb (T2 stirring ring outer face, 3 cells; quasar-p4.md §3.1)", block: "RESERVED for P4's GRB Receptor Port (28125); Galactic Shell Plating until then, so adding the port never un-forms a build", fallback: "G", min: 0 },
  },
  reality_diffraction: {
    "~": { role: "controller", kind: "control (base ring, front)", block: "Reality Diffraction controller (MTE 28131)" },
    A: { role: "deck_band_shell", kind: "deckband", block: "NEW slCasing3:4 Region Deck Band" },
    E: { role: "region_crystal", kind: "region (six Region Blades, apex crystal)", block: "NEW slCasing3:5 Region Crystal" },
    H: { role: "phase_ring_shell", kind: "phase", block: "NEW slCasing3:6 Phase Ring Casing" },
    I: { role: "anchor_lens", kind: "alens (either side of each anchor socket)", block: "NEW slCasing3:11 Region Anchor Casing" },
    J: { role: "band_lining", kind: "lining", block: "slCasing2:2 Harmonic Lining (existing)" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.QuantumGlass (TecTech)" },
    L: { role: "spine_glass", kind: "gflens (spine)", block: "Casings.SpatiallyTranscendentGravitationalLens (Godforge glass)" },
    N: { role: "spine_conduit", kind: "cpconduit (spine ribs, cap, struts, base glass line)", block: "Casings.CoherencePreservingPlasmaConduit (RC-2 BEC, sBlockCasingsBEC:0)" },
    Q: { role: "base_ring", kind: "emiso (base ring, hub, spokes)", block: "Casings.ElectromagneticallyIsolatedCasing (RC-2 BEC, sBlockCasingsBEC:1)" },
    Z: { role: "drum_glass", kind: "qglass (drum walls)", block: "Casings.QuantumGlass (TecTech)" },
    b: { role: "bec_socket", kind: "bechatch (base ring)", block: "GT MTEHatchBEC (BEC Hatch), else fallback", fallback: "Q", min: 1, hint: 1 },
    h: { role: "medium_socket", kind: "gas (base ring)", block: "HatchElement.InputHatch (Entanglement Medium), else fallback", fallback: "Q", min: 1, hint: 2 },
    e: { role: "energy_socket", kind: "energy (base ring)", block: "HatchElement.Energy / ExoticEnergy, else fallback", fallback: "Q", min: 1, hint: 9 },
    r: { role: "anchor_socket_I", kind: "anchor, Region I Hyperspace (drum band y -14, 0 deg)", block: "private Region Anchor element I (MTEHatchInput), else fallback", fallback: "I", min: 0, hint: 3 },
    s: { role: "anchor_socket_II", kind: "anchor, Region II Subspace (y -14, 180 deg)", block: "private Region Anchor element II, else fallback", fallback: "I", min: 0, hint: 4 },
    t: { role: "anchor_socket_III", kind: "anchor, Region III Forward Time (y 0, 60 deg)", block: "private Region Anchor element III, else fallback", fallback: "I", min: 0, hint: 5 },
    u: { role: "anchor_socket_IV", kind: "anchor, Region IV Reverse Time (y 0, 240 deg)", block: "private Region Anchor element IV, else fallback", fallback: "I", min: 0, hint: 6 },
    v: { role: "anchor_socket_V", kind: "anchor, Region V Positive Mass (y 14, 120 deg)", block: "private Region Anchor element V, else fallback", fallback: "I", min: 0, hint: 7 },
    w: { role: "anchor_socket_VI", kind: "anchor, Region VI Negative Mass (y 14, 300 deg)", block: "private Region Anchor element VI, else fallback", fallback: "I", min: 0, hint: 8 },
    g: { role: "grb_port_reserved", kind: "grb (top deck band outer face, 6 cells; quasar-p4.md §3.1)", block: "RESERVED for P4's GRB Receptor Port (28125); Region Deck Band until then, so adding the port never un-forms a build", fallback: "A", min: 0 },
  },
  reality_refab: {
    "~": { role: "controller", kind: "control (foot, front)", block: "Reality Refab controller (MTE 28132)" },
    E: { role: "region_crystal", kind: "region (Ground Anchor, six Region Receivers, Reality Focus)", block: "NEW slCasing3:5 Region Crystal" },
    J: { role: "band_lining", kind: "lining", block: "slCasing2:2 Harmonic Lining (existing)" },
    K: { role: "glass_channel", kind: "sglass", block: "Casings.QuantumGlass (TecTech)" },
    L: { role: "spindle_glass", kind: "gflens (spindle, focus tip)", block: "Casings.SpatiallyTranscendentGravitationalLens (Godforge glass)" },
    Q: { role: "foot", kind: "emiso (foot and skirt)", block: "Casings.ElectromagneticallyIsolatedCasing (RC-2 BEC, sBlockCasingsBEC:1)" },
    R: { role: "hull_shell", kind: "hull (stem, ribs, belt, cap, clamps)", block: "NEW slCasing3:7 Reassembly Hull Plating" },
    S: { role: "tray", kind: "tray", block: "NEW slCasing3:8 Reassembly Tray Plating" },
    T: { role: "claw", kind: "claw", block: "NEW slCasing3:9 Reassembly Claw" },
    V: { role: "collar", kind: "gcoil (tray collars)", block: "Casings.CondensateGuidanceCoil (RC-2 BEC, sBlockCasingsBEC:6)" },
    Z: { role: "hull_glass", kind: "qglass (hull panels)", block: "Casings.QuantumGlass (TecTech)" },
    b: { role: "bec_socket", kind: "bechatch (foot)", block: "GT MTEHatchBEC (BEC Hatch), else fallback", fallback: "Q", min: 1, hint: 1 },
    i: { role: "input_bus_socket", kind: "ibus (foot)", block: "HatchElement.InputBus, else fallback", fallback: "Q", min: 1, hint: 2 },
    o: { role: "output_bus_socket", kind: "obus (cap, by the Reality Focus)", block: "HatchElement.OutputBus, else fallback", fallback: "R", min: 1, hint: 3 },
    e: { role: "energy_socket", kind: "energy (foot)", block: "HatchElement.Energy / ExoticEnergy, else fallback", fallback: "Q", min: 1, hint: 4 },
  },
};

// plain kind -> char (sockets and pinned cells come from the socket table instead)
const KERR_KIND = { casing: "A", frame: "B", mirror: "C", mirrorback: "D", injector: "E", quad: "F", pipe: "G", coil: "H",
  bhc: "I", lining: "J", sglass: "K", eyeshell: "L", dump: "M", ring3: "N", bastion: "O", casimir: "P" };
const EYE_KIND = { eyeshell: "U", deck: "Q", lining: "J", sglass: "Z", ring2: "R", ring3: "S", galactic: "T" };
const SOCKET_KINDS = new Set(["control", "energy", "maint", "boltbus", "beamhatch", "output", "ergohatch", "gas", "grb", "dynamo"]);

// ------------------------------------------------------------------------------------------------ socket table
// Every hatch-like cell, pinned by its voxel coordinate (the exact expression voxel.js uses), with the char it
// exports as. Any hatch-like cell missing from this table, or a table entry whose cell holds another kind, aborts.
const d2r = (a) => (a * Math.PI) / 180;
const polar = (r, a, y) => [r * Math.cos(d2r(a)), y, r * Math.sin(d2r(a))];
const KERR_SOCKETS = [
  { at: [0, 0, 37.6], kind: "control", ch: "~" },
  { at: [-2, 0, 37.6], kind: "maint", ch: "m" },
  { at: [2, 0, 37.6], kind: "energy", ch: "e" },
  { at: [4, 0, 37.6], kind: "energy", ch: "e" },
  ...[45, 135, 225, 315].flatMap((c) => [5, -5].flatMap((yy) => [
    { at: polar(34, c, yy), kind: "boltbus", ch: "b" },
    { at: polar(34, c, yy + (yy > 0 ? 1 : -1)), kind: "maint", ch: "u" },
  ])),
  { at: [17, 0, 0], kind: "beamhatch", ch: "G" },
  { at: [47, 0, 0], kind: "beamhatch", ch: "G", tier: 2 },
  { at: [-38, 0, 0], kind: "ergohatch", ch: "M" },
  { at: [-38, 3, 0], kind: "output", ch: "M" },
  ...[255, 265, 275, 285].map((a) => ({ at: polar(32.6, a, 0), kind: "beamhatch", ch: "c" })),
  { at: [0, 0, -39.6], kind: "energy", ch: "c" },
  { at: [0, 2, -39.6], kind: "maint", ch: "c" },
  { at: [0, -2, -39.6], kind: "control", ch: "q" }, // the compressor module's controller (becomes '~' in its own piece)
  { at: polar(32.8, 334, 0), kind: "output", ch: "x" },
  // the old extractor core cell: an I/O socket since the redesign (the module's controller moved to the outer face)
  { at: polar(32.8, 340, 0), kind: "output", ch: "x" },
  { at: polar(32.8, 346, 0), kind: "output", ch: "x" },
];
// Eye: the plinth ring through the hatches is ellipse (46+4.6, 35+4.6) at y = GROUND = -40.
const EYE_PF = (a) => [50.6 * Math.cos(d2r(a)), -40, 39.6 * Math.sin(d2r(a))];
// The plan's 15, 105, 195 and 285 sit inside the arm-blade roots (each blade covers its root +5..+30 deg one row below
// the mid-plane at T4/T5), so every pair is 45 and 75 deg past an arm root: 15 deg clear of the nearest clamp strut.
const EYE_FIELD_ANGLES = [45, 75, 135, 165, 225, 255, 315, 345];
function EYE_CORE_FACE(a, y) {
  const t = d2r(a), c = [34 * Math.cos(t), 0, 25 * Math.sin(t)];
  const T = [-34 * Math.sin(t), 0, 25 * Math.cos(t)], tl = Math.hypot(T[0], T[2]), Tn = [T[0] / tl, 0, T[2] / tl];
  const dot = c[0] * Tn[0] + c[2] * Tn[2], N = [c[0] - Tn[0] * dot, 0, c[2] - Tn[2] * dot], nl = Math.hypot(N[0], N[2]);
  return [c[0] + (N[0] / nl) * 2.5, y, c[2] + (N[2] / nl) * 2.5];
}
const EYE_SOCKETS = [
  ...[0, 60, 120, 240, 300].map((a) => ({ at: EYE_PF(a), kind: "gas", ch: "h" })),
  { at: EYE_PF(90), kind: "control", ch: "~" },
  { at: [EYE_PF(90)[0] + 2, -40, EYE_PF(90)[2]], kind: "maint", ch: "m" },
  { at: [EYE_PF(90)[0] - 2, -40, EYE_PF(90)[2]], kind: "energy", ch: "o" },
  { at: [EYE_PF(90)[0] + 4, -40, EYE_PF(90)[2]], kind: "energy", ch: "o" },
  // beam band (T2): ellipse a44 b32 at y13, port 2.3 outward at a+30
  ...[0, 60, 120, 180, 240, 300].map((a) => { const t = d2r(a + 30), c = [44 * Math.cos(t), 13, 32 * Math.sin(t)];
    const l = Math.hypot(c[0], c[2]); return { at: [c[0] + (c[0] / l) * 2.3, 13, c[2] + (c[2] / l) * 2.3], kind: "beamhatch", ch: "s", tier: 2 }; }),
  // jet crescent (T3): ellipse a46 b34 at y-15, port 3 below the centreline at a+25
  ...[170, 220, 270, 320, 370].map((a) => { const t = d2r(a + 25); return { at: [46 * Math.cos(t), -18, 34 * Math.sin(t)], kind: "beamhatch", ch: "t", tier: 3 }; }),
  // D-49 Field (f) and Sensor (n) sockets: the core band (ellipse a34 b25, profile w2.5 h3) outer face, at the
  // parameter angles clear of the clamp struts (30 + 60k) and the arm roots (0, 90, 180, 270). The face cell is the
  // centre-line point pushed 2.5 along the sweep's normal (the ellipse normal); f one row below the mid-plane, n on
  // it, so every sensor stands right above its Field hatch.
  ...EYE_FIELD_ANGLES.flatMap((a) => [{ at: EYE_CORE_FACE(a, -1), kind: "eyeshell", ch: "f" }, { at: EYE_CORE_FACE(a, 0), kind: "eyeshell", ch: "n" }]),
  // T6 rim (ellipse a76 b72 at y-2): the mockup's four GRB receptors, 3.6 above the centre-line, become rim
  // stellar sockets (R-11)
  ...[45, 135, 225, 315].map((a) => ({ at: [76 * Math.cos(d2r(a)), -2 + 3.6, 72 * Math.sin(d2r(a))], kind: "grb", ch: "g", tier: 6 })),
];

// ------------------------------------------------------------------------------------------------ helpers
function gridOf(b, at) { return [Math.round(at[0]) + b.CX, Math.round(at[1]) + b.CY, Math.round(at[2]) + b.CZ]; }
const keyOf = (x, y, z) => x + "," + y + "," + z;
function socketMap(b, table, tier) {
  const m = new Map();
  for (const s of table) {
    if (s.tier && tier < s.tier) continue;
    const g = gridOf(b, s.at), k = keyOf(...g);
    if (m.has(k)) fail(`two socket entries on one cell ${k}`);
    const v = b.world[g[0] + g[2] * b.W + g[1] * b.W * b.D];
    const kind = v ? KIND[v - 1] : "(empty)";
    if (kind !== s.kind) fail(`socket table: expected ${s.kind} at voxel ${s.at.map((n) => n.toFixed(2))} (grid ${k}), found ${kind}`);
    m.set(k, s.ch);
  }
  return m;
}
// Walk every placed (non-render) cell: cb(x, y, z, kind, tagName)
function eachCell(b, cb) {
  const { W, H, D, world, tags } = b;
  for (let y = 0; y < H; y++) for (let z = 0; z < D; z++) for (let x = 0; x < W; x++) {
    const i = x + z * W + y * W * D, v = world[i];
    if (!v || isRender(v - 1)) continue;
    cb(x, y, z, KIND[v - 1], V.TAGS[tags[i]] || "");
  }
}

// ------------------------------------------------------------------------------------------------ window bands
// my answer 2026-10-13 (render QA K2 + E-9, "Glass window bands"): the Kerr's hole and the Eye's centre object
// must read from the front at eye level (|y| <= WINDOW_HALF from the voxel origin, which is the hole / star centre).
// Each window is ONE Quantum Glass pane with the cells behind it OPEN (' ', checked as nothing): Quantum Glass is a
// glowing blue glass, and the first real-client pass (three glass walls deep on the Kerr's sightline, two to eight on
// the Eye's) hid the hole completely and the star almost (B-86). A pane cell gets its own char per block it replaces,
// and its Java element accepts Quantum Glass OR that old block (glass first, so holograms and auto-builds place
// glass); an open cell accepts anything, so a Kerr or Eye built before the windows still forms. voxel.js is not
// edited: this only re-labels cells it already places.
const WINDOW_HALF = 2; // 5 rows: the object's equator +-2
const WINDOW = {
  // Kerr. The pane in front of the hole is the governor arc's shell (z > 0, |x| <= 12, the arc's span); behind it the
  // arc's lining, both mirror rings and (T2) the reinforcement's mid band in front of the arc are open. All round the
  // rest of the drum, the band's pane is the outer ring's outer skin (its casing) and the rest of both rings is open:
  // "a Quantum Glass band in the Kerr drum". Frames, rails, quadrupoles, pipes and the BHC glass stay.
  kerr: { half: 12, drum: { kinds: new Set(["mirror", "mirrorback", "casing"]), rMin: 16.5, rMax: 28.5 },
    front: new Set(["eyeshell", "lining", "bastion", "casimir"]),
    pane: { A: "S", L: "T" } },
  // Eye. The pane is the core band's shell in its front ring segments (z > 0, |x| <= 26: the band from its 40 to its
  // 140 deg, between the clamp struts at 30 and 150); its lining behind it is open, and so are the T4/T5 arm and T6
  // rim cells in front of it, so one pane is all the sightline crosses at every tier. Struts (W/X), sockets and the
  // glass channel (Z) stay.
  eye: { half: 26, laterHalf: 10, kinds: new Set(["eyeshell", "lining", "galactic"]), pane: { U: "A" } },
};
// c.was = the char the cell had before; c.open = no block wanted (exported as ' ').
// In front of the object only the FIRST contiguous run of pane-kind cells seen from the viewer (+z) in each (x, y)
// column is the pane (a 2-thick shell is one wall; a clamp root deeper inside the band would be a second one).
function frontPane(cells, b, isPaneKind) {
  const cols = new Map();
  for (const c of cells) {
    if (!isPaneKind(c)) continue;
    const k = (c.x - b.CX) + "," + (c.y - b.CY);
    (cols.get(k) || cols.set(k, []).get(k)).push(c);
  }
  const pane = new Set();
  for (const list of cols.values()) {
    list.sort((p, q) => q.z - p.z);
    for (let i = 0; i < list.length && (i === 0 || list[i].z === list[i - 1].z - 1); i++) pane.add(list[i]);
  }
  return pane;
}
function windowKerr(b, cells) {
  const w = WINDOW.kerr, front = [], drum = [];
  for (const c of cells.values()) {
    const x = c.x - b.CX, y = c.y - b.CY, z = c.z - b.CZ, r = Math.hypot(x, z);
    if (Math.abs(y) > WINDOW_HALF || /[a-z~]/.test(c.ch) || c.piece !== "kerr_core" && c.piece !== "kerr_density") continue;
    const inFront = z > 0 && Math.abs(x) <= w.half;
    if (w.drum.kinds.has(c.kind) && r >= w.drum.rMin && r <= w.drum.rMax) (inFront ? front : drum).push(c);
    else if (inFront && w.front.has(c.kind) && r > w.drum.rMax) front.push(c);
  }
  // the governor arc's outer shell is the front pane; the drum's pane elsewhere is the outer ring's casing skin
  const pane = frontPane(front, b, (c) => c.kind === "eyeshell");
  for (const c of front) { c.was = c.ch; if (pane.has(c)) c.ch = w.pane[c.was]; else { c.ch = " "; c.open = true; } }
  for (const c of drum) { c.was = c.ch; if (c.kind === "casing") c.ch = w.pane[c.was]; else { c.ch = " "; c.open = true; } }
}
function windowEye(b, cells) {
  const w = WINDOW.eye, front = [];
  for (const c of cells.values()) {
    const x = c.x - b.CX, y = c.y - b.CY, z = c.z - b.CZ;
    if (Math.abs(y) > WINDOW_HALF || /[a-z~]/.test(c.ch) || !w.kinds.has(c.kind) || z <= 0) continue;
    // the core band (its shell and lining) over the full front segments; a later tier's arm or rim cell only where
    // it hides the T4-T6 objects (|x| <= laterHalf), so the arms and the rim keep their shape
    if (Math.abs(x) > (c.tag === "core" ? w.half : w.laterHalf)) continue;
    front.push(c);
  }
  const pane = frontPane(front, b, (c) => c.kind === "eyeshell");
  for (const c of front) { c.was = c.ch; if (pane.has(c)) c.ch = w.pane[c.was]; else { c.ch = " "; c.open = true; } }
}

// ------------------------------------------------------------------------------------------------ classify
function classifyKerr(t) {
  const b = V.build("kerr", t), sockets = socketMap(b, KERR_SOCKETS, t);
  const cells = new Map(); // key -> { x,y,z, ch, kind, piece }
  eachCell(b, (x, y, z, kind, tag) => {
    const k = keyOf(x, y, z);
    let ch = sockets.get(k);
    if (!ch) {
      if (SOCKET_KINDS.has(kind)) fail(`kerr T${t}: unpinned hatch-like cell ${kind} at grid ${k}`);
      ch = KERR_KIND[kind];
      if (!ch) fail(`kerr T${t}: no char for kind ${kind}`);
    }
    if (!tag.startsWith("kerr_")) fail(`kerr T${t}: untagged cell at ${k}`);
    cells.set(k, { x, y, z, ch, kind, piece: tag });
  });
  for (const k of sockets.keys()) if (!cells.has(k)) fail(`kerr T${t}: socket ${k} not placed`);
  windowKerr(b, cells);
  return { b, cells };
}

// T1 clamp struts: cells placed in the T1 build that are air when the same build runs without clamps.
const EYE_STRUTS = (() => {
  const a = V.build("eye", 1), n = V.build("eye", 1, { noClamps: true }), s = new Set();
  eachCell(a, (x, y, z, kind, tag) => { const v = n.world[x + z * n.W + y * n.W * n.D];
    // Only the open span between the bands: core band |y| <= 3 (h 3), rails at y +-8 with h 1.5 (from 6.5 out).
    // Cells inside a band's hollow are clamp ROOTS and stay band shell.
    const yr = Math.abs(y - a.CY);
    if (tag === "" && kind === "eyeshell" && (!v || isRender(v - 1)) && yr >= 4 && yr <= 6) s.add(keyOf(x, y, z)); });
  return s;
})();

function classifyEye(t) {
  const b = V.build("eye", t), sockets = socketMap(b, EYE_SOCKETS, t);
  const cells = new Map();
  eachCell(b, (x, y, z, kind, tag) => {
    const k = keyOf(x, y, z);
    let ch = sockets.get(k);
    if (!ch) {
      if (SOCKET_KINDS.has(kind)) fail(`eye T${t}: unpinned hatch-like cell ${kind} at grid ${k}`);
      ch = EYE_KIND[kind];
      if (!ch) fail(`eye T${t}: no char for kind ${kind}`);
      // The T1 clamp struts (between the bands, not their roots inside a band) are the field generators:
      // upper rail -> core = spacetime compression, core -> lower rail = time dilation.
      if (EYE_STRUTS.has(k) && kind === "eyeshell") ch = (y - b.CY) > 0 ? "W" : (y - b.CY) < 0 ? "X" : fail(`eye T${t}: strut cell on y=0 at ${k}`);
      if (kind === "deck" && tag === "pad") ch = "Y";
    }
    cells.set(k, { x, y, z, ch, kind, tag });
  });
  for (const k of sockets.keys()) if (!cells.has(k)) fail(`eye T${t}: socket ${k} not placed`);
  windowEye(b, cells);
  return { b, cells };
}

// ------------------------------------------------------------------------------------------------ build pieces
const report = { warnings: [], notes: [] };

const K1 = classifyKerr(1), K2 = classifyKerr(2);
if (K1.b.CX !== K2.b.CX || K1.b.W !== K2.b.W) fail("kerr T1/T2 worlds differ in size");

// Kerr redesign (docs/kerr-redesign.md §6.1): every module arc is a separate machine with its own controller on the
// arc's OUTER face. `out` is the controller's outward face in voxel space (it becomes the module piece's front, +z).
//  - Compressor: the old core cell 'q' (the mockup's "control", outer face at 270 deg) protrudes into the gap that the
//    Kerr T2 reinforcement band (lining + bastion, z -41..-44 for y -2..0) closes, so a player could not reach it at
//    T2. The controller therefore takes the arc's outer shell cell straight below it, at the first y whose front cell
//    and the cell above that are open air in the T2 build (y -4), and the old core cell becomes a 'c' socket.
//  - Extractor: the old core cell sits on the INNER face facing the hole (it is an 'x' socket now, see the socket
//    table); the controller moves to the outermost arc cell on the 340 deg ray at y 0, and the two outer-face cells at
//    y +-2 beside it become 'x' sockets (the compressor's stacked control / energy / maintenance layout).
const KERR_MODULES = [
  { piece: "kerr_compressor", slot: "compressor", mte: 28117, mark: "1", out: [0, 0, -1], below: "q" },
  { piece: "kerr_extractor", slot: "extractor", mte: 28118, mark: "2", out: [1, 0, 0], ray: 340, extraSockets: [2, -2],
    legacyAt: polar(32.8, 340, 0) },
];
function moduleControllers(K, t) {
  const { b, cells } = K, found = {};
  const at = (x, y, z) => cells.get(keyOf(x + b.CX, y + b.CY, z + b.CZ));
  // open air in the FULLEST build (T2), so T1 and T2 pick the same cell
  const airT2 = (x, y, z) => !K2.cells.has(keyOf(x + b.CX, y + b.CY, z + b.CZ));
  for (const m of KERR_MODULES) {
    let ctrl;
    if (m.below) {
      const l = [...cells.values()].filter((c) => c.piece === m.piece && c.ch === m.below);
      if (l.length !== 1) fail(`kerr T${t}: ${m.piece} has ${l.length} '${m.below}' cells`);
      const core = l[0], cx = core.x - b.CX, cz = core.z - b.CZ, sx = cx - m.out[0], sz = cz - m.out[2];
      for (let y = core.y - b.CY - 1; y >= -b.CY && !ctrl; y--) {
        const c = at(sx, y, sz);
        if (!c || c.piece !== m.piece) continue;
        if (airT2(cx, y, cz) && airT2(cx, y + 1, cz)) ctrl = c;
      }
      if (!ctrl) fail(`kerr T${t}: no reachable shell cell below the ${m.piece} core cell`);
      if (ctrl.ch !== "I") fail(`kerr T${t}: ${m.piece} controller cell holds '${ctrl.ch}', expected shell 'I'`);
      core.ch = "c";
      found[m.slot + ".legacy"] = core;
    } else {
      for (let r = 20; r <= 60; r += 0.25) {
        const c = at(Math.round(r * Math.cos(d2r(m.ray))), 0, Math.round(r * Math.sin(d2r(m.ray))));
        if (c && c.piece === m.piece) ctrl = c;
      }
      if (!ctrl) fail(`kerr T${t}: no ${m.piece} cell on the ${m.ray} deg ray`);
      if (ctrl.ch !== "N") fail(`kerr T${t}: ${m.piece} controller cell holds '${ctrl.ch}', expected shell 'N'`);
      for (const dy of m.extraSockets) {
        const s = at(ctrl.x - b.CX, ctrl.y - b.CY + dy, ctrl.z - b.CZ);
        if (!s || s.piece !== m.piece || s.ch !== "N") fail(`kerr T${t}: ${m.piece} has no shell cell at y ${dy} beside its controller`);
        s.ch = "x";
      }
    }
    ctrl.ch = "~";
    found[m.slot] = ctrl;
    if (m.legacyAt) found[m.slot + ".legacy"] = cells.get(keyOf(...gridOf(b, m.legacyAt)));
    if (!found[m.slot + ".legacy"]) fail(`kerr T${t}: ${m.piece} has no legacy core cell`);
  }
  return found;
}
const KM1 = moduleControllers(K1, 1), KM2 = moduleControllers(K2, 2);
for (const m of KERR_MODULES) {
  const a = KM1[m.slot], c = KM2[m.slot];
  if (a.x !== c.x || a.y !== c.y || a.z !== c.z) fail(`${m.piece}: controller differs between the T1 and T2 builds`);
}
const pick = (cells, pred) => [...cells.values()].filter(pred);
const kerrPieces = {
  kerr_t1: pick(K1.cells, (c) => c.piece === "kerr_core" && !c.open),
  kerr_t2: pick(K2.cells, (c) => (c.piece === "kerr_core" || c.piece === "kerr_density") && !c.open),
  kerr_compressor: pick(K1.cells, (c) => c.piece === "kerr_compressor"),
  kerr_extractor: pick(K1.cells, (c) => c.piece === "kerr_extractor"),
};
if (pick(K1.cells, (c) => c.piece === "kerr_density").length) fail("kerr T1 has density cells");
// modules identical in both builds
for (const p of ["kerr_compressor", "kerr_extractor"]) {
  const a = kerrPieces[p], bb = pick(K2.cells, (c) => c.piece === p);
  const sig = (l) => l.map((c) => keyOf(c.x, c.y, c.z) + c.ch).sort().join("|");
  if (sig(a) !== sig(bb)) {
    const sa = new Set(sig(a).split("|")), sb = new Set(sig(bb).split("|"));
    fail(`${p} differs between the T1 and T2 builds: only T1 [${[...sa].filter((k) => !sb.has(k)).slice(0, 12)}]` +
      ` only T2 [${[...sb].filter((k) => !sa.has(k)).slice(0, 12)}]; T2 overwrites ${JSON.stringify(K2.b.collide)}`);
  }
}
// T2 core only adds to T1 core
{
  const t2 = new Map(kerrPieces.kerr_t2.map((c) => [keyOf(c.x, c.y, c.z), c]));
  let changed = 0, lost = 0;
  for (const c of kerrPieces.kerr_t1) { const d = t2.get(keyOf(c.x, c.y, c.z)); if (!d) lost++; else if (d.ch !== c.ch) changed++; }
  if (lost || changed) fail(`kerr_t2 is not a superset of kerr_t1: ${lost} cells lost, ${changed} changed`);
}
// Cells written by one Kerr piece and then by another belong to the LAST writer (spec §5.2); record how many.
for (const [k, n] of Object.entries(K2.b.collide)) report.notes.push(`kerr: ${n} cells drawn by ${k.split(" × ")[0]} then by ${k.split(" × ")[1]}; the later piece owns them`);

const E = [1, 2, 3, 4, 5, 6].map(classifyEye);
const eyePieces = {}, eyeJoins = {}, overwrites = {};
{
  const names = ["eye_core", "eye_beam", "eye_jet", "eye_arms", "eye_arms2", "eye_rim"];
  eyePieces.eye_core = [...E[0].cells.values()].filter((c) => !c.open);
  for (let t = 2; t <= names.length; t++) {
    const prev = E[t - 2].cells, cur = E[t - 1].cells, added = [];
    let joins = 0;
    for (const [k, c] of prev) {
      const d = cur.get(k);
      if (!d) fail(`eye T${t} removed a T${t - 1} cell at ${k}`);
      if (d.ch !== c.ch) {
        // A later band wrote over an earlier piece's cell (a clamp root, or an arm blade's root cutting into the
        // core band). The earlier piece keeps the cell, so every piece stays additive and the core never changes.
        joins++;
        const why = d.tag === "" ? "clamp" : d.tag;
        overwrites[names[t - 1]] = overwrites[names[t - 1]] || {};
        overwrites[names[t - 1]][why] = (overwrites[names[t - 1]][why] || 0) + 1;
      }
    }
    for (const [k, c] of cur) if (!prev.has(k) && !c.open) added.push(c);
    eyePieces[names[t - 1]] = added;
    eyeJoins[names[t - 1]] = joins;
    if (joins) report.notes.push(`${names[t - 1]}: ${joins} cells written over an earlier piece in the mockup (${JSON.stringify(overwrites[names[t - 1]])}); the earlier piece keeps them`);
  }
}

// ------------------------------------------------------------------------------------------------ anchors + text
function anchorOf(cells, ch) { const l = cells.filter((c) => c.ch === ch); return l; }
const KERR_CTRL = (() => { const l = anchorOf(kerrPieces.kerr_t1, "~"); if (l.length !== 1) fail(`kerr_t1 has ${l.length} controllers`); return l[0]; })();
const EYE_CTRL = (() => { const l = anchorOf(eyePieces.eye_core, "~"); if (l.length !== 1) fail(`eye_core has ${l.length} controllers`); return l[0]; })();

function bbox(cells) {
  const mn = [Infinity, Infinity, Infinity], mx = [-Infinity, -Infinity, -Infinity];
  for (const c of cells) { const p = [c.x, c.y, c.z]; for (let i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], p[i]); mx[i] = Math.max(mx[i], p[i]); } }
  return { mn, mx, size: [mx[0] - mn[0] + 1, mx[1] - mn[1] + 1, mx[2] - mn[2] + 1] };
}
// world grid (x right, y up, z toward the controller's front) -> StructureLib (a horizontal, b down, c depth)
function toABC(bb, x, y, z) {
  return [FLIP_H ? bb.mx[0] - x : x - bb.mn[0], bb.mx[1] - y, FLIP_D ? z - bb.mn[2] : bb.mx[2] - z];
}

function render(name, machine, cells, ctrl, b) {
  const bb = bbox(cells), [w, h, d] = bb.size;
  const grid = Array.from({ length: d }, () => Array.from({ length: h }, () => new Array(w).fill(" ")));
  for (const c of cells) { const [a, bv, cc] = toABC(bb, c.x, c.y, c.z); grid[cc][bv][a] = c.ch; }
  const off = toABC(bb, ctrl.x, ctrl.y, ctrl.z);
  const counts = {};
  for (const c of cells) counts[c.ch] = (counts[c.ch] || 0) + 1;
  const sockets = cells.filter((c) => /[a-z~]/.test(c.ch)).map((c) => {
    const p = toABC(bb, c.x, c.y, c.z);
    return { ch: c.ch, abc: p, rel: [p[0] - off[0], p[1] - off[1], p[2] - off[2]],
      voxel: c.ov || [c.x - b.CX, c.y - b.CY, c.z - b.CZ] };
  }).sort((p, q) => (p.ch < q.ch ? -1 : p.ch > q.ch ? 1 : p.abc[2] - q.abc[2] || p.abc[1] - q.abc[1] || p.abc[0] - q.abc[0]));
  const lines = [`# piece=${name} w=${w} h=${h} d=${d} off=${off.join(",")} sha=${SHA}`,
    `# machine=${machine} cells=${cells.length} generated by tools/export-structures.js from designs/voxel.js; do not edit`];
  grid.forEach((layer, i) => { if (i) lines.push("---"); for (const row of layer) lines.push(row.join("").replace(/\s+$/, "")); });
  return { text: lines.join("\n") + "\n", w, h, d, off, counts, sockets, bb, grid };
}

// A voxel offset (dx, dy, dz) as a StructureLib (a, b, c) offset; bbox-independent.
const abcDelta = (dx, dy, dz) => [FLIP_H ? -dx : dx, -dy, FLIP_D ? dz : -dz];
// R: a quarter turn about +y, (x, z) -> (-z, x). A module's turns = how many R take its outward face to +z, the
// front of its own frame (layer 0 side), so its piece reads like any GT multi seen from the controller's front.
function rotQ(dx, dz, k) { let v = [dx, dz]; for (let i = 0; i < k; i++) v = [-v[1], v[0]]; return v; }
function quarterTurns(o) { for (let k = 0; k < 4; k++) { const v = rotQ(o[0], o[2], k); if (v[0] === 0 && v[1] === 1) return k; } return fail(`no quarter turn takes ${o} to +z`); }
function moduleFrame(cells, ctrl, k, b) {
  return cells.map((c) => { const [x, z] = rotQ(c.x - ctrl.x, c.z - ctrl.z, k);
    return { ...c, x, z, ov: [c.x - b.CX, c.y - b.CY, c.z - b.CZ] }; });
}
const PIECE_MACHINE = { kerr_t1: "kerr", kerr_t2: "kerr", kerr_compressor: "kerr_compressor", kerr_extractor: "kerr_extractor", kerr_slots: "kerr_slots" };
const MODULE_CTRL = {}; // piece -> controller cell in the module's own (rotated) frame
const out = {};
for (const [n, cells] of Object.entries(kerrPieces)) {
  const m = KERR_MODULES.find((q) => q.piece === n);
  if (!m) { out[n] = render(n, "kerr", cells, KERR_CTRL, K1.b); continue; }
  m.turns = quarterTurns(m.out);
  const rotated = moduleFrame(cells, KM1[m.slot], m.turns, K1.b);
  MODULE_CTRL[n] = rotated.find((c) => c.ch === "~");
  out[n] = render(n, n, rotated, MODULE_CTRL[n], K1.b);
}
// The KEE's hint-only piece: one marker per module controller cell, in the KEE's frame.
out.kerr_slots = render("kerr_slots", "kerr_slots",
  KERR_MODULES.map((m) => ({ ...KM1[m.slot], ch: m.mark })), KERR_CTRL, K1.b);
for (const [n, cells] of Object.entries(eyePieces)) out[n] = render(n, "eye", cells, EYE_CTRL, E[0].b);

// ------------------------------------------------------------------------------------------------ Homogeny + Reality
// homogeny-reality.md §1-§3 (PROPOSED). Hatch cells are pinned by kind and COUNT per build (the mockup computes their
// positions from the same curves the bands use, so a coordinate table would only restate voxel.js); a count or kind
// change aborts. Region Anchor sockets get one char per region (r..w = I..VI), so region identity is the socket's
// char, never the fluid (homogeny-reality.md §7 regression traps).
const HR_KIND = {
  homogeny: { stir: "A", helix: "B", vessel: "C", inlet: "D", region: "E", eternity: "F", galactic: "G", lining: "J", sglass: "K",
    temporal: "M", bastion: "O", casimir: "P", qglass: "Z" },
  reality_diffraction: { deckband: "A", region: "E", phase: "H", alens: "I", lining: "J", sglass: "K", gflens: "L", cpconduit: "N",
    emiso: "Q", qglass: "Z" },
  reality_refab: { region: "E", lining: "J", sglass: "K", gflens: "L", emiso: "Q", hull: "R", tray: "S", claw: "T", gcoil: "V", qglass: "Z" },
};
const HR_SOCKET = {
  homogeny: { control: "~", gas: "h", output: "o", energy: "e", grb: "g" },
  reality_diffraction: { control: "~", bechatch: "b", gas: "h", energy: "e", grb: "g", anchor: null /* per region, below */ },
  reality_refab: { control: "~", bechatch: "b", ibus: "i", obus: "o", energy: "e" },
};
// expected socket counts per build (cumulative)
const HR_SOCKET_COUNT = {
  "homogeny/1": { "~": 1, h: 3, o: 2, e: 2 },
  "homogeny/2": { "~": 1, h: 6, o: 2, e: 2, g: 3 },
  "homogeny/3": { "~": 1, h: 6, o: 2, e: 2, g: 3 },
  "reality_diffraction/diffraction": { "~": 1, b: 1, h: 1, e: 2, g: 6, r: 1, s: 1, t: 1, u: 1, v: 1, w: 1 },
  "reality_refab/refab": { "~": 1, b: 1, i: 2, o: 1, e: 1 },
};
const HR_SOCKET_KINDS = new Set(["control", "gas", "output", "energy", "bechatch", "anchor", "ibus", "obus"]);
// Diffraction anchors: one under each blade root, 2 below the blade's deck band. The blades sit on the bands at
// y -14 / 0 / +14 (drums 1-3 in the doc), two per band, 60 deg on per band: region = 2 * band + (180 deg side).
const DIFF_BLADE_Y = [-14, 0, 14];
function anchorChar(v) {
  const j = DIFF_BLADE_Y.findIndex((y) => y - 2 === v[1]);
  if (j < 0) fail(`diffraction: anchor at voxel ${v} is not 2 below a blade band`);
  const a = Math.atan2(v[2], v[0]) - d2r(60 * j);
  return "rstuvw"[2 * j + (Math.cos(a) > 0 ? 0 : 1)];
}
function classifyHR(machine, key, t) {
  const b = V.build(key, t), kinds = HR_KIND[machine], sk = HR_SOCKET[machine], cells = new Map(), count = {};
  eachCell(b, (x, y, z, kind) => {
    const k = keyOf(x, y, z), v = [x - b.CX, y - b.CY, z - b.CZ];
    let ch;
    if (HR_SOCKET_KINDS.has(kind) || SOCKET_KINDS.has(kind)) {
      if (!(kind in sk)) fail(`${machine} ${t}: unexpected hatch-like cell ${kind} at voxel ${v}`);
      ch = kind === "anchor" ? anchorChar(v) : sk[kind];
      count[ch] = (count[ch] || 0) + 1;
    } else {
      ch = kinds[kind];
      if (!ch) fail(`${machine} ${t}: no char for kind ${kind} at voxel ${v}`);
    }
    cells.set(k, { x, y, z, ch, kind });
  });
  const want = HR_SOCKET_COUNT[`${machine}/${t}`];
  if (JSON.stringify(Object.keys(want).sort().map((c) => c + want[c])) !== JSON.stringify(Object.keys(count).sort().map((c) => c + count[c]))) {
    fail(`${machine} ${t}: socket counts ${JSON.stringify(count)}, expected ${JSON.stringify(want)}`);
  }
  return { b, cells };
}
// The controller's front: the horizontal neighbour, open air in the fullest build, most aligned with the outward
// radial at the controller (voxel origin = the machine's axis). Every piece is turned by quarter turns so that face
// becomes +z, the front of the piece frame (as the Kerr modules).
function hrFront(full, ctrl) {
  const r = [ctrl.x - full.b.CX, 0, ctrl.z - full.b.CZ];
  let best = null;
  for (const o of [[1, 0, 0], [-1, 0, 0], [0, 0, 1], [0, 0, -1]]) {
    if (full.cells.has(keyOf(ctrl.x + o[0], ctrl.y, ctrl.z + o[2]))) continue;
    const s = o[0] * r[0] + o[2] * r[2];
    if (!best || s > best.s) best = { o, s };
  }
  if (!best || best.s <= 0) fail(`controller at grid ${keyOf(ctrl.x, ctrl.y, ctrl.z)} has no open outward face`);
  return best.o;
}
const HR = {
  homogeny: { key: "homog", builds: [1, 2, 3], names: ["homog_t1", "homog_t2", "homog_t3"], mte: 28130 },
  reality_diffraction: { key: "reality", builds: ["diffraction"], names: ["reality_diffraction"], mte: 28131 },
  reality_refab: { key: "reality", builds: ["refab"], names: ["reality_refab"], mte: 28132 },
};
const hrPieces = {}, hrJoins = {}, HR_CTRL = {}, HR_INFO = {};
for (const [machine, m] of Object.entries(HR)) {
  const builds = m.builds.map((t) => classifyHR(machine, m.key, t)), full = builds[builds.length - 1];
  const ctrls = [...builds[0].cells.values()].filter((c) => c.ch === "~");
  if (ctrls.length !== 1) fail(`${machine}: ${ctrls.length} controllers`);
  const ctrl0 = ctrls[0], out0 = hrFront(full, ctrl0), turns = quarterTurns(out0);
  // tiers only add cells; a later tier writing over an earlier cell leaves it with the earlier piece (as the Eye)
  const raw = [[...builds[0].cells.values()]];
  for (let i = 1; i < builds.length; i++) {
    const prev = builds[i - 1].cells, cur = builds[i].cells, added = [];
    let joins = 0;
    for (const [k, c] of prev) { const d = cur.get(k); if (!d) fail(`${machine} T${i + 1} removed a T${i} cell at ${k}`); if (d.ch !== c.ch) joins++; }
    for (const [k, c] of cur) if (!prev.has(k)) added.push(c);
    raw.push(added);
    if (joins) { hrJoins[m.names[i]] = joins; report.notes.push(`${m.names[i]}: ${joins} cells written over an earlier tier in the mockup; the earlier piece keeps them`); }
  }
  m.names.forEach((n, i) => { hrPieces[n] = moduleFrame(raw[i], ctrl0, turns, builds[0].b); });
  const ctrl = hrPieces[m.names[0]].find((c) => c.ch === "~");
  HR_CTRL[machine] = ctrl;
  HR_INFO[machine] = { turns, outVoxel: out0, ctrlVoxel: [ctrl0.x - full.b.CX, ctrl0.y - full.b.CY, ctrl0.z - full.b.CZ],
    // the machine's axis origin (render centre) from the controller, in the controller's (a,b,c) frame
    centreRel: (() => { const [dx, dz] = rotQ(full.b.CX - ctrl0.x, full.b.CZ - ctrl0.z, turns); return abcDelta(dx, full.b.CY - ctrl0.y, dz); })() };
  for (const n of m.names) out[n] = render(n, machine, hrPieces[n], ctrl, builds[0].b);
}

// ------------------------------------------------------------------------------------------------ sanity checks
const checks = [];
function ok(cond, msg) { checks.push((cond ? "ok   " : "FAIL ") + msg); if (!cond) report.failed = true; }

const HR_CORE = new Set(Object.values(HR).map((m) => m.names[0]));
const hrMachineOf = (n) => Object.keys(HR).find((k) => HR[k].names.includes(n));
// exactly one controller anchor per piece: core pieces hold the '~' at off; add-on pieces hold none
for (const [n, r] of Object.entries(out)) {
  const tildes = r.counts["~"] || 0, core = n === "kerr_t1" || n === "kerr_t2" || n === "eye_core" || !!MODULE_CTRL[n] || HR_CORE.has(n);
  if (core) {
    const [a, bv, c] = r.off;
    ok(tildes === 1 && r.grid[c] && r.grid[c][bv] && r.grid[c][bv][a] === "~", `${n}: one controller '~' at off ${r.off}`);
  } else ok(tildes === 0, `${n}: no '~' (anchored by off ${r.off}, outside its own box)`);
}
// the shared anchor: every piece of a machine resolves the controller to the same world cell
for (const [n, r] of Object.entries(out)) {
  const ctrl = MODULE_CTRL[n] || (hrMachineOf(n) ? HR_CTRL[hrMachineOf(n)] : n.startsWith("kerr") ? KERR_CTRL : EYE_CTRL);
  const back = [FLIP_H ? r.bb.mx[0] - r.off[0] : r.bb.mn[0] + r.off[0], r.bb.mx[1] - r.off[1], FLIP_D ? r.bb.mn[2] + r.off[2] : r.bb.mx[2] - r.off[2]];
  ok(back[0] === ctrl.x && back[1] === ctrl.y && back[2] === ctrl.z, `${n}: off resolves to the machine's controller cell`);
}
// a module controller's front face (its outward neighbour) is open air in the fullest build, so a player can reach it
for (const [K, KM, label] of [[K1, KM1, "T1"], [K2, KM2, "T2"]]) for (const m of KERR_MODULES) {
  const c = KM[m.slot], k = keyOf(c.x + m.out[0], c.y + m.out[1], c.z + m.out[2]);
  ok(!K.cells.has(k), `${m.piece}: the controller's front face is open air in the Kerr ${label} build`);
}
// the slot markers sit exactly on the module controllers
for (const m of KERR_MODULES) ok((out.kerr_slots.counts[m.mark] || 0) === 1, `kerr_slots: one '${m.mark}' marker (${m.slot})`);
// pieces of one machine are disjoint, except the alternative cores kerr_t1 / kerr_t2
function disjoint(a, b) { const s = new Set(kerrOrEye(a)); let n = 0; for (const k of kerrOrEye(b)) if (s.has(k)) n++; return n; }
function kerrOrEye(n) { return pieceCells(n).map((c) => keyOf(c.x, c.y, c.z)); }
function pieceCells(n) { return kerrPieces[n] || eyePieces[n] || hrPieces[n]; }
for (const core of ["kerr_t1", "kerr_t2"]) for (const m of ["kerr_compressor", "kerr_extractor"]) ok(disjoint(core, m) === 0, `${core} and ${m} share no cell`);
ok(disjoint("kerr_compressor", "kerr_extractor") === 0, "kerr_compressor and kerr_extractor share no cell");
{ const e = Object.keys(eyePieces); for (let i = 0; i < e.length; i++) for (let j = i + 1; j < e.length; j++) ok(disjoint(e[i], e[j]) === 0, `${e[i]} and ${e[j]} share no cell`); }
{ const e = HR.homogeny.names; for (let i = 0; i < e.length; i++) for (let j = i + 1; j < e.length; j++) ok(disjoint(e[i], e[j]) === 0, `${e[i]} and ${e[j]} share no cell`); }
// a Homogeny / Reality controller's front face (+z in its own frame) is open air in the fullest build
for (const [machine, m] of Object.entries(HR)) {
  const all = new Set(m.names.flatMap((n) => kerrOrEye(n))), c = HR_CTRL[machine];
  ok(!all.has(keyOf(c.x, c.y, c.z + 1)), `${machine}: the controller's front face is open air in the fullest build (turns ${HR_INFO[machine].turns})`);
}

// sizes against the spec's §5.1 table (whole assembled machine, render-only cells excluded)
function asmBox(names) { return bbox(names.flatMap((n) => pieceCells(n))).size.join("x"); }
const SPEC = [
  ["Kerr T1", ["kerr_t1", "kerr_compressor", "kerr_extractor"], "76x65x79"],
  ["Kerr T2", ["kerr_t2", "kerr_compressor", "kerr_extractor"], "92x65x89"],
  ["Eye T1", ["eye_core"], "102x52x80"],
  ["Eye T2", ["eye_core", "eye_beam"], "102x57x80"],
  ["Eye T3", ["eye_core", "eye_beam", "eye_jet"], "102x57x80"],
  ["Eye T4", ["eye_core", "eye_beam", "eye_jet", "eye_arms"], "102x57x137"],
  ["Eye T5", ["eye_core", "eye_beam", "eye_jet", "eye_arms", "eye_arms2"], "135x57x137"],
  ["Eye T6", ["eye_core", "eye_beam", "eye_jet", "eye_arms", "eye_arms2", "eye_rim"], "158x57x149"],
  // homogeny-reality.md (PROPOSED), in each controller's own frame (the Homogeny is turned a quarter, so its W and D
  // swap against the mockup's title)
  ["Homogeny T1", ["homog_t1"], "69x70x73"],
  ["Homogeny T2", ["homog_t1", "homog_t2"], "103x70x113"],
  ["Homogeny T3", ["homog_t1", "homog_t2", "homog_t3"], "145x70x130"],
  ["Reality Diffraction", ["reality_diffraction"], "103x101x85"],
  ["Reality Refab", ["reality_refab"], "104x139x83"],
];
for (const [label, names, want] of SPEC) { const got = asmBox(names); ok(got === want, `${label} assembled box ${got} (W x H x D, spec ${want})`); }

// sockets reachable from outside: a face of every socket touches air that flood-fills to the outside of the box,
// in the fullest build (every piece placed; render-only cells count as air)
function reach(names, label) {
  const cells = names.flatMap((n) => pieceCells(n));
  const bb = bbox(cells), [w, h, d] = bb.size.map((s) => s + 2);
  const solid = new Uint8Array(w * h * d), idx = (x, y, z) => x + y * w + z * w * h;
  for (const c of cells) solid[idx(c.x - bb.mn[0] + 1, c.y - bb.mn[1] + 1, c.z - bb.mn[2] + 1)] = 1;
  const outside = new Uint8Array(w * h * d), q = new Int32Array(w * h * d); let qh = 0, qt = 0;
  q[qt++] = 0; outside[0] = 1;
  while (qh < qt) { const i = q[qh++], x = i % w, y = ((i / w) | 0) % h, z = (i / (w * h)) | 0;
    for (const [dx, dy, dz] of [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]]) {
      const X = x + dx, Y = y + dy, Z = z + dz; if (X < 0 || Y < 0 || Z < 0 || X >= w || Y >= h || Z >= d) continue;
      const j = idx(X, Y, Z); if (solid[j] || outside[j]) continue; outside[j] = 1; q[qt++] = j; } }
  const bad = [];
  for (const c of cells) { if (!/[a-z~]/.test(c.ch)) continue;
    const x = c.x - bb.mn[0] + 1, y = c.y - bb.mn[1] + 1, z = c.z - bb.mn[2] + 1;
    const open = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]].filter(([dx, dy, dz]) => outside[idx(x + dx, y + dy, z + dz)]);
    if (!open.length) bad.push(`${c.ch}@${c.x - K1.b.CX},${c.y},${c.z}`); }
  ok(bad.length === 0, `${label}: every socket has an outside-reachable face${bad.length ? " — sealed: " + bad.join(" ") : ""}`);
}
reach(["kerr_t1", "kerr_compressor", "kerr_extractor"], "Kerr T1 + modules");
reach(["kerr_t2", "kerr_compressor", "kerr_extractor"], "Kerr T2 + modules");
reach(["eye_core"], "Eye T1");
reach(["eye_core", "eye_beam", "eye_jet", "eye_arms"], "Eye T4");
reach(["eye_core", "eye_beam", "eye_jet", "eye_arms", "eye_arms2", "eye_rim"], "Eye T6");
reach(["homog_t1"], "Homogeny T1");
reach(["homog_t1", "homog_t2"], "Homogeny T2");
reach(["homog_t1", "homog_t2", "homog_t3"], "Homogeny T3");
reach(["reality_diffraction"], "Reality Diffraction");
reach(["reality_refab"], "Reality Refab");

// window bands: every piece that should carry one does, and the sightline through it is clear. From the front, at
// rows y +-1 (the controller and the Kerr's hatches stand on y 0), no cell between the viewer and the object's centre
// may be opaque within |x| <= `half`; window cells, Quantum Glass, the BHC's glass and frame boxes count as clear, and
// so do sockets (a hatch is one cell; the Eye's field and sensor sockets stand in its window).
const windowCount = (n) => pieceCells(n).filter((c) => c.was).length;
// open window cells per piece (dropped from the pieces; counted from the classified builds)
const OPENED = {
  kerr_t1: [...K1.cells.values()].filter((c) => c.open && c.piece === "kerr_core").length,
  kerr_t2: [...K2.cells.values()].filter((c) => c.open && (c.piece === "kerr_core" || c.piece === "kerr_density")).length,
  eye_core: [...E[0].cells.values()].filter((c) => c.open).length,
};
["eye_beam", "eye_jet", "eye_arms", "eye_arms2", "eye_rim"].forEach((n, i) => {
  const prev = E[i].cells; OPENED[n] = [...E[i + 1].cells].filter(([k, c]) => c.open && !prev.has(k)).length; });
for (const n of ["kerr_t1", "kerr_t2", "eye_core"]) ok(windowCount(n) > 0, `${n}: ${windowCount(n)} window pane cells, ${OPENED[n]} opened`);
for (const n of ["eye_arms", "eye_arms2", "eye_rim"]) ok(windowCount(n) === 0 && OPENED[n] > 0, `${n}: no pane, ${OPENED[n]} cells opened in front of the core's pane`);
function sightline(names, label, b, half, clear) {
  const cells = new Map(names.flatMap((n) => pieceCells(n)).map((c) => [keyOf(c.x, c.y, c.z), c]));
  const bad = [];
  for (const y of [-1, 1]) for (let x = -half; x <= half; x++) for (let z = 1; z <= 120; z++) {
    const c = cells.get(keyOf(x + b.CX, y + b.CY, z + b.CZ));
    if (c && !c.was && !clear.has(c.ch) && !/[a-z~]/.test(c.ch)) { bad.push(`${c.ch}@${x},${y},${z}`); break; }
  }
  ok(bad.length === 0, `${label}: the front window is clear at y +-1, |x| <= ${half}${bad.length ? " - blocked: " + bad.slice(0, 8).join(" ") : ""}`);
}
sightline(["kerr_t1"], "Kerr T1", K1.b, 8, new Set(["B", "K"]));
sightline(["kerr_t2"], "Kerr T2", K2.b, 8, new Set(["B", "K"]));
for (let t = 1; t <= 6; t++) {
  const names = ["eye_core", "eye_beam", "eye_jet", "eye_arms", "eye_arms2", "eye_rim"].slice(0, t);
  // T1-T3 stars are big (Sol 9, Arcturus 16): the core pane's width; T4-T6 objects are small: the arm / rim openings
  sightline(names, `Eye T${t}`, E[0].b, t <= 3 ? 20 : WINDOW.eye.laterHalf - 2, new Set(["Z"]));
}

// ------------------------------------------------------------------------------------------------ manifest
const ROLE = (machine, ch) => LEGEND[machine][ch];
const manifest = {
  generator: "tools/export-structures.js",
  source: "designs/voxel.js",
  sha: SHA,
  format: "StructureLib native String[d][h] (w chars). Layer 0 = controller front (mockup +z), rows top to bottom, chars left to right seen from the front (mockup +x). off = controller (h,v,d) in the piece's own frame. ' ' = skip.",
  orientation: { flipH: FLIP_H, flipD: FLIP_D, front: "+z in voxel space for both machines (Kerr: governor arc; Eye: plinth at 90 deg)" },
  legend: LEGEND,
  machines: {
    kerr: { controllerVoxel: [KERR_CTRL.x - K1.b.CX, KERR_CTRL.y - K1.b.CY, KERR_CTRL.z - K1.b.CZ],
      pieces: { kerr_t1: "required without the Density Upgrade", kerr_t2: "required with the Density Upgrade (replaces kerr_t1)",
        kerr_slots: "hint only: where each module controller stands" },
      // Module link geometry (kerr-redesign.md §2.2). rel: module controller from the KEE controller, KEE (a,b,c).
      // keeRel: KEE controller from the module controller, module (a,b,c). facingAbc: the module controller's front
      // as a unit vector in the KEE's (a,b,c) frame. turns: quarter turns about +y taking the module's outward face
      // to its own front (informational; the Java side uses facingAbc and the two offsets).
      slots: Object.fromEntries(KERR_MODULES.map((m) => {
        const c = KM1[m.slot], d = [KERR_CTRL.x - c.x, KERR_CTRL.y - c.y, KERR_CTRL.z - c.z], [rx, rz] = rotQ(d[0], d[2], m.turns);
        return [m.slot, { piece: m.piece, mte: m.mte, mark: m.mark, rel: abcDelta(-d[0], -d[1], -d[2]), keeRel: abcDelta(rx, d[1], rz),
          facingAbc: abcDelta(...m.out), turns: m.turns, controllerVoxel: [c.x - K1.b.CX, c.y - K1.b.CY, c.z - K1.b.CZ],
          // where the pre-redesign module core block (slCasing2) stood, from the KEE controller, KEE (a,b,c): migration
          legacyCoreRel: (() => { const l = KM1[m.slot + ".legacy"]; return abcDelta(l.x - KERR_CTRL.x, l.y - KERR_CTRL.y, l.z - KERR_CTRL.z); })() }];
      })),
      // the hole centre (voxel origin) from the KEE controller, KEE (a,b,c): the render block's cell
      centre: { rel: abcDelta(K1.b.CX - KERR_CTRL.x, K1.b.CY - KERR_CTRL.y, K1.b.CZ - KERR_CTRL.z) } },
    kerr_compressor: { mte: 28117, pieces: { kerr_compressor: "the whole machine: the south arc, in its own frame" } },
    kerr_extractor: { mte: 28118, pieces: { kerr_extractor: "the whole machine: the east arc, in its own frame" } },
    eye: { controllerVoxel: [EYE_CTRL.x - E[0].b.CX, EYE_CTRL.y - E[0].b.CY, EYE_CTRL.z - E[0].b.CZ],
      pieces: { eye_core: "T1, required", eye_beam: "T2 beam band, optional",
        eye_jet: "T3 jet crescent, optional; counts only with eye_beam (D-49: bands are cumulative)",
        eye_arms: "T4 spiral arms, optional; counts only with eye_beam + eye_jet",
        eye_arms2: "T5 second arm pair, optional; counts only with every band below",
        eye_rim: "T6 galactic rim with 4 rim stellar sockets, optional; counts only with every band below" } },
    // homogeny-reality.md (PROPOSED). turns: quarter turns about +y taking the controller's outward face (outVoxel) to
    // +z, the piece frame's front. centre.rel: the machine's axis origin (the render centre) from the controller, (a,b,c).
    ...Object.fromEntries(Object.entries(HR).map(([machine, m]) => [machine, { mte: m.mte, controllerVoxel: HR_INFO[machine].ctrlVoxel,
      outVoxel: HR_INFO[machine].outVoxel, turns: HR_INFO[machine].turns, centre: { rel: HR_INFO[machine].centreRel },
      pieces: machine === "homogeny"
        ? { homog_t1: "T1 (UMV): column, helix, rails, volute + throat, 3 feed vessels; required",
          homog_t2: "T2 (UXV) middle stirring ring, 3 more vessels, outer spokes; optional, counts with homog_t1",
          homog_t3: "T3 (MAX) tilted outer ring, clamps and tail; optional, counts only with homog_t2" }
        : { [m.names[0]]: "the whole machine (one tier)" } }])),
  },
  // the window bands (my answer 2026-10-13, render K2 + E-9): which cells, and how many per piece
  windows: { half: WINDOW_HALF,
    kerr: "|y| <= 2 from the hole. Pane (Quantum Glass): the governor arc's outer shell in front of the hole (z > 0, |x| <= 12) and, elsewhere round the drum, the outer mirror ring's casing skin. Open: the arc's lining, both mirror rings' other layers and the T2 mid band in front of the arc",
    eye: "|y| <= 2 from the star, in front of it (z > 0, |x| <= 26). Pane: the core band's outer shell in its front ring segments. Open: the band's lining behind it and the T4/T5 arm and T6 rim cells in front of it",
    accepts: "a pane char takes Quantum Glass OR the block it replaced (legend: window = that old char); an open cell is ' ' (anything), so builds from before the windows still form",
    pane: Object.fromEntries(["kerr_t1", "kerr_t2", "eye_core"].map((n) => [n, windowCount(n)])),
    open: OPENED },
  assembled: Object.fromEntries(SPEC.map(([label, names]) => [label, { pieces: names, size: asmBox(names),
    blocks: names.reduce((s, n) => s + pieceCells(n).length, 0) }])),
  pieces: {},
};
for (const [n, r] of Object.entries(out)) {
  const machine = PIECE_MACHINE[n] || hrMachineOf(n) || "eye";
  const bom = Object.entries(r.counts).sort((a, b) => b[1] - a[1]).map(([ch, count]) => ({ ch, count, role: ROLE(machine, ch).role, block: ROLE(machine, ch).block }));
  const sk = {};
  for (const s of r.sockets) (sk[s.ch] = sk[s.ch] || []).push({ abc: s.abc, rel: s.rel, voxel: s.voxel });
  const socketCounts = Object.fromEntries(Object.entries(sk).map(([ch, l]) => [ch, l.length]));
  manifest.pieces[n] = { machine, file: `structures/${n}.txt`, w: r.w, h: r.h, d: r.d, off: r.off,
    blocks: Object.values(r.counts).reduce((a, b) => a + b, 0), bom, socketCounts, sockets: sk,
    ...(eyeJoins[n] || hrJoins[n] ? { cellsKeptByEarlierPiece: eyeJoins[n] || hrJoins[n] } : {}) };
}
manifest.checks = checks;
if (report.warnings.length) manifest.warnings = report.warnings;
if (report.notes.length) manifest.notes = report.notes;

// ------------------------------------------------------------------------------------------------ orientation test
// A 3x3x3 asymmetric piece for the in-world check (§5.2). The controller '~' sits front-centre (off 1,1,0). Seen
// from in front of the controller the hologram must show: 'R' directly to the viewer's RIGHT of the controller,
// 'U' at the TOP-LEFT front corner, and a row of 'B' along the top of the BACK layer. Map R/U/B to three different
// casings; a mirrored R means --flipH, a B row in front means --flipD.
const ORIENT = [
  "# piece=orient_test w=3 h=3 d=3 off=1,1,0 sha=" + SHA,
  "# expect: R right of the controller, U top-left front corner, BBB along the top of the back layer",
  "U", " ~R", "", "---", "", "", "", "---", "BBB", "", "",
].join("\n") + "\n";

// ------------------------------------------------------------------------------------------------ write / check
const files = {};
for (const [n, r] of Object.entries(out)) files[`${n}.txt`] = r.text;
files["orient_test.txt"] = ORIENT;
files["manifest.json"] = JSON.stringify(manifest, null, 1) + "\n";

checks.forEach((c) => log(c));
report.warnings.forEach((w) => log("warn " + w));
report.notes.forEach((w) => log("note " + w));
for (const [n, r] of Object.entries(out)) log(`${n.padEnd(16)} ${r.w}x${r.h}x${r.d} off=${r.off.join(",")} blocks=${Object.values(r.counts).reduce((a, b) => a + b, 0)}  ${Object.entries(r.counts).sort((a, b) => b[1] - a[1]).map(([c, k]) => c + ":" + k).join(" ")}`);

if (CHECK) {
  let drift = 0;
  for (const [f, text] of Object.entries(files)) {
    const p = path.join(OUT, f);
    const cur = fs.existsSync(p) ? fs.readFileSync(p, "utf8").replace(/\r\n/g, "\n") : null;
    if (cur !== text) { drift++; console.log(`drift: ${f}${cur === null ? " (missing)" : ""}`); }
  }
  if (drift || report.failed) { console.log(`check failed: ${drift} file(s) drifted${report.failed ? ", sanity checks failed" : ""}`); process.exit(1); }
  console.log("check ok: committed structures match voxel.js " + SHA.slice(0, 12));
} else {
  if (report.failed) { console.error("sanity checks failed; nothing written"); process.exit(1); }
  fs.mkdirSync(OUT, { recursive: true });
  for (const [f, text] of Object.entries(files)) fs.writeFileSync(path.join(OUT, f), text);
  log(`wrote ${Object.keys(files).length} files to ${path.relative(ADDON, OUT) || OUT}`);
}
