package com.nateyburr.beyondthestars;

import static goodgenerator.api.recipe.GoodGeneratorRecipeMaps.neutronActivatorRecipes;
import static goodgenerator.api.recipe.GoodGeneratorRecipeMaps.preciseAssemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.formingPressRecipes;
import static gregtech.api.recipe.RecipeMaps.isotopeDecay;
import static gregtech.api.recipe.RecipeMaps.latheRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.CHEMPLANT_CASING_TIER;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gregtech.api.util.GTRecipeConstants.DECAY_TYPE;
import static gregtech.api.util.GTRecipeConstants.HALF_LIFE;
import static gregtech.api.util.GTRecipeConstants.NKE_RANGE;
import static gregtech.api.util.GTRecipeConstants.PRECISE_ASSEMBLER_CASING_TIER;
import static gregtech.api.util.GTRecipeConstants.QFT_CATALYST;
import static gregtech.api.util.GTRecipeConstants.QFT_FOCUS_TIER;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;
import static gregtech.api.util.GTRecipeConstants.UniversalChemical;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.chemicalDehydratorRecipes;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.chemicalPlantRecipes;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.flotationCellRecipes;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.millingRecipes;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.quantumForceTransformerRecipes;
import static gtPlusPlus.api.recipe.GTPPRecipeMaps.vacuumFurnaceRecipes;
import static gtnhlanth.api.recipe.LanthanidesRecipeMaps.digesterRecipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.fluids.BTSFluidsProcess;
import com.nateyburr.beyondthestars.fluids.BTSWaterGrade;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.items.BTSItemsFuel;
import com.nateyburr.beyondthestars.machines.MTECascadeHall;
import com.nateyburr.beyondthestars.machines.MTEFuelFabricationHall;
import com.nateyburr.beyondthestars.machines.MTEPurexCanyon;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import goodgenerator.util.MyRecipeAdder;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.nuclear.MaterialsFluorides;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/**
 * D-26 THE FUEL LINE - ore to bundle, and the PUREX head-end that closes the loop
 * (docs/production-lines.md S1). <b>Owned exclusively by Agent A (Fuel).</b>
 * <p>
 * <b>The eighteen retired recipes are gone.</b> S8.1's table listed the whole dust -> pellet ->
 * rod -> assembly chain across four grades plus the eight depleted-fuel centrifuge recipes; every
 * one of them has been deleted from this file and replaced by the twenty-three-step chain below.
 * The one recipe that was NOT retired - nuclear waste water -> distilled water + tantalum, the
 * D-09 profit anchor - stands unchanged at the bottom, and for the first time in the mod's life
 * it now has a producer feeding it (R-02h and R-03).
 * <p>
 * <b>The two ratios that may not move, and did not.</b> F-17 takes 4 pellets per rod and F-18
 * takes 3 rods per assembly - exactly the ratios of retired recipes 2 and 3. That is what makes
 * every pellet, rod and assembly already in a player's chest worth precisely what it was worth
 * yesterday: the redesign changes the ROUTE, never the value (S8.2).
 * <p>
 * <b>The water grade is the ceramic-grade wash on every oxide this line makes.</b> There are
 * three oxide-making steps - F-12 (UO2 for the uranium grade), F-20 (depleted UO2 for MOX) and
 * F-21a (thoria for the thorium grade) - and each carries the same nine-rung ladder, one recipe
 * per rung. The naquadah grade needs no rung of its own: its pellet is built out of F-12's
 * sintered pellet, so it inherits one. Every output count on all three ladders is derived from
 * {@link BTSWaterGrade#yieldMilli(int)} through {@link #gradedOutput(int, int)} rather than
 * typed, so this file, the NEI pages and the reactor's readout come off one table and cannot
 * drift. {@link #gradedOutput(int, int)} also carries the balance-guard arithmetic per fuel
 * grade, and why F-20's and F-21a's rungs had to exist for that guard to hold.
 * <p>
 * <b>A player holding nothing but distilled water can still finish the line.</b> Distilled is a
 * full rung of F-12, not an omission: it yields 2 powder against Grade 1's 4, so the whole chain
 * runs end to end at half rate and about 32 Uraninite per fuel rod. Nothing downstream of F-12
 * cares which rung produced the powder, so there is no second gate, no purified-water-only recipe
 * and no dead end. The Purification Plant buys throughput; it is never a prerequisite.
 * <p>
 * <b>Seven deviations from the S1 tables, and every one of them is forced.</b> The design doc's
 * recon established which recipe maps an addon may register into, but not the SHAPE of those maps:
 * {@code maxIO(itemIn, itemOut, fluidIn, fluidOut)} was read out of each map's static initialiser
 * for this file, and five of the tables cannot be expressed as drawn. The other two are chemistry
 * and a recipe collision:
 * <ol>
 * <li><b>F-01 milling</b> - {@code millingRecipes} is {@code maxIO(3, 1, 0, 0)}: no fluid slots at
 * all and a single item output. The wash water and the gangue byproduct move off this step; the
 * IsaMill grinds and does nothing else, which is what an IsaMill is. One of the three item slots
 * is always the MILLING BALL, which is a recipe input rather than a hatch-only item -
 * {@link #millingBall()} proves it against MTEIsaMill's bytecode - so F-19 and F-21b, which spend
 * the other two slots on reagents, carry no programmed circuit.</li>
 * <li><b>F-02 flotation</b> - {@code flotationCellRecipes} is {@code maxIO(6, 0, 1, 1)}: ZERO item
 * outputs. The cell emits a froth FLUID, exactly as GT++'s own sphalerite line does, and a new
 * <b>F-02b</b> thickening step on the Dehydrator recovers the concentrate, the tailings and the
 * gangue. A real flotation circuit has a thickener after the cells for precisely this reason.
 * The cell also gates on the OREDICT: an input must carry a name starting with "milled" or the
 * recipe is never looked at, which is what {@link #registerOreDictNames()} is for.</li>
 * <li><b>F-03 leaching</b> - {@code digesterRecipes} is {@code maxIO(3, 1, 1, 1)}: one fluid input,
 * so the ferric-chloride oxidant cannot ride alongside the sulfuric acid. It is replaced by dust
 * Pyrolusite (MnO2), which is the SOLID oxidant the real Rossing-type acid-leach flowsheet
 * uses - a better answer than the one the table drew, not a worse one.</li>
 * <li><b>F-09/F-10 fluorides</b> - GT++ models {@code URANIUM_TETRAFLUORIDE} and
 * {@code URANIUM_HEXAFLUORIDE} as FLUIDS and its own LFTR loader calls
 * {@code getFluidStack} on both. We produce the fluid forms, because the entire point of
 * collision C2 is that a standing LFTR line and this line meet in the same tank.</li>
 * <li><b>F-15 grinding</b> - {@code latheRecipes} is {@code maxIO(1, 2, 0, 0)}: one item input,
 * so a programmed circuit cannot fit and the grade CANNOT be selected by circuit the way the
 * table assumed. This is exactly why the sintered pellet is grade-tagged as an item.</li>
 * <li><b>F-11c reduction</b> - calciothermic reduction of a fluoride gives calcium FLUORIDE, not
 * calcium chloride. Fluorite is the output; there is no chlorine anywhere in this chain.</li>
 * <li><b>F-16 cladding</b> - {@code extruderRecipes} is {@code maxIO(2, 1, 0, 0)} and a die
 * recipe spends both slots, so an addon recipe there is indistinguishable from GT's own
 * auto-generated ingot-plus-die recipe and one of the two would silently become unreachable.
 * The tube is rolled on the Assembler behind a circuit instead.</li>
 * </ol>
 * Each is called out again at the recipe itself.
 */
public final class BTSRecipesFuel {

    /** Circuit that selects the uranium grade wherever one recipe shape serves all four. */
    private static final int GRADE_URANIUM = 1;
    /** Circuit that selects the MOX grade. */
    private static final int GRADE_MOX = 2;
    /** Circuit that selects the thorium-bonded grade. */
    private static final int GRADE_THORIUM = 3;
    /** Circuit that selects the naquadah-bonded grade. */
    private static final int GRADE_NAQ = 4;

    /** PUREX Canyon mode: the full cooled head-end, R-02. */
    private static final int PUREX_COOLED = 1;
    /** PUREX Canyon mode: the uncooled rush, R-02h. One step, no plutonium. */
    private static final int PUREX_RUSH = 2;
    /** PUREX Canyon mode: partition, R-03. */
    private static final int PUREX_PARTITION = 3;

    private BTSRecipesFuel() {}

    /**
     * F-02's ENTRY TICKET. The Froth Flotation Cell will not look at a recipe unless one of its
     * item inputs carries an OreDictionary name beginning with "milled", so the milled
     * intermediate has to have one - and until now it had none, which is why F-02 could never
     * have run on either flotation implementation.
     * <p>
     * <b>The gate, from the bytecode.</b> {@code MTEFrothFlotationCell$1.validateRecipe} asks
     * {@code getMilledStackName(recipe)} FIRST and returns
     * {@code CheckRecipeResultRegistry.NO_RECIPE} the moment it answers null.
     * {@code getMilledStackName} walks {@code recipe.mInputs}, asks
     * {@code OreDictionary.getOreIDs} of each stack, and returns the first ore name that
     * {@code startsWith(OrePrefixes.milled.toString())} - which is the String "milled", because
     * {@code OrePrefixes.toString()} returns {@code getName()} and the {@code milled} prefix is
     * built from the literal "milled". The name it finds is then LOCKED into the machine, so it
     * has to be a stable, canonical name rather than an ad-hoc one.
     * <p>
     * <b>Why the oredict name and not a different machine.</b> The alternative was to move F-02
     * onto a map with no such gate, which would have deleted the flotation cell from the line
     * altogether - the concentrate/tailings/gangue split at F-02b only makes sense downstream of a
     * real cell. Registering the name keeps the designed machine and costs one line.
     * <p>
     * <b>The name is built by GT, not typed.</b> {@code GTOreDictUnificator.registerOre(prefix,
     * material, stack)} hands {@code OrePrefixes.milled.get(Materials.Uraninite)} to
     * {@code ItemData.toString()}, which is {@code prefix.getName() + material.mName} - so this is
     * GT's own canonical spelling of the name and cannot drift from it.
     * <p>
     * <b>Collision check.</b> Nothing in the pack registers a milled Uraninite. GT ships no milled
     * items at all - there is no {@code ProcessingMilled} ore-processing handler, so registering
     * this name auto-generates no GT recipes - and the only producer of {@code milled*} names is
     * GT++'s {@code BaseItemMilledOre}, whose generated list is Sphalerite, Chalcopyrite, Nickel,
     * Platinum, Pentlandite, Redstone, Spessartine, Grossular, Almandine, Pyrope, Monazite and
     * Netherrack. Uraninite is not among them. {@code registerOre} is idempotent besides: it
     * returns false without duplicating if an equal stack is already filed under the name.
     * <p>
     * This runs FIRST, before any recipe is built, because {@code GTRecipeBuilder.itemInputs}
     * unifies its stacks through {@code GTOreDictUnificator.setStackArray} at build time - the
     * name has to exist before the recipes that carry the item are compiled.
     */
    private static void registerOreDictNames() {
        GTOreDictUnificator
            .registerOre(OrePrefixes.milled, Materials.Uraninite, new ItemStack(BTSItemsFuel.milledUraninite, 1));
    }

    public static void register() {
        registerOreDictNames();
        frontEnd();
        conversionAndEnrichment();
        reconversion();
        ceramics();
        fabrication();
        moxGrade();
        thoriumGrade();
        naquadahGrade();
        reprocessing();
        crudeReprocessing();
        byproductCleanup();
        keptFromBefore();
    }

    // =============================================================== F-01 .. F-08, ore to yellowcake

    /**
     * THE MILLING BALL, as a recipe INPUT at stack size ZERO. Every recipe this file puts on
     * {@code millingRecipes} carries one, and a milling recipe without one is dead on arrival.
     * <p>
     * <b>The ball is not "taken from the hatch instead".</b> {@code MTEIsaMill$1.validateRecipe}
     * walks {@code recipe.mInputs} looking for a stack that {@code MTEIsaMill.isMillingBall}
     * accepts, and when it finds none it returns {@code CheckRecipeResultRegistry.NO_RECIPE} -
     * whatever is sitting in the mill's {@code MTEHatchMillingBalls}. The ball in the RECIPE is
     * what names the medium; {@code searchForMillingBall} then matches that name against the
     * hatch's stacks with {@code GTUtility.areStacksEqual(.., true)} and the hatch supplies the
     * physical ball. The recipe says which ball; the hatch pays for it.
     * <p>
     * <b>Stack size 0 is GT's "required but not consumed".</b> Nothing is ever pulled from an
     * input bus for this: {@code MTEIsaMill$1.onRecipeStart} calls {@code damageMillingBall},
     * which spends one point of the HATCH ball's durability and only decrements its stack size
     * when the ball finally wears out. This is verbatim the shape of GT++'s own IsaMill recipes -
     * {@code BaseItemMilledOre}'s constructor passes
     * {@code GregtechItemList.Milling_Ball_Alumina.get(0)}
     * as a plain second item input.
     * <p>
     * <b>Alumina only, on all four of this file's milling recipes.</b> GT++ registers a soapstone
     * variant beside every alumina one and keys the pair apart with a programmed circuit. This
     * file cannot: {@code millingRecipes} is {@code maxIO(3, 1, 0, 0)} and F-19 and F-21b already
     * spend two of those three slots on real reagents, so the ball takes the third and no circuit
     * fits. A two-tier ball ladder on two recipes and a one-tier one on the other two would be
     * worse than a single uniform rule, so the rule is the alumina ball everywhere - the better of
     * GT++'s two media, and the right one for a nuclear-grade grind.
     */
    private static ItemStack millingBall() {
        return BTSRecipeGuard.nn(GregtechItemList.Milling_Ball_Alumina.get(0), "GregtechItemList.Milling_Ball_Alumina");
    }

    private static void frontEnd() {
        // F-01 COMMINUTION. Wet attrition milling in the Ore Milling Plant (IsaMill).
        //
        // DEVIATION, forced: millingRecipes is maxIO(3, 1, 0, 0) - no fluid slots and one item
        // output. The spec's wash water and gangue byproduct therefore cannot live here. The mill
        // grinds; the gangue is separated downstream at F-02b, where a real circuit separates it.
        // The third item slot is the MILLING BALL, which IS a recipe input - millingBall() is
        // where that is written out, and where the two lines this comment used to get exactly
        // backwards are corrected against MTEIsaMill's own bytecode.
        BTSRecipeGuard.recipe(
            "F-01",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uraninite, 16L), millingBall())
                .circuit(1)
                .itemOutputs(new ItemStack(BTSItemsFuel.milledUraninite, 16))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(millingRecipes));

        // Pitchblende is the richer ore: twelve dust buys the same sixteen milled.
        BTSRecipeGuard.recipe(
            "milledUraninite",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Pitchblende, 12L), millingBall())
                .circuit(2)
                .itemOutputs(new ItemStack(BTSItemsFuel.milledUraninite, 16))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(millingRecipes));

        // F-02a FROTHER PREPARATION. Pine oil collector plus soda ash pH modifier - the real
        // reagent suite of a uranium flotation circuit, made up one charge at a time.
        BTSRecipeGuard.recipe(
            "F-02a",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.SodaAsh, 2L))
                .circuit(4)
                .fluidInputs(BTSRecipeGuard.fluid(GTPPFluids.PineOil, 1_000))
                .itemOutputs(new ItemStack(BTSItemsFuel.frothCell, 1))
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(mixerRecipes));

        // F-02 FROTH FLOTATION.
        //
        // DEVIATION, forced: flotationCellRecipes is maxIO(6, 0, 1, 1) - SIX item inputs and ZERO
        // item outputs. The cell can only ever emit a fluid, which is exactly how GT++'s own
        // sphalerite and chalcopyrite lines run. The concentrate comes back as a solid at F-02b.
        //
        // milledUraninite is what gets this recipe past the cell's gate: validateRecipe rejects
        // any recipe with no "milled*" OreDictionary name among its inputs. registerOreDictNames()
        // gives it one and explains the whole mechanism.
        BTSRecipeGuard.recipe(
            "F-02",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.milledUraninite, 16), new ItemStack(BTSItemsFuel.frothCell, 1))
                .fluidInputs(Materials.Water.getFluid(8_000))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsProcess.uraniniteFlotationFroth, 8_000))
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(flotationCellRecipes));

        // F-02b THICKENING AND FILTRATION. The froth is dewatered back into solids, and this is
        // where the barren rock finally leaves the line: concentrate to the leach, tailings and
        // gangue to the cleanup recipes below, and most of the process water straight back.
        BTSRecipeGuard.recipe(
            "F-02b",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.uraniniteFlotationFroth, 8_000))
                .itemOutputs(
                    new ItemStack(BTSItemsFuel.uraniniteConcentrate, 12),
                    new ItemStack(BTSItemsFuel.flotationTailings, 4),
                    new ItemStack(BTSItemsFuel.gangueDust, 2))
                .fluidOutputs(Materials.Water.getFluid(3_000))
                .duration(25 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(chemicalDehydratorRecipes));

        // F-03 AGITATED SULFURIC LEACH, in the Digester.
        //
        // DEVIATION, forced and for the better: digesterRecipes is maxIO(3, 1, 1, 1) - ONE fluid
        // input, so the spec's ferric-chloride oxidant cannot ride in beside the acid. Pyrolusite
        // (MnO2) takes its place, and MnO2 is the solid oxidant the real Rossing-type flowsheet
        // actually charges to the leach tanks. COIL_HEAT 1000 K is below the cheapest coil, so the
        // heat requirement is the Digester's own and no coil upgrade gates this step.
        BTSRecipeGuard.recipe(
            "F-03",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSItemsFuel.uraniniteConcentrate, 12),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Pyrolusite, 2L))
                .circuit(1)
                .fluidInputs(Materials.SulfuricAcid.getFluid(6_000))
                .itemOutputs(new ItemStack(BTSItemsFuel.leachResidue, 3))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsProcess.pregnantLeachSolution, 8_000))
                .metadata(COIL_HEAT, 1_000)
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(digesterRecipes));

        // F-04 CLARIFICATION AND ION EXCHANGE. The resin bed is a tool, not a reagent: it comes
        // back nine times out of ten, so a line needs a trickle of new resin and not a stream.
        BTSRecipeGuard.recipe(
            "F-04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.ionExchangeResin, 1))
                .circuit(1)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.pregnantLeachSolution, 8_000))
                .itemOutputs(new ItemStack(BTSItemsFuel.ionExchangeResin, 1))
                .outputChances(9_000)
                .fluidOutputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.clarifiedUranylLiquor, 6_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.raffinate, 2_000))
                .metadata(CHEMPLANT_CASING_TIER, 2)
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(chemicalPlantRecipes));

        // The resin itself: sulfonated polystyrene beads aminated with ammonia. Polystyrene is
        // used in its molten form because that is the form GT generates for it.
        BTSRecipeGuard.recipe(
            "ionExchangeResin",
            () -> GTValues.RA.stdBuilder()
                .circuit(6)
                .fluidInputs(Materials.Polystyrene.getMolten(2 * INGOTS), Materials.Ammonia.getGas(1_000))
                .itemOutputs(new ItemStack(BTSItemsFuel.ionExchangeResin, 4))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(UniversalChemical));

        // F-04b ACID RECOVERY. The barren aqueous phase is not waste; it is dilute acid and a
        // little gypsum-forming calcium.
        BTSRecipeGuard.recipe(
            "F-04b",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.raffinate, 2_000))
                .itemOutputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Calcite, 1L))
                .fluidOutputs(Materials.DilutedSulfuricAcid.getFluid(1_200))
                .duration(15 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(distillationTowerRecipes));

        // F-05a SOLVENT SYNTHESIS. Tributyl phosphate in a kerosene diluent - the extractant of
        // every uranium SX plant and of PUREX itself, which is why R-03 buys from this same tank.
        BTSRecipeGuard.recipe(
            "F-05a",
            () -> GTValues.RA.stdBuilder()
                .circuit(3)
                .fluidInputs(
                    BTSRecipeGuard.fluid(GTPPFluids.Butanol, 3_000),
                    Materials.PhosphoricAcid.getFluid(1_000),
                    BTSRecipeGuard.fluid(GTPPFluids.Kerosene, 4_000))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsProcess.tbpSolvent, 8_000), Materials.Water.getFluid(1_000))
                .metadata(CHEMPLANT_CASING_TIER, 2)
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(chemicalPlantRecipes));

        // F-05 SOLVENT EXTRACTION. Uranium crosses into the organic phase; everything else stays.
        BTSRecipeGuard.recipe(
            "F-05",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.clarifiedUranylLiquor, 6_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.tbpSolvent, 2_000))
                .fluidOutputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.loadedOrganic, 4_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.raffinate, 2_000))
                .metadata(CHEMPLANT_CASING_TIER, 3)
                .duration(25 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(chemicalPlantRecipes));

        // F-06 STRIPPING AND SOLVENT RECOVERY. 1,800 of the 2,000 L of solvent come back - the
        // counter-current recycle, written as a flat 90 % rather than a chanced fluid output so a
        // player can size the make-up line by reading the NEI page instead of by watching it run.
        BTSRecipeGuard.recipe(
            "F-06",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.SodaAsh, 4L))
                .circuit(2)
                .fluidInputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.loadedOrganic, 4_000),
                    Materials.Water.getFluid(2_000))
                .fluidOutputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.uranylStripLiquor, 3_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.tbpSolvent, 1_800))
                .metadata(CHEMPLANT_CASING_TIER, 3)
                .duration(25 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(chemicalPlantRecipes));

        // F-07 ADU PRECIPITATION. Ammonia drops (NH4)2U2O7 out of solution and leaves an ammonium
        // sulfate liquor behind, which is a saleable co-product of every real ADU plant.
        BTSRecipeGuard.recipe(
            "F-07",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.uranylStripLiquor, 3_000),
                    Materials.Ammonia.getGas(1_000))
                .itemOutputs(new ItemStack(BTSItemsFuel.ammoniumDiuranate, 6))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsProcess.ammoniumSulfateLiquor, 1_000))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(UniversalChemical));

        // F-07b AMMONIUM SULFATE RECOVERY. Without this the co-product has a producer and no
        // consumer at all: its output hatch fills, and a full output hatch STALLS the reactor
        // running F-07. Cracking the liquor sends 300 L of the 1,000 L of ammonia straight back
        // to F-07 and turns the rest into dilute acid, so a standing line needs a 200 L ammonia
        // make-up per cycle rather than either a full feed or a free lunch.
        BTSRecipeGuard.recipe(
            "F-07b",
            () -> GTValues.RA.stdBuilder()
                .circuit(2)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.ammoniumSulfateLiquor, 1_000))
                .fluidOutputs(Materials.Ammonia.getGas(300), Materials.DilutedSulfuricAcid.getFluid(400))
                .duration(15 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(distillationTowerRecipes));

        // F-08 CALCINING TO YELLOWCAKE, 800 C. Ammonia comes back off the kiln and returns to
        // F-07. Only one fluid output slot exists on this map, so the steam is not modelled.
        BTSRecipeGuard.recipe(
            "F-08",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.ammoniumDiuranate, 6))
                .circuit(1)
                .itemOutputs(new ItemStack(BTSItemsFuel.yellowcake, 4))
                .fluidOutputs(Materials.Ammonia.getGas(500))
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(chemicalDehydratorRecipes));
    }

    // ======================================================= F-09 .. F-11c, conversion and enrichment

    private static void conversionAndEnrichment() {
        // F-09 HYDROFLUORINATION - "green salt". THE COLLISION C2 JOIN POINT.
        //
        // The output is GT++'s OWN MaterialsFluorides.URANIUM_TETRAFLUORIDE, not a private copy,
        // and it is produced as a FLUID because that is the form GT++ generates and consumes in its
        // LFTR chain. A player with a standing molten-salt line can feed it straight in here.
        // The HF is recovered downstream at F-12 and comes back, which is the real IDR loop.
        BTSRecipeGuard.recipe(
            "F-09",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.yellowcake, 4))
                .circuit(1)
                .fluidInputs(Materials.HydrofluoricAcid.getFluid(4_000), Materials.Hydrogen.getGas(1_000))
                .fluidOutputs(
                    MaterialsFluorides.URANIUM_TETRAFLUORIDE.getFluidStack(6_000),
                    Materials.Steam.getGas(2_000))
                .metadata(CHEMPLANT_CASING_TIER, 3)
                .duration(25 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(chemicalPlantRecipes));

        // F-10 FLUORINATION TO UF6. Again GT++'s own material, again as its own fluid.
        BTSRecipeGuard.recipe(
            "F-10",
            () -> GTValues.RA.stdBuilder()
                .circuit(2)
                .fluidInputs(
                    MaterialsFluorides.URANIUM_TETRAFLUORIDE.getFluidStack(6_000),
                    Materials.Fluorine.getGas(2_000))
                .fluidOutputs(MaterialsFluorides.URANIUM_HEXAFLUORIDE.getFluidStack(6_000))
                .metadata(CHEMPLANT_CASING_TIER, 3)
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(chemicalPlantRecipes));

        // F-11 GAS CENTRIFUGE CASCADE. One sixth of the feed leaves enriched; five sixths leave as
        // tails, and the tails are a PRODUCT - F-20 makes the MOX diluent out of them and F-11c
        // makes the shielding metal Agent B's M-03b spends on casings.
        BTSRecipeGuard.recipe(
            "F-11",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(MaterialsFluorides.URANIUM_HEXAFLUORIDE.getFluidStack(6_000))
                .fluidOutputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.enrichedUF6, 1_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.depletedUF6, 5_000))
                .duration(1 * MINUTES)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTECascadeHall.RECIPE_MAP));

        // F-11b THE HEU BRANCH. Re-feeding the enriched cut costs four fifths of it to make one
        // fifth of a far hotter assay - which is why only the naquadah grade is allowed to want it.
        BTSRecipeGuard.recipe(
            "F-11b",
            () -> GTValues.RA.stdBuilder()
                .circuit(2)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.enrichedUF6, 1_000))
                .fluidOutputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.highlyEnrichedUF6, 200),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.depletedUF6, 800))
                .duration(90 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(MTECascadeHall.RECIPE_MAP));

        // F-11c DEPLETED-URANIUM METAL, by calciothermic reduction in the Neutron Activator.
        //
        // DEVIATION: the spec's byproduct was calcium CHLORIDE, which cannot be right - there is
        // no chlorine anywhere in this chain. Reducing a fluoride with calcium gives calcium
        // FLUORIDE, and the eight calcium in balance the eight fluoride out exactly.
        //
        // THE CHEMISTRY WAS RIGHT; THE ITEM WAS NOT. This line is what killed 0.24.0's boot.
        // dust Materials.Fluorite compiles, because the field exists - but it answers null at
        // runtime and GT's builder throws "null in argument" on it. Materials.Fluorite is a
        // VESTIGIAL GT entry: it is absent from MaterialsIDMap, so its mMetaItemSubID stays -1,
        // so fillGeneratedMaterialsMap skips it, so MetaGeneratedItem01's generation loop - which
        // walks sGeneratedMaterials by index rather than walking materials - never reaches it.
        // loadFluorite() calling addDustItems() makes no difference: that flag is only read
        // downstream of the null check the material never survives. NO prefix resolves for it,
        // so a gem or a dustSmall would have failed in exactly the same way. GT itself never
        // references Materials.Fluorite in a single recipe.
        //
        // The replacement is bartworks' Fluorspar, and it is a better answer than the original on
        // every axis: its composition is literally Calcium x1 + Fluorine x2, so it IS CaF2 rather
        // than a mineral that mostly is; it has DUSTS in its generation features and real
        // worldgen; three shipping GTNH recipes in goodgenerator already emit
        // Fluorspar.get(OrePrefixes.dust, N), which is behavioural proof rather than a bit-flag
        // argument; and Werkstoff.get cannot return null the way GTOreDictUnificator.get can.
        //
        // It is NOT routed through BTSRecipeGuard.req. req delegates to GTOreDictUnificator.get,
        // whose OrePrefixes.get(Object) path special-cases Materials and falls back to toString()
        // for anything else - and Werkstoff does not override toString(), so a Werkstoff passed
        // through req would build the OreDict name "dustbartworks.system.material.Werkstoff@1a2b"
        // and find nothing. Werkstoff has its own accessor and it is the one to use.
        //
        // NKE_RANGE is encoded max-then-min by MyRecipeAdder.computeRangeNKE(max, min).
        BTSRecipeGuard.recipe(
            "F-11c",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Calcium, 8L))
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluidsProcess.depletedUF6, 5_000))
                .itemOutputs(
                    new ItemStack(BTSItemsFuel.depletedUraniumIngot, 4),
                    BTSRecipeGuard
                        .nn(WerkstoffLoader.Fluorspar.get(OrePrefixes.dust, 8), "WerkstoffLoader.Fluorspar dust"))
                .metadata(NKE_RANGE, MyRecipeAdder.computeRangeNKE(40, 20))
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(neutronActivatorRecipes));
    }

    // ============================================================ F-12, THE WATER STEP. Nine rungs.

    /**
     * F-12 RECONVERSION, the Integrated Dry Route: {@code UF6 + 2 H2O + H2 -> UO2 + 4 HF}.
     * <p>
     * The water is literally the reagent, which is why the ruling's yield term lands here - and,
     * for the same reason, on the other two oxide-making steps F-20 and F-21a. Nine otherwise
     * identical recipes differ only in which water they drink and how much powder they give back,
     * and the count is asked of {@link BTSWaterGrade#powderFor(int)} rather than typed - 2, 4, 6,
     * 8, 10, 12, 14, 16, 18 for distilled through Grade 8, every rung an integer because base 4
     * was chosen for that. {@link #gradedOutput(int, int)} is the same arithmetic for the two
     * ladders whose base is not 4.
     * <p>
     * <b>They are registered BEST RUNG FIRST, deliberately.</b> GT walks a recipe map in insertion
     * order when it is matching on fluids, so a hatch row holding several grades spends the best
     * one it has - the opposite of {@code drawFeedwater}'s worst-first sweep on the reactor, and
     * for the opposite reason. On the reactor, cheap water must never be hoarded past a job that
     * plain water could do. Here, a player who has bothered to pipe Grade 8 into a fuel line wants
     * that line to use it.
     * <p>
     * <b>Distilled water is a real rung, not a fallback.</b> It yields half of Grade 1 and about
     * 32 Uraninite per rod, and everything after F-12 is grade-blind, so a player who never builds
     * a Purification Plant still reaches all four fuel grades - slowly. That is the whole of the
     * ruling's x0.5.
     */
    /**
     * THE WATER LADDER, APPLIED TO A STEP WHOSE GRADE 1 OUTPUT IS {@code baseAtGradeOne}.
     * Grade 1 is the identity rung, so a step that used to be flat keeps its exact old numbers
     * when it grows a ladder - nothing a player already builds gets cheaper or dearer.
     * <p>
     * The multiplier is {@link BTSWaterGrade#yieldMilli(int)} / 1000: x0.5 distilled, x1.0 Grade
     * 1, +0.5 a grade to x4.5 at Grade 8. The three bases in this file are 4 (F-12, giving 2..18
     * and identical to {@link BTSWaterGrade#powderFor(int)}), 8 (F-20, giving 4..36) and 6
     * (F-21a, giving 3..27). All three are multiples of 2, which is what makes every one of the
     * twenty-seven rungs land on an integer, and the largest is 36 - inside one stack.
     * <p>
     * <b>WHY F-20 AND F-21a NEEDED RUNGS: the balance guard, per fuel grade.</b> The ruling wants
     * the YIELD term to be the larger effect and the reactor's OUTPUT term the smaller. Yield
     * spans x9.00 (0.5 to 4.5); output spans x1.40 (1.000 to 1.400). Write {@code f} for the share
     * of a fuel grade's ceramic mass that comes from a water-graded step; the feed cost across the
     * ladder is then {@code (1 - f) + f / Y}, and its span from distilled to Grade 8 is the yield
     * effect that grade actually feels:
     *
     * <pre>
     *   grade      f       cost @ x0.5   cost @ x4.5   YIELD span   OUTPUT span   guard
     *   uranium    1.0000     2.0000        0.2222       x9.00         x1.40      holds
     *   MOX        0.9375     1.9375        0.2708       x7.15         x1.40      holds
     *   thorium    1.0000     2.0000        0.2222       x9.00         x1.40      holds
     *   naquadah   0.5714     1.5714        0.5556       x2.83         x1.40      holds
     * </pre>
     *
     * MOX is 30 parts depleted UO2 from F-20 to 2 parts PuO2 from R-03, so f = 30/32. Thorium is
     * 30 parts thoria from F-21a to 2 parts UO2 from F-12 - BOTH graded, so f = 1. Naquadah is 8
     * sintered pellets off F-12 against 4 enriched-naquadah and 2 U-235 dust in F-22, so f = 8/14;
     * its HEU-UF6 rider comes off F-11b, upstream of F-12, and is ungraded.
     * <p>
     * <b>What it looked like before F-20 and F-21a had rungs</b>, which is why this is a blocking
     * fix rather than a polish one: MOX had f = 0 and a yield span of x1.00 against an output span
     * of x1.40, and thorium had f = 2/32 and x1.12 against x1.40. On two of the four fuel grades
     * the reactor's water bonus was the LARGER effect - the guard was not merely weak, it was
     * inverted, and a Purification Plant was a route to power rather than to cheaper fuel. The
     * narrowest margin now is naquadah at x2.83 against x1.40, still twice the output term.
     */
    private static int gradedOutput(int grade, int baseAtGradeOne) {
        return baseAtGradeOne * BTSWaterGrade.yieldMilli(grade) / 1000;
    }

    private static void reconversion() {
        for (int grade = BTSWaterGrade.MAX_GRADE; grade >= BTSWaterGrade.DISTILLED; grade--) {
            final int rung = grade;
            final FluidStack water = BTSWaterGrade.fluidFor(grade, 2_000);
            // A rung whose fluid the pack does not have is SKIPPED, never thrown from: recipe
            // registration runs in postInit, where one escaping exception costs the whole mod.
            if (water == null) continue;

            BTSRecipeGuard.recipe(
                "F-12 rung " + rung,
                () -> GTValues.RA.stdBuilder()
                    .circuit(1)
                    .fluidInputs(
                        BTSRecipeGuard.fluid(BTSFluidsProcess.enrichedUF6, 1_000),
                        Materials.Hydrogen.getGas(2_000),
                        water)
                    .itemOutputs(new ItemStack(BTSItemsFuel.ceramicUO2Powder, BTSWaterGrade.powderFor(rung)))
                    .fluidOutputs(Materials.HydrofluoricAcid.getFluid(4_000))
                    .metadata(CHEMPLANT_CASING_TIER, 3)
                    .duration(30 * SECONDS)
                    .eut(TierEU.RECIPE_IV)
                    .addTo(chemicalPlantRecipes));
        }
    }

    // ================================================= F-13 .. F-16b, ceramics and rod components

    private static void ceramics() {
        // F-13 COLD PRESSING. The die lubricant is a real one; the circuit is what keeps the three
        // ceramic grades on three separate NEI entries instead of one ambiguous page.
        pressGreenPellet(BTSItemsFuel.ceramicUO2Powder, BTSItemsFuel.greenPellet, GRADE_URANIUM);
        pressGreenPellet(BTSItemsFuel.moxPowder, BTSItemsFuel.greenPelletMox, GRADE_MOX);
        pressGreenPellet(BTSItemsFuel.thoriumUraniumOxidePowder, BTSItemsFuel.greenPelletThorium, GRADE_THORIUM);

        // F-14 SINTERING, 1,800 K in hydrogen. The reducing atmosphere is not decoration: UO2
        // fired in anything else comes out over-oxidised and useless as fuel.
        sinter(BTSItemsFuel.greenPellet, BTSItemsFuel.sinteredPellet, GRADE_URANIUM);
        sinter(BTSItemsFuel.greenPelletMox, BTSItemsFuel.sinteredPelletMox, GRADE_MOX);
        sinter(BTSItemsFuel.greenPelletThorium, BTSItemsFuel.sinteredPelletThorium, GRADE_THORIUM);

        // F-15 CENTRELESS GRINDING. This is where the chain finally lands on the items an existing
        // world already has in its chests.
        //
        // DEVIATION, forced: latheRecipes is maxIO(1, 2, 0, 0). One item input means NO programmed
        // circuit, so the grade cannot be selected by circuit here - it has to be carried by the
        // item, which is exactly why sinteredPelletMox and sinteredPelletThorium exist as separate
        // registrations. Had they been one fungible "sintered pellet", uranium powder could have
        // been ground into MOX pellets on circuit 2 without a gram of plutonium in the line.
        grind(BTSItemsFuel.sinteredPellet, BTSItems.uraniumFuelPellet);
        grind(BTSItemsFuel.sinteredPelletMox, BTSItems.moxFuelPellet);
        grind(BTSItemsFuel.sinteredPelletThorium, BTSItemsFuel.thoriumBondedPellet);

        // F-15b SWARF RECOVERY. Grinding sludge is reoxidised and goes back into the powder blend.
        // At a 10 % sludge chance and 8 sludge to 4 powder this returns about 5 % of the line -
        // which is roughly what a real pellet shop recovers, and small enough that it never
        // competes with the water ladder as the thing that decides a plant's throughput.
        BTSRecipeGuard.recipe(
            "F-15b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.grinderSludge, 8))
                .circuit(1)
                .itemOutputs(new ItemStack(BTSItemsFuel.ceramicUO2Powder, 4))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(chemicalDehydratorRecipes));

        // F-16 CLADDING TUBE from GoodGenerator's Zircaloy-4 (collision C5). Zircaloy-4 is the
        // real PWR cladding alloy, the pack already makes it, and this gives it a large second
        // customer without touching one GG recipe.
        //
        // DEVIATION, forced by collision: the spec put this on the Extruder with a Pipe (Tiny)
        // die, and it cannot go there. extruderRecipes is maxIO(2, 1, 0, 0) - BOTH item slots are
        // spent on the billet and the die, leaving nowhere to put a programmed circuit. GT's own
        // ProcessingShaping registers "<ingot> + Pipe (Tiny) die -> tiny pipe" for shaped
        // materials, so an addon recipe on the same two slots is indistinguishable from it:
        // whichever registered first wins the lookup and the other becomes unreachable. That is
        // overriding a GTNH recipe by accident, which this addon does not do. The Assembler has
        // nine input slots, so a circuit keys the recipe unambiguously, and rolling and
        // seam-welding two Zircaloy strips into a tube is a real way to make one.
        BTSRecipeGuard.recipe(
            "F-16",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.nn(GGMaterial.zircaloy4.get(OrePrefixes.plate, 2), "GGMaterial.zircaloy4"))
                .circuit(7)
                .fluidInputs(Materials.Lubricant.getFluid(100))
                .itemOutputs(new ItemStack(BTSItemsFuel.claddingTube, 1))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // F-16b END PLUGS AND PLENUM SPRING.
        //
        // DEVIATION, chosen: the spec routed this through the Large Chemical Reactor. Welding two
        // end plugs and a spring is assembly, not chemistry, and a fluid-free LCR page is a poor
        // way to teach it - so it is an Assembler recipe. Nothing is overridden; the output is a
        // new item that nothing else in the pack produces.
        BTSRecipeGuard.recipe(
            "F-16b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.stick, Materials.StainlessSteel, 1L),
                    BTSRecipeGuard.req(OrePrefixes.spring, Materials.StainlessSteel, 1L),
                    BTSRecipeGuard.req(OrePrefixes.plate, WerkstoffLoader.RhodiumPlatedPalladium.getGTMaterial(), 1L))
                .circuit(5)
                .itemOutputs(new ItemStack(BTSItemsFuel.rodHardwareSet, 1))
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));

        // The bundle's spacer grids: Zircaloy strip in an Inconel-substitute frame.
        BTSRecipeGuard.recipe(
            "gridSpacer",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.nn(GGMaterial.zircaloy4.get(OrePrefixes.plate, 2), "GGMaterial.zircaloy4"),
                    BTSRecipeGuard.req(OrePrefixes.ring, Materials.StainlessSteel, 4L))
                .circuit(6)
                .itemOutputs(new ItemStack(BTSItemsFuel.gridSpacer, 2))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(assemblerRecipes));
    }

    private static void pressGreenPellet(Item powder, Item green, int gradeCircuit) {
        BTSRecipeGuard.recipe(
            "F-13 " + BTSRecipeGuard.name(green),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(powder, 4))
                .circuit(gradeCircuit)
                .fluidInputs(Materials.Lubricant.getFluid(250))
                .itemOutputs(new ItemStack(green, 4))
                .duration(8 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(formingPressRecipes));
    }

    private static void sinter(Item green, Item sintered, int gradeCircuit) {
        BTSRecipeGuard.recipe(
            "F-14 " + BTSRecipeGuard.name(sintered),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(green, 4))
                .circuit(gradeCircuit)
                .fluidInputs(Materials.Hydrogen.getGas(1_000))
                .itemOutputs(new ItemStack(sintered, 4))
                .metadata(COIL_HEAT, 1_800)
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(vacuumFurnaceRecipes));
    }

    private static void grind(Item sintered, Item pellet) {
        BTSRecipeGuard.recipe(
            "F-15 " + BTSRecipeGuard.name(pellet),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(sintered, 1))
                .itemOutputs(new ItemStack(pellet, 1), new ItemStack(BTSItemsFuel.grinderSludge, 1))
                .outputChances(10_000, 1_000)
                .duration(5 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(latheRecipes));
    }

    // =============================================== F-17 / F-18, the Fuel Fabrication Hall's job

    private static void fabrication() {
        // F-17 ROD LOADING, WELDING AND HELIUM PRESSURISATION.
        //
        // FOUR PELLETS PER ROD - the exact ratio of retired recipe 2, and one of the two numbers
        // in this whole redesign that may not move (S8.2).
        loadRod(BTSItems.uraniumFuelPellet, BTSItems.fuelRodUranium, GRADE_URANIUM);
        loadRod(BTSItems.moxFuelPellet, BTSItems.fuelRodMox, GRADE_MOX);
        loadRod(BTSItemsFuel.thoriumBondedPellet, BTSItems.fuelRodThoriumBonded, GRADE_THORIUM);

        // F-18 BUNDLE ASSEMBLY.
        //
        // THREE RODS PER ASSEMBLY - the exact ratio of retired recipe 3, and the other number that
        // may not move. Every assembly standing in a Rod Loading Port is worth what it always was.
        bundle(BTSItems.fuelRodUranium, BTSItems.fuelAssemblyUranium, GRADE_URANIUM);
        bundle(BTSItems.fuelRodMox, BTSItems.fuelAssemblyMox, GRADE_MOX);
        bundle(BTSItems.fuelRodThoriumBonded, BTSItems.fuelAssemblyThoriumBonded, GRADE_THORIUM);
    }

    private static void loadRod(Item pellet, Item rod, int gradeCircuit) {
        BTSRecipeGuard.recipe(
            "F-17 " + BTSRecipeGuard.name(rod),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(pellet, 4),
                    new ItemStack(BTSItemsFuel.claddingTube, 1),
                    new ItemStack(BTSItemsFuel.rodHardwareSet, 1))
                .circuit(gradeCircuit)
                .fluidInputs(Materials.Helium.getGas(100))
                .itemOutputs(new ItemStack(rod, 1))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTEFuelFabricationHall.RECIPE_MAP));
    }

    private static void bundle(Item rod, Item assembly, int gradeCircuit) {
        BTSRecipeGuard.recipe(
            "F-18 " + BTSRecipeGuard.name(assembly),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(rod, 3),
                    new ItemStack(BTSItemsFuel.gridSpacer, 2),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Iridium, 1L))
                .circuit(gradeCircuit)
                .fluidInputs(Materials.Platinum.getMolten(2 * INGOTS))
                .itemOutputs(new ItemStack(assembly, 1))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTEFuelFabricationHall.RECIPE_MAP));
    }

    // ====================================================== F-19 / F-20, the MOX grade (F = 2)

    private static void moxGrade() {
        // F-20 TAILS RECONVERSION. Chemically the same reaction as F-12, and now on the same
        // nine-rung water ladder rather than on plain Materials.Water.
        //
        // It used to be flat, on the argument that a MOX pellet's quality is set by its PuO2 blend
        // rather than by the wash water. That argument was wrong about the consequence: with plain
        // water here, MOX was the one fuel grade the ladder could not touch at all, its yield span
        // was x1.00 against the reactor's x1.40 output span, and the balance guard came out
        // INVERTED. gradedOutput() has the arithmetic for all four grades.
        //
        // The batch is two fifths of what it was - 2,000 L of tails against 5,000 and 18 s against
        // 45 s - which leaves powder per litre and powder per second at Grade 1 exactly where they
        // were (8 per 2,000 L is 20 per 5,000 L; 8 in 18 s is 20 in 45 s), and keeps the top rung
        // at 36 rather than 90, comfortably inside one stack.
        //
        // Registered best rung first, as F-12 is and for the same reason. No HF is recovered here
        // even now: the tails route is still the dirty route, which is why its product is only
        // ever good enough to be a diluent.
        for (int grade = BTSWaterGrade.MAX_GRADE; grade >= BTSWaterGrade.DISTILLED; grade--) {
            final int rung = grade;
            final FluidStack water = BTSWaterGrade.fluidFor(grade, 4_000);
            // A rung whose fluid the pack does not have is SKIPPED, never thrown from - see F-12.
            if (water == null) continue;

            BTSRecipeGuard.recipe(
                "F-20 rung " + rung,
                () -> GTValues.RA.stdBuilder()
                    .circuit(1)
                    .fluidInputs(
                        BTSRecipeGuard.fluid(BTSFluidsProcess.depletedUF6, 2_000),
                        Materials.Hydrogen.getGas(4_000),
                        water)
                    .itemOutputs(new ItemStack(BTSItemsFuel.depletedUO2Powder, gradedOutput(rung, 8)))
                    .metadata(CHEMPLANT_CASING_TIER, 3)
                    .duration(18 * SECONDS)
                    .eut(TierEU.RECIPE_IV)
                    .addTo(chemicalPlantRecipes));
        }

        // F-19 THE MIMAS MASTERBLEND. Two parts PuO2 in thirty of depleted UO2 is 6.25 %, which is
        // the real ratio, and the mill is the real machine: MOX is co-milled, not stirred.
        //
        // No programmed circuit: the two reagents plus the milling ball fill all three of
        // millingRecipes' maxIO(3, 1, 0, 0) item slots, and the two reagents already key this
        // recipe uniquely. The ball is not optional - millingBall() says why.
        BTSRecipeGuard.recipe(
            "F-19",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSItemsFuel.plutoniumDioxide, 2),
                    new ItemStack(BTSItemsFuel.depletedUO2Powder, 30),
                    millingBall())
                .itemOutputs(new ItemStack(BTSItemsFuel.moxPowder, 32))
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(millingRecipes));
    }

    // ================================================ F-21a / F-21b, the thorium grade (F = 5)

    private static void thoriumGrade() {
        // F-21a THORIA. GoodGenerator's thorium oxalate had exactly one consumer before this
        // (collision C4); calcining it to ThO2 gives it a second without editing GG's own chain.
        //
        // The oxalate cake is WASHED before it is fired, and that wash is the thorium grade's rung
        // on the water ladder. Without it the thorium fuel was 30 parts ungraded thoria to 2 parts
        // graded UO2, the ladder reached 6 % of the grade, its yield span was x1.12 against the
        // reactor's x1.40 output span, and the balance guard came out inverted exactly as it did
        // on MOX. Washing an oxalate cake before calcining is also simply what the real process
        // does: whatever the wash water leaves behind ends up in the fired ceramic.
        //
        // Base 6, so Grade 1 gives the 6 this step always gave and the rung runs 3 to 27.
        // chemicalDehydratorRecipes is maxIO(2, 9, 1, 1): the oxalate and the circuit fill both
        // item slots, the wash water takes the only fluid input and the CO2 the only fluid output.
        for (int grade = BTSWaterGrade.MAX_GRADE; grade >= BTSWaterGrade.DISTILLED; grade--) {
            final int rung = grade;
            final FluidStack water = BTSWaterGrade.fluidFor(grade, 2_000);
            if (water == null) continue;

            BTSRecipeGuard.recipe(
                "F-21a rung " + rung,
                () -> GTValues.RA.stdBuilder()
                    .itemInputs(
                        BTSRecipeGuard
                            .nn(GGMaterial.thoriumOxalate.get(OrePrefixes.dust, 8), "GGMaterial.thoriumOxalate"))
                    .circuit(1)
                    .fluidInputs(water)
                    .itemOutputs(new ItemStack(BTSItemsFuel.thoriumDioxidePowder, gradedOutput(rung, 6)))
                    .fluidOutputs(Materials.CarbonDioxide.getGas(2_000))
                    .duration(25 * SECONDS)
                    .eut(TierEU.RECIPE_IV)
                    .addTo(chemicalDehydratorRecipes));
        }

        // F-21b (Th,U)O2. Thoria is fertile, not fissile: it needs a driver, and the driver is two
        // parts of the main line's own ceramic powder - the same 6.25 % blend F-19 uses.
        // As at F-19, the milling ball takes the third and last item slot, so this recipe carries
        // no programmed circuit either.
        BTSRecipeGuard.recipe(
            "F-21b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSItemsFuel.thoriumDioxidePowder, 30),
                    new ItemStack(BTSItemsFuel.ceramicUO2Powder, 2),
                    millingBall())
                .itemOutputs(new ItemStack(BTSItemsFuel.thoriumUraniumOxidePowder, 32))
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(millingRecipes));
    }

    // ========================================== F-22 / F-23 / F-18n, the naquadah grade (F = 9)

    /**
     * The Quantum Force Transformer selects its output by programmed circuit; index 0 is the only
     * one a single-output recipe has, and GT numbers the circuits from 1. Omitting it leaves the
     * focus index at -1 and the output is silently voided.
     */
    private static final int QFT_FOCUS_ONLY_OUTPUT = 1;

    private static void naquadahGrade() {
        // F-22 NAQUADAH CHEMICAL VAPOUR INFILTRATION. The UHV step: unrealistic but shaped exactly
        // like CVI, which is a real ceramic-matrix-composite process - a gas phase is driven into
        // the pore network of an already-sintered body and deposits inside it. Here the gas phase
        // is the cascade's HEU cut and molten naquadah, and what comes out is a pellet no chemistry
        // permits. It is the only step in the chain that consumes highlyEnrichedUF6.
        BTSRecipeGuard.recipe(
            "F-22",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSItemsFuel.sinteredPellet, 8),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.NaquadahEnriched, 4L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium235, 2L))
                .fluidInputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.highlyEnrichedUF6, 200),
                    Materials.Naquadah.getMolten(2 * INGOTS))
                .itemOutputs(new ItemStack(BTSItemsFuel.naqBondedPellet, 8))
                // The QFT reads the programmed circuit as WHICH OUTPUT the focus lands on. With no
                // circuit the index resolves to -1 and the machine silently voids 50-100% of what it
                // just made - the same trap P-03h carries a circuit for. One output here, so index 0.
                // The map is maxIO(6, 6, 6, 6), so this fourth item input is well inside the limit.
                .circuit(QFT_FOCUS_ONLY_OUTPUT)
                .metadata(
                    QFT_CATALYST,
                    BTSRecipeGuard.nn(
                        GregtechItemList.AdvancedNaquadahCatalyst.get(0),
                        "GregtechItemList.AdvancedNaquadahCatalyst"))
                .metadata(QFT_FOCUS_TIER, 2)
                .duration(1 * MINUTES)
                .eut(TierEU.RECIPE_UHV)
                .addTo(quantumForceTransformerRecipes));

        // F-23 THE NAQUADAH ROD, on the Precise Assembler rather than the Fuel Fabrication Hall.
        // Same four-pellets-per-rod ratio as F-17; a different machine, so that the top fuel in the
        // game still costs a real endgame multiblock and the Hall cannot become the one building
        // that makes everything.
        //
        // COUNT AUDIT. preciseAssemblerRecipes is RecipeMapBuilder.of("gg.recipe.precise_assembler")
        // .maxIO(4, 1, 4, 0) with no minInputs and no custom recipe emitter, so the only limits are
        // those four. This recipe spends 4 item inputs (three reagents plus the grade circuit) -
        // EXACTLY at the cap, so nothing may be added here without taking something out - 1 fluid
        // input of 4, 1 item output of 1, and 0 fluid outputs, which is the cap: a fluid output
        // here would be silently impossible.
        BTSRecipeGuard.recipe(
            "F-23",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    new ItemStack(BTSItemsFuel.naqBondedPellet, 4),
                    new ItemStack(BTSItemsFuel.claddingTube, 1),
                    new ItemStack(BTSItemsFuel.rodHardwareSet, 1))
                .circuit(GRADE_NAQ)
                .fluidInputs(Materials.Helium.getGas(100))
                .itemOutputs(new ItemStack(BTSItems.fuelRodNaqBonded, 1))
                .metadata(PRECISE_ASSEMBLER_CASING_TIER, 2)
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_UV)
                .addTo(preciseAssemblerRecipes));

        // F-18n THE NAQUADAH BUNDLE, on the Assembly Line. Three rods per assembly, as everywhere
        // else. The research item is the thorium-bonded assembly: the grade below this one, so the
        // progression is scan-what-you-already-make rather than a token from nowhere.
        //
        // COUNT AUDIT, AND THE REGRESSION IT FIXES. GTRecipeConstants.AssemblyLine is a recipe
        // emitter, not a plain map, and it runs five validators before it will build anything:
        // forceOreDictInput(), validateInputCount(4, 16), validateOutputCount(1, 1),
        // validateOutputFluidCount(-1, 0), validateInputFluidCount(1, 4). Any of them failing
        // calls invalidate() and the emitter returns Collections.emptyList() - the recipe is
        // DISCARDED IN SILENCE at postInit, with no log line and no crash.
        //
        // This recipe shipped with THREE item inputs and was therefore never registered at all,
        // leaving fuelAssemblyNaqBonded with no recipe anywhere in the mod - a regression against
        // 0.23.0, where it was a five-slot Assembler recipe. The fourth input below is the
        // bundle's outer channel box: a real fuel bundle is a shroud around the rod array, the
        // pack already makes Zircaloy-4 (F-16 buys from the same tank), and it is the cheapest
        // honest way back to four. Counts now: 4 item inputs of the required 4..16, 1 fluid input
        // of the required 1..4, 1 item output of the required 1..1, 0 fluid outputs of the
        // permitted 0. RESEARCH_ITEM is also mandatory - the emitter returns emptyList() when
        // getMetadata(RESEARCH_ITEM) is null - and it is set on the first line.
        //
        // Note there is no programmed circuit here and there cannot usefully be one: GT's own
        // AssemblyLineRecipes registers 47 recipes and not one of them calls circuit(), and
        // circuit() writes into inputsBasic, so it would count toward validateInputCount as a
        // fourth "component" without being one.
        BTSRecipeGuard.recipe(
            "F-18n",
            () -> GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, new ItemStack(BTSItems.fuelAssemblyThoriumBonded, 1))
                .metadata(SCANNING, new Scanning(5 * MINUTES, TierEU.RECIPE_ZPM))
                .itemInputs(
                    new ItemStack(BTSItems.fuelRodNaqBonded, 3),
                    new ItemStack(BTSItemsFuel.gridSpacer, 2),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Iridium, 2L),
                    BTSRecipeGuard.nn(GGMaterial.zircaloy4.get(OrePrefixes.plate, 4), "GGMaterial.zircaloy4"))
                .fluidInputs(Materials.Naquadah.getMolten(2 * INGOTS))
                .itemOutputs(new ItemStack(BTSItems.fuelAssemblyNaqBonded, 1))
                .duration(1 * MINUTES)
                .eut(TierEU.RECIPE_UV)
                .addTo(AssemblyLine));
    }

    // ================================================ R-01 .. R-04, PUREX and the D-09 closure

    private static void reprocessing() {
        // R-01 THE SPENT FUEL COOLING POND, in a Decay Warehouse.
        //
        // isotopeDecay is maxIO(1, 1, 0, 0) - one item in, one out, no fluids and no circuit - so
        // each of the eight depleted forms needs its own entry. HALF_LIFE is a Double in SECONDS:
        // MTEDecayWarehouse ticks once every twenty ticks and applies exp(-ln2 / halfLife) each
        // time. An assembly is bulkier than a rod and takes three times as long to cool.
        coolInPond(BTSItems.depletedFuelAssemblyUranium, BTSItemsFuel.cooledAssemblyUranium, 120.0);
        coolInPond(BTSItems.depletedFuelAssemblyMox, BTSItemsFuel.cooledAssemblyMox, 120.0);
        coolInPond(BTSItems.depletedFuelAssemblyThoriumBonded, BTSItemsFuel.cooledAssemblyThoriumBonded, 120.0);
        coolInPond(BTSItems.depletedFuelAssemblyNaqBonded, BTSItemsFuel.cooledAssemblyNaqBonded, 120.0);
        coolInPond(BTSItems.depletedFuelRodUranium, BTSItemsFuel.cooledRodUranium, 40.0);
        coolInPond(BTSItems.depletedFuelRodMox, BTSItemsFuel.cooledRodMox, 40.0);
        coolInPond(BTSItems.depletedFuelRodThoriumBonded, BTSItemsFuel.cooledRodThoriumBonded, 40.0);
        coolInPond(BTSItems.depletedFuelRodNaqBonded, BTSItemsFuel.cooledRodNaqBonded, 40.0);

        // R-02 CHOP-LEACH DISSOLUTION, the cooled head-end. Every grade dissolves into the same
        // liquor - that is what a dissolver does - but each one gives back the material that made
        // it special, so a thorium bundle still returns thorium and a naquadah bundle still returns
        // naquadah, exactly as the retired centrifuge recipes did.
        chopLeach(BTSItemsFuel.cooledAssemblyUranium, 6_000, 4_000, 2, null);
        chopLeach(BTSItemsFuel.cooledAssemblyMox, 6_000, 4_000, 2, new ItemStack(BTSItemsFuel.plutoniumDioxide, 1));
        chopLeach(
            BTSItemsFuel.cooledAssemblyThoriumBonded,
            6_000,
            4_000,
            2,
            BTSRecipeGuard.req(OrePrefixes.dust, Materials.Thorium, 2L));
        chopLeach(
            BTSItemsFuel.cooledAssemblyNaqBonded,
            6_000,
            4_000,
            2,
            BTSRecipeGuard.req(OrePrefixes.dust, Materials.Naquadah, 2L));

        // The SD-1 rod cascade drops rods, not assemblies, so every reprocessing shape has a
        // rod-scale twin at one third the numbers. Three rod-scale runs make exactly the 6,000 L
        // of liquor one R-03 partition wants, so no rod-scale R-03 is needed at all.
        chopLeach(BTSItemsFuel.cooledRodUranium, 2_000, 1_400, 1, null);
        chopLeach(
            BTSItemsFuel.cooledRodMox,
            2_000,
            1_400,
            1,
            BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 1L));
        chopLeach(
            BTSItemsFuel.cooledRodThoriumBonded,
            2_000,
            1_400,
            1,
            BTSRecipeGuard.req(OrePrefixes.dust, Materials.Thorium, 1L));
        chopLeach(
            BTSItemsFuel.cooledRodNaqBonded,
            2_000,
            1_400,
            1,
            BTSRecipeGuard.req(OrePrefixes.dust, Materials.Naquadah, 1L));

        // R-02h THE UNCOOLED RUSH. This is the mechanic that earns the PUREX Canyon its MTE slot:
        // nothing else in the pack has an input whose HISTORY changes the recipe. Fuel straight out
        // of the reactor is short-lived-isotope hot, so the partition cannot be run on it - it goes
        // through in one pass, it costs half again as much nitric acid, and it yields NO plutonium
        // and therefore no MOX. What it does do is clear a backlog with one machine instead of two,
        // which is the migration mitigation S8.4 promises: a world that loads into 0.24.0 with a
        // pile of depleted fuel needs the Canyon and nothing else to dig itself out.
        rushLeach(BTSItems.depletedFuelAssemblyUranium, 4, 1, 2, 6_000, 2_000);
        rushLeach(BTSItems.depletedFuelAssemblyMox, 4, 1, 2, 6_000, 2_000);
        rushLeach(BTSItems.depletedFuelAssemblyThoriumBonded, 4, 1, 2, 6_000, 2_000);
        rushLeach(BTSItems.depletedFuelAssemblyNaqBonded, 4, 1, 2, 6_000, 2_000);
        rushLeach(BTSItems.depletedFuelRodUranium, 1, 1, 1, 2_000, 700);
        rushLeach(BTSItems.depletedFuelRodMox, 1, 1, 1, 2_000, 700);
        rushLeach(BTSItems.depletedFuelRodThoriumBonded, 1, 1, 1, 2_000, 700);
        rushLeach(BTSItems.depletedFuelRodNaqBonded, 1, 1, 1, 2_000, 700);

        // R-03 U/Pu CO-EXTRACTION AND PARTITION - the same TBP solvent the front end already
        // makes at F-05a, which is not a coincidence: PUREX and uranium SX are the same chemistry.
        //
        // ITS RAFFINATE IS BTSFluids.nuclearWasteWater, AND THAT CLOSES D-09. That fluid has had a
        // consumer since the mod shipped - the distillation at the bottom of this file - and no
        // producer at all. It has one now.
        BTSRecipeGuard.recipe(
            "R-03",
            () -> GTValues.RA.stdBuilder()
                .circuit(PUREX_PARTITION)
                .fluidInputs(
                    BTSRecipeGuard.fluid(BTSFluidsProcess.dissolverLiquor, 6_000),
                    BTSRecipeGuard.fluid(BTSFluidsProcess.tbpSolvent, 2_000))
                .itemOutputs(
                    new ItemStack(BTSItemsFuel.ceramicUO2Powder, 4),
                    new ItemStack(BTSItemsFuel.plutoniumDioxide, 1),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, 1L))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluids.nuclearWasteWater, 2_000))
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTEPurexCanyon.RECIPE_MAP));

        // R-04 HULL RECOVERY. Zircaloy is expensive and the hulls are most of what a bundle weighs;
        // getting the cladding back is what makes reprocessing worth building rather than merely
        // worth doing.
        BTSRecipeGuard.recipe(
            "R-04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.claddingHull, 4))
                .circuit(1)
                .fluidInputs(Materials.HydrofluoricAcid.getFluid(1_000))
                .itemOutputs(
                    BTSRecipeGuard.nn(GGMaterial.zircaloy4.get(OrePrefixes.ingot, 1), "GGMaterial.zircaloy4"),
                    WerkstoffLoader.getCorrespondingItemStack(OrePrefixes.dust, WerkstoffLoader.Zirconium, 1))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(UniversalChemical));
    }

    private static void coolInPond(Item depleted, Item cooled, double halfLifeSeconds) {
        BTSRecipeGuard.recipe(
            "R-01 " + BTSRecipeGuard.name(cooled),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(depleted, 1))
                .itemOutputs(new ItemStack(cooled, 1))
                .metadata(HALF_LIFE, halfLifeSeconds)
                .metadata(DECAY_TYPE, GTRecipeConstants.DecayType.Alpha)
                .duration(1)
                .eut(1)
                .addTo(isotopeDecay));
    }

    private static void chopLeach(Item cooled, int liquor, int nitric, int hulls, ItemStack gradeReturn) {
        final ItemStack[] outputs = gradeReturn == null
            ? new ItemStack[] { new ItemStack(BTSItemsFuel.claddingHull, hulls) }
            : new ItemStack[] { new ItemStack(BTSItemsFuel.claddingHull, hulls), gradeReturn };

        BTSRecipeGuard.recipe(
            "R-02 " + BTSRecipeGuard.name(cooled),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(cooled, 1))
                .circuit(PUREX_COOLED)
                .fluidInputs(Materials.NitricAcid.getFluid(nitric))
                .itemOutputs(outputs)
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluidsProcess.dissolverLiquor, liquor))
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTEPurexCanyon.RECIPE_MAP));
    }

    private static void rushLeach(Item depleted, int powder, int lead, int hulls, int nitric, int wasteWater) {
        BTSRecipeGuard.recipe(
            "R-02h " + BTSRecipeGuard.name(depleted),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(depleted, 1))
                .circuit(PUREX_RUSH)
                .fluidInputs(Materials.NitricAcid.getFluid(nitric))
                .itemOutputs(
                    new ItemStack(BTSItemsFuel.ceramicUO2Powder, powder),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, lead),
                    new ItemStack(BTSItemsFuel.claddingHull, hulls))
                .fluidOutputs(BTSRecipeGuard.fluid(BTSFluids.nuclearWasteWater, wasteWater))
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTEPurexCanyon.RECIPE_MAP));
    }

    // ======================================= the crude route: depleted fuel WITHOUT a PUREX Canyon

    /**
     * THE EIGHT CENTRIFUGE ROUTES, RESTORED. Every one of the eight depleted forms can be torn
     * down on a plain Centrifuge again, at exactly the numbers it had in 0.23.0.
     * <p>
     * <b>Why they came back.</b> The redesign moved all reprocessing onto {@link MTEPurexCanyon},
     * and every replacement sink - R-02, R-02h, R-03 - is on that machine's map. A world that
     * loads into this version has depleted fuel in its chests and, very often, a Centrifuge array
     * already piped to consume it; on the day of the update that array stops dead and its stock
     * has NO route at all until a whole new multiblock is built. That is not a difficulty curve,
     * it is a stall, and it is the one migration failure a release pass has to catch.
     * <p>
     * <b>PUREX stays the efficient route; this is the crude one.</b> The Canyon gives back
     * {@code ceramicUO2Powder}, a ceramic that drops straight back into F-13, plus the cladding
     * hulls for R-04 and the nuclear waste water that pays the D-09 anchor. The Centrifuge gives
     * back raw metal DUST, which has to climb the whole conversion ladder from F-09 again, no
     * hulls, and no waste water. So the Canyon is strictly better on every axis and remains the
     * thing worth building. It is now an upgrade rather than a toll gate.
     * <p>
     * <b>Not one number moved, and no programmed circuit was added.</b> The values are the eight
     * retired recipes verbatim, from {@code git show 5344bcf}. A circuit would have been tidier
     * beside this file's other Centrifuge entries, but it would also have broken every standing
     * automated line, which is the exact failure these recipes exist to prevent.
     * {@code centrifugeRecipes} is {@code maxIO(2, 6, 1, 1)}: one item input and four chanced
     * outputs fit with a slot to spare.
     */
    private static void crudeReprocessing() {
        crudeCentrifuge(
            BTSItems.depletedFuelAssemblyUranium,
            1 * MINUTES,
            TierEU.RECIPE_EV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 4L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 2L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, 1L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.Platinum, 2L) },
            new int[] { 10_000, 10_000, 10_000, 5_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelAssemblyMox,
            1 * MINUTES,
            TierEU.RECIPE_EV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 3L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 3L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, 2L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.Platinum, 2L) },
            new int[] { 10_000, 10_000, 10_000, 5_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelAssemblyThoriumBonded,
            1 * MINUTES,
            TierEU.RECIPE_IV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 4L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Thorium, 4L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 2L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, 2L) },
            new int[] { 10_000, 10_000, 10_000, 10_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelAssemblyNaqBonded,
            1 * MINUTES,
            TierEU.RECIPE_LuV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 4L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Naquadah, 4L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 3L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Lead, 2L) },
            new int[] { 10_000, 10_000, 10_000, 10_000 });

        // The SD-1 rod cascade drops rods, so every assembly shape has its rod-scale twin here too,
        // at a third of the numbers - exactly as R-02 and R-02h do above.
        crudeCentrifuge(
            BTSItems.depletedFuelRodUranium,
            20 * SECONDS,
            TierEU.RECIPE_EV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Lead, 1L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.Platinum, 1L) },
            new int[] { 10_000, 6_700, 10_000, 5_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelRodMox,
            20 * SECONDS,
            TierEU.RECIPE_EV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Lead, 2L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.Platinum, 1L) },
            new int[] { 10_000, 10_000, 10_000, 5_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelRodThoriumBonded,
            20 * SECONDS,
            TierEU.RECIPE_IV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Thorium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Lead, 2L) },
            new int[] { 10_000, 10_000, 6_700, 10_000 });

        crudeCentrifuge(
            BTSItems.depletedFuelRodNaqBonded,
            20 * SECONDS,
            TierEU.RECIPE_LuV,
            new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uranium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dust, Materials.Naquadah, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Plutonium, 1L),
                BTSRecipeGuard.req(OrePrefixes.dustSmall, Materials.Lead, 2L) },
            new int[] { 10_000, 10_000, 10_000, 10_000 });
    }

    private static void crudeCentrifuge(Item depleted, int duration, long eut, ItemStack[] outputs, int[] chances) {
        BTSRecipeGuard.recipe(
            "crude centrifuge " + BTSRecipeGuard.name(depleted),
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(depleted, 1))
                .itemOutputs(outputs)
                .outputChances(chances)
                .duration(duration)
                .eut(eut)
                .addTo(centrifugeRecipes));
    }

    // ============================================================ the three barren-rock streams

    /**
     * Gangue, tailings and leach residue are the three things the front end throws away, and a
     * chain that only ever makes rubbish is a chain that teaches players to void its outputs. Each
     * gets one cheap Centrifuge entry, and the flotation tailings get back a little of the uranium
     * the cell failed to float - which is what a real scavenger circuit is for.
     */
    private static void byproductCleanup() {
        BTSRecipeGuard.recipe(
            "dust-Stone",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.gangueDust, 8))
                .circuit(1)
                .itemOutputs(
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Stone, 4L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Calcite, 1L))
                .duration(15 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(centrifugeRecipes));

        BTSRecipeGuard.recipe(
            "dust-Stone (2)",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.flotationTailings, 8))
                .circuit(1)
                .itemOutputs(
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Stone, 4L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Uraninite, 2L))
                .outputChances(10_000, 3_300)
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_HV)
                .addTo(centrifugeRecipes));

        BTSRecipeGuard.recipe(
            "dust-Gypsum",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(BTSItemsFuel.leachResidue, 4))
                .circuit(1)
                .itemOutputs(
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Gypsum, 2L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Silicon, 1L))
                .duration(15 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(centrifugeRecipes));
    }

    // ================================================================= what did NOT change (D-09)

    /**
     * The one recipe S8.1 kept. Not one number moved; it is here so the file that owns the fuel
     * line owns the fluid the fuel line finally produces. Until R-02h and R-03 landed above, this
     * was a consumer with no producer anywhere in the mod.
     */
    private static void keptFromBefore() {
        BTSRecipeGuard.recipe(
            "D-09 nuclear waste water",
            () -> GTValues.RA.stdBuilder()
                .circuit(1)
                .fluidInputs(BTSRecipeGuard.fluid(BTSFluids.nuclearWasteWater, 8_000))
                .itemOutputs(Materials.Tantalum.getDustSmall(1))
                .fluidOutputs(GTModHandler.getDistilledWater(7_200))
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(distillationTowerRecipes));
    }
}
