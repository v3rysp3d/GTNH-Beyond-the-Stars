# Kerr redesign: the hole controller and separate module machines

Spec for the `kerr-redesign` branch, written 2026-10-12. It covers the ruling in the build brief (item 9):
"this multiblock is EXTREMELY hard to understand". No Java has been written for it yet. Where this file and
`singularity-ladder-build.md` §3.1 / §3.2 / "Contract additions (Kerr)" disagree, this file wins for the redesign.
The judgement calls are logged as questions B-33..B-37 in `singularity-ladder-questions.md`.

**Status (2026-10-12, built on `kerr-redesign`):** R1 (exporter), R2 (KEE) and R3 (modules) are built and compile
against RC-2; the render (R4) is not, only its per-machine switch. Where the build differs from this spec it is
logged as B-38 (the compressor controller cell), B-39 (pausing, the power switch, the KEE GUI) and B-40 (render hooks,
recipe ids, migration). The "As built" section at the end lists the files.

The GUI mockup is a separate page (local work folder `kerr-gui/kerr-gui.html`, not committed). It shows the KEE screen,
a module screen and the link diagram.

Everything quoted from GT was read in the GT 5.09.54.205 (RC-2) sources (`port-tools/rc2/src`):

- `MTEForgeOfGods` / `MTEBaseModule` (TecTech godforge);
- `TileEntitySpaceElevator` / `TileEntityModuleBase` (gtnhintergalactic);
- `MTEMultiBlockBaseGui`, `MTEBaseModuleGui`, `FuelConfigPanel`, `TileEntityModuleBaseGui`;
- `BlackholeRenderer`, `RenderingTileEntityBlackhole`, `AbstractRenderingTileEntity`, `BlockRenderer`,
  `ShaderHandle` / `FrameMatrices`, the `blackhole.vert*.glsl` shaders;
- `MTEBlackHoleCompressor`.

Widget classes were checked in the ModularUI2 2.3.92 dev jar.

---

## 1. Responsibilities

| | **Kerr Ergosphere Engine (KEE)**, MTE 28114 | **Kerr module** (Compressor 28117, Extractor 28118, more later) |
|---|---|---|
| Is | the hole and its cage | a separate multiblock machine that stands in one of the cage's arcs |
| Structure | `kerr_t1`, or `kerr_t2` once the Density Upgrade is in (unchanged) | its own arc, exported in its own frame (§6) |
| Controller | governor arc, front centre (unchanged) | the module's own controller, on the outer face of its arc |
| Does | seed / open / close the hole, the mass setpoint, the governor and bolt buses, Hawking bleed, stability, containment power, utility hatches, the Density Upgrade (T2), the render | runs recipes from its own recipe maps |
| Hatches | energy (1-2), maintenance, 8 bolt buses (private), 8 optional BHC utility hatches (all unchanged) | its own input / output buses and hatches and its own energy hatch, all in GT's **standard** lists |
| Recipes | **none**: no recipe map, no machine modes, no `ProcessingLogic` | Compressor: `compressorRecipes` + `neutroniumCompressorRecipes` + `KerrRecipeMaps.DENSITY`. Extractor: `extractorRecipes` (items only, Q-9/Q-22). |
| GUI | MUI2 hole console (§4.1) | the standard GT multiblock GUI, plus a "Kerr link" status (§4.2) |
| Screwdriver | not needed for anything | not needed for anything |
| Needs | its structure and power | a formed KEE **with an open hole**, at its slot's fixed position (§2) |

The KEE never needs to be reconfigured to use a module. A module never changes the hole.

What gets deleted from today's `MTEKerrEngine`:

- machine modes (`MODE_*`, `setMachineModeIcons`, `nextMachineMode`, `onScrewdriverRightClick`);
- `bindModuleIO` / `startRecipeProcessing`, `createProcessingLogic`, `getRecipeMap` / `getAvailableRecipeMaps`;
- the module pieces and the `c q x k` elements in its definition;
- the `Compressor*` / `Extractor*` hatch elements;
- `KerrModule.java`.

`KerrHatchElement` keeps only `BoltBus` and `Utility`.

**DENSIFY needs no mode any more.** The Compressor module searches its three maps together. A DENSITY recipe is
accepted only while the linked KEE has the Density Upgrade and M ≥ 128 SM (`validateRecipe`). The maps' inputs do not
overlap: DENSITY takes RHSP + molten SpaceTime + bolts, the other two single solid items. So recipe inputs pick the
map, as today's COMPRESS mode already does across two maps.

---

## 2. Linking: how a module finds its KEE

### 2.1 What GT does

Both of GT's module systems put the module **controller inside the main structure as a socket**:

- **Godforge.** `MTEForgeOfGods` registers each `MTEBaseModule` through a hatch element (`moduleElement.Module`, the
  `J` cells) during its walk. Every cycle it **pushes** `connect()` / `disconnect()` and parameters (heat, parallel,
  speed, voltage) into each module. The module's `onPostTick` does nothing (`super` is not even called) while
  `isConnected` is false, and `isConnected` is saved in the module's NBT.
- **Space Elevator.** The same idea: `ProjectModuleElement` in the elevator's walk, `connect(this)` on a timer, and EU
  pushed into each module's buffer.

Neither fits us cleanly.

- Our module controller cell lies in the arc, not in the cage piece. Making it a KEE socket would put a module MTE
  into the KEE's hologram, survival build and NEI preview (trap 4), and a module would only count once the KEE walks.
- A pushed `isConnected` flag persisted in NBT goes stale across unloads (a module can load "connected" while its
  KEE is unloaded).

### 2.2 Our scheme: both sides compute the other from the shared geometry (pull, no stored link)

The exporter knows exactly where every module controller sits relative to the KEE controller. It writes a **slot
table** into `manifest.json` (§6):

```
machines.kerr.slots.<slot> = { piece, rel: [a,b,c] /* module controller from the KEE controller, KEE frame */,
                               keeRel: [a,b,c]    /* KEE controller from the module controller, module frame */,
                               turns: 0..3        /* module facing = KEE facing rotated this many quarter turns */ }
machines.kerr.centre = { rel: [0,0,38] }          /* hole centre from the KEE controller, KEE frame */
```

- **A new enum `KerrSlot`** (`COMPRESSOR`, `EXTRACTOR`) reads that table and names the module MTE class for each
  slot. Adding a module adds one entry.
- **Module side.** `MTEKerrModuleBase.findKee()` turns `keeRel` into a world position with its own `ExtendedFacing`.
  It is linked if **all** of these hold:
  1. the chunk is loaded (`world.blockExists`);
  2. the TE there is an `MTEKerrEngine`;
  3. the KEE's facing equals the module's facing rotated back by `turns`.

  The result is cached in a `WeakReference<MTEKerrEngine>` and re-checked every 20 ticks and before every recipe
  start. Nothing about the link is saved in NBT.
- **KEE side.** `MTEKerrEngine.slotStatus(slot)` does the mirror lookup (`rel`). The module there must be formed and
  must resolve back to this KEE. It runs every 20 ticks for the GUI's module list. The KEE never stores modules
  either, so "KEE lists connected modules" is derived, never registered.
- **Uniqueness.** At most one module per slot. Two KEEs side by side cannot confuse a module, because it only looks
  at its one cell. A module of the wrong kind in a slot (an Extractor module built in the compressor arc) fails the
  module's own walk, because its arc shape does not match.

`KerrHole`, a small read-only view, is what a module reads from its KEE:

```java
interface KerrHole {
    boolean isFormed();
    boolean isHoleOpen();
    double mass();
    boolean densityInstalled();
}
```

`MTEKerrEngine` implements it. The modules never call anything else on the KEE.

### 2.3 Edge cases

| Situation | Module | KEE |
|---|---|---|
| KEE chunk unloaded | status "KEE not loaded"; **paused** | n/a |
| Module chunk unloaded | n/a | slot shows "not loaded" |
| KEE unformed (block broken, rails swapped, chunk gap) | status "KEE not formed"; paused | the hole freezes as today (B-8) |
| KEE broken | the cached ref is dead; status "no KEE"; paused | n/a |
| Hole closed (Collapser, evaporation, GUI) **mid-recipe** | **paused, recipe kept** (Godforge pattern, B-34): progress and EU stop, and the recipe resumes when the hole reopens | `Collapse` in the GUI warns how many modules will pause |
| Mass changes mid-recipe | parallel and speed were fixed at recipe start (GT's suppliers run at start), so there is no anti-cheese to write | n/a |
| Module structure broken | the module unforms like any GT multi; it does not touch the KEE | slot shows "not formed" |
| Two KEEs nearby | each module looks at one cell; no ambiguity | each KEE looks at its own slot cells |
| KEE facing changed (wrench) | the facing check fails: "KEE faces the wrong way" | n/a |

"Paused" is implemented as Godforge does it: `MTEKerrModuleBase.onPostTick` calls `super.onPostTick` only while
linked. While unlinked it runs only its own 20-tick link check, so no EU is drawn and no progress is made. The
module's GUI still opens, because MUI2 sync does not depend on the post-tick.

---

## 3. What the hole gives modules, and what modules cost the hole

**Gives (numbers unchanged from B-8, now read by the module from `KerrHole`):**

- **Parallel** = 8 × `getTierExtended(module max input EU)` × ⌊√M⌋. The voltage tier is the **module's own** energy
  hatch tier. At UIV (tier 11): 16 SM → 352, 64 SM → 704, 512 SM → 1,936.
- **Speed** = clamp((3200 / M) / 50, 1, 4). That is ×4 at 16 SM or less, and ×1 at 64 SM or more.
- **DENSITY recipes** need KEE T2 and M ≥ 128 SM (Compressor module).

**Requires:** an open hole for **every** recipe (B-33). The current Kerr let plain compressor recipes run with the
hole closed. RC-2's own Black Hole Compressor refuses every recipe while its hole is closed
(`validateRecipe`: `blackHoleStatus == 1` gives `NO_BLACK_HOLE`). So "a hole being open to run" (my words)
is also BHC parity.

**Costs the hole: nothing** (default, as asked). A module never changes mass, bleed or stability. B-37 lists the
alternative (each running module adds a share of bleed).

**The KEE's own power (new; B-35, PROPOSED).** The hole draws **containment power**: 8,388,608 EU/t (1 A UEV)
while open, nothing while closed. It is drained each tick from the KEE's energy hatches. If the drain fails, the
governor fires no bolts that tick, so the hole bleeds and visibly destabilises (stability 0 %, the render wobbles).
It evaporates at 2 SM, as today. This gives the KEE's energy sockets, and the GUI's power line, a meaning now that
the KEE runs no recipes. Alternative: 0 EU/t, which drops the KEE's energy hatches.

**Stability**, as shown and fed to the render, is the governor's coverage: mass fed ÷ mass bled over the last 20
ticks, capped at 100 %. Holding the setpoint gives 100 %. Out of bolts, short on bolt cap, or out of power gives
less, and the GUI then shows the time to evaporation without bolts: (M³ − 8) / 9.6 ticks, which is 21 s at 16 SM and
22.8 min at 64 SM.

---

## 4. GUIs (ModularUI2, GT's `MTEMultiBlockBaseGui`)

Both machines set `useMui2() = true` and return their GUI from `getGui()`. This is the pattern of
`MTEReactorSiloGui` in this addon, and of GT's own module GUIs. The KEE's GUI looks like any GT multi: the grey
panel, the dark terminal with its text list, the gap row of round-cornered buttons, the player inventory and the
button column. Side panels open beside it, like Godforge's `FuelConfigPanel`.

**Nothing a player needs takes a screwdriver.** The screwdriver override is deleted. Every control is a GUI
widget, and right-clicking with the Density Upgrade still installs it.

### 4.1 KEE: `MTEKerrEngineGui extends MTEMultiBlockBaseGui<MTEKerrEngine>`

**Sync values** (`registerSyncValues`), all server-authoritative; setters clamp on the server:

| Key | Type | Notes |
|---|---|---|
| `holeOpen` | `BooleanSyncValue` | |
| `mass` | `DoubleSyncValue` | |
| `setpoint` | `IntSyncValue` | settable, clamped 2..64 (T1) or 2..512 (T2) |
| `bleed` | `DoubleSyncValue` | SM/t |
| `boltsPerTick`, `boltCap`, `railMk` | `DoubleSyncValue`, `IntSyncValue`, `IntSyncValue` | |
| `boltStockSm`, `boltStockSeconds` | `DoubleSyncValue`, `LongSyncValue` | bolt stock as SM and as seconds at the current feed rate |
| `stability` | `DoubleSyncValue` | 0..1 |
| `evapSeconds` | `LongSyncValue` | time to evaporation without bolts |
| `containmentEut`, `powerOk` | `LongSyncValue`, `BooleanSyncValue` | |
| `density` | `BooleanSyncValue` | |
| `seeds`, `collapsers` | `IntSyncValue` | counts in the bolt buses |
| `autoSeeds` | `BooleanSyncValue` | settable |
| `render` | `BooleanSyncValue` | settable |
| `slot.<name>` | `StringSyncValue` per `KerrSlot` | `state\|parallel\|speed\|x,y,z` |

**Main panel** (198 wide, standard height, player inventory bound):

- **Terminal** (`createTerminalTextWidget`: `super`, then):
  1. `Hole OPEN · Kerr T1`, in green (closed: grey, with "insert a Black Hole Seed into a bolt bus, or press Open").
  2. **Mass bar**: a `ProgressWidget` (mass / max setpoint) with an overlay drawable that ticks the setpoint, and the
     label `16.0 / 64 SM`.
  3. `Setpoint 16 SM · bleed 0.0125 SM/t`.
  4. `Governor 0.80 / 4 bolts/t (Mk I rails)`.
  5. `Bolt stock 288 SM · 19m 12s`.
  6. `Stability 100 % (holding)`, or in red `62 % (drifting, evaporates in 14 s without bolts)`.
  7. `Containment 8.39M EU/t`, or in red `Containment power LOW: governor stopped`.
  8. `Modules: Compressor RUNNING · Extractor IDLE` (coloured).
- **Gap row, left:** the setpoint control, a `TextFieldWidget` with `numbersLong(2, max)` and `scrollValues(1, 8, 1,
  64)` (the `MTEHatchInputBusCompressedGui` idiom), flanked by `−` / `+` buttons. No void / batch / separation /
  lock / mode buttons (`shouldDisplay*` return false; `supportsMachineModeSwitch` false).
- **Gap row, right:** **Hole control** (opens panel A), **Modules** (opens panel B), a render toggle (eye icon,
  synced `render`) and GT's power-panel button. These are in the gap row because the button column has room for only
  4 buttons in its 76 px.
- **Button column** (bottom to top): the controller slot, power switch, structure update (GT's default).

**Panel A, "Hole control"** (`IPanelHandler` in `panelMap`, opens to the right; 110 × ~150):

- a `SliderWidget` bound to `setpoint` (2..max, whole SM), with the number field mirrored;
- the T1 / T2 line: "Density Upgrade: installed", or "not installed: right-click the controller with it";
- **Open hole**: enabled only when the hole is closed and a Seed is in a bolt bus (it consumes 1);
- **Collapse**: enabled only when the hole is open and a Collapser is in a bolt bus. It needs two clicks; the first
  turns it red with "N modules will pause";
- **Auto**: a toggle, default on, keeps today's behaviour (a Seed or Collapser placed in a bolt bus acts on its own,
  so automation keeps working).

**Panel B, "Modules"**: one row per `KerrSlot`. Each row has the module icon, its name, its state and its
coordinates. States: `RUNNING` / `IDLE` / `PAUSED: hole closed` / `NOT FORMED` / `NOT BUILT` / `NOT LOADED`. A
linked module also shows "parallel 352 · speed ×4.00". Read-only: modules are run from their own GUIs.

### 4.2 Module: `MTEKerrModuleGui extends MTEMultiBlockBaseGui<MTEKerrModuleBase>`

This is the standard GT multiblock GUI. Void excess, input separation, batch mode and the power panel all work as on
any GT multi; there are **no machine modes**. It adds, after `Godforge MTEBaseModuleGui`:

- **In the left gap row:** a `Linked` (green) / `Not linked` (red) status label (`createConnectionStatus`).
- **In the terminal:** `Kerr link: KEE at x, y, z · hole OPEN 16.0 SM`, then `Hole bonus: parallel 352 · speed
  ×4.00`. When unlinked, the reason, from the §2.3 table.
- **Shutdown and recipe-result texts** (lang keys `beyondthestars.kerr.module.*`): "No Kerr Ergosphere Engine at
  the slot", "Hole closed", "Needs Kerr T2 and 128 SM" (DENSITY).

---

## 5. Render

Reuse GT's own black hole: its shader, its model and its tile. The KEE places a render block at the hole centre, as
`MTEBlackHoleCompressor.createRenderBlock` does.

**What GT has:**

- `GregTechAPI.sBlackholeRender` is a `BlockRenderer<RenderingTileEntityBlackhole>`: no collision, unbreakable, no
  drops.
- `RenderingTileEntityBlackhole` gives `startScaleChange(grow)` (a 40-tick smootherstep), `setStability(0..1)` (an
  input of 0.5 or more looks fully stable) and the lasers. It syncs through a description packet.
- `BlackholeRenderer` draws `textures/model/blackhole.obj` (radius 4.15 model units; event horizon at 2.6) scaled by
  a fixed `modelScale = 0.5`. So the BHC's hole is about 2.6 blocks across, with a disc about 4 blocks across. The
  laser mesh is fixed at ±8.5 blocks along x and z.
- **Scaling hook.** Both shader profiles multiply by the *current* GL modelview: the legacy `blackhole.vert.glsl`
  uses `gl_ModelViewProjectionMatrix * u_ModelMatrix`, and the core profile reads `GL_MODELVIEW_MATRIX` in
  `FrameMatrices.uploadMVP`. A `glScalef` around the centre therefore scales GT's draw with no copied shader code.

**What we add** (four small classes and one config flag):

1. **`TileKerrHole extends RenderingTileEntityBlackhole`.**
   - It adds a synced `float scale` (written in `writeToNBT`, so the description packet carries it).
   - It overrides `getRenderBoundingBox` (± `scale × 2.1 + 1`) and `getMaxRenderDistanceSquared` (256², because the
     cage is 76 wide).
2. **The render block.** `new BlockRenderer<>("beyondthestars.kerrhole", TileKerrHole::new)` (GT's class, public),
   created in our preInit, plus `GameRegistry.registerTileEntity(TileKerrHole.class, "bts.kerrhole")`.
3. **`KerrHoleRenderer extends TileEntitySpecialRenderer`** (client only). It holds a delegate `new
   BlackholeRenderer()` and forwards `func_147497_a` (the dispatcher) to it. In `renderTileEntityAt`:

   ```
   if (!Config.renderKerrHole) return;
   push; translate(x+.5, y+.5, z+.5); scale(s, s, s); translate(-(x+.5), -(y+.5), -(z+.5));
   delegate.renderTileEntityAt(tile, x, y, z, pt);
   pop;
   ```

   GT's own static `initialized` flag and its resource-reload hook keep working. If GT's shader failed (an
   incompatible shader pack), nothing is drawn, as with the BHC.
4. **`ClientProxy.init`**: `ClientRegistry.bindTileEntitySpecialRenderer(TileKerrHole.class, new
   KerrHoleRenderer())`. The `ClientProxy` is empty today.

**Placement and lifecycle** (server, KEE):

- **Where.** At `machines.kerr.centre.rel`, (0, 0, 38) from the controller, through `getExtendedFacing()`. That
  cell is a don't-care (space) in `kerr_t1`/`kerr_t2`: the horizon sphere is render-only and is not exported.
- **Never overwrite.** It is placed only if the cell is air or already our render block. Otherwise the GUI says
  "render blocked at x, y, z".
- **When it exists.** While `holeOpen && renderOn`, whether the KEE is formed or not (a frozen hole is still a hole).
  - On open: `startScaleChange(true)`.
  - On close: `startScaleChange(false)`, and the block is removed 40 ticks later (the BHC's `collapseTimer`).
  - It is removed on `onBlockDestroyed` / `onRemoval`.
  - On load the KEE checks it once and removes a stale block.
- **Size by mass.** s(M) = clamp(4 · (M / 16)^(1/3), 2, 6.5):
  - at 16 SM, s = 4: horizon radius 5.2, disc out to about 8.3. That matches the mockup's `horizon` sphere (r 5.2)
    and its photon disc (r 6.4-8.6);
  - at 2 SM, s = 2: horizon 2.6;
  - at 64 SM, s = 6.35: horizon 8.3, disc about 13;
  - from 64 SM up to 512 SM it is capped at 6.5, so the disc stays inside the r 18 mirror ring.

  Pushed to the tile only when it changes by more than 2 %, at most once a second.
- **Stability.** `setStability(coverage / 2)`: full coverage looks calm, and below 50 % coverage the shader's
  wobble shows.
- **Lasers: off.** GT's beams run along the x and z axes and would cut through the governor and dump arcs. Our
  injector rails sit at 45°. Drawing our own rail beams is later polish.

**Toggles:**

- `Config.renderKerrHole` (default true), read on both sides: the server places the block, the client draws it.
- A per-machine `render` toggle in the KEE GUI, saved in NBT. It replaces the BHC's screwdriver toggle.

---

## 6. Structures, hologram and NEI

**Exporter changes** (`tools/export-structures.js`; `voxel.js` is still never edited):

1. **Module pieces in their own frame.** `kerr_compressor` and `kerr_extractor` are re-exported rotated about y so
   that the module controller is front centre, facing outward (layer 0 = outward). The controller cell gets `~` and
   its own `off`.
   - **Compressor**: the controller is the existing `q` cell, the mockup's `control` on the outer face at 270°. It
     faces voxel −z, so `turns` = 2.
   - **Extractor**: the existing `k` cell is on the arc's **inner** face (r 32.8), facing the hole, where a player
     cannot comfortably reach. The controller moves to the outermost arc cell on the 340° ray at y 0 (about r 37),
     facing voxel +x (`turns` = 1 or 3: the exporter computes and asserts it). The old `k` cell becomes an `x`
     socket. Two outer-face cells at y ± 2 beside the controller become `x` sockets too, mirroring the
     compressor's stacked control / energy / maintenance layout. That gives 5 sockets, room for 1 in + 1 out +
     1 energy + 2 spare.
   - The exporter asserts that each module controller's front face is open air on a full build.
2. **Socket chars.** Module sockets accept GT's standard `InputBus`, `InputHatch` (the compressor only),
   `OutputBus`, `OutputHatch` (the compressor only) and `Energy` / `ExoticEnergy`.
   - Minimums: 1 input, 1 output, 1 energy.
   - No maintenance: `getDefaultHasMaintenanceChecks` is false, as on RC-2's BHC.
   - These are now the module's own lists, so trap 3 no longer needs private lists for modules.
3. **`kerr_slots` piece**: only the module controller cells (char `1` = compressor, `2` = extractor), in the KEE
   frame.
4. **Manifest.** `machines.kerr.slots` and `machines.kerr.centre`, as in §2.2. `--check` covers them.

**Hologram:**

| Controller | Stack 1 | Stack 2+ | Notes |
|---|---|---|---|
| KEE | `kerr_t1` | `kerr_t2` | It also hints `kerr_slots` with StructureLib `ofHint(7)` / `ofHint(8)` (check always true, no block placed). So the projector shows where each module controller goes, and no module MTE enters the KEE's build or preview (trap 4). |
| Module | its arc | as 1 | Built from its own controller. The arc never touches a KEE cell: the overlap policy already gives the 40 + 9 shared cells to the module. |

Survival auto-place works as today: `noOverwrite`, the budget rule, hatches only with `gt_hatch`.

**NEI preview** (BlockRenderer6343 discovers `IConstructable` MTEs on its own):

- **KEE**: 2 tiers, as today. The `ofHint` slots draw nothing.
- **Each module**: 1 tier, a lone arc. Its tooltip says "Stands in the KEE's south arc (east arc); place this
  controller where the KEE's hologram shows the marker".

---

## 7. Migration from the single-controller Kerr

Existing worlds have module **core blocks** (`slCasing2:0` compressor, `slCasing2:1` extractor) in the arcs, and the
KEE holds their hatches.

1. **Automatic, one time, on the KEE** (`KerrMigration.convert(kee)`). It runs on the KEE's first structure check
   after load when a core block stands in a slot's legacy cell. It is idempotent and server side, and it uses the
   `MTEReactorCoreMk1.placeGtMachine` helper (:1720):
   - **Compressor:** the `q` cell is the new controller cell. Replace the core block with an `MTEKerrCompressorModule`
     facing outward.
   - **Extractor:** replace the `k` core block with Ergosphere Extraction Casing (it is a socket now), and place an
     `MTEKerrExtractorModule` at the new outer cell if that cell is air or plain arc casing.
   - The hatches already in the arc sockets stay where they are and become the module's own hatches on its first
     walk. The extractor's two new outer sockets fall back to casing, so nothing else changes.
   - Each conversion is logged, and the player who opens the KEE GUI sees "converted N modules".
2. **A fallback command** for odd cases (a blocked cell, a chunk unloaded at the time): `/btskerr migrate [x y z]`
   runs the same conversion on the Kerr the player looks at.
3. **A recipe running in the old KEE** at update time. On load, the KEE hands the old in-flight outputs
   (`mOutputItems` / `mOutputFluids`) to the converted module's output buses, and drops what does not fit as items at
   the module controller. The release note also says: switch the Kerr off before updating.
4. **Items.**
   - SL-K02 / SL-K03 now **output the module controllers** (same inputs, so the cost table is unchanged).
   - A shapeless 1:1 recipe turns a legacy core block into its module controller, so core blocks in chests are not
     wasted.
   - The `slCasing2:0/1` metas stay registered (never remove a meta from a live block) and are hidden from NEI.
5. **NBT.**
   - `btsKerrBoundMode` and the machine mode are read once and discarded.
   - `btsKerrDensity`, `btsKerrHoleOpen`, `btsKerrMass` and `btsKerrSetpoint` are kept. A setpoint read from the old
     ladder is already a whole number.
   - New: `btsKerrAutoSeeds`, `btsKerrRender`. `writeKerrState` stays the one writer (trap 2).

---

## 8. Work plan for the next units

Each step is one unit, in this order. They share the worktree, so only one runs gradle at a time.

| # | Agent | Files | Done when |
|---|---|---|---|
| R1 | **Exporter** | `tools/export-structures.js`, `structures/*` (re-export), the spec's Structure export section | Module pieces in their own frame with `~` (§6.1); the extractor's controller moved and its sockets added; `kerr_slots`; `slots` / `centre` in the manifest; `--check` clean; the open-air and front-face asserts pass. |
| R2 | **KEE** | `MTEKerrEngine` (strip it to the hole; containment power; `KerrHole`; `slotStatus`; the render placement calls), `KerrSlot`, `KerrHatchElement` (2 entries), `KerrMigration`, `MTEKerrEngineGui`, lang | Compiles. `/btskerr` builds a T1 with no modules. Open / close / setpoint work from the GUI. The scanner lines are updated. |
| R3 | **Modules** | `MTEKerrModuleBase`, `MTEKerrCompressorModule` (28117), `MTEKerrExtractorModule` (28118), `MTEKerrModuleGui`; `BTSMachines` lines (base+17, base+18); `KerrKit` / `CommandBTSKit` / `CommandBTSKerr` (builds place the module controllers); the SL-K02/K03 outputs plus the legacy conversion recipe; controller textures (front, active, glow ×2 each, through `tools/gen-sl-textures.js`); lang | Each module forms on its own arc, links, pauses on close, and resumes. NEI lists both on GT's Compressor / Neutronium Compressor / Extractor pages and on the DENSITY page. |
| R4 | **Render** | `TileKerrHole`, `KerrHoleRenderer`, the block and TE registration, `ClientProxy`, `Config.renderKerrHole` | The hole shows at the centre in a client run. It grows and shrinks with mass, collapses on close, honours both toggles, and never replaces a non-air block. |
| R5 | **Test** | `CommandBTSBuild` / the console harness, the questions file | Headless dev server, in all four facings: link; KEE chunk unloaded (paused); KEE broken / unformed (paused, resumes); hole closed mid-recipe (paused, resumes); two KEEs side by side; the migration of a legacy-built Kerr (build with the old jar's shape, then load); the hologram (KEE slot hints, module arcs); the NEI preview (KEE 2 tiers, each module 1). |

R2 and R4 can be written in parallel: the interface between them is the `TileKerrHole` API (`setScale`,
`setStability`, `startScaleChange`). R3 needs R1's manifest and R2's `KerrHole` interface.

Regression traps to re-check:

1. The KEE's `construct` and its `ofHint` slots read only the stack.
2. `writeKerrState` stays the single writer for both tile and item NBT.
3. The KEE has no recipe lists, and the modules use only their own standard lists.
4. Every module socket is `buildHatchAdder(...).buildAndChain(casing)`, hatch first.

**"Adding a Kerr module" after the redesign:**

1. a new arc anchor and slot in the exporter;
2. a `KerrSlot` entry;
3. a subclass of `MTEKerrModuleBase` that names its recipe maps, with an MTE id from 28119 up;
4. a controller recipe, textures and lang.

The KEE does not change.

---

## As built (2026-10-12)

| File | What it is |
|---|---|
| `tools/export-structures.js` | Module pieces in their own frame (`~` = the module controller), `kerr_slots`, and `machines.kerr.slots` (`rel`, `keeRel`, `facingAbc`, `turns`, `legacyCoreRel`, `controllerVoxel`) + `machines.kerr.centre` in the manifest. Asserts each module controller's front is open air at T1 and T2. |
| `machines/kerr/MTEKerrEngine.java` | The KEE: the hole only (B-35 containment power, whole-number setpoint, Auto seeds, render switch). Implements `KerrHole`. Derives the module list every second (`slotStatus`). |
| `machines/kerr/MTEKerrEngineGui.java` | The KEE console, laid out as the mockup: terminal (hole; a lavender mass bar labelled "16.0 / 64 SM" with a yellow setpoint tick; setpoint and bleed; governor; bolt stock; stability; containment; modules), setpoint field bounded by the synced tier range with − / +, Hole control panel (slider over the current range with its scale, mirrored field, tier line, Seed / Collapser counts, Open, Collapse that arms for 4 s on the first click, Auto check box), Modules panel (controller icon, state, bonus or reason, coordinates), render toggle. Only one side panel is open at a time. B-41. |
| `machines/kerr/KerrHole.java`, `KerrSlot.java` | The module's view of the KEE; the slot table (geometry from the manifest, world math, which MTE class fits). |
| `machines/kerr/MTEKerrModuleBase.java`, `MTEKerrModuleGui.java` | A module machine: link (pull, every 20 ticks and before each recipe), pause by skipping `runMachine`, the hole's bonus fixed at recipe start, own structure / hologram / survival build, GUI with the link label and terminal lines for the link, the hole's bonus, the live recipe gate (`gateLine()`: the compressor's Densification status) and what the module runs (`mapsLine()`). |
| `machines/kerr/MTEKerrCompressorModule.java` (28117), `MTEKerrExtractorModule.java` (28118) | The two modules: their arcs, sockets (GT-standard hatches + own energy, no maintenance), maps, tooltips, overlays. |
| `machines/kerr/KerrMigration.java` | Legacy core blocks → module controllers; the old in-flight recipe handed to its module. |
| `machines/kerr/KerrHatchElement.java` | `BoltBus` and `Utility` only. `KerrModule.java` is deleted. |
| `machines/kerr/KerrKit.java`, `CommandBTSKerr.java` | `/btskit kerr`; `/btskerr` builds the KEE and both modules (creative or survival path) and diagnoses every walk and link; `/btskerr migrate [x y z]`; test bench `/btskerr legacy x y z`. |
| `BTSRecipesKerr.java` | SL-K02 / SL-K03 output the module controllers; SL-K07 / SL-K08 convert legacy cores. |
| `tools/gen-sl-textures.js` | `OVERLAY_FRONT_KERR_COMPRESSOR*` and `OVERLAY_FRONT_KERR_EXTRACTOR*` (idle, glow, active, active glow). |
