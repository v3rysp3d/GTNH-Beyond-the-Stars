package com.nateyburr.beyondthestars.machines;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * Reads and drains of fluid input hatches that sit in a machine's PRIVATE lists (sockets GT never opens a recipe window
 * on: the quasar's gas and trim sockets, a conduit element's fuel hatches, the Reality anchors).
 * <p>
 * An ME stocking input hatch ({@link MTEHatchInputME}) is never put into GT's recipe-processing window here. Outside
 * that window GT's {@code getStoredFluids()} reports 1 L per configured fluid, and inside it a drain only lowers a
 * shadow copy that {@code endRecipeProcessing} extracts afterwards: a network short of AE power extracts less than
 * was drained, and a caller that already credited the drain keeps fluid that never left the network. So for an ME
 * hatch:
 * <ul>
 * <li>{@link #available} asks the hatch's grid for its stock of the configured fluid with a SIMULATE extract;</li>
 * <li>{@link #drain} extracts through {@code Platform.poweredExtraction} on that grid and returns what really came
 * out, which is the only amount a caller may credit.</li>
 * </ul>
 * The fluid must be configured in one of the hatch's slots (or auto-pulled), and the hatch must be allowed to work
 * with an active grid, exactly as GT's own reads require. Plain and quadruple hatches are read and drained directly.
 */
public final class PrivateHatchFluids {

    private PrivateHatchFluids() {}

    public static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    /** Litres of {@code fluid} the live hatches in {@code list} can give now. */
    public static long available(List<? extends MTEHatchInput> list, Fluid fluid) {
        if (fluid == null) return 0;
        long n = 0;
        for (MTEHatchInput hatch : list) {
            if (!live(hatch)) continue;
            n = saturatingAdd(n, available(hatch, fluid));
        }
        return n;
    }

    /** Litres of {@code fluid} one hatch can give now (an ME hatch: its grid's stock, if that fluid is configured). */
    public static long available(MTEHatchInput hatch, Fluid fluid) {
        if (hatch == null || fluid == null) return 0;
        if (hatch instanceof MTEHatchInputME) return meStock((MTEHatchInputME) hatch, fluid);
        long n = 0;
        for (FluidStack s : plainFluids(hatch)) {
            if (s != null && s.getFluid() == fluid && s.amount > 0) n += s.amount;
        }
        return n;
    }

    /** Drains up to {@code amount} of {@code fluid} from the live hatches in {@code list}; returns what was taken. */
    public static long take(List<? extends MTEHatchInput> list, Fluid fluid, long amount) {
        if (fluid == null || amount <= 0) return 0;
        long got = 0;
        for (MTEHatchInput hatch : list) {
            if (got >= amount) break;
            if (!live(hatch)) continue;
            got += drain(hatch, fluid, amount - got);
        }
        return got;
    }

    /** As {@link #take}, plain hatches only (ME stocking hatches are skipped). */
    public static long takePlain(List<? extends MTEHatchInput> list, Fluid fluid, long amount) {
        if (fluid == null || amount <= 0) return 0;
        long got = 0;
        for (MTEHatchInput hatch : list) {
            if (got >= amount) break;
            if (!live(hatch) || hatch instanceof MTEHatchInputME) continue;
            got += drain(hatch, fluid, amount - got);
        }
        return got;
    }

    /** As {@link #take}, ME stocking hatches only. */
    public static long takeStocking(List<? extends MTEHatchInput> list, Fluid fluid, long amount) {
        if (fluid == null || amount <= 0) return 0;
        long got = 0;
        for (MTEHatchInput hatch : list) {
            if (got >= amount) break;
            if (!live(hatch) || !(hatch instanceof MTEHatchInputME)) continue;
            got += drain(hatch, fluid, amount - got);
        }
        return got;
    }

    /** True when the list holds a live ME stocking hatch. */
    public static boolean hasStocking(List<? extends MTEHatchInput> list) {
        for (MTEHatchInput hatch : list) if (live(hatch) && hatch instanceof MTEHatchInputME) return true;
        return false;
    }

    /**
     * Drains up to {@code amount} of {@code fluid} from one hatch and returns the litres that really left it: for an
     * ME stocking hatch, what the powered extraction returned from its network.
     */
    public static long drain(MTEHatchInput hatch, Fluid fluid, long amount) {
        if (hatch == null || fluid == null || amount <= 0) return 0;
        if (hatch instanceof MTEHatchInputME) return meExtract((MTEHatchInputME) hatch, fluid, amount, true);
        long got = 0;
        for (FluidStack s : plainFluids(hatch)) {
            if (got >= amount) break;
            if (s == null || s.getFluid() != fluid || s.amount <= 0) continue;
            final int want = (int) Math.min(s.amount, amount - got);
            final FluidStack d = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(fluid, want), true);
            if (d != null && d.amount > 0) got += d.amount;
        }
        return got;
    }

    private static FluidStack[] plainFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    // ======================================================================== ME stocking hatches

    private static long meStock(MTEHatchInputME hatch, Fluid fluid) {
        return meExtract(hatch, fluid, Long.MAX_VALUE, false);
    }

    /** True when one of the hatch's 16 slots is configured to {@code fluid}. */
    private static boolean configured(MTEHatchInputME hatch, Fluid fluid) {
        final FluidStack probe = new FluidStack(fluid, 1);
        for (int i = 0; i < MTEHatchInputME.SLOT_COUNT; i++) {
            final FluidStack config = hatch.getSlotConfig(i);
            if (config != null && GTUtility.areFluidsEqual(config, probe)) return true;
        }
        return false;
    }

    /**
     * A SIMULATE ({@code real == false}) or powered ({@code real}) extraction of up to {@code amount} litres from the
     * hatch's network, no recipe window. Returns the litres the network gave (or would give).
     */
    private static long meExtract(MTEHatchInputME hatch, Fluid fluid, long amount, boolean real) {
        final IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
        if (base == null || !base.isServerSide() || !hatch.isAllowedToWork() || !configured(hatch, fluid)) return 0;
        if (!(base instanceof IActionHost)) return 0;
        try {
            final AENetworkProxy proxy = hatch.getProxy();
            final IMEMonitor<IAEFluidStack> store = proxy.getStorage()
                .getFluidInventory();
            final IAEFluidStack request = AEFluidStack.create(new FluidStack(fluid, 1));
            if (request == null) return 0;
            request.setStackSize(amount);
            final MachineSource source = new MachineSource((IActionHost) base);
            final IAEFluidStack got;
            if (real) {
                final IEnergyGrid energy = proxy.getEnergy();
                got = Platform.poweredExtraction(energy, store, request, source);
            } else {
                got = store.extractItems(request, Actionable.SIMULATE, source);
            }
            return got == null ? 0 : Math.max(0, got.getStackSize());
        } catch (GridAccessException e) {
            return 0;
        }
    }

    private static long saturatingAdd(long a, long b) {
        final long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0 ? Long.MAX_VALUE : r;
    }
}
