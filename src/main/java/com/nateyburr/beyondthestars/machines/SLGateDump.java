package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BTSBlackHoleSwap;
import com.nateyburr.beyondthestars.Config;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.NaniteTier;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import tectech.recipe.TecTechRecipeMaps;
import tectech.thing.CustomItemList;

/**
 * SINGULARITY LADDER test bench, {@code /btseye gates} (console or op): dumps the Spacetime
 * Compression Field Generator recipes the running server actually has (Condensate Assembler, after
 * the BHC to Kerr swap) and checks the gate rule of the RC-2 port at runtime - compression generator
 * tier index t is the Eye tier t+1 gate, so it must not need anything the Eye first makes at tier
 * t+1 or later (costs doc §9.4). Direct inputs, their nanite tiers and condensates are checked; an
 * input the Condensate Assembler itself makes (Resonance Chambers and the like) passes if at least
 * one of its recipes passes, one level deep. Also counts what still consumes or makes a Black Hole
 * Compressor controller. Read-only; op tooling, never reached in normal play.
 */
public final class SLGateDump {

    private SLGateDump() {}

    /**
     * The Eye's first-product ladder (D-42/D-43, costs doc §9.4) and what is derived from it, keyed
     * by a normalised name fragment (lower case, letters and digits only) matched against material,
     * item and fluid names. The table is the DECIDED ladder (D-48, accepted 2026-10-12): Eternity at
     * T7 because its QFT loop moves onto Ergomatter (proposal 1; not built yet, so until it is Eternity
     * really needs Universium, T10, and BTSRecipesReality's gate log says so), and MagMatter is not an
     * Eye product at all because its only T10 tie was the MHDCSM quest prerequisite D-48 proposal 2
     * drops (question B-67).
     */
    private static final Object[][] FIRST_MADE = { { "rawstarmatter", 1 }, { "boundlesscosmicsolder", 1 },
        { "primordialmatter", 1 }, { "excitedstellarplasma", 2 }, { "rawhyperdense", 3 },
        { "hyperdensestellarmixture", 3 }, { "whitedwarf", 4 }, { "blackdwarf", 5 }, { "magnetar", 6 },
        { "ergomatter", 7 }, { "eternity", 7 }, { "whitehole", 9 }, { "universium", 10 }, { "mhdcsm", 10 },
        { "magnetohydrodynamically", 10 } };

    public static void run(ICommandSender sender) {
        final RecipeMap<?> map = TecTechRecipeMaps.condensateAssemblingRecipes;
        SLConsoleTest.tell(
            sender,
            "== Gate dump: " + map.getAllRecipes()
                .size()
                + " Condensate Assembler recipes; replaceTecTechEyeOfHarmony="
                + Config.replaceTecTechEyeOfHarmony
                + ", removeBlackHoleCompressorRecipe="
                + Config.removeBlackHoleCompressorRecipe
                + " ==");
        int loops = 0;
        for (int t = 0; t < 9; t++) {
            final ItemStack gen = CustomItemList.valueOf("SpacetimeCompressionFieldGeneratorTier" + t)
                .get(1);
            final int gate = t + 1;
            final List<GTRecipe> recipes = producers(map, gen);
            if (recipes.isEmpty()) {
                SLConsoleTest.tell(sender, "Compression gen t" + t + " (Eye T" + gate + " gate): NO RECIPE");
                loops++;
                continue;
            }
            int index = 0;
            for (GTRecipe recipe : recipes) {
                index++;
                SLConsoleTest.tell(
                    sender,
                    "Compression gen t" + t
                        + " (Eye T"
                        + gate
                        + " gate) recipe "
                        + index
                        + "/"
                        + recipes.size()
                        + ": "
                        + recipe.mEUt
                        + " EU/t, "
                        + recipe.mDuration
                        + " t");
                SLConsoleTest.tell(sender, "  in: " + inputs(recipe));
                SLConsoleTest.tell(sender, "  condensates: " + condensates(recipe));
                final List<String> bad = violations(recipe, gate);
                // One level down: an input the Condensate Assembler makes passes if one of its recipes does.
                for (ItemStack input : recipe.mInputs) {
                    if (input == null || matches(input, gen)) continue;
                    final List<GTRecipe> sub = producers(map, input);
                    if (sub.isEmpty()) continue;
                    String firstBad = null;
                    boolean anyClean = false;
                    for (GTRecipe s : sub) {
                        final List<String> subBad = violations(s, gate);
                        if (subBad.isEmpty()) anyClean = true;
                        else if (firstBad == null) firstBad = String.join(", ", subBad);
                    }
                    SLConsoleTest.tell(
                        sender,
                        "  via " + name(input)
                            + ": "
                            + sub.size()
                            + " recipe(s), "
                            + (anyClean ? "a clean one exists" : "ALL need later products")
                            + (firstBad == null ? "" : " (another needs: " + firstBad + ")"));
                    if (!anyClean) bad.add(name(input) + " [" + firstBad + "]");
                }
                if (bad.isEmpty()) {
                    SLConsoleTest.tell(sender, "  gate rule: CLEAN for Eye T" + gate);
                } else {
                    loops++;
                    SLConsoleTest
                        .tell(sender, "  gate rule: LOOP for Eye T" + gate + " - needs " + String.join("; ", bad));
                }
            }
        }
        final ItemStack bhc = ItemList.Machine_Multi_BlackHoleCompressor.get(1);
        final ItemStack kerr = BTSMachines.kerrEngine;
        SLConsoleTest.tell(
            sender,
            "== Gate dump done: " + loops
                + " generator recipe(s) break the gate rule. BHC controller consumers left: "
                + BTSBlackHoleSwap.countConsumers(bhc)
                + "; recipes making a BHC controller: "
                + makersOf(bhc)
                + "; Kerr controller consumers: "
                + (kerr == null ? "n/a" : String.valueOf(BTSBlackHoleSwap.countConsumers(kerr)))
                + " ==");
    }

    private static List<GTRecipe> producers(RecipeMap<?> map, ItemStack output) {
        final List<GTRecipe> out = new ArrayList<>();
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (recipe == null || recipe.mOutputs == null) continue;
            for (ItemStack stack : recipe.mOutputs) {
                if (matches(stack, output)) {
                    out.add(recipe);
                    break;
                }
            }
        }
        return out;
    }

    /** GT recipe maps whose outputs include the stack (the BHC controller recipe's NEI pages included). */
    private static int makersOf(ItemStack stack) {
        int n = 0;
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            n += producers(map, stack).size();
        }
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (recipe != null && matches(recipe.mOutput, stack)) n++;
        }
        return n;
    }

    private static boolean matches(ItemStack a, ItemStack b) {
        return a != null && b != null
            && a.getItem() != null
            && a.getItem() == b.getItem()
            && a.getItemDamage() == b.getItemDamage();
    }

    private static String inputs(GTRecipe recipe) {
        final NaniteTier[] nanites = recipe.getMetadata(GTRecipeConstants.NANITE_TIERS);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recipe.mInputs.length; i++) {
            final ItemStack input = recipe.mInputs[i];
            if (sb.length() > 0) sb.append(", ");
            sb.append(input == null ? "-" : input.stackSize + "x " + name(input));
            if (nanites != null && i < nanites.length && nanites[i] != null) {
                sb.append(" [")
                    .append(nanites[i].name())
                    .append(']');
            }
        }
        return sb.toString();
    }

    private static String condensates(GTRecipe recipe) {
        final FluidStack[] fluids = fluids(recipe);
        final StringBuilder sb = new StringBuilder();
        for (FluidStack fluid : fluids) {
            if (fluid == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(fluid.amount)
                .append(" L ")
                .append(fluidName(fluid));
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private static FluidStack[] fluids(GTRecipe recipe) {
        final FluidStack[] condensate = recipe.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
        if (condensate != null && condensate.length > 0) return condensate;
        return recipe.mFluidInputs == null ? new FluidStack[0] : recipe.mFluidInputs;
    }

    /** What in this recipe the Eye first makes at {@code gate} or later (direct inputs only). */
    private static List<String> violations(GTRecipe recipe, int gate) {
        final Set<String> bad = new LinkedHashSet<>();
        final NaniteTier[] nanites = recipe.getMetadata(GTRecipeConstants.NANITE_TIERS);
        for (int i = 0; i < recipe.mInputs.length; i++) {
            final ItemStack input = recipe.mInputs[i];
            if (input != null) check(bad, gate, name(input), keys(input));
            if (nanites != null && i < nanites.length && nanites[i] != null) {
                check(
                    bad,
                    gate,
                    nanites[i].name() + " nanites",
                    nanites[i].getMaterial() == null ? nanites[i].name() : nanites[i].getMaterial().mName);
            }
        }
        for (FluidStack fluid : fluids(recipe)) {
            if (fluid == null || fluid.getFluid() == null) continue;
            check(
                bad,
                gate,
                fluidName(fluid),
                fluid.getFluid()
                    .getName() + " "
                    + fluidName(fluid));
        }
        return new ArrayList<>(bad);
    }

    private static void check(Set<String> bad, int gate, String label, String keys) {
        final String norm = normalise(keys);
        for (Object[] entry : FIRST_MADE) {
            final int tier = (Integer) entry[1];
            if (tier >= gate && norm.contains((String) entry[0])) {
                bad.add(label + " (Eye T" + tier + ")");
                return;
            }
        }
    }

    private static String keys(ItemStack stack) {
        final StringBuilder sb = new StringBuilder(name(stack));
        try {
            sb.append(' ')
                .append(stack.getUnlocalizedName());
        } catch (RuntimeException ignored) {
            // unnamed
        }
        try {
            final ItemData data = GTOreDictUnificator.getAssociation(stack);
            if (data != null && data.mMaterial != null && data.mMaterial.mMaterial != null) {
                sb.append(' ')
                    .append(data.mMaterial.mMaterial.mName);
            }
        } catch (RuntimeException ignored) {
            // no association
        }
        return sb.toString();
    }

    private static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private static String name(ItemStack stack) {
        try {
            final String name = stack.getDisplayName();
            return name == null ? "?" : name.replaceAll("§.", "");
        } catch (RuntimeException unnamed) {
            return String.valueOf(stack.getItem()) + "@" + stack.getItemDamage();
        }
    }

    private static String fluidName(FluidStack fluid) {
        try {
            return fluid.getLocalizedName();
        } catch (RuntimeException unnamed) {
            return fluid.getFluid() == null ? "?"
                : fluid.getFluid()
                    .getName();
        }
    }
}
