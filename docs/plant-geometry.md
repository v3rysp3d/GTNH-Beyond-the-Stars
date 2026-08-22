# Unit One — canonical plant geometry (as of 0.20.0 — ONE controller, ONE projection)

Frame: the main station controller sits at platform (a=25, c=17), pad level y=0. `a` is
the horizontal axis (0..49), `c` the depth axis (0=front, 49=back), everything rotates
with the controller's facing. `edge` = silo diameter = 7 + 2*tier (tiers 1..5: 9-17).

**There is exactly one controller in the whole design.** Every building — silo, spine,
spiral, gallery, both cooling towers, and the four D-17 plant modules (Vent Stack,
Switchyard, Extraction Derrick, Enrichment Annex) — is a piece of the main station, raised
by one hologram projection. Module controllers and dock markers are retired (A3).

**There is no plumbing wall.** The POINT OF USE ruling (2026-08-19, in-game session:
*"the inputs need to go exactly where they are fed into in the logic flow"*) reverses the
earlier one-automation-bay answer, which the same player gave before seeing it built. The
Automation Bay piece (a 20..30, c 13..14, y 0..3) is **deleted**; every socket it carried
now sits in the building that consumes what enters through it, on the course where it
enters. This ruling is **`D-20`** in `../PROPOSAL.md` ("Point-of-use I/O + onboarding",
settled 2026-08-19) - registered, and the Java comment tags now point at it. `D-19` remains
"Turbine Control (RPM + sockets + rotors)"; the turbine-control work is tagged `TC-1`..`TC-7`
in the source and was never re-pointed.

## Platform (mandatory)
- 50x50, two layers: floor layer at y=-1 (2251 Light Concrete 'O' + 249 Tungstensteel
  Reinforced 'B' shield-pad octagon, max-tier footprint a,c 17..33), walking level y=0
  open except the controller.
- Controller at (25,17) on the pad's front line.
- The eight flush Solid Steel dock markers are GONE (they existed only to stand module
  controllers on). Those cells are plain Light Concrete now.
- checkPiece/buildPiece offsets: (25, 0, 17).

## THE POINT-OF-USE SOCKET TABLE (canonical)

Every socket in the plant, on the building that consumes what enters through it. Verified
numerically at all five tiers (0 overlaps, 0 air-vs-block contradictions, 0 entombed
sockets; every declared outward face open and standing +0..+4 above a walkable surface,
except the two pre-existing high injectors noted below).

| char | piece | a | c | y | normal | creative auto-fit MTE | tier-varies? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `u` **Rod Input Bus** | silo face | 24 | 17 | 2 | FORWARD | `ItemList.Hatch_Input_Bus_IV` | no |
| `Q` **Depleted Output Bus** | silo face | 26 | 17 | 2 | FORWARD | `ItemList.Hatch_Output_Bus_IV` | no |
| `y` **Feedwater Input Hatch** | silo face | 24 | 17 | 1 | FORWARD | `ItemList.Hatch_Input_IV` | no |
| `p` Silo Control Block | silo face | 25 | 17 | 1 | FORWARD | `BTSMachines.siloControlBlock` | no |
| `Y` **Output Hatch** (waste water / steam) | silo face | 26 | 17 | 1 | FORWARD | `ItemList.Hatch_Output_IV` | no |
| `t` **Maintenance** | silo face | 24 | 17 | 0 | FORWARD | `ItemList.Hatch_Maintenance` | no |
| `k` **Coolant Input Hatch** | Spiral front wall | 44 | 14+edge/2 | 1 | FORWARD | `ItemList.Hatch_Input_IV` | yes: c 18/19/20/21/22 |
| `P` **Propellant Input Hatch** (bulk) | Spiral front wall | 48 | 14+edge/2 | 1 | FORWARD | `ItemList.Hatch_Input_IV` | yes: c 18/19/20/21/22 |
| `r` **Energy Hatch** ×4 | manifold back | 44,45,47,48 | 38+edge/2 | 4 | BACK | `ItemList.Hatch_Energy_IV` | yes: c 42/43/44/45/46 |
| `m` Dynamo ×4 | manifold back | 44,45,47,48 | 38+edge/2 | 3 | BACK | `ItemList.Hatch_Dynamo_IV` | same rows as `r` |
| `L` Laser Source ×4 | manifold back | 44,45,47,48 | 38+edge/2 | 5 | BACK | `CustomItemList.eM_dynamoTunnel3_ZPM` | same rows as `r` |
| `q` Turbine Control Block | manifold back | 46 | 38+edge/2 | 2 | BACK | `BTSMachines.turbineControlBlock` | same rows as `r` |
| `n` Maintenance | manifold back | 46 | 38+edge/2 | 1 | BACK | `ItemList.Hatch_Maintenance` | same rows as `r` |
| `i`/`v`/`w`/`x` Precision Injector ×8 | Spiral walls | — | — | 3..10 | BACK/RIGHT/FORWARD/LEFT | `BTSMachines.precisionInjector` | rows follow the chamber |
| `j` Spine Sensor | Spiral roof | 46 | 14+edge/2 | 11 | UP | `BTSMachines.spineSensor` | rows follow the chamber |
| `l` Maintenance | Spiral floor | 49 | 14+edge/2 | 0 | FORWARD | `ItemList.Hatch_Maintenance` | rows follow the chamber |
| `b` **Derrick Output Bus** | Derrick deck, front wall | 36 | 4 | 1 | FORWARD | `ItemList.Hatch_Output_Bus_IV` | no |
| `e` **Annex Input Bus** (uraninite) | Annex +a wall | 21 | 3 | 1 | LEFT | `ItemList.Hatch_Input_Bus_IV` | no |
| `f` **Annex Input Hatch** (HF acid) | Annex +a wall | 21 | 7 | 1 | LEFT | `ItemList.Hatch_Input_IV` | no |
| `g` **Annex Output Bus** (yellowcake) | Annex +a wall | 21 | 11 | 1 | LEFT | `ItemList.Hatch_Output_Bus_IV` | no |

The four module sockets join **no plant-wide hatch list**. That is a correctness property,
not tidiness: `mInputHatches` / `mOutputBusses` have no notion of which piece registered a
hatch, so an Annex acid hatch in `mInputHatches` would satisfy the SILO's
`checkHasInputHatch` demand and let a plant form with no feedwater socket — and a shared
list is a shared inventory, so the reactor's rod distribution would eat the Annex's
uraninite and the cascade's depleted rods would land in the Derrick's bus. Each keeps a
dedicated field on the controller (`ModuleHatchElement`).

The **silo face** reads (offsets ±1 from the octagon's centre column, which is inside the
front row at EVERY tier — tier 1's front row is the narrowest, centre ±2):

```
y2   [ ]  u   [ ]  Q   [ ]     bottom FUEL course = the segment the cascade burns
y1   [ ]  y    p    Y   [ ]     drum course: water in | panel | water/steam out
y0   [ ]  t    ~   [ ]  [ ]     pad level: MAIN STATION + Maintenance
     a:  24   25   26
```

Why the Output Hatch sits there: waste water and exported steam are the RETURN LEG of the
same drum circuit the Feedwater feeds, so both sit on the drum course, one cell either side
of the Silo Control Block that reads the water level. In on the left, out on the right,
panel between them.

Casing budget note: those 6 silo-face sockets now cost up to 6 of the `-16` hatch allowance
in `minCasingsFor`. Checked: T1 actual ≈520 vs min 392; T5 ≈1808 vs min 1568. A blank silo
socket falls back to Radiation Proof at every tier and still counts.

The two chamber sockets sit one course above the chamber floor, at the feet of a player
standing on the open platform and directly under the eight injectors the propellant line
stocks. The chamber's front wall looks out over bare platform at every tier (front row
c = 14+edge/2, the cell in front of it c = 13+edge/2 = 17..21, and nothing is ever built
in the a 43..49 strip forward of the chamber), so neither can be entombed.

**Not reachable from the ground, and always was not:** the level-3 (`w`, front wall) and
level-4 (`x`, +a wall) injector sockets stand +8 and +7 above the platform. Shipped
geometry, unchanged by the relocation — scaffolding or flight is required to fit them.

## Steam Generator silo (optional piece, tier 1-5)
- Octagon edge n = 7+2t, corner cut CUT={-,2,2,3,3,4}, FRONT-ANCHORED: front row c=17,
  centered a=25 (spans a 25-n/2 .. 25+n/2, c 17 .. 16+n).
- Walls 2n tall from pad level, hemispherical dome n/2 on top. Wall chars by tier:
  Radiation-Proof (t1-2) / Europium-Reinforced (t3-4) / Radiant Naquadah Alloy (t5);
  radiation-proof band rings at bottom-up layers {2, (9+2n-1)/2, 2n-2}; 7 buttress ribs
  (front skipped). Dome shield: TS-Reinforced (t1-2) / Iridium TS (t3+).
- Interior: fuel pillar frames 8 tall (bottom-up layers 3..10), Rod Loading Ports layer
  11 — OPTIONAL (the Rod Input Bus feeds every pillar internally; ports are for
  hand-feeding only). Pillar caps per tier: 45/77/109/157/201.
- FACE PANEL: the silo's front face carries the MAIN STATION plus the six sockets that
  belong TO THE SILO — `t` `y` `p` `Y` `u` `Q`, all DECLARED FORWARD, on the courses
  listed in the socket table above. All other wall cells are PLAIN casings (no hatches,
  no hints).
- **MIGRATION (0.19.0 and earlier) — the one place a legacy plant needs player action.**
  That face used to be the plumbing frontage: 4 Energy Hatches across the drum course with
  the buses out at the corners. The two layouts conflict CELL FOR CELL — each puts a hatch
  where the other wants wall — so a standing legacy silo fails every tier probe and **no
  projection repairs it**: the projector measures the standing shell and never re-tiers or
  overwrites it. `checkMachine` now names the fault instead of failing quietly —
  `standingSiloTier()` re-walks the standing tier into the real error list and the panel
  prints `siloAbsentLine(tier)`, "Tier N Steam Generator IS STANDING and does NOT pass its
  check", with coordinates. **`/btsmigrate`** (perm 2, standing beside the main station)
  rewrites the front row's bottom three courses in place at the measured tier, dropping wrong
  cells' contents at the wall first; shell, ribs, dome, pillars and rods are never read or
  written. The old face's 4 Energy Hatches belong on the manifold back face (y4) now, where
  they are optional. So: a legacy PLATFORM needs nothing, a legacy TOWER needs 2 casings, and
  a legacy SILO needs exactly one command.

## The Spine duct (optional)
- 3x3 cross-section, y 0..2, rows centered on the silo mid-row c = 17+n/2 (rows
  c 16+n/2 .. 18+n/2), running a = 25+n/2+2 (flush past the +a rib) to a=42
  (length 16-n/2). Tungstensteel Turbine Casing shell, hollow bore.
- 4 Turbine Housing sockets in the bore at a-offsets 1,3,5,7 from the silo end;
  right-click sets CW/ACW; swirl S accumulates: riding x(1+0.08|S|) speed,
  opposing x(1+0.10|S|) efficiency; residual |S|^2 x5 heat/s.

## The Spiral chamber (optional)
- 7x7x12 box, a 43..49, rows tier-mid +/-3 (c 14+n/2 .. 20+n/2), y 0..11. TT Turbine
  Casing shell, interior: helix of 5-cell Light Concrete arcs stepping down the 5x5
  interior ring (3 cells advance per level) around a 3x3 open downcomer.
- TC-1 SOCKETS: 8 Precision Injectors on the walls, one per interior level 1..8,
  rotating faces BACK(+c) / -a / FRONT(-c) / +a from level 1 (mid-cell). The rotation starts on
  the back face deliberately: back-face sockets then land at levels 1 and 5 (world y 10
  and 6), clear above the gallery's 5-tall front wall - a front-first rotation entombed
  the level-7 socket. Spine Sensor in the roof's front-center cell; 1 Maintenance ('l')
  in the bottom wall's front-RIGHT corner.
  Each injector wall has its OWN shape char ('i' back / 'v' -a / 'w' front / 'x' +a) so
  the auto-fitter knows which way that socket's exposed face points (see FACING below).
- POINT-OF-USE SOCKETS: the chamber's own two supply hatches sit in its FRONT wall at
  interior level `SPIRAL_HEIGHT - 2` = world y 1 — `k` COOLANT at grid x=1 (platform
  a=44) and `P` bulk PROPELLANT at grid x=5 (a=48), both DECLARED FORWARD like the `w`
  injector and the `l` maintenance socket in the same wall. Coolant is fed to the thing
  that overheats; propellant is fed directly under the eight injectors it stocks.

## The Turbine Generator (optional; TC redesign of the Rotor Assembly)
- Rotor gallery 7 wide x 5 tall x 15 long, a 43..49, c (21+n/2)..(35+n/2), hollow bore,
  8 rotor STAGE housings on the centerline at bore height y2: gallery rows 1,3,5,7,9,
  11,13 plus one hanging in the manifold mouth (row 15). Each housing (a tile entity)
  seats a placeable TURBINE ROTOR item (10 durability; -5% stage factor per damage
  point; shatters at 10; sneak-click empty-handed extracts, wear preserved).
- OUTPUT MANIFOLD at the BACK end (+c): 7x7x3 (y 0..6), c (36+n/2)..(38+n/2), hollow; its
  front plane is walled except the mouth matching the gallery bore (a 1..5, y 1..3);
  4 Dynamo sockets mid-height on the back face (y 3, a 1,2,4,5).
- TC-1/2 SOCKETS: TURBINE CONTROL BLOCK on the MANIFOLD BACK FACE (last row, a 3,
  y 2) with 1 Maintenance directly below it (y 1) - the gallery's front wall is
  entombed against the Spiral's back wall, so a redstone line could never reach a
  control block there. All other cells plain TT casings.
- TC-5 LASER SOCKETS ('L'): 4 cells on the SAME manifold back face, two rows above the
  dynamos - y 5, a 1,2,4,5. POINT OF USE added the four ENERGY HATCH sockets ('r') in the
  course BETWEEN them, y 4, same four columns, so the back face now reads, top-down:
  `8888888 / 8LL8LL8 / 8rr8rr8 / 8mm8mm8 / 888q888 / 888n888 / 8888888`.
  Chosen because nothing is ever built behind the manifold at ANY tier, so all twelve
  look straight out at open sky and a cable or a laser pipe reaches every one of them; the
  a=3 column stays casing as a clean spine over the control block, a=0/6 are the frame.
  **The Energy Hatches are OPTIONAL — not a structure demand** (C1) — **but since 0.21.0
  they ARE a load** (`C5`). Do not read "optional" as "furniture", and do not delete the `r`
  socket as unused: `moduleSupplied()` calls `drainEnergyInput()` once per tick for each of
  the two D-17 **process modules** — the Extraction Derrick at `DERRICK_EU_PER_TICK`
  (`TierEU.RECIPE_EV` = 1,920 EU/t) and the Enrichment Annex at `ANNEX_EU_PER_TICK`
  (`TierEU.RECIPE_HV` = 480 EU/t). That is the only EU the class consumes, and an empty `r`
  socket on a plant carrying either module means **neither module ever turns over**.
  What is still true, and is why the socket stays optional: the **reactor and turbine** never
  draw EU. `checkProcessing()` sets `lEUt = 0` in all three success branches and nothing in
  the steam chain reads `mEnergyHatches`, so an empty `r` socket cannot change a single number
  the *reactor* produces. `checkHasEnergyHatch(errors)` is therefore
  **removed**, not merely gated: `galleryBuilt` flips true the instant the last gallery
  Turbine Housing is placed, three steps before the questbook asks for the hatches, so the
  gated form still un-formed a running plant mid-build — and un-forming arms the 20-minute
  decay-heat obligation. The empty sockets are reported instead, by `energyHatchNote(...)`,
  as a grey non-fatal line in the OUTPUT group — which turns **RED** when a process module
  stands with no Energy Hatch fitted at all.
  They take TecTech LASER SOURCE HATCHES (HatchElement.LaserSource -> MTEHatchDynamoTunnel
  -> mExoticDynamoHatches); /btsbuild fits the ZPM 4,096 A/t one (536,870,912 EU/t each).
- RPM model: redstone rising edge on the Turbine Control Block starts a cycle
  (consumes a 20xL/t steam slug + opens the +/-2t injector window); RPM gain =
  176 x P x timed/8, decay 0.5%/t, redline 12,000; EU/t = L/t x speed x eff x
  (RPM/1000) x avgRotorFactor (avg over all 8 sockets, so it carries stages/8),
  clamped to DYNAMO + LASER capacity (and to 2^31-1, the int the GT injector
  accumulates in); coolant ceil((200x(RPM/1000)^2 + |S|^2x5/20)/Q) per tick.
- TC-7 INTERLOCK: a cycle is only accepted while a SIMULATED drain says the chamber can
  cool the RPM that cycle would reach and the injectors are not dry; otherwise the RPM is
  held and the reason printed. Coolant lost at speed = protective spin-down (x0.95/t), not
  a seizure. A cooled chamber carries the residual swirl away instead of heating the core.

## SOCKET FACING (canonical - do NOT re-derive by scanning neighbours)
Every socket in these shells has AIR ON BOTH FACES (hollow interior one side, sky the
other), so "first air neighbour" cannot tell outward from inward and turned one of any
two opposite walls around on every rotation - a hatch facing inward renders as the
casing it was textured with, which is why placed control blocks looked absent. The
outward normal is therefore DECLARED per socket char, relative to the controller, and
resolved through ExtendedFacing at placement time:
- silo face 't','y','p','Y','u','Q'            -> FORWARD (getRelativeForwardInWorld)
- spiral front wall 'k','P'                    -> FORWARD
- manifold back face 'm','r','L','n','q'       -> BACK
- spiral roof sensor 'j'                       -> UP
- spiral floor corner 'l'                      -> FORWARD
- spiral injectors 'i'/'v'/'w'/'x'             -> BACK / RIGHT / FORWARD / LEFT
Structure space = the frame the shape strings are written in: shape ROW index runs +c
(relative BACK), the CHAR index inside a row runs +a (relative LEFT), the LAYER index
runs +b (relative DOWN). ExtendedFacing: a = getRelativeLeftInWorld, b = ...Down,
c = ...Back.

Point of use makes this rule matter MORE, not less: the silo face (FORWARD) and the
manifold back face (BACK) are now both socket-bearing walls facing opposite ways, so a
neighbour scan would turn exactly one of them inward on every rotation. The normal is
DECLARED per socket char. The rule has no exceptions.

## Cooling Towers (optional CHECKED pieces of the main controller — A3)
- 17x17x20 hyperboloid (base r7.5, waist r4.6 at 62%, top r5.6), Light Concrete 'O'
  shell on a Solid Steel 'R' base ring. Both stand on the `-a` strip (the platform strip
  to your LEFT as you face the silo). FRONT tower body a 0..16 c 0..16 (offsets 25, 19,
  17); BACK tower body a 0..16 c 33..49, rows reversed (offsets 25, 19, -16). D-21 retired
  the docks' NW/SW labels with the docks: `a` is the controller's relative LEFT, so a
  compass word is only ever true for one facing — and "NW" was not true for that one
  either (it is north-EAST on a north-facing plant).
- Derived bill per tower: **736 Light Concrete + 52 Solid Steel Machine Casing.**
- They are pieces now, not modules: no controller, no dock marker, no maintenance hatch —
  the two cells that used to hold the module controller are plain base ring. Detected on
  every structure walk exactly like the Spine and the Spiral. Effects per standing tower
  (max 2): feedwater draw x0.5, passive heat decay x2, decay-heat demand x0.5.
- **MIGRATION (R2).** `towerShape()` emits TWO controller-anchor `'~'` cells per tower in
  its bottom layer — `base[0][8]` and `base[frontZ=1][8]`. `remapShape(..., "NS~",
  "ORR", ...)` turns both into Solid Steel base ring, and `remapShape` now THROWS if a
  `'~'` ever survives the remap. On a platform whose towers were raised by the RETIRED
  module controller those cells hold that controller and open air instead, so the ring is
  broken and the tower is not detected. Per tower, by hand, at the platform walking
  level: FRONT tower (a 8, c 1) — break the retired controller, place Solid Steel — and
  (a 8, c 0), which was air; BACK tower (a 8, c 48) and (a 8, c 49), same. **4 casings for
  a two-tower plant.**
  A 0.19.0 Maintenance Hatch left in the base ring may STAY — `towerRingElement()` chains
  Solid Steel with a tile adder that answers for one — but it is **tolerated, never
  registered**: nothing a tower holds reaches `mMaintenanceHatches`, so the plant's three
  maintenance sockets stay the silo face, the Spiral floor and the manifold and only those.
- `MTEModuleCoolingTower` / `MTEModuleTurbineHall` / `MTEPlantModuleBase` are RETIRED
  museum pieces: MTE ids still registered so existing worlds load, recipes removed,
  tooltips point at the main station. `PlantModuleElement` is deleted.
- Turbine Hall (RETIRED, museum): 5x4x9 — no longer part of any projection.

## The four D-17 PLANT MODULES (optional checked pieces — D-22)

Built and checked exactly like the Cooling Towers: pieces of the one controller, raised by
the one projection, detected on every structure walk, no dock and no second controller.
Each site is a **fixed platform rectangle** — no tier term anywhere in its offsets — so a
module never moves when the silo is re-tiered.

| module | a | c | y | bill | sockets | effect |
| --- | --- | --- | --- | --- | --- | --- |
| **Vent Stack** 7x7x20 | 2..8 | 21..27 | 0..19 | 161 Clean Stainless Steel Machine Casing + 152 Filter Machine Casing | none | decay-heat window **24,000t → 12,000t** (20 min → 10) and meltdown radiation **36,000t → 18,000t** (30 min → 15) |
| **Switchyard** 21x6x6 | 20..40 | 44..49 | 0..5 | 261 Solid Steel Machine Casing + 90 Steel Frame Box | none | lifts the **100,000,000 EU/t** grid soft cap |
| **Extraction Derrick** 7x7x20 | 33..39 | 4..10 | 0..19 | 144 Solid Steel Machine Casing + 80 Steel Frame Box + 19 Steel Pipe Casing + 1 Output Bus | 1 Output Bus (front wall, y1, FORWARD) | every 200t: 6 Uraninite + 1 Thorium, +1 Tantalum every 4th cycle; **1,920 EU/t** (`TierEU.RECIPE_EV`) |
| **Enrichment Annex** 5x15x6 | 17..21 | 0..14 | 0..5 | 291 Clean Stainless Steel Machine Casing + 13 Steel Pipe Casing + 3 sockets | Input Bus + Input Hatch + Output Bus (+a wall, y1, LEFT) | every 600t: 8 Uraninite + 2,000 L Hydrofluoric Acid → **2 Yellowcake** + 2 Thorium; **480 EU/t** (`TierEU.RECIPE_HV`) |

Total module buildout: **1,215 blocks** (313 + 351 + 244 + 307). Every count in the table
comes from `questbook/gen-bom.ps1`, which re-implements each shape cell by cell and fails
loudly if the Java moves underneath it; nothing here is hand-counted.

Clearance verified cell by cell at **all five tiers** against platform, silo (incl. ribs and
dome), spine, spiral, gallery, manifold and both towers: **0 overlaps, 0 cells off the
platform, 0 of the eight legacy dock cells covered, 4/4 declared socket faces open.** The
one cell reported twice by the checker is `(25, 17, 0)`, the controller anchor, which both
the platform pad layer and the silo front row declare as `~` — the same block.

Re-verified independently in `docs/mockup/unit-one.html`, which now draws all four modules:
its `__btsAudit()` hook measures the footprints back out of the rendered voxel world and
re-runs the overlap test at whichever tier is on screen. Measured at tiers 1, 2, 3, 4 and 5:
`vent 313 · yard 351 · derrick 244 · annex 307`, bounding boxes exactly the a/c/y columns
above, `overlap 0`, at every tier.

### How a module is detected

`checkMachine` walks each module at its fixed offset through `checkPiece` and keeps the
result in that module's own `pieceErrors` list, so **nothing a module reports can un-form
the plant**. A module that is simply absent is not an error; a module with blocks standing
that will not form prints its reason through `pieceFault()` on the ungated `moduleLine`,
the same way the Cooling Towers do. Because module placement uses only compile-time
constants, `construct(stack, hintsOnly = true)` draws all four correctly CLIENT-side, and
every module field the GUI reads is a sync value (`gridCapEnabled` included — a Forge
config is per-side).

### What it costs to skip one

| skipped | consequence |
| --- | --- |
| Vent Stack | decay-heat obligation stays 24,000t and the meltdown radiation field stays 36,000t |
| Switchyard | everything above 100,000,000 EU/t is discarded — on a tuned tier-5 plant, most of the output |
| Extraction Derrick | no ore cycle. Never a structure demand, never stalls the plant |
| Enrichment Annex | no yellowcake cycle. Never a structure demand, never stalls the plant |

Neither of the two invariants the shipped sockets rest on is touched: nothing is built in
the `a 43..49` strip in front of the Spiral chamber, and nothing is built behind the output
manifold at any tier (the Switchyard stops at `a 40`; the manifold is `a 43..49`).

**No cell of this plant ever overwrites a standing tile entity** (`C1`). A cell that already
satisfies its element is left alone; a cell holding a *tile entity* is skipped and reported
rather than written. Three wrappers carry the rule and between them cover every write path
in `getStructureDefinition()`:

| wrapper | covers |
| --- | --- |
| `protectedElement` | the module material chars `V W Z M X` (leave-if-already-valid + refuse) |
| `autoFit` | **every socket in the plant**, module sockets `b e f g` included |
| `noOverwrite` | every remaining plain char — `B I A C D E G O T 5 8 H d` — plus the tower ring `R` and the four raw `world.setBlock` elements (fuel pillar, rod port, both housing seats) |

`autoFit` used to refuse only for `IGregTechTileEntity`, which was the hole: both of its
remaining paths write (`placeGtMachine` calls `setBlockToAir` on what stands there, and the
chained fallback is `ofBlock`), so an AE2 ME Interface, a chest, a hopper or a conduit parked
on the Derrick's bus cell (`a36, c4, y1`) or one of the Annex's three socket cells
(`a21, c3|c7|c11, y1`) was voided by one right-click of a projector. The module sites are
virgin concrete on a plant this version raised, but not necessarily on one a player has been
running for weeks — and the same is true of the platform under every other piece.

The two turbine-housing elements matter for a second reason: a Turbine Housing carries
`TileTurbineHousing`, and that tile is where the **rotor** lives. Housing meta encodes spin
direction, so laying a blank meta-0 housing over a standing meta-1 one was a real block
replacement — it voided the rotor in it.

**A committed batch is never destroyed** (`C2`). The Annex consumes its feedstock **up front**,
at `annexProgress == 0` — 8 Uraninite Dust and 2,000 L of Hydrofluoric Acid — so *any* non-zero
`annexProgress` means the player has already paid and nothing has been handed back. An
un-formed module therefore **SUSPENDS** its batch where it stopped; it does not zero the
counter. That matters because a structure check fires on every neighbour update: breaking one
Clean Stainless casing to route a pipe used to eat the batch outright. The Derrick suspends on
the same terms (its stake is the EU already paid into the load, up to 200t × 1,920 EU/t).
A suspended batch survives a **reload** via `saveNBTData` and a **wrench** via `setItemNBT` —
tile NBT alone would have dropped it the moment the controller came out — and the OUTPUT
group prints a `SUSPENDED at n/600t` line naming the feedstock being held, so the player can
see it is theirs. Only the *product* Output Bus is required to finish a committed batch; the
feed bus and acid hatch have already been drawn from.

**Deposits are all-or-nothing, and ME buses work** (`C3`). `storeAll` asks the bus through its
own `IOutputBusTransaction` rather than counting empty slots. Slot counting made an **ME Output
Bus impossible to pay into, forever** — `MTEHatchOutputBusME` passes `aSlots = 1` and that one
slot is the *storage cell* slot, so `empty` was 1 or 0 against a `wanted` of 2 — and it also
stalled any load whose items would simply have merged into a part-full slot. Every bus
implementation stages into a copy (a snapshot inventory, or the ME cache counter) and only
writes on `commit()`, so a load that does not fit is abandoned without a single item moving,
and the module holds it and retries. Item **locks** still refuse from inside `storePartial`,
as before.

**Every module socket is validated before use** (`C4`). `MetaTileEntity.isValid()`, not
`!= null`: a broken hatch keeps a non-null base whose `isDead()` is true until the next
structure check clears the field, and in that window the Derrick was writing product into a
dead tile entity. Single references go through `live(...)`, lists through
`GTUtility.validMTEList`.

**The two process modules draw EU** — the first and only things in this plant that do —
through the manifold's four ENERGY HATCH sockets. Those are still not a structure demand
(C1: the reactor and the turbine run without them and un-forming a plant over furniture
arms the decay-heat obligation); `energyHatchNote` now says which of the two truths applies.
The draw is per tick and not a lump sum, because a 409,600 EU lump is bigger than an IV
Energy Hatch's buffer and a perfectly powered plant would have stalled forever.

### The Switchyard soft cap (D-17 6, live balance change)

Without a Switchyard, the plant may inject at most `GRID_SOFT_CAP = 100,000,000` EU/t. The
reactor still derives its full figure — `lastEUDemand` is untouched — and the surplus is
discarded. Implemented as `outputLimit() = min(outputCeiling(), gridCeiling())`, and
`onRunningTick` clamps to *that*: the cap can only ever LOWER what is injected, never raise
it above what the fitted hatches absorb, which is the condition
`addEnergyOutputMultipleDynamos` calls `explodeMultiblock()` over.

Config flag `switchyardSoftCap` (default **true**) is the single setting that turns it off.
Four panel states, all printed: cap disabled / yard standing / yard missing but under the
cap / yard missing and discarding N EU/t with the bill for the yard in the line.

## One projection covers everything (A4)
`construct()` / `survivalConstruct()` build, in order: platform -> silo (tier) -> spine
duct -> spiral -> gallery + manifold -> FRONT tower -> BACK tower -> Vent Stack ->
Switchyard -> Extraction Derrick -> Enrichment Annex. There is no build-only
"shell" leniency left anywhere and no second controller to place: what the projection
draws is exactly what the structure check wants.

**R1: Build Tier only ever decides what to RAISE.** Both `construct()` and
`survivalConstruct()` use `reactorTier > 0 ? reactorTier : tierFromTrigger(stackSize)`,
where `reactorTier` is written by the silo probe and describes the silo that is
PHYSICALLY STANDING whether or not the plant formed. Before the fix, any I/O change that
un-formed a pre-existing plant left `mMachine` false with `reactorTier` still correct, and
the projection tried to raise a Build-Tier-1 silo inside a standing tier-5 one. Consequence
of the fix: **a standing silo is never re-tiered — dismantle it first.**

**What the projector does NOT place**, in survival: the 12 Turbine Housing cells (4 spine
`'H'`, 8 gallery `'9'`) return `PlaceResult.SKIP` and stay AIR; every hatch/injector/
sensor/control-block socket chains past the hatch adder to its plain casing; the fuel
pillar and rod-port cells are optional interior and stay air. Creative placement
(`/btsbuild`, projector in creative) auto-fits the real machines through `autoFit`.

| piece | offsets (A, B, C) | platform footprint |
| --- | --- | --- |
| platform floor | 25, 0, 17 | a 0..49, c 0..49, y -1..0 |
| silo (tier t) | edge/2+1, siloVOff(t), 1 | a 25±edge/2 (+rib), c 17..16+edge |
| spine duct | -(edge/2+2), 2, 1-edge/2 | a 25+edge/2+2..42, c 16+edge/2..18+edge/2 |
| spiral | -18, 11, 3-edge/2 | a 43..49, c 14+edge/2..20+edge/2 |
| gallery + manifold | -18, 6, -4-edge/2 | a 43..49, c 21+edge/2..38+edge/2 |
| tower (front) | 25, 19, 17 | a 0..16, c 0..16, y 0..19 |
| tower (back) | 25, 19, -16 | a 0..16, c 33..49, y 0..19 |
| Vent Stack | 23, 19, -4 | a 2..8, c 21..27, y 0..19 |
| Switchyard | 5, 5, -27 | a 20..40, c 44..49, y 0..5 |
| Extraction Derrick | -8, 19, 13 | a 33..39, c 4..10, y 0..19 |
| Enrichment Annex | 8, 5, 17 | a 17..21, c 0..14, y 0..5 |

## Bills of materials — DERIVED, never typed

`questbook/gen-bom.ps1` re-implements every shape function above — the four module shapes
included, each guarded by its own `Assert-Source` literal — tallies each cell through
the structure definition's element table, and guards itself against the Java changing
(it reads `MTEReactorCoreMk1.java` / `MTEModuleCoolingTower.java` and throws if any
constant or socket-placing line it was ported from has moved). `gen-questbook.ps1`
dot-sources it, so no count in the questbook is a remembered number — and a script run of
`gen-bom.ps1` re-renders `BTSBom.java` and rewrites `DefaultQuests/` in the same step, so
`/btskit` and the book cannot go out of step either (T1). Run it to print the whole table.
Current output:

| piece | bill |
| --- | --- |
| platform | 2,251 Light Concrete + 249 Tungstensteel Reinforced Block |
| silo T1 | 520 Radiation Proof + 113 Tungstensteel Reinforced + 360 Reactor Fuel Column (45 pillars) + 45 Rod Loading Port |
| silo T2 | 812 Radiation Proof + 193 Tungstensteel Reinforced + 616 columns (77 pillars) |
| silo T3 | 963 Europium Reinforced + 105 Radiation Proof + 257 Iridium Reinforced + 872 columns (109) |
| silo T4 | 1,343 Europium + 129 Radiation Proof + 381 Iridium Reinforced + 1,256 columns (157) |
| silo T5 | 1,667 Radiant Naquadah Alloy + 141 Radiation Proof + 497 Iridium Reinforced + 1,608 columns (201) |
| spine duct | Tungstensteel Turbine Casing 96 / 88 / 80 / 72 / 64 (T1..T5) + 4 Turbine Housing |
| spiral chamber | 326 Tungstensteel Turbine Casing + 50 Light Concrete + 8 Precision Injector + 1 Spine Sensor + 1 Maintenance + 2 Input Hatch |
| gallery + manifold | 408 Tungstensteel Turbine Casing + 8 Turbine Housing + 4 Dynamo + 4 Energy + 4 Laser Source + 1 Turbine Control Block + 1 Maintenance |
| cooling tower (each) | 736 Light Concrete + 52 Solid Steel Machine Casing |
| Vent Stack | 161 Clean Stainless Steel Machine Casing + 152 Filter Machine Casing |
| Switchyard | 261 Solid Steel Machine Casing + 90 Steel Frame Box |
| Extraction Derrick | 144 Solid Steel Machine Casing + 80 Steel Frame Box + 19 Steel Pipe Casing + 1 Output Bus (IV) |
| Enrichment Annex | 291 Clean Stainless Steel Machine Casing + 13 Steel Pipe Casing + 1 Input Bus (IV) + 1 Input Hatch (IV) + 1 Output Bus (IV) |

The silo's Radiation Proof figure at T3-T5 is the three radiation band rings, which stay
radiation proof at every tier. Fuel columns and rod ports are OPTIONAL interior.

## Color legend for mockups
concrete #9aa0a2, shield TS #9d8f6f, rad-proof #8a8f5a, europium #7a9b6a, radiant naq
#6a5a9b, solid steel #5d6b78, TT turbine casing #4a6d99, stainless #b8bfc4, fuel rod
#76c44e, housing CW #4a86c8 / ACW #8a5ac8, sockets: energy #d9a521, dynamo #d94a21,
injector #21b8d9, sensor #d921b8, control blocks #e8e8e8, maintenance #c87a4a,
laser source #d921d9.
