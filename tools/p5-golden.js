#!/usr/bin/env node
/*
 * p5-golden.js — the P5 golden test (designs/docs/p5-t9-t10.md §2, §3, §6; the B-37 discipline).
 *
 * Runs the P5 PROTOTYPE's own engine under Node's vm: the shared D-49 ENGINE block (tools/fixtures/
 * star-lifecycle-engine.js) with P5's one changed line (§9.7: the per-second extra-drift record gains V:0, H:0), the P4
 * block (tools/fixtures/quasar-engine.js) and the P5 block (the /*P5-START*\/ … /*P5-END*\/ block of
 * designs/p5-prototype.html, copied to tools/fixtures/p5-engine.js with its sha1): Eye T9 the Great Attractor (gauge
 * V, the pellet beam, Galactic Flow, galaxy companions, planets mode 9), the Deep Link, The Infall, Eye T10 the
 * Universe (gauge H, the universe batch, Recombination, Cosmic Dawn, the collapse salvage) and Inflation. It writes
 * src/main/resources/assets/beyondthestars/eye/p5-golden.json; the Java port (QuasarSim golden-p5) must reproduce
 * every number bit for bit. The T1-T8 goldens (fieldsim-golden.js, quasar-golden.js) are separate and unchanged.
 *
 *   node tools/p5-golden.js                    regenerate the golden JSON from the fixtures
 *   node tools/p5-golden.js --java             … then javac (--release 8) the pure packages and run QuasarSim golden-p5
 *   node tools/p5-golden.js --check [--java]   regenerate in memory; exit 1 if the committed JSON differs
 *   node tools/p5-golden.js --refresh-fixture [p5-prototype.html]
 *                                              re-copy the P5 block (default: the design repo's designs/p5-prototype.html);
 *                                              its ENGINE and P4 blocks must equal the fixtures' (plus the one engine line)
 *
 * The two universeTier fixes of B-169 are in the design repo's prototype itself since the P5 merge (the fixture was
 * refreshed from it; nothing is patched in memory any more):
 *   1. universeTier's d sets S.q (as attractorTier's does). Without it a T10 started at T10 read S.q undefined, so
 *      Recombination's P drop and Cosmic Dawn's P kick turned into NaN, which the engine's (d[k]||0) silently drops
 *      (the whole P drift of those seconds). An Eye that reached T10 through Inflation always had S.q set.
 *   2. universeTier's perSec puts the countdown (Recombination or Cosmic Dawn ahead) on S.warn. The d's S.warn = 1 was
 *      overwritten by step's own S.warn = warn later in the same second, so the Universe set's bit 3 and the event
 *      channel never showed the countdown.
 *
 * The harness (mirrored by QuasarSim.runCaseP5) is the local p5/harness.js: setP5Tiers(CFG); W = newWorld(tier, CFG,
 * QC, K); setP5Tiers(CFG); S.x.V = S.x.H = 0; each second: [beginInfall / beginInflation at their second], worldTick(W),
 * and if the Eye is DARK and relight is on, relight(S). No npm dependencies; deterministic.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const crypto = require('crypto');
const cp = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const ENGINE_FIXTURE = path.join(__dirname, 'fixtures', 'star-lifecycle-engine.js');
const P4_FIXTURE = path.join(__dirname, 'fixtures', 'quasar-engine.js');
const FIXTURE = path.join(__dirname, 'fixtures', 'p5-engine.js');
const GOLDEN = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'beyondthestars', 'eye', 'p5-golden.json');
const DESIGNS = [path.resolve(ROOT, '..', 'designs'), path.resolve(ROOT, '..', 'GTNH-Addons', 'designs')]
  .find(d => fs.existsSync(d)) || path.resolve(ROOT, '..', 'designs');
const PROTOTYPE = path.join(DESIGNS, 'p5-prototype.html');
const JAVA = path.join(ROOT, 'src', 'main', 'java', 'com', 'nateyburr', 'beyondthestars', 'machines');
const javaSrc = () => [
  ...fs.readdirSync(path.join(JAVA, 'eye', 'field')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'eye', 'field', f)),
  ...fs.readdirSync(path.join(JAVA, 'quasar', 'engine')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'quasar', 'engine', f)),
  ...['DirectiveSet.java', 'GalaxyDirective.java', 'QuasarDirective.java', 'P5Directive.java'].map(f => path.join(JAVA, 'directive', f))];
const CLASSES = path.join(ROOT, 'build', 'p5-classes');
const E_START = '/*ENGINE-START*/', E_END = '/*ENGINE-END*/', P4_START = '/*P4-START*/', P4_END = '/*P4-END*/';
const P5_START = '/*P5-START*/', P5_END = '/*P5-END*/';
const DP_OLD = 'const dp={P:-5*(1-r), Th:0, Om:0, Pi:0, W:0, L:0};';
const DP_NEW = 'const dp={P:-5*(1-r), Th:0, Om:0, Pi:0, W:0, L:0, V:0, H:0};';

const sha1 = s => crypto.createHash('sha1').update(s, 'utf8').digest('hex');
const readLF = f => fs.readFileSync(f, 'utf8').replace(/\r\n/g, '\n');
function block(text, a, b, what) {
  const i = text.indexOf(a), j = text.indexOf(b);
  if (i < 0 || j < i) throw new Error(`no ${a} … ${b} block in ${what}`);
  return text.slice(i, j + b.length);
}
function once(text, needle, repl, what) {
  if (text.split(needle).length !== 2) throw new Error(`expected exactly one ${JSON.stringify(needle)} in ${what}`);
  return text.replace(needle, repl);
}
// the engine as P5 runs it: the shared block with its one changed line (p5-t9-t10.md §9.7; the prototype marks it with
// a trailing comment, which the comparison strips)
function p5Engine(eng) { return once(eng, DP_OLD, DP_NEW, 'the ENGINE block (the dp record)'); }
const stripDpComment = s => s.replace(DP_NEW + '   // P5: the one changed engine line (designs/docs/p5-t9-t10.md §9.7)', DP_NEW);

// ---------------------------------------------------------------- fixtures
function refreshFixture(src) {
  const html = readLF(src);
  const p5 = block(html, P5_START, P5_END, src), p4 = block(html, P4_START, P4_END, src);
  const eng = stripDpComment(block(html, E_START, E_END, src));
  const et = readLF(ENGINE_FIXTURE), pt = readLF(P4_FIXTURE);
  const fixEng = p5Engine(block(et, E_START, E_END, ENGINE_FIXTURE)), fixP4 = block(pt, P4_START, P4_END, P4_FIXTURE);
  if (sha1(eng) !== sha1(fixEng)) throw new Error('the P5 prototype\'s ENGINE block differs from tools/fixtures/star-lifecycle-engine.js ' +
    'beyond the one dp line: keep one definition (refresh the engine fixture first)');
  if (sha1(p4) !== sha1(fixP4)) throw new Error('the P5 prototype\'s P4 block differs from tools/fixtures/quasar-engine.js: refresh that first');
  const header = [
    '// p5-engine.js — FIXTURE for tools/p5-golden.js. Do not edit by hand:',
    '// regenerate with `node tools/p5-golden.js --refresh-fixture`, then regenerate the golden JSON.',
    '// The P5 block of the P5 play prototype, verbatim (the reference for the Java Eye T9 / T10 maths). It runs on top',
    '// of the shared ENGINE block (with P5\'s one changed line) and the P4 block (tools/fixtures/quasar-engine.js).',
    '// source: designs/p5-prototype.html (design repo GTNH-Addons)',
    `// source-sha1: ${sha1(html)}`,
    `// engine-sha1: ${(et.match(/engine-sha1: ([0-9a-f]{40})/) || [])[1]}`,
    `// p4-sha1: ${sha1(p4)}`,
    `// p5-sha1: ${sha1(p5)}`,
    ''].join('\n');
  fs.writeFileSync(FIXTURE, header + p5 + '\n');
  console.log(`fixture written: ${path.relative(ROOT, FIXTURE)} (p5-sha1 ${sha1(p5)})`);
}

function loadEngine() {
  const et = readLF(ENGINE_FIXTURE), pt = readLF(P4_FIXTURE), ft = readLF(FIXTURE);
  const eng = block(et, E_START, E_END, ENGINE_FIXTURE), p4 = block(pt, P4_START, P4_END, P4_FIXTURE), p5 = block(ft, P5_START, P5_END, FIXTURE);
  const engSha = (et.match(/engine-sha1: ([0-9a-f]{40})/) || [])[1];
  const want = k => (ft.match(new RegExp(k + ': ([0-9a-f]{40})')) || [])[1];
  if (engSha !== sha1(eng)) throw new Error('star-lifecycle-engine.js does not match its engine-sha1 (edited by hand?)');
  if (want('p5-sha1') !== sha1(p5)) throw new Error('p5-engine.js does not match its p5-sha1 (edited by hand?)');
  if (want('p4-sha1') !== sha1(p4)) throw new Error('p5-engine.js was cut against another P4 block than tools/fixtures/quasar-engine.js');
  if (want('engine-sha1') !== engSha) throw new Error('p5-engine.js was cut against another ENGINE block');
  if (fs.existsSync(PROTOTYPE)) {
    const html = readLF(PROTOTYPE), live = sha1(block(html, P5_START, P5_END, PROTOTYPE));
    if (live !== want('p5-sha1')) console.warn(`WARNING: the design repo's P5 prototype (P5 ${live}) differs from the fixture ` +
      `(${want('p5-sha1')}).\n         Run --refresh-fixture if the prototype is the newer truth, then port the change to the Java.`);
  }
  return { engine: p5Engine(eng) + '\n' + p4 + '\n' + p5, engineSha1: engSha, p4Sha1: want('p4-sha1'), p5Sha1: want('p5-sha1'),
    sourceSha1: want('source-sha1') };
}

// ---------------------------------------------------------------- cases
// A case: tier, secs, seed, cfg / qc / k / rig / urig overrides on the harness defaults (the local p5/harness.js),
// infallAt / inflationAt (begin that transition before that second's tick), relight (default true).
function cases() {
  const out = [];
  const add = (name, o) => out.push({ name, spec: Object.assign({ secs: 14400, relight: true }, o) });
  for (const seed of [1, 2, 3]) add(`T9 standard 300SM s${seed}`, { tier: 9, seed });
  add('T9 250SM s1', { tier: 9, seed: 1, qc: { M0: 250 } });
  add('T9 200SM (relights) s2', { tier: 9, seed: 2, qc: { M0: 200 }, secs: 7200 });
  add('T9 two-level beam s3', { tier: 9, seed: 3, cfg: { beamTwoLevel: true } });
  add('T9 all binary s1', { tier: 9, seed: 1, cfg: { beamTwoLevel: true }, k: { twoLevel: true } });
  add('T9 beam unwired s2', { tier: 9, seed: 2, cfg: { beamRig: false, bman: 7.5 }, secs: 7200 });
  add('T9 Ergomatter Mk I rating 0 s3', { tier: 9, seed: 3, cfg: { grade: 'erg', inj: 1 }, secs: 7200 });
  add('T9 Ergomatter Mk I accept 12 s1', { tier: 9, seed: 1, cfg: { grade: 'erg', inj: 1, acceptMax: 12 } });
  add('T9 Magnetar Mk III s2', { tier: 9, seed: 2, cfg: { grade: 'mag', inj: 3 }, secs: 7200 });
  add('T9 Ergomatter Mk II s3', { tier: 9, seed: 3, cfg: { grade: 'erg', inj: 2 }, secs: 7200 });
  add('T9 quasar unattended f0.85 s3', { tier: 9, seed: 3, qc: { f: 0.85 } });
  add('T9 + SGR 1806-20 s1', { tier: 9, seed: 1, cfg: { comp9: [6] } });
  add('T9 + 3 companions 500SM s2', { tier: 9, seed: 2, cfg: { comp9: [6, 7, 8] }, qc: { M0: 500 } });
  add('T9 + 3 companions 300SM s3', { tier: 9, seed: 3, cfg: { comp9: [6, 7, 8] }, secs: 7200 });
  add('T9 planets 120SM s1', { tier: 9, seed: 1, cfg: { mode: 'planets' }, qc: { M0: 120 }, secs: 7200 });
  add('T9 planets 64SM throat1 s2', { tier: 9, seed: 2, cfg: { mode: 'planets' }, qc: { M0: 64 }, k: { throat: 1 }, secs: 7200 });
  add('T9 pellets run out s1', { tier: 9, seed: 1, cfg: { pelletsOK: false }, secs: 1800, relight: false });
  add('T9 pre-fire s2', { tier: 9, seed: 2, cfg: { preFire: true }, secs: 7200 });
  add('T9 upkeep r0.8 s3', { tier: 9, seed: 3, cfg: { r: 0.8 }, secs: 7200 });
  for (const M of [250, 300]) for (const seed of [1, 2]) add(`Deep Link ${M}SM s${seed}`, { tier: 8, seed, qc: { M0: M }, k: { test9: true }, secs: 7200 });
  add('Deep Link 200SM never s3', { tier: 8, seed: 3, qc: { M0: 200 }, k: { test9: true }, secs: 3600 });
  for (const seed of [1, 2, 3]) add(`Infall 300SM s${seed}`, { tier: 8, seed, infallAt: 120, secs: 4000, relight: false });
  add('Infall beam unwired (soft fail) s1', { tier: 8, seed: 1, infallAt: 120, cfg: { beamRig: false, bman: 7.5 }, secs: 4000, relight: false });
  add('Infall Ergomatter Mk I (soft fail) s2', { tier: 8, seed: 2, infallAt: 120, cfg: { grade: 'erg', inj: 1 }, secs: 4000, relight: false });
  add('Infall 200SM s3', { tier: 8, seed: 3, infallAt: 120, qc: { M0: 200 }, secs: 4000, relight: false });
  add('Infall after the Deep Link 300SM s1', { tier: 8, seed: 1, k: { test9: true }, infallAt: 2400, secs: 6000, relight: false });
  const U = { qc: { M0: 600, coil: 4 }, k: { throat: 2 }, relight: false };
  for (const seed of [1, 2, 3]) add(`T10 standard 600SM s${seed}`, Object.assign({ tier: 10, seed }, U));
  add('T10 no H rig s1', Object.assign({ tier: 10, seed: 1, urig: { on: false }, secs: 7200 }, U));
  add('T10 400SM s2', Object.assign({ tier: 10, seed: 2, secs: 7200 }, U, { qc: { M0: 400, coil: 4 } }));
  add('T10 stabilisation 9 s3', Object.assign({ tier: 10, seed: 3, cfg: { ts: 9 }, secs: 7200 }, U));
  add('T10 valves 4 s1', Object.assign({ tier: 10, seed: 1, urig: { dm: 4, st: 4 }, secs: 7200 }, U));
  for (const seed of [1, 2, 3]) add(`Inflation 600SM s${seed}`, Object.assign({ tier: 9, seed, inflationAt: 300, secs: 2400 }, U));
  add('Inflation shock 95 s1', Object.assign({ tier: 9, seed: 1, inflationAt: 300, secs: 2400, cfg: { infShock: 95 } }, U));
  add('Inflation no H rig (soft fail) s2', Object.assign({ tier: 9, seed: 2, inflationAt: 300, secs: 2400, urig: { on: false } }, U));
  add('Inflation then a whole universe s3', Object.assign({ tier: 9, seed: 3, inflationAt: 300, secs: 9000 }, U));
  return out;
}

// ---------------------------------------------------------------- the harness (mirrored by QuasarSim.runCaseP5)
const HARNESS = `
function __fnv(h,v){ v|=0; for(let i=0;i<4;i++){ h^=(v>>>(8*i))&255; h=Math.imul(h,16777619); } return h; }
function __q6(v){ return Math.round(v*1e6)|0; }
function __sc(st){ return st==='LIT'?0:st==='IGNITING'?1:2; }
function __run(c){
  const CFG=Object.assign({seed:c.seed, auto:true, fine:true, m:1, tt:1, ts:1, r:1, feed:0.694, man:{1:{},2:{},3:{},4:{},5:{},6:{},7:{},8:{},9:{},10:{}},
    tachRate:100, stBuf:30000, core:true, hsm:true, unlocked4:true, f8:5, unlocked7:true, unlocked8:true, coreMC:true, bufMC:true,
    coreGM:true, bufGM:true, shock:120, mode:'galaxy', comp:[], comp9:[], planets:['mars','titan','bare','io','pluto','dd','moon','barc','dd'],
    grade:'sup', inj:1, bcAuto:true, bc:0.125, bk:1, bwin:30, beamRig:true, beamTwoLevel:false, acceptMax:null, pelletsOK:true, preFire:false, bman:8}, c.cfg||{});
  const QC=Object.assign({M0:300, coil:3, b:3, gas:'He', f:1, stocked:true, power:true, purgeAt:40, manGas:0, manSpace:0}, c.qc||{});
  const K=Object.assign({top:'mouth', bot:'mouth', throat:2, fuel:'erg', fuelOK:true, c1:0.5, k1:2, inv1:true, p3:0, c2:0.5, k2:2,
    c3:0.2, k3:2, win:30, mk:3, stack:false, test8:false, c3t:0.75, twoLevel:false, test9:false, c4t:0.75}, c.k||{});
  Object.assign(RIG, {disc:true, aim:true, space:true, assist:true}, c.rig||{});
  Object.assign(U_RIG, {on:true, dm:2, st:2}, c.urig||{});
  if(CFG.acceptMax==null) CFG.acceptMax=holdRating(CFG);
  setP5Tiers(CFG); const W=newWorld(c.tier, CFG, QC, K); setP5Tiers(CFG); const S=W.S, Q=W.Q; S.x.V=0; S.x.H=0;
  let he=0x811c9dc5|0, hq=0x811c9dc5|0, hg=0x811c9dc5|0, hd=0x811c9dc5|0, hb=0x811c9dc5|0;
  for(let i=0;i<c.secs;i++){
    if(c.infallAt===i) beginInfall(S);
    if(c.inflationAt===i) beginInflation(S);
    worldTick(W);
    if(S.state==='DARK' && c.relight) relight(S);
    for(const k of ['P','Th','Om','Pi','W','L','V','H']) he=__fnv(he,__q6(S.x[k]||0));
    he=__fnv(he,__q6(S.A)); he=__fnv(he,__q6(S.I)); he=__fnv(he,S.zone); he=__fnv(he,__sc(S.state)); he=__fnv(he,S.tier); he=__fnv(he,S.lock); he=__fnv(he,S.prec);
    for(const v of [Q.E,Q.Phi,Q.a,Q.M,Q.G,Q.I]) hq=__fnv(hq,__q6(v));
    hq=__fnv(hq,Q.zone); hq=__fnv(hq,Q.reform); hq=__fnv(hq,Q.state==='OPEN'?0:1);
    const dl=W.link9d||{};
    for(const v of [S.g.beta,S.g.gHi,S.g.gLo,S.g.gHalo,S.g.Geye,W.link.mean,W.link8.mean,dl.mean||0,dl.rho||0]) hg=__fnv(hg,__q6(v));
    hg=__fnv(hg,W.link.streak); hg=__fnv(hg,W.link8.streak); hg=__fnv(hg,dl.streak||0);
    hd=__fnv(hd,W.dir.disc); hd=__fnv(hd,W.dir.field); hd=__fnv(hd,W.dir.event); hd=__fnv(hd,W.dir.trans); hd=__fnv(hd,W.dir.flow); hd=__fnv(hd,W.dir.uni);
    const B=S.beam, F=S.flow, U=S.uni;
    hb=__fnv(hb,__q6(B?B.B:0)); hb=__fnv(hb,__q6(B?B.n:0));
    hb=__fnv(hb,F?__q6(F.next):-1); hb=__fnv(hb,F?F.left:-1); hb=__fnv(hb,F?F.size:-1); hb=__fnv(hb,F?F.sign:0); hb=__fnv(hb,F?F.ok:-1);
    hb=__fnv(hb,F?(F.warned?1:0)|(F.accepted?2:0)|(F.forced?4:0):-1);
    hb=__fnv(hb,U?U.t:-1); hb=__fnv(hb,U?U.dawn:-1); hb=__fnv(hb,U?(U.dawnOk?1:0):-1);
  }
  const L=S.L, res={};
  res.zoneT=Array.from(L.zoneT); res.ySum=L.ySum; res.yN=L.yN; res.fails=L.fails; res.charges=L.charges; res.cycles=L.cycles; res.eu=L.eu;
  res.up=Object.assign({},L.up); res.prod=Object.assign({},L.prod); res.trims=Object.assign({},L.trims); res.runs=Object.assign({},L.runs||{});
  res.gal=L.gal||0; res.flows=L.flows||0; res.caught=L.caught||0; res.pellets=L.pellets||0; res.batches=L.batches; res.dawns=L.dawns||0;
  res.x=['P','Th','Om','Pi','W','L','V','H'].map(k=>S.x[k]||0); res.A=S.A; res.I=S.I; res.lock=S.lock; res.t=S.t; res.eyeT=S.eyeT;
  res.tier=S.tier; res.state=__sc(S.state); res.prec=S.prec;
  res.unlocked9=!!CFG.unlocked9; res.unlocked10=!!CFG.unlocked10;
  res.trans=S.trans?{kind:S.trans.kind, phase:S.trans.phase, st:S.trans.st, streak:S.trans.streak}:null;
  res.beam=S.beam?{B:S.beam.B, rho:S.beam.rho, n:S.beam.n, s:S.beam.s}:null;
  res.flow=S.flow?{next:S.flow.next, left:S.flow.left, tau:S.flow.tau, size:S.flow.size, sign:S.flow.sign, ok:S.flow.ok,
    warned:S.flow.warned, accepted:S.flow.accepted, forced:S.flow.forced}:null;
  res.uni=S.uni?{t:S.uni.t, ySum:S.uni.ySum, n:S.uni.n, rMin:S.uni.rMin, dawn:S.uni.dawn, dawnOk:S.uni.dawnOk}:null;
  const dl=W.link9d;
  res.link9={streak:dl.streak, best:dl.best, done:dl.done, mean:dl.mean, frac:dl.frac, n:dl.n, rho:dl.rho};
  res.quasar={M:Q.M, a:Q.a, E:Q.E, Phi:Q.Phi, I:Q.I, open:Q.state==='OPEN', fails:Q.L.fails, eruptions:Q.L.eruptions, purges:Q.L.purges, bu:Q.L.bu};
  res.conduit=Object.assign({},W.KL);
  res.hash={eye:he>>>0, quasar:hq>>>0, bursts:hg>>>0, directive:hd>>>0, p5:hb>>>0};
  return res;
}
__out = __cases.map(c => __run(Object.assign({}, c)));
`;

function runPrototype(engine, list) {
  const ctx = vm.createContext({ __cases: list.map(c => c.spec), __out: null });
  vm.runInContext(engine + '\n' + HARNESS, ctx, { filename: 'p5-engine.js' });
  return JSON.parse(JSON.stringify(ctx.__out));
}

function summary(list, results) {
  for (let i = 0; i < list.length; i++) {
    const r = results[i], n = r.zoneT.reduce((a, b) => a + b, 0) || 1, h = list[i].spec.secs / 3600;
    const prod = Object.entries(r.prod).filter(([k]) => /Dark Matter|Universium/.test(k)).map(([k, v]) => `${k} ${Math.round(v / h)}/h`).join(' ');
    console.log(`  ${list[i].name.padEnd(40)} T${r.tier} fails ${String(r.fails).padStart(3)}  zones ${r.zoneT.map(v => Math.round(100 * v / n)).join('/')}` +
      `  Y ${(r.ySum / Math.max(1, r.yN)).toFixed(2)}  ${prod}  flows ${r.caught}/${r.flows} gal ${r.gal} pellets ${Math.round(r.pellets)}` +
      `${r.unlocked9 && list[i].spec.tier === 8 ? '  T9 UNLOCKED' : ''}${r.unlocked10 ? '  T10 UNLOCKED' : ''}${r.link9.done ? '  DEEP LINK' : ''}`);
  }
}

function runJava() {
  fs.mkdirSync(CLASSES, { recursive: true });
  const jc = cp.spawnSync('javac', ['-Xlint:-options', '--release', '8', '-encoding', 'UTF-8', '-d', CLASSES, ...javaSrc()], { stdio: 'inherit' });
  if (jc.status !== 0) { console.error('javac failed'); return 1; }
  const j = cp.spawnSync('java', ['-cp', CLASSES, 'com.nateyburr.beyondthestars.machines.quasar.engine.QuasarSim', 'golden-p5', GOLDEN], { stdio: 'inherit' });
  return j.status === null ? 1 : j.status;
}

function main() {
  const args = process.argv.slice(2);
  if (args[0] === '--refresh-fixture') { refreshFixture(args[1] ? path.resolve(args[1]) : PROTOTYPE); return 0; }
  const check = args.includes('--check'), java = args.includes('--java');
  const { engine, engineSha1, p4Sha1, p5Sha1, sourceSha1 } = loadEngine();
  const list = cases();
  const t0 = Date.now();
  const results = runPrototype(engine, list);
  const doc = {
    about: 'P5 golden results (Eye T9 the Great Attractor, the Deep Link, The Infall, Eye T10 the Universe, Inflation) from the ' +
      'prototype engine (the B-169 fixes are in the prototype). QuasarSim golden-p5 must match every number exactly. Regenerate; do not edit.',
    engineSha1, p4Sha1, p5Sha1, sourceSha1,
    cases: list.map((c, i) => ({ name: c.name, spec: c.spec, expect: results[i] })),
  };
  const text = JSON.stringify(doc, null, 1) + '\n';
  console.log(`${list.length} cases run on the prototype engine in ${Date.now() - t0} ms`);
  summary(list, results);
  if (check) {
    const old = fs.existsSync(GOLDEN) ? fs.readFileSync(GOLDEN, 'utf8').replace(/\r\n/g, '\n') : '';
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
