package com.nateyburr.beyondthestars.rendertest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * DEV-ONLY automation photo rig B's ME network ({@link AutoRig}): an export bus with a redstone card on each Kerr
 * station's cable-bus block (bolts with acceleration cards; a Black Hole Seed), orange dense cable straight out from
 * both stations and across in front of the KEE, the core on that trunk (a creative-energy ME controller, a drive, an
 * interface) and a level emitter with a lamp. The parts go in through {@link MeParts}. AE2 is GT's own dependency, so
 * it is always on the classpath where this runs.
 */
final class AutoRigAE2 {

    private AutoRigAE2() {}

    static int[] boltBus, seedBus, controller, drive, iface, emitterHost, emitterLamp;
    private static boolean cellIn;
    static final long BOLTS = 4096, SEEDS = 4;
    /** Acceleration cards on the bolt export bus (the redstone card takes the fourth slot): 64 bolts an operation. */
    static final int BOLT_SPEED_CARDS = 3;

    /** The network's colour: the same orange as the AE2 Eye rig (docs/recording.md). */
    static final AEColor COLOR = AEColor.Orange;
    /** How far out from the bolt buses the cross trunk runs (clear of the cage at the KEE's height). */
    static final int TRUNK_OUT = 16;

    /**
     * The Kerr's ME network, laid out for the camera: an export bus on a smart-cable block in front of each station's
     * bolt bus; from each, a straight dense run out from the cage; one cross trunk in front of the KEE joining them;
     * the core (creative-energy controller, drive above, interface below) just beyond the middle of the trunk; a level
     * emitter with a lamp beside it. Runs that are blocked fall back to the router.
     */
    static void build(World world, AutoRig.Frame bolt, AutoRig.Frame seed, List<int[]> route, List<String> log) {
        final EntityPlayer player = RenderTest.player();
        final ItemStack boltStack = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 1L);
        final ItemStack seedStack = ItemList.Black_Hole_Opener.get(1L);
        cellIn = false;
        boltBus = bolt.at(1, 0, 0);
        seedBus = seed.at(1, 0, 0);
        MeParts.exportBus(
            world,
            boltBus,
            bolt.o.getOpposite(),
            AEItemStack.create(boltStack),
            BOLT_SPEED_CARDS,
            false,
            true,
            MeParts.Kind.SMART,
            COLOR,
            player,
            log,
            "B: ");
        MeParts.exportBus(
            world,
            seedBus,
            seed.o.getOpposite(),
            AEItemStack.create(seedStack),
            0,
            false,
            true,
            MeParts.Kind.SMART,
            COLOR,
            player,
            log,
            "B: ");
        route.clear();
        final Set<Long> avoid = new HashSet<>();
        for (AutoRig.Frame f : new AutoRig.Frame[] { bolt, seed }) {
            for (int dO = 1; dO <= 3; dO++) for (int dY = -1; dY <= 2; dY++) for (int dS = 1; dS <= 3; dS++) {
                avoid.add(MeParts.key(f.at(dO, dY, dS)));
            }
            avoid.add(MeParts.key(f.at(1, 1, 0)));
            avoid.add(MeParts.key(f.at(1, 2, 0)));
        }
        // straight out from each station, then one cross trunk (both stations face the same way on the T1 cage)
        final int[] boltOut = bolt.at(TRUNK_OUT, 0, 0), seedOut = seed.at(TRUNK_OUT, 0, 0);
        run(world, MeParts.line(bolt.at(2, 0, 0), boltOut), avoid, player, route, log, "bolt-station run");
        run(world, MeParts.line(seed.at(2, 0, 0), seedOut), avoid, player, route, log, "seed-station run");
        run(world, MeParts.line(seedOut, boltOut), avoid, player, route, log, "cross trunk");
        // the core: just beyond the middle of the trunk
        final int[] mid = { (boltOut[0] + seedOut[0]) / 2, boltOut[1], (boltOut[2] + seedOut[2]) / 2 };
        final ForgeDirection out = bolt.o;
        controller = new int[] { mid[0] + out.offsetX, mid[1], mid[2] + out.offsetZ };
        drive = new int[] { controller[0], controller[1] + 1, controller[2] };
        iface = new int[] { controller[0], controller[1] - 1, controller[2] };
        MeParts.block(world, controller, MeParts.controllerBlock());
        MeParts.block(world, drive, MeParts.driveBlock());
        MeParts.block(world, iface, MeParts.interfaceBlock());
        // the level emitter on a smart-cable block beside the core (fed by the trunk behind it), its lamp in front
        final ForgeDirection side = bolt.s;
        emitterHost = new int[] { controller[0] + side.offsetX * 3, controller[1], controller[2] + side.offsetZ * 3 };
        MeParts.levelEmitter(world, emitterHost, out, boltStack, 512, MeParts.Kind.SMART, COLOR, player, log, "B: ");
        route.add(emitterHost);
        emitterLamp = new int[] { emitterHost[0] + out.offsetX, emitterHost[1], emitterHost[2] + out.offsetZ };
        world.setBlock(emitterLamp[0], emitterLamp[1], emitterLamp[2], Blocks.redstone_lamp, 0, 3);
        sign(
            world,
            new int[] { iface[0] + out.offsetX, iface[1], iface[2] + out.offsetZ },
            out,
            log,
            "ME interface:",
            "the factory",
            "delivers bolts",
            "and Seeds here");
        log.add(
            "B: ME network: controller " + RenderTest.pos(controller)
                + ", drive (no cell yet) "
                + RenderTest.pos(drive)
                + ", interface "
                + RenderTest.pos(iface)
                + ", level emitter on "
                + RenderTest.pos(emitterHost)
                + " (SpaceTime bolts >= 512 lights its lamp); "
                + route.size()
                + " cable cells");
        AutoRig.POINTS.put("k.core", AutoRigEyeAE.ctr(controller));
        AutoRig.POINTS.put("k.drive", AutoRigEyeAE.ctr(drive));
        AutoRig.POINTS.put("k.iface", AutoRigEyeAE.ctr(iface));
        AutoRig.POINTS.put("k.emitter", AutoRigEyeAE.ctr(emitterLamp));
        AutoRig.POINTS.put("k.boltbus", AutoRigEyeAE.ctr(boltBus));
        AutoRig.POINTS.put("k.seedbus", AutoRigEyeAE.ctr(seedBus));
        AutoRig.POINTS.put("k.boltdir", AutoRigEyeAE.ctr(bolt.at(0, 1, 0)));
        AutoRig.POINTS.put("k.seeddir", AutoRigEyeAE.ctr(seed.at(0, 1, 0)));
        AutoRig.POINTS.put("k.boltlamp", AutoRigEyeAE.ctr(bolt.at(1, 1, 2)));
        AutoRig.POINTS.put("k.seedlamp", AutoRigEyeAE.ctr(seed.at(1, 1, 2)));
        AutoRig.POINTS.put("k.trunk", AutoRigEyeAE.ctr(mid));
    }

    /** Lays dense cable along a straight run, or routes round whatever is in the way. */
    private static void run(World world, List<int[]> line, Set<Long> avoid, EntityPlayer player, List<int[]> route,
        List<String> log, String what) {
        if (line == null) return;
        List<int[]> path = line;
        if (!MeParts.clear(world, line, avoid)) {
            path = MeParts.route(world, line.get(0), line.get(line.size() - 1), avoid, 14, AutoRig.GROUND);
            if (path == null) {
                log.add("B: NO CABLE ROUTE for the " + what);
                return;
            }
            what += " (routed)";
        }
        MeParts.run(world, path, MeParts.Kind.DENSE_COVERED, COLOR, player);
        route.addAll(path);
        log.add("B: " + what + ": " + path.size() + " cells");
    }

    // ----------------------------------------------------------------------------------- parts

    private static void sign(World world, int[] p, ForgeDirection out, List<String> log, String... lines) {
        if (!world.isAirBlock(p[0], p[1], p[2])) return;
        final int meta = out == ForgeDirection.NORTH ? 2
            : out == ForgeDirection.SOUTH ? 3 : out == ForgeDirection.WEST ? 4 : 5;
        world.setBlock(p[0], p[1], p[2], Blocks.wall_sign, meta, 3);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof net.minecraft.tileentity.TileEntitySign) {
            final net.minecraft.tileentity.TileEntitySign s = (net.minecraft.tileentity.TileEntitySign) te;
            for (int i = 0; i < 4; i++) s.signText[i] = i < lines.length ? lines[i] : "";
            world.markBlockForUpdate(p[0], p[1], p[2]);
        }
    }

    // ------------------------------------------------------------------------------------ cell

    /**
     * The ME cell goes into the drive: a 64k cell holding 4,096 SpaceTime bolts and 4 Black Hole Seeds. Until then
     * the network is empty, so the start-up (the Seed, the first bolts) happens in front of the camera.
     */
    static String insertCell(World world) {
        final TileEntity te = world.getTileEntity(drive[0], drive[1], drive[2]);
        if (!(te instanceof TileDrive)) return "no ME drive at " + RenderTest.pos(drive);
        final ItemStack cell = AEApi.instance()
            .definitions()
            .items()
            .cell64k()
            .maybeStack(1)
            .orNull();
        if (cell == null) return "no 64k cell";
        @SuppressWarnings("unchecked")
        final IMEInventoryHandler<IAEItemStack> inv = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, null, StorageChannel.ITEMS);
        if (inv == null) return "the 64k cell has no inventory";
        final BaseActionSource src = new BaseActionSource();
        // the AE stack carries the count (an ItemStack from GT's helper is capped at 64)
        final IAEItemStack bolts = AEItemStack
            .create(GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 1L));
        bolts.setStackSize(BOLTS);
        final IAEItemStack seeds = AEItemStack.create(ItemList.Black_Hole_Opener.get(1L));
        seeds.setStackSize(SEEDS);
        final IAEItemStack leftB = inv.injectItems(bolts, Actionable.MODULATE, src);
        final IAEItemStack leftS = inv.injectItems(seeds, Actionable.MODULATE, src);
        ((TileDrive) te).getInternalInventory()
            .setInventorySlotContents(0, cell);
        cellIn = true;
        return "64k cell in the drive: " + BOLTS
            + " SpaceTime bolts ("
            + (leftB == null ? 0 : leftB.getStackSize())
            + " left out), "
            + SEEDS
            + " Black Hole Seeds ("
            + (leftS == null ? 0 : leftS.getStackSize())
            + " left out)";
    }

    static boolean cellIn() {
        return cellIn;
    }

    /** How many of {@code what} the network holds (-1 if no grid yet). */
    static long meCount(World world, ItemStack what) {
        if (boltBus == null || what == null) return -1;
        final TileEntity te = world.getTileEntity(boltBus[0], boltBus[1], boltBus[2]);
        if (!(te instanceof IPartHost)) return -1;
        final IPart cablePart = ((IPartHost) te).getPart(ForgeDirection.UNKNOWN);
        if (cablePart == null) return -1;
        final IGridNode node = cablePart.getGridNode();
        if (node == null) return -1;
        final IGrid grid = node.getGrid();
        if (grid == null) return -1;
        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        if (storage == null || storage.getItemInventory() == null) return -1;
        final IAEItemStack found = storage.getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(what));
        return found == null ? 0 : found.getStackSize();
    }

    static boolean emitterLampLit(World world) {
        return emitterLamp != null && AutoRig.lit(world, emitterLamp);
    }

}
