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
const VOXEL = path.resolve(positional[0] || path.join(ADDON, "..", "designs", "voxel.js"));
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
  // 3. The Eye's six pylon foot pads (deck, written inside the "ground" tag) get their own tag.
  body = splice(body, "for (let dx=-3;dx<=3;dx++) for (let dz=-3;dz<=3;dz++) set(foot[0]+dx, GROUND+1, foot[2]+dz, B.deck);",
    `tag("pad"); `, "before");
  body = splice(body, "set(foot[0]+dx, GROUND+1, foot[2]+dz, B.deck);", ` tag("ground");`, "after");
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
    J: { role: "arc_lining", kind: "lining", block: "Casings.BackgroundRadiationAbsorbentCasing (FC-1: Harmonic Lining not registered)" },
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
    c: { role: "compressor_io", kind: "beamhatch/energy/maint on the compressor arc", block: "InputBus / InputHatch / OutputBus / OutputHatch (standard lists, mark-and-truncate), else fallback", fallback: "I", min: "1 in + 1 out", hint: 4 },
    q: { role: "compressor_core", kind: "control on the compressor arc", block: "NEW slCasing2:0 Kerr Compressor Module (exactly 1, no fallback)", min: 1 },
    x: { role: "extractor_io", kind: "output on the extractor arc", block: "as c, else fallback", fallback: "N", min: "1 in + 1 out", hint: 5 },
    k: { role: "extractor_core", kind: "output at 340 deg", block: "NEW slCasing2:1 Kerr Extractor Module (exactly 1, no fallback)", min: 1 },
  },
  eye: {
    "~": { role: "controller", kind: "control", block: "Eye of Harmony controller (MTE 28115)" },
    U: { role: "band_shell", kind: "eyeshell (core band, rails, pylons)", block: "Casings.ReinforcedSpatialStructureCasing (TTCasingsContainer.sBlockCasingsBA0:11)" },
    Q: { role: "plinth", kind: "deck", block: "Casings.ReinforcedTemporalStructureCasing (BA0:10)" },
    J: { role: "band_lining", kind: "lining", block: "Casings.InfiniteSpacetimeEnergyBoundaryCasing (BA0:12)" },
    Z: { role: "glass_channel", kind: "sglass", block: "Casings.QuantumGlass (TecTech)" },
    W: { role: "compression_field", kind: "eyeshell clamps, upper rail -> core (T1)", block: "TTCasingsContainer.SpacetimeCompressionFieldGenerators metas 0-8, ofBlocksTiered (as TecTech's EOH)", tiered: true },
    X: { role: "dilation_field", kind: "eyeshell clamps, core -> lower rail (T1)", block: "TTCasingsContainer.TimeAccelerationFieldGenerator metas 0-8, ofBlocksTiered", tiered: true },
    Y: { role: "stabilisation_field", kind: "deck, the six pylon foot pads", block: "TTCasingsContainer.StabilisationFieldGenerators metas 0-8, ofBlocksTiered", tiered: true },
    R: { role: "beam_band", kind: "ring2", block: "NEW slCasing:10 Beam Band Casing" },
    S: { role: "jet_crescent", kind: "ring3", block: "NEW slCasing:11 Jet Crescent Casing" },
    T: { role: "galactic_shell", kind: "galactic", block: "NEW slCasing:12 Galactic Shell Plating" },
    h: { role: "gas_socket", kind: "gas", block: "EyeHatchElement.Gas (MTEHatchInput, private -> H/He buffers), else fallback", fallback: "Q", min: 1, hint: 1 },
    o: { role: "me_output_socket", kind: "energy (plinth)", block: "EyeHatchElement.Output (MTEHatchOutputBusME, MTEHatchOutputME), else fallback", fallback: "Q", min: "1 bus + 1 hatch", hint: 2 },
    m: { role: "maintenance_socket", kind: "maint", block: "HatchElement.Maintenance, else fallback", fallback: "Q", min: 1, hint: 3 },
    s: { role: "stellar_socket_beam", kind: "beamhatch on the beam band", block: "EyeHatchElement.Stellar (MTEHatchInput, private -> stellar buffers), else fallback", fallback: "R", min: 0, hint: 4 },
    t: { role: "stellar_socket_jet", kind: "beamhatch on the jet crescent", block: "EyeHatchElement.Stellar, else fallback", fallback: "S", min: 0, hint: 4 },
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
  { at: [0, -2, -39.6], kind: "control", ch: "q" },
  { at: polar(32.8, 334, 0), kind: "output", ch: "x" },
  { at: polar(32.8, 340, 0), kind: "output", ch: "k" },
  { at: polar(32.8, 346, 0), kind: "output", ch: "x" },
];
// Eye: the plinth ring through the hatches is ellipse (46+4.6, 35+4.6) at y = GROUND = -40.
const EYE_PF = (a) => [50.6 * Math.cos(d2r(a)), -40, 39.6 * Math.sin(d2r(a))];
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
  return { b, cells };
}

// ------------------------------------------------------------------------------------------------ build pieces
const report = { warnings: [], notes: [] };

const K1 = classifyKerr(1), K2 = classifyKerr(2);
if (K1.b.CX !== K2.b.CX || K1.b.W !== K2.b.W) fail("kerr T1/T2 worlds differ in size");
const pick = (cells, pred) => [...cells.values()].filter(pred);
const kerrPieces = {
  kerr_t1: pick(K1.cells, (c) => c.piece === "kerr_core"),
  kerr_t2: pick(K2.cells, (c) => c.piece === "kerr_core" || c.piece === "kerr_density"),
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

const E = [1, 2, 3, 4].map(classifyEye);
const eyePieces = {}, eyeJoins = {}, overwrites = {};
{
  const names = ["eye_core", "eye_beam", "eye_jet", "eye_arms"];
  eyePieces.eye_core = [...E[0].cells.values()];
  for (let t = 2; t <= 4; t++) {
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
    for (const [k, c] of cur) if (!prev.has(k)) added.push(c);
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
      voxel: [c.x - b.CX, c.y - b.CY, c.z - b.CZ] };
  }).sort((p, q) => (p.ch < q.ch ? -1 : p.ch > q.ch ? 1 : p.abc[2] - q.abc[2] || p.abc[1] - q.abc[1] || p.abc[0] - q.abc[0]));
  const lines = [`# piece=${name} w=${w} h=${h} d=${d} off=${off.join(",")} sha=${SHA}`,
    `# machine=${machine} cells=${cells.length} generated by tools/export-structures.js from designs/voxel.js; do not edit`];
  grid.forEach((layer, i) => { if (i) lines.push("---"); for (const row of layer) lines.push(row.join("").replace(/\s+$/, "")); });
  return { text: lines.join("\n") + "\n", w, h, d, off, counts, sockets, bb, grid };
}

const out = {};
for (const [n, cells] of Object.entries(kerrPieces)) out[n] = render(n, "kerr", cells, KERR_CTRL, K1.b);
for (const [n, cells] of Object.entries(eyePieces)) out[n] = render(n, "eye", cells, EYE_CTRL, E[0].b);

// ------------------------------------------------------------------------------------------------ sanity checks
const checks = [];
function ok(cond, msg) { checks.push((cond ? "ok   " : "FAIL ") + msg); if (!cond) report.failed = true; }

// exactly one controller anchor per piece: core pieces hold the '~' at off; add-on pieces hold none
for (const [n, r] of Object.entries(out)) {
  const tildes = r.counts["~"] || 0, core = n === "kerr_t1" || n === "kerr_t2" || n === "eye_core";
  if (core) {
    const [a, bv, c] = r.off;
    ok(tildes === 1 && r.grid[c] && r.grid[c][bv] && r.grid[c][bv][a] === "~", `${n}: one controller '~' at off ${r.off}`);
  } else ok(tildes === 0, `${n}: no '~' (anchored by off ${r.off}, outside its own box)`);
}
// the shared anchor: every piece of a machine resolves the controller to the same world cell
for (const [n, r] of Object.entries(out)) {
  const ctrl = n.startsWith("kerr") ? KERR_CTRL : EYE_CTRL;
  const back = [FLIP_H ? r.bb.mx[0] - r.off[0] : r.bb.mn[0] + r.off[0], r.bb.mx[1] - r.off[1], FLIP_D ? r.bb.mn[2] + r.off[2] : r.bb.mx[2] - r.off[2]];
  ok(back[0] === ctrl.x && back[1] === ctrl.y && back[2] === ctrl.z, `${n}: off resolves to the machine's controller cell`);
}
// pieces of one machine are disjoint, except the alternative cores kerr_t1 / kerr_t2
function disjoint(a, b) { const s = new Set(kerrOrEye(a)); let n = 0; for (const k of kerrOrEye(b)) if (s.has(k)) n++; return n; }
function kerrOrEye(n) { return (kerrPieces[n] || eyePieces[n]).map((c) => keyOf(c.x, c.y, c.z)); }
for (const core of ["kerr_t1", "kerr_t2"]) for (const m of ["kerr_compressor", "kerr_extractor"]) ok(disjoint(core, m) === 0, `${core} and ${m} share no cell`);
ok(disjoint("kerr_compressor", "kerr_extractor") === 0, "kerr_compressor and kerr_extractor share no cell");
{ const e = Object.keys(eyePieces); for (let i = 0; i < e.length; i++) for (let j = i + 1; j < e.length; j++) ok(disjoint(e[i], e[j]) === 0, `${e[i]} and ${e[j]} share no cell`); }

// sizes against the spec's §5.1 table (whole assembled machine, render-only cells excluded)
function asmBox(names) { return bbox(names.flatMap((n) => kerrPieces[n] || eyePieces[n])).size.join("x"); }
const SPEC = [
  ["Kerr T1", ["kerr_t1", "kerr_compressor", "kerr_extractor"], "76x65x79"],
  ["Kerr T2", ["kerr_t2", "kerr_compressor", "kerr_extractor"], "92x65x89"],
  ["Eye T1", ["eye_core"], "102x52x80"],
  ["Eye T2", ["eye_core", "eye_beam"], "102x57x80"],
  ["Eye T3", ["eye_core", "eye_beam", "eye_jet"], "102x57x80"],
  ["Eye T4", ["eye_core", "eye_beam", "eye_jet", "eye_arms"], "102x57x137"],
];
for (const [label, names, want] of SPEC) { const got = asmBox(names); ok(got === want, `${label} assembled box ${got} (W x H x D, spec ${want})`); }

// sockets reachable from outside: a face of every socket touches air that flood-fills to the outside of the box,
// in the fullest build (every piece placed; render-only cells count as air)
function reach(names, label) {
  const cells = names.flatMap((n) => kerrPieces[n] || eyePieces[n]);
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
        kerr_compressor: "optional checked module", kerr_extractor: "optional checked module" } },
    eye: { controllerVoxel: [EYE_CTRL.x - E[0].b.CX, EYE_CTRL.y - E[0].b.CY, EYE_CTRL.z - E[0].b.CZ],
      pieces: { eye_core: "T1, required", eye_beam: "T2 band, optional", eye_jet: "T3 band, optional, independent of eye_beam",
        eye_arms: "T4 arms, optional (plus the Galactic Upgrade Core)" } },
  },
  assembled: Object.fromEntries(SPEC.map(([label, names]) => [label, { pieces: names, size: asmBox(names),
    blocks: names.reduce((s, n) => s + (kerrPieces[n] || eyePieces[n]).length, 0) }])),
  pieces: {},
};
for (const [n, r] of Object.entries(out)) {
  const machine = n.startsWith("kerr") ? "kerr" : "eye";
  const bom = Object.entries(r.counts).sort((a, b) => b[1] - a[1]).map(([ch, count]) => ({ ch, count, role: ROLE(machine, ch).role, block: ROLE(machine, ch).block }));
  const sk = {};
  for (const s of r.sockets) (sk[s.ch] = sk[s.ch] || []).push({ abc: s.abc, rel: s.rel, voxel: s.voxel });
  const socketCounts = Object.fromEntries(Object.entries(sk).map(([ch, l]) => [ch, l.length]));
  manifest.pieces[n] = { machine, file: `structures/${n}.txt`, w: r.w, h: r.h, d: r.d, off: r.off,
    blocks: Object.values(r.counts).reduce((a, b) => a + b, 0), bom, socketCounts, sockets: sk,
    ...(eyeJoins[n] ? { cellsKeptByEarlierPiece: eyeJoins[n] } : {}) };
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
