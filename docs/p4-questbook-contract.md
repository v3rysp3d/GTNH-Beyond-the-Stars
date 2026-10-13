# P4 questbook contract

The questbook branch is not in `p4-quasar`'s base, so P4 units write their quest and tooltip content here. The merge
turns each section into quests (rules 14 and 15: every mechanic must be learnable in the questbook or the tooltip).
Numbers come from the engine (`machines/quasar/engine`, `eye/field`). If a number changes there, change it here too.

> **Merged (B-111, 2026-10-13).** Every quest below is now in `questbook/singularity.json` (ids 48144-48150 real,
> 48154-48174 and 48194 new; features `quasar`, `conduit`, `crown`, `eyeT7T8`, `eternityOnErgomatter` on). From now
> on that file is the source; this contract is kept as the record of what the P4 units asked for. The merge rewrote the
> text in master's player-text rules (no `{e}` / `{b}` / `{a}` / `{Phi}`), moved the positions to fit master's map, and
> gated the Eternity edits on `replaceTecTechEyeOfHarmony`. Differences are listed in `docs/questbook-integration.md`
> §2.3 ("The P4 quests") and in B-111.

## Engine maths unit

### Quest: "Feeding the hole" (the Quasar Engine's disc)

- Gas in the 24 Rim Gas Injectors builds the accretion disc. Set the feed f in the GUI (0-1.5, default 0.85) as a
  share of the Eddington feed Ṁ_Edd = 40 × M / κ L/s.
  - Hydrogen has κ 0.4, Helium 0.2 and Raw Star Matter 0.1, so Helium doubles the feed and Raw Star Matter doubles it
    again.
  - At 64 SM on Helium, Ṁ_Edd is 12,800 L/s.
  - ME stocking hatches are fine in the injectors.
- **E, the Eddington balance** (−120 to +120, target 0): starved is negative and over-Eddington is positive. It uses
  the Eye's zones:
  - Resonant within ±10, Stable within ±40, Strained within ±70, Critical within ±100, then Breach.
  - Y_q is 1.25 / 1 / 0.5 / 0. It rises to 1.5 after 10 min Resonant.
- **Trims** go into the 4 trim sockets, and they take **plain input hatches only**:
  - the feed gas: +2 on E per unit of 1,000 × M/64 L;
  - Spatially Enlarged Fluid: −2 per unit of 100 × M/64 L;
  - each is capped at 10 units per second.
- **Blow-out:** a Breach, or Integrity 0. The disc re-forms over 5 min once f ≥ 0.3, with no jets meanwhile. The hole
  (M, a) is never lost.
- **Unattended** at f 0.85 the disc sits Stable. **Riding Eddington** (f 1, with the two trims on E ±5) gives about
  1.5-1.8× the output.

### Quest: "Flux and eruptions" (Φ)

- Φ (0-100) charges by b × λ / 2.4 per second. At b 3 it goes from 0 to 100 in 80 s.
- At 100 it **erupts**: +3 × G0 × Y_q for 3 s, then Φ 0.
- A rising redstone edge on the quasar's **Burst Control Hatch** **purges** early, if Φ ≥ 20. Under 20 the purge is
  refused.
- Never purging averages 1.11 × G0 × Y_q; purging at Φ 40 averages about 0.8. A timed purge spikes the stream (see
  "Magnetar Collapse").

### Quest: "Disc Field Coils"

- Coil tier 1-4 (Infinity, Transcendent Metal, SpaceTime, MagMatter). The field b can be at most the tier.
- Coil power is 2 A UXV × b² while the hole is open. Without it b = 0: the jets stop and Φ freezes.
- The mass caps are 256 / 1,024 / 2,048 / 4,096 SM. Growth stops at the cap, and the GUI says "coil cap".
- The stream is G0 = λ × a² × b² × (M/64)² Bu/t, and each of the two jet ports carries half of it.

### Quest: "Bursts into power" (crowns)

- A crown turns 1 Bu/t into 10¹³ EU/t, sent to the owner's wireless network.
- The caps are Mk I 32 / Mk II 512 / Mk III 8,192 Bu/t. A second crown stacked on top takes the overflow.

### Quest: "The Conduit"

- **Throats:** Mk I / II / III carry 24 / 384 / 6,144 Bu/t, and anything above is clipped.
- **Upkeep** per element (a Junction counts one per leg in use):
  - Molten SpaceTime 4 / 16 / 64 L/s before T7;
  - Ergomatter 0.25 / 1 / 4 L/s after T7.
- Short upkeep closes the element. It reopens after 10 s of full upkeep.
- **The Junction split:** out 1's share is clamp(c + k × (s/15 − 0.5), 0, 1), where:
  - c is the GUI ratio;
  - k is 0, 1, 2 or 4;
  - s is the redstone on its Burst Control Hatch.
  Out 3 takes a fixed percentage off the top first.

### Quest: "Stability Link" (T6 → T7 milestone)

- A linked quasar must deliver a 60 s mean of at least 1.5 Bu/t to the rail's ports, with the high bank taking
  25-75 %, for **30 min unbroken**. The streak resets the second it fails.
- At T6 the bursts are a test fire with no field effect, so send everything to the Eye. Wire Slots [8] to J1.

### Quest: "Magnetar Collapse" (T6 → T7)

- **Overfeed**, 5 min: WDM 61.44 + BDM 15.36 L/s on top of the upkeep, with Ω and Π winding up.
- **Collapse**: the shock is P +120, minus 10 for every Bu/t above 1.5 that lands that second. The skill move is to time
  a purge so the eruption lands on the collapse second. The Galaxy Transition set does it for you:
  - [4] means purge now;
  - [8] means hold your regulator's purges.
- **Halo capture**, 10 min: hold Stable for 60 s in a row between 7:00 and 10:00. A minor merger comes at 7:30.

### Quest: "Triangulum" (Eye T7)

- **Upkeep:** Molten Magnetar Matter 7.5 L per cycle, plus bursts.
- **Product:** Ergomatter 600 L × Y × r × min(β̄, 1.5).
- **P** is pressure: bursts heat it, at 3 × (β − 1) per s, where β is the delivered bursts ÷ 1.5 Bu/t. Space trims cool
  it (−2 per unit, cap 10).
  - **Bring at least 1.5× the demand into J1, and send the surplus to a crown.**
- **W** is the warp of the tilted rail: it drifts 1.5 per s in the precession sense. Aim the bursts between the high and
  low banks (+6 × (low − high)).
  - Minor mergers (every 15-30 min, 15 s warning with the sign) push W ±12 per s for 5 s, then flip the precession.

### Quest: "Heavy Link" and "Galactic Merger" (T7 → T8)

- **Heavy Link:** a 60 s mean of at least 6 Bu/t on the Eye's ports, at least half of it on the halo, for 30 min. The
  test fire (Slots [8]) forces J1 to the Eye and J3 to 75 % halo. In practice this needs a quasar of about 120 SM.
- **The merger**, 30 min:
  - the demand ramps from 1.5 to 6 Bu/t, and the Ergomatter charge feeds at 5 L/s;
  - three warned pericentre passes at 10 / 20 / 30 min give W ±12 and P +5 per s for 5 s;
  - then hold Stable for 2 min in a row within 5 min.

### Quest: "Milky Way" (Eye T8)

- **Feed:** Ergomatter, f₈ 0-10 L/s (default 5).
- **Product:** Superdense Ergomatter = 1.0 × the Ergomatter fed × Y × r.
- **Λ** is the tidal shear: the feed drives it at 0.4 × f₈, and halo bursts hold it at −10 × the halo share. Wire J3.
- **Satellite crossing** (every 30-60 min, an exact 2 min countdown): P is pushed for 5 min. Stay Stable throughout to
  **catch** it, for a bonus cycle of Superdense Ergomatter. Run the Space rig, and lower f₈ during the countdown at
  high feed.

### Quest: "Planets mode" and "Companions" (T7+)

- **Planets mode:** 3 slots at T7 and 6 at T8.
  - One gauge (P, drift 1 × (β − 1), calm), no events, at a quarter of the demand.
  - Each planet runs at parallel 2^(N − k). Its success per parallel is (1 − 0.05 × rocket tier) × the field factor
    (Resonant / Stable 1, Strained 0.5, Critical 0).
- **Companions** run 1 at T7 and 2 at T8, beside the galaxy.
  - Each runs at parallel 2^(N − 1 − k), pays its own upkeep, and adds a quarter of the demand and more noise on P.

### Directive Hatch tables (for the hatch tooltip's shift-expansion and the automation quest)

Each code adds its bits: [1] [2] [4] [8]. The window w is set in the hatch's GUI (default 5).

| Set | [1] | [2] | [4] | [8] |
|---|---|---|---|---|
| Quasar: Disc | E < −w: pulse feed gas | E > +w: Space trim | Φ ≥ purge setpoint: purge | Alarm: blow-out, no coil power, venting, conduit closed |
| Eye: Field (T7+) | P > +w: Space trim | P < −w: more bursts | W > +w: aim high | W < −w: aim low |
| Eye: Events (T7+) | Λ > +w: more to the halo | Λ < −w: less to the halo | A countdown is running | Its sign is − |
| Eye: Galaxy Transition | Ready: begin | Running | Purge now (collapse assist) | Hold the regulator's purges |
| Eye: Slots | A slot is idle | Companion upkeep short | Planet run lost parallels | Link test fire wanted |

**The standard rig** (proved unattended, T6 → T7 and T7 → T8, 5 of 5 seeds, no Eye failures):

- Disc [1] → the feed-gas valve, and Disc [2] → the Space valve (10 units/s each).
- (Disc [4] AND NOT Transition [8]) OR Transition [4] → the quasar's Burst Control Hatch.
- **J1:** the sensor's P (inverted, ±30) OR Transition [4] → 15. **J2:** the sensor's W. **J3:** the sensor's Λ.
- Field [1] → the Eye's Space valve.
- Slots [8] → the test fire. Transition [1] → begin.
- **All binary** (AE2-friendly): feed each Junction two bits as 15 / 8 / 0 instead of the analog sensor:
  - J1: Field [2] / [1];
  - J2: Field [4] / [8];
  - J3: Events [1] / [2].
  It does even better (T8 about 90 % Resonant).

## Content + recipes unit

Item and block ids for the quest tasks (registry names; `beyondthestars:` prefix): `quasarSeed`, `conduitThroatMk1`,
`conduitThroatMk2`, `conduitThroatMk3`, `intergalacticUpgradeCore`, `supermassiveSeedCore`, `starChartTriangulum`,
`starChartMilkyWay`; blocks `slCasing2` metas 3-6 (Disc Field Coil T1-T4), 7 (Eddington Wall Casing), 8 (Rim Gas
Injector), 9 (Jet Collimator Ring), 10 (Burst Thermaliser Casing), 11 (Radiator Petal). Fluids `btsergomatter`,
`btssuperdenseergomatter`. NEI pages: `beyondthestars.recipe.quasar`, `beyondthestars.recipe.thermaliser`.

### Quest: "A seed for a quasar" (needs: Kerr T2)

Task: retrieve 1 Quasar Seed. Text:

> The Quasar Engine needs its own black hole. A Kerr T2 makes the seed without spending its own hole: put 4 Black Hole
> Seeds, 64,000 L of Hyperdense Stellar Mixture and 1,440 L of Magnetar Matter into its Compressor Module while the
> KEE holds at least 256 SM. The seed opens a 64 SM hole once; a hole that collapses goes dormant and can be reopened,
> so you only ever need one.

### Quest: "Building the quasar" (needs: Eye T6)

Tasks: retrieve 64 Eddington Wall Casing, 32 Disc Field Coil (Tier I), 32 Jet Collimator Ring, 8 Rim Gas Injector.
Text:

> The quasar is the largest machine in the UXV chapter: about 28,000 Eddington Wall Casings, 3,354 Disc Field Coils,
> 596 Jet Collimator Rings and 112 Rim Gas Injectors, plus the Black Hole Compressor's own casings for the cage. Every
> one of them is a batch recipe in the Condensate Assembler (64, 32, 32 and 8 a craft). Start with Tier I coils
> (Infinity): the coil tier caps the field b and how big the hole may grow (256 / 1,024 / 2,048 / 4,096 SM), and you
> can rebuild the coils later. Nothing in the quasar needs Ergomatter: the Antipolarization Agent takes its place in
> every recipe you need before Eye T7.

### Quest: "Throats" (needs: the Conduit quest)

Tasks: retrieve 1 Conduit Throat Mk I; optional Mk II. Text:

> A throat sits in a Wormhole Mouth, Conduit Mirror or Junction controller like a turbine rotor. Mk I carries 24 Bu/t,
> Mk II 384, Mk III 6,144 (Mk III takes Ergomatter, so it is a T7 part). Each element burns throat fuel while a stream
> passes: Molten SpaceTime before T7 (4 / 16 / 64 L/s per element), Ergomatter after (0.25 / 1 / 4 L/s).

### Quest: "Intergalactic Upgrade Core" (needs: Stability Link)

Task: retrieve 1. Text: the core of the Magnetar Collapse. Made in the Condensate Assembler from Entangled Magnetar
Matter, the Agent, SpaceTime and Black Dwarf condensates, two Mk I throats and White Dwarf nanites. Held in escrow,
consumed only when the collapse succeeds.

### Quest: "Supermassive Seed Core" (needs: Eye T7, Heavy Link)

Task: retrieve 1. Text: 128,000 L of Ergomatter (Entangled) and 2,304 L of Magnetar Matter make the Galactic Merger's
core; Black Dwarf nanites. The name is the hole it seeds, not the fluid.

### Quest: "Superdense Ergomatter, the slow way" (needs: Eye T7, a quasar link on the Kerr)

Task: retrieve 500 L Superdense Ergomatter. Text: the Kerr Compressor Module refines Ergomatter 8 : 1 (4,000 L +
1,440 L Molten SpaceTime → 500 L, 10 min, UXV) with the KEE at T2, at least 256 SM, and a quasar link on the KEE at
σ ≥ 1. Eye T8 makes the same fluid 1 : 1 from its feed, about nine times better: this route is for before T8.

### Quest: "Eternity before the end" (needs: Eye T7)

Text (replaces the pack's Eternity prerequisite text where it mentions Universium): with Beyond the Stars' Eye of
Harmony, the Temporal Harmony Catalyst takes 9,216 L of Ergomatter instead of a Universium Nanite, and the Plasma
Forge's Timepiece takes 4,608 L of Ergomatter instead of one, so Eternity is made from Eye T7. Set the questbook's
`eternityOnErgomatter` feature flag to true at the merge (it gates the same text in the questbook branch).

### Tooltips already in the jar (rule 14)

Every item and block above has a lang tooltip (what it is for, the key numbers, where it is made). The NEI pages
"Quasar Engine (accretion disc)" and "Burst Thermaliser Crown" show the Eddington feed per gas and each crown's cap and
EU/t; quests can point at them.

## Quasar Engine MTE unit

### Quest: "Opening the quasar" (needs: "Building the quasar")

Put the Quasar Seed in the controller's slot (beside the power button in its GUI) and press **Open** in the Disc
control panel. The hole starts at 64 SM with no spin: the jets are faint for the first hour while the spin climbs (the
GUI says "spinning up"). Feed Helium into the 24 rim Input Hatches (ME stocking hatches are fine); the quasar keeps 10
seconds of feed in its own buffer. Coil power is 2 A UXV × b² from the control box's Energy or Laser Hatches (any
mix). **Collapse** makes the hole dormant (mass kept); a Black Hole Seed in the slot re-opens it. Nothing ever destroys
the hole or a block. Keep the whole quasar chunk-loaded: an unloaded chunk freezes the disc and pauses every machine it
feeds.

### Quest: "Reading the disc" (sensors and the console)

The console's E dial is the disc's Eddington balance (keep it near 0; the bands are Resonant, Stable, Strained). The
flux bar fills to an eruption at 100 (yellow tick: your purge setpoint; red tick: the 90 warning). A Stellar Field
Sensor Hatch in a control-box socket reads E, flux, λ, a, M, Bu/t and more: analog with a window w (E ±w), or 15
above / below a threshold. Invert flips it. Trims go into the 4 plain Input Hatches in the wall tanks: the feed gas
(1,000 × M/64 L a unit) raises E by 2, Spatially Enlarged Fluid (100 × M/64 L) lowers it by 2, at most 10 units a
second each.

### Quest: "Burst Control" (the purge hatch)

A Burst Control Hatch on a cap rim reads redstone. **Purge** mode: a rising edge dumps the flux early (from 20).
**Field b** mode: the redstone level sets b = 0.5 + (coil tier − 0.5) × level/15. The standard rig: Directive Hatch
(Disc set, code output) → bit 3 AND NOT the Eye's Transition bit 4 → this hatch.

### Tooltips already in the jar (Quasar Engine unit)

The controller (disc maths, trims, coils, jets, automation), the Burst Control Hatch (four modes), the Stellar Field
Sensor Hatch (quasar channels, window, invert) and the Directive Hatch (the Disc set).

## Conduit + crowns unit

**Correction to "The Conduit" above:** upkeep is one unit per element (each mouth of a pair, each mirror, each
junction), whatever it carries (F-5, P4-48), not one per Junction leg. The standard T7 rig (4 mouths, combine, J1, J2)
is 7 units: 28 L/s of Molten SpaceTime, or 1.75 L/s of Ergomatter.

### Quest: "Docking a crown" (needs: "Opening the quasar")

A Burst Thermaliser Crown takes a jet when its base centre sits on the quasar's port cell: the cell just past each cap,
on the jet axis. Stand it on the top cap; hang it upside down under the bottom cap (wrench the controller's front to
rotate it). `/btsconduit dock <quasar x y z>` prints both cells and where the controller goes. Mk I / II / III = the
crown you build around the controller (32 / 512 / 8,192 Bu/t); build the bigger one around the same controller to
upgrade. 1 Bu/t = 10¹³ EU/t into your wireless network, no dynamo. A second crown stacked on the first takes the
overflow. A port with nothing docked vents.

### Quest: "A wormhole" (needs: "Throats")

Build two Wormhole Mouths and right-click one with a Data Stick, then the other: they are paired. A stream enters a ring
through its front face (the controller's side) and leaves the partner out of its back, at any distance, even in another
dimension. To take a jet, lay a mouth on a cap with the ring's front-face centre on the port cell, facing the quasar.
Put a Conduit Throat in each controller and fuel in its input hatches (Molten SpaceTime now, Ergomatter after T7; the
GUI's fuel button picks Auto / SpaceTime / Ergomatter). A new element opens after 10 seconds of paid upkeep. Keep both
ends chunk-loaded.

### Quest: "Mirrors and Junctions"

A stream leaves a throat in a straight line and must reach the next throat (or a GRB Receptor Port) through up to 64
blocks of air. The scanner names the target, or "blocked at x, y, z". A Mirror turns it 90° (any facing; either throat
in). A Junction splits (big throat in; out 3 takes its fixed % first, out 1 and out 2 share the rest by
c + k × (s/15 − 0.5), s from its Burst Control Hatch) or combines (three in, one out). One Throat caps the whole
element; the excess is clipped, never damage.

### Quest: "A quasar link" (GRB Receptor Port)

A GRB Receptor Port takes a stream into a machine: the Eye's rail and halo, a crown's base, a Kerr utility socket, the
Homogeny's ring, Reality Diffraction's portal. It records last second's bursts per quasar: your machine is **linked**
while bursts arrived from a formed quasar with its hole open and coils powered, through an open path. Break an element or
starve its fuel and the link drops the next second. If the quasar's chunk unloads, linked machines pause instead of
failing. The quasar's console lists every machine it feeds, with σ.

### Tooltips already in the jar (conduit + crowns unit)

The Wormhole Mouth, Conduit Mirror and Conduit Junction (throats, upkeep, rays, pairing, the split formula, OC), the
Burst Thermaliser Crown (Mk by structure, docking, wireless EU, stacking, OC) and the GRB Receptor Port (links, hosts,
Q-8). OpenComputers: `bts_mouth`, `bts_mirror`, `bts_junction`, `bts_crown`, `bts_receptor`.

## Eye T7 + T8 unit

This text goes with the engine maths unit's quests above, "Stability Link" to "Planets mode" and "Companions". It
corrects or adds the following. Quest ids follow the questbook branch's scheme.

### Quest: "The stability rail" (needs: Eye T6, "A quasar link")

Build the T7 band, the **stability rail**, round the galactic rim. It is Bastion Plate with Casimir trim, tilted 8°.
Its 8 sockets take **GRB Receptor Ports**: 4 on the high bank and 4 on the low bank.

- At T6 the rail's bursts do nothing to the magnetar. They only count for the **Stability Link**: a 60 s mean of at
  least 1.5 Bu/t on the rail, with the high bank taking 25-75 %, for 30 min unbroken.
- Wire the Eye's **Slots [8]** (link test fire wanted) to J1's Burst Control Hatch, so everything goes to the Eye while
  the link is wanted.
- The GUI, the scanner, the sensor's "Link streak %" and "Link 60 s mean" channels, and OpenComputers `getUnlock()`
  show the streak and the mean.

### Quest: "Magnetar Collapse" (corrections)

- Put the **Intergalactic Upgrade Core** in escrow (right-click the controller) and the **Star Chart: Triangulum** in
  the controller slot while the magnetar burns. The Eye is then PENDING, and the magnetar keeps burning.
- The collapse does not begin by itself. When **Galaxy Transition [1]** (ready) is on, **Begin** it: from the GUI's
  Galaxy panel, with a rising edge on a **Burst Control Hatch in Begin mode** in a plinth `d` socket, or with
  OpenComputers `beginTransition()`.
- Before you begin, the buffers must hold the charge: White Dwarf Matter 18,432 L + Black Dwarf Matter 4,608 L (× p),
  plus Triangulum's first cycle (8 L of Magnetar Matter).
- The collapse assist: Galaxy Transition [8] holds your regulator's purges from T−80 s, and [4] purges at T−80 s and on
  the collapse second. Wire them as the standard rig does, and the eruption lands on the collapse.
- If it fails, the magnetar is lost and the core stays in escrow. Leave the Triangulum chart in: the Eye re-spawns the
  magnetar (the Magnetar Merger's charge) and the collapse can run again, with no core and no link.

### Quest: "Triangulum" (additions)

- The **Galaxy panel** (the GUI's galaxy button) sets the mode, the T8 feed and Begin.
- The terminal shows the bursts, β against the demand, W and the event countdown.
- The 4 plinth `d` sockets take Directive Hatches. The standard set is Field, Events and Galaxy Transition, plus a
  Begin hatch; a Slots hatch can go in a Sensor socket.

### Quest: "The shard halo" (needs: Triangulum)

Build the T8 band, the **shard halo**. Its 8 shard roots take GRB Receptor Ports, and the Galactic Merger needs all
eight.

- At T7 the halo's bursts are a **test fire**: they count for the **Heavy Link** (6 Bu/t on the Eye's ports, at least
  half of it on the halo, for 30 min) and do nothing to the galaxy.
- Slots [8] asks for the test fire: J1 all to the Eye, and J3 at its 75 % halo preset. In practice this needs a quasar
  of about 120 SM.

### Quest: "Galactic Merger" (corrections)

- Put the **Supermassive Seed Core** in escrow and the **Star Chart: Milky Way** in the slot while Triangulum burns, in
  galaxy mode with no companion charts in the slot bus.
- The buffers must hold Ergomatter 9,000 L (× p) plus the Milky Way's first feed cycle.
- Then **Begin**.
- A failure loses Triangulum and keeps the core. The Eye re-runs the chain from the magnetar.

### Quest: "Planets mode" (corrections)

- Planets mode is the Eye's **mode switch**: the GUI's Galaxy panel, or OpenComputers `setMode("planets")`. The galaxy
  chart in the slot picks the tier: Triangulum gives 3 slots, the Milky Way 6. The planets field lights at once, with
  no collapse.
- Put **planet items** (the dimension display blocks) in the **slot bus**, the plinth's `p` input bus. They are read,
  never consumed.
- Each slot pays its run at the start (H/He × parallel from the buffers, and its EU) and waits while it cannot.
- A Breach fails the runs in flight and re-forms the field.
- Switching the mode puts a lit galaxy out, and the galaxy comes back only through the Magnetar Collapse.

### Quest: "Companions" (corrections)

- Put the **star charts** of earlier objects (Sol to the magnetar) in the slot bus: 1 at T7, 2 at T8.
- Each companion pays its own upkeep every cycle, in full, and makes its own product at the galaxy's yield.
- Changing the companions **re-forms the galaxy in place**: the gauges go to 0 and the cycle in flight is lost.

### Tooltips in the jar (Eye T7 + T8 unit)

- **The Eye controller** has six lines on T7 / T8 (bursts, the rail and halo, the links, Begin, planets, companions),
  two automation lines (the `d` sockets) and structure lines for the rail, the halo, the Receptor Ports and the `d` /
  `p` sockets.
- **The Directive Hatch** has the Eye Field, Events, Galaxy Transition and Slots tables.
- **The Stellar Field Sensor Hatch** has W, Λ, β, the link streak and mean, slot progress and the countdown.
- **The two galaxy charts** explain how to begin and what planets mode does.
- **NEI** has the T7, T8 and planets-mode pages and the Magnetar Collapse and Galactic Merger pages.

## Hosts + automation unit

This unit adds the other burst hosts (the Kerr, the Homogeny, Reality Diffraction), the P4 OpenComputers methods and
the automation proof. It also corrects three rules the quests above teach. The last section, "Generator entries", holds
every P4 quest in `questbook/singularity.json`'s input format, for the merge.

### Corrections to the quests above

- **"Stability Link", "Heavy Link", the Directive tables.** On T6 and T7, Transition [4] (next core wanted) comes
  when every milestone **except the link** is met (P4-76). Push the core first: the link's test fire (Slots [8]) is
  only wanted once the core is in escrow. The order on T6 / T7 is item milestones, then the core, then the test fire
  and the link, then the chart, then Begin.
- **"Heavy Link".** At T7, Slots [8] also waits for the linked quasar's mean stream to reach 6 Bu/t, about 130 SM at
  coils 3 (P4-79). The halo test fire leaves the rail only a quarter of the stream, so on a smaller quasar it would
  starve Triangulum. The Galaxy panel says "Heavy Link test fire waits: the quasar's mean stream x Bu/t is under 6
  (grow it)".
- **"Heavy Link", the ceiling (P4-88).** Slots [8] also stops above a mean stream of 32 Bu/t (about 260 SM at coils
  3, reachable with Mk II throats): J1 at 15 would put a quarter of it on the rail, and above about 8 Bu/t there the P
  drift outruns the Space valve and Triangulum breaches. The Galaxy panel then says "Heavy Link test fire held ...
  Test-fire with J1's Eye share at x" (16 Bu/t ÷ the stream); OpenComputers `getUnlock()` gives `testFireShare`. Take
  the link with J1 set to that share (a Burst Control Hatch level or `junction.setRatio`) and J3 at 75 % halo.
- **"Feeding the hole", the links.** For the Stability Link, run the disc at **f 1.0**, riding Eddington with the
  Disc [1] / [2] valves. At the GUI default (f 0.85) a 64 SM quasar's stream is too thin for the link (P4-81). A fresh
  hole spins up over about an hour before its stream is full.

### Quest: "A quasar-linked Kerr" (needs: Kerr T2, "A quasar link")

- A GRB Receptor Port fits any KEE utility socket. Its demand is 0.1 Bu/t at T1 and 0.25 at T2. σ = delivered ÷
  demand, on a 60 s mean.
- The Hawking bleed is × (1 − 0.75·min(σ, 1)): at σ ≥ 1 the hole needs 75 % fewer bolts.
- On T2 at σ ≥ 1 the setpoint cap rises from 512 to 768 SM, so the modules get 27 parallels instead of 22.
- A lost link drops the cap back to 512. The hole bleeds down to it slowly; your setpoint is kept and climbs back when
  the link returns. The modules count the mass only up to the cap in force, so the +23 % parallel goes with the link
  at once (P4-90).
- A quasar that is frozen (unformed) or not loaded pauses the link (Q-8): the bleed relief, the 768 SM cap and the
  Superdense refining stop until it runs again (P4-89).
- The Compressor Module's Superdense refining (SL-Q23) needs σ ≥ 1 and at least 256 SM. A run that loses the link
  pauses; it is never voided.
- One Junction's out 3 at 10 % feeds a KEE T2 from any quasar of 2.5 Bu/t or more.

### Quest: "A quasar-held vortex" (Homogeny T2 / T3; needs: Homogeny T2, "A quasar link")

- 3 GRB Receptor Ports fit the stirring ring's reserved cells. Demand is 1.5 Bu/t at T2 and 6 at T3.
- Speed × (1 + 0.5·min(σ, 1)): up to 6× (T2) and 24× (T3) the T1 rate.
- **Entanglement Medium** (T3) needs σ ≥ 1 to start, and pauses below it.
- **Degenerate Stellar Mixture never needs bursts**: Eye T6 needs it before any quasar exists.

### Quest: "Riding the rift" (Reality Diffraction; needs: Reality Diffraction, "A quasar link")

- 6 GRB Receptor Ports fit the portal's reserved cells. Demand is 24 Bu/t.
- **No port fitted:** the regions run at × 1 on their anchor fluids. No link is needed.
- **Ports fitted:** the regions ride the stream. Speed is × min(σ, 1.5). Below σ 0.5 no run starts and a running one
  pauses. Fit ports only when you can feed them.
- A second quasar with its own mouths is the clean way to feed it.

### Quest: "Automating the quasar" (needs: "Burst Control", "Reading the disc")

- A Directive Hatch on **Quasar: Disc** (Code mode, window 5):
  - [1] → a valve of 10 units of the feed gas into a plain trim hatch;
  - [2] → a valve of 10 units of Spatially Enlarged Fluid.
- (Disc [4] AND NOT the Eye's Galaxy Transition [8]) OR Galaxy Transition [4] → the quasar's Burst Control Hatch
  (Purge). That is one AND-NOT gate and one OR gate.
- ME stocking hatches in the gas sockets; set f 1.0 in the GUI.
- OpenComputers instead: `bts_quasar.trim("gas" | "space", units)`, `purge()`, `setFeed(f)`, `setField(b)`.

### Quest: "A galaxy that runs itself" (needs: "Automating the quasar", "The stability rail")

The standard rig, proved on a dev server: a real quasar from a fresh 64 SM hole and a real Eye ran T6 → T7 → T8 with
nothing touched, 0 Eye failures.

- **Eye Field** [1] → the Eye's Space valve (10 units into a Field hatch).
- **J1:** Field [2] / [1] as 15 / 8 / 0 (two emitter covers), OR Galaxy Transition [4] → 15, OR Slots [8] → 15.
- **J2:** Field [4] / [8]. **J3:** Events [1] / [2]; Slots [8] → J3's 75 % preset at T7.
- **The items:**
  - Transition [4] → push the next core into a plinth bus;
  - Transition [1] → push the next chart;
  - Status [1] → stock the charge;
  - Galaxy Transition [1] → the Eye's Burst Control Hatch in Begin mode.
- Keep Mk I throats on the Eye's branch: they cap what reaches J1 at 24 Bu/t, so a growing quasar cannot flood the Eye.

### Quest: "Scripting a quasar" (optional; needs: "Automating the quasar")

With OpenComputers, an Adapter gives these components, rate-limited to one setter call per second:

- `bts_quasar`;
- `bts_eye` with `getEvent`, `getUnlock`, `beginTransition`, `setFeed8`, `setMode`, `trim` and `getSlots`;
- `bts_mouth` / `bts_mirror` (`getLink`, `getThroat`, `getUpkeep`, `setFuel`, `isOpen`);
- `bts_junction` (`getFlow`, `setRatio`, `setGain`, `setOut3`, `setMode`);
- `bts_crown`, `bts_receptor`, `bts_homogeny`, `bts_reality`;
- `bts_kerr` with its link.

### Tooltips in the jar (hosts + automation unit)

- The KEE, Homogeny and Reality Diffraction tooltips: the Receptor Port sockets, demand, what σ does, and that no link is
  needed.
- Their GUIs and scanners show the link line ("Quasar link: x, y, z (dim) · Bu/t · σ").
- The Eye's Galaxy panel says why the T7 test fire waits.

## Generator entries (every P4 quest, `questbook/singularity.json` format)

The merge pastes these into `singularity.json` and then runs `node tools/gen-singularity.js --check`.

**Ids.** The UXV range 48140-48179 is used up to 48153, so the new UXV quests take 48154-48174. The MAX range is used up
to 48193, so the Reality one takes 48194. Every id is appended to `questbook/id-ledger.txt`.

**Positions.** The positions are proposals. Placeholders keep their ids and their main-row positions; new quests sit on
the rows above and below the main row. The tidiness check (overlap, grid, line crossings) has the final word: move a
quest, never change its id.

**At the merge:**

- **Features:** set `quasar`, `conduit`, `crown`, `eyeT7T8` and `eternityOnErgomatter` to `true`. Every placeholder
  below then ships with its real tasks under the same id, and a ticked placeholder box is cleared (spec §4.6).
- **Registry:** our ids are read from the Java and need no `registry.json` row:
  - items `quasarSeed`, `conduitThroatMk1..3`, `intergalacticUpgradeCore`, `supermassiveSeedCore`,
    `starChartTriangulum`, `starChartMilkyWay`;
  - fluids `btsergomatter`, `btssuperdenseergomatter`;
  - blocks `slCasing2:3..11`;
  - MTEs 28120-28127.

### Placeholders this replaces (same id, real tasks)

| Id | Key | Was | Becomes |
|---|---|---|---|
| 48144 | `quasar_engine` | retrieval of the controller, "will be" text | the controller and the quasar BOM; the disc taught |
| 48145 | `einstein_rosen_conduit` | "will carry" text | mouths, a mirror, a junction and a Mk I throat; wormholes, mirrors and junctions taught |
| 48146 | `eye_t7` | `tbd` tasks | 1,000 L of Ergomatter after the collapse (the core and the chart move to the new "Magnetar Collapse") |
| 48147 | `ergomatter` | `tbd` | 16,000 L of Ergomatter; its uses (throat fuel, T8 feed, Eternity) |
| 48148 | `eye_t8` | `tbd` | 1,000 L of Superdense Ergomatter after the merger |
| 48149 | `supermassive_ergomatter` (stem `SupermassiveErgo`) | `tbd` | 16,000 L of Superdense Ergomatter; the two routes |
| 48150 | `burst_thermaliser_crown` | retrieval, "will" text | the crown and the Mk I BOM; docking taught |
| 48142 | `homogeny_t2` | the H/R unit's quest | unchanged, plus one appended line pointing at "A quasar-held vortex" |
| 48184 | `homogeny_t3` | the H/R unit's quest | unchanged, plus "Entanglement Medium needs σ ≥ 1 (a quasar link)" |
| 48185 | `reality_diffraction` | the H/R unit's quest | unchanged, plus one line about its ports (see "Riding the rift") |

The checkbox quests `planets_on_the_eye` (48111) and `harmonic_lock` (48112) are T1-T4 content and are not touched.

**`bomItems`: rows for the new manifest roles.** Add families `quasar`, `crown` and `conduit`; `eye` gains the rail
and halo roles.

```json
"quasar": {
  "eddington_wall": {"item": "beyondthestars:slCasing2:7", "expect": "Eddington Wall"},
  "disc_field_coil": {"item": "beyondthestars:slCasing2:3", "expect": "Disc Field Coil"},
  "collimator": {"item": "beyondthestars:slCasing2:9", "expect": "Jet Collimator Ring"},
  "rim_injector": {"item": "beyondthestars:slCasing2:8", "expect": "Rim Gas Injector"},
  "cage_casing": {"item": "gregtech:gt.blockcasings10:11", "expect": "ExtremeDensitySpaceBendingCasing"},
  "cage_frame": {"item": "gregtech:gt.blockframes:325", "expect": "NaquadahAlloy"},
  "wall_glass": {"item": "tectech:tile.quantumGlass:0", "expect": "QuantumGlass"},
  "tank_wall": {"item": "gregtech:gt.blockcasings10:12", "expect": "BackgroundRadiationAbsorbentCasing"},
  "casimir": {"item": "beyondthestars:slCasing:14", "expect": "slCasing:14"},
  "gas_socket": null, "trim_socket": null, "burst_control_socket": null, "energy_socket": null,
  "directive_socket": null, "sensor_socket": null, "maintenance_socket": null, "controller": null
},
"crown": {
  "radiator_petal": {"item": "beyondthestars:slCasing2:11", "expect": "Radiator Petal"},
  "thermaliser": {"item": "beyondthestars:slCasing2:10", "expect": "Burst Thermaliser Casing"},
  "collimator": {"item": "beyondthestars:slCasing2:9", "expect": "Jet Collimator Ring"},
  "base_casing": {"item": "gregtech:gt.blockcasings10:11", "expect": "ExtremeDensitySpaceBendingCasing"},
  "grb_receptor": null, "maintenance_socket": null, "controller": null
},
"conduit": {
  "outer_rings": {"item": "tectech:gt.blockcasingsBA0:10", "expect": "ReinforcedTemporalStructureCasing"},
  "throat_rings": {"item": "tectech:gt.blockcasingsBA0:10", "expect": "ReinforcedTemporalStructureCasing"},
  "throat_shell": {"item": "tectech:gt.blockcasingsBA0:11", "expect": "ReinforcedSpatialStructureCasing"},
  "housing": {"item": "tectech:gt.blockcasingsBA0:11", "expect": "ReinforcedSpatialStructureCasing"},
  "chamber_shell": {"item": "tectech:gt.blockcasingsBA0:11", "expect": "ReinforcedSpatialStructureCasing"},
  "chamber_glass": {"item": "tectech:tile.quantumGlass:0", "expect": "QuantumGlass"},
  "collimator": {"item": "beyondthestars:slCasing2:9", "expect": "Jet Collimator Ring"},
  "fuel_injector": {"item": "beyondthestars:slCasing2:8", "expect": "Rim Gas Injector"},
  "casimir": {"item": "beyondthestars:slCasing:14", "expect": "slCasing:14"},
  "mirror": {"item": "beyondthestars:slCasing:1", "expect": "slCasing:1"},
  "fuel_socket": null, "burst_control_socket": null, "energy_socket": null, "maintenance_socket": null,
  "controller": null
},
"eye (add)": {
  "rail_halo_shell": {"item": "beyondthestars:slCasing:13", "expect": "slCasing:13"},
  "rail_trim": {"item": "beyondthestars:slCasing:14", "expect": "slCasing:14"},
  "grb_receptor": null
}
```

The `expect` strings must match the manifest's block text the way the existing rows do (the generator's
`bomItems … expect` check). Adjust them to its rule at the merge.

### The quests

`{b}`…`{r}` is bold, `{e}{l}` a heading, `{a}{l}` the done-when line, as in the existing entries. σ, λ, Λ and β are
`{u+03c3}`, `{u+03bb}`, `{u+039b}` and `{u+03b2}`. Prerequisites name lows (or pack edit ids).

```json
[
{"low":48144,"key":"quasar_engine","chapter":"uxv","x":456,"y":156,"size":48,"main":true,
 "name":"{4}{l}{n}Quasar Engine","icon":"mte:28120","needs":["quasar"],"prereqs":[48141,48154],
 "tasks":[{"type":"retrieval","items":["1x mte:28120"]},{"type":"optional_retrieval","bom":"quasar"}],
 "desc":["The Quasar Engine is its own black hole with an {b}accretion disc{r}. It runs no recipes; it makes {b}bursts{r} (Bu/t) on two jets, for power (crowns) and for stability (the Eye, the Kerr, the Homogeny, Reality Diffraction).",
  "",
  "{e}{l}BUILD{r} {bomdims:quasar}: {bom:quasar:E} Eddington Wall, {bom:quasar:H} Disc Field Coils (one tier everywhere), {bom:quasar:C} Jet Collimator Rings, {bom:quasar:I} Rim Gas Injectors and the Black Hole Compressor's casings. The coil tier caps the field b and the hole: 256 / 1,024 / 2,048 / 4,096 SM.",
  "",
  "{e}{l}OPEN{r} A Quasar Seed in the controller slot, then Open in the GUI: a 64 SM hole with no spin. It spins up over about an hour. Collapse makes it dormant; a Black Hole Seed reopens it. Nothing ever destroys the hole or a block.",
  "",
  "{e}{l}THE DISC{r} Helium (or Hydrogen, Raw Star Matter) into the 24 gas sockets; ME stocking hatches are fine. The feed f (GUI, 0-1.5) is a share of the Eddington feed 40 {times} M / {u+03ba} L/s.",
  "- {b}E{r}, the Eddington balance: Resonant within {pm}10, Stable {pm}40, Strained {pm}70, Critical {pm}100, then a blow-out (5 min to re-form, the hole kept).",
  "- Trims into the 4 {b}plain{r} trim hatches: the feed gas +2 E a unit, Spatially Enlarged Fluid -2, 10 units a second at most.",
  "- {b}{Phi}{r}, the flux, charges with b and erupts at 100 (3 s at 3 {times} the stream). A Burst Control Hatch purges it early (from 20).",
  "- The stream is G0 = {u+03bb} {times} a{sq} {times} b{sq} {times} (M/64){sq}; each jet carries half. Coil power is 2 A UXV {times} b{sq}.",
  "- {b}f 0.85{r} sits Stable with no trims. {b}f 1.0{r} with the two trim valves (riding Eddington) gives more, and the links need it.",
  "",
  "{a}{l}DONE WHEN{r} you hold the controller."]},
{"low":48154,"key":"quasar_seed","chapter":"uxv","x":384,"y":60,"size":24,
 "name":"{4}{l}{n}A Seed for a Quasar","icon":"beyondthestars:quasarSeed","needs":["quasar"],"prereqs":[48113],
 "tasks":[{"type":"retrieval","items":["1x beyondthestars:quasarSeed"]}],
 "desc":["The Quasar Engine needs its own black hole. A Kerr T2 makes the seed without spending its own hole: 4 Black Hole Seeds, 64,000 L of Hyperdense Stellar Mixture and 1,440 L of Magnetar Matter in its Compressor Module, with the KEE holding at least 256 SM.",
  "The seed opens a 64 SM hole once. A collapsed hole goes dormant and reopens with a plain Black Hole Seed, so you only ever need one."]},
{"low":48155,"key":"disc_field_coils","chapter":"uxv","x":456,"y":60,"size":24,
 "name":"{4}{l}{n}Disc Field Coils","icon":"beyondthestars:slCasing2:3","needs":["quasar"],"prereqs":[48144],
 "tasks":[{"type":"retrieval","items":["32x beyondthestars:slCasing2:3"]},{"type":"optional_retrieval","items":["1x beyondthestars:slCasing2:4","1x beyondthestars:slCasing2:5","1x beyondthestars:slCasing2:6"]}],
 "desc":["Tier I-IV: Infinity, Transcendent Metal, SpaceTime, MagMatter. Every coil in a quasar is the same tier.",
  "- The field b is at most the tier; b mode on a Burst Control Hatch sets it from redstone (0.5 + (tier - 0.5) {times} s/15).",
  "- The hole grows to the tier's cap: 256 / 1,024 / 2,048 / 4,096 SM, then the GUI says {b}coil cap{r}.",
  "- Without coil power b is 0: the jets stop and {Phi} freezes.",
  "Rebuild with a higher tier at any time; the hole is kept. Tier IV's MagMatter comes from the Godforge."]},
{"low":48156,"key":"reading_the_disc","chapter":"uxv","x":528,"y":60,"size":24,
 "name":"{4}{l}{n}Reading the Disc","icon":"mte:28116","needs":["quasar"],"prereqs":[48144],
 "tasks":[{"type":"retrieval","items":["1x mte:28127"]},{"type":"optional_retrieval","items":["1x mte:28116"]}],
 "desc":["The control box takes 2 Directive Hatches and 2 Stellar Field Sensor Hatches.",
  "{e}{l}QUASAR: DISC{r} (Directive Hatch, window w, default 5): [1] E < -w: pulse the feed gas. [2] E > +w: Space trim. [4] {Phi} at the purge setpoint: purge. [8] alarm: blow-out or re-forming, no coil power, a jet venting, a conduit element closed.",
  "{e}{l}SENSOR{r} E, {Phi}, {u+03bb}, a, M, Bu/t, the eruption warning, seconds to the next eruption, the best host's {u+03c3}, the link streak: analog with a window, or 15 above / below a threshold; invert flips it.",
  "A comparator reads the Directive code; Steve's Factory Manager reads it as an analog range."]},
{"low":48157,"key":"burst_control","chapter":"uxv","x":600,"y":60,"size":24,
 "name":"{4}{l}{n}Burst Control","icon":"mte:28126","needs":["quasar"],"prereqs":[48144],
 "tasks":[{"type":"retrieval","items":["1x mte:28126"]}],
 "desc":["A Burst Control Hatch reads redstone. Its mode is set in its GUI:",
  "- {b}Purge{r} (quasar cap rims): a rising edge purges the flux (from 20). Purging at {Phi} 40 averages 0.8 {times} the stream; never purging 1.11, but a timed purge makes a spike.",
  "- {b}Field b{r} (quasar): b = 0.5 + (coil tier - 0.5) {times} s/15.",
  "- {b}Junction split{r} (a Conduit Junction): out 1's share = c + k {times} (s/15 - 0.5).",
  "- {b}Begin{r} (an Eye plinth d socket): a rising edge begins the Magnetar Collapse or Galactic Merger when Galaxy Transition [1] is on."]},
{"low":48150,"key":"burst_thermaliser_crown","chapter":"uxv","x":468,"y":264,"size":24,
 "name":"{4}{l}{n}Burst Thermaliser Crown","icon":"mte:28124","needs":["crown"],"prereqs":[48144],
 "tasks":[{"type":"retrieval","items":["1x mte:28124"]},{"type":"optional_retrieval","bom":"crown_mk1"}],
 "desc":["The quasar's power plant: {b}1 Bu/t {to} 10{sup13} EU/t{r} into your wireless network, no dynamo.",
  "- Dock it on a jet: its base centre on the port cell just past the cap, on the jet axis. Stand it on the top cap; hang it upside down under the bottom cap (wrench the controller's front to turn it).",
  "- Mk I / II / III is the crown you build around the same controller: 32 / 512 / 8,192 Bu/t. A second crown stacked on the first takes the overflow.",
  "- A port with nothing docked vents (Disc [8] lights)."]},
{"low":48158,"key":"conduit_throats","chapter":"uxv","x":564,"y":252,"size":24,
 "name":"{4}{l}{n}Throats","icon":"beyondthestars:conduitThroatMk1","needs":["conduit"],"prereqs":[48144],
 "tasks":[{"type":"retrieval","items":["1x beyondthestars:conduitThroatMk1"]},{"type":"optional_retrieval","items":["1x beyondthestars:conduitThroatMk2"]}],
 "desc":["A throat sits in a Wormhole Mouth, Conduit Mirror or Junction controller like a turbine rotor. Mk I carries 24 Bu/t, Mk II 384, Mk III 6,144 (Mk III takes Ergomatter: a T7 part). Anything above is clipped, never damage.",
  "Each element burns one upkeep unit a second while a throat is fitted: Molten SpaceTime 4 / 16 / 64 L/s before T7, Ergomatter 0.25 / 1 / 4 L/s after (the GUI's fuel button: Auto, SpaceTime, Ergomatter). Short upkeep closes it; it reopens after 10 s paid."]},
{"low":48145,"key":"einstein_rosen_conduit","chapter":"uxv","x":564,"y":168,"size":24,
 "name":"{4}{l}{n}Einstein-Rosen Conduit","icon":"mte:28121","needs":["conduit"],"prereqs":[48144,48158],
 "tasks":[{"type":"retrieval","items":["2x mte:28121","1x mte:28122","1x mte:28123"]},{"type":"optional_retrieval","bom":"conduit_mouth"}],
 "desc":["The Conduit carries bursts anywhere with no loss.",
  "- {b}Wormhole Mouths{r}: right-click one with a Data Stick, then the other: paired, at any distance, any dimension. A stream enters a ring through its front face and leaves the partner from its back. Dock a mouth on a quasar cap with its front-face centre on the port cell.",
  "- {b}Mirrors{r} turn a stream 90{deg} (any facing, either throat in).",
  "- {b}Junctions{r} split (big throat in; out 3 takes its fixed % first; out 1 and out 2 share the rest by c + k(s/15 - 0.5), s from its Burst Control Hatch) or combine (three in, one out).",
  "- A stream flies straight up to 64 blocks of air to the next throat or a GRB Receptor Port; the scanner names the target or what blocks it.",
  "Keep every element chunk-loaded: an unloaded one breaks the path."]},
{"low":48159,"key":"quasar_link","chapter":"uxv","x":648,"y":60,"size":24,
 "name":"{4}{l}{n}A Quasar Link","icon":"mte:28125","needs":["conduit"],"prereqs":[48145],
 "tasks":[{"type":"retrieval","items":["1x mte:28125"]}],
 "desc":["A GRB Receptor Port takes a stream into a machine: the Eye's rail and halo, a crown's base, a Kerr utility socket, the Homogeny's ring, Reality Diffraction's portal.",
  "A machine is {b}linked{r} while bursts arrived last second from a formed quasar with its hole open and coils powered, through an open path. {u+03c3} = delivered {div} the machine's demand, on a 60 s mean. Conditions and bonuses use the mean.",
  "If the quasar's chunk unloads, linked machines {b}pause{r} instead of failing (their {u+03c3} is held). The quasar's GUI lists every machine it feeds, with {u+03c3}.",
  "No machine needs a link to work, except a Reality Diffraction with ports fitted."]},
{"low":48160,"key":"stability_rail","chapter":"uxv","x":720,"y":60,"size":24,
 "name":"{4}{l}{n}The Stability Rail","icon":"beyondthestars:slCasing:13","needs":["eyeT7T8"],"prereqs":[48141,48159],
 "tasks":[{"type":"retrieval","items":["8x mte:28125"]},{"type":"optional_retrieval","bom":"eye_rail"}],
 "desc":["Eye band 7, round the galactic rim: Bastion Plate with Casimir trim, tilted 8{deg} ({bom:eye_rail:B} Bastion). Its 8 sockets take GRB Receptor Ports: 4 on the high bank, 4 on the low.",
  "At T6 the rail's bursts do nothing to the magnetar; they count only for the {b}Stability Link{r}."]},
{"low":48161,"key":"stability_link","chapter":"uxv","x":720,"y":108,"size":24,
 "name":"{4}{l}{n}Stability Link","icon":"mte:28125","needs":["eyeT7T8"],"prereqs":[48160,48145],
 "tasks":[{"type":"checkbox"}],
 "desc":["The T6 {to} T7 machine milestone: a linked quasar delivers a 60 s mean of at least {b}1.5 Bu/t{r} to the rail, the high bank taking 25-75 %, for {b}30 min unbroken{r}. The streak resets the second it fails.",
  "{e}{l}THE ORDER{r} The item milestones first (Magnetar Matter 2,700 L from all-Stable cycles, 2 h Resonant, 50 giant flares ridden). Then {b}Transition [4]{r} asks for the Intergalactic Upgrade Core: put it in escrow. Then {b}Slots [8]{r} asks for the test fire: J1 all to the Eye.",
  "Run the quasar at {b}f 1.0{r} with its trim valves: a 64 SM quasar at the default f 0.85 is too thin. A fresh hole spins up for about an hour first.",
  "The GUI, the scanner, the sensor's link streak and mean channels, and OpenComputers getUnlock() show the streak.",
  "{a}{l}DONE WHEN{r} the Eye's GUI says the Stability Link is HELD (tick the box)."]},
{"low":48162,"key":"magnetar_collapse","chapter":"uxv","x":648,"y":108,"size":24,
 "name":"{4}{l}{n}Magnetar Collapse","icon":"beyondthestars:intergalacticUpgradeCore","needs":["eyeT7T8"],"prereqs":[48161],
 "tasks":[{"type":"retrieval","items":["1x beyondthestars:intergalacticUpgradeCore","1x beyondthestars:starChartTriangulum"]}],
 "desc":["The T6 {to} T7 transition. With the core in escrow and the Triangulum chart in the slot the Eye is PENDING; the magnetar keeps burning.",
  "- Buffer the charge: White Dwarf Matter 18,432 L + Black Dwarf Matter 4,608 L ({times} p), plus Triangulum's first cycle.",
  "- {b}Begin{r} when Galaxy Transition [1] is on: the GUI's Galaxy panel, a Burst Control Hatch in Begin mode on a plinth d socket, or OpenComputers beginTransition().",
  "{e}{l}PHASES{r} Overfeed 5 min (WDM 61.44 + BDM 15.36 L/s, {omega} and {Pi} wind up). Collapse: a shock of P +120, minus 10 for every Bu/t above 1.5 that lands that second. Halo capture 10 min: Stable 60 s in a row between 7:00 and 10:00, a minor merger at 7:30.",
  "{e}{l}THE SKILL MOVE{r} Land an eruption on the collapse second. {b}Galaxy Transition{r} [4] means purge now, [8] means hold your regulator's purges: wire (Disc [4] AND NOT [8]) OR [4] to the quasar's Burst Control Hatch.",
  "A failure loses the magnetar and keeps the core. Leave the chart in: the Eye re-spawns the magnetar and runs it again, with no core."]},
{"low":48146,"key":"eye_t7","chapter":"uxv","x":648,"y":156,"size":48,"main":true,
 "name":"{4}{l}{n}Eye T7: Triangulum","icon":"beyondthestars:starChartTriangulum","needs":["eyeT7T8"],"prereqs":[48162],
 "tasks":[{"type":"fluid","fluids":["1000x fluid:btsergomatter"]}],
 "desc":["Triangulum: the first galaxy, and the first tier the quasar holds together. The new verb is {b}aim{r}.",
  "{e}{l}RUN{r} Molten Magnetar Matter 7.5 L a cycle plus bursts, for {b}600 L Ergomatter {times} Y {times} r {times} min({u+03b2}, 1.5){r}.",
  "{e}{l}THE GAUGES{r}",
  "- {b}P{r}, pressure: bursts heat it at 3 {times} ({u+03b2} - 1) a second ({u+03b2} = delivered {div} 1.5 Bu/t); Space trims cool it (-2 a unit). Bring 1.5 {times} the demand into J1 and send the surplus to a crown.",
  "- {b}W{r}, the rail's warp: drifts 1.5 a second; aim between the banks (+6 {times} (low - high)). Minor mergers (every 15-30 min, 15 s warning with the sign) push W {pm}12 a second for 5 s.",
  "{e}{l}DIRECTIVES{r} Field: [1] P > +w Space trim, [2] P < -w more bursts, [4] W > +w aim high, [8] W < -w aim low. Events: [1] {u+039b} > +w, [2] {u+039b} < -w, [4] a countdown, [8] its sign is -.",
  "The Galaxy panel (the GUI's galaxy button) sets the mode, the T8 feed and Begin.",
  "{a}{l}DONE WHEN{r} you have 1,000 L of Ergomatter."]},
{"low":48147,"key":"ergomatter","chapter":"uxv","x":648,"y":252,"size":48,"main":true,
 "name":"{4}{l}{n}Ergomatter","icon":"eternalsingularity:combined_singularity:15","needs":["eyeT7T8"],"prereqs":[48146],
 "tasks":[{"type":"fluid","fluids":["16000x fluid:btsergomatter"]}],
 "desc":["Eye T7's harvest. It is the Conduit's fuel after T7 (0.25 / 1 / 4 L/s per element), Eye T8's feed, the Mk III throat, and with Beyond the Stars' Eye the new input of the Eternity loop (the Temporal Harmony Catalyst and the Timepiece take Ergomatter instead of Universium).",
  "A quasar-linked Kerr T2 refines it into Superdense Ergomatter at 8 : 1."]},
{"low":48163,"key":"shard_halo","chapter":"uxv","x":744,"y":252,"size":24,
 "name":"{4}{l}{n}The Shard Halo","icon":"beyondthestars:slCasing:13","needs":["eyeT7T8"],"prereqs":[48146],
 "tasks":[{"type":"retrieval","items":["8x mte:28125"]},{"type":"optional_retrieval","bom":"eye_halo"}],
 "desc":["Eye band 8: the shard halo ({bom:eye_halo:B} Bastion). Its 8 shard roots take GRB Receptor Ports; the Galactic Merger needs all eight.",
  "At T7 the halo's bursts are a test fire: they count for the Heavy Link and do nothing to the galaxy. Compression must be at tier 8."]},
{"low":48164,"key":"heavy_link","chapter":"uxv","x":744,"y":300,"size":24,
 "name":"{4}{l}{n}Heavy Link","icon":"mte:28125","needs":["eyeT7T8"],"prereqs":[48163],
 "tasks":[{"type":"checkbox"}],
 "desc":["The T7 {to} T8 machine milestone: a 60 s mean of at least {b}6 Bu/t{r} on the Eye's ports, at least half of it on the halo, for {b}30 min unbroken{r}.",
  "{e}{l}THE ORDER{r} The item milestones (216,000 L Ergomatter from all-Stable cycles, 2 h Resonant, 4 minor mergers ridden), then {b}Transition [4]{r}: the Supermassive Seed Core into escrow, then {b}Slots [8]{r}: the test fire, J1 all to the Eye and J3 to its 75 % halo preset.",
  "Slots [8] {b}waits until the quasar's mean stream reaches 6 Bu/t{r}, about 130 SM at coils 3: below that the rail's quarter would starve Triangulum. The Galaxy panel says how far you are. Keep the quasar growing (f 1.0).",
  "Above {b}32 Bu/t{r} (about 260 SM) Slots [8] stops again: the rail's quarter would flood the field. The Galaxy panel gives the J1 Eye share to test-fire with instead (16 Bu/t to the Eye); set J1 to it by hand, a Burst Control Hatch level or OpenComputers.",
  "{a}{l}DONE WHEN{r} the Eye's GUI says the Heavy Link is HELD (tick the box)."]},
{"low":48165,"key":"galactic_merger","chapter":"uxv","x":744,"y":156,"size":24,
 "name":"{4}{l}{n}Galactic Merger","icon":"beyondthestars:supermassiveSeedCore","needs":["eyeT7T8"],"prereqs":[48164],
 "tasks":[{"type":"retrieval","items":["1x beyondthestars:supermassiveSeedCore","1x beyondthestars:starChartMilkyWay"]}],
 "desc":["The T7 {to} T8 transition. The Supermassive Seed Core in escrow, the Milky Way chart in the slot, galaxy mode, no companion charts; buffer Ergomatter 9,000 L ({times} p) plus the Milky Way's first feed cycle. Then {b}Begin{r}.",
  "30 min: the demand ramps from 1.5 to 6 Bu/t and the charge feeds at 5 L/s; three warned pericentre passes (10 / 20 / 30 min) give W {pm}12 and P +5 a second for 5 s; then Stable 2 min in a row within 5 min.",
  "A failure loses Triangulum and keeps the core; the Eye re-runs the chain from the magnetar."]},
{"low":48148,"key":"eye_t8","chapter":"uxv","x":828,"y":144,"size":72,"main":true,
 "name":"{4}{l}{n}Eye T8: Milky Way","icon":"beyondthestars:starChartMilkyWay","needs":["eyeT7T8"],"prereqs":[48165],
 "tasks":[{"type":"fluid","fluids":["1000x fluid:btssuperdenseergomatter"]}],
 "desc":["The Milky Way. You aim a third stream into the shard halo and catch the satellite crossings.",
  "{e}{l}RUN{r} Ergomatter feed f8 0-10 L/s (Galaxy panel, default 5) for {b}Superdense Ergomatter = 1.0 {times} the feed {times} Y {times} r{r}.",
  "- {b}{u+039b}{r}, tidal shear: the feed drives it at 0.4 {times} f8, halo bursts hold it at -10 {times} the halo share. Wire J3 to Events [1] / [2].",
  "- {b}Satellite crossing{r} (every 30-60 min, an exact 2 min countdown): P is pushed for 5 min; stay Stable to catch it for a bonus cycle. Lower f8 during the countdown at high feed.",
  "T8 is required: the ladder is strict.",
  "{a}{l}DONE WHEN{r} you have 1,000 L of Superdense Ergomatter."]},
{"low":48149,"key":"supermassive_ergomatter","chapter":"uxv","stem":"SupermassiveErgo","x":840,"y":252,"size":48,"main":true,
 "name":"{4}{l}{n}Superdense Ergomatter","icon":"eternalsingularity:combined_singularity:10","needs":["eyeT7T8"],"prereqs":[48148],
 "tasks":[{"type":"fluid","fluids":["16000x fluid:btssuperdenseergomatter"]}],
 "desc":["Ergomatter, compacted. Two routes, one fluid:",
  "- {b}Eye T8{r}: 1 : 1 from its feed, about nine times better.",
  "- {b}The Kerr{r} (before T8): the Compressor Module refines 4,000 L Ergomatter + 1,440 L Molten SpaceTime {to} 500 L in 10 min at UXV, with the KEE at T2, 256 SM or more and a quasar link at {u+03c3} {ge} 1.",
  "It feeds the T9 pellets, the Homogeny's Entanglement Medium and Reality Diffraction's Positive Mass anchor."]},
{"low":48166,"key":"superdense_refinery","chapter":"uxv","x":744,"y":348,"size":24,
 "name":"{4}{l}{n}Superdense Ergomatter, the Slow Way","icon":"mte:28117","needs":["eyeT7T8"],"prereqs":[48147,48167],
 "tasks":[{"type":"fluid","fluids":["500x fluid:btssuperdenseergomatter"]}],
 "desc":["Before T8, the Kerr Compressor Module refines Ergomatter 8 : 1: 4,000 L + 1,440 L Molten SpaceTime {to} 500 L, 10 min, UXV.",
  "It needs the KEE at T2, holding at least 256 SM, with a quasar link at {u+03c3} {ge} 1 (a GRB Receptor Port in a utility socket). A run that loses the link pauses; nothing is voided."]},
{"low":48167,"key":"kerr_link","chapter":"uxv","x":840,"y":348,"size":24,
 "name":"{4}{l}{n}A Quasar-Linked Kerr","icon":"mte:28114","needs":["conduit"],"prereqs":[48113,48159],
 "tasks":[{"type":"checkbox"}],
 "desc":["A GRB Receptor Port in any KEE utility socket. Demand 0.1 Bu/t (T1) / 0.25 (T2); {u+03c3} on the 60 s mean.",
  "- Hawking bleed {times} (1 - 0.75 {times} min({u+03c3}, 1)): 75 % fewer bolts at {u+03c3} {ge} 1.",
  "- T2 at {u+03c3} {ge} 1: the setpoint cap rises 512 {to} 768 SM, so the modules get 27 parallels instead of 22. A lost link drops the cap back and the modules count the mass only up to it; the hole bleeds down slowly, your setpoint is kept.",
  "- A Junction's out 3 at 10 % feeds a KEE T2 from any quasar of 2.5 Bu/t or more.",
  "The KEE's GUI and scanner show the link; OpenComputers bts_kerr.getState().link."]},
{"low":48168,"key":"homogeny_link","chapter":"uxv","x":228,"y":360,"size":24,
 "name":"{4}{l}{n}A Quasar-Held Vortex","icon":"mte:28130","needs":["homogeny","conduit"],"prereqs":[48142,48159],
 "tasks":[{"type":"checkbox"}],
 "desc":["3 GRB Receptor Ports on the Homogeny's stirring ring. Demand 1.5 Bu/t (T2) / 6 (T3).",
  "- Speed {times} (1 + 0.5 {times} min({u+03c3}, 1)): up to 6{times} (T2) and 24{times} (T3) the T1 rate.",
  "- {b}Entanglement Medium{r} (T3) needs {u+03c3} {ge} 1 to start and pauses below it.",
  "- {b}Degenerate Stellar Mixture never needs bursts.{r}"]},
{"low":48194,"key":"reality_link","chapter":"max","x":336,"y":96,"size":24,
 "name":"{4}{l}{n}Riding the Rift","icon":"mte:28131","needs":["realityRefab","conduit"],"prereqs":[48185,48159],
 "tasks":[{"type":"checkbox"}],
 "desc":["Reality Diffraction's portal has 6 reserved cells for GRB Receptor Ports. Demand 24 Bu/t.",
  "- {b}No port fitted{r}: the regions run at {times} 1 on their anchor fluids. No link needed.",
  "- {b}Ports fitted{r}: speed {times} min({u+03c3}, 1.5); below {u+03c3} 0.5 no run starts and a running one pauses (nothing voided). Fit them only when you can feed them: a second quasar with its own mouths is the clean way."]},
{"low":48169,"key":"planets_mode","chapter":"uxv","x":552,"y":348,"size":24,
 "name":"{4}{l}{n}Planets Mode","icon":"mte:28115","needs":["eyeT7T8"],"prereqs":[48146],
 "tasks":[{"type":"checkbox"}],
 "desc":["The Eye's mode switch (Galaxy panel, or OpenComputers setMode(\"planets\")). The galaxy chart picks the tier: Triangulum 3 slots, the Milky Way 6. It lights at once, with no collapse.",
  "- Planet items (the dimension display blocks) go in the {b}slot bus{r} (the plinth's p input bus); they are read, never consumed.",
  "- One gauge (P, drift 1 {times} ({u+03b2} - 1)), no events, a quarter of the demand. Each planet runs at parallel 2^(N - k); its success per parallel is (1 - 0.05 {times} rocket tier) {times} the field factor.",
  "- Each slot pays its run at the start and waits while it cannot (Slots [1]). A Breach fails the runs and re-forms the field.",
  "Switching the mode puts a lit galaxy out; it comes back only through the Magnetar Collapse."]},
{"low":48170,"key":"companions","chapter":"uxv","x":600,"y":348,"size":24,
 "name":"{4}{l}{n}Companions","icon":"beyondthestars:starChartSol","needs":["eyeT7T8"],"prereqs":[48146],
 "tasks":[{"type":"checkbox"}],
 "desc":["Star charts of earlier objects (Sol to the magnetar) in the slot bus run beside the galaxy: 1 at T7, 2 at T8.",
  "Each pays its own upkeep every cycle, runs at parallel 2^(N - 1 - k), makes its own product at the galaxy's yield, and adds a quarter of the demand and more noise on P. Slots [2]: a companion's upkeep is short.",
  "Changing the companions re-forms the galaxy in place: the gauges go to 0 and the cycle in flight is lost."]},
{"low":48171,"key":"automating_quasar","chapter":"uxv","x":528,"y":12,"size":24,
 "name":"{4}{l}{n}Automating the Quasar","icon":"mte:28127","needs":["quasar"],"prereqs":[48156,48157],
 "tasks":[{"type":"checkbox"}],
 "desc":["The disc runs itself on two valves and two logic gates:",
  "- Directive Hatch, {b}Quasar: Disc{r}, Code mode, window 5. [1] {to} 10 units of the feed gas a second into a plain trim hatch (an AE2 fluid export bus with a redstone card, or a GT Fluid Regulator cover set to enable with signal). [2] {to} 10 units of Spatially Enlarged Fluid.",
  "- (Disc [4] AND NOT the Eye's Galaxy Transition [8]) OR Galaxy Transition [4] {to} the quasar's Burst Control Hatch in Purge mode.",
  "- ME stocking hatches in the 24 gas sockets; f 1.0 in the GUI.",
  "OpenComputers instead: bts_quasar.trim(\"gas\" or \"space\", units), purge(), setFeed(f), setField(b), getDisc()."]},
{"low":48172,"key":"galaxy_runs_itself","chapter":"uxv","x":720,"y":12,"size":24,
 "name":"{4}{l}{n}A Galaxy That Runs Itself","icon":"mte:28123","needs":["eyeT7T8"],"prereqs":[48171,48161],
 "tasks":[{"type":"checkbox"}],
 "desc":["The standard rig, proved with nothing touched from a fresh 64 SM quasar and a T6 magnetar to Eye T8:",
  "- Eye Field [1] {to} the Eye's Space valve (10 units into a Field hatch).",
  "- {b}J1{r}: Field [2] / [1] as 15 / 8 / 0 (two emitter covers), OR Galaxy Transition [4] {to} 15, OR Slots [8] {to} 15. {b}J2{r}: Field [4] / [8]. {b}J3{r}: Events [1] / [2], Slots [8] {to} its 75 % preset at T7.",
  "- Transition [4] {to} push the next core into a plinth bus; Transition [1] {to} push the next chart; Status [1] {to} stock the charge; Galaxy Transition [1] {to} the Eye's Burst Control Hatch in Begin mode.",
  "- Keep Mk I throats on the Eye's branch: they cap what reaches J1 at 24 Bu/t, so a growing quasar cannot flood the Eye.",
  "Analog instead: the sensor's P (inverted, {pm}30), W and {u+039b} straight to J1, J2, J3."]},
{"low":48173,"key":"scripting_quasar","chapter":"uxv","x":624,"y":12,"size":24,
 "name":"{4}{l}{n}Scripting a Quasar","icon":"OpenComputers:adapter","needs":["quasar"],"prereqs":[48171],"opt":[48171],
 "tasks":[{"type":"checkbox"}],
 "desc":["With OpenComputers, an Adapter against a P4 machine gives a component. Setters take one call a second (the field's step); a refused call returns nil and the reason.",
  "bts_quasar (getDisc, getBurst, setFeed, setGas, setField, trim, purge, getLinks, getMilestones, getDirective); bts_eye (getEvent, getUnlock, beginTransition, setFeed8, setMode, trim, getSlots); bts_mouth / bts_mirror (getLink, getThroat, getUpkeep, setFuel, isOpen); bts_junction (getFlow, setRatio, setGain, setOut3, setMode); bts_crown; bts_receptor; bts_homogeny; bts_reality; bts_kerr (getState().link)."]},
{"low":48174,"key":"quasar_power","chapter":"uxv","x":468,"y":312,"size":24,
 "name":"{4}{l}{n}Bursts into Power","icon":"mte:28124","needs":["crown"],"prereqs":[48150],
 "tasks":[{"type":"checkbox"}],
 "desc":["1 Bu/t = 10{sup13} EU/t. A 64 SM quasar at coils 3 makes about 2 Bu/t; it grows with its disc (to the coil cap) and the stream grows as M{sq}.",
  "Sizing: G0 = {u+03bb} {times} a{sq} {times} b{sq} {times} (M/64){sq}; never purging averages 1.11 {times} G0 {times} Y_q, purging at {Phi} 40 about 0.8. The NEI page \"Burst Thermaliser Crown\" lists each Mk's cap and EU/t."]}
]
```

**Appended lines for the H/R unit's quests** (an `appendText` edit or a line added to their `desc`):

- 48142 `homogeny_t2`: "A quasar link on its stirring ring speeds it up to 6 {times}: see {b}A Quasar-Held Vortex{r}."
- 48184 `homogeny_t3`: "Entanglement Medium needs a quasar link at {u+03c3} {ge} 1; without it the run waits."
- 48185 `reality_diffraction`: "Its portal takes 6 GRB Receptor Ports: faster with a strong quasar link, paused with a
  weak one, ×1 with none (see {b}Riding the Rift{r})."

**Chapter text.** The UXV chapter's `desc` loses "follow in a later update": "The {b}Quasar Engine{r} and the first
galaxies (T7, T8) are here: the quasar's bursts hold the Eye and power your base."

**Eternity.** The text is in "Eternity before the end" above: set `eternityOnErgomatter` to true.
