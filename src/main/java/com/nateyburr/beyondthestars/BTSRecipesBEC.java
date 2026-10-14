package com.nateyburr.beyondthestars;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.NANITE_TIERS;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLCondensates;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.CondensateType;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.NaniteTier;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * THE BOSE-EINSTEIN CONDENSATE LINE (ruling 11; designs/docs/bec-expansion-proposal.md, all 14 open questions
 * taken as recommended). <b>One owner: the BEC unit.</b> Called from one line in {@link BTSRecipes} ("bec", after
 * "stellar") and, for the load-complete rewrites, from {@link CommonProxy#loadComplete}.
 *
 * <h2>What is here</h2>
 * <ul>
 * <li><b>SL-B01..B04</b> (load-complete, ruling 11a): RC-2's Condensate Assembler recipes for TecTech's Reinforced
 * Spatial / Temporal Structure Casing output x32 instead of x4; fallbacks below.</li>
 * <li><b>SL-B10..B15</b>: six addon condensates ({@link SLCondensates}) and their Condensate Generator recipes. SL-B16
 * (Magnetar Matter) is reserved for P4 (Q-B6).</li>
 * <li><b>SL-B20..B81</b>: 20 Condensate Assembler recipes. 18 replace an Assembler or assembly-line recipe (the
 * replaced ID is retired from its old file and kept HERE only as a fallback); SL-B34 (Harmonic Lining x64) and SL-B62
 * (Galactic Shell Plating x64) are batch alternatives next to SL-P12 / SL-P17, which stay.</li>
 * </ul>
 *
 * <h2>The safety net (proposal §6.3, Q-B8: no config switch)</h2>
 * Every replacement runs through {@link #replace}: if the BEC recipe is skipped (a missing metamaterial, a condensate
 * whose generator recipe failed, GT rejecting the recipe), the old recipe body registers instead, verbatim, and the
 * boot log says so. So no Eye or Kerr part can become uncraftable because of this file. Retired for good (no
 * fallback): SL-P27 / SL-P28, replaced by SL-B01/B02's x32 (they stay only as SL-B03/B04's last resort).
 *
 * <h2>Rules every recipe follows (proposal §2)</h2>
 * <ul>
 * <li><b>Gate audit (rule 8, R2):</b> a part first needed to build or unlock Eye tier N uses only products of tiers
 * below N (a transition core, made while running tier N, may use tier N's product). So Eye T1 parts carry no Raw Star
 * Matter or Boundless Cosmic Solder and nanites at most Transcendent; White Dwarf nanites/metamaterials appear first
 * in the T4 -> T5 core, Black Dwarf ones in the T5 -> T6 core. No compression generator recipe is touched. The audit
 * covers GT's parts too: RC-2's UXV Field Generator / Sensor / Emitter need molten Universium and MHDCSM (Eye T10
 * with the TecTech Eye replaced), so the T4 -> T5 and T5 -> T6 cores take twice as many UMV parts instead (B-72;
 * the D-45 override of RC-2's UXV part recipes is still open). The UXV circuits are unchanged and not re-audited
 * here.</li>
 * <li><b>Families (R3):</b> each recipe uses at most 4 condensates from one family (RC-2 family 1; S1 "Stellar": Raw
 * Star Matter, Excited Stellar Plasma, Hyperdense Stellar Mixture, SpaceTime, Space, Infinity, Transcendent Metal;
 * S2 "Remnant": White / Black Dwarf Matter, Antipolarization Agent, SpaceTime, DSS, Hyperdense), so one network runs
 * a tier's set without tripping the "more than 3 unrequested condensates" failure.</li>
 * <li><b>Unique item sets:</b> the Condensate Assembler looks recipes up by items only, so no two recipes here differ
 * only in condensates (each chart has its own signature item, circuit and Sensor Array tier).</li>
 * <li>Kerr T1 is pre-BEC and untouched; controllers stay assembly lines.</li>
 * </ul>
 * Ruling 19's loop fixes (no Magnetar Matter in the Homogeny core, no Ergomatter in pre-T7 UXV parts) touch none of
 * these recipes: nothing here uses Magnetar Matter or Ergomatter.
 */
public final class BTSRecipesBEC {

    /** The rewritten output size (ruling 11a). */
    public static final int CASINGS_PER_CRAFT = 32;

    // Nanite tiers by the proposal's numbers (GT's NaniteTier.tier): 1 Carbon .. 7 Black Dwarf.
    private static final NaniteTier N1 = NaniteTier.Carbon;
    private static final NaniteTier N2 = NaniteTier.Silver;
    private static final NaniteTier N3 = NaniteTier.Gold;
    private static final NaniteTier N4 = NaniteTier.Transcendent;
    private static final NaniteTier N5 = NaniteTier.SixPhasedCopper;
    private static final NaniteTier N6 = NaniteTier.WhiteDwarf;
    private static final NaniteTier N7 = NaniteTier.BlackDwarf;

    /** Our condensates GT already makes from the same source fluid: ours -> GT's (proposal §4). */
    private static final Map<Fluid, Fluid> RESOLVED = new HashMap<>();
    /** Our condensates that have a working generator recipe (ours or GT's). */
    private static final Set<Fluid> MAKEABLE = new HashSet<>();

    private static int fallbacks;

    private BTSRecipesBEC() {}

    // =================================================================================== registration

    public static void register() {
        fallbacks = 0;
        RESOLVED.clear();
        MAKEABLE.clear();
        condensates();
        eyeT1();
        kerrT2();
        stellar();
        remnant();
        if (fallbacks == 0) {
            BeyondTheStars.LOG.info(
                "[BTS] BEC line: {} addon condensates makeable, Condensate Assembler recipes SL-B20..B81 in place, "
                    + "no legacy fallbacks.",
                MAKEABLE.size());
        } else {
            BeyondTheStars.LOG.warn(
                "[BTS] BEC line: {} Condensate Assembler recipe(s) could not be built; their old Assembler / "
                    + "assembly-line recipes were registered instead (see the SKIPPED lines).",
                fallbacks);
        }
    }

    // =================================================================================== test bench

    /**
     * {@code /btseye bec} (test bench, B-71): what the server's Condensate Assembler and Condensate Generator maps
     * really hold for us - every recipe that makes one of our items or machines, whether GT's own lookup with that
     * recipe's inputs finds that recipe (no shadowing), and the generator recipes behind our condensates.
     */
    public static List<String> benchLines() {
        final List<String> out = new ArrayList<>();
        int ours = 0, unique = 0;
        for (GTRecipe r : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
            final ItemStack o = r.mOutputs != null && r.mOutputs.length > 0 ? r.mOutputs[0] : null;
            if (!isOurs(o)) continue;
            ours++;
            final GTRecipe found = TecTechRecipeMaps.condensateAssemblingRecipes.findRecipeQuery()
                .items(r.mInputs)
                .fluids(r.mFluidInputs)
                .find();
            if (found == r) unique++;
            final StringBuilder fl = new StringBuilder();
            // the map moves the condensates into the recipe's metadata at compile (RC-2 TecTechRecipeMaps)
            final FluidStack[] cond = r.getMetadata(gregtech.api.util.GTRecipeConstants.CONDENSATE_INPUT);
            if (cond != null) {
                for (FluidStack f : cond) {
                    if (f == null) continue;
                    if (fl.length() > 0) fl.append(", ");
                    fl.append(f.amount)
                        .append(" L ")
                        .append(
                            f.getFluid()
                                .getName());
                }
            }
            out.add(
                "  " + o.stackSize
                    + "x "
                    + o.getDisplayName()
                    + " <- "
                    + (r.mInputs == null ? 0 : r.mInputs.length)
                    + " item stacks; "
                    + fl
                    + "; "
                    + r.mDuration / 20
                    + " s"
                    + (found == r ? ""
                        : "; LOOKUP finds "
                            + (found == null || found.mOutputs == null || found.mOutputs.length == 0 ? "nothing"
                                : found.mOutputs[0].getDisplayName())));
        }
        final Set<Fluid> condensates = new HashSet<>();
        for (Fluid f : new Fluid[] { SLCondensates.entangledStarMatter, SLCondensates.entangledExcitedPlasma,
            SLCondensates.entangledHyperdense, SLCondensates.entangledWhiteDwarf, SLCondensates.entangledBlackDwarf,
            SLCondensates.entangledAgent }) {
            if (f != null) condensates.add(RESOLVED.getOrDefault(f, f));
        }
        int generators = 0;
        for (GTRecipe r : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
            if (r.mFluidOutputs == null || r.mFluidOutputs.length == 0 || r.mFluidOutputs[0] == null) continue;
            if (!condensates.contains(r.mFluidOutputs[0].getFluid())) continue;
            generators++;
            out.add(
                "  generator: "
                    + (r.mFluidInputs != null && r.mFluidInputs.length > 0 && r.mFluidInputs[0] != null
                        ? r.mFluidInputs[0].amount + " L "
                            + r.mFluidInputs[0].getFluid()
                                .getName()
                        : "?")
                    + " -> "
                    + r.mFluidOutputs[0].amount
                    + " L "
                    + r.mFluidOutputs[0].getFluid()
                        .getName());
        }
        out.add(
            0,
            "Condensate Assembler: " + ours
                + " recipe(s) make Beyond the Stars parts (20 SL-B2x..B8x expected), "
                + unique
                + " found by GT's own lookup with their inputs; Condensate Generator: "
                + generators
                + " recipe(s) for our "
                + condensates.size()
                + " condensate(s) (6 makeable at load: "
                + MAKEABLE.size()
                + "); legacy fallbacks fired at load: "
                + fallbacks);
        return out;
    }

    private static boolean isOurs(ItemStack o) {
        if (o == null || o.getItem() == null) return false;
        final cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier id = cpw.mods.fml.common.registry.GameRegistry
            .findUniqueIdentifierFor(o.getItem());
        if (id != null && "beyondthestars".equals(id.modId)) return true;
        return o.getItem() == net.minecraft.item.Item.getItemFromBlock(gregtech.api.GregTechAPI.sBlockMachines)
            && o.getItemDamage() >= 28100
            && o.getItemDamage() < 28140;
    }

    // ------------------------------------------------------------------------- SL-B10..B15 condensates

    /**
     * One generator recipe per addon condensate, source amount -> the same amount of condensate (GT's pattern).
     * Tiers rise one step per product tier like GT's table (Q-B13). If GT already has a generator recipe on the
     * same source fluid (a future GT condensate), ours is skipped and recipes use GT's fluid.
     */
    private static void condensates() {
        generator(
            "SL-B10",
            SLCondensates.entangledStarMatter,
            () -> Materials.RawStarMatter.getFluid(1_000L),
            30 * SECONDS,
            TierEU.RECIPE_UIV);
        generator(
            "SL-B11",
            SLCondensates.entangledExcitedPlasma,
            () -> BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 1_000),
            60 * SECONDS,
            TierEU.RECIPE_UMV);
        generator(
            "SL-B12",
            SLCondensates.entangledHyperdense,
            () -> BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 1_000),
            90 * SECONDS,
            TierEU.RECIPE_UMV);
        generator(
            "SL-B13",
            SLCondensates.entangledWhiteDwarf,
            () -> Materials.WhiteDwarfMatter.getMolten(144L),
            120 * SECONDS,
            TierEU.RECIPE_UXV);
        generator(
            "SL-B14",
            SLCondensates.entangledBlackDwarf,
            () -> Materials.BlackDwarfMatter.getMolten(144L),
            150 * SECONDS,
            TierEU.RECIPE_UXV);
        generator(
            "SL-B15",
            SLCondensates.entangledAgent,
            () -> BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 1_000),
            60 * SECONDS,
            TierEU.RECIPE_UMV);
    }

    private static void generator(String id, Fluid entangled, Supplier<FluidStack> source, int ticks, long eut) {
        BTSRecipeGuard.recipe(id, () -> {
            final Fluid ours = BTSRecipeGuard.nn(entangled, id + " condensate fluid");
            final FluidStack src = BTSRecipeGuard.nn(source.get(), id + " source fluid");
            for (GTRecipe recipe : TecTechRecipeMaps.condensateGeneratorRecipes.getAllRecipes()) {
                if (recipe == null || recipe.mFluidInputs == null
                    || recipe.mFluidInputs.length == 0
                    || recipe.mFluidOutputs == null
                    || recipe.mFluidOutputs.length == 0
                    || recipe.mFluidInputs[0] == null
                    || recipe.mFluidOutputs[0] == null) continue;
                if (recipe.mFluidInputs[0].getFluid() != src.getFluid()) continue;
                final Fluid theirs = recipe.mFluidOutputs[0].getFluid();
                RESOLVED.put(ours, theirs);
                MAKEABLE.add(ours);
                BeyondTheStars.LOG.info(
                    "[BTS] {}: the Condensate Generator already makes {} from {}; Beyond the Stars' recipes use it "
                        + "instead of {}.",
                    id,
                    theirs.getName(),
                    src.getFluid()
                        .getName(),
                    ours.getName());
                return;
            }
            added(
                GTValues.RA.stdBuilder()
                    .fluidInputs(src)
                    .fluidOutputs(new FluidStack(ours, src.amount))
                    .duration(ticks)
                    .eut(eut),
                TecTechRecipeMaps.condensateGeneratorRecipes);
            MAKEABLE.add(ours);
        });
    }

    // ---------------------------------------------------------------- S0: the Eye T1 BOM (RC-2 family 1)

    private static void eyeT1() {
        // SL-B20 Stabiliser Pylon (x6 per Eye). Replaces SL-P21; P21's Super Coolant gives way to two tier-I
        // metamaterials, its SpaceTime stays (as condensate).
        replace(
            "SL-B20",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1),
                1_800 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(
                    entangled(CondensateType.SpaceTime, 256 * INGOTS),
                    entangled(CondensateType.Neutronium, 64 * INGOTS),
                    entangled(CondensateType.Space, 16 * INGOTS),
                    entangled(CondensateType.Time, 16 * INGOTS)),
                s(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 32), N4),
                s(tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 16), N4),
                s(tectech(CustomItemList.EOH_Infinite_Energy_Casing, 16), N3),
                s(item(ItemList.Field_Generator_UMV, 8), N3),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Neutronium, 16L), N1),
                s(circuit(Materials.UMV, 8), N2),
                s(BTSRecipesSingularity.dense(Materials.Neutronium, 16), N1),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 4), N4),
                s(item(ItemList.MetaMaterial_Shielding1, 8), N2)),
            "SL-P21",
            BTSRecipesBEC::legacyP21);

        // SL-B21 Star Chart: Sol. Replaces SL-P22.
        replace(
            "SL-B21",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartSol, 1),
                120 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.SpaceTime, INGOTS)),
                s(dataOrb(), N1),
                s(circuit(Materials.UMV, 1), N2),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 4L), N1),
                s(item(ItemList.MetaMaterial_SensorArray1, 1), N3)),
            "SL-P22",
            BTSRecipesBEC::legacyP22);

        // SL-B22 Stellar Field Sensor Hatch (MTE 28116, 8 per Eye). Replaces SL-E07.
        replace(
            "SL-B22",
            () -> assemble(
                BTSRecipeGuard.nn(BTSMachines.stellarFieldSensor, "BTSMachines.stellarFieldSensor")
                    .copy(),
                60 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.SpaceTime, 4 * INGOTS)),
                s(item(ItemList.Hull_UMV, 1), N2),
                s(item(ItemList.Sensor_UMV, 2), N3),
                s(item(ItemList.MetaMaterial_SensorArray1, 1), N4),
                s(circuit(Materials.UMV, 2), N2),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.SpaceTime, 4L), N1)),
            "SL-E07",
            BTSRecipesBEC::legacyE07);
    }

    // ------------------------------------------------------------------- S1: Kerr T2 (after Eye T1)

    private static void kerrT2() {
        // SL-B31 Kerr Density Upgrade. Replaces SL-K04 (Q-B1: no research step). Excited DTEC (not a condensate)
        // gives way to the Field Manipulators, Valves and Space.
        replace(
            "SL-B31",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.kerrDensityUpgrade, 1),
                3_600 * SECONDS,
                TierEU.RECIPE_UMV * 4,
                cond(
                    ours(SLCondensates.entangledStarMatter, 4_000_000),
                    entangled(CondensateType.SpaceTime, 4_096 * INGOTS),
                    entangled(CondensateType.Space, 64 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 64), N1),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 64), N1),
                s(BTSRecipesSingularity.dense(Materials.Infinity, 64), N1),
                s(item(ItemList.Field_Generator_UMV, 32), N3),
                s(circuit(Materials.UMV, 32), N2),
                s(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 32), N2),
                s(item(ItemList.Black_Hole_Opener, (int) BTSRecipesKerr.DENSITY_UPGRADE_SEEDS), N4),
                s(SLBlocks.casing(BlockSLCasing.INJECTOR_MK3, 16), N3),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 8), N4),
                s(item(ItemList.MetaMaterial_ElectrograviticValve1, 8), N3)),
            "SL-K04",
            BTSRecipesBEC::legacyK04);

        // SL-B32 Bastion Plate x64 = 16 SL-P18 crafts' metals plus 4 Shielding I. Replaces SL-P18.
        replace(
            "SL-B32",
            () -> assemble(
                SLBlocks.casing(BlockSLCasing.BASTION, 64),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.Infinity, 256 * INGOTS)),
                s(BTSRecipesSingularity.dense(Materials.Neutronium, 64), N1),
                s(BTSRecipesSingularity.dense(Materials.Neutronium, 32), N1),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 32L), N2),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 16L), N2),
                s(item(ItemList.MetaMaterial_Shielding1, 4), N3)),
            "SL-P18",
            BTSRecipesBEC::legacyP18);

        // SL-B33 Casimir Plate x16. Replaces SL-P19.
        replace(
            "SL-B33",
            () -> assemble(
                SLBlocks.casing(BlockSLCasing.CASIMIR, 16),
                600 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.SpaceTime, 16 * INGOTS), entangled(CondensateType.Space, 8 * INGOTS)),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 32L), N1),
                s(BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 64L), N2),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 2), N4),
                s(item(ItemList.MetaMaterial_Waveguide1, 2), N3)),
            "SL-P19",
            BTSRecipesBEC::legacyP19);

        // SL-B34 Harmonic Lining x64: a batch ALTERNATIVE (Q-B3). SL-P12 (x4, Assembler) stays: Kerr T1 is pre-BEC.
        BTSRecipeGuard.recipe(
            "SL-B34",
            () -> assemble(
                SLBlocks.casing2(BlockSLCasing2.LINING, 64),
                1_800 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.Infinity, 64 * INGOTS)),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 72L), N1),
                s(BTSRecipeGuard.req(OrePrefixes.foil, Materials.Infinity, 96L), N1),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 12L), N2),
                s(item(ItemList.MetaMaterial_Shielding1, 2), N3)));
    }

    // --------------------------------------------------- S1/S2: the Eye's bands, charts and transition cores

    private static void stellar() {
        // SL-B30 Stellar Mass Core (T1 -> T2). Replaces SL-E03; the gate stays 256,000 L of Raw Star Matter.
        replace(
            "SL-B30",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.stellarMassCore, 1),
                2_400 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(
                    ours(SLCondensates.entangledStarMatter, 256_000),
                    entangled(CondensateType.SpaceTime, 256 * INGOTS),
                    entangled(CondensateType.Infinity, 512 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.BEAM_BAND, 32), N2),
                s(item(ItemList.Field_Generator_UMV, 16), N3),
                s(circuit(Materials.UMV, 32), N2),
                s(BTSRecipesEye.denseSpaceTime(16), N1),
                s(item(ItemList.Emitter_UMV, 8), N3),
                s(BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 16), N4),
                s(item(ItemList.MetaMaterial_EnergyConduit1, 4), N3)),
            "SL-E03",
            BTSRecipesBEC::legacyE03);

        // SL-B40 Beam Band Casing x32 (Eye T2 band). Replaces SL-P15.
        replace(
            "SL-B40",
            () -> assemble(
                SLBlocks.casing(BlockSLCasing.BEAM_BAND, 32),
                3_600 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(entangled(CondensateType.SpaceTime, 128 * INGOTS), ours(SLCondensates.entangledStarMatter, 4_000)),
                s(tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 2), N4),
                s(SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 2), N3),
                s(item(ItemList.Emitter_UMV, 4), N3),
                s(BTSRecipesEye.denseSpaceTime(8), N1),
                s(item(ItemList.MetaMaterial_Waveguide1, 2), N3)),
            "SL-P15",
            BTSRecipesBEC::legacyP15);

        // SL-B41 Star Chart: Sirius A. Replaces SL-P23.
        replace(
            "SL-B41",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartSiriusA, 1),
                120 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(ours(SLCondensates.entangledStarMatter, 10_000), entangled(CondensateType.SpaceTime, INGOTS)),
                s(dataOrb(), N1),
                s(circuit(Materials.UMV, 1), N2),
                s(BTSRecipesEye.denseSpaceTime(1), N1),
                s(item(ItemList.MetaMaterial_SensorArray1, 1), N3)),
            "SL-P23",
            BTSRecipesBEC::legacyP23);

        // SL-B50 Shell-Burning Core (T2 -> T3). Replaces SL-E04; ESPM 128,000 L stays the gate.
        replace(
            "SL-B50",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.shellBurningCore, 1),
                3_600 * SECONDS,
                TierEU.RECIPE_UMV * 2,
                cond(
                    ours(SLCondensates.entangledExcitedPlasma, 128_000),
                    entangled(CondensateType.SpaceTime, 512 * INGOTS),
                    entangled(CondensateType.TranscendentMetal, 512 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 32), N2),
                s(item(ItemList.Field_Generator_UMV, 24), N3),
                s(circuit(Materials.UMV, 48), N2),
                s(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 16), N2),
                s(BTSRecipesEye.denseSpaceTime(24), N1),
                s(item(ItemList.Emitter_UMV, 12), N3),
                s(item(ItemList.MetaMaterial_ResonanceChamber2, 4), N5),
                s(item(ItemList.MetaMaterial_WaveFocus1, 4), N4)),
            "SL-E04",
            BTSRecipesBEC::legacyE04);

        // SL-B51 Jet Crescent Casing x32 (Eye T3 band). Replaces SL-P16; 4,000 L Raw Star Matter per block as before.
        replace(
            "SL-B51",
            () -> assemble(
                SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 32),
                4_800 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(
                    ours(SLCondensates.entangledStarMatter, 128_000),
                    entangled(CondensateType.SpaceTime, 64 * INGOTS),
                    ours(SLCondensates.entangledExcitedPlasma, 1_000)),
                s(tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 2), N4),
                s(SLBlocks.casing(BlockSLCasing.FIELD_COIL, 4), N2),
                s(BTSRecipesSingularity.dense(Materials.Infinity, 16), N1),
                s(item(ItemList.Emitter_UMV, 4), N3),
                s(item(ItemList.MetaMaterial_WaveFocus1, 2), N3)),
            "SL-P16",
            BTSRecipesBEC::legacyP16);

        // SL-B52 Star Chart: Arcturus. Replaces SL-P24.
        replace(
            "SL-B52",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartArcturus, 1),
                120 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(ours(SLCondensates.entangledExcitedPlasma, 1_000), entangled(CondensateType.SpaceTime, INGOTS)),
                s(dataOrb(), N1),
                s(circuit(Materials.UMV, 1), N2),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 4L), N1),
                s(item(ItemList.MetaMaterial_SensorArray1, 1), N3)),
            "SL-P24",
            BTSRecipesBEC::legacyP24);

        // SL-B60 Galactic Upgrade Core (T3 -> T4, Core Collapse). Replaces SL-E02; HSM 32,000 L stays the gate.
        replace(
            "SL-B60",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.galacticUpgradeCore, 1),
                4_800 * SECONDS,
                TierEU.RECIPE_UMV * 4,
                cond(
                    ours(SLCondensates.entangledHyperdense, 32_000),
                    entangled(CondensateType.SpaceTime, 1_024 * INGOTS),
                    entangled(CondensateType.TranscendentMetal, 1_024 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64), N2),
                s(item(ItemList.Field_Generator_UMV, 32), N3),
                s(circuit(Materials.UMV, 64), N2),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 32), N3),
                s(BTSRecipesEye.denseSpaceTime(32), N1),
                s(item(ItemList.Emitter_UMV, 16), N3),
                s(item(ItemList.MetaMaterial_ResonanceChamber2, 16), N5),
                s(item(ItemList.MetaMaterial_FieldManipulator1, 16), N4)),
            "SL-E02",
            BTSRecipesBEC::legacyE02);

        // SL-B61 Star Chart: Neutron Star. Replaces SL-P25.
        replace(
            "SL-B61",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartNeutronStar, 1),
                120 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(ours(SLCondensates.entangledHyperdense, 1_000)),
                s(dataOrb(), N1),
                s(circuit(Materials.UMV, 1), N2),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 1), N2),
                s(item(ItemList.MetaMaterial_SensorArray1, 1), N3)),
            "SL-P25",
            BTSRecipesBEC::legacyP25);
    }

    // ------------------------------------------------------------------ S3-S5: the remnant family (T4+)

    private static void remnant() {
        // SL-B62 Galactic Shell Plating x64: a batch ALTERNATIVE (Q-B2). SL-P17 (HSM, the T4 gate) stays; this one
        // needs White Dwarf Matter, so it only exists once the Eye runs T4 and never gates T4.
        BTSRecipeGuard.recipe(
            "SL-B62",
            () -> assemble(
                SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                4_800 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(ours(SLCondensates.entangledHyperdense, 500), ours(SLCondensates.entangledWhiteDwarf, 4 * INGOTS)),
                s(BTSRecipesSingularity.dense(Materials.Infinity, 32), N1),
                s(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TranscendentMetal, 8L), N2),
                s(BTSRecipeGuard.req(OrePrefixes.plate, Materials.CosmicNeutronium, 32L), N1),
                s(BTSRecipeGuard.req(OrePrefixes.ingot, Materials.TranscendentMetal, 64L), N1),
                s(item(ItemList.MetaMaterial_Shielding1, 2), N3)));

        // SL-B70 Subspace Cooling Core (T4 -> T5). Replaces SL-E05. White Dwarf nanites and metamaterials are T4
        // products: legal for the core that leaves T4. UMV parts x2, not UXV: RC-2's UXV Field Generator / Sensor /
        // Emitter need molten Universium and MHDCSM, which only Eye T10 makes with the TecTech Eye replaced (B-72).
        replace(
            "SL-B70",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.subspaceCoolingCore, 1),
                4_800 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(
                    ours(SLCondensates.entangledWhiteDwarf, 1_024 * INGOTS),
                    entangled(CondensateType.DimensionallyShiftedSuperfluid, 256_000),
                    entangled(CondensateType.SpaceTime, 1_024 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64), N2),
                s(item(ItemList.Field_Generator_UMV, 32), N4),
                s(circuit(Materials.UXV, 16), N3),
                s(SLBlocks.casing(BlockSLCasing.CASIMIR, 32), N3),
                s(BTSRecipesEye.denseSpaceTime(48), N1),
                s(item(ItemList.Sensor_UMV, 16), N4),
                s(item(ItemList.MetaMaterial_SensorArray2, 4), N6),
                s(item(ItemList.MetaMaterial_FieldManipulator2, 4), N6)),
            "SL-E05",
            BTSRecipesBEC::legacyE05);

        // SL-B71 Star Chart: Sirius B. Replaces SL-P35 (1,008 L: 7 ingots, the half-unit rule).
        replace(
            "SL-B71",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1),
                120 * SECONDS,
                TierEU.RECIPE_UMV,
                cond(ours(SLCondensates.entangledWhiteDwarf, 7 * INGOTS)),
                s(dataOrb(), N1),
                s(circuit(Materials.UXV, 1), N3),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 1), N2),
                s(item(ItemList.MetaMaterial_SensorArray2, 1), N6)),
            "SL-P35",
            BTSRecipesBEC::legacyP35);

        // SL-B80 Magnetar Seed Core (T5 -> T6, the Magnetar Merger). Replaces SL-E06; adds the merger's White Dwarf
        // Matter (D-49 answer 1, Q-B14). Black Dwarf nanites and Chamber III are T5-legal. UMV parts x2 (B-72).
        replace(
            "SL-B80",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.magnetarSeedCore, 1),
                6_000 * SECONDS,
                TierEU.RECIPE_UXV * 2,
                cond(
                    ours(SLCondensates.entangledBlackDwarf, 256 * INGOTS),
                    ours(SLCondensates.entangledAgent, 64_000),
                    ours(SLCondensates.entangledWhiteDwarf, 256 * INGOTS),
                    entangled(CondensateType.SpaceTime, 2_048 * INGOTS)),
                s(SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64), N2),
                s(item(ItemList.Field_Generator_UMV, 64), N4),
                s(circuit(Materials.UXV, 32), N3),
                s(SLBlocks.casing(BlockSLCasing.BASTION, 32), N2),
                s(BTSRecipesEye.denseSpaceTime(64), N1),
                s(item(ItemList.Emitter_UMV, 32), N4),
                s(item(ItemList.MetaMaterial_ResonanceChamber3, 4), N7),
                s(item(ItemList.MetaMaterial_Shielding2, 4), N6)),
            "SL-E06",
            BTSRecipesBEC::legacyE06);

        // SL-B81 Star Chart: SGR 1806-20. Replaces SL-P37.
        replace(
            "SL-B81",
            () -> assemble(
                BTSRecipeGuard.stack(SLItems.starChartMagnetar, 1),
                120 * SECONDS,
                TierEU.RECIPE_UXV,
                cond(ours(SLCondensates.entangledBlackDwarf, 7 * INGOTS), ours(SLCondensates.entangledAgent, 1_000)),
                s(dataOrb(), N1),
                s(circuit(Materials.UXV, 1), N3),
                s(BTSRecipesEye.denseSpaceTime(1), N1),
                s(item(ItemList.MetaMaterial_SensorArray3, 1), N7)),
            "SL-P37",
            BTSRecipesBEC::legacyP37);
    }

    // ================================================================================ BEC helpers

    /** One group of item inputs that all take the same nanite tier (a dense-plate fallback may be several). */
    private static final class Slot {

        final List<ItemStack> stacks = new ArrayList<>();
        final NaniteTier tier;

        Slot(NaniteTier tier) {
            this.tier = tier;
        }
    }

    /** {@code stacks} (an ItemStack or ItemStack[]) at nanite {@code tier}; stacks above 64 are split. */
    private static Slot s(Object stacks, NaniteTier tier) {
        final Slot slot = new Slot(tier);
        final ItemStack[] parts = stacks instanceof ItemStack[] ? (ItemStack[]) stacks
            : new ItemStack[] { (ItemStack) stacks };
        for (ItemStack part : parts) {
            BTSRecipeGuard.nn(part, "an item input");
            int left = part.stackSize;
            while (left > 0) {
                final int n = Math.min(64, left);
                slot.stacks.add(GTUtility.copyAmount(n, part));
                left -= n;
            }
        }
        return slot;
    }

    private static FluidStack[] cond(FluidStack... condensates) {
        return condensates;
    }

    /**
     * Registers one Condensate Assembler recipe: one nanite tier per item stack (GT's map throws otherwise), at most
     * 16 item stacks and 4 condensates, and GT must keep it (an empty addTo throws, so {@link #replace} sees a skip).
     */
    private static void assemble(ItemStack output, int ticks, long eut, FluidStack[] condensates, Slot... slots) {
        final List<ItemStack> items = new ArrayList<>();
        final List<NaniteTier> tiers = new ArrayList<>();
        for (Slot slot : slots) {
            for (ItemStack stack : slot.stacks) {
                items.add(stack);
                tiers.add(slot.tier);
            }
        }
        if (items.size() > 16) {
            throw new IllegalStateException(
                "the Condensate Assembler takes at most 16 item stacks, got " + items.size());
        }
        if (condensates.length > 4) {
            throw new IllegalStateException("the Condensate Assembler takes at most 4 condensates");
        }
        added(
            GTValues.RA.stdBuilder()
                .itemInputs(items.toArray(new ItemStack[0]))
                .fluidInputs(condensates)
                .itemOutputs(BTSRecipeGuard.nn(output, "the output"))
                .metadata(NANITE_TIERS, tiers.toArray(new NaniteTier[0]))
                .duration(ticks)
                .eut(eut),
            TecTechRecipeMaps.condensateAssemblingRecipes);
    }

    /** {@code builder.addTo(map)}, throwing if GT kept nothing (so the guard counts a skip). */
    private static void added(GTRecipeBuilder builder, RecipeMap<?> map) {
        final Collection<GTRecipe> recipes = builder.addTo(map);
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalStateException("GT rejected the recipe for " + map.unlocalizedName);
        }
    }

    /**
     * The safety net: the BEC recipe, or - if it is skipped - the old recipe it replaces, so the part stays
     * craftable (proposal §6.3).
     */
    private static void replace(String becId, Runnable bec, String legacyId, Runnable legacy) {
        final int before = BTSRecipeGuard.skippedCount();
        BTSRecipeGuard.recipe(becId, bec);
        if (BTSRecipeGuard.skippedCount() == before) return;
        fallbacks++;
        BeyondTheStars.LOG.warn(
            "[BTS] {} (Condensate Assembler) was skipped; its old recipe {} is registered instead, so the part stays "
                + "craftable.",
            becId,
            legacyId);
        BTSRecipeLedger.fallback(legacyId, becId + " (Condensate Assembler) was skipped");
        BTSRecipeGuard.recipe(legacyId, legacy);
    }

    /**
     * {@code litres} of one of our condensates: GT's fluid if GT took the source, checked against GT's half-unit rule,
     * and only if a generator recipe makes it (else the recipe is skipped and its fallback registers).
     */
    private static FluidStack ours(Fluid condensate, int litres) {
        final Fluid fluid = BTSRecipeGuard.nn(condensate, "an addon condensate");
        if (!MAKEABLE.contains(fluid)) {
            throw new IllegalStateException("no Condensate Generator recipe makes " + fluid.getName());
        }
        final int unit = SLCondensates.unitOf(fluid);
        if (unit > 0 && litres % (unit / 2) != 0) {
            throw new IllegalArgumentException(
                litres + " L of " + fluid.getName() + " is not a multiple of half its " + unit + " L unit");
        }
        final Fluid resolved = RESOLVED.get(fluid);
        return new FluidStack(resolved != null ? resolved : fluid, litres);
    }

    private static ItemStack item(ItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.get(count), "ItemList." + entry.name());
    }

    private static ItemStack circuit(Materials tier, int count) {
        return BTSRecipeGuard.req(OrePrefixes.circuit, tier, count);
    }

    private static ItemStack dataOrb() {
        return item(ItemList.Tool_DataOrb, 1);
    }

    // =================================================================== load-complete rewrites (SL-B01..B04)

    /**
     * BEC rewrites at load-complete (ruling 11a, bec-expansion-proposal §3): RC-2's own Condensate Assembler
     * recipes for TecTech's Reinforced Spatial Structure Casing (SL-B01) and Reinforced Temporal Structure Casing
     * (SL-B02) output 32 instead of 4. They replace the retired batch Assembler recipes SL-P27/P28, which I
     * found "a bit too cheap". Applied whatever {@code replaceTecTechEyeOfHarmony} /
     * {@code removeBlackHoleCompressorRecipe} say, because our Eye needs about 13,900 of these casings either way.
     * <p>
     * Only the output stack size changes, so GT's item-keyed lookup index needs no rebuild, and NEI builds its cache
     * later. Idempotent: a recipe already at 32 is left alone. A recipe matches when any of its outputs is the casing.
     * <p>
     * RC-2's BEC recipe is the ONLY recipe in the pack that makes these two casings (TecTech's research-station
     * recipe is gone in RC-2), and every Eye part needs them. So if it is missing (a pack that moved it), SL-B03/B04
     * register RC-2's own recipe at x32 (same inputs, nanite tiers, condensates, 40,000 s at UMV); if even that cannot
     * be built (an input absent), the old Assembler bodies SL-P27/P28 come back as a last resort, so no Eye part can
     * become uncraftable (question B-60). Called from {@link CommonProxy#loadComplete}, inside
     * {@link BTSRecipeRemovals#guard}.
     */
    public static void onLoadComplete() {
        if (rewrite("SL-B01", CustomItemList.EOH_Reinforced_Spatial_Casing, "Reinforced Spatial Structure Casing")
            == 0) {
            fallback(true);
        }
        if (rewrite("SL-B02", CustomItemList.EOH_Reinforced_Temporal_Casing, "Reinforced Temporal Structure Casing")
            == 0) {
            fallback(false);
        }
    }

    /** Rewrites every Condensate Assembler recipe making {@code casing}; returns how many there were (-1: no item). */
    private static int rewrite(String id, CustomItemList casing, String name) {
        final ItemStack want = casing.hasBeenSet() ? casing.get(1) : null;
        if (want == null || want.getItem() == null) {
            BeyondTheStars.LOG.error("[BTS] {}: TecTech's {} is not in this pack; nothing to rewrite.", id, name);
            return -1;
        }
        int hits = 0;
        for (GTRecipe recipe : TecTechRecipeMaps.condensateAssemblingRecipes.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null) continue;
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                final ItemStack out = recipe.mOutputs[i];
                if (out == null || !out.isItemEqual(want)) continue;
                hits++;
                final int was = out.stackSize;
                // The pack's recipe as it was, for /btsrecipes dump (before the edit; a no-op edit records nothing).
                final java.util.Map<String, Object> snapshot = was == CASINGS_PER_CRAFT ? null
                    : CommandBTSRecipes.snapshot(recipe);
                if (was == CASINGS_PER_CRAFT) {
                    BeyondTheStars.LOG.info(
                        "[BTS] {}: RC-2 {} (Condensate Assembler) already outputs {}; left alone.",
                        id,
                        name,
                        CASINGS_PER_CRAFT);
                    continue;
                }
                recipe.mOutputs[i] = GTUtility.copyAmount(CASINGS_PER_CRAFT, out);
                BTSRecipeLedger.changed(
                    id,
                    CommandBTSRecipes.BEC,
                    null,
                    "RC-2's own recipe for TecTech's " + name
                        + ": output x"
                        + CASINGS_PER_CRAFT
                        + " instead of x"
                        + was,
                    TecTechRecipeMaps.condensateAssemblingRecipes,
                    recipe,
                    snapshot);
                if (was == 4) {
                    BeyondTheStars.LOG.info(
                        "[BTS] {}: RC-2 {} (Condensate Assembler) now outputs {}, was 4.",
                        id,
                        name,
                        CASINGS_PER_CRAFT);
                } else {
                    BeyondTheStars.LOG.warn(
                        "[BTS] {}: RC-2 {} (Condensate Assembler) output was {}, not the expected 4; set to {} as ruled.",
                        id,
                        name,
                        was,
                        CASINGS_PER_CRAFT);
                }
            }
        }
        if (hits > 1) {
            BeyondTheStars.LOG.warn(
                "[BTS] {}: {} Condensate Assembler outputs are the {}; every one now outputs {}.",
                id,
                hits,
                name,
                CASINGS_PER_CRAFT);
        }
        if (hits == 0) {
            BeyondTheStars.LOG.error(
                "[BTS] {}: no Condensate Assembler recipe makes TecTech's {} in this pack (RC-2 has one in "
                    + "BECRecipes.addEyeOfHarmonyCasings), and nothing else makes it: registering Beyond the Stars' "
                    + "fallback so the Eye stays craftable.",
                id,
                name);
        }
        return hits;
    }

    /**
     * SL-B03 (spatial) / SL-B04 (temporal): RC-2's own Condensate Assembler recipe at x32. If an input is missing,
     * SL-P27 / SL-P28 (the retired Assembler recipes, x4) instead.
     */
    private static void fallback(boolean spatial) {
        final String id = spatial ? "SL-B03" : "SL-B04";
        final CustomItemList casing = spatial ? CustomItemList.EOH_Reinforced_Spatial_Casing
            : CustomItemList.EOH_Reinforced_Temporal_Casing;
        final int before = BTSRecipeGuard.skippedCount();
        BTSRecipeLedger
            .fallback(id, "no RC-2 Condensate Assembler recipe made the casing, so it could not be rewritten to x32");
        BTSRecipeGuard.recipe(
            id,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    tectech(CustomItemList.Godforge_SingularityShieldingCasing, 32),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.ProtoHalkonite, 32L),
                    BTSRecipeGuard.req(OrePrefixes.block, Materials.Hexanite, 8L),
                    BTSRecipeGuard.req(OrePrefixes.nanite, Materials.Neutronium, 48L),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Bedrockium, 2L),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.CosmicNeutronium, 2L),
                    BTSRecipeGuard.nn(GGMaterial.shirabon.get(OrePrefixes.plateSuperdense, 2), "superdense Shirabon"),
                    BTSRecipeGuard.req(OrePrefixes.plateSuperdense, Materials.Infinity, 2L),
                    BTSRecipeGuard.nn(ItemList.Machine_UV_SolarPanel.get(1L), "ItemList.Machine_UV_SolarPanel"),
                    spatial ? BTSRecipeGuard.nn(ItemList.Quantum_Chest_IV.get(1L), "ItemList.Quantum_Chest_IV")
                        : BTSRecipeGuard.nn(ItemList.AcceleratorUV.get(4L), "ItemList.AcceleratorUV"),
                    BTSRecipeGuard.nn(
                        GTModHandler.getModItem(Mods.GraviSuite.ID, "itemSimpleItem", 64, 3),
                        "GraviSuite itemSimpleItem:3"),
                    BTSRecipeGuard.nn(ItemList.EnergisedTesseract.get(1L), "ItemList.EnergisedTesseract"))
                .fluidInputs(
                    entangled(CondensateType.Neutronium, 512 * INGOTS),
                    entangled(CondensateType.CosmicNeutronium, 512 * INGOTS),
                    entangled(CondensateType.Bedrockium, 256 * INGOTS),
                    entangled(spatial ? CondensateType.Space : CondensateType.Time, 10 * INGOTS))
                .itemOutputs(tectech(casing, CASINGS_PER_CRAFT))
                // RC-2's nanites(4, 1, 1, 2, 1, 1, 4, 1, 1, 1, 1, 2), one per item input.
                .metadata(
                    NANITE_TIERS,
                    new NaniteTier[] { NaniteTier.Transcendent, NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Silver,
                        NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Transcendent, NaniteTier.Carbon,
                        NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Carbon, NaniteTier.Silver })
                .duration(40_000 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(TecTechRecipeMaps.condensateAssemblingRecipes));
        if (BTSRecipeGuard.skippedCount() == before) {
            BeyondTheStars.LOG
                .warn("[BTS] {}: fallback Condensate Assembler recipe registered (x{}).", id, CASINGS_PER_CRAFT);
            return;
        }
        // Last resort: the retired SL-P27/P28 bodies (programmed circuit 1/2 keeps them apart).
        final String last = spatial ? "SL-P27" : "SL-P28";
        BTSRecipeLedger.fallback(last, id + " could not be built either (last resort)");
        BTSRecipeGuard.recipe(
            last,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    tectech(CustomItemList.Godforge_SingularityShieldingCasing, 1),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Neutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.CosmicNeutronium, 4L),
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Infinity, 1L),
                    GTUtility.getIntegratedCircuit(spatial ? 1 : 2))
                .fluidInputs(BTSRecipeGuard.nn(Materials.SpaceTime.getMolten(576), "molten SpaceTime"))
                .itemOutputs(tectech(casing, 4))
                .duration(1_200)
                .eut(TierEU.RECIPE_UMV)
                .addTo(assemblerRecipes));
        BeyondTheStars.LOG
            .error("[BTS] {} could not be built; {} (Assembler, x4) registered as the last resort.", id, last);
    }

    // ============================================================================ legacy fallbacks
    // The retired recipes, moved here verbatim from BTSRecipesSingularity / BTSRecipesEye / BTSRecipesKerr. Each runs
    // ONLY if the BEC recipe that replaced it was skipped (see replace()).

    /** SL-P15 Beam Band Casing x16 (Assembler). */
    private static void legacyP15() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                items(
                    tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 1),
                    SLBlocks.casing(BlockSLCasing.QUADRUPOLE, 1),
                    item(ItemList.Emitter_UMV, 2),
                    BTSRecipesSingularity.dense(Materials.SpaceTime, 4)))
            .fluidInputs(molten(Materials.SpaceTime, 9_216))
            .itemOutputs(SLBlocks.casing(BlockSLCasing.BEAM_BAND, 16))
            .duration(1_800)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P16 Jet Crescent Casing x16 (Assembler; Raw Star Matter the gate). */
    private static void legacyP16() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                items(
                    tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 1),
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 2),
                    BTSRecipesSingularity.dense(Materials.Infinity, 8),
                    item(ItemList.Emitter_UMV, 2),
                    BTSRecipeGuard.req(OrePrefixes.ingot, Materials.SpaceTime, 32L)))
            .fluidInputs(rawStarMatter(64_000))
            .itemOutputs(SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 16))
            .duration(2_400)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P18 Bastion Plate x4 (Assembler). */
    private static void legacyP18() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                items(
                    BTSRecipesSingularity.dense(Materials.Neutronium, 6),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 2L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Infinity, 1L)))
            .fluidInputs(molten(Materials.Infinity, 2_304))
            .itemOutputs(SLBlocks.casing(BlockSLCasing.BASTION, 4))
            .duration(1_200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P19 Casimir Plate x4 (Assembler). */
    private static void legacyP19() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.Neutronium, 8L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 16L))
            .fluidInputs(molten(Materials.SpaceTime, 576))
            .itemOutputs(SLBlocks.casing(BlockSLCasing.CASIMIR, 4))
            .duration(1_200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P21 Stabiliser Pylon (assembly line, research Star Chart: Sol). */
    private static void legacyP21() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartSol, 1))
            .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UMV))
            .itemInputs(
                items(
                    tectech(CustomItemList.EOH_Reinforced_Spatial_Casing, 32),
                    tectech(CustomItemList.EOH_Infinite_Energy_Casing, 16),
                    item(ItemList.Field_Generator_UMV, 8),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Neutronium, 16L),
                    circuit(Materials.UMV, 8),
                    BTSRecipesSingularity.dense(Materials.Neutronium, 16),
                    tectech(CustomItemList.EOH_Reinforced_Temporal_Casing, 16)))
            .fluidInputs(molten(Materials.SpaceTime, 36_864), superCoolant(500_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.stabiliserPylon, 1))
            .duration(18_000)
            .eut(TierEU.RECIPE_UMV * 4)
            .addTo(AssemblyLine);
    }

    /** SL-P22 Star Chart: Sol (Assembler). */
    private static void legacyP22() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                dataOrb(),
                circuit(Materials.UMV, 1),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.Infinity, 4L))
            .fluidInputs(molten(Materials.SpaceTime, 144))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSol, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P23 Star Chart: Sirius A (Assembler, Raw Star Matter). */
    private static void legacyP23() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                items(dataOrb(), circuit(Materials.UMV, 1), BTSRecipesSingularity.dense(Materials.SpaceTime, 1)))
            .fluidInputs(rawStarMatter(10_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusA, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P24 Star Chart: Arcturus (Assembler, ESPM). */
    private static void legacyP24() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                dataOrb(),
                circuit(Materials.UMV, 1),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.TranscendentMetal, 4L))
            .fluidInputs(BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 1_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartArcturus, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P25 Star Chart: Neutron Star (Assembler, HSM). */
    private static void legacyP25() {
        GTValues.RA.stdBuilder()
            .itemInputs(dataOrb(), circuit(Materials.UMV, 1), SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 1))
            .fluidInputs(BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 1_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartNeutronStar, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P35 Star Chart: Sirius B (Assembler, WDM). */
    private static void legacyP35() {
        GTValues.RA.stdBuilder()
            .itemInputs(dataOrb(), circuit(Materials.UXV, 1), SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 1))
            .fluidInputs(molten(Materials.WhiteDwarfMatter, 1_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-P37 Star Chart: SGR 1806-20 (Assembler, BDM). */
    private static void legacyP37() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                items(dataOrb(), circuit(Materials.UXV, 1), BTSRecipesSingularity.dense(Materials.SpaceTime, 1)))
            .fluidInputs(molten(Materials.BlackDwarfMatter, 1_000))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.starChartMagnetar, 1))
            .duration(200)
            .eut(TierEU.RECIPE_UXV)
            .addTo(assemblerRecipes);
    }

    /** SL-E02 Galactic Upgrade Core (assembly line, research the Eye controller). */
    private static void legacyE02() {
        GTValues.RA.stdBuilder()
            .metadata(
                RESEARCH_ITEM,
                BTSRecipeGuard.nn(BTSMachines.eyeOfHarmony, "BTSMachines.eyeOfHarmony")
                    .copy())
            .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                    item(ItemList.Field_Generator_UMV, 32),
                    circuit(Materials.UMV, 64),
                    SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                    BTSRecipesEye.denseSpaceTime(32),
                    item(ItemList.Emitter_UMV, 16)))
            .fluidInputs(
                BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 32_000),
                molten(Materials.SpaceTime, 147_456),
                molten(Materials.TranscendentMetal, 147_456))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.galacticUpgradeCore, 1))
            .duration(48_000)
            .eut(TierEU.RECIPE_UMV * 4)
            .addTo(AssemblyLine);
    }

    /** SL-E03 Stellar Mass Core (assembly line, research the Stellar Ignition Charge). */
    private static void legacyE03() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 1))
            .metadata(SCANNING, new Scanning(4 * MINUTES, TierEU.RECIPE_UMV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.BEAM_BAND, 32),
                    item(ItemList.Field_Generator_UMV, 16),
                    circuit(Materials.UMV, 32),
                    BTSRecipesEye.denseSpaceTime(16),
                    item(ItemList.Emitter_UMV, 8),
                    BTSRecipeGuard.stack(SLItems.stellarIgnitionCharge, 16)))
            .fluidInputs(
                rawStarMatter(256_000),
                molten(Materials.SpaceTime, 36_864),
                molten(Materials.Infinity, 73_728))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.stellarMassCore, 1))
            .duration(24_000)
            .eut(TierEU.RECIPE_UMV)
            .addTo(AssemblyLine);
    }

    /** SL-E04 Shell-Burning Core (assembly line, research Star Chart: Arcturus). */
    private static void legacyE04() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartArcturus, 1))
            .metadata(SCANNING, new Scanning(6 * MINUTES, TierEU.RECIPE_UMV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.JET_CRESCENT, 32),
                    item(ItemList.Field_Generator_UMV, 24),
                    circuit(Materials.UMV, 48),
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 16),
                    BTSRecipesEye.denseSpaceTime(24),
                    item(ItemList.Emitter_UMV, 12)))
            .fluidInputs(
                BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 128_000),
                molten(Materials.SpaceTime, 73_728),
                molten(Materials.TranscendentMetal, 73_728))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.shellBurningCore, 1))
            .duration(36_000)
            .eut(TierEU.RECIPE_UMV * 2)
            .addTo(AssemblyLine);
    }

    /** SL-E05 Subspace Cooling Core (assembly line, research Star Chart: Sirius B). */
    private static void legacyE05() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartSiriusB, 1))
            .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UXV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                    item(ItemList.Field_Generator_UMV, 32),
                    circuit(Materials.UXV, 16),
                    SLBlocks.casing(BlockSLCasing.CASIMIR, 32),
                    BTSRecipesEye.denseSpaceTime(48),
                    item(ItemList.Sensor_UMV, 16)))
            .fluidInputs(
                molten(Materials.WhiteDwarfMatter, 147_456),
                BTSRecipeGuard.nn(
                    Materials.DimensionallyShiftedSuperfluid.getFluid(256_000L),
                    "Materials.DimensionallyShiftedSuperfluid fluid"),
                molten(Materials.SpaceTime, 147_456))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.subspaceCoolingCore, 1))
            .duration(48_000)
            .eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);
    }

    /** SL-E06 Magnetar Seed Core (assembly line, research Star Chart: SGR 1806-20). */
    private static void legacyE06() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, BTSRecipeGuard.stack(SLItems.starChartMagnetar, 1))
            .metadata(SCANNING, new Scanning(10 * MINUTES, TierEU.RECIPE_UXV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.GALACTIC_SHELL, 64),
                    item(ItemList.Field_Generator_UMV, 64),
                    circuit(Materials.UXV, 32),
                    SLBlocks.casing(BlockSLCasing.BASTION, 32),
                    BTSRecipesEye.denseSpaceTime(64),
                    item(ItemList.Emitter_UMV, 32)))
            .fluidInputs(
                molten(Materials.BlackDwarfMatter, 36_864),
                BTSRecipeGuard.fluid(SLFluids.antipolarizationAgent, 64_000),
                molten(Materials.SpaceTime, 294_912))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.magnetarSeedCore, 1))
            .duration(60_000)
            .eut(TierEU.RECIPE_UXV * 2)
            .addTo(AssemblyLine);
    }

    /** SL-E07 Stellar Field Sensor Hatch (Assembler). */
    private static void legacyE07() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                item(ItemList.Hull_UMV, 1),
                item(ItemList.Sensor_UMV, 2),
                circuit(Materials.UMV, 2),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.SpaceTime, 4L))
            .fluidInputs(molten(Materials.SpaceTime, 576))
            .itemOutputs(
                BTSRecipeGuard.nn(BTSMachines.stellarFieldSensor, "BTSMachines.stellarFieldSensor")
                    .copy())
            .duration(400)
            .eut(TierEU.RECIPE_UMV)
            .addTo(assemblerRecipes);
    }

    /** SL-K04 Kerr Density Upgrade (assembly line, research a Bastion casing). */
    private static void legacyK04() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, SLBlocks.casing(BlockSLCasing.BASTION, 1))
            .metadata(SCANNING, new Scanning(8 * MINUTES, TierEU.RECIPE_UMV))
            .itemInputs(
                items(
                    SLBlocks.casing(BlockSLCasing.BASTION, 64),
                    SLBlocks.casing(BlockSLCasing.BASTION, 64),
                    BTSRecipesSingularity.dense(Materials.Infinity, 64),
                    item(ItemList.Field_Generator_UMV, 32),
                    circuit(Materials.UMV, 32),
                    SLBlocks.casing(BlockSLCasing.FIELD_COIL, 32),
                    item(ItemList.Black_Hole_Opener, (int) BTSRecipesKerr.DENSITY_UPGRADE_SEEDS),
                    SLBlocks.casing(BlockSLCasing.INJECTOR_MK3, 16)))
            .fluidInputs(
                rawStarMatter(4_000_000),
                molten(Materials.SpaceTime, 589_824),
                BTSRecipeGuard.nn(Materials.ExcitedDTEC.getFluid(128_000L), "Materials.ExcitedDTEC fluid"))
            .itemOutputs(BTSRecipeGuard.stack(SLItems.kerrDensityUpgrade, 1))
            .duration(36_000)
            .eut(TierEU.RECIPE_UMV * 4)
            .addTo(AssemblyLine);
    }

    // ============================================================================ shared helpers

    /** Flattens ItemStacks and ItemStack[]s into one input array (at most 16 slots, the assembly line's cap). */
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
        if (out.size() > 16) {
            throw new IllegalStateException("at most 16 item inputs, got " + out.size());
        }
        return out.toArray(new ItemStack[0]);
    }

    private static FluidStack molten(Materials material, int litres) {
        return BTSRecipeGuard.nn(material.getMolten(litres), "molten " + material);
    }

    private static FluidStack superCoolant(int litres) {
        return BTSRecipeGuard.nn(Materials.SuperCoolant.getFluid(litres), "Materials.SuperCoolant fluid");
    }

    /** Raw Stellar Plasma Mixture = the pack's Raw Star Matter (Q-1). */
    private static FluidStack rawStarMatter(int litres) {
        return BTSRecipeGuard.nn(Materials.RawStarMatter.getFluid(litres), "Materials.RawStarMatter fluid");
    }

    private static ItemStack tectech(CustomItemList entry, int count) {
        return BTSRecipeGuard.nn(entry.hasBeenSet() ? entry.get(count) : null, "CustomItemList." + entry.name());
    }

    private static FluidStack entangled(CondensateType type, int amount) {
        return BTSRecipeGuard.nn(type.getEntangled(amount), "entangled " + type.name());
    }
}
