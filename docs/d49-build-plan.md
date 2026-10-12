# D-49 build plan: the Eye's star lifecycle, T1-T6, and the stability field

Status: **build plan**, 2026-10-12. Written read-only against the addon working tree of 2026-10-12 (HEAD dbd09cc plus
uncommitted edits from the running build) and the GT 5.09.52.594 / 5.09.54.133 jars. Nothing here is built yet.

**What was approved.** I approved the D-49 proposal "as proposed (numbers stay tunable)" (`yours.md` §8d). So
every recommendation in `designs/docs/star-lifecycle.md` §7 is a decision, unless it contradicts my own words.
Section 1 lists each one. Where the proposal is silent or ambiguous, this plan makes a **plan reading**. Each reading is
marked, and each becomes a questions-file entry when it is built.

**Sources, in order of authority:**

1. `PROPOSAL.md` D-49 and `designs/docs/yours.md` §8d. These are my words and the truth.
2. The build brief's rulings (`sl-build-brief.md`, top section).
3. `designs/docs/star-lifecycle.md` (the proposal).
4. The playable prototype `lifecycle/star-lifecycle.html`, the block between `/*ENGINE-START*/` and `/*ENGINE-END*/`.
   It is the reference for the per-second maths. The Java must reproduce it step for step, RNG draws included.
5. The as-built addon: `docs/singularity-ladder-build.md`, `docs/singularity-ladder-questions.md` and the code.

**Port.** The codebase moves from GT 5.09.52.594 to 5.09.54.133 **before** this build starts. Section 14 lists
everything here that depends on a version-specific API, with what javap showed in both jars.

**Prototype location.** The prototype lives only in my local work folder
(`work/lifecycle/star-lifecycle.html`). Copy it into the design repo
(for example `designs/lifecycle/star-lifecycle.html`) before the local work folder is cleared. Unit 1 also copies its
engine block into `addon/tools/fixtures/`.

Code paths use `<pkg>` = `src/main/java/com/nateyburr/beyondthestars`.

---

## 1. Decisions

### 1.1 The proposal's open questions (§7)

| # | Question | Decision |
|---|---|---|
| Q-1 | Is T2 still optional? | **Required: recommended → adopted.** Shell Ignition starts from a lit Sirius A. Structurally, `eye_jet` requires `eye_beam` (bands are cumulative). The Homogeny stays a bulk ESPM route when it is built. This revises D-42 and the earlier Q-2 ruling; D-49 says "change the others". |
| Q-2 | How does T5 cool? | **Subspace Cooling: recommended → adopted.** A DSSF heat sink, with the general time dial for speed. This is my own D-43 phrase, so there is no conflict with "I haven't decided". |
| Q-3 | How harsh is failure? | **Recommended → adopted.** You lose the star instance, the in-flight output and the event materials. Cores are consumed only on success. There is no block damage and no tier loss. No consolation drops. |
| Q-4 | Which red giant at T3? | **Arcturus: recommended → adopted.** The existing chart is kept. |
| Q-5 | The time dial | **Recommended → adopted.** It works on every tier, capped at `m_max = 1 + 0.25·max(0, tt − N)`, and costs Tachyon Rich Temporal Fluid. |
| Q-6 | Trim fluid budgets | **Accept them; tune after a play test: recommended → adopted.** All numbers are constants in one table (§5). |
| Q-7 | Antimatter rate | **"Check the forge, then retune": recommended → adopted, and checked.** The 54.133 Antimatter Forge tooltip gives production of about `Antimatter^(0.5+C) × N(0.2+A, 1)` per second across 16 hatches. Its passive cost is `10⁷ + (Antimatter·1000)^(1.5−M)` EU/t. A forge holding about 10⁴ L makes about 20 L/s at about 3·10¹⁰ EU/t. The proposal's 10 L/s at T6 (100 L of Antimatter per 1,000 L of Agent) is a real but fair UXV running cost. **Keep 100 L.** |
| Q-8 | One live star per Eye | **Recommended → adopted.** |
| Q-9 | T4 throughput | **HSM feed up to 1.5 L/s: recommended → adopted.** |
| Q-10 | Transitions and wrenches | **Recommended → adopted.** A wrench aborts a transition. The event materials are lost and the core stays in escrow. |

### 1.2 The prototype findings (questions file, the first B-25)

| Item | Decision |
|---|---|
| B-25 call 1: the T4 HSM feed push and the T6 WDM + BDM feed push scale with the dial m | **Recommended → adopted.** The prototype engine does this; the feed is part of drift `d`. |
| B-25 call 2: the T5 pressure trim is Helium | **As proposed → adopted.** |
| B-25 finding: the bounce rarely threatens a supernova | **Raise the bounce to +150 and keep "1,000 L of SpaceTime absorbs 1 point, up to 90,000 L": recommended → adopted.** This differs from the prototype engine, which uses +90, so it is the constant `BOUNCE_SHOCK = 150`. The golden test (§15) runs the collapse at both 90 and 150. |

### 1.3 Plan readings (each becomes a question when built)

| # | Gap in the proposal | Reading taken |
|---|---|---|
| R-1 | The Stellar Ignition Charge goes "in any Eye input socket", but every Eye socket is a fluid hatch. | The plinth `h` sockets also accept item input buses (`MTEHatchInputBus`, ME input bus included) into a private list. Charges are taken from there. Purified Tengam returns go to the existing ME output bus. |
| R-2 | How a transition is started. | **Swap the controller-slot chart to the next tier's chart while the current star is lit.** The Eye then holds the current star in a PENDING state. The transition starts the moment every precondition holds: milestones met, core in escrow (or tier already unlocked, for re-runs), band built, compression high enough, event materials buffered. Any other target change puts the star out. This is automatable and needs no new button. |
| R-3 | Preconditions for Mass Infusion and Shell Ignition. | Same shape as the Core Collapse: milestones, core in escrow, next band built, compression at N+1 everywhere, star lit and Stable or better at the start, and the event fluid fully buffered. The event fluid is taken in full when the event starts. |
| R-4 | Shell Ignition dynamics. | Sirius A's P parameters plus a Θ gauge that starts at 0. Over the 600 s, with `s = t/600`: `d_Θ = 1.0·s`, `g_Θ = +0.01·s`, P coupling `0.02·Θ·s`. The T3 trim table applies from the start (Θ needs Super Coolant). Ash is off. The forced flash warns at t = 585 and hits for t = 600-602; success comes at t = 603 with no failure. |
| R-5 | What a transition does to the cycle in progress. | Following prototype `light()`, the partial cycle is discarded (no output) and the new tier's cycle is charged at once. |
| R-6 | Magnetar Merger dynamics. | T6 rules (P, Θ, Ω, Π, shield, the WDM + BDM feed) except that Π drifts +0.5/s and there are no flares. Phase 1: hold Π in [+60, +90] for 120 consecutive seconds, **timing out at 600 s**. Phase 2: bring Π within ±40 within 60 s. Breach, Integrity 0 or a timeout loses the charge and keeps the core. |
| R-7 | A soft-mallet (machine disabled) while a star is lit. | Same as a wrench: the star goes out, any transition aborts, and the core stays in escrow. Without this, disabling would freeze the field, which is an exploit. Chunk unloading still freezes (§3.3 of the proposal). |
| R-8 | "Structure incomplete: the field freezes" against "a star that loses its structure goes out" (§3.3). | `mMachine == false` puts the star out; an unloaded chunk freezes it. |
| R-9 | The bounce's Integrity −20 is moot, because prototype `light()` resets I to 100 straight after. | Keep the prototype's behaviour (Integrity is 100 when the settle phase starts). If the −20 should bite, `light()` must skip the reset at the bounce; that is a question. |
| R-10 | Proposal §6 item 1 says output is "× mean Y × r × p × m". | Per **cycle**, output is `base × p × meanY × r`. The dial makes cycles come m times faster, so ×m holds per hour, not per cycle. This matches the prototype; multiplying per cycle again would double-count m. |
| R-11 | T6's four `grb` cells in the mockup (GRB receptors) have no role (GRB is T7+). | They become stellar input sockets `g` on the rim (fallback `T`), so the T6 merger and upkeep fluids can be fed at the rim. |
| R-12 | T5/T6 band casings. | The mockup draws both in `galactic`, so they reuse **Galactic Shell Plating (slCasing:12)**. No new block metas. Fairness comes from the compression gate (tiers 5/6), the UXV cores and the running costs. The alternative is two new slCasing2 metas (3 and 4) for band identity. |
| R-13 | Ignition seed, upkeep and EU with parallel p > 1. | All of them scale × p, the same as upkeep and product. The time-dial Time-fluid cost does **not** scale with p (proposal formula). |
| R-14 | Escrow retrieval. | Sneak-right-click with an empty hand returns the escrowed core when no transition is running. |
| R-15 | ME stocking hatch in a Field socket. | Accepted by the walk, so the Eye still forms, but kept on a rejected list: never drained, flagged on the scanner and in the GUI. A structure error would un-form the Eye and kill the star. |
| R-16 | The time dial and q need Time Acceleration and Stabilisation generators above tier 1. Their recipes change in the port (§14). | No new generator recipes in this build. Re-audit after the port; see the open items. |

### 1.4 Already settled, carried forward

- The 1:1 compression gate (Q-20) extends to T6: `COMPRESSION_META_FOR_TIER = { -1, 0, 1, 2, 3, 4, 5 }`.
- The Eye is a pure EU sink (Q-4). Its name is "Eye of Harmony" at every tier (Q-13).
- The field generators now have effects (proposal §3.6), which supersedes the Q-20 "placeholders" ruling for Time
  Dilation and Stabilisation:
  - Compression remains the hard gate.
  - Time Acceleration sets `m_max`.
  - Stabilisation sets `q`.
  - Planets keep only the compression time reward, as built.
- A-2/B-1 (milestones + install run for the Eye) are superseded by §8 here. The Kerr's Density Upgrade install is
  **not** in D-49 and stays as built (A-2 remains open for the Kerr).

---

## 2. What changes in the as-built Eye (summary)

| Area | As built | After D-49 |
|---|---|---|
| Star targets | Batch runs of 20-120 min; success always 1.0; EU in one lump | Live stars: `DARK → IGNITING → LIT`, 60 s cycles, EU and upkeep per cycle, output × mean Y × r |
| T4 | Sirius B white dwarf (HSM 5,000 + He 4·10⁹ → WDM 18,432 per 2 h) | Neutron Star (Arcturus Remnant): SpaceTime 144 L/cycle + HSM feed f → WDM 3.6864 L per L of HSM |
| T5, T6 | Not built; interim Sirius B Cooled at T4 makes BDM | T5 Sirius B cooling batches (WDM → BDM); T6 SGR 1806-20 magnetar (WDM + BDM feed → Magnetar Matter); the interim recipe and the SL-P26 chart recipe are deleted |
| Tier unlock | Band + compression; Galactic core by right-click at T4 | `installedTier = min(unlockedTier, structureTier, compressionTier)`; milestone → core (escrow) → transition event |
| Field generators | Dilation and Stabilisation are placeholders | Dilation sets the dial cap; Stabilisation sets q |
| Sockets | `h` gas, `o` ME out, `m` maintenance, `s`/`t` stellar | Plus `f` Field (×8, unbuffered trims), `n` Sensor (×8), `g` rim stellar (×4, T6), and input buses on `h` |
| New MTE | none | 28116 Stellar Field Sensor Hatch |
| Pieces | `eye_core`, `eye_beam`, `eye_jet`, `eye_arms` | Plus `eye_arms2` (T5) and `eye_rim` (T6); `eye_core` re-exported with `f`/`n` |

Planets are untouched (batch runs, as built). Selecting a planet puts the star out.

---

## 3. Data model and NBT

### 3.1 Classes

| Class | Where | What it holds |
|---|---|---|
| `FieldRng` | `<pkg>/machines/eye/field/` | A mulberry32 port: one `int` of state, `next()` in [0, 1). |
| `TierParams` (enum `T1..T6`) | `field/` | Gauges, d, g, σ, coupling, events, trim table, product, batch/feed/shield/ash flags. One table, §5. |
| `TrimDef` | `field/` | Per tier: fluid key, unit (L), cap (units/s per parallel), effect vector, side notes. |
| `EventDef` | `field/` | Interval [a, b] in Eye-seconds, warning lead, duration, effect, signed. |
| `FieldState` | `field/` | `x[4]` (P, Θ, Ω, Π), A, I, lock, `prevE[4]`, prevWarn, event state, the cycle and batch accumulators, the field clock `t`, `eyeT`, the RNG. |
| `StellarField` | `field/` | `step(FieldState, TierParams, Overlay, Inputs) → StepResult`: the pure per-second maths (§4). No Minecraft types. |
| `Overlay` (interface) | `field/` | The transition overlays: extra drift `d'`, parameter ramps, forced events. |
| `FieldReadout` | `field/` | An immutable per-second snapshot for the GUI, scanner and sensors. |
| `FieldRig` | `field/` | The prototype's `ctrl()` rigs (off, bang-bang, two-stage) for tests and the debug `rig` command. Never used in play. |
| `FieldSim` | `field/` | The offline runner with a `main()`: golden cases, failure plus auto-relight harness, ledger. |
| `StarDef` | `<pkg>/machines/eye/star/` | key, name, tier, chart item, upkeep per cycle (fluid, L), product (fluid, base L), `TierParams` ref. |
| `EyeStarRuntime` | `star/` | Per controller: state machine, cycle charging, ignition, dial payment, trim draining, feed draw, output, publishing. Owns a `FieldState`. |
| `EyeTransition` (+ subclasses `MassInfusion`, `ShellIgnition`, `CoreCollapse`, `FirstCooling`, `MagnetarMerger`) | `star/` | The §7 state machines; each implements `Overlay`. |
| `EyeMilestones` | `star/` | Per-tier counters and the predicates of §8. |
| `EyeNbt` | `star/` | Schema version, the one writer and reader, legacy migration (§13). |

### 3.2 State enums

- `StarState`:
  - `DARK`
  - `IGNITING`
  - `LIT`
  - `PENDING` (LIT, with the next tier's chart waiting on preconditions; §7.1)
  - `IDLE` (T5 with no batch, or T6 waiting for a merger charge)
  - `BATCH` (T5)
- `TransitionType`:
  - `MASS_INFUSION`
  - `SHELL_IGNITION`
  - `CORE_COLLAPSE`
  - `FIRST_COOLING`
  - `MAGNETAR_MERGER`

### 3.3 NBT keys

`writeEyeState` stays the one serializer for the persistent part, called from both `saveNBTData` and `setItemNBT`
(trap 2).

**Item and tile NBT** (survives a wrench):

| Key | Type | Meaning |
|---|---|---|
| `btsEyeSchema` | int | 2 for D-49. Absent means legacy; see §13. |
| `btsEyeUnlocked` | int 1..6 | The permanent unlocked tier. |
| `btsEyeMilestones` | compound | `t1`: `{prod: double, res: long}`. `t2`: same. `t3`: `{prod, res, flashes: int}`. `t4`: `{wdmStable: double, res, deepDark: int}`. `t5`: `{goodBatches: int}`. |
| `btsEyeEscrow` | compound | `ItemStack.writeToNBT` of the escrowed core. Absent when empty. |
| `btsEyeDial` | double | The dial setpoint m (1.00-3.00, step 0.25). |
| `btsEyeFeed` | double | The T4 HSM feed setpoint, in L/s (0-1.5, default 0.694). |
| `btsEyeBuffers` | compound | As built (fluid name → long). |
| `btsEyeGalactic` | boolean | **Legacy: read, never written.** Feeds the migration. |

**Tile NBT only** (the live star):

| Key | Type | Meaning |
|---|---|---|
| `btsEyeStar` | compound | `state` (byte), `tier` (byte), `key` (StarDef key), `P`/`Th`/`Om`/`Pi`/`A`/`I` (double), `lock` (int), `rng` (int, the mulberry32 state), `t` (long, field clock), `eyeT` (double), `cycR` (double), `prevE` (double[4] as 4 doubles), `prevWarn` (byte), `ev` {`next` long, `left` int, `warned` bool, `sign` byte}, `cyc` {`eyeT`, `ySum`, `n`, `rMin`, `hsm`}, `batch` {`eyeT`, `ySum`, `n`, `rMin`, `warned`}, `tengam` (double, fractional Purified Tengam owed), `flashWatch` (int, §8). |
| `btsEyeTrans` | compound | `type`, `phase` (byte), `t` (int), `tach` (double), `Mc` (double), `st` (int), `streak` (int), `coreUsed` (bool). |
| `btsEyeRun*` | as built | The planet run in flight. Unchanged. |

The RNG state is saved exactly, so a reload continues the same noise sequence; test 11 relies on this.

---

## 4. The per-second field algorithm

It runs from `onPostTick` when `aTick % 20 == 0`, server side, straight after `drainSockets()`. That is the existing
20-tick drain, so one field second is one real second.

The pure part (`StellarField.step`) matches the prototype's `step()` line for line. The controller supplies the inputs:
trims drained from the Field sockets, the feed availability, the cycle's r, m, q and p.

**Exact-match rules for the golden test:**

- RNG draws come in this order: one `uni` at `light()` if the tier has events; one sign draw at a signed warning;
  then one draw per active gauge per step, in the order P, Θ, Ω, Π.
- Trims are summed in the rig's insertion order. The prototype iterates object keys in insertion order, so use a
  `LinkedHashMap`. In play, the order is the tier's trim-table order.
- The prototype clock `t` increments every step, DARK steps included.

```java
static final int P = 0, TH = 1, OM = 2, PI = 3;
static final double[] ZONE_Y  = { 1.25, 1.0, 0.5, 0, 0 };
static final double[] ZONE_DI = { 1, 0.2, -0.5, -2, 0 };
static int zoneOf(double w) { return w <= 10 ? 0 : w <= 40 ? 1 : w <= 70 ? 2 : w <= 100 ? 3 : 4; }
static double spAt(double eyeT) { return 90 - 180 * Math.min(eyeT, 7200) / 7200; }   // T5 cooling setpoint

/** One field second. Mirrors star-lifecycle.html step(S). */
StepResult step(FieldState s, TierParams T, Overlay ov, Inputs in) {
    // in.m: 1 during a transition, else the dial actually paid for (§6.4); in.q = 1 - 0.05*(ts-1);
    // in.r: this cycle's upkeep fraction; in.p: parallel; in.u: units drained per trim (ordered), each <= cap*p;
    // in.feed: T4 HSM L/s per parallel available this second (= the setpoint when the buffer is full)
    final double m = in.m, q = in.q, r = in.r, p = in.p;
    final StepResult out = new StepResult();

    // ---- events (prototype: the "events" block)
    final double[] E = new double[4];
    int warn = 0;
    final EventDef ev = ov.event(T);                 // the tier's event, or a forced one, or null
    if (ev != null && s.ev != null) {
        if (!s.ev.warned && s.t >= s.ev.next - ev.warn) {
            s.ev.warned = true;
            s.ev.sign = ev.signed ? (s.rng.next() < 0.5 ? -1 : 1) : 1;
        }
        if (s.ev.warned && s.ev.left == 0 && s.t >= s.ev.next) s.ev.left = ev.dur;
        if (s.ev.left > 0) {
            for (int k = 0; k < 4; k++) E[k] += ev.fx[k] * s.ev.sign * q;
            if (--s.ev.left == 0) { s.ev.warned = false; s.ev.next = s.t + 1 + uni(s, ev.every) / m; }
        }
        if (s.ev.warned && ev.warn > 0) warn = s.ev.sign;
    }

    // ---- extra drift d': upkeep sag, T5 plateau, transition swelling
    final double[] dp = { -5 * (1 - r), 0, 0, 0 };
    double sp = 0;
    if (T.batch) {
        sp = spAt(s.batch.eyeT);
        if (sp >= -10 && sp <= 10) dp[TH] += 0.15;                       // crystallisation plateau
        if (sp > 10 && spAt(s.batch.eyeT + 15 * m) <= 10) { warn = 1; s.batch.warned = true; }
    }
    ov.addDrift(dp, s);                               // Collapse phase 1: +2(Mc-0.5)/0.94 on P and Θ; Mass Infusion: +4 on P

    // ---- the gauge update
    final double[] d = T.drift(in.feed, r);           // T4: P -3+5f, Θ -0.5+3f, Ω 1.5f; T6: P -4+5r, Θ -0.5+3r, Ω r
    for (int k : ov.gauges(T)) {                      // ascending P, Θ, Ω, Π
        final double e  = s.x[k] - (k == TH && T.batch ? sp : 0);
        final double c  = ov.coupling(T, s.x, k);     // T3: k==P ? 0.02*Θ : 0
        final double xi = (s.rng.next() * 2 - 1) * T.sigma;
        double dv = m * (d[k] + dp[k] + ov.gain(T, k) * e + c + q * xi);
        for (Map.Entry<TrimDef, Double> t : in.u.entrySet()) {       // trims: NOT time-scaled
            final double f = t.getKey().fx[k];
            if (f != 0) dv += f * t.getValue() / p;
        }
        dv += E[k];                                   // events: NOT time-scaled, already × q
        s.x[k] = clamp(s.x[k] + dv, -120, 120);
    }

    // ---- T6 agent shield, after the update
    if (T.shield) {
        s.A = Math.min(100, 0.95 * s.A + 4 * in.units(AGENT) / p);
        final double c = Math.min(s.A, Math.min(Math.abs(s.x[PI]), 25));
        s.x[PI] -= Math.signum(s.x[PI]) * c;
        s.A -= c;
    }

    // ---- zone, stability, Harmonic Lock, yield
    double worst = 0; int wk = ov.gauges(T)[0];
    final double[] err = new double[4];
    for (int k : ov.gauges(T)) {
        err[k] = s.x[k] - (k == TH && T.batch ? sp : 0);
        if (Math.abs(err[k]) > worst) { worst = Math.abs(err[k]); wk = k; }
    }
    final int z = zoneOf(worst);
    s.lock = z == 0 ? s.lock + 1 : 0;                 // real seconds: the dial cannot shortcut it
    double Y = ZONE_Y[z];
    if (s.lock >= 600) Y = 1.5;
    if (T.ash && in.units(HELIUM) > 0) Y *= Math.max(0, 1 - 0.005 * in.units(HELIUM) / p);
    s.prevE = err; s.prevWarn = warn;
    out.zone = z; out.worst = worst; out.stability = clamp(100 - worst, 0, 100); out.Y = Y; out.warn = warn; out.sp = sp;

    // ---- failure
    if (z == 4) { out.fail = "Breach, " + GAUGE_SYM[wk] + " " + fmtSigned(err[wk]); s.t++; return out; }
    s.I = clamp(s.I + ZONE_DI[z], 0, 100);
    if (s.I <= 0) { out.fail = "Integrity reached 0"; s.t++; return out; }

    // ---- the transition (may bounce, complete or fail; prototype order: after Integrity, before cycles)
    ov.tick(s, z, out);                               // sets out.bounce / out.success / out.fail
    if (out.fail != null || out.stateNotLit) { s.t++; return out; }

    // ---- cycle and batch accounting (the tier may have changed at the bounce: use the current one)
    final TierParams Tn = out.tierAfter(T);
    if (Tn.feed) { final double h = in.feed * m * p; s.cyc.hsm += h; out.hsmToDraw = h; }   // drawn by the controller
    s.cyc.ySum += Y; s.cyc.n++; s.cyc.rMin = Math.min(s.cyc.rMin, r); s.cyc.eyeT += m; s.eyeT += m;
    if (s.cyc.eyeT >= 60 - 1e-9) {
        final double mY = s.cyc.ySum / s.cyc.n;
        if (!Tn.batch) out.product = (Tn.feed ? 3.6864 * s.cyc.hsm : Tn.base * p) * mY * s.cyc.rMin;
        s.cyc = new Cycle(s.cyc.eyeT - 60);           // the carry-over
        out.chargeCycle = true;                       // the controller debits EU and takes upkeep, setting the next r
    }
    if (Tn.batch) {
        final Batch b = s.batch;
        b.ySum += Y; b.n++; b.rMin = Math.min(b.rMin, r); b.eyeT += m;
        if (b.eyeT >= 7200 - 1e-9) {
            out.batchDone = true;
            out.batchMeanY = b.ySum / b.n;
            out.product = 4608 * p * out.batchMeanY * b.rMin;
        }
    }
    s.t++;
    return out;
}

static double uni(FieldState s, double[] ab) { return ab[0] + (ab[1] - ab[0]) * s.rng.next(); }
```

**`FieldRng`** (exact port of the prototype's `mulberry`; Java `int` arithmetic wraps like JS `|0` and `Math.imul`):

```java
final class FieldRng {
    int a;
    FieldRng(long seed) { a = (int) ((seed * 2654435761L) & 0xFFFFFFFFL); }  // = JS (seed*2654435761)>>>0 for seed < 3.39e6
    double next() {
        a += 0x6D2B79F5;
        int t = (a ^ (a >>> 15)) * (1 | a);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }
}
```

**The controller around `step`** (`EyeStarRuntime.second()`):

1. If `!mMachine`, put out any live star (R-8) and return.
2. **DARK / IDLE.** Try the auto-start (§6.3, §7) and publish.
3. **IGNITING.** `x[P] = min(x[P] + 1.5, 0)`, not time-scaled. If `x[P] ≥ −20`, call `light()`. Then `t++`, publish
   and return. While igniting there is no noise, no trim, no upkeep and no output.
4. Work out m (§6.4), paying Time from the Field sockets. During a transition m = 1.
5. Drain the trims from the Field sockets. Each trim fluid of the current tier or overlay is drained up to
   `cap · p · unit` L across every live, non-rejected Field hatch. `u = litres / unit`, fractional. With `rig` set,
   the debug rig's u replaces this.
6. **T4 only.** `feedAvail = min(feedSetpoint, hsmBuffer / (m·p))`.
7. Call `StellarField.step(...)`.
8. Apply the result:
   - `hsmToDraw` comes out of the HSM buffer.
   - `product` goes to the ME output hatch, and the milestone counters are credited.
   - On `chargeCycle`, run `chargeCycle()`.
   - On `fail`, run `fail()` (§6.6).
   - On `batchDone`, emit the BDM and run `startBatch()`.
   - On a transition result, run §7.
9. **T6 only.** Credit the Tengam return: `tengam += agentLitres · 12 / 1000`; whole dust go to the ME bus.
10. Publish a `FieldReadout` to every sensor hatch, and to the GUI sync values.

`light()` follows the prototype:

- state = LIT; I = 100; lock = 0; A = 0; the cycle is reset.
- If the tier has an event: `ev.next = t + uni(every) / min(dialSetpoint, m_max)`.
- T5: `startBatch()`.
- `chargeCycle()`.
- x is **not** reset, so an ignition hands over at P = −20.

---

## 5. Per-tier parameters

All of these are constants in `TierParams` and tunable. d is drift per Eye-second, g is the feedback gain (negative
restores, positive runs away) and σ is the noise half-width. The "Field" column marks the tier where each gauge first
appears.

| Tier | Star (chart) | Gauges | d | g | σ | Coupling | Events |
|---|---|---|---|---|---|---|---|
| T1 | Sol | P | P +0.7 | P −0.02 | 0.5 | none | none (unattended it settles at P ≈ +35, Stable) |
| T2 | Sirius A | P | P +2.0 | P −0.01 | 2.0 | none | none; **ash**: Y × max(0, 1 − 0.005·u_He/p) |
| T3 | Arcturus | P, Θ | P +1.5, Θ +1.0 | P −0.01, Θ +0.01 | 1.5 | P gains +0.02·Θ | Helium flash: every 1,200-2,400 s, 15 s warning, Θ +30/s for 3 s |
| T4 | Neutron Star (Arcturus Remnant) | P, Θ, Ω | P −3+5f, Θ −0.5+3f, Ω 1.5f (f = HSM L/s) | P −0.01, Θ 0, Ω +0.02 | 2.0 | none | Glitch: every 900-1,800 s, no warning, Ω +20 for 1 s |
| T5 | Sirius B | P, Θ (Θ against sp) | P −1, Θ +0.02 (+0.15 while sp ∈ [−10, +10]) | P −0.01, Θ +0.01 on the error | 1.5 | none | Plateau warning 15 s ahead (batch code) |
| T6 | SGR 1806-20 | P, Θ, Ω, Π (+ A) | P −4+5r, Θ −0.5+3r, Ω r, Π 0 | P −0.01, Θ 0, Ω +0.02, Π +0.03 | 2.0 | none | Giant flare: every 60-120 s, 15 s warning **with sign**, Π ±25/s for 4 s |

**Trims.** Field sockets only, never buffered. u is in units, capped at `cap × p` per second, and its effect is
`fx × u / p`.

| Trim (unit) | Fluid (registry name) | Tiers | Effect per unit | Cap |
|---|---|---|---|---|
| Helium (1,000 L) | `Materials.Helium.getGas` | T1, T2 | P −2 (T2: ash) | T1 10, T2 20 |
| Hydrogen (1,000 L) | `Materials.Hydrogen.getGas` | T1, T2 | P +2 | T1 10, T2 20 |
| Helium (1,000 L) | as above | T3 | P −1, Θ +3 | 20 |
| Hydrogen (1,000 L) | as above | T3 | P +3, Θ +0.5 | 20 |
| Super Coolant (100 L) | `Materials.SuperCoolant.getFluid` | T3 / T4, T6 | T3: P −1, Θ −3; T4/T6: Θ −3 | T3 20; T4/T6 10 |
| Spatially Enlarged Fluid (100 L) | `Materials.Space` (`spatialfluid`) | T4, T6 | P −2 | 10 |
| Molten SpaceTime (100 L) | `Materials.SpaceTime.getMolten` (`molten.spacetime`) | T4, T6 | P +2 | 10 |
| Molten Purified Tengam (36 L) | `Materials.TengamPurified.getMolten` (`molten.tengampurified`) | T4, T6 | Ω −4, Θ +1 | 5 |
| DSSF (100 L) | `Materials.DimensionallyShiftedSuperfluid.getFluid` | T5 | Θ −1, P −0.5 | 10 |
| Raw Star Matter (1,000 L) | `Materials.RawStarMatter.getFluid` | T5 | Θ +3 | 5 |
| Helium (1,000 L) | as above | T5 | P +2 | 10 |
| Antipolarization Agent (100 L) | `btsantipolarizationagent` (new) | T6 | Θ +0.5; shield A += 4·u/p | 5 |
| Tachyon Rich Temporal Fluid | `Materials.Time` (`temporalfluid`) | any | the dial (§6.4) and the Core Collapse dose (≤ 100 L/s) | — |

**Zones** (worst |e| across the active gauges):

| Zone | Worst \|e\| | Stability | Y | Integrity per second |
|---|---|---|---|---|
| Resonant | ≤ 10 | ≥ 90 | 1.25 (1.5 after 600 s of Harmonic Lock) | +1 |
| Stable | ≤ 40 | ≥ 60 | 1.00 | +0.2 |
| Strained | ≤ 70 | ≥ 30 | 0.50 | −0.5 |
| Critical | ≤ 100 | > 0 | 0 | −2 |
| Breach | > 100 | 0 | failure | failure |

Stability = `clamp(100 − worst, 0, 100)`.

**Design invariant.** Keep it as a static check in `TierParams` that logs at boot: for every gauge,
`cap × |effect| > m_max·(|d| + σ + |g|·40)`.

---

## 6. Cycles, upkeep, ignition, parallel, dial, EU

### 6.1 Star definitions (per 60 s cycle, p = 1)

The T1-T3 figures are today's per-run numbers prorated per minute; the check is in parentheses.

| Key | Tier | Chart | Upkeep per cycle (× p) | Product per cycle (× p × mean Y × r) |
|---|---|---|---|---|
| `star:sol` | 1 | Star Chart: Sol | H 50,000,000 + He 50,000,000 (2·10⁹ / 20 min) | RSPM 50,000 (10⁶ / 20 min) |
| `star:siriusa` | 2 | Star Chart: Sirius A | H 12,500,000 + He 12,500,000 + RSPM 25,000 (was He 10⁹ + RSPM 10⁶ / 40 min) | ESPM 2,500 |
| `star:arcturus` | 3 | Star Chart: Arcturus | H 12,500,000 + He 12,500,000 + ESPM 1,250 (was H 2·10⁹ + ESPM 10⁵ / 80 min) | RHSP 625 |
| `star:neutronstar` | 4 | **Star Chart: Neutron Star** (new) | Molten SpaceTime 144; HSM feed f·60·m per cycle, drawn per second (default f 0.694 L/s = 41.67 L/cycle) | WDM 3.6864 × HSM fed (153.6 at default) |
| `star:siriusb` | 5 | Star Chart: Sirius B (moved from T4) | per batch: WDM 18,432 (× p); EU per 60 s cycle | per batch: BDM 4,608 × batch mean Y × r |
| `star:magnetar` | 6 | **Star Chart: Magnetar** (new) | WDM 60 + BDM 15 | Magnetar Matter 15 |

**EU per cycle** = `RECIPE_UMV × 4^(N−1) × 1,200 × p`, debited from wireless (BigInteger) when the cycle is charged.
Per parallel that is:

| T1 | T2 | T3 | T4 | T5 | T6 |
|---|---|---|---|---|---|
| 1.51·10¹¹ | 6.04·10¹¹ | 2.42·10¹² | 9.66·10¹² | 3.87·10¹³ | 1.55·10¹⁴ |

### 6.2 Upkeep fraction r

`chargeCycle()` debits the EU first. On a failed debit `r = 0`, and the fluids are still taken (upkeep is paid in
full whatever the yield). Then, for each upkeep fluid, `got = min(buffer, need·p)` and `r = min(r, got/(need·p))`.
`r` holds for the whole cycle:

- the sag on P is −5(1−r) per second;
- T6's feed drift uses r;
- the output is × the cycle's minimum r.

### 6.3 Ignition (T1-T3)

**Auto-start.** The star starts igniting when all of these hold:

- the state is DARK;
- the chart k ≤ 3 and k ≤ installedTier;
- a **Stellar Ignition Charge** is in a plinth input bus;
- the buffers hold at least 10 cycles of upkeep × p, plus the seed cloud (H and He, each `10⁸ × 2^(k−1) × p`);
- the wireless network holds at least 10 cycles of EU.

**What it consumes:** 1 charge, the seed cloud and 10 cycles of EU. Then P = −80 rising at +1.5/s; the star lights at
P = −20 (40 s).

**Relight** after any failure needs a new charge. `/btseye star … lit` skips all of this (§15).

### 6.4 Time dial

- **Setpoint.** `m_set ∈ {1, 1.25, …}`. The screwdriver steps it +0.25 and wraps; the GUI has ± buttons.
- **Cap.** `m_max = 1 + 0.25·max(0, tt − N)`, where tt is the lowest Time Acceleration generator tier (meta + 1) and N
  is the star tier.
- **Cost per second.** `((m_set − 1)/0.25) · 10 · 2^(N−1)` L of Time, drained from the Field sockets.
- **Short payment.** If the Field sockets hold less, `m = 1 + 0.25·paid/(10·2^(N−1))`.
- **During transitions** m = 1.
- **Stabilisation.** `q = 1 − 0.05·(ts − 1)`, where ts is the lowest Stabilisation generator tier (1-9).

### 6.5 Parallel and the installed tier

- `p = 2^(installedTier − starTier)`.
- `installedTier = min(unlockedTier, structureTier, compressionTier)`.
- `structureTier` is cumulative: core = 1; + `eye_beam` = 2; + `eye_jet` = 3; + `eye_arms` = 4; + `eye_arms2` = 5;
  + `eye_rim` = 6. A band only counts if every band below it stands; this is the new "jet requires beam" rule.
- `compressionTier` = the highest N with `fieldMin[COMPRESSION] ≥ COMPRESSION_META_FOR_TIER[N]`.
- Transition preconditions test `structureTier ≥ N+1` and `compressionTier ≥ N+1` directly, not installedTier.

### 6.6 Failure

| Tier | Effect |
|---|---|
| T1-T3 | The star goes DARK, x = 0, A = 0, lock = 0. In-flight output is lost. Relight needs a charge. |
| T4 | The neutron star collapses to a black hole: DARK. Relight Arcturus and swap to the Neutron Star chart; the Core Collapse re-runs without a core. |
| T5 | Fracture: emit `4,608·p·0.25·(batch.eyeT/7200)` BDM; I = 100, lock = 0; `startBatch()` (it takes a new charge, or goes IDLE). |
| T6 | Field decoherence: DARK. A re-merge needs the merger charge, but no core. |

A failure during a transition is §7's failure. In every case event materials are lost and the core stays in escrow.
Unlocked tiers, consumed cores and blocks are never lost.

### 6.7 Target rules (one live star per Eye)

- **Slot chart = the lit star's chart:** it runs.
- **Slot chart = the next tier's chart:** PENDING (§7.1).
- **Planet, empty slot or any other chart:** the star goes out (a T5 batch takes the fracture salvage). A planet target
  runs planets exactly as built.

---

## 7. Transitions as state machines

### 7.1 Common frame

```
LIT(N) --chart(N+1) inserted--> PENDING(N) --all preconditions hold--> TRANSITION --success--> LIT(N+1) / BATCH
   ^                               |                                       |
   |                               +--chart(N) back--> LIT(N)              +--fail / wrench / mallet / structure--> DARK
```

**`beginTransition(type)`** is the one entry point. It:

- checks the preconditions (the scanner lists the missing ones);
- takes the event fluids;
- marks `coreUsed` if the tier is not yet unlocked and a core is in escrow;
- sets m = 1.

**`tickTransition()`** runs inside `step` through `Overlay.tick`.

**`completeTransition()`**:

- `unlockedTier = max(unlockedTier, N+1)`;
- if `coreUsed`, the escrow is cleared (the core is consumed);
- milestones for N+1 start counting.

**Abort** (wrench, soft-mallet, structure break, target change):

- trans = null and the star goes DARK;
- event materials are lost; the core stays in escrow.

### 7.2 Mass Infusion (T1 → T2), Sol becomes Sirius A

| Phase | Rule |
|---|---|
| Pre | T1 milestones met. Stellar Mass Core in escrow (first time). `eye_beam` built. compressionTier ≥ 2. Sol lit, zone ≤ Stable. RSPM ≥ 5,000,000 L buffered. |
| Start | Take 5,000,000 L RSPM. |
| Run, t = 0..599 | T1 params; overlay `d'_P += 4`; T1 trims; Sol upkeep and output continue. |
| Success | t = 600 with no failure: star tier 2 (Sirius A), x and I carried over, lock = 0, `light()`-style cycle reset (R-5). Unlock T2. |
| Fail | Breach or I = 0: Sol is lost (DARK); RSPM spent; core kept. |

### 7.3 Shell Ignition (T2 → T3), Sirius A becomes Arcturus

| Phase | Rule |
|---|---|
| Pre | T2 milestones. Shell-Burning Core in escrow (first time). `eye_jet` (and `eye_beam`) built. compressionTier ≥ 3. Sirius A lit, ≤ Stable. ESPM ≥ 250,000 L. |
| Start | Take 250,000 L ESPM. Θ appears at 0. |
| Run, t = 0..602 | R-4: T2 P parameters; Θ ramps with s = t/600 (`d_Θ = 1.0s`, `g_Θ = 0.01s`, coupling `0.02·Θ·s`); T3 trim table. Forced helium flash: warning at t = 585, Θ +30·q for t = 600..602. |
| Success | t = 603 with no failure: tier 3 (Arcturus). Unlock T3. |
| Fail | As Mass Infusion. |

### 7.4 Core Collapse (T3 → T4), Arcturus becomes the neutron star

This is the prototype's `beginCollapse` / `bounce` / settle.

| Phase | Rule |
|---|---|
| Pre | T3 milestones (first time only). Galactic Upgrade Core in escrow (first time; T4 re-runs need none). `eye_arms` built. compressionTier ≥ 4. Arcturus lit, ≤ Stable. HSM ≥ 5,000 L buffered. |
| 1, fast-forward | Each second, dose up to 100 L of Time from the Field sockets: `tach += dose`, `Mc = 0.5 + tach/100,000`. Overlay `sw = 2(Mc−0.5)/0.94` is added to P and Θ drift. Arcturus upkeep and output continue. About 940 s at the cap. |
| 2, bounce (at tach ≥ 94,000) | `Mc = 1.44`. `s = min(90, floor(SpaceTime buffer / 1,000))`. Take `s·1,000` SpaceTime and 5,000 HSM (if the HSM is gone: supernova, "no neutron seed"). `shock = BOUNCE_SHOCK − s` (150 adopted; the prototype uses 90). P += shock, Θ += shock (clamped), I −= 20. If P > 100 or Θ > 100: **supernova**. Arcturus is lost (DARK); Time, HSM and SpaceTime are spent; the core is kept. |
| 3, settle | Tier = 4; x = (0, +60, +30, 0); lock = 0; `light()` (T4 cycles and the HSM feed start; I resets, R-9). Each second: `st++`; `streak = zone ≤ Stable ? streak+1 : 0`. |
| Success | `streak ≥ 60`: unlock T4, consume the core if used; the neutron star stays LIT. |
| Fail | `st ≥ 180`: "not Stable for 60 s within 180 s". The star is lost; the core is kept. Breach or I = 0 in any phase is the same. |

**Readouts:** phase, Mc, Time used / 94,000, SpaceTime buffered, predicted shock (`BOUNCE_SHOCK − s`), predicted P/Θ
after the bounce, the settle streak.

### 7.5 First Cooling (T4 → T5)

| Phase | Rule |
|---|---|
| Pre | T4 milestones. Subspace Cooling Core in escrow. `eye_arms2` built. compressionTier ≥ 5. Chart Sirius B in the slot (the neutron star, if lit, goes out: one star per Eye). WDM ≥ 18,432·p L. |
| Run | The first batch, under T5 rules (`startBatch`: take the WDM charge, x = (P 0, Θ +90), batch accumulators reset). |
| Success | The batch completes with mean Y ≥ 0.9 and no fracture: unlock T5, consume the core. The BDM is emitted normally. |
| Fail | A fracture, or a completed batch with mean Y < 0.9. The batch's BDM is emitted (salvage or full); the core is kept; the next batch is another attempt. |

Once T5 is unlocked, batches run back to back while WDM charges are buffered; otherwise the state is IDLE.

### 7.6 Magnetar Merger (T5 → T6, and every re-spawn)

| Phase | Rule |
|---|---|
| Pre | T5 milestones (first time). Magnetar Seed Core in escrow (first time). `eye_rim` built. compressionTier ≥ 6. No star lit (a T5 batch in progress takes the salvage rule). Chart Magnetar. Buffers hold WDM 18,432 + BDM 4,608 + Agent 100,000 (× p). |
| Start | Take the charge. x = 0, I = 100, A = 0. T6 cycles begin (WDM 60 + BDM 15 per cycle). |
| 1, imprint | R-6: T6 rules with Π drift +0.5/s, no flares. `inBand = 60 ≤ Π ≤ 90`; `hold = inBand ? hold+1 : 0`. `hold ≥ 120` → phase 2. A timeout at 600 s fails. |
| 2, release | Π within ±40 within 60 s → **success**: the magnetar is LIT under full T6 rules (flares scheduled), unlock T6, consume the core if used. |
| Fail | Breach, I = 0 or a timeout: the charge is lost; the core is kept; the state is DARK. |

---

## 8. Milestones, cores, escrow

Counters are per controller and kept in item NBT. They keep counting after unlock (display only). Resonant time counts
**real seconds** on a star of that tier.

| Step | Milestones on the current star | Core (escrow) | Event |
|---|---|---|---|
| → T1 | Eye formed | none | First Light (ignition) |
| T1 → T2 | RSPM ≥ 20,000,000 L **and** Resonant ≥ 7,200 s | Stellar Mass Core | Mass Infusion |
| T2 → T3 | ESPM ≥ 1,000,000 L **and** Resonant ≥ 10,800 s | Shell-Burning Core | Shell Ignition |
| T3 → T4 | RHSP ≥ 400,000 L **and** Resonant ≥ 14,400 s **and** 3 helium flashes ridden | Galactic Upgrade Core (existing SL-E02) | Core Collapse |
| T4 → T5 | WDM ≥ 147,456 L from cycles with **no second worse than Stable** **and** Resonant ≥ 14,400 s **and** ≥ 1 Deep Dark run completed on this Eye | Subspace Cooling Core | First Cooling |
| T5 → T6 | 6 batches with mean Y ≥ 0.9 and no fracture | Magnetar Seed Core | Magnetar Merger |

**"Ridden" (plan reading).** From the flash's warning until 30 s after its last hit second, the zone never went worse
than Stable. `flashWatch` tracks the window.

**Escrow.**

- Right-clicking the controller with the core for `unlockedTier → unlockedTier+1` moves one core from the hand into
  escrow, if the escrow is empty.
- Any other core is refused with a message naming the core needed.
- Sneak-right-clicking with an empty hand returns the escrowed core when no transition is running (R-14).
- The escrow survives a wrench (item NBT).
- This replaces `beginGalacticUpgrade` / `completeGalacticUpgrade`. `galacticInstalled` becomes legacy (§13).

---

## 9. New items, fluids, blocks and MTE ids

**Items** (append to `SLItems`, via `BTSItems.define`; the field names are the registry names and are frozen once
shipped):

| Field | Display | Notes |
|---|---|---|
| `stellarIgnitionCharge` | Stellar Ignition Charge | stack 16 |
| `stellarMassCore` | Stellar Mass Core | T1 → T2 |
| `shellBurningCore` | Shell-Burning Core | T2 → T3 |
| `subspaceCoolingCore` | Subspace Cooling Core | T4 → T5 |
| `magnetarSeedCore` | Magnetar Seed Core | T5 → T6 |
| `starChartNeutronStar` | Star Chart: Neutron Star (Arcturus Remnant) | T4 target, stack 1 |
| `starChartMagnetar` | Star Chart: SGR 1806-20 (Magnetar) | T6 target, stack 1 |

- `starChartSiriusB` stays and becomes the T5 target; its display name is unchanged.
- `starChartSiriusBCooled` **stays registered** (registry stability) but has no Eye recipe. It gets a reclaim recipe
  back to Sirius B (SL-P36). Its tooltip reads "Obsolete since D-49: craft back into Star Chart: Sirius B".
- `galacticUpgradeCore` is unchanged and is now the Core Collapse core.

**Fluids** (append to `SLFluids`, `liquid(...)` with GT's tinted molten texture):

| Internal name | Display | RGB | K |
|---|---|---|---|
| `btspositronictengamslurry` | Positronic Tengam Slurry | 196, 92, 214 | 2,000 |
| `btsantipolarizationagent` | Antipolarization Agent | 120, 230, 255 | 4 |
| `btsmagnetarmatter` | Magnetar Matter | 214, 40, 140 | 10,000,000 |

**Existing pack items and fluids used.** Verified with javap on 5.09.54.133; the Materials fields exist in 52.594 too.

| Thing | Reference |
|---|---|
| Raw Tengam / Purified Tengam / Attuned Tengam | `Materials.TengamRaw` / `TengamPurified` / `TengamAttuned`. Dusts via `OrePrefixes.dust`. Molten Purified Tengam = `molten.tengampurified` (auto-generated, because `addMetalItems` sets `SMELTING_TO_FLUID`; also named by the Antimatter Forge tooltip). |
| Tengam processing | EMS `RecipeMaps.electroMagneticSeparatorRecipes` (Raw Tengam → Purified Tengam + magnetic Nd/Sm); Polarizer `RecipeMaps.polarizerRecipes` (Purified → Attuned, IV/UHV). Both already in the pack. |
| Semi-Stable Antimatter | `Materials.Antimatter`; fluid `antimatter` (registered in `LoaderGTBlockFluid` and linked to the material). Made by GoodGenerator's Antimatter Forge from Protomatter (`protomatter`). |
| DSSF | `Materials.DimensionallyShiftedSuperfluid` (`dimensionallyshiftedsuperfluid`); made in the DTPF (`PlasmaForgeRecipes`). |
| Time / Space | `Materials.Time` (`temporalfluid`), `Materials.Space` (`spatialfluid`). |
| UXV parts | `ItemList.Field_Generator_UXV`, `Emitter_UXV`, `Sensor_UXV`, `Hull_UXV` (present in both jars). |

**Blocks.** None new (R-12). T5/T6 reuse Galactic Shell Plating `slCasing:12` (`T`), Infinite SpaceTime Energy
Boundary `J` and Quantum Glass `Z`.

**MTE ids** (from the reserved P2/P3 spares):

| Id | Offset | Machine |
|---|---|---|
| **28116** | base+16 | **Stellar Field Sensor Hatch**: `hatch.beyondthestars.stellarfieldsensor`, class `<pkg>/machines/eye/MTEHatchStellarFieldSensor` |
| 28117-28119 | base+17..19 | still spare |

The Field sockets take plain GT input hatches, so they need no new MTE. Add the 28116 line to the `BTSMachines`
ledger comment and register it with one appended line.

---

## 10. Recipes

All recipes are `BTSRecipeGuard.recipe("<ID>", …)`, with lookups through `req` / `fluid` / `nn`. Times are in ticks.
"AL" is a research-station or scanner assembly line in the SL-E02 pattern; every research item is unique.

### 10.1 Antipolarization Agent chain: new `BTSRecipesStellar.java`, `line("stellar")`

| ID | Map | Inputs | Output | EU | Ticks |
|---|---|---|---|---|---|
| SL-S01 | `RecipeMaps.multiblockChemicalReactorRecipes` | dust TengamAttuned ×16; Antimatter 100 L; molten SpaceTime 144 L | Positronic Tengam Slurry 1,000 L | RECIPE_UXV | 400 |
| SL-S02 | `RecipeMaps.vacuumFreezerRecipes` | Slurry 1,000 L; Super Coolant 2,000 L | Antipolarization Agent 1,000 L | RECIPE_UXV | 200 |

The Eye loop returns 12 dust TengamPurified per 1,000 L of Agent consumed (shield trims and merger charge) to the ME
output bus.

### 10.2 Cores and the sensor hatch: append to `BTSRecipesEye.java`

| ID | Output | Research | Items | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-E02 | Galactic Upgrade Core | (unchanged) | (unchanged) | HSM 32,000; SpaceTime 147,456; TranscendentMetal 147,456 | UMV×4 | 48,000 |
| SL-E03 | Stellar Mass Core | Stellar Ignition Charge | Beam Band Casing ×32, Field_Generator_UMV ×16, circuit UMV ×32, plateDense SpaceTime ×16, Emitter_UMV ×8, Stellar Ignition Charge ×16 | **RSPM 256,000**; SpaceTime 36,864; Infinity 73,728 | UMV | 24,000 |
| SL-E04 | Shell-Burning Core | Star Chart: Arcturus (was the Stellar Mass Core, B-62) | Jet Crescent Casing ×32, Field_Generator_UMV ×24, circuit UMV ×48, Ergosphere Field Coil ×16, plateDense SpaceTime ×24, Emitter_UMV ×12 | **ESPM 128,000**; SpaceTime 73,728; TranscendentMetal 73,728 | UMV×2 | 36,000 |
| SL-E05 | Subspace Cooling Core | Star Chart: Sirius B (was the Galactic Upgrade Core, B-62) | Galactic Shell Plating ×64, Field_Generator_UXV ×16, circuit UXV ×16, Casimir Plate ×32, plateDense SpaceTime ×48, Sensor_UXV ×8 | **WDM 147,456 + DSSF 256,000**; SpaceTime 147,456 | UXV | 48,000 |
| SL-E06 | Magnetar Seed Core | Star Chart: SGR 1806-20 (was the Subspace Cooling Core, B-62) | Galactic Shell Plating ×64, Field_Generator_UXV ×32, circuit UXV ×32, Bastion Plate ×32, plateDense SpaceTime ×64, Emitter_UXV ×16 | **BDM 36,864 + Agent 64,000**; SpaceTime 294,912 | UXV×2 | 60,000 |
| SL-E07 | Stellar Field Sensor Hatch | — (Assembler) | Hull_UMV ×1, Sensor_UMV ×2, circuit UMV ×2, plate SpaceTime ×4 | SpaceTime 576 | UMV | 400 |

### 10.3 Charts and parts: `BTSRecipesSingularity.java`

| ID | Change |
|---|---|
| SL-P25 | Same inputs (Tool_DataOrb, circuit UMV, Galactic Shell Plating ×1, HSM 1,000); **output becomes Star Chart: Neutron Star** (proposal: "takes SL-P25's slot"). |
| SL-P26 | **Deleted** (the Sirius B Cooled chart). The ID is retired; keep the gap. |
| SL-P32 | **Obsolete (2.9.0-RC-2 port).** SL-P30..P32 are retired: RC-2 crafts every compression generator in the Condensate Assembler, with no research step, and the port keeps RC-2's generator costs (singularity-ladder-costs.md §9.5, B-29). There is no SL-P32 recipe whose research item could change. Was: research item becomes Star Chart: Neutron Star. |
| SL-P35 (new) | Star Chart: Sirius B (T5): ASM, Tool_DataOrb ×1, circuit UXV ×1, Galactic Shell Plating ×1; **WDM 1,000**; UMV; 200. |
| SL-P36 (new) | Reclaim: Star Chart: Sirius B, Cooled ×1 + programmed circuit 3 → Star Chart: Sirius B ×1; ASM; UMV; 100. |
| SL-P37 (new) | Star Chart: Magnetar: ASM, Tool_DataOrb ×1, circuit UXV ×1, plateDense SpaceTime ×1; **BDM 1,000**; UXV; 200. |
| SL-P38 (new) | Stellar Ignition Charge ×4: ASM, plate Infinity ×4, Emitter_UMV ×1, circuit UMV ×1; **Helium plasma** (`Materials.Helium.getPlasma`) 1,000 L; UMV; 200. |
| SL-P33 / SL-P34 (conditional) | **Replaced by the RC-2 port's audit (costs §9.4, B-29); do not build generator-upgrade recipes.** The SL-P30 pattern is retired. On RC-2 the Eye T2-T4 gates are clean through **SL-P39** (Resonance Chamber II with SpaceTime foils in place of the WDM foil). The Eye T5 gate (compression t4) loops only through **Resonance Chamber III's Black Dwarf Matter foil** (T5's own product). The fix there is a chamber substitute in the SL-P39 pattern (Condensate Assembler, GT's recipe with the BDM foil swapped for a stand-in to be chosen and logged then), claimed as SL-P33. The T6 gate (t5) is clean on RC-2, so SL-P34 stays reserved and unused. Both are P4 work: Eye T5 is beyond D-49's T1-T4. |

### 10.4 Removals and config

- **The interim BDM path.** Delete the `star:siriusbcooled` registration and `SL-P26`. Remove
  `Config.interimBlackDwarfAtEyeT4`; FML drops the stale key. Remove it from `replacementStarsPresent()`.
- **`replacementStarsPresent()`** now requires `star:sol` (RSPM), `star:neutronstar` (WDM) and `star:siriusb` (BDM)
  before TecTech's Eye is cleared. Universium still has no source (B-2, unchanged).

### 10.5 Fair-cost notes, for the cost table in unit 6

**Band blocks, from the mockup counts measured in this session:**

| Band | Galactic Shell Plating | Boundary casings | Quantum Glass | SL-P17 crafts | HSM |
|---|---|---|---|---|---|
| T5 (`eye_arms2`) | 5,065 | 762 | 404 | 317 | 79,000 L |
| T6 (`eye_rim`) | 7,286 | 3,159 | 753 | 456 | 114,000 L |

The real gates are elsewhere:

- 188 compression generators at tier 5 and then 6 (upgrade recipes);
- UXV cores costing 147k WDM / 37k BDM;
- the running costs: DSSF about 100,000 L per batch at T5, and Agent about 100 L/s plus Super Coolant about
  110 L/s at T6.

---

## 11. Structure work

### 11.1 Exporter (`tools/export-structures.js`)

- **Eye tiers 1..6.** Pieces are `eye_core`, `eye_beam`, `eye_jet`, `eye_arms`, **`eye_arms2`** (T5 − T4) and
  **`eye_rim`** (T6 − T5). The existing "earlier piece keeps a cell" join rule applies. Measured this session with the
  exporter's own headless loader:

  | Build | Assembled box (W × H × D) | Blocks | Piece box | Piece blocks | Joins kept by the earlier piece |
  |---|---|---|---|---|---|
  | T4 | 102 × 57 × 137 | 34,502 | `eye_arms` 95 × 10 × 137 | 6,559 | 84 |
  | T5 | 135 × 57 × 137 | 40,733 | `eye_arms2` 135 × 11 × 83 | 6,231 (galactic 5,065, lining 762, sglass 404) | 73 (blade roots into the core band) |
  | T6 | 158 × 57 × 149 | 51,935 | `eye_rim` 158 × 8 × 149 | 11,202 (galactic 7,286, lining 3,159, sglass 753, grb 4) | 112 (arm-tip clamps) |

- **New socket table entries** (`EYE_SOCKETS`):
  - **`f`** Field ×8 and **`n`** Sensor ×8 on the core band's outer `eyeshell` face. The candidate compass angles
    are 15, 75, 105, 165, 195, 255, 285 and 345: clear of the clamp struts at 30 + 60k and of the arm roots at 0, 90,
    180 and 270. `f` sits one row below the band's mid-plane and `n` directly above it, so each sensor sits next to
    its Field hatch. Kind = `eyeshell`, tier 1.
  - **`g`** ×4 at the T6 `grb` cells, voxel (±54, 2, ±51), tier 6.
  - If the reach check finds a candidate sealed by a T5 blade, shift it by ±5° and re-check.
- **Legend.** `f`: Field socket, `EyeHatchElement.Field`, fallback `U`, min 0, hint 5. `n`: Sensor socket,
  `EyeHatchElement.Sensor`, fallback `U`, min 0, hint 6. `g`: `EyeHatchElement.Stellar`, fallback `T`, min 0, hint 4.
- **Checks.** Add Eye T5 and T6 to the assembled-size checks and run the outside-reach check on Eye T1 and **T6**.
  The manifest gets the new pieces, their text and the socket lists.
- **`eye_core` is re-exported** (`U` −16, `f` +8, `n` +8). The f and n cells were `U`, and the sockets fall back to
  `U`, so **every Eye already built still forms** (§13).

### 11.2 Controller structure (`MTEEyeOfHarmonyBTS`)

- `PIECES` gains `PIECE_ARMS2 = "eye_arms2"` and `PIECE_RIM = "eye_rim"`. `bandsFor` goes to 6, and
  `COMPRESSION_META_FOR_TIER` becomes `{-1, 0, 1, 2, 3, 4, 5}`.
- New elements:
  - `f`: `buildHatchAdder(...).atLeast(EyeHatchElement.Field).casingIndex(shellIndex).hint(5).buildAndChain(casing(U))`
  - `n`: Sensor, hint 6, same shape
  - `g`: Stellar, hint 4, `buildAndChain(block(sl, GALACTIC_SHELL))`
  - `h`: gains `EyeHatchElement.Items` (input buses) beside `Gas`
- **The walk.** Bands are checked in order, and a band counts only if the one below it is built (the cumulative
  rule). Each band keeps its own error list and mark-and-truncates its stellar sockets, as now.
- `construct` / `survivalConstruct` go to stack 6, in order and cumulative. Tooltip BOM lines are added for the new
  pieces and chars (`bom(PIECE_ARMS2, 'T')` and so on).
- **`EyeHatchElement`** gains:
  - `Field`: an `MTEHatchInput` adder. `MTEHatchInputME` goes to the rejected list (R-15).
  - `Sensor`: `MTEHatchStellarFieldSensor` only.
  - `Items`: `MTEHatchInputBus`, ME input bus included.
- **Hologram.** Stack 5 = + `eye_arms2`, 6 = + `eye_rim`. Compression is placed at the stack's gate (metas 4/5).
  Dilation and Stabilisation stay at tier 1 unless GT's EOH channels raise them.
- **NEI preview.** BlockRenderer counts tiers by stack, so it should find 6. Re-verify on blockrenderer6343 1.4.21
  (§14). The T6 render is 158 × 57 × 149 and about 52k blocks: re-check B-19's size note.
- **`EyeKit` / `/btseye`.** `t5`/`t6` builds. `/btskit eye` adds:
  - 8 sensor hatches, 8 plain input hatches and 1 input bus;
  - the new charts and cores;
  - 16 ignition charges;
  - Agent, DSSF and Time cells.

---

## 12. UI and automation surfaces

| Surface | What it shows or does |
|---|---|
| **Scanner** (`getInfoData` → `statusLines`) | State; star and tier; p; m (set / paid / max); q; Stability and zone; Y; Lock (n/600); Integrity; every active gauge with its error (T5: Θ, setpoint, error); A (T6); event channel (warning, sign); upkeep r and the next cycle in s; T4 feed setpoint and actual; unlocked tier; milestone progress lines; escrow; transition phase and its readouts (§7.4); Field sockets (live / rejected); sensors; buffers (as built). |
| **GUI** (MUI2, `EyeGui extends MTEMultiBlockBaseGui<MTEEyeOfHarmonyBTS>`) | Terminal lines as the scanner (`createTerminalTextWidget`, `registerSyncValues`). Buttons: dial −/+ and HSM feed −/+ (`createButtonColumn`). A "Field" side panel with one bar per gauge (−120..+120, zone bands, setpoint marker at T5) and the P/Θ dial with band rings (a custom `IDrawable`). The panel is a stretch goal; the bars are required. |
| **Screwdriver on the controller** | Click: dial +0.25 (wraps to 1). Sneak-click: HSM feed +0.1 L/s (wraps 1.5 → 0). Chat confirms. |
| **Stellar Field Sensor Hatch** (MTE 28116, `n` sockets) | GUI cloned from `MTEHatchPHSensorGui` (MUI2). **Channel:** P, Θ, Ω, Π, A, I, Stability, zone (0 Resonant .. 4 Breach), event warning (+ / − / any), upkeep OK (r = 1), T5 setpoint error. **Comparator:** > or <, with a threshold. **Output:** digital 15/0 or analog; analog = `round(15·(v+120)/240)` for gauges and `round(15·v/100)` for 0-100 values. Values are pushed every field second; the hatch emits on all sides, as the pH sensor does. |
| **GT covers on the controller** | The Activity Detector reads `isActive` = LIT / BATCH. The progress fields are **not** used for stars: GT's run loop would fire `outputAfterRecipe`. |
| **The standard rig** (documented in the tooltip and the questions file) | Sensor past its threshold → redstone → GT Fluid Regulator cover (conditional) or AE2 fluid export bus with a redstone card → plain input hatch in an `f` socket. Two sensors per two-sided gauge; the T3 flash warning and the signed T6 flare warning open a bigger valve. |
| **NEI** | (a) The **star pages** on the existing `beyondthestars.recipe.eye` map, per cycle: fluid inputs raised to 4 slots (`maxIO(1, 81, 4, 18)`, a 2 × 2 grid), product per cycle, EU per cycle, "×2 parallel per tier above". (b) A new display map **`beyondthestars.recipe.eyetransition`**: core + event fluids → the next star chart (display), with the description listing the milestones and the rules. (c) A new display map **`beyondthestars.recipe.eyefield`**: per tier, the trim fluids as inputs (units), with effects and caps in the description. Both new maps are `.fake()` and touched at class init, like `EYE`. |
| **Tooltip** | Rewritten for D-49: the six tiers, unlocking, the sockets, the rig. Bill of materials through `bom()` for all six pieces. |

---

## 13. Save migration (Eyes already built at T1-T4)

`EyeNbt.read()` runs when `btsEyeSchema` is absent:

1. **Unlocked tier.**
   - `btsEyeGalactic = true` → `unlockedTier = 4`. They paid for a Galactic core; their neutron star still needs
     Arcturus lit and a core-free Core Collapse.
   - Otherwise set `pendingLegacyUnlock`. At the **first successful walk**,
     `unlockedTier = max(1, min(3, structureTier_legacy))`, where `structureTier_legacy` uses the **old** rule (the jet
     crescent counts without the beam band). This grandfathers T2/T3, which were unlocked by building the band.
   - Then write schema 2.
2. **Milestones** start at 0 for the current tier. Earlier tiers count as met.
3. **A legacy star run in flight** (`btsEyeRunKey` = `star:*`). Its inputs and EU were paid, so credit the full output
   at once on the first server tick, from a fixed legacy table:

   | Run | Output per success |
   |---|---|
   | `star:sol` | 1,000,000 RSPM |
   | `star:siriusa` | 100,000 ESPM |
   | `star:arcturus` | 50,000 RHSP |
   | `star:siriusb` | 18,432 WDM |
   | `star:siriusbcooled` | 4,608 BDM |

   Multiply by `runningSuccesses × runningYield`, log it and clear the keys. Planet runs are untouched.
4. **Buffers** are kept, including leftover helium from the old Sirius B.
5. **Structure.** The `f`/`n`/`g` cells fall back to their casing, so old Eyes form. **A Jet-without-Beam Eye drops
   to structural T1** until the beam band is built (Q-1). The old message names this.
6. **Charts.**
   - An old T4 Eye with Star Chart: Sirius B in the slot now sees a T5 target: "T5 locked: craft Star Chart: Neutron
     Star and run the Core Collapse".
   - Sirius B Cooled charts reclaim through SL-P36.
7. **Wrenched controllers** (item NBT) take the same path on placement.
8. **Docs.** Append to `docs/migration.md`: the above, plus "finish or note in-flight star runs; they are credited in
   full on load".

---

## 14. Version-specific dependencies (port 5.09.52.594 → 5.09.54.133)

Checked with javap on both instance jars (`. a GTNH- Beyond the Stars` = 52.594; `GT_New_Horizons_2.9.0-beta-3` =
54.133).

| Dependency | 52.594 → 54.133 | Impact on this plan |
|---|---|---|
| **TecTech EOH, its casings and all three field-generator families** | 54.133 crafts them in **`gregtech.loaders.postload.recipes.BECRecipes`** (`addEyeOfHarmonyRecipes`, `addEyeOfHarmonyCasings`, `addTimeDilation`, `addSpacetimeCompression`, `addStabilisation`, with nanites and cosmic solder). `ResearchStationAssemblyLine` keeps only one WDM reference, and BECRecipes has two WDM/BDM references. | **High.** SL-E01's TecTech inputs, the SL-P27..P29 batch casings and the SL-P30..P32 gate upgrades copy **52.594** recipes. The B-22 WDM deadlock may not exist on 54.133. Re-decode before deciding SL-P33/P34 (§10.3), and before relying on Time Acceleration / Stabilisation tiers > 1 for the dial and q. `removeAssemblyLineByOutput(Machine_Multi_EyeOfHarmony)` may now find nothing, because the controller is a BEC recipe; `replaceTecTechEyeOfHarmony` must also remove from the BEC map. |
| `MTEMultiBlockBaseGui` (MUI2) | Private helpers changed (item and fluid text rows, void and separation buttons). The protected hooks used here exist in 54.133. | The GUI (unit 6) builds against 54.133 only, using only protected hooks. |
| `MTEHatchPHSensor` / `MTEHatchPHSensorGui` | Identical signatures (`buildUI(PosGuiData, PanelSyncManager, UISettings)`, `useMui2`, threshold, inverted). | Clone-safe. |
| `MTEHatchInputME` | `justUpdated`, `setRecipeCheck` and `doFastRecipeCheck` removed; watchers added. `getStoredFluids` and `IRecipeProcessingAwareHatch` remain. | The drain code is fine; the Field socket rejects ME stocking anyway. |
| `MTEHatchInput` | Now implements `ISmartInputHatch` and has `onPostTick` and MUI2 `buildUI`. | None expected; check that a Field hatch's own tick does not auto-push fluids. |
| `MTEMultiBlockBase` | Adds `IHatchWatcher`; `checkMachine(..., List<StructureError>)` unchanged. | None. |
| `WirelessNetworkManager` | Identical. | None. |
| `GTStructureChannels.EOH_COMPRESSION` / `_DILATION` / `_STABILISATION` | Identical. | None. |
| `tectech.recipe.EyeOfHarmonyRecipeStorage` (private `recipeHashMap`) | Identical. | The reflection stays valid. |
| `Materials` (Tengam ×3, Antimatter, Protomatter, DSSF, Time, Space, SpaceTime, SuperCoolant, WDM, BDM) | Present in both. Fluid names were verified on 54.133 (`antimatter`, `protomatter`, `temporalfluid`, `spatialfluid`, `molten.spacetime`, `molten.tengampurified`, `dimensionallyshiftedsuperfluid`). | Look them up through `req` / `nn`; the boot log names any null. |
| `RecipeMaps.multiblockChemicalReactorRecipes`, `vacuumFreezerRecipes`, `polarizerRecipes`, `electroMagneticSeparatorRecipes` | Present in both. | None. |
| `ItemList.*_UXV`, `Hull_UMV` | Present in both. | None. |
| structurelib 1.4.38 → 1.4.42; blockrenderer6343 1.4.14 → 1.4.21 | Not diffed. | Re-verify the hologram budget, `survivalBuildPiece` and NEI tier discovery for six tiers. |
| GTNewHorizonsCoreMod 2.8.279 → 2.9.61 | Not diffed. | The BHC removal (not D-49). |
| `dependencies.gradle` | Pins `GT5-Unofficial:5.09.52.594:dev` and `StructureLib:1.4.38:dev`. | The port bumps these; this build assumes it has landed. |

The field engine (unit 1) has no GT or Minecraft dependency and is port-proof.

---

## 15. Test plan

### 15.1 Offline golden test: the Java maths equals the prototype

- **`tools/fieldsim-golden.js`.**
  - Extracts the `/*ENGINE-START*/ … /*ENGINE-END*/` block from `tools/fixtures/star-lifecycle-engine.js` (a copy of
    the prototype, stored with its sha1).
  - Runs it under Node `vm` with a harness: `newState(tier, cfg)`, then `step` for N seconds. On `state==='DARK'`
    after a failure it calls `relight(S)` at once and counts the failure.
  - Writes `src/main/resources/assets/beyondthestars/eye/fieldsim-golden.json`. For each case it records: zone-second
    counts, mean Y, failures, cycles, product totals, trim litres, final x/A/I, and an FNV-1a hash of
    `round(x_k·10⁶)` for every gauge every second.
- **Cases:**
  - T1-T6 × rig {off, bang (`fine=false`), two-stage (`fine=true`)} × seeds {1806, 7, 424242} × 14,400 s, at m = 1,
    ts = 1, r = 1, feed 0.694.
  - T4 at feed 1.5.
  - T4 at m = 2.25 with tt = 6, ts = 1 and ts = 9.
  - T6 at r = 0.8.
  - The Core Collapse from T3 two-stage at seed 1806: SpaceTime buffer {0, 30,000, 60,000} × shock {90, 150}, Time
    100 L/s.
- **Java side.** Run `java -cp build/classes/java/main com.nateyburr.beyondthestars.machines.eye.field.FieldSim golden
  <json>`. It needs a plain JDK, no Minecraft and no gradle run.
- **Pass:** every case prints PASS, meaning identical counts and hash, and |Δ| ≤ 1e-9 on the means. The behavioural
  numbers then follow from the golden file. Sanity floor, from the proposal's simulation table:
  - T1 left alone: 0 failures, ≥ 99 % Stable.
  - Two-stage: 0 failures on T1-T6 and 0 fractures on T5.
  - Left alone on T2, T3, T4 and T6: more than 100 failures; T5: every batch fractures.

### 15.2 Dev-server console tests

Run `./gradlew.bat runServer` (one gradle invocation at a time) and type into the console, or use the test instance.
Coordinates use the console form `/btseye … <x> <y> <z> [facing]`.

| # | Commands | Pass |
|---|---|---|
| 1 | `/btseye t6 0 100 0 north gen 9 fill survival` | "FORMED"; `check` repeats FORMED; the build log shows the 6 pieces and the f/n/g socket counts. |
| 2 | `/btseye star 0 100 0 status` | Prints unlocked tier, state, sockets: Field 8, Sensor 8, rim 4. |
| 3 | `star … unlock 1`, `target sol`, `seed 1806`, `lit`, `rig fine`, `sim 14400` | The ledger equals the offline `FieldSim` case "T1 fine 1806" (hash and counts). RSPM out = 240 × 50,000 × mean Y (± rounding). |
| 4 | Remove the charge from the bus, `target arcturus` (unlock 3), insert 1 charge | IGNITING for exactly 40 s, then LIT; 1 charge, the seed cloud (4·10⁸ H + He) and 10 cycles of EU consumed; no product during ignition. |
| 5 | Drain the He buffer, `sim 120` | r < 1, output × r, P sags; status shows "upkeep r 0.xx". |
| 6 | `unlock 1`, `lit sol`, `milestones 1 done`, `core stellarmass`, `target siriusa`, `rig fine`, `sim 700` | Mass Infusion starts at once (PENDING then TRANSITION), RSPM −5·10⁶; at t = 600 the star is Sirius A, T2 unlocked, escrow empty. Repeat with `rig off`: fails, core still in escrow, Sol DARK. |
| 7 | Shell Ignition: as 6 with T2 → T3 | Warning at t = 585 on the event channel; flash at 600-602; Arcturus at 603. |
| 8 | Core Collapse: `unlock 3`, `lit arcturus`, `rig fine`, `milestones 3 done`, `core galactic`, SpaceTime buffer 0, `target neutron`, `sim 1200` | Supernova at the bounce (shock 150 > 100); core in escrow; Time, HSM and SpaceTime spent. With a 60,000 L buffer: survives; settles within 180 s; T4 unlocked; core consumed. `wrench` mid-phase 1: aborted, core kept. |
| 9 | T4 run: `feed 1.5`, `sim 3600` | WDM ≈ 3.6864 × HSM drained × mean Y; HSM drain 1.5 × 60 L per cycle. |
| 10 | First Cooling: `unlock 4`, `milestones 4 done`, `core subspace`, WDM buffered, `target siriusb`, `rig fine`, `sim 7300` | One batch, mean Y ≥ 0.9, T5 unlocked; BDM = 4,608 × mean Y. Fracture path with `rig off`: salvage = 25 % × fraction. |
| 11 | Merger: `unlock 5`, `milestones 5 done`, `core magnetar`, the charge buffered, `target magnetar`, `rig fine`, `sim 400` | The imprint holds 120 s in [60, 90], release within 60 s, LIT T6; Agent use returns 12 Purified Tengam per 1,000 L to the ME bus. |
| 12 | Sensors: `sensor <x y z> P gt 10`, `probe` | Redstone 15 exactly on the seconds where P > 10 (compare with `status`); analog mode equals the §12 formula. |
| 13 | An ME stocking input hatch in an `f` socket | The Eye still forms; the scanner flags it "rejected"; it is never drained. |
| 14 | Persistence: `sim 100`, `save-all`, restart the server, `sim 100` | The final hash equals an uninterrupted offline 200 s run (same seed). The RNG and clock persisted. |
| 15 | Wrench the controller, place it back, rebuild | Status keeps unlocked tier, milestones, escrow and buffers; the star is DARK. |
| 16 | Migration: open a world saved with 0.24.x holding (a) a Galactic T4 Eye, (b) a T3 Eye mid-Sol-run, (c) a jet-without-beam Eye | (a) unlocked 4; (b) RSPM credited, unlocked 3; (c) forms at structural T1 with the beam-band message. All schema 2 after a save. |
| 17 | Boot log | "[BTS] Recipe registration complete: N registered, 0 skipped"; no null fluid warnings for the §9 names; the TierParams invariant check logs OK. |
| 18 | Performance | Time one `checkMachine` at T6 (target < 20 ms, §5.1(b)) and one field second (< 0.1 ms). |

### 15.3 Client checks (manual, after the console tests)

- The hologram projector at stack 5 and 6: holograms, then survival auto-place.
- NEI: the multiblock preview with 6 tiers, the star / transition / field pages, and the SL-S01/S02 chain.
- The GUI bars and buttons; the sensor GUI; textures for the 7 items, 3 fluids and the hatch overlay.

---

## 16. Work breakdown

The units are sequential, one unit each, with one gradle invocation at a time.

- Every unit makes local `wip:` commits staging only its own files, and appends its plan readings to
  `docs/singularity-ladder-questions.md` (next free B-n; note that the file currently has **two** "B-25" headings).
- Shared files take append-only edits: `BTSMachines`, `CommonProxy`, `BTSRecipes`, `en_US.lang`, `CommandBTSKit`,
  `Config`, `docs/migration.md`.
- All units start after the 54.133 port lands. Unit 1 has no GT dependency and could start earlier, if the gradle
  slot is free.

### Unit 1: field engine and golden harness (pure Java)

- **Owns (new):**
  - `<pkg>/machines/eye/field/`: `FieldRng`, `TierParams`, `TrimDef`, `EventDef`, `FieldState`, `StellarField`,
    `Overlay`, `CollapseOverlay`, `FieldReadout`, `FieldRig`, `FieldSim`;
  - `tools/fieldsim-golden.js`;
  - `tools/fixtures/star-lifecycle-engine.js`;
  - `src/main/resources/assets/beyondthestars/eye/fieldsim-golden.json`.
- **Does:** §4 and §5 exactly, the prototype's rigs, and the collapse overlay with `BOUNCE_SHOCK` as a parameter.
  No Minecraft or GT imports in `field/`.
- **Done when:** `compileJava` passes and `FieldSim golden` prints PASS for every §15.1 case.

### Unit 2: content (items, fluids, sensor hatch, recipes, textures, lang)

- **Owns:**
  - `items/SLItems.java` and `fluids/SLFluids.java` (append);
  - `<pkg>/machines/eye/MTEHatchStellarFieldSensor.java` and `StellarFieldSensorGui.java` (new);
  - `machines/BTSMachines.java` (append 28116);
  - `BTSRecipesStellar.java` (new) and `BTSRecipes.java` (append `line("stellar")`);
  - `BTSRecipesEye.java` (SL-E03..E07) and `BTSRecipesSingularity.java` (SL-P25 output, SL-P26 delete, SL-P32
    research is obsolete, see §10.3; SL-P35..P38; SL-P33/P34 are P4 work, see §10.3. The port's WDM-free Resonance
    Chamber II is **SL-P39**, so do not reuse that ID);
  - `Config.java` (remove `interimBlackDwarfAtEyeT4`);
  - item and hatch textures plus the generator script lines;
  - `en_US.lang`;
  - `CommandBTSKit` / `EyeKit` kit contents only.
- **Does:**
  - the §14 BEC re-audit of the TecTech generator and casing recipes. Done by the 2.9.0-RC-2 port: costs doc §9.4
    and B-29; the SL-P33/P34 position is in §10.3;
  - the sensor hatch consumes `FieldReadout` (from unit 1).
- **Done when:** the boot log is clean (test 17); the NEI chain shows SL-S01/S02; `/btskit eye` hands out the new
  parts.

### Unit 3: structures and sockets

- **Owns:**
  - `tools/export-structures.js` and the regenerated `structures/*` (`eye_core`, `eye_arms2`, `eye_rim`,
    `manifest.json`);
  - `MTEEyeOfHarmonyBTS` **structure sections only** (pieces, definition, walk, cumulative bands, compression table,
    construct / survival, tooltip BOM);
  - `EyeHatchElement` (`Field`, `Sensor`, `Items`; rejected list);
  - `EyeKit` (t5 / t6 build);
  - `CommandBTSEye` (`t5` / `t6` tokens).
- **Done when:**
  - `node tools/export-structures.js --check` is clean;
  - tests 1 and 2 pass;
  - a legacy T4 Eye built with the pre-unit jar re-forms (f/n fallback);
  - hologram stacks 5 and 6 project.

### Unit 4: star runtime T1-T4, registry, NBT, migration, scanner, debug commands

- **Owns:**
  - `<pkg>/machines/eye/star/`: `StarDef`, `EyeStarRuntime`, `EyeNbt` (new);
  - `MTEEyeOfHarmonyBTS` (runtime: `onPostTick` hook, star bypass of `checkProcessing`, target rules, screwdriver,
    `writeEyeState` / `loadNBTData` through `EyeNbt`, `getInfoData`, soft-mallet and wrench handling);
  - `EyeRecipeRegistry` (star defs replace the star `EyeRecipe`s; input-fluid set; `replacementStarsPresent`; NEI star
    pages);
  - `EyeRecipe` (planets only);
  - `EyeRecipeMaps` (4 fluid input slots);
  - `CommandBTSEye` (`star` subcommands: `status`, `lit`, `target`, `seed`, `rig`, `sim`, `set`, `sensor`, `probe`,
    `wrench`);
  - `docs/migration.md` (append).
- **Done when:** tests 3, 4, 5, 9, 12-16 pass. T4 is reachable here via `lit` only.

### Unit 5: unlocking, transitions, T5 and T6

- **Owns:**
  - `<pkg>/machines/eye/star/`: `EyeTransition` and its five subclasses, `EyeMilestones` (new);
  - `MTEEyeOfHarmonyBTS` (escrow right-click, PENDING, transition hooks, Deep Dark counter, T5 batch and T6 shield
    wiring, Tengam return);
  - `EyeRecipeMaps` and `EyeRecipeRegistry` (the `eyetransition` and `eyefield` display maps);
  - `CommandBTSEye` (`unlock`, `milestones`, `core`, `transition`, `event` subcommands).
- **Done when:** tests 6, 7, 8, 10 and 11 pass; the golden collapse cases match in-world at shock 90 with `set
  shock 90`.

### Unit 6: GUI, tooltip, documentation and acceptance

- **Owns:**
  - `<pkg>/machines/eye/EyeGui.java` (MUI2, 54.133 hooks);
  - `MTEEyeOfHarmonyBTS.createTooltip` and `getGui`;
  - `en_US.lang` (GUI strings);
  - `docs/singularity-ladder-build.md` ("Contract additions (Eye, D-49)", "What's in the build");
  - `docs/singularity-ladder-questions.md` (B-1/A-2 superseded, B-3 answered, B-5 per-cycle, B-6 T3 needs T2, B-9
    partly reversed, Q-31 replaced, plus the plan readings);
  - `docs/singularity-ladder-costs.md` (T5/T6 cost table from `manifest.json` × the recipes);
  - the design repo's `PROPOSAL.md` D-register updates (D-30, D-31, D-34, D-41, D-42, D-43), or hand these to the
    main session.
- **Done when:** all of §15.2 and §15.3 pass; the cost table is committed.

---

## 17. Contradictions and open items for review

1. **T2 optional (D-42, Q-2) against "each tier unlocks the next" (D-49).** Resolved as "required" (Q-1). D-49 says to
   change the others. A side effect: Eyes built jet-without-beam lose T3 until the beam band is built.
2. **Time Dilation / Stabilisation "placeholders, do not invent their maths" (Q-20 ruling) against the proposal's
   §3.6 maths.** The approval of the proposal supersedes the ruling.
3. **"Stabilisation starts at T7" (D-42) against q on every tier.** D-49 revises D-42.
4. **"Each band unlocked by an upgrade core consuming the previous one" (D-34)** against cores made from the
   previous tier's **product**. D-34 is to be revised (proposal §6.19).
5. **"Antipolarization agent … tengam and antimatter to feed" (D-49).** The proposal's continuous feed is WDM + BDM,
   and the Agent is a shield trim "as needed". In practice it runs at about 100 L/s, which is effectively a feed. If
   I meant the Agent itself is the upkeep, move it from trim to upkeep.
6. **Bounce +90 (proposal, prototype engine) against +150 (the B-25 recommendation).** +150 adopted as a constant.
7. **Ignition charge "in any Eye input socket" when every socket is a fluid hatch.** R-1 adds input buses on the
   plinth.
8. **Proposal §6 "× m" on output** would double-count against the prototype. Per hour, not per cycle (R-10).
9. **The bounce's Integrity −20 is wiped by the prototype's `light()`** (R-9).
10. **Structure-incomplete "freezes" against "goes out"** in the same section (R-8).
11. **Port.** On 54.133, TecTech's Eye, its casings and its field generators are BEC recipes. SL-E01 and SL-P27..P32
    were copied from 52.594's research-station recipes and must be re-audited. TecTech's EOH controller removal may
    silently find nothing.
12. **Magnetar Matter has no consumer yet** (its D-43 use, T9 pellets, is P5). Universium still has no source (B-2).
13. **The questions file has two "B-25" entries** (the prototype readings and the compression upgrades); renumber one.
14. **The prototype lives only in my local work folder.** Copy it into the design repo.

## My answers (2026-10-12, after this plan) — these override the plan where they differ

1. **Magnetar (T6) feed: the Antipolarization Agent is the CONSTANT feed (upkeep).** The White + Black Dwarf Matter
   merger spawns the magnetar and tops it up to keep it stable. This reverses the proposal's "WDM + BDM constant feed,
   Agent as an as-needed shield trim" (plan §17 item 5). Re-balance the T6 upkeep table and field trims accordingly.
2. **Eye T2 is REQUIRED** (strict ladder per D-49). An Eye with the T3 jet crescent but no T2 beam band runs as T1
   until the beam band is built (plan §17 item 1, adopted). When the Homogeny returns it is an alternative Excited
   plasma source, not a way to skip T2.
3. **Centre render (user, 2026-10-12: "where is the render btw?")**: the Eye renders its current object at the centre
   of the structure — the live star for T1-T3 (size/colour per star), the neutron star at T4, the cooling white→black
   dwarf at T5, the magnetar at T6, and the collapse/supernova-suppression event as a visible transition. Reuse the
   pack's own renderers where they fit (TecTech's Eye of Harmony render and the Forge of the Gods star render in the
   RC-2 GT jar; javap them for how they spawn/sync/scale), client cosmetic behind the existing `eyeRenderCentre`
   config (which must finally do something), no orphan render blocks on unform/break/unload. The Kerr's black-hole
   render is done separately on the `kerr-redesign` branch.
