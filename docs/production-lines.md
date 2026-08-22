# PRODUCTION LINES — the fuel, material, propellant and coolant chains (0.24.0 design of record)

**Status: BUILT.** Sections 0-9 are the design of record - the contract the implementing units
followed. **Section 10 is the as-built record**, read back out of the shipped Java afterwards, and
where the two disagree section 10 is right. Section 11 is the flowchart the ruling asked for, and
its verification. Every GT/GT++/GG/BW class, field and recipe map named below was read out of the
pinned jar (`GT5-Unofficial-5.09.52.594-dev.jar`, D-06), not remembered.

> **Release-pass status (0.24.0 final).** Everything §§0-9 asked for is built, and the twenty
> findings of the pre-release review are fixed — nine of them blocking. The Enrichment Annex
> re-point §8.3 marked REQUIRED **has landed** (§10.6). The three changes that moved a number a
> player can feel are recorded in **§10.8**: F-20 and F-21a grew water rungs, the eight depleted-fuel
> Centrifuge recipes came back, and decay-heat cooling now reads the same feedwater ladder the
> running plant does.

**My ruling, implemented here without relitigation:**

1. The purified-water grade drives **BOTH** a fuel-line YIELD term and a reactor OUTPUT term.
2. The existing 4-step chain (dust → pellet → rod → assembly) is **REPLACED ENTIRELY**.
3. The machines cover the plant's **own casings** (low tiers) **and** the propellant/coolant
   ladders (high tiers), staged by tier.
4. Implement it all, then produce a flowchart.

**Balance guard, discharged in §2.4:** the reactor OUTPUT term is the smaller of the two effects
by construction — the whole water ladder is worth **+40 % EU**, while the *smallest single rung*
of the fuel ladder is worth **+100 % EU**. Buying a water line can never substitute for the fuel
chain.

---

## 0. RECON — what actually exists today

### 0.1 `BTSRecipes.java` (486 lines, one file, 26 registration blocks)

Read in full. It registers exactly this, and nothing else:

| # | Recipe | Map | Tier |
|---|--------|-----|------|
| 1 | 1 dust Uranium + circuit 2 → **2 uraniumFuelPellet** | `assemblerRecipes` | HV |
| 2 | 4 uraniumFuelPellet + 2 plate Pt + 4 foil Pt → **fuelRodUranium** | `assemblerRecipes` | EV |
| 3 | 3 fuelRodUranium + 2 plate RhPdPalladium + 1 plate Ir + 2 ingot molten Pt → **fuelAssemblyUranium** | `assemblerRecipes` | IV |
| 4 | 4 plate StainlessSteel + 1 pipeMedium + 2 foil Pt → **2 reactorFuelColumn** | `assemblerRecipes` | EV |
| 5 | 1 dust Plutonium + 8 dust Uranium → **18 moxFuelPellet** | `mixerRecipes` | IV |
| 6 | 4 moxFuelPellet + Pt cladding → **fuelRodMox** | `assemblerRecipes` | IV |
| 7 | 3 fuelRodMox + hardware → **fuelAssemblyMox** | `assemblerRecipes` | IV |
| 8 | depletedFuelAssemblyUranium → 4 U + 2 small Pu + 1 Pb + 2 foil Pt (50 %) | `centrifugeRecipes` | EV |
| 9 | depletedFuelAssemblyMox → 3 U + 3 small Pu + 2 Pb + 2 foil Pt (50 %) | `centrifugeRecipes` | EV |
| 10 | 8,000 L nuclearWasteWater → small Tantalum + 7,200 L distilled water | `distillationTowerRecipes` | EV |
| 11 | 3 fuelRodUranium + 8 dust Thorium + 2 plate RhPdPd + molten Pt → **fuelAssemblyThoriumBonded** | `assemblerRecipes` | LuV |
| 12 | 3 fuelRodUranium + 8 dust NaquadahEnriched + 2 dust Uranium235 + 2 plate Ir + molten Naq → **fuelAssemblyNaqBonded** | `assemblerRecipes` | ZPM |
| 13 | depletedFuelAssemblyThoriumBonded → U + Th + small Pu + Pb | `centrifugeRecipes` | IV |
| 14 | depletedFuelAssemblyNaqBonded → U + Naq + small Pu + Pb | `centrifugeRecipes` | LuV |
| 15 | 1 fuelRodUranium + 3 dust Th + 1 plate RhPdPd + molten Pt → **fuelRodThoriumBonded** | `assemblerRecipes` | LuV |
| 16 | 1 fuelRodUranium + 3 dust NaqEnriched + 1 dust U235 + 1 plate Ir + molten Naq → **fuelRodNaqBonded** | `assemblerRecipes` | ZPM |
| 17–20 | depleted **ROD** reprocessing ×4 (U / MOX / Th-bonded / Naq-bonded) | `centrifugeRecipes` | EV–LuV |
| 21 | Turbine Housing | `assemblerRecipes` | IV |
| 22 | Rod Loading Port | `assemblerRecipes` | EV |
| 23 | Precision Injector | `assemblerRecipes` | IV |
| 24 | Spine Sensor | `assemblerRecipes` | EV |
| 25 | Silo Control Block / Turbine Control Block | `assemblerRecipes` | IV |
| 26 | Turbine Rotor | `assemblerRecipes` | IV |
| 27 | naqPropellant T1–T6 (6 recipes, one Mixer step each) | `mixerRecipes` | LuV–UIV |
| 28 | Extraction Platform / Enrichment Train / Reactor Core Mk1 controllers | `assemblerRecipes` | EV–IV |

Then `BTSDisplayRecipes.register()` fills the three DISPLAY-ONLY NEI pages.

**Nothing else exists.** There is no ore step, no leach, no conversion, no enrichment, no
sintering, no cladding. The "4-step chain" the ruling replaces is recipes 1–3 plus their MOX,
thorium and naquadah siblings — sixteen recipes in total (see §8).

### 0.2 `BTSItems.java` — 22 items, all of which SURVIVE

`uraniumFuelPellet`, `fuelRodUranium`, `fuelAssemblyUranium`, `depletedFuelAssemblyUranium`,
`moxFuelPellet`, `fuelRodMox`, `fuelAssemblyMox`, `depletedFuelAssemblyMox`,
`fuelAssemblyThoriumBonded`, `depletedFuelAssemblyThoriumBonded`, `fuelAssemblyNaqBonded`,
`depletedFuelAssemblyNaqBonded`, `fuelRodThoriumBonded`, `fuelRodNaqBonded`,
`depletedFuelRodUranium`, `depletedFuelRodMox`, `depletedFuelRodThoriumBonded`,
`depletedFuelRodNaqBonded`, `turbineFanMatched`, `turbineFanTDA`, `turbineFanTDAOptimized`,
`turbineRotor` (10 durability, wear on the item).

**Design constraint taken from this list:** the new chain must END at these same items. Every one
of the twenty-two keeps a route (§8). Nothing in a player's chest stops working.

### 0.3 `BTSFluids.java`

`nuclearWasteWater` + `naqPropellantT1..T6`. `nuclearWasteWater` already has a Distillation Tower
recipe to distilled water + tantalum (the D-09 profit anchor) but **no producer anywhere in the
recipe system**. The new reprocessing line (§1.6) becomes its source, which closes D-09.

### 0.4 `docs/steam-dynamics.md` — the ladders as they stand (D-08, do not change)

Propellant, stage factor P (all-8-timed):

| Rung | Fluid | P |
|---|---|---|
| — | Benzene | 1.25 |
| — | High Octane Gasoline | 1.5 |
| — | H8N4C2O4 Rocket Fuel | 2.0 |
| 1 | `BTSFluids.naqPropellantT1` | 3.0 |
| 2 | `naqPropellantT2` | 4.0 |
| 3 | `naqPropellantT3` | 5.0 (knee) |
| 4 | `naqPropellantT4` | 5.5 |
| 5 | `naqPropellantT5` | 6.0 |
| 6 | `naqPropellantT6` | 6.5 |

Coolant, quality Q: Water 1 · Distilled 1.5 · IC2 Coolant 4 · Super Coolant 12 · Liquid
Cryotheum 40 · Tachyon Rich Temporal Fluid 160.

Reactor arithmetic of record (`MTEReactorCoreMk1`):

```
ROD_MULT           = { 0, 1, 2, 5, 9 }        // empty, uranium, MOX, Th-bonded, Naq-bonded
STEAM_PER_COLUMN   = 16,384 L/t
euDemandOf(afterSpine, rpmMilli, rotorMilli, progMilli, upgradeMilli)
    = afterSpine * rpmMilli / 500_000
                 * rotorMilli               / 1000
                 * max(1000, progMilli)     / 1000
                 * max(1000, upgradeMilli)  / 1000
```

`drawFeedwater()` (line ≈4533) today accepts **Water or Distilled Water only**, drained out of
`feedwaterHatches` — the silo's own private list, never the Spiral's `chamberHatches`.

### 0.5 `questbook/gen-bom.ps1` + `BTSBom.java` — what the plant is really built from

`gen-bom.ps1` re-derives `BTSBom.java` from the structure shape code and throws if the copy on
disk disagrees; **the material lines below are sized against these numbers and no others.**

Whole-plant totals (`BTSBom.TOTAL[tier]`), the shopping list the material lines must serve:

| Piece | T1 | T2 | T3 | T4 | T5 |
|---|---|---|---|---|---|
| Light Concrete | 3,773 | 3,773 | 3,773 | 3,773 | 3,773 |
| Tungstensteel Turbine Casing | 830 | 822 | 814 | 806 | 798 |
| Radiation Proof Machine Casing | 520 | 812 | 105 | 129 | 141 |
| Europium Reinf. Radiation Proof | — | — | 963 | 1,343 | — |
| Radiant Naquadah Alloy Casing | — | — | — | — | 1,667 |
| Reactor Fuel Column *(ours)* | 360 | 616 | 872 | 1,256 | 1,608 |
| Solid Steel Machine Casing | 509 | 509 | 509 | 509 | 509 |
| Clean Stainless Machine Casing | 452 | 452 | 452 | 452 | 452 |
| Tungstensteel Reinforced Block | 362 | 442 | 249 | 249 | 249 |
| Iridium Reinforced Block | — | — | 257 | 381 | 497 |
| Steel Frame Box | 170 | 170 | 170 | 170 | 170 |
| Filter Machine Casing | 152 | 152 | 152 | 152 | 152 |
| Rod Loading Port *(ours)* | 45 | 77 | 109 | 157 | 201 |
| Steel Pipe Casing | 32 | 32 | 32 | 32 | 32 |
| Turbine Housing *(ours)* | 12 | 12 | 12 | 12 | 12 |
| Precision Injector *(ours)* | 8 | 8 | 8 | 8 | 8 |

Plus 4 Dynamo IV, 4 Energy IV, 4 Input Hatch IV, 4 Laser Source, 3 Maintenance, 3 Output Bus,
2 Input Bus, 1 Output Hatch, 1 Silo Control Block, 1 Spine Sensor, 1 Turbine Control Block.

**A T5 buildout is ≈ 8,000 blocks.** That single number is why the Site Batching Plant (§5.4)
earns its place.

### 0.6 Recipe maps an addon may register into — VERIFIED

`gregtech.api.recipe.RecipeMap` is `public final class … implements
gregtech.api.interfaces.IRecipeMap`, and `GTRecipeBuilder.addTo(IRecipeMap)` is public. Every map
below is a `public static final RecipeMap` on a class that ships inside the pinned
`GT5-Unofficial` jar, i.e. already on this addon's compile classpath (the codebase already
imports `bartworks.system.material.WerkstoffLoader` and `goodgenerator.items.GGMaterial`).

**Maps this design uses, per step:**

| Map (verified field) | Machine | Used by |
|---|---|---|
| `GTPPRecipeMaps.millingRecipes` | Ore Milling Plant (IsaMill) | F-01, F-19, F-21b, P-02 |
| `GTPPRecipeMaps.flotationCellRecipes` | Flotation Cell | F-02 |
| `LanthanidesRecipeMaps.digesterRecipes` | Digester | F-03 |
| `GTPPRecipeMaps.chemicalPlantRecipes` | Chemical Plant | F-04..F-06, F-09, F-10, F-12, F-20, P-01, P-03, C-02, C-03 |
| `GTRecipeConstants.UniversalChemical` | Chemical Reactor + LCR | F-07, F-16b, R-04, C-04 |
| `GTPPRecipeMaps.chemicalDehydratorRecipes` | Dehydrator | F-08, F-21a |
| `GTPPRecipeMaps.vacuumFurnaceRecipes` | Vacuum Furnace | F-14 |
| `RecipeMaps.centrifugeRecipes` | Industrial Centrifuge | byproduct cleanup |
| `RecipeMaps.mixerRecipes` | Mixer | F-02a, C-01 |
| `RecipeMaps.distillationTowerRecipes` | Distillation Tower | F-04b, existing waste-water recipe |
| `RecipeMaps.formingPressRecipes` | Forming Press | F-13, M-02d |
| `RecipeMaps.latheRecipes` | Lathe | F-15 |
| `RecipeMaps.extruderRecipes` | Extruder | F-16 |
| `RecipeMaps.assemblerRecipes` | Assembler | the plant's own blocks |
| `GTRecipeConstants.AssemblyLine` | Assembly Line | F-18n |
| `GoodGeneratorRecipeMaps.preciseAssemblerRecipes` | Precise Assembler | F-23 |
| `GoodGeneratorRecipeMaps.neutronActivatorRecipes` | Neutron Activator | F-11c |
| `RecipeMaps.isotopeDecay` | Decay Warehouse (`MTEDecayWarehouse`) | R-01 |
| `RecipeMaps.vacuumFreezerRecipes` | Vacuum Freezer | C-03 |
| `GTPPRecipeMaps.quantumForceTransformerRecipes` | Quantum Force Transformer | F-22, P-03h |
| `RecipeMaps.fluidSolidifierRecipes` | Fluid Solidifier | P-04 |

Useful metadata keys confirmed on `GTRecipeConstants`: `CHEMPLANT_CASING_TIER`, `COIL_HEAT`,
`HALF_LIFE`, `DECAY_TYPE`, `QFT_FOCUS_TIER`, `QFT_CATALYST`, `PRECISE_ASSEMBLER_CASING_TIER`,
`NKE_RANGE`, `RESEARCH_ITEM`, `DISSOLUTION_TANK_RATIO`, `SPARGE_MAX_BYPRODUCT`, `MASS`,
`ADDITIVE_AMOUNT`. There is **no** `RESEARCH_TIME` key — the Assembly Line research recipe carries
its own `duration()`/`eut()`; mirror an existing GT registration rather than guessing.

### 0.7 The water ladder is real and already in the pack — VERIFIED

`gregtech.api.enums.Materials` carries `Grade1PurifiedWater` … `Grade8PurifiedWater` (the
nine-unit Purification Plant chain: Clarifier, Ozonation, Flocculation, PH Neutralization,
Temperature Fluctuation, Laser Purification, Residual Degasser, Baryonic Perfection).

**The helper the implementation must use, rather than a hand-rolled comparison:**

```java
gregtech.common.tileentities.machines.multi.purification.PurifiedWaterHelpers
    public static int       getWaterTier(FluidStack);   // 0 for null/unknown, 1..8 for the grades
    public static Materials getPurifiedWaterTier(int);  // 1..8 -> the Materials constant; throws otherwise
```

Read out of the bytecode: `getWaterTier` returns **0** for `null` *and* for anything that is not
one of the eight grades — so plain water and distilled water both return 0 and must be told apart
by an explicit `isFluidEqual(GTModHandler.getDistilledWater(1))` test **before** calling it.

### 0.8 COLLISIONS FOUND — what GTNH already has, and how we sit beside it

We boost, never replace. Five real collisions were found; each is handled:

| # | What already exists | Where | Our stance |
|---|---|---|---|
| C1 | **Uraninite → Uranium metal.** `9 dust Uraninite + 4 dust Aluminium → 3 dust Uranium + 10 dust Aluminiumoxide` and `3 Uraninite + 2 Magnesium → 1 Uranium + 4 Magnesia`, both LV, both on `UniversalChemical` (`gregtech.loaders.postload.recipes.ChemicalRecipes`, bytecode offsets 3350 / 3451). | GT core | **Untouched.** That is metallothermic reduction to *metal*; our line goes to *ceramic oxide fuel*. Both consume Uraninite; neither is removed. |
| C2 | **The whole LFTR molten-salt chain.** `MaterialsFluorides.URANIUM_TETRAFLUORIDE` / `URANIUM_HEXAFLUORIDE` / `THORIUM_TETRAFLUORIDE`, `MaterialsNuclides.LiFBeF2ThF4UF4` etc., produced by `RecipeLoaderNuclearFuelProcessing` through the Reactor Processing Unit, Chemical Plant, Cold Trap, Nuclear Fuel Processing and Sparge Tower. | GT++ | **We JOIN it.** F-09/F-10 add a *second* production route to the SAME `URANIUM_TETRAFLUORIDE` / `URANIUM_HEXAFLUORIDE` materials, from ore instead of from metal. No GT++ recipe is edited or removed; a player with a standing LFTR line can feed its UF₆ straight into our cascade. The single biggest "boost, don't duplicate" decision in the document. |
| C3 | **BartWorks THTR pebble-bed fuel** — `MTEThoriumHighTempReactor`, TRISO pebbles/balls for uranium and thorium, burned-out forms. | BartWorks | **Untouched, and deliberately not imitated.** TRISO is a *pebble* fuel; ours is a *rod* fuel. Different physics, no shared item. |
| C4 | **GoodGenerator's liquid nuclear fuels** — `graphiteUraniumMixture`, `uraniumBasedLiquidFuel`, `uraniumCarbideThoriumMixture`, `plutoniumOxideUraniumMixture`, `thoriumNitrate` / `thoriumOxalate` / `thoriumHydroxide` / `thoriumTetrafluoride`, and the whole `naquadahBasedFuelMkI..MkVI` family. | GoodGenerator | **We EXTEND the thorium half.** F-21a calcines GG's existing `thoriumOxalate` into ThO₂ — a new consumer for a material that currently has one. The naquadah fuels stay the propellant line's feedstock exactly as today. |
| C5 | **`GGMaterial.zircaloy4` / `zircaloy2`** already exist with recipes. | GoodGenerator | **We CONSUME them.** F-16 extrudes GG's Zircaloy-4 into our cladding tube. Zircaloy-4 is the real PWR cladding alloy; the pack already makes it; we give it a second, large customer. |

**Checked and NOT found anywhere in the pack** (so these intermediates are genuinely new, no
collision): *yellowcake*, *uranyl*, *ammonium diuranate*, *pregnant leach solution*, *loaded
organic*, *tributyl phosphate*, *enriched/depleted UF₆*, *ceramic-grade UO₂ powder*, *green
pellet*, *sintered pellet*.

**Reagents confirmed present, so nothing below needs a new chemical chain:**
`Materials.SulfuricAcid`, `NitricAcid`, `HydrofluoricAcid`, `Fluorine`, `Ammonia`, `SodaAsh`,
`Quicklime`, `IronIIIChloride`, `PhosphoricAcid`, `Hydrogen`, `Helium`, `Thorium`,
`NaquadahEnriched`, `Uranium235`, `DinitrogenTetroxide`, `Uraninite`, `Pitchblende`;
`GTPPFluids.Kerosene`, `GTPPFluids.Butanol`, `GTPPFluids.HydrogenPeroxide`,
`GTPPFluids.BoricAcid`, `GTPPFluids.PineOil`, `GTPPFluids.IndustrialStrengthHydrofluoricAcid`;
`WerkstoffLoader.Zirconium`, `WerkstoffLoader.AmmoniumChloride`, `WerkstoffLoader.Thorianit`,
`WerkstoffLoader.CalciumChloride`.

**Confirmed ABSENT where you would expect them** (grep before you type any of these):
`Materials.Kerosene`, `Materials.HydrogenPeroxide`, `Materials.Zirconium`, `Materials.Hafnium`,
`Materials.Yellowcake`, `GTRecipeConstants.RESEARCH_TIME`.

---

## 1. THE FUEL LINE — 23 steps, ore to bundle

Real industrial process throughout the base tiers: comminution → flotation → acid leach → ion
exchange → solvent extraction → stripping → ADU precipitation → calcining → hydrofluorination →
fluorination → enrichment → reconversion → pelletising → sintering → grinding → cladding → rod
loading → bundle assembly. The UHV branch (F-22) is deliberately impossible but shaped like
chemical vapour infiltration, which is a real ceramic-composite process.

**Notation.** Items in `code` are existing registry entries; **bold** names are new BTS items or
fluids created by this design (§7 says who registers each). Tier is the recipe's `TierEU`.

### 1.1 Front end — ore to yellowcake (F-01 … F-08)

| Step | Name | Machine (map) | Tier | In | Out | Byproduct |
|---|---|---|---|---|---|---|
| **F-01** | Comminution (wet attrition milling) | Ore Milling Plant · `millingRecipes` | HV | 16 dust `Uraninite` *(or 12 dust `Pitchblende`)* + Milling Ball + 1,000 L Water | 16 **Milled Uraninite** | 2 **Gangue Dust** |
| **F-02a** | Frother preparation | Mixer · `mixerRecipes` | MV | 1,000 L `GTPPFluids.PineOil` + 2 dust `SodaAsh` + circuit 4 | 1 **Uraninite Flotation Froth Cell** | — |
| **F-02** | Froth flotation | Flotation Cell · `flotationCellRecipes` | EV | 16 **Milled Uraninite** + 1 **Froth Cell** + 8,000 L Water | 12 **Uraninite Concentrate** | 4 **Flotation Tailings** |
| **F-03** | Agitated sulfuric leach | Digester · `digesterRecipes` | EV | 12 **Uraninite Concentrate** + 6,000 L `SulfuricAcid` + 1,000 L `IronIIIChloride` *(Fe³⁺ oxidant)* | 8,000 L **Pregnant Leach Solution** | 3 **Leach Residue** |
| **F-04** | Clarification + ion exchange | Chemical Plant t2 · `chemicalPlantRecipes` | EV | 8,000 L **PLS** + 1 **Ion Exchange Resin** *(90 % returned)* | 6,000 L **Clarified Uranyl Liquor** | 2,000 L **Raffinate** |
| **F-04b** | Acid recovery | Distillation Tower · `distillationTowerRecipes` | HV | 2,000 L **Raffinate** | 1,200 L `DilutedSulfuricAcid` | 1 dust `Calcite` |
| **F-05a** | Solvent synthesis (tributyl phosphate) | Chemical Plant t2 · `chemicalPlantRecipes` | EV | 3,000 L `GTPPFluids.Butanol` + 1,000 L `PhosphoricAcid` + 4,000 L `GTPPFluids.Kerosene` | 8,000 L **TBP/Kerosene Solvent** | 1,000 L Water |
| **F-05** | Solvent extraction | Chemical Plant t3 · `chemicalPlantRecipes` | IV | 6,000 L **Clarified Uranyl Liquor** + 2,000 L **TBP/Kerosene Solvent** | 4,000 L **Loaded Organic** | 2,000 L **Raffinate** |
| **F-06** | Stripping + solvent recovery | Chemical Plant t3 · `chemicalPlantRecipes` | IV | 4,000 L **Loaded Organic** + 4 dust `SodaAsh` + 2,000 L Water | 3,000 L **Uranyl Strip Liquor** + **1,800 L TBP/Kerosene Solvent at 90 % chance — the counter-current recycle** | — |
| **F-07** | ADU precipitation | Large Chemical Reactor · `UniversalChemical` | HV | 3,000 L **Uranyl Strip Liquor** + 1,000 L `Ammonia` | 6 **Ammonium Diuranate** | 1,000 L **Ammonium Sulfate Liquor** *(a real ADU-plant co-product)* |
| **F-08** | Calcining to yellowcake (800 °C) | Dehydrator · `chemicalDehydratorRecipes` | EV | 6 **Ammonium Diuranate** | 4 **Yellowcake (U₃O₈)** | 500 L `Ammonia` recovered + 500 L `Steam` |

### 1.2 Conversion and enrichment (F-09 … F-11)

| Step | Name | Machine | Tier | In | Out | Byproduct |
|---|---|---|---|---|---|---|
| **F-09** | Hydrofluorination — "green salt" | Chemical Plant t3 | IV | 4 **Yellowcake** + 4,000 L `HydrofluoricAcid` + 1,000 L `Hydrogen` | **6 dust `MaterialsFluorides.URANIUM_TETRAFLUORIDE`** *(existing GT++ material — the C2 join point)* | 2,000 L `Steam` |
| **F-10** | Fluorination to UF₆ | Chemical Plant t3 | IV | 6 dust `URANIUM_TETRAFLUORIDE` + 2,000 L `Fluorine` | **6,000 L `URANIUM_HEXAFLUORIDE`** *(existing GT++ material)* | — |
| **F-11** | **Gas centrifuge cascade** | **Cascade Hall (NEW, §5.1)** · `btsCascadeRecipes` | IV | 6,000 L `URANIUM_HEXAFLUORIDE` + circuit 1 | 1,000 L **Enriched UF₆ (3.2 %)** + 5,000 L **Depleted UF₆ (tails)** | — |
| **F-11b** | HEU branch (for the naq grade) | Cascade Hall, circuit 2 | LuV | 1,000 L **Enriched UF₆** | 200 L **Highly Enriched UF₆** + 800 L **Depleted UF₆** | — |
| **F-11c** | DU metal (shielding) | Neutron Activator · `neutronActivatorRecipes`, `NKE_RANGE` 20–40 MeV | LuV | 5,000 L **Depleted UF₆** + 8 dust `Calcium` | 4 ingot **Depleted Uranium** | 8 dust `WerkstoffLoader.CalciumChloride` |

### 1.3 **F-12 — RECONVERSION. THE WATER STEP.** (arithmetic in §2)

Integrated Dry Route: `UF₆ + 2 H₂O + H₂ → UO₂ + 4 HF`. Chemically exact, and the *water is
literally the reagent* — which is why the ruling's water term belongs here and nowhere else.

Machine: **Chemical Plant, casing tier 3** · `chemicalPlantRecipes` · IV.

**Nine parallel recipes, one per water rung:**

```
in : 1,000 L Enriched UF6
   + 2,000 L Hydrogen
   + 2,000 L  <one of: distilled water, Grade1PurifiedWater .. Grade8PurifiedWater>
out: N x Ceramic-Grade UO2 Powder     where N = round(4 * Y(grade))   -- table in S2.1
   + 4,000 L HydrofluoricAcid  (recovered; feeds F-09 straight back)
```

Nine NEI entries side by side is exactly how GTNH already teaches this ladder (the Purification
Plant does the same), so the mechanic is discoverable without a bespoke GUI. **No new machine is
spent on the water term** — see §5.5 for why that was the right call.

### 1.4 Ceramics and fabrication (F-13 … F-18)

| Step | Name | Machine | Tier | In | Out |
|---|---|---|---|---|---|
| **F-13** | Cold pressing (green pellets) | Forming Press · `formingPressRecipes` | MV | 4 **Ceramic-Grade UO₂ Powder** + 250 L `Lubricant` *(zinc-stearate proxy)* | 4 **Green Pellet** |
| **F-14** | Sintering, H₂ atmosphere | Vacuum Furnace · `vacuumFurnaceRecipes`, `COIL_HEAT` 1,800 K | IV | 4 **Green Pellet** + 1,000 L `Hydrogen` | 4 **Sintered UO₂ Pellet** |
| **F-15** | Centreless grinding | Lathe · `latheRecipes` | MV | 1 **Sintered UO₂ Pellet** | **1 `uraniumFuelPellet`** *(THE EXISTING ITEM)* + 1 dustTiny **UO₂ Grinder Sludge** *(→ back into F-13)* |
| **F-16** | Cladding tube (pilgering) | Extruder · `extruderRecipes`, shape Pipe Tiny | EV | 2 ingot `GGMaterial.zircaloy4` | 1 **Zircaloy-4 Cladding Tube** |
| **F-16b** | End plugs + plenum spring | LCR · `UniversalChemical` | EV | 1 stick + 1 spring `StainlessSteel` + 1 plate `WerkstoffLoader.RhodiumPlatedPalladium` | 1 **Rod Hardware Set** |
| **F-17** | Rod loading, welding, He pressurisation | **Fuel Fabrication Hall (NEW, §5.2)** | IV | **4 `uraniumFuelPellet`** + 1 **Cladding Tube** + 1 **Rod Hardware Set** + 100 L `Helium` | **1 `fuelRodUranium`** |
| **F-18** | Bundle assembly (grids + tie plates) | Fuel Fabrication Hall | IV | 3 `fuelRodUranium` + 2 **Grid Spacer** + 1 plate `Iridium` + 2 ingot molten `Platinum` | **1 `fuelAssemblyUranium`** |

**Migration property, deliberate:** F-17 takes **4 pellets per rod** and F-18 **3 rods per
assembly** — the exact ratios of the retired recipes 2 and 3. Every pellet, rod and assembly
already in a player's chest keeps precisely the value it had yesterday. Only the *route* changes.

### 1.5 The other three fuel grades

**MOX (F=2) — the enrichment tails become fuel. Real (MIMAS process), and it closes the loop.**

| Step | Machine | Tier | In | Out |
|---|---|---|---|---|
| **F-20** | Chemical Plant t3 | IV | 2,000 L **Depleted UF₆** + 4,000 L `Hydrogen` + 4,000 L **of one water rung** | **N** **Depleted UO₂ Powder**, `N = 8 × Y` → 4 … 36 *(as-built; the "flat, no water key" of §2.3 was overturned — §10.8.1)* |
| **F-19** | Ore Milling Plant · `millingRecipes` | LuV | 2 **PuO₂** + 30 **Depleted UO₂ Powder** + Milling Ball | 32 **MOX Powder** *(6.25 % Pu — the real MIMAS masterblend ratio)* |
| | then F-13 → F-14 → F-15 (circuit 2) → **32 `moxFuelPellet`** → F-17 / F-18 (circuit 2) → `fuelRodMox`, `fuelAssemblyMox` | | | |

**Thorium-bonded (F=5) — (Th,U)O₂, extending GoodGenerator's existing thorium chemistry.**

| Step | Machine | Tier | In | Out |
|---|---|---|---|---|
| **F-21a** | Dehydrator · `chemicalDehydratorRecipes` | IV | 8 **`GGMaterial.thoriumOxalate`** *(existing GG material, C4)* + circuit 1 + 2,000 L **of one water rung** *(the oxalate cake is washed before it is fired)* | **N** **ThO₂ Powder**, `N = 6 × Y` → 3 … 27, + 2,000 L `CarbonDioxide` *(as-built — §10.8.1)* |
| **F-21b** | Ore Milling Plant | LuV | 30 **ThO₂ Powder** + 2 **Ceramic-Grade UO₂ Powder** + Milling Ball | 32 **(Th,U)O₂ Powder** *(6.25 % fissile driver)* |
| | then F-13 → F-14 → F-15 (circuit 3) → **Thorium-Bonded Pellet** → F-17 / F-18 (circuit 3) → `fuelRodThoriumBonded`, `fuelAssemblyThoriumBonded` | | | |

**Naquadah-bonded (F=9) — the UHV step: "unrealistic but big", shaped like chemical vapour
infiltration of a ceramic matrix.**

| Step | Machine | Tier | In | Out |
|---|---|---|---|---|
| **F-22** | Quantum Force Transformer · `quantumForceTransformerRecipes`, `QFT_FOCUS_TIER` 2 | UHV | 8 **Sintered UO₂ Pellet** + 4 dust `NaquadahEnriched` + 2 dust `Uranium235` + 200 L **Highly Enriched UF₆** + 2 ingot molten `Naquadah` + QFT catalyst | 8 **Naquadah-Bonded Pellet** |
| **F-23** | Precise Assembler · `preciseAssemblerRecipes`, casing tier 2 | UV | 4 **Naquadah-Bonded Pellet** + 1 **Cladding Tube** + 1 **Rod Hardware Set** + 100 L `Helium` | **1 `fuelRodNaqBonded`** |
| **F-18n** | Assembly Line · `GTRecipeConstants.AssemblyLine` | UV | 3 `fuelRodNaqBonded` + 2 **Grid Spacer** + 2 plate `Iridium` + molten `Naquadah` | **1 `fuelAssemblyNaqBonded`** |

### 1.6 Reprocessing — PUREX, and the D-09 closure

The four "depleted assembly → centrifuge" and four "depleted rod → centrifuge" recipes were
specified as retired here. **They were restored before release** (§10.8.2): PUREX is the better
head-end, not the only one, and an updating world's centrifuge array keeps running.

| Step | Name | Machine | Tier | In | Out |
|---|---|---|---|---|---|
| **R-01** | Spent fuel cooling pond | Decay Warehouse · `RecipeMaps.isotopeDecay`, `HALF_LIFE` / `DECAY_TYPE` metadata | EV | 1 `depletedFuelAssemblyUranium` *(and each of the other three)* | 1 **Cooled Depleted Assembly (U / MOX / Th / Naq)** |
| **R-02** | Chop-leach dissolution | **PUREX Canyon (NEW, §5.3)** | IV | 1 **Cooled Depleted Assembly** + 4,000 L `NitricAcid` | 6,000 L **Dissolver Liquor** + 2 **Cladding Hull** |
| **R-02h** | Chop-leach, **UNCOOLED** | PUREX Canyon | IV | 1 `depletedFuelAssemblyUranium` straight from the reactor + 6,000 L `NitricAcid` | 4,000 L **Dissolver Liquor** + 2 **Cladding Hull**, and **no plutonium downstream** |
| **R-03** | U/Pu co-extraction and partition | PUREX Canyon | IV | 6,000 L **Dissolver Liquor** + 2,000 L **TBP/Kerosene Solvent** | 4 **Ceramic-Grade UO₂ Powder** + 1 **PuO₂** + 1 dust `Lead` + **2,000 L `BTSFluids.nuclearWasteWater`** |
| **R-04** | Hull recovery | LCR · `UniversalChemical` | EV | 4 **Cladding Hull** + 1,000 L `HydrofluoricAcid` | 1 ingot `GGMaterial.zircaloy4` + 1 dust `WerkstoffLoader.Zirconium` |

**R-03's raffinate is `BTSFluids.nuclearWasteWater`** — the fluid whose Distillation Tower recipe
(→ distilled water + tantalum) already exists and is the D-09 profit anchor.

*As-built correction:* that fluid was **never** without a producer. `MTEReactorCoreMk1.emitWasteWater()`
puts one litre per live pillar into the silo's Output Hatch on **every generating tick** — about
201 L/t on a full tier-5 plant — and has done since before 0.20.0. What R-02h and R-03 add is a
**controllable** source: 2,000 L a batch, on demand, out of fuel you already burned, instead of an
unthrottleable flood you only get while the plant runs. That is what makes D-09 a closed loop
rather than a firehose.

The **cooled vs uncooled** split is the mechanic that earns the PUREX Canyon its slot: cool your
fuel in a Decay Warehouse and you get plutonium (and therefore MOX); rush it and you get uranium
and waste only. Nothing else in the pack expresses "how long has this sat".

Rod-scale variants of R-02 / R-03 exist at one third the numbers, for the four `depletedFuelRod*`
items (the SD-1 rod cascade drops rods, not assemblies).

---

## 2. THE WATER GRADE — where it enters, and the exact arithmetic

### 2.1 The YIELD term (fuel line) — ruled ladder

> As designed this section applied the ladder at **F-12 only**. As built it also runs at **F-20**
> (base 8) and **F-21a** (base 6), because F-12 alone left the balance guard inverted on the MOX and
> thorium grades — §2.3's retraction and §10.8.1. The Y column below is the ruling and is unchanged;
> only the number of steps that multiply by it changed.

The ruling: distilled ×0.5, Grade 1 ×1.0, +0.5 per grade to Grade 8 ×4.5.

`base = 4` Ceramic-Grade UO₂ Powder per 1,000 L Enriched UF₆. Output `N = round(4 × Y)`:

| Water fed to F-12 | `getWaterTier` | Y | **N powder** | pellets | rods (÷4) | **Uraninite per rod** |
|---|---|---|---|---|---|---|
| Distilled Water | 0 *(special-cased)* | 0.5 | **2** | 2 | 0.5 | 32.0 |
| Grade 1 Purified | 1 | 1.0 | **4** | 4 | 1.0 | **16.0** |
| Grade 2 Purified | 2 | 1.5 | **6** | 6 | 1.5 | 10.7 |
| Grade 3 Purified | 3 | 2.0 | **8** | 8 | 2.0 | 8.0 |
| Grade 4 Purified | 4 | 2.5 | **10** | 10 | 2.5 | 6.4 |
| Grade 5 Purified | 5 | 3.0 | **12** | 12 | 3.0 | 5.3 |
| Grade 6 Purified | 6 | 3.5 | **14** | 14 | 3.5 | 4.6 |
| Grade 7 Purified | 7 | 4.0 | **16** | 16 | 4.0 | 4.0 |
| Grade 8 Purified | 8 | 4.5 | **18** | 18 | 4.5 | **3.6** |

Every value is an integer — `base = 4` was chosen for exactly that reason.

**The "Uraninite per rod" column is the whole balance argument.** Chain arithmetic at Grade 1:

```
16 Uraninite -F01-> 16 milled -F02-> 12 concentrate -F03-> 8,000 L PLS
             -F04->  6,000 L clarified -F05/06-> 3,000 L strip liquor
             -F07->  6 ADU -F08-> 4 yellowcake -F09-> 6 UF4 -F10-> 6,000 L UF6
             -F11->  1,000 L enriched UF6 (+ 5,000 L tails)
             -F12@G1-> 4 powder -F13/14/15-> 4 pellets -F17-> 1 fuel rod
```

The Extraction Derrick (D-17) already produces **6 Uraninite per 200 t = 36/min**. A decay cycle
is `genICampaignMinutes / 8 = 90 min` by default, so one Derrick delivers **3,240 Uraninite per
cycle**. A tier-5 silo burns **201 rods per cycle** (one per pillar).

```
3,240 / 16.0 = 202.5 rods per cycle   vs   201 rods needed
```

**At Grade 1 water, one Extraction Derrick exactly feeds a fully-built T5 plant.** Grades 2–8 buy
the headroom for the MOX / thorium / naquadah grades (which cost more input per assembly) and for
stockpiling. Distilled water halves that and does not keep up — which is the point of the ruling's
×0.5.

For comparison, the retired chain cost `2 dust Uranium ≈ 6 Uraninite` per rod. So **Grade 4
restores the old economics almost exactly (6.4), Grade 8 beats them (3.6), and Grades 1–3 are
dearer.** The water ladder is what pays for the extra fourteen steps.

### 2.2 The OUTPUT term (reactor) — the SMALLER effect

The reactor's feedwater grade multiplies EU, as one more `× n / 1000` factor on the existing
`euDemandOf` chain — the exact shape D-23's `progMilli` and D-24's `upgradeMilli` already use:

```java
euDemandOf(afterSpine, rpmMilli, rotorMilli, progMilli, upgradeMilli, waterMilli)
    = afterSpine * rpmMilli / 500_000
                 * rotorMilli               / 1000
                 * max(1000, progMilli)     / 1000
                 * max(1000, upgradeMilli)  / 1000
                 * max(1000, waterMilli)    / 1000;   // NEW
```

| Feedwater in the silo's `feedwaterHatches` | `waterMilli` | multiplier |
|---|---|---|
| `Materials.Water` | 1000 | **×1.000** (unchanged baseline) |
| Distilled Water | 1000 | **×1.000** (unchanged baseline) |
| Grade 1 Purified | 1050 | ×1.05 |
| Grade 2 Purified | 1100 | ×1.10 |
| Grade 3 Purified | 1150 | ×1.15 |
| Grade 4 Purified | 1200 | ×1.20 |
| Grade 5 Purified | 1250 | ×1.25 |
| Grade 6 Purified | 1300 | ×1.30 |
| Grade 7 Purified | 1350 | ×1.35 |
| Grade 8 Purified | 1400 | **×1.40** |

One line: **+5 % per purified grade, nothing below Grade 1.**

Plain water and distilled water stay at ×1.000 deliberately: `drawFeedwater()` accepts both today,
so a standing plant derives a **bit-identical** EU figure after this change. That is the
regression test, and it is the same test D-23 used for `progMilli == 1000`.

`drawFeedwater()` must gain the eight grades as accepted feedwater fluids, tried **worst first**
so a hatch row holding several never spends the good water when the cheap water would do.

### 2.3 Why the water key lands on exactly one step

> **OVERTURNED at the release pass — see §10.8.1.** This section is kept as the reasoning that was
> tried, because the argument reads well and is still wrong. The legibility case held; the balance
> case did not. With a rung on F-12 only, MOX drew **none** of its ceramic mass from a graded step
> and thorium drew 1/16 of it, so on those two fuel grades the yield span was ×1.00 and ×1.12
> against the reactor's ×1.40 output span — the guard of §2.4 was not merely weak on two of the
> four grades, it was **inverted**. F-20 and F-21a now carry rungs of their own, bases 8 and 6, and
> Grade 1 is the identity rung so no standing line got dearer.

F-12 is the only recipe in the document with a water rung. F-20 (tails → depleted UO₂ powder)
deliberately has **no** water key and a flat output, even though it is chemically the same
reaction: the water mechanic must be legible, and nine recipes on one NEI page is legible while
twenty-seven across three pages is not. In-world the justification holds too — depleted powder is
a MOX *diluent*, and MOX pellet quality is set by the PuO₂ blend, not by the wash water.

### 2.4 THE BALANCE GUARD, discharged

The ruling requires the output term to be the smaller of the two effects. It is, by two
independent measures.

**Measure 1 — span.** The water ladder moves the fuel-line yield by **×9.00** (distilled 0.5 →
Grade 8 4.5). It moves reactor output by **×1.40**. The output effect is smaller by a factor of
**6.4**.

**Measure 2 — the substitution test, which is the one that actually matters.** Can a player skip
the fuel chain by buying a water line?

```
whole water ladder, best case             : +40 %  EU   (x1.000 -> x1.400)
ONE rung of the fuel ladder, the smallest : +100 % EU   (ROD_MULT 1 -> 2, uranium -> MOX)
whole fuel ladder                         : +800 % EU   (ROD_MULT 1 -> 9, uranium -> naq-bonded)
```

**The entire water ladder is worth less than the cheapest single step of the fuel ladder, and one
twentieth of the whole fuel ladder.** A Purification Plant can therefore never be a route to
power; it is a route to *cheaper fuel*, which is a production effect, not an output effect. That
is the split, and that is why it was chosen.

### 2.5 The four-axis check for the water term

1. **CLIENT vs SERVER.** `waterMilli` is derived inside `drawFeedwater()`, which runs only on the
   server. The GUI and both control-block panels must read a **synced** field, added beside the
   existing `lastEUDemand` sync — not the raw field, which reads 0 on the client.
2. **TILE NBT vs ITEM NBT.** The grade is re-derived every tick from what is standing in the
   hatch. **Nothing is persisted**, so `saveNBTData` / `setItemNBT` gain nothing. Do not add it.
3. **PLANT-WIDE HATCH LISTS.** The grade is read from **`feedwaterHatches` only** — the silo's own
   private list. The Spiral's `chamberHatches` may legitimately hold Grade 8 water as *coolant*,
   and it must never pay the reactor's output term. This is exactly the failure mode axis 3
   exists to catch.
4. **IStructureElementChain.** The water term adds no structure. The four new machines in §5 each
   do; their `placeBlock` must never overwrite a standing tile entity, and `spawnHint` must return
   at the intended fallback.

---

## 3. THE MATERIAL LINES — the plant's own casings, staged by tier

The plant's own blocks (Reactor Fuel Column, Rod Loading Port, Turbine Housing, Precision
Injector, Spine Sensor, both Control Blocks, Turbine Rotor) keep their existing Assembler recipes
**verbatim** — they move file (§7) but not a single ingredient changes.

Everything else on the §0.5 list is a stock GT casing with a stock GT recipe. **We do not touch
those recipes.** The material lines are *bulk* routes that sit beside them, gated behind the Site
Batching Plant (§5.4), trading a multiblock and a higher voltage for 8–32× the batch.

| Step | Stage | Machine | Tier | In | Out |
|---|---|---|---|---|---|
| **M-01** | T1+ | Site Batching Plant | EV | 16 dust `Stone` + 8 dust `Clay` + 4 dust `Quicklime` + 4,000 L Water + circuit 1 | **32 Light Concrete** |
| **M-02** | T1+ | Site Batching Plant | EV | 32 plate `Steel` + 8 `frameGt` `Steel` + circuit 2 | **8 Solid Steel Machine Casing** |
| **M-02b** | T1+ | Site Batching Plant | EV | 32 plate `StainlessSteel` + 8 `frameGt` `StainlessSteel` + circuit 3 | **8 Clean Stainless Steel Machine Casing** |
| **M-02c** | T1+ | Site Batching Plant | EV | 8 Solid Steel Casing + 32 foil `Zinc` + 8 `plateDense` `Steel` + circuit 4 | **8 Filter Machine Casing** |
| **M-02d** | T1+ | Forming Press · `formingPressRecipes` | HV | 8 `frameGt` `Steel` | **8 Steel Frame Box** |
| **M-03** | T1–T2 | Site Batching Plant | IV | 8 plate `TungstenSteel` + 16 `plateDense` `Lead` + 8 `frameGt` `TungstenSteel` + circuit 5 | **8 Radiation Proof Machine Casing** |
| **M-03b** | T1–T2 | Site Batching Plant | IV | 8 plate `TungstenSteel` + 4 **ingot Depleted Uranium** *(F-11c)* + circuit 6 | **8 Tungstensteel Reinforced Block** |
| **M-04** | T3–T4 | Site Batching Plant | LuV | 8 Radiation Proof Casing + 8 dust `Europium` + 4,000 L molten `Europium` + circuit 7 | **8 Europium Reinforced Radiation Proof Casing** |
| **M-04b** | T3–T5 | Site Batching Plant | LuV | 8 plate `Iridium` + 8 `frameGt` `TungstenSteel` + circuit 8 | **8 Iridium Reinforced Block** |
| **M-05** | T5 | Site Batching Plant | ZPM | 8 plate `NaquadahAlloy` + 8 `frameGt` `NaquadahAlloy` + 4,000 L molten `Naquadah` + circuit 9 | **8 Radiant Naquadah Alloy Casing** |
| **M-06** | all | Site Batching Plant | EV | 32 plate `TungstenSteel` + 8 `frameGt` `TungstenSteel` + circuit 10 | **8 Tungstensteel Turbine Casing** |

**Real process where credible.** M-01 is an aggregate-and-cement batching plant, which is what
every real construction site of this size has on it. M-02 / M-02b / M-06 are plate-and-frame press
lines. M-03 is lead-lined shielding-panel fabrication. M-04 / M-05 are doping and cladding steps
and are frankly the "unrealistic but big" end.

**The depleted-uranium tie (M-03b) is deliberate and real:** DU is used as radiation shielding
because it is dense, and the enrichment tails from F-11 are exactly where it comes from. The fuel
line therefore feeds the casing line, which is the sort of interlock this plant should have.

**Sizing check.** A T5 plant wants 3,773 Light Concrete = **118 M-01 batches**; 1,667 Radiant
Naquadah Alloy Casing = **209 M-05 batches**; 798 Tungstensteel Turbine Casing = **100 M-06
batches**. With EV/LuV overclocks and a Processing Array those are minutes, not hours — which is
the entire justification for the line.

---

## 4. THE PROPELLANT AND COOLANT LINES

### 4.1 Propellant — real chains behind the D-08 ladder (P values unchanged)

The bottom three rungs (Benzene, High Octane Gasoline, H8N4C2O4 Rocket Fuel) are stock pack fluids
with stock chains. **Untouched.**

The six BTS rungs currently each have **one** Mixer recipe. Those six are retired and replaced by
a three-stage chain per rung — an oxidiser, a metallised fuel slurry, and a blend. That is how
real high-energy propellants are made: a fuel, an oxidiser, and a metal powder for density impulse
(aluminised propellant is the textbook case).

| Step | Name | Machine | Tier | In | Out |
|---|---|---|---|---|---|
| **P-01** | Oxidiser conditioning | Chemical Plant t2 · `chemicalPlantRecipes` | EV | 2,000 L `NitricAcid` + 1,000 L `DinitrogenTetroxide` + circuit 1 | 3,000 L **Oxidiser Blend** |
| **P-02** | Metal additive milling | Ore Milling Plant · `millingRecipes` | EV–UIV | 8 dust `Aluminium` *(Lv 1–2)* / `Adamantium` *(Lv 3–4)* / `Infinity` *(Lv 5–6)* + Milling Ball + 1,000 L `GTPPFluids.Kerosene` | 2,000 L **Metal Additive Slurry Lv n** |
| **P-03** | Blending, Lv 1–3 | Chemical Plant t3 · `chemicalPlantRecipes` | LuV–UV | 1,000 L `GGMaterial.lightNaquadahFuel` / `naquadahBasedFuelMkI` / `MkIII` + 1,000 L **Oxidiser Blend** + 500 L **Metal Additive Slurry** + circuit 5 | 250 L `naqPropellantT1` / `T2` / `T3` |
| **P-03h** | Blending, Lv 4–6 | Quantum Force Transformer · `quantumForceTransformerRecipes`, `QFT_FOCUS_TIER` 1 | UHV–UIV | 1,000 L `naquadahBasedFuelMkIV` / `MkV` / `MkVI` + 1,000 L **Oxidiser Blend** + 500 L **Metal Additive Slurry Lv 5/6** + catalyst | 250 L `naqPropellantT4` / `T5` / `T6` |
| **P-04** | Cartridge gelling *(storage and logistics only)* | Fluid Solidifier · `fluidSolidifierRecipes` | LuV | 1,000 L `naqPropellantTn` + gel mould | 1 **Propellant Cartridge Lv n** *(1,000 L; re-liquefied in a Fluid Extractor)* |

Output volume per rung is unchanged from the retired Mixer recipes (250 L per cycle), so the
Spiral chamber's supply-rate-vs-clock-frequency balance (D-20) is not disturbed. Only the input
side gets longer.

**As-built (§10.4.1 and the release pass).** The six Mixer recipes were **not** retired — they are
kept standing, byte-identical, and the chain is additive beside them. P-02 is split into P-02a
(IsaMill, dust + **Alumina Milling Ball**, items only) and P-02b (Chemical Plant t2, 1 powder +
1,000 L Kerosene → 2,000 L slurry, i.e. 250 L per dust). P-03/P-03h spend **500 L** of naquadah fuel
per rung, not 1,000 — half the legacy Mixer cost, which is the whole point of the longer chain — plus
1,000 L Oxidiser Blend and `dustEquivalent × 250 L` of slurry, in 30 s. **P-03h takes circuit 1, not
5** (§10.8.6). P-04's mould is `Shape_Mold_Ball` and both legs are registered.

### 4.2 Coolant — bulk production only; the ladder is not touched

**Scope statement, said plainly:** the coolant ladder is a settled D-08 table of six *existing*
pack fluids. Inventing a seventh, or changing a Q, would be relitigating a ruling. This line
therefore does exactly one thing: it makes the ladder's own fluids at plant scale.

| Step | Name | Machine | Tier | In | Out |
|---|---|---|---|---|---|
| **C-01** | Deionised makeup | Mixer · `mixerRecipes` | HV | 16,000 L Distilled Water + 2 **Ion Exchange Resin** *(90 % returned)* | 16,000 L Distilled Water *(a no-op rung, present so the NEI page shows the bottom of the ladder)* |
| **C-02** | IC2 Coolant, bulk | Chemical Plant t2 · `chemicalPlantRecipes` | EV | **8×** the stock GT input set (8 dust Lapis + 8,000 L Distilled Water) + 200 L **Chemical Shim** + circuit 11 | **8×** IC2 Coolant, in 1,024 t — half what 8 stock cycles cost |
| **C-03** | Super Coolant, bulk | Chemical Plant t3 → Vacuum Freezer · `vacuumFreezerRecipes` | IV | 8× the stock input set + 400 L **Chemical Shim** | 8× Super Coolant |
| **C-04** | Chemical shim package | LCR · `UniversalChemical` | HV | 2,000 L `GTPPFluids.BoricAcid` + 2 dust `Lithium` + 1,000 L Water | 2,000 L **Chemical Shim** *(a real PWR boric-acid / LiOH shim; a corrosion inhibitor for C-02/C-03, never fed to the reactor)* |

**As-built: the batch is 8×, not 10×.** 8,000 L fills a plain Input Hatch exactly, which is worth
more than a round number. C-03 is likewise 8,000 L of Hot Super Coolant + 400 L shim in 800 t, half
what 8 stock cycles cost. One C-04 batch doses 80,000 L of C-02 or 40,000 L of C-03, so the shim is
a light tax rather than the thing that gates the route. **C-01 was deliberately not shipped**
(§10.4.2).

Liquid Cryotheum and Tachyon Rich Temporal Fluid keep their existing pack chains untouched — both
are top-tier fluids with well-established production and neither needs a second route.

**Explicitly out of scope and flagged for a future ruling:** a *coolant recovery loop* (hot coolant
out of the Spiral chamber → chill train → cold coolant back). It would be excellent, it is
genuinely how a real plant works, and it requires changing `MTEReactorCoreMk1`'s chamber drain to
emit a fluid — which is a reactor-behaviour change, not a recipe. It is not in this document.

---

## 5. NEW MACHINES — four, each with the reason an existing one will not do

MTE ids: `Config.mteBaseId` is 28100. **`base+1` is BURNED, not free** — the design assumed the
gap between `base` and `base+2` was an unused id; it is a *retired* one. `git show
0.5.0:.../BTSMachines.java` registers `MTETurbineHall(base + 1)`, and 0.5.0 is a shipped tag, so a
world saved on it has a Turbine Hall bound to 28101 and handing that id to a new controller would
resolve every one of them into a Cascade Hall on load. Caught at the release pass (M9). Final ids:
**28113 (Cascade Hall), 28110, 28111, 28112.** Next free id is `base+14`; `MetaTileEntityIDs` in the
pinned jar leaves 28056–29989 empty, so there is room.

### 5.1 Cascade Hall — MTE 28113, IV, multiblock

**Why no existing machine.** GT's Centrifuge and GT++'s Reactor Processing Unit are fixed-ratio
splitters: one input set, one output set, ratios baked into the recipe. Isotope separation is a
**cascade** — its defining property is that it produces *two* streams whose split is set by how
many stages you built, and that the tails are a real product rather than a discard. Nothing in the
pack expresses "the structure decides the split".

**Structure.** A 5×5×N hall, N = 8 + 4k for k = 0..4 (stage banks). Tungstensteel Turbine Casing
shell; each bank is a 3×3 ring of Iridium Reinforced Block around a centrifuge rotor core;
1 Maintenance, 1 Energy Hatch IV, 1 Input Hatch, 2 Output Hatch, all on **dedicated sockets**
(TC-1: no blanket wall adders — every hatch gets one cell, so the hologram shows the exact count
at the exact spot).

**Behaviour.** Stage banks set **parallel**, not assay: `parallel = 2^k`. The assay is chosen by
the recipe circuit (1 = LEU, 2 = HEU). Own recipe map `btsCascadeRecipes`, built with the same
`RecipeMapBuilder` pattern `BTSDisplayRecipes` already uses — but **real, not `.fake()`**.

⚠ Do not name this "Enrichment Train": `MTEEnrichmentTrain` (MTE 28103) already exists as the
Enrichment Annex module.

### 5.2 Fuel Fabrication Hall — MTE 28110, IV, multiblock

**Why no existing machine.** Throughput, and it is a real thing. A T5 silo eats **201 rods per
90-minute decay cycle**, in up to four grades at once. The Assembly Line is one recipe at a time
behind research; the Assembler has no parallel. A fuel fabrication plant is a continuous line and
should behave like one.

**Structure.** 7×5×12 hall: Clean Stainless Steel Machine Casing shell, a Tungstensteel Turbine
Casing spine down the centreline, 1 Maintenance, 2 Energy IV, 2 Input Bus, 1 Input Hatch, 1 Output
Bus on dedicated sockets.

**Behaviour.** `parallel = 8 × casingTier` (Clean Stainless = 1, Iridium Reinforced = 2, Radiant
Naquadah Alloy = 3), read off the structure walk. Own recipe map `btsFabricationRecipes`. Handles
F-17 and F-18 for the first three grades (circuit 1–3); the naquadah grade's *rod* goes to the
Precise Assembler and its *bundle* to the Assembly Line instead, so the top grade still costs a
real endgame machine.

### 5.3 PUREX Canyon — MTE 28111, IV, multiblock

**Why no existing machine.** The Chemical Plant and Digester can each run one dissolution recipe.
Neither can express **"the output slate depends on how long this fuel cooled"** — the mechanic that
makes reprocessing interesting and the one thing a spent-fuel head-end genuinely is. R-02 (cooled)
yields plutonium; R-02h (straight from the reactor) does not. Nothing in the pack has an input
whose *history* changes the recipe.

**Structure.** 9×7×9 shielded canyon: Radiation Proof Machine Casing shell over a Light Concrete
cell floor, 1 Maintenance, 2 Energy IV, 1 Input Bus, 2 Input Hatch, 1 Output Bus, 2 Output Hatch on
dedicated sockets. Both casings are ones the plant already needs in bulk, so the material lines
serve it too.

**Behaviour.** Own recipe map `btsPurexRecipes`. Emits `BTSFluids.nuclearWasteWater`, closing D-09.

### 5.4 Site Batching Plant — MTE 28112, EV, multiblock

**Why no existing machine.** Volume. §0.5 says a T5 buildout is ≈8,000 blocks and 3,773 of them
are Light Concrete. Hand-crafting or single-block-assembling that is the largest single friction in
building this plant, and the questbook's Blueprint quests quote those counts directly. Every real
construction site of this size has a batching plant on it; this one should too.

**Structure.** 5×5×9: Solid Steel Machine Casing shell over a Steel Frame Box lattice, one Steel
Pipe Casing riser, 1 Maintenance, 1 Energy EV, 2 Input Bus, 1 Input Hatch, 1 Output Bus on
dedicated sockets. Deliberately the cheapest of the four — it is the *first* thing a player builds,
before the silo.

**Behaviour.** `parallel = 4 × casingTier`. Own recipe map `btsBatchingRecipes`. Registers M-01 …
M-06 only; it can make nothing GT cannot already make, only faster and in bulk.

### 5.4b How the four are crafted — **added at the release pass (B1)**

All four shipped **with no recipe of any kind**: outside `/bts kit` they could not be obtained, so
the whole of §1 was unreachable. Four Assembler entries, on this file's existing tier idiom (EV runs
dry at 30 s, IV takes one ingot of molten Platinum at 45 s), each built out of the casing it is
actually walled in. Circuits 14–17 are fresh — `RecipeMapBackend.doAdd` silently drops a colliding
recipe, so reusing a number already in `BTSRecipesMachines` would have lost one.

| Machine | Tier | Item inputs (circuit counts as one of the nine) | Fluid |
|---|---|---|---|
| **Site Batching Plant** | EV, 30 s, circuit 14 | 8 Solid Steel Casing, 4 `frameGt` Steel, 2 Steel Pipe Casing, 2 Conveyor EV, 2 circuit EV | — |
| **Cascade Hall** | IV, 45 s, circuit 15 | 8 Tungstensteel Turbine Casing, 4 TS Pipe Casing, 4 Motor IV, 8 `stickLong` TungstenSteel, 2 plate RhPdPalladium, 4 circuit IV | 1 ingot molten Pt |
| **Fuel Fabrication Hall** | IV, 45 s, circuit 16 | 8 Clean Stainless Casing, 4 TS Turbine Casing, 4 Robot Arm IV, 4 Conveyor IV, 2 plate RhPdPalladium, 4 circuit IV | 1 ingot molten Pt |
| **PUREX Canyon** | IV, 45 s, circuit 17 | 8 Radiation Proof Casing, 4 `plateDense` Lead, 4 Pump IV, 4 `pipeMedium` Stainless, 2 plate RhPdPalladium, 4 circuit IV | 1 ingot molten Pt |

`assemblerRecipes` is `maxIO(9, 1, 1, 0)` with `minInputs(1, 0)` — nine item inputs, one fluid in,
one item out, **zero fluid outputs** — and `GTRecipeBuilder.circuit(int)` writes the integrated
circuit back into `inputsBasic`, so it spends one of the nine. The widest of the four uses seven.

**No bootstrap loop:** every casing these four need keeps its stock GT recipe (§3's premise, that
the material lines sit *beside* GT's own), so the Site Batching Plant is reachable before the Site
Batching Plant exists.

### 5.5 Candidates considered and REJECTED — and why

Discipline matters more than count. These were designed and then cut:

| Rejected | Why an existing machine does it |
|---|---|
| **Ceramic Powder Line** (a dedicated home for the water step) | Nine parallel recipes on the Chemical Plant teach the ladder just as well, and that is exactly how GTNH already teaches purified water. A bespoke GUI would be nicer and is not worth an MTE. |
| **Solvent Extraction Battery** (counter-current mixer-settler train) | The 90 %-return output chance on the organic in F-06 models the recycle loop faithfully enough on the Chemical Plant. |
| **Spent Fuel Pond** | `MTEDecayWarehouse` + `RecipeMaps.isotopeDecay` with `HALF_LIFE` / `DECAY_TYPE` metadata is *precisely* this machine, already in the pack. |
| **Pellet Sinter Kiln** | GT++'s Vacuum Furnace with `COIL_HEAT` is a sintering furnace. |
| **Propellant Blending Skid** | Chemical Plant (Lv 1–3) and QFT (Lv 4–6) already stage by tier, which is what the rung ladder needs. |
| **Coolant Conditioning Loop** | Would require changing the reactor's chamber drain to emit a fluid. That is a behaviour ruling, not a recipe. Out of scope (§4.2). |
| **Uranium Metal Reduction Furnace** | GT already reduces Uraninite to Uranium metal on `UniversalChemical` (C1). Duplicating it would break "boost, never replace" outright. |

---

## 6. TOTAL MACHINE COUNT — **25**, against a budget of 30

**Existing machines the lines require (21) — no new code for any of them:**

| # | Machine | Owning mod | Recipe map | Tier band |
|---|---|---|---|---|
| 1 | Ore Milling Plant (IsaMill) | GT++ | `millingRecipes` | HV–UIV |
| 2 | Flotation Cell | GT | `flotationCellRecipes` | EV |
| 3 | Digester | GTNH Lanthanides | `digesterRecipes` | EV |
| 4 | Chemical Plant | GT++ | `chemicalPlantRecipes` | EV–UV |
| 5 | Large Chemical Reactor | GT | `UniversalChemical` | HV–EV |
| 6 | Dehydrator | GT++ | `chemicalDehydratorRecipes` | EV–IV |
| 7 | Vacuum Furnace | GT++ | `vacuumFurnaceRecipes` | IV |
| 8 | Industrial Centrifuge | GT | `centrifugeRecipes` | MV–EV |
| 9 | Mixer | GT | `mixerRecipes` | MV–HV |
| 10 | Distillation Tower | GT | `distillationTowerRecipes` | HV–EV |
| 11 | Forming Press | GT | `formingPressRecipes` | MV–HV |
| 12 | Lathe | GT | `latheRecipes` | MV |
| 13 | Extruder | GT | `extruderRecipes` | EV |
| 14 | Assembler | GT | `assemblerRecipes` | LV–IV |
| 15 | Assembly Line | GT | `GTRecipeConstants.AssemblyLine` | UV |
| 16 | Precise Assembler | GoodGenerator | `preciseAssemblerRecipes` | UV |
| 17 | Neutron Activator | GoodGenerator | `neutronActivatorRecipes` | LuV |
| 18 | Decay Warehouse | GT | `isotopeDecay` | EV |
| 19 | Vacuum Freezer | GT | `vacuumFreezerRecipes` | IV |
| 20 | Quantum Force Transformer | GT++ | `quantumForceTransformerRecipes` | UHV–UIV |
| 21 | Fluid Solidifier | GT | `fluidSolidifierRecipes` | LuV |

**New machines (4):** Cascade Hall (IV) · Fuel Fabrication Hall (IV) · PUREX Canyon (IV) · Site
Batching Plant (EV).

**TOTAL 25.** Five under budget, and §5.5 lists the seven candidates cut to get there. Padding to
thirty would have meant building machines the pack already has, which is the one thing the "boost,
never replace" rule forbids.

Staging, as the ruling asked — casings low, propellant/coolant high:

```
EV       Site Batching Plant, Flotation Cell, Digester, Dehydrator, Extruder,
         Decay Warehouse, Distillation Tower              -> CASINGS + fuel front end
IV       Cascade Hall, Fuel Fabrication Hall, PUREX Canyon,
         Chemical Plant t3, Vacuum Furnace, Vacuum Freezer -> conversion, fabrication, coolant
LuV      Neutron Activator, Fluid Solidifier               -> propellant Lv 1-3
UV       Assembly Line, Precise Assembler                  -> naquadah grade
UHV-UIV  Quantum Force Transformer                         -> propellant Lv 4-6, naq-bonded fuel
```

---

## 7. THE FILE SPLIT — five units, zero shared files

`BTSRecipes.java` is one 486-line file and several units must add recipes at once. The split below
gives every unit files **nobody else opens**.

### 7.1 The coordinating commit (done FIRST, by one unit, before any other starts)

This is the only commit that touches the shared registrars. After it lands, **no unit edits
`BTSItems.java`, `BTSFluids.java`, `BTSMachines.java` or `BTSRecipes.java` again.**

```
BTSRecipes.java        -> emptied to an orchestrator:
                            BTSRecipesFuel.register();
                            BTSRecipesMaterials.register();
                            BTSRecipesPropellant.register();
                            BTSRecipesCoolant.register();
                            BTSRecipesMachines.register();
                            BTSDisplayRecipes.register();
BTSItems.register()    -> + BTSItemsFuel.register();
                          + BTSItemsMaterials.register();
                          + BTSItemsPropellant.register();
BTSFluids.register()   -> + BTSFluidsProcess.register();
                          + BTSFluidsPropellant.register();
BTSMachines.register() -> + 4 lines, ids base+1, base+10, base+11, base+12
BTSWaterGrade.java     -> NEW, written in full and FROZEN in this commit (see 7.3)
```

The existing 26 registration blocks move **verbatim** into their new files in this same commit, so
the split is provably behaviour-neutral before anyone adds anything.

### 7.2 Ownership

| Agent | Owns (exclusively) |
|---|---|
| **A — Fuel** | `BTSRecipesFuel.java`, `BTSItemsFuel.java`, `BTSFluidsProcess.java`, `MTECascadeHall.java`, `MTEFuelFabricationHall.java`, `MTEPurexCanyon.java` |
| **B — Materials** | `BTSRecipesMaterials.java`, `BTSItemsMaterials.java`, `MTESiteBatchingPlant.java` |
| **C — Propellant + Coolant** | `BTSRecipesPropellant.java`, `BTSRecipesCoolant.java`, `BTSItemsPropellant.java`, `BTSFluidsPropellant.java` |
| **D — Machines** | `BTSRecipesMachines.java` (the controller / hatch / control-block / rotor recipes, moved verbatim) |
| **E — Reactor** | `MTEReactorCoreMk1.java` (the water OUTPUT term §2.2 + the Annex re-point §8.3), `MTEEnrichmentTrain.java` |

Two files are *read* by more than one unit and **written by none of them after 7.1**:
`BTSWaterGrade.java` and `BTSBom.java`.

### 7.3 `BTSWaterGrade.java` — the one place the ladder is typed

```java
public final class BTSWaterGrade {
    /** 0 = distilled, 1..8 = the purified grades, -1 = not a fuel-line water. */
    public static int gradeOf(FluidStack fs);      // distilled test FIRST, then PurifiedWaterHelpers.getWaterTier
    /** Fuel-line YIELD, x1000: 500, 1000, 1500 .. 4500. */
    public static int yieldMilli(int grade);
    /** Reactor OUTPUT, x1000: 1000 for water/distilled, 1050 .. 1400 for grades 1..8. */
    public static int outputMilli(int grade);
    /** F-12 output count: round(4 * Y) = 2,4,6,8,10,12,14,16,18. */
    public static int powderFor(int grade);
    public static FluidStack fluidFor(int grade, int litres);
}
```

Agent A writes it in the coordinating commit; units A and E both consume it. The ladder is typed
**once**, so the recipe page and the reactor readout cannot drift — the same discipline
`BTSDisplayRecipes` already applies to the propellant and coolant tables.

### 7.4 Rules that keep the parallel work safe

- `git add <explicit paths>` only. Never `git add -A`.
- Commit prefix `wip:`, one commit per step group, never one at the end.
- On `index.lock`: wait two seconds and retry. Never delete it.
- No unit may add a `Materials` / `Werkstoff` / `GGMaterial` reference without grepping the pinned
  jar for it first. Several plausible names in §0.8 **do not exist** where you expect them.
- `BTSDisplayRecipes` gains a fourth page (`…plantwater`) listing the nine rungs. Its existing
  missing-dependency guard pattern applies: a rung that cannot be built must **skip, never throw** —
  `register()` runs in postInit and one escaping exception takes the whole mod down.

---

## 8. MIGRATION — every removed recipe, and what a world actually loses

### 8.1 Removed recipes — **ten**, all in `BTSRecipes.java`

> **The design said eighteen. Ten shipped.** Rows 11–18 below — the eight
> `depletedFuel* → dusts` Centrifuge recipes — were retired as specified and then **restored
> before release** (release-pass finding B6, §10.8.2). They are back verbatim from
> `git show 5344bcf`: same outputs, same chances, same durations, same EU tiers, and still no
> programmed circuit, because adding one would have invalidated exactly the standing automation
> they exist to protect. Nine Assembler recipes and one Mixer recipe are gone; nothing else is.

| # | Removed recipe | Map | Replaced by |
|---|---|---|---|
| 1 | 1 dust Uranium → 2 `uraniumFuelPellet` | assembler | F-01 … F-15 |
| 2 | 4 `uraniumFuelPellet` + Pt cladding → `fuelRodUranium` | assembler | F-16, F-16b, **F-17** |
| 3 | 3 `fuelRodUranium` + hardware → `fuelAssemblyUranium` | assembler | **F-18** |
| 4 | 1 Pu + 8 U dust → 18 `moxFuelPellet` | mixer | F-20, **F-19**, F-13 … F-15 |
| 5 | 4 `moxFuelPellet` + Pt → `fuelRodMox` | assembler | F-17 circuit 2 |
| 6 | 3 `fuelRodMox` → `fuelAssemblyMox` | assembler | F-18 circuit 2 |
| 7 | 3 rods + 8 Th → `fuelAssemblyThoriumBonded` | assembler | **F-21a/b**, F-13 … F-15, F-18 circuit 3 |
| 8 | 3 rods + NaqEnr + U235 → `fuelAssemblyNaqBonded` | assembler | **F-22**, **F-18n** |
| 9 | 1 rod + 3 Th → `fuelRodThoriumBonded` | assembler | F-17 circuit 3 |
| 10 | 1 rod + 3 NaqEnr + U235 → `fuelRodNaqBonded` | assembler | **F-23** |
| ~~11–14~~ | ~~the four `depletedFuelAssembly*` → dusts~~ | centrifuge | **KEPT** — restored before ship. PUREX (R-01 → R-02 → R-03) is the better route, not the only one |
| ~~15–18~~ | ~~the four `depletedFuelRod*` → dusts~~ | centrifuge | **KEPT** — as above |

**Kept, unchanged, moved file only:** the `nuclearWasteWater` distillation (D-09), Reactor Fuel
Column, Rod Loading Port, Turbine Housing, Precision Injector, Spine Sensor, Silo Control Block,
Turbine Control Block, Turbine Rotor, Extraction Platform, Enrichment Train, Reactor Core Mk1.

**Kept but rebuilt on a longer chain:** the six `naqPropellantT1..T6` Mixer recipes become P-01 →
P-02 → P-03 / P-03h. Output volume per cycle (250 L) is identical, so D-20's supply-rate balance is
untouched.

### 8.2 Items that become unobtainable: **NONE**

This is a deliberate design constraint, not an accident. All twenty-two `BTSItems` fields keep a
route, and the two load-bearing ratios are preserved exactly:

- **4 pellets per rod** (F-17 = old recipe 2)
- **3 rods per assembly** (F-18 = old recipe 3)

So every pellet, rod and assembly standing in a chest, an ME system or a Rod Loading Port buys
exactly what it bought yesterday.

### 8.3 ⚠ REQUIRED CODE CHANGE — the Enrichment Annex is now a bypass

> **STATUS: DONE (release pass, finding M7).** Both call sites now pay `BTSItemsFuel.yellowcake`.
> The rest of this section is the specification they were built to; §10.6 records what actually
> landed, including the milestone interaction the plain two-line change would have broken.

`MTEReactorCoreMk1.annexPellets()` and `MTEEnrichmentTrain.checkProcessing()` currently output
**`BTSItems.uraniumFuelPellet`** directly:

```
MTEEnrichmentTrain.java:145   new ItemStack(BTSItems.uraniumFuelPellet, OUTPUT_PELLETS)
MTEReactorCoreMk1.java:5910   out.add(new ItemStack(BTSItems.uraniumFuelPellet, annexPellets()));
```

`8 Uraninite + 2,000 L HF → 20 Fuel Pellets` every 600 t. **Left alone, that skips the entire fuel
line.** It must be re-pointed to yellowcake:

```
8 Uraninite + 2,000 L HydrofluoricAcid  ->  2 Yellowcake (U3O8)     every 600 t, 480 EU/t
```

The ratio is chosen to match the main line exactly (F-01 … F-08 turn 16 Uraninite into
4 Yellowcake — 4 Uraninite per Yellowcake, identical), so the Annex stays a **slow, HF-hungry
bootstrap** past the front end while F-09 … F-18 remain compulsory. That is precisely the module's
designed role ("the campus processing factory"), and it stops being a shortcut past the interesting
steps.

Everything that quotes the Annex must move with it: `MTEEnrichmentTrain` tooltip (line 159),
`MTEReactorCoreMk1` tooltip (line 8150), the D-17 row in `../PROPOSAL.md`, the wiki, and the
questbook — rebuilt via `powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1`,
the one command that re-renders `BTSBom.java` **and** `questbook/DefaultQuests` together. Never run
one without the other.

Owner: **Agent E.** Four-axis note: `annexPellets()` is read by a GUI panel, so its replacement must
stay on the existing sync path (axis 1), and the Annex's sockets join **no plant-wide hatch list**
(axis 3) — that property must survive the edit.

### 8.4 What an existing world loses, plainly

**Dead automation — ten setups, not eighteen.** Every standing **Assembler** and **Mixer** running
one of the ten retired recipes stops on world load. No crash, no item loss, no machine damage — GT
simply finds no recipe and the machine idles. **Ten AE2 / pattern / Steve's-Factory setups go
invalid** and must be rebuilt against the new chain. This is the cost I accepted when ruling
that the old chain is replaced entirely.

**Centrifuge arrays keep running.** The eight `depletedFuel* → dusts` recipes are unchanged, so a
reprocessing line built in 0.23.0 survives the update untouched — same inputs, same chances, same
durations, and deliberately still no programmed circuit so existing patterns match byte for byte.

**There is no depleted-fuel backlog window any more.** The design created one: with reprocessing
moved wholly onto the PUREX Canyon, an updating world's spent fuel had no route at all until an IV
multiblock stood. That is a stall, not a difficulty curve, and it was the one migration failure a
release pass had to catch. Two mitigations now exist rather than one: the Centrifuge route never
went away, and R-02h accepts *uncooled* assemblies straight from the reactor, so the Canyon
**alone** also clears a backlog. A Decay Warehouse is only needed for the plutonium, and therefore
only for MOX. **PUREX remains strictly better on every axis** — ceramic UO₂ that drops straight back
into F-13 rather than metal dust that must climb from F-09, plus cladding hulls for R-04 and the
waste water that pays the D-09 anchor — so it is an upgrade you build, not a toll gate you must.

**The Annex changes what it makes.** Any output bus, AE2 storage bus or logistics pipe reading the
Enrichment Annex's or the standalone Enrichment Train's socket now receives **Yellowcake, not Fuel
Pellets**. This is the one migration hazard in the release that silently *keeps working* while
delivering the wrong item, so it is flagged in the changelog, in both tooltips, in the wiki and in
the two questbook pages that quote the module.

**Purified-water plants no longer melt down on shutdown.** A plant plumbed for exactly what the
release rewards — one Grade N Purified Water feedwater hatch and nothing else — ran perfectly and
then could not pay its decay heat, because the running draw knew eleven fluids and the shutdown
draw knew three. Both read one ladder now (§10.8.3). Nobody who followed the tooltip should have
been able to reach a guaranteed meltdown, and now nobody can.

**A steeper front end at low water grades.** At Grade 1 the chain costs 16 Uraninite per rod against
the retired chain's ~6, and it takes an IsaMill, a Flotation Cell, a Digester, a Chemical Plant and
a Dehydrator to reach yellowcake. **Grade 4 water restores the old cost almost exactly (6.4) and
Grade 8 beats it (3.6)** — the Purification Plant is how a player buys the old economics back, which
is the whole point of the ruling.

**Nothing is lost that a player is holding.** No item is deleted, no NBT is rewritten, no structure
changes shape, and no standing plant un-forms. A plant that never feeds purified water derives a
bit-identical EU figure to the one it derived before this release (§2.2).

### 8.5 Release checklist

1. Coordinating commit (§7.1) — split only, zero behaviour change, `spotlessApply compileJava` green.
2. Agents A–E in parallel, `wip:` commits per step group.
3. `powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1` — the ONE command
   that re-renders `BTSBom.java` and the questbook together.
4. `./gradlew.bat spotlessApply compileJava --no-daemon` to BUILD SUCCESSFUL.
5. The flowchart — **built**, `docs/production-flowchart.html`, verified in-browser (§11).
6. Changelog: **the ten dead recipes** (Assembler and Mixer only — the eight Centrifuge
   reprocessing recipes are KEPT), the water ladder on all three oxide steps, **the Annex and the
   Enrichment Train now paying Yellowcake instead of Fuel Pellets**, and the decay-heat draw
   widening to the whole feedwater ladder. §10.8 is the list.
7. A browser mockup for each of the four new multiblocks (standing rule, D-19).

---

## 9. THE FLOWCHART

```mermaid
graph TD
  subgraph FRONT["FRONT END - ore to yellowcake (EV)"]
    ORE["Uraninite / Pitchblende dust<br/>Extraction Derrick: 36/min"]
    ORE -->|F-01 IsaMill| MILL["Milled Uraninite"]
    MILL -->|F-02 Flotation Cell| CONC["Uraninite Concentrate"]
    CONC -->|"F-03 Digester<br/>H2SO4 + Fe3+"| PLS["Pregnant Leach Solution"]
    PLS -->|"F-04 Chem Plant<br/>ion exchange"| CLAR["Clarified Uranyl Liquor"]
    CLAR -->|"F-05 Chem Plant<br/>TBP/kerosene SX"| ORG["Loaded Organic"]
    ORG -->|"F-06 Chem Plant<br/>strip, 90% solvent back"| STRIP["Uranyl Strip Liquor"]
    STRIP -->|"F-07 LCR + NH3"| ADU["Ammonium Diuranate"]
    ADU -->|"F-08 Dehydrator<br/>800C calcine"| YC["YELLOWCAKE U3O8"]
  end

  ANNEX["Enrichment Annex / Enrichment Train<br/>8 Uraninite + 2,000 L HF"] -.->|"2 YELLOWCAKE - 4 Uraninite/cake,<br/>the same ratio F-01..F-08 reach"| YC

  subgraph CONV["CONVERSION + ENRICHMENT (IV)"]
    YC -->|"F-09 Chem Plant + HF"| UF4["UF4 - GT++ material"]
    UF4 -->|"F-10 Chem Plant + F2"| UF6["UF6 - GT++ material"]
    LFTR["existing LFTR line's UF6"] -.->|"boost, not replace"| UF6
    UF6 -->|F-11 CASCADE HALL| ENR["Enriched UF6 3.2%"]
    UF6 --> TAILS["Depleted UF6 tails"]
    ENR -->|F-11b| HEU["Highly Enriched UF6"]
    TAILS -->|"F-11c Neutron Activator"| DU["Depleted Uranium ingot"]
  end

  subgraph WATER["THE WATER STEP"]
    PW["Purification Plant<br/>distilled / Grade 1..8"]
    ENR -->|F-12 Chem Plant| POW["Ceramic-Grade UO2 Powder<br/>N = round(4 x Y)<br/>2 / 4 / 6 .. 18"]
    PW -->|"YIELD x0.5 .. x4.5"| POW
    POW -.->|"4,000 L HF recovered"| UF4
  end

  subgraph CER["CERAMICS + FABRICATION"]
    POW -->|F-13 Forming Press| GREEN["Green Pellet"]
    GREEN -->|"F-14 Vacuum Furnace<br/>1,800 K + H2"| SINT["Sintered UO2 Pellet"]
    SINT -->|F-15 Lathe| PEL["uraniumFuelPellet<br/>EXISTING ITEM"]
    ZR["GG zircaloy4"] -->|F-16 Extruder| TUBE["Zircaloy-4 Cladding Tube"]
    PEL -->|"F-17 FUEL FABRICATION HALL<br/>4 pellets + tube + He"| ROD["fuelRodUranium"]
    TUBE --> ROD
    ROD -->|"F-18 Fab Hall - 3 rods"| ASM["fuelAssemblyUranium F=1"]
  end

  subgraph GRADES["THE OTHER THREE GRADES"]
    TAILS -->|"F-20 Chem Plant<br/>base 8: 4 / 8 .. 36"| DUPOW["Depleted UO2 Powder"]
    PW -->|"YIELD x0.5 .. x4.5"| DUPOW
    PUO2["PuO2"] -->|"F-19 IsaMill MIMAS<br/>6.25% Pu"| MOXP["MOX Powder"]
    DUPOW --> MOXP
    MOXP -->|"F-13/14/15/17/18"| MOXA["fuelAssemblyMox F=2"]
    THOX["GG thoriumOxalate"] -->|"F-21a Dehydrator - WASHED<br/>base 6: 3 / 6 .. 27"| THO2["ThO2 Powder"]
    PW -->|"YIELD x0.5 .. x4.5"| THO2
    THO2 -->|F-21b IsaMill| THU["(Th,U)O2 Powder"]
    POW --> THU
    THU -->|"F-13/14/15/17/18"| THA["fuelAssemblyThoriumBonded F=5"]
    SINT -->|"F-22 QFT - naquadah CVI"| NAQP["Naquadah-Bonded Pellet"]
    HEU --> NAQP
    NAQP -->|F-23 Precise Assembler| NROD["fuelRodNaqBonded"]
    NROD -->|F-18n Assembly Line| NAQA["fuelAssemblyNaqBonded F=9"]
  end

  subgraph REACT["THE PLANT"]
    ASM --> CORE["Reactor Silo<br/>steam = 16,384 x ROD_MULT x live"]
    MOXA --> CORE
    THA --> CORE
    NAQA --> CORE
    PW -->|"feedwater OUTPUT term<br/>x1.00 .. x1.40"| CORE
    CORE --> EU["EU/t"]
    CORE --> SPENT["depleted assemblies"]
  end

  subgraph REPRO["REPROCESSING"]
    SPENT -->|R-01 Decay Warehouse| COOL["Cooled Depleted Assembly"]
    COOL -->|"R-02 PUREX CANYON<br/>HNO3 chop-leach"| LIQ["Dissolver Liquor"]
    SPENT -.->|"R-02h uncooled<br/>no plutonium"| LIQ
    LIQ -->|"R-03 PUREX Canyon<br/>TBP partition"| POW
    LIQ --> PUO2
    LIQ --> HULL["Cladding Hull"]
    LIQ --> NWW["nuclearWasteWater"]
    HULL -->|R-04 LCR| ZR
    SPENT -.->|"R-00 plain Centrifuge - KEPT<br/>metal dust only, no hulls, no waste water"| DUSTS["U / Pu / Pb / Th / Naq dust"]
    CORE -.->|"emitWasteWater: 1 L per live pillar per tick"| NWW
    NWW -->|"EXISTING recipe - D-09 anchor"| TA["Tantalum + distilled water"]
  end

  subgraph MAT["MATERIAL LINES - SITE BATCHING PLANT (EV)"]
    RAW["Stone / Clay / Quicklime<br/>Steel / Stainless / Tungstensteel<br/>Lead / Europium / Naquadah Alloy"]
    RAW -->|"M-01 .. M-06"| CAS["Light Concrete 3,773<br/>Turbine Casing 798<br/>Radiation Proof 141<br/>Eu Reinforced 1,343<br/>Radiant Naquadah 1,667<br/>Iridium Reinf. 497"]
    DU -->|"M-03b shielding"| CAS
    CAS --> CORE
  end

  subgraph PROP["PROPELLANT + COOLANT"]
    HNO3["NitricAcid + N2O4"] -->|P-01 Chem Plant| OXI["Oxidiser Blend"]
    METAL["Al / Adamantium / Infinity"] -->|"P-02 IsaMill + kerosene"| SLUR["Metal Additive Slurry"]
    NAQF["GG naquadah fuels Mk I..VI"] -->|"P-03 Chem Plant Lv 1-3<br/>P-03h QFT Lv 4-6"| PROPF["naqPropellant T1..T6<br/>P = 3.0 .. 6.5"]
    OXI --> PROPF
    SLUR --> PROPF
    PROPF --> SPIRAL["Spiral chamber<br/>8 timed injectors"]
    COOLB["C-02/C-03 bulk<br/>IC2 / Super Coolant"] --> SPIRAL
    SPIRAL --> EU
  end

  style POW fill:#1f6f4a,color:#fff
  style PW fill:#1f4f8f,color:#fff
  style CORE fill:#8f3a1f,color:#fff
  style EU fill:#8f7a1f,color:#fff
```

**Reading the chart.** The four blue-to-green edges out of `Purification Plant` are the ruling:
three **YIELD** edges (×0.5 … ×4.5) into `Ceramic-Grade UO2 Powder` (F-12), `Depleted UO2 Powder`
(F-20) and `ThO2 Powder` (F-21a), and one **OUTPUT** edge (×1.00 … ×1.40) into `Reactor Silo`. The
dashed edges are the boosts that keep existing content alive — the LFTR line's UF₆ feeding our
cascade, the Annex paying yellowcake into the front end's own product, the kept Centrifuge route,
the reactor's own waste-water emission, and the uncooled reprocessing path that unblocks a legacy
world's depleted-fuel backlog.

---

## 10. AS-BUILT — what actually shipped, and where it differs from §§1–9

**Everything above this line is the design of record; everything below it was read back out of the
Java after the fact.** Where the two disagree, this section is right and the design section is the
history of how we got there. Every figure here was produced by grepping
`BTSRecipesFuel/Materials/Propellant/Coolant.java`, `BTSWaterGrade.java` and
`MTEReactorCoreMk1.java`, not by trusting an implementation report.

### 10.1 The count, derived

| File | Owner | `.addTo()` sites | Recipes actually registered |
|---|---|---|---|
| `BTSRecipesFuel.java` | A — Fuel | 47 | **109** (108 new + 1 kept) |
| `BTSRecipesMaterials.java` | B — Materials | 12 | **12** |
| `BTSRecipesPropellant.java` | C — Propellant | 14 | **32** |
| `BTSRecipesCoolant.java` | C — Coolant | 3 | **3** |
| `BTSRecipesMachines.java` | D — Machines | 15 | **15** (11 moved verbatim + 4 new controllers) |
| | | | **171 total, 156 in the four production lines** |

Sites ≠ recipes because nine helpers and three loops multiply: `pressGreenPellet`/`sinter`/`grind`/
`loadRod`/`bundle` are called **three** times each (uranium, MOX, thorium),
`coolInPond`/`chopLeach`/`rushLeach`/`crudeCentrifuge` **eight** times each (four grades × rod and
assembly), `mill`/`slurry`/`blendLow`/`blendHigh` three times each and `cartridge` six times (twice
over, both legs), and `reconversion()`, `moxGrade()` and `thoriumGrade()` each loop the **nine**
water rungs.

**What moved since the first as-built pass (132 → 156):** F-20 grew a nine-rung ladder (+8), F-21a
grew one (+8), and the eight `crudeCentrifuge` recipes came back (+8). The four new controller
recipes (+4) land in the machines file, outside the production-line total. §10.8 has the reasons.

The flowchart computes its own figure from its own step table rather than quoting this one, so the
two cannot silently drift; if they disagree, one of them has not been regenerated.

**Machines: 4 new, 21 existing = 25**, exactly as §6 budgeted, against a budget of 30 — though not
the same 21 §6 listed. **The Extruder is not used** (F-16 moved to the Assembler, §10.3 row 7) and
the **Fluid Extractor** is, for P-04's return leg. Count unchanged; membership swapped. MTE ids
28113 (Cascade Hall), 28110 (Fuel Fabrication Hall), 28111 (PUREX Canyon), 28112 (Site Batching
Plant), all confirmed against `BTSMachines.register()`. The Cascade Hall moved off 28101 at the
release pass — see §5 and §10.8.4.

### 10.2 The water ladder, as coded

`BTSWaterGrade.java` is the single source and is frozen. Verified by reading it:

```java
yieldMilli(0) = 500;  yieldMilli(g) = 1000 + (g - 1) * 500;   // distilled x0.5 .. Grade 8 x4.5
outputMilli(g) = (g < 1) ? 1000 : 1000 + g * 50;              // x1.000 .. x1.400
powderFor(g)   = 4 * yieldMilli(g) / 1000;                    // 2,4,6,8,10,12,14,16,18
```

**Three steps carry the yield term, not one.** `BTSRecipesFuel.gradedOutput(grade, base)` is
`base * yieldMilli(grade) / 1000` and is the same arithmetic `powderFor` performs at base 4. The
three bases:

| Step | Machine | Base @ Grade 1 | Ladder (distilled → Grade 8) |
|---|---|---|---|
| **F-12** reconversion | Chemical Plant t3, IV, 30 s | 4 | 2, 4, 6, 8, 10, 12, 14, 16, **18** |
| **F-20** tails reconversion | Chemical Plant t3, IV, 18 s | 8 | 4, 8, 12, 16, 20, 24, 28, 32, **36** |
| **F-21a** thoria calcining | Chemical Dehydrator, IV, 25 s | 6 | 3, 6, 9, 12, 15, 18, 21, 24, **27** |

All three bases are even, which is what makes every one of the twenty-seven rungs an integer, and
the largest output is 36 — inside one stack. **Grade 1 is the identity rung**, so growing a ladder
on a step that used to be flat cost a standing line nothing: F-20 gives 8 powder per 2,000 L of
tails in 18 s where it gave 20 per 5,000 L in 45 s — identical per litre and per second.

The reactor's sixth factor is present at `MTEReactorCoreMk1.euDemandOf(...)` as
`* Math.max(1000, waterMilli) / 1000`, and `feedwaterGrade` is a synced field written by
`drawFeedwater()`, by `runFluidTick()`'s Hot Coolant branch and by `clearCampaign()` — three
writers, not one, which the field's own javadoc got wrong until the release pass. The
**decay-heat** draw deliberately does not write it: a shut-down plant earns no output term.
**The `Math.max(1000, ...)` floor is what makes the release regression-safe:**
plain and distilled water both grade to 1000, so a standing plant multiplies by exactly 1 and
derives a bit-identical EU figure to 0.23.0.

All three steps ask `BTSWaterGrade` for their output count rather than typing it, and each skips
any rung whose fluid the pack does not have (`fluidFor` returns null) instead of throwing — recipe
registration runs in postInit, where one escaping exception costs the whole mod. All three register
**best rung first**, because GT walks a recipe map in insertion order when matching on fluids, so a
line piped with Grade 8 spends it. That is the exact opposite of the reactor's worst-first feedwater
sweep, and for the opposite reason.

### 10.3 Forced deviations from §§1–4 — twelve, each with the reason

The design established *which* recipe maps an addon may register into, but not their **shape**.
`maxIO` was read out of each map's static initialiser during implementation, and five of the
tables in §1 could not be expressed as drawn.

| # | Design said | Built as | Why |
|---|---|---|---|
| 1 | F-01 mills with wash water, drops gangue | no fluids, one output, **and the milling ball IS a recipe input** | `millingRecipes` is `maxIO(3,1,0,0)` — **zero fluid slots**, so the wash water and the gangue byproduct move to F-02b. The first draft of this row claimed the ball "comes from the mill's own hatch, so a ball in `itemInputs` would have made every recipe eat one". **That is backwards and it shipped as a dead recipe.** `MTEIsaMill$1.validateRecipe` walks `recipe.mInputs` for a stack `MTEIsaMill.isMillingBall` accepts and returns `NO_RECIPE` when it finds none, whatever is in the ball hatch. The recipe NAMES the ball (at stack size 0, GT's "required but not consumed"); `searchForMillingBall` then matches that name against the hatch and `onRecipeStart` damages the **hatch's** stack. Declared by the recipe, supplied by the hatch — both. Alumina everywhere, on all four milling recipes plus P-02a |
| 2 | F-02 Flotation Cell → concentrate (solid) | → **froth FLUID**, new F-02b dewaters it | `flotationCellRecipes` is `maxIO(6,0,1,1)` — **zero item outputs**. GT++'s own sphalerite line has the same shape. This is why `uraniniteFlotationFroth` exists |
| 3 | F-03 leaches with an Fe(III) oxidant | **dust Pyrolusite** | The Digester allows one fluid in. Pyrolusite is the solid oxidant a real acid-leach flowsheet charges anyway — the constraint improved the chemistry |
| 4 | F-11c byproduct is calcium **chloride** | calcium **fluoride** (dust Fluorite) | There is no chlorine anywhere in this chain. Reducing a *fluoride* with calcium gives a fluoride, and 8 Ca in balances 8 fluorite out |
| 5 | F-15 selects grade by circuit | grade-**tagged** sintered pellets | `latheRecipes` is `maxIO(1,2,0,0)` — one input, so **no circuit slot**. Hence `sinteredPelletMox` / `sinteredPelletThorium` |
| 6 | F-13/F-14 select grade by circuit | circuit **plus** tagged items | A circuit cannot survive an item. One fungible green pellet would let uranium powder be pressed, sintered and ground on circuit 2 into MOX pellets **with no plutonium in the line** |
| 7 | F-16 extrudes the cladding tube | **Assembler**, behind circuit 7 | `extruderRecipes` is `maxIO(2,1,0,0)` and a die spends both slots, so the recipe would be indistinguishable from GT's own `ingot + die` and one of the two would silently become unreachable |
| 8 | P-02 mills metal in kerosene, one step | split **P-02a / P-02b** | Same zero-fluid-slot problem as F-01 |
| 9 | P-01 uses `Materials.DinitrogenTetroxide` | `getGas`, and **both** N₂O₄ fluids | GT's N₂O₄ is gas-side, and the pack has two of them (GT core's and `GTPPFluids`') — registering both means a standing rocket-fuel line is not forced to build a second one |
| 10 | C-04 on `UniversalChemical` | straight to the **LCR** | `UniversalChemical` fans out to the single-block reactor, which is `maxIO(2,2,1,1)` — one fluid input, and C-04 has two |
| 11 | M-02d presses filter casings | folded into M-02c | Pressing implies a blank, which implies a new item; `BTSItemsMaterials` registers nothing by decision. Everything in the materials file goes to **one** map |
| 12 | M-05 makes 8 casings a batch | 20 (M-05a) / 32 (M-05b) | 8× would want 96 plates in one slot and **a stack stops at 64**. The same limit forced M-04 off fasteners (8 casings = 192 screws) and onto diffusion bonding |

### 10.4 Three judgement calls the implementers made, recorded here

1. **The propellant ladder was NOT retired.** §4.1 and §8.1 said the six one-step Mixer recipes
   were "kept but rebuilt" — i.e. replaced. They are **kept standing, unchanged**, and the
   three-stage chain was added beside them. My ruling that fuel-chain automation may break
   covers the *fuel* chain; no ruling covers the propellant ladder, and retiring the only recipe
   that makes the fluid a running plant drinks would stop that plant dead on jar swap. The design's
   P-03 as written was strictly dominated anyway (same naquadah fuel **plus** oxidiser and slurry),
   so nobody would ever have built it.
2. **C-01 was deliberately not shipped.** As specified it consumed ion-exchange resin and returned
   its own input — a no-op that bills the player for nothing and a loop an autocrafter can chase.
   Its stated purpose (showing the ladder's bottom in NEI) is already served by
   `BTSDisplayRecipes.plantCoolant`.
3. **Three ultrafine metal powders were added** to `BTSItemsPropellant`, which §7.2 had scoped to
   cartridges only. One fungible powder would have let cheap aluminium be blended into an
   Infinity-grade slurry — the same "a circuit cannot survive an item" rule that F-13/F-14 rest on.

### 10.5 Two defects the implementers caught, and what they cost

- **F-07's ammonium sulfate liquor was a stall, not untidiness.** It had a producer and no
  consumer, and **a GT machine with a full output hatch stops.** The first player to automate F-07
  without a void hatch would have watched the reactor's fuel supply halt on a byproduct. F-07b now
  cracks it, returning 300 of the 1,000 L of ammonia. Every one of the fuel line's 21 fluid streams
  and 30 item intermediates was then re-walked for the same defect.
- **`OrePrefixes.screw.mMaterialAmount` is M/8, not M/9.** M-04's europium was computed on the
  wrong assumption and would have shipped **7 % cheaper in europium** than the stock recipe it sits
  beside — silently, in the one line item where 1,343 casings makes 7 % matter. The amounts are now
  read off the decompiled `OrePrefixBuilder` calls rather than from memory.

### 10.6 §8.3 — DONE. The Annex bypass is closed.

Both call sites pay **Yellowcake** now, at §8.3's ratio exactly:

```
machines/MTEEnrichmentTrain.java:163   new ItemStack(BTSItemsFuel.yellowcake, OUTPUT_YELLOWCAKE)   // 2
machines/MTEReactorCoreMk1.java:6081   out.add(new ItemStack(BTSItemsFuel.yellowcake, annexYellowcake()));
```

`8 Uraninite + 2,000 L HF → 2 Yellowcake + 2 Thorium` every 600 powered ticks is **4 Uraninite per
cake** — the same ratio F-01 … F-08 reach by hand (16 → 4). The module therefore buys *simplicity*,
never yield: it replaces the eight-step front end and nothing else, the line rejoins at F-09, and
F-09 … F-18 stay compulsory. The output lands on a real consumer — `BTSRecipesFuel:352` emits four
yellowcake at F-08 and `:369` consumes four at F-09.

**The milestone interaction that a plain two-line change would have silently broken.**
`BTSProgression.annexPellets(base)` is `max(base, base * 6/5 * ANNEX_BATCH / 1000)`. REPROCESSING
tier 7 ("YIELD UPLIFT — the batch grows a fifth") turned 20 into 24; but `2 * 6 / 5` truncates
straight back to **2**, so re-pointing with a plain multiply would have turned the milestone into a
no-op that still advertised itself on the panel. `annexYellowcake()` instead reads
`progression.annexPellets(1000)` back as a **milli multiplier** and carries the fraction across
batches off the already-persisted `annexCycles`: at ×1.200 it pays 2, 2, 3, 2, 3 — twelve cakes per
five batches, the promised ×2.4 — and at ×1.000 every term is exactly 2. No new NBT field, and a
wrenched controller resumes the sequence rather than restarting it.

**Still outstanding, in a file this pass did not own:** the panel string at
`progression/BTSMilestone.java:81` still reads `YIELD UPLIFT - Annex batch 24 pellets`. Every
surface that quotes it (wiki §6.2, the questbook's *The Four Lines*) prints the correction beside
the quote; the string itself wants one line of Java.

### 10.7 What still cannot be settled statically

None of the four new multiblocks has been **formed in world**. Their recipe shapes are proven
against the map limits and their structure definitions compile, but the Cascade Hall's negative
depth offsets, the Fabrication Hall's 7×5×12 and the Canyon's 9×7×9 want a `/btsbuild` projection
test before 0.24.0 ships (standing rule, D-19). Two further first-load questions are worth watching:
whether the circuit numbers actually disambiguate M-03a/M-03b/M-03c in a bus holding several
programmed circuits, and whether one Input Hatch comfortably holds M-05b's 13,824 L × parallel.

---

### 10.8 THE RELEASE PASS — what the last review changed

Twenty findings, nine blocking. All fixed. Six of them moved something a player can feel; the rest
were dead recipes that could never have run, and comments that argued confidently for the wrong
behaviour. **Every GT-internals claim below was re-established against bytecode in
`GT5-Unofficial-5.09.52.594-dev.jar`, because three of the findings were exactly the failure of not
doing that.**

#### 10.8.1 The water ladder reaches all four fuel grades (M8)

§2.3 argued the yield term should sit on F-12 alone. It was wrong, and §2.3 now carries the
retraction. F-20 (base 8) and F-21a (base 6) grew nine-rung ladders; the arithmetic is
`gradedOutput(grade, base)` over `BTSWaterGrade.yieldMilli`, so no rung is typed.

Writing `f` for the share of a fuel grade's ceramic mass that comes from a graded step, feed cost
across the ladder is `(1 − f) + f/Y` and its span is the yield effect that grade actually feels:

| Fuel grade | `f` before | span before | `f` now | span now | vs OUTPUT ×1.40 |
|---|---|---|---|---|---|
| uranium | 1.0000 | ×9.00 | 1.0000 | ×9.00 | held → held |
| MOX | 0.0000 | **×1.00** | 0.9375 | ×7.15 | **was INVERTED** |
| thorium | 0.0625 | **×1.12** | 1.0000 | ×9.00 | **was INVERTED** |
| naquadah | 0.5714 | ×2.83 | 0.5714 | ×2.83 | held → held |

MOX is 30 parts F-20 powder to 2 parts PuO₂, so `f = 30/32`; thorium is 30 parts F-21a thoria to 2
parts F-12 ceramic, both graded, so `f = 1`; naquadah is 8 sintered pellets against 4
enriched-naquadah and 2 U-235 dust in F-22, so `f = 8/14` (its HEU-UF₆ rider comes off F-11b,
upstream of F-12, and is ungraded). On two of the four grades the *reactor's* water bonus used to be
the larger effect — §2.4's guard was not weak, it was backwards. Nothing got dearer: Grade 1 is the
identity rung.

#### 10.8.2 The eight depleted-fuel Centrifuge recipes came back (B6)

§8.1 retired them and §1.6 replaced them with PUREX. Every replacement sink is on the Canyon's own
map, so on update day a world's existing centrifuge array stopped dead and its stock of spent fuel
had **no route at all** until an IV multiblock stood. Restored verbatim from `git show 5344bcf` —
same outputs, chances, durations and EU tiers, and still no programmed circuit, because adding one
would have invalidated exactly the automation they exist to protect. PUREX stays strictly better
(ceramic straight back into F-13, hulls for R-04, waste water for D-09) and is now an upgrade
rather than a toll gate. §8.1 and §8.4 are corrected.

#### 10.8.3 Decay-heat cooling reads the same feedwater ladder the running plant does (B7)

D-26 widened the *running* draw to eleven fluids and left the *post-shutdown* draw on its original
three (Water, Distilled, IC2 Coolant). A plant plumbed for exactly what this release rewards — one
purified-water feedwater hatch and nothing else — therefore ran perfectly and then could not pay
its decay heat: a guaranteed meltdown handed to the player who followed the tooltip. Both callers
now go through one `drainFeedwaterLadder(litres, acceptCoolant)` and cannot drift apart again.
Order for the three previously-accepted fluids is byte-identical; the purified rungs are appended
last, so no standing plant's spend order changed. Offering ten fluids a tick is free:
`MTEHatchInput` inherits `drain` from `CommonMetaTileEntity`, which returns null unless
`getFluid().isFluidEqual(want)` — one comparison per miss, which is also precisely why the old
three-fluid list came up dry against a purified hatch instead of falling back to it.

The `/btssupply` waiver is now symmetric too: `refillDebugSupplies` deliberately never materialises
feedwater, so a commanded plant melted down the moment it was switched off with nothing a player
could put in a hatch to stop it. The destruction policy itself is untouched.

#### 10.8.4 The Cascade Hall moved off a burned MTE id (M9)

§5 assumed `base+1` was unused. It is *retired*, not free: tag **0.5.0** ships
`MTETurbineHall(base + 1)`, so a world saved on it has a Turbine Hall bound to 28101 and handing
that id to a new controller resolves every one of them into a Cascade Hall on load — the exact rule
that freezes the four new ids is what forbids reusing that one. **Cascade Hall is 28113.** Per-tag
id ledger: `base`, `base+1` burned, `base+2 … base+9` live (two of them retired in place),
`base+10 … base+13` the four new. Next free is `base+14`; `gregtech.api.enums.MetaTileEntityIDs`
leaves 28056–29989 empty for the whole pack, so there is room.

#### 10.8.5 Four dead recipes, and four machines with no recipe at all

- **B2 / B5 — the milling ball is a RECIPE INPUT.** `MTEIsaMill$1.validateRecipe` iterates
  `recipe.mInputs` for `MTEIsaMill.isMillingBall` and returns `NO_RECIPE` when it finds none,
  whatever is in the ball hatch. F-01 (×2), F-19, F-21b and P-02a (×3) had none and could never
  run — the whole propellant chain was dead at its first step. All now carry
  `GregtechItemList.Milling_Ball_Alumina.get(0)`, stack size 0, exactly as GT++'s own
  `BaseItemMilledOre` does. Alumina everywhere: GT++ pairs a soapstone entry with every alumina one
  for a *yield* ladder, not for compatibility, and `millingRecipes` is `maxIO(3,1,0,0)` — F-19 and
  F-21b already spend two slots on reagents, so no circuit fits to key a pair apart. See §10.3 row 1.
- **B3 — F-02 needed a `milled*` OreDictionary name.** Both flotation implementations call
  `getMilledStackName(recipe)` first, which walks `mInputs` for an ore name starting with the
  literal `"milled"`; no such input, no recipe. `registerOreDictNames()` runs first in `register()`
  and calls `GTOreDictUnificator.registerOre(OrePrefixes.milled, Materials.Uraninite, …)`, so GT
  builds the string itself and it cannot drift. No collision: GT ships no milled items, has no
  `ProcessingMilled` handler, and `BaseItemMilledOre`'s twelve materials do not include Uraninite.
- **B4 — F-18n was silently discarded at postInit.** `GTRecipeConstants.AssemblyLine` is an emitter,
  not a plain map: `forceOreDictInput()`, `validateInputCount(4, 16)`, `validateOutputCount(1, 1)`,
  `validateOutputFluidCount(-1, 0)`, `validateInputFluidCount(1, 4)`, and any failure returns
  `Collections.emptyList()` with no log line. It shipped with **three** item inputs, so
  `fuelAssemblyNaqBonded` had no recipe anywhere in the mod — a regression against 0.23.0. Fourth
  input is `4 × zircaloy4 plate`, the bundle's outer channel box.
- **B1 — the four new controllers had no recipe.** Every one of the four multiblocks §5 specifies
  was unobtainable outside `/bts kit`. All four are Assembler jobs on fresh circuits 14–17, each
  built out of the casing it is walled in, and every one of those casings keeps its stock GT recipe
  so there is no bootstrap loop.

#### 10.8.6 Two more, in the same class

- **The QFT circuit trap (P-03h).** `MTEQuantumForceTransformer.findProgrammedCircuitNumber` scans
  the input **buses** before the controller slot and hands `damage − 1` to `getOutputChances` as the
  index of the output to focus. Circuit 5 named index 4; P-03h has one output, at index 0. With
  Neptunium plasma anywhere in the hatches and a tier-3/4 shielding core, the non-focused branch
  sets every chance to 0 — Lv 4–6 would have burned naquadah fuel, oxidiser, slurry and UHV+ power
  for nothing. Circuit 1 pins the focus to the only output there is.
- **M7 — the Annex and the Train pay Yellowcake.** §10.6.

#### 10.8.7 Documentation defects the same pass found

Recorded here because they are the same failure mode as the code ones — a confident claim nobody
checked. The questbook taught the **deleted** ZPM Assembler recipe for the naquadah rod word for
word; the wiki's entire "Fuel chain items" reference block quoted five retired recipes as crafting
hints; the wiki printed the plant's EU formula as `× rpm/1000` where `euDemandOf` divides by
**500,000** (a figure 500× out for anyone back-solving); the questbook and the wiki both asserted
that nothing produced `nuclearWasteWater` before R-02h/R-03, when `MTEReactorCoreMk1.emitWasteWater`
has emitted one litre per live pillar per generating tick since before 0.20.0; and the F-01 quest
note stated the milling-ball rule exactly backwards, in the same words the code comment did.

---

## 11. THE FLOWCHART, BUILT

**`docs/production-flowchart.html`** — a self-contained page: no external requests, full light and
dark theming through CSS tokens (never defined only inside a media query), and a theme toggle that
remembers its setting. It carries the master diagram as **plain SVG** — deliberately not canvas,
which cannot be screenshotted reliably outside a `requestAnimationFrame` turn — plus every one of
the **68** steps as a card with its machine, tier band and real inputs and outputs.

**Every count on the page is summed from the step data, never typed.** The masthead reads
**156 registered recipes · 68 steps · 4 new multiblocks**, and the five rail heads read 74 (fuel),
35 (reprocessing), 12 (material), 32 (propellant) and 3 (coolant). Fuel + reprocessing = **109**,
which is exactly what `BTSRecipesFuel.java` registers — the two halves of that file are drawn as two
lanes. That is why §10.1's table and this page cannot silently drift: if they disagree, one of them
has not been regenerated.

### 11.1 The release-pass re-verification

Re-audited after this round's recipe changes, over the same method: `python -m http.server 8731
--bind 127.0.0.1` from `docs/`, opened in Chromium through the Playwright browser tools, audited in
the DOM, screenshotted in **both** explicit themes, server shut down.

| Check | Result |
|---|---|
| Console messages, all levels (error, warning, info, debug) | **0** |
| Edge segments passing through a node box | **0** — every `path.e` sampled at 2 px against every `rect.nbox` |
| SVG text runs overflowing their own `nbox` | **0** |
| SVG text runs overflowing the `viewBox` | **0** — checked with the full parent-transform chain applied |
| Free labels drawn over a node box they do not belong to | **0** |
| Horizontal page overflow at 1440 px | **0** (`scrollWidth − clientWidth = 0`, no element wider than the viewport without its own `overflow-x`) |
| Light theme | renders, `body` background `rgb(242, 244, 247)` |
| Dark theme | renders, `body` background `rgb(11, 14, 18)`, ink `rgb(204, 213, 222)` |
| Water table | 11 rows, three graded columns, values match `BTSWaterGrade` at bases 4 / 8 / 6 |

**A note on the audit itself.** The first run of the text check reported 35 false positives, because
`text.parentNode` for a free-floating SVG label is the `<svg>` element, and
`svg.querySelector('rect.nbox')` then returns the *first* node box on the page — so every lane label
was measured against an unrelated box. The check now scopes to `g > rect.nbox` and walks the real
transform chain. Worth recording: an audit that reports defects it cannot have found is worse than
no audit, because the next person turns it off.

### 11.2 What changed on the page this round

- **F-01, F-19, F-21b, P-02a** now name the Alumina Milling Ball, and F-01's note no longer states
  the rule backwards.
- **F-20** and **F-21a** are hero cards with nine rungs each; the master SVG grew a **second YIELD
  edge** into the `F-19 … F-23` box, and that box is drawn in the water colour.
- The water table gained **F-20** and **F-21a** columns, computed from the same
  `base × yieldMilli / 1000` the Java performs.
- The formula block printed **`x rpm/1000`**. It is `rpmMilli / 500_000`. Fixed, with the divisor
  spelled out so it cannot be "corrected" back — a player back-solving off that line was 500× out.
- **R-00** added: the eight kept Centrifuge recipes, with what they do *not* give back.
- The reprocessing phase note and the `kept` card no longer claim `nuclearWasteWater` had no
  producer.
- **P-03h** documents the circuit-1 focus trap.
- **F-23** and **F-18n** carry their real input lists, and F-18n carries the reason it shipped
  unregistered.
- A note in §3 gives the **four new controllers' Assembler recipes** — they had none — and the
  28101 → 28113 move.

The mermaid graph in §9 is kept as the design-time sketch. **Where it disagrees with the HTML, the
HTML is as-built** — §9 still draws F-16 on the Extruder and F-02 producing a solid, both of which
§10.3 shows could not be registered that way.
