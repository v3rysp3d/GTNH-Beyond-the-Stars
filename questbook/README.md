# Beyond the Stars — questbook overlay

## ⚠ READ THIS FIRST: an EXISTING world will not show the quests

BetterQuesting reads `config/betterquesting/DefaultQuests/` **exactly once — when a world is
created**. From that moment the world owns a private copy at
`saves/<world>/betterquesting/QuestDatabase.json` and never looks at the defaults again.

So merging this overlay into an instance makes the chapter appear in **new worlds only**.
In a world you are already playing, nothing shows up no matter how correct the files are.
This is the single most common "the quest isn't in my book" report, and it is not a bug in
the overlay.

**Fix it in game, in that world, with one op command:**

```
/btsquests            # says whether our ids are in THIS world's database, and why not
/btsquests install    # puts them there
```

`/btsquests install` adds only our chapter (`190/47990`), our 25 quests
(`190/48000`–`190/48019` and `190/48021`–`190/48025`) and the `Unit One` entry quest
(`190/48020`) to the world's live
database, snapshotting and restoring player progress around the write, then broadcasts a
resync so open books redraw. No pack quest is read, written or reordered — the only change
to a pack chapter is the extra `Unit One` icon in *How to Generate Power*. Re-running it is
safe and idempotent. It needs the overlay merged into the instance first (steps below):
it installs *from* `DefaultQuests`, it does not carry the quests inside the mod jar.

**The vanilla alternative** is BetterQuesting's own `/bq_admin default load`, which reloads
the WHOLE `DefaultQuests` folder over the world's database. On BetterQuesting **3.8.x-GTNH**
that command snapshots quest progress and writes it back afterwards
(`QuestDatabase.writeProgressToNBT` → reload → `readProgressFromNBT`), so completed quests
survive — but any quest edited in-world reverts to the pack default, and progress on quests
the defaults no longer contain is dropped. On older BetterQuesting builds it is a straight
overwrite and **progress does not survive**. Back the world up first.

**On the API question**, precisely: BetterQuesting's public API *does* hand out the live
databases — `QuestingAPI.getAPI(ApiReference.QUEST_DB)` and `…LINE_DB` return the same
`QuestDatabase` / `QuestLineDatabase` objects the world is running on — but it exposes **no
operation** for "load this quest line out of DefaultQuests into the running world". The only
thing shipped that comes close is `/bq_admin default load`, which is all-or-nothing.
`/btsquests install` is that missing operation, composed from the databases' own public
methods (`createNew`, `put`, `setOrderIndex`, `writeProgressToNBT`/`readProgressFromNBT`,
`NetBulkSync.sendReset`, `SaveLoadHandler.markDirty`) and reached by reflection, so the mod
keeps no compile-time or runtime dependency on BetterQuesting — see
`src/main/java/com/nateyburr/beyondthestars/BQBridge.java`. Without BetterQuesting installed,
`/btsquests` says so and stops.

## What the overlay is

A BetterQuesting quest line ("Beyond the Stars", **25 quests**) in GTNH 2.9.0's split
DefaultQuests format, laid out as five rows:

**Rows 1–2 — THE PATH (10 small guided steps, `y=0` left→right, `y=52` right→left, so the
chapter reads as one continuous line).** Each step says exactly what to do and what "done"
looks like, gates on a real retrieval task, and pays a reward that is the next step's tool:

| # | quest | gate (retrieval, never consumed) | reward |
| --- | --- | --- | --- |
| 1 | Step 1 - The Controller | Nuclear Plant Foundation x1 | Hologram Projector, 16x Radiation Proof Casing |
| 2 | Step 2 - Break Ground | Hologram Projector x1 | 64x Light Concrete |
| 3 | Step 3 - One Projection | 64x Light Concrete, 16x Radiation Proof Casing | 32x Radiation Proof Casing, **3x Maintenance Hatch** (silo, chamber, manifold) |
| 4 | Step 4 - Plumb the Points of Use | the silo face + chamber wall bill: Input Bus, Output Bus, 3x Input Hatch, Output Hatch, **2x Maintenance**, Silo Control Block | 16x Reactor Fuel Column, **4x Precision Injector** |
| 5 | Step 5 - Load the Rods | 8x Uranium Fuel Rod | 8x Uranium Fuel Rod |
| 6 | Step 6 - First Steam | 1x Water Bucket | **12x Turbine Housing** (4 spine + 8 gallery) |
| 7 | Step 7 - Seat the Rotors | 12x Turbine Housing, 8x Turbine Rotor | 1x Turbine Rotor, **4x Precision Injector**, **4x Water Bucket** (the chamber's first coolant) |
| 8 | Step 8 - Wire the Clock | Turbine Control Block, Spine Sensor, **8x Precision Injector** (= 4 + 4 above), **4x Water Bucket** | **4x Energy Hatch (IV)** |
| 9 | Step 9 - First EU | 4x Dynamo Hatch (IV), 4x Energy Hatch (IV) | 1x Turbine Rotor, 32x Tungstensteel Turbine Casing |
| 10 | Step 10 - Read the Panel | 1x Thorium-Bonded Fuel Rod | 4x Thorium-Bonded Fuel Rod |

Steps 3, 4, 6, 7 and 8 hand over the parts the *next* gate asks for, because **the projector
places none of them**: it skips every Turbine Housing cell outright (`PlaceResult.SKIP`, so
the cell stays air) and fills every hatch / injector / sensor / control-block socket with
plain casing in survival. Step 3 says so; steps 4, 7, 8 and 9 do the placing.

### ⚠ Why every gate says HOLD → CLAIM → INSTALL

`bq_standard:retrieval` with `consume = 0` is **not** a "you once had it" flag while the task
is still incomplete. In BetterQuesting **3.8.70-GTNH**, `TaskRetrieval$Detector`'s constructor
takes the `!consume` path and **zeroes every required-item entry whose progress is below its
required count**, then re-counts from the live inventory — `IntStream.range(0,
InventoryPlayer.getSizeInventory())` → `getStackInSlot`, so **worn armour counts too**. An
entry only latches once it has been seen at its FULL count at one instant, and
`TaskRetrieval.detect` returns immediately for a task that is already complete.

So: a **finished** step never comes undone, but **partial progress never accumulates**. Four
Precision Injectors now and four later reads `0/8` for ever, and a step whose body says "set
these into the wall" reads `0` while the player does exactly what it asked. Three rules
follow, and every quest in the chapter obeys them:

1. **the rewards must add up to the count the next gate asks for** — hence 4 + 4 = 8
   injectors at steps 4 and 7, 3 Maintenance Hatches at step 3 for the 2 step 4 gates on
   plus the manifold's, and the 4 Water Buckets step 7 pays for step 8's coolant gate;
2. **a step that installs its parts says HOLD → CLAIM → INSTALL in its `DONE WHEN`**, and the
   world-state check that used to live there moves into its own `CHECK YOUR WORK` block;
3. **a reward stack never exceeds the item's own max stack size.** `turbineRotor` and
   `minecraft:water_bucket` are both `maxStackSize = 1`, so each is handed over as its own
   `bq_standard:item` entry of count 1 — step 7 pays four separate bucket entries, not one
   stack of four. A *gate* may still ask for 4: `bq_standard:retrieval` sums the whole
   inventory, so four buckets in four slots read `4/4`.

A gate that could only be satisfied by wrenching finished machinery back out is a **checkbox**
instead: that is **Spin Doctrine** (its 4 spine housings have been in the wall since step 7)
and **The Propellant Ladder** (from step 8 on, its 8 injectors are in the chamber wall). The
Coolant Ladder (16 empty cells) and Written in Fire (a hazmat suit, worn) are held items and
stay retrievals. Water buckets are the one gate item that cannot be wrenched back out of the
machine it went into, so step 8 says *claim before you pour* rather than relying on
`HOLD → CLAIM → INSTALL` alone.

**Row 3 — MASTERY (`y=112`):** Break the Clamp (laser output), Spin Doctrine *(checkbox)*,
Written in Fire (SCRAM / meltdown / decay heat / seizure) and **The 100M Wall** *(checkbox,
`x=240`)* sit behind step 10. The 100M Wall is the Switchyard's place on the main path: it
names the `100,000,000 EU/t` grid soft cap, shows the panel text that announces it
(`[GRID-CAPPED from N - no Switchyard]`), carries the yard's derived bill, and says plainly
that fitting more Dynamo or Laser hatches will not move it because the cap is applied AFTER
the hatch ceiling. It is deliberately on the mastery row and not in the blueprints: a player
past 100M EU/t is losing most of their output and must not have to go looking for that. **The Propellant
Ladder *(checkbox)* and The Coolant Ladder sit behind step 7** — one step before the clock,
because step 8 cannot spin the fan until both chamber hatches are fed, and a ladder the player
cannot open while they need it is not depth, it is a dead end (see *Coolant is on the path*).

**Row 4 — PLANT BLUEPRINTS (`y=164`, `countAsQuest 0`, checkbox tasks, no prerequisites):**
bills of materials for The Platform, Steam Generator, Spine + Spiral, Turbine Generator,
Cooling Towers.

**Row 5 — MODULE BLUEPRINTS (`y=216`, same shape):** Vent Stack, Switchyard, Extraction
Derrick, Enrichment Annex — the four D-17 plant modules, one page each, with footprint,
derived bill, effect and failure mode. They were a single cramped section inside *Blueprint:
Cooling Towers* until the id budget was widened past the entry quest; that page now just
points at this row.

**Every count in both blueprint rows is derived by `gen-bom.ps1` from the structure shape
code at generation time — none of those numbers is typed by hand.** The module pages go
through `BomReq` rather than `Bom`, which throws on an item name the bill has never heard of
instead of quietly rendering `0x`.

The generator also emits ONE entry quest ("Unit One", ID `190/48020`) into the pack's own
"How to Generate Power" chapter (`QuestLines/HowtoGeneratePow-…/` + `Quests/HowtoGeneratePow-…/`):
a retrieval of the Nuclear Plant Foundation controller, prerequisite "Standard Nuclear Fuel:
Uranium" (`0/191`), pointing players at our chapter. These are ADDED files only — no pack file
is ever modified.

### Coolant is on the path, not behind it

The TC-7 interlock refuses a cycle **outright** unless the injectors have propellant *and* the
Spiral chamber's coolant hatch can cover the RPM that cycle would reach — the
`dry || coolantRungAvailable(projectedLoad) < 0` branch in `MTEReactorCoreMk1.onPostTick`:
no steam drunk, no propellant burned, no heat added, **no RPM**. A ten-step path that never
asked for coolant therefore ended at 0 EU/t no matter how correctly it was followed, with the
only explanation sitting in a quest gated behind the end of the path.

So: **step 8 fills both chamber hatches before it wires the clock** (COOL, then PROP, then the
redstone), **step 7 pays the four Water Buckets** that prime the coolant one — plain water is
the bottom rung of the ladder and a first cycle's few hundred RPM costs tens of litres a tick,
not thousands — and **both fluid ladders moved from step 10 to step 7** so they are open while
step 8 needs them. Step 8's `CHECK YOUR WORK` quotes the line that proves it: the coolant line
stops saying `Coolant NOT SUPPLIED` and names the fluid with a live L/t figure.

### A legacy plant needs one command, and the book says which

A plant raised by **0.19.0 or earlier** carries the retired plumbing frontage on its silo face
(4 Energy Hatches across the drum course, the buses out at the corners). It conflicts with the
point-of-use layout **cell for cell**, so the silo fails at every tier and no projection can
repair it — the projector measures the shell that stands and never re-tiers or overwrites it.
The main station now says so out loud (*"Tier N Steam Generator IS STANDING and does NOT pass
its check"*, with coordinates in the STRUCTURE ERRORS panel) and the fix is one op command,
`/btsmigrate`, run beside the controller.

That is documented in **`Blueprint: Steam Generator`**, and deliberately there: the blueprint
rows have **no prerequisites and `countAsQuest 0`**, so they are the only part of the chapter
a legacy owner with no quest progress can open. `Blueprint: Cooling Towers` carries the tower
half of the same migration.

**The four D-17 PLANT MODULES now have a blueprint each** — `Blueprint: Vent Stack`,
`Blueprint: Switchyard`, `Blueprint: Extraction Derrick`, `Blueprint: Enrichment Annex`, on
a second blueprint row — rather than being crammed into the Cooling Towers page for want of
quest ids. `Blueprint: Cooling Towers` now just points at them.

The Switchyard also gets **`The 100M Wall`** on the mastery row, prerequisite `Step 10`,
because a player past `100,000,000 EU/t` is hard-capped until that building stands and that
is not a fact to leave buried in an optional blueprint.

### Formatting and diagrams

Descriptions use the pack's own house style: section-sign colour codes, `[note]` and
`[warn]` blocks, short paragraphs, an `§e§lDO THIS§r` / `§a§lDONE WHEN§r` pair in every
step of the path.

`gen-questbook.ps1` is **ASCII-only on purpose**. Windows PowerShell 5.1 reads a BOM-less
`.ps1` as Windows-1252, so a section sign typed as UTF-8 would come back as mojibake;
descriptions therefore write `{6}` `{l}` `{r}` … and `Fmt()` substitutes the real section
sign at generation time.

**Images work.** BetterQuesting 3.8.70-GTNH renders a texture inside a quest description
with

```
[img height=N]domain:textures/path/file.png[/img]
```

`GuiQuest` compiles exactly `\[img height=([1-9]\d*)] *(.*?:.*?) *\[/img]`, splits the
description around each match, and draws the image with `addQuestDescImageSegment` at the
description canvas's width with the aspect maintained (so `height=` is an upper bound, not a
promise). A texture that fails to load degrades to a 1×1 rect — it cannot crash the book.

Three diagrams are embedded: the **plant overview** (Step 3), the **point-of-use socket
map** (Step 4 — the silo face and the Spiral chamber's front wall) and the **output manifold
back face** (Step 9 and Blueprint: Turbine Generator).

**Diagram orientation is load-bearing.** The plan view puts the **FRONT edge (`c = 0`) at the
BOTTOM**, where its "FRONT (you stand here)" caption is. That is the only orientation that
reads true: the reader stands at the caption looking *into* the page, so page-right is their
right hand — and `a` is the controller's relative LEFT (StructureLib 1.4.38's
`ExtendedFacing.a` *is* `getRelativeLeftInWorld`; facing NORTH gives `a = WEST`, `c = SOUTH`),
which is the right hand of anyone facing the silo face. Drawing `c = 0` at the top puts the
reader behind the plant, mirrors the cooling towers and the rotor gallery onto the wrong
sides, and leaves the caption on the back edge as well. The two face diagrams are elevations
seen from outside the wall, so they share that handedness: `a` increasing runs to the reader's
right, which is why the ROD bus (`a = 24`) is drawn left of the main station (`a = 25`) and the
DEP bus (`a = 26`) right of it. Verified this round against the real
`ExtendedFacing` source: `direction == NORTH` sets `a = WEST, b = DOWN, c = SOUTH`, and
`getRelativeLeftInWorld()` returns `a`. The manifold **back** face is the one elevation seen
from the opposite side, so its handedness is mirrored — which is harmless only because every
row of that diagram is left-right symmetric (`. L L . L L .`, `. . . T . . .`) and it labels
no `a` coordinates. Do not add one.

**Compass words are not used in the chapter**, for the same reason: `a` is the controller's
relative left, so "west strip" is only true for a plant whose controller happens to face
north — and by that convention `a 0..16` is the EAST side, the opposite of what the phrase
says. The quests describe the two strips the way the plan view draws them and the way a
player standing at the front sees them: **cooling towers on your LEFT, rotor gallery on your
RIGHT**. (D-21 brought `MTEReactorCoreMk1`'s structure-info tooltip into line — it said
"platform's west strip (a 0-16)" and now says "the platform strip to your LEFT as you face
the silo (a 0-16)" — and renamed the two towers' fault messages from `NW`/`SW` to
`FRONT-LEFT`/`BACK-LEFT` for the same reason. The chapter and the mod now use one
vocabulary.) They are generated by
`gen-diagrams.ps1` into `resources/btsquests/textures/gui/`
and merge into `config/betterquesting/resources/`, which BetterQuesting registers as a folder
resource pack (`betterquesting.misc.QuestResourcesFolder`, added to Minecraft's
`defaultResourcePacks`): a resource location resolves to `<root>/<domain>/<path>`, and the
domain is simply the folder name — which **must be lowercase**. Shipping them there instead
of in the mod jar keeps the whole questbook self-contained, so a diagram change needs no mod
rebuild — but a **resource reload (F3+T) or a restart** is needed after adding the folder for
the first time, because the domain list is rebuilt on reload.

## The IDs (verified against the merged instance)

| what | id (high/low) | base64 file suffix |
| --- | --- | --- |
| chapter "Beyond the Stars" | `190 / 47990` | `AAAAAAAAAL4AAAAAAAC7dg==` |
| Step 1 … Blueprint: Cooling Towers | `190 / 48000` … `190 / 48019` | `…AAC7gA==` … `…AAC7kw==` |
| entry quest "Unit One" (in *How to Generate Power*, `0/9`) | `190 / 48020` | `AAAAAAAAAL4AAAAAAAC7lA==` |
| Blueprint: Vent Stack … The 100M Wall | `190 / 48021` … `190 / 48025` | `…AAC7lQ==` … `…AAC7mQ==` |
| its prerequisite, the pack's "Standard Nuclear Fuel: Uranium" | `0 / 191` | — |

Chapter order: 48000–48009 the ten path steps · 48010 Break the Clamp · 48011 Spin Doctrine ·
48012 The Propellant Ladder · 48013 The Coolant Ladder · 48014 Written in Fire · 48015–48019
the five plant Blueprints · **48020 is the entry quest and is SKIPPED** · 48021–48024 the four
module Blueprints · 48025 The 100M Wall.

Quest IDs use `questIDHigh = 190` — the pack's own quests all use high 0, so the
overlay can NEVER collide with GTNH quest IDs and survives pack updates by rebase:
when GTNH ships a new DefaultQuests, just re-apply the two folders and the order line.
(Verified after every merge: 3,770 quest files in the instance, 3,770 unique ids.)

That high word is also what makes `/btsquests install` safe: it is the whole test for
"is this file ours". `questIDHigh`/`questIDLow` are literally the two halves of the
`java.util.UUID` BetterQuesting keys its databases with (`new UUID(190, 48020)`), and the
base64 in each file name is those 16 bytes, high word first.

**48020 is reserved and the generator steps over it.** It belongs to the entry quest in the
pack chapter and must not move: a world that already ran `/btsquests install` holds it under
that id. `gen-questbook.ps1` hands out lows in `$quests` array order, skips 48020, resumes at
48021, and throws if any quest is ever handed 48020 or if two quests share a low.

**Consequence: a new quest may only ever be APPENDED to `$quests`.** Lows are positional, so
inserting a quest in the middle renumbers every quest after it and orphans the progress each
installed world has already recorded against those ids. The 20 quests that existed before the
module blueprints still hold exactly `48000`–`48019`, which is what makes this expansion safe.

**`/btsquests status` counts a declared LIST, not a range.** `BQBridge.CHAPTER_QUEST_LOWS`
holds all 25 lows (`48000`–`48019` plus `48021`–`48025`; `48020` is the pack-line entry
quest and is checked separately as `ENTRY_QUEST_UUID`). It used to be a `FIRST_QUEST_LOW` /
`LAST_QUEST_LOW` pair reading `48000..48019`; when the five new quests were appended the pair
was not widened, so `status` looked for 20 ids, found all 20 and reported *"Everything is
loaded"* while five blueprints were missing from the book. A range could never describe this
chapter anyway – `48020` is a hole in the middle of it by design.

`gen-questbook.ps1` reads that array back out of `BQBridge.java` and **throws** if it does not
match the ids it just handed out, so appending a quest to `$quests` without adding its low to
the Java fails the questbook build rather than shipping a status command that lies. The
command also names what it could not find, so `20/25` is followed by
`missing 190/48021, 48022, ...`.

**The entry quest's file stem is pinned to `UnitOne`.** Our own chapter folder is
clean-replaced on every merge, so a renamed quest there simply loses its stale file — but the
pack's folder is file-MERGED and never emptied, so a stem that drifted would leave the old
`UnitOne-<same base64>.json` behind and the pack chapter would carry `190/48020` twice.

## ⚠ Merge rule: NEVER delete a pack line's folder

Our own line (`BeyondTheStars-*`) may be clean-replaced — delete the instance folder
first, then copy, so renamed quests leave no stale files behind.

The pack line (`HowtoGeneratePow-*`) must be **file-merged only**: copy our
`UnitOne-*.json` into the existing folders and touch nothing else. Deleting that folder
destroys the pack's own power chapter in `DefaultQuests` (worlds keep their own copy in
`saves/<world>/betterquesting/QuestDatabase.json`, so live saves survive, but every new
world and every `/bq_admin default load` would lose the chapter). If it ever happens,
restore the folder from another GTNH 2.9 instance's `DefaultQuests` and re-add our file.
For reference, a healthy 2.9.0-beta-1 `HowtoGeneratePow-…` holds **169** placement files and
**138** quest files once our one file is merged in.

## Merge into an instance

1. DELETE any existing `BeyondTheStars-*` folders under the instance's
   `DefaultQuests/QuestLines/` and `DefaultQuests/Quests/` first — renamed quests
   leave stale files behind.
2. Copy `DefaultQuests/QuestLines/BeyondTheStars-*` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLines/`
3. Copy `DefaultQuests/Quests/BeyondTheStars-*` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/Quests/`
4. Copy the files from `DefaultQuests/QuestLines/HowtoGeneratePow-*/` and
   `DefaultQuests/Quests/HowtoGeneratePow-*/` INTO the instance's matching pack
   folders (they only add `UnitOne-*.json`; nothing is overwritten).
5. Copy `resources/btsquests/` into
   `<instance>/.minecraft/config/betterquesting/resources/` — this is the quest diagrams.
6. Append the line from `DefaultQuests/OrderLine.txt` to
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLinesOrder.txt`
   (skip if already present).
7. New worlds pick it up automatically. For an EXISTING world, load it and run
   `/btsquests install` (op) — see the top of this file. `/bq_admin default load` is the
   heavier vanilla fallback.

Verify the merge landed by running `/btsquests` in any world: it prints whether
`190/47990`, all 25 chapter quests (`190/48000-48019, 48021-48025`) and the entry quest
`190/48020` are in that world's live database, where it is reading `DefaultQuests` from, and
– when anything is short – exactly which ids are missing.

## Editing

**There is ONE build command, and it is `gen-bom.ps1`.** Whatever you changed — a quest
description in `gen-questbook.ps1`, or the structure geometry in the Java — run:

    powershell -NoProfile -ExecutionPolicy Bypass -File gen-bom.ps1
    gradlew spotlessApply compileJava

That single run re-derives the bills from the shape code, re-renders `BTSBom.java` (the
list `/btskit` prints), then runs `gen-questbook.ps1` itself to rewrite `DefaultQuests/`
from the same tally, and finally prints the table. **Do not run `gen-questbook.ps1` as the
build step** — it cannot write `BTSBom.java`, so after a geometry change it throws instead
of finishing. See *"Both consumers or neither"* below for why it is welded together this
way; it used to be two commands and the second one got forgotten.

`gen-questbook.ps1` regenerates `DefaultQuests/` from scratch (BOM-less, filename-safe
base64 IDs) and asserts on the way out that every icon / task / reward stack has a real id
and a count of at least one — `Icon` takes `(id, damage, count)`, and a dropped final
argument otherwise ships a retrieval nobody can complete.

Redraw the diagrams with:

    powershell -NoProfile -ExecutionPolicy Bypass -File gen-diagrams.ps1

Every one of these scripts is safe to re-run; do not hand-edit the generated JSON, PNGs or
`BTSBom.java`, they get overwritten. `gen-diagrams.ps1` also DELETES the retired `bay-sockets.png` if it finds one,
so a stale texture can never contradict the book.

### Bills of materials are derived, not typed

`gen-bom.ps1` re-implements every structure shape function of `MTEReactorCoreMk1` — the four
plant-module shapes (`ventShape`, `switchyardShape`, `derrickShape`, `annexShape`) included,
each with its own `Assert-Source` literal — plus the borrowed
`MTEModuleCoolingTower.towerShape`, tallies each cell through the element table
transcribed from `getStructureDefinition()`, and cross-checks itself (fuel-column cells vs
`COLUMN_CAP` x 8 segments, rod-port cells vs the pillar cap, the shield pad vs the octagon
formula). `gen-questbook.ps1` dot-sources it and interpolates the results into the five
Blueprint quests, so a bill cannot go stale by hand.

It also GUARDS the port: before computing anything it reads `MTEReactorCoreMk1.java` and
`MTEModuleCoolingTower.java` and throws, naming the offender, if any constant or
socket-placing line it was ported from has moved. **A geometry change in the Java therefore
breaks the questbook build instead of silently shipping a wrong shopping list.**

Running it prints the whole table — and, as above, rewrites both consumers on the way past.
There is no read-only switch on purpose: a mode that re-renders `BTSBom.java` without the
questbook is exactly the hole that shipped last time.

### /btskit reads the same tally the quests do

`/btskit` prints a shopping list too. It used to keep its own hand-typed estimates, which had
drifted by up to 3.7x and named `Iridium Tungstensteel Block` - a GT `ItemList` enum constant,
not a registered display name, so NEI found nothing for it (review finding C5/A4). The command
now owns no numbers and no material names at all: it reads
`src/main/java/com/nateyburr/beyondthestars/BTSBom.java`, which `gen-bom.ps1` renders from the
same in-memory `$BTS_BOM` it interpolates into the Blueprint quests. Four things keep the two
consumers from ever disagreeing again:

1. **One derivation.** `$BTS_BOM` is computed once per run, from the shape code, and both the
   quest descriptions and `BTSBom.java` are written out of it.
2. **A stale-file gate.** Every run of `gen-bom.ps1` - dot-sourced runs included - re-derives
   `BTSBom.java` and throws if the copy on disk disagrees. Because `gen-questbook.ps1`
   dot-sources `gen-bom.ps1`, **the questbook cannot be regenerated while `BTSBom.java` is
   stale.** The comparison ignores whitespace and comments, so `spotlessApply` is free to
   reformat the generated file.
3. **Both consumers or neither.** That gate only ever pointed one way. Run as a script,
   `gen-bom.ps1` used to rewrite `BTSBom.java` and stop — so the *other* direction,
   `BTSBom.java` newer than the shipped quest JSON, was unguarded, and it is the direction a
   geometry edit actually takes. Nothing re-derives `questbook/DefaultQuests/**.json` after
   the fact: no gate, no test, and neither `AssertStacks` nor `AssertDesc` looks at a count.
   `/btskit` would print the new numbers, the book the old ones, exit code 0. So the write no
   longer stands alone: **the same script run that re-renders `BTSBom.java` runs
   `gen-questbook.ps1` and rewrites `DefaultQuests/` from the same shape code**, and there is
   no switch that does one without the other. If that regeneration throws, `BTSBom.java` is
   **rolled back** and the error is re-thrown. That is not a claim the two agree at every
   instant — `gen-questbook.ps1` clears `DefaultQuests/` and writes it file by file, so a
   failure part way through can leave it empty, partial, or fully rewritten with the new
   numbers. It is a claim about where the tree comes to **rest**: nothing here can detect a
   stale `DefaultQuests/`, but *every* gate detects a stale `BTSBom.java`, so a failed run is
   parked in the state that gets caught — it exits non-zero, the next dot-sourced run throws,
   and the next script run rewrites both. The tree cannot come to rest split *and silent*,
   which is what T1 was. Re-run the script, or `git checkout questbook`, to restore
   `DefaultQuests/`. (No recursion: `gen-questbook.ps1` dot-sources `gen-bom.ps1`, and a
   dot-sourced run only verifies and stops.)
4. **Compile-time name checking.** Each material name reaches the Java exactly once, as the
   value of a generated `BTSBom` constant (`RADIANT_NAQUADAH_ALLOY_CASING`, ...), and
   `CommandBTSKit` may only name a material through those. An item that is renamed or drops out
   of the element alphabet therefore breaks `gradlew compileJava` rather than printing a block
   that does not exist.

`BTSBom.java` is generated - do not hand-edit it. After a geometry change run `gen-bom.ps1`
(which rewrites the questbook too), then `gradlew spotlessApply compileJava`.

### Every registry id in the chapter is verified, not guessed

| item | id | source of truth |
| --- | --- | --- |
| Nuclear Plant Foundation | `gregtech:gt.blockmachines:28100` | `Config.mteBaseId` |
| Precision Injector / Spine Sensor / Silo Control Block / Turbine Control Block | `…:28106` / `28107` / `28108` / `28109` | `BTSMachines.register()` |
| Dynamo (IV) / Energy (IV) / Input (IV) / Output (IV) / Input Bus (IV) / Output Bus (IV) / Maintenance | `…:35` / `45` / `55` / `65` / `75` / `85` / `90` | `gregtech/api/enums/MetaTileEntityIDs.java` |
| ZPM 4,096A Laser Source / Target, Laser Vacuum Pipe | `…:15252` / `15152` / `15465` | same enum + `tectech/loader/thing/MachineLoader.java` |
| Tungstensteel Turbine | `gregtech:gt.blockcasings4:12` | `gregtech/api/casing/Casings.java` + GT lang |
| Radiation Proof / Solid Steel | `gregtech:gt.blockcasings3:12` / `gt.blockcasings2:0` | same |
| Europium Reinforced Rad Proof / Radiant Naquadah Alloy | `gregtech:gt.blockcasings8:5` / `:10` | same |
| Iridium Reinforced Block | `gregtech:gt.blockreinforced:1` | GT lang (`gt.blockreinforced.1.name`) |
| Light Concrete / Tungstensteel Reinforced | `gregtech:gt.blockconcretes:8` / `gt.blockreinforced:3` | GT lang |
| Hologram Projector | `structurelib:item.structurelib.constructableTrigger:0` | the pack's own Multiblock Goals quests |
| Hazmat suit, empty cell | `IC2:itemArmorHazmat{Helmet,Chestplate,Leggings}`, `IC2:itemArmorRubBoots`, `IC2:itemCellEmpty` | the pack's own "Repairing Your Hazmat Suit" quest |
| our rods, columns, housings, rotors | `beyondthestars:*` | `BTSItems` / `BTSBlocks` |
