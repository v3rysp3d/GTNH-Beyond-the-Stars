// quasar-engine.js — FIXTURE for tools/quasar-golden.js. Do not edit by hand:
// regenerate with `node tools/quasar-golden.js --refresh-fixture`, then regenerate the golden JSON.
// The P4 block of the quasar play prototype, verbatim (the reference for the Java quasar / conduit / Eye T7-T8
// maths). It runs on top of the shared ENGINE block in tools/fixtures/star-lifecycle-engine.js.
// source: designs/quasar-p4-prototype.html (design repo GTNH-Addons)
// source-sha1: b03e302d3f0a318e2d24338b386699b12f2c8e05
// engine-sha1: c28c22097debf085fd21226aed3f656c7706d04b
// p4-sha1: fe1dff025f9bb324f31248da0c76dc85ad33b91c
/*P4-START*/
/* P4 extension: Eye T7/T8 tiers, the Magnetar Collapse and Galactic Merger, the Quasar Engine disc, and the
   Einstein-Rosen Conduit. Uses the D-49 engine above through its additive hooks only (T.d, T.bx, T.sigK, T.perSec,
   T.cycOut, T.tde, transDrift, transTick). */
const UXV_V = 536870912;          // GT UXV voltage
const D7 = 1.5;                   // D_7, Bu/t
function demandT(T){ return 1.5*Math.pow(4,T-7); }
const RIG = {disc:true, aim:true, space:true, assist:true};   // sub-switches of the automation toggle
const SPACE_ON = 10;   // the Eye's Space trim is one binary valve: Directive Field bit 1 (P > +5) opens it at 10 units/s
const spaceRig = E => { const u={}; if(RIG.space && E.P>5) u.Space=SPACE_ON; return u; };
const rigLines = (t8,planets) => planets ? [
  'Quasar disc: Disc bit 1 (E < −5) → feed-gas valve, 10 units/s; Disc bit 2 (E > +5) → Space valve, 10 units/s',
  'Quasar purge: Disc bit 3 (Φ ≥ purge setpoint) AND NOT Transition bit 4 → rising edge on the Burst Control Hatch',
  'J1 Eye : crown ← sensor P, analog, inverted, window ±w, k = 2 (a straight wire, no gate)',
  'Eye Space trim: Field bit 1 (P > +5) → Space valve, 10 units of 100 L/s',
  'Planets mode: one gauge (P), no aim, no events. An ME interface keeps the slots filled.'] : [
  'Quasar disc: Disc bit 1 (E < −5) → feed-gas valve, 10 units/s; Disc bit 2 (E > +5) → Space valve, 10 units/s',
  'Quasar purge: Disc bit 3 (Φ ≥ purge setpoint) AND NOT Transition bit 4 → rising edge on the Burst Control Hatch',
  'J1 Eye : crown ← sensor P, analog, inverted, window ±w, k = 2; OR Transition bit 3 forces it to 15',
  ...(t8?['J3 halo : rail ← sensor Λ, analog, window ±w, k = 2']:[]),
  'J2 rail-high : rail-low ← sensor W, analog, window ±w, k = 2',
  'Eye Space trim: Field bit 1 (P > +5) → Space valve, 10 units of 100 L/s (one binary valve, no proportional control)',
  'Collapse assist (T6 → T7): Transition bit 3 (purge now) → quasar purge; bit 4 (hold) blocks the regulator purge'];
function f8of(S){ return S.trans && S.trans.kind==='gm' ? 5 : S.cfg.f8; }

/* ---- companions (multi-object) and planets mode, ruling 2026-10-13 ---- */
// A companion is an earlier tier's object run inside the galaxy's field. It pays its own upkeep, runs at parallel
// 2^(N−1−k), takes the galaxy's Y, and adds 0.25·D_T to the burst demand and +0.25 to P's noise.
const COMP={
 1:{name:'Sol (T1)', out:['Raw Star Matter',50000], up:[['Hydrogen',5e7],['Helium',5e7]]},
 2:{name:'Sirius A (T2)', out:['Excited Stellar Plasma',2500], up:[['Hydrogen',1.25e7],['Helium',1.25e7],['Raw Star Matter',25000]]},
 3:{name:'Arcturus (T3)', out:['Raw Hyperdense Stellar Plasma',625], up:[['Hydrogen',1.25e7],['Helium',1.25e7],['Excited Stellar Plasma',1250]]},
 4:{name:'Neutron star (T4)', out:['White Dwarf Matter',153.5], up:[['Hyperdense Stellar Mixture',41.64],['Molten SpaceTime',144]]},
 5:{name:'Sirius B (T5)', out:['Black Dwarf Matter',38.4], up:[['White Dwarf Matter',153.6]]},
 6:{name:'SGR 1806-20 (T6)', out:['Magnetar Matter',15], up:[['White Dwarf Matter',60],['Black Dwarf Matter',15],['Antipolarization Agent',3000]]}};
const compPar=(N,k)=>Math.pow(2,N-1-k);
function compUp(N,comp){ const u=[]; for(const k of comp) for(const [n,v] of COMP[k].up) u.push([n,v*compPar(N,k)]); return u; }
function compOut(S,cy,mY){ const T=TIERS[S.tier]; for(const k of T.compList||[]){ const c=COMP[k], v=c.out[1]*compPar(S.tier,k)*mY*cy.rMin; add(S.L.prod,c.out[0]+' (companion)',v); } }
// Planets: TecTech's dims as converted in P3 (rocket tier R → Eye tier k: R0-3 T1, R4-6 T2, R7-8 T3, Deep Dark T4).
const PLANETS={
 moon:{name:'Moon',R:1,k:1,min:25}, mars:{name:'Mars',R:2,k:1,min:31}, io:{name:'Io',R:3,k:1,min:39}, titan:{name:'Titan',R:5,k:2,min:61},
 pluto:{name:'Pluto',R:6,k:2,min:76}, barc:{name:'Barnard C',R:7,k:3,min:95}, bare:{name:'Barnard E',R:8,k:3,min:119}, dd:{name:'Deep Dark',R:9,k:4,min:240}};
const PLANET_SLOTS={7:3,8:6,9:9};
function planetTick(S,m){
  const T=TIERS[S.tier];
  if(!S.pl) S.pl=S.cfg.planets.slice(0,T.slots).map(id=>({id,t:0,zf:0,n:0}));
  const zf=[1,1,0.5,0,0][S.zone];
  for(const sl of S.pl){ const p=PLANETS[sl.id]; sl.t+=m; sl.zf+=zf; sl.n++;
    if(sl.t>=p.min*60){ const par=Math.pow(2,S.tier-p.k), f=sl.zf/sl.n, ok=par*(1-0.05*p.R)*f;
      S.L.runs=S.L.runs||{}; const key=`${p.name} (R${p.R} → T${p.k}) ×${par}`; S.L.runs[key]=(S.L.runs[key]||0)+ok; S.mark|=8;
      log(S,`Planet run done: ${p.name} ×${par} parallel, ${ok.toFixed(1)} succeeded (base ${(1-0.05*p.R).toFixed(2)}, field factor ${f.toFixed(2)}).`,'ok');
      sl.t=0; sl.zf=0; sl.n=0; } } }

function galaxyTier(n,comp){
  const nc=comp.length, cu=compUp(n,comp);
  const base={sig:1, trims:{Space:{cap:10,fx:{P:-2}}}, compList:comp, comp:nc,
    ev:{name:'Minor merger', every:[900,1800], warn:15, dur:5, fx:{W:12}, signed:true, flip:true}};
  if(n===7) return Object.assign(base,{n:7, star:'Triangulum', stage:'Galaxy nucleus: M33 gets its first supermassive hole', gauges:['P','W'], sigK:{P:1+0.25*nc,W:1.5}, g:{P:-0.01,W:0.02},
    d:S=>{ const tr=S.trans, b=S.g.beta;
      if(tr && tr.kind==='mc' && tr.phase===3){ const rho=Math.min(1,tr.st/600); return {P:3*(b-rho), W:1.5*S.prec*rho}; }
      return {P:3*(b-1), W:1.5*S.prec}; },
    bx:(S,k)=> k==='W' ? 6*(S.g.gLo-S.g.gHi) : 0,
    prod:{name:'Ergomatter',base:600}, up:[['Magnetar Matter',7.5],...cu],
    ctrl:E=>spaceRig(E), rig:()=>rigLines(false),
    perSec:S=>{ S.cyc.bSum+=S.g.beta; },
    cycOut:(S,cy,mY)=>{ if(S.trans) return 0; compOut(S,cy,mY); return 600*mY*cy.rMin*Math.min(cy.bSum/Math.max(1,cy.n),1.5); }});
  return Object.assign(base,{n:8, star:'Milky Way', stage:'Merged spiral galaxy, with satellite galaxies falling through its disc', gauges:['P','W','L'], sigK:{P:1+0.25*nc,W:1.5,L:1.5}, g:{P:-0.01,W:0.02,L:0.02},
    d:S=>{ const tr=S.trans, ramp = tr && tr.kind==='gm' ? clamp((tr.st-600)/600,0,1) : 1;
      TIERS[8].g.L=0.02*ramp;   // finding F-4: the shear gain ramps in with its drift
      return {P:3*(S.g.beta-1), W:1.5*S.prec, L:0.4*f8of(S)*ramp}; },
    bx:(S,k)=> k==='W' ? 6*(S.g.gLo-S.g.gHi) : k==='L' ? -10*S.g.gHalo : 0,
    prod:{name:'Superdense Ergomatter',base:0}, up:cu,
    tde:{every:[1800,3600], warn:120, dur:300},
    ctrl:E=>spaceRig(E), rig:()=>rigLines(true),
    perSec:(S,m)=>{ S.cyc.bSum+=S.g.beta; if(!S.trans){ const f=S.cfg.f8*m; add(S.L.up,'Ergomatter',f); S.cyc.f8+=f; } tdePost(S); },
    cycOut:(S,cy,mY)=>{ if(S.trans) return 0; compOut(S,cy,mY); return T8_RATIO*cy.f8*mY*cy.rMin; }});
}
const T8_RATIO = 1.0;   // revision 2026-10-13: Eye T8 makes Superdense Ergomatter 1 : 1 from the Ergomatter fed (was 0.5)
function planetsTier(n){
  return {n, star:`${PLANET_SLOTS[n]} planets`, stage:'Planets mode: a calmer, lighter field (one gauge, no events)', gauges:['P'], sig:0.5, sigK:{P:0.5}, g:{P:-0.01},
    d:S=>({P:1*(S.g.beta-1)}), trims:{Space:{cap:10,fx:{P:-2}}}, prod:{name:'Planet runs',base:0}, up:[], slots:PLANET_SLOTS[n],
    ctrl:E=>spaceRig(E), rig:()=>rigLines(false,true), perSec:(S,m)=>planetTick(S,m), cycOut:()=>0};
}
function setGalaxyTiers(cfg){
  for(const n of [7,8]) TIERS[n] = cfg.mode==='planets' ? planetsTier(n) : galaxyTier(n,(cfg.comp||[]).slice(0,n-6));
}
setGalaxyTiers({mode:'galaxy',comp:[]});

function nucleusFailText(S,reason){
  if(TIERS[S.tier].slots){ if(S.pl) for(const sl of S.pl){ sl.t=0; sl.zf=0; sl.n=0; }
    return `Planets-mode field breached: ${reason}. The planet runs in flight fail and their inputs are spent. Nothing else is lost: Relight re-forms the field.`; }
  return S.tier===7
    ? `Triangulum lost: ${reason}. The galaxy's nucleus is gone. In the mod you rerun the Magnetar Collapse from a lit magnetar (charge again, no core); here Relight skips it.`
    : `The Milky Way is lost: ${reason}. In the mod you rerun the Galactic Merger from a lit Triangulum (charge again, no core); here Relight skips it.`;
}
function transFailText(S,reason){
  const tr=S.trans;
  if(tr.kind==='mc') return `Magnetar Collapse failed (${reason}). The magnetar is lost and the charge (WDM 18,432 L + BDM 4,608 L) is spent. The Intergalactic Upgrade Core stays in escrow.`;
  return `Galactic Merger failed (${reason}). Triangulum is lost and the Ergomatter charge is spent. The Supermassive Seed Core stays in escrow.`;
}
/* ---- satellite crossing (T8): exact 120 s countdown, a decaying P kick for 300 s, caught if Stable or better throughout.
   This was the T8 tidal disruption event; the TDE proper moves to T9, whose object is the central black hole. ---- */
function tdeTick(S,E,q){
  const td=S.tde; if(!td || S.trans) return 0;
  const T=TIERS[S.tier];
  if(!td.warned && td.left===0 && S.t>=td.next-T.tde.warn){ td.warned=true; S.mark|=1; log(S,'Event channel: satellite crossing in 120 s (a dwarf galaxy reaches the disc plane). Ready the Space rig, or lower the feed.','warn'); }
  if(td.warned && td.left===0 && S.t>=td.next){ td.left=T.tde.dur; td.tau=0; td.ok=true; td.warned=false; S.mark|=2; log(S,'Satellite crossing: the dwarf galaxy plunges through the disc. Its gas heats P by 12·(1 + τ/10)^(−5/3) per s for 300 s.','warn'); }
  if(td.left>0) E.P=(E.P||0)+12*Math.pow(1+td.tau/10,-5/3)*q;
  return 0;
}
function tdePost(S){
  const td=S.tde; if(!td || td.left<=0) return;
  if(S.zone>1) td.ok=false;
  td.tau++; td.left--;
  if(td.left===0){
    td.n++;
    if(td.ok){ const y=T8_RATIO*S.cfg.f8*60*S.cfg.r; td.caught++; add(S.L.prod,'Superdense Ergomatter',y); S.mark|=8;
      log(S,`Crossing caught: Stable or better for all 300 s. Bonus cycle +${fmtL(y)} Superdense Ergomatter.`,'ok'); }
    else log(S,'Crossing missed: the field left Stable during the 300 s. No bonus.','warn');
    td.next=S.t+1+uni(S,TIERS[8].tde.every);
  }
}
/* ---- transitions ---- */
function transDrift(S,dp,E,q){
  const tr=S.trans;
  if(tr.kind==='mc'){
    if(tr.phase===1){ const k=2*(tr.st/300); dp.Om+=k; dp.Pi+=k; if(tr.st>=270) return 1; }
    return 0;
  }
  let w=0;
  for(let i=0;i<3;i++){
    const pt=tr.passes[i], sg=tr.signs[i];
    if(tr.st===pt-60){ S.mark|=1; log(S,`Event channel: pericentre pass ${i+1} of 3 in 60 s, sign ${sg>0?'+':'−'} (W ${sg>0?'+':'−'}12/s and P +5/s for 5 s).`,'warn'); }
    if(tr.st>=pt-60 && tr.st<pt) w=sg;
    if(tr.st>=pt && tr.st<pt+5){ E.W=(E.W||0)+12*sg*q; E.P=(E.P||0)+5*q; if(tr.st===pt){ S.mark|=2; log(S,`Pericentre pass ${i+1}: the nuclei swing past each other.`,'warn'); } }
  }
  return w;
}
function collapse(S){
  const tr=S.trans, G=S.g.Geye, SH=S.cfg.shock||80, cancel=Math.min(SH,10*Math.max(0,G-D7)), shock=SH-cancel, P0=S.x.P;
  S.tier=7; S.x={P:clamp(P0+shock,-120,120),Th:0,Om:0,Pi:0,W:0,L:0}; S.A=0; S.I=Math.max(0,S.I-20); S.lock=0; S.mark|=2;
  tr.shock={P0,P:S.x.P,G,cancel}; tr.phase=3; tr.st=0; tr.streak=0;
  log(S,`Collapse: the magnetar passes its TOV limit and becomes a black hole. Θ, Ω, Π and A retire; W appears at 0. Bursts that second ${G.toFixed(2)} Bu/t cancel ${cancel.toFixed(0)} of the +${SH} shock: P ${fmtS(P0)} → ${fmtS(S.x.P)}. Integrity −20.`,'warn');
  if(S.x.P>100){ fail(S,`P ${fmtS(S.x.P)} after the collapse shock`); return; }
  S.ev={next:S.t+1+450,left:0,warned:false,sign:1}; S.prec=1;
  S.cyc={eyeT:0, ySum:0, n:0, rMin:1, hsm:0, bSum:0, f8:0};
  log(S,'Halo capture, 600 s: demand ramps 0 → 1.5 Bu/t and W drift ramps in. A forced minor merger comes at 450 s. Hold Stable or better for 60 s inside 420-600 s.','gold');
}
function transTick(S){
  const tr=S.trans, C=S.cfg, L=S.L;
  if(tr.kind==='mc'){
    if(tr.phase===1){
      add(L.up,'White Dwarf Matter',61.44); add(L.up,'Black Dwarf Matter',15.36);
      tr.st++; tr.Mns=2.0+0.3*Math.min(1,tr.st/300);
      if(tr.st===270){ S.mark|=1; log(S,'Event channel: collapse in 30 s. Time a purge so the eruption lands on the collapse second.','warn'); }
      if(tr.st>=300) collapse(S);
      return;
    }
    tr.st++;
    if(tr.st>=420) tr.streak = S.zone>=0 && S.zone<=1 ? tr.streak+1 : 0;
    if(tr.streak>=60){
      S.trans=null; C.unlocked7=true; C.coreMC=false; S.mark|=2;
      log(S,`Halo captured at ${tr.st} s. Intergalactic Upgrade Core consumed, T7 unlocked permanently, Triangulum is lit.`,'ok');
      S.banner={kind:'good',text:'Magnetar Collapse complete: Triangulum is lit and T7 is unlocked. Throats can now run on Ergomatter.',act:null};
    } else if(tr.st>=600) fail(S,'the halo was not captured: no 60 s Stable streak inside 420-600 s');
    return;
  }
  // Galactic Merger
  add(L.up,'Ergomatter',5);
  tr.st++;
  if(tr.st===600 && S.tier===7){ S.tier=8; S.x.L=0; S.tde=null; log(S,'First pass: the second nucleus is bound. Λ (tidal shear) appears at 0, its drift ramping in over 600 s.','gold'); }
  if(tr.st>1800){
    tr.phase=2; tr.streak = S.zone>=0 && S.zone<=1 ? tr.streak+1 : 0;
    if(tr.streak>=120){
      S.trans=null; C.unlocked8=true; C.coreGM=false; S.mark|=2; light(S);
      log(S,'Merger complete: the Milky Way is lit. Supermassive Seed Core consumed, T8 unlocked permanently.','ok');
      S.banner={kind:'good',text:'Galactic Merger complete: the Milky Way is lit and T8 is unlocked.',act:null};
    } else if(tr.st>=2100) fail(S,'no 120 s Stable streak inside 300 s after the last pass');
  }
}
function beginMC(S){ S.trans={kind:'mc', phase:1, st:0, Mns:2.0, streak:0, shock:null};
  log(S,'Magnetar Collapse begun: Intergalactic Upgrade Core in escrow. Overfeeding WDM 61.44 L/s + BDM 15.36 L/s for 300 s; the field winds up (Ω, Π +2·t/300).','gold'); }
function beginGM(S){
  const sg=()=>S.rng()<0.5?-1:1;
  S.trans={kind:'gm', phase:1, st:0, streak:0, passes:[600,1200,1800], signs:[sg(),sg(),sg()]};
  S.ev={next:1e15,left:0,warned:false,sign:1}; S.tde=null;
  log(S,'Galactic Merger begun: Supermassive Seed Core in escrow. Demand ramps 1.5 → 6 Bu/t over 1,800 s; the Ergomatter charge feeds at 5 L/s. Three warned pericentre passes.','gold');
}
function demandNow(S){
  const tr=S.trans;
  if(tr && tr.kind==='mc') return tr.phase===3 ? D7*Math.min(1,tr.st/600) : D7;
  if(tr && tr.kind==='gm') return 1.5+4.5*Math.min(1,tr.st/1800);
  return S.tier>=7 ? demandT(S.tier)*demandFactor(S) : D7;
}
// planets mode draws a quarter of D_T; each companion adds a quarter (revision 2026-10-13)
function demandFactor(S){ const T=TIERS[S.tier]; return T.slots ? 0.25 : 1+0.25*(T.comp||0); }
// the β normaliser: D_T, except during halo capture, where it is D_7 (finding F-1: β = G/D(t) diverges as D → 0)
function normDemand(S){ const tr=S.trans; return tr && tr.kind==='mc' && tr.phase===3 ? D7 : demandNow(S); }

/* ---- the Quasar Engine: its own hole and the accretion disc field (E, Φ) ---- */
const GASES={H:{name:'Hydrogen',k:0.4}, He:{name:'Helium',k:0.2}, RSM:{name:'Raw Star Matter',k:0.1}};
const COILS=[null,{name:'Infinity',cap:256},{name:'Transcendent Metal',cap:1024},{name:'SpaceTime',cap:2048},{name:'MagMatter',cap:4096}];
function aEq(l,b){ return l>0 ? l/(l/0.998+0.1091*b*b) : 0; }
function iscoEta(a){ const z1=1+Math.cbrt(1-a*a)*(Math.cbrt(1+a)+Math.cbrt(1-a)); const z2=Math.sqrt(3*a*a+z1*z1); const r=3+z2-Math.sqrt((3-z1)*(3+z1+2*z2)); return 1-Math.sqrt(1-2/(3*r)); }
function newQuasar(QC,seed){
  return {rng:mulberry((seed*40503+7)>>>0), M:QC.M0, a:aEq(Math.min(QC.f,1),Math.min(QC.b,QC.coil)), E:0, Phi:0, I:100, lock:0, zone:0, Yq:1.25,
    erupt:0, eruptG:0, state:'OPEN', reform:0, lagE:0, lagPhi:0, G0:0, G:0, lam:1, lc:1, fEff:QC.f, feedLs:0, uGas:0, uSpace:0,
    purgeReq:false, forcePurge:false, hold:false, pulseG:0, pulseS:0, flash:0, lastKind:'',
    L:{gas:0, gasTrim:0, space:0, coilEU:0, zoneT:[0,0,0,0,0], ySum:0, n:0, fails:0, eruptions:0, purges:0, bu:0},
    ms:{firstLight:false, edd:0, eddDone:false, firstErupt:false}};
}
function quasarStep(W){
  const Q=W.Q, QC=W.QC, auto=W.CFG.auto&&RIG.disc, rng=Q.rng;
  const b = QC.power ? Math.min(QC.b,QC.coil) : 0;
  const Medd=40*Q.M/GASES[QC.gas].k, F=QC.stocked?QC.f*Medd:0, fEff=F/Medd;
  Q.feedLs=F; Q.L.gas+=F; Q.fEff=fEff; Q.b=b; Q.Medd=Medd;
  Q.coilEU=2*UXV_V*b*b; Q.L.coilEU+=Q.coilEU*20;
  if(Q.flash>0) Q.flash--;
  if(Q.state==='REFORM'){
    if(fEff>=0.3) Q.reform++;
    Q.G=0; Q.G0=0; Q.erupt=0; Q.zone=-1; Q.Yq=0; Q.uGas=0; Q.uSpace=0;
    if(Q.reform>=300){ Q.state='OPEN'; Q.E=0; Q.I=100; Q.Phi=0; Q.lock=0; Q.reform=0; log(W.S,'Quasar: the accretion disc has re-formed (E 0, Integrity 100). Jets relit.','ok'); }
    Q.lagE=Q.E; Q.lagPhi=Q.Phi; return;
  }
  let uG=0,uS=0;
  if(auto){ if(Q.lagE<-5) uG=10; else if(Q.lagE>5) uS=10; }
  else { uG=QC.manGas; uS=QC.manSpace; }
  if(Q.pulseG>0){ uG=10; Q.pulseG--; } if(Q.pulseS>0){ uS=10; Q.pulseS--; }
  Q.uGas=uG; Q.uSpace=uS; Q.L.gasTrim+=uG*1000*Q.M/64; Q.L.space+=uS*100*Q.M/64;
  Q.E=clamp(Q.E+(100*(fEff-1)-Q.E)/30+0.04*Math.max(Q.E,0)+(rng()*2-1)*2+2*uG-2*uS,-120,120);
  const lam=Math.max(0,1+Q.E/100), lc=Math.min(lam,1); Q.lam=lam; Q.lc=lc;
  Q.a+=(aEq(lc,b)-Q.a)/1800;
  const z=zoneOf(Math.abs(Q.E)); Q.zone=z; Q.L.zoneT[z]++;
  Q.lock = z===0 ? Q.lock+1 : 0;
  Q.I=clamp(Q.I+ZONES[z].dI,0,100);
  if(z===4 || Q.I<=0){
    const why = z===4 ? (Q.E>0?`over-Eddington runaway, E ${fmtS(Q.E)}`:`starved, E ${fmtS(Q.E)}`) : 'disc Integrity reached 0';
    Q.L.fails++; Q.state='REFORM'; Q.reform=0; Q.Phi=0; Q.G=0; Q.G0=0; Q.erupt=0; Q.zone=-1; Q.Yq=0; Q.lagE=Q.E; Q.lagPhi=0; Q.flash=0;
    log(W.S,`Quasar disc blow-out (${why}). Φ dumped, jets dark. The disc re-forms over 300 s once the feed is at least 0.3. M and a are kept.`,'bad');
    W.pauseReq=true; return;
  }
  const Yq=(z===0 && Q.lock>=600)?1.5:ZONES[z].Y; Q.Yq=Yq; Q.L.ySum+=Yq; Q.L.n++;
  const G0=lc*Q.a*Q.a*b*b*Math.pow(Q.M/64,2); Q.G0=G0;
  Q.Phi=Math.min(100,Q.Phi+b*lc/2.4);
  let purge=false;
  if(auto && QC.purgeAt>0 && !Q.hold && Q.lagPhi>=QC.purgeAt) purge=true;
  if(Q.purgeReq){ purge=true; Q.purgeReq=false; }
  if(Q.forcePurge){ purge=true; Q.forcePurge=false; }
  if(Q.Phi>=100 || (purge && Q.Phi>=20)){
    const full=Q.Phi>=100; Q.lastKind=full?'eruption':'purge';
    Q.erupt=3; Q.eruptG=G0*Yq*3*(Q.Phi/100); Q.E=clamp(Q.E-30*Q.Phi/100,-120,120); Q.a-=0.002*Q.Phi/100; Q.Phi=0; Q.flash=3;
    if(full) Q.L.eruptions++; else Q.L.purges++;
    if(!Q.ms.firstErupt){ Q.ms.firstErupt=true; log(W.S,'Quasar milestone: First Eruption.','ok'); }
  }
  let G=G0*Yq*(0.5+Q.Phi/100); if(Q.erupt>0){ G+=Q.eruptG; Q.erupt--; }
  Q.G=G; Q.L.bu+=G;
  const cap=COILS[QC.coil].cap;
  if(Q.M<cap) Q.M=Math.min(cap,Q.M+6.4e-8*(1-iscoEta(clamp(Q.a,0,0.998)))*lc*Medd);
  if(!Q.ms.firstLight && G>0){ Q.ms.firstLight=true; log(W.S,'Quasar milestone: First Light.','ok'); }
  if(z===0 && lam>=0.95) Q.ms.edd++; else Q.ms.edd=0;
  if(!Q.ms.eddDone && Q.ms.edd>=3600){ Q.ms.eddDone=true; log(W.S,'Quasar milestone: Eddington (1 h Resonant at λ ≥ 0.95).','ok'); }
  Q.lagE=Q.E; Q.lagPhi=Q.Phi;
}

/* ---- the Einstein-Rosen Conduit: mouths, combine, J1 (Eye : crown), J3 (halo : rail, T8), J2 (high : low) ---- */
const THROAT=[null,{name:'Mk I',cap:24,st:4,erg:0.25},{name:'Mk II',cap:384,st:16,erg:1},{name:'Mk III',cap:6144,st:64,erg:4}];
const CROWN=[null,{name:'Mk I',cap:32},{name:'Mk II',cap:512},{name:'Mk III',cap:8192}];
const lvlOf=(v,w)=>Math.round(15*clamp((v+w)/(2*w),0,1));
const shareOf=(c,k,s)=>clamp(c+k*(s/15-0.5),0,1);
function conduitStep(W){
  const S=W.S, Q=W.Q, K=W.K, st=W.Kst, wired=W.CFG.auto&&RIG.aim;
  const half=Q.G/2, cap=THROAT[K.throat].cap;
  const nMouthPorts=(K.top==='mouth')+(K.bot==='mouth');
  const planets=!!(S.tier>=7 && TIERS[S.tier].slots);
  const t8=(S.tier>=8 && !planets) || (S.trans&&S.trans.kind==='gm');
  const test8=S.tier===7 && !planets && !S.trans && K.test8;   // T7: J3 test-fires the halo ports for the Heavy Link
  const el={mouths:2*nMouthPorts, comb:nMouthPorts===2?1:0, j1:nMouthPorts?1:0, j3:nMouthPorts&&(t8||test8)?1:0, j2:nMouthPorts?1:0};
  el.n=el.mouths+el.comb+el.j1+el.j3+el.j2;
  const ergOK=W.CFG.unlocked7||S.tier>=7;
  const fuel = K.fuel==='erg' && ergOK ? 'erg' : 'st';
  if(!K.fuelOK){ if(st.open && el.n) log(S,'Conduit upkeep short: every element closes and its streams stop.','bad'); st.open=false; st.reopen=0; }
  else if(!st.open){ st.reopen++; if(st.reopen>=10){ st.open=true; if(el.n) log(S,'Conduit upkeep restored for 10 s: elements reopen.','ok'); } }
  const rate=fuel==='erg'?THROAT[K.throat].erg:THROAT[K.throat].st;
  st.fuelLs = K.fuelOK ? el.n*rate : 0; st.fuel=fuel; st.el=el;
  W.KL[fuel==='erg'?'Ergomatter':'Molten SpaceTime']=(W.KL[fuel==='erg'?'Ergomatter':'Molten SpaceTime']||0)+st.fuelLs;
  let sIn=0, clip=0, vent=0; const ports={};
  for(const p of ['top','bot']){
    const d=K[p];
    if(d==='crown') ports[p]={dock:'crown', s:half};
    else if(d==='mouth'){ if(st.open){ const v=Math.min(half,cap); clip+=half-v; sIn+=v; ports[p]={dock:'mouth',s:v}; } else { vent+=half; ports[p]={dock:'mouth',s:0,closed:true}; } }
    else { vent+=half; ports[p]={dock:'vent',s:half}; }
  }
  if(el.comb && st.open){ const v=Math.min(sIn,cap); clip+=sIn-v; sIn=v; }
  const pe = S.state==='LIT' ? S.prevE : {};
  const P=pe.P||0, Wg=pe.W||0, Lg=pe.L||0;
  // two-level route (AE2 has no analog output): each Junction reads two Directive bits as 15 / 8 / 0
  const tri=(hi,lo)=>hi?15:lo?0:8, dr=W.dir||{};
  let s1=K.twoLevel?tri(dr.field&2,dr.field&1):K.inv1?lvlOf(-P,K.win):lvlOf(P,K.win); if(W.force1) s1=15;
  const k1 = (wired && S.tier>=7) || W.force1 ? K.k1 : 0;
  const sh1=test8?1:shareOf(K.c1,k1,s1), out3=K.p3*sIn, eye=sh1*(1-K.p3)*sIn, crownLeg=(1-sh1)*(1-K.p3)*sIn+out3;
  let halo=0, rail=eye, s3=0, sh3=0, dump=0;
  if(el.j3 && S.tier>=8 && !planets){ s3=K.twoLevel?tri(dr.event&1,dr.event&2):lvlOf(Lg,K.win); sh3=shareOf(K.c3,wired?K.k3:0,s3); halo=eye*sh3; rail=eye-halo; }
  else if(el.j3 && test8){ sh3=K.c3t; halo=eye*sh3; rail=eye-halo; dump=halo; }   // T7 halo ports: counted, no field effect
  const s2=K.twoLevel?tri(dr.field&4,dr.field&8):lvlOf(Wg,K.win), sh2=shareOf(K.c2,wired?K.k2:0,s2), hi=rail*sh2, lo=rail-hi;
  // crowns: on each docked jet cap, and the power plant on J1's crown leg
  const ccap=CROWN[K.mk].cap*(K.stack?2:1); let eu=0, cclip=0; const sites=[];
  const site=(name,s)=>{ const used=Math.min(s,ccap); eu+=1e13*used; cclip+=s-used; sites.push({name,s,used}); };
  if(ports.top.dock==='crown') site('Top jet cap',half);
  if(ports.bot.dock==='crown') site('Bottom jet cap',half);
  if(el.j1) site('Power plant (J1 crown leg)',crownLeg);
  W.flow={half, ports, sIn, clip, vent, s1, sh1, eye, crownLeg, out3, s3, sh3, halo, rail, s2, sh2, hi, lo, sites, eu, cclip, ccap, cap, el, open:st.open, fuel, test8, dump};
  W.KL.eu=(W.KL.eu||0)+eu*20; W.KL.vent=(W.KL.vent||0)+vent+cclip; W.KL.clip=(W.KL.clip||0)+clip;
  // the Eye's burst inputs
  const mm=S.trans?1:Math.min(S.cfg.m,mMax(S)), Dn=normDemand(S);
  S.g={Geye:eye, hi, lo, halo, D:demandNow(S), Dn, beta:(eye-dump)/(Dn*mm), gHi:hi/(Dn*mm), gLo:lo/(Dn*mm), gHalo:(halo-dump)/(Dn*mm)};
  // Machine unlocks (revision 2026-10-13): a linked quasar actively stabilising the Eye is the core of each milestone.
  const linked = st.open && Q.state==='OPEN' && Q.b>0 && nMouthPorts>0;
  W.linked=linked;
  const lk=W.link; roll(lk,rail,hi);
  if(S.tier===6 && !S.trans && !lk.done){
    const ok=linked && lk.n>=60 && lk.mean>=D7 && lk.frac>=0.25 && lk.frac<=0.75;
    lk.streak=ok?lk.streak+1:0; lk.best=Math.max(lk.best,lk.streak);
    if(lk.streak>=LINK_S){ lk.done=true; log(S,`Milestone: Stability Link held for 30 min (60 s mean ${lk.mean.toFixed(2)} Bu/t on the rail, high bank ${(lk.frac*100).toFixed(0)} %). The Magnetar Collapse can begin.`,'ok'); } }
  const hk=W.link8; roll(hk,eye,halo);
  if(S.tier===7 && !planets && !S.trans && !hk.done){
    const ok=linked && test8 && hk.n>=60 && hk.mean>=demandT(8) && hk.frac>=0.5;
    hk.streak=ok?hk.streak+1:0; hk.best=Math.max(hk.best,hk.streak);
    if(hk.streak>=LINK_S){ hk.done=true; log(S,`Milestone: Heavy Link held for 30 min (60 s mean ${hk.mean.toFixed(2)} Bu/t into the Eye, ${(hk.frac*100).toFixed(0)} % test-fired into the halo). The Galactic Merger can begin.`,'ok'); } }
}
const LINK_S=1800;   // 30 min unbroken
function newLink(){ return {r:new Float64Array(60), h:new Float64Array(60), sum:0, p:0, n:0, mean:0, frac:0, streak:0, best:0, done:false}; }
function roll(o,v,part){ const i=o.n%60; o.sum+=v-o.r[i]; o.p+=part-o.h[i]; o.r[i]=v; o.h[i]=part; o.n++; o.mean=Math.max(0,o.sum/60); o.frac=o.sum>1e-9?o.p/o.sum:0; }
/* ---- the world: one tick = quasar, conduit, Eye ---- */
function newWorld(tier,CFG,QC,K){
  setGalaxyTiers(CFG);
  const S=newState(tier,CFG);
  return {S, CFG, QC, K, Q:newQuasar(QC,CFG.seed), Kst:{open:true,reopen:0,fuelLs:0}, KL:{}, flow:null, force1:false, pauseReq:false,
    link:newLink(), link8:newLink(), linked:false, dir:{disc:0,field:0,event:0,trans:0}};
}
/* ---- Directive Hatches (28127): 4-bit codes, read as one analog strength (Degasser style) or one bit per face ---- */
function readyToBegin(W){ const S=W.S, C=W.CFG; if(S.trans || S.state!=='LIT' || S.zone<0 || S.zone>1) return false;
  if(S.tier===6) return W.link.done && C.coreMC && C.bufMC;
  if(S.tier===7 && !TIERS[7].slots) return W.link8.done && C.coreGM && C.bufGM;
  return false; }
// Transition set (bit n has value 2^(n−1)): bit 1 ready to begin, bit 2 running, bit 3 purge now, bit 4 hold the regulator's purges
function transBits(W){
  const S=W.S, Q=W.Q, tr=S.trans; let b=0;
  if(tr) b|=2; else if(readyToBegin(W)) b|=1;
  if(tr && tr.kind==='mc' && tr.phase===1){
    if(tr.st>=219 && tr.st<299){ b|=8; if(Q.Phi>=20 && Q.Phi+(Q.b||0)/2.4*(299-tr.st)>=100) b|=4; }
    if(tr.st===299) b|=4;
  }
  return b;
}
function worldTick(W){
  const S=W.S, Q=W.Q, auto=W.CFG.auto&&RIG.assist;
  const tb=transBits(W); W.dir.trans=tb;
  Q.hold = auto && !!(tb&8);                    // the regulator purge is Disc bit 3 AND NOT Transition bit 4
  if(auto && (tb&4)) Q.forcePurge=true;         // Transition bit 3 → purge
  W.force1 = auto && !!(tb&4);                  // ... OR'ed into J1's Burst Control Hatch
  quasarStep(W); conduitStep(W); step(S);
  const e=S.state==='LIT'?S.prevE:{}, f=W.flow||{}, P=e.P||0, Wg=e.W||0, Lg=e.L||0;
  W.dir.disc=(Q.lagE<-5?1:0)|(Q.lagE>5?2:0)|(W.QC.purgeAt>0&&Q.lagPhi>=W.QC.purgeAt?4:0)|((Q.state!=='OPEN'||!Q.b||(f.vent||0)>0.005||!W.Kst.open)?8:0);
  W.dir.field=S.tier>=7&&S.state==='LIT'?(P>5?1:0)|(P<-5?2:0)|(Wg>5?4:0)|(Wg<-5?8:0):0;
  const ew=S.warn||((S.tde&&S.tde.warned)?1:0);
  W.dir.event=S.tier>=7&&S.state==='LIT'?(Lg>5?1:0)|(Lg<-5?2:0)|(ew?4:0)|(ew<0?8:0):0;
}
/*P4-END*/
