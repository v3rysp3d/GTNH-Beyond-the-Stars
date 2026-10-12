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
The Kerr Ergosphere Engine takes its place at UIV.

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

**Kerr Ergosphere Engine** (MTE 28114), builds from before the round-3/4 follow-up pass (addon 4bd6bf2 and earlier).

- The Kerr's lining cells (the white inner face of every arc and band) now take the new **Harmonic Lining**
  (`slCasing2:2`), not the Background Radiation Absorbent Casing. A Kerr built with the old block un-forms until the
  lining is swapped. The compressor arc's outer shell is still Background Radiation Absorbent Casing.
- The Extractor Module now runs GT's item extractor recipes and takes **item buses only**. A fluid output hatch in an
  extractor socket (the earlier `/btskerr` placed one) faults the module until it is swapped for an output bus. The
  old "Ergosphere Extraction" melt recipes are gone; finish any melt run before updating, or its fluid may be lost.
