# UNIT ONE — plant architecture plan (shipped through 0.21.0)

Live mockup: `docs/mockup/unit-one.html` — every voxel is one placeable block, at all five
tiers, with the four plant modules drawn at their real platform rectangles.

Ruling (final, v0.10): "This flooring needs to be the ENTIRE way of the 50x50 multiblock,
this is the starter structure and every addon will be added to this." The 50x50 FOUNDATION
PLATFORM is the only mandatory structure; the Steam Generator silo is the FIRST piece built
onto it, then the steam chain, then everything else.

**Nothing in this plan docks.** The Turbine Hall that once "docked beside" the silo was
retired by the 2026-08-18 Steam Dynamics ruling, and D-20 deleted the dock-pad architecture
outright: every building is an optional checked PIECE of the one main-station controller.
Numbers below are the shipped numbers, not proposals.

## Mandatory starter — Foundation Platform (50x50)

- Full 50x50 Light Concrete platform. The CONTROLLER is the main station: it stands on
  the front line of the shield pad, and every silo tier's front wall is anchored on that
  line, so once the silo is built the controller is embedded in the silo face at ground
  level (one fixed spot for all five tiers).
- Tungstensteel-Reinforced shield pad (max-tier octagon, 249 blocks) marks the reactor
  site. The 8 flush Solid Steel dock markers are RETIRED with the docks (D-20); those cells
  are plain Light Concrete now, and a legacy platform's steel is accepted as-is.
- Bare platform forms and idles (no hatches required); GUI says "Steam Generator: NOT
  BUILT" until the silo is projected onto the shield pad.
- ONE-PROJECTION BUILD (review ruling): the main controller's hologram builds the ENTIRE
  plant - platform, silo, Spine duct, Spiral, rotor gallery + manifold, both Cooling Towers
  and all four plant modules. There is no module controller to stand anywhere and no dock
  marker to stand it on. The player swaps hatches into the sockets and the plant is done.
  The questbook's "The Blueprint" quest is the walkthrough.
- Controller GUI settings (PCB-Factory style): control-rod throttle 1-24 AND Build Tier
  1-5 - the tier the hologram/projector builds (a trigger stack of 2-5 overrides it).
- Periodic self-revalidation (600t idle / 200t running) because block updates cannot
  cross the unregistered concrete to the controller; modules reconnect instantly after
  every successful structure check.

## First addon — Steam Generator silo (piece of the main controller)

- Five tiers (hologram trigger 1-5), ribbed octagons 9-17 wide, walls 2x diameter tall +
  hemispherical shield dome; radiation-proof casing bands at the thirds. Stands on the
  platform's shield pad; hatches live in the silo walls.
- Tier wall materials unchanged (RadProof -> Europium-Reinforced -> Radiant Naq Alloy);
  dome TS-Reinforced -> Iridium TS.
- Column caps stay 45/77/109/157/201, so ALL existing output math carries over unchanged.
- Silo alone, no other piece standing = fluid exporter (Steam / Hot Coolant), exactly as
  today. Every other piece is optional and the plant runs without all of them.

## The optional pieces (all raised by the SAME projection; every panel lists them with status)

There is no such thing as an "addon controller" in this design. Each entry below is an
OPTIONAL CHECKED PIECE of the main station: a fixed platform rectangle with no tier term in
its offsets, walked on every structure check, and free to be absent. A piece that has blocks
standing but will not form reports why on a line every readout carries in every state.

1. **Turbine Hall** — RETIRED (2026-08-18 Steam Dynamics ruling): replaced by the
   rod-cascade → duct/housings → injection-spiral → Large Turbine Rotor Assembly chain.
   Full design and math in docs/steam-dynamics.md; questbook overlay in questbook/.
2. **Cooling Tower** (up to two, hyperboloids) — SHIPPED 0.20.0. 17x17x20 shells on the
   LEFT platform strip, one in front of the silo face (`a 0..16, c 0..16`) and one behind
   the shell (`a 0..16, c 33..49`). Each tower returns condensate - water draw x0.5
   (both: x0.1); doubles passive heat decay; halves decay-heat water demand. Two towers =
   shutdown cooling fully passive.
3. **Extraction Derrick** (attached lattice tower) — SHIPPED 0.21.0. 7x7 deck under a
   20-block lattice tower, platform `a 33..39, c 4..10, y 0..19`. Every 200 powered ticks
   it mines 6 Uraninite Dust + 1 Thorium Dust, plus 1 Tantalum Dust on every 4th cycle,
   into its OWN Output Bus (`a 36, c 4, y 1`, declared FORWARD), drawing 1,920 EU/t
   (`TierEU.RECIPE_EV`) from the manifold's Energy Hatches. Those are the standalone
   `MTEExtractionPlatform`'s numbers, unchanged.
   Bill: 144 Solid Steel Machine Casing + 80 Steel Frame Box + 19 Steel Pipe Casing +
   1 Output Bus = **244 blocks**.
4. **Enrichment Annex** (long process building) — SHIPPED 0.21.0. 5x15x6 process shed
   along the platform's front edge, `a 17..21, c 0..14, y 0..5`. Every 600 powered ticks
   it converts 8 Uraninite Dust + 2,000 L Hydrofluoric Acid into **2 Yellowcake** +
   2 Thorium Dust, drawing 480 EU/t (`TierEU.RECIPE_HV`). The standalone
   `MTEEnrichmentTrain`'s numbers and rate, unchanged.
   **M7 (0.24.0): it pays YELLOWCAKE, not finished Fuel Pellets.** 4 Uraninite per cake is
   exactly what F-01 .. F-08 reach by hand (16 -> 4), so the module buys SIMPLICITY and
   never yield. Derrick + Annex make **feedstock**: the pair join the fuel line at F-09 and
   F-09 .. F-18 stay compulsory. **No module in this addon makes a finished rod.** A plant
   is self-fuelling once the fuel line is standing, and not before. Anything reading either
   module's output socket - AE2 storage bus, pattern, pipe filter - is handed a different
   item than it was in 0.23.0.
   Three sockets, all on the `+a` wall at `a 21, y 1`, declared LEFT: Input Bus (`c 3`,
   uraninite), Input Hatch (`c 7`, Hydrofluoric Acid), Output Bus (`c 11`, yellowcake).
   Bill: 291 Clean Stainless Steel Machine Casing + 13 Steel Pipe Casing + 3 sockets =
   **307 blocks**.
5. **Vent Stack** — SHIPPED 0.21.0. 7x7x20 scrubber house and flue on the left strip
   in the gap between the two Cooling Towers, `a 2..8, c 21..27, y 0..19`. Halves BOTH
   safety windows: the post-shutdown decay-heat obligation 24,000t -> 12,000t (20 min ->
   10) and the meltdown radiation field 36,000t -> 18,000t (30 min -> 15). Both windows are
   clamped EVERY TICK, not merely stamped when the obligation is armed, so a stack finished
   halfway through a cooldown shortens THAT cooldown. No sockets at all.
   Bill: 161 Clean Stainless Steel Machine Casing + 152 Filter Machine Casing =
   **313 blocks**.
6. **Switchyard** — SHIPPED 0.21.0. 21x6x6 transformer yard on the platform's back edge,
   `a 20..40, c 44..49, y 0..5`. Without it the plant is SOFT-CAPPED at 100,000,000 EU/t;
   the reactor still derives its full figure and every panel prints what is being discarded
   and what to build. Config flag `switchyardSoftCap` (default true) turns the cap off.
   The cap only ever LOWERS what is injected, so it can never push the plant past what the
   fitted hatches absorb. No sockets at all.
   Bill: 261 Solid Steel Machine Casing + 90 Steel Frame Box = **351 blocks**.

### How a module is detected, and what it costs to skip one

- **Detection** is the ordinary structure walk. `checkMachine` calls `checkPiece` for each
  module at its fixed offset and records the result in that module's own `pieceErrors`
  list. Nothing a module says can un-form the plant: a missing module is simply a module
  that is not standing, and a module with blocks up that will not form prints its reason
  through `pieceFault()` on the ungated `moduleLine`, exactly the way the towers do.
- **Sockets belong to the building they are cut into.** The Derrick's bus and the Annex's
  three sockets go through `ModuleHatchElement` into dedicated fields; none of those adders
  calls its super, so none of them ever reaches `mInputHatches` / `mInputBusses` /
  `mOutputBusses`. Nothing the reactor demands can reach them, the rod cascade can never
  eat the Annex's uraninite, and a depleted rod can never land in the Derrick's bus.
- **Cost of skipping one:** Vent Stack — the two safety windows stay at their full 24,000t
  / 36,000t. Switchyard — everything above 100,000,000 EU/t is discarded, which on a tuned
  tier-5 plant is the overwhelming majority of the output. Derrick / Annex — no fuel cycle,
  and nothing else changes: neither is ever a structure demand and neither stalls the plant.
- **Total module buildout: 1,215 blocks** (313 + 351 + 244 + 307) on top of the plant.
- Every count above comes from `questbook/gen-bom.ps1`, which re-implements the shapes and
  fails loudly if the Java moves underneath it. None of them is hand-counted.

## Implementation notes (ONE controller — D-20 / A3, 2026-08-19)

**The Space Elevator dock-pad architecture described here until 0.21.0 is DELETED.** There
are no dock pads, no module controllers and no docking mechanic anywhere in the design.

- Every building in the plant — silo, Spine, Spiral, rotor gallery + manifold, both Cooling
  Towers, and all four modules above — is an OPTIONAL CHECKED PIECE of the one main-station
  controller, raised by the one hologram projection and detected on every structure walk.
  A piece that has blocks standing and still will not form prints WHY on a panel line that
  every readout carries in every state.
- `MTEModuleCoolingTower` / `MTEModuleTurbineHall` / `MTEPlantModuleBase` survive only as
  museum pieces so old worlds load: MTE ids registered, recipes removed, tooltips pointing
  at the main station. `PlantModuleElement` is deleted.
- The eight flush Solid Steel dock markers are gone from the platform floor. The `'d'`
  element accepts either Light Concrete or the legacy Solid Steel so no one has to re-block
  their platform, and **no module footprint covers a dock cell**, so a retired module
  controller still standing on one can never be in a cell a new piece wants.
- Standalone Extraction Platform / Enrichment Train machines REMAIN as the IV entry tech;
  the Derrick and the Annex are their integrated endgame form and run their exact rates.
- Module block cells go through `protectedElement`: a projection onto a lived-in platform
  never overwrites a standing tile entity, it skips the cell and reports it.
- Survival cost ~40k blocks at full buildout - "insanely expensive once" fulfilled;
  creative testing via `/btsbuild`, which now raises all four modules too.

## Phasing

- **P1**: silo reshape (ribs, bands, dome, spaced lattice) + Turbine Hall addon (port deck
  logic). Review swarm + smoke test + deploy. **DONE** (Hall later retired.)
- **P2**: Cooling Towers + Vent Stack (heat/recycling systems). **DONE** (0.20.0 / 0.21.0.)
- **P3**: Derrick + Enrichment Annex + Switchyard (feedstock + conditioning). **DONE**
  (0.21.0.)
