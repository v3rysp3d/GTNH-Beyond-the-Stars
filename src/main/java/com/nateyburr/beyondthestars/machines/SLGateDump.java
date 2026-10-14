package com.nateyburr.beyondthestars.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BTSBlackHoleSwap;
import com.nateyburr.beyondthestars.BTSCircuitChecks;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing4;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.fluids.SLQuasarFluids;
import com.nateyburr.beyondthestars.items.SLCircuitItems;
import com.nateyburr.beyondthestars.items.SLQuasarItems;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.NaniteTier;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
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
 * Compressor controller. Then the P4 gate audit ({@link #p4}): every P4 part and the Eye tier it gates. Read-only; op
 * tooling, never reached in normal play.
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
     * drops (question B-80).
     */
    private static final Object[][] FIRST_MADE = { { "rawstarmatter", 1 }, { "boundlesscosmicsolder", 1 },
        { "primordialmatter", 1 }, { "excitedstellarplasma", 2 }, { "rawhyperdense", 3 },
        { "hyperdensestellarmixture", 3 }, { "whitedwarf", 4 }, { "blackdwarf", 5 }, { "magnetar", 6 },
        { "ergomatter", 7 }, { "eternity", 7 }, { "whitehole", 9 }, { "universium", 10 }, { "mhdcsm", 10 },
        { "magnetohydrodynamically", 10 },
        // Circuits v2 (circuits-v2.md §3.1): our circuit items carry the Eye tier of the newest matter inside them.
        { "stellarboule", 1 }, { "stellarwafer", 1 }, { "sidereal", 1 }, { "stellarsmd", 2 }, { "empyrean", 4 },
        { "omegawafer", 5 }, { "omegachip", 5 }, { "omegaprocessor", 5 }, { "omegaassembly", 5 },
        { "omegasupercomputer", 5 }, { "omegamainframe", 5 } };

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
        final int p4Loops = p4(sender);
        final int circuitProblems = circuits(sender);
        final ItemStack bhc = ItemList.Machine_Multi_BlackHoleCompressor.get(1);
        final ItemStack kerr = BTSMachines.kerrEngine;
        SLConsoleTest.tell(
            sender,
            "== Gate dump done: " + loops
                + " generator recipe(s) and "
                + p4Loops
                + " P4 part(s) break the gate rule; circuits: "
                + circuitProblems
                + " problem(s). BHC controller consumers left: "
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

    // ================================================================================== P4 (quasar-p4.md §6.4)

    /** One P4 part to check: what it is, the Eye tier it gates (it may not need anything first made there or later). */
    private static final class Gate {

        final String label;
        final ItemStack item;
        final FluidStack fluid;
        final int gate;

        Gate(String label, ItemStack item, FluidStack fluid, int gate) {
            this.label = label;
            this.item = item;
            this.fluid = fluid;
            this.gate = gate;
        }
    }

    /** A recipe from any map or the assembly lines, flattened: items (first ore-dict alternative), nanites, fluids. */
    private static final class Rec {

        final String where;
        final List<ItemStack> items = new ArrayList<>();
        NaniteTier[] nanites;
        FluidStack[] fluids;

        Rec(String where) {
            this.where = where;
        }
    }

    /**
     * The P4 gate audit (quasar-p4.md §6.4-§6.5): every P4 part, the tier it gates, and whether any recipe that makes
     * it is clean - its direct inputs, nanite tiers and fluids, and one level down (an input passes if at least one of
     * its own recipes is clean). The tier is the Eye tier whose unlock (or first build) needs the part: the quasar, the
     * conduit, the crowns and the T6 -> T7 core are needed before T7 (gate 7); the T7 -> T8 core, the T8 chart, Throat
     * Mk III, Resonance Chamber IV and the Eternity catalyst before T8 (gate 8); Superdense Ergomatter before T9. The
     * optional Disc Field Coil T4 (gate 0) is reported, not judged. Returns how many parts break the rule.
     */
    private static int p4(ICommandSender sender) {
        final List<Gate> gates = new ArrayList<>();
        gates.add(new Gate("Quasar Engine (28120)", BTSMachines.quasarEngine, null, 7));
        gates.add(new Gate("Wormhole Mouth (28121)", BTSMachines.wormholeMouth, null, 7));
        gates.add(new Gate("Conduit Mirror (28122)", BTSMachines.conduitMirror, null, 7));
        gates.add(new Gate("Conduit Junction (28123)", BTSMachines.conduitJunction, null, 7));
        gates.add(new Gate("Burst Thermaliser Crown (28124)", BTSMachines.burstThermaliserCrown, null, 7));
        gates.add(new Gate("GRB Receptor Port (28125)", BTSMachines.grbReceptorPort, null, 7));
        gates.add(new Gate("Burst Control Hatch (28126)", BTSMachines.burstControlHatch, null, 7));
        for (int t = 1; t <= 4; t++) {
            gates.add(
                new Gate(
                    "Disc Field Coil T" + t,
                    SLBlocks.casing2(BlockSLCasing2.discCoil(t), 1),
                    null,
                    t == 4 ? 0 : 7));
        }
        gates.add(new Gate("Eddington Wall Casing", SLBlocks.casing2(BlockSLCasing2.EDDINGTON_WALL, 1), null, 7));
        gates.add(new Gate("Rim Gas Injector", SLBlocks.casing2(BlockSLCasing2.RIM_GAS_INJECTOR, 1), null, 7));
        gates.add(new Gate("Jet Collimator Ring", SLBlocks.casing2(BlockSLCasing2.JET_COLLIMATOR, 1), null, 7));
        gates.add(new Gate("Burst Thermaliser Casing", SLBlocks.casing2(BlockSLCasing2.BURST_THERMALISER, 1), null, 7));
        gates.add(new Gate("Radiator Petal", SLBlocks.casing2(BlockSLCasing2.RADIATOR_PETAL, 1), null, 7));
        gates.add(item("Quasar Seed", SLQuasarItems.quasarSeed, 7));
        gates.add(item("Conduit Throat Mk I", SLQuasarItems.conduitThroatMk1, 7));
        gates.add(item("Conduit Throat Mk II", SLQuasarItems.conduitThroatMk2, 7));
        gates.add(item("Intergalactic Upgrade Core (T6 -> T7)", SLQuasarItems.intergalacticUpgradeCore, 7));
        gates.add(item("Star Chart: Triangulum (T7)", SLQuasarItems.starChartTriangulum, 7));
        gates.add(item("Conduit Throat Mk III", SLQuasarItems.conduitThroatMk3, 8));
        gates.add(item("Supermassive Seed Core (T7 -> T8)", SLQuasarItems.supermassiveSeedCore, 8));
        gates.add(item("Star Chart: Milky Way (T8)", SLQuasarItems.starChartMilkyWay, 8));
        gates.add(
            new Gate("MetaMaterial Resonance Chamber IV", ItemList.MetaMaterial_ResonanceChamber4.get(1), null, 8));
        gates.add(
            new Gate(
                "Temporal Harmony Catalyst (Eternity's QFT)",
                GregtechItemList.TemporalHarmonyCatalyst.get(1),
                null,
                8));
        gates.add(
            new Gate(
                "Superdense Ergomatter (refined)",
                null,
                SLQuasarFluids.superdenseErgomatter == null ? null
                    : new FluidStack(SLQuasarFluids.superdenseErgomatter, 1),
                9));
        // SMBH v1 (smbh-v1.md §1.3): the L1 lattice is the Eye T9 -> T10 gate (the Supermassive Link), so its frame and
        // node must not need anything first made at T10; L2-L4, the Mk IV crown casing and throat are reported only
        gates.add(new Gate("Lattice Frame Casing", SLBlocks.casing4(BlockSLCasing4.LATTICE_FRAME, 1), null, 10));
        gates.add(new Gate("Containment Node L1", SLBlocks.casing4(BlockSLCasing4.NODE_L1, 1), null, 10));
        for (int t = 2; t <= 4; t++) {
            gates.add(new Gate("Containment Node L" + t, SLBlocks.casing4(BlockSLCasing4.node(t), 1), null, 0));
        }
        gates.add(
            new Gate(
                "Event Horizon Thermaliser Casing",
                SLBlocks.casing4(BlockSLCasing4.HORIZON_THERMALISER, 1),
                null,
                0));
        gates.add(item("Conduit Throat Mk IV", SLQuasarItems.conduitThroatMk4, 0));
        SLConsoleTest.tell(sender, "== P4 gate audit: " + gates.size() + " parts ==");
        int loops = 0;
        for (Gate gate : gates) {
            if (gate.item == null && gate.fluid == null) {
                SLConsoleTest.tell(sender, "P4 " + gate.label + ": not registered in this build (its MTE unit)");
                continue;
            }
            final List<Rec> recs = gate.item != null ? makers(gate.item) : makers(gate.fluid);
            if (recs.isEmpty()) {
                SLConsoleTest.tell(sender, "P4 " + gate.label + ": NO RECIPE");
                loops++;
                continue;
            }
            String verdict = null;
            final List<String> notes = new ArrayList<>();
            for (Rec rec : recs) {
                final List<String> bad = gate.gate == 0 ? new ArrayList<>() : violations(rec, gate.gate);
                if (gate.gate > 0) {
                    for (ItemStack input : rec.items) {
                        if (input == null || (gate.item != null && matches(input, gate.item))) continue;
                        final List<Rec> sub = makers(input);
                        if (sub.isEmpty()) continue;
                        String firstBad = null;
                        boolean anyClean = false;
                        for (Rec s : sub) {
                            final List<String> subBad = violations(s, gate.gate);
                            if (subBad.isEmpty()) {
                                anyClean = true;
                                break;
                            }
                            if (firstBad == null) firstBad = String.join(", ", subBad);
                        }
                        if (!anyClean) bad.add(name(input) + " [" + firstBad + "]");
                    }
                }
                notes.add(rec.where + (bad.isEmpty() ? " clean" : " needs " + String.join("; ", bad)));
                if (bad.isEmpty() && verdict == null) verdict = rec.where;
            }
            final String head = "P4 " + gate.label
                + (gate.gate == 0 ? " (optional, reported only)" : " (gate Eye T" + gate.gate + ")");
            if (gate.gate == 0) {
                SLConsoleTest.tell(sender, head + ": " + recs.size() + " recipe(s)");
                for (Rec rec : recs) SLConsoleTest.tell(sender, "  in (" + rec.where + "): " + describe(rec));
            } else if (verdict != null) {
                SLConsoleTest.tell(sender, head + ": CLEAN via " + verdict);
            } else {
                loops++;
                SLConsoleTest.tell(sender, head + ": LOOP - " + String.join(" | ", notes));
                for (Rec rec : recs) SLConsoleTest.tell(sender, "  in (" + rec.where + "): " + describe(rec));
            }
        }
        SLConsoleTest.tell(sender, "== P4 gate audit done: " + loops + " part(s) break the gate rule ==");
        return loops;
    }

    private static Gate item(String label, Item item, int gate) {
        return new Gate(label, item == null ? null : new ItemStack(item, 1), null, gate);
    }

    /** Every recipe (any map, any assembly line) whose outputs include {@code stack}; NEI-only maps skipped. */
    private static List<Rec> makers(ItemStack stack) {
        final List<Rec> out = new ArrayList<>();
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            if (skipMap(entry.getKey())) continue;
            for (GTRecipe recipe : entry.getValue()
                .getAllRecipes()) {
                if (recipe == null || recipe.mFakeRecipe || recipe.mOutputs == null) continue;
                for (ItemStack o : recipe.mOutputs) {
                    if (matches(o, stack)) {
                        out.add(rec(entry.getKey(), recipe));
                        break;
                    }
                }
            }
        }
        for (GTRecipe.RecipeAssemblyLine line : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (line == null || !matches(line.mOutput, stack)) continue;
            final Rec rec = new Rec("assembly line");
            for (int i = 0; i < line.mInputs.length; i++) {
                ItemStack in = line.mInputs[i];
                if (line.mOreDictAlt != null && i < line.mOreDictAlt.length
                    && line.mOreDictAlt[i] != null
                    && line.mOreDictAlt[i].length > 0) in = stockFirst(line.mOreDictAlt[i]);
                rec.items.add(in);
            }
            rec.fluids = line.mFluidInputs;
            out.add(rec);
        }
        return out;
    }

    /**
     * The alternative of an ore-dict slot to judge: a stock one when the slot has any. TecTech sorts alternatives by
     * mod id, so our circuits (circuits-v2.md §1.3, "beyondthestars") come first, but a slot that also takes stock
     * circuits never needs ours: the gate rule is about whether a clean route exists.
     */
    private static ItemStack stockFirst(ItemStack[] alts) {
        for (ItemStack alt : alts) {
            if (alt != null && !BTSCircuitChecks.isOurs(alt.getItem())) return alt;
        }
        return alts[0];
    }

    /** Every recipe whose fluid outputs include {@code fluid}'s fluid; NEI-only maps skipped. */
    private static List<Rec> makers(FluidStack fluid) {
        final List<Rec> out = new ArrayList<>();
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            if (skipMap(entry.getKey())) continue;
            for (GTRecipe recipe : entry.getValue()
                .getAllRecipes()) {
                if (recipe == null || recipe.mFakeRecipe || recipe.mFluidOutputs == null) continue;
                for (FluidStack o : recipe.mFluidOutputs) {
                    if (o != null && o.getFluid() == fluid.getFluid()) {
                        out.add(rec(entry.getKey(), recipe));
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** NEI-only and recycling maps say nothing about how a part is first made. */
    private static boolean skipMap(String name) {
        final String n = name.toLowerCase(Locale.ROOT);
        return n.contains("fake") || n.contains("visual")
            || n.contains("scanner")
            || n.contains("research")
            || n.contains("disassembl")
            || n.contains("recycl")
            || n.contains("macerator")
            || n.contains("arcfurnace")
            || n.contains("extractor");
    }

    private static Rec rec(String where, GTRecipe recipe) {
        final Rec rec = new Rec(where);
        if (recipe.mInputs != null) Collections.addAll(rec.items, recipe.mInputs);
        rec.nanites = recipe.getMetadata(GTRecipeConstants.NANITE_TIERS);
        rec.fluids = fluids(recipe);
        return rec;
    }

    private static List<String> violations(Rec rec, int gate) {
        final Set<String> bad = new LinkedHashSet<>();
        for (int i = 0; i < rec.items.size(); i++) {
            final ItemStack input = rec.items.get(i);
            if (input != null) check(bad, gate, name(input), keys(input));
            if (rec.nanites != null && i < rec.nanites.length && rec.nanites[i] != null) {
                final NaniteTier n = rec.nanites[i];
                check(bad, gate, n.name() + " nanites", n.getMaterial() == null ? n.name() : n.getMaterial().mName);
            }
        }
        if (rec.fluids != null) {
            for (FluidStack fluid : rec.fluids) {
                if (fluid == null || fluid.getFluid() == null) continue;
                check(
                    bad,
                    gate,
                    fluidName(fluid),
                    fluid.getFluid()
                        .getName() + " "
                        + fluidName(fluid));
            }
        }
        return new ArrayList<>(bad);
    }

    private static String describe(Rec rec) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rec.items.size(); i++) {
            final ItemStack input = rec.items.get(i);
            if (sb.length() > 0) sb.append(", ");
            sb.append(input == null ? "-" : input.stackSize + "x " + name(input));
            if (rec.nanites != null && i < rec.nanites.length && rec.nanites[i] != null) {
                sb.append(" [")
                    .append(rec.nanites[i].name())
                    .append(']');
            }
        }
        if (rec.fluids != null) {
            for (FluidStack fluid : rec.fluids) {
                if (fluid == null) continue;
                sb.append("; ")
                    .append(fluid.amount)
                    .append(" L ")
                    .append(fluidName(fluid));
            }
        }
        return sb.toString();
    }

    // ======================================================================== circuits (circuits-v2.md §3.1)

    /**
     * Circuits v2 gate section (circuits-v2.md §3.1, revision 3): re-proves the unlock table at boot. For each of our
     * items, every recipe that makes it is judged against its unlock tier (it may not need anything the Eye first makes
     * after that tier; one level down as above), and the gate items must really carry their tier's matter: the Stellar
     * Boule Raw Star Matter (T1), the Stellar SMDs ESPM (T2), the Sidereal / Empyrean / Omega Wafers RSM (T1) / WDM
     * (T4) / BDM (T5). Then, with no special casing, no SL card outside the chain may name one of our items. Returns
     * the number of problems.
     */
    private static int circuits(ICommandSender sender) {
        if (!Config.stellarCircuits) {
            SLConsoleTest.tell(sender, "== Circuits gate audit: stellarCircuits=false, nothing to check ==");
            return 0;
        }
        final Object[][] items = { { "Stellar Boule", SLCircuitItems.stellarBoule, 1, true },
            { "Stellar Wafer", SLCircuitItems.stellarWafer, 1, false },
            { "Stellar SMD Transistor", SLCircuitItems.stellarSmdTransistor, 2, true },
            { "Stellar SMD Resistor", SLCircuitItems.stellarSmdResistor, 2, true },
            { "Stellar SMD Capacitor", SLCircuitItems.stellarSmdCapacitor, 2, true },
            { "Stellar SMD Diode", SLCircuitItems.stellarSmdDiode, 2, true },
            { "Stellar SMD Inductor", SLCircuitItems.stellarSmdInductor, 2, true },
            { "Sidereal Wafer", SLCircuitItems.siderealWafer, 1, true },
            { "Sidereal Chip", SLCircuitItems.siderealChip, 1, false },
            { "Sidereal Circuit Board", SLCircuitItems.siderealCircuitBoard, 1, false },
            { "Sidereal Processor", SLCircuitItems.siderealProcessor, 1, false },
            { "Sidereal Assembly", SLCircuitItems.siderealAssembly, 1, false },
            { "Sidereal Supercomputer", SLCircuitItems.siderealSupercomputer, 1, false },
            { "Sidereal Mainframe", SLCircuitItems.siderealMainframe, 1, false },
            { "Empyrean Wafer", SLCircuitItems.empyreanWafer, 4, true },
            { "Empyrean Chip", SLCircuitItems.empyreanChip, 4, false },
            { "Empyrean Circuit Board", SLCircuitItems.empyreanCircuitBoard, 4, false },
            { "Empyrean Processor", SLCircuitItems.empyreanProcessor, 4, false },
            { "Empyrean Assembly", SLCircuitItems.empyreanAssembly, 4, false },
            { "Empyrean Supercomputer", SLCircuitItems.empyreanSupercomputer, 4, false },
            { "Empyrean Mainframe", SLCircuitItems.empyreanMainframe, 4, false },
            { "Omega Wafer", SLCircuitItems.omegaWafer, 5, true }, { "Omega Chip", SLCircuitItems.omegaChip, 5, false },
            { "Omega Circuit Board", SLCircuitItems.omegaCircuitBoard, 5, false },
            { "Omega Processor", SLCircuitItems.omegaProcessor, 5, false },
            { "Omega Assembly", SLCircuitItems.omegaAssembly, 5, false },
            { "Omega Supercomputer", SLCircuitItems.omegaSupercomputer, 5, false },
            { "Omega Mainframe", SLCircuitItems.omegaMainframe, 5, false } };
        SLConsoleTest.tell(sender, "== Circuits gate audit: " + items.length + " items ==");
        int problems = 0;
        for (Object[] row : items) {
            final String label = (String) row[0];
            final Item item = (Item) row[1];
            final int unlock = (Integer) row[2];
            final boolean carries = (Boolean) row[3];
            if (item == null) {
                SLConsoleTest.tell(sender, "Circuits " + label + ": NOT REGISTERED");
                problems++;
                continue;
            }
            final ItemStack stack = new ItemStack(item, 1);
            final List<Rec> recs = makers(stack);
            if (recs.isEmpty()) {
                SLConsoleTest.tell(sender, "Circuits " + label + ": NO RECIPE");
                problems++;
                continue;
            }
            String clean = null;
            int newest = 0;
            final List<String> notes = new ArrayList<>();
            for (Rec rec : recs) {
                final List<String> bad = violations(rec, unlock + 1);
                for (ItemStack input : rec.items) {
                    if (input == null || matches(input, stack)) continue;
                    final List<Rec> sub = makers(input);
                    if (sub.isEmpty()) continue;
                    String firstBad = null;
                    boolean anyClean = false;
                    for (Rec s : sub) {
                        final List<String> subBad = violations(s, unlock + 1);
                        if (subBad.isEmpty()) {
                            anyClean = true;
                            break;
                        }
                        if (firstBad == null) firstBad = String.join(", ", subBad);
                    }
                    if (!anyClean) bad.add(name(input) + " [" + firstBad + "]");
                }
                newest = Math.max(newest, newest(rec));
                notes.add(rec.where + (bad.isEmpty() ? " clean" : " needs " + String.join("; ", bad)));
                if (bad.isEmpty() && clean == null) clean = rec.where;
            }
            final boolean tierOk = !carries || newest == unlock;
            if (clean == null || !tierOk) problems++;
            SLConsoleTest.tell(
                sender,
                "Circuits " + label
                    + " (unlock Eye T"
                    + unlock
                    + "): "
                    + (clean == null ? "LOOP - " + String.join(" | ", notes) : "CLEAN via " + clean)
                    + "; newest Eye matter inside T"
                    + newest
                    + (carries ? (tierOk ? " (the gate holds)" : " (WRONG: should be T" + unlock + ")") : ""));
        }
        final List<String> naming = BTSCircuitChecks.cardsNamingOurs();
        for (String line : naming) SLConsoleTest.tell(sender, "Circuits: SL card NAMES ours: " + line);
        problems += naming.size();
        SLConsoleTest.tell(
            sender,
            "== Circuits gate audit done: " + problems
                + " problem(s); SL cards naming our circuit items outside the chain: "
                + naming.size()
                + " ==");
        return problems;
    }

    /** The newest Eye tier among a recipe's direct inputs (items, nanites, fluids), by {@link #FIRST_MADE}. */
    private static int newest(Rec rec) {
        int max = 0;
        final List<String> keys = new ArrayList<>();
        for (int i = 0; i < rec.items.size(); i++) {
            final ItemStack input = rec.items.get(i);
            if (input != null) keys.add(keys(input));
            if (rec.nanites != null && i < rec.nanites.length && rec.nanites[i] != null) {
                final NaniteTier n = rec.nanites[i];
                keys.add(n.getMaterial() == null ? n.name() : n.getMaterial().mName);
            }
        }
        if (rec.fluids != null) {
            for (FluidStack fluid : rec.fluids) {
                if (fluid == null || fluid.getFluid() == null) continue;
                keys.add(
                    fluid.getFluid()
                        .getName() + " "
                        + fluidName(fluid));
            }
        }
        for (String k : keys) {
            final String norm = normalise(k);
            for (Object[] entry : FIRST_MADE) {
                if (norm.contains((String) entry[0])) max = Math.max(max, (Integer) entry[1]);
            }
        }
        return max;
    }
}
