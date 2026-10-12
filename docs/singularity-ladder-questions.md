# Singularity Ladder: questions for you

Consolidated 2026-10-12, after the P1-P3 build (Foundation, Kerr T1/T2, Eye T1-T4) loaded cleanly in the test
instance: 255 recipes registered, 0 skipped, no errors. The build units logged 38 questions (Q-1..Q-38) as they
went. This file merges the duplicates, drops what you have already answered, and sorts what is left by how much it
changes play.

Each item says what the problem is, what is in the jar now, what we suggest, and the other options. Nothing here
blocks the build. The old Q-numbers are kept in brackets, because the code and the build spec still use them in
comments (`TODO(Q-20)` and so on). The table at the end shows where each old number went.

The spec these refer to is [`singularity-ladder-build.md`](singularity-ladder-build.md). Its top section, "What's in
the build", explains how to get the machines in creative.

---

## Part A. You have answered, but the jar does not match yet

### A-1 The Kerr Extractor Module still runs its own "melting" recipes [Q-9, Q-22]

**Your ruling.** The Extractor Module runs GT's normal extractor recipes only: no fluid extraction and no custom map.
The Kerr is a module host, so adding a module later must stay cheap.

**What the jar does.** The module runs a new NEI page, "Ergosphere Extraction". It melts Neutronium, Infinity,
SpaceTime, WDM, BDM and similar ingots, plates and blocks back to fluid at 144 L per ingot. That page was built before
your ruling.

**Next step.** A small code change: point EXTRACT mode at GT's extractor map and delete the melt recipes and their
NEI page. The module system already works as "new arc + its own private hatches + a recipe map", so it stays
extensible. No question for you; this is a to-do.

### A-2 Upgrades still install instantly [Q-10, Q-24, Q-38]

Your ruling is milestones plus an install run (see B-1 for the numbers). Today a plain right-click with the Kerr
Density Upgrade or the Galactic Upgrade Core installs it at once. Both machines route the install through one method
each, so the mechanic can drop in without rework. It is blocked only on the numbers in B-1.

### A-3 Acceptance checks not done yet

The build compiles, loads and registers every recipe. These checks from your acceptance list are still open:

- **Cost table.** Per-block recipe × block count → total raw materials per machine tier. Not written yet. The block
  counts are in the spec (§5.1). The structures need thousands of each casing (for example 2,852 mirror casings
  on a Kerr T1), so this table is the real fairness check.
- **NEI multiblock preview.** The controllers implement the hologram interfaces, but nobody has opened the NEI
  "Multiblock" tab for them yet, or checked whether it caps the size of what it renders.
- **In-game build test.** The smoke test only loaded the game. Nobody has run `/btskerr` or `/btseye` yet, so the
  structure orientation, the hologram and survival auto-build from AE are unverified. If a build reports
  NOT FORMED with the first wrong block on the mirrored side, the structure files need re-exporting flipped
  (spec, "Structure export").

---

## Part B. Needs your decision (most important first)

### B-1 Upgrade install: the milestone and install-run numbers [Q-10, Q-24, Q-38]

**Context.** You ruled "milestones + install run" for the Kerr Density Upgrade (Kerr T1 → T2) and the Galactic
Upgrade Core (Eye T3 → T4). The upgrade is consumed and permanent, and an install survives a wrench.

**Proposal** (all numbers to tune):

| | Kerr Density Upgrade | Galactic Upgrade Core |
|---|---|---|
| Milestone to unlock | Hold the hole at 64 SM (the T1 maximum) for 30 minutes in total | Finish 4 Arcturus (T3 star) runs on this Eye, about 5 h 20 min |
| Install run | 10 minutes, machine offline | 60 minutes, machine offline |
| Install cost | The upgrade + 9,216 L molten SpaceTime + full hatch power for the run | The core + 50,000 L Raw Hyperdense Stellar Plasma (one Arcturus run's output) + one Arcturus run's EU (about 1.9 × 10¹⁴) from wireless |
| Progress shown | GUI, tooltip and scanner | GUI, tooltip and scanner |

Start the install with a plain right-click holding the upgrade. A sneak-right-click with an item in hand never
reaches the block in 1.7.10, so it cannot be the gesture.

**Also needs a ruling.** If someone tears down the Kerr's T2 reinforcement after the upgrade, the Kerr currently
refuses to form at all.

- Suggestion: keep that ("T2 is a commitment").
- Alternative: it falls back to running as T1 until the reinforcement is rebuilt.

**Alternatives.** Make the milestone count running time instead of mass held or runs finished. Or skip the
milestone and keep only the install run.

### B-2 Turning off TecTech's Eye leaves Universium with no source until P5 [Q-3, Q-14, Q-33]

**Context.** TecTech's Eye of Harmony was the pack's only source of White Dwarf Matter, Black Dwarf Matter, Universium
and Raw Star Matter. You ruled that the removal ships ON and is a config option. That is now real:
`replaceTecTechEyeOfHarmony=true` in `config/beyondthestars.cfg`, and the boot log confirms 43 TecTech recipes and its
controller recipe removed.

**What happens to each matter:**

- **Raw Star Matter:** our Eye T1 (Sol) makes it.
- **White Dwarf Matter:** our Eye T4 (Sirius B) makes it.
- **Black Dwarf Matter:** an interim T4 recipe ("Sirius B, Cooled") makes it until Eye T5 ships in P4. It sits behind
  its own flag, `interimBlackDwarfAtEyeT4=true`.
- **Universium:** no source until Eye T10 (P5). Universium feeds UXV components, EIC nanites and late Godforge
  upgrades, so anyone who reaches those is stuck.

**Also built.** TecTech's Eye controller recipe goes too, so nobody crafts a machine that cannot run. Placed ones
stay as decoration. The removal runs only after our Eye has copied TecTech's planets. If that copy fails, TecTech's
Eye is left fully working and an error is logged.

**Suggestion.** Keep the interim Black Dwarf recipe until P4. Ship the planned recipe swaps (UXV parts without
Universium, D-45; the Eternity loop on Ergomatter, D-48) no later than P4.

**Alternatives.**

- Keep TecTech's rocket-tier 6-9 planets as a small Universium trickle.
- Accept the Universium stall for now.

### B-3 What Time Dilation and Stabilisation do, and the tier-gating block [Q-16, Q-20, Q-29]

**Context.** Following your rulings:

- The Spacetime Compression Field Generators hard-gate the Eye's tier 1:1. Eye T4 needs compression tier 4 in every
  slot. The table is in one place in the code.
- Time Dilation and Stabilisation become separate upgrades, but their effect is not designed yet.
- You also want "a block that gates tiers"; that design session is still pending.

**What we built.**

- **Where the generators sit:** compression on the six upper struts (188 blocks), dilation on the six lower struts
  (138) and stabilisation under the six pylon feet (294). All three are in the T1 core, as in TecTech's Eye.
- **Dilation and stabilisation** accept any tier and do nothing yet (marked TODO).
- **Mixed tiers** are allowed in one structure, and the lowest one counts. Upgrading 188 generators one by one never
  un-forms the Eye.

**Need from you.**

1. What dilation and stabilisation do. Our suggestion is TecTech's meaning, which players already know:
   - dilation = speed;
   - stabilisation = planet success chance.
2. Whether the dedicated "tier gate" block replaces the compression gate or adds to it.
3. What the T10 gate part is. TecTech's generators stop at tier 9, so T10 needs a new part in P5.

**Alternatives for mixing.** One uniform tier per type, as TecTech does: stricter, and a swap un-forms the Eye.

### B-4 Should planets still need TecTech's compression tier on top of the 1:1 gate? [Q-29]

**Context.** TecTech's planets each need a minimum compression tier (R7-R8 planets need tier 7-8). We kept that rule
on top of your 1:1 gate. So a T3 Eye with exactly tier-3 compression cannot run R7-R8 planets, even though they are
"T3 planets". The player has to over-build compression to reach them.

**Suggestion.** Drop TecTech's per-planet rule and let the 1:1 gate decide alone. It is simpler, and it is what the
tier names promise.

**Alternative.** Keep both rules, as built. Higher compression tiers then stay useful inside a tier.

### B-5 Eye balance: outputs, run times, power, success [Q-5, Q-6, Q-31]

**What we built** (every number is a proposal):

| Tier | Star | In → out per run | Time |
|---|---|---|---|
| T1 | Sol | 1e9 L hydrogen + 1e9 L helium → 1,000,000 L Raw Star Matter | 20 min |
| T2 | Sirius A | 1,000,000 L Raw Star Matter + 1e9 L helium → 100,000 L Excited Stellar Plasma | 40 min |
| T3 | Arcturus | 100,000 L Excited Stellar Plasma + 2e9 L hydrogen → 50,000 L Raw Hyperdense Stellar Plasma | 80 min |
| (Kerr T2) | densify | 10,000 L Raw Hyperdense Stellar Plasma → 1,000 L Hyperdense Stellar Mixture | 20 s |
| T4 | Sirius B | 5,000 L Hyperdense Stellar Mixture + 4e9 L helium → 18,432 L White Dwarf Matter | 120 min |

The White Dwarf figure matches TecTech's best per-run amount, at about 5× its speed.

**How a run works:**

- **Power:** wireless EU only, paid as one lump when the run starts. It costs UMV-recipe power ×1 / ×4 / ×16 / ×64
  for T1-T4; a Sol run is about 3 × 10¹² EU. No EU comes back (you ruled it a pure sink).
- **Parallel:** a higher-tier Eye runs lower recipes in parallel, ×2 per tier of difference. A T4 Eye runs Sol ×8.
- **Success:** stars always succeed. Planets keep TecTech's 1 − 5 % per rocket tier, rolled per parallel. A failed
  parallel eats its inputs.
- **Planets:** outputs go to an ME output bus and hatch only, as with TecTech. A run takes 20 min × 1.25 per rocket
  tier; the Deep Dark takes 4 h.

**Suggestion.** Play-test, then tune one knob per tier.

**Alternatives.**

- Raise the White Dwarf rate so it is not scarce before P4.
- Roll a planet run once, all or nothing, instead of per parallel.
- Laser or energy hatches instead of wireless.

### B-6 Eye T2 and T3 unlock by building the band; band names [Q-2, Q-11, Q-13]

**Context.** The design says each band is "unlocked by an upgrade core", but cores exist only for T4, T7 and T10.
Eye T2 is also optional by design (D-42). You accepted Eye T2 as the interim Excited Stellar Plasma source while the
Homogeny is on hold, so T2 is the de-facto path to T3 for now.

**What we built.**

- T2 recipes need the beam band. T3 recipes need the jet crescent, which works without the beam band. T4 needs the jet
  crescent, the spiral arms and the Galactic Upgrade Core.
- The bands cost only their casings. Each band only adds blocks, so nothing is torn down when the Homogeny arrives.
- Star picks: Sirius A (T2), Arcturus (T3) and Sirius B (T4, Sirius A's white-dwarf companion). The T3 product is
  named "Raw Hyperdense Stellar Plasma", the card's name.

**Need from you.** The tier/band names. The code says Planetary/Galactic; mine.md proposed Stellar · Systems ·
Galactic · Universal.

**Alternative.** A "Stellar Upgrade Core" for T2 and T3 as well.

### B-7 Which planets map to which Eye tier, and what they lose [Q-12]

**What we built.**

- Our Eye copies TecTech's 43 planet recipes, keeping their ores and plasmas.
- It drops TecTech's White/Black Dwarf Matter, Universium and Raw Star Matter outputs. Sol is the only Raw Star Matter
  source, which gives T1 a reason to exist.
- Rocket tiers map as R0-R3 → T1, R4-R6 → T2 and R7-R8 → T3. The Deep Dark → T4.

**Suggestion.** As built.

**Alternative.** Let planets keep a small Raw Star Matter by-product.

### B-8 Kerr balance: the hole's numbers [Q-8, Q-25, Q-26]

**Context.** You ruled the Kerr keeps its mass mechanic.

**What we built** (every number is a proposal):

- **The hole:** loses mass over time. A governor keeps it at the setpoint by firing SpaceTime bolts from the bolt
  buses. The setpoint is 2-64 SM at T1 and up to 512 SM at T2, default 8.
- **Mass trade-off:** parallel grows with mass and voltage tier (8 × tier × √mass). Speed is ×4 for a small hole
  and falls to ×1 as the hole grows. The spec's formula made big holes *slower* than ×1, so we clamped it at ×1.
  Throughput peaks around 16 SM, and a 512 SM hole is about ×22.
- **Closed hole:** only plain compressor recipes run. Heated and black-hole compressor recipes need the hole open.
  This is a little kinder than the Black Hole Compressor, which does nothing without a hole.
- **Injector rails:** can be mixed Mk I/II/III, and the lowest tier sets the bolt cap (4/8/16 per tick). Upgrading
  232 rails one by one never un-forms the Kerr.
- **Broken structure:** the hole freezes; it neither drains nor evaporates.
- **Bolt buses:** must be plain input buses, not ME stocking buses.

**Suggestion.** As built; tune in play.

**Alternatives.**

- No speed clamp, so a big hole is purely a parallel play.
- The hole keeps draining while the structure is broken.
- One rail tier for the whole structure.

### B-9 Eye fluid inputs: any input hatch takes any Eye fluid [Q-30]

**What we built.** The Eye pulls every fluid it uses from every input hatch in its sockets, every second, into one
big internal tank per fluid. These tanks are saved, and they survive a wrench. A T1-only Eye can therefore take Raw
Star Matter through its plinth hatches, and ME stocking hatches work.

**Suggestion.** As built: less fiddly.

**Alternative.** The spec's version: gas hatches on the plinth take only hydrogen and helium, and band hatches take
only stellar fluids.

### B-10 The Eye's centre render is not built [Q-34]

The centre of the ring is empty for now. The `eyeRenderCentre` config option exists but does nothing yet.

**Suggestion.** Build it after the in-game test, reusing TecTech's orbiting-planet render scaled to our 80-block ring.

**Alternative.** Leave the Eye without a render.

### B-11 Where the Eye's bands meet, the earlier piece keeps the overlapping blocks [Q-19]

**Context.** In the mockup, three Eye band joins overlap, and the Kerr's module arcs overlap the frame ring.

**What we built.**

- **Eye:** the earlier piece keeps those blocks, and the later band starts where it ends. The spiral-arm roots butt
  against the core band instead of sinking into it.
- **Kerr:** the module arc owns its overlap. Without the module, the frame ring has a 49-block gap there.

**Suggestion.** As built; the difference is a few blocks at each join.

**Alternative.** Adjust the mockup so nothing overlaps.

### B-12 The Kerr's inner lining reads darker than the mockup [Q-16]

**Context.** You ruled to reuse the Black Hole Compressor's blocks. The mockup's lining is bright white, and the BHC
has no white casing, so we used its Background Radiation Absorbent Casing. The Eye's white-ish casing (Infinite
SpaceTime Energy Boundary) is post-UMV, too late for the UIV Kerr.

**Suggestion.** Accept the darker look.

**Alternative.** One extra new "Harmonic Lining" block just for the Kerr.

### B-13 Textures are generated from code [Q-15]

**What we built.**

- All 33 new textures (16 casings, 8 controller overlays, 9 items) come from a committed script, in the Black Hole
  Compressor and TecTech Eye colour families.
- The three new fluids use GT's standard molten texture, tinted: Excited Stellar Plasma gold, Raw Hyperdense Stellar
  Plasma orange-red, Hyperdense Stellar Mixture crimson.

**Suggestion.** Hand-polish only what players stare at: the two controller faces and the mirror and bastion walls.

**Alternative.** Hand-drawn textures; the file names stay the same.

### B-14 Smaller as-built choices: say if you disagree

- **Raw Stellar Plasma Mixture is the pack's existing Raw Star Matter [Q-1].** The pack already names it "Condensed Raw
  Stellar Plasma Mixture". One fluid, not two, so the Godforge keeps its fuel.
- **The Kerr's utility-hatch sockets [Q-21, Q-27].** The eight spots beside the bolt buses optionally take a Black
  Hole Compressor Utility Hatch. It gives a redstone signal while the hole is open.
- **Each Kerr module has its own hatches [Q-23].** The compressor never eats the extractor's inputs, and recipes never
  touch the bolt buses.
- **The Kerr re-checks its structure on a timer [Q-37].** It re-checks every 10 s idle or every 50 s running, and
  never while part of it is unloaded. Without this, a module built far from the controller would not register
  until a relog.
- **Assembler recipes have one fluid slot [Q-17].** Four casing cards had two fluids, so one of them became
  foil or ingots.
- **Dense SpaceTime plates [Q-18, Q-32].** The fallback for a missing dense plate never triggered in the boot log, so
  the pack has them and the recipes use real dense plates. Closed.
- **Every assembly line has its own research item [Q-36].** The boot log confirms all 9 are unique, so every one can
  be put on a data stick.
- **Eye controller recipe [Q-32].** It uses Tier-0 (Crude) field generators, as TecTech's own controller does, not
  Gallifreyan. Gallifreyan would have forced all 24 field-generator research steps before a T1 Eye, and it is
  now the T9 gate.

---

## Part C. Settled (for the record)

| Topic | Your answer | State |
|---|---|---|
| Eye T2 as the interim Excited Stellar Plasma source [Q-2] | Accepted | Built |
| Remove TecTech's Eye recipes [Q-3] | On by default, and a config option | Built (`replaceTecTechEyeOfHarmony`) |
| Eye gives EU back? [Q-4] | No, pure EU sink | Built |
| Stale-card substitutions [Q-7] | Accepted; the review cards get redrawn | Built (spec §2.1) |
| Kerr mass mechanic [Q-8] | Keep | Built |
| Extractor Module [Q-9, Q-22] | GT extractor recipes only | **Not built yet (A-1)** |
| Upgrades [Q-10] | Consumed, permanent, milestones + install run | **Install run not built (A-2, B-1)** |
| Eye name [Q-13] | "Eye of Harmony" at every tier for now; per-tier names later | Built (name set in one place) |
| Compression gate [Q-20] | Hard gate, 1:1 | Built |
| Reuse the EOH and BHC blocks [Q-16] | Yes | Built (spec FC-1) |

## Where the old question numbers went

| Old | Now | Old | Now | Old | Now |
|---|---|---|---|---|---|
| Q-1 | B-14 | Q-14 | B-2 | Q-27 | B-14 |
| Q-2 | B-6, C | Q-15 | B-13 | Q-28 | spec, top section |
| Q-3 | B-2, C | Q-16 | B-3, B-12, C | Q-29 | B-3, B-4 |
| Q-4 | C | Q-17 | B-14 | Q-30 | B-9 |
| Q-5 | B-5 | Q-18 | B-14 | Q-31 | B-5 |
| Q-6 | B-5 | Q-19 | B-11 | Q-32 | B-14 |
| Q-7 | C | Q-20 | B-3, C | Q-33 | B-2 |
| Q-8 | B-8, C | Q-21 | B-14 | Q-34 | B-10 |
| Q-9 | A-1 | Q-22 | A-1 | Q-35 | spec, top section |
| Q-10 | A-2, B-1 | Q-23 | B-14 | Q-36 | B-14 |
| Q-11 | B-6 | Q-24 | B-1 | Q-37 | B-14 |
| Q-12 | B-7 | Q-25 | B-8 | Q-38 | A-2, B-1 |
| Q-13 | B-6, C | Q-26 | B-8 | | |

The full original wording of Q-1..Q-38 is in git history (addon commit 457bbf8).
