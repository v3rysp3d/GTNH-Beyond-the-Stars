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
old Q-1..Q-38 numbers, and its last table maps them. This document is what gets built; the questions record what I
still have to rule on.

Code references below use `<pkg>` = `com.nateyburr.beyondthestars`.

---

## GTNH 2.9.0-RC-2 port (2026-10-12)

My ruling: the addon targets **GTNH 2.9.0-RC-2** (DreamAssemblerXXL manifest 2026-10-04). This supersedes the D-06
beta-1 pin. The port plan with the full API, behaviour and id diff is my local `port-plan.md`; this section is
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
- **BHC to Kerr in recipes** (my ruling). In RC-2 the BHC controller is an ingredient: all 9 Spacetime Compression
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
  **SL-G01** (B-29 settled 2026-10-13, "Rewrite GT's in place"): GT's own Condensate Assembler recipe for that chamber
  is rewritten at load-complete to take 2 SpaceTime foils in place of the WDM foil (only with
  `replaceTecTechEyeOfHarmony=true`), so NEI shows one recipe; **SL-P39** (the separate recipe it replaces) is retired
  and registers only if no GT chamber recipe is found to rewrite. **SL-G02 / SL-G03** (B-135, Q4 / Q21) do the same
  for Resonance Chamber III (2 White Dwarf Matter foils for the Black Dwarf Matter foil; retired SL-P33) and IV (2
  SpaceTime foils for the MHDCSM foil plus 1,500 L Entangled Ergomatter; retired SL-P40). **SL-P30..P32 are retired**: RC-2 prices the
  generators itself and the ruling keeps those costs. The tier upgrade path is back on RC-2's terms (B-30 settled
  2026-10-13, "Add an upgrade path"; B-83): **SL-U00..U48** in `BTSRecipesGeneratorUpgrade` turn a tier t-1
  compression / Time Dilation / Stabilisation generator into tier t in the Condensate Assembler, keeping its Kerr
  controller inside. SL-P29 stays (RC-2's Boundary casing costs a whole LSC). SL-P27/P28 are
  retired by ruling 11a: RC-2's own Condensate Assembler recipes for the Spatial / Temporal casings output x32 instead
  of x4, rewritten at load-complete (SL-B01/B02, `BTSRecipesBEC`, whatever the flags say). It is their only recipe in
  RC-2, so if no BEC recipe makes a casing, SL-B03/B04 register RC-2's own recipe at x32, and if that cannot be built
  the old SL-P27/P28 Assembler bodies return as a last resort (B-60).
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

### Automation pass (ruling 18, 2026-10-13)

Every action on the Eye and the Kerr now has an AE2, redstone, OpenComputers and Steve's Factory Manager route
([`automation.md`](automation.md); question B-70):

- **Directive Hatch, MTE 28127** (`machines/directive`, recipe SL-A01 at UIV): a Degasser-style 4-bit redstone code per
  set (Eye Trims / Status / Transition, Kerr Hole / Status), or a control input that sets the Kerr mass setpoint, the
  Eye time dial or the HSM feed. It fits the Eye's Sensor sockets and the KEE's utility sockets (no structure change).
- **Eye plinth-bus pickups:** the next transition core goes into escrow, and a star chart goes into the controller slot.
- **OpenComputers** (soft dependency): Adapter components `bts_eye`, `bts_kerr`, `bts_kerr_module`, `bts_sensor`,
  `bts_directive` over `integration/BTSComputerApi`.
- **Proof:** `DirectiveRigSim` (offline) and the dev-server bench (`autorig`, `ocloop`, `ctrl`, `ocprobe`): Eye T1 to T4
  climbed and held on redstone bits alone; the Kerr was fed and opened from empty buses.

### BEC expansion (ruling 11b, 2026-10-13)

Much more of the ladder now runs through RC-2's Bose-Einstein Condensate system (designs/docs/bec-expansion-proposal.md,
all 14 open questions as recommended; §2.5 below; costs doc §11; question B-64):

- **Six addon condensates** (`fluids/SLCondensates`): Entangled Raw Star Matter, Excited Stellar Plasma, Hyperdense
  Stellar Mixture, White Dwarf Matter, Black Dwarf Matter, Antipolarization Agent. Each is one fluid plus one Condensate
  Generator recipe (SL-B10..B15); the BEC network is fluid-generic, so they store, travel and feed nodes like GT's own.
- **20 Condensate Assembler recipes** (SL-B20..B81, `BTSRecipesBEC`): the Stabiliser Pylon, all six star charts, the
  Stellar Field Sensor Hatch, all five transition cores (Stellar Mass, Shell-Burning, Galactic Upgrade, Subspace
  Cooling, Magnetar Seed), the Kerr Density Upgrade, Bastion Plate x64, Casimir Plate x16, Beam Band Casing x32, Jet
  Crescent Casing x32, plus batch alternatives Harmonic Lining x64 and Galactic Shell Plating x64. Each core and chart
  keeps its gate fluid (now as condensate).
- **Retired** from their old files: SL-P15, P16, P18, P19, P21-P25, P35, P37, SL-E02..E07, SL-K04 (SL-P27/P28 were
  already retired by SL-B01/B02). Each lives on in `BTSRecipesBEC` only as the **fallback** that registers if its BEC
  replacement is skipped, so no part can become uncraftable. The cores lose their research steps (BEC recipes have none).
- Kerr T1 is untouched (pre-BEC); controllers stay assembly lines; SL-P12 and SL-P17 stay next to their alternatives.
- Tooltips on the cores, charts, Pylon and Density Upgrade say "Crafted in the Condensate Assembler" and with which
  condensate.


### D-49: the Eye's star lifecycle (units 1-6, 2026-10-13)

This block supersedes the Eye rows below where they differ. Plan: [`d49-build-plan.md`](d49-build-plan.md); judgement
calls: questions B-34..B-38, B-45, B-62 (review fixes) and B-63 (unit 5).

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
  the neutron star, the white dwarf cooling to black over its batch, the magnetar (a steady body since B-219; its flare warnings show in the magnetosphere),
  and the Core Collapse (Arcturus swelling, then the bounce's shock shell, or a supernova flash). It uses TecTech's
  star shader and textures (the Forge of the Gods' `tectech:star` and `StarLayer0..2`). Config `eyeRenderCentre`
  and a per-Eye switch in the GUI; the render block is placed only into air, never into a block, and removes
  itself when its Eye is gone.
- **NEI:** the star pages (per cycle, up to 4 fluids), the transition pages and the stability-field pages (display
  only).
- **Bench (op, console):** `/btseye t1..t6 <x> <y> <z> [facing] [gen n] [fill]` builds an Eye;
  `/btseye star <x> <y> <z> ...` drives the star (status, target, lit, sim, rig, unlock, milestones, core, escrow,
  sensor, probe, wrench, legacy, field, **render [on|off]**: the centre cell and its look, **gui [info]**: the controller GUI's header and rows as text, B-96).
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
  creative energy hatch again (B-57). `/btskerr gui x y z [info]` and `/btskerr module x y z <c|x> gui [info]` print
  the header and the rows (or the Info page) the KEE's and a module's GUI would show, as text (B-97).
  `/btskerr gui x y z hatches` and `/btseye star x y z hatchgui` print what each of the machine's Directive (and
  Stellar Field Sensor) Hatch GUIs shows: the readout box, then the control labels (B-99).
- **GUI screenshot harness** (dev client only, B-100): `JAVA_TOOL_OPTIONS="-Dbts.guishots=true" ./gradlew.bat
  runClient` makes the client enter (or create) the creative superflat world `saves/bts-guishots` and then run the
  lines appended to `run/client/guishots/queue.txt` (the file is emptied at start): `cmd <command>` (as the player),
  `tp x y z [yaw] [pitch]`, `wait <ticks>`, `gui x y z` (waits until the client holds the tile, then the server opens the GT
  GUI), `scale <n>`, `mouse x y` / `mouse off` (a fake mouse in GUI units from the main panel's top left, set on
  LWJGL's position every frame, so hover tooltips show and the real cursor never moves), `click x y [button]` (MUI2's
  hovered widget gets the press), `scroll x y <n>`, `shot <name>` (`run/client/screenshots/guishots/<name>.png`, and
  the open panels' areas in the log for cropping), `shotw <name>` (the world, no screen), `nei usage|recipe <item> [tab <text>] [page <n>]` (items as `ItemList.X`,
  `sl.<field>`, `cc.<CircuitComponent>` or `mod:name[:meta]`; B-136), `dump [syncKey]` (a sync value and the widget tree), `find <MTE
  class> x y z r`, `probe`, `close`, `say <text>`, `quit`. Log: `run/client/guishots/guishots.log`, with the chat the
  bench commands print. `tools/guishots/drive.sh -f <file>` feeds a file and waits for it; `build-sites.shots` builds
  every machine, then (after a re-join: the client only learns a structure built under it on re-entry) the other
  `.shots` files photograph each GUI state. Window 1920x1080 (`-Dbts.guishots.width/height`); the player is held
  flying where `tp` put it, so it stays in GUI range. Inert without the property.
- **Overnight test bench** (B-71): `/btseye bec` lists our Condensate Assembler and Generator recipes as the server holds them (and whether GT's lookup finds each); `/btseye star x y z compression <1-9> [all]` swaps a Spacetime Compression Field Generator to test the 1:1 gate, and `/btseye star x y z check` prints the reason GT's run check would show.
- **Circuit chain bench** (B-136): `/btsrecipes chain build x y z` lays out stock GT machines (an EBF with Hypogen
  coils, a Hyper-Intensity Laser Engraver, the UMV Cutting Machine, Assembling Machine and Circuit Assembler, a
  16-slice Assembly Line, a chest), `plan|make <sidereal|empyrean|omega> x y z` runs SL-X01..X32 one recipe at a time
  through them (our items only from machine outputs, the rest supplied and counted; finished Mainframes go in the
  chest), `gt <umv|uxv|max> x y z` runs a stock Assembler recipe with our Mainframe in its any-of-tier slot, `status`,
  `stop`. Single blocks need `/gt cleanroom_bypass on cleanroom`; the bench keeps the rig charged and the weather clear.
  Use `/btswarp 20` for speed. `/btsrecipes nac build|run` (B-133) is the NAC's bench.
- **Recipe ledger** (B-91): `/btsrecipes dump [file]` writes JSON of every recipe the mod adds (`new`), rewrites in place (`changed-GT`, with the pack's recipe before) or removes (`removed`, with its replacement), read from the live recipe maps after load; `/btsrecipes status` prints the counts by line and badge.
- **More console tools** (test bench): `/btseye gates` dumps the compression generator recipes the server actually
  has and checks the gate rule against them, and counts BHC consumers left. `/btsbuild t<1-5> <x> <y> <z> [facing]
  [rod] [force] [crib] | [check]` builds a reactor plant from the console; `crib` checks that a feedwater socket
  refuses a Crafting Input Buffer. Questions B-32.
- **Older standalone multis** (B-88): `/btsbuild hall <yellowcake|platform|cascade|fuelfab|purex|batching> x y z
  [facing] build|check|status|stock|tier|circuit|place` builds one through the projector's survival path with test
  hatches fitted, stocks a recipe, puts a circuit in the controller slot and prints the localized name.
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
- **Cheaper bulk blocks:** RC-2's Condensate Assembler makes TecTech's Spatial and Temporal casings x32 (SL-B01/B02) and
  SL-P29 batch-makes the Boundary casing (always registered, B-26);
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

Star picks follow proposals.md (Sirius A for "a larger star", Arcturus for "a red giant"). Sirius B is our choice for the
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
| SL-P15 | *BEC expansion: now **SL-B40** (§2.5); this row is only its fallback.* Beam Band Casing ×16 (was ×4, cost audit #8) | ASM | EOH_Reinforced_Spatial_Casing ×1, Beam Focusing Quadrupole ×1, Emitter_UMV ×2, plateDense SpaceTime ×4 | SpaceTime molten 9,216 | RECIPE_UMV | 1,800 |
| SL-P16 | *BEC expansion: now **SL-B51** (§2.5); this row is only its fallback.* Jet Crescent Casing ×16 (was ×4, cost audit #9) | ASM | EOH_Reinforced_Temporal_Casing ×1, Ergosphere Field Coil ×2, plateDense Infinity ×8, Emitter_UMV ×2, ingot SpaceTime ×32 | RawStarMatter 64,000 (4,000 per block, as before) | RECIPE_UMV | 2,400 |
| SL-P17 | Galactic Shell Plating ×16 (was ×2, cost audit #10) | ASM | plateDense Infinity ×8, frameGt TranscendentMetal ×2, plate CosmicNeutronium ×8, ingot TranscendentMetal ×16 | **HSM 250** (was 1,000 per 2) | RECIPE_UMV | 800 |
| SL-P18 | *BEC expansion: now **SL-B32** (§2.5); this row is only its fallback.* Bastion Plate ×4 | ASM | plateDense Neutronium ×6, plate TranscendentMetal ×2, frameGt Infinity ×1 | Infinity molten 2,304 | RECIPE_UMV | 1,200 |
| SL-P19 | *BEC expansion: now **SL-B33** (§2.5); this row is only its fallback.* Casimir Plate ×4 | ASM | plate Neutronium ×8, foil SpaceTime ×16 | SpaceTime molten 576 | RECIPE_UMV | 1,200 |
| SL-P21 | *BEC expansion: now **SL-B20** (§2.5); this row is only its fallback.* Stabiliser Pylon (item) | AL (research: Star Chart: Sol, Q-36) | Eye Shell Casing ×32, Harmonic Lining ×16, Field_Generator_UMV ×8, frameGt Neutronium ×16, circuit UMV ×8, plateDense Neutronium ×16, Containment Deck Plating ×16 | SpaceTime molten 36,864 (was 147,456, cost audit #12), Super Coolant 500,000 | RECIPE_UMV ×4 | 18,000 |
| SL-P22 | *BEC expansion: now **SL-B21** (§2.5); this row is only its fallback.* Star Chart: Sol | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate Infinity ×4 | SpaceTime molten 144 | RECIPE_UMV | 200 |
| SL-P23 | *BEC expansion: now **SL-B41** (§2.5); this row is only its fallback.* Star Chart: Sirius A | ASM | Tool_DataOrb ×1, circuit UMV ×1, plateDense SpaceTime ×1 | RSPM 10,000 | RECIPE_UMV | 200 |
| SL-P24 | *BEC expansion: now **SL-B52** (§2.5); this row is only its fallback.* Star Chart: Arcturus | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate TranscendentMetal ×4 | ESPM 1,000 | RECIPE_UMV | 200 |
| SL-P25 | *BEC expansion: now **SL-B61** (§2.5); this row is only its fallback.* Star Chart: Sirius B | ASM | Tool_DataOrb ×1, circuit UMV ×1, Galactic Shell Plating ×1 | HSM 1,000 | RECIPE_UMV | 200 |
| SL-P26 | Star Chart: Sirius B, Cooled (interim) | ASM | Star Chart: Sirius B ×1, circuit UMV ×1 | Super Coolant 64,000 | RECIPE_UMV | 200 |
| SL-P27 | *retired by ruling 11a* (was Reinforced Spatial Structure Casing ×4 in the Assembler): RC-2's Condensate Assembler recipe outputs ×32 instead (SL-B01) | | | | | |
| SL-P28 | *retired by ruling 11a* (was Reinforced Temporal Structure Casing ×4): RC-2's Condensate Assembler recipe outputs ×32 instead (SL-B02) | | | | | |
| SL-P29 | TecTech Infinite SpaceTime Energy Boundary Casing ×8 (gated) | ASM | wireGt16 SuperconductorUMV ×1, plateDense Neutronium ×8, plateDense CosmicNeutronium ×8, Emitter_UIV ×1 | SpaceTime molten 1,152 | RECIPE_UMV | 1,200 |
| SL-P30..P32 | *retired in the RC-2 port* (were research-station "upgrade the compression generator" recipes, B-28) | | | | | |
| SL-P33..P38 | *reserved* by the D-49 build plan (P33/P34: conditional Eye T5/T6 gate recipes, whose §14 post-port audit is answered by costs doc §9.4; P35..P38: Star Chart: Sirius B, its reclaim, Star Chart: Magnetar, Stellar Ignition Charge). *SL-P33 (the Resonance Chamber III with 2 WDM foils) retired 2026-10-14 (B-135): now only the fallback of SL-G02, GT's own recipe rewritten in place with the same inputs* | | | | | |
| SL-P39 | *retired 2026-10-13 (B-29 settled): now only the fallback of SL-G01, GT's own recipe rewritten in place with the same inputs* MetaMaterial Resonance Chamber II (gated; RC-2 gate audit, B-29) | Condensate Assembler (`TecTechRecipeMaps.condensateAssemblingRecipes`), nanite tiers SixPhasedCopper / Silver / Gold / Silver | Emitter_UEV ×2, rod Hypogen ×2, **foil SpaceTime ×2** (GT's recipe: foil WhiteDwarfMatter ×1), plate ProtoHalkonite ×1 | entangled Chromatic Glass ×12, Infinity ×6, Boundless Cosmic Solder ×2 (condensate units, as GT's) | RECIPE_UIV | 6,000 |

SL-P20 is reserved and unused. Keep the ID gap; do not renumber. Each star chart's fluid is the previous tier's
product, so a chart cannot be crafted before its tier is reachable. SL-P26 registers only when
`Config.interimBlackDwarfAtEyeT4` is true. SL-P29 (and the retired SL-P27/P28) come from the cost audit
([`singularity-ladder-costs.md`](singularity-ladder-costs.md) §6, #3-#4) and always register, because our Eye needs
them either way (B-26). TecTech's own recipes for those blocks (Condensate Assembler in RC-2) stay. SL-G01..G03 (the
in-place chamber rewrites that retired SL-P39, SL-P33 and SL-P40) and SL-U00..U48 (the generator upgrade path, B-83) run only when
`Config.replaceTecTechEyeOfHarmony` is true: with TecTech's Eye kept, WDM has its stock source and the 1:1 gate is not
ours.

| ID | What | Where |
|---|---|---|
| SL-G01 | GT's Resonance Chamber II: 1 White Dwarf Matter foil -> 2 SpaceTime foils, same index (nanites and condensates untouched); fallback SL-P39 | `BTSRecipesSingularity.onLoadComplete`, load-complete |
| SL-G02 | GT's Resonance Chamber III (compression tiers 4-5, Eye T5 / T6 gates): 1 Black Dwarf Matter foil -> 2 White Dwarf Matter foils, same index (Q4 / Q21 settled 2026-10-13, "rewrite in place", B-135); fallback SL-P33 | same |
| SL-G03 | GT's Resonance Chamber IV (compression tier 8, Eye T8 gate): 1 MHDCSM foil -> 2 SpaceTime foils, same index, plus 1,500 L Entangled Ergomatter as the fourth condensate (the recipe's `CONDENSATE_INPUT` metadata) (B-135); fallback SL-P40 | same |
| SL-U00 | Field Generator Retrofit Frame x2: 1 Hexanite frame, 4 SpaceTime plates, 1 UMV circuit, 144 L molten Infinity; 20 s UIV (Assembler) | `BTSRecipesGeneratorUpgrade` |
| SL-U01..U08 / SL-U11..U18 | Spacetime Compression / Time Dilation generator tier t-1 -> t: the old generator + what GT's tier-t recipe takes more of than tier t-1 (items and condensates; a changed item in full); GT's tier-t EU/t and duration | derived from GT's recipes at postInit |
| SL-U21..U28 | Stabilisation generator tier t-1 -> t: the old one + a Compression and a Time Dilation Retrofit Kit of tier t + the stabiliser's own difference | same |
| SL-U31..U38 / SL-U41..U48 | Compression / Time Dilation Retrofit Kit tier t: a Retrofit Frame + exactly SL-U0t's / SL-U1t's difference | same |
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
| SL-K04 | *BEC expansion: now **SL-B31** (§2.5); this row is only its fallback.* Kerr Density Upgrade (item) | AL (research: Bastion Plate, Q-36) | Bastion Plate ×64, Bastion Plate ×64, plateDense Infinity ×64, Field_Generator_UMV ×32, circuit UMV ×32, Ergosphere Field Coil ×32, Black_Hole_Opener ×32 (was Black_Hole_Stabilizer ×1, which nothing crafts; cost audit #1, B-21), Bolt Injector Rail Mk III ×16 | RawStarMatter 4,000,000, SpaceTime molten 589,824, ExcitedDTEC 128,000 | RECIPE_UMV ×4 | 36,000 |
| SL-K05 | HSM 1,000 L | `beyondthestars.recipe.kerrdensity` (new, section 3.1) | bolt Neutronium ×64 | RHSP 10,000, SpaceTime molten 288 | RECIPE_UMV | 400 |

The Kerr's other maps are GT's own (`compressorRecipes`, `neutroniumCompressorRecipes`, `extractorRecipes`), so it
adds no recipes there. SL-K06 (a Kerr-only "Ergosphere Extraction" map) is retired; the ID stays reserved.

### 2.4 Eye recipes: `BTSRecipesEye.java` + `eye/EyeRecipeRegistry.java` (Eye unit)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-E01 | Eye of Harmony (controller, MTE base+15) | AL (research: Stabiliser Pylon, Q-36) | EOH_Reinforced_Spatial_Casing ×64, EOH_Reinforced_Temporal_Casing ×64, SpacetimeCompressionFieldGeneratorTier0 ×1, TimeAccelerationFieldGeneratorTier0 ×1, StabilisationFieldGeneratorTier0 ×1 (Tier0 since the review fix pass, Q-32), circuit UMV ×64, Field_Generator_UMV ×32, plateDense SpaceTime ×64, Eye Shell Casing ×64, Harmonic Lining ×64, Stabiliser Pylon ×6, Containment Deck Plating ×64 | SpaceTime molten 294,912 (was 1,179,648, cost audit #12), ExcitedDTEC 256,000, Infinity molten 589,824 | RECIPE_UMV ×4 | 36,000 |
| SL-E02 | *BEC expansion: now **SL-B60** (§2.5); this row is only its fallback.* Galactic Upgrade Core | AL (research: Eye controller) | Galactic Shell Plating ×64, Field_Generator_UMV ×32, circuit UMV ×64, Casimir Plate ×32, plateDense SpaceTime ×32, Emitter_UMV ×16 | **HSM 32,000** (was 256,000, cost audit #11), SpaceTime molten 147,456, TranscendentMetal molten 147,456 | RECIPE_UMV ×4 | 48,000 |

The Eye's process recipes (SL-E1x) are **not** GT recipe-map recipes; they are section 3.3's registry, shown in NEI
through a display-only map `beyondthestars.recipe.eye` built with `.fake()` like `BTSDisplayRecipes`.

### 2.5 BEC recipes: `BTSRecipesBEC.java` (BEC expansion, ruling 11b)

Map for SL-B20..B81: `TecTechRecipeMaps.condensateAssemblingRecipes` (maxIO 16 items / 1 out / 4 condensates).
Nanite tiers are given per item slot in brackets (1 Carbon, 2 Silver, 3 Gold, 4 Transcendent, 5 Six-Phased Copper,
6 White Dwarf, 7 Black Dwarf); the map needs exactly one per item stack, and `BTSRecipesBEC.s(...)` copies a slot's tier
onto every stack it expands to (dense-plate fallbacks, stacks above 64). "ing" = ingots of condensate (144 L). Times
are seconds (BEC durations are nanite-time: they shrink with the nanites in the Assembler). EU/t is constant.
Nanites are catalysts in the Assembler's Nanite Hatch, not consumed.

**Condensates (SL-B10..B15)**, map `TecTechRecipeMaps.condensateGeneratorRecipes`, source -> the same amount:

| ID | Fluid (registry name) | Source (unit) | Time, EU/t | First stage |
|---|---|---|---|---|
| SL-B10 | Entangled Raw Star Matter (`btsentangledstarmatter`) | Raw Star Matter 1,000 L | 30 s, UIV | Eye T1 output |
| SL-B11 | Entangled Excited Stellar Plasma (`btsentangledexcitedplasma`) | ESPM 1,000 L | 60 s, UMV | Eye T2 |
| SL-B12 | Entangled Hyperdense Stellar Mixture (`btsentangledhyperdense`) | HSM 1,000 L | 90 s, UMV | Kerr T2 densify (Eye T3's RHSP) |
| SL-B13 | Entangled White Dwarf Matter (`btsentangledwhitedwarf`) | molten WDM 144 L | 120 s, UXV | Eye T4 |
| SL-B14 | Entangled Black Dwarf Matter (`btsentangledblackdwarf`) | molten BDM 144 L | 150 s, UXV | Eye T5 |
| SL-B15 | Entangled Antipolarization Agent (`btsentangledantipolarization`) | Agent 1,000 L | 60 s, UMV | pre-Eye chemistry |
| SL-B16 | *reserved*: Entangled Magnetar Matter (`btsentangledmagnetar`, registered by the Homogeny/Reality work) | | | Eye T6 |

If GT already condenses a source fluid (a future `CondensateType`), our generator recipe is skipped and the recipes
use GT's fluid. Amounts keep GT's half-unit rule (multiples of 500 L / 72 L). If a generator recipe fails, every
recipe needing that condensate is skipped and falls back.

**Condensate Assembler recipes:**

| ID | Output | Items [nanite tier] | Condensates | Time, EU/t | Replaces |
|---|---|---|---|---|---|
| SL-B20 | Stabiliser Pylon | Reinforced Spatial 32 [4], Reinforced Temporal 16 [4], Boundary 16 [3], FG UMV 8 [3], frame Neutronium 16 [1], circuit UMV 8 [2], dense Neutronium 16 [1], MM Field Manipulator I 4 [4], MM Shielding I 8 [2] | SpaceTime 256 ing, Neutronium 64 ing, Space 16 ing, Time 16 ing | 1,800 s, UMV | SL-P21 |
| SL-B21 | Star Chart: Sol | Data Orb [1], circuit UMV [2], plate Infinity 4 [1], MM Sensor Array I [3] | SpaceTime 1 ing | 120 s, UMV | SL-P22 |
| SL-B22 | Stellar Field Sensor Hatch | Hull UMV [2], Sensor UMV 2 [3], MM Sensor Array I [4], circuit UMV 2 [2], plate SpaceTime 4 [1] | SpaceTime 4 ing | 60 s, UMV | SL-E07 |
| SL-B30 | Stellar Mass Core | Beam Band 32 [2], FG UMV 16 [3], circuit UMV 32 [2], dense SpaceTime 16 [1], Emitter UMV 8 [3], Stellar Ignition Charge 16 [4], MM Energy Conduit I 4 [3] | **Raw Star Matter 256,000 L**, SpaceTime 256 ing, Infinity 512 ing | 2,400 s, UMV | SL-E03 |
| SL-B31 | Kerr Density Upgrade | Bastion 64 [1], Bastion 64 [1], dense Infinity 64 [1], FG UMV 32 [3], circuit UMV 32 [2], Field Coil 32 [2], Black Hole Seed 32 [4], Rail Mk III 16 [3], MM Field Manipulator I 8 [4], MM Electrogravitic Valve I 8 [3] | **Raw Star Matter 4,000,000 L**, SpaceTime 4,096 ing, Space 64 ing | 3,600 s, 4x UMV | SL-K04 |
| SL-B32 | Bastion Plate x64 | dense Neutronium 64 [1] + 32 [1], plate TM 32 [2], frame Infinity 16 [2], MM Shielding I 4 [3] | Infinity 256 ing | 2,400 s, UMV | SL-P18 |
| SL-B33 | Casimir Plate x16 | plate Neutronium 32 [1], foil SpaceTime 64 [2], MM Field Manipulator I 2 [4], MM Waveguide I 2 [3] | SpaceTime 16 ing, Space 8 ing | 600 s, UMV | SL-P19 |
| SL-B34 | Harmonic Lining x64 (alternative) | plate Neutronium 72 [1], foil Infinity 96 [1], plate TM 12 [2], MM Shielding I 2 [3] | Infinity 64 ing | 1,800 s, UMV | none: SL-P12 stays |
| SL-B40 | Beam Band Casing x32 | Reinforced Spatial 2 [4], Quadrupole 2 [3], Emitter UMV 4 [3], dense SpaceTime 8 [1], MM Waveguide I 2 [3] | SpaceTime 128 ing, **Raw Star Matter 4,000 L** | 3,600 s, UMV | SL-P15 |
| SL-B41 | Star Chart: Sirius A | Data Orb [1], circuit UMV [2], dense SpaceTime 1 [1], MM Sensor Array I [3] | **Raw Star Matter 10,000 L**, SpaceTime 1 ing | 120 s, UMV | SL-P23 |
| SL-B50 | Shell-Burning Core | Jet Crescent 32 [2], FG UMV 24 [3], circuit UMV 48 [2], Field Coil 16 [2], dense SpaceTime 24 [1], Emitter UMV 12 [3], MM Resonance Chamber II 4 [5], MM Wave Focus I 4 [4] | **ESPM 128,000 L**, SpaceTime 512 ing, TM 512 ing | 3,600 s, 2x UMV | SL-E04 |
| SL-B51 | Jet Crescent Casing x32 | Reinforced Temporal 2 [4], Field Coil 4 [2], dense Infinity 16 [1], Emitter UMV 4 [3], MM Wave Focus I 2 [3] | **Raw Star Matter 128,000 L**, SpaceTime 64 ing, ESPM 1,000 L | 4,800 s, UMV | SL-P16 |
| SL-B52 | Star Chart: Arcturus | Data Orb [1], circuit UMV [2], plate TM 4 [1], MM Sensor Array I [3] | **ESPM 1,000 L**, SpaceTime 1 ing | 120 s, UMV | SL-P24 |
| SL-B60 | Galactic Upgrade Core | Galactic Shell 64 [2], FG UMV 32 [3], circuit UMV 64 [2], Casimir 32 [3], dense SpaceTime 32 [1], Emitter UMV 16 [3], MM Resonance Chamber II 16 [5], MM Field Manipulator I 16 [4] | **HSM 32,000 L**, SpaceTime 1,024 ing, TM 1,024 ing | 4,800 s, 4x UMV | SL-E02 |
| SL-B61 | Star Chart: Neutron Star | Data Orb [1], circuit UMV [2], Galactic Shell 1 [2], MM Sensor Array I [3] | **HSM 1,000 L** | 120 s, UMV | SL-P25 |
| SL-B62 | Galactic Shell Plating x64 (alternative) | dense Infinity 32 [1], frame TM 8 [2], plate Cosmic Neutronium 32 [1], ingot TM 64 [1], MM Shielding I 2 [3] | HSM 500 L, **WDM 4 ing** | 4,800 s, UMV | none: SL-P17 (the T4 gate) stays |
| SL-B70 | Subspace Cooling Core | Galactic Shell 64 [2], FG UMV 32 [4], circuit UXV 16 [3], Casimir 32 [3], dense SpaceTime 48 [1], Sensor UMV 16 [4], MM Sensor Array II 4 [6], MM Field Manipulator II 4 [6] | **WDM 1,024 ing**, DSS 256,000 L, SpaceTime 1,024 ing | 4,800 s, UXV | SL-E05 |
| SL-B71 | Star Chart: Sirius B | Data Orb [1], circuit UXV [3], Galactic Shell 1 [2], MM Sensor Array II [6] | **WDM 7 ing** | 120 s, UMV | SL-P35 |
| SL-B80 | Magnetar Seed Core | Galactic Shell 64 [2], FG UMV 64 [4], circuit UXV 32 [3], Bastion 32 [2], dense SpaceTime 64 [1], Emitter UMV 32 [4], MM Resonance Chamber III 4 [7], MM Shielding II 4 [6] | **BDM 256 ing**, **Agent 64,000 L**, WDM 256 ing, SpaceTime 2,048 ing | 6,000 s, 2x UXV | SL-E06 |
| SL-B81 | Star Chart: SGR 1806-20 | Data Orb [1], circuit UXV [3], dense SpaceTime 1 [1], MM Sensor Array III [7] | **BDM 7 ing**, **Agent 1,000 L** | 120 s, UXV | SL-P37 |

Bold = the gate (the product of the tier the part belongs to, or of the tier below). Every recipe stays inside one
condensate family (RC-2 family 1; S1 "Stellar"; S2 "Remnant": proposal §2 R3), so one network runs a tier's set
without the "more than 3 unrequested condensates" failure. Item sets are unique (the node looks recipes up by items
only).

**Fallback (the safety net).** `BTSRecipesBEC.replace(becId, bec, legacyId, legacy)`: if the BEC recipe is skipped (a
missing component, a condensate without a generator recipe, more than 16 stacks, or GT keeping nothing from `addTo`),
the old body registers instead and the boot log says "SL-Bnn (Condensate Assembler) was skipped; its old recipe SL-xnn
is registered instead". The skip still shows in the recipe summary. No config switch (Q-B8). SL-B01..B04 (the x32
rewrite of RC-2's Spatial / Temporal casing recipes and its fallbacks) are unchanged; see the RC-2 port section.

**Gate audit (rule 8).** A part first needed to build or unlock Eye tier N uses only products of tiers below N; a
transition core may use its own tier's product. The Eye T1 BOM (B20-B22) has no Raw Star Matter or Boundless Cosmic
Solder and nanites at most Transcendent; White Dwarf nanites and metamaterials first appear in the T4 -> T5 core (B70)
and B71, Black Dwarf ones in B80/B81. SL-B62 needs WDM but is only an alternative to SL-P17. No compression generator
recipe changes, so `/btseye gates` is unaffected. GT's own parts count too: RC-2's UXV Field Generator, Sensor and
Emitter each need 576 L molten Universium and 576 L MHDCSM, which only Eye T10 makes once the TecTech Eye is replaced,
so B70/B80 (and their fallbacks SL-E05/E06) take twice as many **UMV** parts instead (question B-72; until then they
were not gate-clean and survival stopped at Eye T4). Ruling 20 / D-45 (overriding RC-2's UXV part recipes) is still
open; the UXV circuits are not re-audited here.


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
| EXTRACT | extractor piece | `extractorRecipes` only | GT's item extractor recipes; no fluid extraction, no Kerr-only map (my ruling, Q-9/Q-22) |
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
- **Power:** **wireless EU** from the owner's network (proposals.md Q2: plain wireless until T7). The whole run's EU is
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
| 28116 | base+16 | **Stellar Field Sensor Hatch** (D-49) |
| 28117-28118 | base+17..18 | **Kerr Compressor Module**, **Kerr Extractor Module** (Kerr redesign) |
| 28119 | base+19 | spare (the next Kerr module) |
| 28120-28129 | base+20..29 | P4: Quasar Engine 28120, Wormhole Mouth 28121, Conduit Mirror 28122, Conduit Junction 28123, Burst Thermaliser Crown Mk I/II/III 28124-28126, **Directive Hatch 28127** (built by the automation pass: one hatch for the Eye, the Kerr and the Quasar), spare 28128-28129 |
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
- **Window bands** (my answer 2026-10-13, render K2 + E-9; B-86): after classifying a build, `windowKerr` /
  `windowEye` turn the cells at |y| ≤ 2 from the object's centre that hide it from the front into ONE Quantum Glass
  pane per sightline (a char per replaced block, whose Java element is Quantum Glass or the old block) with the cells
  behind it open (`' '`). Kerr: the governor arc's outer shell is the pane in front of the hole, the outer mirror
  ring's skin is the drum band's pane elsewhere. Eye: the core band's outer shell in its front ring segments; the
  T4-T6 arm and rim cells and the T7 rail's front clamp (B-114) in front of it open within |x| ≤ 10. The pane in
  front is the first contiguous run of shell cells seen from +z in each column. voxel.js is not edited. Checks: the
  pane counts, the openings, and a clear front sightline at y ±1 (Kerr |x| ≤ 8, Eye T1-T3 |x| ≤ 20, T4-T8 |x| ≤ 8). The manifest carries a `windows`
  section. Run it on the voxel.js revision the shapes' `sha=` names (a01c6b6, 2742379a) until P4's exporter takes
  the window pass over.
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

- One builder at a time, in sequence: Foundation → Kerr → Eye.
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
  `BTSRecipeRemovals.run()` line, or have the registry call the removal itself (agree it between the two units).
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

Written by the Foundation code unit on 2026-10-12. These pin names the spec left open and apply the mid-run
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
| `S` / `T` | **Window band pane** (my answer 2026-10-13, render K2; B-86): `ofChain(Casings.QuantumGlass, Casings.ExtremeDensitySpaceBendingCasing)`, over `A` (the outer mirror ring's casing skin, all round the drum except in front of the hole) / `L` (the governor arc's outer shell in front of the hole, z > 0, \|x\| ≤ 12). At \|y\| ≤ 2 from the hole the cells behind the pane are open (`' '`): the arc's lining, every other layer of both mirror rings, and at T2 the mid band in front of the arc. Glass first (holograms, survival builds and NEI place glass); a Kerr built before the window still forms with its old blocks. 700 pane cells; 3,006 cells opened at T1, 3,323 at T2. |

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
  - EXTRACT searches GT's `extractorRecipes` only (my ruling, Q-9/Q-22).
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
- **SL-K06 is retired.** It was a Kerr-only "Ergosphere Extraction" map melting exotic metals at 144 L per ingot. I
  ruled (Q-9/Q-22) that the extractor runs GT's item extractor recipes only; the map, its lang key and its NEI
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
| `A` | **Window band pane** (my answer 2026-10-13, render E-9; B-86): `ofChain(Casings.QuantumGlass, Casings.ReinforcedSpatialStructureCasing)` over `U`: the core band's outer shell in its front ring segments (z > 0, \|x\| ≤ 26), at \|y\| ≤ 2 from the star. Open (`' '`): the band's lining behind the pane (371 cells) and, in front of it, the T4 / T5 arm and T6 rim cells within \|x\| ≤ 10 (422 / 371 / 213) and, since B-114, the 42 cells of the T7 rail's front clamp (Bastion, rim to rail at 90°) in the band. Glass first; an Eye built before the window still forms. 339 pane cells. |

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
- **Buffers:** one `long` per fluid, capped at 2⁶² L. Every live socket is drained every 20 ticks (Q-30), but only
  up to the fluid's **intake cap** (B-108, `MTEEyeOfHarmonyBTS.intakeCaps`): per star in play max(ignition need,
  20 cycles of upkeep) × p, plus each transition that may begin, plus a planet target's 2 runs at full parallel. A
  fluid with no cap is not pulled; a buffer above its cap is only drawn down.
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

## Contract additions (Homogeny / Reality structures + content)

Written by H/R unit 1 (branch `homogeny-reality`), 2026-10-13. Source: design repo `designs/docs/homogeny-reality.md`
(PROPOSED) and the sketch mockups in `designs/voxel.js` (`3e23c09`). Questions B-74 and B-75.

### Pieces

Re-run `node tools/export-structures.js` (from a worktree it finds `../GTNH-Addons/designs/voxel.js` itself). Same file
format as every other piece. Each machine's frame has its controller facing +z (layer 0 is the front).

| Piece | Machine (MTE) | w × h × d | off | Blocks | Bill of materials |
|---|---|---|---|---|---|
| `homog_t1` | Homogeny (28130), required | 69 × 70 × 73 | 24,35,2 | 17,783 | `Z` 5,008, `A` 3,200, `M` 2,430, `O` 2,246, `B` 1,873, `J` 1,290, `C` 690, `L` 610, `K` 303, `D` 120, `E` 5, `h` 3, `e` 2, `o` 2, `~` 1 |
| `homog_t2` | Homogeny T2 band | 103 × 16 × 113 | 48,8,22 | 10,813 | `G` 7,110, `J` 1,745, `K` 833, `C` 690, `O` 228, `Z` 120, `D` 84, `h` 3 |
| `homog_t3` | Homogeny T3 band | 145 × 52 × 130 | 69,30,31 | 13,002 | `F` 8,489, `J` 3,131, `K` 736, `P` 484, `O` 83, `E` 79 |
| `reality_diffraction` | Reality Diffraction (28131) | 103 × 101 × 85 | 51,97,0 | 53,005 | `A` 11,344, `Z` 9,936, `H` 8,647, `J` 7,795, `Q` 5,756, `N` 3,743, `K` 2,630, `L` 1,916, `E` 1,215, `I` 12, `e` 2, `h` 1, `b` 1, `r`..`w` 1 each, `~` 1 |
| `reality_refab` | Reality Refab (28132) | 104 × 139 × 83 | 51,137,19 | 59,245 | `R` 20,649, `Z` 14,260, `S` 8,952, `J` 4,432, `Q` 3,298, `L` 2,837, `K` 2,357, `E` 1,030, `V` 736, `T` 688, `i` 2, `e` 1, `b` 1, `o` 1, `~` 1 |

- The Homogeny is ONE controller with tier channels: T1 = `homog_t1`; T2 adds `homog_t2`; T3 adds `homog_t3` (counts
  only with `homog_t2`). Tiers only add cells; the earlier piece keeps the 30 / 45 cells a later tier drew over.
- The Homogeny pieces are turned a quarter turn (`machines.homogeny.turns = 1`): the controller on the volute throat
  faces +x in the mockup. `machines.<m>.centre.rel` is the machine axis (the render centre: vortex, region clouds,
  reassembly stream) from the controller, in (a,b,c).
- Assembled sizes (checked by the exporter): Homogeny T1 69 × 70 × 73, T2 103 × 70 × 113, T3 145 × 70 × 130.

### Shape char → intended block

Chars shared by the three machines: `E` Region Crystal (slCasing3:5), `J` Harmonic Lining (slCasing2:2), `K` and `Z`
`Casings.QuantumGlass` (channel / wall, kept apart so either can change), `L`
`Casings.SpatiallyTranscendentGravitationalLens`, `Q` `Casings.ElectromagneticallyIsolatedCasing` (BEC hatch casing).

| Machine | Char → block |
|---|---|
| Homogeny | `A` slCasing3:0 Stirring Band, `B` slCasing3:1 Shear Helix Coil, `C` slCasing3:2 Feed Vessel, `D` slCasing3:3 Vortex Inlet, `F` slCasing3:10 Eternity Shell, `G` slCasing:12 Galactic Shell Plating, `M` `Casings.BoundlessGravitationallySeveredStructureCasing` (Godforge), `O` slCasing:13 Bastion Plate, `P` slCasing:14 Casimir Plate |
| Diffraction | `A` slCasing3:4 Region Deck Band, `H` slCasing3:6 Phase Ring, `I` slCasing3:11 Region Anchor Casing, `N` `Casings.CoherencePreservingPlasmaConduit` |
| Refab | `R` slCasing3:7 Reassembly Hull, `S` slCasing3:8 Reassembly Tray, `T` slCasing3:9 Reassembly Claw, `V` `Casings.CondensateGuidanceCoil` |

### Sockets

| Char | Machine | Count | Accepts (intent) | Fallback | Hint |
|---|---|---|---|---|---|
| `h` | Homogeny (vessel tops) | 3 at T1, +3 at T2 | private feed Input Hatch element | `C` | 1 |
| `o` | Homogeny (throat) | 2 | Output Hatch | `A` | 2 |
| `e` | Homogeny (bottom cap rim) | 2 | Energy / ExoticEnergy | `M` | 3 |
| `b` | Diffraction, Refab | 1 | GT `MTEHatchBEC` | `Q` | 1 |
| `h` | Diffraction (base ring) | 1 | Input Hatch (Entanglement Medium) | `Q` | 2 |
| `r s t u v w` | Diffraction | 1 each | private Region Anchor I..VI (Input Hatch) | `I` | 3..8 |
| `e` | Diffraction, Refab | 2 / 1 | Energy / ExoticEnergy | `Q` | 9 / 4 |
| `i` | Refab (foot) | 2 | Input Bus | `Q` | 2 |
| `o` | Refab (cap) | 1 | Output Bus | `R` | 3 |

Every socket has an outside-reachable face (checked), and each controller's front face is open air in the fullest
build (checked).

### Content

- **Blocks:** `SLBlocks.slCasing3` (`BlockSLCasing3`, constants `STIRRING_BAND` .. `REGION_ANCHOR`), stacks via
  `SLBlocks.casing3(meta, n)`. Region Crystal emits light 9. Block tooltips come from
  `tile.beyondthestars.slCasing3.<meta>.tooltip` (`ItemBlockSL`; slCasing and slCasing2 work the same way, every
  meta in use has one since B-112).
- **Items:** `SLMaxItems.homogenyTier2Core`, `homogenyTier3Core`, `universalUpgradeCore`, `starChartUniverse`
  (stack 1), with lang tooltips.
- **Fluids** (`SLRealityFluids`): `degenerateStellarMixture` (`btsdegeneratestellarmixture`), `entanglementMedium`
  (`btsentanglementmedium`, 0 K), `regionHyperspace` .. `regionNegativeMass` (`btsregion*`, GAS 0 K; `regions()` gives
  I..VI in order), condensates `entangledMagnetar` (SL-B16), `entangledDegenerate` (C01), `entangledErgomatter` (C02),
  `entangledSuperdense` (C03), `entangledWhiteHole` (C04); `ergomatter`, `superdenseErgomatter`, `whiteHoleMatter`
  registered only if absent (H-24, B-75 item 5).
- **Textures:** `sl_stir` .. `sl_alens`, the four item icons, and `iconsets/OVERLAY_FRONT_HOMOGENY`,
  `_REALITY_DIFFRACTION`, `_REALITY_REFAB` (+ `_GLOW`, `_ACTIVE`, `_ACTIVE_GLOW`), all from `tools/gen-sl-textures.js`.
- **Lang:** names and tooltips above; recipe maps `beyondthestars.recipe.homogeny`, `.reality_diffraction`,
  `.reality_refab`; recipe results `beyondthestars.homogeny.tier` / `.installing` and `beyondthestars.reality.nonetwork`
  / `.anchors` / `.full` / `.noassembler` / `.nanites` under `GT5U.gui.text.recipe_result.`.

## Contract additions (Homogeny / Reality machines)

Written by H/R unit 2 (branch `homogeny-reality`), 2026-10-13. Questions B-76 and B-77. Not yet opened in game.

### Files and ids

| MTE | Registry name | Class | GUI |
|---|---|---|---|
| 28130 (base+30) | `multimachine.beyondthestars.homogeny` | `machines/homogeny/MTEHomogeny` | `HomogenyGui` |
| 28131 (base+31) | `multimachine.beyondthestars.realitydiffraction` | `machines/reality/MTERealityDiffraction` | `RealityDiffractionGui` |
| 28132 (base+32) | `multimachine.beyondthestars.realityrefab` | `machines/reality/MTERealityRefab` | `RealityRefabGui` |

`BTSMachines.homogeny`, `.realityDiffraction`, `.realityRefab`. 28133 and 28134 stay spare. Shared:
`MTERealityBase` (single piece, the BEC element), `RealityHatchElement` (`Bec`, `anchor(0..5)`), `HRGui` (rows, bar,
lamps), `HRKit` (kit, build, fill, diagnose), `CommandBTSHR` (`/btshr`).

### Recipe maps (`RealityRecipeMaps`) and process recipes (`RealityProcessRecipes`, guard line `reality-process`)

| Map | maxIO (items in, out, fluids in, out) | Recipes | Notes |
|---|---|---|---|
| `beyondthestars.recipe.homogeny` | 0, 0, 3, 1 | SL-H06 (T1), SL-H11 (T2), SL-H21 (T3) | special value = minimum Homogeny tier |
| `beyondthestars.recipe.reality_diffraction` | 0, 0, 7, 6 | SL-R03 | fluid in 0 = medium, 1..6 = anchors I..VI; out k = region k+1; the machine reads all amounts from here |
| `beyondthestars.recipe.reality_refab` | 16, 1, 6, 0 | SL-R04, SL-R05 | GT's BEC layout: `NANITE_TIERS` per item slot; the compile step moves fluid inputs to `CONDENSATE_INPUT` |

The recipe unit owns every Condensate Assembler / Generator recipe (see "Contract additions (Homogeny / Reality
recipes)"); it must not register the six ids above again.

### Sockets as walked

| Machine | Char | Element | Fallback | Required |
|---|---|---|---|---|
| Homogeny | `h` | GT `InputHatch` (joins `mInputHatches`; truncated if its ring does not count) | `C` | 1+ |
| Homogeny | `o` | GT `OutputHatch` | `A` | 1+ |
| Homogeny | `e` | GT `Energy` or `ExoticEnergy` | `M` | 1+ |
| Homogeny | `E` | Region Crystal, or the Godforge lens glass (B-76 item 3) | | |
| Diffraction | `b` | `RealityHatchElement.Bec` (GT `MTEHatchBEC`) | `Q` | 1+ |
| Diffraction | `h` | GT `InputHatch` (the medium) | `Q` | 1+ |
| Diffraction | `r s t u v w` | `RealityHatchElement.anchor(0..5)` (any `MTEHatchInput`, private list per region) | `I` | 0+ |
| Diffraction | `e` | GT `Energy` or `ExoticEnergy` | `Q` | 1+ |
| Refab | `b` / `i` / `o` / `e` | `Bec` / GT `InputBus` / GT `OutputBus` / energy | `Q` / `Q` / `R` / `Q` | 1+ each |

Hatches are textured as the Godforge casing (Homogeny) or the Electromagnetically-Isolated Casing (Reality).
Structure-error keys: `beyondthestars.structure.homogeny.feed|output|energy`,
`beyondthestars.structure.reality.bec|medium|energy|inputbus|outputbus`.

### State and NBT

| Machine | Item + tile NBT (survives a wrench) | Tile only |
|---|---|---|
| Homogeny (`writeState`) | `btsHomTier`, `btsHomEspm`, `btsHomDegenerate`, `btsHomEscrow`, `btsHomInstalling`, `btsHomInstallTicks` | `btsHomPendEspm`, `btsHomPendDeg`, `btsHomNote` |
| Diffraction | | `btsDiffMask`, `btsDiffPend0..5`, `btsDiffRuns`, `btsDiffNote` |
| Refab | | `btsRefabLinked`, `btsRefabDim/X/Y/Z`, `btsRefabSteps`, `btsRefabCraft`, `btsRefabSub`, `btsRefabNote` |

The Homogeny's one install path is `beginInstall()` then `completeInstall()` (constants `MILESTONE_LITRES` 100,000,
`INSTALL_TICKS` 12,000, `INSTALL_EUT[tier]`). New recipe-result ids: none beyond unit 1's seven (their texts for
`.noassembler`, `.full` and `.nanites` now match what the machines do).

### Hologram, NEI, survival

- Homogeny: trigger stack 1..3 = T1..T3, rings cumulative; survival builds the pieces in order. NEI counts 3 tiers
  (stack 4 adds nothing). Reality machines: one piece, one tier.
- Every plain element is `noOverwrite`; every socket is `buildHatchAdder(...).buildAndChain(casing)`.
- NEI scratch-world height: Refab spans 137 above its controller (y 64 + 137 = 201), Diffraction 97, the Homogeny 35
  above and 34 below; all inside 0-255.

### Test bench (`/btshr`)

- `/btshr <homogeny|diffraction|refab> [t1..t3] [fill] [force]` builds in front of the player.
- `/btshr <machine> [t1..t3] <x> <y> <z> [facing] [fill] [force] [survival] [check]` is the console / command-block
  form: FORMED / NOT FORMED, first wrong cells, a creative re-projection check, survival damage-and-repair.
- `/btshr keep <machine> <x> <y> <z> <facing> [tN]` force-loads (`/btskerr release` frees).
- `/btshr bench <x> <y> <z> <status|check|fill|wrench|milestone e d|escrow|install|itick n|relink|pending [L]|scan|on|off|project n>` (`wrench` also on the Diffraction; `pending <L>` seeds its buffer, B-80).
- `/btshr gui <x> <y> <z> [info]` prints the kit GUI's header and rows (or its Info page) as text, built on the server
  as the GUI syncs them, for the Homogeny, both Reality machines, the Reactor Silo and the production halls (B-98).
  `fill` on a Refab puts the Star Chart's (SL-R05) item inputs in its input bus (B-137).
- `/btshr becnet ...` (B-137, `BECNetBench`): the smallest TecTech BEC network the two Reality machines need, raised
  by script. `field x y z [strength n]` builds a BEC Containment Field (controller facing north) by its own creative
  construct, a GT Debug Energy Hatch (MAX, 64 A) in one of its casing cells, field strength 1,000,000 (its capacity in
  L and its EU/t), on. `assembler x y z [nanites n] [tier t]` builds a Condensate Assembler the same way with a Nanite
  Containment Bus of 64 Black Dwarf nanites (the Refab links the nearest within 64 blocks by itself). `link fx fy fz
  mx my mz` fits the field's BEC Hatch on the side nearest the Reality machine and lays BEC conduit through air from
  the machine's own BEC Hatch to it (or to a conduit already there), and keeps every chunk the conduit crosses loaded
  (an unloaded conduit leaves the grid and splits the network). `power x y z` swaps a multiblock's energy hatches for
  Debug Energy Hatches; `status x y z` prints the field's condensate, the Assembler's nanites, or a Reality machine's
  network, the regions it can reach and its route count. `tools/recording/reality.shots` films the two running on it.
- `/btskit homogeny | diffraction | refab`.

## Contract additions (Homogeny / Reality recipes)

Written by H/R unit 3 (branch `homogeny-reality`), 2026-10-13. Question B-78; costs §12. Not yet booted in game.

### Files and ownership

| File | Owns |
|---|---|
| `BTSRecipesReality` (guard line `reality`, after `reality-process`; after `bec` once master is merged) | every Condensate Assembler and Condensate Generator recipe of the three machines: their blocks, cores, controllers and condensates, plus the `H/R gate:` boot log |
| `RealityProcessRecipes` (line `reality-process`, unit 2) | the machines' own maps (SL-H06/H11/H21, SL-R03, SL-R04/R05); unchanged here |
| `SLRealityFluids.isPlaceholder(Fluid)` | true for Ergomatter / Superdense Ergomatter / White Hole Matter while only this file registered them (no Eye makes them yet) |
| `RealityRecipeMaps` | the three NEI tabs show their controller (`neiHandlerInfo` display stack, read lazily) |

### Condensates (Condensate Generator, `gt.recipe.create-condensate`, source -> the same amount)

| Id | Condensate (fluid) | Source, unit | Time, EU/t |
|---|---|---|---|
| SL-B10 | Entangled Raw Star Matter (`btsentangledstarmatter`) | Raw Star Matter, 1,000 L | 30 s, UIV |
| SL-B13 | Entangled White Dwarf Matter (`btsentangledwhitedwarf`) | molten WDM, 144 L | 120 s, UXV |
| SL-B14 | Entangled Black Dwarf Matter (`btsentangledblackdwarf`) | molten BDM, 144 L | 150 s, UXV |
| SL-C01 | Entangled Degenerate Stellar Mixture (`btsentangleddegenerate`) | Degenerate, 1,000 L | 120 s, UXV |
| SL-C02 | Entangled Ergomatter (`btsentangledergomatter`) | Ergomatter, 1,000 L | 120 s, UXV |
| SL-C03 | Entangled Superdense Ergomatter (`btsentangledsuperdense`) | Superdense Ergomatter, 1,000 L | 150 s, 4 × UXV |
| SL-C04 | Entangled White Hole Matter (`btsentangledwhitehole`) | White Hole Matter, 144 L | 180 s, MAX |
| (SL-B16) | Entangled Magnetar Matter (`btsentangledmagnetar`) | no generator recipe: ruling 19a left it without a consumer | - |

SL-B10/B13/B14 have one owner: `SLCondensates` registers the fluids at preInit and `BTSRecipesBEC` (line `bec`) their
generator recipes. `BTSRecipesReality.becCondensate` only resolves which fluid the Reality recipes ask for (the output
of whatever generator recipe already condenses the source fluid, else `SLCondensates`' fluid) and registers nothing,
so the order of the `bec` and `reality` lines does not matter. (Before the post-merge tidy, B-87, the H/R branch's
`shim` could register them itself and re-ran SL-B10/B13/B14 under the same ids.) `SLRealityFluids` registers its
condensates through `SLCondensates`' registrar.

### Condensate Assembler recipes (`gt.recipe.assemble-condensate`)

Nanite tier per stack: 1 Carbon, 2 Silver, 3 Gold, 4 Transcendent, 5 Six-Phased Copper, 6 White Dwarf, 7 Black Dwarf
(never above 7: H-17). "ing" = 144 L. Durations are nanite-time; EU/t constant.

| Id | Output | Items (count; nanite tier) | Condensates | Time, EU/t | Stage |
|---|---|---|---|---|---|
| SL-H01 | The Homogeny | Stirring Band 16 (2); Vortex Inlet 4 (2); FG UMV 16 (3); Pump UMV 32 (2); circuit UMV 32 (2); Bastion 32 (1); dense SpaceTime 16 (1); Energy Conduit I 4 (3); Field Manipulator I 4 (4) | Raw Star Matter 1,000,000 L; SpaceTime 4,096 ing; Infinity 256 ing | 3,600 s, 4 × UMV | S1 |
| SL-H02 | Stirring Band Casing ×32 | Reinforced Spatial 2 (4); dense Infinity 8 (1); frame SpaceTime 8 (1); Piston UMV 2 (3); Waveguide I 2 (3) | SpaceTime 64 ing; Raw Star Matter 8,000 L | 2,400 s, UMV | S1 |
| SL-H03 | Shear Helix Coil ×32 | Ergosphere Field Coil 4 (2); wireGt04 SC UMV 8 (2); foil Infinity 32 (1); Motor UMV 2 (3); Field Manipulator I 1 (4) | SpaceTime 16 ing; Space 8 ing | 1,200 s, UMV | S1 |
| SL-H04 | Feed Vessel Casing ×32 | plate Neutronium 32 (1); Quantum Glass 8 (1); Pump UMV 2 (2); frame Infinity 4 (1); Shielding I 1 (3) | SpaceTime 8 ing; Infinity 16 ing | 900 s, UMV | S1 |
| SL-H05 | Vortex Inlet ×8 | Pump UMV 8 (2); huge Neutronium pipe 8 (1, large if absent); Stirring Band 4 (2); circuit UMV 4 (2) | SpaceTime 16 ing; Raw Star Matter 4,000 L | 600 s, UMV | S1 |
| SL-H10 | Homogeny Tier II Core | Galactic Shell 64 (2); Feed Vessel 16 (2); Vortex Inlet 8 (2); FG UXV 16 (5); Pump UXV 32 (4); circuit UXV 32 (4); Bastion 32 (2); Electrogravitic Valve II 4 (6); Field Manipulator II 4 (6) | **Black Dwarf Matter 256 ing** (gate, ruling 19a); White Dwarf Matter 256 ing; SpaceTime 1,024 ing | 4,800 s, UXV | S5 |
| SL-H20 | Homogeny Tier III Core | Eternity Shell 64 (6); Region Crystal 8 (7); FG UXV 64 (6); Pump UXV 64 (5); circuit UXV 64 (5); Galactic Shell 64 (3); Resonance Chamber III 8 (7); Sensor Array III 4 (7) | **White Hole Matter 64 ing** (gate); Superdense 32,000 L; Degenerate 64,000 L; Eternity 64 ing | 6,000 s, MAX | S9 |
| SL-R12 | Eternity Shell Casing ×32 | Reinforced Temporal 2 (4); dense Infinity 8 (1); plate MagMatter 1 (5); frame SpaceTime 8 (2); Shielding II 2 (6) | Degenerate 500 L; SpaceTime 32 ing | 3,600 s, UXV | S6 |
| SL-R10 | Region Deck Band ×32 | Reinforced Spatial 2 (4); dense TM 16 (2); frame Infinity 8 (1); EM-Isolated Casing 1 (4); Shielding II 1 (6) | Degenerate 500 L; SpaceTime 32 ing | 3,600 s, UXV | S6 |
| SL-R11 | Region Crystal ×16 | Godforge lens glass 2 (6); block Hexanite 2 (4); Emitter UXV 2 (5); Wave Focus II 2 (6) | Ergomatter 1,000 L; Superdense 500 L; Degenerate 500 L; MagMatter 1 ing | 2,400 s, UXV | S8 |
| SL-R13 | Phase Ring Casing ×32 | Reinforced Temporal 2 (4); dense Cosmic Neutronium 16 (2); FG UXV 1 (6); Condensate Transformative Coil 1 (5); Waveguide II 1 (6) | Degenerate 500 L; Ergomatter 1,000 L; SpaceTime 32 ing | 3,600 s, UXV | S8 |
| SL-R14 | Reassembly Hull Plating ×64 | Reinforced Spatial 2 (4); dense Infinity 16 (1); dense Cosmic Neutronium 16 (2); frame TM 16 (2); Shielding II 2 (6) | Degenerate 500 L; SpaceTime 32 ing | 3,600 s, UXV | S6 |
| SL-R15 | Reassembly Tray Plating ×32 | Reinforced Temporal 1 (4); dense TM 16 (2); Casimir Plate 4 (3); frame Infinity 8 (1); Field Manipulator II 1 (6) | Degenerate 500 L; SpaceTime 32 ing | 2,400 s, UXV | S6 |
| SL-R16 | Reassembly Claw ×32 | Robot Arm UXV 8 (5); Conveyor UXV 4 (5); Tray Plating 4 (3); circuit UXV 4 (4); Field Manipulator II 1 (6) | Superdense 500 L; Degenerate 500 L | 1,200 s, UXV | S8 |
| SL-R17 | Region Anchor Casing ×4 | Region Crystal 2 (7); EM-Isolated Casing 2 (4); Harmonic Lining 4 (1); Sensor UXV 1 (5) | Degenerate 500 L; SpaceTime 8 ing | 600 s, UXV | S8 |
| SL-R01 | Reality Diffraction | Region Crystal 6 (7); Phase Ring 32 (6); Deck Band 32 (6); FG UXV 32 (6); Emitter UXV 32 (5); circuit UXV 64 (5); Coherence-Preserving Plasma Conduit 16 (5); Condensate Transformative Coil 8 (5); Sensor Array III 4 (7); Resonance Chamber III 4 (7) | **White Hole Matter 256 ing** (gate); Superdense 64,000 L; Degenerate 64,000 L; SpaceTime 4,096 ing | 7,200 s, MAX | S9 |
| SL-R02 | Reality Refab | Region Crystal 8 (7); Hull 32 (6); Claw 24 (6); Robot Arm UXV 32 (5); FG UXV 16 (6); circuit UXV 64 (5); Condensate Guidance Coil 16 (5); EM-Isolated Casing 16 (4); Field Manipulator II 8 (6); Resonance Chamber III 4 (7) | **White Hole Matter 256 ing** (gate); Eternity 128 ing; Degenerate 64,000 L; SpaceTime 4,096 ing | 7,200 s, MAX | S9 |

Stages (doc §0): S1 after Eye T1 (Raw Star Matter, nanites ≤ 4); S5 after Eye T5 (WDM, BDM, nanites ≤ 7); S6 after
Homogeny T2 (Degenerate); S8 after Eye T8 (Ergomatter, Superdense; P4); S9 after Eye T9 (White Hole Matter; P5).
Families (≤ 4 condensates per recipe): Homogeny T1 in S1 "Stellar" (Raw Star Matter, SpaceTime, Space, Infinity);
SL-H10 in S2 "Remnant" (BDM, WDM, SpaceTime); everything else in S4 "Reality" (White Hole, Superdense, Ergomatter,
Degenerate, Eternity, MagMatter, SpaceTime).

### Rules the helper enforces (`assemble`, `cond`)

- At most 16 item stacks (stacks above 64 split, each with its slot's tier), at most 4 condensates, one output; a
  nanite tier above Black Dwarf throws (H-17); GT must keep the recipe (an empty `addTo` is a logged skip).
- Our condensates' amounts are multiples of half their unit (72 L or 500 L); a condensate whose generator recipe was
  skipped makes every recipe that needs it a logged skip, never a recipe asking for an unmakeable fluid.
- Dense plates fall back to the block, then to 9 plates; the huge Neutronium pipe falls back to the large one.

### Boot log

- `[BTS] H/R gate: <recipe> is registered but unreachable until an Eye makes <fluid> (Eye T<n>, P4/P5).` for SL-C02..C04,
  SL-R11, R13, R16, R17, SL-H20, SL-R01, R02 and the process recipes SL-H21, SL-R03, R04, R05, while the fluids are
  placeholders; then a count line. When P4/P5 own the fluids: "every Homogeny/Reality recipe is reachable".
- `[BTS] SL-B16 Entangled Magnetar Matter: no Condensate Generator recipe ...` (ruling 19a).
- On a build without master's BEC expansion: `SL-B10/B13/B14: ... registered it under the same name`.

## Contract additions (H/R review fixes)

Written by the H/R review-fix pass (branch `homogeny-reality`), 2026-10-13. Question B-80; costs §12.

### Machines

- **Recipe-check pushes (RC-2 checks are event-driven).** `MTEHomogeny.beginInstall` / `completeInstall` / the
  sneak-punch cancel call `scheduleRecipeCheckImmediate()`. `MTERealityBase.onPostTick` pushes
  `scheduleRecipeCheck(RecipeCheckReason.THROTTLED)` once a second while the machine is formed, enabled and idle.
  Diffraction anchor hatches go through `addIfSmartInput`; `flushPending` emptying the buffer pushes an immediate
  check. Any new idle reason a Reality machine returns is covered by the 1 s push; a new Homogeny state that blocks
  mixing must push a check when it clears.
- **Diffraction item NBT.** `setItemNBT` writes `btsDiffPend0..5` (when > 0) and `btsDiffRuns`; the same keys as the
  tile NBT.
- **Quasar hooks (P4 fills them; all return 0 now).** `MTEHomogeny.quasarSigma()`, `MTEHomogeny.minSigma(GTRecipe)`
  (refusal `beyondthestars.homogeny.stability`), `MTERealityDiffraction.quasarSigma()`, `MTERealityDiffraction.MIN_SIGMA`
  (refusal `beyondthestars.reality.stability`). Both refusals have `GT5U.gui.text.recipe_result.` lang keys.
- **Reserved port cells, char `g`.** `homog_t2`: 3 cells (stirring ring outer face), element = Galactic Shell
  Plating. `reality_diffraction`: 6 cells (top deck band outer face), element = Region Deck Band. P4 replaces each
  element with `buildHatchAdder(...).atLeast(<GRB Receptor Port>).buildAndChain(<the same casing>)`, never a new
  cell, so standing builds stay formed. `HRKit` places no hatch in `g`.
- **Homogeny caps.** Legend `M` = `Casings.ReinforcedTemporalStructureCasing` (was the Godforge Boundless casing);
  the cap centres are `Z` (Quantum Glass; the Homogeny's `L` char is gone). `MTEHomogeny.hatchIndex()`, the energy
  socket's fallback and the controller texture use the Temporal casing.

### Recipes / gates

- `BTSRecipesReality.ETERNITY_BEFORE_T10` (false) marks D-48 proposal 1 as unbuilt; the gate log reports SL-H20,
  SL-R02, SL-R03, SL-R04, SL-R05 against it. The commit that builds the Eternity-on-Ergomatter swap sets it true and
  flips the questbook's `eternityOnErgomatter` feature in the same merge.
- `SLGateDump.FIRST_MADE`: Eternity 7 (decided), MagMatter not an Eye product (D-48 proposal 2).

### Questbook merge (homogeny-reality + questbook; rule 15)

The Homogeny T1 and T2 ship on this branch, so when the two branches merge:

1. Flip `features.homogeny` to `true` in `questbook/singularity.json`, but first give **48184 The Homogeny T3** (and
   the Reality quests) a `needs` that stays false until P4/P5 (`["homogeny", "eyeT9T10"]`), so they stay
   placeholders: their fluids are P4/P5.
2. **48118 The Homogeny** (`TheHomogeny`, Tier12UMV): keep the retrieval task `1x mte:28130`; add a fluid task
   `1000x` Excited Stellar Plasma Mixture (or a checkbox "Build T1"). Rewrite the text in the present tense, drop any
   "(planned)" / "Coming in a later update" note, and replace the "About five Homogenies keep a T3 Eye fed without a
   second Eye" paragraph with: "Turns stockpiled Raw Star Matter into Excited Stellar Plasma Mixture unattended: no
   hydrogen, helium, ignition or field. It costs 100 L per 1 L, 10x the Raw Star Matter an Eye T2 needs, so it suits
   banking a Sol surplus or a hands-off line, not replacing Sirius A." Keep "Eye T2 is still required".
3. **48142 The Homogeny T2** (Tier13UXV): replace the `tbd` task with a retrieval of the Homogeny Tier II Core
   (`beyondthestars:homogenyTier2Core`) and a fluid task `1000x` Degenerate Stellar Mixture; present tense; mention
   the three reserved Quasar port cells only as "later".
4. Regenerate the bundle (`tools/gen-singularity.js`, `--check`, `--selftest`) after the edit.

**Done at the merge into master (2026-10-13, B-82).** Steps 1-4 as written; 48118 has no optional structure
checklist (no `bomItems.homogeny` table yet), and 48142 warns that the Tier II Core's UXV parts still need Universium.

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


---

## P4

**Design of record:** `designs/docs/quasar-p4.md` (rev 7f91607; D-50 in `PROPOSAL.md`). Every PROPOSED line is
accepted, and §10.2's questions take their recommendations. Branch `p4-quasar`; judgement calls are P4-xx in
`singularity-ladder-questions.md`.

### What's built

| Unit | State | Commit |
|---|---|---|
| Structures: voxel cells (§7), export, manifest | built | 26b04e7 (addon); design repo 1b407b3 |
| Design repo §9: `bh-model.js`, `recipes.js` cards, renames, D-50, page rebuilt | built | design repo 1b407b3 |
| Engine maths: quasar disc, conduit maths, links, Eye T7/T8 field and transitions, P4 Directive rules, golden, rig proof | built | 4850153, 566cfe5, 1747224; design repo 330e4df |
| Content + recipes (§6): fluids, items, slCasing2:3-11, textures, lang, BTSRecipesQuasar, SL-P40, the Eternity swap, NEI pages, P4 gate audit | built | a67ef11, 2799a9e, 486877f, e89b314 |
| Quasar Engine MTE 28120, Burst Control Hatch 28126, burst API, sensor quasar channels, render, kit, bench, OC | built | ee919a7, c44f995, 89ac19e, c47fc63 |
| Conduit (Mouth 28121, Mirror 28122, Junction 28123), Crown 28124, GRB Receptor Port 28125, solver, host API, OC, kit, bench | built | 0f99c6b, 92c10ca, 6d6e8db |
| Eye T7 Triangulum + T8 Milky Way: rail / halo bands, receptor groups, the two galaxy transitions, links, planets mode, companions, Directive sets, sensor channels, GUI, render, NEI, OC, kit, bench | built | 57ff9a9, 4d7f61c, 6ff4868, 60a569f |
| Other hosts (KEE, Homogeny T2/T3, Reality Diffraction), OpenComputers (rate limit, trim, the P4 components), automation bench and the unattended T6 → T7 → T8 proof on a real quasar and Eye, automation doc §7, questbook contract | built | ef77c88, 93e992f, cabab76, 0a02809, d6652e0, 9c58843, bbcac79, 4bf6030, 8bdb47d, f9e5c2f, 9d0360f, aadf752 |
| Full RC-2 dev-server test of the branch (load, lang lint, golden, gates, every structure both ways in several facings, quasar, conduit, Eye T6 → T8, hosts, Superdense refinery, OC, rigs, restart, a pre-P4 world, regression); fixes P4-83..P4-87 | tested, 4 fixes | eb8e7fa, d2250a8, d1132d0, 05bc373, a6dedd2, ad235eb |
| Review fixes P4-88..P4-94: T7 test-fire ceiling, Q-8 pauses non-Eye link work, module parallel capped, NaN-proof setters, receptor warm-up after a load, weak conduit tables, misaligned docks vent | fixed, dev-server tested | 00b5c89, 670ba74, a166922, c518fc9, b4f24cb, bd5e283, e3f0e70, ba90b9a |

### MTE ledger (P4 block 28120-28129)

| Id | Machine | Class (proposed) | Shape(s) |
|---|---|---|---|
| 28120 | Quasar Engine | `machines/quasar/MTEQuasarEngine` | `quasar` |
| 28121 | Wormhole Mouth | `machines/conduit/MTEWormholeMouth` (built) | `conduit_mouth` |
| 28122 | Conduit Mirror | `machines/conduit/MTEConduitMirror` | `conduit_mirror` |
| 28123 | Conduit Junction | `machines/conduit/MTEConduitJunction` | `conduit_junction` |
| 28124 | Burst Thermaliser Crown (Mk by structure) | `machines/quasar/MTEBurstThermaliserCrown` | `crown_mk1`, `crown_mk2`, `crown_mk3` |
| 28125 | GRB Receptor Port (hatch) | `machines/conduit/MTEHatchGRBReceptor` | — |
| 28126 | Burst Control Hatch (analog redstone in) | `machines/conduit/MTEHatchBurstControl` | — |
| 28127 | Directive Hatch | exists (`machines/directive/MTEHatchDirective`); P4 adds the Disc / Field / Events / Transition / Slots sets | — |
| 28116 | Stellar Field Sensor Hatch | exists; P4 adds the quasar channels, the analog window and invert | — |
| 28128-28129 | spare | — | — |

### Shapes (resources `assets/beyondthestars/structures/`, loaded with `BTSShapes.load(name)`)

Regenerate the shapes with `node tools/export-structures.js`; `--check` must pass. Every new piece is in its
controller's own frame (front = +z, layer 0), and `off=` is the controller.

| Piece | Machine | Size (w × h × d) | off | Chars |
|---|---|---|---|---|
| `quasar` | 28120 | 143 × 161 × 144 | 71,95,0 | A B C E G H I P T · ~ m e h t c n d |
| `crown_mk1/2/3` | 28124 | 33×17×33 / 49×23×49 / 65×29×65 | 16,16,4 / 24,22,12 / 32,28,20 | A C F R · ~ m r |
| `conduit_mouth` | 28121 | 39 × 42 × 10 | 19,39,0 | A C E I P · ~ m e u |
| `conduit_mirror` | 28122 | 23 × 13 × 24 | 8,11,11 | A C E M P · ~ m u |
| `conduit_junction` | 28123 | 89 × 30 × 83 | 45,0,41 | A C E G P · ~ m e u c |
| `eye_rail` | Eye T7 band | 183 × 31 × 175 | 91,57,47 (outside the box) | B J Z P · r |
| `eye_halo` | Eye T8 band | 196 × 62 × 218 | 98,71,71 (outside the box) | B Z · r |
| `eye_core` (changed) | Eye T1 | 102 × 52 × 80 | 50,50,0 | + `d` ×4, `p` ×1 on former plinth deck cells |
| `homog_t2`, `reality_diffraction` | unchanged shapes | — | — | `g` is now the GRB Receptor Port (fallback G / A) |

### Chars → elements (contract for the MTE units; the legend in `manifest.json` carries the same)

**Quasar** (`quasar`):

| Char | Element |
|---|---|
| `A` | BHC Extreme Density Space-Bending Casing |
| `B` | Naquadah Alloy frame |
| `C` | NEW Jet Collimator Ring |
| `E` | NEW Eddington Wall Casing |
| `G` | Quantum Glass |
| `H` | NEW Disc Field Coil T1-T4, `ofBlocksTiered` on channel **`coil`** (1-4; one tier everywhere; the tier sets b ≤ tier and the mass cap 256 / 1,024 / 2,048 / 4,096 SM) |
| `I` | NEW Rim Gas Injector |
| `P` | Casimir Plate (slCasing:14) |
| `T` | BHC Background Radiation Absorbent Casing |
| `h` ×24 | private gas element: any input hatch, ME stocking included; fallback `E`; min 1 |
| `t` ×4 | private trim element: plain input hatch only; fallback `T` |
| `c` ×4 | Burst Control Hatch on the cap rims (0, ±80, ±12); fallback `A` |
| `n` ×2 | Sensor Hatch; fallback `A` |
| `d` ×2 | Directive Hatch; fallback `A` |
| `e` ×3 | Energy / ExoticEnergy; min 1 |
| `m` | Maintenance |

**Crown** (`crown_mk1/2/3`):

- Chars: `A` BHC casing, `C` Jet Collimator Ring, `F` NEW Burst Thermaliser Casing, `R` NEW Radiator Petal, `r` GRB
  Receptor Port (fallback `A`), `m` Maintenance.
- No dynamo and no energy socket: EU goes to wireless.
- The Mk is whichever of the three pieces checks. Try Mk III, then II, then I. The hologram channel **`crown`** (1-3)
  picks the piece to build.

**Conduit:**

| Char | Element |
|---|---|
| `E` | TecTech Reinforced Spatial Structure Casing |
| `A` | Reinforced Temporal Structure Casing |
| `C` | Jet Collimator Ring |
| `P` | Casimir Plate |
| `I` | Rim Gas Injector (mouth) |
| `M` | Superradiant Mirror Casing (mirror) |
| `G` | Quantum Glass (junction) |
| `u` | private Throat Fuel element: any input hatch, ME stocking OK; mouth 8, mirror 1, junction 6; min 1 |
| `c` | Burst Control Hatch, junction only, 2 cells: the split's level s, plus one spare |
| `e` | optional energy socket (P4-07) |
| `m` | Maintenance |

**Eye:**

- `B` Bastion Plate (slCasing:13).
- `P` Casimir Plate.
- `r` GRB Receptor Port, fallback `B`. Its group is `manifest.pieces.eye_rail|eye_halo.sockets.r[i].group`:
  `rail-high` / `rail-low` / `halo` (P4-03).
- `d` Directive, fallback plinth casing (element added).
- `p` slot bus, fallback plinth casing. Its element is temporarily `EyeHatchElement.Items`; the TODO is a private
  SlotBus element (P4-04).
- `eye_rail` counts only with every band through `eye_rim`, and `eye_halo` only with `eye_rail` (cumulative, D-49).

**Homogeny T2 / Reality Diffraction:** `g` = GRB Receptor Port (28125). Its fallback stays the ring / deck casing, so
pre-P4 builds stay formed.

**Kerr:** no shape change. The `u` utility sockets also accept a GRB Receptor Port (legend text).

### Geometry contracts in `manifest.json` (all offsets in the controller's (a, b, c) frame)

- `machines.quasar.slots.top|bottom`:
  - `portRel`: the jet axis cell, (0, ±81, 0) in voxel space.
  - `axisAbc`: the jet's direction.
  - `crownControllerRel` and `crownFacingAbc`: where a docked crown's controller stands and faces.
  - `crownFlippedVertically`: true on the bottom port.
- `machines.quasar.centre.rel`: the hole (render) centre.
- `machines.crown.intake[piece]`: the base centre, where the jet enters from the crown controller, and `axisAbc`.
- `machines.crown.stack[piece].rel`: the controller of a second crown stacked on this one (same shape, same facing; 0
  shared cells).
- `machines.conduit_mouth|mirror|junction.throats[]`: `role`, `radius`, `centreRel` and `normalAbc`, the opening's
  outward normal.
  - Mirror: `in` and `out` (90°).
  - Junction: the big throat (split in / combine out), then out1 (0°), out2 (+60°) and out3 (−60°, the fixed share off
    the top).
- `machines.quasar.channels.coil` and `machines.crown.channels.crown`.

### Contracts between units

- **Eye unit.** It owns the T7/T8 bands. Load `eye_rail` / `eye_halo` as cumulative optional pieces, and add the `B`,
  `P` and `r` elements. Turn the `p` element into a private slot bus.
- **Quasar / crown / conduit units.** Load the shapes above, with `noOverwrite` on plain casings. Read the slot / throat
  data from `BTSShapes.manifest()`; do not hard-code offsets.
- **Recipe unit.** Done: every block in the BOM above has a batch recipe (see "Content and recipes" below, costs
  §14).
- **Design repo.** `designs/bh-model.js` summarises the disc (`discStep`), the demand (`demand`), the 1.5× rule
  (`topTier`, `massForTier`), crown banks (`thermBank` on the mean) and throat upkeep (`throatUpkeep`). The **reference
  for the Java** is the prototype's engine (`designs/quasar-p4-prototype.html`, ENGINE + P4 blocks), which the golden
  test pins. `discStep` is the same maths, but some of its float operations are grouped differently.
- **Every MTE unit** calls the engine below and never re-derives a formula. A tuning change goes into the prototype
  first, then `--refresh-fixture`, then the Java, then the golden.

### Content and recipes (§6; questions P4-23..P4-35, costs §14)

**Fluids** (`fluids/SLQuasarFluids`, registered before `SLRealityFluids`; `SLRealityFluids.ergomatter` /
`superdenseErgomatter` are aliases):

| Field | Registry name | Made by |
|---|---|---|
| `SLQuasarFluids.ergomatter` | `btsergomatter` | Eye T7 |
| `SLQuasarFluids.superdenseErgomatter` | `btssuperdenseergomatter` | Eye T8 (1 : 1), or SL-Q23 (8 : 1) |
| `SLRealityFluids.entangledMagnetar` | `btsentangledmagnetar` | SL-B16 (now P4's generator) |

**Items** (`items/SLQuasarItems`; texture and lang key from the field name; tooltips in the lang file):
`quasarSeed`, `conduitThroatMk1` / `Mk2` / `Mk3` (`throatTier(stack)` 0-3, `throat(tier)`), `intergalacticUpgradeCore`,
`supermassiveSeedCore`, `starChartTriangulum`, `starChartMilkyWay`, `gammaRayBurst` (NEI display only, no creative tab).

**Blocks** (`blocks/BlockSLCasing2`, `SLBlocks.casing2(meta, n)`):

| Meta | Constant | Block | Shape char |
|---|---|---|---|
| 3-6 | `DISC_COIL_T1..T4` (`discCoil(tier)`, `discCoilTier(meta)`) | Disc Field Coil T1-T4 | quasar `H`, channel `coil` |
| 7 | `EDDINGTON_WALL` | Eddington Wall Casing | quasar `E` |
| 8 | `RIM_GAS_INJECTOR` | Rim Gas Injector | quasar / mouth `I` |
| 9 | `JET_COLLIMATOR` | Jet Collimator Ring | `C` everywhere |
| 10 | `BURST_THERMALISER` | Burst Thermaliser Casing | crown `F` |
| 11 | `RADIATOR_PETAL` | Radiator Petal | crown `R` |

**Machine stacks** (`BTSMachines`, null until the MTE unit registers its machine; its recipe waits until then):
`quasarEngine` 28120, `wormholeMouth` 28121, `conduitMirror` 28122, `conduitJunction` 28123,
`burstThermaliserCrown` 28124, `grbReceptorPort` 28125, `burstControlHatch` 28126.

**Overlays** (`Textures.BlockIcons.custom("beyondthestars:iconsets/<NAME>")`, from `tools/gen-sl-textures.js`):
`OVERLAY_FRONT_QUASAR_ENGINE`, `OVERLAY_FRONT_BURST_CROWN`, `OVERLAY_FRONT_WORMHOLE_MOUTH`, `OVERLAY_FRONT_CONDUIT_MIRROR`,
`OVERLAY_FRONT_CONDUIT_JUNCTION` (each `+ _GLOW`, `_ACTIVE`, `_ACTIVE_GLOW`, 8 frames), `OVERLAY_HATCH_GRB_RECEPTOR`,
`OVERLAY_HATCH_BURST_CONTROL` (each `+ _GLOW`). Block textures `sl_disccoil_t1..t4`, `sl_eddington`, `sl_rimgas`,
`sl_collimator`, `sl_thermaliser`, `sl_petal`.

**Recipes** (`BTSRecipesQuasar`, one owner, guard line `quasar` after `reality`):

| Id | Output | Machine | Notes |
|---|---|---|---|
| SL-B16 | Entangled Magnetar Matter | Condensate Generator | 144 L → 144 L, 180 s, 2 A UXV |
| SL-Q01 | Quasar Engine | assembly line (research: Quasar Seed) | Agent 256,000 L; Magnetar 21,888 L; waits for 28120 |
| SL-Q02..Q05 | Disc Field Coil T1-T4 ×32 | Condensate Assembler | T4 keeps MagMatter (optional, P4-24) |
| SL-Q06 | Eddington Wall Casing ×64 | Condensate Assembler | |
| SL-Q07 | Rim Gas Injector ×8 | Condensate Assembler | |
| SL-Q08 | Jet Collimator Ring ×32 | Condensate Assembler | |
| SL-Q09 | Quasar Seed | `KerrRecipeMaps.DENSITY` | HSM 64,000 + Magnetar 1,440 L + 4 Black Hole Seeds |
| SL-Q10 / Q11 / Q12 | Wormhole Mouth / Conduit Mirror / Conduit Junction | assembly line | Agent; wait for 28121-28123 |
| SL-Q13 / Q14 / Q15 | Conduit Throat Mk I / II / III | CA / CA / assembly line | Mk III takes Ergomatter |
| SL-Q16 | Burst Thermaliser Crown | assembly line | waits for 28124 |
| SL-Q17 / Q18 | Burst Thermaliser Casing ×64 / Radiator Petal ×64 | Condensate Assembler | |
| SL-Q19 / Q20 | GRB Receptor Port / Burst Control Hatch | Assembler, UMV (Hull UMV) | wait for 28125 / 28126 |
| SL-Q21 | Intergalactic Upgrade Core | Condensate Assembler | SpaceTime fallback for MagMatter (Q-22) |
| SL-Q22 | Supermassive Seed Core | Condensate Assembler | Entangled Ergomatter 128,000 L |
| SL-Q23 | Superdense Ergomatter 500 L | `KerrRecipeMaps.DENSITY` | Ergomatter 4,000 + SpaceTime 1,440 L, circuit 8 |
| SL-Q24 / Q25 | Star Chart: Triangulum / Milky Way | Condensate Assembler | |
| SL-Q26 | Eternity on Ergomatter | load-complete rewrite | catalyst 9,216 L, Timepiece 4,608 L Ergomatter |
| SL-Q27 / Q28 | NEI pages | `QuasarRecipeMaps.QUASAR` / `THERMALISER` | fake recipes |
| SL-Q29 | Lattice Frame Casing ×4 (SMBH) | Assembler, UXV | MagMatter frame, 4 BDM plates, 2 SpaceTime foils (the 72 L), 36 L Superdense Ergomatter |
| SL-Q30 | Containment Node L1 (SMBH) | assembly line, UXV (research: Lattice Frame) | 8,000 L Superdense Ergomatter + 2,304 L Magnetar Matter; 4 UMV FG (B-72), 4 any-UXV, 2 dense BDM |
| SL-Q31 | Containment Node L2 (SMBH) | Condensate Assembler, MAX | L1 node + 4,032 L Entangled White Hole Matter; hidden in NEI, placeholder until P5 |
| SL-Q32 / Q33 | Containment Node L3 / L4 (SMBH) | Condensate Assembler, MAX / MAX ×4 | L3: 2,304 L Entangled Universium; L4: 4 Eternity + 4 MHDCSM plates, MHDCSM + Universium 1,440 L each, 16 any-MAX |
| SL-Q34 | Event Horizon Thermaliser Casing ×4 (Crown Mk IV) | Assembler, UXV | 4 MagMatter plates, Neutronium frame, 144 L Superdense Ergomatter |
| SL-Q35 | Conduit Throat Mk IV | assembly line, UXV (research: Throat Mk III) | Mk III, 2 UMV FG (B-72), 16 Casimir, 4 any-UXV, 4,000 L Superdense Ergomatter |
| SL-P40 | *retired 2026-10-14 (B-135): now only the fallback of SL-G03, GT's own recipe rewritten in place* Resonance Chamber IV | Condensate Assembler | SpaceTime foils + 1,500 L Entangled Ergomatter |

SL-G03 (which retired SL-P40) and SL-Q26 run only with `replaceTecTechEyeOfHarmony`. `BTSRecipesReality.ETERNITY_BEFORE_T10` =
`BTSRecipesQuasar.eternityBeforeT10()`; `eternitySwapped()` reports SL-Q26's outcome after load-complete.

**Dev-server check** (this worktree, `runServer` on port 25581, `/btseye gates`): `Recipe registration complete: 308
registered, 0 skipped`; "P4 recipes: 7 wait for their machine's MTE unit"; SL-Q26 rewrote 1 catalyst and 1 Timepiece
recipe; research items unique; compression gates T1-T9 clean; P4 gate audit 27 parts, 0 loops (P4-34).

**Contracts.**

- **MTE units:** assign your `BTSMachines` stack in `register()`; your recipe then registers on the next boot with no
  other change. Use the overlays above. The Quasar and crown controllers return `QuasarRecipeMaps.QUASAR` /
  `THERMALISER` from `getAvailableRecipeMaps()`.
- **Conduit units:** read a throat with `SLQuasarItems.throatTier(stack)`; `BurstRouting`'s Mk index is that value.
- **Hosts unit (Kerr):** in the Compressor module's DENSITY gate, require `BTSRecipesQuasar.densityMinMass(recipe)` SM
  and, if `densityNeedsLink(recipe)`, the KEE's quasar link at σ ≥ `SUPERDENSE_MIN_SIGMA`. Add the GT5U recipe-result
  lang keys for any new failure ids (ruling 10).
- **Questbook merge:** `docs/p4-questbook-contract.md` "Content + recipes unit"; flip `eternityOnErgomatter`.
- **Recipe cost:** `singularity-ladder-costs.md` §14.

### Engine maths (pure Java; the B-37 discipline)

The engine has no Minecraft or GT types. It is bit-identical to the prototype on 38 golden cases (`node
tools/quasar-golden.js --check --java`), and the T1-T6 golden still passes 78 of 78 (`node tools/fieldsim-golden.js
--check --java`). Both tools compile only the pure packages with javac, so no gradle run is needed. `QuasarRigSim` is
the §8.4 proof: `java -cp build/classes/java/main com.nateyburr.beyondthestars.machines.quasar.engine.QuasarRigSim
[hours] [seeds]`.

**One engine definition.** The D-49 ENGINE block is the same text in `star-lifecycle-prototype.html` and
`quasar-p4-prototype.html` (P4-14). `tools/fixtures/star-lifecycle-engine.js` is that block.
`tools/fixtures/quasar-engine.js` is the P4 block, cut against the same engine sha. Both tools hash with LF line
endings.

| Class | What it is |
|---|---|
| `quasar/engine/QuasarDisc` | `step(QuasarState, QuasarInputs) → QuasarResult`: one disc second (the prototype's `quasarStep`, without its rig). |
| `quasar/engine/QuasarState` | The hole and disc: `hole` (NONE / OPEN / REFORM / DORMANT), M, a, E, Φ, I, lock, zone, Y_q, the eruption, re-form count, lagE / lagPhi, G0 / G / λ / λc / f_eff / b / Ṁ_Edd, the purge latch, milestones, `rng`. It has `seed(M0, a0)`, `collapse()`, `reopen()`, `requestPurge()` and `rngFor(seed)`. |
| `quasar/engine/QuasarInputs` | Per second: gas, coil tier, b setpoint, `powerOk`, `gasDrawn` (L), trim units (gas / Space, ≤ 10), `regulatorPurge` (golden only). |
| `quasar/engine/QuasarResult` | b, coil EU/t, Ṁ_Edd, f_eff, G (both jets), `jet` (= G/2 per port), G0, trim litres, zone, reforming / reformed / blowOut (+ reason), eruption / purge / purgeRefused, massCapped, milestones. |
| `quasar/engine/QuasarParams` | Constants and helpers: `eddingtonFeed`, `coilEUt` (2·UXV·b²), `massCap` (256 / 1,024 / 2,048 / 4,096), `aEq`, `iscoEta`, `g0`, `stream`, trim litres per unit, `meanStream` (× 1.11 with no purge, × 0.8 at Φ 40), `stability`, `EU_PER_BU` (10¹³). |
| `quasar/engine/QuasarGas` | H (κ 0.4), He (0.2), Raw Star Matter (0.1). |
| `quasar/engine/BurstRouting` | Throat caps (24 / 384 / 6,144) and upkeep (SpaceTime 4 / 16 / 64, Ergomatter 0.25 / 1 / 4 L/s per element-leg), crown caps (32 / 512 / 8,192), `crownEUt`, the Junction `share(c, k, s)` and `split`, the sensor `level(v, w)`, `twoLevel(hi, lo)`, the reopen time (10 s), hop limits (64 blocks, 32 hops). |
| `quasar/engine/BurstLink` | A host's link check: `roll(v, part)` (60 s window), `update(ok)` (1,800 s streak, `done`, `best`), `stabilityOk(linked)`, `heavyOk(linked, testFire)`, `sigma(delivered, D_h)`, host demands. |
| `quasar/engine/QuasarSim` | The prototype's world (quasar, standard conduit rig, links, Directive bits, Eye T6-T8) and the golden runner. It is not loaded in game. |
| `quasar/engine/QuasarRigSim` | The §8.4 proof: Directive bits plus three analog wires, and the all-binary variant. |
| `eye/field/TierParams` | `T7` (Triangulum: P, W), `T8` (Milky Way: P, W, Λ), `PLANETS7` / `PLANETS8` (one gauge, 3 / 6 slots). `of(n)` gives galaxy mode and `of(n, planets)` picks the mode. Also `burstDriven()`, `burstDrift`, `burstTerm`, `sigma(k, companions)`. |
| `eye/field/StellarField` | Gauges W = 4 and Λ = 5 (`GAUGES` = 6). T7/T8 run inside the same `step`: the minor merger's precession flip, the satellite crossing, the β sum, the T8 feed, and the galaxy cycle product (0 while a transition runs). |
| `eye/field/GalaxyField` | `demandT(T)` = 1.5·4^(T−7), `demand(T, planets, companions)`, the products (600 × Y × r × min(β̄, 1.5); 1.0 × fed × Y × r), Magnetar 7.5 L per cycle, f₈ 0-10 (default 5), the minor merger, the crossing (`crossingKick`, `crossingBonus`), planets and companions (parallels, the `Companion` table). |
| `eye/field/MagnetarCollapseOverlay` | T6 → T7: overfeed (WDM 61.44 + BDM 15.36 L/s, Ω and Π +2·t/300, countdown at 270), the collapse at 300 (shock − 10 per Bu/t above D₇, Integrity −20, the tier becomes T7, a forced merger at +450), and the halo capture (60 s Stable within 420-600). It has `demand()`, `normDemand()` and `predictedShock(G)`. |
| `eye/field/GalacticMergerOverlay` | T7 → T8: `begin(s)` (3 sign draws, events held), passes at 600 / 1,200 / 1,800 (W ±12, P +5 for 5 s, warned 60 s ahead), the tier becomes T8 at 600 (Λ and its gain ramp in), Ergomatter 5 L/s, a 120 s Stable streak within 300 s, then `light()`. It has `demand()`, `ramp()`, `warnedPass()` and `restore(..)`. |
| `eye/field/PlanetSlots` | Planets mode: `tick(tier, zone, m)` returns the finished `Run`s (parallel 2^(N − k), success (1 − 0.05R) × field factor). Also `failRuns()` and `anyIdle(tier)`. |
| `directive/QuasarDirective` | `disc(State, w)`: the Disc set. |
| `directive/GalaxyDirective` | `field`, `events`, `transition` (with the collapse-assist schedule), `ready`, `slots` and `testFireWanted`. |
| `directive/DirectiveSet` | The new sets, appended: QUASAR_DISC (host QUASAR), EYE_FIELD, EYE_EVENTS, EYE_GALAXY_TRANSITION and EYE_SLOTS. The Eye's built Transition set is unchanged (P4-17). |

**StepInputs / StepResult additions** (the Eye unit's side):

- In: `beta`, `gHi`, `gLo`, `gHalo` (β and γ, normalised by D·m·p, halo test fire excluded), `gEye` (raw Bu/t, for
  the collapse shock), `f8` (L/s per parallel), `companions`.
- Out: `cycleMeanY`, `cycleRMin` (companion products are base × 2^(N−1−k) × mY × rMin), `companionCycle`, `ergoFed` /
  `ergoToDraw`, `crossingWarned` / `Hit` / `Caught` / `Missed` / `Bonus`, `countdown`, `wdmCharged` / `bdmCharged` /
  `ergoCharged` (a transition's charge, taken even on its failing second), `collapsed`, `merged`.
- `Overlay` gains `running()` (false only on NONE), `drift(t, s, in)`, `sigma(t, k, in)` and `addEvents(s, in, E, q,
  out)`. T1-T6 overlays are untouched.
- `FieldState` gains `prec` and `tde` (Crossing), and `Cycle.bSum` / `Cycle.f8`. `x` and `prevE` are 6 long. **The Eye
  unit must save** `prec`, `tde` (next, left, warned, ok, tau, n, caught), `x[4..5]`, `prevE[4..5]`, `cyc.bSum` and
  `cyc.f8`, and each overlay's phase, st, streak (and, for the merger, its signs) to NBT.

**Contracts between the engine and the MTE units:**

- **Quasar unit (28120).** Each second, if the hole is OPEN or REFORM:
  1. `medd = QuasarParams.eddingtonFeed(M, gas)`; drain up to `f·medd` L of gas from the `h` sockets into
     `in.gasDrawn`.
  2. Drain `coilEUt(min(b, coil))` and set `powerOk`.
  3. Read the `t` sockets into the trim units (the litres come out of `QuasarResult`).
  4. Call `QuasarDisc.step`, then push `result.jet` into each jet port's dock. A port with no formed dock vents.
  5. A Burst Control Hatch rising edge (or OpenComputers `purge()`) calls `requestPurge()`. In b mode the hatch sets
     the b setpoint to 0.5 + (tier − 0.5)·s/15.
  6. Directive hatches: `QuasarDirective.disc` with the hatch's window. The state is `lagE`, `lagPhi`, the GUI's purge
     setpoint, open, `b`, the vented Bu/t, and conduit-open.
  7. Save the whole `QuasarState` (and M, a on the item). Seed new holes with `seed(64, SEED_SPIN)` (P4-16).
- **Conduit / crown units.** Use `BurstRouting` for every cap, upkeep, split and level. The crown's EU/t is
  `crownEUt(min(stream, cap))`, pushed to wireless.
- **Receptor / host units.** Each GRB Receptor Port records last second's Bu/t per source. A host rolls a `BurstLink`
  per milestone:
  - Eye T6 Stability Link: roll the rail total, with the rail-high part.
  - Eye T7 Heavy Link: roll the Eye total, with the halo part.
  - Other hosts read `BurstLink.sigma`.
- **Eye unit.**
  - Fill the StepInputs above from the receptor groups.
  - Use `TierParams.of(n, planets)`.
  - Run the transitions through `MagnetarCollapseOverlay` (shock 120, P4-21) and `GalacticMergerOverlay`.
  - Apply the StepResult extras: Magnetar Matter 7.5 L per cycle at T7, Ergomatter `ergoToDraw`, the crossing bonus,
    companion upkeep and products from `GalaxyField.Companion`, and planet runs from `PlanetSlots`.
  - Codes for the new sets come from `GalaxyDirective`, with `testFireWanted` for Slots [8] (P4-18).
- **Directive Hatch tooltip** (rule 14). The P4 sets are selectable now and read 0 until their host reports them. The
  hatch's tooltip must gain the P4 tables when the hosts land (shift-expansion; `docs/p4-questbook-contract.md` has the
  text).

### Quasar Engine MTE (28120) and the Burst Control Hatch (28126) (questions P4-36..P4-45)

**Files.**

| Class | What it is |
|---|---|
| `machines/quasar/MTEQuasarEngine` | The controller: shape `quasar`, coils on channel `coil`, private sockets, the disc second, jets, Directive / sensor publishing, NBT, render placement, tooltip. |
| `machines/quasar/MTEQuasarEngineGui` | MUI2 console: terminal (hole, mass bar, spin / λ / Bu/t, E dial, flux bar, Integrity / Y_q / Stability, jets, feed, coils, links, last event), gap row (feed f), side panels Disc control and Jets & links, render toggle. |
| `machines/quasar/QuasarHatchElement` | Private sockets: Gas `h`, Trim `t` (plain only), BurstControl `c`, Sensor `n`, Directive `d`. |
| `machines/quasar/QuasarGeometry` | `manifest.json machines.quasar`: centre, port cells and axes, crown controller cells. |
| `machines/quasar/QuasarSensorChannel` | The sensor hatch's 16 quasar channels and their analog mapping. |
| `machines/quasar/QuasarKit` | `/btskit quasar`, `build` (creative / survival + standard hatches + seed), `diagnose`. |
| `machines/quasar/render/*` | `TileQuasarHole`, `BlockQuasarHole`, `QuasarHoleRenderer` (client). `Config.renderQuasar`. |
| `machines/quasar/burst/*` | The burst API (below). |
| `machines/conduit/MTEHatchBurstControl` (+ GUI) | 28126: level s, latched rising edge, modes PURGE / FIELD / SPLIT / BEGIN. |
| `CommandBTSQuasar` | `/btsquasar` build / check / bench. |
| `integration/QuasarComputerApi` | `bts_quasar` (OpenComputers, soft). |

**The second** (every 20 ticks while formed and every structure chunk loaded): Burst Control polls (PURGE edge →
`requestPurge`, FIELD → b); refill the gas buffer from `h`; draw f·Ṁ_Edd; trims from `t`; `QuasarDisc.step`; drain the
trim litres; a `JetPacket` per port to the dock on its cell, else vent; Directive + sensor publish. Coil power
`coilEUt(min(b, coil))` is drained every tick while OPEN or REFORM; one short tick makes the next second's
`powerOk = false`.

**Frozen** (`isFrozen()`): not formed, or any chunk under the structure's box unloaded. Nothing steps or publishes.

**NBT.** `writeQuasarState` (tag `btsQuasar`) is the one serializer for tile and item: every `QuasarState` field, the
RNG state, feed, gas, b, purge setpoint, render switch, counters. Tile only: `btsQuasarBuffer` (fluid → L) and
`btsQuasarRenderPos`.

**Bench** (`/btsquasar`, dev server port 25581): `[t1..t4] x y z [facing] [force] [survival] [seed] | check`;
`status | seed [M] [a] | open | collapse | purge | mass M | feed f | gas H|He|RSM | b v | run s [fill] | trim g s |
refill | redstone on|off | keep` + `x y z`; `release`. Run (2026-10-13): creative FORMED; survival 51,983 blocks in
261 rounds FORMED, re-projection safe, damage-and-repair 2/2; seeded from the item, an hour of bench seconds (spin
0 → 0.35, 35 eruptions), trims, a redstone edge on a Burst Control Hatch purged flux 48 → 4, collapse at 1,024 SM →
dormant, Open refused without a Black Hole Seed; both holes identical after a server restart. `Recipe registration
complete: 310 registered, 0 skipped` (SL-Q01 and SL-Q20 register now).

**The burst API (contract for the Conduit, crown, receptor and host units).**

- `JetPort` TOP / BOTTOM. Port cell = `QuasarGeometry.portRel(port)` from the controller; `MTEQuasarEngine.portCell`.
- `BurstSource(dim, x, y, z)`: the quasar controller. `resolve()` gives the MTE if loaded; `status()` gives RUNNING /
  FROZEN / UNLOADED. **Q-8:** a host whose source is FROZEN or UNLOADED pauses (freezes its field / cycle); RUNNING
  with 0 Bu/t is felt in full.
- `JetPacket(source, port, buPerTick, linkable, tick)`: one port's second. `linkable` = formed, hole OPEN, b > 0 (the
  source end of a link). Stale after 40 ticks (`stale(now)`).
- `IBurstDock`: `dockReady()`, `acceptJet(JetPacket)` returns the Bu/t taken (the rest shows as clipped), or
  `IBurstDock.REFUSED` when the dock faces the wrong way (the quasar counts the whole jet as vented, P4-94),
  `dockLabel()`. A crown or mouth calls `BurstDocks.register(world, x, y, z, this)` every second with its intake cell
  (crown: its base centre, `machines.crown.intake[piece].rel`; mouth: its throat centre) while formed and facing the
  axis; `unregister` on unform is optional.
- Back to the quasar: `MTEQuasarEngine.reportHost(key, label, buPerTick, sigma, streak0to1)` once a second per host
  fed (the GUI's Links, the sensor's σ / streak channels, `getLinks()`); entries expire after 60 ticks. A Conduit
  element-closed flag for Disc bit 4 is still to be added (P4-44).
- Readers: `jetBu(port)`, `jetTaken(port)`, `jetDock(port)`, `getVented()`, `source()`, `isFrozen()`.
- `MTEHatchBurstControl`: `poll(host, line, modes...)` once a second, `level()`, `consumeRisingEdge()`, `getMode()`.

### The Conduit, the crowns and the GRB Receptor Port (28121-28125) (questions P4-46..P4-57)

**Files.**

| Class | What it is |
|---|---|
| `machines/conduit/MTEConduitElement` | Shared base of the three elements: the throat item (controller slot, `SLQuasarItems.throatTier`), upkeep, open / closed, `receive` (cycle, 32 hops, throat cap, clipped), `emit` through cached routes, books, scanner, NBT (tag `btsConduit`), tooltip helpers, `facingFor` (bench). |
| `machines/conduit/MTEWormholeMouth` (28121) | A pair by data stick (`partner` {dim, x, y, z}); a dock (`IBurstDock`) on its front-face centre; front → back; `relay` on the partner. |
| `machines/conduit/MTEConduitMirror` (28122) | 90° corner, any facing, reversible (P4-49). |
| `machines/conduit/MTEConduitJunction` (28123) | Split (big in; out 3 = p3 first, then `BurstRouting.split` on share₁) or combine (3 small in, big out). c, k, p3, mode in its GUI / OC; s from the first Burst Control Hatch (SPLIT mode). |
| `machines/conduit/MTEHatchGRBReceptor` (28125) | The sink: per (source, jet port) delivery of last second, same-tick arrivals add; Q-8 known sources (positions, NBT `btsReceptorKnown`). |
| `machines/quasar/MTEBurstThermaliserCrown` (28124) | One controller, Mk = biggest of `crown_mk3/2/1` that checks (channel `crown`, else stack 1-3); dock on its intake; receptor `r`; EU to wireless; overflow to one stacked crown. |
| `machines/conduit/ConduitNet` | The solver's world side: in-throat registry (40-tick freshness, weak per world), `trace` (ray → in-throat / receptor / blocked / unloaded / nothing), `MAX_RAY` 72, entry cos ≥ 0.5, routes cached `ROUTE_TICKS` 100. |
| `machines/conduit/ConduitThroats` | `manifest.json machines.<piece>.throats[]` → `Throat` / `WorldThroat` (centre, unit normal, radius). |
| `machines/conduit/FlowBook` | Per-second books: same tick adds, a later tick replaces; < 20 ticks = this second (caps), < 40 = last second (display). |
| `machines/conduit/ConduitHatchElement`, `machines/quasar/CrownHatchElement` | Private sockets: Fuel `u`, BurstControl `c` (junction), Receptor `r` (crown). |
| `machines/conduit/ConduitElementGui`, `GRBReceptorGui`, `machines/quasar/CrownGui` | MUI2: status terminal; fuel cycle button; the Junction's panel (mode, c slider, k cycle, out 3 slider). |
| `machines/quasar/burst/BurstStream`, `BurstReading`, `BurstHost` | The stream in flight (source, port, Bu/t, linkable, path); a port's / host's reading; the host-side API (below). |
| `machines/conduit/ConduitKit`, `CommandBTSConduit` | `/btskit conduit`; `/btsconduit status | dock | rig x y z | rig status | pair | throat | fuel on/off | junction | rebuild [tier] | keep | survival`. |
| `integration/ConduitComputerApi` + `BTSEnvironments.Conduit/Junction/Crown/Receptor` | OpenComputers `bts_mouth`, `bts_mirror`, `bts_junction`, `bts_crown`, `bts_receptor` (§8.2; plus `setFuel`). |

**The walk.** A quasar's second pushes each jet port's `JetPacket` into the dock on its cell. A Wormhole Mouth takes it
(`receive`, its cap), its partner `relay`s it (its own cap) and emits it out of its back; every hop is a cached ray to the
next in-throat or receptor, delivered at once, in the same tick. Each element applies `BurstRouting` (caps, split,
share). A closed element (no throat, or upkeep short) loses the stream and calls `MTEQuasarEngine.noteConduitClosed`
(Disc bit 4 for 2 s; closes P4-44). Unloaded elements break the path (P4-56).

**Upkeep** (P4-48): one unit per element a second while formed with a throat, `BurstRouting.throatUpkeep(mk, fuel)`,
from the `u` hatches; Auto / SpaceTime / Ergomatter; closed until 10 s of full upkeep.

**Docking** (contract with the quasar unit, P4-52, P4-54): a crown registers its intake (`machines.crown.intake`) and
checks in `acceptJet` that its intake axis equals the jet's world axis (standing on the top cap; rotation UPSIDE_DOWN
under the bottom cap). A mouth registers its front-face ring centre and checks that its back axis (+c) equals the jet's.
A misaligned dock takes 0 and says so. `/btsconduit dock <quasar>` prints both port cells and the crown cells.

**NBT.** Elements: fuel mode, open, reopen, owed litres and fuel, litres used, the partner (mouth), mode / c / k / p3
(junction). Crown: the last formed Mk and lifetime EU. Receptor: known sources. Nothing about a link is saved.

**Host-side API (contract for the Eye and Hosts units).**

- Put `MTEHatchGRBReceptor` sockets in the host (private element, as the crown does with `CrownHatchElement`); the Eye
  groups them by `manifest.pieces.eye_rail|eye_halo.sockets.r[i].group`.
- Own one `BurstHost` per host (not saved). Once per host second:
  `BurstReading r = host.second(ports, hostKey, hostLabel, demand, streak01)` — it polls the ports (they learn the
  host's name), rolls the 60 s mean unless paused, computes σ against `demand` (`BurstLink.KERR_T1/T2`, `HOMOGENY_T2/T3`,
  `REALITY`, or `GalaxyField.demand(...)`), and reports the host to every feeding quasar (`reportHost`).
- Read: `r.total`, `r.bySource`, `host.delivered()`, `host.sigma()` (last second), `host.sigmaMean()` (60 s mean,
  P4-50; held while paused), **`host.sigmaInForce()`** (the 60 s mean, or 0 while paused: use it for recipe conditions
  and bonuses, P4-89), `host.mean()`, `host.meanFull()`, `host.linked()`,
  `host.linkSource()`, `host.linkLine()` (GUI / scanner text).
- **Q-8:** if `r.paused` (`host.paused()`), freeze the field, cycle, streaks and recipe progress for that second; do
  not read 0. `r.pausedBy` names the source.
- **Groups** (the Eye): read each group with `BurstHost.read(groupPorts, label)` (a static, no rolling) and roll the
  milestone `BurstLink`s yourself: Stability Link = roll(rail total, rail-high part), `stabilityOk(linked)`; Heavy Link
  = roll(Eye total, halo part), `heavyOk(linked, testFire)`. `linked` = the group reading's `linked()`.
- A link is a fresh delivery > 0 Bu/t from a linkable stream (formed, OPEN, b > 0) over an open path (P4-51).

**Bench** (`/btsconduit`, dev server port 25581, flat world `p4test`, 2026-10-13):

- `rig 8000 100 8000`: quasar (coils T3, 64 SM, a 0.5) + Mk I crown on the top port + mouth A on the bottom port, mouth
  B, Mirror 1, split Junction (c 0.5, out 3 10 %), Mirror 2 turning up into a remote Mk I crown's receptor, receptors on
  out 2 / out 3. All FORMED; every element opened after 10 s. Jets 1.35 Bu/t each; top crown 1.35 Bu/t →
  1.35 × 10¹³ EU/t; bottom jet → mouth A → B (1.35) → Mirror 1 (40 blocks) → Junction (30) → out 1 0.61 → Mirror 2 (20)
  → remote crown receptor (20) 6.1 × 10¹² EU/t, LINKED; out 2 0.61 and out 3 0.135 LINKED. The bench owner's wireless
  network grew by 1.2 × 10¹⁶ EU in about 30 s.
- Fuel off on Mirror 1: CLOSED, downstream receptors "nothing received, not linked", the quasar's Disc code 13 (bit 4,
  "conduit closed"). Fuel back: open after 10 s, links back.
- Two quasar blocks removed: the quasar FROZEN; receptors and the remote crown "PAUSED: its quasar ... is not running"
  (Q-8). `rebuild` re-formed it.
- Junction k 4, s 0: share₁ 0, out 2 takes it all.
- Server restart: pairs, throats, open state, junction settings and litres kept; with the chunks forced the whole chain
  flowed again (remote crown linked).
- `survival` (StructureLib's survival path, then re-projection and damage-and-repair): mouth 1,403, mirror 1,702,
  junction 17,232, crown Mk I / II / III 1,834 / 2,810 / 4,421 blocks, all finished, FORMED, re-projection safe, repair
  2/2 or 1/1.
- Boot: `Recipe registration complete: 315 registered, 0 skipped`; "P4 recipes: every machine recipe is registered"
  (SL-Q10, Q11, Q12, Q16, Q19 now register; SL-Q16 fixed, P4-55).
- Not tested in game: the Ergomatter fuel path, cross-dimension mouths, the GUIs and NEI preview (no client run).

### Eye T7 Triangulum and T8 Milky Way (questions P4-58..P4-73)

**What's built.** The Eye (MTE 28115) runs T7 and T8 on the engine unit's field maths (`TierParams` T7 / T8 /
PLANETS7 / PLANETS8, and `StellarField`'s galaxy hooks). It also runs the T6 → T7 Magnetar Collapse and the T7 → T8
Galactic Merger in the D-49 frame: milestone, then the core in escrow, then the charge, then the transition.

**Files.**

| Class | What it is |
|---|---|
| `eye/MTEEyeOfHarmonyBTS` | `PIECES` gains `eye_rail` (T7) and `eye_halo` (T8): cumulative bands, `MAX_TIER` 8, compression gate metas 6 / 7. New elements: `B` (Bastion), `P` (Casimir), `r` (a GRB Receptor Port, falling back to `B`; its group comes from the manifest, matched by its offset, `receptorSockets()`). `d` takes a Directive Hatch or a Burst Control Hatch; `p` is the private slot bus. `StarHost.bursts()` reads the three groups through `BurstHost` and reports the Eye to its quasars. Bench: `debugInject`, `firstSlotBus`. |
| `eye/EyeHatchElement` | `Receptor`, `BurstControl`, `SlotBus`. |
| `eye/star/EyeStarRuntime` | The galaxy flow: `wanted()` (planets-mode variants), the galaxy chain (`galaxyChain`, `towardDest`, `tryGalaxy`), `galaxyInputs` (β, the γs, `gEye`, f₈, companions), `takeCharges`, `collapsed` / `merged`, `galaxyAfterStep` (the T8 feed, the crossing, countdowns, planets, companions, the two links), `requestBegin`, the P4 Directive codes, `galaxyReady`, `testFireWanted`, planets mode, companions, and the scanner lines. |
| `eye/star/EyeGalaxy` | The galaxy state and its NBT: the mode and f₈ (item NBT); the links' windows and streaks, carries, companions and planet slots (tile NBT). |
| `eye/star/EyeBursts` | One second of the receptor groups (hi, lo, halo, linked, paused, source, port counts). |
| `eye/star/MagnetarCollapse`, `GalacticMerger` | `EyeTransition`s wrapping `MagnetarCollapseOverlay` (shock 120) and `GalacticMergerOverlay`, with NBT. |
| `eye/star/EyeMilestones` | T6 → T7 and T7 → T8 (`linkDone`, `linkBest`, flares, mergers, `prod[6..7]`, `res[6..7]`). |
| `eye/star/EyeGalaxyBench` | `/btseye star … burst | galaxy`: the scripted link (P4-71). |
| `eye/field/FieldReadout` | Channels W, LAMBDA, BETA, LINK_STREAK, LINK_MEAN, SLOT_PROGRESS, COUNTDOWN (appended); `withGalaxy`. |
| `eye/render/EyeStarLook`, `EyeStarRenderer` | TRIANGULUM, MILKY_WAY, PLANETS; arms, bar and orbits (P4-64). |
| `eye/EyeGui` | Galaxy terminal lines and the Galaxy panel (mode, f₈, Begin). |
| `integration/BTSComputerApi`, `oc/BTSEnvironments` | `bts_eye` `getEvent`, `getUnlock`, `beginTransition`, `setFeed8`, `setMode`, `getSlots`, plus `getField`'s W, L, β, γs, D. |

**Stars** (`EyeRecipeRegistry.registerGalaxies`):

- `star:triangulum`:
  - T7, chart Triangulum;
  - upkeep: Molten Magnetar Matter 7.5 L a cycle (`StarDef.exactUpkeep`, carried);
  - product: Ergomatter 600 × Y × r × min(β̄, 1.5);
  - charge, NEI only: the collapse's WDM 18,432 + BDM 4,608.
- `star:milkyway`:
  - T8, chart the Milky Way;
  - feed: Ergomatter f₈ (0-10 L/s, default 5);
  - product: Superdense Ergomatter 1 : 1 × Y × r;
  - charge, NEI only: Ergomatter 9,000.
- `star:planets7` / `star:planets8`: registered by key only (`StarDef.registerVariant`). `StarDef.ofTier` skips them.

**The flow.**

1. **The link.** The magnetar is lit at T6. The rail's receptor ports feed the Stability Link: rail total, high bank,
   1.5 Bu/t, 25-75 %, 1,800 s. The galaxy rig wires Slots [8] to J1 = 15.
2. **The begin.** With the item milestones met, the Intergalactic Upgrade Core in escrow and the Triangulum chart in
   the slot, the Eye is PENDING. Galaxy Transition [1] goes on; Begin starts the Magnetar Collapse.
3. **The collapse.** Overfeed, collapse (`collapsed()`: the star becomes Triangulum and its first cycle is charged),
   halo capture, then T7 is unlocked.
4. **T7 steady state.** P is held by bursts and Space trims, W by the rail's banks. The Heavy Link counts the Eye's
   total with the halo share (the halo is a test fire at T7).
5. **The merger.** The Supermassive Seed Core and the Milky Way chart, then Begin: the Galactic Merger (`merged()` at
   the first pass), then T8 (the Milky Way lit by `relit()`).
6. **T8 steady state.** P, W and Λ; the feed; the satellite crossing and its bonus.

A lost galaxy re-runs from a re-spawned magnetar (P4-59) until T7 is unlocked. From then on (D-54, B-217) it relights at
Triangulum, the base of the intergalactic band, from a new Magnetar Collapse charge, and the transitions above re-run.

**NBT** (every key is new; a pre-P4 save reads defaults):

- **Item NBT:** `btsEyeMode` and `btsEyeF8`; milestones `t6` / `t7` (prod, res, flares / mergers, link, linkBest).
- **Tile NBT, star:** `planets`, `W`, `L`, `prec`, `prevE4..5`, `tde{next, left, warned, ok, tau, n, caught}`,
  `cyc.bSum`, `cyc.f8`, `mcShock`.
- **Tile NBT, `galaxy{}`:** the links' sparse windows, n, streak, best and done; carries; companions with their r;
  slots; the last run.
- **Transitions:** `btsEyeTrans`. A MAGNETAR_COLLAPSE saves its phase, st, streak, Mns and the shock readouts; a
  GALACTIC_MERGER saves its phase, st, streak and signs. A merger with no signs is dropped.

**Contracts for other units.**

- **Hosts unit.** The Eye uses the host-side API as specified. Its host key is `eye@dim:x,y,z`, its label "Eye of
  Harmony", and its demand is `galaxyDemand × p`.
- **The Directive Hatch's tooltip** has the Eye's Field, Events, Galaxy Transition and Slots tables.
- **Questbook merge:** `docs/p4-questbook-contract.md` "Eye T7 + T8 unit".
- **Master merge:** P4-72 ports B-71's core (`Overlay.shieldPerUpkeep`); take master's version.

**Dev-server run** (port 25581, world `p4test`, Eye T8 at 12000 100 12000 north).

**Dev-server results** (2026-10-13). The Eye was built with `/btseye t8 12000 100 12000 north gen 9 fill force` and
formed T8. The structure checks passed:

- 16 receptor ports grouped 4 rail-high, 4 rail-low and 8 halo;
- creative re-projection safe; damage and repair 1/1.

Boot: 315 recipes, 0 skipped. 10 live stars: T1-T8 plus the two planets variants.

1. **T6 → T7.**
   - A magnetar lit at T6, with the Triangulum chart and the Intergalactic core in escrow, and the galaxy rig on a 64 SM
     bench quasar.
   - The Stability Link held after about 2,500 s. The item milestones were met at about 12,000 s.
   - The rig's Transition [1] edge began the Magnetar Collapse. The assist's eruption landed 9.07 Bu/t on the collapse
     second and cancelled 76 of the 120 shock.
   - The halo was captured: COMPLETE in 779 s, T7 unlocked, the core consumed.
   - The first attempt, before the bench passed the quasar's Φ to the assist (P4-73), failed at P +102.8 as designed:
     the core was kept and the chain re-spawned the magnetar.
2. **T7 steady state.** 7,200 s: 0 failures; Ergomatter 97,000 L in 2 h (about 46,000 L/h; the design says about
   43,000); 5 minor mergers ridden.
3. **T7 → T8.**
   - The Supermassive core and the Milky Way chart; the rig at a 125 SM bench quasar.
   - The Heavy Link held (the halo test fire on Slots [8]).
   - A merger begun on a 66 SM quasar failed on Integrity (the core was kept), as §5.3 predicts.
   - The re-run chain at 125 SM: magnetar re-spawn (COMPLETE in 178 s), the collapse re-run (12 Bu/t cancelled 105 of
     120), and the Galactic Merger COMPLETE in 1,920 s, with the first pass binding the nuclei. T8 unlocked and the core
     consumed.
4. **T8 steady state.** 14,400 s from a fresh light at 130-147 SM: 0 failures, zones 94 % Resonant / 6 % Stable,
   mean Y 1.24, Superdense Ergomatter 90,800 L (about 22,700 L/h; the design says about 20,000), 5 of 5 satellite
   crossings caught (+300 L each).
   - An earlier T8 run was lost to P −100 when the bench owner's wireless network ran dry (r = 0, the upkeep sag): the
     designed failure.
5. **Planets mode.**
   - `mode planets` on the Milky Way chart lit "Milky Way: 6 planets". The slot bus took Moon, Mars, Phobos, Io, Titan
     and the Deep Dark, one per slot. Slots waited while their H/He × 128 was not buffered (Slots [1] on).
   - Mars finished 123 of 128 parallels at field factor 1.00 (expected about 122), and the Deep Dark ran.
   - 7,200 s, 0 failures, 89 % Resonant.
6. **Companions.** Sol and the magnetar charts in the slot bus: the galaxy re-formed in place ("companions changed").
   One hour at 140 SM with both: 0 failures, Y 1.14, a crossing caught (+450 L at f₈ 7.5).
7. **The real receptor path and Q-8.**
   - A real quasar (`/btsquasar t3`) fed the Eye's ports through `inject`: 1.50 Bu/t read as 0.60 / 0.50 / 0.40,
     "linked to 12000, 100, 12600". The quasar listed the host "Eye of Harmony, 1.50 Bu/t, sigma 1.00".
   - A quasar block removed: the quasar FROZE, and the Eye reported "PAUSED: its quasar … is not running (Q-8)" with P
     held.
   - Rebuilt: the Eye resumed.
8. **Persistence and the wrench.**
   - After a server restart the item milestones (link HELD), the escrow and a merger in progress all came back.
   - `wrench` kept T8 unlocked, planets mode and f₈ 7.5; the star went DARK (tile-only).
9. **Golden.** 78/78 and 38/38 still pass, with the B-71 port (P4-72).

**Not run:**

- no client: the GUI, the centre render's galaxies and the four harness cases, the NEI pages;
- OpenComputers in game: the calls go through `BTSComputerApi`, compiled only;
- a full in-world conduit rig aimed at the Eye (the bench's virtual quasar stands in for it).

The rig keeps stepping in real time after a run (`galaxy off` stops it); after a restart run `/btseye keep` again,
because the chunk tickets do not persist.

### Other hosts, OpenComputers and automation (questions P4-74..P4-82)

**What's built.** The KEE, the Homogeny T2 / T3 and Reality Diffraction are burst hosts (R.2, §3.1). Every P4 machine
is an OpenComputers component with rate-limited setters (§8.2). Every §8.3 action has a working route, and the
standard rig was proved unattended on a dev server: T6 → T7 → T8 on a real quasar and a real Eye (§8.4).

**Files.**

| Class | What it is |
|---|---|
| `kerr/MTEKerrEngine`, `KerrHatchElement.Receptor`, `KerrHole` | A GRB Receptor Port in a utility socket; `BurstHost` key `kerr@dim:x,y,z`, demand 0.1 / 0.25 Bu/t; `burstSigma()` (60 s mean), `bleedFactor()`, `maxSetpoint()` (768 at T2 with σ ≥ 1), `effectiveSetpoint()`; GUI link row, scanner, tooltip |
| `kerr/MTEKerrCompressorModule`, `MTEKerrModuleBase` | The DENSITY gate: `BTSRecipesQuasar.densityMinMass` (256 SM for SL-Q09 / SL-Q23) and `densityNeedsLink` (SL-Q23 needs the KEE's σ ≥ 1); a Superdense run that loses the link pauses (`btsKerrRunLink`) |
| `conduit/ReceptorHatchElement` | The shared private socket for the H/R units' reserved `g` cells (port first, old casing second) |
| `homogeny/MTEHomogeny`, `HomogenyGui` | 3 ports on the T2 stirring ring; demand 1.5 / 6; speed × (1 + 0.5·min(σ, 1)); Entanglement Medium (SL-H21) gated at σ ≥ 1 and paused below |
| `reality/MTERealityDiffraction`, `RealityDiffractionGui` | 6 portal ports; demand 24; with a port fitted, speed × min(σ, 1.5) and pause below 0.5; with none, × 1 (P4-74) |
| `integration/BTSComputerApi`, `oc/BTSEnvironments`, `oc/BTSDriver`, `oc/BTSOpenComputers` | `rateOk` (one call per 20 ticks per machine and method); `bts_homogeny`, `bts_reality`; `bts_kerr` link fields; `bts_directive.getBits`; `trim` on `bts_quasar` and `bts_eye` (P4-77) |
| `quasar/MTEQuasarEngine` | `requestTrim` (OC pulses from the feed buffer / gas sockets); `linkable()`; `benchRun` |
| `eye/star/EyeStarRuntime`, `EyeMilestones` | `coreWanted` on T6 / T7 = all but the link (`metButLink`, P4-76); `testFireWanted` at T7 also needs `linkedQuasarStream() ≥ 6` (P4-79); `requestTrim` and `StarHost.drainStock` (P4-77) |
| `eye/star/MagnetarMerger`, `eye/field/EyeTrimRule` | Master's merger Directive fix, ported word for word (P4-78) |
| `eye/EyeGui` | The Galaxy panel's "Heavy Link test fire waits" line |
| `quasar/QuasarRigBench`, `directive/DirectiveBench.EyeRig`, `eye/star/EyeGalaxyBench.conduit` | The automation bench: `/btsquasar autorig` and `ocloop` |
| `CommandBTSKerr`, `CommandBTSHR`, `CommandBTSConduit`, `eye/star/EyeStarBench` | Bench: `/btskerr receptor | occall`, `/btshr bench … receptors | inject`, `/btsconduit inject`, `/btseye star … milestones n reset | buses clear` |

**Contracts.**

- **Host-side API** (unchanged from the conduit unit): one `BurstHost` per host, `second(ports, key, label, demand,
  streak01)` once a host second; conditions and bonuses use `sigmaInForce()` (P4-89; was `sigmaMean()`).
- **OpenComputers:** every setter and action goes through `BTSComputerApi.rateOk(mte, method)`. A refused call returns
  `nil, "rate limited: one call per 20 ticks (1 s, the field's step)"`.
- **The Eye's link rules** (P4-76, P4-79) live in `EyeStarRuntime`. `GalaxyDirective` keeps the prototype's rule, so
  QuasarSim and the 38 golden cases are unchanged.
- **Master merge:** P4-78's hunks are master's (`c97a35e` / `e054e9c`); take master's version.
- **Questbook merge:** `docs/p4-questbook-contract.md`, "Hosts + automation unit" and "Generator entries".
- **Automation doc:** `docs/automation.md` §7.

**Dev-server results** (2026-10-13, port 25581, world `p4test`; quasar at 12000 100 12600, coils 3; Eye T8 structure
at 12000 100 12000; boot 315 recipes, 0 skipped).

1. **T6 → T7 → T8 unattended.** The gate rig saw only Directive codes and acted only through the machines' hatches
   (`/btsquasar autorig … eye …`). The conduit was the rig's maths (P4-75).
   - Start: a fresh 64 SM hole with no spin at f 1.0, and a lit magnetar with T6 unlocked. Milestones were reset, and
     the escrow, plinth buses and debug rig were empty or off.
   - **T7 at t+14,555 s.** The core went in on Transition [4], the test fire held the Stability Link, and the chart went
     in on Transition [1]. The Begin edge started the collapse, and its eruption landed 9.97 Bu/t (85 of 120). The
     collapse was COMPLETE in 779 s.
   - **T8 after about 52,600 s at T7.** The quasar grew by itself from 80 to about 135 SM. The test fire waited for
     6 Bu/t. The Heavy Link was held at about 135 SM, and the Galactic Merger was COMPLETE in 1,920 s.
   - Then about 41,600 s at T8 with the quasar growing to 222 SM: 14 of 14 crossings caught.
   - 0 Eye failures and 0 disc blow-outs end to end.
2. **Found and fixed on the way:**
   - the core / link deadlock (P4-76);
   - the T7 test fire starving the rail at 88 SM (P4-79);
   - the merger re-spawn failing 344 times (P4-78);
   - `autorig off` leaving stale bursts;
   - the Stability Link needing f 1.0 (P4-81).
3. **Real redstone.** After a run the rig acts in real time with redstone blocks on the Burst Control Hatches. The
   Eye's bursts followed J1 second by second, with the field Resonant.
4. **Hosts through real ports** (P4-74):
   - **KEE:** σ 2.0, bleed × 0.25, cap 768, setpoint 700 kept. With the link off it was held at 512.
   - **Homogeny T3:** σ 1.5, speed × 1.5, Entanglement Medium open. It closed when the mean fell.
   - **Reality Diffraction:** σ 0.5 gave × 0.5, and σ 0.25 paused it. With the quasar frozen it said "PAUSED (σ held)",
     and it resumed when the quasar was rebuilt.
5. **OpenComputers.**
   - `ocloop`: getters, clamps, `trim` (clamped and taken the next second), and the rate limit (the second call in a
     tick is refused).
   - `/btskerr occall … setSetpoint 700`.
6. **Golden.** 38/38 and 78/78 still pass.

**Not run:**

- no client: the GUIs, including the Galaxy panel's new line;
- no OpenComputers Adapter in game;
- the SL-Q23 recipe in the Compressor Module (the gate is code-checked);
- a 16-port in-world conduit aimed at the Eye.

**Commits:** ef77c88, 93e992f, cabab76, 0a02809, d6652e0 (the first attempt, cut off at 11:39), then 9c58843,
bbcac79, 4bf6030, 8bdb47d, f9e5c2f, 9d0360f, aadf752.

### Full RC-2 dev-server test (questions P4-83..P4-87)

**How.** `runServer` on port 25581, five boots (world `p4test`, plus one boot on a world made by the pre-P4 base
c8f8ca7). The console was fed through `run/server/cmds.txt`. No client was run.

**Load (every boot).**

- "Recipe registration complete: 315 registered, 0 skipped".
- No `com.nateyburr` stack trace in any log.
- The only placeholder is `btswhiteholematter`.
- No H/R gate line names Eternity any more; the eight left all wait for White Hole Matter (Eye T9, P5).
- SL-B01 / B02 (×32), SL-Q26 and BHC → Kerr all logged as before.
- The 329 GregTech `NullPointerException` lines and the coremod's `ArrayIndexOutOfBoundsException` are the dev
  environment's: the pre-P4 jar logs the same counts.

**Static checks.**

- `quasar-golden --check --java`: 38 / 38.
- `fieldsim-golden --check --java`: 78 / 78.
- `export-structures --check`: matches voxel.js fa6fec2a751c.
- `gen-sl-textures --check`: 123 / 123.
- `DirectiveRigSim`: ALL PASS.
- `QuasarRigSim`: PASS. T6 → T7 and T7 → T8 were each reached 5 of 5 times, with 0 Eye failures and 0 blow-outs.
- Lang lint: every recipe-result id has its RC-2 key, and every item, block, recipe map and GT5U key resolves (P4-86).
- `/btseye gates`: compression generators T1-T9 CLEAN. P4 audit: 0 of 27 parts break the rule. BHC consumers left: 0.

**Structures** (hologram creative and survival; every one FORMED, then a creative re-projection over it that broke
nothing, then a damage-and-repair check that restored the cells):

| Machine | Creative | Survival (blocks placed) |
|---|---|---|
| Quasar (coils T1-T4) | N, E, S, W | N, E, S (51,983 each) |
| Wormhole Mouth | E, W, down | N, S, up (1,403) |
| Conduit Mirror | E, W, down | N, S, up (1,702) |
| Conduit Junction | E, W, up | N, S (17,232) |
| Crown Mk I / II / III | E, W each | N, S each (1,834 / 2,810 / 4,421) |
| Eye T7 | W | E (67,366) |
| Eye T8 | N, N with gen 7 | S (71,075) |
| Eye T6 | E (legacy: no D-49/P4 hatches) | S |

**Quasar.**

- Open refused without a seed. A seed in the slot opened a 64 SM hole, with first light about 20 s later.
- 1 h fast-forward: 34 eruptions.
- Purge was refused under Φ 20 and requested at Φ 48. A redstone block on the Burst Control Hatch purged on the rising
  edge.
- f 1.5 gave an over-Eddington blow-out; back at f 0.85 it re-formed.
- Collapse → DORMANT. Re-opening without a Black Hole Seed was refused.
- Restart: mass, spin, feed, counters and the dormant state all kept.

**Conduit and crowns** (`/btsconduit rig`):

- The top jet went to a Crown Mk I, which put 1.18e13 EU/t onto wireless.
- The bottom jet went to Mouth A → Mouth B → Mirror → Junction split (0.53 / 0.53 / 0.12) → Mirror → the remote crown's
  receptor, which was linked.
- All elements opened after 10 s of SpaceTime upkeep.
- After a restart the rig re-formed and carried on.

**Eye T6 → T7 → T8 unattended** (`/btsquasar autorig … eye …`, a fresh 64 SM quasar at f 1.0, an Eye built by the
survival path):

- The Stability Link was held, and T7 came at t+2,727 s.
- The test fire waited for the quasar to grow. The Heavy Link was held at about 136 SM, and the Galactic Merger was
  COMPLETE in 1,920 s: T8.
- 0 Eye failures and 0 blow-outs.
- Companions: Sol and the magnetar re-formed the galaxy, with 0 failures.
- Planets mode: 6 slots; Mars and the Moon ran ×128, and the rest waited for H/He and EU, as built.
- The whole state (mode, slots, progress, milestones) survived a restart.

**Hosts.**

- KEE: σ 2.0 gave bleed × 0.25, cap 768 and the setpoint 700 kept. With no link it was held at 512.
- Homogeny T3: σ 1.5 gave speed × 1.5 and Entanglement Medium open (after P4-83).
- Reality Diffraction: σ 0.5 gave × 0.5, and σ 0.25 paused it.
- SL-Q23 Superdense refinery in the Compressor Module (M 700, σ 2): it ran, paused at 1,703 / 12,000 when the link was
  cut, and resumed when it came back.

**Directive codes seen per state:**

- Disc: 1, 8, 9, 10, 13, covering E low, E high, purge now, the alarm with no hole, a vent or a blow-out, and their
  combinations.
- Eye T8 sets: Field, Events, Galaxy Transition, Trims, Status, Transition and Slots. Slots read 1 (a slot idle) in
  planets mode. Galaxy Transition's Ready / Running edges were taken by the rig.
- Kerr: Hole 0 / 8 and Status 0.
- Eye T1-T6: the Trims / Status / Transition bit-seconds.

**OpenComputers.** OC was loaded, with 13 components. These were exercised:

- `bts_quasar` ocloop: the getters, the clamps, setField after P4-84, trim, the rate limit and purge refused.
- `bts_eye` ocloop: the getters, setDial and setFeed clamps, trim, and EYE_DIAL 1-15.
- `bts_kerr` ocloop / occall: setSetpoint clamps to the cap in force, and collapse / open with and without items.

**Unattended rigs.**

- Quasar autorig, binary and analog (FIELD-mode b).
- Eye autorig T1, T3, T4, T5 and T6, 7,200 s each: 0 stars lost.
- Kerr autorig 3,600 s: never closed.

**A pre-P4 world.** The base jar (c8f8ca7, 282 recipes) built a Kerr T2 with both modules running, an Eye T6 with a lit
magnetar mid-transition (its own Sensor 5 / Directive 3 layout), a Homogeny T3 and a Reality Diffraction. On the P4 jar
all of them re-formed with their state:

- the Eye kept unlocked T6 and the magnetar's TRANSITION; it was grandfathered with no receptor ports;
- the Kerr kept its hole at 128 SM;
- the module's recipe carried on at 1,248 / 2,000;
- the H/R machines re-walked FORMED; Reality reads × 1 with no port (P4-74);
- a GRB Receptor Port fitted into the old Kerr's utility socket.

**Fixed (each committed):**

- P4-83: a paused remembered source no longer pauses a re-aimed host for ever.
- P4-84: the b setting clamps to the coil tier.
- P4-85: the P4 machines mark their chunk modified once a second. Before this, a mouth and a crown reloaded a 20-minute
  old save.
- P4-86: the Casimir Plate tooltip.

Bench: `/btsconduit survival … [facing|up|down] [creative]`, and `/btskerr module … stock` / `clear` for circuit inputs
and fluids.

**Not run** (no client, per the build notes):

- the GUIs, the TESRs and the render-harness cases;
- the NEI pages and the NEI multiblock preview;
- OpenComputers through a real Adapter (the components were exercised through the same API);
- re-opening a dormant quasar with a real Black Hole Seed item;
- a cross-dimension mouth pair, and a data-stick pairing by hand.

### Review fixes (questions P4-88..P4-94)

Seven review findings, all confirmed against the code and all fixed. Each fix is its own commit.

| # | Finding | Fix | Where |
|---|---|---|---|
| P4-88 | The T7 Heavy Link test fire had no ceiling. With Mk II throats, a quasar above about 300 SM breached Triangulum. | Slots [8] needs a mean stream of 6-32 Bu/t. Above that, the Galaxy panel and OC `testFireShare` give J1's Eye share (16 Bu/t ÷ stream). | `EyeStarRuntime`, `EyeGui`, OC, Directive tooltip |
| P4-89 | Q-8 held σ on the non-Eye hosts, so a frozen or unloaded quasar kept every bonus for free. | `BurstHost.sigmaInForce()` is 0 while paused, so link work pauses on the KEE, Homogeny and Reality. | `BurstHost`, `MTEKerrEngine`, `MTEHomogeny`, `MTERealityDiffraction` |
| P4-90 | After a lost link, the modules kept a 768 SM parallel for hundreds of hours. | Module parallel uses min(M, the cap in force). | `MTEKerrModuleBase` |
| P4-91 | NaN passed the quasar and Junction setters and corrupted the disc and the streams. | Setters ignore non-finite values; NBT loads heal them. | `MTEQuasarEngine`, `MTEConduitJunction`, `MTEConduitElement` |
| P4-92 | After a restart, a host read one empty second and lost its link streak. | A 200-tick warm-up after a load: a remembered source that has not arrived pauses the host. | `MTEHatchGRBReceptor` |
| P4-93 | `ConduitNet` / `BurstDocks` pinned unloaded worlds. | The entries hold weak references. | `ConduitNet`, `BurstDocks` |
| P4-94 | A misaligned dock lost the jet silently. | `IBurstDock.REFUSED`: the jet is counted as vented. | `IBurstDock`, mouth, crown, quasar, GUI, OC |

**Checks.**

- `spotlessApply compileJava` passes.
- `quasar-golden --check --java` passes 38 / 38, and `fieldsim-golden --check --java` passes 78 / 78.
- The P4-88 evidence is the prototype harness, the T7 rig with Mk II throats: 0 failures up to 265 SM, and breaches
  from 300 SM.

**Dev server** (port 25581, three boots). 315 recipes registered, and no `com.nateyburr` stack trace.

- **KEE T2 at 700 SM, quasar link σ 2.00:**
  - with the link: cap 768, SL-Q23 running, bonus parallel 3,328;
  - quasar frozen: PAUSED, bleed ×1, cap 512, SL-Q23 held at 10,525 / 12,000, bonus 2,816;
  - quasar repaired: everything resumed within 13 s.
- **Setters:** `feed NaN` and `b Infinity` were ignored, and the disc was unharmed. On a Junction, NaN c and p3 were
  ignored.
- **Restart:** the port read PAUSED about 10 s, then "not linked". With an injection running, the link was there from
  the first check.
- **Misaligned dock:** an upright Crown Mk I under a bottom cap reads "venting (Crown Mk I misaligned)".

**Not run.**

- An Eye link streak across a real restart.
- The ConduitNet rig after the weak-reference change. Only a BurstDocks lookup was exercised.
- The T7 test fire in game. The ceiling rests on the harness.

---

## SMBH v1: the Supermassive Lattice (structure, content, recipes, helpers)

**Design of record:** design repo `designs/docs/smbh-v1.md` (revision 3). Branch `smbh`, worktree `addon-smbh`; the
engine (§10.1) and the max-quasar fixes are B-150; this unit's judgement calls are B-151..B-155 in
`singularity-ladder-questions.md`.

### Shapes

- `quasar_lattice` (piece of the Quasar Engine, MTE 28120; 179 × 101 × 179, the quasar and lattice together
  179 × 161 × 179): 8,436 Lattice Frame Casings `L` and 48 Containment Nodes `N`. An equatorial band at r 87.5-89.5
  (3 tall), two latitude rings at ±35° (r 71.5-73.5 at y ±49..50), 24 radial struts at y 0 onto the outer wall's 24
  buttresses, 8 meridian ribs; 16 nodes per ring at 11.25 + 22.5k°, on each ring's outer face. Disjoint from `quasar`;
  crowns Mk I-IV docked (and stacked) on both ports never touch it; the controller's front stays open.
- `crown_mk4` (81 × 35 × 81): the Mk I-III crown continued (h 32, ro 40, 32 petals); its thermaliser cells are the new
  Event Horizon Thermaliser Casing `X`. 6,418 blocks.
- voxel.js has neither, so `tools/export-structures.js` draws both in memory (two block kinds appended last, a fourth
  `CROWN` entry, `smbhLattice()`); every other shape is byte-identical and `--check` passes.

### Blocks and items (§8.1)

| Id | Block / item | Role |
|---|---|---|
| `slCasing4:0` | Lattice Frame Casing | lattice char `L` |
| `slCasing4:1..4` | Containment Node L1-L4 | lattice char `N`, `ofBlocksTiered` on channel `lattice`; light 7 |
| `slCasing4:5` | Event Horizon Thermaliser Casing | crown_mk4 char `X` |
| `conduitThroatMk4` | Conduit Throat Mk IV | throat tier 4: 524,288 Bu/t; upkeep 2 L/s Superdense Ergomatter (preferred), else 16 L/s Ergomatter or 256 L/s SpaceTime |

Textures: `tools/gen-sl-textures.js` (`sl_latframe`, `sl_node_l1..l4`, `sl_ehthermaliser`, `conduitThroatMk4`), the
quasar's BHC family with each tier's colour. No new MTE id, fluid or recipe-result id.

### The walk (`MTEQuasarEngine`)

- `checkMachine` walks `quasar`, then `quasar_lattice`. A whole lattice of one node tier sets `latticeBuilt`; it is in
  force (`latticeTier`, what the engine reads) only after **Saturation** and only on MagMatter coils.
- **Saturation (§1.2):** 4,096 SM on T4 coils, the hole open, |E| ≤ 40, 3,600 disc seconds unbroken. Saved with the
  hole (`saturated`, `satStreak` in `btsQuasar`, so it survives a wrench). The scanner shows the streak.
- A lattice swapped to a lower tier is a whole lattice: the engine sheds to that tier's top (§2.5). An **incomplete**
  lattice under a hole over 4,096 SM unforms the quasar (error `beyondthestars.structure.quasar.lattice` plus the first
  wrong cell): the hole is frozen, nothing is lost. To take a lattice down, lower the setpoint to 4,096 SM first.
- The chunk box includes the lattice once one stands (or the hole needs one).
- Hologram / survival / NEI: the `lattice` channel (1-4) adds the lattice at that node tier; with no `coil` channel the
  coils are built at T4. NEI's preview finds the channel through `withChannel(ofBlocksTiered)` (BlockRenderer6343's
  `StructureHacks` / `ConstructableData.channelMaxTierMap`), as the coil channel.

### Crown Mk IV and Throat Mk IV

- `MTEBurstThermaliserCrown.PIECES` gains `crown_mk4`; `MK_MAX` = 4; the biggest crown that stands wins.
- `MTEConduitElement.upkeep`: a Mk IV throat burns Superdense Ergomatter at 2 L/s whenever the fuel mode allows the
  Ergomatter family and the hatches hold enough (counted with the Ergomatter litres).

### Helpers

- `/btskit lattice [1-4]`: 8,436 frames, 48 nodes of the tier, a crown controller with the Mk IV parts, a Throat Mk IV.
- `/btsquasar [t1..t4] [l1..l4] [saturate] ...`: the quasar with the lattice (creative or `survival`), and Saturation
  set by command.
- `/btssmbh` (op only): `bench [x y z [facing]]` (L4 lattice, Saturation, a 4,096 SM hole, every upkeep hatch filled,
  Mk IV crowns on both ports, chunks kept), `mass <M>` (clamped; the setpoint follows), `fill` (the trims get Molten
  MHDCSM, Molten Universium and Primordial Matter, never Spatially Enlarged Fluid, which would be a Space trim every
  second: B-160), `drain anchor`, `saturate on|off`, `unbind on|off [force]`, `damage`, `status` (ends with the
  bench owner's wireless total, a BigInteger: B-162), and (controller unit) `overdrive <o>`, `setpoint <M>`, `shed`,
  `gui [info]`, `oc`.

### Dev-server test (port 25651, world `smbh-test`, five boots)

- Boot: `Recipe registration complete: 371 registered, 0 skipped` (364 before: +7, SL-Q29..Q35).
  `/btsrecipes dump`: all seven rows in "Quasar / Conduit", SL-Q31..Q33 marked placeholder.
- Forms by command at every tier: L1, L2, L3, L4 (creative, swapped in place over one quasar), each "FORMED, lattice Ln
  (in force)" after `saturate`; the re-projection keeps it formed; the damage-and-repair check passes.
- Survival auto-place: quasar + L1 lattice, 60,467 blocks in 304 rounds, FORMED; `crown4` survival 6,417 blocks, FORMED
  Mk 4.
- Clamps: L1 65,536, L2 262,144, L3 4,194,304, L4 67,108,864 SM.
- Natural Saturation on a real disc (f 1.0, 3,700 s bench run): the milestone landed and L1 came into force; at f 0.85
  unattended the eruption kick (E −30 from about −16) breaks the streak, so Saturation wants a trimmed or purged disc.
- L1 at 60,000 SM: hold-up 100 %, x 0.94, output ×3.82, both Mk IV crowns took 65,958 Bu/t.
- One frame block removed under the 60,000 SM hole: NOT FORMED, "Lattice INCOMPLETE", M kept; rebuilt: FORMED.
- L4 → L2 swap at 67 M SM: cap 262,144, shedding (and frozen, not shed, while a lattice chunk was unloaded).
- Unbound L4 at 134,217,728 SM (n 1): anchor paid; `drain anchor` (131,002,368 L): "SHEDDING (anchor short)" down to
  67,108,864 SM, then hold-up 100 % again. A Mk IV crown taking 74,599 Bu/t pushed 7.46 × 10¹⁷ EU/t to wireless.
- `/btseye gates`: Lattice Frame Casing and Containment Node L1 CLEAN for the Eye T10 gate.

### Controller (§10.2) and "Shown in EU/t" (§10.3) (question B-156)

- **Accretion Buffer** (`MTEQuasarEngine.accretion()` / `bufferCap()`): 60 s of Disc (f·Ṁ_Edd, at least the re-form's
  0.3) + Hold-up (3·x·Ṁ_Edd), plus one second of Growth room, in a long. The Growth segment takes only what is over a
  59 s reserve, so it drops out first when the feed runs short. After each second the buffer refills **every tick**
  while the rim hatches give (`refillMore`), each drain one FluidStack (at most 2³¹ − 1 L). Without a lattice the buffer
  is 60 s of the disc (it was 10 s of 1.5 x the feed).
- **Setpoint, Overdrive, Unbind, Shed:** `setMassSetpoint`, `setOverdrive` (5 % steps), `unbind(on)` (L4 in force and
  `smbhAllowUnbound`, new config key, default true; off is always allowed and sheds to the L4 top), `shedToSafe()`
  (setpoint to the safe limit). A Burst Control Hatch in Hold / Overdrive mode overrides the GUI's value while it is in
  that mode (`massSetpointInUse`, `overdriveInUse`).
- **Link-source record:** every `JetPacket` and `BurstStream` carries `lattice` (in force) and `mass`;
  `BurstStream.supermassiveSource()` and `MTEQuasarEngine.supermassiveLinkSource()` say whether the source can serve
  P5's Supermassive Link (a lattice, M ≥ 65,536 SM, linkable). The streak and the Eye side are P5's.
- **GUI** (`MTEQuasarEngineGui`): the Lattice section (Saturation or "Reach Saturation first", band x with the setpoint
  tick, the intake as a stacked bar, `BTSRows.Kind.STACK`, with the 5x cap tick, the split line, the leak, the buffer,
  Overdrive and its upkeep, Unbound and the anchor, a shed); the mass bar goes log with a lattice; the Supermassive
  Lattice side panel (gap-row button left of Disc control): log setpoint slider + whole-SM field, Overdrive slider, Shed,
  two-click Unbind / Bind. Info page paragraphs EU/t, Lattice, Intake, Overdrive, Unbound, Automate the lattice.
- **Automation:** Directive set `QUASAR_HORIZON` (`QuasarDirective.horizon`), 18 appended sensor channels, Burst Control
  modes HOLD / OVERDRIVE / SHED, OpenComputers `getBand / getIntake / getHoldUp / getBuffer / getOverdrive / getAnchor /
  getHorizon / setMassSetpoint / setOverdrive / setUnbound / shed` (docs/automation.md §8).
- **EU/t beside every Bu/t:** one formatter, `BTSFormat` (`buEu` "9,796 Bu/t (9.8e16 EU/t, 98 quadrillion)",
  `buEuShort`, `euOfBu`, `euValue`, `euWords`, `buCompact` for GUI rows). Used by the quasar, crowns, conduit elements,
  the receptor, every host's link line, the Eye (bursts, link, weak-quasar row, galaxy panel, scanner, NEI), the
  Kerr / Homogeny / Reality lines and tooltips, item tooltips, NEI (+ a Mk IV crown page: 372 registered), quest text
  (`questbook/singularity.json`, regenerated) and the OC docs.
- **Bench verbs** (`/btssmbh`): `overdrive <o>`, `setpoint <M>`, `shed`, `gui [info]` (the GUI rows as text, built as the
  GUI syncs them), `oc` (the OpenComputers getters); `unbind on|off` goes through the GUI's checks (`force` sets it raw);
  `fill` re-walks after placing trim hatches.
- **Tested** (dev server, three boots, 372 registered / 0 skipped, 0 exceptions after boot; dev client, GUI shot
  harness, five passes): a plain T4 quasar at 4,096 SM (Saturation row, buffer 59 s, EU/t everywhere); L1 at 30,000 SM
  (intake at the cap: Disc 6.0M, Hold-up 7.9M 100 %, Growth 16.2M L/s, +16 % per hour; Unbind refused on L1; Overdrive
  idle with a note); L3 at 262,144 SM, Overdrive 50 % (25,000 L/s each paid, x34.1); Unbound n 1 with full Overdrive
  (anchor 4,096 L/s, upkeep 50,000 L/s each, x98.9, intake 80 % of the cap); the anchor cut (Horizon bit 4, a safe shed);
  a shed on L1. Shots and text dumps: local work folder `smbh-ctl/` (`shots/`, `ctl-s345.txt`, `ctl-s7.txt`).

## SMBH v1: the Eye side, quests and P4-88 (questions B-157..B-159)

### The Supermassive Link's data (§1.3; B-157)

- **Record:** the GRB Receptor Port keeps, per source and second, the tightest Junction out-1 limit and the source record
  (lattice in force, mass) of the streams it took (`BurstReading.limitBySource` / `recordBySource`); the Eye copies
  the link source's into `EyeBursts.limit / smLattice / smMass` (`supermassiveSource()`). The bench injectors (`/btseye
  star ... inject`, `/btsconduit inject`) now copy their quasar's record too.
- **Streak:** `EyeGalaxy.link9` (tile NBT `link9`), rolled every field second (`EyeStarRuntime.supermassiveSecond`):
  `BurstLink.supermassiveOk(linked, source)` = linked, the source qualifies, the window full, mean >= D10 (96 Bu/t).
  Held after 1,800 s; `EyeMilestones.smLinkDone / smLinkBest` (item NBT `t9`) for P5. Nothing gates on it.
- **Shown:** the Galaxy panel's "Supermassive Link (P5)" check row (`EyeGui.supermassiveRow`), a scanner line
  (`supermassiveLinkLine`), OpenComputers `getUnlock().supermassiveLink`.
- **SL-Q36** (`BTSGodforgeMagMatter`, load-complete, `replaceTecTechEyeOfHarmony` only): the Godforge EE upgrade's 2
  Universium frame boxes -> 8 Black Dwarf Matter frame boxes, in place. The gate audit reads MagMatter's tier from EE's
  live cost (`SLGateDump.magMatterTier`: T7 after SL-Q36) and gates the T4 Disc Field Coil at T10.

| ID | Thing | Where | Note |
|---|---|---|---|
| SL-Q36 | Godforge EE upgrade cost (MagMatter mode) | load-complete rewrite | 2 Universium frame boxes -> 8 Black Dwarf Matter (B-153, B-157) |

### Quests (§8.3; B-158)

17 quests under feature `smbh`: UXV 48176-48179, MAX 48195-48199 (Supermassive Link and White Hole Lattice are
`eyeT9T10` placeholders; Eye T10 now needs the Supermassive Link), The Endgame 48221-48228. Bills: `quasar_lattice`
(roles `lattice_frame`, `containment_node`) and `crown_mk4` (`horizon_thermaliser`) in `bomItems`. The Empyrean / Omega
quests name the multiblock engraver (question 25); the circuit config comments (question 26). Regenerated with
`gen-bom.ps1`; `--check` and `--selftest` green.

### P4-88: the Junction's out-1 limit (B-159)

- `BurstRouting.split(in, share1, p3, limit)`: share 1 of at most `limit` Bu/t, the rest to out 2 (no limit, or the rest
  under it: the old split). `STANDARD_J1_LIMIT` = 7.5 Bu/t (5 x D7) per parallel, x the time dial
  (`EyeStarRuntime.standardJ1Limit()`).
- `MTEConduitJunction`: `limit` (NBT `limit`), `getLimit / setLimit` (0..Mk IV's cap, non-finite ignored), the scanner
  line, the GUI row's tooltip, the split panel's "Out 1 limit, Bu/t" field (`jLimit`), OC `setLimit`, `getFlow().limit`,
  `/btsconduit junction x y z c k p3 [split|combine] [limit bu]`. Out 1's streams carry the limit
  (`BurstStream.withLimit`).
- The T7 test fire's size is `testFireStream()` = min(the linked quasar's mean stream, J1's limit); the 6-32 Bu/t
  window and the Galaxy panel's notes use it.
- Benches: `/btsquasar autorig ... [mk 1-4] [limit bu|off|std]`, `/btseye star ... galaxy ... [mk n] [limit ...]`
  (`EyeGalaxyBench.conduit` models the throat Mk and the limit; default Mk I and the standard limit).
- **Tested** (dev server, port 25651, two boots, 372 registered / 0 skipped, no exception from our code): Mk II, a
  real coils-T3 quasar at 310 SM and a real T8 Eye lit as Triangulum: no limit, Triangulum lost in its first second;
  the standard limit, 1,800 s + 20,000 s with 0 failures, the Heavy Link held and the Galactic Merger complete (T8), and
  2,458 game-s live on real redstone under `/btswarp 20` with no failure. Real elements: a Junction at limit 0.05 split
  0.80 Bu/t into out 3 0.08 / out 1 0.03 / out 2 0.70. `/btseye gates`: everything clean, MagMatter at T7. The
  Supermassive Link row read an injected L1 hole at 65,536 SM as qualifying. Dev client: the Junction panel's field.

## SMBH v1: end-to-end test (questions B-160..B-162)

- **Build and checks:** `gradlew build` green; goldens 30 / 38 / 78; questbook `--check` / `--selftest` green.
- **Headless** (port 25651, the RC-2 pack's DefaultQuests copied read-only for the run): 372 registered / 0 skipped with
  defaults and with `smbhAllowUnbound` + `replaceTecTechEyeOfHarmony` off; `/btsrecipes status` 507 rows;
  `/btsquests verify` 159 / 161 (the known `eternalsingularity` misses); `/btseye gates` clean (with the EOH flag off
  the audit's own premise does not hold: B-162). Unbind refused with `smbhAllowUnbound=false`, a forced one sheds back.
- **Dev client** (silent, GUI shot harness): a 4,096 SM quasar, Saturation by the engine, L1 in place, growth to 16,384 SM
  on real ticks under `/btswarp 100` matching the growth law, the band and the intake bar, the under-fed sequence
  (Growth first, the buffer, then the leak and Horizon bits 1 / 0), L3 Overdrive (x34.15 / x56.04 at 25,000 / 100,000 L/s
  each), L4 Unbound n 1 (upkeep halved to 50,000, anchor 4,096 L/s), both Mk IV crowns at their cap with the wireless
  total equal to their lifetime counters, the state through three restarts, the anchor cut.
- **Fixed:** B-160 (`/btssmbh fill` and Space fluid), B-161 (GUI rows cut at supermassive sizes).

## P5 content: the Great Attractor's matter, MAX and UXV parts (questions B-200..B-216)

Design of record: design repo `designs/docs/p5-t9-t10.md` (D-52, D-53; Q-P5-1..13 accepted 2026-10-16). This
section is the content unit's contract; the Eye machine code (T9 / T10 fields, the Pellet Injector Hatch, the
structures) is the Eye unit's.

**Fluids** (`SLRealityFluids`): `darkMatter` = `btsdarkmatter` "Attractor Dark Matter"; `entangledDarkMatter` =
`btsentangleddarkmatter` (144 L condensate, SL-C04). `btswhiteholematter` / `btsentangledwhitehole` are retired (B-200).

**Items** (`items/SLP5Items`, registry name = field name): `gatedPelletMagnetar` / `gatedPelletErgomatter` /
`gatedPelletSuperdense` (1.25e9 / 5e9 / 2e10 GeV: `pelletGrade`, `pelletGeV`), `injectorBarrelMk1..3` (4 / 8 / 16 a
second: `barrelTier`, `barrelRate`), `attractorSeedCore` (T8 -> T9), `starChartGreatAttractor` (T9 target),
`attractorDarkMatterPlate`, `attractorDarkMatterDensePlate`, `superconductorWireUXV`, `superconductorWireMAX`,
`maxBattery` (`ItemMaxBattery`, IC2 electric, tier 14, 2^59 - 1 EU: B-208).

**Blocks:** `slCasing4` 6 `UNIVERSAL_COMPRESSION` (compression tier 10), 7 `DARK_MATTER_COIL` (`IHeatingCoil`,
`HeatingCoilLevel.MAX`), 8 hole, 9 `DARK_MATTER_FRAME`; `slGlass` (`BlockSLGlass`) 0 `MAGNETAR_UXV` (glass tier 13), 1
`DARK_MATTER_MAX` (tier 14).

**MTEs** (`machines/BTSMaxHatches`, B-204): base+33 MAX Energy Hatch, base+34 MAX Dynamo Hatch, base+36 MAX 4096A
Laser Target Hatch, base+37 MAX 4096A Laser Source Hatch. base+28 (the Pellet Injector Hatch) is the Eye unit's.

**Recipes** (`BTSRecipesP5`, line "p5"): SL-F01..F03 pellets (Kerr DENSITY), SL-F04..F06 barrels, SL-F07 Attractor
Seed Core, SL-F08 the T9 chart, SL-F09 the Pellet Injector Hatch (waits for its MTE); SL-M01..M03 plate, dense plate,
frame box; SL-M04 MAX wire; SL-M05 MAX glass; SL-M06 coil; SL-M07 battery; SL-M08 / M09 MAX casing and hull
(`Casing_MAXV`, `Hull_MAXV`); SL-M10..M17 the eight MAX components (Component Assembly Line, `COAL_CASING_TIER` 14);
SL-M18 the MAX CoAL casing; SL-M19..M23 MAX energy, dynamo, wireless and laser hatches; SL-V01 UXV wire; SL-V02 UXV
glass; SL-V03 / V04 UXV 4096A laser hatches. Reality: SL-R06 Universal Compression Field Generator (Refab). Changed:
SL-C04, SL-H20, SL-H21, SL-R01..R04 (Attractor Dark Matter; UMV x2 for UXV parts, B-205); SL-Q31 / Q32 behind
`latticeNodesUseP5Matter` (B-213).

**Checks:** `/btseye gates` adds a P5 audit (51 parts; T8 -> T9 parts gate 9, T9 -> T10 and MAX parts gate 10, the UXV
leftovers gate 7) and prints each part's registry id. Boot: 408 registered, 0 skipped (B-215).

## P5: the Eye machine, T9 the Great Attractor and T10 the Universe (branch p5-eye)

**Design of record:** `designs/docs/p5-t9-t10.md` (approved 2026-10-16, Q-P5-1..13 on their recommendations). This
branch is the Eye's machine side; the content (fluids, pellets, recipes, questbook) and the client renders are their
own branches. Judgement calls: B-169..B-177 in `singularity-ladder-questions.md`.

**Engine** (pure, the B-37 discipline; `node tools/p5-golden.js --check --java`, 46 cases bit for bit):

| Class | What it is |
|---|---|
| `eye/field/AttractorField` | T9: the pellet beam (rate law, 20 s spool, ρ = B / 10¹⁰ GeV/s, hold rating), Galactic Flow (warning, accept or pass, the 240 s surge, capture), the drift and burst terms of P, W, Λ, V |
| `eye/field/UniverseField` | T10: the universe batch, dark energy's drift, Recombination, Cosmic Dawn, the last light and the collapse salvage |
| `eye/field/InfallOverlay`, `InflationOverlay` | The two transitions, with the soft fail (Q-P5-5) inside the field (`StepResult.recovered`) |
| `quasar/engine/DeepLink` | The T8 → T9 machine milestone (the Eye's total, its collar part, the beam's ρ) |
| `directive/P5Directive` | The Flow and Universe codes |
| `TierParams.T9`, `PLANETS9`, `T10`; gauges V = 6, H = 7 (`GAUGES` 8) | `GalaxyField.Companion` gains Triangulum and the Milky Way (T9 only, weight 2 in the demand) |

**Runtime.** `EyeTransition.Type` INFALL (8) and INFLATION (9) with `Infall` / `Inflation`; `coreFor(8)` Attractor
Seed Core, `coreFor(9)` Universal Upgrade Core; `MAX_UNLOCK` 10 (config `enableEyeT9T10` caps at 8);
`EyeMilestones` t8 (Deep Link, 108,000 L Superdense Ergomatter all-Stable, 2 h Resonant, 4 crossings) and t9
(Supermassive Link, 60,000 L Dark Matter, 2 h Resonant, 8 flows held), plus the display counters (galaxies, universes,
dawns); `EyeAttractor` (the beam's inputs, the pellet carry, the Deep Link window, the universe charge owed, the accept
limit). Star definitions `star:attractor`, `star:planets9`, `star:universe`.

**Structure.** Pieces `eye_jet9` (5 collar ports `r`, 2 injector sockets `i`) and `eye_crown` (12 crown ports `k`,
Eternity Shell `E`); `PIECES` and `MAX_TIER` 10; compression tier 9 = TecTech Gallifreyan, tier 10 = slCasing4:6
(walked as meta 9). Receptor groups collar (3) and crown (4) from the manifest.

**Hatches.** Pellet Injector Hatch (MTE 28128, `MTEHatchPelletInjector`); the Burst Control Hatch's Pellet beam mode.

**Automation.** Directive sets EYE_FLOW, EYE_UNIVERSE; sensor channels V, H, BEAM_RHO, BEAM_GEV, HOLD_RATING, FLOW_SIZE,
GALAXIES, UNIVERSE_AGE, DEEP_LINK_STREAK; `bts_eye` getBeam, setBeam, setGrade, getFlow, setAcceptLimit, getUniverse
(and `trim("dm" | "spacetime", n)`); `bts_injector` getStock, getRate.

**Render contract.** `EyeStarLook.ATTRACTOR` (12) / `UNIVERSE` (13) and `EyeP5Look` (synced in the render tile's look
compound), as agreed with the render unit.

**Bench.** `/btseye t9|t10 <x> <y> <z> …` builds with the collar / crown ports, the injector (a Mk I barrel, 960
Superdense pellets) and its Burst Control Hatch, wired to a V sensor (analog, window 30) over GT wireless covers
(B-225); add `fill stdrig` for an Eye that runs unattended through restarts on the saved standard rig (`EyeStdRig`: a
bench quasar, the trim valves, the stock; a T9 lit at once, a T10 begins its own first universe). `/btseye star <x>
<y> <z> p5 status | pellets | refill | accept | beam | deeplink [reset] | flow | uni | stdrig on [mass M]|off|status |
unwire`; `core attractor|universal`, charts `attractor`, `universe`. The galaxy rig routes the Deep Link's test fire to
the collars and T10's bursts to the crown. The dev-server runs are B-177 and B-225.
