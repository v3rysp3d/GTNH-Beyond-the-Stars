package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.SLP5Items;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.eye.field.UniverseField;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * B-225: the bench's standard rig for a kit-built Eye T9 / T10, saved with the Eye ({@code /btseye star x y z p5
 * stdrig on [mass M] | off | status}, or {@code stdrig} on the {@code /btseye t9|t10 …} build). It is what the
 * questbook's standard rig gives a player's Eye, every field second, through the Eye's own hatches:
 * <ul>
 * <li><b>The bursts</b>: a bench quasar ({@link EyeGalaxyBench}'s virtual disc: helium, coils 3, b 3, M SM, throats Mk
 * 2) through the standard conduit rig, J1 / J2 / J3 binary on the Eye's Field and Events codes (the B-178 smoke test's
 * rig). The disc is not saved: after a restart it spins up again at M.</li>
 * <li><b>The trims</b> into the Field sockets (as export buses with redstone cards): the Space valve on Field bit 1 (10
 * units/s), at T10 the two 2-unit valves on Universe bits 1 (Attractor Dark Matter) and 2 (Molten SpaceTime).</li>
 * <li><b>The pellets</b>: the Pellet Injector kept at 256 or more Superdense pellets (an ME stocking bus).</li>
 * <li><b>The power</b>: the owner's wireless network kept at 10^21 EU or more (a power plant), and the Eye's input
 * buffers topped up on Status bit 1 (an AE2 level maintainer).</li>
 * </ul>
 * The beam's V loop is NOT here: the kit wires it with real covers (the V sensor to the injector's Burst Control Hatch,
 * {@code EyeKit}), so the Eye's own beam hardware holds V. The debug field rig ({@code rig}) must be off (none), or it
 * replaces the Field sockets and the beam's wire.
 */
public final class EyeStdRig {

    private EyeStdRig() {}

    /** The default bench-quasar mass: 300 SM holds T9, 600 SM T10 (p5-t9-t10.md §2.5, §6.5). */
    public static double defaultMass(int tier) {
        return tier >= 10 ? 600 : 300;
    }

    /** The bench quasars, by Eye position (not saved: re-seeded at the saved mass after a load). */
    private static final Map<Integer, EyeGalaxyBench.Rig> DISCS = new HashMap<>();

    static final BigInteger EU_FLOOR = BigInteger.TEN.pow(21);
    static final BigInteger EU_TOP = BigInteger.TEN.pow(22);
    static final int PELLETS_FLOOR = 256;

    /** Turns the rig on at {@code mass} SM (0 or less: off). Returns the lines to print. */
    public static List<String> set(MTEEyeOfHarmonyBTS eye, double mass) {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = eye.starRuntime();
        DISCS.remove(key(eye));
        if (mass <= 0) {
            rt.benchStdRig = 0;
            EyeGalaxyBench.unscripted(eye);
            out.add("standard rig OFF: the Eye reads its GRB Receptor Ports again, nothing pours into its sockets");
        } else {
            rt.benchStdRig = mass;
            if (rt.rig != null) {
                rt.setRig(null);
                out.add("(the debug field rig was on: set to none, so the Field sockets and the beam's wire count)");
            }
            out.add(
                String.format(
                    Locale.ROOT,
                    "standard rig ON (saved with the Eye): a %.0f SM bench quasar through the standard conduit rig,"
                        + " the Space valve on Field [1], at T10 the 2-unit Dark Matter / SpaceTime valves on Universe"
                        + " [1] / [2], the injector kept stocked, the EU and buffers topped up",
                    mass));
        }
        if (eye.getBaseMetaTileEntity() != null) eye.getBaseMetaTileEntity()
            .markDirty();
        out.add(status(eye));
        return out;
    }

    public static String status(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        if (rt.benchStdRig <= 0) return "standard rig: off";
        final EyeGalaxyBench.Rig r = DISCS.get(key(eye));
        return String.format(
            Locale.ROOT,
            "standard rig: on, bench quasar %.0f SM%s",
            rt.benchStdRig,
            r == null ? " (spins up on the next field second)"
                : String.format(
                    Locale.ROOT,
                    ", now M %.1f SM, G %.2f Bu/t, eruptions %d, purges %d",
                    r.q.M,
                    r.q.G,
                    r.eruptions,
                    r.purges));
    }

    /**
     * The {@code stdrig} option of {@code /btseye t9|t10 …}: the rig on at the tier's mass, and a T9 lit at once as
     * the Great Attractor (the kit skipped the chain below it); a T10 begins its first universe by itself, as a dark
     * Eye with T10 unlocked does (D-54), once the bench quasar's bursts reach the crown.
     */
    public static List<String> kitStart(MTEEyeOfHarmonyBTS eye, int tiers) {
        final List<String> out = new ArrayList<>(set(eye, defaultMass(tiers)));
        final EyeStarRuntime rt = eye.starRuntime();
        if (tiers == 9) {
            final String err = rt.debugLight(rt.wanted());
            out.add(err == null ? "The Great Attractor LIT (bench)" : "lit refused: " + err);
        } else if (tiers >= 10) {
            out.add(
                "The first universe begins by itself on the field second the bench quasar's bursts reach the crown");
        }
        return out;
    }

    /** One field second, before the Eye's own (from {@link EyeGalaxyBench#liveSecond}). */
    static void second(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        if (rt.benchStdRig <= 0) return;
        final int k = key(eye);
        EyeGalaxyBench.Rig r = DISCS.get(k);
        if (r == null) {
            r = new EyeGalaxyBench.Rig(rt.benchStdRig, 1, false);
            r.mk = 2;
            DISCS.put(k, r);
        }
        // the bursts: the bench quasar's disc second, the conduit, the Eye's scripted reading
        EyeGalaxyBench.second(eye, rt, r);
        // the trims (one second of the valves on last second's codes)
        final int p = Math.max(1, rt.lastParallel());
        final int field = rt.directiveCode(DirectiveSet.EYE_FIELD, 5);
        if ((field & 1) != 0) {
            pour(eye, TrimDef.Kind.SPACE, EyeStarRuntime.SPACE_VALVE * p);
        }
        final int uni = rt.directiveCode(DirectiveSet.EYE_UNIVERSE, 5);
        if ((uni & 1) != 0) pour(eye, TrimDef.Kind.DARK_MATTER, UniverseField.RIG_VALVE * p);
        if ((uni & 2) != 0) pour(eye, TrimDef.Kind.SPACETIME, UniverseField.RIG_VALVE * p);
        // the stock: pellets, buffers (Status bit 1), EU
        stockPellets(eye);
        if ((rt.directiveCode(DirectiveSet.EYE_STATUS, 5) & 1) != 0) eye.debugFillBuffers(1_000_000_000_000L);
        final UUID owner = rt.host.owner();
        if (owner != null && WirelessNetworkManager.getUserEU(owner)
            .compareTo(EU_FLOOR) < 0) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            WirelessNetworkManager.addEUToGlobalEnergyMap(owner, EU_TOP);
        }
    }

    /** {@code units} of a trim into the Field sockets (an export bus on a redstone card). */
    private static void pour(MTEEyeOfHarmonyBTS eye, TrimDef.Kind kind, double units) {
        final Fluid fluid = EyeStarRuntime.trimFluid(kind);
        if (fluid == null || units <= 0) return;
        int left = (int) Math.round(units * kind.unitLitres);
        for (MTEHatchInput h : eye.fieldHatches()) {
            if (left <= 0) break;
            if (h == null || !h.isValid()) continue;
            left -= h.fill(ForgeDirection.UNKNOWN, new FluidStack(fluid, left), true);
        }
    }

    /** Tops the first injector up to a full load of Superdense pellets once it is under {@link #PELLETS_FLOOR}. */
    private static void stockPellets(MTEEyeOfHarmonyBTS eye) {
        if (SLP5Items.gatedPelletSuperdense == null) return;
        for (MTEHatchPelletInjector inj : eye.injectorHatches()) {
            final IGregTechTileEntity b = inj == null ? null : inj.getBaseMetaTileEntity();
            if (b == null || !b.isServerSide()) continue;
            if (inj.pellets(3) >= PELLETS_FLOOR) return;
            for (int i = 0; i < MTEHatchPelletInjector.SLOTS; i++) {
                final ItemStack s = b.getStackInSlot(i);
                if (s == null) b.setInventorySlotContents(i, new ItemStack(SLP5Items.gatedPelletSuperdense, 64, 0));
                else if (s.getItem() == SLP5Items.gatedPelletSuperdense) s.stackSize = 64;
            }
            b.markDirty();
            return;
        }
    }

    private static int key(MTEEyeOfHarmonyBTS eye) {
        final IGregTechTileEntity b = eye.getBaseMetaTileEntity();
        return b == null ? 0 : (b.getXCoord() * 31 + b.getYCoord()) * 31 + b.getZCoord();
    }
}
