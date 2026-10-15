# Migration notes

Per-build notes for updating an existing world.

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
Assembler. With `replaceTecTechEyeOfHarmony=true`, GT's own Condensate Assembler recipe for Resonance Chamber II
uses 2 SpaceTime foils instead of the White Dwarf Matter foil (rewritten in place, SL-G01; earlier jars had a second
recipe, SL-P39, instead), so the tier 1-3 generators can be made before Eye T4. Resonance Chamber III and IV are
rewritten the same way (SL-G02: 2 White Dwarf Matter foils for the Black Dwarf Matter foil; SL-G03: 2 SpaceTime foils
for the MHDCSM foil plus 1,500 L Entangled Ergomatter); earlier jars had them as second recipes (SL-P33, SL-P40), and
NEI now shows one recipe per chamber. Chambers already crafted are the same items. Generators you already crafted are the
same items and still work, and from this jar they upgrade one tier at a time in the Condensate Assembler (SL-U01..U28):
the old generator plus what the next tier adds, its Kerr controller kept inside.

**Renamed in the nuclear plant (display names only; ids, items and saves unchanged).** Enrichment Train -> Yellowcake
Mill, Spine Sensor -> Spiral Cycle Sensor, Naquadah Propellant / Propellant Cartridge Lv 1-6 -> Grade 1-6, milestone line
REPROCESSING -> ORE PROCESSING, upgrade node Extra Centrifuge Stage -> Extra Leach Tank. GregTech stores machine and
fluid names in `config/GregTech/GregTech.lang` and never rewrites them, but it prefers a key that a mod's lang file
provides, and ours now provides these, so existing installs show the new names as well; the old GregTech.lang lines
just stop being used. The production halls now honour the Power Panel's parallel limit when its "max" box is unticked
(ticked by default) and offer input separation (off until you turn it on). `/btsbuild` with no tier now builds tier 1.

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

**Eye of Harmony GUI and centre render, D-49 unit 6.** Nothing to do. The Eye now has a full GUI (gauges, setpoint
buttons, transitions, the last failure) and draws its star at the centre of the structure while it is formed and its
star burns. The render is a hidden, unbreakable block (`beyondthestars:gt.beyondthestars.eyeStar`) that the Eye places
into the empty centre cell; if that cell holds a block, the GUI and the scanner name it and nothing is placed. It goes
away with the star or the Eye, and removes itself if its Eye is gone. Turn it off per Eye in the GUI, or everywhere
with `eyeRenderCentre=false` in `config/beyondthestars.cfg` (the file is now rewritten on every start, so its comments
are current; values are kept). The Stellar Field Sensor Hatch now leaves covers on its faces alone; GT's internal
redstone transmitter covers carry its signal.

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

## Beyond the Stars: Circuits v2 (Sidereal, Empyrean, Omega)

Three new circuit families, made in stock GT machines and in the Nanochip Assembly Complex (NAC). The config switches
are in `config/beyondthestars.cfg`, category `general`.

**Empty the NAC's Vacuum Conveyors of Beyond the Stars components before you remove the mod** (or before you run a
build whose NAC patch is inactive; the log says "NAC patch inactive" once). Our 34 NAC components (`BTS_...`, ids
10000-10211) exist only while the mod's patch is loaded. When a saved conveyor or module holds a component the game no
longer knows, it is **deleted** on load, not crashed: the log names each one once, with its amount. Get them back
out as items first, the way you would GregTech's own components, then remove the mod.
`nacCircuitComponents=false` does not delete anything: the components stay known and can still leave the NAC, only our
NAC recipes and the item -> component conversions stop.

**Piko, Quantum and Planck cost more** (`raiseStockSpecialCircuits`, default `true`; factors
`stockSpecialCircuitEnergyFactor` 2.5 and `stockSpecialCircuitMaterialFactor` 2.0). The NAC Assembly Matrix recipes
for Piko, Quantum, the Raw Manifold Bud and Planck take 2.5x the time and twice the items and fluids. A run already in
the matrix when you update finishes at the old price. With the switch off, the stock recipes are untouched.

**`stellarCircuits=false`** keeps the 28 items registered (so nothing in a world disappears) but hides them from NEI,
gives them no recipe and no circuit ore name, and their tooltip says "Disabled by config". Circuits already crafted
then fit no recipe until the switch is back on. The quests stay in the book; the pack-quest notes that point at our
circuits (UMV / UXV / MAX Circuits) are taken out on the next server start, and put back when the switch is on.

**Questbook.** Six quests are added to the existing chapters (Stellar Silicon, Sidereal Circuits, Stellar SMDs,
Empyrean Circuits and Circuits in the Nanochip Complex in Tier 12 - UMV; Omega Circuits in Tier 13 - UXV), and the
pack's UMV / UXV / MAX Circuits quests gain a note. The mod installs them on the next server start; progress is kept.
