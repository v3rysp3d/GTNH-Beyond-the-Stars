# Migration guides

## Generic migration
Migration for the typical mod which doesn't use anything special but Minecraft forge and some library dependencies.
For core plugin, Mixins, shadowing, access transformers, ASM or etc. you'll need to do some extra steps.
If they are missing in this document - we will gladly receive your suggestions/contribution.

1. Copy and replace all files from [template](https://github.com/GTNewHorizons/ExampleMod1.7.10/releases/download/master-packages/migration.zip) to your repository, but `build.gradle`
2. Copy all repositories from your `build.gradle(.kts)` to `repositories.gradle`
3. Copy all dependencies from your `build.gradle(.kts)` to `dependecies.gradle`
4. replace your `build.gradle(.kts)` with `build.gradle` from template. In case you have written some custom tasks/configurations not present in the template - move them into `addon.gradle`. It will automatically be integrated if present.
5. Adapt `gradle.properties` to your mod
6. Ensure `src/main/resources/mcmod.info` contains `${modId}`, `${modName}`. `${modVersion}` and `${minecraftVersion}`
7. Re-import the project to your IDE (e.g. restart with clean caches in IntelliJ IDEA)
8. Run `./gradlew clean setupDecompWorkspace`

## Mixin configuration
For the reference checkout the [example mixin configuration branch](https://github.com/GTNewHorizons/ExampleMod1.7.10/tree/example-mixins) of the template.

1. Extract mixins package and plugin configuration from `mixins.yourModId.json` to `gradle.properties`
2. Implement MixinPlugin according to example from the reference
3. Remove mixins.mymodid.json

## Beyond the Stars: Singularity Ladder old-save notes (P2/P3)

These notes are for players updating a world, not for porting the mod. Both switches are in the mod's config file,
category `general`.

**Black Hole Compressor** (`removeBlackHoleCompressorRecipe`, default `true`). Placed Black Hole Compressors keep
working. Only the controller's crafting recipe is removed, and a data stick that holds that recipe stops resolving.
The Kerr Ergosphere Engine takes its place at UIV. On GTNH 2.9.0-RC-2 the recipes that consume a BHC controller also switch to the Kerr controller (see the RC-2 port notes below).

**TecTech Eye of Harmony** (`replaceTecTechEyeOfHarmony`, default `true`). At load-complete, Beyond the Stars first
copies every TecTech planet recipe into its own Eye of Harmony, then clears TecTech's recipe storage, its NEI page and
its controller recipe.

- A placed TecTech Eye of Harmony becomes a museum piece: it still forms, but it finds no recipe and idles.
- **Finish or cancel running TecTech EOH recipes before updating.** A run that is in flight when the recipes are
  cleared re-resolves to nothing, and its outputs are lost.
- If the copy fails (for example, TecTech moved its recipe storage), TecTech's Eye is left fully intact and the log
  says so. No planet disappears without a replacement.
- With `replaceTecTechEyeOfHarmony=false`, TecTech's EOH runs as stock beside ours, and the pack's White Dwarf Matter,
  Black Dwarf Matter and Universium sources are untouched.

**Beyond the Stars' Eye of Harmony** (new, MTE 28115).

- Wrenching the controller keeps the Galactic Upgrade Core and the fluid buffers. A run that is in progress when the
  controller is wrenched is lost: its inputs and EU were taken at the start.
- Breaking the structure mid-run also stops the run, and its outputs are lost.
- **Eye cores built before the cost audit's #5 (addon 757660e and earlier, e.g. the 0.24.1-master.32 test jar, by
  `/btseye` or a hologram) un-form.** The pylon foot pads now hold Stabilisation Field Generators only on each pad's
  centre ring of 8 (48 in all, was 294). The other 246 pad cells now take Reinforced Temporal Structure Casing (the
  plinth), and the walk reports the first generator it meets there as a wrong block. Swap those 246 generators for
  Reinforced Temporal Structure Casing and the Eye forms again. The removed generators can be reused elsewhere.

**Kerr Ergosphere Engine** (MTE 28114), builds from before the round-3/4 follow-up pass (addon 4bd6bf2 and earlier).

- The Kerr's lining cells (the white inner face of every arc and band) now take the new **Harmonic Lining**
  (`slCasing2:2`), not the Background Radiation Absorbent Casing. A Kerr built with the old block un-forms until the
  lining is swapped. The compressor arc's outer shell is still Background Radiation Absorbent Casing.
- The Extractor Module now runs GT's item extractor recipes and takes **item buses only**. A fluid output hatch in an
  extractor socket (the earlier `/btskerr` placed one) faults the module until it is swapped for an output bus. The
  old "Ergosphere Extraction" melt recipes are gone; finish any melt run before updating, or its fluid may be lost.

## Beyond the Stars: GTNH 2.9.0-RC-2 port (2026-10-12)

**Pack version.** From this build on, Beyond the Stars targets **GTNH 2.9.0-RC-2** (GT5-Unofficial 5.09.54.205,
StructureLib 1.4.45, coremod 2.9.84). It does not load on 2.9.0-beta-1: the GT++ recipe maps it registers into moved
into GT's `RecipeMaps`, and the beta-1 jar does not have them. Update the pack first, then this mod.

**For developers.** `dependencies.gradle` pins the two RC-2 lines. `runClient` / `runServer` also pull the coremod,
BetterQuesting, BlockRenderer6343, Avaritia, Universal Singularities (with WanionLib), Avaritiaddons and Eternal
Singularity as `runtimeOnlyNonPublishable`, so the dev server registers the recipes the port rewrites. None of them is
a compile or published dependency. The spec's "GTNH 2.9.0-RC-2 port" section lists the API changes.

**Black Hole Compressor in recipes** (`removeBlackHoleCompressorRecipe`, default `true`). In 2.9 the BHC controller is
also an ingredient: every Spacetime Compression Field Generator tier and the Empty Spatially Compressed Circuit Array
(64) consume one. With the switch on, those recipes now take a **Kerr Ergosphere Engine controller** instead, same
count, and the BHC controller recipe is removed. BHC controllers you already have can no longer be used in those
recipes; keep them as machines (placed BHCs work as before). If the Kerr controller recipe failed to register, the
BHC recipe and its uses are left as the pack ships them, and the log says so. With the switch off, nothing changes.

**Kerr controller recipe.** SL-K01 is cheaper now (about one BHC controller's worth, including the BHC's compressor
lineage). A data stick holding the old recipe resolves to the new one, because sticks store only the output.

**Compression generator recipes (Eye T2-T4 gates).** The beta-1 "upgrade the generator" assembly lines
(SL-P30..P32) are gone, and so are their data sticks' recipes. 2.9 makes every generator in the Condensate
Assembler. With `replaceTecTechEyeOfHarmony=true` there is one extra Condensate Assembler recipe (SL-P35) for
Resonance Chamber II that uses SpaceTime foil instead of White Dwarf Matter foil, so the tier 1-3 generators can be
made before Eye T4. Generators you already crafted are the same items and still work.

**Reactor fluid sockets.** The feedwater, chamber-coolant and propellant sockets accept plain Input Hatches only. A
crafting input buffer (CRIB) or its slave in one of them now un-forms the plant, and the structure check names the
cell. Swap it for an Input Hatch. Without this check, 2.9 would have accepted the buffer and it would never have fed the plant.

**Kerr utility hatches.** The BHC utility hatch now stores its on/off state (`isOn`) instead of a pulse timer. Existing
hatches load cleanly; a hatch in pulse mode starts the first second after a reload off, as GT's own BHC does.

## Beyond the Stars: Kerr redesign (branch kerr-redesign, 2026-10-12)

**What changed.** The Kerr Ergosphere Engine (MTE 28114) now controls only the black hole. The compressor and
extractor modules are separate machines: the **Kerr Compressor Module** (MTE 28117) and the **Kerr Extractor
Module** (MTE 28118), each with its own controller, GUI, hatches and energy, standing in its arc of the cage. A module
runs only while the Kerr's hole is open. There are no machine modes and no screwdriver controls any more: open the
Kerr's GUI.

**Switch the Kerr off and let any running recipe finish before you update.**

**Existing Kerrs convert themselves.** About two seconds after a Kerr's chunks load, it replaces the old module
core blocks in its arcs:

- Compressor arc: the core block becomes Background Radiation Absorbent Casing, and a Kerr Compressor Module
  controller goes into the outer shell cell just below it, facing out of the cage.
- Extractor arc: the core block (on the inner face) becomes Ergosphere Extraction Casing, and a Kerr Extractor Module
  controller goes into the outermost shell cell of the arc at that height, facing out.
- The hatches already in the arcs stay where they are and become the modules' own hatches. **Each module now also
  needs its own energy hatch** (any socket in its arc), and the Kerr's own energy hatches feed its containment power
  (8,388,608 EU/t while the hole is open).
- If a controller cell is not plain casing or air, that arc is not converted and the Kerr's GUI says so. Clear the
  cell and run `/btskerr migrate` (an operator command) while standing near the Kerr, or `/btskerr migrate x y z`.
- A recipe that was running in the old Kerr is held in the Kerr (nothing is lost) and handed to the module it ran
  on as soon as that module forms, which is after you give it its energy hatch. Items that do not fit its output
  buses drop at the module controller; fluids wait in the Kerr until its output hatches have room. The Kerr's GUI
  says what it is holding.
- The Kerr no longer needs a maintenance hatch. An old one in the governor arc can stay.

**Items.** Old module core blocks in chests craft 1:1 (shapeless) into the matching module controller. The Kerr
Compressor / Extractor Module assembly-line recipes keep their inputs and now make the controllers.

**Settings that changed meaning.** The setpoint is now any whole number of SM (2-64, or 2-512 with the Density
Upgrade); an old setpoint carries over. "Auto" (on by default) keeps the old behaviour: a Black Hole Seed or
Collapser placed in a bolt bus acts on its own.
