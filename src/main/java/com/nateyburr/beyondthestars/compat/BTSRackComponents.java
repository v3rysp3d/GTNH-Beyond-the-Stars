package com.nateyburr.beyondthestars.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.items.SLCircuitItems;

import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import tectech.thing.metaTileEntity.hatch.MTEHatchRack;
import tectech.util.TTUtility;

/**
 * CIRCUITS v2 §3.4: our three Mainframes as TecTech computer-rack components (the Quantum Computer's rack hatch).
 * <p>
 * The rack binds its parts by exact item ({@code MTEHatchRack.componentBinds}, keyed by
 * {@link TTUtility#getUniqueIdentifier}), not by ore name, so the Piko / Quantum / Planck raise would otherwise make
 * late-game computation dearer with no route through our circuits. I ruled "Yes, slightly better": each of
 * ours gets slightly better numbers than the stock circuit it stands beside, and stays below the next stock tier's
 * computation. All numbers live in {@link #TABLE} (knobs).
 * <p>
 * <b>How.</b> {@code MTEHatchRack.RackComponent}'s constructors are package-private; the
 * {@code (ItemStack, float computation, float heat, float cool, float maxHeat, boolean subZero)} one does everything
 * GT's own entries get (the bind, {@code validRackItems}, the NEI Quantum Computer page), so it is called reflectively
 * once per item in postInit, after TecTech's {@code MTEHatchRack.run()} (TecTech init). No mixin. Any reflective
 * failure logs one warning ("rack components unavailable") and returns; nothing else depends on this. Skipped with
 * {@code stellarCircuits=false}.
 */
public final class BTSRackComponents {

    private BTSRackComponents() {}

    /** One row of the table: our item and its rack numbers, beside the stock circuit it stands next to. */
    public static final class Row {

        public final String item;
        public final String beside;
        public final float computation, heat, cool, maxHeat;
        public final boolean subZero;

        Row(String item, String beside, float computation, float heat, float cool, float maxHeat, boolean subZero) {
            this.item = item;
            this.beside = beside;
            this.computation = computation;
            this.heat = heat;
            this.cool = cool;
            this.maxHeat = maxHeat;
            this.subZero = subZero;
        }

        /** The registered item, or null before preInit. */
        public Item item() {
            switch (item) {
                case "siderealMainframe":
                    return SLCircuitItems.siderealMainframe;
                case "empyreanMainframe":
                    return SLCircuitItems.empyreanMainframe;
                default:
                    return SLCircuitItems.omegaMainframe;
            }
        }
    }

    /**
     * The table (spec §3.4; computation, heat constant, cool constant, max heat, sub-zero). Stock beside it
     * ({@code MTEHatchRack.run}): Piko 260 / 12 / -1 / 9,500; Quantum 320 / 10 / -1 / 10,000; Planck 360 / 8 / -1 /
     * 10,000. Ours: +20 computation, one less heat, +500 max heat. All under TecTech's "20k heat cap max".
     */
    public static final Row[] TABLE = { new Row("siderealMainframe", "Piko", 280f, 11f, -1f, 10_000f, true),
        new Row("empyreanMainframe", "Quantum", 340f, 9f, -1f, 10_500f, true),
        new Row("omegaMainframe", "Planck", 380f, 7f, -1f, 10_500f, true) };

    private static int registered;

    /** Registers the three rows. Called once from {@code CommonProxy.postInit}. Never throws. */
    public static void register() {
        if (!Config.stellarCircuits) {
            BeyondTheStars.LOG.info("[BTS] Circuits v2: stellarCircuits=false, no computer-rack components.");
            return;
        }
        try {
            final Constructor<MTEHatchRack.RackComponent> make = MTEHatchRack.RackComponent.class
                .getDeclaredConstructor(
                    ItemStack.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    boolean.class);
            make.setAccessible(true);
            for (Row row : TABLE) {
                final Item item = row.item();
                if (item == null) throw new IllegalStateException(row.item + " is not registered");
                make.newInstance(
                    new ItemStack(item, 1, 0),
                    row.computation,
                    row.heat,
                    row.cool,
                    row.maxHeat,
                    row.subZero);
                registered++;
            }
            BeyondTheStars.LOG.info(
                "[BTS] Circuits v2: {} Mainframes registered as computer-rack components (Sidereal 280, Empyrean 340, "
                    + "Omega 380 computation).",
                registered);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            BeyondTheStars.LOG.warn(
                "[BTS] Circuits v2: rack components unavailable ({}); our Mainframes cannot go in a computer rack. "
                    + "Nothing else is affected.",
                failed.toString());
        }
    }

    /**
     * The C2 check: for each row, the rack's own bind (looked up as the rack does, by unique identifier) and its
     * numbers, whether the item is in {@code validRackItems}, and whether the NEI Quantum Computer page has it. Ends
     * with a {@code PASS} or {@code FAIL} line. Read-only.
     */
    public static List<String> report() {
        final List<String> lines = new ArrayList<>();
        int bad = 0;
        if (!Config.stellarCircuits) {
            lines.add("rack: stellarCircuits=false, no rack components expected.");
            return lines;
        }
        Map<?, ?> binds = null;
        try {
            final Field f = MTEHatchRack.class.getDeclaredField("componentBinds");
            f.setAccessible(true);
            binds = (Map<?, ?>) f.get(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            lines.add("BAD  rack: cannot read MTEHatchRack.componentBinds (" + failed + ")");
            bad++;
        }
        for (Row row : TABLE) {
            final Item item = row.item();
            final ItemStack stack = item == null ? null : new ItemStack(item, 1, 0);
            if (stack == null) {
                lines.add("BAD  rack " + row.item + ": item not registered");
                bad++;
                continue;
            }
            final String key = TTUtility.getUniqueIdentifier(stack);
            final Object comp = binds == null ? null : binds.get(key);
            String numbers = "no bind";
            boolean ok = comp != null;
            if (comp != null) {
                final float c = read(comp, "computation"), h = read(comp, "heatConstant"),
                    k = read(comp, "coolConstant"), m = read(comp, "maxHeat");
                final boolean z = readBool(comp, "subZero");
                numbers = fmt(c) + " / " + fmt(h) + " / " + fmt(k) + " / " + fmt(m) + " / subZero=" + z;
                ok = c == row.computation && h == row.heat && k == row.cool && m == row.maxHeat && z == row.subZero;
            }
            boolean valid = false;
            for (ItemStack v : MTEHatchRack.validRackItems) {
                if (v != null && v.getItem() == item && v.getItemDamage() == 0) valid = true;
            }
            boolean nei = false;
            for (GTRecipe r : RecipeMaps.quantumComputerFakeRecipes.getAllRecipes()) {
                if (r != null && r.mInputs != null
                    && r.mInputs.length > 0
                    && r.mInputs[0] != null
                    && r.mInputs[0].getItem() == item) nei = true;
            }
            ok = ok && valid && nei;
            if (!ok) bad++;
            lines.add(
                (ok ? "ok   " : "BAD  ") + "rack "
                    + key
                    + ": "
                    + numbers
                    + " (want "
                    + fmt(row.computation)
                    + " / "
                    + fmt(row.heat)
                    + " / "
                    + fmt(row.cool)
                    + " / "
                    + fmt(row.maxHeat)
                    + ", beside "
                    + row.beside
                    + "); validRackItems="
                    + valid
                    + ", NEI Quantum Computer page="
                    + nei);
        }
        lines.add(
            (bad == 0 ? "PASS" : "FAIL") + " rack components: " + TABLE.length + " checked, " + bad + " problem(s)");
        return lines;
    }

    private static float read(Object comp, String field) {
        try {
            final Field f = comp.getClass()
                .getDeclaredField(field);
            f.setAccessible(true);
            return f.getFloat(comp);
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return Float.NaN;
        }
    }

    private static boolean readBool(Object comp, String field) {
        try {
            final Field f = comp.getClass()
                .getDeclaredField(field);
            f.setAccessible(true);
            return f.getBoolean(comp);
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return false;
        }
    }

    private static String fmt(float f) {
        return f == (long) f ? String.valueOf((long) f) : String.valueOf(f);
    }
}
