// star-lifecycle-engine.js — FIXTURE for tools/fieldsim-golden.js. Do not edit by hand:
// regenerate with `node tools/fieldsim-golden.js --refresh-fixture`, then regenerate the golden JSON.
// The ENGINE block of the D-49 play prototype, verbatim (the reference for the Java field maths).
// source: designs/star-lifecycle-prototype.html (design repo GTNH-Addons)
// source-sha1: 55ffde82b28086e25e4c728506cb23cb97209fca
// engine-sha1: bbf7e878a420735e230f80f2b01d823fc1d81ec2
/*ENGINE-START*/
const RECIPE_UMV = 125829120;
const HN = 7200;
const ZONES = [
  {name:'Resonant', Y:1.25, dI:1},
  {name:'Stable',   Y:1.0,  dI:0.2},
  {name:'Strained', Y:0.5,  dI:-0.5},
  {name:'Critical', Y:0,    dI:-2},
  {name:'Breach',   Y:0,    dI:0}];
function zoneOf(w){ return w<=10?0 : w<=40?1 : w<=70?2 : w<=100?3 : 4; }
const clamp=(v,a,b)=>v<a?a:v>b?b:v;
const TRIM = {
  He:{name:'Helium',unit:1000}, H:{name:'Hydrogen',unit:1000}, Cool:{name:'Super Coolant',unit:100},
  Space:{name:'Spatially Enlarged Fluid',unit:100}, ST:{name:'Molten SpaceTime',unit:100},
  Tg:{name:'Molten Purified Tengam',unit:36}, DSSF:{name:'Dimensionally Shifted Superfluid',unit:100},
  RSPM:{name:'Raw Star Matter',unit:1000}, Agent:{name:'Antipolarization Agent',unit:100}};
const GAUGE = {P:{sym:'P',name:'Pressure'}, Th:{sym:'Θ',name:'Core heat'}, Om:{sym:'Ω',name:'Spin'}, Pi:{sym:'Π',name:'Polarity'}};
const ctl4 = (E,w,f)=>{ const u={}, t=f?5:10, r=f?0.5:1;
  if(E.P>t) u.Space=10*r; if(E.P<-t) u.ST=10*r; if(E.Th>t) u.Cool=10*r; if(E.Om>t) u.Tg=5*r; return u; };
const TIERS = {
 1:{n:1, star:'Sol', stage:'Main-sequence star', gauges:['P'], sig:0.5, g:{P:-0.02}, d:()=>({P:0.7}),
    trims:{He:{cap:10,fx:{P:-2}}, H:{cap:10,fx:{P:2}}}, prod:{name:'Raw Star Matter',base:50000},
    up:[['Hydrogen',5e7],['Helium',5e7]], ignite:true,
    ctrl:(E,w,f)=>{const u={},t=f?5:10,r=f?2:10; if(E.P>t)u.He=r; if(E.P<-t)u.H=r; return u;},
    rig:f=>{const t=f?5:10,r=f?2:10; return [`P > +${t} → Helium ${r} kL/s`,`P < −${t} → Hydrogen ${r} kL/s`];}},
 2:{n:2, star:'Sirius A', stage:'Bigger, hotter main-sequence star', gauges:['P'], sig:2, g:{P:-0.01}, d:()=>({P:2}),
    trims:{He:{cap:20,fx:{P:-2},note:'ash: −0.5 % yield per unit'}, H:{cap:20,fx:{P:2}}}, ash:true, prod:{name:'Excited Stellar Plasma',base:2500},
    up:[['Hydrogen',1.25e7],['Helium',1.25e7],['Raw Star Matter',25000]], ignite:true,
    ctrl:(E,w,f)=>{const u={},t=f?5:10,r=f?4:20; if(E.P>t)u.He=r; if(E.P<-t)u.H=r; return u;},
    rig:f=>{const t=f?5:10,r=f?4:20; return [`P > +${t} → Helium ${r} kL/s`,`P < −${t} → Hydrogen ${r} kL/s`];}},
 3:{n:3, star:'Arcturus', stage:'Red giant: helium core, hydrogen shell', gauges:['P','Th'], sig:1.5, g:{P:-0.01,Th:0.01}, d:()=>({P:1.5,Th:1}),
    cpl:(x,k)=>k==='P'?0.02*x.Th:0,
    trims:{He:{cap:20,fx:{P:-1,Th:3}}, H:{cap:20,fx:{P:3,Th:0.5}}, Cool:{cap:20,fx:{P:-1,Th:-3}}},
    prod:{name:'Raw Hyperdense Stellar Plasma',base:625}, up:[['Hydrogen',1.25e7],['Helium',1.25e7],['Excited Stellar Plasma',1250]], ignite:true,
    ev:{name:'Helium flash', every:[1200,2400], warn:15, dur:3, fx:{Th:30}},
    ctrl:(E,w,f)=>{const t=f?5:10,r=f?4:10,u={};
      if(E.P>t){u.He=(u.He||0)+r; u.Cool=(u.Cool||0)+r;}
      if(E.P<-t) u.H=r;
      if(E.Th>t || (w && E.Th>-30)) u.Cool=(u.Cool||0)+(w?20:r);
      if(E.Th<-t) u.He=(u.He||0)+r;
      return u;},
    rig:f=>{const t=f?5:10,r=f?4:10; return [`P > +${t} → Helium ${r} kL/s + Super Coolant ${r*100} L/s`,`P < −${t} → Hydrogen ${r} kL/s`,
      `Θ > +${t} → Super Coolant ${r*100} L/s`,`Θ < −${t} → Helium ${r} kL/s`,`Flash warning and Θ > −30 → Super Coolant 2,000 L/s (big valve)`];}},
 4:{n:4, star:'Neutron Star', stage:'Arcturus Remnant, collapsed core', gauges:['P','Th','Om'], sig:2, g:{P:-0.01,Th:0,Om:0.02},
    d:S=>{const f=S.cfg.feed; return {P:-3+5*f, Th:-0.5+3*f, Om:1.5*f};}, feed:true,
    trims:{Space:{cap:10,fx:{P:-2}}, ST:{cap:10,fx:{P:2}}, Cool:{cap:10,fx:{Th:-3}}, Tg:{cap:5,fx:{Om:-4,Th:1}}},
    prod:{name:'White Dwarf Matter',base:0}, up:[['Molten SpaceTime',144]],
    ev:{name:'Glitch', every:[900,1800], warn:0, dur:1, fx:{Om:20}},
    ctrl:ctl4,
    rig:f=>{const t=f?5:10,r=f?0.5:1; return [`P > +${t} → Spatially Enlarged Fluid ${1000*r} L/s`,`P < −${t} → Molten SpaceTime ${1000*r} L/s`,
      `Θ > +${t} → Super Coolant ${1000*r} L/s`,`Ω > +${t} → Molten Purified Tengam ${180*r} L/s`];}},
 5:{n:5, star:'Sirius B', stage:'White dwarf, cooled to a black dwarf', gauges:['P','Th'], sig:1.5, g:{P:-0.01,Th:0.01}, d:()=>({P:-1,Th:0.02}),
    batch:true, trims:{DSSF:{cap:10,fx:{Th:-1,P:-0.5}}, RSPM:{cap:5,fx:{Th:3}}, He:{cap:10,fx:{P:2}}},
    prod:{name:'Black Dwarf Matter',base:4608}, up:[], batchUp:[['White Dwarf Matter',18432]],
    ctrl:(E,w,f)=>{const u={},t=f?5:10,r=f?0.5:1;
      if(E.Th>t) u.DSSF=10*r; if(E.Th<-t) u.RSPM=5*r; if(E.P<-t) u.He=10*r; if(E.P>t) u.DSSF=(u.DSSF||0)+5*r; return u;},
    rig:f=>{const t=f?5:10,r=f?0.5:1; return [`Θ error > +${t} → DSSF ${1000*r} L/s`,`Θ error < −${t} → Raw Star Matter ${5000*r} L/s`,
      `P < −${t} → Helium ${10*r} kL/s`,`P > +${t} → DSSF +${500*r} L/s`];}},
 6:{n:6, star:'SGR 1806-20', stage:'Magnetar, a merged remnant', gauges:['P','Th','Om','Pi'], sig:2, g:{P:-0.01,Th:0,Om:0.02,Pi:0.03},
    d:S=>{const r=S.cfg.r; return {P:-4+5*r, Th:-0.5+3*r, Om:r, Pi:0};}, shield:true,
    trims:{Space:{cap:10,fx:{P:-2}}, ST:{cap:10,fx:{P:2}}, Cool:{cap:10,fx:{Th:-3}}, Tg:{cap:5,fx:{Om:-4,Th:1}}, Agent:{cap:5,fx:{Th:0.5},note:'A +4 per unit; the upkeep adds 2r/s'}},
    prod:{name:'Magnetar Matter',base:15}, up:[['White Dwarf Matter',60],['Black Dwarf Matter',15]],
    ev:{name:'Giant flare', every:[60,120], warn:15, dur:4, fx:{Pi:25}, signed:true},
    ctrl:(E,w,f)=>{const u=ctl4(E,w,f), t=f?5:10; if(w || Math.abs(E.Pi)>t) u.Agent=5; return u;},
    rig:f=>{const t=f?5:10,r=f?0.5:1; return [`P > +${t} → Spatially Enlarged Fluid ${1000*r} L/s`,`P < −${t} → Molten SpaceTime ${1000*r} L/s`,
      `Θ > +${t} → Super Coolant ${1000*r} L/s`,`Ω > +${t} → Molten Purified Tengam ${180*r} L/s`,`Flare warning or |Π| > ${t} → Antipolarization Agent 500 L/s`];}}
};
function mulberry(a){ return function(){ a|=0; a=a+0x6D2B79F5|0; let t=Math.imul(a^a>>>15,1|a); t=t+Math.imul(t^t>>>7,61|t)^t; return ((t^t>>>14)>>>0)/4294967296; }; }
function uni(S,[a,b]){ return a+(b-a)*S.rng(); }
function spAt(eyeT){ return 90-180*Math.min(eyeT,7200)/7200; }
function mMax(S){ return 1+0.25*Math.max(0,S.cfg.tt-S.tier); }
function newLedger(){ return {up:{}, eu:0, prod:{}, trims:{}, tach:0, charges:0, fails:0, zoneT:[0,0,0,0,0], ySum:0, yN:0, cycles:0, batches:0}; }
function newHist(){ return {tier:new Uint8Array(HN), P:new Float32Array(HN).fill(NaN), Th:new Float32Array(HN).fill(NaN), Om:new Float32Array(HN).fill(NaN), Pi:new Float32Array(HN).fill(NaN), sp:new Float32Array(HN).fill(NaN), mk:new Uint8Array(HN)}; }
function add(o,k,v){ o[k]=(o[k]||0)+v; }
function log(S,msg,kind){ S.log.unshift({t:S.t,msg,kind:kind||''}); if(S.log.length>120) S.log.length=120; S.logRev++; }
function newState(tier,cfg){
  const S={tier, cfg, rng:mulberry(cfg.seed*2654435761>>>0), t:0, eyeT:0, state:'LIT', x:{P:0,Th:0,Om:0,Pi:0}, A:0, I:100, lock:0,
    prevE:{P:0,Th:0,Om:0,Pi:0}, prevWarn:0, warn:0, zone:0, worst:0, stab:100, Y:1.25, sp:NaN, u:{}, m:1,
    ev:null, cyc:null, batch:null, L:newLedger(), log:[], logRev:0, H:newHist(), trans:null, pulse:{}, banner:null, pauseReq:false, mark:0};
  log(S, `${TIERS[tier].star} selected, seed ${cfg.seed}. ${tier<=3?'Star lit.':tier===5?'White dwarf assembled at Θ +90.':'Remnant lit.'}`,'gold');
  light(S);
  return S;
}
function light(S){
  const T=TIERS[S.tier];
  S.state='LIT'; S.I=100; S.lock=0; S.A=0;
  S.cyc={eyeT:0, ySum:0, n:0, rMin:1, hsm:0};
  S.ev = T.ev ? {next:S.t+uni(S,T.ev.every)/Math.min(S.cfg.m,mMax(S)), left:0, warned:false, sign:1} : null;
  if(T.batch) startBatch(S);
  chargeCycle(S);
}
function startBatch(S){
  S.x.Th=90; S.x.P=0; S.batch={eyeT:0, ySum:0, n:0, rMin:1, warned:false};
  for(const [k,v] of TIERS[S.tier].batchUp) add(S.L.up,k,v);
}
function chargeCycle(S){
  const T=TIERS[S.tier], r=S.cfg.r;
  for(const [k,v] of T.up) add(S.L.up,k,v*r);
  S.L.eu += RECIPE_UMV*Math.pow(4,T.n-1)*1200;
}
function fail(S,reason){
  const T=TIERS[S.tier], tr=S.trans;
  S.L.fails++; S.mark|=4; S.pauseReq=true;
  if(T.batch && !tr){
    const frac=S.batch.eyeT/7200, y=4608*0.25*frac;
    add(S.L.prod,T.prod.name,y);
    log(S,`Fracture (${reason}). Batch ends at ${(frac*100).toFixed(0)} %: ${fmtL(y)} BDM salvaged, the rest of the charge is lost. Next batch charged.`,'bad');
    S.banner={kind:'bad', text:`Fracture: ${reason}. ${fmtL(y)} BDM salvaged (25 % × ${(frac*100).toFixed(0)} % done). A new white dwarf is assembled.`, act:null};
    S.I=100; S.lock=0; startBatch(S); return;
  }
  let text, act;
  if(tr && tr.phase===3){ text=`Collapse: the proto-neutron star was lost (${reason}). The Galactic Upgrade Core stays in escrow; relight Arcturus and run the Core Collapse again.`; act='T3'; }
  else if(tr){ text=`Arcturus lost during the fast-forward (${reason}). The Time fluid is spent; the core stays in escrow.`; act='relight'; }
  else if(S.tier<=3){ text=`${T.star} went out: ${reason}. In-flight output is lost. Relighting takes a new Stellar Ignition Charge and a seed cloud.`; act='relight'; }
  else if(S.tier===4){ text=`The neutron star collapsed to a black hole: ${reason}. In the mod you rerun the Core Collapse (no core needed); here Relight skips to its settle phase.`; act='relight'; }
  else { text=`Field decoherence: ${reason}. The magnetar is lost. Respawning needs the merger charge (WDM 18,432 L + BDM 4,608 L + Agent 100,000 L), no core.`; act='relight'; }
  log(S,text,'bad');
  S.banner={kind:'bad', text, act};
  S.trans=null; S.state='DARK'; S.x={P:0,Th:0,Om:0,Pi:0}; S.A=0; S.lock=0;
}
function relight(S){
  const T=TIERS[S.tier]; S.banner=null;
  if(T.ignite){
    S.state='IGNITING'; S.x={P:-80,Th:0,Om:0,Pi:0}; S.I=100; S.lock=0; S.L.charges++;
    const seed=1e8*Math.pow(2,T.n-1); add(S.L.up,'Hydrogen',seed); add(S.L.up,'Helium',seed);
    S.L.eu += 10*RECIPE_UMV*Math.pow(4,T.n-1)*1200;
    log(S,`Ignition: Stellar Ignition Charge used, seed cloud H + He ${fmtL(seed)} each. Protostar at P −80, contracting +1.5/s.`,'gold');
  } else if(S.tier===4){
    S.x={P:0,Th:60,Om:30,Pi:0}; light(S); log(S,'Proto-neutron star at P 0, Θ +60, Ω +30 (Core Collapse rerun skipped in the prototype).','gold');
  } else if(S.tier===6){
    S.x={P:0,Th:0,Om:0,Pi:0}; light(S); add(S.L.up,'White Dwarf Matter',18432); add(S.L.up,'Black Dwarf Matter',4608); add(S.L.up,'Antipolarization Agent',100000);
    log(S,'Magnetar re-merged (merger imprint skipped in the prototype). Merger charge spent.','gold');
  }
}
function manualU(S,T){
  const u={}, man=S.cfg.man[S.tier]||{};
  for(const k in T.trims){ let v=man[k]||0; if(S.pulse[k]>0){ v=T.trims[k].cap; S.pulse[k]--; } if(v>0) u[k]=v; }
  return u;
}
function rec(S,dark){
  const i=S.t%HN, H=S.H;
  H.P[i]=dark?NaN:S.x.P; H.Th[i]=dark?NaN:S.x.Th; H.Om[i]=dark?NaN:S.x.Om; H.Pi[i]=dark?NaN:S.x.Pi;
  H.sp[i]=(!dark && TIERS[S.tier].batch)?S.sp:NaN; H.mk[i]=S.mark;
  H.tier[i]=S.tier;
}
function step(S){
  const C=S.cfg; S.mark=0;
  if(S.state==='DARK'){ S.zone=-1; rec(S,true); S.t++; return; }
  if(S.state==='IGNITING'){
    S.x.P=Math.min(S.x.P+1.5,0); S.zone=-1; S.u={};
    if(S.x.P>=-20){ light(S); log(S,`${TIERS[S.tier].star} lit at P −20. Field rules take over.`,'ok'); S.mark|=2; }
    rec(S,false); S.t++; return;
  }
  const T=TIERS[S.tier], tr=S.trans;
  const m = tr?1:Math.min(C.m,mMax(S)), q=1-0.05*(C.ts-1), r=C.r;
  let u = C.auto ? T.ctrl(S.prevE,S.prevWarn,C.fine) : manualU(S,T);
  for(const k in u){ if(!T.trims[k]){ delete u[k]; continue; } u[k]=clamp(u[k],0,T.trims[k].cap); if(!u[k]) delete u[k]; }
  if(C.auto) for(const k in S.pulse) S.pulse[k]=0;
  // events
  const E={}; let warn=0;
  if(T.ev && S.ev){
    const ev=T.ev, st=S.ev;
    if(!st.warned && S.t>=st.next-ev.warn){
      st.warned=true; st.sign=ev.signed?(S.rng()<0.5?-1:1):1;
      if(ev.warn){ S.mark|=1; log(S,`Event channel: ${ev.name} in ${ev.warn} s${ev.signed?(st.sign>0?', sign +':', sign −'):''}.`,'warn'); }
    }
    if(st.warned && st.left===0 && S.t>=st.next){
      st.left=ev.dur; S.mark|=2;
      const fxs=Object.entries(ev.fx).map(([k,v])=>`${GAUGE[k].sym} ${st.sign*v>0?'+':'−'}${(Math.abs(v)*q).toFixed(1)}${ev.dur>1?'/s for '+ev.dur+' s':''}`).join(', ');
      log(S,`${ev.name}: ${fxs}.`,'warn');
    }
    if(st.left>0){
      for(const k in ev.fx) E[k]=(E[k]||0)+ev.fx[k]*st.sign*q;
      st.left--;
      if(st.left===0){ st.warned=false; st.next=S.t+1+uni(S,ev.every)/m; }
    }
    if(st.warned && ev.warn) warn=st.sign;
  }
  const dp={P:-5*(1-r), Th:0, Om:0, Pi:0};
  let sp=0;
  if(T.batch){
    sp=spAt(S.batch.eyeT);
    if(sp>=-10 && sp<=10) dp.Th+=0.15;
    if(sp>10 && spAt(S.batch.eyeT+15*m)<=10){ warn=1; if(!S.batch.warned){ S.batch.warned=true; S.mark|=1; log(S,'Event channel: crystallisation plateau in 15 s (latent heat Θ +0.15/s while the setpoint is within ±10).','warn'); } }
  }
  if(tr && tr.phase===1){ const sw=2*(tr.Mc-0.5)/0.94; dp.P+=sw; dp.Th+=sw; }
  const d=T.d(S);
  for(const k of T.gauges){
    const e=S.x[k]-(k==='Th'&&T.batch?sp:0);
    const c=T.cpl?T.cpl(S.x,k):0;
    const xi=(S.rng()*2-1)*T.sig;
    let dv=m*((d[k]||0)+dp[k]+T.g[k]*e+c+q*xi);
    for(const i in u){ const f=T.trims[i].fx[k]; if(f) dv+=f*u[i]; }
    dv+=E[k]||0;
    S.x[k]=clamp(S.x[k]+dv,-120,120);
  }
  if(T.shield){
    S.A=Math.min(100,0.95*S.A+4*(u.Agent||0)+2*r); // +2r: the Agent upkeep charges the shield (ruling 2026-10-13)
    const c=Math.min(S.A,Math.abs(S.x.Pi),25); S.x.Pi-=Math.sign(S.x.Pi)*c; S.A-=c;
  }
  const err={}; let worst=0, wk=T.gauges[0];
  for(const k of T.gauges){ err[k]=S.x[k]-(k==='Th'&&T.batch?sp:0); if(Math.abs(err[k])>worst){ worst=Math.abs(err[k]); wk=k; } }
  const z=zoneOf(worst);
  S.zone=z; S.worst=worst; S.stab=clamp(100-worst,0,100); S.sp=T.batch?sp:NaN; S.u=u; S.m=m; S.warn=warn;
  S.lock = z===0 ? S.lock+1 : 0;
  let Y=ZONES[z].Y; if(S.lock>=600) Y=1.5; if(T.ash && u.He) Y*=Math.max(0,1-0.005*u.He); S.Y=Y;
  const L=S.L;
  for(const k in u) add(L.trims,k,u[k]*TRIM[k].unit);
  if(m>1) L.tach += ((m-1)/0.25)*10*Math.pow(2,T.n-1);
  L.zoneT[z]++; L.ySum+=Y; L.yN++;
  S.prevE=err; S.prevWarn=warn;
  if(z===4){ fail(S,`Breach, ${GAUGE[wk].sym} ${fmtS(err[wk])}`); rec(S,false); S.t++; return; }
  S.I=clamp(S.I+ZONES[z].dI,0,100);
  if(S.I<=0){ fail(S,'Integrity reached 0'); rec(S,false); S.t++; return; }
  // transition
  if(tr){
    if(tr.phase===1){
      const dose=C.tachRate; tr.tach+=dose; L.tach+=dose; tr.Mc=0.5+tr.tach/100000;
      if(tr.tach>=94000){ tr.Mc=1.44; bounce(S); }
    } else if(tr.phase===3){
      tr.st++; tr.streak = S.zone<=1 ? tr.streak+1 : 0;
      if(tr.streak>=60){
        S.trans=null; C.unlocked4=true; C.core=false; S.mark|=2;
        log(S,`Core Collapse complete in ${tr.st} s of settling. Galactic Upgrade Core consumed, T4 unlocked permanently, the neutron star is lit.`,'ok');
        S.banner={kind:'good', text:'Core Collapse complete: the neutron star is lit and T4 is unlocked. Keep it Stable; its spin runs away and glitches come without warning.', act:null};
      } else if(tr.st>=180){
        fail(S,'not Stable for 60 s within 180 s');
      }
    }
  }
  if(S.state!=='LIT'){ rec(S,true); S.t++; return; }
  // cycles
  const Tn=TIERS[S.tier];
  if(Tn.feed){ const h=C.feed*m; add(L.up,'Hyperdense Stellar Mixture',h); S.cyc.hsm+=h; }
  const cy=S.cyc; cy.ySum+=Y; cy.n++; cy.rMin=Math.min(cy.rMin,r); cy.eyeT+=m; S.eyeT+=m;
  if(cy.eyeT>=60-1e-9){
    const mY=cy.ySum/cy.n;
    if(!Tn.batch){
      const base=Tn.feed?3.6864*cy.hsm:Tn.prod.base, out=base*mY*cy.rMin;
      add(L.prod,Tn.prod.name,out); S.mark|=8;
      log(S,`Cycle ${L.cycles+1}: +${fmtL(out)} ${Tn.prod.name} (mean Y ${mY.toFixed(2)}, r ${cy.rMin.toFixed(2)}).`,'ok');
    }
    L.cycles++;
    S.cyc={eyeT:cy.eyeT-60, ySum:0, n:0, rMin:1, hsm:0};
    chargeCycle(S);
  }
  if(Tn.batch){
    const b=S.batch; b.ySum+=Y; b.n++; b.rMin=Math.min(b.rMin,r); b.eyeT+=m;
    if(b.eyeT>=7200-1e-9){
      const mY=b.ySum/b.n, out=4608*mY*b.rMin; add(L.prod,Tn.prod.name,out); L.batches++; S.mark|=8;
      log(S,`Batch ${L.batches} cooled to a black dwarf: +${fmtL(out)} Black Dwarf Matter (mean Y ${mY.toFixed(2)}). Next white dwarf assembled.`,'ok');
      startBatch(S);
    }
  }
  rec(S,false); S.t++;
}
function bounce(S){
  const C=S.cfg, tr=S.trans, s=Math.min(90,Math.floor(C.stBuf/1000)), shock=90-s;
  C.stBuf-=s*1000; add(S.L.up,'Molten SpaceTime',s*1000); add(S.L.up,'Hyperdense Stellar Mixture',5000); C.hsm=false;
  const P0=S.x.P, T0=S.x.Th;
  S.x.P=clamp(S.x.P+shock,-120,120); S.x.Th=clamp(S.x.Th+shock,-120,120); S.I=Math.max(0,S.I-20); S.mark|=2;
  tr.shock={P0,T0,P:S.x.P,Th:S.x.Th,s};
  log(S,`Core bounce at M_c 1.44 M☉: HSM 5,000 L seeded, ${fmtL(s*1000)} SpaceTime absorbed ${s} points. Shock +${shock}: P ${fmtS(P0)} → ${fmtS(S.x.P)}, Θ ${fmtS(T0)} → ${fmtS(S.x.Th)}. Integrity −20.`,'warn');
  if(S.x.P>100 || S.x.Th>100){
    S.L.fails++; S.mark|=4; S.pauseReq=true;
    const text=`Supernova: ${S.x.P>100?'P':'Θ'} ended above +100 after the bounce. Arcturus is lost; the Time fluid, HSM and SpaceTime are spent. The Galactic Upgrade Core stays in escrow. No blocks are damaged.`;
    log(S,text,'bad'); S.banner={kind:'bad',text,act:'relight'};
    S.trans=null; S.state='DARK'; S.x={P:0,Th:0,Om:0,Pi:0};
    return;
  }
  S.tier=4; S.x={P:0,Th:60,Om:30,Pi:0}; S.lock=0; tr.phase=3; tr.st=0; tr.streak=0;
  light(S);
  log(S,'Proto-neutron star under T4 rules at P 0, Θ +60, Ω +30. Hold Stable or better for 60 s within 180 s.','gold');
}
function beginCollapse(S){
  const C=S.cfg;
  S.trans={phase:1, tach:0, Mc:0.5, st:0, streak:0, shock:null};
  log(S,'Core Collapse begun: Galactic Upgrade Core held in escrow. Fast-forwarding the core with Tachyon Rich Temporal Fluid.','gold');
}
function fmtL(v){ const a=Math.abs(v); return (a>=100?Math.round(v).toLocaleString('en-US'):a>=10?v.toFixed(1):v.toFixed(2))+' L'; }
function fmtS(v){ return (v>0?'+':v<0?'−':'')+Math.abs(v).toFixed(1); }
/*ENGINE-END*/
