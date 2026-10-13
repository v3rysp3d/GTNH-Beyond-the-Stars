# Automation: every action, four ways (the Eye and the Kerr)

Status: **built** on master, 2026-10-13 (automation pass, build brief item 18). Question B-70 lists the judgement
calls. The Quasar, the Homogeny and the Reality machines get their own sets on the same hatch when they are built
(P4 design `designs/docs/quasar-p4.md` §8).

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
| **Upkeep** (H, He, RSPM, ESPM, SpaceTime, HSM feed, WDM/BDM, Agent) | ME stocking input hatches in the gas / stellar sockets (they drain into the buffers), or level maintainers | Status bit 1 → an export bus or pump | `getBuffers()`, `getDirective("EYE_STATUS")` | Fluid node into the gas hatches on bit 1 |
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

---

## 5. Where it is learnable in game (rule 14)

- The Directive Hatch tooltip carries every bit table above, the output modes, the window and the control sets.
- The Eye and the KEE tooltips name the hatch, their sets, the plinth-bus pickups and the OpenComputers component.
- Their scanners list each Directive Hatch with its set, code and bits by name; the hatch GUI shows the four bits by
  name with the ones on now.
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
- `/btskerr autorig x y z [s] [bolts/s] [empty] | ocloop x y z | ctrl x y z on|off|out | ocprobe x y z`
