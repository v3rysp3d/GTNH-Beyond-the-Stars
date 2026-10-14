package com.nateyburr.beyondthestars.machines.quasar;

import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BTSDisplayRecipes;
import com.nateyburr.beyondthestars.BTSRecipeGuard;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * P4: the two DISPLAY-ONLY NEI pages (quasar-p4.md §1-§2; the BTSDisplayRecipes pattern: {@code .fake()}, no EU or
 * duration line, every number read from the engine). Neither machine runs a recipe map.
 * <ul>
 * <li>{@link #QUASAR} "beyondthestars.recipe.quasar": one page per feed gas (Hydrogen, Helium, Raw Star Matter) to
 * the Gamma-Ray Burst display item, with its opacity, the Eddington feed at 64 and 1,024 SM, the coil power and the
 * jet split.</li>
 * <li>{@link #THERMALISER} "beyondthestars.recipe.thermaliser": one page per crown Mk, bursts in, the cap and the EU/t
 * to wireless.</li>
 * </ul>
 * Contract for the Quasar and crown MTE units: return the map from {@code getAvailableRecipeMaps()} (and nothing from
 * {@code getRecipeMap()}), so NEI lists the controller as the page's catalyst. Filled from {@code BTSRecipesQuasar}
 * (guard line "quasar", postInit, before NEI builds its handlers).
 */
public final class QuasarRecipeMaps {

    private QuasarRecipeMaps() {}

    /** Feed gas -> bursts (the accretion disc). */
    public static final RecipeMap<RecipeMapBackend> QUASAR = RecipeMapBuilder.of("beyondthestars.recipe.quasar")
        .maxIO(0, 1, 1, 0)
        .minInputs(0, 1)
        .frontend(BTSDisplayRecipes.DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(200))
        .build();

    /** Bursts -> EU to wireless (the Burst Thermaliser Crown, Mk I / II / III / IV). */
    public static final RecipeMap<RecipeMapBackend> THERMALISER = RecipeMapBuilder
        .of("beyondthestars.recipe.thermaliser")
        .maxIO(1, 0, 0, 0)
        .minInputs(1, 0)
        .frontend(BTSDisplayRecipes.DisplayFrontend::new)
        .neiHandlerInfo(builder -> builder.setHeight(180))
        .build();

    private static final String[] MK = { "", "I", "II", "III", "IV" };
    /** Radiator Petals per crown Mk (manifest.json crown_mk1..4, char R). */
    private static final int[] PETALS = { 0, 643, 1_428, 2_803, 4_599 };

    /** Registers both pages (each page under the recipe guard, so a missing fluid costs one page). */
    public static void register() {
        for (QuasarGas gas : QuasarGas.values()) {
            BTSRecipeGuard.recipe("SL-Q27 " + gas.key, () -> {
                final FluidStack fluid = BTSRecipeGuard.nn(gasFluid(gas), gas.displayName + " fluid");
                GTValues.RA.stdBuilder()
                    .fluidInputs(fluid)
                    .itemOutputs(grb())
                    .duration(20)
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    .setNEIDesc(
                        "Feed gas: " + gas.displayName,
                        "Opacity k " + gas.kappa,
                        "Eddington feed 40 M/k L/s:",
                        "  64 SM: " + fmt(QuasarParams.eddingtonFeed(64, gas)) + " L/s",
                        "  1,024 SM: " + fmt(QuasarParams.eddingtonFeed(1_024, gas)) + " L/s",
                        "Fed at f x that (default " + QuasarParams.F_DEFAULT + ")",
                        "Coils: 2 A UXV x b squared",
                        "Two jets, half the stream each",
                        "Crown: 1 Bu/t = 10^13 EU/t",
                        "Maxed 4,096 SM: about 9,796",
                        "  Bu/t = 9.8e16 EU/t (98",
                        "  quadrillion)",
                        "Display only - not a recipe")
                    .addTo(QUASAR);
            });
        }
        for (int mk = 1; mk <= 4; mk++) {
            final int m = mk;
            BTSRecipeGuard.recipe(
                "SL-Q28 Mk " + MK[m],
                () -> GTValues.RA.stdBuilder()
                    .itemInputs(grb())
                    .duration(20)
                    .eut(0)
                    .fake()
                    .ignoreCollision()
                    .setNEIDesc(
                        "Crown Mk " + MK[m] + ": " + fmt(BurstRouting.crownCap(m)) + " Bu/t cap",
                        "1 Bu/t = 10^13 EU/t (1 %)",
                        "At cap: " + BTSFormat.euSci(BurstRouting.crownEUt(BurstRouting.crownCap(m))) + " EU/t",
                        "  (" + BTSFormat.euWords(BurstRouting.crownEUt(BurstRouting.crownCap(m))) + ")",
                        "EU goes to your wireless",
                        "  network: no dynamo hatch",
                        PETALS[m] + (m == 4 ? " Radiator Petals, Event" : " Radiator Petals"),
                        m == 4 ? "  Horizon Thermaliser core" : "  (Thermaliser Core inside)",
                        "A second crown stacked on",
                        "  top takes the overflow",
                        "Display only - not a recipe")
                    .addTo(THERMALISER));
        }
    }

    private static ItemStack grb() {
        return BTSRecipeGuard.stack(SLQuasarItems.gammaRayBurst, 1);
    }

    private static FluidStack gasFluid(QuasarGas gas) {
        switch (gas) {
            case H:
                return Materials.Hydrogen.getGas(1_000L);
            case HE:
                return Materials.Helium.getGas(1_000L);
            default:
                return Materials.RawStarMatter.getFluid(1_000L);
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    private static String sci(double v) {
        return String.format(Locale.ROOT, "%.1e", v);
    }
}
