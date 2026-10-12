package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEColor;
import appeng.items.parts.ItemFacade;
import appeng.parts.automation.PartLevelEmitter;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.automation.PartUpgradeable;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * DEV-ONLY automation photo rig B's ME network ({@link AutoRig}): an export bus with a redstone card on each Kerr
 * station's cable-bus block (bolts with an acceleration card; a Black Hole Seed), glass cable to the core beside the
 * bolt station (a creative-energy ME controller, a drive, an interface) and a level emitter with a lamp. AE2 is GT's
 * own dependency, so it is always on the classpath where this runs.
 */
final class AutoRigAE2 {

    private AutoRigAE2() {}

    static int[] boltBus, seedBus, controller, drive, iface, emitterHost, emitterLamp;
    private static boolean cellIn;
    static final long BOLTS = 4096, SEEDS = 4;
    /** Acceleration cards on the bolt export bus (the redstone card takes the fourth slot): 64 bolts an operation. */
    static final int BOLT_SPEED_CARDS = 3;

    static void build(World world, AutoRig.Frame bolt, AutoRig.Frame seed, List<int[]> route, List<String> log) {
        final EntityPlayer player = RenderTest.player();
        final ItemStack boltStack = GTOreDictUnificator.get(OrePrefixes.bolt, Materials.SpaceTime, 1L);
        final ItemStack seedStack = ItemList.Black_Hole_Opener.get(1L);
        boltBus = bolt.at(1, 0, 0);
        seedBus = seed.at(1, 0, 0);
        exportBus(world, boltBus, bolt.o.getOpposite(), boltStack, BOLT_SPEED_CARDS, player, log);
        exportBus(world, seedBus, seed.o.getOpposite(), seedStack, 0, player, log);
        // the core four blocks out from the bolt station: cable, cable + level emitter (lamp beside it), controller
        // (drive above, interface below), so the station itself stays in clear view
        for (int dO = 2; dO <= 4; dO++) cable(world, bolt.at(dO, 0, 0), player);
        emitterHost = bolt.at(5, 0, 0);
        cable(world, emitterHost, player);
        emitterLamp = bolt.at(5, 0, 1);
        levelEmitter(world, emitterHost, bolt.s, boltStack, 512, player, log);
        world.setBlock(emitterLamp[0], emitterLamp[1], emitterLamp[2], Blocks.redstone_lamp, 0, 3);
        controller = bolt.at(6, 0, 0);
        drive = bolt.at(6, 1, 0);
        iface = bolt.at(6, -1, 0);
        place(
            world,
            controller,
            AEApi.instance()
                .definitions()
                .blocks()
                .creativeEnergyController()
                .maybeBlock()
                .orNull(),
            log,
            "creative-energy ME controller");
        place(
            world,
            drive,
            AEApi.instance()
                .definitions()
                .blocks()
                .drive()
                .maybeBlock()
                .orNull(),
            log,
            "ME drive");
        place(
            world,
            iface,
            AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeBlock()
                .orNull(),
            log,
            "ME interface");
        // glass cable from the seed station to the far side of the controller
        final int[] from = seed.at(2, 0, 0);
        final int[] to = bolt.at(7, 0, 0);
        final Set<Long> avoid = new HashSet<>();
        // keep the cable off the redstone side of each station (dust, lamp, sign cells) and the core's sign cell
        for (AutoRig.Frame f : new AutoRig.Frame[] { bolt, seed }) {
            for (int dO = 1; dO <= 3; dO++) for (int dY = -1; dY <= 2; dY++) for (int dS = 1; dS <= 3; dS++) {
                avoid.add(key(f.at(dO, dY, dS)));
            }
            avoid.add(key(f.at(1, 1, 0)));
            avoid.add(key(f.at(1, 2, 0)));
        }
        avoid.add(key(bolt.at(7, -1, 0)));
        avoid.add(key(emitterLamp));
        avoid.remove(key(from));
        avoid.remove(key(to));
        final List<int[]> path = route3d(world, from, to, avoid);
        route.clear();
        if (path == null) {
            log.add("B: NO CABLE ROUTE from the seed station to the core");
        } else {
            for (int[] q : path) cable(world, q, player);
            route.addAll(path);
            log.add("B: glass cable, " + path.size() + " cells, from the seed station to the core");
        }
        sign(world, bolt.at(7, -1, 0), bolt.o, log, "ME interface:", "the factory", "delivers bolts", "and Seeds here");
        log.add(
            "B: ME network: controller " + RenderTest.pos(controller)
                + ", drive (no cell yet) "
                + RenderTest.pos(drive)
                + ", interface "
                + RenderTest.pos(iface)
                + ", level emitter on "
                + RenderTest.pos(emitterHost)
                + " (SpaceTime bolts >= 512 lights its lamp)");
    }

    // ----------------------------------------------------------------------------------- parts

    private static IPartHost host(World world, int[] p) {
        final Block block = AEApi.instance()
            .definitions()
            .blocks()
            .multiPart()
            .maybeBlock()
            .orNull();
        if (block == null) return null;
        TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IPartHost)) {
            world.setBlock(p[0], p[1], p[2], block, 0, 3);
            te = world.getTileEntity(p[0], p[1], p[2]);
        }
        return te instanceof IPartHost ? (IPartHost) te : null;
    }

    private static void cable(World world, int[] p, EntityPlayer player) {
        final IPartHost h = host(world, p);
        if (h == null || h.getPart(ForgeDirection.UNKNOWN) != null) return;
        h.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
    }

    private static void exportBus(World world, int[] p, ForgeDirection side, ItemStack filter, int speedCards,
        EntityPlayer player, List<String> log) {
        cable(world, p, player);
        final IPartHost h = host(world, p);
        if (h == null) {
            log.add("B: no cable bus at " + RenderTest.pos(p));
            return;
        }
        final ItemStack busItem = AEApi.instance()
            .definitions()
            .parts()
            .exportBus()
            .maybeStack(1)
            .orNull();
        if (busItem == null || h.addPart(busItem, side, player) == null) {
            log.add("B: could not add an export bus at " + RenderTest.pos(p));
            return;
        }
        final IPart part = h.getPart(side);
        if (!(part instanceof PartSharedItemBus)) {
            log.add("B: the export bus at " + RenderTest.pos(p) + " is " + part);
            return;
        }
        final PartSharedItemBus<?> bus = (PartSharedItemBus<?>) part;
        bus.getAEInventoryByName(StorageName.CONFIG)
            .putAEStackInSlot(0, AEItemStack.create(filter));
        upgrades(bus, speedCards);
        facade(h, ForgeDirection.UP, log);
        bus.getConfigManager()
            .putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.HIGH_SIGNAL);
        h.markForUpdate();
        log.add(
            "B: export bus at " + RenderTest.pos(p)
                + " facing "
                + side
                + ": "
                + filter.getDisplayName()
                + ", redstone card (active with signal)"
                + (speedCards > 0 ? ", " + speedCards + " acceleration card(s)" : ""));
    }

    /** A quartz facade on {@code side}: a solid top the redstone dust can stand on. */
    private static void facade(IPartHost h, ForgeDirection side, List<String> log) {
        final net.minecraft.item.Item item = AEApi.instance()
            .definitions()
            .items()
            .facade()
            .maybeItem()
            .orNull();
        if (!(item instanceof ItemFacade)) {
            log.add("B: no AE2 facade item");
            return;
        }
        final ItemStack f = ((ItemFacade) item).createFacadeForItem(new ItemStack(Blocks.quartz_block), false);
        if (f == null || !h.getFacadeContainer()
            .addFacade(((ItemFacade) item).createPartFromItemStack(f, side))) {
            log.add("B: could not add a facade");
            return;
        }
        h.markForUpdate();
    }

    private static void upgrades(PartUpgradeable part, int speedCards) {
        final net.minecraft.inventory.IInventory up = part.getInventoryByName("upgrades");
        if (up == null) return;
        int slot = 0;
        final ItemStack red = AEApi.instance()
            .definitions()
            .materials()
            .cardRedstone()
            .maybeStack(1)
            .orNull();
        if (red != null) up.setInventorySlotContents(slot++, red);
        for (int i = 0; i < speedCards; i++) {
            final ItemStack sp = AEApi.instance()
                .definitions()
                .materials()
                .cardSpeed()
                .maybeStack(1)
                .orNull();
            if (sp != null) up.setInventorySlotContents(slot++, sp);
        }
    }

    private static void levelEmitter(World world, int[] p, ForgeDirection side, ItemStack watch, long level,
        EntityPlayer player, List<String> log) {
        final IPartHost h = host(world, p);
        final ItemStack item = AEApi.instance()
            .definitions()
            .parts()
            .levelEmitter()
            .maybeStack(1)
            .orNull();
        if (h == null || item == null || h.addPart(item, side, player) == null) {
            log.add("B: could not add a level emitter at " + RenderTest.pos(p));
            return;
        }
        final IPart part = h.getPart(side);
        if (part instanceof PartLevelEmitter) {
            final PartLevelEmitter e = (PartLevelEmitter) part;
            e.getAEInventoryByName(StorageName.CONFIG)
                .putAEStackInSlot(0, AEItemStack.create(watch));
            e.setReportingValue(level);
            e.getConfigManager()
                .putSetting(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
            h.markForUpdate();
        }
    }

    private static void place(World world, int[] p, Block block, List<String> log, String what) {
        if (block == null) {
            log.add("B: " + what + " is not registered");
            return;
        }
        world.setBlock(p[0], p[1], p[2], block, 0, 3);
    }

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

    // ----------------------------------------------------------------------------------- route

    private static long key(int[] p) {
        return ((long) (p[0] & 0x1fffff) << 42) | ((long) (p[1] & 0x1fffff) << 21) | (p[2] & 0x1fffff);
    }

    private static final int[][] DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** Turn-penalised shortest air path in 3D, both ends included. */
    private static List<int[]> route3d(World world, int[] from, int[] to, Set<Long> avoid) {
        final int r = 30;
        final int minX = Math.min(from[0], to[0]) - r, minY = Math.max(1, Math.min(from[1], to[1]) - 8),
            minZ = Math.min(from[2], to[2]) - r;
        final int w = Math.max(from[0], to[0]) + r - minX + 1,
            hgt = Math.min(254, Math.max(from[1], to[1]) + 20) - minY + 1, d = Math.max(from[2], to[2]) + r - minZ + 1;
        final int n = w * hgt * d;
        final int[] dist = new int[n * 6];
        final int[] prev = new int[n * 6];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        final PriorityQueue<int[]> q = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        final int start = ((from[0] - minX) * hgt + (from[1] - minY)) * d + (from[2] - minZ);
        for (int k = 0; k < 6; k++) {
            dist[start * 6 + k] = 0;
            q.add(new int[] { 0, start * 6 + k });
        }
        int goal = -1;
        while (!q.isEmpty()) {
            final int[] cur = q.poll();
            if (cur[0] > dist[cur[1]]) continue;
            final int cell = cur[1] / 6, k = cur[1] % 6;
            final int cx = cell / (hgt * d) + minX, cy = (cell / d) % hgt + minY, cz = cell % d + minZ;
            if (cx == to[0] && cy == to[1] && cz == to[2]) {
                goal = cur[1];
                break;
            }
            for (int nk = 0; nk < 6; nk++) {
                final int nx = cx + DIRS[nk][0], ny = cy + DIRS[nk][1], nz = cz + DIRS[nk][2];
                if (nx < minX || ny < minY || nz < minZ || nx >= minX + w || ny >= minY + hgt || nz >= minZ + d)
                    continue;
                final int[] np = { nx, ny, nz };
                final boolean isGoal = nx == to[0] && ny == to[1] && nz == to[2];
                if (!isGoal && (!world.isAirBlock(nx, ny, nz) || avoid.contains(key(np)))) continue;
                final int cost = cur[0] + 1 + (nk != k ? 4 : 0);
                final int ncell = ((nx - minX) * hgt + (ny - minY)) * d + (nz - minZ);
                if (cost < dist[ncell * 6 + nk]) {
                    dist[ncell * 6 + nk] = cost;
                    prev[ncell * 6 + nk] = cur[1];
                    q.add(new int[] { cost, ncell * 6 + nk });
                }
            }
        }
        if (goal < 0) return null;
        final List<int[]> path = new ArrayList<>();
        for (int idx = goal; idx >= 0; idx = prev[idx]) {
            final int cell = idx / 6;
            path.add(0, new int[] { cell / (hgt * d) + minX, (cell / d) % hgt + minY, cell % d + minZ });
            if (prev[idx] < 0) break;
        }
        return path;
    }
}
