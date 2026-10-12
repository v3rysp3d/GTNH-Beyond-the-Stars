# Singularity Ladder: build spec for P1-P3

Status: build spec, 2026-10-12. This covers P1 (Foundation), P2 (Kerr T1/T2) and P3 (Eye of Harmony T1-T4).
**ON HOLD, do not build:** the Homogeny (all tiers), Reality Diffraction and Reality Refab. P4 (Quasar, Conduit,
Eye T5-T8), P5 (MAX) and P6 (questbook) come later.

Sources: `PROPOSAL.md` D-27..D-48 (the latest ruling wins), `designs/docs/{yours,mine,multis,questbook}.md`,
`designs/recipes.js` (groups `kerr`, `eye1`, `eye4`), `designs/voxel.js` (`buildKerr`, the Eye `BANDS`) and
`designs/bh-model.js`. Pack facts come from `javap` on the pinned instance jars (gregtech-5.09.52.594,
GTNewHorizonsCoreMod-2.8.279). Every resolved conflict is logged in
[`singularity-ladder-questions.md`](singularity-ladder-questions.md) (Q-1..Q-14). This document is what gets built;
the questions record what the player still has to rule on.

Code references below use `<pkg>` = `com.nateyburr.beyondthestars`.

---

## 0. Pack facts this spec depends on (verified 2026-10-12 with javap)

| Fact | Where it comes from | Why it matters |
|---|---|---|
| **TecTech's EOH is the pack's ONLY producer of White Dwarf Matter, Black Dwarf Matter, Universium and Raw Star Matter.** `EyeOfHarmonyRecipe.SPECIAL_FLUIDS[10]`: rocket tier 0-2 gives molten WDM (1,152 / 4,608 / 18,432 L), 3-5 gives BDM, 6-9 gives Universium. Every recipe also gives `RawStarMatter` ×(min(8,R)+1)·100,000 L and every valid plasma ×(min(8,R)+1)·8,000,000 L. No other class in the gregtech jar outputs any of the four (the other references are consumers: EIC magneto recipes and nanites, NaniteChain, BEC metamaterial, Nano Forge, Exo-Foundry, CoAL misc, Godforge upgrade costs, research-station UXV parts). | `tectech.recipe.EyeOfHarmonyRecipe` `<clinit>` and constructor | Removing TecTech's recipes (D-28) cuts all four supplies. See Q-3. |
| `Materials.RawStarMatter`'s local name is **"Condensed Raw Stellar Plasma Mixture"**. | `MaterialsInit`: `setName("RawStarMatter").setDefaultLocalName("Condensed Raw Stellar Plasma Mixture")` | Our "Raw Stellar Plasma Mixture" **is** this material (Q-1). |
| TecTech EOH per-recipe numbers: H and He = (R+1)·10⁹ L each; base time = 18,000 s · 1.4^R; base success = 1 − 0.05R; EU efficiency = 0.6 + R/10. The Deep Dark (`"DD"`) is a special R9 recipe that outputs every ore and every plasma. Recipes are built in `TecTech.onLoadCompleted` (`FMLLoadCompleteEvent`) into `TecTech.eyeOfHarmonyRecipeStorage` (`private HashMap<String,EyeOfHarmonyRecipe> recipeHashMap`) and mirrored into `TecTechRecipeMaps.eyeOfHarmonyRecipes` for NEI. | `EyeOfHarmonyRecipeStorage$2`, `timeCalculator`, `specialDeepDarkRecipe` | The Eye unit converts these recipes instead of rewriting ore tables (section 3.3), and the removal runs at load-complete (section 4). |
| TecTech EOH could burn `RawStarMatter` in place of H/He, and it **returned EU**: `euOutput = euStartCost × (0.6 + R/10)`, so R5+ was a net EU source. | `MTEEyeOfHarmony` tooltip, `EyeOfHarmonyRecipe` constructor | Our Eye returns no EU (Q-4). |
| Black Hole Compressor: `MTEBlackHoleCompressor`, maps `RecipeMaps.compressorRecipes` + `neutroniumCompressorRecipes`; parallel = 8 × `GTUtility.getTierExtended(maxInputEu)`, ×2/×4 by hole stability, unbounded for non-black-hole recipes once stabilised; catalyst cost is `Materials.SpaceTime.getMolten(1)`. Casings: `Casings.ExtremeDensitySpaceBendingCasing`, `Casings.BackgroundRadiationAbsorbentCasing`, `Casings.HawkingRadiationRealignmentFocus`, frame `Materials.NaquadahAlloy`. Seeds: `ItemList.Black_Hole_Opener` ("Black Hole Seed"), `Black_Hole_Closer` ("Black Hole Collapser"), `Black_Hole_Stabilizer` ("Superstable Black Hole Seed"). | `MTEBlackHoleCompressor` | Kerr parity targets; SpaceTime is a UIV-available material because the BHC already burns it. |
| The BHC controller recipe is in `com.dreammaster.gthandler.recipes.AssemblingLineRecipes` (modid `dreamcraft`). | GTNewHorizonsCoreMod jar | Section 4. |
| Assembly-line recipes live in `RecipeMaps.assemblylineVisualRecipes` (NEI) **and** `GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes` (what data sticks resolve against, through the lazily built `AssemblyLineUtils.AL_RECIPE_LOOKUP`). Research-station recipes also sit in `TecTechRecipeMaps.researchableALRecipeList` and `researchStationFakeRecipes`. `RecipeMapBackend.removeRecipes(Collection)` exists. | `GTRecipe$RecipeAssemblyLine`, `AssemblyLineUtils`, `RecipeMapBackend` | Section 4. |
| The pack has no `MaterialsUEVplus`. "Tachyon Rich Temporal Fluid" is `Materials.Time` (name `temporalFluid`); "Spatially Enlarged Fluid" is `Materials.Space`. Superconductor wire materials go up to `Materials.SuperconductorUMV`, with no UXV. | `MaterialsInit` | Recipe translation. |
| Wireless EU: `gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap(UUID, long/BigInteger)` (returns false when a debit cannot be paid), `strongCheckOrAddUser(UUID)`, `getUserEU(UUID)`. | `WirelessNetworkManager` | Eye power. |
| `tectech.util.ItemStackLong` / `FluidStackLong` carry `long` sizes; TecTech EOH output is ME-only for that reason. | `tectech.util` | Eye outputs. |
| The ResearchStation scan of `addEOHRecipes` found WDM, BDM and MHDCSM only as alternative bolts in the **TecTech EOH controller** recipe, and WDM, BDM and Eternity only in the recipe after the Tier-8 Spacetime Compression generator. The three Tier-8 ("Gallifreyan") field-generator recipes carry none of those markers. | `tectech.loader.recipe.ResearchStationAssemblyLine` | Our Eye controller can keep the card's Gallifreyan field generators (the Eye unit re-verifies, Q-7). |

---

## 1. P1 item, fluid and casing list

**EXISTING** = already in the pack (exact reference given). **NEW** = registered by this addon. The addon registers
no GT `Materials` and no Werkstoff: new matter is a `GTFluidFactory` fluid, new parts are plain `Item`s, and new
casings are BTS meta-blocks.

### 1.1 Fluids

| Name (display) | Status | Reference / internal name | Notes |
|---|---|---|---|
| Raw Stellar Plasma Mixture (RSPM) | **EXISTING** | `Materials.RawStarMatter.getFluid(n)` (shows as "Condensed Raw Stellar Plasma Mixture") | Eye T1 (Sol) output. Reused, not re-registered (Q-1). After D-28 our Eye is its only source, so the Godforge fuel, EIC, mixer and nanite recipes that drink it keep a supplier. |
| Excited Stellar Plasma Mixture (ESPM) | **NEW** | `btsexcitedstellarplasma`, liquid, 50,000 K, RGB 255,194,58 | Eye T2 (Sirius A) output. While the Homogeny is on hold, Eye T2 is its only source (Q-2). |
| Raw Hyperdense Stellar Plasma (RHSP) | **NEW** | `btsrawhyperdenseplasma`, liquid, 80,000 K, RGB 210,72,42 | Eye T3 (Arcturus) output. D-42's text says "raw hyperdense star plasma" and the cards say "Raw Hyperdense Stellar Plasma"; the build uses the card name (Q-13). |
| Hyperdense Stellar Mixture (HSM) | **NEW** | `btshyperdensestellarmixture`, liquid, 120,000 K, RGB 154,26,58 | Kerr T2 output (density map). Gates Eye T4: it is in Galactic Shell Plating, the Galactic Upgrade Core and the T4 white-dwarf recipe. |
| White Dwarf Matter | **EXISTING** | `Materials.WhiteDwarfMatter.getMolten(n)` | Eye T4 (Sirius B) output. |
| Black Dwarf Matter (interim only) | **EXISTING** | `Materials.BlackDwarfMatter.getMolten(n)` | Not a P3 product by design (D-42 puts it at T5). It is made by an interim T4 recipe behind a config flag until P4 (Q-3). |
| Hydrogen / Helium | EXISTING | `Materials.Hydrogen.getGas`, `Materials.Helium.getGas` | Eye feed, drawn into long buffers like TecTech's. |
| Molten SpaceTime / Transcendent Metal / Infinity / Cosmic Neutronium | EXISTING | `Materials.SpaceTime/TranscendentMetal/Infinity/CosmicNeutronium.getMolten(n)` | |
| Super Coolant | EXISTING | `Materials.SuperCoolant.getFluid(n)` | |
| Excited DTEC | EXISTING | `Materials.ExcitedDTEC.getFluid(n)` | |
| Lubricant | EXISTING | `Materials.Lubricant.getFluid(n)` | |

Registration: a new `fluids/SLFluids.java` with the same `liquid(...)` / `build(...)` helper as
`BTSFluidsProcess.java:96-105`, called from `BTSFluids.register()` on one appended line after
`BTSFluidsPropellant.register()`.

### 1.2 Blocks (structure casings)

Three new meta-blocks in `blocks/`. They are tile-less and solid (glass is translucent), with hardness and harvest
like GT casings, an `ItemBlock` with metadata, and one icon per meta at `textures/blocks/sl_<name>.png`. Textures
are generated from the voxel atlas painters (section 5.5), so the in-game face matches the mockup tile.

`BlockSLCasing` (`slCasing`), 16 metas:

| Meta | Name | Voxel kind | Used in | Texture idea |
|---|---|---|---|---|
| 0 | Containment Deck Plating | `deck` | Kerr cage decks; Eye ground plinth | grey plate with a stud grid (atlas `deck`) |
| 1 | Superradiant Mirror Casing | `mirror` | Kerr inner and outer mirror walls (2,852) | pale cyan with diagonal glints |
| 2 | Mirror Backing Casing | `mirrorback` | behind every mirror (3,551) | slate casing, line motif |
| 3 | Bolt Injector Rail (Mk I) | `injector` | the 8 angled injector rails (232) | blue-slate with a cyan bore |
| 4 | Bolt Injector Rail (Mk II) | `injector` | same cells | same bore, amber ring |
| 5 | Bolt Injector Rail (Mk III) | `injector` | same cells | same bore, magenta ring |
| 6 | Ergosphere Field Coil | `coil` | Kerr dome coils (842) | copper coil stripes |
| 7 | Beam Focusing Quadrupole | `quad` | injector and beamline rings (208) | purple casing with a pink cross |
| 8 | Beam Dump | `dump` | Kerr west dump arc | yellow and black hazard |
| 9 | Eye Shell Casing | `eyeshell` | Eye core, pylons; Kerr governor and dump arcs | dark shell, line motif |
| 10 | Beam Band Casing | `ring2` | Eye T2 beam band | steel-blue grid |
| 11 | Jet Crescent Casing | `ring3` | Eye T3 jet crescent | teal grid |
| 12 | Galactic Shell Plating | `galactic` | Eye T4 spiral-arm blades | navy hex with a blue rule |
| 13 | Bastion Plate | `bastion` | Kerr T2 hoops, band and struts (8,316) | black grid with gold rules |
| 14 | Casimir Plate | `casimir` | Kerr T2 trim (677) | gold ribbed |
| 15 | Ergosphere Extraction Casing | (`ring3` in `buildKerr`) | Kerr extractor arc shell | teal grid, pump glyph |

`BlockSLCasing2` (`slCasing2`):

| Meta | Name | Voxel kind | Notes |
|---|---|---|---|
| 0 | Kerr Compressor Module | stand-in `control` at the south arc | The module core block: one per compressor arc. Crafted from the card "Kerr Compressor Module". |
| 1 | Kerr Extractor Module | an extractor-arc shell cell | One per extractor arc. Crafted from the card "Kerr Extractor Module". |
| 2 | Harmonic Lining | `lining` | Emissive (light 12): the white inner face of every band and arc. |

`BlockSLGlass` (`slGlass`, translucent, render pass 1):

| Meta | Name | Voxel kind |
|---|---|---|
| 0 | Spatial Glass Channel | `sglass` |

EXISTING blocks the structures use:

| Voxel kind | Block | Reference |
|---|---|---|
| `casing` ("Space-Bending Casing") | Extreme Density Space-Bending Casing | `Casings.ExtremeDensitySpaceBendingCasing` (BHC casing) |
| `bhc` (compressor arc shell) | Background Radiation Absorbent Casing | `Casings.BackgroundRadiationAbsorbentCasing` (the BHC's own casing, so the compressor arc reads as "the old BHC") |
| `frame` | Naquadah Alloy Frame Box | `StructureUtility.ofFrame(Materials.NaquadahAlloy)` (as in the BHC) |
| `pipe` (beamline) | Particle Beam Guidance Pipe Casing | `Casings.ParticleBeamGuidancePipeCasing` |

Hatch textures: every new socket uses `Casings.UMVMachineCasing`'s texture index, so P1 does **not** register a GT
casing-texture page. Prefer deleting machinery over adding it.

### 1.3 Items (`items/SLItems.java`, via `BTSItems.define(name)`)

| Item | Internal name | Kind | Used by | Texture idea |
|---|---|---|---|---|
| Superradiant Mirror Segment | `superradiantMirrorSegment` | component | Kerr controller (×8) | layered mirror plate, cyan glint |
| Stabiliser Pylon | `stabiliserPylon` | component | Eye controller (×6) | dark flared strut |
| Kerr Density Upgrade | `kerrDensityUpgrade` | upgrade core | installed into the Kerr controller: Kerr T2 | `cond` orb, crimson (`0x9a1a3a`) |
| Galactic Upgrade Core | `galacticUpgradeCore` | upgrade core | installed into the Eye controller: unlocks T4 | `cond` orb, sky blue (`0x3fa6d9`) |
| Star Chart: Sol | `starChartSol` | recipe target (not consumed) | Eye T1 star recipe | data-orb disc, yellow star |
| Star Chart: Sirius A | `starChartSiriusA` | recipe target | Eye T2 | blue-white star |
| Star Chart: Arcturus | `starChartArcturus` | recipe target | Eye T3 | orange giant |
| Star Chart: Sirius B | `starChartSiriusB` | recipe target | Eye T4 | small white dwarf |
| Star Chart: Sirius B, Cooled (interim) | `starChartSiriusBCooled` | recipe target | Eye T4 interim BDM (Q-3); delete with P4 | white dwarf, frost rim |

Star picks follow mine.md (Sirius A for "a larger star", Arcturus for "a red giant"). Sirius B is our choice for the
white dwarf: it is Sirius A's companion and the best-known white dwarf (Q-13). Planets and the Deep Dark keep
TecTech's targets, the `gtneioreplugin` dimension display blocks (`EyeOfHarmonyRecipe.getRecipeTriggerItem()`).

EXISTING items the recipes use: `ItemList.{Field_Generator,Emitter,Sensor,Robot_Arm,Electric_Piston,Electric_Pump,
Conveyor_Module}_{UIV,UMV}`, `ItemList.Black_Hole_Opener/Black_Hole_Stabilizer`, circuits through
`req(OrePrefixes.circuit, Materials.UIV|UMV, n)`, `CustomItemList.EOH_Reinforced_Spatial_Casing`,
`EOH_Reinforced_Temporal_Casing`, `SpacetimeCompressionFieldGeneratorTier8`, `TimeAccelerationFieldGeneratorTier8`,
`StabilisationFieldGeneratorTier8`, `ItemList.Tool_DataOrb`, and GT parts through `BTSRecipeGuard.req(prefix,
material, n)`.

### 1.4 Cut from the cards for P1-P3 (not built)

Beam Port, Particle Beam, BEC Metamaterial Film, White Dwarf Matter Lens, Ergomatter (it is D-42's T7 output), GRB
Receptor Port, Conduit parts, and the Accretion Upgrade Core (already obsolete). In-structure `beamhatch` and
`ergohatch` cells are remapped (section 5.3). Reasons: Q-7 and Q-8.

---

## 2. Recipes

Conventions: every recipe is wrapped as `BTSRecipeGuard.recipe("<ID>", () -> GTValues.RA.stdBuilder()...addTo(map))`.
Item and fluid lookups always go through `req(...)`, `fluid(...)` or `nn(...)`. Times are in **ticks**. EU is a
`TierEU` constant. "AL" means an assembly line: `GTRecipeConstants.AssemblyLine`, with
`.metadata(GTRecipeConstants.RESEARCH_ITEM, X).metadata(GTRecipeConstants.SCANNING, new Scanning(...))`, at most 16
item inputs and 4 fluids. "ASM" is `RecipeMaps.assemblerRecipes`. Structure blocks are made on the **Assembler**, in
batches: a structure that needs thousands of a block cannot reasonably take an assembly-line craft per block (the
card "Superradiant Mirror Segment" stays an AL **component**, Q-7).

### 2.1 Progression-loop audit (D-42 matter map: T4 makes WDM, T5 BDM, T7 Ergomatter, T10 Universium)

The order a player meets these things: **Kerr T1 (UIV, the BHC's slot)** → Eye T1 (UMV) → Kerr T2 → Eye T2 → Eye T3 →
HSM → Eye T4. Every input must exist before the machine that needs it.

| Card input | Card | Problem | Substitution (built) |
|---|---|---|---|
| Dense Black Dwarf Matter Plate / BDM ingot / BDM plate | Mirror Segment, Eye Shell Casing, Stabiliser Pylon, Bastion Plate, Beam Dump, Kerr Density Upgrade | BDM is T5 (P4); TecTech's supply is removed | `plateDense Neutronium` (Bastion, Eye Shell, Pylon), `ingot CosmicNeutronium` (Beam Dump), `plateDense Infinity` (Density Upgrade); Mirror Segment drops it |
| White Dwarf Matter (plates, molten, lens, condensate) | Mirror Segment, Harmonic Lining, Jet Crescent, Galactic Shell Plating, Galactic Upgrade Core | WDM is T4's **output**; T4's own structure cannot need it | `plateDense Infinity` (Jet Crescent, Galactic Shell); HSM is the new T4 gate (Galactic Shell, Galactic Core); the lens is cut |
| Ergomatter | HSM process card | T7 (P4) | `Molten SpaceTime 288 L` |
| Raw Star Matter 4,000,000 L | Eye controller | after removal only our Eye makes it: the Eye would need itself | `Excited DTEC 256,000 L` (already on the card) + Molten Infinity; the 4M L is dropped |
| Jet Collimator Ring | Jet Crescent Casing | quasar part (P4) | 2 × Ergosphere Field Coil |
| GRB Receptor Port, Conduit Throat | Galactic Upgrade Core | P4; D-42 moved stabilisation to T7 | dropped |
| MagMatter Plate | Bastion Plate | UXV per D-48 | `plate TranscendentMetal ×2` |
| UMV Superconductor Wire, UMV components | Ergosphere Field Coil, injectors | Kerr T1 is **UIV** (D-44) and needs these blocks | `wireGt01 SuperconductorUIV`, UIV components; Mk III rails stay UMV |
| Tachyon Rich Temporal Fluid (`Materials.Time`) | Spatial Glass Channel | the Kerr (UIV) uses the glass; Time is believed to be UMV | `Molten SpaceTime` |
| Metamaterial, condensates, "Observation Array (BEC)" | Harmonic Lining, Casimir Plate, Galactic Core | the BEC condensate API is unverified on the 5.09.52.594 pin | moved to ASM/AL with molten metals |
| Beam Port | Beam Band, Jet Crescent | no beam I/O in P2/P3 | `Emitter_UMV` |
| SpaceTime at UIV | Kerr cards | OK: the BHC (UIV) already burns molten SpaceTime | kept |
| Black Hole Seed / Superstable Seed | Kerr | OK: UIV BHC items, untouched | kept |
| Gallifreyan field generators | Eye controller | OK: the javap scan finds no WDM/BDM/Universium/Eternity/MHDCSM markers in their recipes | kept; re-verify (Q-7) |
| RSPM 4,000,000 L | Kerr Density Upgrade | OK: Kerr T2 comes after Eye T1 | kept |

Wider fallout of removing TecTech's recipes (everything in the pack that eats WDM, BDM or Universium before our
Eye reaches it): Q-3.

### 2.2 Foundation recipes: `BTSRecipesSingularity.java` (parts)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-P01 | Containment Deck Plating ×4 | ASM | plate Neutronium ×6, frameGt NaquadahAlloy ×1 | CosmicNeutronium molten 576 | RECIPE_UIV | 400 |
| SL-P02 | Mirror Backing Casing ×2 | ASM | plate Infinity ×6, frameGt SpaceTime ×1 | Infinity molten 1,152 | RECIPE_UIV | 600 |
| SL-P03 | Superradiant Mirror Casing ×2 | ASM | plate Neutronium ×4, foil Infinity ×8, Mirror Backing Casing ×1 | Infinity molten 576, SpaceTime molten 144 | RECIPE_UIV | 600 |
| SL-P04 | Superradiant Mirror Segment (item) | AL (research: Black_Hole_Opener) | plate Neutronium ×32, foil Infinity ×64, Emitter_UIV ×4, circuit UIV ×4, Superradiant Mirror Casing ×4 | Infinity molten 18,432, SpaceTime molten 2,304 | RECIPE_UIV | 4,800 |
| SL-P05 | Bolt Injector Rail Mk I | ASM | Robot_Arm_UIV ×1, Electric_Piston_UIV ×1, frameGt SpaceTime ×1, gearGt SpaceTime ×2 | Lubricant 1,000 | RECIPE_UIV | 300 |
| SL-P06 | Bolt Injector Rail Mk II | ASM | Rail Mk I ×1, Conveyor_Module_UIV ×2, Robot_Arm_UIV ×1 | Lubricant 2,000 | RECIPE_UIV ×4 | 400 |
| SL-P07 | Bolt Injector Rail Mk III | ASM | Rail Mk II ×1, Robot_Arm_UMV ×1, Electric_Piston_UMV ×1, Conveyor_Module_UMV ×1 | SpaceTime molten 576, Lubricant 4,000 | RECIPE_UMV | 600 |
| SL-P08 | Ergosphere Field Coil | ASM | wireGt01 SuperconductorUIV ×64, frameGt TranscendentMetal ×1, plate CosmicNeutronium ×8 | Super Coolant 32,000 | RECIPE_UIV | 1,200 |
| SL-P09 | Beam Focusing Quadrupole | ASM | Ergosphere Field Coil ×2, Sensor_UIV ×1, ParticleBeamGuidancePipeCasing ×2, plate Neutronium ×8 | Super Coolant 16,000 | RECIPE_UIV | 1,200 |
| SL-P10 | Beam Dump | ASM | plateDense Neutronium ×8, ingot CosmicNeutronium ×16, frameGt NaquadahAlloy ×1 | Super Coolant 16,000 | RECIPE_UIV | 600 |
| SL-P11 | Eye Shell Casing ×2 | ASM | plateDense Neutronium ×4, frameGt SpaceTime ×1, plate Neutronium ×4 | SpaceTime molten 576 | RECIPE_UIV | 800 |
| SL-P12 | Harmonic Lining ×2 | ASM | foil Infinity ×8, plate TranscendentMetal ×2 | Infinity molten 288, Super Coolant 1,000 | RECIPE_UIV | 1,200 |
| SL-P13 | Spatial Glass Channel ×4 | ASM | IC2 reinforced glass ×4 (`GTModHandler.getIC2Item("reinforcedGlass", 4)` through `nn`), foil SpaceTime ×8, Emitter_UIV ×1 | SpaceTime molten 144 | RECIPE_UIV | 600 |
| SL-P14 | Ergosphere Extraction Casing ×2 | ASM | plateDense Neutronium ×2, frameGt SpaceTime ×1, Electric_Pump_UIV ×1 | SpaceTime molten 288 | RECIPE_UIV | 800 |
| SL-P15 | Beam Band Casing ×4 | ASM | EOH_Reinforced_Spatial_Casing ×1, Beam Focusing Quadrupole ×2, Emitter_UMV ×2, plateDense SpaceTime ×2 | SpaceTime molten 9,216 | RECIPE_UMV | 1,800 |
| SL-P16 | Jet Crescent Casing ×4 | ASM | EOH_Reinforced_Temporal_Casing ×1, Ergosphere Field Coil ×2, plateDense Infinity ×8, Emitter_UMV ×1 | SpaceTime molten 4,608, RawStarMatter 16,000 | RECIPE_UMV | 2,400 |
| SL-P17 | Galactic Shell Plating ×2 | ASM | plateDense Infinity ×4, frameGt TranscendentMetal ×1, plate CosmicNeutronium ×4 | TranscendentMetal molten 1,152, **HSM 1,000** | RECIPE_UMV | 800 |
| SL-P18 | Bastion Plate ×4 | ASM | plateDense Neutronium ×6, plate TranscendentMetal ×2, frameGt Infinity ×1 | Infinity molten 2,304 | RECIPE_UMV | 1,200 |
| SL-P19 | Casimir Plate ×4 | ASM | plate Neutronium ×8, foil SpaceTime ×16 | SpaceTime molten 576 | RECIPE_UMV | 1,200 |
| SL-P21 | Stabiliser Pylon (item) | AL (research: EOH_Reinforced_Spatial_Casing) | Eye Shell Casing ×32, Harmonic Lining ×16, Field_Generator_UMV ×8, frameGt Neutronium ×16, circuit UMV ×8, plateDense Neutronium ×16, Containment Deck Plating ×16 | SpaceTime molten 147,456, Super Coolant 500,000 | RECIPE_UMV ×4 | 18,000 |
| SL-P22 | Star Chart: Sol | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate Infinity ×4 | SpaceTime molten 144 | RECIPE_UMV | 200 |
| SL-P23 | Star Chart: Sirius A | ASM | Tool_DataOrb ×1, circuit UMV ×1, plateDense SpaceTime ×1 | RSPM 10,000 | RECIPE_UMV | 200 |
| SL-P24 | Star Chart: Arcturus | ASM | Tool_DataOrb ×1, circuit UMV ×1, plate TranscendentMetal ×4 | ESPM 1,000 | RECIPE_UMV | 200 |
| SL-P25 | Star Chart: Sirius B | ASM | Tool_DataOrb ×1, circuit UMV ×1, Galactic Shell Plating ×1 | HSM 1,000 | RECIPE_UMV | 200 |
| SL-P26 | Star Chart: Sirius B, Cooled (interim) | ASM | Star Chart: Sirius B ×1, circuit UMV ×1 | Super Coolant 64,000 | RECIPE_UMV | 200 |

SL-P20 is reserved and unused. Keep the ID gap; do not renumber. Each star chart's fluid is the previous tier's
product, so a chart cannot be crafted before its tier is reachable. SL-P26 registers only when
`Config.interimBlackDwarfAtEyeT4` is true.
`plateDense SpaceTime` and the other dense-plate pairings may not be generated by GT. `req()` names a missing one at
boot (`mGeneratedItems` check). The Foundation unit swaps any that fails to `plate` ×9 in the same recipe and
notes it in the commit.

### 2.3 Kerr recipes: `BTSRecipesKerr.java` (Kerr unit)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-K01 | Kerr Ergosphere Engine (controller, MTE base+14) | AL (research: Black_Hole_Opener) | plateDense SpaceTime ×32, frameGt SpaceTime ×32, Field_Generator_UIV ×16, Emitter_UIV ×16, Sensor_UIV ×16, circuit UIV ×32, Superradiant Mirror Segment ×8, Bolt Injector Rail Mk I ×16, Black_Hole_Opener ×8, screw TranscendentMetal ×64, Ergosphere Field Coil ×16, Containment Deck Plating ×32 | SpaceTime molten 294,912, TranscendentMetal molten 147,456, Super Coolant 1,000,000 | RECIPE_UIV | 18,000 |
| SL-K02 | Kerr Compressor Module (block) | AL (research: Kerr controller) | Electric_Piston_UIV ×32, Field_Generator_UIV ×8, circuit UIV ×16, plateDense Neutronium ×32, Black_Hole_Opener ×4, gearGt SpaceTime ×16 | SpaceTime molten 73,728, Super Coolant 500,000 | RECIPE_UIV | 12,000 |
| SL-K03 | Kerr Extractor Module (block) | AL (research: Kerr controller) | Electric_Pump_UIV ×32, Field_Generator_UIV ×8, circuit UIV ×16, plateDense Neutronium ×32, Beam Focusing Quadrupole ×4, pipeHuge Neutronium ×16 | SpaceTime molten 73,728, Super Coolant 500,000 | RECIPE_UIV | 12,000 |
| SL-K04 | Kerr Density Upgrade (item) | AL (research: Kerr controller) | Bastion Plate ×64, Bastion Plate ×64, plateDense Infinity ×64, Field_Generator_UMV ×32, circuit UMV ×32, Ergosphere Field Coil ×32, Black_Hole_Stabilizer ×1, Bolt Injector Rail Mk III ×16 | RawStarMatter 4,000,000, SpaceTime molten 589,824, ExcitedDTEC 128,000 | RECIPE_UMV ×4 | 36,000 |
| SL-K05 | HSM 1,000 L | `beyondthestars.recipe.kerrdensity` (new, section 3.1) | bolt Neutronium ×64 | RHSP 10,000, SpaceTime molten 288 | RECIPE_UMV | 400 |

The Kerr's other maps are GT's own (`compressorRecipes`, `neutroniumCompressorRecipes`, `extractorRecipes`,
`fluidExtractionRecipes`), so it adds no recipes there.

### 2.4 Eye recipes: `BTSRecipesEye.java` + `eye/EyeRecipeRegistry.java` (Eye unit)

| ID | Output | Map | Item inputs | Fluids | EU | Ticks |
|---|---|---|---|---|---|---|
| SL-E01 | Eye of Harmony (controller, MTE base+15) | AL (research: EOH_Reinforced_Spatial_Casing) | EOH_Reinforced_Spatial_Casing ×64, EOH_Reinforced_Temporal_Casing ×64, SpacetimeCompressionFieldGeneratorTier8 ×1, TimeAccelerationFieldGeneratorTier8 ×1, StabilisationFieldGeneratorTier8 ×1, circuit UMV ×64, Field_Generator_UMV ×32, plateDense SpaceTime ×64, Eye Shell Casing ×64, Harmonic Lining ×64, Stabiliser Pylon ×6, Containment Deck Plating ×64 | SpaceTime molten 1,179,648, ExcitedDTEC 256,000, Infinity molten 589,824 | RECIPE_UMV ×4 | 36,000 |
| SL-E02 | Galactic Upgrade Core | AL (research: Eye controller) | Galactic Shell Plating ×64, Field_Generator_UMV ×32, circuit UMV ×64, Casimir Plate ×32, plateDense SpaceTime ×32, Emitter_UMV ×16 | **HSM 256,000**, SpaceTime molten 147,456, TranscendentMetal molten 147,456 | RECIPE_UMV ×4 | 48,000 |

The Eye's process recipes (SL-E1x) are **not** GT recipe-map recipes; they are section 3.3's registry, shown in NEI
through a display-only map `beyondthestars.recipe.eye` built with `.fake()` like `BTSDisplayRecipes`.

---

## 3. Machines

### 3.1 Kerr Ergosphere Engine (P2): `machines/kerr/MTEKerrEngine.java`, MTE **base+14 (28114)**

`extends MTEExtendedPowerMultiBlockBase<MTEKerrEngine> implements ISurvivalConstructable` (the
`MTEProductionHallBase` style: a `ProcessingLogic` with a parallel supplier, void protection, batch mode, and a
machine-mode switch). Tier: UIV. It replaces the BHC (D-44).

**Pieces** (section 5 has the shapes):

| Piece | Required | Contents |
|---|---|---|
| `kerr_t1` | yes, unless the Density Upgrade is installed | the cage, mirror cavity, 8 injector rails, governor arc with controller, dump arc, beamline |
| `kerr_t2` | yes, when the Density Upgrade is installed (it **replaces** `kerr_t1`) | `kerr_t1` plus the density reinforcement: 2 Bastion hoops (y ±14, r 41), a mid band (r 43), 12 paired struts, the dome-foot hoops, and the beamline extended to x 47 |
| `kerr_compressor` | optional checked piece | south service arc (245°-295°), Background Radiation Absorbent shell, Kerr Compressor Module core block, 6 I/O sockets |
| `kerr_extractor` | optional checked piece | east arc (328°-352°), Ergosphere Extraction shell, Kerr Extractor Module core block, 3 I/O sockets |

Module pieces follow the `MTEReactorCoreMk1.java:2946-3077` pattern: their own `pieceErrors` list, a fault string
when blocks stand, and nothing they report can un-form the core. Their I/O sockets **do** join the standard GT
lists (`mInputBusses`, `mOutputBusses`, `mInputHatches`, `mOutputHatches`), because the Kerr has no other recipe I/O.
They use **mark-and-truncate** on a failed walk (the Spiral pattern at `MTEReactorCoreMk1` ~:3030) so a half-built arc
leaves nothing registered. **Bolt buses are private**: a `KerrHatchElement.BoltBus` enum (the
`ModuleHatchElement` shape), so recipes never eat bolts and the governor never eats recipe inputs (trap 3).

**Upgrade install.** Sneak-right-click the controller holding a Kerr Density Upgrade (server side,
`onRightclick`). That consumes one, sets `densityInstalled = true` and re-checks the structure. It cannot be undone.
`densityInstalled` is written by **one** serializer, called from both `saveNBTData` and `setItemNBT` (a wrench keeps
it), and read in `loadNBTData` (trap 2, the D-23 pattern). The hologram never reads it: `construct(stack,
hintsOnly)` picks T1 or T2 from the trigger stack size (1 = T1, ≥2 = T2), and both modules are always projected
(trap 1).

**The hole** (bh-model.js constants, kept as tuning knobs; marked PROPOSED, Q-8):

- State persisted in NBT: `holeOpen`, `mass` (double, SM) and `setpoint`.
- **Open:** a Black Hole Seed in any bolt bus is consumed, and the hole opens at M = 8 SM. **Close:** a Black Hole
  Collapser. If M falls below `M_FLOOR` = 2 SM, the hole evaporates (closes). There is no explosion in P2.
- **Hawking bleed:** 3.2 / M² SM per tick. Each tick the **governor** fires bolts from the bolt buses to hold M at the
  setpoint. Bolt masses: Neutronium 1/64, Cosmic Neutronium 1/32, Infinity 1/16, Transcendent Metal 1/8, SpaceTime 1/4
  SM. It fires at most `cap` bolts per tick, where the cap is set by the **lowest** injector-rail tier found in
  the walk (Mk I 4, Mk II 8, Mk III 16). Short on bolts or on cap, the hole drifts down.
- **Setpoint** (screwdriver): 2, 4, 8, 16, 32 or 64 SM at T1; T2 adds 128, 256 and 512 ("more stellar mass", D-44).
- **Mass buys parallel, heat buys speed:** parallel = 8 × `getTierExtended(maxInputEu)` × ⌊√M⌋, so 64 SM gives ×8
  over the BHC's base and 512 SM gives ×22. Particle energy E = 3200 / M GeV, and the recipe time is divided by
  min(4, E/50): ×1 at 64 SM, ×4 at ≤12.5 SM. A small hole is fast but burns bolts (2 SM needs 51 Neutronium bolts a
  tick, above even Mk III, so it needs SpaceTime bolts at 3.2 a tick); a big hole is wide and cheap to hold.
- The mirror cavity, spin and beam are **structure only** in P2: no beam output, and no Ergomatter (D-42 moves it to
  T7) (Q-8).

**Machine modes** (`supportsMachineModeSwitch`, screwdriver; the cycle skips unavailable modes):

| Mode | Needs | Maps | Notes |
|---|---|---|---|
| COMPRESS | compressor piece | `compressorRecipes` always; `neutroniumCompressorRecipes` only while the hole is open | BHC parity. With the hole closed it runs at the base parallel 8 × tier, like the BHC's plain-compressor mode. |
| EXTRACT | extractor piece | `extractorRecipes`, `fluidExtractionRecipes` | the "extraction" half (Q-9) |
| DENSIFY | compressor piece **and** `kerr_t2` **and** M ≥ 128 SM | `beyondthestars.recipe.kerrdensity` (`RecipeMapBuilder.of(...).maxIO(2,1,2,1).minInputs(1,1)`) | HSM (SL-K05). "Density gates the hyperdense stuff" (D-44). |

With neither module built: no modes, the machine idles, and the panel says why. **Power:** standard energy or
exotic (laser) hatches on the 2 governor-arc sockets, at least 1. Recipe EU/t comes from the map; the hole draws no
EU. **Tooltip and panel:** mass, setpoint, bleed, bolts per tick against cap, parallel, speed, mode, modules found,
the density state, and each module's fault string.

### 3.2 Kerr T2 (density): same controller

Kerr T2 is not a new machine. Installing the Density Upgrade switches the required shape from `kerr_t1` to
`kerr_t2`, raises the setpoint range to 512 SM and unlocks DENSIFY. If the T2 reinforcement is missing after the
upgrade is installed, the machine does **not** form, and the panel prints the `kerr_t2` structure error. The upgrade
is a commitment to the bigger build, which is the "required" reading of D-44.

### 3.3 Eye of Harmony T1-T4 (P3): `machines/eye/MTEEyeOfHarmonyBTS.java`, MTE **base+15 (28115)**

One controller (D-34). Display name "Eye of Harmony", with the tooltip line "Beyond the Stars: replaces TecTech's".
Grounded on six pylons over the oval plinth, so its alignment limits allow horizontal facings only. Its parent is
`MTEExtendedPowerMultiBlockBase`, but processing is overridden like TecTech's EOH, because inputs and outputs are
`long`-sized:

- **Pieces:** `eye_core` (T1: core band, two rails, clamps, six pylons, plinth; **required**), `eye_beam` (T2 beam
  band + clamps; optional), `eye_jet` (T3 jet crescent + clamps; optional, and **independent of** `eye_beam`, so T2
  stays optional per D-42), `eye_arms` (T4: two spiral-arm blades; optional).
- **Tier unlock:** T1 = core. T2 recipes need `eye_beam`. T3 recipes need `eye_jet`. T4 recipes need `eye_jet` +
  `eye_arms` + an installed **Galactic Upgrade Core** (sneak-right-click to install; it is consumed; same persistence
  rules as the Kerr upgrade). There are **no** cores for T2/T3: the band is the cost (Q-11). The hologram uses the
  stack size (1..4) and projects the bands cumulatively.
- **Sockets:** 5 gas sockets on the plinth (any `MTEHatchInput`, ME stocking included) drained every 20 ticks into
  `long` buffers for H and He. Stellar-fluid sockets: 6 on the beam band and 5 on the jet crescent, drained into
  buffers for RSPM, ESPM, HSM and (interim recipe only) WDM. Outputs are **ME only** (`MTEHatchOutputBusME`, `MTEHatchOutputME`): 2 sockets on the
  plinth, at least one of each. 1 maintenance socket. All sockets are private `EyeHatchElement`s. Buffers are
  persisted as longs, capped at 2⁶² each, and printed on the panel.
- **Target:** the controller slot (`mInventory[1]`) holds a star chart or a `gtneioreplugin` dimension block; the
  recipe is looked up by that item (TecTech's `recipeLookUp` shape). The target is not consumed.
- **Power:** **wireless EU** from the owner's network (mine.md Q2: plain wireless until T7). The whole run's EU is
  debited at start (`addEUToGlobalEnergyMap(uuid, -cost)`); if the debit fails, the run doesn't start. There is no EU
  return (Q-4). No energy hatches.
- **Parallel:** 2^(installedTier − recipeTier), so a T4 Eye runs Sol ×8, Sirius A ×4, Arcturus ×2 and Sirius B ×1.
  Inputs, outputs and EU scale with it.
- **Success:** stars always succeed. Planets keep TecTech's base 1 − 0.05R. A failed run consumes its inputs and EU
  and outputs nothing. There is no SpaceTime penalty and no stabilisation (D-42: stabilisation starts at T7).
- **Centre render:** client-side render of the band's object (D-34), toggled by a client config
  `eyeRenderCentre` (default true). This is a stretch goal for P3: the structure ships first.

**Process recipes. Every amount is PROPOSED (D-31 is open, Q-6).** Back-solve: each star recipe consumes the
previous star's product **1 : 1 per run**, so one run of each tier chains into the next. WDM per run equals
TecTech's best WDM recipe (R2, 18,432 L). Planet times follow bh-model's T1 band, `t1Minutes(R) = 20 × 1.25^R`,
continued past R3.

| Tier | Target | Inputs per run | Outputs per run | Base time | EU (wireless, per run) |
|---|---|---|---|---|---|
| T1 | Star Chart: Sol | H 1,000,000,000 L + He 1,000,000,000 L | RSPM (`RawStarMatter`) 1,000,000 L | 20 min | RECIPE_UMV × ticks |
| T1 | planets R0-R3 (TecTech dims) | TecTech's H/He for that dim | TecTech's ores and plasmas, **minus** RawStarMatter and the WDM/BDM/Universium special fluid | 20 / 25 / 31 / 39 min | RECIPE_UMV × ticks |
| T2 | Star Chart: Sirius A | RSPM 1,000,000 L + He 1,000,000,000 L | ESPM 100,000 L (10:1, against the Homogeny's 100:1) | 40 min | RECIPE_UMV ×4 × ticks |
| T2 | planets R4-R6 | as above | as above | 49 / 61 / 76 min | RECIPE_UMV ×4 × ticks |
| T3 | Star Chart: Arcturus | ESPM 100,000 L + H 2,000,000,000 L | RHSP 50,000 L | 80 min | RECIPE_UMV ×16 × ticks |
| T3 | planets R7-R8 | as above | as above | 95 / 119 min | RECIPE_UMV ×16 × ticks |
| T4 | Star Chart: Sirius B | HSM 5,000 L + He 4,000,000,000 L | molten WDM 18,432 L | 120 min | RECIPE_UMV ×64 × ticks |
| T4 | Deep Dark (TecTech `DD`) | H/He 10,000,000,000 L each | TecTech's DD ores and every plasma, **no Universium** | 240 min | RECIPE_UMV ×64 × ticks |
| T4 interim | Star Chart: Sirius B, Cooled | WDM 18,432 L + Super Coolant 1,000,000 L | molten BDM 4,608 L | 120 min | RECIPE_UMV ×64 × ticks |

The chain balances: one Sol run (1M RSPM) feeds one Sirius A run (100k ESPM), which feeds one Arcturus run (50k
RHSP), which is 5 HSM batches (5,000 L), which feeds one Sirius B run. **Conversion:** at `FMLLoadCompleteEvent`,
`EyeRecipeRegistry` reads every `EyeOfHarmonyRecipe` from `TecTech.eyeOfHarmonyRecipeStorage`'s private
`recipeHashMap` by reflection, **before** removal, and builds ours: same trigger item, `getOutputItems()`, plasma
fluids, and H/He from `getHydrogenRequirement/getHeliumRequirement`. Rocket tier → Eye tier as R0-3 → T1, R4-6 → T2,
R7-8 → T3, DD → T4. The `RawStarMatter`, WDM, BDM and Universium fluids are dropped (D-42 "without the matters",
Q-12). The interim BDM recipe registers only if `Config.interimBlackDwarfAtEyeT4` (Q-3).

### 3.4 MTE id plan (block base+14 .. base+39 = 28114-28139)

| Id | Offset | Allocation |
|---|---|---|
| 28114 | base+14 | **Kerr Ergosphere Engine**: `multimachine.beyondthestars.kerrengine` (P2) |
| 28115 | base+15 | **Eye of Harmony**: `multimachine.beyondthestars.eyeofharmony` (P3) |
| 28116-28119 | base+16..19 | reserved for P2/P3 spares (none planned; module sockets use GT hatches) |
| 28120-28129 | base+20..29 | P4: Quasar Engine 28120, Wormhole Mouth 28121, Conduit Mirror 28122, Conduit Junction 28123, Burst Thermaliser Crown Mk I/II/III 28124-28126, spare 28127-28129 |
| 28130-28134 | base+30..34 | ON HOLD: Homogeny 28130, Reality Diffraction 28131, Reality Refab 28132, spare 28133-28134 |
| 28135-28139 | base+35..39 | spare |

The Foundation unit adds this table as a comment to the ledger in `BTSMachines.register()`. The Kerr and Eye units
each append their own single registration line. Ids are frozen once shipped.

---

## 4. Recipe removals: `BTSRecipeRemovals.java` (Foundation) + `eye/EyeRecipeRegistry.java` (Eye)

**Load phase.** Removals run in a new `FMLLoadCompleteEvent` handler:
`BeyondTheStars.loadComplete` → `CommonProxy.loadComplete` → `BTSRecipeRemovals.run()`. Add
`after:dreamcraft;after:tectech` to the `@Mod` `dependencies` string. FML dispatches load-complete in dependency
order, so ours runs after TecTech's `onLoadCompleted`, which is where the EOH storage is built. The dreamcraft
assembly-line recipes are long registered by then. Every removal is wrapped in `BTSRecipeGuard.recipe(...)`-style
containment and logs per-collection counts: `[BTS] removed BHC controller: AL-visual 1, AL-data 1, research 1`. It
warns if a count is 0 (the pack changed).

**Shared helper:** `removeAssemblyLineByOutput(ItemStack out)` removes every recipe whose output `isItemEqual(out)`
from:

1. `RecipeMaps.assemblylineVisualRecipes.getBackend()` (via `removeRecipes`)
2. `GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes` (what data sticks resolve against)
3. `TecTechRecipeMaps.researchableALRecipeList` and the matching `researchStationFakeRecipes` entries
4. `RecipeMaps.scannerFakeRecipes` entries whose data-stick output (`AssemblyLineUtils.getDataStickOutput`) matches

If `AssemblyLineUtils.loadedALRecipes` is already true, it clears `AL_RECIPE_LOOKUP` and resets the flag by
reflection, so the lookup rebuilds without the removed recipe.

| Removal | Config switch (`Config.java`, general) | Default | What it does |
|---|---|---|---|
| BHC controller | `removeBlackHoleCompressorRecipe` | true | `removeAssemblyLineByOutput(ItemList.Machine_Multi_BlackHoleCompressor.get(1))` |
| TecTech EOH | `replaceTecTechEyeOfHarmony` | true | (a) the Eye registry's conversion snapshot runs first, always; (b) `recipeHashMap.clear()` on `TecTech.eyeOfHarmonyRecipeStorage`, so TecTech's `recipeLookUp` returns null and placed EOHs idle; (c) remove every recipe from `TecTechRecipeMaps.eyeOfHarmonyRecipes` (NEI); (d) `removeAssemblyLineByOutput(CustomItemList.Machine_Multi_EyeOfHarmony.get(1))` |
| Interim BDM | `interimBlackDwarfAtEyeT4` | true | registers the T4 interim cooling recipe (Q-3); remove it when P4 ships Eye T5 |

**Old-save note** (Eye unit appends to `docs/migration.md`):

- A placed Black Hole Compressor keeps working. Only its crafting recipe goes, and data sticks holding it stop
  resolving.
- A placed TecTech Eye of Harmony becomes a museum piece: it forms but finds no recipe. **Finish or cancel running
  EOH recipes before updating.** A run in flight when the recipes are cleared re-resolves to nothing and its outputs
  are lost.
- With `replaceTecTechEyeOfHarmony=false`, TecTech's EOH runs as stock, our Eye coexists, and the pack's
  WDM/BDM/Universium sources are untouched.

---

## 5. Structure plan

### 5.1 Size sanity (measured: headless run of `designs/voxel.js`, render-only cells excluded)

| Structure | Bounding box (W × H × D) | Placed blocks | Cells in box | Chunks spanned |
|---|---|---|---|---|
| Kerr T1 | 76 × 65 × 79 | 15,355 | 390,260 | 5-6 × 5-6 |
| Kerr T2 | 92 × 65 × 89 | 29,831 | 532,220 | 6-7 × 6-7 |
| Eye T1 | 102 × 52 × 80 | 19,616 | 424,320 | 7-8 × 5-6 |
| Eye T2 | 102 × 57 × 80 | 23,959 | 465,120 | |
| Eye T3 | 102 × 57 × 80 | 27,943 | 465,120 | |
| Eye T4 | 102 × 57 × 137 | 34,509 | 796,518 | 7-8 × 9-10 |

Block counts per kind:

- **Kerr T1:** mirrorback 3,551, frame 3,186, mirror 2,852, casing 1,920, coil 842, eyeshell 682, bhc 633, lining 521,
  ring3 315, injector 232, quad 208, sglass 199, dump 158, pipe 31, sockets 25.
- **Kerr T2 adds:** bastion 8,316, lining 4,031, sglass 1,442, casimir 677.
- **Eye T1:** eyeshell 9,732, deck 4,752, lining 3,582, sglass 1,541.
- **Eye T2 adds:** ring2 2,971.
- **Eye T3 adds:** ring3 2,850.
- **Eye T4 adds:** galactic 5,374.

**Verdict: build full size; no scaled build is needed.** The structures are sparse shells (3-6 % fill) and sit in
the same block-count class as GTNH's Forge of the Gods. Conditions:

- **(a) Chunk loading:** the check fails across unloaded chunks, so the tooltip says "chunk-load the full
  footprint".
- **(b) Check cost:** each piece is a trimmed bounding box, and spaces are skipped. Each unit times one
  `checkMachine` with a debug timer during smoke test. If one takes more than 20 ms, re-check only on structure
  updates (GT's default), never per tick.
- **(c) Holograms:** 15-35k hint particles is heavy but works. Survival auto-place is budgeted by StructureLib per
  use. Creative testing uses the projector's creative build.

### 5.2 Exporter: `addon/tools/export-structures.js` (committed; Foundation)

- Usage: `node tools/export-structures.js ../designs/voxel.js`, with `--check` to regenerate into a temp dir and diff
  against the committed files (non-zero exit on drift). No npm dependencies.
- **Headless load:** read voxel.js and cut it at the `// ---- GL` banner. Append, inside the IIFE, an export hook
  `globalThis.__vox = { SCENES, BLOCKS, B, RENDER_ONLY, enableTags(){ tagArr = new Uint16Array(W*H*D); }, ... }`.
  Run it in `vm` with stubs: `document.createElement('canvas')` returns a **software canvas** whose 2D context
  implements `fillRect` (1×1 cells), `fillStyle` (an `rgb()` parser), `getImageData` and `drawImage` no-ops; plus
  `$ = () => null` and `window = {}`. A prototype of this load is proven: it produced the numbers in 5.1.
- **Kerr piece tags:** buildKerr has no tags, so the exporter inserts no-op `tag("...")` calls into the **source
  string** at four asserted anchors:
  - `arc(36, 245, 295` → `kerr_compressor`
  - `arc(35, 70, 110` → `kerr_core`
  - `arc(36, 328, 352` → `kerr_extractor`
  - `if (tier>=2){` → `kerr_density`
  
  It throws if any anchor is missing. Cells take the tag of their **last** writer, so pieces are disjoint by
  construction. `kerr_t1` = core cells of the tier-1 build. `kerr_t2` = core + density cells of the tier-2 build. The
  module pieces must be identical in both builds (asserted).
- **Eye pieces by tier diff:** build tiers 1-4. `eye_core` = T1. `eye_beam` = T2 − T1. `eye_jet` = T3 − T2.
  `eye_arms` = T4 − T3. The exporter **asserts** that no non-render cell changes between tiers, only gets added (the
  D-34 overlap audit, re-proved at export).
- Render-only kinds (`/render|not built/`) become `' '` (skip): the centre stays free for the render.
- **Output:** one file per piece, `src/main/resources/assets/beyondthestars/structures/<piece>.txt`. Format:
  - a header `# piece=<name> w=<a> h=<b> d=<c> off=<h>,<v>,<d> sha=<sha1 of voxel.js>`;
  - then `d` layers separated by `---`, each layer `h` rows of `w` chars, top row first, in StructureLib's
    **native** `String[depth][height]` layout (char index = horizontal);
  - `off` = the controller's (horizontal, vertical, depth) offset into this piece, passed straight to
    `checkPiece` / `buildPiece` / `survivalBuildPiece`.
  
  Pieces are trimmed to their own bounding box.
- Also `structures/manifest.json`: per piece, its char → kind → count bill of materials, socket counts and offsets.
  The kit command (`CommandBTSKit`) and the tooltips read their counts from it, never hand-typed (D-22).
- **Orientation:** before the first full export, the Foundation unit exports a 3×3×3 asymmetric test piece and
  confirms in-world with a hologram that controller-left, up and back map the way the loader assumes. Flags
  `--flipH/--flipD` exist for the fix. Controller front: Kerr = outward on the governor arc (+z in voxel space). Eye =
  outward on the plinth at 90°.
- **Loader:** `machines/BTSShapes.java` (Foundation): `static String[][] load(String piece)` reads the resource once
  into a cache. `static int[] offset(String piece)` returns `off`. Shapes are never Java literals (constant-pool
  limits).

### 5.3 Block-kind → shape char → real block

| Char | Voxel kind | Element |
|---|---|---|
| `' '` | empty and every render-only kind | skip |
| `~` | `control` (Kerr governor arc / Eye plinth) | controller |
| `A` | `casing` | `ofBlock(Casings.ExtremeDensitySpaceBendingCasing)`, `noOverwrite` |
| `B` | `frame` | `ofFrame(Materials.NaquadahAlloy)` |
| `C` | `mirror` | slCasing:1 |
| `D` | `mirrorback` | slCasing:2 |
| `E` | `injector` | slCasing:3/4/5, **tiered** (`ofBlocksTiered`; the minimum tier sets the bolt cap) |
| `F` | `quad` | slCasing:7 |
| `G` | `pipe`; and `beamhatch` at Kerr x=17 / x=47 | `Casings.ParticleBeamGuidancePipeCasing` |
| `H` | `coil` | slCasing:6 |
| `I` | `bhc` | `Casings.BackgroundRadiationAbsorbentCasing` |
| `J` | `lining` | slCasing2:2 |
| `K` | `sglass` | slGlass:0 |
| `L` | `eyeshell` | slCasing:9 |
| `M` | `dump`; and the dump arc's `ergohatch`/`output` cells | slCasing:8 |
| `N` | `ring3` inside `buildKerr` | slCasing:15 |
| `O` | `bastion` | slCasing:13 |
| `P` | `casimir` | slCasing:14 |
| `Q` | `deck` | slCasing:0 |
| `R` | `ring2` | slCasing:10 |
| `S` | `ring3` in the Eye | slCasing:11 |
| `T` | `galactic` | slCasing:12 |
| `A` | the 8 `maint` cells beside the bolt buses | casing (not sockets: 8 maintenance hatches would be noise) |

Every plain element is wrapped in `BTSStructures.noOverwrite(...)`.

**Sockets.** Each is `ofChain(hatchAdder, <its arc's casing>)`, so an unused socket may hold casing. The hatch adder
comes first, so the hologram hints the hatch (trap 4). Counts are checked after the walk. The exporter pins sockets
by cell (a table of voxel coordinates), not by guessing.

| Char | Machine | Cells (voxel) | Accepts | Min |
|---|---|---|---|---|
| `e` | Kerr | governor arc (2,0,37.6), (4,0,37.6) | Energy / ExoticEnergy (laser) | 1 |
| `m` | Kerr | governor arc (−2,0,37.6) | Maintenance | 1 |
| `b` | Kerr | 8 `boltbus` at r 34, y ±5 | `KerrHatchElement.BoltBus` (InputBus, private) | 1 |
| `c` | Kerr compressor | 4 `beamhatch` at r 32.6 (255-285°) + the arc's outer `energy`/`maint` cells (0,0/2,−39.6) | InputBus / InputHatch / OutputBus / OutputHatch (standard lists, mark-and-truncate) | 1 in + 1 out |
| `q` | Kerr compressor | the arc's `control` cell (0,−2,−39.6) | slCasing2:0, the module core (exactly 1) | 1 |
| `x` | Kerr extractor | `output` at 334° and 346° | as `c` | 1 in + 1 out |
| `k` | Kerr extractor | `output` at 340° | slCasing2:1, the module core | 1 |
| `h` | Eye | 5 plinth `gas` | `EyeHatchElement.Gas` (MTEHatchInput, private → buffer) | 1 |
| `o` | Eye | 2 plinth `energy` | `EyeHatchElement.Output` (MTEHatchOutputBusME, MTEHatchOutputME) | 1 bus + 1 hatch |
| `m` | Eye | plinth `maint` | Maintenance | 1 |
| `s` | Eye | 6 beam-band + 5 jet-crescent `beamhatch` | `EyeHatchElement.Stellar` (MTEHatchInput, private → buffer) | 0 (T2+ only use them) |

Hint numbers: Kerr: energy 1, maintenance 2, bolt bus 3, compressor I/O 4, extractor I/O 5. Eye: gas 1, ME output 2,
maintenance 3, stellar 4.

### 5.4 Kerr T1 vs T2; Eye core and bands

- **Kerr:** two complete core shapes (`kerr_t1`, `kerr_t2`) plus two module pieces shared by both. T2 is T1 with the
  reinforcement added: the exporter asserts T2 only adds cells to T1's non-render cells. The larger horizon is
  render-only.
- **Eye:** one required core piece plus three additive band pieces (`eye_beam`, `eye_jet`, `eye_arms`). Only clamps
  join bands, and each clamp belongs to the band built after it. T5-T10 are new pieces in P4/P5. Because every band
  is additive, the core never re-exports.

### 5.5 Textures

The exporter also writes textures using the same software canvas:

- 16×16 PNGs for every new block from the voxel atlas painters (`def(...)` painters → `textures/blocks/sl_*.png`),
  with a ~40-line zlib PNG encoder (Node built-in `zlib`).
- Module cores and injector Mk II/III get a ring recolour.
- Items get a small port of recipes.js's `cond`/`plate` icon painters.

So the in-game look equals the approved mockup tiles.

---

## 6. Build order and file ownership

**Rules.**

- One builder at a time; the orchestrator sequences Foundation → Kerr → Eye.
- Every unit commits granular local `wip:` commits staging **only** its own files. Never `git add -A`; never push,
  tag, reset, stash, checkout or rebase.
- Shared files take **append-only** edits: add lines at the end of the relevant block, never reorder or reformat
  others' lines. Shared files: `BTSMachines.java`, `CommonProxy.java`, `BTSRecipes.java`, `en_US.lang`,
  `CommandBTSKit.java`, `BTSBlocks.java`, `BTSFluids.java`, `Config.java`, `docs/migration.md`.
- Every change is checked against the four regression traps.

### 6.1 Foundation (P1)

**Owns (new files):**

- `tools/export-structures.js`
- `src/main/resources/assets/beyondthestars/structures/*` (generated)
- `textures/blocks/sl_*.png` and `textures/items/` for every §1.3 item
- `items/SLItems.java`
- `fluids/SLFluids.java`
- `blocks/SLBlocks.java`, `blocks/BlockSLCasing.java`, `blocks/BlockSLCasing2.java`, `blocks/BlockSLGlass.java`,
  `blocks/ItemBlockSL.java`
- `machines/BTSShapes.java`
- `BTSRecipesSingularity.java`
- `BTSRecipeRemovals.java` (the helper + the BHC removal + the config switches)

**Shared edits:**

- `BeyondTheStars.java`: the `loadComplete` handler and `after:dreamcraft;after:tectech`.
- `CommonProxy.java`: `loadComplete()`; `SLItems.register()` after `BTSItems.register()`.
- `BTSBlocks.java`: one line calling `SLBlocks.register()`.
- `BTSFluids.java`: one line calling `SLFluids.register()`.
- `Config.java`: the three removal flags + `eyeRenderCentre`.
- `BTSRecipes.java`: `line("singularity", ...)`.
- `BTSMachines.java`: the §3.4 ledger comment only.
- `en_US.lang`: §1 names.

**Checklist:**

1. Run the exporter and verify the orientation test piece in-world.
2. Commit the exporter and the structures.
3. Register blocks, items and fluids, and generate textures and lang. Compile.
4. Add the parts recipes (SL-P01..P26); the boot log must read `0 skipped`.
5. Add the removal framework + BHC removal + flags; the boot log must show the counts.
6. Smoke test: the parts appear in NEI and the BHC AL recipe is gone.

### 6.2 Kerr unit (P2)

**Owns:**

- `machines/kerr/MTEKerrEngine.java`
- `machines/kerr/KerrHatchElement.java`
- `machines/kerr/KerrRecipeMaps.java` (the `kerrdensity` map)
- `BTSRecipesKerr.java`
- controller overlay textures if any

**Shared edits (append-only):**

- `BTSMachines.java`: `kerrEngine` field + the base+14 line.
- `BTSRecipes.java`: `line("kerr", ...)`.
- `CommandBTSKit.java`: controller, module cores, rails, bolts, seed, density upgrade; counts from
  `manifest.json`.
- `en_US.lang`: the Kerr section.

**Checklist:**

1. Build the core shape, controller and hologram for T1 only.
2. Add the module pieces with mark-and-truncate and the private bolt buses.
3. Add the hole state, governor, NBT and the item-NBT path.
4. Add the modes and parallel.
5. Add the density upgrade install and the T2 shape.
6. Add recipes SL-K01..K05.
7. Smoke test: form T1 by projection, run a compressor recipe and a neutronium recipe with the hole open, wrench and
   replace (state kept), install the density upgrade, form T2, run HSM.

### 6.3 Eye unit (P3)

**Owns:**

- `machines/eye/MTEEyeOfHarmonyBTS.java`
- `machines/eye/EyeHatchElement.java`
- `machines/eye/EyeRecipe.java`
- `machines/eye/EyeRecipeRegistry.java` (TecTech conversion, star recipes, the TecTech EOH removal, the NEI fake
  map)
- `BTSRecipesEye.java`
- the centre render (stretch goal)

**Shared edits (append-only):**

- `BTSMachines.java`: `eyeOfHarmony` field + the base+15 line.
- `BTSRecipes.java`: `line("eye", ...)`.
- `CommonProxy.loadComplete()`: one line `EyeRecipeRegistry.onLoadComplete()`, placed **before** the Foundation's
  `BTSRecipeRemovals.run()` line, or have the registry call the removal itself (coordinate through the orchestrator).
- `CommandBTSKit.java`
- `en_US.lang`
- `docs/migration.md` (the old-save note)

**Checklist:**

1. Build the core shape, controller and hologram for T1.
2. Add the long buffers + sockets + the ME output path.
3. Add the wireless debit + the Sol recipe.
4. Add the TecTech conversion (planets/DD) and the NEI fake map.
5. Add the bands T2/T3 + star recipes.
6. Add the Galactic core install + `eye_arms` + T4 recipes + the interim BDM.
7. Add the TecTech EOH removal under its flag.
8. Smoke test: form T1-T4 by projection, run Sol, run a planet (ores to ME), check the TecTech EOH shows no recipes,
   and check that with the flag off both coexist.

### 6.4 Not in P1-P3

Homogeny, Reality machines, Quasar, Conduit, Eye T5+, questbook changes (P6), and the D-45/D-48 pack-recipe swaps
(UXV parts dropping Universium, the Eternity QFT loop on Ergomatter, Magnetic Monopole Matter dropping its MHDCSM
prerequisite). Those swaps matter for Q-3 and are listed there.

---

## Contract additions (foundation-code)

Written by the Foundation code unit on 2026-10-12. These pin names the spec left open and apply the mid-run user
ruling ("use the eye of harmony blocks that currently exist"; prefer the Black Hole Compressor's blocks for the
Kerr). Where this section and §1.2 disagree, this section wins.

### FC-1 Which §1.2 blocks are registered

Four §1.2 roles are played by **existing** blocks, so they are **not registered** (no block, no recipe, no lang):

| Spec block | Voxel kind | Eye uses (TecTech EOH family) | Kerr uses (BHC family) |
|---|---|---|---|
| slCasing:0 Containment Deck Plating | `deck` | Reinforced Temporal Structure Casing (`Casings.ReinforcedTemporalStructureCasing` = `TTCasingsContainer.sBlockCasingsBA0`:10) | (Kerr has no `deck` cells) |
| slCasing:9 Eye Shell Casing | `eyeshell` | Reinforced Spatial Structure Casing (`Casings.ReinforcedSpatialStructureCasing` = BA0:11) | Extreme Density Space-Bending Casing (`Casings.ExtremeDensitySpaceBendingCasing`) |
| slCasing2:2 Harmonic Lining | `lining` | Infinite Spacetime Energy Boundary Casing (`Casings.InfiniteSpacetimeEnergyBoundaryCasing` = BA0:12) | Background Radiation Absorbent Casing (`Casings.BackgroundRadiationAbsorbentCasing`) |
| slGlass:0 Spatial Glass Channel (`BlockSLGlass` is not created) | `sglass` | Quantum Glass (`Casings.QuantumGlass`, TecTech) | Hawking Radiation Realignment Focus (`Casings.HawkingRadiationRealignmentFocus`, the BHC's glass) |

These are recommendations to the Kerr and Eye units, who own the char-to-element mapping; any existing block is
fine as long as no new block is invented for these roles. The TecTech field generators
(`TTCasingsContainer.SpacetimeCompressionFieldGenerators` / `TimeAccelerationFieldGenerator` /
`StabilisationFieldGenerators`, tiered metas 0-8, `ofBlocksTiered` as TecTech's EOH uses them) are available to the
Eye unit; no voxel kind maps to them yet (see the questions file).

All other metas keep their spec numbers. The unregistered metas stay as **holes** (never reused), so the §5.3 table
and every unit's references stay valid.

**Recipe fallout.** SL-P01, SL-P11, SL-P12 and SL-P13 are retired (the IDs stay reserved, like SL-P20).
SL-P21 (Stabiliser Pylon) takes `EOH_Reinforced_Spatial_Casing` ×32, `EOH_Infinite_Energy_Casing` ×16 and
`EOH_Reinforced_Temporal_Casing` ×16 in place of Eye Shell ×32, Harmonic Lining ×16 and Deck ×16. Suggested for the
other units: SL-K01 takes `ExtremeDensitySpaceBendingCasing` ×32 for Deck ×32; SL-E01 takes
`EOH_Infinite_Energy_Casing` ×64 for Harmonic Lining ×64 and drops the Eye Shell ×64 / Deck ×64 lines (the card
already has Spatial ×64 and Temporal ×64).

### FC-2 Registry, lang and texture names

Blocks (`blocks/SLBlocks.java`, fields `SLBlocks.slCasing`, `SLBlocks.slCasing2`; meta constants on
`BlockSLCasing` / `BlockSLCasing2`): registry names `slCasing`, `slCasing2`, item block `ItemBlockSL`. Lang key
`tile.beyondthestars.<registry>.<meta>.name`. One icon per meta on all six faces, 16×16 PNG at
`src/main/resources/assets/beyondthestars/textures/blocks/<texture>.png`:

| Block:meta | Name | Texture |
|---|---|---|
| slCasing:1 | Superradiant Mirror Casing | `sl_mirror` |
| slCasing:2 | Mirror Backing Casing | `sl_mirrorback` |
| slCasing:3 | Bolt Injector Rail (Mk I) | `sl_injector_mk1` |
| slCasing:4 | Bolt Injector Rail (Mk II) | `sl_injector_mk2` |
| slCasing:5 | Bolt Injector Rail (Mk III) | `sl_injector_mk3` |
| slCasing:6 | Ergosphere Field Coil | `sl_coil` |
| slCasing:7 | Beam Focusing Quadrupole | `sl_quad` |
| slCasing:8 | Beam Dump | `sl_dump` |
| slCasing:10 | Beam Band Casing | `sl_ring2` |
| slCasing:11 | Jet Crescent Casing | `sl_ring3` |
| slCasing:12 | Galactic Shell Plating | `sl_galactic` |
| slCasing:13 | Bastion Plate | `sl_bastion` |
| slCasing:14 | Casimir Plate | `sl_casimir` |
| slCasing:15 | Ergosphere Extraction Casing | `sl_extraction` |
| slCasing2:0 | Kerr Compressor Module | `sl_module_compressor` |
| slCasing2:1 | Kerr Extractor Module | `sl_module_extractor` |

`sl_deck`, `sl_eyeshell`, `sl_lining` and `sl_sglass` are not referenced by code (harmless if present).

Items (`items/SLItems.java`, fields named as the §1.3 internal names): texture
`textures/items/<internalName>.png`, e.g. `superradiantMirrorSegment.png`, `starChartSiriusBCooled.png`; lang
`item.beyondthestars.<internalName>.name`.

Fluids (`fluids/SLFluids.java`, fields `excitedStellarPlasma`, `rawHyperdensePlasma`, `hyperdenseStellarMixture`):
GT's tinted `molten.autogenerated` texture, like every existing BTS fluid, so they need no PNG.

### FC-3 Shape loader API (`machines/BTSShapes.java`)

- Resource `/assets/beyondthestars/structures/<piece>.txt`, UTF-8. `\r` is stripped. The first non-blank line must
  start with `# piece=`; other `#` lines are ignored. Rows shorter than `w` are right-padded with spaces (so stripped
  trailing whitespace is harmless); longer rows, or a layer/row count that disagrees with the header, throw.
- `String[][] load(String piece)` → `[d][h]`, each string `w` chars (StructureLib native), cached; callers must not
  mutate it. `int[] offset(String piece)` → `{h, v, d}`. `int[] size(String piece)` → `{w, h, d}`.
  `String sha(String piece)`. `int count(String piece, char c)` counts a char straight from the shape.
- `JsonObject manifest()` parses `structures/manifest.json` once (Gson). `JsonObject manifestPiece(String piece)`
  returns `manifest().pieces.<piece>` if the root has a `pieces` object, else `manifest().<piece>`; null if absent.
- A missing or malformed file throws `IllegalStateException` naming the resource, so a broken shape fails the
  controller's class-init loudly instead of forming nothing.

### FC-4 Load-complete hook and removals (`BTSRecipeRemovals.java`)

- `@Mod.EventHandler loadComplete(FMLLoadCompleteEvent)` → `proxy.loadComplete(event)` →
  `CommonProxy.loadComplete()`, whose body is a list of lines. The Eye unit inserts
  `EyeRecipeRegistry.onLoadComplete();` **above** the `BTSRecipeRemovals.run();` line.
- `public static int removeAssemblyLineByOutput(ItemStack out, String label)` removes from the four collections of
  §4, resets the AL lookup, logs `[BTS] removed <label>: AL-visual a, AL-data b, research c, scanner d` (WARN when
  everything is 0) and returns the total. Safe to call from the Eye registry for the TecTech EOH controller.
- `public static void guard(String label, Runnable body)` contains any throw to that one removal and logs it.
- Config (`Config.java`, category `general`): `removeBlackHoleCompressorRecipe`, `replaceTecTechEyeOfHarmony`,
  `interimBlackDwarfAtEyeT4` (all default true), `eyeRenderCentre` (default true, client cosmetic).

---

## Contract additions (textures)

Defined by the "textures" unit. **Block, item and fluid names follow FC-2 above exactly** (FC-2 was committed first;
an earlier draft of this section used other block names and is superseded). Every PNG is generated by
`tools/gen-sl-textures.js` (Node, no dependencies): `node tools/gen-sl-textures.js` rewrites them all,
`--check` regenerates in memory and exits non-zero if a committed PNG's pixels differ. The file list is the manifest
at the top of the script. Paths are under `src/main/resources/assets/beyondthestars/textures/`.

- **Blocks:** `blocks/<texture>.png` for the 16 FC-2 textures (`sl_mirror` … `sl_module_extractor`). The four
  retired roles (`sl_deck`, `sl_eyeshell`, `sl_lining`, `sl_sglass`) are not generated. Family: Kerr blocks
  (`sl_mirror`, `sl_mirrorback`, `sl_injector_mk1..3`, `sl_coil`, `sl_quad`, `sl_dump`, `sl_bastion`, `sl_casimir`,
  `sl_extraction`, `sl_module_*`) are drawn on the Black Hole Compressor's Extreme Density palette with its purple
  accents; Eye blocks (`sl_ring2`, `sl_ring3`, `sl_galactic`) carry TecTech's EOH casing bevel and blue palette.
- **Items:** `items/<internalName>.png` for the nine §1.3 items.
- **Fluids:** none. FC-2 keeps GT's tinted `molten.autogenerated` (the registered RGBA gives each fluid its colour), so
  no fluid PNG is needed and none is shipped.
- **Controller front overlays:** `blocks/iconsets/<NAME>.png`, loaded with
  `Textures.BlockIcons.custom("beyondthestars:iconsets/<NAME>")` (GT keeps a name containing `:` as-is; the
  `iconsets/` sub-path matches GT's own layout) and layered with `MTEProductionHallBase.overlayPair(icon, glow)`-style
  `TextureFactory` builders (the glow layer gets `.glow()`):

| Machine | Idle | Idle glow | Active (animated) | Active glow (animated) |
|---|---|---|---|---|
| Kerr Ergosphere Engine | `OVERLAY_FRONT_KERR_ENGINE` | `OVERLAY_FRONT_KERR_ENGINE_GLOW` | `OVERLAY_FRONT_KERR_ENGINE_ACTIVE` | `OVERLAY_FRONT_KERR_ENGINE_ACTIVE_GLOW` |
| Eye of Harmony | `OVERLAY_FRONT_EYE_OF_HARMONY` | `OVERLAY_FRONT_EYE_OF_HARMONY_GLOW` | `OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE` | `OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE_GLOW` |

  Overlays are transparent outside the emblem, so they sit on the controller's casing background (Kerr: the
  `Casings.ExtremeDensitySpaceBendingCasing` texture index; Eye: TecTech's Reinforced Spatial Structure Casing index).
  Active files are vertical strips of 16×16 frames with a `.mcmeta` (`frametime` inside).

---

## Contract additions (structures)

Written by the structures unit on 2026-10-12, early, so the loader matches the files. The full "Structure export"
section (run command, legend, manifest) follows once the exporter is committed.

- **Blank rows are rows.** Shape rows are written with trailing spaces stripped, so a row that is all skip is an
  **empty line**. Inside a layer, every line up to the next `---` (or end of file) is a row, blank or not; the loader
  right-pads each to `w`. A layer always has exactly `h` lines. Only lines before the header and `#` lines are
  skipped.
- **Header** is exactly `# piece=<name> w=<w> h=<h> d=<d> off=<h>,<v>,<d> sha=<sha1>` on the first line. Extra
  `# key=value` lines may follow it (ignored by the loader).
- **Layer order**: layer 0 is the controller's front (the outermost +z layer of the mockup), rows run top to
  bottom, chars run left to right as seen from in front of the controller (mockup +x), so `[d][h]` is StructureLib's
  native layout with no `transpose`.
