# Beyond the Stars — questbook overlay

A BetterQuesting quest line ("Beyond the Stars", 11 quests) in GTNH 2.9.0's split
DefaultQuests format, covering the whole Unit One chain: foundation platform, steam
generator, rod-pillar decay cascade, the 3x3 turbine spine and spin doctrine, the 7x7
injection spiral (sensor timing, +/-2t windows), propellant + coolant ladders, the
Large Turbine Rotor Assembly, and the SCRAM/decay/meltdown safety sheet.

Quest IDs use `questIDHigh = 190` — the pack's own quests all use high 0, so the
overlay can NEVER collide with GTNH quest IDs and survives pack updates by rebase:
when GTNH ships a new DefaultQuests, just re-apply the two folders and the order line.

## Merge into an instance

1. Copy `DefaultQuests/QuestLines/BeyondTheStars-*` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLines/`
2. Copy `DefaultQuests/Quests/BeyondTheStars-*` into
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/Quests/`
3. Append the line from `DefaultQuests/OrderLine.txt` to
   `<instance>/.minecraft/config/betterquesting/DefaultQuests/QuestLinesOrder.txt`
4. New worlds pick it up automatically. For an EXISTING world, run
   `/bq_admin default load` (op) in that world to reload the default database.

## Editing

Edit the quest definitions in `gen-questbook.ps1` (names, descriptions, icons,
prerequisites, positions) and re-run it:

    powershell -NoProfile -ExecutionPolicy Bypass -File gen-questbook.ps1

It regenerates `DefaultQuests/` from scratch (BOM-less, filename-safe base64 IDs).
Do not hand-edit the generated JSON — it gets overwritten.
