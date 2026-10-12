# Questbook integration: the Singularity Ladder in the GTNH 2.9.0-RC-2 book

Status: **spec**, 2026-10-13, for the `questbook` branch (worktree `addon-qb`). No Java, no generator code and no quest
JSON are written yet; section 6 splits the work between the next units.

**Rulings this implements** (build brief, rules 14 and 15):

- *"make sure everything the player needs to find out the system is either in the questbook or in the tooltip"*:
  every quest below teaches its system. It says what the system is, how to build it, how to run and automate it, and
  the key numbers, and it points to the tooltip and GUI for the rest. Nothing is left only in docs.
- *"While the blocks we dont have arent in then just add a tickbox quest but the pack's own book may be reworked freely"*: content that is not built yet gets a **checkbox placeholder** that holds its real id and real tasks. The
  generator turns it into a real quest when its feature flag flips (§4.3). The pack's own quests are moved, rewritten
  and re-gated freely (§3). Player progress is preserved (§4.6).
- *"nice and very tidy designs"*: every chapter is laid out on a 24-unit grid. The main chain runs along one centre
  line, gates sit on the row above it and products on the row below. The generator **asserts** that no two quests
  overlap and that no prerequisite line crosses a quest icon (§4.2), so tidiness is enforced at build time.

**Sources read for this spec.** All of them are read-only references.

- The RC-2 instance `DefaultQuests` (`rc - 2 TO THE STARS`): `QuestLinesOrder.txt`, `QuestSettings.json`
  (`pack_version` 2171), and every quest and chapter file (3,813 quests). Chapters used: Tier 11 - UIV (57 entries),
  Tier 12 - UMV (46) and Endgame Goals (31).
- Every pack quest id, position, prerequisite and task below was read out of those files by a dump script
  (local work folder `qbspec/dump.js`, `rev.js`).
- The design: `designs/questbook.js`, `designs/docs/questbook.md` (D-44, D-47, D-48), `star-lifecycle.md` (D-49) with
  the addon's `docs/d49-build-plan.md` and its user answers, `addon-kerr-redesign/docs/kerr-redesign.md`,
  `bec-expansion-proposal.md` and `quasar-p4.md` (P4, proposed).
- The built code on `questbook` (HEAD 6c41ce7, D-49 unit 6), and on `kerr-redesign` (859f5b7) for the KEE and the
  modules.
- The RC-2 GT sources (`port-tools/rc2/src`), used for item ids and one recipe check.

**Corrections to the task's premises:**

- The existing chapter is **not** `47990-48028`. It is chapter `190/47990` plus quests `48000-48034`.
  `BQBridge.CHAPTER_QUEST_LOWS` already lists `48029-48034` (The Fuel Line row), and `48020` is the Unit One entry
  quest. All of these are kept.
- RC-2's UMV chapter is not the beta-1 layout drawn in `questbook.js`. RC-2 added an EOH ladder inside UMV: the
  field-generator tier quests T2-T9, Raw Stellar Plasma, White and Black Dwarf Matter, Universium, Eternity, MHDCSM,
  Magnetic Monopole Matter, the four BEC component tiers and The SUCC. There is no "Black Hole Seed" quest anywhere in
  the RC-2 book. The layout below is re-fitted to RC-2's real positions.

---

## 0. Decisions at a glance

| # | Decision | Why |
|---|---|---|
| D1 | **The pack's quests are edited only in the live world database, at runtime.** No pack file in `DefaultQuests` is ever modified. | Pack files change with every RC. Our files only ADD (the merge rule in `questbook/README.md`). Runtime edits match by id + name and skip on drift (§4.7). |
| D2 | **The whole overlay ships inside the mod jar** as one bundle, `assets/beyondthestars/questbook/overlay.json`. It holds our chapters, our quests and the compiled pack edits. `BQBridge` installs from the jar. `DefaultQuests` keeps a mirror of our own chapters for new worlds and for `/bq_admin default load`. | `/btsquests install` no longer depends on a manual merge, and the book always matches the jar it came with. |
| D3 | **Auto-install on server start** (config `questbookAutoInstall`, default on) whenever the world's applied bundle differs from the jar's, or a cheap check finds our content missing. | New worlds get the edits on their first start. A pack update or a `/bq_admin default load` heals itself on the next start. |
| D4 | **Pack edits are applied to the stored ORIGINAL** of each pack quest, never to the already-edited quest. | Re-install is exactly idempotent, uninstall restores bit-identical pack quests, and non-idempotent ops (append text) stay safe. |
| D5 | **The KEE and the Eye take over the pack's BHC and EOH quests in place** (same id, same slot). Dependents keep working, and a player who finished the BHC quest keeps it finished. | D-44 ("Kerr T1 takes the Black Hole Compressor's place") and the "rework the pack's book freely" ruling. |
| D6 | **Placeholders are real quests with `needs` features.** They are checkboxes plus a planned note until built, then real tasks with the same id. Ticking a placeholder never counts for the real step (§4.6). | One source and one id, so the book upgrades itself as P4/P5 land. |
| D7 | **Stage-aware edits.** An edit can carry `needs` / `unless` features. Eternity, Magnetic Monopole Matter, Perfected Components and The SUCC sit in **The Endgame** until the Eternity-on-Ergomatter swap ships, then move to **UXV**. | The book never shows a path the recipes cannot walk. |
| D8 | **Placeholders only for machines, blocks and Eye tiers** (my "blocks we don't have"). Planned *recipe moves* get no quest. Examples: Hexanite on the T4 Eye, the Reinforced Resonance Chamber on the T4 Eye, the BEC condensates. | Tidy. A quest that says "this recipe will move some day" teaches nothing. Each one is noted where it matters instead. |
| D9 | **The MAX parts ring is collapsed** from 13 placeholders to a hub plus 4 group placeholders, plus the pack's MAX Circuits. | Tidy. It expands to individual quests when P5 designs the parts (question QB-3). |
| D10 | **With pack edits disabled**, two fallback quests of our own (`48103` KEE, `48109` Eye) appear next to the untouched BHC and EOH quests. | Rule 14 still holds with the edits switched off. |

**Counts.**

- **Chapters:** 3 new chapters of ours (Tier 13 - UXV, Tier 14 - MAX, The Endgame) and 3 pack chapters changed
  (Tier 11 - UIV, Tier 12 - UMV, Endgame Goals). Our plant chapter is unchanged.
- **New quests:** 42, plus 2 that appear only when pack edits are off.
  - 24 of the 42 are placeholders, and 4 are info checkboxes.
  - By chapter: UIV 3, UMV 10, UXV 14, MAX 14, Endgame 1.
- **Pack edits:** 61 pack quests (§3).
  - 2 taken over in place (BHC → KEE, EOH → Eye T1).
  - 14 rewritten or re-positioned inside UIV and UMV (texts, gates, the new grid).
  - 43 moved into our chapters (4 of them stage-aware).
  - 2 trophy item swaps.
  - Several of the above also get prerequisite rewires.
  - Nothing is hidden: every quest made obsolete by our removals is rewritten, because TecTech's field generators
    are still the Eye's tier gate.

---

## 1. Ids, chapters and order

`questIDHigh = 190` for everything of ours (`new UUID(190, low)`), as today.

| Range | Use |
|---|---|
| line `47990` | **Beyond the Stars** (the plant chapter): unchanged |
| line `47991` `AAAAAAAAAL4AAAAAAAC7dw==` | **Tier 13 - UXV** (new) |
| line `47992` `AAAAAAAAAL4AAAAAAAC7eA==` | **Tier 14 - MAX** (new) |
| line `47993` `AAAAAAAAAL4AAAAAAAC7eQ==` | **The Endgame** (new) |
| lines `47994-47999` | reserved |
| quests `48000-48034` | the plant chapter (`48020` = Unit One in How to Generate Power): unchanged |
| quests `48035-48099` | reserved for the plant chapter |
| quests `48100-48139` | the ladder in UIV and UMV (Kerr, Eye T1-T4) |
| quests `48140-48179` | UXV |
| quests `48180-48219` | MAX |
| quests `48220-48249` | The Endgame |
| quests `48250-48299` | spare |

**Ids are explicit in the source file, never positional**, unlike `gen-questbook.ps1`'s `$quests` array. An
append-only ledger, `questbook/id-ledger.txt` (one `low name` line per id ever shipped), makes the generator throw if
an id is reused for a different quest or disappears.

**Chapter order.** RC-2's `QuestLinesOrder.txt` runs `… Tier 11 - UIV, Tier 12 - UMV, Endgame Goals, Feeding
Yourself …`. Ours go in **directly after Tier 12 - UMV**: Tier 13 - UXV, Tier 14 - MAX, The Endgame. The pack's
Endgame Goals follows them (it keeps two legacy trophies, §3.6). The plant chapter stays parked last, as today.

- `BQBridge` finds UMV's index in the order list by its line id (`l37V2WvbSHGZQ5oMgAfBuw==`), and falls back to
  "last" if it is missing.
- `OrderLine.txt` becomes `OrderLines.txt`, with an "insert after `l37V2WvbSHGZQ5oMgAfBuw==`" header that README
  merge step 6 follows.

**Pack chapter ids** used below: UIV `ytf2QCzUSk6ihcYeRbwblw==`, UMV `l37V2WvbSHGZQ5oMgAfBuw==`, Endgame Goals
`AAAAAAAAAAAAAAAAAAAAHQ==`.

**Registry ids used in tasks.**

- **Verified in this session:**
  - KEE `gregtech:gt.blockmachines:28114`, Eye `:28115`, Stellar Field Sensor Hatch `:28116`, Kerr Compressor
    Module `:28117`, Kerr Extractor Module `:28118` (BTSMachines ledger).
  - Black Hole Seed `gregtech:gt.metaitem.01:32418` and Collapser `:32419` (`IDMetaItem01` 418/419 + 32000).
  - Field Generator UXV `gregtech:gt.metaitem.01:32047`.
  - TecTech's EOH `gregtech:gt.blockmachines:15410` and the BHC `:3008`.
  - Our items `beyondthestars:<SLItems field>` (`BTSItems.define` registers the field name).
  - Our fluids: `btsexcitedstellarplasma`, `btsrawhyperdenseplasma`, `btshyperdensestellarmixture`,
    `btsantipolarizationagent`, `btsmagnetarmatter`.
  - Pack fluids `rawstarmatter`, `molten.whitedwarfmatter`, `molten.blackdwarfmatter`, `molten.universium`.
- **To be verified by `/btsquests verify` (§6, unit Q4):** `temporalfluid` (Tachyon Rich Temporal Fluid), the ore
  names `boltNeutronium` and `dustTengamAttuned`, and the TecTech UXV laser hatch ids.

---

## 2. Every chapter and quest

**Format.**

- Positions are BetterQuesting units, top-left corner, with the size in brackets (24 / 48 / 72).
- "Pack" means an RC-2 quest by its base64 id. Our quests are written as their low (`48100` = `190/48100`).
- Task kinds:
  - `R` = `bq_standard:retrieval` (consume 0);
  - `OR` = `optional_retrieval`;
  - `F` = `bq_standard:fluid`;
  - `C` = checkbox;
  - `bom:<piece>` = an optional retrieval whose counts the generator derives from
    `structures/manifest.json` (never typed by hand, §4.2).
- Rewards are pack style: mostly none, and small and useful where given.
- **Every placeholder** (marked ⧗) is emitted as a `C` task with `countAsQuest 0`. Its description ends with the
  standard block:

  > [note]Planned. This machine is not in Beyond the Stars yet: tick the box to keep going. When it ships, this quest
  > becomes a real task under the same id, and a ticked box is cleared so the real step counts.[/note]

  The placeholder's **real tasks** are listed in its row. The generator uses them when the quest's `needs` features
  are built.

**Description style.**

- Section signs come through the existing `{6}{l}{r}` tokens and `Fmt()`, so the source stays ASCII.
- `[note]` and `[warn]` blocks are used. Headings are `{e}{l}BUILD{r}`, `{e}{l}RUN{r}` and `{e}{l}AUTOMATE{r}`,
  matching the plant chapter's `DO THIS` / `DONE WHEN` house style.
- Numbers come from the built constants. Every quest that shows a structure ends with "NEI's Multiblock tab and the
  controller tooltip have the rest."

### 2.1 Tier 11 - UIV (pack chapter): the BHC's corner becomes the Kerr

The pack's BHC quest `6OibD9bRQwq_hBPWz-y9xQ==` at **456,312 (48)** becomes the KEE (edit E01, §3.1). Three quests of
ours are added around it. The chapter's other 56 quests stay where they are.

| Id | Name | Pos | Prereqs | Tasks | Rewards |
|---|---|---|---|---|---|
| pack `6Oib…` (E01) | **Kerr Ergosphere Engine** | 456,312 (48) | pack (unchanged): Another Type of Steel (t1), Your First SpaceTime, UIV Energy Hatch | R 1× `28114`; OR `bom:kerr_t1` + 1× Black Hole Seed | 1× Black Hole Seed, 1× Black Hole Collapser |
| 48100 | **Kerr Compressor Module** | 516,348 (24) | E01 | R 1× `28117`; OR `bom:kerr_compressor` | none |
| 48101 | **Kerr Extractor Module** | 516,312 (24) | E01 | R 1× `28118`; OR `bom:kerr_extractor` | none |
| 48102 | **Feeding the Governor** | 420,324 (24) | E01 | R 64× ore `boltNeutronium` (HOLD → CLAIM → INSTALL) | none |
| 48103 | *Kerr Ergosphere Engine (fallback)* | 420,276 (24) | pack `6Oib…` | as E01 | as E01; **emitted only when pack edits are off** |

The two modules sit stacked right of the KEE (compressor below, so the pack's "Bending Space and Time" line from it
clears the extractor). The governor quest sits in the gap left of the KEE, between Your First SpaceTime (ends at
x 408) and the KEE (starts at x 456).

**E01, Kerr Ergosphere Engine** (name style `{9}{l}{n}`, as the pack's big UIV quests):

> As the materials around you achieve transcendental properties, pistons and gases stop being enough. The answer is
> gravity: a black hole, held in a cage of mirrors and coils, spinning fast enough that its ergosphere can be put to
> work.
>
> The **Kerr Ergosphere Engine** (KEE) holds the hole and nothing else. It runs no recipes. The machines that use the
> hole are the **Kerr modules**: separate machines standing in the cage's arcs (next quests).
>
> {e}{l}BUILD{r} Hologram Projector stack 1 projects Kerr T1, about 75 × 66 × 75. Chunk-load all of it. It is the
> Black Hole Compressor's own casings (Extreme Density Space-Bending Casing, Background Radiation Absorbent Casing,
> Hawking Radiation Realignment Focus, Naquadah Alloy frames) plus Superradiant Mirrors, Mirror Backing, Ergosphere
> Field Coils, Beam Focusing Quadrupoles, Beam Dumps, Harmonic Lining and Bolt Injector Rails. The optional task is the
> full bill. Hatches: 1 laser or up to 2 energy hatches in the governor arc, and up to 8 **bolt buses** (plain input
> buses) on the cage. No maintenance hatch. Markers 7 and 8 in the hologram show where the module controllers go.
>
> {e}{l}RUN{r} Open the GUI.
> 1. A **Black Hole Seed** (in a bolt bus, or the Open button) opens the hole at **8 SM**.
> 2. The hole bleeds mass as Hawking radiation: **3.2 / M² SM a tick**. Small holes die fast.
> 3. The **governor** fires bolts from the bolt buses to hold your **setpoint (2-64 SM at T1)**:
>    - Neutronium 1/64 SM, Cosmic Neutronium 1/32, Infinity 1/16, Transcendent Metal 1/8, SpaceTime 1/4 SM per bolt;
>    - at most 4 / 8 / 16 bolts a tick, set by the **lowest** rail (Mk I / II / III).
> 4. While open, the hole draws **8,388,608 EU/t (1 A UEV)** of containment. Without power or bolts it bleeds, and
>    below 2 SM it evaporates.
> 5. A **Black Hole Collapser** closes it safely (the Collapse button, or in a bolt bus with Auto on).
>
> [note]A bigger hole gives the modules more parallels; a smaller one makes them faster. The GUI shows mass,
> setpoint, bolt stock, stability and every linked module.[/note]
>
> [note]The Black Hole Compressor is gone: its recipe now makes this. Every recipe that consumed a BHC controller
> (the Spacetime Compression Field Generators, the Compressed Circuit Array) takes a Kerr Ergosphere Engine controller
> instead. A BHC you already placed keeps working.[/note]

**48100, Kerr Compressor Module:**

> The Black Hole Compressor's job, done next to a real black hole. The module is its **own machine** in the KEE's
> south arc, with its own GUI and its own hatches.
>
> {e}{l}BUILD{r} Place this controller where the KEE's hologram shows **marker 7**, facing out of the cage. Then
> project the arc from the module's own controller (one tier). Any arc socket takes input buses or hatches, output
> buses or hatches, and 1 laser or up to 2 energy hatches. There is no maintenance hatch.
>
> {e}{l}RUN{r} It runs GT's Compressor and Neutronium Compressor recipes. There are no circuits and no modes: the
> recipe picks the map. It runs only while the KEE's hole is **open**. When the hole closes it pauses and keeps its
> recipe, and it resumes when the hole reopens.
>
> The hole sets the bonus, fixed when a recipe starts:
> - **parallel = 8 × voltage tier × ⌊√M⌋** (at UIV: 352 at 16 SM, 704 at 64 SM);
> - **speed × (3200 / M) / 50**, from 1 to 4.
>
> The module costs the hole nothing.
>
> [note]At Kerr T2 it also runs **Kerr Densification**, which needs a hole of at least 128 SM (UMV
> chapter).[/note]

**48101, Kerr Extractor Module:**

> The east arc's machine: GT's **Extractor** recipes, items only.
>
> {e}{l}BUILD{r} Place the controller where the KEE's hologram shows **marker 8**, on the 340° ray, facing out. Then
> project the arc from it. It has five sockets: at least 1 input bus, 1 output bus and 1 energy (1 laser or up to 2
> energy hatches).
>
> {e}{l}RUN{r} It works like the compressor: open hole only, the same parallel and speed bonus, and it pauses
> without losing its recipe. Either module is enough to start; you can run both.

**48102, Feeding the Governor:**

> A hole is cheap to **hold** and expensive to **grow**. The bleed is 3.2 / M² SM a tick:
> - at 16 SM, about 0.8 Neutronium bolts a tick (16 a second);
> - at 64 SM, about 1 a second.
>
> Without bolts, a 16 SM hole lives about 21 seconds and a 64 SM hole about 23 minutes.
>
> {e}{l}AUTOMATE{r} Stock the bolt buses from ME: an ME Interface or export bus per bus, or a Crafting Input Proxy.
> The governor picks the bolt it needs to hold the setpoint. Better bolts carry more mass per bolt, so the rail cap
> stops mattering. Put a Collapser in a bolt bus and switch **Auto** on: the hole is then closed and reseeded without
> you.
>
> {a}{l}DONE WHEN{r} you are holding 64 Neutronium bolts (HOLD → CLAIM → INSTALL).

**48103 (fallback, pack edits off)** carries E01's text with the last note replaced by "[note]The Black Hole Compressor
quest is left as the pack wrote it, because pack edits are turned off (`questbookPackEdits`).[/note]".

### 2.2 Tier 12 - UMV (pack chapter): the right half becomes Eye T1 to T4 and Kerr T2

The pack's EOH quest `kazCPEWoSmiZhR4bkHlEBQ==` at **636,144 (72)** becomes Eye T1 (E10). Everything the RC-2 pack had
to its right is moved, either within UMV onto the new grid or out to UXV, MAX and The Endgame (§3). The left half
(x < 636) is untouched apart from two text edits.

**Grid.**

- The main chain runs on centre line y 180: Eye T1 (center 672), T2 (804), T3 (924), T4 (1080), WDM (1188).
- The compression gates sit straight above each tier on centre line y 84.
- Products and Kerr T2 sit below on centre line y 276.
- Teaching side quests sit in the free pocket left of the Eye, below "Are you Ready?".

| Id | Name | Pos | Prereqs | Tasks | Rewards |
|---|---|---|---|---|---|
| pack `kazC…` (E10) | **An Eye at the End of Time** | 636,144 (72) | pack (unchanged) + **A Second Ring has Hit the Godforge** `Qjg1J4kuS-6937dCEUqHUw==` (D-41) | R 1× `28115`, 1× `starChartSol`, 1× `stellarIgnitionCharge`; OR `bom:eye_core` | 2× `stellarIgnitionCharge` |
| pack `cEX6…` (E18) | A Solder Singularity | 660,72 (24) | unchanged | unchanged | — |
| pack `cO67…` (E12) | T2: Primitive | 792,72 (24) | unchanged (EOH, Solder t1) | unchanged | — |
| pack `Hgj4…` (E13) | T3: Stable | 912,72 (24) | unchanged | unchanged | — |
| pack `-fvT…` (E14) | T4: Advanced | 1068,72 (24) | unchanged | unchanged | — |
| 48119 | **The Time Dial** | 792,12 (24) | E12 | F 1,000 L `temporalfluid` | none |
| 48114 | **Eye T2: Sirius A** | 780,156 (48) | E10, E12 | R 1× `stellarMassCore`, 1× `starChartSiriusA`; F 1,000 L `btsexcitedstellarplasma`; OR `bom:eye_beam` | 1× `stellarIgnitionCharge` |
| 48115 | **Eye T3: Arcturus** | 900,156 (48) | 48114, E13 | R 1× `shellBurningCore`, 1× `starChartArcturus`; F 1,000 L `btsrawhyperdenseplasma`; OR `bom:eye_jet` | none |
| 48117 | **Eye T4: Core Collapse** | 1044,144 (72) | 48115, 48116, E14 | R 1× `galacticUpgradeCore`, 1× `starChartNeutronStar`; OR `bom:eye_arms` | none |
| pack `DVLa…` (E15) | White Dwarf Matter | 1164,156 (48) | **48117** (was the EOH quest) | unchanged (F 144 `molten.whitedwarfmatter`) | — |
| pack `RTzM…` (E16) | Reinforced Components | 1176,264 (24) | unchanged (WDM) | unchanged | — |
| pack `uXcz…` (E11) | Raw Stellar Plasma | 648,252 (48) | unchanged (EOH) | unchanged (F 1,000 `rawstarmatter`) | — |
| pack `fj2V…` (E17) | Stellar Catalyst | 720,312 (24) | unchanged | unchanged | — |
| 48113 | **Kerr T2: The Density Upgrade** | 780,252 (48) | pack E01 (UIV), E11 | R 1× `kerrDensityUpgrade`; OR `bom:kerr_t2` (the T2 difference) | none |
| 48116 | **Hyperdense Stellar Mixture** | 972,252 (48) | 48115, 48113 | F 1,000 L `btshyperdensestellarmixture` | none |
| 48118 ⧗ | **The Homogeny** | 876,336 (24) | E11 | real: R 1× Homogeny controller (MTE tbd) | none |
| 48110 | **The Stability Field** | 588,288 (24) | E10 | R 1× `28116` | 1× `28116` |
| 48112 | **Harmonic Lock** | 528,288 (24) | 48110 | C (a state, not an item) | none |
| 48111 | **Planets on the Eye** | 588,348 (24) | E10 | C (info) | none |
| 48109 | *Eye of Harmony (fallback)* | 708,264 (24) | pack `kazC…` | as E10 | **only when pack edits are off** |

**E10, An Eye at the End of Time** (the pack's name is kept; icon `28115`):

> So you have made yourself an Eternal DTPF or hopefully a few, and now it is time for the push onwards.
>
> Beyond the Stars replaces TecTech's Eye of Harmony with **one Eye that grows from tier 1 to tier 10**. It grows one
> band at a time, with a **live star** at its centre.
>
> {e}{l}WHAT IT IS{r} Put a **Star Chart** in the controller slot (it is not consumed) to choose what the Eye runs:
> Star Chart: Sol at T1. A dimension display block runs planets instead (see *Planets on the Eye*).
>
> {e}{l}BUILD{r} Hologram stack 1 = the T1 core. It is mostly TecTech's own Eye blocks:
> - Reinforced Spatial and Temporal Structure Casings;
> - Infinite SpaceTime Energy Boundary Casing;
> - Quantum Glass;
> - the three field generator families.
>
> On the plinth go gas input hatches (Hydrogen, Helium), an input bus for Stellar Ignition Charges, an ME Output Bus
> and an ME Output Hatch. Power is **wireless EU only**. Chunk-load the whole footprint.
>
> {e}{l}LIGHT IT{r} Sol lights when all of these hold:
> - a **Stellar Ignition Charge** is in a plinth input bus;
> - the buffers hold 10 cycles of upkeep plus a seed cloud of **100,000,000 L Hydrogen and 100,000,000 L Helium**;
> - the wireless network holds 10 cycles of EU.
>
> The protostar contracts for about 40 seconds, then lights.
>
> {e}{l}RUN{r} Every **60 s cycle**, Sol burns **50,000,000 L Hydrogen + 50,000,000 L Helium** and **1.51 × 10¹¹
> EU**, and makes **50,000 L Raw Star Matter** to the ME output hatch. Inputs drain into internal buffers every
> second, so ME stocking hatches are fine on the gas sockets. Upkeep is paid in full even when the star runs badly.
>
> [warn]The **Spacetime Compression Field Generators gate the tier**: an Eye of tier N needs every compression
> generator at tier N or higher. Crude runs T1.[/warn]
>
> [note]The Eye is a pure power sink. It makes matter, not EU. The next quests teach the stability field, planets,
> and how to unlock T2; the controller tooltip and GUI have the rest.[/note]

**E11, Raw Stellar Plasma.** The name and task are kept and the description is replaced:

> **First Light.** Sol's product, made 50,000 L a cycle × your yield.
>
> It goes into:
> - Sirius A's upkeep (25,000 L a cycle);
> - the **Stellar Mass Core** that unlocks T2 (256,000 L);
> - the **Kerr Density Upgrade** (4,000,000 L);
> - the Stellar Catalyst, as before.
>
> [note]Your first milestone is counting already: **20,000,000 L** made on this Eye and **2 h Resonant** opens the
> road to T2.[/note]

**48110, The Stability Field:**

> A live star is not a recipe. Every second the Eye updates a **stability field**. At T1 it has one gauge, **P**
> (pressure balance): minus is collapse, plus is expansion, and **0 is the target**.
>
> {e}{l}ZONES{r} The worst gauge decides the zone:
>
> | Zone | Worst gauge | Yield |
> |---|---|---|
> | Resonant | within ±10 | ×1.25 |
> | Stable | within ±40 | ×1.00 |
> | Strained | within ±70 | ×0.50 |
> | Critical | within ±100 | 0, and Integrity drains |
> | Breach | beyond ±100 | the star is lost |
>
> **Integrity** (0-100) refills while you are Stable and drains while you are not. At 0 the star is lost. Losing a
> star costs the star and its in-flight output, **never** blocks, cores or unlocked tiers. Relight it with a new
> charge.
>
> {e}{l}TRIM{r} Pour trim fluids into the **Field sockets**: 8 plain input hatches on the core band. They are never
> buffered, and ME stocking hatches there are refused. At T1, 1,000 L Helium is **P −2** and 1,000 L Hydrogen is
> **P +2**, up to 10 units a second.
>
> {e}{l}SENSE{r} A **Stellar Field Sensor Hatch** (8 sockets on the core band) reads any gauge against a threshold
> and emits redstone, digital or analog. The gauges: P, Θ, Ω, Π, Agent shield, Integrity, Stability, zone, event
> warning and upkeep.
>
> {e}{l}AUTOMATE{r} The standard rig:
> - a sensor on **P > +10** → a Fluid Regulator cover, or an ME fluid export bus with a redstone card → Helium into a
>   Field hatch;
> - a second sensor on **P < −10** → Hydrogen.
>
> Covers on the sensor's own side work too, wireless redstone transmitters included.
>
> [note]Sol settles at about P +35 (Stable) by itself, so T1 runs unattended. The rig earns the ×1.25. Higher stars do
> **not** settle.[/note]

**48112, Harmonic Lock:**

> Hold **Resonant for 600 consecutive real seconds** and the Eye locks in: **yield × 1.5** until the streak breaks.
> The GUI shows the streak. Every milestone's Resonant time counts real seconds too, so a tidy rig is how you climb.
> Tick the box when your Eye shows HARMONIC LOCK.

**48111, Planets on the Eye:**

> Planets are still **batch runs**, as TecTech ran them, but **without** Raw Star Matter or dwarf matter.
> - Put the dimension display block in the controller slot.
> - The planets by Eye tier: **T1** rocket tiers 0-3, **T2** 4-6, **T3** 7-8, **T4** the Deep Dark.
> - A tier N Eye runs every planet of tiers 1..N, with **×2 parallel per tier above** the planet's.
> - Compression above the planet's TecTech tier gives −3 % time per tier.
>
> [warn]One target per Eye: selecting a planet puts your star out.[/warn]

**48119, The Time Dial:**

> **Time Dilation** generators above the star's tier open the **time dial m**, up to **1 + 0.25 × (lowest Time
> Dilation tier − star tier)**.
> - Each +0.25 costs **10 × 2^(N−1) L/s of Tachyon Rich Temporal Fluid**, from the Field sockets.
> - m speeds up everything the star does (upkeep, product, EU, drift, noise and events) but **not your trims**. A
>   faster star is harder to hold.
>
> **Stabilisation** generators calm the star. Noise and event size are × (1 − 0.05 × (lowest tier − 1)), down to
> ×0.60 at tier 9.
>
> Set the dial with the GUI's ± buttons. It is locked at ×1 during transitions.

**E12 / E13 / E14, T2: Primitive / T3: Stable / T4: Advanced.** The names and tasks are kept, and the description is
replaced. Here is T2's; T3 and T4 change the tier and the star.

> The **Primitive** field generators, tier 2. In the Beyond the Stars Eye the three families do three jobs:
> - **Spacetime Compression is the hard gate.** Eye tier N needs every compression generator at tier N or higher.
>   Primitive lets your Eye reach **T2 (Sirius A)**.
> - **Time Dilation** opens the time dial, but only above the star's tier (*The Time Dial*).
> - **Stabilisation** calms the field.
>
> The families mix freely, and the lowest of each family counts. The hologram places compression at the trigger
> stack's tier, and never downgrades a generator already placed.

- T3 adds: "Stable gates **T3 (Arcturus)**."
- T4 adds: "Advanced gates **T4**, and the Core Collapse will not start without it."

**48114, Eye T2: Sirius A:**

> **Tiers unlock in order, in three steps.**
> 1. **Milestones** on the current star: **20,000,000 L Raw Star Matter** made and **2 h Resonant**. The GUI and
>    scanner count them, and they stay with the controller through a wrench.
> 2. **The core.** Right-click the controller with the **Stellar Mass Core**. It goes into escrow and is consumed
>    **only when the transition succeeds**. Sneak-punch with an empty hand to take it back.
> 3. **The transition.** Build the **beam band** (hologram stack 2: 2,856 Beam Band Casing), bring compression to
>    tier 2, buffer **5,000,000 L Raw Star Matter**, then put **Star Chart: Sirius A** in the slot while Sol burns.
>    **Mass Infusion** runs for 10 minutes: the RSPM pours into Sol and P is pushed **+4 a second**, so your rig must
>    hold. Survive it and Sol becomes Sirius A, with T2 unlocked for good.
>
> {e}{l}RUN{r} Sirius A takes **12,500,000 L H + 12,500,000 L He + 25,000 L RSPM** a cycle and makes **2,500 L
> Excited Stellar Plasma**.
>
> [warn]Sirius A drifts hard and breaches if left alone. Run the rig. Helium trims leave **ash: −0.5 % yield per
> 1,000 L** dosed that second, so a two-stage rig (small doses near the target, big ones far out) beats full-blast
> valves.[/warn]
>
> [note]A higher Eye still hosts lower stars, at ×2 parallel per tier above. There is one live star per Eye.[/note]

**48115, Eye T3: Arcturus:**

> A red giant: a helium core inside a hydrogen shell. It brings a second gauge, **Θ (core heat)**, which runs away on
> its own.
>
> {e}{l}UNLOCK{r} The milestones are **1,000,000 L ESPM** and **3 h Resonant** on Sirius A. Then:
> - put the **Shell-Burning Core** in escrow;
> - build the **jet crescent** (stack 3: 2,750 Jet Crescent Casing; the beam band must stand too);
> - bring compression to 3 and buffer **250,000 L ESPM**;
> - swap in **Star Chart: Arcturus**.
>
> **Shell Ignition** (10 min): Θ appears and ramps up, and the event ends in one forced helium flash.
>
> {e}{l}RUN{r} Arcturus takes **12,500,000 L H + 12,500,000 L He + 1,250 L ESPM** a cycle and makes **625 L Raw
> Hyperdense Stellar Plasma**.
>
> {e}{l}TRIMS{r}
> - Helium: P −1, Θ +3.
> - Hydrogen: P +3, Θ +0.5.
> - Super Coolant (100 L): P −1, Θ −3.
>
> To pull P down without heating the core, dose Helium **and** coolant together.
>
> {e}{l}HELIUM FLASHES{r} Every 20-40 minutes a flash adds **Θ +30 a second for 3 s**. The sensor's event channel
> warns **15 s ahead**. Pre-cool on the warning and you stay Stable.
>
> [note]The T4 milestones: **400,000 L RHSP**, **4 h Resonant**, and **3 flashes ridden** without leaving
> Stable.[/note]

**48113, Kerr T2: The Density Upgrade:**

> Kerr T2 is the same Kerr with more **density**. Right-click the KEE controller with the **Kerr Density Upgrade**. It
> is consumed and permanent, and it survives a wrench.
>
> The cage then needs the T2 reinforcement (hologram stack 2: **8,316 Bastion Plates**, more Harmonic Lining, Casimir
> Plates and glass). A T2 Kerr with the reinforcement torn down will **not form**.
>
> - Holes go up to **512 SM**.
> - The Compressor Module gains **Kerr Densification**, which needs a hole of **at least 128 SM**.
>
> [note]The upgrade costs 4,000,000 L Raw Star Matter and 32 Black Hole Seeds. Your T1 Eye pays for it.[/note]

**48116, Hyperdense Stellar Mixture:**

> Raw Hyperdense Stellar Plasma is too loose to seed a neutron star, so densify it. The Kerr Compressor Module runs
> **Kerr Densification**: RHSP + molten SpaceTime + bolts → **Hyperdense Stellar Mixture**. It runs only on a
> **Kerr T2 holding at least 128 SM**.
>
> HSM is used for:
> - seeding the Core Collapse (5,000 L);
> - feeding the neutron star;
> - the Galactic Upgrade Core (32,000 L);
> - Galactic Shell Plating;
> - the Neutron Star chart.

**48117, Eye T4: Core Collapse** (big; name style `{5}{l}`):

> Fast-forward a red giant into a **neutron star**, and stop it going supernova.
>
> {e}{l}BEFORE{r}
> - the T3 milestones;
> - the **Galactic Upgrade Core** in escrow;
> - the **spiral arms** (stack 4: 5,325 Galactic Shell Plating);
> - compression 4;
> - Arcturus lit and Stable;
> - **5,000 L HSM** buffered.
>
> Swap in **Star Chart: Neutron Star**.
>
> {e}{l}1 FAST-FORWARD{r} (about 16 min). Dose **Tachyon Rich Temporal Fluid** into the Field sockets, up to 100 L/s.
> The core climbs from 0.50 to **1.44 M☉**, which takes 94,000 L. The envelope swells as it goes, and your T3 rig must
> hold the field through it.
>
> {e}{l}2 BOUNCE{r} The HSM becomes the neutron seed, and the shock hits P and Θ for **+150**.
> - Every **1,000 L of molten SpaceTime** in the buffers at that moment cancels 1 point, up to 90,000 L.
> - Any gauge above +100 means **SUPERNOVA**: Arcturus, the Time fluid, the HSM and the SpaceTime are lost. The core
>   stays in escrow.
>
> {e}{l}3 SETTLE{r} Hold Stable for 60 seconds within 180. T4 is then yours.
>
> {e}{l}THE NEUTRON STAR{r}
> - It has no fusion, so it needs no H or He. **144 L molten SpaceTime** a cycle holds it.
> - Set the **HSM feed** in the GUI (0-1.5 L/s, default 0.694). **White Dwarf Matter = 3.6864 L per L of HSM fed.**
> - Every 0.1 L/s of feed pushes P, Θ and the spin **Ω** up. Ω runs away by itself, and **glitches** kick it +20
>   without warning.
> - New trims: Space (P −2), SpaceTime (P +2), Molten Purified Tengam 36 L (Ω −4).
> - If it breaches, it collapses into a black hole. Relight Arcturus and rerun the collapse; no core is needed the
>   second time.
>
> [note]The Deep Dark planet runs at T4.[/note]

**E15, White Dwarf Matter.** The task is kept, the description replaced, and the prerequisite changed to 48117:

> The neutron star's harvest: the first matter that does not come from a living star.
>
> It feeds Sirius B at UXV and Reinforced Components here.
>
> [note]The T4 → T5 milestones: **147,456 L** made from cycles that were never worse than Stable, **4 h Resonant**,
> and **one Deep Dark run** on this Eye.[/note]

**48118 ⧗, The Homogeny:**

> A mega mixer: five stirring bands around a plasma vortex. It turns Raw Star Matter into **Excited Stellar Plasma at
> 100 : 1**, a bulk ESPM route alongside Sirius A. It does **not** skip T2; the ladder is strict.

### 2.3 Tier 13 - UXV (new chapter of ours, line 47991)

- **Chapter:** "Tier 13 - UXV", icon `gregtech:gt.blockmachines:11233` (the pack's UXV hull).
- **Chapter description:** "The Eye's UXV half: Sirius B, the magnetar, then the Quasar Engine and the first
  galaxies. The Stargate waits in The Endgame, after MAX."

**Three bands.**

| Band | y range | Contents |
|---|---|---|
| The Eye, T5 to T8 | 48-372 | Main centre line y 180; gates on y 84; products and inputs on y 276 and 348 |
| UXV parts | 408-432 | The pack's UXV parts, and the new UXV parts |
| Moved in | 492-528 | The rest of the pack's UXV content |

| Id | Name | Pos | Prereqs | Tasks |
|---|---|---|---|---|
| pack `7wI7…` (E20) | **Tier 13 (UXV)** | 24,144 (72) | **48117** (was Magnetic Monopole Matter) | unchanged (R UXV hull) |
| pack `va9y…` (E21) | T5: Superb | 180,72 (24) | **48117, E20** (was Refined Components t1, Black Dwarf Matter) | unchanged |
| pack `HFpu…` (E22) | T6: Exotic | 324,72 (24) | unchanged (T5) | unchanged |
| pack `bKW2…` (E23) | T7: Perfect | 660,72 (24) | unchanged (T6) | unchanged |
| pack `lU1c…` (E24) | T8: Tipler | 852,72 (24) | unchanged (MHDCSM, Resplendent: QB-13) | unchanged |
| 48140 | **Eye T5: Sirius B** | 168,156 (48) | E20, E21, 48117 | R 1× `subspaceCoolingCore`, 1× `starChartSiriusB`; OR `bom:eye_arms2` |
| 48141 | **Eye T6: SGR 1806-20** | 312,156 (48) | 48140, E22, 48143, E25 | R 1× `magnetarSeedCore`, 1× `starChartMagnetar`; F 1,000 L `btsmagnetarmatter`; OR `bom:eye_rim` |
| 48144 ⧗ | **Quasar Engine** | 456,156 (48) | 48141, 48113 (Kerr T2) | real: R 1× `28120` |
| 48145 ⧗ | **Einstein-Rosen Conduit** | 564,168 (24) | 48144 | real: R 2× `28121`, 1× `28122`, 1× `28123` |
| 48146 ⧗ | **Eye T7: Triangulum** | 648,156 (48) | 48145, E23 | real: R 1× Intergalactic Upgrade Core; F Ergomatter |
| 48148 ⧗ | **Eye T8: Milky Way** | 828,144 (72) | 48146, E24 | real: R 1× Supermassive Seed Core |
| pack `bPYT…` (E25) | Black Dwarf Matter | 168,252 (48) | **48140** (was WDM, T4: Advanced, RSP) | unchanged (F 144 `molten.blackdwarfmatter`) |
| pack `teFE…` (E26) | Refined Components | 156,336 (24) | unchanged (BDM) | unchanged |
| 48143 | **Antipolarization Agent** | 312,252 (48) | E20 | F 1,000 L `btsantipolarizationagent`; OR 16× ore `dustTengamAttuned` |
| 48142 ⧗ | **The Homogeny T2** | 324,336 (24) | 48140 | real: R tbd |
| 48150 ⧗ | **Burst Thermaliser Crown** | 468,264 (24) | 48144 | real: R 1× `28124` |
| 48147 ⧗ | **Ergomatter** | 648,252 (48) | 48146 | real: F Ergomatter |
| 48149 ⧗ | **Supermassive Ergomatter** | 840,252 (48) | 48148 | real: F Supermassive Ergomatter |
| pack `M4A=` (E27) | You Made It! | 24,420 (24) | unchanged (Tier 13) | unchanged |
| pack `smCK…` (E28) | Your Lord and Saviour, UXV CoAL | 84,408 (48) | unchanged | unchanged |
| pack `DGfr…` (E29) | UXV Energy Hatch | 156,420 (24) | unchanged | unchanged |
| pack `Ni-Y…` (E30) | It Gets to a Point | 216,420 (24) | unchanged | unchanged |
| pack `3jUq…` (E31) | Strings Upon Strings Upon Strings Upon- | 276,420 (24) | unchanged | unchanged |
| pack `Uj0P…` (E32) | Humongous Hatch | 336,420 (24) | unchanged | unchanged |
| 48151 ⧗ | **Magnetar-Reinforced Borosilicate Glass** | 396,420 (24) | 48141 | real: R the glass |
| 48152 ⧗ | **UXV Superconductor** | 456,420 (24) | 48141 | real: R the wire |
| 48153 | **UXV Laser Hatches** | 516,420 (24) | E29 | R 1× TecTech UXV laser source + 1× target (ids via Q4) |
| pack `hUEb…` (E33) | Nano Forge Tier 4 | 24,504 (24) | unchanged | unchanged |
| pack `iLn_…` (E34) | T3 Exo-Foundry | 84,504 (24) | unchanged | unchanged |
| pack `UVG2…` (E35) | Assembling on Space Steroids | 144,504 (24) | unchanged | unchanged |
| pack `bHFE…` (E36) | Rule of Threes | 204,504 (24) | unchanged | unchanged |
| *(stage `eternityOnErgomatter`)* `wGq5` / `U7Ta` / `VYn4` / `k0ry` | Eternity / Magnetic Monopole Matter / Perfected Components / The SUCC | 300,492 (48) / 372,492 (48) / 444,504 / 504,504 | Eternity ← **48147** + Stellar Catalyst (t1); MMM ← Eternity + Heliofusion Exoticizer (no MHDCSM); the others as the pack | unchanged |

Eye T7 does not list Eye T6 directly. The Quasar does, and a direct line would run through the Quasar and Conduit
icons.

**E20, Tier 13 (UXV)** (`setDesc`; the first, second and last paragraphs are the pack's own):

> Welcome to the final tier of NH (for now).
>
> We hope you enjoy your stay, though be prepared for a lot of waiting.
>
> In Beyond the Stars this is the Eye's UXV half: **Sirius B (T5)** cools white dwarfs into black, **the magnetar
> (T6)** runs on Antipolarization Agent, and then the **Quasar Engine** and the first galaxies (T7, T8). The Stargate
> has moved to **The Endgame**, after MAX.
>
> [warn]**Universium now comes only from Eye T10 (MAX), which is not built yet.** The Eye's T5 and T6 cores do not
> need it: for now they are built from twice as many UMV parts, so Sirius B and the magnetar are craftable. The pack's
> own UXV parts (UXV components, the UXV CoAL, the UXV Field Generator) still need MHDCSM and Universium; nothing of
> Beyond the Stars before Eye T7 needs them. That stays so until the new UXV parts and circuits are designed and built.
> UXV circuits are craftable: the Polychrome and Dimensional Quantum Circuits need no Universium.[/warn]
>
> *(Rewritten 2026-10-13 after the merges: the T5/T6 cores take the B-72 UMV stopgap; the UXV parts and circuits
> design is a brainstorm, brief item 22a, so D-45's part override is not built. B-84: the Homogeny Tier II Core takes
> the same stopgap, so the warning no longer names it.)*
>
> [warn]This tier is far from complete. Expect changes.[/warn]

**48140, Eye T5: Sirius B:**

> A white dwarf, **cooled into a black dwarf**.
>
> {e}{l}UNLOCK{r} The T4 milestones, then:
> - put the **Subspace Cooling Core** in escrow;
> - build the **second arms** (stack 5: 5,065 Galactic Shell Plating);
> - bring compression to 5;
> - put **Star Chart: Sirius B** in the slot. The neutron star goes out, because there is one star per Eye.
>
> **First Cooling** is one batch with a mean yield of at least 0.9 and no fracture.
>
> {e}{l}BATCHES{r} Each batch takes **18,432 L WDM** and assembles a white dwarf at Θ +90. Over **2 h** (÷ the dial)
> the cooling setpoint ramps from **+90 to −90**, and you keep Θ on the curve: at T5 the gauge reads your error from
> the setpoint. The output is **4,608 L Black Dwarf Matter × the batch's mean yield**.
>
> {e}{l}SUBSPACE COOLING{r}
> - Dimensionally Shifted Superfluid (100 L) is the heat sink: Θ −1, P −0.5.
> - Raw Star Matter (1,000 L) reheats an overshoot: Θ +3.
> - Helium (1,000 L) is accretion pressure: P +2.
>
> Expect about **100,000 L DSSF a batch**. Decay heat pushes Θ up the whole way, and crystallisation drags P down.
>
> {e}{l}THE PLATEAU{r} While the setpoint crosses −10..+10 (about 800 s), latent heat adds **Θ +0.15 a second**. The
> event channel warns 15 s ahead.
>
> [warn]**Fracture** (Breach or Integrity 0): the batch ends and yields 25 % × the fraction done.[/warn]
>
> [note]One T4 Eye on the default feed makes exactly enough WDM for one T5 Eye.[/note]

**E25, Black Dwarf Matter** (`setDesc`, prerequisite 48140):

> Sirius B's harvest, made batch by batch.
>
> It goes into Refined Components, the Magnetar Seed Core and the merger that spawns the magnetar.
>
> [note]The T5 → T6 milestone: **6 batches** with mean yield ≥ 0.9 and no fracture (27,648 L).[/note]

**48143, Antipolarization Agent:**

> Tengam with its electrons swapped for positrons is literally *antipolarised*. Field-cooling it locks the reversed
> domains, and its counter-currents untwist a magnetar's field.
>
> {e}{l}CHAIN{r}
> 1. Raw Tengam → Electromagnetic Separator → Purified Tengam → Polarizer → **Attuned Tengam dust**.
> 2. **Large Chemical Reactor** (UXV, 20 s): 16 Attuned Tengam dust + 100 L Semi-Stable Antimatter + 144 L molten
>    SpaceTime → **1,000 L Positronic Tengam Slurry**.
> 3. **Vacuum Freezer** (UXV, 10 s): slurry + 2,000 L Super Coolant → **1,000 L Antipolarization Agent**.
>
> {e}{l}USE{r}
> - the magnetar's **constant feed**, 3,000 L a cycle;
> - the merger charge, 100,000 L;
> - the Magnetar Seed Core, 64,000 L;
> - the flare shield trim.
>
> [note]The Eye returns **12 Purified Tengam dust per 1,000 L** of Agent consumed, so antimatter is the real
> cost.[/note]

**48141, Eye T6: SGR 1806-20:**

> {e}{l}UNLOCK{r} The T5 milestones, then:
> - put the **Magnetar Seed Core** in escrow;
> - build the **galactic rim** (stack 6: 7,286 Galactic Shell Plating);
> - bring compression to 6, with **no star lit**;
> - put **Star Chart: SGR 1806-20** in the slot;
> - buffer **18,432 L WDM + 4,608 L BDM + 100,000 L Agent**.
>
> {e}{l}MAGNETAR MERGER{r} White and black dwarf matter combine into a magnetar. The dynamo winds the polarity **Π**
> up by itself.
> 1. Hold Π inside **+60..+90 for 120 s** to imprint the field. You have 600 s.
> 2. Then bring Π back inside **±40 within 60 s**.
>
> A failure loses the charge; the core is kept.
>
> {e}{l}RUN{r} **3,000 L Antipolarization Agent** a cycle is its constant feed, and the same Agent charges the
> **flare shield A**. It makes **15 L Magnetar Matter** a cycle.
> - Π runs away by itself.
> - **Giant flares** come every 60-120 s, announced **15 s ahead with their sign**.
> - Pre-charge the shield on the warning (100 L of Agent = A +4, at most 5 a second).
>
> Lose the magnetar and a re-merge needs the charge again, but not the core.
>
> [note]Magnetar Matter is Eye T7's upkeep and the base of the T9 pellets (planned).[/note]

**Placeholders.** The text is from `quasar-p4.md` and the D-42/D-43 design, written in present tense. The standard
planned note follows each one.

- **48144 Quasar Engine:**

  > Its own machine with its own black hole, seeded once from a Kerr T2 (a **Quasar Seed** made on the Compressor
  > Module with a hole of at least 256 SM). Feed it gas to build an **accretion disc**. Hold its Eddington balance E,
  > and let its flux Φ charge and **erupt**. The disc spins and grows the hole and drives **two jets**. Each jet goes
  > to the Eye (stabilisation) or to a crown (power).

- **48150 Burst Thermaliser Crown:**

  > Sits on a jet. 1 burst unit a tick → 10¹³ EU/t to wireless (1 %). Mk I / II / III caps are 32 / 512 / 8,192
  > Bu/t.

- **48145 Einstein-Rosen Conduit:**

  > Carries bursts with no loss. A **Wormhole Mouth** pair (linked by data stick, any distance), **Mirrors** to turn
  > them, and **Junctions** to split or combine them. Upkeep is molten SpaceTime, or Ergomatter after T7. **GRB
  > Receptor Ports** on the Eye's rail take the stream.

- **48146 Eye T7: Triangulum:**

  > The first galaxy, and the first tier the quasar holds together. The **Magnetar Collapse** overfeeds the magnetar
  > past its mass limit, and bursts cancel the shock. The new verb is **aim**: split the bursts between the high and
  > low banks of the tilted rail. Upkeep is 7.5 L Magnetar Matter a cycle plus 1.5 Bu/t; output is 600 L Ergomatter a
  > cycle.

- **48147 Ergomatter:**

  > T7's harvest: the Conduit's fuel after T7, Eye T8's feed and the Eternity loop's new input.

- **48148 Eye T8: Milky Way:**

  > Sgr A*. The **Galactic Merger** brings three pericentre passes while the burst demand climbs to 6 Bu/t. Aim a
  > third stream into the shard halo and catch tidal disruption events. It is fed 0-10 L/s Ergomatter and makes
  > Supermassive Ergomatter. **Required**: the ladder is strict.

- **48149 Supermassive Ergomatter:**

  > Galaxy residue, the T9 pellets' input.

- **48142 The Homogeny T2:**

  > Four times the T1 mixer, and a new mix: Degenerate Stellar Mixture (white + black dwarf matter).

- **48151 Magnetar-Reinforced Borosilicate Glass:**

  > The UXV glass tier, from Magnetar Matter.

- **48152 UXV Superconductor:**

  > The pack's superconductors stop at UMV; this one is drawn from Magnetar Matter.

**48153, UXV Laser Hatches** (real):

> The UXV laser source and target hatches already exist. Put them on anything that drinks more than an energy hatch
> can give.

### 2.4 Tier 14 - MAX (new chapter of ours, line 47992)

- **Chapter:** "Tier 14 - MAX".
- **Chapter description:** "The last voltage tier. MAX parts are built from Eye T9's White Hole Matter; Eye T10 makes
  Universium, which opens The Endgame."

| Id | Name | Pos | Prereqs | Tasks |
|---|---|---|---|---|
| 48180 | **Tier 14 (MAX)** | 24,144 (72) | 48148 | C (chapter intro) |
| pack `g5Cf…` (E37) | T9: Gallifreyan | 180,72 (24) | unchanged (T8: Tipler) | unchanged |
| 48181 ⧗ | **T9 Pellets** | 180,264 (24) | 48180, 48149 | real: R pellets |
| 48182 ⧗ | **Eye T9: White Hole** | 168,156 (48) | 48180, 48181, E37 | real: R core + chart |
| 48183 ⧗ | **White Hole Matter** | 264,264 (24) | 48182 | real: F |
| 48184 ⧗ | **The Homogeny T3** | 324,252 (48) | 48182 | real: R |
| 48185 ⧗ | **Reality Diffraction** | 324,156 (48) | 48182, 48184, 48183 | real: R |
| 48186 ⧗ | **Reality Refab** | 468,156 (48) | 48185 | real: R |
| 48187 ⧗ | **The Tenth Compression Tier** | 624,72 (24) | E37 | real: R the new generator |
| 48188 ⧗ | **Eye T10: The Universe** | 600,144 (72) | 48186, 48187 | real: R core + chart; F Universium |
| 48189 ⧗ | **MAX Parts** | 24,396 (48) | 48183 | real: C (hub) |
| pack `2rrP…` (E38) | MAX Circuits | 108,408 (24) | unchanged | unchanged |
| pack `itcS…` (E39) | A Minor Roadblock | 168,408 (24) | unchanged | unchanged |
| 48190 ⧗ | **MAX Components and Casings** | 36,480 (24) | 48189 | real: R the nine components, CoAL casing, machine casing |
| 48191 ⧗ | **MAX Power** | 96,480 (24) | 48189 | real: R energy and dynamo hatch, laser source and target, wireless hatch |
| 48192 ⧗ | **MAX Materials** | 156,480 (24) | 48189 | real: R superconductor, White Hole heating coil, White Hole glass, White Hole frame |
| 48193 ⧗ | **MAX Battery** | 216,480 (24) | 48189 | real: R |

**Texts.**

- **48180:**

  > The last voltage tier. MAX parts are built from **White Hole Matter**, so they come before T10. **Eye T10** makes
  > Universium, the only source in the game, and Universium opens **The Endgame**.

- **48181:**

  > Gated pellets made from T6, T7 and T8 matter, fired for a higher-GeV beam that powers and stabilises T9.

- **48182:**

  > The largest galaxies; output White Hole Matter.

- **48184:**

  > Sixteen times the T1 mixer; Entanglement Medium (White Hole Matter + Supermassive Ergomatter).

- **48185:**

  > Entangles its inputs across six regions outside reality.

- **48186:**

  > Grabs the entangled inputs and reassembles them. With Diffraction, this is the jump to T10.

- **48187:**

  > Gallifreyan is TecTech's last generator. Eye T10 needs a tenth tier.

- **48188:**

  > A jump as big as the T4 Nano Forge. Output: **Universium**, and only here.

- **48189 to 48193:** each lists its parts and "built from White Hole Matter (D-47)".

**E37, T9: Gallifreyan** gets T2's text pattern: "gates Eye **T9**; T10 needs a new tier".

### 2.5 The Endgame (new chapter of ours, line 47993)

- **Chapter:** "The Endgame".
- **Chapter description:** "After MAX: what Universium is for, and the Stargate."

**Two blocks.**

| Block | x range | Contents |
|---|---|---|
| Universium and what it feeds | 0-400 | |
| The Stargate | 432 and up | The pack's crafted Stargate chain in its own order. Main centre line y 180, Frame and Few of These above, Maybe Change and Command below, the three computation quests on y 372 |

| Id | Name | Pos | Prereqs | Tasks |
|---|---|---|---|---|
| 48220 | **The Endgame** | 24,144 (72) | 48188 | C (intro) |
| pack `21tD…` (E40) | Finally, Universium! | 168,156 (48) | **48220** (was BDM, T7: Perfect) | unchanged |
| pack `52Ye…` (E41) | Magnetohydrodynamically Constrained Star Matter | 288,156 (48) | unchanged | unchanged |
| pack `LxgR…` (E42) | Resplendent Components | 300,264 (24) | unchanged | unchanged |
| pack `IGVr…` (E47) | Fabricating Solar Systems... in Parallel | 180,264 (24) | **E40** (was T9: Gallifreyan, Tier 13, Assembly To The MAX+) | unchanged |
| *(stage: until `eternityOnErgomatter`)* `wGq5…` (E43) | Eternity | 288,48 (48) | unchanged | unchanged |
| *(same)* `U7Ta…` (E44) | Magnetic Monopole Matter | 168,48 (48) | unchanged | unchanged |
| *(same)* `VYn4…` (E45) | Perfected Components | 108,60 (24) | unchanged | unchanged |
| *(same)* `k0ry…` (E46) | The SUCC | 372,60 (24) | unchanged | unchanged |
| pack `KDA=` (E48) | The Door to Heaven? | 432,156 (48) | pack + **MHDCSM `52Ye…`, Resplendent `LxgR…`** | unchanged |
| pack `fTEQ…` (E49) | Stargate Frame Part | 540,60 (48) | unchanged | unchanged |
| pack `KIg=` (E50) | You'll Only Need a Few of These | 660,60 (48) | unchanged | unchanged |
| pack `KEA=` (E51) | Stargate Chevron | 540,156 (48) | unchanged | unchanged |
| pack `W5cc…` (E59) | Maybe Change Your Mind | 552,264 (24) | unchanged | unchanged |
| pack `KDQ=` (E52) | Stargate Ring Block | 660,156 (48) | unchanged | unchanged |
| pack `KDw=` (E53) | Stargate Chevron Block | 780,156 (48) | unchanged | unchanged |
| pack `KJQ=` (E54) | Stargate Command | 780,252 (48) | unchanged | unchanged |
| pack `KDg=` (E55) | Stargate Base | 900,156 (48) | unchanged | unchanged |
| pack `KJg=` (E56) | **What, How?** | 1008,144 (**72**) | unchanged | unchanged |
| pack `OCv5…` (E57) | The Factory Must Grow! | 1128,120 (24) | unchanged | unchanged |
| pack `KNM9…` (E58) | See You Next Time! | 1128,216 (24) | unchanged | unchanged |
| pack `ZUdn…` (E60) | Universal Collapser | 660,360 (24) | unchanged | unchanged |
| pack `iu0r…` (E61) | UNLIMITED COMPUTATION | 720,360 (24) | unchanged | unchanged |
| pack `2QVk…` (E62) | A Universe In Your Drives | 780,360 (24) | unchanged | unchanged |

(`KDA=` etc. abbreviate the pack's `AAAAAAAAAAAAAAAAAAAK…==` ids; §3 has them in full.)

**48220, The Endgame:**

> **Universium is not the end of the game.** It is where the pack's MHDCSM line begins, which coats Eternity parts,
> builds Resplendent Components and stands between you and the Stargate. The crafted Stargate has been pushed back to
> here, past MAX. The pack's own Stargate quests follow in their own order; *What, How?* is the end of the book.

**Text edits in this chapter.**

- **E40, Finally, Universium!:**
  - `replaceText` "This is your third and final material from the EOH." → "In Beyond the Stars, only Eye T10 (The
    Universe) makes it."
  - `replaceText` "The quest for it is located in the Endgame Goals tab." → "Its quest is in this tab."
- **E41, MHDCSM:** `appendText`: "[note]Beyond the Stars plans to take MHDCSM out of UXV recipes (Magnetar Matter and
  Ergomatter instead). Until then, the pack's recipes stand.[/note]"
- **E47, Fabricating Solar Systems... in Parallel:** `appendText`: "[warn]The Beyond the Stars Eye does not take
  Astral Array Fabricators yet. A higher Eye already runs lower stars at ×2 parallel per tier.[/warn]"
- **E48, The Door to Heaven?:** `appendText`: "[note]It now also needs MHDCSM and Resplendent Components, so the
  Stargate cannot start before Eye T10.[/note]"

### 2.6 Beyond the Stars (plant chapter, line 47990)

**Unchanged.** Its 35 quests (`48000-48034`, with `48020` grafted into How to Generate Power) keep their generator
(`gen-questbook.ps1`, BOM-welded, `questbook/README.md`). It joins the new mechanism in only two ways:

- `BTSQuestIds.java` (§4.2) lists its lows too, so `status` counts one declared list for everything we own.
- `/btsquests uninstall` and `install` treat it like the other chapters.

It is not moved: it is an IV-era campaign, unrelated to the ladder.

---

## 3. Pack edits

**Each edit has:**

- the pack quest id (base64, as in `DefaultQuests` file names);
- the expected name (§ codes stripped) and the expected source chapter;
- its ops (§4.4);
- the progress policy (§4.6): `keep` keeps completion and task progress, `completionOnly` keeps completion and drops
  task progress because the task list changed.

**Default prereq rule.** Unless stated otherwise:

- a moved quest keeps the pack's prerequisites;
- a quest moved out of Endgame Goals or UMV leaves **only that** chapter's entry. Its other chapter memberships stay,
  for example Nano Forge Tier 4 and Universal Collapser also sit in Multiblock Goals.

### 3.1 Tier 11 - UIV

| E | Pack id | Name (expected) | Ops | Progress |
|---|---|---|---|---|
| E01 | `6OibD9bRQwq_hBPWz-y9xQ==` | Pseudostable Black Hole Containment Field | `setName` "Kerr Ergosphere Engine" (`{9}{l}{n}`); `setIcon` `28114`; `setDesc` §2.1; `setTasks` §2.1; `setRewards` §2.1; position and prerequisites unchanged | completionOnly |
| E02 | `-HsIRcNoQYS35kDJwhdUpQ==` | Bending Space and Time | `replacePrereq` BHC → **48100** (it needs the neutronium-compressor recipe the module now runs) | keep |
| E03 | `yySGZ5RzSZm38ElTS1kL7g==` | Maybe I Do Need These | `replacePrereq` BHC → **48100**; `replaceText` "can only be crafted in the BHC." → "can only be crafted in the Kerr Compressor Module." | keep |
| E04 | `4UNfsN-3TlKiC2JkOUP4Hg==` | A Fork in The Road | `replaceText` "With the creation of the Black Hole Compressor, three different paths have opened up." → "With a black hole held in your Kerr Ergosphere Engine, three different paths have opened up." | keep |
| E05 | `UIg8uIZxS8K_JVlg7eZR9g==` | T2 Exo-Foundry | `replaceText` "unlocked the BHC" → "opened the Kerr's hole" | keep |
| E06 | `u-1Y-eQ-TLyw-WGi5EEnVA==` | Tier 11 (UIV) | `replaceText` "and the Black Hole Compressor that you will use" → "and the Kerr Ergosphere Engine that you will use" | keep |

Heliocast Reinforcement, Hypercooler and T2 Exo-Foundry keep the BHC id as a prerequisite, which is now the KEE quest.

### 3.2 Tier 12 - UMV

| E | Pack id | Name (expected) | Ops | Progress |
|---|---|---|---|---|
| E10 | `kazCPEWoSmiZhR4bkHlEBQ==` | An Eye at the End of Time | `setIcon` `28115`; `setDesc` §2.2; `setTasks` §2.2; `setRewards` 2× Ignition Charge; `addPrereq` `Qjg1J4kuS-6937dCEUqHUw==` | completionOnly |
| E11 | `uXczmEsfT2uRCSnx8Vm4RQ==` | Raw Stellar Plasma | `setDesc` §2.2; `move` UMV 648,252 (48) | keep |
| E12 | `cO67NYK2QsyDlcvFGkk43A==` | T2: Primitive | `setDesc` §2.2; `move` UMV 792,72 (24) | keep |
| E13 | `Hgj4aBbmQUCkp325rL-McQ==` | T3: Stable | `setDesc`; `move` UMV 912,72 | keep |
| E14 | `-fvTafFJRo67bYWh46USSw==` | T4: Advanced | `setDesc`; `move` UMV 1068,72 | keep |
| E15 | `DVLahVNUQ-CvjYwmvkUn-Q==` | White Dwarf Matter | `replacePrereq` EOH → **48117**; `setDesc` §2.2; `move` UMV 1164,156 (48) | keep |
| E16 | `RTzMnAU8SvC0mzrDPHDbPg==` | Reinforced Components | `move` UMV 1176,264 | keep |
| E17 | `fj2VzJf2Rgev7uY-h3CkJQ==` | Stellar Catalyst | `move` UMV 720,312 | keep |
| E18 | `cEX6SRxLTnSHOyvKPkBybw==` | A Solder Singularity | `move` UMV 660,72 | keep |
| E19 | `v5GltE2BTW-Me_rEqy5uFg==` | Are you Ready? | `replaceText` "The Eye of Harmony will be consuming most of your power before it starts generating more than it uses." → "The Eye of Harmony is a pure power sink: every cycle is paid from your wireless network, and none of it comes back." | keep |

### 3.3 To Tier 13 - UXV (ours)

| E | Pack id | Name (expected) | From | Ops | Progress |
|---|---|---|---|---|---|
| E20 | `7wI7VuLKRe6lqAJpuYKKJg==` | Tier 13 (UXV) | Endgame Goals | `move` UXV 24,144 (72); `replacePrereq` Magnetic Monopole Matter `U7Ta5d7PSii76sbIPbltlw==` → **48117**; `setDesc` §2.3 | keep |
| E21 | `va9yzhTSSUWxgareXVIaXQ==` | T5: Superb | UMV | `move` UXV 180,72; `setPrereqs` [48117, E20]; `setDesc` (gates Eye T5; its RC-2 Black Dwarf chamber is replaced by SL-P33, so no BDM loop) | keep |
| E22 | `HFpuUdRKS--dsQzGEvjW2w==` | T6: Exotic | UMV | `move` UXV 324,72; `setDesc` (gates Eye T6) | keep |
| E23 | `bKW2YJfES6yvbes8JQRb0g==` | T7: Perfect | UMV | `move` UXV 660,72; `setDesc` (gates Eye T7, planned) | keep |
| E24 | `lU1caedNR6G6fdJqbivacw==` | T8: Tipler | UMV | `move` UXV 852,72; `setDesc` (gates Eye T8, planned; its recipe still needs MHDCSM-coated parts, QB-13) | keep |
| E25 | `bPYTUdlkSCiRi0eDHHw7tw==` | Black Dwarf Matter | UMV | `move` UXV 168,252 (48); `setPrereqs` [48140]; `setDesc` §2.3 | keep |
| E26 | `teFEMExsSEWHYV79M8gpHw==` | Refined Components | UMV | `move` UXV 156,336 | keep |
| E27 | `AAAAAAAAAAAAAAAAAAAM4A==` | You Made It! | Endgame Goals | `move` UXV 24,420 | keep |
| E28 | `smCKxKkMSeyIAUeFInS4hQ==` | Your Lord and Saviour, UXV CoAL | Endgame Goals | `move` UXV 84,408 (48); `replaceText` "Every L of Universium matters." → "Every L of Universium matters, and Universium now comes only from Eye T10." | keep |
| E29 | `DGfri1njSxOeS79XXo4NIg==` | UXV Energy Hatch | Endgame Goals | `move` UXV 156,420 | keep |
| E30 | `Ni-YWTVjSz6Bk_MeC0hIiQ==` | It Gets to a Point | Endgame Goals | `move` UXV 216,420 | keep |
| E31 | `3jUq4tkmRty9x5vvnqtD9w==` | Strings Upon Strings Upon Strings Upon- | Endgame Goals | `move` UXV 276,420 | keep |
| E32 | `Uj0PvsNCQw-aWaEOvlFpow==` | Humongous Hatch | Endgame Goals | `move` UXV 336,420; `replaceText` "the egregious EoH hatch input limitations" → "the egregious Eye hatch input limits" | keep |
| E33 | `hUEbNbngSA6JxT_Pjq8DNw==` | Nano Forge Tier 4 | Endgame Goals | `move` UXV 24,504 | keep |
| E34 | `iLn_-6-LSk-wEOfi9muW3w==` | T3 Exo-Foundry | Endgame Goals | `move` UXV 84,504 | keep |
| E35 | `UVG21mYSQJex90gQWdkpIw==` | Assembling on Space Steroids | Endgame Goals | `move` UXV 144,504 | keep |
| E36 | `bHFEDKAiTwGxaiyZEsmdjg==` | Rule of Threes | Endgame Goals | `move` UXV 204,504 | keep |

### 3.4 To Tier 14 - MAX (ours)

| E | Pack id | Name (expected) | From | Ops | Progress |
|---|---|---|---|---|---|
| E37 | `g5CfYPLATaiRyVInsqn9_w==` | T9: Gallifreyan | UMV | `move` MAX 180,72; `setDesc` (gates Eye T9) | keep |
| E38 | `2rrP1wt_Qt2LlOqDppdjLQ==` | MAX Circuits | Endgame Goals | `move` MAX 108,408 | keep |
| E39 | `itcSYyc3Tdq9Q1qrP5RoEg==` | A Minor Roadblock | Endgame Goals | `move` MAX 168,408 | keep |

### 3.5 To The Endgame (ours)

| E | Pack id | Name (expected) | From | Ops | Progress |
|---|---|---|---|---|---|
| E40 | `21tDEYptRuOgSJym30Watg==` | Finally, Universium! | UMV | `move` END 168,156 (48); `setPrereqs` [48220]; 2× `replaceText` (§2.5) | keep |
| E41 | `52Yelo6KSLGumzXo-_F5DQ==` | Magnetohydrodynamically Constrained Star Matter | UMV | `move` END 288,156 (48); `appendText` (§2.5) | keep |
| E42 | `LxgRRQ25SE2IQOCMxZa8ug==` | Resplendent Components | UMV | `move` END 300,264 | keep |
| E43 | `wGq5FvuuROyyMPbT_SkMYg==` | Eternity | UMV | **unless `eternityOnErgomatter`**: `move` END 288,48 (48). **needs it**: `move` UXV 300,492; `setPrereqs` [48147, `fj2V…` (type 1)] | keep |
| E44 | `U7Ta5d7PSii76sbIPbltlw==` | Magnetic Monopole Matter | UMV | unless: `move` END 168,48 (48). needs: `move` UXV 372,492; `removePrereq` MHDCSM | keep |
| E45 | `VYn4yi1TSiqJRhtK8YOjjQ==` | Perfected Components | UMV | unless: `move` END 108,60. needs: `move` UXV 444,504 | keep |
| E46 | `k0ryGOkkSBOgYUcVY-J4rw==` | The SUCC | UMV | unless: `move` END 372,60. needs: `move` UXV 504,504 | keep |
| E47 | `IGVryoMbRu28q08v4uwSuw==` | Fabricating Solar Systems... in Parallel | Endgame Goals | `move` END 180,264; `setPrereqs` [E40]; `appendText` (§2.5) | keep |
| E48 | `AAAAAAAAAAAAAAAAAAAKDA==` | The Door to Heaven? | Endgame Goals | `move` END 432,156; `addPrereq` E41, E42; `appendText` (§2.5) | keep |
| E49 | `fTEQD_w8TTerm_ETSVcBmA==` | Stargate Frame Part | Endgame Goals | `move` END 540,60 | keep |
| E50 | `AAAAAAAAAAAAAAAAAAAKIg==` | You'll Only Need a Few of These | Endgame Goals | `move` END 660,60 | keep |
| E51 | `AAAAAAAAAAAAAAAAAAAKEA==` | Stargate Chevron | Endgame Goals | `move` END 540,156 | keep |
| E52 | `AAAAAAAAAAAAAAAAAAAKDQ==` | Stargate Ring Block | Endgame Goals | `move` END 660,156 | keep |
| E53 | `AAAAAAAAAAAAAAAAAAAKDw==` | Stargate Chevron Block | Endgame Goals | `move` END 780,156 | keep |
| E54 | `AAAAAAAAAAAAAAAAAAAKJQ==` | Stargate Command | Endgame Goals | `move` END 780,252 | keep |
| E55 | `AAAAAAAAAAAAAAAAAAAKDg==` | Stargate Base | Endgame Goals | `move` END 900,156 | keep |
| E56 | `AAAAAAAAAAAAAAAAAAAKJg==` | What, How? | Endgame Goals | `move` END 1008,144 (72) | keep |
| E57 | `OCv5OWo6Q9KDfqe1w0ET9w==` | The Factory Must Grow! | Endgame Goals | `move` END 1128,120 | keep |
| E58 | `KNM9ICNAQIa8NaFoXwCC9g==` | See You Next Time! | Endgame Goals | `move` END 1128,216 | keep |
| E59 | `W5cccuPzQGqJzhYW_kBMcQ==` | Maybe Change Your Mind | Endgame Goals | `move` END 552,264 | keep |
| E60 | `ZUdnzN4GSxmaxY7xZIgNTg==` | Universal Collapser | Endgame Goals | `move` END 660,360 (keeps its Multiblock Goals entry) | keep |
| E61 | `iu0rzCLcR_K7Oc2EDtQqgg==` | UNLIMITED COMPUTATION | Endgame Goals | `move` END 720,360 | keep |
| E62 | `2QVk9gqBSWKWZQ2T0rXh8A==` | A Universe In Your Drives | Endgame Goals | `move` END 780,360 | keep |

### 3.6 Endgame Goals (stays): the trophies

TecTech's Eye controller recipe is removed (`replaceTecTechEyeOfHarmony`), so two consume-tasks would be uncompletable.

| E | Pack id | Name (expected) | Ops | Progress |
|---|---|---|---|---|
| E63 | `hNdNrT_aSKaYi_TnT7cowA==` | Simulating a Solar System | `swapItem` `gregtech:gt.blockmachines:15410` → `:28115` (12×) | completionOnly |
| E64 | `ztV-j35XSXaQ0OQ1TSyKew==` | An Offering To The Gods | `swapItem` `15410` → `28115` (8×) | completionOnly |

After the moves, Endgame Goals holds these two trophies. Its order position and description are not touched.

### 3.7 Config gating of edits

| Edits | Also need |
|---|---|
| E01-E06, and the `bom` / text facts that name the KEE | `removeBlackHoleCompressorRecipe = true` (with it false, the BHC is still the pack's compressor) |
| E10-E19, E20-E25, E37, E40, E63, E64 | `replaceTecTechEyeOfHarmony = true` |
| E26-E36, E38, E39, E41-E62 | nothing beyond `questbookPackEdits` (layout moves) |

With either removal flag off, its edits are skipped, the fallback quest (48103 or 48109) is emitted, and our quests
that named the edited quest as a prerequisite keep naming the pack id. The pack quest still exists, unedited.

---

## 4. Mechanism

### 4.1 The single source: `questbook/singularity.json`

One hand-edited JSON file. It is ASCII-only, with § as `{x}` tokens, as the existing generator does.

```jsonc
{
  "format": 1,
  "idHigh": 190,
  "packVersion": 2171,                     // QuestSettings.json pack_version the edits were written against (RC-2)
  "features": {                            // what the jar being built contains; flipped by the unit that ships it
    "kerrRedesign": true, "eyeD49": true, "agentChain": true,
    "homogeny": false, "quasar": false, "conduit": false, "crown": false, "eyeT7T8": false,
    "eyeT9T10": false, "realityRefab": false, "maxParts": false, "uxvLaserIds": true,
    "eternityOnErgomatter": false, "uxvSwap": false, "astralArrayOnEye": false, "bec": false
  },
  "packChapters": { "uiv": "ytf2QCzUSk6ihcYeRbwblw==", "umv": "l37V2WvbSHGZQ5oMgAfBuw==",
                    "egoals": "AAAAAAAAAAAAAAAAAAAAHQ==" },
  "chapters": [
    { "key": "uxv", "low": 47991, "name": "Tier 13 - UXV", "icon": "gregtech:gt.blockmachines:11233",
      "desc": "...", "orderAfter": "l37V2WvbSHGZQ5oMgAfBuw==" },
    { "key": "max", "low": 47992, ... }, { "key": "end", "low": 47993, ... }
  ],
  "quests": [
    { "low": 48100, "chapter": "uiv", "x": 516, "y": 348, "size": 24,
      "name": "{b}{l}Kerr Compressor Module", "icon": "gregtech:gt.blockmachines:28117",
      "prereqs": ["pack:6OibD9bRQwq_hBPWz-y9xQ=="],
      "tasks": [ { "type": "retrieval", "items": ["gregtech:gt.blockmachines:28117"] },
                 { "type": "optional_retrieval", "bom": "kerr_compressor" } ],
      "rewards": [], "desc": "...", "needs": ["kerrRedesign"], "main": false },
    { "low": 48144, "chapter": "uxv", ..., "needs": ["quasar"],          // placeholder until "quasar" is true
      "tasks": [ { "type": "retrieval", "items": ["gregtech:gt.blockmachines:28120"] } ] },
    { "low": 48103, ..., "onlyWithout": "packEdit:E01" }                   // the fallback quest
  ],
  "packEdits": [
    { "id": "E01", "quest": "6OibD9bRQwq_hBPWz-y9xQ==", "expectName": "Pseudostable Black Hole Containment Field",
      "expectChapter": "uiv", "config": ["removeBlackHoleCompressorRecipe"], "progress": "completionOnly",
      "ops": [ { "op": "setName", "name": "{9}{l}{n}Kerr Ergosphere Engine" }, { "op": "setIcon", "item": "..." },
               { "op": "setDesc", "desc": "..." }, { "op": "setTasks", "tasks": [ ... ] },
               { "op": "setRewards", "rewards": [ ... ] } ] },
    { "id": "E43", "quest": "wGq5FvuuROyyMPbT_SkMYg==", "expectName": "Eternity", "expectChapter": "umv",
      "variants": [ { "unless": ["eternityOnErgomatter"], "ops": [ { "op": "move", "to": "end", "x": 288, "y": 48, "size": 48 } ] },
                    { "needs":  ["eternityOnErgomatter"], "ops": [ { "op": "move", "to": "uxv", ... },
                                                                    { "op": "setPrereqs", "prereqs": [48147, "pack:fj2V...:1"] } ] } ] }
  ]
}
```

**Item references.**

- `mod:name:meta` is a plain item.
- `ore:boltNeutronium` writes BQ's `OreDict` field plus a representative id from `questbook/registry.json`.
- `fluid:name` with an `amount` is a fluid.
- `bom:<piece>` is a manifest-derived optional retrieval.

**Prerequisites.** A number is ours. `pack:<b64>` is the pack's; a `:1` suffix is BQ's prerequisite type 1, as the
pack writes it.

### 4.2 The generator: `questbook/gen-singularity.ps1`

PowerShell 5.1 and ASCII-only, like the rest of `questbook/`. The shared helpers (`Fmt`, base64 ids, `AssertStacks`,
`AssertDesc`) move out of `gen-questbook.ps1` into `questbook/qb-common.ps1`, which both generators dot-source.
`gen-questbook.ps1` calls `gen-singularity.ps1` as its last step, so the one build command stays `gen-bom.ps1`
(README, "There is ONE build command").

**Inputs:**

- `singularity.json`;
- `src/main/resources/assets/beyondthestars/structures/manifest.json`, for the `bom:` counts and the band totals the
  texts quote (via `{bom:eye_beam:R}` tokens in descriptions, so no structure number is typed by hand);
- `questbook/registry.json`, the verified id table filled from `/btsquests dumpids` (§6, unit Q4);
- `questbook/id-ledger.txt`.

**Outputs:**

1. **`questbook/DefaultQuests/`**, for new worlds and for `/bq_admin default load`:
   - our three chapter folders (`QuestLines/Tier13UXV-AAAAAAAAAL4AAAAAAAC7dw==/…`, `Quests/…`);
   - our UIV and UMV quests as **added** entry and quest files inside the pack folders `Tier11UIV-ytf2…` and
     `Tier12UMV-l37V…`. These are file-merged, never cleared. The stem is pinned per id, as `UnitOne` is.
   - `OrderLines.txt`.

   Only quests whose `needs` are built emit real tasks; the rest emit as placeholders. **No pack file is written.**
2. **`src/main/resources/assets/beyondthestars/questbook/overlay.json`**: the bundle the jar carries. It holds:
   - every chapter and quest of ours (the plant chapter included, read back from `DefaultQuests/BeyondTheStars-*`) in
     BQ's typed-key JSON;
   - the pack edits with variants resolved against `features`, and each op's NBT payload prebuilt;
   - the placeholder flag per quest;
   - `bundleVersion` = SHA-1 of the bundle.
3. **`src/main/java/com/nateyburr/beyondthestars/BTSQuestIds.java`** (generated, like `BTSBom.java`): every line and
   quest low of ours, per chapter, plus the edit ids. `BQBridge.CHAPTER_QUEST_LOWS` and the hand-kept array check go
   away; `status` counts these lists.

**Assertions.** It throws on any of these, naming the offender:

- an id outside its chapter's range, a duplicate id, or an id that differs from the ledger (append-only);
- an item, fluid or ore missing from `registry.json`, or a stack count below 1;
- a placeholder without the standard planned block, or a real quest with no task;
- **tidiness:**
  - two quests in one chapter whose rectangles overlap;
  - a prerequisite line, centre to centre, that crosses a third quest's rectangle in the same chapter (the pack's own
    left halves included: the check runs on the post-edit layout);
  - a position off the 12-unit grid;
- a pack edit whose `expectName` / `expectChapter` does not match the RC-2 `DefaultQuests` the generator is pointed
  at (`-PackDir`, default the RC-2 instance). This is the offline drift check (§4.7);
- non-ASCII in the source, as today.

### 4.3 Placeholders

A quest with `needs` that are not all built is emitted with:

- one checkbox task;
- `countAsQuest 0`;
- the standard planned block appended to its description;
- the bundle flag `placeholder: true`.

When the feature flips (the unit that ships the Quasar sets `"quasar": true`), the next build emits the real tasks
under the **same id**. On install, the world's state file (§4.5) shows it was a placeholder last time, so its progress
is reset (§4.6): a ticked box never stands in for building the real machine.

### 4.4 Pack-edit ops

The ops act on the pack quest's NBT, as `QuestInstance.writeToNBT` gives it, and on chapter entries:

| Op | Effect |
|---|---|
| `setName`, `setDesc`, `setIcon` | Replace `properties.betterquesting.{name, desc, icon}`. |
| `replaceText {find, with}` | Exact substring replacement in `desc`. If `find` is missing, **this op only** is skipped and reported. |
| `appendText` | `desc += "\n\n" + text`. |
| `setTasks`, `setRewards` | Replace the lists (BQ typed keys, built by the generator). |
| `swapItem {from, to}` | Replace an item id/meta in every task's item lists, keeping counts and flags. |
| `setPrereqs`, `addPrereq`, `removePrereq`, `replacePrereq` | Edit `preRequisites` (UUID + type). A missing target is skipped and reported. |
| `move {to, x, y, size}` | Remove the quest's entry from the expected source chapter and put a `QuestLineEntry(x, y, size)` in the target chapter. A target equal to the source re-positions it. Other chapter memberships stay. |
| `hide` | Visibility `HIDDEN` and remove every chapter entry. **Unused today** (§0); kept for RC-3. |

An edit is **atomic on its preconditions**. Name mismatch, expected source chapter missing, or quest missing: the
whole edit is skipped, the quest is left as the pack has it, and the edit is reported.

### 4.5 The installer: `BQBridge` v2

It is still reflection-only, with no compile or runtime dependency on BetterQuesting, and every method used is
checked by javap against `BetterQuesting-3.8.89-GTNH.jar` in unit Q3.

**`install(force)`** runs in this order:

1. **Load the bundle** from the jar (`overlay.json`). If the jar has none, fall back to `DefaultQuests` (today's path).
2. **Read the world state file** `saves/<world>/beyondthestars/questbook-state.json`, which holds:
   - the applied `bundleVersion`;
   - for each applied edit, the **original** quest NBT and its original chapter entries (captured the first time the
     edit was applied, and never overwritten while the edit stays applied);
   - the placeholder flag of every quest of ours at the last install.
3. **Snapshot progress**: `QuestDatabase.writeProgressToNBT(list, null)`, as now.
4. **Our chapters:**
   - rebuild each 190-high line from the bundle (`QuestLine.readFromNBT`) and `put` it;
   - `setOrderIndex` UXV / MAX / Endgame directly after UMV's index, and the plant chapter last;
   - graft our UIV and UMV entries into the pack lines (`QuestLine.put(uuid, QuestLineEntry)`).
5. **Our quests:** `createNew` or reuse, then `readFromNBT(bundle NBT)`. A fallback quest (`onlyWithout`) is created
   only while its edit is not applied; otherwise it is removed.
6. **Pack edits**, when `Config.questbookPackEdits` and each edit's config flags hold. For each edit:
   1. take the stored original, or capture it now from the live quest;
   2. check the preconditions against that original;
   3. apply the ops to a **copy of the original**;
   4. `readFromNBT` the result into the live quest;
   5. rebuild the quest's chapter entries from the original entries plus the `move` ops.

   An edit that is no longer wanted (flag off, or dropped from the bundle) is **reverted**: the original NBT and
   entries are restored and its state entry is deleted.
7. **Restore progress** from the step-3 snapshot with `readProgressFromNBT(list, true)`, after filtering it (§4.6).
8. **Write the state file**, `SaveLoadHandler.markDirty()`, then `NetBulkSync.sendReset(null, true, true)`, as now.

All of steps 4-7 run inside the existing `try { … } finally { restore progress }`, so an exception mid-way still puts
the campaign back.

**Idempotence.** Running it twice gives the same database. Our content is replaced wholesale from the bundle, and
edits are always computed from the stored originals.

**`uninstall [all|edits]`:**

- `edits` reverts every applied pack edit from its original, and removes the fallback quests.
- `all` (the default) does that, then removes our three chapters, our grafted entries, and every 190-high quest. The
  plant chapter goes too, so `uninstall all` → `install` is a clean reinstall.
- Progress is snapshotted and restored around it, as in install. Pack quests keep their completion.
- The state file is deleted (`all`) or trimmed (`edits`).

**`status`** prints:

- the BQ version and the bundle version against the world's;
- each of our chapters with "n / expected" and the missing lows (from `BTSQuestIds`);
- the pack edits: applied, skipped (with the reason: name drift, chapter drift, config flag off, `questbookPackEdits`
  off), and per-op text misses;
- the placeholders still pending (with their feature names);
- whether auto-install is on.

**`verify`** (op, dev tool) resolves every item, fluid and ore name in the bundle against the live registries
(`GameData`, `FluidRegistry`, `OreDictionary`) and lists the misses.

**`dumpids`** writes the resolved ids the generator needs to `config/beyondthestars/questbook-registry.json`, for
copying into `questbook/registry.json`.

**Auto-install** (`Config.questbookAutoInstall`, default true) hooks the server start *after* BetterQuesting has
loaded the world's database. Unit Q3 confirms the event: BQ loads in its own `FMLServerStartingEvent` handler, so ours
must run later, either at `FMLServerStartedEvent` or on BQ's `DatabaseEvent.Load` if 3.8.89 posts one. It runs
`install(false)` when:

- the state file's `bundleVersion` differs from the jar's; or
- a cheap check fails: any of our line ids is missing, or an applied edit's live name no longer equals its edited
  name. That last case is what a `/bq_admin default load` leaves behind.

Otherwise it does nothing, so an unchanged world pays one file read. It logs one line, and broadcasts nothing unless
it changed something.

**New worlds.** BQ copies `DefaultQuests` (pack plus our mirror) at creation; the first server start then applies the
edits. Without the auto-install, the world has our chapters (from `DefaultQuests`) and the unedited pack, with the
fallback quests standing in, until an op runs `/btsquests install`.

**Config** (`config/beyondthestars.cfg`, category `questbook`):

| Key | Default | Meaning |
|---|---|---|
| `questbookAutoInstall` | true | Install or refresh our chapters and edits on server start when needed |
| `questbookPackEdits` | true | Apply the pack edits. When false, the next install **reverts** any applied ones and emits the fallback quests |

`/btsquests` keeps `status` as the default. Its usage line becomes `/btsquests [status|install [force]|uninstall
[all|edits]|verify|dumpids]`, permission level 2.

**As built (unit Q3, 2026-10-13).** `BQBridge` holds the reflection primitives (every member
javap-checked against `BetterQuesting-3.8.89-GTNH`); the engine is `QuestbookInstaller`, with
`QuestbookBundle` (the jar's `overlay.json`), `QuestbookState` and `QuestbookEvents`. Where it
differs from the text above:

- **The state file is NBT**, `saves/<world>/beyondthestars/questbook-state.dat` (gzip), not JSON:
  it stores whole pack quests, and NBT round-trips them exactly, which is what makes `uninstall`
  byte-identical. Besides the original it keeps each edit's quest **as applied**; the cheap
  check compares the live quest with that (`NBTTagCompound.equals`), which catches a
  `/bq_admin default load` and a hand edit alike.
- **Re-capture, gated on `pack_version`.** Each stored original records the world's
  BetterQuesting `pack_version` it was captured under. While the world's number is unchanged,
  the stored original stays the truth: a live quest that differs from what we applied (a hand
  edit, a same-pack `/bq_admin default load`) gets the edit re-applied over the stored original,
  and is never recaptured (it may still carry our ops). Once the number differs (a pack update
  and BetterQuesting's default load), the live quest is the pack's: it is matched again and, if
  it passes the id + name + chapter check, becomes the new original; if not, the edit is skipped
  with a warning and its old application reverted and dropped, so a renamed or moved RC-3 quest
  is left as RC-3 wrote it and `uninstall` never restores the old pack's quest.
- **Revert keeps the book whole.** Reverting an edit whose quest no longer equals what we
  applied leaves its definition alone but still restores its chapter placement when the quest
  is where we put it, or only in chapters of ours (those are rebuilt or removed right after).
  As a safety net, rebuilding one of our chapters keeps any pack quest that would otherwise be
  left in no chapter; `uninstall all` names any it has to strand.
- **Task progress** is dropped wherever a quest's task list changed (live task NBT before vs
  after), for our quests and pack edits alike. In practice that is exactly the `completionOnly`
  edits on their first application; a re-install never drops anything. A `keep` edit that
  changes tasks is reported as an op miss.
- **`uninstall edits`** leaves the world exactly as `questbookPackEdits=false` would: every
  pack quest restored from its original, our chapters kept, and the fallback quests 48103 /
  48109 **shown** (rule 14: the KEE and the Eye must still be taught). The spec text said it
  removes them.
- **Uninstall is sticky.** Both forms record `uninstalled` in the state file, and the
  auto-install leaves that world alone until an op runs `/btsquests install`.
- **`install` without `force`** answers "already up to date" when the cheap check passes;
  `install force` always runs.
- **Progress backup** (not in the spec): BetterQuesting's own default load, which it runs by
  itself when `pack_version` changes, clears the whole database and restores progress only
  for quests in `DefaultQuests`. Without our mirror merged, our quests would come back from the
  jar with no progress. `questbook-progress.dat` holds every player's progress on our quests,
  refreshed on each overworld save; a quest the installer has to re-create gets its rows back
  (task progress only if taken under the same bundle; a placeholder that became real gets
  nothing). Each row says whether its quest was a checkbox placeholder when saved, and
  `uninstall all` keeps the state's placeholder list, so `uninstall all`, a jar update and
  `install` cannot hand a ticked box's completion to the real quest.
- **Auto-install event:** `FMLServerStartedEvent`. BetterQuesting loads the world and default-
  loads a new world in its `FMLServerStartingEvent` handler (`SaveLoadHandler.loadDatabases`,
  then `/bq_admin default load` when `loadDefaultsOnStartup` or `hasUpdate`). Its
  `DatabaseEvent.Load` would need a compile dependency to subscribe to.
- **Grafts** use their `on` position only when every edit that moves a quest into or out of that
  chapter applied; otherwise `off`.
- **Chapter order** is re-set on every install (ladder chapters directly after UMV, the plant
  chapter last).

### 4.6 Progress

BQ keeps progress inside each quest: a completion map per player UUID, and task progress per task index. The snapshot
from step 3 is filtered before step 7 restores it.

| Case | Kept | Dropped |
|---|---|---|
| Our quest, unchanged kind | completion and task progress | — |
| Our quest that **was a placeholder** at the last install and is real now | — | completion and task progress (the box was not the machine) |
| Pack edit with policy `keep` (text, prereqs and moves only) | completion and task progress | — |
| Pack edit with policy `completionOnly` (E01, E10, E63, E64: the task list changed) | completion | task progress (old task indexes would land on new tasks) |
| Everything else in the database | everything | — |

So a player who finished the BHC quest keeps it finished: it is now the KEE quest, still complete, and its
dependents stay unlocked. A player halfway through "An Eye at the End of Time" starts its new tasks fresh. Moving a
quest between chapters never touches progress: chapter membership is not progress.

Unit Q3 confirms the progress NBT layout, `{questIDHigh, questIDLow, completed[], tasks[{index, …}]}`, with javap, and
writes the filter against it.

### 4.7 Pack updates (RC-3 and on)

- **At build time** the generator checks every edit against a pack `DefaultQuests` (`-PackDir`):
  - the quest id exists;
  - the stripped name equals `expectName`;
  - the quest sits in `expectChapter`;
  - every `replaceText` `find` occurs;
  - every prerequisite id named exists.

  On an RC-3 pack this produces a **drift report** (`questbook/pack-drift.txt`), and the build fails until each
  drifted edit is updated or dropped. `packVersion` in the source records which pack the edits were last checked
  against.
- **At runtime** a world on a newer pack than the jar was built for gets the same checks live. A drifted edit is
  **skipped with a warning**: the server log, `/btsquests status`, and a one-line op chat message on login if any edit
  was skipped. The rest still apply. A skipped edit leaves the pack's quest as the pack wrote it, so the book is never
  half-edited.
- **New pack quests** that depend on a quest we moved just follow it. Prerequisites cross chapters fine.
- The RC-3 pack may itself rewrite the EOH or the BHC quests. Their names then change, E01 and E10 are skipped, and
  the fallback quests 48103 and 48109 take over automatically, because the fallback rule is "edit not applied", not
  "config off".

---

## 5. Open questions (log as QB-1.. in `singularity-ladder-questions.md`)

| # | Question | Built / proposed | Suggestion |
|---|---|---|---|
| **QB-1** | **BLOCKER found while writing this spec.** With `replaceTecTechEyeOfHarmony=true`, Universium has no source until Eye T10 (B-2). RC-2's `Field_Generator_UXV` takes **576 L MHDCSM + 576 L Universium** (`ResearchStationAssemblyLine.java:2178`), and SL-E05 / SL-E06 (the Subspace Cooling and Magnetar Seed Cores) take Field Generators UXV and UXV sensors / emitters. So Eye T5 and T6 are unreachable in survival, and so is every UXV component. | The book shows the real path, with the E20 warning. | Ship the D-45 UXV swap before P4, or swap SL-E05 / E06's UXV parts for UMV ones plus more WDM / BDM. |
| QB-2 | T8 optional (D-42) or required? | Required (D-49 strict ladder, P4 §5.4) | Keep required. |
| QB-3 | MAX parts: 13 placeholders in the mockup | 1 hub + 4 groups + MAX Circuits | Expand in P5. |
| QB-4 | Recipe moves (Hexanite on the T4 Eye, the Reinforced Resonance Chamber on the T4 Eye, the BEC condensates) | No placeholder quests | Add quests when built. |
| QB-5 | Eternity, MMM, Perfected and The SUCC stage in The Endgame until `eternityOnErgomatter` | As in §3.5 | — |
| QB-6 | "Constrained Eternity Parts" (the mockup's new quest) | Dropped. The Door to Heaven? gains MHDCSM + Resplendent instead. | — |
| QB-7 | The trophy quests now consume 12 / 8 of **our** Eye controllers | E63 / E64 | **Settled 2026-10-13: "Keep as trophies"** (the trophy quests keep consuming our Eye controllers). Built as is. |
| QB-8 | Homogeny placeholders shown although the Homogeny is on HOLD | 48118, 48142, 48184 | Keep; they say "planned". |
| QB-9 | The Astral Array does nothing in our Eye | E47 warning | Decide in P5 (D-48 has it parallel T10). |
| QB-10 | Eye T1 gains Godforge T2 ("A Second Ring has Hit the Godforge") as a prerequisite (D-41) | E10 `addPrereq` | — |
| QB-11 | Magnetic Monopole Matter dropping MHDCSM (D-48 proposal) is staged behind `eternityOnErgomatter`; the RC-2 Magmatter route (Godforge exotic module and upgrade costs) was not fully audited | — | Audit with the UXV swap. |
| QB-12 | Pack edits are runtime-only; `DefaultQuests` pack files are never written | §4 | — |
| QB-13 | T8: Tipler keeps its pack prerequisites (MHDCSM, Resplendent), so the Eye T8 gate sits behind Universium | — | P4's gate audit must clear it, as SL-P33 did for T5. |

---

## 6. Work breakdown for the next units

They share the worktree, run one at a time, and only Q3 and Q5 run gradle. Each one commits granular `wip:` commits,
staging only its own files.

| # | Agent | Files | Done when |
|---|---|---|---|
| **Q1** | **Source data** | `questbook/singularity.json` (all of §1-§3: every quest, text, task, position and edit, transcribed exactly; descriptions in `{x}` tokens), `questbook/id-ledger.txt` (48000-48034 + 48100-48220 as allocated), first `questbook/registry.json` (the verified ids from §1, the rest flagged `verify`) | The JSON parses. Every §2 and §3 row is present. No text says anything §2 does not. |
| **Q2** | **Generator** | `questbook/qb-common.ps1` (helpers moved out), `gen-singularity.ps1`, the one call added at the end of `gen-questbook.ps1`, `questbook/DefaultQuests/**` (regenerated), `assets/beyondthestars/questbook/overlay.json`, `BTSQuestIds.java` (generated), `questbook/README.md` (new sections: the bundle, the ladder chapters, `OrderLines.txt`, merge steps, drift check) | `gen-bom.ps1` runs clean against the RC-2 instance `DefaultQuests`. Every §4.2 assertion is implemented and seen to fire on a deliberately broken input (one overlap, one line crossing, one drifted name, one unknown item). The plant chapter's files are byte-identical to before. |
| **Q3** | **Installer** | `BQBridge.java` (v2: bundle, state file, edit engine, progress filter, uninstall, verify, dumpids, auto-install hook), `CommandBTSQuests.java`, `Config.java` (two keys), `CommonProxy` (the hook), lang if any | javap on `BetterQuesting-3.8.89-GTNH.jar` confirms every reflected member: `QuestLine.removeID` (or its 3.8.89 name), `QuestLineDatabase` order list access, `writeProgressToNBT` / `readProgressFromNBT` and the progress NBT layout, `QuestInstance.writeToNBT`, `NetBulkSync.sendReset`, and BQ's load event. `gradlew spotlessApply compileJava` and then `build` pass. |
| **Q4** | **Id verification** | `questbook/registry.json`, `singularity.json` fixes | On the dev server, `/btsquests verify` reports **0 misses** for the built features (`temporalfluid`, `boltNeutronium`, `dustTengamAttuned`, the UXV laser hatches resolved), and the generator is re-run. |
| **Q5** | **Test** (dev server, headless; no client needed except Q6) | `CommandBTSBuild` / console harness lines if needed, `docs/singularity-ladder-questions.md` (QB-1..13 plus findings) | (a) Fresh world from the merged `DefaultQuests`: the auto-install applies 61 edits, and `status` is clean. (b) A copy of an RC-2 save with progress on the BHC, EOH, Universium and Stargate quests: install keeps every completion, and E01 / E10's task progress is dropped as specified. (c) A second install is a no-op (same DB dump). (d) `uninstall edits` restores every pack quest **byte-identical** to the snapshot (compare `writeToNBT` dumps), and `uninstall all` + `install` round-trips. (e) `questbookPackEdits=false` reverts the edits, and 48103 and 48109 appear. (f) Drift: rename one pack quest in a copy of `DefaultQuests`, make a new world, and see that edit skipped with a warning while the rest apply. (g) Flip one feature (`quasar`) in a scratch build: 48144 becomes real and its ticked box is cleared. |
| **Q6** | **Look** (with I) | none | Screenshots of UIV, UMV, UXV, MAX and The Endgame in a client: tidy, nothing overlapping, and the lines reading as §2 draws them. Any layout change goes back into `singularity.json` (Q1), never hand-edited JSON. |

**Order:** Q1 → Q2 → Q3 → Q4 → Q5 → Q6. Q3 can start in parallel with Q2 once Q1's bundle shape is fixed, because
the bundle format in §4.2 is the interface between them.

**Not in scope here:**

- recipe changes (the UXV swap, Eternity on Ergomatter);
- new machines;
- tooltip work. Rule 14's tooltip half belongs to each machine's own unit.

This spec only makes the book teach what the tooltips and GUIs already show.
