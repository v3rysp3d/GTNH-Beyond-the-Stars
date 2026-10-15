// smbh-engine.js — the SMBH v1 reference block for tools/smbh-golden.js (designs/docs/smbh-v1.md §2-§5, §9, §10.1).
// It runs on top of the shared ENGINE block (tools/fixtures/star-lifecycle-engine.js) and the P4 block
// (tools/fixtures/quasar-engine.js): it uses their clamp, zoneOf, ZONES, mulberry, GASES, COILS, aEq, iscoEta and
// newQuasar. The Java port (machines/quasar/engine: QuasarParams' SMBH section, QuasarDisc's two hooks,
// HorizonLattice, SmbhSim) must reproduce every number of tools/smbh-golden.js's cases bit for bit.
// It was written in the addon worktree (the SMBH build, B-150). The spec (§10.1) asks for it in
// designs/quasar-p4-prototype.html as well: paste the SMBH-START … SMBH-END block after the P4 block there; it adds
// no globals the P4 block uses and does not change quasarStep.
/*SMBH-START*/
/* SMBH v1: the Supermassive Lattice (the quasar's last upgrade). Constants: smbh-v1.md §10.1 (every number a knob). */
const M_REF=4096;                                   // the MagMatter coil cap: X = 1 there
const LATTICE=[null,
  {name:'Superdense Ergomatter', safe:16384,   top:65536},
  {name:'White Hole Matter',     safe:65536,   top:262144},
  {name:'Universium',            safe:262144,  top:4194304},
  {name:'Constrained Eternity',  safe:4194304, top:67108864}];
const LATTICE_COIL=4, LATTICE_OVERDRIVE=3, UNBOUND_MAX=1125899906842624;   // 2^50 SM: numeric sanity only
const INTAKE_CAP=5, HOLDUP_K=3, LEAK_HALF_LIFE=3600, GROWTH_K=6.4e-8;
const OVERDRIVE_UPKEEP=1e5, OVERDRIVE_GAIN=7, X_CEIL=100, XM_CEIL=32;
const ANCHOR_BASE=1024, ANCHOR_GROWTH=4, SHED_RATE=0.02, LN2_=0.6931471805599453;
// b^y as exp(y·ln b): V8's exp and log are fdlibm's (Java StrictMath's) bit for bit; its pow is not
const powE=(b,y)=>Math.exp(y*Math.log(b));

// §3.1: the mass term and the output multiplier (X ≤ ×100 always)
function massTerm(M){ const r=M/M_REF; if(r<=1) return r*r; return 1+(XM_CEIL-1)*Math.tanh((Math.sqrt(r)-1)/(XM_CEIL-1)); }
function outputX(M,o){ const raw=massTerm(M)*(1+OVERDRIVE_GAIN*o); if(raw<=1) return raw; return 1+(X_CEIL-1)*Math.tanh((raw-1)/(X_CEIL-1)); }
// G0: today's expression at or under 4,096 SM without Overdrive (bit for bit), λc·a²·b²·4,096·X above
function g0x(lc,a,b,M,oEff){ if(!(oEff>0) && M<=M_REF) return lc*a*a*b*b*Math.pow(M/64,2); return lc*a*a*b*b*M_REF*outputX(M,oEff); }
const coilCap=coil=>COILS[Math.max(1,Math.min(4,coil))].cap;
// §2.1-§2.2, §10.3: the lattice in force, the band, the structure's top, the setpoint in force
function latticeInForce(coil,L){ return coil>=LATTICE_COIL ? Math.max(0,Math.min(4,L)) : 0; }
function safeLimit(coil,L){ const l=latticeInForce(coil,L); return l>0 ? LATTICE[l].safe : coilCap(coil); }
function bandTop(coil,L){ const l=latticeInForce(coil,L); return l>0 ? LATTICE[l].top : coilCap(coil); }
function structureTop(coil,L,unbound){ const l=latticeInForce(coil,L); if(l===4 && unbound) return UNBOUND_MAX; return bandTop(coil,L); }
function massSetpoint(coil,L,unbound,sp){ const l=latticeInForce(coil,L); if(l===0) return coilCap(coil);
  const top=structureTop(coil,L,unbound); if(!(sp>0)) return LATTICE[l].safe; return Math.max(M_REF,Math.min(top,sp)); }
function bandX(M,coil,L){ const l=latticeInForce(coil,L); if(l===0) return 0; const s=LATTICE[l].safe, t=LATTICE[l].top;
  if(M<=s) return 0; if(M>=t) return 1; return Math.log(M/s)/Math.log(t/s); }
const eddFeed=(M,gas)=>40*M/GASES[gas].k;
function unboundN(M){ const t=LATTICE[4].top; return M>t ? Math.log(M/t)/LN2_ : 0; }
// §3.2, §5.2: the upkeeps
const overdriveUpkeep=(o,n)=>OVERDRIVE_UPKEEP*o*o*powE(2,-n);
const anchorUpkeep=n=>n>0 ? ANCHOR_BASE*powE(ANCHOR_GROWTH,n*n) : 0;
function anchorDoublings(L){ if(!(L>=ANCHOR_BASE)) return 0; return Math.sqrt(Math.log(L/ANCHOR_BASE)/Math.log(ANCHOR_GROWTH)); }
const ANCHOR_SLACK=1e-9;   // "anchor short" ignores a cover this close to 1 (n → U(n) → n is not exact in floating point)
// the most mass the Primordial Matter on hand holds Unbound (the L4 top under 1,024 L/s)
const anchorCap=L=>LATTICE[4].top*powE(2,anchorDoublings(L));
// the growth cap: the setpoint in force, and Unbound also the anchor's cap (the coil cap without a lattice)
function growthCap(I){ const sp=massSetpoint(I.coil,I.lattice,I.unbound,I.setpoint);
  if(!(I.unbound && latticeInForce(I.coil,I.lattice)===4)) return sp; return Math.min(sp,anchorCap(I.anchorAvailable)); }

/* The disc second with the SMBH's two hooks (G0 with o_eff; growth capped at growthCap). The P4 block's
   quasarStep minus its own rig and logging: the inputs I carry what the rig and the controller decided. At lattice 0
   with M ≤ 4,096 SM and o_eff 0 it is quasarStep exactly (tools/smbh-golden.js cross-checks that every run). */
function smbhDiscStep(Q,I){
  const R={G:0,jet:0,G0:0,zone:-1,blowOut:false,reforming:false,eruption:false,purge:false,gasTrimLitres:0,spaceLitres:0};
  if(Q.state!=='OPEN' && Q.state!=='REFORM'){ Q.G=0; Q.G0=0; return R; }
  const b=I.power ? Math.min(I.b,I.coil) : 0;
  const Medd=eddFeed(Q.M,I.gas), fEff=I.gasDrawn/Medd;
  Q.fEff=fEff; Q.b=b; Q.Medd=Medd; R.coilEU=2*UXV_V*b*b;
  if(Q.flash>0) Q.flash--;
  if(Q.state==='REFORM'){
    if(fEff>=0.3) Q.reform++;
    Q.G=0; Q.G0=0; Q.erupt=0; Q.zone=-1; Q.Yq=0; R.reforming=true;
    if(Q.reform>=300){ Q.state='OPEN'; Q.E=0; Q.I=100; Q.Phi=0; Q.lock=0; Q.reform=0; }
    Q.lagE=Q.E; Q.lagPhi=Q.Phi; return R;
  }
  const uG=clamp(I.uGas,0,10), uS=clamp(I.uSpace,0,10);
  R.gasTrimLitres=uG*1000*Q.M/64; R.spaceLitres=uS*100*Q.M/64;
  Q.E=clamp(Q.E+(100*(fEff-1)-Q.E)/30+0.04*Math.max(Q.E,0)+(Q.rng()*2-1)*2+2*uG-2*uS,-120,120);
  const lam=Math.max(0,1+Q.E/100), lc=Math.min(lam,1); Q.lam=lam; Q.lc=lc;
  Q.a+=(aEq(lc,b)-Q.a)/1800;
  const z=zoneOf(Math.abs(Q.E)); Q.zone=z; R.zone=z;
  Q.lock = z===0 ? Q.lock+1 : 0;
  Q.I=clamp(Q.I+ZONES[z].dI,0,100);
  if(z===4 || Q.I<=0){ R.blowOut=true; Q.state='REFORM'; Q.reform=0; Q.Phi=0; Q.G=0; Q.G0=0; Q.erupt=0; Q.zone=-1; Q.Yq=0; Q.lagE=Q.E; Q.lagPhi=0; Q.flash=0; return R; }
  const Yq=(z===0 && Q.lock>=600)?1.5:ZONES[z].Y; Q.Yq=Yq;
  const G0=g0x(lc,Q.a,b,Q.M,Q.oEff); Q.G0=G0; R.G0=G0;
  Q.Phi=Math.min(100,Q.Phi+b*lc/2.4);
  let purge=!!I.regulatorPurge;
  if(Q.purgeReq){ purge=true; Q.purgeReq=false; }
  if(Q.Phi>=100 || (purge && Q.Phi>=20)){
    const full=Q.Phi>=100;
    Q.erupt=3; Q.eruptG=G0*Yq*3*(Q.Phi/100); Q.E=clamp(Q.E-30*Q.Phi/100,-120,120); Q.a-=0.002*Q.Phi/100; Q.Phi=0; Q.flash=3;
    if(full) R.eruption=true; else R.purge=true;
  }
  let G=G0*Yq*(0.5+Q.Phi/100); if(Q.erupt>0){ G+=Q.eruptG; Q.erupt--; }
  Q.G=G; R.G=G; R.jet=G/2;
  const cap=growthCap(I);
  if(Q.M<cap) Q.M=Math.min(cap,Q.M+6.4e-8*(1-iscoEta(clamp(Q.a,0,0.998)))*lc*Medd);
  Q.lagE=Q.E; Q.lagPhi=Q.Phi;
  return R;
}

/* §2.3-§2.5, §3.2, §5: the plan, from the start-of-second state (I.gasDrawn = the disc's draw, I.anchorAvailable =
   the Primordial Matter on hand; the other supplies may be set after it) */
function smbhPlan(Q,I){
  const P={active:Q.state==='OPEN'||Q.state==='REFORM', open:Q.state==='OPEN', M:Q.M, oEffUsed:Q.oEff,
    lattice:latticeInForce(I.coil,I.lattice), safe:safeLimit(I.coil,I.lattice), top:bandTop(I.coil,I.lattice),
    structureTop:structureTop(I.coil,I.lattice,I.unbound), setpoint:massSetpoint(I.coil,I.lattice,I.unbound,I.setpoint),
    x:bandX(Q.M,I.coil,I.lattice), n:0, medd:0, intakeCap:0, holdUpNeed:0, growthRoom:0, shedPlanned:false,
    overdrive:0, overdriveNeed:0, anchorNeed:0, anchorCap:Infinity, growthCap:0, target:0};
  P.n = P.lattice===4 && I.unbound ? unboundN(Q.M) : 0;
  P.medd=eddFeed(Q.M,I.gas); P.intakeCap=INTAKE_CAP*P.medd;
  P.growthCap=growthCap(I);
  if(I.unbound && P.lattice===4) P.anchorCap=anchorCap(I.anchorAvailable);
  P.target=Math.min(P.growthCap,P.structureTop);
  P.shedPlanned = P.active && Q.M>P.target;
  if(P.open && P.lattice>0 && !P.shedPlanned){
    P.holdUpNeed=HOLDUP_K*P.x*P.medd;
    if(Q.M<P.growthCap) P.growthRoom=Math.max(0,P.intakeCap-I.gasDrawn-P.holdUpNeed);
  }
  const o=Math.max(0,Math.min(1,I.overdrive));
  if(P.open && P.lattice>=LATTICE_OVERDRIVE && o>0){ P.overdrive=o; P.overdriveNeed=overdriveUpkeep(o,P.n); }
  if(P.active && P.n>0) P.anchorNeed=anchorUpkeep(P.n);
  return P;
}
/* the lattice's part of the second, after smbhDiscStep (R); fills R's SMBH fields: the litres to drain */
function horizonStep(Q,I,P,R){
  Object.assign(R,{holdUpLitres:0, growthLitres:0, holdUpCoverage:1, leaking:false, leakedMass:0, grownMass:0, shedding:false,
    shedTarget:0, shedReason:'', overdriveNeed:0, overdriveCoverage:1, mhdcsmLitres:0, universiumLitres:0, oEff:0,
    anchorNeed:0, anchorCoverage:1, anchorLitres:0, anchorPaysN:0, anchorShort:false, outputX:outputX(P.M,P.oEffUsed)});
  if(!P.active){ Q.oEff=0; Q.shed=false; return R; }
  const lit=Q.state==='OPEN';
  const avail=Math.max(0,I.intakeAvailable);
  let hold=0, grow=0, ch=1;
  if(lit){ hold=Math.min(avail,P.holdUpNeed); grow=Math.min(avail-hold,P.growthRoom); if(P.holdUpNeed>0) ch=hold/P.holdUpNeed; }
  else if(P.lattice>0 && Q.M>P.safe) ch=0;
  R.holdUpCoverage=ch;
  if(P.anchorNeed>0){
    const used=Math.min(Math.max(0,I.anchorAvailable),P.anchorNeed);
    R.anchorNeed=P.anchorNeed; R.anchorCoverage=used/P.anchorNeed; R.anchorLitres=used;
    R.anchorPaysN=Math.min(P.n,anchorDoublings(Math.max(0,I.anchorAvailable)));
    R.anchorShort=R.anchorCoverage<1-ANCHOR_SLACK;
  }
  const target=P.target;
  const why = Q.M>P.structureTop ? 'structure' : P.anchorCap<Math.min(P.setpoint,P.structureTop) && Q.M>P.anchorCap ? 'anchor' : 'setpoint';
  if(Q.M>target){
    Q.M=Math.max(target,Q.M*(1-SHED_RATE)); Q.oEff=0; Q.shed=true;
    R.shedding=true; R.shedTarget=target; R.shedReason=why; Q.G=0; R.G=0; R.jet=0; return R;
  }
  Q.shed=false;
  let oEff=0;
  if(lit && P.overdriveNeed>0){
    const have=Math.min(Math.max(0,I.mhdcsmAvailable),Math.max(0,I.universiumAvailable)), co=Math.min(1,have/P.overdriveNeed);
    R.overdriveNeed=P.overdriveNeed; R.overdriveCoverage=co; R.mhdcsmLitres=co*P.overdriveNeed; R.universiumLitres=R.mhdcsmLitres;
    oEff=P.overdrive*co;
  }
  Q.oEff=oEff; R.oEff=oEff;
  // QA-B09: the Growth segment draws only the litres that still reach the growth cap after the disc's own growth
  if(grow>0){ const perL=GROWTH_K*(1-iscoEta(clamp(Q.a,0,0.998))); grow=Q.M>=P.growthCap?0:perL>0?Math.min(grow,(P.growthCap-Q.M)/perL):grow; }
  R.holdUpLitres=hold; R.growthLitres=grow;
  if(grow>0 && Q.M<P.growthCap){ const m0=Q.M; Q.M=Math.min(P.growthCap,Q.M+GROWTH_K*(1-iscoEta(clamp(Q.a,0,0.998)))*grow); R.grownMass=Q.M-m0; }
  if(P.lattice>0 && ch<1 && Q.M>P.safe){
    const m0=Q.M, l=Math.log(Q.M/P.safe)*powE(2,-(1-ch)/LEAK_HALF_LIFE);
    Q.M=Math.max(P.safe,P.safe*Math.exp(l)); R.leaking=true; R.leakedMass=m0-Q.M;
  }
  return R;
}

/* §2.3 growth times (the spec's idealised sum: λc 1, f 1, η fixed, 60 s steps on ln M, as in smbh/v3.js in my local work folder),
   built from the engine's pieces. F = total intake, × Ṁ_Edd. band = the installed lattice tier (0 = none). */
const TABLE_ETA=0.0821;
function smbhGrowthHours(target,F,gas,band){
  const dt=60, lt=Math.log(target); let lnM=Math.log(M_REF), T=0;
  while(lnM<lt){
    const M=Math.exp(lnM), medd=eddFeed(M,gas), x=bandX(M,LATTICE_COIL,band), safe=band?LATTICE[band].safe:0;
    const disc=Math.min(1,F), hold=HOLDUP_K*x, avail=Math.max(0,F-disc), holdPaid=Math.min(avail,hold), ch=hold>0?holdPaid/hold:1;
    const grow=Math.max(0,Math.min(avail-holdPaid,INTAKE_CAP-disc-hold));
    let d=GROWTH_K*(1-TABLE_ETA)*(disc+grow)*medd/M;
    if(band && M>safe) d-=(1-ch)*Math.log(M/safe)*LN2_/LEAK_HALF_LIFE;
    if(d<=1e-12) return Infinity;
    lnM+=d*dt; T+=dt; if(T>1e8) return Infinity;
  }
  return T/3600;
}
/* one Unbound doubling at the 5× cap (x = 1: disc 1 + growth 1 = 2× the Eddington rate), hours */
const smbhDoublingHours=gas=>LN2_/(GROWTH_K*(1-TABLE_ETA)*eddFeed(1,gas)*(INTAKE_CAP-HOLDUP_K))/3600;
/* §9's practical ×1: the max-quasar bench's 9,796 Bu/t (b 2.75, f 1, never purged, four Mk III crowns) */
const SMBH_X1_BU=9796, PUMP3_H=2.007e7;
/* the spec's tables (§2.3, §3.2, §5.2, §9), as numbers */
function smbhTables(){
  const H=v=>v===Infinity?-1:v;
  const t={};
  const bands=[[65536,1],[65536,2],[2097152,3],[4194304*0.999,3],[4194304,4]];
  t.growthHe=[1,2,3,4,5].map(F=>bands.map(([m,b])=>H(smbhGrowthHours(m,F,'He',b))));
  t.growthCap=['H','He','RSM'].map(g=>[H(smbhGrowthHours(65536,5,g,1)),H(smbhGrowthHours(4194304,5,g,4)),
    H(smbhGrowthHours(67108864*0.999,5,g,4)),smbhDoublingHours(g)]);
  t.perSM=['H','He','RSM'].map(g=>[eddFeed(1,g)/20, INTAKE_CAP*eddFeed(1,g)/20]);
  t.overdrive=[0,0.25,0.5,0.75,1].map(o=>[overdriveUpkeep(o,0),outputX(262144,o),outputX(4194304,o),outputX(67108864,o)]);
  t.unbound=[0,0.5,1,1.5,2,2.5,3,4].map(n=>{ const od=overdriveUpkeep(1,n), a=anchorUpkeep(n);
    return [LATTICE[4].top*powE(2,n), a, od, 2*od+a, 2*overdriveUpkeep(0.5,n)+a]; });
  // §9: mass, the lattice holding it (0 = the coils), hold / cap intake on Hydrogen (L/t), Pump III at hold,
  // sustained ×X and EU/t, full Overdrive ×X and EU/t, upkeep each (L/s), anchor (L/s)
  const rows=[[4096,0],[16384,1],[65536,1],[262144,3],[2097152,3],[4194304,3],[16777216,4],[67108864,4],
    [94906266,4],[134217728,4],[189812531,4],[268435456,4],[536870912,4]];
  t.table9=rows.map(([M,L])=>{ const medd=eddFeed(M,'H'), x=bandX(M,LATTICE_COIL,L), n=L===4?unboundN(M):0;
    const hold=(1+HOLDUP_K*x)*medd/20, xs=outputX(M,0), xo=outputX(M,1);
    return [M,L,hold,INTAKE_CAP*medd/20,hold/PUMP3_H,xs,xs*SMBH_X1_BU*1e13,xo,xo*SMBH_X1_BU*1e13,overdriveUpkeep(1,n),anchorUpkeep(n)]; });
  return t;
}
/*SMBH-END*/
