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
import java.util.function.Supplier;

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
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsElements;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER PARTS, SL-P01..SL-P39 (singularity-ladder-build.md §2.2; SL-P29 comes
 * from the cost audit (SL-P27/P28 are retired for RC-2's x32 BEC recipes, SL-B01/B02),
 * docs/singularity-ladder-costs.md; SL-G01..G03 (GT's Resonance Chambers II, III and IV rewritten in place; they
 * retired SL-P39, SL-P33 and P4's SL-P40, now only their fallbacks) from the 2.9.0-RC-2 gate audit;
 * SL-P30..P32 are retired; SL-P35..P38 are the D-49 build's, d49-build-plan.md §10.3:
 * the T4-T6 star charts, the Sirius B, Cooled reclaim and the
 * Stellar Ignition Charge; SL-P26 is retired with the interim Black Dwarf Matter star; SL-P34 stays
 * reserved and unused, because the Eye T6 gate is clean on RC-2). <b>Owned by the
 * Foundation unit.</b> The Kerr's and the Eye's own recipes live in {@code BTSRecipesKerr} and
 * {@code BTSRecipesEye}.
 * <p>
 * <b>BEC expansion (ruling 11b, bec-expansion-proposal.md):</b> SL-P15, P16, P18, P19, P21, P22-P25, P35 and P37
 * moved to the Condensate Assembler ({@link BTSRecipesBEC}, SL-B20..B81); their old bodies live there only as
 * fallbacks for a skipped BEC recipe. What stays here: Kerr T1 (pre-BEC), SL-P17 (the HSM T4 gate; SL-B62 is a
 * batch alternative), SL-P29 and SL-G01..G03 (already BEC), SL-P36 and SL-P38.
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
        // The compression gate chambers (SL-G01..G03) are GT's own recipes rewritten at load-complete
        // (onLoadComplete); nothing is registered for them here.
        if (!Config.replaceTecTechEyeOfHarmony) {
            BeyondTheStars.LOG.info(
                "[BTS] replaceTecTechEyeOfHarmony=false: GT's Resonance Chambers II, III and IV keep their White Dwarf "
                    + "Matter, Black Dwarf Matter and MHDCSM foils (no SL-G01..G03 rewrites); TecTech's Eye still "
                    + "makes them.");
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
                    BTSRecipeGuard.alInputs(
                        BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 32L),
                        BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 64L),
                        nn(ItemList.Emitter_UIV.get(4L), "ItemList.Emitter_UIV"),
                        BTSRecipeGuard.anyCircuit(Materials.UIV, 4),
                        SLBlocks.casing(BlockSLCasing.MIRROR, 4)))
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

        // SL-P12 Harmonic Lining x4: the Kerr's white inner face (my ruling, round 4: one new Kerr
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

    // ============================== Compression gates: GT's Resonance Chambers II-IV rewritten in place (only with
    // TecTech's Eye replaced)

    /**
     * SL-G01..G03: GT's own Resonance Chamber II, III and IV recipes are rewritten IN PLACE (decided 2026-10-13: B-29
     * "Rewrite GT's in place" for chamber II; Q4/Q21 the same for III and IV), so NEI shows one recipe per chamber,
     * not GT's plus ours. Each swaps the one foil that waits on a later Eye tier for the tier below's product, at the
     * same input index:
     * <ul>
     * <li><b>SL-G01</b> Resonance Chamber II (compression tiers 1-3, the Eye T2-T4 gates): 1 White Dwarf Matter foil
     * (Eye T4) becomes 2 SpaceTime foils. Retired SL-P39.</li>
     * <li><b>SL-G02</b> Resonance Chamber III (compression tiers 4-5, the Eye T5 / T6 gates): 1 Black Dwarf Matter
     * foil (Eye T5) becomes 2 White Dwarf Matter foils (Eye T4). Retired SL-P33 (D-49).</li>
     * <li><b>SL-G03</b> Resonance Chamber IV (compression tier 8, the Eye T8 gate): 1 MHDCSM foil (Endgame, D-48;
     * Universium-made) becomes 2 SpaceTime foils, and 1,500 L of Entangled Ergomatter (Eye T7; the card's 1,440 L on
     * GT's half-unit rule) joins the condensates as the fourth. Retired SL-P40 (P4). Its Eternity plate is reachable
     * before T8 only through SL-Q26 (Eternity on Ergomatter).</li>
     * </ul>
     * Everything else stays GT 2.9.0-RC-2's ({@code BECMetaMaterialRecipes.registerResonanceChamber}): the other
     * inputs, the nanite tiers (one per input index, so each new foil keeps the old foil's nanite), the condensates,
     * 300 s at the chamber's voltage.
     * <p>
     * <b>Why (gate audit, 2.9.0-RC-2 port, B-29, B-35, P4 §6.4).</b> In RC-2 every Spacetime Compression Field
     * Generator is a Condensate Assembler recipe built on these chambers. With TecTech's Eye replaced, WDM, BDM and
     * MHDCSM first exist at or after the Eye tier the chamber gates, so a generator would need its own tier first.
     * <p>
     * <b>How.</b> The foil is swapped at the same array index, so the nanite-tier metadata (indexed by input) is
     * untouched; GT's map moved the condensates into the recipe's {@code CONDENSATE_INPUT} metadata when it compiled
     * the
     * recipe, so SL-G03 stores a new, longer array there (NEI and the BEC I/O nodes both read it). Then the map
     * rebuilds
     * its item-keyed lookup ({@code reInit()}), as {@link BTSBlackHoleSwap} does. Idempotent. It runs at load-complete
     * (from {@code CommonProxy.loadComplete}), because GT's Condensate Assembler recipes are not in the map yet when
     * our
     * postInit runs. Only with {@code replaceTecTechEyeOfHarmony=true}: with TecTech's Eye kept, the matter has its
     * stock source and GT's recipes stay as shipped.
     * <p>
     * <b>Fallbacks.</b> The retired ids keep their bodies ONLY for a pack that moved GT's recipe (none found to
     * rewrite): SL-P39, SL-P33 and SL-P40 (the same chamber with the same inputs, a separate Condensate Assembler
     * recipe), so the gates can never lose their chamber. Also retired: SL-P30..P32 (the old research-station "upgrade
     * the generator" recipes); the RC-2 upgrade path is {@link BTSRecipesGeneratorUpgrade}.
     */
    public static void onLoadComplete() {
        if (!Config.replaceTecTechEyeOfHarmony) return;
        chamber(
            "SL-G01",
            "SL-P39",
            "II",
            ItemList.MetaMaterial_ResonanceChamber2,
            Materials.WhiteDwarfMatter,
            Materials.SpaceTime,
            2,
            null,
            BTSRecipesSingularity::legacyP39);
        chamber(
            "SL-G02",
            "SL-P33",
            "III",
            ItemList.MetaMaterial_ResonanceChamber3,
            Materials.BlackDwarfMatter,
            Materials.WhiteDwarfMatter,
            2,
            null,
            BTSRecipesSingularity::legacyP33);
        chamber(
            "SL-G03",
            "SL-P40",
            "IV",
            ItemList.MetaMaterial_ResonanceChamber4,
            Materials.MHDCSM,
            Materials.SpaceTime,
            2,
            BTSRecipesQuasar::chamberIVErgomatter,
            BTSRecipesQuasar::legacyP40);
    }

    /**
     * One in-place chamber rewrite under {@code id}; if no GT recipe makes the chamber with the {@code from} foil (or
     * already with the new foil), the retired {@code fallbackId} registers its separate recipe instead.
     */
    private static void chamber(String id, String fallbackId, String numeral, ItemList chamberItem, Materials from,
        Materials to, int toCount, Supplier<FluidStack> extraCondensate, Runnable fallback) {
        final int before = BTSRecipeGuard.skippedCount();
        BTSRecipeGuard.recipe(id, () -> {
            if (rewriteChamber(id, numeral, chamberItem, from, to, toCount, extraCondensate) == 0) {
                throw new IllegalStateException(
                    "no Condensate Assembler recipe makes Resonance Chamber " + numeral
                        + " with a "
                        + from.mDefaultLocalName
                        + " foil");
            }
        });
        if (BTSRecipeGuard.skippedCount() != before) {
            BeyondTheStars.LOG.warn(
                "[BTS] {} found no GT Resonance Chamber {} recipe to rewrite; {} (the same chamber with {} {} foils, a "
                    + "separate Condensate Assembler recipe) is registered instead.",
                id,
                numeral,
                fallbackId,
                toCount,
                to.mDefaultLocalName);
            BTSRecipeLedger
                .fallback(fallbackId, id + " found no GT Resonance Chamber " + numeral + " recipe to rewrite");
            BTSRecipeLedger.gated("replaceTecTechEyeOfHarmony=true", () -> BTSRecipeGuard.recipe(fallbackId, fallback));
        }
    }

    /**
     * The body of SL-G01..G03: in every Condensate Assembler recipe making the chamber, the {@code from} foil input
     * becomes {@code toCount} {@code to} foils at the same index (and {@code extraCondensate}, if any, joins the
     * condensates). Returns how many recipes make the chamber with the new foils afterwards (rewritten now or
     * already); 0 means nothing was found.
     */
    private static int rewriteChamber(String id, String numeral, ItemList chamberItem, Materials from, Materials to,
        int toCount, Supplier<FluidStack> extraCondensate) {
        final ItemStack chamber = nn(chamberItem.get(1L), "ItemList." + chamberItem.name());
        final ItemStack newFoil = BTSRecipeGuard.req(OrePrefixes.foil, to, toCount);
        final FluidStack extra = extraCondensate == null ? null : extraCondensate.get();
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
                if (isFoilOf(in, to) && in.stackSize == toCount) {
                    done++;
                    break;
                }
                if (isFoilOf(in, from)) {
                    final int was = in.stackSize;
                    final java.util.Map<String, Object> snapshot = CommandBTSRecipes.snapshot(recipe);
                    recipe.mInputs[i] = newFoil.copy();
                    if (extra != null) addCondensate(recipe, extra);
                    final String what = toCount + " "
                        + to.mDefaultLocalName
                        + " foils in place of "
                        + was
                        + " "
                        + from.mDefaultLocalName
                        + " foil"
                        + (extra != null ? ", plus " + extra.amount + " L " + extra.getLocalizedName() : "");
                    BTSRecipeLedger.changed(
                        id,
                        CommandBTSRecipes.EYE,
                        "replaceTecTechEyeOfHarmony=true",
                        "GT's Resonance Chamber " + numeral
                            + ": "
                            + what
                            + " (nanites and the other condensates unchanged)",
                        TecTechRecipeMaps.condensateAssemblingRecipes,
                        recipe,
                        snapshot);
                    done++;
                    rewritten++;
                    BeyondTheStars.LOG.info(
                        "[BTS] {}: GT's Resonance Chamber {} (Condensate Assembler) takes {} (input {}; nanites and the "
                            + "other condensates unchanged).",
                        id,
                        numeral,
                        what,
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
            BeyondTheStars.LOG
                .warn("[BTS] {}: {} Condensate Assembler recipes make Resonance Chamber {}.", id, done, numeral);
        }
        return done;
    }

    /**
     * Bench ({@code /btseye bec}): every Condensate Assembler recipe that makes a Resonance Chamber I-IV, as NEI lists
     * them by output (the map's recipes), each with its items, condensates and whether the map's own lookup on its
     * inputs finds it. With {@code replaceTecTechEyeOfHarmony} on, chambers II-IV should show ONE recipe each, GT's,
     * with SL-G01..G03's foils.
     */
    public static List<String> chamberLines() {
        final List<String> out = new ArrayList<>();
        final ItemList[] chambers = { ItemList.MetaMaterial_ResonanceChamber1, ItemList.MetaMaterial_ResonanceChamber2,
            ItemList.MetaMaterial_ResonanceChamber3, ItemList.MetaMaterial_ResonanceChamber4 };
        for (int c = 0; c < chambers.length; c++) {
            final ItemStack chamber = chambers[c].get(1L);
            if (chamber == null) continue;
            final List<GTRecipe> makers = new ArrayList<>();
            for (GTRecipe r : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
                if (r == null || r.mOutputs == null) continue;
                for (ItemStack o : r.mOutputs) {
                    if (o != null && o.isItemEqual(chamber)) {
                        makers.add(r);
                        break;
                    }
                }
            }
            out.add("Resonance Chamber " + (c + 1) + ": " + makers.size() + " Condensate Assembler recipe(s)");
            for (GTRecipe r : makers) {
                final StringBuilder sb = new StringBuilder("  ");
                for (ItemStack in : r.mInputs) {
                    if (in == null) continue;
                    sb.append(in.stackSize)
                        .append("x ")
                        .append(in.getDisplayName())
                        .append(", ");
                }
                final FluidStack[] cond = r.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
                if (cond != null) {
                    for (FluidStack f : cond) {
                        if (f != null) sb.append(f.amount)
                            .append(" L ")
                            .append(f.getLocalizedName())
                            .append(", ");
                    }
                }
                final GTRecipe found = TecTechRecipeMaps.condensateAssemblingRecipes.findRecipeQuery()
                    .items(r.mInputs)
                    .find();
                sb.append(r.mDuration / 20)
                    .append(" s; lookup ")
                    .append(found == r ? "finds it" : "finds " + (found == null ? "nothing" : "another recipe"));
                out.add(sb.toString());
            }
        }
        return out;
    }

    /**
     * Appends {@code extra} to the recipe's condensates. GT's Condensate Assembler map moved the recipe's fluid inputs
     * into {@code CONDENSATE_INPUT} when it compiled the recipe (and left {@code mFluidInputs} empty); a new array is
     * stored, so the snapshot taken before keeps the pack's original.
     */
    private static void addCondensate(GTRecipe recipe, FluidStack extra) {
        final FluidStack[] had = recipe.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
        final FluidStack[] base = had != null ? had
            : recipe.mFluidInputs == null ? new FluidStack[0] : recipe.mFluidInputs;
        for (FluidStack f : base) {
            if (f != null && f.isFluidEqual(extra)) return;
        }
        if (base.length >= 4) {
            throw new IllegalStateException("the recipe already has 4 condensates (the Condensate Assembler's limit)");
        }
        final FluidStack[] now = new FluidStack[base.length + 1];
        for (int k = 0; k < base.length; k++) now[k] = base[k] == null ? null : base[k].copy();
        now[base.length] = extra.copy();
        if (had != null || recipe.mFluidInputs == null || recipe.mFluidInputs.length == 0) {
            recipe.getMetadataStorage()
                .store(GTRecipeConstants.CONDENSATE_INPUT, now);
        } else {
            recipe.mFluidInputs = now;
        }
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

    /**
     * SL-P33 (D-49; retired, fallback only): GT's Resonance Chamber III recipe with 2 White Dwarf Matter foils for its
     * Black Dwarf Matter foil, registered apart. GT's tier-3 chamber otherwise: 2 Emitter UIV, 2 Shirabon rods, 1
     * Hexanite plate; nanites White Dwarf / Gold / Transcendent / Gold; Chromatic Glass 16, Infinity 8, Boundless
     * Cosmic
     * Solder 3; 300 s at UMV.
     */
    private static void legacyP33() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                nn(ItemList.Emitter_UIV.get(2L), "ItemList.Emitter_UIV"),
                nn(GGMaterial.shirabon.get(OrePrefixes.stick, 2), "Shirabon rod"),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.WhiteDwarfMatter, 2L),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.Hexanite, 1L))
            .fluidInputs(
                entangled(CondensateType.ChromaticGlass, 16),
                entangled(CondensateType.Infinity, 8),
                entangled(CondensateType.BoundlessCosmicSolder, 3))
            .itemOutputs(nn(ItemList.MetaMaterial_ResonanceChamber3.get(1L), "ItemList.MetaMaterial_ResonanceChamber3"))
            .metadata(
                NANITE_TIERS,
                new NaniteTier[] { NaniteTier.WhiteDwarf, NaniteTier.Gold, NaniteTier.Transcendent, NaniteTier.Gold })
            .duration(300 * SECONDS)
            .eut(TierEU.RECIPE_UMV)
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
