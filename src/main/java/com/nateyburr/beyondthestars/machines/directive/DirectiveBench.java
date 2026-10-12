package com.nateyburr.beyondthestars.machines.directive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.integration.BTSComputerApi;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.EyeTrimRule;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTOreDictUnificator;

/**
 * AUTOMATION PASS (task 4): the dev-server test rig. Console / op only, never reached in play.
 * <p>
 * {@code /btseye star x y z directive} swaps three of the Eye's Stellar Field Sensor Hatches for Directive Hatches
 * (Trims, Status, Transition; code output) - the creative build already fits them. {@code /btseye star x y z autorig
 * <s>} then runs the Eye for s field seconds on a scripted gate rig that sees ONLY those three hatches' codes (what a
 * comparator reads from each), a second late, and does what plain redstone, an export bus with a redstone card or an
 * ME interface would do with each bit:
 * <ul>
 * <li>Trims bit n: pour the n-th trim's valve (the prototype's bang-bang rate x the parallel) into the Field
 * sockets;</li>
 * <li>Status bit 1: stock the buffers (an AE2 level maintainer); bit 2: a Stellar Ignition Charge into the plinth
 * bus; bit 3: the event valve (T3 Super Coolant, T6 the Agent);</li>
 * <li>Transition bit 1: the next star chart into the plinth bus; bit 3: the wanted core into the plinth bus; bit 4
 * (Tachyon wanted): 100 L/s of Tachyon Rich Temporal Fluid into the Field sockets.</li>
 * </ul>
 * The Eye itself runs unchanged (its own drain, the bus pickups, the star). {@code autorig <s> cut <k>} keeps every
 * valve shut for the first k seconds, so the star can be lost and the rig seen to bring it back.
 * {@code /btskerr autorig x y z <s>} does
 * the same for the KEE on the Hole set; {@code ocloop} on either drives {@link BTSComputerApi}, the calls behind the
 * OpenComputers components, and prints what each returned.
 */
public final class DirectiveBench {

    private DirectiveBench() {}

    // ===================================================================================== Eye

    /** Swaps the first live sensor hatches for Directive Hatches until the Eye has Trims, Status and Transition. */
    public static List<String> eyeSetup(MTEEyeOfHarmonyBTS eye) {
        final List<String> out = new ArrayList<>();
        final DirectiveSet[] want = { DirectiveSet.EYE_TRIMS, DirectiveSet.EYE_STATUS, DirectiveSet.EYE_TRANSITION };
        final List<MTEHatchStellarFieldSensor> sensors = new ArrayList<>(eye.sensorHatches());
        int s = 0;
        for (DirectiveSet set : want) {
            if (hatch(eye.directiveHatches(), set) != null) continue;
            MTEHatchStellarFieldSensor sensor = null;
            while (s < sensors.size() && sensor == null) {
                final MTEHatchStellarFieldSensor c = sensors.get(s++);
                if (c != null && c.isValid()) sensor = c;
            }
            if (sensor == null) {
                out.add("No sensor hatch left to swap for the " + set.label + " hatch.");
                break;
            }
            final IGregTechTileEntity hb = sensor.getBaseMetaTileEntity();
            final int hx = hb.getXCoord(), hy = hb.getYCoord(), hz = hb.getZCoord();
            if (!MTEReactorCoreMk1.placeGtMachine(
                hb.getWorld(),
                hx,
                hy,
                hz,
                (short) BTSMachines.directiveHatch.getItemDamage(),
                hb.getFrontFacing())) {
                out.add("Could not place a Directive Hatch at (" + hx + ", " + hy + ", " + hz + ").");
                continue;
            }
            final MTEHatchDirective h = directiveAt(hb.getWorld(), hx, hy, hz);
            if (h != null) {
                h.setSet(set);
                h.setOutput(DirectiveSet.Output.CODE);
            }
            out.add(set.label + " Directive Hatch placed at (" + hx + ", " + hy + ", " + hz + ")");
        }
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        if (base != null) eye.checkStructure(true, base);
        out.add("Re-walked: formed " + eye.mMachine + ", Directive Hatches " + eye.directiveHatchCount());
        return out;
    }

    /** The trim valve rates (units/s at p = 1) by trim-table position: the prototype's bang-bang rig. */
    private static double rate(int tier, int i) {
        return EyeTrimRule.rigRate(tier, i);
    }

    /** The scripted gate rig: {@code secs} field seconds, each reacting to last second's codes. */
    public static List<String> eyeAutorig(MTEEyeOfHarmonyBTS eye, long secs, long cut) {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = eye.starRuntime();
        final MTEHatchDirective trimsH = hatch(eye.directiveHatches(), DirectiveSet.EYE_TRIMS);
        final MTEHatchDirective statusH = hatch(eye.directiveHatches(), DirectiveSet.EYE_STATUS);
        final MTEHatchDirective transH = hatch(eye.directiveHatches(), DirectiveSet.EYE_TRANSITION);
        if (trimsH == null || statusH == null || transH == null) {
            out.add("The Eye needs Trims, Status and Transition Directive Hatches: run 'directive' first.");
            return out;
        }
        final long[] zones = new long[5];
        long lit = 0, ignitions = 0, losses = 0, charges = 0, stocks = 0, cores = 0, charts = 0, tach = 0;
        final long[] bitSeconds = new long[12];
        int unlockedStart = rt.unlockedTier();
        String lastFailure = rt.lastFailure();
        EyeStarRuntime.StarState prev = rt.state();
        final long t0 = System.nanoTime();
        for (long i = 0; i < secs; i++) {
            final boolean shut = i < cut; // the valves are held shut: the rig does nothing
            final int trims = shut ? 0 : trimsH.currentCode(), status = shut ? 0 : statusH.currentCode(),
                trans = shut ? 0 : transH.currentCode();
            for (int b = 0; b < 4; b++) {
                if ((trims & (1 << b)) != 0) bitSeconds[b]++;
                if ((status & (1 << b)) != 0) bitSeconds[4 + b]++;
                if ((trans & (1 << b)) != 0) bitSeconds[8 + b]++;
            }
            final TierParams table = rt.directiveTrimTable();
            final int p = Math.max(1, rt.lastParallel());
            if (table != null) {
                final List<TrimDef> defs = table.trims();
                for (int b = 0; b < Math.min(4, defs.size()); b++) {
                    if ((trims & (1 << b)) == 0) continue;
                    final TrimDef.Kind kind = defs.get(b).kind;
                    pour(eye, EyeStarRuntime.trimFluid(kind), rate(table.n, b) * p * kind.unitLitres);
                }
                if ((status & 4) != 0) {
                    final TrimDef.Kind kind = defs.size() > 4 ? defs.get(4).kind
                        : table.n == 3 ? TrimDef.Kind.SUPER_COOLANT : null;
                    if (kind != null) {
                        pour(
                            eye,
                            EyeStarRuntime.trimFluid(kind),
                            EyeTrimRule.rigExtraRate(table.n, (trans & 2) != 0) * p * kind.unitLitres);
                    }
                }
            }
            if ((status & 1) != 0) {
                eye.debugFillBuffers(1_000_000_000_000L); // an AE2 level maintainer on every input the Eye takes
                stocks++;
            }
            if ((status & 2) != 0 && put(eye, new ItemStack(SLItems.stellarIgnitionCharge, 1, 0))) charges++;
            if ((trans & 4) != 0) {
                final Item core = EyeStarRuntime.coreFor(rt.unlockedTier());
                if (core != null && put(eye, new ItemStack(core, 1, 0))) cores++;
            }
            if ((trans & 1) != 0) {
                final StarDef cur = rt.star() != null && rt.live() ? rt.star() : StarDef.ofTier(rt.unlockedTier());
                final StarDef next = cur == null ? null : StarDef.ofTier(cur.n() + 1);
                if (next != null && !inBus(eye, next.chart) && put(eye, next.chart.copy())) charts++;
            }
            if ((trans & 8) != 0) {
                pour(eye, EyeStarRuntime.timeFluid(), 100);
                tach++;
            }
            eye.fieldSecond();
            final EyeStarRuntime.StarState now = rt.state();
            if (prev != EyeStarRuntime.StarState.IGNITING && now == EyeStarRuntime.StarState.IGNITING) ignitions++;
            prev = now;
            if (!rt.lastFailure()
                .equals(lastFailure)) {
                lastFailure = rt.lastFailure();
                if (lastFailure.contains("LOST") || lastFailure.contains("SUPERNOVA")) losses++;
                out.add("  t+" + i + " s: " + lastFailure);
            }
            if (rt.burning()) {
                lit++;
                final int z = rt.lastReadout().zone;
                if (z >= 0 && z < 5) zones[z]++;
            }
        }
        final long ms = (System.nanoTime() - t0) / 1_000_000L;
        out.add(
            String.format(
                Locale.ROOT,
                "autorig %d s in %d ms: burning %d s (Resonant %d / Stable %d / Strained %d / Critical %d / Breach %d),"
                    + " stars lost %d, ignitions %d; rig actions: charges %d, stock-ups %d, cores %d, charts %d, "
                    + "Tachyon seconds %d; unlocked T%d -> T%d",
                secs,
                ms,
                lit,
                zones[0],
                zones[1],
                zones[2],
                zones[3],
                zones[4],
                losses,
                ignitions,
                charges,
                stocks,
                cores,
                charts,
                tach,
                unlockedStart,
                rt.unlockedTier()));
        out.add(
            String.format(
                Locale.ROOT,
                "bit-seconds: Trims %d/%d/%d/%d, Status %d/%d/%d/%d, Transition %d/%d/%d/%d",
                bitSeconds[0],
                bitSeconds[1],
                bitSeconds[2],
                bitSeconds[3],
                bitSeconds[4],
                bitSeconds[5],
                bitSeconds[6],
                bitSeconds[7],
                bitSeconds[8],
                bitSeconds[9],
                bitSeconds[10],
                bitSeconds[11]));
        for (MTEHatchDirective h : eye.directiveHatches()) out.add("  " + h.statusLine());
        return out;
    }

    /** The OpenComputers calls on the Eye, as a script would make them, with what each returned. */
    public static List<String> eyeOcLoop(MTEEyeOfHarmonyBTS eye) {
        final List<String> out = new ArrayList<>();
        out.add("getState() = " + BTSComputerApi.eyeState(eye));
        out.add("getField() = " + BTSComputerApi.eyeField(eye));
        out.add("getTransition() = " + BTSComputerApi.eyeTransition(eye));
        for (String set : new String[] { "EYE_TRIMS", "EYE_STATUS", "EYE_TRANSITION", "KERR_HOLE", "nonsense" }) {
            out.add("getDirective(\"" + set + "\") = " + BTSComputerApi.directive(eye, set, 10));
        }
        final double dial = eye.starRuntime()
            .dialSetpoint();
        final double feed = eye.starRuntime()
            .feedSetpoint();
        for (double v : new double[] { 2.1, 9, 0, 1.25 })
            out.add("setDial(" + v + ") = " + BTSComputerApi.eyeSetDial(eye, v));
        for (double v : new double[] { 2, -1, 0.5 })
            out.add("setFeed(" + v + ") = " + BTSComputerApi.eyeSetFeed(eye, v));
        BTSComputerApi.eyeSetDial(eye, dial);
        BTSComputerApi.eyeSetFeed(eye, feed);
        out.add("(dial and feed put back to " + dial + " / " + feed + ")");
        // the control hatches: redstone 1..15 into the dial and the feed, as a Directive Hatch on a control set
        final StringBuilder sb = new StringBuilder("control EYE_DIAL 1..15 ->");
        for (int s = 1; s <= 15; s++) {
            eye.directiveControl(DirectiveSet.EYE_DIAL, s);
            sb.append(
                String.format(
                    Locale.ROOT,
                    " %.2f",
                    eye.starRuntime()
                        .dialSetpoint()));
        }
        out.add(sb.toString());
        BTSComputerApi.eyeSetDial(eye, dial);
        return out;
    }

    // ==================================================================================== Kerr

    /** Directive Hatches into free utility sockets, then the Hole-set rig for {@code secs} seconds. */
    public static List<String> kerrAutorig(World world, MTEKerrEngine kee, long secs, int boltsPerSecond,
        boolean empty) {
        final List<String> out = new ArrayList<>();
        if (empty) {
            // start from nothing: every bolt bus emptied and the hole closed, so the rig must seed and bolt it
            for (MTEHatchInputBus bus : kee.boltBuses()) {
                if (bus == null || !bus.isValid()) continue;
                for (int i = 0; i < bus.getSizeInventory(); i++) bus.getBaseMetaTileEntity()
                    .setInventorySlotContents(i, null);
            }
            kee.debugCloseHole();
            kee.benchSecond();
            out.add("Bolt buses emptied and the hole closed.");
        }
        if (hatch(kee.directiveHatches(), DirectiveSet.KERR_HOLE) == null) {
            out.add("Fitted " + KerrKit.fitDirectives(world, kee) + " Directive Hatch(es); formed " + kee.mMachine);
        }
        final MTEHatchDirective hole = hatch(kee.directiveHatches(), DirectiveSet.KERR_HOLE);
        final MTEHatchDirective status = hatch(kee.directiveHatches(), DirectiveSet.KERR_STATUS);
        if (hole == null) {
            out.add("No Kerr Hole Directive Hatch.");
            return out;
        }
        final ItemStack bolt = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 1L);
        final ItemStack seed = ItemList.Black_Hole_Opener.get(1L);
        long boltsIn = 0, seedsIn = 0, open = 0, below = 0, closures = 0;
        double maxDev = 0;
        boolean wasOpen = kee.isHoleOpen();
        for (long i = 0; i < secs; i++) {
            final int code = hole.currentCode();
            if ((code & 4) != 0 && bolt != null) boltsIn += putKerr(kee, bolt, boltsPerSecond);
            if ((code & 1) != 0 && kee.getSeeds() == 0 && seed != null) seedsIn += putKerr(kee, seed, 1);
            kee.benchSecond();
            if (kee.isHoleOpen()) {
                open++;
                maxDev = Math.max(maxDev, Math.abs(kee.getMass() - kee.getSetpoint()));
                if ((hole.currentCode() & 2) != 0) below++;
            }
            if (wasOpen && !kee.isHoleOpen()) closures++;
            wasOpen = kee.isHoleOpen();
        }
        out.add(
            String.format(
                Locale.ROOT,
                "kerr autorig %d s: open %d s, closed by itself %d time(s), mass %s SM (setpoint %d), max deviation"
                    + " %s SM, below-setpoint %d s; rig put in %d SpaceTime bolts and %d Seed(s)",
                secs,
                open,
                closures,
                MTEKerrEngine.fmt(kee.getMass()),
                kee.getSetpoint(),
                MTEKerrEngine.fmt(maxDev),
                below,
                boltsIn,
                seedsIn));
        out.add("  " + hole.statusLine());
        if (status != null) out.add("  " + status.statusLine());
        return out;
    }

    /** The OpenComputers calls on the KEE, as a script would make them. */
    public static List<String> kerrOcLoop(MTEKerrEngine kee) {
        final List<String> out = new ArrayList<>();
        out.add("getState() = " + BTSComputerApi.kerrState(kee));
        out.add("getDirective(\"KERR_HOLE\") = " + BTSComputerApi.directive(kee, "KERR_HOLE", 10));
        out.add("getDirective(\"EYE_TRIMS\") = " + BTSComputerApi.directive(kee, "EYE_TRIMS", 10));
        final int sp = kee.getSetpoint();
        for (int v : new int[] { 100_000, 1, 24 })
            out.add("setSetpoint(" + v + ") = " + BTSComputerApi.kerrSetSetpoint(kee, v));
        BTSComputerApi.kerrSetSetpoint(kee, sp);
        out.add("(setpoint put back to " + sp + ")");
        final boolean auto = kee.isAutoSeeds();
        out.add("setAuto(false) = " + BTSComputerApi.kerrSetAuto(kee, false));
        out.add("collapse() = " + java.util.Arrays.toString(BTSComputerApi.kerrCollapse(kee)));
        out.add("open() = " + java.util.Arrays.toString(BTSComputerApi.kerrOpen(kee)));
        final ItemStack closer = ItemList.Black_Hole_Closer.get(1L);
        if (kee.isHoleOpen() && closer != null) {
            putKerr(kee, closer, 1);
            out.add(
                "(a Collapser put in a bolt bus) collapse() = "
                    + java.util.Arrays.toString(BTSComputerApi.kerrCollapse(kee)));
            out.add("open() without a Seed = " + java.util.Arrays.toString(BTSComputerApi.kerrOpen(kee)));
            final ItemStack seed = ItemList.Black_Hole_Opener.get(1L);
            if (seed != null) putKerr(kee, seed, 1);
            kee.benchSecond(); // the readouts see the Seed
            out.add("(a Seed put in) open() = " + java.util.Arrays.toString(BTSComputerApi.kerrOpen(kee)));
        }
        out.add("setAuto(" + auto + ") = " + BTSComputerApi.kerrSetAuto(kee, auto));
        final StringBuilder sb = new StringBuilder("control KERR_SETPOINT 1..15 ->");
        for (int s = 1; s <= 15; s++) {
            kee.directiveControl(DirectiveSet.KERR_SETPOINT, s);
            sb.append(' ')
                .append(kee.getSetpoint());
        }
        out.add(sb.toString());
        BTSComputerApi.kerrSetSetpoint(kee, sp);
        out.add("getState() = " + BTSComputerApi.kerrState(kee));
        return out;
    }

    /**
     * {@code /btskerr ctrl x y z on|off}: the control set with a real redstone signal. {@code on} turns the KEE's
     * Hole hatch into a mass-setpoint control and puts a redstone block against its open face (strength 15); the KEE
     * reads it on its own once a second while the chunk ticks. {@code off} reports the setpoint, takes the block away
     * and turns the hatch back.
     */
    public static List<String> kerrCtrl(World world, MTEKerrEngine kee, String mode) {
        final List<String> out = new ArrayList<>();
        if ("out".equals(mode)) {
            // the output as the world sees it: what a block against the hatch's open face receives now; then the
            // hatch's invert flips (code 0 inverted drives 15), so run it twice a few seconds apart
            final MTEHatchDirective hole = hatch(kee.directiveHatches(), DirectiveSet.KERR_HOLE);
            if (hole == null) {
                out.add("No Kerr: Hole Directive Hatch on this KEE.");
                return out;
            }
            final IGregTechTileEntity b = hole.getBaseMetaTileEntity();
            final ForgeDirection f = b.getFrontFacing();
            final int x = b.getXCoord() + f.offsetX, y = b.getYCoord() + f.offsetY, z = b.getZCoord() + f.offsetZ;
            out.add(
                "World redstone at the Hole hatch's face (" + x
                    + ", "
                    + y
                    + ", "
                    + z
                    + "): "
                    + world.getStrongestIndirectPower(x, y, z)
                    + "; comparator value "
                    + hole.getComparatorValue(f)
                    + "; the hatch drives "
                    + hole.currentOutput()
                    + " (invert "
                    + hole.isInvert()
                    + "); invert now "
                    + !hole.isInvert());
            hole.setInvert(!hole.isInvert());
            return out;
        }
        final boolean on = "on".equals(mode);
        MTEHatchDirective h = hatch(kee.directiveHatches(), on ? DirectiveSet.KERR_HOLE : DirectiveSet.KERR_SETPOINT);
        if (h == null) {
            out.add("No " + (on ? "Kerr: Hole" : "control") + " Directive Hatch on this KEE.");
            return out;
        }
        final IGregTechTileEntity b = h.getBaseMetaTileEntity();
        final ForgeDirection f = b.getFrontFacing();
        final int x = b.getXCoord() + f.offsetX, y = b.getYCoord() + f.offsetY, z = b.getZCoord() + f.offsetZ;
        if (on) {
            h.setSet(DirectiveSet.KERR_SETPOINT);
            world.setBlock(x, y, z, net.minecraft.init.Blocks.redstone_block);
            out.add(
                "Hatch at (" + b.getXCoord()
                    + ", "
                    + b.getYCoord()
                    + ", "
                    + b.getZCoord()
                    + ") set to the mass-setpoint control; redstone block at ("
                    + x
                    + ", "
                    + y
                    + ", "
                    + z
                    + "); setpoint now "
                    + kee.getSetpoint()
                    + " SM");
        } else {
            out.add(
                "Setpoint with redstone 15 on the control hatch: " + kee.getSetpoint()
                    + " SM (top "
                    + kee.maxSetpoint()
                    + "); the hatch read "
                    + h.getControlIn()
                    + "; "
                    + h.statusLine());
            world.setBlockToAir(x, y, z);
            h.setSet(DirectiveSet.KERR_HOLE);
            out.add("Redstone block removed, hatch back on Kerr: Hole.");
        }
        return out;
    }

    // ================================================================================= helpers

    static MTEHatchDirective hatch(List<MTEHatchDirective> list, DirectiveSet set) {
        for (MTEHatchDirective h : list) if (h != null && h.isValid() && h.getSet() == set) return h;
        return null;
    }

    private static MTEHatchDirective directiveAt(World world, int x, int y, int z) {
        final net.minecraft.tileentity.TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final Object mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEHatchDirective ? (MTEHatchDirective) mte : null;
    }

    /** A valve: {@code litres} of {@code fluid} into the Field-socket hatches (as a regulator cover would). */
    private static void pour(MTEEyeOfHarmonyBTS eye, Fluid fluid, double litres) {
        if (fluid == null || litres < 1) return;
        int left = (int) Math.round(litres);
        for (MTEHatchInput h : eye.fieldHatches()) {
            if (left <= 0) break;
            if (h == null || !h.isValid()) continue;
            left -= h.fill(ForgeDirection.UNKNOWN, new FluidStack(fluid, left), true);
        }
    }

    /** One stack into the first free slot of a plain plinth input bus (an ME interface or a conveyor). */
    private static boolean put(MTEEyeOfHarmonyBTS eye, ItemStack stack) {
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                if (bus.getStackInSlot(i) != null) continue;
                bus.getBaseMetaTileEntity()
                    .setInventorySlotContents(i, stack.copy());
                return true;
            }
        }
        return false;
    }

    private static boolean inBus(MTEEyeOfHarmonyBTS eye, ItemStack chart) {
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                final ItemStack s = bus.getStackInSlot(i);
                if (s != null && s.getItem() == chart.getItem() && s.getItemDamage() == chart.getItemDamage())
                    return true;
            }
        }
        return false;
    }

    /** Up to {@code count} of {@code what} into the KEE's bolt buses (an export bus); returns how many went in. */
    private static int putKerr(MTEKerrEngine kee, ItemStack what, int count) {
        int left = count;
        for (MTEHatchInputBus bus : kee.boltBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory() && left > 0; i++) {
                final ItemStack s = bus.getStackInSlot(i);
                if (s == null) {
                    final int n = Math.min(left, what.getMaxStackSize());
                    final ItemStack put = what.copy();
                    put.stackSize = n;
                    bus.getBaseMetaTileEntity()
                        .setInventorySlotContents(i, put);
                    left -= n;
                } else if (s.getItem() == what.getItem() && s.getItemDamage() == what.getItemDamage()
                    && s.stackSize < s.getMaxStackSize()) {
                        final int n = Math.min(left, s.getMaxStackSize() - s.stackSize);
                        s.stackSize += n;
                        left -= n;
                    }
            }
            if (left <= 0) break;
        }
        return count - left;
    }

}
