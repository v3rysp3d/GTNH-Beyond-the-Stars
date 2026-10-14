#!/usr/bin/env node
/*
 * quasar-golden.js — the P4 golden test (quasar-p4.md §1-§5, §8; the B-37 discipline).
 *
 * Runs the P4 PROTOTYPE's own engine under Node's vm: the shared D-49 ENGINE block (tools/fixtures/
 * star-lifecycle-engine.js, one definition for both prototypes) plus the P4 block (the /*P4-START*\/ … /*P4-END*\/
 * block of designs/quasar-p4-prototype.html, copied to tools/fixtures/quasar-engine.js with its sha1): the Quasar
 * Engine's disc (E, Φ, λ, a, M, purge, eruptions, blow-out and re-forming, coil caps), the Einstein-Rosen Conduit's
 * standard rig (mouths, combine, J1/J2/J3, crowns, out 3), the links (Stability / Heavy), the Directive bits, and Eye
 * T7/T8 (galaxy, planets mode, companions, the satellite crossing, the Magnetar Collapse and the Galactic Merger).
 * It writes src/main/resources/assets/beyondthestars/quasar/quasar-golden.json; the Java port
 * (machines/quasar/engine/QuasarSim golden) must reproduce every number bit for bit.
 *
 *   node tools/quasar-golden.js                    regenerate the golden JSON from the fixtures
 *   node tools/quasar-golden.js --java             … then javac (--release 8) the pure packages into
 *                                                  build/quasar-classes and run QuasarSim golden (no gradle)
 *   node tools/quasar-golden.js --check [--java]   regenerate in memory; exit 1 if the committed JSON differs
 *   node tools/quasar-golden.js --refresh-fixture [prototype.html]
 *                                                  re-copy the P4 block (default: the design repo's
 *                                                  designs/quasar-p4-prototype.html); its ENGINE block must equal
 *                                                  tools/fixtures/star-lifecycle-engine.js's (one definition)
 *
 * The harness (mirrored by QuasarSim.runCase): W = newWorld(tier, CFG, QC, K); each second: [autoBegin cases: if the
 * Directive Transition bit 1 was on at the last tick, beginMC / beginGM once], worldTick(W), and if the Eye is DARK
 * call the engine's relight(S) at once (as tools/fieldsim-golden.js does). No npm dependencies; deterministic.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const crypto = require('crypto');
const cp = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const ENGINE_FIXTURE = path.join(__dirname, 'fixtures', 'star-lifecycle-engine.js');
const FIXTURE = path.join(__dirname, 'fixtures', 'quasar-engine.js');
const GOLDEN = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'beyondthestars', 'quasar', 'quasar-golden.json');
const DESIGNS = [path.resolve(ROOT, '..', 'designs'), path.resolve(ROOT, '..', 'GTNH-Addons', 'designs')]
  .find(d => fs.existsSync(d)) || path.resolve(ROOT, '..', 'designs');
const PROTOTYPE = path.join(DESIGNS, 'quasar-p4-prototype.html');
const JAVA = path.join(ROOT, 'src', 'main', 'java', 'com', 'nateyburr', 'beyondthestars', 'machines');
const javaSrc = () => [
  ...fs.readdirSync(path.join(JAVA, 'eye', 'field')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'eye', 'field', f)),
  ...fs.readdirSync(path.join(JAVA, 'quasar', 'engine')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'quasar', 'engine', f)),
  ...['DirectiveSet.java', 'GalaxyDirective.java', 'QuasarDirective.java'].map(f => path.join(JAVA, 'directive', f))];
const CLASSES = path.join(ROOT, 'build', 'quasar-classes');
const E_START = '/*ENGINE-START*/', E_END = '/*ENGINE-END*/', P_START = '/*P4-START*/', P_END = '/*P4-END*/';

const sha1 = s => crypto.createHash('sha1').update(s, 'utf8').digest('hex');
const readLF = f => fs.readFileSync(f, 'utf8').replace(/\r\n/g, '\n');
function block(text, a, b, what) {
  const i = text.indexOf(a), j = text.indexOf(b);
  if (i < 0 || j < i) throw new Error(`no ${a} … ${b} block in ${what}`);
  return text.slice(i, j + b.length);
}

// ---------------------------------------------------------------- fixtures
function refreshFixture(src) {
  const html = readLF(src);
  const p4 = block(html, P_START, P_END, src), eng = block(html, E_START, E_END, src);
  const want = (readLF(ENGINE_FIXTURE).match(/engine-sha1: ([0-9a-f]{40})/) || [])[1];
  if (sha1(eng) !== want) throw new Error(`the P4 prototype's ENGINE block (${sha1(eng)}) differs from ` +
    `tools/fixtures/star-lifecycle-engine.js (${want}): keep one definition (copy the block across first)`);
  const header = [
    '// quasar-engine.js — FIXTURE for tools/quasar-golden.js. Do not edit by hand:',
    '// regenerate with `node tools/quasar-golden.js --refresh-fixture`, then regenerate the golden JSON.',
    '// The P4 block of the quasar play prototype, verbatim (the reference for the Java quasar / conduit / Eye T7-T8',
    '// maths). It runs on top of the shared ENGINE block in tools/fixtures/star-lifecycle-engine.js.',
    '// source: designs/quasar-p4-prototype.html (design repo GTNH-Addons)',
    `// source-sha1: ${sha1(html)}`,
    `// engine-sha1: ${sha1(eng)}`,
    `// p4-sha1: ${sha1(p4)}`,
    ''].join('\n');
  fs.writeFileSync(FIXTURE, header + p4 + '\n');
  console.log(`fixture written: ${path.relative(ROOT, FIXTURE)} (p4-sha1 ${sha1(p4)})`);
}

function loadEngine() {
  const et = readLF(ENGINE_FIXTURE), pt = readLF(FIXTURE);
  const eng = block(et, E_START, E_END, ENGINE_FIXTURE), p4 = block(pt, P_START, P_END, FIXTURE);
  const engSha = (et.match(/engine-sha1: ([0-9a-f]{40})/) || [])[1];
  const p4Sha = (pt.match(/p4-sha1: ([0-9a-f]{40})/) || [])[1];
  const p4Eng = (pt.match(/engine-sha1: ([0-9a-f]{40})/) || [])[1];
  if (engSha !== sha1(eng)) throw new Error('star-lifecycle-engine.js does not match its engine-sha1 (edited by hand?)');
  if (p4Sha !== sha1(p4)) throw new Error('quasar-engine.js does not match its p4-sha1 (edited by hand?)');
  if (p4Eng !== engSha) throw new Error(`quasar-engine.js was cut against engine ${p4Eng}, the engine fixture is ${engSha}`);
  if (fs.existsSync(PROTOTYPE)) {
    const html = readLF(PROTOTYPE);
    const live = sha1(block(html, P_START, P_END, PROTOTYPE)), liveE = sha1(block(html, E_START, E_END, PROTOTYPE));
    if (live !== p4Sha || liveE !== engSha) console.warn(`WARNING: the design repo's P4 prototype (P4 ${live}, ENGINE ${liveE}) ` +
      `differs from the fixtures (P4 ${p4Sha}, ENGINE ${engSha}).\n         Run --refresh-fixture if the prototype is the newer truth, then port the change to the Java.`);
  }
  return { engine: eng + '\n' + p4, engineSha1: engSha, p4Sha1: p4Sha, sourceSha1: (pt.match(/source-sha1: ([0-9a-f]{40})/) || [])[1] };
}

// ---------------------------------------------------------------- cases
// A case: tier, secs, seed, cfg / qc / k / rig overrides on the harness defaults (the local harness.js
// defaults), autoBegin (Transition bit 1 begins the next transition).
function cases() {
  const out = [];
  const add = (name, o) => out.push({ name, spec: Object.assign({ secs: 7200 }, o) });
  for (const seed of [1, 2, 3]) add(`T7 standard 64SM s${seed}`, { tier: 7, seed, secs: 14400 });
  for (const seed of [1, 2, 3]) add(`T8 standard 120SM crossing s${seed}`, { tier: 8, seed, secs: 14400, qc: { M0: 120 } });
  add('T7 never purged (eruptions) s1', { tier: 7, seed: 1, qc: { purgeAt: 0 } });
  add('T7 purge at phi 60 s2', { tier: 7, seed: 2, qc: { purgeAt: 60 } });
  add('T7 purge refused under phi 20 s3', { tier: 7, seed: 3, qc: { purgeAt: 10 } });
  add('T7 quasar unattended f0.85 s1', { tier: 7, seed: 1, qc: { f: 0.85 }, rig: { disc: false } });
  add('blow-out over-Eddington f1.3 s2', { tier: 7, seed: 2, qc: { f: 1.3 }, rig: { disc: false } });
  add('blow-out starved f0.35 then re-form s3', { tier: 7, seed: 3, qc: { f: 0.35 }, rig: { disc: false } });
  add('feed unstocked, gas trims hold E s1', { tier: 7, seed: 1, qc: { stocked: false }, secs: 1800 });
  add('coil cap tier1 RSM 250SM s1', { tier: 7, seed: 1, qc: { coil: 1, b: 1, gas: 'RSM', M0: 250 } });
  add('coil cap tier2 hydrogen b1.5 1020SM s2', { tier: 8, seed: 2, qc: { coil: 2, b: 1.5, gas: 'H', M0: 1020 } });
  add('coil tier4 b4 4000SM RSM s3', { tier: 8, seed: 3, qc: { coil: 4, b: 4, gas: 'RSM', M0: 4000 } });
  add('coil power lost s1', { tier: 7, seed: 1, qc: { power: false }, secs: 1800 });
  add('T7 one jet vented s2', { tier: 7, seed: 2, k: { bot: 'vent' } });
  add('T7 crowns on both jets s3', { tier: 7, seed: 3, k: { top: 'crown', bot: 'crown' }, secs: 1800 });
  add('T7 10% out3 tap s1', { tier: 7, seed: 1, k: { p3: 0.1 } });
  add('T7 conduit fuel short s2', { tier: 7, seed: 2, k: { fuelOK: false }, secs: 1800 });
  add('T7 junctions unwired s3', { tier: 7, seed: 3, rig: { aim: false }, secs: 3600 });
  add('T7 all binary (two-level) s1', { tier: 7, seed: 1, k: { twoLevel: true } });
  add('T8 all binary (two-level) 120SM s2', { tier: 8, seed: 2, qc: { M0: 120 }, k: { twoLevel: true } });
  add('T8 140SM f8 10 s3', { tier: 8, seed: 3, qc: { M0: 140 }, cfg: { f8: 10 } });
  add('T7 planets 64SM s1', { tier: 7, seed: 1, cfg: { mode: 'planets' } });
  add('T8 planets 64SM s2', { tier: 8, seed: 2, cfg: { mode: 'planets' } });
  add('T7 + 1 companion s3', { tier: 7, seed: 3, cfg: { comp: [6] } });
  add('T8 + 2 companions 140SM s1', { tier: 8, seed: 1, qc: { M0: 140 }, cfg: { comp: [6, 4] } });
  add('T8 time dial m1.25 tt9 s2', { tier: 8, seed: 2, qc: { M0: 120 }, cfg: { m: 1.25, tt: 9, ts: 5 } });
  const chain6 = { unlocked7: false, unlocked8: false };
  for (const seed of [1, 2, 3])
    add(`chain T6->T7 auto s${seed}`, { tier: 6, seed, secs: 14400, autoBegin: true, cfg: chain6, k: { c1: 1 } });
  add('chain T6->T7 plain rig shock120 s1', { tier: 6, seed: 1, secs: 10800, autoBegin: true, cfg: Object.assign({ shock: 120 }, chain6), k: { c1: 1 }, rig: { assist: false } });
  for (const seed of [1, 2, 3])
    add(`chain T7->T8 auto 120SM s${seed}`, { tier: 7, seed, secs: 14400, autoBegin: true, qc: { M0: 120 }, k: { test8: true } });
  add('chain T7->T8 all binary 120SM s2', { tier: 7, seed: 2, secs: 14400, autoBegin: true, qc: { M0: 120 }, k: { test8: true, twoLevel: true } });
  return out;
}

// ---------------------------------------------------------------- the harness (mirrored by QuasarSim.runCase)
const HARNESS = `
function __fnv(h,v){ v|=0; for(let i=0;i<4;i++){ h^=(v>>>(8*i))&255; h=Math.imul(h,16777619); } return h; }
function __q6(v){ return Math.round(v*1e6)|0; }
function __sc(st){ return st==='LIT'?0:st==='IGNITING'?1:2; }
function __run(c){
  const CFG=Object.assign({seed:c.seed, auto:true, fine:true, m:1, tt:1, ts:1, r:1, feed:0.694, man:{1:{},2:{},3:{},4:{},5:{},6:{},7:{},8:{}},
    tachRate:100, stBuf:30000, core:true, hsm:true, unlocked4:true, f8:5, unlocked7:true, unlocked8:false, coreMC:true, bufMC:true,
    coreGM:true, bufGM:true, shock:80, mode:'galaxy', comp:[], planets:['mars','titan','bare','io','pluto','dd']}, c.cfg||{});
  const QC=Object.assign({M0:64, coil:3, b:3, gas:'He', f:1, stocked:true, power:true, purgeAt:40, manGas:0, manSpace:0}, c.qc||{});
  const K=Object.assign({top:'mouth', bot:'mouth', throat:1, fuel:'st', fuelOK:true, c1:0.5, k1:2, inv1:true, p3:0, c2:0.5, k2:2,
    c3:0.2, k3:2, win:30, mk:2, stack:false, test8:false, c3t:0.75, twoLevel:false}, c.k||{});
  Object.assign(RIG, {disc:true, aim:true, space:true, assist:true}, c.rig||{});
  const W=newWorld(c.tier, CFG, QC, K), S=W.S, Q=W.Q;
  let he=0x811c9dc5|0, hq=0x811c9dc5|0, hg=0x811c9dc5|0, hd=0x811c9dc5|0, begun=false, beginT=-1, beginKind='';
  for(let i=0;i<c.secs;i++){
    if(c.autoBegin && !begun && (W.dir.trans&1)){ begun=true; beginT=S.t; beginKind=S.tier===6?'mc':'gm'; if(S.tier===6) beginMC(S); else beginGM(S); }
    worldTick(W);
    if(S.state==='DARK') relight(S);
    for(const k of ['P','Th','Om','Pi','W','L']) he=__fnv(he,__q6(S.x[k]||0));
    he=__fnv(he,__q6(S.A)); he=__fnv(he,__q6(S.I)); he=__fnv(he,S.zone); he=__fnv(he,__sc(S.state)); he=__fnv(he,S.tier); he=__fnv(he,S.lock); he=__fnv(he,S.prec);
    for(const v of [Q.E,Q.Phi,Q.a,Q.M,Q.G,Q.I]) hq=__fnv(hq,__q6(v));
    hq=__fnv(hq,Q.zone); hq=__fnv(hq,Q.reform); hq=__fnv(hq,Q.state==='OPEN'?0:1);
    for(const v of [S.g.beta,S.g.gHi,S.g.gLo,S.g.gHalo,S.g.Geye,W.link.mean,W.link8.mean]) hg=__fnv(hg,__q6(v));
    hg=__fnv(hg,W.link.streak); hg=__fnv(hg,W.link8.streak);
    hd=__fnv(hd,W.dir.disc); hd=__fnv(hd,W.dir.field); hd=__fnv(hd,W.dir.event); hd=__fnv(hd,W.dir.trans);
  }
  const L=S.L, res={};
  res.zoneT=Array.from(L.zoneT); res.ySum=L.ySum; res.yN=L.yN; res.fails=L.fails; res.charges=L.charges; res.cycles=L.cycles; res.eu=L.eu;
  res.up=Object.assign({},L.up); res.prod=Object.assign({},L.prod); res.trims=Object.assign({},L.trims); res.runs=Object.assign({},L.runs||{});
  res.x=['P','Th','Om','Pi','W','L'].map(k=>S.x[k]||0); res.A=S.A; res.I=S.I; res.lock=S.lock; res.t=S.t; res.eyeT=S.eyeT;
  res.tier=S.tier; res.state=__sc(S.state); res.prec=S.prec;
  res.crossing=S.tde?{n:S.tde.n, caught:S.tde.caught, left:S.tde.left}:null;
  const QL=Q.L;
  res.quasar={M:Q.M, a:Q.a, E:Q.E, Phi:Q.Phi, I:Q.I, open:Q.state==='OPEN', reform:Q.reform, lock:Q.lock,
    gas:QL.gas, gasTrim:QL.gasTrim, space:QL.space, coilEU:QL.coilEU, zoneT:Array.from(QL.zoneT), ySum:QL.ySum, n:QL.n,
    fails:QL.fails, eruptions:QL.eruptions, purges:QL.purges, bu:QL.bu,
    firstLight:Q.ms.firstLight, edd:Q.ms.edd, eddDone:Q.ms.eddDone, firstErupt:Q.ms.firstErupt};
  res.conduit=Object.assign({},W.KL); res.open=W.Kst.open;
  res.link={streak:W.link.streak, best:W.link.best, done:W.link.done, mean:W.link.mean, frac:W.link.frac, n:W.link.n};
  res.link8={streak:W.link8.streak, best:W.link8.best, done:W.link8.done, mean:W.link8.mean, frac:W.link8.frac, n:W.link8.n};
  res.unlocked7=!!CFG.unlocked7; res.unlocked8=!!CFG.unlocked8; res.coreMC=!!CFG.coreMC; res.coreGM=!!CFG.coreGM;
  res.trans=S.trans?{kind:S.trans.kind, phase:S.trans.phase, st:S.trans.st, streak:S.trans.streak}:null;
  res.beginT=beginT; res.beginKind=beginKind;
  res.hash={eye:he>>>0, quasar:hq>>>0, bursts:hg>>>0, directive:hd>>>0};
  return res;
}
__out = __cases.map(c => __run(Object.assign({}, c)));
`;

function runPrototype(engine, list) {
  const ctx = vm.createContext({ __cases: list.map(c => c.spec), __out: null });
  vm.runInContext(engine + '\n' + HARNESS, ctx, { filename: 'quasar-engine.js' });
  return JSON.parse(JSON.stringify(ctx.__out));
}

// ---------------------------------------------------------------- informational summary
function summary(list, results) {
  for (let i = 0; i < list.length; i++) {
    const r = results[i], n = r.zoneT.reduce((a, b) => a + b, 0) || 1, q = r.quasar;
    console.log(`  ${list[i].name.padEnd(42)} T${r.tier} fails ${String(r.fails).padStart(3)}  zones ${r.zoneT.map(v => Math.round(100 * v / n)).join('/')}` +
      `  Y ${(r.ySum / Math.max(1, r.yN)).toFixed(2)}  quasar M ${q.M.toFixed(1)} blow-outs ${q.fails} erupt ${q.eruptions} purge ${q.purges}` +
      `${r.unlocked7 && list[i].spec.tier === 6 ? '  T7 UNLOCKED' : ''}${r.unlocked8 ? '  T8 UNLOCKED' : ''}`);
  }
}

// ---------------------------------------------------------------- java
function runJava() {
  fs.mkdirSync(CLASSES, { recursive: true });
  const jc = cp.spawnSync('javac', ['-Xlint:-options', '--release', '8', '-encoding', 'UTF-8', '-d', CLASSES, ...javaSrc()], { stdio: 'inherit' });
  if (jc.status !== 0) { console.error('javac failed'); return 1; }
  const j = cp.spawnSync('java', ['-cp', CLASSES, 'com.nateyburr.beyondthestars.machines.quasar.engine.QuasarSim', 'golden', GOLDEN], { stdio: 'inherit' });
  return j.status === null ? 1 : j.status;
}

// ---------------------------------------------------------------- main
function main() {
  const args = process.argv.slice(2);
  if (args[0] === '--refresh-fixture') { refreshFixture(args[1] ? path.resolve(args[1]) : PROTOTYPE); return 0; }
  const check = args.includes('--check'), java = args.includes('--java');
  const { engine, engineSha1, p4Sha1, sourceSha1 } = loadEngine();
  const list = cases();
  const t0 = Date.now();
  const results = runPrototype(engine, list);
  const doc = {
    about: 'P4 golden results (quasar disc, conduit rig, links, Directive bits, Eye T7/T8) from the prototype engine ' +
      '(tools/quasar-golden.js). QuasarSim golden must match every number exactly. Regenerate; do not edit.',
    engineSha1, p4Sha1, sourceSha1,
    cases: list.map((c, i) => ({ name: c.name, spec: c.spec, expect: results[i] })),
  };
  const text = JSON.stringify(doc, null, 1) + '\n';
  console.log(`${list.length} cases run on the prototype engine in ${Date.now() - t0} ms`);
  summary(list, results);
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
