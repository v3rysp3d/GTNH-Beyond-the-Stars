# Singularity Ladder: open questions logged during the build

User ruling (2026-10-12): "design conflicts are fine to change around; don't block". Each entry below records a
conflict we hit, what we decided and built, what we suggest, and the alternatives. Nothing here blocks the build;
answer at leisure.
The spec these refer to is [`singularity-ladder-build.md`](singularity-ladder-build.md).

---

## Q-1 Raw Stellar Plasma Mixture is the pack's existing Raw Star Matter

**Context.** The cards treat "Raw Star Matter" (pack) and "Raw Stellar Plasma Mixture" (our Eye T1 output) as two
fluids. In the pinned jar, `Materials.RawStarMatter` is already named **"Condensed Raw Stellar Plasma Mixture"**.
TecTech's EOH is its only producer. The Godforge (fuel), EIC, mixer, nanite and plasma-mixer recipes consume it.

**What we built.** RSPM = `Materials.RawStarMatter`, and no new fluid. Eye T1 (Sol) is its source once D-28 removes
TecTech's recipes, so every pack consumer keeps a supplier.

**Suggestion.** Keep it this way. Two near-identical names for two fluids would confuse players, and a second fluid
would leave the Godforge without fuel after D-28.

**Alternatives.** A new `btsrawstellarplasma` fluid, plus a Sol by-product of Raw Star Matter for pack compatibility.

## Q-2 With the Homogeny on hold, Eye T2 is the only source of Excited Stellar Plasma

**Context.** D-41/D-42 make Eye T2 optional because the Homogeny makes ESPM at 100 : 1. The Homogeny is on hold, and
Eye T3's Arcturus recipe consumes ESPM.

**What we built.** Eye T2 (Sirius A) makes ESPM at **10 : 1** from RSPM, so it is "10-100× better" than the Homogeny
would be. For now it is the de-facto required path to T3. The T2 band is still structurally independent of T3, so
nothing has to be torn down once the Homogeny ships.

**Suggestion.** Accept it as interim. When the Homogeny is ruled on, it slots in as the 100 : 1 alternative.

**Alternatives.** A temporary 100 : 1 mixer recipe on GT's Mega Mixer, or on our Kerr, until the Homogeny exists.

## Q-3 Removing TecTech's EOH recipes removes the pack's only WDM, BDM, Universium and Raw Star Matter

**Context.** From javap: TecTech's EOH is the only producer of all four. WDM, BDM and Universium come from its
`SPECIAL_FLUIDS`: rocket tiers 0-2 make WDM, 3-5 make BDM, and 6-9 make Universium. Consumers in the pinned jar:

- EIC magneto recipes and WDM/BDM/Universium nanites (`ElectricImplosionCompressorRecipes`, `NaniteChain`)
- BEC metamaterial recipes
- Nano Forge, Exo-Foundry and CoAL misc recipes
- Godforge upgrade costs (WDM/BDM/Universium frames on late upgrades)
- research-station UXV component recipes (Universium)

Under D-42, WDM returns at our Eye T4 (P3), BDM at T5 (P4) and Universium at T10 (P5). D-45/D-48 already plan to
take Universium out of UXV parts and to move the Eternity QFT loop onto Ergomatter, but those pack-recipe swaps are
not in P1-P3.

**What we built.** `replaceTecTechEyeOfHarmony` defaults **true** (D-28 is settled), and Raw Star Matter returns at
our Eye T1. An **interim** T4 recipe, "Sirius B, Cooled", makes 4,608 L of molten BDM from WDM + Super Coolant, behind
`interimBlackDwarfAtEyeT4` (default true), so BDM consumers are not stranded until Eye T5 ships. Universium has **no**
interim source, by D-48's design.

**Suggestion.** Keep the interim BDM recipe until P4. Ship the D-45/D-48 swaps (UXV parts without Universium,
Eternity on Ergomatter) in the same release as P4 or earlier, because until then any player past UXV parts is
stalled.

**Alternatives.**

- (a) Default the removal flag to **false** until P4/P5 land, and treat D-28 as staged.
- (b) Keep TecTech's planet recipes for rocket tiers 6-9 only, as a Universium trickle.
- (c) Accept the stall on a private test world.

## Q-4 Our Eye returns no EU; TecTech's EOH was a net power source

**Context.** TecTech pays back `euOutput = start cost × (0.6 + R/10)`, so R5 and above were net positive. D-41 makes
the Quasar the power machine.

**What we built.** Our Eye is a pure EU sink: a wireless debit per run, and no return.

**Suggestion.** Keep it. The player loses an EOH power loop that D-32 says should be the quasar's job.

**Alternatives.** Return a fraction (for example 50 %) of the run's EU at T3+ as a bridge until the Quasar ships in
P4.

## Q-5 How Eye T1-T4 are powered

**Context.** mine.md Q2 is open: the Kerr beam and quasar disc no longer power T2/T3.

**What we built.** Wireless EU for T1-T4, paid as one lump at run start. Per-run EU = recipe EU/t × ticks ×
parallel, with RECIPE_UMV ×1 / ×4 / ×16 / ×64 for T1 / T2 / T3 / T4. A Sol run is about 3.0 × 10¹² EU.

**Suggestion.** As built. Stabilisation and burst power begin at T7 (D-42).

**Alternatives.** Laser target hatches on the plinth. Or a per-tick draw (needs hatches and a long run, so more
babysitting).

## Q-6 Eye output amounts (D-31 is open): proposed numbers

**What we built (PROPOSED).** Per run, each star recipe consumes the previous star's product 1 : 1:

- Sol: 1e9 L H + 1e9 L He → 1,000,000 L RSPM, 20 min.
- Sirius A: 1,000,000 L RSPM + 1e9 L He → 100,000 L ESPM, 40 min.
- Arcturus: 100,000 L ESPM + 2e9 L H → 50,000 L RHSP, 80 min.
- Kerr densify: 10,000 L RHSP → 1,000 L HSM, 20 s.
- Sirius B: 5,000 L HSM + 4e9 L He → 18,432 L WDM, 120 min. That is the same per-run figure as TecTech's best WDM
  recipe (R2), at about 5× its unaccelerated rate.
- Planets: TecTech's per-dimension ores and plasmas, unchanged, on the time ladder 20 min × 1.25^R.

Parallel = 2^(installed tier − recipe tier).

**Suggestion.** Play-test, then tune one knob per tier.

**Alternatives.** Give WDM a much higher rate so it is not scarce before P4. Or scale all star outputs by
TecTech's maximum time-acceleration factor.

## Q-7 Stale cards: loops and later-tier inputs we substituted

**Context.** Several cards predate D-42, or use materials that only exist later. Kerr T1 is UIV (D-44), so its
blocks must be UIV-craftable.

**What we built** (spec §2.1):

| Card | Problem input | Replaced with |
|---|---|---|
| Superradiant Mirror Segment | BDM, WDM, WDM lens | dropped; now SpaceTime + Infinity |
| Eye Shell Casing, Stabiliser Pylon, Bastion Plate, Beam Dump | BDM | Neutronium / Cosmic Neutronium |
| Kerr Density Upgrade | dense BDM | dense Infinity |
| Harmonic Lining | WDM, BEC | Infinity / Transcendent Metal on the Assembler |
| Jet Crescent Casing | WDM, Jet Collimator Ring | dense Infinity, 2 Ergosphere Field Coils |
| Galactic Shell Plating, Galactic Upgrade Core | WDM | **Hyperdense Stellar Mixture**, the new T4 gate (the core also drops GRB ports, condensates and Metamaterial) |
| Bastion Plate | MagMatter | Transcendent Metal |
| HSM process | Ergomatter | Molten SpaceTime |
| Eye controller | 4M L Raw Star Matter | dropped (the Eye would need its own output) |
| Ergosphere Field Coil | UMV superconductor | UIV superconductor |
| Spatial Glass | Temporal fluid | SpaceTime |
| Beam Band, Jet Crescent | Beam Port | UMV Emitter |
| every BEC "Observation Array" card | BEC | Assembler / assembly line, because the condensate API isn't verified on the 5.09.52.594 pin |

**Also changed.**

- Structure blocks are batch-crafted on the Assembler. The structures need thousands of each (for example 2,852
  mirror cells and 2,971 beam-band cells), so one assembly-line craft per block is unreasonable.
- The card "Superradiant Mirror Segment" stays an assembly-line **component** (8 per Kerr). The wall block is a
  cheaper "Superradiant Mirror Casing".
- The Eye controller keeps the Gallifreyan (Tier-8) field generators. Their TecTech recipes showed no
  WDM/BDM/Universium/Eternity/MHDCSM markers, and the Eye unit re-verifies.

**Suggestion.** Accept the table, and re-draw the cards on the review page to match.

**Alternatives.** Per item. The likeliest change is a different material for the T4 gate than HSM.

## Q-8 The Kerr in P2 is fabrication only: no beam, no Ergomatter, mirrors are structural

**Context.** D-27's Kerr physics output a particle beam and Ergomatter. D-41/D-44 made the Kerr fabrication, and
D-42 moved Ergomatter to Eye T7.

**What we built.**

- The hole keeps its mass physics: Hawking bleed, a governor firing bolts from private bolt buses, injector-rail
  caps of 4/8/16 bolts per tick, and a setpoint of 2-64 SM (T2: up to 512 SM).
- Mass and heat become a trade-off. Parallel = 8 × voltage tier × ⌊√M⌋. Speed = min(4, (3200/M)/50), so a small
  hole is fast but bolt-hungry, and a big hole is wide.
- The mirror cavity, spin and beamline are structure only.

**Suggestion.** Keep this for P2. Revisit the cavity gain (for example as a speed bonus) when P4 adds the Quasar.

**Alternatives.** Drop the mass mechanic and copy the BHC's seed-and-stability rules exactly.

## Q-9 What the Kerr Extractor Module does

**Context.** D-42: "require a module to do compressing and/or extraction". The BHC has no extraction recipes, and
the card says "pulls matter back out of the ergosphere" without naming a product.

**What we built.** The Extractor Module runs GT's `extractorRecipes` and `fluidExtractionRecipes` at Kerr parallels.

**Suggestion.** Fine for P2. Give it an addon-specific extraction product in a later phase (for example a P4
by-product).

**Alternatives.** A new map with no recipes yet. Or merge both modules' maps into one.

## Q-10 Modules and upgrades in the "one controller" pattern

**What we built.**

- **Module cards become blocks.** The "Kerr Compressor Module" and "Kerr Extractor Module" cards craft the module
  **core block**, one per arc. The arc is an optional checked piece of the Kerr controller (the D-22 pattern), not a
  docked machine.
- **Upgrades are consumed.** The Kerr Density Upgrade and the Galactic Upgrade Core are consumed on install
  (sneak-right-click the controller), are irreversible, and survive a wrench.
- **T2 is a commitment.** If the T2 reinforcement is missing after the Density Upgrade is installed, the Kerr does
  not form.

**Suggestion.** As built.

**Alternatives.**

- Upgrades as removable items in the controller slot, so a downgrade is possible.
- With the upgrade installed but the reinforcement missing, the Kerr falls back to running as T1.

## Q-11 Eye T2 and T3 are unlocked by building the band, not by an upgrade core

**Context.** D-34 says each band is "unlocked by an upgrade core", but cores exist only for T4, T7 and T10. D-42
makes T2 optional, so T3 cannot require the T2 band.

**What we built.**

- T2 recipes need the beam band and T3 recipes need the jet crescent. Each band is an independent optional piece,
  and the band's casings are the cost.
- T4 needs the jet crescent, the spiral-arm blades and an installed Galactic Upgrade Core.
- The band names stay Planetary/Galactic in the code. mine.md Q5 proposes Stellar · Systems · Galactic · Universal.

**Suggestion.** Keep T2/T3 core-free, and rule on the band names.

**Alternatives.** Add a "Stellar Upgrade Core II/III".

## Q-12 What "planets without the matters" strips, and the rocket-tier mapping

**What we built.**

- Our Eye converts TecTech's per-dimension recipes. It keeps the ores and plasmas, and drops WDM, BDM, Universium
  **and** Raw Star Matter. Sol is the only RSPM source, which gives the T1 star recipe a reason to exist.
- Rocket tiers map as R0-R3 → T1 (R0 included because bh-model's T1 band starts at R0), R4-R6 → T2 and R7-R8 → T3.
- The Deep Dark (TecTech's special R9) → T4, with no Universium.

**Suggestion.** As built.

**Alternatives.** Let planets keep TecTech's Raw Star Matter as a minor by-product.

## Q-13 Names and star picks

**What we built.**

- Star picks: Sirius A (T2) and Arcturus (T3), from mine.md. **Sirius B** is the T4 white dwarf: the best-known white
  dwarf, and Sirius A's companion.
- The intermediate is named "Raw Hyperdense Stellar Plasma" (cards and questbook). D-42's text says "raw hyperdense
  star plasma".
- Our controller displays as "Eye of Harmony", the same name as TecTech's museum piece. The tooltip tells them
  apart.

**Suggestion.** Confirm, or rename ours (for example "Stellar Eye of Harmony") so NEI searches don't show two
"Eye of Harmony" controllers.

## Q-14 The TecTech EOH controller's own recipe is removed too

**Context.** D-28 says "our planetary EOH replaces its recipes", which leaves open whether the controller's own
recipe goes as well.

**What we built.** Under the same flag, we also remove TecTech's EOH **controller** recipe (research-station assembly
line), so nobody crafts a machine that can't run. Placed ones remain as museum pieces.

**Suggestion.** As built.

**Alternatives.** Leave it craftable as a decoration.

## Q-15 Textures are procedural, and the three new fluids share GT's molten texture

**Context.** I asked for real textures on every new item, fluid, casing and controller, in the EOH / Black
Hole Compressor families. FC-2 registers the three new fluids (ESPM, RHSP, HSM) with GT's tinted
`molten.autogenerated`, like every other BTS fluid.

**What was built.** `tools/gen-sl-textures.js` draws all 33 PNGs from code (16 casings, 8 overlay files for the Kerr
and Eye controllers, 9 items). Kerr casings use the Extreme Density / Radiation Absorbent / Hawking palette sampled
from the GT jar; Eye casings (`sl_ring2`, `sl_ring3`, `sl_galactic`) copy TecTech's EOH casing bevel and blues. The
Kerr overlay keeps `OVERLAY_MULTI_BLACKHOLE`'s edge bars around a black hole with a disc. The Eye overlay is an
`EM_CONTROLLER`-style blue screen with an eye. No fluid PNGs ship: the fluids differ only by tint
(ESPM `ffc23a`, RHSP `d2482a`, HSM `9a1a3a`, the review page's colours). RHSP and HSM are both reds, but RHSP is
orange-red and HSM is crimson.

**Suggestion.** Keep the procedural set for P1-P3 and hand-polish only what shows up most in play: the two controller
faces and the mirror and Bastion walls. If the fluids should look distinct beyond colour, the generator can add
greyscale animated still and flowing strips. GT tints those by the registered RGBA, and
`SLFluids` would switch to `withStillIconResourceLocation` / `withFlowingIconResourceLocation`.

**Alternatives.** (a) Hand-drawn textures from a ruling or an artist; the file names stay the same. (b) Reuse GT's own
textures for some casings (for example the mirror as a recolour of `RADIATION_ABSORBENT_CASING`). That ties them to
GT's art, but it also ties them to GT texture changes.

## Q-16 Which spec blocks became existing EOH / BHC blocks (block-reuse ruling)

**Context.** Mid-run user ruling: "use the eye of harmony blocks that currently exist as we want to try keep
everything similar", and prefer the Black Hole Compressor's blocks for the Kerr. The spec had 20 new block metas.

**What we built** (Foundation, build spec "Contract additions FC-1"). Four roles use existing blocks and are not
registered; their metas stay as reserved holes so nothing renumbers:

| Role (voxel kind) | Eye | Kerr |
|---|---|---|
| Containment Deck Plating (`deck`) | Reinforced Temporal Structure Casing | (no deck cells) |
| Eye Shell Casing (`eyeshell`) | Reinforced Spatial Structure Casing | Extreme Density Space-Bending Casing |
| Harmonic Lining (`lining`) | Infinite Spacetime Energy Boundary Casing | Background Radiation Absorbent Casing |
| Spatial Glass Channel (`sglass`) | Quantum Glass (TecTech) | Hawking Radiation Realignment Focus |

New blocks remain only where nothing existing fits: the Kerr's mirror, mirror backing, three injector rails, field
coil, quadrupole, beam dump, Bastion, Casimir, extraction casing and the two module cores, and the Eye's three
per-tier band casings (Beam Band, Jet Crescent, Galactic Shell). Recipes SL-P01/P11/P12/P13 are retired, and the
Stabiliser Pylon (SL-P21) now takes TecTech's EOH casings in their place.

**Open points.**

- The Kerr's lining was a bright white inner face in the mockup; the BHC has no white casing, so it reads darker
  in-game. The EOH's Infinite Spacetime Energy Boundary Casing is the closest look, but EOH casings are post-UMV and
  the Kerr is UIV.
- TecTech's tiered field generators (Spacetime Compression / Time Dilation / Stabilisation, 9 tiers each) have no
  voxel kind in the mockup yet, so no piece uses them. A natural mapping: pylon cores = compression, rails = time
  dilation, clamps = stabilisation, with a minimum tier per Eye tier (for example T1 needs tier 5, T4 needs tier 8).
  That needs a new shape char from the exporter and a call from the Eye unit.

**Suggestion.** Accept the table. Rule on whether the Eye should demand field-generator tiers.

**Alternatives.** Keep a new "Harmonic Lining" only for the Kerr's look, at the cost of one more block.

## Q-17 Assembler recipes are limited to one fluid

**Context.** GT's Assembler map is `maxIO(9, 1, 1, 0)`: one fluid input. Four §2.2 cards had two fluids.

**What we built.**

- SL-P03 (Superradiant Mirror Casing) takes its SpaceTime as 2 SpaceTime foils.
- SL-P07 (Injector Rail Mk III) drops the Lubricant and keeps the molten SpaceTime.
- SL-P16 (Jet Crescent) keeps Raw Star Matter as the fluid, because it is the gate, and takes the 4,608 L of SpaceTime as
  32 ingots.
- SL-P17 (Galactic Shell) keeps Hyperdense Stellar Mixture, because it is the T4 gate, and takes the 1,152 L of
  Transcendent Metal as 8 ingots.

**Suggestion.** As built.

**Alternatives.** Move those four to the assembly line (4 fluids), at the cost of one AL craft per batch.

## Q-18 Dense SpaceTime plates may not exist in this pack

**Context.** No GT, TecTech or dreamcraft recipe in the pinned jars uses `plateDense` SpaceTime, so GT probably
doesn't generate it. Dense Neutronium, Infinity and Transcendent Metal are all used by GT, so they exist.

**What we built.** Every dense plate in the Foundation recipes goes through
`BTSRecipesSingularity.dense(material, n)`. It returns the dense plate if GT has one. Otherwise it returns 9 plates
per dense plate, in stacks of at most 64, and logs a single warning for that material. SL-P15 and SL-P23 are affected.
The helper is public so the Kerr (SL-K01) and Eye (SL-E01, SL-E02) units can use the same rule. Note: ×32 or ×64
dense SpaceTime becomes 288 or 576 plates, which is 5 or 9 extra assembly-line slots.

**Suggestion.** Read the boot log once. If the warning appears, swap the big AL inputs to `plateSuperdense`
SpaceTime, or to ingots, rather than to plate stacks.

**Alternatives.** Ask the pack to generate dense SpaceTime.

## Q-19 The mockup's bands cut into each other at the joins; the earlier piece keeps those cells

**Context.** The spec (§5.2) asserted that no Eye tier changes a cell of an earlier tier. The headless export finds
three places where `designs/voxel.js` breaks that:

- the T2 beam-band clamps start inside the upper rail (115 cells);
- the T3 jet-crescent clamps start inside the lower rail (100 cells);
- the T4 spiral-arm blades start 1.8 blocks outside the core band's centreline, with a 7.5-block half-width, so their
  roots cut into the core band (84 cells).

On the Kerr, the compressor arc is drawn over 40 cells of the r 33 frame ring and the extractor arc over 9.

**What we built.**

- **Eye:** the earlier piece keeps every such cell, and the later piece starts where it ends. `eye_core` is
  identical whatever bands are built, and each band piece only adds cells. The arm roots butt against the core band
  instead of sinking into it.
- **Kerr:** per §5.2, the last writer owns the cell, so those 49 cells belong to the module. Without the module, the
  frame ring has a gap in that arc.

**Suggestion.** As built. The visible difference is a few blocks at each join.

**Alternatives.**

- Let the later band own its join cells. The earlier piece would then need "either block" elements at those cells,
  which is more machinery.
- Adjust the mockup so the arm roots start outside the core band.

## Q-20 Where TecTech's field generators go in our Eye, and what their tiers do

**Context.** The mid-run ruling says to reuse TecTech's EOH blocks, including the Spacetime Compression, Time
Dilation and Stabilisation field generators, and to "keep the field-generator tiers meaningful where it fits the band
tiers". The mockup has no field-generator kind. TecTech's own EOH uses 138 compression, 168 dilation and 48
stabilisation generators.

**What we built** (shape chars only; the Eye unit owns the elements and the mechanics):

- `W`, Spacetime Compression: the six T1 clamp struts between the upper rail and the core band, 188 blocks.
- `X`, Time Dilation: the six struts between the core band and the lower rail, 138 blocks.
- `Y`, Stabilisation: the six 7 × 7 pylon foot pads, 294 blocks. The Stabiliser Pylons stand on stabilisation
  fields.
- Only the open span between bands counts as a strut. Clamp roots inside a band stay band shell.

All three sit in `eye_core`, as in TecTech's single-structure EOH, so a T1 Eye already has all three.

**Suggestion.** Keep TecTech's semantics, which the player already knows:

- The lowest compression tier caps which planets run, by rocket tier.
- The dilation tier buys speed.
- The stabilisation tier raises planet success.

Stars ignore all three. Each type uses `ofBlocksTiered`, and mixed tiers count as the lowest, or as invalid like
TecTech's.

**Alternatives.**

- Accept any tier, with no effect (decorative). This is the least machinery.
- Gate each Eye tier on a minimum generator tier, for example T2 needs Stable, T3 Superb and T4 Perfect.
- Move the generators onto the band clamps instead: compression on the T1 clamps, dilation on the T2 beam clamps and
  stabilisation on the T3 jet clamps. This ties each generator type to a band, but a T1 Eye would have only one type.

## Q-21 The Kerr's eight "maintenance" cells beside the bolt buses become optional BHC utility-hatch sockets

**Context.** The mockup puts a `maint` cell beside each of the 8 bolt buses. The spec (§5.3) made them plain casing,
because 8 maintenance hatches would be noise. The mid-run ruling prefers the Black Hole Compressor's own blocks and
utility hatch for the Kerr. The BHC utility hatch (`MTEBlackHoleUtility`, id 3012) is not an item bus: it emits
redstone from the hole's state.

**What we built.** Those 8 cells are `u` sockets in `kerr_t1`/`kerr_t2`. Each accepts a BHC utility hatch, with a
minimum of 0, and falls back to Extreme Density Space-Bending Casing. The bolt buses (`b`) stay a private input-bus
list.

**Suggestion.** The Kerr unit drives each placed utility hatch from the hole's state (open, mass below setpoint,
bolts short), the way the BHC drives it from its stability. If that is more than P2 wants, accept the hatch and
leave it inert for now.

**Alternatives.** Make the cells plain casing as §5.3 had it (then `u` maps straight to `A`), or accept the utility
hatch only on the governor arc.
