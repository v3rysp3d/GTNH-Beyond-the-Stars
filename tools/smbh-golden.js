#!/usr/bin/env node
/*
 * smbh-golden.js — the SMBH v1 golden test (designs/docs/smbh-v1.md §2-§5, §9, §10.1; the B-37 discipline).
 *
 * Runs the SMBH reference block (tools/fixtures/smbh-engine.js) on top of the shared ENGINE block and the P4 block
 * (tools/fixtures/star-lifecycle-engine.js, tools/fixtures/quasar-engine.js) under Node's vm, and:
 *   1. cross-checks that its disc second (smbhDiscStep) IS the P4 prototype's quasarStep, number for number, every
 *      second of every lattice-0 case at or under the coil cap with no Overdrive (the "bit-identical below 4,096 SM"
 *      proof on the reference side; tools/quasar-golden.js --java proves it on the Java side);
 *   2. checks the spec's tables (§2.3 growth times, §3.2 Overdrive, §5.2 Unbound, §9, the per-SM intake) against the
 *      numbers printed in smbh-v1.md, within the printed rounding;
 *   3. writes src/main/resources/assets/beyondthestars/quasar/smbh-golden.json (the cases' results and the tables);
 *      the Java port (machines/quasar/engine/SmbhSim golden) must reproduce every number bit for bit.
 *
 *   node tools/smbh-golden.js                    regenerate the golden JSON
 *   node tools/smbh-golden.js --java             … then javac (--release 8) the pure packages into
 *                                                build/quasar-classes and run SmbhSim golden (no gradle)
 *   node tools/smbh-golden.js --check [--java]   regenerate in memory; exit 1 if the committed JSON differs
 *
 * The harness (mirrored by SmbhSim.runCase), each second: apply the case's events for this second; Ṁ_Edd; the disc's
 * draw f·Ṁ_Edd (0 unstocked); the disc rig (E < −5: 10 gas-trim units, E > +5: 10 Space units) and the purge
 * regulator (Φ last second ≥ purgeAt), as the P4 prototype's; the anchor supply (a fixed L/s, on hand before the
 * plan); smbhPlan; the other supplies (intake = F·Ṁ_Edd in total, Overdrive fluids = od / uni × the need);
 * smbhDiscStep; horizonStep. No npm dependencies.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const cp = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const ENGINE_FIXTURE = path.join(__dirname, 'fixtures', 'star-lifecycle-engine.js');
const P4_FIXTURE = path.join(__dirname, 'fixtures', 'quasar-engine.js');
const SMBH_FIXTURE = path.join(__dirname, 'fixtures', 'smbh-engine.js');
const GOLDEN = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'beyondthestars', 'quasar', 'smbh-golden.json');
const JAVA = path.join(ROOT, 'src', 'main', 'java', 'com', 'nateyburr', 'beyondthestars', 'machines');
const javaSrc = () => [
  ...fs.readdirSync(path.join(JAVA, 'eye', 'field')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'eye', 'field', f)),
  ...fs.readdirSync(path.join(JAVA, 'quasar', 'engine')).filter(f => f.endsWith('.java')).map(f => path.join(JAVA, 'quasar', 'engine', f)),
  ...['DirectiveSet.java', 'GalaxyDirective.java', 'QuasarDirective.java'].map(f => path.join(JAVA, 'directive', f))];
const CLASSES = path.join(ROOT, 'build', 'quasar-classes');

const readLF = f => fs.readFileSync(f, 'utf8').replace(/\r\n/g, '\n');
function block(text, a, b, what) {
  const i = text.indexOf(a), j = text.indexOf(b);
  if (i < 0 || j < i) throw new Error(`no ${a} … ${b} block in ${what}`);
  return text.slice(i, j + b.length);
}
function loadEngine() {
  return [block(readLF(ENGINE_FIXTURE), '/*ENGINE-START*/', '/*ENGINE-END*/', ENGINE_FIXTURE),
    block(readLF(P4_FIXTURE), '/*P4-START*/', '/*P4-END*/', P4_FIXTURE),
    block(readLF(SMBH_FIXTURE), '/*SMBH-START*/', '/*SMBH-END*/', SMBH_FIXTURE)].join('\n');
}

// ---------------------------------------------------------------- cases
// qc: the quasar and its supplies (defaults below); events: [{t, qc}] changes at the start of second t;
// rig false: no disc trims and no purge regulator (an unattended disc).
const L4TOP = 67108864;
function cases() {
  const out = [];
  const add = (name, o) => out.push({ name, spec: Object.assign({ secs: 3600, seed: 1 }, o) });
  // no lattice: today's quasar through the SMBH path (the MTE runs this every second)
  add('L0 64SM coil3 He s1', { seed: 1, secs: 7200, qc: { M0: 64, coil: 3 } });
  add('L0 1020SM coil2 H b1.5 s2', { seed: 2, qc: { M0: 1020, coil: 2, b: 1.5, gas: 'H' } });
  add('L0 4000SM coil4 b4 RSM s3 (to the cap)', { seed: 3, secs: 7200, qc: { M0: 4000, coil: 4, b: 4, gas: 'RSM' } });
  add('L0 intake and Overdrive ignored without a lattice s1', { seed: 1, qc: { M0: 4096, coil: 4, intake: 5, overdrive: 1 } });
  add('L4 on coil3 counts as none s2', { seed: 2, qc: { M0: 2000, coil: 3, lattice: 4, intake: 5 } });
  // the max-quasar fixes: a hole above its structure's top sheds to it (2 %/s, no power)
  add('coil downgrade 4096SM on coil2 sheds to 1024 s3', { seed: 3, secs: 600, qc: { M0: 4096, coil: 2 } });
  add('lattice downgrade 1M SM on L1 sheds to 65536 s1', { seed: 1, secs: 600, qc: { M0: 1048576, lattice: 1, intake: 4, setpoint: 65536 } });
  add('L1 default setpoint is the safe limit: 30000 sheds to 16384 s2', { seed: 2, secs: 120, qc: { M0: 30000, lattice: 1, intake: 2 } });
  // L1: growth by the intake, hold-up, leak
  add('L1 from 4096 He at the 5x cap s1', { seed: 1, secs: 7200, qc: { M0: 4096, lattice: 1, intake: 5, setpoint: 65536 } });
  add('L1 from 4096 He disc only (F 1) s2', { seed: 2, secs: 7200, qc: { M0: 4096, lattice: 1, intake: 1, setpoint: 65536 } });
  add('L1 x0.5 held at F 2.5 s3', { seed: 3, qc: { M0: 32768, lattice: 1, intake: 2.5, setpoint: 32768 } });
  add('L1 top under-fed F 3 leaks s1', { seed: 1, secs: 7200, qc: { M0: 65536, lattice: 1, intake: 3, setpoint: 65536 } });
  add('L1 top held F 4 s2', { seed: 2, qc: { M0: 65536, lattice: 1, intake: 4, setpoint: 65536 } });
  // QA-B09: a one-second intake dip at the setpoint; the recovery second draws only what reaches the cap
  add('L1 top, a one-second dip at 100 s s1', { seed: 1, secs: 300, qc: { M0: 65536, lattice: 1, intake: 5, setpoint: 65536 }, events: [{ t: 100, qc: { intake: 3.9 } }, { t: 101, qc: { intake: 5 } }] });
  add('L1 top, the feed cut at 600 s s3', { seed: 3, qc: { M0: 65536, lattice: 1, intake: 4, setpoint: 65536 }, events: [{ t: 600, qc: { intake: 0, stocked: false } }] });
  add('L1 blow-out over-Eddington, dark disc leaks s2', { seed: 2, rig: false, qc: { M0: 50000, lattice: 1, intake: 4, f: 1.3, setpoint: 65536 } });
  add('L1 setpoint lowered at 600 s: shed s1', { seed: 1, secs: 1200, qc: { M0: 60000, lattice: 1, intake: 4, setpoint: 60000 }, events: [{ t: 600, qc: { setpoint: 20000 } }] });
  add('L2 RSM from 65536 at the cap s2', { seed: 2, qc: { M0: 65536, lattice: 2, gas: 'RSM', intake: 5, setpoint: 262144 } });
  add('L2 Overdrive refused (needs L3) s3', { seed: 3, secs: 600, qc: { M0: 65536, lattice: 2, intake: 1, overdrive: 1 } });
  // L3: Overdrive
  add('L3 safe Overdrive 0.5 paid s1', { seed: 1, qc: { M0: 262144, lattice: 3, intake: 1, overdrive: 0.5 } });
  add('L3 Overdrive 1.0 half paid s2', { seed: 2, qc: { M0: 262144, lattice: 3, intake: 1, overdrive: 1, od: 0.5 } });
  add('L3 Overdrive runs dry at 1800 s s3', { seed: 3, qc: { M0: 2097152, lattice: 3, intake: 4, setpoint: 2097152, overdrive: 1 }, events: [{ t: 1800, qc: { od: 0 } }] });
  add('L3 Overdrive, Universium short s1', { seed: 1, secs: 1200, qc: { M0: 262144, lattice: 3, intake: 1, overdrive: 0.75, od: 1, uni: 0.25 } });
  // L4 and Unbound
  add('L4 top held, full Overdrive s2', { seed: 2, qc: { M0: L4TOP, lattice: 4, intake: 4, setpoint: L4TOP, overdrive: 1 } });
  add('Unbound n1 anchor paid, full Overdrive s3', { seed: 3, qc: { M0: 2 * L4TOP, lattice: 4, unbound: true, intake: 4, setpoint: 2 * L4TOP, overdrive: 1, anchor: 4096 } });
  add('Unbound n1 anchor cut at 600 s: shed to the L4 top s1', { seed: 1, secs: 1800, qc: { M0: 2 * L4TOP, lattice: 4, unbound: true, intake: 4, setpoint: 2 * L4TOP, overdrive: 1, anchor: 4096 }, events: [{ t: 600, qc: { anchor: 0 } }] });
  add('Unbound n1.5 anchor 2000 L/s: shed to what it pays s2', { seed: 2, secs: 1200, qc: { M0: 189812531, lattice: 4, unbound: true, intake: 4, setpoint: 189812531, anchor: 2000 } });
  add('Unbound growth at the cap on RSM s3', { seed: 3, qc: { M0: 2 * L4TOP, lattice: 4, unbound: true, gas: 'RSM', intake: 5, setpoint: 1e12, anchor: 1e9 } });
  add('Unbind off on an Unbound hole: shed to the L4 top s1', { seed: 1, secs: 600, qc: { M0: 2 * L4TOP, lattice: 4, unbound: true, intake: 4, setpoint: 2 * L4TOP, anchor: 4096 }, events: [{ t: 60, qc: { unbound: false } }] });
  return out;
}

// ---------------------------------------------------------------- the harness (mirrored by SmbhSim.runCase)
const HARNESS = `
function __fnv(h,v){ v|=0; for(let i=0;i<4;i++){ h^=(v>>>(8*i))&255; h=Math.imul(h,16777619); } return h; }
function __q6(v){ return Math.round(v*1e6)|0; }
function __qc(c){ return Object.assign({M0:4096, coil:4, b:3, gas:'He', f:1, stocked:true, power:true, purgeAt:40, lattice:0,
  setpoint:0, unbound:false, overdrive:0, intake:0, od:1, uni:-1, anchor:0}, c.qc||{}); }
function __inputs(Q,QC,rig){
  const medd=eddFeed(Q.M,QC.gas), gasDrawn=QC.stocked?QC.f*medd:0;
  let uG=0,uS=0; if(rig){ if(Q.lagE<-5) uG=10; else if(Q.lagE>5) uS=10; }
  return {medd, I:{gas:QC.gas, coil:QC.coil, b:QC.b, power:QC.power, gasDrawn, uGas:uG, uSpace:uS,
    regulatorPurge:rig && QC.purgeAt>0 && Q.lagPhi>=QC.purgeAt, lattice:QC.lattice, setpoint:QC.setpoint, unbound:QC.unbound,
    overdrive:QC.overdrive, intakeAvailable:0, mhdcsmAvailable:0, universiumAvailable:0, anchorAvailable:0}};
}
function __run(c){
  const QC=__qc(c), rig=c.rig!==false;
  const Q=newQuasar(QC,c.seed); Q.oEff=0; Q.shed=false;
  const L={bu:0, gas:0, hold:0, growth:0, mhdcsm:0, universium:0, anchor:0, shedS:0, leakS:0, anchorShortS:0, leaked:0, grown:0,
    maxM:Q.M, minM:Q.M, xSum:0, fails:0, eruptions:0, purges:0, zoneT:[0,0,0,0,0]};
  let hq=0x811c9dc5|0;
  for(let i=0;i<c.secs;i++){
    for(const ev of (c.events||[])) if(ev.t===i) Object.assign(QC,ev.qc);
    const {medd,I}=__inputs(Q,QC,rig);
    I.anchorAvailable=QC.anchor;
    const P=smbhPlan(Q,I);
    I.intakeAvailable=Math.max(0,QC.intake*medd-I.gasDrawn);
    I.mhdcsmAvailable=QC.od*P.overdriveNeed;
    I.universiumAvailable=(QC.uni>=0?QC.uni:QC.od)*P.overdriveNeed;
    const R=smbhDiscStep(Q,I); horizonStep(Q,I,P,R);
    L.bu+=R.G; L.gas+=I.gasDrawn; L.hold+=R.holdUpLitres; L.growth+=R.growthLitres; L.mhdcsm+=R.mhdcsmLitres;
    L.universium+=R.universiumLitres; L.anchor+=R.anchorLitres; if(R.anchorShort) L.anchorShortS++; L.leaked+=R.leakedMass; L.grown+=R.grownMass; L.xSum+=R.outputX;
    if(R.shedding) L.shedS++; if(R.leaking) L.leakS++; if(R.blowOut) L.fails++; if(R.eruption) L.eruptions++; if(R.purge) L.purges++;
    if(R.zone>=0) L.zoneT[R.zone]++;
    if(Q.M>L.maxM) L.maxM=Q.M; if(Q.M<L.minM) L.minM=Q.M;
    for(const v of [Q.E,Q.Phi,Q.a,Q.M/M_REF,Q.G,Q.I,Q.oEff]) hq=__fnv(hq,__q6(v));
    hq=__fnv(hq,Q.zone); hq=__fnv(hq,Q.reform); hq=__fnv(hq,Q.state==='OPEN'?0:1); hq=__fnv(hq,Q.shed?1:0);
  }
  return {M:Q.M, a:Q.a, E:Q.E, Phi:Q.Phi, I:Q.I, open:Q.state==='OPEN', reform:Q.reform, oEff:Q.oEff, shed:!!Q.shed, L, hash:hq>>>0};
}
// the cross-check: the same second through the P4 prototype's own quasarStep (its rig on, logging to a stub)
function __cross(c){
  const QC=Object.assign(__qc(c),{manGas:0, manSpace:0}), rig=c.rig!==false;
  Object.assign(RIG,{disc:rig, aim:true, space:true, assist:true});
  const A=newQuasar(QC,c.seed); A.oEff=0; A.shed=false;
  const W={Q:newQuasar(QC,c.seed), QC, CFG:{auto:true}, S:{log:[], t:0, logRev:0}};
  for(let i=0;i<c.secs;i++){
    const {medd,I}=__inputs(A,QC,rig);
    const P=smbhPlan(A,I); I.intakeAvailable=Math.max(0,QC.intake*medd-I.gasDrawn);
    const R=smbhDiscStep(A,I); horizonStep(A,I,P,R);
    quasarStep(W); const B=W.Q;
    for(const k of ['E','Phi','a','M','G','G0','I','zone','reform','state','lock','erupt','lagE','lagPhi'])
      if(!Object.is(A[k],B[k])) return 'second '+i+': '+k+' '+A[k]+' vs quasarStep '+B[k];
  }
  return '';
}
__out = __cases.map(c => __run(Object.assign({}, c)));
__crossOut = __cases.map(c => { const q=__qc(c); return q.lattice===0 && !(c.events||[]).length && q.M0<=COILS[Math.max(1,Math.min(4,q.coil))].cap
  ? __cross(Object.assign({}, c)) : null; });
__tables = smbhTables();
`;

function runReference(engine, list) {
  const ctx = vm.createContext({ __cases: list.map(c => c.spec), __out: null, __crossOut: null, __tables: null });
  vm.runInContext(engine + '\n' + HARNESS, ctx, { filename: 'smbh-engine.js' });
  return JSON.parse(JSON.stringify({ out: ctx.__out, cross: ctx.__crossOut, tables: ctx.__tables }));
}

// ---------------------------------------------------------------- the spec's printed tables (smbh-v1.md, rev 3)
// A string is the spec's figure as printed; it must match within half a unit of its last printed digit. Round
// integers the spec prints to 3-4 figures ("36,800") are written here in e-notation with those figures. A trailing
// '~' marks a figure the spec truncated or rounded twice (a full unit of tolerance; B-150 lists both: §5.2 n 2.5's
// full total is 5.97 million, printed 5.9; §9's Pump III at 16,777,216 SM is 10.45, printed 10.5).
// 'x' = "can't hold it" (the hole never gets there). Every row is quoted from the section named.
const SPEC = {
  // §2.3, Helium, f 1, by total intake F: gate on L1, gate on L2, 2.1 M on L3, 4.19 M at the L3 top, 4.19 M on L4
  growthHe: [['x', '65.6', 'x', 'x', '163.9'], ['x', '32.8', 'x', 'x', '82.0'], ['x', '21.9', 'x', 'x', '54.6'],
    ['23.3', '16.4', '42.6', '54.9', '41.0'], ['16.6', '13.1', '32.7', '39.7', '32.8']],
  // §2.3 at the cap, by gas (H, He, RSM): gate on L1, 4.19 M on L4, 67 M (L4 top), one Unbound doubling
  growthCap: [['33.1', '65.6', '105.6', '16.4'], ['16.6', '32.8', '52.8', '8.2'], ['8.3', '16.4', '26.4', '4.1']],
  // §2.3: Eddington feed and the 5x cap, L/t per SM (H, He, RSM)
  perSM: [['5', '25'], ['10', '50'], ['20', '100']],
  // §3.2: o, upkeep each (L/s), x at 262,144 / 4,194,304 / 67,108,864 SM
  overdrive: [['0', '7.9', '24.2', '31.0'], ['6250', '21.4', '59.1', '70.9'], ['25000', '34.1', '80.5', '89.5'],
    ['56250', '45.8', '91.4', '96.5'], ['100000', '56.0', '96.3', '98.9']],
  // §5.2: n = 0, .5, 1, 1.5, 2, 2.5, 3, 4: M, anchor, full Overdrive each, full total, half total
  unbound: [['67.1e6', '0', '100000', '200000', '50000'], ['94.9e6', '1448', '70711', '142870', '3.68e4'],
    ['134e6', '4096', '50000', '104096', '2.91e4'], ['190e6', '23170', '35355', '9.388e4', '4.085e4'],
    ['268e6', '262144', '25000', '312144', '2.746e5'], ['380e6', '5.9e6', '17678', '5.9e6~', '5.9e6'],
    ['537e6', '2.7e8', '12500', '2.7e8', '2.7e8'], ['1.07e9', '4.4e12', '6250', '4.4e12', '4.4e12']],
  // §9: hold / cap on Hydrogen (L/t), Pump III at hold, sustained x and EU/t, full Overdrive x and EU/t, upkeep, anchor
  // ('-' = not printed for that row)
  table9: [
    ['2.0e4', '-', '-', '1', '9.8e16', '-', '-', '-', '-'],
    ['8.2e4', '4.1e5', '0.00', '2.0', '2.0e17', '15.9', '1.6e18', '100000', '0'],
    ['1.3e6', '1.6e6', '0.07', '4.0', '3.9e17', '31.0', '3.0e18', '100000', '0'],
    ['1.3e6', '6.6e6', '0.07', '7.9', '7.7e17', '56.0', '5.5e18', '100000', '0'],
    ['3.4e7', '5.2e7', '1.7', '19.5', '1.9e18', '92.0', '9.0e18', '100000', '0'],
    ['8.4e7', '1.05e8', '4.2', '24.2', '2.4e18', '96.3', '9.4e18', '100000', '0'],
    ['2.1e8', '4.2e8', '10.5~', '30.1', '2.9e18', '98.7', '9.7e18', '100000', '0'],
    ['1.34e9', '1.68e9', '67', '31.0', '3.0e18', '98.9', '9.7e18', '100000', '0'],
    ['1.9e9', '2.4e9', '95', '31.0', '-', '98.9', '-', '70711', '1448'],
    ['2.7e9', '3.4e9', '134', '31.0', '-', '98.9', '-', '50000', '4096'],
    ['3.8e9', '4.7e9', '189', '31.0', '-', '98.9', '-', '35355', '23170'],
    ['5.4e9', '6.7e9', '267', '31.0', '-', '98.9', '-', '25000', '262144'],
    ['1.1e10', '1.3e10', '535', '31.0', '-', '98.9', '-', '12500', '2.7e8']],
};
// half a unit of the last printed digit (of the mantissa for e-notation); exact integers within 0.5
function tolOf(s) {
  if (s.endsWith('~')) return 2 * tolOf(s.slice(0, -1));
  const m = /^(-?\d+)(?:\.(\d+))?(?:e([+-]?\d+))?$/.exec(s);
  if (!m) throw new Error('bad spec number ' + s);
  const dec = m[2] ? m[2].length : 0, exp = m[3] ? +m[3] : 0;
  return 0.5 * Math.pow(10, exp - dec);
}
function checkTables(t) {
  const bad = [];
  const cmp = (where, spec, got) => {
    if (spec === '-') return;
    if (spec === 'x') { if (got !== -1) bad.push(`${where}: spec "can't hold it", got ${got}`); return; }
    if (got === -1) { bad.push(`${where}: spec ${spec}, got "can't hold it"`); return; }
    const want = Number(spec.replace('~', '')), tol = tolOf(spec) * 1.0000001;
    if (Math.abs(got - want) > tol) bad.push(`${where}: spec ${spec}, got ${got} (off ${(got - want).toPrecision(3)}, tol ${tol})`);
  };
  SPEC.growthHe.forEach((r, i) => r.forEach((s, j) => cmp(`§2.3 He F${i + 1} col${j + 1}`, s, t.growthHe[i][j])));
  SPEC.growthCap.forEach((r, i) => r.forEach((s, j) => cmp(`§2.3 cap ${['H', 'He', 'RSM'][i]} col${j + 1}`, s, t.growthCap[i][j])));
  SPEC.perSM.forEach((r, i) => r.forEach((s, j) => cmp(`§2.3 per SM ${['H', 'He', 'RSM'][i]} col${j + 1}`, s, t.perSM[i][j])));
  SPEC.overdrive.forEach((r, i) => r.forEach((s, j) => cmp(`§3.2 row${i} col${j}`, s, t.overdrive[i][j])));
  SPEC.unbound.forEach((r, i) => r.forEach((s, j) => cmp(`§5.2 row${i} col${j}`, s, t.unbound[i][j])));
  // §9: got row = [M, L, hold, cap, pumps, xs, eus, xo, euo, upkeep, anchor]
  SPEC.table9.forEach((r, i) => r.forEach((s, j) => cmp(`§9 M ${t.table9[i][0]} col${j}`, s, t.table9[i][j + 2])));
  return bad;
}

// ---------------------------------------------------------------- java
function runJava() {
  fs.mkdirSync(CLASSES, { recursive: true });
  const jc = cp.spawnSync('javac', ['-Xlint:-options', '--release', '8', '-encoding', 'UTF-8', '-d', CLASSES, ...javaSrc()], { stdio: 'inherit' });
  if (jc.status !== 0) { console.error('javac failed'); return 1; }
  const j = cp.spawnSync('java', ['-cp', CLASSES, 'com.nateyburr.beyondthestars.machines.quasar.engine.SmbhSim', 'golden', GOLDEN], { stdio: 'inherit' });
  return j.status === null ? 1 : j.status;
}

// ---------------------------------------------------------------- main
function main() {
  const args = process.argv.slice(2);
  const check = args.includes('--check'), java = args.includes('--java');
  const list = cases();
  const t0 = Date.now();
  const { out, cross, tables } = runReference(loadEngine(), list);
  console.log(`${list.length} cases run on the SMBH reference in ${Date.now() - t0} ms`);
  let failed = 0, crossed = 0;
  for (let i = 0; i < list.length; i++) {
    const r = out[i], L = r.L, n = Math.max(1, list[i].spec.secs);
    console.log(`  ${list[i].name.padEnd(58)} M ${r.M.toExponential(4)} (max ${L.maxM.toExponential(3)})  mean ${(L.bu / n).toExponential(3)} Bu/t` +
      `  x̄ ${(L.xSum / n).toFixed(2)}  shed ${L.shedS} s  leak ${L.leakS} s  anchor short ${L.anchorShortS} s  blow-outs ${L.fails}`);
    if (cross[i] !== null) {
      crossed++;
      if (cross[i]) { failed++; console.error(`    CROSS-CHECK FAILED vs the P4 quasarStep: ${cross[i]}`); }
    }
  }
  console.log(`cross-check: ${crossed} lattice-0 cases, the SMBH disc second equals the P4 prototype's quasarStep every second` +
    (failed ? ` EXCEPT ${failed}` : ''));
  const bad = checkTables(tables);
  if (bad.length) { failed += bad.length; console.error('SPEC TABLES:'); for (const b of bad) console.error('  ' + b); }
  else console.log('spec tables: §2.3 growth times, the per-SM intake, §3.2, §5.2 and §9 match smbh-v1.md within the printed rounding');
  if (failed) return 1;
  const doc = {
    about: 'SMBH v1 golden results (the lattice, intake split, leak, shed, Overdrive, Unbound anchor; the spec tables) ' +
      'from the reference block tools/fixtures/smbh-engine.js (tools/smbh-golden.js). SmbhSim golden must match every ' +
      'number exactly. Regenerate; do not edit.',
    cases: list.map((c, i) => ({ name: c.name, spec: c.spec, expect: out[i] })),
    tables,
  };
  const text = JSON.stringify(doc, null, 1) + '\n';
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
