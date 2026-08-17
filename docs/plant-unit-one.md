# UNIT ONE — plant architecture plan (approved mockup -> v0.9+)

Mockup: (review mockup, kept outside the repo)
Ruling: the Containment Silo is the ONLY mandatory structure; every side building is an
OPTIONAL addon detected by the one controller, each granting efficiency, recycling, or
capability. Numbers marked (proposed) are tuning knobs for sign-off.

## Mandatory core — Containment Silo

- Five tiers, ribbed cylinder-octagons ~21/25/29/33/37 wide, wall height 62 + hemispherical
  shield dome (peak ~80) + shield floors. Twelve vertical buttress ribs (tier wall material),
  three radiation-proof casing bands, warning beacon.
- Tier wall materials unchanged (RadProof -> Europium-Reinforced -> Radiant Naq Alloy);
  shields TS-Reinforced -> Iridium TS.
- Fuel lattice at spacing 3 (mockup look): column caps stay ~45/77/109/157/201, so ALL
  existing output math carries over unchanged.
- No addons built = fluid exporter (Steam / Hot Coolant), exactly as today.
- Controller: silo wall at ground level, front center.

## Addons (each an optional structure piece at a fixed offset; GUI lists all with status)

1. **Turbine Hall** (gabled building, pipe-bridged): REPLACES the rooftop Turbine Deck.
   Carries the whole T/P system - fan in input bus, dynamos in hall walls, naquadah
   propellant with blowout/seizure. EU generation only exists with this addon.
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

- The plant works like the GTNH Space Elevator: the silo is the MAIN controller whose
  structure contains designated DOCKING positions; each addon building is its own MODULE
  MACHINE with its own controller (like space pumps/assemblers docking into the elevator).
  A module only functions while docked to a formed silo; the silo lists docked modules in
  its GUI and feeds/receives their effects.
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
