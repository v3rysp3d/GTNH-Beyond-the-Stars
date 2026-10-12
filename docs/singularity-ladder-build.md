# Singularity Ladder: build spec for P1-P3

Status: build spec, 2026-10-12. This covers P1 (Foundation), P2 (Kerr T1/T2) and P3 (Eye of Harmony T1-T4).
**ON HOLD, do not build:** the Homogeny (all tiers), Reality Diffraction and Reality Refab. P4 (Quasar, Conduit,
Eye T5-T8), P5 (MAX) and P6 (questbook) come later.

Sources: `PROPOSAL.md` D-27..D-48 (the latest ruling wins), `designs/docs/{yours,mine,multis,questbook}.md`,
`designs/recipes.js` (groups `kerr`, `eye1`, `eye4`), `designs/voxel.js` (`buildKerr`, the Eye `BANDS`) and
`designs/bh-model.js`. Pack facts come from `javap` on the pinned instance jars (gregtech-5.09.52.594,
GTNewHorizonsCoreMod-2.8.279), re-checked for the **GTNH 2.9.0-RC-2 port** against GT 5.09.54.205 and coremod
2.9.84 (see "GTNH 2.9.0-RC-2 port" below; where a section still cites the old jars, the port notes say what changed).
Every resolved conflict is logged in
[`singularity-ladder-questions.md`](singularity-ladder-questions.md). That file was consolidated after the build:
items A-1..A-3 are rulings the jar does not match yet, and B-1..B-14 are open decisions. Code comments still cite the
old Q-1..Q-38 numbers, and its last table maps them. This document is what gets built; the questions record what the
user still has to rule on.

Code references below use `<pkg>` = `com.nateyburr.beyondthestars`.

---

## GTNH 2.9.0-RC-2 port (2026-10-12)

User ruling: the addon targets **GTNH 2.9.0-RC-2** (DreamAssemblerXXL manifest 2026-10-04). This supersedes the D-06
beta-1 pin. The port plan with the full API, behaviour and id diff is the session's `port-plan.md`; this section is
what was built.

**Target versions** (`dependencies.gradle`):

| Dependency | Was (2.9.0-beta-1) | Now (2.9.0-RC-2) | How |
|---|---|---|---|
| GT5-Unofficial | 5.09.52.594 | **5.09.54.205** | `api(...:dev)` |
| StructureLib | 1.4.38 | **1.4.45** | `api(...:dev)` |
| NotEnoughItems / GTNHLib / ModularUI2 | 2.8.98 / 0.10.3 / 2.3.70 | 2.8.155-GTNH / 0.11.52 / 2.3.92 | GT's POM |
| NewHorizonsCoreMod | (instance only) | 2.9.84 | `runtimeOnlyNonPublishable`, dev runs only |
| BetterQuesting, BlockRenderer6343 | (instance only) | 3.8.89-GTNH, 1.4.24 | `runtimeOnlyNonPublishable`, `transitive = false` |
| Avaritia, Universal Singularities (+ WanionLib), Avaritiaddons, Eternal Singularity | - | 1.99, 8.15.4 (1.10.1), 1.9.6-GTNH, 1.4.3 | `runtimeOnlyNonPublishable`, `transitive = false`: they switch on GT's Eye of Harmony Condensate Assembler recipes and the coremod's BHC recipe in `runServer` |

The buildscript is unchanged (gtnhsettingsconvention 2.0.20, Gradle 9.3.1; 2.0.34 / 9.7.1 is an optional parity bump).
MTE ids 28100-28139 are still free in RC-2 (enum and bytecode scan).

**Compile breaks fixed (4 API areas, 45 errors):**

- `GTPPRecipeMaps` is gone; the six GT++ maps live in `gregtech.api.recipe.RecipeMaps` (names now `gt.recipe.*`):
  `BTSRecipesCoolant`, `BTSRecipesFuel`, `BTSRecipesPropellant`.
- `IOutputBusTransaction` is `IOutputTransaction<ItemId, ItemStack>`: `isFilteredTo`, `storePartial(id, stack,
  total, perParallel)`, `complete` (`MTEReactorCoreMk1.storeAll`).
- `MTEMultiBlockBase.dumpFluid(List, FluidStack, boolean)` is gone: the Eye dumps through `addOutputPartial(stack,
  mOutputHatches, false)` (GT's `FluidEjectionHelper`; locked / ME hatches and void protection are GT's).
- `MTEBlackHoleUtility.updateRedstoneOutput` is gone: the Kerr drives its utility hatches with the RC-2 BHC cadence
  (`MTEKerrEngine.pulseUtilityHatches`): hole open, `cycleStart()` every 20 ticks and `cycleMiddle()` 5 ticks later
  (a pulse-mode hatch gives a 5-tick pulse, continuous mode stays lit); hole closed, `blackHoleClosed()`.

**Behaviour changes handled:**

- **Reactor sockets vs crafting input buffers.** RC-2's `addInputHatchToMachineList` also accepts fluid-capable dual
  input hatches (CRIBs) into `mDualInputHatches`. The reactor's feedwater, chamber-coolant and propellant sockets
  now refuse anything that is not a plain `MTEHatchInput` before super (a CRIB there would form and feed nothing),
  and the Spiral's failed-walk rollback truncates `mDualInputHatches` too. Regression traps: the hint/autoplace
  path uses `FluidHatchElement.mteClasses` (unchanged, client-safe); no NBT involved; the plant-wide list now rolls
  back with the others; no chain fallback involved.
- **BHC to Kerr in recipes** (user ruling). In RC-2 the BHC controller is an ingredient: all 9 Spacetime Compression
  Field Generator tiers (Condensate Assembler) take one, and the coremod's Compressed Circuit Array takes 64. Under
  `removeBlackHoleCompressorRecipe`, `BTSBlackHoleSwap` rewrites every consumer at load-complete to take the Kerr
  Ergosphere Engine controller (same count, in place, so nanite tiers and condensates survive) in every GT recipe
  map, the assembly-line data and research lists and the crafting table, and logs each one
  (`[BTS] BHC->Kerr (load-complete): <map>: <output>`). The BHC recipe is removed only after that, only if SL-K01 is
  registered and no consumer is left; otherwise it is kept and the log says why. A server-start pass catches late
  consumers. Section 4.
- **SL-K01 is re-priced at BHC-controller parity** (B-30): each compression generator now costs one Kerr controller.
  Costs doc §9.
- **Gate audit** (WDM ruling, costs doc §9, B-29). RC-2's tier 1-3 compression generators (Eye T2-T4 gates) still
  need White Dwarf Matter, through the Resonance Chamber II they take (1 / 4 / 16, one WDM foil each). New
  **SL-P39**: a Condensate Assembler recipe for that chamber with 2 SpaceTime foils in place of the WDM foil (only with
  `replaceTecTechEyeOfHarmony=true`). **SL-P30..P32 are retired**: RC-2 prices the generators itself (BEC, no tier
  upgrade chain) and the ruling keeps those costs. SL-P27..P29 stay: RC-2's BEC casings still cost 8 Godforge casings
  per Spatial/Temporal casing and a whole LSC per Boundary casing.
- **TecTech Eye removal** is unchanged: the controller recipe is still a `TTRecipeAdder` assembly line, removed from
  all four places.

**Verified:** `spotlessApply compileJava` against the RC-2 jars, and a headless `runServer` (76 mods, coremod 2.9.84):
"[BTS] Recipe registration complete: 232 registered, 0 skipped"; TecTech's 43 Eye recipes and its controller removed
(AL-visual 1, AL-data 1, research 2); "BHC->Kerr: 10 recipes now take the Kerr controller" (Crude ... Gallifreyan and
the Empty Spatially Compressed Circuit Array); "removed Black Hole Compressor controller: AL-visual 1, AL-data 1,
research 0, scanner 1"; GT's assembly-line validation passes. The dev server lacks Railcraft and other pack mods, so
its recipe count is lower than the pack's (255 before the port, minus SL-P30..P32, plus SL-P39).

**RC-2 dev-server test** (questions B-32, fresh world, two restarts): 0 recipes still consume a BHC controller;
`/btseye gates` finds the Eye T1-T4 gates clean (through SL-P39) and the known P4/P5 loops at T5, T8 and T9; Kerr
T1 and T2 with both modules and Eye T1-T4 all FORMED in two facings, one creative and one survival, and still FORMED
after a restart; the reactor forms, and a CRIB in its feedwater socket is refused (the silo drops out, but no error
names the cell: B-32).

**Still to watch at RC-3:** `MultiblockTooltipBuilder` int-count overloads (91 deprecated calls) and
`Textures.BlockIcons.custom(String)` (8 calls) are deprecated; `MTEBlackHoleCompressor` and `MTEMultiBlockBase` keep
changing; the BEC costs were rebalanced between beta-3 and RC-2, so re-run the gate audit (costs doc §9) and re-check
the coremod's BHC entries on every bump.

---

## What's in the build (2026-10-12, addon 457bbf8)

### D-49: the Eye's star lifecycle (units 1-6, 2026-10-13)

This block supersedes the Eye rows below where they differ. Plan: [`d49-build-plan.md`](d49-build-plan.md); judgement
calls: questions B-34..B-38 and B-45.

- **Live stars, T1-T6.** A star chart in the controller slot picks the star; it burns live, in 60 s cycles of
  wireless EU and upkeep, and is held by a four-gauge stability field (P, Θ, Ω, Π) fed from the Field sockets.
  T1 Sol (Raw Star Matter), T2 Sirius A (Excited Stellar Plasma), T3 Arcturus (Raw Hyperdense Stellar Plasma) light
  from a **Stellar Ignition Charge** in a plinth input bus; T4 the neutron star (White Dwarf Matter from a fed
  Hyperdense Stellar Mixture); T5 Sirius B cooling batches (Black Dwarf Matter); T6 the magnetar SGR 1806-20
  (Magnetar Matter, Antipolarization Agent as the constant feed). Planets still run as batch runs.
- **Tiers unlock in order:** the current star's milestones, then the next core right-clicked into the escrow, then
  that tier's transition: Mass Infusion, Shell Ignition, Core Collapse (with supernova suppression), First Cooling,
  Magnetar Merger. Bands are cumulative; the compression gate stays 1:1.
- **Sockets:** 8 Field (plain input hatches, unbuffered trims; ME stocking hatches are refused) and 8 Sensor
  (Stellar Field Sensor Hatch, MTE 28116) sockets on the core band; input buses on the plinth gas sockets; 4 rim
  stellar sockets at T6.
- **Stellar Field Sensor Hatch:** one field channel (P, Θ, Ω, Π, shield, Integrity, Stability, zone, the event
  warnings, upkeep OK, the T5 setpoint error) as digital or analog redstone, pushed every field second. Output is
  cover-friendly: GT's Redstone Transmitter (Internal) and Advanced Wireless Transmitter (Internal) covers carry it
  to a wireless channel, and covers that drive their own side are never overwritten.
- **GUI (MUI2):** the tier line, the star and its state, one bar per active gauge with the zone bands (the T5 bands
  follow the cooling setpoint), Stability / Y / Lock / Integrity, the event warning, upkeep r and the next cycle, the
  dial and HSM feed (with - / + buttons in the gap row), the transition with a progress bar, milestones and escrow,
  the last failure, and a Field side panel (zone legend, sockets, every sensor's status, the trims this second, the
  standard rig). The screwdriver still steps the dial (sneak: the feed).
- **Centre render:** the Eye draws its current object at the centre of the structure while it is formed and its star
  exists: the igniting protostar, the live star (Sol 9, Sirius A 11, Arcturus 15 blocks, each in its own colour),
  the neutron star, the white dwarf cooling to black over its batch, the magnetar (throbbing on its flare warnings),
  and the Core Collapse (Arcturus swelling, then the bounce's shock shell, or a supernova flash). It uses TecTech's
  star shader and textures (the Forge of the Gods' `tectech:star` and `StarLayer0..2`). Config `eyeRenderCentre`
  and a per-Eye switch in the GUI; the render block is placed only into air, never into a block, and removes
  itself when its Eye is gone.
- **NEI:** the star pages (per cycle, up to 4 fluids), the transition pages and the stability-field pages (display
  only).
- **Bench (op, console):** `/btseye t1..t6 <x> <y> <z> [facing] [gen n] [fill]` builds an Eye;
  `/btseye star <x> <y> <z> ...` drives the star (status, target, lit, sim, rig, unlock, milestones, core, escrow,
  sensor, probe, wrench, legacy, field, **render [on|off]**: the centre cell and its look).
- **Cost:** Eye T5 and T6 are in [`singularity-ladder-costs.md`](singularity-ladder-costs.md) §10: each step is
  about 2.4e15 / 3.0e15 EU, almost all of it the 188 compression generators of the next tier.
- **Verified headless** (dev server): the field equals the prototype (78 golden cases, COMPARE PASS in game), every
  transition completes and unlocks its tier, the supernova path, persistence, the wrench, the save migration, and
  the centre render's placement, looks and removal (B-45). **Not yet seen in a client:** the GUI and the render's
  drawing.

The jar loads cleanly in the test instance: "[BTS] Recipe registration complete: 255 registered, 0 skipped", with no
errors and no crash. The boot log also confirms:

- TecTech's 43 Eye of Harmony recipes and its controller recipe are removed;
- our Eye has 48 recipes: 43 planets copied from TecTech, plus 5 stars;
- the Black Hole Compressor's controller recipe is removed;
- all 9 of our assembly lines have a unique research item.

**Machines.**

| Machine | Tier | What it does | Footprint |
|---|---|---|---|
| **Kerr Ergosphere Engine** (replaces the Black Hole Compressor) | T1 UIV; T2 with the Kerr Density Upgrade | Module host. The **Compressor Module** arc runs GT's compressor and neutronium-compressor recipes (the BHC's). The **Extractor Module** arc runs GT's item extractor recipes (`extractorRecipes` only). At **T2**, DENSIFY mode turns Raw Hyperdense Stellar Plasma into Hyperdense Stellar Mixture, the Eye T4 gate. A black hole with mass trades speed for parallel. | T1 76×65×79 (~15k blocks), T2 92×65×89 (~30k) |
| **Eye of Harmony** (Beyond the Stars; replaces TecTech's) | T1-T4, one controller, bands added on | A star chart (or a TecTech dimension block) in the controller slot picks the run. T1 Sol → Raw Star Matter. T2 Sirius A (beam band) → Excited Stellar Plasma. T3 Arcturus (jet crescent) → Raw Hyperdense Stellar Plasma. T4 Sirius B (spiral arms + Galactic Upgrade Core) → White Dwarf Matter, plus an interim Black Dwarf recipe. TecTech's planets also run, by rocket tier. Wireless EU only; outputs go to an ME bus and hatch. | T1 102×52×80 (~20k blocks), T4 102×57×137 (~35k) |

**Getting them in creative** (op only):

- `/btskit kerr` gives the KEE controller, both module controllers, injector rails, a Density Upgrade, Black Hole
  Seeds/Collapser, SpaceTime bolts and hatches. It prints the bill of materials for every piece.
- `/btskerr [t1|t2] [force]` builds a complete, formed Kerr in front of you. You get creative power and every hatch
  (the KEE needs no maintenance hatch), and the bolt buses come stocked. The controller ends up 32 blocks above your feet, facing you.
  With `t2` it also installs the Density Upgrade.
- `/btskit eye` gives the Eye controller, a Galactic Upgrade Core, all five star charts, the hatches, ME output
  bus and hatch, auto-maintenance and field generators. It prints the bill of materials.
- `/btseye [t1|t2|t3|t4] [gen 1-9] [fill] [force]` builds a complete Eye at that tier in front of you. `gen` sets the
  field-generator tier. `fill` tops up every fluid tank and gives you 10¹⁸ wireless EU, so a run starts at once.
- Both build commands refuse a spot that is more than 15 % solid, unless you add `force`. Use a flat, cleared area,
  ideally a void or superflat world, and keep the whole footprint chunk-loaded.
- **Console / command-block forms** (no player needed): `/btskerr [t1|t2] <x> <y> <z> [facing] [force] [survival]`
  and `/btseye [t1..t4] <x> <y> <z> [facing] [gen n] [fill] [force] [survival]`. `~` coordinates are relative to a
  command block, and the facing is the controller front (default north). Each command loads the footprint's chunks,
  builds, and prints FORMED / NOT FORMED with the first wrong cells. It then runs the mirror check, a re-projection
  check and a survival damage-and-repair check. `survival` builds through the projector's survival path. `check`
  builds nothing and re-walks the machine standing at x y z. To test on a dev server: put `eula=true` in
  `run/server/eula.txt`, run `tail -n 0 -f run/server/cmds.txt | ./gradlew.bat runServer` in Git Bash, append
  commands to `cmds.txt`, and finish with `stop`. See questions B-27 for what a run covers.
- **Kerr module test bench** (B-56): `/btskerr keep x y z [t2]` force-loads a whole Kerr (a playerless server unloads
  it within seconds; `/btskerr release` frees the tickets). `/btskerr module x y z <c|x> stock [map#] [longest|first|n]
  [copies]` picks a recipe from one of the module's maps that fits its energy, and fills its input bus and hatches.
  `status` shows the link, progress, outputs and fluids. `tier t [amps]` sets its creative energy hatches, and `clear`
  empties its buses. `/btskerr damage|repair x y z` unforms the KEE by one cage block, then puts the block back.
  `/btskerr legacy x y z [inflight]` turns a built Kerr back into the pre-redesign layout; `inflight` also strips the
  module energy hatches and puts an old recipe in the KEE. `/btskerr module x y z <c|x> energy` fits a module a
  creative energy hatch again (B-57).
- **More console tools** (test bench): `/btseye gates` dumps the compression generator recipes the server actually
  has and checks the gate rule against them, and counts BHC consumers left. `/btsbuild t<1-5> <x> <y> <z> [facing]
  [rod] [force] [crib] | [check]` builds a reactor plant from the console; `crib` checks that a feedwater socket
  refuses a Crafting Input Buffer. Questions B-32.
- **Hologram projector:** on the Kerr, stack 1 = T1 and 2+ = T2. On the Eye, stack 1-4 = T1-T4. Survival auto-build
  works from inventory or AE, like other GTNH multis.

**What to look at in-game.**

1. Run `/btskerr`. The scanner (or the GUI) should show FORMED, the hole's mass and setpoint, and both modules.
2. Open the KEE's GUI: set the setpoint (field, − / +, or the Hole control panel's slider), try Collapse (two
   clicks) and Open, and look at the Modules panel. Open each module's GUI: "Linked" and the hole bonus. Then do the
   same after `/btskerr t2` (DENSITY recipes run on the compressor module).
3. Run `/btseye t1 fill`, let a Sol run finish, and check the Raw Star Matter in the ME system. Then try
   `/btseye t4 fill`.
4. On a `/btseye t3` Eye, swap one Spacetime Compression Field Generator for a tier-1 one. A T3 star chart must
   then refuse with "every Spacetime Compression Field Generator must be tier 3 or higher" (the 1:1 gate).
   `/btseye … gen n` always raises compression to the asked tier's gate, so it cannot test this.
5. Check the NEI pages: "Kerr Densification", GT's Compressor, Neutronium Compressor and Extractor pages (the Kerr
   is listed as a machine on each) and "Eye of Harmony (Beyond the Stars)".
   TecTech's Eye page should be empty.
6. Check the NEI "Multiblock" preview tab on both controllers, and the look of the generated textures.
7. If a build says NOT FORMED with the first wrong block on the mirrored side, the structure orientation is flipped.
   See "Structure export" below.

**Config** (`config/beyondthestars.cfg`):

- `replaceTecTechEyeOfHarmony` (true) removes TecTech's Eye recipes and its controller recipe.
- `removeBlackHoleCompressorRecipe` (true) removes the BHC controller recipe.
- `interimBlackDwarfAtEyeT4` (true) adds the interim Black Dwarf recipe at Eye T4.
- `eyeRenderCentre` (true) draws the Eye's star at its centre (D-49 unit 6; see the D-49 block above).
- `renderKerrHole` (true) draws the Kerr's black hole at the cage centre, scaled by mass (GT's BHC render; each KEE
  also has a switch in its GUI). Test bench: `/btskerr render x y z [open [m]|close|off|on|break]` (questions B-55).

**Follow-up pass (round-3/4 rulings, after 4bd6bf2).**

- The Extractor Module runs GT's item extractor recipes only (`RecipeMaps.extractorRecipes`). The Kerr-only
  "Ergosphere Extraction" map, its lang key and its SL-K06 recipes are gone; its sockets take item buses only.
  Modules are now `KerrModule` entries; "Adding a Kerr module" (in the Kerr contract below) lists the steps.
- The Kerr's lining (shape char `J`, 521 blocks at T1 with both modules, 4,552 at T2) is a new white block,
  **Harmonic Lining** (`slCasing2:2`, the meta reserved for the lining), with texture `sl_lining` and Assembler recipe
  SL-P12. It was the BHC's Background Radiation Absorbent Casing before. The Eye's lining is unchanged.
- The Eye no longer enforces TecTech's per-planet compression minimum: the 1:1 compression gate alone decides, so an
  Eye at tier N runs every planet mapped to tiers 1..N. Compression above a planet's TecTech tier still cuts 3 % of
  its time per tier; below it there is no penalty.
- Kerr T2 with the reinforcement torn down still does not form (ruled: keep).

**Recipe audit applied (after d0181c2).** The cost audit ([`singularity-ladder-costs.md`](singularity-ladder-costs.md))
found two dead ends and several over-priced blocks; §6 there lists what was applied and what was not.

- **Kerr T2 is craftable:** SL-K04 takes 32 Black Hole Seeds in place of the Superstable Black Hole Seed, which no
  recipe in the pack makes.
- **Eye T2-T4 are craftable** with `replaceTecTechEyeOfHarmony=true`. *Superseded by the RC-2 port:* SL-P30..P32
  (research-station upgrades of a Spacetime Compression Field Generator, paying Raw Star Matter / ESPM / RHSP where
  TecTech's beta-1 recipe wanted White Dwarf Matter bolts) are retired; RC-2's Condensate Assembler recipes make the
  generators, and SL-P39 (a WDM-free Resonance Chamber II) closes the one WDM loop left in them (B-29).
- **Cheaper bulk blocks:** SL-P27..P29 batch-make TecTech's Spatial, Temporal and Boundary casings (always registered, B-26);
  the coil, quadrupole, beam band, jet crescent, Galactic Shell, pylon, Eye controller and Galactic Upgrade Core
  recipes are cheaper or make more per craft (§2.2-§2.4 mark each change).
- **Eye structure:** 48 Stabilisation Field Generators (each pylon pad's centre ring), not 294; the rest of each
  pad is plinth.

**Not done yet** (questions file, part A):

- The upgrade milestones and install run: upgrades still install on a right-click.
- In-game build checks. The hologram and the NEI preview paths are verified against the StructureLib and
  BlockRenderer6343 bytecode ("Hologram & NEI" below), but nobody has opened them in game yet. **Structure
  formation is now verified headless** (questions B-27): on a dev server, every Kerr and Eye tier formed in all
  four facings through both the creative and the survival hologram paths, with the orientation as in the mockup.
- The Eye's centre render. *Built in D-49 unit 6*; not yet seen in a client.

---

## 0. Pack facts this spec depends on (verified 2026-10-12 with javap)

| Fact | Where it comes from | Why it matters |
|---|---|---|
| **TecTech's EOH is the pack's ONLY producer of White Dwarf Matter, Black Dwarf Matter, Universium and Raw Star Matter.** `EyeOfHarmonyRecipe.SPECIAL_FLUIDS[10]`: rocket tier 0-2 gives molten WDM (1,152 / 4,608 / 18,432 L), 3-5 gives BDM, 6-9 gives Universium. Every recipe also gives `RawStarMatter` ×(min(8,R)+1)·100,000 L and every valid plasma ×(min(8,R)+1)·8,000,000 L. No other class in the gregtech jar outputs any of the four (the other references are consumers: EIC magneto recipes and nanites, NaniteChain, BEC metamaterial, Nano Forge, Exo-Foundry, CoAL misc, Godforge upgrade costs, research-station UXV parts). | `tectech.recipe.EyeOfHarmonyRecipe` `<clinit>` and constructor | Removing TecTech's recipes (D-28) cuts all four supplies. See Q-3. |
| `Materials.RawStarMatter`'s local name is **"Condensed Raw Stellar Plasma Mixture"**. | `MaterialsInit`: `setName("RawStarMatter").setDefaultLocalName("Condensed Raw Stellar Plasma Mixture")` | Our "Raw Stellar Plasma Mixture" **is** this material (Q-1). |
| TecTech EOH per-recipe numbers: H and He = (R+1)·10⁹ L each; base time = 18,000 s · 1.4^R; base success = 1 − 0.05R; EU efficiency = 0.6 + R/10. The Deep Dark (`"DD"`) is a special R9 recipe that outputs every ore and every plasma. Recipes are built in `TecTech.onLoadCompleted` (`FMLLoadCompleteEvent`) into `TecTech.eyeOfHarmonyRecipeStorage` (`private HashMap<String,EyeOfHarmonyRecipe> recipeHashMap`) and mirrored into `TecTechRecipeMaps.eyeOfHarmonyRecipes` for NEI. | `EyeOfHarmonyRecipeStorage$2`, `timeCalculator`, `specialDeepDarkRecipe` | The Eye unit converts these recipes instead of rewriting ore tables (section 3.3), and the removal runs at load-complete (section 4). |
| TecTech EOH could burn `RawStarMatter` in place of H/He, and it **returned EU**: `euOutput = euStartCost × (0.6 + R/10)`, so R5+ was a net EU source. | `MTEEyeOfHarmony` tooltip, `EyeOfHarmonyRecipe` constructor | Our Eye returns no EU (Q-4). |
| Black Hole Compressor: `MTEBlackHoleCompressor`, maps `RecipeMaps.compressorRecipes` + `neutroniumCompressorRecipes`; parallel = 8 × `GTUtility.getTierExtended(maxInputEu)`, ×2/×4 by hole stability, unbounded for non-black-hole recipes once stabilised; catalyst cost is `Materials.SpaceTime.getMolten(1)`. Casings: `Casings.ExtremeDensitySpaceBendingCasing`, `Casings.BackgroundRadiationAbsorbentCasing`, `Casings.HawkingRadiationRealignmentFocus`, frame `Materials.NaquadahAlloy`. Seeds: `ItemList.Black_Hole_Opener` ("Black Hole Seed"), `Black_Hole_Closer` ("Black Hole Collapser"), `Black_Hole_Stabilizer` ("Superstable Black Hole Seed"). | `MTEBlackHoleCompressor` | Kerr parity targets; SpaceTime is a UIV-available material because the BHC already burns it. |
| The BHC controller recipe is in `com.dreammaster.gthandler.recipes.AssemblingLineRecipes` (modid `dreamcraft`). | GTNewHorizonsCoreMod jar | Section 4. |
| Assembly-line recipes live in `RecipeMaps.assemblylineVisualRecipes` (NEI) **and** `GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes` (what data sticks resolve against, through the lazily built `AssemblyLineUtils.AL_RECIPE_LOOKUP`). Research-station recipes also sit in `TecTechRecipeMaps.researchableALRecipeList` and `researchStationFakeRecipes`. `RecipeMapBackend.removeRecipes(Collection)` exists. | `GTRecipe$RecipeAssemblyLine`, `AssemblyLineUtils`, `RecipeMapBackend` | Section 4. |
| The pack has no `MaterialsUEVplus`. "Tachyon Rich Temporal Fluid" is `Materials.Time` (name `temporalFluid`); "Spatially Enlarged Fluid" is `Materials.Space`. Superconductor wire materials go up to `Materials.SuperconductorUMV`, with no UXV. | `MaterialsInit` | Recipe translation. |
| Wireless EU: `gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap(UUID, long/BigInteger)` (returns false when a debit cannot be paid), `strongCheckOrAddUser(UUID)`, `getUserEU(UUID)`. | `WirelessNetworkManager` | Eye power. |
| `tectech.util.ItemStackLong` / `FluidStackLong` carry `long` sizes; TecTech EOH output is ME-only for that reason. | `tectech.util` | Eye outputs. |
| The ResearchStation scan of `addEOHRecipes` found WDM, BDM and MHDCSM only as alternative bolts in the **TecTech EOH controller** recipe, and WDM, BDM and Eternity only in the recipe after the Tier-8 Spacetime Compression generator. The three Tier-8 ("Gallifreyan") field-generator recipes carry none of those markers. | `tectech.loader.recipe.ResearchStationAssemblyLine` | Our Eye controller can keep the card's Gallifreyan field generators (the Eye unit re-verifies, Q-7). |

---

## 1. P1 item, fluid and casing list

**EXISTING** = already in the pack (exact reference given). **NEW** = registered by this addon. The addon registers
no GT `Materials` and no Werkstoff: new matter is a `GTFluidFactory` fluid, new parts are plain `Item`s, and new
casings are BTS meta-blocks.

### 1.1 Fluids

| Name (display) | Status | Reference / internal name | Notes |
|---|---|---|---|
| Raw Stellar Plasma Mixture (RSPM) | **EXISTING** | `Materials.RawStarMatter.getFluid(n)` (shows as "Condensed Raw Stellar Plasma Mixture") | Eye T1 (Sol) output. Reused, not re-registered (Q-1). After D-28 our Eye is its only source, so the Godforge fuel, EIC, mixer and nanite recipes that drink it keep a supplier. |
| Excited Stellar Plasma Mixture (ESPM) | **NEW** | `btsexcitedstellarplasma`, liquid, 50,000 K, RGB 255,194,58 | Eye T2 (Sirius A) output. While the Homogeny is on hold, Eye T2 is its only source (Q-2). |
| Raw Hyperdense Stellar Plasma (RHSP) | **NEW** | `btsrawhyperdenseplasma`, liquid, 80,000 K, RGB 210,72,42 | Eye T3 (Arcturus) output. D-42's text says "raw hyperdense star plasma" and the cards say "Raw Hyperdense Stellar Plasma"; the build uses the card name (Q-13). |
| Hyperdense Stellar Mixture (HSM) | **NEW** | `btshyperdensestellarmixture`, liquid, 120,000 K, RGB 154,26,58 | Kerr T2 output (density map). Gates Eye T4: it is in Galactic Shell Plating, the Galactic Upgrade Core and the T4 white-dwarf recipe. |
| White Dwarf Matter | **EXISTING** | `Materials.WhiteDwarfMatter.getMolten(n)` | Eye T4 (Sirius B) output. |
| Black Dwarf Matter (interim only) | **EXISTING** | `Materials.BlackDwarfMatter.getMolten(n)` | Not a P3 product by design (D-42 puts it at T5). It is made by an interim T4 recipe behind a config flag until P4 (Q-3). |
| Hydrogen / Helium | EXISTING | `Materials.Hydrogen.getGas`, `Materials.Helium.getGas` | Eye feed, drawn into long buffers like TecTech's. |
| Molten SpaceTime / Transcendent Metal / Infinity / Cosmic Neutronium | EXISTING | `Materials.SpaceTime/TranscendentMetal/Infinity/CosmicNeutronium.getMolten(n)` | |
| Super Coolant | EXISTING | `Materials.SuperCoolant.getFluid(n)` | |
| Excited DTEC | EXISTING | `Materials.ExcitedDTEC.getFluid(n)` | |
| Lubricant | EXISTING | `Materials.Lubricant.getFluid(n)` | |

Registration: a new `fluids/SLFluids.java` with the same `liquid(...)` / `build(...)` helper as
`BTSFluidsProcess.java:96-105`, called from `BTSFluids.register()` on one appended line after
`BTSFluidsPropellant.register()`.

### 1.2 Blocks (structure casings)

Three new meta-blocks in `blocks/`. They are tile-less and solid (glass is translucent), with hardness and harvest
like GT casings, an `ItemBlock` with metadata, and one icon per meta at `textures/blocks/sl_<name>.png`. Textures
are generated from the voxel atlas painters (section 5.5), so the in-game face matches the mockup tile.

`BlockSLCasing` (`slCasing`), 16 metas:

| Meta | Name | Voxel kind | Used in | Texture idea |
|---|---|---|---|---|
| 0 | Containment Deck Plating | `deck` | Kerr cage decks; Eye ground plinth | grey plate with a stud grid (atlas `deck`) |
| 1 | Superradiant Mirror Casing | `mirror` | Kerr inner and outer mirror walls (2,852) | pale cyan with diagonal glints |
| 2 | Mirror Backing Casing | `mirrorback` | behind every mirror (3,551) | slate casing, line motif |
| 3 | Bolt Injector Rail (Mk I) | `injector` | the 8 angled injector rails (232) | blue-slate with a cyan bore |
| 4 | Bolt Injector Rail (Mk II) | `injector` | same cells | same bore, amber ring |
| 5 | Bolt Injector Rail (Mk III) | `injector` | same cells | same bore, magenta ring |
| 6 | Ergosphere Field Coil | `coil` | Kerr dome coils (842) | copper coil stripes |
| 7 | Beam Focusing Quadrupole | `quad` | injector and beamline rings (208) | purple casing with a pink cross |
| 8 | Beam Dump | `dump` | Kerr west dump arc | yellow and black hazard |
| 9 | Eye Shell Casing | `eyeshell` | Eye core, pylons; Kerr governor and dump arcs | dark shell, line motif |
| 10 | Beam Band Casing | `ring2` | Eye T2 beam band | steel-blue grid |
| 11 | Jet Crescent Casing | `ring3` | Eye T3 jet crescent | teal grid |
| 12 | Galactic Shell Plating | `galactic` | Eye T4 spiral-arm blades | navy hex with a blue rule |
| 13 | Bastion Plate | `bastion` | Kerr T2 hoops, band and struts (8,316) | black grid with gold rules |
| 14 | Casimir Plate | `casimir` | Kerr T2 trim (677) | gold ribbed |
| 15 | Ergosphere Extraction Casing | (`ring3` in `buildKerr`) | Kerr extractor arc shell | teal grid, pump glyph |

`BlockSLCasing2` (`slCasing2`):

| Meta | Name | Voxel kind | Notes |
|---|---|---|---|
| 0 | Kerr Compressor Module | stand-in `control` at the south arc | The module core block: one per compressor arc. Crafted from the card "Kerr Compressor Module". |
| 1 | Kerr Extractor Module | an extractor-arc shell cell | One per extractor arc. Crafted from the card "Kerr Extractor Module". |
| 2 | Harmonic Lining | `lining` | The Kerr's white inner face (every Kerr arc and band). Live again since the round-4 ruling (FC-1); the Eye keeps TecTech's Infinite Spacetime Energy Boundary Casing. Not emissive: the texture is bright, and thousands of light sources would cost lighting updates on every build. |

`BlockSLGlass` (`slGlass`, translucent, render pass 1):

| Meta | Name | Voxel kind |
|---|---|---|
| 0 | Spatial Glass Channel | `sglass` |

EXISTING blocks the structures use:

| Voxel kind | Block | Reference |
|---|---|---|
| `casing` ("Space-Bending Casing") | Extreme Density Space-Bending Casing | `Casings.ExtremeDensitySpaceBendingCasing` (BHC casing) |
| `bhc` (compressor arc shell) | Background Radiation Absorbent Casing | `Casings.BackgroundRadiationAbsorbentCasing` (the BHC's own casing, so the compressor arc reads as "the old BHC") |
| `frame` | Naquadah Alloy Frame Box | `StructureUtility.ofFrame(Materials.NaquadahAlloy)` (as in the BHC) |
| `pipe` (beamline) | Particle Beam Guidance Pipe Casing | `Casings.ParticleBeamGuidancePipeCasing` |

Hatch textures: every new socket uses `Casings.UMVMachineCasing`'s texture index, so P1 does **not** register a GT
casing-texture page. Prefer deleting machinery over adding it.

### 1.3 Items (`items/SLItems.java`, via `BTSItems.define(name)`)

| Item | Internal name | Kind | Used by | Texture idea |
|---|---|---|---|---|
| Superradiant Mirror Segment | `superradiantMirrorSegment` | component | Kerr controller (×8) | layered mirror plate, cyan glint |
| Stabiliser Pylon | `stabiliserPylon` | component | Eye controller (×6) | dark flared strut |
| Kerr Density Upgrade | `kerrDensityUpgrade` | upgrade core | installed into the Kerr controller: Kerr T2 | `cond` orb, crimson (`0x9a1a3a`) |
| Galactic Upgrade Core | `galacticUpgradeCore` | upgrade core | installed into the Eye controller: unlocks T4 | `cond` orb, sky blue (`0x3fa6d9`) |
| Star Chart: Sol | `starChartSol` | recipe target (not consumed) | Eye T1 star recipe | data-orb disc, yellow star |
| Star Chart: Sirius A | `starChartSiriusA` | recipe target | Eye T2 | blue-white star |
| Star Chart: Arcturus | `starChartArcturus` | recipe target | Eye T3 | orange giant |
| Star Chart: Sirius B | `starChartSiriusB` | recipe target | Eye T4 | small white dwarf |
| Star Chart: Sirius B, Cooled (interim) | `starChartSiriusBCooled` | recipe target | Eye T4 interim BDM (Q-3); delete with P4 | white dwarf, frost rim |

Star picks follow mine.md (Sirius A for "a larger star", Arcturus for "a red giant"). Sirius B is our choice for the
white dwarf: it is Sirius A's companion and the best-known white dwarf (Q-13). Planets and the Deep Dark keep
TecTech's targets, the `gtneioreplugin` dimension display blocks (`EyeOfHarmonyRecipe.getRecipeTriggerItem()`).

EXISTING items the recipes use: `ItemList.{Field_Generator,Emitter,Sensor,Robot_Arm,Electric_Piston,Electric_Pump,
Conveyor_Module}_{UIV,UMV}`, `ItemList.Black_Hole_Opener/Black_Hole_Stabilizer`, circuits through
`req(OrePrefixes.circuit, Materials.UIV|UMV, n)`, `CustomItemList.EOH_Reinforced_Spatial_Casing`,
`EOH_Reinforced_Temporal_Casing`, `SpacetimeCompressionFieldGeneratorTier8`, `TimeAccelerationFieldGeneratorTier8`,
`StabilisationFieldGeneratorTier8`, `ItemList.Tool_DataOrb`, and GT parts through `BTSRecipeGuard.req(prefix,
material, n)`.

### 1.4 Cut from the cards for P1-P3 (not built)

Beam Port, Particle Beam, BEC Metamaterial Film, White Dwarf Matter Lens, Ergomatter (it is D-42's T7 output), GRB
Receptor Port, Conduit parts, and the Accretion Upgrade Core (already obsolete). In-structure `beamhatch` and
`ergohatch` cells are remapped (section 5.3). Reasons: Q-7 and Q-8.

---

## 2. Recipes

Conventions: every recipe is wrapped as `BTSRecipeGuard.recipe("<ID>", () -> GTValues.RA.stdBuilder()...addTo(map))`.
Item and fluid lookups always go through `req(...)`, `fluid(...)` or `nn(...)`. Times are in **ticks**. EU is a
`TierEU` constant. "AL" means an assembly line: `GTRecipeConstants.AssemblyLine`, with
`.metadata(GTRecipeConstants.RESEARCH_ITEM, X).metadata(GTRecipeConstants.SCANNING, new Scanning(...))`, at most 16
item inputs and 4 fluids. "ASM" is `RecipeMaps.assemblerRecipes`. Structure blocks are made on the **Assembler**, in
batches: a structure that needs thousands of a block cannot reasonably take an assembly-line craft per block (the
card "Superradiant Mirror Segment" stays an AL **component**, Q-7).

### 2.1 Progression-loop audit (D-42 matter map: T4 makes WDM, T5 BDM, T7 Ergomatter, T10 Universium)

The order a player meets these things: **Kerr T1 (UIV, the BHC's slot)** → Eye T1 (UMV) → Kerr T2 → Eye T2 → Eye T3 →
HSM → Eye T4. Every input must exist before the machine that needs it.

| Card input | Card | Problem | Substitution (built) |
|---|---|---|---|
| Dense Black Dwarf Matter Plate / BDM ingot / BDM plate | Mirror Segment, Eye Shell Casing, Stabiliser Pylon, Bastion Plate, Beam Dump, Kerr Density Upgrade | BDM is T5 (P4); TecTech's supply is removed | `plateDense Neutronium` (Bastion, Eye Shell, Pylon), `ingot CosmicNeutronium` (Beam Dump), `plateDense Infinity` (Density Upgrade); Mirror Segment drops it |
| White Dwarf Matter (plates, molten, lens, condensate) | Mirror Segment, Harmonic Lining, Jet Crescent, Galactic Shell Plating, Galactic Upgrade Core | WDM is T4's **output**; T4's own structure cannot need it | `plateDense Infinity` (Jet Crescent, Galactic Shell); HSM is the new T4 gate (Galactic Shell, Galactic Core); the lens is cut |
| Ergomatter | HSM process card | T7 (P4) | `Molten SpaceTime 288 L` |
| Raw Star Matter 4,000,000 L | Eye controller | after removal only our Eye makes it: the Eye would need itself | `Excited DTEC 256,000 L` (already on the card) + Molten Infinity; the 4M L is dropped |
| Jet Collimator Ring | Jet Crescent Casing | quasar part (P4) | 2 × Ergosphere Field Coil |
| GRB Receptor Port, Conduit Throat | Galactic Upgrade Core | P4; D-42 moved stabilisation to T7 | dropped |
| MagMatter Plate | Bastion Plate | UXV per D-48 | `plate TranscendentMetal ×2` |
| UMV Superconductor Wire, UMV components | Ergosphere Field Coil, injectors | Kerr T1 is **UIV** (D-44) and needs these blocks | `wireGt01 SuperconductorUIV`, UIV components; Mk III rails stay UMV |
| Tachyon Rich Temporal Fluid (`Materials.Time`) | Spatial Glass Channel | the Kerr (UIV) uses the glass; Time is believed to be UMV | `Molten SpaceTime` |
| Metamaterial, condensates, "Observation Array (BEC)" | Harmonic Lining, Casimir Plate, Galactic Core | the BEC condensate API is unverified on the 5.09.52.594 pin | moved to ASM/AL with molten metals. *RC-2:* the API is verified now (SL-P39 is a Condensate Assembler recipe); these blocks stay as built |
| Beam Port | Beam Band, Jet Crescent | no beam I/O in P2/P3 | `Emitter_UMV` |
| SpaceTime at UIV | Kerr cards | OK: the BHC (UIV) already burns molten SpaceTime | kept |
| Black Hole Seed / Superstable Seed | Kerr | OK: UIV BHC items, untouched | kept |
| Gallifreyan field generators | Eye controller | OK: the javap scan finds no WDM/BDM/Universium/Eternity/MHDCSM markers in their recipes | kept; re-verify (Q-7). *RC-2:* the controller takes the Tier0 (Crude) generators; the RC-2 gate audit (costs doc §9) finds Tier0 clean and Tier1-3 needing WDM only through Resonance Chamber II (fixed by SL-P39). Tier4+ (P4/P5) are not clean: see costs doc §9 |
| RSPM 4,000,000 L | Kerr Density Upgrade | OK: Kerr T2 comes after Eye T1 | kept |

Wider fallout of removing TecTech's recipes (everything in the pack that eats WDM, BDM or Universium before our
Eye reaches it): Q-3.

### 2.2 Foundation recipes: `BTSRecipesSingularity.java` (parts)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-P01 | Containment Deck Plating ×4 | ASM | plate Neutronium ×6, frameGt NaquadahAlloy ×1 | CosmicNeutronium molten 576 | RECIPE_UIV | 400 |
| SL-P02 | Mirror Backing Casing ×2 | ASM | plate Infinity ×6, frameGt SpaceTime ×1 | Infinity molten 1,152 | RECIPE_UIV | 600 |
| SL-P03 | Superradiant Mirror Casing ×2 | ASM | plate Neutronium ×4, foil Infinity ×8, Mirror Backing Casing ×1 | Infinity molten 576, SpaceTime molten 144 | RECIPE_UIV | 600 |
| SL-P04 | Superradiant Mirror Segment (item) | AL (research: Superradiant Mirror Casing, Q-36) | plate Neutronium ×32, foil Infinity ×64, Emitter_UIV ×4, circuit UIV ×4, Superradiant Mirror Casing ×4 | Infinity molten 18,432, SpaceTime molten 2,304 | RECIPE_UIV | 4,800 |
| SL-P05 | Bolt Injector Rail Mk I | ASM | Robot_Arm_UIV ×1, Electric_Piston_UIV ×1, frameGt SpaceTime ×1, gearGt SpaceTime ×2 | Lubricant 1,000 | RECIPE_UIV | 300 |
| SL-P06 | Bolt Injector Rail Mk II | ASM | Rail Mk I ×1, Conveyor_Module_UIV ×2, Robot_Arm_UIV ×1 | Lubricant 2,000 | RECIPE_UIV ×4 | 400 |
| SL-P07 | Bolt Injector Rail Mk III | ASM | Rail Mk II ×1, Robot_Arm_UMV ×1, Electric_Piston_UMV ×1, Conveyor_Module_UMV ×1 | SpaceTime molten 576, Lubricant 4,000 | RECIPE_UMV | 600 |
| SL-P08 | Ergosphere Field Coil | ASM | wireGt01 SuperconductorUIV ×16 (was ×64, cost audit #6), frameGt TranscendentMetal ×1, plate CosmicNeutronium ×8 | Super Coolant 8,000 (was 32,000) | RECIPE_UIV | 1,200 |
| SL-P09 | Beam Focusing Quadrupole ×4 (was ×1, cost audit #7) | ASM | Ergosphere Field Coil ×2, Sensor_UIV ×1, ParticleBeamGuidancePipeCasing ×2, plate Neutronium ×8 | Super Coolant 16,000 | RECIPE_UIV | 1,200 |
| SL-P10 | Beam Dump | ASM | plateDense Neutronium ×8, ingot CosmicNeutronium ×16, frameGt NaquadahAlloy ×1 | Super Coolant 16,000 | RECIPE_UIV | 600 |
| SL-P11 | Eye Shell Casing ×2 | ASM | plateDense Neutronium ×4, frameGt SpaceTime ×1, plate Neutronium ×4 | SpaceTime molten 576 | RECIPE_UIV | 800 |
| SL-P12 | Harmonic Lining ×4 (Kerr only, round-4 ruling) | ASM | plate Neutronium ×6, foil Infinity ×8, plate TranscendentMetal ×1 | Infinity molten 576 | RECIPE_UIV | 600 |
| SL-P13 | Spatial Glass Channel ×4 | ASM | IC2 reinforced glass ×4 (`GTModHandler.getIC2Item("reinforcedGlass", 4)` through `nn`), foil SpaceTime ×8, Emitter_UIV ×1 | SpaceTime molten 144 | RECIPE_UIV | 600 |
| SL-P14 | Ergosphere Extraction Casing ×2 | ASM | plateDense Neutronium ×2, frameGt SpaceTime ×1, Electric_Piston_UIV ×1 (was a pump, cost audit #14) | SpaceTime molten 288 | RECIPE_UIV | 800 |
| SL-P15 | Beam Band Casing ×16 (was ×4, cost audit #8) | ASM | EOH_Reinforced_Spatial_Casing ×1, Beam Focusing Quadrupole ×1, Emitter_UMV ×2, plateDense SpaceTime ×4 | SpaceTime molten 9,216 | RECIPE_UMV | 1,800 |
| SL-P16 | Jet Crescent Casing ×16 (was ×4, cost audit #9) | ASM | EOH_Reinforced_Temporal_Casing ×1, Ergosphere Field Coil ×2, plateDense Infinity ×8, Emitter_UMV ×2, ingot SpaceTime ×32 | RawStarMatter 64,000 (4,000 per block, as before) | RECIPE_UMV | 2,400 |
| SL-P17 | Galactic Shell Plating ×16 (was ×2, cost audit #10) | ASM | plateDense Infinity ×8, frameGt TranscendentMetal ×2, plate CosmicNeutronium ×8, ingot TranscendentMetal ×16 | **HSM 250** (was 1,000 per 2) | RECIPE_UMV | 800 |
| SL-P18 | Bastion Plate ×4 | ASM | plateDense Neutronium ×6, plate TranscendentMetal ×2, frameGt Infinity ×1 | Infinity molten 2,304 | RECIPE_UMV | 1,200 |
| SL-P19 | Casimir Plate ×4 | ASM | plate Neutronium ×8, foil SpaceTime ×16 | SpaceTime molten 576 | RECIPE_UMV | 1,200 |
| SL-P21 | Stabiliser Pylon (item) | AL (research: Star Chart: Sol, Q-36) | Eye Shell Casing ×32, Harmonic Lining ×16, Field_Generator_UMV ×8, frameGt Neutronium ×16, circuit UMV ×8, plateDense Neutronium ×16, Containment Deck Plating ×16 | SpaceTime molten 36,864 (was 147,456, cost audit #12), Super Coolant 500,000 | RECIPE_UMV ×4 | 18,000 |
| SL-P22 | Star Chart: Sol | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate Infinity ×4 | SpaceTime molten 144 | RECIPE_UMV | 200 |
| SL-P23 | Star Chart: Sirius A | ASM | Tool_DataOrb ×1, circuit UMV ×1, plateDense SpaceTime ×1 | RSPM 10,000 | RECIPE_UMV | 200 |
| SL-P24 | Star Chart: Arcturus | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate TranscendentMetal ×4 | ESPM 1,000 | RECIPE_UMV | 200 |
| SL-P25 | Star Chart: Sirius B | ASM | Tool_DataOrb ×1, circuit UMV ×1, Galactic Shell Plating ×1 | HSM 1,000 | RECIPE_UMV | 200 |
| SL-P26 | Star Chart: Sirius B, Cooled (interim) | ASM | Star Chart: Sirius B ×1, circuit UMV ×1 | Super Coolant 64,000 | RECIPE_UMV | 200 |
| SL-P27 | TecTech Reinforced Spatial Structure Casing ×4 (gated) | ASM | Godforge Singularity Shielding Casing ×1, plateDense Neutronium ×4, plateDense CosmicNeutronium ×4, plateDense Infinity ×1, programmed circuit 1 | SpaceTime molten 576 | RECIPE_UMV | 1,200 |
| SL-P28 | TecTech Reinforced Temporal Structure Casing ×4 (gated) | ASM | as SL-P27, on programmed circuit 2 | SpaceTime molten 576 | RECIPE_UMV | 1,200 |
| SL-P29 | TecTech Infinite SpaceTime Energy Boundary Casing ×8 (gated) | ASM | wireGt16 SuperconductorUMV ×1, plateDense Neutronium ×8, plateDense CosmicNeutronium ×8, Emitter_UIV ×1 | SpaceTime molten 1,152 | RECIPE_UMV | 1,200 |
| SL-P30..P32 | *retired in the RC-2 port* (were research-station "upgrade the compression generator" recipes, B-28) | | | | | |
| SL-P33..P38 | *reserved* by the D-49 build plan (P33/P34: conditional Eye T5/T6 gate recipes, whose §14 post-port audit is answered by costs doc §9.4; P35..P38: Star Chart: Sirius B, its reclaim, Star Chart: Magnetar, Stellar Ignition Charge) | | | | | |
| SL-P39 | MetaMaterial Resonance Chamber II (gated; RC-2 gate audit, B-29) | Condensate Assembler (`TecTechRecipeMaps.condensateAssemblingRecipes`), nanite tiers SixPhasedCopper / Silver / Gold / Silver | Emitter_UEV ×2, rod Hypogen ×2, **foil SpaceTime ×2** (GT's recipe: foil WhiteDwarfMatter ×1), plate ProtoHalkonite ×1 | entangled Chromatic Glass ×12, Infinity ×6, Boundless Cosmic Solder ×2 (condensate units, as GT's) | RECIPE_UIV | 6,000 |

SL-P20 is reserved and unused. Keep the ID gap; do not renumber. Each star chart's fluid is the previous tier's
product, so a chart cannot be crafted before its tier is reachable. SL-P26 registers only when
`Config.interimBlackDwarfAtEyeT4` is true. SL-P27..P29 come from the cost audit
([`singularity-ladder-costs.md`](singularity-ladder-costs.md) §6, #3-#4) and always register, because our Eye needs
them either way (B-26). TecTech's own recipes for those blocks (Condensate Assembler in RC-2) stay. SL-P39 ("gated")
registers only when `Config.replaceTecTechEyeOfHarmony` is true: with TecTech's Eye kept, WDM has its stock source.
It is the RC-2 replacement for SL-P30..P32 (B-22, B-28, B-29): GT's Resonance Chamber II is the only place a White
Dwarf Matter product enters RC-2's tier 1-3 Spacetime Compression Field Generators (costs doc §9).
`plateDense SpaceTime` and the other dense-plate pairings may not be generated by GT. `req()` names a missing one at
boot (`mGeneratedItems` check). The Foundation unit swaps any that fails to `plate` ×9 in the same recipe and
notes it in the commit.

### 2.3 Kerr recipes: `BTSRecipesKerr.java` (Kerr unit)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-K01 | Kerr Ergosphere Engine (controller, MTE base+14). **Re-priced in the RC-2 port at BHC-controller parity** (B-30, costs doc §9). The card's recipe was plateDense SpaceTime ×32, frameGt SpaceTime ×32, FG/Emitter/Sensor UIV ×16, circuit UIV ×32, Mirror Segment ×8, Rail Mk I ×16, Seed ×8, TM screw ×64, Field Coil ×16, Deck ×32; SpaceTime 294,912, TM 147,456, Super Coolant 1,000,000; 18,000 ticks | AL (research: Black_Hole_Opener) | Machine_Multi_HIPCompressor ×1, Machine_Multi_NeutroniumCompressor ×1, AdvancedImplosionCompressor ×16, CompressorUIV ×8, ElectricImplosionCompressor ×4, Field_Generator_UEV ×4, circuit UIV ×4, Black_Hole_Opener ×1, Superradiant Mirror Segment ×1, Ergosphere Field Coil ×4, Extreme Density Space-Bending Casing ×1, plateSuperdense Neutronium ×16 / Naquadria ×16 / CosmicNeutronium ×8, screw TranscendentMetal ×16 | SpaceTime molten 2,304, SuperconductorUIVBase molten 43,200, Infinity molten 14,400, Super Coolant 64,000 | RECIPE_UIV | 3,600 |
| SL-K02 | Kerr Compressor Module (block) | AL (research: Kerr controller) | Electric_Piston_UIV ×32, Field_Generator_UIV ×8, circuit UIV ×16, plateDense Neutronium ×32, Black_Hole_Opener ×4, gearGt SpaceTime ×16 | SpaceTime molten 73,728, Super Coolant 500,000 | RECIPE_UIV | 12,000 |
| SL-K03 | Kerr Extractor Module (block) | AL (research: Extraction Casing, Q-36) | Electric_Piston_UIV ×32 (was pumps, cost audit #14), Field_Generator_UIV ×8, circuit UIV ×16, plateDense Neutronium ×32, Beam Focusing Quadrupole ×4, pipeHuge Neutronium ×16 | SpaceTime molten 73,728, Super Coolant 500,000 | RECIPE_UIV | 12,000 |
| SL-K04 | Kerr Density Upgrade (item) | AL (research: Bastion Plate, Q-36) | Bastion Plate ×64, Bastion Plate ×64, plateDense Infinity ×64, Field_Generator_UMV ×32, circuit UMV ×32, Ergosphere Field Coil ×32, Black_Hole_Opener ×32 (was Black_Hole_Stabilizer ×1, which nothing crafts; cost audit #1, B-21), Bolt Injector Rail Mk III ×16 | RawStarMatter 4,000,000, SpaceTime molten 589,824, ExcitedDTEC 128,000 | RECIPE_UMV ×4 | 36,000 |
| SL-K05 | HSM 1,000 L | `beyondthestars.recipe.kerrdensity` (new, section 3.1) | bolt Neutronium ×64 | RHSP 10,000, SpaceTime molten 288 | RECIPE_UMV | 400 |

The Kerr's other maps are GT's own (`compressorRecipes`, `neutroniumCompressorRecipes`, `extractorRecipes`), so it
adds no recipes there. SL-K06 (a Kerr-only "Ergosphere Extraction" map) is retired; the ID stays reserved.

### 2.4 Eye recipes: `BTSRecipesEye.java` + `eye/EyeRecipeRegistry.java` (Eye unit)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-E01 | Eye of Harmony (controller, MTE base+15) | AL (research: Stabiliser Pylon, Q-36) | EOH_Reinforced_Spatial_Casing ×64, EOH_Reinforced_Temporal_Casing ×64, SpacetimeCompressionFieldGeneratorTier0 ×1, TimeAccelerationFieldGeneratorTier0 ×1, StabilisationFieldGeneratorTier0 ×1 (Tier0 since the review fix pass, Q-32), circuit UMV ×64, Field_Generator_UMV ×32, plateDense SpaceTime ×64, Eye Shell Casing ×64, Harmonic Lining ×64, Stabiliser Pylon ×6, Containment Deck Plating ×64 | SpaceTime molten 294,912 (was 1,179,648, cost audit #12), ExcitedDTEC 256,000, Infinity molten 589,824 | RECIPE_UMV ×4 | 36,000 |
| SL-E02 | Galactic Upgrade Core | AL (research: Eye controller) | Galactic Shell Plating ×64, Field_Generator_UMV ×32, circuit UMV ×64, Casimir Plate ×32, plateDense SpaceTime ×32, Emitter_UMV ×16 | **HSM 32,000** (was 256,000, cost audit #11), SpaceTime molten 147,456, TranscendentMetal molten 147,456 | RECIPE_UMV ×4 | 48,000 |

The Eye's process recipes (SL-E1x) are **not** GT recipe-map recipes; they are section 3.3's registry, shown in NEI
through a display-only map `beyondthestars.recipe.eye` built with `.fake()` like `BTSDisplayRecipes`.

---

## 3. Machines

### 3.1 Kerr Ergosphere Engine (P2): `machines/kerr/MTEKerrEngine.java`, MTE **base+14 (28114)**

`extends MTEExtendedPowerMultiBlockBase<MTEKerrEngine> implements ISurvivalConstructable` (the
`MTEProductionHallBase` style: a `ProcessingLogic` with a parallel supplier, void protection, batch mode, and a
machine-mode switch). Tier: UIV. It replaces the BHC (D-44).

**Pieces** (section 5 has the shapes):

| Piece | Required | Contents |
|---|---|---|
| `kerr_t1` | yes, unless the Density Upgrade is installed | the cage, mirror cavity, 8 injector rails, governor arc with controller, dump arc, beamline |
| `kerr_t2` | yes, when the Density Upgrade is installed (it **replaces** `kerr_t1`) | `kerr_t1` plus the density reinforcement: 2 Bastion hoops (y ±14, r 41), a mid band (r 43), 12 paired struts, the dome-foot hoops, and the beamline extended to x 47 |
| `kerr_compressor` | optional checked piece | south service arc (245°-295°), Background Radiation Absorbent shell, Kerr Compressor Module core block, 6 I/O sockets |
| `kerr_extractor` | optional checked piece | east arc (328°-352°), Ergosphere Extraction shell, Kerr Extractor Module core block, 3 I/O sockets |

Module pieces follow the `MTEReactorCoreMk1.java:2946-3077` pattern: their own `pieceErrors` list, a fault string
when blocks stand, and nothing they report can un-form the core. Their I/O sockets **do** join the standard GT
lists (`mInputBusses`, `mOutputBusses`, `mInputHatches`, `mOutputHatches`), because the Kerr has no other recipe I/O.
They use **mark-and-truncate** on a failed walk (the Spiral pattern at `MTEReactorCoreMk1` ~:3030) so a half-built arc
leaves nothing registered. **Bolt buses are private**: a `KerrHatchElement.BoltBus` enum (the
`ModuleHatchElement` shape), so recipes never eat bolts and the governor never eats recipe inputs (trap 3).

**Upgrade install.** Sneak-right-click the controller holding a Kerr Density Upgrade (server side,
`onRightclick`). That consumes one, sets `densityInstalled = true` and re-checks the structure. It cannot be undone.
`densityInstalled` is written by **one** serializer, called from both `saveNBTData` and `setItemNBT` (a wrench keeps
it), and read in `loadNBTData` (trap 2, the D-23 pattern). The hologram never reads it: `construct(stack,
hintsOnly)` picks T1 or T2 from the trigger stack size (1 = T1, ≥2 = T2), and both modules are always projected
(trap 1).

**The hole** (bh-model.js constants, kept as tuning knobs; marked PROPOSED, Q-8):

- State persisted in NBT: `holeOpen`, `mass` (double, SM) and `setpoint`.
- **Open:** a Black Hole Seed in any bolt bus is consumed, and the hole opens at M = 8 SM. **Close:** a Black Hole
  Collapser. If M falls below `M_FLOOR` = 2 SM, the hole evaporates (closes). There is no explosion in P2.
- **Hawking bleed:** 3.2 / M² SM per tick. Each tick the **governor** fires bolts from the bolt buses to hold M at the
  setpoint. Bolt masses: Neutronium 1/64, Cosmic Neutronium 1/32, Infinity 1/16, Transcendent Metal 1/8, SpaceTime 1/4
  SM. It fires at most `cap` bolts per tick, where the cap is set by the **lowest** injector-rail tier found in
  the walk (Mk I 4, Mk II 8, Mk III 16). Short on bolts or on cap, the hole drifts down.
- **Setpoint** (screwdriver): 2, 4, 8, 16, 32 or 64 SM at T1; T2 adds 128, 256 and 512 ("more stellar mass", D-44).
- **Mass buys parallel, heat buys speed:** parallel = 8 × `getTierExtended(maxInputEu)` × ⌊√M⌋, so 64 SM gives ×8
  over the BHC's base and 512 SM gives ×22. Particle energy E = 3200 / M GeV, and the recipe time is divided by
  min(4, E/50): ×1 at 64 SM, ×4 at ≤12.5 SM. A small hole is fast but burns bolts (2 SM needs 51 Neutronium bolts a
  tick, above even Mk III, so it needs SpaceTime bolts at 3.2 a tick); a big hole is wide and cheap to hold.
- The mirror cavity, spin and beam are **structure only** in P2: no beam output, and no Ergomatter (D-42 moves it to
  T7) (Q-8).

**Machine modes** (`supportsMachineModeSwitch`, screwdriver; the cycle skips unavailable modes):

| Mode | Needs | Maps | Notes |
|---|---|---|---|
| COMPRESS | compressor piece | `compressorRecipes` always; `neutroniumCompressorRecipes` only while the hole is open | BHC parity. With the hole closed it runs at the base parallel 8 × tier, like the BHC's plain-compressor mode. |
| EXTRACT | extractor piece | `extractorRecipes` only | GT's item extractor recipes; no fluid extraction, no Kerr-only map (user ruling, Q-9/Q-22) |
| DENSIFY | compressor piece **and** `kerr_t2` **and** M ≥ 128 SM | `beyondthestars.recipe.kerrdensity` (`RecipeMapBuilder.of(...).maxIO(2,1,2,1).minInputs(1,1)`) | HSM (SL-K05). "Density gates the hyperdense stuff" (D-44). |

With neither module built: no modes, the machine idles, and the panel says why. **Power:** standard energy or
exotic (laser) hatches on the 2 governor-arc sockets, at least 1. Recipe EU/t comes from the map; the hole draws no
EU. **Tooltip and panel:** mass, setpoint, bleed, bolts per tick against cap, parallel, speed, mode, modules found,
the density state, and each module's fault string.

### 3.2 Kerr T2 (density): same controller

Kerr T2 is not a new machine. Installing the Density Upgrade switches the required shape from `kerr_t1` to
`kerr_t2`, raises the setpoint range to 512 SM and unlocks DENSIFY. If the T2 reinforcement is missing after the
upgrade is installed, the machine does **not** form, and the panel prints the `kerr_t2` structure error. The upgrade
is a commitment to the bigger build, which is the "required" reading of D-44.

### 3.3 Eye of Harmony T1-T4 (P3): `machines/eye/MTEEyeOfHarmonyBTS.java`, MTE **base+15 (28115)**

One controller (D-34). Display name "Eye of Harmony", with the tooltip line "Beyond the Stars: replaces TecTech's".
Grounded on six pylons over the oval plinth, so its alignment limits allow horizontal facings only. Its parent is
`MTEExtendedPowerMultiBlockBase`, but processing is overridden like TecTech's EOH, because inputs and outputs are
`long`-sized:

- **Pieces:** `eye_core` (T1: core band, two rails, clamps, six pylons, plinth; **required**), `eye_beam` (T2 beam
  band + clamps; optional), `eye_jet` (T3 jet crescent + clamps; optional, and **independent of** `eye_beam`, so T2
  stays optional per D-42), `eye_arms` (T4: two spiral-arm blades; optional).
- **Tier unlock:** T1 = core. T2 recipes need `eye_beam`. T3 recipes need `eye_jet`. T4 recipes need `eye_jet` +
  `eye_arms` + an installed **Galactic Upgrade Core** (sneak-right-click to install; it is consumed; same persistence
  rules as the Kerr upgrade). There are **no** cores for T2/T3: the band is the cost (Q-11). The hologram uses the
  stack size (1..4) and projects the bands cumulatively.
- **Sockets:** 5 gas sockets on the plinth (any `MTEHatchInput`, ME stocking included) drained every 20 ticks into
  `long` buffers for H and He. Stellar-fluid sockets: 6 on the beam band and 5 on the jet crescent, drained into
  buffers for RSPM, ESPM, HSM and (interim recipe only) WDM. Outputs are **ME only** (`MTEHatchOutputBusME`, `MTEHatchOutputME`): 2 sockets on the
  plinth, at least one of each. 1 maintenance socket. All sockets are private `EyeHatchElement`s. Buffers are
  persisted as longs, capped at 2⁶² each, and printed on the panel.
- **Target:** the controller slot (`mInventory[1]`) holds a star chart or a `gtneioreplugin` dimension block; the
  recipe is looked up by that item (TecTech's `recipeLookUp` shape). The target is not consumed.
- **Power:** **wireless EU** from the owner's network (mine.md Q2: plain wireless until T7). The whole run's EU is
  debited at start (`addEUToGlobalEnergyMap(uuid, -cost)`); if the debit fails, the run doesn't start. There is no EU
  return (Q-4). No energy hatches.
- **Parallel:** 2^(installedTier − recipeTier), so a T4 Eye runs Sol ×8, Sirius A ×4, Arcturus ×2 and Sirius B ×1.
  Inputs, outputs and EU scale with it.
- **Success:** stars always succeed. Planets keep TecTech's base 1 − 0.05R. A failed run consumes its inputs and EU
  and outputs nothing. There is no SpaceTime penalty and no stabilisation (D-42: stabilisation starts at T7).
- **Centre render:** client-side render of the band's object (D-34), toggled by a client config
  `eyeRenderCentre` (default true). This is a stretch goal for P3: the structure ships first.

**Process recipes. Every amount is PROPOSED (D-31 is open, Q-6).** Back-solve: each star recipe consumes the
previous star's product **1 : 1 per run**, so one run of each tier chains into the next. WDM per run equals
TecTech's best WDM recipe (R2, 18,432 L). Planet times follow bh-model's T1 band, `t1Minutes(R) = 20 × 1.25^R`,
continued past R3.

| Tier | Target | Inputs per run | Outputs per run | Base time | EU (wireless, per run) |
|---|---|---|---|---|---|
| T1 | Star Chart: Sol | H 1,000,000,000 L + He 1,000,000,000 L | RSPM (`RawStarMatter`) 1,000,000 L | 20 min | RECIPE_UMV × ticks |
| T1 | planets R0-R3 (TecTech dims) | TecTech's H/He for that dim | TecTech's ores and plasmas, **minus** RawStarMatter and the WDM/BDM/Universium special fluid | 20 / 25 / 31 / 39 min | RECIPE_UMV × ticks |
| T2 | Star Chart: Sirius A | RSPM 1,000,000 L + He 1,000,000,000 L | ESPM 100,000 L (10:1, against the Homogeny's 100:1) | 40 min | RECIPE_UMV ×4 × ticks |
| T2 | planets R4-R6 | as above | as above | 49 / 61 / 76 min | RECIPE_UMV ×4 × ticks |
| T3 | Star Chart: Arcturus | ESPM 100,000 L + H 2,000,000,000 L | RHSP 50,000 L | 80 min | RECIPE_UMV ×16 × ticks |
| T3 | planets R7-R8 | as above | as above | 95 / 119 min | RECIPE_UMV ×16 × ticks |
| T4 | Star Chart: Sirius B | HSM 5,000 L + He 4,000,000,000 L | molten WDM 18,432 L | 120 min | RECIPE_UMV ×64 × ticks |
| T4 | Deep Dark (TecTech `DD`) | H/He 10,000,000,000 L each | TecTech's DD ores and every plasma, **no Universium** | 240 min | RECIPE_UMV ×64 × ticks |
| T4 interim | Star Chart: Sirius B, Cooled | WDM 18,432 L + Super Coolant 1,000,000 L | molten BDM 4,608 L | 120 min | RECIPE_UMV ×64 × ticks |

The chain balances: one Sol run (1M RSPM) feeds one Sirius A run (100k ESPM), which feeds one Arcturus run (50k
RHSP), which is 5 HSM batches (5,000 L), which feeds one Sirius B run. **Conversion:** at `FMLLoadCompleteEvent`,
`EyeRecipeRegistry` reads every `EyeOfHarmonyRecipe` from `TecTech.eyeOfHarmonyRecipeStorage`'s private
`recipeHashMap` by reflection, **before** removal, and builds ours: same trigger item, `getOutputItems()`, plasma
fluids, and H/He from `getHydrogenRequirement/getHeliumRequirement`. Rocket tier → Eye tier as R0-3 → T1, R4-6 → T2,
R7-8 → T3, DD → T4. The `RawStarMatter`, WDM, BDM and Universium fluids are dropped (D-42 "without the matters",
Q-12). The interim BDM recipe registers only if `Config.interimBlackDwarfAtEyeT4` (Q-3).

### 3.4 MTE id plan (block base+14 .. base+39 = 28114-28139)

| Id | Offset | Allocation |
|---|---|---|
| 28114 | base+14 | **Kerr Ergosphere Engine**: `multimachine.beyondthestars.kerrengine` (P2) |
| 28115 | base+15 | **Eye of Harmony**: `multimachine.beyondthestars.eyeofharmony` (P3) |
| 28116-28119 | base+16..19 | reserved for P2/P3 spares (none planned; module sockets use GT hatches) |
| 28120-28129 | base+20..29 | P4: Quasar Engine 28120, Wormhole Mouth 28121, Conduit Mirror 28122, Conduit Junction 28123, Burst Thermaliser Crown Mk I/II/III 28124-28126, spare 28127-28129 |
| 28130-28134 | base+30..34 | ON HOLD: Homogeny 28130, Reality Diffraction 28131, Reality Refab 28132, spare 28133-28134 |
| 28135-28139 | base+35..39 | spare |

The Foundation unit adds this table as a comment to the ledger in `BTSMachines.register()`. The Kerr and Eye units
each append their own single registration line. Ids are frozen once shipped.

---

## 4. Recipe removals: `BTSRecipeRemovals.java` (Foundation) + `eye/EyeRecipeRegistry.java` (Eye)

**Load phase.** Removals run in a new `FMLLoadCompleteEvent` handler:
`BeyondTheStars.loadComplete` → `CommonProxy.loadComplete` → `BTSRecipeRemovals.run()`. Add
`after:dreamcraft;after:tectech` to the `@Mod` `dependencies` string. FML dispatches load-complete in dependency
order, so ours runs after TecTech's `onLoadCompleted`, which is where the EOH storage is built. The dreamcraft
assembly-line recipes are long registered by then. Every removal is wrapped in `BTSRecipeGuard.recipe(...)`-style
containment and logs per-collection counts: `[BTS] removed BHC controller: AL-visual 1, AL-data 1, research 1`. It
warns if a count is 0 (the pack changed).

**Shared helper:** `removeAssemblyLineByOutput(ItemStack out)` removes every recipe whose output `isItemEqual(out)`
from:

1. `RecipeMaps.assemblylineVisualRecipes.getBackend()` (via `removeRecipes`)
2. `GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes` (what data sticks resolve against)
3. `TecTechRecipeMaps.researchableALRecipeList` and the matching `researchStationFakeRecipes` entries
4. `RecipeMaps.scannerFakeRecipes` entries whose data-stick output (`AssemblyLineUtils.getDataStickOutput`) matches

If `AssemblyLineUtils.loadedALRecipes` is already true, it clears `AL_RECIPE_LOOKUP` and resets the flag by
reflection, so the lookup rebuilds without the removed recipe.

| Removal | Config switch (`Config.java`, general) | Default | What it does |
|---|---|---|---|
| BHC controller | `removeBlackHoleCompressorRecipe` | true | RC-2 port: first `BTSBlackHoleSwap.run(BHC, Kerr)` rewrites every recipe that consumes a BHC controller to consume the Kerr controller (see "GTNH 2.9.0-RC-2 port"); then, only if the Kerr has an assembly line and no BHC consumer is left, `removeAssemblyLineByOutput(ItemList.Machine_Multi_BlackHoleCompressor.get(1))`. `CommonProxy.serverStarting` re-runs the swap for late consumers |
| TecTech EOH | `replaceTecTechEyeOfHarmony` | true | (a) the Eye registry's conversion snapshot runs first, always; (b) `recipeHashMap.clear()` on `TecTech.eyeOfHarmonyRecipeStorage`, so TecTech's `recipeLookUp` returns null and placed EOHs idle; (c) remove every recipe from `TecTechRecipeMaps.eyeOfHarmonyRecipes` (NEI); (d) `removeAssemblyLineByOutput(CustomItemList.Machine_Multi_EyeOfHarmony.get(1))` |
| Interim BDM | `interimBlackDwarfAtEyeT4` | true | registers the T4 interim cooling recipe (Q-3); remove it when P4 ships Eye T5 |

**Old-save note** (Eye unit appends to `docs/migration.md`):

- A placed Black Hole Compressor keeps working. Only its crafting recipe goes, and data sticks holding it stop
  resolving.
- A placed TecTech Eye of Harmony becomes a museum piece: it forms but finds no recipe. **Finish or cancel running
  EOH recipes before updating.** A run in flight when the recipes are cleared re-resolves to nothing and its outputs
  are lost.
- With `replaceTecTechEyeOfHarmony=false`, TecTech's EOH runs as stock, our Eye coexists, and the pack's
  WDM/BDM/Universium sources are untouched.

---

## 5. Structure plan

### 5.1 Size sanity (measured: headless run of `designs/voxel.js`, render-only cells excluded)

| Structure | Bounding box (W × H × D) | Placed blocks | Cells in box | Chunks spanned |
|---|---|---|---|---|
| Kerr T1 | 76 × 65 × 79 | 15,355 | 390,260 | 5-6 × 5-6 |
| Kerr T2 | 92 × 65 × 89 | 29,831 | 532,220 | 6-7 × 6-7 |
| Eye T1 | 102 × 52 × 80 | 19,616 | 424,320 | 7-8 × 5-6 |
| Eye T2 | 102 × 57 × 80 | 23,959 | 465,120 | |
| Eye T3 | 102 × 57 × 80 | 27,943 | 465,120 | |
| Eye T4 | 102 × 57 × 137 | 34,509 | 796,518 | 7-8 × 9-10 |

Block counts per kind:

- **Kerr T1:** mirrorback 3,551, frame 3,186, mirror 2,852, casing 1,920, coil 842, eyeshell 682, bhc 633, lining 521,
  ring3 315, injector 232, quad 208, sglass 199, dump 158, pipe 31, sockets 25.
- **Kerr T2 adds:** bastion 8,316, lining 4,031, sglass 1,442, casimir 677.
- **Eye T1:** eyeshell 9,732, deck 4,752, lining 3,582, sglass 1,541.
- **Eye T2 adds:** ring2 2,971.
- **Eye T3 adds:** ring3 2,850.
- **Eye T4 adds:** galactic 5,374.

**Verdict: build full size; no scaled build is needed.** The structures are sparse shells (3-6 % fill) and sit in
the same block-count class as GTNH's Forge of the Gods. Conditions:

- **(a) Chunk loading:** the check fails across unloaded chunks, so the tooltip says "chunk-load the full
  footprint".
- **(b) Check cost:** each piece is a trimmed bounding box, and spaces are skipped. Each unit times one
  `checkMachine` with a debug timer during smoke test. If one takes more than 20 ms, re-check only on structure
  updates (GT's default), never per tick.
- **(c) Holograms:** 15-35k hint particles is heavy but works. Survival auto-place is budgeted by StructureLib per
  use (raised to 200 elements a round, see "Hologram & NEI"). Creative testing uses the projector's creative build.

### 5.2 Exporter: `addon/tools/export-structures.js` (committed; Foundation)

- Usage: `node tools/export-structures.js ../designs/voxel.js`, with `--check` to regenerate into a temp dir and diff
  against the committed files (non-zero exit on drift). No npm dependencies.
- **Headless load:** read voxel.js and cut it at the `// ---- GL` banner. Append, inside the IIFE, an export hook
  `globalThis.__vox = { SCENES, BLOCKS, B, RENDER_ONLY, enableTags(){ tagArr = new Uint16Array(W*H*D); }, ... }`.
  Run it in `vm` with stubs: `document.createElement('canvas')` returns a **software canvas** whose 2D context
  implements `fillRect` (1×1 cells), `fillStyle` (an `rgb()` parser), `getImageData` and `drawImage` no-ops; plus
  `$ = () => null` and `window = {}`. A prototype of this load is proven: it produced the numbers in 5.1.
- **Kerr piece tags:** buildKerr has no tags, so the exporter inserts no-op `tag("...")` calls into the **source
  string** at four asserted anchors:
  - `arc(36, 245, 295` → `kerr_compressor`
  - `arc(35, 70, 110` → `kerr_core`
  - `arc(36, 328, 352` → `kerr_extractor`
  - `if (tier>=2){` → `kerr_density`
  
  It throws if any anchor is missing. Cells take the tag of their **last** writer, so pieces are disjoint by
  construction. `kerr_t1` = core cells of the tier-1 build. `kerr_t2` = core + density cells of the tier-2 build. The
  module pieces must be identical in both builds (asserted).
- **Eye pieces by tier diff:** build tiers 1-4. `eye_core` = T1. `eye_beam` = T2 − T1. `eye_jet` = T3 − T2.
  `eye_arms` = T4 − T3. The exporter **asserts** that no non-render cell changes between tiers, only gets added (the
  D-34 overlap audit, re-proved at export).
- Render-only kinds (`/render|not built/`) become `' '` (skip): the centre stays free for the render.
- **Output:** one file per piece, `src/main/resources/assets/beyondthestars/structures/<piece>.txt`. Format:
  - a header `# piece=<name> w=<a> h=<b> d=<c> off=<h>,<v>,<d> sha=<sha1 of voxel.js>`;
  - then `d` layers separated by `---`, each layer `h` rows of `w` chars, top row first, in StructureLib's
    **native** `String[depth][height]` layout (char index = horizontal);
  - `off` = the controller's (horizontal, vertical, depth) offset into this piece, passed straight to
    `checkPiece` / `buildPiece` / `survivalBuildPiece`.
  
  Pieces are trimmed to their own bounding box.
- Also `structures/manifest.json`: per piece, its char → kind → count bill of materials, socket counts and offsets.
  The kit command (`CommandBTSKit`) and the tooltips read their counts from it, never hand-typed (D-22).
- **Orientation:** before the first full export, the Foundation unit exports a 3×3×3 asymmetric test piece and
  confirms in-world with a hologram that controller-left, up and back map the way the loader assumes. Flags
  `--flipH/--flipD` exist for the fix. Controller front: Kerr = outward on the governor arc (+z in voxel space). Eye =
  outward on the plinth at 90°.
- **Loader:** `machines/BTSShapes.java` (Foundation): `static String[][] load(String piece)` reads the resource once
  into a cache. `static int[] offset(String piece)` returns `off`. Shapes are never Java literals (constant-pool
  limits).

### 5.3 Block-kind → shape char → real block

| Char | Voxel kind | Element |
|---|---|---|
| `' '` | empty and every render-only kind | skip |
| `~` | `control` (Kerr governor arc / Eye plinth) | controller |
| `A` | `casing` | `ofBlock(Casings.ExtremeDensitySpaceBendingCasing)`, `noOverwrite` |
| `B` | `frame` | `ofFrame(Materials.NaquadahAlloy)` |
| `C` | `mirror` | slCasing:1 |
| `D` | `mirrorback` | slCasing:2 |
| `E` | `injector` | slCasing:3/4/5, **tiered** (`ofBlocksTiered`; the minimum tier sets the bolt cap) |
| `F` | `quad` | slCasing:7 |
| `G` | `pipe`; and `beamhatch` at Kerr x=17 / x=47 | `Casings.ParticleBeamGuidancePipeCasing` |
| `H` | `coil` | slCasing:6 |
| `I` | `bhc` | `Casings.BackgroundRadiationAbsorbentCasing` |
| `J` | `lining` | slCasing2:2 |
| `K` | `sglass` | slGlass:0 |
| `L` | `eyeshell` | slCasing:9 |
| `M` | `dump`; and the dump arc's `ergohatch`/`output` cells | slCasing:8 |
| `N` | `ring3` inside `buildKerr` | slCasing:15 |
| `O` | `bastion` | slCasing:13 |
| `P` | `casimir` | slCasing:14 |
| `Q` | `deck` | slCasing:0 |
| `R` | `ring2` | slCasing:10 |
| `S` | `ring3` in the Eye | slCasing:11 |
| `T` | `galactic` | slCasing:12 |
| `A` | the 8 `maint` cells beside the bolt buses | casing (not sockets: 8 maintenance hatches would be noise) |

Every plain element is wrapped in `BTSStructures.noOverwrite(...)`.

**Sockets.** Each is `ofChain(hatchAdder, <its arc's casing>)`, so an unused socket may hold casing. The hatch adder
comes first, so the hologram hints the hatch (trap 4). Counts are checked after the walk. The exporter pins sockets
by cell (a table of voxel coordinates), not by guessing.

| Char | Machine | Cells (voxel) | Accepts | Min |
|---|---|---|---|---|
| `e` | Kerr | governor arc (2,0,37.6), (4,0,37.6) | Energy / ExoticEnergy (laser) | 1 |
| `m` | Kerr | governor arc (−2,0,37.6) | plain shell; an old Maintenance hatch is still accepted (the KEE has no maintenance) | 0 |
| `b` | Kerr | 8 `boltbus` at r 34, y ±5 | `KerrHatchElement.BoltBus` (InputBus, private) | 1 |
| `c` | Kerr compressor | 4 `beamhatch` at r 32.6 (255-285°) + the arc's outer `energy`/`maint` cells (0,0/2,−39.6) | InputBus / InputHatch / OutputBus / OutputHatch (standard lists, mark-and-truncate) | 1 in + 1 out |
| `q` | Kerr compressor | the arc's `control` cell (0,−2,−39.6) | slCasing2:0, the module core (exactly 1) | 1 |
| `x` | Kerr extractor | `output` at 334° and 346° | as `c` | 1 in + 1 out |
| `k` | Kerr extractor | `output` at 340° | slCasing2:1, the module core | 1 |
| `h` | Eye | 5 plinth `gas` | `EyeHatchElement.Gas` (MTEHatchInput, private → buffer) | 1 |
| `o` | Eye | 2 plinth `energy` | `EyeHatchElement.Output` (MTEHatchOutputBusME, MTEHatchOutputME) | 1 bus + 1 hatch |
| `m` | Eye | plinth `maint` | Maintenance | 1 |
| `s` | Eye | 6 beam-band + 5 jet-crescent `beamhatch` | `EyeHatchElement.Stellar` (MTEHatchInput, private → buffer) | 0 (T2+ only use them) |

Hint numbers: Kerr: energy 1, (maintenance 2 retired), bolt bus 3, compressor I/O 4, extractor I/O 5. Eye: gas 1, ME output 2,
maintenance 3, stellar 4.

### 5.4 Kerr T1 vs T2; Eye core and bands

- **Kerr:** two complete core shapes (`kerr_t1`, `kerr_t2`) plus two module pieces shared by both. T2 is T1 with the
  reinforcement added: the exporter asserts T2 only adds cells to T1's non-render cells. The larger horizon is
  render-only.
- **Eye:** one required core piece plus three additive band pieces (`eye_beam`, `eye_jet`, `eye_arms`). Only clamps
  join bands, and each clamp belongs to the band built after it. T5-T10 are new pieces in P4/P5. Because every band
  is additive, the core never re-exports.

### 5.5 Textures

The exporter also writes textures using the same software canvas:

- 16×16 PNGs for every new block from the voxel atlas painters (`def(...)` painters → `textures/blocks/sl_*.png`),
  with a ~40-line zlib PNG encoder (Node built-in `zlib`).
- Module cores and injector Mk II/III get a ring recolour.
- Items get a small port of recipes.js's `cond`/`plate` icon painters.

So the in-game look equals the approved mockup tiles.

---

## 6. Build order and file ownership

**Rules.**

- One builder at a time; the orchestrator sequences Foundation → Kerr → Eye.
- Every unit commits granular local `wip:` commits staging **only** its own files. Never `git add -A`; never push,
  tag, reset, stash, checkout or rebase.
- Shared files take **append-only** edits: add lines at the end of the relevant block, never reorder or reformat
  others' lines. Shared files: `BTSMachines.java`, `CommonProxy.java`, `BTSRecipes.java`, `en_US.lang`,
  `CommandBTSKit.java`, `BTSBlocks.java`, `BTSFluids.java`, `Config.java`, `docs/migration.md`.
- Every change is checked against the four regression traps.

### 6.1 Foundation (P1)

**Owns (new files):**

- `tools/export-structures.js`
- `src/main/resources/assets/beyondthestars/structures/*` (generated)
- `textures/blocks/sl_*.png` and `textures/items/` for every §1.3 item
- `items/SLItems.java`
- `fluids/SLFluids.java`
- `blocks/SLBlocks.java`, `blocks/BlockSLCasing.java`, `blocks/BlockSLCasing2.java`, `blocks/BlockSLGlass.java`,
  `blocks/ItemBlockSL.java`
- `machines/BTSShapes.java`
- `BTSRecipesSingularity.java`
- `BTSRecipeRemovals.java` (the helper + the BHC removal + the config switches)

**Shared edits:**

- `BeyondTheStars.java`: the `loadComplete` handler and `after:dreamcraft;after:tectech`.
- `CommonProxy.java`: `loadComplete()`; `SLItems.register()` after `BTSItems.register()`.
- `BTSBlocks.java`: one line calling `SLBlocks.register()`.
- `BTSFluids.java`: one line calling `SLFluids.register()`.
- `Config.java`: the three removal flags + `eyeRenderCentre`.
- `BTSRecipes.java`: `line("singularity", ...)`.
- `BTSMachines.java`: the §3.4 ledger comment only.
- `en_US.lang`: §1 names.

**Checklist:**

1. Run the exporter and verify the orientation test piece in-world.
2. Commit the exporter and the structures.
3. Register blocks, items and fluids, and generate textures and lang. Compile.
4. Add the parts recipes (SL-P01..P26); the boot log must read `0 skipped`.
5. Add the removal framework + BHC removal + flags; the boot log must show the counts.
6. Smoke test: the parts appear in NEI and the BHC AL recipe is gone.

### 6.2 Kerr unit (P2)

**Owns:**

- `machines/kerr/MTEKerrEngine.java`
- `machines/kerr/KerrHatchElement.java`
- `machines/kerr/KerrRecipeMaps.java` (the `kerrdensity` map)
- `BTSRecipesKerr.java`
- controller overlay textures if any

**Shared edits (append-only):**

- `BTSMachines.java`: `kerrEngine` field + the base+14 line.
- `BTSRecipes.java`: `line("kerr", ...)`.
- `CommandBTSKit.java`: controller, module cores, rails, bolts, seed, density upgrade; counts from
  `manifest.json`.
- `en_US.lang`: the Kerr section.

**Checklist:**

1. Build the core shape, controller and hologram for T1 only.
2. Add the module pieces with mark-and-truncate and the private bolt buses.
3. Add the hole state, governor, NBT and the item-NBT path.
4. Add the modes and parallel.
5. Add the density upgrade install and the T2 shape.
6. Add recipes SL-K01..K05.
7. Smoke test: form T1 by projection, run a compressor recipe and a neutronium recipe with the hole open, wrench and
   replace (state kept), install the density upgrade, form T2, run HSM.

### 6.3 Eye unit (P3)

**Owns:**

- `machines/eye/MTEEyeOfHarmonyBTS.java`
- `machines/eye/EyeHatchElement.java`
- `machines/eye/EyeRecipe.java`
- `machines/eye/EyeRecipeRegistry.java` (TecTech conversion, star recipes, the TecTech EOH removal, the NEI fake
  map)
- `BTSRecipesEye.java`
- the centre render (stretch goal)

**Shared edits (append-only):**

- `BTSMachines.java`: `eyeOfHarmony` field + the base+15 line.
- `BTSRecipes.java`: `line("eye", ...)`.
- `CommonProxy.loadComplete()`: one line `EyeRecipeRegistry.onLoadComplete()`, placed **before** the Foundation's
  `BTSRecipeRemovals.run()` line, or have the registry call the removal itself (coordinate through the orchestrator).
- `CommandBTSKit.java`
- `en_US.lang`
- `docs/migration.md` (the old-save note)

**Checklist:**

1. Build the core shape, controller and hologram for T1.
2. Add the long buffers + sockets + the ME output path.
3. Add the wireless debit + the Sol recipe.
4. Add the TecTech conversion (planets/DD) and the NEI fake map.
5. Add the bands T2/T3 + star recipes.
6. Add the Galactic core install + `eye_arms` + T4 recipes + the interim BDM.
7. Add the TecTech EOH removal under its flag.
8. Smoke test: form T1-T4 by projection, run Sol, run a planet (ores to ME), check the TecTech EOH shows no recipes,
   and check that with the flag off both coexist.

### 6.4 Not in P1-P3

Homogeny, Reality machines, Quasar, Conduit, Eye T5+, questbook changes (P6), and the D-45/D-48 pack-recipe swaps
(UXV parts dropping Universium, the Eternity QFT loop on Ergomatter, Magnetic Monopole Matter dropping its MHDCSM
prerequisite). Those swaps matter for Q-3 and are listed there.

---

## Contract additions (foundation-code)

Written by the Foundation code unit on 2026-10-12. These pin names the spec left open and apply the mid-run user
ruling ("use the eye of harmony blocks that currently exist"; prefer the Black Hole Compressor's blocks for the
Kerr). Where this section and §1.2 disagree, this section wins.

### FC-1 Which §1.2 blocks are registered

Four §1.2 roles are played by **existing** blocks, so they are **not registered** (no block, no recipe, no lang):

| Spec block | Voxel kind | Eye uses (TecTech EOH family) | Kerr uses (BHC family) |
|---|---|---|---|
| slCasing:0 Containment Deck Plating | `deck` | Reinforced Temporal Structure Casing (`Casings.ReinforcedTemporalStructureCasing` = `TTCasingsContainer.sBlockCasingsBA0`:10) | (Kerr has no `deck` cells) |
| slCasing:9 Eye Shell Casing | `eyeshell` | Reinforced Spatial Structure Casing (`Casings.ReinforcedSpatialStructureCasing` = BA0:11) | Extreme Density Space-Bending Casing (`Casings.ExtremeDensitySpaceBendingCasing`) |
| slCasing2:2 Harmonic Lining | `lining` | Infinite Spacetime Energy Boundary Casing (`Casings.InfiniteSpacetimeEnergyBoundaryCasing` = BA0:12) | **Since the round-4 ruling: the NEW Harmonic Lining itself** (slCasing2:2 is registered again, Kerr only). Before: Background Radiation Absorbent Casing. |
| slGlass:0 Spatial Glass Channel (`BlockSLGlass` is not created) | `sglass` | Quantum Glass (`Casings.QuantumGlass`, TecTech) | Hawking Radiation Realignment Focus (`Casings.HawkingRadiationRealignmentFocus`, the BHC's glass) |

These are recommendations to the Kerr and Eye units, who own the char-to-element mapping; any existing block is
fine as long as no new block is invented for these roles. The TecTech field generators
(`TTCasingsContainer.SpacetimeCompressionFieldGenerators` / `TimeAccelerationFieldGenerator` /
`StabilisationFieldGenerators`, tiered metas 0-8, `ofBlocksTiered` as TecTech's EOH uses them) are available to the
Eye unit; no voxel kind maps to them yet (see the questions file).

All other metas keep their spec numbers. The unregistered metas stay as **holes** (never reused), so the §5.3 table
and every unit's references stay valid.

**Recipe fallout.** SL-P01, SL-P11 and SL-P13 are retired (the IDs stay reserved, like SL-P20). SL-P12 (Harmonic
Lining) was retired too, and is back since the round-4 ruling as the Kerr's lining recipe (§2.2).
SL-P21 (Stabiliser Pylon) takes `EOH_Reinforced_Spatial_Casing` ×32, `EOH_Infinite_Energy_Casing` ×16 and
`EOH_Reinforced_Temporal_Casing` ×16 in place of Eye Shell ×32, Harmonic Lining ×16 and Deck ×16. Suggested for the
other units: SL-K01 takes `ExtremeDensitySpaceBendingCasing` ×32 for Deck ×32; SL-E01 takes
`EOH_Infinite_Energy_Casing` ×64 for Harmonic Lining ×64 and drops the Eye Shell ×64 / Deck ×64 lines (the card
already has Spatial ×64 and Temporal ×64).

### FC-2 Registry, lang and texture names

Blocks (`blocks/SLBlocks.java`, fields `SLBlocks.slCasing`, `SLBlocks.slCasing2`; meta constants on
`BlockSLCasing` / `BlockSLCasing2`): registry names `slCasing`, `slCasing2`, item block `ItemBlockSL`. Lang key
`tile.beyondthestars.<registry>.<meta>.name`. One icon per meta on all six faces, 16×16 PNG at
`src/main/resources/assets/beyondthestars/textures/blocks/<texture>.png`:

| Block:meta | Name | Texture |
|---|---|---|
| slCasing:1 | Superradiant Mirror Casing | `sl_mirror` |
| slCasing:2 | Mirror Backing Casing | `sl_mirrorback` |
| slCasing:3 | Bolt Injector Rail (Mk I) | `sl_injector_mk1` |
| slCasing:4 | Bolt Injector Rail (Mk II) | `sl_injector_mk2` |
| slCasing:5 | Bolt Injector Rail (Mk III) | `sl_injector_mk3` |
| slCasing:6 | Ergosphere Field Coil | `sl_coil` |
| slCasing:7 | Beam Focusing Quadrupole | `sl_quad` |
| slCasing:8 | Beam Dump | `sl_dump` |
| slCasing:10 | Beam Band Casing | `sl_ring2` |
| slCasing:11 | Jet Crescent Casing | `sl_ring3` |
| slCasing:12 | Galactic Shell Plating | `sl_galactic` |
| slCasing:13 | Bastion Plate | `sl_bastion` |
| slCasing:14 | Casimir Plate | `sl_casimir` |
| slCasing:15 | Ergosphere Extraction Casing | `sl_extraction` |
| slCasing2:0 | Kerr Compressor Module | `sl_module_compressor` |
| slCasing2:1 | Kerr Extractor Module | `sl_module_extractor` |
| slCasing2:2 | Harmonic Lining (Kerr lining, round-4 ruling) | `sl_lining` |

`sl_deck`, `sl_eyeshell` and `sl_sglass` are not referenced by code (harmless if present).

Items (`items/SLItems.java`, fields named as the §1.3 internal names): texture
`textures/items/<internalName>.png`, e.g. `superradiantMirrorSegment.png`, `starChartSiriusBCooled.png`; lang
`item.beyondthestars.<internalName>.name`.

Fluids (`fluids/SLFluids.java`, fields `excitedStellarPlasma`, `rawHyperdensePlasma`, `hyperdenseStellarMixture`):
GT's tinted `molten.autogenerated` texture, like every existing BTS fluid, so they need no PNG.

### FC-3 Shape loader API (`machines/BTSShapes.java`)

- Resource `/assets/beyondthestars/structures/<piece>.txt`, UTF-8. `\r` is stripped. The first non-blank line must
  start with `# piece=`; other `#` lines are ignored. Rows shorter than `w` are right-padded with spaces (so stripped
  trailing whitespace is harmless); longer rows, or a layer/row count that disagrees with the header, throw.
- `String[][] load(String piece)` → `[d][h]`, each string `w` chars (StructureLib native), cached; callers must not
  mutate it. `int[] offset(String piece)` → `{h, v, d}`. `int[] size(String piece)` → `{w, h, d}`.
  `String sha(String piece)`. `int count(String piece, char c)` counts a char straight from the shape.
- `JsonObject manifest()` parses `structures/manifest.json` once (Gson). `JsonObject manifestPiece(String piece)`
  returns `manifest().pieces.<piece>` if the root has a `pieces` object, else `manifest().<piece>`; null if absent.
- A missing or malformed file throws `IllegalStateException` naming the resource, so a broken shape fails the
  controller's class-init loudly instead of forming nothing.

### FC-4 Load-complete hook and removals (`BTSRecipeRemovals.java`)

- `@Mod.EventHandler loadComplete(FMLLoadCompleteEvent)` → `proxy.loadComplete(event)` →
  `CommonProxy.loadComplete()`, whose body is a list of lines. The Eye unit inserts
  `EyeRecipeRegistry.onLoadComplete();` **above** the `BTSRecipeRemovals.run();` line.
- `public static int removeAssemblyLineByOutput(ItemStack out, String label)` removes from the four collections of
  §4, resets the AL lookup, logs `[BTS] removed <label>: AL-visual a, AL-data b, research c, scanner d` (WARN when
  everything is 0) and returns the total. Safe to call from the Eye registry for the TecTech EOH controller.
- `public static void guard(String label, Runnable body)` contains any throw to that one removal and logs it.
- Config (`Config.java`, category `general`): `removeBlackHoleCompressorRecipe`, `replaceTecTechEyeOfHarmony`,
  `interimBlackDwarfAtEyeT4` (all default true), `eyeRenderCentre` (default true, client cosmetic).

---

## Contract additions (textures)

Defined by the "textures" unit. **Block, item and fluid names follow FC-2 above exactly** (FC-2 was committed first;
an earlier draft of this section used other block names and is superseded). Every PNG is generated by
`tools/gen-sl-textures.js` (Node, no dependencies): `node tools/gen-sl-textures.js` rewrites them all,
`--check` regenerates in memory and exits non-zero if a committed PNG's pixels differ. The file list is the manifest
at the top of the script. Paths are under `src/main/resources/assets/beyondthestars/textures/`.

- **Blocks:** `blocks/<texture>.png` for the 17 FC-2 textures (`sl_mirror` … `sl_module_extractor`, `sl_lining`).
  The three retired roles (`sl_deck`, `sl_eyeshell`, `sl_sglass`) are not generated. Family: Kerr blocks
  (`sl_mirror`, `sl_mirrorback`, `sl_injector_mk1..3`, `sl_coil`, `sl_quad`, `sl_dump`, `sl_bastion`, `sl_casimir`,
  `sl_extraction`, `sl_module_*`, `sl_lining`) are drawn on the Black Hole Compressor's Extreme Density palette with its purple
  accents; Eye blocks (`sl_ring2`, `sl_ring3`, `sl_galactic`) carry TecTech's EOH casing bevel and blue palette.
- **Items:** `items/<internalName>.png` for the nine §1.3 items.
- **Fluids:** none. FC-2 keeps GT's tinted `molten.autogenerated` (the registered RGBA gives each fluid its colour), so
  no fluid PNG is needed and none is shipped.
- **Controller front overlays:** `blocks/iconsets/<NAME>.png`, loaded with
  `Textures.BlockIcons.custom("beyondthestars:iconsets/<NAME>")` (GT keeps a name containing `:` as-is; the
  `iconsets/` sub-path matches GT's own layout) and layered with `MTEProductionHallBase.overlayPair(icon, glow)`-style
  `TextureFactory` builders (the glow layer gets `.glow()`):

| Machine | Idle | Idle glow | Active (animated) | Active glow (animated) |
|---|---|---|---|---|
| Kerr Ergosphere Engine | `OVERLAY_FRONT_KERR_ENGINE` | `OVERLAY_FRONT_KERR_ENGINE_GLOW` | `OVERLAY_FRONT_KERR_ENGINE_ACTIVE` | `OVERLAY_FRONT_KERR_ENGINE_ACTIVE_GLOW` |
| Eye of Harmony | `OVERLAY_FRONT_EYE_OF_HARMONY` | `OVERLAY_FRONT_EYE_OF_HARMONY_GLOW` | `OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE` | `OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE_GLOW` |

  Overlays are transparent outside the emblem, so they sit on the controller's casing background (Kerr: the
  `Casings.ExtremeDensitySpaceBendingCasing` texture index; Eye: TecTech's Reinforced Spatial Structure Casing index).
  Active files are vertical strips of 16×16 frames with a `.mcmeta` (`frametime` inside).

---

## Contract additions (structures)

Written by the structures unit on 2026-10-12, early, so the loader matches the files. The full "Structure export"
section (run command, legend, manifest) follows once the exporter is committed.

- **Blank rows are rows.** Shape rows are written with trailing spaces stripped, so a row that is all skip is an
  **empty line**. Inside a layer, every line up to the next `---` (or end of file) is a row, blank or not; the loader
  right-pads each to `w`. A layer always has exactly `h` lines. Only lines before the header and `#` lines are
  skipped.
- **Header** is exactly `# piece=<name> w=<w> h=<h> d=<d> off=<h>,<v>,<d> sha=<sha1>` on the first line. Extra
  `# key=value` lines may follow it (ignored by the loader).
- **Layer order**: layer 0 is the controller's front (the outermost +z layer of the mockup), rows run top to
  bottom, chars run left to right as seen from in front of the controller (mockup +x), so `[d][h]` is StructureLib's
  native layout with no `transpose`.

---

## Structure export

Written by the structures unit, 2026-10-12. This section is the contract for the generated shape files. Where it
and §5.2/§5.3 disagree, this section wins. It builds on "Contract additions (foundation-code)" FC-1 and FC-3.

### Run

From the addon root (Node 18+, no npm dependencies):

```
node tools/export-structures.js ../designs/voxel.js          # regenerate src/main/resources/assets/beyondthestars/structures/
node tools/export-structures.js ../designs/voxel.js --check  # regenerate in memory, diff against the committed files, exit 1 on drift
```

The voxel path defaults to `../designs/voxel.js`. Options:

- `--out <dir>` writes somewhere else.
- `--flipH` / `--flipD` mirror the horizontal / depth axis if the in-world orientation test fails.
- `--quiet` prints only the result.

A run takes under a second. It prints every sanity check and writes nothing if one fails. Re-run it and commit the
output whenever `designs/voxel.js` changes. The `sha=` in every header (sha1 of voxel.js with LF line endings) tells
you which mockup a shape came from.

**voxel.js is never edited.** The exporter cuts the source at the GL banner, splices these hooks into the source
**string** at asserted anchors (it throws if an anchor is missing or not unique), and runs it in `vm`:

| Hook | Why |
|---|---|
| `def("boltbus", ...)` after the last `def` | `buildKerr` writes `B.boltbus`, which the mockup never defines, so its 8 bolt-bus cells came out **empty** (the §5.1 counts are 8 short). |
| `tag("kerr_core")` at the top of `buildKerr` and before `arc(35, 70, 110`; `tag("kerr_compressor")` before `arc(36, 245, 295`; `tag("kerr_extractor")` before `arc(36, 328, 352`; `tag("kerr_density")` inside `if (tier>=2){` | Kerr piece membership (§5.2). |
| `tag("pad")` / `tag("ground")` around the pylon foot pads | Each pad's centre ring (8 cells around the foot, 48 in all) becomes the stabilisation field generators (below); the other 246 pad cells stay plinth `Q` (cost audit #5). |
| `if (globalThis.__noClamps) return;` at the top of `clamp()` | A second T1 build without clamps separates a clamp **strut** (air without clamps) from a clamp root inside a band. |
| `if (!globalThis.__noCentre)` before `centreRender(...)` | The centre is render-only (D-34), but the T4 render draws one "planet" with the solid `ring2` block. That would leave 7 stray Beam Band casings in the middle of `eye_arms`, so the exporter never draws the centre. |

### Pieces

The files are `structures/<piece>.txt`, `structures/manifest.json` and `structures/orient_test.txt`. The `.txt`
format is the earlier "Contract additions (structures)" section, and `BTSShapes` parses it.

| Piece | w × h × d | off (h,v,d) | Blocks | Bill of materials (char count) |
|---|---|---|---|---|
| `kerr_t1` | 76 × 65 × 71 | 38,32,0 | 13,899 | `D` 3,551, `B` 3,186, `C` 2,852, `A` 1,920, `H` 842, `L` 682, `E` 232, `F` 208, `M` 160, `J` 131, `K` 95, `G` 20, `u` 8, `b` 8, `e` 2, `m` 1, `~` 1 |
| `kerr_t2` | 92 × 65 × 89 | 44,32,6 | 28,375 | `O` 8,316, `J` 4,162, `D` 3,551, `B` 3,186, `C` 2,852, `A` 1,920, `K` 1,537, `H` 842, `L` 682, `P` 677, `E` 232, `F` 208, `M` 160, `G` 30, `u` 8, `b` 8, `e` 2, `m` 1, `~` 1 |
| `kerr_compressor` | 33 × 11 × 20 | 16,5,-59 | 965 | `I` 633, `J` 253, `K` 60, `G` 12, `c` 6, `q` 1 |
| `kerr_extractor` | 9 × 11 × 16 | -29,5,-43 | 499 | `N` 315, `J` 137, `K` 44, `x` 2, `k` 1 |
| `eye_core` | 102 × 52 × 80 | 50,50,0 | 19,616 | `U` 9,406, `Q` 4,704, `J` 3,582, `Z` 1,541, `W` 188, `X` 138, `Y` 48 (was 294; cost audit #5), `h` 5, `o` 2, `~` 1, `m` 1 |
| `eye_beam` | 92 × 9 × 69 | 45,55,-6 | 4,343 | `R` 2,856, `J` 1,106, `Z` 375, `s` 6 |
| `eye_jet` | 97 × 12 × 67 | 48,33,-10 | 3,984 | `S` 2,750, `J` 908, `Z` 321, `t` 5 |
| `eye_arms` | 95 × 10 × 137 | 47,44,28 | 6,559 | `T` 5,325, `J` 830, `Z` 404 |

- **One controller anchor per piece.**
  - `kerr_t1`, `kerr_t2` and `eye_core` each hold exactly one `~`, at `off`.
  - The module and band pieces hold no `~`. Their `off` is the same controller cell in the piece's own frame, so it
    can be negative or outside the piece; StructureLib accepts that.
  - Pass each piece its own `off` in `checkPiece` / `buildPiece` / `survivalBuildPiece`.
- **Assembled sizes match §5.1.** The exporter checks these:

  | Build | Size | Blocks |
  |---|---|---|
  | Kerr T1 | 76 × 65 × 79 | 15,363 |
  | Kerr T2 | 92 × 65 × 89 | 29,839 |
  | Eye T1 | 102 × 52 × 80 | 19,616 |
  | Eye T2 | 102 × 57 × 80 | 23,959 |
  | Eye T3 | 102 × 57 × 80 | 27,943 |
  | Eye T4 | 102 × 57 × 137 | 34,502 |

  The Kerr counts are 8 higher than §5.1 because the bolt buses now exist. Eye T4 is 7 lower because the stray centre
  planet is gone.
- **Orientation.** Both machines face **+z** in voxel space: the Kerr outward on the governor arc, the Eye outward on
  the plinth at 90°. Layer 0 is the front. Rows run top to bottom. Chars run left to right as seen from in front of
  the controller (= voxel +x), so the in-game geometry is the mockup, unmirrored.
- **Verified in-world (2026-10-12, headless dev server, questions B-27):** in all four facings the mockup's +x
  landmarks (the Kerr's extractor core, the Eye core's east gas hatch and the jet band's east socket) stand on the
  viewer's right, seen from in front of the controller. So the shapes are unmirrored, and `--flipH` / `--flipD`
  stay off. The original manual check, kept for reference: build `orient_test` and check:
  - The controller `~` is front-centre.
  - `R` is to the viewer's right of the controller. If it is on the left, re-export with `--flipH`.
  - `U` is at the top-left front corner.
  - `BBB` runs along the top of the back layer. If it is in front, re-export with `--flipD`.

  Commit any re-export.

### Overlap policy

- **Kerr: the last writer owns a cell** (§5.2). The compressor arc is drawn over 40 core cells (the r 33 frame ring
  passes through it) and the extractor arc over 9, so those cells belong to the module. Without the module the
  frame ring has a gap there, which is also what the mockup shows.
- **Kerr T2 ⊇ T1.** `kerr_t2` contains every `kerr_t1` cell with the same char. The modules are identical in both
  builds. The exporter asserts both.
- **Eye: the earlier piece keeps a cell.** §5.2 asserted that no tier changes an earlier cell. The mockup breaks this
  in three places, all at joins:
  - the T2 clamps' roots (115 cells);
  - the T3 clamps' roots (100 cells);
  - the T4 blade roots, which cut into the core band (84 cells).

  The earlier piece keeps those cells and the later piece starts where it ends. So every band piece only adds
  cells, and `eye_core` never changes (Q-19).
- **Disjoint.** No two pieces of one machine share a cell, except the alternatives `kerr_t1` / `kerr_t2`. The
  exporter checks this.

### Legend: char → role → intended block

The manifest's `legend` carries the same tables. They follow the mid-run ruling and FC-1: existing TecTech EOH blocks
on the Eye, and Black Hole Compressor blocks on the Kerr. The Java units own the final char → element mapping; this
is the intent. Every plain element is wrapped in `BTSStructures.noOverwrite(...)`.

**Kerr** (`kerr_*`):

| Char | Role | Voxel kind | Intended block |
|---|---|---|---|
| `~` | controller | `control` (governor arc) | Kerr Ergosphere Engine (28114) |
| `A` | cage casing | `casing` | `Casings.ExtremeDensitySpaceBendingCasing` (BHC) |
| `B` | cage frame | `frame` | `ofFrame(Materials.NaquadahAlloy)` (BHC) |
| `C` | mirror | `mirror` | NEW slCasing:1 Superradiant Mirror Casing |
| `D` | mirror backing | `mirrorback` | NEW slCasing:2 Mirror Backing Casing |
| `E` | injector rail | `injector` | NEW slCasing:3/4/5 Bolt Injector Rail Mk I/II/III, `ofBlocksTiered` (the lowest tier sets the bolt cap) |
| `F` | quadrupole | `quad` | NEW slCasing:7 Beam Focusing Quadrupole |
| `G` | beam pipe | `pipe`, plus the `beamhatch` cells at x=17 and x=47 | `Casings.ParticleBeamGuidancePipeCasing` |
| `H` | field coil | `coil` | NEW slCasing:6 Ergosphere Field Coil |
| `I` | compressor shell | `bhc` | `Casings.BackgroundRadiationAbsorbentCasing` (BHC) |
| `J` | arc lining | `lining` | NEW slCasing2:2 Harmonic Lining (round-4 ruling: the white inner face; was the BHC's Background Radiation Absorbent Casing) |
| `K` | glass channel | `sglass` | `Casings.HawkingRadiationRealignmentFocus` (the BHC's glass) |
| `L` | arc shell | `eyeshell` (governor and dump arcs) | `Casings.ExtremeDensitySpaceBendingCasing` (FC-1) |
| `M` | beam dump | `dump`, plus the dump arc's `ergohatch` and `output` cells | NEW slCasing:8 Beam Dump |
| `N` | extractor shell | `ring3` | NEW slCasing:15 Ergosphere Extraction Casing |
| `O` | bastion | `bastion` | NEW slCasing:13 Bastion Plate |
| `P` | casimir | `casimir` | NEW slCasing:14 Casimir Plate |

**Eye** (`eye_*`):

| Char | Role | Voxel kind | Intended block |
|---|---|---|---|
| `~` | controller | `control` (plinth, 90°) | Eye of Harmony (28115) |
| `U` | band shell | `eyeshell` (core band, rails, pylons, clamp roots) | `Casings.ReinforcedSpatialStructureCasing` (BA0:11) |
| `Q` | plinth | `deck` | `Casings.ReinforcedTemporalStructureCasing` (BA0:10) |
| `J` | band lining | `lining` | `Casings.InfiniteSpacetimeEnergyBoundaryCasing` (BA0:12) |
| `Z` | glass channel | `sglass` | `Casings.QuantumGlass` (TecTech) |
| `W` | spacetime compression field | T1 clamp struts, upper rail → core | `TTCasingsContainer.SpacetimeCompressionFieldGenerators` metas 0-8, `ofBlocksTiered` (Q-20) |
| `X` | time dilation field | T1 clamp struts, core → lower rail | `TTCasingsContainer.TimeAccelerationFieldGenerator` metas 0-8, `ofBlocksTiered` (Q-20) |
| `Y` | stabilisation field | the centre ring (8 cells) of each of the six 7 × 7 pylon foot pads (`deck`), 48 in all; the rest of each pad is `Q` | `TTCasingsContainer.StabilisationFieldGenerators` metas 0-8, `ofBlocksTiered` (Q-20) |
| `R` | beam band | `ring2` | NEW slCasing:10 Beam Band Casing |
| `S` | jet crescent | `ring3` | NEW slCasing:11 Jet Crescent Casing |
| `T` | galactic shell | `galactic` | NEW slCasing:12 Galactic Shell Plating |

The Kerr and the Eye use separate structure definitions, so a char means only what its own machine's table says.
`J`, `m` and `~` appear in both, with the same role but different blocks.

### Sockets

Each socket is `ofChain(hatchAdder, fallback)`, hatch adder first (trap 4), and the counts are checked after the walk.

The exporter pins sockets by voxel coordinate in its socket table. The export aborts if a hatch-like cell is missing
from the table, or if a table entry's cell holds another kind. The manifest lists every socket per piece with:

- `abc`: its position in the piece;
- `rel`: its offset from the controller, in StructureLib a/b/c;
- `voxel`: its mockup coordinate.

| Char | Piece | Count | Accepts | Fallback | Min | Hint |
|---|---|---|---|---|---|---|
| `e` | `kerr_t1/t2` (governor arc) | 2 | Energy / ExoticEnergy | `L` | 1 | 1 |
| `m` | `kerr_t1/t2` (governor arc) | 1 | shell first, then an old Maintenance hatch (KEE has no maintenance) | `L` | 0 | - |
| `b` | `kerr_t1/t2` (r 34, y ±5) | 8 | `KerrHatchElement.BoltBus` (private input-bus list) | `A` | 1 | 3 |
| `u` | `kerr_t1/t2` (beside each bolt bus) | 8 | BHC utility hatch `MTEBlackHoleUtility` (id 3012, redstone hole state); optional (Q-21) | `A` | 0 | 6 |
| `c` | `kerr_compressor` | 6 | InputBus / InputHatch / OutputBus / OutputHatch (standard lists, mark-and-truncate) | `I` | 1 in + 1 out | 4 |
| `q` | `kerr_compressor` | 1 | the module core block, NEW slCasing2:0 Kerr Compressor Module (not a hatch) | none | 1 | |
| `x` | `kerr_extractor` (334°, 346°) | 2 | as `c` | `N` | 1 in + 1 out | 5 |
| `k` | `kerr_extractor` (340°) | 1 | the module core block, NEW slCasing2:1 Kerr Extractor Module | none | 1 | |
| `h` | `eye_core` (plinth) | 5 | `EyeHatchElement.Gas` (MTEHatchInput, private) | `Q` | 1 | 1 |
| `o` | `eye_core` (plinth; the mockup's `energy` cells) | 2 | `EyeHatchElement.Output` (MTEHatchOutputBusME, MTEHatchOutputME) | `Q` | 1 bus + 1 hatch | 2 |
| `m` | `eye_core` (plinth) | 1 | Maintenance | `Q` | 1 | 3 |
| `s` | `eye_beam` | 6 | `EyeHatchElement.Stellar` (MTEHatchInput, private) | `R` | 0 | 4 |
| `t` | `eye_jet` | 5 | `EyeHatchElement.Stellar` | `S` | 0 | 4 |

Every socket has a face that reaches open air outside the fully built machine. The exporter checks this on Kerr T1
and T2 with both modules, and on Eye T1 and T4.

Changes from §5.3:

- The Eye's stellar sockets are split into `s` (beam band) and `t` (jet crescent), so each falls back to its own band
  casing.
- The 8 cells beside the bolt buses are `u` sockets, not plain casing (Q-21).
- The Eye's field generators get the new chars `W`/`X`/`Y`.
- The Eye's `deck`/`eyeshell`/`lining`/`sglass` and the Kerr's `eyeshell`/`sglass` follow FC-1. The Kerr's `lining` is
  the new Harmonic Lining (round-4 ruling).

### manifest.json

The top level is `{ generator, source, sha, format, orientation, legend: { kerr, eye }, machines: { kerr, eye },
assembled, pieces, checks, notes }`.

`pieces.<piece>`, which `BTSShapes.manifestPiece(piece)` returns, is `{ machine, file, w, h, d, off, blocks, bom:
[{ ch, count, role, block }], socketCounts: { ch: n }, sockets: { ch: [{ abc, rel, voxel }] },
cellsKeptByEarlierPiece? }`.

The kit command and the tooltips read their counts from `bom` / `socketCounts` (D-22), never hand-typed.

---

## Contract additions (Kerr)

Written by the Kerr unit on 2026-10-12. This records what P2 built. Where it and §3.1/§3.2/§2.3 disagree, this
section wins; each deviation is a question (Q-22..Q-28).

> **Kerr redesign (branch `kerr-redesign`).** The KEE now controls only the hole and the modules are separate
> machines. [`kerr-redesign.md`](kerr-redesign.md) is the spec and its "As built" table lists the files; this
> section has been updated where it changed (files, shape chars, behaviour, adding a module). Questions B-46..B-53.

### Files

| File | What it is |
|---|---|
| `machines/kerr/MTEKerrEngine.java` | The KEE, MTE 28114 (`multimachine.beyondthestars.kerrengine`, "Kerr Ergosphere Engine"): the hole only. GUI: `MTEKerrEngineGui`. |
| `machines/kerr/MTEKerrCompressorModule.java`, `MTEKerrExtractorModule.java` | The module machines, MTE 28117 / 28118 (`multimachine.beyondthestars.kerr.compressor` / `.extractor`), on `MTEKerrModuleBase` + `MTEKerrModuleGui`. |
| `machines/kerr/KerrSlot.java`, `KerrHole.java`, `KerrMigration.java` | The slot table (manifest), the module's read-only view of the KEE, and the legacy conversion. |
| `machines/kerr/KerrHatchElement.java` | The KEE's private sockets: `BoltBus` and `Utility`. |
| `machines/kerr/KerrRecipeMaps.java` | `DENSITY` (`beyondthestars.recipe.kerrdensity`, maxIO 2/1/2/1). The `EXTRACTION` map is retired (the extractor runs GT's `extractorRecipes`). |
| `machines/kerr/KerrKit.java` | `/btskit kerr` and the creative build used by `/btskerr`. |
| `BTSRecipesKerr.java` | SL-K01..SL-K05 (`line("kerr")`); SL-K06 is retired. |
| `CommandBTSKerr.java` | `/btskerr [t1\|t2] [force]`, `/btskerr migrate [x y z]`, test bench `/btskerr legacy x y z`. |

Shared edits, each append-only: the `kerrEngine` field and its base+14 line in `BTSMachines`; `line("kerr")` in
`BTSRecipes`; a `kerr` branch at the top of `CommandBTSKit.processCommand`; `CommandBTSKerr` in
`CommonProxy.serverStarting`; and the Kerr section of `en_US.lang`.

### Shape char → element (Kerr)

The exported shapes are used unchanged; the exporter needed no new anchors. One `StructureDefinition` holds all four
pieces, and each is checked at its own `off`. Every plain element is wrapped in `noOverwrite` and counts into
`mCasing`.

| Char | Element |
|---|---|
| `A`, `L` | `Casings.ExtremeDensitySpaceBendingCasing` |
| `B` | `ofFrame(Materials.NaquadahAlloy)` |
| `C` / `D` / `F` / `H` / `M` / `N` / `O` / `P` | slCasing 1 / 2 / 7 / 6 / 8 / 15 / 13 / 14 |
| `E` | slCasing 3/4/5 in any mix. The walk records the lowest tier, which sets the bolt cap (Q-26). |
| `G` | `Casings.ParticleBeamGuidancePipeCasing` |
| `I` | `Casings.BackgroundRadiationAbsorbentCasing` |
| `J` | slCasing2:2 Harmonic Lining (round-4 ruling) |
| `K` | `Casings.HawkingRadiationRealignmentFocus` |
| `e` | Energy or ExoticEnergy, hint 1, else `L`; `checkExoticAndNormalEnergyHatches` |
| `m` | Maintenance, hint 2, else `L` |
| `b` | `KerrHatchElement.BoltBus`, a plain input bus (not ME), hint 3, else `A`. At least 1. |
| `u` | `KerrHatchElement.Utility` (`MTEBlackHoleUtility`), hint 6, else `A`. Optional; redstone on while the hole is open (Q-27). |
| `1` / `2` | `kerr_slots` only: `ofHint(7)` / `ofHint(8)`, the module controller markers (no block, no tile) |
| `c` | Compressor module piece (its own machine): GT `InputBus`, `InputHatch`, `OutputBus`, `OutputHatch`, `Energy.or(ExoticEnergy)`, hint 1, else `I`. At least 1 in, 1 out, 1 energy. |
| `x` | Extractor module piece: GT `InputBus`, `OutputBus`, `Energy.or(ExoticEnergy)` (item buses only), hint 1, else slCasing:15. At least 1 in, 1 out, 1 energy. |
| `~` | In `kerr_compressor` / `kerr_extractor`: the module's own controller (28117 / 28118). The old `q` / `k` core cells are `c` / `x` sockets now (B-51). |

### Behaviour

- **Required core:** `kerr_t1`, or `kerr_t2` once the Density Upgrade is installed (a plain right-click with the
  upgrade, Q-24).
- **Modules:** each has its own error list and a fault string. The fault is reported only when the module's core
  block stands; the cell is found through `manifest.json`'s socket `rel`. A failed module walk clears that module's
  private lists.
- **Modes (pre-redesign; gone on `kerr-redesign`):** COMPRESS (compressor module), EXTRACT (extractor module) and
  DENSIFY (compressor module + T2). DENSIFY fails the recipe check below 128 SM. With no module, `checkProcessing`
  fails with "No Kerr module". Since the redesign the compressor module searches all three of its maps together and
  every module recipe needs an open hole (B-46).
  - `bindModuleIO` puts only the active mode's module into GT's lists (Q-23).
  - COMPRESS searches `compressorRecipes`, plus `neutroniumCompressorRecipes` while the hole is open. A recipe with
    `COMPRESSION_TIER` ≥ 1 needs the hole open.
  - EXTRACT searches GT's `extractorRecipes` only (user ruling, Q-9/Q-22).
  - DENSIFY searches `KerrRecipeMaps.DENSITY` (SL-K05).
- **T2 without the reinforcement:** once the Density Upgrade is installed, only `kerr_t2` forms. With the
  reinforcement torn down the Kerr stays unformed and does not fall back to T1 (ruled: keep).
- **Hole:** §3.1's numbers, with speed clamped to ×1..×4 (Q-25).
  - Setpoints are 2..64 SM, plus 128/256/512 at T2. The screwdriver steps the setpoint; sneak + screwdriver cycles
    the mode.
  - State is persisted by `writeKerrState` from both `saveNBTData` and `setItemNBT`: `btsKerrDensity`,
    `btsKerrHoleOpen`, `btsKerrMass` and `btsKerrSetpoint`.
- **Hologram:** stack 1 = T1, 2+ = T2; both modules are always projected. Survival auto-place builds T2 when the
  upgrade is installed.
- **Status:** `getInfoData` (scanner) shows the mode, the hole (mass, setpoint, bleed, bolts against cap), parallel,
  speed, rails, bolt buses and each module's state.

### Adding a Kerr module

The Kerr is a module host: compressor and extractor now, more later. Since the redesign a module is a separate
machine standing in one arc; the KEE does not change when one is added:

1. **Shape.** In `tools/export-structures.js`, splice a `tag("kerr_<name>")` anchor before the new arc's `arc(...)`
   call, add its socket cells to `KERR_SOCKETS`, and add a `KERR_MODULES` entry (piece, slot key, MTE id, marker
   char, the controller's outward face, how to find the controller cell). Add a legend for the piece. Re-export
   (`node tools/export-structures.js <design repo>/designs/voxel.js`): the piece comes out in its own frame and the
   slot table gains its `rel` / `keeRel` / `facingAbc`. The disjointness, open-air and front-face checks cover it.
2. **Slot.** One `KerrSlot` entry (key, label, piece, marker hint dots) with its cases in `controllerStack`,
   `accepts` and the legacy-cell helpers.
3. **Machine.** A subclass of `MTEKerrModuleBase` with an MTE id from 28119 up: its structure definition (sockets as
   GT-standard hatch adders, `buildAndChain(casing)`), `getAvailableRecipeMaps` (all searched together), an optional
   `gateRecipe`, its casing texture, overlays and tooltip. Register it in `BTSMachines`.
4. **Recipe map.** GT's own map when one fits; a `KerrRecipeMaps` map only for something no GT machine makes.
5. **Controller recipe, textures** (`tools/gen-sl-textures.js`) **and lang.**

Linking, pausing, the hole's bonus, the GUI, the KEE's module list and `/btskerr`'s build all loop over `KerrSlot`.

### Recipes (`BTSRecipesKerr`)

- **SL-K01..K05** are as in §2.3, except that SL-K01's 32 Containment Deck Plating become 32 Extreme Density
  Space-Bending Casing (FC-1).
- **Dense plates** use `BTSRecipesSingularity.dense()`.
- **SL-K03's Huge Neutronium Fluid Pipe** falls back to the Large pipe, then to 6 plates per pipe.
- **SL-K06 is retired.** It was a Kerr-only "Ergosphere Extraction" map melting exotic metals at 144 L per ingot. The
  user ruled (Q-9/Q-22) that the extractor runs GT's item extractor recipes only; the map, its lang key and its NEI
  page are gone, and the Kerr shows up on GT's own Extractor page instead.
- **The BHC controller recipe** is removed by the Foundation's `BTSRecipeRemovals` under
  `removeBlackHoleCompressorRecipe`. Placed BHCs are untouched. RC-2 port: every recipe that consumes a BHC
  controller takes the Kerr controller first (`BTSBlackHoleSwap`), and SL-K01 is priced at BHC-controller parity.
- **Utility hatches** follow the RC-2 BHC protocol (`pulseUtilityHatches`): `cycleStart` / `cycleMiddle` while the
  hole is open, `blackHoleClosed` while it is shut.

---

## Contract additions (Eye)

Written by the Eye unit on 2026-10-12. This records what P3 built. Where it disagrees with §2.4, §3.3 or §4, this
section wins, and each deviation is a question (Q-29..Q-35). The exporter needed no new anchors for the Eye, so the
committed shapes are used unchanged (`--check` reports no drift).

### Files

| File | What it is |
|---|---|
| `machines/eye/MTEEyeOfHarmonyBTS.java` | The controller, MTE 28115 (`multimachine.beyondthestars.eyeofharmony`, "Eye of Harmony"). |
| `machines/eye/EyeHatchElement.java` | Sockets: `Gas` and `Stellar` (private input lists) and `Output` (ME bus/hatch into GT's output lists). |
| `machines/eye/EyeRecipe.java` | One long-sized process recipe (star or planet). |
| `machines/eye/EyeRecipeRegistry.java` | Star recipes, the TecTech snapshot and conversion, the TecTech EOH removal, the NEI page. |
| `machines/eye/EyeRecipeMaps.java` | `EYE` (`beyondthestars.recipe.eye`), the display-only NEI page, laid out like TecTech's. |
| `machines/eye/EyeKit.java` | `/btskit eye` and the creative build used by `/btseye`. |
| `BTSRecipesEye.java` | SL-E01 and SL-E02 (`line("eye")`). |
| `CommandBTSEye.java` | `/btseye [t1\|t2\|t3\|t4] [gen 1-9] [fill] [force]`. |

Shared edits, each append-only:

- `BTSMachines`: the `eyeOfHarmony` field and its base+15 line.
- `BTSRecipes`: `line("eye")`.
- `CommonProxy`: `EyeRecipeRegistry.onLoadComplete()` above `BTSRecipeRemovals.run()`, and `CommandBTSEye`.
- `CommandBTSKit`: an `eye` branch.
- `en_US.lang`: the Eye section.
- `docs/migration.md`: the old-save notes.

### Shape char → element (Eye)

| Char | Element |
|---|---|
| `U` | `Casings.ReinforcedSpatialStructureCasing` |
| `Q` | `Casings.ReinforcedTemporalStructureCasing` |
| `J` | `Casings.InfiniteSpacetimeEnergyBoundaryCasing` |
| `Z` | `Casings.QuantumGlass` |
| `W` / `X` / `Y` | `TTCasingsContainer.SpacetimeCompressionFieldGenerators` / `TimeAccelerationFieldGenerator` / `StabilisationFieldGenerators`, metas 0-8 in any mix; the lowest of each type counts (Q-29) |
| `R` / `S` / `T` | slCasing 10 / 11 / 12 (Beam Band, Jet Crescent, Galactic Shell) |
| `h` | `EyeHatchElement.Gas` (any `MTEHatchInput`), hint 1, else `Q`. At least 1. |
| `o` | `EyeHatchElement.Output` (`MTEHatchOutputBusME`, `MTEHatchOutputME`), hint 2, else `Q`. At least 1 of each. |
| `m` | Maintenance, hint 3, else `Q` |
| `s` / `t` | `EyeHatchElement.Stellar`, hint 4, else `R` / `S`. Optional. |

Every plain element is wrapped in `noOverwrite`.

### Behaviour

- **Pieces:** `eye_core` is required. `eye_beam`, `eye_jet` and `eye_arms` are optional, each with its own error
  list. A band whose walk fails drops the stellar sockets it registered (mark-and-truncate).
- **Tiers:**
  - T1 needs the core.
  - T2 needs the beam band.
  - T3 needs the jet crescent (independent of the beam band).
  - T4 needs the jet crescent, the arms and an installed Galactic Upgrade Core (plain right-click, Q-24).
  - The installed tier is the highest available one.
- **Target:** the controller slot holds a star chart or a TecTech dimension display block. The registry looks it up
  by item, and it is never consumed.
- **Buffers:** one `long` per fluid, capped at 2⁶² L. Every live socket is drained every 20 ticks (Q-30).
- **A run** (Q-31), in order:
  1. Parallel is 2^(installed − recipe tier), limited by the buffers.
  2. The EU is debited from the owner's wireless network in one lump at the start.
  3. Inputs are taken.
  4. Success is rolled per parallel.
  5. Planets get the field-generator time, success and yield (Q-29).
  6. Outputs go to the ME bus/hatch when the run completes.
- **Planet eligibility** (round-4 ruling): the 1:1 compression gate alone decides. An Eye at tier N runs every
  planet mapped to tiers 1..N; TecTech's per-planet compression minimum is not enforced. Compression above a planet's
  TecTech tier still cuts 3 % of its time per tier; below it there is no penalty.
- **Persistence:**
  - `writeEyeState` writes `btsEyeGalactic` and `btsEyeBuffers` from both `saveNBTData` and `setItemNBT`.
  - The run in flight (`btsEyeRunKey`/`Parallel`/`Successes`/`Yield`) is tile NBT only.
- **Hologram:** a stack of 1..4 projects T1..T4, with the bands cumulative. Compression generators are placed at
  the stack's 1:1 gate, Time Dilation and Stabilisation at tier 1; see "Hologram & NEI".
- **Status:** `getInfoData` (scanner) shows the tier, the bands, the field tiers, the sockets, the buffers, the
  target (parallel, success, seconds) and the last run.

### Recipes

- **Star recipes** are §3.3's table, with the PROPOSED amounts:
  - Sol, Sirius A, Arcturus and Sirius B;
  - Sirius B, Cooled, which registers only under `interimBlackDwarfAtEyeT4`.
- **Planets** are converted from TecTech at load-complete:
  - Each keeps TecTech's H/He, ores, plasmas, base success and compression tier (now only the baseline of the
    -3 %-per-tier time reward, not a requirement).
  - Raw Star Matter, WDM, BDM and Universium are stripped.
  - Rocket tiers map R0-3 → T1, R4-6 → T2 and R7-8 → T3. The Deep Dark (or anything R9+) → T4.
- **EU per run** is `RECIPE_UMV × 4^(tier−1) × base ticks`.
- **SL-E01 / SL-E02:** see Q-32.

### Removal (`replaceTecTechEyeOfHarmony`)

`EyeRecipeRegistry.onLoadComplete()` runs in this order:

1. stars;
2. the TecTech snapshot (always);
3. if the flag is on **and** at least one planet converted (Q-33):
   - `recipeHashMap.clear()` on `TecTech.eyeOfHarmonyRecipeStorage`;
   - every recipe removed from `TecTechRecipeMaps.eyeOfHarmonyRecipes`;
   - `BTSRecipeRemovals.removeAssemblyLineByOutput` for `CustomItemList.Machine_Multi_EyeOfHarmony`;
4. the NEI page.

Each step runs inside `BTSRecipeRemovals.guard`. The old-save behaviour is in `docs/migration.md`.

## Contract additions (Eye, D-49)

Units 1-6 of [`d49-build-plan.md`](d49-build-plan.md). Where this disagrees with "Contract additions (Eye)" above,
this section wins.

### Files

| File | What it is |
|---|---|
| `machines/eye/field/*` | The pure field engine (`StellarField.step`, `TierParams`, `TrimDef`, `EventDef`, `FieldState`, `FieldRng`, `CollapseOverlay`, `FieldReadout`, `FieldRig`, `FieldSim`). No Minecraft types; bit-identical to the prototype (`tools/fixtures`, the golden file). |
| `machines/eye/star/EyeStarRuntime.java` | Per controller: the star's state machine, cycles, ignition, dial, trims, feed, output, milestones, escrow, transitions, publishing. Read-only accessors for the GUI and the render. |
| `machines/eye/star/EyeTransition.java` + five subclasses | The transitions as field overlays. |
| `machines/eye/star/StarDef.java`, `EyeMilestones.java`, `EyeNbt.java`, `EyeStarBench.java` | The star table, the milestones, the one NBT writer and reader with the migration, the bench. |
| `machines/eye/MTEHatchStellarFieldSensor.java` + `StellarFieldSensorGui.java` | MTE 28116 and its GUI. |
| `machines/eye/EyeGui.java` | The controller's MUI2 GUI (`getGui`). |
| `machines/eye/render/EyeStarLook.java` | What the centre shows (kind, radius, colour, gamma, spin, wobble), from the runtime. Pure data. |
| `machines/eye/render/EyeCentreRender.java` | The server half of the render: places, updates and removes the render block. |
| `machines/eye/render/BlockEyeStar.java` + `TileEyeStar.java` | GT's `BlockRenderer` (as the BHC) and the synced, self-policing render tile. |
| `machines/eye/render/EyeStarRenderer.java` | The client TESR (TecTech's star shader, textures, sphere and cull helpers). Registered in `ClientProxy`, with GT's resource-reload hook. |

### Centre render

- **Cell:** the voxel origin of `designs/voxel.js` (where the mockup draws its star). From the controller it is
  `(a, b, c) = (-x, y, z)` of `manifest.json` `machines.eye.controllerVoxel`, i.e. `(0, -40, 40)`, through
  `ExtendedFacing.getWorldOffset`. Every piece leaves it empty; the nearest structure block is 20.9 blocks away.
- **Shown** while formed, the star exists (IGNITING, LIT, PENDING, BATCH; not DARK or IDLE), the per-Eye switch is
  on and `eyeRenderCentre` is on (the client checks its own copy before drawing). A supernova keeps the block 60
  ticks for its flash.
- **Placed** only into air, never over another block (the scanner and GUI then say "clear that block"); it never
  loads a chunk. **Removed** at once on unform, star out, IDLE, switch off, a turned controller, break and wrench
  (`onBlockDestroyed`, `onRemoval`). The tile checks its owner every 100 ticks and turns to air if the owner's cell is
  not loaded or no Eye there claims it, so a block can never be orphaned.
- **NBT:** the per-Eye switch (`btsEyeRender`) through `writeEyeState` (item and tile); the block position
  (`btsEyeRenderPos`) tile only. The look is synced by the tile's description packet, only when it moves (2 % radius,
  any colour, 0.05 gamma or spin, 0.02 wobble).

---

## Hologram & NEI

Acceptance pass, 2026-10-12. Verified by reading our code and `javap` of the instance jars (structurelib-1.4.38,
blockrenderer6343-1.4.14, gregtech-5.09.52.594). Nothing here has been opened in game yet. RC-2 port: the public API
of every StructureLib class used is identical in 1.4.45, and BlockRenderer6343 1.4.24 still discovers controllers
through `IConstructable` / `ISurvivalConstructable`.

### Trigger stack → tier

| Controller | Stack 1 | Stack 2 | Stack 3 | Stack 4+ |
|---|---|---|---|---|
| Kerr (KEE) | `kerr_t1` + slot markers 7 / 8 | `kerr_t2` + slot markers | as 2 | as 2 |
| Kerr module (28117 / 28118) | its arc | as 1 | as 1 | as 1 |
| Eye | `eye_core` | + `eye_beam` | + `eye_jet` | + `eye_arms` |

Both tooltips state this. `construct` reads the stack only (trap 1), so the hologram never depends on server state.
Survival auto-place on the Kerr also builds T2 once the Density Upgrade is installed, whatever the stack.

### Creative build and hints (`construct`)

- Every piece for the stack's tier is built or hinted, the optional modules and bands included. Each piece uses its own
  `off` from the manifest.
- Plain casings are wrapped in `BTSStructures.noOverwrite`: a cell holding any tile entity (a hatch, a chest, a
  machine) is never written.
- Tiered parts go through `BTSStructures.tieredBlock`. The walk accepts any tier (and records the lowest); builds place
  one tier and never replace a part that is already at that tier or higher:
  - Kerr injector rails: Mk I.
  - Eye Spacetime Compression Field Generators: the stack's 1:1 gate (`COMPRESSION_META_FOR_TIER`), so a stack-4
    build is T4-capable.
  - Eye Time Dilation and Stabilisation generators: tier 1 (Crude); they have no effect yet.
  - GT's EOH structure channels raise these when the projector sets them: `spacetime_compression` (never below the
    gate), `time_dilation`, `stabilisation`. Channel value n = generator tier n.

### Survival auto-place (`survivalConstruct`)

- It works through StructureLib's `ISurvivalBuildEnvironment`, so it takes blocks from the player's inventory or a
  linked AE system like any GT multi. Hatches are placed only when the projector's `gt_hatch` channel is set (GT's
  rule); otherwise a socket gets its fallback casing.
- **Order.** Kerr: the core, then each module on its own; an unfinished module does not block the other. Eye: core,
  beam band, jet crescent, arms; a piece starts only when every piece before it is complete. It returns -1 once
  everything the stack asks for stands.
- **Budget.** StructureLib's default round is 25 elements every 300 ms. `BTSStructures.survivalBudget` raises a small
  budget eightfold, capped at 200, the same kind of rule TecTech's Eye uses (×5, cap 200). A T4 Eye or a Kerr T2 then
  takes about a minute of holding the projector. NEI's preview passes no limit.
- **Tiered parts.** A cell already holding the wanted tier or higher is skipped. An empty cell gets the wanted tier, or
  the next higher tier the player has. A cell holding a lower tier is left alone, because auto-place never breaks
  blocks; that piece then stays incomplete until the player swaps the part. On the Eye this is the 1:1 gate: going
  from T1 to T2 means swapping the core's 188 compression generators by hand before the beam band is placed.
- Tiles are never overwritten: `noOverwrite` returns SKIP for any cell with a tile entity.

### NEI multiblock preview (BlockRenderer6343)

How it finds and draws a controller (bytecode of `GTNEIMultiblockHandler`, `GTConstructableScan`, `ObserverWorld`,
`GTGuiMultiblockHandler`, `StructureHacks`):

- **Discovery.** Every entry of `GregTechAPI.METATILEENTITIES` that implements `IConstructable` is listed. Both
  controllers qualify (`ISurvivalConstructable` extends it). No registration is needed.
- **Tier count.** A background thread reads `getStructureDefinition()` and walks every element. If any element is a
  known tiered type (StructureLib's `ofBlocksTiered`, a `withChannel` wrapper, GT's coils), the tier slider and channel
  sliders come from those elements. Ours have none, so for a definition with more than one shape it counts tiers
  instead: it calls `construct(stack, false)` with stack 1, 2, 3, ... in a scratch world until a stack changes
  nothing. That gives **Kerr: 2 tiers** and **Eye: 4 tiers** (stack 5 adds no block and no new generator tier).
- **Drawing.** The controller is placed at (0, 64, 0) facing south, the `gt_hatch` channel is set, and
  `survivalConstruct(stack, Integer.MAX_VALUE, creative source)` is called until a round changes nothing (at most
  2,000 rounds). So the preview shows exactly what survival auto-place builds, hatches included. Hint-dot labels come
  from `getStructureDescription`, i.e. the tooltip's numbered lines.
- **Size.** There is no block-count or footprint cap. The only hard limit is the scratch world's height, y 0-255.
  Ours span y 32-96 (Kerr) and y 63-119 (Eye bands included), so they fit. The camera zooms out with the
  structure's size. The renderer re-tessellates every block every frame (no display list), so 30-35k blocks may run
  at a low frame rate; the layer slider shows one y-layer at a time. See question B-19.
- **NEI index.** `noOverwrite` and `tieredBlock` forward `getBlocksToPlace`, so NEI's "used in multiblock" lookup
  finds our casings and generators.
- **Thread safety.** The structure definition is built once behind a lock, because the preview reads it from its
  scan thread.
- **Not exposed: channel sliders.** If the field generators were channel elements, the preview's main tier slider
  would read 1-9 (the generators' tier count) instead of 1-4, and the Kerr's would read 1-3 if the rails had one. The
  tier count is the more important thing to get right, so the channels work on the projector only. See question B-19.
