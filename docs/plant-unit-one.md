# UNIT ONE — plant architecture plan (approved mockup -> v0.9+)

Mockup: (review mockup, kept outside the repo)
Ruling (final, v0.10): "This flooring needs to be the ENTIRE way of the 50x50 multiblock,
this is the starter structure and every addon will be added to this." The 50x50 FOUNDATION
PLATFORM is the only mandatory structure; the Steam Generator silo is the FIRST addon built
onto it, the Turbine Hall docks directly beside it second, then everything else. Numbers
marked (proposed) are tuning knobs for sign-off.

## Mandatory starter — Foundation Platform (50x50)

- Full 50x50 Light Concrete platform. The CONTROLLER is the main station: it stands on
  the front line of the shield pad, and every silo tier's front wall is anchored on that
  line, so once the silo is built the controller is embedded in the silo face at ground
  level (one fixed spot for all five tiers).
- Tungstensteel-Reinforced shield pad (max-tier octagon, 249 blocks) marks the reactor
  site; 8 flush Solid Steel markers set INTO the floor mark the module dock positions.
  Dock positions stay CLEAR (air) until a module controller stands on them - nothing is
  auto-placed there.
- Bare platform forms and idles (no hatches required); GUI says "Steam Generator: NOT
  BUILT" until the silo is projected onto the shield pad.
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
- No modules docked = fluid exporter (Steam / Hot Coolant), exactly as today.

## Addons (each an optional structure piece at a fixed offset; GUI lists all with status)

1. **Turbine Hall** — RETIRED (2026-08-18 Steam Dynamics ruling): replaced by the
   rod-cascade → duct/housings → injection-spiral → Large Turbine Rotor Assembly chain.
   Full design and math in docs/steam-dynamics.md; questbook overlay in questbook/.
2. **Cooling Tower** (up to two, hyperboloids): the recycling addon (proposed):
   each tower returns condensate - water draw x0.5 (both: x0.1); doubles passive heat
   decay; halves decay-heat water demand. Two towers = shutdown cooling fully passive.
3. **Extraction Derrick** (attached lattice tower): self-mining (proposed): the plant
   trickle-mines uraninite/thorium/tantalum at standalone-Platform rates into its own
   buses, drawing extra EU from the energy hatches.
4. **Enrichment Annex** (long process building): on-site pellet line (proposed): converts
   bus uraninite + HF acid into pellets at standalone-Train rates each cycle. Derrick +
   Annex + an external centrifuge = a near-self-fueling plant.
5. **Vent Stack**: safety systems (proposed): decay-heat window and meltdown radiation
   duration both halved.
6. **Switchyard**: grid conditioning (proposed): outputs above 100M EU/t require it
   (soft cap without it); visual transformer yard.

## Implementation notes (Space Elevator architecture - review ruling)

- The plant works like the GTNH Space Elevator: the platform is the MAIN controller whose
  flooring contains designated DOCK PADS; each addon building is its own MODULE MACHINE
  with its own controller (like space pumps/assemblers docking into the elevator) standing
  on a pad. A module only functions while docked to a formed platform; the main controller
  lists docked modules in its GUI and feeds/receives their effects. The Steam Generator
  silo is NOT a module - it is an optional second piece of the main controller itself.
- Module MTEs: Turbine Hall, Cooling Tower, Extraction Derrick, Enrichment Annex,
  Vent Stack, Switchyard - each with its own structure piece and MTE ID.
- Standalone Extraction Platform / Enrichment Train machines REMAIN as the IV entry tech;
  the plant addons are their integrated endgame form.
- Rooftop Turbine Deck is removed when the Hall lands (pre-release; rebuild expected).
- Survival cost ~40k blocks at full buildout - "insanely expensive once" fulfilled;
  creative testing via projector auto-build per piece.

## Phasing

- **P1**: silo reshape (ribs, bands, dome, spaced lattice) + Turbine Hall addon (port deck
  logic). Review swarm + smoke test + deploy.
- **P2**: Cooling Towers + Vent Stack (heat/recycling systems).
- **P3**: Derrick + Enrichment Annex + Switchyard (self-fueling + conditioning).
