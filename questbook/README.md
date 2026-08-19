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

`/btsquests install` adds only our chapter (`190/47990`), our 18 quests
(`190/48000`–`190/48017`) and the `Unit One` entry quest (`190/48020`) to the world's live
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

## The IDs (verified against the merged instance)

| what | id (high/low) | base64 file suffix |
| --- | --- | --- |
| chapter "Beyond the Stars" | `190 / 47990` | `AAAAAAAAAL4AAAAAAAC7dg==` |
| The Blueprint … Blueprint: Cooling Tower | `190 / 48000` … `190 / 48017` | `…AAC7gA==` … `…AAC7kQ==` |
| entry quest "Unit One" (in *How to Generate Power*, `0/9`) | `190 / 48020` | `AAAAAAAAAL4AAAAAAAC7lA==` |
| its prerequisite, the pack's "Standard Nuclear Fuel: Uranium" | `0 / 191` | — |

Chapter order: 48000 The Blueprint · 48001 Raising the Silo · 48002 First Steam ·
48003 The Decay Cascade · 48004 The Spine · 48005 Spin Doctrine · 48006 The Spiral ·
48007 Octuple Injection · 48008 Cold Blood · 48009 The Assembly · 48010 Written in Fire ·
48011 The Controllers · 48012–48017 the six Blueprint quests. 48018/48019 are unused.

Quest IDs use `questIDHigh = 190` — the pack's own quests all use high 0, so the
overlay can NEVER collide with GTNH quest IDs and survives pack updates by rebase:
when GTNH ships a new DefaultQuests, just re-apply the two folders and the order line.

That high word is also what makes `/btsquests install` safe: it is the whole test for
"is this file ours". `questIDHigh`/`questIDLow` are literally the two halves of the
`java.util.UUID` BetterQuesting keys its databases with (`new UUID(190, 48020)`), and the
base64 in each file name is those 16 bytes, high word first.

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
6. New worlds pick it up automatically. For an EXISTING world, load it and run
   `/btsquests install` (op) — see the top of this file. `/bq_admin default load` is the
   heavier vanilla fallback.

Verify the merge landed by running `/btsquests` in any world: it prints whether
`190/47990`, `190/48000-48017` and `190/48020` are in that world's live database, and
where it is reading `DefaultQuests` from.

## Editing

Edit the quest definitions in `gen-questbook.ps1` (names, descriptions, icons,
prerequisites, positions) and re-run it:

    powershell -NoProfile -ExecutionPolicy Bypass -File gen-questbook.ps1

It regenerates `DefaultQuests/` from scratch (BOM-less, filename-safe base64 IDs).
Do not hand-edit the generated JSON — it gets overwritten.
