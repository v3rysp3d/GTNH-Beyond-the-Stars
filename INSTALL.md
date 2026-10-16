# Installing GTNH: Beyond the Stars

A short guide for playtesters and server owners. The [README](README.md) explains what the mod adds.

## Requirements

- **GTNH 2.9.0-RC-2, exactly.** The mod is built against its GregTech (5.09.54.205), StructureLib 1.4.45 and
  GTNewHorizonsCoreMod 2.9.84. With an older GregTech, Forge stops at its "missing mods" screen asking for
  `gregtech` 5.09.54.205 or newer. It does not run on GTNH 2.8.x or the 2.9.0 betas. A later RC may load but can fail
  in a GregTech class; wait for an update for those.
- **Java:** tested on Java 8 and on Java 21 (with lwjgl3ify, as the pack's Java 17+ builds use). Use the Java your
  RC-2 instance already runs.
- **One jar** goes on the client and on the server: `beyondthestars-<version>.jar`. Do not use the `-dev` or `-sources`
  jars; they are for development and break in a real pack.
- Optional: BetterQuesting (the questbook) and OpenComputers (the computer components) are already in the pack.

## Before you install: back up the world

Copy `saves/<world>` (single player) or the server's world folder somewhere safe. The quest progress lives there too.
This is a playtest build: expect balance changes between builds, and play on a copy of a world you care about.

## Client (single player)

1. Close the game and back up the world.
2. Remove any older `beyondthestars-*.jar` from `mods/` (and from any subfolder of it: a second copy crashes at start).
3. Put the new jar in `mods/` and start the game. The first start takes about a minute longer while the recipe
   changes run.
4. Open the world. The questbook installs itself.
5. In `logs/fml-client-latest.log`, look for `[BTS] Recipe registration complete: ... registered, 0 skipped.` A
   "SKIPPED" count or a `[BTS render]` warning is worth a bug report.

## Server

1. Stop the server and back up the world.
2. Remove older `beyondthestars-*.jar` files from `mods/`, put the new jar in, start the server.
3. Every player needs the same jar. Copy the server's `config/beyondthestars.cfg` to every client as well: each side
   builds its recipes from its own config, so a client with different recipe switches shows recipes in NEI that the
   server does not have (crafting always follows the server). At login the server sends its recipe switches, and a
   client whose values differ gets a chat line naming each one (server value, client value). `/btsrecipes sync`
   lists what the server sends.
4. Check `logs/fml-server-latest.log` for the same lines as above. The questbook installs when the server starts.
5. Every `/bts...` command needs operator permission. Several are testing tools (instant builds, free machines, a time
   warp), so give operator rights only to people you trust with them.

## What changes in an existing world

With the default config the mod changes some of the pack's own content. Each change has a switch in
`config/beyondthestars.cfg`; set it to `false` before the first start to keep the pack's version.

- **TecTech Eye of Harmony** (`replaceTecTechEyeOfHarmony=true`): our Eye of Harmony takes over TecTech's recipes.
  **A TecTech Eye of Harmony you have already built stops working: it still forms but finds nothing to run.** Set
  `replaceTecTechEyeOfHarmony=false` to keep it. Finish or cancel any running TecTech Eye recipe before the first start
  either way; a run in progress is lost. The same switch also rewrites the Resonance Chamber II-IV recipes, moves
  Eternity onto Ergomatter and makes the Godforge's magmatter upgrade take Black Dwarf Matter frames.
- **Black Hole Compressor** (`removeBlackHoleCompressorRecipe=true`): its controller recipe is removed and the
  Kerr Ergosphere Engine takes its place. Placed compressors keep working, but every recipe that used a compressor
  controller as an ingredient (the nine Spacetime Compression Field Generators, the Empty Spatially Compressed Circuit
  Array) now takes a Kerr controller.
- **Special circuits** (`raiseStockSpecialCircuits=true`): the Nanochip Assembly Complex's Piko, Quantum, Raw Manifold
  Bud and Planck steps take 2.5 times as long and twice the items and fluids.
- **TecTech's Reinforced Spatial / Temporal Structure Casing** recipes make 32 per craft instead of 4 (no switch).
- **AE2 patterns and data sticks** encoded for any recipe above no longer match. Re-encode them after the first start
  (NEI shows the new recipes).
- **The questbook** (`questbookPackEdits=true`): 66 of the pack's quests are edited (the Black Hole Compressor quest
  becomes the Kerr Ergosphere Engine, the Eye of Harmony quest becomes our Eye, the UXV / MAX / endgame quests move
  into our chapters) and the emptied Endgame Goals chapter is hidden. Completed quests stay completed; four edited pack
  quests restart their tasks once. Set `questbookAutoInstall=false` to install the book only when an operator runs
  `/btsquests install`.
- **Universium** comes only from our Eye T10 (the Universe). Nothing before T10 needs it; the pack's own Universium
  line (MHDCSM, the Stargate) opens in The Endgame chapter once T10 runs. With `replaceTecTechEyeOfHarmony=false`,
  TecTech's Eye of Harmony still makes Universium as the pack ships it.
- **MAX parts** (no switch): the eight MAX components, the MAX machine casing and hull, the MAX wireless energy hatch
  and the Component Assembly Line's MAX casing get recipes on Attractor Dark Matter (Eye T9); RC-2 had none. The pack's
  UXV 4096A laser hatches get a second recipe on Magnetar Matter. New MTE ids 28133, 28134, 28136 and 28137 (the wired
  MAX energy, dynamo and laser hatches).
- **Supermassive Lattice nodes** (`latticeNodesUseP5Matter=true`): the L2 node is made from Attractor Dark Matter and
  the L3 node from Universium. Set it to `false` to keep the older L2 / L3 recipes; nodes already built stay.
- **Retired fluids:** the placeholder fluids `btswhiteholematter` and `btsentangledwhitehole` are gone (no recipe ever
  made them). A tank that held one loses it.

## Removing the mod

1. Run `/btsquests uninstall all` (operator) while the mod is still installed. It restores every edited pack quest,
   brings Endgame Goals back and removes our chapters.
2. Take the `BTS_` components out of any Nanochip Assembly Complex Vacuum Conveyor (as items); a conveyor holding
   one deletes it on load once the mod is gone.
3. Break and keep anything of ours you want to keep: our machines, blocks, items and fluids disappear from the world.
4. Back up the world, then take the jar out of `mods/` (on the server and on every client).
5. A server then stops at Forge's "missing blocks or items" question; start it once with `-Dfml.queryResult=confirm`,
   or answer `/fml confirm` on the console.

The pack's recipes (the Black Hole Compressor, TecTech's Eye of Harmony, the circuit costs) come back by themselves:
the mod only changes them in memory while it is loaded.

## Reporting bugs

Open an issue at <https://github.com/v3rysp3d/GTNH-Beyond-the-Stars/issues> with the jar's file name, your Java
version, the log (`logs/fml-client-latest.log` or the server's), and a screenshot of the machine's GUI if a machine
is involved.
