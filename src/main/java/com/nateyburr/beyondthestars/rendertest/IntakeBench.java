package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;

import appeng.api.storage.StorageChannel;
import appeng.api.util.AEColor;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * DEV-ONLY test bench for the Eye's buffer intake cap (B-108): {@code /btseye star x y z intake setup [G L]} and
 * {@code intake status}, typed on the dev-server console. Loaded only by those commands.
 * <p>
 * {@code setup} turns the Eye's first gas socket into an ME stocking input hatch (Helium, Hydrogen) on a small AE2
 * network built straight out from the hatch: a creative-energy ME controller, an ME drive with one fluid cell holding
 * {@code G} gigalitres (default 10) of each gas, and a SECOND CONSUMER: a fluid export bus (Hydrogen, three
 * acceleration
 * cards, no redstone condition) into a Super Tank. Before the cap one stocking hatch emptied such a network into the
 * buffers within a second and the export bus found nothing (the AE2 video rig, B-107). {@code status} prints the
 * buffers against their caps, what the network still holds, and what the second consumer got.
 */
public final class IntakeBench {

    private IntakeBench() {}

    /** The rig of the last {@code setup}: the stocking hatch, the export-bus cable and the consumer tank. */
    private static int[] hatch, busCable, tank;
    private static long seeded;

    public static List<String> run(MTEEyeOfHarmonyBTS eye, String[] args) {
        final List<String> out = new ArrayList<>();
        final String sub = args.length > 5 ? args[5].toLowerCase(Locale.ROOT) : "status";
        if ("setup".equals(sub)) {
            setup(eye, args.length > 6 ? Double.parseDouble(args[6]) : 10, out);
        }
        status(eye, out);
        return out;
    }

    private static void setup(MTEEyeOfHarmonyBTS eye, double gigalitres, List<String> out) {
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        final World world = base.getWorld();
        AutoRig.initFluids();
        final Fluid he = AutoRig.helium, hy = AutoRig.hydrogen;
        final ExtendedFacing al = eye.getExtendedFacing();
        final int[][] h = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'h');
        hatch = SLConsoleTest.worldPos(al, base.getXCoord(), base.getYCoord(), base.getZCoord(), h[0]);
        final net.minecraft.tileentity.TileEntity te = world.getTileEntity(hatch[0], hatch[1], hatch[2]);
        if (!(te instanceof IGregTechTileEntity)) {
            out.add("no hatch in the first gas socket at " + RenderTest.pos(hatch));
            return;
        }
        final ForgeDirection face = ((IGregTechTileEntity) te).getFrontFacing();
        MTEReactorCoreMk1.placeGtMachine(
            world,
            hatch[0],
            hatch[1],
            hatch[2],
            (short) ItemList.Hatch_Input_ME.get(1L)
                .getItemDamage(),
            face);
        final MTEHatchInputME sh = AutoRig.mteAt(world, hatch, MTEHatchInputME.class);
        if (sh == null) {
            out.add("could not place the ME stocking input hatch at " + RenderTest.pos(hatch));
            return;
        }
        sh.setSlotConfig(0, new FluidStack(he, 1));
        sh.setSlotConfig(1, new FluidStack(hy, 1));
        // straight out from the hatch's face: controller, drive, export-bus cable, consumer tank
        final int[] ctrl = step(hatch, face, 1), drive = step(hatch, face, 2);
        busCable = step(hatch, face, 3);
        tank = step(hatch, face, 4);
        KerrTestBench.keep(
            world,
            new int[] { Math.min(hatch[0], tank[0]) - 1, 0, Math.min(hatch[2], tank[2]) - 1,
                Math.max(hatch[0], tank[0]) + 1, 255, Math.max(hatch[2], tank[2]) + 1 });
        final EntityPlayer player = FakePlayerFactory.getMinecraft((WorldServer) world);
        MeParts.block(world, ctrl, MeParts.controllerBlock());
        MeParts.block(world, drive, MeParts.driveBlock());
        MTEReactorCoreMk1.placeGtMachine(
            world,
            tank[0],
            tank[1],
            tank[2],
            (short) ItemList.Super_Tank_LV.get(1L)
                .getItemDamage(),
            face); // its front (GT's output side, which refuses input) faces away from the bus
        final List<String> log = new ArrayList<>();
        final appeng.parts.automation.PartSharedItemBus<?> bus = MeParts.exportBus(
            world,
            busCable,
            face,
            MeParts.fluid(hy, 1),
            3,
            true,
            false,
            MeParts.Kind.SMART,
            AEColor.Transparent,
            player,
            log,
            "");
        if (bus != null) {
            // the second consumer runs all the time (no redstone condition)
            bus.getConfigManager()
                .putSetting(appeng.api.config.Settings.REDSTONE_CONTROLLED, appeng.api.config.RedstoneMode.IGNORE);
        }
        seeded = (long) (gigalitres * 1e9);
        // one cell per gas (a fluid quantum cell takes one type)
        Item cellItem = MeParts.ae2fc("QUANTUM_CELL");
        if (cellItem == null) cellItem = MeParts.ae2fc("CELL16384K");
        int cells = 0;
        for (Fluid f : new Fluid[] { he, hy }) {
            final ItemStack cell = MeParts.filledCell(
                cellItem == null ? null : new ItemStack(cellItem),
                StorageChannel.FLUIDS,
                log,
                "cell: ",
                MeParts.fluid(f, seeded));
            if (MeParts.intoDrive(world, drive, cells, cell)) cells++;
        }
        final boolean inDrive = cells == 2;
        out.addAll(log);
        out.add(
            "intake rig: ME stocking hatch (He, H) " + RenderTest.pos(hatch)
                + " facing "
                + face
                + ", creative ME controller "
                + RenderTest.pos(ctrl)
                + ", drive "
                + RenderTest.pos(drive)
                + (inDrive ? " (2 cells in)" : " (" + cells + " cells in)")
                + ", fluid export bus (H) "
                + RenderTest.pos(busCable)
                + " -> Super Tank "
                + RenderTest.pos(tank));
        eye.checkStructure(true, base);
        out.add("Eye re-walked: formed " + eye.mMachine + ", gas sockets " + eye.gasHatchCount());
    }

    private static int[] step(int[] p, ForgeDirection d, int n) {
        return new int[] { p[0] + d.offsetX * n, p[1] + d.offsetY * n, p[2] + d.offsetZ * n };
    }

    private static void status(MTEEyeOfHarmonyBTS eye, List<String> out) {
        final World world = eye.getBaseMetaTileEntity()
            .getWorld();
        AutoRig.initFluids();
        final Map<String, Long> buf = eye.bufferSnapshot();
        final StringBuilder sb = new StringBuilder("intake:");
        for (Fluid f : new Fluid[] { AutoRig.hydrogen, AutoRig.helium }) {
            final Long held = buf.get(f.getName());
            sb.append(' ')
                .append(f.getName())
                .append(" buffer ")
                .append(String.format(Locale.ROOT, "%,d", held == null ? 0 : held))
                .append(" / cap ")
                .append(String.format(Locale.ROOT, "%,d", eye.intakeCap(f)))
                .append(", network ")
                .append(String.format(Locale.ROOT, "%,d", busCable == null ? -1 : MeParts.litres(world, busCable, f)))
                .append(';');
        }
        if (tank != null) {
            final MTEBasicTank t = AutoRig.mteAt(world, tank, MTEBasicTank.class);
            final FluidStack in = t == null ? null : t.getDrainableStack();
            sb.append(" second consumer (Super Tank) ")
                .append(t == null ? "MISSING" : String.format(Locale.ROOT, "%,d L", in == null ? 0 : in.amount));
        }
        if (seeded > 0) sb.append("; seeded ")
            .append(String.format(Locale.ROOT, "%,d", seeded))
            .append(" L each");
        out.add(sb.toString());
        out.add(
            "star: " + eye.starRuntime()
                .state()
                + (eye.starRuntime()
                    .waitReason()
                    .isEmpty() ? ""
                        : " (waiting: " + eye.starRuntime()
                            .waitReason() + ")"));
    }
}
