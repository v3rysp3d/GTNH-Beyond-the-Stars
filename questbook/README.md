# Beyond the Stars — questbook overlay

## ⚠ READ THIS FIRST: how the book gets into a world

BetterQuesting reads `config/betterquesting/DefaultQuests/` **only when a world is created**
(and again on its own `/bq_admin default load`, which it also runs by itself when the pack's
`pack_version` changes). From then on the world owns a private copy at
`saves/<world>/betterquesting/QuestDatabase.json`. Merging this overlay into an instance
therefore reaches new worlds only.

**So the mod installs the book itself.** The jar carries the whole overlay as one bundle,
`assets/beyondthestars/questbook/overlay.json`, and on every server start
(`FMLServerStartedEvent`, after BetterQuesting has loaded the world) it brings the world's
LIVE database in line with it whenever the bundle, the config or our content differs:
our chapters and quests, our quests grafted into pack chapters, and the **pack edits** (the
BHC quest becomes the Kerr Ergosphere Engine, the EOH quest becomes our Eye, the UXV / MAX /
endgame quests move into our chapters). An unchanged world pays one file read. Config
(`config/beyondthestars.cfg`, category `questbook`):

| Key | Default | Meaning |
| --- | --- | --- |
| `questbookAutoInstall` | true | Install or refresh on server start when needed. Off: only `/btsquests install`. |
| `questbookPackEdits` | true | Apply the pack edits. Off: the next install restores every edited pack quest from its stored original, and the fallback quests 48103 (KEE) and 48109 (Eye) stand beside the untouched pack quests. |

The edits also follow `removeBlackHoleCompressorRecipe` (E01-E06) and
`replaceTecTechEyeOfHarmony` (E10-E25, E37, E40, E47, E63, E64); with a flag off, its edits are
reverted on the next install and the matching fallback quest appears.

**Op commands** (`/btsquests`, permission 2):

```
/btsquests                     # status: our chapters n/expected (missing ids named), pack edits
                               #   applied / skipped (with why), planned checkboxes, auto-install
/btsquests install [force]     # bring this world in line with the jar (force: even if it matches)
/btsquests uninstall [all|edits]
                               # edits: restore every pack quest from its stored original (our
                               #   chapters and the fallback quests stay); all: that, then remove
                               #   every chapter and quest of ours. Sticky: the server does not
                               #   reinstall on start until an op runs /btsquests install.
/btsquests verify              # every item/fluid/ore name in the bundle, resolved live
/btsquests dumpids             # the same, written to config/beyondthestars/questbook-registry.json
```

**What every install does** (`QuestbookInstaller`, reflection only, BetterQuesting
3.8.89-GTNH members checked with javap):

1. snapshots every player's progress (`QuestDatabase.writeProgressToNBT`);
2. plans each pack edit against the pack quest's **original**: matched by id **and** name **and**
   expected chapter; a mismatch (the pack changed the quest, e.g. RC-3) **skips that edit with
   a warning** (server log, `status`, one chat line to ops on login) and leaves the quest as the
   pack wrote it, so the book is never half-edited;
3. reverts every applied edit that is no longer wanted (flag off, drifted, dropped);
4. replaces our quests and chapters wholesale from the bundle, puts UXV / MAX / The Endgame
   directly after the pack's Tier 12 - UMV and the plant chapter last, and grafts ours into
   pack chapters (edited positions only when every edit that re-lays that chapter applied);
5. applies each edit to a **copy of the stored original**, never to the edited quest, so a
   second run gives the same database and `uninstall` restores the pack quest bit for bit;
6. restores progress on every path (`try/finally`): completion is always kept; task progress
   is dropped only where a quest's task list changed (the `completionOnly` edits E01, E10,
   E63, E64 on their first application, never on a re-install); a planned checkbox that has
   become a real quest is cleared (a ticked box never stands in for the machine);
7. writes `saves/<world>/beyondthestars/questbook-state.dat` (NBT: per applied edit the pack
   quest's original definition and chapter entries plus the definition as applied, the
   placeholders, the last report, a sticky-uninstall marker), marks BetterQuesting dirty and
   resyncs every client (`NetBulkSync.sendReset`).

A `/bq_admin default load` (by hand, or BetterQuesting's own on a pack update) wipes our edits
and, if this overlay was NOT merged into `DefaultQuests`, our quests too. The next server start
re-applies everything. Each stored original remembers the world's `pack_version`: after a pack
update the pack's new quests become the originals (an edit whose quest the pack renamed or moved
is skipped with a warning), while with the same `pack_version` the stored originals stay and the
edits are re-applied over them. Progress on our own quests
survives that through `saves/<world>/beyondthestars/questbook-progress.dat`, a backup of it
refreshed on every world save. Merging the overlay (below) is still recommended: new worlds
then show our chapters even before the first install, and `/bq_admin default load` keeps them.

A jar without a bundle falls back to the old path: our quests are added from the instance's
`DefaultQuests` (no pack edits). Without BetterQuesting installed, `/btsquests` says so and
stops, and the mod has no dependency on it.

## What the overlay is

A BetterQuesting quest line ("Beyond the Stars", **28 quests**) in GTNH 2.9.0's split
DefaultQuests format, laid out as six rows:

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

**Row 6 — THE CAMPAIGN (`y=268`, `Size 32`):** *The Campaign Begins* (`48026`, prerequisite
Step 10), *The Four Lines* (`48027`) and *The Upgrade Tree* (`48028`). This is where the
ten-step path leads: the build is finished, the **campaign has not started**. Step 10's
description now ends by saying exactly that and naming the row, and the chapter blurb calls the
bottom row THE CAMPAIGN, because a player who reaches the end of the path and is told nothing
concludes the mod is over.

*The Campaign Begins* and *The Upgrade Tree* are **checkbox** tasks, for the same reason
*Spin Doctrine* is: a milestone tier is not an item and a purchased node is not an item, so
there is nothing a `bq_standard:retrieval` could ever read. *The Four Lines* is a real
retrieval of one **Naquadah-Bonded Fuel Rod** — the top of the cascade ladder, ×9 — because
that is the item the BURNUP line exists to ask for, and it is a held item, so it obeys
`HOLD → CLAIM → INSTALL` and carries `$HOLD_FIRST` like every other install-it-afterwards gate.

**Every panel string those three quests quote was checked character for character against the
formatter that prints it** — `BTSMilestone.milestoneLine` / `milestoneSummaryLine` /
`milestoneUnlockLine`, `BTSUpgrade.treeSummaryLine` / `treeEffectLine`,
`MTEReactorCoreMk1.mathTurbineLine`, the twelve tier-5-to-7 grant strings in `BTSMilestone`,
and the three `beyondthestars.gui.tree.*` lang entries. Only the ASCII part of a line is ever
quoted, per the rule below: the panel separates fields with a middle dot this ASCII-only
generator cannot write. The thresholds, the four multiplier columns and the tree's full-tree
channel totals are transcribed from `BTSMilestone` / `BTSUpgrade` / `BTSChannel`; the design of
record for all of it is `addon/docs/progression.md`.

**Two existing quests were edited by this row, and no id moved.** Step 10 gained the handover
paragraph; Step 2 gained the controls that appeared in the controller's gap row — the
18×18 **chart button** that opens the tree sits to the LEFT of the number fields (the row
is `reverseLayout(true)`, so the last child added is leftmost), 0.22.1 added a fourth control
between Build Tier and the rods — the 5-digit **RPM LIMIT** field (F4), which is where the
TURBINE line's redline unlock is opted into — and the rod field now accepts
up to **36** rather than 24, because `BURNUP` T5–7 and the tree's three rod nodes can withdraw
the rods that far. Step 2 says a value above 24 is clamped straight back until they do.

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

## The Singularity Ladder chapters (`tools/gen-singularity.js`)

The ladder's part of the book (design: `docs/questbook-integration.md`) is data, not code:

| | |
| --- | --- |
| **Source** | `questbook/singularity.json`: every chapter, quest, text and pack edit. ASCII-only: `{6}{l}{r}` are section-sign codes, `{times}` `{theta}` `{u+XXXX}` are glyphs, `{bom:eye_beam:R}` / `{bomdims:kerr_t1}` are read from `structures/manifest.json`, a text line `@name` pastes `texts.name`. |
| **Ids** | Explicit `low`s, never positional. `questbook/id-ledger.txt` is append-only: the generator appends new ids and throws if a shipped id disappears or changes meaning (its `key`). |
| **Registry** | Our items, fluids and MTEs are checked against the Java of the source tree. Anything the RC-2 pack itself uses is accepted. Everything else must be in `questbook/registry.json` with where it was read (`status: verify` = derived, to confirm with `/btsquests verify`). |
| **Generator** | `tools/gen-singularity.js` (node). It runs as the LAST step of `gen-questbook.ps1`, so `gen-bom.ps1` is still the one build command. |

**Outputs** (all generated; never hand-edit):

1. `questbook/DefaultQuests/`: our chapters `Tier13UXV-*`, `Tier14MAX-*`, `TheEndgame-*`; our quests
   ADDED to the pack's `Tier11UIV-*` and `Tier12UMV-*` folders (pinned file stems, file-merged); and
   `OrderLines.txt` (replaces `OrderLine.txt`). This is an **edits-off world**: the pack exactly as
   the pack ships it, plus our content, plus the two fallback quests (48103 KEE, 48109 Eye). Our
   UMV quests sit `offShift` lower here so they clear the pack's unedited right half.
2. `src/main/resources/assets/beyondthestars/questbook/overlay.json`: the bundle the jar carries.
   Every chapter and quest of ours (the plant chapter too, read back from the files
   `gen-questbook.ps1` just wrote) in BetterQuesting's typed-key JSON, the grafts with both
   positions (`on` = pack edits applied, `off`), and the 62 compiled pack edits (variants resolved
   against `features`; ops: `setName setDesc setIcon replaceText appendText setTasks setRewards
   swapItem setPrereqs addPrereq removePrereq replacePrereq move`, ids as base64). `bundleVersion`
   is the SHA-1 of the content. `QuestbookInstaller` (on top of `BQBridge`) applies it to a live
   world: see the top of this file.
3. `src/main/java/com/nateyburr/beyondthestars/BTSQuestIds.java`: every line and quest id we own,
   per chapter, plus fallbacks, placeholders and edit ids. Run `gradlew spotlessApply` after.
4. `questbook/id-ledger.txt` (appended), and `questbook/pack-drift.txt` only when the pack drifted.

**Source tree.** Until `kerr-redesign` is merged into this branch, its Kerr module controllers
(28117/28118) exist only in the integration tree, so run the build with

    $env:BTS_QB_SRC = '../GTNH-Addons/addon'; powershell -NoProfile -ExecutionPolicy Bypass -File gen-bom.ps1

(`BTS_QB_PACK` points at another pack's `DefaultQuests`; default: the RC-2 instance). Without it
the run fails and says so. On its own: `node tools/gen-singularity.js [--src DIR] [--pack DIR]
[--check] [--selftest] [--svg DIR] [--feature name=true] [--verbose]`.

**What fails the build** (all errors are collected and printed together, naming the offender):

- an id outside its chapter's range, a duplicate id, a ledger change, a quest without tasks or icon;
- an item, fluid or ore that is neither ours (in the Java), nor used by the pack, nor in `registry.json`;
  a stack count below 1; an unknown `{token}` or `@text`;
- a `bomItems` row whose manifest block no longer matches (`expect`), or a role with no row;
- a placeholder whose features are now built but still has a `tbd` task;
- **pack drift**: an edited quest missing, renamed (`expectName`), out of its `expectChapter`, a
  `replaceText` whose text is gone, a prerequisite or swapped item that is not there;
- **a prerequisite cycle** through our content or edits (only AND quests are followed). The spec's
  layout had one: T8: Tipler kept its pack prerequisites MHDCSM and Resplendent, which now sit
  behind Eye T10, which sits behind Eye T8, which needs T8: Tipler. E24 now gates Tipler on
  T7: Perfect, like T5 to T7;
- **tidiness**, checked on three layouts (pack edits on with today's features, the same once every
  planned feature ships, and edits off): two icons overlapping, a quest of ours or a moved pack
  quest off the 12-unit grid, and a normal prerequisite line passing within 3 units of a third
  icon (BetterQuesting draws lines 4 to 8 wide). In the pack's chapters a crossing the pack already
  draws is never ours; we may never ADD one. Hover-only (implicit, type 1) lines and the edits-off
  layout only warn (`--verbose` lists them).

`--selftest` breaks the data on purpose (overlap, crossing, drifted name, unknown item and fluid,
duplicate id, range, grid, missing prerequisite, stale replaceText, ledger change and loss, cycle,
`tbd` shipping, a manifest role change) and fails unless every assertion fires and the real data
builds clean. `--svg DIR` draws every chapter in every layout (ours blue, moved pack quests gold,
crossings red) for eyeballing before a client screenshot.

**Placeholders.** A quest whose `needs` features are not all `true` ships as one checkbox,
`countAsQuest 0`, no rewards, a grey name ending `(planned)`, and the standard planned note; its
real `tasks` stay in the data. The unit that ships a feature flips its flag; the next build
emits the real tasks under the same id (and the bridge clears a ticked box, spec §4.6).

## The IDs (verified against the merged instance)

| what | id (high/low) | base64 file suffix |
| --- | --- | --- |
| chapter "Beyond the Stars" | `190 / 47990` | `AAAAAAAAAL4AAAAAAAC7dg==` |
| Step 1 … Blueprint: Cooling Towers | `190 / 48000` … `190 / 48019` | `…AAC7gA==` … `…AAC7kw==` |
| entry quest "Unit One" (in *How to Generate Power*, `0/9`) | `190 / 48020` | `AAAAAAAAAL4AAAAAAAC7lA==` |
| Blueprint: Vent Stack … The 100M Wall | `190 / 48021` … `190 / 48025` | `…AAC7lQ==` … `…AAC7mQ==` |
| The Campaign Begins … The Upgrade Tree | `190 / 48026` … `190 / 48028` | `…AAC7mg==` … `…AAC7nA==` |
| its prerequisite, the pack's "Standard Nuclear Fuel: Uranium" | `0 / 191` | — |

Chapter order: 48000–48009 the ten path steps · 48010 Break the Clamp · 48011 Spin Doctrine ·
48012 The Propellant Ladder · 48013 The Coolant Ladder · 48014 Written in Fire · 48015–48019
the five plant Blueprints · **48020 is the entry quest and is SKIPPED** · 48021–48024 the four
module Blueprints · 48025 The 100M Wall · 48026–48028 the three CAMPAIGN quests.

Quest IDs use `questIDHigh = 190` — the pack's own quests all use high 0, so the
overlay can NEVER collide with GTNH quest IDs and survives pack updates by rebase:
when GTNH ships a new DefaultQuests, just re-apply the two folders and the order line.
(Verified after every merge: 3,770 quest files in the instance, 3,770 unique ids — that was
the 26-file overlay; the three CAMPAIGN quests take the overlay to 29 files and the instance
to 3,773. The overlay's own side is re-checked on every regeneration: 29 quest files, 29
unique ids, lows `48000`–`48028` with no gap and no repeat.)

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
module blueprints still hold exactly `48000`–`48019`, and the 25 that existed before the
CAMPAIGN row still hold exactly `48000`–`48019` + `48021`–`48025` — which is what makes each
expansion safe. Confirm it the same way every time: regenerate, then check that the only new
files are the appended ones and that no existing file's `questIDLow:4` moved.

**`/btsquests status` counts a declared LIST, not a range.** It counts
`BTSQuestIds.PLANT_QUESTS` (and the ladder lists beside it), which
`tools/gen-singularity.js` GENERATES from the files `gen-questbook.ps1` just wrote, with
`48020` (the pack-line entry quest) checked separately as `ENTRY_QUEST`. It used to be a
`FIRST_QUEST_LOW` / `LAST_QUEST_LOW` pair reading `48000..48019`; when five quests were
appended the pair was not widened, so `status` reported *"Everything is loaded"* while five
blueprints were missing. A generated list cannot fall behind the chapter, and the command
names what it could not find (`missing 190/48021, 48022, ...`).

**The entry quest's file stem is pinned to `UnitOne`.** Our own chapter folder is
clean-replaced on every merge, so a renamed quest there simply loses its stale file — but the
pack's folder is file-MERGED and never emptied, so a stem that drifted would leave the old
`UnitOne-<same base64>.json` behind and the pack chapter would carry `190/48020` twice.

## ⚠ Merge rule: NEVER delete a pack line's folder

Our own line (`BeyondTheStars-AAAAAAAAAL4AAAAAAAC7dg==`) may be clean-replaced — delete the
instance folder first, then copy, so renamed quests leave no stale files behind.

The same holds for the ladder's three chapters of ours, and ONLY these exact folder names:
`Tier13UXV-AAAAAAAAAL4AAAAAAAC7dw==`, `Tier14MAX-AAAAAAAAAL4AAAAAAAC7eA==` and
`TheEndgame-AAAAAAAAAL4AAAAAAAC7eQ==`: clean-replace. Never match them by wildcard
(`Tier13UXV-*`): a future pack that ships its own `Tier 13 - UXV` or `Tier 14 - MAX` chapter
gets a folder with the same name stem and a different suffix, and that folder is the pack's.
Every folder of ours ends in a suffix starting `AAAAAAAAAL4` (questIDHigh 190); nothing else
is ours. The pack lines we graft into (`HowtoGeneratePow-*`,
`Tier11UIV-*`, `Tier12UMV-*`) are file-merged only.

The pack line (`HowtoGeneratePow-*`) must be **file-merged only**: copy our
`UnitOne-*.json` into the existing folders and touch nothing else. Deleting that folder
destroys the pack's own power chapter in `DefaultQuests` (worlds keep their own copy in
`saves/<world>/betterquesting/QuestDatabase.json`, so live saves survive, but every new
world and every `/bq_admin default load` would lose the chapter). If it ever happens,
restore the folder from another GTNH 2.9 instance's `DefaultQuests` and re-add our file.
For reference, a healthy 2.9.0-beta-1 `HowtoGeneratePow-…` holds **169** placement files and
**138** quest files once our one file is merged in.

## Merge into an instance

1. DELETE exactly these four folders, if present, under the instance's
   `DefaultQuests/QuestLines/` and `DefaultQuests/Quests/` first (renamed quests leave stale
   files behind): `BeyondTheStars-AAAAAAAAAL4AAAAAAAC7dg==`, `Tier13UXV-AAAAAAAAAL4AAAAAAAC7dw==`,
   `Tier14MAX-AAAAAAAAAL4AAAAAAAC7eA==`, `TheEndgame-AAAAAAAAAL4AAAAAAAC7eQ==`. Match the whole
   name, never a wildcard: a pack folder such as `Tier13UXV-<pack id>` (a future pack's own UXV
   chapter) is NOT ours and must not be touched. Only suffixes starting `AAAAAAAAAL4` are ours.
2. Copy those four chapter folders from `DefaultQuests/QuestLines/` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLines/`
3. Copy the same four from `DefaultQuests/Quests/` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/Quests/`
4. Copy the FILES from `DefaultQuests/QuestLines/HowtoGeneratePow-*/`, `Tier11UIV-*/` and
   `Tier12UMV-*/` (and the same three under `Quests/`) INTO the instance's matching pack
   folders. They only add our `190/…` files; nothing is overwritten. Never delete those
   pack folders.
5. Copy `resources/btsquests/` into
   `<instance>/.minecraft/config/betterquesting/resources/` — this is the quest diagrams.
6. Edit `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLinesOrder.txt` as
   `DefaultQuests/OrderLines.txt` says: the three ladder lines go directly after the pack's
   `Tier 12 - UMV` line, the plant line at the very end (skip any already present). It
   replaced `OrderLine.txt`.
7. New worlds pick it up automatically, and the mod's own install (top of this file) handles
   existing worlds on their next server start. `/btsquests install` (op) does it on the spot.

Verify by running `/btsquests` in any world: it prints, per chapter of ours, how many of
its quests are in that world's live database (and exactly which ids are missing), the entry
quest `190/48020`, and every pack edit applied or skipped.

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
