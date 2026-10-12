# P4 questbook contract

The questbook branch is not in `p4-quasar`'s base, so P4 units write their quest and tooltip content here. The merge
turns each section into quests (rules 14 and 15: every mechanic must be learnable in the questbook or the tooltip).
Numbers come from the engine (`machines/quasar/engine`, `eye/field`). If a number changes there, change it here too.

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
