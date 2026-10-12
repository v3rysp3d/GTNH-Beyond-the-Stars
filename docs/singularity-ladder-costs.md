# Singularity Ladder: recipe completeness and cost audit (Kerr T1/T2, Eye T1-T4; Eye T5/T6 in §10)

Status: audit, 2026-10-12, addon `7a47289`. **GTNH 2.9.0-RC-2 port: see §9 for the BHC→Kerr swap, the Kerr controller price and the RC-2 gate audit.** It reads the code and the jars and changes no code. **The recipe audit
pass has since applied it: see §8 for what was applied, changed or skipped.** It answers the
acceptance requirement "all recipes exist and are fair", which A-3 in
[`singularity-ladder-questions.md`](singularity-ladder-questions.md) lists as open. The judgement calls it raises are
logged there as **B-21..B-24**.

Inputs:

- **Block counts:** `src/main/resources/assets/beyondthestars/structures/manifest.json`, the re-export with the
  Harmonic Lining as the Kerr's `J` (commit `ed9f34f`, re-read at the end of this audit; no change since).
- **Our recipes:** `BTSRecipesSingularity` (SL-P), `BTSRecipesKerr` (SL-K) and `BTSRecipesEye` (SL-E) at HEAD.
- **GT, TecTech and Godforge recipes and structures:** `javap -c` on the instance jars (`gregtech-5.09.52.594`,
  `GTNewHorizonsCoreMod-2.8.279`):
  - TecTech `ResearchStationAssemblyLine.addEOHRecipes`, `addGodforgeRecipes` and `runLateRecipes`;
  - the coremod's `AssemblerRecipes`, `AssemblingLineRecipes`, `FluidSolidifierRecipes` and
    `SpaceAssemblerRecipes`;
  - the structure strings of `MTEBlackHoleCompressor`, `MTEEyeOfHarmony`, `ForgeOfGodsStructureString`,
    `ForgeOfGodsRingsStructureString` and `MTENanoForge`.
- **The model** is a throwaway script. Each recipe is divided by its output count and expanded down to raw materials.
  The numbers below come from it.

**Conventions.**

- Metals are counted in **ingots**: 144 L molten = 1 ingot, plate 1, dense plate 9, superdense 64, foil ¼, frame 2,
  gear 4, small gear 1, `wireGt01` ½, huge and quadruple pipes 12, block 9.
- Components (Emitters, Sensors, Field Generators, Robot Arms) are rolled up to **their circuits only**:
  - Emitter and Sensor UIV: 4 UIV each.
  - Field Generator UIV: 4 UMV circuits + 4 Emitter UIV.
  - Robot Arm UIV: 2 UIV + 4 UEV + 8 UHV.
  - The UMV parts follow the same pattern one tier up. A Field Generator UMV is **4 UXV** circuits + 4 Emitter UMV.
- Their own metals and assembly-line time are not counted.
- **EU** is recipe EU/t × duration, summed over every craft the rollup triggers. **Machine-hours** are the same
  durations on one machine, with no overclock or parallel.
- **Sockets** count as their fallback casing; the hatches themselves are ordinary GT/AE parts.
- **Avaritia's Neutronium block** counts as Cosmic Neutronium (GTNH unifies them).

---

## 1. Verdict

| Machine tier | Today | Main reason | After the proposed adjustments |
|---|---|---|---|
| **Kerr T1** (UIV, replaces the BHC) | **Fair**, a little heavy | It reuses about 70 % of a BHC's own casing bill (2,621 Extreme Density, 639 Background Radiation) and adds an Infinity/SpaceTime body. Coils and quadrupoles are over-priced: 41k SC UIV ingots, 47M L Super Coolant, 448 Particle Beam Guidance Pipe Casings. | Fair: about 1.5× a BHC (#6, #7) |
| **Kerr T2** (UMV) | **Uncraftable** | SL-K04 needs a Superstable Black Hole Seed. **No recipe for it exists anywhere in the pack** (all 242 mod jars scanned). The reinforcement itself is fair, cheaper than one Godforge ring. | Fair (#1) |
| **Eye T1** (UMV, replaces TecTech's EOH) | **Far too expensive** | 13,872 TecTech Reinforced Spatial/Temporal casings (TecTech's Eye: 1,430) at 8 Godforge Singularity Shielding casings each, which is **118k Godforge casings = 42 Godforge main rings**. Plus 3,582 Infinite Spacetime Energy Boundary casings (TecTech's: 36), each 1 LSC + 16 Active Transformers. Plus 620 field generators (TecTech's: 354), 294 of them placeholders. **8× TecTech's whole Eye.** | Fair: about TecTech's crude Eye in EU, UXV circuits and assembly-line hours (#4, #5, #12). **Shell since ruling 11a (§4.1):** RC-2's BEC recipe at ×32, about 1.3× TecTech's stock RC-2 shell in Godforge casings and in BEC time; #3's ¼-casing Assembler recipes are retired |
| **Eye T2** | **Uncraftable** with `replaceTecTechEyeOfHarmony=true` | The 1:1 gate needs tier-2 Spacetime Compression generators. TecTech's recipe takes **2 White Dwarf Matter bolts** each, and WDM's only source is now our Eye T4: circular. The beam band is also over-priced (12.9k UIV/UMV circuits, 114M L Super Coolant). | Fair (#2, #8) |
| **Eye T3** | **Uncraftable** (as T2) | Tier-3 generators need 8 WDM bolts each. The jet crescent is over-priced (689 TecTech casings, 1,378 coils). | Fair (#2, #9) |
| **Eye T4** | **Uncraftable** (as T2, plus the HSM chain is blocked by #1) | Tier-4 generators need 32 WDM bolts each. Galactic Shell needs 2.66M L HSM, which is 532 Arcturus runs (about 710 h of an Eye T3). | Fair (#2, #10, #11) |

The whole pack-native gate cost (TecTech's compression-generator recipes) stays as TecTech prices it. Each tier step
re-buys 188 generators, about 3.8 / 5.7 / 7.6 × 10¹⁵ EU and 420 / 630 / 840 assembly-line hours. That is TecTech
parity (TecTech's Eye pays the same per generator), but the old set is stranded each step: see B-23 / #15.

---

## 2. Completeness: every block and item, and its recipe

✔ craftable · ✘ **no recipe** · ⚠ craftable only through something that has no source.

### 2.1 Kerr T1 (pieces `kerr_t1` + `kerr_compressor` + `kerr_extractor`, 15,363 blocks)

| Char | Block | Count | Recipe | Status |
|---|---|---|---|---|
| `D` | Mirror Backing Casing (slCasing:2) | 3,551 | SL-P02 ASM ×2 | ✔ |
| `B` | Naquadah Alloy frame | 3,186 | GT | ✔ |
| `C` | Superradiant Mirror Casing (slCasing:1) | 2,852 | SL-P03 ASM ×2 | ✔ |
| `A`,`L` + sockets `b`,`u`,`e`,`m` | Extreme Density Space-Bending Casing (BHC) | 2,621 | coremod ASM: 64 superdense Bedrockium + 64 Steel + 64 Iron + Neutronium-Plated Reinforced Stone + 16,000 L wet concrete | ✔ |
| `H` | Ergosphere Field Coil (slCasing:6) | 842 | SL-P08 | ✔ |
| `I` + sockets `c` | Background Radiation Absorbent Casing (BHC) | 639 | coremod ASM: 64,000 L UU-Matter + Radiation Proof Casing + 8 adv. radiation plates + superdense Prismatic Naquadah | ✔ |
| `J` | Harmonic Lining (slCasing2:2) | 521 | SL-P12 ASM ×4 | ✔ |
| `N` + sockets `x` | Ergosphere Extraction Casing (slCasing:15) | 317 | SL-P14 ASM ×2 | ✔ |
| `E` | Bolt Injector Rail Mk I / II / III (slCasing:3/4/5) | 232 | SL-P05 / P06 / P07 | ✔ |
| `F` | Beam Focusing Quadrupole (slCasing:7) | 208 | SL-P09 | ✔ |
| `K` | Hawking Radiation Realignment Focus (BHC glass) | 199 | coremod fluid solidifier: BW glass + 1,152 L Rhugnor | ✔ |
| `M` | Beam Dump (slCasing:8) | 160 | SL-P10 | ✔ |
| `G` | Particle Beam Guidance Pipe Casing (`BlockQuarkPipe`) | 32 | coremod AL ×4 (UIV) | ✔ |
| `~` (compressor arc) | Kerr Compressor Module controller (MTE 28117; was the slCasing2:0 core, Kerr redesign) | 1 | SL-K02 AL (same inputs) | ✔ |
| `~` (extractor arc) | Kerr Extractor Module controller (MTE 28118; was the slCasing2:1 core) | 1 | SL-K03 AL (same inputs) | ✔ |
| `~` | Kerr Ergosphere Engine | 1 | SL-K01 AL (needs 8 Superradiant Mirror Segments, SL-P04 AL) | ✔ |
| sockets | energy/laser, maintenance, input buses (bolts), BHC utility hatch, compressor/extractor I/O | | GT / coremod (utility hatch: coremod ASM) | ✔ |
| (fuel) | Black Hole Seed / Collapser | | coremod Space Assembler (UHV) | ✔ |

### 2.2 Kerr T2 (adds the `kerr_t2` reinforcement; 29,839 blocks)

| Block / item | Count | Recipe | Status |
|---|---|---|---|
| Bastion Plate (slCasing:13) | 8,316 | SL-P18 ASM ×4 | ✔ |
| Harmonic Lining (more) | +4,031 | SL-P12 | ✔ |
| Hawking glass (more) | +1,442 | coremod | ✔ |
| Casimir Plate (slCasing:14) | 677 | SL-P19 ASM ×4 | ✔ |
| Particle Beam Guidance Pipe Casing (more) | +10 | coremod | ✔ |
| **Kerr Density Upgrade** | 1 | SL-K04 AL | ✘ **needs `ItemList.Black_Hole_Stabilizer` (Superstable Black Hole Seed). Only `ItemList`, the item registration and the BHC reference it; no mod in the pack crafts it (#1)** |
| Mk III rails (inside SL-K04) | 16 | SL-P07 | ✔ |

**Consequence:** no Kerr T2, so no DENSIFY and no Hyperdense Stellar Mixture. So no Galactic Shell Plating (SL-P17),
no Galactic Upgrade Core (SL-E02) and no Star Chart: Sirius B (SL-P25), and therefore no Eye T4.

### 2.3 Eye T1 (piece `eye_core`, 19,616 blocks)

| Char | Block | Count | Recipe | Status |
|---|---|---|---|---|
| `U` | Reinforced Spatial Structure Casing (TecTech) | 9,406 | TecTech AL ×4: **32 Godforge Singularity Shielding Casings**, 64 Neutronium blocks, 64 Avaritia Neutronium blocks, 48 Neutronium nanites, 1,024 Neutronium + 1,024 Cosmic Neutronium molten, … (10,000 ticks UMV) | ✔ (needs Godforge-structure-tier production; the shielding casing is TecTech AL ×4 with 2.36M L molten Bedrockium) |
| `Q` + sockets `h`,`o`,`m` | Reinforced Temporal Structure Casing (TecTech) | 4,466 | as `U`, with Accelerator UV ×4 | ✔ |
| `J` | Infinite Spacetime Energy Boundary Casing (TecTech) | 3,582 | TecTech AL **×1**: LSC controller, 16 Active Transformers, 4 UMV Wireless Energy Hatches, UMV laser tunnel, 64 High Energy Flow Circuits, 4,096 Neutronium + 4,096 Cosmic Neutronium (molten), … | ✔ |
| `Z` | Quantum Glass (TecTech) | 1,541 | TecTech ASM (UV): Containment casing + reinforced glass + 576 L Trinium | ✔ |
| `Y` | Stabilisation Field Generator, any tier | 294 | TecTech AL: consumes a Time Dilation **and** a Compression generator of its tier, plus a Boundary casing; 2 UXV circuits; 80,000 ticks UMV | ✔ |
| `W` | Spacetime Compression Field Generator, tier 1 (Crude, meta 0) | 188 | TecTech AL: 1 Spatial casing, 1 UXV circuit, ME Singularity cell, quantum tanks, Infinity chest, Cosmic Fabric Manipulator, **2 Shirabon bolts**; 80,000 ticks UMV | ✔ |
| `X` | Time Dilation Field Generator, any tier | 138 | TecTech AL: 1 Temporal casing, 1 UXV, ZPM fusion computer, Dyson parts…; 80,000 ticks UMV | ✔ |
| `~` | Eye of Harmony (ours) | 1 | SL-E01 AL: needs 6 Stabiliser Pylons (SL-P21 AL) and the three Tier0 generators | ✔ |
| target | Star Chart: Sol | 1 | SL-P22 | ✔ |
| sockets | input hatches, ME output bus + hatch, maintenance | | GT / AE2 | ✔ |

### 2.4 Eye T2-T4 (bands, gates, cores, charts)

| Tier | Block / item | Count | Recipe | Status |
|---|---|---|---|---|
| T2 | Beam Band Casing (slCasing:10) | 2,856 + 6 sockets | SL-P15 ASM ×4 | ✔ |
| T2 | Boundary casing / Quantum Glass | 1,106 / 375 | TecTech | ✔ |
| T2 | Star Chart: Sirius A | 1 | SL-P23 (Raw Star Matter) | ✔ |
| T2 | **Compression generators tier 2** (meta 1), all 188 `W` cells | 188 | TecTech AL: needs **2 White Dwarf Matter bolts** each | ⚠ **circular**: with `replaceTecTechEyeOfHarmony=true`, WDM comes only from our Eye T4 (Sirius B) (#2) |
| T3 | Jet Crescent Casing (slCasing:11) | 2,750 + 5 sockets | SL-P16 ASM ×4 (Raw Star Matter) | ✔ |
| T3 | Boundary / Quantum Glass | 908 / 321 | TecTech | ✔ |
| T3 | Star Chart: Arcturus | 1 | SL-P24 (ESPM) | ✔ |
| T3 | Compression generators tier 3 (meta 2) | 188 | TecTech AL: **8 WDM bolts** each | ⚠ circular (#2) |
| T4 | Galactic Shell Plating (slCasing:12) | 5,325 | SL-P17 ASM ×2 (HSM) | ⚠ HSM is blocked by the Kerr Density Upgrade (#1) |
| T4 | Boundary / Quantum Glass | 830 / 404 | TecTech | ✔ |
| T4 | Galactic Upgrade Core | 1 | SL-E02 AL (HSM) | ⚠ (#1) |
| T4 | Star Chart: Sirius B / Cooled | 1 / 1 | SL-P25 (HSM) / SL-P26 | ⚠ (#1) |
| T4 | Compression generators tier 4 (meta 3) | 188 | TecTech AL: **32 WDM bolts** each | ⚠ circular (#2) |

TecTech's generator tiers 5-9 need Black Dwarf Matter and MHDCSM bolts; those are the P4/P5 gates and out of scope
here. Nothing in the three recipe files was skipped at boot ("255 registered, 0 skipped"). A registered recipe is not
always a craftable one, though: the two ✘/⚠ chains above register fine.

---

## 3. Per-block cost (one block, raw ingots unless marked)

### 3.1 Today

| Block | Infinity | SpaceTime | TM | Neutronium | Cosmic N. | Other | Circuits | EU / block |
|---|---|---|---|---|---|---|---|---|
| Mirror Backing (SL-P02) | 7 | 1 | | | | | | 9.4e9 |
| Superradiant Mirror (SL-P03) | 6.5 | 0.75 | | 2 | | | | 1.4e10 |
| Rail Mk I (SL-P05) | | 10 | | | | 1 Robot Arm UIV, 1 Piston UIV | 2 UIV, 4 UEV, 8 UHV | 9.4e9 |
| Field Coil (SL-P08) | | | 2 | | 8 | **32 SC UIV**, **32,000 L Super Coolant** | | 3.8e10 |
| Quadrupole (SL-P09) | **120** | | 4 | 8 | 40 | **64 SC UIV**, 2 beam-pipe casings, 80,000 L Super Coolant | 5 UIV, 2 UEV | 1.3e11 |
| Beam Dump (SL-P10) | | | | 72 | 16 | 16,000 L Super Coolant | | 1.9e10 |
| Harmonic Lining (SL-P12) | 1.5 | | 0.25 | 1.5 | | | | 4.7e9 |
| Extraction Casing (SL-P14) | | 2 | | 9 | | ½ Pump UIV | | 1.3e10 |
| Bastion Plate (SL-P18) | 4.5 | | 0.5 | 13.5 | | | | 3.8e10 |
| Casimir Plate (SL-P19) | | 2 | | 2 | | | | 3.8e10 |
| Beam Band (SL-P15) | 124 | 20 | 66 | 1,128 | 184 | ¼ Spatial casing (= 2 Godforge casings), 8,200 Bedrockium, 40,000 L Super Coolant | 2.5 UIV, 2 UMV, 1 UEV | 2.1e11 |
| Jet Crescent (SL-P16) | 82 | 8 | 65 | 1,124 | 168 | ¼ Temporal casing, 8,192 Bedrockium, 4,000 L Raw Star Matter | 1 UMV | 1.8e11 |
| Galactic Shell (SL-P17) | 18 | | 5 | | 2 | **500 L HSM** | | 5.0e10 |
| Extreme Density (BHC) | | | | | | **4,096 Bedrockium**, 4,096 Steel, 4,096 Iron | | 1e7 |
| Background Radiation (BHC) | | | | | | **64,000 L UU-Matter**, 64 Prismatic Naquadah | | 3e8 |
| Particle Beam Guidance Pipe | 60 | | | | 12 | 8 Bedrockium, 8 Naquadria, 8 Tritanium, ¼ Pump UEV | 1 UEV, ½ UIV | 9.4e9 |
| Reinforced Spatial / Temporal (TecTech) | 256 | | 256 | 4,496 | 656 | **8 Godforge casings**, 32,768 Bedrockium, 4,096 Celestial Tungsten, 4 Combined Singularities | | 3.5e11 |
| Infinite Spacetime Energy Boundary (TecTech) | | 111 | | 4,096 | 4,096 | **1 LSC, 16 Active Transformers, 4 UMV wireless hatches** | | 1.3e12 |
| Compression generator tier 1 / 2 / 3 / 4 | 256 | 10 | 256 | 4,496 | 656 | 1 Spatial casing; WDM 0 / ¼ / 1 / 4 ingots | 1 / 1 / 1 / 2 UXV | 1.0 / 2.0 / 3.1 / 4.1e13 |
| Stabilisation generator (Crude) | 512 | 146 | 512 | 13,088 | 5,408 | compression + dilation generator + Boundary casing | 4 UXV | 3.2e13 |

### 3.2 After the proposed adjustments (only the blocks that change)

| Block | Infinity | SpaceTime | TM | Neutronium | Cosmic N. | Other | Circuits | EU / block |
|---|---|---|---|---|---|---|---|---|
| Field Coil (#6) | | | 2 | | 8 | 8 SC UIV, 8,000 L Super Coolant | | 3.8e10 |
| Quadrupole (#7) | 30 | | 1 | 2 | 10 | 4 SC UIV, ½ beam-pipe casing | 1.25 UIV, ½ UEV | 3.3e10 |
| Beam Band (#8) | 2.5 | 6.3 | 0.6 | 8.7 | 1.7 | 1/16 Spatial casing | 0.5 UMV | 1.9e10 |
| Jet Crescent (#9) | 5.1 | 2.1 | 0.8 | 8.6 | 2.1 | 1/16 Temporal casing, 4,000 L Raw Star Matter | 0.5 UMV | 2.6e10 |
| Galactic Shell (#10) | 4.5 | | 1.25 | | 0.5 | 15.6 L HSM | | 6.3e9 |
| ~~Spatial / Temporal via SL-P27/P28 (#3)~~ retired by ruling 11a | ~~10.3~~ | ~~1~~ | ~~8~~ | ~~137~~ | ~~17~~ | ~~¼ Godforge casing~~ | | ~~3.9e10~~ |
| Spatial / Temporal via RC-2's BEC recipe at ×32 (SL-B01/B02, ruling 11a) | 4 | | | 16 condensate + 1.5 nanites (consumed) | 4 + 16 condensate | **1 Godforge casing**, 1 dense ProtoHalkonite, ¼ Hexanite block, 4 Bedrockium + 8 Bedrockium condensate, 4 Shirabon, 2 GraviSuite parts, 1/32 UV Solar + Energised Tesseract (+ 1/32 Quantum Chest IV or 1/8 Accelerator UV), 0.3 ingot Space / Time condensate; a **BEC network** (Condensate Assembler, nanites) | | 3.1e12 (1,250 s of a 40,000 s UMV craft) |
| Boundary via SL-P29 (#4) | | 1 | | 9 | 9 | 1 SC UMV | ½ UIV | 1.9e10 |

---

## 4. Totals per machine tier (today → **proposed**)

Bands and gates are **increments** on the tier below. The Eye rows include the field generators each tier's gate
needs, 188 compression generators re-bought at every step (B-20: they are swapped by hand). "Eye T4 complete"
excludes the Kerr T2 that makes its HSM.

| Machine tier | Blocks | Godforge shield casings | Bedrockium | Infinity | SpaceTime | Transc. Metal | Neutronium | Cosmic Neut. | SC UIV | Circuits UHV / UEV / UIV / UMV / UXV | Main fluids | EU | Machine-hours |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Kerr T1 (structure + both modules + controller) | 15k | 0 | 10.9M | 73k → **54k** | 12k | 3725 → **3089** | 24k → **22k** | 18k → **12k** | 41k → **7712** | 1984 / 1448 / 2436 / 128 / 0 → **1984 / 1130 / 1641 / 128 / 0** | Super Coolant 49.0M, UU-Matter 40.9M → **Super Coolant 13.1M, UU-Matter 40.9M** | 1.5e+14 → **1.3e+14** | 67 → **58** |
| Kerr T2 increment (reinforcement + Density Upgrade) | 14k | 0 | 80 | 45k | 5674 | 5294 | 121k | 376 | 1024 → **256** | 256 / 266 / 133 / 576 / 128 | Super Coolant 1.0M, Raw Star Matter 4.0M → **Super Coolant 256k, Raw Star Matter 4.0M** | 4.0e+14 | 56 |
| Eye T1 (core + controller + Sol chart) | 20k | 122k → **3740** | 498.3M → **15.3M** | 3.9M → **157k** | 477k → **28k** | 3.9M → **120k** | 84.9M → **2.1M** | 26.5M → **288k** | 0 | 0 / 0 / 0 / 1393 / 1828 → **0 / 0 / 1896 / 1393 / 844** | Super Coolant 3.0M | 2.3e+16 → **5.5e+15** | 2696 → **612** |
| Eye T2 increment (beam band + gate gens + chart) | 4532 | 7228 → **92** | 29.6M → **376k** | 403k → **9127** | 183k → **21k** | 237k → **3114** | 8.6M → **61k** | 5.2M → **18k** | 92k → **716** | 0 / 2862 / 7155 / 5725 / 188 → **0 / 89 / 777 / 1432 / 188** | Super Coolant 114.5M, Raw Star Matter 10k, WDM ingots 47 → **Super Coolant 1.4M, Raw Star Matter 3.0M** | 5.8e+15 → **3.9e+15** | 720 → **430** |
| Eye T3 increment (jet crescent + gate gens + chart) | 4173 | 7014 → **90** | 28.7M → **369k** | 274k → **16k** | 125k → **8658** | 227k → **3574** | 7.7M → **58k** | 4.3M → **17k** | 44k → **2755** | 0 / 0 / 0 / 2756 / 188 → **0 / 0 / 454 / 1378 / 188** | Super Coolant 44.1M, Raw Star Matter 11.0M, ESPM 1000, WDM ingots 188 → **Super Coolant 2.8M, Raw Star Matter 11.0M, ESPM 753k** | 7.4e+15 → **5.8e+15** | 845 → **643** |
| Eye T4 increment (arms + Galactic Core + gate gens + chart) | 6749 | 1504 → **47** | 6.2M → **193k** | 145k → **26k** | 95k → **4274** | 76k → **9266** | 4.2M → **33k** | 3.5M → **13k** | 0 | 0 / 0 / 0 / 641 / 504 → **0 / 0 / 415 / 641 / 504** | HSM 3.0M, WDM ingots 752 → **RHSP 376k, HSM 117k** | 9.0e+15 → **7.7e+15** | 992 → **844** |
| Eye T4 complete (T1..T4) | 35k | 137k → **3969** | 562.8M → **16.3M** | 4.7M → **209k** | 880k → **62k** | 4.4M → **136k** | 105.4M → **2.2M** | 39.5M → **337k** | 136k → **3470** | 0 / 2862 / 7155 / 11k / 2708 → **0 / 89 / 3541 / 4844 / 1724** | Super Coolant 161.6M, Raw Star Matter 11.0M, ESPM 1000, HSM 3.0M, WDM ingots 987 → **Super Coolant 7.2M, Raw Star Matter 14.0M, ESPM 753k, RHSP 376k, HSM 117k** | 4.5e+16 → **2.3e+16** | 5253 → **2528** |
| REF Black Hole Compressor structure | 4863 | 0 | 15.0M | 0 | 0 | 0 | 0 | 0 | 0 | 0 / 0 / 0 / 0 / 0 | UU-Matter 63.2M | 1.2e+12 | 1 |
| REF TecTech Eye of Harmony structure (Crude generators) | 1820 | 15k | 60.0M | 469k | 14k | 469k | 8.6M | 1.5M | 0 | 0 / 0 / 0 / 0 / 498 |  | 5.3e+15 | 606 |
| REF Godforge main ring, Singularity Shielding casings only | 2800 | 2800 | 11.5M | 90k | 0 | 90k | 1.4M | 90k | 0 | 0 / 0 / 0 / 0 / 0 |  | 1.3e+13 | 6 |

How to read the proposed Eye T1 row:

- **Includes #5** (stabilisation pads cut from 294 to 48). Without it, the proposed Eye T1 is 1.3e16 EU, 1,424 h and
  1,502 UXV circuits, which is 2.4× TecTech's whole Eye in EU.
- **Gate fluid per tier step:** Eye T2's generators take 3.0M L Raw Star Matter (3 Sol runs), Eye T3's take 753k L
  ESPM (7.5 Sirius A runs), and Eye T4's take 376k L RHSP (7.5 Arcturus runs).

### 4.1 The Spatial / Temporal casings since ruling 11a (supersedes the Eye rows' Godforge column above)

The table above prices every Reinforced Spatial / Temporal Structure Casing through SL-P27/P28 (¼ Godforge casing, an
Assembler). Ruling 11a retired those: the casings now come only from RC-2's Condensate Assembler recipe, rewritten to
output ×32 (SL-B01/B02; SL-B03/B04 register the same recipe if a pack moved it). So for the Eye rows, read the Godforge
column (and add the BEC inputs) from this table instead. Casing counts are from the live shapes (`eye_core`: `U`
9,390, `Q` 4,704 after the D-49 sockets) plus the generators (1 casing each; a stabilisation generator consumes one
of each type), SL-E01 with its 6 Pylons (258 Spatial + 162 Temporal) and the band recipes (1/16 casing per Beam Band
or Jet Crescent). EU and hours are nominal: one Condensate Assembler, 40,000 s at `RECIPE_UMV` per craft of 32.

| Machine tier | Spatial + Temporal casings | BEC crafts | Godforge shield casings | Neutronium nanites (consumed) | Neutronium / Cosmic Neut. condensate (ingots each) | Bedrockium condensate | EU | BEC hours |
|---|---|---|---|---|---|---|---|---|
| Eye T1 (core + generators + controller) | 14,936 | 467 | **14,936** (was 3,740) | 22,404 | 239k | 119k | 4.7e16 | 5,186 |
| Eye T2 increment | 367 | 11.5 | **367** (was 92) | 550 | 5,870 | 2,935 | 1.2e15 | 127 |
| Eye T3 increment | 360 | 11.3 | **360** (was 90) | 540 | 5,763 | 2,882 | 1.1e15 | 125 |
| Eye T4 increment | 188 | 5.9 | **188** (was 47) | 282 | 3,008 | 1,504 | 5.9e14 | 65 |
| **Eye T4 complete** | 15,851 | 495 | **15,851** (was 3,969) ≈ 5.7 Godforge main rings | 23,777 | 254k | 127k | 5.0e16 | 5,504 |
| REF TecTech's Eye, stock RC-2 (1,430 casings) | 1,430 | 358 | 11,456 | 17,184 | 183k | 92k | 3.6e16 | 3,978 |

RC-2's own compression / dilation generator recipes also take Godforge casings directly; that is RC-2's price for
every Eye (TecTech's too) and is not in either table. **Verdict: fair.** The Eye T1 shell at ×32 costs about 1.3× TecTech's
stock RC-2 shell in Godforge casings and in BEC time, for a structure about 10× larger; it is 4× SL-P27/P28's
shielding bill (my "a bit too cheap") and 1/8 of stock ×4. The Eye now needs a working BEC network before T1,
as TecTech's does in RC-2. Every other column of §4 (bands, gates, circuits, gate fluids) is unchanged by 11a.

---

## 5. Comparison with GTNH multis at the same tier (jar counts)

| Multi | Tier | Structure (javap) | What makes it expensive |
|---|---|---|---|
| **Black Hole Compressor** | UIV | 4,863 blocks: 3,667 Extreme Density, 988 Background Radiation (+ hatches), 144 Naquadah Alloy frames, 64 Hawking glass | Per Extreme Density casing: 64 superdense Bedrockium + Steel + Iron (15.0M Bedrockium ingots in all). 63M L UU-Matter. Controller AL: SpaceTime 1,440 L, 4 UIV circuits, 4 FG UEV. No circuits in the casings. |
| **TecTech Eye of Harmony** (removed by us) | UMV, post-Godforge | 1,820 blocks: 896 Spatial, 534 Temporal, 36 Boundary, 138 compression, 168 dilation, 48 stabilisation generators (each type one uniform tier) | Its casings need Godforge shielding casings (14.7k in all = 5.2 Godforge main rings). Its controller needs **4 Godforge controllers**, 4 DTPFs, 16 FG UMV, and 144,000 L each of Time and Space. Generators are 80,000-ticks-per-tier UMV assembly lines with UXV circuits. |
| **Forge of the Gods** main / ring 2 / ring 3 | UIV-UMV | main 7,561 (Singularity Shielding 2,800, Magnetic Confinement 3,948, Celestial Matter Guidance 272, …), ring 2 +6,004 (2,012 shielding, 3,336 confinement, 357 Medial Graviton Modulators), ring 3 +6,286 (1,736, 3,726, 397 Central modulators) | One shielding casing is ¼ of an AL with 2.36M L molten Bedrockium, superdense Infinity/TM/CN/Netherite and Eternal Singularities: 4,096 Bedrockium ingots per casing. Only the shielding casings are modelled above, so the Godforge's real cost is higher. |
| **Nano Forge T4** (estimate) | UXV | tier-4 base 6,455 + tier-4 render piece 4,399: Magnetic Anchor 2,795, Nanite Shielding Glass 3,208, Loadbearing Distribution 871, Field Energy Absorber 943, Precision Field Sync 1,452, MagMatter frames 582, BDM 147, WDM 166 | Glass is ×4 per craft with 1 FG UMV, so about 800 FG UMV = 3,200 UXV circuits. Loadbearing is 1 UXV circuit + 6 MagMatter plates each. About 4,000 UXV circuits plus MagMatter, BDM and WDM. Magnetic Anchor's AL inputs were not extracted. |

Judgement per tier:

- **Kerr T1 vs BHC.** The Kerr already carries about 70 % of the BHC's Bedrockium and UU-Matter bill through the
  reused casings. It adds about 54k Infinity, 12k SpaceTime and roughly 1.6k UIV circuits after #6/#7, and it replaces
  the BHC (whose recipe we remove) while adding an extractor module and a T2. **Fair** at about 1.5× the BHC. Today's
  coil and quadrupole prices tip it to "heavy", mostly in SC UIV (41k ingots) and Super Coolant (47M L).
- **Kerr T2 vs Godforge ring 2.** 121k Neutronium, 45k Infinity and 5k TM for 14k blocks, plus 4M L Raw Star Matter
  and 128 UXV circuits in the upgrade. That is cheaper than one Godforge ring's shielding casings (1.0M Neutronium,
  8.2M Bedrockium). **Fair**: Neutronium is bulk at UMV, and the real gate is the Eye-T1 Raw Star Matter and the HSM
  it unlocks. Only #1 is needed.
- **Eye T1 vs TecTech's Eye.** Today it is 8× TecTech's Eye in Godforge casings and 4.3× in EU, so **too expensive**.
  After #3/#4/#5 it lands at TecTech parity in EU (5.5e15 vs 5.3e15), assembly-line hours (612 vs 606) and UXV
  circuits (844 vs 498, of which 326 are the controller). It was ¼ of TecTech in Godforge casings (3.7k) through
  SL-P27/P28; since ruling 11a the shell goes through RC-2's BEC at ×32 and is about TecTech's stock RC-2 shell (14.9k
  vs 11.5k Godforge casings, 4.7e16 vs 3.6e16 EU of BEC time, §4.1). Still no Godforge controllers, unlike TecTech's Eye.
- **Eye T2-T4 vs Godforge rings / Nano Forge T4.** After the fixes, each step is dominated by TecTech's own generator
  recipes: 3.9 / 5.8 / 7.7 × 10¹⁵ EU and 188 / 188 / 376 UXV circuits. The bands themselves are small (≤ 1e14 EU,
  ≤ 1.4k UMV circuits). The complete Eye T4 is 2.3e16 EU and 1.7k UXV circuits, against the Nano Forge T4's roughly
  4k UXV circuits plus MagMatter at a tier higher. **Fair.** Without #15 most of it is the stranded generator sets.

---

## 6. Proposed adjustments

Ordered by severity. "Gated" means the recipe registers only when `replaceTecTechEyeOfHarmony=true`. Items marked
**D-49** touch the Eye's tier ladder: they are crafting recipes, not the frozen star/tier code, but should be confirmed
against the D-49 redesign (B-18) before they are built.

1. **SL-K04 (Kerr Density Upgrade).** *Current:* `Black_Hole_Stabilizer` ×1. *Proposed:* `Black_Hole_Opener` (Black
   Hole Seed) ×32, everything else unchanged. *Why:* the Superstable Black Hole Seed has no recipe anywhere in the
   pack (all 242 mod jars scanned). Kerr T2, HSM and the whole Eye T4 chain are uncraftable today. Thirty-two seeds
   are 32 UHV Space Assembler crafts, a real but reachable cost. Giving GT's Superstable Seed a recipe instead would
   also unlock the BHC's superstable mode pack-wide (B-21).
2. **New SL-P30 / SL-P31 / SL-P32** (gated; **D-49**). These are alternate assembly-line recipes for TecTech's
   Spacetime Compression Field Generator Tier1 / Tier2 / Tier3 (metas 1-3, our Eye T2 / T3 / T4 gate).
   - *Current:* TecTech's recipe takes 2 / 8 / 32 White Dwarf Matter bolts.
   - *Proposed:* TecTech's own inputs, research item and duration, with the WDM bolts replaced by a 4th fluid, the
     previous tier's product: Raw Star Matter 16,000 L / ESPM 4,000 L / RHSP 2,000 L.
   - *Why:* with TecTech's Eye removed, WDM's only source is our Eye T4, so Eye T2-T4 are deadlocked. The fluids
     keep the "a chart needs the tier below" rule. Per tier step: 3 Sol runs, 7.5 Sirius A runs, 7.5 Arcturus runs.
   - *Note:* if B-3's dedicated tier-gate block replaces compression, this is moot (B-22).
3. **New SL-P27 / SL-P28** (gated). These are batch Assembler recipes for TecTech's Reinforced Spatial / Temporal
   Structure Casing.
   - *Current:* TecTech's AL only: 32 Godforge Singularity Shielding Casings plus about 1,600 ingots each of
     Neutronium and Cosmic Neutronium, for ×4.
   - *Proposed:* 1 Godforge Singularity Shielding Casing, 4 dense Neutronium, 4 dense Cosmic Neutronium, 1 dense
     Infinity and 576 L molten SpaceTime → ×4, UMV, 1,200 ticks.
   - *Why:* the Eye core uses 13,872 of them (TecTech's Eye: 1,430), which is 111k Godforge casings for the shell
     alone. The proposal keeps the exact TecTech blocks and look (my ruling) and keeps the Godforge gate, at
     ¼ casing per block. Alternatives are in B-23.
   - *Superseded (ruling 11a):* SL-P27/P28 are retired; the casings come from RC-2's BEC recipe at ×32 (§4.1, §9.5).
4. **New SL-P29** (gated). A batch recipe for TecTech's Infinite Spacetime Energy Boundary Casing.
   - *Current:* TecTech AL ×1: LSC, 16 Active Transformers, 4 UMV wireless hatches, 4,096 Neutronium + 4,096 Cosmic
     Neutronium.
   - *Proposed:* 1 `wireGt16` SuperconductorUMV, 8 dense Neutronium, 8 dense Cosmic Neutronium, 1 Emitter UIV and
     1,152 L molten SpaceTime → ×8, UMV, 1,200 ticks.
   - *Why:* the Eye uses 6,426 of them across T1-T4 (TecTech's Eye: 36): 6,426 LSCs and 103k Active Transformers.
5. **Eye structure `Y`** (stabilisation pads). This is not a recipe: it is an exporter legend change, and the
   structure owner should make it. *Current:* 294 Stabilisation Field Generators under the six pylon feet.
   *Proposed:* 48 (8 per pylon foot, the pad's centre ring; TecTech's Eye also uses 48), with the other 246 cells
   plinth casing (`Q`). *Why:* each stabilisation generator embeds a compression generator, a dilation generator and
   a Boundary casing. The 294 are placeholders with no effect yet (B-3), and cost 1,176 UXV circuits and 9.5e15 EU,
   1.8× TecTech's whole Eye (B-24).
6. **SL-P08 (Ergosphere Field Coil).** *Current:* `wireGt01` SuperconductorUIV ×64 and Super Coolant 32,000 L.
   *Proposed:* ×16 and 8,000 L. *Why:* a Kerr T1 holds 1,258 coils (842 placed, 416 inside quadrupoles), which is
   41k SC UIV ingots and 47M L Super Coolant. That is out of line with the BHC, which uses no superconductor in its
   casings. Proposed: 7.7k ingots and 13M L.
7. **SL-P09 (Beam Focusing Quadrupole).** *Current:* output ×1. *Proposed:* output ×4, inputs unchanged. *Why:* one
   quadrupole is 2 coils, 1 Sensor UIV and 2 Particle Beam Guidance Pipe Casings (60 Infinity, 1 UEV + ½ UIV circuit
   each). The 208 quadrupoles alone are 25k Infinity and 1,040 UIV circuits. This also cheapens SL-K03 (×4) and SL-P15.
8. **SL-P15 (Beam Band Casing).** *Current:* ×4 from 1 Spatial casing, 2 Quadrupoles, 2 Emitter UMV, 2 dense
   SpaceTime and 9,216 L SpaceTime. *Proposed:* ×16 from 1 Spatial casing, 1 Quadrupole, 2 Emitter UMV, 4 dense
   SpaceTime and 9,216 L SpaceTime. *Why:* the 2,862 blocks cost 715 TecTech Spatial casings, 1,431 Sensor UIV, 1,431
   Emitter UMV (12.9k UIV/UMV circuits) and 114M L Super Coolant. Proposed: 179 casings, 358 emitters, 1.4M L.
9. **SL-P16 (Jet Crescent Casing).** *Current:* ×4 from 1 Temporal casing, 2 Field Coils, 8 dense Infinity, 1
   Emitter UMV, 32 SpaceTime ingots and 16,000 L Raw Star Matter. *Proposed:* ×16 from the same items with 2 Emitter
   UMV and 64,000 L Raw Star Matter. That keeps 4,000 L per block, so the gate is still 11 Sol runs. *Why:* 2,755
   blocks cost 689 Temporal casings, 1,378 coils and 50k Infinity. Proposed: 173, 346 and 12.5k.
10. **SL-P17 (Galactic Shell Plating)** (**D-49**). *Current:* ×2 from 4 dense Infinity, 1 TM frame, 4 Cosmic
    Neutronium plates, 8 TM ingots and 1,000 L HSM. *Proposed:* ×16 from 8 dense Infinity, 2 TM frames, 8 Cosmic
    Neutronium plates, 16 TM ingots and 250 L HSM. *Why:* 5,325 blocks need 2.66M L HSM. That is 26.6M L RHSP, 532
    Arcturus runs, about 710 h of an Eye T3, while one Sirius B run eats only 5,000 L. Proposed: 83k L HSM, about 17
    Arcturus runs.
11. **SL-E02 (Galactic Upgrade Core)** (**D-49**). *Current:* HSM 256,000 L. *Proposed:* 32,000 L. *Why:* 256k L is
    51 Arcturus runs (about 68 h) on top of the shell. 32k L is about 6. D-49's own T4 feed (B-18 item 9) sets the
    running cost; the core should be a step, not a month.
12. **SL-E01 (Eye controller) and SL-P21 (Stabiliser Pylon).** *Current:* molten SpaceTime 1,179,648 L in SL-E01
    and 147,456 L in each of the 6 pylons. *Proposed:* 294,912 L and 36,864 L. *Why:* the controller chain is 33k
    SpaceTime ingots (about half of it inside the 160 Boundary casings), against TecTech's controller at about 2k ingots of exotic fluid (144,000 L each of Time and
    Space). With #3/#4 the pylons' TecTech casings get cheaper anyway. Proposed: about 4.8k ingots.
13. **SL-K01 (Kerr controller).** No change. 2.7k SpaceTime and 8 Black Hole Seeds is heavier than the BHC
    controller, but the Kerr replaces the BHC plus an extractor and the BHC's own catalyst is SpaceTime. Listed so
    the decision is explicit.
14. **SL-K03 / SL-P14.** *Current:* Electric Pump UIV ×32 / ×1. *Proposed:* Electric Piston UIV ×32 / ×1. *Why:*
    cost-neutral; the extractor module now runs item recipes (B-17, already suggested).
15. **New, optional: compression-generator upgrade recipes** (gated; **D-49**, B-3). *Current:* every Eye tier step
    crafts 188 new generators from scratch and strands the old set: 1.7e16 EU and about 1,900 AL hours for T2-T4.
    *Proposed:* an alternate recipe tier m → m+1 that consumes the tier-m generator in place of the Spatial casing,
    at 80,000 ticks flat. *Why:* this cuts the T2-T4 gate cost to about a third without touching TecTech's tiers.
    Decide together with the pending tier-gate block design (B-22).

Not changed, and judged fair: SL-P02/P03 (the mirror body, 7 Infinity per block), SL-P05..P07 (rails: 232 UIV robot
arms is about 3 h of assembly line), SL-P10, SL-P12, SL-P18, SL-P19, SL-K02, SL-K05 and the star charts SL-P22..P26.

---

## 7. Caveats

- **Component metals and AL time are not rolled up.** That covers the Robot Arms, Emitters, Sensors, Field Generators
  and Pumps, and the motors, TM rods and NetherStar cables inside them. The circuit counts are.
- **The Godforge reference counts only its Singularity Shielding casings.** Its Magnetic Confinement casings (3,948
  in the main ring), graviton modulators and controller are not in the model.
- **The Nano Forge T4 row is a partial extraction:** the counts are exact, and the Magnetic Anchor and Precision Field
  Sync recipes were not decoded.
- **No machine-hours include research** (scanner and research station).
- **TecTech's Eye reference assumes Crude generators.** A TecTech player who later upgrades compression to tier 4
  pays 138 × 320,000 ticks at UMV (5.6e15 EU) on top.
- **Bedrockium is reported raw.** In GTNH it is a bulk late-game metal; the BHC's 15M ingots are the pack's own
  number.

---

## 8. Applied (recipe audit pass, 2026-10-12)

Every uncraftable chain in §2 now has a recipe that can run: Kerr T2 (#1) and Eye T2-T4 (#2). The numbers in §1-§5
are the audit's and were not re-run. The one place the build differs from the "proposed" columns is #2/#15 (below).

**D-49 check.** None of the 15 changes the Eye's star (process) recipes, their outputs, the tier transition or
upgrade-install code, or the stability mechanics; all are crafting recipes or structure. The ones marked D-49 were
checked against the D-49 proposal (design repo `designs/docs/star-lifecycle.md`): compression stays the 1:1 gate
and tiers unlock in sequence (#2, #15), the Galactic Upgrade Core stays the HSM-gated T4 unlock (#11), and HSM still
gates T4 (#10 keeps HSM in the shell). So nothing was skipped for the D-49 hold.

| # | What | Status | Where / notes |
|---|---|---|---|
| 1 | SL-K04: 32 Black Hole Seeds, not the Superstable Seed | **Applied** | `BTSRecipesKerr` (`DENSITY_UPGRADE_SEEDS`) |
| 2 | Compression generator tiers 1-3 without WDM | **Applied, combined with #15; retired in the RC-2 port (§9.5, SL-P39 instead)** | SL-P30..P32 in `BTSRecipesSingularity.compressionGates()`, one table row per step. Each consumes the generator one tier below in place of the Spatial casing, 80,000 ticks flat, gate fluid Raw Star Matter 16,000 / ESPM 4,000 / RHSP 2,000 L. Research-station AL, researched from Star Chart Sirius A / Arcturus / Sirius B (TecTech's research items are taken by its own recipes). Gated. B-28. |
| 3 | SL-P27/P28 batch Spatial/Temporal casings | **Applied, with a change** | Programmed circuit 1 / 2 tells them apart (the proposal had identical inputs). Registered whatever the flag says: our Eye needs them either way. B-26. |
| 4 | SL-P29 batch Boundary casing | **Applied** | Registered whatever the flag says, as #3. B-26. |
| 5 | Eye `Y`: 48 stabilisation generators | **Applied** | Exporter legend; `eye_core` re-exported (`Y` 48, `Q` 4,704). |
| 6 | SL-P08 coil: 16 wires, 8,000 L | **Applied** | |
| 7 | SL-P09 quadrupole ×4 | **Applied** | |
| 8 | SL-P15 beam band ×16 | **Applied** | |
| 9 | SL-P16 jet crescent ×16, 64,000 L RSM | **Applied** | Still 4,000 L Raw Star Matter per block. |
| 10 | SL-P17 Galactic Shell ×16, 250 L HSM | **Applied** | |
| 11 | SL-E02 32,000 L HSM | **Applied** | |
| 12 | SL-E01 294,912 L, SL-P21 36,864 L SpaceTime | **Applied** | |
| 13 | SL-K01 unchanged | **Skipped (no change proposed); superseded by the RC-2 port (§9.2: re-priced at BHC parity)** | The audit's own verdict: heavier than the BHC controller, but the Kerr replaces the BHC and adds a module host. |
| 14 | SL-K03 / SL-P14 pistons, not pumps | **Applied** | |
| 15 | Compression generator upgrade recipe | **Applied inside #2** | Not a separate recipe: a second set would need three more unique research items. |

**What #2/#15 combined does to §4's proposed Eye rows** (approximate, from the per-generator figures there, not a
model re-run): each tier step is 188 upgrades of 80,000 ticks at `RECIPE_UMV`, about **1.9e15 EU and 209
assembly-line hours** per step, in place of 3.8 / 5.7 / 7.6e15 EU and 418 / 627 / 836 h. That takes the Eye T2 / T3 /
T4 increments to roughly 2.0e15 EU each, and the complete Eye T4 from 2.3e16 to about 1.2e16 EU. Each step also stops
consuming 188 Spatial casings (47 Godforge Singularity Shielding Casings through SL-P27). The gate fluids are the
same as §4 (3.0M L Raw Star Matter, 753k L ESPM, 376k L RHSP per step).

**Still open:** B-21..B-26 in the questions file record each of these as a suggestion you can still overrule. Only the
crafting side changed; nobody has crafted the chains in game yet.

---

## 9. GTNH 2.9.0-RC-2 port: the BHC→Kerr swap, the Kerr controller price, and the gate audit (2026-10-12)

Sources: GT 5.09.54.205 sources (`BECRecipes`, `BECMetaMaterialRecipes`, `NaniteChain`, `TranscendentPlasmaMixerRecipes`,
`ElectricImplosionCompressorRecipes`, `RecipeLoaderChemicalSkips`), coremod 2.9.84 disassembly (`AssemblingLineRecipes`,
`BECRecipes`), and a headless `runServer` of the ported addon. §1-§8 above are the beta-1 audit and were not re-run;
this section says what RC-2 changes. Questions: B-29 (gate), B-30 (Kerr price).

### 9.1 What moved in RC-2

- Every Eye of Harmony casing and field generator is now a **Condensate Assembler (BEC)** recipe; TecTech's
  research-station recipes for them are gone. One recipe per tier, from scratch (no tier-upgrade input).
- **Every Spacetime Compression Field Generator tier consumes 1 Black Hole Compressor controller**, and the coremod's
  Empty Spatially Compressed Circuit Array consumes 64. Every Stabilisation generator consumes a compression
  generator of its tier, so it carries a controller too.
- With `removeBlackHoleCompressorRecipe=true`, `BTSBlackHoleSwap` moves all of them onto the **Kerr Ergosphere Engine
  controller** (verified in `runServer`: 10 recipes, the 9 tiers and the circuit array).

### 9.2 Kerr controller vs BHC controller (SL-K01, re-priced)

| | BHC controller (coremod 2.9.84 AL) | Kerr controller, card price (beta-1 SL-K01) | **Kerr controller, RC-2 SL-K01** |
|---|---|---|---|
| Research | Universal Singularities singularity (meta 11), 3,600 t UEV | Black Hole Seed, 4 min UIV | Black Hole Seed, 4 min UIV |
| Compression lineage | HIP Compressor 1, Neutronium Compressor 1, Advanced Implosion 16, Compressor UIV 8, Electric Implosion 4 | none | **the same** |
| Circuits / field gens | 4 UIV / 4 FG UEV | 32 UIV + 16 FG, 16 Emitter, 16 Sensor (UIV) | 4 UIV / 4 FG UEV |
| Identity parts | 64 Laser Lens Special, 2 ZPM3, ProtoHalkonite ring 32 / rotor 16 / gear 8, superdense Kevlar 4 | 8 Mirror Segments, 16 Rail Mk I, 8 Seeds, 16 Field Coils, 32 ED casings, 64 TM screws | 1 Seed, 1 Mirror Segment, 4 Field Coils, 1 ED casing, 16 TM screws |
| Superdense plates | Neutronium 16, Naquadria 16, Cosmic Neutronium 8 | (dense SpaceTime 32, SpaceTime frames 32) | Neutronium 16, Naquadria 16, Cosmic Neutronium 8 |
| Fluids | SpaceTime 1,440, Oganesson 256,000, SC UIV base 43,200, Infinity 14,400 | SpaceTime 294,912, TM 147,456, Super Coolant 1,000,000 | SpaceTime 2,304, SC UIV base 43,200, Infinity 14,400, Super Coolant 64,000 |
| SpaceTime (ingots, incl. parts) | 10 | about 2,400 | about 35 |
| Time | 2,400 t at UIV | 18,000 t at UIV | 3,600 t at UIV |

Judgement: the RC-2 SL-K01 is about 1.1-1.3× a BHC controller. The swapped-out BHC parts (lenses, ProtoHalkonite,
Kevlar, Oganesson) and the swapped-in Kerr parts (a seed, a mirror segment of about 170 Infinity and 20 UIV-circuit
equivalents, 4 coils of 32 SC UIV ingots, one ED casing of 4,096 Bedrockium) are of the same order, and the
compression lineage, circuits, plates and three of four fluids are identical. The card price would have charged
about 2,400 SpaceTime ingots and 8 Black Hole Seeds **per generator**.

### 9.3 What the swap costs per generator, per Eye tier and per circuit array

| Item | Controllers | With the card SL-K01 | With the RC-2 SL-K01 | Stock RC-2 (BHC) for comparison |
|---|---|---|---|---|
| One compression generator (any tier) | 1 | 2,400 SpaceTime, 8 Seeds | 35 SpaceTime, 1 Seed | 10 SpaceTime |
| One stabilisation generator | 1 (inside its compression generator) | as above | as above | as above |
| Empty Spatially Compressed Circuit Array | 64 | 154k SpaceTime | 2.2k SpaceTime | 640 SpaceTime |
| **Eye T1** (188 `W` + 48 `Y` + SL-E01's Tier0 compression and stabilisation) | **238** | 571k SpaceTime | 8.3k SpaceTime, 238 HIP + 238 Neutronium Compressors | TecTech's Eye: 138 + 48 + 2 = **188** |
| Each later Eye tier step (188 new `W` generators of the next tier) | 188 | | 6.6k SpaceTime | TecTech's Eye upgrades 138 |
| Eye T1-T4 complete | 802 | | 28k SpaceTime, 802 of each compressor-lineage machine | |

The Eye's controller count is 1.27× TecTech's (its gate is 188 cells, not 138), so the compressor-lineage bill
scales with it. That is stock RC-2 economics on our structure; B-30 lists the levers if it is too much.

### 9.4 Gate audit: compression generator tier N must not need anything the Eye first makes at tier ≥ N

The Eye's first-product ladder (D-42/D-43): T1 Raw Star Matter, T2 ESPM, T3 RHSP (and HSM from Kerr T2), **T4 White
Dwarf Matter**, T5 Black Dwarf Matter (the interim `interimBlackDwarfAtEyeT4` path is gone with D-49), T6 Magnetar
Matter, T7 Ergomatter, T8 Superdense Ergomatter, T9 White Hole Matter, T10 Universium. Derived: White Dwarf nanites
(WDM + RSM) T4, Black Dwarf nanites T5, Universium / Eternity / MagMatter nanites and MHDCSM (Universium or Eternity
nanites + RSM) T10; Boundless Cosmic Solder, Primordial Matter and Excited DTSC need RSM (T1).

**RC-2's compression generator, tier index t (Eye tier N = t+1):** EOH Spatial casing, BHC (now Kerr), AE2 and AE2FC
singularity cells, a QFT manipulator (Cosmic Fabric t0-2, Infinity Infused t3-5, SpaceTime Continuum Ripper t6-8),
MetaMaterial Shielding I ×4(t+1), **Resonance Chamber** (I at t0; II ×1/4/16 at t1-3; III ×1/4/16 at t4-6; IV ×1/4
at t7-8), UXV circuits ×(t+1), Godforge Singularity Shielding casings, Hexanite frame/rod/superdense, dense plate of
Netherite / ProtoHalkonite / Six-Phased Copper / TM / Mellion / Creon / SpaceTime / Hexanite / Eternity, superdense
Cosmic Neutronium / Infinity / TM; condensates: TM (t0) or Boundless Cosmic Solder (t≥1), Space, SpaceTime,
Neutronium; nanite tiers up to Transcendent (t0), Six-Phased Copper (t1-3), White Dwarf (t4-6), Black Dwarf (t7-8).

| Eye tier (gate) | Compression tier | Eye products it needs (first tier) | Verdict |
|---|---|---|---|
| T1 | Crude (t0) | none (Resonance Chamber I: Tairitsu / Churitsu / Shijima / Emitter UHV; TM condensate; nanites ≤ Transcendent) | **clean** |
| T2 | Primitive (t1) | Boundless Cosmic Solder (RSM, T1) ✔; **Resonance Chamber II: 1 White Dwarf Matter foil (T4)** ✘ | **loop**, fixed by SL-P39 |
| T3 | Stable (t2) | as T2, 4 chambers ✘ | **loop**, fixed by SL-P39 |
| T4 | Advanced (t3) | as T2, 16 chambers: WDM is T4's own product ✘ | **loop**, fixed by SL-P39 |
| T5 | Superb (t4) | White Dwarf nanites (T4) ✔; **Resonance Chamber III: Black Dwarf Matter foil (T5)** ✘ | **loop**, fixed by SL-P33 (BDM-free Resonance Chamber III; `/btseye gates` CLEAN, B-59) |
| T6 (P4) | Exotic (t5) | BDM (T5) ✔, Creon ✔ | clean |
| T7 (P4) | Perfect (t6) | BDM ✔, SpaceTime ✔ | clean |
| T8 (P4) | Tipler (t7) | Black Dwarf nanites (T5) ✔; **Resonance Chamber IV: MHDCSM foil (T10), Eternity plate** | **loop**; P4/P5 (D-48 moves Eternity onto Ergomatter, T7; MHDCSM still needs Universium) |
| T9 (P5) | Gallifreyan (t8) | as T8, plus dense Eternity | **loop**; P5 |

The other families (not gates: the Eye accepts any tier of Time Dilation and Stabilisation, B-3):

- **Time Dilation** tier t: Temporal casing, Godforge Graviton Modulator II, Wave Focus (I at t0, **II at t1-3: WDM
  ring**, III at t4-6: BDM ring, IV at t7-8: MHDCSM ring), Energy Conduit I, Electrogravitic Valve I, Waveguide I.
  Tier 0 is clean.
- **Stabilisation** tier t: the same-tier Time Dilation and compression generators, a Boundary casing, Sensor Array
  (I at t0-1, **II at t2-4: WDM casing**, III, IV) and Field Manipulator (I at t0, **II at t1-3: WDM ring**, ...).
  Tier 0 is clean.
- **EOH casings** (Spatial, Temporal, Boundary): clean (Godforge casings, Hexanite, ProtoHalkonite, Shirabon, Neutronium
  nanites, LSC parts; Transcendent-tier nanites).

So for P1-P3 the only loop is the WDM foil in Resonance Chamber II. **SL-P39** is GT's recipe for that chamber with 2
SpaceTime foils instead of the foil (Condensate Assembler, same nanites, condensates and time; registered only with
`replaceTecTechEyeOfHarmony=true`). The chamber keeps its Boundless Cosmic Solder, so the generators still need Eye
T1's Raw Star Matter, and each chart (SL-P23..P25) still needs the tier below. Everything else in the T2-T4
generators is pre-Eye. UXV circuits are taken as pre-Eye because stock RC-2 already demands them for TecTech's Crude
generators, which the stock EOH needs before it can make anything.

### 9.5 SL-P27..P32 and SL-E01 against RC-2

| ID | RC-2 recipe for the same block | Decision |
|---|---|---|
| SL-P27 / P28 (Spatial / Temporal ×4) | BEC: 32 Godforge Shielding casings, 32 dense ProtoHalkonite, 8 Hexanite blocks, 48 Neutronium nanites, ... → ×4, 40,000 s | **Retired (ruling 11a) → SL-B01/B02:** RC-2's BEC recipe rewritten to ×32 at load-complete (1 Godforge casing per block; §4.1). SL-B03/B04 register that recipe at ×32 if a pack moved it, and the old SL-P27/P28 bodies return only if even that cannot be built (B-60). |
| SL-P29 (Boundary ×8) | BEC: LSC, Lapotronic unit, 16 Active Transformers, 4 UMV wireless hatches, ... → ×1 | **Kept.** Our Eye uses 6,426. |
| SL-P30..P32 (compression generator t-1 → t, research station) | BEC from scratch per tier, with a BHC/Kerr controller | **Retired.** The ruling keeps RC-2's generator costs; the old copy of TecTech's beta-1 inputs (Yotta cells, Infinity Chests) no longer matches anything in the pack, and `TTRecipeAdder` now charges the research EU/t they declared (`RECIPE_MAX`). SL-P39 is the replacement. |
| SL-E01 (our Eye controller, a GT assembly line, not `TTRecipeAdder`) | n/a | Re-audited: its Tier0 generators and 64 + 64 + 64 TecTech casings are clean and craftable; the Tier0 compression and stabilisation generators inside it now cost a Kerr controller each (2 in all). Its research (Stabiliser Pylon, scanner) is unaffected by the `TTRecipeAdder` change. |

**Lost with SL-P30..P32:** the "upgrade the old generator" path (#15 / B-28). Each Eye tier step re-crafts 188
generators from scratch, as stock RC-2 does for TecTech's Eye. B-29 lists the alternative (a BEC upgrade recipe).

---

## 10. D-49: Eye T5 and T6 (second arms, galactic rim, their gates, cores and charts; 2026-10-13)

Sources: `manifest.json` at HEAD (pieces `eye_arms2`, `eye_rim`), our recipes at HEAD (SL-P17, SL-P29, SL-P33,
SL-P35, SL-P37, SL-E05, SL-E06), and GT 5.09.54.205's `BECRecipes.addSpacetimeCompression` (the Condensate Assembler
recipe for every Spacetime Compression Field Generator tier). Same conventions as above: ingots, circuits rolled up,
EU = EU/t × ticks, machine-hours on one machine with no overclock or parallel. Computed by hand from the per-block
figures in §3.2 (the audit's model script is not kept); the arithmetic is in the questions file, B-45.

### 10.1 Completeness

| Tier | Block / item | Count | Recipe | Status |
|---|---|---|---|---|
| T5 | Galactic Shell Plating (`T`) | 5,065 | SL-P17 ASM ×16 (250 L HSM) | ✔ |
| T5 | Infinite Spacetime Energy Boundary (`J`) / Quantum Glass (`Z`) | 762 / 404 | SL-P29 ×8 / TecTech | ✔ |
| T5 | Compression generators, TecTech tier 5 (Superb, meta 4), all 188 `W` cells | 188 | RC-2 BEC: Resonance Chamber **III** ×1, White Dwarf nanites, UXV ×5, a Kerr controller | ✔ with **SL-P33** (the chamber with 2 WDM foils for GT's BDM foil: BDM is T5's own product, B-35) |
| T5 | Subspace Cooling Core (the First Cooling's core) | 1 | SL-E05 AL (WDM 147,456 L, DSSF 256,000 L) | ✔ (WDM: Eye T4) |
| T5 | Star Chart: Sirius B | 1 | SL-P35 (WDM 1,000 L) | ✔ |
| T6 | Galactic Shell Plating (`T` + the 4 `g` rim sockets) | 7,290 | SL-P17 | ✔ |
| T6 | Boundary / Quantum Glass | 3,159 / 753 | SL-P29 / TecTech | ✔ |
| T6 | Compression generators tier 6 (Exotic, meta 5) | 188 | RC-2 BEC: Resonance Chamber III ×4, Creon, UXV ×6, a Kerr controller | ✔ (SL-P33 again; nothing T6 or later in it) |
| T6 | Magnetar Seed Core (the Magnetar Merger's core) | 1 | SL-E06 AL (BDM 36,864 L, Agent 64,000 L) | ✔ (BDM: Eye T5) |
| T6 | Star Chart: SGR 1806-20 | 1 | SL-P37 (BDM 1,000 L) | ✔ |
| T6 | Antipolarization Agent (upkeep) | per cycle | `BTSRecipesStellar` chain | ✔ |

Gate audit (§9.4's rule): the tier-5 generator needs nothing first made at Eye T5 or later once SL-P33 replaces the
BDM foil; the tier-6 generator needs BDM (T5) and nothing from T6. Both clean.

### 10.2 Totals per tier step (increments on the tier below)

| Tier step | Blocks | Infinity | Transc. Metal | Neutronium | Cosmic Neut. | SpaceTime | Godforge shield casings | Circuits UIV / UXV | Main fluids | EU | Machine-hours |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Eye T5 band only (`eye_arms2`) | 6,231 | 22.8k | 6.3k | 6.9k | 9.4k | 762 | 0 | 381 / 0 | HSM 79k L | 4.6e13 | 5 |
| Eye T5 gate (188 tier-5 generators) | | 60k (superdense) | 60k | 18.8k (condensate) | 60k | 1.9k (condensate) | 3,760 | 0 / 940 | Boundless Cosmic Solder 7.5M L, 188 Kerr controllers, 188 Resonance Chamber III (376 WDM foils) | 2.4e15 | 261 |
| Eye T5 core + chart | | | | | | 1.5k | | 0 / 17 | WDM 148k L, DSSF 256k L | 2.4e13 | 0.7 |
| **Eye T5 increment** | **6,231** | **83k** | **66k** | **26k** | **70k** | **4.1k** | **3,760** | **381 / 957** | | **2.4e15** | **267** |
| Eye T6 band only (`eye_rim`) | 11,202 | 32.8k | 9.1k | 28.4k | 32.1k | 3.2k | 0 | 1,580 / 0 | HSM 114k L | 1.1e14 | 12 |
| Eye T6 gate (188 tier-6 generators) | | 72k | 72k | 22.6k | 72k | 1.9k | 4,512 | 0 / 1,128 | Boundless Cosmic Solder 9.4M L, 188 Kerr controllers, 752 Resonance Chamber III | 2.8e15 | 313 |
| Eye T6 core + chart | | | | | | 2.6k | | 0 / 33 | BDM 38k L, Agent 64k L | 6.0e13 | 0.8 |
| **Eye T6 increment** | **11,202** | **105k** | **81k** | **51k** | **104k** | **7.7k** | **4,512** | **1,580 / 1,161** | | **3.0e15** | **325** |
| REF one Eye T2-T4 step in RC-2 (188 generators of tier 1-3) | | | | | | | 1,504-3,008 | 0 / 376-752 | | **7.1e16-1.2e17** (BEC times 3-5M ticks) | 7.8k-13k |
| REF TecTech's EOH, compression tier 4 → 5 | 1,820 (whole EOH) | | | | | | 138 × 20 = 2,760 | 0 / 690 | | 1.7e15 | 192 |

How to read it:

- **The bands are cheap; the gates are the price.** The arms and the rim together cost about 1.5e14 EU and 193k L
  HSM, under 6 % of the two gate steps. That is the plan's §10.5 finding, and the TecTech parity rule (B-30): our gate
  is 188 cells where TecTech's is 138, so each step is 1.36× TecTech's own step to the same tier.
- **RC-2's own generator times are odd:** tiers 1-3 (our Eye T2-T4 gates) take 3, 4 and 5 million ticks per
  generator, tier 4 and 5 (our T5/T6 gates) 100,000 and 120,000. So T5 and T6 are each about 30× cheaper in EU and
  hours than the step to T2. This is stock GT data, not ours; B-45 logs it.
- **Running costs** (per parallel, `StarDef`): T5 takes an 18,432 L WDM charge per 2-hour batch, 3.9e13 EU a cycle
  (4.6e15 per batch), and DSSF trims (about 100,000 L a batch in the prototype's rig). T6 takes 3,000 L of
  Antipolarization Agent a cycle (the constant feed, B-37; it also charges the flare shield, 2·r A/s, B-60), 1.55e14 EU a
  cycle (9.3e15 an hour), Agent shield trims for the flares,
  and the merger charge (WDM 18,432 + BDM 4,608 + Agent 100,000 L) with WDM 1,800 + BDM 450 L top-ups every 50
  cycles.
- **Verdict: fair.** Every block and item has a recipe, nothing loops, and each step is in line with TecTech's own
  step to the same compression tier.
