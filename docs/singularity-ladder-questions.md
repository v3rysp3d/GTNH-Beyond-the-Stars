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

### A-1 The Kerr Extractor Module still runs its own "melting" recipes [Q-9, Q-22]: DONE

**Your ruling.** The Extractor Module runs GT's normal extractor recipes only: no fluid extraction and no custom map.
The Kerr is a module host, so adding a module later must stay cheap.

**What the jar does.** The module runs a new NEI page, "Ergosphere Extraction". It melts Neutronium, Infinity,
SpaceTime, WDM, BDM and similar ingots, plates and blocks back to fluid at 144 L per ingot. That page was built before
your ruling.

**Next step.** A small code change: point EXTRACT mode at GT's extractor map and delete the melt recipes and their
NEI page. The module system already works as "new arc + its own private hatches + a recipe map", so it stays
extensible. No question for you; this is a to-do.

**Done (follow-up pass, 2026-10-12).** EXTRACT mode runs `RecipeMaps.extractorRecipes` and nothing else. The
"Ergosphere Extraction" map, its NEI page, its lang key and the SL-K06 melt recipes are deleted; the Kerr now shows on
GT's own Extractor page. The extractor's sockets take item buses only (B-17). Each module is now one `KerrModule`
entry that the walk, the hologram, survival build and the status lines loop over; the spec's new section "Adding a
Kerr module" lists the steps for the next one.

### A-2 Upgrades still install instantly [Q-10, Q-24, Q-38]

**D-49 status (unit 6).** Superseded for the Eye: the Galactic Upgrade Core (and the four new cores) go into an escrow by right-click and are consumed only by a successful transition, after the current star's milestones (B-37). The Kerr Density Upgrade belongs to the Kerr redesign branch.

Your ruling is milestones plus an install run (see B-1 for the numbers). Today a plain right-click with the Kerr
Density Upgrade or the Galactic Upgrade Core installs it at once. Both machines route the install through one method
each, so the mechanic can drop in without rework. It is blocked only on the numbers in B-1.

### A-3 Acceptance checks not done yet

The build compiles, loads and registers every recipe. These checks from your acceptance list are still open:

- **Cost table.** Per-block recipe × block count → total raw materials per machine tier. Not written yet. The block
  counts are in the spec (§5.1). The structures need thousands of each casing (for example 2,852 mirror casings
  on a Kerr T1), so this table is the real fairness check.
  **Written (cost audit):** [`singularity-ladder-costs.md`](singularity-ladder-costs.md) has the completeness check,
  per-block costs, per-tier totals and the BHC / TecTech Eye / Godforge / Nano Forge comparison. It found two
  uncraftable chains (Kerr T2 and Eye T2-T4) and an Eye T1 at 8× TecTech's Eye: B-21..B-24, 15 proposed changes.
  **Applied (recipe audit pass):** every block and item in the BOM now has a recipe that can run; costs doc §6 lists
  what was applied and how (B-26, B-28 are the new judgement calls).
- **NEI multiblock preview.** The controllers implement the hologram interfaces, but nobody has opened the NEI
  "Multiblock" tab for them yet, or checked whether it caps the size of what it renders.
  **Checked against the bytecode (acceptance pass):** the preview finds both controllers on its own, counts Kerr 2
  tiers and Eye 4, builds them through our survival auto-place, and has no size cap our structures exceed. The
  hologram paths were reviewed and fixed at the same time. Details: spec, "Hologram & NEI"; open points B-19, B-20.
  Still to do: open both in game.
- **In-game build test.** The smoke test only loaded the game. Nobody has run `/btskerr` or `/btseye` yet, so the
  structure orientation, the hologram and survival auto-build from AE are unverified. If a build reports
  NOT FORMED with the first wrong block on the mirrored side, the structure files need re-exporting flipped
  (spec, "Structure export").
  **Done headless (dev server, console forms of the build commands):** every Kerr and Eye tier forms in all four
  facings, through both the creative and the survival hologram paths. The orientation matches the mockup, so no
  re-export was needed. A re-projection never breaks a formed machine, a survival repair puts back exactly the
  missing blocks, and every build survives a server restart. Results and limits: B-27. Still to do in game: the
  client-side hologram hints, survival build from a real AE system, and the NEI tab.

---

## Part B. Needs your decision (most important first)

### B-1 Upgrade install: the milestone and install-run numbers [Q-10, Q-24, Q-38]

**D-49 status (unit 6).** Superseded for the Eye by D-49's milestones, escrow and transitions (numbers in B-37, `EyeMilestones`). Still open for the Kerr only.

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

**Also needed a ruling (now settled).** If someone tears down the Kerr's T2 reinforcement after the upgrade, the Kerr
refuses to form at all. **You ruled: keep "won't form".** Built, unchanged, and now commented in the code and the
spec.

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

**D-49 status (unit 6).** Answered by the approved D-49 proposal: Time Dilation sets the time dial's cap (1 + 0.25 x (tier - N)) and Stabilisation sets q (1 - 0.05 x (tier - 1)), which scales the field's noise and events. Compression stays the 1:1 gate. Planets keep only the compression time reward.

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

### B-4 Should planets still need TecTech's compression tier on top of the 1:1 gate? [Q-29]: RULED, DONE

**Context.** TecTech's planets each need a minimum compression tier (R7-R8 planets need tier 7-8). We kept that rule
on top of your 1:1 gate. So a T3 Eye with exactly tier-3 compression cannot run R7-R8 planets, even though they are
"T3 planets". The player has to over-build compression to reach them.

**Suggestion.** Drop TecTech's per-planet rule and let the 1:1 gate decide alone. It is simpler, and it is what the
tier names promise.

**Alternative.** Keep both rules, as built. Higher compression tiers then stay useful inside a tier.

**Ruled (round 4): drop it. Done.** The Eye no longer checks a planet's compression minimum; the 1:1 gate alone
decides, so an Eye at tier N runs every planet mapped to tiers 1..N. One judgement call is in B-16 (the -3 % time
reward).

### B-5 Eye balance: outputs, run times, power, success [Q-5, Q-6, Q-31]

**D-49 status (unit 6).** Stars are now per-cycle (60 s) with upkeep, EU per cycle and output x mean Y x r (`StarDef`, plan §6.1); success is no longer rolled for stars. Q-31's batch-run model is replaced. The numbers below apply to planets only.

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

**D-49 status (unit 6).** Bands are cumulative now: T3 needs T2 (my answer 2), and every tier also needs its transition. The band names are unchanged.

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

**D-49 status (unit 6).** Partly reversed: the 8 Field sockets are unbuffered (trims and Time are drained live, ME stocking hatches refused) and the Sensor sockets take only the Stellar Field Sensor Hatch. The gas and stellar sockets still buffer any Eye fluid.

**What we built.** The Eye pulls every fluid it uses from every input hatch in its sockets, every second, into one
big internal tank per fluid. These tanks are saved, and they survive a wrench. A T1-only Eye can therefore take Raw
Star Matter through its plinth hatches, and ME stocking hatches work.

**Suggestion.** As built: less fiddly.

**Alternative.** The spec's version: gas hatches on the plinth take only hydrogen and helium, and band hatches take
only stellar fluids.

### B-10 The Eye's centre render is not built [Q-34]

**D-49 status (unit 6): BUILT.** See B-45. The Eye draws its star, neutron star, cooling dwarf, magnetar and the Core Collapse at its centre, behind `eyeRenderCentre` and a per-Eye GUI switch.

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

### B-12 The Kerr's inner lining reads darker than the mockup [Q-16]: RULED, DONE

**Context.** You ruled to reuse the Black Hole Compressor's blocks. The mockup's lining is bright white, and the BHC
has no white casing, so we used its Background Radiation Absorbent Casing. The Eye's white-ish casing (Infinite
SpaceTime Energy Boundary) is post-UMV, too late for the UIV Kerr.

**Suggestion.** Accept the darker look.

**Alternative.** One extra new "Harmonic Lining" block just for the Kerr.

**Ruled (round 4): one new white Kerr lining block. Done.** Harmonic Lining, `slCasing2:2`, white with the BHC's
rim and lavender shading; every Kerr `J` cell uses it, and the Kerr shapes' manifest was re-exported. The choices made
are in B-15.

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

### B-15 Harmonic Lining: which meta, its recipe, no light [follow-up pass]

**Context.** You ruled one new white Kerr lining block, in a reserved casing meta, with a fair recipe.

**What we built.**

- **Meta:** `slCasing2:2`, the meta the spec reserved for "Harmonic Lining" and left as a hole when the BHC casing took
  the role. The other holes (`slCasing:0` deck, `slCasing:9` eye shell) belong to other roles, so they stay holes.
  The name is the card's, "Harmonic Lining". The Eye keeps TecTech's Infinite Spacetime Energy Boundary Casing as its
  lining; the new block is Kerr-only.
- **Recipe SL-P12** (Assembler, UIV, 30 s): 6 Neutronium plates, 8 Infinity foil, 1 Transcendent Metal plate and
  576 L molten Infinity make 4 linings. Per block that is 1.5 Neutronium, 1.5 Infinity (foil is ¼ ingot) and ¼
  Transcendent Metal.
  - A Kerr T1 with both modules needs 521 (131 in the T1 cage, 253 in the compressor arc, 137 in the extractor
    arc): about 790 Neutronium, 790 Infinity and 130 Transcendent Metal.
  - A Kerr T2 needs 4,552: about 6,800 Neutronium, 6,800 Infinity and 1,140 Transcendent Metal.
  - For scale, the 3,551 Mirror Backing casings cost about 7 Infinity each. The block it replaces, the BHC's
    Background Radiation Absorbent Casing, costs 64,000 L of UU-Matter each, so the old mapping was far dearer.
- **No light.** The spec once said "emissive (light 12)". The texture is bright, and making 4,500 blocks light sources
  would cost lighting updates on every build and hologram, so it does not emit light.

**Suggestion.** As built.

**Alternatives.** Make it glow (light 8-12) for the mockup look. Price it like the mirror casings if the Kerr T2
should cost more.

### B-16 The planet time reward stays, with no penalty below a planet's old tier [follow-up pass]

**Context.** With TecTech's per-planet compression minimum gone (B-4), its other half was the -3 % time per
compression tier above that minimum.

**What we built.** The reward stays: each compression tier above the planet's TecTech tier still cuts 3 % of its
time. Below that tier there is no penalty, because the gate alone decides. Stars never use it. The NEI page now says
"-3%/field > T*n*" where it said "field T*n*+".

**Suggestion.** As built: it keeps a reason to over-build compression without gating anything.

**Alternative.** Drop the reward too, so compression is purely the tier gate.

### B-17 The extractor module takes item buses only; extractor runs and the hole [follow-up pass]

**What we built.**

- GT's extractor map is items in, items out, so the extractor's two sockets accept input and output **buses** only.
  A fluid hatch there faults the module. `/btskerr` and `/btskit kerr` now place and give output buses.
- Extractor recipes need no open hole (they carry no compression tier). They run at the base parallel (8 × voltage
  tier) with the hole closed, and get the hole's parallel and speed bonuses when it is open, like plain compressor
  recipes.
- The Kerr Extractor Module core (SL-K03) and the Ergosphere Extraction Casing (SL-P14) keep their names, which were
  designed around fluid extraction.

**Built (recipe audit pass):** pistons, cost audit #14. SL-K03 takes 32 Electric Pistons UIV in place of the 32
Electric Pumps (fc3eff4), and SL-P14 takes an Electric Piston UIV in place of its pump (6b7346b), so the parts match
an item extractor.

**Suggestion.** As built: keep the names, with pistons.

**Alternative.** Revert both recipes to Electric Pumps.

### B-18 D-49 star lifecycle and stability field: the proposal and its open choices [follow-up pass, design only]

**Context.** D-49 redesigns the Eye: live T1-T3 stars, a T4 neutron star reached by Core Collapse, T5 black-dwarf
cooling, a T6 magnetar on Antipolarization Agent, and a stability field on every tier. Three designs were judged and
merged into one proposal: design repo `designs/docs/star-lifecycle.md`, everything PROPOSED. Nothing is built. The
Eye freeze holds until you accept it.

**In short.**

- Stars run in 60 s cycles at today's per-minute economy.
- The field is a gauge model on every tier: P, then Θ from T3, Ω from T4, and Π plus an agent shield at T6. Zones
  set the yield. Integrity stops one bad second from killing a star.
- A new sensor hatch (MTE 28116) and unbuffered Field sockets make the field automatable with threshold rigs. This
  is simulated.
- Tiers unlock in sequence: milestone, then core, then transition event.
- This supersedes A-2/B-1, answers B-3, and changes B-5, B-6 and B-9 (partly).

**Open choices** (recommendation first; details in §7 of the doc):

1. **T2 optional?** Required. Alternative: a direct Sol → red giant route that takes 5.7× longer.
2. **T5 cooling.** Subspace Cooling (DSSF) plus the general time dial. Alternatives: Urca + subspace, chrono-cooling
   or a phonon drain.
3. **Failure.** Lose the star, in-flight output and event materials. Cores are consumed only on success. No block or
   tier loss.
4. **T3 star.** Keep Arcturus, pushed past 1.44 M☉. Alternative: Betelgeuse.
5. **Time dial.** Tachyon Rich Temporal Fluid buys output per hour at a harder field, capped by the Time Acceleration
   tier above the Eye's tier. Alternative: the dial only at T5.
6. **Trim budgets** (Super Coolant, Purified Tengam, SpaceTime). Accept, and tune after a play test.
7. **Antimatter for the Agent** (about 10 L/s at T6). Check the Antimatter Forge's throughput before P4.
8. **One live star per Eye.** Yes.
9. **T4 HSM feed up to 1.5 L/s** (about 2.16× today's WDM rate). Yes.
10. **A wrench during a transition.** Aborts it: event materials are lost, the core is kept.

### B-19 NEI preview: a large render and no channel sliders [acceptance pass]

**Context.** You asked that both controllers show in NEI's multiblock preview at every tier, and that any size cap be
logged.

**What we found.** BlockRenderer6343 has no block-count or footprint cap. Its only hard limit is the scratch world's
height (y 0-255), and ours fit (Kerr y 32-96, Eye y 63-119). The preview counts **2 tiers on the Kerr and 4 on the
Eye** by building each tier until one adds nothing, then draws each tier through our survival auto-place, hatches
included. Two catches:

- **Frame rate.** The preview redraws every block every frame, with no caching. A T4 Eye (34,502 blocks) or a Kerr
  T2 (29,839) may stutter while the tab is open. The layer slider (one y-layer at a time) stays fast.
- **No field-generator sliders.** TecTech's own Eye gives NEI a slider for each field generator. If ours did the
  same, NEI would also stretch the main tier slider to 1-9 (the generators' tier count), and tiers 5-9 would show
  the T4 Eye again. We kept the slider at 1-4. The generators follow GT's EOH channels on the projector only.

**What we built.** As described; no fallback was needed. The spec section "Hologram & NEI" has the details.

**Suggestion.** Keep it as built, and check the frame rate in game. If the T4 Eye is too slow, the cheapest fallback
is a lighter preview: implement GT's `INEIPreviewModifier` and leave the band interiors (the linings and glass, about
a third of the blocks) out of the NEI build only. The real structure would not change.

**Alternatives.** Expose the generator sliders and accept a 1-9 main slider. Or ask BlockRenderer6343 upstream for a
display-list cache, which helps every large GTNH multi.

### B-20 Hologram build rules: budget, rails, and the compression swap [acceptance pass]

**What we built.**

- **Speed.** Survival auto-place now does up to 200 blocks per round, not StructureLib's default 25. It uses the same
  kind of rule TecTech's Eye uses (×5, capped at 200); ours is ×8, capped at 200. A T4 Eye or a Kerr T2 then takes
  about a minute of holding the projector, not about seven.
- **Tiered parts are never downgraded.** Re-projecting over upgraded parts no longer replaces them with lower tiers,
  in creative or in survival. This covers the Kerr's Mk II/III rails and the Eye's field generators. A creative
  projection used to downgrade Mk III rails to Mk I.
- **Survival takes what you have.** If the player has no Mk I rail or no tier-N generator, survival places the next
  higher tier they do have.
- **Compression swaps are by hand.** Survival auto-place never breaks blocks. To take an Eye from T1 to T2, the
  player must swap the core's 188 compression generators for tier 2 before the projector places the beam band.
- **Projector channels.** GT's `spacetime_compression`, `time_dilation` and `stabilisation` channels raise the
  generator tier a build places. Compression never goes below the stack's 1:1 gate.

**Suggestion.** As built.

**Alternatives.** A bigger round (more server time per projector use). Or let survival skip the cells that hold a
too-low compression tier and still build the later bands, leaving the gate to refuse the recipe until the player
swaps them. That builds faster, but the half-upgraded Eye would look finished.

### B-21 The Kerr Density Upgrade needs an item no one can craft [cost audit]

**Context.** SL-K04 takes one Superstable Black Hole Seed (`ItemList.Black_Hole_Stabilizer`). A scan of every class
in all of the instance's mod jars finds it only in GT's item list, its registration, the BHC, and our SL-K04. No
recipe makes it. So Kerr T2 is uncraftable, and so is everything after it: no DENSIFY, no Hyperdense Stellar
Mixture, no Galactic Shell, no Galactic Upgrade Core, no Sirius B chart, no Eye T4. The boot log could not see this:
the recipe registers fine, it just can never run.

**What we built.** Nothing yet; this audit changes no code.
**Built (recipe audit pass):** the suggestion. SL-K04 takes 32 Black Hole Seeds (`BTSRecipesKerr.DENSITY_UPGRADE_SEEDS`).

**Suggestion.** Replace the Superstable Seed in SL-K04 with 32 Black Hole Seeds (UHV Space Assembler). That costs
about the same, fits the "density" theme, and touches nothing outside the addon. Costs doc, adjustment #1.

**Alternatives.**

- Give GT's Superstable Seed a recipe of ours. This also unlocks the BHC's superstable mode pack-wide (placed BHCs
  included).
- A new BTS "Kerr Seed" item.

### B-22 Eye T2-T4's compression generators need White Dwarf Matter, which only Eye T4 makes [cost audit]

**Context.** The 1:1 gate (Q-20) needs TecTech tier 2 / 3 / 4 Spacetime Compression Field Generators (metas 1-3) for
Eye T2 / T3 / T4. Their TecTech recipes take 2 / 8 / 32 **White Dwarf Matter bolts** per generator: javap,
`addEOHRecipes`, a shared per-tier bolt array of Shirabon, WDM ×3, BDM ×3 and MHDCSM ×2. With
`replaceTecTechEyeOfHarmony=true`, WDM's only source is our Eye T4. So Eye T2 cannot be built without Eye T4: a
deadlock. The spec's §0 note ("no WDM/BDM markers in the generator recipes") only checked the Tier-8 recipe.

**What we built.** Nothing yet.
**Built (recipe audit pass):** SL-P30..P32, with the optional upgrade folded in; see B-28.
**2.9.0-RC-2 port:** superseded. RC-2 makes the generators in the Condensate Assembler and their WDM now comes in
through Resonance Chamber II; SL-P30..P32 are retired and SL-P39 replaces them (B-29).

**Suggestion.** Three alternate assembly-line recipes of ours (SL-P30..P32, registered only with the flag on):
TecTech's own inputs, research and duration, with the WDM bolts replaced by the previous tier's product as a 4th
fluid: Raw Star Matter 16,000 L, ESPM 4,000 L, RHSP 2,000 L. Per tier step that is 3 Sol, 7.5 Sirius A and 7.5
Arcturus runs. Optionally, add an upgrade recipe (tier m → m+1, consuming the old generator), because every step now
strands 188 generators: 1.7e16 EU and about 1,900 assembly-line hours for T2-T4 (costs doc, #2 and #15).

**Alternatives.**

- Make the dedicated tier-gate block from B-3 the gate, with its own recipes, and stop gating on compression.
- Keep TecTech's rocket-tier 0-2 planets' WDM output (B-2 option) as a trickle.

Both need D-49 (B-18) in view.

### B-23 The Eye's TecTech casings cost 8× TecTech's whole Eye [cost audit]

**Context.** The ruling to reuse TecTech's Eye blocks put them in bulk roles:

- 13,872 Reinforced Spatial/Temporal casings in the core (TecTech's Eye: 1,430);
- 6,426 Infinite Spacetime Energy Boundary casings across T1-T4 (TecTech's: 36).

TecTech prices them for its small Eye. A Spatial/Temporal casing is ¼ of an assembly line with **32 Godforge
Singularity Shielding Casings**. A Boundary casing is a whole LSC, 16 Active Transformers and 4 UMV wireless hatches.
An Eye T1 today needs 118k Godforge casings (42 Godforge main rings, 8× TecTech's Eye), 3,876 LSCs and 2.3e16 EU.

**What we built.** Nothing yet.
**Built (recipe audit pass):** the suggestion, SL-P27..P29, plus the band batch sizes (#8-#12); see B-26.

**Suggestion.** Keep the blocks (and the look) and add batch Assembler recipes of ours, registered only with
`replaceTecTechEyeOfHarmony=true` (since changed: they now register whatever the flag says, B-26):

- SL-P27/P28: 1 Godforge Shielding Casing + dense Neutronium/Cosmic Neutronium/Infinity + SpaceTime → 4 casings.
- SL-P29: SC UMV wire + dense Neutronium/Cosmic Neutronium + 1 Emitter UIV + SpaceTime → 8 Boundary casings.

With B-24 this puts Eye T1 at TecTech parity in EU (5.5e15), assembly-line hours (612) and UXV circuits, and at ¼ of
TecTech in Godforge casings. The bands' own recipes get batch outputs too (#8-#10). Costs doc, #3, #4 and #8-#12.

**Alternatives.**

- New BTS look-alike blocks (same textures) for the bulk roles. This means a legend change and new metas.
- Map most shell cells to cheaper existing blocks. This changes the look.

Side effect of the suggestion: cheaper TecTech casings also make TecTech's field generators and its Stargate parts
cheaper, but only while TecTech's own Eye is switched off.

### B-24 294 placeholder stabilisation generators under the pylon feet [cost audit]

**Context.** The six 7 × 7 pylon pads are 294 Stabilisation Field Generators (spec, Eye `Y`). They do nothing yet
(B-3). Each one is crafted from a compression generator, a dilation generator and a Boundary casing. Together they
cost 1,176 UXV circuits and 9.5e15 EU, which is 1.8× TecTech's whole Eye. TecTech's Eye uses 48.

**What we built.** As exported.
**Built (recipe audit pass):** the suggestion. The exporter tags only each pad's centre ring (8 cells around the
foot) as `Y`; `eye_core` is re-exported with `Y` 48 and `Q` 4,704. The tooltip, kit and hologram read the counts
from the shape, so nothing else changed.

**Suggestion.** 48 generators, 8 per pad (the pad's centre ring), with the other 246 cells as plinth casing (`Q`).
This is an exporter-legend change for the structure owner. Costs doc, #5.

**Alternatives.**

- Keep 294 and give stabilisation a real effect that justifies them.
- One generator per pad (6).

### B-25 D-49 play prototype: two readings of the proposal, and a weak supernova check [follow-up pass, design only]

**Context.** A playable page of the D-49 proposal (B-18) was built in my local work folder
(`lifecycle/star-lifecycle.html`). It runs the §3.2 field update once per second with the §3.4 numbers, the
sensor-hatch rigs and the T3 → T4 Core Collapse. Its engine reproduces the proposal's simulated-rig table (for
example T3 two-stage: 99 % Resonant, 0 failures, Y 1.29; T6 left alone: 411 failures in 4 h). No addon code changed;
the Eye freeze holds.

**Judgement calls in the prototype.**

1. **The T4 HSM feed push and the T6 WDM + BDM feed push scale with the time dial m.** §3.4 lists them under drift
   d, which §3.2 multiplies by m; `judge/synthsim.py` adds them unscaled, as trims. The two agree at m = 1.
   *Suggestion:* scale them with m. The feed is upkeep, upkeep is drawn per Eye-second, so a faster star eats and
   pushes faster. *Alternative:* treat them as trims, as in synthsim.
2. **T5's pressure trim is Helium (P +2 per 1,000 L, cap 10)**, as the proposal says. synthsim used molten
   SpaceTime with the same numbers, so the results carry over.

**Finding: the Core Collapse rarely threatens a supernova.** The bounce adds +90 to P and Θ, and a supernova needs a
gauge above +100. So the SpaceTime buffer only matters if the field sits above +10 at that second. In five seeded
runs with no SpaceTime buffered, the two-stage rig survived all five bounces and the bang-bang rig went supernova
twice.

- *Suggestion:* raise the bounce to +150 and keep "1,000 L absorbs 1 point, up to 90,000 L". A centred field then
  needs at least 50,000 L, and a tidy rig about 60,000 L.
- *Alternatives:* keep +90 and fail the bounce above +40 (the Stable band), which also needs about 50,000 L; or keep
  it as written and treat the collapse as a holding test.

### B-28 Compression generators: upgrade the old tier, researched from the star charts [recipe audit pass]

*Numbered B-25 until the follow-up review; renumbered because the D-49 prototype section above already had B-25.*

**Context.** B-22: Eye T2-T4 need TecTech's tier 2-4 Spacetime Compression Field Generators (metas 1-3), whose
recipes take White Dwarf Matter bolts, and WDM has no source while TecTech's Eye is off. The audit proposed
TecTech-copy recipes with a stellar fluid in place of the bolts (#2), and separately an optional "upgrade the old
generator" recipe (#15), because every tier step strands 188 generators.

**What we built.** One recipe per step, #2 and #15 combined (SL-P30/P31/P32, only with
`replaceTecTechEyeOfHarmony=true`):

- tier t-1 generator + TecTech's own tier-t items and fluids (javap'd from `addEOHRecipes`: Yotta cells, Quantum
  Tanks, Infinity Chests, Cosmic Fabric Manipulators, AE2 Singularity cells, UXV circuits, Mutated Living Solder,
  Space, SpaceTime) **→ tier t generator**;
- the WDM bolts become a 4th fluid: Raw Star Matter 16,000 L / ESPM 4,000 L / RHSP 2,000 L;
- the Reinforced Spatial casing TecTech's recipe takes is replaced by the old generator;
- 80,000 ticks flat at UMV (TecTech: 80,000 × (t+1));
- research-station assembly lines with TecTech's research cost for the tier, but researched from **Star Chart:
  Sirius A / Arcturus / Sirius B**. TecTech's own recipe already researches from the generator below, and a
  research item can serve only one recipe.

TecTech's recipes stay (they need WDM). Crude (tier 1, the Eye T1 gate) keeps TecTech's recipe.

Effect: a tier step costs 188 upgrades at about 1.9e15 EU and 209 assembly-line hours, the same at every step, where
re-crafting from scratch cost 3.8 / 5.7 / 7.6e15 EU and 418 / 627 / 836 h. The fluid gate is unchanged (3 Sol,
7.5 Sirius A, 7.5 Arcturus runs per step). A fresh generator of tier t now takes t+1 crafts from a Crude one.

**2.9.0-RC-2 port:** retired. TecTech's research-station generator recipes no longer exist in RC-2, and the ruling
keeps RC-2's generator costs; see B-29 (SL-P39) and its alternative for an RC-2 upgrade path.

**Suggestion.** As built. It fits D-49 (compression stays the 1:1 gate, tiers unlock in sequence).

**Alternatives.**

- Two recipes per step, as the audit listed: a from-scratch copy (Spatial casing, TecTech's duration) and the
  upgrade. That needs three more unique research items.
- Remove TecTech's tier 2-4 recipes under the flag and give ours TecTech's research items (the generator below).
  Cleaner in NEI, but it is more recipe-removal code.

### B-26 Batch TecTech casings: told apart by a programmed circuit, registered whatever the flag says [recipe audit pass]

**Context.** B-23's SL-P27 (Spatial) and SL-P28 (Temporal) have the same inputs in the audit's proposal, and GT's
Assembler cannot hold two recipes with the same inputs.

**What we built.** Programmed circuit 1 (Spatial) and 2 (Temporal); circuits are not consumed. TecTech's own
assembly lines stay.

- **The flag (follow-up review).** SL-P27..P29 first registered only with `replaceTecTechEyeOfHarmony=true`. But our
  Eye is craftable with the flag off too, and then its 13,872 Spatial/Temporal casings came only from TecTech's
  assembly line, about 111k Godforge casings: the "8× TecTech's whole Eye" cost of B-23. So SL-P27..P29 now
  register whatever the flag says. Only SL-P30..P32 follow the flag: they exist because White Dwarf Matter loses its
  source when TecTech's Eye goes, and with the flag off TecTech's Eye still makes it.
- **The side effect.** A pack with the flag off can also build TecTech's own Eye from the cheaper casings, so it is
  no longer at TecTech's stock price.

**Suggestion.** As built.

**Alternatives.**

- Gate SL-P27..P29 on the flag again, which keeps TecTech's Eye at stock price but puts our Eye back at the 8× cost
  when the flag is off.
- Also remove our Eye's controller recipe when the flag is off, so a flag-off pack has TecTech's Eye only.
- TecTech's own markers (a Quantum Chest IV for Spatial, an Accelerator UV for Temporal), at one per
  4 casings. For the 14,000-odd casings that is about 2,350 Quantum Chests and 1,180 World Accelerators, which is why
  we did not.

### B-27 Headless structure test: what it covers, and the console forms of the build commands [structure test pass]

**Context.** No one had built the structures in a world yet (A-3). The test ran on the gradle dev server
(`runServer`, flat world, no player), driven through the server console's stdin.

**What we built.** Console and command-block forms of the two build commands:

- `/btskerr [t1|t2] <x> <y> <z> [north|south|east|west] [force] [survival]`
- `/btseye [t1..t4] <x> <y> <z> [facing] [gen n] [fill] [force] [survival]`

`~` makes a coordinate relative to a command block. The facing is the way the controller's front faces, default
north. Each command:

1. loads the footprint's chunks first, with a 1-chunk margin, so world population has run before anything is placed;
2. builds through `KerrKit` / `EyeKit`, as the player form does;
3. prints FORMED or NOT FORMED, and the first wrong cells with what stands there;
4. runs a mirror check, reading where the mockup's +x landmark actually stands: the Kerr's extractor core, the Eye
   core's lone east gas hatch, and the jet band's east socket;
5. re-projects the creative hologram over the formed machine and checks it is still formed, with no field
   generator changed;
6. runs a damage-and-repair test: it knocks out the plain block next to the controller, checks that the walk names
   that cell, runs the survival auto-place, and checks that it puts back exactly that block, leaves every tile entity
   alone, and the machine forms again.

`survival` builds the whole machine through `survivalConstruct` instead of `construct`: projector-sized rounds (25,
scaled to 200), from an endless item source, with a Forge fake player as the actor. `check` builds nothing and
re-walks the machine already standing at x y z. A console-built Eye is owned by a fixed test UUID
(`beyondthestars:console-test`) for its wireless EU. All of this lives in `machines/SLConsoleTest.java`.

**Results (every case FORMED):**

| Case | Facings | Path |
|---|---|---|
| Kerr T1 + both modules | N, E, S, W | creative N/E/S; survival E/W (15,362 blocks in 79 rounds) |
| Kerr T2 + both modules | S, W, E, N | creative S/W/E; survival N (29,838 blocks in 151 rounds; trigger stack 2 before the upgrade) |
| Eye T1 | N, E, S | creative N/E; survival S (19,615 / 100 rounds) |
| Eye T2 | E, N, E (`~` coords) | creative |
| Eye T3 | S, W, W | creative S/W; survival W (27,942 / 142 rounds) |
| Eye T4 | W, N (`fill`), S, E | creative W/N/S; survival E (34,501 / 175 rounds) |

- **Survival counts.** Each survival count is the §5.1 assembled size minus the controller, and every run ended
  with -1 (no stall).
- **Orientation.** On every case the +x landmark stands on the viewer's right, as in the mockup. The exporter's
  `--flipH` / `--flipD` stay off.
- **Re-projection and repair.** A creative re-projection always left the machine formed. The damage test always
  named the missing cell ("Expects Extreme Density Space-Bending Casing / Reinforced Temporal Structure Casing at
  ..."), and the survival repair placed exactly 1 block. Tile-entity counts were unchanged (20 → 20 on the Kerr
  and Eye T3/T4).
- **Restart.** After a restart, all 11 machines rechecked with `check` were still FORMED: modules, Density Upgrade,
  Galactic core and bands all came back.

**Also fixed.** GT's multi-line "Any of:" structure errors split the Eye/Kerr band and module fault strings over
several lines in the scanner and status output. `BTSStructures.oneLine` now flattens them to "Any of: Stellar /
Beam Band Casing".

**Not covered by a headless run.**

- The client-side hologram hints (`construct(stack, true)`).
- Survival build from a real AE system or player inventory. The endless source supplies exact stacks only, so a
  hatch socket gets its casing and `KerrKit` / `EyeKit` then fit the hatch.
- The NEI preview.
- The 1:1 compression gate and recipe runs (D-49 is in progress, so this pass leaves the star code alone).

**Dev runtime notes.** The dev server loads 63 mods. It has no `dreamcraft` (GTNewHorizonsCoreMod) and no
AvaritiaAddons, and the code tolerates both:

- SL-P30..P32 are SKIPPED: "could not resolve AvaritiaAddons InfinityChest". The dev log says 231 registered,
  3 skipped.
- The Black Hole Compressor controller removal and TecTech's Eye controller removal both log "NOTHING matched ...
  the removal is a no-op". TecTech's 43 Eye recipes are still removed, and our Eye has its 48.

In the full pack these resolve, as the earlier smoke test showed (255 registered, 0 skipped).

**Observation, not a structure problem.** With no player online, the server unloads chunks after a command
finishes. A machine built from the console stops ticking until something loads it again. Two of the T2 Kerrs
still showed "Hole closed" at `check` time, because their seed had not been fired yet. In game, the spec's "keep
the whole footprint chunk-loaded" advice stands.

**Suggestion.** Keep the console forms. They are op-only, cost nothing in play, and give a regression test for
every future structure change: re-export, `runServer`, then pipe the command list in.

**Alternatives.** Move the bench into a dev-only source set so it does not ship in the jar. That needs a
buildscript change for about 400 lines of op tooling.

### B-29 RC-2's Eye T2-T4 compression generators still need White Dwarf Matter, through Resonance Chamber II [2.9.0-RC-2 port]

**Context.** In RC-2 every Spacetime Compression Field Generator is a Condensate Assembler recipe, and TecTech's old
WDM bolts are gone, so the port plan read tiers 1-3 as WDM-free. They are not: tiers 1-3 (the Eye T2 / T3 / T4
gates) take 1 / 4 / 16 MetaMaterial **Resonance Chamber II**, and each chamber takes one **White Dwarf Matter foil**
(`BECMetaMaterialRecipes`, the 4-tier "gray" material). With TecTech's Eye replaced, WDM first exists at our Eye T4,
so Eye T2 would need Eye T4: the same deadlock as B-22. The full per-tier audit (all three generator families and
the EOH casings) is costs doc §9.4; this is the only loop in P1-P3.

**What we built.**

- **SL-P39**: GT's Resonance Chamber II recipe with **2 SpaceTime foils** in place of the WDM foil; everything else
  as GT's (Condensate Assembler, nanite tiers Six-Phased Copper / Silver / Gold / Silver, Chromatic Glass, Infinity and
  Boundless Cosmic Solder condensates, 300 s at UIV). Registered only with `replaceTecTechEyeOfHarmony=true`. GT's
  recipe stays. The chamber keeps its Boundless Cosmic Solder (made from Raw Star Matter), so the generators still
  need Eye T1's product, and each star chart still needs the tier below.
- **SL-P30..P32 retired** (B-28's upgrade recipes). The ruling keeps RC-2's generator costs, their inputs were a copy
  of TecTech's beta-1 recipe that no longer exists, and `TTRecipeAdder` now charges the research EU/t they declared
  (`RECIPE_MAX`). The IDs stay reserved.
- SL-P27..P29 kept: RC-2's BEC casings cost what TecTech's assembly lines did (8 Godforge casings per Spatial/Temporal
  casing, a whole LSC per Boundary casing).

**Suggestion.** As built.

**Alternatives.**

- Another stand-in for the WDM foil: a Raw Star Matter or ESPM product (the chamber already carries RSM through the
  solder), or a Shirabon / Transcendent Metal foil.
- Rewrite GT's own chamber recipe in place under the flag (like the BHC swap), so NEI shows one recipe, not two.
- Bring back an upgrade path on RC-2's terms: a Condensate Assembler recipe tier t-1 generator → tier t, so an Eye
  tier step stops stranding 188 generators (each now carries a Kerr controller, B-30).

**Numbering.** The D-49 build plan (§10.3) claims SL-P33..P38: P33/P34 for conditional Eye T5/T6 gate recipes, and
P35..P38 for Star Chart: Sirius B, its reclaim, Star Chart: Magnetar and the Stellar Ignition Charge. This recipe is
therefore **SL-P39**. It was SL-P35 in commits c9b2376..the port-review fix, which collided with the plan's Sirius B
chart; logs from those builds say SL-P35 for this chamber. `BTSRecipeGuard.recipe` does not check that IDs are
unique, so a collision would register silently. That plan's "post-port audit" (§14) is answered by costs doc §9.4:
on RC-2 the T2-T4 gates need WDM only through Resonance Chamber II, and the T5 gate needs BDM through Resonance
Chamber III. The plan's §10.3 rows for SL-P32 (retired) and SL-P33/P34 (generator upgrades, retired with SL-P30) are
marked obsolete there.

**For P4/P5 (not built):** Eye T5's gate (tier 4) needs Resonance Chamber III (a Black Dwarf Matter foil, T5's own
product), which only the interim T4 BDM recipe unlocks; Eye T8/T9 (tiers 7-8) need Resonance Chamber IV (an MHDCSM
foil, Universium-based) and Eternity plates. Time Dilation and Stabilisation tiers 1+ also need WDM (Wave Focus II,
Field Manipulator II, Sensor Array II); harmless while the Eye accepts any tier of those (B-3), but revisit if B-3
gives their tiers a meaning.

### B-30 The Kerr controller replaces the BHC controller in recipes, so it is priced like one [2.9.0-RC-2 port]

**Context.** I ruled that, with `removeBlackHoleCompressorRecipe=true`, every recipe consuming a BHC
controller takes the Kerr Ergosphere Engine controller instead. In RC-2 that is all 9 compression generator tiers
(and with them every stabilisation generator) plus the coremod's Empty Spatially Compressed Circuit Array (64). An
Eye T1 consumes 238 controllers (188 `W` + 48 `Y` + 2 in SL-E01) and each later tier step 188 more; TecTech's own
Eye consumes 188. The card's SL-K01 cost about 2,400 SpaceTime ingots and 8 Black Hole Seeds: 571k SpaceTime for one
Eye T1 gate set.

**What we built.**

- `BTSBlackHoleSwap` rewrites every BHC consumer in place at load-complete (every recipe map, the assembly-line data
  and research lists, the crafting table), logs each one, and the BHC recipe is removed only when the Kerr is
  craftable and nothing consumes a BHC any more. Verified in `runServer`: 10 recipes rewritten, then the BHC recipe
  removed.
- **SL-K01 re-priced at BHC-controller parity** (costs doc §9.2): the BHC's compression lineage (HIP + Neutronium
  Compressor, 16 Advanced Implosion, 8 Compressor UIV, 4 Electric Implosion), circuits, field generators, superdense
  plates and three of its four fluids, with the Kerr's identity (1 Black Hole Seed, 1 Superradiant Mirror Segment,
  4 Field Coils, 1 Extreme Density casing, TM screws, Super Coolant) in place of the BHC's lenses, ProtoHalkonite
  parts, Kevlar and Oganesson. About 1.1-1.3× a BHC controller; 3,600 ticks.

**Suggestion.** As built. The Kerr is the BHC's successor (D-44), so it inherits the BHC's role as an ingredient and
its price, and the generators stay at RC-2's stock cost.

**Alternatives.**

- Keep the card's expensive SL-K01 and put a cheaper BTS item (a "Kerr core") in the generators instead. Deviates
  from the ruling's "the Kerr controller", but keeps the Kerr itself a big-ticket craft.
- Keep this SL-K01 and cut the Eye's gate count (188 `W` cells, TecTech uses 138), which scales the whole bill.
- Bring back a generator upgrade path (B-29), so each tier step does not re-buy 188 controllers.

### B-31 Reactor fluid sockets refuse crafting input buffers [2.9.0-RC-2 port]

**Context.** In RC-2, GT's input-hatch adder also accepts fluid-capable dual input hatches (CRIBs and their slaves)
and answers "added". The reactor's feedwater, chamber-coolant and propellant sockets drain only their own plain
input hatches, so a CRIB there would have formed the structure and fed nothing.

**What we built.** The three sockets refuse anything that is not a plain Input Hatch, before GT's adder sees it, so
a CRIB can never be registered and starve the reactor silently. A failed Spiral walk also rolls back GT's
dual-input list. The production halls that run through GT's own processing logic (`createProcessingLogic`, which
reads `mDualInputHatches`) accept CRIBs as before. Two machines do not use that logic. Their notes:
- **The Enrichment Train (MTE 28103)** draws its uraninite and HF by hand with `depleteInput`, and GT's helpers walk
  only the plain hatch and bus lists. A CRIB in one of its casings would form and feed nothing. This was already true
  before the port, through the bus adder. The Train now refuses dual input hatches in both adders
  (`supportsCraftingMEBuffer() = false` plus an `addInputHatchToMachineList` override), so the walk fails at that cell
  instead.
- **The Turbine Hall (MTE 28104, `MTEPlantModuleBase`)** is a retired museum piece. Nothing calls its
  `processReactorSteam`, and it never processes, so a CRIB in it changes nothing. It is left as it is.

**Measured on the RC-2 dev server (B-32).** The silo is an optional piece of the plant, so a CRIB in the feedwater
socket does not un-form the plant. The silo walk fails, and the plant idles as a bare platform: silo T0, and the
Spine, Spiral and gallery MISSING. The status shows that, but no error names the socket cell. Any other wrong block
in a silo under construction behaves the same way (the B2 rule: only a legacy face gets the diagnostic re-walk).
Putting the plain hatch back re-forms the full T3 plant. See B-32 for the open question.

**Suggestion.** As built: each socket is a point-of-use line (feedwater grade, coolant grade, propellant) that the
reactor meters itself.

**Alternatives.** Support CRIBs in the sockets: give each socket its own dual-hatch list and drain the CRIB's fluid
inventory in the per-socket draws. More code in the reactor's fluid paths for little gain.

### B-32 RC-2 dev-server test: results, and a silo socket with the wrong hatch is not named [2.9.0-RC-2 test pass]

**Context.** This is the first headless test of the RC-2 port: `runServer` with 76 mods, including coremod 2.9.84, on
a fresh flat world (`sltest-rc2`), driven from the server console like B-27. The world was restarted twice.

**Load.** "[BTS] Recipe registration complete: 232 registered, 0 skipped" on all three boots, with no
`beyondthestars` warning, error or stack frame. GT's own null-input recipe errors come from pack mods missing on the
dev server. TecTech's 43 Eye recipes and its controller recipe are removed (AL-visual 1, AL-data 1, research 2).
"BHC->Kerr: 10 recipes now take the Kerr controller": the Empty Spatially Compressed Circuit Array and the 9
Spacetime Compression Field Generators, Crude to Gallifreyan, all in the Condensate Assembler. Then "removed Black
Hole Compressor controller: AL-visual 1, AL-data 1, research 0, scanner 1". The server-start pass found nothing new.

**New console tools** (op only):

- `/btseye gates` dumps every compression generator recipe the server has, with inputs, nanite tiers and
  condensates. It checks the gate rule (generator tier t must not need anything the Eye first makes at Eye tier t+1
  or later) on direct inputs and one level of Condensate Assembler sub-recipes. It also counts what still consumes
  or makes a BHC controller.
- `/btsbuild t<1-5> <x> <y> <z> [facing] [rod] [force] [crib] | [check]` is a console form of the reactor build.
  `crib` swaps the first feedwater hatch for a fluid Crafting Input Buffer, re-walks the plant, puts the hatch back
  and re-walks again.

**Results.**

| Check | Result |
|---|---|
| BHC consumers left (flag on) | **0**. 0 recipes make a BHC controller; the Kerr controller has 12 consumers |
| Gate rule, Eye T1-T4 gates (generator t0-t3) | **clean**. t1-t3 pass through SL-P39, the second Reinforced Resonance Chamber recipe. GT's own chamber recipe still needs a White Dwarf Matter foil |
| Gate rule, Eye T5-T9 | t4 loops on the Refined Resonance Chamber's Black Dwarf Matter foil; t5 and t6 are clean; t7 and t8 loop on the Resplendent Resonance Chamber's MHDCSM foil. This matches costs doc §9.4 and B-29 (P4/P5 work) |
| Kerr T1 + both modules | FORMED facing N (creative) and E (survival: 15,362 blocks in 79 rounds) |
| Kerr T2 + both modules | FORMED facing S (creative) and W (survival: 29,838 blocks in 151 rounds) |
| Eye T1 | FORMED facing N (creative) and E (survival: 19,615 / 100 rounds) |
| Eye T2 | FORMED facing S (creative) and W (survival: 23,958 / 122 rounds) |
| Eye T3 | FORMED facing N (creative) and E (survival: 27,942 / 142 rounds) |
| Eye T4 | FORMED facing S (creative, `fill`: Sirius B targeted) and W (survival: 34,501 / 175 rounds) |
| Every build | orientation check on the viewer's right; re-projection still FORMED; damage test names the cell; survival repair puts back exactly 1 block, with tile entities unchanged |
| Restart | all 12 still FORMED with `check`, with modules, Density Upgrade, Galactic core and bands |
| Reactor T3 | FORMED facing N, E and S; still FORMED after a restart |
| Reactor CRIB in the feedwater socket | refused: no dual input hatch and no feedwater hatch registered. The silo drops out (see below); the plain hatch re-forms T3. PASS |

**Open: the refused socket is not named.** With a CRIB in the feedwater socket, the plant stays FORMED as a bare
platform. The status reads "silo T0 ... spine MISSING | spiral MISSING | gallery MISSING", but no positioned error
names the socket cell. The probes walk five silo tiers with a null error list, and only a legacy face earns the
diagnostic re-walk (B2). Before the port the same happened to any wrong block in a silo socket, so this is not a
regression. The CRIB case just makes it more likely: a player swaps a working hatch for a CRIB and the whole plant
goes quiet.

**Suggestion.** When the silo probes all fail and a silo socket cell holds a GT machine that is not a plain Input
Hatch, re-walk that tier with the real error list, as the legacy-face diagnostic does. That names the cell. Or add a
status line, "Feedwater socket holds a Crafting Input Buffer: use a plain Input Hatch". It touches the silo probe
loop (R1-B / B2 history), so it waits for your call.

**Alternatives.** Keep it as it is: the status already says the silo is missing, and B-31's tooltip text can say
"plain Input Hatch only".

### B-33 The RC-2 test package: an untagged jar, the pre-redesign Kerr, and an older jar set aside [2.9.0-RC-2 package pass]

**Context.** The package pass built addon `0a878c0` and copied the plain jar plus a player-facing `INSTALL.md` to
`GTNH-Addons/dist-rc2/`, which is outside the addon repo and is not tracked anywhere.

**What was done (judgement calls).**

- **Version name.** The jar is `beyondthestars-0.24.1-master.93+0a878c0080.jar`. Agents never tag, so it carries the
  untagged build name rather than a release number like `0.25.0`.
- **What is in it.** It is the main tree as it stands: the Kerr with modes and screwdriver controls (the
  `kerr-redesign` branch is not merged) and the Eye before D-49. `INSTALL.md` says both will change.
- **Older jar.** `dist-rc2/` already held `beyondthestars-0.24.1-master.85+07deec8b78.jar`, the one now in the
  `rc - 2 TO THE STARS` instance. It was moved to `dist-rc2/superseded/`, not deleted, so nobody installs two copies.
  `INSTALL.md` tells the player to replace it in the instance. The package pass did not touch any instance.
- **RC-3.** `INSTALL.md` tells the player not to install on RC-3 and to ask for a re-pin first (port plan §7
  watch-list).

**Suggestion.** When you want a numbered release, tag it (e.g. `0.25.0-rc2`) and rebuild, so the Mods list shows a
clean version.

**Alternatives.** Ship the untagged name as it is (it is a test build), or delete `superseded/`.

---

### B-34 D-49: the field engine follows the prototype; the Agent-feed ruling lands in the star table, not the maths [D-49 unit 1]

**Context.** Unit 1 of the D-49 build plan ported the stability field to pure Java (`machines/eye/field/`) and added
the offline golden test: `node tools/fieldsim-golden.js --java` runs the prototype's own engine block (fixture
`tools/fixtures/star-lifecycle-engine.js`, sha1-pinned to `designs/star-lifecycle-prototype.html`) and the Java on
78 scripted cases. All 78 match bit for bit: every count, every ledger total, the final state and a per-second hash.
Three deliberate one-line mutations of the Java each fail 6-39 cases, so the test is not vacuous. Nothing in the
game uses the engine yet (units 4-5).

**Judgement calls.**

1. **Your answer "the Antipolarization Agent is the magnetar's constant feed" does not change the field maths.** In
   the engine, T6's feed push (P −4+5r, Θ −0.5+3r, Ω r) depends only on the upkeep fraction r, whatever the upkeep
   fluid is. So if unit 4 makes the Agent the T6 upkeep, a short Agent supply sags the magnetar exactly as a short
   WDM + BDM supply did in the prototype. The engine and the golden file stay as the prototype has them. The Agent
   **trim** (the flare shield, A +4 per unit) also stays.
   - *Suggestion:* unit 4 sets the T6 upkeep per cycle to Agent only. WDM + BDM become the merger charge, and the
     re-merge after a loss is your "tops it up". The Field-socket Agent trim stays as the flare response, so a
     player pipes Agent to two places: bulk upkeep and the trim valve.
   - *Alternative:* the constant feed also charges the shield. That is a maths change: update the prototype first,
     refresh the fixture, and regenerate the golden file.
2. **Plan §15.1's "T4 at m = 2.25 with tt = 6" cannot happen.** At T4, Time Acceleration tier 6 caps the dial at
   1 + 0.25·(6 − 4) = 1.5. The golden cases use tt = 9, which allows 2.25, with ts = 1 and ts = 9.
3. **The event clock is fractional.** The prototype schedules the next event at `t + uni(every)/m`, so plan §3.3's
   `ev.next` must be saved as a double, not a long. The field state notes this for unit 4.
4. **`light()` no longer starts the T5 batch itself.** The prototype's `light()` always starts it. In the mod, the
   runtime calls `startBatch` only when a WDM charge is buffered, and otherwise goes IDLE (plan §7.5).
5. **A missing 5,000 L HSM seed at the bounce is a supernova ("no neutron seed")**, as plan §7.4 says. The prototype
   never reaches this case, because it needs the HSM before the collapse can begin, so the golden test does not
   cover it.
6. **The Core Collapse results at the adopted +150 shock** (seed 1806): with 0 L or 30,000 L of SpaceTime both rigs
   go supernova, and with 60,000 L both settle. At +90 every case settles. This matches B-25's "a centred field needs
   about 50,000 L". The golden set also runs the collapse with the bang-bang rig, which goes beyond the plan.
7. **FieldSim's ledger uses the prototype's upkeep table** (T6: WDM 60 + BDM 15). It is a test ledger only. The mod's
   table is unit 4's star definitions, so ruling 1 does not touch the golden file.

**Suggestion.** Accept 1-7 as built. Re-run `node tools/fieldsim-golden.js --check --java` after any change to the
prototype or to `machines/eye/field/`.

**Alternatives.** For 1, see the alternative above. For 2, add a tt = 6 case at m = 1.5 as well.

### B-35 D-49: content for the star lifecycle; the Eye T5 gate needs a WDM chamber [D-49 unit 2]

**Context.** Unit 2 of the D-49 build plan added the content:

- 7 items: the Stellar Ignition Charge, the four transition cores, and Star Chart: Neutron Star and SGR 1806-20;
- 3 fluids: Positronic Tengam Slurry, Antipolarization Agent and Magnetar Matter;
- the Stellar Field Sensor Hatch (MTE 28116) with its MUI2 GUI;
- recipes: the Agent chain SL-S01/S02 (new `BTSRecipesStellar`), the cores SL-E03..E06, the hatch SL-E07, and charts
  and parts SL-P25 (now the Neutron Star chart), SL-P33 and SL-P35..P38;
- removals: SL-P26 and the interim T4 Black Dwarf Matter star go, together with `interimBlackDwarfAtEyeT4`;
- textures, lang entries with tooltips, and the `/btskit eye` contents.

Every pack material the Agent chain uses was checked in the RC-2 GT sources (5.09.54.205): `Materials.TengamAttuned`
has dust items, and GT's own Polarizer recipe makes it from Purified Tengam (`ProcessingTransforming`). Raw Tengam
goes to Purified in the EMS (`ElectromagneticSeparatorRecipes`). `Materials.Antimatter` is linked to the fluid
`antimatter` in `LoaderGTBlockFluid`. Molten Purified Tengam is generated because the material has metal items.

**Judgement calls.**

1. **SL-P33: Resonance Chamber III with 2 White Dwarf Matter foils instead of GT's 1 Black Dwarf Matter foil.**
   The audit rule says compression generator tier N must not need anything the Eye first makes at tier N or later.
   Tier index 4 (the Eye T5 gate) takes one Resonance Chamber III. That chamber takes a BDM foil, and with the
   interim T4 BDM star gone, BDM first exists at Eye T5 itself. Everything else is GT's tier-3 chamber unchanged:
   - inputs: 2 Emitter UIV, 2 Shirabon rods, 1 Hexanite plate;
   - nanites: White Dwarf, Gold, Transcendent, Gold;
   - condensates: Chromatic Glass 16, Infinity 8, Boundless Cosmic Solder 3;
   - 300 s at UMV, in the Condensate Assembler.

   WDM is the product of the tier below (Eye T4). That is the same "use the previous tier" step SL-P39 takes with
   SpaceTime for the WDM foil. It is registered only with `replaceTecTechEyeOfHarmony=true`, like SL-P39, and GT's
   own recipe stays. The Eye T6 gate (tier index 5: 4 chambers, Creon) may use BDM, so it is clean as it is, and
   **SL-P34 stays reserved and unused**. Verified in `runServer` (`/btseye gates`): the tier-index-4 generator now reads "via Refined Resonance Chamber: 2 recipe(s), a clean one exists", and the gates for Eye T1-T7 are all CLEAN. Only T8 and T9 loop, through Resonance Chamber IV's MHDCSM foil (P4/P5, as B-29 found). The boot log reads "243 registered, 0 skipped".
   - *Alternatives:* 2 SpaceTime foils (exactly SL-P39's stand-in, so the T5 gate needs no WDM at all), or a
     Shirabon foil. Another option is to rewrite GT's chamber recipe in place, so NEI shows only one recipe.
2. **The interim BDM star is deleted now, in unit 2, not in unit 5.** The plan gives unit 2 the
   `Config.interimBlackDwarfAtEyeT4` removal. The flag's only readers were SL-P26 and two lines in
   `EyeRecipeRegistry` (unit 4's file). Those two lines were removed so that unit 2 compiles: the
   `star:siriusbcooled` registration and its check in `replacementStarsPresent()`. Until units 4 and 5 land, a jar
   built from master has no BDM source, and the Sirius B chart is crafted from WDM (SL-P35) while Sirius B is still
   the T4 WDM star. These intermediate commits are not a release. The stale config key is removed from the file
   when the game loads.
3. **The sensor hatch emits on all six sides.** Plan §12 says "as the pH sensor does", but GT's pH sensor only
   powers its front face. The addon's Spine Sensor powers all six sides, and a hatch sunk into the Eye's shell has
   only one exposed face anyway, so all six sides costs nothing. Other details:
   - The output falls to 0 when no readout arrives for 60 ticks (Eye unformed, star dark, chunk unloaded).
   - The threshold is clamped to ±120, and the comparators are strict (`>` / `<`).
   - Analog mode ignores the threshold.
   - The settings are saved in the tile, not the item: the Eye pushes the readings again every second.
   - **For unit 4:** after every field second, call `publish(FieldReadout)` on each Sensor-socket hatch, and
     `publish(FieldReadout.DARK)` when the star goes out.
4. **Tooltips.** Plain items have no tooltip. `SLItems.defineWithTooltip` is `BTSItems.define` plus a tooltip read
   from the lang key `item.beyondthestars.<name>.tooltip`. The Galactic Upgrade Core and both Sirius B charts now
   use it too. The item class is not stored in a saved world, so this changes nothing in existing saves.
5. **Agent chain cost, now that the Agent is the constant feed (your answer 1).** The chain is the proposal's. At
   the simulated ~100 L/s of Agent, it costs:
   - about 10 L/s of Antimatter;
   - 1.6 Attuned Tengam dust/s, of which about 0.4/s is fresh Tengam once the Eye returns 12 Purified dust per
     1,000 L;
   - 200 L/s of Super Coolant in the freezer step;
   - two LCRs and one Vacuum Freezer running all the time at UXV.

   The T6 upkeep figure belongs to unit 4's star table. If it comes out well above 100 L/s, retune the 100 L of
   Antimatter per 1,000 L of Agent before you retune the Eye.
6. **Smaller choices.**
   - SL-P38 makes 4 charges per craft from Helium **plasma** (`Materials.Helium.getPlasma`).
   - SL-E03 takes 16 charges and researches from one charge. The research chain is: charge → Stellar Mass Core →
     Shell-Burning Core, and Galactic Upgrade Core → Subspace Cooling Core → Magnetar Seed Core. Every research item
     is unique to this mod. *Changed (B-62):* GT's Scanner consumes the research item, so SL-E04/E05/E06 now research
     from Star Chart: Arcturus / Sirius B / SGR 1806-20 instead of the previous core.
   - SL-P36 (the Sirius B, Cooled reclaim) takes programmed circuit 3 and no fluid.
   - In the kit, Antipolarization Agent, DSSF and Tachyon Rich Temporal Fluid come in Neutronium Large Fluid Cells
     filled to capacity. The obsolete Sirius B, Cooled chart is no longer in the kit.
   - **For unit 4:** Tachyon Rich Temporal Fluid is a **molten** fluid in RC-2 (`Materials.Time.getMolten`; the
     material name is `temporalFluid`). `Materials.Time.getFluid` returns null.

**Suggestion.** Accept 1-6 as built.

**Alternatives.** For 1, see above. For 3, use front-face output, exactly as GT's pH sensor does.

---

### B-36 D-49: the Eye's T5/T6 bands, cumulative bands and the new sockets [D-49 unit 3]

**Context.** Unit 3 of the D-49 build plan built the Eye's structure to T6:

- **Exporter.** `tools/export-structures.js` now exports two more pieces from `designs/voxel.js`:
  - `eye_arms2` (T5, the second arm pair): 135 × 11 × 83, 6,231 blocks;
  - `eye_rim` (T6, the galactic rim): 158 × 8 × 149, 11,202 blocks, with 4 rim stellar sockets `g` at the mockup's
    GRB cells.
- **`eye_core` re-export.** It now carries 8 Field sockets `f` and 8 Sensor sockets `n`. Those 16 cells were
  Reinforced Spatial casing (`U` 9,406 → 9,390). The assembled T5 Eye is 135 × 57 × 137 (40,733 blocks), and the T6
  Eye is 158 × 57 × 149 (51,935 blocks). `--check` is clean, and the Kerr shapes did not change.
- **Controller.**
  - `MTEEyeOfHarmonyBTS` gains the two pieces, `structureTier()` (the bands are cumulative), `compressionTier()`, and
    the 1:1 gate table extended to T6: `{-1, 0, 1, 2, 3, 4, 5}`.
  - Hologram and survival auto-place take stacks 1..6.
  - `EyeHatchElement` gains `Field`, `Sensor` and `Items`. The `h` sockets also take input buses.
  - The scanner names a band that stands above a missing one.
  - The tooltip lists the bill of materials for all six pieces.
- **Test bench.** `/btseye t5|t6`, plus two console tokens: `legacy` builds without the D-49 hatches, and `nobeam`
  clears the beam band after the build.

**Dev-server results (`runServer`, 243 registered, 0 skipped):**

| Case | Result |
|---|---|
| T6 north, creative, `fill` | FORMED. Sockets: gas 4, input bus 1, stellar 15 (6 + 5 + 4), Field 8, Sensor 8. Mirror check right, re-projection still FORMED, survival repair 1/1. |
| T6 east, survival path | FORMED. 51,934 blocks in 264 rounds (the whole T6 minus the controller), ended -1 |
| T5 south, creative | FORMED, structure T5 (rim absent) |
| T4 west, `legacy` (f/n cells left as Reinforced Spatial casing) | FORMED, Field 0, Sensor 0 |
| T6 north, `legacy` | FORMED, Field 0, Sensor 0 |
| T3 north, `nobeam` | FORMED as T3, then 4,343 beam-band cells cleared → "Eye T1: structure T1", the scanner says the jet crescent stands but does not count, and the jet's stellar sockets dropped (stellar 11 → 0) |
| `check` on the T6 | walk + status 9.4 ms the first time, 2.7 ms later (the plan's target is under 20 ms) |
| `/btseye gates` | CLEAN for Eye T1-T7 (T5 through unit 2's SL-P33; T6 clean as GT ships it), LOOP only at T8/T9 (P4/P5) |

**Judgement calls.**

1. **The Field and Sensor sockets moved.** Plan §11.1 put them at 15, 75, 105, 165, 195, 255, 285 and 345 degrees.
   The exporter's socket check found 15, 105, 195 and 285 inside the arm blades: each blade covers its root
   +5..+30 degrees one row below the mid-plane, and ±5 degrees does not clear it. So every pair now sits **45 and 75
   degrees past an arm root** (45, 75, 135, 165, 225, 255, 315, 345). That is 15 degrees from the nearest clamp strut
   and rotationally symmetric. `f` is one row below the core band's mid-plane on its outer face, and `n` is on the
   mid-plane directly above it, so every sensor touches its Field hatch. The T6 reach check passes: every socket has
   an outside face.
   - *Alternative:* put the 16 sockets on the upper and lower rails instead.
2. **The tier unlock stays the pre-D-49 rule until units 4 and 5.** `installedTier` is the highest tier allowed by
   both the cumulative bands and the compression gate, capped by the old unlock: T1-T3 by the bands, T4 with the
   Galactic Upgrade Core. So a full T6 Eye forms and reports "structure T6, compression T6", but runs as T4, with no
   extra parallels from the T5/T6 bands. Unit 4 replaces the cap with `unlockedTier`.
3. **Every band is walked, even above a missing one.** This lets the scanner name the gap ("the jet crescent stands
   but does not count until the beam band is built"). A band that does not count drops the stellar sockets it
   registered, the same as a failed band. It costs at most a few milliseconds per walk.
4. **No crafting input buffers on the plinth.** The input-bus role of the `h` sockets refuses a CRIB, as the reactor
   sockets do (B-31): it holds patterns, not stock. As with B-31, the socket then fails and the Eye does not form
   until it is swapped.
   - *Alternative:* accept a CRIB and read its inventory like a bus.
5. **Kit and test build.**
   - The last plinth gas socket takes an input bus, for the ignition charges; the other four take gas hatches.
   - The T4 build still loads Star Chart: Sirius B, which is still the T4 white-dwarf run until unit 4.
   - T5 loads Sirius B and T6 loads the Magnetar chart. Neither has a star until units 4 and 5.
6. **Status text.** The recipe-result lines for tier 3 and tier 4 were edited in place to name the beam band, since
   the bands are cumulative now. This is an exception to the append-only rule for `en_US.lang`. The tier5/6 and
   compression5/6 lines were appended.
7. **NEI preview (B-19 follow-up), not checked in a client.** BlockRenderer6343 counts tiers by building stack after
   stack until one adds nothing, so it should find 6 (stack 7 is clamped to 6). The T6 Eye is the same height as T4
   (57 blocks), so it still fits the scratch world, but it has 51,935 blocks against T4's 34,502 (+50 %). Expect the
   frame rate in the preview tab to be worse than at T4.
   - *Suggestion:* check it in the client. If it stutters, use B-19's fallback: an `INEIPreviewModifier` that leaves
     the band linings and glass (about 14,100 blocks across the six pieces) out of the preview only.
8. **The hologram at stacks 5 and 6** places compression generators at tiers 5 and 6 (metas 4 and 5), the T5/T6
   gates. The client-side hint path (`construct(stack, true)`) was not run headless. It reads only the trigger stack
   and the shape files, which the server paths above exercised.
9. **Migration.** A pre-D-49 Eye has Reinforced Spatial casing in all 16 new socket cells. The old `eye_core.txt`
   had `U` there, as the exporter diff shows, and both new sockets fall back to that casing, so old Eyes still form.
   The `legacy` runs prove this. An Eye built with the jet crescent but no beam band now runs as **T1** (your answer
   2), and the scanner says why. A pre-D-49 jar was not loaded; the `legacy` token reproduces the old cells exactly.

**Suggestion.** Accept 1-9 as built.

**Alternatives.** For 1, sockets on the rails. For 4, accept CRIBs. For 7, build the lighter NEI preview now rather
than after a client check.

---

### B-37 D-49: live stars T1-T4, the unlocked tier, escrow and the save migration [D-49 unit 4]

**Context.** Unit 4 of the D-49 build plan makes the Eye's stars live. The new package is
`machines/eye/star/`:

- `StarDef` is the per-cycle star table.
- `EyeStarRuntime` is the state machine (DARK, IGNITING, LIT, PENDING), and it handles cycles, ignition, the time
  dial, trims, the T4 feed, output, milestones and the core escrow.
- `EyeNbt` is the one NBT writer and reader. It also handles the 0.24.x migration.
- `EyeMilestones` holds the per-tier counters.
- `EyeStarBench` is the `/btseye star …` test bench.

The controller runs one field second every 20 ticks, straight after its socket drain. `installedTier` is now
`min(unlockedTier, structureTier, compressionTier)`. Planets still run as batch runs, as built.

**Dev-server results** (`runServer`, flat world, console; boot log "243 registered, 0 skipped", "6 live stars, 43 planet
recipes"):

| Plan test | Result |
|---|---|
| 3 (Sol, `unlock 1`, `seed 1806`, `lit`, `rig fine`, `sim 14400 compare`) | **COMPARE PASS**: the hash `3cde8ee7`, the zone counts (14,400 Resonant), the mean Y 1.489601 and the RSPM 17,875,208 L all equal offline `FieldSim` |
| 4 (Arcturus ignition from 1 charge) | IGNITING at P −80; P was −23.0 after 38 s and lit after 40 s; the seed cloud took 4·10⁸ L of H and of He; 2.42·10¹³ EU (10 cycles) debited; no product while igniting |
| 5 (Sol at p = 4 with 6·10⁷ L He) | First cycle r 0.30, then 0.00: P sags; the cycle's RSPM was 25,750 L = 50,000 × 4 × mean Y 0.4292 × 0.30; the star breached at P −101 |
| 9 (neutron star, feed 1.5, 3,600 s) | **COMPARE PASS**; 5,400 L of HSM drawn (1.5 × 3,600); WDM 24,716 L = 3.6864 × 5,400 × mean Y 1.2416 |
| 12 (sensors) | P > 5 at P +4.3 gave 0; P < 5 at P +8.7 gave 0; P > 0 at P +6.7 gave 15; Ω analog at +1.2 gave 8 (= round(15·121.2/240)) |
| 14 (100 s, `save-all`, restart, 100 s) | **COMPARE PASS** after the restart: 200 s, hash `f29e488b`. The RNG, the clock and the bench ledger persisted |
| 15 (`core subspace`, `set dial 1.5`, `wrench`) | Unlocked tier, milestones, escrow and buffers kept; the star is DARK |
| 16 (migration via `legacy`) | Galactic → unlocked T4. Arcturus run in flight ×2 → 100,000 L of RHSP owed, paid on the next formed second. Bands T1-T3 → grandfathered to T3. Jet without beam → unlocked T3, but runs as T1 (the scanner names the gap) |
| 18 | One field second takes about 2-5 µs (`sim 14400` ran in 68 ms) |

Two findings came out of the run and are fixed. A tile that had just been reloaded with its chunk had no hatch lists
until GT's start-up walk, and the status lines threw an NPE on the never-walked band faults. The bench now walks
such a tile first.

**Judgement calls.**

1. **T6 upkeep: Antipolarization Agent 3,000 L per cycle (50 L/s, × p).** This follows your answer that the Agent is
   the constant feed. WDM 18,432 + BDM 4,608 + Agent 100,000 become the merger charge (and the re-merge "top up").
   The flare-shield Agent trim in the Field sockets stays on top of the upkeep (B-34 item 1). So a magnetar uses about
   50 L/s of Agent as upkeep plus what the rig doses. Only the table is built here; the T6 runtime is unit 5.
   - *Alternatives:* 6,000 L per cycle (the simulated rig's ~100 L/s as pure upkeep), or let the upkeep also charge
     the shield (a maths change: prototype first, then regenerate the golden file).
2. **Milestones and the escrow are built in unit 4.** The plan gives them to unit 5. The task asks for "milestones →
   core → escrow", and their NBT belongs to `EyeNbt`. The transitions that consume the core are still unit 5's.
   Until then, a next-tier chart in the slot parks the star in **PENDING**: it keeps burning and nothing more
   happens. Any other chart, a planet or an empty slot puts the star out.
3. **The escrow is returned by sneak-PUNCHING the controller with an empty hand, not sneak-right-click (plan
   R-14).** GT's `BaseMetaTileEntity.onRightclick` never passes a sneaking right-click to the machine; it opens
   covers instead. Left-click does reach the machine.
   - *Alternative:* a GUI button (unit 6).
4. **A freshly loaded star freezes until GT's first structure check (about 5 s).** It does not go out. Otherwise
   every world load would extinguish every star, because `mMachine` is false until that walk. After the walk, an
   unformed Eye puts the star out (R-8), and so does a disabled one (R-7). The maintenance and structure stops GT
   applies disable the machine, so the star goes out then too.
5. **A star chart never starts a GT run.** `checkProcessing` returns a status (`star_lit`, `star_igniting`,
   `star_pending`, `star_dark`, `star_remnant`, `star_unit5`) or the tier gate. The tier gate is split into
   bands (`tierN`), the new unlock (`lockedN`, new lang keys) and compression. GT's active flag is forced on while
   the star burns.
6. **Fractional litres.** The T4 HSM draw and every cycle's product are fractional. The remainder carries to the
   next second or cycle (`hsmOwed`, `prodCarry` in the tile NBT), so nothing rounds away.
7. **Migration edges.**
   - Any `star:*` run key in a loaded tile is pre-D-49. Its output is owed (`btsEyeLegacyPay`, kept in item NBT too),
     and GT's progress is zeroed so GT cannot finish the run.
   - The bands are grandfathered at the first walk without errors (`btsEyeLegacyPending` persists until then).
   - A controller wrenched to an item **before** the update with no Galactic core and no buffered fluid has no item
     NBT at all (GT writes a tag only when there is something in it). It cannot be told apart from a new controller,
     so it starts unlocked at T1. `migration.md` says so.
8. **Smaller choices.**
   - The ignition seed cloud and the 10 cycles of EU scale with p.
   - The dial setpoint wraps 3.00 → 1.00. In use it is capped by m_max.
   - The feed setpoint steps 0.1 and wraps 1.5 → 0.
   - The creative `/btseye tN` build is unlocked to tN.
   - The kit's T4 build loads Star Chart: Neutron Star.
   - NEI star pages show one cycle: up to 4 fluid inputs (`maxIO(1, 81, 4, 18)`). The T4 page shows the HSM feed at
     the 0.694 L/s default.
   - `replacementStarsPresent()` now needs Sol, the neutron star and Sirius B (plan §10.4). Sirius B is registered
     as data (its batch runtime is unit 5).
9. **Tooltip.** Only the lines D-49 made false were edited (T4 Sirius B, the Galactic right-click, "stars always
   succeed", the placeholder fields). The full rewrite is unit 6's.
10. **Loading `FieldSim` in the game makes FML warn "MOD HAS DIRECT REFERENCE System.exit()".** The warning comes
    from `FieldSim.main` (unit 1's offline runner), which the bench's `sim … compare` loads. It is harmless.
    - *Suggestion:* swap the call for a return code in a later pass.

**Not run here.** These need a client or another unit:

- the controller GUI text;
- the screwdriver and the right-click escrow (the bench's `core` / `escrow` take the same code path);
- the time dial paid from real Field-socket Time;
- an ME input bus as the charge source.

**Suggestion.** Accept 1-10 as built.

**Alternatives.** For 1, see above. For 3, a GUI button. For 4, put the star out after a load too (harsher; not
recommended).

---

### B-38 D-49: the test package ships without the Eye GUI and the centre render [D-49 package pass]

**Context.** The D-49 package is `dist-rc2/beyondthestars-0.24.1-master.120+12fcf51c4a.jar` (sha256 `36ab9d10…56e4`),
built from `12fcf51` with `gradlew build` clean (no spotless or checkstyle fixes needed). `dist-rc2/INSTALL.md` is
rewritten for D-49. The hotfix jar `0.24.1-hotfix-rc2-recipe-result-lang.95+b320e4e8e6` moved to `superseded/`;
master already carries that fix (`f484699`).

**Judgement calls.**

1. **Packaged as is, with plan unit 6 not built.** There is no `EyeGui` (gauge bars, dial/feed buttons), no centre
   render (`eyeRenderCentre` is still read by nothing, and its config comment still says so), and the controller
   tooltip is only partly rewritten. INSTALL.md says all three are missing and points players at the scanner and the
   screwdriver. The task was to package, not to build them.
2. **The `/btseye tN` chat line is stale.** `CommandBTSEye` still says "the star chart in the controller slot runs at
   once" after a `fill` build. Under D-49 a T1-T3 star still needs an ignition charge (or `/btseye star … lit`). It is
   not a spotless/checkstyle fix, so I left it; INSTALL.md warns about it.
   - *Suggestion:* reword it in the unit 6 pass ("drop a Stellar Ignition Charge in the plinth input bus").
3. **Unit 5 and the test pass have no questions entry.** INSTALL.md's "dev-server results" line comes from the test
   pass's server logs: every transition reached COMPLETE and unlocked its tier, the Core Collapse also produced a
   supernova, and COMPARE PASS held. Unit 5's judgement calls still need their own entry.

### B-45 D-49: the Eye GUI, the centre render, cover-friendly sensors and the T5/T6 cost table [D-49 unit 6]

**Numbering.** This is B-45, not B-39: the `kerr-redesign` branch used B-33..B-44 for its own questions. At the merge
those became **B-46..B-57** (old kerr-redesign B-n = B-(n+13)); B-33..B-38 and B-45 are master's D-49 / RC-2 entries.

**What was built.** The Eye's MUI2 GUI (`EyeGui`), its centre render (`machines/eye/render/`), the sensor hatch's
cover-friendly output, the bench's `render` command, the T5/T6 cost table (costs §10), the build spec's D-49 block
and "Contract additions (Eye, D-49)", the migration note, and D-49 status lines on A-2, B-1, B-3, B-5, B-6, B-9 and
B-10. No star maths, recipes or NBT schema changed; the only new persisted keys are `btsEyeRender` (per-Eye switch,
item and tile) and `btsEyeRenderPos` and the star's `lastFail` (tile only).

**Judgement calls.**

1. **The render reuses TecTech's star, not TecTech's render tile.** TecTech's Eye render (`TileEntityEyeOfHarmony` +
   `EOHTileEntitySR`) draws one fixed orange star with a 13-block "outer space" shell and orbiting planet blocks, and
   the Forge of the Gods' renderer (`RenderForgeOfGods`) always draws its rings and beam with the star and keeps its
   shader private. So the Eye has its own render block (GT's `BlockRenderer`, as the BHC and the Kerr) and TESR, which
   bakes the Forge of the Gods' own shader recipe (`tectech:star`: the texture's luminance in our colour, raised to
   1/gamma), uses TecTech's `StarLayer0..2` textures and `EOHRenderingUtils.buildSphere` / `beginSphereCull`, and
   draws the layers in the Forge of the Gods' order (opaque core, then two translucent shells).
   - *Suggestion:* keep. *Alternative:* place TecTech's own EOH render tile for T1-T3 (the exact TecTech look,
     planets included, but always orange).
2. **Looks (PROPOSED, `EyeStarLook`).** Sol r 9 yellow-white, Sirius A r 11 blue-white, Arcturus r 15 orange (the
   mockup's sphere), the neutron star r 2.2 blue-white spinning fast, Sirius B r 3 white → orange → dull red → black
   over its batch (gamma 3.5 → 0.35), the magnetar r 2.6 violet. Instability makes a star breathe (wobble up to 10 %
   of its radius as Stability falls), an event warning adds more (the magnetar throbs 25 % on a flare warning).
   The nearest `eye_core` block is 20.9 blocks from the centre, so the Core Collapse's swelling Arcturus is capped at
   r 17 (about 19.4 with its wobble and outer shell).
3. **What counts as "lit".** The render shows while the star exists: igniting (a dim red protostar swelling toward
   the star), lit, pending and a T5 batch. T5 IDLE (no batch) and DARK show nothing. A supernova keeps the block 3 s
   for its flash, then it goes: the one exception to "removed when the star goes out".
   - *Alternative:* nothing until LIT; or a dark remnant while IDLE.
4. **Unload.** GT calls no MTE hook a block could be removed from on a chunk unload, and removing blocks while a
   chunk unloads is unsafe. Instead the render tile checks every 100 ticks and turns to air when its owner's cell is
   not loaded or no Eye there claims it; the Eye places it again when it next ticks (the first tick after its first
   structure check). An Eye that has lost track of its block (a save written before it was placed) clears it at once.
   So there are no orphans, and an unloaded Eye shows nothing.
5. **Config.** `eyeRenderCentre` off on a client: nothing is drawn there. Off on a server: no Eye places a block (and
   placed ones go). The per-Eye switch is in the GUI. The config file is now written on every start, because Forge
   neither reads comments back nor counts a changed comment as a change, so the old "NOT IMPLEMENTED" comment would
   never have gone (verified on the dev server: the comment is rewritten, values kept).
6. **Sensor output and covers.** The hatch now emits through `setInternalOutputRedstoneSignal` (as GT's own pH, lens
   and electrode sensors), so a cover that drives its own side (a receiver, a detector) is never overwritten. GT's
   Redstone Transmitter (Internal) and Advanced Wireless Transmitter (Internal) read the side's own output, which
   that call skips, so those two get the signal directly: one sensor can drive a wireless channel to a Fluid
   Regulator cover or an AE2 export bus anywhere. It is still pushed every field second.
7. **GUI choices.** GT's standard multiblock layout, English strings in code (as the Kerr's GUI; no GT-looked-up lang
   key and no new recipe-result id was added). Dial and feed are − / + buttons (right-click four steps) with the
   value between them, not text fields; the server clamps. The gauge bars draw the five zones around the setpoint
   (0, or the T5 cooling setpoint, with a yellow tick). The plan's P/Θ dial drawable (a stretch goal) is not built;
   the Field side panel is (zone legend, sockets, every sensor's status, trims this second, the rig).
8. **Cost table (costs §10), by hand.** The audit's model script is not kept, so §10 multiplies the manifest's
   counts by §3.2's per-block figures and GT's BEC generator recipe (`addSpacetimeCompression`, tiers 4 and 5).
   Verdict fair: the bands are cheap (1.5e14 EU, 193k L HSM for both), the 188 generators per step are the price
   (2.4e15 / 2.8e15 EU), 1.36× TecTech's own step to the same tier. **RC-2 oddity:** GT's generator times are 2, 3, 4
   and 5 million ticks for tiers 0-3 but 100,000 and 120,000 for tiers 4-5, so our T2-T4 gate steps cost about 30×
   the T5 and T6 steps in EU and hours. That is stock GT data; we do not change it.
   - *Suggestion:* leave it (it is the pack's balance for TecTech's Eye too). *Alternative:* our own BEC recipes for
     tiers 1-3 at the tier-4 time.
9. **The plan's PROPOSAL.md D-register updates (D-30, D-31, D-34, D-41, D-42, D-43)** are left to the main session
   (the design repo is not this unit's to edit).

**Dev-server results** (`runServer`, a T6 Eye built by `/btseye t6 3000 100 0 north gen 9 fill`, centre (3000, 140,
40) as computed from the manifest; `render` prints the cell, its block and the look):

| Step | Render |
|---|---|
| Dark Eye | nothing placed |
| Sol / Sirius A / Arcturus lit | live star r 9 FFD678 / r 11 AAC8FF / r 15 FF8232 |
| Neutron star lit | neutron star r 2.2 BEDCFF, spin 12 |
| Chart removed (star out) | air at once |
| Ignition from a charge | protostar r 3.55 E94F23, then Sol once lit |
| Core Collapse phase 1 (300 s in) | swell r 15.6 FF6C2C |
| Supernova at the bounce (no SpaceTime) | supernova flash (kind 8). 6 s later the block still stood, unclaimed: the controller had been reloaded and lost its position. The stray-block clearing (judgement call 4) was added after this run for exactly that |
| Core Collapse with 60,999 L SpaceTime, settling | bounce, proto-neutron star r 2.6 |
| Sirius B, half a batch | cooling dwarf FF9F5D, gamma 1.93 |
| Magnetar Merger | magnetar r 2.6 C85AFF |
| Per-Eye switch off / on | air / placed again |

The dev server unloads the Eye's chunks between console commands (no player there), so each command can meet a
freshly loaded controller; the first run's stale looks came from that, and the bench now drives the render itself
(`render`, and every `sim` second). One check stayed inconclusive for the same reason: after breaking the controller,
`testforblock` found the centre's chunk unloaded. The tile's own check is the path that removes the block there.
**Not seen in a client yet:** the drawing itself (and under Angelica's core profile), the GUI layout.

---

### B-46 Kerr redesign: every module recipe needs an open hole [Kerr redesign, spec only]

**Context.** You ruled that the modules become separate machines "that just have a requirement of a hole being
open to run". Today's Kerr is kinder than that: with the hole closed, COMPRESS still runs plain compressor recipes at
the base parallel (B-8).

**What the spec says** (`kerr-redesign.md` §3). A module runs nothing unless it is linked to a formed KEE with an
open hole. This is also RC-2's Black Hole Compressor behaviour: its `validateRecipe` returns `NO_BLACK_HOLE` for
every recipe while the hole is closed. DENSITY needs Kerr T2 and at least 128 SM, as before. The compressor module
searches its three maps together, so it needs no machine mode at all.

**Suggestion.** As specced.

**Alternative.** Plain compressor and extractor recipes run with the hole closed, at 8 × tier with no bonus. That is
friendlier, but it blurs "the hole powers the modules".

### B-47 Kerr redesign: closing the hole mid-recipe pauses the modules, it does not void them [Kerr redesign, spec only]

**Context.** A hole can close while a module is mid-recipe: a Collapser, evaporation when the bolts run out, the
containment power failing, the KEE unforming, or the KEE's chunk unloading.

**What the spec says** (§2.3). The Godforge pattern: an unlinked module skips its post-tick. Progress and EU stop,
and the recipe resumes when the link returns. Parallel and speed were fixed at recipe start, so changing the mass
mid-recipe cannot be exploited. The GUI's Collapse button takes two clicks, and the first warns how many modules
will pause.

**Suggestion.** As specced.

**Alternatives.**

- Void the running recipe, as the BHC does when its hole goes unstable. That is harsher, and it punishes running out
  of bolts twice.
- Let the running recipe finish and block only new starts.

### B-48 Kerr redesign: the KEE draws containment power while the hole is open [Kerr redesign, spec only, PROPOSED number]

**Context.** The KEE no longer runs recipes, so its two energy sockets and the GUI's power line would mean nothing
(today "the hole draws no EU").

**What the spec says** (§3). The hole draws 8,388,608 EU/t (1 A UEV) while it is open, and nothing while it is
closed. Without that power the governor fires no bolts, so the hole bleeds down, shows as unstable and evaporates at
2 SM. The modules draw their recipe EU from their **own** energy hatches.

**Suggestion.** As specced; tune the number in play.

**Alternatives.**

- 0 EU/t: delete the KEE's energy requirement, so the GUI shows no power line.
- Upkeep that scales with mass, for example 1 A UEV × M / 16.
- Modules fed from the KEE's hatches (the Space Elevator pattern) instead of their own.

### B-49 Kerr redesign: a whole-number setpoint, GUI open/close buttons, and the extractor controller moves [Kerr redesign, spec only]

**Context.** You asked for "a neat GUI" and no screwdriver.

**What the spec says** (§4, §6).

- **Setpoint.** The 9-rung screwdriver ladder (2 … 512) becomes any whole number of SM: 2-64 at T1, 2-512 at T2.
  It is set with a number field, a slider and −/+ buttons.
- **Open and close.** Open and Collapse are GUI buttons. They are enabled only when a Seed or a Collapser is in a
  bolt bus, and Collapse takes a second click to confirm. An "Auto" toggle (default on) keeps today's behaviour, so
  automation still works: a Seed or Collapser dropped into a bolt bus acts on its own.
- **Extractor controller.** The module's core cell today sits on the arc's inner face, facing the hole, where a
  player can hardly reach it. The Extractor Module's controller therefore moves to the arc's outer face at 340°. The
  old cell becomes an I/O socket, and two more sockets are added beside the new controller. The compressor's core
  cell is already on the outer face, so its controller stays in that cell.

**Suggestion.** As specced.

**Alternative.** Keep the ladder as the only values the slider stops on.

### B-50 Kerr redesign: modules cost the hole nothing; render scale and migration [Kerr redesign, spec only]

**What the spec says.**

- **Module cost.** None: a running module never changes the mass, the bleed or the stability (§3).
- **Render** (§5). It reuses GT's black hole renderer through a GL scale around the hole centre: s = clamp(4 ·
  (M / 16)^(1/3), 2, 6.5). That gives a 5.2-block horizon at 16 SM, the mockup's size, and it is capped so the disc
  stays inside the mirror ring. GT's lasers are off, because they run along the axes and the rails sit at 45°. It is
  toggled by the `renderKerrHole` config and a per-machine GUI button.
- **Migration** (§7). The KEE converts legacy core blocks into module controllers once, automatically. The
  fallback is `/btskerr migrate`. SL-K02 and SL-K03 now output the controllers, at an unchanged cost, and a
  shapeless recipe turns old core blocks into controllers.

**Suggestion.** As specced.

**Alternative for the module cost.** Each running module adds a share of bleed, for example +10 % each, so the hole
"feels" the load and a busy Kerr burns more bolts.

### B-51 Kerr redesign: the compressor module's controller sits below the T2 band, not in the old core cell [Kerr redesign, built]

**Context.** The spec (§6.1) put the Kerr Compressor Module's controller in the old core cell (the mockup's
"control" block on the south arc's outer face) and asked the exporter to assert that every module controller's
front face is open air in a full build. That assert fails at Kerr T2: the T2 reinforcement band (Harmonic Lining,
Bastion and Casimir plates at z −41..−44, y −2..0) closes the one-block gap in front of the old core cell, so a
player cannot reach the controller once the Density Upgrade is in.

**What is built.** The exporter puts the controller in the arc's outer shell cell straight below the old core cell,
at the first height whose front cell and the cell above it are open air in the T2 build (voxel (0, −4, −39)). The
old core cell becomes a seventh `c` I/O socket. The front-face assert now passes for both modules at T1 and T2. The
extractor's controller is where the spec put it (outermost cell on the 340° ray, voxel (36, 0, −13)). Migration
uses the exported `legacyCoreRel`, so it swaps the old core cell to casing and places the controller in the new cell.

**Suggestion.** As built.

**Alternatives.** Keep the old cell and carve a 1 × 1 access tunnel through the T2 band (4 blocks deep, beyond
survival reach), or move the controller above the band (y +4).

### B-52 Kerr redesign: how pausing, the power switch and the KEE's GUI work [Kerr redesign, built]

**What is built, where it differs from the spec.**

- **Pausing.** The spec copied Godforge: skip the whole post-tick while unlinked. Built instead: an unlinked module
  skips only GT's `runMachine` (no progress, no EU, no new recipe, the running recipe is kept). It still walks its
  own structure, so a module forms, and its GUI says why it waits, before any KEE exists. Godforge's version never
  forms an unconnected module.
- **The link check** is a full round trip: the module computes the KEE cell from its own facing (`keeRel`), and the
  KEE there must compute this module's cell back (`rel`) with the slot's facing (`facingAbc`). The manifest also
  carries `turns`, but only as documentation.
- **The KEE's power switch does not touch the hole** (shown greyed out). GT switches a machine off whenever its
  structure breaks, so if "off" stopped the governor, a cage repaired after one broken block would keep bleeding
  until someone switched it back on. Collapse is the way to shut a hole down; containment power still matters.
- **No power panel on the KEE.** GT's power panel holds the max-parallel field and the power-fail toggle, which mean
  nothing on a machine that runs no recipes, so the KEE has no power-panel button. The modules keep it.
- **Stability** counts as 100 % while the mass is at or above the setpoint (the governor is holding, or the hole is
  being let down to a lower setpoint). Below the setpoint it is mass fed ÷ mass bled over the last second.
- **The setpoint field** accepts 2-512 on the client; the server clamps to 64 at T1. (Superseded by B-54: the
  field, slider and - / + now follow the synced tier range; the server still clamps.)

**Suggestion.** As built.

### B-53 Kerr redesign: what the render pass still has to do, and the new recipe ids [Kerr redesign, built]

**Render.** This pass built only the per-machine switch: the KEE saves `btsKerrRender` (default on), and the GUI has
the eye toggle in the gap row. The render block, `TileKerrHole`, `KerrHoleRenderer`, `Config.renderKerrHole` and
the placement calls are the render pass's (spec §5, work plan R4). Its hooks are `MTEKerrEngine.isRenderEnabled()`,
`isHoleOpen()`, `mass()`, `getStability()` and `KerrSlot.centreRel()` (the hole centre, from the manifest).

**Recipe ids.** SL-K02 / SL-K03 keep their ids and inputs and now output the module controllers. The legacy-core →
controller shapeless recipes are **SL-K07** (compressor) and **SL-K08** (extractor); SL-K06 stays retired. The two
legacy core metas (`slCasing2:0/1`) stay registered and are hidden from NEI and the creative tab.

**Migration.** The KEE converts legacy core blocks once per load, from 40 ticks in, as soon as its slot cells are loaded;
`/btskerr migrate [x y z]` repeats it on demand. A controller cell is replaced only if it is air or the plain arc
shell. The old in-flight recipe goes to the module it ran on (the saved `btsKerrBoundMode`, else the old machine
mode); items that do not fit drop at the module controller, and fluids that do not fit are lost. The test bench
`/btskerr legacy x y z` turns a built Kerr back into the old layout so this can be exercised.

**Dev-server results (headless `runServer`, RC-2, 2026-10-12).**

- `/btskerr t1` in all four facings and `/btskerr t2` north: the KEE and both modules FORM; each module controller
  lands in its slot facing out of the cage; the extractor stands on the viewer's right (the mockup's side); creative
  re-projection over every machine breaks nothing; the KEE's damage-and-repair test passes. After the hole opens
  both modules report "Linked" (parallel 448, speed ×4.0 at 16 SM with creative UMV 16 A energy), also after their
  chunks unload and reload (nothing about the link is saved).
- Survival path (`survival`): KEE 13,898 blocks in 71 rounds, compressor arc 964 in 6, extractor arc 498 in 4, all
  finished (-1) and formed.
- Migration: `legacy` → `migrate` converts both arcs, and the modules form on their existing hatches and link. A Kerr
  left in the legacy layout across a server restart converts itself (both arcs) about two seconds after load.
- Not yet run: a module recipe actually completing, pausing mid-recipe on Collapse, two KEEs side by side, the
  T2 DENSITY recipe, the GUI in a client, the NEI preview (work plan R5).
- Test-bench only: removing a console-placed (ownerless) GT multiblock can throw inside GT's power-fail tracker
  (`UsernameCache` with a null UUID). Player-placed machines have owners; `legacy` gives a stand-in owner and
  carries on.

**Suggestion.** As built.

### B-54 Kerr redesign: KEE GUI details the spec and mockup left open [Kerr redesign, built]

**Context.** The KEE console (`MTEKerrEngineGui`) was redone to match the mockup (`kerr-gui.html`) and spec §4.1;
the module screen gained status lines (§4.2). A few details were judgement calls.

**What is built.**

- **Setpoint range follows the tier live.** The gap-row field, the panel's field, the slider and the − / + buttons
  are all bounded by the synced maximum (64 at T1, 512 at T2). The slider runs 0..1 over the *current* range and
  snaps to whole SM, so it follows a Density Upgrade going in while the panel is open (MUI2's slider bounds are
  fixed at build time). Every write still goes through `MTEKerrEngine.setSetpoint`, which clamps on the server.
  Field scroll steps: 1, shift 8, alt 64 (GT's compressed-bus idiom); right-click on − / + steps by 8.
- **Collapse confirmation** is client-side: the first click arms it for 4 s (label "Confirm", and the line under the
  buttons turns red with "N linked modules will pause"); a second click inside that window sends the server action,
  which re-checks the open hole and the Collapser in a bolt bus.
- **Auto** is a check box (GT's checkmark / cross overlays) rather than a text button, as in the mockup.
- **Mass bar colour** is the BHC lavender, not a stability colour; stability has its own coloured line.
- **One side panel at a time.** Hole control and Modules open in the same spot to the right of the main panel, so
  opening one closes the other.
- **Modules panel rows** show the module controller's item icon, its state, then either the hole bonus (linked) or a
  short reason (paused: why; not built: "place its controller at marker N"; not formed / not loaded / not linked).
  The mockup's "IDLE: no input" reason is not shown: the KEE does not read a module's last recipe result.
- **Module screen:** besides the link and bonus lines, a live recipe-gate line (`gateLine()`; the compressor shows
  "Densification: needs Kerr T2 / needs 128 SM / available", also on the scanner) and a constant maps line
  (`mapsLine()`).
- **Text is English in code**, like the rest of the Kerr GUIs and the scanner lines. Only GT-looked-up ids (recipe
  results, structure errors) have lang keys.
- **Not verified in a client.** It compiles against RC-2; nobody has opened it in a game client yet (R5).

**Suggestion.** As built; move the GUI strings to `en_US.lang` when the Kerr text settles.

**Alternatives.** Rebuild the slider when the tier changes (more code, same result); a server-side Collapse
confirmation (needs per-player state for no gain, because the server re-checks anyway).

### B-55 Kerr redesign: the hole render as built [Kerr redesign, built]

**Context.** Spec §5 / work plan R4: a black hole at the KEE's centre, reusing GT's Black Hole Compressor render
(`BlackholeRenderer`, `RenderingTileEntityBlackhole`, `BlockRenderer`), sized for the cage and scaled by mass,
shown only while the hole is open, with no orphan render blocks. The judgement calls:

**What is built.**

- **GT's classes, three small ones of ours.** `TileKerrHole extends RenderingTileEntityBlackhole` (GT's growth /
  collapse animation, stability input and description-packet sync, plus a synced scale and the owning KEE's
  coordinates); `BlockKerrHole extends BlockRenderer` (GT's render block, registered by its constructor as
  `beyondthestars:gt.beyondthestars.kerrHole`, no item in NEI or the creative tab); `KerrHoleRenderer` (client) wraps
  GT's `BlackholeRenderer` and `glScalef`s it about the centre, so GT's shaders and model are used unchanged. GT's
  lasers stay off (spec: they would cut through the arcs).
- **Server places, server removes** (as GT's BHC). The KEE puts the block at `machines.kerr.centre.rel` (0, 0, 38)
  through its own facing, the same cell for T1 and T2 (checked in both shapes: a don't-care cell, nearest solid
  block 12.3 away, a rail tip). Only into air, or it adopts our own block already standing there; otherwise the GUI
  terminal and the scanner say "Render blocked at x, y, z". It never loads a chunk.
- **When it shows.** Hole open AND the KEE's GUI switch AND `Config.renderKerrHole` (default true; the server
  places nothing when it is off, a client with it off draws nothing). Formed or not (a frozen hole is still a hole).
  When any of the three goes false, GT's 40-tick collapse plays and the block goes after it (the spec said this only
  for a close; a switch turning off does the same, which looks better than a pop). Re-opening during the collapse
  regrows from zero (GT's animation has no mid-point start).
- **Scale** `clamp(4 (M/16)^(1/3), 2, 6.5)` as specced (16 SM → 4.0, 64 SM → 6.36, 128 SM and up → 6.5).
  **Stability**: GT's input is the governor's coverage / 2, so the shader's stability equals the coverage (calm at
  full coverage, the disc shrinks below it, spins at 0, e.g. with no containment power). Both are pushed only when
  the scale moves 2 % or the coverage 0.02, checked once a second.
- **No orphans, two ways.** (1) The KEE removes the block on break (`onBlockDestroyed`), on any tile removal
  (`onRemoval`, wrench), when its facing no longer points at that cell, and on load if the hole or a switch says it
  should not exist. Its position is saved in the TILE NBT only, never on the wrenched item (trap 2), so a KEE placed
  elsewhere never claims a block here. (2) **Added beyond the spec:** the render tile polices itself. Every 100 ticks
  on the server it asks the owner's cell (only once that chunk is loaded) whether a KEE there still claims exactly
  this cell, and turns itself to air if not. That covers what (1) cannot: a KEE broken while the centre's chunk was
  unloaded, a world edit, a `/setblock`-placed ownerless block.
- **Test bench** `/btskerr render x y z [open [m]|close|off|on|break]`: acts, then reports the hole, both switches,
  the centre cell and every Kerr render block within 100 blocks.

**Verified (headless `runServer`, RC-2, 2026-10-12).** T1 north, east, south and T2 west: the block lands on the
centre cell, claimed, scale 4.01 at 16 SM, 6.36 at 64 SM, 2.32 at 3 SM, 6.50 at 128 SM (T2). Close: the block stays
for the 40-tick collapse, then air. Render switch off: same; on again: placed again. Breaking the controller: the
block is gone in the same tick. Two `/setblock` ownerless render blocks next to a running KEE: gone within 100 ticks,
the KEE's own block kept. Server restart with two open holes: each KEE adopts its block (one block each, still
claimed after the tile's 100-tick self-check). **Not verified:** the look in a game client (size against the cage,
the disc against the rails, Angelica's core profile), R5. Dev-server note: with no player online, Forge stops ticking
tile entities 1,200 ticks after the last player left (or the start), so console tests must finish within a minute
of start or a player must be online.

**Suggestion.** As built; look at it in a client. If the disc at the 6.5 cap looks crowded against the rail tips
(12.3 from the centre; the disc reaches about 13.5), lower the cap to 5.8 (disc about 12).

**Alternatives.** A client-only render with no block (a world-render hook keyed on the KEE: no block to orphan, but
the KEE controller's chunk must be loaded for the client to know about the hole, and it means our own render
dispatch instead of GT's TESR path); GT's `GregTechAPI.sBlackholeRender` block itself (no way to scale it or tag its
owner).

### B-56 Kerr redesign: module recipes on a dev server, and SL-K05 lost to the Eternal Singularity recipe [Kerr redesign, R5 test]

**Context.** R5 dev-server pass (headless `runServer`, RC-2, 2026-10-12): KEE T1 and both modules formed in two
facings through both paths, module recipes driven from the console, the hole closed and the KEE unformed mid-recipe,
T2 with DENSITY, render cell checks and two restarts. New test bench: `/btskerr module x y z <c|x>
stock|status|tier|clear`, `/btskerr damage|repair x y z`, `/btskerr keep x y z [t2]` / `release`.

**Found and fixed: SL-K05 never ran.** The coremod's neutronium-compressor recipe turns 72 L of molten SpaceTime alone
into an Eternal Singularity (UMV). SL-K05 also takes molten SpaceTime, and the compressor module searched DENSITY
last, so a Kerr T2 at 128 SM fed bolts + RHSP + molten SpaceTime made Eternal Singularities batch after batch and
never touched the bolts. **Built:** DENSITY is searched first. With the gate open (T2, 128 SM) SL-K05 ran (64 bolts →
1,000 L Hyperdense Stellar Mixture); with the gate shut (T2 at 64 SM) the bolts stayed untouched and the module fell
through to GT's maps. The creative kit's compressor module also gets a second input hatch, because SL-K05 takes two
fluids and a GT input hatch holds one.

**Results.**

- **Formed:** T1 north and east on the creative path, T1 east and north on the survival path (KEE 13,898 blocks in 71
  rounds, compressor arc 964 in 6, extractor arc 498 in 4), and T2 south and west. Every module formed and linked,
  the extractor stood on the viewer's right, re-projection broke nothing, and damage-and-repair passed each time.
- **Hole closed:** both modules sat in `HOLE_CLOSED` with their inputs untouched, through a restart too. After Open,
  the extractor ran (128 Stemcells → 128 Plasma Membrane at parallel 448, ×4) and so did the compressor (Fusion
  Control Computer Mk-III → Hypervisor Matrix, 14 in parallel, and Poppy → Plantball).
- **Closed mid-recipe** (B-47): progress froze (379/2066), the recipe was kept, the machine went idle, and it resumed
  on reopen. The module notices on its next 20-tick link check: it ran about 14 more ticks after the close.
- **KEE unformed mid-recipe** (one cage block removed): both modules went `KEE_NOT_FORMED`. The compressor's progress
  froze, a freshly stocked extractor did not start, and the hole stayed frozen at 16 SM with no bleed. After the
  repair, both resumed and the recipe finished, with the outputs in the bus.
- **Render:** placed on open (and on the auto seed), gone after the 40-tick collapse on close, gone at once when a
  loaded KEE broke, and gone after a restart for a KEE broken while its centre chunk was unloaded (the tile's
  self-check). A closed hole has no block after a restart; each open hole adopts its one block. **While the KEE is
  unformed the render stays,** as built in B-55 (a frozen hole is still a hole). The R5 task list expected it to go
  on unform. That is your call.
- **Restart:** hole state, mass, setpoint, T2 and in-flight module recipes all persisted (a compressor batch went
  644 → 725 across the restart and finished).
- **Cosmetic, not changed:** the scanner bonus line shows `p112 x1.00` while the hole is closed (the GUI says
  "none"), and GT's `lastParallel` reads 0 until a reload.
- **Test-bench note:** a playerless dev server unloads a Kerr's chunks within seconds of the last command, so
  console tests must force-load them with `keep`.
- **Not run here:** the GUIs and the render's look in a game client, and the NEI preview.

**Suggestion.** Keep DENSITY first. Decide whether the render should go when the KEE unforms. If yes, it is one
condition in `MTEKerrEngine.tickRender` (`want` gains `&& mMachine`).

---

### B-57 Kerr redesign: review fixes (recipe-check pushes, DENSITY cache, migration hold, no KEE maintenance, Collapse button, Auto) [Kerr redesign, review fix]

**Context.** A review of the Kerr redesign against GT 5.09.54.205 (RC-2) sources found six issues. All six were
checked against the RC-2 sources and fixed. They were re-tested on the headless dev server (2026-10-12) without the
`stock` command where a refill would hide the bug.

**What was fixed.**

- **Idle modules never restarted.** RC-2 checks recipes only on a push (`doPeriodicChecks` is false). A check that
  failed with `HOLE_CLOSED`, `KEE_NOT_FORMED` or `density` used the pending push up. The module now pushes
  `RecipeCheckReason.IMMEDIATE` when its link becomes `LINKED`, and the compressor pushes one when its DENSITY gate
  opens (polled every 20 ticks). *Test:* the hole was closed and the extractor stocked in the same tick, so the
  module stayed idle with `HOLE_CLOSED` and 128 Stemcells. The hole was then reopened and nothing else was touched:
  128 Plasma Membrane came out. Same with the KEE unformed (`damage`), the compressor stocked with Poppy, then
  `repair`: 8 Plantball.
- **DENSITY first did not hold.** GT offers `ProcessingLogic.lastRecipe` to every map query ahead of the map's own
  recipes, and never clears it. The last recipe is now offered only to its own map (`GTRecipe.getRecipeCategory()
  .recipeMap`). A DENSITY match also hides GT's maps (`shadowsLaterMaps`), so a shut gate waits with "density"
  instead of the Eternal Singularity recipe eating the SpaceTime. *Test (T2, no restart):* SL-K05 stocked at 64 SM:
  the bolts and SpaceTime stayed untouched. Raised to 128 SM with nothing restocked: SL-K05 ran. The bolts were then
  cleared, so an Eternal Singularity batch ran and became the cached recipe. After restocking the bolts, the next
  recipe was SL-K05 (2,000 L Hyperdense Stellar Mixture in total).
- **Migration destroyed in-flight recipes.** Old arcs had no energy hatch, so a converted module cannot form, and
  every fluid was counted lost. The KEE now moves the old outputs off GT's recipe fields at load (GT's `stopMachine`
  clears those while unformed). It holds them in its NBT (`btsKerrLegacy`, tile and wrenched item) and hands them over
  each second once the module is formed. Items that do not fit drop at the module controller. Fluids that do not fit
  stay held, and the GUI note says so. *Test:* `/btskerr legacy x y z inflight` (strips the module energy hatches,
  puts 2 x 64 bolts, 1,000 L water and 500 L lava in the KEE's GT recipe fields) followed by a restart. The load
  captured the recipe, auto-migration converted both arcs, and the note said "held until the Compressor module forms".
  After `/btskerr module x y z c energy`, water went to the output hatch, lava stayed held, and it was still held after
  another restart. All 128 bolts were found as drops: the test layout has no output bus, because `legacy` puts the
  core block back on the I/O socket. Same result on a T2 without a restart.
- **KEE maintenance.** The KEE runs no recipes. `getDefaultHasMaintenanceChecks` is now false and the
  maintenance-hatch requirement is gone. The governor arc's `m` cell is shell first: builds place casing and there is
  no hint. It still accepts the maintenance hatch an existing Kerr has there, so nothing unforms. *Test:* the kit's
  KEE formed with 10 hatches and no maintenance hatch, in T1 and T2. The dev server also caught a crash in the first
  version: GT's `HatchElementBuilder.build()` requires a hint.
- **Collapse on an unformed KEE.** The button now needs a formed cage, as the server does. The status line says
  "Collapse needs a formed cage". GUI only; not seen in a client.

**Judgement call: Auto (finding 6).** With Auto on (the default), a Collapser placed in a bolt bus collapses the hole
within 20 ticks, so the panel's two-click Collapse and its Collapser count are moot while Auto is on. B-54 built Auto
as "a Seed or Collapser placed in a bolt bus acts on its own" (migration.md promises the old behaviour), so
**built:** the behaviour is unchanged. With Auto on, the panel now says "Auto on: a Collapser in a bolt bus collapses
at once", and the Auto tooltip says the same. **Suggestion:** Auto acts on Seeds only (evaporation recovery keeps
working) and Collapsers are left to the button. Alternatives: split into "Auto open" / "Auto collapse" with collapse
off by default, or keep as built.

**Seen, not changed.**

- A migrated module controller stands "switched off". GT disables any multiblock that is allowed to work while
  unformed, so after adding its energy hatch the player switches it on (standard GT behaviour).
- SL-K05 inputs that are short (for example 10 bolts instead of 64) do not match DENSITY, so they still fall through
  and the Eternal Singularity recipe can use the SpaceTime. That is the inputs' ambiguity, not the gate.

---

### B-58 Merging kerr-redesign into master: renumbered questions, config save, dead Kerr lang keys [merge]

**What was done.** `kerr-redesign` (hole-only KEE, the two module machines, the KEE's MUI2 GUI, the black-hole render)
is merged into master, which carries the RC-2 port and D-49 units 1-6.

- **Question numbers.** Both branches had used B-33..B-44. Master keeps B-33..B-38 and B-45; the branch's twelve
  became **B-46..B-57** (old n -> n+13), moved after B-45, with every reference in docs and code comments rewritten
  (only lines that came from the branch were touched, so master's B-34..B-38 references still point at D-49). The
  table is under "Where the old question numbers went". Branch commit messages keep the old numbers.
- **Config.** Both options are kept (`eyeRenderCentre`, `renderKerrHole`). Master's "always save" wins over the
  branch's "save if changed", so the `renderKerrHole` comment is also rewritten on every start.
- **Client and block registration.** Both renders are bound (TecTech-shader star for the Eye, GT's BlackholeRenderer
  for the KEE) and both render blocks registered (`eyeStar`, `kerrHole`).
- **Structures.** `manifest.json` was regenerated by `tools/export-structures.js` (same voxel.js sha on both sides).
  The result is the union: Kerr machines, legend, module pieces and the `kerr_slots` table equal the branch's; the
  Eye's machine and its pieces including the T5 `eye_arms2` and T6 `eye_rim` bands equal master's. `--check` is clean.
- **Lang (RC-2 recipe_result).** The module results (`beyondthestars.kerr.module.<holeclosed|keenotformed|
  keenotloaded|wrongfacing|nokee|density>`) already had their `GT5U.gui.text.recipe_result.` keys on the branch.
  Master's f484699 had added recipe_result keys for the old KEE's `kerr.nomodule`, `kerr.modeunavailable` and
  `kerr.lowmass`; no code produces those ids after the redesign and their text tells the player to use a
  screwdriver, so they are removed. Every literal and generated `ofFailure` / `ofSuccess` id in the merged code has
  its recipe_result key.
- **Checks.** `spotlessApply compileJava` passes; `node tools/fieldsim-golden.js --check --java` 78/78 PASS. No
  in-game run was part of the merge.

**Left as it was.** SL-K04's old non-BEC path stays for the BEC step. The design repo's
`designs/docs/bec-expansion-proposal.md` still says "start at B-41" for its questions; the next free number is now
**B-59**.

**Suggestion.** Nothing to decide unless you want the Kerr questions to keep their branch numbers (that would mean
renumbering master's D-49 entries instead, which the brief and PROPOSAL already cite as B-34 / B-37).

---

### B-59 Final integration: the RC-2 dev-server test of master, and ruling 11a found missing [final integration test]

**What was run.** `runServer` (RC-2 deps, fresh flat world `sltest-final`, stdin console), the static lang lint and
`node tools/fieldsim-golden.js --check --java` on master `5efdfb6` and again after the fixes below.

- **Load.** `Recipe registration complete: 245 registered, 0 skipped` (243 after SL-P27/P28 retired); TecTech Eye
  recipes removed (43 + controller); 10 BHC->Kerr swap lines (Empty Spatially Compressed Circuit Array + the 9
  compression generator tiers, all `gt.recipe.assemble-condensate`); BHC controller recipe removed; 13 research items
  unique. No `[BTS]` warning or error; the GT load errors in the log are the pack's own (absent mods), as in earlier runs.
- **Lang lint.** All 18 literal `ofFailure` ids plus the generated `eye.tier1-6`, `eye.locked2-6`,
  `eye.compression1-6` and `kerr.module.<link>` ids have `GT5U.gui.text.recipe_result.` keys; the five shutdown reasons
  have `GT5U.gui.text.shutdown_reason.` keys; every `StructureErrors.of`, `translateToLocal`, recipe-map name, item and
  casing meta has its key. Nothing was missing.
- **Golden.** 78/78 PASS, the committed JSON is up to date.
- **Kerr.** T1 north (creative), T1 east (survival), T2 south (creative), T2 west (survival): KEE and both modules FORMED,
  re-projection keeps them formed, damage/repair works. Modules stay idle with the hole closed and run when it opens
  (extractor: GT item extractor recipes; compressor: GT compressor recipes at parallel 7, x4 speed). Closing the hole and
  unforming the KEE mid-recipe pause the compressor (progress kept) and it resumes. T2 DENSITY waits at 64 SM and runs at
  130 SM (SL-K05: 1,000 L Hyperdense Stellar Mixture). Render: one claimed block at the centre, cleared on close, on the
  off switch, while unformed and when the controller is broken; no orphans. A `legacy inflight` Kerr migrates (2 modules,
  the old recipe handed to the compressor, a fluid held in the KEE). After a restart the hole, render and running recipe
  are as before.
- **Eye.** T1-T6 each in two facings, creative and survival: all FORMED with the expected bands and compression tier.
  Ignition by a charge (protostar render, then lit), upkeep starvation (r 0, P sags, Sol lost to a Breach, failure line
  shown, render cleared), sensor-hatch regulation (bang-bang on P through the Field sockets keeps P in about -2..+9; with
  no regulation P drifts to +30), Mass Infusion, Shell Ignition, a failed Core Collapse (SpaceTime 0: SUPERNOVA, core
  kept in escrow, the reason stays in "Last failure", supernova flash render then air) and a successful one (T4 unlocked,
  neutron star render), First Cooling and T5 batches (Black Dwarf Matter per batch, milestones count good batches), the
  Magnetar Merger, the Agent upkeep at 3,000 L per 60 s cycle with the rig's Agent trim charging the shield (starved:
  shield 0, magnetar lost to decoherence, re-merge waits for 100,000 L of Agent). *Correction (B-60):* in this build
  the upkeep did NOT charge the shield; only the trim did. Ruling 12 (the upkeep charges it) is built in B-60. Unforming and breaking the controller put the star out and clear
  the render; no orphan render block at any of the 13 Eyes' centres. Restart: unlocked tiers, live stars, milestones,
  escrow and last failures are as before. Legacy 0.24.x NBT migrates (galactic -> unlocked T4; plain -> bands
  grandfathered).
- **BEC.** The server has 111 Condensate Assembler recipes. Ours: SL-P39 (WDM-free Resonance Chamber II) and SL-P33
  (BDM-free Resonance Chamber III) are in the map (`/btseye gates`: "a clean one exists" for both chambers, gates CLEAN
  for Eye T1-T7), and the RC-2 Spatial / Temporal casing recipes now output 32. Nothing can be crafted on the console:
  a Condensate Assembler needs a whole BEC network (generator, containment field, nanites), which the bench does not
  build.

**Fixed here.**
1. **Ruling 11a was not on master** (no branch had it): `BTSRecipesBEC` now rewrites RC-2's Condensate Assembler
   recipes for TecTech's Reinforced Spatial / Temporal Structure Casing to x32 at load-complete (SL-B01/B02: logged per
   hit, a WARN for an unexpected old size or several matches, idempotent, whatever the flags say) and SL-P27/P28 are
   retired. Verified: `SL-B01 ... now outputs 32, was 4`, same for SL-B02.
2. **Test bench:** `/btseye keep x y z facing [tN]` force-loads an Eye's footprint. Without it the dev server unloads the
   centre cell's chunk between console commands, and several early render checks read "nothing" for a burning star
   (a test artifact: with the chunks kept, every render check matched the star).

**Not done (open).**
- **SL-B03/B04** (the proposal's fallback BEC recipes if RC-2's casing recipe is missing) are not built: a missing
  recipe logs an ERROR saying the shell costs 8x what we intend. *Suggestion:* leave it; RC-2 has the recipe.
  *Correction (B-60):* that ERROR was wrong. RC-2 has no other recipe for these casings (TecTech's research-station
  one is gone), so a missing BEC recipe would make every Eye part uncraftable. SL-B03/B04 are built in B-60.
- **Ruling 11b, the BEC expansion** (6 condensates, 20 Condensate Assembler recipes, 20 retired ids) is still only the
  proposal (`designs/docs/bec-expansion-proposal.md`, PROPOSED). Nothing of it is on master. *Suggestion:* build it as
  its own pass once you have read the proposal; `BTSRecipesBEC` is where it would live.
- **Docs that still describe SL-P27/P28 as live:** `singularity-ladder-costs.md` §6/§9.5 cost rows (the build spec is
  updated). *Suggestion:* refresh the cost table with the x32 numbers from the proposal §3 in the BEC pass. *Done in
  B-60* (costs §3.2, §4.1, §5, §6 #3, §9.5).

**Small things seen (no change).** The module bench line prints `lastParallel 0` while a recipe runs (it shows GT's
field before GT fills it; after a restart it read 17). A legacy-layout Eye (`legacy` build) has no input bus, so it
cannot take a Stellar Ignition Charge until the player puts one on a plinth gas socket (`migration.md` already says so). A target change that puts a star out is recorded as the "Last failure" (by design: "the last way a star
was lost or put out"), so it replaces an older, more useful failure line.

### B-60 Final integration fixes: the Agent upkeep charges the shield, a BEC fallback for the Eye casings, the Kerr render on load [final integration fix]

**Context.** A review of the final-integration build found four things; all four were real and are fixed here.

**What was built, and the judgement calls.**

1. **Ruling 12: the Antipolarization Agent upkeep charges the T6 flare shield.** Built in the order the ruling sets:
   the prototype engine first (`designs/star-lifecycle-prototype.html`, design repo), then the fixture
   (`tools/fixtures/star-lifecycle-engine.js`, re-pinned), the golden file, and `StellarField.step`:
   `A <- min(100, 0.95·A + 4·u_agent/p + 2·r)`. `node tools/fieldsim-golden.js --check --java`: 78/78 PASS, bit
   for bit. **PROPOSED number: `SHIELD_PER_UPKEEP` = 2.** 3,000 L per 60 s cycle is 50 L/s = 0.5 Agent unit (100 L)
   per second; at the trim's 4 A per unit that gives 2 A/s. r is the cycle's delivered fraction (EU and every upkeep
   fluid, so for T6 the Agent), so a starved magnetar's shield decays. With no trim the shield settles at 40, which
   absorbs about 40 of a giant flare's 100 (25/s for 4 s) and keeps Π's noise near 0. Flares still need the Agent
   trim in a Field socket. The design-invariant check still counts only the trim (r may be 0). The Eye tooltip, the
   Magnetar chart tooltip, the NEI eyefield line, the T6 trim note and the costs doc §10 say so.
   - *Alternatives:* a bigger K (K = 5 settles at 100 and would absorb a whole flare with no trim, which makes the
     trim pointless), or scale the shield with litres actually drained per second instead of r (the upkeep is
     taken once per cycle, so that would need a per-second upkeep drain).
2. **The WDM 1,800 + BDM 450 L dwarf-matter top-up stays.** "One fluid runs the whole magnetar" could be read as
   dropping it, but your 2026-10-12 answer says "the dwarf-matter merger spawns the star **and tops it up**", and
   the 2026-10-13 ruling only makes the Agent do the upkeep and the shield. So the per-cycle running cost is one
   fluid (Agent) and the dwarf matter is the merger charge plus a top-up every 50 cycles.
   - *Alternative:* delete the reserve (`dwarfReserve`, `TOPUP_*`, its NEI and bench lines); then a magnetar needs
     dwarf matter only for the merger and a re-merge.
3. **The Eye casings can no longer become uncraftable.** RC-2's Condensate Assembler recipe is the only recipe that
   makes TecTech's Reinforced Spatial / Temporal Structure Casing (the coremod and Godforge only consume them;
   TecTech's research-station recipe is gone in RC-2). `BTSRecipesBEC` now matches a recipe on any output slot and
   rewrites that slot. If nothing matches: **SL-B03 / SL-B04** register RC-2's own recipe at ×32 (same 12 inputs,
   nanite tiers 4,1,1,2,1,1,4,1,1,1,1,2, Neutronium / Cosmic Neutronium / Bedrockium / Space or Time condensate,
   40,000 s UMV), every input through `req` / `nn`. If even that is skipped (an input missing from the pack), the old
   SL-P27 / SL-P28 Assembler bodies (×4) come back as a last resort with an ERROR. In RC-2 as shipped neither
   fallback runs. The wrong "falls back to TecTech's research-station recipe at 8x" ERROR and javadoc are gone.
4. **The Kerr's hole render no longer collapses and regrows on every load.** `tickRender` now waits for GT's first
   structure check (`mStartUpCheck`, as the Eye's centre render does). Before, at tick 0 `mMachine` was still
   false, so a formed Kerr with an open hole started GT's collapse, removed its block at tick 40 and placed a new one
   at tick 100.
5. **Costs doc** (the fourth finding): §3.2, a new §4.1 (Eye T1 14,936 Spatial + Temporal casings = 467 BEC crafts =
   14,936 Godforge casings, 22,404 Neutronium nanites, 239k ingots each of Neutronium / Cosmic Neutronium condensate,
   4.7e16 EU and 5,186 BEC hours; complete Eye T4 15,851 casings = 5.7 Godforge main rings), §5, §6 #3 and §9.5 now
   describe the ×32 BEC recipe. That is about 1.3× TecTech's stock RC-2 shell (11,456 Godforge casings, 3.6e16 EU).

**Dev-server results (RC-2, this build).** Load: `SL-B01 ... now outputs 32, was 4` and the same for SL-B02; no
SL-B03/B04 or last-resort line (RC-2 has the recipe). Kerr: after a restart, `/btskerr keep` on the T2 Kerr with an
open 130 SM hole, then `/btskerr render` every second for 11 s: the one claimed render block stood the whole time
(scale 6.5; the old code removed it at tick 40 and replaced it at tick 100). Eye T6: magnetar lit (debug), rig off (no
trims at all), r 1: after 30 s Π 0.0 and shield A 18 (it was 0 without trims before this fix); the field itself still
breaches on Θ/Ω without trims, as designed. Golden: 78/78 PASS.

**Suggestion.** Accept 1-5 as built. Re-check K after playing a magnetar: if flares feel unanswerable with the
standard rig, raise K; if the trim never matters, lower it.

### B-61 Package: what the test jar `0.24.1-master.137+175c89997a` contains [final integration package]

**Context.** The package task asked for one jar with the Kerr redesign, the D-49 Eye T1-T6, "the BEC expansion and
x32 structure casings" and the RC-2 lang fix, plus `dist-rc2/INSTALL.md` (design repo) with a dedicated-server
section.

**What was built.** `gradlew build` on master `175c899` (clean tree, spotless + checkstyle pass, nothing to fix). The
jar is `dist-rc2/beyondthestars-0.24.1-master.137+175c89997a.jar`, SHA-256
`6f29c4d7bd87c3d99f6075294f95394a28543327b74803d76b3511734432eab7`; the older `master.120` jar moved to
`dist-rc2/superseded/`. The judgement calls:

1. **The BEC expansion (ruling 11b) is not in the jar.** Master has only ruling 11a (SL-B01/B02 x32, SL-B03/B04
   fallback); the expansion is still the proposal (B-59). INSTALL.md says so plainly instead of listing it as
   shipped. *Suggestion:* build 11b as its own pass, then package again.
2. **Unmerged branches are not in the jar:** `questbook`, `homogeny-reality`, `tidy-old-multis`. The package pass
   does not merge (only the merge task may). INSTALL.md lists them under "Not in this build".
3. **`SERVER_DIR` stays a placeholder** in the dedicated-server command, with a note to replace it: the server's folder is
   not recorded anywhere in the repos.

**Suggestion.** Accept as is; tell us the dedicated-server folder and it goes into the next INSTALL.md.

---

### B-62 D-49 review fixes: paid parallel, first-cycle reserves, failures, milestones, migration, T4 recovery [D-49 review fix]

**Context.** The D-49 review (15 findings, 8 test problems) was checked again against master after unit 6, the Kerr
merge and the final-integration fixes. Already fixed before this pass: the centre render, the GUI, unit 6's cost table
and build-doc contract (B-45), the persistent "Last failure" line in the GUI and scanner (B-45), ruling 12 (the Agent
upkeep charges the shield, B-60), and INSTALL.md's retired `interimBlackDwarfAtEyeT4` text (B-61). The rest were real
and are fixed here. Numbered after master's B-61; if a branch merged later also used B-62 / B-63, the merge renumbers
as B-58 did.

**What was built, and the judgement calls.**

1. **Parallel paid vs used.** A lit second now runs at `min(live parallel, the cycle's paid parallel)`, and a T5 batch
   also at most the parallel its WDM charge paid for (`cycP`, `batchP`, tile NBT; 0 in an older save = no cap until
   the next charge). Product, HSM feed, trim caps and the field's per-parallel division all use it, and the fracture
   salvage uses the batch's paid parallel. Knocking a band out at the charge and putting it back no longer gives 32x
   the output for 1/32 of the upkeep. The scanner shows "the structure gives N from the next charge".
   - *Call:* a parallel that FALLS mid-cycle takes effect at once (a broken band costs the rest of that cycle's
     extra output). *Alternative:* refund, or keep the paid parallel until the cycle ends (that rewards breaking
     bands).
2. **The new star's first cycle is reserved.** A transition now begins only when the buffers hold its event fluids
   plus the new star's first cycle of upkeep x its parallel (Mass Infusion: + Sirius A's H / He / RSPM; Shell
   Ignition: + Arcturus's; the Core Collapse: + the neutron star's 144 L SpaceTime and one cycle of HSM feed at the
   setpoint; the merger: + 3,000 L Agent). The bounce absorbs SpaceTime only above the neutron star's 144 L x p, so a
   round 60,000 L buffer now absorbs 59 points (the scanner readout says what is kept). The NEI transition page shows
   the same totals at p = 1 (`EyeStarRuntime.transitionNeeds`, one method for both).
   - *Call:* the reserve is ONE cycle, not ignition's ten. *Alternative:* ten cycles (safer, much more buffer).
3. **Failures are kept and logged.** Every failure (star lost, transition failed or aborted, fracture, a First
   Cooling batch below mean Y 0.9, a batch put out) writes the persistent "Last failure" line and is logged at INFO;
   the automatic restart that follows changes only the event line. A plain chart change or a planet selection is the
   player's own choice: it is logged but no longer replaces a more useful failure line (B-59's note).
4. **Milestones count only on unlocked tiers.** The First Cooling batch no longer counts toward the six good batches
   for T5 -> T6, and the Resonant seconds and product of a star whose tier is still locked (the collapse settle, the
   merger imprint, Sirius A during Mass Infusion) do not count.
5. **Legacy NBT.** Schema-less NBT is a 0.24.x save only if it carries GT's tile key `mID`, a 0.24.x Eye key
   (`btsEye*`) or a key GT's `getDrops` writes on a dropped machine (upgrades, colour, covers, strong redstone). An
   anvil-renamed new controller starts at T1 instead of being grandfathered to T3. The bench's `legacy` sets `mID`.
6. **Grandfathering with unloaded chunks.** Every successful walk of a migrating Eye raises its unlocked tier (max,
   never lower); the migration is settled only by a walk that saw every chunk under the core, beam band and jet
   crescent loaded. An old T3 Eye first walked with its band chunks unloaded is no longer stuck at T1.
7. **T4 recovers unattended.** With T4 unlocked, a dark Eye holding the Neutron Star chart relights Arcturus from a
   Stellar Ignition Charge (10 cycles + seed, as a T1-T3 relight); Arcturus goes PENDING and re-runs the Core Collapse
   (no core) once its fluids are buffered. An igniting star whose chart is swapped for the next tier's now finishes
   its ignition and then goes PENDING (before, it was put out).
   - *Call:* each relight spends a charge, as T1-T3 relights do. *Alternative:* make the controller slot automatable
     (still two chart swaps).
8. **Research items.** GT's Scanner consumes the research item, so SL-E04 / E05 / E06 now research from Star Chart:
   Arcturus / Sirius B / SGR 1806-20 (cheap, unique, each made from the product of the tier the core leaves) instead
   of the previous expensive core. No other recipe (on any branch) researches from these charts. B-35 and the plan's
   §10.2 table are annotated.
9. **Salvage while unformed.** A batch put out because the structure broke owes its salvage (kept with the other owed
   outputs, item NBT, survives a wrench) and pays it the next second the Eye is formed; the line says "owed". Before,
   GT had already cleared the hatch lists, so the fluid was voided while the chat said "salvaged".
10. **Sensor analog scaling.** Analog mode maps the zone 0..4 onto 0/4/8/11/15 and the warning and upkeep flags to 15
    when set (they gave 0 or 1). The hatch tooltip says so.
11. **Sensor GUI sync.** The "no reading" value syncs as a sentinel (negative infinity), not NaN, so MUI2 no longer
    resends it every tick.
12. **Ruling 12 (the T6 Agent upkeep charges the shield)** was already complete in B-60: the prototype (design repo
    `feec8dd`), the fixture (re-pinned), the golden file and `StellarField.step` all use `0.95A + 4u/p + 2r`. Checked
    again: `node tools/fieldsim-golden.js --check --java` 78/78 PASS. No change.
13. **Small things.** The "does not reach TN" wait names all three limits (unlocked, bands, compression); it was
    confusing for a migrated jet-without-beam Eye. The Eye tooltip has three new lines (items 1, 2 and 7). Costs §9.4:
    the T5 gate row reads "loop, fixed by SL-P33", the interim-flag text is gone, and the T8 material is named
    Superdense Ergomatter (ruling 17).

**Not changed (design questions).** The merger's imprint band (+60..+90) sits in Strained / Critical and drains
Integrity about 0.6-0.7/s, so an Eye with Stabilisation tier 4 can fail it; the failure line now shows why. Plan test
17's "TierParams invariant check logs OK at boot" does not exist (the check is offline only). The questbook (its own
branch) should mention the T4 relight and the first-cycle reserve.

**Checks.** `spotlessApply compileJava` passes; golden 78/78 PASS. **Not run in-world** in this pass (no dev server):
the paid-parallel cap, the reserves, the T4 relight and the unformed salvage still need a `/btseye star` bench run.

**Suggestion.** Accept 1-13 as built.

---

### B-63 D-49: unlocking, transitions, T5 and T6 [D-49 unit 5]

**Context.** Unit 5 (commits `788e29f`..`8f626f5`) built the five transitions as field overlays, PENDING, success and
failure, the T5 cooling batches, the T6 magnetar, and the NEI transition and field pages. Its judgement calls were
never logged (B-38 item 3); the code cited "PROPOSED (B-38)" for the dwarf-matter numbers, which now cites B-63.

**Judgement calls.**

1. **The T6 dwarf-matter reserve (PROPOSED).** The merger charge fills a reserve of 60 cycles; each cycle spends one;
   at 10 cycles or fewer the Eye tops it up from the buffers with White Dwarf Matter 1,800 + Black Dwarf Matter
   450 L x p for 60 more; an empty reserve makes that cycle's r 0 (no output, P sags). This is the reading of "the
   merger spawns the star and tops it up". B-60 item 2 kept it after ruling 12.
   - *Alternative:* no reserve; dwarf matter only for the merger and a re-merge.
2. **T5 fracture salvage: 25 % of the batch x the fraction done** (plan §6.6), then I 100, lock 0 and the next batch
   (or IDLE). B-62 item 9: owed while unformed.
3. **The Purified Tengam return:** 12 dust per 1,000 L of Agent consumed (upkeep, merger charge and trims) to the ME
   output bus; whole dust out, the rest carried in tile NBT.
4. **PENDING is T1-T3 only.** A lit star with the next tier's chart is PENDING, and the transition begins the moment
   every precondition holds. The First Cooling (T4 -> T5) and the Magnetar Merger (T5 -> T6) start from a dark Eye
   with the Sirius B / Magnetar chart in the slot, because neither has a lit predecessor to transform.
5. **The merger re-spawns automatically** from a dark Eye with the Magnetar chart: every attempt takes the full charge
   (WDM 18,432 + BDM 4,608 + Agent 100,000, x p); no core after the first success.
6. **The First Cooling retries automatically:** a failed First Cooling batch leaves the Eye IDLE, and the next WDM
   charge starts another attempt (each takes a batch charge; the core stays in escrow).
7. **A transition's new star runs at p 1** before its tier is unlocked, if the bands and compression reach it.
   Transitions hold the time dial at m = 1.
8. **A running transition lives in the tile NBT only:** a wrench aborts it (its materials are lost, the core stays in
   escrow), and the escrow cannot be taken back while it runs.
9. **The overlays (plan R-4, R-6).** Mass Infusion: 600 s with P drift +4 (RSPM 5,000,000). Shell Ignition: Θ ramps
   in over 600 s with T3's trims and the ash off, a forced helium flash warned at 585 s and hitting at 600-602 s,
   Arcturus at 603 s. Core Collapse: bounce +150 (B-25; the bench's `set shock 90` reproduces the golden cases).
   Merger: Π drifts +0.5/s with no flares, imprint inside +60..+90 for 120 s within 600 s, release inside ±40 within
   60 s.
10. **Status ids** `star_transition`, `star_batch`, `star_idle` and `star_merger` (with recipe_result keys). The next
    tier's chart is not hidden behind its locked gate, since putting it in is how the tier unlocks.

**Suggestion.** Accept 1-10. Re-check 1 and 6 after playing T5 / T6: with 6, each failed First Cooling spends
18,432 L of WDM without asking.

### B-64 BEC expansion: the Condensate Assembler takes the ladder's parts, cores and charts [BEC expansion, ruling 11b, built]

**Context.** Ruling 11b: "we need way more things in the bose einstein". Built exactly per
designs/docs/bec-expansion-proposal.md (build spec §2.5, costs doc §11): six addon condensates with Condensate
Generator recipes (SL-B10..B15) and 20 Condensate Assembler recipes (SL-B20..B81). SL-B01..B04 (ruling 11a's x32 and
its fallbacks) were already on master and are unchanged. Compile-checked (`spotlessApply compileJava`); not yet booted
on a dev server or seen in NEI (see "Not verified" below).

**Your 14 proposal questions, all taken as recommended:**

| # | Question | Built |
|---|---|---|
| Q-B1 | The cores and the Density Upgrade lose their research steps (BEC recipes have none) | Accepted: SL-E02..E06, SL-K04 and SL-P21's scans are gone |
| Q-B2 | Galactic Shell Plating | SL-P17 (Assembler, HSM: the T4 gate) stays; SL-B62 (x64, needs WDM) is a batch alternative for the T5/T6 bands |
| Q-B3 | Harmonic Lining | SL-B34 (x64) added as an alternative; SL-P12 stays (Kerr T1 is pre-BEC) |
| Q-B4 | x32 makes the casings 8x cheaper pack-wide | Accepted (ruled "regardless of flags") |
| Q-B5 | 48 Neutronium nanites per Spatial/Temporal craft | Accepted as RC-2's economics; not rewritten |
| Q-B6 | Magnetar Matter condensate (SL-B16) | Deferred: not registered here. The Homogeny/Reality branch registers `btsentangledmagnetar` with its first consumers, same name, so the two merge cleanly |
| Q-B7 | SL-P29 (Boundary x8) | Unchanged |
| Q-B8 | A config switch for BEC routes | None: the old recipes are automatic fallbacks only |
| Q-B9 | Kerr T2 needs the BEC network | Accepted (Kerr T2 already needs Eye T1, which needs the BEC) |
| Q-B10 | Our condensates are not in the Diode's "filters from input hatches" mode (GT maps its 19 enum types only) | Accepted; manual filter slots take any fluid |
| Q-B11 | Stellar Field Sensor Hatch in the BEC | Included (SL-B22) |
| Q-B12 | Metamaterial Film, Intergalactic / Homogeny cores | Not now; SL-B90+ reserved |
| Q-B13 | Generator tiers | RSM 30 s UIV, ESPM 60 s UMV, HSM 90 s UMV, WDM 120 s UXV, BDM 150 s UXV, Agent 60 s UMV |
| Q-B14 | WDM in the Magnetar Seed Core | Added: 256 ingots (36,864 L), the merger's white dwarf |

**Judgement calls in the build:**

1. **The safety net counts what GT kept.** GT's `addTo` returns an empty list (and logs) when it rejects a recipe, which
   the guard used to count as "registered". `BTSRecipesBEC` throws on an empty `addTo`, so a rejected BEC recipe is a
   skip and its old recipe registers. The skip still appears in the boot summary ("N registered, 1 SKIPPED") even
   though the part stays craftable, so a pack change is visible. A condensate whose generator recipe fails makes every
   recipe that needs it skip and fall back, so no recipe can ask for a condensate nothing makes.
2. **Where the old bodies live.** Moved verbatim into `BTSRecipesBEC` as `legacyXnn()` methods; `BTSRecipesSingularity`,
   `BTSRecipesEye` (now only SL-E01) and `BTSRecipesKerr` keep a one-line comment where each recipe was.
   `BTSRecipesEye.denseSpaceTime` is package scope now (shared).
3. **Stacks above 64** (e.g. Harmonic Lining's 72 Neutronium plates, dense-plate fallbacks) are split into 64-stacks
   that share the slot's nanite tier, so the nanite array always matches the item array (GT's map throws otherwise).
4. **Condensate names**: `btsentangledstarmatter`, `btsentangledexcitedplasma`, `btsentangledhyperdense`,
   `btsentangledwhitedwarf`, `btsentangledblackdwarf`, `btsentangledantipolarization` (frozen once shipped). Their
   display names contain the gate-audit fragments ("raw star matter", "white dwarf" ...), so `/btseye gates` would
   catch any of them in a compression generator.
5. **Tooltips (rule 14).** The five cores, the six charts, the Stabiliser Pylon and the Kerr Density Upgrade say
   "Crafted in the Condensate Assembler (Bose-Einstein Condensate)" and with which condensate; the Pylon, the Density
   Upgrade and the Sol / Sirius A / Arcturus charts switched to the tooltip item class to show it (the item class is
   not saved in worlds). The questbook branch should mention the BEC route on the Eye T1 and Kerr T2 quests.
6. **Costs doc section** is §11 (the proposal said §10, which D-49 took).
7. **Rule 17 (Superdense Ergomatter)**: master's code, lang and docs had no "Supermassive Ergomatter" left (grep), so
   nothing to rename here. **Rule 19 (loop fixes)**: none of these recipes uses Magnetar Matter or Ergomatter, so
   neither fix applies; the UXV parts in SL-B70/B80 (Field Generator, Emitter, Sensor UXV, UXV circuits) are the same GT
   parts SL-E05/E06 already used.

**Gate audit.** Clean by construction (proposal §5.7): Eye T1 parts have no Raw Star Matter, no Boundless Cosmic
Solder and nanites at most Transcendent; each later part uses only products of the tier below (a transition core
also its own tier's); White Dwarf nanites first appear in the T4 -> T5 core, Black Dwarf in the T5 -> T6 core. No
compression generator recipe changed.

**Not verified (needs a dev-server boot):** the boot log should show 6 generator recipes, 20 assembler recipes, no
"was skipped; its old recipe" lines, 0 skipped; NEI's Condensate Generator page should list the six fluids and the
Condensate Assembler page SL-B20..B81 with their nanite tiers; a scripted craft of SL-B21 (Sol chart) on a dev network,
and SL-B40 without Raw Star Matter condensate failing at the end with "missing condensate".

**Suggestion.** Accept as built. Watch the Density Upgrade's 4M L of Raw Star Matter condensate in play: it is the
largest single BEC bill in the ladder (the same Raw Star Matter as SL-K04 asked for, now through a Condensate Generator).

---

### B-65 Real-client render QA, part 1: the screenshot harness and round 1 [render QA]

**What was built.** A dev-only harness (`rendertest/RenderTest`, `RenderTestClient`) that is registered only when the
client JVM runs with `-Dbts.rendertest=true`. It has no effect in normal play. Run it with
`JAVA_TOOL_OPTIONS="-Dbts.rendertest=true" ./gradlew.bat runClient --no-daemon`: this buildscript's `runClient` takes
no JVM-argument property, and `JAVA_TOOL_OPTIONS` reaches the forked client. Add `-Dbts.rendertest.only=<prefixes>`
to run a subset (e.g. `kerr,ref_gt`).

The harness creates or reuses the creative superflat world `run/client/saves/bts-rendertest-L2` on the integrated
server. It builds 10 sites with the existing kit code (`KerrKit.build`, `EyeKit.build`, so the same path as `/btskerr` and
`/btseye`) and force-loads each site. For 14 cases it shoots front, three-quarter elevated and close-up views, by day
(time 6000) and by night (18000), into `run/client/screenshots/rendertest/<case>_<view>_<day|night>.png`. It logs the
debug-counter FPS at each shot (`rendertest.log`, `fps.tsv`) and then closes the client.

**Judgement calls (built, PROPOSED):**
1. **The Eye's objects are forced, not lived.** Driving a real star into each state (T5 batch, magnetar, Core
   Collapse phases) would mean running the lifecycle for hours. Instead `EyeCentreRender.debugForceLook` (via
   `MTEEyeOfHarmonyBTS`, never saved, null in play) shows the exact `EyeStarLook` that `EyeStarLook.of` computes for
   that state. `swell(f)` and `bounce(unrest)` were extracted from `of()` with the same numbers, and `lit` and `dwarf` are now public.
   The cases use Sol / Sirius A / Arcturus / neutron / magnetar lit with no unrest, Core Collapse mid-swell at f = 0.5,
   the bounce with no unrest, and Sirius B at 10 % and 90 % of its cooling batch. The Core Collapse looks are shown on
   the T4 structure (the transition builds into T4).
2. **The Kerr's hole** is set with `debugOpenHole(mass)` and held there once a second (T1 16 and 64 SM, T2 256 SM).
3. **References.** GT's Black Hole Compressor is built by its own `construct`. Three of its hatch casings nearest the
   controller become a creative energy hatch, an input bus and an output bus (it forms). The hole is opened the way a
   seed opens it: status 2 and GT's private `createRenderBlock`, called by reflection. The harness holds its stability
   at 100. TecTech's Eye of Harmony is built by its own `construct` (tier-9 generators) but does not form (no hatches,
   no recipe). TecTech only places its star while a recipe runs, so the harness places the star the way
   `createRenderBlock` does (render block 16 behind the controller, rocket tier 8, size 1.4). The controller is
   disabled, because an unformed TT multi that is allowed to work removes its star at every check.
4. **Viewpoints.** Every structure faces north. "front" stands level with the structure's middle, far enough that its
   box fits a 70 degree view. "34" is front-right and raised. "close" stands inside the open space around the object,
   between it and the front, at max(0.55 x the free distance, 1.9 x the object radius + 1.5).

**Harness bugs found and fixed while running it (not mod bugs):**
- Forge queues a chunk for unloading the moment its force-load ticket is released, even with a player in it, and
  unloading that chunk takes the player entity out of the world. The first runs released the previous site's tickets
  before the teleport, so the server stopped ticking the player and never sent another chunk; those shots showed
  only sky. The harness now keeps its own tickets and releases the old site's 100 ticks after the player leaves.
- The client player only sends movement packets while the chunk under it exists, so every camera spot is loaded on the
  server before the teleport.
- dreamcraft's "really quit?" Swing window cancels `Minecraft.shutdown()`, which hung the first unattended run. The
  harness switches `DreamCoreMod.showConfirmExitWindow` off for its session only.
- A reused world keeps an older site layout's structures, so the world folder names the layout (`-L2`).

**Round 1 (84 shots, all 14 cases; copies in my local work folder `render-qa/round1/`).** RTX 4090, 1280x720,
16-chunk render distance, no frame cap. Average FPS at the shots (min-max):

| Case | FPS | Case | FPS |
|---|---|---|---|
| kerr_t1_16sm | 1247 (1223-1267) | eye_t4_collapse_swell | 1120 (1070-1178) |
| kerr_t1_64sm | 1237 (1203-1254) | eye_t4_collapse_bounce | 1134 (1094-1203) |
| kerr_t2_256sm | 1169 (1123-1191) | eye_t5_dwarf_early | 1097 (1061-1151) |
| eye_t1_sol | 1211 (1200-1233) | eye_t5_dwarf_late | 1101 (1082-1127) |
| eye_t2_sirius_a | 1221 (1186-1251) | eye_t6_magnetar | 1049 (887-1154) |
| eye_t3_arcturus | 1180 (1165-1196) | ref_gt_bhc | 1301 (1258-1371) |
| eye_t4_neutron | 1139 (1101-1203) | ref_tt_eoh | 1176 (855-1271) |

None of our renders costs measurable frame time on this GPU. The spread comes from structure size: the T6 Eye is the
largest box and has the lowest average.

**First observations for part 2 (no change made here):**
- **Late cooling dwarf.** It reads as a dull brick red (rgb 6D322D, gamma 0.66), not near-black. D-49 wants it to
  "darken to black".
- **Close-up of the largest objects.** The Core Collapse swell (radius 16) and Arcturus (15) fill the whole close-up
  frame. Part 2 could pull the close view back for radii above about 12.
- **GT's `gregtech:orbit` shader** does not compile on this NVIDIA 610.88 driver ("ambiguous overloaded function
  reference min(int, int)"). GT logs that it will not render, which probably affects TecTech's orbiting planets in the
  reference. That is GT's bug, not ours.
- **GT's Black Hole Compressor** keeps one chunk re-rendering every second while its hole is open. The Kerr does not.

**Suggestion.** Keep the harness as the standard way to check renders. Part 2 reviews the round-1 images against the
mockups and acts on the observations above.

### B-66 Render fix round 1: the Kerr hole's size, the Eye's glow, colours and flares, and round 2 [render QA]

**What was built** (addon commits 06b16e3..e479cb9). It fixes the defects the part-2 review found in round 1 (B-65).

- **Kerr K1, size and clipping.** The core scale is now logarithmic in mass: `clamp(3.2 + 0.75 log2(M / 16), 1.5, 6.4)`,
  which gives 3.2 at 16 SM, 4.7 at 64 SM and 6.2 at 256 SM. The visible halo/disc reaches 1.35 times the core scale:
  4.3, 6.3 and 8.4 blocks, and at most 8.6 at the 512 SM T2 cap. Before the fix it was about 11.5 blocks at 64 SM.
  This keeps the halo about 1.5 blocks inside the lowest coil ring and inside the interior ring of cyan-eye casings.
  GT's model draws its halo at about 1.8 blocks per unit of GL scale (measured in round 1), so the KEE draws it at
  0.75 times the core scale. The functions are `MTEKerrEngine.coreScaleFor`, `haloRadiusFor` and `renderScaleFor`.
- **Eye E1/E9, glow.** Every object gets an additive glow billboard. Its radius is 2.5 times the object's radius, at
  least 6 blocks. It uses the object's colour at alpha 0.30, falling to 0 at the edge. It is depth-tested and writes
  no depth. It stands just behind the object (scaled up so it looks the same size), so the star's face stays clean.
  The glow shows through the crown band's gaps.
- **Eye E9, beams.** The neutron star and the magnetar sweep two polar beams: 12 blocks long, 0.4 wide, additive
  alpha 0.5, tilted 20 degrees off the vertical and turning with the spin.
- **E2, shells.** The two shells are additive. The NOVA core pass keeps ONE_MINUS_SRC_ALPHA.
- **E3/E4/E5, colours.** The colours are as the review proposed: Sol FFF0C8 at gamma 4.2, Sirius A C8DCFF at 4.8, the
  neutron star DCE8FF at 6, the bounce C8E1FF at 6. The swell's gamma falls from 2.6 to 1.6.
- **E7, cooling dwarf.** It uses the 4-stop palette. Its shells fade by (1 − cooling), and it gets the faint dark-red
  rim.
- **E8, magnetar.** It gets a flare shell (1.8 times its radius, B070FF, alpha 0.15 + 0.45 × warning). It also flares
  every 80-160 ticks, out to 6 radii over 40 ticks, using the bounce shell's code. The warning reaches the client as
  a new synced value, `EyeStarLook.aux`, which for the dwarf carries the cooling fraction.
- **E10, chunk churn.** `TileEyeStar.setLook` sends the look as a description packet straight to the players watching
  the chunk. It used to call `markBlockForUpdate`, which also sent a block change, so every client rebuilt the chunk
  section. `differs()` now allows 4 per colour channel.
- **Harness.**
  - The bounce case freezes its shell at 30 ticks (dev-only `TileEyeStar.debugEventAge`).
  - A new case, `eye_t6_magnetar_warn`, shows the magnetar with warning 1 and a flare frozen at 15 ticks.
  - Close-ups of objects with a radius over 12 stand at 2.2 radii + 4. The camera climbs over the crown until it
    has a clear line.
  - The Kerr cases log the core scale, the halo radius and the GL scale.
  - `RenderDirtTracker`, a client world listener, logs what re-rendered chunk sections during each shot's still
    window.

**Judgement calls (built, PROPOSED). The review's numbers that did not hold up in the real client:**
1. **Shell strength.** At the review's 0.4 / 0.2 and the star's own gamma, the additive shells blew Sol, Sirius A, the
   neutron star and the white dwarf out to flat white. The shells now run at the review's fallback of 0.3 / 0.15,
   with their gamma capped at 1.2. Round 2 shows Sol yellow-white and Sirius A blue-white, both with visible texture,
   and no cyan blobs.
2. **Why the review's dark colours do not darken.** The tectech:star shader (checked in the RC-2 jar) keeps the
   texture's luminance and takes only the tint's chroma (YIQ). A dark tint therefore does not darken the ball; only
   the gamma does (luminance^(1/gamma)). The review's late dwarf gammas (1.2 / 0.6 / 0.4) still rendered a pink-grey
   Moon at 90 %. The late stops are now 0.45 / 0.15 / 0.08. At 90 % the ball reads dark copper-brown with some bright
   glints, and the rim is at gamma 0.6. Going fully black would need a shader of our own (see the suggestion below).
3. **Magnetar colour.** It is 6A3CFF at gamma 1.2, not 8A3CFF at 2.2, which rendered white-pink. At night it reads
   violet with a violet halo. By day the core still reads pink-violet, because the chroma swap puts its hue near
   magenta. Its flare shell runs at gamma 1.
4. **E10 diagnosis.** Over 90 still windows, the only things that re-rendered chunks at the Eye sites were
   `minecraft:dirt` block updates: superflat grass dying in the shade of the freshly built structures. None came from
   the Eye's render tile or its hatches. Round 1 ran on a fresh world, which explains most of its 4-18 updates/s. In
   round 2 (the same world) the Eye sites average 0-1.8 per shot. The setLook packet change still removes a rebuild
   per look change in live play (the cooling dwarf changed colour every field second).
   - Also seen: GT's BHC re-renders its render block once a second.
   - Also seen: the KEE controller asks for a block update about every 8 ticks, but only for a nearby player (5 per
     40-tick window, close views only). This is probably GT's client data / active-flag traffic. It is harmless at
     these numbers. If it matters on weak clients, investigate.

**Round 2.** 90 shots, all 15 cases, copied to my local work folder `render-qa/round2/`. The T5/T6 shots come from a
re-run after the last tuning (`rendertest-t5t6-rerun.log`). The first pass, before the shell and dwarf tuning, is in
`round2/pass-a/`. FPS at the shots: Kerr 1152-1269, Eye 978-1209 (lowest: Sol 34-view), BHC reference 1134-1315,
TecTech EOH reference 1237-1290.

**Open design questions (no render change made):**
- **K2: the hole is hidden from the front at eye level.** It is hidden by the drum at T1, and at T2 only two cream
  crescents show. GT's BHC behaves the same. Suggestion: add a Quantum Glass window, 3 high and about 9 wide, in the
  drum's front wall at the hole's equator (a structure change: re-export the Kerr shapes and the BOM). The
  alternative is to accept it.
- **E1: the crown at eye level.** With the glow, the Eye's object reads from the front at night as a bright slit
  through the band gaps with a halo above the crown. By day the additive glow barely shows against the sky. TecTech's
  Eye is fully readable from the front. Suggestion: open a 3-block window ring in the crown band at the object's
  height (a structure change). The alternative is to accept "look from above / inside", as the Forge of the Gods does.
- **Dwarf and magnetar colour.** To reach a true black dwarf and a non-pink violet magnetar, ship our own copy of the
  star fragment shader with a brightness multiplier (a tint that scales the luminance as well as the chroma). That
  adds about 30 lines of GLSL in two profiles. Suggestion: do it in round 2 of the fixes if you want them exact.

### B-67 Render fix round 2: the Eye's colours, glow seam and black dwarf, the KEE's chunk churn, and round 3 [render QA]

**What was built** (addon commits 0eedb59..fc1848c). It fixes the round-2 review's defects E-1..E-8, P-1 and H-1. E-9
(readability from the front) is a structure question and stays open (K2 / E1 in B-66). The Kerr's render needed no
change.

- **E-1..E-4, E-6: colours.** Every look's core gamma is capped at 2.5 (`EyeStarLook.MAX_GAMMA`, applied in the
  constructor, so the protostar blend and the nova flash obey it too). The tints are saturated, because the star shader
  keeps only the tint's chroma.
  - Sol is FFE08A at gamma 2.0. Sirius A is 9EC2FF at 1.8. The neutron star is B8D0FF at 2.5.
  - The bounce's proto-neutron star is B8D4FF at 2.0.
  - Arcturus is FF6A20 at **0.9**. The swell starts at Arcturus's gamma and falls by 0.3.
  - The magnetar core is 4A30FF at **0.7**. Its flare shell is unchanged (B070FF, gamma 1).
  - The cooling dwarf's early stop is **B8CCFF at 1.5**, and its half-way stop runs at gamma **1.8**.
- **E-1, shells.** The two additive shells run at 0.12 / 0.06 for the lit stars, the swell, the neutron star, the bounce
  and the dwarf. The protostar, the magnetar and the nova keep 0.3 / 0.15.
- **E-8, limb ring.** Every kind's shells sit at 1.01 / 1.02 radii, not 1.02 / 1.04.
- **E-5, glow seam.** The glow disc now passes through the object's centre. It is no longer pushed back or scaled up.
  Its radius is `min(max(6, 2.5r), 20.4)` blocks, so it never reaches the nearest `eye_core` block (20.9 blocks out).
  The core's depth still hides its middle.
- **E-7, black dwarf.** An untextured, alpha-blended sphere in 0E0A0C is drawn over the core. Its alpha is
  `0.85 × clamp((cooling − 0.5) / 0.4, 0, 1)` and it writes no depth. The dark-red rim (1.03r) is kept.
- **P-1, KEE churn.** The cause was not client data or the active flag. The harness's new server-side packet spy showed
  the server sends ONE tile description packet per KEE structure re-check, every 200 ticks. GT's
  `MTEEnhancedMultiBlockBase.onStructureCheckFinished` issues a tile update after every formed check, to sync the
  structure's sound centre. GT's client reads that packet through four client events plus one texture update, and each
  marks the block. That is the "5 block updates per window" seen in round 2: one chunk rebuild every 10 s, not 2.5 per
  second.
  - The fix is the new `machines/QuietTileUpdate`. The KEE passes GT's request through only on the first formed check
    (after a load, a placement or a re-form) or when the centre or radius moved.
  - In round 3, no shot window shows a KEE block update.
- **H-1, harness.** Close-ups use "2.2 radii + 4" from radius 9 (it was over 12). Sol's camera is now 23.8 blocks out
  and Sirius A's 28.2.
- **Harness diagnostics.** The client dirt tracker logs the call path of the first machine block update in each window.
  A server-side tracker logs block-update requests. A dev-only `PacketSpy` (harness runs only) replaces the harness
  player's net handler. It counts the tile description packets and the GT block events the server sends per shot, with
  the call path.

**Round 3.** 90 shots are in my local work folder `render-qa/round3/`. The full run is in `rendertest.log`.
Retuned cases were re-shot and overwrite the full run's files; their earlier shots are in `pass-a/`, `pass-b/` and
`pass-c/`:
- `rendertest-retune.log`: Arcturus, the Core Collapse, T5 and T6.
- `rendertest-retune2.log`: the bounce and the early dwarf.
- `rendertest-retune3.log`: the bounce.

The magnetar-warn shots were taken with the flare's shock shell at gamma 1.5. It is now 1.2, which is slightly dimmer.

Measured on the close-ups (disc samples, same method as the review's judge2 statistics):

| Case | Round 2 | Round 3 | Target |
|---|---|---|---|
| Sol | FEFEF7, 71-77 % white | F8E695 / F0DA82, 0 % white, median 216-229 | < 15 %, about FFE6A8 |
| Sirius A | FEFEFF, 93-98 % white | B9D8FB / BFDFFD, 3-8 %, median 209-216 | < 15 %, about C0D6FF, 200-215 |
| Arcturus | F3EC93, median 233-240 | FC7022 / F78D3C, 0 %, median 139-170 | about FF9A50, about 170 |
| Swell (half way) | F2C47C | F07A48, median 153 | redder than Arcturus |
| Neutron star | DDE4E6, 76 % white | D6E7FC, 6-10 % | < 20 % |
| Bounce | 100 % white | C0DEFB / C4E1FC, 2-6 % | < 20 % |
| Dwarf 10 % | 100 % white | D4C9E0 / E4D9ED, 9-19 % | < 20 % |
| Dwarf 90 % | 462623, p90 74-77 | 150D0F, p90 21; the rim limb shows at night | ≤ 1E1416, p90 ≤ 35 |
| Magnetar | FEDCFF / FDCAFF | B080FF / B584FF, 0 % | about A070FF |

FPS at the shots: Kerr 1165-1306, Eye 957-1243 (lowest is the neutron star's 3/4 view), BHC reference 1239-1331,
TecTech EOH reference 1281-1318.

**Judgement calls (built, PROPOSED). The review's numbers that did not hold up in the real client:**
1. **Arcturus at gamma 0.9, not 1.5.** At 1.5 the shots measured FEC072, median 204, which is still golden. The
   texture's luminance has to come down before the shader's chroma reads orange.
2. **Magnetar at gamma 0.7, not 0.9.** 0.9 measured C597FF (lilac). 0.7 lands on B080FF.
3. **Dwarf early stop at B8CCFF / 1.5, not E6EEFF / 2.2.** The review's values measured 41-54 % white. The case sits at
   10 % cooling, where the blue-to-orange blend passes through grey, and grey adds no chroma. Only a lower gamma keeps
   the texture. The half-way stop runs at 1.8 for the same reason.
4. **Bounce.** The proto-neutron star is at gamma 2.0 (not 2.5). The shock shells (the bounce's and a magnetar flare's)
   run at gamma 1.2 (not 4), and the bounce shell at alpha 0.25 (not 0.6). The magnetar flare's burst is at alpha 0.3
   (not 0.4). At gamma 4 the bounce shell alone blew the star out to 97 % white.
5. **Veil geometry.** The veil's vertices stand out by `1 / (cos(π / slices) × cos(π / 2·stacks))`, about 1.007 at
   32 × 24. Its flat faces therefore clear the core's depth. With the vertices at 1.003r the faces sank up to 0.9 %
   inside the core, so the depth test hid most of the veil. It is drawn after the shells and the rim, so it also darkens
   the rim's face, and only the rim's limb shows. That is the "rim still visible" reading.
6. **P-1: a shared helper rather than a KEE-only patch.** `QuietTileUpdate` can serve any of our controllers that
   re-check on a timer. Only the KEE uses it now. The Eye was not flagged.

**Still open:** E-9 (K2 / E1 in B-66): Quantum Glass windows in the Eye's crown band and the Kerr drum, or accept the
"look from above" behaviour. Both are structure changes (shapes, BOM, recipes), so they wait for your ruling.

### B-68 Render fix round 3: the Eye's see-through layers in pass 1, shock-front shells, corona and limb, and round 4 [render QA]

**What was built** (addon commits 9755807..f922a1d). It fixes the round-3 review's Eye defects D1-D7. The Kerr needed
no change (the review passed it).

- **D1, sorting.** The Eye's render tile now renders in passes 0 and 1. Pass 0 draws the object: the opaque core, the
  two thin additive surface shells, the dwarf's dark-red rim, the new limb darkening and the black dwarf's veil.
  Pass 1 runs after the translucent and glowing block faces. It draws everything see-through around the object: the
  corona, the glow disc, the pulsar beams, the magnetar's shells, the bounce shell and the nova flash. These layers are
  additive, depth-tested and write no depth, and back faces are drawn before front faces. In the round-4 shots the
  bands' cyan strips behind the bounce shell and the magnetar shells are tinted.
- **D2, bounce.** The bounce is now an untextured shock front, B8D4FF, with additive alpha
  `0.04 + 0.60 × (1 − |N·V|)^3` per vertex. It is clear in the middle with a bright edge, and fades with
  `1 − age / 100`. At 30 ticks it is 7.4 blocks across. The cloud texture is gone.
- **D3, magnetar.** The magnetosphere is 4.5 blocks, C080FF, rim alpha 0.04 → 0.3 when quiet. On a warning its edge
  pulses 0.35 ↔ 0.6 at 2 Hz, and an outer shell appears at 7 blocks (7A4CFF, edge 0.29 ↔ 0.5). Flares run from 4.5
  to 7 blocks (edge 0.5, fading). The close view is no longer veiled.
- **D4, swell.** The swell eases from 15 to a peak of 20 blocks (19.4 at half way). It reaches FF3A12 at gamma 0.6 by
  half way, spins at 3× Arcturus's speed at half way, and throbs at 0.7 Hz with pulse 0.15. Measured F8280C against
  Arcturus FC8130.
- **D5, early dwarf.** F2F5FF holds until 20 % of the batch, at gamma 2.5 → 2.3. Its surface shells run at 0.3 / 0.15,
  and the dark-red rim fades in with the cooling, as the veil does. Measured F7F8F9 by day and F0F2F6 by night: every
  channel is at least 235 and blue is at or above red. The late dwarf is unchanged (170E10).
- **D6, corona and limb.** Every live star, the swell, the bounce and the early dwarf get a corona: a ring from the limb
  out to 1.12× the limb, star colour, 0.30 at the limb falling to 0. It lies in the limb's own plane, so it neither hides
  the star nor is hidden by it. The core is limb-darkened to `colour × (0.75 + 0.25 |N·V|)`.
- **D7, pulsar.** Beams are 0.45 blocks wide each side, 0.6 at the root falling to 0, with the tip 19.4 blocks from the
  centre. The neutron star's glow ring reaches 1.6× its limb at 0.45.
- **Meshes.** Every sphere is 32 × 16, the core included (it was 64 × 64). The untextured spheres use a unit-sphere table
  built once, with per-vertex alphas filled once per shell for both faces. The first run built them with trig every
  frame, and the magnetar fell to 348-939 FPS.
- **Harness.** New `ref_gt_godforge` case: TecTech's Forge of the Gods render block alone on a site (x 6000), set up as
  `MTEForgeOfGods.createRenderer` sets up a fresh Godforge (star 20, speed 5, one ring, default colour).

**Round 4.** 96 shots are in my local work folder `render-qa/round4/`. The Kerr and reference shots come from the full
run (`rendertest-full.log`, `fps-full.tsv`). The Eye shots come from the re-run after the mesh-table fix
(`rendertest-eye.log`), and the T5 shots from the dwarf retune (`rendertest-dwarf.log`). Each run's raw output is in
`run1/`..`run3/`.

| Case | Close-up centre, day | FPS avg (min-max) |
|---|---|---|
| Sol | EFDA8A | 1122 (1109-1138) |
| Sirius A | BDDDFC | 1119 (1086-1155) |
| Arcturus | FC8130 | 1115 (1080-1138) |
| Neutron star | D6E7F9, 8 % white | 1074 (1041-1130) |
| Swell | F8280C | 1066 (1043-1088) |
| Bounce | C2DCF9 | 1023 (978-1065) |
| Dwarf 10 % | F7F8F9, 83 % white | 1061 (1022-1117) |
| Dwarf 90 % | 170E10 | 1011 (946-1055) |
| Magnetar | 845DFF | 929 (879-1017) |
| Magnetar warning | - | 855 (765-916) |
| Kerr T1 16 / 64, T2 256 | - | 1248 / 1262 / 1185 |
| BHC / TT EOH / Godforge refs | - | 1319 / 1318 / 1283 |

Round 3's Eye cases ran at 957-1212.

**Judgement calls (built, PROPOSED):**
1. **Swell peak 20 blocks, not 21.** The nearest `eye_core` block face is 20.4 blocks from the centre (measured from the
   exported shape: cell (0, −6, −20)), not the review's estimated 22-24. The throb only shrinks the swell (by a third of
   the pulse) and swings its brightness (gamma ×(1 ± 0.15)) by the full pulse, so the swell never reaches a block.
   Every see-through layer also stops at 20.4: the glow disc, the corona's outer edge and the beam tip (minus 1). The
   swell's corona is therefore only about one block wide.
2. **Quiet magnetosphere at edge 0.3, outer shell only on a warning.** The review gave the warning's numbers only. At
   rest the inner shell runs at half strength and no outer shell is drawn, so a warning reads as a clearly bigger,
   pulsing violet ball.
3. **The neutron star's 1.6× glow replaces its 1.12× corona** (0.45 at the limb), rather than adding a second ring.
4. **Dwarf at 83 % near-white by day.** ">= 235 on every channel" with blue ≥ red leaves little texture. The limb
   darkening and the shells' grey keep it reading as a ball. Round 3 measured 9-19 % white.
5. **Magnetar FPS is 10-20 % under round 3** (855-929 avg, against TecTech's 1318). It now draws three CPU-tessellated
   rim shells, both faces, every frame. If that matters, the next step is a small rim shader on GPU meshes instead of
   the Tessellator.
6. **Shell silhouettes are 32-sided.** The 32 × 16 cap shows as slight facets on the 7-block shells in the close views.
   48 × 24 would hide them for about 2× the shell cost.

**Still open:** E-9 (B-66 / B-67): from the front the bands hide the small objects (magnetar, neutron star), as in
`eye_t6_magnetar_front_night`.

### B-69 Render gallery: one page with every round-4 shot and its verdict [render QA]

**What was built.** A single HTML page, `render-qa/gallery.html` in my local work folder (6.6 MB). It holds all 96
round-4 shots, grouped by machine (Kerr Ergosphere Engine, Eye of Harmony) and by case. Each case shows the
harness's description, the FPS at every shot, the case's average and range, and the chunk rebuilds counted during its
shots. It also gives the verdict, the defect codes, the measured centre colour and any open structure question. The
references (GT's Black Hole Compressor for the Kerr, TecTech's Eye of Harmony and Forge of the Gods for the Eye) open
each machine. Clicking a shot opens it beside the reference from the same view and time. The page has View and Time
filters and works at 400 px in light and dark themes. Its generator is `render-qa/gallery-build/build_gallery.py`,
which reads `verdicts.json` and the 1024-px JPEG copies in `thumbs/`.

**Judgement calls (built, PROPOSED):**
1. **No round-4 verdict exists yet.** No judge has reviewed round 4. Each case therefore shows the judge's latest
   ruling (the round-3 review) and what round 4 changed in answer (B-68). On that basis:
   - The Kerr cases are **Pass**, because the round-3 review passed them and the Kerr has not changed since.
   - The late dwarf shows **No defect raised**, because round 3 named no defect for it.
   - The nine other Eye cases show **Fixed, awaiting review**.
   - The banner at the top says so. When the round-4 review lands, edit `verdicts.json` and re-run the generator. A
     `fail` status is supported.
2. **FPS per shot comes from the run that produced the final file.** Following B-68, that is the full run for the
   Kerr and the references, the Eye re-run for T1-T4 and T6, and the dwarf retune for T5. This was checked by
   comparing file hashes against `run1/`..`run3/`. The case averages are recomputed from the six shots. They match
   B-68's table to within 1 FPS (rounding).
3. **Downscaled to 1024 × 576 JPEG, quality 80** (4.6 MB of images), so the page stays well under 12 MB while
   keeping close-up detail. The full-size PNGs stay in `round4/`.

### B-70 Automation pass: the Directive Hatch, OpenComputers, plinth-bus pickups and the unattended proof [automation pass, built]

**Context.** Ruling 18: every player action on every system must have a working route through AE2, redstone and
logic gates, OpenComputers and Steve's Factory Manager, with detection hatches including a "degasser-style"
binary-coded one. This pass covers what is on master: the Eye of Harmony (D-49, T1-T6) and the Kerr Ergosphere
Engine with its modules. `docs/automation.md` has the bit tables, the route matrix for every action, the
OpenComputers methods, the minimal rigs and a questbook section to copy.

**What was built.**

- **Directive Hatch, MTE 28127** (`machines/directive`). Once a second its machine pushes a 4-bit code. The hatch
  outputs the code, one bit, or "any bit" (with invert) on every side, and a comparator reads the raw code. Control
  sets turn it into a redstone input for a setpoint. It goes in the Eye's Sensor sockets (`n`) and the KEE's utility
  sockets (`u`), so no structure changed. Recipe SL-A01: an Assembler at UIV. Overlay textures come from
  `gen-sl-textures.js`. The bit tables are in its tooltip.
- **Eye plinth-bus pickups.** The next transition core in a plinth input bus goes into the escrow. A star chart in a
  plain plinth bus goes into the controller slot, and the old target leaves through the ME output bus.
- **OpenComputers**, a soft dependency. It compiles against OpenComputers 1.12.61's API jar, and the driver is
  registered only if the mod is loaded. Adapter components: `bts_eye`, `bts_kerr`, `bts_kerr_module`, `bts_sensor`
  and `bts_directive`. They are thin wrappers over `BTSComputerApi`, which calls the GUI's own setters.
- **The creative builds** now fit Directive Hatches: Eye Trims / Status / Transition in three Sensor sockets, and Kerr
  Hole / Status in two utility sockets. The kits hand some out.
- **The proof.** `DirectiveRigSim` is a plain-JDK offline run. The dev-server bench adds
  `/btseye star … directive | autorig <s> [cut <k>] | ocloop` and
  `/btskerr autorig | ocloop | ctrl on|off|out | ocprobe`.

**Judgement calls (built, PROPOSED):**

1. **One hatch MTE for every host, on 28127** (P4's reserved id, P4's Q-19 recommendation). Its set is picked in its
   GUI, and a hatch adopts its machine's first set when placed. The P4 Quasar adds its sets (Disc, Field, Events,
   Transition, Slots) to `DirectiveSet`; the enum is saved by name. *Alternative:* one MTE per host.
2. **The code is a bitmask, as on the Degasser.** Several bits can be on at once, and each bit names one thing
   wanted now. It is not a single enumerated "most needed" number: an enumeration cannot drive two valves at once and
   needs a decoder for every use. *Alternative:* add a "Priority" set whose value 0-15 names the single most urgent
   action.
3. **Output modes are Code / Bit n / Any (+ invert), not P4's "split: bit n on face n".** A hatch in a socket shows
   one open face, so a per-face split would show a single bit. Instead, one hatch per bit in Bit mode, or one hatch
   in Code mode decoded with comparators or SFM's analog redstone conditions. *Alternative:* add Split for hatches
   with several open faces.
4. **The Eye's sets.**
   - **Trims:** one bit per trim, in the star's trim-table order. A bit is on when the prototype's bang-bang rule
     with threshold w asks for that trim. The default w is 10, the bang-bang threshold the D-49 tests use; P4 suggests
     5 for T7+.
   - **Status:** buffers short, ignition charge wanted, event, alarm.
   - **Transition:** ready, running, next core wanted, Tachyon wanted.
   - The T6 magnetar's fifth trim (the Agent) shares Status bit 3 with "event warned". That bit is a T3 flash or a
     T6 flare, and either way it means open the event valve.
   - **Transition bit 4 changed after the first dev-server run.** It was "dark, waiting". Then the rig poured
     Tachyon all through the Mass Infusion and the Shell Ignition, because bit 2 "running" was its only cue. It is
     now **Tachyon wanted**: the Core Collapse is fast-forwarding, or the dial is set above 1 and could not be paid.
5. **The Kerr's sets.**
   - **Hole** uses the task's four conditions: closed; more than 1 SM below the setpoint; bolts short; more than
     1 SM above the setpoint.
   - **Status:** containment power short, no Seed while closed, unstable, not formed.
   - **"Bolts short"** means the bolt stock lasts less than w minutes at the current bleed (default 10).
6. **Control sets.** Redstone 1-15 into a Directive Hatch sets the Kerr mass setpoint (2 SM to the tier's top), the
   Eye's time dial (1.00-3.00 in 0.25 steps) or the HSM feed (0-1.5 L/s). The mapping is linear. Strength 0 leaves the
   GUI's value, so an unwired control hatch changes nothing. This gives setpoints an AE2 route (a level emitter) and an
   SFM route (an analog emitter), not only OpenComputers. *Alternative:* setpoints by GUI and OpenComputers only, as P4
   has for f₈.
7. **The plinth-bus pickups change the Eye's input behaviour.** They react only to a transition core or a star chart
   put into a plinth input bus.
   - A wrong core stays in the bus.
   - Charts are taken from plain buses only. An ME stocking bus would show its configured chart every second and swap
     back and forth.
   - Only one chart is taken a second. The docs say to put ONE in at a time.
   - Swapping a chart this way follows the same rules as by hand: the next tier's chart while the star burns starts
     the transition; any other chart puts the star out.
   - *Alternative:* chart swaps by hand or OpenComputers only. Climbing tiers unattended would then need a player at
     each step.
8. **The recipe is at UIV, not P4's UXV card.** The Kerr (UIV) needs the hatch from day one. The Stellar Field Sensor
   Hatch stays an Eye-only UMV Condensate Assembler part. *Alternative:* a BEC recipe at UMV, and a cheaper Kerr-only
   variant.
9. **OpenComputers uses an Adapter driver, not SimpleComponent.** Our machines are GT tiles (BaseMetaTileEntity), so a
   block driver is the clean route; P4 assumed this too.
   - Setters exist only where the GUI has one, with the same clamps. Open and Collapse still need the Seed or
     Collapser in a bolt bus.
   - There is no 20-tick rate limit: there is no trim setter to race the field's 1 s lag. P4's limit is for its own
     trim setters.
   - `runServer` now also loads OpenComputers (dev-only, `runtimeOnlyNonPublishable`), so the driver registration is
     tested headless.
10. **Status bit 1 also counts the next transition's fluids** (its event fluids and its new star's first cycle), but
    only once the core is in escrow. An AE2 level maintainer on bit 1 therefore stocks a transition too.
11. **The bench's milestones were completed by command** (`milestones n done`). Milestones are hours of play, not an
    automation step. Everything after that (the core, the chart, the transition and its doses) was done by the rig.

**Results.**

*Offline* (`DirectiveRigSim 4 5`: 4 h x 5 seeds per case, with valves only on Directive bits, a second late):

- **T1-T6:** 0 stars lost on the gate rig and on the prototype's bang-bang rig. The yields are equal; T3 differs
  by 0.003.
- **T1, T2 and T4:** the gate rig asks for exactly the bang-bang trims every second (0 differing seconds).
- **Core Collapse (begun on Transition bit 1, Tachyon on bit 4):** 5 / 5 settled.
- **Kerr:** 5 cases (T1 / T2; SpaceTime, Neutronium and Infinity bolts; setpoints 16-512; from a closed hole). Every
  case had 0 evaporations, and the OpenComputers-style setter loop reached 64 and 512 SM.
- **FieldSim golden:** still 78 / 78.

*Dev server* (headless `runServer`, RC-2 deps plus OpenComputers; Eye T4 structure at (44000, 100, 0), unlocked set
to T1, buffers empty; Kerr T1 at (12800, 100, 10000); boot log "252 registered, 0 skipped" and the OpenComputers line):

| Step | Result |
|---|---|
| T1 from a dark, empty Eye | Status bit 1 stocked the buffers, bit 2 put in a charge, Sol lit; 860 s burning, 829 Resonant |
| T1 → T2 (milestones set) | Transition bit 3 put the Stellar Mass Core in the bus (taken into escrow), bit 1 put Sirius A's chart in (swapped into the slot), Mass Infusion completed in 600 s, no Tachyon poured |
| T2 → T3 | Shell Ignition completed in 604 s; the forced flash rode on Status bit 3 (17 s of the event valve) |
| T3 → T4 | Core Collapse completed in 1,002 s; Tachyon on Transition bit 4 for exactly the 940 s of fast-forward |
| T4, 4 h | 14,400 s burning, 0 lost, 61 % Resonant |
| T4 lost (valves held shut), then left to the rig | Breach; Status bit 2 put in a charge, Arcturus relit and the Core Collapse re-ran with no core: the neutron star was back |
| Kerr from emptied bolt buses and a closed hole, 1 h | The rig put in 1 Seed and 4,176 SpaceTime bolts; open 3,600 s, never closed, 16.08 SM at setpoint 16 |
| Real redstone (`ctrl out`) | The world read 4 at the Hole hatch's face (bolts short), and 11 inverted; the comparator read 4 |
| Control set with a redstone block (`ctrl on/off`) | The hatch read 15; the setpoint went from 16 to 64 SM |
| `ocloop` (the calls behind the OpenComputers components) | setDial 2.1 → 2.0, 9 → 3.0, 0 → 1.0; setFeed 2 → 1.5, −1 → 0; setSetpoint 100000 → 64, 1 → 2; collapse() / open() refused without a Collapser / Seed, accepted with one; getDirective rejects another machine's set |
| `ocprobe` (OpenComputers' own driver registry) | A compound driver for the Eye, the KEE, a Directive Hatch and a Kerr module, with our components bts_eye, bts_kerr, bts_directive and bts_kerr_module and their callbacks; no driver for a plain casing |

**Not covered.** A real computer running Lua (the console has no computer). An AE2 network or a Factory Manager in a
world (the routes go through plain GT hatches, which those mods already drive). The hatch GUI in a client.

### B-71 Full RC-2 dev-server test of master after the overnight pass, and what it fixed [overnight test, built]

**What was run.** Headless `runServer` (RC-2 dependencies plus OpenComputers 1.12.61) on a fresh flat world
(`sltest-ovt`), driven from the console in five boots on the same world (boots 2-5 are the restart checks). Before
that: a static lang lint (every `ofFailure` / `ofSuccess` id, shutdown reason, `StructureErrors.of`,
`translateToLocal`, recipe-map name, item, block and casing meta, and every `GT5U.*` literal, checked against our
`en_US.lang` and RC-2 GT's) and `node tools/fieldsim-golden.js --check --java`.

| Area | Result |
|---|---|
| Load | `252 registered, 0 skipped`; `BEC line: 6 addon condensates makeable ... no legacy fallbacks`; SL-B01/B02 x32; TecTech Eye removed (43 + controller); 10 BHC->Kerr swaps; OpenComputers components registered. No `beyondthestars` frame in any stack trace in five boots (one in boot 4: fix 5 below). |
| Lang lint | 168 lookups, **0 missing**, 0 duplicate keys. One key has no producer any more (`recipe_result.beyondthestars.eye.star_unit5`); left in place (harmless). |
| Golden | 78 / 78 PASS before and after every fix. |
| Kerr | T1 north / T1 east survival / T2 south / T2 west survival: KEE and both modules FORMED, re-projection, damage and repair. Extractor runs GT extractor recipes (128 Stemcells at parallel 448); compressor runs GT compressor and neutronium compressor recipes; DENSITY is refused on T1 and runs on T2 (2 parallels). Closing the hole pauses the compressor with its progress kept; it resumes when the hole opens. Render: claimed at the centre, gone 4 s after a close, off / on switch, gone at once when the controller is broken. |
| Eye | T1-T6 in four facings, creative and survival, all FORMED with the expected bands. Sol: ignition (protostar render), lit, starved (Hydrogen 0: Status bit 1, then Breach with the rig off, render cleared, "Last failure" kept), relit with the failure line still shown. The gate rig took one Eye T1 -> T5 with no hand on it (Mass Infusion 600 s, Shell Ignition 604 s, Core Collapse 1,002 s with Tachyon on Transition bit 4 for exactly 940 s, First Cooling 7,200 s) and, after fix 1, T5 -> T6 (Magnetar Merger 181 s, core consumed). T4: 4 h held; valves cut 900 s -> lost, then relit Arcturus and re-ran the Core Collapse unattended. 1:1 gate: one compression generator swapped to tier 3 on a T4 Eye -> "This target is Eye T4: every Spacetime Compression Field Generator must be tier 4 or higher"; tier 2 under an Arcturus chart -> the T3 message. |
| BEC | `/btseye bec`: 20 Condensate Assembler recipes make our parts, all 20 found by GT's own lookup with their inputs (no shadowing), condensates as RC-2 stores them (recipe metadata), 6 Condensate Generator recipes for our 6 condensates, 0 fallbacks. |
| Directive codes | Eye Status 1 / 2 / 3 / 8, Trims per tier, Transition 1 / 2 / 4 / 8; Kerr Hole 1 / 2 / 4, Status 2 / 5 / 8; control sets (redstone block -> setpoint 64 / 512). |
| OpenComputers | `ocloop` on the Eye and the KEE (clamps and refusals as the GUI's), `ocprobe`: bts_eye, bts_kerr, bts_kerr_module, bts_directive drivers from OpenComputers' own registry. |
| Render lifecycle | Every Eye centre checked after the run: a block only where a star burns (T6 magnetar, T4 while its Core Collapse ran); none at the dark, broken or unformed Eyes; the T3 Eye's star went with its controller. |
| Restart | Unlocked tiers, milestones, live stars and batches, transitions in progress (merger imprint seconds), last failures, render re-claimed, Kerr holes / modules mid-recipe, Directive and sensor hatch settings: as before (after fix 3). |

**Fixed here (each its own commit).**

1. **The Magnetar Merger could never complete.** Since ruling 12 (B-60) the paid Agent upkeep charges the flare shield
   by 2r a second, and the shield absorbs Π every second, so the dynamo's imprint drift was eaten and Π sat at 0.0 for
   the whole 600 s. Every merger timed out and every automatic re-merge spent the charge again: **T5 -> T6 was
   impossible**. B-59's merger test predates B-60, and the prototype skips the merger, so the golden set never saw it.
   - *Built:* a new `Overlay.shieldPerUpkeep` hook (default = `SHIELD_PER_UPKEEP`, so the plain field and the golden
     cases are unchanged). During the imprint the merger returns 0: only Agent doses charge the shield there. The
     release keeps T6's own rate.
   - *Judgement call (PROPOSED):* the imprint's band is +60..+90, but above +70 the field is Critical (Integrity
     −2/s), so a 120 s hold only survives inside +60..+70. The Directive's Agent bit during the imprint is now
     **Π > +66** (was 75; release stays > +30), and the automation valve during a transition on T6 is **1 unit/s
     (100 L/s)** (`EyeTrimRule.MERGER_AGENT_RATE`). A redstone rig opens it on Status bit 3 AND Transition bit 2.
     The standard 5-unit valve takes 20 off Π at once and leaves the band.
   - The Magnetar Seed Core tooltip, the Directive Hatch tooltip and `docs/automation.md` say so (rule 14).
   - *Alternatives:* widen the survivable band (make the imprint band +60..+70, or make the imprint not drain
     Integrity while Π is in its band). Or keep the shield on and give the dynamo a stronger drift. Both change the
     merger's maths, which is yours to set.
2. **Directive "not formed" never reached the redstone.** Every walk clears the hatch lists, and most breaks stop
   the walk before it reaches the sockets. So an unformed Eye or KEE published to nobody, and its hatches fell
   silent after 3 s (Eye Status 8 "alarm" and Kerr Status 8 "not formed" never showed).
   - Both controllers now keep the last formed walk's Directive Hatches and publish to them while unformed.
   - That list is not saved: after a restart, an unformed machine stays silent until it forms once.
3. **Restart persistence.** GT marks a chunk for saving only on inventory changes. The KEE's mass, setpoint and
   open state change without one, so a restart brought back an old save (a 64 SM hole on setpoint 64 came back as
   16 SM on 16).
   - The KEE marks its chunk once a second, as the Eye already does.
   - The Directive and Stellar Field Sensor hatch setters mark theirs.
4. **A wrench lost the failure line.** The live star is tile-only, so picking the controller up put a burning star
   out with no event or failure line. The placed-back Eye showed only "Star: DARK".
   - The item now carries "<star> put out: the controller was picked up", or the last failure as it stood.
5. **Kit-built Kerr controllers had no owner.** Breaking an ownerless GT multiblock throws in GT's power-fail tracker
   (GTNHLib team lookup) and aborts the break. In the test, that left the KEE's hole render standing until a restart.
   Its self-check then removed it.
   - `/btskerr` now gives the KEE and both modules the player who ran it, or the console stand-in.
6. **Test bench** (no gameplay change):
   - `/btseye bec`.
   - `/btseye star … compression <1-9> [all]` and `check`.
   - The Kerr bench now walks a machine GT has not checked yet since its chunk loaded. Right after `keep`, an
     autorig fitted a second pair of Directive Hatches, and a module stock found no energy hatch.

**Seen, not changed.**

- **Merger start counts as a failure.** Putting the Magnetar chart in while Sirius B batches puts the batch out with
  a salvage, and that is recorded as the "Last failure" (B-63's batch rule). That is right for a lost batch. But when
  it is how the merger starts, the line reads like a fault.
  - *Suggestion:* when the new chart is the next tier's and its transition begins, write it as an event, not a
    failure.
- **Creative `fill` T6 build.** It merges by itself and, with no regulation, fails and re-merges every ~10 minutes,
  each time spending the charge. That is by design (auto re-merge), but it is costly in a creative test world.
- **Fresh `/btskerr` and an instant autorig.** An autorig run right after building a fresh `/btskerr` can close the
  hole once (the creative energy hatches start empty and the sim runs seconds instantly). It is a bench artifact.

**Not covered.** A client: GUIs, NEI pages, the hologram preview and the renders' look. A real OpenComputers
computer. AE2 or Factory Manager in a world.

### B-72 Automation built for real: two photo rigs in a dev client, and what they taught [automation photos, built]

**What was asked.** "Since you have an automation proven, can you make me an artifact with photos of it so I can see
an example?" The earlier proof (B-70) was scripted. This one is real blocks in a real client
(`-Dbts.rendertest=true -Dbts.rendertest.cases=automation`, dev-only harness `rendertest/AutoRig*`), and the only
actor on the machines is the wiring: the harness builds, reads the state once a second into `eye.tsv` / `kerr.tsv`,
and moves the camera. Page: local work folder `autoshots/automation-example.html` (not published).

**Rig A, Eye T1 (Sol), redstone and GT covers.** Kit-built Eye, Crude field generators (the offline proof's ts = tt =
1), empty buffers. Directive Hatches in five Sensor sockets: Trims Bit 1 (Helium valve), Trims Bit 2 (Hydrogen valve),
Status Bit 1 (both upkeep valves, through a 48-cell redstone staircase with 3 repeater landings and two ground lines
with 5 and 6 repeaters), Trims Any (a torch NOT gate lights an "in band" lamp), Transition Code (a comparator and a
lamp). Each valve is a GT Debug Fluid Tank (the endless supply) with a Machine Controller cover (enable with redstone)
and a Fluid Regulator cover set to Conditional: HV 500 L/t on the trims (Sol's rig rate, 10 units/s), UV 150,000 L/t on
the upkeep. Four Stellar Ignition Charges in the plinth bus. **Result (run 4):** the upkeep valves stocked the
ignition in 202 s, the Eye lit itself (Sol lit at 243 s) and burned 866 s (14 cycles): 837 s Resonant (96.7 %), 29 s
Stable, 0 worse, Integrity 100 throughout, mean Y 1.242 (the offline proof: 96.6 %, 1.242). Helium valve open 28 s
(280,000 L), Hydrogen 1 s (the first lit second). Status bit 1 re-opened the upkeep valves 6 times as the buffers ran
down. Star runtime test rig: off the whole time.

**Rig B, Kerr T1, AE2.** Kit-built Kerr, every bolt bus emptied, hole closed. Directive Hatches in the two upper front
utility sockets: Hole Bit 3 (bolts short, window 1 minute) and Hole Bit 1 (closed). In front of each bolt bus an AE2
cable block with an export bus (redstone card, active with signal; the bolt bus one with 3 acceleration cards) and a
quartz facade on top; dust on the facade in front of the hatch powers it. ME: creative-energy controller, drive (64k
cell, 4,096 SpaceTime bolts, 4 Seeds, inserted once the camera was ready), interface, glass cable, a level emitter
(bolts >= 512) with a lamp. **Result:** Seed out, hole open 2 s after the cell went in, 16.1 SM a second later; open
1,002 s, 0 evaporations, mass 16.00-16.25 SM, bolt stock 57-121, bit 3 on 41 s, 1,088 bolts exported.

**What failed on the way, and what changed.**
1. *My wiring:* the 1.7.10 diode direction bits were reversed in the harness (direction 2 outputs south), so every
   repeater faced backwards; the wire dump showed the first landing stuck. Fixed in the harness.
2. *My wiring:* a step-up dust beside the export bus's cable block lit its lamp but not the cable block (dust powers
   only the way it flows). Redesign: the hatch above the bus, a facade on the cable block, dust on the facade.
3. *My rig:* one acceleration card (8 bolts a move) could not feed the hole's climb from 8 to 16 SM (bleed 1 SM/s at
   8 SM); it evaporated twice. Three cards (64 a move) hold it. The cell also got only 64 bolts at first: GT's stack
   helper caps an ItemStack at 64; the AE stack now carries the count.
4. *Real bug, fixed:* the Eye's GUI head line stayed "The star is dark" while Sol burned. On GT 5.09.54 recipe checks
   are event-driven, and a star changes state with no hatch event. The Eye now schedules a check when the star state,
   a transition or the unlocked tier changes (`refreshStatusLine`).
5. *Real bug, fixed:* the Directive Hatch GUI drew its four bit lines and its drive line under the player inventory
   (ten lines in a standard hatch panel). `DirectiveHatchGui.getBasePanelHeight` adds 64 px.
6. *Docs, fixed:* automation.md said the regulator is "enable with signal". RC-2's Fluid Regulator has no redstone
   mode; Conditional follows the block's work flag, so a GT valve is two covers. automation.md (§2.1, §4, new §4.1,
   the questbook text) and the Directive Hatch tooltip (two lines) now say so.

**Seen, not fixed.** The KEE GUI shows a missing texture (magenta and black) in one slot of its right-hand button
column (runs 3 and 4).

**Judgement calls (please confirm or change).**
- Trims window left at the default 10: P peaks just over +10 for one second each cycle (Stable), as the offline proof
  does. A window of 9 would keep every second Resonant; I left the documented default.
- Kerr bolt window 1 minute (default 10): at 16 SM the hole burns a SpaceTime bolt every 4 s, so w = 10 would keep
  about 600-660 bolts in the bus and w = 1 keeps 60-120 (the log: 57-121). Either way the bit fires about once a
  minute, because each export moves 64 bolts. I used 1 so the bus starts near empty and the refills show; both work.
- The upkeep valve rate (3 M L/s) is a choice: fast enough to stock the 600 M L ignition in about 3 minutes and to
  refill a cycle (50 M L) in about 17 s.
- The ME cell went into the drive when the camera was ready (a harness action, logged); everything after it was the
  wiring.
- Not built: the OpenComputers computer (optional in the brief).

**Not covered.** Tiers above T1, events (T3 flash, T6 Agent), transitions, Kerr T2, real supply chains (endless tanks,
creative ME power), OpenComputers in a world.

---

## Part C. Settled (for the record)

| Topic | Your answer | State |
|---|---|---|
| Eye T2 as the interim Excited Stellar Plasma source [Q-2] | Accepted | Built |
| Remove TecTech's Eye recipes [Q-3] | On by default, and a config option | Built (`replaceTecTechEyeOfHarmony`) |
| Eye gives EU back? [Q-4] | No, pure EU sink | Built |
| Stale-card substitutions [Q-7] | Accepted; the review cards get redrawn | Built (spec §2.1) |
| Kerr mass mechanic [Q-8] | Keep | Built |
| Extractor Module [Q-9, Q-22] | GT extractor recipes only | Built (A-1, follow-up pass) |
| Upgrades [Q-10] | Consumed, permanent, milestones + install run | **Install run not built (A-2, B-1)** |
| Eye name [Q-13] | "Eye of Harmony" at every tier for now; per-tier names later | Built (name set in one place) |
| Compression gate [Q-20] | Hard gate, 1:1 | Built |
| Reuse the EOH and BHC blocks [Q-16] | Yes | Built (spec FC-1) |
| Kerr lining [Q-16, B-12] | One new white Kerr lining block | Built: Harmonic Lining, `slCasing2:2` (B-15) |
| Planets and compression [Q-29, B-4] | Drop TecTech's per-planet rule; the 1:1 gate alone decides | Built (B-16) |
| Kerr T2 with the reinforcement torn down [B-1] | Won't form | Built (unchanged) |

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

**Kerr redesign questions (merge of `kerr-redesign`, B-58).** The branch numbered its questions B-33..B-44, which
master had already used for the RC-2 package and D-49 entries. They are now B-46..B-57: old B-33 -> B-46, B-34 -> B-47,
B-35 -> B-48, B-36 -> B-49, B-37 -> B-50, B-38 -> B-51, B-39 -> B-52, B-40 -> B-53, B-41 -> B-54, B-42 -> B-55,
B-43 -> B-56, B-44 -> B-57. Commit messages on the branch keep the old numbers; docs and code comments use the new ones.
