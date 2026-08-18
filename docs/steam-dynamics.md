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
        speed mult  m(i) = 1 + 0.15 x |S(i)|      (t)
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
| Naquadah Fuel Mk I        | 3.0   |                                    |
| Naquadah Fuel Mk II       | 4.0   |                                    |
| Naquadah Fuel Mk III      | 5.0   | knee of the curve                  |
| Naquadah Fuel Mk IV       | 5.5   | linear from here -                 |
| Naquadah Fuel Mk V        | 6.0   | deliberately                       |
| Naquadah Fuel Mk VI (UMV) | 6.5   | "not worth it really"              |

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

- SD-1: rod pillar cascade in the silo (block states per segment for the visual fall,
  loading ports, base extraction, bus fallback penalty). Questbook quests 4.
- SD-2: 3x3 duct piece + Turbine Housing sockets + swirl math + wrench chirality.
- SD-3: spiral chamber + sensor hatch + precision injectors + window/heat/coolant/seize.
- SD-4: Large Turbine Rotor Assembly + Turbine Hall retirement + migration.
- Each phase: recon vs sources -> implement -> review swarm -> smoke test -> tag.

## Questbook

The "Beyond the Stars" quest line overlay lives in `questbook/` (GTNH 2.9.0 BQ split
format). Merge per `questbook/README.md`; regenerate files with
`questbook/gen-questbook.ps1` after editing quest definitions.
