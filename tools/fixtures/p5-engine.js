// p5-engine.js — FIXTURE for tools/p5-golden.js. Do not edit by hand:
// regenerate with `node tools/p5-golden.js --refresh-fixture`, then regenerate the golden JSON.
// The P5 block of the P5 play prototype, verbatim (the reference for the Java Eye T9 / T10 maths). It runs on top
// of the shared ENGINE block (with P5's one changed line) and the P4 block (tools/fixtures/quasar-engine.js).
// source: designs/p5-prototype.html (design repo GTNH-Addons)
// source-sha1: 6f9e1d8846343f36464cfd598ceb8d3f43ccf800
// engine-sha1: c28c22097debf085fd21226aed3f656c7706d04b
// p4-sha1: 8bc2943b9f93db5da721d4469b63bddb3da7b1f2
// p5-sha1: f158517cd247cb5fcd12ebac34d017bc1c111bb3
/*P5-START*/
/* P5 extension: Eye T9 (the Great Attractor: gauge V, the pellet beam, Galactic Flow, The Infall) and Eye T10 (the
   Universe: gauge H, one universe per batch, Recombination, Cosmic Dawn, Inflation). Uses the D-49 engine and the P4
   block above through their hooks (T.d, T.bx, T.sigK, T.perSec, T.cycOut) and by wrapping five functions
   (transTick, transDrift, fail, demandNow, normDemand). Every number is a tuning knob (designs/docs/p5-t9-t10.md). */
GAUGE.V={sym:'V',name:'Infall'}; GAUGE.H={sym:'H',name:'Expansion'};
const D9=demandT(9), D10=demandT(10);          // 24 and 96 Bu/t (GalaxyField.demandT, unchanged)
const PI9=1e10;                                // the Attractor's drive: 10^10 GeV/s holds the steady flow (rho = 1)
const SPOOL=20;                                // s: the five collars spool the beam toward its setpoint
const DM_CYCLE=144;                            // L of Dark Matter per 60 s cycle at Y 1 (one galaxy's halo, stripped)
const PELLETS={mag:{name:'Magnetar Pellet',short:'Magnetar',e:1.25e9}, erg:{name:'Ergomatter Pellet',short:'Ergomatter',e:5e9}, sup:{name:'Superdense Pellet',short:'Superdense',e:2e10}};
const INJ=[null,{name:'Mk I',cap:4},{name:'Mk II',cap:8},{name:'Mk III',cap:16}];
const GFLOW={every:[1200,2400], warn:180, dur:240, min:3, max:12};
const V_GRAV=2, V_BEAM=2;                      // dV = 2·phi − 2·rho: the beam cancels the infall at rho = phi
const flowBell=tau=>Math.pow(Math.sin(Math.PI*tau/GFLOW.dur),2);
const rhoMax=C=>INJ[C.inj].cap*PELLETS[C.grade].e/PI9;
// the hold rating: the largest group the beam can hold. Group n adds A = 0.25·n to phi at the peak; the beam needs
// rho_max ≥ 1 + A + 1 (one unit of margin for the noise, the runaway gain and the 20 s spool). Capped at 12.
const holdRating=C=>Math.max(0,Math.min(GFLOW.max,Math.floor(4*(rhoMax(C)-2))));
const centreFor=C=>clamp(PI9/(PELLETS[C.grade].e*INJ[C.inj].cap),0,1);   // the injector centre that fires rho = 1 at V = 0

// galaxy companions at T9 (proposal Q-P5-7): Triangulum and the Milky Way may ride the Attractor's flow
COMP[7]={name:'Triangulum (T7)', out:['Ergomatter',600], up:[['Magnetar Matter',7.5]], galaxy:true};
COMP[8]={name:'Milky Way (T8)', out:['Superdense Ergomatter',300], up:[['Ergomatter',300]], galaxy:true};

function newFlow(S){ return {next:S.t+uni(S,GFLOW.every), warned:false, left:0, tau:0, sign:1, size:0, ok:0, accepted:false, forced:false}; }
function beamTick(S){
  const C=S.cfg, B=S.beam||(S.beam={B:0,set:0,rho:0,n:0,s:8});
  const e=PELLETS[C.grade].e, cap=INJ[C.inj].cap, V=(S.prevE&&S.prevE.V)||0;
  let s;
  if(C.auto && C.beamRig) s = C.beamTwoLevel ? (V>5?15:V<-5?0:8) : lvlOf(V,C.bwin);   // the V sensor, straight to the injector
  else s = C.bman;
  if(C.preFire && S.flow && S.flow.warned && S.flow.accepted && S.t>=S.flow.next-SPOOL) s=Math.max(s,12);
  const c = C.bcAuto ? centreFor(C) : C.bc;
  let n = cap*clamp(c + C.bk*(s/15-0.5),0,1);
  if(!C.pelletsOK) n=0;
  B.s=s; B.n=n; B.set=n*e; B.B+=(B.set-B.B)/SPOOL; B.rho=B.B/PI9;
  add(S.L.up,PELLETS[C.grade].name,n); S.L.pellets=(S.L.pellets||0)+n;
  return B;
}
function flowTick(S){
  const C=S.cfg, tr=S.trans; let F=S.flow; if(!F) F=S.flow=newFlow(S);
  const out={A:0,sign:F.sign,warn:0};
  if(tr && tr.kind==='in' && !F.forced) return out;           // during the Infall only the forced flow runs
  if(!F.warned && F.left===0 && S.t>=F.next-GFLOW.warn){
    F.warned=true; F.sign=S.rng()<0.5?-1:1; F.size=F.forced?6:Math.round(GFLOW.min+(GFLOW.max-GFLOW.min)*S.rng());
    F.accepted = F.forced || F.size<=C.acceptMax; S.mark|=1;
    log(S,`Event channel: Galactic Flow in ${GFLOW.warn} s: a group of ${F.size} galaxies down the ${F.sign>0?'high':'low'}-side filament. ${F.accepted?'Accepted: the field will hold it.':'Declined (larger than the accept limit '+C.acceptMax+'): it streams past.'}`,'warn');
  }
  if(F.warned && F.left===0 && S.t>=F.next){
    F.warned=false; S.mark|=2;
    if(F.accepted){ F.left=GFLOW.dur; F.tau=0; F.ok=0; log(S,`Galactic Flow: ${F.size} galaxies stream into the core. φ rises to ${(1+0.25*F.size).toFixed(2)} at the peak.`,'warn'); }
    else { F.next=S.t+1+uni(S,GFLOW.every); log(S,`The group of ${F.size} streams past along the filament. Nothing is captured and nothing is lost.`,''); }
  }
  if(F.left>0){ out.A=0.25*F.size*flowBell(F.tau)*S.q; out.sign=F.sign; }
  if(F.warned && F.accepted) out.warn=F.sign;
  return out;
}
function flowPost(S){
  const F=S.flow; if(!F || F.left<=0) return;
  if(S.zone>=0 && S.zone<=1) F.ok++;
  F.tau++; F.left--;
  if(F.left===0){
    const frac=F.ok/GFLOW.dur, cap=frac>=1?F.size:Math.floor(F.size*frac), y=DM_CYCLE*cap*S.cfg.r;
    S.L.flows=(S.L.flows||0)+1; if(frac>=1) S.L.caught=(S.L.caught||0)+1;
    S.L.gal=(S.L.gal||0)+cap; add(S.L.prod,'Dark Matter',y); S.mark|=8;
    log(S, frac>=1 ? `Flow held: all ${F.size} galaxies captured, +${fmtL(y)} Dark Matter from their halos.` : `Flow partly held (${Math.round(frac*100)} % of the time Stable or better): ${cap} of ${F.size} captured, +${fmtL(y)} Dark Matter.`, frac>=1?'ok':'warn');
    if(!F.forced) S.prec=F.sign; F.forced=false;
    F.next=S.t+1+uni(S,GFLOW.every);
    if(S.trans && S.trans.kind==='in') S.trans.flowDone=true;
  }
}
function attractorTier(comp){
  const nc=comp.length, ce=comp.reduce((a,k)=>a+(COMP[k].galaxy?2:1),0), cu=compUp(9,comp);
  return {n:9, star:'Great Attractor', stage:'The hidden mass at the heart of Laniakea: galaxies stream down the cosmic web into a dark core',
    gauges:['P','W','L','V'], sig:1, sigK:{P:1+0.25*nc,W:1.5,L:1.5,V:1.5}, g:{P:-0.01,W:0.02,L:0.02,V:0.01},
    trims:{Space:{cap:10,fx:{P:-2}}}, compList:comp, comp:ce,
    prod:{name:'Dark Matter',base:DM_CYCLE}, up:cu, attractor:true,
    d:S=>{
      S.q=1-0.05*(S.cfg.ts-1);
      const tr=S.trans, vr = tr && tr.kind==='in' ? Math.min(1,tr.st/600) : 1;
      beamTick(S); const fl=flowTick(S); S.flowOut=fl; S.vr=vr;
      const phi=1+fl.A;
      return {P:3*(S.g.beta-1)+0.5*fl.A, W:1.5*S.prec+0.5*fl.sign*fl.A, L:(1+phi), V:V_GRAV*phi*vr};
    },
    bx:(S,k)=> k==='W' ? 6*(S.g.gLo-S.g.gHi) : k==='L' ? -10*S.g.gHalo : k==='V' ? -V_BEAM*S.beam.rho*S.vr : 0,
    ctrl:E=>spaceRig(E), rig:()=>rigLines(true),
    perSec:S=>{ S.cyc.bSum+=S.g.beta; flowPost(S); if(S.flowOut && S.flowOut.warn && !S.warn) S.warn=S.flowOut.warn; },
    cycOut:(S,cy,mY)=>{ if(S.trans) return 0; compOut(S,cy,mY); if(mY>=1) S.L.gal=(S.L.gal||0)+1; return DM_CYCLE*mY*cy.rMin; }};
}

/* ---- Eye T10: the Universe. One universe per 7,200 s batch. H is the expansion error against the Friedmann curve
   the crown is told to follow. ---- */
const U_RIG={on:true, dm:2, st:2};   // the Universe rig: one binary Dark Matter valve and one SpaceTime valve, 2 units/s each
const U_BATCH=7200, U_OUT=9216, U_CHARGE=9216, U_DAWN_BONUS=0.125;
const U_REC={at:1800, warn:60, dur:300};       // Recombination: the CMB is released
const U_DAWN={win:[3240,4680], warn:120, dur:300};
function deDrift(t){ const x=clamp((t/U_BATCH-0.6)/0.2,0,1); return 0.4+1.2*x*x*(3-2*x); }   // dark energy takes over at 60-80 %
function universeTier(){
  return {n:10, star:'The Universe', stage:'The observable universe, run from its first light to its last: keep it flat',
    gauges:['P','H'], sig:1, sigK:{P:1,H:1.5}, g:{P:-0.01,H:0.01},
    trims:{Space:{cap:10,fx:{P:-2}}, DM:{cap:10,fx:{H:-2}}, ST:{cap:10,fx:{H:2}}},
    prod:{name:'Universium',base:0}, up:[], universe:true,
    d:S=>{
      S.q=1-0.05*(S.cfg.ts-1); const U=S.uni||(S.uni=newUniverse(S)), tr=S.trans; const out={P:3*(S.g.beta-1), H:0};
      if(tr && tr.kind==='inf'){ out.H = tr.phase>=2 ? 0.4 : 0; return out; }
      U.t++;
      out.H=deDrift(U.t);
      if(U.t>=U_REC.at && U.t<U_REC.at+U_REC.dur) out.H+=0.15;
      if(U.t===U_REC.at-U_REC.warn){ S.mark|=1; log(S,'Event channel: Recombination in 60 s. The universe turns transparent: the expansion drifts +0.15/s for 300 s and the pressure drops for 5 s.','warn'); }
      if(U.t>=U_REC.at && U.t<U_REC.at+5) out.P-=6*S.q;
      if(U.t===U.dawn-U_DAWN.warn){ S.mark|=1; log(S,'Event channel: Cosmic Dawn in 120 s. The first stars ignite in a starburst across the crown.','warn'); }
      if(U.t>=U.dawn && U.t<U.dawn+U_DAWN.dur){ const tau=U.t-U.dawn; out.P+=12*Math.pow(1+tau/10,-5/3)*S.q; out.H+=0.3; if(S.zone>1) U.dawnOk=false; if(tau===0){ S.mark|=2; log(S,'Cosmic Dawn: the starburst floods the crown. P +12·(1 + τ/10)^(−5/3) and H +0.3/s for 300 s.','warn'); } }
      return out; },
    ctrl:(E)=>{ const u=spaceRig(E); if(U_RIG.on){ if(E.H>5) u.DM=U_RIG.dm; if(E.H<-5) u.ST=U_RIG.st; } return u; },
    rig:()=>['J1 Eye : crown ← sensor P, analog, inverted, window ±w, k = 2 (all bursts land on the crown\'s 12 receptors)',
      'Eye Space trim: Field bit 1 (P > +5) → Space valve, 10 units of 100 L/s',
      'Universe bit 1 (H > +5) → Dark Matter valve, 2 units of 4 L/s; bit 2 (H < −5) → Molten SpaceTime valve, 2 units of 100 L/s'],
    perSec:S=>{ const U=S.uni; if(U && !S.trans && ((U.t>U_REC.at-U_REC.warn && U.t<U_REC.at) || (U.t>U.dawn-U_DAWN.warn && U.t<U.dawn))) S.warn=1; if(!U || S.trans) return; U.ySum+=S.Y; U.n++; U.rMin=Math.min(U.rMin,S.cfg.r);
      if(U.t>=U_BATCH){ const mY=U.ySum/U.n, base=U_OUT*mY*U.rMin, bonus=U.dawnOk?U_OUT*U_DAWN_BONUS*U.rMin:0;
        add(S.L.prod,'Universium',base+bonus); S.L.batches++; S.mark|=8; if(U.dawnOk) S.L.dawns=(S.L.dawns||0)+1;
        log(S,`Universe ${S.L.batches} ran to its last light: +${fmtL(base)} Universium (mean Y ${mY.toFixed(2)})${bonus?`, +${fmtL(bonus)} for catching Cosmic Dawn`:''}. The next universe is charged.`,'ok');
        S.uni=newUniverse(S); S.x.H=0; } },
    cycOut:()=>0};
}
function newUniverse(S){ add(S.L.up,'Dark Matter',U_CHARGE*S.cfg.r); return {t:0, ySum:0, n:0, rMin:1, dawn:Math.round(uni(S,U_DAWN.win)), dawnOk:true}; }

function setP5Tiers(cfg){
  TIERS[9] = cfg.mode==='planets' ? planetsTier(9) : attractorTier((cfg.comp9||[]).slice(0,3));
  TIERS[10] = universeTier();
  TRIM.DM={name:'Dark Matter',unit:4};
}
setP5Tiers({mode:'galaxy',comp9:[]});

/* ---- the T8 → T9 transition: The Infall (1,800 s) ---- */
function beginInfall(S){
  S.trans={kind:'in', phase:1, st:0, streak:0, flowDone:false};
  S.tier=9; S.x.V=0; S.flow={next:S.t+1200, warned:false, left:0, tau:0, sign:1, size:6, ok:0, accepted:true, forced:true};
  S.beam=S.beam||{B:0,set:0,rho:0,n:0,s:8};
  log(S,'The Infall begun: Attractor Seed Core in escrow. The Milky Way drops out of the Zone of Avoidance into a filament. V appears at 0 and its drift ramps in over 600 s; demand ramps 6 → 24 Bu/t over 1,800 s; a forced Galactic Flow of 6 comes at 1,200 s.','gold');
}
const _transTick=transTick, _transDrift=transDrift, _fail=fail, _demandNow=demandNow, _normDemand=normDemand, _nucleusFailText=nucleusFailText;
transDrift=function(S,dp,E,q){ const k=S.trans.kind; if(k==='in'||k==='inf') return 0; return _transDrift(S,dp,E,q); };
demandNow=function(S){ const tr=S.trans;
  if(tr && tr.kind==='in') return 6+18*Math.min(1,tr.st/1800);
  if(tr && tr.kind==='inf') return 24+72*Math.min(1,tr.st/900);
  if(S.tier>=9) return demandT(S.tier)*demandFactor(S);
  return _demandNow(S); };
normDemand=function(S){ const tr=S.trans; if(tr && (tr.kind==='in'||tr.kind==='inf')) return demandNow(S); return _normDemand(S); };
nucleusFailText=function(S,reason){
  if(S.tier===9 && !TIERS[9].slots) return `The Great Attractor lost its grip: ${reason}. The flow scatters back up the filaments. In the mod you rerun the Infall from a lit Milky Way (charge again, no core); here Relight skips it. Galaxies held so far are kept.`;
  if(S.tier===10) return `The universe collapsed: ${reason}.`;
  return _nucleusFailText(S,reason); };
fail=function(S,reason){
  const tr=S.trans;
  if(tr && tr.kind==='in'){   // soft fail (Q-P5-5): the Milky Way is thrown back out of the filament, still lit
    S.L.fails++; S.mark|=4; S.pauseReq=true;
    const text=`The Infall failed (${reason}). The Milky Way is thrown back out of the filament, still lit at T8 with Integrity 50. The Superdense charge is spent; the Attractor Seed Core stays in escrow.`;
    log(S,text,'bad'); S.banner={kind:'bad',text,act:null};
    S.trans=null; S.tier=8; S.flow=null; S.x={P:0,Th:0,Om:0,Pi:0,W:0,L:0,V:0}; light(S); S.I=50; return; }
  if(S.tier===10 && (!tr || tr.kind!=='inf')){   // a universe collapses: salvage 25 % × the fraction run, next universe charged
    const U=S.uni, frac=U?U.t/U_BATCH:0, y=U_OUT*0.25*frac; S.L.fails++; S.mark|=4; S.pauseReq=true;
    add(S.L.prod,'Universium',y);
    const text=`The universe ${S.x.H>0?'tore itself apart (Big Rip)':'recollapsed (Big Crunch)'}: ${reason}. ${fmtL(y)} Universium salvaged (25 % × ${Math.round(frac*100)} % run). The Eye stays at T10; the next universe is charged.`;
    log(S,text,'bad'); S.banner={kind:'bad',text,act:null};
    S.I=100; S.lock=0; S.x.H=0; S.x.P=0; S.uni=newUniverse(S); return; }
  if(tr && tr.kind==='inf'){
    S.L.fails++; S.mark|=4; S.pauseReq=true;
    const text=`Inflation failed (${reason}). The new universe never formed; the Great Attractor is relit at T9 with Integrity 50. The Dark Matter charge is spent; the Universal Upgrade Core stays in escrow.`;
    log(S,text,'bad'); S.banner={kind:'bad',text,act:null};
    S.trans=null; S.tier=9; S.x={P:0,Th:0,Om:0,Pi:0,W:0,L:0,V:0}; light(S); S.I=50; return; }
  return _fail(S,reason); };
transTick=function(S){
  const tr=S.trans, C=S.cfg, L=S.L;
  if(tr.kind==='in'){
    add(L.up,'Superdense Ergomatter',10); tr.st++;
    if(tr.st>=1500 && tr.flowDone) tr.streak = S.zone>=0 && S.zone<=1 ? tr.streak+1 : 0;
    if(tr.streak>=120){ S.trans=null; C.unlocked9=true; C.coreIN=false; S.mark|=2; light(S); S.flow=newFlow(S);
      log(S,`Infall complete at ${tr.st} s: the Great Attractor is lit. Attractor Seed Core consumed, T9 unlocked permanently.`,'ok');
      S.banner={kind:'good',text:'The Infall complete: the Great Attractor is lit and T9 is unlocked. Hold the flow with the pellet beam.',act:null}; }
    else if(tr.st>=1800+300) fail(S,'no 120 s Stable streak after the forced flow');
    return; }
  if(tr.kind==='inf'){
    tr.st++;
    if(tr.phase===1 && tr.st>=300){ tr.phase=2; S.tier=10; const keep=S.x; S.x={P:keep.P,Th:0,Om:0,Pi:0,W:0,L:0,V:0,H:C.infShock||85}; S.I=Math.max(0,S.I-20); S.lock=0; S.mark|=2;
      log(S,`Inflation: the reassembled regions unfold into a universe inside the crown. W, Λ and V retire; H appears at +${C.infShock||85}. Integrity −20. Dark Matter pulls it down onto the curve.`,'warn'); }
    if(tr.phase===2){ add(L.up,'Dark Matter',30.72); if(tr.st>=600){ tr.phase=3; S.mark|=2; log(S,'Recombination: the cosmic microwave background appears on the crown. Hold Stable or better for 120 s inside 600-900 s.','gold'); } }
    if(tr.phase===3){ tr.streak = S.zone>=0 && S.zone<=1 ? tr.streak+1 : 0;
      if(tr.streak>=120){ S.trans=null; C.unlocked10=true; C.coreINF=false; S.mark|=2; S.x.H=0; S.uni=newUniverse(S); light(S);
        log(S,`Inflation complete at ${tr.st} s: the Universe is lit. Universal Upgrade Core consumed, T10 unlocked permanently. The first universe is charged.`,'ok');
        S.banner={kind:'good',text:'Inflation complete: T10 is unlocked. Keep the universe flat to its last light.',act:null}; }
      else if(tr.st>=900) fail(S,'no 120 s Stable streak inside 600-900 s'); }
    return; }
  return _transTick(S); };
function beginInflation(S){ S.trans={kind:'inf', phase:1, st:0, streak:0};
  log(S,'Inflation begun: Universal Upgrade Core in escrow. 300 s of unfolding at T9 rules while demand ramps 24 → 96 Bu/t over 900 s.','gold'); }
/* ---- Deep Link (T8 → T9 milestone) ---- */
function newLink9(){ const o=newLink(); o.rr=new Float64Array(60); o.rsum=0; o.rho=0; return o; }
function deepLinkTick(W){
  const S=W.S, K=W.K, f=W.flow||{}, lk=W.link9d||(W.link9d=newLink9());
  if(!(S.tier===8 && !TIERS[8].slots && !S.trans && K.test9) || lk.done) return;
  const B=beamTick(S), i=lk.n%60; lk.rsum+=B.rho-lk.rr[i]; lk.rr[i]=B.rho; roll(lk,f.eye||0,f.collar||0); lk.rho=lk.rsum/60;
  const ok=W.linked && lk.n>=60 && lk.mean>=D9 && lk.frac>=0.5 && lk.rho>=1;
  lk.streak=ok?lk.streak+1:0; lk.best=Math.max(lk.best,lk.streak);
  if(lk.streak>=LINK_S){ lk.done=true; log(S,`Milestone: Deep Link held for 30 min (60 s mean ${lk.mean.toFixed(1)} Bu/t into the Eye, ${Math.round(lk.frac*100)} % test-fired into the collars, beam ρ ${lk.rho.toFixed(2)}). The Infall can begin.`,'ok'); }
}
const _conduitStep=conduitStep;
conduitStep=function(W){
  const S=W.S, K=W.K, t9=S.tier===8 && !TIERS[8].slots && !S.trans && K.test9;
  if(t9) W.force1=true;
  _conduitStep(W);
  const f=W.flow; f.collar=0;
  if(t9 && f.eye>0){ const d=K.c4t*f.eye, sc=(f.eye-d)/f.eye; f.collar=d; S.g.beta*=sc; S.g.gHi*=sc; S.g.gLo*=sc; S.g.gHalo*=sc; }
  if(S.tier===10){ S.g.gHi=0; S.g.gLo=0; S.g.gHalo=0; }
};
const _worldTick=worldTick;
worldTick=function(W){ _worldTick(W); deepLinkTick(W);
  const S=W.S, e=S.state==='LIT'?S.prevE:{}, F=S.flow;
  W.dir.flow = S.tier===9 && S.state==='LIT' ? ((e.V||0)>5?1:0)|((e.V||0)<-5?2:0)|(F&&F.warned&&F.accepted?4:0)|(!S.cfg.pelletsOK?8:0) : 0;
  W.dir.uni = S.tier===10 && S.state==='LIT' ? ((e.H||0)>5?1:0)|((e.H||0)<-5?2:0)|(S.warn?4:0) : 0; };
const _relight=relight; relight=function(S){ if(S.tier===9 && !TIERS[9].slots){ S.banner=null; S.x={P:0,Th:0,Om:0,Pi:0,W:0,L:0,V:0}; light(S); S.flow=newFlow(S); log(S,'Great Attractor relit at P 0, V 0 (in the mod you rerun the Infall from a lit Milky Way: charge again, no core).','gold'); return; } return _relight(S); };
const _light=light; light=function(S){ if(S.x.V==null) S.x.V=0; if(S.x.H==null) S.x.H=0; return _light(S); };
/*P5-END*/
