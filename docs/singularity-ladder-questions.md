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
     is unique to this mod.
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
