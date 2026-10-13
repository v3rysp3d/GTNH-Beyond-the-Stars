package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.SLCircuitItems;
import com.nateyburr.beyondthestars.nac.BTSNacComponents;
import com.nateyburr.beyondthestars.nac.BTSNacPacketGuard;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.items.CircuitComponentFakeItem;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.metadata.BoardProcessingModuleFluidKey;
import gregtech.api.recipe.metadata.NanochipAssemblyMatrixTierKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitCalibration;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponentPacket;

/**
 * CIRCUITS v2 §4.5 (design repo {@code designs/docs/circuits-v2.md}): <b>the NAC route for our circuit parts,
 * SL-N01..N23</b>, in the NAC's existing module maps only (no module, no new map). <b>One owner: the circuits
 * unit.</b>
 * <table>
 * <tr>
 * <th>Ids</th>
 * <th>Module map</th>
 * <th>Recipe</th>
 * </tr>
 * <tr>
 * <td>SL-N01..N05</td>
 * <td>Part Processor</td>
 * <td>each Stellar SMD component to its processed one (GT's Optical SMD pattern), 20 t at UIV</td>
 * </tr>
 * <tr>
 * <td>SL-N06..N08</td>
 * <td>Cutting Chamber</td>
 * <td>each chip + 10 L Dimensionally Shifted Superfluid (GT's APIC pattern), 200 t at UMV</td>
 * </tr>
 * <tr>
 * <td>SL-N09..N11</td>
 * <td>Board Processor</td>
 * <td>each board; fluid key 4 for Sidereal (Prismatic Acid, as the Optical board), 5 for Empyrean / Omega (UU, as the
 * Pico board); a hidden recipe plus a {@code .fake()} NEI twin, as the coremod does; 600 t UEV / 800 t UMV / 800 t
 * UMV</td>
 * </tr>
 * <tr>
 * <td>SL-N12..N23</td>
 * <td>Assembly Matrix</td>
 * <td>the 12 circuits: the inputs of SL-X21..X32 with every item replaced by its processed component (ours, or GT's
 * for a stock part) or, for a circuit used inside the next one, its circuit component; same fluids, duration and EU/t
 * as the X recipe (open question 5: "same EU as the AL route"); matrix tier = the member's circuit tier as GT keys
 * its own lines (9 up to UIV, 10 UMV, 11 UXV, 12 MAX); calibration NONE (open question 4).</td>
 * </tr>
 * </table>
 * The matrix recipes are DERIVED from the live SL-X21..X32 recipes (Circuit Assembler / research Assembly Line, the
 * "circuits" line runs first), so the two routes can never drift apart.
 * <p>
 * <b>Gates.</b> Nothing registers unless the NAC patch is active ({@link BTSNacComponents#isActive()}; otherwise one
 * "NAC patch inactive" line, once), {@code stellarCircuits} is on (no NAC back door to disabled items) and
 * {@code nacCircuitComponents} is on. That key switches only this server-side recipe data, never the enum (§4.4).
 * <p>
 * <b>Conversions</b> (load-complete, {@link #onLoadComplete()}): the coremod builds the NAC's real &lt;-&gt; fake
 * conversion recipes from {@code CircuitComponent.VALUES} in its OWN load-complete pass (its scripts run there in
 * RC-2), so our 23 parts and circuits get theirs with no code of ours. This pass checks each one and adds any that is
 * missing; with the routes switched off it removes the coremod's real -&gt; fake conversions for our items (so the NAC
 * never pulls them in) and keeps fake -&gt; real (so stray components can still leave).
 */
public final class BTSRecipesNac {

    private BTSRecipesNac() {}

    private static final String FLAG = "nacCircuitComponents=true";

    /** The board processor's fluid keys and timings, Sidereal / Empyrean / Omega. */
    private static final int[] BOARD_KEY = { 4, 5, 5 }, BOARD_TICKS = { 600, 800, 800 };
    private static final long[] BOARD_EUT = { TierEU.RECIPE_UEV, TierEU.RECIPE_UMV, TierEU.RECIPE_UMV };

    /** What the last load-complete conversion pass found, for the checks. */
    private static int conversionsAdded, conversionsRemoved;

    // ============================================================================================== postInit

    /** Called from one line in {@link BTSRecipes} ("nac"), after the "circuits" line. */
    public static void register() {
        if (!BTSNacComponents.isActive()) {
            BTSNacComponents.reportInactive(
                "the enum extension never ran (the mixin was skipped or its injection point was not found)",
                null);
            return;
        }
        if (!Config.stellarCircuits) {
            BeyondTheStars.LOG.info("[BTS] NAC: stellarCircuits=false, no SL-N recipe registered.");
            return;
        }
        if (!Config.nacCircuitComponents) {
            BeyondTheStars.LOG.info(
                "[BTS] NAC: nacCircuitComponents=false, no SL-N recipe registered (the 34 NAC components still exist,"
                    + " so client and server agree).");
            return;
        }
        BTSRecipeLedger.gated(FLAG, () -> {
            parts();
            boards();
            matrix();
        });
    }

    /** SL-N01..N08: the Stellar SMDs (Part Processor) and the chips (Cutting Chamber). */
    private static void parts() {
        final List<CircuitComponent> cc = BTSNacComponents.parts(), pc = BTSNacComponents.processedParts();
        for (int i = 0; i < 5; i++) {
            final int k = i;
            BTSRecipeGuard.recipe(
                id(i + 1),
                () -> GTValues.RA.stdBuilder()
                    .itemInputs(
                        cc.get(k)
                            .getFakeStack(1))
                    .itemOutputs(
                        pc.get(k)
                            .getFakeStack(1))
                    .duration(20)
                    .eut(TierEU.RECIPE_UIV)
                    .addTo(RecipeMaps.nanochipPartProcessorRecipes));
        }
        for (int i = 5; i < 8; i++) {
            final int k = i;
            BTSRecipeGuard.recipe(
                id(i + 1),
                () -> GTValues.RA.stdBuilder()
                    .itemInputs(
                        cc.get(k)
                            .getFakeStack(1))
                    .fluidInputs(
                        BTSRecipeGuard.nn(
                            Materials.DimensionallyShiftedSuperfluid.getFluid(10L),
                            "Dimensionally Shifted Superfluid"))
                    .itemOutputs(
                        pc.get(k)
                            .getFakeStack(1))
                    .duration(200)
                    .eut(TierEU.RECIPE_UMV)
                    .addTo(RecipeMaps.nanochipCuttingChamber));
        }
    }

    /** SL-N09..N11: the boards (Board Processor), a hidden recipe plus its NEI twin, as the coremod does. */
    private static void boards() {
        final List<CircuitComponent> cc = BTSNacComponents.parts(), pc = BTSNacComponents.processedParts();
        for (int f = 0; f < 3; f++) {
            final int k = 8 + f, fam = f;
            BTSRecipeGuard.recipe(id(9 + f), () -> {
                GTValues.RA.stdBuilder()
                    .hidden()
                    .metadata(BoardProcessingModuleFluidKey.INSTANCE, BOARD_KEY[fam])
                    .itemInputs(
                        cc.get(k)
                            .getFakeStack(1))
                    .itemOutputs(
                        pc.get(k)
                            .getFakeStack(1))
                    .duration(BOARD_TICKS[fam])
                    .eut(BOARD_EUT[fam])
                    .addTo(RecipeMaps.nanochipBoardProcessorRecipes);
                final boolean prismatic = BOARD_KEY[fam] == 4;
                GTValues.RA.stdBuilder()
                    .fake()
                    .itemInputs(
                        cc.get(k)
                            .getFakeStack(1))
                    .fluidInputs(
                        BTSRecipeGuard.nn(
                            (prismatic ? Materials.PrismaticAcid : Materials.UUMatter).getFluid(0L),
                            prismatic ? "Prismatic Acid" : "UU-Matter"))
                    .fluidOutputs(
                        BTSRecipeGuard.nn(
                            (prismatic ? Materials.PrismaticGas : Materials.UUAmplifier).getFluid(0L),
                            prismatic ? "Prismatic Gas" : "UU-Amplifier"))
                    .itemOutputs(
                        pc.get(k)
                            .getFakeStack(1))
                    .duration(BOARD_TICKS[fam])
                    .eut(BOARD_EUT[fam])
                    .addTo(RecipeMaps.nanochipBoardProcessorRecipes);
            });
        }
    }

    /** SL-N12..N23: the 12 circuits in the Assembly Matrix, derived from the live SL-X21..X32. */
    private static void matrix() {
        for (int f = 0; f < SLCircuitItems.FAMILIES.length; f++) {
            for (int m = 0; m < SLCircuitItems.MEMBERS.length; m++) {
                final int fam = f, member = m;
                BTSRecipeGuard.recipe(id(12 + f * 4 + m), () -> {
                    final Item out = SLCircuitItems.circuit(fam, member);
                    final Source x = source(out);
                    final ItemStack[] inputs = new ItemStack[x.items.length];
                    for (int i = 0; i < inputs.length; i++) inputs[i] = component(x.items[i]);
                    final FluidStack[] fluids = new FluidStack[x.fluids.length];
                    for (int i = 0; i < fluids.length; i++) fluids[i] = x.fluids[i].copy();
                    GTValues.RA.stdBuilder()
                        .metadata(NanochipAssemblyMatrixTierKey.INSTANCE, matrixTier(fam, member))
                        .metadata(GTRecipeConstants.CIRCUIT_CALIBRATION_TYPE, CircuitCalibration.NONE)
                        .itemInputs(inputs)
                        .fluidInputs(fluids)
                        .itemOutputs(
                            BTSNacComponents.circuit(fam, member)
                                .getFakeStack(1))
                        .duration(x.duration)
                        .eut(x.eut)
                        .addTo(RecipeMaps.nanochipAssemblyMatrixRecipes);
                });
            }
        }
    }

    /** GT's convention (§4.5): Optical up to UIV at 9, Piko (UMV) 10, Quantum (UXV) 11, Planck (MAX) 12. */
    public static int matrixTier(int family, int member) {
        return Math.max(9, SLCircuitItems.tierIndex(family, member) + 7);
    }

    /** The SL-X recipe that makes {@code out}: inputs, fluids, duration, EU/t. */
    private static final class Source {

        ItemStack[] items;
        FluidStack[] fluids;
        int duration;
        long eut;
    }

    private static Source source(Item out) {
        final Source s = new Source();
        // the Processors: Circuit Assembler (SL-X21 / X25 / X29)
        for (GTRecipe r : RecipeMaps.circuitAssemblerRecipes.getAllRecipes()) {
            if (r == null || r.mFakeRecipe || r.mOutputs == null || r.mOutputs.length == 0) continue;
            if (r.mOutputs[0] == null || r.mOutputs[0].getItem() != out) continue;
            s.items = r.mInputs;
            s.fluids = r.mFluidInputs == null ? new FluidStack[0] : r.mFluidInputs;
            s.duration = r.mDuration;
            s.eut = r.mEUt;
            return s;
        }
        // the rest: research Assembly Line (SL-X22..X24, X26..X28, X30..X32)
        for (GTRecipe.RecipeAssemblyLine r : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (r == null || r.mOutput == null || r.mOutput.getItem() != out) continue;
            s.items = r.mInputs;
            s.fluids = r.mFluidInputs == null ? new FluidStack[0] : r.mFluidInputs;
            s.duration = r.mDuration;
            s.eut = r.mEUt;
            return s;
        }
        throw new IllegalStateException(
            "no SL-X recipe makes " + new ItemStack(out).getDisplayName() + " (the circuits line skipped it)");
    }

    /** The fake stack an SL-X input becomes in the NAC: processed part, circuit component, or GT's processed part. */
    private static ItemStack component(ItemStack in) {
        if (in == null || in.getItem() == null) throw new IllegalStateException("a null SL-X input");
        CircuitComponent cc = BTSNacComponents.processedFor(in.getItem());
        if (cc == null) cc = BTSNacComponents.forItem(in.getItem());
        if (cc == null) cc = stockProcessed(in);
        if (cc == null) {
            throw new IllegalStateException("no NAC circuit component for " + in.getDisplayName());
        }
        return cc.getFakeStack(in.stackSize);
    }

    /** GT's processed component whose unprocessed component's real item is {@code real} (item and meta), or null. */
    private static CircuitComponent stockProcessed(ItemStack real) {
        for (CircuitComponent pc : CircuitComponent.VALUES) {
            if (!pc.isProcessed || pc.componentForProcessed == null || BTSNacComponents.isOurs(pc)) continue;
            try {
                final CircuitComponent cc = pc.componentForProcessed.get();
                if (cc == null || cc.realComponent == null) continue;
                final ItemStack r = cc.realComponent.get();
                if (r != null && r.getItem() == real.getItem() && r.getItemDamage() == real.getItemDamage()) return pc;
            } catch (RuntimeException ignored) {
                // a GT component whose material is missing in this pack: not ours to fix, try the next
            }
        }
        return null;
    }

    private static String id(int n) {
        return String.format(Locale.ROOT, "SL-N%02d", n);
    }

    /** The circuit component an SL-N matrix id makes ("SL-N12".."SL-N23"), or null. */
    public static CircuitComponent matrixOutput(String id) {
        if (id == null || !id.startsWith("SL-N")) return null;
        final int n;
        try {
            n = Integer.parseInt(id.substring(4));
        } catch (NumberFormatException e) {
            return null;
        }
        if (n < 12 || n > 23) return null;
        return BTSNacComponents.circuit((n - 12) / 4, (n - 12) % 4);
    }

    // ========================================================================================= load-complete

    /** Called once from {@code CommonProxy.loadComplete}, after the coremod's scripts built the conversions. */
    public static void onLoadComplete() {
        if (!BTSNacComponents.isActive()) return;
        final boolean on = Config.nacCircuitComponents && Config.stellarCircuits;
        final RecipeMap<?> map = RecipeMaps.nanochipConversionRecipes;
        int both = 0;
        conversionsAdded = 0;
        conversionsRemoved = 0;
        final List<GTRecipe> doomed = new ArrayList<>();
        for (CircuitComponent cc : BTSNacComponents.withRealItem()) {
            final ItemStack real = cc.realComponent.get();
            if (real == null) continue;
            final List<GTRecipe> toFake = conversions(map, real, cc, true);
            final List<GTRecipe> toReal = conversions(map, real, cc, false);
            if (on) {
                if (toFake.isEmpty()) addConversion(cc, real, true);
                if (toReal.isEmpty()) addConversion(cc, real, false);
                if (!toFake.isEmpty() && !toReal.isEmpty()) both++;
            } else {
                doomed.addAll(toFake);
                if (toReal.isEmpty()) addConversion(cc, real, false);
            }
        }
        if (!doomed.isEmpty()) {
            map.getBackend()
                .removeRecipes(doomed);
            for (GTRecipe r : doomed) {
                BTSRecipeLedger.removed(
                    "SL-N00",
                    "Circuits",
                    "nacCircuitComponents=false",
                    "none: the NAC no longer takes our items in (fake -> real kept)",
                    "the coremod's NAC conversion of one of our items",
                    map,
                    r);
            }
            conversionsRemoved = doomed.size();
        }
        if (on) {
            BeyondTheStars.LOG.info(
                "[BTS] NAC conversions: {} of {} of our parts and circuits have the coremod's real -> fake and fake ->"
                    + " real recipes; {} missing one(s) added.",
                both,
                BTSNacComponents.withRealItem()
                    .size(),
                conversionsAdded);
        } else {
            BeyondTheStars.LOG.info(
                "[BTS] NAC conversions: our NAC route is off ({}, {}); {} real -> fake conversion(s) of our items"
                    + " removed, fake -> real kept ({} added).",
                "nacCircuitComponents=" + Config.nacCircuitComponents,
                "stellarCircuits=" + Config.stellarCircuits,
                conversionsRemoved,
                conversionsAdded);
        }
    }

    /** The conversion recipes real -> {@code cc} ({@code toFake}) or {@code cc} -> real. */
    private static List<GTRecipe> conversions(RecipeMap<?> map, ItemStack real, CircuitComponent cc, boolean toFake) {
        final List<GTRecipe> out = new ArrayList<>();
        final Collection<GTRecipe> all = map.getAllRecipes();
        for (GTRecipe r : all) {
            if (r == null || r.mInputs == null || r.mOutputs == null || r.mInputs.length != 1 || r.mOutputs.length != 1)
                continue;
            final ItemStack in = r.mInputs[0], o = r.mOutputs[0];
            if (in == null || o == null) continue;
            final ItemStack fakeSide = toFake ? o : in, realSide = toFake ? in : o;
            if (fakeSide.getItem() != CircuitComponentFakeItem.INSTANCE || fakeSide.getItemDamage() != cc.metaId)
                continue;
            if (realSide.getItem() == real.getItem() && realSide.getItemDamage() == real.getItemDamage()) out.add(r);
        }
        return out;
    }

    /** One missing conversion, as the coremod makes them (100 t, 0 EU). */
    private static void addConversion(CircuitComponent cc, ItemStack real, boolean toFake) {
        BTSRecipeGuard.recipe(
            "SL-N00",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(toFake ? real.copy() : cc.getFakeStack(1))
                .itemOutputs(toFake ? cc.getFakeStack(1) : real.copy())
                .duration(100)
                .eut(0)
                .addTo(RecipeMaps.nanochipConversionRecipes));
        conversionsAdded++;
        BeyondTheStars.LOG.info(
            "[BTS] NAC conversion added: {} {} {} (the coremod had none).",
            toFake ? real.getDisplayName() : cc.name(),
            "->",
            toFake ? cc.name() : real.getDisplayName());
    }

    // ================================================================================================ checks

    /**
     * The C5 checks for {@code /btsrecipes circuits} (spec §7.2): the enum, the conversions, the SL-N recipes, the
     * packet guard. Judged against the mode the server is in (patch active or not, routes on or off). Ends with one
     * PASS/FAIL line.
     */
    public static List<String> report() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        final boolean active = BTSNacComponents.isActive();
        final boolean on = active && Config.nacCircuitComponents && Config.stellarCircuits;
        final int total = CircuitComponent.VALUES.length, stock = BTSNacComponents.stockLength();
        int ours = 0;
        for (CircuitComponent cc : CircuitComponent.VALUES) if (BTSNacComponents.isOurs(cc)) ours++;
        lines.add(
            "patch " + (active ? "ACTIVE" : "INACTIVE (" + BTSNacComponents.failure() + ")")
                + "; CircuitComponent.VALUES.length = "
                + total
                + " (stock "
                + stock
                + " + ours "
                + ours
                + "); values().length = "
                + CircuitComponent.values().length
                + "; nacCircuitComponents="
                + Config.nacCircuitComponents
                + ", stellarCircuits="
                + Config.stellarCircuits);
        final int wantOurs = active ? BTSNacComponents.COUNT : 0;
        if (ours != wantOurs || total != stock + wantOurs || CircuitComponent.values().length != total) {
            bad++;
            lines.add("BAD  enum size: expected stock + " + wantOurs);
        }

        // lookups by meta and by name
        if (active) {
            final CircuitComponent byMeta = CircuitComponent.tryGetFromMetaId(10211);
            CircuitComponent byName = null;
            try {
                byName = CircuitComponent.valueOf("BTS_OmegaMainframe");
            } catch (IllegalArgumentException ignored) {}
            final boolean ok = byMeta != null && byMeta == byName && byMeta.realComponent != null;
            if (!ok) bad++;
            lines.add(
                (ok ? "ok   " : "BAD  ") + "tryGetFromMetaId(10211) = "
                    + (byMeta == null ? "null" : byMeta.name())
                    + ", valueOf(\"BTS_OmegaMainframe\") = "
                    + (byName == null ? "null" : byName.name())
                    + ", real item "
                    + (byMeta == null || byMeta.realComponent.get() == null ? "null"
                        : byMeta.realComponent.get()
                            .getDisplayName()));
            int metaBad = 0;
            for (CircuitComponent cc : CircuitComponent.VALUES) {
                if (BTSNacComponents.isOurs(cc) && CircuitComponent.tryGetFromMetaId(cc.metaId) != cc) metaBad++;
            }
            if (metaBad > 0) bad++;
            lines.add((metaBad == 0 ? "ok   " : "BAD  ") + "all 34 resolve by meta id (" + metaBad + " wrong)");
        } else {
            final boolean none = CircuitComponent.tryGetFromMetaId(10211) == null
                && !BTSNacPacketGuard.isKnown("BTS_OmegaMainframe");
            if (!none) bad++;
            lines.add((none ? "ok   " : "BAD  ") + "no BTS_ constant by meta 10211 or by name (stock enum)");
        }

        // conversions, both directions, for the 23 parts and circuits
        if (active) {
            int toFake = 0, toReal = 0;
            for (CircuitComponent cc : BTSNacComponents.withRealItem()) {
                final ItemStack real = cc.realComponent.get();
                if (real == null) continue;
                if (!conversions(RecipeMaps.nanochipConversionRecipes, real, cc, true).isEmpty()) toFake++;
                if (!conversions(RecipeMaps.nanochipConversionRecipes, real, cc, false).isEmpty()) toReal++;
            }
            final int n = BTSNacComponents.withRealItem()
                .size();
            final boolean ok = toReal == n && toFake == (on ? n : 0);
            if (!ok) bad++;
            lines.add(
                (ok ? "ok   " : "BAD  ") + "conversions: real -> fake "
                    + toFake
                    + " / "
                    + n
                    + " (want "
                    + (on ? n : 0)
                    + "), fake -> real "
                    + toReal
                    + " / "
                    + n
                    + "; this boot added "
                    + conversionsAdded
                    + ", removed "
                    + conversionsRemoved);
        }

        // SL-N recipes: registered and live, with the tier key, NONE calibration and X parity
        int live = 0, wrongTier = 0;
        for (BTSRecipeLedger.Added a : BTSRecipeLedger.allAdded()) {
            if (a.gt == null || !a.id.startsWith("SL-N") || "SL-N00".equals(a.id)) continue;
            if (a.map == null || !a.map.getAllRecipes()
                .contains(a.gt)) continue;
            live++;
            final CircuitComponent out = matrixOutput(a.id);
            if (out != null) {
                final int n = Integer.parseInt(a.id.substring(4)) - 12;
                final Integer tier = a.gt.getMetadata(NanochipAssemblyMatrixTierKey.INSTANCE);
                final Object cal = a.gt.getMetadata(GTRecipeConstants.CIRCUIT_CALIBRATION_TYPE);
                if (tier == null || tier != matrixTier(n / 4, n % 4) || (cal != null && cal != CircuitCalibration.NONE))
                    wrongTier++;
            }
        }
        final List<String> ids = new ArrayList<>();
        for (String id : BTSRecipeLedger.guardedIds()) if (id.startsWith("SL-N") && !"SL-N00".equals(id)) ids.add(id);
        final int wantIds = on ? 23 : 0;
        final boolean idsOk = ids.size() == wantIds && (on ? live >= 23 : live == 0) && wrongTier == 0;
        if (!idsOk) bad++;
        lines.add(
            (idsOk ? "ok   " : "BAD  ") + "SL-N recipes: "
                + ids.size()
                + " id(s) registered (want "
                + wantIds
                + "), "
                + live
                + " live recipe object(s), "
                + wrongTier
                + " with a wrong matrix tier or calibration");
        if (on) {
            int found = 0;
            for (int f = 0; f < 3; f++) {
                for (int m = 0; m < 4; m++) {
                    final String id = id(12 + f * 4 + m);
                    final GTRecipe r = lookupMatrix(id);
                    if (r != null) found++;
                    else lines.add("BAD  " + id + ": GT's findRecipeQuery does not find it with its own inputs");
                }
            }
            if (found != 12) bad++;
            lines.add((found == 12 ? "ok   " : "BAD  ") + "matrix lookup: " + found + " / 12 found by GT's query");
        }

        // the packet: an NBT round-trip, and a saved name the enum does not know
        bad += packetChecks(lines, active);

        lines.add((bad == 0 ? "PASS" : "FAIL") + " nac: " + bad + " problem(s)");
        return lines;
    }

    /** GT's own lookup with the recipe's inputs (the NAC module's {@code findRecipeQuery}), or null. */
    private static GTRecipe lookupMatrix(String id) {
        final CircuitComponent out = matrixOutput(id);
        if (out == null) return null;
        for (GTRecipe r : RecipeMaps.nanochipAssemblyMatrixRecipes.getAllRecipes()) {
            if (r == null || r.mFakeRecipe || r.mOutputs == null || r.mOutputs.length == 0) continue;
            if (CircuitComponent.tryGetFromFakeStack(r.mOutputs[0]) != out) continue;
            final ItemStack[] items = new ItemStack[r.mInputs.length];
            for (int i = 0; i < items.length; i++) items[i] = r.mInputs[i].copy();
            final FluidStack[] fluids = new FluidStack[r.mFluidInputs.length];
            for (int i = 0; i < fluids.length; i++) fluids[i] = r.mFluidInputs[i].copy();
            final GTRecipe found = RecipeMaps.nanochipAssemblyMatrixRecipes.findRecipeQuery()
                .items(items)
                .fluids(fluids)
                .find();
            if (found != null && CircuitComponent.tryGetFromFakeStack(found.mOutputs[0]) == out) return found;
        }
        return null;
    }

    private static int packetChecks(List<String> lines, boolean active) {
        int bad = 0;
        // round trip
        final CircuitComponent sample = active ? BTSNacComponents.parts()
            .get(5) : CircuitComponent.PicoCircuit;
        final CircuitComponentPacket p = new CircuitComponentPacket(sample, 5, null);
        p.unifyWith(new CircuitComponentPacket(sample, 3, "named"));
        final NBTTagCompound saved = p.writeToNBT();
        CircuitComponentPacket back = null;
        try {
            back = new CircuitComponentPacket(saved);
        } catch (RuntimeException e) {
            lines.add("BAD  packet round trip threw " + e);
        }
        final boolean trip = back != null && back.equals(p) && back.getAmount(sample) == 8;
        if (!trip) bad++;
        lines.add(
            (trip ? "ok   " : "BAD  ") + "packet NBT round trip of "
                + sample.name()
                + " (5 unnamed + 3 named): "
                + (back == null ? "failed" : back.getAmount(sample) + " back, equal " + back.equals(p)));
        // unknown name: ours when the patch is inactive (a world saved with it), a made-up one otherwise
        final String unknown = active ? "BTS_NoSuchComponent" : "BTS_SiderealChip";
        final NBTTagCompound mixed = new CircuitComponentPacket(CircuitComponent.PicoCircuit, 2, null).writeToNBT();
        mixed.setLong(unknown, 7L);
        final int before = BTSNacPacketGuard.droppedCount();
        CircuitComponentPacket loaded = null;
        try {
            loaded = new CircuitComponentPacket(mixed);
        } catch (RuntimeException e) {
            lines.add("BAD  a packet with the unknown name " + unknown + " threw " + e);
        }
        final boolean guarded = loaded != null && loaded.getAmount(CircuitComponent.PicoCircuit) == 2
            && loaded.getComponents()
                .size() == 1
            && BTSNacPacketGuard.droppedCount() == before + 1;
        if (!guarded) bad++;
        lines.add(
            (guarded ? "ok   " : "BAD  ") + "packet with an unknown name \""
                + unknown
                + "\" (7) beside 2 x PicoCircuit: "
                + (loaded == null ? "did not load"
                    : "loaded, " + loaded.getComponents()
                        .size() + " component kind(s), PicoCircuit " + loaded.getAmount(CircuitComponent.PicoCircuit))
                + ", dropped +"
                + (BTSNacPacketGuard.droppedCount() - before));
        return bad;
    }
}
