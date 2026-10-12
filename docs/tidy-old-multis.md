# Tidy pass: the older multiblocks (presentation and UX only)

Branch `tidy-old-multis`, worktree `GTNH-Projects/addon-tidy`. Planned 2026-10-12 from ~130 audit
findings, deduplicated to the work list below. User request: "review old stuff for a tidy such as
cleaning up the GUI, tooltip and other parts of our multis".

Review page for review (before/after renders):
`work/tidy/tidy-review.html` (local work folder).

## Ground rules for every implementer

- **Presentation only.** No change to balance, recipes, structure shapes, element chains, energy
  maths, timing or saved NBT. A gameplay bug found on the way goes to "Log only" at the bottom,
  not into code.
- **Out of scope, do not touch:** `machines/kerr/**`, `machines/eye/**`, `BTSRecipesKerr/Eye/
  Stellar/Singularity/BEC`, `SLConsoleTest`, `SLGateDump`, and the Kerr/Eye lines in
  `en_US.lang`. The questbook is also out of scope (see "Questbook handoff").
- **GT conventions (RC-2, gregtech 5.09.54.205), checked against the source jar:**
  - Info block (visible by default) = description, then key stats; structure block (Shift) =
    dimensions, casings, hatches. Keep the info block to about 15 lines and each line to about
    70 characters. 1.7.10 tooltips do not wrap or scroll.
  - Use the count-first builder calls. The old forms are `@Deprecated` in RC-2:
    `addCasing(count, name, tiered)` replaces `addCasingInfoRange/Exactly/Min`;
    `addXxxHatch(count, info, dots...)` replaces `addXxxHatch(info, dots...)`. The count renders
    in YELLOW for hatches and GOLD for casings, so put counts in the count argument and keep them
    out of the text.
  - `addStaticParallelInfo(n)`, `TooltipHelper.parallelText/euRateText`, `addAir(info)`,
    `addSupportLaser()`, `addDeprecatedLine(info)` (renders `§4DEPRECATED - %s`),
    `beginVariableStructureBlock(wmin, wmax, hmin, hmax, lmin, lmax, hollow)`,
    `addOtherStructurePart(name, info, dots...)`, `toolTipFinisher(authors...)` (adds
    "Added by: ...").
  - `addMarkdown(ResourceLocation)` loads `assets/<mod>/lang/<lang>/tooltip/<name>.md` and appends
    its lines to the INFO block. It does not fold or hide anything, so it is a localisation tool,
    not a way to make a tooltip shorter. Long explanations go to the questbook.
  - Item tooltips: lines after the name render GRAY by default.
  - Failure text: `SimpleCheckRecipeResult.ofFailure(id)` needs a lang line
    `GT5U.gui.text.recipe_result.<id>=` (brief item 10). Shutdown reasons use
    `GT5U.gui.text.shutdown_reason.<id>=`, and GT's own values start with `§4` and end with a
    full stop.
- **Regression traps** (brief): the hologram hint path runs client-side; tile NBT and item NBT
  are separate; plant-wide hatch lists have no notion of piece; a chain's first element is the
  one that hints. The hint-number change (A4) touches only `.hint(n)`.
- **Display names in GregTech.lang are sticky.** `aNameRegional` and `GTFluidFactory` local names
  are written to my GregTech.lang on first run and are not refreshed. Renaming an MTE or a
  GT fluid in code only reaches new installs. Those renames are logged as questions here, not
  done.
- **Git:** granular local `wip:` commits on `tidy-old-multis`, staging only touched files, with the
  two trailers. No push, tag, reset, stash, checkout or merge. Gradle is run only from this
  worktree, one invocation at a time: `./gradlew.bat spotlessApply compileJava --no-daemon`, and
  `./gradlew.bat build` once a unit is done (checkstyle rejects wildcard imports).

## Glossary (one name per thing, in every player-facing string)

| Thing | Use | Stop using |
|---|---|---|
| plant controller | **Nuclear Plant Foundation** (or "the controller" when the context is obvious) | Unit One main station, main station, reactor controller |
| the silo | **Reactor Silo** | Steam Generator, Steam Generator silo |
| rotor gallery + output manifold | **Rotor Assembly** (the parts: "gallery", "output manifold") | Turbine Generator, "Rotor gallery + output manifold" as a name |
| the 3x3 duct / the chamber | **Spine** / **Spiral chamber** | |
| pillar block | **Reactor Fuel Pillar Frame** | Reactor Fuel Column, pillar frames |
| plant modules | **Extraction Derrick**, **Enrichment Annex**, **Vent Stack**, **Switchyard** | PLANT MODULES (D-17) |
| standalone machines | **Extraction Platform**, **Enrichment Train** ("standalone version of the Derrick / Annex") | campus front end |
| hatches by role | Rod Input Bus, Depleted Output Bus, Feedwater Input Hatch, Coolant Input Hatch, Propellant Input Hatch (Title Case, never all caps) | ROD INPUT BUS, PROPELLANT INPUT HATCH |
| product of Derrick/Annex | **feedstock** (Uraninite, Yellowcake, Thorium) | self-fuelling, pellets |
| rods setting | **rods N/24** | rods 100%, throttle cap, campaign power |
| units | number, space, unit: `1 L`, `16 L`, `10 s`, `20 min`, `±2 ticks`, `0.5%/tick` | 1L, 10s, +/-2t |
| spelling | centre, aluminium (NZ/UK, as the rest of the addon) | center |

Remove every design-doc ID (D-xx, F-xx, R-xx, TC-x), script name and version history from player
text. Use the machine or step name instead.

## File ownership (so the three units do not collide)

The units are meant to run in order A, then B, then C. If any run in parallel, keep to these
regions.

| File | A | B | C |
|---|---|---|---|
| `MTEReactorCoreMk1.java` | `createTooltip()` (~8380-8550); `.hint()` ints of the 4 module sockets (~2224-2255) | panel formatters ~4920-5010 (hold-reason strings), 5922 `euRate`, 6900-8170; `supportsPowerPanel` | `statusReport()` sections + new `scannerSummary()`, `getInfoData()` (~8169-8378) |
| `en_US.lang` | item/block names, item tooltips, shutdown reasons, `structure.silo.standing`, hatch names | `beyondthestars.gui.*` | `beyondthestars.recipe.*`, new `recipe_result` keys |
| `MTEProductionHallBase.java` | `toolTipFinisher` author | parallel supplier, GUI class | `getInfoData()` |
| hall / Train / Platform / retired MTEs | `createTooltip()` | `supportsPowerPanel`, GUI | `getInfoData()`, `checkProcessing` failure ids |
| control blocks (`MTESiloControlBlock`, `MTETurbineControlBlock`) | constructor description | | `onRightclick` chat |
| `progression/*`, `BTSUpgradeTreePanel` | | all | |
| `Command*.java`, `BTSCommands`, `BTSDisplayRecipes`, `BTSMachines` NEI hide | | | all |
| `BTSBom.java` | `REACTOR_FUEL_COLUMN` name | | |

New lang keys: append them in one block per unit (`# ---- tidy-old-multis: unit A ----`, and so
on) next to the existing nuclear keys, so a merge with the Kerr/Eye branches touches separate
lines.

---

## Unit A: tooltips, lang, item and block names

**Status: DONE** (commits fbadc70, 24afcd4, da737a2, 5ce97c3; `spotlessApply compileJava` and `build` pass).
What differs from the text below is listed under "Unit A: as built" at the end of this unit.

### A1. Nuclear Plant Foundation: cut the info block from 79 lines to about 16 (HIGH) - DONE
`machines/MTEReactorCoreMk1.java` `createTooltip()`.
At GUI scale 2-3 the block is taller than the screen, so players never see Heat, Control or the
math. Replace it with this. Text in `assets/beyondthestars/lang/en_US/tooltip/nuclear-plant.md`
via `addMarkdown` is preferred, with `{var:}` for the numbers. Plain `addInfo` is acceptable if the
markdown loader is awkward. A missing .md file throws, so test it in game.

```
Machine Type: Nuclear Reactor, Steam Turbine
§bA whole nuclear power plant, run from one controller.
Fuel rods boil feedwater into steam; the turbine turns it into EU.
One hologram projection builds every piece: platform, Reactor Silo,
Spine, Spiral chamber, Rotor Assembly, Cooling Towers and the modules.
---
§bSupply:§7 rods + feedwater at the silo face; coolant + propellant at
the Spiral chamber's front wall; Dynamos + Laser Sources at the manifold.
§bRunning:§7 each redstone pulse on the Turbine Control Block runs one cycle.
§bGUI:§7 Rods 1-24 (up to 36), RPM limit 12,000 (up to 19,500), Build Tier.
A Programmed Circuit in the controller overrides the Rods field.
§cHeat:§7 SCRAM at 8,000, meltdown at 10,000; 20 min of decay cooling.
§cWithout a Switchyard, output is capped at 100,000,000 EU/t.
§bProgress:§7 milestones raise output; spend points in the Upgrade Tree.
Progress is saved in the controller and survives breaking it.
The GUI's MATH section shows how the EU/t is worked out.
NEI: look up this controller for the fuel, propellant and coolant tables.
```
Before deleting any paragraph, check that the questbook already explains it; add any gap to the
"Questbook handoff" list below. Drop the 0.19.0 migration story, "furniture", "AT ITS POINT OF
USE", "BLAST-PROOF", and all caps except SCRAM, MELTDOWN and NOT FORMED.

### A2. Foundation: structure section, about 22 lines, with every hint dot named (HIGH) - DONE
Same method. Currently 41 `addStructureInfo` lines, a wrong Energy Hatch line, and no Dynamo,
Laser, Injector, Sensor or control-block lines. Target (verify every count against the socket
definitions and `checkMachine` before writing it):

```
.beginVariableStructureBlock(50, 50, 21, 41, 50, 50, true)   // height follows plantHeightFor(tier)
.addController("Silo face, bottom row, centre")
.addCasing("2,251", "Light Concrete (platform)", false)
.addCasing("249", "Tungstensteel Reinforced Block (shield pad)", false)
.addCasing("400-1,700", "Silo wall: Radiation Proof Machine Casing (T1-2), Europium Reinforced Radiation Proof Machine Casing (T3-4), Radiant Naquadah Alloy Casing (T5)", true)
.addCasing("40-320", "Dome: Tungstensteel Reinforced Block (T1-2), Iridium Reinforced Block (T3+)", true)
.addCasing("60-150", "Radiation Proof Machine Casing (bands, empty face sockets)", false)
.addCasing("45-201", "Reactor Fuel Pillar Frame (8 high inside the silo)", false)
.addCasing("0-12", "Turbine Housing (4 in the Spine, 8 in the gallery)", false)
.addInputBus("1", "Silo face, fuel row: Rod Input Bus", 1)
.addOutputBus("1", "Silo face, fuel row: Depleted Output Bus", 1)
.addInputHatch("1-3", "Silo face: Feedwater; Spiral front wall: Coolant, Propellant", 1, 3)
.addOutputHatch("1", "Silo face, middle row, right of the Silo Control Block", 1)
.addMaintenanceHatch("1-3", "Silo face (required); Spiral floor and manifold once built", 1, 3, 4)
.addDynamoHatch("0-4", "Output manifold back face, bottom row", 4)
.addEnergyHatch("0-4", "Manifold back face, middle row. The reactor needs none; they power the Derrick and the Annex", 4)
.addOtherStructurePart("Laser Source Hatch", "Up to 4: manifold back face, top row; paint each to match its Laser Target", 5)
.addOtherStructurePart("Silo Control Block", "Silo face, middle row, centre", 1)
.addOtherStructurePart("Turbine Control Block", "Manifold back face, above its Maintenance Hatch", 4)
.addOtherStructurePart("Precision Injector", "8: Spiral chamber walls, along the helix", 3)
.addOtherStructurePart("Spine Sensor", "Optional: Spiral chamber roof, front centre", 3)
.addOtherStructurePart("Module sockets", "Derrick Output Bus; Annex Input Bus, Acid Input Hatch, Output Bus", 2)
.addMasterChannel("Silo tier 1-5 (projector stack size; 1 uses the GUI Build Tier)")
.addStructureInfo("Plants built before 0.20: run /btsmigrate beside the controller once.")
.addSupportLaser()   // info block: GT's "Supports Laser Hatches!"
```
Delete the duplicated "Energy Hatches are NOT a structure demand" lines (they contradict the
Derrick/Annex text) and the 8 legacy/migration lines. Before: `"OPTIONAL, up to 4 - manifold back
face y4. This plant NEVER draws EU"`. After: as above.

### A3. Foundation: machine type, author footer, and a shared author constant (MEDIUM) - DONE
`addMachineType("Nuclear Plant Foundation - Unit One main station")` becomes
`addMachineType("Nuclear Reactor, Steam Turbine")`. Add
`BeyondTheStars.TOOLTIP_AUTHOR = EnumChatFormatting.AQUA + "Beyond the Stars"` and call
`toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR)` in all 9 in-scope `createTooltip()`s (Foundation,
4 halls, Platform, Train, 2 retired modules). The Kerr/Eye owners adopt the same constant later
(handoff).

### A4. Module sockets get hint 2 (HIGH, presentation) - DONE
`MTEReactorCoreMk1.java` ~2224-2255: the four `moduleSocket(..., 1, ...)` calls for `b e f g`
become `2`. Hints then read 1 silo face, 2 modules, 3 Spiral, 4 manifold, 5 lasers, and A2
documents hint 2. Only the hint int changes; elements, casings and faces stay as they are.

### A5. Hatch element display names (MEDIUM) - DONE
`ModuleHatchElement`, `PanelHatchElement`, `SpiralHatchElement`, `FluidHatchElement`: override
`getDisplayName()` through a constructor string (lang keys `beyondthestars.hatch.<name>`).
Structure errors and NEI previews currently show raw enum names.

| enum | before | after |
|---|---|---|
| DerrickBus | DerrickBus | Extraction Derrick Output Bus |
| AnnexFeed | AnnexFeed | Enrichment Annex Uraninite Input Bus |
| AnnexAcid | AnnexAcid | Enrichment Annex Acid Input Hatch |
| AnnexProduct | AnnexProduct | Enrichment Annex Output Bus |
| SiloPanel / TurbinePanel | SiloPanel / TurbinePanel | Silo Control Block / Turbine Control Block |
| Injector / Sensor | Injector / Sensor | Precision Injector / Spine Sensor |
| Feedwater / Coolant / Propellant | Feedwater ... | Feedwater Input Hatch / Coolant Input Hatch / Propellant Input Hatch |

Also fix the `AnnexProduct` javadoc ("fuel pellets" becomes Yellowcake).

### A6. Turbine Control Block tooltip points at the wrong building (HIGH) - DONE
`MTETurbineControlBlock.java` constructor + class javadoc (line ~18).
Before: `The turbine panel - socket it in the rotor gallery's front wall` /
`A redstone RISING EDGE runs one turbine cycle:` / `one buffered steam slug + one +/-2t injector
window + RPM gain` / `No clock = RPM decays to zero. Right-click for the live turbine readout`.
After: `Turbine panel: output manifold back face, above its Maintenance Hatch` / `Each redstone
pulse runs one turbine cycle (one steam slug + one injector window)` / `No pulses: the RPM decays
to zero` / `Right-click: turbine and output readout`. The last line must match C6.

### A7. Silo Control Block tooltip (MEDIUM) - DONE
Before: `The reactor panel - socket it in the silo face above the main station` / `Right-click for
the live reactor readout:` / `tier, rod cascade, core heat, decay heat, radiation`.
After: `Reactor panel: silo face, middle row, directly above the controller` / `Right-click:
reactor readout (tier, rods, heat, supplies)`. Must match C6.

### A8. Precision Injector tooltip (MEDIUM) - DONE
Before: `Feeds the Spiral chamber 1L of propellant at a time` / `Every litre is stamped with its
arrival tick - the slug window wants EXACTLY 1L` / `Mistimed or excess litres are wasted and heat
the chamber` / `Capacity: 16L - by design`.
After: `Spiral chamber injector (8 per plant, along the helix)` / `Stocked and timed automatically
from the chamber's Propellant Input Hatch` / `Hand-feeding works, but only 1 L inside the ±2 tick
window counts` / `Propellants: see NEI "Nuclear Plant: Propellant" · Capacity 16 L`.

### A9. Spine Sensor tooltip (MEDIUM) - DONE
Keep the name (GregTech.lang caching, see TQ-3). Before: three long lines, the second 105
characters. After: `Optional: Spiral chamber roof, front centre` / `Emits a 2-tick redstone
pulse on every accepted turbine cycle` / `Use it as the timing reference if you inject propellant
by hand`.

### A10. Production halls: structure counts, hollow flag, air, count-first hatches (HIGH) - DONE
`MTEFuelFabricationHall`, `MTEPurexCanyon`, `MTECascadeHall`, `MTESiteBatchingPlant`.
Re-count every casing from the shape before writing it (a short script over the `String[][]` is
fine). The numbers below are from the audit.

| hall | before | after |
|---|---|---|
| Batching | `addCasingInfoRange("Solid Steel / Clean Stainless / Tungstensteel Reinforced shell", 100, 130)` | `addCasing("155-156", "Solid Steel Machine Casing / Clean Stainless Steel Machine Casing / Tungstensteel Reinforced Block", true)` |
| Batching | `Steel Frame Box (lattice)` exactly 24 | `addCasing("16", ...)` |
| Fuel Fab | shell 250-269, short names, not tiered | `addCasing("262-264", "Clean Stainless Steel Machine Casing / Iridium Reinforced Block / Radiant Naquadah Alloy Casing", true)` |
| PUREX | Radiation Proof 220-240 | `addCasing("231-234", ...)` |
| Cascade | `beginStructureBlock(5, 5, 24, false)`; Turbine Casing 100-340; Iridium exactly 32; Pipe exactly 4 | `beginVariableStructureBlock(5, 5, 5, 5, 8, 24, true)`; `addCasing("140-397", "Tungstensteel Turbine Casing (+64 per stage bank)")`; `"0-32"` Iridium (8 per bank); `"0-4"` Pipe Casing (1 per bank); `addStructureInfo("Each stage bank adds 4 blocks of length: a ring of Iridium Reinforced Block around one Tungstensteel Pipe Casing.")`; move "Hold 1-5 controllers..." here as `"Projector: stack size 1-5 builds 0-4 stage banks."` |
| all four | `hollow=false`, no air line | `hollow=true` + `.addAir("The whole interior: keep pipes and cables outside")` |
| all four | `addEnergyHatch("Front face, top row (2)", 2)` | `addEnergyHatch("1-2", "Front face, 2nd row from the top", 2)`; same pattern for every hatch, with the true row ("top row" is the roof) |

### A11. Production halls: info text (MEDIUM) - DONE
- NEI-tab and machine-type consistency (with C4): machine type = category, item name = NEI tab.
  `Reprocessing Canyon` becomes `Fuel Reprocessor`; `Fuel Fabrication Line` becomes `Fuel Rod
  Assembler`; `Gas Centrifuge Cascade` becomes `Gas Centrifuge`; `Batching Plant` becomes `Bulk
  Casing Assembler`.
- PUREX: drop "Parallel 8, fixed." and add `.addStaticParallelInfo(8)` and `Circuit 1 = cooled
  fuel, 2 = fresh fuel, 3 = partition the dissolver liquor.`
- Fuel Fab / Batching / Cascade: parallel line via `TooltipHelper.parallelText(n)`, for example
  `"Parallel: " + parallelText(8) + " per shell tier (8 / 16 / 24)"`, plus a gray line
  `Parallels are also limited by the power your Energy Hatches supply.`
- Cascade: `Circuit 1 = low-enriched cut (IV), 2 = highly-enriched cut (needs LuV power).`
- Caps to colour (PUREX, Fuel Fab, Cascade, Batching): `HOW LONG THE FUEL COOLED CHANGES WHAT
  COMES OUT.` becomes `§bCooling time matters:§7 how long the fuel cooled changes what comes
  out.` The same treatment applies to WEAKEST, ENRICHED, TAILS, STAGE BANK, FIRST.
- Cascade: `...MOX diluent (F-20)` / `...the casing line spends (F-11c).` becomes `The tails
  (Depleted UF6) are a product, not waste: they become MOX diluent` / `and Depleted Uranium Ingots
  for shielded casings.`
- Batching: `Makes nothing GT cannot - only 8-32x the batch, for a higher voltage.` becomes
  `Makes nothing GT cannot: each recipe is an 8-32x bulk batch of a stock GT recipe.`
- Fix the Cascade class javadoc that calls `MTEEnrichmentTrain` "the Enrichment Annex".

### A12. Extraction Platform and Enrichment Train tooltips (HIGH) - DONE
Platform before: `Machine Type: Drilling Platform` / `The campus fuel front end: drinks EV-tier
EU, drills forever.` / `Every 10s: 6x Uraninite Dust + 1x Thorium Dust; every 4th cycle +1
Tantalum Dust.` / `Feed the uraninite to an Enrichment Train for fuel pellets.`
Platform after: `Machine Type: Ore Drill` / `A uranium mine that never runs dry: needs only power.`
/ `Every 10 s: 6 Uraninite Dust + 1 Thorium Dust; +1 Tantalum Dust every 4th cycle.` / `Uses
1,920 EU/t (EV)` (via `TooltipHelper.euRateText`) / `Feed the Uraninite to an Enrichment Train to
make Yellowcake.` / gray: `The Nuclear Plant Foundation has this drill built in as its Extraction
Derrick.` / `Output Bus required: output is lost without one.` (see L1).
Structure: `beginStructureBlock(5, 8, 5, false)`; `addController("Front face, centre, 2nd layer
from the bottom")`; hatches `addEnergyHatch("1+", "Any Solid Steel Machine Casing", 1)` and so on.

Train before: `Enrichment Line` / `The campus processing factory: leaches uraninite into
YELLOWCAKE.` / ... `F-01 .. F-08` ... `SIMPLICITY` ... `F-09 onward` ... `Hydrofluorination step`.
Train after: `Machine Type: Yellowcake Mill` / `Leaches Uraninite Dust with Hydrofluoric Acid into
Yellowcake.` / `Every 30 s: 8 Uraninite Dust + 2,000 L Hydrofluoric Acid -> 2 Yellowcake + 2
Thorium Dust` / `Uses 480 EU/t (HV)` / `Same 4 Uraninite per Yellowcake as the full ore line: it
saves machines, not ore.` / `Next step: Chemical Plant, Yellowcake -> Uranium Tetrafluoride.` /
gray: `Despite the name it does not enrich; enrichment is the Cascade Hall.` / gray: `The Nuclear
Plant Foundation has this built in as its Enrichment Annex.`
Structure: `beginStructureBlock(3, 3, 9, false)`; casing `"65-69"`; bus and hatch lines `"1+", "Any
Clean Stainless Steel Machine Casing (no Crafting Input Buffer)"`; `addController("Front face,
centre")`.

### A13. Retired module controllers use GT's deprecation line (MEDIUM) - DONE
`MTEModuleCoolingTower`, `MTEModuleTurbineHall`. Cooling Tower (17 info lines) becomes:
```
Machine Type: Plant Module - Cooling Tower
§4DEPRECATED - this controller does nothing; kept so old worlds load.
Cooling Towers are now pieces of the Nuclear Plant Foundation.
To migrate: break this block, place a Solid Steel Machine Casing here
and one in the cell directly outward of it (2 per tower).
§7A legacy Maintenance Hatch in the ring may stay (it is not counted).
```
Turbine Hall: `§4DEPRECATED - this controller does nothing; replaced by the plant's Spine, Spiral
chamber and Rotor Assembly.` plus 1-2 lines. Correct the structure counts (tower: 51 Solid Steel,
736 Light Concrete; hall: 76 Clean Stainless + 17 Glass) or drop the structure section.

### A14. Item and block tooltips (MEDIUM) - DONE
Move `SLItems.TooltipItem` to a shared class (`items/BTSTooltipItem`), use it for the in-scope
items, and add `addInformation` to the three blocks' ItemBlocks. All text goes through lang keys
`item.beyondthestars.<name>.tooltip` / `tile.beyondthestars.<name>.tooltip` (`\n` splits lines).
- Reactor Fuel Pillar Frame: `Stack 8 high inside the Reactor Silo.` / `Holds the fuel rods the
  Rod Input Bus feeds.`
- Rod Loading Port: `Optional: sits on a pillar; hand-feed rods here.`
- Turbine Housing: `Spine and gallery rotor socket.` / `Right-click with a Turbine Rotor to seat it;`
  / `sneak-right-click empty-handed to remove it.`
- Turbine Rotor: `Seat in a Turbine Housing.` / `Wear 0-9 (-5% output each); shatters at 10.`
- Fuel rods: `Steam multiplier xN per pillar.` / `Load into the silo's Rod Input Bus.`
- Depleted assemblies and rods: `Cool it in a Decay Warehouse first:` / `only cooled fuel yields
  plutonium in the PUREX Canyon (circuit 1).`
- Cooled assemblies and rods: `PUREX Canyon, circuit 1: yields plutonium for MOX.`
- Yellowcake: `From an Enrichment Train, the Annex, or the ore line's kiln.` / `Next: Chemical
  Plant -> Uranium Tetrafluoride.`
- Legacy turbine fans (3): `Legacy part: only the retired Turbine Hall used this.`

### A15. Lang names and shutdown reasons (LOW-MEDIUM) - DONE
- Shutdown reasons (5 lines): add `§4`, replace the em dash with ` - `, end with a full stop.
  `...scram=SCRAM: core overheat — re-enable the reactor to restart` becomes `...scram=§4SCRAM: core
  overheat - re-enable the reactor to restart.`
- `beyondthestars.structure.silo.standing`: `Tier %s Steam Generator IS STANDING and does not
  match...` becomes `Tier %s Reactor Silo is standing but not formed: the first wrong cell is
  listed above. Built on 0.19.0 or older? Run /btsmigrate beside the controller.`
- Item names with formulas: `Yellowcake (U3O8)` becomes `Yellowcake` (tooltip `U₃O₈`); likewise
  `Plutonium Dioxide`, `Thorium Dioxide Powder`. Leave `Ceramic-Grade UO2 Powder` and `Sintered UO2
  Pellet` (the formula is the name there).
- `BTSBom.REACTOR_FUEL_COLUMN = "Reactor Fuel Column"` becomes `"Reactor Fuel Pillar Frame"`.
  Re-running the questbook BOM generator is the questbook pass's job (handoff).
- Lang hygiene: move the stray `# D-24` comment above the tree keys; delete the orphan "instrument
  panel" comment.

### Unit A: as built (where it differs from the items above)

- **A1.** Plain `addInfo`, not `addMarkdown` (a missing .md throws and this pass had no game run to prove the
  file loads). The numbers come from the constants (`RPM_REDLINE`, `RPM_LIMIT_MAX`, `SCRAM_HEAT`, `MELTDOWN_HEAT`,
  `DECAY_HEAT_TICKS`, `GRID_SOFT_CAP`), so a tuning change cannot leave the tooltip behind. `addSupportLaser()` was
  in the info block; **the review removed it**: GT's green "Supports Laser Hatches!" means laser energy INPUT, and
  the plant's Energy Hatch sockets (`HatchElement.Energy`) refuse a Laser Target Hatch. Its lasers are Laser Source
  (output) hatches, already named in the info block and the structure block.
  The review also corrected the structure height from `21, 41` to `2, 42`: the silo piece is dome + wall - 1 layers
  (21 at T1, 41 at T5) standing at controller level, and the platform's floor layer adds one below it, so a plant is
  22 tall at T1 and 42 at T5 (as the old tooltip's 42 said); the bare platform, which forms on its own, is 2.
- **A2.** Counts re-derived from the shape code with `questbook/gen-bom.ps1`'s shape functions (per-char tallies),
  and several differ from the audit numbers above: silo wall `451-1,667` (Radiation Proof T1-2, Europium Reinforced
  T3-4, Radiant Naquadah T5), bands `69-141`, dome `113-497` (not 40-320), and the pillar line counts blocks, not
  pillars: `0-1,608` Reactor Fuel Pillar Frames (stacks of 8 on 45-201 pillars; frames and ports are optional
  cells). Added `0-830` Tungstensteel Turbine Casing (Spine 64-96 + Spiral 326 + gallery 408). The manifold back
  face is 7 rows: casing, Maintenance, Turbine Control Block, Dynamos (4th row up), Energy (5th), Lasers (6th),
  casing, so the hatch lines say "4th / 5th / 6th row up" instead of bottom / middle / top. Silo face sockets are
  worded from the controller: Silo Control Block directly above it, Feedwater left of the panel, Output Hatch right
  of it, Rod Input Bus / Depleted Output Bus two above the controller either side of centre. Extra structure lines:
  Rod Loading Ports optional; the list of optional pieces; "The Blueprint quests list every piece's blocks."
- **A8.** RC-2's `MTEHatchInput.getDescription()` returns GT's generic input-hatch text and ignored the injector's
  own lines. `MTEHatchPrecisionInjector` now overrides `getDescription()` to return its description array.
- **A10.** The Cascade Hall and the Batching Plant have ONE Energy Hatch socket each, so their count is `1`, not
  `1-2` (PUREX and the Fuel Rod Assembler have two). The Fuel Rod Assembler and the Batching Plant say "The rest of
  the interior" in the air line, because their interiors also hold the spine / lattice / riser. Hatch rows are
  counted from the front face of each shape (the roof row is the top row). "Left / right" follows the shapes'
  column order, the same convention the old tooltips used; check it in game.
- **A11 / A12.** PUREX circuit line: "Circuit 1 = cooled fuel, 2 = uncooled fuel, 3 = partition the liquor."
  The Train's numbers come from its constants. Long lines are split to stay near 70 characters.
- **A13.** The retired Cooling Tower's own shape holds 50 Solid Steel cells, not 51: it has two controller cells
  (see L4). Both retired controllers keep a short structure section, because they still form and the hologram and
  NEI preview still use it. The Turbine Hall's plinth line is `4-42` (44 cells less the required Dynamo and
  Maintenance Hatch, at least 4 casings to reach its 80 minimum).
- **A14.** `BTSItems.define()` and the 22 frozen `BTSItems` fields now create `BTSTooltipItem` (no lang key = no
  tooltip, so nothing else changes); `SLItems.defineWithTooltip` uses the same class. Cooled fuel says "circuit 1
  dissolves it; circuit 3 then partitions out plutonium", because plutonium comes out of the partition step (R-03),
  not the dissolver (R-02). Fuel assemblies say "burned at half rate (rods do better)" (the bus-fallback rule). The
  Turbine Rotor says "gallery Turbine Housing (8 per plant)"; the housing tooltip adds the spin toggle line.
- **A15.** `BTSBom.REACTOR_FUEL_COLUMN` keeps its constant name and now holds "Reactor Fuel Pillar Frame". gen-bom.ps1
  still maps shape char `F` to "Reactor Fuel Column", so its check now reports BTSBom as stale until the questbook
  pass re-runs it (see the handoff).
- **Not checked in game** (no game run in this pass): the Foundation tooltip height at GUI scale 3, the Shift view
  hints 1-5, the hall left/right wording, and the Yellowcake / PuO2 / ThO2 subscript glyphs.

---

## Unit B: GUIs (controller terminal, gap row, upgrade tree, halls)

**Status: DONE** (commits b53a594, 496158e, 1735f21, 1647929; `spotlessApply compileJava` and `build` pass).
What differs from the text below is listed under "Unit B: as built" at the end of this unit.

### B1. Gap row: label the three number fields (HIGH) - DONE
`MTEReactorSiloGui.createRightPanelGapRow`. Today there are three bare boxes of 20, 36 and 30 px.
Add a short text widget before each (`IKey.str("Tier")`, `"RPM"`, `"Rods"`, scale ~0.8). The row
is `reverseLayout(true)`, so add each label after its field. Check that it fits at the 480x270
viewport (GUI scale 4). If it does not fit, use one-letter labels and keep the GOLD titles.
Every field tooltip starts with a GOLD title and continues in GRAY (new keys `.title`):
- `beyondthestars.gui.throttle.title=Control Rods (1-36)`; tooltip `Rods N/24 set reactor power
  (24/24 = 100%). A Programmed Circuit in the controller overrides this field.`
- `beyondthestars.gui.rpmlimit.title=RPM Limit`; tooltip `The redline the turbine is held to. 12,000
  RPM is the design limit and the default.`; milestone line: "the TC-7 interlock" becomes "the
  coolant interlock".
- `beyondthestars.gui.buildtier.title=Build Tier (1-5)`; tooltip "Steam Generator" becomes "Reactor
  Silo".

### B2. Show the Programmed Circuit override (MEDIUM) - DONE
`isThrottleOverridden()` exists and nothing calls it. Sync it and append to `tierLine`: `rods
12/24 (Programmed Circuit - remove it to use the field)`. Also add it to the Rods field tooltip.

### B3. One unit for rods (MEDIUM) - DONE
`tierLine` `rods 100%` becomes `rods 24/24`. In milestone grants, `throttle cap 26/24` becomes
`rods up to 26/24`. The tree's `control-rod cap +2` becomes `rods +2 (N/24)`.

### B4. Panel lines that mislead (HIGH) - DONE
`MTEReactorCoreMk1` formatters:
- Seizure: the hold reason and the tooltip (A1 already drops "TRIPS into a protective spin-down")
  must not say it is safe. The hold reason `COOLANT LOST - protective spin-down, RPM bleeding off`
  becomes `COOLANT LOST - spinning down; near SCRAM heat this can SEIZE the Spine`.
- `selfFuelLine`: `Self-fuelling · Derrick 1,234 cycles (6 uraninite + 1 thorium /10s, +1 tantalum
  every 4th) · Annex ...` becomes `Feedstock · Derrick 1,234 loads (6 Uraninite + 1 Thorium / 8 s,
  Tantalum every 3rd) · Annex 56 batches (2 Yellowcake + 2 Thorium / 24 s)`. Build the rates from
  the progression getters, not literals.
- `outputSafetyLine` (armed state): `Output safety ARMED - over-injecting the hatch row explodes the
  plant (GT parity). /btssafety on to test without that.` becomes `Output safety: armed (output
  above dynamo + laser capacity explodes the plant, as in GT)`. Keep the test-mode and caught
  variants.
- Hold line: `no cycle requests - pulse the Turbine Control Block (or /btsclock on); RPM is
  decaying` becomes `no cycle requests - pulse the Turbine Control Block with redstone; RPM is
  decaying`.
- `energyHatchNote`: return "" unless a Derrick or Annex stands. Text: `Manifold Energy Hatches
  1/4 - they power the Derrick and the Annex`. Drop "furniture".
- `siloAbsentLine`: about 600 characters becomes `Tier N Reactor Silo is standing but not
  formed - see Structure Errors for the first wrong cell. (Built on 0.19.0 or older? Run
  /btsmigrate beside the controller.)` The not-built variant: `Reactor Silo not built - project
  this controller to raise it on the shield pad`.
- Weakest-term fix (~8040): `wrench the 4 Spine housings` becomes `right-click the 4 Spine housings
  to retune them (ride the swirl for speed, oppose it for efficiency)`.
- Empty-silo fix (~7986): `stack Reactor Fuel Column frames 8 high...` becomes `stack Reactor Fuel
  Pillar Frames 8 high on its floor, then stock the Rod Input Bus`.
- Coolant hold (~4931): `(1,234 L/t of water) - fill the Spiral chamber's COOLANT INPUT HATCH
  (...)` becomes `(needs 1,234 L/t of water, less of a better coolant) - top up the Spiral
  chamber's Coolant Input Hatch`.
- `rpmLine` `(trimmed)` becomes `(overspeed)`.
- `heatLine` Vent Stack: compute the minutes from the same getters, or print `Vent Stack: windows
  halved`.
- Jargon: `swirl 3` becomes `swirl 3 (heat if uncooled)`; `(P x1.00)` becomes `propellant x1.00`;
  `steam buffer 1.5 slugs` becomes `steam buffer 1.5 cycles`; `wear - - - - - - - - (of 10)`
  becomes `no rotors seated`; `no campaign running - ...` becomes `reactor not running - put fuel
  rods in the silo's Rod Input Bus and enable the controller`.
- Glossary sweep: every `Steam Generator`, `main station` and `Turbine Generator` in panel text.
- Colours on GT's dark terminal: feedwater BLUE becomes AQUA; radiation DARK_RED becomes RED; hint
  and locked text DARK_GRAY becomes GRAY.
- `euRate`: append the voltage tier, `1,920 EU/t (EV)`, via `GTUtility.getColoredTierNameFromVoltage`.

### B5. Milestones and tree summary read correctly (HIGH) - DONE
- `BTSUpgrade.treeEffectLine`: build each entry from `BTSChannel.describe(total - base)`. Before:
  `Tree effects: plant output x1.27 · decay-heat window x1.50 · Derrick tantalum interval +1`.
  After: `Tree effects: +27.0% plant output · decay-heat window 1.5x shorter · Derrick tantalum 1
  load sooner`.
- `BTSMilestone.milestoneLine`: show the NEXT grant after the progress, and UPTIME in hours. Before:
  `UPTIME T1/7 · 90,000 generating ticks -> 144,000 (25%) · +2.0% output`. After: `UPTIME T1/7 ·
  1.3 h -> 2.0 h (25%) · next: +5.0% output`. Review: the output grants are the line's running total, so a held
  tier adds the current total, `next: +5.0% output (now +2.0%)`, and "next:" cannot be read as a further +5.0%.
- REPROCESSING unit text: `loads + batches` becomes `Derrick loads + Annex batches`. The rename to
  "ORE PROCESSING" is TQ-1.
- Use one vocabulary: `x/112 points spent · y available` in both `treeSummaryLine` and the tree
  header.

### B6. Upgrade tree panel (MEDIUM) - DONE
`BTSUpgradeTreePanel`: add a column label above each branch (UPTIME, BURNUP, REPROC., TURBINE) in
its connector colour. The help line goes from DARK_GRAY to GRAY with the text `Left-click: buy ·
Right-click: refund · Hover: details`. The header becomes `UPGRADES n/25 · x/112 spent · y
available`.

### B7. Hide the Power Panel where it does nothing (LOW-MEDIUM) - DONE
Add `supportsPowerPanel() { return false; }` to `MTEReactorCoreMk1`, `MTEEnrichmentTrain`,
`MTEExtractionPlatform` and `MTEPlantModuleBase` (covers both retired modules). Check in game that
the button is gone.

### B8. Production halls: make the Power Panel limiter work, and show the parallel (HIGH / MEDIUM) - DONE
- `MTEProductionHallBase`: `setMaxParallelSupplier(this::getMaxParallelRecipes)` becomes
  `this::getTrueParallel`. This is the RC-2 javadoc rule. With GT's default `alwaysMaxParallel =
  true` the result is identical; only a player who unticks "max" in the panel sees a difference.
  It is not a balance change.
- New `MTEProductionHallGui extends MTEMultiBlockBaseGui` (pattern: GT's
  `MTELargeFluidExtractorGui`): sync `getMaxParallelRecipes`, `casingTier` and stage banks, and add
  one terminal line, `Parallel: 16 (shell: Iridium Reinforced Block, tier 2/3)` or, for the
  Cascade, `Parallel: 8 (stage banks 3/4)`. Server-only fields need sync values (trap 1).

### Unit B: as built (where it differs from the items above)

- **B1.** Captions and titles are lang keys (`beyondthestars.gui.<field>.label` / `.title`, unit B block in
  `en_US.lang`), drawn at 0.8 scale left of each field. The row did not fit at 480x270 with GT's four left-hand
  gap buttons showing (they were greyed out anyway: the plant supports no void protection, input separation, batch
  mode or recipe lock), so `MTEReactorSiloGui` hides them the way GT's own `MTETeslaTowerGui` does. With B7's Power
  Panel button gone the row is ~175 of 192 px. The existing tooltip, milestone and tree lang lines were reworded in
  place (same keys): "Reactor Silo", "coolant interlock", no TC-7, shorter.
- **B2.** `isThrottleOverridden()` now reads the controller slot with the campaign commit's own test (integrated
  circuit, damage >= 1). The old "committed N differs from the field" compare would have read "overridden" for a
  whole campaign after a plain field edit, because N only moves at the next campaign. Display only; nothing in the
  plant's logic calls it. Synced as `throttleOverride`; the Rods tooltip is dynamic and adds
  `beyondthestars.gui.throttle.override` (YELLOW) while a circuit is in.
- **B3.** `tierLine(tier, edge, rodsN, circuitOverride)`; `getThrottlePercent()` and its sync value are gone. The
  `statusReport()` call (unit C's region) was changed only to the new signature.
- **B4.** `selfFuelLine` takes the counters and node mask (GUI: synced; `statusReport`: `progression.snapshot()` /
  `nodes()`), and the Annex's Yellowcake is the batch average (`2` or e.g. `2.4`). `euRate` appends the tier
  UNCOLOURED (`1,920 EU/t (EV)`, `GTValues.VN`): GT's coloured name ends in a RESET that would cut the colour of the
  YELLOW module-stall text it is spliced into. Also tidied on the way: module-fault and tower names in Title Case
  (NOT FORMED kept), the suspend / under-powered stall notes (no y-coordinates, no caps), the mistimed-window hold
  line (`1 L`, `±2 tick`), the laser-socket row ("6th row up", unit A's count), the coolant STARVED / short lines.
  The Energy Hatch row in the GUI is gated on a Derrick or Annex standing, matching the formatter.
- **B5.** UPTIME prints hours with one decimal (`1.3 h -> 2.0 h`); the other lines keep counts. `BTSChannel`
  factors print as `1.5x` (trailing zeros dropped) in the node tooltips too, so tooltips and the effects line match.
  The STEAM ACCUMULATOR and OVERSPEED TRIM grant texts were brought to the glossary as well (cycles, RPM Limit).
- **B6.** The captions are fixed in the header (HEADER_H 32 -> 42; the graph scrolls under them), x taken from each
  branch's depth-1 node. Colours BLUE / RED / GREEN / GOLD for the blue / red / green / orange connectors. The root
  node is now "Plant Commissioning" (glossary: no "Unit One").
- **B7.** Hiding the Power Panel also hides GT's power-fail-events toggle on the Extraction Platform and the
  Enrichment Train (the two that draw EU). GT does the same on its own single-parallel multis (Heat Exchanger, Air
  Filter, Integrated Ore Factory).
- **B8.** `MTEProductionHallBase.getParallelDetail()` (shell tier; the Cascade Hall overrides it with stage banks)
  and `parallelSource(int)` (argument only, runs client-side) feed `MTEProductionHallGui`. The line shows only while
  GT's synced `machine` flag says the hall is formed. The PUREX Canyon prints `Parallel: 8 (fixed)`.
  **Review: the supplier switch is reverted** (back to `getMaxParallelRecipes`). A limit already saved in a
  controller's NBT would start to bite, which is a gameplay change; logged as L5 / TQ-8. The GUI line and the
  scanner print `getMaxParallelRecipes`, which is again what the hall runs at.
- **Left for unit C (chat, not GUI):** `MTEReactorCoreMk1.warnLegacyFace()` (~2748, the projection-refused chat)
  still says "Stand beside the main station".
- **Not checked in game** (no game run in this pass): the gap-row fit and caption legibility at GUI scale 4, the
  tree captions over the columns, the hall parallel line, and that the Power Panel button is gone.

---

## Unit C: scanner, chat, commands, NEI names, deprecated API

**Status: DONE** (commits a5e0dde, b3c7fb7, f2b6a27, 0083183, 43c3d24, 35ddef7; `spotlessApply compileJava` and
`build` pass). What differs from the text below is listed under "Unit C: as built" at the end of this unit.

### C1. One shared "no plant nearby" message (HIGH) - DONE
`BTSCommands.noStation(player)`, used by clock, safety, supply, migrate, milestone and tree.
Before: `No Unit One main station within 64 blocks.` After: `No Nuclear Plant Foundation within 64
blocks (its chunk must be loaded).` Fix the class javadocs that say "Unit One main station".

### C2. Strict on/off for the bench toggles (MEDIUM) - DONE
`/btsclock`, `/btssafety`, `/btssupply`: accept `on`, `off` and no argument (toggle); anything
else throws `WrongUsageException(getCommandUsage)`. Add `addTabCompletionOptions` returning
on/off. Name the plant in the reply: `... for the plant at (x, y, z)`. The setters do not change.

### C3. /btssupply off is not safe (HIGH) - DONE
`CommandBTSSupply` ~60. Before: `Infinite supplies OFF - the plant now runs on what you plumb.
Missing coolant or propellant just holds the RPM down (the panels say why); nothing seizes.` After:
`Infinite supplies OFF - the plant now runs on what you plumb. Missing coolant or propellant holds
the RPM down (the panels say why). Losing coolant at speed near SCRAM heat can still seize the
Spine.`

### C4. NEI tab names match the controllers (MEDIUM) - DONE
`en_US.lang`: `beyondthestars.recipe.cascade=Gas Centrifuge Cascade` becomes `Cascade Hall`;
`fabrication=Fuel Fabrication Line` becomes `Fuel Fabrication Hall`; `purex=Reprocessing Canyon`
becomes `PUREX Canyon`. Only the values change; the keys are the map ids.

### C5. NEI page text (MEDIUM) - DONE
`BTSDisplayRecipes`: `No cover = cycle refused` becomes `Too little coolant = cycle refused`;
`Redline 12,000` becomes `Redline 12,000 (up to 19,500 unlocked)`; `Out: bus beside it, same
course` becomes `Out: Depleted Output Bus beside it`; `FUEL LINE - F-12 reconversion:` becomes
`Fuel line - UF6 reconversion:`.

### C6. Control blocks print their own section, not the whole plant (HIGH) - DONE
`MTESiloControlBlock.onRightclick`, `MTETurbineControlBlock.onRightclick`, and
`MTEReactorCoreMk1.statusReport()`. Split `statusReport()` into named groups
(`statusReportSection(Section.X)`) that still use the same formatters. The GUI keeps the full
panel, so the shared-formatter rule still holds.
- Silo Control Block: header, REACTOR group, weakest-term line, then `§7Open the Nuclear Plant
  Foundation for the full panel`.
- Turbine Control Block: header, TURBINE + OUTPUT groups, weakest term, the same closing line.
- Not linked: `Not linked - reform the main station's structure` becomes YELLOW `Not linked: the
  Nuclear Plant Foundation is not formed - fix its structure, then right-click again.`

### C7. Portable Scanner gets a summary (MEDIUM) - DONE
`MTEReactorCoreMk1.getInfoData()`: replace `statusReport()` (35-45 lines) with `scannerSummary()`
of about 8 lines: tier + rods, heat, supply, RPM/redline, EU/t (+ clamp), weakest term, milestones
summary, and `Open the controller GUI for the full panel`.

### C8. /btskit and /btsbuild wording (HIGH / MEDIUM) - DONE
- `CommandBTSKit` 223 and 274: `(on-site pellet line, ...` becomes `(on-site yellowcake line, ...`;
  `output bus (pellets)` becomes `output bus (yellowcake + thorium)`.
- 193: `Grab via NEI (creative). Every count below is DERIVED from the structure shape code by
  questbook/gen-bom.ps1 - ...` becomes `Bill of materials (the same counts the Blueprint quests
  show) - grab via NEI in creative:`.
- 232: `Point-of-use sockets (D-20)` becomes `Point-of-use sockets`.
- Kit contents message: `Kit given: 3 controllers, ...` becomes `Kit given: Nuclear Plant
  Foundation (+ the standalone Extraction Platform and Enrichment Train, optional), ...`. Stop
  handing out the 3 retired turbine fans. Add the 4 hall controllers (dev command, presentation).
- `/btskit` usage: `/btskit [1-5|kerr|eye]`. Add tab completion.
- `CommandBTSBuild` ~262: print the result line, one `Bench clock + infinite supplies ON - /btsclock
  off and /btssupply off to run it for real` line, and one unpainted-laser warning. The long
  paragraphs go to the questbook handoff. Both commands default to the same tier (TQ-5).

### C9. /btstree refusals say why (MEDIUM) - DONE
Move `BTSUpgradeTreePanel.verdict()` into `BTSUpgrade` (static, plain text) and use it in both
places. Before: `Refused: Moderator Trim - prerequisites, milestone gate or points`. After:
`Refused: Moderator Trim - LOCKED: the BURNUP tier 1 milestone opens this`.

### C10. Retired modules (HIGH / MEDIUM) - DONE
- `MTEPlantModuleBase` scanner: the shared second line becomes an abstract `retiredNote()`.
  Cooling Tower: `The tower is a piece of the Nuclear Plant Foundation now - project that
  controller.` Turbine Hall: `Replaced by the plant's Spine, Spiral chamber and Rotor Assembly. This
  block does nothing.`
- `checkProcessing`: return `SimpleCheckRecipeResult.ofFailure("beyondthestars.retired")` +
  `GT5U.gui.text.recipe_result.beyondthestars.retired=§eRetired controller - it does nothing (see
  its tooltip)`.
- Delete `MTEModuleTurbineHall.getInfoData()` (the fan / boost / 0 EU/t lines).
- Hide both from NEI (`codechicken.nei.api.API.hideItem` in our NEI config, client only).

### C11. Enrichment Train says what it is missing (MEDIUM) - DONE
`checkProcessing`: two `NO_RECIPE` returns become `ofFailure("beyondthestars.train.uraninite")` and
`ofFailure("beyondthestars.train.acid")`, with
`GT5U.gui.text.recipe_result.beyondthestars.train.uraninite=§7Needs 8 Uraninite Dust in an input
bus` and `...train.acid=§7Needs 2,000 L Hydrofluoric Acid in an input hatch`.

### C12. Hall scanner lines (LOW-MEDIUM) - DONE
`MTEProductionHallBase.getInfoData`: `Shell tier: 1  -  casings counted: N` becomes `Casings: N`.
Fuel Fab and Batching print one shell line, `Shell: Iridium Reinforced Block (tier 2/3) - the
weakest block sets the tier`. The Cascade `  ·  ` separator becomes ` · `. Use ` · ` everywhere.

### C13. Turbine Housing right-click message (MEDIUM) - DONE
`BlockTurbineHousing` ~120. Before: `Rotor: CLOCKWISE (rides right-hand swirl)`. After (Spine
housing): `Spin: CLOCKWISE - rides the swirl (more speed)` / `Spin: ANTICLOCKWISE - opposes the
swirl (more efficiency)`. In a gallery stage add: `Gallery stages ignore spin direction`. The
toggle itself is unchanged. If a housing cannot tell which piece it is in, word the message so it
holds for both.

### C14. Chat style helper (LOW) - DONE
`BTSChat` with header/ok/warn/error/note (AQUA / GREEN / YELLOW / RED / GRAY). Bad arguments throw
`WrongUsageException`. Toggles reply in one short sentence. This is optional if time is short.

### C15. Deprecated GT API sweep (MEDIUM, finishes the A work) - DONE
Unit A writes all new tooltip lines with the count-first calls. Unit C then greps the in-scope
files for `addCasingInfoRange|addCasingInfoExactly|addCasingInfoMin` and any `addXxxHatch("...",
<int>` without a count argument, and migrates the rest: `addCasingInfoRange(n, a, b, t)` becomes
`addCasing("a-b", n, t)`, `Exactly` becomes `"n"`, `Min` becomes `"n+"`. The target is zero
deprecated `MultiblockTooltipBuilder` calls outside kerr/eye. Handoff: Kerr/Eye `BlockIcons.custom
("beyondthestars:iconsets/X")` becomes `custom("beyondthestars", "iconsets/X")`.

### Unit C: as built (where it differs from the items above)

- **C1.** `BTSCommands.noStation(sender)` is the one reply ("No Nuclear Plant Foundation within 64 blocks (its chunk
  must be loaded).") for clock, safety, supply, migrate, milestone and tree; `/btsmigrate` dropped its extra "stand
  beside the controller" sentence. `/btsmigrate`'s other replies were brought to the glossary on the way (Reactor
  Silo, no MAIN STATION or y-coordinates; the Energy Hatch paragraph is one line pointing at the manifold's 5th row).
- **C2.** `BTSCommands.parseToggle` / `toggleCompletions` / `plantAt`. The argument is checked after the plant
  lookup, so with no plant nearby a bad argument still gets the no-plant reply. `/btsmilestone` and `/btstree`
  also throw `WrongUsageException` for a missing or unknown verb now (they printed the usage in red).
- **C4.** `beyondthestars.recipe.batching` already read "Site Batching Plant", so only three values changed.
- **C5.** NEI lines stay within the ~28-character description column, so the texts are shorter than the item
  above: `Redline 12,000 (19,500 max)` (from `RPM_LIMIT_MAX`), `Too little coolant = refused`, `Out: Depleted Output
  Bus`. The water page also lost its caps (`Reactor - feedwater:`, "water buys cheap fuel").
- **C6.** `MTEReactorCoreMk1.ReportSection` (REACTOR, STEAM_CHAIN, TURBINE, OUTPUT, MATH, PROGRESSION),
  `statusReportSection(...)`, `controlBlockReport(boolean turbinePanel)` and a `weakestTermLine()` split out of
  MATH. `statusReport()` is now built from the sections (same order, same lines) and no longer has a caller; it
  stays as the full text panel the GUI comments refer to. Line counts: Silo Control Block 10-11; Turbine Control
  Block 12 normally, 13 while a HOLD line shows, 14 if the manifold Energy Hatch note also shows (the OUTPUT group
  keeps its always-printed safety and Switchyard lines). A Turbine Control Block on a plant with no silo prints
  the silo-absent line instead of empty groups. The bench-flag line is left out of the control blocks (the RPM
  line already says "bench clock", the safety line says test mode).
- **C7.** `scannerSummary()`: tier/rods, heat, supply, RPM, EU/t, HOLD (only when holding), weakest term, the
  milestone summary, the bench line when a bench flag is on, and `Open the controller GUI for the full panel` -
  8-10 lines after GT's own scanner lines. A plant with no silo prints the silo-absent, tower and module lines.
- **C8.** `/btskit` gives the four hall controllers and no turbine fans; the shopping-list headings read "feedstock
  mine" / "on-site yellowcake line" and the Switchyard line takes its number from `GRID_SOFT_CAP`. The BOM piece
  names come from `BTSBom` (generated by gen-bom.ps1) and were not edited, so "Rotor gallery + output manifold"
  still prints there (handoff). `/btsbuild` keeps its per-site FORMED line, supplies line and ceiling line, then
  the facing line, the bench line and the laser warning. Defaults unchanged: `/btsbuild` 3, `/btskit` 1 (TQ-5 open).
- **C9.** `BTSUpgrade.verdict(node, mask, counters)` returns the coloured line; the tree tooltip shows it as-is
  and `/btstree` strips the colour. The verdict words now use a colon ("LOCKED: buy X first", "SHORT: 2 more
  point(s) needed", "OWNED: right-click to refund ..."), in the tooltip too. A refund of a node that is not held
  says "not bought".
- **C10.** There was no NEI config in the addon; `NEIBTSConfig` (NEI's class scan picks up `NEI*Config`, as GT's
  `NEIGTConfig`) is new and hides only the two retired controllers. The retired result is a static
  `SimpleCheckRecipeResult.ofFailure("beyondthestars.retired")`; key in the unit C lang block.
- **C11.** Same checks in the same order (uraninite count first, then the acid drain); only the result text differs.
- **C12.** The base prints `Parallel: N` and `Casings: N`; the Fuel Fabrication Hall and Site Batching Plant print
  `Shell: <block> (tier t/3) - the weakest block sets the tier` from the GUI's `SHELL_NAMES` (their per-block
  shell counts are no longer printed).
- **C13.** A housing knows neither its piece nor whether it rides (that depends on the housings nearer the silo,
  `computeSpine`), so it prints `Spin: CLOCKWISE` / `ANTICLOCKWISE` and one GRAY rule line that holds everywhere:
  "Spine: turning with the swirl of the housings nearer the silo adds speed, against it adds efficiency. Gallery
  stages ignore spin."
- **C14.** `BTSChat` (root package, public) is used by the plant commands; the Kerr/Eye commands were not touched.
- **C15.** A call-shape scan and a `-deprecation` compile find no deprecated `MultiblockTooltipBuilder` or texture
  call in the in-scope files. Two in-scope deprecation warnings remain, both StructureLib structure code and kept
  on purpose: `BTSStructures`' delegating `survivalPlaceBlock(..., IItemSource, ...)` override, and the Cascade
  Hall's bank probe, which uses the error-free `checkPiece(name, h, v, d)` so a missing optional bank does not
  post a structure error.
- **Also:** the projection-refused chat (`warnLegacyFace`, left over from unit B) and the over-injection server-log
  tag no longer say "main station" / "Unit One".
- **Remaining "Unit One" strings:** `CommandBTSQuests` names the quest whose title IS "Unit One"; it changes with the
  quest (handoff). The `F-xx` / `R-xx` / `TC-x` strings left in `BTSRecipesFuel` / `BTSRecipesMachines` are recipe
  guard IDs for the log, not player text.
- **Not checked in game** (no game run in this pass): control-block and scanner line counts, `/btskit` and
  `/btsbuild` tab completion, the NEI hide (`NEIBTSConfig` discovered), the retired / Train status-line texts.

---

## Deferred (not in this pass, with reason)

- **Full localisation** of the ~95% hardcoded panel, chat and NEI text. Rule for this pass: new
  tooltip text for items, blocks and GUI fields goes to lang. Panel lines stay in Java until a
  dedicated pass.
- **NumberFormatUtil** (GTNHLib) for about 65 `%,d` numbers: a wide change in shared formatters;
  needs its own pass.
- **New NEI display maps** for the Extraction Platform and Enrichment Train (display only). Good
  idea, but it adds new content; do it after this pass. The tooltips (A12) carry the numbers
  meanwhile.
- **Hatch textures follow the shell tier** (Fuel Fab, Batching) and **better front overlays**
  (Platform/Train use the Heat Exchanger overlay, Batching uses the EBF overlay): texture work.
- **`supportsInputSeparation()=true`** on the halls changes recipe search behaviour. TQ-6.

## Log only: gameplay bugs and questions (do not fix here)

- **L1** Extraction Platform forms and runs with no Output Bus, burning 1,920 EU/t while its output
  is voided. A full bus also voids (`supportsVoidProtection` false). The Enrichment Train voids on
  full output too. Suggest `checkHasOutputBus` plus void protection.
- **L2** Starved-coolant friction can still `seize()` near SCRAM (Mk1 ~5010). The text is fixed
  (B4/C3). Is seizure on coolant loss intended at all?
- **L3** The halls' energy sockets take plain Energy Hatches only, so top parallels need ZPM+
  hatches and re-walls feel inert at IV. Allow ExoticEnergy, or document the cap (A11 adds the
  line).
- **L4** (found in unit A) The retired Cooling Tower controller's `towerShape()` marks TWO controller cells: the
  front-centre cell of row z=0 is turned into '~' when it is ring, and then `Math.round(8 - 7.5)` = 1 puts the
  anchor '~' at z=1 as well. The z=0 cell is never checked, so that ring cell can be anything. The Foundation's own
  tower pieces remap both cells to ring ('R'), so the live plant is unaffected; only the museum controller is.
- **L5** (found in the tidy review) The production halls ignore the Power Panel's parallel limit: their
  `ProcessingLogic` takes `getMaxParallelRecipes`, not GT's `getTrueParallel()`, so the panel's "max parallel"
  field shows on all four halls and does nothing. Tidy B8 switched the supplier, but that makes a limit stored in
  old controllers' NBT start to bite, so the review reverted it (gameplay is out of scope here). See TQ-8.

Questions for review (implementers append these to `docs/singularity-ladder-questions.md` as
"## Q-n" when they reach them). **All eight settled 2026-10-13** (user: "You have full free reign on this"; the choices and the
build are question B-85 and Part D of the questions doc): TQ-1 renamed to ORE PROCESSING; TQ-2 Yellowcake Mill;
TQ-3 Spiral Cycle Sensor; TQ-4 "Grade 1-6" (not "Mk I-VI": GoodGenerator's Naquadah Fuel MkI-MkVI feeds the same
rungs); TQ-5 both default to tier 1; TQ-6 the halls offer input separation, off by default; TQ-7 Extra Leach Tank;
TQ-8 the halls use `getTrueParallel()`.
- **TQ-1** Rename the milestone line REPROCESSING to ORE PROCESSING (it counts Derrick loads and
  Annex batches, not the PUREX Canyon)? This needs the questbook text changed too. This pass only
  fixes the unit text.
- **TQ-2** (logged in singularity-ladder-questions.md Part D) Rename "Enrichment Train" (it does no enrichment)? MTE display names are sticky in
  GregTech.lang, so a rename only reaches new installs.
- **TQ-3** (logged, Part D) Rename "Spine Sensor" to "Spiral Cycle Sensor"? The same stickiness applies; this pass
  adds a placement line only.
- **TQ-4** (logged, Part D) Propellant "Lv 1-6" (items and fluids) reads like GT's LV tier. Change it to "Mk I-VI"?
  The fluid names are sticky as above.
- **TQ-5** Default tier for `/btsbuild` (3) vs `/btskit` (1): which one?
- **TQ-6** Enable input separation on the halls?
- **TQ-7** Rename upgrade node "Extra Centrifuge Stage" (it boosts the Annex) to "Extra Leach
  Tank"? This also needs questbook text.
- **TQ-8** (logged, Part D) Should the halls honour the Power Panel's parallel limit (L5)?

## Questbook handoff (for the questbook pass)

Text removed from tooltips and chat that the questbook must carry if it does not already: the
Spine swirl ride/oppose rule; the injector slug window; laser painting and voltage matching; the
Switchyard soft cap and its config flag; tower effects; module sizes and placement; decay heat on
the same water ladder; the milestone tiers 5-7 opt-in unlocks; controller blast-proofing; the
/btsbuild module and point-of-use paragraphs. Names changed by the glossary (update quest text and
re-run gen-bom): Unit One main station, Steam Generator, Reactor Fuel Column, Gas Centrifuge
Cascade, Fuel Fabrication Line, Reprocessing Canyon.

Added by unit A:
- **gen-bom.ps1:** change `$ELEMENT_ITEM['F']` to "Reactor Fuel Pillar Frame", then re-run it. The constant it
  renders will be named `REACTOR_FUEL_PILLAR_FRAME`, so rename `BTSBom.REACTOR_FUEL_COLUMN` in `CommandBTSKit` (line
  ~118) in the same commit. Until then gen-bom reports BTSBom.java as stale (unit A changed only the value).
- **Text cut from the Foundation tooltip** that the questbook must carry if it does not already: the RPM rule
  (each cycle adds 176 x propellant x timed/8 RPM, 0.5%/tick decay, redline 12,000; TURBINE T5/T6 raise the
  ceiling to 13,500 / 15,000 and the player opts in through the RPM Limit field); coolant drain goes with RPM
  squared and the interlock refuses a cycle the chamber cannot cool; the dynamo and laser ceiling (4 IV Dynamos cap
  a tier-5 plant at 32,768 EU/t, two ZPM Laser Sources carry the full ~1.1B); the rod ladder (Uranium x1, MOX x2,
  Thorium-Bonded x5, Naquadah-Bonded x9) and the half-rate assembly fallback; residual swirl becomes heat in an
  uncooled chamber; Build Tier never re-tiers a standing silo; decay heat draws the same feedwater ladder (plain,
  distilled, IC2 Coolant, purified grades, worst first); breaking a fuelled pillar frame ejects its rods; module
  sockets join no plant-wide list; projections never overwrite a standing machine.
- **Tooltip names changed by unit A** (machine types only; the MTE display names are unchanged): Fuel Reprocessor
  (PUREX Canyon), Fuel Rod Assembler (Fuel Fabrication Hall), Gas Centrifuge (Cascade Hall), Bulk Casing Assembler
  (Site Batching Plant), Ore Drill (Extraction Platform), Yellowcake Mill (Enrichment Train). Item names: Yellowcake,
  Plutonium Dioxide, Thorium Dioxide Powder (formulas moved to the tooltips).
- **Kerr / Eye owners:** adopt `BeyondTheStars.TOOLTIP_AUTHOR` in `toolTipFinisher(...)`; `items/BTSTooltipItem`
  is the shared lang-tooltip item class (SLItems already uses it).

Added by unit B:
- **gen-questbook.ps1** quotes panel text that changed: the BURNUP grants (~1207-1209, "throttle cap 26/24" is now
  "rods up to 26/24"), the root node title (~1258, ~1285, "Unit One Commissioning" is now "Plant Commissioning"),
  and any quoted milestone line (UPTIME now prints hours and the NEXT grant; REPROCESSING's unit is "Derrick loads +
  Annex batches"). The GUI field names are now "Rods", "RPM" and "Tier" captions with the titles "Control Rods",
  "RPM Limit", "Build Tier".
- **Text cut from the panel** that the questbook should carry: the silo-face migration story (0.19.0 plumbing
  frontage, what /btsmigrate rewrites and drops), the Vent Stack's halved windows in minutes, and the
  output-safety test mode (`/btssafety on`) now that the armed line no longer names the command.

Added by unit C:
- **CommandBTSQuests** prints the entry quest's title, "Unit One" (lines ~102, ~109, ~190). Rename it there in the
  same commit that renames the quest.
- **gen-bom.ps1** piece name "Rotor gallery + output manifold" (glossary: Rotor Assembly) shows in `/btskit`'s bill.
- **Text cut from chat** that the questbook should carry: `/btsbuild`'s module paragraph (what the Vent Stack,
  Switchyard, Derrick and Annex do, with their rates), its point-of-use socket layout and "Spine left all-clockwise";
  `/btsmigrate`'s face layout; the Turbine Housing's old "rides right-hand / left-hand swirl" wording (the rule is
  now: with the swirl of the housings nearer the silo = speed, against = efficiency).
- **Kerr / Eye owners** (from the `-deprecation` compile): `addCasingInfoExactly` (11 Kerr, 7 Eye), two-argument
  `addOtherStructurePart` (1 Kerr, 5 Eye), `addInputBus` / `addOutputBus` / `addEnergyHatch` /
  `addMaintenanceHatch(String, int...)` (Kerr 4, Eye 1), `BlockIcons.custom(String)` (Kerr 4, Eye 4, Stellar Field
  Sensor 2: use `custom("beyondthestars", "iconsets/X")`), `GTUtility.sendChatToPlayer` (Kerr 4, Eye 5). `BTSChat`
  is available for their chat. The Eye lang line `starChartSiriusBCooled.tooltip` still says "since D-49".

## Done when

- `./gradlew.bat build` passes. A grep of the in-scope files finds no `D-[0-9]`, `F-[0-9]`,
  `TC-[0-9]`, `main station`, `Steam Generator`, `Unit One` in player strings, and no deprecated
  `MultiblockTooltipBuilder` call.
- Every new `ofFailure` id has a `GT5U.gui.text.recipe_result.` line.
- In game: the Foundation tooltip fits on screen at GUI scale 3; the Shift view lists hints 1-5;
  the gap row labels are readable at GUI scale 4; the control blocks print 12 lines or fewer; the
  scanner prints a short summary; the halls show their parallel in the GUI.
