# Beyond the Stars — questbook overlay

A BetterQuesting quest line ("Beyond the Stars", 18 quests) in GTNH 2.9.0's split
DefaultQuests format, covering the whole Unit One chain: foundation platform, steam
generator, rod-pillar decay cascade, the 3x3 turbine spine and spin doctrine, the 7x7
injection spiral (sensor timing, +/-2t windows), propellant + coolant ladders, the
Large Turbine Rotor Assembly, and the SCRAM/decay/meltdown safety sheet — plus a
BLUEPRINTS row (y=120): optional per-structure bill-of-materials quests (countAsQuest
0, checkbox tasks) for Platform / Steam Generator / Spine / Spiral / Turbine
Generator / Cooling Tower, and the required "The Controllers" retrieval quest
(1x main station + 2x tower controllers, nothing consumed).

The generator also emits ONE entry quest ("Unit One", ID 190/48020) into the pack's
own "How to Generate Power" chapter (`QuestLines/HowtoGeneratePow-…/` +
`Quests/HowtoGeneratePow-…/`): a retrieval of the Nuclear Plant Foundation
controller, prerequisite "Standard Nuclear Fuel: Uranium" (0/191), pointing players
at our chapter. These are ADDED files only — no pack file is ever modified.

Quest IDs use `questIDHigh = 190` — the pack's own quests all use high 0, so the
overlay can NEVER collide with GTNH quest IDs and survives pack updates by rebase:
when GTNH ships a new DefaultQuests, just re-apply the two folders and the order line.

## ⚠ Merge rule: NEVER delete a pack line's folder

Our own line (`BeyondTheStars-*`) may be clean-replaced — delete the instance folder
first, then copy, so renamed quests leave no stale files behind.

The pack line (`HowtoGeneratePow-*`) must be **file-merged only**: copy our
`UnitOne-*.json` into the existing folders and touch nothing else. Deleting that folder
destroys the pack's own power chapter in `DefaultQuests` (worlds keep their own copy in
`saves/<world>/betterquesting/QuestDatabase.json`, so live saves survive, but every new
world and every `/bq_admin default load` would lose the chapter). If it ever happens,
restore the folder from another GTNH 2.9 instance's `DefaultQuests` and re-add our file.

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
5. Append the line from `DefaultQuests/OrderLine.txt` to
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLinesOrder.txt`
   (skip if already present).
6. New worlds pick it up automatically. For an EXISTING world, run
   `/bq_admin default load` (op) in that world to reload the default database.

## Editing

Edit the quest definitions in `gen-questbook.ps1` (names, descriptions, icons,
prerequisites, positions) and re-run it:

    powershell -NoProfile -ExecutionPolicy Bypass -File gen-questbook.ps1

It regenerates `DefaultQuests/` from scratch (BOM-less, filename-safe base64 IDs).
Do not hand-edit the generated JSON — it gets overwritten.
