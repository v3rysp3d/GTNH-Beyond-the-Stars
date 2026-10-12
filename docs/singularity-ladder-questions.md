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

## Q-22 The Kerr Extractor Module runs its own recipe map, and for now it only melts exotic metals

**Context.** Q-9 had the extractor module run GT's `extractorRecipes` and `fluidExtractionRecipes`. The Kerr build
task asked for the extractor module "on its own new recipe map". Nothing in the design names an extraction product:
the card only says "pulls matter back out of the ergosphere".

**What we built.**

- A new map, `beyondthestars.recipe.kerrextraction`, shown in NEI as "Ergosphere Extraction". The Kerr is its
  catalyst.
- It holds SL-K06: Neutronium, Cosmic Neutronium, Infinity, Transcendent Metal, SpaceTime, White Dwarf Matter and
  Black Dwarf Matter, as ingot, plate, dense plate and block, melted to their molten fluids. The rate is GT's standard
  144 L per ingot, at UEV and 1 s per ingot (8 s per dense plate or block).
- A form that GT never generated is simply not offered, so the boot log stays at "0 skipped".
- This is neutral by construction: every solidifier runs the other way at the same rate. It adds throughput, not
  value.

**Suggestion.** Keep the map. Give it a real Kerr-only product in P4 (for example a by-product of the hole's mass).
Until then the melts are harmless and give the module a job.

**Alternatives.**

- Run GT's extractor and fluid-extractor maps as Q-9 said (the map stays for later products).
- Mirror GT's extraction recipes into this map at load-complete. This would duplicate a few thousand NEI entries.
- Leave the map empty until a product is decided. The module would then be pure decoration.

## Q-23 Each Kerr module has its own private sockets; only the active mode's module feeds GT's recipe lists

**Context.** §3.1 put the module I/O sockets into GT's standard lists (`mInputBusses` and the others). The Kerr task
asked for private hatch elements per module.

**What we built.**

- `KerrHatchElement` has private lists for the bolt buses, the BHC utility hatches, the compressor's inputs and
  outputs, and the extractor's inputs and outputs.
- Before every recipe search, `bindModuleIO` copies only the current mode's module into GT's lists.
- A recipe that is already running keeps the module it started on, even if the structure is re-checked.

So the compressor never eats the extractor's inputs, a densify output never lands in the extractor's hatch, and
recipes never touch the bolt buses.

**Suggestion.** As built.

**Alternatives.** One shared I/O list for both modules, as §3.1 had it. This is simpler, but the modes then compete
for the same inputs.

## Q-24 Upgrades are installed with a plain right-click, not a sneak-right-click

**Context.** §3.1 says to sneak-right-click the controller holding the Kerr Density Upgrade. Minecraft 1.7.10 never
activates a block for a sneaking player who is holding an item (`PlayerControllerMP` skips `onBlockActivated` unless
the item says it bypasses sneak). A sneak-click with the upgrade would therefore do nothing.

**What we built.** A plain right-click while holding a Kerr Density Upgrade installs it. One upgrade is consumed (none
in creative), the structure is re-checked, and the GUI does not open. Any other right-click opens the GUI as usual.

**Suggestion.** As built. The Eye unit should use the same gesture for the Galactic Upgrade Core.

**Alternatives.** An "install" slot or button in the controller GUI.

## Q-25 Kerr hole details the spec left open

**What we built** (every number is still PROPOSED, Q-8):

- **Speed is clamped to ×1..×4.** The §3.1 formula, time ÷ min(4, E/50) with E = 3200/M, divides by less than 1
  above 64 SM, which makes a 512 SM hole 8× *slower*. A big hole would then be strictly worse than a small one. We
  clamp the divisor to at least 1.

  Throughput (parallel × speed) peaks around 16 SM (×16 over the base). It is ×8 at 8 SM and at 64 SM, and ×22 at
  512 SM.
- **Closed hole:** only plain compressor recipes run, at 8 × voltage tier. Heated (HIP) and black-hole recipes
  (`COMPRESSION_TIER` ≥ 1) need the hole open. The neutronium map is searched only while the hole is open. The BHC
  refused everything while it had no hole, so this is a little kinder.
- **Screwdriver:** a plain click steps the setpoint, and sneak + screwdriver cycles the mode. The GUI's machine-mode
  button also cycles the mode, and the cycle skips modes whose module is missing. The default setpoint is 8 SM.
- **While the structure is broken, the hole is frozen:** there is no bleed and no governor. It does not evaporate.
- **A wrench carries everything persistent:** the upgrade, the hole and its mass, and the setpoint. The seed was
  already spent, so carrying the hole only hands property back.
- **Seeds and collapsers** are checked once a second. The governor and the bleed run every tick.
- **Bolt buses** must be plain input buses. An ME stocking bus is refused, because its slots are shadows the governor
  cannot decrement.

**Suggestion.** As built. Tune the numbers in the smoke test.

**Alternatives.** Apply the §3.1 speed formula unclamped, so a big hole is a pure parallel play. Or let the hole keep
bleeding while the structure is broken.

## Q-26 Injector rails can be mixed; the lowest tier sets the bolt cap

**Context.** §5.3 says `ofBlocksTiered`. StructureLib's tiered element demands one tier across the whole structure, so
a player upgrading 232 rails from Mk I to Mk II would un-form the Kerr at the first swap.

**What we built.** The rail element accepts Mk I, Mk II or Mk III in any cell. The walk records the lowest tier it
meets, and that tier sets the bolt cap (4, 8 or 16 a tick). The hologram and the creative build place Mk I.

**Suggestion.** As built. This matches §3.1's "the lowest injector-rail tier found in the walk".

**Alternatives.** Uniform tiers (`ofBlocksTiered`). These are stricter and match how GT treats coils.

## Q-27 The BHC utility hatch shows the hole's state as redstone

**Context.** Q-21 left open what the eight optional `u` sockets do.

**What we built.** Each Black Hole Compressor Utility Hatch on the cage is set every second to redstone **on while the
hole is open** and off while it is closed (`MTEBlackHoleUtility.updateRedstoneOutput`, as the BHC drives it). They are
optional (minimum 0) and never join a recipe list.

**Suggestion.** As built. Possible later signals: mass below the setpoint, or bolts short of the cap.

**Alternatives.** Make the cells plain casing (then `u` maps to `A`).

## Q-28 The Kerr's creative test path

**What we built.**

- **`/btskit kerr`** gives the controller, both module cores, Mk I rails, a Density Upgrade, two seeds and a collapser,
  bolts, and hatches. The number of rails comes from the shape file. It also prints every piece's bill of materials
  from `manifest.json`.
- **`/btskerr [t1|t2] [force]`** raises a complete Kerr in front of the player:
  - The controller faces the player, 32 blocks above their feet, so the cage's bottom row is at foot level.
  - It places every block and fits a hatch in every socket: creative UMV 16 A energy, auto-maintenance, MAX buses
    and hatches on both modules, and bolt buses stocked with a seed and SpaceTime bolts.
  - With `t2` it installs the Density Upgrade, then forms the machine and prints its status.
  - It refuses to build into a footprint that is more than 15 % solid, unless `force` is given.
- The hologram projector works too: a stack of 1 projects T1, 2+ projects T2, and both modules are always projected.

**Not verified in game.** No in-game smoke test has run yet. In particular, the structure orientation (the
`orient_test` piece) is unverified. If `/btskerr` reports NOT FORMED with a first wrong cell on the mirrored side,
re-export with `--flipH` or `--flipD`, as the Structure export section says.

**Suggestion.** Run `/btskerr` and `/btskerr t2` in the test instance as the first smoke test of P2.

## Q-29 What the Eye's field generators do, and that tiers can be mixed

**Context.** Q-20 put TecTech's three field-generator types into `eye_core` (`W` compression on the upper clamp
struts, `X` time dilation on the lower struts, `Y` stabilisation on the pylon foot pads). I then ruled (Q-20,
two rounds): compression **hard-gates** the Eye's tier, **1:1**; Time Dilation and Stabilisation are separate
upgrades whose mechanics are not decided, so they are placeholders with no effect.

**What we built** (rebuilt in the review fix pass; the first build used TecTech's planet-only maths):

- **Spacetime Compression is the tier gate.** Eye tier N runs only if every compression generator is TecTech tier N
  or higher. The mapping is ONE table, `MTEEyeOfHarmonyBTS.COMPRESSION_META_FOR_TIER = {-, 0, 1, 2, 3}` (generator
  meta = TecTech tier - 1). `tierAvailable`, `installedTier` and the parallel all follow from it. A gate failure has
  its own message ("every Spacetime Compression Field Generator must be tier N or higher").
- **Planets keep TecTech's own compression rule on top:** the lowest compression tier must reach the planet's
  `getSpacetimeCasingTierRequired()`, and each tier above that cuts 3 % of the time. The two rules do not conflict,
  because the gate is a minimum: a T3 Eye may carry tier-7 compression. They do interact: a T3 Eye at exactly the
  T3 gate (tier 3) cannot run TecTech's R7-R8 planets, which need tier 7-8 compression.
- **Time Dilation and Stabilisation: placeholders.** Any tier is accepted and shown in the scanner; neither changes
  time, success or yield (TODO(Q-20) in code). Planet success is TecTech's base chance; yield is 1.
- **Holograms and builds:** the compression cells place the tier the trigger stack asks for (stack 1..4 = tier 1..4),
  read from the trigger only. Dilation and stabilisation place tier 1. `/btseye gen <1-9>` sets all three, and
  raises compression to at least the asked Eye tier's gate.

Any tier 0-8 of each type is accepted in any cell, and the **lowest** tier of each type counts (as the Kerr's
injector rails, Q-26).

**Suggestion.** As built. Decide in the pending design session what dilation and stabilisation do, and whether the
planet compression requirement should stay once the 1:1 gate exists.

**Alternatives.**

- Drop TecTech's per-planet compression requirement and let the 1:1 gate alone decide (R7-R8 planets would then run
  on a T3 Eye with tier-3 compression).
- Uniform tiers, as TecTech does (`ofBlocksTiered`).

## Q-30 The Eye has one set of fluid buffers, and every input socket feeds it

**Context.** §3.3 has gas sockets (H/He) on the plinth and stellar sockets (RSPM, ESPM, HSM, WDM) on the beam band and
jet crescent, drained into separate buffers.

**What we built.**

- **One buffer per fluid**, as a `long` capped at 2⁶² L.
- **Every 20 ticks**, every live socket is drained of any fluid that some Eye recipe consumes: H, He, Raw Star Matter,
  ESPM, HSM, molten WDM and Super Coolant (the interim recipe's coolant).
- **Socket kinds are interchangeable.** The gas sockets (`h`) and the stellar sockets (`s`, `t`) differ only in where
  they sit, so a T1-only Eye can take RSPM through its plinth.
- **ME stocking input hatches work.** They are drained between `startRecipeProcessing` and `endRecipeProcessing`, as
  GT does for recipes.
- **Buffers persist.** They are saved with the tile and survive a wrench.

**Suggestion.** As built. It is less machinery, and the band sockets remain the natural place for stellar fluids.

**Alternatives.** Restrict each socket kind to its own fluids, as §3.3 had it.

## Q-31 How an Eye run is sized, paid and rolled

**What we built** (every amount is still PROPOSED, Q-6):

- **Parallel** = 2^(installed tier − recipe tier), reduced to what the buffers can pay for, with a minimum of 1. The
  installed tier is the highest tier whose bands stand. A T4 Eye therefore runs Sol ×8.
- **EU** = `RECIPE_UMV × 4^(tier−1) × base ticks × parallel`. It is debited from the owner's wireless network in one
  lump when the run starts. If the network can't pay, nothing starts. Dilation does not lower it.
- **Success** is rolled per parallel. A failed parallel consumes its inputs and EU and outputs nothing. Planets keep
  TecTech's base `1 − 0.05R`. Stars always succeed.
- **Outputs** go to the ME output bus and ME output hatch only. TecTech's long-count outputs are ejected in
  `Integer.MAX_VALUE` chunks, as TecTech does. If the ME network can't take them, they are lost.
- **Planet times** are bh-model's ladder, `20 min × 1.25^R`. The Deep Dark is 240 min.
- **No TecTech extras:** no astral-array parallels, no circuit-based speed, no overflow or pity rules, no SpaceTime
  failure penalty, and no EU return (Q-4).
- **Saving mid-run:** a run in flight is saved with the tile and completes after a reload. A wrench or a broken
  structure loses it (see `docs/migration.md`).

**Suggestion.** Play-test, then tune one knob per tier, as Q-6 says.

**Alternatives.** Roll once for the whole parallel batch (all-or-nothing). Or keep the inputs when a run fails.

## Q-32 Eye crafting recipes: what changed from the §2.4 cards

**What we built** (`BTSRecipesEye`):

- **SL-E01 (Eye controller):** follows FC-1's suggestion.
  - The retired Eye Shell ×64 and Containment Deck ×64 lines are dropped. The card already has Reinforced Spatial ×64
    and Reinforced Temporal ×64.
  - Harmonic Lining ×64 becomes TecTech's Infinite SpaceTime Energy Boundary Casing ×64.
  - The three field generators are **Tier 0 (Crude)**, not the card's Gallifreyan (Tier 8). (Changed in the review
    fix pass.) Each TecTech generator tier N is researched from tier N-1 at RECIPE_MAX with UXV circuits, so Tier 8
    ingredients forced all 24 research steps (3 types x 8) before a T1 Eye could exist. Under the 1:1 compression
    gate (Q-20) Gallifreyan is the T9 gate, so it would also unlock every later gate early. TecTech's own EOH
    controller recipe uses the Tier 0 generators too.
  - Research item: the **Stabiliser Pylon**, not the Reinforced Spatial Structure Casing. TecTech's own EOH
    research recipes already use that casing (registered first), and GT's scanner writes the FIRST assembly-line
    recipe whose research item matches, so SL-E01 could never be put on a data stick. SL-P21 (the Pylon) moved
    for the same reason, to Star Chart: Sol. Every SL assembly line now has a unique, addon-only research item
    (except SL-K01's Black Hole Seed, which no other pack recipe researches from), and `BTSRecipeGuard.checkResearchCollisions`
    logs an ERROR at load-complete if any of our research items is shared.
- **Dense SpaceTime** (×64 in SL-E01, ×32 in SL-E02) is chosen in this order:
  1. the dense plate, if GT has one;
  2. otherwise the same mass as SpaceTime **blocks** (9 ingots each, in one slot);
  3. otherwise the shared 9-plates fallback (Q-18).

  Blocks come before plates because the 9-plates fallback would push SL-E01 to 18 item slots, over the assembly
  line's 16.
- **SL-E02 (Galactic Upgrade Core):** as the card. Hyperdense Stellar Mixture 256,000 L is the gate.

**Suggestion.** As built. Read the boot log once to see which dense-SpaceTime form was used.

**Alternatives.** Superdense SpaceTime plates, if GT generates them.

## Q-33 TecTech's Eye is only removed if our copy of its planets succeeded

**Context.** §4 has `replaceTecTechEyeOfHarmony` clear TecTech's storage, NEI page and controller recipe after the
Eye's conversion snapshot.

**What we built.**

- The snapshot always runs.
- The removal runs only if the flag is on **and** the snapshot converted at least one planet **and** the replacement
  stars registered: Sol (Raw Star Matter), Sirius B (White Dwarf Matter) and, while `interimBlackDwarfAtEyeT4` is on,
  Sirius B Cooled (Black Dwarf Matter). Star fluids are looked up without throwing, so one missing GT fluid costs only
  the stars that use it, and then TecTech stays intact.
- If TecTech's private `recipeHashMap` has moved and the copy fails, TecTech's Eye is left fully intact and an ERROR
  is logged. Deleting every planet with nothing to replace them would be worse.
- Each step is contained, so one failed step never stops the boot.

**Suggestion.** As built.

**Alternatives.** Remove regardless, as §4 literally reads.

## Q-34 The Eye's centre render is not built yet

**Context.** §3.3 lists a client-side render of the band's object at the Eye's centre, toggled by `eyeRenderCentre`.
It is a stretch goal; the structure ships first.

**What we built.** Nothing yet. The centre stays empty, as the exporter leaves it (render-only cells are skipped). The
`eyeRenderCentre` flag has no effect for now.

**Suggestion.** Build it after the smoke test. Reusing TecTech's `eyeOfHarmonyRenderBlock` (`TileEntityEyeOfHarmony`)
would give the orbiting-planet look players already know, scaled to our 80-block ring.

**Alternatives.** Drop it and keep the Eye render-free.

## Q-35 The Eye's creative test path

**What we built.**

- **`/btskit eye`** gives the controller, a Galactic Upgrade Core and all five star charts. It also gives an input
  hatch for every gas and stellar socket, an ME output bus and hatch, auto-maintenance, and tier-9 field generators,
  in the counts the shape needs. It prints each piece's bill of materials from `manifest.json`.
- **`/btseye [t1|t2|t3|t4] [gen 1-9] [fill] [force]`** raises a complete Eye in front of the player:
  - the core, plus the bands for the asked tier;
  - field generators at the asked tier;
  - a hatch in every socket;
  - the Galactic core at T4, and the tier's star chart in the controller slot;
  - the player as owner.

  Options and limits:

  - `fill` tops every input buffer up to 10¹² L and gives the player's wireless network 10¹⁸ EU, so a run starts at
    once. It is an op-only creative cheat.
  - At T4 the arms reach 28 blocks in front of the controller, so the build stands further from the player.
  - It refuses to build into a footprint that is more than 15 % solid, unless `force` is given.
- **Hologram:** a projector stack of 1..4 projects T1..T4, with the bands cumulative.

**Not verified in game.** The structure orientation (`orient_test`) is still unverified, as for the Kerr. ME outputs
need an AE network on the ME output bus and hatch.

**Suggestion.** Run `/btseye t1 fill`, then `/btseye t4 fill`, as the first smoke test of P3.

## Q-36 Every SL assembly line has its own research item

**Context.** GT's scanner (`ScannerHandlerLoader.doAssemblyLineResearch`, checked with javap) writes a data stick for
the FIRST recipe in `sAssemblylineRecipes` whose research item matches the scanned stack. Two recipes sharing a
research item means the later one can never be put on a data stick. The first build had four such collisions:
SL-P04 and SL-K01 (Black Hole Seed); SL-K02, K03 and K04 (the Kerr controller); SL-P21 and SL-E01 (TecTech's
Reinforced Spatial Structure Casing, which TecTech's own EOH research already uses).

**What we built** (review fix pass):

| Recipe | Research item |
|---|---|
| SL-P04 Superradiant Mirror Segment | Superradiant Mirror casing |
| SL-K01 Kerr Ergosphere Engine | Black Hole Seed (unchanged) |
| SL-K02 Compressor Module | Kerr controller (unchanged) |
| SL-K03 Extractor Module | Extraction casing |
| SL-K04 Kerr Density Upgrade | Bastion casing |
| SL-P21 Stabiliser Pylon | Star Chart: Sol |
| SL-E01 Eye of Harmony | Stabiliser Pylon |
| SL-E02 Galactic Upgrade Core | Eye controller (unchanged) |

`BTSRecipeGuard.checkResearchCollisions()` runs at load-complete and logs an ERROR for any of our assembly lines whose
research item another assembly line in the pack also uses.

**Suggestion.** As built. Treat that ERROR line as a release blocker.

**Alternatives.** Dedicated research-only items (one "schematic" item per recipe), if I prefer a uniform look.

## Q-37 The Kerr re-checks its structure on a timer

**Context.** GT re-checks a multiblock only after a block update reaches the controller, and that update only walks
through registered machine blocks. Our SL casings were not registered (fixed: they are GT machine blocks now and
cause machine updates, like GT's casings). Even so, the Kerr's cage, arcs and modules are hundreds of separate
islands to GT's walk, so a module built or broken, rails swapped, or the last hologram block placed would not reach
the controller until a relog.

**What we built.** The Kerr re-walks its structure every 200 ticks while idle and every 1,000 while running. The walk
is skipped while any chunk within 96 blocks of the controller is unloaded, so a half-loaded cage never un-forms.

**Suggestion.** As built. The Eye relies on GT's update walk (its bands are now machine blocks); give it the same
timer if play-testing shows a stale band.

**Alternatives.** Shorter intervals (more responsive, more cost on a ~30,000-block walk), or a "re-check" screwdriver
action instead of a timer.

## Q-38 The upgrade install mechanic is routed but not built yet

**Context.** Q-10 (third round): the Kerr Density Upgrade and the Galactic Upgrade Core install through MILESTONES +
an INSTALL RUN, not a free right-click.

**What we built.** The Eye now has the one install path the ruling asks for: `beginGalacticUpgrade(player, stack)`
owns validation, consumption and feedback, and `completeGalacticUpgrade()` is the one place the upgrade takes effect
(the creative build calls it). Today `begin` still completes at once, marked TODO(Q-10). The Kerr's Density Upgrade
still installs on right-click through `installDensity()`.

**Suggestion.** Build the milestone check and the install run as their own task, inside `beginGalacticUpgrade` (and a
matching `beginDensityUpgrade` on the Kerr), reusing the progression/ milestone code.

**Alternatives.** None proposed; the mechanic is ruled.
