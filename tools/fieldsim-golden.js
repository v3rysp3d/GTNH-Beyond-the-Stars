#!/usr/bin/env node
/*
 * fieldsim-golden.js — the D-49 stability-field golden test (d49-build-plan.md §15.1).
 *
 * Runs the PROTOTYPE's own engine (the /*ENGINE-START*\/ … /*ENGINE-END*\/ block of
 * designs/star-lifecycle-prototype.html, copied to tools/fixtures/star-lifecycle-engine.js with its sha1) under
 * Node's vm, on scripted cases for every tier, and writes the results to
 * src/main/resources/assets/beyondthestars/eye/fieldsim-golden.json. The Java port
 * (machines/eye/field/, FieldSim golden) must reproduce every number bit for bit: counts, ledger totals, the final
 * state, and an FNV-1a hash of round(x·10⁶) for P, Θ, Ω, Π, A, I plus zone, state, tier and lock every second.
 *
 *   node tools/fieldsim-golden.js                    regenerate the golden JSON from the fixture
 *   node tools/fieldsim-golden.js --java             … then javac (--release 8) the field package into
 *                                                    build/fieldsim-classes and run FieldSim golden (no gradle)
 *   node tools/fieldsim-golden.js --check [--java]   regenerate in memory; exit 1 if the committed JSON differs
 *   node tools/fieldsim-golden.js --refresh-fixture [prototype.html]
 *                                                    re-copy the engine block from the prototype (default: the
 *                                                    design repo's designs/star-lifecycle-prototype.html)
 *
 * No npm dependencies. Output is deterministic (seeded; no timestamps).
 *
 * The harness (the same in FieldSim.runCase): S = newState(tier, cfg); each second: [collapse cases: begin the Core
 * Collapse at the first t ≥ beginAt where the prototype's canBegin() holds], step(S), and if the star is DARK call
 * relight(S) at once (fail() already counted the failure). The rig is the prototype's regulator: off = no trims,
 * bang = fine:false, fine = fine:true. The prototype's bounce (+90) is patched to a variable so the Core Collapse
 * runs at both 90 and the adopted 150 (plan §1.2).
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const crypto = require('crypto');
const cp = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const FIXTURE = path.join(__dirname, 'fixtures', 'star-lifecycle-engine.js');
const GOLDEN = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'beyondthestars', 'eye', 'fieldsim-golden.json');
const PROTOTYPE = path.resolve(ROOT, '..', 'designs', 'star-lifecycle-prototype.html');
const FIELD_SRC = path.join(ROOT, 'src', 'main', 'java', 'com', 'nateyburr', 'beyondthestars', 'machines', 'eye', 'field');
const CLASSES = path.join(ROOT, 'build', 'fieldsim-classes');
const START = '/*ENGINE-START*/', END = '/*ENGINE-END*/';

const sha1 = s => crypto.createHash('sha1').update(s, 'utf8').digest('hex');
function extractBlock(text, what) {
  const a = text.indexOf(START), b = text.indexOf(END);
  if (a < 0 || b < a) throw new Error(`no ${START} … ${END} block in ${what}`);
  return text.slice(a, b + END.length);
}

// ---------------------------------------------------------------- fixture
function refreshFixture(src) {
  const html = fs.readFileSync(src, 'utf8');
  const block = extractBlock(html, src);
  const header = [
    '// star-lifecycle-engine.js — FIXTURE for tools/fieldsim-golden.js. Do not edit by hand:',
    '// regenerate with `node tools/fieldsim-golden.js --refresh-fixture`, then regenerate the golden JSON.',
    '// The ENGINE block of the D-49 play prototype, verbatim (the reference for the Java field maths).',
    '// source: designs/star-lifecycle-prototype.html (design repo GTNH-Addons)',
    `// source-sha1: ${sha1(html)}`,
    `// engine-sha1: ${sha1(block)}`,
    ''].join('\n');
  fs.mkdirSync(path.dirname(FIXTURE), { recursive: true });
  fs.writeFileSync(FIXTURE, header + block + '\n');
  console.log(`fixture written: ${path.relative(ROOT, FIXTURE)} (engine-sha1 ${sha1(block)})`);
}

function loadEngine() {
  const text = fs.readFileSync(FIXTURE, 'utf8');
  const block = extractBlock(text, FIXTURE);
  const want = (text.match(/engine-sha1: ([0-9a-f]{40})/) || [])[1];
  const sourceSha1 = (text.match(/source-sha1: ([0-9a-f]{40})/) || [])[1];
  if (want !== sha1(block)) throw new Error(`fixture engine block does not match its engine-sha1 (edited by hand?)`);
  if (fs.existsSync(PROTOTYPE)) {
    const live = sha1(extractBlock(fs.readFileSync(PROTOTYPE, 'utf8'), PROTOTYPE));
    if (live !== want) console.warn(`WARNING: the design repo's prototype engine (${live}) differs from the fixture (${want}).\n` +
      '         Run --refresh-fixture if the prototype is the newer truth, then port the change to the Java.');
  }
  const needle = 'shock=90-s';
  if (block.split(needle).length !== 2) throw new Error(`expected exactly one "${needle}" in the engine (the bounce)`);
  return { engine: block.replace(needle, 'shock=BOUNCE_SHOCK-s'), engineSha1: want, sourceSha1 };
}

// ---------------------------------------------------------------- cases
function cases() {
  const out = [];
  const base = { m: 1, tt: 1, ts: 1, r: 1, feed: 0.694, secs: 14400 };
  const RIGS = ['off', 'bang', 'fine'], SEEDS = [1806, 7, 424242];
  for (let tier = 1; tier <= 6; tier++)
    for (const rig of RIGS)
      for (const seed of SEEDS) out.push({ name: `T${tier} ${rig} ${seed}`, spec: { tier, rig, seed, ...base } });
  for (const rig of RIGS) {
    out.push({ name: `T4 feed1.5 ${rig} 1806`, spec: { tier: 4, rig, seed: 1806, ...base, feed: 1.5 } });
    // m 2.25 needs Time Acceleration tier 9 at T4 (m_max = 1 + 0.25·(tt − 4)); tt 6 would cap it at 1.5
    out.push({ name: `T4 m2.25 tt9 ts1 ${rig} 1806`, spec: { tier: 4, rig, seed: 1806, ...base, m: 2.25, tt: 9, ts: 1 } });
    out.push({ name: `T4 m2.25 tt9 ts9 ${rig} 1806`, spec: { tier: 4, rig, seed: 1806, ...base, m: 2.25, tt: 9, ts: 9 } });
    out.push({ name: `T6 r0.8 ${rig} 1806`, spec: { tier: 6, rig, seed: 1806, ...base, r: 0.8 } });
  }
  for (const rig of ['fine', 'bang'])
    for (const stBuf of [0, 30000, 60000])
      for (const shock of [90, 150])
        out.push({
          name: `collapse ${rig} st${stBuf} shock${shock} 1806`,
          spec: { tier: 3, rig, seed: 1806, ...base, secs: 3600, collapse: { beginAt: 600, stBuf, shock, tachRate: 100 } },
        });
  return out;
}

// ---------------------------------------------------------------- the harness (mirrored by FieldSim.runCase)
const HARNESS = `
function __fnv(h,v){ v|=0; for(let i=0;i<4;i++){ h^=(v>>>(8*i))&255; h=Math.imul(h,16777619); } return h; }
function __q6(v){ return Math.round(v*1e6)|0; }
function __sc(st){ return st==='LIT'?0:st==='IGNITING'?1:2; }
function __canBegin(S){ const C=S.cfg; return S.tier===3 && S.state==='LIT' && !S.trans && S.zone>=0 && S.zone<=1 && C.core && C.hsm && C.tachRate>0; }
function __run(c){
  const col=c.collapse||null;
  const cfg={seed:c.seed, auto:c.rig!=='off', fine:c.rig==='fine', m:c.m, tt:c.tt, ts:c.ts, r:c.r, feed:c.feed,
    man:{1:{},2:{},3:{},4:{},5:{},6:{}}, tachRate:col?col.tachRate:100, stBuf:col?col.stBuf:0, core:true, hsm:true, unlocked4:false};
  BOUNCE_SHOCK = col ? col.shock : 90;
  const S=newState(c.tier,cfg);
  let h=0x811c9dc5|0, tr=null, beginT=-1, endT=-1;
  for(let i=0;i<c.secs;i++){
    if(col && !tr && S.t>=col.beginAt && __canBegin(S)){ beginCollapse(S); tr=S.trans; beginT=S.t; }
    step(S);
    if(S.state==='DARK') relight(S);
    if(tr && endT<0 && S.trans!==tr) endT=S.t;
    h=__fnv(h,__q6(S.x.P)); h=__fnv(h,__q6(S.x.Th)); h=__fnv(h,__q6(S.x.Om)); h=__fnv(h,__q6(S.x.Pi));
    h=__fnv(h,__q6(S.A)); h=__fnv(h,__q6(S.I));
    h=__fnv(h,S.zone); h=__fnv(h,__sc(S.state)); h=__fnv(h,S.tier); h=__fnv(h,S.lock);
  }
  const L=S.L, res={};
  res.zoneT=Array.from(L.zoneT); res.ySum=L.ySum; res.yN=L.yN; res.meanY=L.yN>0?L.ySum/L.yN:0;
  res.fails=L.fails; res.charges=L.charges; res.cycles=L.cycles; res.batches=L.batches; res.eu=L.eu; res.tach=L.tach;
  res.up=Object.assign({},L.up); res.prod=Object.assign({},L.prod); res.trims=Object.assign({},L.trims);
  res.x=[S.x.P,S.x.Th,S.x.Om,S.x.Pi]; res.A=S.A; res.I=S.I; res.lock=S.lock; res.t=S.t; res.eyeT=S.eyeT;
  res.tier=S.tier; res.state=__sc(S.state); res.hash=h>>>0;
  if(col){
    const o={beginT, endT};
    let outcome='none';
    if(tr){
      if(cfg.unlocked4) outcome='settled';
      else if(tr.shock && tr.phase===1) outcome='supernova';
      else if(tr.phase===3 && S.trans!==tr) outcome='failed';
      else if(S.trans!==tr) outcome='lost';
      else outcome='running';
      o.phase=tr.phase; o.tach=tr.tach; o.Mc=tr.Mc; o.st=tr.st; o.streak=tr.streak;
      o.shock=tr.shock?{P0:tr.shock.P0, T0:tr.shock.T0, P:tr.shock.P, Th:tr.shock.Th, s:tr.shock.s}:null;
    }
    o.outcome=outcome;
    res.collapse=o;
  }
  return res;
}
__out = __cases.map(c => __run(Object.assign({}, c)));
`;

function runPrototype(engine, list) {
  const ctx = vm.createContext({ __cases: list.map(c => c.spec), __out: null });
  vm.runInContext('var BOUNCE_SHOCK = 90;\n' + engine + '\n' + HARNESS, ctx, { filename: 'star-lifecycle-engine.js' });
  return JSON.parse(JSON.stringify(ctx.__out)); // plain objects, own-context prototypes dropped
}

// ---------------------------------------------------------------- sanity floor (plan §15.1), informational
function sanity(list, results) {
  const by = {};
  list.forEach((c, i) => { const m = c.name.match(/^T(\d) (off|bang|fine) \d+$/); if (m) (by[`T${m[1]} ${m[2]}`] ||= []).push(results[i]); });
  const rows = [], warn = [];
  for (const k of Object.keys(by)) {
    const rs = by[k], fails = rs.reduce((a, r) => a + r.fails, 0), n = rs.reduce((a, r) => a + r.yN, 0);
    const res = rs.reduce((a, r) => a + r.zoneT[0], 0) / n, stab = rs.reduce((a, r) => a + r.zoneT[0] + r.zoneT[1], 0) / n;
    const y = rs.reduce((a, r) => a + r.ySum, 0) / n, batches = rs.reduce((a, r) => a + r.batches, 0);
    rows.push(`${k.padEnd(9)} fails ${String(fails).padStart(5)}  Resonant ${(100 * res).toFixed(1).padStart(5)} %  ≥Stable ${(100 * stab).toFixed(1).padStart(5)} %  Y ${y.toFixed(3)}${k.startsWith('T5') ? `  batches ${batches}` : ''}`);
    const [tier, rig] = k.split(' ');
    if (tier === 'T1' && rig === 'off' && (fails !== 0 || stab < 0.99)) warn.push(`${k}: expected 0 failures and ≥ 99 % Stable`);
    if (rig === 'fine' && fails !== 0) warn.push(`${k}: expected 0 failures${tier === 'T5' ? ' / fractures' : ''}`);
    if (rig === 'off' && ['T2', 'T3', 'T4', 'T6'].includes(tier) && fails / rs.length <= 100) warn.push(`${k}: expected > 100 failures per run`);
    if (rig === 'off' && tier === 'T5' && batches !== 0) warn.push(`${k}: expected every batch to fracture`);
  }
  console.log('sanity floor (3 seeds × 4 h per row):\n  ' + rows.join('\n  '));
  list.forEach((c, i) => { if (results[i].collapse) console.log(`  ${c.name.padEnd(36)} ${results[i].collapse.outcome}`); });
  if (warn.length) console.log('SANITY WARNINGS:\n  ' + warn.join('\n  '));
  else console.log('sanity floor: OK');
}

// ---------------------------------------------------------------- java
function runJava() {
  const srcs = fs.readdirSync(FIELD_SRC).filter(f => f.endsWith('.java')).map(f => path.join(FIELD_SRC, f));
  fs.mkdirSync(CLASSES, { recursive: true });
  const jc = cp.spawnSync('javac', ['-Xlint:-options', '--release', '8', '-encoding', 'UTF-8', '-d', CLASSES, ...srcs], { stdio: 'inherit' });
  if (jc.status !== 0) { console.error('javac failed'); return 1; }
  const j = cp.spawnSync('java', ['-cp', CLASSES, 'com.nateyburr.beyondthestars.machines.eye.field.FieldSim', 'golden', GOLDEN], { stdio: 'inherit' });
  return j.status === null ? 1 : j.status;
}

// ---------------------------------------------------------------- main
function main() {
  const args = process.argv.slice(2);
  if (args[0] === '--refresh-fixture') { refreshFixture(args[1] ? path.resolve(args[1]) : PROTOTYPE); return 0; }
  const check = args.includes('--check'), java = args.includes('--java');
  const { engine, engineSha1, sourceSha1 } = loadEngine();
  const list = cases();
  const t0 = Date.now();
  const results = runPrototype(engine, list);
  const doc = {
    about: 'D-49 stability field golden results from the prototype engine (tools/fieldsim-golden.js). ' +
      'FieldSim golden must match every number exactly. Regenerate; do not edit.',
    engineSha1, sourceSha1,
    cases: list.map((c, i) => ({ name: c.name, spec: c.spec, expect: results[i] })),
  };
  const text = JSON.stringify(doc, null, 1) + '\n';
  console.log(`${list.length} cases run on the prototype engine in ${Date.now() - t0} ms`);
  sanity(list, results);
  if (check) {
    const old = fs.existsSync(GOLDEN) ? fs.readFileSync(GOLDEN, 'utf8') : '';
    if (old !== text) { console.error(`CHECK FAILED: ${path.relative(ROOT, GOLDEN)} differs from a fresh run`); return 1; }
    console.log(`check: ${path.relative(ROOT, GOLDEN)} is up to date`);
  } else {
    fs.mkdirSync(path.dirname(GOLDEN), { recursive: true });
    fs.writeFileSync(GOLDEN, text);
    console.log(`written: ${path.relative(ROOT, GOLDEN)} (${(text.length / 1024).toFixed(0)} KiB)`);
  }
  return java ? runJava() : 0;
}
process.exitCode = main();
