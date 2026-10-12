package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;

import goodgenerator.items.GGMaterial;
import gregtech.api.casing.Casings;
import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsElements;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER PARTS, SL-P01..SL-P39 (singularity-ladder-build.md §2.2; SL-P29 comes
 * from the cost audit (SL-P27/P28 are retired for RC-2's x32 BEC recipes, SL-B01/B02),
 * docs/singularity-ladder-costs.md; SL-G01 (GT's Resonance Chamber II rewritten in place; it retired SL-P39, now
 * only its fallback) from the 2.9.0-RC-2 gate audit;
 * SL-P30..P32 are retired; SL-P33 and SL-P35..P38 are the D-49 build's, d49-build-plan.md §10.3:
 * the Eye T5 gate's Resonance Chamber III, the T4-T6 star charts, the Sirius B, Cooled reclaim and the
 * Stellar Ignition Charge; SL-P26 is retired with the interim Black Dwarf Matter star; SL-P34 stays
 * reserved and unused, because the Eye T6 gate is clean on RC-2). <b>Owned by the
 * Foundation unit.</b> The Kerr's and the Eye's own recipes live in {@code BTSRecipesKerr} and
 * {@code BTSRecipesEye}.
 * <p>
 * <b>BEC expansion (ruling 11b, bec-expansion-proposal.md):</b> SL-P15, P16, P18, P19, P21, P22-P25, P35 and P37
 * moved to the Condensate Assembler ({@link BTSRecipesBEC}, SL-B20..B81); their old bodies live there only as
 * fallbacks for a skipped BEC recipe. What stays here: Kerr T1 (pre-BEC), SL-P17 (the HSM T4 gate; SL-B62 is a
 * batch alternative), SL-P29, SL-P33 and SL-G01 (already BEC), SL-P36 and SL-P38.
 *
 * <h2>Deviations from the §2.2 table, and why</h2>
 *
 * <ul>
 * <li><b>Retired: SL-P01 (Containment Deck Plating), SL-P11 (Eye Shell Casing), SL-P13 (Spatial
 * Glass Channel).</b> I ruled (2026-10-12) that existing Eye of Harmony and Black Hole
 * Compressor blocks play those roles (Contract additions FC-1), so the blocks are not registered.
 * The IDs stay reserved, like SL-P20.</li>
 * <li><b>SL-P12 (Harmonic Lining) is back</b> as a Kerr-only block (round-4 ruling: one new white
 * Kerr lining block, slCasing2:2), x4 per craft with one fluid instead of the card's condensates.</li>
 * <li><b>SL-P21</b> (Stabiliser Pylon) takes TecTech's EOH casings in place of those three blocks.</li>
 * <li><b>One fluid per Assembler recipe.</b> GT's {@code assemblerRecipes} is {@code maxIO(9, 1, 1, 0)}:
 * a single-block Assembler has one fluid slot. Four cards had two. SL-P03 moves its SpaceTime into
 * two SpaceTime foils; SL-P07 drops the Lubricant and keeps the SpaceTime; SL-P16 keeps the Raw Star
 * Matter (the gate) and takes its 4,608 L of SpaceTime as 32 ingots; SL-P17 keeps the Hyperdense
 * Stellar Mixture (the T4 gate) and takes its 1,152 L of Transcendent Metal as 8 ingots.</li>
 * <li><b>Dense plates</b> go through {@link #dense(Materials, int)}: if GT never generated a
 * {@code plateDense} for a material (likely for SpaceTime), the recipe takes 9 plates per dense
 * plate instead, and the boot log says so once per material (spec §2.2).</li>
 * </ul>
 * Every recipe runs under {@link BTSRecipeGuard#recipe(String, Runnable)}; every foreign component
 * goes through {@code req}, {@code fluid} or {@code nn}.
 */
public final class BTSRecipesSingularity {

    private BTSRecipesSingularity() {}

    public static void register() {
        kerrParts();
        eyeParts();
        starCharts();
        ignitionCharge();
        // Our Eye needs SL-P29's casings whatever the flag says (B-26). Only the gate recipes exist
        // because TecTech's Eye (WDM's other source) is gone, so only they follow the flag.
        tecTechEyeBlocks();
        if (Config.replaceTecTechEyeOfHarmony) {
            BTSRecipeLedger.gated("replaceTecTechEyeOfHarmony=true", BTSRecipesSingularity::compressionGateParts);
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] replaceTecTechEyeOfHarmony=false: GT's Resonance Chamber II keeps its White Dwarf Matter foil "
                    + "(no SL-G01 rewrite), and SL-P33 "
                    + "(BDM-free Resonance Chamber III) is not registered; TecTech's Eye still makes White and "
                    + "Black Dwarf Matter.");
        }
    }

    // ================================================================ Kerr structure (UIV / UMV)

    private static void kerrParts() {
        // SL-P02 Mirror Backing Casing x2
        BTSRecipeGuard.recipe(
            "SL-P02",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 6L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L))
                .fluidInputs(molten(Materials.Infinity, 1_152))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.MIRROR_BACKING, 2))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P03 Superradiant Mirror Casing x2 (SpaceTime as foil: one fluid slot)
        BTSRecipeGuard.recipe(
            "SL-P03",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 8L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 2L),
                    SLBlocks.casing(BlockSLCasing.MIRROR_BACKING, 1))
                .fluidInputs(molten(Materials.Infinity, 576))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.MIRROR, 2))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P04 Superradiant Mirror Segment (component, x8 per Kerr controller). Research: a Superradiant
        // Mirror casing - addon-only, because the scanner writes only the FIRST recipe sharing a research
        // item and the Black Hole Seed is SL-K01's (BTSRecipeGuard.checkResearchCollisions).
        BTSRecipeGuard.recipe(
            "SL-P04",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.MIRROR, 1))
                .metadata(SCANNING, new Scanning(2 * MINUTES, TierEU.RECIPE_UIV))
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 32L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 64L),
                    nn(ItemList.Emitter_UIV.get(4L), "ItemList.Emitter_UIV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UIV, 4L),
                    SLBlocks.casing(BlockSLCasing.MIRROR, 4))
                .fluidInputs(molten(Materials.Infinity, 18_432), molten(Materials.SpaceTime, 2_304))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.superradiantMirrorSegment, 1))
                .duration(4_800)
                .eut(TierEU.RECIPE_UIV)
                .addTo(AssemblyLine));

        // SL-P05 Bolt Injector Rail Mk I
        BTSRecipeGuard.recipe(
            "SL-P05",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Robot_Arm_UIV.get(1L), "ItemList.Robot_Arm_UIV"),
                    nn(ItemList.Electric_Piston_UIV.get(1L), "ItemList.Electric_Piston_UIV"),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L),
                    BTSRecipeGuard.req(OrePrefixes.gearGt, Materials.SpaceTime, 2L))
                .fluidInputs(nn(Materials.Lubricant.getFluid(1_000L), "Materials.Lubricant fluid"))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1))
                .duration(300)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P06 Bolt Injector Rail Mk II
        BTSRecipeGuard.recipe(
            "SL-P06",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.INJECTOR_MK1, 1),
                    nn(ItemList.Conveyor_Module_UIV.get(2L), "ItemList.Conveyor_Module_UIV"),
                    nn(ItemList.Robot_Arm_UIV.get(1L), "ItemList.Robot_Arm_UIV"))
                .fluidInputs(nn(Materials.Lubricant.getFluid(2_000L), "Materials.Lubricant fluid"))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK2, 1))
                .duration(400)
                .eut(TierEU.RECIPE_UIV * 4)
                .addTo(assemblerRecipes));

        // SL-P07 Bolt Injector Rail Mk III (Lubricant dropped: one fluid slot)
        BTSRecipeGuard.recipe(
            "SL-P07",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.INJECTOR_MK2, 1),
                    nn(ItemList.Robot_Arm_UMV.get(1L), "ItemList.Robot_Arm_UMV"),
                    nn(ItemList.Electric_Piston_UMV.get(1L), "ItemList.Electric_Piston_UMV"),
                    nn(ItemList.Conveyor_Module_UMV.get(1L), "ItemList.Conveyor_Module_UMV"))
                .fluidInputs(molten(Materials.SpaceTime, 576))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.INJECTOR_MK3, 1))
                .duration(600)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P08 Ergosphere Field Coil. A Kerr T1 holds 1,258 coils (842 placed, 416 inside quadrupoles),
        // so 16 wires and 8,000 L, not the card's 64 and 32,000 L (cost audit #6).
        BTSRecipeGuard.recipe(
            "SL-P08",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUIV, 16L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 8L))
                .fluidInputs(superCoolant(8_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 1))
                .duration(1_200)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P09 Beam Focusing Quadrupole x4 (the card made 1; 208 per Kerr T1, cost audit #7)
        BTSRecipeGuard.recipe(
            "SL-P09",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                    nn(ItemList.Sensor_UIV.get(1L), "ItemList.Sensor_UIV"),
                    casingStack(Casings.ParticleBeamGuidancePipeCasing, 2),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 8L))
                .fluidInputs(superCoolant(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P10 Beam Dump
        BTSRecipeGuard.recipe(
            "SL-P10",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 8),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.CosmicNeutronium, 16L),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1L)))
                .fluidInputs(superCoolant(16_000))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.BEAM_DUMP, 1))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P12 Harmonic Lining x4: the Kerr's white inner face (user ruling, round 4: one new Kerr
        // lining block). Hundreds to thousands per Kerr (521 at T1 with both modules, 4,552 at T2), so a batch
        // Assembler recipe of plain UIV-tier metals; it replaces the BHC's Radiation Absorbent Casing in
        // that role, which costs 64,000 L of UU-Matter per block.
        BTSRecipeGuard.recipe(
            "SL-P12",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 6L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 8L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 1L))
                .fluidInputs(molten(Materials.Infinity, 576))
                .itemOutputs(SLBlocks.casing2(BlockSLCasing2.LINING, 4))
                .duration(600)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));

        // SL-P14 Ergosphere Extraction Casing x2
        BTSRecipeGuard.recipe(
            "SL-P14",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Neutronium, 2),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.SpaceTime, 1L),
                        // a Piston, not a Pump: the module runs item recipes (cost audit #14, B-17)
                        nn(ItemList.Electric_Piston_UIV.get(1L), "ItemList.Electric_Piston_UIV")))
                .fluidInputs(molten(Materials.SpaceTime, 288))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.EXTRACTION, 2))
                .duration(800)
                .eut(TierEU.RECIPE_UIV)
                .addTo(assemblerRecipes));
    }

    // ================================================================ Eye structure (UMV)

    private static void eyeParts() {
        // SL-P15 (Beam Band x16) and SL-P16 (Jet Crescent x16) moved to the Condensate Assembler as SL-B40 / SL-B51
        // (BTSRecipesBEC; their old bodies are its fallbacks). SL-P17 stays: HSM is the T4 gate (Q-B2).

        // SL-P17 Galactic Shell Plating x16 (Eye T4). HSM is the fluid (the T4 gate); the card's
        // 1,152 L of Transcendent Metal arrives as ingots. x16 on 250 L, not x2 on 1,000 L: the arms are
        // 5,325 blocks, which was 2.66M L HSM = 532 Arcturus runs; now about 83k L (cost audit #10).
        // A crafting recipe only: D-49 keeps HSM as the T4 gate and leaves this recipe alone.
        BTSRecipeGuard.recipe(
            "SL-P17",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        dense(Materials.Infinity, 8),
                        BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 2L),
                        BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 8L),
                        BTSRecipeGuard.req(OrePrefixes.ingot, Materials.TranscendentMetal, 16L)))
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 250))
                .itemOutputs(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 16))
                .duration(800)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));

        // SL-P21 (Stabiliser Pylon) moved to the Condensate Assembler as SL-B20 (BTSRecipesBEC).
    }

    // ======================================================================= TecTech Eye blocks in bulk

    /**
     * SL-P29: the batch Assembler recipe for the TecTech Eye block our Eye uses in bulk (cost audit #3/#4,
     * B-23): TecTech prices it for its own small Eye (36 Boundary casings); ours uses 6,426. The block and
     * the look stay TecTech's (round-1 ruling). Registered whatever {@code replaceTecTechEyeOfHarmony}
     * says: our Eye is craftable either way and needs it (B-26). TecTech's own assembly line stays as well.
     * SL-P27/P28 (Reinforced Spatial / Temporal Structure Casing x4) are retired by ruling 11a: RC-2's own
     * Condensate Assembler recipes for those two casings output x32 instead (SL-B01/B02, {@link BTSRecipesBEC}).
     */
    private static void tecTechEyeBlocks() {
        // SL-P29 Infinite SpaceTime Energy Boundary Casing x8: TecTech's needs an LSC, 16 Active
        // Transformers and 4 UMV wireless hatches per block. The SC UMV wire (TecTech's research item for
        // it) and an Emitter UIV keep the energy theme.
        BTSRecipeGuard.recipe(
            "SL-P29",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    items(
                        BTSRecipeGuard.req(OrePrefixes.wireGt16, Materials.SuperconductorUMV, 1L),
                        dense(Materials.Neutronium, 8),
                        dense(Materials.CosmicNeutronium, 8),
                        nn(ItemList.Emitter_UIV.get(1L), "ItemList.Emitter_UIV")))
                .fluidInputs(molten(Materials.SpaceTime, 1_152))
                .itemOutputs(tectech(CustomItemList.EOH_Infinite_Energy_Casing, 8, "EOH_Infinite_Energy_Casing"))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ============================== Compression gate: the WDM-free Resonance Chamber II (only with TecTech's Eye
    // replaced)

    /**
     * SL-G01: GT's own Resonance Chamber II recipe is rewritten IN PLACE to take 2 SpaceTime foils where it took 1
     * White Dwarf Matter foil (user, 2026-10-13, B-29: "Rewrite GT's in place"), so NEI shows one recipe for the
     * chamber, not GT's plus ours. Everything else stays GT 2.9.0-RC-2's
     * ({@code BECMetaMaterialRecipes.registerResonanceChamber}, tier 2): the other inputs, the nanite tiers (one per
     * input index, so the foil keeps its Gold nanite), the condensates, 300 s at UIV.
     * <p>
     * <b>Why (gate audit, 2.9.0-RC-2 port, B-29).</b> In RC-2 every Spacetime Compression Field Generator is a
     * Condensate Assembler recipe. Tiers 1-3 (TecTech metas 1-3: the 1:1 gates of Eye T2/T3/T4) take 1 / 4 / 16
     * Resonance Chamber II, and each chamber takes a White Dwarf Matter foil. With TecTech's Eye replaced, WDM's only
     * source is our Eye T4, so Eye T2 would need Eye T4 first. The chamber still takes Boundless Cosmic Solder, which
     * is made from Raw Star Matter (our Eye T1), so the generators still need the tier below, never a tier above.
     * <p>
     * <b>How.</b> The stack is swapped at the same array index, so the nanite-tier metadata (indexed by input) and
     * the condensates (moved into metadata when GT compiled the recipe) are untouched; then the map rebuilds its
     * item-keyed lookup ({@code reInit()}), as {@link BTSBlackHoleSwap} does. Idempotent. It runs at load-complete
     * (from {@code CommonProxy.loadComplete}), because GT's Condensate Assembler recipes are not in the map yet when
     * our postInit runs (seen on the RC-2 dev server: nothing to rewrite at postInit, the BHC swap finds all ten at
     * load-complete). Only with
     * {@code replaceTecTechEyeOfHarmony=true}: with TecTech's Eye kept, WDM has its stock source and GT's recipe stays
     * as shipped.
     * <p>
     * <b>Retired: SL-P39</b> (the separate Condensate Assembler recipe with the same inputs). Its ID stays reserved and
     * its body is kept below ONLY as the fallback when no GT recipe for the chamber is found to rewrite (a pack that
     * moved it), so the Eye T2-T4 gates can never lose their chamber. Also retired: SL-P30..P32 (the old
     * research-station "upgrade the generator" recipes); the RC-2 upgrade path is {@link BTSRecipesGeneratorUpgrade}.
     */
    public static void onLoadComplete() {
        if (!Config.replaceTecTechEyeOfHarmony) return;
        final int before = BTSRecipeGuard.skippedCount();
        BTSRecipeGuard.recipe("SL-G01", () -> {
            if (rewriteResonanceChamberII() == 0) {
                throw new IllegalStateException(
                    "no Condensate Assembler recipe makes Resonance Chamber II with a White Dwarf Matter foil");
            }
        });
        if (BTSRecipeGuard.skippedCount() != before) {
            BeyondTheStars.LOG.warn(
                "[BTS] SL-G01 found no GT Resonance Chamber II recipe to rewrite; SL-P39 (the same chamber with 2 "
                    + "SpaceTime foils, a separate Condensate Assembler recipe) is registered instead.");
            BTSRecipeLedger.fallback("SL-P39", "SL-G01 found no GT Resonance Chamber II recipe to rewrite");
            BTSRecipeLedger.gated(
                "replaceTecTechEyeOfHarmony=true",
                () -> BTSRecipeGuard.recipe("SL-P39", BTSRecipesSingularity::legacyP39));
        }
    }

    /** The gate parts registered at postInit with the flag on: SL-P33 (SL-G01 runs at load-complete). */
    private static void compressionGateParts() {
        // SL-P33 (D-49): MetaMaterial Resonance Chamber III with 2 White Dwarf Matter foils in place of GT's 1
        // Black Dwarf Matter foil. GT's tier-3 chamber (BECMetaMaterialRecipes.registerResonanceChamber, t = 2)
        // otherwise unchanged: 2 Emitter UIV, 2 Shirabon rods, 1 Hexanite plate; nanites White Dwarf / Gold /
        // Transcendent / Gold; Chromatic Glass 16, Infinity 8, Boundless Cosmic Solder 3; 300 s at UMV.
        // Why: compression generator tier index 4 (the Eye T5 gate) takes 1 Resonance Chamber III, and with the
        // interim Eye T4 BDM star gone (D-49) Black Dwarf Matter first exists at Eye T5 itself. White Dwarf
        // Matter is the tier below's product (Eye T4), the same "previous tier" step as SL-P39's SpaceTime for
        // WDM. Tier index 5 (the Eye T6 gate) takes 4 chambers but may use BDM, so it needs no substitute and
        // SL-P34 stays unused.
        BTSRecipeGuard.recipe(
            "SL-P33",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Emitter_UIV.get(2L), "ItemList.Emitter_UIV"),
                    nn(GGMaterial.shirabon.get(OrePrefixes.stick, 2), "Shirabon rod"),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.WhiteDwarfMatter, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Hexanite, 1L))
                .fluidInputs(
                    entangled(CondensateType.ChromaticGlass, 16),
                    entangled(CondensateType.Infinity, 8),
                    entangled(CondensateType.BoundlessCosmicSolder, 3))
                .itemOutputs(
                    nn(ItemList.MetaMaterial_ResonanceChamber3.get(1L), "ItemList.MetaMaterial_ResonanceChamber3"))
                .metadata(
                    NANITE_TIERS,
                    new NaniteTier[] { NaniteTier.WhiteDwarf, NaniteTier.Gold, NaniteTier.Transcendent,
                        NaniteTier.Gold })
                .duration(300 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(TecTechRecipeMaps.condensateAssemblingRecipes));
    }

    /**
     * SL-G01's body: in every Condensate Assembler recipe making Resonance Chamber II, the White Dwarf Matter foil
     * input becomes 2 SpaceTime foils, at the same index. Returns how many recipes make the chamber with the
     * SpaceTime foils afterwards (rewritten now or already); 0 means nothing was found.
     */
    private static int rewriteResonanceChamberII() {
        final ItemStack chamber = nn(
            ItemList.MetaMaterial_ResonanceChamber2.get(1L),
            "ItemList.MetaMaterial_ResonanceChamber2");
        final ItemStack spaceTime = BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 2L);
        int done = 0, rewritten = 0;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null || recipe.mInputs == null) continue;
            boolean makesChamber = false;
            for (ItemStack out : recipe.mOutputs) {
                if (out != null && out.isItemEqual(chamber)) makesChamber = true;
            }
            if (!makesChamber) continue;
            for (int i = 0; i < recipe.mInputs.length; i++) {
                final ItemStack in = recipe.mInputs[i];
                if (in == null) continue;
                if (in.isItemEqual(spaceTime) && in.stackSize == 2) {
                    done++;
                    break;
                }
                if (isFoilOf(in, Materials.WhiteDwarfMatter)) {
                    final int was = in.stackSize;
                    final java.util.Map<String, Object> before = CommandBTSRecipes.snapshot(recipe);
                    recipe.mInputs[i] = spaceTime.copy();
                    BTSRecipeLedger.changed(
                        "SL-G01",
                        CommandBTSRecipes.EYE,
                        "replaceTecTechEyeOfHarmony=true",
                        "GT's Resonance Chamber II: 2 SpaceTime foils in place of " + was
                            + " White Dwarf Matter foil (nanites and condensates unchanged)",
                        TecTechRecipeMaps.condensateAssemblingRecipes,
                        recipe,
                        before);
                    done++;
                    rewritten++;
                    BeyondTheStars.LOG.info(
                        "[BTS] SL-G01: GT's Resonance Chamber II (Condensate Assembler) takes 2 SpaceTime foils "
                            + "in place of {} White Dwarf Matter foil (input {}; nanites and condensates unchanged).",
                        was,
                        i + 1);
                    break;
                }
            }
        }
        if (rewritten > 0) {
            TecTechRecipeMaps.condensateAssemblingRecipes.getBackend()
                .reInit();
        }
        if (done > 1) {
            BeyondTheStars.LOG.warn("[BTS] SL-G01: {} Condensate Assembler recipes make Resonance Chamber II.", done);
        }
        return done;
    }

    private static boolean isFoilOf(ItemStack stack, Materials material) {
        final ItemData data = GTOreDictUnificator.getAssociation(stack);
        return data != null && data.mPrefix == OrePrefixes.foil
            && data.mMaterial != null
            && data.mMaterial.mMaterial == material;
    }

    /** SL-P39 (retired, fallback only): GT's Resonance Chamber II recipe with 2 SpaceTime foils, registered apart. */
    private static void legacyP39() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                nn(ItemList.Emitter_UEV.get(2L), "ItemList.Emitter_UEV"),
                nn(MaterialsElements.STANDALONE.HYPOGEN.getRod(2), "Hypogen rod"),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 2L),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.ProtoHalkonite, 1L))
            .fluidInputs(
                entangled(CondensateType.ChromaticGlass, 12),
                entangled(CondensateType.Infinity, 6),
                entangled(CondensateType.BoundlessCosmicSolder, 2))
            .itemOutputs(nn(ItemList.MetaMaterial_ResonanceChamber2.get(1L), "ItemList.MetaMaterial_ResonanceChamber2"))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.SixPhasedCopper, NaniteTier.Silver, NaniteTier.Gold, NaniteTier.Silver })
            .duration(300 * SECONDS)
            .eut(TierEU.RECIPE_UIV)
            .addTo(TecTechRecipeMaps.condensateAssemblingRecipes);
    }

    /** {@code units} of a condensate's entangled fluid, in the condensate's own unit (GT's convention). */
    private static FluidStack entangled(CondensateType type, int units) {
        return nn(type.getEntangled(units * type.getUnit()), "entangled " + type.name());
    }

    // ================================================================ Star charts (Eye targets)

    private static void starCharts() {
        // Each chart's fluid is the previous tier's product, so a chart cannot be crafted before
        // its tier is reachable (§2.2).

        // SL-P22..P25, SL-P35 and SL-P37 (the six charts) moved to the Condensate Assembler as SL-B21, B41, B52,
        // B61, B71 and B81 (BTSRecipesBEC; the old bodies are its fallbacks). SL-P26 is retired with D-49.

        // SL-P36 Reclaim: an obsolete Star Chart: Sirius B, Cooled back into Star Chart: Sirius B
        // (programmed circuit 3, not consumed). No fluid: the old chart already paid for its data.
        BTSRecipeGuard.recipe(
            "SL-P36",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.stack(SLItems.starChartSiriusBCooled, 1), GTUtility.getIntegratedCircuit(3))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1))
                .duration(100)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ================================================================ Stellar Ignition Charge (D-49)

    /**
     * SL-P38 Stellar Ignition Charge x4: one lights a dark T1-T3 star (d49-build-plan.md §6.3), and a failed
     * star needs a new one. Helium plasma is the spark; Infinity plates and a UMV emitter hold it. Sixteen
     * also go into the Stellar Mass Core (SL-E03), which researches from the charge.
     */
    private static void ignitionCharge() {
        BTSRecipeGuard.recipe(
            "SL-P38",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 4L),
                    nn(ItemList.Emitter_UMV.get(1L), "ItemList.Emitter_UMV"),
                    BTSRecipeGuard.req(OrePrefixes.circuit, Materials.UMV, 1L))
                .fluidInputs(nn(Materials.Helium.getPlasma(1_000L), "Materials.Helium plasma"))
                .itemOutputs(BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 4))
                .duration(200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
    }

    // ================================================================ helpers

    private static final Set<Materials> DENSE_FALLBACK_LOGGED = new HashSet<>();

    /**
     * {@code count} dense plates of {@code material}, or - if GT never generated a {@code plateDense}
     * for it - {@code 9 x count} plain plates in stacks of at most 64 (spec §2.2). The fallback is
     * logged once per material so the deviation is visible in the boot log. Throws through
     * {@link BTSRecipeGuard#req} if even the plate is missing.
     * <p>
     * Public so the Kerr and Eye recipe files can share the same rule for their dense SpaceTime.
     */
    public static ItemStack[] dense(Materials material, int count) {
        final ItemStack dense = GTOreDictUnificator.get(OrePrefixes.plateDense, material, count);
        if (dense != null) {
            return new ItemStack[] { dense };
        }
        if (DENSE_FALLBACK_LOGGED.add(material)) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: GT has no Dense {} Plate in this pack; the Singularity Ladder recipe takes 9 plates "
                    + "per dense plate instead.",
                BTSRecipeGuard.currentRecipe(),
                material);
        }
        final List<ItemStack> plates = new ArrayList<>();
        int left = count * 9;
        while (left > 0) {
            final int n = Math.min(64, left);
            plates.add(BTSRecipeGuard.req(OrePrefixes.plate, material, n));
            left -= n;
        }
        return plates.toArray(new ItemStack[0]);
    }

    /** Flattens ItemStacks and ItemStack[]s (from {@link #dense}) into one input array. */
    private static ItemStack[] items(Object... parts) {
        final List<ItemStack> out = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemStack) {
                out.add((ItemStack) part);
            } else if (part instanceof ItemStack[]) {
                for (ItemStack stack : (ItemStack[]) part) {
                    out.add(stack);
                }
            } else {
                throw new IllegalArgumentException("not an item input: " + part);
            }
        }
        return out.toArray(new ItemStack[0]);
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static FluidStack molten(Materials material, int litres) {
        return nn(material.getMolten(litres), "molten " + material);
    }

    private static FluidStack superCoolant(int litres) {
        return nn(Materials.SuperCoolant.getFluid(litres), "Materials.SuperCoolant fluid");
    }

    private static ItemStack tectech(CustomItemList entry, int count, String what) {
        if (!entry.hasBeenSet()) {
            return nn(null, "CustomItemList." + what);
        }
        return nn(entry.get(count), "CustomItemList." + what);
    }

    private static ItemStack casingStack(Casings casing, int count) {
        final Block block = nn(casing.getBlock(), "Casings." + casing.name());
        return new ItemStack(block, count, casing.getBlockMeta());
    }
}
