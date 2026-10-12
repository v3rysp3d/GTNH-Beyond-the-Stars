package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrSlot;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.conditions.RedstoneCondition;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * DEV-ONLY AUTOMATION PHOTO RIGS ({@code -Dbts.rendertest=true -Dbts.rendertest.cases=automation}), the server half.
 * In normal play nothing here is loaded.
 * <p>
 * Two real rigs are built in the superflat test world with real blocks, and from then on the harness never touches
 * the machines: the ONLY actor is the wiring. The harness only stocks nothing, reads state once a second into
 * {@code screenshots/autorig/*.tsv} and moves the camera.
 * <ul>
 * <li><b>Rig A, Eye of Harmony T1 (Sol), plain redstone and GT covers.</b> The Eye is raised by its kit, dark, with
 * empty buffers. Directive Hatches in five Sensor sockets: Trims in Bit 1 mode and Trims in Bit 2 mode (the
 * docs' minimal rig), Status in Bit 1 mode (buffers short), Transition in Code mode (a comparator reads it) and Trims
 * in Any mode (a redstone-torch NOT gate lights an "in band" lamp). Each bit drives a GT Machine Controller cover
 * ("enable with redstone") on a GT Debug Fluid Tank (an endless supply standing in for the player's), whose Fluid
 * Regulator cover ("Conditional": only while the tank is enabled) feeds the Field hatch (trims) or a gas hatch
 * (upkeep). The Status line runs from the band 40 blocks up to the plinth as a redstone staircase with repeaters.
 * Four Stellar Ignition Charges sit in the plinth bus (the docs' minimal rig); the Eye lights itself once the
 * upkeep valves have stocked its ignition.</li>
 * <li><b>Rig B, Kerr T1, AE2.</b> The Kerr is raised by its kit, its bolt buses emptied and its hole closed. Directive
 * Hatches in two lower utility sockets: Hole in Bit 3 mode (bolts short) and Hole in Bit 1 mode (closed). Each drives
 * an AE2 export bus with a redstone card facing the bolt bus beside it (bolts; a Black Hole Seed). An ME controller,
 * drive, interface and a level emitter with a lamp complete the network; the cell goes into the drive when the camera
 * is ready ({@link AutoRigAE2#insertCell}).</li>
 * </ul>
 */
public final class AutoRig {

    private AutoRig() {}

    /** The switch: the render harness on, with {@code -Dbts.rendertest.cases=automation}. */
    public static final boolean ENABLED = RenderTest.ENABLED && "automation".equals(
        System.getProperty("bts.rendertest.cases", "")
            .trim());

    static final int GROUND = 4;
    static final int EYE_X = 1000, KERR_X = 1700, SITE_Z = 4000;
    static final ForgeDirection FACING = ForgeDirection.NORTH;
    static final UUID OWNER = UUID.nameUUIDFromBytes("beyondthestars:autorig".getBytes(StandardCharsets.UTF_8));

    /** Trim valve: T1's rig rate, 10 units/s x 1,000 L at parallel 1 = 500 L/t (an HV Fluid Regulator, max 512). */
    static final int TRIM_LPT = 500;
    /** Upkeep valve: 150,000 L/t = 3 M L/s (a UV Fluid Regulator, max 524,288). */
    static final int UPKEEP_LPT = 150_000;

    // ===================================================================================== frames

    /** A station's frame: origin (a hatch socket), its outward direction o and a lateral s. */
    static final class Frame {

        final int[] p;
        final ForgeDirection o, s;

        Frame(int[] p, ForgeDirection o, ForgeDirection s) {
            this.p = p;
            this.o = o;
            this.s = s;
        }

        int[] at(int dO, int dY, int dS) {
            return new int[] { p[0] + o.offsetX * dO + s.offsetX * dS, p[1] + dY,
                p[2] + o.offsetZ * dO + s.offsetZ * dS };
        }

        double[] atD(double dO, double dY, double dS) {
            return new double[] { p[0] + 0.5 + o.offsetX * dO + s.offsetX * dS, p[1] + 0.5 + dY,
                p[2] + 0.5 + o.offsetZ * dO + s.offsetZ * dS };
        }
    }

    // ---------------------------------------------------------------- Rig A
    static int[] eyeCtrl;
    static int[] eyeBox;
    static double[] eyeCentre;
    /** Field sockets (origin) under their Sensor-socket hatch: Status, Helium trim, Hydrogen trim, Transition, NOT. */
    static Frame fStatus, fHe, fH, fTrans, fNot;
    /** Gas sockets (origin) with their upkeep valve: Helium, Hydrogen. */
    static Frame upHe, upH;
    static final List<int[]> statusWire = new ArrayList<>();
    static int[] stairFoot;
    static int[] stairMid;
    static Fluid helium, hydrogen;

    // ---------------------------------------------------------------- Rig B
    static int[] keeCtrl;
    static int[] kerrBox;
    static double[] kerrCentre;
    /** Bolt-bus sockets (origin) with the Directive Hatch one below: bolts (Hole bit 3) and seeds (Hole bit 1). */
    static Frame kBolt, kSeed;
    static final List<int[]> meRoute = new ArrayList<>();

    static final List<String> buildLog = new ArrayList<>();
    static volatile boolean built;
    static long t0Tick = -1;
    static long serverTick;

    // ===================================================================================== build

    /** Builds both rigs (server thread). Returns the build log. */
    static List<String> buildAll() {
        buildLog.clear();
        final WorldServer world = RenderTest.world();
        helium = Materials.Helium.getGas(1L) == null ? null
            : Materials.Helium.getGas(1L)
                .getFluid();
        hydrogen = Materials.Hydrogen.getGas(1L) == null ? null
            : Materials.Hydrogen.getGas(1L)
                .getFluid();
        try {
            buildEyeRig(world);
        } catch (RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] Rig A failed", bad);
            buildLog.add("RIG A FAILED: " + bad);
        }
        try {
            buildKerrRig(world);
        } catch (RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] Rig B failed", bad);
            buildLog.add("RIG B FAILED: " + bad);
        }
        openLogs();
        t0Tick = serverTick;
        built = true;
        return new ArrayList<>(buildLog);
    }

    // ------------------------------------------------------------------------------- Rig A

    private static void buildEyeRig(WorldServer world) {
        final int x = EYE_X, z = SITE_Z;
        final String[] pieces = Arrays.copyOf(MTEEyeOfHarmonyBTS.PIECES, 1);
        final int[] probe = SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, 100, z, pieces);
        final int y = 100 + GROUND - probe[1];
        eyeBox = SLConsoleTest.bounds(ExtendedFacing.of(FACING), x, y, z, pieces);
        RenderTest.loadAndKeep(world, grow(eyeBox, 90));
        eyeCtrl = new int[] { x, y, z };
        // genMeta 0: Crude field generators, as a T1 player has them (the offline proof's ts = tt = 1)
        final EyeKit.Result r = EyeKit.build(world, x, y, z, FACING, 1, 0, false, OWNER, "BTS autorig", false);
        if (r.failure != null) throw new IllegalStateException("Eye build: " + r.failure);
        for (String line : r.lines) buildLog.add("A: " + RenderTest.strip(line));
        final MTEEyeOfHarmonyBTS eye = eye();
        if (eye == null) throw new IllegalStateException("no Eye after the build");
        final int[] c = com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender
            .centrePos(eye.getExtendedFacing(), x, y, z);
        eyeCentre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
        // the owner's wireless network: 10^16 EU (Sol: 1.5e11 EU a cycle, ten cycles to ignite)
        WirelessNetworkManager.strongCheckOrAddUser(OWNER);
        WirelessNetworkManager.addEUToGlobalEnergyMap(OWNER, BigInteger.TEN.pow(16));
        // four Stellar Ignition Charges in the plinth bus (the docs' minimal rig: "an input bus with charges")
        int charges = 0;
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            bus.getBaseMetaTileEntity()
                .setInventorySlotContents(0, new ItemStack(SLItems.stellarIgnitionCharge, 4, 0));
            charges = 4;
            break;
        }
        buildLog.add("A: " + charges + " Stellar Ignition Charges in the plinth bus; buffers empty; star DARK");

        final ExtendedFacing al = eye.getExtendedFacing();
        final int[][] f = EyeKit.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'f');
        final int[][] h = EyeKit.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h');
        fStatus = frame(world, al, f[0], 0, 0, 2, 2);
        fHe = frame(world, al, f[1], 1, 0, 2, 3);
        fH = frame(world, al, f[2], 1, 0, 2, 3);
        fTrans = frame(world, al, f[3], 1, 0, 2, 1);
        fNot = frame(world, al, f[4], 1, 0, 2, 4);
        upHe = frame(world, al, h[0], 1, -1, 4, 1);
        upH = frame(world, al, h[1], 1, -1, 4, 1);

        trimStation(
            world,
            fHe,
            helium,
            DirectiveSet.Output.BIT1,
            "Trims hatch",
            "Bit 1: trim 1",
            "wanted = Helium",
            "valve opens");
        trimStation(
            world,
            fH,
            hydrogen,
            DirectiveSet.Output.BIT2,
            "Trims hatch",
            "Bit 2: trim 2",
            "wanted = Hydro-",
            "gen valve opens");
        transStation(world, fTrans);
        notStation(world, fNot);
        upkeepStation(world, upHe, helium, "Status bit 1", "buffers short:", "Helium upkeep", "valve opens");
        upkeepStation(world, upH, hydrogen, "Status bit 1", "buffers short:", "Hydrogen upkeep", "valve opens");
        statusLine(world);

        eye.checkStructure(true, eye.getBaseMetaTileEntity());
        buildLog.add(
            "A: Eye re-walked: formed " + eye.mMachine
                + ", Directive Hatches "
                + eye.directiveHatchCount()
                + ", Field hatches "
                + eye.fieldHatches()
                    .size()
                + ", rig "
                + (eye.starRuntime()
                    .rig() == null ? "none (the trims come from the Field hatches only)"
                        : "SET: " + eye.starRuntime()
                            .rig()));
    }

    /**
     * A frame on a socket: origin at the socket, o = the hatch's front (outward), s = the horizontal side whose test
     * cells (o 1..maxO, y lo..hi, s 1..maxS) are clearest.
     */
    private static Frame frame(World world, ExtendedFacing al, int[] rel, int lo, int hi, int maxO, int maxS) {
        final int[] p = SLConsoleTest.worldPos(al, eyeOrKerr()[0], eyeOrKerr()[1], eyeOrKerr()[2], rel);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        ForgeDirection o = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getFrontFacing()
            : ForgeDirection.NORTH;
        if (o == ForgeDirection.UP || o == ForgeDirection.DOWN) o = FACING;
        final ForgeDirection s1 = o.getRotation(ForgeDirection.UP);
        final ForgeDirection s2 = s1.getOpposite();
        final Frame a = new Frame(p, o, s1), b = new Frame(p, o, s2);
        return blocked(world, a, lo, hi, maxO, maxS) <= blocked(world, b, lo, hi, maxO, maxS) ? a : b;
    }

    /** Whose controller {@link #frame} measures from (the rig being built). */
    private static int[] buildingFor;

    private static int[] eyeOrKerr() {
        return buildingFor != null ? buildingFor : eyeCtrl;
    }

    private static int blocked(World world, Frame f, int lo, int hi, int maxO, int maxS) {
        int n = 0;
        for (int dO = 1; dO <= maxO; dO++)
            for (int dY = Math.min(lo, hi) - 1; dY <= Math.max(lo, hi) + 1; dY++) for (int dS = 0; dS <= maxS; dS++) {
                final int[] q = f.at(dO, dY, dS);
                if (!world.isAirBlock(q[0], q[1], q[2])) n++;
            }
        return n;
    }

    /** A Directive Hatch at {@code p} (placed if the socket holds something else), set and output mode. */
    private static MTEHatchDirective directive(World world, int[] p, ForgeDirection facing, DirectiveSet set,
        DirectiveSet.Output out, int window) {
        MTEHatchDirective h = mteAt(world, p, MTEHatchDirective.class);
        if (h == null) {
            MTEReactorCoreMk1
                .placeGtMachine(world, p[0], p[1], p[2], (short) BTSMachines.directiveHatch.getItemDamage(), facing);
            h = mteAt(world, p, MTEHatchDirective.class);
        }
        if (h == null) {
            buildLog.add("COULD NOT place a Directive Hatch at " + RenderTest.pos(p));
            return null;
        }
        h.setSet(set);
        h.setOutput(out);
        h.setWindow(window);
        return h;
    }

    /**
     * A trim valve under its Trims hatch. f = the Field hatch (origin), the hatch one up. In front of f a Debug Fluid
     * Tank: Fluid Regulator (HV, 500 L/t, Conditional) on its face to f, Machine Controller ("enable with redstone")
     * on its top. Dust on the tank top takes the hatch's output and powers the controller; it runs on along s to a
     * repeater and a lamp. A sign on the quartz under the dust.
     */
    private static void trimStation(World world, Frame F, Fluid fluid, DirectiveSet.Output bit, String... sign) {
        directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_TRIMS, bit, 10);
        final int[] t = F.at(1, 0, 0);
        tank(world, t, F.s, fluid);
        regulator(world, t, F.o.getOpposite(), ItemList.FluidRegulator_HV, TRIM_LPT);
        controller(world, t, ForgeDirection.UP);
        solid(world, F.at(1, 0, 1));
        solid(world, F.at(1, 0, 2));
        dust(world, F.at(1, 1, 0));
        dust(world, F.at(1, 1, 1));
        repeater(world, F.at(1, 1, 2), F.s, 0);
        lamp(world, F.at(1, 1, 3));
        sign(world, F.at(2, 0, 1), F.o, sign);
        sign(world, F.at(2, 0, 2), F.o, "Debug tank =", "your supply;", "regulator 500", "L/t into Field");
    }

    /** The Transition hatch in Code mode, a comparator reading it, a lamp lit while the code is above 0. */
    private static void transStation(World world, Frame F) {
        directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_TRANSITION, DirectiveSet.Output.CODE, 10);
        solid(world, F.at(1, 0, 0));
        solid(world, F.at(2, 0, 0));
        comparator(world, F.at(1, 1, 0), F.o, false);
        lamp(world, F.at(2, 1, 0));
        solid(world, F.at(2, 0, 1));
        sign(world, F.at(3, 0, 0), F.o, "Transition hatch", "Code mode: the", "comparator reads", "0-15 -> lamp");
    }

    /** NOT gate: a Trims hatch in Any mode, dust into a block, a torch on its far side, a lamp: lit = in band. */
    private static void notStation(World world, Frame F) {
        directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_TRIMS, DirectiveSet.Output.ANY, 10);
        solid(world, F.at(1, 0, 0));
        solid(world, F.at(1, 0, 1));
        dust(world, F.at(1, 1, 0));
        dust(world, F.at(1, 1, 1));
        solid(world, F.at(1, 1, 2));
        torch(world, F.at(1, 1, 3), F.s);
        lamp(world, F.at(1, 1, 4));
        sign(world, F.at(2, 0, 1), F.o, "Trims, Any bit", "NOT gate (torch)", "lamp lit = no", "trim: in band");
    }

    /**
     * An upkeep valve at a gas hatch (origin): Debug Fluid Tank in front, Fluid Regulator (UV, 150,000 L/t,
     * Conditional) into the hatch, Machine Controller on its front, fed by a repeater; dust on quartz behind it.
     * The ground line arrives at {@code at(4, -1, 0)}.
     */
    private static void upkeepStation(World world, Frame G, Fluid fluid, String... sign) {
        final int[] t = G.at(1, 0, 0);
        tank(world, t, G.s, fluid);
        regulator(world, t, G.o.getOpposite(), ItemList.FluidRegulator_UV, UPKEEP_LPT);
        controller(world, t, G.o);
        solid(world, G.at(2, -1, 0));
        repeater(world, G.at(2, 0, 0), G.o.getOpposite(), 0);
        solid(world, G.at(3, -1, 0));
        dust(world, G.at(3, 0, 0));
        sign(world, G.at(3, -1, 1), G.s, sign);
    }

    /**
     * The Status hatch (Bit 1, buffers short) and its line: dust on quartz in front of it, a redstone staircase down
     * along o to the ground with a repeater landing whenever the signal runs low, then along the ground to both upkeep
     * stations (a turn-penalised shortest path, a repeater on a straight run whenever the signal runs low).
     */
    private static void statusLine(WorldServer world) {
        statusWire.clear();
        final Frame F = fStatus;
        directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_STATUS, DirectiveSet.Output.BIT1, 10);
        int o = 1, y = 1, strength = 15;
        solid(world, F.at(1, 0, 0));
        dust(world, F.at(1, 1, 0));
        statusWire.add(F.at(1, 1, 0));
        int landings = 0;
        while (F.p[1] + y > GROUND) {
            if (strength <= 3) {
                solid(world, F.at(o + 1, y - 1, 0));
                repeater(world, F.at(o + 1, y, 0), F.o, 0);
                statusWire.add(F.at(o + 1, y, 0));
                solid(world, F.at(o + 2, y - 1, 0));
                dust(world, F.at(o + 2, y, 0));
                statusWire.add(F.at(o + 2, y, 0));
                o += 2;
                strength = 15;
                landings++;
                continue;
            }
            o++;
            y--;
            if (F.p[1] + y - 1 >= GROUND) solid(world, F.at(o, y - 1, 0));
            dust(world, F.at(o, y, 0));
            statusWire.add(F.at(o, y, 0));
            strength--;
        }
        stairFoot = F.at(o, y, 0);
        stairMid = F.at(o / 2, y / 2, 0);
        sign(world, F.at(1, 0, 1), F.s, "Status hatch", "Bit 1: buffers", "short -> both", "upkeep valves");
        buildLog.add(
            "A: Status staircase " + statusWire.size()
                + " cells, "
                + landings
                + " repeater landings, foot "
                + RenderTest.pos(stairFoot)
                + " with signal "
                + strength);
        final Set<Long> reserved = new HashSet<>();
        for (Frame g : new Frame[] { upHe, upH }) {
            for (int dO = 0; dO <= 3; dO++) for (int dS = -1; dS <= 1; dS++) {
                final int[] q = g.at(dO, -1, dS);
                reserved.add(key(q[0], q[2]));
            }
        }
        final Set<Long> used = new HashSet<>();
        for (int[] q : statusWire) if (q[1] == GROUND) used.add(key(q[0], q[2]));
        for (Frame g : new Frame[] { upHe, upH }) {
            final int[] end = g.at(4, -1, 0);
            final List<int[]> path = route(world, stairFoot, end, reserved, used);
            if (path == null) {
                buildLog.add("A: NO GROUND ROUTE from the staircase foot to " + RenderTest.pos(end));
                continue;
            }
            int s = strength;
            int reps = 0;
            for (int i = 1; i < path.size(); i++) {
                final int[] q = path.get(i);
                final int[] prev = path.get(i - 1);
                final boolean straight = i + 1 < path.size() && collinear(prev, q, path.get(i + 1));
                if (s <= 5 && straight) {
                    final ForgeDirection d = dir(q, path.get(i + 1));
                    repeater(world, new int[] { q[0], GROUND, q[2] }, d, 0);
                    s = 16; // the next cell gets 15
                    reps++;
                } else {
                    dust(world, new int[] { q[0], GROUND, q[2] });
                }
                s--;
                statusWire.add(new int[] { q[0], GROUND, q[2] });
                used.add(key(q[0], q[2]));
                for (ForgeDirection d : HORIZONTAL) used.add(key(q[0] + d.offsetX, q[2] + d.offsetZ));
            }
            buildLog.add(
                "A: ground line to " + RenderTest
                    .pos(end) + ": " + (path.size() - 1) + " cells, " + reps + " repeaters, arrives with signal " + s);
        }
    }

    private static final ForgeDirection[] HORIZONTAL = { ForgeDirection.NORTH, ForgeDirection.SOUTH,
        ForgeDirection.WEST, ForgeDirection.EAST };

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static boolean collinear(int[] a, int[] b, int[] c) {
        return (b[0] - a[0]) == (c[0] - b[0]) && (b[2] - a[2]) == (c[2] - b[2]);
    }

    private static ForgeDirection dir(int[] a, int[] b) {
        for (ForgeDirection d : HORIZONTAL) if (a[0] + d.offsetX == b[0] && a[2] + d.offsetZ == b[2]) return d;
        return ForgeDirection.NORTH;
    }

    /**
     * Turn-penalised shortest ground path (y = GROUND) from {@code from} to {@code to}: cells must be air at the
     * ground and one above, not reserved, and not next to an earlier line (except near the start).
     */
    private static List<int[]> route(World world, int[] from, int[] to, Set<Long> reserved, Set<Long> used) {
        final int minX = Math.min(from[0], to[0]) - 60, maxX = Math.max(from[0], to[0]) + 60;
        final int minZ = Math.min(from[2], to[2]) - 60, maxZ = Math.max(from[2], to[2]) + 60;
        final int w = maxX - minX + 1, d = maxZ - minZ + 1;
        final int[] dist = new int[w * d * 4];
        final int[] prev = new int[w * d * 4];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        final PriorityQueue<int[]> q = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        for (int k = 0; k < 4; k++) {
            final int idx = ((from[0] - minX) * d + (from[2] - minZ)) * 4 + k;
            dist[idx] = 0;
            q.add(new int[] { 0, idx });
        }
        int goal = -1;
        while (!q.isEmpty()) {
            final int[] cur = q.poll();
            final int idx = cur[1];
            if (cur[0] > dist[idx]) continue;
            final int cell = idx / 4, k = idx % 4;
            final int cx = cell / d + minX, cz = cell % d + minZ;
            if (cx == to[0] && cz == to[2]) {
                goal = idx;
                break;
            }
            for (int nk = 0; nk < 4; nk++) {
                final ForgeDirection dd = HORIZONTAL[nk];
                final int nx = cx + dd.offsetX, nz = cz + dd.offsetZ;
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                final boolean isGoal = nx == to[0] && nz == to[2];
                if (!isGoal) {
                    if (!world.isAirBlock(nx, GROUND, nz) || !world.isAirBlock(nx, GROUND + 1, nz)) continue;
                    if (reserved.contains(key(nx, nz))) continue;
                    final boolean nearStart = Math.abs(nx - from[0]) + Math.abs(nz - from[2]) <= 2;
                    if (!nearStart && used.contains(key(nx, nz))) continue;
                }
                final int cost = cur[0] + 1 + (nk != k ? 6 : 0);
                final int nidx = ((nx - minX) * d + (nz - minZ)) * 4 + nk;
                if (cost < dist[nidx]) {
                    dist[nidx] = cost;
                    prev[nidx] = idx;
                    q.add(new int[] { cost, nidx });
                }
            }
        }
        if (goal < 0) return null;
        final List<int[]> path = new ArrayList<>();
        for (int idx = goal; idx >= 0; idx = prev[idx]) {
            final int cell = idx / 4;
            path.add(0, new int[] { cell / d + minX, GROUND, cell % d + minZ });
            if (prev[idx] < 0) break;
        }
        return path;
    }

    // ------------------------------------------------------------------------------- Rig B

    private static void buildKerrRig(WorldServer world) {
        final int x = KERR_X, z = SITE_Z;
        final int[] probe = KerrKit.assemblyBounds(FACING, x, 100, z, false);
        final int y = 100 + GROUND - probe[1];
        kerrBox = KerrKit.assemblyBounds(FACING, x, y, z, false);
        RenderTest.loadAndKeep(world, grow(kerrBox, 60));
        keeCtrl = new int[] { x, y, z };
        final KerrKit.Result r = KerrKit.build(world, x, y, z, FACING, false, false, OWNER, "BTS autorig");
        if (r.failure != null) throw new IllegalStateException("Kerr build: " + r.failure);
        for (String line : r.lines) buildLog.add("B: " + RenderTest.strip(line));
        final MTEKerrEngine kee = kee();
        if (kee == null) throw new IllegalStateException("no KEE after the build");
        final int[] c = KerrSlot.worldPos(kee.getExtendedFacing(), x, y, z, KerrSlot.centreRel());
        kerrCentre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
        // start from nothing: every bolt bus emptied (the kit stocks a Seed and bolts) and the hole closed
        for (MTEHatchInputBus bus : kee.boltBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) bus.getBaseMetaTileEntity()
                .setInventorySlotContents(i, null);
        }
        kee.debugCloseHole();
        final ExtendedFacing al = kee.getExtendedFacing();
        final int[][] u = KerrKit.socketRels(MTEKerrEngine.PIECE_T1, 'u');
        final int[][] b = KerrKit.socketRels(MTEKerrEngine.PIECE_T1, 'b');
        buildingFor = keeCtrl;
        try {
            // b[0] / b[1]: the upper front pair, the kit's Directive Hatches one block ABOVE their bus
            kBolt = frame(world, al, b[0], 0, 1, 4, 3);
            kSeed = frame(world, al, b[1], 0, 1, 4, 3);
        } finally {
            buildingFor = null;
        }
        // the ME network first: the dust sits on a facade on top of each export bus's cable block
        AutoRigAE2.build(world, kBolt, kSeed, meRoute, buildLog);
        kerrStation(
            world,
            kBolt,
            DirectiveSet.Output.BIT3,
            1,
            "Hole hatch",
            "Bit 3: bolts",
            "short -> export",
            "bus (bolts)");
        kerrStation(
            world,
            kSeed,
            DirectiveSet.Output.BIT1,
            10,
            "Hole hatch",
            "Bit 1: hole",
            "closed -> export",
            "bus (a Seed)");
        kee.checkStructure(true, kee.getBaseMetaTileEntity());
        buildLog.add(
            "B: KEE re-walked: formed " + kee.mMachine
                + ", hole "
                + (kee.isHoleOpen() ? "OPEN" : "closed")
                + ", setpoint "
                + kee.getSetpoint()
                + " SM, Directive Hatches "
                + kee.directiveHatchCount()
                + ", bolt buses "
                + kee.boltBusCount()
                + " (all empty)");
    }

    /**
     * A Kerr station on a bolt bus (origin) with its Directive Hatch one above: the AE2 cable-bus block in front of the
     * bus carries the export bus and a quartz facade on top; dust on the facade takes the hatch's output and powers the
     * cable block below it (dust always powers the block it stands on); it runs on along s to a lamp. A sign.
     */
    private static void kerrStation(World world, Frame F, DirectiveSet.Output bit, int window, String... sign) {
        directive(world, F.at(0, 1, 0), F.o, DirectiveSet.KERR_HOLE, bit, window);
        solid(world, F.at(1, 0, 1));
        dust(world, F.at(1, 1, 0));
        dust(world, F.at(1, 1, 1));
        lamp(world, F.at(1, 1, 2));
        sign(world, F.at(2, 0, 1), F.o, sign);
    }

    // ===================================================================================== blocks

    private static void solid(World world, int[] p) {
        if (world.isAirBlock(p[0], p[1], p[2])) world.setBlock(p[0], p[1], p[2], Blocks.quartz_block, 0, 3);
    }

    private static void dust(World world, int[] p) {
        world.setBlock(p[0], p[1], p[2], Blocks.redstone_wire, 0, 3);
    }

    private static void lamp(World world, int[] p) {
        world.setBlock(p[0], p[1], p[2], Blocks.redstone_lamp, 0, 3);
    }

    /**
     * Diode direction bits by OUTPUT side (1.7.10 BlockRedstoneDiode: direction d takes its input from
     * Direction.offset[d] and powers the opposite neighbour): 0 north, 1 east, 2 south, 3 west.
     */
    private static int diodeDir(ForgeDirection out) {
        switch (out) {
            case NORTH:
                return 0;
            case EAST:
                return 1;
            case SOUTH:
                return 2;
            default:
                return 3;
        }
    }

    private static void repeater(World world, int[] p, ForgeDirection out, int delay) {
        world.setBlock(p[0], p[1], p[2], Blocks.unpowered_repeater, diodeDir(out) | (delay << 2), 3);
    }

    private static void comparator(World world, int[] p, ForgeDirection out, boolean subtract) {
        world.setBlock(p[0], p[1], p[2], Blocks.unpowered_comparator, diodeDir(out) | (subtract ? 4 : 0), 3);
    }

    /** A wall torch pointing {@code out} (attached to the block behind it). */
    private static void torch(World world, int[] p, ForgeDirection out) {
        final int meta;
        switch (out) {
            case EAST:
                meta = 1;
                break;
            case WEST:
                meta = 2;
                break;
            case SOUTH:
                meta = 3;
                break;
            default:
                meta = 4;
                break;
        }
        world.setBlock(p[0], p[1], p[2], Blocks.redstone_torch, meta, 3);
    }

    /** A wall sign facing {@code out} (attached to the block behind it), up to four lines of 15 characters. */
    private static void sign(World world, int[] p, ForgeDirection out, String... lines) {
        if (!world.isAirBlock(p[0], p[1], p[2])) {
            buildLog.add("sign skipped at " + RenderTest.pos(p) + " (not air)");
            return;
        }
        final int meta;
        switch (out) {
            case NORTH:
                meta = 2;
                break;
            case SOUTH:
                meta = 3;
                break;
            case WEST:
                meta = 4;
                break;
            default:
                meta = 5;
                break;
        }
        world.setBlock(p[0], p[1], p[2], Blocks.wall_sign, meta, 3);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof TileEntitySign) {
            final TileEntitySign s = (TileEntitySign) te;
            for (int i = 0; i < 4; i++) s.signText[i] = i < lines.length ? cut(lines[i]) : "";
            s.markDirty();
            world.markBlockForUpdate(p[0], p[1], p[2]);
        }
    }

    private static String cut(String s) {
        return s.length() > 15 ? s.substring(0, 15) : s;
    }

    /** A GT Debug Fluid Tank (endless: it holds Integer.MAX_VALUE of its fluid every tick). */
    private static void tank(World world, int[] p, ForgeDirection facing, Fluid fluid) {
        final ItemStack stack = ItemList.Debug_Fluid_Tank.get(1L);
        MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing);
        final MTEBasicTank t = mteAt(world, p, MTEBasicTank.class);
        if (t == null || fluid == null) {
            buildLog.add("COULD NOT place a Debug Fluid Tank at " + RenderTest.pos(p));
            return;
        }
        t.setFillableStack(new FluidStack(fluid, 1000));
    }

    private static Cover cover(World world, int[] p, ForgeDirection side, ItemStack item) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity) || item == null) return null;
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        final Cover c = CoverRegistry.buildCover(item, side, base);
        base.attachCover(c);
        return base.getCoverAtSide(side);
    }

    private static void regulator(World world, int[] p, ForgeDirection side, ItemList item, int litresPerTick) {
        final Cover c = cover(world, p, side, item.get(1L));
        if (!(c instanceof CoverFluidRegulator)) {
            buildLog.add("COULD NOT fit a Fluid Regulator at " + RenderTest.pos(p) + " side " + side);
            return;
        }
        final CoverFluidRegulator r = (CoverFluidRegulator) c;
        r.setTickRateForUi(1);
        r.setSpeed(litresPerTick);
        r.setCondition(CoverFluidRegulator.Conditional.Conditional);
        issue(world, p, side);
    }

    private static void controller(World world, int[] p, ForgeDirection side) {
        final Cover c = cover(world, p, side, ItemList.Cover_Controller.get(1L));
        if (!(c instanceof CoverControlsWork)) {
            buildLog.add("COULD NOT fit a Machine Controller at " + RenderTest.pos(p) + " side " + side);
            return;
        }
        ((CoverControlsWork) c).setRedstoneCondition(RedstoneCondition.ENABLE_WITH_REDSTONE);
        issue(world, p, side);
    }

    private static void issue(World world, int[] p, ForgeDirection side) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof IGregTechTileEntity) ((IGregTechTileEntity) te).issueCoverUpdate(side);
    }

    // ===================================================================================== access

    static MTEEyeOfHarmonyBTS eye() {
        return eyeCtrl == null ? null : mteAt(RenderTest.world(), eyeCtrl, MTEEyeOfHarmonyBTS.class);
    }

    static MTEKerrEngine kee() {
        return keeCtrl == null ? null : KerrTestBench.keeAt(RenderTest.world(), keeCtrl[0], keeCtrl[1], keeCtrl[2]);
    }

    static <T> T mteAt(World world, int[] p, Class<T> type) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    static int[] grow(int[] box, int r) {
        return new int[] { box[0] - r, 0, box[2] - r, box[3] + r, 255, box[5] + r };
    }

    /** Seconds since the rigs were built. */
    static long seconds() {
        return t0Tick < 0 ? 0 : (serverTick - t0Tick) / 20;
    }

    // ===================================================================================== logs

    private static PrintWriter eyeTsv, kerrTsv;
    private static boolean hooked;

    private static void openLogs() {
        final File dir = MinecraftServer.getServer()
            .getFile("screenshots/autorig");
        if (!dir.isDirectory() && !dir.mkdirs()) BeyondTheStars.LOG.error("[BTS autorig] cannot create " + dir);
        try {
            eyeTsv = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "eye.tsv")), StandardCharsets.UTF_8),
                true);
            kerrTsv = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "kerr.tsv")), StandardCharsets.UTF_8),
                true);
            eyeTsv.println(
                "t\tstate\tP\tzone\tintegrity\tY\ttrims\tstatus\ttrans\the_dust\th_dust\the_valve\th_valve\the_in\th_in"
                    + "\tup_dust\tup_he_valve\tup_h_valve\tbuf_He\tbuf_H\tfield_He\tfield_H\tinband_lamp\ttrans_lamp"
                    + "\tcharges\trig\tevent\tfailure");
            kerrTsv.println(
                "t\topen\tmass\tsetpoint\tstock_bolts\tseeds_in_bus\thole_code\tbolt_dust\tseed_dust\tbolt_lamp"
                    + "\tseed_lamp\tme_bolts\tme_seeds\temitter_lamp\tcell");
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] cannot open the logs", bad);
        }
        if (!hooked) {
            hooked = true;
            FMLCommonHandler.instance()
                .bus()
                .register(new Sampler());
        }
    }

    /** Once a second: one line per rig. Reads only. */
    public static final class Sampler {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            serverTick++;
            if (!built || serverTick % 20 != 0) return;
            if (seconds() == 5 || seconds() == 40) dumpWires();
            try {
                sampleEye();
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS autorig] eye sample failed: " + bad);
            }
            try {
                sampleKerr();
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS autorig] kerr sample failed: " + bad);
            }
        }
    }

    /** Last sampled lines (the client logs them at each shot). */
    static volatile String lastEye = "", lastKerr = "";

    private static void sampleEye() {
        final MTEEyeOfHarmonyBTS eye = eye();
        if (eye == null || eyeTsv == null) return;
        final World world = RenderTest.world();
        final EyeStarRuntime rt = eye.starRuntime();
        final FieldReadout r = rt.lastReadout();
        final Map<String, Long> trims = rt.lastTrimsByName();
        final String line = String.join(
            "\t",
            String.valueOf(seconds()),
            String.valueOf(rt.state()),
            r.lit ? String.format(Locale.ROOT, "%.2f", r.x(StellarField.P)) : "",
            r.lit ? String.valueOf(r.zone) : "",
            r.lit ? String.format(Locale.ROOT, "%.1f", r.I) : "",
            r.lit ? String.format(Locale.ROOT, "%.3f", r.Y) : "",
            String.valueOf(code(world, fHe.at(0, 1, 0))),
            String.valueOf(code(world, fStatus.at(0, 1, 0))),
            String.valueOf(code(world, fTrans.at(0, 1, 0))),
            String.valueOf(world.getBlockMetadata(fHe.at(1, 1, 0)[0], fHe.at(1, 1, 0)[1], fHe.at(1, 1, 0)[2])),
            String.valueOf(world.getBlockMetadata(fH.at(1, 1, 0)[0], fH.at(1, 1, 0)[1], fH.at(1, 1, 0)[2])),
            valve(world, fHe.at(1, 0, 0)),
            valve(world, fH.at(1, 0, 0)),
            String.valueOf(trimLitres(trims, "Helium")),
            String.valueOf(trimLitres(trims, "Hydrogen")),
            String.valueOf(world.getBlockMetadata(upHe.at(3, 0, 0)[0], upHe.at(3, 0, 0)[1], upHe.at(3, 0, 0)[2])),
            valve(world, upHe.at(1, 0, 0)),
            valve(world, upH.at(1, 0, 0)),
            String.valueOf(eye.buffer(helium)),
            String.valueOf(eye.buffer(hydrogen)),
            String.valueOf(hatchFluid(world, fHe.p)),
            String.valueOf(hatchFluid(world, fH.p)),
            lit(world, fNot.at(1, 1, 4)) ? "1" : "0",
            lit(world, fTrans.at(2, 1, 0)) ? "1" : "0",
            String.valueOf(charges(eye)),
            rt.rig() == null ? "none" : String.valueOf(rt.rig()),
            rt.lastEvent()
                .replace('\t', ' '),
            rt.lastFailure()
                .replace('\t', ' '));
        eyeTsv.println(line);
        lastEye = line;
    }

    private static void sampleKerr() {
        final MTEKerrEngine kee = kee();
        if (kee == null || kerrTsv == null) return;
        final World world = RenderTest.world();
        final ItemStack bolt = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 1L);
        final ItemStack seed = ItemList.Black_Hole_Opener.get(1L);
        final String line = String.join(
            "\t",
            String.valueOf(seconds()),
            kee.isHoleOpen() ? "1" : "0",
            String.format(Locale.ROOT, "%.3f", kee.getMass()),
            String.valueOf(kee.getSetpoint()),
            String.valueOf(inBuses(kee, bolt)),
            String.valueOf(inBuses(kee, seed)),
            String.valueOf(code(world, kBolt.at(0, 1, 0))),
            String.valueOf(world.getBlockMetadata(kBolt.at(1, 1, 0)[0], kBolt.at(1, 1, 0)[1], kBolt.at(1, 1, 0)[2])),
            String.valueOf(world.getBlockMetadata(kSeed.at(1, 1, 0)[0], kSeed.at(1, 1, 0)[1], kSeed.at(1, 1, 0)[2])),
            lit(world, kBolt.at(1, 1, 2)) ? "1" : "0",
            lit(world, kSeed.at(1, 1, 2)) ? "1" : "0",
            String.valueOf(AutoRigAE2.meCount(world, bolt)),
            String.valueOf(AutoRigAE2.meCount(world, seed)),
            AutoRigAE2.emitterLampLit(world) ? "1" : "0",
            AutoRigAE2.cellIn() ? "1" : "0");
        kerrTsv.println(line);
        lastKerr = line;
    }

    /** Diagnostics: every Status-line cell (block, meta) and the power the world sees at each station's key cells. */
    private static void dumpWires() {
        final World world = RenderTest.world();
        final File f = MinecraftServer.getServer()
            .getFile("screenshots/autorig/wires-" + seconds() + ".txt");
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            final int[] n = fStatus.at(0, 1, 0);
            w.println(
                "status hatch " + RenderTest
                    .pos(n) + " code " + code(world, n) + ", o " + fStatus.o + ", s " + fStatus.s);
            for (int[] q : statusWire) {
                w.println(
                    RenderTest.pos(q) + " "
                        + Block.blockRegistry.getNameForObject(world.getBlock(q[0], q[1], q[2]))
                        + " meta "
                        + world.getBlockMetadata(q[0], q[1], q[2])
                        + " power-in "
                        + world.getStrongestIndirectPower(q[0], q[1], q[2])
                        + " below "
                        + Block.blockRegistry.getNameForObject(world.getBlock(q[0], q[1] - 1, q[2])));
            }
            for (Frame g : new Frame[] { upHe, upH }) {
                for (int[] q : new int[][] { g.at(1, 0, 0), g.at(2, 0, 0), g.at(3, 0, 0), g.at(4, -1, 0),
                    g.at(2, -1, 0), g.at(3, -1, 0) }) {
                    w.println(
                        "upkeep " + RenderTest.pos(q)
                            + " "
                            + Block.blockRegistry.getNameForObject(world.getBlock(q[0], q[1], q[2]))
                            + " meta "
                            + world.getBlockMetadata(q[0], q[1], q[2])
                            + " power-in "
                            + world.getStrongestIndirectPower(q[0], q[1], q[2]));
                }
            }
            for (Frame k : new Frame[] { kBolt, kSeed }) {
                if (k == null) continue;
                for (int[] q : new int[][] { k.at(0, -1, 0), k.at(1, -1, 0), k.at(1, 0, 1), k.at(1, 0, 0),
                    k.at(1, 0, 2) }) {
                    w.println(
                        "kerr " + RenderTest.pos(q)
                            + " "
                            + Block.blockRegistry.getNameForObject(world.getBlock(q[0], q[1], q[2]))
                            + " meta "
                            + world.getBlockMetadata(q[0], q[1], q[2])
                            + " power-in "
                            + world.getStrongestIndirectPower(q[0], q[1], q[2])
                            + " powered "
                            + world.isBlockIndirectlyGettingPowered(q[0], q[1], q[2]));
                }
            }
        } catch (IOException bad) {
            BeyondTheStars.LOG.warn("[BTS autorig] wire dump failed: " + bad);
        }
    }

    private static int code(World world, int[] p) {
        final MTEHatchDirective h = mteAt(world, p, MTEHatchDirective.class);
        return h == null ? -1 : h.currentCode();
    }

    private static String valve(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof IGregTechTileEntity ? (((IGregTechTileEntity) te).isAllowedToWork() ? "1" : "0") : "-";
    }

    private static long trimLitres(Map<String, Long> trims, String name) {
        if (trims == null) return 0;
        for (Map.Entry<String, Long> e : trims.entrySet()) {
            if (e.getKey() != null && e.getKey()
                .contains(name)) return e.getValue();
        }
        return 0;
    }

    private static int hatchFluid(World world, int[] p) {
        final MTEHatchInput h = mteAt(world, p, MTEHatchInput.class);
        return h == null || h.getFluid() == null ? 0 : h.getFluid().amount;
    }

    static boolean lit(World world, int[] p) {
        return world.getBlock(p[0], p[1], p[2]) == Blocks.lit_redstone_lamp;
    }

    private static int charges(MTEEyeOfHarmonyBTS eye) {
        int n = 0;
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                final ItemStack s = bus.getStackInSlot(i);
                if (s != null && s.getItem() == SLItems.stellarIgnitionCharge) n += s.stackSize;
            }
        }
        return n;
    }

    private static int inBuses(MTEKerrEngine kee, ItemStack what) {
        if (what == null) return 0;
        int n = 0;
        for (MTEHatchInputBus bus : kee.boltBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                final ItemStack s = bus.getStackInSlot(i);
                if (s != null && s.getItem() == what.getItem() && s.getItemDamage() == what.getItemDamage())
                    n += s.stackSize;
            }
        }
        return n;
    }

    // ===================================================================================== camera

    /** Moves the player to {@code eye} looking at {@code target}; day or night. Returns a log line. */
    static String view(double[] eye, double[] target, boolean night) {
        final WorldServer world = RenderTest.world();
        world.setWorldTime(night ? 18000 : 6000);
        final EntityPlayerMP player = RenderTest.player();
        if (player == null) return "no player";
        double[] at = eye.clone();
        final double[] back = norm(new double[] { at[0] - target[0], at[1] - target[1], at[2] - target[2] });
        int pushed = 0;
        while (pushed < 40 && !clear(world, at)) {
            at = new double[] { at[0] + back[0], at[1] + back[1], at[2] + back[2] };
            pushed++;
        }
        final double dx = target[0] - at[0], dy = target[1] - at[1], dz = target[2] - at[2];
        final float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        final float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        RenderTest.fly(player);
        final int ex = (int) Math.floor(at[0]), ez = (int) Math.floor(at[2]);
        RenderTest.loadAndKeep(world, new int[] { ex - 48, 0, ez - 48, ex + 48, 255, ez + 48 });
        player.playerNetServerHandler.setPlayerLocation(at[0], at[1] - 1.62, at[2], yaw, pitch);
        player.motionX = player.motionY = player.motionZ = 0;
        world.getPlayerManager()
            .updatePlayerPertinentChunks(player);
        return String.format(
            Locale.ROOT,
            "%s: eye (%.1f, %.1f, %.1f) -> (%.1f, %.1f, %.1f)%s",
            night ? "night" : "day",
            at[0],
            at[1],
            at[2],
            target[0],
            target[1],
            target[2],
            pushed > 0 ? ", pushed " + pushed : "");
    }

    private static boolean clear(World world, double[] eye) {
        final int x = (int) Math.floor(eye[0]), z = (int) Math.floor(eye[2]);
        for (int y = (int) Math.floor(eye[1] - 1.62); y <= (int) Math.floor(eye[1]); y++) {
            if (y < 0 || y > 255) continue;
            final Block b = world.getBlock(x, y, z);
            if (!b.isAir(world, x, y, z)) return false;
        }
        return true;
    }

    private static double[] norm(double[] v) {
        final double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-9 ? new double[] { 0, 0, -1 } : new double[] { v[0] / l, v[1] / l, v[2] / l };
    }

    // ===================================================================================== GUIs

    /** Opens the MTE's GUI at {@code p} for the player (server side; the player must be within 8 blocks). */
    static String openGui(int[] p) {
        final EntityPlayerMP player = RenderTest.player();
        final TileEntity te = RenderTest.world()
            .getTileEntity(p[0], p[1], p[2]);
        if (player == null || !(te instanceof IGregTechTileEntity))
            return "no player or no machine at " + RenderTest.pos(p);
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(mte instanceof gregtech.api.metatileentity.CommonMetaTileEntity)) return "not a GT MTE";
        ((gregtech.api.metatileentity.CommonMetaTileEntity) mte).openGui(player);
        return "GUI opened: " + mte.getClass()
            .getSimpleName() + " at " + RenderTest.pos(p);
    }

    /** One status line for the shot log. */
    static String status() {
        return "t=" + seconds() + " s | eye: " + lastEye + " | kerr: " + lastKerr;
    }

    /** Server-side condition: the Eye's star is LIT (or burning). */
    static boolean eyeLit() {
        final MTEEyeOfHarmonyBTS eye = eye();
        return eye != null && eye.starRuntime()
            .burning();
    }

    /** Seconds the Eye's star has burned (counted by the sampler's state column). */
    static long litSince = -1;

    static long litSeconds() {
        if (eyeLit()) {
            if (litSince < 0) litSince = seconds();
            return seconds() - litSince;
        }
        return litSince < 0 ? 0 : seconds() - litSince;
    }

    static void closeLogs() {
        if (eyeTsv != null) eyeTsv.close();
        if (kerrTsv != null) kerrTsv.close();
        built = false;
    }
}
