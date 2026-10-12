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
Assembler. With `replaceTecTechEyeOfHarmony=true` there is one extra Condensate Assembler recipe (SL-P39) for
Resonance Chamber II that uses SpaceTime foil instead of White Dwarf Matter foil, so the tier 1-3 generators can be
made before Eye T4. Generators you already crafted are the same items and still work.

**Reactor fluid sockets.** The feedwater, chamber-coolant and propellant sockets accept plain Input Hatches only. A
crafting input buffer (CRIB) or its slave in one of them is refused. A CRIB in the feedwater socket drops the silo,
so the plant stands as a bare platform (silo T0; Spine, Spiral and gallery MISSING), and no error names the cell yet
(B-32). Swap it for an Input Hatch. Without this check, 2.9 would have accepted the buffer and it would never have fed
the plant.

**Enrichment Train.** Its casings no longer take crafting input buffers or their slaves, in either the bus or the
hatch role. The Train draws its uraninite and acid by hand from plain buses and hatches, so a buffer there never fed
it, even before 2.9. A Train with a buffer in its casing now fails to form at that cell. Swap the buffer for an Input
Bus or Input Hatch.

**Kerr utility hatches.** The BHC utility hatch now stores its on/off state (`isOn`) instead of a pulse timer. Existing
hatches load cleanly; a hatch in pulse mode starts the first second after a reload off, as GT's own BHC does.

**Eye of Harmony structure, D-49 (T5/T6 bands and the stability-field sockets).** The core band now has 8 Field
sockets and 8 Sensor sockets on its outer face. In an Eye built before D-49 those 16 cells hold Reinforced Spatial
Structure Casing, and both sockets accept that casing, so existing Eyes still form and need nothing. Two new optional
bands follow the spiral arms: the second arm pair (T5) and the galactic rim (T6, with 4 more stellar sockets).
**Bands are now cumulative:** each band counts only while every band below it stands. An Eye built with the jet
crescent but no beam band now runs as **T1** until the beam band is built; the scanner names the gap. Plinth gas
sockets also take an input bus (for the Stellar Ignition Charges); a crafting input buffer there is refused.

**Eye of Harmony stars, D-49 (live stars and the tier unlock).** Stars are no longer batch runs. Each star burns
live: it lights from a Stellar Ignition Charge in a plinth input bus (plus 10 cycles of upkeep and EU), takes its
EU and upkeep every 60 s cycle, and delivers its product to ME at the end of every cycle, scaled by how well you hold
its stability field (Field and Sensor sockets on the core band). Planets still run as batch runs.

- **Unlocked tier.** Every Eye now stores the tier it has unlocked; a tier runs only when it is unlocked, built and
  gated by compression. New tiers unlock by milestones on the current star, the next core (right-click it into the
  escrow) and that tier's transition.
- **Existing Eyes keep what they had.** An Eye that carried the Galactic Upgrade Core is unlocked to **T4** (its
  neutron star still needs a lit Arcturus and a core-free Core Collapse). Any other Eye from 0.24.x is unlocked, at
  its first structure check after the update, to the tier its bands gave it under the old rule (the jet crescent
  counted without the beam band): T1-T3. Bands are cumulative now, so a jet-without-beam Eye is unlocked to T3 but
  runs as T1 until the beam band is built.
- **A star run in flight is paid in full.** Its inputs and EU were paid when it started, so the Eye credits the whole
  run (Sol 1,000,000 L Raw Star Matter, Sirius A 100,000 L Excited Stellar Plasma, Arcturus 50,000 L Raw Hyperdense
  Stellar Plasma, Sirius B 18,432 L White Dwarf Matter, Sirius B Cooled 4,608 L Black Dwarf Matter, per success) to
  its ME output once it stands formed, and logs it. Planet runs in flight finish as before.
- **Buffers are kept**, including leftover helium from the old Sirius B run.
- **Charts.** Star Chart: Sirius B is now the T5 target (Black Dwarf Matter). T4 is Star Chart: Neutron Star. An Eye
  with Sirius B in the slot shows the T5 lock until it is unlocked.
- **Wrenched controllers** carry the unlocked tier, the milestones, the escrowed core and the setpoints. A
  controller wrenched to an item **before** the update carries the Galactic flag (T4) or its buffers if it had any;
  one wrenched with neither has no item NBT at all, so it is indistinguishable from a new controller and starts
  unlocked at T1.
- **Galactic Upgrade Core.** It is no longer installed by right-click: it goes into the escrow and is consumed by a
  successful Core Collapse.
