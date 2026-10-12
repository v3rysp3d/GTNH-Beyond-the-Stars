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
 * <li><b>Trims:</b> two Directive Hatches (Trims, Bit 1 and Bit 2) on the band, each over a plain Field input hatch;
 * in front of each Field hatch an AE2FC fluid export bus (Helium / Hydrogen) with a redstone card ("active with
 * signal"). One redstone dust on the bus block's facade carries the hatch's bit onto the bus.</li>
 * <li><b>Products:</b> the kit's ME output bus and ME output hatch beside the controller, cabled in, so the star's
 * output lands in the same network.</li>
 * <li><b>The core:</b> a creative-energy ME controller, an ME drive, and a level emitter with a lamp (lit while the
 * network holds an ignition charge). Orange dense cable: a ground trunk in front of the Eye, two risers to the
 * band.</li>
 * </ul>
 * The drive is empty until {@link #insertCells} (the recorder's {@code aecells}): the cells are filled by script
 * (Helium, Hydrogen, charges), standing in for the player's factory. The Eye's power is the owner's wireless EU,
 * also set by script. From then on nothing touches the machine: a sampler only reads ({@code eye-ae.tsv}).
 */
final class AutoRigEyeAE {

    private AutoRigEyeAE() {}

    static final int X = 2400, Z = 4000;
    static final AEColor COLOR = AEColor.Orange;
    /** Fluid in each gas cell (filled by script). Sol's ignition needs 600 M L of each. */
    static final long GAS_LITRES = 40_000_000_000L;
    static final int CHARGES = 8;
    /** Acceleration cards on each trim bus: 2 = 2,000 L an operation. */
    static final int TRIM_CARDS = 2;

    static int[] ctrl, box;
    static double[] centre;
    static AutoRig.Frame fHe, fH;
    static int[] stockHatch, stockBus, outBus, outHatch;
    static int[] meCtrl, drive, emitterHost, emitterLamp, trunkWest, trunkEast;
    static int[] trimBusHe, trimBusH;
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
        final int[][] h = EyeKit.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h');
        final int[][] f = EyeKit.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'f');
        final int[][] o = EyeKit.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'o');
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

        // 2. the trim stations on the band: Directive Hatch (Trims, bit) over the Field hatch, fluid export bus in front
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
        final int west = Math.min(Math.min(stockHatch[0], stockBus[0]), Math.min(trimBusHe[0], trimBusH[0]));
        final int east = Math.max(Math.max(stockHatch[0], stockBus[0]), Math.max(trimBusHe[0], trimBusH[0]));
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
        world.setBlock(
            emitterLamp[0],
            emitterLamp[1],
            emitterLamp[2],
            net.minecraft.init.Blocks.redstone_lamp,
            0,
            3);
        // 4. a riser from the trunk up to each trim station
        for (int[] bus : new int[][] { trimBusHe, trimBusH }) riser(world, bus, zT, player);

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
        point("ae.drive", ctr(drive));
        point("ae.emitter", ctr(emitterLamp));
        point("ae.stockhatch", ctr(stockHatch));
        point("ae.stockbus", ctr(stockBus));
        point("ae.outbus", ctr(outBus));
        point("ae.outhatch", ctr(outHatch));
        point("ae.trimhe", ctr(trimBusHe));
        point("ae.trimh", ctr(trimBusH));
        point("ae.dirhe", ctr(fHe.at(0, 1, 0)));
        point("ae.dirh", ctr(fH.at(0, 1, 0)));
        point("ae.fieldhe", ctr(fHe.p));
        point("ae.lamphe", ctr(fHe.at(1, 1, 2)));
        point("ae.lamph", ctr(fH.at(1, 1, 2)));
        point("ae.trunk", ctr(new int[] { X, y, zT }));
        point("ae.riserhe", new double[] { trimBusHe[0] + 0.5, (y + trimBusHe[1]) / 2.0, zT + 0.5 });
        point("ae.riserh", new double[] { trimBusH[0] + 0.5, (y + trimBusH[1]) / 2.0, zT + 0.5 });
    }

    /**
     * A trim station: the kit's Directive Hatch above the Field hatch set to Trims in Bit mode; the fluid export bus
     * on the cell in front of the Field hatch; dust on its facade (from the hatch's face down onto the bus block) and
     * on along s over a quartz block to a lamp (lit = the trim valve is open). Returns the bus cell.
     */
    private static int[] trimStation(World world, AutoRig.Frame F, Fluid fluid, DirectiveSet.Output bit,
        EntityPlayer player) {
        AutoRig.directive(world, F.at(0, 1, 0), F.o, DirectiveSet.EYE_TRIMS, bit, 10);
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
            COLOR,
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
                + " over the Field hatch "
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
            for (int dO = 0; dO <= 2; dO++) for (int dY = 1; dY <= 2; dY++) for (int dS = -1; dS <= 3; dS++)
                avoid.add(MeParts.key(F.at(dO, dY, dS)));
            for (int dS = 1; dS <= 3; dS++) avoid.add(MeParts.key(F.at(1, 0, dS)));
        }
        final List<int[]> up = MeParts.line(foot, top);
        final List<int[]> along = MeParts.line(top, bus);
        if (MeParts.clear(world, up, avoid) && along != null && MeParts.clear(world, along.subList(0, along.size() - 1), avoid)) {
            lay(world, up, MeParts.Kind.DENSE_COVERED, player, "riser");
            lay(world, along.subList(0, along.size() - 1), MeParts.Kind.DENSE_COVERED, player, "riser top");
            return;
        }
        final List<int[]> path = MeParts.route(world, foot, bus, avoid, 14, AutoRig.GROUND);
        if (path == null) {
            log.add("C: NO RISER ROUTE to " + RenderTest.pos(bus));
            return;
        }
        lay(world, path.subList(0, path.size() - 1), MeParts.Kind.DENSE_COVERED, player, "riser (routed)");
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
        if (path == null) return;
        MeParts.run(world, path, kind, COLOR, player);
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

    // ------------------------------------------------------------------------------------- the cells

    /**
     * The drive's cells go in (set up by script): one fluid cell of Helium and one of Hydrogen ({@link #GAS_LITRES}
     * each), an item cell with {@link #CHARGES} Stellar Ignition Charges, and an empty fluid cell and item cell for the
     * star's products.
     */
    static List<String> insertCells(World world) {
        final List<String> out = new ArrayList<>();
        if (drive == null) {
            out.add("no AE2 Eye rig (build eyeae first)");
            return out;
        }
        final ItemStack heCell = MeParts
            .filledCell(bigFluidCell(), StorageChannel.FLUIDS, out, "", MeParts.fluid(AutoRig.helium, GAS_LITRES));
        final ItemStack hCell = MeParts
            .filledCell(bigFluidCell(), StorageChannel.FLUIDS, out, "", MeParts.fluid(AutoRig.hydrogen, GAS_LITRES));
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
        for (ItemStack cell : new ItemStack[] { heCell, hCell, items, cellOf("CELL16384K"), AEApi.instance()
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

    /** The largest AE2FC fluid cell there is (Singularity, else Quantum, else 16384k). */
    private static ItemStack bigFluidCell() {
        for (String f : new String[] { "SINGULARITY_CELL", "QUANTUM_CELL", "CELL16384K" }) {
            final ItemStack c = cellOf(f);
            if (c != null) return c;
        }
        return null;
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
            BeyondTheStars.LOG.info("[BTS autorig] Rig C: the AE2 Eye lit itself " + litAt + " s after the cells went in");
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
                .contains(name)) return e.getValue();
        }
        return 0;
    }

    static String status() {
        return "aeeye t=" + seconds() + " s, lit at " + litAt + " | " + last;
    }
}
