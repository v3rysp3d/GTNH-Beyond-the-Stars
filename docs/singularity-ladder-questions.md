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
