# STEAM DYNAMICS — the reactor-to-rotor chain (review ruling 2026-08-18)

Ruling summary: the plant's steam path becomes a physical, staged system. The reactor
makes a specific reactor steam; rods cascade down real pillars; a 3x3 logic duct carries
steam through four chirality-tuned turbine housings; a 7x7 spiral chamber takes eight
PRECISION 1L injections per steam slug (sensor-timed, misses hurt); and a Large Turbine
Rotor Assembly converts the final flow into "insane numbers". The Turbine Hall module is
RETIRED and replaced by this chain (per AskUserQuestion sign-off: rods replace assemblies
with a penalized bus fallback; spin = speed vs efficiency; +/-2t windows, misses add heat;
hall replaced entirely).

All constants marked (t) are tuning knobs; verify every fluid/item registry name against
the GTNH 2.9.0-beta-1 sources before wiring.

## Stage 0 — Reactor: the decay cascade (rod pillars)

Fuel columns become WORKING pillars, 8 segments tall (fuel section grows from 6 to 8
layers; per-segment steam recalibrated so column totals are unchanged).

- INSERT AT TOP: rods go into the pillar's top segment. A rod falls to the LOWEST empty
  segment - the pillar behaves like a gravity column.
  **(A2, 0.20.0: the normal path is now the ROD INPUT BUS in the silo's own front face,
  on the bottom fuel course - the controller distributes to every pillar itself each
  cycle. The Rod Loading Ports in the upper plenum still work for hand-feeding but are no
  longer required.)**
- BURN AT BOTTOM: only the bottom segment burns. One decay cycle consumes it into a
  depleted rod, which drops into the base extraction buffer (pulled via output bus /
  rod drain at shield-pad level). Everything above shifts down one.
- OUTPUT: a column is LIVE while its bottom segment holds a burning rod. Steam per tick
  scales with live columns exactly as today (C x 16,384 L/t basis x fuel mult of the
  burning rod x throttle N/24).
- THE AUTOMATION CHALLENGE: keep the plant fed. **A2 moved this from "reach 45-201
  individual ports" to "keep ONE bus stocked"** - the challenge is now supply throughput,
  which is a factory problem, not a plumbing chore. Partial pillars are still partial
  output; a starved pillar still goes dark.
- BUS FALLBACK (heavy penalty): classic fuel assemblies in the input bus still run a
  campaign at 50% (t) output so early setups are not bricked. The GUI labels this
  "Assembly fallback - output halved".

## Stage 1 — The Spine: 3x3 steam duct + 4 turbine housings

A custom 3x3 cross-section duct piece leaves the silo and carries reactor steam to the
spiral chamber. Along it sit FOUR Turbine Housing sockets. Each housing takes a rotor
set CLOCKWISE or ANTICLOCKWISE (wrench toggle).

Swirl model - backwash accumulates through the train:

    S(0) = 0                      (swirl entering housing 1)
    d(i) = +1 for CW, -1 for CCW
    riding  (d(i) matches sign of S(i), or S(i)=0):
        speed mult  m(i) = 1 + 0.08 x |S(i)|      (t; all-riding lands at ~x1.55)
    opposing (d(i) against S(i)):
        eff   mult  e(i) = 1 + 0.10 x |S(i)|      (t)
    S(i+1) = S(i) + d(i)

    V1 = V0 x product(m(i))       (steam speed leaving the spine)
    E  = product(e(i))            (EU-per-litre efficiency)
    residual swirl S(4) carries into the spiral chamber as a heat bias:
        q_spine = k1 x |S(4)|     (t)

Consequences (the "worse/better depending on the goal" ruling):
- CW-CW-CW-CW: rides all the way - V x ~1.55, no efficiency bonus, S(4)=4 hot.
- CW-CCW-CW-CCW: cancels - V x1, E x ~1.2, S(4)=0 cool. Fuel-economy build.
- Mixed patterns land in between; the optimum depends on whether you are chasing peak
  EU/t (speed, more coolant) or EU per litre of steam (efficiency, cool running).

## Stage 2 — The Spiral: 7x7 injection chamber

A 7x7 chamber, ~12 tall, whose internals spiral the steam DOWNWARD past eight
PRECISION INJECTOR hatches. Steam moves in SLUGS: one slug enters every 20 ticks (t).

- SENSOR OUTPUT: a Sensor Hatch emits a redstone pulse (and comparator level) the tick
  a slug enters. This is the timing reference the ruling demands.
- THE WINDOW: each injector must fire EXACTLY 1L of propellant within +/-2 ticks (t) of
  the pulse. In-window: that injector's boost applies to the slug. Out-of-window or a
  second litre in the same window: the litre is WASTED and the chamber takes a heat
  spike (+50 heat, t).
- VELOCITY: with propellant stage factor P (table below) and n of 8 injectors timed:
      V2 = V1 x (1 + (P - 1) x n / 8)
  All eight timed = the full factor P. This is where the "crazy amounts" live.
- HEAT AND COOLANT (the seizure math): chamber heat load per tick
      q = k2 x V2^2 + q_spine                    (t, superlinear in speed)
  coolant drain per tick
      L/t = ceil(V2^2 / Q)                       (Q = coolant quality, table below)
  Coolant starvation -> heat climbs; at the seize threshold the ROTOR ASSEMBLY SEIZES:
  rotors destroyed + shutdown (family of the existing SCRAM/decay model, own shutdown
  reason + lang line). The faster it spins, the more it drinks.

## Stage 3 — Large Turbine Rotor Assembly (replaces the Turbine Hall)

The final multiblock: a long rotor gallery fed by the spiral chamber's downcomer.
Dynamo hatches take:

    EU/t = steam L/t x V2 x E / 2                (t; dynamo-clamped < 2^31 as today)

The old Turbine Hall module (MTE 28104), its fan items, and its K/T/P multipliers are
RETIRED; fan/coolant/propellant live in the new chain now. Migration: hall controllers
become museum pieces (recipe removed, tooltip pointing here) until deleted in a later
breaking release.

## Propellant ladder (worse -> best; stage factor P, all-8-timed)

| Propellant                | P (t) | Notes                              |
|---------------------------|-------|------------------------------------|
| Benzene                   | 1.25  | entry                              |
| High Octane Gasoline      | 1.5   |                                    |
| H8N4C2O4 Rocket Fuel      | 2.0   | "the best rocket fuel"             |
| Naquadah Fuel Lv 1        | 3.0   |                                    |
| Naquadah Fuel Lv 2        | 4.0   |                                    |
| Naquadah Fuel Lv 3        | 5.0   | knee of the curve                  |
| Naquadah Fuel Lv 4        | 5.5   | linear from here -                 |
| Naquadah Fuel Lv 5        | 6.0   | deliberately                       |
| Naquadah Fuel Lv 6 (UMV)  | 6.5   | "not worth it really"              |

## Coolant ladder (worse -> best; quality Q)

| Coolant                     | Q (t) |
|-----------------------------|-------|
| Water                       | 1     |
| Distilled Water             | 1.5   |
| IC2 Coolant                 | 4     |
| Super Coolant               | 12    |
| Liquid Cryotheum            | 40    |
| Tachyon Rich Temporal Fluid | 160   |

## Implementation phasing

- SD-1 (IMPLEMENTED 0.12.0): rod pillar cascade in the silo - 8-segment frames with
  fill-level block states, Rod Loading Ports above each pillar (4-slot TE buffer,
  hopper/AE2/right-click insert), bottom-burn per decay cycle (campaign/8), depleted
  rods to the output bus, bus fallback at 50%.
- SD-2 (IMPLEMENTED 0.13.0): the Spine as an optional piece of the main controller -
  3x3x8 Tungstensteel Turbine Casing duct two blocks past the silo's side wall, resting
  on the platform, centered on the tier's mid-row, 4 Turbine Housing sockets alternating
  with the open bore (right-click toggles CW/CCW). Swirl per the model above; interim
  payout until SD-3/4: hall input steam x speed x eff, exported steam x speed; residual
  swirl = |S|^2 x 5 heat/s while running (all-riding patterns need cooling towers).
  The +a corridor (a 31-42, c 20-26) - the strip to your RIGHT as you face the silo - is
  reserved for the duct + future spiral chamber;
  the +a dock marker moved {38,25} -> {38,30} (0.12.0 platforms: swap the steel marker
  at the old spot for concrete and set it at the new one, or reproject the floor).
- SD-3 (IMPLEMENTED 0.15.0): the Spiral as an optional checked piece at the duct's end
  (7x7x12 at platform a 43-49, tier-centered rows; ducts now run tier-length so they end
  flush at a=42). Interior: helix of concrete arcs stepping down the 5x5 ring around a
  3x3 downcomer. Chamber walls accept up to 8 Precision Injectors (MTE 28106, 16L ports
  that tick-stamp every litre), 1 Spine Sensor (28107, 2-tick redstone pulse per slug),
  plus Input/Maintenance hatches. Slug every 20t; window +/-2t (scored one tick after it
  closes, so timing never depends on tile-entity order; stale stamps from stops are
  discarded); exactly 1L per injector = boost, misses waste the litre + 50 heat each
  (capped 8/slug). Boost = 1 + (P-1)*n/8
  using the LOWEST propellant among timed injectors. Coolant drain = ceil(200*v^2)/Q
  per tick (v = spine speed x injector boost); ladder Water 1 / Distilled 1.5 / IC2 4 /
  Super Coolant 12 / Liquid Cryotheum 40 / Tachyon Rich Temporal Fluid 160 (verified:
  Materials.Time molten). Starved: +ceil(v^2)*3 heat/s until the rotors SEIZE - the
  Spine's housings are destroyed and the plant slams shut. Propellant fluids Lv 4-6
  added (GoodGenerator Mk IV-VI based, UHV/UEV/UIV mixers).
- SD-4 (IMPLEMENTED 0.15.0): the Large Turbine Rotor Assembly as the final optional
  piece - a 7x5x18 gallery running BACK from the Spiral down the +a strip (your RIGHT as
  you face the silo), Dynamo +
  Maintenance hatches in its walls, EIGHT rotor stage sockets (Turbine Housings, spin
  agnostic) along the centerline. EU/t = L/t x speed x eff x (1 + 0.75 x stages) / 2,
  clamped to summed DYNAMO CAPACITY (GT detonates the multiblock past it) and < 2^31.
  Fully optimized T5 (all-naq cascade, all-riding spine, 8 timed Lv-6 injectors, 8
  stages) computes ~1.045B EU/t - on the settled D-01 ~1B ceiling, above the retired
  hall's 991M (t; the 0.75/stage coefficient is the tuning knob).
  Takes precedence over a docked Turbine Hall; hall + fan RECIPES removed (existing
  ones keep working as museum pieces until a later breaking release).
- Each phase: recon vs sources -> implement -> review swarm -> smoke test -> tag.

## TURBINE CONTROL ruling (2026-08-19, review feedback after the first 0.16.0 session)

Nine-point ruling; phases TC-1..TC-5. All constants (t).

- **TC-1 Socketed hatches** (pt 4, IMPLEMENTED 0.17.0): blanket wall hatch-adders are gone - every hatch
  gets a DEDICATED socket cell, so holograms show the exact count at the exact spot.
  Silo front wall around the main station: 4 energy, 1 maintenance, 2 input hatch,
  1 output hatch, 1 input bus, 1 output bus. Spiral: 8 injector sockets following the
  helix, 1 sensor (top front), 1 input hatch, 1 maintenance. Turbine gen: 4 dynamo
  sockets in the output manifold's back face, 1 maintenance, 1 Turbine Control Block.
- **TC-2 Control blocks** (pts 2, 3, IMPLEMENTED 0.17.0): SILO CONTROL BLOCK (MTE 28108)
  socketed in the silo face above the main station - its panel is the reactor readout
  (tier, cascade, heat, decay, radiation). TURBINE CONTROL BLOCK (MTE 28109) socketed
  in the turbine gen's front wall center - its panel is the turbine readout (RPM,
  cycles/min, stages, rotor health, propellant, EU) and it accepts the REDSTONE cycle
  signal (rising edge, any side). Implementation ruling: a hatch cannot cleanly host
  the main controller's MUI2 screen in .52, so both panels are RIGHT-CLICK CHAT
  READOUTS - explicit, testable, no GUI risk; the main controller GUI keeps the full
  terminal. Both blocks hold a parent back-reference set by the structure walk and
  cleared on every reset/failed probe.
- **TC-3 RPM model** (pt 2, IMPLEMENTED 0.17.0): cycles are redstone-triggered (rising
  edge on the Turbine Control Block). Each accepted cycle: consumes one slug of
  buffered steam (20 x L/t, from a standing buffer fed at L/t, cap 100 x L/t), opens
  the +/-2t injector window (scored one tick after it closes, the SD-3 deferral
  pattern), burns the timed litres, and adds RPM:
      gain = 176 x P x (timed / 8)                     (implemented; ruling sketch 60)
      RPM decays 0.5% per tick toward 0; redline 12,000 (hard cap)
  Equilibrium: at the 1 Hz steam budget the peak fixed point is gain/(1-0.995^20)
  ~= 10.486 x gain - Lv 6 all-timed lands at ~11,996 ~= redline (hence 176; the
  sketched 60 topped out at ~4,089). Lv 3 ~= 9,227, Lv 1 naq ~= 5,536, Benzene ~= 2,307.
  Pulsing faster than steam production = missed cycles (propellant still burned, no
  gain, no echo). FAN RPM IS THE CENTRAL VARIABLE:
      EU/t     = L/t x speed x eff x (RPM / 1000) x avgRotorFactor
                 (avgRotorFactor averages ALL 8 sockets, so it carries stages/8)
      coolant  = ceil(200 x (RPM/1000)^2 / Q) per tick; starved -> heat -> SEIZE
                 (a seizure also slams RPM to zero)
      no redstone clock = RPM decays to zero = no output. The Sensor hatch echoes
      each ACCEPTED cycle as its confirmation pulse. The old x(1+(P-1)n/8) flow boost
      is retired - injectors only feed cycles now.
  Ceiling note: fully optimized T5 at redline (all-naq cascade, all-riding spine,
  fresh rotors) computes ~551M EU/t - 0.53x the retired SD-4 formula's ~1.045B,
  because the x6.5 chamber flow boost folded into the x12 RPM factor. The (RPM/1000)
  normalization (or RPM_GAIN) is the tuning knob if the D-01 ~1B ceiling is wanted
  back.
- **TC-4 Rotor items** (pts 1, 6, IMPLEMENTED 0.17.0): the turbine gen's output end
  widens into a 7x7x3 OUTPUT MANIFOLD (7 tall, hollow; 4 Dynamo sockets mid-height on
  its back face); stage sockets are odd gallery rows 1-13 plus one in the manifold
  mouth (row 15) = 8. Every Turbine Housing is now a tile entity with ONE rotor seat:
  right-click with a TURBINE ROTOR (10 durability, wear ON the item) to seat it,
  sneak-right-click empty-handed to pull it, breaking the housing drops it. Slow
  random damage - each second per stage, chance = (RPM/redline)/600 (one point per
  ~10 min at full tilt, shatter per ~100 min) - and like a plane turbine every chip
  hurts: stage factor 1.0 minus 5% per damage point; at 10 the rotor SHATTERS (item
  destroyed, stage dead until replaced).
- **TC-5 LASER OUTPUT** (IMPLEMENTED 0.19.0): the manifold's back face gains 4 LASER
  SOURCE sockets ('L' at y 5, a 1,2,4,5 - two rows above the dynamo row, same columns,
  exposed at every tier). They accept `HatchElement.LaserSource` =
  `tectech.thing.metaTileEntity.hatch.MTEHatchDynamoTunnel`, which
  `addLaserSourceToMachineList` files into `mExoticDynamoHatches`. `/btsbuild` auto-fits
  the ZPM 4,096 A/t Laser Source Hatch (`CustomItemList.eM_dynamoTunnel3_ZPM`, MTE 15252,
  V[7] = 131,072 x 4,096 = 536,870,912 EU/t each).
  - **The ceiling is now DYNAMO + LASER.** `addEnergyOutputMultipleDynamos` always sized
    and fed both lists; only the controller's own pre-clamp summed `mDynamoHatches`
    alone, which pinned the whole plant at 4 x 1 A x 8,192 V = **32,768 EU/t** - one amp
    of LuV - and threw ~99.997% of a tier-5 campaign away. The clamp now sums both and
    is still applied BEFORE emitting (injecting more than capacity explodes the multi)
    and still capped at 2^31-1 (`injected` is an int inside GT).
  - **T5 arithmetic** (201 naq pillars, full throttle, all-riding spine, redline RPM,
    8 fresh rotors): steam 201 x 16,384 x 9 = 29,638,656 L/t; spine x1.553 ->
    46,028,832 L/t; `46,028,832 x 12,000,000 / 500,000 x 1.000` = **1,104,691,968 EU/t**
    demand. Ceilings: dynamos alone 32,768 (0.003% of demand); +1 laser 536,903,680
    (48.6%); +2 lasers 1,073,774,592 (97.2%); **+3 lasers 1,610,645,504 - first setting
    that clears the peak**; +4 lasers 2,147,516,416 -> the int cap 2,147,483,647 (1.94x
    headroom). The ~1.05B sawtooth average is covered by 2.
  - **Player wiring** (must be in the questbook/wiki): PAINT the source hatch, aim its
    front down a run of Laser Pipes (mirrors allowed) at a same-colour Laser Target
    Hatch whose own front faces back; **voltage tiers must match exactly - a lower-tier
    target is DESTROYED**. An unpainted source silently buffers everything it is handed,
    so the GUI and both control blocks print `fitted / painted` counts.
- **TC-7 SAFETY INTERLOCK + full instrumentation** (2026-08-19 ruling, IMPLEMENTED 0.19.0):
  the plant must never spin up into a seizure because its consumables are not plumbed yet,
  and every panel must show everything.
  - **The interlock.** A redstone (or bench-clock) cycle request is now only ACCEPTED when
    both hold: (a) the chamber can cool the RPM the cycle would reach - `projectedRpmMilli`
    assumes the cycle goes perfectly (all fitted injectors timed, best propellant loaded) and
    the ladder is probed with a SIMULATED drain, so nothing is consumed by the test; (b) the
    injectors are not DRY (no propellant anywhere for 100t - longer than a cycle span, so the
    intended "empty port, one litre inside the window" play is never mistaken for dry). A
    refused cycle drinks no steam, burns no propellant, adds no heat, opens no window (and
    discards stamps so the next real cycle cannot score them as misses) - it just holds the
    RPM and states the reason. A slugless cycle is still MISSED per TC-3.
  - **No seizure from an unplumbed plant.** Losing coolant AT SPEED trips the fan into a
    protective spin-down (RPM x0.95/t on top of the normal decay) instead of cooking it.
    Friction heat at speed still accrues, so `seize()` remains reachable for a chamber that
    was already near SCRAM - but a plant that never got coolant never gained RPM, so it has
    nothing to seize.
  - **The coolant carries the swirl.** The chamber's per-tick load is now
    `200 x (RPM/1000)^2 + |S|^2 x 5 / 20` - SD-3's `q = k2 x V^2 + q_spine`, both terms
    paid in coolant. While the chamber IS cooled the residual-swirl turbulence no longer
    adds heat (the loop carries it away); starved or with no chamber at all it bites as the
    old `|S|^2 x 5` per second. All-riding spines therefore cost coolant rather than
    inevitably cooking the core.
  - **One panel everywhere.** `MTEReactorCoreMk1.statusReport()` builds the whole instrument
    panel from shared static formatters; the controller GUI, BOTH control blocks and the
    scanner all print the same grouped, colour-coded lines: REACTOR (tier, rods, modules /
    cascade live+total pillars, fuel, steam L/t / heat+decay+radiation), STEAM CHAIN (spine
    pattern, housings, speed, eff, swirl / injectors fitted+timed, propellant in use and its
    P / coolant type, drain, STARVED), TURBINE (RPM vs redline, cycles/min, steam buffer in
    slugs, bench-clock flag / rotor stages, average factor, per-stage wear / the HOLD reason),
    OUTPUT (EU/t emitted, clamp, dynamo+laser ceiling, fitted vs painted lasers) and a DEBUG
    line whenever a test flag is on.
  - **Test aid: infinite supplies.** A NBT-persisted `debugInfiniteSupplies` flag refills
    feedwater (silo face-panel hatches), the best coolant in the pack (the Spiral's own
    hatch, recorded by its structure walk) and the best propellant (all 8 injectors) every
    tick, and gives each ACCEPTED cycle a perfectly timed 1L shot per injector. The reserve
    is set without a timing stamp - going through `fill()` would stamp every refilled litre
    and the scorer would read it as a dumped bucket (8 wasted litres + heat spike per cycle).
    `/btsbuild` turns it on with the bench clock; `/btssupply [on|off]` toggles it.
## AUTOMATION ruling (2026-08-19, in-game session) — phases A1..A4

The player's words: *"it doesnt have a good automation flow, we need insert sections"*,
*"make the controllers for all multiblocks attached automatically be placed where needed
or at least show up in the hologram design"*. The answers they picked: **ONE automation
bay** grouping every insert/extract point with rods distributed internally to all pillars,
and **absorb the module buildings into the main structure — no separate module
controllers at all**.

⚠ **A1 was later REVERSED by the same player in the same session** once they saw the bay
standing — see "POINT OF USE ruling" below. A2, A3 and A4 all stand; only the socket
LOCATIONS changed.

- **A1 THE AUTOMATION BAY** (IMPLEMENTED 0.20.0, then **REVERSED — see POINT OF USE
  below**). It was an 11 x 2 x 4 Clean Stainless frontage at platform a 20..30, c 13..14,
  y 0..3 carrying all eleven sockets in one wall. The player, having seen it built, ruled
  it out; the piece, its `bayCell()`/`BayHatchElement` code and the whole Clean Stainless
  bill are deleted. Kept for the record because the phase numbering below refers to it.
- **A2 INTERNAL DISTRIBUTION** (IMPLEMENTED 0.20.0) — this is what makes it automatable,
  and it SURVIVES the reversal intact; only the socket locations moved.
  - **RODS.** The controller pulls fuel rods out of the silo face's Rod Input Bus every
    decay cycle and tops the pillar reserves up itself, best grade first, lowest empty
    segment first. 45-201 Rod Loading Ports spread across a 17-wide octagon were the whole
    reason the flow was bad; the ports still work for hand-feeding but are **no longer
    REQUIRED** and the hologram no longer asks for one per pillar. Depleted rods leave
    through the Depleted Output Bus on the same course.
  - **COOLANT.** `chamberHatches` semantics are unchanged — a private list the chamber
    alone drains, so the reactor's feedwater can never answer "can the chamber be cooled?"
    — and it is sourced from the SPIRAL CHAMBER's own Coolant Input Hatch. Feedwater is
    likewise drawn from the silo's Feedwater hatch ONLY, through the private
    `feedwaterHatches` list, never a blanket `depleteInput()` that would drink the
    chamber's water-grade coolant out from under it.
  - **PROPELLANT — ⚠ RULING-ADJACENT CHANGE, flag this for a ruling.** The Spiral
    chamber's bulk Propellant Input Hatch feeds the eight injector reserves automatically,
    one litre each, set directly (never through `fill()`, which would stamp the refill as an
    injection and score it as a dumped bucket). **The timing ruling is preserved in
    substance:** a CYCLE still only happens on a redstone rising edge at the Turbine
    Control Block, the ±2t window still exists, and the number of injectors that count as
    TIMED is still decided per cycle — by how many of them the bulk line managed to stock
    before the edge arrived. `gain = 176 x P x timed/8` is untouched, and a line that only
    keeps 5 of 8 stocked runs at 5/8 gain.
    **What changed is who presses the eight buttons.** The player supplies bulk propellant
    on one line instead of hand-timing eight 1L inserts; on an accepted cycle the plant
    lands the perfectly timed shot. Hand injection still works exactly as ruled and still
    has to hit the window — an injector carrying a player's timing stamp is never touched
    by the auto-feed, so the two styles can never double-stamp one window (two litres in a
    window is still a miss, still +50 heat).
- **A3 ABSORB THE MODULES** (IMPLEMENTED 0.20.0). The Cooling Towers are OPTIONAL CHECKED
  PIECES of the main controller, detected like the Spine and the Spiral (FRONT tower
  a 0..16 c 0..16, BACK tower a 0..16 c 33..49, offsets 25/19/17 and 25/19/-16; both on
  the `-a` strip, your LEFT as you face the silo). Their effects are
  unchanged and apply while the piece stands: feedwater draw x0.5, passive heat decay x2,
  decay-heat demand x0.5, max 2. The dock-marker mechanic and `PlantModuleElement` are
  gone, the eight `K`/`M` marker cells are plain floor, and
  `MTEModuleCoolingTower` / `MTEModuleTurbineHall` / `MTEPlantModuleBase` are museum
  pieces: MTE ids still registered so existing worlds do not corrupt, recipes removed,
  tooltips saying they are retired and the buildings are part of the main plant.
- **A4 HOLOGRAM COMPLETENESS** (IMPLEMENTED 0.20.0). One projection is now literally the
  whole plant: platform -> silo -> spine -> spiral -> gallery -> FRONT tower -> BACK tower, in
  `construct()` and `survivalConstruct()` alike. No build-only "shell" leniency is left
  anywhere, and there is no second controller to place.

## POINT OF USE ruling (2026-08-19, same in-game session) — reverses A1

The player's words, having seen the bay standing: *"also the inputs shouldnt be on a wall,
the inputs need to go exactly where they are fed into in the logic flow"*. The newer
ruling wins. **The Automation Bay is deleted** and every socket moved onto the building
that consumes what enters through it, on the course where it enters:

| I/O | now lives on | why there |
| --- | --- | --- |
| Rod Input Bus / Depleted Output Bus | silo face, bottom FUEL course (y2) | the exact segment the cascade pushes into and burns out of |
| Feedwater Input Hatch / Output Hatch | silo face, drum course (y1), either side of the Silo Control Block | in and out of the same drum circuit, panel between them |
| Maintenance | silo face, pad level (y0), beside the main station | — |
| Coolant Input Hatch | Spiral chamber FRONT WALL, one course above the floor | the chamber is the thing that overheats |
| bulk Propellant Input Hatch | Spiral chamber FRONT WALL, same course | directly under the eight injectors the line stocks |
| **Energy Hatch ×4** | output manifold BACK FACE, y4 | at the generator, between the dynamo course and the laser course |
| Dynamo ×4 / Laser Source ×4 | output manifold BACK FACE, y3 / y5 | unchanged — already point of use |

Exact coordinates, facings and creative auto-fit ids: `docs/plant-geometry.md`, socket table.

**Register note (settled).** This ruling is **`D-20`** in `../PROPOSAL.md` — "Point-of-use
I/O + onboarding", settled 2026-08-19 — and every Java comment tag now points at it.
`D-19` keeps its own meaning, "Turbine Control (RPM + sockets + rotors)"; that work is
tagged `TC-1`..`TC-7` in the source and was never re-pointed.

Consequences that landed with it:
- `checkHasEnergyHatch(errors)` is **gone** — not conditional, removed. The plant never
  draws EU on any branch (`checkProcessing` sets `lEUt = 0` everywhere, `onRunningTick`
  emits through the dynamo and laser rows, `outputCeiling()` is dynamos + lasers, and
  nothing but `buildSummary()` even reads `mEnergyHatches`), so a missing Energy Hatch
  cannot change one number the plant produces. Making it fatal un-formed a plant that was
  functionally complete — and un-forming starts the 20-minute decay-heat obligation, which
  is a real cost for a hatch that does nothing. The empty sockets are still reported, by
  `energyHatchNote(...)` as a grey non-fatal line in the OUTPUT group.
- The public readout API lost every `Bay` name: `bayLine` -> `supplyLine`,
  `getBayRodsQueued` -> `getRodsQueued`, `getBayPropellantLitres` ->
  `getPropellantLitres`, `getBayCoolantLitres` -> `getChamberCoolantLitres`,
  `getBayFeedwaterLitres` -> `getFeedwaterLitres`, `isBayBuilt` removed (the GUI gates on
  `isSiloBuilt`), and `BayHatchElement` -> `machines/FluidHatchElement.java`.
- The panel's third line now leads `Supply · N rods queued -> N pillar gaps · propellant
  N L · coolant N L`, and its failure branch is `No Steam Generator silo - its front face
  is where the rod bus, feedwater and output sockets live`.

**Migration (revised — a legacy plant DOES need player action, and exactly one command).**
The relocation un-forms every plant built before it, and not by merely missing blocks: the old
face puts a hatch in every cell the new one wants wall and vice versa, so a standing legacy
silo fails all five tier probes and **no projection can repair it** — `construct()` /
`survivalConstruct()` measure the shell that stands and never re-tier or overwrite it. A bare
platform is a different case and is untouched: the eight retired steel dock markers are
accepted as-is.

The plant no longer fails quietly. `checkMachine` asks `standingSiloTier()` after all five
probes fail, re-walks that tier into the real error list (one positioned error, not a packet
flood) and the REACTOR group's first line becomes `siloAbsentLine(tier)`: *"Tier N Steam
Generator IS STANDING and does NOT pass its check"*, with the first wrong cell and its
coordinates in the STRUCTURE ERRORS panel above it
(`beyondthestars.structure.silo.standing`). `mMachine` goes false rather than pretending.

The fix is **`/btsmigrate`** (`CommandBTSMigrate`, permission 2), run standing beside the main
station. It reads the tier off the standing walls — never a remembered number, never the GUI
Build Tier — and rewrites the silo front row's bottom three courses, the only cells the two
layouts disagree about. Cells already correct are left alone (sockets are only re-pointed
outward); wrong ones drop their contents at the wall as items before the correct casing or
socket goes in, so every hatch and everything inside it comes back. It then re-forms and
reports tier, cells rewritten and machines handed back. **Shell, ribs, dome, fuel pillars and
the rods standing in them are never read and never written.** The four Energy Hatches the old
face carried belong on the output manifold back face (y4) now, and are optional there — they
are not a structure demand.

An existing plant still needs 2 Solid Steel Machine Casings per cooling tower added by hand
where the retired module controller stood (see plant-geometry.md, Cooling Towers, MIGRATION).
A 0.19.0 Maintenance Hatch left in a tower base ring may stay — the ring cell tolerates one —
but it is **never counted**: the plant's three maintenance sockets are the silo face, the
Spiral floor and the manifold, and only those.

## READABILITY ruling (2026-08-19, same in-game session) — phases B1..B2

The player's words: *"show the math in the recipe with the controller like how the waterline
or other controllers show the fuel and the math for the fuel"* and *"if someone was to pick up
the game they would see its easy and they would see how to do it easily"*. The answer they
picked: **NEI page AND a live GUI math breakdown** — both, not either.

- **B1 THE NEI PAGES** (IMPLEMENTED 0.20.0, `BTSDisplayRecipes`). Three DISPLAY-ONLY recipe
  maps, built the way GT builds `largeBoilerFakeFuels`:
  `beyondthestars.recipe.plantfuel` (one entry per rod grade: fuel multiplier, steam per live
  pillar, segment burn time, whole-silo totals at T1/T5, the depleted rod it drops, and the
  point of use it enters and leaves by — `In: silo face, rod course` /
  `Out: bus beside it, same course`), `…plantpropellant` (one entry per ladder rung:
  stage factor P, the 8 L a cycle burns, `gain = 176 x P x timed/8`, the RPM that gain
  HOLDS at the sustainable one cycle per second, that a missed cycle burns 8 L anyway, and
  the rung's PROVENANCE — `Mixed from Naquadah Fuel` on rungs 3-8, which is the one line
  that stops a player piping GoodGenerator's Naquadah Fuel into an injector) and
  `…plantcoolant` (one entry per rung: quality Q, the chamber-load formula, and the
  litres/tick it drinks at the redline and at half the redline, which is where the
  quadratic becomes obvious). Both fluid pages say `In: Spiral chamber, front wall`.
  - **A missing optional dependency costs one page, never the mod.** Forge 1.7.10's
    `FluidStack(Fluid,int)` THROWS from the constructor on a null or unregistered fluid, so
    every hand-wrapped rung (propellant 2-8, coolant 1) is vetted by a
    `propellantRungAvailable` / `coolantRungAvailable` switch that mirrors the ladder case
    for case BEFORE the ladder is asked for it; each per-entry builder chain also sits
    inside a `catch (RuntimeException)` backstop, and skips are logged, never silent.
  - **It is display-only three times over, deliberately.** (1) The controller's
    `getRecipeMap()` is still `null` — `checkProcessing()` is hand-written and never calls
    `findRecipe`; what the controller overrides is `getAvailableRecipeMaps()`, which ONLY
    NEI's catalyst index reads (`NEIGTConfig#generateRecipeCatalystIndex`). (2) The maps are
    therefore never handed to the hatches as `mRecipeMap`, so nothing starts filtering.
    (3) Every entry is `.fake()`, which GT documents as "findRecipe won't find fake Recipes".
  - **The page cannot drift.** Every number is read out of `MTEReactorCoreMk1`'s own runtime
    tables through public accessors (`rodMultiplier`, `propellantOfRung` + `propellantMilli`,
    `coolantOfRung` + `coolantQMilli`, `chamberLoadFor`, `coolantDrainFor`, `rpmGain`,
    `rpmDecayPermille`, `decayCycleTicks`), so retuning a constant moves the page with the plant.
  - NEI's description column is 170 px and does not wrap, so the lines are hard-kept to ~28
    characters — the same reason GT hard-wraps its own `GT5U.nei.*` strings.
- **B2 THE GUI MATH BREAKDOWN** (IMPLEMENTED 0.20.0). A `MATH` group at the foot of the
  instrument panel — controller GUI, BOTH control blocks and the scanner, via the same shared
  static formatters — walking the CURRENT EU/t in four numbered steps with every factor
  carrying the value it has right now:
  1. uniform load: `N pillars x<fuel> x16,384 = L/t (rods 24/24)`, or at a throttle
     `... = <base> · rods N/24 -> L/t`
     MIXED load (R7): `N pillars avg x<avg> fuel (<census>) x16,384 = L/t`, where the
     census is the grade breakdown, e.g. `45 pillars avg x6.69 fuel (32x9 + 13x1)`. The
     printed base is the reactor's own per-pillar SUM (`fuelBaseSteam`), not
     `live x best grade`, so the arithmetic on the line always closes.
  2. `x<spine speed> x<spine efficiency> = L/t through the chain`
  3. `x RPM N/500 (xM) x rotors xR = EU/t demand`
  4. `under/CLAMPED at ceiling C -> EU/t out`
  then a fifth line, **`Weakest term:`**, which ranks pillars-filled, fuel grade (scored on
  the AVERAGE burning grade, not the best rod in the silo), throttle and spine against each
  term's own ceiling, names the one costing the most, and gives the fix. That line is the
  "a newcomer can see what to do" half of the ruling.
  - **R9: no generator, no generator terms.** On a plant with no rotor gallery the group
    re-titles itself `MATH · how that steam figure is derived`, steps 3 and 4 collapse into
    one export line (`no Turbine Generator - this is a STEAM EXPORT plant: x<speed> spine
    speed = N L/t out of the silo's Output Hatch`), and RPM, rotors and the output ceiling
    stop being scored at all — they were telling a healthy export plant to seat rotors it
    has nowhere to put. The spine is then scored on SPEED alone, because export is paid in
    speed. The gate is `galleryBuilt`, the same condition the OUTPUT group's ceiling and
    laser lines use. A gallery with unseated rotors is deliberately NOT export mode.
  - **The RPM divisor is 500, not 1,000.** TC-3 sketched `(RPM/1000)`; the implementation has
    always used `rpmMilli / 500_000` (= RPM/500, x24 at the redline) — that is the back-solve
    that puts a fully optimized T5 on the D-01 ~1B ceiling instead of half of it, and it is
    what the T5 arithmetic in TC-5 computes with. The panels, the NEI page and the controller
    tooltip now all print `RPM/500`, and the one arithmetic lives in
    `MTEReactorCoreMk1.euDemandOf` / `steamAfterSpine`, which the generating tick itself calls.

- **TC-6 Book + wiki** (pts 5, 7, 8): entry quest with item requirements + achievement
  placed INSIDE the GTNH questbook's power-progression chapter; a BLUEPRINTS chapter
  listing every structure's items as optional quests (controllers = the required
  ones); a local static-HTML wiki under addon/wiki/ until a hosted one exists.
- **Standing rule** (pt 9): every build/redesign ships with a browser HTML mockup.

## ONBOARDING ruling (2026-08-19, same in-game session) — phase C

The player's words: *"the quest has to look a lot nicer"* and *"if someone was to pick up the
game they would see its easy and they would see how to do it easily"*. The answer they picked:
**all of it** — visual polish, smaller guided steps, item requirements plus rewards, and
diagrams.

- **C1 VISUAL POLISH** (IMPLEMENTED 0.20.0). The chapter is written in the pack's own house
  style: section-sign colour codes, `[note]`/`[warn]` blocks, short paragraphs and a
  `DO THIS` / `DONE WHEN` pair in every step. Every icon is a real registry id that means
  something (the retired module controllers, MTE 28104/28105, are gone from the book — they
  have no recipe any more — and the old "Spine" icon `gt.blockcasings:12` did not exist;
  Tungstensteel Turbine Casing is `gt.blockcasings4:12`).
- **C2 SMALLER GUIDED STEPS** (IMPLEMENTED 0.20.0). The chapter is now a LINEAR ten-step
  beginner path: craft the controller -> break ground -> one projection -> **plumb the
  points of use** -> load the rods -> first steam -> seat the rotors -> wire the clock ->
  first EU -> read the panel. Rows 1 and 2 run left-to-right then right-to-left, so the
  prerequisite chain draws as one continuous line. Everything that used to be step one's
  wall of text is spread across those ten, and the deep dives (laser output, spin doctrine,
  the two ladders, the safety sheet) hang off step 10 instead of blocking it.
- **C3 REQUIREMENTS + REWARDS** (IMPLEMENTED 0.20.0). Every step gates on a `bq_standard:retrieval`
  of the items that step actually needs — step 4's gate IS the silo face's and the chamber
  wall's bill of materials — and pays a `bq_standard:item` reward that is usually the next
  step's tool (step 1 hands over the Hologram Projector, step 6 hands over the 12 Turbine
  Housings step 7 must place, step 8 hands over the 4 Energy Hatches step 9 must fit).
  **`consume` and `autoConsume` are 0 everywhere**: a step that gated on the controller and
  then ate it would brick the plant the player just built. The generator asserts that every
  icon/task/reward stack has a real id and a count of at least one, and that no description
  shipped an un-interpolated bill placeholder or a count of zero.
  - **What `consume = 0` actually means, and the trap it set (C2, fixed in the C-round).**
    In BetterQuesting 3.8.70-GTNH, `TaskRetrieval$Detector`'s constructor takes the
    `!consume` path and **zeroes every required-item entry whose progress is below its
    required count**, then re-counts from the live inventory
    (`IntStream.range(0, InventoryPlayer.getSizeInventory())` → `getStackInSlot`, so worn
    ARMOUR counts too). An entry only latches once it has been seen at its FULL count at one
    instant; `TaskRetrieval.detect` then returns immediately for a task already complete, so
    a finished step never comes undone. The consequence is that **partial progress never
    accumulates**: four Precision Injectors now and four later reads 0/8 for ever, and a
    step whose body says "set these into the wall" reads 0 while the player does exactly
    what it asked. Step 8 was the worst case — it demanded 8 injectors, the path paid out
    only 2 + 2, and its own body told you to install them. Two rules now hold across the
    whole chapter: **(1)** the rewards must add up to the count the next gate asks for
    (injectors are 4 + 4 = 8 at steps 4 and 7); **(2)** a step that installs its parts says
    HOLD → CLAIM → INSTALL in its `DONE WHEN`, and the world-state check that used to live
    there moves into its own `CHECK YOUR WORK` block. A gate satisfiable only by wrenching
    finished machinery back out is a **checkbox** instead — which is what "Spin Doctrine"
    (4 spine housings, in the wall since step 7) and "The Propellant Ladder" (8 injectors,
    in the chamber since step 8) now are.
  - **The path must be COMPLETABLE, not just readable** (review findings Q1/Q2). The
    projector places no Turbine Housing at all (`PlaceResult.SKIP` on both the spine `'H'`
    and the gallery `'9'`) and no hatch, injector, sensor or control block in survival
    (the hatch adder rejects without the HATCH channel and the chain falls through to plain
    casing). Step 3 now states that explicitly; step 7 gates on 12 housings and step 6 pays
    for them; step 8 gates on the 8 Precision Injectors and the Spine Sensor and says where
    each one goes, including that two injector sockets stand +7 and +8 above the platform.
  - **The reactor and turbine draw no EU** (review finding Q3). `checkProcessing()` sets
    `lEUt = 0` in all three success branches and nothing in the steam chain calls
    `drainEnergyInput` or `decreaseStoredEnergyUnits`. The old step-6 line "a reactor this
    size still draws to run" and the matching diagram legend were false and are gone; the
    ruling was to correct the TEXT, not to add a parasitic load to the steam chain (that
    would be a new gameplay ruling and my call). **Superseded in the C-round:** the
    four Energy Hatches are not a structure requirement either —
    `checkHasEnergyHatch(errors)` is removed outright and the OUTPUT group carries a grey
    `energyHatchNote(...)` counting the empty sockets. **Revised again in 0.21.0 (`C5`):**
    "nothing in the plant draws EU" is no longer true and must not be quoted as an
    invariant. `moduleSupplied()` calls `drainEnergyInput()` once per tick for the
    Extraction Derrick (`TierEU.RECIPE_EV`, 1,920 EU/t) and the Enrichment Annex
    (`TierEU.RECIPE_HV`, 480 EU/t). The sockets stay optional because the reactor does not
    need them; they are not furniture, and the note turns red when a process module stands
    with none fitted.
  - **Bills are DERIVED** (review finding Q5). `questbook/gen-bom.ps1` re-implements every
    shape function, tallies each cell through the element table, cross-checks itself against
    `COLUMN_CAP` and the octagon formula, and refuses to run if a constant or socket-placing
    line it was ported from has moved in the Java. `gen-questbook.ps1` dot-sources it, so
    the five Blueprint quests can never drift by hand again. **T1:** and a script run of
    `gen-bom.ps1` re-renders `BTSBom.java` and runs `gen-questbook.ps1` in the same step —
    there is no way to regenerate one consumer without the other, and a run that throws
    rolls `BTSBom.java` back to stale, the one state every gate here refuses, so a broken
    build cannot come to rest quietly.
- **C4 DIAGRAMS — images ARE possible in this pack** (IMPLEMENTED 0.20.0). BetterQuesting
  3.8.70-GTNH parses `[img height=N]domain:path.png[/img]` inside a quest description
  (`GuiQuest.img`, split by `addQuestDescPanels`, drawn by `addQuestDescImageSegment` at the
  description canvas's width with the aspect maintained; a texture that fails to load
  degrades to a 1x1 rect via `TextureSizeHelper.getDimension`'s catch, so it cannot crash the
  book). Three diagrams are drawn by `questbook/gen-diagrams.ps1` and embedded:
  `plant-overview.png` (step 3), `point-of-use.png` (step 4 — the silo face and the Spiral
  chamber's front wall, one panel each) and `manifold-face.png` (step 9 + the turbine
  blueprint, now carrying the Energy Hatch course between the dynamos and the lasers). The
  retired `bay-sockets.png` is DELETED by the generator on every run so a stale texture can
  never contradict the book. They ship in the OVERLAY, not the mod jar, under
  `config/betterquesting/resources/btsquests/` — BetterQuesting registers that folder as a
  resource pack (`betterquesting.misc.QuestResourcesFolder`), so the domain is the folder
  name and must be lowercase, and a diagram change needs no mod rebuild. Adding the folder
  for the first time needs one resource reload (F3+T) or a restart.
- **ID budget.** The chapter is **25 quests**, `190/48000-48019` and `190/48021-48025`.
  `190/48020` stays the entry quest grafted into *How to Generate Power*, because a world
  that already ran `/btsquests install` holds it under that id — so the chapter has a HOLE
  in the middle of it by design, and lows are handed out in `$quests` array order, which
  means a new quest may only ever be APPENDED (inserting one renumbers every quest after it
  and orphans installed progress).
  <br>The chapter is therefore described by a LIST and never by a range.
  `BQBridge.CHAPTER_QUEST_LOWS` declares all 25 lows and `/btsquests` status is
  PRESENCE-BASED — complete means every id in that array is in the world's database. It used
  to be a `FIRST..LAST` pair reading `48000..48019`, and when `48021..48025` were appended
  the pair was not widened, so `status` counted 20 ids out of a 25-quest chapter, found all
  20 and reported "Everything is loaded" with five blueprint quests missing from the book.
  There is no `BQBridge.LAST_QUEST_LOW` any more. `gen-questbook.ps1` reads
  `CHAPTER_QUEST_LOWS` back out of `BQBridge.java` and THROWS if it does not match the ids it
  just handed out (and throws again if the array contains `48020`, which is checked
  separately as `ENTRY_QUEST_UUID`), so a quest cannot be added to the book without the
  status command learning about it. The entry quest's FILE STEM is pinned to `UnitOne` so the
  file-merge into the pack folder overwrites instead of duplicating the id.

## Test commands (creative / debug)

The hologram projector previews one piece at a time and is height-capped, so it cannot
raise a whole plant. Use the commands instead (op level 2):

- `/btsbuild [1-5|all] [uranium|mox|thorium|naq] [force]` - builds a COMPLETE RUNNING plant
  where you stand, facing the way you face: platform, silo, Spine, Spiral,
  Turbine Generator and both Cooling Towers (checked pieces now, not module buildings),
  every hatch and control block fitted, rotors seated, rod ports stocked, bench clock ON
  and INFINITE SUPPLIES ON (feedwater + best coolant + best propellant, topped up every
  tick). Defaults: tier 3, naq rods. Footprint: 32 blocks behind the controller, 17 in
  front, 25 to each side. A commanded plant climbs to the redline and stays there -
  nothing to plumb, nothing to blow up.
  - `all` raises ALL FIVE tiers side by side in one command, on a 60-block pitch (the 50
    platform + a 10-block walking corridor) running to your RIGHT, so every silo size can
    be compared without them ever touching. It reports where each tier's main station
    landed and whether that tier FORMED, with a census of what its walk found (cooling towers,
    silo tier + columns, spine housings, spiral injectors, gallery stages, dynamos + lasers).
    It generates a lot of chunks - expect a pause.
  - It refuses to build if more than 15% of the sampled footprint is already solid (a
    50x50 platform overwrites everything under it). Air, water, lava and grass count as
    empty. Append `force` to build anyway.
  - The height check is per tier, not a flat number: a plant needs 20/25/30/35/40 blocks
    of headroom at tiers 1-5 (the silo is `2*edge + edge/2 - 2` tall; the 19-high tower
    shells only matter at tier 1). `all` therefore needs y <= 215.
  - Per-tier geometry that has to hold at every tier, and does: the duct is `16 - edge/2`
    long (12/11/10/9/8) and always starts one block past the silo's +a rib and ends at
    a=42, so its four housings at offsets 1,3,5,7 fit even the 8-long tier-5 duct; the
    Spiral starts at a=43 with `dOff = 3 - edge/2` and the gallery one row behind it with
    `dOff = -4 - edge/2`, which puts the tier-5 manifold at c=46, still on the platform;
    and the tier-1 octagon's front row is EXACTLY 5 flat cells wide, which is exactly what
    the socket panel needs - it fits with nothing to spare.
- `/btsclock [on|off]` - toggles the bench clock on the nearest main station. ON pulses
  one turbine cycle a second with no wiring (test aid); OFF restores the ruled mechanic,
  where only redstone edges at the Turbine Control Block start cycles.
- `/btssupply [on|off]` - toggles infinite supplies on the nearest main station (mirrors
  `/btsclock`). ON waives the silo's feedwater and the chamber's coolant draws in code,
  refills all eight
  injectors every tick and lands a perfectly timed 1L per injector on every accepted cycle
  (it takes the injectors over from the A2 auto-feed entirely, so the two can never
  double-stamp a window);
  OFF hands the plant back to real plumbing. With it off, missing supplies only hold the
  RPM down (the panels say exactly why) - they never seize the turbine.
- `/btskit [tier]` - hands over the controller, blocks, rods, rotors and hatches to
  build by hand. It no longer hands out module controllers - there are none.
- `/btsquests [status|install]` - see the Questbook section below.

Creative socket auto-fitting: in creative placement (command or projector) every socket
cell installs its real hatch instead of a blank casing, so a built plant is immediately
functional. Cells already holding a GT machine are never overwritten, so rebuilding over
a configured plant is safe. Survival autobuild is unchanged - players supply hatches.

Socket FACING is declared, never guessed (see docs/plant-geometry.md "SOCKET FACING").
Every socket has air on both sides, so scanning for the first air neighbour turned one
of any two opposite walls inward - and an inward-facing hatch is pixel-identical to the
casing it was textured with (`MTEHatch.getTexture` only paints the overlay on
`side == frontFacing`), which read as "the control blocks were never placed". The
decisive check on a suspect casing is still to RIGHT-CLICK it: a control block answers.

## Questbook

The "Beyond the Stars" quest line overlay lives in `questbook/` (GTNH 2.9.0 BQ split
format): a 20-quest chapter that is a ten-step guided path, five mastery quests and five
bills of materials (see the ONBOARDING ruling above). Merge per `questbook/README.md` —
note that the merge now also copies `questbook/resources/btsquests/` into the instance's
`config/betterquesting/resources/`, which is where the three embedded diagrams live.
Regenerate with **`questbook/gen-bom.ps1`** — the one build command, whether you edited a
quest definition or the geometry — and `questbook/gen-diagrams.ps1` after editing a diagram.
`gen-bom.ps1` derives the five Blueprint bills straight from the structure shape code,
re-renders `BTSBom.java` (what `/btskit` prints), then runs `gen-questbook.ps1` itself so
`DefaultQuests/` is rewritten from the same tally, and prints the table. **After any
geometry change in `MTEReactorCoreMk1`, re-run `gen-bom.ps1`, then `gradlew spotlessApply
compileJava`** — if the shapes moved under the port it throws and names the constant that
changed. Do NOT run `gen-questbook.ps1` as the build step: it cannot write `BTSBom.java`,
so it throws once the geometry has moved, and running it alone was how the book and
`/btskit` used to end up quoting different numbers (review finding T1).

**An existing world will not show the quests, and that is not a bug in the files.**
BetterQuesting reads `config/betterquesting/DefaultQuests/` exactly once, when a world is
created; after that the world owns `saves/<world>/betterquesting/QuestDatabase.json` and
never reads the defaults again. Merging the overlay therefore fixes NEW worlds only.

- `/btsquests` (op) - prints BetterQuesting's version, where it is reading DefaultQuests
  from, and whether our chapter (190/47990), our 25 quests (190/48000-48019 and
  190/48021-48025) and the entry quest "Unit One" (190/48020) are in THIS world's live
  database. If they are missing it explains why and gives both fixes.
- `/btsquests install` (op) - the fix. It reads only the files carrying our high word out
  of the instance's `DefaultQuests` and adds them to the live `QuestDatabase` /
  `QuestLineDatabase` singletons, snapshotting player progress with
  `writeProgressToNBT` before the write and restoring it with `readProgressFromNBT`
  after - the same trick BetterQuesting's own `default load` uses - then broadcasts
  `NetBulkSync.sendReset` so open books redraw and calls `SaveLoadHandler.markDirty()` so
  the world persists it. No pack quest is read, written or reordered; the only change to a
  pack chapter is the extra `Unit One` icon in *How to Generate Power*. Idempotent.
- The API question, precisely: BetterQuesting's public API DOES hand out the live databases
  (`QuestingAPI.getAPI(ApiReference.QUEST_DB)` / `LINE_DB` return the very objects the world
  is running on, and `/btsquests` goes through that accessor), but it exposes NO operation
  for "load this quest line out of DefaultQuests into the running world" - the only shipped
  thing close to it is the all-or-nothing `/bq_admin default load`. `/btsquests install` is
  that missing operation, composed from the databases' own public methods and reached by
  reflection, so the mod keeps no compile-time or runtime dependency on BetterQuesting (see
  `BQBridge.java`). Without BetterQuesting installed, `/btsquests` says so and stops.
- The vanilla fallback is `/bq_admin default load`, which reloads the WHOLE DefaultQuests
  folder over the world's database. On BQ 3.8.x-GTNH that command DOES preserve quest
  progress (verified in `QuestCommandDefaults.load`: it snapshots progress, clears and
  reloads the database, then writes progress back), but any quest edited in-world reverts
  and progress on quests the defaults no longer contain is dropped. On older BQ builds it
  is a straight overwrite. Back the world up first.
