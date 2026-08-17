# Reactor Core (Gen I) — v0.2 mechanics spec

Target numbers come from the design plan's output formula (`EU/t = C x 8,192 x F x K x T x P`);
Gen I is the C-scaling stage: uranium fuel (F=1), water/steam loop (K=1), generic turbine (T=1).

## Structure (5x5x5)

- Roof & floor: 5x5 **Tungstensteel Reinforced Block** each (GT `sBlockReinforced` meta 3) —
  the biological shield; no hatches may penetrate it.
- Walls (middle 3 layers): Radiation Proof Machine Casings
  (GT5u `Casings.RadiationProofMachineCasing`); hatches substitute in walls only
  (39 casing minimum of 47). Controller: front center, middle layer.
- Hologram: the controller implements `ISurvivalConstructable`, so the StructureLib
  Hologram Projector shows build hints and survival auto-place works.
- Interior 3x3x3: each of the 9 interior column positions may hold a vertical stack of
  3 **Reactor Fuel Column** blocks (full column) or be air. Structure check counts segments;
  `columns = segments / 3` (floor). 0 columns = structure forms but reactor cannot start.

## Operation

- **Fuel:** starting a campaign consumes `columns` x 1 Fuel Assembly (Uranium) from an input
  bus. Campaign length: 12 h real time (864,000 ticks). Fuel chain: 4x Pellet -> assembler ->
  Fuel Rod; 3x Fuel Rod + steel casing-ish binder -> assembler -> Fuel Assembly.
- **Throttle (control-rod insertion):** programmed circuit in the controller slot.
  Circuit N (1..24) -> power fraction N/24. No circuit = full power. Read at campaign start.
- **Output:** Steam only (Gen I is the water/steam loop stage).
  `steam L/t = columns x 16,384 x throttle` (9 columns full = 147,456 L/t ~ 73,728 EU/t,
  matching the plan's Gen I row). Water draw: `steam / 160` L/t (GT water->steam ratio),
  rounded up, from input hatches.
- **Heat (0..10,000):** +4/t while fueled but water-starved; -2/t otherwise (idle or watered).
  At 10,000: SCRAM — campaign aborts (remaining fuel in that campaign is lost), machine shuts
  off, heat then decays. Explosion + radiation (D-02) intentionally NOT in v0.2; SCRAM-only
  until the vertical-detonation event is built.
- **Info:** `getInfoData` / scanner shows columns, throttle, steam rate, heat.

## Fuel chain pricing (review ruling)

Platinum-line based, "super realistic on costs": rods are platinum-clad (platinum is cheap at
IV), assemblies take rhodium-plated palladium plates + iridium + molten platinum, and the
controller demands IV circuits x4 plus tungstensteel/tungsten/iridium/RhPd spam. Campaigns
eject one **Depleted Fuel Assembly** per column at completion (SCRAM forfeits them) - the
reprocessing chain (-> breeder milestone) consumes these later, aligning with the pack's
existing depleted-fuel convention.

## Gen II+ upgrade pattern (from PCB Factory / Multi Solidifier study)

GT5u's tiered multis register MULTIPLE `addShape` variants on one controller and use
`GTStructureChannels` + `addSubChannelUsage` so the Hologram Projector can project each tier
(the PCB Factory's bio-chamber/cooling addons work this way). Gen II applies the same idiom:
extra shapes for the expanded core + coolant-plant wing, hologram channel selects the build
stage, and efficiency scales per the plan's output formula.

## v0.4 systems (ultracode session)

- **Dual fuel:** MOX assemblies (preferred when stocked) double output (F=2). MOX chain:
  1 Pu dust + 8 U dust -> mixer -> 18 MOX pellets; reprocessing depleted assemblies in a
  centrifuge recovers U + Pu smalls + lead (+ platinum foil chance), closing the cycle.
- **Coolant loop:** if IC2 Coolant is in an input hatch it is preferred: the reactor drinks
  `ceil(steam/160)` L/t of coolant and emits the same amount of Hot Coolant. Via the LHE
  (200 EU per L hot coolant, cold coolant returned 1:1) that is exactly 2.5x the steam
  route - the plan's K=2.5 without a new multiblock.
- **Waste water:** 1 L/t per column of Nuclear Waste Water while producing. Distillation
  tower: 8,000 L -> 1 tantalum dust small + 7,200 L distilled water (D-09 anchor; numbers
  are tuning knobs).
- **Heat ladder (D-02/D-03 settled model):** starvation +4/t; **SCRAM at 8,000** (reactor
  self-disables, GUI shows reason); after EVERY shutdown, decay heat demands 64 L/t of
  water/coolant for 20 minutes; unserved decay heat climbs +2/t; **10,000 = MELTDOWN**:
  vertical detonation column (strength 3, flaming, above the bio-shield) then a 30-minute
  radiation field (radius 12, GT radioactivity effects, full radiation hazmat immune).
  Config toggles: `meltdownExplosions`, `radiationField`; GT's global
  `GregTechAPI.sMachineExplosions` kill-switch is honored.

## v0.6 unified Reactor Silo (architecture ruling)

ONE controller, silo-shaped, addon-based (replaces the separate Turbine Hall):

- **Silo geometry:** octagonal cross-section (corners rounded, wider tiers rounder), 7 tall
  core — shield floor, lower plenum, 3 fuel layers (controller front-center of the middle
  one), upper plenum, containment lid. Five diameters: 5/7/9/11/13 wide; fuel column caps
  9/25/45/77/109 (rounding costs the corners; naq-bonded F is x9 so the ceiling stays ~1B:
  109 x 8,192 x 9 x 5 x 2.5 x 10 = 1,004,544,000 EU/t).
- **Tier materials ("blocks get more expensive"):** walls T1-2 Radiation Proof, T3-4
  Europium-Reinforced Radiation Proof, T5 Radiant Naquadah Alloy; shields T1-2 Tungstensteel
  Reinforced, T3-5 Iridium Tungstensteel.
- **Turbine Deck ADDON** (not a separate machine): built on the lid — two turbine-casing
  layers with a steel-pipe shaft, dynamo hatches, dome cap. Detected as an optional
  structure piece; the machine menu/scanner lists it under "Addon". With deck + a fan in an
  input bus the silo generates EU directly (fan T, stocked-coolant K — coolant is a charge,
  not consumed - propellant P, blowout/seizure). Without it: fluid export as before.

## v0.5 final-gen systems (full formula implemented)

`EU/t = C x 8,192 x F x K x T x P` is now fully in-game:

- **C — five shell tiers** on one controller (PCB-Factory multi-shape): 5x5x5 / 7x7x5 /
  9x9x5 / 11x11x5 / 13x13x5 = 9 / 25 / 49 / 81 / 121 columns. Hologram trigger stack size
  1-5 selects the tier.
- **F — fuel ladder:** Uranium x1, MOX x2, Thorium-Bonded x5, Naquadah-Bonded x8
  (assemblies preferred best-first; each has a depleted form with centrifuge reprocessing).
- **K — coolant:** water/steam x1, IC2 coolant loop x2.5, tier-5 high-flow loop x5
  (double hot-coolant throughput).
- **T & P — the Turbine Hall** (new multi, 3x3x4 tungstensteel turbine casings + steel pipe
  shaft, dynamo hatches required): steam 2 L = 1 EU, hot coolant 1 L = 200 EU with cold
  coolant returned 1:1; output clamped to dynamo capacity (never explodes). Fan in the
  controller slot: Matched x1.25, TDA-Bonded x1.25 (propellant-rated), Optimized TDA x2.5.
  Naquadah Propellant Mk I/II/III (mixer recipes off the GoodGenerator naq fuel family)
  boosts x2/x3/x10, burned in single litres per tick. Propellant on a non-TDA fan =
  BLOWOUT; cutting propellant or working fluid mid-boost = SEIZURE (fan destroyed, 25%
  bang, honors the explosion config).
- **Ceiling check:** tier-5 shell, naq-bonded, high-flow, optimized TDA, Mk III propellant:
  121 x 8,192 x 8 x 5 x 2.5 x 10 = 991,232,000 EU/t — the plan's ~1B row, reached only
  with UHV+ materials exactly as ruled.

## Deliberately deferred

- Gen II+ core expansion (7x7 etc. via PCB-Factory shape/channel idiom), bonded fuels, naq
  propellant, matched turbines, Core Access Hatch AE2 rod rotation, uptime/burnup
  milestones, radiation cleanup items, Extraction Platform + Enrichment Train multis.
