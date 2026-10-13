package com.nateyburr.beyondthestars.nac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nateyburr.beyondthestars.items.SLCircuitItems;
import com.nateyburr.beyondthestars.mixins.late.nac.CircuitComponentInvoker;

import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitCalibration;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;

/**
 * CIRCUITS v2 §4.2-§4.3 (design repo {@code designs/docs/circuits-v2.md}): <b>our 34 NAC circuit components</b>, built
 * from one table and appended to GT's {@link CircuitComponent} enum by {@code MixinCircuitComponent}.
 * <table>
 * <tr>
 * <th>Ids</th>
 * <th>Constants</th>
 * <th>Kind</th>
 * </tr>
 * <tr>
 * <td>10000-10010</td>
 * <td>{@code BTS_StellarSMD{Transistor,Resistor,Capacitor,Diode,Inductor}}, {@code BTS_{Sidereal,Empyrean,Omega}Chip},
 * {@code BTS_{Sidereal,Empyrean,Omega}Board}</td>
 * <td>CC (real item = ours)</td>
 * </tr>
 * <tr>
 * <td>10100-10110</td>
 * <td>{@code BTS_Processed...} in the same order</td>
 * <td>PC ({@code isProcessed = true})</td>
 * </tr>
 * <tr>
 * <td>10200-10211</td>
 * <td>{@code BTS_SiderealProcessor} ... {@code BTS_OmegaMainframe} (family order, member order)</td>
 * <td>circuit, {@link CircuitCalibration#NONE}, weight 0</td>
 * </tr>
 * </table>
 * Ids sit in an addon-only block that GT's own ranges (0-2199, PR #8422's additions) never reach; the {@code BTS_}
 * prefix keeps NBT names apart. Name keys {@code gt.circuitcomponent.bts.<lowercase>} and
 * {@code gt.circuitcomponent.bts.processed.<lowercase>} are in our {@code en_US.lang}; processed icons in
 * {@code assets/gregtech/textures/items/gt.circuitcomponent/processed/bts_processed*.png}.
 * <p>
 * <b>Calibration NONE</b> (open question 4, decided): our circuits leave the Assembly Matrix as real items but feed no
 * calibration bar; GT's calibration stays GT's.
 * <p>
 * <b>Timing.</b> {@link #extend} runs inside GT's enum initialiser, which can come before our preInit: nothing here may
 * touch our config or items at class-init. The real-item suppliers are lazy (they read {@link SLCircuitItems} when GT
 * or the coremod first asks, by postInit at the earliest), the same constraint GT's own constants live under.
 * <p>
 * <b>Degrade, never crash</b> (revision 2): {@link #extend} catches every {@code Throwable}, builds ours in a LOCAL
 * array first and publishes only a complete set, so GT's enum initialises exactly as stock on any failure and
 * {@link #active} stays false. A missed injection (the handler never runs) leaves it false too. The one log line
 * "NAC patch inactive" is written by {@link #reportInactive}, once per run.
 */
public final class BTSNacComponents {

    private BTSNacComponents() {}

    private static final Logger LOG = LogManager.getLogger("beyondthestars");

    /** The prefix of every constant of ours: the NBT name space the packet guard and the checks key on. */
    public static final String PREFIX = "BTS_";

    /** Our 34 constants, as the mixin appends them. */
    public static final int COUNT = 34;

    /** True only after a complete, successful extension of GT's enum. Read through {@link #isActive()}. */
    private static volatile boolean active;

    /** GT's own constant count when {@link #extend} ran (-1: it never ran). */
    private static int stockLength = -1;

    /** Why the patch is inactive (null while active, or before anything was tried). */
    private static String failure;

    private static boolean inactiveLogged;

    /** The CC names (after {@code BTS_}) of the 11 parts, in id order. */
    private static final String[] PART_NAMES = { "StellarSMDTransistor", "StellarSMDResistor", "StellarSMDCapacitor",
        "StellarSMDDiode", "StellarSMDInductor", "SiderealChip", "EmpyreanChip", "OmegaChip", "SiderealBoard",
        "EmpyreanBoard", "OmegaBoard" };

    private static final int PART_BASE = 10000, PROCESSED_BASE = 10100, CIRCUIT_BASE = 10200;

    /** Filled by a successful {@link #extend}; null entries otherwise. */
    private static CircuitComponent[] parts = new CircuitComponent[PART_NAMES.length];
    private static CircuitComponent[] processed = new CircuitComponent[PART_NAMES.length];
    private static CircuitComponent[] circuits = new CircuitComponent[12];

    // =========================================================================================== the extension

    /**
     * Called by {@code MixinCircuitComponent} inside {@code CircuitComponent.<clinit>}, right after GT stored
     * {@code $VALUES}. Returns {@code stock} plus our 34, or {@code stock} itself on any failure. Never throws.
     */
    public static CircuitComponent[] extend(CircuitComponent[] stock) {
        try {
            if (stock == null) throw new IllegalStateException("GT's $VALUES is null");
            stockLength = stock.length;
            final CircuitComponent[] ours = buildAll(stock.length);
            final CircuitComponent[] out = Arrays.copyOf(stock, stock.length + ours.length);
            System.arraycopy(ours, 0, out, stock.length, ours.length);
            parts = Arrays.copyOfRange(ours, 0, PART_NAMES.length);
            processed = Arrays.copyOfRange(ours, PART_NAMES.length, 2 * PART_NAMES.length);
            circuits = Arrays.copyOfRange(ours, 2 * PART_NAMES.length, ours.length);
            active = true;
            failure = null;
            LOG.info(
                "[BTS] NAC patch active: {} circuit components appended to GT's CircuitComponent ({} stock, {} total;"
                    + " ids {}-{}).",
                ours.length,
                stock.length,
                out.length,
                PART_BASE,
                CIRCUIT_BASE + 11);
            return out;
        } catch (Throwable t) {
            // A skipped invoker (its body throws AssertionError, or the interface refuses to load), a LinkageError,
            // anything: GT's enum must initialise as stock.
            active = false;
            reportInactive("the enum extension failed (" + t + ")", t);
            return stock;
        }
    }

    /** All 34 through the invoker, into a local array (ordinals continue GT's). */
    private static CircuitComponent[] buildAll(int ordinalBase) {
        final List<CircuitComponent> out = new ArrayList<>(COUNT);
        int ordinal = ordinalBase;
        final CircuitComponent[] localParts = new CircuitComponent[PART_NAMES.length];
        for (int i = 0; i < PART_NAMES.length; i++) {
            final int index = i;
            final String name = PREFIX + PART_NAMES[i];
            localParts[i] = CircuitComponentInvoker.bts$cc(
                name,
                ordinal++,
                PART_BASE + i,
                "gt.circuitcomponent.bts." + PART_NAMES[i].toLowerCase(Locale.ROOT),
                () -> real(partItem(index)));
            out.add(localParts[i]);
        }
        for (int i = 0; i < PART_NAMES.length; i++) {
            final CircuitComponent cc = localParts[i];
            final Supplier<CircuitComponent> of = () -> cc;
            out.add(
                CircuitComponentInvoker.bts$pc(
                    PREFIX + "Processed" + PART_NAMES[i],
                    ordinal++,
                    PROCESSED_BASE + i,
                    "gt.circuitcomponent.bts.processed." + PART_NAMES[i].toLowerCase(Locale.ROOT),
                    of,
                    true));
        }
        for (int f = 0; f < SLCircuitItems.FAMILIES.length; f++) {
            for (int m = 0; m < SLCircuitItems.MEMBERS.length; m++) {
                final int family = f, member = m;
                final String simple = capitalised(SLCircuitItems.FAMILIES[f]) + SLCircuitItems.MEMBERS[m];
                out.add(
                    CircuitComponentInvoker.bts$circuit(
                        PREFIX + simple,
                        ordinal++,
                        CIRCUIT_BASE + f * SLCircuitItems.MEMBERS.length + m,
                        "gt.circuitcomponent.bts." + simple.toLowerCase(Locale.ROOT),
                        () -> real(SLCircuitItems.circuit(family, member)),
                        CircuitCalibration.NONE,
                        0f));
            }
        }
        if (out.size() != COUNT) throw new IllegalStateException("built " + out.size() + " of " + COUNT);
        for (CircuitComponent cc : out) {
            if (cc == null) throw new IllegalStateException("the invoker returned null");
        }
        return out.toArray(new CircuitComponent[0]);
    }

    /** The real item of part {@code i} (PART_NAMES order); null before our preInit. */
    private static Item partItem(int i) {
        switch (i) {
            case 0:
                return SLCircuitItems.stellarSmdTransistor;
            case 1:
                return SLCircuitItems.stellarSmdResistor;
            case 2:
                return SLCircuitItems.stellarSmdCapacitor;
            case 3:
                return SLCircuitItems.stellarSmdDiode;
            case 4:
                return SLCircuitItems.stellarSmdInductor;
            case 5:
                return SLCircuitItems.siderealChip;
            case 6:
                return SLCircuitItems.empyreanChip;
            case 7:
                return SLCircuitItems.omegaChip;
            case 8:
                return SLCircuitItems.siderealCircuitBoard;
            case 9:
                return SLCircuitItems.empyreanCircuitBoard;
            default:
                return SLCircuitItems.omegaCircuitBoard;
        }
    }

    private static ItemStack real(Item item) {
        return item == null ? null : new ItemStack(item, 1, 0);
    }

    private static String capitalised(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Logs "NAC patch inactive" with the reason, at most once per run (the extension's catch and
     * {@code BTSRecipesNac} both report through here).
     */
    public static void reportInactive(String reason, Throwable cause) {
        if (failure == null) failure = reason;
        if (inactiveLogged) return;
        inactiveLogged = true;
        final String text = "[BTS] NAC patch inactive: " + reason
            + ". GT's CircuitComponent enum is stock; no SL-N recipe and no NAC route for our circuit parts (the"
            + " Circuit Assembler / Assembly Line route is unaffected).";
        if (cause != null) LOG.error(text, cause);
        else LOG.error(text);
    }

    // ============================================================================================ read side

    /**
     * True when our 34 constants are in GT's enum. Initialises the enum first (its static initialiser is where the
     * extension runs), so the answer never depends on whether something else touched {@link CircuitComponent} yet.
     */
    public static boolean isActive() {
        if (CircuitComponent.VALUES.length < 0) return false; // the read forces CircuitComponent.<clinit>
        return active;
    }

    /** The reason the patch is inactive, or null. */
    public static String failure() {
        return failure;
    }

    /** GT's own constant count (from the extension, or by counting non-{@code BTS_} names when it never ran). */
    public static int stockLength() {
        if (stockLength >= 0) return stockLength;
        int n = 0;
        for (CircuitComponent cc : CircuitComponent.values()) if (!isOurs(cc)) n++;
        return n;
    }

    public static boolean isOurs(CircuitComponent cc) {
        return cc != null && cc.name()
            .startsWith(PREFIX);
    }

    /** The 11 part CCs (Stellar SMDs, chips, boards), id order; empty while inactive. */
    public static List<CircuitComponent> parts() {
        return isActive() ? Collections.unmodifiableList(Arrays.asList(parts)) : Collections.emptyList();
    }

    /** The 11 processed parts, same order as {@link #parts()}; empty while inactive. */
    public static List<CircuitComponent> processedParts() {
        return isActive() ? Collections.unmodifiableList(Arrays.asList(processed)) : Collections.emptyList();
    }

    /** The 12 circuit CCs, family then member order; empty while inactive. */
    public static List<CircuitComponent> circuits() {
        return isActive() ? Collections.unmodifiableList(Arrays.asList(circuits)) : Collections.emptyList();
    }

    /** Every component with a real item (the 11 parts and the 12 circuits): the 23 the conversion recipes cover. */
    public static List<CircuitComponent> withRealItem() {
        final List<CircuitComponent> out = new ArrayList<>(parts());
        out.addAll(circuits());
        return out;
    }

    /** The circuit CC of family {@code f}, member {@code m} (null while inactive). */
    public static CircuitComponent circuit(int f, int m) {
        return isActive() ? circuits[f * SLCircuitItems.MEMBERS.length + m] : null;
    }

    /** The CC whose real item is {@code item} (a part or a circuit), or null. */
    public static CircuitComponent forItem(Item item) {
        if (!isActive() || item == null) return null;
        for (int i = 0; i < PART_NAMES.length; i++) if (partItem(i) == item) return parts[i];
        for (int f = 0; f < SLCircuitItems.FAMILIES.length; f++) {
            for (int m = 0; m < SLCircuitItems.MEMBERS.length; m++) {
                if (SLCircuitItems.circuit(f, m) == item) return circuit(f, m);
            }
        }
        return null;
    }

    /** The processed component of the part whose real item is {@code item}, or null. */
    public static CircuitComponent processedFor(Item item) {
        if (!isActive() || item == null) return null;
        for (int i = 0; i < PART_NAMES.length; i++) if (partItem(i) == item) return processed[i];
        return null;
    }
}
