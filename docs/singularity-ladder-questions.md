# Singularity Ladder: questions for you

One list, restructured 2026-10-13 after the tidy, questbook and Homogeny / Reality branches were merged into master
(B-87). It has four parts:

1. **Open questions**: each open decision once, with the answer we recommend and where the detail is.
2. **Settled**: everything you have answered, in your words where we have them, and what was built.
3. **Open work and checks**: things that need doing or seeing in a client, but no decision from you.
4. **Detailed entries**: the full write-up of every question (A-1..A-3, B-1..B-101 and B-105..B-122, the tidy pass's
   TQ-1..TQ-8, and P4's P4-01..P4-94 in Part E).

Nothing is renumbered. The code, commit messages and the build spec cite these numbers (`B-72`, `TODO(Q-20)` and so
on); the old Q-1..Q-38 and the merged branches' numbers are mapped under "Where the old question numbers went".

The spec these refer to is [`singularity-ladder-build.md`](singularity-ladder-build.md). Its top section, "What's in
the build", explains how to get the machines in creative.

---

## 1. Open questions for you

Most important first. "Recommended" is what we would build if you say nothing. Nothing here blocks the playtest jar.

| # | Question | Recommended answer | Detail |
|---|---|---|---|
| 1 | The window bands made the Kerr T1 about a third cheaper in Infinity. Win it back? | Yes: the mirror-backing and mirror recipes (SL-P02 / SL-P03) make 1 block per craft instead of 2, which puts the drum at about 113 % of its old price | B-86, open 1 |
| 2 | The Kerr Density Upgrade still installs on a plain right-click. Your ruling was milestones + an install run (Q-10); D-49 replaced that for the Eye only | Build it the Homogeny's way (core in escrow, an Install button, a run that pauses on low power) with B-1's Kerr numbers: hold 64 SM for 30 minutes in total, then a 10-minute offline run costing the upgrade and 9,216 L molten SpaceTime | A-2, B-1 |
| 3 | Kerr "Auto": a Collapser dropped in a bolt bus collapses the hole within a second, so the GUI's two-click Collapse means little while Auto is on | Auto acts on Seeds only (evaporation recovery keeps working); collapsing is the button, OpenComputers or the Directive Hatch | B-57 |
| 4 | Resonance Chamber III (the Eye T5 gate) still has two recipes in NEI: GT's (Black Dwarf Matter foil) and our SL-P33 (White Dwarf Matter foils) | Rewrite GT's in place, as you chose for chamber II (SL-G01) | B-83, item 1 |
| 5 | Magnetar Merger: the imprint band is +60..+90, but above +70 the field is Critical, so only +60..+70 survives the 120 s hold | Keep as built (the Directive's Agent bit at Π > +66, a 1 unit/s Agent valve during the merger) and re-check after playing T6. The alternatives change the merger's maths: narrow the band to +60..+70, or no Integrity drain inside the band | B-71 fix 1; B-62 "Not changed" |
| 6 | Starting the Magnetar Merger (the Magnetar chart in while Sirius B batches) is recorded as the "Last failure" | Record it as an event, not a failure, when the next tier's transition begins | B-71 "Seen, not changed" |
| 7 | A silo socket holding the wrong hatch (e.g. a Crafting Input Buffer in the feedwater socket) drops the silo with no error naming the cell | A status line that names the socket: "Feedwater socket holds a Crafting Input Buffer: use a plain Input Hatch" | B-32 |
| 8 | Window height: 5 rows shows the whole star up to Sol's size and the middle of the larger holes | Keep 5 rows (7 would remove the Eye core band's top and bottom rims) | B-86, open 2 |
| 9 | The Homogeny T1's throat tip has 5 Region Crystals, which need Ergomatter (Eye T7) on a UMV machine; the controller also accepts Godforge lens glass there | Re-draw the T1 throat tip as lens glass in voxel.js (a new char) and keep the crystal for the T3 tail only. voxel.js is the P4 build's file right now, so this waits for the P4 merge | B-76, item 3 |
| 10 | Reality Diffraction has no detection hatch of its own (ruling 18) | Build a "Region Anchor Sensor" (one redstone bit per missing anchor) on the base ring, modelled on the Stellar Field Sensor Hatch | B-77, item 10 |
| 11 | The Homogeny turns 100 L Raw Star Matter into 1 L ESPM, so it cannot keep a T3 Eye fed (2.5 Sols' worth per Arcturus) | Keep 100 : 1 (D-46 / D-47 as ruled): the Homogeny banks a Sol surplus; Eye T2 stays required | B-80, item 4 |
| 12 | Eye band and tier names: the code says Planetary / Galactic; mine.md proposed Stellar · Systems · Galactic · Universal | Decide them together with the per-tier Eye names you left for later (Q-13) | B-6 |
| 13 | Numbers to tune after a playtest: the T6 shield per upkeep (K = 2), the T6 dwarf-matter reserve and top-up, the First Cooling's automatic retry, the KEE's containment draw (1 A UEV) | Keep; re-check each after playing T5 / T6 and a Kerr | B-48, B-60, B-63 |
| 14 | The jar carries the op-only bench commands and the dev-only harnesses: render QA and the automation photo rigs (inert without `-Dbts.rendertest=true`), the questbook screenshots (`-Dbts.qbshots=true`) and the video recorder (`-Dbts.rec=true`) | Keep them in test jars; exclude `rendertest/**` and `devshots/**` from a numbered release | B-27, B-73 item 2, B-105, B-107 |
| 15 | A numbered release name | Tag one (e.g. `0.25.0-rc2`) when you want the Mods list to show a clean version | B-33 |
| 16 | Hand-drawn textures | Hand-polish only what players stare at: the two controller faces and the mirror and bastion walls | B-13 |
| 17 | The Eye T10 gate part (TecTech's generators stop at tier 9) | Design it with P5 | B-3, need 3 |
| 18 | Everything else that ends "Suggestion: as built" and has no ruling | Accept as built unless you object: B-5, B-7, B-8, B-9, B-11, B-14..B-17, B-19, B-20, B-26, B-27, B-31, B-35..B-37, B-45..B-54, B-62, B-64, B-66..B-68 (judgement calls), B-70, B-74..B-78, B-87, B-108 | each entry |
| 19 | Reality Diffraction, Reality Refab and the Homogeny T3 are built machines but their quests are still "(planned)" checkboxes, because the `realityRefab` flag is off (since P4 only White Hole Matter, Eye T9, is missing; "Riding the Rift", 48194, waits with them) | Keep the checkboxes until P4/P5 make the inputs; until then, give their quests a variant of the placeholder note: "Built, but its inputs come from Eye T8/T9 (a later update)" | B-93, question 3 |
| 20 | A plus-signed decimal ("+0.5", "+0.15") can still break after its point at a line end: Java's line breaker splits it, 6 places in the Eye T4-T6 trims | Accept (rare, width-dependent); or write the trims without the plus sign | B-93, question 2 |
| 21 | Resonance Chamber IV (the Eye T8 gate) has two recipes in NEI: GT's (MHDCSM foil) and our SL-P40 | Rewrite GT's in place under the same flag, with chamber III (question 4) | B-110 |
| 22 | The intake cap at T7 / T8 buffers tens to hundreds of G L of hydrogen and helium (companions, planets mode) | Keep B-108's 20 cycles / 2 runs rule; shorten the companions' window if a shared network still starves | B-110 |
| 23 | Eye T7 / T8 from the front at eye level: the window shows the galaxy's core and inner arms (\|x\| ≤ 10), but its outer arms (to r 16 / 19) stand behind the T6 rim and the arm blades | Keep the opening at \|x\| ≤ 10: wider would cut the arms and the rim out of the T4-T6 Eye too, and the near-flat disc (8° tilt) reads best from above anyway | B-114 |
| 24 | The time warp `/btswarp` (op only, cheats): ship it in release jars? | Yes, op only like the other benches: it changes nothing for a player who does not type it and is a real aid for testing a rig | B-117 |

## 2. Settled

Your answers, newest first. Quotes are your words.

| Topic | Your answer | What was built |
|---|---|---|
| UXV parts and circuits (D-45; B-72 item 1, B-84; brief item 22a superseded item 20) | First "Brainstorm page first" and, for the playtest jar, "Ship UMV stopgap". Then, on `designs/docs/uxv-circuits.md`: Q-U1 "Both", Q-U2 "Empyrean", the above-MAX tier "Omega" (not Apotheosis), Q-U9 "The Eye's live star"; the other Q-U items as recommended | In the jar: the UMV stopgap (B-72 for SL-B70 / B80, B-84 for SL-H10). Next: the Empyrean circuit tier before MAX (Phase U1, Grades I-II, which unblocks T5 / T6 and retires the stopgap), then U2 (Grade III, the Godforge re-key), on master after the playtest run and before P4 merges |
| Generator upgrade path [B-30; B-29's alternative] | "Add an upgrade path" | SL-U00..U48 and the Retrofit Kits (B-83) |
| Resonance Chamber II [B-29] | "Rewrite GT's in place" | SL-G01 rewrites GT's recipe at load-complete; SL-P39 retired (B-83) |
| The hole and the star hidden from the front [render K2, E1 / E-9; B-66..B-68] | "Glass window bands" | One Quantum Glass pane per window, the cells behind it open (B-86) |
| The Reality machines' Godforge lens glass [B-78 item 7] | Keep: "This will be a MAX tier / Endgame item, its allowed and meant to be expensive" | Unchanged |
| Old-plant renames and questions [TQ-1..TQ-8] | "You have full free reign on this" | Yellowcake Mill, Spiral Cycle Sensor, Grade 1-6, ORE PROCESSING, Extra Leach Tank, tier-1 defaults, input separation (off), `getTrueParallel()` (B-85, Part D) |
| Superdense Ergomatter [ruling 17; P4's Q-13] | "it should be ergomatter and superdense ergomatter which is just compacted and refined but the t8 gives you a better version and easier to get"; then one fluid, with Eye T8 the better route (P4's Q-13 as designed) | One fluid, `btssuperdenseergomatter`; the last "Supermassive Ergomatter" text on master is renamed (B-87). The Supermassive Seed Core keeps its name |
| Trophy quests consume our Eye controllers [QB-7] | "Keep as trophies" | Unchanged (B-85); since B-92 they sit in The Endgame and the emptied Endgame Goals chapter is hidden |
| Jar handover; the dedicated-server `SERVER_DIR` [B-61, B-73] | "SSH key will be tonight, run locally for now" | The playtest jar goes to the local Prism instance; the server's SSH key exists but is not yet authorised |
| T6 Antipolarization Agent upkeep [B-34 item 1, B-37 item 1; ruling 12] | 3,000 L per 60 s cycle, and the Agent fed also charges the flare shield (one fluid runs the whole magnetar) | Prototype, fixture, golden file and Java (B-60); the merger imprint fix (B-71) |
| Unlocks and the Quasar [ruling 17a] | "the 'to unlock the next' doesnt need to be materials it can also be machines so the quasar can provide stability to a different machine" | P4's design (quasar-p4.md §3.1); the Homogeny T2 / T3 and the Diffraction carry reserved Quasar port cells and hooks (B-80 item 7) |
| P4 loop fixes [ruling 19] | Assumed per "rest asume": the Homogeny Tier II Core uses Black Dwarf Matter, not Magnetar Matter; no UXV part before Eye T7 uses Ergomatter | SL-H10 on Black Dwarf Matter (B-78 item 2); SL-B16 has no generator recipe |
| The Homogeny and the Reality machines [ruling 16] | Unheld and built from your sketches; "make asumptions for everything" | B-74..B-80 |
| D-49 star lifecycle [B-18, B-25] | The proposal approved: T2 required, cumulative bands, the Agent as the magnetar's constant feed, and the rest of B-18's recommendations | D-49 units 1-6 (B-34..B-38, B-45, B-62, B-63), the +150 bounce |
| Kerr redesign [brief item 9] | "this multiblock is EXTREMELY hard to understand": the KEE controls only the hole; modules are separate machines; a neat GUI; a black-hole render | B-46..B-57. B-56's question (should the render go while the KEE is unformed) was built as yes from B-59 on |
| BEC [brief item 11] | (a) RC-2's own BEC recipe makes the Spatial / Temporal casings ×32 and our batch Assembler recipes go ("a bit too cheap"); (b) "we need way more things in the bose einstein"; (c) one jar to test | SL-B01..B04 (B-59, B-60); the BEC expansion SL-B10..B81 (B-64); packages B-61, B-73 |
| RC-2 port: the BHC deadlock and White Dwarf Matter [B-29, B-30] | The Kerr replaces the BHC in recipes; "We shouldnt be making white dwarf matter until t4?" (no WDM before Eye T4) | `BTSBlackHoleSwap`; SL-K01 at BHC parity; the gate audit rule (B-29, B-30, B-35) |
| Eye T2 as the interim Excited Stellar Plasma source [Q-2] | Accepted | Built |
| Remove TecTech's Eye recipes [Q-3] | On by default, and a config option | Built (`replaceTecTechEyeOfHarmony`) |
| Eye gives EU back? [Q-4] | No, pure EU sink | Built |
| Stale-card substitutions [Q-7] | Accepted; the review cards get redrawn | Built (spec §2.1) |
| Kerr mass mechanic [Q-8] | Keep | Built |
| Extractor Module [Q-9, Q-22] | GT extractor recipes only | Built (A-1) |
| Upgrades [Q-10] | Consumed, permanent, milestones + install run | Eye: D-49's milestones, escrow and transitions. Homogeny: built (B-76). **Kerr: not built (open question 2)** |
| Eye name [Q-13] | "Eye of Harmony" at every tier for now; per-tier names later | Built (the name is set in one place) |
| Compression gate [Q-20] | Hard gate, 1:1 | Built |
| Reuse the EOH and BHC blocks [Q-16] | "use the eye of harmony blocks that currently exist as we want to try keep everything similar" | Built (spec FC-1) |
| Kerr lining [Q-16, B-12] | One new white Kerr lining block | Built: Harmonic Lining, `slCasing2:2` (B-15) |
| Planets and compression [Q-29, B-4] | Drop TecTech's per-planet rule; the 1:1 gate alone decides | Built (B-16) |
| Kerr T2 with the reinforcement torn down [B-1] | Won't form | Built (unchanged) |

**Closed without a ruling (superseded or done):** A-1 (done); B-2 (D-49 retired the interim Black Dwarf star, the UXV
parts follow the Empyrean design, and Eternity before T10 is D-48's swap, shipping with P4); B-3 (answered by D-49,
except the T10 part: open question 17); B-10 (built, B-45); B-21..B-24 (built in the recipe audit); B-22 and B-28
(retired on RC-2, B-29); B-34 item 1 (ruling 12); B-38 item 2 (the `/btseye` chat line was reworded); B-55's size cap
(the logarithmic scale of B-66); B-59's open items (B-60, B-64); B-75 item 6 and B-78 items 3 and 6 (B-78, B-80, B-87).

## 3. Open work and checks (no decision needed)

**To write or build.**

- ~~The tidy pass's questbook handoff~~: done in the 2026-10-13 text pass. Most of the mechanics were already in the
  plant chapter; the four that were not are added, and the last glossary names are fixed. [B-82 item 5, B-85, B-92]
- A `bomItems.homogeny` table, so the Homogeny quest (48118) gets the optional structure checklist; one table covers
  the Reality machines too. [B-82 item 3]
- Renders not built: the Homogeny's vortex, the Reality region clouds and the reassembly stream. [B-76 item 7, B-77
  item 11]
- Move the KEE GUI strings to `en_US.lang` when the Kerr text settles. [B-54]
- `FieldSim.main` still calls `System.exit`, so FML warns once when the bench loads it. Harmless. [B-37 item 10]

**For the P4 merge** (done, B-110).

- ~~P4's H/R entries as B-60..B-67~~: master's B-74..B-80 kept; P4's own entries are Part E. [B-81, B-110]
- ~~One owner for Ergomatter and Superdense Ergomatter~~: `SLQuasarFluids`; `SLRealityFluids` aliases them. White
  Hole Matter stays `SLRealityFluids.ifAbsent` until P5. [B-75 item 5, B-110]
- ~~The exporter takes over the window code~~: merged, re-exported, `--check` clean. [B-86 open 3, B-110]
- ~~Eternity-on-Ergomatter~~: SL-Q26 at load-complete, `ETERNITY_BEFORE_T10` follows it; the H/R gate lines drop from
  14 to 8 (only White Hole Matter, P5, is left). [B-80 item 3, B-110]
- ~~The MTE ledger comments in `BTSMachines`~~: tidied. [B-87, B-110]
- ~~P4's questbook content (the contract)~~: in `singularity.json`, the five P4 features on, 22 new quests, the
  Eternity stage live and gated on `replaceTecTechEyeOfHarmony`; tested on a world with progress and a fresh one. [B-111]
- Eye T8's gate: SL-P40 makes Resonance Chamber IV without MHDCSM as a separate recipe; GT's own chamber IV recipe is
  still in NEI beside it (question 1 of B-110). T9 is P5. [B-29 "For P4/P5"]

- The merge + playtest run (B-88) passed headless; the Cascade Hall tooltip fix is in, and the walk now requires two
  output hatches unless one is ME (B-89). [B-88 fix 1, B-89]
- The playtest jar `master.235` is packaged with a local-install INSTALL.md and a playtest guide (B-90). [B-90]
- ~~Re-package the playtest jar~~: done, `master.250` carries `/btsrecipes`, the B-92 / B-93 text pass and the B-95
  fixes; `master.235` is in `dist-rc2/superseded/`. [B-105]
- ~~Harmonic Lock's "The GUI's field line counts it as Lock n/600" becomes "Lock bar"~~: done in the questbook
  source, bundle `622edc863402`. [B-93, B-96, B-115]
- ~~Keep the Eye's status-line push (`refreshStatusLine`) and check the Directive Hatch's height in the new
  layout~~: the push is kept; the kit's Directive panel (234) replaces the +64 fix. [B-107, B-115]
- ~~Cap the Eye buffers' intake~~: done, each buffer fills only to its intake cap (Sol: 1 G L of each gas), tested
  with a stocking hatch on a 10 G L network and a second consumer on it. [B-107 open work (a), B-108]
- ~~The Eye GUI shows each buffer as held / cap~~: the Field panel's Buffers section. [B-108, B-115]
- ~~A missing texture in the KEE GUI's right-hand button column (seen in the photo run)~~: the kit's KEE GUI no
  longer draws it (B-97), merged. [B-106, B-115]
- ~~Merge `gui-clean` into master~~: done, with P4's GUI additions carried into the kit. [B-115]
- ~~The P4 machines' GUIs on the kit, and the Eye's T7 / T8 rows seen in a dev client~~: done; the Quasar Engine,
  the conduit elements, the crowns, the Burst Control Hatch and the GRB Receptor Port are on the kit (the E dial's
  bands and the missing power-switch texture fixed), shot at GUI scale 3 against the mockups. Still to see in a
  client: the Galaxy panel and the Field panel's Buffers section. [B-115, B-116]
- The photo harnesses (`RenderTestClient`, `AutoRigClient`) may film at the dev client's saved render distance. Give
  them the recorder's per-tick settings hold if they are used again. [B-109]
- ~~Restart persistence on master's machines (P4-85) and tooltips for the older casings (P4-86)~~: done; the
  Eye, Homogeny, Reality machines and Kerr modules mark their chunk once a second, the Burst Control Hatch on a mode
  change; tested with a clean stop and a killed server. [B-112]
- ~~A full RC-2 dev-server test of master after the P4 merge~~: done; every check passed, the T6 -> T7 -> T8 chain
  ran unattended with 0 failures, and nothing needed fixing. [B-113]
- ~~The Eye T7 / T8 front window (review finding)~~: done; the stability rail's front clamp no longer stands in
  front of the galaxy's core, and the window check covers T1-T8. [B-114]
- Controller GUI cleanup unit 1 (branch `gui-clean`): the GUI kit, the family frame and the Eye of Harmony GUI;
  the questbook's "field line" sentence becomes "Lock bar" when the questbook owner applies it. [B-96]
- Controller GUI cleanup unit 2 (branch `gui-clean`): the Kerr Ergosphere Engine and the two Kerr module GUIs on the
  kit; the disabled power switch no longer draws GT's missing texture. [B-97]
- Controller GUI cleanup unit 3 (branch `gui-clean`): the Homogeny, Reality Diffraction / Refab, the Reactor Silo and
  the production halls on the kit; recipe results are compared by key (fixes the Kerr module and Eye planet rows). [B-98]
- Controller GUI cleanup unit 4 (branch `gui-clean`): the hatch layout, the Directive Hatch (panel 214, nothing over
  the inventory) and the Stellar Field Sensor Hatch GUIs; spec §6 lang keys complete, GUI lang check 0 missing. [B-99]
- Controller GUI cleanup round 1 (branch `gui-clean`): every master GUI checked in a dev client against the mockups;
  12 fixes, among them the empty kit terminal (row clock overflow), the Refab lamp overlap and the Directive readout. [B-100]

- ~~Merge the `showcase` branch (recorder v2, the time warp, the live rigs)~~: done; its B-108 is B-117. [B-117, B-118]
- ~~P4-95: the live collapse purge lands late~~: fixed; the cue leads by 2 s and the live rig wires it with GT
  wireless covers; every collapse on a real quasar under the live rig got its eruption on the collapse second. [B-118]
- A BEC network in the showcase world (a TecTech BEC Storage, BEC Hatches, Diffraction's regions to the Refab) so
  Reality Diffraction and Refab can run on camera; suggested as a `/btshr bench ... becnet` helper. [B-118]
- ~~Test merged master before the final recording~~: done: the static checks, a clean boot (311 / 0, no
  warnings), `/btswarp`, and the merged GUIs in a client. The Eye's Field panel values were cut by its
  scrollbar; fixed. [B-119]
- `/btswarp` for a player who is not an op: refused in code (permission level 2), not yet tried with a real
  non-op client. [B-119]
- ~~Review of the merged Eye GUI: a T8 merger warning hidden by a satellite crossing, the Buffers list cutting the
  fluid the star needs~~: both fixed; the overflow row is not yet seen in a client. [B-120]
- ~~Record the final showcase~~: done, 7:05 at 1080p from master, with the pack's UIV / UMV quest chapters; the
  recorder parks the mouse after a screen opens (a quest tooltip had covered the book). [B-121]
- ~~Package the playtest jar from master after P4, the GUI merge and the showcase~~: done, `master.302`
  (`362b228`) in `dist-rc2`, INSTALL.md and the playtest guide refreshed (phases Q, T, W; 425 recipes from a fresh
  dump); `master.250` is in `dist-rc2/superseded/`. [B-122]

**To see in a client** (the headless dev server cannot): the hologram hints, a survival build from a real AE system,
the NEI multiblock tab and its frame rate (a T6 Eye is 51,935 blocks), every GUI (Eye, KEE, modules, Directive Hatch,
Homogeny, Reality), the round-4 and round-5 render verdicts, a real Condensate Assembler craft (the BEC parts, the
upgrade path and the Retrofit Kits), the renamed machines over an existing `GregTech.lang`, an OpenComputers computer
running Lua, AE2 and Steve's Factory Manager in a world, Reality Diffraction / Refab runs (they need P4's fluids), and
the P4 quests in the book (the UXV map, the new quest pages and their wrapping). [A-3, B-19, B-36, B-45, B-54, B-69,
B-70, B-79, B-83, B-85, B-111]

---

## 4. Detailed entries

Parts A, B and D below are the full entries, in the order they were logged. Their status is in sections 1 and 2;
the ones answered since carry a "Settled" or "Done" line.

## Part A. Answered, but the jar did not match yet (A-1..A-3)

### A-1 The Kerr Extractor Module still runs its own "melting" recipes [Q-9, Q-22]: DONE

**Your ruling.** The Extractor Module runs GT's normal extractor recipes only: no fluid extraction and no custom map.
The Kerr is a module host, so adding a module later must stay cheap.

**What the jar does.** The module runs a new NEI page, "Ergosphere Extraction". It melts Neutronium, Infinity,
SpaceTime, WDM, BDM and similar ingots, plates and blocks back to fluid at 144 L per ingot. That page was built before
your ruling.

**Next step.** A small code change: point EXTRACT mode at GT's extractor map and delete the melt recipes and their
NEI page. The module system already works as "new arc + its own private hatches + a recipe map", so it stays
extensible. No question for you; this is a to-do.

**Done (follow-up pass, 2026-10-12).** EXTRACT mode runs `RecipeMaps.extractorRecipes` and nothing else. The
"Ergosphere Extraction" map, its NEI page, its lang key and the SL-K06 melt recipes are deleted; the Kerr now shows on
GT's own Extractor page. The extractor's sockets take item buses only (B-17). Each module is now one `KerrModule`
entry that the walk, the hologram, survival build and the status lines loop over; the spec's new section "Adding a
Kerr module" lists the steps for the next one.

### A-2 Upgrades still install instantly [Q-10, Q-24, Q-38]

**D-49 status (unit 6).** Superseded for the Eye: the Galactic Upgrade Core (and the four new cores) go into an escrow by right-click and are consumed only by a successful transition, after the current star's milestones (B-37). The Kerr Density Upgrade belongs to the Kerr redesign branch.

Your ruling is milestones plus an install run (see B-1 for the numbers). Today a plain right-click with the Kerr
Density Upgrade or the Galactic Upgrade Core installs it at once. Both machines route the install through one method
each, so the mechanic can drop in without rework. It is blocked only on the numbers in B-1.

### A-3 Acceptance checks not done yet

The build compiles, loads and registers every recipe. These checks from your acceptance list are still open:

- **Cost table.** Per-block recipe × block count → total raw materials per machine tier. Not written yet. The block
  counts are in the spec (§5.1). The structures need thousands of each casing (for example 2,852 mirror casings
  on a Kerr T1), so this table is the real fairness check.
  **Written (cost audit):** [`singularity-ladder-costs.md`](singularity-ladder-costs.md) has the completeness check,
  per-block costs, per-tier totals and the BHC / TecTech Eye / Godforge / Nano Forge comparison. It found two
  uncraftable chains (Kerr T2 and Eye T2-T4) and an Eye T1 at 8× TecTech's Eye: B-21..B-24, 15 proposed changes.
  **Applied (recipe audit pass):** every block and item in the BOM now has a recipe that can run; costs doc §6 lists
  what was applied and how (B-26, B-28 are the new judgement calls).
- **NEI multiblock preview.** The controllers implement the hologram interfaces, but nobody has opened the NEI
  "Multiblock" tab for them yet, or checked whether it caps the size of what it renders.
  **Checked against the bytecode (acceptance pass):** the preview finds both controllers on its own, counts Kerr 2
  tiers and Eye 4, builds them through our survival auto-place, and has no size cap our structures exceed. The
  hologram paths were reviewed and fixed at the same time. Details: spec, "Hologram & NEI"; open points B-19, B-20.
  Still to do: open both in game.
- **In-game build test.** The smoke test only loaded the game. Nobody has run `/btskerr` or `/btseye` yet, so the
  structure orientation, the hologram and survival auto-build from AE are unverified. If a build reports
  NOT FORMED with the first wrong block on the mirrored side, the structure files need re-exporting flipped
  (spec, "Structure export").
  **Done headless (dev server, console forms of the build commands):** every Kerr and Eye tier forms in all four
  facings, through both the creative and the survival hologram paths. The orientation matches the mockup, so no
  re-export was needed. A re-projection never breaks a formed machine, a survival repair puts back exactly the
  missing blocks, and every build survives a server restart. Results and limits: B-27. Still to do in game: the
  client-side hologram hints, survival build from a real AE system, and the NEI tab.

---

## Part B. Entries B-1..B-101, B-105..B-122

### B-1 Upgrade install: the milestone and install-run numbers [Q-10, Q-24, Q-38]

**D-49 status (unit 6).** Superseded for the Eye by D-49's milestones, escrow and transitions (numbers in B-37, `EyeMilestones`). Still open for the Kerr only.

**Context.** You ruled "milestones + install run" for the Kerr Density Upgrade (Kerr T1 → T2) and the Galactic
Upgrade Core (Eye T3 → T4). The upgrade is consumed and permanent, and an install survives a wrench.

**Proposal** (all numbers to tune):

| | Kerr Density Upgrade | Galactic Upgrade Core |
|---|---|---|
| Milestone to unlock | Hold the hole at 64 SM (the T1 maximum) for 30 minutes in total | Finish 4 Arcturus (T3 star) runs on this Eye, about 5 h 20 min |
| Install run | 10 minutes, machine offline | 60 minutes, machine offline |
| Install cost | The upgrade + 9,216 L molten SpaceTime + full hatch power for the run | The core + 50,000 L Raw Hyperdense Stellar Plasma (one Arcturus run's output) + one Arcturus run's EU (about 1.9 × 10¹⁴) from wireless |
| Progress shown | GUI, tooltip and scanner | GUI, tooltip and scanner |

Start the install with a plain right-click holding the upgrade. A sneak-right-click with an item in hand never
reaches the block in 1.7.10, so it cannot be the gesture.

**Also needed a ruling (now settled).** If someone tears down the Kerr's T2 reinforcement after the upgrade, the Kerr
refuses to form at all. **You ruled: keep "won't form".** Built, unchanged, and now commented in the code and the
spec.

**Alternatives.** Make the milestone count running time instead of mass held or runs finished. Or skip the
milestone and keep only the install run.

### B-2 Turning off TecTech's Eye leaves Universium with no source until P5 [Q-3, Q-14, Q-33]

**Status (2026-10-13).** Closed without a ruling: D-49 retired the interim Black Dwarf star (Black Dwarf Matter is Eye
T5's), the UXV parts follow the UXV parts and circuits design (Empyrean, brief item 23) instead of D-45's override, and
Eternity before T10 is D-48's swap, which ships with P4. Universium itself stays an Eye T10 product.

**Context.** TecTech's Eye of Harmony was the pack's only source of White Dwarf Matter, Black Dwarf Matter, Universium
and Raw Star Matter. You ruled that the removal ships ON and is a config option. That is now real:
`replaceTecTechEyeOfHarmony=true` in `config/beyondthestars.cfg`, and the boot log confirms 43 TecTech recipes and its
controller recipe removed.

**What happens to each matter:**

- **Raw Star Matter:** our Eye T1 (Sol) makes it.
- **White Dwarf Matter:** our Eye T4 (Sirius B) makes it.
- **Black Dwarf Matter:** an interim T4 recipe ("Sirius B, Cooled") makes it until Eye T5 ships in P4. It sits behind
  its own flag, `interimBlackDwarfAtEyeT4=true`.
- **Universium:** no source until Eye T10 (P5). Universium feeds UXV components, EIC nanites and late Godforge
  upgrades, so anyone who reaches those is stuck.

**Also built.** TecTech's Eye controller recipe goes too, so nobody crafts a machine that cannot run. Placed ones
stay as decoration. The removal runs only after our Eye has copied TecTech's planets. If that copy fails, TecTech's
Eye is left fully working and an error is logged.

**Suggestion.** Keep the interim Black Dwarf recipe until P4. Ship the planned recipe swaps (UXV parts without
Universium, D-45; the Eternity loop on Ergomatter, D-48) no later than P4.

**Alternatives.**

- Keep TecTech's rocket-tier 6-9 planets as a small Universium trickle.
- Accept the Universium stall for now.

### B-3 What Time Dilation and Stabilisation do, and the tier-gating block [Q-16, Q-20, Q-29]

**D-49 status (unit 6).** Answered by the approved D-49 proposal: Time Dilation sets the time dial's cap (1 + 0.25 x (tier - N)) and Stabilisation sets q (1 - 0.05 x (tier - 1)), which scales the field's noise and events. Compression stays the 1:1 gate. Planets keep only the compression time reward.

**Context.** Following your rulings:

- The Spacetime Compression Field Generators hard-gate the Eye's tier 1:1. Eye T4 needs compression tier 4 in every
  slot. The table is in one place in the code.
- Time Dilation and Stabilisation become separate upgrades, but their effect is not designed yet.
- You also want "a block that gates tiers"; that design session is still pending.

**What we built.**

- **Where the generators sit:** compression on the six upper struts (188 blocks), dilation on the six lower struts
  (138) and stabilisation under the six pylon feet (294). All three are in the T1 core, as in TecTech's Eye.
- **Dilation and stabilisation** accept any tier and do nothing yet (marked TODO).
- **Mixed tiers** are allowed in one structure, and the lowest one counts. Upgrading 188 generators one by one never
  un-forms the Eye.

**Need from you.**

1. What dilation and stabilisation do. Our suggestion is TecTech's meaning, which players already know:
   - dilation = speed;
   - stabilisation = planet success chance.
2. Whether the dedicated "tier gate" block replaces the compression gate or adds to it.
3. What the T10 gate part is. TecTech's generators stop at tier 9, so T10 needs a new part in P5.

**Alternatives for mixing.** One uniform tier per type, as TecTech does: stricter, and a swap un-forms the Eye.

### B-4 Should planets still need TecTech's compression tier on top of the 1:1 gate? [Q-29]: RULED, DONE

**Context.** TecTech's planets each need a minimum compression tier (R7-R8 planets need tier 7-8). We kept that rule
on top of your 1:1 gate. So a T3 Eye with exactly tier-3 compression cannot run R7-R8 planets, even though they are
"T3 planets". The player has to over-build compression to reach them.

**Suggestion.** Drop TecTech's per-planet rule and let the 1:1 gate decide alone. It is simpler, and it is what the
tier names promise.

**Alternative.** Keep both rules, as built. Higher compression tiers then stay useful inside a tier.

**Ruled (round 4): drop it. Done.** The Eye no longer checks a planet's compression minimum; the 1:1 gate alone
decides, so an Eye at tier N runs every planet mapped to tiers 1..N. One judgement call is in B-16 (the -3 % time
reward).

### B-5 Eye balance: outputs, run times, power, success [Q-5, Q-6, Q-31]

**D-49 status (unit 6).** Stars are now per-cycle (60 s) with upkeep, EU per cycle and output x mean Y x r (`StarDef`, plan §6.1); success is no longer rolled for stars. Q-31's batch-run model is replaced. The numbers below apply to planets only.

**What we built** (every number is a proposal):

| Tier | Star | In → out per run | Time |
|---|---|---|---|
| T1 | Sol | 1e9 L hydrogen + 1e9 L helium → 1,000,000 L Raw Star Matter | 20 min |
| T2 | Sirius A | 1,000,000 L Raw Star Matter + 1e9 L helium → 100,000 L Excited Stellar Plasma | 40 min |
| T3 | Arcturus | 100,000 L Excited Stellar Plasma + 2e9 L hydrogen → 50,000 L Raw Hyperdense Stellar Plasma | 80 min |
| (Kerr T2) | densify | 10,000 L Raw Hyperdense Stellar Plasma → 1,000 L Hyperdense Stellar Mixture | 20 s |
| T4 | Sirius B | 5,000 L Hyperdense Stellar Mixture + 4e9 L helium → 18,432 L White Dwarf Matter | 120 min |

The White Dwarf figure matches TecTech's best per-run amount, at about 5× its speed.

**How a run works:**

- **Power:** wireless EU only, paid as one lump when the run starts. It costs UMV-recipe power ×1 / ×4 / ×16 / ×64
  for T1-T4; a Sol run is about 3 × 10¹² EU. No EU comes back (you ruled it a pure sink).
- **Parallel:** a higher-tier Eye runs lower recipes in parallel, ×2 per tier of difference. A T4 Eye runs Sol ×8.
- **Success:** stars always succeed. Planets keep TecTech's 1 − 5 % per rocket tier, rolled per parallel. A failed
  parallel eats its inputs.
- **Planets:** outputs go to an ME output bus and hatch only, as with TecTech. A run takes 20 min × 1.25 per rocket
  tier; the Deep Dark takes 4 h.

**Suggestion.** Play-test, then tune one knob per tier.

**Alternatives.**

- Raise the White Dwarf rate so it is not scarce before P4.
- Roll a planet run once, all or nothing, instead of per parallel.
- Laser or energy hatches instead of wireless.

### B-6 Eye T2 and T3 unlock by building the band; band names [Q-2, Q-11, Q-13]

**D-49 status (unit 6).** Bands are cumulative now: T3 needs T2 (my answer 2), and every tier also needs its transition. The band names are unchanged.

**Context.** The design says each band is "unlocked by an upgrade core", but cores exist only for T4, T7 and T10.
Eye T2 is also optional by design (D-42). You accepted Eye T2 as the interim Excited Stellar Plasma source while the
Homogeny is on hold, so T2 is the de-facto path to T3 for now.

**What we built.**

- T2 recipes need the beam band. T3 recipes need the jet crescent, which works without the beam band. T4 needs the jet
  crescent, the spiral arms and the Galactic Upgrade Core.
- The bands cost only their casings. Each band only adds blocks, so nothing is torn down when the Homogeny arrives.
- Star picks: Sirius A (T2), Arcturus (T3) and Sirius B (T4, Sirius A's white-dwarf companion). The T3 product is
  named "Raw Hyperdense Stellar Plasma", the card's name.

**Need from you.** The tier/band names. The code says Planetary/Galactic; mine.md proposed Stellar · Systems ·
Galactic · Universal.

**Alternative.** A "Stellar Upgrade Core" for T2 and T3 as well.

### B-7 Which planets map to which Eye tier, and what they lose [Q-12]

**What we built.**

- Our Eye copies TecTech's 43 planet recipes, keeping their ores and plasmas.
- It drops TecTech's White/Black Dwarf Matter, Universium and Raw Star Matter outputs. Sol is the only Raw Star Matter
  source, which gives T1 a reason to exist.
- Rocket tiers map as R0-R3 → T1, R4-R6 → T2 and R7-R8 → T3. The Deep Dark → T4.

**Suggestion.** As built.

**Alternative.** Let planets keep a small Raw Star Matter by-product.

### B-8 Kerr balance: the hole's numbers [Q-8, Q-25, Q-26]

**Context.** You ruled the Kerr keeps its mass mechanic.

**What we built** (every number is a proposal):

- **The hole:** loses mass over time. A governor keeps it at the setpoint by firing SpaceTime bolts from the bolt
  buses. The setpoint is 2-64 SM at T1 and up to 512 SM at T2, default 8.
- **Mass trade-off:** parallel grows with mass and voltage tier (8 × tier × √mass). Speed is ×4 for a small hole
  and falls to ×1 as the hole grows. The spec's formula made big holes *slower* than ×1, so we clamped it at ×1.
  Throughput peaks around 16 SM, and a 512 SM hole is about ×22.
- **Closed hole:** only plain compressor recipes run. Heated and black-hole compressor recipes need the hole open.
  This is a little kinder than the Black Hole Compressor, which does nothing without a hole.
- **Injector rails:** can be mixed Mk I/II/III, and the lowest tier sets the bolt cap (4/8/16 per tick). Upgrading
  232 rails one by one never un-forms the Kerr.
- **Broken structure:** the hole freezes; it neither drains nor evaporates.
- **Bolt buses:** must be plain input buses, not ME stocking buses.

**Suggestion.** As built; tune in play.

**Alternatives.**

- No speed clamp, so a big hole is purely a parallel play.
- The hole keeps draining while the structure is broken.
- One rail tier for the whole structure.

### B-9 Eye fluid inputs: any input hatch takes any Eye fluid [Q-30]

**D-49 status (unit 6).** Partly reversed: the 8 Field sockets are unbuffered (trims and Time are drained live, ME stocking hatches refused) and the Sensor sockets take only the Stellar Field Sensor Hatch. The gas and stellar sockets still buffer any Eye fluid.

**What we built.** The Eye pulls every fluid it uses from every input hatch in its sockets, every second, into one
big internal tank per fluid. These tanks are saved, and they survive a wrench. A T1-only Eye can therefore take Raw
Star Matter through its plinth hatches, and ME stocking hatches work.

**Suggestion.** As built: less fiddly.

**Alternative.** The spec's version: gas hatches on the plinth take only hydrogen and helium, and band hatches take
only stellar fluids.

### B-10 The Eye's centre render is not built [Q-34]

**D-49 status (unit 6): BUILT.** See B-45. The Eye draws its star, neutron star, cooling dwarf, magnetar and the Core Collapse at its centre, behind `eyeRenderCentre` and a per-Eye GUI switch.

The centre of the ring is empty for now. The `eyeRenderCentre` config option exists but does nothing yet.

**Suggestion.** Build it after the in-game test, reusing TecTech's orbiting-planet render scaled to our 80-block ring.

**Alternative.** Leave the Eye without a render.

### B-11 Where the Eye's bands meet, the earlier piece keeps the overlapping blocks [Q-19]

**Context.** In the mockup, three Eye band joins overlap, and the Kerr's module arcs overlap the frame ring.

**What we built.**

- **Eye:** the earlier piece keeps those blocks, and the later band starts where it ends. The spiral-arm roots butt
  against the core band instead of sinking into it.
- **Kerr:** the module arc owns its overlap. Without the module, the frame ring has a 49-block gap there.

**Suggestion.** As built; the difference is a few blocks at each join.

**Alternative.** Adjust the mockup so nothing overlaps.

### B-12 The Kerr's inner lining reads darker than the mockup [Q-16]: RULED, DONE

**Context.** You ruled to reuse the Black Hole Compressor's blocks. The mockup's lining is bright white, and the BHC
has no white casing, so we used its Background Radiation Absorbent Casing. The Eye's white-ish casing (Infinite
SpaceTime Energy Boundary) is post-UMV, too late for the UIV Kerr.

**Suggestion.** Accept the darker look.

**Alternative.** One extra new "Harmonic Lining" block just for the Kerr.

**Ruled (round 4): one new white Kerr lining block. Done.** Harmonic Lining, `slCasing2:2`, white with the BHC's
rim and lavender shading; every Kerr `J` cell uses it, and the Kerr shapes' manifest was re-exported. The choices made
are in B-15.

### B-13 Textures are generated from code [Q-15]

**What we built.**

- All 33 new textures (16 casings, 8 controller overlays, 9 items) come from a committed script, in the Black Hole
  Compressor and TecTech Eye colour families.
- The three new fluids use GT's standard molten texture, tinted: Excited Stellar Plasma gold, Raw Hyperdense Stellar
  Plasma orange-red, Hyperdense Stellar Mixture crimson.

**Suggestion.** Hand-polish only what players stare at: the two controller faces and the mirror and bastion walls.

**Alternative.** Hand-drawn textures; the file names stay the same.

### B-14 Smaller as-built choices: say if you disagree

- **Raw Stellar Plasma Mixture is the pack's existing Raw Star Matter [Q-1].** The pack already names it "Condensed Raw
  Stellar Plasma Mixture". One fluid, not two, so the Godforge keeps its fuel.
- **The Kerr's utility-hatch sockets [Q-21, Q-27].** The eight spots beside the bolt buses optionally take a Black
  Hole Compressor Utility Hatch. It gives a redstone signal while the hole is open.
- **Each Kerr module has its own hatches [Q-23].** The compressor never eats the extractor's inputs, and recipes never
  touch the bolt buses.
- **The Kerr re-checks its structure on a timer [Q-37].** It re-checks every 10 s idle or every 50 s running, and
  never while part of it is unloaded. Without this, a module built far from the controller would not register
  until a relog.
- **Assembler recipes have one fluid slot [Q-17].** Four casing cards had two fluids, so one of them became
  foil or ingots.
- **Dense SpaceTime plates [Q-18, Q-32].** The fallback for a missing dense plate never triggered in the boot log, so
  the pack has them and the recipes use real dense plates. Closed.
- **Every assembly line has its own research item [Q-36].** The boot log confirms all 9 are unique, so every one can
  be put on a data stick.
- **Eye controller recipe [Q-32].** It uses Tier-0 (Crude) field generators, as TecTech's own controller does, not
  Gallifreyan. Gallifreyan would have forced all 24 field-generator research steps before a T1 Eye, and it is
  now the T9 gate.

### B-15 Harmonic Lining: which meta, its recipe, no light [follow-up pass]

**Context.** You ruled one new white Kerr lining block, in a reserved casing meta, with a fair recipe.

**What we built.**

- **Meta:** `slCasing2:2`, the meta the spec reserved for "Harmonic Lining" and left as a hole when the BHC casing took
  the role. The other holes (`slCasing:0` deck, `slCasing:9` eye shell) belong to other roles, so they stay holes.
  The name is the card's, "Harmonic Lining". The Eye keeps TecTech's Infinite Spacetime Energy Boundary Casing as its
  lining; the new block is Kerr-only.
- **Recipe SL-P12** (Assembler, UIV, 30 s): 6 Neutronium plates, 8 Infinity foil, 1 Transcendent Metal plate and
  576 L molten Infinity make 4 linings. Per block that is 1.5 Neutronium, 1.5 Infinity (foil is ¼ ingot) and ¼
  Transcendent Metal.
  - A Kerr T1 with both modules needs 521 (131 in the T1 cage, 253 in the compressor arc, 137 in the extractor
    arc): about 790 Neutronium, 790 Infinity and 130 Transcendent Metal.
  - A Kerr T2 needs 4,552: about 6,800 Neutronium, 6,800 Infinity and 1,140 Transcendent Metal.
  - For scale, the 3,551 Mirror Backing casings cost about 7 Infinity each. The block it replaces, the BHC's
    Background Radiation Absorbent Casing, costs 64,000 L of UU-Matter each, so the old mapping was far dearer.
- **No light.** The spec once said "emissive (light 12)". The texture is bright, and making 4,500 blocks light sources
  would cost lighting updates on every build and hologram, so it does not emit light.

**Suggestion.** As built.

**Alternatives.** Make it glow (light 8-12) for the mockup look. Price it like the mirror casings if the Kerr T2
should cost more.

### B-16 The planet time reward stays, with no penalty below a planet's old tier [follow-up pass]

**Context.** With TecTech's per-planet compression minimum gone (B-4), its other half was the -3 % time per
compression tier above that minimum.

**What we built.** The reward stays: each compression tier above the planet's TecTech tier still cuts 3 % of its
time. Below that tier there is no penalty, because the gate alone decides. Stars never use it. The NEI page now says
"-3%/field > T*n*" where it said "field T*n*+".

**Suggestion.** As built: it keeps a reason to over-build compression without gating anything.

**Alternative.** Drop the reward too, so compression is purely the tier gate.

### B-17 The extractor module takes item buses only; extractor runs and the hole [follow-up pass]

**What we built.**

- GT's extractor map is items in, items out, so the extractor's two sockets accept input and output **buses** only.
  A fluid hatch there faults the module. `/btskerr` and `/btskit kerr` now place and give output buses.
- Extractor recipes need no open hole (they carry no compression tier). They run at the base parallel (8 × voltage
  tier) with the hole closed, and get the hole's parallel and speed bonuses when it is open, like plain compressor
  recipes.
- The Kerr Extractor Module core (SL-K03) and the Ergosphere Extraction Casing (SL-P14) keep their names, which were
  designed around fluid extraction.

**Built (recipe audit pass):** pistons, cost audit #14. SL-K03 takes 32 Electric Pistons UIV in place of the 32
Electric Pumps (fc3eff4), and SL-P14 takes an Electric Piston UIV in place of its pump (6b7346b), so the parts match
an item extractor.

**Suggestion.** As built: keep the names, with pistons.

**Alternative.** Revert both recipes to Electric Pumps.

### B-18 D-49 star lifecycle and stability field: the proposal and its open choices [follow-up pass, design only]

**Context.** D-49 redesigns the Eye: live T1-T3 stars, a T4 neutron star reached by Core Collapse, T5 black-dwarf
cooling, a T6 magnetar on Antipolarization Agent, and a stability field on every tier. Three designs were judged and
merged into one proposal: design repo `designs/docs/star-lifecycle.md`, everything PROPOSED. Nothing is built. The
Eye freeze holds until you accept it.

**In short.**

- Stars run in 60 s cycles at today's per-minute economy.
- The field is a gauge model on every tier: P, then Θ from T3, Ω from T4, and Π plus an agent shield at T6. Zones
  set the yield. Integrity stops one bad second from killing a star.
- A new sensor hatch (MTE 28116) and unbuffered Field sockets make the field automatable with threshold rigs. This
  is simulated.
- Tiers unlock in sequence: milestone, then core, then transition event.
- This supersedes A-2/B-1, answers B-3, and changes B-5, B-6 and B-9 (partly).

**Open choices** (recommendation first; details in §7 of the doc):

1. **T2 optional?** Required. Alternative: a direct Sol → red giant route that takes 5.7× longer.
2. **T5 cooling.** Subspace Cooling (DSSF) plus the general time dial. Alternatives: Urca + subspace, chrono-cooling
   or a phonon drain.
3. **Failure.** Lose the star, in-flight output and event materials. Cores are consumed only on success. No block or
   tier loss.
4. **T3 star.** Keep Arcturus, pushed past 1.44 M☉. Alternative: Betelgeuse.
5. **Time dial.** Tachyon Rich Temporal Fluid buys output per hour at a harder field, capped by the Time Acceleration
   tier above the Eye's tier. Alternative: the dial only at T5.
6. **Trim budgets** (Super Coolant, Purified Tengam, SpaceTime). Accept, and tune after a play test.
7. **Antimatter for the Agent** (about 10 L/s at T6). Check the Antimatter Forge's throughput before P4.
8. **One live star per Eye.** Yes.
9. **T4 HSM feed up to 1.5 L/s** (about 2.16× today's WDM rate). Yes.
10. **A wrench during a transition.** Aborts it: event materials are lost, the core is kept.

### B-19 NEI preview: a large render and no channel sliders [acceptance pass]

**Context.** You asked that both controllers show in NEI's multiblock preview at every tier, and that any size cap be
logged.

**What we found.** BlockRenderer6343 has no block-count or footprint cap. Its only hard limit is the scratch world's
height (y 0-255), and ours fit (Kerr y 32-96, Eye y 63-119). The preview counts **2 tiers on the Kerr and 4 on the
Eye** by building each tier until one adds nothing, then draws each tier through our survival auto-place, hatches
included. Two catches:

- **Frame rate.** The preview redraws every block every frame, with no caching. A T4 Eye (34,502 blocks) or a Kerr
  T2 (29,839) may stutter while the tab is open. The layer slider (one y-layer at a time) stays fast.
- **No field-generator sliders.** TecTech's own Eye gives NEI a slider for each field generator. If ours did the
  same, NEI would also stretch the main tier slider to 1-9 (the generators' tier count), and tiers 5-9 would show
  the T4 Eye again. We kept the slider at 1-4. The generators follow GT's EOH channels on the projector only.

**What we built.** As described; no fallback was needed. The spec section "Hologram & NEI" has the details.

**Suggestion.** Keep it as built, and check the frame rate in game. If the T4 Eye is too slow, the cheapest fallback
is a lighter preview: implement GT's `INEIPreviewModifier` and leave the band interiors (the linings and glass, about
a third of the blocks) out of the NEI build only. The real structure would not change.

**Alternatives.** Expose the generator sliders and accept a 1-9 main slider. Or ask BlockRenderer6343 upstream for a
display-list cache, which helps every large GTNH multi.

### B-20 Hologram build rules: budget, rails, and the compression swap [acceptance pass]

**What we built.**

- **Speed.** Survival auto-place now does up to 200 blocks per round, not StructureLib's default 25. It uses the same
  kind of rule TecTech's Eye uses (×5, capped at 200); ours is ×8, capped at 200. A T4 Eye or a Kerr T2 then takes
  about a minute of holding the projector, not about seven.
- **Tiered parts are never downgraded.** Re-projecting over upgraded parts no longer replaces them with lower tiers,
  in creative or in survival. This covers the Kerr's Mk II/III rails and the Eye's field generators. A creative
  projection used to downgrade Mk III rails to Mk I.
- **Survival takes what you have.** If the player has no Mk I rail or no tier-N generator, survival places the next
  higher tier they do have.
- **Compression swaps are by hand.** Survival auto-place never breaks blocks. To take an Eye from T1 to T2, the
  player must swap the core's 188 compression generators for tier 2 before the projector places the beam band.
- **Projector channels.** GT's `spacetime_compression`, `time_dilation` and `stabilisation` channels raise the
  generator tier a build places. Compression never goes below the stack's 1:1 gate.

**Suggestion.** As built.

**Alternatives.** A bigger round (more server time per projector use). Or let survival skip the cells that hold a
too-low compression tier and still build the later bands, leaving the gate to refuse the recipe until the player
swaps them. That builds faster, but the half-upgraded Eye would look finished.

### B-21 The Kerr Density Upgrade needs an item no one can craft [cost audit]

**Context.** SL-K04 takes one Superstable Black Hole Seed (`ItemList.Black_Hole_Stabilizer`). A scan of every class
in all of the instance's mod jars finds it only in GT's item list, its registration, the BHC, and our SL-K04. No
recipe makes it. So Kerr T2 is uncraftable, and so is everything after it: no DENSIFY, no Hyperdense Stellar
Mixture, no Galactic Shell, no Galactic Upgrade Core, no Sirius B chart, no Eye T4. The boot log could not see this:
the recipe registers fine, it just can never run.

**What we built.** Nothing yet; this audit changes no code.
**Built (recipe audit pass):** the suggestion. SL-K04 takes 32 Black Hole Seeds (`BTSRecipesKerr.DENSITY_UPGRADE_SEEDS`).

**Suggestion.** Replace the Superstable Seed in SL-K04 with 32 Black Hole Seeds (UHV Space Assembler). That costs
about the same, fits the "density" theme, and touches nothing outside the addon. Costs doc, adjustment #1.

**Alternatives.**

- Give GT's Superstable Seed a recipe of ours. This also unlocks the BHC's superstable mode pack-wide (placed BHCs
  included).
- A new BTS "Kerr Seed" item.

### B-22 Eye T2-T4's compression generators need White Dwarf Matter, which only Eye T4 makes [cost audit]

**Context.** The 1:1 gate (Q-20) needs TecTech tier 2 / 3 / 4 Spacetime Compression Field Generators (metas 1-3) for
Eye T2 / T3 / T4. Their TecTech recipes take 2 / 8 / 32 **White Dwarf Matter bolts** per generator: javap,
`addEOHRecipes`, a shared per-tier bolt array of Shirabon, WDM ×3, BDM ×3 and MHDCSM ×2. With
`replaceTecTechEyeOfHarmony=true`, WDM's only source is our Eye T4. So Eye T2 cannot be built without Eye T4: a
deadlock. The spec's §0 note ("no WDM/BDM markers in the generator recipes") only checked the Tier-8 recipe.

**What we built.** Nothing yet.
**Built (recipe audit pass):** SL-P30..P32, with the optional upgrade folded in; see B-28.
**2.9.0-RC-2 port:** superseded. RC-2 makes the generators in the Condensate Assembler and their WDM now comes in
through Resonance Chamber II; SL-P30..P32 are retired and SL-P39 replaces them (B-29).

**Suggestion.** Three alternate assembly-line recipes of ours (SL-P30..P32, registered only with the flag on):
TecTech's own inputs, research and duration, with the WDM bolts replaced by the previous tier's product as a 4th
fluid: Raw Star Matter 16,000 L, ESPM 4,000 L, RHSP 2,000 L. Per tier step that is 3 Sol, 7.5 Sirius A and 7.5
Arcturus runs. Optionally, add an upgrade recipe (tier m → m+1, consuming the old generator), because every step now
strands 188 generators: 1.7e16 EU and about 1,900 assembly-line hours for T2-T4 (costs doc, #2 and #15).

**Alternatives.**

- Make the dedicated tier-gate block from B-3 the gate, with its own recipes, and stop gating on compression.
- Keep TecTech's rocket-tier 0-2 planets' WDM output (B-2 option) as a trickle.

Both need D-49 (B-18) in view.

### B-23 The Eye's TecTech casings cost 8× TecTech's whole Eye [cost audit]

**Context.** The ruling to reuse TecTech's Eye blocks put them in bulk roles:

- 13,872 Reinforced Spatial/Temporal casings in the core (TecTech's Eye: 1,430);
- 6,426 Infinite Spacetime Energy Boundary casings across T1-T4 (TecTech's: 36).

TecTech prices them for its small Eye. A Spatial/Temporal casing is ¼ of an assembly line with **32 Godforge
Singularity Shielding Casings**. A Boundary casing is a whole LSC, 16 Active Transformers and 4 UMV wireless hatches.
An Eye T1 today needs 118k Godforge casings (42 Godforge main rings, 8× TecTech's Eye), 3,876 LSCs and 2.3e16 EU.

**What we built.** Nothing yet.
**Built (recipe audit pass):** the suggestion, SL-P27..P29, plus the band batch sizes (#8-#12); see B-26.

**Suggestion.** Keep the blocks (and the look) and add batch Assembler recipes of ours, registered only with
`replaceTecTechEyeOfHarmony=true` (since changed: they now register whatever the flag says, B-26):

- SL-P27/P28: 1 Godforge Shielding Casing + dense Neutronium/Cosmic Neutronium/Infinity + SpaceTime → 4 casings.
- SL-P29: SC UMV wire + dense Neutronium/Cosmic Neutronium + 1 Emitter UIV + SpaceTime → 8 Boundary casings.

With B-24 this puts Eye T1 at TecTech parity in EU (5.5e15), assembly-line hours (612) and UXV circuits, and at ¼ of
TecTech in Godforge casings. The bands' own recipes get batch outputs too (#8-#10). Costs doc, #3, #4 and #8-#12.

**Alternatives.**

- New BTS look-alike blocks (same textures) for the bulk roles. This means a legend change and new metas.
- Map most shell cells to cheaper existing blocks. This changes the look.

Side effect of the suggestion: cheaper TecTech casings also make TecTech's field generators and its Stargate parts
cheaper, but only while TecTech's own Eye is switched off.

### B-24 294 placeholder stabilisation generators under the pylon feet [cost audit]

**Context.** The six 7 × 7 pylon pads are 294 Stabilisation Field Generators (spec, Eye `Y`). They do nothing yet
(B-3). Each one is crafted from a compression generator, a dilation generator and a Boundary casing. Together they
cost 1,176 UXV circuits and 9.5e15 EU, which is 1.8× TecTech's whole Eye. TecTech's Eye uses 48.

**What we built.** As exported.
**Built (recipe audit pass):** the suggestion. The exporter tags only each pad's centre ring (8 cells around the
foot) as `Y`; `eye_core` is re-exported with `Y` 48 and `Q` 4,704. The tooltip, kit and hologram read the counts
from the shape, so nothing else changed.

**Suggestion.** 48 generators, 8 per pad (the pad's centre ring), with the other 246 cells as plinth casing (`Q`).
This is an exporter-legend change for the structure owner. Costs doc, #5.

**Alternatives.**

- Keep 294 and give stabilisation a real effect that justifies them.
- One generator per pad (6).

### B-25 D-49 play prototype: two readings of the proposal, and a weak supernova check [follow-up pass, design only]

**Context.** A playable page of the D-49 proposal (B-18) was built in my local work folder
(`lifecycle/star-lifecycle.html`). It runs the §3.2 field update once per second with the §3.4 numbers, the
sensor-hatch rigs and the T3 → T4 Core Collapse. Its engine reproduces the proposal's simulated-rig table (for
example T3 two-stage: 99 % Resonant, 0 failures, Y 1.29; T6 left alone: 411 failures in 4 h). No addon code changed;
the Eye freeze holds.

**Judgement calls in the prototype.**

1. **The T4 HSM feed push and the T6 WDM + BDM feed push scale with the time dial m.** §3.4 lists them under drift
   d, which §3.2 multiplies by m; `judge/synthsim.py` adds them unscaled, as trims. The two agree at m = 1.
   *Suggestion:* scale them with m. The feed is upkeep, upkeep is drawn per Eye-second, so a faster star eats and
   pushes faster. *Alternative:* treat them as trims, as in synthsim.
2. **T5's pressure trim is Helium (P +2 per 1,000 L, cap 10)**, as the proposal says. synthsim used molten
   SpaceTime with the same numbers, so the results carry over.

**Finding: the Core Collapse rarely threatens a supernova.** The bounce adds +90 to P and Θ, and a supernova needs a
gauge above +100. So the SpaceTime buffer only matters if the field sits above +10 at that second. In five seeded
runs with no SpaceTime buffered, the two-stage rig survived all five bounces and the bang-bang rig went supernova
twice.

- *Suggestion:* raise the bounce to +150 and keep "1,000 L absorbs 1 point, up to 90,000 L". A centred field then
  needs at least 50,000 L, and a tidy rig about 60,000 L.
- *Alternatives:* keep +90 and fail the bounce above +40 (the Stable band), which also needs about 50,000 L; or keep
  it as written and treat the collapse as a holding test.

### B-28 Compression generators: upgrade the old tier, researched from the star charts [recipe audit pass]

*Numbered B-25 until the follow-up review; renumbered because the D-49 prototype section above already had B-25.*

**Context.** B-22: Eye T2-T4 need TecTech's tier 2-4 Spacetime Compression Field Generators (metas 1-3), whose
recipes take White Dwarf Matter bolts, and WDM has no source while TecTech's Eye is off. The audit proposed
TecTech-copy recipes with a stellar fluid in place of the bolts (#2), and separately an optional "upgrade the old
generator" recipe (#15), because every tier step strands 188 generators.

**What we built.** One recipe per step, #2 and #15 combined (SL-P30/P31/P32, only with
`replaceTecTechEyeOfHarmony=true`):

- tier t-1 generator + TecTech's own tier-t items and fluids (javap'd from `addEOHRecipes`: Yotta cells, Quantum
  Tanks, Infinity Chests, Cosmic Fabric Manipulators, AE2 Singularity cells, UXV circuits, Mutated Living Solder,
  Space, SpaceTime) **→ tier t generator**;
- the WDM bolts become a 4th fluid: Raw Star Matter 16,000 L / ESPM 4,000 L / RHSP 2,000 L;
- the Reinforced Spatial casing TecTech's recipe takes is replaced by the old generator;
- 80,000 ticks flat at UMV (TecTech: 80,000 × (t+1));
- research-station assembly lines with TecTech's research cost for the tier, but researched from **Star Chart:
  Sirius A / Arcturus / Sirius B**. TecTech's own recipe already researches from the generator below, and a
  research item can serve only one recipe.

TecTech's recipes stay (they need WDM). Crude (tier 1, the Eye T1 gate) keeps TecTech's recipe.

Effect: a tier step costs 188 upgrades at about 1.9e15 EU and 209 assembly-line hours, the same at every step, where
re-crafting from scratch cost 3.8 / 5.7 / 7.6e15 EU and 418 / 627 / 836 h. The fluid gate is unchanged (3 Sol,
7.5 Sirius A, 7.5 Arcturus runs per step). A fresh generator of tier t now takes t+1 crafts from a Crude one.

**2.9.0-RC-2 port:** retired. TecTech's research-station generator recipes no longer exist in RC-2, and the ruling
keeps RC-2's generator costs; see B-29 (SL-P39) and its alternative for an RC-2 upgrade path.

**Suggestion.** As built. It fits D-49 (compression stays the 1:1 gate, tiers unlock in sequence).

**Alternatives.**

- Two recipes per step, as the audit listed: a from-scratch copy (Spatial casing, TecTech's duration) and the
  upgrade. That needs three more unique research items.
- Remove TecTech's tier 2-4 recipes under the flag and give ours TecTech's research items (the generator below).
  Cleaner in NEI, but it is more recipe-removal code.

### B-26 Batch TecTech casings: told apart by a programmed circuit, registered whatever the flag says [recipe audit pass]

**Context.** B-23's SL-P27 (Spatial) and SL-P28 (Temporal) have the same inputs in the audit's proposal, and GT's
Assembler cannot hold two recipes with the same inputs.

**What we built.** Programmed circuit 1 (Spatial) and 2 (Temporal); circuits are not consumed. TecTech's own
assembly lines stay.

- **The flag (follow-up review).** SL-P27..P29 first registered only with `replaceTecTechEyeOfHarmony=true`. But our
  Eye is craftable with the flag off too, and then its 13,872 Spatial/Temporal casings came only from TecTech's
  assembly line, about 111k Godforge casings: the "8× TecTech's whole Eye" cost of B-23. So SL-P27..P29 now
  register whatever the flag says. Only SL-P30..P32 follow the flag: they exist because White Dwarf Matter loses its
  source when TecTech's Eye goes, and with the flag off TecTech's Eye still makes it.
- **The side effect.** A pack with the flag off can also build TecTech's own Eye from the cheaper casings, so it is
  no longer at TecTech's stock price.

**Suggestion.** As built.

**Alternatives.**

- Gate SL-P27..P29 on the flag again, which keeps TecTech's Eye at stock price but puts our Eye back at the 8× cost
  when the flag is off.
- Also remove our Eye's controller recipe when the flag is off, so a flag-off pack has TecTech's Eye only.
- TecTech's own markers (a Quantum Chest IV for Spatial, an Accelerator UV for Temporal), at one per
  4 casings. For the 14,000-odd casings that is about 2,350 Quantum Chests and 1,180 World Accelerators, which is why
  we did not.

### B-27 Headless structure test: what it covers, and the console forms of the build commands [structure test pass]

**Context.** No one had built the structures in a world yet (A-3). The test ran on the gradle dev server
(`runServer`, flat world, no player), driven through the server console's stdin.

**What we built.** Console and command-block forms of the two build commands:

- `/btskerr [t1|t2] <x> <y> <z> [north|south|east|west] [force] [survival]`
- `/btseye [t1..t4] <x> <y> <z> [facing] [gen n] [fill] [force] [survival]`

`~` makes a coordinate relative to a command block. The facing is the way the controller's front faces, default
north. Each command:

1. loads the footprint's chunks first, with a 1-chunk margin, so world population has run before anything is placed;
2. builds through `KerrKit` / `EyeKit`, as the player form does;
3. prints FORMED or NOT FORMED, and the first wrong cells with what stands there;
4. runs a mirror check, reading where the mockup's +x landmark actually stands: the Kerr's extractor core, the Eye
   core's lone east gas hatch, and the jet band's east socket;
5. re-projects the creative hologram over the formed machine and checks it is still formed, with no field
   generator changed;
6. runs a damage-and-repair test: it knocks out the plain block next to the controller, checks that the walk names
   that cell, runs the survival auto-place, and checks that it puts back exactly that block, leaves every tile entity
   alone, and the machine forms again.

`survival` builds the whole machine through `survivalConstruct` instead of `construct`: projector-sized rounds (25,
scaled to 200), from an endless item source, with a Forge fake player as the actor. `check` builds nothing and
re-walks the machine already standing at x y z. A console-built Eye is owned by a fixed test UUID
(`beyondthestars:console-test`) for its wireless EU. All of this lives in `machines/SLConsoleTest.java`.

**Results (every case FORMED):**

| Case | Facings | Path |
|---|---|---|
| Kerr T1 + both modules | N, E, S, W | creative N/E/S; survival E/W (15,362 blocks in 79 rounds) |
| Kerr T2 + both modules | S, W, E, N | creative S/W/E; survival N (29,838 blocks in 151 rounds; trigger stack 2 before the upgrade) |
| Eye T1 | N, E, S | creative N/E; survival S (19,615 / 100 rounds) |
| Eye T2 | E, N, E (`~` coords) | creative |
| Eye T3 | S, W, W | creative S/W; survival W (27,942 / 142 rounds) |
| Eye T4 | W, N (`fill`), S, E | creative W/N/S; survival E (34,501 / 175 rounds) |

- **Survival counts.** Each survival count is the §5.1 assembled size minus the controller, and every run ended
  with -1 (no stall).
- **Orientation.** On every case the +x landmark stands on the viewer's right, as in the mockup. The exporter's
  `--flipH` / `--flipD` stay off.
- **Re-projection and repair.** A creative re-projection always left the machine formed. The damage test always
  named the missing cell ("Expects Extreme Density Space-Bending Casing / Reinforced Temporal Structure Casing at
  ..."), and the survival repair placed exactly 1 block. Tile-entity counts were unchanged (20 → 20 on the Kerr
  and Eye T3/T4).
- **Restart.** After a restart, all 11 machines rechecked with `check` were still FORMED: modules, Density Upgrade,
  Galactic core and bands all came back.

**Also fixed.** GT's multi-line "Any of:" structure errors split the Eye/Kerr band and module fault strings over
several lines in the scanner and status output. `BTSStructures.oneLine` now flattens them to "Any of: Stellar /
Beam Band Casing".

**Not covered by a headless run.**

- The client-side hologram hints (`construct(stack, true)`).
- Survival build from a real AE system or player inventory. The endless source supplies exact stacks only, so a
  hatch socket gets its casing and `KerrKit` / `EyeKit` then fit the hatch.
- The NEI preview.
- The 1:1 compression gate and recipe runs (D-49 is in progress, so this pass leaves the star code alone).

**Dev runtime notes.** The dev server loads 63 mods. It has no `dreamcraft` (GTNewHorizonsCoreMod) and no
AvaritiaAddons, and the code tolerates both:

- SL-P30..P32 are SKIPPED: "could not resolve AvaritiaAddons InfinityChest". The dev log says 231 registered,
  3 skipped.
- The Black Hole Compressor controller removal and TecTech's Eye controller removal both log "NOTHING matched ...
  the removal is a no-op". TecTech's 43 Eye recipes are still removed, and our Eye has its 48.

In the full pack these resolve, as the earlier smoke test showed (255 registered, 0 skipped).

**Observation, not a structure problem.** With no player online, the server unloads chunks after a command
finishes. A machine built from the console stops ticking until something loads it again. Two of the T2 Kerrs
still showed "Hole closed" at `check` time, because their seed had not been fired yet. In game, the spec's "keep
the whole footprint chunk-loaded" advice stands.

**Suggestion.** Keep the console forms. They are op-only, cost nothing in play, and give a regression test for
every future structure change: re-export, `runServer`, then pipe the command list in.

**Alternatives.** Move the bench into a dev-only source set so it does not ship in the jar. That needs a
buildscript change for about 400 lines of op tooling.

### B-29 RC-2's Eye T2-T4 compression generators still need White Dwarf Matter, through Resonance Chamber II [2.9.0-RC-2 port]

**Context.** In RC-2 every Spacetime Compression Field Generator is a Condensate Assembler recipe, and TecTech's old
WDM bolts are gone, so the port plan read tiers 1-3 as WDM-free. They are not: tiers 1-3 (the Eye T2 / T3 / T4
gates) take 1 / 4 / 16 MetaMaterial **Resonance Chamber II**, and each chamber takes one **White Dwarf Matter foil**
(`BECMetaMaterialRecipes`, the 4-tier "gray" material). With TecTech's Eye replaced, WDM first exists at our Eye T4,
so Eye T2 would need Eye T4: the same deadlock as B-22. The full per-tier audit (all three generator families and
the EOH casings) is costs doc §9.4; this is the only loop in P1-P3.

**What we built.**

- **SL-P39**: GT's Resonance Chamber II recipe with **2 SpaceTime foils** in place of the WDM foil; everything else
  as GT's (Condensate Assembler, nanite tiers Six-Phased Copper / Silver / Gold / Silver, Chromatic Glass, Infinity and
  Boundless Cosmic Solder condensates, 300 s at UIV). Registered only with `replaceTecTechEyeOfHarmony=true`. GT's
  recipe stays. The chamber keeps its Boundless Cosmic Solder (made from Raw Star Matter), so the generators still
  need Eye T1's product, and each star chart still needs the tier below.
- **SL-P30..P32 retired** (B-28's upgrade recipes). The ruling keeps RC-2's generator costs, their inputs were a copy
  of TecTech's beta-1 recipe that no longer exists, and `TTRecipeAdder` now charges the research EU/t they declared
  (`RECIPE_MAX`). The IDs stay reserved.
- SL-P27..P29 kept: RC-2's BEC casings cost what TecTech's assembly lines did (8 Godforge casings per Spatial/Temporal
  casing, a whole LSC per Boundary casing).

**Suggestion.** As built.

**Alternatives.**

- Another stand-in for the WDM foil: a Raw Star Matter or ESPM product (the chamber already carries RSM through the
  solder), or a Shirabon / Transcendent Metal foil.
- Rewrite GT's own chamber recipe in place under the flag (like the BHC swap), so NEI shows one recipe, not two.
- Bring back an upgrade path on RC-2's terms: a Condensate Assembler recipe tier t-1 generator → tier t, so an Eye
  tier step stops stranding 188 generators (each now carries a Kerr controller, B-30).

**Settled 2026-10-13 (your answer: "Rewrite GT's in place").** SL-G01 rewrites GT's own Resonance Chamber II
recipe at load-complete to take the 2 SpaceTime foils, so NEI shows one recipe; SL-P39 is retired (id reserved, its
body only a fallback if no GT chamber recipe is found). And the upgrade path is back: B-83.

**Numbering.** The D-49 build plan (§10.3) claims SL-P33..P38: P33/P34 for conditional Eye T5/T6 gate recipes, and
P35..P38 for Star Chart: Sirius B, its reclaim, Star Chart: Magnetar and the Stellar Ignition Charge. This recipe is
therefore **SL-P39**. It was SL-P35 in commits c9b2376..the port-review fix, which collided with the plan's Sirius B
chart; logs from those builds say SL-P35 for this chamber. `BTSRecipeGuard.recipe` does not check that IDs are
unique, so a collision would register silently. That plan's "post-port audit" (§14) is answered by costs doc §9.4:
on RC-2 the T2-T4 gates need WDM only through Resonance Chamber II, and the T5 gate needs BDM through Resonance
Chamber III. The plan's §10.3 rows for SL-P32 (retired) and SL-P33/P34 (generator upgrades, retired with SL-P30) are
marked obsolete there.

**For P4/P5 (not built):** Eye T5's gate (tier 4) needs Resonance Chamber III (a Black Dwarf Matter foil, T5's own
product), which only the interim T4 BDM recipe unlocks; Eye T8/T9 (tiers 7-8) need Resonance Chamber IV (an MHDCSM
foil, Universium-based) and Eternity plates. Time Dilation and Stabilisation tiers 1+ also need WDM (Wave Focus II,
Field Manipulator II, Sensor Array II); harmless while the Eye accepts any tier of those (B-3), but revisit if B-3
gives their tiers a meaning.

### B-30 The Kerr controller replaces the BHC controller in recipes, so it is priced like one [2.9.0-RC-2 port]

**Context.** I ruled that, with `removeBlackHoleCompressorRecipe=true`, every recipe consuming a BHC
controller takes the Kerr Ergosphere Engine controller instead. In RC-2 that is all 9 compression generator tiers
(and with them every stabilisation generator) plus the coremod's Empty Spatially Compressed Circuit Array (64). An
Eye T1 consumes 238 controllers (188 `W` + 48 `Y` + 2 in SL-E01) and each later tier step 188 more; TecTech's own
Eye consumes 188. The card's SL-K01 cost about 2,400 SpaceTime ingots and 8 Black Hole Seeds: 571k SpaceTime for one
Eye T1 gate set.

**What we built.**

- `BTSBlackHoleSwap` rewrites every BHC consumer in place at load-complete (every recipe map, the assembly-line data
  and research lists, the crafting table), logs each one, and the BHC recipe is removed only when the Kerr is
  craftable and nothing consumes a BHC any more. Verified in `runServer`: 10 recipes rewritten, then the BHC recipe
  removed.
- **SL-K01 re-priced at BHC-controller parity** (costs doc §9.2): the BHC's compression lineage (HIP + Neutronium
  Compressor, 16 Advanced Implosion, 8 Compressor UIV, 4 Electric Implosion), circuits, field generators, superdense
  plates and three of its four fluids, with the Kerr's identity (1 Black Hole Seed, 1 Superradiant Mirror Segment,
  4 Field Coils, 1 Extreme Density casing, TM screws, Super Coolant) in place of the BHC's lenses, ProtoHalkonite
  parts, Kevlar and Oganesson. About 1.1-1.3× a BHC controller; 3,600 ticks.

**Suggestion.** As built. The Kerr is the BHC's successor (D-44), so it inherits the BHC's role as an ingredient and
its price, and the generators stay at RC-2's stock cost.

**Alternatives.**

- Keep the card's expensive SL-K01 and put a cheaper BTS item (a "Kerr core") in the generators instead. Deviates
  from the ruling's "the Kerr controller", but keeps the Kerr itself a big-ticket craft.
- Keep this SL-K01 and cut the Eye's gate count (188 `W` cells, TecTech uses 138), which scales the whole bill.
- Bring back a generator upgrade path (B-29), so each tier step does not re-buy 188 controllers.

**Settled 2026-10-13 (your answer: "Add an upgrade path").** Built as B-83: a tier t-1 generator becomes tier t in
the Condensate Assembler and keeps its Kerr controller; SL-K01 stays at BHC parity.

### B-31 Reactor fluid sockets refuse crafting input buffers [2.9.0-RC-2 port]

**Context.** In RC-2, GT's input-hatch adder also accepts fluid-capable dual input hatches (CRIBs and their slaves)
and answers "added". The reactor's feedwater, chamber-coolant and propellant sockets drain only their own plain
input hatches, so a CRIB there would have formed the structure and fed nothing.

**What we built.** The three sockets refuse anything that is not a plain Input Hatch, before GT's adder sees it, so
a CRIB can never be registered and starve the reactor silently. A failed Spiral walk also rolls back GT's
dual-input list. The production halls that run through GT's own processing logic (`createProcessingLogic`, which
reads `mDualInputHatches`) accept CRIBs as before. Two machines do not use that logic. Their notes:
- **The Enrichment Train (MTE 28103)** draws its uraninite and HF by hand with `depleteInput`, and GT's helpers walk
  only the plain hatch and bus lists. A CRIB in one of its casings would form and feed nothing. This was already true
  before the port, through the bus adder. The Train now refuses dual input hatches in both adders
  (`supportsCraftingMEBuffer() = false` plus an `addInputHatchToMachineList` override), so the walk fails at that cell
  instead.
- **The Turbine Hall (MTE 28104, `MTEPlantModuleBase`)** is a retired museum piece. Nothing calls its
  `processReactorSteam`, and it never processes, so a CRIB in it changes nothing. It is left as it is.

**Measured on the RC-2 dev server (B-32).** The silo is an optional piece of the plant, so a CRIB in the feedwater
socket does not un-form the plant. The silo walk fails, and the plant idles as a bare platform: silo T0, and the
Spine, Spiral and gallery MISSING. The status shows that, but no error names the socket cell. Any other wrong block
in a silo under construction behaves the same way (the B2 rule: only a legacy face gets the diagnostic re-walk).
Putting the plain hatch back re-forms the full T3 plant. See B-32 for the open question.

**Suggestion.** As built: each socket is a point-of-use line (feedwater grade, coolant grade, propellant) that the
reactor meters itself.

**Alternatives.** Support CRIBs in the sockets: give each socket its own dual-hatch list and drain the CRIB's fluid
inventory in the per-socket draws. More code in the reactor's fluid paths for little gain.

### B-32 RC-2 dev-server test: results, and a silo socket with the wrong hatch is not named [2.9.0-RC-2 test pass]

**Context.** This is the first headless test of the RC-2 port: `runServer` with 76 mods, including coremod 2.9.84, on
a fresh flat world (`sltest-rc2`), driven from the server console like B-27. The world was restarted twice.

**Load.** "[BTS] Recipe registration complete: 232 registered, 0 skipped" on all three boots, with no
`beyondthestars` warning, error or stack frame. GT's own null-input recipe errors come from pack mods missing on the
dev server. TecTech's 43 Eye recipes and its controller recipe are removed (AL-visual 1, AL-data 1, research 2).
"BHC->Kerr: 10 recipes now take the Kerr controller": the Empty Spatially Compressed Circuit Array and the 9
Spacetime Compression Field Generators, Crude to Gallifreyan, all in the Condensate Assembler. Then "removed Black
Hole Compressor controller: AL-visual 1, AL-data 1, research 0, scanner 1". The server-start pass found nothing new.

**New console tools** (op only):

- `/btseye gates` dumps every compression generator recipe the server has, with inputs, nanite tiers and
  condensates. It checks the gate rule (generator tier t must not need anything the Eye first makes at Eye tier t+1
  or later) on direct inputs and one level of Condensate Assembler sub-recipes. It also counts what still consumes
  or makes a BHC controller.
- `/btsbuild t<1-5> <x> <y> <z> [facing] [rod] [force] [crib] | [check]` is a console form of the reactor build.
  `crib` swaps the first feedwater hatch for a fluid Crafting Input Buffer, re-walks the plant, puts the hatch back
  and re-walks again.

**Results.**

| Check | Result |
|---|---|
| BHC consumers left (flag on) | **0**. 0 recipes make a BHC controller; the Kerr controller has 12 consumers |
| Gate rule, Eye T1-T4 gates (generator t0-t3) | **clean**. t1-t3 pass through SL-P39, the second Reinforced Resonance Chamber recipe. GT's own chamber recipe still needs a White Dwarf Matter foil |
| Gate rule, Eye T5-T9 | t4 loops on the Refined Resonance Chamber's Black Dwarf Matter foil; t5 and t6 are clean; t7 and t8 loop on the Resplendent Resonance Chamber's MHDCSM foil. This matches costs doc §9.4 and B-29 (P4/P5 work) |
| Kerr T1 + both modules | FORMED facing N (creative) and E (survival: 15,362 blocks in 79 rounds) |
| Kerr T2 + both modules | FORMED facing S (creative) and W (survival: 29,838 blocks in 151 rounds) |
| Eye T1 | FORMED facing N (creative) and E (survival: 19,615 / 100 rounds) |
| Eye T2 | FORMED facing S (creative) and W (survival: 23,958 / 122 rounds) |
| Eye T3 | FORMED facing N (creative) and E (survival: 27,942 / 142 rounds) |
| Eye T4 | FORMED facing S (creative, `fill`: Sirius B targeted) and W (survival: 34,501 / 175 rounds) |
| Every build | orientation check on the viewer's right; re-projection still FORMED; damage test names the cell; survival repair puts back exactly 1 block, with tile entities unchanged |
| Restart | all 12 still FORMED with `check`, with modules, Density Upgrade, Galactic core and bands |
| Reactor T3 | FORMED facing N, E and S; still FORMED after a restart |
| Reactor CRIB in the feedwater socket | refused: no dual input hatch and no feedwater hatch registered. The silo drops out (see below); the plain hatch re-forms T3. PASS |

**Open: the refused socket is not named.** With a CRIB in the feedwater socket, the plant stays FORMED as a bare
platform. The status reads "silo T0 ... spine MISSING | spiral MISSING | gallery MISSING", but no positioned error
names the socket cell. The probes walk five silo tiers with a null error list, and only a legacy face earns the
diagnostic re-walk (B2). Before the port the same happened to any wrong block in a silo socket, so this is not a
regression. The CRIB case just makes it more likely: a player swaps a working hatch for a CRIB and the whole plant
goes quiet.

**Suggestion.** When the silo probes all fail and a silo socket cell holds a GT machine that is not a plain Input
Hatch, re-walk that tier with the real error list, as the legacy-face diagnostic does. That names the cell. Or add a
status line, "Feedwater socket holds a Crafting Input Buffer: use a plain Input Hatch". It touches the silo probe
loop (R1-B / B2 history), so it waits for your call.

**Alternatives.** Keep it as it is: the status already says the silo is missing, and B-31's tooltip text can say
"plain Input Hatch only".

### B-33 The RC-2 test package: an untagged jar, the pre-redesign Kerr, and an older jar set aside [2.9.0-RC-2 package pass]

**Context.** The package pass built addon `0a878c0` and copied the plain jar plus a player-facing `INSTALL.md` to
`GTNH-Addons/dist-rc2/`, which is outside the addon repo and is not tracked anywhere.

**What was done (judgement calls).**

- **Version name.** The jar is `beyondthestars-0.24.1-master.93+0a878c0080.jar`. Agents never tag, so it carries the
  untagged build name rather than a release number like `0.25.0`.
- **What is in it.** It is the main tree as it stands: the Kerr with modes and screwdriver controls (the
  `kerr-redesign` branch is not merged) and the Eye before D-49. `INSTALL.md` says both will change.
- **Older jar.** `dist-rc2/` already held `beyondthestars-0.24.1-master.85+07deec8b78.jar`, the one now in the
  `rc - 2 TO THE STARS` instance. It was moved to `dist-rc2/superseded/`, not deleted, so nobody installs two copies.
  `INSTALL.md` tells the player to replace it in the instance. The package pass did not touch any instance.
- **RC-3.** `INSTALL.md` tells the player not to install on RC-3 and to ask for a re-pin first (port plan §7
  watch-list).

**Suggestion.** When you want a numbered release, tag it (e.g. `0.25.0-rc2`) and rebuild, so the Mods list shows a
clean version.

**Alternatives.** Ship the untagged name as it is (it is a test build), or delete `superseded/`.

---

### B-34 D-49: the field engine follows the prototype; the Agent-feed ruling lands in the star table, not the maths [D-49 unit 1]

**Context.** Unit 1 of the D-49 build plan ported the stability field to pure Java (`machines/eye/field/`) and added
the offline golden test: `node tools/fieldsim-golden.js --java` runs the prototype's own engine block (fixture
`tools/fixtures/star-lifecycle-engine.js`, sha1-pinned to `designs/star-lifecycle-prototype.html`) and the Java on
78 scripted cases. All 78 match bit for bit: every count, every ledger total, the final state and a per-second hash.
Three deliberate one-line mutations of the Java each fail 6-39 cases, so the test is not vacuous. Nothing in the
game uses the engine yet (units 4-5).

**Judgement calls.**

1. **Your answer "the Antipolarization Agent is the magnetar's constant feed" does not change the field maths.** In
   the engine, T6's feed push (P −4+5r, Θ −0.5+3r, Ω r) depends only on the upkeep fraction r, whatever the upkeep
   fluid is. So if unit 4 makes the Agent the T6 upkeep, a short Agent supply sags the magnetar exactly as a short
   WDM + BDM supply did in the prototype. The engine and the golden file stay as the prototype has them. The Agent
   **trim** (the flare shield, A +4 per unit) also stays.
   - *Suggestion:* unit 4 sets the T6 upkeep per cycle to Agent only. WDM + BDM become the merger charge, and the
     re-merge after a loss is your "tops it up". The Field-socket Agent trim stays as the flare response, so a
     player pipes Agent to two places: bulk upkeep and the trim valve.
   - *Alternative:* the constant feed also charges the shield. That is a maths change: update the prototype first,
     refresh the fixture, and regenerate the golden file.
2. **Plan §15.1's "T4 at m = 2.25 with tt = 6" cannot happen.** At T4, Time Acceleration tier 6 caps the dial at
   1 + 0.25·(6 − 4) = 1.5. The golden cases use tt = 9, which allows 2.25, with ts = 1 and ts = 9.
3. **The event clock is fractional.** The prototype schedules the next event at `t + uni(every)/m`, so plan §3.3's
   `ev.next` must be saved as a double, not a long. The field state notes this for unit 4.
4. **`light()` no longer starts the T5 batch itself.** The prototype's `light()` always starts it. In the mod, the
   runtime calls `startBatch` only when a WDM charge is buffered, and otherwise goes IDLE (plan §7.5).
5. **A missing 5,000 L HSM seed at the bounce is a supernova ("no neutron seed")**, as plan §7.4 says. The prototype
   never reaches this case, because it needs the HSM before the collapse can begin, so the golden test does not
   cover it.
6. **The Core Collapse results at the adopted +150 shock** (seed 1806): with 0 L or 30,000 L of SpaceTime both rigs
   go supernova, and with 60,000 L both settle. At +90 every case settles. This matches B-25's "a centred field needs
   about 50,000 L". The golden set also runs the collapse with the bang-bang rig, which goes beyond the plan.
7. **FieldSim's ledger uses the prototype's upkeep table** (T6: WDM 60 + BDM 15). It is a test ledger only. The mod's
   table is unit 4's star definitions, so ruling 1 does not touch the golden file.

**Suggestion.** Accept 1-7 as built. Re-run `node tools/fieldsim-golden.js --check --java` after any change to the
prototype or to `machines/eye/field/`.

**Alternatives.** For 1, see the alternative above. For 2, add a tt = 6 case at m = 1.5 as well.

### B-35 D-49: content for the star lifecycle; the Eye T5 gate needs a WDM chamber [D-49 unit 2]

**Context.** Unit 2 of the D-49 build plan added the content:

- 7 items: the Stellar Ignition Charge, the four transition cores, and Star Chart: Neutron Star and SGR 1806-20;
- 3 fluids: Positronic Tengam Slurry, Antipolarization Agent and Magnetar Matter;
- the Stellar Field Sensor Hatch (MTE 28116) with its MUI2 GUI;
- recipes: the Agent chain SL-S01/S02 (new `BTSRecipesStellar`), the cores SL-E03..E06, the hatch SL-E07, and charts
  and parts SL-P25 (now the Neutron Star chart), SL-P33 and SL-P35..P38;
- removals: SL-P26 and the interim T4 Black Dwarf Matter star go, together with `interimBlackDwarfAtEyeT4`;
- textures, lang entries with tooltips, and the `/btskit eye` contents.

Every pack material the Agent chain uses was checked in the RC-2 GT sources (5.09.54.205): `Materials.TengamAttuned`
has dust items, and GT's own Polarizer recipe makes it from Purified Tengam (`ProcessingTransforming`). Raw Tengam
goes to Purified in the EMS (`ElectromagneticSeparatorRecipes`). `Materials.Antimatter` is linked to the fluid
`antimatter` in `LoaderGTBlockFluid`. Molten Purified Tengam is generated because the material has metal items.

**Judgement calls.**

1. **SL-P33: Resonance Chamber III with 2 White Dwarf Matter foils instead of GT's 1 Black Dwarf Matter foil.**
   The audit rule says compression generator tier N must not need anything the Eye first makes at tier N or later.
   Tier index 4 (the Eye T5 gate) takes one Resonance Chamber III. That chamber takes a BDM foil, and with the
   interim T4 BDM star gone, BDM first exists at Eye T5 itself. Everything else is GT's tier-3 chamber unchanged:
   - inputs: 2 Emitter UIV, 2 Shirabon rods, 1 Hexanite plate;
   - nanites: White Dwarf, Gold, Transcendent, Gold;
   - condensates: Chromatic Glass 16, Infinity 8, Boundless Cosmic Solder 3;
   - 300 s at UMV, in the Condensate Assembler.

   WDM is the product of the tier below (Eye T4). That is the same "use the previous tier" step SL-P39 takes with
   SpaceTime for the WDM foil. It is registered only with `replaceTecTechEyeOfHarmony=true`, like SL-P39, and GT's
   own recipe stays. The Eye T6 gate (tier index 5: 4 chambers, Creon) may use BDM, so it is clean as it is, and
   **SL-P34 stays reserved and unused**. Verified in `runServer` (`/btseye gates`): the tier-index-4 generator now reads "via Refined Resonance Chamber: 2 recipe(s), a clean one exists", and the gates for Eye T1-T7 are all CLEAN. Only T8 and T9 loop, through Resonance Chamber IV's MHDCSM foil (P4/P5, as B-29 found). The boot log reads "243 registered, 0 skipped".
   - *Alternatives:* 2 SpaceTime foils (exactly SL-P39's stand-in, so the T5 gate needs no WDM at all), or a
     Shirabon foil. Another option is to rewrite GT's chamber recipe in place, so NEI shows only one recipe.
2. **The interim BDM star is deleted now, in unit 2, not in unit 5.** The plan gives unit 2 the
   `Config.interimBlackDwarfAtEyeT4` removal. The flag's only readers were SL-P26 and two lines in
   `EyeRecipeRegistry` (unit 4's file). Those two lines were removed so that unit 2 compiles: the
   `star:siriusbcooled` registration and its check in `replacementStarsPresent()`. Until units 4 and 5 land, a jar
   built from master has no BDM source, and the Sirius B chart is crafted from WDM (SL-P35) while Sirius B is still
   the T4 WDM star. These intermediate commits are not a release. The stale config key is removed from the file
   when the game loads.
3. **The sensor hatch emits on all six sides.** Plan §12 says "as the pH sensor does", but GT's pH sensor only
   powers its front face. The addon's Spine Sensor powers all six sides, and a hatch sunk into the Eye's shell has
   only one exposed face anyway, so all six sides costs nothing. Other details:
   - The output falls to 0 when no readout arrives for 60 ticks (Eye unformed, star dark, chunk unloaded).
   - The threshold is clamped to ±120, and the comparators are strict (`>` / `<`).
   - Analog mode ignores the threshold.
   - The settings are saved in the tile, not the item: the Eye pushes the readings again every second.
   - **For unit 4:** after every field second, call `publish(FieldReadout)` on each Sensor-socket hatch, and
     `publish(FieldReadout.DARK)` when the star goes out.
4. **Tooltips.** Plain items have no tooltip. `SLItems.defineWithTooltip` is `BTSItems.define` plus a tooltip read
   from the lang key `item.beyondthestars.<name>.tooltip`. The Galactic Upgrade Core and both Sirius B charts now
   use it too. The item class is not stored in a saved world, so this changes nothing in existing saves.
5. **Agent chain cost, now that the Agent is the constant feed (your answer 1).** The chain is the proposal's. At
   the simulated ~100 L/s of Agent, it costs:
   - about 10 L/s of Antimatter;
   - 1.6 Attuned Tengam dust/s, of which about 0.4/s is fresh Tengam once the Eye returns 12 Purified dust per
     1,000 L;
   - 200 L/s of Super Coolant in the freezer step;
   - two LCRs and one Vacuum Freezer running all the time at UXV.

   The T6 upkeep figure belongs to unit 4's star table. If it comes out well above 100 L/s, retune the 100 L of
   Antimatter per 1,000 L of Agent before you retune the Eye.
6. **Smaller choices.**
   - SL-P38 makes 4 charges per craft from Helium **plasma** (`Materials.Helium.getPlasma`).
   - SL-E03 takes 16 charges and researches from one charge. The research chain is: charge → Stellar Mass Core →
     Shell-Burning Core, and Galactic Upgrade Core → Subspace Cooling Core → Magnetar Seed Core. Every research item
     is unique to this mod. *Changed (B-62):* GT's Scanner consumes the research item, so SL-E04/E05/E06 now research
     from Star Chart: Arcturus / Sirius B / SGR 1806-20 instead of the previous core.
   - SL-P36 (the Sirius B, Cooled reclaim) takes programmed circuit 3 and no fluid.
   - In the kit, Antipolarization Agent, DSSF and Tachyon Rich Temporal Fluid come in Neutronium Large Fluid Cells
     filled to capacity. The obsolete Sirius B, Cooled chart is no longer in the kit.
   - **For unit 4:** Tachyon Rich Temporal Fluid is a **molten** fluid in RC-2 (`Materials.Time.getMolten`; the
     material name is `temporalFluid`). `Materials.Time.getFluid` returns null.

**Suggestion.** Accept 1-6 as built.

**Alternatives.** For 1, see above. For 3, use front-face output, exactly as GT's pH sensor does.

---

### B-36 D-49: the Eye's T5/T6 bands, cumulative bands and the new sockets [D-49 unit 3]

**Context.** Unit 3 of the D-49 build plan built the Eye's structure to T6:

- **Exporter.** `tools/export-structures.js` now exports two more pieces from `designs/voxel.js`:
  - `eye_arms2` (T5, the second arm pair): 135 × 11 × 83, 6,231 blocks;
  - `eye_rim` (T6, the galactic rim): 158 × 8 × 149, 11,202 blocks, with 4 rim stellar sockets `g` at the mockup's
    GRB cells.
- **`eye_core` re-export.** It now carries 8 Field sockets `f` and 8 Sensor sockets `n`. Those 16 cells were
  Reinforced Spatial casing (`U` 9,406 → 9,390). The assembled T5 Eye is 135 × 57 × 137 (40,733 blocks), and the T6
  Eye is 158 × 57 × 149 (51,935 blocks). `--check` is clean, and the Kerr shapes did not change.
- **Controller.**
  - `MTEEyeOfHarmonyBTS` gains the two pieces, `structureTier()` (the bands are cumulative), `compressionTier()`, and
    the 1:1 gate table extended to T6: `{-1, 0, 1, 2, 3, 4, 5}`.
  - Hologram and survival auto-place take stacks 1..6.
  - `EyeHatchElement` gains `Field`, `Sensor` and `Items`. The `h` sockets also take input buses.
  - The scanner names a band that stands above a missing one.
  - The tooltip lists the bill of materials for all six pieces.
- **Test bench.** `/btseye t5|t6`, plus two console tokens: `legacy` builds without the D-49 hatches, and `nobeam`
  clears the beam band after the build.

**Dev-server results (`runServer`, 243 registered, 0 skipped):**

| Case | Result |
|---|---|
| T6 north, creative, `fill` | FORMED. Sockets: gas 4, input bus 1, stellar 15 (6 + 5 + 4), Field 8, Sensor 8. Mirror check right, re-projection still FORMED, survival repair 1/1. |
| T6 east, survival path | FORMED. 51,934 blocks in 264 rounds (the whole T6 minus the controller), ended -1 |
| T5 south, creative | FORMED, structure T5 (rim absent) |
| T4 west, `legacy` (f/n cells left as Reinforced Spatial casing) | FORMED, Field 0, Sensor 0 |
| T6 north, `legacy` | FORMED, Field 0, Sensor 0 |
| T3 north, `nobeam` | FORMED as T3, then 4,343 beam-band cells cleared → "Eye T1: structure T1", the scanner says the jet crescent stands but does not count, and the jet's stellar sockets dropped (stellar 11 → 0) |
| `check` on the T6 | walk + status 9.4 ms the first time, 2.7 ms later (the plan's target is under 20 ms) |
| `/btseye gates` | CLEAN for Eye T1-T7 (T5 through unit 2's SL-P33; T6 clean as GT ships it), LOOP only at T8/T9 (P4/P5) |

**Judgement calls.**

1. **The Field and Sensor sockets moved.** Plan §11.1 put them at 15, 75, 105, 165, 195, 255, 285 and 345 degrees.
   The exporter's socket check found 15, 105, 195 and 285 inside the arm blades: each blade covers its root
   +5..+30 degrees one row below the mid-plane, and ±5 degrees does not clear it. So every pair now sits **45 and 75
   degrees past an arm root** (45, 75, 135, 165, 225, 255, 315, 345). That is 15 degrees from the nearest clamp strut
   and rotationally symmetric. `f` is one row below the core band's mid-plane on its outer face, and `n` is on the
   mid-plane directly above it, so every sensor touches its Field hatch. The T6 reach check passes: every socket has
   an outside face.
   - *Alternative:* put the 16 sockets on the upper and lower rails instead.
2. **The tier unlock stays the pre-D-49 rule until units 4 and 5.** `installedTier` is the highest tier allowed by
   both the cumulative bands and the compression gate, capped by the old unlock: T1-T3 by the bands, T4 with the
   Galactic Upgrade Core. So a full T6 Eye forms and reports "structure T6, compression T6", but runs as T4, with no
   extra parallels from the T5/T6 bands. Unit 4 replaces the cap with `unlockedTier`.
3. **Every band is walked, even above a missing one.** This lets the scanner name the gap ("the jet crescent stands
   but does not count until the beam band is built"). A band that does not count drops the stellar sockets it
   registered, the same as a failed band. It costs at most a few milliseconds per walk.
4. **No crafting input buffers on the plinth.** The input-bus role of the `h` sockets refuses a CRIB, as the reactor
   sockets do (B-31): it holds patterns, not stock. As with B-31, the socket then fails and the Eye does not form
   until it is swapped.
   - *Alternative:* accept a CRIB and read its inventory like a bus.
5. **Kit and test build.**
   - The last plinth gas socket takes an input bus, for the ignition charges; the other four take gas hatches.
   - The T4 build still loads Star Chart: Sirius B, which is still the T4 white-dwarf run until unit 4.
   - T5 loads Sirius B and T6 loads the Magnetar chart. Neither has a star until units 4 and 5.
6. **Status text.** The recipe-result lines for tier 3 and tier 4 were edited in place to name the beam band, since
   the bands are cumulative now. This is an exception to the append-only rule for `en_US.lang`. The tier5/6 and
   compression5/6 lines were appended.
7. **NEI preview (B-19 follow-up), not checked in a client.** BlockRenderer6343 counts tiers by building stack after
   stack until one adds nothing, so it should find 6 (stack 7 is clamped to 6). The T6 Eye is the same height as T4
   (57 blocks), so it still fits the scratch world, but it has 51,935 blocks against T4's 34,502 (+50 %). Expect the
   frame rate in the preview tab to be worse than at T4.
   - *Suggestion:* check it in the client. If it stutters, use B-19's fallback: an `INEIPreviewModifier` that leaves
     the band linings and glass (about 14,100 blocks across the six pieces) out of the preview only.
8. **The hologram at stacks 5 and 6** places compression generators at tiers 5 and 6 (metas 4 and 5), the T5/T6
   gates. The client-side hint path (`construct(stack, true)`) was not run headless. It reads only the trigger stack
   and the shape files, which the server paths above exercised.
9. **Migration.** A pre-D-49 Eye has Reinforced Spatial casing in all 16 new socket cells. The old `eye_core.txt`
   had `U` there, as the exporter diff shows, and both new sockets fall back to that casing, so old Eyes still form.
   The `legacy` runs prove this. An Eye built with the jet crescent but no beam band now runs as **T1** (your answer
   2), and the scanner says why. A pre-D-49 jar was not loaded; the `legacy` token reproduces the old cells exactly.

**Suggestion.** Accept 1-9 as built.

**Alternatives.** For 1, sockets on the rails. For 4, accept CRIBs. For 7, build the lighter NEI preview now rather
than after a client check.

---

### B-37 D-49: live stars T1-T4, the unlocked tier, escrow and the save migration [D-49 unit 4]

**Context.** Unit 4 of the D-49 build plan makes the Eye's stars live. The new package is
`machines/eye/star/`:

- `StarDef` is the per-cycle star table.
- `EyeStarRuntime` is the state machine (DARK, IGNITING, LIT, PENDING), and it handles cycles, ignition, the time
  dial, trims, the T4 feed, output, milestones and the core escrow.
- `EyeNbt` is the one NBT writer and reader. It also handles the 0.24.x migration.
- `EyeMilestones` holds the per-tier counters.
- `EyeStarBench` is the `/btseye star …` test bench.

The controller runs one field second every 20 ticks, straight after its socket drain. `installedTier` is now
`min(unlockedTier, structureTier, compressionTier)`. Planets still run as batch runs, as built.

**Dev-server results** (`runServer`, flat world, console; boot log "243 registered, 0 skipped", "6 live stars, 43 planet
recipes"):

| Plan test | Result |
|---|---|
| 3 (Sol, `unlock 1`, `seed 1806`, `lit`, `rig fine`, `sim 14400 compare`) | **COMPARE PASS**: the hash `3cde8ee7`, the zone counts (14,400 Resonant), the mean Y 1.489601 and the RSPM 17,875,208 L all equal offline `FieldSim` |
| 4 (Arcturus ignition from 1 charge) | IGNITING at P −80; P was −23.0 after 38 s and lit after 40 s; the seed cloud took 4·10⁸ L of H and of He; 2.42·10¹³ EU (10 cycles) debited; no product while igniting |
| 5 (Sol at p = 4 with 6·10⁷ L He) | First cycle r 0.30, then 0.00: P sags; the cycle's RSPM was 25,750 L = 50,000 × 4 × mean Y 0.4292 × 0.30; the star breached at P −101 |
| 9 (neutron star, feed 1.5, 3,600 s) | **COMPARE PASS**; 5,400 L of HSM drawn (1.5 × 3,600); WDM 24,716 L = 3.6864 × 5,400 × mean Y 1.2416 |
| 12 (sensors) | P > 5 at P +4.3 gave 0; P < 5 at P +8.7 gave 0; P > 0 at P +6.7 gave 15; Ω analog at +1.2 gave 8 (= round(15·121.2/240)) |
| 14 (100 s, `save-all`, restart, 100 s) | **COMPARE PASS** after the restart: 200 s, hash `f29e488b`. The RNG, the clock and the bench ledger persisted |
| 15 (`core subspace`, `set dial 1.5`, `wrench`) | Unlocked tier, milestones, escrow and buffers kept; the star is DARK |
| 16 (migration via `legacy`) | Galactic → unlocked T4. Arcturus run in flight ×2 → 100,000 L of RHSP owed, paid on the next formed second. Bands T1-T3 → grandfathered to T3. Jet without beam → unlocked T3, but runs as T1 (the scanner names the gap) |
| 18 | One field second takes about 2-5 µs (`sim 14400` ran in 68 ms) |

Two findings came out of the run and are fixed. A tile that had just been reloaded with its chunk had no hatch lists
until GT's start-up walk, and the status lines threw an NPE on the never-walked band faults. The bench now walks
such a tile first.

**Judgement calls.**

1. **T6 upkeep: Antipolarization Agent 3,000 L per cycle (50 L/s, × p).** This follows your answer that the Agent is
   the constant feed. WDM 18,432 + BDM 4,608 + Agent 100,000 become the merger charge (and the re-merge "top up").
   The flare-shield Agent trim in the Field sockets stays on top of the upkeep (B-34 item 1). So a magnetar uses about
   50 L/s of Agent as upkeep plus what the rig doses. Only the table is built here; the T6 runtime is unit 5.
   - *Alternatives:* 6,000 L per cycle (the simulated rig's ~100 L/s as pure upkeep), or let the upkeep also charge
     the shield (a maths change: prototype first, then regenerate the golden file).
2. **Milestones and the escrow are built in unit 4.** The plan gives them to unit 5. The task asks for "milestones →
   core → escrow", and their NBT belongs to `EyeNbt`. The transitions that consume the core are still unit 5's.
   Until then, a next-tier chart in the slot parks the star in **PENDING**: it keeps burning and nothing more
   happens. Any other chart, a planet or an empty slot puts the star out.
3. **The escrow is returned by sneak-PUNCHING the controller with an empty hand, not sneak-right-click (plan
   R-14).** GT's `BaseMetaTileEntity.onRightclick` never passes a sneaking right-click to the machine; it opens
   covers instead. Left-click does reach the machine.
   - *Alternative:* a GUI button (unit 6).
4. **A freshly loaded star freezes until GT's first structure check (about 5 s).** It does not go out. Otherwise
   every world load would extinguish every star, because `mMachine` is false until that walk. After the walk, an
   unformed Eye puts the star out (R-8), and so does a disabled one (R-7). The maintenance and structure stops GT
   applies disable the machine, so the star goes out then too.
5. **A star chart never starts a GT run.** `checkProcessing` returns a status (`star_lit`, `star_igniting`,
   `star_pending`, `star_dark`, `star_remnant`, `star_unit5`) or the tier gate. The tier gate is split into
   bands (`tierN`), the new unlock (`lockedN`, new lang keys) and compression. GT's active flag is forced on while
   the star burns.
6. **Fractional litres.** The T4 HSM draw and every cycle's product are fractional. The remainder carries to the
   next second or cycle (`hsmOwed`, `prodCarry` in the tile NBT), so nothing rounds away.
7. **Migration edges.**
   - Any `star:*` run key in a loaded tile is pre-D-49. Its output is owed (`btsEyeLegacyPay`, kept in item NBT too),
     and GT's progress is zeroed so GT cannot finish the run.
   - The bands are grandfathered at the first walk without errors (`btsEyeLegacyPending` persists until then).
   - A controller wrenched to an item **before** the update with no Galactic core and no buffered fluid has no item
     NBT at all (GT writes a tag only when there is something in it). It cannot be told apart from a new controller,
     so it starts unlocked at T1. `migration.md` says so.
8. **Smaller choices.**
   - The ignition seed cloud and the 10 cycles of EU scale with p.
   - The dial setpoint wraps 3.00 → 1.00. In use it is capped by m_max.
   - The feed setpoint steps 0.1 and wraps 1.5 → 0.
   - The creative `/btseye tN` build is unlocked to tN.
   - The kit's T4 build loads Star Chart: Neutron Star.
   - NEI star pages show one cycle: up to 4 fluid inputs (`maxIO(1, 81, 4, 18)`). The T4 page shows the HSM feed at
     the 0.694 L/s default.
   - `replacementStarsPresent()` now needs Sol, the neutron star and Sirius B (plan §10.4). Sirius B is registered
     as data (its batch runtime is unit 5).
9. **Tooltip.** Only the lines D-49 made false were edited (T4 Sirius B, the Galactic right-click, "stars always
   succeed", the placeholder fields). The full rewrite is unit 6's.
10. **Loading `FieldSim` in the game makes FML warn "MOD HAS DIRECT REFERENCE System.exit()".** The warning comes
    from `FieldSim.main` (unit 1's offline runner), which the bench's `sim … compare` loads. It is harmless.
    - *Suggestion:* swap the call for a return code in a later pass.

**Not run here.** These need a client or another unit:

- the controller GUI text;
- the screwdriver and the right-click escrow (the bench's `core` / `escrow` take the same code path);
- the time dial paid from real Field-socket Time;
- an ME input bus as the charge source.

**Suggestion.** Accept 1-10 as built.

**Alternatives.** For 1, see above. For 3, a GUI button. For 4, put the star out after a load too (harsher; not
recommended).

---

### B-38 D-49: the test package ships without the Eye GUI and the centre render [D-49 package pass]

**Context.** The D-49 package is `dist-rc2/beyondthestars-0.24.1-master.120+12fcf51c4a.jar` (sha256 `36ab9d10…56e4`),
built from `12fcf51` with `gradlew build` clean (no spotless or checkstyle fixes needed). `dist-rc2/INSTALL.md` is
rewritten for D-49. The hotfix jar `0.24.1-hotfix-rc2-recipe-result-lang.95+b320e4e8e6` moved to `superseded/`;
master already carries that fix (`f484699`).

**Judgement calls.**

1. **Packaged as is, with plan unit 6 not built.** There is no `EyeGui` (gauge bars, dial/feed buttons), no centre
   render (`eyeRenderCentre` is still read by nothing, and its config comment still says so), and the controller
   tooltip is only partly rewritten. INSTALL.md says all three are missing and points players at the scanner and the
   screwdriver. The task was to package, not to build them.
2. **The `/btseye tN` chat line is stale.** `CommandBTSEye` still says "the star chart in the controller slot runs at
   once" after a `fill` build. Under D-49 a T1-T3 star still needs an ignition charge (or `/btseye star … lit`). It is
   not a spotless/checkstyle fix, so I left it; INSTALL.md warns about it.
   - *Suggestion:* reword it in the unit 6 pass ("drop a Stellar Ignition Charge in the plinth input bus").
3. **Unit 5 and the test pass have no questions entry.** INSTALL.md's "dev-server results" line comes from the test
   pass's server logs: every transition reached COMPLETE and unlocked its tier, the Core Collapse also produced a
   supernova, and COMPARE PASS held. Unit 5's judgement calls still need their own entry.

### B-45 D-49: the Eye GUI, the centre render, cover-friendly sensors and the T5/T6 cost table [D-49 unit 6]

**Numbering.** This is B-45, not B-39: the `kerr-redesign` branch used B-33..B-44 for its own questions. At the merge
those became **B-46..B-57** (old kerr-redesign B-n = B-(n+13)); B-33..B-38 and B-45 are master's D-49 / RC-2 entries.

**What was built.** The Eye's MUI2 GUI (`EyeGui`), its centre render (`machines/eye/render/`), the sensor hatch's
cover-friendly output, the bench's `render` command, the T5/T6 cost table (costs §10), the build spec's D-49 block
and "Contract additions (Eye, D-49)", the migration note, and D-49 status lines on A-2, B-1, B-3, B-5, B-6, B-9 and
B-10. No star maths, recipes or NBT schema changed; the only new persisted keys are `btsEyeRender` (per-Eye switch,
item and tile) and `btsEyeRenderPos` and the star's `lastFail` (tile only).

**Judgement calls.**

1. **The render reuses TecTech's star, not TecTech's render tile.** TecTech's Eye render (`TileEntityEyeOfHarmony` +
   `EOHTileEntitySR`) draws one fixed orange star with a 13-block "outer space" shell and orbiting planet blocks, and
   the Forge of the Gods' renderer (`RenderForgeOfGods`) always draws its rings and beam with the star and keeps its
   shader private. So the Eye has its own render block (GT's `BlockRenderer`, as the BHC and the Kerr) and TESR, which
   bakes the Forge of the Gods' own shader recipe (`tectech:star`: the texture's luminance in our colour, raised to
   1/gamma), uses TecTech's `StarLayer0..2` textures and `EOHRenderingUtils.buildSphere` / `beginSphereCull`, and
   draws the layers in the Forge of the Gods' order (opaque core, then two translucent shells).
   - *Suggestion:* keep. *Alternative:* place TecTech's own EOH render tile for T1-T3 (the exact TecTech look,
     planets included, but always orange).
2. **Looks (PROPOSED, `EyeStarLook`).** Sol r 9 yellow-white, Sirius A r 11 blue-white, Arcturus r 15 orange (the
   mockup's sphere), the neutron star r 2.2 blue-white spinning fast, Sirius B r 3 white → orange → dull red → black
   over its batch (gamma 3.5 → 0.35), the magnetar r 2.6 violet. Instability makes a star breathe (wobble up to 10 %
   of its radius as Stability falls), an event warning adds more (the magnetar throbs 25 % on a flare warning).
   The nearest `eye_core` block is 20.9 blocks from the centre, so the Core Collapse's swelling Arcturus is capped at
   r 17 (about 19.4 with its wobble and outer shell).
3. **What counts as "lit".** The render shows while the star exists: igniting (a dim red protostar swelling toward
   the star), lit, pending and a T5 batch. T5 IDLE (no batch) and DARK show nothing. A supernova keeps the block 3 s
   for its flash, then it goes: the one exception to "removed when the star goes out".
   - *Alternative:* nothing until LIT; or a dark remnant while IDLE.
4. **Unload.** GT calls no MTE hook a block could be removed from on a chunk unload, and removing blocks while a
   chunk unloads is unsafe. Instead the render tile checks every 100 ticks and turns to air when its owner's cell is
   not loaded or no Eye there claims it; the Eye places it again when it next ticks (the first tick after its first
   structure check). An Eye that has lost track of its block (a save written before it was placed) clears it at once.
   So there are no orphans, and an unloaded Eye shows nothing.
5. **Config.** `eyeRenderCentre` off on a client: nothing is drawn there. Off on a server: no Eye places a block (and
   placed ones go). The per-Eye switch is in the GUI. The config file is now written on every start, because Forge
   neither reads comments back nor counts a changed comment as a change, so the old "NOT IMPLEMENTED" comment would
   never have gone (verified on the dev server: the comment is rewritten, values kept).
6. **Sensor output and covers.** The hatch now emits through `setInternalOutputRedstoneSignal` (as GT's own pH, lens
   and electrode sensors), so a cover that drives its own side (a receiver, a detector) is never overwritten. GT's
   Redstone Transmitter (Internal) and Advanced Wireless Transmitter (Internal) read the side's own output, which
   that call skips, so those two get the signal directly: one sensor can drive a wireless channel to a Fluid
   Regulator cover or an AE2 export bus anywhere. It is still pushed every field second.
7. **GUI choices.** GT's standard multiblock layout, English strings in code (as the Kerr's GUI; no GT-looked-up lang
   key and no new recipe-result id was added). Dial and feed are − / + buttons (right-click four steps) with the
   value between them, not text fields; the server clamps. The gauge bars draw the five zones around the setpoint
   (0, or the T5 cooling setpoint, with a yellow tick). The plan's P/Θ dial drawable (a stretch goal) is not built;
   the Field side panel is (zone legend, sockets, every sensor's status, trims this second, the rig).
8. **Cost table (costs §10), by hand.** The audit's model script is not kept, so §10 multiplies the manifest's
   counts by §3.2's per-block figures and GT's BEC generator recipe (`addSpacetimeCompression`, tiers 4 and 5).
   Verdict fair: the bands are cheap (1.5e14 EU, 193k L HSM for both), the 188 generators per step are the price
   (2.4e15 / 2.8e15 EU), 1.36× TecTech's own step to the same tier. **RC-2 oddity:** GT's generator times are 2, 3, 4
   and 5 million ticks for tiers 0-3 but 100,000 and 120,000 for tiers 4-5, so our T2-T4 gate steps cost about 30×
   the T5 and T6 steps in EU and hours. That is stock GT data; we do not change it.
   - *Suggestion:* leave it (it is the pack's balance for TecTech's Eye too). *Alternative:* our own BEC recipes for
     tiers 1-3 at the tier-4 time.
9. **The plan's PROPOSAL.md D-register updates (D-30, D-31, D-34, D-41, D-42, D-43)** are left to the main session
   (the design repo is not this unit's to edit).

**Dev-server results** (`runServer`, a T6 Eye built by `/btseye t6 3000 100 0 north gen 9 fill`, centre (3000, 140,
40) as computed from the manifest; `render` prints the cell, its block and the look):

| Step | Render |
|---|---|
| Dark Eye | nothing placed |
| Sol / Sirius A / Arcturus lit | live star r 9 FFD678 / r 11 AAC8FF / r 15 FF8232 |
| Neutron star lit | neutron star r 2.2 BEDCFF, spin 12 |
| Chart removed (star out) | air at once |
| Ignition from a charge | protostar r 3.55 E94F23, then Sol once lit |
| Core Collapse phase 1 (300 s in) | swell r 15.6 FF6C2C |
| Supernova at the bounce (no SpaceTime) | supernova flash (kind 8). 6 s later the block still stood, unclaimed: the controller had been reloaded and lost its position. The stray-block clearing (judgement call 4) was added after this run for exactly that |
| Core Collapse with 60,999 L SpaceTime, settling | bounce, proto-neutron star r 2.6 |
| Sirius B, half a batch | cooling dwarf FF9F5D, gamma 1.93 |
| Magnetar Merger | magnetar r 2.6 C85AFF |
| Per-Eye switch off / on | air / placed again |

The dev server unloads the Eye's chunks between console commands (no player there), so each command can meet a
freshly loaded controller; the first run's stale looks came from that, and the bench now drives the render itself
(`render`, and every `sim` second). One check stayed inconclusive for the same reason: after breaking the controller,
`testforblock` found the centre's chunk unloaded. The tile's own check is the path that removes the block there.
**Not seen in a client yet:** the drawing itself (and under Angelica's core profile), the GUI layout.

---

### B-46 Kerr redesign: every module recipe needs an open hole [Kerr redesign, spec only]

**Context.** You ruled that the modules become separate machines "that just have a requirement of a hole being
open to run". Today's Kerr is kinder than that: with the hole closed, COMPRESS still runs plain compressor recipes at
the base parallel (B-8).

**What the spec says** (`kerr-redesign.md` §3). A module runs nothing unless it is linked to a formed KEE with an
open hole. This is also RC-2's Black Hole Compressor behaviour: its `validateRecipe` returns `NO_BLACK_HOLE` for
every recipe while the hole is closed. DENSITY needs Kerr T2 and at least 128 SM, as before. The compressor module
searches its three maps together, so it needs no machine mode at all.

**Suggestion.** As specced.

**Alternative.** Plain compressor and extractor recipes run with the hole closed, at 8 × tier with no bonus. That is
friendlier, but it blurs "the hole powers the modules".

### B-47 Kerr redesign: closing the hole mid-recipe pauses the modules, it does not void them [Kerr redesign, spec only]

**Context.** A hole can close while a module is mid-recipe: a Collapser, evaporation when the bolts run out, the
containment power failing, the KEE unforming, or the KEE's chunk unloading.

**What the spec says** (§2.3). The Godforge pattern: an unlinked module skips its post-tick. Progress and EU stop,
and the recipe resumes when the link returns. Parallel and speed were fixed at recipe start, so changing the mass
mid-recipe cannot be exploited. The GUI's Collapse button takes two clicks, and the first warns how many modules
will pause.

**Suggestion.** As specced.

**Alternatives.**

- Void the running recipe, as the BHC does when its hole goes unstable. That is harsher, and it punishes running out
  of bolts twice.
- Let the running recipe finish and block only new starts.

### B-48 Kerr redesign: the KEE draws containment power while the hole is open [Kerr redesign, spec only, PROPOSED number]

**Context.** The KEE no longer runs recipes, so its two energy sockets and the GUI's power line would mean nothing
(today "the hole draws no EU").

**What the spec says** (§3). The hole draws 8,388,608 EU/t (1 A UEV) while it is open, and nothing while it is
closed. Without that power the governor fires no bolts, so the hole bleeds down, shows as unstable and evaporates at
2 SM. The modules draw their recipe EU from their **own** energy hatches.

**Suggestion.** As specced; tune the number in play.

**Alternatives.**

- 0 EU/t: delete the KEE's energy requirement, so the GUI shows no power line.
- Upkeep that scales with mass, for example 1 A UEV × M / 16.
- Modules fed from the KEE's hatches (the Space Elevator pattern) instead of their own.

### B-49 Kerr redesign: a whole-number setpoint, GUI open/close buttons, and the extractor controller moves [Kerr redesign, spec only]

**Context.** You asked for "a neat GUI" and no screwdriver.

**What the spec says** (§4, §6).

- **Setpoint.** The 9-rung screwdriver ladder (2 … 512) becomes any whole number of SM: 2-64 at T1, 2-512 at T2.
  It is set with a number field, a slider and −/+ buttons.
- **Open and close.** Open and Collapse are GUI buttons. They are enabled only when a Seed or a Collapser is in a
  bolt bus, and Collapse takes a second click to confirm. An "Auto" toggle (default on) keeps today's behaviour, so
  automation still works: a Seed or Collapser dropped into a bolt bus acts on its own.
- **Extractor controller.** The module's core cell today sits on the arc's inner face, facing the hole, where a
  player can hardly reach it. The Extractor Module's controller therefore moves to the arc's outer face at 340°. The
  old cell becomes an I/O socket, and two more sockets are added beside the new controller. The compressor's core
  cell is already on the outer face, so its controller stays in that cell.

**Suggestion.** As specced.

**Alternative.** Keep the ladder as the only values the slider stops on.

### B-50 Kerr redesign: modules cost the hole nothing; render scale and migration [Kerr redesign, spec only]

**What the spec says.**

- **Module cost.** None: a running module never changes the mass, the bleed or the stability (§3).
- **Render** (§5). It reuses GT's black hole renderer through a GL scale around the hole centre: s = clamp(4 ·
  (M / 16)^(1/3), 2, 6.5). That gives a 5.2-block horizon at 16 SM, the mockup's size, and it is capped so the disc
  stays inside the mirror ring. GT's lasers are off, because they run along the axes and the rails sit at 45°. It is
  toggled by the `renderKerrHole` config and a per-machine GUI button.
- **Migration** (§7). The KEE converts legacy core blocks into module controllers once, automatically. The
  fallback is `/btskerr migrate`. SL-K02 and SL-K03 now output the controllers, at an unchanged cost, and a
  shapeless recipe turns old core blocks into controllers.

**Suggestion.** As specced.

**Alternative for the module cost.** Each running module adds a share of bleed, for example +10 % each, so the hole
"feels" the load and a busy Kerr burns more bolts.

### B-51 Kerr redesign: the compressor module's controller sits below the T2 band, not in the old core cell [Kerr redesign, built]

**Context.** The spec (§6.1) put the Kerr Compressor Module's controller in the old core cell (the mockup's
"control" block on the south arc's outer face) and asked the exporter to assert that every module controller's
front face is open air in a full build. That assert fails at Kerr T2: the T2 reinforcement band (Harmonic Lining,
Bastion and Casimir plates at z −41..−44, y −2..0) closes the one-block gap in front of the old core cell, so a
player cannot reach the controller once the Density Upgrade is in.

**What is built.** The exporter puts the controller in the arc's outer shell cell straight below the old core cell,
at the first height whose front cell and the cell above it are open air in the T2 build (voxel (0, −4, −39)). The
old core cell becomes a seventh `c` I/O socket. The front-face assert now passes for both modules at T1 and T2. The
extractor's controller is where the spec put it (outermost cell on the 340° ray, voxel (36, 0, −13)). Migration
uses the exported `legacyCoreRel`, so it swaps the old core cell to casing and places the controller in the new cell.

**Suggestion.** As built.

**Alternatives.** Keep the old cell and carve a 1 × 1 access tunnel through the T2 band (4 blocks deep, beyond
survival reach), or move the controller above the band (y +4).

### B-52 Kerr redesign: how pausing, the power switch and the KEE's GUI work [Kerr redesign, built]

**What is built, where it differs from the spec.**

- **Pausing.** The spec copied Godforge: skip the whole post-tick while unlinked. Built instead: an unlinked module
  skips only GT's `runMachine` (no progress, no EU, no new recipe, the running recipe is kept). It still walks its
  own structure, so a module forms, and its GUI says why it waits, before any KEE exists. Godforge's version never
  forms an unconnected module.
- **The link check** is a full round trip: the module computes the KEE cell from its own facing (`keeRel`), and the
  KEE there must compute this module's cell back (`rel`) with the slot's facing (`facingAbc`). The manifest also
  carries `turns`, but only as documentation.
- **The KEE's power switch does not touch the hole** (shown greyed out). GT switches a machine off whenever its
  structure breaks, so if "off" stopped the governor, a cage repaired after one broken block would keep bleeding
  until someone switched it back on. Collapse is the way to shut a hole down; containment power still matters.
- **No power panel on the KEE.** GT's power panel holds the max-parallel field and the power-fail toggle, which mean
  nothing on a machine that runs no recipes, so the KEE has no power-panel button. The modules keep it.
- **Stability** counts as 100 % while the mass is at or above the setpoint (the governor is holding, or the hole is
  being let down to a lower setpoint). Below the setpoint it is mass fed ÷ mass bled over the last second.
- **The setpoint field** accepts 2-512 on the client; the server clamps to 64 at T1. (Superseded by B-54: the
  field, slider and - / + now follow the synced tier range; the server still clamps.)

**Suggestion.** As built.

### B-53 Kerr redesign: what the render pass still has to do, and the new recipe ids [Kerr redesign, built]

**Render.** This pass built only the per-machine switch: the KEE saves `btsKerrRender` (default on), and the GUI has
the eye toggle in the gap row. The render block, `TileKerrHole`, `KerrHoleRenderer`, `Config.renderKerrHole` and
the placement calls are the render pass's (spec §5, work plan R4). Its hooks are `MTEKerrEngine.isRenderEnabled()`,
`isHoleOpen()`, `mass()`, `getStability()` and `KerrSlot.centreRel()` (the hole centre, from the manifest).

**Recipe ids.** SL-K02 / SL-K03 keep their ids and inputs and now output the module controllers. The legacy-core →
controller shapeless recipes are **SL-K07** (compressor) and **SL-K08** (extractor); SL-K06 stays retired. The two
legacy core metas (`slCasing2:0/1`) stay registered and are hidden from NEI and the creative tab.

**Migration.** The KEE converts legacy core blocks once per load, from 40 ticks in, as soon as its slot cells are loaded;
`/btskerr migrate [x y z]` repeats it on demand. A controller cell is replaced only if it is air or the plain arc
shell. The old in-flight recipe goes to the module it ran on (the saved `btsKerrBoundMode`, else the old machine
mode); items that do not fit drop at the module controller, and fluids that do not fit are lost. The test bench
`/btskerr legacy x y z` turns a built Kerr back into the old layout so this can be exercised.

**Dev-server results (headless `runServer`, RC-2, 2026-10-12).**

- `/btskerr t1` in all four facings and `/btskerr t2` north: the KEE and both modules FORM; each module controller
  lands in its slot facing out of the cage; the extractor stands on the viewer's right (the mockup's side); creative
  re-projection over every machine breaks nothing; the KEE's damage-and-repair test passes. After the hole opens
  both modules report "Linked" (parallel 448, speed ×4.0 at 16 SM with creative UMV 16 A energy), also after their
  chunks unload and reload (nothing about the link is saved).
- Survival path (`survival`): KEE 13,898 blocks in 71 rounds, compressor arc 964 in 6, extractor arc 498 in 4, all
  finished (-1) and formed.
- Migration: `legacy` → `migrate` converts both arcs, and the modules form on their existing hatches and link. A Kerr
  left in the legacy layout across a server restart converts itself (both arcs) about two seconds after load.
- Not yet run: a module recipe actually completing, pausing mid-recipe on Collapse, two KEEs side by side, the
  T2 DENSITY recipe, the GUI in a client, the NEI preview (work plan R5).
- Test-bench only: removing a console-placed (ownerless) GT multiblock can throw inside GT's power-fail tracker
  (`UsernameCache` with a null UUID). Player-placed machines have owners; `legacy` gives a stand-in owner and
  carries on.

**Suggestion.** As built.

### B-54 Kerr redesign: KEE GUI details the spec and mockup left open [Kerr redesign, built]

**Context.** The KEE console (`MTEKerrEngineGui`) was redone to match the mockup (`kerr-gui.html`) and spec §4.1;
the module screen gained status lines (§4.2). A few details were judgement calls.

**What is built.**

- **Setpoint range follows the tier live.** The gap-row field, the panel's field, the slider and the − / + buttons
  are all bounded by the synced maximum (64 at T1, 512 at T2). The slider runs 0..1 over the *current* range and
  snaps to whole SM, so it follows a Density Upgrade going in while the panel is open (MUI2's slider bounds are
  fixed at build time). Every write still goes through `MTEKerrEngine.setSetpoint`, which clamps on the server.
  Field scroll steps: 1, shift 8, alt 64 (GT's compressed-bus idiom); right-click on − / + steps by 8.
- **Collapse confirmation** is client-side: the first click arms it for 4 s (label "Confirm", and the line under the
  buttons turns red with "N linked modules will pause"); a second click inside that window sends the server action,
  which re-checks the open hole and the Collapser in a bolt bus.
- **Auto** is a check box (GT's checkmark / cross overlays) rather than a text button, as in the mockup.
- **Mass bar colour** is the BHC lavender, not a stability colour; stability has its own coloured line.
- **One side panel at a time.** Hole control and Modules open in the same spot to the right of the main panel, so
  opening one closes the other.
- **Modules panel rows** show the module controller's item icon, its state, then either the hole bonus (linked) or a
  short reason (paused: why; not built: "place its controller at marker N"; not formed / not loaded / not linked).
  The mockup's "IDLE: no input" reason is not shown: the KEE does not read a module's last recipe result.
- **Module screen:** besides the link and bonus lines, a live recipe-gate line (`gateLine()`; the compressor shows
  "Densification: needs Kerr T2 / needs 128 SM / available", also on the scanner) and a constant maps line
  (`mapsLine()`).
- **Text is English in code**, like the rest of the Kerr GUIs and the scanner lines. Only GT-looked-up ids (recipe
  results, structure errors) have lang keys.
- **Not verified in a client.** It compiles against RC-2; nobody has opened it in a game client yet (R5).

**Suggestion.** As built; move the GUI strings to `en_US.lang` when the Kerr text settles.

**Alternatives.** Rebuild the slider when the tier changes (more code, same result); a server-side Collapse
confirmation (needs per-player state for no gain, because the server re-checks anyway).

### B-55 Kerr redesign: the hole render as built [Kerr redesign, built]

**Context.** Spec §5 / work plan R4: a black hole at the KEE's centre, reusing GT's Black Hole Compressor render
(`BlackholeRenderer`, `RenderingTileEntityBlackhole`, `BlockRenderer`), sized for the cage and scaled by mass,
shown only while the hole is open, with no orphan render blocks. The judgement calls:

**What is built.**

- **GT's classes, three small ones of ours.** `TileKerrHole extends RenderingTileEntityBlackhole` (GT's growth /
  collapse animation, stability input and description-packet sync, plus a synced scale and the owning KEE's
  coordinates); `BlockKerrHole extends BlockRenderer` (GT's render block, registered by its constructor as
  `beyondthestars:gt.beyondthestars.kerrHole`, no item in NEI or the creative tab); `KerrHoleRenderer` (client) wraps
  GT's `BlackholeRenderer` and `glScalef`s it about the centre, so GT's shaders and model are used unchanged. GT's
  lasers stay off (spec: they would cut through the arcs).
- **Server places, server removes** (as GT's BHC). The KEE puts the block at `machines.kerr.centre.rel` (0, 0, 38)
  through its own facing, the same cell for T1 and T2 (checked in both shapes: a don't-care cell, nearest solid
  block 12.3 away, a rail tip). Only into air, or it adopts our own block already standing there; otherwise the GUI
  terminal and the scanner say "Render blocked at x, y, z". It never loads a chunk.
- **When it shows.** Hole open AND the KEE's GUI switch AND `Config.renderKerrHole` (default true; the server
  places nothing when it is off, a client with it off draws nothing). Formed or not (a frozen hole is still a hole).
  When any of the three goes false, GT's 40-tick collapse plays and the block goes after it (the spec said this only
  for a close; a switch turning off does the same, which looks better than a pop). Re-opening during the collapse
  regrows from zero (GT's animation has no mid-point start).
- **Scale** `clamp(4 (M/16)^(1/3), 2, 6.5)` as specced (16 SM → 4.0, 64 SM → 6.36, 128 SM and up → 6.5).
  **Stability**: GT's input is the governor's coverage / 2, so the shader's stability equals the coverage (calm at
  full coverage, the disc shrinks below it, spins at 0, e.g. with no containment power). Both are pushed only when
  the scale moves 2 % or the coverage 0.02, checked once a second.
- **No orphans, two ways.** (1) The KEE removes the block on break (`onBlockDestroyed`), on any tile removal
  (`onRemoval`, wrench), when its facing no longer points at that cell, and on load if the hole or a switch says it
  should not exist. Its position is saved in the TILE NBT only, never on the wrenched item (trap 2), so a KEE placed
  elsewhere never claims a block here. (2) **Added beyond the spec:** the render tile polices itself. Every 100 ticks
  on the server it asks the owner's cell (only once that chunk is loaded) whether a KEE there still claims exactly
  this cell, and turns itself to air if not. That covers what (1) cannot: a KEE broken while the centre's chunk was
  unloaded, a world edit, a `/setblock`-placed ownerless block.
- **Test bench** `/btskerr render x y z [open [m]|close|off|on|break]`: acts, then reports the hole, both switches,
  the centre cell and every Kerr render block within 100 blocks.

**Verified (headless `runServer`, RC-2, 2026-10-12).** T1 north, east, south and T2 west: the block lands on the
centre cell, claimed, scale 4.01 at 16 SM, 6.36 at 64 SM, 2.32 at 3 SM, 6.50 at 128 SM (T2). Close: the block stays
for the 40-tick collapse, then air. Render switch off: same; on again: placed again. Breaking the controller: the
block is gone in the same tick. Two `/setblock` ownerless render blocks next to a running KEE: gone within 100 ticks,
the KEE's own block kept. Server restart with two open holes: each KEE adopts its block (one block each, still
claimed after the tile's 100-tick self-check). **Not verified:** the look in a game client (size against the cage,
the disc against the rails, Angelica's core profile), R5. Dev-server note: with no player online, Forge stops ticking
tile entities 1,200 ticks after the last player left (or the start), so console tests must finish within a minute
of start or a player must be online.

**Suggestion.** As built; look at it in a client. If the disc at the 6.5 cap looks crowded against the rail tips
(12.3 from the centre; the disc reaches about 13.5), lower the cap to 5.8 (disc about 12).

**Alternatives.** A client-only render with no block (a world-render hook keyed on the KEE: no block to orphan, but
the KEE controller's chunk must be loaded for the client to know about the hole, and it means our own render
dispatch instead of GT's TESR path); GT's `GregTechAPI.sBlackholeRender` block itself (no way to scale it or tag its
owner).

### B-56 Kerr redesign: module recipes on a dev server, and SL-K05 lost to the Eternal Singularity recipe [Kerr redesign, R5 test]

**Context.** R5 dev-server pass (headless `runServer`, RC-2, 2026-10-12): KEE T1 and both modules formed in two
facings through both paths, module recipes driven from the console, the hole closed and the KEE unformed mid-recipe,
T2 with DENSITY, render cell checks and two restarts. New test bench: `/btskerr module x y z <c|x>
stock|status|tier|clear`, `/btskerr damage|repair x y z`, `/btskerr keep x y z [t2]` / `release`.

**Found and fixed: SL-K05 never ran.** The coremod's neutronium-compressor recipe turns 72 L of molten SpaceTime alone
into an Eternal Singularity (UMV). SL-K05 also takes molten SpaceTime, and the compressor module searched DENSITY
last, so a Kerr T2 at 128 SM fed bolts + RHSP + molten SpaceTime made Eternal Singularities batch after batch and
never touched the bolts. **Built:** DENSITY is searched first. With the gate open (T2, 128 SM) SL-K05 ran (64 bolts →
1,000 L Hyperdense Stellar Mixture); with the gate shut (T2 at 64 SM) the bolts stayed untouched and the module fell
through to GT's maps. The creative kit's compressor module also gets a second input hatch, because SL-K05 takes two
fluids and a GT input hatch holds one.

**Results.**

- **Formed:** T1 north and east on the creative path, T1 east and north on the survival path (KEE 13,898 blocks in 71
  rounds, compressor arc 964 in 6, extractor arc 498 in 4), and T2 south and west. Every module formed and linked,
  the extractor stood on the viewer's right, re-projection broke nothing, and damage-and-repair passed each time.
- **Hole closed:** both modules sat in `HOLE_CLOSED` with their inputs untouched, through a restart too. After Open,
  the extractor ran (128 Stemcells → 128 Plasma Membrane at parallel 448, ×4) and so did the compressor (Fusion
  Control Computer Mk-III → Hypervisor Matrix, 14 in parallel, and Poppy → Plantball).
- **Closed mid-recipe** (B-47): progress froze (379/2066), the recipe was kept, the machine went idle, and it resumed
  on reopen. The module notices on its next 20-tick link check: it ran about 14 more ticks after the close.
- **KEE unformed mid-recipe** (one cage block removed): both modules went `KEE_NOT_FORMED`. The compressor's progress
  froze, a freshly stocked extractor did not start, and the hole stayed frozen at 16 SM with no bleed. After the
  repair, both resumed and the recipe finished, with the outputs in the bus.
- **Render:** placed on open (and on the auto seed), gone after the 40-tick collapse on close, gone at once when a
  loaded KEE broke, and gone after a restart for a KEE broken while its centre chunk was unloaded (the tile's
  self-check). A closed hole has no block after a restart; each open hole adopts its one block. **While the KEE is
  unformed the render stays,** as built in B-55 (a frozen hole is still a hole). The R5 task list expected it to go
  on unform. That is your call.
- **Restart:** hole state, mass, setpoint, T2 and in-flight module recipes all persisted (a compressor batch went
  644 → 725 across the restart and finished).
- **Cosmetic, not changed:** the scanner bonus line shows `p112 x1.00` while the hole is closed (the GUI says
  "none"), and GT's `lastParallel` reads 0 until a reload.
- **Test-bench note:** a playerless dev server unloads a Kerr's chunks within seconds of the last command, so
  console tests must force-load them with `keep`.
- **Not run here:** the GUIs and the render's look in a game client, and the NEI preview.

**Suggestion.** Keep DENSITY first. Decide whether the render should go when the KEE unforms. If yes, it is one
condition in `MTEKerrEngine.tickRender` (`want` gains `&& mMachine`).

**Built (by B-59):** `want` includes `mMachine`, so the render goes while the KEE is unformed.

---

### B-57 Kerr redesign: review fixes (recipe-check pushes, DENSITY cache, migration hold, no KEE maintenance, Collapse button, Auto) [Kerr redesign, review fix]

**Context.** A review of the Kerr redesign against GT 5.09.54.205 (RC-2) sources found six issues. All six were
checked against the RC-2 sources and fixed. They were re-tested on the headless dev server (2026-10-12) without the
`stock` command where a refill would hide the bug.

**What was fixed.**

- **Idle modules never restarted.** RC-2 checks recipes only on a push (`doPeriodicChecks` is false). A check that
  failed with `HOLE_CLOSED`, `KEE_NOT_FORMED` or `density` used the pending push up. The module now pushes
  `RecipeCheckReason.IMMEDIATE` when its link becomes `LINKED`, and the compressor pushes one when its DENSITY gate
  opens (polled every 20 ticks). *Test:* the hole was closed and the extractor stocked in the same tick, so the
  module stayed idle with `HOLE_CLOSED` and 128 Stemcells. The hole was then reopened and nothing else was touched:
  128 Plasma Membrane came out. Same with the KEE unformed (`damage`), the compressor stocked with Poppy, then
  `repair`: 8 Plantball.
- **DENSITY first did not hold.** GT offers `ProcessingLogic.lastRecipe` to every map query ahead of the map's own
  recipes, and never clears it. The last recipe is now offered only to its own map (`GTRecipe.getRecipeCategory()
  .recipeMap`). A DENSITY match also hides GT's maps (`shadowsLaterMaps`), so a shut gate waits with "density"
  instead of the Eternal Singularity recipe eating the SpaceTime. *Test (T2, no restart):* SL-K05 stocked at 64 SM:
  the bolts and SpaceTime stayed untouched. Raised to 128 SM with nothing restocked: SL-K05 ran. The bolts were then
  cleared, so an Eternal Singularity batch ran and became the cached recipe. After restocking the bolts, the next
  recipe was SL-K05 (2,000 L Hyperdense Stellar Mixture in total).
- **Migration destroyed in-flight recipes.** Old arcs had no energy hatch, so a converted module cannot form, and
  every fluid was counted lost. The KEE now moves the old outputs off GT's recipe fields at load (GT's `stopMachine`
  clears those while unformed). It holds them in its NBT (`btsKerrLegacy`, tile and wrenched item) and hands them over
  each second once the module is formed. Items that do not fit drop at the module controller. Fluids that do not fit
  stay held, and the GUI note says so. *Test:* `/btskerr legacy x y z inflight` (strips the module energy hatches,
  puts 2 x 64 bolts, 1,000 L water and 500 L lava in the KEE's GT recipe fields) followed by a restart. The load
  captured the recipe, auto-migration converted both arcs, and the note said "held until the Compressor module forms".
  After `/btskerr module x y z c energy`, water went to the output hatch, lava stayed held, and it was still held after
  another restart. All 128 bolts were found as drops: the test layout has no output bus, because `legacy` puts the
  core block back on the I/O socket. Same result on a T2 without a restart.
- **KEE maintenance.** The KEE runs no recipes. `getDefaultHasMaintenanceChecks` is now false and the
  maintenance-hatch requirement is gone. The governor arc's `m` cell is shell first: builds place casing and there is
  no hint. It still accepts the maintenance hatch an existing Kerr has there, so nothing unforms. *Test:* the kit's
  KEE formed with 10 hatches and no maintenance hatch, in T1 and T2. The dev server also caught a crash in the first
  version: GT's `HatchElementBuilder.build()` requires a hint.
- **Collapse on an unformed KEE.** The button now needs a formed cage, as the server does. The status line says
  "Collapse needs a formed cage". GUI only; not seen in a client.

**Judgement call: Auto (finding 6).** With Auto on (the default), a Collapser placed in a bolt bus collapses the hole
within 20 ticks, so the panel's two-click Collapse and its Collapser count are moot while Auto is on. B-54 built Auto
as "a Seed or Collapser placed in a bolt bus acts on its own" (migration.md promises the old behaviour), so
**built:** the behaviour is unchanged. With Auto on, the panel now says "Auto on: a Collapser in a bolt bus collapses
at once", and the Auto tooltip says the same. **Suggestion:** Auto acts on Seeds only (evaporation recovery keeps
working) and Collapsers are left to the button. Alternatives: split into "Auto open" / "Auto collapse" with collapse
off by default, or keep as built.

**Seen, not changed.**

- A migrated module controller stands "switched off". GT disables any multiblock that is allowed to work while
  unformed, so after adding its energy hatch the player switches it on (standard GT behaviour).
- SL-K05 inputs that are short (for example 10 bolts instead of 64) do not match DENSITY, so they still fall through
  and the Eternal Singularity recipe can use the SpaceTime. That is the inputs' ambiguity, not the gate.

---

### B-58 Merging kerr-redesign into master: renumbered questions, config save, dead Kerr lang keys [merge]

**What was done.** `kerr-redesign` (hole-only KEE, the two module machines, the KEE's MUI2 GUI, the black-hole render)
is merged into master, which carries the RC-2 port and D-49 units 1-6.

- **Question numbers.** Both branches had used B-33..B-44. Master keeps B-33..B-38 and B-45; the branch's twelve
  became **B-46..B-57** (old n -> n+13), moved after B-45, with every reference in docs and code comments rewritten
  (only lines that came from the branch were touched, so master's B-34..B-38 references still point at D-49). The
  table is under "Where the old question numbers went". Branch commit messages keep the old numbers.
- **Config.** Both options are kept (`eyeRenderCentre`, `renderKerrHole`). Master's "always save" wins over the
  branch's "save if changed", so the `renderKerrHole` comment is also rewritten on every start.
- **Client and block registration.** Both renders are bound (TecTech-shader star for the Eye, GT's BlackholeRenderer
  for the KEE) and both render blocks registered (`eyeStar`, `kerrHole`).
- **Structures.** `manifest.json` was regenerated by `tools/export-structures.js` (same voxel.js sha on both sides).
  The result is the union: Kerr machines, legend, module pieces and the `kerr_slots` table equal the branch's; the
  Eye's machine and its pieces including the T5 `eye_arms2` and T6 `eye_rim` bands equal master's. `--check` is clean.
- **Lang (RC-2 recipe_result).** The module results (`beyondthestars.kerr.module.<holeclosed|keenotformed|
  keenotloaded|wrongfacing|nokee|density>`) already had their `GT5U.gui.text.recipe_result.` keys on the branch.
  Master's f484699 had added recipe_result keys for the old KEE's `kerr.nomodule`, `kerr.modeunavailable` and
  `kerr.lowmass`; no code produces those ids after the redesign and their text tells the player to use a
  screwdriver, so they are removed. Every literal and generated `ofFailure` / `ofSuccess` id in the merged code has
  its recipe_result key.
- **Checks.** `spotlessApply compileJava` passes; `node tools/fieldsim-golden.js --check --java` 78/78 PASS. No
  in-game run was part of the merge.

**Left as it was.** SL-K04's old non-BEC path stays for the BEC step. The design repo's
`designs/docs/bec-expansion-proposal.md` still says "start at B-41" for its questions; the next free number is now
**B-59**.

**Suggestion.** Nothing to decide unless you want the Kerr questions to keep their branch numbers (that would mean
renumbering master's D-49 entries instead, which the brief and PROPOSAL already cite as B-34 / B-37).

---

### B-59 Final integration: the RC-2 dev-server test of master, and ruling 11a found missing [final integration test]

**What was run.** `runServer` (RC-2 deps, fresh flat world `sltest-final`, stdin console), the static lang lint and
`node tools/fieldsim-golden.js --check --java` on master `5efdfb6` and again after the fixes below.

- **Load.** `Recipe registration complete: 245 registered, 0 skipped` (243 after SL-P27/P28 retired); TecTech Eye
  recipes removed (43 + controller); 10 BHC->Kerr swap lines (Empty Spatially Compressed Circuit Array + the 9
  compression generator tiers, all `gt.recipe.assemble-condensate`); BHC controller recipe removed; 13 research items
  unique. No `[BTS]` warning or error; the GT load errors in the log are the pack's own (absent mods), as in earlier runs.
- **Lang lint.** All 18 literal `ofFailure` ids plus the generated `eye.tier1-6`, `eye.locked2-6`,
  `eye.compression1-6` and `kerr.module.<link>` ids have `GT5U.gui.text.recipe_result.` keys; the five shutdown reasons
  have `GT5U.gui.text.shutdown_reason.` keys; every `StructureErrors.of`, `translateToLocal`, recipe-map name, item and
  casing meta has its key. Nothing was missing.
- **Golden.** 78/78 PASS, the committed JSON is up to date.
- **Kerr.** T1 north (creative), T1 east (survival), T2 south (creative), T2 west (survival): KEE and both modules FORMED,
  re-projection keeps them formed, damage/repair works. Modules stay idle with the hole closed and run when it opens
  (extractor: GT item extractor recipes; compressor: GT compressor recipes at parallel 7, x4 speed). Closing the hole and
  unforming the KEE mid-recipe pause the compressor (progress kept) and it resumes. T2 DENSITY waits at 64 SM and runs at
  130 SM (SL-K05: 1,000 L Hyperdense Stellar Mixture). Render: one claimed block at the centre, cleared on close, on the
  off switch, while unformed and when the controller is broken; no orphans. A `legacy inflight` Kerr migrates (2 modules,
  the old recipe handed to the compressor, a fluid held in the KEE). After a restart the hole, render and running recipe
  are as before.
- **Eye.** T1-T6 each in two facings, creative and survival: all FORMED with the expected bands and compression tier.
  Ignition by a charge (protostar render, then lit), upkeep starvation (r 0, P sags, Sol lost to a Breach, failure line
  shown, render cleared), sensor-hatch regulation (bang-bang on P through the Field sockets keeps P in about -2..+9; with
  no regulation P drifts to +30), Mass Infusion, Shell Ignition, a failed Core Collapse (SpaceTime 0: SUPERNOVA, core
  kept in escrow, the reason stays in "Last failure", supernova flash render then air) and a successful one (T4 unlocked,
  neutron star render), First Cooling and T5 batches (Black Dwarf Matter per batch, milestones count good batches), the
  Magnetar Merger, the Agent upkeep at 3,000 L per 60 s cycle with the rig's Agent trim charging the shield (starved:
  shield 0, magnetar lost to decoherence, re-merge waits for 100,000 L of Agent). *Correction (B-60):* in this build
  the upkeep did NOT charge the shield; only the trim did. Ruling 12 (the upkeep charges it) is built in B-60. Unforming and breaking the controller put the star out and clear
  the render; no orphan render block at any of the 13 Eyes' centres. Restart: unlocked tiers, live stars, milestones,
  escrow and last failures are as before. Legacy 0.24.x NBT migrates (galactic -> unlocked T4; plain -> bands
  grandfathered).
- **BEC.** The server has 111 Condensate Assembler recipes. Ours: SL-P39 (WDM-free Resonance Chamber II) and SL-P33
  (BDM-free Resonance Chamber III) are in the map (`/btseye gates`: "a clean one exists" for both chambers, gates CLEAN
  for Eye T1-T7), and the RC-2 Spatial / Temporal casing recipes now output 32. Nothing can be crafted on the console:
  a Condensate Assembler needs a whole BEC network (generator, containment field, nanites), which the bench does not
  build.

**Fixed here.**
1. **Ruling 11a was not on master** (no branch had it): `BTSRecipesBEC` now rewrites RC-2's Condensate Assembler
   recipes for TecTech's Reinforced Spatial / Temporal Structure Casing to x32 at load-complete (SL-B01/B02: logged per
   hit, a WARN for an unexpected old size or several matches, idempotent, whatever the flags say) and SL-P27/P28 are
   retired. Verified: `SL-B01 ... now outputs 32, was 4`, same for SL-B02.
2. **Test bench:** `/btseye keep x y z facing [tN]` force-loads an Eye's footprint. Without it the dev server unloads the
   centre cell's chunk between console commands, and several early render checks read "nothing" for a burning star
   (a test artifact: with the chunks kept, every render check matched the star).

**Not done (open).**
- **SL-B03/B04** (the proposal's fallback BEC recipes if RC-2's casing recipe is missing) are not built: a missing
  recipe logs an ERROR saying the shell costs 8x what we intend. *Suggestion:* leave it; RC-2 has the recipe.
  *Correction (B-60):* that ERROR was wrong. RC-2 has no other recipe for these casings (TecTech's research-station
  one is gone), so a missing BEC recipe would make every Eye part uncraftable. SL-B03/B04 are built in B-60.
- **Ruling 11b, the BEC expansion** (6 condensates, 20 Condensate Assembler recipes, 20 retired ids) is still only the
  proposal (`designs/docs/bec-expansion-proposal.md`, PROPOSED). Nothing of it is on master. *Suggestion:* build it as
  its own pass once you have read the proposal; `BTSRecipesBEC` is where it would live.
- **Docs that still describe SL-P27/P28 as live:** `singularity-ladder-costs.md` §6/§9.5 cost rows (the build spec is
  updated). *Suggestion:* refresh the cost table with the x32 numbers from the proposal §3 in the BEC pass. *Done in
  B-60* (costs §3.2, §4.1, §5, §6 #3, §9.5).

**Small things seen (no change).** The module bench line prints `lastParallel 0` while a recipe runs (it shows GT's
field before GT fills it; after a restart it read 17). A legacy-layout Eye (`legacy` build) has no input bus, so it
cannot take a Stellar Ignition Charge until the player puts one on a plinth gas socket (`migration.md` already says so). A target change that puts a star out is recorded as the "Last failure" (by design: "the last way a star
was lost or put out"), so it replaces an older, more useful failure line.

### B-60 Final integration fixes: the Agent upkeep charges the shield, a BEC fallback for the Eye casings, the Kerr render on load [final integration fix]

**Context.** A review of the final-integration build found four things; all four were real and are fixed here.

**What was built, and the judgement calls.**

1. **Ruling 12: the Antipolarization Agent upkeep charges the T6 flare shield.** Built in the order the ruling sets:
   the prototype engine first (`designs/star-lifecycle-prototype.html`, design repo), then the fixture
   (`tools/fixtures/star-lifecycle-engine.js`, re-pinned), the golden file, and `StellarField.step`:
   `A <- min(100, 0.95·A + 4·u_agent/p + 2·r)`. `node tools/fieldsim-golden.js --check --java`: 78/78 PASS, bit
   for bit. **PROPOSED number: `SHIELD_PER_UPKEEP` = 2.** 3,000 L per 60 s cycle is 50 L/s = 0.5 Agent unit (100 L)
   per second; at the trim's 4 A per unit that gives 2 A/s. r is the cycle's delivered fraction (EU and every upkeep
   fluid, so for T6 the Agent), so a starved magnetar's shield decays. With no trim the shield settles at 40, which
   absorbs about 40 of a giant flare's 100 (25/s for 4 s) and keeps Π's noise near 0. Flares still need the Agent
   trim in a Field socket. The design-invariant check still counts only the trim (r may be 0). The Eye tooltip, the
   Magnetar chart tooltip, the NEI eyefield line, the T6 trim note and the costs doc §10 say so.
   - *Alternatives:* a bigger K (K = 5 settles at 100 and would absorb a whole flare with no trim, which makes the
     trim pointless), or scale the shield with litres actually drained per second instead of r (the upkeep is
     taken once per cycle, so that would need a per-second upkeep drain).
2. **The WDM 1,800 + BDM 450 L dwarf-matter top-up stays.** "One fluid runs the whole magnetar" could be read as
   dropping it, but your 2026-10-12 answer says "the dwarf-matter merger spawns the star **and tops it up**", and
   the 2026-10-13 ruling only makes the Agent do the upkeep and the shield. So the per-cycle running cost is one
   fluid (Agent) and the dwarf matter is the merger charge plus a top-up every 50 cycles.
   - *Alternative:* delete the reserve (`dwarfReserve`, `TOPUP_*`, its NEI and bench lines); then a magnetar needs
     dwarf matter only for the merger and a re-merge.
3. **The Eye casings can no longer become uncraftable.** RC-2's Condensate Assembler recipe is the only recipe that
   makes TecTech's Reinforced Spatial / Temporal Structure Casing (the coremod and Godforge only consume them;
   TecTech's research-station recipe is gone in RC-2). `BTSRecipesBEC` now matches a recipe on any output slot and
   rewrites that slot. If nothing matches: **SL-B03 / SL-B04** register RC-2's own recipe at ×32 (same 12 inputs,
   nanite tiers 4,1,1,2,1,1,4,1,1,1,1,2, Neutronium / Cosmic Neutronium / Bedrockium / Space or Time condensate,
   40,000 s UMV), every input through `req` / `nn`. If even that is skipped (an input missing from the pack), the old
   SL-P27 / SL-P28 Assembler bodies (×4) come back as a last resort with an ERROR. In RC-2 as shipped neither
   fallback runs. The wrong "falls back to TecTech's research-station recipe at 8x" ERROR and javadoc are gone.
4. **The Kerr's hole render no longer collapses and regrows on every load.** `tickRender` now waits for GT's first
   structure check (`mStartUpCheck`, as the Eye's centre render does). Before, at tick 0 `mMachine` was still
   false, so a formed Kerr with an open hole started GT's collapse, removed its block at tick 40 and placed a new one
   at tick 100.
5. **Costs doc** (the fourth finding): §3.2, a new §4.1 (Eye T1 14,936 Spatial + Temporal casings = 467 BEC crafts =
   14,936 Godforge casings, 22,404 Neutronium nanites, 239k ingots each of Neutronium / Cosmic Neutronium condensate,
   4.7e16 EU and 5,186 BEC hours; complete Eye T4 15,851 casings = 5.7 Godforge main rings), §5, §6 #3 and §9.5 now
   describe the ×32 BEC recipe. That is about 1.3× TecTech's stock RC-2 shell (11,456 Godforge casings, 3.6e16 EU).

**Dev-server results (RC-2, this build).** Load: `SL-B01 ... now outputs 32, was 4` and the same for SL-B02; no
SL-B03/B04 or last-resort line (RC-2 has the recipe). Kerr: after a restart, `/btskerr keep` on the T2 Kerr with an
open 130 SM hole, then `/btskerr render` every second for 11 s: the one claimed render block stood the whole time
(scale 6.5; the old code removed it at tick 40 and replaced it at tick 100). Eye T6: magnetar lit (debug), rig off (no
trims at all), r 1: after 30 s Π 0.0 and shield A 18 (it was 0 without trims before this fix); the field itself still
breaches on Θ/Ω without trims, as designed. Golden: 78/78 PASS.

**Suggestion.** Accept 1-5 as built. Re-check K after playing a magnetar: if flares feel unanswerable with the
standard rig, raise K; if the trim never matters, lower it.

### B-61 Package: what the test jar `0.24.1-master.137+175c89997a` contains [final integration package]

**Context.** The package task asked for one jar with the Kerr redesign, the D-49 Eye T1-T6, "the BEC expansion and
x32 structure casings" and the RC-2 lang fix, plus `dist-rc2/INSTALL.md` (design repo) with a dedicated-server
section.

**What was built.** `gradlew build` on master `175c899` (clean tree, spotless + checkstyle pass, nothing to fix). The
jar is `dist-rc2/beyondthestars-0.24.1-master.137+175c89997a.jar`, SHA-256
`6f29c4d7bd87c3d99f6075294f95394a28543327b74803d76b3511734432eab7`; the older `master.120` jar moved to
`dist-rc2/superseded/`. The judgement calls:

1. **The BEC expansion (ruling 11b) is not in the jar.** Master has only ruling 11a (SL-B01/B02 x32, SL-B03/B04
   fallback); the expansion is still the proposal (B-59). INSTALL.md says so plainly instead of listing it as
   shipped. *Suggestion:* build 11b as its own pass, then package again.
2. **Unmerged branches are not in the jar:** `questbook`, `homogeny-reality`, `tidy-old-multis`. The package pass
   does not merge (only the merge task may). INSTALL.md lists them under "Not in this build".
3. **`SERVER_DIR` stays a placeholder** in the dedicated-server command, with a note to replace it: the server's folder is
   not recorded anywhere in the repos.

**Suggestion.** Accept as is; tell us the dedicated-server folder and it goes into the next INSTALL.md.

---

### B-62 D-49 review fixes: paid parallel, first-cycle reserves, failures, milestones, migration, T4 recovery [D-49 review fix]

**Context.** The D-49 review (15 findings, 8 test problems) was checked again against master after unit 6, the Kerr
merge and the final-integration fixes. Already fixed before this pass: the centre render, the GUI, unit 6's cost table
and build-doc contract (B-45), the persistent "Last failure" line in the GUI and scanner (B-45), ruling 12 (the Agent
upkeep charges the shield, B-60), and INSTALL.md's retired `interimBlackDwarfAtEyeT4` text (B-61). The rest were real
and are fixed here. Numbered after master's B-61; if a branch merged later also used B-62 / B-63, the merge renumbers
as B-58 did.

**What was built, and the judgement calls.**

1. **Parallel paid vs used.** A lit second now runs at `min(live parallel, the cycle's paid parallel)`, and a T5 batch
   also at most the parallel its WDM charge paid for (`cycP`, `batchP`, tile NBT; 0 in an older save = no cap until
   the next charge). Product, HSM feed, trim caps and the field's per-parallel division all use it, and the fracture
   salvage uses the batch's paid parallel. Knocking a band out at the charge and putting it back no longer gives 32x
   the output for 1/32 of the upkeep. The scanner shows "the structure gives N from the next charge".
   - *Call:* a parallel that FALLS mid-cycle takes effect at once (a broken band costs the rest of that cycle's
     extra output). *Alternative:* refund, or keep the paid parallel until the cycle ends (that rewards breaking
     bands).
2. **The new star's first cycle is reserved.** A transition now begins only when the buffers hold its event fluids
   plus the new star's first cycle of upkeep x its parallel (Mass Infusion: + Sirius A's H / He / RSPM; Shell
   Ignition: + Arcturus's; the Core Collapse: + the neutron star's 144 L SpaceTime and one cycle of HSM feed at the
   setpoint; the merger: + 3,000 L Agent). The bounce absorbs SpaceTime only above the neutron star's 144 L x p, so a
   round 60,000 L buffer now absorbs 59 points (the scanner readout says what is kept). The NEI transition page shows
   the same totals at p = 1 (`EyeStarRuntime.transitionNeeds`, one method for both).
   - *Call:* the reserve is ONE cycle, not ignition's ten. *Alternative:* ten cycles (safer, much more buffer).
3. **Failures are kept and logged.** Every failure (star lost, transition failed or aborted, fracture, a First
   Cooling batch below mean Y 0.9, a batch put out) writes the persistent "Last failure" line and is logged at INFO;
   the automatic restart that follows changes only the event line. A plain chart change or a planet selection is the
   player's own choice: it is logged but no longer replaces a more useful failure line (B-59's note).
4. **Milestones count only on unlocked tiers.** The First Cooling batch no longer counts toward the six good batches
   for T5 -> T6, and the Resonant seconds and product of a star whose tier is still locked (the collapse settle, the
   merger imprint, Sirius A during Mass Infusion) do not count.
5. **Legacy NBT.** Schema-less NBT is a 0.24.x save only if it carries GT's tile key `mID`, a 0.24.x Eye key
   (`btsEye*`) or a key GT's `getDrops` writes on a dropped machine (upgrades, colour, covers, strong redstone). An
   anvil-renamed new controller starts at T1 instead of being grandfathered to T3. The bench's `legacy` sets `mID`.
6. **Grandfathering with unloaded chunks.** Every successful walk of a migrating Eye raises its unlocked tier (max,
   never lower); the migration is settled only by a walk that saw every chunk under the core, beam band and jet
   crescent loaded. An old T3 Eye first walked with its band chunks unloaded is no longer stuck at T1.
7. **T4 recovers unattended.** With T4 unlocked, a dark Eye holding the Neutron Star chart relights Arcturus from a
   Stellar Ignition Charge (10 cycles + seed, as a T1-T3 relight); Arcturus goes PENDING and re-runs the Core Collapse
   (no core) once its fluids are buffered. An igniting star whose chart is swapped for the next tier's now finishes
   its ignition and then goes PENDING (before, it was put out).
   - *Call:* each relight spends a charge, as T1-T3 relights do. *Alternative:* make the controller slot automatable
     (still two chart swaps).
8. **Research items.** GT's Scanner consumes the research item, so SL-E04 / E05 / E06 now research from Star Chart:
   Arcturus / Sirius B / SGR 1806-20 (cheap, unique, each made from the product of the tier the core leaves) instead
   of the previous expensive core. No other recipe (on any branch) researches from these charts. B-35 and the plan's
   §10.2 table are annotated.
9. **Salvage while unformed.** A batch put out because the structure broke owes its salvage (kept with the other owed
   outputs, item NBT, survives a wrench) and pays it the next second the Eye is formed; the line says "owed". Before,
   GT had already cleared the hatch lists, so the fluid was voided while the chat said "salvaged".
10. **Sensor analog scaling.** Analog mode maps the zone 0..4 onto 0/4/8/11/15 and the warning and upkeep flags to 15
    when set (they gave 0 or 1). The hatch tooltip says so.
11. **Sensor GUI sync.** The "no reading" value syncs as a sentinel (negative infinity), not NaN, so MUI2 no longer
    resends it every tick.
12. **Ruling 12 (the T6 Agent upkeep charges the shield)** was already complete in B-60: the prototype (design repo
    `feec8dd`), the fixture (re-pinned), the golden file and `StellarField.step` all use `0.95A + 4u/p + 2r`. Checked
    again: `node tools/fieldsim-golden.js --check --java` 78/78 PASS. No change.
13. **Small things.** The "does not reach TN" wait names all three limits (unlocked, bands, compression); it was
    confusing for a migrated jet-without-beam Eye. The Eye tooltip has three new lines (items 1, 2 and 7). Costs §9.4:
    the T5 gate row reads "loop, fixed by SL-P33", the interim-flag text is gone, and the T8 material is named
    Superdense Ergomatter (ruling 17).

**Not changed (design questions).** The merger's imprint band (+60..+90) sits in Strained / Critical and drains
Integrity about 0.6-0.7/s, so an Eye with Stabilisation tier 4 can fail it; the failure line now shows why. Plan test
17's "TierParams invariant check logs OK at boot" does not exist (the check is offline only). The questbook (its own
branch) should mention the T4 relight and the first-cycle reserve.

**Checks.** `spotlessApply compileJava` passes; golden 78/78 PASS. **Not run in-world** in this pass (no dev server):
the paid-parallel cap, the reserves, the T4 relight and the unformed salvage still need a `/btseye star` bench run.

**Suggestion.** Accept 1-13 as built.

---

### B-63 D-49: unlocking, transitions, T5 and T6 [D-49 unit 5]

**Context.** Unit 5 (commits `788e29f`..`8f626f5`) built the five transitions as field overlays, PENDING, success and
failure, the T5 cooling batches, the T6 magnetar, and the NEI transition and field pages. Its judgement calls were
never logged (B-38 item 3); the code cited "PROPOSED (B-38)" for the dwarf-matter numbers, which now cites B-63.

**Judgement calls.**

1. **The T6 dwarf-matter reserve (PROPOSED).** The merger charge fills a reserve of 60 cycles; each cycle spends one;
   at 10 cycles or fewer the Eye tops it up from the buffers with White Dwarf Matter 1,800 + Black Dwarf Matter
   450 L x p for 60 more; an empty reserve makes that cycle's r 0 (no output, P sags). This is the reading of "the
   merger spawns the star and tops it up". B-60 item 2 kept it after ruling 12.
   - *Alternative:* no reserve; dwarf matter only for the merger and a re-merge.
2. **T5 fracture salvage: 25 % of the batch x the fraction done** (plan §6.6), then I 100, lock 0 and the next batch
   (or IDLE). B-62 item 9: owed while unformed.
3. **The Purified Tengam return:** 12 dust per 1,000 L of Agent consumed (upkeep, merger charge and trims) to the ME
   output bus; whole dust out, the rest carried in tile NBT.
4. **PENDING is T1-T3 only.** A lit star with the next tier's chart is PENDING, and the transition begins the moment
   every precondition holds. The First Cooling (T4 -> T5) and the Magnetar Merger (T5 -> T6) start from a dark Eye
   with the Sirius B / Magnetar chart in the slot, because neither has a lit predecessor to transform.
5. **The merger re-spawns automatically** from a dark Eye with the Magnetar chart: every attempt takes the full charge
   (WDM 18,432 + BDM 4,608 + Agent 100,000, x p); no core after the first success.
6. **The First Cooling retries automatically:** a failed First Cooling batch leaves the Eye IDLE, and the next WDM
   charge starts another attempt (each takes a batch charge; the core stays in escrow).
7. **A transition's new star runs at p 1** before its tier is unlocked, if the bands and compression reach it.
   Transitions hold the time dial at m = 1.
8. **A running transition lives in the tile NBT only:** a wrench aborts it (its materials are lost, the core stays in
   escrow), and the escrow cannot be taken back while it runs.
9. **The overlays (plan R-4, R-6).** Mass Infusion: 600 s with P drift +4 (RSPM 5,000,000). Shell Ignition: Θ ramps
   in over 600 s with T3's trims and the ash off, a forced helium flash warned at 585 s and hitting at 600-602 s,
   Arcturus at 603 s. Core Collapse: bounce +150 (B-25; the bench's `set shock 90` reproduces the golden cases).
   Merger: Π drifts +0.5/s with no flares, imprint inside +60..+90 for 120 s within 600 s, release inside ±40 within
   60 s.
10. **Status ids** `star_transition`, `star_batch`, `star_idle` and `star_merger` (with recipe_result keys). The next
    tier's chart is not hidden behind its locked gate, since putting it in is how the tier unlocks.

**Suggestion.** Accept 1-10. Re-check 1 and 6 after playing T5 / T6: with 6, each failed First Cooling spends
18,432 L of WDM without asking.

### B-64 BEC expansion: the Condensate Assembler takes the ladder's parts, cores and charts [BEC expansion, ruling 11b, built]

**Context.** Ruling 11b: "we need way more things in the bose einstein". Built exactly per
designs/docs/bec-expansion-proposal.md (build spec §2.5, costs doc §11): six addon condensates with Condensate
Generator recipes (SL-B10..B15) and 20 Condensate Assembler recipes (SL-B20..B81). SL-B01..B04 (ruling 11a's x32 and
its fallbacks) were already on master and are unchanged. Compile-checked (`spotlessApply compileJava`); not yet booted
on a dev server or seen in NEI (see "Not verified" below).

**Your 14 proposal questions, all taken as recommended:**

| # | Question | Built |
|---|---|---|
| Q-B1 | The cores and the Density Upgrade lose their research steps (BEC recipes have none) | Accepted: SL-E02..E06, SL-K04 and SL-P21's scans are gone |
| Q-B2 | Galactic Shell Plating | SL-P17 (Assembler, HSM: the T4 gate) stays; SL-B62 (x64, needs WDM) is a batch alternative for the T5/T6 bands |
| Q-B3 | Harmonic Lining | SL-B34 (x64) added as an alternative; SL-P12 stays (Kerr T1 is pre-BEC) |
| Q-B4 | x32 makes the casings 8x cheaper pack-wide | Accepted (ruled "regardless of flags") |
| Q-B5 | 48 Neutronium nanites per Spatial/Temporal craft | Accepted as RC-2's economics; not rewritten |
| Q-B6 | Magnetar Matter condensate (SL-B16) | Deferred: not registered here. The Homogeny/Reality branch registers `btsentangledmagnetar` with its first consumers, same name, so the two merge cleanly |
| Q-B7 | SL-P29 (Boundary x8) | Unchanged |
| Q-B8 | A config switch for BEC routes | None: the old recipes are automatic fallbacks only |
| Q-B9 | Kerr T2 needs the BEC network | Accepted (Kerr T2 already needs Eye T1, which needs the BEC) |
| Q-B10 | Our condensates are not in the Diode's "filters from input hatches" mode (GT maps its 19 enum types only) | Accepted; manual filter slots take any fluid |
| Q-B11 | Stellar Field Sensor Hatch in the BEC | Included (SL-B22) |
| Q-B12 | Metamaterial Film, Intergalactic / Homogeny cores | Not now; SL-B90+ reserved |
| Q-B13 | Generator tiers | RSM 30 s UIV, ESPM 60 s UMV, HSM 90 s UMV, WDM 120 s UXV, BDM 150 s UXV, Agent 60 s UMV |
| Q-B14 | WDM in the Magnetar Seed Core | Added: 256 ingots (36,864 L), the merger's white dwarf |

**Judgement calls in the build:**

1. **The safety net counts what GT kept.** GT's `addTo` returns an empty list (and logs) when it rejects a recipe, which
   the guard used to count as "registered". `BTSRecipesBEC` throws on an empty `addTo`, so a rejected BEC recipe is a
   skip and its old recipe registers. The skip still appears in the boot summary ("N registered, 1 SKIPPED") even
   though the part stays craftable, so a pack change is visible. A condensate whose generator recipe fails makes every
   recipe that needs it skip and fall back, so no recipe can ask for a condensate nothing makes.
2. **Where the old bodies live.** Moved verbatim into `BTSRecipesBEC` as `legacyXnn()` methods; `BTSRecipesSingularity`,
   `BTSRecipesEye` (now only SL-E01) and `BTSRecipesKerr` keep a one-line comment where each recipe was.
   `BTSRecipesEye.denseSpaceTime` is package scope now (shared).
3. **Stacks above 64** (e.g. Harmonic Lining's 72 Neutronium plates, dense-plate fallbacks) are split into 64-stacks
   that share the slot's nanite tier, so the nanite array always matches the item array (GT's map throws otherwise).
4. **Condensate names**: `btsentangledstarmatter`, `btsentangledexcitedplasma`, `btsentangledhyperdense`,
   `btsentangledwhitedwarf`, `btsentangledblackdwarf`, `btsentangledantipolarization` (frozen once shipped). Their
   display names contain the gate-audit fragments ("raw star matter", "white dwarf" ...), so `/btseye gates` would
   catch any of them in a compression generator.
5. **Tooltips (rule 14).** The five cores, the six charts, the Stabiliser Pylon and the Kerr Density Upgrade say
   "Crafted in the Condensate Assembler (Bose-Einstein Condensate)" and with which condensate; the Pylon, the Density
   Upgrade and the Sol / Sirius A / Arcturus charts switched to the tooltip item class to show it (the item class is
   not saved in worlds). The questbook branch should mention the BEC route on the Eye T1 and Kerr T2 quests.
6. **Costs doc section** is §11 (the proposal said §10, which D-49 took).
7. **Rule 17 (Superdense Ergomatter)**: master's code, lang and docs had no "Supermassive Ergomatter" left (grep), so
   nothing to rename here. **Rule 19 (loop fixes)**: none of these recipes uses Magnetar Matter or Ergomatter, so
   neither fix applies; the UXV parts in SL-B70/B80 (Field Generator, Emitter, Sensor UXV, UXV circuits) are the same GT
   parts SL-E05/E06 already used (they needed Universium, Eye T10; replaced by UMV parts in B-72).

**Gate audit.** Clean by construction (proposal §5.7): Eye T1 parts have no Raw Star Matter, no Boundless Cosmic
Solder and nanites at most Transcendent; each later part uses only products of the tier below (a transition core
also its own tier's); White Dwarf nanites first appear in the T4 -> T5 core, Black Dwarf in the T5 -> T6 core. No
compression generator recipe changed.

**Not verified (needs a dev-server boot):** the boot log should show 6 generator recipes, 20 assembler recipes, no
"was skipped; its old recipe" lines, 0 skipped; NEI's Condensate Generator page should list the six fluids and the
Condensate Assembler page SL-B20..B81 with their nanite tiers; a scripted craft of SL-B21 (Sol chart) on a dev network,
and SL-B40 without Raw Star Matter condensate failing at the end with "missing condensate".

**Suggestion.** Accept as built. Watch the Density Upgrade's 4M L of Raw Star Matter condensate in play: it is the
largest single BEC bill in the ladder (the same Raw Star Matter as SL-K04 asked for, now through a Condensate Generator).

---

### B-65 Real-client render QA, part 1: the screenshot harness and round 1 [render QA]

**What was built.** A dev-only harness (`rendertest/RenderTest`, `RenderTestClient`) that is registered only when the
client JVM runs with `-Dbts.rendertest=true`. It has no effect in normal play. Run it with
`JAVA_TOOL_OPTIONS="-Dbts.rendertest=true" ./gradlew.bat runClient --no-daemon`: this buildscript's `runClient` takes
no JVM-argument property, and `JAVA_TOOL_OPTIONS` reaches the forked client. Add `-Dbts.rendertest.only=<prefixes>`
to run a subset (e.g. `kerr,ref_gt`).

The harness creates or reuses the creative superflat world `run/client/saves/bts-rendertest-L2` on the integrated
server. It builds 10 sites with the existing kit code (`KerrKit.build`, `EyeKit.build`, so the same path as `/btskerr` and
`/btseye`) and force-loads each site. For 14 cases it shoots front, three-quarter elevated and close-up views, by day
(time 6000) and by night (18000), into `run/client/screenshots/rendertest/<case>_<view>_<day|night>.png`. It logs the
debug-counter FPS at each shot (`rendertest.log`, `fps.tsv`) and then closes the client.

**Judgement calls (built, PROPOSED):**
1. **The Eye's objects are forced, not lived.** Driving a real star into each state (T5 batch, magnetar, Core
   Collapse phases) would mean running the lifecycle for hours. Instead `EyeCentreRender.debugForceLook` (via
   `MTEEyeOfHarmonyBTS`, never saved, null in play) shows the exact `EyeStarLook` that `EyeStarLook.of` computes for
   that state. `swell(f)` and `bounce(unrest)` were extracted from `of()` with the same numbers, and `lit` and `dwarf` are now public.
   The cases use Sol / Sirius A / Arcturus / neutron / magnetar lit with no unrest, Core Collapse mid-swell at f = 0.5,
   the bounce with no unrest, and Sirius B at 10 % and 90 % of its cooling batch. The Core Collapse looks are shown on
   the T4 structure (the transition builds into T4).
2. **The Kerr's hole** is set with `debugOpenHole(mass)` and held there once a second (T1 16 and 64 SM, T2 256 SM).
3. **References.** GT's Black Hole Compressor is built by its own `construct`. Three of its hatch casings nearest the
   controller become a creative energy hatch, an input bus and an output bus (it forms). The hole is opened the way a
   seed opens it: status 2 and GT's private `createRenderBlock`, called by reflection. The harness holds its stability
   at 100. TecTech's Eye of Harmony is built by its own `construct` (tier-9 generators) but does not form (no hatches,
   no recipe). TecTech only places its star while a recipe runs, so the harness places the star the way
   `createRenderBlock` does (render block 16 behind the controller, rocket tier 8, size 1.4). The controller is
   disabled, because an unformed TT multi that is allowed to work removes its star at every check.
4. **Viewpoints.** Every structure faces north. "front" stands level with the structure's middle, far enough that its
   box fits a 70 degree view. "34" is front-right and raised. "close" stands inside the open space around the object,
   between it and the front, at max(0.55 x the free distance, 1.9 x the object radius + 1.5).

**Harness bugs found and fixed while running it (not mod bugs):**
- Forge queues a chunk for unloading the moment its force-load ticket is released, even with a player in it, and
  unloading that chunk takes the player entity out of the world. The first runs released the previous site's tickets
  before the teleport, so the server stopped ticking the player and never sent another chunk; those shots showed
  only sky. The harness now keeps its own tickets and releases the old site's 100 ticks after the player leaves.
- The client player only sends movement packets while the chunk under it exists, so every camera spot is loaded on the
  server before the teleport.
- dreamcraft's "really quit?" Swing window cancels `Minecraft.shutdown()`, which hung the first unattended run. The
  harness switches `DreamCoreMod.showConfirmExitWindow` off for its session only.
- A reused world keeps an older site layout's structures, so the world folder names the layout (`-L2`).

**Round 1 (84 shots, all 14 cases; copies in my local work folder `render-qa/round1/`).** RTX 4090, 1280x720,
16-chunk render distance, no frame cap. Average FPS at the shots (min-max):

| Case | FPS | Case | FPS |
|---|---|---|---|
| kerr_t1_16sm | 1247 (1223-1267) | eye_t4_collapse_swell | 1120 (1070-1178) |
| kerr_t1_64sm | 1237 (1203-1254) | eye_t4_collapse_bounce | 1134 (1094-1203) |
| kerr_t2_256sm | 1169 (1123-1191) | eye_t5_dwarf_early | 1097 (1061-1151) |
| eye_t1_sol | 1211 (1200-1233) | eye_t5_dwarf_late | 1101 (1082-1127) |
| eye_t2_sirius_a | 1221 (1186-1251) | eye_t6_magnetar | 1049 (887-1154) |
| eye_t3_arcturus | 1180 (1165-1196) | ref_gt_bhc | 1301 (1258-1371) |
| eye_t4_neutron | 1139 (1101-1203) | ref_tt_eoh | 1176 (855-1271) |

None of our renders costs measurable frame time on this GPU. The spread comes from structure size: the T6 Eye is the
largest box and has the lowest average.

**First observations for part 2 (no change made here):**
- **Late cooling dwarf.** It reads as a dull brick red (rgb 6D322D, gamma 0.66), not near-black. D-49 wants it to
  "darken to black".
- **Close-up of the largest objects.** The Core Collapse swell (radius 16) and Arcturus (15) fill the whole close-up
  frame. Part 2 could pull the close view back for radii above about 12.
- **GT's `gregtech:orbit` shader** does not compile on this NVIDIA 610.88 driver ("ambiguous overloaded function
  reference min(int, int)"). GT logs that it will not render, which probably affects TecTech's orbiting planets in the
  reference. That is GT's bug, not ours.
- **GT's Black Hole Compressor** keeps one chunk re-rendering every second while its hole is open. The Kerr does not.

**Suggestion.** Keep the harness as the standard way to check renders. Part 2 reviews the round-1 images against the
mockups and acts on the observations above.

### B-66 Render fix round 1: the Kerr hole's size, the Eye's glow, colours and flares, and round 2 [render QA]

**What was built** (addon commits 06b16e3..e479cb9). It fixes the defects the part-2 review found in round 1 (B-65).

- **Kerr K1, size and clipping.** The core scale is now logarithmic in mass: `clamp(3.2 + 0.75 log2(M / 16), 1.5, 6.4)`,
  which gives 3.2 at 16 SM, 4.7 at 64 SM and 6.2 at 256 SM. The visible halo/disc reaches 1.35 times the core scale:
  4.3, 6.3 and 8.4 blocks, and at most 8.6 at the 512 SM T2 cap. Before the fix it was about 11.5 blocks at 64 SM.
  This keeps the halo about 1.5 blocks inside the lowest coil ring and inside the interior ring of cyan-eye casings.
  GT's model draws its halo at about 1.8 blocks per unit of GL scale (measured in round 1), so the KEE draws it at
  0.75 times the core scale. The functions are `MTEKerrEngine.coreScaleFor`, `haloRadiusFor` and `renderScaleFor`.
- **Eye E1/E9, glow.** Every object gets an additive glow billboard. Its radius is 2.5 times the object's radius, at
  least 6 blocks. It uses the object's colour at alpha 0.30, falling to 0 at the edge. It is depth-tested and writes
  no depth. It stands just behind the object (scaled up so it looks the same size), so the star's face stays clean.
  The glow shows through the crown band's gaps.
- **Eye E9, beams.** The neutron star and the magnetar sweep two polar beams: 12 blocks long, 0.4 wide, additive
  alpha 0.5, tilted 20 degrees off the vertical and turning with the spin.
- **E2, shells.** The two shells are additive. The NOVA core pass keeps ONE_MINUS_SRC_ALPHA.
- **E3/E4/E5, colours.** The colours are as the review proposed: Sol FFF0C8 at gamma 4.2, Sirius A C8DCFF at 4.8, the
  neutron star DCE8FF at 6, the bounce C8E1FF at 6. The swell's gamma falls from 2.6 to 1.6.
- **E7, cooling dwarf.** It uses the 4-stop palette. Its shells fade by (1 − cooling), and it gets the faint dark-red
  rim.
- **E8, magnetar.** It gets a flare shell (1.8 times its radius, B070FF, alpha 0.15 + 0.45 × warning). It also flares
  every 80-160 ticks, out to 6 radii over 40 ticks, using the bounce shell's code. The warning reaches the client as
  a new synced value, `EyeStarLook.aux`, which for the dwarf carries the cooling fraction.
- **E10, chunk churn.** `TileEyeStar.setLook` sends the look as a description packet straight to the players watching
  the chunk. It used to call `markBlockForUpdate`, which also sent a block change, so every client rebuilt the chunk
  section. `differs()` now allows 4 per colour channel.
- **Harness.**
  - The bounce case freezes its shell at 30 ticks (dev-only `TileEyeStar.debugEventAge`).
  - A new case, `eye_t6_magnetar_warn`, shows the magnetar with warning 1 and a flare frozen at 15 ticks.
  - Close-ups of objects with a radius over 12 stand at 2.2 radii + 4. The camera climbs over the crown until it
    has a clear line.
  - The Kerr cases log the core scale, the halo radius and the GL scale.
  - `RenderDirtTracker`, a client world listener, logs what re-rendered chunk sections during each shot's still
    window.

**Judgement calls (built, PROPOSED). The review's numbers that did not hold up in the real client:**
1. **Shell strength.** At the review's 0.4 / 0.2 and the star's own gamma, the additive shells blew Sol, Sirius A, the
   neutron star and the white dwarf out to flat white. The shells now run at the review's fallback of 0.3 / 0.15,
   with their gamma capped at 1.2. Round 2 shows Sol yellow-white and Sirius A blue-white, both with visible texture,
   and no cyan blobs.
2. **Why the review's dark colours do not darken.** The tectech:star shader (checked in the RC-2 jar) keeps the
   texture's luminance and takes only the tint's chroma (YIQ). A dark tint therefore does not darken the ball; only
   the gamma does (luminance^(1/gamma)). The review's late dwarf gammas (1.2 / 0.6 / 0.4) still rendered a pink-grey
   Moon at 90 %. The late stops are now 0.45 / 0.15 / 0.08. At 90 % the ball reads dark copper-brown with some bright
   glints, and the rim is at gamma 0.6. Going fully black would need a shader of our own (see the suggestion below).
3. **Magnetar colour.** It is 6A3CFF at gamma 1.2, not 8A3CFF at 2.2, which rendered white-pink. At night it reads
   violet with a violet halo. By day the core still reads pink-violet, because the chroma swap puts its hue near
   magenta. Its flare shell runs at gamma 1.
4. **E10 diagnosis.** Over 90 still windows, the only things that re-rendered chunks at the Eye sites were
   `minecraft:dirt` block updates: superflat grass dying in the shade of the freshly built structures. None came from
   the Eye's render tile or its hatches. Round 1 ran on a fresh world, which explains most of its 4-18 updates/s. In
   round 2 (the same world) the Eye sites average 0-1.8 per shot. The setLook packet change still removes a rebuild
   per look change in live play (the cooling dwarf changed colour every field second).
   - Also seen: GT's BHC re-renders its render block once a second.
   - Also seen: the KEE controller asks for a block update about every 8 ticks, but only for a nearby player (5 per
     40-tick window, close views only). This is probably GT's client data / active-flag traffic. It is harmless at
     these numbers. If it matters on weak clients, investigate.

**Round 2.** 90 shots, all 15 cases, copied to my local work folder `render-qa/round2/`. The T5/T6 shots come from a
re-run after the last tuning (`rendertest-t5t6-rerun.log`). The first pass, before the shell and dwarf tuning, is in
`round2/pass-a/`. FPS at the shots: Kerr 1152-1269, Eye 978-1209 (lowest: Sol 34-view), BHC reference 1134-1315,
TecTech EOH reference 1237-1290.

**Open design questions (no render change made):**
- **K2: the hole is hidden from the front at eye level.** It is hidden by the drum at T1, and at T2 only two cream
  crescents show. GT's BHC behaves the same. Suggestion: add a Quantum Glass window, 3 high and about 9 wide, in the
  drum's front wall at the hole's equator (a structure change: re-export the Kerr shapes and the BOM). The
  alternative is to accept it.
- **E1: the crown at eye level.** With the glow, the Eye's object reads from the front at night as a bright slit
  through the band gaps with a halo above the crown. By day the additive glow barely shows against the sky. TecTech's
  Eye is fully readable from the front. Suggestion: open a 3-block window ring in the crown band at the object's
  height (a structure change). The alternative is to accept "look from above / inside", as the Forge of the Gods does.
- **Dwarf and magnetar colour.** To reach a true black dwarf and a non-pink violet magnetar, ship our own copy of the
  star fragment shader with a brightness multiplier (a tint that scales the luminance as well as the chroma). That
  adds about 30 lines of GLSL in two profiles. Suggestion: do it in round 2 of the fixes if you want them exact.

**Settled 2026-10-13 (your answer to K2 / E1: "Glass window bands").** Built in B-86. The colours were reached
without a shader of our own (B-67, B-68).

### B-67 Render fix round 2: the Eye's colours, glow seam and black dwarf, the KEE's chunk churn, and round 3 [render QA]

**What was built** (addon commits 0eedb59..fc1848c). It fixes the round-2 review's defects E-1..E-8, P-1 and H-1. E-9
(readability from the front) is a structure question and stays open (K2 / E1 in B-66). The Kerr's render needed no
change.

- **E-1..E-4, E-6: colours.** Every look's core gamma is capped at 2.5 (`EyeStarLook.MAX_GAMMA`, applied in the
  constructor, so the protostar blend and the nova flash obey it too). The tints are saturated, because the star shader
  keeps only the tint's chroma.
  - Sol is FFE08A at gamma 2.0. Sirius A is 9EC2FF at 1.8. The neutron star is B8D0FF at 2.5.
  - The bounce's proto-neutron star is B8D4FF at 2.0.
  - Arcturus is FF6A20 at **0.9**. The swell starts at Arcturus's gamma and falls by 0.3.
  - The magnetar core is 4A30FF at **0.7**. Its flare shell is unchanged (B070FF, gamma 1).
  - The cooling dwarf's early stop is **B8CCFF at 1.5**, and its half-way stop runs at gamma **1.8**.
- **E-1, shells.** The two additive shells run at 0.12 / 0.06 for the lit stars, the swell, the neutron star, the bounce
  and the dwarf. The protostar, the magnetar and the nova keep 0.3 / 0.15.
- **E-8, limb ring.** Every kind's shells sit at 1.01 / 1.02 radii, not 1.02 / 1.04.
- **E-5, glow seam.** The glow disc now passes through the object's centre. It is no longer pushed back or scaled up.
  Its radius is `min(max(6, 2.5r), 20.4)` blocks, so it never reaches the nearest `eye_core` block (20.9 blocks out).
  The core's depth still hides its middle.
- **E-7, black dwarf.** An untextured, alpha-blended sphere in 0E0A0C is drawn over the core. Its alpha is
  `0.85 × clamp((cooling − 0.5) / 0.4, 0, 1)` and it writes no depth. The dark-red rim (1.03r) is kept.
- **P-1, KEE churn.** The cause was not client data or the active flag. The harness's new server-side packet spy showed
  the server sends ONE tile description packet per KEE structure re-check, every 200 ticks. GT's
  `MTEEnhancedMultiBlockBase.onStructureCheckFinished` issues a tile update after every formed check, to sync the
  structure's sound centre. GT's client reads that packet through four client events plus one texture update, and each
  marks the block. That is the "5 block updates per window" seen in round 2: one chunk rebuild every 10 s, not 2.5 per
  second.
  - The fix is the new `machines/QuietTileUpdate`. The KEE passes GT's request through only on the first formed check
    (after a load, a placement or a re-form) or when the centre or radius moved.
  - In round 3, no shot window shows a KEE block update.
- **H-1, harness.** Close-ups use "2.2 radii + 4" from radius 9 (it was over 12). Sol's camera is now 23.8 blocks out
  and Sirius A's 28.2.
- **Harness diagnostics.** The client dirt tracker logs the call path of the first machine block update in each window.
  A server-side tracker logs block-update requests. A dev-only `PacketSpy` (harness runs only) replaces the harness
  player's net handler. It counts the tile description packets and the GT block events the server sends per shot, with
  the call path.

**Round 3.** 90 shots are in my local work folder `render-qa/round3/`. The full run is in `rendertest.log`.
Retuned cases were re-shot and overwrite the full run's files; their earlier shots are in `pass-a/`, `pass-b/` and
`pass-c/`:
- `rendertest-retune.log`: Arcturus, the Core Collapse, T5 and T6.
- `rendertest-retune2.log`: the bounce and the early dwarf.
- `rendertest-retune3.log`: the bounce.

The magnetar-warn shots were taken with the flare's shock shell at gamma 1.5. It is now 1.2, which is slightly dimmer.

Measured on the close-ups (disc samples, same method as the review's judge2 statistics):

| Case | Round 2 | Round 3 | Target |
|---|---|---|---|
| Sol | FEFEF7, 71-77 % white | F8E695 / F0DA82, 0 % white, median 216-229 | < 15 %, about FFE6A8 |
| Sirius A | FEFEFF, 93-98 % white | B9D8FB / BFDFFD, 3-8 %, median 209-216 | < 15 %, about C0D6FF, 200-215 |
| Arcturus | F3EC93, median 233-240 | FC7022 / F78D3C, 0 %, median 139-170 | about FF9A50, about 170 |
| Swell (half way) | F2C47C | F07A48, median 153 | redder than Arcturus |
| Neutron star | DDE4E6, 76 % white | D6E7FC, 6-10 % | < 20 % |
| Bounce | 100 % white | C0DEFB / C4E1FC, 2-6 % | < 20 % |
| Dwarf 10 % | 100 % white | D4C9E0 / E4D9ED, 9-19 % | < 20 % |
| Dwarf 90 % | 462623, p90 74-77 | 150D0F, p90 21; the rim limb shows at night | ≤ 1E1416, p90 ≤ 35 |
| Magnetar | FEDCFF / FDCAFF | B080FF / B584FF, 0 % | about A070FF |

FPS at the shots: Kerr 1165-1306, Eye 957-1243 (lowest is the neutron star's 3/4 view), BHC reference 1239-1331,
TecTech EOH reference 1281-1318.

**Judgement calls (built, PROPOSED). The review's numbers that did not hold up in the real client:**
1. **Arcturus at gamma 0.9, not 1.5.** At 1.5 the shots measured FEC072, median 204, which is still golden. The
   texture's luminance has to come down before the shader's chroma reads orange.
2. **Magnetar at gamma 0.7, not 0.9.** 0.9 measured C597FF (lilac). 0.7 lands on B080FF.
3. **Dwarf early stop at B8CCFF / 1.5, not E6EEFF / 2.2.** The review's values measured 41-54 % white. The case sits at
   10 % cooling, where the blue-to-orange blend passes through grey, and grey adds no chroma. Only a lower gamma keeps
   the texture. The half-way stop runs at 1.8 for the same reason.
4. **Bounce.** The proto-neutron star is at gamma 2.0 (not 2.5). The shock shells (the bounce's and a magnetar flare's)
   run at gamma 1.2 (not 4), and the bounce shell at alpha 0.25 (not 0.6). The magnetar flare's burst is at alpha 0.3
   (not 0.4). At gamma 4 the bounce shell alone blew the star out to 97 % white.
5. **Veil geometry.** The veil's vertices stand out by `1 / (cos(π / slices) × cos(π / 2·stacks))`, about 1.007 at
   32 × 24. Its flat faces therefore clear the core's depth. With the vertices at 1.003r the faces sank up to 0.9 %
   inside the core, so the depth test hid most of the veil. It is drawn after the shells and the rim, so it also darkens
   the rim's face, and only the rim's limb shows. That is the "rim still visible" reading.
6. **P-1: a shared helper rather than a KEE-only patch.** `QuietTileUpdate` can serve any of our controllers that
   re-check on a timer. Only the KEE uses it now. The Eye was not flagged.

**Still open:** E-9 (K2 / E1 in B-66): Quantum Glass windows in the Eye's crown band and the Kerr drum, or accept the
"look from above" behaviour. Both are structure changes (shapes, BOM, recipes), so they wait for your ruling.
**Settled 2026-10-13 (your answer: "Glass window bands"):** built in B-86.

### B-68 Render fix round 3: the Eye's see-through layers in pass 1, shock-front shells, corona and limb, and round 4 [render QA]

**What was built** (addon commits 9755807..f922a1d). It fixes the round-3 review's Eye defects D1-D7. The Kerr needed
no change (the review passed it).

- **D1, sorting.** The Eye's render tile now renders in passes 0 and 1. Pass 0 draws the object: the opaque core, the
  two thin additive surface shells, the dwarf's dark-red rim, the new limb darkening and the black dwarf's veil.
  Pass 1 runs after the translucent and glowing block faces. It draws everything see-through around the object: the
  corona, the glow disc, the pulsar beams, the magnetar's shells, the bounce shell and the nova flash. These layers are
  additive, depth-tested and write no depth, and back faces are drawn before front faces. In the round-4 shots the
  bands' cyan strips behind the bounce shell and the magnetar shells are tinted.
- **D2, bounce.** The bounce is now an untextured shock front, B8D4FF, with additive alpha
  `0.04 + 0.60 × (1 − |N·V|)^3` per vertex. It is clear in the middle with a bright edge, and fades with
  `1 − age / 100`. At 30 ticks it is 7.4 blocks across. The cloud texture is gone.
- **D3, magnetar.** The magnetosphere is 4.5 blocks, C080FF, rim alpha 0.04 → 0.3 when quiet. On a warning its edge
  pulses 0.35 ↔ 0.6 at 2 Hz, and an outer shell appears at 7 blocks (7A4CFF, edge 0.29 ↔ 0.5). Flares run from 4.5
  to 7 blocks (edge 0.5, fading). The close view is no longer veiled.
- **D4, swell.** The swell eases from 15 to a peak of 20 blocks (19.4 at half way). It reaches FF3A12 at gamma 0.6 by
  half way, spins at 3× Arcturus's speed at half way, and throbs at 0.7 Hz with pulse 0.15. Measured F8280C against
  Arcturus FC8130.
- **D5, early dwarf.** F2F5FF holds until 20 % of the batch, at gamma 2.5 → 2.3. Its surface shells run at 0.3 / 0.15,
  and the dark-red rim fades in with the cooling, as the veil does. Measured F7F8F9 by day and F0F2F6 by night: every
  channel is at least 235 and blue is at or above red. The late dwarf is unchanged (170E10).
- **D6, corona and limb.** Every live star, the swell, the bounce and the early dwarf get a corona: a ring from the limb
  out to 1.12× the limb, star colour, 0.30 at the limb falling to 0. It lies in the limb's own plane, so it neither hides
  the star nor is hidden by it. The core is limb-darkened to `colour × (0.75 + 0.25 |N·V|)`.
- **D7, pulsar.** Beams are 0.45 blocks wide each side, 0.6 at the root falling to 0, with the tip 19.4 blocks from the
  centre. The neutron star's glow ring reaches 1.6× its limb at 0.45.
- **Meshes.** Every sphere is 32 × 16, the core included (it was 64 × 64). The untextured spheres use a unit-sphere table
  built once, with per-vertex alphas filled once per shell for both faces. The first run built them with trig every
  frame, and the magnetar fell to 348-939 FPS.
- **Harness.** New `ref_gt_godforge` case: TecTech's Forge of the Gods render block alone on a site (x 6000), set up as
  `MTEForgeOfGods.createRenderer` sets up a fresh Godforge (star 20, speed 5, one ring, default colour).

**Round 4.** 96 shots are in my local work folder `render-qa/round4/`. The Kerr and reference shots come from the full
run (`rendertest-full.log`, `fps-full.tsv`). The Eye shots come from the re-run after the mesh-table fix
(`rendertest-eye.log`), and the T5 shots from the dwarf retune (`rendertest-dwarf.log`). Each run's raw output is in
`run1/`..`run3/`.

| Case | Close-up centre, day | FPS avg (min-max) |
|---|---|---|
| Sol | EFDA8A | 1122 (1109-1138) |
| Sirius A | BDDDFC | 1119 (1086-1155) |
| Arcturus | FC8130 | 1115 (1080-1138) |
| Neutron star | D6E7F9, 8 % white | 1074 (1041-1130) |
| Swell | F8280C | 1066 (1043-1088) |
| Bounce | C2DCF9 | 1023 (978-1065) |
| Dwarf 10 % | F7F8F9, 83 % white | 1061 (1022-1117) |
| Dwarf 90 % | 170E10 | 1011 (946-1055) |
| Magnetar | 845DFF | 929 (879-1017) |
| Magnetar warning | - | 855 (765-916) |
| Kerr T1 16 / 64, T2 256 | - | 1248 / 1262 / 1185 |
| BHC / TT EOH / Godforge refs | - | 1319 / 1318 / 1283 |

Round 3's Eye cases ran at 957-1212.

**Judgement calls (built, PROPOSED):**
1. **Swell peak 20 blocks, not 21.** The nearest `eye_core` block face is 20.4 blocks from the centre (measured from the
   exported shape: cell (0, −6, −20)), not the review's estimated 22-24. The throb only shrinks the swell (by a third of
   the pulse) and swings its brightness (gamma ×(1 ± 0.15)) by the full pulse, so the swell never reaches a block.
   Every see-through layer also stops at 20.4: the glow disc, the corona's outer edge and the beam tip (minus 1). The
   swell's corona is therefore only about one block wide.
2. **Quiet magnetosphere at edge 0.3, outer shell only on a warning.** The review gave the warning's numbers only. At
   rest the inner shell runs at half strength and no outer shell is drawn, so a warning reads as a clearly bigger,
   pulsing violet ball.
3. **The neutron star's 1.6× glow replaces its 1.12× corona** (0.45 at the limb), rather than adding a second ring.
4. **Dwarf at 83 % near-white by day.** ">= 235 on every channel" with blue ≥ red leaves little texture. The limb
   darkening and the shells' grey keep it reading as a ball. Round 3 measured 9-19 % white.
5. **Magnetar FPS is 10-20 % under round 3** (855-929 avg, against TecTech's 1318). It now draws three CPU-tessellated
   rim shells, both faces, every frame. If that matters, the next step is a small rim shader on GPU meshes instead of
   the Tessellator.
6. **Shell silhouettes are 32-sided.** The 32 × 16 cap shows as slight facets on the 7-block shells in the close views.
   48 × 24 would hide them for about 2× the shell cost.

**Still open:** E-9 (B-66 / B-67): from the front the bands hide the small objects (magnetar, neutron star), as in
`eye_t6_magnetar_front_night`. **Settled 2026-10-13 (your answer: "Glass window bands"):** built in B-86 (from T4
the arms and the rim leave an opening in front of the core's pane).

### B-69 Render gallery: one page with every round-4 shot and its verdict [render QA]

**What was built.** A single HTML page, `render-qa/gallery.html` in my local work folder (6.6 MB). It holds all 96
round-4 shots, grouped by machine (Kerr Ergosphere Engine, Eye of Harmony) and by case. Each case shows the
harness's description, the FPS at every shot, the case's average and range, and the chunk rebuilds counted during its
shots. It also gives the verdict, the defect codes, the measured centre colour and any open structure question. The
references (GT's Black Hole Compressor for the Kerr, TecTech's Eye of Harmony and Forge of the Gods for the Eye) open
each machine. Clicking a shot opens it beside the reference from the same view and time. The page has View and Time
filters and works at 400 px in light and dark themes. Its generator is `render-qa/gallery-build/build_gallery.py`,
which reads `verdicts.json` and the 1024-px JPEG copies in `thumbs/`.

**Judgement calls (built, PROPOSED):**
1. **No round-4 verdict exists yet.** No judge has reviewed round 4. Each case therefore shows the judge's latest
   ruling (the round-3 review) and what round 4 changed in answer (B-68). On that basis:
   - The Kerr cases are **Pass**, because the round-3 review passed them and the Kerr has not changed since.
   - The late dwarf shows **No defect raised**, because round 3 named no defect for it.
   - The nine other Eye cases show **Fixed, awaiting review**.
   - The banner at the top says so. When the round-4 review lands, edit `verdicts.json` and re-run the generator. A
     `fail` status is supported.
2. **FPS per shot comes from the run that produced the final file.** Following B-68, that is the full run for the
   Kerr and the references, the Eye re-run for T1-T4 and T6, and the dwarf retune for T5. This was checked by
   comparing file hashes against `run1/`..`run3/`. The case averages are recomputed from the six shots. They match
   B-68's table to within 1 FPS (rounding).
3. **Downscaled to 1024 × 576 JPEG, quality 80** (4.6 MB of images), so the page stays well under 12 MB while
   keeping close-up detail. The full-size PNGs stay in `round4/`.

### B-70 Automation pass: the Directive Hatch, OpenComputers, plinth-bus pickups and the unattended proof [automation pass, built]

**Context.** Ruling 18: every player action on every system must have a working route through AE2, redstone and
logic gates, OpenComputers and Steve's Factory Manager, with detection hatches including a "degasser-style"
binary-coded one. This pass covers what is on master: the Eye of Harmony (D-49, T1-T6) and the Kerr Ergosphere
Engine with its modules. `docs/automation.md` has the bit tables, the route matrix for every action, the
OpenComputers methods, the minimal rigs and a questbook section to copy.

**What was built.**

- **Directive Hatch, MTE 28127** (`machines/directive`). Once a second its machine pushes a 4-bit code. The hatch
  outputs the code, one bit, or "any bit" (with invert) on every side, and a comparator reads the raw code. Control
  sets turn it into a redstone input for a setpoint. It goes in the Eye's Sensor sockets (`n`) and the KEE's utility
  sockets (`u`), so no structure changed. Recipe SL-A01: an Assembler at UIV. Overlay textures come from
  `gen-sl-textures.js`. The bit tables are in its tooltip.
- **Eye plinth-bus pickups.** The next transition core in a plinth input bus goes into the escrow. A star chart in a
  plain plinth bus goes into the controller slot, and the old target leaves through the ME output bus.
- **OpenComputers**, a soft dependency. It compiles against OpenComputers 1.12.61's API jar, and the driver is
  registered only if the mod is loaded. Adapter components: `bts_eye`, `bts_kerr`, `bts_kerr_module`, `bts_sensor`
  and `bts_directive`. They are thin wrappers over `BTSComputerApi`, which calls the GUI's own setters.
- **The creative builds** now fit Directive Hatches: Eye Trims / Status / Transition in three Sensor sockets, and Kerr
  Hole / Status in two utility sockets. The kits hand some out.
- **The proof.** `DirectiveRigSim` is a plain-JDK offline run. The dev-server bench adds
  `/btseye star … directive | autorig <s> [cut <k>] | ocloop` and
  `/btskerr autorig | ocloop | ctrl on|off|out | ocprobe`.

**Judgement calls (built, PROPOSED):**

1. **One hatch MTE for every host, on 28127** (P4's reserved id, P4's Q-19 recommendation). Its set is picked in its
   GUI, and a hatch adopts its machine's first set when placed. The P4 Quasar adds its sets (Disc, Field, Events,
   Transition, Slots) to `DirectiveSet`; the enum is saved by name. *Alternative:* one MTE per host.
2. **The code is a bitmask, as on the Degasser.** Several bits can be on at once, and each bit names one thing
   wanted now. It is not a single enumerated "most needed" number: an enumeration cannot drive two valves at once and
   needs a decoder for every use. *Alternative:* add a "Priority" set whose value 0-15 names the single most urgent
   action.
3. **Output modes are Code / Bit n / Any (+ invert), not P4's "split: bit n on face n".** A hatch in a socket shows
   one open face, so a per-face split would show a single bit. Instead, one hatch per bit in Bit mode, or one hatch
   in Code mode decoded with comparators or SFM's analog redstone conditions. *Alternative:* add Split for hatches
   with several open faces.
4. **The Eye's sets.**
   - **Trims:** one bit per trim, in the star's trim-table order. A bit is on when the prototype's bang-bang rule
     with threshold w asks for that trim. The default w is 10, the bang-bang threshold the D-49 tests use; P4 suggests
     5 for T7+.
   - **Status:** buffers short, ignition charge wanted, event, alarm.
   - **Transition:** ready, running, next core wanted, Tachyon wanted.
   - The T6 magnetar's fifth trim (the Agent) shares Status bit 3 with "event warned". That bit is a T3 flash or a
     T6 flare, and either way it means open the event valve.
   - **Transition bit 4 changed after the first dev-server run.** It was "dark, waiting". Then the rig poured
     Tachyon all through the Mass Infusion and the Shell Ignition, because bit 2 "running" was its only cue. It is
     now **Tachyon wanted**: the Core Collapse is fast-forwarding, or the dial is set above 1 and could not be paid.
5. **The Kerr's sets.**
   - **Hole** uses the task's four conditions: closed; more than 1 SM below the setpoint; bolts short; more than
     1 SM above the setpoint.
   - **Status:** containment power short, no Seed while closed, unstable, not formed.
   - **"Bolts short"** means the bolt stock lasts less than w minutes at the current bleed (default 10).
6. **Control sets.** Redstone 1-15 into a Directive Hatch sets the Kerr mass setpoint (2 SM to the tier's top), the
   Eye's time dial (1.00-3.00 in 0.25 steps) or the HSM feed (0-1.5 L/s). The mapping is linear. Strength 0 leaves the
   GUI's value, so an unwired control hatch changes nothing. This gives setpoints an AE2 route (a level emitter) and an
   SFM route (an analog emitter), not only OpenComputers. *Alternative:* setpoints by GUI and OpenComputers only, as P4
   has for f₈.
7. **The plinth-bus pickups change the Eye's input behaviour.** They react only to a transition core or a star chart
   put into a plinth input bus.
   - A wrong core stays in the bus.
   - Charts are taken from plain buses only. An ME stocking bus would show its configured chart every second and swap
     back and forth.
   - Only one chart is taken a second. The docs say to put ONE in at a time.
   - Swapping a chart this way follows the same rules as by hand: the next tier's chart while the star burns starts
     the transition; any other chart puts the star out.
   - *Alternative:* chart swaps by hand or OpenComputers only. Climbing tiers unattended would then need a player at
     each step.
8. **The recipe is at UIV, not P4's UXV card.** The Kerr (UIV) needs the hatch from day one. The Stellar Field Sensor
   Hatch stays an Eye-only UMV Condensate Assembler part. *Alternative:* a BEC recipe at UMV, and a cheaper Kerr-only
   variant.
9. **OpenComputers uses an Adapter driver, not SimpleComponent.** Our machines are GT tiles (BaseMetaTileEntity), so a
   block driver is the clean route; P4 assumed this too.
   - Setters exist only where the GUI has one, with the same clamps. Open and Collapse still need the Seed or
     Collapser in a bolt bus.
   - There is no 20-tick rate limit: there is no trim setter to race the field's 1 s lag. P4's limit is for its own
     trim setters.
   - `runServer` now also loads OpenComputers (dev-only, `runtimeOnlyNonPublishable`), so the driver registration is
     tested headless.
10. **Status bit 1 also counts the next transition's fluids** (its event fluids and its new star's first cycle), but
    only once the core is in escrow. An AE2 level maintainer on bit 1 therefore stocks a transition too.
11. **The bench's milestones were completed by command** (`milestones n done`). Milestones are hours of play, not an
    automation step. Everything after that (the core, the chart, the transition and its doses) was done by the rig.

**Results.**

*Offline* (`DirectiveRigSim 4 5`: 4 h x 5 seeds per case, with valves only on Directive bits, a second late):

- **T1-T6:** 0 stars lost on the gate rig and on the prototype's bang-bang rig. The yields are equal; T3 differs
  by 0.003.
- **T1, T2 and T4:** the gate rig asks for exactly the bang-bang trims every second (0 differing seconds).
- **Core Collapse (begun on Transition bit 1, Tachyon on bit 4):** 5 / 5 settled.
- **Kerr:** 5 cases (T1 / T2; SpaceTime, Neutronium and Infinity bolts; setpoints 16-512; from a closed hole). Every
  case had 0 evaporations, and the OpenComputers-style setter loop reached 64 and 512 SM.
- **FieldSim golden:** still 78 / 78.

*Dev server* (headless `runServer`, RC-2 deps plus OpenComputers; Eye T4 structure at (44000, 100, 0), unlocked set
to T1, buffers empty; Kerr T1 at (12800, 100, 10000); boot log "252 registered, 0 skipped" and the OpenComputers line):

| Step | Result |
|---|---|
| T1 from a dark, empty Eye | Status bit 1 stocked the buffers, bit 2 put in a charge, Sol lit; 860 s burning, 829 Resonant |
| T1 → T2 (milestones set) | Transition bit 3 put the Stellar Mass Core in the bus (taken into escrow), bit 1 put Sirius A's chart in (swapped into the slot), Mass Infusion completed in 600 s, no Tachyon poured |
| T2 → T3 | Shell Ignition completed in 604 s; the forced flash rode on Status bit 3 (17 s of the event valve) |
| T3 → T4 | Core Collapse completed in 1,002 s; Tachyon on Transition bit 4 for exactly the 940 s of fast-forward |
| T4, 4 h | 14,400 s burning, 0 lost, 61 % Resonant |
| T4 lost (valves held shut), then left to the rig | Breach; Status bit 2 put in a charge, Arcturus relit and the Core Collapse re-ran with no core: the neutron star was back |
| Kerr from emptied bolt buses and a closed hole, 1 h | The rig put in 1 Seed and 4,176 SpaceTime bolts; open 3,600 s, never closed, 16.08 SM at setpoint 16 |
| Real redstone (`ctrl out`) | The world read 4 at the Hole hatch's face (bolts short), and 11 inverted; the comparator read 4 |
| Control set with a redstone block (`ctrl on/off`) | The hatch read 15; the setpoint went from 16 to 64 SM |
| `ocloop` (the calls behind the OpenComputers components) | setDial 2.1 → 2.0, 9 → 3.0, 0 → 1.0; setFeed 2 → 1.5, −1 → 0; setSetpoint 100000 → 64, 1 → 2; collapse() / open() refused without a Collapser / Seed, accepted with one; getDirective rejects another machine's set |
| `ocprobe` (OpenComputers' own driver registry) | A compound driver for the Eye, the KEE, a Directive Hatch and a Kerr module, with our components bts_eye, bts_kerr, bts_directive and bts_kerr_module and their callbacks; no driver for a plain casing |

**Not covered.** A real computer running Lua (the console has no computer). An AE2 network or a Factory Manager in a
world (the routes go through plain GT hatches, which those mods already drive). The hatch GUI in a client.

### B-71 Full RC-2 dev-server test of master after the overnight pass, and what it fixed [overnight test, built]

**What was run.** Headless `runServer` (RC-2 dependencies plus OpenComputers 1.12.61) on a fresh flat world
(`sltest-ovt`), driven from the console in five boots on the same world (boots 2-5 are the restart checks). Before
that: a static lang lint (every `ofFailure` / `ofSuccess` id, shutdown reason, `StructureErrors.of`,
`translateToLocal`, recipe-map name, item, block and casing meta, and every `GT5U.*` literal, checked against our
`en_US.lang` and RC-2 GT's) and `node tools/fieldsim-golden.js --check --java`.

| Area | Result |
|---|---|
| Load | `252 registered, 0 skipped`; `BEC line: 6 addon condensates makeable ... no legacy fallbacks`; SL-B01/B02 x32; TecTech Eye removed (43 + controller); 10 BHC->Kerr swaps; OpenComputers components registered. No `beyondthestars` frame in any stack trace in five boots (one in boot 4: fix 5 below). |
| Lang lint | 168 lookups, **0 missing**, 0 duplicate keys. One key has no producer any more (`recipe_result.beyondthestars.eye.star_unit5`); left in place (harmless). |
| Golden | 78 / 78 PASS before and after every fix. |
| Kerr | T1 north / T1 east survival / T2 south / T2 west survival: KEE and both modules FORMED, re-projection, damage and repair. Extractor runs GT extractor recipes (128 Stemcells at parallel 448); compressor runs GT compressor and neutronium compressor recipes; DENSITY is refused on T1 and runs on T2 (2 parallels). Closing the hole pauses the compressor with its progress kept; it resumes when the hole opens. Render: claimed at the centre, gone 4 s after a close, off / on switch, gone at once when the controller is broken. |
| Eye | T1-T6 in four facings, creative and survival, all FORMED with the expected bands. Sol: ignition (protostar render), lit, starved (Hydrogen 0: Status bit 1, then Breach with the rig off, render cleared, "Last failure" kept), relit with the failure line still shown. The gate rig took one Eye T1 -> T5 with no hand on it (Mass Infusion 600 s, Shell Ignition 604 s, Core Collapse 1,002 s with Tachyon on Transition bit 4 for exactly 940 s, First Cooling 7,200 s) and, after fix 1, T5 -> T6 (Magnetar Merger 181 s, core consumed). T4: 4 h held; valves cut 900 s -> lost, then relit Arcturus and re-ran the Core Collapse unattended. 1:1 gate: one compression generator swapped to tier 3 on a T4 Eye -> "This target is Eye T4: every Spacetime Compression Field Generator must be tier 4 or higher"; tier 2 under an Arcturus chart -> the T3 message. |
| BEC | `/btseye bec`: 20 Condensate Assembler recipes make our parts, all 20 found by GT's own lookup with their inputs (no shadowing), condensates as RC-2 stores them (recipe metadata), 6 Condensate Generator recipes for our 6 condensates, 0 fallbacks. |
| Directive codes | Eye Status 1 / 2 / 3 / 8, Trims per tier, Transition 1 / 2 / 4 / 8; Kerr Hole 1 / 2 / 4, Status 2 / 5 / 8; control sets (redstone block -> setpoint 64 / 512). |
| OpenComputers | `ocloop` on the Eye and the KEE (clamps and refusals as the GUI's), `ocprobe`: bts_eye, bts_kerr, bts_kerr_module, bts_directive drivers from OpenComputers' own registry. |
| Render lifecycle | Every Eye centre checked after the run: a block only where a star burns (T6 magnetar, T4 while its Core Collapse ran); none at the dark, broken or unformed Eyes; the T3 Eye's star went with its controller. |
| Restart | Unlocked tiers, milestones, live stars and batches, transitions in progress (merger imprint seconds), last failures, render re-claimed, Kerr holes / modules mid-recipe, Directive and sensor hatch settings: as before (after fix 3). |

**Fixed here (each its own commit).**

1. **The Magnetar Merger could never complete.** Since ruling 12 (B-60) the paid Agent upkeep charges the flare shield
   by 2r a second, and the shield absorbs Π every second, so the dynamo's imprint drift was eaten and Π sat at 0.0 for
   the whole 600 s. Every merger timed out and every automatic re-merge spent the charge again: **T5 -> T6 was
   impossible**. B-59's merger test predates B-60, and the prototype skips the merger, so the golden set never saw it.
   - *Built:* a new `Overlay.shieldPerUpkeep` hook (default = `SHIELD_PER_UPKEEP`, so the plain field and the golden
     cases are unchanged). During the imprint the merger returns 0: only Agent doses charge the shield there. The
     release keeps T6's own rate.
   - *Judgement call (PROPOSED):* the imprint's band is +60..+90, but above +70 the field is Critical (Integrity
     −2/s), so a 120 s hold only survives inside +60..+70. The Directive's Agent bit during the imprint is now
     **Π > +66** (was 75; release stays > +30), and the automation valve during a transition on T6 is **1 unit/s
     (100 L/s)** (`EyeTrimRule.MERGER_AGENT_RATE`). A redstone rig opens it on Status bit 3 AND Transition bit 2.
     The standard 5-unit valve takes 20 off Π at once and leaves the band.
   - The Magnetar Seed Core tooltip, the Directive Hatch tooltip and `docs/automation.md` say so (rule 14).
   - *Alternatives:* widen the survivable band (make the imprint band +60..+70, or make the imprint not drain
     Integrity while Π is in its band). Or keep the shield on and give the dynamo a stronger drift. Both change the
     merger's maths, which is yours to set.
2. **Directive "not formed" never reached the redstone.** Every walk clears the hatch lists, and most breaks stop
   the walk before it reaches the sockets. So an unformed Eye or KEE published to nobody, and its hatches fell
   silent after 3 s (Eye Status 8 "alarm" and Kerr Status 8 "not formed" never showed).
   - Both controllers now keep the last formed walk's Directive Hatches and publish to them while unformed.
   - That list is not saved: after a restart, an unformed machine stays silent until it forms once.
3. **Restart persistence.** GT marks a chunk for saving only on inventory changes. The KEE's mass, setpoint and
   open state change without one, so a restart brought back an old save (a 64 SM hole on setpoint 64 came back as
   16 SM on 16).
   - The KEE marks its chunk once a second, as the Eye already does.
   - The Directive and Stellar Field Sensor hatch setters mark theirs.
4. **A wrench lost the failure line.** The live star is tile-only, so picking the controller up put a burning star
   out with no event or failure line. The placed-back Eye showed only "Star: DARK".
   - The item now carries "<star> put out: the controller was picked up", or the last failure as it stood.
5. **Kit-built Kerr controllers had no owner.** Breaking an ownerless GT multiblock throws in GT's power-fail tracker
   (GTNHLib team lookup) and aborts the break. In the test, that left the KEE's hole render standing until a restart.
   Its self-check then removed it.
   - `/btskerr` now gives the KEE and both modules the player who ran it, or the console stand-in.
6. **Test bench** (no gameplay change):
   - `/btseye bec`.
   - `/btseye star … compression <1-9> [all]` and `check`.
   - The Kerr bench now walks a machine GT has not checked yet since its chunk loaded. Right after `keep`, an
     autorig fitted a second pair of Directive Hatches, and a module stock found no energy hatch.

**Seen, not changed.**

- **Merger start counts as a failure.** Putting the Magnetar chart in while Sirius B batches puts the batch out with
  a salvage, and that is recorded as the "Last failure" (B-63's batch rule). That is right for a lost batch. But when
  it is how the merger starts, the line reads like a fault.
  - *Suggestion:* when the new chart is the next tier's and its transition begins, write it as an event, not a
    failure.
- **Creative `fill` T6 build.** It merges by itself and, with no regulation, fails and re-merges every ~10 minutes,
  each time spending the charge. That is by design (auto re-merge), but it is costly in a creative test world.
- **Fresh `/btskerr` and an instant autorig.** An autorig run right after building a fresh `/btskerr` can close the
  hole once (the creative energy hatches start empty and the sim runs seconds instantly). It is a bench artifact.

**Not covered.** A client: GUIs, NEI pages, the hologram preview and the renders' look. A real OpenComputers
computer. AE2 or Factory Manager in a world.

### B-72 Review fixes after the dev-server test: the T5/T6 cores' UXV parts, chart pickups, Directive edges [review fix, built]

A review of B-70/B-71 found seven issues; all seven were real and are fixed (each its own commit).

1. **The T4 -> T5 and T5 -> T6 cores were not craftable in survival (progression).** SL-B70 (Subspace Cooling Core)
   and SL-B80 (Magnetar Seed Core), and their fallbacks SL-E05/E06, took Field Generator UXV x16 / x32, Sensor UXV x8
   and Emitter UXV x16. In RC-2 each of those parts needs 576 L molten Universium and 576 L MHDCSM. With
   `replaceTecTechEyeOfHarmony=true`, Universium only exists at Eye T10, so survival stopped at Eye T4. The B-71
   T5 -> T6 run passed only because the Eye was kit-built. The BEC gate audit (B-64, spec §2.5) called these recipes
   gate-clean; it was wrong, because it checked our condensates and not GT's parts.
   - *Built (PROPOSED, the interim from the review):* the UXV parts become **twice as many UMV parts**. B70: FG UMV
     x32, Sensor UMV x16. B80: FG UMV x64, Emitter UMV x32. Everything else is unchanged (UXV circuits, tier-II/III
     metamaterials, nanite tiers, condensates). RC-2's UMV parts use SpaceTime, Hypogen, Celestial Tungsten,
     Quantium, Mutated Living Solder and DSS, all pre-T4. The change is not behind the config: it is our own recipe.
   - *Not done:* ruling 20 / D-45 itself (overriding RC-2's UXV part research-station recipes so every UXV part drops
     Universium/MHDCSM). That is a larger change to GT's own assembly-line recipes and data sticks, and it needs an
     in-game check. The UXV circuits (coremod Quantum Circuit and GT's Cosmic Mainframe / Transcendent Computer) were
     not re-audited.
   - *Cross-branch:* the homogeny-reality / p4-quasar branches' SL-H10 (Homogeny Tier II Core: FG UXV 16, Pump UXV)
     and later H/R cores take UXV parts too. SL-H10 sits before Eye T6, so it has the same loop. It was not touched
     here (other worktree).
   - *Suggestion:* build D-45 next (Field Generator, Sensor, Emitter, Pump, Motor, Piston, Conveyor, Robot Arm UXV:
     swap molten Universium and MHDCSM for molten White Dwarf Matter / Hyperdense Stellar Mixture, and swap the
     Universium/MHDCSM foils and wires for Infinity / Transcendent Metal, behind `replaceTecTechEyeOfHarmony`). Then
     move B70/B80 back to UXV parts if you want the old price.
   - *Alternatives:* keep UMV parts for good (cheaper than the UXV parts would have been), or make the swap depend on
     the config (UXV parts when the TecTech Eye stays and makes Universium).
   - **Settled 2026-10-13.** D-45's override is not built: your answers were "Brainstorm page first" and, for the
     playtest jar, "Ship UMV stopgap" (this fix, and SL-H10 in B-84). The brainstorm then decided the UXV parts and
     circuits (`designs/docs/uxv-circuits.md`: Q-U1 "Both", Q-U2 "Empyrean", "Omega" above MAX, Q-U9 "The Eye's live
     star"); its Phase U1 retires this stopgap.
2. **Duplicate charts in a plinth bus put a later star out (bug).** An AE2 export bus with a redstone card on
   "active with signal" pushes 2-4 charts while Transition bit 1 stays on until the next pickup. A conveyor cover
   moves a whole stack. The extras equalled the target, so they stayed in the bus. When the rig later loaded the next
   tier's chart, the stale one was swapped back in a second later. That aborted the Shell Ignition: the star and the
   event fluids were lost.
   - *Built:* the Eye sends a copy of the loaded chart (the whole stack) to the ME output bus, and swaps no chart in
     while a transition runs or is PENDING.
   - The Eye's tooltip and `docs/automation.md` recommend the redstone card's **Signal pulse** mode for the chart and
     core export buses. A spare core stays in the bus until the next transition wants it.
3. **"Last failure" after a wrench was lost again on a restart (minor).** 871bf2d put the line in item NBT. Once the
   controller was placed, the tile saved it only inside the star compound, and a wrenched Eye has none until it
   lights.
   - *Built:* with no field, the tile saves the event and failure lines as `btsEyeDarkEvent` / `btsEyeDarkFail`.
   - A live star put out by a pick-up is now logged at INFO, like every other failure.
4. **Partly broken walks left some Directive Hatches silent (minor).** The "not formed" fallback applied only when
   the broken walk reached no hatch at all. A walk that stopped between two hatches published only to the one it
   reached. The Eye and the KEE now publish to the last formed walk's hatches plus any the broken walk reached.
5. **A comparator on a Directive Hatch went stale in Bit n / Any mode (minor).** GT tells the neighbours only when a
   redstone output changes. The hatch now issues a block update whenever its raw code changes.
6. **An OpenComputers NaN threshold stuck the sensor at 0 (minor).** `setThreshold` keeps the old value on NaN.
   `bts_sensor.setThreshold` returns `nil, "threshold is not a number"`.
7. **The Directive Hatch GUI's Set button could not cycle backwards (minor).** A right-click from the host's first
   set landed on the same set. The server setter now walks in the direction the button moved and wraps to the host's
   last set.

**Checks.** `spotlessApply compileJava` passed after every fix. `node tools/fieldsim-golden.js --check --java`: 78 / 78
PASS. Headless `runServer` on `sltest-ovt`, two boots:

- *Load (fix 1):* `252 registered, 0 skipped`; "BEC line: ... no legacy fallbacks"; `/btseye bec`: 20 Condensate Assembler
  recipes, all 20 found by GT's own lookup with their inputs (so the UMV-part B70/B80 shadow nothing), 0 fallbacks.
- *Fix 3:* T5 Eye wrenched mid-batch (`wrench`, then `target none` so no field is made). It logs
  "[BTS] Eye Sirius B put out: the controller was picked up". After a restart the status still shows that line as
  the last event and the last failure.
- *Fix 2:* `bus siriusb 3` on the lit Sirius B -> "3 duplicate Star Chart: Sirius B sent ... to the ME output bus".
  On a T2 Eye, Sol lit with the core in escrow, `bus siriusa 1` started the Mass Infusion from the bus. Then
  `bus sol 1` stayed in the bus for 3 s with the Infusion still running (before this fix it would have been swapped
  in and aborted it), and `bus siriusa 2` was sent out as duplicates.
- *Fix 6:* `sensors theta gt 50`, then `sensors theta gt NaN`: all five sensors still read "> 50.0".
- *Fix 4 regression:* the T2 Eye with the block behind its controller removed -> unformed, Status 8 (Alarm).
- New bench sub-command `/btseye star <x> <y> <z> bus <chart> [n]` (n charts into a free plinth bus slot).
- *Not run:* fix 5 (a comparator next to a Bit-mode hatch) and fix 7 (the GUI button) need a client or bench plumbing;
  a partly broken walk (fix 4's exact case) was not staged. No `beyondthestars` frame in any stack trace.

### B-73 Package: the interim test jar `0.24.1-master.196+fcea018132` [interim package]

**Context.** The overnight queue asked for an interim jar with the D-49 fixes, the BEC expansion, the automation pass
(Directive Hatch, OpenComputers) and the render QA results, plus an updated `dist-rc2/INSTALL.md` (design repo).

**What was built.** `gradlew build --no-daemon` on master `fcea018` (clean tree; spotless + checkstyle pass, nothing to
fix). The jar is `dist-rc2/beyondthestars-0.24.1-master.196+fcea018132.jar` (1,077,713 bytes), SHA-256
`6cb840e4e27a40bfff41fe05b61e5323e0e2cbc25962ae8950d54e579ff766d0`; the previous test jar `master.137` moved to
`dist-rc2/superseded/`. INSTALL.md gained a "what's new" table (section 0), the D-49 fixes (3.2.1), the render QA
results (3.2.2), the BEC expansion (3.3), automation (3.5), the new bench commands, a `master.137` migration line, and
the dedicated-server command with the new jar name. The judgement calls:

1. **Unmerged branches stay out.** `tidy-old-multis`, `questbook`, `homogeny-reality` and `p4-quasar` are not
   ancestors of master; the package pass does not merge. INSTALL.md says so at the top and in 3.6, and notes that
   the quest book therefore still has the old chapter (the new mechanics are learnable from tooltips only until the
   questbook branch lands, rule 14).
2. **The jar still carries the dev-only render-QA harness classes** (`rendertest/*`, inert unless the JVM runs with
   `-Dbts.rendertest=true`) and the OpenComputers driver classes (registered only when OpenComputers is loaded; no
   OpenComputers classes are shipped). Left as built. *Alternative:* exclude `rendertest/**` from the release jar.
3. **Render QA is reported as "fixed, awaiting review"** for the Eye cases (B-69: no verdict on round 4 yet), with
   the open structure question (E-9 / K2: windows in the crown band and the Kerr drum) listed as open.
4. **`SERVER_DIR` stays a placeholder** in the dedicated-server command (still not recorded anywhere).
   **Settled 2026-10-13 (your answer: "SSH key will be tonight, run locally for now"):** the playtest jar is for the
   local Prism instance; the dedicated server waits for the key to be authorised.

**Suggestion.** Accept as is; package again once the tidy, questbook and Homogeny/Reality branches are merged.

---

### B-74 H/R: the three machines' shapes as exported, and what they cost in blocks [H/R unit 1, built]

**Context.** Ruling 16 and the design doc `designs/docs/homogeny-reality.md` (PROPOSED, H-1..H-28). The voxel mockups
(design repo `3e23c09`) are exported unchanged by `tools/export-structures.js` into `homog_t1..t3`,
`reality_diffraction` and `reality_refab` (build spec "Contract additions (Homogeny / Reality structures + content)").

**What was built, and the judgement calls.**

1. **Sizes are the mockup's, not the doc's.** The doc's §1-§3 numbers were written before the mockup was drawn. As
   exported (W x H x D, each in its controller's own frame): Homogeny T1 69 x 70 x 73, T2 103 x 70 x 113,
   T3 145 x 70 x 130; Reality Diffraction 103 x 101 x 85; Reality Refab 104 x 139 x 83.
2. **Block counts are well above the doc's §1.5 / §2.5 / §3.5 estimates.** Homogeny T1 17,783 (doc about 15,100),
   T2 +10,813 (about 7,300), T3 +13,002 (about 10,000); Diffraction 53,005 (about 40,000); Refab 59,245 (about
   35,600: the hull glass alone is 14,260 Quantum Glass and the hull plating 20,649). The per-block recipes and the
   cost table must be priced from `manifest.json` (ruling 6), not the doc. **Suggestion:** keep the shapes (they
   are the sketches) and price per block; if the Refab is too dear, thin its hull to one glass ring between ribs or
   scale the mockup to 0.85 (about -35 % blocks). Alternative: halve the bulk casings' condensate.
3. **The Homogeny is exported turned a quarter turn.** Its controller sits on the volute throat's outer face, which
   faces +x in the mockup. Like the Kerr modules, every Homogeny piece is turned so that face is +z, the piece
   frame's front (`turns: 1` in the manifest). In game it looks exactly like the mockup, seen from the throat.
4. **Tiers only add blocks.** T2 writes over 30 T1 cells (its inlets meet the volute) and T3 over 45 T2 cells (the
   clamps where the outer ring crosses the middle ring). As on the Eye, the earlier piece keeps them, so `homog_t1`
   never changes when a tier is added.
5. **Hatches are pinned by kind and count, not coordinate.** The mockup computes them from its band curves, so a
   coordinate table would only restate voxel.js. A changed count or a new hatch kind aborts the export.
6. **Region identity is the socket's char.** The six Region Anchor sockets export as `r s t u v w` = Regions I..VI
   (Hyperspace, Subspace, Forward Time, Reverse Time, Positive Mass, Negative Mass). The blades sit on the deck bands
   at y -14 / 0 / +14 (the doc's "drums 1-3"), region = 2 x band + (180 degree side).
7. **Sockets as drawn:** Homogeny 3 feed hatches at T1 (6 at T2), 2 output hatches on the throat, 2 energy on the
   bottom cap; Diffraction 1 BEC hatch, 1 medium input hatch, 2 energy, 6 anchors on the base ring; Refab 1 BEC
   hatch, 2 input buses, 1 output bus on the cap, 1 energy. The doc's "any Electromagnetically-Isolated Casing" for
   the BEC hatch is the machine unit's call (a hatch element on the `Q` casing); the shapes carry one pinned cell.

**Alternatives.** Re-draw the mockups to the doc's sizes; or export the Homogeny unturned with the controller
facing sideways (worse: the hologram would be built from the side).

### B-75 H/R: the new blocks, items and fluids [H/R unit 1, built]

1. **`slCasing3` is a new block (H-22).** Metas 0-11, 12-15 spare, so P4's casings (expected in `slCasing2:3+`) never
   collide. Meta 11, **Region Anchor Casing**, is the mockup's red lens beside each anchor socket (recipe card
   SL-R17); the doc's §4.2 table stopped at 10.
2. **Region Crystal glows** (light 9, the sketches' orange diamonds). About 1,300 per Reality machine. **Suggestion:**
   keep it; drop to 0 if a hologram build of a Diffraction lags on light updates.
3. **Block tooltips.** `ItemBlockSL` now shows a `tile.beyondthestars.<registry>.<meta>.tooltip` lang line when one
   exists (ruling 14): every slCasing3 block says which machine uses it and "Made in the Condensate Assembler".
   Older SL casings have no tooltip key yet, so nothing changes for them.
4. **Superdense, not Supermassive (ruling 17).** The fluid is `btssuperdenseergomatter` (P4's name) and its condensate
   `btsentangledsuperdense`, "Entangled Superdense Ergomatter". The design doc and SL-H20/H21/R01/R11/R16 still say
   Supermassive; the recipes must use the Superdense fluid.
5. **Later phases' fluids are registered only if absent (H-24).** `btsergomatter`, `btssuperdenseergomatter` and
   `btswhiteholematter` are registered by `SLRealityFluids` under exactly P4/P5's names when nothing registered them
   first, logged at boot as placeholders. **When P4 or P5 merges, one file must own each fluid**: delete the
   `ifAbsent` line here, or have P4 call it, so the name is never built twice.
6. **SL-B16 (Entangled Magnetar Matter) is registered, but ruling 19a may leave it without a consumer.** The design
   doc and recipe card SL-H10 gate the Homogeny Tier II Core on Magnetar Matter (so it registers SL-B16), while ruling
   19a says that core must use Black Dwarf Matter instead. The fluid is registered so either choice works. If the
   recipe unit follows 19a, the generator recipe should not be added (an orphan condensate in NEI suggests a use that
   does not exist, BEC Q-B6), and the fluid stays as a harmless reserved name.
7. **Entanglement Medium is a 0 K liquid (doc §4.4).** GT pipes treat fluids under 120 K as cryogenic, so it needs
   cryo-proof pipes from the Homogeny to the Diffraction. **Suggestion:** keep it (thematic) and say so in the
   Homogeny T3 quest and the Diffraction tooltip; alternative: 300 K. The region and entangled fluids are GAS at 0 K
   like GT's condensates; they live in the BEC network.
8. **Controller overlays are ready** for the machine unit: `beyondthestars:iconsets/OVERLAY_FRONT_HOMOGENY`,
   `_REALITY_DIFFRACTION`, `_REALITY_REFAB` (+ `_GLOW`, `_ACTIVE`, `_ACTIVE_GLOW`).
9. **Lang:** the seven recipe-result ids of the doc (`beyondthestars.homogeny.tier`, `.installing`,
   `beyondthestars.reality.nonetwork`, `.anchors`, `.full`, `.noassembler`, `.nanites`) have
   `GT5U.gui.text.recipe_result.` keys, and the three recipe maps' names are keyed as `beyondthestars.recipe.homogeny`,
   `.reality_diffraction`, `.reality_refab`. Any other id the machine unit adds needs its own key (ruling 10).

### B-76 H/R: the Homogeny controller [H/R unit 2, built]

**Context.** Ruling 16, the design doc `designs/docs/homogeny-reality.md` §1 (PROPOSED), D-46/D-47/D-49 and brief
rulings 4-5 (one install path, milestones + install run). Built in `machines/homogeny/MTEHomogeny` (MTE 28130) with
its MUI2 screen `HomogenyGui`.

**What was built, and the judgement calls.**

1. **Tier = min(installed, structure).** The structure tier is cumulative (`homog_t1`, + `homog_t2`, + `homog_t3`; the
   outer ring counts only with the stirring ring). The installed tier is permanent. A broken ring drops the machine to
   the tier below until it is rebuilt; nothing is lost.
2. **The install path** (doc §1.3, rulings 4-5): next ring built + milestone (100,000 L of the current tier's product
   mixed over the machine's life: ESPM for T1 to T2, Degenerate for T2 to T3) + the next core in escrow (right-click
   it onto the controller) + **Install** (a GUI button, or a screwdriver: GT never passes a sneaking right-click to a
   machine, B-50). The run is 12,000 ticks offline, paying RECIPE_UXV (T2) / RECIPE_MAX (T3) EU/t from the energy
   hatches every tick; a short tick pauses it, never loses progress. Sneak-punching the controller with an empty hand
   returns the core and **cancels** an install in progress (its progress is lost), as the doc says. Escrow, the
   milestones and an install in progress live in item NBT, so they survive a wrench. **Suggestion:** keep.
   Alternative: start the run by itself once everything holds (one fewer click, fully automatic).
3. **Region Crystal at the T1 throat tip is a gate violation.** Unit 1's legend puts 5 Region Crystals (`E`) at the
   throat tip of `homog_t1` (UMV), but Region Crystal needs Ergomatter (stage S8, Eye T7). The controller accepts the
   Godforge lens glass (Spatially Transcendent Gravitational Lens Block, already 610 cells of T1) in those cells as
   well; hints and creative builds place the crystal, survival auto-place falls back to the lens. The T3 tail tip is
   the same char, so it accepts the lens too. **Suggestion:** re-draw the T1 throat tip as lens glass in voxel.js (a
   new char) and keep `E` for the tail only. Alternative: leave as built.
4. **Feed sockets use GT's Input Hatch element**, not a private one: the Homogeny is one machine with no plant-wide
   lists, and GT's processing logic reads `mInputHatches` (ME input hatches work). Trap 3 is still kept: a ring whose
   walk fails drops the feed hatches it registered.
5. **Running:** GT's processing logic on `beyondthestars.recipe.homogeny`; 1 / 4 / 16 parallels; **no overclock**
   (EU/t = recipe EU/t x parallels, so a full T3 is one MAX amp); void protection, batch mode and recipe lock as on
   any GT multi; no maintenance. A recipe's special value is its minimum Homogeny tier; a too-low tier fails with
   `beyondthestars.homogeny.tier`, an install in progress with `beyondthestars.homogeny.installing`.
6. **Milestones count at completion.** A run's product is counted when its output lands; a run stopped part-way
   counts nothing.
7. **Not built: the vortex render** in the column (the doc's empty render centre with a GUI toggle). The column is
   glass with nothing inside. **Suggestion:** a render pass reusing the Eye's centre-render pattern
   (`EyeCentreRender`, one render block at the manifest's `centre.rel`).
8. **No questbook quests yet** (rules 14/15). The tooltip carries the mechanics; the doc's §6.2 quests are for the
   questbook unit.

**Alternatives.** Core only, instant (it is a side machine); or an install run without the milestone.

### B-77 H/R: Reality Diffraction and Reality Refab [H/R unit 2, built]

**Context.** Ruling 16, the design doc §2-§3 (PROPOSED, H-12..H-16, H-27, H-28). Built in `machines/reality/`
(`MTERealityBase`, `MTERealityDiffraction` 28131, `MTERealityRefab` 28132, their GUIs, `RealityRecipeMaps`,
`RealityProcessRecipes`).

**What was built, and the judgement calls.**

1. **The BEC glue on a GT multiblock (H-14 primary path, no fallback needed).** RC-2's BEC machines extend
   `MTEBECMultiblockBase` (a TecTech multiblock with a text-structure wrapper that cannot hold our exported shapes);
   ours keep GT's `MTEExtendedPowerMultiBlockBase` and implement `BECFactoryElement` + `NotableBECFactoryElement`
   exactly as that base does (neighbours = our formed BEC Hatches; routed neighbours by a walk from them). **One
   wrinkle:** GT's `MTEHatchBEC` only lists TecTech BEC controllers as its own neighbours, so when the hatch
   re-registers itself (its first tick, a facing or colour change) the grid drops the edge to us. The controller
   re-asserts it every second (a neighbour-set compare when nothing changed), so the worst case is one second
   disconnected. **Suggestion:** keep; upstream, `MTEHatchBEC.addController` could take any `BECFactoryElement`.
2. **Diffraction never voids.** Each run's regions are injected at the end; whatever the network does not take (no
   running Containment Field: RC-2's storage accepts nothing while off) waits in the controller, is retried every
   second, and the next run waits (`beyondthestars.reality.full`) until it is gone. The doc's "won't start without
   free capacity" became "won't start without a network that has a storage" (`beyondthestars.reality.nonetwork`);
   RC-2 storages have no hard capacity.
3. **SL-R03 is read from the map.** The medium, the six anchors (input slots 1..6 = Regions I..VI), the outputs, the
   time and the EU/t all come from the one recipe in `beyondthestars.recipe.reality_diffraction`, so tweaking the
   recipe is the whole change. The region identity is the socket (`r..w`), never the fluid; any input hatch works,
   ME stocking hatches included. An unanchored region is not paid and yields nothing (its share of the medium is
   lost); no region anchored, no run.
4. **Refab links the nearest running Condensate Assembler within 64 blocks by itself.** The doc's Data Stick link
   cannot be done from our side (a stick right-clicked on GT's Assembler never reaches our code), and RC-2 links its
   nodes with a line-of-sight beam, which would need a LoS hatch socket in our structure. The link is kept and
   re-found if the Assembler goes. **Suggestion:** keep (zero setup). Alternative: a LoS-hatch socket on the Refab's
   foot, so it links exactly like a Teleportation Node.
5. **The reassembly lock replaces GT's end-of-craft failure.** The six regions are drawn from the Refab's OWN BEC
   Hatch's network in lockstep with the progress; the craft never passes the fraction the scarcest region can pay
   for, so a short region pauses it (GUI: "waiting for regions") and nothing is wasted. GT's node instead drains
   what it can and fails the craft at the end. GT's other node rules are kept: one step per item slot, a step needs
   nanites of at least its tier, each tier above slows it 2x, progress = nanites / tier penalty / (contaminants + 1),
   more than three foreign condensates clog it (here it waits instead of stopping).
6. **Refab refuses to start a recipe the Assembler's nanites cannot finish** (the recipe's highest step tier above
   the Assembler's tier: `beyondthestars.reality.nanites`), so inputs never sit in a craft that would stall at a
   step. GT's node starts and stalls.
7. **Refab pays its own EU, and only on ticks that advance** (GT's node defers EU to its Assembler).
8. **SL-R04 runs at 1 x RECIPE_MAX, not the doc's 4 x.** A GT recipe's EU/t is an int and 4 x RECIPE_MAX does not
   fit. **Suggestion:** keep; or make it 4x longer. The Refab map takes six condensates (GT's takes four), so its NEI
   page is taller.
9. **The process recipes are owned by this unit** (`RealityProcessRecipes`, guard line `reality-process`):
   SL-H06/H11/H21, SL-R03, SL-R04, SL-R05. The recipe unit must not add them again; it owns the Condensate
   Assembler / Generator recipes of the blocks, cores and controllers.
10. **Automation (ruling 18) as built:** GT's standard covers on the controllers (machine controller, activity
    detector); anchors and the medium take ME input hatches (a level emitter keeps each anchor stocked); RC-2's
    Condensate Detector hatch on the BEC network reads region stock. **Not built:** a dedicated detection hatch or
    OpenComputers component for these machines. **Suggestion:** a "Region Anchor Sensor" (one redstone bit per
    missing anchor) on the Diffraction's base ring, modelled on the Stellar Field Sensor Hatch.
11. **Not built: the region-cloud and reassembly-stream renders** (doc §2.1 / §3.1, render-only). The drums and the
    spindle are glass with nothing inside.
12. **Not tested in game.** `spotlessApply compileJava` passes. The console bench is ready:
    `/btshr diffraction <x> <y> <z> north fill`, then `/btshr bench <x> <y> <z> status`.

**Alternatives.** The doc's fallback (six output hatches feeding six input hatches, no BEC) if the grid wrinkle in
item 1 proves annoying in play.

### B-78 H/R: every Homogeny / Reality recipe goes through the Condensate Assembler [H/R unit 3, built]

**Context.** Ruling 16 ("make sure all of these recipes integrate with Bose Einstein condensate assembler and similar
for our requirements"), ruling 6 (every BOM block craftable at a fair cost), rulings 17 and 19a, and the design doc
`designs/docs/homogeny-reality.md` §5 (PROPOSED). Built in `BTSRecipesReality` (guard line `reality`); build spec
"Contract additions (Homogeny / Reality recipes)"; costs §12. Numbered B-65 on the branch (master's BEC expansion took B-64); B-78 since the merge (see B-81).

**What was built, and the judgement calls.**

1. **All 17 block / core / controller recipes are Condensate Assembler recipes**, one nanite tier per stack, none above
   Black Dwarf (H-17), with the condensates of the doc's families. Controllers too (H-19: ruling 16 overrides the BEC
   proposal's R4 for these three machines). Four Condensate Generator recipes make our condensates (SL-C01..C04).
   **Suggestion:** keep. Alternative: assembly lines for the three controllers, like the Eye and the Kerr.
2. **SL-H10 (Homogeny Tier II Core) is gated on Black Dwarf Matter, not Magnetar Matter** (ruling 19a): 256 ingots of
   BDM condensate (the Magnetar Seed Core's amount) + 256 ingots of WDM + 1,024 of SpaceTime. The Homogeny T2 is
   therefore buildable right after Eye T5. As a result **SL-B16 (Entangled Magnetar Matter) has no consumer**, so it
   gets no generator recipe (BEC Q-B6: an orphan condensate in NEI suggests a use that does not exist); its fluid stays
   a reserved name. **Suggestion:** keep; give SL-B16 its generator when P4 adds a Magnetar consumer.
3. **This branch has no `SLCondensates`.** Master's BEC expansion (SL-B10..B15) landed after the branch forked. The
   Homogeny T1 needs Entangled Raw Star Matter and the T2 core needs Entangled White / Black Dwarf Matter, so
   `BTSRecipesReality.shim` looks each up by its frozen registry name, registers the same fluid (name, colour, GAS at
   0 K) at postInit only if nothing did, and adds a generator recipe only if none exists on that source fluid. Merged,
   master's run first and the shim only reads them. **Suggestion:** keep (order-proof, nothing to delete on merge).
   Alternative: delete the shim after the merge. **Superseded (B-87):** the shim is gone; `SLCondensates` and
   `BTSRecipesBEC` own these condensates and the Reality unit only resolves them, which is order-proof too.
4. **Recipes needing fluids nobody makes yet are registered, visible in NEI, and logged.** Ergomatter and Superdense
   Ergomatter (Eye T7/T8, P4) and White Hole Matter (Eye T9, P5) are still `SLRealityFluids` placeholders (H-24). Every
   recipe that needs one, directly or through a Region Crystal, prints `[BTS] H/R gate: <recipe> is registered but
   unreachable until an Eye makes <fluid>` at boot: SL-C02..C04, SL-R11, R13, R16, R17, SL-H20, SL-R01, R02, and unit
   2's process recipes SL-H21, SL-R03, R04, R05. So until P4 the Reality machines and the Homogeny T3 cannot be built,
   which is the ladder's design. **Suggestion:** keep. Alternative: skip those recipes until P4/P5 ship.
5. **Priced against the exported BOM, not the doc's estimates (B-74).** Seven per-craft inputs were trimmed because the
   real block counts made them balloon: H02 pistons 8 → 2 (824 UMV pistons → 206), H03 field coils 8 → 4, SC UMV
   wire 16 → 8 and motors 4 → 2, R11 emitters 4 → 2, R12 dense Infinity 16 → 8 and SpaceTime 64 → 32 ingots, R13 FG
   UXV 2 → 1 (544 → 272), R14 dense Infinity 32 → 16 (93k → 47k Infinity ingots in the Refab hull) and SpaceTime 64
   → 32 ingots, R15 Casimir 8 → 4. **SL-R16 outputs 32 claws, not 8**: the Refab has 688 claws, not the doc's 190, so
   ×32 keeps the bill near the doc's (23 crafts, 216 Robot Arm UXV with the controller). **Suggestion:** keep.
6. **Built (B-80 item 5): the caps are Reinforced Temporal Structure Casing now.** *Original:* **The Homogeny T1's 2,430 Godforge Boundless casings are its heaviest item** (column caps; 1,215 Godforge BEC
   crafts, 29k Mellion frames, 3,645 FG UEV, 7,290 Artificial Gravity Generators), more than all its ladder blocks
   together, at UMV. **Suggestion:** re-draw the caps in Reinforced Temporal Structure Casing (×32 since ruling 11a: 76
   crafts) or in Stirring Band Casing (a structure change for the mockup and unit 1's legend). Alternative: keep (the
   Homogeny becomes a post-Godforge build).
7. **The Reality machines' Godforge lens glass is their outlier.** The Diffraction spine (1,916) and the Refab spindle
   (2,837) plus the Region Crystal inputs are about 2.5k Godforge glass crafts, about 15k Stable Boson Containment
   Units. **Suggestion:** swap both to Quantum Glass (they are windows for renders that are not built yet, B-77 item
   11). Alternative: keep the Godforge look. Also heavy but in line with MAX: 752 Condensate Guidance Coils (one
   Compact Fusion Coil T3 each) for the Refab collars, and 9.4k Electromagnetically-Isolated Casings across both bases.
   **Settled 2026-10-13 (your answer: keep, "This will be a MAX tier / Endgame item, its allowed and meant to be
   expensive").** Unchanged.
8. **SL-R17 Region Anchor Casing ×4 is new** (12 per Diffraction; unit 1 added the block after the doc's table): Region
   Crystal 2, EM-Isolated Casing 2, Harmonic Lining 4, Sensor UXV 1; Degenerate 500 L, SpaceTime 8 ingots.
9. **Small substitutions.** Dense plates fall back to the block, then to 9 plates; the huge Neutronium pipe of SL-H05
   falls back to the large one. "Supermassive" in the doc is Superdense everywhere (ruling 17). Nanite tier 2 is
   Silver (GT's own `nanites()` index, as master's BEC recipes use), not Neutronium (both are tier 2).
10. **NEI.** Our recipes appear on GT's Condensate Assembler and Condensate Generator pages with their nanite tiers;
    the three machine tabs now show their controller. The Homogeny Tier II / III Core tooltips name the condensates
    and the Eye tier after which they can be made (rule 14).
11. **Not tested in game.** `spotlessApply compileJava` passes. On a dev server: the boot summary should show 21 new
    recipes (4 generators, 17 assembler; plus 3 generators on a build without master's BEC expansion) and 0 skipped,
    the 14 `H/R gate:` lines, and NEI usage of Degenerate Stellar Mixture should show SL-C01.

**Alternatives.** Assembly lines for the controllers (H-19); keep the doc's ×8 claws and untrimmed inputs; skip the
P4/P5-gated recipes until their fluids have an owner.

### B-79 H/R: the dev-server test of the three machines, the Install queue and a persistence fix [H/R test]

**What was run.** `runServer` in the `addon-hr` worktree (RC-2 deps, fresh flat world `hrtest`, stdin console, every
footprint force-loaded with `/btshr keep`), six boots. Bench additions for it: `/btshr bench <x y z> scan` (progress,
the last recipe check's id and text, hatch contents, stored EU), `on` / `off`, `project <n>` (the projector's creative
build of rings 1..n over a standing Homogeny, controller and hatches kept) and `/btshr recipes`.

- **Load.** `273 registered, 0 skipped`; the three SL-B10/B13/B14 shim lines, the SL-B16 line and the 14 `H/R gate:`
  lines as B-78 predicted; no `[beyondthestars]` WARN/ERROR and no exception with a `com.nateyburr` frame in any boot.
  GT logs `EUt > Practical Voltage` for SL-C04 (RECIPE_MAX) and clamps it to 2,013,265,912, as it does for its own MAX
  recipes; same clamp on SL-R03/R04/R05.
- **Lang lint.** All 7 H/R recipe-result ids (`homogeny.tier/installing`, `reality.anchors/full/nanites/noassembler/
  nonetwork`) have `GT5U.gui.text.recipe_result.` keys; every H/R structure-error and recipe-map key is in the lang
  file. On the server the scanner printed readable text for `installing`, `nonetwork` and `noassembler`.
- **Recipes.** Homogeny map 3 (SL-H06 100,000 L RSM -> 1,000 L ESPM at UMV 240 s, H11, H21), Diffraction 1 (SL-R03),
  Refab 2 (SL-R04/R05); Condensate Assembler 17 of ours (every block, core and controller), Condensate Generator 7.
- **Forming.** Homogeny T1 north (creative) / east (survival), T2 south / west, T3 north / east; Diffraction north /
  west / east (survival); Refab south / east (survival) / west: all FORMED, re-projection over each keeps it formed,
  damage-and-repair restores the cell, the tile count is unchanged. Survival auto-place: T1 17,782, T2 28,595, T3
  41,597, Diffraction 53,004, Refab 59,244 blocks.
- **Homogeny T1 run.** With console-stocked hatches the T1 took 100,000 L of Raw Star Matter and, 4,800 ticks later,
  put 1,000 L of ESPM in the output hatch; the milestone read 1,000 L. T2 runs 4 parallels (400,000 L in, 4,000 L
  out), T3 16 (1.6M L in, 16,000 L out). A bench whose two 64 A UXV hatches run dry shuts the mix down as any GT multi
  does (power loss, inputs lost): the bench hatches hold 275 G EU, a T1 mix costs 604 G.
- **Upgrades.** T1 -> T2 -> T3 in place on one controller: Install refused with the reason for each missing piece
  (ring, milestone, core), the ring added by `project`, tier = min(installed, structure) until the install run ends,
  the run pauses on power and on a disabled controller and keeps its progress, survives a wrench, then T2 (4
  parallels) and T3 (16 parallels).
- **Diffraction / Refab.** Recipes registered; with no BEC network they idle with "No BEC network: needs a BEC Hatch on
  the base and a Containment Field on its network" / "No running Condensate Assembler within 64 blocks". Inputs are
  P4/P5-gated, so no run was attempted.
- **Restart.** Every machine re-forms; installed tiers, milestones, the escrowed core, a mix in progress and (after
  fix 2) an install run at 48.8% come back as they were.

**Fixed here.**
1. **Install while a mix runs now queues.** `installBlocker` refused Install while a mix ran. A Homogeny fed by ME
   stocking hatches has no gap between mixes, so the player had to soft-mallet it off, and the install then paused as
   "controller disabled" until it was switched back on. Install now starts at once and waits for the running mix ("paused:
   waiting for the running mix to finish"); no new mix starts while installing. The tooltip says so. Verified: the mix
   finished, its ESPM counted, then the run advanced.
2. **Restart persistence.** Nothing marked the controller's chunk for saving while the install advanced (GT marks
   it only when a recipe starts or ends), so a restart reloaded the install as it was when Install was pressed (48.8%
   -> 0.0%, and the controller disabled as it had been then). The install now marks the chunk every 20 ticks and a
   finished mix marks it after its milestone. Verified across a restart. The same gap was closed without an in-game
   test (no BEC network on the bench): the Refab marks the chunk every 20 ticks of lockstep progress (its drawn
   regions would otherwise be drawn twice after a restart) and the Diffraction marks it whenever its never-void
   buffer injects into the network (a stale buffer would otherwise be injected twice).

**Not tested (open).** The `homogeny.tier` refusal (a T1 machine always has Raw Star Matter, so H06 matches first);
Diffraction and Refab runs, their anchor / nanite / full refusals and the BEC hatch edge re-assert (they need a BEC
network with a running Containment Field and Condensate Assembler and P4/P5 fluids); GUIs, tooltips, NEI and the
hologram projector item on a client. *Suggestion:* a client session once P4 makes Ergomatter, using `/btshr bench`
on a BEC network built by hand.

**Small things seen (no change).** `gtnhlib: Unable to find team for player 249b60b8...` once, when the bench's console
owner UUID owned a T3 (the bench owner is not a real player). BEC network ids change when a machine re-forms (expected,
the grid rebuilds the network).

### B-80 H/R: review fixes - recipe-check pushes, the Diffraction buffer on a wrench, the Eternity loop, D-49 teaching, the Homogeny caps, the Refab hull, reserved Quasar ports, the questbook merge [H/R review fix]

**Context.** A review of the `homogeny-reality` branch against RC-2, D-49, the P4 design of record
(`designs/docs/quasar-p4.md`, 7f91607) and ruling 6. Each fix is its own `wip: H/R review fix:` commit;
`spotlessApply compileJava` passes after each, and `node tools/export-structures.js --check` is clean.

**Dev-server re-test (`runServer`, world `hrtest`, two boots).** `273 registered, 0 skipped`; the 14 `H/R gate:`
lines, now with the Eternity reason on SL-H20, R02, R03, R04, R05; no exception and no `beyondthestars` WARN/ERROR
beyond FML's usual chunk-loading note.
- *Forming with the new shapes:* Homogeny T1 east (survival path, Temporal caps), T2 north and T3 south (creative),
  Reality Diffraction north (survival, the 6 reserved cells), Reality Refab west (survival) and north (creative, the
  new lower hull): all FORMED, re-projection keeps them formed, damage-and-repair restores the cell, tile count
  unchanged.
- *Install with no hatch event at the end (fix 1):* a T1 built at 11000 100 0, its stirring ring added by `project
  2`, milestone, escrow, Install, then `fill` + `itick 130` until the run ended. With only read-only `scan`s after
  the last tick, the Homogeny started a T2 mix on its own (`Progress 141 / 4800`, last check "Processing recipe",
  4,000 L ESPM running = 4 parallels). The B-79 bench's `fill` / `on` had masked this path.
- *Diffraction buffer (fix 2):* `pending 1000`, then `wrench` (item NBT written and read back) kept `[1000 x 6]`. With
  no hatch event, the next `scan` showed the last check as `reality.full` ("The last run's regions are still
  waiting..."), so the once-a-second idle push runs.
- *Not tested:* a real Diffraction / Refab run (P4/P5 fluids and a BEC network), the anchor-hatch watcher on a refill,
  GUIs and tooltips on a client.

**What was built, and the judgement calls.**

1. **RC-2 recipe checks are event-driven; three idle traps closed.** `MTEMultiBlockBase.shouldCheckRecipeThisTick`
   only runs a check when something pushes one (hatch events, structure, on/off), and `doPeriodicChecks` is off
   without an ME stocking hatch.
   - *Homogeny:* every check during an install returns `homogeny.installing`, including the one GT makes as the last
     mix ends. `beginInstall`, `completeInstall` and the sneak-punch cancel now call `scheduleRecipeCheckImmediate()`,
     so a Homogeny whose plain feed hatches filled up during the 12,000-tick run mixes at its new tier at once.
   - *Reality Diffraction / Refab:* their own failure reasons (`reality.full`, `nonetwork`, `noassembler`,
     `nanites`, `anchors`) clear through changes no hatch reports (a Containment Field or the linked Assembler
     starting, the buffer draining). An idle, formed, enabled Reality machine now pushes one THROTTLED check a second
     from `MTERealityBase.onPostTick` (GT's post-failure cooldown applies, default 0). The anchor hatches register as
     watched inputs (`addIfSmartInput`; `clearHatches` drops them), and the Diffraction's buffer emptying pushes an
     immediate check. *Suggestion:* keep the 1 s poll; it is one recipe check a second per idle Reality machine, and
     there are at most a handful per base.
2. **The Diffraction's never-void buffer rides on the controller item** (`setItemNBT`: the six pending amounts and the
   run count; `loadNBTData` already reads them on placement, and the next second flushes them). Before, wrenching the
   controller while no Containment Field ran deleted up to a run (6,000 L) of Entangled Regions.
3. **The Eternity loop on the way to T10 is reported, not fixed.** SL-H20 (Tier III Core), SL-R02 (Refab
   controller), SL-R03's Region IV anchor (molten Eternity) and so SL-R04/R05 need Eternity. The pack's Eternity QFT
   loop needs Universium, which only Eye T10 makes, and Eye T10 needs SL-R04. D-48 proposal 1 (the loop takes
   Ergomatter, T7) is accepted but not built on any branch (the questbook's `eternityOnErgomatter` feature flag is
   false too). Built: `BTSRecipesReality.ETERNITY_BEFORE_T10 = false`; the gate log adds "Eternity before Eye T10" to
   those five lines and only says "every recipe is reachable" once it is true. `SLGateDump`'s ladder is the decided
   one: Eternity first made at T7, and MagMatter dropped (its only T10 tie was the MHDCSM quest prerequisite that
   D-48 proposal 2 removes; our SL-R11/R12 use it before T10). The Tier III Core tooltip says Eternity needs
   Universium until the swap ships. **Suggestion:** build the swap with P4 (rewrite the pack's Eternity QFT recipe to
   take Ergomatter, behind `replaceTecTechEyeOfHarmony`), then set `ETERNITY_BEFORE_T10` and the questbook flag in the
   same commit. *Alternative:* replace Eternity here with something pre-T10 (anchor IV molten Time is already
   anchor III; Superdense Ergomatter or MagMatter for SL-H20/R02).
4. **D-49 teaching corrected.** The tooltip, doc H-6 and the questbook outline claimed five T1 Homogenies keep a T3
   Eye fed "without a second Eye". One live star per Eye and only Sol makes Raw Star Matter: Arcturus' 1,250 L/min of
   ESPM is 125,000 L/min of Raw Star Matter through Homogenies (2.5 Sols), while a second Eye on Sirius A needs 25,000
   L/min. The tooltip now says the Homogeny costs 10x the Raw Star Matter an Eye T2 needs and suits banking a Sol
   surplus or a hands-off line; Eye T2 is still required. **Open:** if the "keeps a T3 fed" role is wanted, the ratio
   has to change (e.g. 10 : 1 at the 10x-slower speed). *Suggestion:* keep 100 : 1 (D-46/D-47 as ruled).
5. **The Homogeny caps are Reinforced Temporal Structure Casing** (closes B-78 item 6). The column caps were 2,430
   Godforge Boundless casings (1,215 Godforge crafts, more than a Godforge module) plus 615 Godforge lens-glass
   blocks, on an optional UMV side machine. Now: 2,430 Reinforced Temporal Structure Casing (TecTech's Eye casing, x32
   in the BEC since ruling 11a: 76 crafts) with Quantum Glass centres; the mockup has a new `temporal` voxel kind.
   Hatches and the controller take the Temporal casing texture. The throat tip's 5-cell Region Crystal still accepts
   the Godforge lens as its survival fallback (B-76 item 3). Existing T1 test builds must be re-placed (the caps'
   block changed). B-78 item 7 (Reality lens glass) stays open.
6. **Reality Refab sketch fidelity: the lower hull runs to the foot.** The export closed the hull at y -40 and left a
   bare stem for 21 blocks, so the machine read as a goblet on a post; the sketch's lower lines run from the belt to
   the foot. The lower hull is now a tall half-oval (b 58 instead of 34) from the belt (r 34) down to the foot skirt
   (r about 15 at y -62); the ribs and glass panels run -62 .. -5.5, and the stem and its Ground Anchor crystal stand
   inside. The piece grew from 59,245 to 65,187 cells (hull +2,260, glass +3,092, tray +581); box unchanged (104 x
   139 x 83). Costs §12 updated.
7. **Quasar stability: cells reserved, one hook each, tooltips honest** (P4 design of record, quasar-p4.md §3.1). The
   built machines said "no stability field" while P4 makes the Homogeny T2/T3 and the Diffraction Quasar hosts.
   Built: 3 `g` cells on the T2 stirring ring's outer face (90, 210, 330 degrees, where a spoke meets it from inside)
   and 6 `g` cells on the Diffraction's top deck band (one per 60 degrees). Each is its ring's casing now (Galactic
   Shell / Region Deck Band); P4 chains the GRB Receptor Port (28125) in front of that casing, so adding the port never
   un-forms a standing build. Hooks: `MTEHomogeny.quasarSigma()` / `minSigma(recipe)` (refusal
   `homogeny.stability`) and `MTERealityDiffraction.quasarSigma()` / `MIN_SIGMA` (refusal `reality.stability`), all 0
   until P4 (lang keys added). Tooltips say a Quasar link will speed T2/T3 up and Entanglement Medium will need one,
   and that the Diffraction's regions will be stabilised. *Suggestion:* P4 fills these in exactly as §3.1's host table
   says (Homogeny D_h 1.5 / 6, Entanglement Medium at sigma >= 1; Diffraction D_h 24, pause below 0.5).
8. **Questbook merge (rule 15).** The Homogeny T1 and T2 are reachable now, but the questbook branch still has them
   as `needs: ["homogeny"]` placeholders with "It will ..." text and the old D-49 claim. The conversion is in the build
   spec's "Contract additions (H/R review fixes)" so the merge pass does it; nothing on this branch touches the
   questbook.

**Alternatives.** Periodic checks via `doPeriodicChecks = true` instead of the 1 s push (GT's own back-off schedule,
but it is reset by every walk); build the Eternity swap now on this branch; keep the Godforge caps and call the
Homogeny a post-Godforge build; draw new port cells outside the current shapes when P4 lands (un-forms builds).

---

### B-81 Merging tidy-old-multis, questbook and homogeny-reality into master: renumbered H/R questions [merge]

**What was done.** The three finished branches are merged into master in this order, each as its own merge commit:
`tidy-old-multis` (the presentation tidy of the nuclear plant and production halls), `questbook` (the jar-bundled
Singularity Ladder questbook and its installer) and `homogeny-reality` (the Homogeny, Reality Diffraction and Reality
Refab). `spotlessApply compileJava` passed after each.

- **Question numbers.** The H/R branch numbered its questions B-60..B-63 and B-65..B-67 (it skipped B-64, master's
  BEC expansion), all already used by master. Master keeps B-01..B-73; the branch's seven entries are now
  **B-74..B-80** (B-60 -> B-74, B-61 -> B-75, B-62 -> B-76, B-63 -> B-77, B-65 -> B-78, B-66 -> B-79,
  B-67 -> B-80). Only lines that came from the branch were rewritten (the questions, the build spec's H/R sections,
  costs §12, the code comments in `BTSRecipesReality`, `SLGateDump`, `MTEHomogeny`, `MTERealityBase`,
  `MTERealityRefab`, `RealityProcessRecipes` and the exporter's column-cap legend line, so `manifest.json` was
  re-exported). H/R's references to master's B-50 and B-64 were left alone. The table is under "Where the old
  question numbers went". Branch commit messages keep the old numbers. The tidy branch's TQ-2..TQ-8 are their own
  series (Part D) and the questbook branch added no B entries, so nothing else moved.
- **p4-quasar still carries the old numbers.** It merged `homogeny-reality` before this renumbering (`c8f8ca7`), so
  its copy of the questions doc has the H/R entries as B-60..B-67 next to master's B-60..B-67. When p4-quasar is
  merged, take master's B-74..B-80 for those seven entries and apply the same mapping to any P4 text that cites them
  (P4's own entries are P4-nn and do not collide).
- **Code conflicts** (all additive, both sides kept): `BTSRecipes` runs `bec`, `automation`, then `reality-process`
  and `reality` (as P4 resolved it); `BTSFluids` registers `SLCondensates` then `SLRealityFluids`; `BTSMachines` has the
  Directive Hatch (28127) beside the Homogeny (28130), Reality Diffraction (28131) and Reality Refab (28132) with one
  ledger comment; `SLItems.defineWithTooltip` is package-private (H/R's `SLMaxItems` uses it) and builds the tidy
  branch's shared `BTSTooltipItem`; `/btskit` keeps the tidy usage form, lists and tab-completes the three H/R kits;
  the costs audit's title names §11 (BEC) and §12 (H/R). The questbook merge kept master's OpenComputers registration
  and `renderKerrHole` beside the branch's questbook listener and config keys.
- **Structures.** Master had not touched the exporter or the shapes since the H/R branch left it, so the merged
  shapes are the branch's (exported from design-repo `a01c6b6`, voxel sha `2742379a5cf9`); `--check` against that
  voxel.js is clean. The design repo's working voxel.js has since moved on with P4 (`1b407b3`); re-exporting from it is
  P4's merge.
- **Checks.** Lang has no duplicate keys, and every literal `ofFailure` / `ofSuccess` id has its RC-2
  `recipe_result` key. No in-game run was part of the merges.

**Suggestion.** Nothing to decide unless you would rather the H/R questions kept their branch numbers (that would mean
renumbering master's render QA, automation and package entries, which the brief and INSTALL.md already cite).

---

### B-82 After the merges: the Homogeny quests, the Tier 13 warning and the questbook build [merge follow-up, built]

**What was done.** The questbook steps the H/R review left for the merge (build spec "Contract additions (H/R review
fixes)", B-80 item 8), the questbook build notes, and the Tier 13 (UXV) warning.

1. **The Homogeny quests are real.** `features.homogeny` is true. 48118 The Homogeny keeps the controller retrieval
   (28130) and adds 1,000 L Excited Stellar Plasma Mixture; 48142 The Homogeny T2 takes the Homogeny Tier II Core
   (detect only) and 1,000 L Degenerate Stellar Mixture; 48184 The Homogeny T3 now needs `homogeny` and `eyeT9T10`, so
   it stays a placeholder (its fluids are P4/P5). The texts are present tense in the book's BUILD / RUN / AUTOMATE form,
   with the contract's "bank a Sol surplus, not a Sirius A replacement" paragraph. Two corrections against the old
   placeholder text: T1 has **three** feed vessels (six from T2), and the book's name "Raw Stellar Plasma" is used for
   Raw Star Matter, as everywhere else in it.
2. **The Homogeny T2 quest is reachable in the book but its core is not craftable in survival yet.** SL-H10 (Homogeny
   Tier II Core) takes Field Generator UXV x16, Electric Pump UXV x32 and UXV circuits; RC-2's UXV Field Generator needs
   molten Universium (Eye T10). B-72 noted the same loop and left SL-H10 alone. The quest carries a [warn] saying so,
   and the Tier 13 warning names the Tier II Core.
   - *Suggestion:* give SL-H10 the same UMV stopgap as the T5/T6 cores (B-72: twice as many UMV parts, here Field
     Generator UMV x32 and Electric Pump UMV x64), until the UXV parts and circuits design (brief item 22a) is built.
   - *Alternatives:* keep it as built (the quest waits on the UXV design); or gate 48142 on `uxvParts` so it shows as
     "(planned)" until then.
   - *Done (B-84):* the suggestion, as you chose for the playtest jar ("Ship UMV stopgap").
3. **No structure checklist on 48118.** The Kerr and Eye quests have an optional "bring the blocks" retrieval built from
   the manifest; the generator needs a `bomItems.homogeny` table (one item id per legend role) for that, and there is
   none yet, so 48118 has only the controller and fluid tasks. *Suggestion:* add the table when the H/R blocks are
   next touched (all three machines share the legend, so one table covers the Reality machines too).
4. **Tier 13 (UXV) warning.** It now says plainly: Universium only comes from Eye T10, which is not built; the Eye's
   T5/T6 cores do not need it (B-72's UMV stopgap); the pack's own UXV parts, and anything built from them (the
   Homogeny Tier II Core included), still need MHDCSM and Universium until the new UXV parts and circuits are designed
   and built. The two "planned swap that takes Universium out of UXV parts" lines on the UXV glass and superconductor
   placeholders are unchanged (they describe the design, not the jar).
5. **Questbook build.** With `kerr-redesign` and `questbook` both in master, a plain `gen-bom.ps1` run checks our ids
   against this repo and finds 28117/28118; the README no longer tells you to set `BTS_QB_SRC`. The run had been
   failing on a tidy x questbook seam: the tidy pass renamed the pillar block to "Reactor Fuel Pillar Frame" in
   `BTSBom` only, and `gen-bom.ps1` still mapped it to "Reactor Fuel Column". The scripts now use the new name (the
   constant is `BTSBom.REACTOR_FUEL_PILLAR_FRAME`), and the plant chapter and the bundle were regenerated.
   - *Not done:* the rest of the tidy pass's questbook handoff (`tidy-old-multis.md`, "Questbook handoff"): the other
     glossary renames in quest text (Unit One main station, Steam Generator, Gas Centrifuge Cascade, Fuel Fabrication
     Line, Reprocessing Canyon) and the mechanics cut from tooltips that the book must carry. That belongs with the
     TQ renames (your "full free reign" answer) so the names change once.
   - The bundle records the commit it was generated from (`srcHead`), so `gen-singularity --check` reports the overlay
     "stale" after any later commit even when nothing else differs. Harmless; regenerate when packaging.

**Checks.** `gen-singularity` `--check` and `--selftest` pass; `spotlessApply compileJava` passes. Not run in game.

### B-83 The field-generator upgrade path, and Resonance Chamber II rewritten in place [user answers 2026-10-13, built]

**Context.** Your answers to B-30 ("Add an upgrade path") and B-29 ("Rewrite GT's in place"). Every Eye tier step
re-crafted 188 compression generators (and the 48 stabilisers, which hold one each), each consuming a Kerr controller;
and Resonance Chamber II had two recipes in NEI (GT's with a White Dwarf Matter foil, our SL-P39 with SpaceTime).

**What we built.**

1. **SL-G01: GT's own chamber recipe, rewritten in place.** With `replaceTecTechEyeOfHarmony=true`, the Condensate
   Assembler recipe for Resonance Chamber II takes 2 SpaceTime foils where it took 1 White Dwarf Matter foil, swapped
   at the same input index (so its nanite tiers and condensates are untouched), and the map's lookup is rebuilt. One
   recipe in NEI. **SL-P39 is retired**; its id stays reserved and its body registers only if no GT chamber recipe is
   found to rewrite. SL-P33 (the Black Dwarf-free Resonance Chamber III for the T5 gate) is unchanged, a separate
   recipe as before: you ruled on chamber II only. *Suggestion:* give it the same in-place treatment.
2. **SL-U00..U48: tier t-1 -> t in the Condensate Assembler** (`BTSRecipesGeneratorUpgrade`; since B-89 whatever the flag says). The inputs
   are derived at load from GT's own tier t and t-1 recipes, so they follow the pack:
   - **Compression (SL-U01..U08) and Time Dilation (SL-U11..U18):** the tier t-1 generator, plus every item and
     condensate GT's tier-t recipe takes MORE of than its tier t-1 recipe. An item that changes identity between tiers
     (the tier material's dense plate or gear, a manipulator or metamaterial of a new tier) is taken in full; nothing is
     refunded. What both tiers take the same of stays inside the old generator: **the Kerr controller** (the BHC swap
     runs on GT's recipe, both tiers hold one), the Reinforced Spatial / Temporal casing and the two AE2 singularity
     cells. Same EU/t and duration as GT's tier-t craft, so the path is never faster than crafting from scratch, only
     cheaper. Nanites: each input keeps the nanite GT's tier-t recipe gives it; the old generator takes its casing's.
   - **Stabilisation (SL-U21..U28):** a stabiliser holds a compression and a time generator, whose 23 loose parts do
     not fit the Condensate Assembler's 16 item slots. So it takes the old stabiliser, one **Spacetime Compression
     Retrofit Kit** and one **Time Dilation Retrofit Kit** of tier t, and its own difference (circuits, frames,
     solenoids, sensor array, manipulator, screws, rods). The generators inside are upgraded, Kerr included.
   - **Retrofit Kits (SL-U31..U38, SL-U41..U48):** a **Field Generator Retrofit Frame** plus exactly the compression /
     time difference above, at GT's tier-t time. The frame (SL-U00, Assembler x2: Hexanite frame, 4 SpaceTime plates,
     a UMV circuit, 144 L molten Infinity, 20 s UIV) keeps a kit's inputs from being a subset of GT's generator recipe,
     which the Condensate Assembler would otherwise be free to pick (it looks recipes up by items only).
   - New item `fieldRetrofitKit` (meta 0 frame, 1-8 compression kits, 11-18 time kits; names use TecTech's tier names,
     "Stable Spacetime Compression Retrofit Kit"), tooltips, three generated EOH-family textures.
   - Example, compression Crude -> Primitive (as the server built it): the Crude generator, 1 Cosmic Fabric
     Manipulator, 4 Primitive Shielding, 1 Resonance Chamber II, 1 UXV circuit, 4 Singularity Shielding casings,
     1 Hexanite frame, 2 long Hexanite rods, 2 dense Proto-Halkonite plates, 1 superdense Hexanite / Cosmic Neutronium /
     Infinity / Transcendent Metal plate; 10,000 L Boundless Cosmic Solder, 10 ingots Space, 20 ingots Neutronium
     condensate. GT's fresh Primitive also takes the Kerr controller, the Spatial casing, both singularity cells and
     the shared half of every scaling input.
3. **Teaching (rule 14):** the Eye tooltip's field-generator lines say how a tier step works; the questbook's shared
   gate paragraph (on every pack compression-tier quest) has a TIER STEP paragraph; the kits' tooltips say what they
   are for; NEI shows the recipes.
4. **Load phase (found by the test):** GT's Condensate Assembler recipes are not in the map yet when our postInit runs
   (the first boot found nothing to rewrite or derive and skipped 41 recipes). SL-G01 and the upgrade path now run at
   load-complete, like SL-B01/B02 and the BHC swap, and print their own summary line.

**Checks.** `spotlessApply compileJava`; headless `runServer` (RC-2 deps, world `sltest-ovt`, port 25565):
"Recipe registration complete: 281 registered, 0 skipped"; "SL-G01: GT's Resonance Chamber II ... takes 2 SpaceTime
foils in place of 1 White Dwarf Matter foil (input 3 ...)"; "Field-generator upgrade path: 40 Condensate Assembler
recipes + the Retrofit Frame ..., 0 skipped". `/btseye bec`: 53 Condensate Assembler recipes make our parts and GT's
lookup finds all 53; the upgrade path's 40 are all found by GT's own lookup with their inputs (nothing shadows them;
stabiliser upgrades use 15-16 item stacks, 3 condensates each). `/btseye gates`: every compression tier now has GT's
recipe and our upgrade; both are CLEAN for Eye T1-T7, each Resonance Chamber II via its single (rewritten) recipe.
T8/T9 (Tipler, Gallifreyan) show the known P4/P5 loop (Resplendent Resonance Chamber, MHDCSM foil) on both recipes,
because the upgrade inherits what GT's tier adds. BHC consumers 0; Kerr consumers 12, as before (the upgrades take
none). Not run: a real Condensate Assembler craft (the bench builds no BEC network).

**Suggestion.** As built. **Alternatives.** Kits for every family (one recipe shape everywhere, but two crafts per
compression upgrade); a shorter upgrade time than a fresh craft (it would make the path faster as well as cheaper).

### B-84 UXV parts before Eye T7: the Homogeny Tier II Core takes the UMV stopgap, and the audit [user answers 2026-10-13, built]

**Context.** Brief item 22a: the UXV-part override (D-45, item 20) waits for a design brainstorm; until then the jar
ships B-72's stopgap ("Ship UMV stopgap"). B-82 found SL-H10 still on UXV parts.

**What we built.** SL-H10 (Homogeny Tier II Core, needed before Eye T6): Field Generator UXV x16 -> **Field Generator
UMV x32**, Electric Pump UXV x32 -> **Electric Pump UMV x64**; everything else unchanged. The Homogeny T2 quest's
warning becomes a note, and the Tier 13 warning no longer names the core.

**Audit: every recipe on master a player needs before Eye T7, for UXV parts and for circuits/parts that need
Universium or MHDCSM.**

| Recipe | Needed for | UXV parts | UXV circuits | State |
|---|---|---|---|---|
| SL-B70 / SL-E05 Subspace Cooling Core | Eye T4 -> T5 | none (UMV x2, B-72) | 16 | clean |
| SL-B80 / SL-E06 Magnetar Seed Core | Eye T5 -> T6 | none (UMV x2, B-72) | 32 | clean |
| SL-B71 / SL-B81 (Star Chart: Sirius B / SGR 1806-20) and their fallbacks SL-P35 / SL-P37 | Eye T5 / T6 | none | 1 each | clean |
| SL-H10 Homogeny Tier II Core | Degenerate Stellar Mixture, Eye T6 | **was FG UXV 16, Pump UXV 32** | 32 | **fixed here** |
| SL-H01..H05 (Homogeny T1 and its blocks) | Excited Stellar Plasma bank | none | none | clean |
| RC-2 compression / time / stabilisation generators t0-t6 (Eye T1-T7 gates) and our SL-U upgrades | the gates | none | t+1 each | clean (`/btseye gates`) |
| Kerr (SL-K*), the Eye controller and its T1-T6 parts and bands (SL-E01..E04, SL-E07, SL-P*, SL-B20..B62), SL-A01, SL-S01/S02 | Eye T1-T6 | none (UIV/UMV parts) | none, or UMV | clean |
| SL-H20 Homogeny T3 core, SL-R01/R02 Reality machines, SL-R10..R17 their blocks | after Eye T7 (White Hole Matter, Eye T9; Ergomatter, T7) | FG / Pump / Emitter / Sensor / Robot Arm / Conveyor UXV | yes | out of scope (post-T7); they wait for the UXV parts design |

**UXV circuits are not a loop.** The `circuitUXV` ore name is met by the coremod's Quantum Circuits. Two of the three
(Polychrome: Bedrockium, Neutronium, QPIC chips, UU-Matter, Osmium; Dimensional: SpaceTime, mutated living solder) need
neither Universium nor MHDCSM. Only the Heavenly one does (UXV parts, Universium nanites, MHDCSM). GT's Cosmic Mainframe
and Transcendent Computer have no recipe in RC-2. (Read from the coremod 2.9.84 assembly-line dump; not crafted.)

**Not ours, still open:** the pack's own UXV parts (Field Generator, Pump, Emitter, Sensor, Robot Arm, Conveyor UXV,
the UXV CoAL) keep RC-2's Universium/MHDCSM recipes, so anything of the pack built from them waits for Eye T10 or the
UXV design. Nothing of Beyond the Stars before Eye T7 needs one now.

**Suggestion.** As built until the UXV parts and circuits brainstorm lands; then move B70/B80/H10 back to UXV parts
if you want the old price.

**Settled 2026-10-13.** The brainstorm landed (`designs/docs/uxv-circuits.md`; your answers Q-U1 "Both", Q-U2
"Empyrean", "Omega", Q-U9 "The Eye's live star"). Its Phase U1, on master after the playtest run, retires this
stopgap.

### B-85 The tidy questions settled: renames, defaults, the halls' parallel [my answer 2026-10-13, built]

**Context.** TQ-1..TQ-8 (Part D and `tidy-old-multis.md`). Your answer: "You have full free reign on this". Each
choice is recorded under its TQ entry in Part D.

**What we built.** Display text only for the renames: registry names, MTE ids, enum names and NBT keys are unchanged,
so worlds, progress and bought upgrade nodes carry over.

| TQ | Choice |
|---|---|
| TQ-1 | Milestone line REPROCESSING shows as **ORE PROCESSING** (tree caption "ORE PROC."; `/btsmilestone ore`) |
| TQ-2 | Enrichment Train -> **Yellowcake Mill** (the "Despite the name it does not enrich" line goes) |
| TQ-3 | Spine Sensor -> **Spiral Cycle Sensor** (BOM constant `BTSBom.SPIRAL_CYCLE_SENSOR`, diagram text) |
| TQ-4 | Propellant Lv 1-6 -> **Grade 1-6** (cartridges and fluids). Not "Mk I-VI": GoodGenerator's Naquadah Fuel MkI / MkIII feeds Grade 2 / 3 |
| TQ-5 | `/btsbuild` and `/btskit` both default to **tier 1** |
| TQ-6 | The halls offer **input separation**, off by default on new and existing halls |
| TQ-7 | Extra Centrifuge Stage -> **Extra Leach Tank** |
| TQ-8 | The halls use GT's **`getTrueParallel()`**; the GUI and scanner print the effective parallel |

- **Existing installs see the new names.** GregTech writes MTE and GT-fluid names into
  `config/GregTech/GregTech.lang` once and never refreshes them, but `GTLanguageManager` prefers a key the normal lang
  system provides (it skips GregTech.lang when `StatCollector.canTranslate` the key, and a resource reload takes the
  resource-pack value). Our `en_US.lang` now carries `gt.blockmachines.multimachine.beyondthestars.enrichmenttrain.name`,
  `gt.blockmachines.hatch.beyondthestars.spinesensor.name` and `fluid.naqpropellant1..6`, so the old GregTech.lang lines
  just stop being used. *Read from GT's code; not yet seen in a client.* If a client still shows an old name, deleting
  that line from GregTech.lang fixes it.
- **Questbook:** the plant chapter is regenerated (`gen-bom.ps1`, `gen-diagrams.ps1`) with the new names and the tidy
  glossary handoff B-82 left open: "main station" -> the controller / Nuclear Plant Foundation, "Steam Generator" ->
  Reactor Silo (the blueprint quest, and the quoted not-formed message as the code prints it now), "Unit One
  Commissioning" -> Plant Commissioning (the tree's root was renamed by the tidy pass), the entry quest is "Beyond the
  Stars - The Nuclear Plant". Quest ids are unchanged; the blueprint quest's DefaultQuests file name follows its title.
- TQ-6 / TQ-8 are small gameplay changes: TQ-8 bites only for a player who unticked the Power Panel's "max" box.

**Not done:** the rest of the tidy questbook handoff (mechanics cut from tooltips that the book must carry: the RPM
rule, the dynamo ceiling, the rod ladder and so on). The names are done once, as B-82 asked; the text additions are a
writing pass of their own. *Suggestion:* do them with the next questbook pass.

**QB-7 settled (your answer: "Keep as trophies").** The trophy quests E63 / E64 keep consuming 12 / 8 of our Eye
controllers; nothing changes.

**Checks.** `spotlessApply compileJava`; `gen-bom.ps1` (BTSBom in step, DefaultQuests regenerated) and
`gen-singularity --check` clean apart from the overlay's `srcHead`; the dev-server boot above ("281 registered, 0
skipped"); golden 78/78.

### B-86 The window bands: the Kerr's hole and the Eye's star read from the front [my answer 2026-10-13, built]

**Context.** Render QA K2 / E1 (B-66) and E-9 (B-67, B-68): from the front at their own height the Kerr's drum and
governor arc hid the hole, and the Eye's core band (and from T4 its arms and rim) hid the star. Your answer: "Glass
window bands" (a Quantum Glass band in the Kerr drum and Quantum Glass in the Eye's front ring segments at eye level;
structure, hologram, NEI and BOM updated).

**What we built.** `tools/export-structures.js` re-labels cells the mockup already places (voxel.js is not edited) at
|y| ≤ 2 from the object's centre, a 5-row window:

| Machine | Quantum Glass pane | Open behind / in front of it | Cells |
|---|---|---|---|
| Kerr (T1 and T2) | the governor arc's outer shell in front of the hole (z > 0, \|x\| ≤ 12); all round the rest of the drum, the outer mirror ring's casing skin | the arc's lining, every other layer of both mirror rings, and at T2 the mid band in front of the arc | 700 panes; 3,006 open (T1), 3,323 (T2) |
| Eye (all tiers) | the core band's outer shell in its front ring segments (z > 0, \|x\| ≤ 26, between the clamp struts at 30 and 150 deg) | the band's lining behind it; the T4 / T5 arm and T6 rim cells in front of it within \|x\| ≤ 10 | 339 panes; 371 open in the core, 422 / 371 / 213 in the arms, arms2, rim |

- **Old machines keep forming.** A pane char (Kerr `S` / `T`, Eye `A`) is `ofChain(Quantum Glass, the block it
  replaced)`: glass first, so holograms, survival builds and NEI place glass; an earlier build's casing still
  counts. An open cell is `' '` in the shape, which StructureLib never checks, so whatever an earlier build has
  there is fine too. Hatches and sockets are untouched.
- **Shapes, hologram, NEI, BOM.** The shapes and manifest are regenerated (legend rows, a `windows` section). New
  exporter checks: the pane and opening counts, and a clear front sightline at y ±1 (Kerr |x| ≤ 8; Eye T1-T3
  |x| ≤ 20, T4-T6 |x| ≤ 8). The kits' bills, the tooltips (a "Quantum Glass (window band)" line on the Kerr, the Eye's
  glass count, one structure line each) and the questbook checklists follow (bomItems map the pane roles to
  Quantum Glass); the Kerr and Eye build quests say where the window is.
- **Costs** (costs doc §13): the Eye barely moves (2.4 % of the core's Spatial / Temporal casings). The Kerr T1
  drops 1,180 mirrors, 1,595 backing, 810 Extreme Density casings and 121 linings: about a third of its Infinity
  (54k → 35k), 10.9M → 7.6M Bedrockium, 1.3e14 → 1.0e14 EU.
- **Render harness:** a fourth view, `level` (level with the object, 24 blocks in front of the structure's front
  face), so every case is shot from the front at eye level; the test world moved to `bts-rendertest-L3` (the builds
  never clear a site, and the old world's sites had blocks where the windows are open now).
- **Bench:** `oldwindows` on the console builds of `/btskerr` and `/btseye` puts the pre-window block back into every
  pane cell and re-walks (open cells need no test: nothing checks them).

**Dev server** (`runServer`, `sltest-ovt`, "281 registered, 0 skipped"): Kerr T1 creative + `oldwindows` (700 panes back
to casing) FORMED, modules linked; Kerr T2 survival path FORMED (25,051 blocks auto-placed) and FORMED again after
`oldwindows`; Eye T1 creative + `oldwindows` (339) FORMED; Eye T6 survival FORMED, all bands, and after `oldwindows`.
The damage test now hits the pane behind the KEE controller ("Any of: Quantum Glass / Extreme Density Space-Bending
Casing") and the survival repair restores it. A Kerr T2 and an Eye T6 built earlier in the session with the old block
in every cell the first window pass had touched (4,023 / 3,078 cells, i.e. exactly a pre-window build) re-walk FORMED
on the final shapes.

**Judgement call: one pane, not solid glass.** The first build made every window cell Quantum Glass (3,706 on the
Kerr T1, up to 3,078 on the Eye T6). In the real client (`render-qa/round5/pass-a/`) Quantum Glass turned out to be
a glowing blue glass, and three walls of it on the Kerr's sightline hid the hole completely; the Eye's star showed
only as a haze through two to eight walls. So each window is now one pane with the cells behind it open, and from
T4 the arms and the rim leave an opening in front of the core's pane instead of adding more glass. The Kerr's band
is still a Quantum Glass band all round its drum (the outer ring's skin); only in front of the hole is the pane the
governor arc's shell.

**Round 5** (`render-qa/round5/`, 104 shots: 13 cases × front / 34 / close / level × day / night; contact sheets in
`round5/sheets/`):

- **Kerr:** the hole reads from the front through the window at 16, 64 and 256 SM: a black disc with the cream
  lensing ring at its sides, plainest at night; by day the blue pane tints it. At 256 SM (T2) the 5-row window shows
  the middle of the hole and the ring's sides, not the whole ring.
- **Eye:** Sol, Sirius A (weakest by day: white-blue behind blue glass), Arcturus and the Core Collapse swell read
  through the pane; the neutron star with its jets, the bounce, the white and the black dwarf and the magnetar read
  through the arm / rim openings and the pane, at eye level, day and night.
- FPS at the shots: Kerr 1,221-1,336, Eye 842-1,219 (the magnetar with its flare warning lowest, as in round 4).

**Open (your call):**

1. **Win the Kerr's Infinity back?** *Suggestion:* SL-P02 / SL-P03 (mirror backing, mirror) output ×2 → ×1: the
   cavity's remaining 3,628 blocks then cost 113 % of the old 6,403. Alternative: accept the cheaper cage (it is
   still well above the BHC it replaces).
2. **Window height.** 5 rows shows the whole star up to Sol's size and the middle of the larger holes. *Suggestion:*
   keep 5; a 7-row window would take the Eye core band's top and bottom rims away entirely.
3. **P4 merge note:** P4's exporter must take over `WINDOW` / `windowKerr` / `windowEye` / `frontPane` and the open-cell
   handling when it merges, and re-export (the current shapes were exported from voxel.js a01c6b6, the sha the
   shapes carry; the design repo's newer voxel.js needs P4's exporter).

### B-87 Post-merge tidy: one condensate owner, shared kit helpers, Superdense Ergomatter, this list restructured, merged worktrees removed [post-integration tidy, built]

**Context.** After the three merges (B-81) and the 2026-10-13 answers (B-83..B-86): tidy the merged tree without
changing behaviour, finish the Superdense rename (ruling 17) on master, turn this file into one deduplicated list,
and remove the worktrees of branches that are fully merged.

**What was done.**

1. **One owner for the BEC condensates.** The H/R branch's `BTSRecipesReality.shim` registered SL-B10 / B13 / B14's
   fluids if nothing had, and re-ran their generator ids. Now `SLCondensates` registers the fluids (preInit) and
   `BTSRecipesBEC` owns their generator recipes; the Reality unit's `becCondensate` only resolves which fluid to ask
   for (the output of whatever generator recipe already condenses the source, else `SLCondensates`' fluid) and
   registers nothing, so the order of the `bec` and `reality` lines no longer matters. `SLRealityFluids` registers its
   condensates through `SLCondensates`' registrar. B-78 item 3 is closed.
2. **Shared kit helpers.** `socketRels` (identical in `KerrKit`, `EyeKit` and `HRKit`) is `BTSShapes.socketRels`;
   `give` / `giveMany` (identical in all three) are `SLConsoleTest.give` / `giveMany`; `EyeKit`'s private copy of
   `worldPos` uses `SLConsoleTest.worldPos`. (The commit message says HRKit's `worldPos` too; HRKit never had one.)
3. **Dead code and stale comments.** `MTEReactorCoreMk1.RPM_REDLINE_MILLI` was never read; the D-26 MTE ledger comment
   no longer says the next free id is base+14. A scan found no unused imports, no other unused private method or
   constant, and no code comment citing a renumbered question.
4. **Superdense Ergomatter.** Master's code and lang already said Superdense; the questbook spec
   (`questbook-integration.md`) and quest 48149's DefaultQuests file stem (`SupermassiveErgo` -> `SuperdenseErgo`) did
   not. The Supermassive Seed Core and Sgr A*'s "supermassive black hole" keep their names.
5. **This file.** The open questions, the settled answers and the open work now sit at the top, each once; Part C's
   table is folded into "Settled" with the 2026-10-13 answers (brief item 22) and the UXV design answers (item 23) in
   your words. The B entries they settle carry a "Settled" line. Nothing was renumbered.
6. **Worktrees.** `addon-kerr-redesign`, `addon-tidy` and `addon-hr` were clean and their branches are in
   `git branch --merged master`, so they were removed with `git worktree remove` (no `--force`). The branches stay.
   `addon-qb`, `addon-p4` and `addon-autoshots` are kept.

**Judgement calls.**

1. **`addon-hotfix` is NOT removed.** Its branch `hotfix/rc2-recipe-result-lang` (b320e4e) is not in
   `git branch --merged master`: its fix reached master as the cherry-pick f484699, and the Kerr redesign later
   removed or reworded five of its lines (B-58). The worktree is clean. *Suggestion:* remove it by hand
   (`git worktree remove ../addon-hotfix`); nothing on that branch is missing from master.
2. **Quest 48149's ledger key stays `supermassive_ergomatter`.** `questbook/id-ledger.txt` is append-only (the
   generator refuses a key that changes meaning), and the key is internal: players see the title, which already said
   Superdense. Only the file stem changed. *Alternative:* re-key it before the questbook first ships.
3. **"N registered" drops by 3** (281 -> 278): SL-B10 / B13 / B14 were counted twice (once by `bec`, once by the shim),
   with no extra recipe. Dev server boot on this commit: "Recipe registration complete: 278 registered, 0 skipped";
   `/btseye bec` 53 Condensate Assembler recipes, all found by GT's lookup, 6 generator recipes for the 6 BEC
   condensates, 0 fallbacks; `/btshr recipes` 10 generator recipes of ours (the 6 plus SL-C01..C04), one per source.
4. **Left as they are:** the recipe files' own small helpers (`items`, `tectech`, `molten`, `cond`, `s`), which the
   one-owner-per-file convention keeps local; the near-copies of `solidPercent` in the build commands (each takes its
   machine's own pieces); the BTSMachines ledger lines P4 also edits (left for that merge).

**Checks.** `spotlessApply compileJava`; `node tools/gen-singularity.js` (regenerated); the dev-server boot above.

### B-88 Merge + playtest: the full RC-2 dev-server test of merged master [playtest, built]

**What was run.** Master `6ca4490`, which has the three merges, the 2026-10-13 answers, the window bands and the
tidy pass.

Static checks first:
- `spotlessApply compileJava`.
- The lang lint, 214 lookups: every `ofFailure` / `ofSuccess` id (including the H/R ids and the dynamic Eye / Kerr
  ids), the shutdown reasons, `StructureErrors.of`, `translateToLocal`, the recipe-map names, items, blocks, casing
  metas and every `GT5U.*` literal, checked against our `en_US.lang` and RC-2 GT's.
- `node tools/fieldsim-golden.js --check --java`.

Then headless `runServer` (RC-2 deps + OpenComputers + BetterQuesting, port 25565), eight boots on two fresh flat
worlds:
- `pt-main` for the machines. Its questbook came from the RC-2 pack's DefaultQuests with our overlay merged as the
  README says.
- `pt-qb`, standing in for an existing world: the pack's DefaultQuests only, and two fake players with progress.

| Area | Result |
|---|---|
| Load | `278 registered, 0 skipped` in every boot. The expected `H/R gate:` placeholder lines are all there: 14 recipe lines (SL-C02, SL-C03, SL-C04, SL-R11, SL-R13, SL-R16, SL-R17, SL-H20, SL-R01, SL-R02, SL-H21, SL-R03, SL-R04, SL-R05) plus the summary "14 Homogeny/Reality recipe(s) are registered but unreachable until P4/P5". The BEC line reports 6 condensates and no legacy fallbacks. SL-B01/B02 are x32. SL-G01 rewrote GT's Resonance Chamber II (input 3). "Field-generator upgrade path: 40 ... + the Retrofit Frame, 0 skipped". 10 BHC->Kerr swaps. TecTech's Eye recipes are removed. None of the eight logs has a `com.nateyburr` frame or a `[beyondthestars]` WARN/ERROR. |
| Lang lint | 0 missing and 0 duplicate keys. One false positive: `item.beyondthestars.fieldRetrofitKit.name`. The kit names come per meta from `fieldRetrofitFrame` / `compressionRetrofitKit` / `timeRetrofitKit`, and all three are present. `recipe_result.beyondthestars.eye.star_unit5` still has no producer; it is harmless (as in B-71). |
| Golden | 78 / 78 PASS, before and after. |
| Gate dump | `/btseye gates`: Eye T1-T7 are CLEAN, on GT's recipes and on our upgrade recipes. Resonance Chamber II now has one recipe, the rewritten one. T8 / T9 LOOP through the Resplendent Resonance Chamber's MHDCSM foil (known; P4/P5, B-29). 0 BHC consumers are left. |
| BEC | `/btseye bec`: 53 Condensate Assembler recipes make our parts, and GT's own lookup finds all 53. There are 6 Condensate Generator recipes. GT's lookup finds all 40 upgrade-path recipes. `/btshr recipes`: Homogeny 3, Diffraction 1, Refab 2; 52 Condensate Assembler recipes and 10 condensate generators of ours. SL-H10 (the Homogeny Tier II Core on the UMV stopgap) is registered and is not in the H/R gate list. |
| Kerr | Four builds, KEE and both modules FORMED each time, with re-projection, damage and repair: T1 north (creative), T1 east (survival), T2 south (creative + `oldwindows`, 700 panes back to the old casing), T2 west (survival). The damaged cell names "Quantum Glass / Extreme Density Space-Bending Casing". Extractor: 128 Stemcells -> 128 Plasma Membrane at parallel 896. Compressor: GT compressor recipes at parallel 1,232 on T2. DENSITY is refused on T1 and runs on T2 (2,000 L Hyperdense Stellar Mixture, 2 parallels). The render is claimed at the centre and scales with mass. |
| Eye | Seven builds, all FORMED with the expected bands, with re-projection, damage and repair: T1 north, T2 east survival, T3 south + `oldwindows`, T4 west survival, T5 north + `oldwindows`, T6 east survival, T6 south + `oldwindows`. Lifecycle on a T6 structure from unlocked T1, unattended (`directive` + `autorig`, milestones forced): Sol lit -> Mass Infusion 600 s -> Shell Ignition 604 s -> Core Collapse 1,001 s -> First Cooling 7,200 s -> Magnetar Merger 177 s. Each core was consumed and T6 lit. 1:1 gate: one compression generator at tier 3 under a T4 chart -> "every Spacetime Compression Field Generator must be tier 4 or higher"; tier 2 under Arcturus -> the T3 message; all back to tier 4 -> runs. |
| Homogeny / Reality | Homogeny T1 north: 100,000 L Raw Star Matter -> 1,000 L ESPM. T1 -> T2: Install was refused for the missing ring, then the milestone, then the core. With all three in place, the install queued behind the running mix, paused on power and finished: T2, 4 parallels. Diffraction (survival build, 53,004 blocks) and Refab are FORMED and idle with "No BEC network ..." / "No running Condensate Assembler within 64 blocks ...". |
| Directive / OC | Eye `ocloop`: getState / getField / getTransition / getDirective, the dial and feed clamps, control EYE_DIAL. Kerr `ocloop`: setpoint clamps, collapse / open refusals and successes. `ocprobe` finds bts_kerr in OpenComputers' registry. `ctrl out`: comparator and invert. Kerr `autorig 120`: no self-close, mass within 0.25 SM. |
| Old plant (tidy pass) | `/btsbuild t1` and `t3`: FORMED. With the new `/btsbuild hall` bench, all six standalone multis form from the projector's survival path: Yellowcake Mill, Extraction Platform, Cascade Hall, Fuel Fabrication Hall, PUREX Canyon and Site Batching Plant. Runs: PUREX (128 Cladding Hull + 384,000 L Dissolver Liquor at parallel 8), Fuel Fabrication (16 Uranium Fuel Rods), Batching (80 Concrete Dust at parallel 4), Platform (Thorium / Uraninite / Tantalum dust), and Cascade after fix 1. Names: GT's lookup shows "Yellowcake Mill" although the server's `GregTech.lang` still holds `enrichmenttrain.name=Enrichment Train`, so B-85's override works on a server with an old GregTech.lang. |
| Questbook | See the list below the table. |
| Restart | `pt-main` across three restarts: Kerr holes (64 / 130 SM) and setpoints, linked modules, the Eye's unlocked tiers, compression tiers, transition state and last failures, the Homogeny's installed T2, hall contents, and Directive codes (after the first tick) all came back as before. |

**Questbook results.**
- **Fresh world** (merged DefaultQuests): the auto-install applied 62 / 62 edits and `status` is all ok. `verify`
  resolves 113 / 115. The 2 misses are `eternalsingularity:combined_singularity:10` / `:15`, the icons of 48149 /
  48147. The pack's own quests use both metas, so they exist in RC-2; the dev server lacks the mods that add them.
- **Existing world with progress** (pack DefaultQuests). Player a1 had completed the BHC, EOH, Stargate Ring Block,
  T5: Superb, Universium and quest 191; player a2 had Star Fuel, Simulating a Solar System and the BHC.
  - Install keeps every completion. E01 / E10 / E63 / E64 restart their tasks, as specified.
  - A second install is a no-op (the DB is byte-identical). `install force` gives the same DB apart from key order.
  - `uninstall edits` restores all 3,813 pack quest definitions identical to the snapshot, and the fallbacks 48103 /
    48109 appear.
  - `uninstall all` restores the pack exactly (quests and lines), and stays in force across a restart. Installing
    again brings our quests' progress back from the backup.
- **Restarts.** With the pack's BQ setting, a restart says "already up to date". With BQ's "Load the default quest DB
  on world startup" = true, which wipes our chapters on every start, the book is re-applied, every completion is kept,
  and our own quests (48000, 48114) come back from the progress backup.

**Fixed here (each its own commit).**
1. **The Cascade Hall could not be run from its tooltip** (`33f0996`).
   - The hall has no input bus, and the circuit that picks the cut is read from the controller slot. Nothing said so.
   - Every cascade recipe makes two fluids, but the tooltip allowed "1-2" output hatches. With one plain Output Hatch
     the hall forms and then sits at "Not enough fluid output space".
   - The tooltip now says where the circuit goes and asks for 2 output hatches (or 1 ME Output Hatch).
   - Verified: with one hatch it is stuck; with the circuit in the controller slot and a second hatch it runs.
   - *Judgement call:* tooltip only. The structure still forms with one output hatch, because one ME Output Hatch is
     enough. *Alternative:* make the walk require two plain output hatches unless one is ME.
2. **The questbook auto-install now logs why it runs** (`aa459b8`; no behaviour change).
   - On `pt-main` every boot re-applied the book.
   - Cause: the dev server's `betterquesting.cfg` had BQ's default "Load the default quest DB on world startup" = true
     (the RC-2 pack ships false), so BQ's own default load put the fallback quests back on each start.
   - That is the documented path and it kept all progress, but the log only said "62 applied". It now names the
     reason.
3. **Test bench** (`21969a9`): `/btsbuild hall <kind> x y z [facing] build|check|status|stock|tier|circuit|place`, the
   first console bench for the six older standalone multis.

**Seen, not changed.**
- An Eye left unregulated at T6 fails and re-runs the Magnetar Merger about every 40 s, each time spending the merger
  charge (B-71 "seen"). This happened on the `/btseye ... fill` T6 build and on the lifecycle Eye after its `autorig`
  ended. A player without the rig would lose charges the same way. The tooltip and the questbook already say the
  merger needs the Agent valve.
- After a DENSITY run, the compressor module turns leftover Molten SpaceTime in its hatch into Eternal Singularities
  (72 L each). This is GT's own fluid-only recipe, as on the BHC; known since B-56.
- `oldwindows` reverts only the pane cells (700 / 339). The opened cells are ' ' in the shapes, so StructureLib never
  checks them and a pre-window build forms by construction.

**Not covered.**
- A client: GUIs, NEI, hologram hints, renders and the questbook screens.
- A real Condensate Assembler craft of an upgrade or kit (the bench builds no BEC network).
- Diffraction / Refab runs (they need P4/P5 fluids).
- The Yellowcake Mill's own run: its bench input is not the recipe-map kind. It reported the missing Uraninite
  correctly.


### B-89 Review fixes after the playtest: the Cascade Hall's two output hatches, the upgrade path with the flag off [review, built]

1. **The Cascade Hall now refuses to form with one plain Output Hatch** (`d84b270`; the alternative B-88 fix 1 left
   open).
   - Every cascade recipe makes two fluids (the cut and the tails). A plain Output Hatch holds one, so a hall with one
     hatch formed and then sat at "Not enough fluid output space" with no structure error.
   - `checkMachine` now wants 2 output hatches or 1 ME Output Hatch. Otherwise it reports "The Cascade Hall needs 2
     Output Hatches (one per stream: the cut and the tails) or 1 ME Output Hatch"
     (`beyondthestars.structure.cascade.outputs`).
   - The output-hatch element counts 2, so the projector's survival path fills both cells. With GT's default count of
     1 it placed one hatch and casing in the other cell, and that hall would no longer form.
   - A hall that was already built with one plain hatch stops forming. That takes away no working setup: it could
     never finish a run.
   - Dev server (`fix89`): the projector build is FORMED with 2 hatches. One plain hatch gives NOT FORMED with the new
     error. An ME hatch alone gives FORMED. Two plain hatches run F-11 (70,000 L enriched + 350,000 L tails at 72/126).
2. **The field-generator upgrade path registers whatever `replaceTecTechEyeOfHarmony` says** (`f5637d8`).
   - B-83 registered SL-U00..U48 only with the flag on. But our Eye, its 1:1 compression gate, the tooltip line "Tier
     step: upgrade each generator in place ... + Retrofit Kits" and the Retrofit Frame / Kit items exist with the flag
     off too. With the flag off, the tooltip taught a recipe that did not exist, and every tier step re-crafted 188
     compression generators, each with a Kerr or BHC controller inside.
   - The recipes are derived from GT's own generator recipes and keep whichever controller the pack's recipe holds, so
     they also serve TecTech's Eye. This is the same reasoning as B-26 for SL-P27..P29.
   - Dev server: with the flag off, "40 Condensate Assembler recipes + the Retrofit Frame, 0 skipped", and GT's lookup
     finds all 40 (277 registered / 0 skipped). With the flag on, the same (278 / 0).
   - *Alternative:* keep the gate and make the Eye tooltip line, the questbook paragraph and the kit items follow the
     flag.
3. **Test bench** (`4fa9675`): `firstErrors` clears the hatch lists before its walk, as GT's `checkStructure` does.
   Without the clear, every hatch was added twice and a hatch-count check passed on the doubled list, so the bench
   printed no error for a hall that was not formed.

Also: `fieldsim-golden --check --java` 78 / 78, and `spotlessApply compileJava` clean.

### B-90 Package: the playtest jar `0.24.1-master.235+21c063e24d` [playtest package]

**Context.** Brief items 11c and 22i: one jar with everything merged for you to test, "run locally for now".

**What was built.** `gradlew build --no-daemon` on master `21c063e` (clean tree; spotless + checkstyle pass, nothing to
fix). The jar is `dist-rc2/beyondthestars-0.24.1-master.235+21c063e24d.jar` (1,451,130 bytes), SHA-256
`8fb935af9b6debd8d4e8da9539ea025f210fa87b5e9c906309030fe2d0c77aa0`; `master.196` moved to `dist-rc2/superseded/`.
`dist-rc2/INSTALL.md` is rewritten for it: the local install into the Prism instance "rc - 2 TO THE STARS" first
(back up the world, move the old `beyondthestars-*.jar` out of `mods` to `mods-bak-bts`, one PowerShell block), the
load lines to expect (278 / 0, the 14 H/R gate lines), the config keys including the two questbook keys, the
questbook's auto-install and `/btsquests`, what is new since `master.196` and `master.137`, what is not in it (P4, the
UXV parts / Empyrean circuits; the UMV stopgap ships), known issues, migration from `master.93`, and the dedicated-server
command kept for later (`SERVER_DIR` placeholder, the SSH key with the one-time authorise command).
A playtest guide page (checklist A1..H2 with expected results and what to report, the chapters, a command card and
section 1's open questions) went to my local work folder; it is not published.

**Judgement calls.**

1. **The questbook overlay's `srcHead` is left at `f8b6297`.** `gen-singularity --check` reports the overlay
   "stale", but regenerating it changes only that one field (checked: the diff is the `srcHead` line), the bundle id
   is the same, and committing it would make the overlay stale against the new commit again. B-82 item 5 already
   calls this harmless. *Alternative:* teach `--check` to ignore `srcHead`.
2. **No new dev-server boot for the package.** The jar's code is what B-88 / B-89 ran (`21c063e` is docs only on top of
   the B-89 fixes, which were booted). The playtest guide covers what only a client can show.
3. **The instance holds `master.93`** (read from its `mods` folder; nothing in the instance was touched), so INSTALL.md's
   migration section leads with the `master.93` path (Kerr arcs converting, Eyes starting dark) rather than the
   `master.196` one.
4. **The bench commands and the render harness stay in the jar** (open question 14).

**Suggestion.** Accept as is.

### B-91 `/btsrecipes dump`: every recipe we add, rewrite or remove, from the live maps [follow-up, built]

**Context.** I asked for "all new recipes" in the playtest guide. A list kept by hand drifts, so the list is
read from the server after load.

**What was built.** `/btsrecipes [status | dump [file]]` (op or console; default file `bts-recipes.json` in the server
folder) writes JSON. Each entry has our id, the line, the badge (`new`, `changed-GT`, `removed`), the machine and recipe
map, items (display name, `registry:meta`, count, nanite tier, "or" alternatives, chance) and fluids (L, condensate
mark), EU/t with the tier name, seconds, special values (Homogeny tier, recipe metadata such as research or coil heat,
the Eye's tier / EU per run / compression baseline), the config flag, and the fallback and placeholder marks with their
reasons. A `changed-GT` entry carries the pack's recipe as it was before our edit. A `removed` entry carries the
removed recipe and what replaces it.

How it knows what is ours: `BTSRecipeGuard.recipe` (the one door every recipe goes through) now notes the size of every
category list of every GT recipe map, of the crafting table and of GT's assembly-line data around each body.
`BTSRecipeLedger` remembers by identity whatever a body added. The dump walks the live maps and asks the ledger, so a
recipe that GT or a load-complete pass dropped is not listed, and ids that left nothing live are reported apart
(`registeredWithoutLiveRecipe`, empty today). The code that edits or removes pack recipes reports to the ledger:
the BHC -> Kerr swap (`SL-K01/swap-NN`), the BHC and EOH controller removals (`SL-K01/removes-BHC`,
`SL-E01/removes-EOH`), SL-G01 (Resonance Chamber II), SL-B01 / B02 (x32) and TecTech's planets converted to our Eye.
`BTSRecipeLedger.gated` marks the flag-gated block (`compressionGateParts`, SL-P39). The H/R gate and the BEC
fallbacks mark placeholders and fallbacks.

*Dev server* (headless `runServer`, fresh flat world `recipedump`, port 25565, two boots):
- Shipped defaults (both flags `true`): `278 registered, 0 skipped` at postInit, 320 with load-complete. The dump has
  **386 entries**: 328 new, 56 changed-GT, 2 removed. By line: Kerr 28, Eye 59, BEC / Condensate 28, Homogeny / Reality
  27, Automation 1, Upgrade path 41, old plant 202, other 0. There are 28 NEI-only display recipes, 14 placeholders
  (the H/R gate lines) and 0 fallbacks.
- Both flags `false`: `277 registered, 0 skipped`, **372 entries** (370 new, 2 changed-GT, 0 removed). Gone are the 10
  BHC -> Kerr swaps, both controller removals, SL-G01 and SL-P33. The 43 planets become `new` copies (TecTech's Eye
  keeps its own). SL-B01 / B02 stay `changed-GT`: they ship whatever the flags say.

The playtest guide (local work folder) has a new "All new recipes" section built from both dumps. It has a search and
filter chips per line and badge, and one collapsible group per line. Changed recipes show the pack's lines, then ours.

**Judgement calls.**

1. **A new command, not a subcommand.** `/btseye` and `/btshr` are machine benches. A recipe dump covers every line, so
   it gets its own `/btsrecipes`. *Alternative:* `/btseye recipes dump`.
2. **Line assignment.** The line is the `BTSRecipeGuard.line` the recipe registered in. The `singularity` line is split:
   SL-P02..P14 (the Kerr's structure parts) go to Kerr, the rest to Eye. Load-complete ids are placed by prefix: SL-U to
   Upgrade path, SL-B to BEC, SL-G to Eye. The BHC swaps are Kerr even when the rewritten recipe is a compression
   generator. *Alternative:* a line argument on every `recipe()` call.
3. **TecTech's 43 planets count as `changed-GT`** with the flag on: our Eye's planet recipe replaces TecTech's, and
   TecTech's is the "before". Their item outputs are identical. Only the run time, the EU and the success figures
   change.
4. **The pack's recipe as it was before is a snapshot** taken just before each in-place edit (`CommandBTSRecipes.snapshot`).
   It is not re-read from the pack, so it is exactly what our code saw.
5. **Cost.** One size snapshot of every recipe map per registered recipe at load, about 320 snapshots and no
   measurable load time. The ledger holds references only to recipes that already exist.

**Suggestion.** Keep. Re-run the dump before each playtest package and rebuild the guide section from it (the builder
script is in my local work folder, `recipedump/build_section.py`).

---

### B-92 Questbook text pass: readable colours, short bold, no odd glyphs, Eye T1: Sol, Endgame Goals folded [follow-up, built]

**Context.** The questbook screenshots (2026-10-13) showed text problems we said we would fix and re-shoot:

- bold text cut off at line ends;
- yellow and light-cyan text that is hard to read on the parchment page;
- the Eye T1 quest still called "An Eye at the End of Time";
- an "Endgame Goals" chapter left with only the two trophies;
- `Φ` and `√` drawn as odd characters;
- two icons (48147 / 48149) that did not resolve on the dev server.

The tidy pass's questbook handoff was also still open.

**What was built.**

1. **Generator rules** (`tools/gen-singularity.js`, "player-text rules"). Every text a player reads in the book is
   checked: our quests and chapters, the plant chapter read back from `gen-questbook.ps1`, and every text a pack
   edit writes. The build fails, naming the quest, on:
   - `§e` / `§b` / `§a`;
   - a bold run over 16 visible characters (names are exempt: they are one line and never wrapped);
   - any of `√ Φ ⌊ ⌋ ☉`.

   The `{sqrt}`, `{Phi}`, `{lfloor}` / `{rfloor}` and `{sun}` tokens are gone. Four new `--selftest` cases prove
   each rule fires.
2. **Colours**, measured against the page colour in a client screenshot (#FBF3DF, contrast ratio):

   | Was | Ratio | Now | Ratio | Used for |
   |---|---|---|---|---|
   | `§e` yellow | 0.96 | `§5` dark purple | 5.8 | headings (BUILD, RUN, DO THIS ...) |
   | `§b` aqua | 1.11 | `§9` blue | 4.6 | highlights |
   | `§a` green | 1.20 | `§2` dark green | 2.8 | DONE WHEN |

   Quest names that were `§b` are now `§3` dark aqua, the pack's own name colour, which also reads on the dark map
   tooltip. This covers both sources: `singularity.json` (58 / 217 / 25 tokens) and the plant chapter
   (100 / 381 / 24).
3. **Bold.**
   - In the ladder: 7 runs shortened by hand. Examples: "The tier gate:" before the compression sentence, "Coming
     later." in the planned note, "Universium:" in the Tier 13 warning (its text is otherwise unchanged, ruling h).
   - In the plant chapter: 91 long headings keep their colour and capitals but lose the bold.
4. **Glyphs.**
   - `floor(sqrt M)` replaces the floor-bracket form in the Kerr parallel formula (3 places).
   - "flux charge" replaces "flux Φ charge" (the Quasar placeholder).
   - "1.44 SM" replaces "1.44 M☉".
   - No tooltip or lang line used any of these glyphs (checked).
5. **Eye T1: Sol.** E10 gets a `setName` (`§c§l§n`, the style of Eye T2-T4). The fallback quest 48109 has the same
   name. The quest ids are kept, and the drift check still matches the pack's original name.
6. **Endgame Goals folded.**
   - E63 / E64 also `move` the two trophies into The Endgame at 1008,276 / 1068,276, under "What, How?". They still
     swap in our Eye controller (QB-7).
   - A new pack chapter edit **C01** (`packChapterEdits`, op `hideWhenEmpty`) takes the emptied chapter out of the
     book.
   - The installer saves the chapter's `writeToNBT` and its order index in the state file (`hiddenLines`). Every
     install first puts it back, then hides it again only if it is still empty.
   - `uninstall` (edits or all) and pack edits off put it back where it was. `status` shows it.
   - The cheap check notices a default load that brought it back.
   - The generator fails if the edits-on layout leaves the chapter with any quest.
7. **Icons 48147 / 48149: unchanged, correctly.** The RC-2 pack's `eternalsingularity-1.4.3.jar` (read-only)
   registers `combined_singularity` with 16 metas, 0-15: `CommonProxy` builds `new CompoundSingularityItem(16)`.
   - :10 is the Arithmetic Singularity and :15 the Chronic Singularity.
   - The pack's own quests use both (Eternity uses :15).
   - Only the dev server lacks the mod. `/btsquests verify` still lists those two as its only misses.
8. **Tidy handoff, the rest.**
   - **Already in the plant chapter:** the RPM rule (176 x P x timed/8, 0.5 %/tick, redline 12,000; TURBINE T5/T6
     to 13,500 / 15,000), the RPM-squared coolant drain and the interlock, the dynamo and laser ceiling (32,768 vs
     the ZPM lasers), the rod ladder and the half-rate fallback, residual swirl as heat, Build Tier never re-tiers,
     decay heat on the same water ladder (worst rung first), the Switchyard soft cap and its flag, the tower
     effects, the tiers 5-7 unlocks, the slug window, `/btsmigrate`, and the Vent Stack's halved windows.
   - **Added:**
     - "Written in Fire" gets a WHAT SURVIVES block: the controller is blast-proof; breaking a fuelled Reactor
       Fuel Pillar Frame drops its rods; over-injection explodes the plant like any GT generator, and
       `/btssafety on` / `off` is the safe test mode.
     - The module paragraph says a module socket's hatch serves only that module.
   - **Glossary:**
     - "WHAT UNIT ONE IS" becomes "WHAT IT IS".
     - "Blueprint: Turbine Generator" becomes "Blueprint: Rotor Assembly" (same id 48018; the file stem follows).
     - "throttle cap N/24" becomes "rods up to N/24".
     - "campaign power" becomes "rods N/24".
     - "rotor gallery + output manifold" becomes "rotor assembly", in the text and in `BTSBom.GALLERY`'s name
       (`/btskit` prints it).
     - "pillar frames" becomes "Reactor Fuel Pillar Frames".
     - Units: "60s" becomes "60 s", "1,024t" becomes "1,024 ticks", and "%/t" becomes "%/tick".
     - The diagrams (`gen-diagrams.ps1`) now say NUCLEAR PLANT - OVERVIEW, FOUNDATION and REACTOR SILO instead of
       UNIT ONE / MAIN STATION / STEAM GENERATOR.
     - The Gas Centrifuge Cascade, Fuel Fabrication Line and Reprocessing Canyon names were already gone from the
       book.

**Judgement calls.**

- **Not gold / dark aqua.** The task suggested gold (§6) and dark aqua (§3). Against the measured page, gold is
  1.73 : 1 and dark aqua 2.59 : 1, both still low. Dark purple (the pack's most-used quest colour) and blue reach
  5.8 and 4.6. Gold stays where we already used it: chapter cross-references and gold quest names. *Alternative:*
  §1 dark blue (12 : 1) for highlights if blue reads too close to the title colour.
- **The 16-character limit.** At GUI scale 4 on 1080p the text box is about 40 characters wide. Each bold glyph
  adds one unit, and the cut was seen with a 37-character run. 16 characters keeps the worst-case overflow to about
  two letters, inside the usual space left at a wrap.
- **With `replaceTecTechEyeOfHarmony` off**, E63 / E64 are off too, so the trophies stay, unedited, in Endgame Goals
  and the chapter stays (with E20 / E37, 4 quests). The move is gated with the swap rather than split into a second
  edit, because one quest carries one edit.
- **Plant step labels kept.** The F-xx / R-xx / C-xx step labels stay in the plant chapter's fuel-line quests. They
  number the chain there; the glossary's "no design ids" rule was scoped to tooltips and lang.

**Checks.**

- `gen-bom.ps1` (the one build command) and `gen-diagrams.ps1` ran clean.
- `gen-singularity --check` and `--selftest` pass (20 cases).
- `spotlessApply compileJava` passes.
- Headless dev server, RC-2 pack DefaultQuests, fresh world `qbtext`, boot 1 with both flags on:
  - Auto-install off, then `install`: "62 applied, 0 skipped ... 1 emptied pack chapter(s) hidden". `status` is all
    ok and lists "C01 Endgame Goals". `verify` resolves 113 / 115; the two misses are the eternalsingularity icons
    above.
  - The book: 49 chapters. The Endgame holds the trophies at 1008,276 / 1068,276, and E10 is named "Eye T1: Sol".
  - Second `install`: a no-op, with a byte-identical DB. `install force`: the same database.
  - `uninstall edits`: all 3,813 pack quests, the 46 pack chapters (Endgame Goals back, with its 31 entries) and the
    pack's chapter order are identical to the pre-install snapshot.
  - `install` again: identical to the first install.
  - `uninstall all`: the whole database (quests, chapters, entries, order) is identical to the pre-install snapshot.
- Boot 2, both flags off: `install` (clears the sticky uninstall) applies 38, switches 24 off, and keeps Endgame Goals
  ("still holds 4 quest(s)"). The fallbacks 48103 / 48109 are shown.
- Boot 3, both flags on again, auto-install on (this dev server's BetterQuesting reloads its default book on every
  start): "config changed", then 62 applied and the chapter hidden. The database is identical to the first install,
  and the progress of our quests came back from the backup.
- **Not re-shot.** The client screenshots are the next unit (questbook-shot client, port 25601).

---

### B-93 Questbook re-shoot (round 2): what the shots showed, wrap-safe text, the plant diagrams in the jar [follow-up, built]

**Context.** You asked to see the questbook with our changes; round 1 (B-83 on the questbook branch) found cut bold
text, unreadable colours, odd glyphs, the pack Eye name and a near-empty Endgame Goals, and B-92 fixed them in the
sources. This round shoots the book again from master and checks the fixes. Page: local work folder
`qbshots/round2/questbook-shots.html` (one file, 6.7 MB, lossless WebP; round-1 sliders where the same shot exists).

**How.** The `QuestbookShots` harness was not on master (the questbook merge took the branch before its last two
commits), so it is cherry-picked (0bf0c2c; dev-only, `-Dbts.qbshots=true`). Dev client from master, RC-2
DefaultQuests copied into `run/client/config/betterquesting`, a fresh creative world each pass: auto-install "77
quests added, 62 edits applied, 1 emptied pack chapter hidden"; `/btsquests verify` 113 / 115. 181 shots per pass: the
chapter list (top and end, pack-only and reinstalled), the UXV / MAX / Endgame / UIV / UMV / plant / How to Generate
Power maps with hovers, every quest of ours (44), every pack quest we edit with text or a moved trophy (33), the whole
plant chapter (35 quests, scrolled pages included), and the RC-2 "before" side after `uninstall edits` / `uninstall
all`. Three passes; every shot of the last pass was read (pixel diffs against the second pass picked out what the
last fixes changed).

**What the shots showed, and what was done.**

1. **Bold text was still cut** ("needs ever[y]" in Eye T1, "an[d]" after MASS INFUSION and "escro[w]" in Eye T2/T3,
   the pack's own "a maximum of thr[ee]" in T2 Exo-Foundry). B-92's 16-character limit could not fix it: BetterQuesting
   3.8.89-GTNH wraps with `java.text.BreakIterator` and measures each segment on its own (`RenderUtils.splitString`:
   width of the line so far + width of the next word), so only the first word of a bold run is measured bold. Every
   later bold glyph overflows by one pixel. **Fix:** `wrapSafe()` in `tools/gen-singularity.js` (every description;
   titles stay as typed, they never wrap) and the same steps as `WrapSafe` in `gen-questbook.ps1` (the plant chapter)
   re-open bold after each space inside a bold run. The text draws the same. E05 also re-bolds "Eternal Magnetic
   Chassis" word by word with a second replaceText, so the pack's own cut line is whole in our edited quest.
2. **Numbers broke at a line end** ("1." / "51 x 10^11 EU" in Eye T1). A colour code glued to a number ("{9}1.51")
   makes BreakIterator break after the number's first '.' or ','; "(1,920" did the same, and ")§r." let a full stop
   start the next line (Blueprint: Reactor Silo). **Fix:** wrapSafe moves a code group that sits between a space or
   '(' and a digit in front of it, and one between ')' and punctuation after the punctuation.
3. **Low-contrast colours.** B-92's yellow / aqua / green were gone, but the plant chapter still had 76 gold key lines
   (1.73 : 1 on the parchment) and one light-grey line (2.0 : 1), our text used gold for chapter names, and the
   Primitive / Stable / Advanced tier names were light purple (2.37 : 1). **Fix:** gold body text is dark blue `{1}`
   (12 : 1), grey is dark grey `{8}`, the tier names dark purple; the Gallifreyan rainbow swaps its gold and light
   purple letters. Titles keep gold / dark aqua / light red.
4. **The plant chapter's diagrams were missing-texture squares** (Step 3, Step 4, Step 9, Blueprint: Rotor
   Assembly). Since D2 the jar installs the whole book, but the `btsquests` image domain only existed in
   `questbook/resources` for the manual merge into `config/betterquesting/resources`. **Fix:** the three PNGs ship in
   the jar (`assets/btsquests/textures/gui`; a mod jar serves every `assets/` domain it holds), and `gen-diagrams.ps1`
   writes that copy too.
5. **Confirmed fixed (B-92 and the merges):** "Eye T1: Sol" on map, tooltip and page; the trophies in The Endgame and
   Endgame Goals hidden (back on uninstall); no odd glyphs ("floor(sqrt M)", "flux charge"; Θ Ω π ± × ² → ≥ and the
   superscript exponents draw); the rewritten Tier 13 (UXV) warning; the Kerr module quests' icons and task items
   (master registers MTE 28117/28118); the Homogeny and Homogeny T2 are real quests.
6. **Dev client only (not bugs):** Ergomatter / Superdense Ergomatter icons (`eternalsingularity:combined_singularity:15
   / :10`, which the RC-2 pack registers, B-92), and every red question mark / "Item Placeholder" on a pack quest or
   chapter icon (mods the dev client does not load).

**The generator now rejects** (lint over every player text, the plant chapter read back included): text that wrapSafe
would still change, and gold / grey / light purple in a description, next to B-92's rules. Selftest: "gold in a
description" plus six wrapSafe cases (each output exact, idempotent, and the lint fires on the raw input). `--check`
and `--selftest` pass; the plant text from PowerShell passes the JS lint, so the two implementations agree.

**Judgement calls.**

1. Kept at 2.6-2.8 : 1 and readable in every shot: dark green DONE WHEN (B-92's choice), light red quest references and
   warnings, and BetterQuesting's own `[note]` style (dark aqua, used across the whole pack). The threshold is 2.5 : 1.
2. **Open: "+0.5" can still break after the point.** Java's line breaker splits a plus-signed decimal ("+0." / "5")
   with no code involved; 6 places (Eye T4-T6 trims). Rare and width-dependent; I left the text as it is. Suggestion:
   accept, or write those trims without the plus sign. Open question 20.
3. **Open: Reality Diffraction, Reality Refab and the Homogeny T3 still show "(planned)"** although the Reality machines
   are built: `realityRefab` is off because their inputs (White Hole Matter, Superdense Ergomatter) come only from
   P4/P5. Suggestion: keep the checkboxes, and give those three a placeholder note that says the machine is built but
   its inputs come later. Open question 19.
4. The harness is cherry-picked rather than re-written; the round-1 B-83 doc entry of the questbook branch is not
   copied (B-83 on master is the upgrade path), this entry replaces it.
5. The "field line" -> "Lock bar" sentence (GUI branch, B-96 there) is not applied: master's Eye GUI still has the field
   line. It belongs with the GUI merge.

**Not done.** The playtest jar is not re-packaged (dist-rc2 untouched), so master.235 has neither B-92 nor this.
Trophy progress across the move was not re-tested with players (B-92 note).

---

### B-94 Follow-up test on master: static checks, boot, recipe dump, questbook install / uninstall [follow-up, test]

**What was run.** Master `d82a7ea` (the `/btsrecipes` dump, the questbook text pass and the round-2 re-shoot fixes on
top of the B-90 package), then `f4a119b` (the one fix below), which is what the dev server ran.

Static checks:
- `spotlessApply compileJava`: passes, and spotless changed nothing.
- The lang lint (215 lookups, against our `en_US.lang` and RC-2 GT's): 0 missing and 0 duplicate keys. The one false
  positive and the one key with no producer are the same as in B-88 (`fieldRetrofitKit`, `star_unit5`).
- `node tools/fieldsim-golden.js --check --java`: 78 / 78 PASS.
- `gen-singularity --selftest`: passes.
- `gen-singularity --check`: **failed** ("stale: overlay.json"), see fix 1. Passes after it.

Headless `runServer` (RC-2 deps, port 25565), five boots:
- **Config:** the shipped defaults (`replaceTecTechEyeOfHarmony` and `removeBlackHoleCompressorRecipe` true, pack edits
  on).
- **Book:** the RC-2 pack's DefaultQuests (identical to the Prism instance's), with BetterQuesting's "Load the default
  quest DB on world startup" false, as the pack ships it.
- **Worlds:** `fu-qb` (auto-install off on its first boot, two fake players with progress) and `fu-fresh` (auto-install
  on, the player's path).
- The dev server's config, level name and BetterQuesting setting are put back afterwards. The two flags are left at
  their shipped `true`; the recipe-dump run had left them `false`.

| Area | Result |
|---|---|
| Load (all five boots) | `278 registered, 0 skipped`. The 14 H/R gate lines (SL-C02, C03, C04, R11, R13, R16, R17, H20, R01, R02, H21, R03, R04, R05) and the summary "14 Homogeny/Reality recipe(s) ... unreachable until P4/P5". BEC line: 6 condensates, no legacy fallbacks. SL-B01 / B02: 4 -> 32. SL-G01: the Resonance Chamber II rewrite. 10 BHC -> Kerr swaps. TecTech's Eye: recipes removed (storage 43, NEI 43) and the controller removed; 43 planets converted. Upgrade path: 40 + the Retrofit Frame, 0 skipped. No `[beyondthestars]` / `[BTS]` WARN or ERROR and no `com.nateyburr` frame in any log. All as in B-88. |
| `/btsrecipes` | `status` and `dump fu-recipes.json` both report 386 entries: 328 new, 56 changed-GT, 2 removed. By line: Kerr 28, Eye 59, BEC 28, H/R 27, Automation 1, old plant 202, upgrade path 41. 28 display-only, 0 fallbacks, 14 placeholders, and `registeredWithoutLiveRecipe` is empty. These match B-91 and the boot log: the guard counts 320 registered = 278 at postInit + 42 at load-complete, 0 skipped. The 14 placeholders are exactly the 14 gate ids. Changed-GT 56 = 10 swaps + 43 planets + SL-G01 + SL-B01 + SL-B02. Removed 2 = the BHC and EOH controllers. Upgrade path 41 = 40 + the frame. |
| Questbook, world with progress | Below. |
| Questbook, fresh world | Below. |

**Questbook, world with progress** (`fu-qb`, compared as whole databases ignoring JSON key order). Before the install,
`status` showed "never installed" and every chapter missing. Then fake progress was added:
- a1: the BHC, the EOH, Stargate Ring Block, T5: Superb and quest 191;
- a2: Star Fuel, both trophies (Simulating a Solar System, An Offering To The Gods) and the BHC.

| Step | Result |
|---|---|
| `install` | 77 quests added, 62 / 62 edits, "1 emptied pack chapter(s) hidden". Every completion is kept: the BHC and EOH show as Kerr Ergosphere Engine and Eye T1: Sol, and a2's two trophies stay complete after their move into The Endgame (the trophy check B-92 / B-93 left open). `status` is all ok. `verify` resolves 113 / 115; the 2 misses are the eternalsingularity icons, as before. Our 48000 / 48114 were then completed for a1. |
| Structure check | 0 errors: the chapter order after UMV is UXV, MAX, The Endgame; the plant chapter is last; Endgame Goals is gone and The Endgame holds the trophies. |
| `install` again | No-op ("already matches the jar"); the database is byte-identical. |
| `install force` | 77 refreshed; the same database (only the key order differs). |
| `uninstall edits` | All 3,813 pack quests are identical to the pre-install snapshot. The fallbacks 48103 / 48109 appear. **Endgame Goals is back** with its 31 entries, and the pack chapters' relative order is identical (only our own chapters sit in between). The trophies show as complete for a2. |
| `install` again | Identical to the first install. |
| `uninstall all` | The whole database (quests, chapters, entries, chapter order, Endgame Goals at its old slot 16) is identical to the pre-install snapshot. |
| Restart, auto-install on | "an op ran /btsquests uninstall all ... leaves it alone" (sticky); the database is still identical to the pre-install snapshot. `install`: "progress on 77 re-created quest(s) of ours came back from the progress backup", identical to the first install, a1's 48000 / 48114 complete again. |
| Restart | "already up to date"; identical. |

**Questbook, fresh world** (`fu-fresh`, auto-install on):
- First start: "no install state in this world yet", then 77 added, 62 applied and the chapter hidden. `status` is all
  ok and `verify` resolves 113 / 115. The database is identical to the `fu-qb` install.
- Restart with BetterQuesting's default load on world start = true (BetterQuesting's own default, which reloads the
  pack's book on every start): "chapter ... missing", re-applied, our progress came back from the backup, and the
  chapter was hidden again. The database is identical.

**Fixed here** (its own commit).
1. **`gen-singularity --check` could never pass on a committed tree** (`f4a119b`).
   - The overlay records the commit it was generated from (`srcHead`). After any later commit, `--check` called the
     overlay stale and exited 1, even when nothing else differed (B-82 item 5, B-90 call 1, B-92 and B-93 all noted
     it). A real drift in the overlay would have hidden behind it.
   - `--check` now blanks that one field of the overlay before comparing. Any other difference is still stale:
     checked by changing `manifestSha`, which gives exit 1.
   - Now: "95 generated files are up to date (bundle cf6158303e87)".
   - *Alternative:* drop `srcHead` from the bundle. The status command shows it as the bundle's source, so it stays.

**Not covered.** A client (the round-2 shots in B-93 cover the book's look); the playtest jar is still master.235 (the
package unit follows).

### B-95 Review fixes: no pointer to the hidden Endgame Goals tab, a skipped chapter edit is not stale [follow-up, built]

Two review findings on the B-92 / B-94 questbook work. Both were checked before fixing and both were real.

**1. Two pack quests still sent the player to "the Endgame Goals tab"** (`9d5affc`).
- C01 hides the emptied chapter. E40 already rewrote Finally, Universium!, but two more pack quests named the tab and
  had no edit: One Ingot, Infinite Power (`AAAAAAAAAAAAAAAAAAAKcw==`, Tier 9 - UHV: "Check the Endgame Goals tab or NEI
  recipes for the Table") and Gen XXXXXXXXXX... (`AAAAAAAAAAAAAAAAAAAJbA==`, Adept Thaumaturgy: "(see the Endgame
  Goals tab when you're ready)"). Confirmed in the RC-2 pack mirror (identical to the Prism instance's).
- New `replaceText` edits: **E65** "Check the Tier 13 - UXV, Tier 14 - MAX and The Endgame tabs, or NEI recipes for
  the Table"; **E66** "(see The Endgame tab when you're ready)". Plain text, like the pack's own note and E40. Two
  pack chapter keys (`uhv`, `thaum2`) were added for `expectChapter`.
- **The generator now fails** if any pack quest text a player still reads (after our edits) names a chapter that a
  `hideWhenEmpty` edit hides. New selftest case "a pack quest naming a hidden chapter" (drop E66) fires.
- Dev server, fresh world, shipped flags, the pack's DefaultQuests: 77 added, **64 / 64** edits applied, Endgame Goals
  hidden, both texts replaced. The only "Endgame Goals" left anywhere in the database is the UHV quest name "Misc
  Endgame Goals", which names a quest, not the tab. Restart: "already up to date", database identical (ignoring key
  order).

*Judgement call (differs from the review's suggestion):* E65 / E66 are **not** gated on `replaceTecTechEyeOfHarmony`
like E63 / E64. The text points at where the Dire Crafting Table goals and the Stargate now live, and those quests
(E20-E62, apart from E20 / E47) move with no flag. With that flag off, Endgame Goals stays (holding only E20, E47 and
the two trophies), but the Stargate and DCT quests are still in UXV / MAX / The Endgame, so the new text is the right
one in both cases. Gating it would leave the old "see the Endgame Goals tab" pointing at a chapter that no longer
holds the Stargate. *Alternative:* a variant per flag state; not needed while the moves are unconditional.

**2. A skipped chapter edit made the world "stale" on every boot** (`d516361`).
- `staleReason` said "pack chapter ... is empty and still shown" whenever a chapter edit was not in `state.hidden` and
  its chapter was empty. It ignored that `hideEmpty` had skipped it on purpose (the chapter's name drifted, or an op
  this jar does not know). Such a world was never up to date: every start re-ran the whole install (progress snapshot
  and restore) and re-set the "pack edit(s) were skipped" login notice.
- Both now ask one helper, `chapterEditSkip` (unknown op, chapter name drift). `staleReason` calls an empty, shown
  chapter stale only when `hideEmpty` would actually hide it now. This is the review's second option (check name and
  op the same way). It was chosen over "skip edits in `state.skipped`" because it also reacts if the name comes back.
- Dev server, with Endgame Goals renamed "Endgame Goals (renamed)" in the server's copy of the pack's DefaultQuests
  (the quest edits only check the chapter by id, so E63 / E64 still empty it):

| Boot | Jar | Result |
|---|---|---|
| 1 (fresh world) | fixed | 77 added, 64 applied, **C01 skipped: chapter name drift**, chapter left in the book, empty |
| 2 | fixed | "already up to date"; database identical |
| 3 | **pre-fix** installer | "this world differs from the jar: pack chapter AAAAAAAAAAAAAAAAAAAAHQ is empty and still shown", 77 refreshed: the bug |
| 4 | fixed again | "already up to date" |

Every boot: `278 registered, 0 skipped`, the H/R gate summary, and no beyondthestars WARN or ERROR apart from the
expected C01 drift warning in the drift test. The dev server's `server.properties`, BetterQuesting setting
("Load the default quest DB on world startup" back to true) and `cmds.txt` were put back. The DefaultQuests copy was
removed. Test worlds `b95-fresh` and `b95-drift` were left in `run/server`.

**Checks:** `spotlessApply compileJava` and `spotlessCheck` pass; `gen-singularity --selftest` passes (the new case
included); `--check`: 95 files up to date (bundle `4e3fbb04c8d2`). The playtest guide now says 64 edits and names
the two re-pointed quests.

**Not covered.** No client screenshots of the two quests. The text is plain and short, and B-93's wrap rules lint
it. The playtest jar is still master.235 (the package unit follows).

### B-105 Package: the playtest jar `0.24.1-master.250+be66c178aa` [follow-up, playtest package]

*Number:* B-96..B-99 are already taken on the `gui-clean` branch (the controller GUI cleanup), and its review and fix
units may add more. This entry takes B-105 so that the GUI merge needs no renumbering; B-100..B-104 are left for that
branch.

**Context.** The follow-up on master (B-91..B-95) added `/btsrecipes`, the questbook text pass and the review fixes,
but the shipped jar was still `master.235` (B-90). This unit packages them for the playtest ("in the artifact give me
all new recipes too please"; the questbook re-shoot).

**What was built.**
- `gradlew build --no-daemon` on master `be66c17`, clean tree (spotless and checkstyle pass, nothing changed). The jar
  is `dist-rc2/beyondthestars-0.24.1-master.250+be66c178aa.jar`, 1,567,006 bytes, SHA-256
  `a2d2d1200f5f85e0e90110ae831855d1328e9799cc6f7784b169760458abd55a`. `master.235` moved to `dist-rc2/superseded/`.
- Jar contents checked: `CommandBTSRecipes` and `BTSRecipeLedger`, the questbook `overlay.json`, the three plant
  diagrams under `assets/btsquests/textures/gui`, and `mcmod.info` version `0.24.1-master.250+be66c178aa`.
- `dist-rc2/INSTALL.md`: new jar name, size and hash everywhere (header, the local install block, the dedicated-server
  command); a new section 0.1 "Since `master.235`" (recipe list, questbook text pass, Endgame Goals folded with E65 /
  E66 and 64 edits, the installer fix, tools); the old 0.1 / 0.2 become 0.2 / 0.3. Section 3 expects 64 edits plus "1
  emptied pack chapter(s) hidden" and the `/btsrecipes status` line. Section 5 names Eye T1: Sol, adds the hidden
  Endgame Goals row, and describes the text style. Section 9 adds open questions 19 and 20 and the screenshot harness,
  and drops "the questbook screens" from the never-seen-in-a-client list (B-93 shot them in a dev client). Section 10
  adds `/btsrecipes`, and section 11 adds "from `master.235`". The file keeps its CRLF line ends.
- Playtest guide (local work folder `playtest/playtest-guide.html`):
  - the header shows the new jar, hash and commit;
  - a "New since `master.235`" note;
  - A1 names the new jar and the Mods-list version;
  - B1 expects 64 / 64 edits and `C01 Endgame Goals` hidden;
  - B2 says the Endgame Goals chapter is folded into ours, says where its quests went, and asks to check that both
    trophies are in The Endgame;
  - a new phase **R** with step **R1**: run `/btsrecipes status` and `dump`, expect 386, and check five or six entries
    of the "All new recipes" list against NEI (one per line, one changed-GT, one removed);
  - open questions 19 and 20 are added to the table.

  R1 sits outside the generated `RECIPES:BEGIN` / `END` block, so re-running `recipedump/build_section.py` keeps it.
  Headless Edge (Playwright, `msedge` channel) in light, dark, 1280 px and 390 px: 27 steps (R 0 / 1), "386 of 386
  recipes shown", no console or page errors, no broken images, no horizontal overflow, and ticking R1 counts.

**Judgement calls.**

1. **No new dev-server boot or recipe re-dump.** Between the dump the guide was built from (`master.236`, B-91) and
   `be66c17`, the only Java changes are the questbook installer, the screenshot harness (dev only) and one display
   name in `BTSBom`; no recipe code changed. B-94 re-dumped on master and got the same 386. B-95 booted the questbook
   changes. The guide's recipe note says so.
2. **The guide keeps its tick storage key** (`bts-playtest-master235`), so ticks you already made survive. The "New
   since" note asks you to re-check A1, A2, B1 and B2.
3. **The Prism instance was not touched.** Its `mods` folder still holds `master.93` (read only), so INSTALL.md keeps
   the `master.93` path first and says that a `master.235` jar is moved out the same way.
4. **The dev-only questbook screenshot harness ships**, inert without `-Dbts.qbshots=true`, next to the render
   harness. It is listed with open question 14.

**Suggestion.** Install `master.250` instead of `master.235` and run the guide from A1.

---

### B-106 Automation built for real: two photo rigs in a dev client, and what they taught [automation photos, built]

*Numbered B-72 on the `autoshots` branch (master's review fixes already had B-72); B-106 since the merge (see B-107).*

**What was asked.** "Since you have an automation proven, can you make me an artifact with photos of it so I can see
an example?" The earlier proof (B-70) was scripted. This one is real blocks in a real client
(`-Dbts.rendertest=true -Dbts.rendertest.cases=automation`, dev-only harness `rendertest/AutoRig*`), and the only
actor on the machines is the wiring: the harness builds, reads the state once a second into `eye.tsv` / `kerr.tsv`,
and moves the camera. Page: local work folder `autoshots/automation-example.html` (not published).

**Rig A, Eye T1 (Sol), redstone and GT covers.** Kit-built Eye, Crude field generators (the offline proof's ts = tt =
1), empty buffers. Directive Hatches in five Sensor sockets: Trims Bit 1 (Helium valve), Trims Bit 2 (Hydrogen valve),
Status Bit 1 (both upkeep valves, through a 48-cell redstone staircase with 3 repeater landings and two ground lines
with 5 and 6 repeaters), Trims Any (a torch NOT gate lights an "in band" lamp), Transition Code (a comparator and a
lamp). Each valve is a GT Debug Fluid Tank (the endless supply) with a Machine Controller cover (enable with redstone)
and a Fluid Regulator cover set to Conditional: HV 500 L/t on the trims (Sol's rig rate, 10 units/s), UV 150,000 L/t on
the upkeep. Four Stellar Ignition Charges in the plinth bus. **Result (run 4):** the upkeep valves stocked the
ignition in 202 s, the Eye lit itself (Sol lit at 243 s) and burned 866 s (14 cycles): 837 s Resonant (96.7 %), 29 s
Stable, 0 worse, Integrity 100 throughout, mean Y 1.242 (the offline proof: 96.6 %, 1.242). Helium valve open 28 s
(280,000 L), Hydrogen 1 s (the first lit second). Status bit 1 re-opened the upkeep valves 6 times as the buffers ran
down. Star runtime test rig: off the whole time.

**Rig B, Kerr T1, AE2.** Kit-built Kerr, every bolt bus emptied, hole closed. Directive Hatches in the two upper front
utility sockets: Hole Bit 3 (bolts short, window 1 minute) and Hole Bit 1 (closed). In front of each bolt bus an AE2
cable block with an export bus (redstone card, active with signal; the bolt bus one with 3 acceleration cards) and a
quartz facade on top; dust on the facade in front of the hatch powers it. ME: creative-energy controller, drive (64k
cell, 4,096 SpaceTime bolts, 4 Seeds, inserted once the camera was ready), interface, glass cable, a level emitter
(bolts >= 512) with a lamp. **Result:** Seed out, hole open 2 s after the cell went in, 16.1 SM a second later; open
1,002 s, 0 evaporations, mass 16.00-16.25 SM, bolt stock 57-121, bit 3 on 41 s, 1,088 bolts exported.

**What failed on the way, and what changed.**
1. *My wiring:* the 1.7.10 diode direction bits were reversed in the harness (direction 2 outputs south), so every
   repeater faced backwards; the wire dump showed the first landing stuck. Fixed in the harness.
2. *My wiring:* a step-up dust beside the export bus's cable block lit its lamp but not the cable block (dust powers
   only the way it flows). Redesign: the hatch above the bus, a facade on the cable block, dust on the facade.
3. *My rig:* one acceleration card (8 bolts a move) could not feed the hole's climb from 8 to 16 SM (bleed 1 SM/s at
   8 SM); it evaporated twice. Three cards (64 a move) hold it. The cell also got only 64 bolts at first: GT's stack
   helper caps an ItemStack at 64; the AE stack now carries the count.
4. *Real bug, fixed:* the Eye's GUI head line stayed "The star is dark" while Sol burned. On GT 5.09.54 recipe checks
   are event-driven, and a star changes state with no hatch event. The Eye now schedules a check when the star state,
   a transition or the unlocked tier changes (`refreshStatusLine`).
5. *Real bug, fixed:* the Directive Hatch GUI drew its four bit lines and its drive line under the player inventory
   (ten lines in a standard hatch panel). `DirectiveHatchGui.getBasePanelHeight` adds 64 px.
6. *Docs, fixed:* automation.md said the regulator is "enable with signal". RC-2's Fluid Regulator has no redstone
   mode; Conditional follows the block's work flag, so a GT valve is two covers. automation.md (§2.1, §4, new §4.1,
   the questbook text) and the Directive Hatch tooltip (two lines) now say so.

**Seen, not fixed.** The KEE GUI shows a missing texture (magenta and black) in one slot of its right-hand button
column (runs 3 and 4).

**Judgement calls (please confirm or change).**
- Trims window left at the default 10: P peaks just over +10 for one second each cycle (Stable), as the offline proof
  does. A window of 9 would keep every second Resonant; I left the documented default.
- Kerr bolt window 1 minute (default 10): at 16 SM the hole burns a SpaceTime bolt every 4 s, so w = 10 would keep
  about 600-660 bolts in the bus and w = 1 keeps 60-120 (the log: 57-121). Either way the bit fires about once a
  minute, because each export moves 64 bolts. I used 1 so the bus starts near empty and the refills show; both work.
- The upkeep valve rate (3 M L/s) is a choice: fast enough to stock the 600 M L ignition in about 3 minutes and to
  refill a cycle (50 M L) in about 17 s.
- The ME cell went into the drive when the camera was ready (a harness action, logged); everything after it was the
  wiring.
- Not built: the OpenComputers computer (optional in the brief).

**Not covered.** Tiers above T1, events (T3 flash, T6 Agent), transitions, Kerr T2, real supply chains (endless tanks,
creative ME power), OpenComputers in a world.

---

### B-107 Merging autoshots into master: the photo-rig fixes and the video recorder [merge]

**What was done.** The `autoshots` branch (worktree `addon-autoshots`, branched at `b4b1ad1`, the B-71 commit) is merged
into master as one merge commit. It brings:

- **Three fixes found by the automation photo run.** The Directive Hatch GUI is 64 px taller so its bit and drive lines
  sit above the player inventory (`a98e2f9`); the Eye GUI's head line no longer says "The star is dark" while a star
  burns: `MTEEyeOfHarmonyBTS.refreshStatusLine` pushes a recipe check whenever the star state, a transition or the
  unlocked tier changes (`cfb2001`); a GT valve on a Directive bit is a Machine Controller cover plus a Conditional
  Fluid Regulator cover, in `automation.md` (§2.1, §4, new §4.1), the questbook text and the Directive Hatch tooltip
  (`9899edd`).
- **The photo-rig harness** (`rendertest/AutoRig*`, `-Dbts.rendertest=true -Dbts.rendertest.cases=automation`) and
  its write-up, B-106.
- **The dev-only video recorder** (`rendertest/RecClient`, `RecScript`, `RecOverlay`, `RecServer`, `FrameRecorder`,
  `-Dbts.rec=true`), the AE2 Eye rig (`AutoRigEyeAE`, Rig C) and the shared ME placement helpers (`MeParts`), the shot
  scripts in `tools/recording/` and `docs/recording.md`.

**Conflicts (two files, both resolved by keeping both sides).**

- `ClientProxy`: master had added the questbook screenshot harness (`-Dbts.qbshots`) after the render harness; the
  branch replaced the render-harness line with "recorder, else render harness". Kept the branch's two lines and
  master's questbook-harness line after them, so each dev harness keeps its own switch.
- `singularity-ladder-questions.md`: both sides added an entry numbered B-72 at the same place. Master's B-72 (review
  fixes after the dev-server test) and B-73 stay as they are; the branch's "Automation built for real" entry is now
  **B-106**, moved to the end of Part B with a renumbering note. The one citation of it (`automation.md` §4.1) now
  says B-106. Master's other B-72 citations (the UMV stopgap, the chart pickup in `automation.md` §2) are master's
  entry and were left alone. Branch commit messages keep the old number.

**Auto-merged, checked by hand.** `MTEEyeOfHarmonyBTS` (the status-line fix on top of master's Eye: the hook sits
after `publishDirectives()` in the once-a-second block), `DirectiveHatchGui` (the taller panel beside master's
backwards Set button), `MTEHatchDirective` (two tooltip lines), `RenderTest` (`fly` and `loadAndKeep` package-private),
`RenderTestClient` (hands over to the photo tour), `automation.md`. The `gui-clean` branch rewrites the Eye and
Directive Hatch GUIs later; when it merges, keep the status-line push (`refreshStatusLine`) whatever the GUI's head
line becomes, and check the Directive Hatch's height in the new layout (the old one needed the 64 px).

**Doc touch-ups.** `docs/recording.md` runs from the addon root instead of the `addon-autoshots` worktree, and its
questbook limit now says the overlay on master is the whole book (the branch's was one chapter).

**Judgement calls (please confirm or change).**

1. **Numbered B-106 / B-107, not B-100.** The task said "B-100 onward", but master's B-105 (written after that
   instruction) leaves B-100..B-104 for the `gui-clean` branch's review and fix units, so this merge takes the next
   number after B-105 and the GUI merge still needs no renumbering.
2. **The recorder ships in the jar, inert.** Like the render and questbook harnesses, the recorder's classes are in
   `rendertest/` and load only with `-Dbts.rec=true` (the client proxy's check); open question 14 now names it. ffmpeg
   is not in the repo: the recorder takes its path from `-Dbts.rec.ffmpeg`. `run/` (and so `run/recordings/`) is
   git-ignored; no video or binary was committed.
3. **The autoshots worktree's two "modified" docs were not committed.** `git status` there lists
   `docs/automation.md` and `docs/singularity-ladder-questions.md` as modified, but both files are byte-identical to
   the branch head (`git diff` is empty, the blob hashes match `9899edd` / `7a2ff13`'s content): a stale index stat,
   nothing to commit.

**Open work from the branch.** (a) Rig C showed that an ME stocking input hatch in an Eye gas socket drains the whole
network into the Eye's buffers (cap 2^62 L), which starves trim buses on the same network: cap the buffers' intake
(recommended: about 20 cycles of upkeep) and say so in the Eye's AE2 docs; until then the rig uses a separate trim
network and LV Field hatches (`docs/recording.md`, Limits). (b) B-106's "Seen, not fixed": a missing texture in the
KEE GUI's right-hand button column (check after the GUI merge). (c) The final showcase recording needs the new GUIs
and the H/R and BEC scenes on master (`tools/recording/showcase.shots` has a TODO card for them).

**Merge fix (no textual conflict).** Master's tidy (`03e7d0d`, B-87) moved the kits' identical `socketRels` into
`BTSShapes.socketRels`; the branch's `AutoRig` and `AutoRigEyeAE` still called `EyeKit.socketRels` /
`KerrKit.socketRels` (seven calls), so they now call `BTSShapes.socketRels`. `spotlessApply` also reformatted the
branch's later files (`AutoRigEyeAE`, `MeParts`, `RecClient`, `RecServer`; formatting only).

**Checks.** `spotlessApply compileJava` passes on the merged tree. No in-game run was part of the merge; the
recorder and photo rigs are next exercised by the showcase recording on master.

### B-108 The Eye's buffer intake cap: one ME stocking hatch no longer drains the network [B-107 open work (a), built]

**The problem.** An ME stocking input hatch in an Eye gas socket shows the Eye up to 2.1 G L of each fluid a second,
and the buffers took everything (their only cap was 2^62 L). One hatch emptied a network of fluid cells into the Eye
within seconds, and anything else on that network (the AE2 video rig's trim export buses) found nothing (B-107).

**What was built.** The sockets now fill each buffer only to its **intake cap**, and each pull takes at most the cap
minus what is buffered (`MTEEyeOfHarmonyBTS.intakeCaps`, star side `EyeStarRuntime.intakeCaps`). Per fluid:

- **Each star in play** (the live star, the slot's chart, and the star a dark Eye lights for it: Arcturus for the
  neutron-star relight): max(ignition need, 20 cycles of upkeep) × p. The ignition need is 10 cycles of upkeep plus
  the seed cloud, the same number the dark-state message names. Also the T4 HSM feed for 20 cycles at its maximum
  (1.5 L/s), 2 T5 batch / T6 merger charges and 2 T6 dwarf-matter top-ups. Stars are max-merged, because only one
  burns at a time.
- **Plus each transition the Eye could begin**: the live star's toward the chart (T4-T6 from the chart alone), the
  one running, and the next one once its core is in escrow (Status bit 1's rule). Each adds its event fluids and the
  new star's first cycle (`transitionNeeds`). The Core Collapse also adds the 90,000 L of SpaceTime the bounce can
  absorb.
- **Plus a planet in the slot**: 2 runs of its inputs at full parallel.

A fluid with no cap is not pulled at all; it stays in the hatch or the network. The cap covers every amount the
Eye checks its buffers against: the dark-state checklist, Status bit 1 (`buffersShort`), the transition's `missing`
list and a planet's parallel. So nothing that could start waits on the cap, and those messages are unchanged. The
Field sockets and trims are untouched. **Old worlds:** a buffer above its cap keeps what it holds and is only drawn
down; the sockets pull again once it is below the cap.

**Where the player sees it.** The tooltip says the buffers fill only to the target's intake cap and names the rule.
The scanner's buffer line is now "held / intake cap" per fluid ("not pulled" for a fluid with no cap). OpenComputers
has a new `bts_eye.getIntakeCaps()`. `docs/automation.md` §2.1 / §2.3 explain it and add a short network note.
Even with the cap, a stocking hatch takes up to the cap in its first second (1 G L of each gas for Sol), so keep the
network's stock above the cap, or put the trims on their own network or behind a small Field hatch. The questbook is
unchanged: no quest says the buffers take everything ("Inputs drain into internal buffers every second, so ME stocking
input hatches on the gas sockets work" is still true).

**Dev-server test** (headless `runServer`, fresh flat worlds `intake-b108b` / `intake-b108c`, console; T1 Eye built by
`/btseye t1 3000 100 0 north gen 1`, `/btseye keep`, then the new bench `/btseye star 3000 100 0 intake setup 10`: an
ME stocking hatch (He, H) in the first gas socket, a creative ME controller, a drive with one 10 G L fluid cell per gas,
and a second consumer on the same network, an always-on fluid export bus (H, 3 cards) into a Super Tank):

| Step | Hydrogen buffer / cap | Helium buffer / cap | Network H / He | Super Tank (2nd consumer) | Star |
|---|---|---|---|---|---|
| 8 s after setup (target Sol) | 1,000,000,000 / 1,000,000,000 | 1,000,000,000 / 1,000,000,000 | 8,999,616,000 / 9,000,000,000 | 384,000 L, filling | DARK, no charge |
| charge + 10^18 EU + `rig fine`, 48 s later | 950,000,000 / 1 G | 950,000,000 / 1 G | 8,896,000,000 / 8,900,000,000 | 4,000,000 L (full) | LIT (seed 100 M burned and refilled) |
| one cycle later | 1 G / 1 G | 1 G / 1 G | 8,796,000,000 / 8,800,000,000 | full | LIT (50 M upkeep a cycle, topped back up) |
| one more cycle | 1 G / 1 G | 1 G / 1 G | 8,746,000,000 / 8,750,000,000 | full | LIT |
| `buffer hydrogen 5e12` (an old world) | 5,000,000,000,000 / 1 G, network H unchanged over 12 s | 1 G / 1 G | 8,750,000,000 / 7,750,000,000 | - | LIT |
| `target none` | cap 0 ("not pulled") | cap 0 | unchanged over 6 s | - | put out (target changed) |

So the buffer stops at the cap and the network keeps the rest (8.75 G L of the 10 G L). The Eye still ignites and
burns, refilling to the cap after the seed and each cycle. The second consumer on the same network filled its tank.
Before the cap, the same rig emptied the cells in its first second (B-107). The table's runs are run 2 (old-world and
`target none` rows) and run 3 (the rest; run 2's Super Tank faced the bus with its output side and took nothing, fixed
in the bench). Static: `spotlessApply compileJava`, `node tools/fieldsim-golden.js --check --java` 78 / 78 PASS (the
field package is untouched).

**Judgement calls (please confirm or change).**

1. **The margins: 20 cycles, 2 charges, 2 planet runs.** 20 cycles is the task's recommended number. For Sol at p = 1
   that is 1 G L per gas, against an ignition need of 600 M L. Alternatives: 10 cycles (just the ignition need, so a
   relight right after a loss could wait a few seconds for the refill), or a config value.
2. **The cap follows the target, not the tier.** A fluid the current target (and a core in escrow) does not use is
   not pulled. So Raw Stellar Plasma for the Mass Infusion is pulled once the Stellar Mass Core is in escrow or the
   Sirius A chart is in, which is the order the questbook already gives. An empty slot pulls nothing. The alternative
   is a cap over every star and planet the Eye's tier reaches: simpler to explain, but a T3 Eye would then hold
   planet-sized H / He stocks it may never use.
3. **The stellar sockets are capped too**, by the same rule (RSPM, ESPM, HSM, SpaceTime, WDM / BDM, Agent). The
   task named the gas buffers; the same drain could empty a network of any of these.
4. **No GUI change on master.** The `gui-clean` branch rewrites the Eye GUI. Its merge can show held / cap from
   `intakeCaps()` (section 3).

**Suggestion:** as built.

### B-109 Test of master after the autoshots merge and the intake cap: checks, boot, benches, recorder smoke [integration, test]

**What was run.** Master `bee683f` (the autoshots merge B-107 and the intake cap B-108), then `adcc4f1` (the one fix
below) for the second recorder run.

Static checks:
- `spotlessApply compileJava`: passes, and spotless changed nothing.
- The lang lint (215 lookups, against our `en_US.lang` and RC-2 GT's): 0 missing and 0 duplicate keys. The one false
  positive (`fieldRetrofitKit`) and the one key with no producer (`star_unit5`) are the same as in B-88 and B-94.
- `node tools/fieldsim-golden.js --check --java`: 78 / 78 PASS.
- `gen-singularity --check`: "95 generated files are up to date (bundle 4e3fbb04c8d2)". `--selftest`: passes.

**Headless `runServer`** (RC-2 deps, port 25565, fresh flat world `irt-b109`, the shipped config with both flags true;
the dev server's level name was put back afterwards):

| Area | Result |
|---|---|
| Load | `278 registered, 0 skipped`. The 14 H/R gate lines (SL-C02, C03, C04, R11, R13, R16, R17, H20, R01, R02, H21, R03, R04, R05) and the "14 Homogeny/Reality recipe(s) ... unreachable" summary. BEC line: 6 condensates, no legacy fallbacks. SL-B01 / B02 4 -> 32, SL-G01, 10 BHC -> Kerr swaps, TecTech's Eye removed (43 planets converted), upgrade path 40 + the frame. No `[beyondthestars]` / `[BTS]` WARN or ERROR and no `com.nateyburr` frame. GT's own dev-environment errors are at the same count as the B-94 and B-108 boots (823 "Exception" lines in each log). |
| `/btsrecipes status` | 386 entries: 328 new, 56 changed-GT, 2 removed; by line Kerr 28, Eye 59, BEC 28, H/R 27, Automation 1, old plant 202, upgrade path 41. Unchanged from B-94: neither the merge nor the cap changed a recipe. |
| `/btsquests status` | Our 6 chapters ok (79 quests), auto-install on. The 64 pack edits say "skipped" because this fresh world has no pack book: unlike B-94, the dev server's BetterQuesting was not given the pack's DefaultQuests this time. Not a change. |
| Eye T1 kit (`/btseye t1`) | FORMED, 24 hatches, 374 generators. A creative re-projection keeps it formed; the one-block damage test and the survival repair pass. Scanner buffer line: "Hydrogen 0 / 1,000,000,000 L; Helium 0 / 1,000,000,000 L" (B-108's held / cap). |
| Intake bench (`/btseye star ... intake setup 10`, B-108's server-side rig) | After 8 s: each buffer 1,000,000,000 / cap 1,000,000,000, network H 8,999,616,000 / He 9,000,000,000, and the second consumer's Super Tank filling (384,000 L). After `eu`, `rig fine` and one charge: Sol LIT (Stability 97, Resonant), buffers back at the cap, network 8.85 G L of each, Super Tank full (4,000,000 L). Same as B-108. |
| Kerr T1 kit (`/btskerr`) | KEE, Compressor and Extractor modules FORMED; the re-projection and damage / repair tests pass. |
| Kerr automation bench (`/btskerr autorig ... 30`) | Open 30 s, closed by itself 0 times, mass 16.02 SM against setpoint 16, max deviation 0.25 SM. |

The photo rigs (`AutoRig`, `AutoRigEyeAE`) have no server-side command: the recorder's `build` drives them from the
client. Rig A (`build eye`) was exercised by the smoke test below.

**Recorder smoke test** (master's dev client, `tools/recording/smoke.shots`, the local work folder ffmpeg, nvenc). The
`addon-show` client was running at the same time; no wait was needed.
- Run 1 made the video (24.0 s, 1920x1080, 30 fps, h264), but filmed the Eye orbit at **render distance 4**: the Eye
  was a pale shape in fog with no ground, and the game rendered at 120 fps. The title card and the GUI shot were fine.
  Fixed below.
- Run 2 (`adcc4f1`): `rec.log` says once "game settings came back from options.txt (render distance 4, fps limit 120,
  vsync true): set to 16 / unlimited / off again". `ffmpeg -i`: 24.00 s, 1920x1080, 30 fps, h264 (Main), yuv420p.
  Scenes of 3.00 / 14.00 / 4.00 / 3.00 s, 0 repeated frames, 458-660 fps. Three stills were looked at:
  - the title card at 1.5 s;
  - the Eye T1 orbit at 10 s: sharp, with grass and sky, the right way up, only the caption bar;
  - the Eye GUI at 19.5 s: no hotbar, no crosshair, no NEI panels.

  None is black, none is flipped, and none shows the HUD.

**Fixed here** (its own commit).
1. **The recorder filmed at the client's saved render distance** (`adcc4f1`).
   - `RecClient.setUp` sets render distance 16, an unlimited frame rate and vsync off on the first client tick. Master's
     dev client has an `options.txt`, saved by the questbook harness with render distance 4, maxFps 120 and vsync on.
     Its values came back about 20 ticks later, before the world was made. `autoshots` had no `options.txt`, so the
     defaults (12 chunks) hid the problem there.
   - `RecClient.holdSettings` now sets the three again every client tick and logs once what it found. Run 2 above is
     the check. `docs/recording.md` item 1 says so.
   - *Alternative:* delete or rewrite `run/client/options.txt` before a run. The questbook harness would write it
     again on its next run, so the recorder holds the values itself.

**Not changed (noted).** The photo harnesses `RenderTestClient` and `AutoRigClient` set render distance 16 the same
way, so on master's dev client they probably get the saved 4 back too. They are not part of the video path and were
not run here. If they are used again, give them the same per-tick hold (section 3).

**Not covered.** The AE2 Eye rig (`build eyeae`, Rig C) and the Kerr rig (`build kerr`) in a client, and the full
`showcase.shots`; the final recording on master runs both.

---

### B-110 Merging p4-quasar into master: conflicts, the recipe ledger, the intake cap at T7 / T8 [merge, built]

**What was done.** `git merge --no-ff p4-quasar` into master `af88fdf` (merge `dd38989`), then four fix commits. The
merge commit's message lists every conflict and how it was resolved. In short:

- **Structures.** Master's window bands live only in the exporter, and the design repo's `voxel.js` (`1b407b3`)
  already has P4's geometry, so the merged `tools/export-structures.js` (master's `WINDOW` code plus P4's legends,
  pieces and manifest section) re-exported everything. `eye_core` is master's windowed core plus P4's 4 Directive
  ('d') and 1 slot-bus ('p') plinth sockets; the Kerr shapes are master's. `--check` is clean against voxel sha
  `fa6fec2a751c`.
- **Code.** Everything else was additive and both sides were kept: proxies, config, fluids (P4 owns Ergomatter and
  Superdense Ergomatter), the MTE ledger, the commands, the kits, the Sensor Hatch (master's chunk-save on every
  setter plus P4's quasar channels), the Eye NBT and the KEE. One compile fix: P4's kits called `socketRels` and
  `worldPos` helpers that master had moved to `BTSShapes` / `SLConsoleTest`.
- **Docs.** In the costs audit, master's window section stays §13 and P4's quasar section is §14. In this file, P4's
  entries are Part E. P4's text cites only master's numbers, so nothing needed renumbering.

**Fixes after the merge.**

1. **`/btsrecipes`** (`3325bbc`). The `quasar` line is its own group, "Quasar / Conduit" (34 entries). SL-P40
   (Resonance Chamber IV, the Eye T8 gate) is filed under "Eye" with the other chamber recipes and carries the
   `replaceTecTechEyeOfHarmony=true` badge. SL-Q26 records its two rewrites of pack recipes as changed-GT, with the
   recipe as it was before: GT++'s Temporal Harmony Catalyst and the coremod's Timepiece. SL-Q05 (the optional
   MagMatter coil) is marked as a placeholder (P4-24).
2. **The intake cap at T7 / T8** (`cf375ec`, `1a2c080`). B-108's cap pulls only the fluids it lists, so after the
   merge several of P4's needs would never have been pulled. Added to the cap:
   - Triangulum's Magnetar Matter upkeep (7.5 L a cycle);
   - the T8 Ergomatter feed (at its 10 L/s maximum);
   - the planets-mode variant of a galaxy chart;
   - the companions (20 of their cycles each, added on top of the galaxy's own share);
   - the slot-bus planets in planets mode (2 runs each);
   - every step of the galaxy chain still ahead: the Magnetar Merger that a dark Eye starts from, the Magnetar
     Collapse, and the Galactic Merger. Without this the Agent for the re-merge was never pulled.

   The tooltip says so.
3. **Questbook data** (`50f44fd`). `gen-singularity --check` failed because the bill table had no rows for the
   Eye's new socket roles. Rows added; bundle `d01088fe8d76`.

**Checks.**
- Static: `spotlessApply compileJava` passes. Golden tests: fieldsim 78 / 78, quasar 38 / 38. `export-structures
  --check`, `gen-sl-textures --check` (126) and `gen-singularity --check` / `--selftest` all pass. Lang: no duplicate
  keys, and every `recipe_result` id has its key.
- Headless `runServer` (port 25571, fresh flat world `p4m-b110`; the dev server's level name and port were put back
  afterwards):

| Area | Result |
|---|---|
| Load | 311 registered, 0 skipped (master: 278). "P4 recipes: every machine recipe is registered". SL-G01, the upgrade path (40 + frame), and SL-Q26 (the catalyst and the Timepiece, 1 recipe each). 8 H/R gate lines, all White Hole Matter (14 before: the Ergomatter, Superdense and Eternity gates are gone). No `[BTS]` warnings or errors apart from the known "pack edit skipped" lines (no pack book in a dev world), and no `com.nateyburr` frame. |
| `/btsrecipes status` | 425 entries: 365 new, 58 changed-GT, 2 removed. By line: Kerr 28, Eye 64, BEC 28, H/R 27, Automation 1, old plant 202, upgrade path 41, Quasar / Conduit 34. The dump has SL-B16, SL-Q01..Q28, and SL-P40 under Eye with its flag. |
| Eye T1 kit with `oldwindows` | FORMED, re-projection, damage and repair pass. With the 339 pre-window blocks put back it still forms. The kit puts deck in the 'd' / 'p' cells, so this is also an Eye from before P4. |
| Kerr T1 kit with `oldwindows` | KEE and both modules FORMED. 700 cells reverted, still FORMED. |
| Eye T7 kit | FORMED (52 hatches, 8 Receptor Ports, slot bus). Re-projection, damage and repair pass. Caps as in item 2 above, for galaxy mode with and without companions, planets mode, and the dark chain to Triangulum and to the Milky Way. |
| Quasar T1 kit | FORMED. Re-projection, damage and repair pass. |

**Not covered.** No client was run, so the new GUIs, renders, NEI pages and holograms were not seen. No real burst
link was run: P4's own test (P4-83..P4-94) covered that on the branch.

**Questions.**
1. **Resonance Chamber IV has two recipes in NEI.** For chamber II you chose to rewrite GT's recipe in place (B-29).
   SL-P40 is still a separate recipe beside GT's MHDCSM one. *Suggestion:* rewrite GT's chamber IV in place under the
   same flag, as SL-G01 does, and decide it together with chamber III (open question 4).
2. **The T7 / T8 caps are large.** A T7 galaxy with a Sol companion buffers 32 G L of hydrogen and helium, and three
   slot-bus planets in planets mode can reach about 336 G L. That is what B-108's "20 cycles / 2 runs" rule gives at
   those parallels. *Suggestion:* keep the rule. Shorten the window for companions if one Eye still starves a shared
   network in a playtest.

### B-111 The P4 quests: the contract turned into the book [merge-p4, built + test]

**What was built** (`04f121a`, docs `b9a53c2`). Every quest in `docs/p4-questbook-contract.md` ("Generator entries",
plus the Conduit, Eye T7 / T8 and Hosts sections that correct them) is now in `questbook/singularity.json`:

- **Features on:** `quasar`, `conduit`, `crown`, `eyeT7T8` and `eternityOnErgomatter`. `uxvParts`, `eyeT9T10`,
  `realityRefab` and `maxParts` stay off.
- **The placeholders became real under the same ids**, with the contract's tasks and text:
  - 48144 Quasar Engine: the controller and the quasar bill;
  - 48145 Einstein-Rosen Conduit: 2 mouths, a mirror, a junction and the mouth bill;
  - 48146 Eye T7: Triangulum: 1,000 L Ergomatter;
  - 48147 Ergomatter: 16,000 L;
  - 48148 Eye T8: Milky Way: 1,000 L Superdense Ergomatter;
  - 48149 Superdense Ergomatter: 16,000 L;
  - 48150 Burst Thermaliser Crown: the controller and the Mk I bill.
- **21 new UXV quests, 48154-48174:** A Seed for a Quasar, Disc Field Coils, Reading the Disc, Burst Control,
  Throats, A Quasar Link, The Stability Rail, Stability Link, Magnetar Collapse, The Shard Halo, Heavy Link, Galactic
  Merger, Superdense Ergomatter the Slow Way, A Quasar-Linked Kerr, A Quasar-Held Vortex, Planets Mode, Companions,
  Automating the Quasar, A Galaxy That Runs Itself, Scripting a Quasar, Bursts into Power.
- **48194 Riding the Rift** (MAX) is in too. It stays a planned checkbox while `realityRefab` is off.
- All the new ids are appended to the id ledger.
- **Bill tables** `quasar`, `crown` and `conduit`: the contract's rows. The Eye's rail and halo rows came with B-110.
- **Text rules (B-92 / B-93).** The contract was written before those rules reached its branch, so every text was
  rewritten:
  - `{5}` headings, `{9}` highlights, `{c}` quest names, `{2}` DONE WHEN;
  - "the flux" for Φ;
  - σ, λ, Λ, β and κ stay (`{u+03c3}` ...). The GUI names its gauges that way, and they are not on the odd-glyph
    list.

  Every gate quest has a DONE WHEN line. Where its item gets installed, the line says HOLD → CLAIM → INSTALL.
- **Two contract details corrected against the code:**
  - upkeep is paid while a throat is fitted (`MTEConduitElement.upkeep`: "no throat, no upkeep");
  - the rail needs at least one port on each bank at T7 (the Eye tooltip).
- **Other quests touched:**
  - Eye T6's note now points at the Stability Link. It said "both coming in a later update".
  - The Homogeny T2 / T3 and Reality Diffraction quests get the contract's appended lines.
  - Stability Field said "a binary Directive Hatch and OpenComputers support are coming in a later update". That has
    been stale since the automation pass; it now says the Directive Hatch and `bts_eye` exist.
- **Pack edits.**
  - E20 (Tier 13): the Quasar paragraph replaces "come in a later update". The Universium warning now says the
    Homogeny Tier II Core and every Quasar and Conduit part take the UMV stopgap. It also says nothing of ours up to
    Eye T8 needs the pack's UXV parts, since P4 uses none.
  - E23 / E24: "which comes in a later update" goes. E24's MHDCSM warning becomes a note on SL-P40.
  - E43 Eternity: a `replaceText` swaps the pack's "You need 20,736 L of Universium for the first 5 attempts and a
    catalyst." for SL-Q26's Ergomatter catalyst (9,216 L) and Timepiece (4,608 L).
- **Layout.** The contract's positions clashed with master's map: it did not have E23 / E24 or the Eternity stage.
  - The quasar's own quests sit above the main row, and the links and milestones below it.
  - The products and hosts sit under those.
  - Eye T7 moves to 744 and Eye T8 to 936, with E23 / E24 above them.
  - The Eternity stage goes under Ergomatter.

  The tidiness check passes on all three layouts. One hover-only line (48159 → 48168) crosses two icons. That is a
  warning, as several pack lines are.

**Judgement calls.**

1. **Eye T7 / T8 prerequisites.** "Keep the prerequisites" and the contract's chain conflict: the contract makes T7
   need the Magnetar Collapse, and T8 the Galactic Merger.
   - Built: T7 = [48162, E23] and T8 = [48165, E24].
   - The old direct prerequisites 48145 / 48146 are still required, through the chain (48162 ← 48161 ← 48160 ← 48159 ←
     48145). A direct line would cross the collapse icon.
   - Both quests were placeholders, so no completion depended on them.
   - Likewise, "The Stability Rail" lists only 48159 (48141 comes through the quasar).
   - "A Galaxy That Runs Itself" needs the rail, not the link. The contract's text says the rail, but its JSON said
     the link, and that line crossed the collapse.

   *Alternative:* keep the direct prerequisites as hover-only lines.
2. **E43-E46 now need `replaceTecTechEyeOfHarmony`.**
   - SL-Q26 (Eternity on Ergomatter) runs only with that flag.
   - With the flag off, the old edits would still have put Eternity behind our Ergomatter quest, while the pack's
     recipe wants Universium from TecTech's Eye.
   - Now, with the flag off, the four stay in UMV as the pack has them (tested, below).
3. **The core quests are notes.** The content unit's "Intergalactic Upgrade Core" and "Supermassive Seed Core" quests
   are not in the generator entries. Their facts became notes in Magnetar Collapse and Galactic Merger, which already
   ask for the cores.
4. **"Scripting a Quasar"** is a normal checkbox: the contract's `opt` field is not a generator field. Its text says it
   is optional.

**Checks.**

- `questbook/gen-bom.ps1` (the one build command): 66 quests of ours (16 planned checkboxes, 2 fallbacks), 64 pack
  edits, bundle `6876d7837407`.
- `gen-singularity --check`: 139 files up to date. `--selftest` passes.
- `spotlessApply compileJava` passes.

**Dev server.** Headless `runServer` on port 25565, four boots.

- The book is the RC-2 pack's DefaultQuests, with BetterQuesting's default load off, as the pack ships it.
- Afterwards, the server's config, level name and `cmds.txt` were put back and the DefaultQuests copy was removed.

| Boot / world | Result |
|---|---|
| 1, `p4qb-up`, **the old bundle**, auto-install off | The old bundle is `c596d51`'s overlay and `BTSQuestIds`, written over the tree's copies for this boot and restored from HEAD after it. The pristine pack DB was snapshotted (3,813 quests). Seeded: player a1 completed Eternity, T7: Perfect, Tier 13 (UXV) and the BHC quest; a2 completed Magnetic Monopole Matter and T8: Tipler. `install` kept all of it. Then a1 completed our 48141 / 48143 (real) and 48144 / 48146 (planned boxes); a2 completed 48113 (real) and 48145 / 48150 (planned boxes). |
| 2, `p4qb-up`, **this bundle**, auto-install on | "differs from the jar: bundle d01088fe8d76 -> 6876d7837407": 22 added, 77 refreshed, 64 / 64 edits, Endgame Goals hidden. "is built now: its planned checkbox was cleared" for 48144-48150. a1 kept Eternity, T7: Perfect, Tier 13, the KEE quest, 48141 and 48143; 48144 / 48146 were cleared. a2 kept MMM, Tipler and 48113; 48145 / 48150 were cleared. Eternity sits in UXV at 744,432 after Stellar Catalyst and 48147, with the Ergomatter text. T7 is at 744,156 after E23 + 48162, and T8 at 936,144 after E24 + 48165. `verify`: 140 / 142 ids resolve; the misses are the two eternalsingularity icons, missing only on the dev server, as before. |
| 2, continued | a1 then completed 48154 and 48161. `install` again: "already matches the jar", byte-identical DB. `uninstall edits`: all 3,813 pack quests identical to the pristine snapshot; the pack chapters identical apart from their order numbers and our grafted entries, in the same relative order; Endgame Goals back. `install`: identical to before (semantic compare). `uninstall all`: the whole DB (quests, chapters, entries, order) identical to the pristine snapshot. `install`: "progress on 99 re-created quest(s) of ours came back from the progress backup", byte-identical to the first install. |
| 3, `p4qb-fresh`, auto-install on | "no install state in this world yet": 99 added, 64 applied, chapter hidden; `verify` 140 / 142. The DB is identical to boot 2's install. |
| 4, `p4qb-off`, `replaceTecTechEyeOfHarmony=false` | 42 applied, 22 switched off ("replaceTecTechEyeOfHarmony is off"), E43-E46 among them: Eternity, MMM, Perfected Components and The SUCC stay in UMV with the pack's text and prerequisites. Endgame Goals stays ("still holds 4 quest(s)"). |

Every boot: 311 registered / 0 skipped (309 with the flag off), no `[BTS]` / beyondthestars WARN or ERROR, and no
`com.nateyburr` frame.

**Seen, not changed.** After `uninstall all`, a2's progress file still listed 48113 as complete, although the quest was
gone.
- BetterQuesting only rewrites a player's progress file when that player is marked dirty.
- It ignores an entry for a missing quest on load.
- Our install puts the same completion back from the progress backup anyway.

So this does not affect restoring the pack: the database is identical.

**Not covered.**
- No client was run, so the map and the new pages were not seen. The round-2 harness (`-Dbts.qbshots=true`) can shoot
  them.
- The P4 machines were not run against the quest tasks in a world.

### B-112 Restart persistence on master's machines (P4-85) and the older casings' tooltips (P4-86) [merge-p4, built + test]

**Context.** GT marks a chunk for saving only when an inventory changes, and Minecraft saves only marked chunks. P4
found that its machines could come back from an old save after a restart (P4-85), and left the same check for
master's machines. P4's lang lint also found casings with a name but no tooltip (P4-86, rule 14).

**Built (persistence, `a7d1e83`).** The P4 helper: one `IGregTechTileEntity.markDirty()`, which only sets the chunk's
modified flag (no block update).

| Machine | What changes with no inventory change | Now |
|---|---|---|
| KEE | hole mass, setpoint, open state, bolt governor, Auto, module list | already once a second since B-71; re-tested |
| Kerr modules | the run's progress and the refinery's link need (a module keeps nothing else) | once a second while a run is in progress |
| Eye of Harmony | buffers filled from the sockets (also while dark), field, milestones, setpoints, transitions | once a second after the field second; the runtime already marked most of its own changes, not a dark Eye's buffer fill |
| Homogeny | install progress, milestones, pending mix totals, burst counters | once a second (the install run also marked every `SAVE_EVERY_TICKS`) |
| Reality Diffraction / Refab | pending litres and run count; steps, sub-ticks and link | once a second, in `MTERealityBase` |
| Burst Control Hatch | its mode (GUI, setter, a host adopting its default) | marks on a change |
| Directive Hatch, Stellar Field Sensor Hatch | settings | already mark on a change since B-71 |

**Built (tooltips, `2442c5c`).** slCasing 1-8, 10-13 and 15 and slCasing2 0-2 each get a
`tile.beyondthestars.<registry>.<meta>.tooltip` in the slCasing3 style: which machine uses the block, where in it, and
where it is made (machine and how many a craft). The injector rails also say that the lowest rail caps the bolt feed
(Mk I 4, Mk II 8, Mk III 16 a tick). The two legacy module cores say no structure uses them and that they craft into
the module controller. **Also fixed:** P4's Casimir Plate tooltip (slCasing:14) was written over three physical lines.
A `.lang` entry is one line, so the tooltip stopped after its first sentence and the other two lines were junk. They
are now joined with `\n`. Lang lint after: every listed meta has exactly one tooltip, no line without `=`, no
duplicate keys.

**Bench (`b5357f4`).** `/btsconduit nbt x y z [key ...]` prints what the GT machine at x y z would save now. It loads
the chunk from disk first, so straight after a restart it shows what the save held. `/btsconduit burstmode x y z
[place|<mode>]` places a lone Burst Control Hatch and sets its mode the way the GUI does.

**Test.** Headless `runServer` on port 25565, fresh flat world `b112-final`, four boots. The scripts are in the
local work folder (`b112f/`). One Eye T1 (intake setup), one KEE T1 with a 64 SM hole and its compressor module
running an 8,250-tick recipe, a Homogeny T1 mixing, a Reality Diffraction and a lone Burst Control Hatch, each in its own
chunks. Before each probe, the changes were made with no inventory change in the controllers' chunks.

| Step | Change made | Read back |
|---|---|---|
| Boot 2, S0 | Homogeny milestones 1,234 / 567, Diffraction pending 777 L x6, setpoint 64, Eye buffers set to 0, then `save-all` | the Eye's buffers refill from the sockets to 1 G L each within a second |
| Boot 2, the next 60 s | Burst mode PURGE -> FIELD, KEE setpoint 64 -> 48 (OpenComputers `setSetpoint`) | P1 before `stop`: buffers 1 G L each, setpoint 48, mass 63.09 SM, module 4,295 / 8,250 |
| **Boot 3, from disk (clean stop)** | none | buffers 1 G L each, setpoint 48, mass 63.01, module 4,376, milestones 1,234 / 567, pending 777 x6, mode FIELD: **all kept** |
| Boot 3, the next 75 s | mode -> SPLIT, setpoint -> 40, buffers zeroed and refilled; at least one autosave | P3 before the kill: setpoint 40, mass 60.84 SM, module 6,974, mode SPLIT |
| Boot 3 | **the server JVM killed** (`taskkill /F` on our own runServer process, found by its port and command line) | |
| **Boot 4, from disk (after the kill)** | none | setpoint 40, mode SPLIT, buffers 1 G L each, milestones and pending kept, module 6,802, mass 60.97: **the last autosave**, about 170 ticks before the kill, as Minecraft does after a crash |

Every boot: 311 registered / 0 skipped, no `[BTS]` / beyondthestars WARN or ERROR beyond the dev server's usual
questbook "pack quest is not in this world's quest database" lines and P4-87's FML chunk-ticket note, no
`com.nateyburr` frame.

**Without the fix.** The run cut off before this one used the same harness on master without the fix (world
`b112-base`). After the clean stop, the Burst Control Hatch came back in PURGE (it was set to FIELD 60 s before the
stop), and the Eye's buffers came back empty (they had refilled to 1 G L each after the save). The Homogeny, the
Diffraction and the module kept their state in that run, because something else in their chunks happened to change.
Without the fix that was luck.

**Judgement calls.**
1. The controllers mark once a second whether formed or not, as P4's machines do; the cost is one flag on a chunk
   that is already loaded. The modules mark only while a run is in progress (an idle module has nothing to save).
2. The hatches mark on a change, as asked; they have no state that moves by itself.

**Not covered.** No client run. The Refab's own state was not driven: it needs P4's fluids and a Condensate Assembler
link. It goes through the same `MTERealityBase` second as the Diffraction, which was tested.

### B-113 Full RC-2 dev-server test of master after the P4 merge [merge-p4, test]

**What was run.** Master `d233074`: the P4 merge (B-110), the P4 quests (B-111) and the restart fix (B-112). Headless
`runServer` on port 25565, fresh flat world `b113`, two boots (the second after a clean stop). The shipped config was
used: `replaceTecTechEyeOfHarmony` and `removeBlackHoleCompressorRecipe` both true, pack edits and auto-install on. The
book was the RC-2 pack's DefaultQuests, copied read-only from the `rc - 2 TO THE STARS` instance, with
BetterQuesting's "Load the default quest DB on world startup" set to false, as the pack ships it. Afterwards the dev
server's level name, BetterQuesting setting and `cmds.txt` were put back and the DefaultQuests copy was removed. The
scripts are in my local work folder (`b113/`). No code needed changing; nothing failed except the harness slip below.

**Static checks.**
- `spotlessApply compileJava`: passes, and spotless changed nothing.
- Golden tests: `fieldsim-golden --check --java` 78 / 78, `quasar-golden --check --java` 38 / 38.
- `export-structures --check`: matches voxel.js `fa6fec2a751c`. `gen-sl-textures --check`: 126 / 126.
- `gen-singularity --check`: 139 files up to date (bundle `6876d7837407`). `--selftest` passes.
- Lang lint (master's, 241 lookups, against our `en_US.lang` and RC-2 GT's): 0 real misses and 0 duplicate keys.
  - The one reported miss is the known false positive `item.beyondthestars.fieldRetrofitKit.name`: the kit names
    itself per meta, from `beyondthestars.eohTier.1..8`, which all exist.
  - The seven `recipe_result` keys with no static producer are dynamic: `star_unit5` as before, and the Eye's
    `tier7` / `tier8`, `compression7` / `8` and `locked7` / `8`, which are built as `eye.tier` + n and so on.
  - P4's own lint also lists `kerr.module.linked` (`Link.LINKED` is never returned as a failure) and
    `gg.recipe.precise_assembler` (GoodGenerator's map, not ours).

**Load (both boots).**
- `311 registered, 0 skipped`. That is master's 278 plus P4's 33.
- "P4 recipes: every machine recipe is registered".
- 8 H/R gate lines plus the summary. All of them wait for White Hole Matter (Eye T9, P5). None waits for Eternity:
  SL-Q26's swap is on, so the Eternity gate lines are gone.
- Logged as expected: SL-Q26 (the Temporal Harmony Catalyst and the Timepiece, one recipe each), SL-G01, SL-B01 /
  B02 (4 -> 32), 10 BHC -> Kerr swaps, TecTech's Eye removed (43 planets converted), the upgrade path (40 + the frame),
  the BEC line, and the 12 research items.
- OpenComputers lists the P4 components.
- No `[BTS]` / beyondthestars WARN or ERROR, and no `com.nateyburr` frame. The only exception is boot 2's FML note
  that our persistent chunk tickets are dropped (P4-87). The 823 "Exception" lines are GT's dev environment: the same
  count as B-109.

**Recipes and gates.**
- `/btsrecipes status`: 425 entries: 365 new, 58 changed-GT, 2 removed. By line: Kerr 28, Eye 64, BEC 28, H/R 27,
  Automation 1, old plant 202, upgrade path 41, Quasar / Conduit 34. Unchanged from B-110.
- `/btsrecipes dump`: 353 recipes registered (the display pages included), and `registeredWithoutLiveRecipe` is
  empty: every one of them is found in its map. The P4 recipes are all there: SL-B16, SL-Q01..Q26, the display pages
  SL-Q27 / Q28, and SL-P40 under Eye with its `replaceTecTechEyeOfHarmony=true` badge. SL-Q05 is the one P4
  placeholder (P4-24).
- `/btseye gates`: all 17 compression-generator recipes, T1..T9, are CLEAN. P4 audit: 0 of 27 parts break the rule.
  BHC controller consumers left: 0; Kerr controller consumers: 12. **0 loops.**

**Questbook.**
- First start (fresh world): "no install state in this world yet". The install added 99 quests and applied 64 of 64
  pack edits, and hid one emptied chapter (Endgame Goals).
- `/btsquests status`: all ok. The plant 34 / 34, UIV 3 / 3, UMV 10 / 10, Tier 13 (UXV) 35 / 35, MAX 15 / 15, The
  Endgame 1 / 1. 16 planned checkboxes.
- `verify`: 140 / 142 ids resolve. The two misses are the `eternalsingularity` icons, which are missing only on the dev
  server, as in B-111.
- `install` again: "already matches the jar". After the restart, `status` was still all ok.

**Structures.** Every one FORMED. Each kit then ran a creative re-projection over the machine (still FORMED, nothing
overwritten) and a damage-and-repair check (NOT FORMED with the cell named, then the survival path put the cells back
and the machine FORMED again).

| Machine | Creative | Survival (blocks placed) |
|---|---|---|
| Quasar, coils T1 / T2 / T3 / T4 | N (T1), E (T2), N (T3) | S (T3), W (T4): 51,983 each |
| Wormhole Mouth, Conduit Mirror, Conduit Junction | E, W | N, S (mirror 1,702) |
| Crown Mk I / II / III | E, W each | N, S each |
| Eye T7 | W | E (65,989) |
| Eye T8 | N | S (69,698) |
| Eye T6 | E, with `legacy oldwindows` (no D-49 / P4 hatches, pre-window blocks) | S (50,557) |
| Eye T1 | N with `oldwindows`: 339 pre-window cells put back, still FORMED | |
| KEE T1 + both modules | N with `oldwindows`: 700 cells put back, still FORMED | |
| KEE T2 + both modules | W with `oldwindows`: still FORMED | S (KEE 25,051, compressor 964, extractor 498) |
| Homogeny T3 / T2 | N (T3) | E (T3, 41,599), W (T2, 28,597) |
| Reality Diffraction | S | W (53,004) |
| Reality Refab | | N (65,186) |

The Eye T7 / T8 survival counts are lower than P4-87's (67,366 / 71,075). That is master's window bands: the cells
behind each pane are open. `export-structures --check` is clean.

**Eye T6 -> T7 -> T8 unattended** (`/btsquasar autorig … eye …`). The rig sees only the Directive codes and acts only
through the hatches.
- **Set-up.** A fresh coils-T3 quasar at 10000 100 4000, seeded at 64 SM with no spin, at f 1.0. The Eye T8 kit at
  20000 100 2000 with the Magnetar lit, T6 unlocked, milestones 6 / 7 reset, buses empty and the debug rig off. Both
  were kept chunk-loaded. The Eye got 40 × 10^18 EU (see the slip below).
- **T6 -> T7.** The Stability Link was held. The Magnetar Collapse was COMPLETE in 779 s (P4's run: 779 s), and the
  Intergalactic Upgrade Core was consumed. T7 at t+12,831 s, 0 failures.
- **T7 -> T8.** The quasar grew by itself from 80 to about 160 SM, and the Heavy Link was held. The Galactic Merger
  was COMPLETE in 1,920 s (P4's run: 1,920 s), and the Supermassive Seed Core was consumed. T8 at t+70,882 s, 0
  failures.
- **T8 held for 40,000 s.** The quasar grew from 160 to 252 SM. 0 failures, 0 blow-outs, Resonant 74 % of the time,
  and satellite crossings were caught. At the end: Milky Way LIT, Stability 94 (Resonant), Integrity 100, upkeep
  r 1.00, 24 Bu/t on the rail (the Mk I throats' cap).

*The harness slip (first attempt, not counted).* The first chain run used the bench's `fill`, which grants 10^18 EU
once. The Milky Way costs 2.47 × 10^15 EU a 60 s cycle, so that lasts about 405 cycles (6.75 h of game time). The EU
ran out at T8. The upkeep went to r 0 ("SHORT: P sags"), P fell to −100, and the Milky Way was lost; the re-merge that
followed failed too. After `eu`, the next cycle read r 1.00. This is the machine working as designed, with an
unpowered network. The run above was made from scratch with enough EU.

**The intake cap at T7 / T8** (each buffer's held / intake cap, as the scanner shows it):

| Eye | Caps |
|---|---|
| T6 unlocked, dark, Triangulum chart, galaxy mode | Agent 303,000, WDM 73,728, BDM 18,432, Magnetar Matter 168 L |
| … with Sol and the magnetar on the slot bus | H and He 32,000,000,000 L each are added |
| … planets mode, slots Ma / Ow / DD | H and He 336,000,000,000 L each; WDM 55,296 L |
| T7 unlocked, dark, Milky Way chart | Ergomatter 27,300, Agent 606,000, WDM 147,456, BDM 36,864 L |
| T7, Triangulum LIT | Magnetar Matter 168, WDM 73,728, BDM 18,432 L; Ergomatter and Agent not pulled |
| T8 unlocked, magnetar re-merge on the way to the Milky Way | Ergomatter 36,300, Agent 1,212,000, WDM 294,912, BDM 73,728, Magnetar Matter 315 L |

The numbers match B-110's where both list them.

**Conduit and crowns** (`/btsconduit rig 32000 100 0`). All elements opened after 10 s of SpaceTime upkeep.
- The top jet goes to Crown Mk I: 1.21 Bu/t, 1.211 × 10^13 EU/t onto wireless.
- The bottom jet goes Mouth A -> Mouth B -> Mirror 1 -> Junction. The Junction splits it 0.54 / 0.54 / 0.12. The
  first share goes through Mirror 2 to the remote crown's receptor (linked): 5.45 × 10^12 EU/t.
- The bench owner's wireless network held 8.29 × 10^15 EU after about 30 s.

**Hosts** (`/btsconduit inject` and `/btshr bench … inject`, from the real coils-T3 quasar):

| Host | σ | Effect |
|---|---|---|
| KEE T2 | 2.00 | bleed × 0.25, cap 768; `setSetpoint 700` returns 700. Before the link it returned 512 |
| KEE T2, injection off | 0 | "Setpoint 700 SM is held at 512 SM: it needs a quasar link at σ ≥ 1", bleed × 1 |
| Homogeny T3 (3 ports) | 1.50 | speed × 1.50, Entanglement Medium open |
| Homogeny T3 | 0.50 | speed × 1.25, the Medium closed |
| Reality Diffraction (6 ports) | 0.50 | speed × 0.50 |
| Reality Diffraction | 0.25 | "regions UNSTABLE, runs pause below σ 0.5" |

`/btsquasar ocloop`: setFeed 9 -> 1.5, setField 99 -> 3 (the coil tier), trim clamped to 10 units and taken the next
second, and a second call in the same tick refused.

**Restart.**
- The state was changed after a `save-all`, with no inventory change:
  - Homogeny milestones 2,222 / 333;
  - Diffraction pending 1,665 L × 6;
  - the Junction's c 0.6 and out-3 share 0.15;
  - the KEE setpoint 500 (through OpenComputers);
  - the quasar's feed 1.2.
- The quasar, the mouths and the crowns kept moving by themselves.
- Then a clean `stop`, boot 2, and `/btsconduit nbt` straight after the start.
- **All of it was kept.** Those values above; the KEE at 700.2 SM; the quasar at 254 SM with its eruption count; the
  mouth's SpaceTime used counter; and the crown's lifetime EU, which went on from where it stopped.
- The Eye kept T8 unlocked. The dormant quasar stayed dormant.
- After its chunks were force-loaded again, the conduit rig carried on: the remote crown was linked and making
  5.7 × 10^12 EU/t.

**Seen, not changed.**
1. **Bench chunk tickets.** A structure's chunks are not force-loaded once the kit is done. The first `seed` on a new
   quasar said "No Quasar Engine controller" until `/btsquasar keep`. Without the rig, the lit Eye drifted to a breach
   within about 30 s (Θ +102). After a restart every forced chunk is released (P4-87), so the quasar reads FROZEN until
   it is kept again. These are bench facts; a player's chunk loader does the same job.
2. **The EU limit.** `fill` / `eu` grant 10^18 EU each, which a T8 Eye burns in about 6.75 h of game time. A long
   unattended bench run at T8 needs several `eu` grants. The Eye's own readout names the cause ("Upkeep r 0.00
   (SHORT …)").

**Not covered.**
- No client was run, so these were not seen: the P4 GUIs (they wait for the gui-clean merge and its GUI kit), the
  renders, NEI, the holograms and the new quest pages.
- The JVM-kill test was not repeated for P4's machines. B-112 ran it for master's machines.
- The Reality Refab's runtime state.
- An OpenComputers Adapter in a world.

### B-114 The Eye T7 / T8 front window: the stability rail's clamp in front of the galaxy [merge-p4, review fix]

**The finding (confirmed).** The merge (B-110) kept the Eye's window sightline check at T1-T6. At T7 and T8, P4's
stability rail hangs on four clamps from the rim, and the one at 90° (Bastion Plate, piece `eye_rail`) roots in the
rim's window opening, dead centre in front of the object: at \|x\| ≤ 1, z 71-77 from the Eye's centre it filled the
whole 5-row band. A player in front of a formed T7 / T8 Eye saw a strut exactly over Triangulum's small core and the
Milky Way's bulge (ruling 22d: "Glass window bands", the object reads from the front). Running the check to T8 on the
merged exporter reported `FAIL Eye T7/T8 ... blocked: B@-1,-1,71 B@0,-1,71 B@1,-1,71 B@-1,1,75 B@0,1,76 B@1,1,75`.

**Fix** (`tools/export-structures.js`, `windowEye`). The rail's clamp cells (kind bastion, no tag: voxel.js's
`clamp()` clears it) are treated like the T4-T6 arm and rim cells in front of the core's pane: open (`' '`, anything)
at \|y\| ≤ 2, z > 0, \|x\| ≤ 10. The strut now leaves the rim's lower lip and resumes above the band, the same cut the
rim itself already has there. voxel.js is not edited.
- 42 cells opened. `eye_rail`: Bastion 9,996 → 9,954, the piece 15,432 → 15,390 blocks. Its box, offset and sockets
  (the 8 GRB Receptor Ports, 4 high / 4 low) are unchanged; the other pieces are byte-identical.
- The sightline check runs T1-T8 (`eye_rail` / `eye_halo` included): Eye T7 and T8 are clear at y ±1, \|x\| ≤ 8.
  Two new checks: `eye_rail` opens cells and has no pane; `eye_halo` has no window cells (its shards stand clear of
  the band). The manifest's `windows.open` lists `eye_rail` 42 and `eye_halo` 0.
- The questbook was rebuilt with `questbook/gen-bom.ps1`: The Stability Rail (48160) bills and names 9,954 Bastion
  Plates. Bundle `534881cb57c8`.
- The build spec's window paragraph and the Eye's `A` legend row say so.

**Not changed, and asked (open question 23).** The later-tier opening stays \|x\| ≤ 10. The T7 / T8 galaxies reach
r 16 / 19, so from the front at eye level their outer arms stand behind the T6 rim and the arm blades (\|x\| 11-22).
Widening the opening at T7 / T8 alone is not possible: those cells belong to `eye_arms` / `eye_arms2` / `eye_rim`,
which a T4-T6 Eye has too, so a wider opening would cut the arms and the rim at every tier. The galaxy disc is nearly
flat (8°), so from eye level it is a thin streak anyway; from above, or from the side, the whole disc reads.

**Checks.**
- `export-structures --check`: matches voxel.js `fa6fec2a751c`, every sanity check ok.
- `gen-singularity --check` (139 files, bundle `534881cb57c8`) and `--selftest` pass.
- `spotlessApply compileJava` passes. No Java changed, so the goldens were not re-run.

**Dev server** (headless `runServer`, port 25565, fresh flat world `b114`, two boots).
- Boot 1, the shapes from before the fix (`e8e1240~1`'s `eye_rail.txt` and `manifest.json` put in place for the
  boot): an Eye T8 creative at 0 100 0 facing north and an Eye T7 survival at 3000 100 0 facing east (65,989 blocks
  placed, as B-113) were both FORMED. These are Eyes with the clamp in the window.
- Boot 2, the fixed shapes: both old Eyes `check` FORMED (all bands, the rail and the halo). New builds: Eye T8
  survival facing south, 69,656 blocks placed (B-113: 69,698), and Eye T7 survival facing east, 65,947 (65,989): 42
  fewer each. Both FORMED, with the kit's creative re-projection and the damage-and-repair test passing. Eye T8
  creative facing west with `oldwindows` (339 pre-window panes put back): FORMED.
- 311 registered, 0 skipped; no BTS warning or error, no `com.nateyburr` stack trace. The dev server's level name and
  `cmds.txt` were put back afterwards.

**Not covered.** No client was run, so the window was not looked at in game; the showcase camera should check it at
Eye T7 / T8.

---

### B-96 Controller GUI cleanup, unit 1: the kit, the family frame and the Eye of Harmony [user approval 2026-10-13, built on branch gui-clean]

**Context.** I watched the smoke clip of the Eye GUI: "Based on the video we need more clean details in the
controller", then approved the design ("All are good to go"): `controller-gui-spec.md` + `controller-gui-mockups.html`
(local work folder `gui/`). This unit builds spec §2-§4 (the kit and the base class) and §5.1 (the Eye). The Kerr,
modules, Homogeny, Reality, Silo, halls and hatches (§5.2-§5.10) are later units; the P4 machines (Quasar, Conduit,
Crown, Receptor, Burst Control, the Eye's T7/T8 rows) come after the P4 merge. The spec's recommended answers to its
own Q1-Q12 are taken as built (terminal 118 / panel 227, no power panel on the Eye, the zone as the lit badge, no G
logo and no always-on maintenance icon, Info as a terminal page, lang keys, render toggle in the button column, Feed
only for the T4 star, TecTech's disabled power-switch icon, M/G litres and 1.51e12 EU).

**What was built.**

- `machines/gui/`: `BTSFormat` (§4.2), `BTSText` (server-built text the client translates: lang references with
  arguments, nested keys, literals), `BTSRows` (the terminal's rows as one synced string, decoded once per change),
  `BTSChecklist` (§4.5 `CheckLine` with state, label, have/need value, fill fraction, tooltip), `BTSGuiKit` (palette,
  badges, gradients, every §4.3 component, the row pool, stepper, info toggle, header), `KitDraw` (client-only text
  and pixel icons) and `BTSControllerGui` (§4.4).
- `EyeStarRuntime.ignitionChecklist(want)` and `transitionChecklist(from)`: every condition, met or not, read only
  (the spec's "checks ALL conditions"). `Host.countIgnitionCharges()` counts the charges across the live plinth buses.
- `EyeGui` rewritten on the kit; `MTEEyeOfHarmonyBTS`: `hasRunningText` false, the recipe-result line only for a
  planet target, no power panel, `planetChecklist()`.
- Bench: `/btseye star x y z gui [info]` prints the header and the rows the GUI would show, as text, so a headless
  server can check every state.

**Judgement calls.**

1. **One row string per machine instead of one sync value per number.** The server decides which rows a state shows
   and sends them as one `StringSyncValue` (rebuilt every 5 ticks, values rounded to what is shown); the client's
   fixed pool of rows draws row i with its kind's component. The spec's per-component factories exist too
   (`BTSGuiKit.gauge/bar/check/...` with suppliers) for panels that want them. Why: the Eye has 13 states with
   different rows; keeping the choice on the server keeps it in one method per machine and makes the kit work
   unchanged for the Kerr, Quasar and the rest. *Alternative:* a sync value per field with `setEnabledIf` per row.
2. **Long labels are cut, not scrolled, on bar / check / stat rows** (a "." marks the cut and the tooltip holds the
   full row); notes are `ScrollingTextWidget`s as specified. MUI2's text scrolling needs a whole widget per label;
   the labels we send are short enough that the cut only shows with a long translation.
3. **The dial note tells "short" from "capped".** The spec's "Dial running {paid} of {set}: Tachyon short" would also
   fire when the Time Dilation generators cap the dial below its setpoint, which is not a fluid shortage. Built: the
   Tachyon note when the paid dial is below min(setpoint, cap), and a separate "Dial capped at {cap} (set {set})" note
   when the setpoint is above the cap.
4. **The transition checklist adds "T{from} unlocked"** when the Eye is not yet at that tier (the one line of
   `missing()` the spec's table leaves out), so the checklist is all-met exactly when `missing()` is empty.
5. **States the spec does not tabulate**: a dark Eye holding the Sirius B chart shows the First Cooling checklist
   (before T5) or the next batch's White Dwarf Matter check (after); a dark Eye holding the Magnetar chart shows the
   Magnetar Merger checklist. A LIT badge before the first solved second shows STABLE.
6. **First Cooling during TRANSITION** shows the phase bar and then the COOLING rows (batch, setpoint, Θ about it, P,
   Integrity, mean Y against 0.90); the other transitions show the phase bar, their stat, the gauges, the bounce
   checks (Core Collapse fast-forward), Stability, Integrity, Y | Parallel and Last.
7. **"plateau now"** shows while the cooling setpoint is within ±10; the batch warning row names the plateau ("Event
   warning: crystallisation plateau") rather than a countdown, because the runtime does not know the seconds to it.
8. **Info NEXT** uses five short new keys ("T2: Sol's milestones, the Stellar Mass Core, the Mass Infusion", ...)
   rather than the longer `recipe_result ... locked{n}` sentences.
9. **Planet target**: GT's recipe rows, Parallel, the planet checklist (tier gate, one run of each input, owner and
   one run's EU), plus the last run's result as the Last line (it was only in the scanner).
10. **NO STAR with a non-chart item** in the slot shows "not a target" instead of "empty".
11. **Two IDLE / PAUSED badges.** T5's IDLE is yellow and an idle machine's IDLE is dark (spec §4.1 lists both); P4's
    galaxy PAUSED is red and a module's PAUSED gold. They are separate badge entries sharing one lang key.
12. **The Field panel** keeps a scrolling list (8 sensor rows, 6 trim rows) so a rig with many sensors does not run
    off the 196 px panel.
13. **Litres from 10^12 up print with a T suffix** (`1.00T`); the spec's table stops at G and a filled bench buffer
    printed `1000.00G`. A lit star whose first second has not been solved yet shows "lit" (not "dark") in "{star} lit
    and Stable".

**Verified headless** (dev server on port 25611, `/btseye t1` and `t4` with `fill`, `/btseye keep`, then `/btseye
star x y z gui`): NO STAR, the Info page, DARK with the full Sol checklist (only the charge missing, as the runtime's
wait reason said), IGNITING (P with the -20 tick, 10 %, "in ~36 s"), RESONANT (gauge, Stability, Integrity, Lock
5/600, Cycle 55 s, Y | Parallel, Milestones 0 of 2 with the escrow), PENDING Sol > Sirius A (8 rows, 4 then 7 of 8
met after `milestones 1 done` and the core), TRANSITION Core Collapse (phase 1/3, core mass, P and Θ, the three bounce
checks), the T4 neutron star lit (three gauges, HSM feed 0.694 / 0.694), the remnant case, the Arcturus relight
checklist, Sirius B IDLE with the First Cooling checklist and the Magnetar Merger checklist. 278 recipes / 0 skipped,
no exception.

**Not done here (logged for its owner).** The questbook sentence in "Harmonic Lock": "The GUI's field line counts it
as Lock n/600" should read "The GUI's Lock bar counts it as Lock n/600" (spec §6); applied later on master by the
questbook owner. Every other quest reference to the Eye GUI still holds (Dial − / + with four steps on right-click,
the HSM feed, the Field panel).

**To see in a client.** The layout against the mockups at GUI scale 2 and 3: the Feed + visible at T4, the header
alert only on an issue, the render toggle in the column top, the Info page, the Field panel at 1.0 text.

**Suggestion.** Accept; the next units (Kerr, modules, Homogeny, Reality, Silo, halls, hatches) reuse the kit as is.

### B-97 Controller GUI cleanup, unit 2: the Kerr Ergosphere Engine and the Kerr modules [user approval 2026-10-13, built on branch gui-clean]

**Context.** Spec §5.2 (the KEE) and §5.3 (the Compressor and Extractor modules) of `controller-gui-spec.md`, on the
kit from B-96. The spec's recommended answers stand (Q1 terminal 118, Q4 no G logo, Q5 Info page, Q6 lang keys, Q8
render toggle in the button column, Q10 TecTech's disabled power-switch icon).

**What was built.**

- `MTEKerrEngineGui` on `BTSControllerGui`: the §5.2 badge table (FROZEN, NOT FORMED, CLOSED, NO POWER, DRIFTING,
  GROWING, OPEN), subject "Hole {m} SM" / "No hole", right chip Kerr T1/T2 (tooltip: setpoint range, rail Mk and cap).
  Open: Mass bar (setpoint tick, value white / gold / aqua against the setpoint), Stability bar, the red evaporation
  note while drifting, Governor bar, Bolt stock with its duration, Containment, Bleed, module chips. Closed: the "To
  open a hole (8 SM)" checklist (cage, Seed, containment power, bolts), Auto, chips. Info: the questbook's RUN steps,
  BOLTS, T2, AUTO. Gap row: Setpoint stepper; [Hole control] [Modules] [i]. Render toggle in the button column. The
  disabled power switch shows TecTech's icon with "The power switch does not stop the hole." Both side panels draw at
  1.0 (Hole control 150 x 196, Modules rows 32 px).
- `MTEKerrModuleGui` on the base as a recipe machine (GT's rows and buttons stay): RUNNING / IDLE / PAUSED / NOT
  LINKED badges, the right chip x{parallel}, the "Hole bonus" stat pair, KEE cell and hole while linked, the "Runs
  again when" checklist otherwise, the DENSITY gate as a note, an Info page.
- Kit: a chip row's tooltip shows the hovered chip only (`KitWidget.mouseX`, `BTSGuiKit.chipAt`).
- `hasRunningText` false on the KEE and the modules; `shouldDisplayCheckRecipeResult` false on the KEE.
- Bench: `/btskerr gui x y z [info]`, `/btskerr module x y z <c|x> gui [info]`.

**Judgement calls.**

1. **NOT FORMED rows for the KEE** (the spec lists none): GT's structure-error rows (with the highlight button), then
   "Not formed: hologram stack {1|2} shows the Kerr T{t} cage" and the module chips.
2. **Modules get a NOT FORMED badge first** (the spec's table starts at LINKED). A module whose own arc is broken but
   whose KEE is open would otherwise read IDLE. Its rows add a red note naming the KEE marker, then the link rows.
3. **The module's GT recipe-result line hides only for the link results** (`kerr.module.holeclosed`, `nokee`, ...),
   which the checklist already says; real results (no recipe, outputs full, the DENSITY gate) still show. This
   matches the paused mockup, which shows no result sentence. It is decided on the client from the synced result id.
4. **"KEE found" values are short** ("none here", "faces away") with the full link reason in the tooltip; the spec's
   "the Link text" does not fit beside the label at 1.0.
5. **"The recipe in progress is kept"** (in the mockup, not the spec's list) shows under the paused note only while a
   recipe is held.
6. **The containment-power check** compares the energy hatches' maximum input (`getMaxInputEu`, lasers included) with
   8,388,608 EU/t; it does not check the stored charge.
7. **The DENSITY gate note** is aqua when the gate is open and amber when shut (the spec says amber); its text is the
   scanner's line with its colour codes stripped, and the tooltip is the recipe-result text.
8. **`mapsLine()` is deleted.** "What the module runs" is now a lang key per slot (`beyondthestars.gui.module.info.
   maps.<slot>`), so a new module adds one key, not a method.
9. **Every Kerr GUI string is a lang key**, including the side panels' strings that were English in Java. The units
   (SM, EU/t, SM/t) stay literal, as on the Eye. The module-state words reuse the badge keys.
10. **The Hole control panel's tier line** reads "Kerr T1 ... Density: 2-512" (T2: "Density: in"), as in the mockup,
    with the full sentence in its tooltip; the old 0.75-scale sentence did not fit at 1.0.
11. **An empty bolt stock** shows "0.00 SM" in red without a duration.

**Verified headless** (dev server on port 25611, `/btskerr t1 200 100 600 north force`, `/btskerr keep`, then the gui
dumps): CLOSED with every check met (Seed 1, containment 8.39M against 4.29G of hatches, Auto ON); OPEN at 16.1 SM
(mass 0.25 of 64 with the tick, stability 100 %, governor 0.05/4, bolt stock 12,773 SM 14h 27m, bleed 0.0123 SM/t);
both modules IDLE / Linked x448 with Parallel 448 | Speed x3.97, the compressor's DENSITY note, the extractor without
it; CLOSED after `render close` (Seed 0 BAD) and with the buses emptied (Bolts to grow it, amber); the compressor
PAUSED with KEE found / formed and loaded OK and Hole open CLOSED; DRIFTING at 14.9 SM with no bolts (stability 0 %,
"Evaporates in 17 s without bolts", governor 0.00/4); FROZEN after `damage` with the hole open (the extractor NOT
LINKED, "KEE formed and loaded: NOT FORMED"); NOT FORMED after `damage` with it closed; both Info pages. 278 recipes /
0 skipped, no exception from our code (GT's own "null detected in fluidInputs" dev-env lines are unchanged).

**Not seen.** RUNNING on a module (the stocked compressor recipe had not started when the hole was closed) and NO
POWER / GROWING (the bench has no unpowered or under-setpoint-with-bolts case); these are one-line badge branches.

**To see in a client.** Both GUIs against `pair-kee.png` / `kee.png` at GUI scale 2 and 3: the power switch with
TecTech's icon (no magenta), the render toggle at the top of the button column, the chip hover picking one chip, the
Hole control panel at 1.0 (the two-line hint and Auto label), the Modules panel's scrolling lines.

**Questbook.** No change needed: "The GUI shows how long the stock lasts", the Hole control panel's Auto, Open and
Collapse twice all still hold.

**Suggestion.** Accept.

### B-98 Controller GUI cleanup, unit 3: the Homogeny, Reality Diffraction / Refab, the Reactor Silo and the production halls [user approval 2026-10-13, built on branch gui-clean]

**Context.** Spec §5.6-§5.9 of `controller-gui-spec.md`, on the kit from B-96. The spec's recommended answers stand
(Q1 terminal 118, Q4 no G logo, Q5 Info page, Q6 lang keys, Q7 keep the Silo's instrument panel). All four are recipe
machines except the Silo, so GT's progress / output / recipe-result rows stay above the kit rows.

**What was built.**

- Kit: a lamp row's lamps carry their own hover tooltip (the hovered lamp's, as the chips do); `recipeBadge()` gives
  every recipe machine one rule (NOT FORMED, RUNNING while active, WAITING while the last check failed for a reason
  to fix, IDLE for no recipe / nothing checked); `infoInButtonColumn()` puts the **i** button in the column's top slot.
- `HomogenyGui`: badge NOT FORMED / INSTALLING / RUNNING / WAITING / IDLE; subject "Mixing" or "T2 > T3"; right chip
  "T{tier}  {n} par" (tooltip: installed, structure). Rows: the Core / Stir / Outer lamps (tooltip: the ring, its tier,
  formed / missing / not needed, and its first structure error), then either "Upgrade to T{n+1}" with `{met} of 3` and
  the three checks (`MTEHomogeny.upgradeChecklist()`: Mixed have/need with a fill line, the core in escrow, the ring
  built with its fault) or, while installing, the Install bar, Time left, "Paused: {why}" and the cancel hint; Last.
  The Install button draws `BUTTON_STANDARD_DISABLED` with grey text until `installBlocker()` is empty (or while an
  install runs); pressing it still goes through `beginInstall()`, the one install path.
- `RealityDiffractionGui`: badge RUNNING / WAITING (no network or storage, regions waiting to enter, nothing
  anchored) / IDLE; subject "Diffracting" or what it waits on (BEC network, Network full, Anchors). Rows: the BEC
  network check, one red check per unanchored region (have / need L with a fill line, or "no hatch";
  `anchorChecklist()`), "Other regions {k} of 6" (or "Regions anchored 6 of 6"), the amber "Waiting to enter" check
  and its hint, Last.
- `RealityRefabGui`: badge as above, WAITING also while the craft waits; subject "Step s/n". Rows: the network and
  Condensate Assembler checks (`linkChecklist()`: "{n} nanites", "none within 64", "OFF", "no nanites"), the step stat
  (nanite tier, contaminants), the Regions bar, the wait reason as a red check, Last.
- Both Reality machines keep their gap-row region lamps (`HRGui.regionLamps`; caption and tooltips now lang keys).
- `MTEReactorSiloGui`: badge NOT BUILT / HELD / CLAMPED / RUNNING / OFFLINE; subject "Silo T{n}"; right "Rods n/cap".
  Rows: Output (against the dynamo + laser ceiling, yellow tick at it), RPM (against the operating redline, red tick
  at 12,000), Heat (against meltdown, red tick at SCRAM), the hold reason, then today's instrument panel unchanged;
  its group headers are kit section rows. The **i** button is in the column; the Tier / RPM / Rods captions are at
  1.0 with fields 18 / 34 / 26 wide. Info = the questbook's "Step 10 - Read the Panel" in five paragraphs.
- `MTEProductionHallGui`: badge, subject "x{parallel}", the Parallel stat and what sets it, Info page.
- `hasRunningText()` false on the Homogeny, the Reality base, the Reactor Silo and the halls. 109 new lang keys.
- Bench: `/btshr gui <x> <y> <z> [info]` prints the header and rows of any of these GUIs (plus the last recipe check).

**Judgement calls.**

1. **Ring lamps:** green when the piece stands, red when the INSTALLED tier needs it and it does not stand, grey
   otherwise. The spec's "red when absent with a fault" would make every ring above the tier red (an absent ring
   always has a structure error); the mockup shows the T3 ring grey on a T2 Homogeny, and the upgrade checklist says
   what the next tier needs. The fault is in the lamp's tooltip either way.
2. **WAITING vs IDLE on recipe machines:** "no recipe" (empty inputs) and "nothing checked yet" are IDLE; every
   other failed check (outputs full, power, a machine condition) is WAITING. Without this an empty hall read WAITING.
3. **Silo subject stays "Silo T{n}" when HELD** (the spec's table also says subject "Held"): the badge already says
   HELD, and the hold reason is the first row under the bars. **Silo right chip "Rods n/cap"** uses the unlocked
   rod cap (`maxThrottle()`, 24 by design, up to 36), not a fixed 24, so it never reads 30/24.
4. **The MATH header is split:** a kit section row "MATH" and a grey line under it with the subtitle ("how that EU/t
   is derived" / "how that steam figure is derived"), because the whole title does not fit 180 px in bold. The
   questbook's words stay on screen.
5. **Heat bar** has a red SCRAM tick (the spec gives only "Heat %"); the bar is heat / meltdown.
6. **Region names and the halls' "(fixed)" / "(shell: ..., tier 2/3)" source** go through lang keys
   (`beyondthestars.gui.reality.region.0..5`, `beyondthestars.gui.hall.source`); the parallel source text itself and
   the Refab's wait reasons stay runtime English (spec Q6).
7. **Refab subject** is empty while no craft runs (the spec gives none); Diffraction's subject names what it waits on,
   as the mockup's "WAITING Anchors".
8. **Found and fixed in units 1-2:** `CheckRecipeResult.getID()` is the result TYPE ("simple_result" for every
   `SimpleCheckRecipeResult`), not its key. The Kerr module's `shouldDisplayCheckRecipeResult` compared it with the
   link keys, so the link results were never hidden; it now compares with `equals` (the key). The Eye's planet
   checklist built the lang key `recipe_result.simple_result`; it now shows the gate's display string.

**Commit note.** Commits fb1a95c, b7aab46, 84755f4 and 3c95056 each hold an en_US.lang with only that machine's new
keys: the staging helper compared the CRLF working tree with the LF blob. Their Java is right; 588ae66 restores the
whole file (baf745f plus the 109 new lines). Anything that reads the lang file should use 588ae66 or later.

**Verified headless** (dev server on port 25611, 278 recipes / 0 skipped, no stack frame from our code in three
boots; `/btshr gui`): Homogeny T2 RUNNING with the T3 checklist 0, 1 and 3 of 3 (after `project 3`, `milestone`,
`escrow`), INSTALLING T2 > T3 with the stall notes (running mix, controller disabled, power) and NOT FORMED after a
restart; T3 (lamps only) and T1 (Stir and Outer grey, the Stirring ring check with its fault); both Info pages.
Diffraction WAITING "BEC network" with all six regions red then "Regions anchored 6 of 6" and the amber 24,000 L
waiting row. Refab WAITING with "none within 64". Silo T1 RUNNING with Output / RPM / Heat and the Info page. PUREX
and Cascade Hall IDLE "x8" / "x1" with "(fixed)" / "(stage banks 0/4)". Full `gradlew build` passes; jar
`beyondthestars-0.24.1-gui-clean.254+97e5fe8590.jar`.

**Not seen.** A Diffraction or Refab run (they need P4's fluids and a Containment Field), the Install button's grey
face, the Silo's NOT BUILT / HELD / CLAMPED badges, the Kerr module fix in a client.

**To see in a client.** The four GUIs against `hr.png` / `plant.png` at GUI scale 2 and 3: the lamp tooltips, Install
greyed then live, the Silo's **i** in the button column and the 1.0 field captions fitting the gap row, the section
rows over the instrument panel.

**Questbook.** No change needed: "Install in the GUI", MATH and MILESTONES "at the foot of the GUI" (they still are,
below the new vitals), the Build Tier / Rods / RPM fields and the Upgrade Tree button all still hold.

**Suggestion.** Accept.

### B-99 Controller GUI cleanup, unit 4: the hatch layout, the Directive Hatch and the Stellar Field Sensor Hatch; the lang completeness pass [user approval 2026-10-13, built on branch gui-clean]

**Context.** Spec §5.10 and §6 of `controller-gui-spec.md`, on the kit from B-96. The P4 hatches (Burst Control,
GRB Receptor) are not on this branch; `BTSHatchGui` is general enough for them (any box height, any panel height,
cycle / toggle / field rows, line / lamp / gauge / bar readout parts).

**What was built.**

- `machines/gui/BTSHatchGui`: the hatch family layout. A 166-wide `BACKGROUND_TERMINAL` readout box at (5, 5); the
  controls 5 px under it, 16x16 buttons at an 18 px pitch, labels in panel text (scroll on hover); no G logo and no
  corner flows. Readout parts drawn at scale 1.0 with the kit's painters: a text line (left / right), a bit lamp, a
  coloured lamp line, the ±120 zone gauge with a threshold tick, a plain bar with a tick.
- `DirectiveHatchGui`: panel **214** (the controls end at y 126, the inventory starts at 130; today's rows ran into
  it). Box 46: the set and the machine it reports for; the four bits as lamps "1 Helium", "2 Hydrogen", "4 ...",
  "8 ..." (lit green and white while on, grey while off); "Code {c}" with "redstone {o}" (green, dark grey at 0). A
  control set shows "Input {n}" with the setpoint it drives, then "Redstone 1-15 sets it" / "0 leaves the GUI's
  value". With no machine pushing, the host reads "no machine" in gold and the code line "No machine reporting" (its
  tooltip says where the hatch goes). Controls: Set (tooltip "Click: next set for this machine; right-click:
  previous"), Output (tooltip lists the six modes and that a comparator reads the code), Invert ("15 - output"),
  Window field 26 wide with "Window w: gauge band, ±w" / "bolt reserve, minutes".
- `StellarFieldSensorGui`: panel 214. Box 40: the channel and "out {n}"; the reading as the ±120 zone gauge (P, Θ,
  Ω, Π, T5 setpoint error; value in the zone colour), a bar (shield, Integrity, Stability; the health colour rule) or
  a lamp (the zone in its colour, a warning red, upkeep green / red), the threshold as a yellow tick in digital mode;
  then "ON above +10.0" / "ON below ..." or "Analog 0-15". "No reading (star dark or Eye silent)" when dark.
  Controls: Channel (cycle), Above / Below, Digital / Analog, Threshold field 40 wide, each with a tooltip.
- `MTEHatchDirective.guiLine()` (only the GUI used it) is replaced by `getControlLine()`; new GUI sync keys only
  (`btsDirOut`, `btsDirHostId`, `btsDirControlIn`, `btsDirControlLine`, `btsSensorThreshold`). No NBT change, no
  behaviour change.
- 80 new lang keys (`beyondthestars.gui.directive.*`, `beyondthestars.gui.sensor.*`); the sensor's zone names reuse
  `beyondthestars.gui.zone.0..4`.
- Bench: `/btseye star x y z hatchgui` and `/btskerr gui x y z hatches` print each hatch GUI's readout and control
  labels from the same text functions the GUIs draw with.

**Judgement calls.**

1. **The readout's first line names the set without its host prefix** ("Trims", "Setpoint (control)") because the
   machine is on the right of the same line; the mockup's "Eye: Trims ... Eye of Harmony" does not fit 156 px for
   "Eye: Transition" or any Kerr set. The Set row below keeps today's full names ("Set: Eye: Trims", "Kerr: control
   the mass setpoint"). The Kerr host reads "Kerr Engine" for the same reason.
2. **When the two halves of a line do not fit, the less important half is cut**: the host on the Directive line, the
   channel name on the sensor line ("out {n}" always stays whole; the long "Zone (0 Resonant .. 4 Breach)" gets the
   full name in its tooltip).
3. **Invert, Above / Below and Digital / Analog keep GT's redstone on / off overlays** (today's buttons, and GT's own
   `createInvertButtonRow`), not the mockup's work-area icon: they show the toggle's state.
4. **Lamp channels**: the zone lamp takes the zone's text colour (Resonant green .. Breach red); a warning lamp is red
   while warned and grey otherwise; upkeep is green when met and red when short. The spec only says "a lamp".
5. **The threshold tick is hidden in analog mode** (the threshold is ignored there). The 0-100 bar puts it at
   threshold / 100.
6. **The bit names and the control line stay runtime text from the host** (the Eye's trim names follow the star's
   tier, and OpenComputers and the scanner share them, spec Q6); every other word is a lang key.

**Lang completeness pass.** All 139 keys listed in spec §6 exist. A GUI key check (local work folder
`hatchgui/guilint.py`) expands every `beyondthestars.gui.*` literal, the one-letter prefix constants (`K + "x"`,
ternaries, `String[]` loops) and the enum-built families (badges, Directive sets / modes / hosts, sensor channels,
zones, Eye gauges and Info keys): 390 literal and 104 dynamic lookups, **0 missing**, 0 duplicates, 0 empty values.
The repo's lang lint (as in B-88 / B-90): 235 lookups, the one known false positive
(`item.beyondthestars.fieldRetrofitKit.name`, named per meta), 0 duplicates. A scan of the branch's GUI diff for
hard-coded words found only today's runtime strings (the Silo's instrument-panel headers, the Eye's star names).

**Verified headless** (dev server on port 25611, world `guitest`, 278 registered / 0 skipped, no stack frame from
our code): the Kerr's two Directive Hatches before the first push ("no machine", "No machine reporting", redstone 0),
reporting (Hole: bits 1 and 4 on, "Code 5", redstone 5; Status: "Code 2"), and a control set with a redstone block
("Setpoint (control) ... Kerr Engine", "Input 15 | mass setpoint 64 SM"). An Eye T1 built at -1500 100 -1500: three
Directive Hatches with T1's trim names ("1 Helium", "2 Hydrogen", "4 (unused on T1)"), Trims "Code 1 / redstone 1"
on a lit Sol; five sensors dark ("No reading") then P (gauge +5.0 and +11.2 against the +10.0 tick, above and below),
Stability (bar 87.1, out 15), Zone analog (Stable lamp, out 4, "Analog 0-15"), Upkeep OK (met, out 15), Event
warning (none), T5 setpoint error on T1 (gauge 0) and Integrity. Full `gradlew build` passes.

**Not seen.** Both GUIs in a client (the headless server cannot draw them).

**To see in a client.** Both panels against `hatch.png` at GUI scale 2 and 3: nothing over the inventory, the lamps
and the gauge tick, the Set cycle's right-click going back, the tooltips.

**Questbook.** No change needed for the hatches. The "field line" -> "Lock bar" sentence (B-96) is still for the
questbook owner on master.

**Suggestion.** Accept. The P4 Burst Control Hatch (196 high, a 0-15 bar) and GRB Receptor (166, a lamp) fit this
layout as they are specified.

### B-100 Controller GUI cleanup, round 1: the GUIs in a real dev client against the mockups [user approval 2026-10-13, built on branch gui-clean]

**Context.** Units 1-4 (B-96..B-99) were checked only on a headless server. This round opened every master-side GUI
in a dev client and put each state next to the approved mockup (`controller-gui-mockups.html`). The comparison page
is in my local work folder: `gui/round1/gui-compare.html` (mockup vs in-game per state, a one-line note each).

**What was built.**

- `rendertest/GuiShotsClient`: a dev-only screenshot harness behind `-Dbts.guishots=true` (inert otherwise),
  documented in the build doc; scripts in `tools/guishots/` (`build-sites.shots` builds every machine, the other
  files shoot each state). It drives the client from a queue file, opens GUIs through the server, sets the GUI scale,
  fakes the mouse without moving the real cursor (hover tooltips, clicks on MUI2's hovered widget, wheel scrolling),
  and logs each panel's area for cropping.

**Found and fixed (each its own commit).**

1. **Every kit terminal was empty** apart from its header. `BTSControllerGui` started its rebuild clock at
   `Long.MIN_VALUE`; `now - MIN_VALUE` overflows to a negative number, so the row and Info strings were never built.
   The bench dumps call `addStatusRows` directly, which is why B-96..B-98 did not see it. Starts at -1,000,000 now.
2. Info paragraphs used a `TextWidget`, which wraps at the font's 9 px; a kit `ParaWidget` draws them at the 10 px
   pitch the spec gives.
3. A bar label wider than the 50 px column ("Milestones") was cut to "Milestone."; it now pushes the bar right (the
   bar keeps its right end at 132).
4. Litres just under 10^12 printed "1000.00G"; every unit now ends where its rounded text would reach the next
   ("1.00T"). EU/t the same.
5. Eye header: "Neutron Star (Arcturus Remnant)" ran into the "Eye T4" chip; the subject drops a parenthetical.
6. Eye Field panel: the zone legend in three columns; sensor and trim rows keep their value right-aligned; the trims
   title, "none" and the footer left-aligned, as drawn.
7. Kerr Hole control: GT's theme gives a `SliderWidget` no track; the slider draws one.
8. Reality Refab: with GT's void / separation / batch buttons in the gap row, region lamps IV-VI ran under the
   **i** button. The "Regions" caption is dropped when GT's buttons are present; each lamp's tooltip names its region.
9. Reactor Silo: the instrument-panel lines wrapped at the list's 186 px, under the scrollbar; they wrap at 180. The
   RPM field cut "12,000" at 34 px: it is 38, the Rods field 22. Group headers are gold, as the mockup draws them
   (the scanner's bold aqua `header()` is unchanged).
10. Directive Hatch: the Status and Transition bit names were cut to stubs ("1 Buffers s.") in two 78 px columns. The
    readout lists the four bits one per row (box 66, panel 234; the controls still clear the inventory). A lamp text
    that still does not fit keeps the first word of its parenthetical ("4 (unused)").
11. Stellar Field Sensor Hatch: a long channel name drops its parenthetical before the readout line cuts it
    ("Zone"); control labels use the mockup's words ("On ABOVE the threshold", "Analog 0-15"; the threshold note moves
    to the mode tooltip).
12. Refab check value "none within 64" cut its label to "Condensate Assem."; it reads "none" (the range stays in the
    tooltip and in the recipe-result line).

**Judgement calls.**

1. **Directive panel 234 instead of 214** (spec §5.10). One bit per row was the only way to show the real bit names
   whole; two columns of 78 px cannot hold "Ignition charge wanted". *Alternative:* keep 214 and two columns with cut
   names (the tooltips do not cover the readout).
2. **Silo group headers gold** rather than the kit's grey section colour: the mockup draws them gold and they
   separate eight groups in a long scrolling panel.
3. **Left as they are (by spec, logged on the page as "differs by spec"):** recipe machines keep GT's own recipe
   rows (progress, output item) and GT's void / separation / lock buttons, which the mockups drew as kit rows or left
   out (Kerr modules, Homogeny, halls). A paused Kerr module still shows GT's last recipe result above the link
   checklist (B-97 #3).
4. One remaining cut: the Directive Hatch's Status bit "Event warned / Agent warning" is cut by a few pixels in one
   full-width row. It is runtime text the scanner and OpenComputers share (B-99 #6); shortening it is for the owner of
   the Directive sets.

**Seen in the client** (GUI scale 3 and 2, window 1920x1080): Eye NO STAR, DARK (full checklist), IGNITING, lit
RESONANT, T4 neutron star STRAINED with the Feed stepper (+ visible), PENDING Sol > Sirius A (wheel scrolling),
TRANSITION Core Collapse, T5 COOLING, the Info page, the Field panel, the right-chip tooltip; Kerr OPEN, CLOSED,
DRIFTING, the Hole control and Modules panels, the per-chip tooltip, the TecTech power-switch icon (no magenta); the
extractor module RUNNING and the compressor PAUSED; Homogeny RUNNING (Install grey) and INSTALLING (Install live);
Diffraction WAITING with 144,000 L to enter; Refab WAITING; the Reactor Silo (scrolled, Info, **i** in the column);
PUREX Canyon RUNNING; the Directive Hatch (Trims, Status, Transition sets) and the sensor's gauge, bar and lamp
channels. The alert icon showed only on a shut-down Homogeny. Full `gradlew build` passes; jar
`beyondthestars-0.24.1-gui-clean.271+996e07a526.jar`.

**Not seen.** The P4 machines (not on this branch); a NOT FORMED controller; a Diffraction or Refab run (P4 fluids);
the Silo's NOT BUILT / HELD / CLAMPED; GUI scale 4.

**Dev-client notes for the next round.** A structure built under the player is only known to the client after a
re-join (the harness's `gui` waits, then reports "the client never got the machine"); build first, re-join, then
shoot. The player must stay within 8 blocks of the controller or the server closes the GUI; the harness holds it
flying in place.

**Questbook.** No change from this round. The "field line" -> "Lock bar" sentence (B-96) is still for the questbook
owner on master.

**Suggestion.** Accept.

### B-101 Controller GUI cleanup, review fixes: the T5 Next batch reach row and a read-only charge count [user approval 2026-10-13, built on branch gui-clean]

**Context.** A code review of the gui-clean branch raised two findings on the Eye GUI. Both were checked against the
code (and the RC-2 GT and AE2 sources) before they were fixed.

**1. T5 "Next batch" hid the only reason a batch did not start (confirmed, fixed in d1cbe55).** With T5 unlocked and
a Sirius B chart, `tryStartCooling` first checks `parallel(5) >= 1` ("the Eye does not reach T5 (bands,
compression)") and only then the White Dwarf Matter charge. The DARK and IDLE rows showed only the charge row, so an
Eye with a band or a compression generator below T5 and 18,432 L buffered showed an all-green list and never started.
The only hint was the yellow right chip. `EyeStarRuntime.batchChecklist()` now sits next to `ignitionChecklist()` and
makes the same test as the runtime: "Eye reaches T5" (OK when `parallel(5) >= 1`; value `T{installed} / T5`; tooltip
the `tier5` / `locked5` / `compression5` recipe-result line, so no new lang keys), then the charge row as before.
`EyeGui.nextBatchRows` draws that list.
Seen in the dev client (`tools/guishots/eye-t5-batch.shots`). With one compression generator at tier 4 and a full
charge buffered, the list shows "Eye reaches T5  T4 / T5" in red and the charge row in green, and the tooltip names
the compression gate. At T5 with no charge, the reach row is green and the charge row red. When a chunk of the spiral
arms was not loaded, the tooltip named the band gate instead.

**2. The DARK checklist's charge count opened a recipe-processing window (fixed in 4317207; the trigger is narrower
than the review said).** `countIgnitionCharges()` is GUI-only and runs every 5 ticks while the GUI is open. It
called `startRecipeProcessing` / `endRecipeProcessing(this)` on each plinth bus. RC-2's
`MTEHatchInputBusME.endRecipeProcessing(controller)` calls `controller.stopMachine(CRITICAL_NONE)` if
`getProxy().getStorage()` throws `GridAccessException`. So a GUI read could reach a critical shutdown.
The review named an unpowered, channel-starved or disconnected network as the trigger. In AE2 that is not enough:
`AENetworkProxy.getGrid()` throws only when the node or its grid is null, and a disconnected node is still a
one-node grid. The trigger is a stocking bus whose node is gone or not yet created (a rare, transient state), and the
extraction branch cannot fire from a read, because a read takes nothing. Either way, a GUI must not be able to reach
`stopMachine`, and the window also cost a 16-slot simulated extraction per bus, per viewer, every 5 ticks.
The count now opens no window. Plain buses are read as before. An ME stocking bus shows only its circuit and manual
slots outside the window, so its network stock is read from the grid's item list (`findPrecise`) for each slot
configured to the charge, once per distinct config. The bus counts 0 when it is disabled or its network is inactive,
which is also what a recipe check would see. No extraction is made and no shutdown path is reached.
Seen in the dev client (`tools/guishots/eye-charge.shots`): an Arcturus chart on the T1 Eye shows "0 in bus", then
"2 in bus" after two charges go into the plain plinth bus, and GT reports "last shutdown none".

**Judgement calls.**

1. **The reach row is always shown** in the Next batch list, met or not, as the ignition checklist does; the spec's
   §5.1 IDLE list names only the White Dwarf Matter row. *Alternative:* show it only when it fails (one row shorter
   in the normal case).
2. **The ME path was not exercised in game.** The bench has no AE network, so the stocking-bus branch is checked
   only by compiling against RC-2 GT and AE2, and by the boot.

**Seen but not changed.** The charge row's label "Molten White Dwarf M." is cut with a dot. It is the fluid's GT
name, and its full name is in the tooltip. The spec draws "White Dwarf Matter". This was the same before the fix.

**Build.** `spotlessApply compileJava` passed before each commit, and the full `gradlew build` passed. Jar:
`beyondthestars-0.24.1-gui-clean.275+d1cbe55c84.jar`. The headless dev server (port 25611) loaded 278 recipes with
0 skipped, and no `com.nateyburr` frame appeared. The comparison page `gui/round1/gui-compare.html` has a new
"T5 idle" card and the two charge shots on the Dark card.

**Suggestion.** Accept.

### B-115 Merging gui-clean into master: P4 in the kit GUIs, the buffers against their caps, the Lock bar [merge, built + test]

**What was done.** `git merge --no-ff gui-clean` into master `82630b4` (merge `3c0daf0`), then three follow-ups.
`gui-clean` branched from `ba08813`, before the autoshots merge, B-108 and P4, so 17 files conflicted. The merge
commit's message lists each one and how it was resolved. The kit GUIs are the new GUIs. Master's GUI-side additions
were rebuilt in the kit's form, so nothing was lost:

- **Eye.** P4's Eye T7 / T8 information is now kit rows:
  - the W and Λ gauges;
  - a Bursts row: Bu/t and β, with high / low / halo, the demand and the linked quasar in the tooltip;
  - the Stability / Heavy Link streak as a bar, or HELD;
  - the countdowns as a warning row: collapse, pericentre pass, satellite crossing, minor merger;
  - the planet slots, or the companions.

  The milestones show up to T7 (T6 / T7 parts: product, Resonant time, flares or mergers, the link). The DARK and
  PENDING checklists carry P4's conditions: the magnetar or Triangulum lit and Stable, the rail and halo ports, no
  companions. The Info page has a GALAXY paragraph and NEXT for T7 / T8. P4's Galaxy panel is a kit side panel
  behind a [Galaxy] button: mode, the f8 stepper, Begin with its answer, the slots, and what the transition waits for.
- **Stellar Field Sensor Hatch.** The kit hatch layout now follows the machine that publishes to it: 20 Eye
  channels, or the quasar's 16. The channel button steps through the synced count. Unbounded channels draw a bar
  against the window w. P4's Invert and Window w are control rows (panel 244).
- **Directive Hatch.** P4's sets (Disc, Field, Events, Galaxy Transition, Slots), the Quasar host and its window
  label have lang keys. Master's +64 height fix (`a98e2f9`) is dropped: the kit panel is 234 and nothing sits under
  the inventory.
- **KEE, Homogeny, Reality Diffraction.** P4's quasar-link line is a kit note row, with the whole line as its
  tooltip.
- **Kept from master.** The Eye's status-line push (`cfb2001`), the B-108 cap, B-112's markDirty.

**Follow-ups.**
1. **Buffers held / cap** (`302d28c`, `fefa148`; the B-108 open item). The Eye's Field panel has a "Buffers (held
   / intake cap)" section, one row per buffer: the name, a small bar (blue filling, green at the cap, gold above it
   or not pulled), and "1.00T / 600M L". Each row's tooltip gives the exact litres; the title's tooltip gives the
   rule. `MTEEyeOfHarmonyBTS.intakeRows()` feeds both this panel and the scanner line.
2. **Questbook** (`90d3ccf`). Harmonic Lock now says "The GUI's Lock bar counts it, n/600" (B-96 / B-93).
   Regenerated to bundle `622edc863402`; `--check` and `--selftest` pass.

**Checks.**
- `spotlessApply compileJava` passes.
- A GUI lang check (literal keys, the `K +` lookups, every sensor channel, Directive set and host, the six gauges)
  finds no missing keys and no duplicates.
- Headless `runServer` (port 25565, fresh flat world `b115`; the level name and `cmds.txt` were put back):
  - 311 registered, 0 skipped. The 8 known H/R gate lines, and no `com.nateyburr` stack trace.
  - Eye T1 kit with `fill`: FORMED, with re-projection and damage / repair. The scanner's buffer line reads from
    `intakeRows` (Helium 1.00T / 1G cap; the fluids Sol does not use show "not pulled").
  - `/btseye star ... gui`: DARK, with the five-row ignition checklist. `gui info`: the Info page.
  - `hatchgui`: three Directive and five sensor readouts, including the new Invert and Window rows.
  - Eye T7 kit: FORMED. Its DARK checklist is the Magnetar Collapse, 6 of 7: the new "magnetar lit and Stable" row
    (X) and "Rail ports 4 / 4" (OK), plus the WDM / BDM / Magnetar Matter needs. Its Info page has GALAXY and NEXT T8.
  - Kerr T1 kit: FORMED; `/btskerr gui` and `gui ... hatches` print the rows.

**Not covered.** No client was run, so the merged GUIs were not seen drawn. That covers the Galaxy panel, the Field
panel's Buffers section, and a lit T6-T8 Eye's burst / link / countdown rows; those rows need a lit galaxy, which the
headless benches did not run. The P4 machines' own GUIs (Quasar, conduit, crowns, Burst Control Hatch) are still
GT's plain layout.

**Question.** *Where the buffers show:* in the Field panel, not the main rows. The main pool is already 26 rows at
T5-T6, and a T7 Eye can hold a dozen fluids. *Suggestion:* keep it there. The DARK and PENDING checklists already
show have / need for every fluid that blocks a start.


### B-116 The P4 machines on the GUI kit, and the Eye's T7 / T8 rows, checked in a dev client [final showcase, built + client check]

**What was built.** The P4 machines' GUIs now use the controller GUI kit (spec §5.4, §5.5, §5.10). Their old GUIs
were GT's plain layout with 0.75 / 0.8-scale text, or one block of status lines. Every piece of information they
showed is still there: as a row, in a row's tooltip, on the **i** page, or in a side panel.

- **Quasar Engine** (`MTEQuasarEngineGui`):
  - **Badge:** NOT FORMED, FROZEN (a chunk unloaded), NO HOLE, DORMANT, RE-FORMING, or the zone of E while the hole
    is open. The header shows "Hole M SM" and "coils T n".
  - **Open:**
    - the E gauge. It uses the kit's zone bands at ±10 / 40 / 70 / 100; the old dial drew ±30 / ±60, which was the
      spec's bug;
    - the Flux bar, with the purge setpoint (yellow) and the 90 warning (red) as ticks, and an "Erupting soon" note
      at 90;
    - Mass against the coil cap;
    - Integrity (yield and Stability in its tooltip);
    - Spin a | Bursts;
    - each jet's dock, or venting;
    - the feed (drawn / Eddington L/s; the buffer and trims in the tooltip);
    - Coils b and EU/t, or a "Coil power SHORT" check;
    - the machines fed;
    - a conduit-closed warning, the last event, the render note.
  - **No hole / dormant:** the Open checklist. The rows are the seed in the slot (a Black Hole Seed when dormant),
    the rim gas hatches, the coil power hatch (Energy / Laser) and the coils (all T n).
  - **Re-forming:** Disc r / 300 s, the "Feed f 0.3 or more" check, the mass, "Jets stopped" and the blow-out.
  - **Info page:** text from the quests "Opening the quasar", "Reading the disc" and "Burst Control".
  - **Gap row:** the Feed f stepper; [Disc control] [Jets & links] [i]. The render toggle is at the top of the button
    column. The power switch shows the kit's disabled icon, so GT's missing texture is gone from the P4 controllers
    (Quasar, conduit elements, crowns). The side panels' text is at scale 1.0, and the Disc panel is 160 x 244.
- **Conduit elements** (`ConduitElementGui`: Wormhole Mouth, Mirror, Junction):
  - **Badge:** NOT FORMED, CLOSED, OPENING (with "paying r / 10 s"), OPEN. A mouth's header says Paired or Not
    paired; the right chip says Mk n.
  - **Open:** the Through bar against the throat cap (clipped and lost as notes), and Upkeep. Then per element:
    - Mouth: the partner and the ring;
    - Mirror: the two faces;
    - Junction: the mode and its three legs.

    Each out-throat names what its ray reaches (aqua), or where it is blocked (red).
  - **Closed:** the opening checklist: the throat, the fuel hatches, and upkeep paid r / 10 with a fill line.
  - The fuel cycle stays in the gap row (a right-click now steps back), and the Junction panel is at scale 1.0.
- **Burst Thermaliser Crown** (`CrownGui`): RUNNING / IDLE / NOT FORMED, with the Mk and cap. The rows:
  - the Stream bar (its parts in the tooltip: jets, receptor, from below);
  - Power in EU/t, with a no-owner check;
  - overflow and clipped notes;
  - the stack;
  - the jet port it is docked on;
  - the receptor's reading;
  - the lifetime EU.
- **Burst Control Hatch** (hatch layout, 196 high):
  - **Readout:** the mode and the machine reading it, a 0-15 redstone bar, and what the host drives.
  - **Controls:** the Mode cycle, whose tooltip now lists the four modes and what each does (the spec's missing
    tooltip), then the mode's help and the rising-edge count.
- **GRB Receptor Port** (hatch layout, 140 high):
  - the machine it feeds;
  - a lamp, LINKED (green) or PAUSED (Q-8, amber), with last second's Bu/t;
  - the reading by source;
  - the lifetime in the tooltip.
- **Eye, T6-T8** (spec §5.1):
  - **Badges:** a READY badge (the galaxy transition waits only for Begin) and a red PAUSED badge (the quasar feeding
    the ports is frozen or unloaded).
  - **READY rows:** the gauges, the bursts, the link, the countdown, and a blue "Begin: Galactic Merger" line whose
    tooltip lists the conditions.
  - **Gauges:** at T7 and T8 they go in two columns (P | W, then Λ).
  - **Bursts:** a bar over 0-6 Bu/t, with a yellow tick at the link floor (1.5 or 6) or at the demand. The value's
    colour is β.
  - **Link:** the bar's value is the streak alone; "12:30/30:00" overflowed the value column. HELD is a full bar.
  - **Events:** a lit galaxy's event shows once. The countdown row names it, and the old "Event warning" row is kept
    for T1-T5.
- **Kit:**
  - a bar row ends its bar 3 px before a value wider than the value column ("184/300 s");
  - a stat row cuts a long value, so its label keeps up to 64 px whole.
- **Engine getters for the GUI:** `MTEQuasarEngine.conduitClosedWhere()` and `coilPowerKinds()`;
  `MTEConduitElement.lastNote()`; the crown's stream parts, stack line and dock note;
  `MTEHatchBurstControl.getHostLine()`.
- **Bench:** `/btsquasar gui x y z [info]` prints the console's rows.
- **Shot scripts:** `tools/guishots/p4-build.shots` and `p4.shots`.

**The client check.** The `-Dbts.guishots=true` dev client in the `bts-guishots` world. `p4-build.shots` built:

- the standard conduit rig at 20000 100 0: a quasar at coils T3, a crown, two mouths, two mirrors, a split Junction,
  a remote crown and the receptors. Every element was FORMED;
- two T1 quasars;
- an Eye T8. It was FORMED, with re-projection and damage / repair.

After a re-join, `p4.shots` photographed 21 GUIs at GUI scale 3. I looked at each against the mockup
(`gui/work/shots/quasar.png`, `hatch.png`, `eye.png`) and fixed what did not match, then re-shot everything twice.

- Fixed after the first shots:
  - labels were cut ("Bottom .", "Out (sid.");
  - route and dock names ran past the row;
  - the Burst Control help overlapped "Rising edges";
  - the receptor's machine name was cut;
  - the T7 / T8 gauges were full width;
  - a galaxy event showed twice.
- The final shots match the mockups' layout:
  - Quasar NO HOLE: the checklist;
  - Quasar STABLE / RESONANT: E, Flux with both ticks, Mass, Integrity, Spin | Bursts, the jets, Helium f, Coils;
  - Quasar RE-FORMING: Disc 205/300 s;
  - Mouth OPEN Paired;
  - Junction OPEN, then CLOSED with "Upkeep paid 0 / 10 s";
  - the Eye T7 READY Triangulum: P | W, Bursts with the tick, Link HELD, the minor-merger countdown, "Begin:
    Galactic Merger".

  The rows were also checked as text with `/btseye star … gui`. `spotlessApply build` (checkstyle) passes, and a
  lang check over the six GUI classes finds no missing key.

Shots: `work/gui/round2/shots/` (cropped to the panels) and `round2/raw/` (full window).

**Not covered.**

- **PAUSED badge.** It was not photographed: no bench pauses the galaxy's quasar. It shows the existing PAUSED line.
- **Owner NPE.** Opening a bench-built machine's GUI from the harness logs GT's `GTPowerfailTracker.getMachineOwner`
  NPE. The machine has no owner, and the GUI opens normally. This is GT's own behaviour for an ownerless machine.
- **Long lines.** A few rows still cut a long line: the quasar's "Feeds" host name, the Burst Control status, a
  last-event note. Each scrolls on hover or carries the full line in its tooltip.

**Question.** *READY rows:* READY shows the galaxy rows and the Begin line, as the mockup does. The field bars
(Stability, Integrity, Lock, Cycle) are not shown there, and come back once Begin is pressed. *Suggestion:* keep it,
since the bars are all in their final state while the Eye waits. The alternative is to add them under the Begin line
(the list scrolls).

---

### B-117 The time warp (`/btswarp`): ship it in release jars? [showcase v2, built]

*Numbered B-108 on the `showcase` branch; master had already given B-108 to the Eye's intake cap, so it is B-117
since the merge (B-118). P4-95 keeps its number.*

**What was asked.** "Why use real run time, add a way to speed it up with commands (cheat mode) so it isn't a waste
of both 80 minutes of time and credits." The showcase's long stretches (a star lighting, the First Cooling's 7,200 s
batch, a merger, the Homogeny install, the T6 -> T8 chain) were filmed in real time with frame sampling.

**Built.** `TimeWarp` + `/btswarp <N> | off | status` (docs/automation.md §4.2): up to N - 1 extra whole-server
ticks after every real tick, through the same calls `MinecraftServer.tick()` makes (FML server and world tick events,
`WorldServer.tick`, `updateEntities`, the entity tracker), within 45 ms of each 50 ms tick; re-entry guarded; the
achieved rate measured; off at server start and stop; op only (permission level 2). The recorder's `warp <N>` turns
it on for a scene and shows "TIME WARP xN (cheat)" with the measured rate and the game time.

**Proof.** Rig C, 444 s of game time, real time against x20: lit at 40 s in both, 77.2 % Resonant / 22.8 % Stable,
Integrity 100, the same 357,708 L of Raw Star Matter. The live gate rig took a kit Eye T1 -> T6 in 9,442 game s in
236 s real (x40, 0 budget hits).

**Question.** Should `/btswarp` ship enabled in release jars? **Recommended: yes, op only with cheats required, like
the other benches** (`/btseye`, `/btskerr`, `/btsquasar`): it changes nothing for a player who does not type it, it is
a real testing aid for anyone building a rig, and on a server only ops can use it. Alternatives: a config switch
(default off), or dev builds only.

---

### B-118 Merging showcase into master, the live collapse purge (P4-95), and the showcase scenes on the new GUIs [final showcase, built + test]

**What was asked.** Merge the `showcase` branch (recorder v2, `/btswarp`, the live gate rig, `showcase2.shots`) into
master; fix P4-95 (on a real quasar driven by real redstone the collapse-assist purge landed 1-2 s late, so the T6 ->
T7 Magnetar Collapse failed) and film the collapse on the real quasar; find why the Kerr Extractor sat idle in the
showcase world; give Reality Diffraction / Refab what they need or caption them honestly; re-frame the washed-out
shot inside the quasar; use the new GUIs in the scenes; frame the Eye T7 / T8 front window (B-114).

**The merge** (`git merge --no-ff showcase`, 4aa480d). Nine files conflicted, all resolved by keeping both sides
(the merge commit lists them): master's `serverStarted` and the branch's `serverStopping` handlers, master's GUI-shot
and questbook-shot harness lines after the recorder line, the bench usage strings with both sides' verbs, the
recorder's settings hold plus the branch's master volume 0, and code both sides had moved (EyeNbt, the sensor hatch,
the KEE). One semantic fix: `AutoRigEyeAE` called `EyeKit.socketRels`, which master moved to `BTSShapes`. The branch's
B-108 ("ship /btswarp?") collided with master's B-108 (the intake cap) and is B-117 now (open question 24); P4-95
keeps its number and cites B-117. `spotlessApply compileJava` passes.

**P4-95: the cause.** Galaxy Transition [4] ("purge now") was on for the single overfeed second 299, for a collapse
at 300. That lands the eruption on the collapse second only when the rig's action, the quasar's disc second, the
conduit and the Eye's second all run inside one second, as the lock-stepped bench does. Live, each hop is its own
tick and the quasar's second runs at its own phase against the Eye's. On top of that, the live rig acted at the start
of the Eye's NEXT second on the code published the second before, so its redstone block reached the Burst Control
Hatch a whole second late by construction, and the quasar purged after the Eye had already read the collapse second.

**The fix (machines).**
1. `GalaxyDirective`: the cue leads by 2 s. [4] is on for overfeed seconds 298 and 299 (`ASSIST_CUE` .. `ASSIST_LAST`,
   one rising edge); the hold [8] runs 219-297; the early purge looks ahead to 298. An eruption lasts 3 quasar
   seconds, so the collapse second sits inside it for any wiring that answers within about 1.5 s, and in the
   lock-stepped bench it is the eruption's second second. The prototype (design repo `designs/quasar-p4-prototype.html`)
   changed the same way; the fixture was refreshed and the golden regenerated: only the three "chain T6->T7 auto" cases
   move (still T7 UNLOCKED, 0 failures) and one directive hash. `quasar-golden --check --java` 38/38, fieldsim 78/78.
2. The Directive Hatch tooltip, the collapse countdown event line and the Magnetar Collapse quest (skill move) say
   when [4] comes on and why (`gen-singularity`, bundle `32326838526c`); `automation.md` §7.4 explains the timing and
   that how much a purge cancels is the quasar's stream.

**The live rig wires the cue as a player would** (`QuasarRigBench`). When `/btsquasar autorig ... eye ...` goes live it
sets the Eye's Galaxy Transition hatch to Bit 3 out, fits it a GT Advanced Redstone Transmitter (Internal) and fits
the quasar's PURGE Burst Control Hatch an Advanced Redstone Receiver (Internal) on the same frequency
(`bts-collapse-cue-x-y-z`, OR, tick rate 1). The rig's own redstone block keeps only the regulator term (Disc [4] AND
NOT Transition [8]) and skips covered sides. `autorig status` counts the collapses it saw, each with its Bu/t on the
collapse second, the shock cancelled, P after and the Eye / quasar second phase, and for the last (and every failed)
one the quasar's flux and stream at each second 290-299.

**Proof** (headless `runServer`, port 25565, fresh flat worlds `p495b` / `p495c` / `p495d`, `/btswarp 60`, achieved
x59-60). Each case: a kit Eye T8 set to a lit T6 magnetar with its milestones done (by command), a fresh Quasar
Engine T3 (coils 3, f 1.0) with its mass set, the live rig; the Eye's own Directive codes do the rest (core, chart,
Begin, the overfeed, the cue, the halo capture).

| Quasar | Phase (ticks) | Collapses (Bu/t on the collapse second, shock cancelled of 120, P after) | T7 |
|---|---|---|---|
| 64 SM fresh | 16 | 3 failed: 1.3 Bu/t, 0 (the purge on second 298 every time; the stream of a spinless 64 SM hole is 0.8 Bu/t) | no |
| 96 SM | 16 | 3 failed as the disc settled, then held: 7.2, 57, +67 | yes |
| 136 SM | 15 | 2 failed, then held: 7.8, 63, +57 | yes |
| 200 SM | 16 | 1 failed, then held: 10.2, 87, +35 | yes |
| 136 SM | 15 / 3 | #1 failed 0.8; #2 held 4.7, 32 / #1 failed 0.9; held 3.9, 24 and 7.9, 64 | yes / yes |
| 200 SM | 11 / 4 | #1 failed 2.1; #2 held 10.2, 87 / #1 failed 1.9; #2 held 10.4, 89 | yes / yes |
| 256 SM | 19 | #1 failed 3.2; #2 held 16.6, all 120, P +12 | yes |
| **Settled first** (the rig ran the quasar 600 s before the Eye was lit): 136 SM | 14 / 3 | #1 held 6.9, 54 / #1 held 6.5, 50 | yes / yes |
| 200 SM settled | 9 | #1 held 14.9, all 120, P +2 | yes |
| 96 SM settled | 17 | #1 held 3.2, 17, P +97; #2 held 5.9, 44 | yes |

In every collapse the trace shows the PURGE on overfeed second 298 and the eruption's stream on the Eye on the collapse
second (e.g. 136 SM: G 3.0-3.7 Bu/t through 290-297, 7.5 at the purge, 7.8 on the collapse second), whatever the
phase (3-19 ticks). What fails now is the stream, not the timing: a first collapse straight after a bench set the
hole's mass (the disc not settled yet, Yq low) gives 0.8-3.2 Bu/t; once the disc has run under the rig, every first
collapse held. A fresh 64 SM hole never holds it (the Stability Link milestone, skipped here by command, is what
grows the quasar first). Before the fix P4-95 recorded 29-47 of 120 cancelled at 136 SM; the same size cancels 50-64
now, and 200-256 SM cancel 87-120.

**The showcase scenes** (`tools/recording/showcase2.shots`, checked in a dev client at 1280x720 with
`-Dbts.rec.only`):
- **Chapter 7 on the real quasar.** The galaxy Eye's quasar is seeded in `q-seed`, grown to 136 SM and handed to the
  live rig at once (the Eye still dark), so its disc has settled by chapter 7. `gal-collapse` lights the T6 magnetar,
  sets the milestones and lets the rig run: in the probe T7 unlocked after 23 s of real time (x35 warp, 6.7 s of
  time-lapse), the GUI showing "the magnetar COLLAPSED". The `galaxy` bench model is no longer used; `gal-merger`
  only sets the T7 milestones and hands in the Supermassive core and chart.
- **The Kerr Extractor (not a machine bug).** On a headless server the module took a stocked bus at once in both
  orders (stocked before and after the hole opened). In the draft the stock command ran before the camera moved and
  the previous scene's x10 warp was still on: the module ran its whole batch (1,024 Stemcells go in one 448-parallel,
  x4 run of 1.7 s) before its GUI opened, so the shot showed IDLE. The scene now turns the warp off, opens the GUI,
  then stocks: RUNNING, "Processing recipe", Plasma Membrane x1.0k, then IDLE with the outputs out.
- **Reality Diffraction / Refab: captioned honestly.** A real run needs a TecTech BEC network with a Containment
  Field (a 1,236-block BEC Storage multiblock, BEC Hatches, conduit and its power) and, for Refab, Diffraction's
  regions through it. Building that in the showcase world was not done (see below). The captions say they are not
  running and why; the new GUIs show it themselves: "WAITING  BEC network", "BEC network 0, 0 storages", "Regions
  anchored 6 of 6".
- **Inside the quasar.** The camera stands in the open chamber around the hole (24 blocks out, 6 up; the chamber is
  air to r 31 at the hole's height, the first Quantum Glass at r 32), then a second angle from under the disc plane.
  The draft's camera looked through two Quantum Glass layers from outside the inner wall.
- **New GUIs.** Every `readout` (the scanner side panel) is gone: the kit GUIs show the star, gauges, batch, shield,
  transition and checklists. The Eye GUI scene's caption describes the new screen.
- **The T7 / T8 window (B-114).** `gal-window` dollies from 132 to 98 blocks in front of the galaxy's centre at its
  height: the rim's opening with the galaxy's core in it, the rail's clamp clear of it. (From 80 blocks the camera
  stands inside the rim and the opening fills the frame; the window reads best from outside.)

**Judgement calls (please confirm or change).**
- The cue's lead is 2 s (298-299). With 1 s, wiring slower than the gap between the Eye's and the quasar's seconds
  misses (P4-95); with 3 s, the collapse second falls on the eruption's last second only for some phases. 2 s puts it
  on the eruption's first or second second for any delay from a tick to about 1.5 s.
- The live rig fits GT wireless covers to the machines it drives (they stay after `autorig off`, like the kit's
  hatches). A player can wire the same with redstone dust and gates; wireless is the shortest real wire across the
  400 blocks between the showcase's Eye and quasar.
- A Magnetar Collapse can begin with a quasar whose stream cannot hold it (any size the link milestone allows). Not
  changed: the GUI's countdown already says "The shock would land at P +x with y Bu/t now; 13.5 Bu/t on the collapse
  second cancels it all".

**Not done.** A BEC network in the showcase world (a TecTech BEC Storage built by its projector, a BEC Hatch on it and
on the Diffraction, a powered field, Diffraction's six regions through it to the Refab): suggested as a `/btshr bench
<x y z> becnet` helper with its own dev-server test before it goes on camera. The full showcase was not re-recorded
here (the final recording is the next step); only the changed scenes were probed.

### B-119 Test of merged master before the final recording: static checks, a clean boot, `/btswarp`, the merged GUIs in a client [final showcase, test + fix]

**What was run.** Master `ba6f52b`: the gui-clean merge (B-115), the P4 GUIs (B-116), and the showcase merge with
P4-95 (B-118). The run had three parts: static checks, two headless `runServer` boots on port 25565, and the GUI shot
harness (`-Dbts.guishots=true`) in the `bts-guishots` world. One fix came out of it (below); it was compiled and
checked in the client again. The scripts and the shots are in my local work folder (`b119/`, `b119/shots/`).

**Static checks.**
- `spotlessApply compileJava`: passes; spotless changed nothing.
- Golden tests: `fieldsim-golden --check --java` 78 / 78, `quasar-golden --check --java` 38 / 38.
- `export-structures --check`: matches voxel.js `fa6fec2a751c`. `gen-sl-textures --check`: 126 / 126.
- `gen-singularity --check`: 139 files up to date (bundle `32326838526c`). `--selftest` passes.
- Lang lint (B-113's script, against our `en_US.lang` with 1,294 keys, and RC-2 GT's): 0 real misses, 0 duplicate
  keys, 0 empty values. Its ten reported lines are all known and dynamic:
  - `fieldRetrofitKit` and `eohTier.`, which are named per meta;
  - `kerr.module.linked` and `gg.recipe.precise_assembler`, as in B-113;
  - six Eye GUI keys that the lint sees without their suffix: `dial.tip1..5`, `feed.tip1..4`, `galaxy.mode.tip1..4`,
    `galaxy.f8.tip1..4`, `galaxy.begin.tip1..4` and `galaxy.rig1..3`. All of them exist.
- GUI lang check (new, `b119/guilint.py`): 0 missing. It covers every `K + "..."` lookup that goes through a GUI
  class's prefix constant (460), and every literal with a dynamic suffix (33).

**Boots.**
- **Boot 1** (fresh world `b119`, no pack quest database):
  - `311 registered, 0 skipped`, and "P4 recipes: every machine recipe is registered".
  - The 8 White Hole Matter H/R gate lines plus their summary. No `com.nateyburr` frame.
  - The only beyondthestars WARN lines were the questbook's "pack edit Exx skipped: pack quest ... is not in this
    world's quest database". That world had no pack book, so boot 2 was set up the way B-113 did it.
- **Boot 2** (fresh world `b119b`, the RC-2 pack's DefaultQuests copied read-only from the `rc - 2 TO THE STARS`
  instance, BetterQuesting's "Load the default quest DB on world startup" set to false):
  - `311 registered, 0 skipped`, and the same 9 gate lines.
  - **No beyondthestars WARN or ERROR line**, and no `com.nateyburr` frame.
  - 823 "Exception" lines. These come from GT's dev environment; B-109 and B-113 had the same count.
  - Questbook: 99 quests added, 64 of 64 pack edits applied, one emptied chapter hidden. `/btsquests status` is all
    ok: the plant 34 / 34, UIV 3 / 3, UMV 10 / 10, UXV 35 / 35, MAX 15 / 15, The Endgame 1 / 1.
- Afterwards the server's level name, `cmds.txt` and `betterquesting.cfg` were put back byte for byte, and the
  DefaultQuests copy was removed.

**Recipes.** `/btsrecipes status` gave the same result on both boots, and the same as B-113:
- 425 entries: 365 new, 58 changed-GT, 2 removed.
- By line: Kerr 28, Eye 64, BEC 28, H/R 27, Automation 1, old plant 202, upgrade path 41, Quasar / Conduit 34.
- Both config switches on.

**`/btswarp`.**
- `status` with no warp: "Time warp is off."
- From the console:
  - `5`: x5.0 achieved (30 s of game time in 6 s). `off` reports the run.
  - `999`: x998.2 overall on the empty world.
  - `1001` is refused ("must be at most 1000").
  - Each on and each off writes one `[BTS warp]` WARN line. That is by design: a cheat leaves a trace in the log.
- As the player in the shot client:
  - `20` held x20.0 (213 s of game time in 11 s) while an Eye burned.
  - The bench's `status` and `off` answered in chat.
  - The Eye's Lock bar showed the extra 200 s.
- Op only:
  - `CommandBTSWarp.getRequiredPermissionLevel()` returns 2, and the command keeps CommandBase's permission check.
    A player who is not an op (in single player: cheats off) should get vanilla's "You do not have permission".
  - **Not tested with a real non-op player.** The shot client is single player with cheats on, where the owner is
    always an op. A client that joins the dev server would need a second gradle run alongside the server's.

**The merged GUIs in a client** (scale 3, 1920 x 1080, sound off). Each shot was looked at.

| GUI | State | Result |
|---|---|---|
| Eye T1 | dark, Sol targeted, no charge | DARK badge, the "Sol lights when all are met" checklist (charge missing in red) |
| Eye T1 | lit (bench `lit`, the fine rig, 213 s under the warp) | RESONANT, P gauge, Stability / Integrity / Lock / Cycle bars, Y, parallel, milestones, last event |
| Eye T1 | Field panel | **values cut on the right: fixed, see below** |
| Eye T1 | Galaxy panel | renders; on a T1 Eye it shows the T7 / T8 controls, with Begin "not ready" |
| Eye T8 | a Magnetar Merger running | TRANSITION badge, the phase bar, P / Θ / Ω / Π gauges in two columns |
| Kerr T1 | open, autorig 400 s at 16 SM | OPEN, Mass / Stability / Governor bars, stock, containment, bleed, module chips |
| Quasar, coils T3 | hole open (the conduit rig) | STABLE, the E dial on the real bands, flux with the purge and 90 ticks, jets, feed, coils |
| Conduit Junction | open, nothing reaching it | OPEN, Through, upkeep, mode, In and the three outs ("no stream yet") |
| Directive Hatch | on the Eye T1, set Trims | the readout (bits 1-8, Code, redstone), and the set / output / invert / window rows |

**The finding (fixed, commit `83ecaeb`).**
- **What was wrong.** B-115 added the Buffers section, so the Eye's Field panel is now always taller than its list.
  MUI2 then shows the list's scrollbar and draws it over the list's right edge. Every right-aligned value lost about
  one character:
  - the zone legend read "x1.2 / x1.0 / no yiel / star los";
  - "8 liv", "0 refuse" and "out 1";
  - the sensor count was hidden.
- **The fix.** The rows of both side panels (Field and Galaxy) are now `ROW_W` wide: the list's width less 6 px, the
  clearance the Silo terminal uses. The legend's range column gives up the 6 px.
- **Checked in the client.** Every value is whole, and the Buffers rows and the footer are whole when scrolled.

**Seen, not changed.**
1. **The first shot session got no chunks.**
   - What happened: after every harness `tp`, the client never held a single machine tile. That included the Quasar,
     the Junction and the Eye T8, which were all on the server, and the server had put the player in the right
     place.
   - A `guinow` then opened the GUI server-side. The client could not build it and dropped out. The harness's
     automatic re-entry then failed in AE2's `serverAboutToStart` (a second integrated server in one JVM), so that
     client was stopped.
   - In the next two sessions the same `tp` / `gui` lines worked every time, including teleports of thousands of
     blocks. A structure built during the session also opened without a re-join.
   - It did not happen again. It is a harness / integrated-server issue, not the mod.
   - The world was missing the Eye T1 and the Kerr, because B-116 had created it fresh for P4; both were built again.
     In this world `/btseye t1 fill force` puts the T1 controller at 300 102 4, not at 300 98 4 as `eye-t1.shots`
     has it.
2. **GT's power-fail tracker NPE**, logged when the GUI of a machine built by a bench command (no owner) is opened, as
   in B-116. The GUI opens.
3. **Buffer names on the Field panel** are cut to one word ("Ergo.", "Molte."), with the full name in the tooltip.
   The bench's `fill` puts 10^12 L in every buffer, so a T1 Eye lists eleven of them.
4. **The Galaxy button on a T1-T6 Eye** opens the T7 / T8 panel with its controls (mode, f8, Begin "not ready"). It
   does no harm, since Begin is refused below T6, and the panel's title says T7 / T8.

**Not covered.**
- The Eye T7 / T8 Galaxy and Field panels with a real galaxy. B-116 photographed T7 READY and T8 LIT.
- The P4 crowns, mouths, mirror and Burst Control Hatch (photographed in B-116).
- The sensor hatch GUI.
- `/btswarp` from a non-op player.

### B-120 Review fixes to the Eye GUI: a merger warned during a satellite crossing, the Buffers list's order and overflow [final showcase, review fix]

Two review findings on merged master (`63f53c5`), both checked against the code and both real. Code commit
`8061701`.

**1. A T8 galaxy hid the minor merger's warning during a satellite crossing.**
- **What was wrong.** B-116 (`9bdb162`) hid the old "Event warning" row on a T6+ star whenever the countdown row had
  text, because the countdown named the same event. But the countdown picked one event only, in the order collapse,
  pericentre pass, crossing, merger. So while a crossing was warned or running (up to 420 s: 120 s warning + 300 s),
  a minor merger warned in that window showed nowhere: the countdown row named the crossing, and the warning row (the
  merger's sign) was hidden. A crossing never sets the readout's warning, so nothing else stood for the merger.
- **The fix** (`EyeGui`).
  - `eventLines()` replaces `eventText()`: one row per running countdown. The collapse or pass (a transition), the
    crossing (its countdown or the seconds left), and the minor merger can all show together.
  - The merger row counts on its own clock: the new `EyeStarRuntime.mergerCountdown()`. The old line took
    `countdownSeconds()`, which gives the crossing's seconds first, so it would have shown the crossing's time.
  - `warnNamedByEvents()`: the Event warning row is hidden on a T6+ star only when a countdown row names the event the
    warning is for. That is a collapse or pass countdown (the transition's warning wins the readout), or the merger.
    With only a crossing showing, the warning row stays.
  - `countdownSeconds()` itself is unchanged. The sensor's countdown channel and `bts_eye.getEvent()` still report one
    event (crossing first), as their single-value design has it.
- **Checked on the dev server** (headless `runServer`, port 25565, fresh flat world `b120`). An Eye T8 was built by
  `/btseye t8 0 100 0 north fill` (FORMED). Then the bench ran `eu`, `lit milkyway`, `crossing` (new, below), and
  `event` 10 s later. `gui` printed:
  - `NOTE: Satellite crossing in 108 s: hold Stable`
  - `NOTE: Minor merger in 13 s (-): W pushed, then the precession flips`
  - no separate Event warning row (the merger row names it), so nothing shows twice.
  The old code was not run; read from it, the same state would have printed only the crossing row.
- **New bench verb.** `/btseye star <x y z> crossing` warns the next satellite crossing now (its 120 s countdown), like
  `event` does for the tier event. It is refused when no star burns or the tier has no crossing.

**2. The Field panel's Buffers section could cut the fluid the star needs.**
- **What was wrong.** The section draws at most 14 rows (`INTAKE_ROWS`) and said nothing about the rest.
  `intakeRows()` listed the held buffers first, in stored order, and only then the fluids the sockets pull but do not
  hold yet. An Eye that has climbed the tiers keeps leftover buffers (a buffer that is not pulled is never drawn down,
  B-108). With 14 or more of them, a fluid the star now needs and has run out of was row 15 or later, so it was never
  drawn.
- **The fix.**
  - `MTEEyeOfHarmonyBTS.intakeRows()` lists the fluids the sockets pull now first (held ones in stored order, then the
    ones not held yet), then the held leftovers that are not pulled. The scanner's "Buffers (held / intake cap)" line
    uses the same list, so it has the same order.
  - `EyeGui` adds a last row to the section, "+n more (see the scanner)" (new key
    `beyondthestars.gui.eye.field.buffers.more`), shown only when the list has more than 14 rows.
- **Checked on the dev server.** On the lit Milky Way above (`fill` puts 10^12 L in every buffer), the scanner's list
  went from Agent, Magnetar Matter, Black Dwarf Matter, White Dwarf Matter, Ergomatter, ... (stored order, while dark)
  to Ergomatter 36,300 L cap first, then the ten leftovers marked "not pulled".
- **Not seen in a client.** The "+n more" row is client-only. That Eye listed 11 buffers, under the 14-row limit, and
  no client was run. The row's count is the list's line count less 14.

**Checks re-run.**
- `spotlessApply compileJava`: passes.
- GUI lang check (`b119/guilint.py`): 460 prefix lookups, 33 dynamic, 0 missing. Every literal `beyondthestars.gui.eye.*`
  key in `EyeGui` exists, apart from the six known dynamic-suffix prefixes (B-119), and the lang file has no duplicate
  keys.
- Boot: `311 registered, 0 skipped`, no `com.nateyburr` frame. The only beyondthestars warnings were the questbook's
  "pack edit skipped" lines, expected in a world with no pack quest book (as B-119's boot 1).
- Not re-run: the goldens (no field or quasar maths changed), the structure and texture checks (no structure or
  texture changed).
- The dev server's level name and `cmds.txt` were put back afterwards.

**Suggestion.** Keep both. A possible later step is a sensor channel or `getEvent()` field for a second running
countdown, so a rig can see a merger during a crossing without the GUI. That was not built.

---

### B-121 The final showcase recording [final showcase, recorded]

**What was asked.** Record the final showcase from master's dev client with `tools/recording/showcase2.shots`, with
the UIV / UMV quest chapters if a pack instance's quest files can be read, check a still of every scene, re-take bad
scenes, and cut a 720p copy into parts under 14 MB.

**What was done.**
- The RC-2 pack instance's `config/betterquesting/DefaultQuests` was copied read-only (the instance was not touched)
  to a scratch folder and into `run/client/config/betterquesting/`, and the run used `-Dbts.rec.questsdir`. The
  setup's `/btsquests install` put our chapters and pack-chapter quests on top. Two scenes were added, `qb-uiv` and
  `qb-umv` (the pack's Tier 11 and 12 chapters, by their UUIDs), and the chapter 9 card says what they are.
- The dev client's BetterQuesting "View mode" was turned on, as in the pack's own config. With it off, the pack
  chapters drew empty (their quests are locked in a fresh world).
- **Recorder fix (`RecClient`).** Opening a screen ungrabs the cursor to the window's centre after the recorder had
  parked it, so in the book a quest under the centre drew its tooltip over the shot (UMV, MAX), and so did NEI's
  "Recipes" label. `gui`, `questbook` and `nei` now park the mouse again once the screen is up.
- **Captions made true to the screen.** `eye-gui` now names what the T1 screen shows (pressure gauge, stability,
  integrity, lock, cycle, Y, milestones, the Field / Galaxy / Info buttons). `refab` says what its checklist says:
  it waits for a running Condensate Assembler nearby (and Diffraction's regions through the BEC network).
- One full run (about 31 minutes real time, 62 scenes, 7:04.8 of video), then two re-takes with `-Dbts.rec.only`
  (`ae-light,ae-burn,eye-gui,c9,qb` and `refab,build-nei,qb-`). Every scene's still was looked at: none black or
  flipped, no HUD, hand or debug screen, captions readable, the time warp and time-lapse tags on every warped scene.
- The live runs held: Rig C lit itself 7 s after its supply opened (x6 warp); the climbing Eye went T1 -> T6 under
  the live gate rig; the Kerr Extractor took its 1,024 Stemcells in one 448-parallel run on camera; the Homogeny
  installed T2; the Magnetar Collapse on the real quasar held ("10.72 Bu/t on the ports cancelled 92 of 120") and T7,
  then T8, unlocked.

**Known, not changed.** Opening the GUI of a machine raised by command logs GT's power-fail-tracker NPE (`FAILED:
ExecutionException: NullPointerException` in `rec.log` for the quasar, mouth and crowns); the GUI opens normally
(B-116). Reality Diffraction and Refab still do not run on camera (no BEC network in the world; B-118).

---

### B-122 Package: the playtest jar `0.24.1-master.302+362b228973` [final showcase, playtest package]

**Context.** Since `master.250` (B-105) master took P4 (B-110..B-114), the GUI kit (B-115, B-116, B-119, B-120), the
showcase merge with `/btswarp` and the P4-95 fix (B-117, B-118) and the final recording (B-121). This unit packages
that for the local playtest.

**What was done.**
- `gradlew build --no-daemon` on master `362b228`, clean tree (spotless and checkstyle ran and passed; nothing
  changed). The jar is `dist-rc2/beyondthestars-0.24.1-master.302+362b228973.jar`, 2,502,474 bytes, SHA-256
  `f29395dab058d6af1bee9b3cdb954965d79b22e32212ba1475e560061b63cb26`; `mcmod.info` says
  `0.24.1-master.302+362b228973`. It holds `TimeWarp` / `CommandBTSWarp`, the P4 machines and the dev-only recorder
  (`rendertest/RecClient`, `FrameRecorder`; loaded only with `-Dbts.rec=true`). `master.250` moved to
  `dist-rc2/superseded/`.
- **A fresh recipe dump and the load lines.** Two headless `runServer` boots (port 25565, fresh flat world `b122-rx`;
  the config, level name and `cmds.txt` were put back afterwards; script `work/b122/run-dumps.sh`), both on the
  dev classes of this same commit (the dump's `mod` field is `0.24.1-master.302+362b228973`):
  - shipped flags: `311 registered, 0 skipped`, "P4 recipes: every machine recipe is registered", SL-Q26 (two lines),
    "Eye of Harmony: 10 live stars, 43 planet recipes", the 13 OpenComputers components, the 8 White Hole Matter gate
    lines (SL-C04, SL-H20, SL-H21, SL-R01..R05) plus the summary, and no `[beyondthestars]` WARN / ERROR except the 64
    "pack edit skipped" lines of a world with no pack book. No `com.nateyburr` frame. `/btsrecipes status`: `Total 425
    | by status {new=365, changed-GT=58, removed=2}` (Kerr 28, Eye 64, Quasar / Conduit 34, BEC 28, H/R 27,
    Automation 1, upgrade path 41, old plant 202), as B-113;
  - both flags off: 309 registered, `Total 408 | {new=406, changed-GT=2, removed=0}`; gone are the 10 BHC swaps,
    SL-E01 / SL-K01 removals, SL-G01, SL-P33, SL-P40 and SL-Q26.
- **`dist-rc2/INSTALL.md`** (CRLF kept): the new jar name, size and hash everywhere (header, the local install block,
  the dedicated-server command); a new 0.1 "Since `master.250`" (the Quasar Engine; the Conduit and crowns; Eye T7 / T8; SL-Q26;
  the P4 quests; the new controller GUIs; the Eye intake cap; restart persistence; the casing tooltips; `/btswarp`; the
  P4-95 fix; the dev-only recorder); the old 0.1 / 0.2-0.3 folded into 0.2 / 0.3. Section 3: 311 registered, the
  P4 and SL-Q26 lines, 10 live stars, 13 OC components, the 8 White Hole Matter gate lines by id, 425. Section 4:
  SL-P40 / SL-Q26 under the Eye flag, 408 with both flags off. Section 5: the UXV chapter's P4 quests, Riding the Rift
  in MAX. Section 6: every GUI on the kit, the Eye table to T8 with the two galaxy transitions, the intake cap and the
  GUI rows, a new 6.7 for the Quasar / Conduit / crowns with their MTE ids and the bench chunk-loading note. Section
  7: P4 out of "not in this build", P5 and the Empyrean redesign in. Section 8: SL-Q01..Q28, SL-P40, SL-Q26, SL-B16.
  Section 9: the GT owner NPE on bench machines, the cut GUI lines, the one-event sensor channel / `getEvent()`, the
  large T7 / T8 caps, a collapse begun on a weak quasar, the harness switches; the "seen in a client" list updated.
  Section 10: `/btsquasar`, `/btsconduit`, `/btswarp`, `/btskit quasar|conduit`, `/btseye t1..t8`, the projector
  stacks. Section 11: "from `master.250`".
- **Playtest guide** (local work folder `playtest/playtest-guide.html`, script `playtest/refresh302.py`, backup
  `playtest-guide.master250.bak.html`):
  - the header's jar, hash and commit; a "New since `master.250`" note naming the steps whose expectations changed;
  - A1 / A2 (the new jar; 311, the 8 White Hole Matter gate lines, SL-Q26, 13 components), B1 / B2 (the P4 quests),
    C1 and D1 (the new GUI layout), D3 (the Buffers section, the sensor's 20 channels), a new **D7** (the intake cap),
    H1 (the hatch GUIs), R1 (425);
  - three new phases: **Q** Quasar Engine, Conduit and crowns (Q1-Q4: build and GUI, seed and the disc, the conduit
    rig and crowns, the disc's hatches), **T** Eye T7 and T8 (T1-T4: build and the dark checklist, the Magnetar
    Collapse on a real quasar under the live rig and `/btswarp`, the Galactic Merger and the Milky Way, planets mode
    and companions; the commands are the ones the showcase scenes ran), **W** time warp, restarts and tooltips;
  - the chapters table (UXV's P4 quests, Riding the Rift), the command card (Eye T8, Quasar, Conduit and crowns, Time
    warp), open questions 21-24, row 14 naming the recorder, and the footer.
  - "All new recipes" rebuilt with `recipedump/build_section.py` from the fresh dumps (its `GROUPS` gained "Quasar /
    Conduit", or that line would have had no chip): 425 entries.

  Headless Edge (Playwright, `msedge` channel) in light, dark, 1280 px and 390 px: 39 steps (Q 0 / 4, T 0 / 4, W 0 /
  3), no duplicate step ids, "425 of 425 recipes shown", the Quasar / Conduit chip filters to 34, ticking Q1 counts,
  no console or page errors, no broken images, no horizontal overflow.

**Judgement calls.**
1. **The tick storage key stays `bts-playtest-master235`**, as in B-105, so ticks already made survive; the new note
   names the steps to re-check (A1, A2, B1, B2, C1, D1, D3, H1, R1).
2. **The guide's T2 is a bench recipe, not a survival one.** It sets the T6 milestones done and uses the live rig, as
   the showcase did, because the Stability Link takes hours of a real link; the steps say so. A Magnetar Collapse
   wired by hand stays on the "not seen" list.
3. **No dev-client or Prism run for this unit.** The code is the one B-119 / B-120 tested and B-121 recorded; only
   docs changed after it. The Prism instance was not touched (its `mods` still holds `master.93`, read only), so
   INSTALL.md keeps the `master.93` path first.
4. `dist-rc2/` is not in a git repo (the design repo leaves it untracked), as before.

**Suggestion.** Install `master.302` instead of `master.250` and run the guide from A1; phases Q and T are the
post-UEV gameplay you asked to see.

---

## Where the old question numbers went

| Old | Now | Old | Now | Old | Now |
|---|---|---|---|---|---|
| Q-1 | B-14 | Q-14 | B-2 | Q-27 | B-14 |
| Q-2 | B-6, §2 | Q-15 | B-13 | Q-28 | spec, top section |
| Q-3 | B-2, §2 | Q-16 | B-3, B-12, §2 | Q-29 | B-3, B-4 |
| Q-4 | §2 | Q-17 | B-14 | Q-30 | B-9 |
| Q-5 | B-5 | Q-18 | B-14 | Q-31 | B-5 |
| Q-6 | B-5 | Q-19 | B-11 | Q-32 | B-14 |
| Q-7 | §2 | Q-20 | B-3, §2 | Q-33 | B-2 |
| Q-8 | B-8, §2 | Q-21 | B-14 | Q-34 | B-10 |
| Q-9 | A-1 | Q-22 | A-1 | Q-35 | spec, top section |
| Q-10 | A-2, B-1 | Q-23 | B-14 | Q-36 | B-14 |
| Q-11 | B-6 | Q-24 | B-1 | Q-37 | B-14 |
| Q-12 | B-7 | Q-25 | B-8 | Q-38 | A-2, B-1 |
| Q-13 | B-6, §2 | Q-26 | B-8 | | |

§2 is section 2, "Settled" (it replaced the old Part C). The full original wording of Q-1..Q-38 is in git history
(addon commit 457bbf8).

**Kerr redesign questions (merge of `kerr-redesign`, B-58).** The branch numbered its questions B-33..B-44, which
master had already used for the RC-2 package and D-49 entries. They are now B-46..B-57: old B-33 -> B-46, B-34 -> B-47,
B-35 -> B-48, B-36 -> B-49, B-37 -> B-50, B-38 -> B-51, B-39 -> B-52, B-40 -> B-53, B-41 -> B-54, B-42 -> B-55,
B-43 -> B-56, B-44 -> B-57. Commit messages on the branch keep the old numbers; docs and code comments use the new ones.

**Homogeny / Reality questions (merge of `homogeny-reality`, B-81).** The branch numbered its questions B-60..B-63 and
B-65..B-67, which master had already used for the D-49 review fixes, the BEC expansion, the render QA and the
automation pass. They are now B-74..B-80: old B-60 -> B-74, B-61 -> B-75, B-62 -> B-76, B-63 -> B-77, B-65 -> B-78,
B-66 -> B-79, B-67 -> B-80. Commit messages on the branch (and the p4-quasar branch's copy of these entries) keep the
old numbers; master's docs and code comments use the new ones.

**Automation photo rigs (merge of `autoshots`, B-107).** The branch numbered its one entry B-72, which master had
already used for the review fixes after the dev-server test. It is now B-106 (B-100..B-104 are left for the
`gui-clean` branch, B-105). Commit messages on the branch keep the old number; docs use the new one.

---

## Part D. Tidy pass on the older multiblocks (branch tidy-old-multis)

Questions from the presentation tidy of the nuclear plant and the production halls
([`tidy-old-multis.md`](tidy-old-multis.md)). They keep that plan's TQ numbers. All settled 2026-10-13 ("You have full
free reign on this", B-85); TQ-5, TQ-6 and TQ-8 change gameplay a little.

### TQ-1 Rename the REPROCESSING milestone line? [tidy unit B]

**Context.** The line counts Extraction Derrick loads and Enrichment Annex batches, not the PUREX Canyon's
reprocessing. The tidy pass fixed only its unit text ("Derrick loads + Annex batches").

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): it shows as **ORE PROCESSING** (milestone
line, tree caption "ORE PROC.", questbook). The enum and its NBT key stay `REPROCESSING`, so progress carries over;
`/btsmilestone` takes "ore" as well as "reproc".

### TQ-2 Rename the "Enrichment Train"? [tidy unit A]

**Context.** The Enrichment Train (MTE 28103) leaches Uraninite with Hydrofluoric Acid into Yellowcake. It does no
enrichment; enrichment is the Cascade Hall. The name sends players looking in the wrong place.

**What we built.** The name is unchanged. The tooltip's machine type is now "Yellowcake Mill", and a line says
"Despite the name it does not enrich; enrichment is the Cascade Hall."

**Suggestion.** Rename it to "Yellowcake Mill". GregTech writes an MTE's display name into the player's
GregTech.lang on first run and never refreshes it, so the rename only reaches new installs (or players who delete
that line). The questbook text must change with it.

**Alternatives.** Keep the name and rely on the tooltip line (as built).

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **Yellowcake Mill**.

### TQ-3 Rename the "Spine Sensor"? [tidy unit A]

**Context.** The sensor sits in the Spiral chamber's roof, not on the Spine, and it reports accepted turbine cycles.

**What we built.** The name is unchanged. Its tooltip now starts with its place: "Optional: Spiral chamber roof,
front centre".

**Suggestion.** "Spiral Cycle Sensor". The same GregTech.lang stickiness applies as in TQ-2.

**Alternatives.** Keep the name (as built).

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **Spiral Cycle Sensor**.

### TQ-4 Propellant "Lv 1-6" reads like GT's LV voltage tier [tidy unit A]

**Context.** Propellant cartridges and fluids are named "Lv 1" to "Lv 6". In GTNH "LV" means Low Voltage, so
"Propellant Cartridge Lv 3" reads like an LV item.

**What we built.** Nothing changed.

**Suggestion.** "Mk I" to "Mk VI". The item names are plain lang keys and update everywhere. The fluid names are
GT fluids, whose local names are written to GregTech.lang once, so those only change for new installs.

**Alternatives.** "Grade 1-6", or keep "Lv".

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **"Grade 1-6"**, the alternative, not "Mk I-VI": the rungs are made from GoodGenerator's Naquadah Fuel MkI / MkIII (Grade 2 from MkI, Grade 3 from MkIII), so "Mk" numbers would clash with the fuel that feeds them.

### TQ-5 Default tier for /btsbuild and /btskit [tidy unit C]

**Context.** `/btsbuild` with no argument raises a tier-3 plant; `/btskit` with no argument hands out the kit and
bill of materials for a tier-1 silo. Both are bench commands, so a tester who runs one after the other gets two
different plants.

**What we built.** The defaults are unchanged. Both commands now tab-complete their tier argument, so the tier is
one keystroke away.

**Suggestion.** Default both to tier 1: the kit is the "start from nothing" path, and tier 1 is the smallest plant
to raise and walk.

**Alternatives.** Default both to 3 (a mid-size plant shows every piece), or keep them different (as built).

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **both tier 1**.

### TQ-6 Enable input separation on the halls? [tidy unit C]

**Context.** The four production halls did not offer GT's input-separation button.

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): they offer it, and it starts **off** on new
and existing halls (GT would default it on for a new machine), so buses keep pooling as before until a player turns it
on.

### TQ-7 Rename the upgrade node "Extra Centrifuge Stage"? [tidy unit B]

**Context.** The node raises the Enrichment Annex's batch; there is no centrifuge stage.

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **Extra Leach Tank** (tree label "Leach
Tank"). The enum name stays, so bought nodes carry over.

### TQ-8 Should the production halls honour the Power Panel's parallel limit? [tidy review, L5]

**Context.** The Fuel Fabrication Hall, Site Batching Plant, Cascade Hall and PUREX Canyon show GT's Power Panel,
whose "max parallel" field is saved in the controller's NBT. The halls' processing logic reads
`getMaxParallelRecipes()`, so that field has never done anything on them. GT's own multis read
`getTrueParallel()`, which is `max(1, alwaysMaxParallel ? max : min(max, powerPanelMaxParallel))`.

**What we built.** Nothing changes. Tidy B8 switched the halls to `getTrueParallel()`, but a player who had unticked
"max" while it did nothing would suddenly run at the lower number, so the review reverted it as a gameplay change.

**Suggestion.** Switch to `getTrueParallel()` in a gameplay pass (it is how every GT multi behaves), and at the
same time make the terminal "Parallel:" line and the scanner print the effective value (`getTrueParallel()`).

**Alternatives.** Keep ignoring the limit and hide the Power Panel's parallel field on the halls.

**Settled 2026-10-13 (your answer: "You have full free reign on this").** Built (B-85): **`getTrueParallel()`**, and the GUI's Parallel line and the scanner print that value.

---

## Part E. P4: the Quasar Engine, the Conduit and Eye T7-T8 (branch p4-quasar, P4-01..P4-94)

P4 is the Quasar Engine, the Conduit and Eye T7-T8, built from `designs/docs/quasar-p4.md` (rev 7f91607, D-50). Every
PROPOSED line there is accepted. These are the judgement calls the P4 units made while building it, numbered P4-01 up.
Each gives the context, what was built, a suggestion and the alternatives.

### P4-01 The quasar's Burst Control sockets sit on the cap rims, not where the mockup drew the old hatches [structures]

**Context.** §7 says the two `ergohatch` cells at y 81, (0, 81, ±8), become `c` Burst Control sockets. §2 also puts
a docked crown's base on the slot layer from y 81, and that base disc is radius 6-11, so it covers (0, 81, ±8).

**Built.** The `c` sockets are at (0, ±80, ±12), on each cap's rim, where two of the four old GRB cells were. The other
two GRB cells, (±12, ±80, 0), are casing. There are 2 sockets per cap and 4 in all. The exporter checks that the 4 rim
sockets keep an open face with a Mk III crown docked on both ports.

**Suggestion.** Keep this.

**Alternatives.** Keep the sockets at y ±81 and dock crowns 2 blocks higher (y 83). That moves the slot cell away from
the doc's y ±81.

### P4-02 The jet-port slot contract [structures]

**Built** (`manifest.json` `machines.quasar.slots.{top,bottom}`):

- The **port** cell is the jet axis just past the cap: voxel (0, ±81, 0).
- A **crown** docks with its base centre on the port cell. Its controller stands at voxel (12, ±81, 0) and faces +x,
  away from the axis. On the bottom port the crown hangs upside down: the same shape, flipped vertically.
- A **Wormhole Mouth** docks with its throat centre on the jet axis and its throat normal along the port's axis.
- The manifest gives every value as an offset from the quasar controller, in its (a, b, c) frame. For the crown it
  also gives `machines.crown.intake` (its base centre, from the crown controller) and `stack` (where a second crown's
  controller stands).

**Suggestion.** The Java link check: a dock is linked when its intake (for a crown) or its throat centre (for a mouth)
is on the port cell, and it faces the right way. The doc's "controller on the slot cell" holds for crowns.

**Also.** The crown's drawn energy hatch is dropped. EU goes to wireless (D-32) and the crown draws no power. The crown
keeps its maintenance socket.

### P4-03 The Eye's receptor groups: one char `r`, with the group in the manifest [structures]

**Context.** §7 says one `r` char, with each cell's group tagged in `manifest.json`. One char per group would have
avoided a position lookup (the Reality Diffraction anchors use one char per region).

**Built.** The doc's wording is kept: `r` in `eye_rail` and in `eye_halo`. Each socket entry under
`pieces.<piece>.sockets.r[i]` carries a `group`:

- `rail-high`: the rail's +z half, which the 8° tilt raises; 4 cells.
- `rail-low`: 4 cells.
- `halo`: the 8 shard roots.

The Java side turns each `rel` into a world position with `ExtendedFacing.getWorldOffset` and matches a found hatch by
its position.

**Alternative.** Use three chars, for example `r` (high), `l` (low) and `a` (halo). That is a one-line exporter change
plus three private elements.

### P4-04 The Eye's Directive and slot-bus sockets reuse plinth deck cells [structures + Eye definition]

**Built.**

- Four `d` cells sit in the plinth's outer face in the row above the controller: voxel (−2 / 0 / 2 / 4, −39, 39). They
  are for Field, Events, Transition and one spare.
- One `p` cell (the slot bus) sits below it: (0, −41, 39).
- These five cells were deck before P4, and both elements fall back to the plinth casing, so every Eye built before P4
  still forms.

StructureLib rejects a shape char that has no element, so `MTEEyeOfHarmonyBTS.buildDefinition` gained two minimal
elements:

- `d`: `EyeHatchElement.Directive`, else the temporal casing.
- `p`: `EyeHatchElement.Items` (an input bus), else the temporal casing. This is a `TODO(P4 Eye unit)`: `p` becomes a
  private Slot Bus element for the planet items and companion charts.

The Eye's `n` sockets still take Directive Hatches too, as the automation pass built them.

### P4-05 Which coils are tiered [structures]

**Built.**

- The quasar's tiered coils, channel `coil` (char `H`, 3,354 blocks, every coil one tier): the cage coil stacks, the
  dome rings and the hanging disc rings.
- Jet Collimator Rings (char `C`, untiered): the 16 rings round the jet towers (596 blocks), and the coil cells on the
  crowns and the conduit elements.

**Cost flag for the recipe unit.** The quasar takes 27,815 Eddington Wall Casings (both walls and the collars), 10,730
BHC casings and 6,392 frames. The Junction takes 7,145 Casimir Plates (its chamber lining). Cost the per-block cards so
these totals stay sane (rule 6). The design card (Eddington Wall Casing ×2 for 4 dense BDM plates) costs about 55,000
dense BDM plates for one quasar.

### P4-06 Junction sockets moved [structures]

**Built.**

- The two `c` sockets are at (0, 15, ±2), beside the controller. §7 put them at (±2, 15, 0), but those two cells are
  the maintenance and energy hatches.
- Two of the six fuel hatches were sealed inside the big throat's wall, at 150° and 210°. They moved to 130° and 230°.
- All six now sit on the chamber shell at r 15. They floated at r 16 before.

### P4-07 The conduit elements' energy sockets are optional [structures]

**Context.** The Mouth and the Junction are drawn with an energy hatch, but §3 gives the conduit no EU upkeep.

**Built.** The `e` socket is exported with min 0.

**Suggestion.** The MTE units ignore EU; drop the socket in a later mockup pass if nobody needs it.

### P4-08 Intended blocks [structures]

| Machine | Cells | Block |
|---|---|---|
| Conduit | shells (the mockup's Eddington cells) | TecTech Reinforced Spatial Structure Casing (§6.2) |
| Conduit | outer rings | Reinforced Temporal Structure Casing |
| Quasar | cage casing | the BHC's Extreme Density Space-Bending Casing |
| Quasar | frame | Naquadah Alloy frame (the Kerr family) |
| Quasar | tank walls | the BHC's Background Radiation Absorbent Casing |
| Quasar | glass | Quantum Glass |
| Mirror | mirror | the Kerr's Superradiant Mirror Casing |

- §6.2's "Casimir-lined conduit casing" is not needed: the existing Casimir Plate (slCasing:14) plays it.
- New blocks: Eddington Wall Casing, Disc Field Coil T1-T4, Rim Gas Injector, Jet Collimator Ring, Burst Thermaliser
  Casing and Radiator Petal.

### P4-09 The Directive Hatch keeps its built UIV recipe [design]

**Context.** §6.3 cards the Directive Hatch at UXV. The automation pass already built it at UIV (SL-A01), because the
Kerr and every Eye tier use it.

**Built.** The design card shows SL-A01 as built.

**Suggestion.** Keep UIV.

### P4-10 Conduit export frames [structures]

**Built.**

- The Mouth is exported from the left mouth of the pair (centre x −44, throat normal +x). A pair is two copies of
  the same shape.
- Every element is exported in its controller's frame, with its throats in `machines.conduit_*.throats`: centre, radius
  and outward normal, in (a, b, c).
- Junction throats: the big throat is in for split and out for combine; out1 / out2 / out3 are at 0° / +60° / −60°,
  and out3 is the fixed share taken off the top.
- The Junction's controller is on top of the chamber, and its piece front is voxel +z.

### P4-11 Stacked crowns use the same shape [structures]

**Built.** The exporter checked a second crown stacked at its core top − 2. It shares no cell with the first at
Mk I, II or III, in the same rotation. The mockup draws it rotated half a petal, but that rotation is not needed, so
one shape serves both. The offsets of the second crown's controller are in `manifest.json` `machines.crown.stack`:
9, 14 and 18 up.

### P4-12 Two stale design cards [design]

- The design card for the Galactic Upgrade Core listed 4 GRB Receptor Ports. A port is now a UXV part, a loop at
  T3 → T4, so the card swaps them for Casimir Plates. Check the addon's SL-E04 for the same thing (it was built from the
  BEC cards, so it should be clean).
- The design card SL-H10 now matches the built recipe, gated on Black Dwarf Matter (ruling 19a).

### P4-13 Sizes as exported [structures]

| Machine | Size | Blocks |
|---|---|---|
| Quasar | 143 × 161 × 144 | 51,984 |
| Crown Mk I | 33 × 17 × 33 | 1,835 |
| Crown Mk II | 49 × 23 × 49 | 2,811 |
| Crown Mk III | 65 × 29 × 65 | 4,422 |
| Wormhole Mouth | 39 × 42 × 10 | 1,404 |
| Conduit Mirror | 23 × 13 × 24 | 1,703 |
| Conduit Junction | 89 × 30 × 83 | 17,233 |
| Eye T7 | 183 × 59 × 175 | 67,367 |
| Eye T8 | 196 × 73 × 218 | 71,076 |

- The quasar is 161 tall, not the mockup title's 162, because the y 81 hatches are gone.
- The Eye's T7 rail adds 15,432 blocks and the T8 halo adds 3,709.
- All fit BlockRenderer6343's y 0-255.

### P4-14 One engine definition for both prototypes [engine]

**Context.** The P4 prototype carried its own copy of the D-49 ENGINE block, taken before the T6 Agent ruling (the
upkeep charges the shield, +2r per s). The star-lifecycle prototype had the ruling but not the P4 hooks.

**Built.** One ENGINE block, the same text in both pages (design repo 330e4df): the P4 hooks (W and Λ, per-gauge σ,
burst terms, per-second and cycle hooks, the satellite crossing, the galaxy transitions) plus the +2r shield. The T7/T8
tier tables, the quasar and the conduit stay in the P4 page's P4 block, cut to `tools/fixtures/quasar-engine.js`. The
golden tools check the engine sha on both pages. The 78 T1-T6 expectations are unchanged byte for byte.

**Consequence.** The P4 page's T6 numbers now include the +2r charge. The T6 → T7 chain still reaches T7 on every
seed (3 of 3 in the golden, 5 of 5 in the rig proof).

**Suggestion.** Keep this.

**Alternatives.** Copy the P4 block into the star-lifecycle page too. That gives two copies to keep in step.

### P4-15 The golden relights with the engine's relight() [engine]

**Context.** The local work folder harness that produced the doc's tables relit a lost Eye inline: no `light()`, so no new
event draw and no new cycle charge. The T1-T6 golden tool calls the engine's `relight(S)`.

**Built.** `tools/quasar-golden.js` calls `relight(S)`, as the T1-T6 tool does and as the page's button does. Runs
with no failures match the doc's tables exactly: T7 standard 84 / 16 % Resonant / Stable with Y 1.21; T8 at 120 SM
has 1, 0 and 0 failures with Y 1.13. Runs with failures differ slightly from the doc's counts.

**Suggestion.** Keep this. The doc's failure counts are illustrations, and the golden pins the engine.

### P4-16 A seeded hole starts at a = 0 [engine]

**Context.** §1.1 seeds the hole at 64 SM with a = 0. The prototype and every simulated table start at a_eq (about
0.5 at coil 3). With a = 0, G0 = λc·a²·b²·(M/64)² is 0 at first, and a relaxes over 1,800 s.

**Built.** `QuasarState.seed(M0, a0)` takes the spin. The MTE should pass `QuasarParams.SEED_SPIN` (0), per the doc.
The golden passes a_eq, as the prototype does. First Light comes in the first seconds because G > 0 as soon as a > 0.
Full output takes about an hour of spin-up.

**Suggestion.** Keep a = 0, and show "spinning up" in the GUI while a < 0.9·a_eq.

**Alternatives.** Seed at a_eq, which matches the simulated tables from the first second.

### P4-17 The Eye's galaxy transitions get their own Directive set [engine + automation]

**Context.** §8.1's Transition set gives bits 3 and 4 as "purge now" and "hold" (the collapse assist). The built Eye
Transition set (automation pass, B-70) already uses bits 3 and 4 for "next core wanted" and "Tachyon wanted" on
T1-T6. The Magnetar Collapse starts from T6, so the two tables would collide on the same tier.

**Built.** A new set, **Eye: Galaxy Transition** (`EYE_GALAXY_TRANSITION`): [1] ready to begin, [2] running, [4] purge
now, [8] hold the regulator's purges. The built Eye Transition set is unchanged. The other P4 sets are also new:
Quasar: Disc (with a new host, QUASAR), Eye: Field, Eye: Events and Eye: Slots. They are appended to `DirectiveSet`,
whose NBT keeps names. The bit rules are `QuasarDirective.disc` and the `GalaxyDirective` functions.

**Suggestion.** Keep this. A rig picks the set it needs, and nothing that is built changes.

**Alternatives.** Make the Transition set's bits 3 and 4 depend on the tier, as the Trims set does. Labels would change
under a running rig.

### P4-18 Slots bit 4 is "link test fire wanted" for both links [engine + automation]

**Context.** F-6 says the Stability Link at T6 needs J1 at centre 1.0, "everything to the Eye". §8.1's Slots [8] is
"Heavy Link wanted" at T7 only. An unattended chain from T6 would leave J1 at centre 1.0 after the collapse. That
over-delivers at T7: on the all-binary wiring the galaxy runs Stable instead of Resonant.

**Built.** `GalaxyDirective.testFireWanted` sets Slots [8] while the next link is not held and its core and charge are
in place:

- At T6 it asks for the Stability Link. The rig forces J1 to 15.
- At T7 it asks for the Heavy Link. J1 goes to 15 and J3 to its 75 % preset, as in F-7.

J1 keeps the standard centre 0.5 through the whole chain. The rig proof runs 5 of 5 on both chains.

**Suggestion.** Keep this. Questbook: "wire Slots [8] to J1's Burst Control Hatch (OR) and to J3's 75 % preset".

**Alternatives.** Keep the bit T7-only. The player then sets J1's centre to 1.0 at T6 and back to 0.5 after the
collapse.

### P4-19 The Events set stays 0 below T7 [engine]

**Context.** §8.1 lists the collapse among the Events countdowns. The prototype computes Events only at T7+, so the
T6 collapse countdown does not show there.

**Built.** As the prototype. During the overfeed the collapse countdown shows on the Galaxy Transition set as the hold
window ([8] from T−80 s) and on the field's warning (the T6 Status set's "event warned" bit).

**Suggestion.** Keep this. Nothing in the standard rig reads it.

**Alternatives.** Raise Events [4] at T6 during the countdown. That needs a prototype change and a new golden.

### P4-20 Purges are edges and latches in the mod [engine]

**Context.** The prototype's regulator purges in any second where the condition holds (Φ at the setpoint and not held),
and the collapse assist sets a force flag. In the mod a purge is a rising redstone edge on the Burst Control Hatch, or
an OpenComputers call.

**Built.** `QuasarState.requestPurge()` latches a request. The next disc second that is not re-forming consumes it,
and a purge below Φ 20 is refused (`purgeRefused`). The golden passes the regulator as `QuasarInputs.regulatorPurge`
(level), exactly like the prototype. The rig proof wires the hatch's rising edge. The results are the same, because a
purge resets Φ, so the condition drops the next second.

**Suggestion.** Keep this.

### P4-21 The collapse shock: 80 in the golden, 120 in the rig proof [engine]

**Context.** The prototype's default shock is +80. F-2 recommends +120, the value the doc's assist proof used.

**Built.** `MagnetarCollapseOverlay.DEFAULT_SHOCK` is the adopted +120, and `PROTOTYPE_SHOCK` is 80, the same
pattern as the Core Collapse's adopted 150 against the prototype's 90. The golden uses 80, and `QuasarRigSim` uses 120.
The plain-rig golden case (assist off, +120) loses the magnetar, as F-2 says.

**Suggestion.** Keep +120. The Eye unit constructs the overlay with `DEFAULT_SHOCK`.

### P4-22 Additions that are not in the prototype [engine]

**Built.** None of these draw from the RNG or touch the golden numbers:

- The hole states NONE and DORMANT (§1.1's Collapse and re-open). A dormant hole is frozen and draws no coil power.
- The mass milestones 128 / 415 / 1,024 / 4,096 SM (§5.1).
- `purgeRefused`, `massCapped` and the readouts in `QuasarResult`.
- `BurstLink.sigma` and the host demands for the Kerr, the Homogeny and Reality Diffraction (R.2). Their effects (bleed
  ×(1 − 0.75·min(σ, 1)), the speed bonuses) belong to the host units.
- The parallel p in the T7/T8 feed, product and bonus. It is 1 in the prototype, where the forms are identical.

**Suggestion.** Keep this.

### P4-23 Ergomatter and Superdense Ergomatter now belong to P4 [content]

**Context.** The Homogeny / Reality branch registered `btsergomatter` and `btssuperdenseergomatter` as placeholders
(`SLRealityFluids.ifAbsent`, H-24) so its recipes showed in NEI.

**Built.** `fluids/SLQuasarFluids` registers both under the same frozen names and colours, before `SLRealityFluids`.
`SLRealityFluids.ergomatter` / `superdenseErgomatter` are now aliases of P4's fields, so the H/R code reads the same and
neither is a placeholder any more. The H/R gate log no longer names them. White Hole Matter stays a placeholder (P5).

**Suggestion.** Keep this. The P5 unit does the same for White Hole Matter.

### P4-24 Q-22 failed: MagMatter is not attainable before T7 in RC-2 [recipes, design]

**Context.** Q-22 asked to check that MagMatter is attainable before the tier that needs it, and §6.5 named the check.

**Found (RC-2 sources).** The only MagMatter source is the Godforge's Heliofusion Exoticizer in magmatter mode
(`MTEExoticModule.generateMagmatterRecipe`: molten Time + molten Space). That mode needs the Godforge upgrade **EE**
(`GodforgeMath`: `setMagmatterCapable(data.isUpgradeActive(EE))`). EE's extra cost (`tectech/loader/recipe/Godforge`)
is 64 White Dwarf + 64 Black Dwarf Matter frames, **16 Eternity frames** (Eye T7 with SL-Q26) and **2 Universium frame
boxes** (Eye T10), plus 48 Stabilisation Field Generators tier 6 and other parts. So MagMatter waits for Eye T10.

**Built.**

- The Intergalactic Upgrade Core (SL-Q21) takes §6.5's fallback: no MagMatter Condensate; SpaceTime Condensate rises to
  1,120 ingots (161,280 L, the card's 128,000 L + 32,000 L, rounded to whole ingots).
- The optional Disc Field Coil T4 (SL-Q05) keeps MagMatter (§6.3, "tier 4 is optional"). It is reachable only after
  T10 until EE is rewritten. Its tooltip says "optional".
- The gate dump reports T4 coils without judging them.

**Suggestion.** Under `replaceTecTechEyeOfHarmony`, rewrite the EE upgrade's 2 Universium frame boxes to 2 frames of a
pre-T7 product (Magnetar Matter has no frame; e.g. 8 Black Dwarf Matter frames), the same D-48 item 2 spirit that keeps
MagMatter in UXV. Then MagMatter (and T4 coils) arrive at T7 with Eternity. **Alternative:** give the T4 coil a
different metal (Superdense Ergomatter would make it a T8 part).

### P4-25 UXV parts on pre-T7 cards: the B-72 UMV stopgap [recipes]

**Context.** RC-2's Field Generator / Sensor / Emitter UXV each need 576 L molten Universium + 576 L MHDCSM, and the UXV
Machine Casing needs 4 MHDCSM + 4 MagMatter plates. You chose "Ship UMV stopgap" (ruling 22a) until the UXV-part
brainstorm.

**Built.** Every P4 card needed before T7 takes **twice as many UMV parts** for the card's UXV parts (Quasar Engine FG /
Emitter / Sensor 32 → UMV 64 each; Throat Mk I FG 2 → 4; Mk II 8 → 16; Intergalactic Core FG 32 → 64; Triangulum and
Milky Way charts Sensor 2 → 4). The GRB Receptor Port and the Burst Control Hatch use a **Machine Hull UMV**, are
Assembler recipes at UMV, and take Sensor UMV ×4 / Sensor + Emitter UMV ×2. UXV circuits stay (RC-2's own Eye T5/T6
compression generators take them; the gate dump passes them).

§6.5's "UXV Superconductor Wire takes the Agent" (ruling 19b) has nothing to change in the build: the pack stops at
UMV superconductors and no recipe makes a UXV wire. The P4 cards that named it (Disc Field Coil T4) take UMV wire
(×16 per 32 coils, twice the other tiers).

**Suggestion.** When the UXV-part design lands, move these cards back to UXV parts in one pass (they are all in
`BTSRecipesQuasar`, marked "B-72 stopgap").

### P4-26 The block recipes are Condensate Assembler batches, priced per block [recipes, costs]

**Context.** P4-05: one quasar takes 27,815 Eddington Wall Casings and 3,354 coils; a Junction takes 7,145 Casimir
Plates. The cards were single-block assembler / assembly-line recipes (the Eddington card alone was about 55,000 dense
Black Dwarf Matter plates per quasar; the Jet Collimator card 8 Field Generators per ring).

**Built** (ruling 11b, the SL-B32 Bastion pattern, about RC-2's BEC casing time per block):

| Id | Block | Per craft | Time | Per block |
|---|---|---|---|---|
| SL-Q02..Q05 | Disc Field Coil T1-T4 | ×32 | 2,400 / 3,000 / 3,600 / 4,200 s | 75-131 s |
| SL-Q06 | Eddington Wall Casing | ×64 | 2,400 s | 37.5 s |
| SL-Q07 | Rim Gas Injector | ×8 | 3,600 s | 450 s |
| SL-Q08 | Jet Collimator Ring | ×32 | 2,400 s | 75 s |
| SL-Q17 | Burst Thermaliser Casing | ×64 | 2,400 s | 37.5 s |
| SL-Q18 | Radiator Petal | ×64 | 1,800 s | 28 s |

Each keeps the card's identity in batch form (the Eddington wall's Black Dwarf Matter as 72 L of condensate per 64;
the coil's tier metal as frames + condensate; the collimator's Disc Field Coil T3 and White Dwarf Matter). Totals per
machine are in `singularity-ladder-costs.md` §14: a quasar is about 70k neutronium, 35k Infinity and 3.3k SpaceTime
ingots (T1 coils), 392 h nanite-time, in line with one Eye T5/T6 step.

**Suggestion.** Keep. *Alternative:* raise the Eddington batch if the quasar should cost more than an Eye step.

### P4-27 The Quasar Engine card's Magnetar Matter items and condensate [recipes]

**Built (SL-Q01).**

- "Dense Magnetar Matter Plate ×8" and "Magnetar Matter Frame Box ×8" do not exist: Magnetar Matter is our fluid, not a
  GT material. They are folded into the molten feed: 9,216 + 8 × 1,296 + 8 × 288 = **21,888 L**, §6.3's "about 20,000 L".
- "Black Dwarf Matter Condensate 64,000 L" is **molten Black Dwarf Matter 448 ingots (64,512 L)**: an assembly line
  cannot draw from a BEC network.
- Research item: the Quasar Seed. Ergomatter → Antipolarization Agent 256,000 L, as §6.3.

### P4-28 The two transition cores and the charts [recipes]

**Built.**

- **SL-Q21 Intergalactic Upgrade Core** (Condensate Assembler, 3,000 s, UXV): Entangled Magnetar Matter 4,608 L,
  Entangled Agent 32,000 L, SpaceTime Condensate 1,120 ingots (P4-24), Entangled Black Dwarf Matter 448 ingots. The
  Assembler takes at most 4 condensates, so the card's White Dwarf Matter Condensate (64,000 L) is 48 dense White Dwarf
  Matter plates. The card's "Metamaterial 64 ×2" is Field Manipulator II ×16 + Shielding II ×16 (tier-II metamaterials,
  White Dwarf nanites, T4-legal). Throat Mk I ×2, FG UMV ×64, UXV circuits 32, Bastion 64.
- **SL-Q22 Supermassive Seed Core** (3,600 s, UXV): Entangled Ergomatter 128,000 L + Entangled Magnetar 2,304 L;
  Bastion 64, Resonance Chamber III ×32, Field Manipulator II ×32 (the card's "Metamaterial 64").
- **SL-Q24 / SL-Q25 Star Charts**: Data Orb + Sensor UMV ×4 + Entangled Magnetar 2,304 L (Triangulum) or Entangled
  Ergomatter 4,000 L (Milky Way), SpaceTime 28 ingots. A Casimir Plate / Bastion Plate keeps the two item sets apart
  (the Assembler finds recipes by items only).
- **SL-B16** (the BEC proposal's reserved Entangled Magnetar Matter): its generator is added here (144 L → 144 L,
  180 s, 2 A UXV), because P4 is its first consumer.
- SpaceTime Condensate "4,000 L" on the throats and charts is 28 ingots (4,032 L): GT's half-unit rule.

### P4-29 Conduit Mirror: "BEC Metamaterial Film" was never built [recipes]

**Built.** SL-Q11 takes 4 MetaMaterial Wave Focus II (GT's focusing metamaterial) where the card has 4 BEC
Metamaterial Film. Everything else as carded, with the Agent for Ergomatter. **Alternative:** build the film item.

### P4-30 Machine recipes wait for their MTE unit [recipes]

**Built.** `BTSMachines` holds the seven P4 stacks (`quasarEngine`, `wormholeMouth`, `conduitMirror`,
`conduitJunction`, `burstThermaliserCrown`, `grbReceptorPort`, `burstControlHatch`), null until each MTE unit
registers its machine. A recipe whose output is null is not registered and not counted as a skip; the boot log names
the waiting ones in one line ("P4 recipes: 7 wait for their machine's MTE unit"). The Junction also consumes a
Wormhole Mouth. Research items are unique (the Quasar Seed, the Rim Gas Injector, the Superradiant Mirror Segment, the
Jet Collimator Ring, the Burst Thermaliser Casing, Throat Mk II).

### P4-31 The DENSITY gates are the Hosts unit's [recipes, contract]

**Built.** SL-Q09 (Quasar Seed) and SL-Q23 (Superdense refined) are on `KerrRecipeMaps.DENSITY`. Today the Compressor
module's own gate applies (Kerr T2, 128 SM). `BTSRecipesQuasar.densityMinMass(recipe)` (256 SM for both) and
`densityNeedsLink(recipe)` (SL-Q23: the KEE's quasar link at σ ≥ 1) are the hooks for the Hosts unit's
`validateRecipe`. SL-Q23 takes a programmed circuit 8 (not consumed): the DENSITY map needs one item input.

### P4-32 SL-P40 and the Eternity swap (SL-Q26) [recipes]

**Built.**

- **SL-P40**, the SL-P39 pattern (a separate Condensate Assembler recipe behind `replaceTecTechEyeOfHarmony`): RC-2's
  Resonance Chamber IV with the MHDCSM foil replaced by 2 SpaceTime foils + 1,500 L Entangled Ergomatter (the card's
  1,440 L on the 500 L half-unit rule). Ruling 22c moves SL-P39 to an in-place rewrite of GT's recipe; SL-P40 can
  follow it in the same commit when that lands.
- **SL-Q26** (D-48 proposal 1, Q-10), at load-complete, behind `replaceTecTechEyeOfHarmony`, rewritten in place:
  - GT++'s Temporal Harmony Catalyst (Assembler, the QFT catalyst of the coremod's Eternity recipe): the Universium
    Nanite goes; Ergomatter 9,216 L takes the one fluid slot; its Dark Iron (10 stacks of molten) moves into the items
    as 64 + 7 blocks.
  - The gate dump then found a second Universium Nanite: the only other **Timepiece** recipe (coremod DTPF, 3
    Timepieces) eats one, and the catalyst consumes a Timepiece. Its nanite becomes Ergomatter 4,608 L (a ninth fluid
    slot is free). The Eternity QFT recipe hands a Timepiece back, so the loop feeds itself after the first catalyst.
  - `BTSRecipesReality.ETERNITY_BEFORE_T10` follows `BTSRecipesQuasar.eternityBeforeT10()` (true: with the flag on SL-Q26
    runs; with it off TecTech's Eye still makes Universium). The H/R "Eternity" gate lines are gone from the boot log;
    SL-Q26 logs an error if it finds nothing to rewrite.
- **Suggestion.** Flip the questbook's `eternityOnErgomatter` feature flag at the questbook merge (the questbook branch
  is not in this base).

### P4-33 NEI pages for the quasar and the crowns [content]

**Built.** `machines/quasar/QuasarRecipeMaps`: two display-only maps with fake recipes (the `BTSDisplayRecipes`
pattern, numbers read from the engine). `beyondthestars.recipe.quasar`: one page per feed gas (Hydrogen, Helium, Raw
Star Matter → the Gamma-Ray Burst display item) with κ, the Eddington feed at 64 and 1,024 SM, the coil power and the
jet split. `beyondthestars.recipe.thermaliser`: one page per crown Mk (cap, EU/t at cap, petals). **Contract:** the
Quasar and crown MTEs return these maps from `getAvailableRecipeMaps()` so NEI lists them as catalysts.

### P4-34 The P4 gate audit on the dev server [recipes, test]

**Run.** `/btseye gates` on a headless `runServer` (this worktree, port 25581). `SLGateDump` now also checks every P4
part (27) against the Eye tier it gates (7: everything built before T7 and the T6 → T7 core and chart; 8: Throat Mk III,
the T7 → T8 core, the Milky Way chart, Resonance Chamber IV and the Eternity catalyst; 9: Superdense Ergomatter), its
direct inputs, nanites and fluids, and one level down. Results in the build spec's P4 section. The seven machine
recipes are not checked until their MTE units land; their inputs are the blocks and parts above.

### P4-35 Casimir-lined conduit casing: not a block [content]

§6.2 lists it; P4-08 found the existing Casimir Plate (`slCasing:14`) plays the role in every conduit shape, so no new
meta was added (a block nothing uses would only be a recipe and a texture to keep). **Alternative:** add it as
`slCasing2:12` if the conduit shapes are redrawn with a distinct lining.

### P4-36 The quasar's energy sockets take any mix of energy and laser hatches [quasar MTE]

**Context.** §7 draws three energy cells for coil power, 2 A UXV × b²: 2 A at b = 1, 18 A at b = 3, 32 A at b = 4. GT's
`checkExoticAndNormalEnergyHatches` allows "1 exotic or 2 normal" hatches, which refused the creative build's three
hatches.

**Built.** The quasar's own check: at least one Energy or Laser Hatch in the `e` sockets, any mix, all drained by
`drainEnergyInput` (lang `beyondthestars.structure.quasar.energy`).

**Suggestion.** Keep. **Alternative:** GT's rule, which forces one laser hatch at b ≥ 2.

### P4-37 How a jet finds its dock: a registry keyed on the port cell [quasar MTE, contract]

**Context.** P4-02 says a dock is linked when its intake (crown) or throat centre (mouth) sits on the port cell. The
quasar knows the port cell but not where a Wormhole Mouth's controller stands.

**Built.** `machines/quasar/burst`: a dock (`IBurstDock`) registers its intake cell every second while formed and
facing the right way (`BurstDocks.register`, entries stale after 40 ticks, weak per-world table). The quasar looks up
each port cell once a second and calls `acceptJet(JetPacket)`; with no ready dock the port vents. The facing check is
the dock's (it simply does not register when it faces wrong).

**Suggestion.** Keep: one mechanism for crowns and mouths, no geometry duplicated. **Alternative:** the quasar probes
`crownControllerRel` directly for crowns and only mouths register.

### P4-38 The gas buffer [quasar MTE]

**Built.** The 24 rim hatches (any input hatch, ME stocking included) refill a buffer of the selected gas only, up to
10 s of the maximum feed (10 × 1.5 × Ṁ_Edd: 192,000 L at 64 SM on Helium, 3.07 ML at 1,024 SM). The disc draws f·Ṁ_Edd
from it every second; a short buffer gives a lower f_eff (the engine's own reaction). Switching gas leaves the old
buffer in place (unused). The buffer is tile NBT only: a wrenched controller keeps its hole but not its gas.

**Suggestion.** Keep. **Alternative:** no buffer (drain the hatches directly each second); a big ME network then sees a
200 kL/s pull per second instead of a 10 s refill.

### P4-39 Trims drain on their own; no OpenComputers trim() call [quasar MTE, automation]

**Context.** §8.2 lists `trim("gas" | "space", units)` for `bts_quasar`.

**Built.** As on the Eye (D-49), the 4 plain trim hatches are drained every second: whole units of the feed gas
(1,000·M/64 L, E +2) and Spatially Enlarged Fluid (100·M/64 L, E −2), at most 10 each. The automation is the valve
that fills the hatch (AE2 export bus on Disc bit 1 / 2, a regulator cover, SFM, an OC transposer). A `trim()` call
would need fluid from nowhere, so it is not built.

**Suggestion.** Keep; the questbook names the valve routes. **Alternative:** a `trim()` that only drains what the trim
hatches already hold (it would duplicate the automatic drain).

### P4-40 Open, Collapse and the controller slot [quasar MTE]

**Built.** The controller slot is shown (GT's MUI2 slot beside the power button). Open (GUI, OpenComputers has no open
call, `/btsquasar open`) consumes a Quasar Seed for a hole that never existed (64 SM, a = 0, P4-16) or one of GT's
Black Hole Seeds for a dormant one. Collapse needs no item (§1.1) and asks twice. Nothing opens on its own when an item
is inserted, because opening the hole is one of §8.3's three manual steps. A new hole gets its own disc noise sequence
(position and world time), saved with the state.

**Suggestion.** Keep.

### P4-41 Burst Control Hatch details [quasar MTE + 28126]

**Built.**

- The level is the strongest redstone on any side (covers included), read every tick; a rising edge is 0 → 1 or more
  and is latched until the host's next second, so a one-tick pulse counts.
- Modes PURGE, FIELD, SPLIT, BEGIN; a hatch adopts its host's first mode when its own is not used there (the
  Directive Hatch pattern). The quasar uses PURGE and FIELD; FIELD with s = 0 gives b = 0.5; several FIELD hatches →
  the highest s. A PURGE edge while the hole is dormant is dropped.
- The Junction (SPLIT) and the Eye (BEGIN) read `level()` / `consumeRisingEdge()` and call `poll()` themselves.

**Suggestion.** Keep.

### P4-42 The sensor window applies to the Eye too [quasar MTE + 28116]

**Built.** The window w (default 120) maps the Eye's signed gauges as ±w; at 120 this is exactly D-49's
`round(15(v + 120)/240)`, so every placed Eye hatch reads the same. Invert and the wider threshold range (±100,000, for
masses) apply to both hosts. The hatch's host (and so its channel list) is whichever machine last published; the
channel ordinal is kept across hosts (append-only lists).

### P4-43 NEI preview: the bottom row falls below the scratch world [quasar MTE, NEI]

**Found.** BlockRenderer6343 1.4.24 places every controller at (0, 64, 0) (`GuiMultiblockHandler.MB_PLACE_POS`). The
quasar's controller sits 65 rows above its bottom (voxel y −15 against the bottom cap at −80), so the bottom cap's
outer layer (voxel y −80, with two `c` sockets) lands at y −1 and is not drawn. Everything else (y 0..159) fits. The
preview's frame rate with 51,984 blocks is not measured (no client run in this unit); `INEIPreviewModifier` only
offers hooks, it cannot move or thin the build.

**Suggestion.** Accept the missing row (the hologram projector builds it). **Alternatives:** move the controller one
row down in the mockup (a voxel.js + manifest change); or ask BlockRenderer6343 to lift tall structures.

### P4-44 What the Disc set's alarm bit sees [quasar MTE, automation]

**Built.** Disc bit 4 is on while the hole is not OPEN (re-forming, dormant), b = 0 (no coil power), or any Bu/t vented
last second. "A Conduit element closed" needs the Conduit unit: `conduitOpen` is true until it reports.

**Contract.** The Conduit unit sets a closed-element flag (a `reportHost`-style call) that the quasar folds into
`QuasarDirective.State.conduitOpen`.

### P4-45 The render [quasar MTE, render]

**Built.** `TileQuasarHole` (a GT `RenderingTileEntityBlackhole`) at the manifest's centre cell while the quasar is
formed and has a hole (open, re-forming or dormant; a dormant or re-forming disc is drawn dark and thin). GT's model is
scaled (4 + 4·log₂(M/64)/6)/1.3; its shader stability is the disc's Stability/100. Our TESR adds the disc (r 40 to
40 + 22·λc, additive, zone colours, three turning arms, puffing above E 0, a 3 s outward flash on an eruption or a
purge) and the jet cones (±78 blocks, brightness log₁₀ of each port's Bu/t). Look packets go straight to the chunk's
watchers. A render-harness site (QUASAR) with `quasar_64sm` and `quasar_1024sm_eruption` is added; not run in this
unit (the desktop is in use).

**Open.** Colours, widths and brightness are first guesses for the render QA pass.

### P4-46 A quasar that is gone is not a pause; receptors remember who fed them [conduit, Q-8]

**Context.** Q-8: a source that is not loaded pauses its hosts. `BurstSource.status()` said UNLOADED whenever no quasar
stood at the position, so a broken quasar would have paused its hosts for ever. And a host learns its sources only from
streams, so after a restart with the quasar's chunk unloaded it would read zero, not pause.

**Built.** `BurstSource.Status.GONE` (chunk loaded, no quasar there) and `pauses()` (FROZEN or UNLOADED). Each GRB
Receptor Port keeps the quasars that fed it (positions only, at most 8, saved in its NBT; never a link). In
`reading()` a remembered source that no longer arrives pauses the host if it pauses, and is forgotten if it is RUNNING
(the path broke: felt in full) or GONE.

**Suggestion.** Keep. **Alternative:** no memory (a restart with the quasar unloaded reads 0 for one session).

### P4-47 Rays: range, aim tolerance and what blocks them [conduit solver]

**Context.** §3: "within 64 blocks", "air or one of our render cells". The throat centres are inside the elements (a
mouth's ring centre is 5 deep) and the Junction's out 2 / out 3 leave at ±60°.

**Built.** A ray leaves an out-throat's centre along its outward normal. It enters an in-throat when it crosses the
opening's plane within radius + 0.5, travelling into it at most 60° off its axis (cos ≥ 0.5; a mouth's ring takes
either side), at most 72 blocks centre to centre (64 of air + 8 for the two throats' depth). The cells on the way
(Amanatides-Woo walk) must be air or `BlockQuasarHole`; the first GRB Receptor Port takes the stream; any other block
blocks it ("blocked at x, y, z (block)"); an unloaded chunk on the way stops it ("an unloaded chunk at ..."). Routes are
cached 5 s per out-throat and dropped on every structure check; a stale target is re-traced at once.

**Suggestion.** Keep. **Alternatives:** exact 64 measured face to face; no angle tolerance (out 2 / out 3 could then
only feed receptors, never another element).

### P4-48 Throat upkeep: one unit per element, always while a throat is fitted [conduit]

**Context.** §3 says "a Junction counts one per leg in use", against the doc's own rig sums (7 elements → 28 L/s); F-5
recommends one unit per element. The design does not say whether an idle element pays.

**Built.** F-5: every element (each mouth of a pair, mirror, junction) pays one unit a second for its throat Mk while
formed with a throat in the slot, idle or not (the throat is held open). Fuel per GUI / OpenComputers: Auto (Ergomatter
when the hatches hold the second's need, else Molten SpaceTime), SpaceTime only, Ergomatter only. Fractional rates carry
(0.25 L/s drains 1 L every 4 s). A new or newly fuelled element is closed until 10 s of full upkeep; no throat = closed.
A stream that reaches a closed element is lost and raises the source quasar's Disc bit 4 for 2 s (closes P4-44).

**Suggestion.** Keep. **Alternatives:** pay only in seconds with traffic; one unit per leg (a 52 L/s rig).

### P4-49 The Mirror is reversible [conduit]

**Built.** A stream into either throat leaves by the other (the drawn in/out is only the default). One mirror then
turns a stream both ways along the same corner; a stream that loops back is dropped by the cycle check.

**Suggestion.** Keep. **Alternative:** in → out only, as drawn.

### P4-50 σ: last second and the 60 s mean; conditions use the mean [host API]

**Context.** R.2 defines σ = delivered ÷ D_h; F-3 says per-second floors cannot survive the Φ sawtooth; §3.1 checks a
recipe condition "at start and every 20 ticks".

**Built.** `BurstHost` gives `sigma()` (last second) and `sigmaMean()` (60 s rolling mean ÷ D_h). The contract tells the
Hosts unit to use `sigmaMean()` for recipe conditions (Superdense refinery, Entanglement Medium, Reality pausing) and
for bonuses (Kerr bleed and setpoint cap, Homogeny speed), so a purge dip does not pause a recipe.

**Suggestion.** Keep. **Alternative:** last-second σ (bonuses flicker with the sawtooth).

### P4-51 A link needs bursts, not just a path [host API]

**Context.** §3.1: linked while "at least one of its ports received bursts" from a linkable quasar over an open path.
The prototype's `linked` is the path alone (a 0 Bu/t share still links).

**Built.** A source is linked when a port received more than 0 Bu/t from it last second on a linkable stream. A 0 share
still delivers a 0 packet (the receptor knows its source for Q-8), but it is not a link. Both milestones need 1.5 / 6
Bu/t means anyway, so this only matters for the σ hosts at a 0 share.

**Suggestion.** Keep.

### P4-52 How a Wormhole Mouth docks and which way a stream crosses it [conduit, contract]

**Context.** P4-02 / P4-37 put a mouth's dock cell at its throat centre. The ring is 10 deep with the centre 5 in, so a
mouth centred on the port cell would sit 5 blocks inside the quasar's cap.

**Built.** The dock cell is the ring's centre on the **front face** (layer 0, the controller's side): the mouth lies on
the cap, front toward the quasar, and the jet must run into the front along the ring's axis (else "misaligned", taken
0). A stream crosses a pair front → back: it leaves the partner out of its back, along +c. A ray may also enter a ring
from either side. Pairing: a Data Stick remembers the first mouth (NBT on the stick) and pairs it with the second; one
partner each, re-pairing breaks the old pair; the pair is tile NBT (a wrench breaks it).

**Suggestion.** Keep. **Alternative:** a dock offset in the manifest so the ring sits elsewhere.

### P4-53 Junction details [conduit]

**Built.** One throat item caps the whole stream through it (each arrival counted against this second's total). Split:
out 3 = p3 × in off the top, then `BurstRouting.split`; the first live Burst Control Hatch (walk order) gives s, the
second socket is spare; with no hatch or k = 0, share₁ = c. Every leg is emitted even at 0 Bu/t (Q-8 bookkeeping).
Combine: any of the three small throats in, the sum out of the big one. Switching mode clears its books.

**Suggestion.** Keep. **Alternative:** the second hatch sets p3 (the design's "future out-3 control").

### P4-54 Crown details [crown]

**Built.**

- Mk is the biggest of `crown_mk3/2/1` that checks; a failed try reports only the errors of the Mk last formed (so an
  upgrade in progress shows its own missing blocks).
- Under the bottom cap the crown is **upside down** (rotation UPSIDE_DOWN), not "flipped vertically" as the manifest
  words it: GT's wrench never sets a vertical flip, and the intake geometry is the same.
- `acceptJet` takes up to its cap plus a stacked crown's, less what it already took this second; the crown's own second
  adds the receptor's delivery and overflow from below, uses min(S, cap) and hands the rest to the crown stacked on it
  (only from the base crown: two, not a tower). EU goes to the owner's wireless network once a second (20 × EU/t); no
  owner (a console-placed crown) = no push, shown.
- The crown reports remote feeds to their quasar (its Links list) with σ 0.

**Suggestion.** Keep.

### P4-55 SL-Q16 used a wireless dynamo RC-2 never registers [recipes]

**Found.** First dev boot with the crown registered: SL-Q16 skipped, "Wireless_Dynamo_Energy_UMV has not been set".
RC-2 leaves GT's `Wireless_Dynamo_Energy_*` ItemList entries unset; the pack's wireless output part is TecTech's
Energized Wireless Dynamo Hatch (`CustomItemList.eM_dynamoWirelessMulti`, tier 12 = UMV).

**Built.** SL-Q16 takes 2 Energized Wireless Dynamo Hatches instead. Everything else on the card is unchanged.

**Suggestion.** Keep.

### P4-56 An unloaded element breaks the path; it does not pause [conduit, Q-8]

**Built.** Q-8 pauses hosts only for their source quasar. A mouth's partner, a mirror or a junction in an unloaded chunk
simply stops the stream (the scanner says "partner ... is not loaded" or "an unloaded chunk at ..."), and the host feels
the loss. Tooltips and the questbook say to chunk-load every element.

**Suggestion.** Keep for v1. **Alternative:** a mouth whose partner is unloaded forwards a "paused" marker so hosts
downstream pause too.

### P4-57 No stream render between elements [conduit, render]

**Built.** Nothing draws the beam between conduit elements (the quasar draws its own jets). The scanner and GUIs show
every route. No render-harness case was added: there is nothing new to draw.

**Suggestion.** A thin additive beam TESR along cached routes in the render QA pass (needs a client run).

### P4-58 A galaxy transition begins on a signal, with the next chart in the slot [Eye T7/T8]

**Context.** D-49's T1-T3 transitions begin by themselves once the next chart is in and every precondition holds.
§8.1 makes Galaxy Transition bit 1 "Ready: begin", and the Burst Control Hatch's begin mode "a rising edge begins the
transition when bit 1 is on". The collapse assist times the quasar's purge against the overfeed, so the begin is a
timed act.

**Built.**

- The next galaxy chart in the controller slot makes the Eye PENDING, as on T1-T3: Triangulum on a lit magnetar, or
  the Milky Way on a lit Triangulum. The object keeps burning.
- The transition begins only on a request while every precondition holds: the milestones (link included), the core in
  escrow, the receptor ports, Stable, and the charge plus the new object's first cycle buffered.
- A request is one of: the GUI Galaxy panel's **Begin**, the rising edge of a **Burst Control Hatch in Begin mode** in
  a plinth `d` socket, or OpenComputers `beginTransition()`.
- A request while not ready is ignored, and the answer names what is missing. Bit 1 is on only while PENDING and
  ready.

**Suggestion.** Keep. **Alternative:** an "auto-begin" toggle that begins the moment it is ready, as T1-T3 do.

### P4-59 The galaxy chain: a dark Eye with a galaxy chart is re-born from the magnetar [Eye T7/T8]

**Context.** §4.6 / §4.7: a lost galaxy re-runs "from a lit magnetar (charge again, no core)" or "from a lit
Triangulum". D-49's T4 relights Arcturus by itself to re-run the Core Collapse.

**Built.**

- In galaxy mode, a dark Eye with the Triangulum or Milky Way chart and T6 unlocked spawns the magnetar (the Magnetar
  Merger re-spawn and its charge).
- The magnetar is then PENDING toward the chart. The Magnetar Collapse re-runs (no core, no link), then the Galactic
  Merger (for the Milky Way, once T7 is unlocked).
- A chart more than one tier past the unlocked tier does not chain: the first Milky Way needs T7 unlocked.
- Each galaxy transition still begins on a signal (P4-58), so a re-run needs the Begin wire. The standard rig has it.

**Suggestion.** Keep.

### P4-60 A companion change relights the galaxy in place [Eye T7/T8]

**Context.** §4.8: "Changing a companion extinguishes and relights the galaxy, as a target change does today." Read
literally, the galaxy goes out and must be re-born by the Magnetar Collapse (and the Galactic Merger): an hour or more.

**Built.**

- The companions are the slot bus's star charts of earlier objects: T1-T6, unlocked, one per tier, in slot order. T7
  runs 1, T8 runs 2.
- When the set changes, the galaxy **re-forms in place**: the gauges go to 0, Integrity to 100, a fresh cycle starts
  (the cycle in flight is lost), and the new set's upkeep is charged. No transition re-runs.
- Each companion pays its own upkeep per cycle in full: `GalaxyField.Companion`'s table × 2^(N−1−k) × p, with
  fractional litres carried. It keeps its own r.
- Its product is × the galaxy's mean Y × min(the galaxy's r, its own r).
- Companions are ignored during a transition. The Galactic Merger needs none loaded.

**Suggestion.** Keep. **Alternative:** the literal reading (the galaxy goes out).

### P4-61 Planets mode: the Eye's mode switch; the galaxy chart picks the slots [Eye T7/T8]

**Context.** §4.8 says planets mode is chosen like a planet target (it extinguishes the galaxy). §6.3 says "planets
mode needs no chart". §8.2 has `setMode("galaxy" | "planets")`.

**Built.**

- **The switch:** a mode on the Eye, set in the GUI Galaxy panel, by OpenComputers `setMode` or by the bench `mode`;
  item NBT.
- **Slots:** in planets mode the Triangulum chart means 3 slots (PLANETS7) and the Milky Way 6 (PLANETS8). There is no
  new chart: the galaxy's own picks the tier.
- **Lighting:** the planets field lights at once on an Eye with T7 / T8 unlocked: no collapse, just its EU per cycle.
  Switching the mode puts the other field out.
- **The slot bus** (`p`, any input bus, ME included; read, never consumed) fills the slots with converted TecTech
  planet items in slot order.
- **A slot's run:**
  - it pays at the start: the planet's H/He × parallel from the buffers and its EU from the network, and waits until
    it can;
  - it runs the planet's base time with the compression reward;
  - at the end it rolls each parallel at (1 − 0.05·R) × the field factor averaged over the run;
  - its parallel is 2^(installed − k).
- **A Breach** (or Integrity 0) fails the runs in flight and re-forms the field.
- A Deep Dark run still counts toward the T4 → T5 milestone. Planets-mode seconds do not count toward Triangulum's
  Resonant milestone.

**Suggestion.** Keep. **Alternative:** a separate planets chart.

### P4-62 The halo test fire is automatic; the "setting" is the Junctions' [Eye T7]

**Context.** R.1: "The test fire setting forces J1 to the Eye and J3 to 75 % halo". The Eye cannot set Junctions. The
rig proof drives them from Slots [8].

**Built.**

- At T7 (galaxy mode) and through the Magnetar Collapse, bursts on the halo ports are always a test fire. They count
  for the Heavy Link (the Eye's total, the halo's share) and are dumped from the field (β, γ_halo).
- From the Galactic Merger on, and at T8, they are the field's halo input.
- Slots [8] ("link test fire wanted") is the setting: the player wires it to J1's and J3's Burst Control Hatches. The
  questbook, the Directive tooltip and the GUI's rig line say so.
- `heavyOk` gets testFire = "the halo has ports".

**Suggestion.** Keep. **Alternative:** an Eye toggle that refuses halo bursts at T7 unless it is on.

### P4-63 Parallel in the galaxy maths [Eye T7/T8]

**Built.**

- β and the γs divide by D · m · p, per StepInputs' contract.
- `gEye` (the collapse shock's cancel) is the Eye's Bu/t ÷ p.
- The Magnetar Collapse's overfeed charge is × the magnetar's parallel; the Galactic Merger's Ergomatter is × the
  Triangulum's. The transitions' buffer checks match (`transitionNeeds(type, ps, pd, feed, f8)`).
- Companions are × p.
- At p = 1 (the first time through, on an Eye unlocked to the tier it runs) every form equals the prototype's.

**Suggestion.** Keep.

### P4-64 The galaxies' render [Eye render]

**Built.**

- **New looks:** EyeStarLook kinds TRIANGULUM (9), MILKY_WAY (10) and PLANETS (11).
- **The bulge** is the shader's textured core: 2.4 blocks, A8C4FF, γ 1.4 (Triangulum); 3 blocks, FFD49A, γ 1.6 (the
  Milky Way).
- **The arms** are additive, camera-facing dots on log spirals in the rail's 8° plane:
  - Triangulum: 3 arms × 44 dots to r 16, with a few pink knots;
  - the Milky Way: 4 arms × 56 dots to r 19, plus a 30-dot bar.

  They turn once in about 2.6 min, brighten on a warning, and sit on a faint disc glow.
- **Planets mode:** a dim core with up to nine coloured planets on orbits at r 5-18.
- **The overfed magnetar** shrinks by 30 %, spins up and throbs before the collapse.
- **Render-harness cases:** `eye_t7_triangulum`, `eye_t7_merger_warn`, `eye_t8_milky_way` and `eye_t8_planets`, on
  two new sites. The sites are appended, so the layout suffix stays. None was run: the desktop is in use.

**Suggestion.** Review in the render QA pass: sizes, colours, dot counts and frame rate.

### P4-65 Where the Eye's Burst Control Hatch goes, and the kit's socket layout [Eye T7/T8]

**Context.** §7 gives the Eye no `c` socket, and the Begin wire needs one.

**Built.**

- The four plinth `d` sockets take a Directive Hatch or a Burst Control Hatch. The Burst Control Hatch is in Begin
  mode; a hatch in another mode adopts Begin.
- `/btseye t6+` fits:
  - Field, Events and Galaxy Transition Directive Hatches on `d` 1-3;
  - a Begin Burst Control Hatch on `d` 4;
  - a Slots Directive Hatch on the fourth Sensor socket `n`;
  - an input bus on the slot bus `p`;
  - at T7 / T8, a GRB Receptor Port in every rail / halo socket.

**Suggestion.** Keep.

### P4-66 Receptor-port preconditions [Eye T7/T8]

**Built.**

- The Stability Link counts only with at least one rail-high and one rail-low port, and the Magnetar Collapse needs
  them (§5.2).
- The Galactic Merger needs all 8 halo ports (§5.3, "the 8 halo ports are built").
- A rail or halo band that does not count (a band below it is missing) drops its ports (mark-and-truncate, as for the
  stellar sockets).

**Suggestion.** Keep. **Alternative:** allow the merger with one halo port or more.

### P4-67 What a link saves [Eye T7/T8]

**Built.** R.1 saves the milestone, and the task asks for the streaks in NBT.

- The two `BurstLink`s' 60 s windows, counts, streaks and best streaks are saved with the live star (tile NBT), so a
  restart continues a 30 min streak.
- `done` and the best streak are milestones (item NBT): they survive a wrench.
- A wrench restarts the window and the streak, as it aborts a transition.
- Nothing about the link itself (the source) is saved.

**Suggestion.** Keep.

### P4-68 Q-8 on the Eye [Eye T7/T8]

**Built.**

- When a quasar that fed the receptor ports is frozen or not loaded (`BurstReading.paused`), a burst-driven field
  holds still that second: no field step, no clock, no cycle, no link roll. That covers T7, T8, planets mode, and
  either galaxy transition. The scanner names the quasar.
- At T6 (where bursts have no effect) only the Stability Link's roll pauses.
- A quasar that is running but dark, or one that is gone, is felt in full (P4-46).

**Suggestion.** Keep.

### P4-69 Milestone details [Eye T7/T8]

**Built.** The item milestones are the doc's halved numbers:

- **T6 → T7:** Magnetar Matter 2,700 L from cycles with no second worse than Stable, 2 h Resonant, and 50 giant flares
  ridden (the helium-flash watch: never worse than Stable from the warning to 30 s after).
- **T7 → T8:** Ergomatter 216,000 L from all-Stable cycles, 2 h Resonant, and 4 minor mergers ridden.

They count once the tier is unlocked, and never during a transition, so the forced merger in the halo capture does not
count.

**Suggestion.** Keep.

### P4-70 Triangulum's upkeep and the NEI pages [Eye T7/T8]

**Built.**

- Molten Magnetar Matter 7.5 L a cycle × p is a fractional upkeep, drawn with a carry (8 L, then 7 L, and so on).
- The T7 star page shows it rounded up (8 L) and says 7.5.
- The T8 page shows the default feed (300 L a cycle) in and 300 L of Superdense Ergomatter out.
- The planets pages carry the slot rules.
- The two transition pages list the link, the milestones, the phases, and the charge plus the new object's first cycle.

**Suggestion.** Keep.

### P4-71 The dev-server bench's scripted link [test]

**Built.**

- `/btseye star <x y z> galaxy <s> [analog] [mass M] [fresh]` runs the §8.4 rig on the real Eye:
  - a virtual quasar: the engine's disc on helium, coils 3, b 3, f 1;
  - the standard conduit: two Mk I mouth pairs, a combine, J1, J3 and J2;
  - the gate rig, which reads only the Eye's Directive codes and the disc's.

  Its streams reach the Eye as a scripted reading each second (the bench quasar is not a machine), and its Φ and b go
  to the collapse assist. After a run the rig keeps stepping in real time until `galaxy off`.
- `inject <qx qy qz> <hi> <lo> <halo>` pushes real streams from a real quasar through the receptor hatches every
  second. This exercises the BurstHost path, σ reported back, and Q-8.
- `burst <Bu/t> [high] [halo]` is a constant feed.

**Suggestion.** Keep as the bench. The full in-world rig (a quasar, mouths, mirrors and junctions aimed at the Eye's
ports) is the questbook's build.

### P4-72 The Magnetar Merger's imprint could never complete on this branch [D-49 fix, ported]

**Context.** Since ruling 12 (B-60), the paid Agent upkeep charges the flare shield at 2r a second. The shield eats
the dynamo's Π during the merger's imprint, so Π never reached its band. The dev-server chain showed it: every magnetar
re-spawn timed out. Master fixed this in B-71 (c97a35e), but that commit is not in this base.

**Built.** The core of B-71, with the same text as master:

- the `Overlay.shieldPerUpkeep(T)` hook (default `SHIELD_PER_UPKEEP`, so the 78 golden cases are unchanged);
- `StellarField` charges `ov.shieldPerUpkeep(T) * r`;
- `MagnetarMerger` returns 0 during the imprint.

Master's Directive changes for the merger (`AGENT_IMPRINT`, the merger valve) arrive with the merge.

**Suggestion.** Keep. The merge takes master's version.

### P4-73 The collapse assist reads the linked quasar's flux as it stands [Eye T7]

**Context.** The Galaxy Transition set's purge-now bit needs the linked quasar's Φ and b (`GalaxyDirective.transition`).
The prototype reads `Q.Phi`, the flux after the quasar's last second.

**Built.** The Eye resolves its strongest linked source (`BurstSource.resolve()`) and reads that quasar's `Phi` and
`b`. It does not read `lagPhi`, the sensor's one-second lag. With no linked quasar both are 0: only the collapse
second's purge bit fires.

**Suggestion.** Keep.

### P4-74 No host needs a link; Reality Diffraction with ports fitted rides the stream [hosts]

**Context.** R.2 and §3.1 say "No host needs a link to work". But Reality Diffraction's row says "below σ 0.5 the run
pauses".

**Built.**

- **Kerr (KEE):**
  - A GRB Receptor Port fits any utility socket (`u`), beside the BHC utility and Directive hatches. It is a private
    list.
  - Demand is 0.1 Bu/t at T1 and 0.25 at T2.
  - `KerrHole.burstSigma()` is the 60 s mean σ (0 with no port).
  - The Hawking bleed is × (1 − 0.75·min(σ, 1)).
  - On T2 at σ ≥ 1 the setpoint cap is 768 SM instead of 512 (P4-80).
- **Homogeny T2 / T3:**
  - 3 ports go on the reserved `g` cells of the T2 stirring ring. The cells fall back to the old casing, so builds from
    before P4 still form.
  - Demand is 1.5 Bu/t at T2 and 6 at T3.
  - Speed is × (1 + 0.5·min(σ, 1)). With the 4 / 16 parallels that is up to 6× (T2) and 24× (T3) the T1 rate.
  - Entanglement Medium (SL-H21) needs σ ≥ 1 to start, and pauses below it.
  - Degenerate Stellar Mixture never needs bursts.
- **Reality Diffraction:** 6 ports on the portal `g` cells, demand 24 Bu/t.
  - With **no port fitted** it runs at ×1 on its fluid anchors. It needs no link.
  - With **a port fitted** the regions ride the stream: speed is × min(σ, 1.5). Below σ 0.5 no run starts, and a
    running one pauses (it is kept, never voided).
- **Every host:**
  - uses `BurstHost`: σ is the 60 s mean, and is reported to the feeding quasars;
  - follows Q-8: a frozen or unloaded quasar holds σ where it was, and the work σ gates pauses.

**Dev server** (2026-10-13, port 25581). A real quasar's bursts went through real receptor ports
(`/btsconduit inject`, `/btshr bench … inject`):

- **KEE T2:**
  - 0.5 Bu/t gave σ 2.00 (60 s mean), bleed × 0.25 and cap 768. `setSetpoint(700)` kept 700.
  - With the injection off, the mean fell to 0 within the minute, and the hole was held at 512. The setpoint stayed
    700.
- **Homogeny T3:**
  - 9 Bu/t over 3 ports gave σ 1.50, speed × 1.50 and "Entanglement Medium open".
  - With the injection off, the mean fell below 1 and the Entanglement Medium line closed.
- **Reality Diffraction:**
  - 12 Bu/t over 6 ports gave σ 0.50 and speed × 0.50.
  - 6 Bu/t gave σ 0.25: "regions UNSTABLE, runs pause below σ 0.5".
  - With a quasar block removed, the quasar froze and the machine said "PAUSED: its quasar … is not running (σ held)".
    When the block was rebuilt, it resumed.

**Suggestion.** Keep. Fitting ports on Reality Diffraction is a choice: it runs faster with a strong link and pauses
with a weak one. The tooltip and the questbook say so.

### P4-75 The automation bench models the conduit; the real elements are proven separately [test]

**Built.** `/btsquasar autorig x y z <s> [eye ex ey ez] [analog] [cut k] [mass M]` drives a **real** Quasar Engine
and a **real** Eye.

- It sees only their Directive Hatches' codes (window 5).
- It acts only through their own hatches: trim valves, Burst Control Hatches, plinth and slot buses.
- Between the two machines, the conduit is the standard rig's maths (`EyeGalaxyBench.conduit`): two Mk I mouth pairs,
  a combine, J1, J3 and J2. It is fed by the real quasar's jets.
- The real Mouth, Mirror and Junction elements are proven by `/btsconduit rig` (the conduit unit). They are not aimed
  at the Eye's 16 ports here.

**Suggestion.** Keep. A 16-port in-world conduit build is the questbook's job, and the video's.

### P4-76 The next core is wanted before the link milestone [Eye T6 / T7, automation]

**Context.** The Directive rule for Transition [4] ("next core wanted") was "this tier's milestones are met". On T6
and T7 the link is one of those milestones. But the link's test fire (Slots [8]: J1 all to the Eye) is wanted only
when the core is already in escrow. So an unattended rig never pushed the core and never got the test fire. J1 stayed
at its 57 % centre, and the Stability Link's best streak was 14 s in a 16,000 s run.

**Built.** On T6 and T7, Transition [4] comes when every milestone **but the link** is met
(`EyeMilestones.metButLink`). The link is still a precondition of the transition (`met()`), so nothing begins early.
Other tiers are unchanged.

**Suggestion.** Keep. On T6 and T7 the order is now:

1. the item milestones;
2. the core;
3. the test fire and the link;
4. the chart and Begin.

### P4-77 OpenComputers `trim(kind, units)` [automation]

**Context.** §8.2 lists `trim("gas" | "space", units)` on `bts_quasar` and `trim("space", units)` on `bts_eye`, as "one
second's pulse, capped". The trim sockets are plain hatches that drain whatever they hold, so a script has no reserve
there.

**Built.** `trim` queues units for the machine's next second. They are added to what its trim or Field hatches hold,
under the same cap.

- **Quasar:**
  - "gas" is paid from the quasar's own feed buffer;
  - "space" is paid from Spatially Enlarged Fluid in its gas sockets (an ME stocking hatch can hold it there);
  - the cap is 10 units.
- **Eye:**
  - any trim of the burning star, by key or name ("space" from T7);
  - paid from its gas and stellar sockets (ME stocking included);
  - the cap is the trim's own cap × p.
- Both are rate-limited like every setter.
- `/btsquasar ocloop` showed `trim(gas, 12)` clamped to 10 and taken the next second.
- `/btseye star … ocloop` showed `trim("space", 99)` → 10 (cap 10), and an unknown trim refused.

**Suggestion.** Keep. A script needs no transposer for trims.

### P4-78 Master's Magnetar Merger Directive fix, ported [D-49 fix, ported]

**Context.** P4-72 ported the core of master's B-71 fix but not its Directive half. With the inline thresholds (the
Agent above Π +75), the gate rig let Π sit in Critical during the imprint. On the dev server, the galaxy chain's
magnetar re-spawn failed on Integrity 344 times in a row.

**Built.** Master's hunks, word for word:

- `MagnetarMerger.agentWanted` (imprint above +66, release above +30);
- `EyeTrimRule.MERGER_AGENT_RATE` (1 unit a second) with `rigExtraRate(tier, transitionRunning)`;
- the call in `EyeStarRuntime.extraWanted`;
- the bench's Status [4] valve.

Golden 78/78 is unchanged.

**Suggestion.** Keep. The merge takes master's version (the hunks are identical).

### P4-79 The T7 halo test fire waits for a quasar big enough [Eye T7, automation]

**Context.** At T7, Slots [8] asks for the Heavy Link test fire: J1 all to the Eye, and J3 75 % to the halo. The rail
then keeps a quarter of the stream. If the stream is under the 6 Bu/t the link needs, that quarter is below the
1.5 Bu/t demand.

On the dev server an 88 SM quasar left the rail 0.9 Bu/t, and Triangulum was lost on Integrity, twice. The link could
not have been held anyway.

**Built.**

- In game, Slots [8] at T7 also needs the linked quasar's mean stream to be at least 6 Bu/t
  (`QuasarParams.meanStream` at its G0 and Y_q, purging). That is about 130 SM at coils 3.
- The Galaxy panel says "Heavy Link test fire waits: the quasar's mean stream x Bu/t is under 6 (grow it)".
- `GalaxyDirective`'s rule is unchanged, so QuasarSim and the 38 golden cases keep the prototype's behaviour. (The
  prototype starts at 120 SM.)

**Suggestion.** Keep. The alternative is a lower halo preset for small quasars, but the link would still not be held.

### P4-80 The 768 SM cap: what a lost link does [Kerr]

**Built.**

- The stored setpoint accepts up to 768 SM.
- The governor holds min(setpoint, cap). The cap is 768 only while the 60 s mean σ ≥ 1 on T2.
- A lost link drops the cap to 512. The hole bleeds down to it, and nothing is lost suddenly.
- A regained link climbs back to the stored setpoint.
- On the control-set hatch, 15 maps to the cap in force.

**Suggestion.** Keep (as §3.1 says).

### P4-81 The proof needs the quasar riding Eddington (f 1.0) [automation, teaching]

**Context.** The GUI's default feed is f 0.85, "unattended, Stable". At f 0.85 the disc's λ is about 0.85, and the
64 SM quasar's stream was too thin for the Stability Link: the best streak was 0:01 in 16,000 s. The prototype's
standard rig runs f 1.0 with the disc trims, and so does the real proof.

A fresh hole also starts with no spin. It settles at a ≈ 0.45 (b 3, purging at Φ 40) over about an hour, so the first
link comes after the spin-up.

**Built.** No code change.

- The proof sets f 1.0 once, as a player does in the GUI.
- The questbook text ("Riding Eddington") and the automation doc say to use f 1.0 with the Disc [1] / [2] valves for
  the links.

**Suggestion.** Keep f 0.85 as the default, because it is safe without trims. Teach f 1.0 for the link.

### P4-82 Disc [8] stays on while a jet vents [quasar, observation]

**Context.** The Disc set's alarm bit includes "a jet port venting". On the bench nothing is docked on the real
quasar's ports (the conduit is modelled), so Disc [8] was on for 99.7 % of the run.

**Built.** No change. In a real rig the mouths dock and the bit clears. The standard rig wires nothing but a lamp to
Disc [8].

**Suggestion.** Keep.

### P4-83 A paused source that never comes back no longer pauses a re-aimed host for ever [conduit, Q-8, full test]

**Context.** P4-46: a GRB Receptor Port remembers the quasars that fed it, and a remembered source that is frozen or not
loaded pauses its host (Q-8). In the full dev-server test, the Homogeny T3 was fed through its ports by a new, running
quasar, while the quasar that fed it in an earlier session stood in an unloaded chunk. The Homogeny stayed "PAUSED: its
quasar at 12000, 100, 12600 is not running (σ held)" while it took 9 Bu/t from the new one. A player who re-aims a
conduit at another quasar, and leaves the old one unloaded, would see the host paused until the old chunk loads.

**Built.** A remembered source that pauses is forgotten when two things are both true: it has been silent for 10 min
(`MTEHatchGRBReceptor.FORGET_PAUSED_TICKS` = 12,000 ticks), and another running source is feeding the port this second.
With no other source arriving, nothing changes: the port keeps the old source and Q-8 holds the host still. On the dev
server the re-aimed Homogeny then read σ 1.50, speed × 1.5, and Entanglement Medium open.

**Suggestion.** Keep. **Alternatives:** forget at once when any running source arrives (a quasar unloading for a
moment would then drop σ on a two-quasar host), or never forget (the old behaviour).

### P4-84 The quasar's b setting clamps to the coil tier [quasar, OpenComputers, full test]

**Context.** `bts_quasar.setField(99)` on a quasar with T3 coils returned 4.0. The coils capped the b in use at 3, but
the GUI and OpenComputers reported a b the machine cannot carry. The automation doc says setField 99 gives the coil tier.

**Built.** `MTEQuasarEngine.setBSetting` clamps to the coil tier. The clamp is 4 while the coils are unknown (unformed,
or the NBT load before the first walk). On the dev server, setField(99) now returns 3.0 on T3 coils. A Burst Control
Hatch in FIELD mode still maps 0-15 onto 0.5..the coil tier.

**Suggestion.** Keep. After a coil upgrade, the player raises b once.

### P4-85 P4 machines mark their chunk modified every second [persistence, full test]

**Context.** GT marks a chunk modified only when an inventory changes, and Minecraft saves only modified chunks (or ones
with entities). On the dev server, a Wormhole Mouth reloaded a 20-minute-old save after a restart: its SpaceTime used
went 4,864 L → 228 L. A rig crown's lifetime EU went 61.1e15 → 10.2e15 the same way. Nothing in the mouth's chunk had
an inventory change. A quasar in that position would lose its growth (mass, spin, counters) after a restart.

**Built.** These mark their chunk modified once a second while they run: the quasar (`second`), every conduit element
(after `upkeep`), the crowns (after `second`), and a lit GRB Receptor Port. This is one `IGregTechTileEntity.markDirty()`
each, which only sets the chunk's flag. After the fix, a restart kept the mouth at 328 L (it had 260 L, plus 17 s more)
and the crown at 11.8e15 EU.

**Suggestion.** Keep. **For master** (not P4 code): the KEE's hole mass, the Homogeny / Reality runtime state and the
Directive / Sensor / Burst Control hatch settings rely on the same luck. The KEE was fine in the test because its bolt
buses change every tick. A master follow-up could add the same one-line `markDirty()` to the KEE's and the H/R
machines' second.

**Done on master (B-112).** The KEE already did it (B-71); the Eye, the Homogeny, both Reality machines and the Kerr
modules now do too, and the Burst Control Hatch marks on a mode change. Tested with a clean stop and a killed server.

### P4-86 Casimir Plate tooltip; older casings still without one [rule 14, lang lint]

**Context.** The full test's lang lint found the following:

- Every recipe-result id has its RC-2 `GT5U.gui.text.recipe_result.<id>` key. The lint's one miss is the Kerr module's
  `linked` state, which is never a failure.
- Every item has `.name` and its `.tooltip`.
- Every recipe map's name key exists. `gg.recipe.precise_assembler` is GoodGenerator's own map.
- Every literal GT5U key exists in the RC-2 GT lang.
- Rule 14 is not met everywhere: slCasing metas 1-8, 10-15 and slCasing2 metas 0-2 have names but no tooltip. The
  Casimir Plate (slCasing:14) is one of them, and P4 made it a main block: 7,145 in a Junction, every throat lining,
  and the quasar's port rings.

**Built.** A tooltip for the Casimir Plate. It names every machine that uses it and where it is made. The other
pre-P4 casings are left to master: they are not P4 blocks, and the tidy pass owns them.

**Suggestion.** Master's tidy pass gives slCasing 1-8, 10-13 and 15 and slCasing2 0-2 one tooltip line each, in the
slCasing3 style.

**Done on master (B-112).** Each has its tooltip now, and the Casimir Plate's was joined onto one `.lang` line (it
was split over three, so only its first sentence showed).

### P4-87 Full dev-server test: what the benches do to a world [test bench, observation]

**Context.** Some bench verbs used by the full test change a world on purpose.

- `/btskerr ocloop` collapses the hole and re-opens it at 8 SM, to prove the OC `collapse()` / `open()` calls.
- `/btseye star … fill` tops every buffer to 10^12 L.
- `/btsconduit rig` force-loads 520 chunks.

Bench chunk tickets are dropped at a restart. FML's warning "registered persistent chunkloading data but doesn't seem
to want to be called back" is that drop: the bench registers its callback only when it is first used.

The pre-P4 base (c8f8ca7) has no `receptor` or `occall` verbs in `/btskerr`. On that jar they fall through to the
build form and rebuild the Kerr at T1. The full test saw this while making the legacy world, and a forced T2 rebuild
put it back.

**Built.** No change. The test world `p4test` is a bench world.

**Suggestion.** Keep. Never run `ocloop` on a Kerr you want to keep.

### P4-88 The T7 Heavy Link test fire has a ceiling too [Eye T7, automation, review fix]

**Context.** P4-79 sets a floor on Slots [8] (6 Bu/t), but there was no ceiling. The test fire sends J1 all to the Eye
and J3 75 % to the halo, so the rail keeps a quarter of the stream. With Mk II throats (384 Bu/t), a grown quasar puts
far more on the rail than the binary Space valve can cool.

The prototype's T7 rig, with Mk II throats and the test fire on (harness, 1 h, 3 seeds):

| Quasar mass | Mean stream | Result |
|---|---|---|
| 250 SM | 29.9 Bu/t | 0 failures |
| 265 SM | 33.6 Bu/t | 0 failures |
| 280 SM | 37.5 Bu/t | 0 failures, but 3 % of the time in zone 3 |
| 300 SM | 43 Bu/t | 35 failures |
| 415 SM | 82 Bu/t | 370 failures; Triangulum lost |

**Built.**

- Slots [8] at T7 needs the linked quasar's mean stream to be from 6 to 32 Bu/t
  (`EyeStarRuntime.HEAVY_TEST_FIRE_MAX_STREAM`).
- Above 32 Bu/t, the Galaxy panel says "Heavy Link test fire held ... Test-fire with J1's Eye share at x". x is 16 Bu/t
  ÷ the stream, which puts 4 Bu/t on the rail and 12 on the halo.
- OpenComputers `getUnlock()` gives `quasarStream` and `testFireShare`.
- The Directive Hatch tooltip and the questbook contract state the 6-32 Bu/t window.
- `GalaxyDirective`, QuasarSim and the golden are unchanged (38/38, 78/78).

**Not fixed (outside this finding).** The same harness shows the standard T7 rig failing at 300 SM with Mk II throats
even without the test fire: 83 failures in 30 min, and 645 at 415 SM. J1's P loop cannot hold a stream that big
either. The design's simulations stop at Mk I's 24 Bu/t clip.

**Suggestion.** Keep the ceiling. For P5 or the next design pass:

- a Directive "test fire preset": an analog level for J1, so a redstone rig follows the quasar's growth;
- a look at the T7 rig above 300 SM, for example a J1 out-3 tap to crowns.

### P4-89 Q-8 now pauses a non-Eye host's link work instead of holding σ [hosts, review fix]

**Context.** Q-8 (§3.1, R.6) says "a source that is not loaded pauses its hosts". The hosts kept σ at its last value
while paused, and only the Eye really froze (P4-68). So after one link, unforming or un-chunkloading the quasar kept
these going for free until the host's chunk reloaded:

- the KEE's bleed relief, its 768 SM cap and the Superdense refinery;
- the Homogeny's speed bonus and Entanglement Medium;
- Reality Diffraction's burst-driven speed.

**Built.**

- `BurstHost.sigmaInForce()` is the 60 s mean σ, or 0 while paused. The KEE (`burstSigma`), the Homogeny and Reality
  Diffraction (`quasarSigma`) use it.
- While paused:
  - the bleed is ×1 and the cap is 512 SM;
  - a link-gated run pauses and is kept (SL-Q23, Entanglement Medium, Reality's burst-driven runs);
  - no link-gated run starts;
  - a new Homogeny mix runs at ×1.
- Work that needs no link carries on (Degenerate Stellar Mixture, ordinary module recipes): no host needs a link
  (P4-74).
- The 60 s mean is still held, so the link resumes where it was when the quasar runs again.

**Dev server** (port 25581). Before the test, a KEE T2 at 700 SM had σ 2.00 with SL-Q23 running. One quasar casing was
then removed (FROZEN):

- the KEE said PAUSED: bleed ×1.00, cap 512, held at 512;
- SL-Q23 stopped at 10,525 / 12,000;
- the module's parallel fell from 3,328 to 2,816.

Within 13 s of putting the casing back: σ 2.00, cap 768, held at 700, and SL-Q23 ACTIVE again (10,799).

**Suggestion.** Keep. **Alternative:** hold σ for a short grace period (about 60 s), then let it decay to 0.

### P4-90 Kerr modules count the mass only up to the cap in force [Kerr, review fix]

**Context.** Losing the link dropped the setpoint cap, but the module parallel read `kee.mass()`. At 768 SM the
Hawking bleed is tiny: 768 → 729 SM takes about 95 h of loaded time, and 768 → 512 SM about 460 h. So one link gave a
near-permanent +23 % parallel, not the reversible bonus §3.1 describes.

**Built.** `MTEKerrModuleBase.bonusParallel()` uses min(M, `kee.maxSetpoint()`). These are unchanged:

- the setpoint, the mass and the slow bleed (P4-80);
- speed, which is ×1 at both 512 and 768 SM.

On the dev server the bonus went from 3,328 to 2,816 when the link paused, and back when it returned.

**Suggestion.** Keep. **Alternative:** bleed the hole toward 512 SM at a set rate while it is unlinked.

### P4-91 The quasar's and the Junction's setters refuse NaN [OpenComputers, GUI, review fix]

**Context.** These setters clamped with `Math.max` / `min`, which lets NaN through:

- the quasar's `setFeed`, `setBSetting` and `setPurgeAt`;
- the Junction's `setRatio` and `setOut3Share`.

One `quasar.setFeed(0/0)` turned E, Φ and M into NaN and saved them. One `junction.setRatio(0/0)` turned every stream
through it into NaN, and that spread to the receptors and the Eye's field. A modified client could do the same through
the C2S sync values.

**Built.**

- These setters ignore any value that is not finite.
- When the quasar loads its NBT, any saved double that is not finite is reset: M to the seed mass, Integrity to 100,
  everything else to 0.
- The Junction loads its ratio and out 3 through the setters.
- A non-finite owed upkeep on an element resets to 0.

**Dev server.**

- `btsquasar feed … NaN` kept f 0.85, and `b … Infinity` kept b 3.0. The disc carried on (E −24).
- `junction … NaN 2 NaN` on a Junction kept c 0.70 and out 3 at 20 %.

**Not changed.** The Stellar Field Sensor's `setThreshold` is master code. A NaN threshold only makes its comparisons
false. A master follow-up could add the same one-line check.

**Suggestion.** Keep.

### P4-92 A receptor warms up its remembered sources after a load [conduit, Q-8, review fix]

**Context.** The sources a port remembers for Q-8 were loaded with tick 0. On the first second after a restart, a
running quasar whose conduit elements had not registered again yet was dropped, and the host read zero. That reset the
Eye's saved Stability or Heavy Link streak.

**Built.**

- For 200 ticks from the port's first reading after a load (`RESTORE_WARMUP_TICKS`: the startup check plus two element
  seconds), a remembered source that has not arrived pauses the host (Q-8). It is no longer dropped.
- After the warm-up the usual rules apply.
- The restored entries keep tick 0, so a load does not restart P4-83's 10-minute forget clock. A first version set them
  to the current tick, and on the dev server a KEE then stayed PAUSED after a restart because of an old unloaded
  quasar. That was fixed before the test.

**Dev server.** After a restart, both remembered quasars were already FORMED and running, but not feeding. The KEE's
port read PAUSED for about 10 s, then "not linked". With the injection on again, it read 0.50 Bu/t LINKED at the first
check.

**Not run.** An Eye link streak across a real restart: the test Eye's links are already done.

**Suggestion.** Keep.

### P4-93 The conduit tables no longer pin unloaded worlds [conduit, review fix]

**Context.** `ConduitNet.TABLE` and `BurstDocks.TABLE` are `WeakHashMap<World, …>`, but each entry reached its world
(Entry → MTE → tile → `worldObj`). So the key was never collected, and every unloaded dimension leaked its WorldServer.

**Built.** Each entry holds its element or dock in a `WeakReference`. The MTEs register themselves, so a loaded element
stays reachable through its tile. No world-unload event handler is needed.

**Dev server.** A dock was still found (the P4-94 test). The full ConduitNet rig was not raised again.

**Suggestion.** Keep.

### P4-94 A misaligned dock vents [quasar, review fix]

**Context.** A crown docked upright under the bottom cap, or a mouth laid the wrong way round, returned 0 from
`acceptJet`. So the whole jet was lost, but `vented`, Disc [8] and the readouts all stayed clear. The tooltip says "an
unfit mouth vents".

**Built.** A misaligned dock returns `IBurstDock.REFUSED`, and the quasar counts the jet as vented. The readouts say:

- GUI: "-> venting: <dock> misaligned";
- scanner: "venting (<dock> misaligned)";
- OpenComputers docks: "venting".

A throat or crown cap that clips is still not venting.

**Dev server.** A Crown Mk I was built upright (facing east) under the bottom cap of quasar 20800: "Bottom jet:
15.2 Bu/t -> venting (Crown Mk I misaligned)". The crown is left standing at 20812, 34, 19928 in `p4test`, a bench
world.

**Suggestion.** Keep.

### P4-95 Live, the quasar rig's collapse purge lands late: the Magnetar Collapse mostly fails on real redstone [automation, showcase v2 finding]

**What happened.** With the time warp (B-117) the showcase ran the standard quasar rig live for the first time on the
T6 -> T7 Magnetar Collapse (`/btsquasar autorig <quasar> 0 eye <Eye>`: no fast-forward, the rig then acts every
Eye field second with real redstone blocks against the Burst Control Hatches, the conduit is P4-75's maths).

- Fresh 64 SM quasar, milestones done: the collapse began and FAILED 34 times in 11,900 game s (Breach, P +103.5;
  "the shock would land at P +120 with 1.47 Bu/t now; 13.5 Bu/t on the collapse second cancels it all").
- 136 SM quasar: 5 failures, then COMPLETE on the 6th re-run (845 s). The failed collapses cancelled 29-47 of the
  120 shock; the fast-forward bench (and the virtual-quasar `galaxy` bench, lock-stepped) cancels 120 of 120.
- The Galactic Merger T7 -> T8 on the same live rig: COMPLETE in 1,920 s, first try.

**Why.** Galaxy Transition [4] ("purge now") goes on at overfeed second 299 for a collapse at 300 (GalaxyDirective
`ASSIST_LAST`). The fast-forward bench runs, inside one second: the rig's action on last second's code, the quasar's
disc second (the purge erupts), the conduit, the Eye's second, so the eruption lands on the collapse second. Live,
each hop is its own tick: the Directive Hatch shows the code, the redstone reaches the Burst Control Hatch, the quasar
acts on its own second (a different phase), and the conduit hands the Eye the quasar's last result at the Eye's next
second. The eruption arrives one to two seconds late and most of the shock lands. A player's real wiring (and a real
conduit) has the same hops.

**Question for P4.** Raise the assist's lead (purge on second 297-298, or a window the 3 s eruption covers), make the
collapse read the eruption over a short window, or document that the purge needs a lead timer? The showcase films the
collapse on the lock-stepped bench model and says so in the caption; the merger is filmed on the real quasar.

**Fixed (B-118).** The cue leads: Galaxy Transition [4] is on for overfeed seconds 298 and 299 (one rising edge), the
hold [8] ends at 297, and the eruption's 3 s cover the collapse second for any wiring that answers within about 1.5 s
(prototype, fixture and golden changed with it; 38/38). The live rig no longer carries the cue on its own block (which
acted a whole second late by construction): it fits the wire a player would, a GT Advanced Redstone Transmitter
(Internal) on the Galaxy Transition hatch and a Receiver (Internal) on the quasar's Burst Control Hatch. On a headless
dev server, 13 sites with the quasar's second 3-19 ticks behind the Eye's: the purge landed on second 298 every time
and the eruption was on the Eye on the collapse second. What it then cancels is the quasar's stream: the 34 failures
at 64 SM were the weak stream of a fresh, spinless hole (1.3 Bu/t), not the timing.
