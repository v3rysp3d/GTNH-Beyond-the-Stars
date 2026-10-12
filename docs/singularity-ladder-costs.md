# Singularity Ladder: recipe completeness and cost audit (Kerr T1/T2, Eye T1-T4)

Status: audit, 2026-10-12, addon `7a47289`. It reads the code and the jars and changes no code. **The recipe audit
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
| **Eye T1** (UMV, replaces TecTech's EOH) | **Far too expensive** | 13,872 TecTech Reinforced Spatial/Temporal casings (TecTech's Eye: 1,430) at 8 Godforge Singularity Shielding casings each, which is **118k Godforge casings = 42 Godforge main rings**. Plus 3,582 Infinite Spacetime Energy Boundary casings (TecTech's: 36), each 1 LSC + 16 Active Transformers. Plus 620 field generators (TecTech's: 354), 294 of them placeholders. **8× TecTech's whole Eye.** | Fair: about TecTech's crude Eye in EU, UXV circuits and assembly-line hours, and ¼ of it in Godforge casings (#3, #4, #5, #12) |
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
| `q` | Kerr Compressor Module (slCasing2:0) | 1 | SL-K02 AL | ✔ |
| `k` | Kerr Extractor Module (slCasing2:1) | 1 | SL-K03 AL | ✔ |
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
| Spatial / Temporal via SL-P27/P28 (#3) | 10.3 | 1 | 8 | 137 | 17 | ¼ Godforge casing (1,024 Bedrockium) | | 3.9e10 |
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
  circuits (844 vs 498, of which 326 are the controller). It is ¼ of TecTech in Godforge casings (3.7k = 1.3 Godforge
  main rings). That is right for the bottom tier of a ladder that, unlike TecTech's Eye, needs no Godforge controllers.
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
| 2 | Compression generator tiers 1-3 without WDM | **Applied, combined with #15** | SL-P30..P32 in `BTSRecipesSingularity.compressionGates()`, one table row per step. Each consumes the generator one tier below in place of the Spatial casing, 80,000 ticks flat, gate fluid Raw Star Matter 16,000 / ESPM 4,000 / RHSP 2,000 L. Research-station AL, researched from Star Chart Sirius A / Arcturus / Sirius B (TecTech's research items are taken by its own recipes). Gated. B-28. |
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
| 13 | SL-K01 unchanged | **Skipped (no change proposed)** | The audit's own verdict: heavier than the BHC controller, but the Kerr replaces the BHC and adds a module host. |
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
