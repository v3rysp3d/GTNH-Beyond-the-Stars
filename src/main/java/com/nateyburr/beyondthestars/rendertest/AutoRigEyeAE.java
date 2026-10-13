package com.nateyburr.beyondthestars.rendertest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;

import appeng.api.AEApi;
import appeng.api.storage.StorageChannel;
import appeng.api.util.AEColor;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * DEV-ONLY AE2 EYE RIG ("Rig C"), built by the video recorder ({@code build eyeae}, docs/recording.md). In normal play
 * nothing here is loaded.
 * <p>
 * An Eye of Harmony T1 (Sol) raised by its kit, dark, with empty buffers, fed and held ONLY through one AE2 network:
 * <ul>
 * <li><b>Upkeep and ignition gas:</b> an ME stocking input hatch in the front-right gas socket, set to Helium and
 * Hydrogen. The Eye drains it into its buffers every second (docs/automation.md §2.3).</li>
 * <li><b>Ignition charge:</b> an ME stocking input bus in the front-left gas socket, set to the Stellar Ignition
 * Charge. A dark Eye with full buffers takes one from it and lights itself.</li>
 * <li><b>Trims:</b> two Directive Hatches (Trims, Bit 1 and Bit 2) on the band, each over a small (LV) Field input
 * hatch;
 * in front of each Field hatch an AE2FC fluid export bus (Helium / Hydrogen) with a redstone card ("active with
 * signal"). One redstone dust on the bus block's facade carries the hatch's bit onto the bus.</li>
 * <li><b>Products:</b> the kit's ME output bus and ME output hatch beside the controller, cabled in, so the star's
 * output lands in the same network.</li>
 * <li><b>The core:</b> a creative-energy ME controller, an ME drive, and a level emitter with a lamp (lit while the
 * network holds an ignition charge). Orange dense cable: a ground trunk in front of the Eye, two risers to the
 * band.</li>
 * </ul>
 * <b>The gas supply</b> stands in for the player's factory: two GT Debug Fluid Tanks (endless) behind AE2FC fluid
 * storage buses set to read only. They are empty, and the drive holds no cell, until {@link #insertCells} (the
 * recorder's {@code aecells}): the tanks get Helium and Hydrogen and the drive a cell of Stellar Ignition Charges,
 * all set up by script. Endless tanks, because before B-108 a finite gas cell did not work here: the ME stocking hatch
 * shows the Eye up to 2.1 G L a second and the Eye's buffers took everything (2^62 L), so a network of cells was
 * emptied
 * into the buffers within seconds and the trim buses on it ran dry (seen in the first test). B-108 caps the intake
 * (1 G L of each gas for Sol); the rig keeps its endless tanks and separate trim network. The
 * Eye's power is the owner's wireless EU, also set by script. From then on nothing touches the machine: a sampler
 * only reads ({@code eye-ae.tsv}).
 */
final class AutoRigEyeAE {

    private AutoRigEyeAE() {}

    static final int X = 2400, Z = 4000;
    static final AEColor COLOR = AEColor.Orange;
    static final int CHARGES = 8;
    /** Acceleration cards on each trim bus (3 = 4,000 L an operation); -Dbts.autorig.trimcards to try others. */
    static final int TRIM_CARDS = Integer.getInteger("bts.autorig.trimcards", 3);

    static int[] ctrl, box;
    static double[] centre;
    static AutoRig.Frame fHe, fH;
    static int[] stockHatch, stockBus, outBus, outHatch;
    static int[] meCtrl, drive, emitterHost, emitterLamp, trunkWest, trunkEast;
    static int[] trimBusHe, trimBusH;
    /** The two supply tanks (Helium, Hydrogen) and their storage-bus cable cells. */
    static int[] tankHe, tankH, sbusHe, sbusH;
    /** The trims' own network: its controller and supply tanks. */
    static int[] trimCtrl, trimTankHe, trimTankH;
    static final AEColor TRIM_COLOR = AEColor.LightBlue;
    static volatile boolean cellsIn;
    static volatile long litAt = -1, cellsAt = -1;
    private static long tick, t0;
    private static PrintWriter tsv;
    private static boolean hooked;
    static volatile String last = "";
    static final List<String> log = new ArrayList<>();
    static final List<int[]> cables = new ArrayList<>();

    static List<String> build(WorldServer world) {
        log.clear();
        cables.clear();
        cellsIn = false;
        litAt = -1;
        cellsAt = -1;
        AutoRig.initFluids();
        try {
            buildEye(world);
            buildMe(world);
        } catch (RuntimeException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] Rig C failed", bad);
            log.add("RIG C FAILED: " + bad);
        }
        openLog();
        return new ArrayList<>(log);
    }

    // ------------------------------------------------------------------------------------- the Eye

    private static void buildEye(WorldServer world) {
        final String[] pieces = { MTEEyeOfHarmonyBTS.PIECES[0] };
        final int[] probe = SLConsoleTest.bounds(ExtendedFacing.of(AutoRig.FACING), X, 100, Z, pieces);
        final int y = 100 + AutoRig.GROUND - probe[1];
        box = SLConsoleTest.bounds(ExtendedFacing.of(AutoRig.FACING), X, y, Z, pieces);
        RenderTest.loadAndKeep(world, AutoRig.grow(box, 90));
        ctrl = new int[] { X, y, Z };
        final EyeKit.Result r = EyeKit
            .build(world, X, y, Z, AutoRig.FACING, 1, 0, false, AutoRig.OWNER, "BTS autorig", false);
        if (r.failure != null) throw new IllegalStateException("Eye build: " + r.failure);
        for (String line : r.lines) log.add("C: " + RenderTest.strip(line));
        final MTEEyeOfHarmonyBTS eye = eye();
        if (eye == null) throw new IllegalStateException("no Eye after the build");
        final int[] c = com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender
            .centrePos(eye.getExtendedFacing(), X, y, Z);
        centre = new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 };
        WirelessNetworkManager.strongCheckOrAddUser(AutoRig.OWNER);
        WirelessNetworkManager.addEUToGlobalEnergyMap(AutoRig.OWNER, BigInteger.TEN.pow(16));
        log.add("C: owner's wireless EU network: 10^16 EU (set by script)");
        point("aeeye", centre);
        point("aeeyectrl", ctr(ctrl));
    }

    // ------------------------------------------------------------------------------------- the ME side

    private static void buildMe(WorldServer world) {
        final EntityPlayer player = RenderTest.player();
        final MTEEyeOfHarmonyBTS eye = eye();
        final ExtendedFacing al = eye.getExtendedFacing();
        final int[][] h = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h');
        final int[][] f = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'f');
        final int[][] o = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'o');
        final Fluid he = AutoRig.helium, hy = AutoRig.hydrogen;

        // 1. the gas sockets at the front: an ME stocking hatch (Helium, Hydrogen) and an ME stocking bus (charges)
        stockHatch = SLConsoleTest.worldPos(al, X, ctrl[1], Z, h[0]);
        stockBus = SLConsoleTest.worldPos(al, X, ctrl[1], Z, h[1]);
        final ForgeDirection hatchFace = facing(world, stockHatch), busFace = facing(world, stockBus);
        MTEReactorCoreMk1.placeGtMachine(
            world,
            stockHatch[0],
            stockHatch[1],
            stockHatch[2],
            (short) ItemList.Hatch_Input_ME.get(1L)
                .getItemDamage(),
            hatchFace);
        final MTEHatchInputME sh = AutoRig.mteAt(world, stockHatch, MTEHatchInputME.class);
        if (sh != null) {
            sh.setSlotConfig(0, new FluidStack(he, 1));
            sh.setSlotConfig(1, new FluidStack(hy, 1));
        }
        MTEReactorCoreMk1.placeGtMachine(
            world,
            stockBus[0],
            stockBus[1],
            stockBus[2],
            (short) ItemList.Hatch_Input_Bus_ME.get(1L)
                .getItemDamage(),
            busFace);
        final MTEHatchInputBusME sb = AutoRig.mteAt(world, stockBus, MTEHatchInputBusME.class);
        if (sb != null) sb.setSlotConfig(0, new ItemStack(SLItems.stellarIgnitionCharge, 1, 0));
        log.add(
            "C: ME stocking input hatch (Helium, Hydrogen) at " + RenderTest.pos(stockHatch)
                + " facing "
                + hatchFace
                + (sh == null ? " NOT PLACED" : "")
                + "; ME stocking input bus (Stellar Ignition Charge) at "
                + RenderTest.pos(stockBus)
                + " facing "
                + busFace
                + (sb == null ? " NOT PLACED" : ""));
        outBus = SLConsoleTest.worldPos(al, X, ctrl[1], Z, o[0]);
        outHatch = SLConsoleTest.worldPos(al, X, ctrl[1], Z, o[1]);

        // 2. the trim stations on the band: Directive Hatch (Trims, bit) over the Field hatch, fluid export bus in
        // front
        AutoRig.buildingFor = ctrl;
        try {
            fHe = AutoRig.frame(world, al, f[1], 0, 1, 2, 3);
            fH = AutoRig.frame(world, al, f[2], 0, 1, 2, 3);
        } finally {
            AutoRig.buildingFor = null;
        }
        trimBusHe = trimStation(world, fHe, he, DirectiveSet.Output.BIT1, player);
        trimBusH = trimStation(world, fH, hy, DirectiveSet.Output.BIT2, player);

        // 3. the ground trunk in front of the Eye (the controller's level, three blocks out) and the core on it
        final int y = ctrl[1];
        final int zT = Z - 3;
        final int west = Math.min(stockHatch[0], stockBus[0]) - 1;
        final int east = Math.max(stockHatch[0], stockBus[0]) + 1;
        trunkWest = new int[] { west, y, zT };
        trunkEast = new int[] { east, y, zT };
        lay(world, MeParts.line(trunkWest, trunkEast), MeParts.Kind.DENSE_COVERED, player, "ground trunk");
        // stubs from the trunk to the front face of each ME hatch (smart cable: the channels show)
        stub(world, new int[] { stockHatch[0], y, zT }, front(stockHatch, hatchFace), player, "stocking hatch");
        stub(world, new int[] { stockBus[0], y, zT }, front(stockBus, busFace), player, "stocking bus");
        stub(world, new int[] { outBus[0], y, zT }, front(outBus, facing(world, outBus)), player, "ME output bus");
        stub(
            world,
            new int[] { outHatch[0], y, zT },
            front(outHatch, facing(world, outHatch)),
            player,
            "ME output hatch");
        // the core: a creative-energy ME controller north of the trunk, the drive on top of it, a level emitter
        meCtrl = new int[] { X - 12, y, zT - 1 };
        drive = new int[] { X - 12, y + 1, zT - 1 };
        MeParts.block(world, meCtrl, MeParts.controllerBlock());
        MeParts.block(world, drive, MeParts.driveBlock());
        // the level emitter on a smart-cable branch north of the trunk (parts cannot sit on dense cable)
        emitterHost = new int[] { X - 9, y, zT - 1 };
        emitterLamp = new int[] { X - 9, y, zT - 2 };
        MeParts.levelEmitter(
            world,
            emitterHost,
            ForgeDirection.NORTH,
            new ItemStack(SLItems.stellarIgnitionCharge, 1, 0),
            1,
            MeParts.Kind.SMART,
            COLOR,
            player,
            log,
            "C: ");
        world.setBlock(emitterLamp[0], emitterLamp[1], emitterLamp[2], net.minecraft.init.Blocks.redstone_lamp, 0, 3);
        // the gas supply: a Debug Fluid Tank behind a read-only fluid storage bus on a stub off the trunk, each
        for (int i = 0; i < 2; i++) {
            final int x = X - 18 - 3 * i;
            final int[] sbc = { x, y, zT - 1 }, tank = { x, y, zT - 2 };
            MeParts.cable(world, sbc, MeParts.Kind.SMART, COLOR, player);
            cables.add(sbc);
            MeParts.storageBus(world, sbc, ForgeDirection.NORTH, player, log, "C: ");
            MTEReactorCoreMk1.placeGtMachine(
                world,
                tank[0],
                tank[1],
                tank[2],
                (short) ItemList.Debug_Fluid_Tank.get(1L)
                    .getItemDamage(),
                ForgeDirection.SOUTH);
            if (i == 0) {
                sbusHe = sbc;
                tankHe = tank;
            } else {
                sbusH = sbc;
                tankH = tank;
            }
        }
        log.add(
            "C: gas supply: Debug Fluid Tanks (Helium " + RenderTest.pos(tankHe)
                + ", Hydrogen "
                + RenderTest.pos(tankH)
                + ") behind read-only fluid storage buses; empty until aecells");
        // 4. the trims' own network (light blue: cables of different colours do not join): a second trunk three
        // blocks further out, its own creative-energy controller and its own supply, and a riser up to each trim
        // station. Before B-108 the Eye's stocking hatch pulled every litre on the main network into the Eye's buffers
        // (it shows the Eye up to 2.1 G L a second), and the trim buses found nothing; the intake cap fixed that, the
        // separate network stays (the recorded rig).
        final int zT2 = zT - 3;
        final int[] w2 = { Math.min(trimBusHe[0], trimBusH[0]), y, zT2 },
            e2 = { Math.max(trimBusHe[0], trimBusH[0]), y, zT2 };
        layC(world, MeParts.line(w2, e2), MeParts.Kind.DENSE_COVERED, TRIM_COLOR, player, "trim trunk");
        trimCtrl = new int[] { X + 12, y, zT2 - 1 };
        MeParts.block(world, trimCtrl, MeParts.controllerBlock());
        for (int i = 0; i < 2; i++) {
            final int x = X + 16 + 3 * i;
            final int[] sbc = { x, y, zT2 - 1 }, tank = { x, y, zT2 - 2 };
            MeParts.cable(world, sbc, MeParts.Kind.SMART, TRIM_COLOR, player);
            cables.add(sbc);
            MeParts.storageBus(world, sbc, ForgeDirection.NORTH, player, log, "C: ");
            MTEReactorCoreMk1.placeGtMachine(
                world,
                tank[0],
                tank[1],
                tank[2],
                (short) ItemList.Debug_Fluid_Tank.get(1L)
                    .getItemDamage(),
                ForgeDirection.SOUTH);
            if (i == 0) trimTankHe = tank;
            else trimTankH = tank;
        }
        for (int[] bus : new int[][] { trimBusHe, trimBusH }) riser(world, bus, zT2, player);
        log.add(
            "C: trim network: controller " + RenderTest
                .pos(trimCtrl) + ", supply tanks " + RenderTest.pos(trimTankHe) + " / " + RenderTest.pos(trimTankH));

        log.add(
            "C: ME core: creative-energy controller " + RenderTest.pos(meCtrl)
                + ", drive (empty until aecells) "
                + RenderTest.pos(drive)
                + ", level emitter "
                + RenderTest.pos(emitterHost)
                + " (lamp lit while the network holds a charge); "
                + cables.size()
                + " cable cells");
        eye.checkStructure(true, eye.getBaseMetaTileEntity());
        log.add(
            "C: Eye re-walked: formed " + eye.mMachine
                + ", Directive Hatches "
                + eye.directiveHatchCount()
                + ", Field hatches "
                + eye.fieldHatches()
                    .size());

        point("ae.core", ctr(meCtrl));
        point("ae.trimcore", ctr(trimCtrl));
        point("ae.trimtank", ctr(trimTankHe));
        point("ae.trimtrunk", ctr(new int[] { X, y, zT2 }));
        point("ae.tankhe", ctr(tankHe));
        point("ae.tankh", ctr(tankH));
        point("ae.drive", ctr(drive));
        point("ae.emitter", ctr(emitterLamp));
        point("ae.stockhatch", ctr(stockHatch));
        point("ae.stockbus", ctr(stockBus));
        point("ae.outbus", ctr(outBus));
        point("ae.outhatch", ctr(outHatch));
        point("ae.trimhe", ctr(trimBusHe));
        point("ae.trimh", ctr(trimBusH));
        point("ae.trimhe.cam", fHe.atD(9, 3, 4));
        point("ae.trimh.cam", fH.atD(9, 3, 4));
        point("ae.dirhe", ctr(fHe.at(0, 1, 0)));
        point("ae.dirh", ctr(fH.at(0, 1, 0)));
        point("ae.fieldhe", ctr(fHe.p));
        point("ae.lamphe", ctr(fHe.at(1, 1, 2)));
        point("ae.lamph", ctr(fH.at(1, 1, 2)));
        point("ae.trunk", ctr(new int[] { X, y, zT }));
        point("ae.riserhe", new double[] { trimBusHe[0] + 0.5, (y + trimBusHe[1]) / 2.0, zT2 + 0.5 });
        point("ae.riserh", new double[] { trimBusH[0] + 0.5, (y + trimBusH[1]) / 2.0, zT2 + 0.5 });
    }

    /**
     * A trim station: the kit's Directive Hatch above the Field hatch set to Trims in Bit mode; the fluid export bus
     * on the cell in front of the Field hatch; dust on its facade (from the hatch's face down onto the bus block) and
     * on along s over a quartz block to a lamp (lit = the trim valve is open). Returns the bus cell.
     */
    private static int[] trimStation(World world, AutoRig.Frame F, Fluid fluid, DirectiveSet.Output bit,
        EntityPlayer player) {
        AutoRig.directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_TRIMS, bit, 10);
        // a SMALL Field hatch (LV, 16,000 L): the Eye drains up to 10,000 L of a trim a second, and a bus faster than
        // that would stockpile trim in a big hatch that the Eye keeps draining after the bit goes off (seen with the
        // kit's MAX hatch: both trims ran at once and the pressure stuck near +35)
        MTEReactorCoreMk1.placeGtMachine(
            world,
            F.p[0],
            F.p[1],
            F.p[2],
            (short) ItemList.Hatch_Input_LV.get(1L)
                .getItemDamage(),
            F.o);
        final int[] bus = F.at(1, 0, 0);
        MeParts.exportBus(
            world,
            bus,
            F.o.getOpposite(),
            MeParts.fluid(fluid, 1),
            TRIM_CARDS,
            true,
            true,
            MeParts.Kind.SMART,
            TRIM_COLOR,
            player,
            log,
            "C: ");
        cables.add(bus);
        AutoRig.solid(world, F.at(1, 0, 1));
        AutoRig.dust(world, F.at(1, 1, 0));
        AutoRig.dust(world, F.at(1, 1, 1));
        AutoRig.lamp(world, F.at(1, 1, 2));
        log.add(
            "C: trim station " + fluid.getName()
                + ": Directive Hatch (Trims, "
                + bit
                + ") "
                + RenderTest.pos(F.at(0, 1, 0))
                + " over the Field hatch (LV, 16,000 L) "
                + RenderTest.pos(F.p)
                + ", o "
                + F.o
                + ", s "
                + F.s);
        return bus;
    }

    /** From the trunk at the bus's x straight up to the bus's height, then along to the bus (routed if blocked). */
    private static void riser(World world, int[] bus, int zT, EntityPlayer player) {
        final int[] foot = { bus[0], ctrl[1], zT };
        final int[] top = { bus[0], bus[1], zT };
        final Set<Long> avoid = new HashSet<>();
        // keep the cable off the station's redstone cells
        for (AutoRig.Frame F : new AutoRig.Frame[] { fHe, fH }) {
            for (int dO = 0; dO <= 2; dO++) for (int dY = 1; dY <= 2; dY++)
                for (int dS = -1; dS <= 3; dS++) avoid.add(MeParts.key(F.at(dO, dY, dS)));
            for (int dS = 1; dS <= 3; dS++) avoid.add(MeParts.key(F.at(1, 0, dS)));
        }
        final List<int[]> up = MeParts.line(foot, top);
        final List<int[]> along = MeParts.line(top, bus);
        if (MeParts.clear(world, up, avoid) && along != null
            && MeParts.clear(world, along.subList(0, along.size() - 1), avoid)) {
            layC(world, up, MeParts.Kind.DENSE_COVERED, TRIM_COLOR, player, "riser");
            layC(
                world,
                along.subList(0, along.size() - 1),
                MeParts.Kind.DENSE_COVERED,
                TRIM_COLOR,
                player,
                "riser top");
            return;
        }
        final List<int[]> path = MeParts.route(world, foot, bus, avoid, 14, AutoRig.GROUND);
        if (path == null) {
            log.add("C: NO RISER ROUTE to " + RenderTest.pos(bus));
            return;
        }
        layC(world, path.subList(0, path.size() - 1), MeParts.Kind.DENSE_COVERED, TRIM_COLOR, player, "riser (routed)");
    }

    private static void stub(World world, int[] from, int[] to, EntityPlayer player, String what) {
        final List<int[]> l = MeParts.line(from, to);
        if (l != null && MeParts.clear(world, l, null)) {
            lay(world, l, MeParts.Kind.SMART, player, "stub to the " + what);
            return;
        }
        final List<int[]> path = MeParts.route(world, from, to, null, 14, AutoRig.GROUND);
        if (path == null) log.add("C: NO CABLE ROUTE to the " + what + " at " + RenderTest.pos(to));
        else lay(world, path, MeParts.Kind.SMART, player, "stub to the " + what + " (routed)");
    }

    private static void lay(World world, List<int[]> path, MeParts.Kind kind, EntityPlayer player, String what) {
        layC(world, path, kind, COLOR, player, what);
    }

    private static void layC(World world, List<int[]> path, MeParts.Kind kind, AEColor color, EntityPlayer player,
        String what) {
        if (path == null) return;
        MeParts.run(world, path, kind, color, player);
        cables.addAll(path);
        log.add(
            "C: " + what
                + ": "
                + path.size()
                + " cells "
                + RenderTest.pos(path.get(0))
                + " -> "
                + RenderTest.pos(path.get(path.size() - 1)));
    }

    private static ForgeDirection facing(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getFrontFacing() : ForgeDirection.NORTH;
    }

    private static int[] front(int[] p, ForgeDirection d) {
        return new int[] { p[0] + d.offsetX, p[1] + d.offsetY, p[2] + d.offsetZ };
    }

    static double[] ctr(int[] p) {
        return new double[] { p[0] + 0.5, p[1] + 0.5, p[2] + 0.5 };
    }

    private static void point(String name, double[] p) {
        AutoRig.POINTS.put(name, p);
    }

    // ------------------------------------------------------------------------------------- any kit Eye's outputs

    /**
     * An ME network on the products of the Eye whose controller is at {@code ctrl} (built by its dev kit, which fits an
     * ME output bus and an ME output hatch in the core's 'o' sockets): a smart-cable trunk three blocks in front of the
     * controller with a stub to each, a creative-energy ME controller and a drive at its west end holding an empty item
     * cell and an empty fluid cell, and an ME terminal on the trunk facing north (the side a camera stands on).
     * Registers {@code <name>.term} (the terminal's cable cell), {@code <name>.termcam} (a camera spot 3 blocks north
     * of
     * it) and {@code <name>.mecore}. Set up by script: the player's own network would do the same.
     */
    static List<String> outputNet(WorldServer world, int[] eyeCtrl, String name, AEColor color) {
        final List<String> out = new ArrayList<>();
        final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(world, eyeCtrl, MTEEyeOfHarmonyBTS.class);
        if (eye == null) {
            out.add("no Eye at " + RenderTest.pos(eyeCtrl));
            return out;
        }
        final EntityPlayer player = RenderTest.player();
        final ExtendedFacing al = eye.getExtendedFacing();
        final int[][] o = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'o');
        final int[] ob = SLConsoleTest.worldPos(al, eyeCtrl[0], eyeCtrl[1], eyeCtrl[2], o[0]);
        final int[] oh = SLConsoleTest.worldPos(al, eyeCtrl[0], eyeCtrl[1], eyeCtrl[2], o[1]);
        final int y = eyeCtrl[1], zT = eyeCtrl[2] - 3;
        final int west = Math.min(ob[0], oh[0]) - 6, east = Math.max(ob[0], oh[0]) + 1;
        final List<int[]> trunk = MeParts.line(new int[] { west, y, zT }, new int[] { east, y, zT });
        MeParts.run(world, trunk, MeParts.Kind.SMART, color, player);
        for (int[] h : new int[][] { ob, oh }) {
            final int[] fr = front(h, facing(world, h));
            final List<int[]> l = MeParts.line(new int[] { h[0], y, zT }, fr);
            if (l != null && MeParts.clear(world, l, null)) MeParts.run(world, l, MeParts.Kind.SMART, color, player);
            else {
                final List<int[]> path = MeParts.route(world, new int[] { h[0], y, zT }, fr, null, 14, AutoRig.GROUND);
                if (path == null) out.add("NO CABLE ROUTE to the ME hatch at " + RenderTest.pos(h));
                else MeParts.run(world, path, MeParts.Kind.SMART, color, player);
            }
        }
        final int[] core = { west - 1, y, zT }, drv = { west - 1, y + 1, zT };
        MeParts.block(world, core, MeParts.controllerBlock());
        MeParts.block(world, drv, MeParts.driveBlock());
        int slot = 0;
        for (ItemStack cell : new ItemStack[] { AEApi.instance()
            .definitions()
            .items()
            .cell64k()
            .maybeStack(1)
            .orNull(), cellOf("CELL16384K"), cellOf("CELL16384K") }) {
            if (cell != null && MeParts.intoDrive(world, drv, slot, cell)) slot++;
        }
        final int[] term = { west + 2, y, zT };
        MeParts.terminal(world, term, ForgeDirection.NORTH, MeParts.Kind.SMART, color, player, out, "");
        final String n = name.toLowerCase(Locale.ROOT);
        point(n + ".term", ctr(term));
        point(n + ".termcam", new double[] { term[0] + 0.5, y + 1.1, term[2] - 2.6 });
        point(n + ".mecore", ctr(core));
        out.add(
            "ME network on the Eye's products (set up by script): ME output bus " + RenderTest.pos(ob)
                + ", ME output hatch "
                + RenderTest.pos(oh)
                + ", controller "
                + RenderTest.pos(core)
                + ", drive with "
                + slot
                + " empty cells, terminal "
                + RenderTest.pos(term));
        return out;
    }

    // ------------------------------------------------------------------------------------- the cells

    /**
     * Set up by script, on cue: the two supply tanks get endless Helium and Hydrogen (the storage buses then show the
     * network 2.1 G L of each), and the drive gets an item cell with {@link #CHARGES} Stellar Ignition Charges plus an
     * empty fluid cell and item cell for the star's products.
     */
    static List<String> insertCells(World world) {
        final List<String> out = new ArrayList<>();
        if (drive == null) {
            out.add("no AE2 Eye rig (build eyeae first)");
            return out;
        }
        for (int i = 0; i < 4; i++) {
            final int[] t = i == 0 ? tankHe : i == 1 ? tankH : i == 2 ? trimTankHe : trimTankH;
            final Fluid fl = i % 2 == 0 ? AutoRig.helium : AutoRig.hydrogen;
            final gregtech.api.metatileentity.implementations.MTEBasicTank tank = AutoRig
                .mteAt(world, t, gregtech.api.metatileentity.implementations.MTEBasicTank.class);
            if (tank == null) {
                out.add("no supply tank at " + RenderTest.pos(t));
                continue;
            }
            tank.setFillableStack(new FluidStack(fl, 1000));
            // the read-only storage bus in front re-scans on a neighbour change: without one it sometimes kept the
            // first 1,000 L it saw and the Eye's stocking hatch never filled the buffers (showcase v2 draft 1)
            world.notifyBlocksOfNeighborChange(t[0], t[1], t[2], world.getBlock(t[0], t[1], t[2]));
            world.markBlockForUpdate(t[0], t[1], t[2]);
            out.add("supply tank " + RenderTest.pos(t) + ": endless " + fl.getName() + " (set up by script)");
        }
        final ItemStack items = MeParts.filledCell(
            AEApi.instance()
                .definitions()
                .items()
                .cell64k()
                .maybeStack(1)
                .orNull(),
            StorageChannel.ITEMS,
            out,
            "",
            MeParts.item(new ItemStack(SLItems.stellarIgnitionCharge, 1, 0), CHARGES));
        int slot = 0;
        for (ItemStack cell : new ItemStack[] { items, cellOf("CELL16384K"), AEApi.instance()
            .definitions()
            .items()
            .cell64k()
            .maybeStack(1)
            .orNull() }) {
            if (cell != null && MeParts.intoDrive(world, drive, slot, cell)) slot++;
        }
        cellsIn = true;
        cellsAt = AutoRig.serverTick;
        t0 = tick;
        out.add(slot + " cells in the ME drive at " + RenderTest.pos(drive) + " (filled by script)");
        return out;
    }

    private static ItemStack cellOf(String field) {
        final Item item = MeParts.ae2fc(field);
        return item == null ? null : new ItemStack(item);
    }

    // ------------------------------------------------------------------------------------- reading

    static MTEEyeOfHarmonyBTS eye() {
        return ctrl == null ? null : AutoRig.mteAt(RenderTest.world(), ctrl, MTEEyeOfHarmonyBTS.class);
    }

    static boolean lit() {
        final MTEEyeOfHarmonyBTS eye = eye();
        return eye != null && eye.starRuntime()
            .burning();
    }

    /** Seconds since the cells went in (0 before). */
    static long seconds() {
        return cellsIn ? (tick - t0) / 20 : 0;
    }

    private static void openLog() {
        final File dir = MinecraftServer.getServer()
            .getFile("screenshots/autorig");
        if (!dir.isDirectory() && !dir.mkdirs()) BeyondTheStars.LOG.error("[BTS autorig] cannot create " + dir);
        try {
            if (tsv != null) tsv.close();
            tsv = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(new File(dir, "eye-ae.tsv")), StandardCharsets.UTF_8),
                true);
            tsv.println(
                "t\tstate\tP\tzone\tintegrity\tY\ttrims_code\the_bit_dust\th_bit_dust\the_bus_powered\th_bus_powered"
                    + "\tfield_He\tfield_H\tbuf_He\tbuf_H\tme_He\tme_H\tme_charges\tme_products\temitter_lamp\tevent\tfailure");
        } catch (IOException bad) {
            BeyondTheStars.LOG.error("[BTS autorig] cannot open eye-ae.tsv", bad);
        }
        if (!hooked) {
            hooked = true;
            FMLCommonHandler.instance()
                .bus()
                .register(new Sampler());
        }
    }

    /** Once a second: one line. Reads only. */
    public static final class Sampler {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            tick++;
            if (ctrl == null || tick % 20 != 0) return;
            try {
                sample();
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS autorig] eye-ae sample failed: " + bad);
            }
        }
    }

    private static void sample() {
        final MTEEyeOfHarmonyBTS eye = eye();
        if (eye == null || tsv == null) return;
        final World world = RenderTest.world();
        final EyeStarRuntime rt = eye.starRuntime();
        final FieldReadout r = rt.lastReadout();
        final Map<String, Long> trims = rt.lastTrimsByName();
        if (rt.burning() && litAt < 0 && cellsIn) {
            litAt = seconds();
            BeyondTheStars.LOG
                .info("[BTS autorig] Rig C: the AE2 Eye lit itself " + litAt + " s after the cells went in");
        }
        final String line = String.join(
            "\t",
            String.valueOf(seconds()),
            String.valueOf(rt.state()),
            r.lit ? String.format(Locale.ROOT, "%.2f", r.x(StellarField.P)) : "",
            r.lit ? String.valueOf(r.zone) : "",
            r.lit ? String.format(Locale.ROOT, "%.1f", r.I) : "",
            r.lit ? String.format(Locale.ROOT, "%.3f", r.Y) : "",
            String.valueOf(AutoRig.code(world, fHe.at(0, 1, 0))),
            String.valueOf(world.getBlockMetadata(fHe.at(1, 1, 0)[0], fHe.at(1, 1, 0)[1], fHe.at(1, 1, 0)[2])),
            String.valueOf(world.getBlockMetadata(fH.at(1, 1, 0)[0], fH.at(1, 1, 0)[1], fH.at(1, 1, 0)[2])),
            MeParts.powered(world, trimBusHe) ? "1" : "0",
            MeParts.powered(world, trimBusH) ? "1" : "0",
            String.valueOf(trimLitres(trims, "Helium")),
            String.valueOf(trimLitres(trims, "Hydrogen")),
            String.valueOf(eye.buffer(AutoRig.helium)),
            String.valueOf(eye.buffer(AutoRig.hydrogen)),
            String.valueOf(MeParts.litres(world, emitterHost, AutoRig.helium)),
            String.valueOf(MeParts.litres(world, emitterHost, AutoRig.hydrogen)),
            String.valueOf(MeParts.count(world, emitterHost, new ItemStack(SLItems.stellarIgnitionCharge, 1, 0))),
            products(world),
            AutoRig.lit(world, emitterLamp) ? "1" : "0",
            rt.lastEvent()
                .replace('\t', ' '),
            rt.lastFailure()
                .replace('\t', ' '));
        tsv.println(line);
        last = line;
        if (tick % 1200 == 0) {
            BeyondTheStars.LOG.info("[BTS autorig] Rig C bus He: " + busState(world, trimBusHe, fHe));
            BeyondTheStars.LOG.info("[BTS autorig] Rig C bus H: " + busState(world, trimBusH, fH));
            BeyondTheStars.LOG.info("[BTS autorig] Rig C trims: " + trims);
        }
    }

    /** Diagnostics: what the trim bus itself sees (config, cards, redstone, target, the Field hatch's contents). */
    static String busState(World world, int[] p, AutoRig.Frame F) {
        final StringBuilder b = new StringBuilder();
        try {
            final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
            if (!(te instanceof appeng.api.parts.IPartHost)) return "no part host";
            final appeng.api.parts.IPart part = ((appeng.api.parts.IPartHost) te).getPart(F.o.getOpposite());
            b.append(
                part == null ? "no part on " + F.o.getOpposite()
                    : part.getClass()
                        .getSimpleName());
            if (part instanceof appeng.parts.automation.PartSharedItemBus) {
                final appeng.parts.automation.PartSharedItemBus<?> bus = (appeng.parts.automation.PartSharedItemBus<?>) part;
                b.append(", config ")
                    .append(
                        bus.getAEInventoryByName(appeng.api.storage.StorageName.CONFIG)
                            .getAEStackInSlot(0));
                b.append(", speed cards ")
                    .append(bus.getInstalledUpgrades(appeng.api.config.Upgrades.SPEED));
                b.append(", redstone cards ")
                    .append(bus.getInstalledUpgrades(appeng.api.config.Upgrades.REDSTONE));
                b.append(", mode ")
                    .append(
                        bus.getConfigManager()
                            .getSetting(appeng.api.config.Settings.REDSTONE_CONTROLLED));
                b.append(", host redstone ")
                    .append(((appeng.api.parts.IPartHost) te).hasRedstone(F.o.getOpposite()));
                b.append(", active ")
                    .append(
                        bus.getProxy()
                            .isActive());
                for (String m : new String[] { "canDoBusWork", "getTarget", "isSleeping" }) {
                    try {
                        java.lang.reflect.Method mm = null;
                        for (Class<?> c = bus.getClass(); c != null && mm == null; c = c.getSuperclass()) {
                            try {
                                mm = c.getDeclaredMethod(m);
                            } catch (NoSuchMethodException next) {
                                // up
                            }
                        }
                        if (mm != null) {
                            mm.setAccessible(true);
                            b.append(", ")
                                .append(m)
                                .append(" ")
                                .append(mm.invoke(bus));
                        }
                    } catch (ReflectiveOperationException | RuntimeException bad) {
                        b.append(", ")
                            .append(m)
                            .append(" ! ")
                            .append(bad);
                    }
                }
            }
            final gregtech.api.metatileentity.implementations.MTEHatchInput h = AutoRig
                .mteAt(world, F.p, gregtech.api.metatileentity.implementations.MTEHatchInput.class);
            b.append(", field hatch ")
                .append(
                    h == null ? "none"
                        : h.getFluid() == null ? "empty"
                            : h.getFluid()
                                .getFluid()
                                .getName() + " "
                                + h.getFluid().amount);
            if (h != null) b.append(", hatch recipe map ")
                .append(h.mRecipeMap);
            final TileEntity ht = world.getTileEntity(F.p[0], F.p[1], F.p[2]);
            final Fluid fl = F == fHe ? AutoRig.helium : AutoRig.hydrogen;
            if (ht instanceof net.minecraftforge.fluids.IFluidHandler) {
                b.append(", fill(")
                    .append(F.o)
                    .append(") sim ")
                    .append(((net.minecraftforge.fluids.IFluidHandler) ht).fill(F.o, new FluidStack(fl, 1000), false));
                b.append(", canFill ")
                    .append(((net.minecraftforge.fluids.IFluidHandler) ht).canFill(F.o, fl));
            }
            b.append(", ME at the bus ")
                .append(MeParts.litres(world, p, fl));
        } catch (RuntimeException bad) {
            b.append(" ! ")
                .append(bad);
        }
        return b.toString();
    }

    private static String products(World world) {
        final appeng.api.networking.storage.IStorageGrid s = MeParts.storage(world, emitterHost);
        if (s == null || s.getFluidInventory() == null) return "-";
        final StringBuilder b = new StringBuilder();
        for (appeng.api.storage.data.IAEFluidStack st : s.getFluidInventory()
            .getStorageList()) {
            final Fluid fl = st.getFluid();
            if (fl == AutoRig.helium || fl == AutoRig.hydrogen) continue;
            if (b.length() > 0) b.append(',');
            b.append(fl.getName())
                .append('=')
                .append(st.getStackSize());
        }
        return b.length() == 0 ? "none" : b.toString();
    }

    private static long trimLitres(Map<String, Long> trims, String name) {
        if (trims == null) return 0;
        for (Map.Entry<String, Long> e : trims.entrySet()) {
            if (e.getKey() != null && e.getKey()
                .toLowerCase(Locale.ROOT)
                .contains(name.toLowerCase(Locale.ROOT))) return e.getValue();
        }
        return 0;
    }

    static String status() {
        return "aeeye t=" + seconds() + " s, lit at " + litAt + " | " + last;
    }
}
