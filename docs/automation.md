# Automation: every action, four ways (the Eye, the Kerr, and P4's quasar, conduit and hosts)

Status: **built** on master, 2026-10-13 (automation pass, build brief item 18). Question B-70 lists the judgement
calls. P4 (branch `p4-quasar`) adds the Quasar, the Conduit, Eye T7-T8 and the other burst hosts on the same hatch:
§7 below (design `designs/docs/quasar-p4.md` §8).

Your ruling: "make sure this is fully automatable with and/or items in game incluiding ae2, redstone, logic gates,
opencomputer or Steves Factory Manager ... hatches that provide detection may help e.g. the binary output from t7
water".

What this pass added:

| Part | What it is | Where |
|---|---|---|
| **Directive Hatch** (MTE 28127) | GT's Grade-7 water Degasser Control Hatch pattern: a 4-bit redstone code naming what the machine needs now. One hatch for every host; the set is chosen in its GUI. Control sets turn it into a redstone input for a setpoint. | `machines/directive/MTEHatchDirective` |
| **Plinth-bus pickups** (Eye) | An item in a plinth input bus can now do the two things that used to need a hand: the next transition core goes into escrow, and a star chart goes into the controller slot (the old target leaves through the ME output bus). | `MTEEyeOfHarmonyBTS.takeFromPlinthBuses` |
| **OpenComputers** (soft dependency) | Adapter components `bts_eye`, `bts_kerr`, `bts_kerr_module`, `bts_sensor`, `bts_directive`. Registered only if OpenComputers is installed. | `integration/oc`, `integration/BTSComputerApi` |
| **The proof** | An offline simulation (`DirectiveRigSim`) and a dev-server bench (`autorig`, `ocloop`). | §6 |

Nothing in the stability field, the star recipes or the transition rules changed. The Directive bits are read from
the same last-second state the Stellar Field Sensor Hatch reads (the 1 s lag the D-49 rigs were designed for).

---

## 1. The Directive Hatch

- **Recipe (SL-A01):** Assembler, 30 s at UIV: Hull (UIV), 2 Sensor (UIV), Emitter (UIV), 2 UIV circuits,
  16 Redstone Alloy plates, 288 L molten Infinity. UIV because the Kerr (UIV) needs it from day one; the Stellar
  Field Sensor Hatch stays a UMV Condensate Assembler part (it is Eye-only).
- **Where it goes:** any Eye **Sensor socket** (`n`, 8 on the core band, beside the Stellar Field Sensor Hatches) or
  any Kerr **utility socket** (`u`, 8 on the cage, beside the BHC Utility Hatch). No structure changed: both socket
  kinds already existed and fall back to their casing.
- **Set:** what the hatch reports (or controls). A hatch adopts its machine's first set when placed; the GUI cycles
  only through that machine's sets.
- **Output** (output sets): **Code** (strength = the code 0-15, the Degasser way), **Bit 1..4** (15 while that bit
  is on, else 0) or **Any** (15 while any bit is on), and **Invert** (15 − output). The signal goes out on every
  side; GT's Redstone Transmitter (Internal) and Advanced Wireless Transmitter (Internal) covers carry it (as on the
  Stellar Field Sensor Hatch). A **comparator** against the hatch reads the raw code whatever the output mode.
- **Window w** (1-60, default 10): the Eye's gauge band ±w; the Kerr's bolt reserve in minutes.
- **Timing:** the machine pushes the code once a second. If the pushes stop (machine unloaded or removed), the
  output falls to 0 after 3 s.
- **Bit values** are 1, 2, 4 and 8 (bit n = 2^(n−1)), and several bits can be on at once, as on the Degasser. The
  hatch tooltip writes each bit by its value ([1] [2] [4] [8]); this file names them bit 1-4 with the value after it.

### 1.1 Eye of Harmony sets

**Eye: Trims** — bit n = the star's n-th trim is wanted now (the gauge past ±w). This is the prototype's bang-bang
rig rule with the threshold set to w; on T1, T2 and T4 it asks for exactly the same trims every second (§6).

| Star | Bit 1 (1) | Bit 2 (2) | Bit 3 (4) | Bit 4 (8) |
|---|---|---|---|---|
| T1 Sol, T2 Sirius A | Helium (P > +w) | Hydrogen (P < −w) | — | — |
| T3 Arcturus (and the Shell Ignition) | Helium (P > +w or Θ < −w) | Hydrogen (P < −w) | Super Coolant (P > +w, Θ > +w, or a flash warned while Θ > −30) | — |
| T4 neutron star, T6 magnetar | Space (P > +w) | SpaceTime (P < −w) | Super Coolant (Θ > +w) | Purified Tengam (Ω > +w) |
| T5 Sirius B | DSSF (P or Θ > +w) | Raw Star Matter (Θ < −w) | Helium (P < −w) | — |

**Eye: Status**

| Bit | Meaning | Typical action |
|---|---|---|
| 1 (1) | **Buffers short**: the next cycle's upkeep (or this cycle ran short), a dark Eye's ignition (10 cycles and the seed cloud), a waiting Sirius B batch, the magnetar's dwarf-matter top-up, or, with the next core in escrow, the next transition's fluids | stock the gas / stellar sockets |
| 2 (2) | **Ignition charge wanted**: a dark Eye would light its chart's star now (T4: relight Arcturus) but no Stellar Ignition Charge is in a plinth bus | push one charge |
| 3 (4) | **Event warned / fifth trim**: T3 a helium flash is warned (open the bigger Super Coolant valve); T6 the Antipolarization Agent is wanted (Π past ±w or a flare warned). During the Magnetar Merger: the imprint asks above Π +66, the release above +30 | the event valve; on T6 **while Transition bit 2 is on, a 1 unit/s (100 L/s) Agent valve** instead of the 5-unit one (the imprint must keep Π inside +60..+70: above +70 the field is Critical and drains Integrity 2/s, and 5 units take 20 off Π at once) |
| 4 (8) | **Alarm**: the Eye is not formed, or the star is Critical or Breach, or Integrity is below 50 | lamp, alarm, backup |

**Eye: Transition**

| Bit | Meaning | Typical action |
|---|---|---|
| 1 (1) | **Ready**: everything the next transition needs is in place except the chart (milestones, core in escrow, band, compression, fluids, star Stable) | push the next star chart into a plinth bus |
| 2 (2) | **Running**: a transition is running, or the next chart waits (PENDING) | a lamp; hold anything that must not change |
| 3 (4) | **Next core wanted**: this tier's milestones are met and the next core is not in escrow | push the core into a plinth bus |
| 4 (8) | **Tachyon wanted**: the Core Collapse is fast-forwarding (it doses up to 100 L/s from the Field sockets), or the time dial is set above 1 and last second could not pay for it | open the Tachyon Rich Temporal Fluid valve |

**Eye control sets** (the hatch reads redstone instead of emitting): **Eye: control the time dial** (1 → 1.00,
15 → 3.00, in 0.25 steps; still capped by the Time Dilation generators) and **Eye: control the HSM feed** (1 → 0,
15 → 1.5 L/s). Strength 0 leaves the GUI's value.

### 1.2 Kerr Ergosphere Engine sets

**Kerr: Hole**

| Bit | Meaning | Typical action |
|---|---|---|
| 1 (1) | **Hole closed** (the KEE is formed) | push a Black Hole Seed into a bolt bus (Auto opens it) |
| 2 (2) | **Mass below setpoint** by more than 1 SM | more bolts or faster rails; an alarm |
| 3 (4) | **Bolts short**: an open hole's bolt stock lasts less than w minutes at the current bleed (a closed one has none) | export bolts into a bolt bus |
| 4 (8) | **Mass above setpoint** by more than 1 SM (it bleeds down by itself) | hold modules, or nothing |

**Kerr: Status** — 1 containment power short, 2 no Black Hole Seed in a bolt bus while closed, 4 unstable (stability
below 100 %), 8 not formed.

**Kerr: control the mass setpoint** — redstone 1 → 2 SM, 15 → the tier's top (64 SM at T1, 512 SM at T2), linear,
whole SM.

---

## 2. Every action, four routes

"Plinth bus" = an input bus in an Eye gas socket (`h`). "Field hatch" = a plain input hatch in a Field socket
(`f`; ME stocking hatches are walked but never drained there, R-15). Hatches are GT hatches, so AE2 buses, AE2
fluid buses (AE2FC), Steve's Factory Manager item and fluid nodes, conveyor and pump covers all work on them.

### 2.1 The Eye

| Action | AE2 | Redstone + logic gates | OpenComputers | Steve's Factory Manager |
|---|---|---|---|---|
| **Trims** (hold the field) | A fluid export bus with a redstone card per trim, into a Field hatch, driven by a Trims hatch in Bit n mode | Trims hatch in Bit n mode (one hatch per trim) or Code mode decoded with comparators; each bit drives a GT Machine Controller cover ("enable with redstone") on the tank that feeds the Field hatch; that tank's Fluid Regulator cover, set to "Conditional", runs only while the tank is enabled (§4.1) | `eye.getDirective("EYE_TRIMS")` or `eye.getField()`, then a transposer moves the trim | A fluid node into the Field hatch with a redstone condition on the hatch (SFM conditions read analog ranges, so one Code-mode hatch is enough) |
| **Event valve** (T3 flash, T6 Agent) | Export bus on Status bit 3 | Status hatch, Bit 3 mode, a second regulator | `getDirective("EYE_STATUS").bits` | Fluid node on bit 3 |
| **Upkeep** (H, He, RSPM, ESPM, SpaceTime, HSM feed, WDM/BDM, Agent) | ME stocking input hatches in the gas / stellar sockets (they drain into the buffers, up to each buffer's intake cap, §2.3), or level maintainers | Status bit 1 → an export bus or pump | `getBuffers()`, `getIntakeCaps()`, `getDirective("EYE_STATUS")` | Fluid node into the gas hatches on bit 1 |
| **Ignition** (Stellar Ignition Charge) | ME input bus in a plinth socket holding charges, or an interface pushing one on Status bit 2 | Status bit 2 → a conveyor cover or export bus | `getState().waitReason`, transposer into the bus | Item node into the bus on bit 2 |
| **Transition core into escrow** | Interface / export bus pushes the core into a plinth bus (new: the Eye takes it) | Transition bit 3 → export bus | `getTransition().coreWanted`, transposer | Item node on bit 3 |
| **Load the next star chart** (starts the transition; also switching stars) | Export bus pushes ONE chart into a plain plinth bus on Transition bit 1 (redstone card on **Signal pulse**, so one rising edge pushes one chart); the old one comes back through the ME output bus. A copy of the chart already loaded is sent to the ME output bus too, and no chart is swapped while a transition runs or waits (B-72) | Transition bit 1 → export bus / conveyor | `getTransition().ready`, transposer | Item node on bit 1 |
| **Tachyon** (the Core Collapse's dose, 100 L/s; the time dial's payment) | Export bus of Tachyon Rich Temporal Fluid into a Field hatch on Transition bit 4 | Transition hatch, Bit 4 mode → regulator | `getDirective("EYE_TRANSITION").bits` | Fluid node on bit 4 |
| **Time dial** | Static; or a level emitter → a control-set Directive Hatch (two presets) | Control-set hatch: redstone 1-15 → dial | `eye.setDial(v)` | Redstone emitter (analog) into a control hatch |
| **T4 HSM feed** | As the dial | Control-set hatch: 1-15 → 0-1.5 L/s | `eye.setFeed(v)` | As the dial |
| **Outputs** | ME output bus / hatch (the only output path) | — | — | — |
| **Alarm** | — | Status bit 4 → lamp, siren, backup | `getField().zone`, `integrity` | Redstone condition on bit 4 |

### 2.2 The Kerr

| Action | AE2 | Redstone + logic gates | OpenComputers | Steve's Factory Manager |
|---|---|---|---|---|
| **Bolts** | Export bus (or interface) into a plain bolt bus on Hole bit 3 (ME stocking buses are refused: the governor cannot take from shadow stacks) | Hole bit 3 → export bus / conveyor | `kerr.getState().boltStockSeconds`, transposer | Item node into the bolt bus on bit 3 |
| **Open the hole** | Push a Black Hole Seed into a bolt bus on Hole bit 1; Auto (on by default) opens it | Hole bit 1 → export bus | `kerr.open()` (needs a Seed in a bolt bus) | Item node on bit 1 |
| **Collapse** | Push a Collapser into a bolt bus (Auto) | Any signal you choose → export bus | `kerr.collapse()` (needs a Collapser) | Item node |
| **Mass setpoint** | Level emitter → control-set hatch | Control-set hatch: 1-15 → 2 .. 64 / 512 SM | `kerr.setSetpoint(n)` | Analog redstone emitter → control hatch |
| **Containment power** | — (energy / laser hatches) | Status bit 1 → backup generator | `getState().powerOk` | Redstone condition on Status bit 1 |
| **Kerr modules** | Standard GT hatches: ME stocking buses and hatches, ME output | GT covers as on any multi | `bts_kerr_module.getState()` (link, parallel, speed, progress) | Item / fluid nodes |

### 2.3 Hatches with AE2 and Steve's Factory Manager (what was checked)

- The Eye's gas and stellar sockets take ME stocking input hatches (they drain into the buffers every second).
  Each buffer fills only to its **intake cap** (B-108; the scanner shows held / cap, OpenComputers
  `getIntakeCaps()`): for a star, max(ignition need, 20 cycles of upkeep) plus any transition the Eye could begin; for
  a planet, 2 runs at full parallel. A fluid with no cap is not pulled. Before B-108 one stocking hatch emptied the
  whole network into the buffers within a second.
- One stocking hatch per gas is enough, and it still takes up to the cap (1 G L of each gas for a T1 Sol) in its
  first second. Keep the network's stock above the cap, or put the trim buses on their own network or behind a small
  Field hatch: a trim bus faster than the Eye's trim draw stockpiles trim in a big Field hatch, which the Eye keeps
  draining after the bit goes off (an LV hatch, 16,000 L, holds about a second of it).
- The Field sockets take plain input hatches only in practice: an ME stocking hatch there is walked but never drained
  (R-15), so trims go in through an export bus, a pump, a regulator cover or an SFM fluid node.
- Plinth buses: ME input buses work for charges and cores; star charts are picked up from **plain** buses only (an ME
  stocking bus would show its configured chart every second). Put the redstone card of the chart and core export
  buses on **Signal pulse**: a bit stays on until the Eye's next pickup a second later, so "active with signal" pushes
  2-4 items. Extra copies of the loaded chart go back out through the ME output bus (B-72); a spare core stays in the
  bus until the next transition wants it.
- Kerr bolt buses: plain input buses only (as before).
- The Directive Hatch and the Stellar Field Sensor Hatch emit redstone on every side and accept GT redstone covers,
  so an AE2 export bus with a redstone card, a GT cover, an SFM Redstone Receiver, a comparator or OpenComputers'
  Redstone I/O can all sit against them (or read them over GT wireless redstone).

---

## 3. OpenComputers

Soft dependency: compiled against OpenComputers' API, registered only when OpenComputers is installed. Put an
**Adapter** against the controller (or a hatch). Calls run on the server thread and go through the GUI's own setters,
so they have the same clamps and safety rules as the GUI. There is no setter a player does not have.

| Component | Methods |
|---|---|
| `bts_eye` | `getState()` (tiers, star, state, transition and progress, dial / feed setpoints, upkeep r, wait reason, last event and failure), `getField()` (P, theta, omega, pi, shield, integrity, stability, zone, yield, warning), `getBuffers()`, `getTransition()` (milestones, core, escrow, ready, missing), `getDirective(set [, w])`, `setDial(v)`, `setFeed(v)` |
| `bts_kerr` | `getState()` (open, mass, setpoint, bleed, bolts a tick, stability, power, bolt stock and seconds, seeds, collapsers, modules, auto), `getDirective(set [, w])`, `setSetpoint(sm)`, `open()`, `collapse()`, `setAuto(on)` |
| `bts_kerr_module` | `getState()` (link, mass, parallel, speed, active, progress) |
| `bts_sensor` | `getState()`, `setChannel(name)`, `setThreshold(v [, "gt"/"lt" [, analog]])` |
| `bts_directive` | `getState()`, `getCode()`, `setSet(name)`, `setOutput(mode)`, `setWindow(w)`, `setInvert(on)` |

A Kerr ramp script (the loop §6 simulates):

```lua
local kerr = require("component").bts_kerr
while true do
  local s = kerr.getState()
  if not s.open then kerr.open() end
  if s.open and s.stability >= 1 and math.abs(s.mass - s.setpoint) <= 1 and s.setpoint < 512 then
    kerr.setSetpoint(s.setpoint + 8)
  end
  os.sleep(60)
end
```

An Eye trim loop (one transposer per trim fluid; a sketch):

```lua
local eye = require("component").bts_eye
while true do
  local d = eye.getDirective("EYE_TRIMS")
  for name, wanted in pairs(d.bits) do
    if wanted then --[[ move one valve's worth of `name` into a Field hatch ]] end
  end
  os.sleep(1)
end
```

---

## 4. The minimal rigs

- **Eye T1 / T2:** two Trims hatches (Bit 1, Bit 2) → two valves (Helium, Hydrogen), each a tank with a Machine
  Controller cover and a Conditional Fluid Regulator cover (§4.1). ME stocking hatches in
  the gas sockets, an ME input bus with charges. That is all: the star relights itself after a loss.
- **Eye T3:** add a third Trims hatch (Bit 3, Super Coolant) and a Status hatch (Bit 3) on a second Super Coolant
  regulator for the flash.
- **Eye T4:** four Trims hatches (Space, SpaceTime, Super Coolant, Tengam). The HSM feed is a setpoint.
- **Climbing tiers unattended:** a Transition hatch in Code mode, decoded with comparators (or SFM's analog
  conditions): bit 3 pushes the core, bit 1 pushes the next chart, bit 4 holds the Tachyon valve (the Core Collapse,
  and the time dial if you set it above 1).
- **Kerr:** one Hole hatch in Code mode: bit 3 (bolts short) and bit 1 (closed) each switch an export bus.

### 4.1 Built for real: the automation photo rigs

Two of these rigs were built with real blocks in a dev client and run with nothing else touching the machines
(dev-only harness `rendertest/AutoRig`, `-Dbts.rendertest=true -Dbts.rendertest.cases=automation`; question B-106 has
the numbers). What the build taught, for players:

- **A GT valve on redstone is two covers.** On GT 5.09.54 the Fluid Regulator cover has no "enable with redstone"
  mode: its "Conditional" mode runs only while the block it sits on is allowed to work. So the valve is a tank (or
  any GT block between the supply and the hatch) with a **Fluid Regulator** cover facing the hatch (Conditional, the
  trim's rate: Sol's 10 units/s of Helium is 500 L/t) and a **Machine Controller** cover ("enable with redstone")
  on another face, where the redstone arrives.
- **Dust and GT blocks.** Dust sits on a GT machine and powers the cover on that top face; a GT machine is not a
  solid redstone block, so dust cannot climb or step down its side (use a quartz or stone block there), and dust
  only points into a GT face that carries a cover.
- **Eye T1 (Rig A):** Trims Bit 1 → Helium valve, Trims Bit 2 → Hydrogen valve, Status Bit 1 → both upkeep valves
  at the gas hatches (a 40-block redstone staircase with repeater landings from the band to the plinth), four
  Stellar Ignition Charges in the plinth bus. From empty buffers the upkeep valves stocked the ignition (600 M L of
  each gas at 3 M L/s), the Eye lit itself, and the trims held the field.
- **Kerr T1 (Rig B):** Hole Bit 3 (bolts short, window 1 minute) → an AE2 export bus with a redstone card and three
  acceleration cards (64 bolts an operation; one card, 8, could not feed the hole's climb from 8 to 16 SM and it
  evaporated), Hole Bit 1 (closed) → an export bus with a redstone card holding a Black Hole Seed. The export bus's
  cable block takes a **facade** on top so the dust in front of the Directive Hatch can stand on it and power it.

### 4.2 Testing aid: the time warp (`/btswarp`, a cheat)

A rig that holds a star for hours is slow to test. `/btswarp <N>` makes the whole server run up to N ticks for every
real one, through the same code as normal play: AE2's grids and buses, GT machines and covers, redstone, the Eye, the
Kerr, the quasar, the conduit and the bench rigs all run N times faster. Nothing is skipped or shortcut per machine,
so a rig that works under the warp works in real time (measured below). Op only (permission level 2: in single
player, cheats on), off by default, off again when the server stops.

| Command | What it does |
|---|---|
| `/btswarp <2-1000>` | On: up to N - 1 extra server ticks after every real tick. |
| `/btswarp status` | Requested and achieved multiplier (game ticks per real second / 20, over the last 3 s and overall), game time against real time, how often a tick hit the budget, the cost of one extra tick. |
| `/btswarp off` | Off (also `0` or `1`). |

- **It never makes the server fall behind.** The extra ticks stop for a tick once that tick has used 45 ms of its
  50 (`-Dbts.warp.tickms`), so the server keeps its 20 real ticks a second. An N the machine cannot hold gives the
  most it can, and `status` says what it got.
- **Not in the extra ticks:** the network tick, autosaves and the time-sync packet run once per real tick, so with
  the daylight cycle on the client's sky jumps once a second.
- **Measured** (dev client, RTX 4090 machine): an extra tick of a world with Rig C costs about 0.4 ms. x20 and x40
  held with 0 budget hits. With the whole conduit rig (a quasar, two mouths, two mirrors, a junction, two crowns),
  the three H/R machines, an Eye T8 and a second quasar loaded, an extra tick cost 0.6 ms and x40 held (2 % of ticks
  at the budget).
- **Same outcome as real time.** Rig C (the AE2 Eye T1 of §4.1), 444 s of game time from the supply opening, once in
  real time and once at x20 (22 s): both lit at t = 40 s, both 77.2 % Resonant / 22.8 % Stable / 0 worse, Integrity
  100 throughout, mean Y 1.193, and the same 357,708 L of Raw Star Matter in the network.
- **The live rigs.** `/btseye star x y z liverig on` runs the standard gate rig of §4 once a game second on the Eye's
  own Directive codes (the `autorig <s>` bench runs the same rig for s seconds inside one command, so nothing moves on
  screen). Under the warp a kit Eye set back to a dark T1, with its milestones set done, climbed T1 → T6 by itself:
  9,442 s of game time (Mass Infusion, Shell Ignition, the Core Collapse with Tachyon for 940 s, the 7,200 s First
  Cooling, the Magnetar Merger) in 236 s of real time at x40, 1 charge, 5 cores and 5 charts pushed.

---

## 5. Where it is learnable in game (rule 14)

- The Directive Hatch tooltip carries every bit table above, the output modes, the window and the control sets.
- The Eye and the KEE tooltips name the hatch, their sets, the plinth-bus pickups and the OpenComputers component.
- Their scanners list each Directive Hatch with its set, code and bits by name; the hatch GUI (B-99) shows, in a
  readout box, the set and its machine, the four bits as lamps by name (lit while on), the code and the redstone it
  drives (on a control set: the input and the setpoint it drives), with the Set / Output / Invert / Window controls
  under it, each with a tooltip.
- The questbook section below.

### 5.1 For the questbook branch (copy into `designs/questbook.js` / BetterQuesting)

```js
// Kerr chapter, after kerr1 (Kerr · T1). A retrieve quest.
E("kdir","Tell me what you need","Directive Hatch",780,60,24,["kerr1"],
  "The <b>Directive Hatch</b> puts out what its machine needs now as a 4-bit redstone code, like GT's Grade-7 water Degasser Control Hatch. In a Kerr utility socket, set <b>Kerr: Hole</b>: 1 = closed, 2 = below setpoint, 4 = bolts short, 8 = above setpoint. Wire the 4 (Bit 3 mode) to an export bus with a redstone card that moves bolts into a bolt bus, and the 1 to one that pushes a Black Hole Seed. A comparator reads the code; Steve's Factory Manager reads it as an analog range. A control set turns the hatch around: redstone 1-15 sets the mass setpoint.",
  { ours:true, label:"Automation", tasks:[{type:"retrieve",item:"Directive Hatch",count:1}] }),
// Eye chapter, after eoh1. A checkbox quest (the routes are a choice).
E("edir","An Eye that runs itself","Directive Hatch",700,200,24,["eoh1","kdir"],
  "Directive Hatches fit the Eye's Sensor sockets. <b>Trims</b>: the star's trims, by value (T1/T2: 1 Helium, 2 Hydrogen; T4: 1 Space, 2 SpaceTime, 4 Super Coolant, 8 Tengam) - one hatch per bit in Bit mode, each on a GT valve (a Machine Controller cover, enable with redstone, plus a Fluid Regulator cover set to Conditional, on the tank feeding a Field hatch) or an export bus with a redstone card. <b>Status</b>: 1 buffers short, 2 ignition charge wanted, 4 event (T3 flash: more Coolant; T6: the Agent), 8 alarm. <b>Transition</b>: 1 ready (push the next chart into a plinth bus), 2 running, 4 core wanted (push it into a plinth bus), 8 Tachyon wanted (open the Tachyon Rich Temporal Fluid valve). The plinth bus now takes cores into escrow and charts into the controller slot.",
  { ours:true, label:"Automation", tasks:[{type:"check"}] }),
// Optional, after edir: OpenComputers.
E("eoc","Scripting a star","Adapter",820,200,24,["edir"],
  "With OpenComputers installed, an Adapter against the Eye gives the component bts_eye (getState, getField, getBuffers, getTransition, getDirective, setDial, setFeed); against the Kerr, bts_kerr (getState, setSetpoint, open, collapse, setAuto); against a hatch, bts_directive / bts_sensor. Same rules as the GUI.",
  { ours:true, opt:["edir"], label:"Optional", tasks:[{type:"check"}] }),
```

---

## 6. The proof

### 6.1 Offline (`DirectiveRigSim`, plain JDK)

`java -cp build/classes/java/main com.nateyburr.beyondthestars.machines.directive.DirectiveRigSim 4 5` runs each
case for 4 hours on 5 seeds. The Eye's rig sees only Directive bits, a second late; each bit opens one fixed-rate
valve (the prototype's bang-bang rates). It is compared seed for seed with the prototype's bang-bang rig.

| Case | Directive gate rig | Prototype bang-bang rig |
|---|---|---|
| T1 Sol | 0 stars lost, 96.6 % Resonant, mean Y 1.242 | 0 lost, mean Y 1.242 (same trims every second) |
| T2 Sirius A | 0 lost, 47 % Resonant / 53 % Stable, Y 1.088 | 0 lost, Y 1.088 (same trims every second) |
| T3 Arcturus | 0 lost, 68 % Resonant, Y 1.167 | 0 lost, Y 1.170 |
| T4 neutron star | 0 lost, 59 % Resonant, Y 1.148 | 0 lost, Y 1.148 (same trims every second) |
| T5 Sirius B | 0 lost, 93 % Resonant, Y 1.233 | 0 lost, Y 1.233 |
| T6 magnetar | 0 lost, 58.5 % Resonant, Y 1.146 | 0 lost, Y 1.146 |
| Core Collapse T3 → T4 (begun on Transition bit 1, Tachyon on bit 4) | 5 / 5 settled | 5 / 5 settled |

The Kerr's rig reads the Hole set (bit 3 → a bolt export bus, bit 1 → one Seed), with the hole physics of
`tickHole` and the bits from `KerrDirective` (the function the hatch uses):

| Case (4 h) | Result |
|---|---|
| T1, SpaceTime bolts, Mk I rails, setpoint 32, 64 bolts/s export | 0 evaporations, 1 Seed, ends 32.04 SM |
| T1, Neutronium bolts, Mk II rails, setpoint 16, 256 bolts/s | 0 evaporations, ends 16.01 SM |
| T2, Infinity bolts, Mk III rails, setpoint 512, 64 bolts/s | 0 evaporations, ends 512.06 SM |
| T1 from a closed hole, OpenComputers-style setter loop 8 → 64 SM | reaches 64 SM, 0 evaporations; setSetpoint(100000) clamps to 64 |
| T2 from a closed hole, setter loop 8 → 512 SM | reaches 512 SM, 0 evaporations |

### 6.2 Dev server

Headless `runServer` (RC-2 dependencies plus OpenComputers), driven from the console; the full table is in question
B-70.

- **The Eye, T1 to T4 with no hand on it.** The rig sees only the three Directive Hatches' codes. A dark Eye with
  empty buffers was stocked (Status bit 1), charged (bit 2) and lit. Each transition got its core (Transition bit 3)
  and its chart (bit 1) through the plinth bus. The Mass Infusion, the Shell Ignition (its forced flash ridden on
  Status bit 3) and the Core Collapse (Tachyon on Transition bit 4 for exactly its 940 s of fast-forward) all
  completed. The milestones were set by command.
- **Held and recovered.** The neutron star held for 4 h with 0 losses. With the valves held shut it was lost; the rig
  then relit Arcturus and re-ran the Core Collapse, with no core.
- **The Kerr from empty buses.** The rig put in one Seed and the bolts; the hole stayed open and on the setpoint for
  an hour.
- **Real redstone.** The world reads the hatch's output at its face (and its inverse). A redstone block on a
  control-set hatch moved the Kerr setpoint to the top.
- **OpenComputers.** The calls behind the components clamp and refuse as the GUI does. OpenComputers' own driver
  registry hands an Adapter our components on the Eye, the KEE, the hatches and the modules.

Bench commands:

- `/btseye star x y z directive | autorig <s> [cut <k>] | ocloop`
- `/btseye star x y z intake setup [G] | intake | eu`: the intake-cap rig (B-108), an ME stocking hatch in a gas socket on
  a network of G gigalitres per gas with a second consumer (fluid export bus into a Super Tank), and its readout
- `/btskerr autorig x y z [s] [bolts/s] [empty] | ocloop x y z | ctrl x y z on|off|out | ocprobe x y z`

---

## 7. P4: the Quasar, the Conduit, Eye T7-T8 and the other hosts

Status: **built** on `p4-quasar`, 2026-10-13. Design: `designs/docs/quasar-p4.md` §8 and R.5. Judgement calls:
P4-74 to P4-82 in `singularity-ladder-questions.md`.

### 7.1 Detection and control hatches

**Directive Hatch sets** (the same hatch, MTE 28127). Each code adds its bits: [1] [2] [4] [8]. The window w is set in
the hatch GUI; the standard rig uses w 5.

| Set (where) | [1] | [2] | [4] | [8] |
|---|---|---|---|---|
| **Quasar: Disc** (quasar `d` socket) | E < −w: pulse the feed gas | E > +w: Space trim | Φ at the purge setpoint: purge | Alarm: blow-out or re-forming, no coil power, a jet venting, a conduit element closed |
| **Eye: Field** (T7+, `d` or `n`) | P > +w: Space trim | P < −w: more bursts to the Eye | W > +w: aim high | W < −w: aim low |
| **Eye: Events** (T7+) | Λ > +w: more to the halo | Λ < −w: less to the halo | A countdown is running (collapse, pass, crossing, merger) | That event's sign is − |
| **Eye: Galaxy Transition** | Ready: begin (milestones, core, charge, chart in, Stable) | A galaxy transition is running | Purge now (the collapse assist) | Hold the regulator's purges |
| **Eye: Slots** | A planet or companion slot is idle | A companion's upkeep is short | The last planet run lost parallels | Link test fire wanted |

Rules that matter for an unattended rig:

- **Transition [4]** (next core wanted) on T6 and T7 comes when every milestone **but the link** is met (P4-76). The
  link's test fire needs the core in escrow.
- **Slots [8]** at T6 means "J1 all to the Eye" (the Stability Link). At T7 it means "J1 all to the Eye, J3 to its 75 %
  halo preset" (the Heavy Link). At T7 it waits until the linked quasar's mean stream reaches 6 Bu/t, about 130 SM at
  coils 3 (P4-79); the Galaxy panel says so.
- In planets mode, Field [4] / [8] and the Events set stay 0.

**Burst Control Hatch** (MTE 28126), a redstone input:

| Mode | Where | What the signal does |
|---|---|---|
| Purge | quasar `c` (cap rims) | A rising edge purges the flux (refused below Φ 20) |
| Field b | quasar `c` | b = 0.5 + (coil tier − 0.5) × s/15 |
| Junction split | a Junction | s in out 1's share: c + k(s/15 − 0.5) |
| Begin | Eye `d` | A rising edge begins the galaxy transition while Galaxy Transition [1] is on; ignored otherwise |

**Stellar Field Sensor Hatch** (28116): the quasar channels (E, Φ, λ, a, M, Bu/t, the eruption warning, the seconds to
the next eruption, the best host's σ, the link streak %) and the Eye's W, Λ, β, link streak %, link 60 s mean, slot progress and countdown.
Analog with the window w, or a threshold (15 above / below), with invert.

### 7.2 Every action, four routes (checked against the code)

"Checked" names the code that does it.

| Action | AE2 | Redstone + logic gates | OpenComputers | Steve's Factory Manager | Checked |
|---|---|---|---|---|---|
| **Feed gas** (the disc's E) | ME stocking hatches in the 24 Rim Gas Injector sockets (the quasar keeps a 60 s Accretion Buffer, refilled every tick); f is a GUI setpoint | — | `quasar.setFeed(f)`, `setGas(name)` | Fluid node into a gas hatch | `MTEQuasarEngine.refillBuffer`, gas element takes any input hatch |
| **Gas trim** (E low) | Fluid export bus with a redstone card on Disc [1] into a **plain** trim hatch | Disc [1] (Bit 1 mode) → a Fluid Regulator cover, "enable with signal" | `quasar.trim("gas", 10)` while `getDisc().E < −5` | Fluid node into the trim hatch on bit 1 | trim element plain hatches only; `requestTrim` (P4-77) |
| **Space trim** (E high) | Export bus on Disc [2] | Disc [2] → regulator | `quasar.trim("space", 10)` while E > 5 | Fluid node on bit 2 | as above |
| **Purge** (Φ) | — (Disc [4] goes straight to the hatch) | (Disc [4] AND NOT Galaxy Transition [8]) OR Galaxy Transition [4] → the quasar's Burst Control Hatch (Purge) | `quasar.purge()` (false below Φ 20) | Redstone emitter pulsed on bit 3 | `MTEHatchBurstControl` rising edge; `requestPurge` |
| **Coil field b** | Level emitter → Burst Control Hatch in b mode (two presets) | Lever or analog source → b mode | `quasar.setField(b)` | Analog emitter | b mode in `pollBurstHatches` |
| **J1** (Eye : crown) | Two-level: Field [2] / [1] → 15 / 8 / 0 into J1's Burst Control Hatch; OR Galaxy Transition [4] → 15; OR Slots [8] → 15 | Sensor P (analog, inverted, ±30) straight to J1 | `junction.setRatio(c)` from `eye.getField().P` | Analog emitter, or the two-level rule | `ConduitNet` split, `BurstRouting.share` |
| **J2** (aim W) | Two-level: Field [4] / [8] | Sensor W straight to J2 | `setRatio` from `getField().W` | as J1 | as J1 |
| **J3** (halo Λ, T8) | Two-level: Events [1] / [2]; Slots [8] → its 75 % preset at T7 | Sensor Λ straight to J3 | `setRatio` from `getField().L` | as J1 | as J1 |
| **Eye Space trim** (P, T7+) | Export bus on Field [1] into a Field hatch (one valve, 10 units) | Field [1] → regulator | `eye.trim("space", 10)` while P > 5 | Fluid node on bit 1 | `EyeStarRuntime` trims; `requestTrim` |
| **Ergomatter feed f₈** (T8) | ME stocking hatch in a gas socket; f₈ is a GUI setpoint | — | `eye.setFeed8(f)` (lower it on `getEvent().kind == "crossing"`) | — | `setF8` |
| **Throat fuel** | ME stocking hatches in each element's fuel hatches; a level emitter on SpaceTime / Ergomatter orders crafting | Disc [8] → a lamp or a backup | `mouth.getUpkeep()`, `setFuel(mode)` | Fluid node on a level condition | `MTEConduitElement` upkeep |
| **The core** | ME interface / export bus pushes it into a plinth input bus on Transition [4] | Transition [4] → export bus | `eye.getTransition().coreWanted`, transposer | Item node on bit 3 | `takeFromPlinthBuses` → escrow |
| **The charge** (WDM + BDM, or Ergomatter 9,000 L) | Level maintainers; Status [1] says short | Status [1] → export bus | `getBuffers()` | Fluid node on bit 1 | `transitionFluidsShort` |
| **The galaxy chart** | Export bus pushes ONE chart into a plain plinth bus on Transition [1] | Transition [1] → conveyor | transposer on `getTransition().ready` | Item node on bit 1 | plinth-bus chart pickup |
| **Begin** | — | Galaxy Transition [1] → the Eye's Burst Control Hatch (Begin) | `eye.beginTransition()` | Redstone emitter on bit 1 | `requestBegin` |
| **Link test fire** | Slots [8] → a toggle bus feeding J1 15 and J3's 75 % preset | Slots [8] → both Burst Control Hatches | `junction.setRatio` on both | Redstone on bit 4 | `testFireWanted` |
| **Planets and companions** | ME interface keeps planet items / charts in the slot bus | Slots [1] → a conveyor cover | `eye.setMode()`, `getSlots()`, transposer | Item node on bit 1 | slot bus `p` |
| **Crown power** | Nothing: the EU goes to the owner's wireless network | Disc [8] → backup generator | `crown.getPower()` | Redstone on bit 4 | `MTEBurstThermaliserCrown` |
| **Other hosts** (Kerr, Homogeny, Reality Diffraction) | J1's out 3 at a fixed share (no signal) | A Junction at a fixed ratio | `quasar.getLinks()`; `kerr.getState().link`, `homogeny.getState()`, `reality.getState()` | — | `BurstHost` per host |

**The only manual steps:** making the Quasar Seed and opening the hole (the controller slot and Open), pairing each
Mouth pair with a data stick, placing the hatches, and setting f 1.0 once if you want the links (P4-81).

### 7.3 OpenComputers (P4 components)

Soft dependency, as in §3. Every setter and action takes **one call per 20 ticks** per machine and method; a refused
call returns nil and "rate limited: one call per 20 ticks (1 s, the field's step)".

| Component | Methods |
|---|---|
| `bts_quasar` | `getDisc()` (state, E, phi, lambda, a, M, cap, integrity, zone, Yq, stability, b, power), `getBurst()` (total Bu/t and totalEUt, top, bottom, docks, vented), `getFeed()`, `setFeed(f)`, `setGas(name)`, `setField(b)`, `trim(kind, units)`, `purge()`, `getLinks()` (host, Bu/t, σ, streak), `getMilestones()`, `getDirective(w)` |
| `bts_eye` (T7+ additions) | `getField()` adds W, L, beta, the gammas and D; `getEvent()`, `getUnlock()` (kind, streak, best, mean, frac, linked, source, ready, testFireWanted, quasarStream, testFireStream, testFireShare, j1Limit, supermassiveLink {streak, best, held, mean, need, sourceMass, sourceLattice, sourceQualifies}: P5's T9 -> T10 data), `beginTransition()`, `setFeed8(f)`, `setMode(mode)`, `trim(kind, units)`, `getSlots()` |
| `bts_mouth`, `bts_mirror` | `getLink()`, `getThroat()`, `getUpkeep()`, `setFuel(mode)`, `isOpen()` |
| `bts_junction` | as a mouth, plus `getFlow()` (with `limit`), `setRatio(c)`, `setGain(k)`, `setOut3(p)`, `setLimit(bu)` (out 1's limit, 0 = none; the Eye's J1: 7.5 Bu/t per parallel, P4-88), `setMode(mode)` |
| `bts_crown` | `getPower()`, `getStream()`, `getMk()` |
| `bts_receptor` | `getDelivery()` (total, bySource) |
| `bts_kerr` | `getState()` adds setpointHeld, bleedFactor and link {ports, sigma, sigmaLast, delivered, mean, linked, source, paused, line} |
| `bts_homogeny` | `getState()`: tier, parallels, speed, demand, Entanglement open, link |
| `bts_reality` | `getState()`: burst-driven, speed, anchors, link |
| `bts_directive` | `getBits()` (code, on[1..4], names[1..4]); `setSet` lists the P4 sets |

A quasar disc loop (what the Disc valves do, by script):

```lua
local q = require("component").bts_quasar
while true do
  local d = q.getDisc()
  if d.E < -5 then q.trim("gas", 10) elseif d.E > 5 then q.trim("space", 10) end
  if d.phi >= 40 then q.purge() end
  os.sleep(1)
end
```

### 7.4 The standard rig (what the proof wires)

- **Quasar:** a Disc Directive Hatch (Code mode, w 5). Disc [1] → the feed-gas valve and Disc [2] → the Space valve,
  10 units a second each, into plain trim hatches. ME stocking hatches in the gas sockets. f 1.0.
- **Purge:** (Disc [4] AND NOT Galaxy Transition [8]) OR Galaxy Transition [4] → the quasar's Burst Control Hatch
  (Purge). That is one AND-NOT gate and one OR gate.
- **The collapse cue's timing (P4-95, B-118).** Galaxy Transition [4] comes on 2 s before the collapse (overfeed
  seconds 298 and 299, one rising edge); the hold [8] runs from T−80 s to T−3 s. Real wiring has hops: the Directive
  Hatch emits a tick after the Eye's second, a cover or a wire carries it, the Burst Control Hatch latches the edge,
  and the quasar purges on its own next second, whatever its phase against the Eye's. An eruption lasts 3 quasar
  seconds, so any wiring that answers within about a second and a half lands it on the collapse second. Wireless is
  fine: a GT Advanced Redstone Transmitter (Internal) on the Galaxy Transition hatch (output Bit 3) and a Receiver
  (Internal) on the Burst Control Hatch, same frequency, is what the live rig fits.
- **How much it cancels is the quasar's.** The collapse second needs D₇ + 12 = 13.5 Bu/t on the Eye to cancel the
  whole +120 shock; an eruption gives about 3 × G0·Yq × Φ/100 on top of the stream. A fresh 64 SM hole with no spin
  gives 1-2 Bu/t and cannot hold the collapse; the Stability Link milestone (normally hours of test fire) is what grows
  the quasar first. A hole whose disc has not settled (just seeded, or its mass just set by a bench) gives far less
  than its size suggests.
- **Eye:** Trims, Status and Transition hatches as in §4 (stock-up on Status [1], charges on [2], the core on
  Transition [4], the chart on [1]); from T7 the one Space valve on Field [1].
- **Junctions, all binary:** J1 = Field [2] / [1] (15 / 8 / 0), OR Galaxy Transition [4] → 15, OR Slots [8] → 15;
  J2 = Field [4] / [8]; J3 = Events [1] / [2], with Slots [8] → 75 % halo at T7.
- **Begin:** Galaxy Transition [1] → the Eye's Burst Control Hatch in Begin mode.

### 7.5 The proof on a dev server (2026-10-13)

`/btsquasar autorig x y z <s> eye ex ey ez` runs the standard rig on a **real** Quasar Engine (coils 3, b 3) and a
**real** Eye (T8 structure). The rig sees only the Directive codes, a second late, and acts only through the machines'
hatches. The conduit between them is the rig's maths, fed by the real quasar's jets (P4-75). Before the run the quasar
was a fresh 64 SM hole (no spin), the Eye a lit magnetar with T6 unlocked, milestones reset, escrow and plinth buses
empty, the Eye's debug rig off. Nothing else was touched; the quasar grew by itself.

| Stretch | Result |
|---|---|
| **T6 → T7** (from a fresh 64 SM, 0-spin hole) | The rig pushed the Intergalactic core on Transition [4] (P4-76), the test fire (Slots [8], 1,818 s) held the Stability Link, the rig pushed the Triangulum chart and its Galaxy Transition [1] edge began the Magnetar Collapse. The assist's eruption landed 9.97 Bu/t on the collapse second and cancelled 85 of 120. COMPLETE in 779 s: **T7 at t+14,555 s**. 0 Eye failures, 0 disc blow-outs, 579 purges. |
| **T7, the quasar growing 80 → 135 SM** | About 52,600 s from the unlock to T8 with 0 Eye failures; about 807,000 L of Ergomatter counted toward the milestone; 36 minor mergers ridden. The halo test fire began once the quasar's mean stream reached 6 Bu/t (about 118 SM, P4-79); the Heavy Link was held at about 135 SM. |
| **T7 → T8** | The rig pushed the Milky Way chart; Begin; the first pass bound the nuclei; Galactic Merger COMPLETE in 1,920 s: **T8**, core consumed, 0 failures. |
| **T8, the quasar growing 142 → 222 SM** | About 41,600 s with 0 failures; 76-85 % Resonant per stretch; 14 of 14 satellite crossings caught. The Mk I throats cap what reaches J1 at 24 Bu/t, so the growth does not flood the Eye. |
| **Real redstone** | After a run the rig keeps acting in real time with redstone blocks against the two Burst Control Hatches; the Eye's bursts followed the bang-bang J1 (24 → 8.4 → 8.8 Bu/t a second apart) with the field Resonant. |
| **The live collapse (B-118)** | The rig live under `/btswarp 60` on a real quasar, the cue on a GT wireless pair (above), 13 Eye / quasar sites in three fresh worlds, the quasar's second 3 to 19 ticks after the Eye's. In every collapse the purge landed on overfeed second 298 and the eruption was on the Eye on the collapse second. With the disc settled under the rig first (600 s): 136 SM 6.9 and 6.5 Bu/t (54 and 50 of 120 cancelled), 200 SM 14.9 Bu/t (all 120), 96 SM 3.2 then 5.9 Bu/t: every first collapse held, T7 unlocked at all four. Seeded and set to 96-256 SM just before: the first collapses failed on the unsettled disc's weak stream (0.8-3.2 Bu/t, the purge on time), a re-run held once it settled (4.7-16.6 Bu/t), T7 at all eight. A fresh 64 SM hole: 1.3 Bu/t, never. |

What failed first, and was fixed (each is a question):

- With the core wanted only after the link, the unattended rig deadlocked: no core, no test fire, J1 at 57 %, the link
  never held (P4-76).
- The T7 halo test fire on an 88 SM quasar starved the rail and lost Triangulum (P4-79).
- The galaxy chain's magnetar re-spawn failed on Integrity 344 times in a row until master's merger Directive fix was
  ported (P4-78); with it the chain re-ran cleanly.
- `autorig off` left the last second's bursts applied for ever (fixed: the rig clears them).
- At the GUI default f 0.85 a 64 SM quasar never held the Stability Link; the proof rides Eddington at f 1.0 (P4-81).

**The other hosts**, through real receptor ports and a real quasar (P4-74): the KEE (σ 2.0, bleed × 0.25, cap 768;
the link lost → held at 512 with the setpoint kept), the Homogeny T3 (σ 1.5 → speed × 1.5, Entanglement Medium open;
closed when the mean fell under 1) and Reality Diffraction (σ 0.5 → × 0.5; σ 0.25 → paused; quasar frozen → "PAUSED …
(σ held)", resumed when rebuilt).

**OpenComputers** (`/btsquasar ocloop`, `/btseye star … ocloop`, `/btskerr occall`): every getter returns, the setters
clamp (setFeed 9 → 1.5, setField 99 → the coil tier), `trim` clamps and is taken the next second, two calls in one tick
→ the second is refused.

Bench commands (P4):

- `/btsquasar autorig x y z <s> [eye ex ey ez] [analog] [cut k] [mass M] | autorig off | autorig status | ocloop x y z`
- `/btseye star x y z milestones <n> reset | buses clear | rig none | unlock <n> | lit <chart>`
- `/btsconduit inject rx ry rz qx qy qz <Bu/t> | inject rx ry rz off`
- `/btshr bench x y z receptors | inject qx qy qz <Bu/t> | inject off`
- `/btskerr receptor x y z | occall x y z method[x2] [args]`

Not run: a client (the GUI lines, the hatch GUIs), OpenComputers through a real Adapter and computer in game (the
calls go through the same API the components wrap), the Superdense refinery (SL-Q23) recipe in the Compressor Module
(the gate is code-checked), and a 16-port in-world conduit aimed at the Eye.

### 7.6 Where it is learnable in game (rule 14)

- The Directive Hatch tooltip carries the Disc, Field, Events, Galaxy Transition and Slots tables.
- The Burst Control Hatch tooltip carries its four modes; the Junction tooltip its split formula.
- The quasar, Eye, KEE, Homogeny and Reality Diffraction tooltips and scanners carry their link lines and the
  OpenComputers component name.
- The Eye's Galaxy panel shows what the next transition waits for and why the T7 test fire waits.
- The questbook: `docs/p4-questbook-contract.md`, quests "Automating the quasar" and "A galaxy that runs itself".

## 8. SMBH: the Supermassive Lattice (smbh-v1.md §6, §10.2)

Status: **built** on `smbh`, 2026-10-14. Every number below is a knob of the spec. **Units:** 1 Bu/t = 10^13 EU/t in a
crown; the GUIs, scanners, tooltips, NEI pages and quests show the EU/t beside every Bu/t (one formatter,
`BTSFormat.buEu`: "9,796 Bu/t (9.8e16 EU/t, 98 quadrillion)").

**Directive Hatch, set Quasar: Horizon** (quasar `d` socket; window not used):

| [1] | [2] | [4] | [8] |
|---|---|---|---|
| Hold-up short: the mass over the safe limit leaks | Feed short: the Accretion Buffer holds under 30 s of Disc + Hold-up | Anchor short: an Unbound hole sheds to the doubling the Primordial Matter pays for | Overdrive upkeep short (MHDCSM or Universium) |

**Burst Control Hatch** modes on the quasar's `c` sockets (a hatch in a mode overrides the GUI's value while it is in it):

| Mode | What the signal does |
|---|---|
| Hold | The mass setpoint from the safe limit (s 0) to the band top (s 15), log-linear. On an Unbound L4: 4,194,304 SM to 268,435,456 SM (two doublings past the L4 top). |
| Overdrive | o = s / 15, snapped to 5 % (needs L3) |
| Shed | A rising edge sets the mass setpoint to the safe limit (a 2 %/s shed, no power while it lasts) |

**Stellar Field Sensor Hatch** channels (appended; 0 without a lattice in force): M / safe %, M / band top %, band x,
the intake Disc / Hold-up / Growth (L/s) and % of the 5x cap, hold-up coverage %, mass change % per hour (signed), the
Accretion Buffer's seconds, Overdrive level %, upkeep paid %, upkeep each (L/s), the output multiplier X (x1 = 9.8e16
EU/t), Unbound n, anchor coverage %, shedding (15/0), Supermassive Link source ready (15/0: a lattice in force, M at
least 65,536 SM, linkable). The window now reaches 2,000,000,000 and the threshold 10^15 (SMBH masses and streams).

**OpenComputers** `bts_quasar` additions:

| Method | Returns / does |
|---|---|
| `getBand()` | lattice (in force), latticeBuilt, saturated, saturationSeconds, M, safe, top, structureTop, setpoint, setpointFromHatch, x, unbound, n, shedding, shedTarget, shedReason, massPerHourPct, supermassiveLinkSource |
| `getIntake()` | gas, disc, holdUp, growth, total, cap, capPct (L/s, last second), massPerHourPct |
| `getHoldUp()` | need, paid, coverage, leaking, leakPerHourPct |
| `getBuffer()` | litres, cap, seconds (-1: nothing needed), feedShort |
| `getOverdrive()` | level, inUse, fromHatch, available, upkeepEach, coverage, mhdcsm, universium, oEff, outputX, bursts (Bu/t), burstsEUt, burstsText |
| `getAnchor()` | allowed, unbound, n, need, paid, coverage, short, paysN |
| `getHorizon()` | the Horizon set's code and named bits |
| `setMassSetpoint(M)` | SM (0 = the safe limit); returns the setpoint in force |
| `setOverdrive(o)` | 0-1 in 5 % steps; returns it |
| `setUnbound(on)` | needs an L4 lattice in force and `smbhAllowUnbound`; returns Unbound and why not |
| `shed()` | the setpoint to the safe limit; says what it does |

`getBurst()` also returns `totalEUt` and `totalText`.

**Every action, four routes:**

| Action | AE2 | Redstone + logic gates | OpenComputers | Steve's Factory Manager |
|---|---|---|---|---|
| **Intake** (hold-up and growth gas) | ME stocking hatches in the 24 rim sockets; the controller draws every tick while they give (each drain at most 2^31 - 1 L) into its 60 s Accretion Buffer | Horizon [2] (buffer short) → an alarm or a backup supply | `getIntake()`, `getBuffer()` | Fluid node into the rim hatches |
| **Mass setpoint** | Level emitter on a stock → a Burst Control Hatch in Hold (two presets) | Analog redstone → Hold | `setMassSetpoint(M)` | Redstone emitter (analog) on the Hold hatch |
| **Shed** | — | A pulse into a hatch in Shed mode | `shed()` | Redstone pulse |
| **Overdrive** | Level emitters on the MHDCSM / Universium stocks → the Overdrive hatch (cut it when a stock runs low) | Horizon [8] → cut the Overdrive hatch's signal | `setOverdrive(o)` | Analog emitter |
| **Overdrive / anchor fluids** | ME stocking hatch in a rim socket, or export bus into a plain trim hatch | Horizon [8] / [4] → a Fluid Regulator on the supply tank | `getOverdrive()`, `getAnchor()` | Fluid node |
| **Unbind** | — | — | `setUnbound(true)` | — (the GUI's two-click button) |

**Where it is learnable in game:** the quasar's tooltip (the intake, the setpoint, Overdrive, Unbind, the units), its
Info page (EU/t, Supermassive Lattice, Intake, Overdrive, Unbound, Automate the lattice), the Supermassive Lattice
panel's tooltips, the Directive and Burst Control Hatch tooltips and the sensor's channel list.
