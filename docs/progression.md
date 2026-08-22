# D-23 — the PROGRESSION system (0.22.0, revised in 0.22.1)

**Design of record.** This file supersedes `progression-milestones.md`, which covered only the
milestone half. `progression-recon.md` stays as the research trail — how the Godforge does it,
what of it is reachable from this addon, and what is not — and is deliberately not maintained
against the shipped code.

The ruling, in my own four parts:

1. build **both** the milestone system **and** the node-graph upgrade tree;
2. rewards are **both** — early tiers pay **multipliers** into the existing EU formula, later
   tiers pay **unlocks**;
3. persistence is **per controller** (the main station's NBT), like the Godforge;
4. **no back-fill** — progress starts at zero on every plant, with a debug command to set it.

Two source labels, one ruling: the code calls the milestone layer **D-23** and the upgrade tree
**D-24**. They are the two halves of this row.

---

## 0. The shape in one paragraph

The plant watches four numbers it already knew about itself. Each is a **line** with seven
tiers. Tiers 1–4 of a line pay a **multiplier** into the plant's published EU/t derivation;
tiers 5–7 pay an **unlock**, and every unlock is a *new maximum* — nothing in this system gates
a capability a plant already had. Every tier also pays **progression points**, triangularly
(`1+2+…+t`), so a full 28-tier sweep pays **112**. Those points are the only currency of a
**25-node upgrade tree** that costs exactly 112. Milestones are what the plant earns by
running; the tree is what the player chooses to do with it.

---

## 1. The four lines

`BTSMilestone`, a four-constant enum carrying the thresholds, the multipliers, the grant strings
and the NBT key for each line.

| Line | Counter unit | Fed at | Guard |
|---|---|---|---|
| **UPTIME** | generating ticks | `onRunningTick`, inside `if (emitted > 0)` | server-only, once a tick; `checkMachine` never calls it |
| **BURNUP** | `ROD_MULT`-weighted fuel segments | `checkProcessing`, in each of the three **commit** branches | the peek loop that runs on every failed `checkProcessing` scores nothing |
| **REPROCESSING** | loads + batches | `runDerrickTick` / `runAnnexTick`, on the **deposit** | scored on the deposit, never the tick — a load held for a full bus scores once |
| **TURBINE** | perfect cycles | `evaluateCycle`, inside `if (hadSteam)` when `good >= GALLERY_STAGES` | needs the steam slug *and* all eight injectors timed |

**No new tick loop was added.** Every counter rides a loop the plant already ran, and none of
them is written by `checkMachine` / `resetPieceState`, which fire on **every neighbour update**.
`resetPieceState` names the exclusion in its own comment, beside the identical one for
`derrickProgress` / `annexProgress`.

Character of each line:

* **UPTIME** counts only *generating* ticks. An idle chunk-loaded plant earns nothing, and no
  amount of engineering accelerates it — so it gets the shallowest ladder (×2 a tier) and
  carries the early multipliers. It is the "just keep it lit" line.
* **BURNUP** is weighted by `ROD_MULT` (`{0, 1, 2, 5, 9}`), so it rewards the **fuel chain**
  and not the rod count: one committed cascade cycle scores `Σ liveByType[t] × ROD_MULT[t]`.
  201 uranium pillars score 201; 201 naquadah-bonded pillars score 1,809. A bus-fallback slice
  runs at half output and scores at half rate. A starter plant on uranium tops out around tier
  3 — this line saying *"your fuel chain is not finished"* in the only language it has.
* **REPROCESSING** is the only line that needs a **building**, so its rewards go straight back
  into the two process modules: a richer by-product stream, faster cycles, a bigger batch. It
  is also the only line that **accelerates itself**.
* **TURBINE** needs the steam slug **and** all eight injectors landed. A fast clock with no
  fuel scores nothing (no slug, no credit); a half-plumbed chamber scores nothing (7/8 is not
  perfect).

### Thresholds

Explicit `long[7]` tables, not a logarithm. The Godforge's `log()` only earns its keep because
it has unbounded post-tier-7 levels; we have exactly seven, so a table is exact, has no float
edge case at `total = 0`, and can be read off by a player. `tierOf` is
`count(threshold <= counter)`, capped at 7.

| Tier | UPTIME | BURNUP | REPROCESSING | TURBINE |
|---|---|---|---|---|
| 1 | 72,000 | 2,000 | 250 | 1,800 |
| 2 | 144,000 | 5,000 | 700 | 5,000 |
| 3 | 288,000 | 13,000 | 1,900 | 15,000 |
| 4 | 576,000 | 34,000 | 5,200 | 45,000 |
| 5 | 1,152,000 | 88,000 | 14,000 | 135,000 |
| 6 | 2,304,000 | 230,000 | 38,000 | 400,000 |
| 7 | 4,608,000 | 600,000 | 100,000 | 1,200,000 |

### What those thresholds cost in real time

Rates on a maxed Build Tier 5 plant (201 naquadah-bonded pillars, default
`genICampaignMinutes = 720` → one cascade cycle per 108,000 ticks = 90 minutes):

| Line | Ceiling rate | Where it comes from |
|---|---|---|
| UPTIME | **72,000 /h** | one per generating tick |
| BURNUP | **1,206 /h** | 201 × 9 = 1,809 a cycle, one cycle per 90 min |
| REPROCESSING | **480 /h** | Derrick 1 per 200 t (360/h) + Annex 1 per 600 t (120/h) |
| TURBINE | **3,600 /h** | one perfect cycle per `CYCLE_SLUG_TICKS` = 20 ticks, i.e. 1 Hz |

| Tier | UPTIME | BURNUP | REPROCESSING | TURBINE |
|---|---|---|---|---|
| 1 | 1.0 h | 1.7 h | 0.5 h | 0.5 h |
| 2 | 2.0 h | 4.1 h | 1.5 h | 1.4 h |
| 3 | 4.0 h | 10.8 h | 4.0 h | 4.2 h |
| 4 | 8.0 h | 28 h | 10.8 h | 12.5 h |
| 5 | 16 h | 3.0 d | 1.2 d | 37.5 h |
| 6 | 32 h | 8.0 d | 3.3 d | 4.6 d |
| 7 | 2.7 d | 20.7 d | 8.7 d | **13.9 d** |

> **The TURBINE thresholds were re-derived (F5, 0.22.1).** `progression-milestones.md` sized
> them against a claimed 10,800 cycles/h. The sustainable rate is **1 Hz**, not 3 Hz: a cycle
> drinks `CYCLE_SLUG_TICKS × steamPerTick`, the buffer refills at `steamPerTick` a tick, so
> steady state is one cycle per 20 ticks and an hour holds **3,600** of them. The buffer unlocks
> raise the *burst*, never the fill rate, and `evaluateCycle` scores exactly one point per
> accepted cycle — nothing in the plant can raise the ceiling. Against the real rate the 0.22.0
> ladder made tier 7 a **1,000-hour / 41.7-day** line and gated the last 28 tree points behind
> it. 10,800 against 3,600 is exactly ×3, so the whole ladder is divided by three — which is the
> same thing as sliding it down one rung and growing a new ×3 bottom rung at 1,800. Tier 7 is
> now **333 h / 13.9 d**, which is the number the original sizing was aiming at, and the line
> sits between REPROCESSING (208 h) and BURNUP (497 h).
>
> The other three columns were checked against their real rates at the same time and are correct
> as written: UPTIME 72,000/h → 64 h, REPROCESSING 480/h → 208 h, BURNUP 1,206/h → 497 h. The
> REPROCESSING figure supersedes `progression-milestones.md`'s 300/h sustained — both modules
> running clean give 480/h, and the line's own tier 6 unlock plus the tree's `MODULE_SPEED`
> channel push that to **904/h**, which is why it finishes earlier than its raw thresholds
> suggest.

Pacing, with those corrections: **tier 1 on all four inside two hours** — the drip starts
immediately, which matters because progress starts at zero — **tier 4 everywhere inside about
1.5 days** on the three fast lines, and a full sweep measured in weeks, paced by BURNUP.

---

## 2. Grants — tiers 1 to 4, the multiplier

| Tier | UPTIME | BURNUP | REPROCESSING | TURBINE |
|---|---|---|---|---|
| 1 | ×1.020 | ×1.030 | ×1.020 | ×1.025 |
| 2 | ×1.050 | ×1.070 | ×1.045 | ×1.050 |
| 3 | ×1.090 | ×1.120 | ×1.070 | ×1.085 |
| 4 | ×1.150 | ×1.200 | ×1.100 | ×1.120 |

Tiers 5–7 hold the tier-4 value — the ladder stops paying multiplier at 4 and starts paying
unlocks. All four multiply into **one** term, `BTSProgression.outputMilli()`, which is exactly
`×1.700` at all-tier-4 and above.

**Why the EU and not the steam.** Steam L/t is what sets the feedwater draw, the waste-water
output and the export line. A milestone that raised steam would raise the water demand of a
plant that is already exactly supplied, and could starve one that ran fine yesterday. The
ruling asked for a reward in EU; it lands in EU, and the MATH panel prints it as a term of
step 3.

---

## 3. Grants — tiers 5 to 7, the unlocks

Every one is a **new maximum** or a **reduced cost**. Read `BTSProgression`'s unlock methods:
at tier < 5 each returns its base argument unchanged.

| Line | T5 | T6 | T7 |
|---|---|---|---|
| **UPTIME** | `CONDENSATE RECOVERY I` — feedwater draw halved again | `STEAM ACCUMULATOR` — the chain holds 1.6× the slugs | `CONDENSATE RECOVERY II` — feedwater draw quartered again |
| **BURNUP** | `CONTROL ROD WITHDRAWAL I` — throttle cap 26/24 | `CONTROL ROD WITHDRAWAL II` — 28/24 | `CONTROL ROD WITHDRAWAL III` — 30/24 |
| **REPROCESSING** | `DEEP CUT` — Derrick tantalum every 3rd load | `PROCESS INTENSIFICATION` — Derrick and Annex cycles −20% | `YIELD UPLIFT` — Annex batch ×1.2 (2.4 **yellowcake**) |
| **TURBINE** | `OVERSPEED TRIM I` — redline **may be set to** 13,500 (RPM LIMIT field); gain follows it | `OVERSPEED TRIM II` — redline **may be set to** 15,000; gain follows it | `BLADE METALLURGY` — rotor wear rolled half as often |

Those names are the strings the panel prints, character for character.

**The redline and the cycle gain move together, on purpose.** Equilibrium RPM is
`gain-per-cycle × 10.486` — decay is `×995/1000` a tick, so a 20-tick cycle loses 9.54 % of
standing RPM and gains `176 × P × timed/8`. At P = 6.5 and 8/8 that is 1,144 a cycle against a
9.54 % loss, settling at ≈11,993 — which is why the stock plant pins itself at the 12,000
redline. Raising a redline **without** raising the gain would move a ceiling the plant can never
touch, so `OVERSPEED TRIM` raises both.

**And OVERSPEED TRIM is opt-in (F4, 0.22.1).** In 0.22.0 the tier raised the redline and the gain
by itself. Chamber coolant draw scales with the **square** of RPM, so a plant plumbed for exactly
12,000 saw steady-state draw rise 26 % at T5 and 56 % at T6, and the TC-7 interlock then started
refusing cycles on a plant that had run fine the day before — the one thing this project's rules
say an update may not do. `BTSProgression.redlineRpm` and `rpmGainMilli` now return a **ceiling**;
what the fan is held to is `MTEReactorCoreMk1.operatingRedline(ceiling, rpmLimitSetting)`, and
`rpmLimitSetting` is a persisted field the player types into a new **RPM LIMIT** box beside the
rods box (default 12,000, clamped server-side to the ceiling exactly like the throttle). The
**gain** is clamped to the same authorised overspeed, not just the redline: a plant on Lv 3
propellant settles at ≈9,227 RPM, well under the redline, so a raised gain alone would still have
lifted its equilibrium — and its coolant bill — without the redline ever binding. Every readout
prints the operating redline and names the unlocked headroom beside it, so the trim is offered
rather than applied.

**Deliberately NOT built: Build Tier 6/7.** Extending `MAX_TIER` means new `coreShape` layers,
new `CUT` / `COLUMN_CAP` entries, and — because every attached building's offset derives from
`edgeFor(tier)` — moving the Spine, Spiral, manifold and both Cooling Towers on the 50×50 pad.
That cannot be verified without building one in world, and un-forming the standing plant is
exactly the failure this release is under orders to avoid. The BURNUP ladder delivers the same
class of reward (+25 % pillar output at 30/24) through three clamps.

---

## 4. The upgrade tree

`BTSUpgrade`, a 25-node enum. **The currency is progression points and nothing else.** That is
the Godforge's own answer — its milestones grant graviton shards triangularly, its tree spends
them, and the two totals match exactly at 112 against a 112-shard sweep. `BTSMilestone.points`
pays out on that curve, so this tree is costed against it and lands on the same number: **112
total, 28 per branch, which is exactly what one milestone line pays out.**

An item cost was the alternative and was deliberately not taken: it needs a per-node "cost paid"
flag in NBT, which is a second persistent shape and a second failure mode on the one axis this
release is most exposed on — for no design gain, since the controller has no free input bus of
its own.

### Every edge, gate and pixel is derived, not typed

A node knows only its **branch**, its **depth** and its **cost**. The graph falls out:

* **prerequisites** — the node one step shallower on the same branch (depth 1 hangs off the
  root), plus, at depth 6, the **depth-4 node of the next branch round**. That cross-link is
  what makes this a graph and not four separate ladders. 24 spine edges + 4 cross-links = **28
  edges**.
* **the milestone gate** — a node at depth *d* on branch *b* needs line *b* at tier *d*. One
  sentence, no table, and it welds the two systems together: the branch that rewards the
  turbine is the branch the TURBINE line opens.
* **the connector lines the GUI draws** — straight off the prerequisite edges, so the picture
  and the rules physically cannot disagree. This is the one place the implementation improves
  on the Godforge, which maintains a second 37-entry line list by hand.

### The nodes

| Node | Branch / depth | Cost | Prereqs | Gate | Effect |
|---|---|---|---|---|---|
| `COMMISSIONING` | root | 0 | — | — | none — the certificate that opens the tree |
| `FEEDWATER_PREHEAT` | UPTIME 1 | 1 | root | UPTIME T1 | `OUTPUT` +20 |
| `MODERATOR_TRIM` | BURNUP 1 | 1 | root | BURNUP T1 | `OUTPUT` +30 |
| `ORE_SORTING` | REPROC 1 | 1 | root | REPROC T1 | `OUTPUT` +20 |
| `ROTOR_BALANCING` | TURBINE 1 | 2 | root | TURBINE T1 | `ROTOR_WEAR` +250 |
| `SURFACE_CONDENSER` | UPTIME 2 | 2 | Preheat | UPTIME T2 | `CONDENSATE` +1 |
| `ROD_SEAL_UPGRADE` | BURNUP 2 | 2 | Moderator | BURNUP T2 | `THROTTLE` +2 |
| `SOLVENT_RECOVERY` | REPROC 2 | 2 | Sorting | REPROC T2 | `MODULE_SPEED` +100 |
| `MAGNETIC_BEARINGS` | TURBINE 2 | 3 | Balancing | TURBINE T2 | `RPM_GAIN` +100 |
| `PIPEWORK_LAGGING` | UPTIME 3 | 3 | Condenser | UPTIME T3 | `OUTPUT` +30 |
| `RECLADDING_LINE` | BURNUP 3 | 3 | Rod Seals | BURNUP T3 | `OUTPUT` +30 |
| `EXTRA_CENTRIFUGE` | REPROC 3 | 3 | Solvent | REPROC T3 | `ANNEX_BATCH` +250 |
| `NOZZLE_REGRIND` | TURBINE 3 | 4 | Bearings | TURBINE T3 | `OUTPUT` +30 |
| `ACCUMULATOR_BANK` | UPTIME 4 | 4 | Lagging | UPTIME T4 | `BUFFER` +500 |
| `CASCADE_SEQUENCER` | BURNUP 4 | 5 | Recladding | BURNUP T4 | `OUTPUT` +40 |
| `TAILINGS_RECLAIM` | REPROC 4 | 4 | Centrifuge | REPROC T4 | `TANTALUM` +1 |
| `OVERSPEED_CERT` | TURBINE 4 | 5 | Nozzles | TURBINE T4 | `REDLINE` +100, `RPM_GAIN` +100 |
| `RESIDUAL_HEAT_SINK` | UPTIME 5 | 6 | Accumulator | UPTIME T5 | `HEAT_SINK` +4, `DECAY_WINDOW` +500 |
| `DEEP_BURN_PROFILE` | BURNUP 5 | 5 | Sequencer | BURNUP T5 | `THROTTLE` +2, `HEAT_SINK` +2 |
| `MODULE_AUTOMATION` | REPROC 5 | 8 | Tailings | REPROC T5 | `MODULE_SPEED` +400 |
| `SUPERALLOY_BLADES` | TURBINE 5 | 6 | Overspeed | TURBINE T5 | `ROTOR_WEAR` +500, `OUTPUT` +30 |
| `BASELOAD_LICENCE` | UPTIME 6 | 12 | Heat Sink **+ Cascade Sequencer** | UPTIME T6 | `OUTPUT` +90, `CONDENSATE` +1 |
| `OVERPOWER_LICENCE` | BURNUP 6 | 12 | Deep Burn **+ Tailings Reclaim** | BURNUP T6 | `THROTTLE` +2, `OUTPUT` +60 |
| `CLOSED_FUEL_CYCLE` | REPROC 6 | 10 | Automation **+ Overspeed Cert** | REPROC T6 | `OUTPUT` +70, `ANNEX_BATCH` +250 |
| `SYNCHRONOUS_CONDENSER` | TURBINE 6 | 8 | Superalloy **+ Accumulator Bank** | TURBINE T6 | `REDLINE` +200, `RPM_GAIN` +200 |

The four bold prerequisites are the cross-links. Every capstone reaches one branch round to its
right, so the late tree forces breadth the way the Godforge's convergence does.

### Buying, refunding, and who decides

**Left-click a node to buy, right-click to refund.** Refunds are **free, unlimited and
reversible** — the Godforge's own rule, and the only rule that makes a branching tree
explorable rather than a one-way trap. The single refusal is the one the Godforge has too: a
refund may not orphan a node still held.

**Purchases are server-authoritative.** The click carries a node *index* through a
server-synced action whose body runs only on the server; `BTSProgression.canBuy` re-asks
ownership, prerequisites, gate and budget against the server's own record. The greying-out in
the panel is decoration.

---

## 5. The twelve effect channels

`BTSChannel` is the whole safety argument for the tree. A node does not carry code — it carries
a **channel and a positive delta**, and the plant reads each channel through one accessor. So
the set of things the tree can possibly affect is one 20-line file; every channel's base is a
**no-op**; every delta is positive and every channel monotone in the player's favour; and the
effect text the GUI prints is *generated* from the channel and the delta, so a node cannot
advertise one thing and do another.

| Channel | Base | Full-tree total | What it moves |
|---|---|---|---|
| `OUTPUT` | 1000 | **×1.450** | the EU/t derivation — the fifth factor |
| `THROTTLE` | 0 | **+6** | control-rod cap, on top of BURNUP's |
| `CONDENSATE` | 0 | **+2** | right-shifts on the feedwater draw |
| `BUFFER` | 1000 | **×1.50** | the standing steam buffer |
| `RPM_GAIN` | 1000 | **×1.40** | RPM gained per accepted cycle |
| `REDLINE` | 1000 | **×1.30** | the turbine redline |
| `ROTOR_WEAR` | 1000 | **×1.75** | the wear divisor — a bigger divisor is a rarer roll |
| `MODULE_SPEED` | 1000 | **×1.50** | Derrick and Annex cycle speed |
| `ANNEX_BATCH` | 1000 | **×1.50** | the Annex's batch size |
| `TANTALUM` | 0 | **+1** | loads shaved off the Derrick's tantalum interval |
| `HEAT_SINK` | 0 | **+6** | extra heat bled off per tick |
| `DECAY_WINDOW` | 1000 | **×1.50** | the post-shutdown decay-heat window divisor |

Nine of the twelve reach the plant through the accessors the milestone layer already had —
`maxThrottle`, `condensateStages`, `steamBufferSlugs`, `redlineRpm`, `rpmGainMilli`,
`rotorWearDivisor`, `derrickTantalumEvery`, `moduleCycleTicks`, `annexPellets` — which now take
the node mask as well as the counters. Not one call site moved. Three (`OUTPUT`, `HEAT_SINK`,
`DECAY_WINDOW`) got accessors of their own.

**Internal coherence worth naming.** The `THROTTLE` nodes are the one tree effect that raises
steam, and therefore feedwater. The same tree's `CONDENSATE` nodes cut the feedwater draw by two
more right-shifts — the water branch pays for the fuel branch. That is why throttle was
touchable by the tree after the milestone layer's (correct) refusal to put multipliers on steam.

---

## 6. Where it all lands in the plant's arithmetic

There is **one** entry point. `euDemand()` is the only place the two progression terms are
applied, and `euDemandOf` is the only place the arithmetic is written:

```java
public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli,
                              int progMilli, int upgradeMilli) {
    return afterSpine * rpmMilliValue
        / 500_000L
        * rotorMilli
        / 1000L
        * Math.max(1000, progMilli)
        / 1000L
        * Math.max(1000, upgradeMilli)
        / 1000L;
}

public long euDemand() {
    return euDemandOf(spineBoostedSteam(true), rpmMilli, avgRotorFactorMilli,
                      progOutputMilli(), progUpgradeMilli());
}
```

The pre-existing 3-argument and 4-argument overloads delegate with the missing terms at `1000`,
so every older caller and readout is untouched. At `1000` the factor is `x * 1000 / 1000`, which
is exactly `x` — **a plant with zero progress and an unbought tree derives a bit-identical
number to the one it derived in 0.21.0.** That is the regression test for the release.

The milestone term and the tree term are kept as **separate factors**, in the order the panel
prints them, rather than pre-multiplied into one. Two printed factors are two performed
factors, down to the integer division between them.

### What the MATH panel shows

`mathTurbineLine` prints each progression factor **only when it is above ×1.000**, so a plant
with no progress sees the exact line it saw before this system existed:

```
3) x RPM 12,000/500 (x24.00) x rotors x1.00 x milestones x1.700 x upgrades x1.450 = ... EU/t demand
```

`rpmLine` and `mathLimiterLine` take the **live redline** as a parameter instead of quoting the
`RPM_REDLINE` constant, so an overspeed-trimmed plant is not painted as permanently redlined;
`rpmLine` also takes the **unlocked** ceiling and names it when the operator has not claimed it.
`mathSteamLine`'s full-throttle short form is `throttleN == 24` (it was `>= 24`, which would
have printed "(rods 24/24)" over a 30/24 overdrive).

`mathLimiterLine`'s control-rod term takes the plant's **cap** as a parameter and scores against
it (F5/F7, 0.22.1). It used to be `min(24, N) × 1000 / 24`, which saturates at 100 % while the
plant legitimately runs 26..36/24 — `checkProcessing` clamps the setting to `maxThrottle()` and
`getThrottleN()` hands back the raw N — so the weakest-term line called a 36/24 plant's rods
"perfect" while `mathSteamLine` one row above printed `rods 36/24`. The 24 **denominator** the
player reads is unchanged; only the ratio this one line scores moves, and at an un-upgraded cap
of 24 it is bit-identical to what it printed before. The GUI derives the cap client-side from the
synced counters and node mask through `BTSProgression.maxThrottle`, the same static body the
server calls.

The whole MILESTONES/UPGRADES block is emitted by one private `appendProgression(List)` called
from **both** exits of `statusReport()` (F6, 0.22.1). It used to sit below the `!isSiloBuilt()`
early return, so a platform with no silo showed progress in the controller GUI — whose rows are
ungated — and nothing on either control block or the scanner, which is exactly the state
REPROCESSING accrues in.

### The stacked maximum

Milestone tier 7 on all four lines **and** the full tree:

| Limit | Stock | Milestones T7 | + full tree |
|---|---|---|---|
| Output term | ×1.000 | ×1.700 | ×1.700 × ×1.450 |
| Control-rod cap | 24/24 | 30/24 | **36/24** |
| Redline (ceiling; the RPM LIMIT box sets what is used) | 12,000 | 15,000 | **19,500** |
| Cycle RPM gain | ×1.000 | ×1.25 | **×1.75** |
| Rotor wear divisor | 600 | 1,200 | **2,100** |
| Derrick cycle | 200 t | 160 t | **106 t** |
| Annex cycle | 600 t | 480 t | **320 t** |
| Annex batch | 20 | 24 | **36** |
| Derrick tantalum | every 4th | every 3rd | **every 2nd** |
| Steam buffer | 100 t | 160 t | **240 t** |
| Condensate stages | 0 | 2 | **4** |

The redline stays **reachable**: at ×1.75 gain the equilibrium is ≈20,987 RPM, above the 19,500
ceiling. Top-end demand is roughly 6.6 B EU/t, which is above `Integer.MAX_VALUE`, so
`outputCeiling()` clamps and `mathClampLine` says so. **It cannot detonate**: `onRunningTick`
still clamps the injection to `outputLimit()`, which is the only condition
`addEnergyOutputMultipleDynamos` explodes over. The plant is genuinely clamp-limited at the top
of the tree, and the panel reports it rather than hiding it.

---

## 7. Nothing that already stands is gated

**ADDITIVE ONLY.** Every unlock and every channel takes the plant's existing constant as its
*base argument* and hands back a new maximum derived from it. At tier 0 and mask 0 each returns
its argument unchanged.

Build Tier 5, naquadah-bonded rods ×9, Naquadah Propellant Lv 6, laser output, throttle 24, four
spine housings, eight rotor stages, two Cooling Towers and the 12,000 redline are all base
arguments this system returns untouched. **Nothing needed grandfathering, so nothing can be got
wrong** — there is no "was-already-X" flag in NBT to drift, which is precisely the shape that
armed `btsLastBuiltTier`.

**No structure shape is gated on progression at all.** The new maxima are enforced in *setters*,
never in the shape registry, so `construct(hintsOnly = true)` — which runs client-side, where
the server-written counters read 0 — never consults progression.

One consequence worth naming: the rod nodes are refundable, which makes the control-rod cap the
first limit in this plant that can go **down**. Every consumer already re-clamped, so nothing
could ever run over-throttle, but `getThrottleSetting()` now returns
`Math.min(maxThrottle(), Math.max(1, throttleSetting))` so the GUI is never shown a number the
plant would refuse.

---

## 8. Persistence — the highest-risk part, and how it is closed

The ruling chose per-controller storage with the trade-off stated out loud: a bug here loses the
campaign. So there is exactly **one serializer**, called from **both writers**, read by exactly
**one reader**. They physically cannot drift, because there is only one of each.

```java
saveNBTData(nbt)  ->  progression.writeTo(nbt, true);    // world reload, chunk unload/reload
setItemNBT(nbt)   ->  progression.writeTo(nbt, false);   // WRENCH
loadNBTData(nbt)  ->  progression.readFrom(nbt);         // BOTH paths land here
```

### Keys

| Key | Type | Meaning |
|---|---|---|
| `btsProgVersion` | int | schema version (`BTSProgression.SCHEMA`) |
| `btsProgUpTicks` | long | UPTIME |
| `btsProgBurnup` | long | BURNUP |
| `btsProgReproc` | long | REPROCESSING |
| `btsProgPerfect` | long | TURBINE |
| `btsProgNodes` | compound of booleans | purchased nodes, keyed by `BTSUpgrade.name()` |

Three **operator settings** ride the same two writers, because they are what the record's
ceilings are consumed through (F9, 0.22.1):

| Key | Type | Default | On the wrench |
|---|---|---|---|
| `btsThrottleSetting` | int | 24 | written when ≠ 24 |
| `btsRpmLimit` | int | 12,000 | written when > 12,000 |
| `btsBuildTierSetting` | int | 1 | written when > 1 |

`btsThrottleSetting` was written by `saveNBTData` and **not** by `setItemNBT`, so `loadNBTData`
read 0 off the item tag and forced 24 — a wrenched plant that had unlocked 30/24 or 36/24 lost
20–33 % of its output until somebody noticed and re-typed the number. Each is written only when
it differs from its default, for the same reason `writeTo(force = false)` writes nothing for an
empty record: `getDrops()` attaches a tag `if (!tNBT.hasNoTags())`, so an unconditional write
would give every wrenched controller a tag and stop untouched ones stacking. None of the three
*arms* anything — M2's distinction — and all three are clamped on load, Build Tier now at both
ends because it arrives from item NBT too.

**Names, never ordinals.** The Godforge writes `"upgrade" + ordinal`, which silently reassigns
every unlock the day a node is inserted mid-enum. A name this build does not know is simply not
read — it cannot land on the wrong node. The bitmask is a **transport and arithmetic format
only** (one jar per session); the disk format is names.

**Nothing derived is stored.** Tiers, multipliers, unlocks, points earned and points spent are
pure functions of the four counters and the mask. A stored tier would be a second source of
truth, and the two would eventually disagree.

### Why the wrench path works — traced, not assumed

`BaseMetaTileEntity.getDrops()` calls `mMetaTileEntity.setItemNBT(tNBT)` and then attaches the
tag only `if (!tNBT.hasNoTags())`. `ItemMachines.placeBlockAt` calls
`setInitialValuesAsNBT(isServerSide() ? aStack.getTagCompound() : null, tDamage)` — **server
only**, the client gets `null` — which reaches `loadMetaTileNBT` →
`CommonBaseMetaTileEntity` → `getMetaTileEntity().loadNBTData(aNBT)`. **Item NBT lands in the
same `loadNBTData` as tile NBT**, so one reader covers both.

`force = false` is load-bearing on that path: an empty record writes **nothing**, `hasNoTags()`
stays true, and a controller that has never earned anything still drops as a plain **stackable**
machine item. The moment there is any progress the whole block goes on the item, schema int and
all. `isEmpty()` folds the node mask in, so a plant that bought the free root node with zero
counters is *not* empty and its tree does travel on the wrench.

### The three deliberate details

1. **`readFrom` runs FIRST in `loadNBTData`,** before the `rpmMilli` clamp further down. That
   clamp is taken against `redlineMilli()`, which is derived from the TURBINE line, so reading
   the record *after* it would knock an overspeed-trimmed plant back to 12,000 RPM on every
   world load.
2. **Load-time pruning enforces prerequisites only** — never the budget and never the gates.
   Those are *purchase-time* rules, and enforcing them on load is how a system deletes a
   campaign: a debug counter reset would silently un-buy legitimately purchased nodes. Instead
   `pointsAvailable()` goes negative and says so, purchases are refused, and refunds keep
   working so the player can settle up.
3. **`resetPieceState()` touches none of it,** and says so in its comment for both the counters
   and the node set. A node is a point already spent — breaking one casing must un-form the
   plant, not un-buy it.

### The five survival cases

| Case | How to test |
|---|---|
| World reload | `/btsmilestone set burnup 50000`, quit to menu, reload, reopen |
| Chunk unload/reload | fly 300+ blocks away and back |
| **Wrench** | wrench it, **inspect the dropped item's NBT for `btsProgNodes`**, re-place, reopen |
| Un-form / re-form | break one Solid Steel casing, replace it, reopen |
| **Explosion** | spawn a creeper against the controller and let it blow |

**The explosion case (F3, 0.22.1).** `Explosion.doExplosionB` calls
`dropBlockAsItemWithChance(world, x, y, z, meta, 1.0F / 3.0F, 0)`, so two times in three an
exploded controller yields **no item at all** — and with it every counter and every node, with
nothing to pick up and no way back. The least machinery that closes it keeps the block out of the
explosion: `MetaTileEntity.getExplosionResistance(ForgeDirection)` is overridden to **2,000**.
Traced, not assumed — Forge's `Entity.func_145772_a` patch routes the exploder's query to
`Block.getExplosionResistance(Entity, World, x, y, z, ex, ey, ez)`, GT's `BlockMachines`
overrides that and delegates it straight to `IMetaTileEntity.getExplosionResistance(UNKNOWN)`,
and `doExplosionA` subtracts `(r + 0.3F) * 0.3F` from a ray that starts at most at `size * 1.3`,
so 2,000 survives any blast up to size ≈461 (creeper 3, charged creeper 6, TNT 4). A world-level
backup was the alternative and is deliberately not taken: it would be a second copy of the record,
a second thing to get wrong, and a quiet contradiction of the per-controller ruling. The plant's
own meltdown is unaffected — it detonates above the dome, never at the front-wall controller, and
disables through `stopMachine` rather than by destroying the block. Every other block in the
multiblock keeps GT's default 10.0F, so a blast still opens a hole in the casings and un-forms the
plant, which is the recoverable case one row up.

The wrench case is the one that fails *silently* — if `setItemNBT` wrote nothing, `getDrops()`
leaves the stack tag-less and the loss is invisible until re-placement. Verify on the **item**,
not just on the replaced block. `verifyRoundTrip()` (behind `/btsmilestone verify` and
`/btstree verify`) serializes through **both** writers into fresh records and diffs every
counter and every node, so a forgotten key surfaces the instant it is forgotten, on both paths,
without wrenching anything.

`/btsmilestone verify` also runs `MTEReactorCoreMk1.verifyItemSettings()` (F9, 0.22.1), which
writes a probe tag through the **real** `setItemNBT` and asserts the writer's actual promise:
*a setting that is off its default is on the item, with the same value.* `verifyRoundTrip`
would have caught F9 the day it happened had the throttle been part of the record — it is not,
it is a plain field, and a plain field written by `saveNBTData` and forgotten by `setItemNBT`
is precisely the shape of the bug. Settings sitting **at** their default are deliberately absent
from the item and are not asserted: their absence is the correct answer, and it is what keeps an
untouched controller stackable.

---

## 9. Reading it in game

Every readout comes out of the **same static formatters** in `BTSMilestone` and `BTSUpgrade`, so
the controller GUI, both control blocks, the scanner and the chat commands cannot describe four
different plants. The formatters take primitives, never fields, which is why the client can
render them off the synced values.

A **MILESTONES** group closes the status report: four line rows, a roll-up, the unlock list, then
two upgrade-tree lines.

```
Milestones 12/28 · plant output x1.310 · 24/112 progression points earned
Upgrades 9/25 · 21/112 points committed · 3 available
Tree effects: plant output x1.10 · control-rod cap +2
Unlocks: none yet - the first arrives at tier 5 on any line
```

It is **ungated on the silo**, exactly like the tower and module lines — the REPROCESSING line
accumulates on a bare platform's Derrick as readily as on a finished silo, and the platform
panel is the only one that exists while a player is still building.

The tree itself opens on a **panel of its own** off an 18×18 button in the controller GUI's gap
row, so the controller's own layout is undisturbed: 25 absolutely positioned buttons in a vertical
scroll region, with connector lines derived from the prerequisite edges and lit when both ends
are owned. It is **300×232, centred on the screen** (F8, 0.22.1). It used to be 300 square and
anchored `relative(parent).leftRel(1).topRel(0)`, which at 1920×1080 on GUI Scale Auto - scale
factor 4, a 480×270 viewport - put its left edge on GT's panel's right edge at x = 339 and ran it
to 639, throwing 159 of its 300 columns off the right of the screen and 63 of its rows off the
bottom. 1.7.10's `ScaledResolution` can only stop incrementing once a further step would take the
viewport below 320×240, so a panel that fits inside 320×240 and is centred on the screen is
reachable at every resolution and every GUI Scale - and that needs no client-side screen
measurement in a builder that also runs on a dedicated server. There is **no detail sub-panel** — title, effect, cost, prerequisites, the gate with
its line's live tier, and *which single requirement is failing* live in an auto-updating
per-node tooltip. One hover instead of two clicks.

### Sync

| Handle | Type | Direction |
|---|---|---|
| the four counters | `LongSyncValue` ×4 | S2C |
| `treeNodes` | `LongSyncValue` | S2C |
| `btsTreeBuy` / `btsTreeRefund` | server-synced action, 1 byte | C2S, **server-only body** |

Twenty-five nodes cost **one handle**, not twenty-five: points, affordability, prerequisites,
gates and all twelve channels are pure functions of that long plus the four counters, and the
client runs the *same static bodies* the server validates with.

---

## 10. The debug commands

Progress starts at zero by ruling — there is no back-fill for a campaign already run — so these
are how a tier is reached in minutes for testing instead of in weeks. Both are permission level
2 and both resolve through `BTSCommands.nearestStation(player)`.

```
/btsmilestone                      the four lines, their tiers and what they are paying
/btsmilestone set <line> <n>       put a raw counter value on one line
/btsmilestone tier <line> <0-7>    put one line exactly on a tier's threshold
/btsmilestone all <0-7>            put ALL FOUR lines on that tier
/btsmilestone reset                the four COUNTERS back to zero - THE TREE IS UNTOUCHED
/btsmilestone reset tree           counters AND every purchased node: the whole campaign
/btsmilestone verify               NBT round-trip self-test, tile path AND wrench path
```

**`reset` no longer takes the tree with it (F1, 0.22.1).** It used to call
`BTSProgression.clear()`, which zeroes the 25-node purchase mask as well as the four counters,
while its message, its usage string and its javadoc all spoke only of "the four lines" — so a
player resetting to re-test pacing lost every node they had bought and was never told. `clear()`
is split into `clearCounters()` and `clear()`; bare `reset` takes only the ladders and **names**
the nodes it left standing and the resulting negative point balance, and the destructive form has
to be typed out. (Points reading negative is the documented honest answer: refunding nodes on the
player's behalf would be this system deleting a campaign.)

**`set <line> <n>` refuses text that is not a number (F2, 0.22.1).** `parseCounter` caught
`NumberFormatException` and returned `0L`, which the caller then wrote, marked dirty and printed
back in **green** — so `set turbine 600k`, or a capital O in `60O000`, silently wiped a tier-7
line. It returns `null` now and the command changes nothing. `tier` and `all` on the same command
always validated, through `CommandBase.parseInt`.

```
/btstree                           every node, its state, its cost and its effect
/btstree buy <node>                buy one, through the SAME server-side rules the GUI click uses
/btstree refund <node>             refund one, same rules
/btstree all                       debug: every node, ignoring points and gates
/btstree reset                     debug: refund everything, counters untouched
/btstree audit                     the cost sum self-check (must be 112) and the live channels
/btstree verify                    NBT round-trip self-test, tile path AND wrench path
```

Both have tab completion on verbs and on line / node names, and `set` and `tier` accept a line
by prefix or by what it measures (`fuel`, `ticks`, `cycles`, `modules`).

**Both reports end by reading the limits back off the plant** — `maxThrottle()`,
`getRedlineRpm()` against `getUnlockedRedlineRpm()`, `progOutputMilli()`, `progUpgradeMilli()` —
rather than off the tables. If an
unlock or a channel ever failed to reach the code that consumes it, that is the line that says
so. `/btstree audit` additionally re-sums the 25 costs against 112 and prints every channel
against its base.

A commanded write marks the chunk dirty. GT tiles ride their chunk's save, so without that
"set it, quit, come back" would read the pre-command value and the persistence test would prove
nothing.

---

## 11. The four-axis check

1. **CLIENT vs SERVER.** All five stored values are server-written. Tier, multiplier, unlock,
   points and every channel are **pure static functions** of the synced longs, run identically
   on both sides, so the GUI never reads the client MTE's zeroed record. Purchase bodies are
   server-only. **No structure shape is gated**, so `construct(hintsOnly = true)` never consults
   progression; the new maxima live in setters.
2. **TILE NBT vs ITEM NBT.** §8. One serializer, two writers, one reader. `super.setItemNBT` is
   kept and no tile state was added to the item — the record joins the module batch counters
   that were already there, and `btsReactorTier` / `btsColumnRods` / `btsSteamPerTick` stay off
   the item.
3. **PLANT-WIDE HATCH LISTS.** Nothing here registers or reads a hatch. `CONDENSATE` is a shift
   on `flowPerTick`, **not** a third Cooling Tower: `coolingTowerCount()` and its two named
   booleans are untouched.
4. **`IStructureElementChain`.** No fallback chain was added or altered.
