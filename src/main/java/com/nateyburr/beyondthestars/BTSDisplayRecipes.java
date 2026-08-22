package com.nateyburr.beyondthestars;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.fluids.BTSWaterGrade;
import com.nateyburr.beyondthestars.items.BTSItemsFuel;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.RecipeDisplayInfo;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.xmod.thermalfoundation.fluid.TFFluids;

/**
 * B1 - the Nuclear Plant Foundation's NEI pages: three DISPLAY-ONLY recipe maps that show the
 * plant's fuel, propellant and coolant math the way the Large Boiler shows its fuels.
 * <p>
 * <b>Why this is display-only, precisely.</b> A GT machine only ever consumes recipes from the
 * map {@code getRecipeMap()} returns, and the Nuclear Plant's {@code getRecipeMap()} stays
 * {@code null} - it runs a hand-written {@code checkProcessing()} that burns rod segments on a
 * decay cycle and never calls {@code findRecipe} at all. What the controller overrides is
 * {@link gregtech.api.interfaces.tileentity.RecipeMapWorkable#getAvailableRecipeMaps()}, which
 * NEI alone reads (NEIGTConfig#generateRecipeCatalystIndex) to decide which tabs a machine is
 * the catalyst for. On top of that every entry here is built with {@code .fake()}, the flag GT
 * documents as "findRecipe won't find fake Recipes". Three independent reasons, then, why
 * nothing on these pages can ever be crafted, consumed or unlocked:
 * <ol>
 * <li>the controller has no recipe map to process,</li>
 * <li>these maps are never handed to the hatches ({@code mRecipeMap} would filter them), and</li>
 * <li>the recipes are fake.</li>
 * </ol>
 * Every number printed here is pulled out of {@link MTEReactorCoreMk1}'s own runtime tables
 * through its public accessors, so retuning a fuel multiplier, a propellant P or a coolant Q
 * moves the page with the plant - the page cannot drift into a lie.
 */
public final class BTSDisplayRecipes {

    private BTSDisplayRecipes() {}

    /** A page whose energy/duration lines are suppressed - it has no recipe to overclock. */
    public static final class DisplayFrontend extends RecipeMapFrontend {

        public DisplayFrontend(BasicUIPropertiesBuilder uiPropertiesBuilder,
            NEIRecipePropertiesBuilder neiPropertiesBuilder) {
            super(uiPropertiesBuilder, neiPropertiesBuilder);
        }

        /** No EU is spent: the plant GENERATES. (Same trick as LargeBoilerFuelFrontend.) */
        @Override
        protected void drawEnergyInfo(RecipeDisplayInfo recipeInfo) {}

        /** The real "duration" is a decay cycle, which the description states in minutes. */
        @Override
        protected void drawDurationInfo(RecipeDisplayInfo recipeInfo) {}
    }

    /** Rod grade -> steam per pillar, cycle length, depleted output. */
    public static final RecipeMap<RecipeMapBackend> plantFuelRods = RecipeMapBuilder
        .of("beyondthestars.recipe.plantfuel")
        .maxIO(1, 1, 0, 0)
        .minInputs(1, 0)
        .frontend(DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(220))
        .build();

    /**
     * Propellant ladder -> stage factor P, RPM gain per cycle, held RPM. 220 px, matching
     * the fuel-rod page: both carry 13 description lines.
     */
    public static final RecipeMap<RecipeMapBackend> plantPropellant = RecipeMapBuilder
        .of("beyondthestars.recipe.plantpropellant")
        .maxIO(0, 0, 1, 0)
        .minInputs(0, 1)
        .frontend(DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(220))
        .build();

    /** Coolant ladder -> quality Q and the litres/tick the chamber drinks at speed. */
    public static final RecipeMap<RecipeMapBackend> plantCoolant = RecipeMapBuilder
        .of("beyondthestars.recipe.plantcoolant")
        .maxIO(0, 0, 1, 0)
        .minInputs(0, 1)
        .frontend(DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(210))
        .build();

    /**
     * D-26: the nine rungs of the purified-water ladder, each with BOTH of its effects on one
     * page - the fuel-line YIELD term and the reactor OUTPUT term.
     * <p>
     * The whole ruling lives on this page, which is why it is worth a fourth tab. Every number on
     * it is read out of {@link BTSWaterGrade}, the same class the F-12 recipes and the reactor's
     * own euDemandOf() call read, so the page cannot tell a player one thing while the plant does
     * another.
     */
    public static final RecipeMap<RecipeMapBackend> plantWater = RecipeMapBuilder.of("beyondthestars.recipe.plantwater")
        .maxIO(0, 1, 1, 0)
        .minInputs(0, 1)
        .frontend(DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(220))
        .build();

    private static boolean loaded;

    /**
     * Fills the three pages. Called from {@link BTSRecipes#register()} in postInit, which is
     * (a) inside our own mod container, so the categories are attributed to this mod, and
     * (b) before NEI builds its handler list at load-complete.
     */
    public static void register() {
        if (loaded) return;
        loaded = true;
        addFuelRodPages();
        addPropellantPages();
        addCoolantPages();
        addWaterPages();
    }

    // --------------------------------------------------------------- missing-dependency guards
    // A page that cannot be built must SKIP, never throw: register() runs in postInit, so one
    // escaping exception takes the whole mod down instead of costing the player one NEI tab.
    //
    // The trap this closes: a `stack == null` test after the fact cannot catch a bad fluid,
    // because Forge 1.7.10's FluidStack(Fluid,int) never RETURNS a broken stack - it throws
    // IllegalArgumentException("Cannot create a fluidstack from a null fluid") and
    // ("...from an unregistered fluid") from the constructor itself. So every rung of the ladders
    // that is hand-built with `new FluidStack(fluid, litres)` has to be vetted BEFORE the ladder
    // is asked for it. Rungs that come out of GT's own Materials table are different: those
    // accessors return null instead of throwing, so for them the caller's null test is the guard.

    /** A Fluid may be wrapped in a FluidStack only if it exists AND the registry knows it. */
    private static boolean usable(Fluid fluid) {
        return fluid != null && FluidRegistry.isFluidRegistered(fluid);
    }

    /**
     * Whether rung {@code rung} of the propellant ladder can be turned into a FluidStack at all.
     * <p>
     * This switch MIRRORS {@link MTEReactorCoreMk1#propellantOfRung(int, int)} case for case,
     * {@code default:} included, so a rung appended to the ladder lands on the same branch in
     * both. Rungs 0-1 are {@code Materials} lookups and need no pre-check; rungs 2-8 are GT++'s
     * rocket fuel and our own six Naquadah Propellants, all of which are hand-wrapped and
     * therefore fatal if absent.
     */
    private static boolean propellantRungAvailable(int rung) {
        switch (rung) {
            case 0: // Materials.Benzene.getFluid - returns null, never throws
            case 1: // Materials.GasolinePremium.getFluid - returns null, never throws
                return true;
            case 2:
                return usable(GTPPFluids.H8N4C2O4RocketFuel);
            case 3:
                return usable(BTSFluids.naqPropellantT1);
            case 4:
                return usable(BTSFluids.naqPropellantT2);
            case 5:
                return usable(BTSFluids.naqPropellantT3);
            case 6:
                return usable(BTSFluids.naqPropellantT4);
            case 7:
                return usable(BTSFluids.naqPropellantT5);
            default:
                return usable(BTSFluids.naqPropellantT6);
        }
    }

    /**
     * Whether rung {@code rung} of the coolant ladder can be turned into a FluidStack at all.
     * <p>
     * Mirrors {@link MTEReactorCoreMk1#coolantOfRung(int, int)} case for case. Only rung 1
     * (Thermal Foundation's Cryotheum, reached through GT++'s bridge class) is hand-wrapped; the
     * other rungs come from {@code Materials} or {@code GTModHandler}, which hand back null when
     * the fluid is absent - IC2 Coolant on rung 3 being the live example.
     */
    private static boolean coolantRungAvailable(int rung) {
        switch (rung) {
            case 0: // Materials.Time.getMolten - returns null, never throws
                return true;
            case 1:
                return usable(TFFluids.fluidCryotheum);
            case 2: // Materials.SuperCoolant.getFluid
            case 3: // GTModHandler.getIC2Coolant - null when IC2 is not installed
            case 4: // GTModHandler.getDistilledWater
            default: // Materials.Water.getFluid
                return true;
        }
    }

    /** One page failed to register: say so loudly, then carry on with the rest. */
    private static void pageFailed(String page, int index, Throwable cause) {
        BeyondTheStars.LOG
            .error("NEI: could not register the " + page + " page for entry " + index + "; skipping it.", cause);
    }

    /** One page was deliberately left out because its fluid/item is not in this pack. */
    private static void pageSkipped(String page, int index) {
        BeyondTheStars.LOG
            .info("NEI: " + page + " entry " + index + " has no registered fluid in this pack - page skipped.");
    }

    // ------------------------------------------------------------------------ fuel rods

    private static void addFuelRodPages() {
        final int cycleTicks = MTEReactorCoreMk1.decayCycleTicks();
        final int cycleMinutes = Math.max(1, cycleTicks / (20 * 60));
        final int segments = MTEReactorCoreMk1.segmentsPerPillar();
        final long basis = MTEReactorCoreMk1.steamPerPillar();
        for (int grade = 1; grade <= MTEReactorCoreMk1.ROD_GRADES; grade++) {
            // Same "vet it before you wrap it" rule as the fluid ladders. `new ItemStack(null, 1)`
            // does not throw in 1.7.10 - it builds a stack whose getItem() is null, which then NPEs
            // inside the recipe builder's unification pass. Check the Item, not the ItemStack.
            final Item fresh = MTEReactorCoreMk1.fuelRodItem(grade);
            final Item depleted = MTEReactorCoreMk1.depletedRodItem(grade);
            if (fresh == null || depleted == null) {
                pageSkipped("fuel rod", grade);
                continue;
            }
            final int mult = MTEReactorCoreMk1.rodMultiplier(grade);
            final long perPillar = basis * mult;
            final long tier1 = perPillar * MTEReactorCoreMk1.columnCapOf(1);
            final long tier5 = perPillar * MTEReactorCoreMk1.columnCapOf(5);
            try {
                GTValues.RA.stdBuilder()
                    .itemInputs(new ItemStack(fresh, 1))
                    .itemOutputs(new ItemStack(depleted, 1))
                    .duration(cycleTicks)
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    // Lines are kept to ~28 characters: the NEI description column is 170px wide
                    // and drawText does NOT wrap (GT hard-wraps its own lines for the same reason).
                    .setNEIDesc(
                        "Fuel multiplier: x" + mult,
                        "Steam/pillar " + fmt(basis) + " x" + mult + " =",
                        "  " + fmt(perPillar) + " L/t at 24/24 rods",
                        "1 rod = 1 of " + segments + " segments",
                        "Burns bottom-up: " + cycleMinutes + " min ea.",
                        "Full pillar: " + (cycleMinutes * segments / 60) + " h",
                        "T1 (" + MTEReactorCoreMk1.columnCapOf(1) + "): " + fmt(tier1) + " L/t",
                        "T5 (" + MTEReactorCoreMk1.columnCapOf(5) + "): " + fmt(tier5) + " L/t",
                        "EU/t = L/t x spd x eff",
                        "  x RPM/500 x avg rotor",
                        "In: silo face, rod course",
                        "Out: bus beside it, same course",
                        "Display only - not a recipe")
                    .addTo(plantFuelRods);
            } catch (RuntimeException broken) {
                // Backstop only - the guards above are the mechanism. This exists because the
                // ladders/tables live in MTEReactorCoreMk1 and can gain entries; a new entry must
                // cost its own page, never the mod's ability to load.
                pageFailed("fuel rod", grade, broken);
            }
        }
    }

    // ----------------------------------------------------------------------- propellant

    private static void addPropellantPages() {
        final int injectors = 8;
        for (int rung = 0; rung < MTEReactorCoreMk1.PROPELLANT_RUNGS; rung++) {
            // BEFORE the ladder is asked for the rung: a null or unregistered Fluid on rungs 2-8
            // would make `new FluidStack(fluid, litres)` THROW inside propellantOfRung, so the
            // `shot == null` test below could never run for them.
            if (!propellantRungAvailable(rung)) {
                pageSkipped("propellant", rung);
                continue;
            }
            final FluidStack shot = MTEReactorCoreMk1.propellantOfRung(rung, injectors);
            // Still needed, and now genuinely reachable: rungs 0-1 are Materials lookups, which
            // return null rather than throwing when the material carries no fluid.
            if (shot == null || shot.getFluid() == null) {
                pageSkipped("propellant", rung);
                continue;
            }
            final int pMilli = MTEReactorCoreMk1.propellantMilli(shot);
            // gain(milli) = RPM_GAIN x P x timed/8, exactly as evaluateCycle computes it.
            final long gain = (long) MTEReactorCoreMk1.rpmGain() * pMilli / 1000L;
            // Q4: "Naquadah Propellant Lv 1-6" (ours, rungs 3-8) and GoodGenerator's
            // "Naquadah Fuel" / "Naquadah Based Fuel Mk I-VI" are DIFFERENT fluids, and the
            // fuel is precisely what BTSRecipes' Mixer recipes consume to make the
            // propellant. A player who pipes the fuel to an injector gets the P x1.00 floor
            // and no error, so the page names each rung's provenance out loud.
            final String source = rung >= 3 ? "Mixed from Naquadah Fuel"
                : rung == 2 ? "GT++ rocket fuel" : "GT distillation line";
            try {
                GTValues.RA.stdBuilder()
                    .fluidInputs(shot)
                    .duration(MTEReactorCoreMk1.cycleSlugTicks())
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    .setNEIDesc(
                        "Stage factor P: x" + String.format("%.2f", pMilli / 1000.0),
                        "1 L in each of " + injectors + " injectors",
                        "per accepted cycle (shown: " + injectors + " L)",
                        "Gain = " + MTEReactorCoreMk1.rpmGain() + " x P x timed/8",
                        "  = " + fmt(gain) + " RPM at 8/8 timed",
                        "Held at 1 cycle/s: " + fmt(heldRpm(gain)),
                        "Redline " + fmt(MTEReactorCoreMk1.RPM_REDLINE),
                        "5 of 8 stocked = 5/8 gain",
                        "A missed cycle burns " + injectors + " L",
                        source,
                        "In: Spiral chamber, front wall",
                        "Plant lands the timed shot",
                        "Display only - not a recipe")
                    .addTo(plantPropellant);
            } catch (RuntimeException broken) {
                pageFailed("propellant", rung, broken);
            }
        }
    }

    /**
     * The RPM a ladder rung settles at when a cycle lands every {@link MTEReactorCoreMk1#cycleSlugTicks()}
     * ticks and the fan decays 0.5% per tick: {@code gain / (1 - 0.995^20)}, capped at the redline.
     */
    private static long heldRpm(long gain) {
        final double perTick = MTEReactorCoreMk1.rpmDecayPermille() / 1000.0;
        final double retained = Math.pow(perTick, MTEReactorCoreMk1.cycleSlugTicks());
        final long held = (long) (gain / (1.0 - retained));
        return Math.min(held, MTEReactorCoreMk1.RPM_REDLINE);
    }

    // -------------------------------------------------------------------------- coolant

    private static void addCoolantPages() {
        final int redlineLoad = MTEReactorCoreMk1.chamberLoadFor(MTEReactorCoreMk1.RPM_REDLINE * 1000, 0);
        final int halfLoad = MTEReactorCoreMk1.chamberLoadFor(MTEReactorCoreMk1.RPM_REDLINE * 1000 / 2, 0);
        for (int rung = 0; rung < MTEReactorCoreMk1.coolantRungs(); rung++) {
            // BEFORE the ladder is asked for the rung: rung 1 is Cryotheum, hand-wrapped in a
            // FluidStack, so without Thermal Foundation coolantOfRung THROWS instead of returning
            // null and the `draw == null` test below never gets a chance to skip it.
            if (!coolantRungAvailable(rung)) {
                pageSkipped("coolant", rung);
                continue;
            }
            final int atRedline = MTEReactorCoreMk1.coolantDrainFor(redlineLoad, rung);
            final FluidStack draw = MTEReactorCoreMk1.coolantOfRung(rung, atRedline);
            // Still needed, and reachable: Materials/GTModHandler rungs return null when absent
            // (IC2 Coolant on rung 3 is the live example).
            if (draw == null || draw.getFluid() == null) {
                pageSkipped("coolant", rung);
                continue;
            }
            final int atHalf = MTEReactorCoreMk1.coolantDrainFor(halfLoad, rung);
            try {
                GTValues.RA.stdBuilder()
                    .fluidInputs(draw)
                    .duration(1)
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    .setNEIDesc(
                        "Coolant quality Q: " + String.format("%.1f", MTEReactorCoreMk1.coolantQMilli(rung) / 1000.0),
                        "Drain = ceil(load / Q) L/t",
                        "Load = 200 x (RPM/1000)^2",
                        "  + swirl^2 x 5/20",
                        "At " + fmt(MTEReactorCoreMk1.RPM_REDLINE) + " RPM: load " + fmt(redlineLoad),
                        "  -> " + fmt(atRedline) + " L/t (shown)",
                        "At " + fmt(MTEReactorCoreMk1.RPM_REDLINE / 2) + " RPM: " + fmt(atHalf) + " L/t",
                        "Half the RPM, a quarter cost",
                        "No cover = cycle refused",
                        "In: Spiral chamber, front wall",
                        "Display only - not a recipe")
                    .addTo(plantCoolant);
            } catch (RuntimeException broken) {
                pageFailed("coolant", rung, broken);
            }
        }
    }

    // ------------------------------------------------------------------- D-26 the water ladder

    /**
     * One page per rung, distilled through Grade 8. Both of the ruling's terms are printed side
     * by side, because the single most important thing a player can learn about this mechanic is
     * the RATIO between them: the water grade moves fuel YIELD by x9.00 and reactor OUTPUT by
     * x1.40, so a Purification Plant buys cheaper fuel and never raw power.
     * <p>
     * The "Uraninite per rod" figure is the balance argument made concrete - 16.0 at Grade 1,
     * where one Extraction Derrick exactly feeds a fully-built tier-5 plant, down to 3.6 at
     * Grade 8. It is derived from the same yield term rather than tabulated, so it moves with the
     * ladder if the ladder is ever retuned.
     */
    private static void addWaterPages() {
        final Item powder = BTSItemsFuel.ceramicUO2Powder;
        if (powder == null) {
            pageSkipped("water grade", 0);
            return;
        }
        for (int grade = BTSWaterGrade.DISTILLED; grade <= BTSWaterGrade.MAX_GRADE; grade++) {
            // Same "vet it before you wrap it" rule the three ladders above use. Every rung here
            // comes out of Materials/GTModHandler, which hand back null rather than throwing - so
            // one null test is the whole guard, and a pack without the Purification Plant's
            // materials loses tabs instead of the mod.
            final FluidStack water = BTSWaterGrade.fluidFor(grade, 2_000);
            if (water == null || !usable(water.getFluid())) {
                pageSkipped("water grade", grade);
                continue;
            }
            final int yieldMilli = BTSWaterGrade.yieldMilli(grade);
            final int count = BTSWaterGrade.powderFor(grade);
            final double orePerRod = yieldMilli <= 0 ? 0 : 16.0 * 1000.0 / yieldMilli;
            try {
                GTValues.RA.stdBuilder()
                    .fluidInputs(water)
                    .itemOutputs(new ItemStack(powder, count))
                    // duration 1, not 0, to match the coolant page: GT's builder treats a
                    // non-positive duration as a malformed recipe and the DisplayFrontend
                    // suppresses the number anyway, so 1 costs nothing and 0 risks the tab.
                    .duration(1)
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    .setNEIDesc(
                        BTSWaterGrade.label(grade),
                        "FUEL LINE - F-12 reconversion:",
                        "  yield x" + String.format("%.1f", yieldMilli / 1000.0) + " -> " + count + " UO2 powder",
                        "  per 1,000 L enriched UF6",
                        "  = " + String.format("%.1f", orePerRod) + " Uraninite per fuel rod",
                        "REACTOR - feedwater hatch:",
                        "  EU/t output x" + String.format("%.2f", BTSWaterGrade.outputMilli(grade) / 1000.0),
                        "Plain and distilled water",
                        "  run the reactor at x1.00 -",
                        "  no grade is ever required.",
                        "Yield spans x9.00, output",
                        "  x1.40: water buys CHEAP",
                        "  FUEL, never raw power.",
                        "Display only - not a recipe")
                    .addTo(plantWater);
            } catch (RuntimeException broken) {
                pageFailed("water grade", grade, broken);
            }
        }
    }

    private static String fmt(long value) {
        return String.format("%,d", value);
    }
}
