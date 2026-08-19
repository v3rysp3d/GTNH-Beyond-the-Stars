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

- INSERT AT TOP: rods go into the pillar's top segment (rod loading port in the upper
  plenum / automation-friendly). A rod falls to the LOWEST empty segment - the pillar
  behaves like a gravity column.
- BURN AT BOTTOM: only the bottom segment burns. One decay cycle consumes it into a
  depleted rod, which drops into the base extraction buffer (pulled via output bus /
  rod drain at shield-pad level). Everything above shifts down one.
- OUTPUT: a column is LIVE while its bottom segment holds a burning rod. Steam per tick
  scales with live columns exactly as today (C x 16,384 L/t basis x fuel mult of the
  burning rod x throttle N/24).
- THE AUTOMATION CHALLENGE: keep 45-201 pillars topped up and their bases drained.
  Partial pillars = partial plant output; a starved pillar simply goes dark.
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
  The east corridor (a 31-42, c 20-26) is reserved for the duct + future spiral chamber;
  the east dock marker moved {38,25} -> {38,30} (0.12.0 platforms: swap the steel marker
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
  piece - a 7x5x18 gallery running south from the Spiral down the east strip, Dynamo +
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
- **TC-6 Book + wiki** (pts 5, 7, 8): entry quest with item requirements + achievement
  placed INSIDE the GTNH questbook's power-progression chapter; a BLUEPRINTS chapter
  listing every structure's items as optional quests (controllers = the required
  ones); a local static-HTML wiki under addon/wiki/ until a hosted one exists.
- **Standing rule** (pt 9): every build/redesign ships with a browser HTML mockup.

## Test commands (creative / debug)

The hologram projector previews one piece at a time and is height-capped, so it cannot
raise a whole plant. Use the commands instead (op level 2):

- `/btsbuild [1-5|all] [uranium|mox|thorium|naq] [force]` - builds a COMPLETE RUNNING plant
  where you stand, facing the way you face: platform, silo, Spine, Spiral, Turbine
  Generator and both tower shells, every hatch and control block fitted, rotors seated,
  rod ports stocked, bench clock ON and INFINITE SUPPLIES ON (feedwater + best coolant +
  best propellant, topped up every tick). Defaults: tier 3, naq rods. Footprint: 32 blocks
  behind the controller, 17 in front, 25 to each side. A commanded plant climbs to the
  redline and stays there - nothing to plumb, nothing to blow up.
  - `all` raises ALL FIVE tiers side by side in one command, on a 60-block pitch (the 50
    platform + a 10-block walking corridor) running to your RIGHT, so every silo size can
    be compared without them ever touching. It reports where each tier's main station
    landed and whether that tier FORMED, with a census of what its walk found (silo tier +
    columns, spine housings, spiral injectors, gallery stages, dynamos + laser sources).
    It generates a lot of chunks - expect a pause.
  - It refuses to build if more than 15% of the sampled footprint is already solid (a
    50x50 platform overwrites everything under it). Air, water, lava and grass count as
    empty. Append `force` to build anyway.
  - The height check is per tier, not a flat number: a plant needs 20/25/30/35/40 blocks
    of headroom at tiers 1-5 (the silo is `2*edge + edge/2 - 2` tall; the 19-high tower
    shells only matter at tier 1). `all` therefore needs y <= 215.
  - Per-tier geometry that has to hold at every tier, and does: the duct is `16 - edge/2`
    long (12/11/10/9/8) and always starts one block past the silo's east rib and ends at
    a=42, so its four housings at offsets 1,3,5,7 fit even the 8-long tier-5 duct; the
    Spiral starts at a=43 with `dOff = 3 - edge/2` and the gallery one row behind it with
    `dOff = -4 - edge/2`, which puts the tier-5 manifold at c=46, still on the platform;
    and the tier-1 octagon's front row is EXACTLY 5 flat cells wide, which is exactly what
    the socket panel needs - it fits with nothing to spare.
- `/btsclock [on|off]` - toggles the bench clock on the nearest main station. ON pulses
  one turbine cycle a second with no wiring (test aid); OFF restores the ruled mechanic,
  where only redstone edges at the Turbine Control Block start cycles.
- `/btssupply [on|off]` - toggles infinite supplies on the nearest main station (mirrors
  `/btsclock`). ON refills the feedwater hatches, the chamber's coolant hatch and all eight
  injectors every tick and lands a perfectly timed 1L per injector on every accepted cycle;
  OFF hands the plant back to real plumbing. With it off, missing supplies only hold the
  RPM down (the panels say exactly why) - they never seize the turbine.
- `/btskit [tier]` - hands over the controllers, blocks, rods, rotors and hatches to
  build by hand.
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
format). Merge per `questbook/README.md`; regenerate files with
`questbook/gen-questbook.ps1` after editing quest definitions.

**An existing world will not show the quests, and that is not a bug in the files.**
BetterQuesting reads `config/betterquesting/DefaultQuests/` exactly once, when a world is
created; after that the world owns `saves/<world>/betterquesting/QuestDatabase.json` and
never reads the defaults again. Merging the overlay therefore fixes NEW worlds only.

- `/btsquests` (op) - prints BetterQuesting's version, where it is reading DefaultQuests
  from, and whether our chapter (190/47990), our 18 quests (190/48000-48017) and the entry
  quest "Unit One" (190/48020) are in THIS world's live database. If they are missing it
  explains why and gives both fixes.
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
